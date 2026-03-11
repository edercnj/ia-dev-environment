# Task Decomposition -- STORY-014: GitHub Assemblers (Instructions, MCP, Skills, Agents, Hooks, Prompts)

**Status:** PENDING
**Date:** 2026-03-10
**Blocked By:** STORY-005 (template engine), STORY-006 (domain mappings), STORY-008 (assembler helpers) -- all complete
**Blocks:** STORY-016 (pipeline orchestrator)

---

## G1 -- GithubHooksAssembler (simplest -- file copy)

**Dependencies:** `src/models.ts` (ProjectConfig), `src/template-engine.ts` (TemplateEngine)
**Pattern reference:** `src/assembler/hooks-assembler.ts` (HooksAssembler -- same copy-based approach)

### T1.1 -- Create `GithubHooksAssembler` class

- **File:** `src/assembler/github-hooks-assembler.ts` (create)
- **Description:** Assembler that copies 3 hook JSON templates from `resources/github-hooks-templates/` to `${outputDir}/github/hooks/`. No template rendering -- files are copied verbatim.
- **Imports:**
  - `node:fs`, `node:path`
  - `ProjectConfig` from `../models.js`
  - `TemplateEngine` from `../template-engine.js`
- **Exported constants:**
  - `GITHUB_HOOK_TEMPLATES: readonly string[]` -- `["post-compile-check.json", "pre-commit-lint.json", "session-context-loader.json"]`
- **Exported class: `GithubHooksAssembler`**
  - `assemble(config: ProjectConfig, outputDir: string, resourcesDir: string, _engine: TemplateEngine): string[]`
    1. Resolve `srcDir = path.join(resourcesDir, "github-hooks-templates")`.
    2. If `srcDir` does not exist, return `[]`.
    3. Create `path.join(outputDir, "github", "hooks")` with `{ recursive: true }`.
    4. For each template in `GITHUB_HOOK_TEMPLATES`:
       - Build `src = path.join(srcDir, template)`.
       - If `src` does not exist, skip.
       - Copy to `dest = path.join(outputDir, "github", "hooks", template)` via `fs.copyFileSync`.
       - Push `dest` to results array.
    5. Return results array.
- **JSDoc:** Module-level doc and class/method JSDoc.
- **Estimated lines:** ~45
- **Tier:** Junior

### T1.2 -- Create `GithubHooksAssembler` tests

- **File:** `tests/node/assembler/github-hooks-assembler.test.ts` (create)
- **Description:** Unit tests using `vitest`, `mkdtempSync`-backed temp directory, cleaned up in `afterEach`.
- **Helpers:**
  - `buildConfig()` -- minimal `ProjectConfig` (same pattern as `hooks-assembler.test.ts`).
  - `createHookTemplate(resourcesDir, filename, content?)` -- creates a JSON file in `github-hooks-templates/`.
- **Test cases:**
  1. `assemble_validConfig_copies3HookFiles` -- all 3 JSON files created in fixture, all 3 copied.
  2. `assemble_templatesDirMissing_returnsEmpty` -- no `github-hooks-templates/` dir, returns `[]`.
  3. `assemble_individualTemplateFileMissing_skipsIt` -- only 2 of 3 templates exist, returns 2 paths.
  4. `assemble_createsHooksDirectory_whenNotPreExisting` -- verify `github/hooks/` created.
  5. `assemble_copiedContentMatchesSource` -- byte-for-byte content comparison.
  6. `assemble_engineParameterNotUsed` -- engine is passed but not invoked (verify API uniformity).

### Compilation checkpoint

```
npx tsc --noEmit   # zero errors with GithubHooksAssembler
```

---

## G2 -- GithubMcpAssembler (JSON generation + env var validation)

**Dependencies:** `src/models.ts` (ProjectConfig, McpServerConfig, McpConfig), `src/assembler/rules-assembler.ts` (AssembleResult type)
**Pattern reference:** JSON output generation, warning collection via `AssembleResult`

### T2.1 -- Create `warnLiteralEnvValues` pure function

- **File:** `src/assembler/github-mcp-assembler.ts` (create)
- **Description:** Module-level exported pure function.
- **Signature:** `warnLiteralEnvValues(servers: readonly McpServerConfig[]): string[]`
- **Logic:**
  1. For each server in `servers`:
     - For each `[key, value]` in `Object.entries(server.env)`:
       - If `value` does not start with `"$"`, push warning string: `"MCP server '${server.id}': env var '${key}' uses literal value instead of $VARIABLE format"`.
  2. Return collected warnings array.
- **Estimated lines:** ~15

### T2.2 -- Create `buildCopilotMcpDict` pure function

