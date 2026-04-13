# Story Planning Report — story-0037-0005

| Field | Value |
|-------|-------|
| Story ID | story-0037-0005 |
| Epic ID | 0037 |
| Date | 2026-04-13 |

## Planning Summary

Dual-mode worktree-awareness for `x-story-implement`. Standalone with `--worktree` creates and owns cleanup; orchestrated (via `x-epic-implement`) detects existing worktree and reuses. Defines `STORY_OWNS_WORKTREE` state variable that Phase 3 reads.

## Architecture Assessment

Modifies `x-story-implement/SKILL.md` frontmatter, parameters table, Phase 0 Step 6 (rewritten into 6.1/6.2/6.3), and Phase 3 (adds conditional cleanup block). Inlines `detect_worktree_context()` snippet. Decision table (Section 3.3) enumerates 3 contexts × flag combinations.

## Test Strategy Summary

5 Gherkin scenarios TPP-ordered: backward compat → standalone happy → orchestrated (nesting prevention) → failure preservation → boundary (2 parallel standalones). Smoke test exercises each path; parallel-standalone scenario proves isolation.

## Security Assessment Summary

- No new input surfaces. Worktree path derived from story ID (deterministic, safe).
- Creator-owns-removal prevents accidental cleanup of orchestrator-owned worktrees.
- Failure-preservation intentionally leaves worktree for diagnostics (operator trust boundary).
- Risk level: **LOW**.

## Implementation Approach

Sequential: frontmatter → Phase 0 Step 6 rewrite → Phase 3 cleanup + state doc → 5-scenario smoke (including parallel) → golden regen → PR.

## Risk Matrix

| Risk | Severity | Likelihood | Mitigation |
|------|----------|------------|------------|
| Orchestrated invocation creates nested worktree | CRITICAL | Low | Detection mandatory in Step 6.1; nesting prevention AC |
| Cleanup wipes orchestrator worktree (creator confusion) | HIGH | Low | `STORY_OWNS_WORKTREE` flag gates cleanup |
| Failed story leaks worktree | Medium | Medium | Failure-preservation is intentional; `x-git-worktree cleanup` periodic |
| Backward-compat breaks for users without flag | HIGH | Low | Backward-compat scenario is TASK-005 first case |

## DoR Status

**READY** — see `dor-story-0037-0005.md`.
