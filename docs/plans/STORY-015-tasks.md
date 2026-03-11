# Task Decomposition -- STORY-015: ReadmeAssembler

**Status:** PENDING
**Date:** 2026-03-11
**Blocked By:** STORY-005 (template engine), STORY-008 (assembler helpers) -- both complete
**Blocks:** STORY-016 (pipeline orchestrator)

---

## G1 -- Foundation (counting, detection, parsing helpers)

**Purpose:** Pure utility functions that scan the output directory to count artifacts and detect knowledge packs. No file writing. All functions are exported for direct testability.
**Dependencies:** `src/models.ts` (ProjectConfig), `node:fs`, `node:path`
**Compiles independently:** Yes -- no dependency on G2-G4 functions.

### T1.1 -- Create `readme-assembler.ts` with counting functions

- **File:** `src/assembler/readme-assembler.ts` (create)
- **What to implement:**
  - Module JSDoc header (same style as `skills-assembler.ts` and `github-agents-assembler.ts`)
  - Imports: `node:fs`, `node:path`, `ProjectConfig` and `DEFAULT_FOUNDATION` from `../models.js`, `getHookTemplateKey` and `LANGUAGE_COMMANDS` from `../domain/stack-mapping.js`
  - `countRules(outputDir: string): number` -- count `*.md` files in `outputDir/rules/`. Return 0 if dir missing.
  - `countSkills(outputDir: string): number` -- count `*/SKILL.md` files in `outputDir/skills/`. Return 0 if dir missing.
  - `countAgents(outputDir: string): number` -- count `*.md` files in `outputDir/agents/`. Return 0 if dir missing.
  - `countKnowledgePacks(outputDir: string): number` -- count skills where `isKnowledgePack()` returns true. Return 0 if dir missing.
  - `countHooks(outputDir: string): number` -- count all entries in `outputDir/hooks/`. Return 0 if dir missing.
  - `countSettings(outputDir: string): number` -- check existence of `settings.json` and `settings.local.json` in `outputDir`. Return 0, 1, or 2.
  - `countGithubFiles(githubDir: string): number` -- recursively count all files under `githubDir`. Use `fs.readdirSync` with `{ withFileTypes: true, recursive: true }` (Node 20+). Return 0 if dir missing.
  - `countGithubComponent(githubDir: string, component: string): number` -- count files directly under `githubDir/{component}/`. Return 0 if dir missing.
  - `countGithubSkills(githubDir: string): number` -- count `*/SKILL.md` in `githubDir/skills/`. Return 0 if dir missing.
- **Python reference:** Lines 120-131 (`_count_rules`, `_count_skills`, `_count_agents`), 288-310 (`_count_knowledge_packs`, `_count_hooks`, `_count_settings`), 371-431 (`_count_github_files`, `_count_github_component`, `_count_github_skills`)
- **Dependencies on other tasks:** None
- **Estimated complexity:** M (9 functions, each small but needs careful dir/file checking)

### T1.2 -- Add knowledge pack detection function

- **File:** `src/assembler/readme-assembler.ts` (append)
- **What to implement:**
  - `isKnowledgePack(skillMdPath: string): boolean` -- read SKILL.md content. Return `true` if content contains `"user-invocable: false"` OR any line starts with `"# Knowledge Pack"`. Return `false` otherwise.
- **Python reference:** Lines 194-201 (`_is_knowledge_pack`)
- **Dependencies on other tasks:** None
- **Estimated complexity:** S

### T1.3 -- Add rule parsing helpers

- **File:** `src/assembler/readme-assembler.ts` (append)
- **What to implement:**
  - `extractRuleNumber(filename: string): string` -- regex `/^(\d+)/` match on filename. Return matched group or empty string.
  - `extractRuleScope(filename: string): string` -- strip leading digits and hyphen (`/^\d+-/`), strip `.md` suffix, replace hyphens with spaces.
  - `extractSkillDescription(skillMdPath: string): string` -- read file, find line starting with `description:`, extract value after colon, strip quotes. Return empty string if not found.
- **Python reference:** Lines 157-166 (`_extract_rule_number`, `_extract_rule_scope`), 185-191 (`_extract_skill_description`)
- **Dependencies on other tasks:** None
- **Estimated complexity:** S

### Compilation checkpoint G1

```
npx tsc --noEmit   # zero errors with counting/detection/parsing functions
```

---

## G2 -- Table builders (markdown table generation functions)

