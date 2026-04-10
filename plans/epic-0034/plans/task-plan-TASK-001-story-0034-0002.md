# Task Plan -- TASK-0034-0002-001

## Header

| Field | Value |
|-------|-------|
| Task ID | TASK-0034-0002-001 |
| Story ID | story-0034-0002 |
| Epic ID | 0034 |
| Source Agent | Architect + Tech Lead (merged) |
| Type | implementation (delete + relocate) |
| TDD Phase | GREEN (compile-verified) |
| Layer | adapter.application |
| Estimated Effort | M |
| Date | 2026-04-10 |

## Objective

Delete the 7 Codex-specific production assembler classes and remove the `buildCodexAssemblers()` factory method from `AssemblerFactory`. This task MUST preserve the `DocsAdrAssembler` shared descriptor — historically registered inside `buildCodexAssemblers()` as an exception — by relocating it into an appropriate shared builder. This is the first atomic step; tests and enum edits follow in subsequent tasks.

## Implementation Guide

### Delete 1: 7 Codex Java classes

Delete the following files from `java/src/main/java/dev/iadev/application/assembler/`:

1. `CodexAgentsMdAssembler.java`
2. `CodexConfigAssembler.java`
3. `CodexSkillsAssembler.java`
4. `CodexRequirementsAssembler.java`
5. `CodexOverrideAssembler.java`
6. `CodexScanner.java`
7. `CodexShared.java`

### Edit 1: `AssemblerFactory.java`

Current state (post-story-0001, verified against `git show develop:.../AssemblerFactory.java` lines 224-253):

```java
private static List<AssemblerDescriptor>
        buildCodexAssemblers() {
    Set<Platform> codex = Set.of(Platform.CODEX);
    Set<Platform> shared = Set.of(Platform.SHARED);
    return List.of(
            desc("CodexAgentsMdAssembler", ROOT, codex, ...),
            desc("CodexConfigAssembler", CODEX, codex, ...),
            desc("CodexSkillsAssembler", CODEX_AGENTS, codex, ...),
            desc("CodexRequirementsAssembler", CODEX, codex, ...),
            desc("CodexOverrideAssembler", ROOT, codex, ...),
            desc("DocsAdrAssembler", ROOT, shared, ...));  // SHARED, not codex
}
```

**Action:**

1. **Relocate `DocsAdrAssembler` descriptor FIRST.** Options:
   - Option A (recommended): Move to `buildCicdAssemblers()` — it already handles shared/CI assemblers.
   - Option B: Create a new `buildDocsAssemblers()` method with only `DocsAdrAssembler`.
   - Option C: Move into `buildSharedAssemblers()` (if such a method exists post-story-0001).
2. Verify relocated descriptor compiles and is picked up by `buildAllAssemblers()`.
3. Delete the entire `buildCodexAssemblers()` method (lines 224-253).
4. Remove the call `all.addAll(buildCodexAssemblers());` (currently line 77) from `buildAllAssemblers()`.
5. Remove any `import dev.iadev.application.assembler.Codex*;` statements.

### Build & verify

- Run `mvn clean compile` from `java/`. Expected: BUILD SUCCESS.
- **Do NOT run `mvn test` yet** — test compilation will likely fail because Codex tests still reference the deleted classes. That cleanup belongs to TASK-002. If this task is committed in isolation, run only `mvn -am -pl . compile` (main source compilation only).
- Grep check: `grep -rn 'CodexConfigAssembler\|CodexScanner\|CodexShared' java/src/main/java` returns 0 matches.
- Grep check: `grep -n 'DocsAdrAssembler' java/src/main/java/dev/iadev/application/assembler/AssemblerFactory.java` returns exactly 1 occurrence (relocated).

### Commit

```
refactor(assembler)!: remove codex assemblers

Delete 7 Codex production classes (CodexAgentsMdAssembler,
CodexConfigAssembler, CodexSkillsAssembler, CodexRequirementsAssembler,
CodexOverrideAssembler, CodexScanner, CodexShared) and drop
buildCodexAssemblers() from AssemblerFactory.

DocsAdrAssembler was historically registered inside the Codex
builder as a shared descriptor — relocated to {buildCicdAssemblers |
buildSharedAssemblers | new buildDocsAssemblers} to preserve
its registration.

Post-edit AssemblerFactory.buildAllAssemblers() returns 19
descriptors (was 26 post-story-0001).

Refs: EPIC-0034, story-0034-0002, RULE-005
```

## Definition of Done

- [ ] 7 Codex Java classes deleted: CodexAgentsMdAssembler, CodexConfigAssembler, CodexSkillsAssembler, CodexRequirementsAssembler, CodexOverrideAssembler, CodexScanner, CodexShared
- [ ] `AssemblerFactory.buildCodexAssemblers()` method removed
- [ ] `AssemblerFactory.buildAllAssemblers()` no longer invokes `buildCodexAssemblers()`
- [ ] `DocsAdrAssembler` descriptor RELOCATED to a surviving builder (not orphaned)
- [ ] No orphan imports of deleted classes in `AssemblerFactory.java`
- [ ] `mvn compile` green (main source only; test-compile deferred to TASK-002)
- [ ] `AssemblerFactory.java` <= 250 lines post-edit
- [ ] [TL-006] Post-edit `buildAllAssemblers()` returns 19 descriptors (verified by `AssemblerFactoryBuildAllTest` in TASK-003 or manual count)
- [ ] Commit follows Conventional Commits with `refactor(assembler)!:` prefix and BREAKING CHANGE context in body
- [ ] Commit body documents the `DocsAdrAssembler` relocation decision

## Dependencies

| Depends On | Reason |
|-----------|--------|
| story-0034-0001 merged to develop | Baseline assumes post-story-0001 state (Platform.COPILOT, AssemblerTarget.GITHUB, 8 Github classes, 15 Github tests already removed; `buildAllAssemblers()` already returns 26) |

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| `DocsAdrAssembler` forgotten during relocation — silently orphaned | Medium | High | Mandatory grep check post-edit: `grep -n 'DocsAdrAssembler' AssemblerFactory.java` must return exactly 1 match. PR review checkpoint. |
| Test compilation fails because `Codex*Test.java` still imports deleted classes | High (by design) | None (expected) | TASK-002 deletes the tests atomically. This task commits with main-source-only compilation verified. |
| 17 dependent test files reference `Platform.CODEX` and fail compile | High | None in this task | TASK-003 handles dependent test cleanup. Deferred signal is acceptable. |
| Relocation choice for `DocsAdrAssembler` is suboptimal (e.g., placed in a builder that doesn't match its semantic purpose) | Low | Low | Option A (buildCicdAssemblers) is the recommended default. Reviewer may suggest Option B (new buildDocsAssemblers) if DocsAdrAssembler's concerns are significantly different. |
