# Implementation Plan — STORY-014: GitHub Assemblers (Instructions, MCP, Skills, Agents, Hooks, Prompts)

## Story Summary

Migrate 6 GitHub assemblers from Python to TypeScript. These assemblers generate all `.github/` artifacts for GitHub Copilot integration: `copilot-instructions.md`, contextual instructions, `copilot-mcp.json`, skills, agents, hooks, and prompts.

**Blocked by:** STORY-005 (template engine), STORY-006 (domain mappings), STORY-008 (assembler helpers) — all complete.
**Blocks:** STORY-016.

---

## 1. Affected Layers and Components

| Layer | Component | Action | Path |
|-------|-----------|--------|------|
| assembler | GithubInstructionsAssembler | **Create** | `src/assembler/github-instructions-assembler.ts` |
| assembler | GithubMcpAssembler | **Create** | `src/assembler/github-mcp-assembler.ts` |
| assembler | GithubSkillsAssembler | **Create** | `src/assembler/github-skills-assembler.ts` |
| assembler | GithubAgentsAssembler | **Create** | `src/assembler/github-agents-assembler.ts` |
| assembler | GithubHooksAssembler | **Create** | `src/assembler/github-hooks-assembler.ts` |
| assembler | GithubPromptsAssembler | **Create** | `src/assembler/github-prompts-assembler.ts` |
| assembler | Barrel export | **Modify** | `src/assembler/index.ts` |
| assembler | Conditions (hasAnyInterface) | Read-only (already exists) | `src/assembler/conditions.ts` |
| assembler | Copy helpers | Read-only (already exists) | `src/assembler/copy-helpers.ts` |
| models | ProjectConfig, McpServerConfig, McpConfig | Read-only (already exists) | `src/models.ts` |
| template-engine | TemplateEngine (replacePlaceholders, renderTemplate) | Read-only (already exists) | `src/template-engine.ts` |
| resources | GitHub templates (instructions, skills, agents, hooks, prompts) | Read-only (already exists) | `resources/github-*-templates/` |
| tests | GithubInstructionsAssembler tests | **Create** | `tests/node/assembler/github-instructions-assembler.test.ts` |
| tests | GithubMcpAssembler tests | **Create** | `tests/node/assembler/github-mcp-assembler.test.ts` |
| tests | GithubSkillsAssembler tests | **Create** | `tests/node/assembler/github-skills-assembler.test.ts` |
| tests | GithubAgentsAssembler tests | **Create** | `tests/node/assembler/github-agents-assembler.test.ts` |
| tests | GithubHooksAssembler tests | **Create** | `tests/node/assembler/github-hooks-assembler.test.ts` |
| tests | GithubPromptsAssembler tests | **Create** | `tests/node/assembler/github-prompts-assembler.test.ts` |

---

## 2. New Classes/Interfaces to Create

### 2.1 `src/assembler/github-instructions-assembler.ts` — GithubInstructionsAssembler

**Purpose:** Generate `github/copilot-instructions.md` (global) and `github/instructions/*.instructions.md` (contextual).

```
export class GithubInstructionsAssembler {
  assemble(config, outputDir, resourcesDir, engine): string[]
  private generateGlobal(config, githubDir): string
  private generateContextual(engine, resourcesDir, instructionsDir): string[]
}

// Module-level pure function (exported for testing):
export function buildCopilotInstructions(config: ProjectConfig): string
```

**Key logic:**
1. Create `${outputDir}/github/` and `${outputDir}/github/instructions/` directories.
2. **Global file:** Build `copilot-instructions.md` programmatically from `config`:
   - Project identity section (name, architecture, DDD, event-driven, interfaces, language, framework).
   - Technology stack table (architecture, language, framework, build tool, container, orchestrator, resilience, native build, smoke tests, contract tests).
   - Constraints section (cloud-agnostic, stateless, externalized config).
   - Contextual instructions reference list.
   - Interface types are uppercased for `rest` and `grpc`, otherwise used as-is.
   - Framework version appended with leading space only if non-empty.
3. **Contextual files:** For each of `["domain", "coding-standards", "architecture", "quality-gates"]`:
   - Read template from `${resourcesDir}/github-instructions-templates/${name}.md`.
   - Apply `engine.replacePlaceholders(content)`.
   - Write to `${outputDir}/github/instructions/${name}.instructions.md`.
   - If template directory or individual template file is missing, skip with warning (log only, no error).
