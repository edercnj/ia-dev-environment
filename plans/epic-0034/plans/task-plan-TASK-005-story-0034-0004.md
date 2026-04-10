# Task Plan -- TASK-0034-0004-005

## Header

| Field | Value |
|-------|-------|
| Task ID | TASK-0034-0004-005 |
| Story ID | story-0034-0004 |
| Epic ID | 0034 |
| Source Agent | merged(QA Engineer, Architect, Tech Lead) |
| Type | test (edit) |
| TDD Phase | GREEN (with explicit RULE-006 green-to-removed audit) |
| Layer | adapter.test (smoke) |
| Estimated Effort | L (~4-5 hours) |
| Date | 2026-04-10 |

## Objective

Edit 4 smoke test classes to remove `@Nested` blocks and parameterized scenarios referencing `Platform.COPILOT`, `Platform.CODEX`, and `AssemblerTarget.GITHUB`/`CODEX`/`CODEX_AGENTS`. After this task, the smoke test suite exercises only the Claude Code platform, with rejection scenarios for previously-accepted platform strings.

## Implementation Guide

**Pre-flight audit (RULE-006)**:
- For each test method to be deleted, open the git log of the story-0034-0003 final merge commit and confirm the test was passing. Any test whose green state cannot be confirmed is NOT deleted in this task; instead it is flagged for the PR reviewer.

1. **`PlatformDirectorySmokeTest.java`**:
   - Delete `@Nested class Copilot` in its entirety (starts ~line 127).
   - Delete `@Nested class Codex` in its entirety.
   - If `@Nested class Agents` exists, delete it.
   - Inside `@Nested class ClaudeCode`: delete `claudeCode_githubInstructionsAbsent` (lines ~84-98), `claudeCode_codexAbsent` (~100-111), `claudeCode_agentsAbsent` (~113-124). These 3 assert that removed directories remain absent — trivially true post-removal, provides zero coverage value.
   - Keep `claudeCode_claudeDirHasContent` and any other positive ClaudeCode assertions.
2. **`AssemblerRegressionSmokeTest.java`**:
   - Read the file. Identify the `@MethodSource` provider that feeds Platform tuples.
   - Filter the stream to exclude `Platform.COPILOT` and `Platform.CODEX`.
   - Remove any explicit assertions checking `.github/`, `.codex/`, `.agents/` output paths.
3. **`CliModesSmokeTest.java`**:
   - Find the parameterized test that exercises `--platform copilot`, `--platform codex`, `--platform agents`.
   - Replace with 3 new scenarios (rejection pattern): each invokes the CLI with the dead platform value and asserts non-zero exit + stderr containing "Invalid platform" + stderr NOT containing the rejected value in the accepted list.
   - Retain `--platform claude-code` success scenario.
   - If `--platform all` scenario exists, verify it still succeeds (now equivalent to `--platform claude-code`).
4. **`GoldenFileCoverageTest.java`**:
   - Find any profile-iteration collection that includes Copilot/Codex/Agents paths.
   - Scope to claude-code only.
   - Remove references to `.github/`, `.codex/`, `.agents/` golden fixture paths.
5. **`AssemblerTargetTest.java`** (story §3.3 mentions but lives in `application/assembler/` not `smoke/`):
   - **EXCLUDED from this task scope** — validation happens via task 006's full `mvn clean verify`.
6. **Cross-file consistency** (Rule 05):
   - Verify the 4 edited files share the same "remove negative-assertion + filter parameter source" pattern.
   - If `SmokeProfiles.java` already exists and provides a `representativeProfiles()` helper, prefer using it over local duplicates.
7. Run the smoke suite: `mvn -pl java test -Dtest=*SmokeTest`.
8. Count remaining smoke test methods and record in commit body for PR reviewer visibility.
9. Commit.

## Definition of Done

- [ ] `PlatformDirectorySmokeTest`: `@Nested class Copilot`, `@Nested class Codex` deleted
- [ ] `PlatformDirectorySmokeTest`: 3 `claudeCode_*Absent` negative methods deleted
- [ ] `AssemblerRegressionSmokeTest`: parameterized sources no longer include removed platforms
- [ ] `CliModesSmokeTest`: rejection scenarios for `copilot/codex/agents` added; success scenarios retained only for `claude-code` (and `all` if still valid)
- [ ] `GoldenFileCoverageTest`: profile iteration claude-code only
- [ ] Each edited smoke test file <= 250 lines; method names follow `[methodUnderTest]_[scenario]_[expectedBehavior]`
- [ ] `grep -rnE '@Nested.*(Copilot\|Codex\|Agents)' java/src/test/java/dev/iadev/smoke` returns 0 matches
- [ ] `mvn -pl java test -Dtest=*SmokeTest` green
- [ ] Commit body records pre/post smoke test method count
- [ ] Conventional commit with scope `task-0034-0004-005` in trailer

## Dependencies

| Depends On | Reason |
|-----------|--------|
| TASK-0034-0004-004 | Previous task completed all main-tree hygienization; smoke tests can now be edited without fighting compile errors from main source references. |

## Estimated Effort

- Pre-flight RULE-006 audit: 30 min
- PlatformDirectorySmokeTest edit: 45 min
- AssemblerRegressionSmokeTest edit: 60 min
- CliModesSmokeTest edit + rejection scenarios: 60 min
- GoldenFileCoverageTest edit: 30 min
- Cross-file consistency verification: 15 min
- Full smoke run: 15 min
- Commit + method count capture: 15 min
- **Total: ~4h 30 min**

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Deleting `@Nested Copilot` / `@Nested Codex` reduces JaCoCo branch coverage of `PlatformFilter` below threshold | Medium | Medium | Task 002b simplified `PlatformFilter` so the branches no longer exist; coverage should be neutral or improve. Task 006 verifies with hard gate. |
| `CliModesSmokeTest` rejection scenarios accidentally match the old success behavior (typo in CLI invocation) | Medium | Low | Assert BOTH exit code AND stderr content; double verification prevents false green |
| `GoldenFileCoverageTest` has implicit dependencies on the golden manifest that was regenerated in stories 0001-0003 | Medium | Medium | Story 0034-0005 handles the final manifest regeneration; this task only edits code references, not the manifest file |
| RULE-006 violation: a test was silently failing in the story-0003 baseline but deleted anyway | Low | High | Explicit pre-flight audit step + PR reviewer sign-off required |
| Smoke test suite runtime INCREASES after edits (regression) | Low | Low | Story §3.5 promises a reduction; task 006 measures and records |
| A `@Nested class Agents` existed but named differently (e.g. `Codex` nests Agents inside) | Medium | Low | Read each file before editing; do not rely solely on grep |
