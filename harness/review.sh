#!/usr/bin/env bash
# Reviewer post-run: submits the model's verdict as a formal PR review (as the reviewer app)
# and applies the round limit.
#
#   harness/review.sh <issue> <pr> <head-sha> <conclusion> <structured-output-json>
#
# approve                           -> APPROVE review, ready-for-human
# request_changes, rounds left      -> REQUEST_CHANGES review (this wakes the Builder)
# request_changes, no rounds left   -> COMMENT review, agent:stuck (a human decides)
#
# Env: REVIEWER_TOKEN submits the review (so it's a real review by the reviewer app);
#      GH_TOKEN (builder) updates labels and the status comment (reviewer has read-only Issues).
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
cfg="$here/harness.yml"
repo="${GITHUB_REPOSITORY:?}"
status="$here/status.sh"
issue="$1" pr="$2" sha="$3" conclusion="$4" result="${5:-}"
max_rounds="$(yq -r .limits.max_review_rounds "$cfg")"
run_url="${GITHUB_SERVER_URL:-https://github.com}/$repo/actions/runs/${GITHUB_RUN_ID:-0}"
run_link="[${GITHUB_RUN_ID:-run}]($run_url)"

log() {
  "$status" log "$issue" "$(jq -cn --arg run "$run_link" --arg result "$1" --arg summary "$2" --arg retro "${3:-}" \
    '{run:$run, mode:"reviewer", result:$result, summary:$summary, retro:$retro}')"
}

[[ -n "$result" ]] && jq -e . >/dev/null 2>&1 <<<"$result" || result='{}'
decision="$(jq -r '.decision // empty' <<<"$result")"
if [[ "$conclusion" != "success" || -z "$decision" ]]; then
  "$status" labels "$pr" "agent:stuck" "agent:working"
  "$status" labels "$issue" "agent:stuck" "agent:working"
  log "error" "Reviewer run failed (conclusion: $conclusion)."
  "$status" update "$issue" '{}' "🛑 Reviewer failed on #$pr: [run]($run_url). Re-run the workflow or review manually."
  exit 0
fi

body="$(jq -r '.body' <<<"$result")"
retro="$(jq -r '.retro // empty' <<<"$result")"
rounds="$("$status" state "$issue" | jq -r '.review_rounds // 0')"

event="APPROVE"
if [[ "$decision" == "request_changes" ]]; then
  if ((rounds >= max_rounds)); then
    event="COMMENT"
    body="$body

---
_Review round limit ($max_rounds) reached, so I'm not requesting changes again. Handing over to a human._"
  else
    event="REQUEST_CHANGES"
  fi
fi

submit() { # $1 = include inline comments (true/false)
  jq -n --arg sha "$sha" --arg event "$event" --arg body "$body" --argjson inline "$1" --argjson r "$result" '
    {commit_id: $sha, event: $event, body: $body}
    + (if $inline and (($r.comments // []) | length > 0)
       then {comments: [$r.comments[] | {path, line, side: "RIGHT", body}]} else {} end)' |
    GH_TOKEN="${REVIEWER_TOKEN:?}" gh api -X POST "repos/$repo/pulls/$pr/reviews" --input - >/dev/null
}

if ! submit true 2>/tmp/review-err; then
  # Usually a line outside the diff. Fold inline comments into the body instead.
  echo "Inline review failed ($(cat /tmp/review-err)); retrying with comments in the body."
  body="$body

$(jq -r '(.comments // [])[] | "- `\(.path):\(.line)`: \(.body)"' <<<"$result")"
  submit false
fi

case "$event" in
  APPROVE)
    "$status" labels "$pr" "ready-for-human" "agent:working,agent:stuck"
    "$status" labels "$issue" "ready-for-human" "agent:working,agent:stuck"
    log "approved #$pr" "$(head -n1 <<<"$body")" "$retro"
    "$status" update "$issue" '{}' "✅ Ready for human: #$pr passed CI and agent review."
    ;;
  REQUEST_CHANGES)
    "$status" labels "$pr" "agent:working" ""
    log "changes requested #$pr (round $((rounds + 1))/$max_rounds)" "$(head -n1 <<<"$body")" "$retro"
    "$status" update "$issue" "{\"review_rounds\": $((rounds + 1))}" "🔁 Reviewer requested changes on #$pr (round $((rounds + 1))/$max_rounds). Builder will respond."
    ;;
  COMMENT)
    "$status" labels "$pr" "agent:stuck" "agent:working"
    "$status" labels "$issue" "agent:stuck" "agent:working"
    log "stuck #$pr" "Review round limit reached with open issues." "$retro"
    "$status" update "$issue" '{}' "🛑 Stuck: the Reviewer still has concerns on #$pr after $max_rounds rounds. Human decision needed."
    ;;
esac
echo "Review submitted: $event"