- **File:** `src/assembler/github-mcp-assembler.ts` (append)
- **Description:** Module-level exported pure function.
- **Signature:** `buildCopilotMcpDict(config: ProjectConfig): Record<string, unknown>`
- **Logic:**
  1. Build `mcpServers: Record<string, unknown> = {}`.
  2. For each `server` in `config.mcp.servers`:
     - Build entry: `{ url: server.url }`.
     - If `server.capabilities.length > 0`, add `capabilities: [...server.capabilities]`.
     - If `Object.keys(server.env).length > 0`, add `env: { ...server.env }`.
     - Assign to `mcpServers[server.id]`.
  3. Return `{ mcpServers }`.
- **Estimated lines:** ~20

### T2.3 -- Create `GithubMcpAssembler` class

- **File:** `src/assembler/github-mcp-assembler.ts` (append)
- **Description:** Assembler class that generates `github/copilot-mcp.json`.
- **Imports:**
  - `node:fs`, `node:path`
  - `ProjectConfig`, `McpServerConfig` from `../models.js`
  - `TemplateEngine` from `../template-engine.js`
  - `AssembleResult` from `./rules-assembler.js`
- **Exported class: `GithubMcpAssembler`**
  - `assemble(config: ProjectConfig, outputDir: string, resourcesDir: string, _engine: TemplateEngine): AssembleResult`
    1. If `config.mcp.servers.length === 0`, return `{ files: [], warnings: [] }`.
    2. Call `warnLiteralEnvValues(config.mcp.servers)` to collect warnings.
    3. Create `path.join(outputDir, "github")` with `{ recursive: true }`.
    4. Build dict via `buildCopilotMcpDict(config)`.
    5. Write `JSON.stringify(dict, null, 2) + "\n"` to `path.join(outputDir, "github", "copilot-mcp.json")`.
    6. Return `{ files: [destPath], warnings }`.
- **Estimated lines:** ~25 (class only)

### T2.4 -- Create `GithubMcpAssembler` tests

- **File:** `tests/node/assembler/github-mcp-assembler.test.ts` (create)
- **Description:** Unit tests for pure functions and `assemble` method.
- **Helpers:**
  - `buildConfig(overrides?)` -- accepts `mcpServers?: McpServerConfig[]`.
  - `buildServer(overrides?)` -- creates `McpServerConfig` with defaults.
- **Test cases (pure functions):**
  1. `warnLiteralEnvValues_allDollarPrefixed_noWarnings`
  2. `warnLiteralEnvValues_literalValue_returnsWarning`
  3. `warnLiteralEnvValues_emptyEnv_noWarnings`
  4. `warnLiteralEnvValues_mixedValues_warnsOnlyLiterals`
  5. `buildCopilotMcpDict_twoServers_correctStructure`
  6. `buildCopilotMcpDict_serverWithoutCapabilities_omitsField`
  7. `buildCopilotMcpDict_serverWithoutEnv_omitsField`
- **Test cases (assemble):**
  8. `assemble_noServers_returnsEmptyResult`
  9. `assemble_twoServers_generatesJson`
  10. `assemble_jsonFormat_2spaceIndentWithTrailingNewline`
  11. `assemble_literalEnvValue_collectsWarning`
  12. `assemble_createsGithubDirectory`

### Compilation checkpoint

```
npx tsc --noEmit   # zero errors with GithubMcpAssembler
```

---

## G3 -- GithubPromptsAssembler (Nunjucks template rendering)

**Dependencies:** `src/models.ts` (ProjectConfig), `src/template-engine.ts` (TemplateEngine -- uses `renderTemplate()`)
**Pattern reference:** Only GitHub assembler that uses full Nunjucks rendering (not just `replacePlaceholders`)

### T3.1 -- Create `GithubPromptsAssembler` class

- **File:** `src/assembler/github-prompts-assembler.ts` (create)
- **Description:** Assembler that renders 4 `.j2` Nunjucks prompt templates to `.github/prompts/`.
- **Imports:**
  - `node:fs`, `node:path`
  - `ProjectConfig` from `../models.js`
  - `TemplateEngine` from `../template-engine.js`
- **Exported constants:**
  - `GITHUB_PROMPT_TEMPLATES: readonly string[]` -- `["new-feature.prompt.md.j2", "decompose-spec.prompt.md.j2", "code-review.prompt.md.j2", "troubleshoot.prompt.md.j2"]`
- **Exported class: `GithubPromptsAssembler`**
  - `assemble(config: ProjectConfig, outputDir: string, resourcesDir: string, engine: TemplateEngine): string[]`
    1. Resolve `srcDir = path.join(resourcesDir, "github-prompts-templates")`.
    2. If `srcDir` does not exist, return `[]`.
    3. Create `path.join(outputDir, "github", "prompts")` with `{ recursive: true }`.
    4. For each `templateName` in `GITHUB_PROMPT_TEMPLATES`:
       - Build `src = path.join(srcDir, templateName)`.
       - If `src` does not exist, skip.
       - Derive `outputName = templateName.replace(/\.j2$/, "")` (removes `.j2` suffix).
       - Render via `engine.renderTemplate("github-prompts-templates/" + templateName)`.
       - Write rendered content to `dest = path.join(outputDir, "github", "prompts", outputName)`.
       - Push `dest` to results.
    5. Return results array.
