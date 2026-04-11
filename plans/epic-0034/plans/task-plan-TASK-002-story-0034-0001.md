# Task Plan -- TASK-0034-0001-002

## Header

| Field | Value |
|-------|-------|
| Task ID | TASK-0034-0001-002 |
| Story ID | story-0034-0001 |
| Epic ID | 0034 |
| Source Agent | Architect + QA Engineer |
| Type | test (deletion) |
| TDD Phase | GREEN (compile-verified) |
| Layer | adapter.test |
| Estimated Effort | M |
| Date | 2026-04-10 |

## Objective

Delete the 15 GitHub Copilot test classes plus the 1 shared test fixture. These tests cover production code that was deleted in TASK-001, so by RULE-006 (TDD Compliance on Removal) they must be removed atomically with the production code. Before deletion, confirm the baseline was green (test-level audit trail).

## Implementation Guide

1. **Pre-delete audit (RULE-006):** On `develop` baseline (pre-deletion), confirm all 16 Github* test files were passing. Evidence: baseline document (`plans/epic-0034/baseline-pre-epic.md`) reports 837 passing tests, 0 failures. Record this in the PR description.
2. Delete the following 15 test classes from `java/src/test/java/dev/iadev/application/assembler/`:
   - `GithubInstructionsCopilotTest.java`
   - `GithubInstructionsFormatTest.java`
   - `GithubInstructionsCoverageTest.java`
   - `GithubInstructionsFileGenTest.java`
   - `GithubInstructionsGoldenTest.java`
   - `GithubMcpAssemblerTest.java`
   - `GithubSkillsAssemblerTest.java`
   - `GithubSkillsAssemblerConditionalTest.java`
   - `GithubSkillsAssemblerIntegrationTest.java`
   - `GithubHooksAssemblerTest.java`
   - `GithubAgentsAssemblerTest.java`
   - `GithubAgentsEventTest.java`
   - `GithubAgentsConditionalTest.java`
   - `GithubAgentsRenderCoreTest.java`
   - `GithubPromptsAssemblerTest.java` (NOT listed in story §3.2 but present in filesystem)
3. Delete the shared test fixture:
   - `GithubInstructionsTestFixtures.java`
4. If `AssemblerFactoryTest.java` references `Platform.COPILOT` or asserts assembler count = 34, update to new count (26). Use minimum edit necessary — do not rewrite unrelated assertions.
5. Run `mvn test-compile` from `java/`. Expected: BUILD SUCCESS.
6. Run `mvn test`. Expected: all remaining tests pass.
7. Commit as a single atomic commit.

## Definition of Done

- [ ] [QA-001/RULE-006] Baseline green confirmed (reference baseline document in PR)
- [ ] 15 Github*Test.java files deleted from `java/src/test/java/dev/iadev/application/assembler/`
- [ ] `GithubInstructionsTestFixtures.java` deleted
- [ ] Minor edits applied to any test that referenced assembler count or `Platform.COPILOT` (e.g., `AssemblerFactoryTest`)
- [ ] `mvn test-compile` green
- [ ] `mvn test` green (remaining tests)
- [ ] Test count reduced from baseline 837 by at least 15 classes worth of tests (exact delta recorded in commit body)
- [ ] Commit message: `test(assembler)!: remove github copilot test suites`

## Dependencies

| Depends On | Reason |
|-----------|--------|
| TASK-0034-0001-001 | Production classes must be deleted first so the test-compile failure provides a clear signal of which tests to delete |

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Story §3.2 lists 14 tests but actual is 15 (missed GithubPromptsAssemblerTest) | Medium | Low | Use filesystem `ls Github*Test.java` as authoritative. Delete all 15. |
| `AssemblerFactoryTest` asserts exact count of 34 | High | Low | Expected — update assertion to 26. This is not a violation of RULE-006 because the test is being adjusted to reflect a deliberate behavior change (assembler count), not weakened. |
| Golden file regression: `AssemblerRegressionSmokeTest` compares assembler list against a fixture | Medium | Medium | If this test fails, check whether fixture includes Github assemblers. If yes, regenerate or hand-edit the fixture in this same task (atomic). Document in commit body. |