4. Return array of all generated file paths.

**Python parity notes:**
- Python uses `config.framework.version` with a leading space. TypeScript: same logic.
- Python uses `config.architecture.domain_driven` (snake_case). TypeScript: `config.architecture.domainDriven` (camelCase).
- Python joins `\n` and appends trailing `\n`. TypeScript: identical.
- Boolean formatting: Python uses `str(bool).lower()` producing `"true"/"false"`. TypeScript: same via `String(bool).toLowerCase()` or template literal.

### 2.2 `src/assembler/github-mcp-assembler.ts` — GithubMcpAssembler

**Purpose:** Generate `github/copilot-mcp.json` if MCP servers are configured, with env var validation.

```
export class GithubMcpAssembler {
  assemble(config, outputDir, resourcesDir, engine): AssembleResult
}

// Module-level pure functions (exported for testing):
export function warnLiteralEnvValues(
  servers: readonly McpServerConfig[]
): string[]
export function buildCopilotMcpDict(
  config: ProjectConfig
): Record<string, unknown>
```

**Key logic:**
1. If `config.mcp.servers` is empty, return `{ files: [], warnings: [] }`.
2. Call `warnLiteralEnvValues(config.mcp.servers)` — for each server, if any env value does not start with `$`, collect a warning string.
3. Create `${outputDir}/github/` directory.
4. Build MCP dict: `{ mcpServers: { [server.id]: { url, capabilities?, env? } } }`.
   - `capabilities` included only if non-empty array.
   - `env` included only if non-empty object.
5. Write `copilot-mcp.json` with `JSON.stringify(dict, null, 2) + "\n"`.
6. Return `{ files: [destPath], warnings: [...collectedWarnings] }`.

**Return type:** Uses `AssembleResult` (from `rules-assembler.ts`) to carry both files and warnings, following the pattern established by `AgentsAssembler`. Python used `logger.warning()` for env validation, but TypeScript should return warnings in the result to maintain testability without log capture.

**Python parity notes:**
- Python `_build_copilot_mcp_dict` creates `list(server.capabilities)` and `dict(server.env)`. TypeScript: spread operator `[...server.capabilities]` and `{ ...server.env }`.
- Python logs warnings via `logger.warning()`. TypeScript: collect into `warnings[]` array.

### 2.3 `src/assembler/github-skills-assembler.ts` — GithubSkillsAssembler

**Purpose:** Generate `.github/skills/` with 7 skill groups and conditional infrastructure filtering.

```
export class GithubSkillsAssembler {
  assemble(config, outputDir, resourcesDir, engine): string[]
  private filterSkills(config, group, skillNames): string[]
  private generateGroup(engine, resourcesDir, outputDir, group, skillNames): string[]
}

// Module-level constants (exported for testing):
export const SKILL_GROUPS: Record<string, readonly string[]>
export const INFRA_SKILL_CONDITIONS: Record<string, (config: ProjectConfig) => boolean>
```

**Key logic:**
1. Iterate over 7 `SKILL_GROUPS`: `story`, `dev`, `review`, `testing`, `infrastructure`, `knowledge-packs`, `git-troubleshooting`.
2. For `infrastructure` group only, apply `INFRA_SKILL_CONDITIONS` filtering:
   - `setup-environment`: `config.infrastructure.orchestrator !== "none"`.
   - `k8s-deployment`: `config.infrastructure.orchestrator === "kubernetes"`.
   - `k8s-kustomize`: `config.infrastructure.templating === "kustomize"`.
   - `dockerfile`: `config.infrastructure.container !== "none"`.
   - `iac-terraform`: `config.infrastructure.iac === "terraform"`.
3. For each surviving skill name:
   - Read template from `${resourcesDir}/github-skills-templates/${group}/${name}.md`.
   - Apply `engine.replacePlaceholders(content)`.
   - Write to `${outputDir}/github/skills/${name}/SKILL.md`.
   - If template directory or file is missing, skip (no error).
4. Return array of all generated file paths.

**Python parity notes:**
- Python uses lambda functions for conditions. TypeScript: arrow functions stored in a constant record.
- Python constructor takes `resources_dir`. TypeScript: passed per-call following established pattern.