- **JSDoc:** Module-level doc and class/method JSDoc.
- **Estimated lines:** ~50

### T3.2 -- Create `GithubPromptsAssembler` tests

- **File:** `tests/node/assembler/github-prompts-assembler.test.ts` (create)
- **Description:** Unit tests. Since `GithubPromptsAssembler` uses `engine.renderTemplate()` (full Nunjucks), tests must create actual `.j2` template files in the temp `resourcesDir`. The `TemplateEngine` constructor needs the `resourcesDir` path for the Nunjucks FileSystemLoader.
- **Helpers:**
  - `buildConfig()` -- minimal `ProjectConfig`.
  - `createPromptTemplate(resourcesDir, filename, content)` -- creates `.j2` file in `github-prompts-templates/`.
- **Test cases:**
  1. `assemble_validConfig_generates4Prompts` -- all 4 templates present, all 4 rendered.
  2. `assemble_templatesDirMissing_returnsEmpty` -- no `github-prompts-templates/` dir, returns `[]`.
  3. `assemble_individualTemplateMissing_skipsIt` -- only 3 of 4 templates exist, returns 3 paths.
  4. `assemble_outputName_removesJ2Suffix` -- output filenames end with `.prompt.md` (not `.j2`).
  5. `assemble_rendersNunjucksTemplates_notJustPlaceholders` -- Nunjucks `{{ variable }}` syntax rendered.
  6. `assemble_renderedContent_containsProjectValues` -- output contains actual project name, language, etc.
  7. `assemble_createsPromptsDirectory` -- `github/prompts/` directory created.

### Compilation checkpoint

```
npx tsc --noEmit   # zero errors with GithubPromptsAssembler
```

---

## G4 -- GithubAgentsAssembler (conditional agent selection)

**Dependencies:** `src/models.ts` (ProjectConfig), `src/template-engine.ts`, `src/assembler/conditions.ts` (hasAnyInterface), `src/assembler/rules-assembler.ts` (AssembleResult)
**Pattern reference:** `src/assembler/agents-assembler.ts` (AgentsAssembler -- core/conditional/developer pattern)

### T4.1 -- Create `selectGithubConditionalAgents` pure function

- **File:** `src/assembler/github-agents-assembler.ts` (create)
- **Description:** Module-level exported pure function for conditional agent selection.
- **Imports:**
  - `ProjectConfig` from `../models.js`
  - `hasAnyInterface` from `./conditions.js`
- **Signature:** `selectGithubConditionalAgents(config: ProjectConfig): string[]`
- **Logic:**
  1. Initialize `agents: string[] = []`.
  2. **DevOps agent:** If any of `config.infrastructure.container`, `.orchestrator`, `.iac`, `.serviceMesh` is not `"none"`, push `"devops-engineer.md"`.
  3. **API engineer:** If `hasAnyInterface(config, "rest", "grpc", "graphql")`, push `"api-engineer.md"`.
  4. **Event engineer:** If `config.architecture.eventDriven` OR `hasAnyInterface(config, "event-consumer", "event-producer")`, push `"event-engineer.md"`.
  5. Return `agents`.
- **Estimated lines:** ~25

### T4.2 -- Create `GithubAgentsAssembler` class

- **File:** `src/assembler/github-agents-assembler.ts` (append)
- **Description:** Assembler that generates `.github/agents/*.agent.md` with core, conditional, and developer agents. Output extension is `.agent.md` (GitHub convention, not `.md`).
- **Imports (additional):**
  - `node:fs`, `node:path`
  - `TemplateEngine` from `../template-engine.js`
  - `AssembleResult` from `./rules-assembler.js`
- **Constants:**
  - `GITHUB_AGENTS_TEMPLATES_DIR = "github-agents-templates"`
  - `CORE_DIR = "core"`, `CONDITIONAL_DIR = "conditional"`, `DEVELOPERS_DIR = "developers"`
  - `AGENT_MD_EXTENSION = ".agent.md"`
