# Task Plan -- TASK-0034-0002-003

## Header

| Field | Value |
|-------|-------|
| Task ID | TASK-0034-0002-003 |
| Story ID | story-0034-0002 |
| Epic ID | 0034 |
| Source Agent | Architect + Security + Tech Lead (merged) |
| Type | implementation (edit — enum + CLI + util + dependent tests) |
| TDD Phase | GREEN |
| Layer | domain + adapter.inbound + util + adapter.test |
| Estimated Effort | S |
| Date | 2026-04-10 |

## Objective

Hygiene pass over the shared classes that reference `Platform.CODEX`, `AssemblerTarget.CODEX`, and `.codex/` string prefixes, plus atomic adjustments to 17 dependent test files whose assertions embed the obsolete enum values. Removes all compile-time references to Codex from the production source tree and test tree simultaneously. This task MUST happen after TASK-001 and TASK-002 because those remove the downstream consumers; any reordering would leave dangling references that break the build. `AssemblerTarget.CODEX_AGENTS` (the `.agents/` dir) is DELIBERATELY PRESERVED — it is owned by story 0034-0003.

## Implementation Guide

### Edit 1: `domain/model/Platform.java`

- Remove the enum constant `CODEX("codex")` (currently lines 35-36 based on file read, plus blank line and `/** OpenAI Codex platform. */` comment).
- Update `allUserSelectable()`: `EnumSet.of(CLAUDE_CODE, COPILOT, CODEX)` should already be `EnumSet.of(CLAUDE_CODE, CODEX)` post-story-0001 — change to `EnumSet.of(CLAUDE_CODE)`.
- Update Javadoc block: remove the `{@link #CODEX}` bullet (currently lines 15-16).
- Expected enum values after edit: `{CLAUDE_CODE, SHARED}`.

### Edit 2: `application/assembler/AssemblerTarget.java`

- Remove the `CODEX(".codex")` enum entry (line 37 + blank line).
- Update the Javadoc table (lines 19): remove the `<tr><td>CODEX</td>...` row.
- **CRITICAL:** Do NOT remove `CODEX_AGENTS(".agents")` (line 40) — that belongs to story 0034-0003.
- Expected enum values after edit: `{ROOT, CLAUDE, CODEX_AGENTS}` (assuming GITHUB already removed in story 0034-0001).

### Edit 3: `cli/PlatformConverter.java`

- `ACCEPTED_VALUES` is computed dynamically from `Platform.allUserSelectable()` — no literal `"codex"` string to remove. Changes in Edit 1 propagate automatically.
- Verify error message format stays CWE-209 compliant: contains only platform name + accepted values, no internal paths/classes/stack traces. Current implementation is compliant — NO EDIT NEEDED but verify.

### Edit 4: `cli/GenerateCommand.java`

- Update the `@Option(names = "--platform", ...)` description. Post-story-0001 the options are `claude-code, codex, all`. Change to `claude-code, all` (or just `claude-code` if `all` is not a special keyword post-epic).
- Verify by running `--help` after build and confirming the description lists only surviving values.

### Edit 5: `cli/FileCategorizer.java`

- Remove lines 70-72 (the `.codex/` branch):
  ```java
  if (path.startsWith(".codex/")) {
      return "Codex";
  }
  ```
- **Preserve** the `.agents/` branch on lines 73-75 — that belongs to story 0034-0003.

### Edit 6: `util/OverwriteDetector.java`

- Update `ARTIFACT_DIRS` list (line 39): remove `".codex"`.
- Post-story-0001 the list is `{".claude", ".codex", ".agents", ...}` (".github" already removed). After this edit: `{".claude", ".agents", ...}`.
- Update the Javadoc (line 15): remove mention of `.codex/`.

### Edit 7: `application/assembler/PlatformContextBuilder.java`