**Purpose:** Functions that scan output directories and produce formatted markdown tables and sections. Depend on G1 counting/detection helpers.
**Dependencies:** G1 functions (counting, detection, parsing)
**Compiles independently:** Yes -- depends only on G1 functions in the same file.

### T2.1 -- Add rules and agents table builders

- **File:** `src/assembler/readme-assembler.ts` (append)
- **What to implement:**
  - `buildRulesTable(outputDir: string): string`
    1. Check `outputDir/rules/` exists. If not, return `"No rules configured."`.
    2. Glob `*.md` files, sort by name.
    3. If empty, return `"No rules configured."`.
    4. Build markdown table with headers `| # | File | Scope |` and `|---|------|-------|`.
    5. For each file, extract rule number and scope using G1 helpers.
    6. Return joined lines.
  - `buildAgentsTable(outputDir: string): string`
    1. Check `outputDir/agents/` exists. If not, return `"No agents configured."`.
    2. Glob `*.md` files, sort by name.
    3. If empty, return `"No agents configured."`.
    4. Build markdown table with headers `| Agent | File |` and `|-------|------|`.
    5. For each file, use `path.basename(file, ".md")` for agent name, file's basename for File column.
    6. Return joined lines.
- **Python reference:** Lines 141-154 (`_build_rules_table`), 204-215 (`_build_agents_table`)
- **Dependencies on other tasks:** T1.3 (extractRuleNumber, extractRuleScope)
- **Estimated complexity:** M

### T2.2 -- Add skills and knowledge packs table builders

- **File:** `src/assembler/readme-assembler.ts` (append)
- **What to implement:**
  - `buildSkillsTable(outputDir: string): string`
    1. Check `outputDir/skills/` exists. If not, return `"No skills configured."`.
    2. Glob `*/SKILL.md`, sort by parent dir name.
    3. For each SKILL.md: if `isKnowledgePack()` returns true, skip it.
    4. Extract skill name from parent dir, description via `extractSkillDescription`.
    5. Build table with headers `| Skill | Path | Description |` and `|-------|------|-------------|`.
    6. If no rows after filtering, return `"No skills configured."`.
    7. Return joined lines.
  - `buildKnowledgePacksTable(outputDir: string): string`
    1. Check `outputDir/skills/` exists. If not, return `"No knowledge packs configured."`.
    2. Glob `*/SKILL.md`, sort.
    3. For each: if `isKnowledgePack()` returns true, add row `| \`{name}\` | Referenced internally by agents |`.
    4. If no rows, return `"No knowledge packs configured."`.
    5. Build table with header `| Pack | Usage |\n|------|-------|`.
    6. Return header + newline + joined rows.
- **Python reference:** Lines 168-182 (`_build_skills_table`), 253-267 (`_build_knowledge_packs_table`)
- **Dependencies on other tasks:** T1.2 (isKnowledgePack), T1.3 (extractSkillDescription)
- **Estimated complexity:** M

### T2.3 -- Add hooks section and settings section builders

- **File:** `src/assembler/readme-assembler.ts` (append)
- **What to implement:**
  - `buildHooksSection(config: ProjectConfig): string`
    1. Call `getHookTemplateKey(config.language.name, config.framework.buildTool)`.
    2. If key is empty, return `"No hooks configured."`.
    3. Look up `LANGUAGE_COMMANDS[config.language.name + "-" + config.framework.buildTool]` for `fileExtension` and `compileCmd`.
    4. Return formatted markdown:
       ```
       ### Post-Compile Check

       - **Event:** `PostToolUse` (after `Write` or `Edit`)
       - **Script:** `.claude/hooks/post-compile-check.sh`
       - **Behavior:** When a `{ext}` file is modified, runs `{compileCmd}` automatically
       - **Purpose:** Catch compilation errors immediately after file changes
       ```
    5. Note: Python uses `_get_file_extension` and `_get_compile_command` as separate functions; in TypeScript inline the lookup from `LANGUAGE_COMMANDS`.
  - `buildSettingsSection(): string`
    1. Return static content matching Python's `_build_settings_section` exactly:
       ```
       ### settings.json

       Permissions are configured in `settings.json` under `permissions.allow`.
       This controls which Bash commands Claude Code can run without asking.

       ### settings.local.json

       Local overrides (gitignored). Use for personal preferences or team-specific tools.

       See the files directly for current configuration.
       ```
