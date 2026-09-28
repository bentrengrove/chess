#!/usr/bin/env bash
# Sticky status comment on an agent issue. The comment holds a hidden JSON state blob
# (counters + run history) and a visible rendering of it. The workflows own this state
# machine; the model never edits it directly.
#
#   harness/status.sh state  <issue>                          print state JSON ({} if none)
#   harness/status.sh update <issue> <patch-json> <headline>  deep-merge patch, set headline
#   harness/status.sh log    <issue> <entry-json>             append a run to history
#       entry: {"run": "...", "mode": "...", "result": "...", "summary": "...", "retro": "..."}
#   harness/status.sh labels <number> <add-csv> [remove-csv]  add/remove labels on issue or PR
#
# Requires gh (GH_TOKEN) and GITHUB_REPOSITORY.
set -euo pipefail

repo="${GITHUB_REPOSITORY:?}"
marker="<!-- agent-status -->"

find_comment() {
  gh api --paginate "repos/$repo/issues/$1/comments" \
    -q ".[] | select(.body | startswith(\"$marker\")) | .id" | tail -n1
}

read_state() {
  local id="$1" s=""
  if [[ -n "$id" ]]; then
    s="$(gh api "repos/$repo/issues/comments/$id" -q .body |
      sed -n 's/^<!-- agent-state: \(.*\) -->$/\1/p' | tail -n1)"
  fi
  if [[ -n "$s" ]]; then echo "$s"; else echo '{}'; fi
}

render() {
  jq -r '
    def cell: tostring | gsub("\\|"; "\\|") | gsub("\n"; " ");
    "### 🤖 Agent status\n\n**\(.headline // "Queued")**\n",
    (if (.history // []) | length > 0 then
      "\n| Run | Mode | Result | Summary | Retro |\n|---|---|---|---|---|",
      (.history[-15:][] | "| \(.run | cell) | \(.mode | cell) | \(.result | cell) | \(.summary // "" | cell) | \(.retro // "" | cell) |")
    else empty end),
    "\n<sub>CI fix attempts: \(.ci_fix_attempts // 0) · review rounds: \(.review_rounds // 0)</sub>"'
}

write() {
  local issue="$1" id="$2" state="$3" payload
  payload="$(mktemp)"
  { echo "$marker"; render <<<"$state"; echo; echo "<!-- agent-state: $(jq -c . <<<"$state") -->"; } >"$payload"
  if [[ -n "$id" ]]; then
    gh api -X PATCH "repos/$repo/issues/comments/$id" -F body=@"$payload" >/dev/null
  else
    gh api -X POST "repos/$repo/issues/$issue/comments" -F body=@"$payload" >/dev/null
  fi
}

case "${1:-}" in
  state)
    read_state "$(find_comment "$2")"
    ;;
  update)
    issue="$2" patch="$3" headline="$4"
    id="$(find_comment "$issue")"
    state="$(jq -c -n --argjson a "$(read_state "$id")" --argjson b "$patch" --arg h "$headline" \
      '($a * $b) | .headline = $h')"
    write "$issue" "$id" "$state"
    ;;
  log)
    issue="$2" entry="$3"
    id="$(find_comment "$issue")"
    state="$(jq -c -n --argjson a "$(read_state "$id")" --argjson e "$entry" '$a | .history = ((.history // []) + [$e])')"
    write "$issue" "$id" "$state"
    ;;
  labels)
    num="$2" add="${3:-}" remove="${4:-}"
    if [[ -n "$add" ]]; then gh issue edit "$num" --repo "$repo" --add-label "$add" >/dev/null; fi
    IFS=, read -ra rm_list <<<"$remove"
    for l in "${rm_list[@]}"; do
      [[ -z "$l" ]] && continue
      gh api -X DELETE "repos/$repo/issues/$num/labels/$(jq -rn --arg l "$l" '$l|@uri')" >/dev/null 2>&1 || true
    done
    ;;
  *)
    echo "usage: $0 state|update|log|labels ..." >&2
    exit 2
    ;;
esac
