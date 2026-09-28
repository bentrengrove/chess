# Agent harness

Issue-driven agents for this repo. You write an issue and add a label; a Builder agent opens a PR, CI and a Reviewer agent check it, and you review a PR that is nearly ready to merge.

## Using it

| You do | What happens |
|---|---|
| File an **Agent task** issue, then add `agent` (or `agent:device` for UI work) | The Builder implements it on `agent/<issue>-<slug>` and opens a PR |
| The Builder asks questions (`agent:needs-info`) | Answer them in a comment containing `@claude`, or re-add the label |
| Comment `@claude ...` on the PR (or inline) | The Builder addresses it and pushes |
| Submit a "Request changes" review | The Builder addresses it and pushes |
| — | CI fails: the Builder auto-fixes, at most `max_ci_fix_attempts` times, then `agent:stuck` |
| — | CI green: the Reviewer reviews. Up to `max_review_rounds` rounds with the Builder, then `ready-for-human` or `agent:stuck` |
| `harness/resume.sh <pr>` on your Mac | Worktree + Claude Remote Control session seeded with the PR context |

Only @bentrengrove can trigger agents. Your `@claude` comment also resets the retry and round counters. Each issue has a sticky **Agent status** comment with the current state, a run history, and each run's retro.

## Pieces

| Path | Role |
|---|---|
| `harness.yml` | Limits, emulator image, bot identities, protected paths |
| `gate.sh` | Builder pre-run: event to mode/issue/PR/branch/tier, owner gate, CI-fix cap, labels |
| `finish.sh` | Builder post-run: result to labels/status, protected-path check |
| `review.sh` | Submits the Reviewer's verdict as a formal review, round cap |
| `status.sh` | Sticky status comment, hidden state, label helpers |
| `slot.sh` | Global concurrency cap across agent jobs |
| `emulator.sh` | Headless emulator setup/snapshot/start/stop (`agent:device`) |
| `evidence.sh` | Pushes captures to the `agent-evidence` branch and fills the PR's "On device" section |
| `prompts/` | Builder, Reviewer and Gardener briefs |
| `schemas/` | Structured output each agent must return |
| `resume.sh` | Live-session helper |

Workflows: `.github/workflows/agent-builder.yml`, `agent-reviewer.yml`, and `agent-gardener.yml` (weekly).

## Rules of the road

- The workflows always load `harness/` from the default branch, so a PR branch can't change how it is run or reviewed.
- The bots can't push workflow changes (the apps have no `workflows` permission) or merge (the `main` ruleset requires a Code Owner review).
- Harness changes arrive only as separate `harness:` PRs, from you or from the weekly gardener.