- **Exported class: `GithubAgentsAssembler`**
  - `assemble(config, outputDir, resourcesDir, engine): AssembleResult`
    1. Create `agentsDir = path.join(outputDir, "github", "agents")` with `{ recursive: true }`.
    2. `files.push(...this.assembleCore(resourcesDir, agentsDir, engine))`.
    3. `files.push(...this.assembleConditional(config, resourcesDir, agentsDir, engine, warnings))`.
    4. Call `this.assembleDeveloper(config, resourcesDir, agentsDir, engine)` -- if null, push warning.
    5. Return `{ files, warnings }`.
  - `private assembleCore(resourcesDir, agentsDir, engine): string[]`
    1. Resolve `coreDir = path.join(resourcesDir, GITHUB_AGENTS_TEMPLATES_DIR, CORE_DIR)`.
    2. If missing, return `[]`.
    3. Read and sort `.md` files from `coreDir`.
    4. For each: `renderAgent(src, agentsDir, engine)` -- push result.
  - `private assembleConditional(config, resourcesDir, agentsDir, engine, warnings): string[]`
    1. Call `selectGithubConditionalAgents(config)`.
    2. For each agent name, resolve source from `conditional/` dir.
    3. If source exists, render and collect. If missing, push warning.
  - `private assembleDeveloper(config, resourcesDir, agentsDir, engine): string | null`
    1. Derive filename: `${config.language.name}-developer.md`.
    2. Resolve source from `developers/` dir.
    3. If exists, render and return path. If missing, return null.
  - `private renderAgent(srcPath, agentsDir, engine): string`
    1. Read content from `srcPath`.
    2. Apply `engine.replacePlaceholders(content)`.
    3. Derive output name: `${stem}${AGENT_MD_EXTENSION}` where `stem` is the template filename without `.md`.
    4. Write to `path.join(agentsDir, outputName)`.
    5. Return dest path.
- **Key detail:** Template filenames are `*.md` but output filenames are `*.agent.md`. The `renderAgent` method strips the `.md` from the stem and appends `.agent.md`.
- **Estimated lines:** ~100
- **Tier:** Mid

### T4.3 -- Create `GithubAgentsAssembler` tests

- **File:** `tests/node/assembler/github-agents-assembler.test.ts` (create)
- **Description:** Unit tests for selection function and assemble method.
- **Helpers:**
  - `buildConfig(overrides?)` -- accepts `interfaces`, `container`, `orchestrator`, `iac`, `serviceMesh`, `eventDriven`, `language`.
  - `createCoreAgent(resourcesDir, filename, content?)` -- creates agent in `github-agents-templates/core/`.
  - `createConditionalAgent(resourcesDir, filename, content?)` -- creates in `conditional/`.
  - `createDeveloperAgent(resourcesDir, filename, content?)` -- creates in `developers/`.
- **Test cases (selection):**
  1. `selectGithubConditionalAgents_containerDocker_includesDevops`
  2. `selectGithubConditionalAgents_allInfraNone_excludesDevops`
  3. `selectGithubConditionalAgents_restInterface_includesApiEngineer`
  4. `selectGithubConditionalAgents_cliInterface_excludesApiEngineer`
  5. `selectGithubConditionalAgents_eventDriven_includesEventEngineer`
  6. `selectGithubConditionalAgents_eventConsumerInterface_includesEventEngineer`
- **Test cases (assemble):**
  7. `assemble_coreAgents_copiedWithAgentMdExtension` -- output files end with `.agent.md`.
  8. `assemble_conditionalAgents_copiedWhenSelected`
  9. `assemble_developerAgent_copiedByLanguage`
  10. `assemble_missingConditionalTemplate_collectsWarning`
  11. `assemble_missingDeveloperTemplate_collectsWarning`
  12. `assemble_placeholderReplacement_applied` -- `{project_name}` replaced in output.
  13. `assemble_outputExtension_agentMd` -- verify all output files match `*.agent.md` pattern.
  14. `assemble_fullCombination_allAgentTypes` -- core + conditional + developer all present.

### Compilation checkpoint

```
npx tsc --noEmit   # zero errors with GithubAgentsAssembler
```

---

## G5 -- GithubSkillsAssembler (conditional skill filtering)

**Dependencies:** `src/models.ts` (ProjectConfig), `src/template-engine.ts` (TemplateEngine)
**Pattern reference:** `src/assembler/skills-assembler.ts` (SkillsAssembler -- group-based iteration)

### T5.1 -- Create `SKILL_GROUPS` and `INFRA_SKILL_CONDITIONS` constants

