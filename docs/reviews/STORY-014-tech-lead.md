============================================================
 TECH LEAD REVIEW -- STORY-014
============================================================
 Decision:  GO
 Score:     39/40
 Critical:  0 issues
 Medium:    0 issues (2 fixed)
 Low:       3 issues
------------------------------------------------------------

## Section Scores

### A. Code Hygiene (7/8)

- No unused imports or variables across all 6 source files.
- No dead code detected. All branches exercised per coverage report.
- Zero compiler warnings (`npx tsc --noEmit` passes cleanly).
- Method signatures are consistent and well-typed.
- Magic strings: All directory names and filenames are extracted to module-level constants (`TEMPLATES_DIR_NAME`, `GITHUB_AGENTS_TEMPLATES_DIR`, `CORE_DIR`, `CONDITIONAL_DIR`, `DEVELOPERS_DIR`, `AGENT_MD_EXTENSION`, `MD_EXTENSION`, `SKILL_MD`, `INFRA_GROUP`, etc.).
- **Deduction (-1):** `generateGroup` in `github-skills-assembler.ts` has 5 parameters (line 95-100), exceeding the 4-parameter limit. Should use a parameter object.

### B. Naming (4/4)

- All names are intention-revealing: `selectGithubConditionalAgents`, `warnLiteralEnvValues`, `buildCopilotMcpDict`, `buildCopilotInstructions`.
- Module-level helper functions use descriptive names: `formatInterfaces`, `formatFrameworkVersion`, `buildIdentitySection`, `buildStackSection`, `buildConstraintsSection`, `buildContextualRefsSection`.
- No disinformation: class names (`GithubHooksAssembler`, `GithubMcpAssembler`, etc.) clearly describe what they assemble.
- File naming follows kebab-case convention consistently.
- Constants use UPPER_SNAKE: `GITHUB_HOOK_TEMPLATES`, `GITHUB_PROMPT_TEMPLATES`, `SKILL_GROUPS`, `INFRA_SKILL_CONDITIONS`.

### C. Functions (4/5)

- SRP well maintained: each private method handles one concern (e.g., `assembleCore`, `assembleConditional`, `assembleDeveloper`, `renderAgent`).
- Most functions are well within the 25-line limit.
- No boolean flag parameters detected anywhere.
- Pure functions exported separately for testability: `buildCopilotInstructions`, `warnLiteralEnvValues`, `buildCopilotMcpDict`, `selectGithubConditionalAgents`.
- **Deduction (-1):** `generateGroup` in `github-skills-assembler.ts` (lines 95-121) is 27 lines including signature, exceeding the 25-line limit by 2 lines.

### D. Vertical Formatting (4/4)

- Blank lines separate concepts consistently across all files.
- Newspaper Rule followed: public API (exports, class, `assemble` method) at top, private methods below.
- All 6 source files are within the 250-line limit:
  - `github-hooks-assembler.ts`: 47 lines
  - `github-prompts-assembler.ts`: 52 lines
  - `github-mcp-assembler.ts`: 81 lines
  - `github-skills-assembler.ts`: 122 lines
  - `github-agents-assembler.ts`: 165 lines
  - `github-instructions-assembler.ts`: 191 lines

### E. Design (3/3)

- Law of Demeter respected: no train-wreck chains. Config property access is at most 2 levels deep (e.g., `config.infrastructure.container`).
- CQS followed: `assemble` methods return results (query) and write files (command) but this is the established pattern for assemblers.
- DRY: No significant duplication. Each assembler has its own distinct logic. The shared pattern (check dir exists, iterate templates, render, write) is the inherent structure of the problem, not accidental duplication. The `hasAnyInterface` helper from `conditions.ts` is properly reused.

### F. Error Handling (3/3)

- No null returns for collections: all methods return `string[]` or `AssembleResult` with empty arrays.
- `assembleDeveloper` returns `string | null` appropriately, and callers handle it with explicit null checks.
- Missing template directories and files handled gracefully with early returns or `continue`.
- Warning messages carry context: include server ID, env key name, template filenames.
- No generic catch blocks.

### G. Architecture (5/5)

- SRP: Each assembler has exactly one responsibility.
- DIP: All assemblers depend on `ProjectConfig` (model) and `TemplateEngine` (abstraction). No concrete infrastructure dependencies beyond `node:fs` and `node:path`.
- Layer boundaries respected: assemblers import only from `../models.js`, `../template-engine.js`, and sibling assembler utilities (`conditions.ts`, `rules-assembler.ts` for the `AssembleResult` type).
- No cross-dependencies between the 6 new assemblers.
- Plan adherence: All 6 assemblers match the implementation plan exactly. File paths, method signatures, return types, and logic all align with `docs/plans/STORY-014-plan.md`.
- Barrel exports added correctly in `src/assembler/index.ts` with the `STORY-014` section comment.

### H. Framework & Infra (3/4)

- No DI framework used (appropriate for this library-style project).
- Configuration externalized through `ProjectConfig` model.
- All file I/O uses explicit `utf-8` encoding.
- Unused parameters (`_config`, `_engine`, `_resourcesDir`) properly prefixed with underscore.
- **Deduction (-1):** `GithubPromptsAssembler.assemble` accepts `_config` but does not use it. While this maintains API uniformity (all assemblers have the same signature), it could benefit from a brief JSDoc note explaining the intentional unused parameter for API parity, similar to `GithubHooksAssembler`.

### I. Tests (3/3)