- **Python reference:** Lines 218-237 (`_build_hooks_section`), 270-285 (`_build_settings_section`), 239-250 (`_get_file_extension`, `_get_compile_command`)
- **Dependencies on other tasks:** None (uses imports from `stack-mapping.ts`)
- **Estimated complexity:** S

### T2.4 -- Add mapping table and generation summary builders

- **File:** `src/assembler/readme-assembler.ts` (append)
- **What to implement:**
  - `buildMappingTable(outputDir: string): string`
    1. Define 8-row hardcoded mapping array (`.claude/` <-> `.github/` equivalences).
    2. Build table with headers `| .claude/ | .github/ | Notes |` and `|----------|----------|-------|`.
    3. Append each row.
    4. Check if `outputDir/github/` exists and has files; if `countGithubFiles > 0`, append empty line + `**Total .github/ artifacts: {count}**`.
    5. Return joined lines.
  - `buildGenerationSummary(outputDir: string, config: ProjectConfig): string`
    1. Count all .claude components: rules, skills (minus KPs), KPs, agents, hooks, settings.
    2. Count all .github components: instructions, skills, agents, prompts, hooks.
    3. GitHub instructions count: `countGithubComponent("instructions")` + 1 if `copilot-instructions.md` exists.
    4. GitHub MCP: 1 if `copilot-mcp.json` exists, else 0.
    5. Build table with headers `| Component | Count |` and `|-----------|-------|`.
    6. Append 12 rows (Rules, Skills, KPs, Agents, Hooks, Settings for .claude; Instructions, Skills, Agents, Prompts, Hooks, MCP for .github).
    7. Append empty line + `Generated by \`ia-dev-env v${DEFAULT_FOUNDATION.version}\`.`
    8. Return joined lines.
- **Python reference:** Lines 313-368 (`_build_mapping_table`), 378-415 (`_build_generation_summary`)
- **Dependencies on other tasks:** T1.1 (all counting functions)
- **Estimated complexity:** L (complex logic, many counts, hardcoded table)

### Compilation checkpoint G2

```
npx tsc --noEmit   # zero errors with table builders
```

---

## G3 -- README generation (full and minimal mode functions)

**Purpose:** Top-level content generation functions that compose G1/G2 functions into complete README content.
**Dependencies:** G1 and G2 functions
**Compiles independently:** Yes -- depends only on G1/G2 functions in the same file.

### T3.1 -- Add `generateReadme` (full template mode)

- **File:** `src/assembler/readme-assembler.ts` (append)
- **What to implement:**
  - `generateReadme(config: ProjectConfig, outputDir: string, templatePath: string): string`
    1. Read template file content from `templatePath`.
    2. Replace 12 `{{PLACEHOLDER}}` tokens using `String.replace()` (NOT `TemplateEngine`):
       - `{{PROJECT_NAME}}` -> `config.project.name`
       - `{{RULES_COUNT}}` -> `String(countRules(outputDir))`
       - `{{SKILLS_COUNT}}` -> `String(countSkills(outputDir))`
       - `{{AGENTS_COUNT}}` -> `String(countAgents(outputDir))`
       - `{{RULES_TABLE}}` -> `buildRulesTable(outputDir)`
       - `{{SKILLS_TABLE}}` -> `buildSkillsTable(outputDir)`
       - `{{AGENTS_TABLE}}` -> `buildAgentsTable(outputDir)`
       - `{{HOOKS_SECTION}}` -> `buildHooksSection(config)`
       - `{{KNOWLEDGE_PACKS_TABLE}}` -> `buildKnowledgePacksTable(outputDir)`
       - `{{SETTINGS_SECTION}}` -> `buildSettingsSection()`
       - `{{MAPPING_TABLE}}` -> `buildMappingTable(outputDir)`
       - `{{GENERATION_SUMMARY}}` -> `buildGenerationSummary(outputDir, config)`
    3. Return the fully-replaced content.
  - **Critical:** Use `content.replace("{{TOKEN}}", value)` directly. Do NOT route through `TemplateEngine` -- the `{{DOUBLE_BRACES}}` pattern is distinct from the engine's `{SINGLE_BRACE}` pattern.
- **Python reference:** Lines 40-71 (`_generate_readme`)
- **Dependencies on other tasks:** T1.1, T2.1, T2.2, T2.3, T2.4
- **Estimated complexity:** M

### T3.2 -- Add `generateMinimalReadme` (fallback mode)