- Remove `boolean codex = effective.contains(Platform.CODEX);` (line 55).
- Remove `flags.put("hasCodex", codex);` (line 60).
- Update `countActive(...)` signature: post-story-0001 it was `countActive(boolean claude, boolean codex)` — becomes `countActive(boolean claude)`. Call site: remove the `codex` argument.
- Update `countActive(...)` body: remove the `if (codex) { count++; }` block (lines 85-94).
- Update Javadoc (lines 13, 42): remove mention of `hasCodex`.

### Edit 8: Adjust 17 dependent test files

The following test files reference `Platform.CODEX`, `AssemblerTarget.CODEX`, or `hasCodex` and require atomic updates (grep baseline confirmed each file has direct references):

| File | Expected Edit |
|------|---------------|
| `domain/model/PlatformTest.java` | `allUserSelectable()` assertion: reduce expected set to `{CLAUDE_CODE}`; remove any `Platform.CODEX` lookup test |
| `domain/model/ProjectConfigTest.java` | Remove CODEX from parametrized test data |
| `application/assembler/PipelineOptionsTest.java` | Remove CODEX from test fixtures |
| `application/assembler/AssemblerFactoryBuildAllTest.java` | Update assembler count assertion: 26 (post-0001) -> 19 (post-0002); remove any Codex-specific descriptor count check |
| `application/assembler/AssemblerPipelineTest.java` | Remove CODEX from pipeline tests |
| `application/assembler/SummaryTableBuilderPlatformTest.java` | Update expected table rows: remove Codex row |
| `application/assembler/AssemblerFactoryPlatformTest.java` | Remove CODEX from platform-filtering tests |
| `application/assembler/PlatformContextBuilderTest.java` | Remove `hasCodex` key assertions from map checks; update `countActive()` call arity |
| `application/assembler/PlatformFilterTest.java` | Remove CODEX from filter tests |
| `application/assembler/SummaryRowFilterTest.java` | Remove CODEX rows |
| `cli/PlatformPrecedenceResolverTest.java` | Remove CODEX precedence cases |
| `cli/BuildPlatformSetTest.java` | Remove CODEX build cases |
| `cli/PlatformVerboseFormatterTest.java` | Remove CODEX format cases |
| `config/ContextBuilderPlatformTest.java` | Remove CODEX context cases |
| `smoke/PlatformPipelineIntegrationTest.java` | Remove CODEX end-to-end case |
| `smoke/AssemblerRegressionSmokeTest.java` | Update expected descriptor list (may need `expected-artifacts.json` regeneration — see TASK-006) |
| `smoke/PlatformDirectorySmokeTest.java` | Remove `.codex/` directory expectations |

For each file, prefer surgical edits (remove specific assertions or parametrized rows) over rewriting. Keep assertions about remaining platforms intact.

### Build & verify

- Run `mvn clean compile` from `java/`. Expected: BUILD SUCCESS.
- Run `mvn test`. Expected: all remaining tests green. Some smoke tests may still fail against stale `expected-artifacts.json` — that regeneration is TASK-006's responsibility; acceptable to defer or mark as expected.
- Grep check: `grep -rn 'Platform\\.CODEX\\|AssemblerTarget\\.CODEX\\b\\|hasCodex' java/src/main` returns 0. (Word boundary `\\b` on `AssemblerTarget.CODEX` is critical — `AssemblerTarget.CODEX_AGENTS` must still be findable.)
- Grep check: `grep -rn 'Platform\\.CODEX\\|AssemblerTarget\\.CODEX\\b\\|hasCodex' java/src/test` returns 0.

### Commit

```
refactor(cli)!: remove Platform.CODEX and AssemblerTarget.CODEX

Remove Codex from the user-selectable Platform enum and delete
the CODEX entry from AssemblerTarget. Hygiene pass covers:
- Platform.java (enum + allUserSelectable + javadoc)
- AssemblerTarget.java (enum + javadoc table) — CODEX_AGENTS preserved
- GenerateCommand.java (--platform description)
- FileCategorizer.java (.codex/ branch removed; .agents/ preserved)
- OverwriteDetector.java (ARTIFACT_DIRS)
- PlatformContextBuilder.java (hasCodex flag + countActive arity)

Atomically adjusts 17 dependent test files whose assertions
embedded the obsolete CODEX enum value.

BREAKING CHANGE: CLI --platform no longer accepts "codex". Users
receive "Invalid platform" error referencing the updated accepted
list (claude-code).

Refs: EPIC-0034, story-0034-0002
```