### 2.4 `src/assembler/github-agents-assembler.ts` — GithubAgentsAssembler

**Purpose:** Generate `.github/agents/*.agent.md` with core, conditional, and developer agents.

```
export class GithubAgentsAssembler {
  assemble(config, outputDir, resourcesDir, engine): AssembleResult
  private assembleCore(resourcesDir, agentsDir, engine): string[]
  private assembleConditional(config, resourcesDir, agentsDir, engine, warnings): string[]
  private assembleDeveloper(config, resourcesDir, agentsDir, engine): string | null
  private renderAgent(template, agentsDir, engine): string
}

// Module-level pure functions (exported for testing):
export function selectGithubConditionalAgents(config: ProjectConfig): string[]
```

**Key logic:**
1. Create `${outputDir}/github/agents/` directory.
2. **Core agents:** Scan `${resourcesDir}/github-agents-templates/core/` for all files, sorted. For each file:
   - Read content, apply `engine.replacePlaceholders()`.
   - Write to `${outputDir}/github/agents/${stem}.agent.md`.
3. **Conditional agents:** Evaluate config to select conditional agents:
   - `devops-engineer.md`: if any of `container`, `orchestrator`, `iac`, `serviceMesh` is not `"none"`.
   - `api-engineer.md`: if config has any interface of type `rest`, `grpc`, or `graphql` (using `hasAnyInterface` from `conditions.ts`).
   - `event-engineer.md`: if `config.architecture.eventDriven` or has `event-consumer`/`event-producer` interfaces.
   - Read from `${resourcesDir}/github-agents-templates/conditional/`, render, write to agents dir.
4. **Developer agent:** Derive `${config.language.name}-developer.md`. Read from `${resourcesDir}/github-agents-templates/developers/`, render, write.
5. Output extension is `.agent.md` (GitHub convention), not `.md` (Claude convention).
6. Return `{ files, warnings }`.

**Python parity notes:**
- Python output uses `.agent.md` extension. The `_render_agent` method builds `${agent_name}${AGENT_MD_EXTENSION}` where stem is the template filename without extension.
- Conditional selection logic is identical to Python's `_select_infra_agents`, `_select_interface_agents`, `_select_event_agents`.
- Reuses `hasAnyInterface` from `src/assembler/conditions.ts` (already migrated in STORY-008).

### 2.5 `src/assembler/github-hooks-assembler.ts` — GithubHooksAssembler

**Purpose:** Copy 3 hook JSON templates to `.github/hooks/`.

```
export class GithubHooksAssembler {
  assemble(config, outputDir, resourcesDir, engine): string[]
}

// Module-level constant (exported for testing):
export const GITHUB_HOOK_TEMPLATES: readonly string[]
```

**Key logic:**
1. Resolve `${resourcesDir}/github-hooks-templates/` directory. If missing, return `[]`.
2. Create `${outputDir}/github/hooks/` directory.
3. For each of the 3 templates (`post-compile-check.json`, `pre-commit-lint.json`, `session-context-loader.json`):
   - Copy file from templates directory to output using `fs.copyFileSync`.
   - If source file is missing, skip with warning (no error).
4. Return array of generated paths.

**Python parity notes:**
- Python uses `shutil.copy2` (preserves metadata). TypeScript: `fs.copyFileSync` is sufficient since these are JSON files with no special permissions.
- No template placeholder replacement — files are copied verbatim. The `engine` parameter is accepted for API uniformity but not used.

### 2.6 `src/assembler/github-prompts-assembler.ts` — GithubPromptsAssembler

**Purpose:** Render 4 Nunjucks prompt templates and write to `.github/prompts/`.

```
export class GithubPromptsAssembler {
  assemble(config, outputDir, resourcesDir, engine): string[]
}

// Module-level constant (exported for testing):
export const GITHUB_PROMPT_TEMPLATES: readonly string[]
```

**Key logic:**
1. Resolve `${resourcesDir}/github-prompts-templates/` directory. If missing, return `[]`.
2. Create `${outputDir}/github/prompts/` directory.
3. For each of the 4 templates:
   - `new-feature.prompt.md.j2`
   - `decompose-spec.prompt.md.j2`
   - `code-review.prompt.md.j2`
   - `troubleshoot.prompt.md.j2`