- **File:** `src/assembler/readme-assembler.ts` (append)
- **What to implement:**
  - `generateMinimalReadme(config: ProjectConfig): string`
    1. Build header:
       ```
       # .claude/ -- {config.project.name}

       This directory contains the Claude Code configuration for **{config.project.name}**.

       ```
       Note: Python uses em dash (U+2014 `\u2014`). TypeScript must match exactly.
    2. Build structure block (inline, not a separate function):
       - `## Structure\n\n` + fenced code block with directory tree.
       - Uses box-drawing characters matching Python output exactly.
    3. Build tips block (inline):
       - Format interfaces: `config.interfaces.map(i => i.type).join(" ")` or `"none"` if empty.
       - 6 bullet points with architecture style and interfaces.
    4. Concatenate header + structure + tips.
    5. Return result.
- **Python reference:** Lines 74-117 (`generate_minimal_readme`, `_build_structure_block`, `_build_tips_block`)
- **Dependencies on other tasks:** None (only uses `config`)
- **Estimated complexity:** M (exact string parity required with Python, including unicode characters)

### Compilation checkpoint G3

```
npx tsc --noEmit   # zero errors with README generation functions
```

---

## G4 -- ReadmeAssembler class implementation

**Purpose:** The main `ReadmeAssembler` class with the `assemble` method that ties everything together, following the established assembler pattern.
**Dependencies:** G3 functions (`generateReadme`, `generateMinimalReadme`)
**Compiles independently:** Yes -- depends only on functions in the same file.

### T4.1 -- Add `ReadmeAssembler` class

- **File:** `src/assembler/readme-assembler.ts` (append, near top of file after imports or as class declaration)
- **What to implement:**
  - `export class ReadmeAssembler`
  - JSDoc: `/** Generates README.md from template or minimal fallback. */`
  - Single method:
    ```typescript
    assemble(
      config: ProjectConfig,
      outputDir: string,
      resourcesDir: string,
      engine: TemplateEngine,
    ): string[]
    ```
  - Logic:
    1. Resolve `templatePath = path.join(resourcesDir, "readme-template.md")`.
    2. If template file exists (`fs.existsSync`), call `generateReadme(config, outputDir, templatePath)`.
    3. Else, call `generateMinimalReadme(config)`.
    4. Write content to `path.join(outputDir, "README.md")` with UTF-8 encoding.
    5. Return `[destPath]` (array with single path).
  - Note: `engine` parameter is accepted for API uniformity with other assemblers but is NOT used. Add JSDoc note explaining this.
  - Note: The Python version accepts `engine` as well and does not use it for README generation (Python uses `str.replace()` directly for `{{PLACEHOLDER}}` tokens).
- **Python reference:** Lines 16-37 (`ReadmeAssembler` class)
- **Dependencies on other tasks:** T3.1, T3.2
- **Estimated complexity:** S

### Compilation checkpoint G4

```
npx tsc --noEmit   # zero errors with ReadmeAssembler class
```

---

## G5 -- Barrel export

**Purpose:** Register the new assembler in the barrel export file so it is accessible from the `assembler` package.
**Dependencies:** G4 (ReadmeAssembler class must exist)

### T5.1 -- Update `src/assembler/index.ts`

- **File:** `src/assembler/index.ts` (modify)
- **What to implement:**
  - Append after the existing STORY-014 section:
    ```typescript
    // --- STORY-015: ReadmeAssembler ---
    export * from "./readme-assembler.js";
    ```
- **Dependencies on other tasks:** T4.1
- **Estimated complexity:** S

### Compilation checkpoint G5

```
npx tsc --noEmit   # zero errors with barrel export
```

---

## G6 -- Unit tests

**Purpose:** Comprehensive unit tests covering all exported functions and the ReadmeAssembler class. Organized by test groups matching G1-G4 implementation groups.
**Dependencies:** G1-G5 (all source code must compile)
**Pattern reference:** `tests/node/assembler/github-agents-assembler.test.ts` for structure, temp dir management, and helper conventions.

### T6.1 -- Create test file with infrastructure and helpers