## Definition of Done

- [ ] `Platform.java`: `CODEX` constant removed; `allUserSelectable()` updated to `EnumSet.of(CLAUDE_CODE)`; Javadoc cleaned
- [ ] `AssemblerTarget.java`: `CODEX(".codex")` entry removed; `CODEX_AGENTS(".agents")` PRESERVED; Javadoc table updated
- [ ] `PlatformConverter.java`: verified to automatically reflect enum changes; error message stays CWE-209 compliant
- [ ] `GenerateCommand.java`: `--platform` description no longer lists `codex`
- [ ] `FileCategorizer.java`: `.codex/` branch removed; `.agents/` branch preserved
- [ ] `OverwriteDetector.java`: `".codex"` removed from `ARTIFACT_DIRS`; Javadoc updated
- [ ] `PlatformContextBuilder.java`: `hasCodex` flag + local var removed; `countActive()` signature reduced; call site updated
- [ ] 17 dependent test files updated with surgical edits (assertions adjusted, parametrized rows removed)
- [ ] [SEC-002/CWE-209] PlatformConverter error message verified: contains only platform name + accepted values, no internal paths/classes/stack traces
- [ ] [TL-003] `grep -rn 'Platform\\.CODEX\\|AssemblerTarget\\.CODEX\\b\\|hasCodex' java/src/main` = 0 matches
- [ ] `grep -rn 'Platform\\.CODEX\\|AssemblerTarget\\.CODEX\\b\\|hasCodex' java/src/test` = 0 matches
- [ ] `grep -n 'CODEX_AGENTS' java/src/main/java/dev/iadev/application/assembler/AssemblerTarget.java` still returns the entry (sanity check — do not over-delete)
- [ ] [TL-002] Post-edit line counts: all 7 edited production files ≤ 250 lines; no method > 25 lines
- [ ] `mvn clean compile` green
- [ ] `mvn test` green (except possibly `AssemblerRegressionSmokeTest` if it requires `expected-artifacts.json` regeneration — acceptable to defer to TASK-006)
- [ ] Commit follows Conventional Commits with `refactor(cli)!:` prefix and `BREAKING CHANGE:` footer

## Dependencies

| Depends On | Reason |
|-----------|--------|
| TASK-0034-0002-001 | `AssemblerFactory` must stop using `Platform.CODEX` before the enum constant can be removed |
| TASK-0034-0002-002 | Codex test classes must be deleted first; otherwise they would break after enum removal without being eligible for deletion |

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Accidentally deleting `CODEX_AGENTS` along with `CODEX` from `AssemblerTarget.java` (similar names, adjacent lines) | Medium | HIGH | Explicit sanity grep after edit: `grep -n 'CODEX_AGENTS' AssemblerTarget.java` MUST return the entry. Documented in DoD. |
| `AssemblerRegressionSmokeTest` fails due to stale `expected-artifacts.json` | High | Medium | Defer manifest regeneration to TASK-006 and mark test as expected-fail in commit body; OR coalesce regeneration into this task. Recommended: defer. |
| One of the 17 dependent test files has a subtler reference (e.g., `values().length == 4`) not caught by grep | Low | Medium | Post-edit full `mvn test` runs all of them; cascade failures point at missed spots. |
| `PlatformContextBuilder.countActive(boolean claude)` becomes trivial (single-arg) | Low | None (cosmetic) | Acknowledged in task 003 escalation notes. Inlining is out of story scope. |
| `PlatformContextBuilderTest` asserts `hasCodex` key is present in returned map | High (expected) | Low | Remove corresponding assertions atomically in this task. |
| Resources still reference `Platform.CODEX` indirectly via YAML | Low | Medium | YAMLs are string data, not compiled code. No build failure. Cleaned in TASK-006. |
