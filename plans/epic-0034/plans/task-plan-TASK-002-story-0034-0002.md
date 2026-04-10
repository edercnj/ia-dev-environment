# Task Plan -- TASK-0034-0002-002

## Header

| Field | Value |
|-------|-------|
| Task ID | TASK-0034-0002-002 |
| Story ID | story-0034-0002 |
| Epic ID | 0034 |
| Source Agent | Architect + QA Engineer (merged) |
| Type | test (delete) |
| TDD Phase | GREEN (compile-verified) |
| Layer | adapter.test |
| Estimated Effort | S |
| Date | 2026-04-10 |

## Objective

Delete the 6 Codex-specific test classes that become obsolete after TASK-001 removed their production counterparts. RULE-006 (TDD Compliance on Removal) requires that each deleted test was green on the pre-task baseline: confirm via the post-story-0001 develop HEAD before deletion. This task restores test-compile green but some dependent test files will still fail to compile because they reference `Platform.CODEX` / `AssemblerTarget.CODEX` — those edits land in TASK-003 atomically.

## Implementation Guide

### Pre-delete baseline check (RULE-006)

Before deleting any test file, confirm they were passing on the branch's baseline:

```bash
cd java
mvn test -Dtest='Codex*Test'
```

Expected: 6 test classes execute, all pass. If any fail or error, HALT and triage — RULE-006 forbids deleting a failing test without fixing it first.

### Delete 1: 6 Codex test classes

Delete the following files from `java/src/test/java/dev/iadev/application/assembler/`:

1. `CodexAgentsMdAssemblerTest.java`
2. `CodexConfigAssemblerTest.java`
3. `CodexSkillsAssemblerTest.java`
4. `CodexRequirementsAssemblerTest.java`
5. `CodexOverrideAssemblerTest.java`
6. `CodexSharedTest.java`

### Build & verify

- Run `mvn test-compile` from `java/`. Expected: BUILD SUCCESS if no OTHER test file imports the deleted classes. If dependent test files (the 17 identified in the tasks breakdown) fail because they reference `Platform.CODEX`, that failure is EXPECTED and signals TASK-003 must land atomically with this one.
- **Important:** If the test-compile chain breaks on the 17 dependent tests, coalesce TASK-002 + TASK-003 into a single atomic commit. RULE-001 (build green) applies to the story, not the individual task — but a single-commit-atomic-squash is acceptable when the dependency is unavoidable.
- Run `mvn test` for remaining tests. Expected: previous count minus 6, all green.

### Commit

```
test(assembler)!: remove codex test suites

Delete 6 Codex-specific test classes that validated assemblers
removed in TASK-0034-0002-001. Pre-delete baseline confirmed
green (RULE-006 compliance).

Remaining tests: previous count - 6.

Refs: EPIC-0034, story-0034-0002, RULE-005, RULE-006
```

## Definition of Done

- [ ] [QA-001/RULE-006] Pre-delete `mvn test -Dtest='Codex*Test'` green — all 6 tests confirmed passing on the baseline
- [ ] 6 test classes deleted: CodexAgentsMdAssemblerTest, CodexConfigAssemblerTest, CodexSkillsAssemblerTest, CodexRequirementsAssemblerTest, CodexOverrideAssemblerTest, CodexSharedTest
- [ ] `mvn test-compile` green for remaining test classes OR coalesced with TASK-003 into a single atomic commit
- [ ] `mvn test` green for remaining tests (or deferred into TASK-003 gate if coalesced)
- [ ] Commit follows Conventional Commits with `test(assembler)!:` prefix

## Dependencies

| Depends On | Reason |
|-----------|--------|
| TASK-0034-0002-001 | Production classes must be deleted first; otherwise deleted tests would leave main-source code untested but referenced by nothing (inverted scope) |

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Dependent test files (17 identified) fail to compile due to `Platform.CODEX` references | High (expected) | Low | Coalesce with TASK-003 atomically; document the coalescence in commit body |
| Pre-delete baseline shows a Codex test already failing | Very Low | Medium | Triage before deletion. RULE-006 requires fix-or-revert, not silent deletion |
| A Codex test was providing coverage for a shared utility (not Codex-specific) | Low | Medium | Code review of each test file's assertions before deletion. If a shared utility loses coverage, add a targeted test in TASK-003 |
