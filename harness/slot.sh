#!/usr/bin/env bash
# Waits until fewer than limits.max_concurrent_jobs agent jobs are running, so at most N
# Builder/Reviewer jobs use the Claude subscription at once. Best-effort (small race window).
#
#   harness/slot.sh      (in the gate job, before the agent job starts)
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
cfg="$root/harness/harness.yml"
repo="${GITHUB_REPOSITORY:?}"
max="$(yq -r .limits.max_concurrent_jobs "$cfg")"
wait_min="$(yq -r .limits.slot_wait_minutes "$cfg")"
deadline=$((SECONDS + wait_min * 60))

running() {
  # Count in-progress *agent* jobs (named "agent: ...") across agent workflows, excluding gates.
  local total=0 wf ids id n
  for wf in agent-builder.yml agent-reviewer.yml agent-gardener.yml; do
    ids="$(gh api "repos/$repo/actions/workflows/$wf/runs?status=in_progress&per_page=20" \
      -q '.workflow_runs[].id' 2>/dev/null || true)"
    for id in $ids; do
      n="$(gh api "repos/$repo/actions/runs/$id/jobs" \
        -q '[.jobs[] | select(.status == "in_progress" and (.name | startswith("agent:")))] | length')"
      total=$((total + n))
    done
  done
  echo "$total"
}

while :; do
  n="$(running)"
  if ((n < max)); then
    echo "Slot free ($n/$max agent jobs running)."
    exit 0
  fi
  if ((SECONDS > deadline)); then
    echo "::error::No free agent slot after ${wait_min} minutes ($n/$max running)."
    exit 1
  fi
  echo "Waiting for a free slot ($n/$max agent jobs running)..."
  sleep 60
done
