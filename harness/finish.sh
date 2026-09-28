#!/usr/bin/env bash
# Builder post-run: validates the result, enforces protected paths, and moves labels/status.
#
#   harness/finish.sh <issue> <pr-from-gate> <mode> <tier> <conclusion> <structured-output-json>
#
# Always runs (if: always()), including when Claude failed or produced no structured output.
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
cfg="$here/harness.yml"
repo="${GITHUB_REPOSITORY:?}"
status="$here/status.sh"
issue="$1" gate_pr="$2" mode="$3" tier="$4" conclusion="$5" result="${6:-}"
run_url="${GITHUB_SERVER_URL:-https://github.com}/$repo/actions/runs/${GITHUB_RUN_ID:-0}"
run_link="[${GITHUB_RUN_ID:-run}]($run_url)"

[[ -n "$result" ]] && jq -e . >/dev/null 2>&1 <<<"$result" || result='{}'
res() { jq -r ".$1 // empty" <<<"$result"; }
st="$(res status)"
pr="$(res pr_number)"
pr="${pr:-$gate_pr}"
summary="$(res summary)"
retro="$(res retro)"

if [[ "$conclusion" != "success" || -z "$st" ]]; then
  st="stuck"
  summary="${summary:-The Builder run failed or ended without a result (conclusion: $conclusion). See the run log.}"
fi

tier_label="agent"
[[ "$tier" == "device" ]] && tier_label="agent:device"

# Feature PRs must not touch harness files (the app can't push workflows anyway, but CLAUDE.md
# and harness/ are writable).
if [[ -n "$pr" && "$st" != "stuck" ]]; then
  pr_labels="$(gh pr view "$pr" --repo "$repo" --json labels -q '[.labels[].name] | join(",")')"
  if [[ ",$pr_labels," != *",harness,"* ]]; then
    base="$(gh pr view "$pr" --repo "$repo" --json baseRefName -q .baseRefName)"
    git fetch -q origin "$base"
    mapfile -t protected < <(yq -r '.protected_paths[]' "$cfg")
    touched="$(git diff --name-only "origin/$base...HEAD" -- "${protected[@]}" || true)"
    if [[ -n "$touched" ]]; then
      gh pr comment "$pr" --repo "$repo" --body "🛑 This feature PR modifies protected harness paths, which CLAUDE.md forbids:
\`\`\`
$touched
\`\`\`
Revert these changes, or split them into a separate \`harness:\` PR." >/dev/null
      st="stuck"
      summary="Touched protected paths: $(tr '\n' ' ' <<<"$touched")"
    fi
  fi
fi

case "$st" in
  pr_opened | pr_updated)
    "$status" labels "$pr" "$tier_label" "agent:working"
    "$status" labels "$issue" "" "agent:working"
    headline="⏳ PR #$pr ${st#pr_}, waiting for CI and review."
    ;;
  needs_info)
    "$status" labels "$issue" "agent:needs-info" "agent:working"
    headline="❓ Needs info: answer the questions above, then comment \`@claude\` (or re-add the label)."
    ;;
  *)
    st="stuck"
    "$status" labels "$issue" "agent:stuck" "agent:working"
    [[ -n "$pr" ]] && "$status" labels "$pr" "agent:stuck" "agent:working"
    headline="🛑 Stuck: ${summary:-see run log}. Comment \`@claude\` to retry."
    ;;
esac

entry="$(jq -cn --arg run "$run_link" --arg mode "builder/$mode" --arg result "$st${pr:+ #$pr}" \
  --arg summary "$summary" --arg retro "$retro" '{run:$run, mode:$mode, result:$result, summary:$summary, retro:$retro}')"
"$status" log "$issue" "$entry"
"$status" update "$issue" '{}' "$headline"

{
  echo "### Builder: $st"
  echo
  echo "$summary"
  [[ -n "$retro" ]] && printf '\n**Retro:** %s\n' "$retro"
} >>"${GITHUB_STEP_SUMMARY:-/dev/null}"
echo "Builder finished: $st${pr:+ (PR #$pr)}"
