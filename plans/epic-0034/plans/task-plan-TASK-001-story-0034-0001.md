# Task Plan -- TASK-0034-0001-001

## Header

| Field | Value |
|-------|-------|
| Task ID | TASK-0034-0001-001 |
| Story ID | story-0034-0001 |
| Epic ID | 0034 |
| Source Agent | Architect + Tech Lead (merged) |
| Type | implementation (deletion + edit) |
| TDD Phase | GREEN (compile-verified) |
| Layer | adapter.application |
| Estimated Effort | M |
| Date | 2026-04-10 |

## Objective

Delete the 8 GitHub Copilot assembler main classes and remove all references to them from `AssemblerFactory.java`. This is the first mechanical step of the atomic Copilot removal: it breaks the link between the generator pipeline and the `.github/` target, making subsequent enum/CLI edits safe.

## Implementation Guide

1. Delete the following 8 files from `java/src/main/java/dev/iadev/application/assembler/`:
   - `GithubInstructionsAssembler.java`
   - `GithubMcpAssembler.java`
   - `GithubSkillsAssembler.java`
   - `GithubAgentsAssembler.java`
   - `GithubHooksAssembler.java`
   - `GithubPromptsAssembler.java`
   - `GithubAgentRenderer.java`
   - `PrIssueTemplateAssembler.java`
2. Edit `AssemblerFactory.java`:
   - Delete method `buildGithubInputAssemblers()` (lines ~142-158)
   - Delete method `buildGithubOutputAssemblers()` (lines ~160-180)
   - Remove the two `all.addAll(buildGithubInputAssemblers()); all.addAll(buildGithubOutputAssemblers());` invocations from `buildAllAssemblers(options)` (lines ~74-75)
   - Update the class-level Javadoc: "34 assemblers" -> "26 assemblers" (line 13)
3. Run `mvn compile` from `java/` directory. Expected: BUILD SUCCESS. If `AssemblerFactoryTest` or any other test class in `src/test` still references deleted methods/classes, this task is incomplete — resolve compile errors by proceeding to TASK-002 which deletes test classes. This task's compile check is scoped to `mvn compile` (main source only), not `mvn test-compile`.
4. Verify assembler count by running: `mvn compile exec:java` or add a temporary assertion. Acceptance: `buildAllAssemblers(PipelineOptions.defaults()).size() == 26`.
5. Commit as a single atomic commit.

## Definition of Done

- [ ] 8 Github*Assembler.java files deleted from `java/src/main/java/dev/iadev/application/assembler/`
- [ ] `AssemblerFactory.buildGithubInputAssemblers()` deleted
- [ ] `AssemblerFactory.buildGithubOutputAssemblers()` deleted
- [ ] `AssemblerFactory.buildAllAssemblers()` no longer invokes github builders
- [ ] `AssemblerFactory.java` Javadoc updated: "34 assemblers" -> "26 assemblers"
- [ ] `mvn compile` green (in `java/` directory)
- [ ] `AssemblerFactory.java` <= 250 lines post-edit (RULE-04)
- [ ] No orphan imports referencing deleted classes (`grep -n 'Github\|PrIssueTemplate' java/src/main/java/dev/iadev/application/assembler/AssemblerFactory.java` = 0)
- [ ] Commit message follows: `refactor(assembler)!: remove github copilot assemblers`
- [ ] [TL-006] `buildAllAssemblers()` returns exactly 26 descriptors (verified by existing AssemblerFactoryTest which will need minor adjustment in TASK-002)

## Dependencies

| Depends On | Reason |
|-----------|--------|
| -- | First task in chain; no blockers |

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| `mvn compile` fails because test code references deleted classes | High | Medium | Scope compile check to `mvn compile` (main only). `mvn test-compile` runs in TASK-002 after test deletion. |
| Orphan imports in `AssemblerPipeline.java` or other classes that directly use `GithubInstructionsAssembler` | Low | Low | Pre-check: `grep -rn 'GithubInstructionsAssembler\|PrIssueTemplateAssembler' java/src/main/java` to identify all consumers before deletion. |
| `PrIssueTemplateAssembler` name does not match Github* prefix; may not be Copilot-specific | Low | Medium | Verified in AssemblerFactory.buildGithubOutputAssemblers (lines 176-179): registered with `AssemblerTarget.GITHUB` and `Platform.COPILOT`. Safe. |