4. For each template:
   - If source file is missing, skip.
   - Derive output name by removing `.j2` suffix (e.g., `new-feature.prompt.md`).
   - Render via `engine.renderTemplate("github-prompts-templates/" + templateName)` — this uses full Nunjucks rendering (not just placeholder replacement), since Python uses `engine.render_template()`.
   - Write rendered content to `${outputDir}/github/prompts/${outputName}`.
5. Return array of generated paths.

**Python parity notes:**
- Python calls `engine.render_template(Path(TEMPLATES_DIR_NAME) / template_name)`. TypeScript: `engine.renderTemplate("github-prompts-templates/" + templateName)` using forward slash (Nunjucks handles path resolution).
- Python uses `removesuffix(".j2")`. TypeScript: `templateName.replace(/\.j2$/, "")` or `.slice(0, -3)`.
- This is the only GitHub assembler that uses full Nunjucks rendering (not just `replacePlaceholders`). All other GitHub assemblers use `replacePlaceholders` for legacy `{placeholder}` syntax.

---

## 3. Existing Classes to Modify

### 3.1 `src/assembler/index.ts` — Add barrel exports

Add at the end:
```typescript
// --- STORY-014: GitHub Assemblers ---
export * from "./github-instructions-assembler.js";
export * from "./github-mcp-assembler.js";
export * from "./github-skills-assembler.js";
export * from "./github-agents-assembler.js";
export * from "./github-hooks-assembler.js";
export * from "./github-prompts-assembler.js";
```

No other existing files require modification.

---

## 4. Dependency Direction Validation

```
github-instructions-assembler.ts ──imports──> models.ts (ProjectConfig)
                                 ──imports──> template-engine.ts (TemplateEngine)

github-mcp-assembler.ts ──imports──> models.ts (ProjectConfig, McpServerConfig)
                         ──imports──> template-engine.ts (TemplateEngine)
                         ──imports──> assembler/rules-assembler.ts (AssembleResult type)

github-skills-assembler.ts ──imports──> models.ts (ProjectConfig)
                            ──imports──> template-engine.ts (TemplateEngine)

github-agents-assembler.ts ──imports──> models.ts (ProjectConfig)
                            ──imports──> template-engine.ts (TemplateEngine)
                            ──imports──> assembler/conditions.ts (hasAnyInterface)
                            ──imports──> assembler/rules-assembler.ts (AssembleResult type)

github-hooks-assembler.ts ──imports──> models.ts (ProjectConfig)
                           ──imports──> template-engine.ts (TemplateEngine)

github-prompts-assembler.ts ──imports──> models.ts (ProjectConfig)
                             ──imports──> template-engine.ts (TemplateEngine)
```

**Validated:** All 6 assemblers depend only on models and the template engine (both in the core layer) plus existing assembler utilities. No cross-assembler dependencies between the 6 new modules. No domain layer modifications. No framework or adapter imports. Dependencies point inward.

---

## 5. Integration Points

### 5.1 Template Engine (read-only)

- `GithubInstructionsAssembler` uses `engine.replacePlaceholders()` for contextual instruction templates.
- `GithubSkillsAssembler` uses `engine.replacePlaceholders()` for skill templates.
- `GithubAgentsAssembler` uses `engine.replacePlaceholders()` for agent templates.
- `GithubPromptsAssembler` uses `engine.renderTemplate()` for full Nunjucks rendering of `.j2` templates.
- `GithubMcpAssembler` and `GithubHooksAssembler` accept `engine` for API uniformity but do not use it.

### 5.2 Conditions module (read-only)

- `GithubAgentsAssembler` imports `hasAnyInterface` from `src/assembler/conditions.ts` (already migrated in STORY-008) for interface-based agent selection.

### 5.3 AssembleResult type (read-only)

- `GithubMcpAssembler` and `GithubAgentsAssembler` return `AssembleResult` from `src/assembler/rules-assembler.ts` to carry warnings alongside file paths.

### 5.4 Resources (read-only)

- `resources/github-instructions-templates/` — 4 contextual instruction templates.
- `resources/github-skills-templates/` — 7 group subdirectories with skill templates.
- `resources/github-agents-templates/` — `core/`, `conditional/`, `developers/` subdirectories.
- `resources/github-hooks-templates/` — 3 JSON hook templates.
- `resources/github-prompts-templates/` — 4 `.j2` Nunjucks prompt templates.