- **File:** `src/assembler/github-skills-assembler.ts` (create)
- **Description:** Module-level exported constants defining skill group structure and conditional filtering.
- **Exports:**
  - `SKILL_GROUPS: Record<string, readonly string[]>` -- 7 groups mapping group name to skill filenames:
    - `story`: `["story-planning.md", "x-story-create.md", "x-story-epic-full.md", "x-story-epic.md", "x-story-map.md"]`
    - `dev`: `["layer-templates.md", "x-dev-implement.md", "x-dev-lifecycle.md"]`
    - `review`: `["x-review-api.md", "x-review-events.md", "x-review-gateway.md", "x-review-grpc.md", "x-review-pr.md", "x-review.md"]`
    - `testing`: `["run-contract-tests.md", "run-e2e.md", "run-perf-test.md", "run-smoke-api.md", "x-test-plan.md", "x-test-run.md"]`
    - `infrastructure`: `["setup-environment.md", "k8s-deployment.md", "k8s-kustomize.md", "dockerfile.md", "iac-terraform.md"]`
    - `knowledge-packs`: `["api-design.md", "architecture.md", "coding-standards.md", "compliance.md", "observability.md", "patterns.md", "protocols.md", "resilience.md", "security.md"]`
    - `git-troubleshooting`: `["x-git-push.md", "x-ops-troubleshoot.md"]`
  - `INFRA_SKILL_CONDITIONS: Record<string, (config: ProjectConfig) => boolean>` -- filtering for infrastructure group:
    - `"setup-environment.md"`: `(config) => config.infrastructure.orchestrator !== "none"`
    - `"k8s-deployment.md"`: `(config) => config.infrastructure.orchestrator === "kubernetes"`
    - `"k8s-kustomize.md"`: `(config) => config.infrastructure.templating === "kustomize"`
    - `"dockerfile.md"`: `(config) => config.infrastructure.container !== "none"`
    - `"iac-terraform.md"`: `(config) => config.infrastructure.iac === "terraform"`
- **Estimated lines:** ~50

### T5.2 -- Create `GithubSkillsAssembler` class

- **File:** `src/assembler/github-skills-assembler.ts` (append)
- **Description:** Assembler that generates `.github/skills/{name}/SKILL.md` from templates, with conditional filtering for infrastructure group.
- **Imports:**
  - `node:fs`, `node:path`
  - `ProjectConfig` from `../models.js`
  - `TemplateEngine` from `../template-engine.js`
- **Constants:**
  - `GITHUB_SKILLS_TEMPLATES_DIR = "github-skills-templates"`
  - `SKILLS_OUTPUT_DIR = "skills"`
  - `SKILL_MD = "SKILL.md"`
- **Exported class: `GithubSkillsAssembler`**
  - `assemble(config, outputDir, resourcesDir, engine): string[]`
    1. Initialize `results: string[] = []`.
    2. For each `[group, skillNames]` in `Object.entries(SKILL_GROUPS)`:
       - Call `filteredNames = this.filterSkills(config, group, skillNames)`.
       - Call `results.push(...this.generateGroup(engine, resourcesDir, outputDir, group, filteredNames))`.
    3. Return `results`.
  - `private filterSkills(config, group, skillNames): string[]`
    1. If `group !== "infrastructure"`, return `[...skillNames]`.
    2. Filter `skillNames` by `INFRA_SKILL_CONDITIONS`: for each name, if `INFRA_SKILL_CONDITIONS[name]` exists and returns `false` for `config`, exclude it.
    3. Return filtered array.
  - `private generateGroup(engine, resourcesDir, outputDir, group, skillNames): string[]`
    1. Resolve `srcDir = path.join(resourcesDir, GITHUB_SKILLS_TEMPLATES_DIR, group)`.
    2. If `srcDir` does not exist, return `[]`.
    3. For each `skillName` in `skillNames`:
       - Build `src = path.join(srcDir, skillName)`.
       - If `src` does not exist, skip.
       - Derive skill identifier: remove `.md` suffix from `skillName` for the directory name.
       - Read content, apply `engine.replacePlaceholders(content)`.
       - Create `destDir = path.join(outputDir, "github", SKILLS_OUTPUT_DIR, skillId)` with `{ recursive: true }`.
       - Write to `path.join(destDir, SKILL_MD)`.
       - Push dest path to results.
    4. Return results.
- **Estimated lines:** ~70
- **Tier:** Mid

### T5.3 -- Create `GithubSkillsAssembler` tests

- **File:** `tests/node/assembler/github-skills-assembler.test.ts` (create)
- **Description:** Unit tests for filtering logic and assemble method.
- **Helpers:**
  - `buildConfig(overrides?)` -- accepts `orchestrator`, `container`, `iac`, `templating`.
  - `createSkillTemplate(resourcesDir, group, filename, content?)` -- creates file in `github-skills-templates/{group}/`.
  - `createAllInfraSkills(resourcesDir)` -- creates all 5 infrastructure skill templates.
- **Test cases (filterSkills):**
  1. `filterSkills_nonInfraGroup_returnsAllSkills` -- `"dev"` group passes through unchanged.
  2. `filterSkills_infraGroup_orchestratorNone_excludesSetupAndK8s` -- 3 skills excluded.
  3. `filterSkills_infraGroup_orchestratorKubernetes_includesK8sSkills`
  4. `filterSkills_infraGroup_containerNone_excludesDockerfile`
  5. `filterSkills_infraGroup_iacTerraform_includesIacTerraform`
  6. `filterSkills_infraGroup_kustomize_includesK8sKustomize`
