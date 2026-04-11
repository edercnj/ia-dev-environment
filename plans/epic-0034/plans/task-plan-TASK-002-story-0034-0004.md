# Task Plan -- TASK-0034-0004-002

## Header

| Field | Value |
|-------|-------|
| Task ID | TASK-0034-0004-002 |
| Story ID | story-0034-0004 |
| Epic ID | 0034 |
| Source Agent | merged(Architect, Tech Lead, QA Engineer) |
| Type | implementation (edit) |
| TDD Phase | GREEN |
| Layer | application |
| Estimated Effort | S (~2-3 hours) |
| Date | 2026-04-10 |

## Objective

Strip the `.github/`, `.codex/`, and `.agents/` columns and row-builders from `MappingTableBuilder` and `SummaryTableBuilder`. After this task, both classes produce single-column Claude-only tables. This eliminates the largest block of dead branching logic in the README generation pipeline.

## Implementation Guide

1. **MappingTableBuilder.java**:
   - Replace `coreArtifactRows()` and `additionalArtifactRows()` with a single `buildRows()` method returning a 2-column (claude / notes) row array.
   - Delete `isSinglePlatform()` entirely.
   - Delete the `ghTotal` computation + the `"**Total .github/ artifacts: %d**"` line.
   - Header line becomes `"| .claude/ | Notes |"` and separator `"|----------|-------|"`.
   - Keep the `build(Path outputDir, Set<Platform> platforms)` overload signature for call-site compatibility, but ignore the `platforms` parameter (or assert it contains only CLAUDE_CODE).
2. **SummaryTableBuilder.java**:
   - Delete `buildGithubRows()`, `resolveGithubDir()`, `resolveCodexDir()`, `resolveAgentsDir()`, `existsAsInt()`, `ghComponent()`.
   - `buildSummaryRows()` reduces to a single group: `return buildClaudeRows(outputDir);` (drop `concatRows` and `buildExtensionRows` if they become trivial).
   - `buildExtensionRows()`: delete — the `AGENTS.md`, `AGENTS.override.md`, `Codex (.codex)`, `Skills (.agents)` rows all correspond to removed targets.
   - `buildClaudeRows()`: unchanged (all rows are Claude-specific).
3. **SummaryRowFilter.java**: inspect the class. If it only filters rows by `Platform.COPILOT`/`Platform.CODEX` presence, the class is now dead code — delete it and remove its call site from `buildGenerationSummary()`. If it performs a broader filter still relevant to Claude-only output, simplify and keep.
4. Update `MappingTableBuilderTest.java` and `SummaryTableBuilderTest.java`: remove test methods that verified multi-column output, multi-platform filtering, or Codex/Agents rows. Update remaining assertions to match the reduced table shape.
5. Delete the temporary stubs in `ReadmeUtils` from task 001 (they are no longer called because the only callers — `MappingTableBuilder.build()` and `SummaryTableBuilder.buildGithubRows()` — have been deleted/simplified in this task).
6. Run `mvn -pl java compile test -Dtest=MappingTableBuilderTest,SummaryTableBuilderTest`.
7. Run full `grep -ni 'copilot\|codex\|agents' java/src/main/java/dev/iadev/application/assembler/MappingTableBuilder.java java/src/main/java/dev/iadev/application/assembler/SummaryTableBuilder.java` — expected 0 matches.
8. Commit.

## Definition of Done

- [ ] `MappingTableBuilder.build()` emits a 2-column table (`.claude/` + `Notes`)
- [ ] `MappingTableBuilder.isSinglePlatform()` deleted
- [ ] `SummaryTableBuilder.buildGithubRows()`, `resolveGithubDir/CodexDir/AgentsDir`, `buildExtensionRows()` deleted
- [ ] `SummaryRowFilter` deleted OR simplified (decision documented in commit body)
- [ ] `MappingTableBuilderTest` and `SummaryTableBuilderTest` updated and green
- [ ] `ReadmeUtils` stubs from task 001 removed
- [ ] Both files remain <= 250 lines; no method > 25 lines
- [ ] `grep -ni 'copilot\|codex\|agents' java/src/main/java/dev/iadev/application/assembler/MappingTableBuilder.java java/src/main/java/dev/iadev/application/assembler/SummaryTableBuilder.java` returns 0 matches
- [ ] `mvn -pl java compile test` green for impacted tests
- [ ] Conventional commit with scope `task-0034-0004-002` in trailer

## Dependencies

| Depends On | Reason |
|-----------|--------|
| TASK-0034-0004-001 | Deleted `ReadmeGithubCounter` and stubbed helper methods; this task finishes removing the call sites and deletes the stubs. |

## Estimated Effort

- MappingTableBuilder edit: 30 min
- SummaryTableBuilder edit: 45 min
- Test updates: 45 min
- Compile + grep verification: 10 min
- Commit: 5 min
- **Total: ~2h 15 min**

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| `SummaryRowFilter` has callers outside the tested files | Low | Medium | Grep `SummaryRowFilter` across all `java/src/main/java` before deletion; if extra callers exist, simplify rather than delete |
| Test fixtures assume multi-column output and break subtly (string-contains assertions) | Medium | Low | Read all test methods before editing; prefer whole-line matching over substring matching in updated assertions |
| Javadoc on `MappingTableBuilder` references `.github/` and `.codex/` | High | Low | Update class-level Javadoc as part of the edit (DoD covers this implicitly via <= 250 line + no-deadcode rule) |
