# Rule 15 — Task Testability (RULE-TF-01)

> **Related:** Rule 14 (Worktree Lifecycle), Rule 16 (I/O Contracts). Applies only when
> `planningSchemaVersion == "2.0"` (EPIC-0038 task-first flow). v1 epics are exempt.

## Rule

Every task file (`task-TASK-XXXX-YYYY-NNN.md`) MUST declare **exactly one** testability
kind in Section 2.3:

- **INDEPENDENT** — the task can be implemented and tested in full isolation. No mocks
  of other tasks required.
- **REQUIRES_MOCK of TASK-ID** — the task depends on the API surface of another task
  that is not yet implemented. Tests use a mock or fake implementation of that surface.
  The mocked TASK-ID MUST be named explicitly.
- **COALESCED with TASK-ID** — the task is mutually recursive with another task and
  the two MUST land in the same commit (see Rule 18). Partner must declare reciprocal
  COALESCED.

## Rationale

EPIC-0034 shipped tasks that silently depended on each other (TASK-001 could not
compile without TASK-002). This broke TDD honesty: `git show <TASK-001-commit>` did
not yield a green build. Rule 15 forces the dependency out of the dark — when a task
cannot be isolated, the author must declare COALESCED up-front and the orchestrator
ensures both land atomically.

## Enforcement

- `TaskFileParser` + `TaskValidator` (TF-SCHEMA-003): zero or multiple checked boxes
  reject the task file.
- `x-task-implement` Phase 0e: verifies the partner of a COALESCED declaration is
  present in the current dispatch batch.

## Forbidden

- Task files with zero testability declarations.
- Task files with more than one testability declaration.
- COALESCED without a real partner TASK-ID (placeholder `TASK-XXXX-YYYY-NNN` left in).
- COALESCED with a partner that does not reciprocate.