### 5.5 Pipeline integration (future STORY-016)

All 6 assemblers follow the established `assemble(config, outputDir, resourcesDir, engine)` signature. The pipeline orchestrator will call each in sequence.

---

## 6. Database Changes

None. This story is pure file generation logic.

---

## 7. API Changes

None. These are internal assembler modules with no external API surface.

---

## 8. Event Changes

None. No event-driven components involved.

---

## 9. Configuration Changes

None. The assemblers read from existing `resources/` templates and the already-defined `ProjectConfig` model (including `McpConfig`, `McpServerConfig`, `InfraConfig`). No new configuration fields are introduced.

---

## 10. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Boolean formatting mismatch in copilot-instructions.md | Medium | Medium | Python uses `str(bool).lower()` producing `"true"/"false"`. TypeScript must use same approach. Unit test with exact string comparison against expected output. |
| Nunjucks template path separator difference | Low | Medium | Python Jinja2 uses `Path(dir) / name`. Nunjucks uses forward-slash strings. Verify with `engine.renderTemplate("github-prompts-templates/" + name)`. |
| MCP env var warning message format difference | Low | Low | Python uses `logger.warning()`. TypeScript collects into `warnings[]` array. Verify warning strings contain server id and env key. |
| Missing resources directories in test environments | Low | Low | All assemblers gracefully handle missing template directories by returning empty arrays. Test both present and absent scenarios. |
| Interface type formatting (uppercase for rest/grpc) | Low | Medium | Python uppercases `rest` and `grpc` in interface list. Must replicate exactly in `buildCopilotInstructions`. Test with mixed interface types. |
| `removesuffix` not available in TypeScript | Low | Low | Python uses `str.removesuffix(".j2")`. TypeScript: use `replace(/\.j2$/, "")` or string slice. |
| GitHub agent `.agent.md` extension vs Claude `.md` extension | Low | High | Must ensure output uses `.agent.md` for GitHub agents (not `.md`). Verify in tests that output filenames end with `.agent.md`. |
| Infra skill conditions not matching Python lambda behavior | Low | Medium | Extract conditions into a typed record. Test each condition individually with edge configs. |
| GithubSkillsAssembler skill group ordering | Low | Low | Python iterates `SKILL_GROUPS.items()` (insertion-order in Python 3.7+). TypeScript: use `Object.entries()` which also preserves insertion order. |
| Copilot instructions content diverges from Python | Medium | High | Extract `buildCopilotInstructions` as a pure function. Snapshot-test against the exact expected string for a reference config. |

---

## 11. Implementation Groups (Execution Order)

### G1: GithubHooksAssembler (simplest — pure file copy, no template rendering)

**Files:**
- `src/assembler/github-hooks-assembler.ts`
- `tests/node/assembler/github-hooks-assembler.test.ts`

**Test scenarios:**
1. `assemble_validConfig_copies3HookFiles` — all 3 JSON files copied.
2. `assemble_templatesDirMissing_returnsEmpty` — graceful skip.
3. `assemble_individualTemplateFileMissing_skipsIt` — partial copy.
4. `assemble_createsHooksDirectory_whenNotPreExisting`.
5. `assemble_copiedContentMatchesSource` — byte-for-byte verification.
6. `assemble_engineParameterNotUsed` — verify engine is not called (API uniformity).

### G2: GithubMcpAssembler (JSON generation + env var validation)

**Files:**
- `src/assembler/github-mcp-assembler.ts`
- `tests/node/assembler/github-mcp-assembler.test.ts`

**Test scenarios (pure functions):**
1. `warnLiteralEnvValues_allDollarPrefixed_noWarnings`.
2. `warnLiteralEnvValues_literalValue_returnsWarning`.
3. `warnLiteralEnvValues_emptyEnv_noWarnings`.
4. `warnLiteralEnvValues_mixedValues_warnsOnlyLiterals`.
5. `buildCopilotMcpDict_twoServers_correctStructure`.
6. `buildCopilotMcpDict_serverWithoutCapabilities_omitsField`.
7. `buildCopilotMcpDict_serverWithoutEnv_omitsField`.

**Test scenarios (assemble):**
8. `assemble_noServers_returnsEmptyResult`.
9. `assemble_twoServers_generatesJson`.
10. `assemble_jsonFormat_2spaceIndentWithTrailingNewline`.
11. `assemble_literalEnvValue_collectsWarning`.
12. `assemble_createsGithubDirectory`.