- **File:** `tests/node/assembler/readme-assembler.test.ts` (create)
- **What to implement:**
  - Imports: `vitest` (`describe`, `it`, `expect`, `beforeEach`, `afterEach`), `node:fs`, `node:path`, `node:os` (`tmpdir`)
  - Import all exported functions from `../../../src/assembler/readme-assembler.js`
  - Import `TemplateEngine` from `../../../src/template-engine.js`
  - Import model classes: `ProjectConfig`, `ProjectIdentity`, `ArchitectureConfig`, `InterfaceConfig`, `LanguageConfig`, `FrameworkConfig`, `InfraConfig`
  - Helper: `buildConfig(overrides?)` -- accepts optional `archStyle`, `interfaces`, `language`, `buildTool`. Returns `ProjectConfig` with sensible defaults (typescript, npm, library arch).
  - Helper: `createRule(outputDir, filename)` -- creates a `.md` file in `outputDir/rules/`.
  - Helper: `createSkill(outputDir, name, description, isKP?)` -- creates `outputDir/skills/{name}/SKILL.md` with optional `user-invocable: false` frontmatter for KPs.
  - Helper: `createAgent(outputDir, name)` -- creates `outputDir/agents/{name}.md`.
  - Helper: `createHook(outputDir, name)` -- creates `outputDir/hooks/{name}`.
  - Helper: `createSettings(outputDir, includeLocal?)` -- creates `settings.json` and optionally `settings.local.json`.
  - Helper: `createGithubArtifacts(outputDir, structure)` -- creates files under `outputDir/github/` based on a `Record<string, string[]>` (dir -> filenames).
  - Helper: `createReadmeTemplate(resourcesDir)` -- copies `resources/readme-template.md` content to `resourcesDir/readme-template.md`.
  - `beforeEach`: create `tmpDir` via `fs.mkdtempSync(path.join(tmpdir(), "readme-asm-test-"))`, set up `resourcesDir` and `outputDir` subdirectories.
  - `afterEach`: clean up with `fs.rmSync(tmpDir, { recursive: true, force: true })`.
- **Dependencies on other tasks:** G1-G5 all complete
- **Estimated complexity:** M

### T6.2 -- Tests for counting functions (G1 coverage)

- **File:** `tests/node/assembler/readme-assembler.test.ts` (append)
- **What to implement:** `describe("counting functions")` block with tests:
  1. `countRules_withMdFiles_returnsCorrectCount` -- create 3 rule files, expect 3.
  2. `countRules_emptyDir_returnsZero` -- create empty `rules/` dir, expect 0.
  3. `countRules_dirMissing_returnsZero` -- no `rules/` dir, expect 0.
  4. `countSkills_withSkillMdFiles_returnsCount` -- create 2 skills, expect 2.
  5. `countSkills_dirMissing_returnsZero`
  6. `countAgents_withMdFiles_returnsCount` -- create 3 agents, expect 3.
  7. `countAgents_dirMissing_returnsZero`
  8. `countKnowledgePacks_detectsUserInvocableFalse` -- create 2 KPs + 1 regular skill, expect 2.
  9. `countKnowledgePacks_excludesRegularSkills` -- create 3 regular skills, expect 0.
  10. `countKnowledgePacks_detectsKnowledgePackHeading` -- SKILL.md with `# Knowledge Pack` heading, expect detected.
  11. `countHooks_countsAllEntries` -- create 2 hook files, expect 2.
  12. `countHooks_dirMissing_returnsZero`
  13. `countSettings_bothExist_returnsTwo` -- both settings files exist, expect 2.
  14. `countSettings_noneExist_returnsZero`
  15. `countSettings_oneExists_returnsOne` -- only `settings.json`, expect 1.
  16. `countGithubFiles_recursiveCounting` -- create nested structure, expect total file count.
  17. `countGithubFiles_dirMissing_returnsZero`
  18. `countGithubComponent_countsDirectFiles` -- create files in `github/agents/`, expect correct count.
  19. `countGithubSkills_countsSkillMdInSubdirs` -- create `github/skills/x/SKILL.md` structures, expect count.
- **Dependencies on other tasks:** T6.1 (helpers)
- **Estimated complexity:** M

### T6.3 -- Tests for detection and parsing functions (G1 coverage)

