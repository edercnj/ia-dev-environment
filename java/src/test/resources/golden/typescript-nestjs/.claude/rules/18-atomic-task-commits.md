# Rule 18 — Atomic Task Commits (RULE-TF-04)

> **Related:** Rule 8 (Release Process / Conventional Commits), Rule 15, Rule 17.
> Applies only when `planningSchemaVersion == "2.0"`.

## Rule

Each task MUST produce EXACTLY ONE git commit when it reaches DONE. The commit:

1. Uses Conventional Commits form with scope `task(TASK-XXXX-YYYY-NNN)`:
   `feat(task-0039-0001-003): add Greeter.greet(name)`.
2. References the task artifact in the body.
3. Includes a TDD cycle summary (count of RED → GREEN → REFACTOR cycles).
4. Passes the pre-commit chain (format → lint → compile → commit).

## Coalesced Exception

A COALESCED pair lands as ONE commit with a footer listing the partner TASK-ID:

```
feat(task-0039-0001-004): add Writer + Generator (coalesced)

...body...

Coalesces-with: TASK-0039-0001-005
```

The partner's `execution-state.json` entry is marked DONE with the same commitSha —
the two task-IDs share a single atomic checkpoint.

## Rationale

Atomic per-task commits enable:

- **Bisect-ability**: every task is a stable checkpoint; `git bisect` can pinpoint
  regressions to a single task's diff.
- **Review granularity**: reviewers examine a focused per-task diff instead of a
  story-sized change.
- **Rollback precision**: `git revert <task-commit>` reverses exactly one task's
  work without touching siblings in the same wave.
- **Accountability**: commit history doubles as execution history.

## Enforcement

- `x-task-implement` Phase 4 (v2): invokes `x-git-commit` with the scope + body; a
  non-atomic commit (multiple logical changes) is rejected by the pre-commit chain.
- `x-story-implement` Phase 4 (v2) story-implementation-report aggregates per-task
  SHAs into the review artifact.

## Forbidden

- More than one commit per task (except when the task is split mid-flight into a
  retry, handled explicitly).
- Zero commits when the task claims DONE.
- Missing `Coalesces-with:` footer on coalesced commits.
- Amending a committed task to include another task's changes.
