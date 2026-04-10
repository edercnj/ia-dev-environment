# Task Plan -- TASK-0034-0004-004

## Header

| Field | Value |
|-------|-------|
| Task ID | TASK-0034-0004-004 |
| Story ID | story-0034-0004 |
| Epic ID | 0034 |
| Source Agent | merged(Architect, Tech Lead, QA Engineer) |
| Type | implementation (edit) |
| TDD Phase | GREEN |
| Layer | application + domain + cli |
| Estimated Effort | M (~3-4 hours) |
| Date | 2026-04-10 |

## Objective

Hygienize the remaining shared files: `FileTreeWalker`, `PlatformParser`, `StackValidator`, `PlatformPrecedenceResolver`, and `IaDevEnvApplication`. These files carry stale references (string literals, category mappings, help text) to removed platforms. After this task, no class in `java/src/main/java` outside the test tree references `copilot`, `codex`, or `agents` as platform identifiers.

## Implementation Guide

1. **`java/src/main/java/dev/iadev/smoke/FileTreeWalker.java`** — `categorizeFiles(Path dir)`:
   - Delete all `countCategory(...)` calls whose `subPath` starts with `.github/`, `.codex/`, or `.agents/`. Specifically remove: `codex-skills (.agents/skills)`, `codex-config (.codex)`, `github-instructions (.github/instructions)`, `github-skills (.github/skills)`, `github-agents (.github/agents)`, `github-prompts (.github/prompts)`, `github-hooks (.github/hooks)`, `github-issue-templates (.github/ISSUE_TEMPLATE)`, `github-top (.github)`.
   - Keep: `claude-rules`, `claude-skills`, `claude-agents`, `claude-hooks`, `claude-settings`, `steering`, `adr`, `contracts`, `results`, `specs`, `plans`, `k8s`, `tests`, `root-files`.
2. **`java/src/main/java/dev/iadev/domain/model/PlatformParser.java`**:
   - Update `VALID_VALUES` constant from `"claude-code, copilot, codex, all"` to `"claude-code, all"` (keep "all" keyword if `GenerateCommand` still accepts it; otherwise drop to just `"claude-code"`).
   - Verify by reading `GenerateCommand` post-story-0001 to confirm whether `"all"` remains a valid input.
   - Update all error message string literals in `parseSingle`, `rejectNonSelectable`, `validateListElements`, `parseList` to reflect the new valid-values string.
   - `parseList()`: keep the method as-is (YAML list form is still valid, just with a degenerate single-value universe).
3. **`java/src/main/java/dev/iadev/domain/stack/StackValidator.java`** — `validatePlatforms()`:
   - Update the hardcoded string `"claude-code, copilot, codex, all"` in the error message to match `PlatformParser.VALID_VALUES`.
   - Consider tightening the method: since `Platform.allUserSelectable()` now returns `{CLAUDE_CODE}`, the loop iterates 0-1 times. Do not over-optimize — a clear loop is fine.
4. **`java/src/main/java/dev/iadev/cli/PlatformPrecedenceResolver.java`**:
   - Update Javadoc example in the class header: remove any reference to "copilot" or "codex" in usage comments.
   - Body logic is mostly platform-agnostic; no body changes expected.
5. **`java/src/main/java/dev/iadev/cli/IaDevEnvApplication.java`**:
   - Line 25: change `description = "Generates .claude/ and .github/ boilerplate for AI-assisted development environments."` to `description = "Generates .claude/ boilerplate for Claude Code development environments."`.
   - Javadoc (lines 10-22): remove the phrase "generates {@code .claude/} and {@code .github/} boilerplate". Keep usage examples.
6. **`EpicReportAssembler.java`** — verify task 003 caught all edits; if lingering comments reference `.github/templates/`, clean them up here.
7. Run the impacted tests:
```
mvn -pl java test -Dtest=FileTreeWalkerTest,ExpectedArtifactsGeneratorTest,PlatformParserTest,StackValidatorTest,PlatformPrecedenceResolverTest,IaDevEnvApplicationTest
```
Expect test failures for any test asserting on the old category set, old VALID_VALUES string, or old help-text description. Update each failing test to match the new reality.
8. Grep sanity:
```
grep -rn 'copilot\|codex\|agents' java/src/main/java | grep -v test
# expected: 0 matches (except allowed leftover in comments — review each hit)
```
9. Commit.

## Definition of Done

- [ ] `FileTreeWalker.categorizeFiles()` has no `.github/*`, `.codex/*`, `.agents/*` category calls
- [ ] `PlatformParser.VALID_VALUES` reflects Claude-only valid inputs
- [ ] `StackValidator.validatePlatforms()` error message matches `PlatformParser.VALID_VALUES`
- [ ] `PlatformPrecedenceResolver` Javadoc updated (no Copilot/Codex references)
- [ ] `IaDevEnvApplication.@Command.description` updated to Claude-only wording
- [ ] All 6 impacted test classes updated and green
- [ ] `grep -rn 'copilot\|codex\|agents' java/src/main/java` returns only allowed residues (documented in commit body if any)
- [ ] All edited files <= 250 lines; no method > 25 lines
- [ ] Error messages in Platform/Stack validators do not leak paths/stack traces (CWE-209 gate)
- [ ] `mvn -pl java compile test` green for impacted test classes
- [ ] Conventional commit with scope `task-0034-0004-004` in trailer

## Dependencies

| Depends On | Reason |
|-----------|--------|
| TASK-0034-0004-003 | Previous task completed the PlanTemplatesAssembler/EpicReportAssembler cleanup; this task finishes the remaining sweep. |

## Estimated Effort

- FileTreeWalker edit + test updates: 45 min
- PlatformParser edit + test updates: 45 min
- StackValidator edit + test updates: 30 min
- PlatformPrecedenceResolver Javadoc: 15 min
- IaDevEnvApplication edit + test updates: 20 min
- Compile + test run + grep sweep: 20 min
- Commit: 10 min
- **Total: ~3h 5 min**

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| `FileTreeWalker.categorizeFiles()` is called by `ExpectedArtifactsGeneratorTest` with expectations that include removed categories | High | Medium | Test updates are explicitly listed in DoD; run the single test class first, fix, then widen |
| `PlatformParser.VALID_VALUES` string is used in parameterized test data sets | Medium | Medium | Grep `"claude-code, copilot, codex, all"` across `java/src/test` before edit; update all hits |
| `IaDevEnvApplication` help-text change breaks a golden file that captures `--help` output | Medium | Low | Run `grep -rn "Generates .claude/ and .github/" java/src/test/resources/golden` — if hit, the golden file must be regenerated via `ExpectedArtifactsGenerator`; **not** hand-edited |
| Error messages in `PlatformParser` accidentally include stack trace context (CWE-209) | Low | High (security) | Read each `throw new ConfigValidationException(...)` statement and verify only the offending value + valid-values string are included |
| `grep -rn 'agents'` returns benign hits (e.g. `@Command(name = "agents")` is not a thing, but the word may appear in Javadoc for real reasons) | Medium | Low | Review each match manually; document allowed residues in commit body |