- **File:** `tests/node/assembler/readme-assembler.test.ts` (append)
- **What to implement:** `describe("detection and parsing")` block with tests:
  1. `isKnowledgePack_userInvocableFalse_returnsTrue` -- SKILL.md with `user-invocable: false`.
  2. `isKnowledgePack_knowledgePackHeading_returnsTrue` -- SKILL.md with `# Knowledge Pack` heading.
  3. `isKnowledgePack_regularSkill_returnsFalse` -- normal SKILL.md content.
  4. `isKnowledgePack_userInvocableTrue_returnsFalse` -- `user-invocable: true` should NOT match.
  5. `extractRuleNumber_numberedFile_returnsNumber` -- `"01-project-identity.md"` -> `"01"`.
  6. `extractRuleNumber_multiDigit_returnsAllDigits` -- `"123-many-digits.md"` -> `"123"`.
  7. `extractRuleNumber_unnumberedFile_returnsEmpty` -- `"no-number.md"` -> `""`.
  8. `extractRuleScope_removesNumberAndExtension` -- `"01-project-identity.md"` -> `"project identity"`.
  9. `extractRuleScope_noNumber_removesExtension` -- `"coding-standards.md"` -> `"coding standards"`.
  10. `extractSkillDescription_findsDescriptionField` -- SKILL.md with `description: "My skill"`.
  11. `extractSkillDescription_noDescription_returnsEmpty` -- SKILL.md without description line.
  12. `extractSkillDescription_stripsQuotes` -- description with single or double quotes.
- **Dependencies on other tasks:** T6.1 (helpers)
- **Estimated complexity:** M

### T6.4 -- Tests for table builders (G2 coverage)

- **File:** `tests/node/assembler/readme-assembler.test.ts` (append)
- **What to implement:** `describe("table builders")` block with tests:
  1. `buildRulesTable_withRules_formatsMarkdownTable` -- 2 rules, verify table header and rows.
  2. `buildRulesTable_noRules_returnsMessage` -- empty dir, expect `"No rules configured."`.
  3. `buildRulesTable_dirMissing_returnsMessage`
  4. `buildSkillsTable_withSkills_formatsTable` -- 2 regular skills, verify table structure.
  5. `buildSkillsTable_excludesKnowledgePacks` -- mix of regular + KP, KPs not in output.
  6. `buildSkillsTable_noSkills_returnsMessage`
  7. `buildAgentsTable_withAgents_formatsTable` -- 2 agents, verify table structure.
  8. `buildAgentsTable_noAgents_returnsMessage`
  9. `buildKnowledgePacksTable_withKps_formatsTable` -- 2 KPs, verify table structure.
  10. `buildKnowledgePacksTable_noKps_returnsMessage`
  11. `buildHooksSection_typescriptNpm_returnsPostCompileCheck` -- config with typescript/npm, verify `.ts` extension and `npx --no-install tsc --noEmit` command.
  12. `buildHooksSection_noHookKey_returnsNoHooksMessage` -- config with python/pip (empty hook key), expect `"No hooks configured."`.
  13. `buildSettingsSection_returnsStaticContent` -- verify contains `settings.json` and `settings.local.json`.
  14. `buildMappingTable_withGithubDir_includesTotal` -- create github artifacts, verify total line.
  15. `buildMappingTable_noGithubDir_omitsTotal` -- no github dir, no total line.
  16. `buildMappingTable_contains8Rows` -- verify 8 data rows (+ 2 header rows).
  17. `buildGenerationSummary_countsAllComponents` -- create fixtures for all component types, verify counts.
  18. `buildGenerationSummary_includesVersionStamp` -- verify `Generated by \`ia-dev-env v0.1.0\`.` line.
  19. `buildGenerationSummary_skillsCountExcludesKps` -- create 3 skills (1 KP), Skills (.claude) row shows 2.
  20. `buildGenerationSummary_githubInstructionsIncludesGlobal` -- create `copilot-instructions.md` + 3 instruction files, Instructions (.github) row shows 4.
- **Dependencies on other tasks:** T6.1 (helpers)
- **Estimated complexity:** L (20 tests, many require complex fixture setup)

### T6.5 -- Tests for README generation functions (G3 coverage)

- **File:** `tests/node/assembler/readme-assembler.test.ts` (append)
- **What to implement:** `describe("README generation")` block with tests:

  **Full mode (`generateReadme`):**
  1. `generateReadme_replacesAllPlaceholders` -- verify no `{{...}}` tokens remain in output.
  2. `generateReadme_containsProjectName` -- output contains config project name.
  3. `generateReadme_containsRulesTable` -- output contains rule table headers.
  4. `generateReadme_containsSkillsTable` -- output contains skill table headers.
  5. `generateReadme_containsAgentsTable` -- output contains agent table headers.
  6. `generateReadme_containsKnowledgePacksTable` -- output contains KP table or "No knowledge packs" message.
  7. `generateReadme_containsHooksSection` -- output contains hooks section.
  8. `generateReadme_containsSettingsSection` -- output contains settings section.
  9. `generateReadme_containsMappingTable` -- output contains mapping table headers.
  10. `generateReadme_containsGenerationSummary` -- output contains generation summary table.

  **Minimal mode (`generateMinimalReadme`):**
  11. `generateMinimalReadme_containsProjectName` -- output contains project name in header and body.
  12. `generateMinimalReadme_containsStructureBlock` -- output contains `## Structure` and code block.
  13. `generateMinimalReadme_containsTipsBlock` -- output contains `## Tips`.
  14. `generateMinimalReadme_tipsContainArchStyle` -- tips mention the architecture style.
  15. `generateMinimalReadme_tipsContainInterfaces` -- tips mention the interfaces.
  16. `generateMinimalReadme_noInterfaces_showsNone` -- config with no interfaces, tips show `none`.
