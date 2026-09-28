#!/usr/bin/env bash
# Builder gate: turns the triggering GitHub event into a concrete job (mode, issue, PR, branch,
# tier), enforces owner-only triggers and retry limits, and moves labels/status to "working".
# Runs in the gate job of agent-builder.yml, on the default branch's copy of the harness.
#
# Outputs (GITHUB_OUTPUT): run, mode, issue, pr, branch, tier, ci_run, trigger (multiline),
#                          max_turns, model, timeout
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
cfg="$here/harness.yml"
repo="${GITHUB_REPOSITORY:?}"
ev="${GITHUB_EVENT_PATH:?}"
event="${GITHUB_EVENT_NAME:?}"
status="$here/status.sh"

owner="$(yq -r .owner "$cfg")"
reviewer_bot="$(yq -r .bots.reviewer.login "$cfg")"
phrase="$(yq -r .trigger_phrase "$cfg")"
max_ci="$(yq -r .limits.max_ci_fix_attempts "$cfg")"
run_url="${GITHUB_SERVER_URL:-https://github.com}/$repo/actions/runs/${GITHUB_RUN_ID:-0}"

j() { jq -r "$1 // empty" "$ev"; }
out() { echo "$1=$2" >>"$GITHUB_OUTPUT"; }
skip() {
  echo "Skipping: $1"
  out run false
  exit 0
}

mode="" issue="" pr="" branch="" ci_run="" trigger="" author="" owner_trigger=false

case "$event" in
  issues)
    issue="$(j .issue.number)"
    author="$(j .sender.login)"
    trigger="Label \`$(j .label.name)\` added."
    ;;
  issue_comment)
    author="$(j .comment.user.login)"
    trigger="$(j .comment.body)"
    if [[ -n "$(j .issue.pull_request.url)" ]]; then pr="$(j .issue.number)"; else issue="$(j .issue.number)"; fi
    ;;
  pull_request_review_comment)
    author="$(j .comment.user.login)"
    pr="$(j .pull_request.number)"
    trigger="Inline comment on \`$(j .comment.path)\` line $(j .comment.line): $(j .comment.body)"
    ;;
  pull_request_review)
    author="$(j .review.user.login)"
    pr="$(j .pull_request.number)"
    trigger="Review (changes requested): $(j .review.body)"
    ;;
  workflow_run)
    author="$(j .workflow_run.actor.login)"
    ci_run="$(j .workflow_run.id)"
    pr="$(j '.workflow_run.pull_requests[0].number')"
    if [[ -z "$pr" ]]; then
      pr="$(gh pr list --repo "$repo" --state open --head "$(j .workflow_run.head_branch)" --json number -q '.[0].number // empty')"
    fi
    [[ -n "$pr" ]] || skip "no open PR for $(j .workflow_run.head_branch)"
    trigger="CI run $ci_run failed."
    ;;
  *) skip "unsupported event $event" ;;
esac

# Comment triggers must contain the trigger phrase (the workflow pre-filter checks this too).
if [[ "$event" == issue_comment || "$event" == pull_request_review_comment ]]; then
  [[ "$trigger" == *"$phrase"* ]] || skip "comment does not mention $phrase"
fi

# Owner-only gate (the reviewer bot may also request changes; CI failures are system events).
case "$event" in
  workflow_run) ;;
  pull_request_review) [[ "$author" == "$owner" || "$author" == "$reviewer_bot" ]] || skip "review by $author" ;;
  *) [[ "$author" == "$owner" ]] || skip "trigger by $author (owner only)" ;;
esac
[[ "$author" == "$owner" ]] && owner_trigger=true

