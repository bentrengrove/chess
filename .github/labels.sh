#!/usr/bin/env bash
# Creates or updates the agent harness labels on a repo. Idempotent.
# Usage: .github/labels.sh [owner/repo]   (defaults to the current gh repo)
set -euo pipefail

repo="${1:-$(gh repo view --json nameWithOwner -q .nameWithOwner)}"

label() {
  gh label create "$1" --repo "$repo" --color "$2" --description "$3" --force
}

label "agent"            "1d76db" "Owner-approved task for the Builder (no emulator)"
label "agent:device"     "5319e7" "Owner-approved task that needs emulator verification"
label "agent:working"    "fbca04" "An agent is currently working on this"
label "agent:needs-info" "d876e3" "Agent is blocked on questions for the owner"
label "agent:stuck"      "b60205" "Retry or review limits hit; a human needs to step in"
label "ready-for-human"  "0e8a16" "CI green and Reviewer approved; ready for owner review"
label "harness"          "c5def5" "Changes the harness itself (CLAUDE.md, workflows, harness/)"
