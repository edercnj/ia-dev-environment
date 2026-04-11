# Task Plan -- TASK-0034-0001-003

## Header

| Field | Value |
|-------|-------|
| Task ID | TASK-0034-0001-003 |
| Story ID | story-0034-0001 |
| Epic ID | 0034 |
| Source Agent | Architect + Security + Tech Lead (merged) |
| Type | implementation (edit — enum + CLI + util) |
| TDD Phase | GREEN |
| Layer | domain + adapter.inbound + util |
| Estimated Effort | S |
| Date | 2026-04-10 |

## Objective

Hygiene pass over the shared classes that reference `Platform.COPILOT`, `AssemblerTarget.GITHUB`, and `.github/` string prefixes. Removes all compile-time references to GitHub Copilot from the production source tree. This task MUST happen after TASK-001 and TASK-002 because those remove the downstream consumers; any reordering would leave dangling references that break the build.

## Implementation Guide

### Edit 1: `domain/model/Platform.java`

- Remove the enum constant `COPILOT("copilot")` (lines 32-33 + blank line)
- Update `allUserSelectable()` (line 85-87): `EnumSet.of(CLAUDE_CODE, COPILOT, CODEX)` -> `EnumSet.of(CLAUDE_CODE, CODEX)`
- Update Javadoc block (lines 12-19): remove the `{@link #COPILOT}` bullet
- Remove the `/** GitHub Copilot platform. */` comment

### Edit 2: `application/assembler/AssemblerTarget.java`

- Remove the `GITHUB(".github")` enum entry (lines 33-34 + blank line)
- Update the Javadoc table (lines 13-21): remove the `<tr><td>GITHUB</td>...` row
- Update class Javadoc if it mentions Copilot

### Edit 3: `cli/PlatformConverter.java`

- `ACCEPTED_VALUES` is computed dynamically from `Platform.allUserSelectable()` — no literal `"copilot"` string to remove. Changes in Edit 1 propagate automatically.
- Verify error message format stays CWE-209 compliant: contains only platform name + accepted values, no internal paths/classes/stack traces. Current implementation (lines 60-65) is compliant — NO EDIT NEEDED but verify.

### Edit 4: `cli/GenerateCommand.java`

- Update the `@Option(names = "--platform", ...)` description (lines ~96-103 per story). Change "Options: claude-code, copilot, codex, all" to "Options: claude-code, codex, all". Preserve the `all` keyword because it's still a valid special value.

### Edit 5: `cli/FileCategorizer.java`

- Remove lines 51-69 (the 6 `.github/*` branches: instructions, skills, agents, hooks, prompts, copilot-config)
- **CRITICAL RULE-003 check:** Currently there is NO `.github/workflows/` branch, so workflows files would fall through to `"Other"` category. This is acceptable because workflows files are preserved in golden files but NOT produced by the generator itself (they are pre-existing CI/CD config). Document this decision in the commit body.
- **Alternative:** If display regression to "Other" for workflows is undesirable, add a single branch `if (path.startsWith(".github/workflows/")) return "CI/CD";` at the top. Discuss with reviewer. Default: do not add (minimalist deletion).

### Edit 6: `util/OverwriteDetector.java`

- Update `ARTIFACT_DIRS` list (lines 38-41): remove `".github"` from the list
- The list becomes: `.claude, .codex, .agents, steering, specs, plans, results, contracts, adr`
- Update the Javadoc (lines 11-17): remove mention of `.github/`

### Edit 7: `application/assembler/PlatformContextBuilder.java`

- Remove `boolean copilot = effective.contains(Platform.COPILOT);` (lines 53-54)
- Remove `flags.put("hasCopilot", copilot);` (line 59)
- Update `countActive(...)` signature: remove `boolean copilot` parameter; update call site (line 62-63)
- Update `countActive(...)` body: remove the `if (copilot) { count++; }` block
- Remove Javadoc mention of `hasCopilot` (line 13)

### Build & verify

- Run `mvn clean compile` from `java/`. Expected: BUILD SUCCESS.
- Run `mvn test`. Expected: all tests pass. If `PlatformTest`, `AssemblerTargetTest`, `PlatformContextBuilderTest`, or `PlatformConverterTest` fail, minimally adjust them in this same task (atomic).
- Run grep check: `grep -ri 'copilot' java/src/main/java` — expected: 0 matches (source code only; resources under `targets/github-copilot/` still exist and will be removed in TASK-004).

### Commit

Use a conventional commit with BREAKING CHANGE footer because this changes the public CLI contract:

```
refactor(cli)!: remove Platform.COPILOT and AssemblerTarget.GITHUB

BREAKING CHANGE: CLI --platform no longer accepts "copilot".
Users receive "Invalid platform" error referencing the updated
accepted list (claude-code, codex, all).
```

## Definition of Done

- [ ] `Platform.java`: `COPILOT` constant removed, `allUserSelectable()` updated, Javadoc cleaned
- [ ] `AssemblerTarget.java`: `GITHUB(".github")` entry removed, Javadoc table updated
- [ ] `PlatformConverter.java`: verified to automatically reflect enum changes; error message stays CWE-209 compliant
- [ ] `GenerateCommand.java`: `--platform` option description no longer lists `copilot`
- [ ] `FileCategorizer.java`: 6 `.github/*` branches removed; workflows fall-through documented
- [ ] `OverwriteDetector.java`: `.github` removed from `ARTIFACT_DIRS`; Javadoc updated
- [ ] `PlatformContextBuilder.java`: `hasCopilot` flag and all related code removed
- [ ] [SEC-002/CWE-209] PlatformConverter error message verified: contains only platform name + accepted values
- [ ] [TL-003] `grep -ri 'copilot' java/src/main/java` = 0 matches (source code scope only)
- [ ] [TL-002] Post-edit line counts: all 7 files <= 250 lines; no method > 25 lines
- [ ] `mvn clean compile` green
- [ ] `mvn test` green
- [ ] Minimally adjust `PlatformTest`, `AssemblerTargetTest`, `PlatformContextBuilderTest`, `PlatformConverterTest` if they break (atomic in this commit)
- [ ] Commit follows Conventional Commits with `BREAKING CHANGE:` footer

## Dependencies

| Depends On | Reason |
|-----------|--------|
| TASK-0034-0001-001 | `AssemblerFactory` must stop using `Platform.COPILOT` before the enum constant can be removed |
| TASK-0034-0001-002 | Test classes that reference `Platform.COPILOT` must be deleted first |

## Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| `FileCategorizer` removal causes `.github/workflows/` files to be categorized as "Other" in CLI display | High | Low | Document decision. Workflows are not generated by this tool, so display regression is cosmetic. If reviewer objects, add a single-line `.github/workflows/` -> `CI/CD` branch in the same task. |
| Test classes that reference `Platform.COPILOT` directly (e.g., `PlatformTest.testAllUserSelectable_returnsThree()`) fail | Medium | Low | Expected. Update assertions to reflect new size. Atomic in this task. |
| `PlatformContextBuilderTest` asserts `hasCopilot` key is present in returned map | High | Low | Expected. Remove corresponding assertions. Atomic in this task. |
| Resources still reference `Platform.COPILOT` indirectly via YAML (e.g., `setup-config.*.yaml` with `platform: copilot`) | Low | Medium | YAMLs are string data, not compiled code. No build failure. Cleaned in TASK-006. |
| Breaking change not documented in CHANGELOG | Medium | Low | Story-0034-0005 updates CHANGELOG. Breaking change footer in this commit provides audit trail. |