- **Test cases (assemble):**
  7. `assemble_allGroupsPresent_generatesAllSkills`
  8. `assemble_templateDirMissing_skipsGroup`
  9. `assemble_templateFileMissing_skipsSkill`
  10. `assemble_appliesPlaceholderReplacement`
  11. `assemble_outputStructure_skillName_SKILL_md` -- verify `github/skills/{name}/SKILL.md` structure.
  12. `assemble_infraFiltered_reducedOutput` -- orchestrator `"none"` + container `"none"` = fewer infra skills.

### Compilation checkpoint

```
npx tsc --noEmit   # zero errors with GithubSkillsAssembler
```

---

## G6 -- GithubInstructionsAssembler (most complex -- multi-file generation)

**Dependencies:** `src/models.ts` (ProjectConfig), `src/template-engine.ts` (TemplateEngine)
**Pattern reference:** Programmatic content building (no template) + template rendering for contextual files

### T6.1 -- Create `buildCopilotInstructions` pure function

- **File:** `src/assembler/github-instructions-assembler.ts` (create)
- **Description:** Module-level exported pure function that builds the full `copilot-instructions.md` content programmatically from a `ProjectConfig`.
- **Signature:** `buildCopilotInstructions(config: ProjectConfig): string`
- **Logic:** Build string with sections:
  1. **Header:** `# GitHub Copilot Instructions -- {project.name}\n\n`
  2. **Project Identity section:**
     - `## Project Identity\n\n`
     - `- **Name:** {project.name}\n`
     - `- **Purpose:** {project.purpose}\n`
     - `- **Architecture:** {architecture.style}\n`
     - `- **Domain-Driven Design:** {String(architecture.domainDriven).toLowerCase()}\n`
     - `- **Event-Driven:** {String(architecture.eventDriven).toLowerCase()}\n`
     - `- **Interfaces:** {formattedInterfaces}\n` where interfaces are formatted: `rest` -> `REST`, `grpc` -> `GRPC`, others as-is. Comma-separated. If empty, `"none"`.
     - `- **Language:** {language.name} {language.version}\n`
     - `- **Framework:** {framework.name}{frameworkVersion}\n` where `frameworkVersion` is ` {version}` (with leading space) only if non-empty.
  3. **Technology Stack table:**
     - `## Technology Stack\n\n`
     - Markdown table with rows: Architecture, Language, Framework, Build Tool, Container, Orchestrator, Resilience (`"Mandatory (always enabled)"`), Native Build, Smoke Tests, Contract Tests.
     - Booleans formatted as `String(value).toLowerCase()` producing `"true"/"false"`.
  4. **Constraints section:**
     - `## Constraints\n\n`
     - `- Cloud-Agnostic: ZERO dependencies on cloud-specific services\n`
     - `- Horizontal scalability: Application must be stateless\n`
     - `- Externalized configuration: All configuration via environment variables or ConfigMaps\n`
  5. **Contextual instructions reference:**
     - `## Contextual Instructions\n\n`
     - List the 4 contextual instruction files: domain, coding-standards, architecture, quality-gates.
  6. Ensure trailing `\n`.
- **Estimated lines:** ~80
- **Tier:** Mid-Senior (string building with exact format parity)

### T6.2 -- Create `GithubInstructionsAssembler` class

- **File:** `src/assembler/github-instructions-assembler.ts` (append)
- **Description:** Assembler that generates global `copilot-instructions.md` and contextual instruction files.
- **Imports:**
  - `node:fs`, `node:path`
  - `ProjectConfig` from `../models.js`
  - `TemplateEngine` from `../template-engine.js`
- **Constants:**
  - `CONTEXTUAL_INSTRUCTIONS: readonly string[]` -- `["domain", "coding-standards", "architecture", "quality-gates"]`
  - `INSTRUCTIONS_TEMPLATES_DIR = "github-instructions-templates"`
- **Exported class: `GithubInstructionsAssembler`**
  - `assemble(config, outputDir, resourcesDir, engine): string[]`
    1. Initialize `results: string[] = []`.
    2. Create `githubDir = path.join(outputDir, "github")` with `{ recursive: true }`.
    3. Push `this.generateGlobal(config, githubDir)` to results.
    4. Create `instructionsDir = path.join(githubDir, "instructions")` with `{ recursive: true }`.
    5. Push `...this.generateContextual(engine, resourcesDir, instructionsDir)` to results.
    6. Return results.
  - `private generateGlobal(config, githubDir): string`
    1. Build content via `buildCopilotInstructions(config)`.
    2. Write to `path.join(githubDir, "copilot-instructions.md")`.
    3. Return dest path.
  - `private generateContextual(engine, resourcesDir, instructionsDir): string[]`
    1. Resolve `srcDir = path.join(resourcesDir, INSTRUCTIONS_TEMPLATES_DIR)`.
    2. If `srcDir` does not exist, return `[]`.
    3. For each name in `CONTEXTUAL_INSTRUCTIONS`:
       - Build `src = path.join(srcDir, name + ".md")`.
       - If `src` does not exist, skip.
       - Read content, apply `engine.replacePlaceholders(content)`.
       - Write to `path.join(instructionsDir, name + ".instructions.md")`.
       - Push dest to results.
    4. Return results.