- 88 tests across 6 test files, all passing.
- Coverage on the 6 new source files: 100% lines, 96.66-100% branches.
- Branch coverage gap is minimal (1 branch in `github-agents-assembler.ts` line 148, the `devDir` not-existing path within `assembleDeveloper`).
- AAA pattern followed consistently: Arrange (create templates, build config), Act (call assemble), Assert (verify files/content).
- Test naming convention followed: `[method]_[scenario]_[expected]`.
- Edge cases covered: missing directories, missing individual files, empty configs, partial configs.
- Parametrized tests used effectively in agents test (`it.each` for infrastructure fields).
- Proper test isolation: `beforeEach`/`afterEach` with temp directories, cleanup with `rmSync`.
- Both pure function tests and integration tests (assemble) present for assemblers with exported helpers.

### J. Security & Production (1/1)

- Path traversal protection: `path.basename(config.language.name)` used in `GithubAgentsAssembler.assembleDeveloper` (line 144).
- MCP assembler validates `$VARIABLE` format for env values.
- No secrets, credentials, or sensitive data in code or tests.
- No new third-party dependencies introduced.

## Cross-File Consistency

All 6 assemblers follow a consistent pattern:

1. **Module structure:** JSDoc module comment, imports, constants, exported pure functions/constants, class with `assemble` method, private helpers.
2. **`assemble` signature:** All accept `(config, outputDir, resourcesDir, engine)` with consistent parameter types and ordering.
3. **Directory creation:** All use `fs.mkdirSync(dir, { recursive: true })`.
4. **Missing directory handling:** All check `fs.existsSync(srcDir)` and return early.
5. **Missing file handling:** All use `if (!fs.existsSync(src)) continue`.
6. **Unused parameters:** Consistently prefixed with `_` (`_config`, `_engine`, `_resourcesDir`).
7. **Return types:** 4 assemblers return `string[]`, 2 return `AssembleResult` (those with warnings).
8. **Template rendering:** 4 use `engine.replacePlaceholders()`, 1 uses `engine.renderTemplate()` (prompts), 1 copies verbatim (hooks).

No inconsistencies detected across the 6 files.

## Comparison with Existing Assemblers

The 6 new GitHub assemblers align well with the established patterns from:

- **`AgentsAssembler`:** Same core/conditional/developer pattern used in `GithubAgentsAssembler`. Both return `AssembleResult`. Both use `selectConditionalAgents`-style exported functions.
- **`SkillsAssembler`:** Same group-based iteration pattern in `GithubSkillsAssembler`.
- **`HooksAssembler`:** Same simple-copy pattern in `GithubHooksAssembler`. Both accept `engine` for API uniformity without using it.
- **`PatternsAssembler`:** Similar render-and-write pattern.

Key differences (all justified):
- GitHub assemblers use `github/` output subdirectory instead of top-level.
- `GithubAgentsAssembler` outputs `.agent.md` extension (GitHub convention) vs `.md` (Claude convention).
- `GithubAgentsAssembler` does not use `copy-helpers.ts` (it inlines the read-render-write pattern). This is acceptable because GitHub agents have the unique `.agent.md` extension transformation that does not fit the generic copy helper API.
- `GithubSkillsAssembler` inlines filtering logic via `INFRA_SKILL_CONDITIONS` record rather than a separate selection module. Justified by simplicity: 5 conditions vs the more complex Claude skills selection.

## Issues Found

### M01: `generateGroup` exceeds 25-line limit (MEDIUM) — FIXED
- **File:** `src/assembler/github-skills-assembler.ts:95-121`
- **Fix applied:** Extracted inner loop body into `renderSkill(engine, srcDir, outputDir, name)` private method. `generateGroup` now 15 lines.

### M02: `generateGroup` has 5 parameters (MEDIUM) — FIXED
- **File:** `src/assembler/github-skills-assembler.ts:95-100`
- **Fix applied:** Moved `srcDir` resolution to `assemble` method. `generateGroup` now takes 4 params: `(engine, srcDir, outputDir, skillNames)`.

### L01: Unused `_config` parameter not documented in GithubPromptsAssembler (LOW)
- **File:** `src/assembler/github-prompts-assembler.ts:29`
- **Description:** `_config` is unused. While `GithubHooksAssembler` has a JSDoc comment explaining the `engine` parameter is accepted for API uniformity, `GithubPromptsAssembler` lacks a similar note for `_config`.
- **Fix suggestion:** Add a `@remarks` tag or inline comment noting the parameter is accepted for API uniformity.

### L02: Test file cleanup in `filterSkills` tests (LOW)
- **File:** `tests/node/assembler/github-skills-assembler.test.ts:77-97`
- **Description:** The `filterSkills_nonInfraGroup_returnsAllSkills` test creates its own temporary directory and does manual cleanup within the test body instead of using the shared `beforeEach`/`afterEach` lifecycle. Same pattern in lines 100-128, 131-155, 157-176, 178-197, 199-218.
- **Fix suggestion:** Move these tests into the `describe` block with shared lifecycle, or extract the temp dir management into a helper.

### L03: `cap` function redefined inline (LOW)
- **File:** `src/assembler/github-instructions-assembler.ts:88`
- **Description:** The `cap` (capitalize) utility is defined as a closure inside `buildStackSection`. If this pattern is needed elsewhere, it should be a module-level function. Currently it is only used in this one function, so this is acceptable but worth noting.
- **Fix suggestion:** No immediate action needed. If capitalize is needed elsewhere in future, extract to a shared utility.

## Verdict

All 6 GitHub assemblers are well-implemented, consistent with each other, and follow the established patterns of existing assemblers. The 2 medium issues in `GithubSkillsAssembler.generateGroup` (line count + parameter count) are minor violations that do not affect correctness or maintainability. Zero critical issues. All 88 tests pass with excellent coverage (100% lines, 96.66%+ branches on all new files). TypeScript compilation is clean. The implementation matches the plan exactly.

**Decision: GO (39/40)** — Medium issues M01 and M02 fixed in commit `4b230ef`.
