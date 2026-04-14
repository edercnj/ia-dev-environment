---
status: Accepted
date: 2026-04-13
deciders:
  - Platform Team
story-ref: "story-0037-0009"
---

# ADR-0004: Worktree-First Branch Creation Policy for Parallel Skill Execution

## Status

Accepted | 2026-04-13

## Context

Multiple skills in `ia-dev-env` create git branches: `x-git-push`, `x-task-implement`, `x-story-implement`, `x-pr-fix-epic`, `x-release`, and `x-epic-implement`. Until EPIC-0037, only `x-epic-implement` used any form of git worktree isolation, and it did so via the harness-native `Agent(isolation:"worktree")` parameter — an implicit "automatic" feature that creates a worktree per subagent with opaque cleanup. Every other branch-creating skill operated directly on the main checkout via `git checkout -b`.

This arrangement caused three distinct problems:

1. **Conflict risk under parallelism.** When multiple instances of branch-creating skills ran concurrently in the same repository (for example, a user invoking `x-story-implement` twice in parallel terminals, or a CI job running alongside an interactive session), they would race on `git checkout -b` — producing cryptic git errors, partially-checked-out working trees, or silent overwrites of unrelated work.

2. **Documentation drift.** `x-git-worktree/SKILL.md` prominently documented an integration with `x-epic-implement` that did not exist. Epic-implement used the harness-native feature; the skill was never called from it. The diagram in "Integration with Epic Execution" therefore described a flow that did not happen, eroding trust in skill documentation more broadly.

3. **Lack of operator visibility.** With `Agent(isolation:"worktree")`, worktree creation and removal were invisible in execution logs. When subagents failed, operators had no straightforward way to inspect the failed worktree because the harness did not expose the path. This made triage of partial-failure states substantially harder than it needed to be.

We needed a unified policy that (a) eliminates the conflict risk for every branch-creating skill, (b) eliminates the documentation drift, (c) makes worktree lifecycle visible and operable, and (d) does not break backward compatibility for existing user invocations.

This ADR is a prerequisite for the rest of EPIC-0037: Rule 14 codifies the normative invariants, and stories `story-0037-0001` through `story-0037-0008` implement the policy across the six branch-creating skills (`x-story-implement`, `x-task-implement`, `x-epic-implement`, `x-pr-fix-epic`, `x-release`, `x-git-push`) plus `x-git-worktree` itself as the worktree manager. ADR-0003 (Skill Taxonomy and Naming) is orthogonal but related — that refactor made the set of branch-creating skills easy to enumerate by category (`git/`, `dev/`, `pr/`, `ops/`).

## Decision

We adopt a **Worktree-First Branch Creation Policy** composed of five sub-decisions.

### D1 — Explicit `/x-git-worktree create|remove` calls; deprecate harness-native isolation

All skills that create branches MUST use explicit calls to the `x-git-worktree` skill (or to its canonical `detect-context` inline snippet, Operation 5) for worktree management. The harness-native `Agent(isolation:"worktree")` parameter is **DEPRECATED** and is removed from `x-epic-implement` — its only previous user — as part of `story-0037-0003`.

**Rationale:** one mechanism, visible in logs, operable with `/x-git-worktree list` and `/x-git-worktree cleanup`. The harness feature's automatic cleanup is replaced by explicit `remove` calls plus a defensive `cleanup --dry-run` sweep at the end of orchestrator runs.

### D2 — Opt-in for standalone skills, automatic for orchestrators

| Skill                                      | Mode                                                                                                      |
|--------------------------------------------|-----------------------------------------------------------------------------------------------------------|
| `x-epic-implement` (orchestrator)          | **Automatic**: always creates a worktree per parallel story dispatch                                      |
| `x-pr-fix-epic` (orchestrator-like)        | **Automatic**: always creates a worktree for the consolidated correction branch                           |
| `x-release` (orchestrator-like)            | **Automatic**: always creates a worktree for the release/hotfix branch                                    |
| `x-story-implement` (standalone or subagent) | **Opt-in via `--worktree`** when standalone; auto-detect when running as a subagent inside an epic worktree |
| `x-task-implement` (standalone or subagent)  | Same as `x-story-implement`                                                                               |
| `x-git-push` (utility)                     | **Opt-in via `--worktree`** flag                                                                          |

**Rationale:** orchestrators have no user choice — parallel execution always benefits from isolation. Standalone skills must serve both interactive use (where users expect legacy behavior in the main checkout) and parallel use (where users opt in via the flag).

### D3 — Nesting invariant

No skill SHALL create a worktree when already running inside one. The check is mandatory before any `/x-git-worktree create` call. The detection mechanism is `git rev-parse --show-toplevel` combined with a substring check for `/.claude/worktrees/` in the canonicalized path. The canonical implementation lives in `x-git-worktree` Operation 5 (`detect-context`); every other skill calls it rather than reimplementing it.