- **Estimated lines:** ~55
- **Tier:** Mid

### T6.3 -- Create `GithubInstructionsAssembler` tests

- **File:** `tests/node/assembler/github-instructions-assembler.test.ts` (create)
- **Description:** Unit tests for pure function and assemble method.
- **Helpers:**
  - `buildConfig(overrides?)` -- accepts `interfaces`, `frameworkVersion`, `domainDriven`, `eventDriven`, `smokeTests`, `contractTests`, `container`, `nativeBuild`.
  - `createInstructionTemplate(resourcesDir, name, content?)` -- creates template in `github-instructions-templates/`.
- **Test cases (pure function -- `buildCopilotInstructions`):**
  1. `buildCopilotInstructions_fullConfig_containsProjectName`
  2. `buildCopilotInstructions_restInterface_uppercased` -- `"rest"` rendered as `"REST"`.
  3. `buildCopilotInstructions_grpcInterface_uppercased` -- `"grpc"` rendered as `"GRPC"`.
  4. `buildCopilotInstructions_cliInterface_lowercase` -- `"cli"` rendered as-is.
  5. `buildCopilotInstructions_multipleInterfaces_commaSeparated`
  6. `buildCopilotInstructions_noInterfaces_showsNone`
  7. `buildCopilotInstructions_frameworkVersionPresent_spacePrefix` -- e.g., `"quarkus 3.0"`.
  8. `buildCopilotInstructions_frameworkVersionEmpty_noSpace` -- e.g., `"commander"` (no trailing space).
  9. `buildCopilotInstructions_booleans_lowercase` -- `"true"` / `"false"` not `"True"` / `"False"`.
  10. `buildCopilotInstructions_containsStackTable` -- output contains `| Layer | Technology |`.
  11. `buildCopilotInstructions_containsConstraints` -- output contains `Cloud-Agnostic`.
  12. `buildCopilotInstructions_containsContextualReferences` -- output references instruction files.
  13. `buildCopilotInstructions_trailingNewline` -- output ends with `\n`.
- **Test cases (assemble):**
  14. `assemble_generatesGlobalFile` -- `copilot-instructions.md` exists with expected content.
  15. `assemble_generatesContextualFiles_whenTemplatesExist` -- all 4 contextual files generated.
  16. `assemble_skipsContextualFile_whenTemplateFileMissing` -- only 3 of 4 templates, generates 3.
  17. `assemble_templatesDirMissing_onlyGeneratesGlobal` -- no templates dir, still generates global file.
  18. `assemble_contextualFilesApplyPlaceholderReplacement` -- `{project_name}` replaced.
  19. `assemble_createsDirectoryStructure` -- `github/` and `github/instructions/` created.

### Compilation checkpoint

```
npx tsc --noEmit   # zero errors with GithubInstructionsAssembler
```

---

## G7 -- Integration (barrel exports + full compile + cross-assembler verification)

**Dependencies:** G1-G6 complete

### T7.1 -- Add barrel exports to `src/assembler/index.ts`

- **File:** `src/assembler/index.ts` (modify)
- **Description:** Append 6 export lines after the existing STORY-013 section.
- **Content to add:**
  ```typescript
  // --- STORY-014: GitHub Assemblers ---
  export * from "./github-hooks-assembler.js";
  export * from "./github-mcp-assembler.js";
  export * from "./github-prompts-assembler.js";
  export * from "./github-agents-assembler.js";
  export * from "./github-skills-assembler.js";
  export * from "./github-instructions-assembler.js";
  ```
- **Tier:** Junior

### T7.2 -- Full compilation check

```
npx tsc --noEmit   # zero errors, all 6 assemblers and barrel exports compile
```

### T7.3 -- Run all tests

```
npx vitest run tests/node/assembler/github-hooks-assembler.test.ts
npx vitest run tests/node/assembler/github-mcp-assembler.test.ts
npx vitest run tests/node/assembler/github-prompts-assembler.test.ts
npx vitest run tests/node/assembler/github-agents-assembler.test.ts
npx vitest run tests/node/assembler/github-skills-assembler.test.ts
npx vitest run tests/node/assembler/github-instructions-assembler.test.ts
```