- **Dependencies on other tasks:** T6.1 (helpers, especially `createReadmeTemplate`)
- **Estimated complexity:** L (requires full fixture setup with rules, skills, agents, hooks, settings, github artifacts)

### T6.6 -- Tests for ReadmeAssembler class (G4 coverage)

- **File:** `tests/node/assembler/readme-assembler.test.ts` (append)
- **What to implement:** `describe("ReadmeAssembler")` block with tests:
  1. `assemble_templateExists_generatesFullReadme` -- provide template in resourcesDir, verify full README written.
  2. `assemble_templateMissing_generatesMinimalReadme` -- no template, verify minimal README written.
  3. `assemble_writesReadmeMd` -- verify `README.md` file exists in outputDir.
  4. `assemble_returnsArrayWithSinglePath` -- return value is `[destPath]`.
  5. `assemble_fullReadme_countsMatchFilesystem` -- create known fixtures, verify counts in output match.
  6. `assemble_fullReadme_knowledgePacksDetected` -- create KP skills, verify they appear in KPs table (not skills table).
  7. `assemble_engineNotUsed_noError` -- pass engine but verify it works without engine doing anything.
- **Dependencies on other tasks:** T6.1 (helpers)
- **Estimated complexity:** M

### Compilation checkpoint G6

```
npx vitest run tests/node/assembler/readme-assembler.test.ts
```

---

## G7 -- Verification (compilation, coverage, parity checks)

**Purpose:** Final verification that all code compiles, tests pass, coverage thresholds are met, and output is consistent with Python behavior.
**Dependencies:** G1-G6 all complete.

### T7.1 -- Full compilation check

```
npx tsc --noEmit   # zero errors across entire project including readme-assembler
```

- **Dependencies on other tasks:** G1-G5
- **Estimated complexity:** S

### T7.2 -- Run all ReadmeAssembler tests

```
npx vitest run tests/node/assembler/readme-assembler.test.ts
```

- **Dependencies on other tasks:** G6
- **Estimated complexity:** S

### T7.3 -- Coverage verification

```
npx vitest run --coverage tests/node/assembler/readme-assembler.test.ts
```

- **Expected:** >= 95% line coverage, >= 90% branch coverage on `src/assembler/readme-assembler.ts`.
- **Coverage strategy:**
  - Every counting function tested with: dir exists with files, dir exists but empty, dir missing.
  - Knowledge pack detection tested with: `user-invocable: false`, `# Knowledge Pack` heading, regular skill, `user-invocable: true`.
  - Table builders tested with: populated data, empty data, missing dirs.
  - Hooks section tested with: typescript/npm (has key), python/pip (empty key).
  - Generation summary tested with: all components present, partial components, version stamp.
  - Both README modes tested: full (template exists) and minimal (template missing).
- **Dependencies on other tasks:** G6
- **Estimated complexity:** S

### T7.4 -- Python parity checks

- **What to verify:**
  1. Full README output for a standard typescript/npm/library config matches Python output (compare key sections).
  2. Minimal README output matches Python output (exact string match including unicode em dashes and box-drawing characters).
  3. Mapping table contains exactly 8 data rows.
  4. Generation summary contains exactly 12 component rows.
  5. `extractRuleNumber("01-project-identity.md")` returns `"01"` (not `"1"`).
  6. `extractRuleScope("01-project-identity.md")` returns `"project identity"` (spaces, no extension).
  7. `buildHooksSection` for typescript/npm returns `".ts"` extension and `"npx --no-install tsc --noEmit"` command.
  8. `isKnowledgePack` with `"user-invocable: false"` in middle of file returns `true`.
  9. `countGithubFiles` correctly counts recursively (files only, not directories).
  10. `buildGenerationSummary` uses `DEFAULT_FOUNDATION.version` (not hardcoded `"0.1.0"`).