### G3: GithubInstructionsAssembler (string building + template rendering)

**Files:**
- `src/assembler/github-instructions-assembler.ts`
- `tests/node/assembler/github-instructions-assembler.test.ts`

**Test scenarios (pure function):**
1. `buildCopilotInstructions_fullConfig_containsProjectName`.
2. `buildCopilotInstructions_restInterface_uppercased`.
3. `buildCopilotInstructions_grpcInterface_uppercased`.
4. `buildCopilotInstructions_cliInterface_lowercase`.
5. `buildCopilotInstructions_multipleInterfaces_commaSeparated`.
6. `buildCopilotInstructions_noInterfaces_showsNone`.
7. `buildCopilotInstructions_frameworkVersionPresent_spacePrefix`.
8. `buildCopilotInstructions_frameworkVersionEmpty_noSpace`.
9. `buildCopilotInstructions_booleans_lowercase`.
10. `buildCopilotInstructions_containsStackTable`.
11. `buildCopilotInstructions_containsConstraints`.
12. `buildCopilotInstructions_containsContextualReferences`.
13. `buildCopilotInstructions_trailingNewline`.

**Test scenarios (assemble):**
14. `assemble_generatesGlobalFile`.
15. `assemble_generatesContextualFiles_whenTemplatesExist`.
16. `assemble_skipsContextualFile_whenTemplateFileMissing`.
17. `assemble_templatesDirMissing_onlyGeneratesGlobal`.
18. `assemble_contextualFilesApplyPlaceholderReplacement`.
19. `assemble_createsDirectoryStructure`.

### G4: GithubSkillsAssembler (group iteration + conditional filtering)

**Files:**
- `src/assembler/github-skills-assembler.ts`
- `tests/node/assembler/github-skills-assembler.test.ts`

**Test scenarios:**
1. `filterSkills_nonInfraGroup_returnsAllSkills`.
2. `filterSkills_infraGroup_orchestratorNone_excludesSetupAndK8s`.
3. `filterSkills_infraGroup_orchestratorKubernetes_includesK8sSkills`.
4. `filterSkills_infraGroup_containerNone_excludesDockerfile`.
5. `filterSkills_infraGroup_iacTerraform_includesIacTerraform`.
6. `filterSkills_infraGroup_kustomize_includesK8sKustomize`.
7. `assemble_allGroupsPresent_generatesAllSkills`.
8. `assemble_templateDirMissing_skipsGroup`.
9. `assemble_templateFileMissing_skipsSkill`.
10. `assemble_appliesPlaceholderReplacement`.
11. `assemble_outputStructure_skillName_SKILL_md`.
12. `assemble_infraFiltered_reducedOutput`.

### G5: GithubAgentsAssembler (core + conditional + developer selection)

**Files:**
- `src/assembler/github-agents-assembler.ts`
- `tests/node/assembler/github-agents-assembler.test.ts`

**Test scenarios (selection):**
1. `selectGithubConditionalAgents_containerDocker_includesDevops`.
2. `selectGithubConditionalAgents_allInfraNone_excludesDevops`.
3. `selectGithubConditionalAgents_restInterface_includesApiEngineer`.
4. `selectGithubConditionalAgents_cliInterface_excludesApiEngineer`.
5. `selectGithubConditionalAgents_eventDriven_includesEventEngineer`.
6. `selectGithubConditionalAgents_eventConsumerInterface_includesEventEngineer`.

**Test scenarios (assemble):**
7. `assemble_coreAgents_copiedWithAgentMdExtension`.
8. `assemble_conditionalAgents_copiedWhenSelected`.
9. `assemble_developerAgent_copiedByLanguage`.
10. `assemble_missingConditionalTemplate_collectsWarning`.
11. `assemble_missingDeveloperTemplate_collectsWarning`.
12. `assemble_placeholderReplacement_applied`.
13. `assemble_outputExtension_agentMd`.
14. `assemble_fullCombination_allAgentTypes`.

### G6: GithubPromptsAssembler (Nunjucks rendering)

**Files:**
- `src/assembler/github-prompts-assembler.ts`
- `tests/node/assembler/github-prompts-assembler.test.ts`

