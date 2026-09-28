#!/usr/bin/env bash
# Publishes device evidence for a PR and embeds it in the PR body's "On device" section.
#
#   harness/evidence.sh <pr-number> <evidence-dir>
#
# <evidence-dir> may contain *.png, *.gif, *.mp4 and an optional notes.md (shown above the media).
# MP4 recordings are converted to GIF so they render inline; the MP4 is linked too.
# Files are committed to the orphan `agent-evidence` branch under <pr>/<run-id>/ so they never
# land in the PR's own history. Requires: git credentials for origin (actions/checkout), gh, GH_TOKEN.
set -euo pipefail

pr="$1"
src="$2"
repo="${GITHUB_REPOSITORY:?}"
run="${GITHUB_RUN_ID:-local-$(date +%s)}"
branch="agent-evidence"
dest="$pr/$run"

shopt -s nullglob
media=("$src"/*.png "$src"/*.gif "$src"/*.mp4)
if ((${#media[@]} == 0)); then
  echo "No evidence in $src; skipping."
  exit 0
fi

# Recordings -> GIF (inline) + keep the MP4 (linked). ubuntu-latest has no ffmpeg; install it on
# demand. A failed conversion only loses the inline GIF, never the evidence.
mp4s=("$src"/*.mp4)
if ((${#mp4s[@]})) && ! command -v ffmpeg >/dev/null; then
  { sudo apt-get update -qq && sudo apt-get install -y -qq ffmpeg >/dev/null; } ||
    echo "::warning::Could not install ffmpeg; recordings will be linked, not inlined."
fi
for mp4 in "${mp4s[@]}"; do
  gif="${mp4%.mp4}.gif"
  [[ -e "$gif" ]] && continue
  ffmpeg -loglevel error -y -i "$mp4" \
    -vf "fps=10,scale=360:-1:flags=lanczos,split[a][b];[a]palettegen[p];[b][p]paletteuse" "$gif" ||
    { echo "::warning::GIF conversion failed for $(basename "$mp4")"; rm -f "$gif"; }
done

work="$(mktemp -d)"
trap 'git worktree remove --force "$work" 2>/dev/null || true' EXIT

if git ls-remote --exit-code --heads origin "$branch" >/dev/null 2>&1; then
  git fetch -q --depth=1 origin "$branch"
  git worktree add -q "$work" FETCH_HEAD
  git -C "$work" checkout -q -B "$branch"
else
  git worktree add -q --detach "$work"
  git -C "$work" checkout -q --orphan "$branch"
  git -C "$work" rm -rq --cached . 2>/dev/null || true
  git -C "$work" clean -fdxq
  printf '# agent-evidence\n\nDevice screenshots and recordings attached to agent PRs. Not code.\n' >"$work/README.md"
fi

mkdir -p "$work/$dest"
cp "$src"/*.{png,gif,mp4,md} "$work/$dest"/ 2>/dev/null || true
git -C "$work" add -A
git -C "$work" commit -qm "Evidence for #$pr (run $run)"
for _ in 1 2 3; do
  git -C "$work" push -q origin "$branch" && break
  git -C "$work" pull -q --rebase origin "$branch"
done

raw() { echo "https://github.com/$repo/blob/$branch/$dest/$1?raw=true"; }
section="$(mktemp)"
{
  echo "<!-- agent-evidence:start -->"
  echo "## On device"
  echo
  [[ -f "$src/notes.md" ]] && { cat "$src/notes.md"; echo; }
  for f in "$src"/*.png "$src"/*.gif; do
    name="$(basename "$f")"
    echo "<img src=\"$(raw "$name")\" width=\"270\" alt=\"$name\">"
  done
  for f in "$src"/*.mp4; do
    name="$(basename "$f")"
    echo
    echo "[$name]($(raw "$name"))"
  done
  echo
  echo "<sub>Run [$run](https://github.com/$repo/actions/runs/$run)</sub>"
  echo "<!-- agent-evidence:end -->"
} >"$section"

body="$(mktemp)"
gh pr view "$pr" --repo "$repo" --json body -q .body >"$body"
if grep -q '<!-- agent-evidence:start -->' "$body"; then
  awk -v f="$section" '
    /<!-- agent-evidence:start -->/ { while ((getline l < f) > 0) print l; skip = 1; next }
    /<!-- agent-evidence:end -->/   { skip = 0; next }
    !skip' "$body" >"$body.new"
else
  { cat "$body"; echo; cat "$section"; } >"$body.new"
fi
gh pr edit "$pr" --repo "$repo" --body-file "$body.new" >/dev/null
echo "Published ${#media[@]} evidence file(s) to $branch/$dest and updated PR #$pr."
