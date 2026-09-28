#!/usr/bin/env bash
# Live-session helper (run on your Mac). Picks up an agent PR in its own worktree and starts a
# Claude Remote Control session seeded with the PR's context, so you can steer it from
# claude.ai/code or the Claude app.
#
#   harness/resume.sh <pr-number> [--local]    --local starts a normal terminal session instead
set -euo pipefail

pr="${1:?usage: harness/resume.sh <pr-number> [--local]}"
mode="${2:-}"
repo_root="$(git rev-parse --show-toplevel)"
repo="$(gh repo view --json nameWithOwner -q .nameWithOwner)"
branch="$(gh pr view "$pr" --json headRefName -q .headRefName)"
wt="$(dirname "$repo_root")/$(basename "$repo_root")-pr-$pr"

git -C "$repo_root" fetch -q origin "$branch"
if [[ ! -d "$wt" ]]; then
  git -C "$repo_root" worktree add -q "$wt" -B "$branch" "origin/$branch"
  git -C "$wt" branch -q --set-upstream-to "origin/$branch"
else
  git -C "$wt" pull -q --ff-only || echo "warning: $wt has diverged from origin/$branch" >&2
fi
[[ -f "$repo_root/local.properties" && ! -f "$wt/local.properties" ]] && cp "$repo_root/local.properties" "$wt/"

# CLAUDE.local.md is loaded automatically by Claude Code and is ignored by git.
grep -qx 'CLAUDE.local.md' "$(git -C "$repo_root" rev-parse --git-common-dir)/info/exclude" 2>/dev/null ||
  echo 'CLAUDE.local.md' >>"$(git -C "$repo_root" rev-parse --git-common-dir)/info/exclude"
issue="$(sed -n 's/^agent\/\([0-9]*\)-.*/\1/p' <<<"$branch")"
{
  echo "# Live session: PR #$pr"
  echo
  echo "You are continuing work on an agent PR together with the repo owner, who is steering live."
  echo "Follow CLAUDE.md. Push new commits to \`$branch\` (no force-push). Run \`./gradlew preflight\` before pushing."
  echo
  echo "## PR"
  gh pr view "$pr" --json title,url,body,labels,reviewDecision \
    -q '"**\(.title)** (\(.url))\nLabels: \([.labels[].name] | join(", "))  Review: \(.reviewDecision)\n\n\(.body)"'
  if [[ -n "$issue" ]]; then
    echo
    echo "## Issue #$issue"
    gh issue view "$issue" --json title,body -q '"**\(.title)**\n\n\(.body)"'
  fi
  echo
  echo "## Recent review activity"
  gh pr view "$pr" --comments --json comments,reviews \
    -q '([.reviews[] | "- review by \(.author.login) [\(.state)]: \(.body | .[0:500])"] + [.comments[-10:][] | "- comment by \(.author.login): \(.body | .[0:500])"]) | .[]'
  echo
  echo "## CI"
  gh pr checks "$pr" 2>/dev/null || true
} >"$wt/CLAUDE.local.md"

echo "Worktree: $wt (branch $branch). Context written to CLAUDE.local.md."
cd "$wt"
if [[ "$mode" == "--local" ]]; then
  exec claude "Read CLAUDE.local.md and summarise where PR #$pr stands and what's left to do."
else
  exec claude remote-control --name "$(basename "$repo") PR #$pr"
fi