**Test scenarios:**
1. `assemble_validConfig_generates4Prompts`.
2. `assemble_templatesDirMissing_returnsEmpty`.
3. `assemble_individualTemplateMissing_skipsIt`.
4. `assemble_outputName_removesJ2Suffix`.
5. `assemble_rendersNunjucksTemplates_notJustPlaceholders`.
6. `assemble_renderedContent_containsProjectValues`.
7. `assemble_createsPromptsDirectory`.

### G7: Barrel export + compile check

- Add exports to `src/assembler/index.ts`.
- Run `npx tsc --noEmit` to verify compilation.

---

## 12. Testing Strategy

### Test infrastructure

Each test file uses the same pattern as existing assembler tests (e.g., `hooks-assembler.test.ts`):
- `beforeEach`: create `tmpDir` with `fs.mkdtempSync`, set up `resourcesDir` and `outputDir`.
- `afterEach`: clean up with `fs.rmSync(tmpDir, { recursive: true, force: true })`.
- Helper functions to create fixture files (template markdown, JSON hook files, Nunjucks templates).

### Coverage targets

| Metric | Target | Strategy |
|--------|--------|----------|
| Line | >= 95% | Every code path exercised |
| Branch | >= 90% | All conditionals tested (missing dirs, missing files, empty configs, infra filtering, env var validation) |

### Config builder helper

Tests use a `buildConfig()` helper pattern similar to existing test files:
```typescript
function buildConfig(overrides: {
  interfaces?: InterfaceConfig[];
  orchestrator?: string;
  container?: string;
  iac?: string;
  serviceMesh?: string;
  templating?: string;
  eventDriven?: boolean;
  language?: string;
  mcpServers?: McpServerConfig[];
}): ProjectConfig
```

### GithubPromptsAssembler test specifics

Since `GithubPromptsAssembler` uses `engine.renderTemplate()` (full Nunjucks rendering), tests must create actual `.j2` template files in the temp `resourcesDir` directory, not just plain markdown. The `TemplateEngine` constructor needs the resourcesDir path to configure the Nunjucks FileSystemLoader.

---

## 13. File-by-File Mapping (Python to TypeScript)

| Python Source | TypeScript Target | Lines (Python) | Notes |
|--------------|------------------|----------------|-------|
| `github_instructions_assembler.py` class + `_build_copilot_instructions` | `github-instructions-assembler.ts` | 156 | Programmatic content building |
| `github_mcp_assembler.py` class + helpers | `github-mcp-assembler.ts` | 64 | JSON generation + env validation |
| `github_skills_assembler.py` class + constants | `github-skills-assembler.ts` | 163 | Group iteration + conditional filtering |
| `github_agents_assembler.py` class + selection helpers | `github-agents-assembler.ts` | 200 | Core/conditional/developer agent assembly |
| `github_hooks_assembler.py` class | `github-hooks-assembler.ts` | 57 | Simple file copy |
| `github_prompts_assembler.py` class | `github-prompts-assembler.ts` | 61 | Nunjucks rendering |

### Method-level mapping

**GithubInstructionsAssembler:**

| Python | TypeScript | Notes |
|--------|-----------|-------|
| `__init__(resources_dir)` | Not needed | `resourcesDir` passed per-call |
| `assemble(config, output_dir, engine)` | `assemble(config, outputDir, resourcesDir, engine)` | Signature aligned with TS pattern |
| `_generate_global(config, github_dir)` | `generateGlobal(config, githubDir)` (private) | Same logic |
| `_generate_contextual(engine, instructions_dir)` | `generateContextual(engine, resourcesDir, instructionsDir)` (private) | `resourcesDir` extracted from constructor |
| `_build_copilot_instructions(config)` | `buildCopilotInstructions(config)` (exported) | Pure function for testability |

**GithubMcpAssembler:**

| Python | TypeScript | Notes |
|--------|-----------|-------|
| `assemble(config, output_dir, engine)` | `assemble(config, outputDir, resourcesDir, engine)` | Signature aligned |
| `_warn_literal_env_values(servers)` | `warnLiteralEnvValues(servers)` (exported) | Returns array instead of logging |
| `_build_copilot_mcp_dict(config)` | `buildCopilotMcpDict(config)` (exported) | Same structure |

**GithubSkillsAssembler:**