**Rationale:** nested worktrees are supported by git but produce confusing operator state (worktrees-within-worktrees), break the creator-owns-removal invariant, and are not necessary for any known use case in this codebase.

### D4 — Creator owns removal

Whoever creates a worktree is responsible for removing it. Skills running as subagents NEVER remove worktrees they did not create. Failure cases intentionally preserve worktrees for diagnostic inspection; cleanup becomes the explicit responsibility of `/x-git-worktree cleanup` (opt-in, dry-run by default).

**Rationale:** pairs symmetrically with D1 — explicit creation implies explicit removal. Preserving failed worktrees gives operators the inspection surface that was missing under the harness-native mechanism.

### D5 — Rule 14 is first-class

The normative invariants of this policy live in `java/src/main/resources/targets/claude/rules/14-worktree-lifecycle.md` (source of truth; generated into `.claude/rules/14-worktree-lifecycle.md` for Claude Code sessions). Slot 14 is the target number; slots 10/11/12 are reserved for conditional rules and slot 13 is the skill invocation protocol. Contributors MUST edit only the source file under `java/src/main/resources/targets/`, never the generated output under `.claude/`. This rule file is loaded into every Claude Code session via the rules system, giving the policy global authority without requiring each skill body to restate it.

**Rationale:** rules are the project's established mechanism for cross-cutting invariants (see Rules 03, 05, 06, 07, 09, 13). A rule is lighter-weight than a skill KP and is always loaded; it is therefore the correct home for a policy whose compliance is checked at every branch-creating step.

## Alternatives Considered

### Alternative A — Keep `Agent(isolation:"worktree")` and only fix the documentation

**Rejected.** Pros: minimal change, automatic cleanup preserved. Cons: perpetuates the "magic" mechanism invisible to operators, does not extend to any standalone branch-creating skill (which remain exposed to the conflict risk), and leaves the fix-the-docs work addressing a symptom rather than the cause. The documentation drift was a signal of a deeper single-mechanism gap, not the root problem.

### Alternative B — Hybrid: harness for orchestrators, explicit for standalone

**Rejected.** Pros: pragmatic — reuses the harness feature where it works. Cons: introduces two mechanisms that contributors must learn and maintain; requires conditional logic in `x-epic-implement` to choose between them; doubles the surface area for bugs at the boundary; and leaves the documentation drift partially present (the harness flow is still undocumented-by-design). The one-mechanism simplification from D1 is worth the short-term migration cost.

### Alternative C — One ADR per affected skill

**Rejected.** The policy is a single coherent architectural commitment with shared invariants (D3 nesting, D4 creator-owns-removal) that would otherwise be duplicated across six to seven ADRs. Splitting the decision fragments what is conceptually one commitment and makes future review harder (reviewers would need to check every per-skill ADR for internal consistency).

### Alternative D (chosen) — Single ADR + Rule 14 + EPIC-0037

One ADR for the decision and its rationale (this document). One rule file (Rule 14) for the normative invariants. One epic (EPIC-0037) to implement the policy across all affected skills in small, independently reviewable stories.

## Consequences

### Positive

- **Operator visibility.** Every worktree create/remove is logged and inspectable via `/x-git-worktree list`. Failed subagents leave their worktrees behind for post-mortem inspection instead of disappearing into the harness.
- **Single mechanism.** Contributors learn one pattern (`/x-git-worktree create` plus the `detect-context` snippet) rather than two. The `Agent(isolation:"worktree")` parameter disappears from the project.
- **Documentation truth.** `x-git-worktree/SKILL.md` becomes accurate; its integration diagram reflects the real flow instead of an aspirational one.
- **Backward compatibility for interactive use.** Standalone skills without the `--worktree` flag behave exactly as before, preserving existing user muscle memory.
- **Conflict-free parallelism.** Two parallel `x-epic-implement` runs, or an `x-story-implement` run alongside an `x-pr-fix-epic` run, can no longer race on `git checkout -b`.

### Negative

- **Boilerplate at every call site.** Each branch-creating skill must inline a ~6-line `detect_worktree_context()` snippet before calling `/x-git-worktree create`. Mitigation: the snippet is canonicalized in `x-git-worktree` Operation 5 and cross-referenced from every consumer.
- **Manual cleanup burden for orchestrators.** Previously the harness silently removed no-change worktrees; now `x-epic-implement`, `x-pr-fix-epic`, and `x-release` must call `/x-git-worktree remove` explicitly and run `cleanup --dry-run` defensively. Mitigation: the cleanup phase is part of each orchestrator's Phase 3 / completion step (see STORY 3, STORY 6, STORY 7).
- **More git operations per epic run.** Each story dispatch adds roughly two git commands (create + remove). Mitigation: the cost is imperceptible compared to test execution time (seconds vs. minutes).
- **Migration window.** Between merging the first EPIC-0037 story and the last, the codebase has a mixed state: some skills use the new policy, some still use legacy behavior. Mitigation: each story's PR is self-consistent for the skills it touches; Rule 14 and the CI grep enforce the end state.