- **Dependencies on other tasks:** G6
- **Estimated complexity:** M

### T7.5 -- Assembler signature consistency check

- **What to verify:**
  1. `ReadmeAssembler.assemble(config, outputDir, resourcesDir, engine)` matches the established 4-parameter signature.
  2. Return type is `string[]` (array of generated file paths).
  3. The `engine` parameter is accepted but not used (API uniformity, not `AssembleResult`).
  4. No cross-assembler dependencies (ReadmeAssembler does not import other assemblers).
  5. Barrel export in `index.ts` exposes all exported symbols from `readme-assembler.ts`.
- **Dependencies on other tasks:** G5
- **Estimated complexity:** S

---

## Summary Table

| Group | Purpose | Files to Create | Files to Modify | Tasks | Test Cases | Complexity |
|-------|---------|----------------|----------------|-------|------------|------------|
| G1 | Counting, detection, parsing | 1 (src) | 0 | 3 | 0 | M |
| G2 | Table builders | 0 | 1 (src append) | 4 | 0 | L |
| G3 | README generation | 0 | 1 (src append) | 2 | 0 | M |
| G4 | ReadmeAssembler class | 0 | 1 (src append) | 1 | 0 | S |
| G5 | Barrel export | 0 | 1 (index.ts) | 1 | 0 | S |
| G6 | Unit tests | 1 (test) | 0 | 6 | ~74 | L |
| G7 | Verification | 0 | 0 | 5 | 0 (verification) | M |
| **Total** | | **2 new files** | **2 modified** | **22 tasks** | **~74 test cases** | |

## File Inventory

### Source files (1 new)

| File | Exports |
|------|---------|
| `src/assembler/readme-assembler.ts` | `ReadmeAssembler`, `generateReadme`, `generateMinimalReadme`, `countRules`, `countSkills`, `countAgents`, `countKnowledgePacks`, `countHooks`, `countSettings`, `countGithubFiles`, `countGithubComponent`, `countGithubSkills`, `isKnowledgePack`, `extractRuleNumber`, `extractRuleScope`, `extractSkillDescription`, `buildRulesTable`, `buildSkillsTable`, `buildAgentsTable`, `buildKnowledgePacksTable`, `buildHooksSection`, `buildSettingsSection`, `buildMappingTable`, `buildGenerationSummary` |

### Test files (1 new)

| File | Test count |
|------|-----------|
| `tests/node/assembler/readme-assembler.test.ts` | ~74 |

### Modified files (1)

| File | Change |
|------|--------|
| `src/assembler/index.ts` | Add 2 barrel export lines (comment + re-export) |

## Key Implementation Notes

1. **`{{DOUBLE_BRACES}}` vs `{SINGLE_BRACE}`:** The README template uses `{{PLACEHOLDER}}` tokens. These are replaced with `String.replace()` directly. Do NOT route through `TemplateEngine` -- the engine's regex `\{(\w+)\}` would partially match and corrupt `{{TOKENS}}`.
2. **`DEFAULT_FOUNDATION.version` replaces Python `__version__`:** Import from `src/models.ts`. Never hardcode the version string.
3. **Unicode characters in minimal README:** The structure block uses box-drawing characters (U+251C, U+2500, U+2502, U+2514) and the header uses em dash (U+2014). These must match Python output exactly.
4. **`engine` parameter accepted but unused:** For API uniformity with the 13 other assemblers. Add JSDoc comment explaining this.
5. **This assembler must run last in the pipeline:** It scans output from all preceding assemblers. Pipeline ordering is enforced in STORY-016.
6. **GitHub directory is `github/` not `.github/`:** The output directory uses `github/` without the leading dot. The pipeline or caller handles renaming.
7. **Skills count in generation summary excludes KPs:** `Skills (.claude)` row = `countSkills - countKnowledgePacks`.
8. **GitHub instructions count includes global:** `Instructions (.github)` = `countGithubComponent("instructions")` + 1 if `copilot-instructions.md` exists.
9. **Sorting:** Rules, skills, and agents are sorted by filename (lexicographic) to match Python's `sorted(dir.glob(...))` behavior.
10. **Empty vs missing dirs:** All counting functions return 0 for both missing directories and empty directories. Table builders return "No X configured." messages for both cases.