| Python | TypeScript | Notes |
|--------|-----------|-------|
| `__init__(resources_dir)` | Not needed | Per-call parameter |
| `assemble(config, output_dir, engine)` | `assemble(config, outputDir, resourcesDir, engine)` | Signature aligned |
| `_filter_skills(config, group, skill_names)` | `filterSkills(config, group, skillNames)` (private) | Same logic |
| `_generate_group(engine, output_dir, group, skill_names)` | `generateGroup(engine, resourcesDir, outputDir, group, skillNames)` (private) | `resourcesDir` parameter added |
| `SKILL_GROUPS` dict | `SKILL_GROUPS` const record | Same data |
| `INFRA_SKILL_CONDITIONS` dict | `INFRA_SKILL_CONDITIONS` const record | Lambdas become arrow functions |

**GithubAgentsAssembler:**

| Python | TypeScript | Notes |
|--------|-----------|-------|
| `__init__(resources_dir)` | Not needed | Per-call parameter |
| `assemble(config, output_dir, engine)` | `assemble(config, outputDir, resourcesDir, engine)` | Returns `AssembleResult` |
| `_assemble_core(agents_dir, engine)` | `assembleCore(resourcesDir, agentsDir, engine)` (private) | Same logic |
| `_assemble_conditional(config, agents_dir, engine)` | `assembleConditional(config, resourcesDir, agentsDir, engine, warnings)` (private) | Collects warnings |
| `_assemble_developer(config, agents_dir, engine)` | `assembleDeveloper(config, resourcesDir, agentsDir, engine)` (private) | Same logic |
| `_render_agent(template, agents_dir, engine)` | `renderAgent(src, agentsDir, engine)` (private) | Path-based instead of Path object |
| `_select_conditional(config)` | `selectGithubConditionalAgents(config)` (exported) | Renamed for clarity |
| `_select_infra_agents(config)` | Inlined in selection function | Same conditions |
| `_select_interface_agents(config)` | Inlined, uses `hasAnyInterface` | Reuses existing helper |
| `_select_event_agents(config)` | Inlined in selection function | Same conditions |

**GithubHooksAssembler:**

| Python | TypeScript | Notes |
|--------|-----------|-------|
| `__init__(resources_dir)` | Not needed | Per-call parameter |
| `assemble(config, output_dir, engine)` | `assemble(config, outputDir, resourcesDir, engine)` | Simple copy loop |
| `HOOK_TEMPLATES` tuple | `GITHUB_HOOK_TEMPLATES` const array | Same 3 filenames |

**GithubPromptsAssembler:**

| Python | TypeScript | Notes |
|--------|-----------|-------|
| `__init__(resources_dir)` | Not needed | Per-call parameter |
| `assemble(config, output_dir, engine)` | `assemble(config, outputDir, resourcesDir, engine)` | Nunjucks rendering |
| `PROMPT_TEMPLATES` tuple | `GITHUB_PROMPT_TEMPLATES` const array | Same 4 filenames |
| `template_name.removesuffix(".j2")` | `templateName.replace(/\.j2$/, "")` | String manipulation difference |
| `engine.render_template(Path(...) / name)` | `engine.renderTemplate(dirName + "/" + name)` | Path handling difference |

---

## 14. Acceptance Criteria Checklist

From story Gherkin scenarios:

- [ ] Copilot instructions generated with stack info: `copilot-instructions.md` contains project name and technology stack table.
- [ ] MCP config generated when servers configured: `copilot-mcp.json` contains 2 servers.
- [ ] MCP warning for env var literal: warning collected when env value does not start with `$`.
- [ ] GitHub skills filter infra by config: K8s skills excluded when orchestrator is `"none"`.
- [ ] GitHub prompts rendered with Nunjucks: 4 prompt files generated with project values (not raw placeholders).
- [ ] GitHub hooks templates copied: 3 JSON files copied to `hooks/`.

From DoD:

- [ ] All 6 assemblers implemented.
- [ ] GithubMcpAssembler validates `$VARIABLE` format.
- [ ] GithubSkillsAssembler filters infra skills conditionally.
- [ ] GithubPromptsAssembler renders templates with Nunjucks.
- [ ] Output identical to Python.
- [ ] Coverage >= 95% line, >= 90% branch.
- [ ] JSDoc on all public classes and exported functions.