### Per-Skill Consequence Matrix

| Skill                                  | Behavior change                                                                                                        | Backward compatibility                                   |
|----------------------------------------|------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------|
| `x-epic-implement`                     | `Agent(isolation:"worktree")` removed; replaced with explicit `/x-git-worktree create|remove` per story dispatch       | Same end behavior; logs differ (worktrees now visible)   |
| `x-story-implement`                    | New `--worktree` flag; Phase 0 detects context; Phase 3 cleanup conditional on creator ownership                      | Without the flag: identical to prior behavior            |
| `x-task-implement`                     | New `--worktree` flag; branch creation detects context before calling `/x-git-worktree create`                        | Without the flag: identical to prior behavior            |
| `x-git-push`                           | New `--worktree` flag; Step 1.3 alternate path creates a worktree before branching                                    | Without the flag: identical to prior behavior            |
| `x-pr-fix-epic`                        | Always creates a worktree for the consolidated correction branch; idempotent across resumes                           | Branch semantics unchanged; isolation is additive        |
| `x-release`                            | Always creates a worktree for the release or hotfix branch                                                            | Branch semantics unchanged; isolation is additive        |
| `x-git-worktree`                       | New Operation 5 (`detect-context`); documentation drift removed; integration diagram rewritten to reflect real callers | Operations 1–4 unchanged                                  |

## Compliance / Enforcement

- **Code review.** PR reviewers verify the presence of the `detect_worktree_context()` guard before any `/x-git-worktree create` call and confirm that the skill's body does not call `git checkout -b` outside the worktree path.
- **CI grep — deprecated harness usage.** `grep -rn "Agent.*isolation.*worktree" java/src/main/resources/targets/` MUST return empty after STORY 3 merges.
- **CI grep — direct checkout bypass.** `grep -rn "git checkout -b" java/src/main/resources/targets/claude/skills/core/{git,dev,pr,ops}/` MUST return only matches inside `x-git-worktree` itself (its own implementation) or inside documented test fixtures.
- **Smoke test.** `/x-epic-implement` invoked with two parallel stories MUST succeed end-to-end without branch-creation conflicts.

## Review Triggers

This ADR should be revisited if any of the following occur:

- The Claude Code harness introduces a new isolation mechanism that supersedes `Agent(isolation:"worktree")` AND offers operator visibility comparable to explicit `/x-git-worktree` calls. D1 would be reconsidered.
- Explicit `/x-git-worktree` overhead becomes a measurable bottleneck — for example, if it adds more than 10 % to typical epic execution time. D2 (automatic vs. opt-in split) would be retuned.
- A new branch-creating skill is added to the codebase. The new skill MUST be evaluated against D2 (orchestrator → automatic; standalone → opt-in) and compliant with D3/D4 before merge.
- The nesting invariant (D3) is found to block a legitimate use case (such as a subagent that itself orchestrates sub-subagents). The invariant would be relaxed to allow controlled nesting, or the use case would be redesigned.

## Related ADRs

- **ADR-0003 — Skill Taxonomy and Naming Refactor.** The post-EPIC-0036 category structure (`git/`, `dev/`, `pr/`, `ops/`) makes the set of branch-creating skills enumerable and keeps this policy's scope unambiguous.
- **ADR-0002 — Skill Delegation Protocol (Rule 13).** Rule 13's explicit-`Skill(...)` delegation made it possible to audit every branch-creating call site mechanically, which is what allowed EPIC-0037 to converge on a single policy with confidence.
- **ADR-0001 — Intentional Architectural Deviations for CLI Tool.** Orthogonal; this ADR operates at the skills template layer and does not affect Java hexagonal layering.

## Story Reference

- **EPIC-0037** — Worktree-First Branch Creation Policy
  - **story-0037-0001** — Rule 14 (Worktree Lifecycle)
  - **story-0037-0002** — `x-git-worktree` Operation 5 (`detect-context`) and documentation correction
  - **story-0037-0003** — `x-epic-implement` migration off `Agent(isolation:"worktree")`
  - **story-0037-0004** — `x-story-implement` `--worktree` flag + Phase 0 detection
  - **story-0037-0005** — `x-task-implement` `--worktree` flag
  - **story-0037-0006** — `x-git-push` `--worktree` flag
  - **story-0037-0007** — `x-pr-fix-epic` automatic worktree
  - **story-0037-0008** — `x-release` automatic worktree
  - **story-0037-0009** — ADR-0004 (this document)
  - **story-0037-0010** — Final documentation pass and CI grep guardrails