# Resolve issue <-> PR <-> branch. Agent branches are named agent/<issue>-<slug>.
if [[ -z "$pr" && -n "$issue" ]]; then
  pr="$(gh pr list --repo "$repo" --state open --json number,headRefName \
    -q "[.[] | select(.headRefName | startswith(\"agent/$issue-\"))][0].number // empty")"
fi

if [[ -n "$pr" ]]; then
  prj="$(gh pr view "$pr" --repo "$repo" --json state,headRefName,headRefOid,isCrossRepository,labels)"
  [[ "$(jq -r .state <<<"$prj")" == "OPEN" ]] || skip "PR #$pr is not open"
  [[ "$(jq -r .isCrossRepository <<<"$prj")" == "false" ]] || skip "PR #$pr is from a fork"
  branch="$(jq -r .headRefName <<<"$prj")"
  [[ "$branch" =~ ^agent/([0-9]+)- ]] || skip "PR #$pr is not an agent branch ($branch)"
  issue="${BASH_REMATCH[1]}"
  if [[ "$event" == "workflow_run" && "$(jq -r .headRefOid <<<"$prj")" != "$(j .workflow_run.head_sha)" ]]; then
    skip "CI failure is for an outdated commit of PR #$pr"
  fi
  mode="iterate"
  [[ "$event" == "workflow_run" ]] && mode="fix-ci"
  pr_labels="$(jq -r '[.labels[].name] | join(",")' <<<"$prj")"
else
  mode="new"
  pr_labels=""
fi

issue_json="$(gh issue view "$issue" --repo "$repo" --json title,labels,state)"
[[ "$(jq -r .state <<<"$issue_json")" == "OPEN" ]] || skip "issue #$issue is closed"
labels="$(jq -r '[.labels[].name] | join(",")' <<<"$issue_json"),$pr_labels"
if [[ ",$labels," == *",agent:device,"* ]]; then
  tier="device"
elif [[ ",$labels," == *",agent,"* ]]; then
  tier="agent"
else
  skip "issue #$issue has no agent / agent:device label"
fi

if [[ "$mode" == "new" ]]; then
  slug="$(jq -r .title <<<"$issue_json" | tr '[:upper:]' '[:lower:]' | sed -E 's/^\[agent\] *//; s/[^a-z0-9]+/-/g; s/^-+|-+$//g' | cut -c1-40 | sed -E 's/-+$//')"
  branch="agent/$issue-${slug:-task}"
fi

# Counters: owner steering resets the budget; CI auto-fix is capped.
state="$("$status" state "$issue")"
patch='{}'
if [[ "$owner_trigger" == true ]]; then
  patch='{"ci_fix_attempts":0,"review_rounds":0}'
fi
if [[ "$mode" == "fix-ci" ]]; then
  attempts=$(($(jq -r '.ci_fix_attempts // 0' <<<"$state") + 1))
  if ((attempts > max_ci)); then
    "$status" labels "$issue" "agent:stuck" "agent:working"
    "$status" labels "$pr" "agent:stuck" "agent:working"
    "$status" update "$issue" '{}' "🛑 Stuck: CI still failing on #$pr after $max_ci auto-fix attempts. Comment \`$phrase\` to retry."
    skip "CI auto-fix limit reached for PR #$pr"
  fi
  patch="$(jq -c ". + {ci_fix_attempts: $attempts}" <<<"$patch")"
fi

"$status" labels "$issue" "agent:working" "agent:needs-info,agent:stuck,ready-for-human"
[[ -n "$pr" ]] && "$status" labels "$pr" "agent:working" "agent:stuck,ready-for-human"
"$status" update "$issue" "$patch" "🔨 Builder working ($mode${pr:+ on #$pr}): [run]($run_url)"

out run true
out mode "$mode"
out issue "$issue"
out pr "$pr"
out branch "$branch"
out tier "$tier"
out ci_run "$ci_run"
out max_turns "$(yq -r .limits.builder_max_turns "$cfg")"
out model "$(yq -r '.models.builder // ""' "$cfg")"
out timeout "$(yq -r .limits.job_timeout_minutes "$cfg")"
{
  echo "trigger<<__TRIGGER_EOF__"
  echo "$trigger (by @$author)"
  echo "__TRIGGER_EOF__"
} >>"$GITHUB_OUTPUT"
echo "Builder: mode=$mode issue=#$issue pr=${pr:-none} branch=$branch tier=$tier"