### T7.4 -- Coverage verification

```
npx vitest run --coverage tests/node/assembler/github-*.test.ts
```

- **Expected:** >= 95% line coverage, >= 90% branch coverage across all 6 assembler files.

### T7.5 -- Cross-assembler consistency checks

- Verify all 6 assemblers share the same method signature: `assemble(config, outputDir, resourcesDir, engine)`.
- Verify all output goes under `${outputDir}/github/` (not `${outputDir}/.github/`).
- Verify `GithubMcpAssembler` and `GithubAgentsAssembler` return `AssembleResult`; the other 4 return `string[]`.
- Verify no cross-assembler dependencies (each GitHub assembler is independent).

---

## Summary Table

| Group | Assembler | Files to Create | Files to Modify | Test Cases | Complexity |
|-------|-----------|----------------|----------------|------------|------------|
| G1 | GithubHooksAssembler | 2 (src + test) | 0 | 6 | Low |
| G2 | GithubMcpAssembler | 2 (src + test) | 0 | 12 | Medium |
| G3 | GithubPromptsAssembler | 2 (src + test) | 0 | 7 | Medium |
| G4 | GithubAgentsAssembler | 2 (src + test) | 0 | 14 | Medium |
| G5 | GithubSkillsAssembler | 2 (src + test) | 0 | 12 | Medium |
| G6 | GithubInstructionsAssembler | 2 (src + test) | 0 | 19 | High |
| G7 | Integration | 0 | 1 (index.ts) | 0 (verification) | Low |
| **Total** | **6 assemblers** | **12 new files** | **1 modified** | **70 test cases** | |

## File Inventory

### Source files (6 new)

| File | Exports |
|------|---------|
| `src/assembler/github-hooks-assembler.ts` | `GITHUB_HOOK_TEMPLATES`, `GithubHooksAssembler` |
| `src/assembler/github-mcp-assembler.ts` | `warnLiteralEnvValues`, `buildCopilotMcpDict`, `GithubMcpAssembler` |
| `src/assembler/github-prompts-assembler.ts` | `GITHUB_PROMPT_TEMPLATES`, `GithubPromptsAssembler` |
| `src/assembler/github-agents-assembler.ts` | `selectGithubConditionalAgents`, `GithubAgentsAssembler` |
| `src/assembler/github-skills-assembler.ts` | `SKILL_GROUPS`, `INFRA_SKILL_CONDITIONS`, `GithubSkillsAssembler` |
| `src/assembler/github-instructions-assembler.ts` | `buildCopilotInstructions`, `GithubInstructionsAssembler` |

### Test files (6 new)

| File | Test count |
|------|-----------|
| `tests/node/assembler/github-hooks-assembler.test.ts` | 6 |
| `tests/node/assembler/github-mcp-assembler.test.ts` | 12 |
| `tests/node/assembler/github-prompts-assembler.test.ts` | 7 |
| `tests/node/assembler/github-agents-assembler.test.ts` | 14 |
| `tests/node/assembler/github-skills-assembler.test.ts` | 12 |
| `tests/node/assembler/github-instructions-assembler.test.ts` | 19 |

### Modified files (1)

| File | Change |
|------|--------|
| `src/assembler/index.ts` | Add 6 barrel export lines |

## Key Implementation Notes

1. **Output directory convention:** All GitHub assemblers write to `${outputDir}/github/` (without leading dot). The pipeline or caller handles renaming to `.github/` if needed.
2. **Template rendering split:** `GithubPromptsAssembler` uses `engine.renderTemplate()` (full Nunjucks). All other assemblers use `engine.replacePlaceholders()` (legacy `{placeholder}` syntax) or no rendering at all.
3. **Return type split:** `GithubMcpAssembler` and `GithubAgentsAssembler` return `AssembleResult` (files + warnings). The other 4 return `string[]`.
4. **GitHub agent extension:** Output filenames use `.agent.md` (GitHub convention), while source templates use `.md`. The `renderAgent` method handles the extension transformation.
5. **Boolean formatting in instructions:** `buildCopilotInstructions` uses `String(bool).toLowerCase()` producing `"true"/"false"` (JavaScript convention). This differs from `TemplateEngine.buildDefaultContext` which uses Python-style `"True"/"False"` for Nunjucks parity. The copilot-instructions.md is programmatically built (not template-rendered), so JavaScript-style booleans are correct here.
6. **Interface type formatting:** `rest` and `grpc` are uppercased to `REST` and `GRPC` in copilot-instructions.md. Other types (e.g., `cli`, `graphql`) remain as-is.
7. **Skill group ordering:** `Object.entries(SKILL_GROUPS)` preserves insertion order (ECMAScript spec), matching Python's dict iteration behavior.
