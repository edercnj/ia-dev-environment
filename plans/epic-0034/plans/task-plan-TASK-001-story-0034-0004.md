# Task Plan -- TASK-0034-0004-001

## Header

| Field | Value |
|-------|-------|
| Task ID | TASK-0034-0004-001 |
| Story ID | story-0034-0004 |
| Epic ID | 0034 |
| Source Agent | merged(Architect, Tech Lead, QA Engineer) |
| Type | implementation (delete + edit) |
| TDD Phase | GREEN (compile-verified; green-to-removed for helper methods per RULE-006) |
| Layer | application |
| Estimated Effort | S (~2 hours including test run) |
| Date | 2026-04-10 |

## Objective

Delete the `ReadmeGithubCounter` utility class and the thin delegator methods in `ReadmeUtils` that only existed to route counting calls to it. This breaks the last compile-time link between the README generation pipeline and the removed GitHub Copilot target. The refactor is degenerate (TPP nil) because the deleted methods have no remaining callers once stories 0001-0003 have physically removed `.github/` golden fixtures and the `GithubAssembler` family.

## Implementation Guide

1. Confirm the starting baseline is green: `mvn -pl java compile test -Dtest=ReadmeAssemblerTest` must pass on the HEAD of story-0034-0003's final merge commit.
2. Delete `java/src/main/java/dev/iadev/application/assembler/ReadmeGithubCounter.java`.
3. Open `ReadmeUtils.java` and delete any `countGithubFiles`, `countGithubComponent`, `countGithubSkills`, `countCodexFiles`, `countCodexAgentsFiles` methods that exist only to delegate to `ReadmeGithubCounter` or mirror its logic for Codex/Agents paths.
4. Compile: `mvn -pl java compile`. Any compile errors reveal callers that need attention. Expected callers (still referencing deleted methods):
   - `MappingTableBuilder.build()` — will be cleaned in TASK-0034-0004-002
   - `SummaryTableBuilder.buildGithubRows()` — will be cleaned in TASK-0034-0004-002
   - These two break compilation temporarily; that is acceptable INSIDE this task only because TASK-002 lives in the same story and the story-level DoD is "green at story end". If you prefer strict intra-task green, do the minimal stubbing: comment out the two call sites with a TODO and let task 002 remove them properly. **Recommended:** temporarily stub the two call-site methods to return empty/zero values, so the build stays green after task 001's commit. Task 002 will delete the stubs.
5. Run `mvn -pl java test -Dtest=ReadmeAssemblerTest` — it should still be green.
6. Run `grep -rn 'ReadmeGithubCounter' java/src` — expected: zero matches (confirms no orphan imports/references).
7. Stage the edits and commit with conventional format.

## Definition of Done

- [ ] `ReadmeGithubCounter.java` deleted
- [ ] `ReadmeUtils` delegator methods for GitHub/Codex/Agents counting deleted (or their bodies reduced to `return 0;` as temporary stubs)
- [ ] `grep -n 'ReadmeGithubCounter' java/src/main/java/dev/iadev/application/assembler/ReadmeAssembler.java` returns 0 matches
- [ ] `grep -rn 'import.*ReadmeGithubCounter' java/src/main` returns 0 matches
- [ ] `ReadmeAssembler.java` remains <= 250 lines and no method exceeds 25 lines
- [ ] `ReadmeAssemblerTest` green at end of task
- [ ] `mvn -pl java compile` green at end of task
- [ ] Conventional commit created with scope `task-0034-0004-001` in trailer
- [ ] No `ReadmeGithubCounterTest.java` file present post-task

## Dependencies

| Depends On | Reason |
|-----------|--------|
| TASK-0034-0003-004 (story-0034-0003 final verification) | Requires green baseline with `.agents/`, `.codex/`, `.github/` golden fixtures + assembler classes already physically removed; otherwise this task's compile would fail for unrelated reasons. |

## Estimated Effort

- Delete + edit: 10 min
- Compile + test run: 5 min
- Stub interim callers (if chosen): 15 min
- Commit + trailer hygiene: 5 min
- **Total: ~35 minutes of focused work** (buffer to S = 2h for test-run contention and review)

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Deleting `ReadmeUtils` methods breaks `MappingTableBuilder` and `SummaryTableBuilder` before task 002 can fix them | High | Medium (inter-task red) | Use temporary stub bodies returning 0 so compile stays green between tasks; delete the stubs in task 002 |
| A stray test file `ReadmeGithubCounterTest.java` survives from earlier stories | Low | Low (test compile break) | Explicit DoD grep covers this; delete if found |
| `ReadmeUtils` has callers outside the assembler package that use the deleted methods | Low | Medium (compile break) | `mvn compile` at the end of the task will catch this; widen grep to `java/src/main/java` if suspicious |
