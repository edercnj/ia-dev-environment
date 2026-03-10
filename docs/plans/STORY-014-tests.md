# Test Plan — STORY-014: GitHub Assemblers (Instructions, MCP, Skills, Agents, Hooks, Prompts)

## Scope

Unit tests for 6 GitHub assemblers that generate all `.github/` artifacts for
GitHub Copilot integration. Each assembler has its own test file. Tests cover
directory creation, template rendering, conditional filtering, env var validation,
file copying, Nunjucks rendering, and output format compliance. All tests use
`vitest` and a `mkdtempSync`-backed temporary directory cleaned up in `afterEach`.

**Target files:**
- `tests/node/assembler/github-hooks-assembler.test.ts`
- `tests/node/assembler/github-mcp-assembler.test.ts`
- `tests/node/assembler/github-instructions-assembler.test.ts`
- `tests/node/assembler/github-skills-assembler.test.ts`
- `tests/node/assembler/github-agents-assembler.test.ts`
- `tests/node/assembler/github-prompts-assembler.test.ts`

**Coverage targets:** >= 95% line, >= 90% branch.

---

## Conventions

- Test names follow `[methodOrBehavior]_[scenario]_[expectedBehavior]`.
- `buildConfig(overrides?)` helper constructs a `ProjectConfig` accepting optional
  fields: `interfaces`, `orchestrator`, `container`, `iac`, `serviceMesh`,
  `templating`, `eventDriven`, `language`, `frameworkVersion`, `mcpServers`.
  Defaults produce a minimal valid config with `language = "typescript"`,
  `framework = "commander"`, `container = "none"`, `orchestrator = "none"`.
- All assemblers receive `(config, outputDir, resourcesDir, engine)` per the
  established assembler signature.
- `TemplateEngine` is instantiated with `new TemplateEngine(resourcesDir, config)`.
- Helper functions create fixture files (template markdown, JSON hooks, Nunjucks
  `.j2` templates) in the temporary `resourcesDir` under the appropriate
  subdirectory.
- Imports follow the pattern established by existing assembler tests (e.g.,
  `hooks-assembler.test.ts`, `agents-assembler.test.ts`).

---

## Part 1 — GithubHooksAssembler

### Helpers

```typescript
function createGithubHookTemplate(
  resourcesDir: string,
  filename: string,
  content: string = '{"event": "test"}',
): void {
  const dir = path.join(resourcesDir, "github-hooks-templates");
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(path.join(dir, filename), content, "utf-8");
}
```

### Exported constant: `GITHUB_HOOK_TEMPLATES`

The constant contains the 3 template filenames:
`["post-compile-check.json", "pre-commit-lint.json", "session-context-loader.json"]`.

### Test Group: `assemble` — full copy

---

**Test GHK-01**
- **Name:** `assemble_validConfig_copies3HookFiles`
- **Scenario:** All 3 JSON template files exist in `github-hooks-templates/`.
- **Setup:** Create all 3 template files via `createGithubHookTemplate()`.
- **Expected:**
  - Return array has length 3.
  - All 3 files exist at `{outputDir}/github/hooks/`.
  - Filenames match: `post-compile-check.json`, `pre-commit-lint.json`,
    `session-context-loader.json`.

---

**Test GHK-02**
- **Name:** `assemble_templatesDirMissing_returnsEmpty`
- **Scenario:** The `github-hooks-templates/` directory does not exist.
- **Setup:** No fixture files created.
- **Expected:**
  - Return array is `[]`.
  - `{outputDir}/github/hooks/` directory does NOT exist.

---

**Test GHK-03**
- **Name:** `assemble_individualTemplateFileMissing_skipsIt`
- **Scenario:** Only 2 of 3 template files exist.
- **Setup:** Create `post-compile-check.json` and `pre-commit-lint.json` only.
- **Expected:**
  - Return array has length 2.
  - `session-context-loader.json` is NOT in output.

---

**Test GHK-04**
- **Name:** `assemble_createsHooksDirectory_whenNotPreExisting`
- **Scenario:** `{outputDir}/github/hooks/` does not exist before `assemble`.
- **Setup:** Create all 3 template files.
- **Expected:**
  - `{outputDir}/github/hooks/` is created automatically.
  - All 3 files are written inside it.

---

**Test GHK-05**
- **Name:** `assemble_copiedContentMatchesSource`
- **Scenario:** Verify files are copied byte-for-byte (no transformation).
- **Setup:** Create a hook template with known JSON content.
- **Expected:**
  - `fs.readFileSync(dest, "utf-8")` equals the original content exactly.

---

**Test GHK-06**
- **Name:** `assemble_engineParameterNotUsed`
- **Scenario:** The `engine` parameter is accepted for API uniformity but not
  called. Verify no placeholder replacement occurs.
- **Setup:** Create a template containing `{project_name}` literal text.
- **Expected:**
  - Output file still contains `{project_name}` (not replaced).

---

## Part 2 — GithubMcpAssembler

### Helpers

```typescript
function buildMcpConfig(overrides: {
  mcpServers?: McpServerConfig[];
}): ProjectConfig {
  // Uses shared buildConfig with mcp.servers override
}
```

### Exported pure functions: `warnLiteralEnvValues`, `buildCopilotMcpDict`

### Test Group: `warnLiteralEnvValues` (pure function)

---

**Test GMC-01**
- **Name:** `warnLiteralEnvValues_allDollarPrefixed_noWarnings`
- **Scenario:** All env values start with `$`.
- **Input:** Server with `env: { API_KEY: "$API_KEY", SECRET: "$SECRET" }`.
- **Expected:** Returns `[]`.

---

**Test GMC-02**
- **Name:** `warnLiteralEnvValues_literalValue_returnsWarning`
- **Scenario:** An env value does not start with `$`.
- **Input:** Server with `env: { API_KEY: "sk-hardcoded-value" }`.
- **Expected:** Returns array with 1 warning string containing the server id
  and the env key name.

---

**Test GMC-03**
- **Name:** `warnLiteralEnvValues_emptyEnv_noWarnings`
- **Scenario:** Server has no env vars.
- **Input:** Server with `env: {}`.
- **Expected:** Returns `[]`.

---

**Test GMC-04**
- **Name:** `warnLiteralEnvValues_mixedValues_warnsOnlyLiterals`
- **Scenario:** Some env values start with `$`, some do not.
- **Input:** Server with `env: { GOOD: "$VAR", BAD: "literal" }`.
- **Expected:** Returns array with 1 warning (for `BAD` only).

---

### Test Group: `buildCopilotMcpDict` (pure function)

---

**Test GMC-05**
- **Name:** `buildCopilotMcpDict_twoServers_correctStructure`
- **Scenario:** Config has 2 MCP servers with full attributes.
- **Expected:**
  - Result has `mcpServers` key.
  - `mcpServers` has entries keyed by each server's `id`.
  - Each entry has `url` field.

---

**Test GMC-06**
- **Name:** `buildCopilotMcpDict_serverWithoutCapabilities_omitsField`
- **Scenario:** Server has `capabilities: []`.
- **Expected:** The server entry in the dict does NOT have a `capabilities` key.

---

**Test GMC-07**
- **Name:** `buildCopilotMcpDict_serverWithoutEnv_omitsField`
- **Scenario:** Server has `env: {}`.
- **Expected:** The server entry in the dict does NOT have an `env` key.

---

**Test GMC-08**
- **Name:** `buildCopilotMcpDict_serverWithCapabilities_includesField`
- **Scenario:** Server has `capabilities: ["tools", "prompts"]`.
- **Expected:** The server entry contains `capabilities: ["tools", "prompts"]`.

---

**Test GMC-09**
- **Name:** `buildCopilotMcpDict_serverWithEnv_includesField`
- **Scenario:** Server has `env: { API_KEY: "$API_KEY" }`.
- **Expected:** The server entry contains `env: { API_KEY: "$API_KEY" }`.

---

### Test Group: `assemble`

---

**Test GMC-10**
- **Name:** `assemble_noServers_returnsEmptyResult`
- **Scenario:** `config.mcp.servers` is empty.
- **Expected:**
  - Return `{ files: [], warnings: [] }`.
  - No files created.

---

**Test GMC-11**
- **Name:** `assemble_twoServers_generatesJson`
- **Scenario:** Config has 2 MCP servers.
- **Expected:**
  - Return `files` has length 1.
  - File exists at `{outputDir}/github/copilot-mcp.json`.
  - Parsed JSON has `mcpServers` with 2 entries.

---

**Test GMC-12**
- **Name:** `assemble_jsonFormat_2spaceIndentWithTrailingNewline`
- **Scenario:** Verify JSON formatting.
- **Expected:**
  - Raw file content uses 2-space indentation.
  - Content ends with `\n`.

---

**Test GMC-13**
- **Name:** `assemble_literalEnvValue_collectsWarning`
- **Scenario:** Server has literal (non-`$`) env value.
- **Expected:**
  - Return `warnings` array has length >= 1.
  - Warning string references the server id and env key.

---

**Test GMC-14**
- **Name:** `assemble_createsGithubDirectory`
- **Scenario:** `{outputDir}/github/` does not exist before `assemble`.
- **Expected:** Directory is created automatically.

---

## Part 3 — GithubInstructionsAssembler

### Helpers

```typescript
function createInstructionTemplate(
  resourcesDir: string,
  templateName: string,
  content: string = "# Template\n{project_name}",
): void {
  const dir = path.join(resourcesDir, "github-instructions-templates");
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(path.join(dir, templateName), content, "utf-8");
}
```

### Exported pure function: `buildCopilotInstructions`

### Test Group: `buildCopilotInstructions` (pure function)

---

**Test GIN-01**
- **Name:** `buildCopilotInstructions_fullConfig_containsProjectName`
- **Scenario:** Config has `project.name = "my-app"`.
- **Expected:** Output string contains `"my-app"`.

---

**Test GIN-02**
- **Name:** `buildCopilotInstructions_restInterface_uppercased`
- **Scenario:** Config has interface type `"rest"`.
- **Expected:** Output contains `"REST"` (uppercased).

---

**Test GIN-03**
- **Name:** `buildCopilotInstructions_grpcInterface_uppercased`
- **Scenario:** Config has interface type `"grpc"`.
- **Expected:** Output contains `"GRPC"` (uppercased).

---

**Test GIN-04**
- **Name:** `buildCopilotInstructions_cliInterface_lowercase`
- **Scenario:** Config has interface type `"cli"`.
- **Expected:** Output contains `"cli"` (not uppercased).

---

**Test GIN-05**
- **Name:** `buildCopilotInstructions_multipleInterfaces_commaSeparated`
- **Scenario:** Config has interfaces `["rest", "grpc"]`.
- **Expected:** Output contains `"REST, GRPC"` (comma-separated, uppercased).

---

**Test GIN-06**
- **Name:** `buildCopilotInstructions_noInterfaces_showsNone`
- **Scenario:** Config has empty interfaces array.
- **Expected:** Output contains an appropriate "none" or empty indicator for
  interfaces.

---

**Test GIN-07**
- **Name:** `buildCopilotInstructions_frameworkVersionPresent_spacePrefix`
- **Scenario:** Config has `framework.version = "3.0"`.
- **Expected:** Output contains `" 3.0"` (space-prefixed version).

---

**Test GIN-08**
- **Name:** `buildCopilotInstructions_frameworkVersionEmpty_noSpace`
- **Scenario:** Config has `framework.version = ""`.
- **Expected:** Output does NOT contain a trailing space after framework name.

---

**Test GIN-09**
- **Name:** `buildCopilotInstructions_booleans_lowercase`
- **Scenario:** Config has `architecture.domainDriven = true`.
- **Expected:** Output contains `"true"` (lowercase, not `"True"`).

---

**Test GIN-10**
- **Name:** `buildCopilotInstructions_containsStackTable`
- **Scenario:** Full config.
- **Expected:** Output contains technology stack table headers (e.g.,
  `"Architecture"`, `"Language"`, `"Framework"`, `"Build Tool"`).

---

**Test GIN-11**
- **Name:** `buildCopilotInstructions_containsConstraints`
- **Scenario:** Full config.
- **Expected:** Output contains constraint text: `"Cloud-Agnostic"`,
  `"stateless"`, `"externalized"`.

---

**Test GIN-12**
- **Name:** `buildCopilotInstructions_containsContextualReferences`
- **Scenario:** Full config.
- **Expected:** Output contains references to contextual instruction files
  (e.g., `"domain.instructions.md"`, `"coding-standards.instructions.md"`).

---

**Test GIN-13**
- **Name:** `buildCopilotInstructions_trailingNewline`
- **Scenario:** Any valid config.
- **Expected:** Output string ends with `"\n"`.

---

### Test Group: `assemble`

---

**Test GIN-14**
- **Name:** `assemble_generatesGlobalFile`
- **Scenario:** Valid config, no contextual templates on disk.
- **Expected:**
  - File exists at `{outputDir}/github/copilot-instructions.md`.
  - Content matches `buildCopilotInstructions(config)`.

---

**Test GIN-15**
- **Name:** `assemble_generatesContextualFiles_whenTemplatesExist`
- **Scenario:** All 4 contextual templates exist on disk (`domain.md`,
  `coding-standards.md`, `architecture.md`, `quality-gates.md`).
- **Setup:** Create all 4 template files.
- **Expected:**
  - 4 contextual files generated in `{outputDir}/github/instructions/`.
  - Each file has `.instructions.md` extension.

---

**Test GIN-16**
- **Name:** `assemble_skipsContextualFile_whenTemplateFileMissing`
- **Scenario:** Only 2 of 4 contextual templates exist.
- **Expected:**
  - Only 2 contextual files generated.
  - No error thrown.

---

**Test GIN-17**
- **Name:** `assemble_templatesDirMissing_onlyGeneratesGlobal`
- **Scenario:** `github-instructions-templates/` directory does not exist.
- **Expected:**
  - `copilot-instructions.md` is still generated.
  - No contextual files generated.
  - Return array has length 1.

---

**Test GIN-18**
- **Name:** `assemble_contextualFilesApplyPlaceholderReplacement`
- **Scenario:** Template contains `{project_name}` placeholder.
- **Setup:** Create template with `"Project: {project_name}"`.
- **Expected:**
  - Output file contains `"Project: my-app"` (placeholder replaced).

---

**Test GIN-19**
- **Name:** `assemble_createsDirectoryStructure`
- **Scenario:** Neither `github/` nor `github/instructions/` exist before call.
- **Expected:**
  - Both directories are created.
  - Files are written inside them.

---

**Test GIN-20**
- **Name:** `assemble_returnsAllGeneratedPaths`
- **Scenario:** Global file + 4 contextual files.
- **Setup:** Create all 4 contextual templates.
- **Expected:**
  - Return array has length 5 (1 global + 4 contextual).

---

## Part 4 — GithubSkillsAssembler

### Helpers

```typescript
function createGithubSkillTemplate(
  resourcesDir: string,
  group: string,
  skillName: string,
  content: string = "# Skill\n{project_name}",
): void {
  const dir = path.join(
    resourcesDir, "github-skills-templates", group,
  );
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(path.join(dir, `${skillName}.md`), content, "utf-8");
}
```

### Exported constants: `SKILL_GROUPS`, `INFRA_SKILL_CONDITIONS`

### Test Group: `filterSkills` (infrastructure conditional filtering)

---

**Test GSK-01**
- **Name:** `filterSkills_nonInfraGroup_returnsAllSkills`
- **Scenario:** Group is `"story"` (not `"infrastructure"`).
- **Expected:** All skill names in the group are returned unfiltered.

---

**Test GSK-02**
- **Name:** `filterSkills_infraGroup_orchestratorNone_excludesSetupAndK8s`
- **Scenario:** `orchestrator = "none"`.
- **Expected:**
  - `"setup-environment"` is excluded.
  - `"k8s-deployment"` is excluded.
  - `"k8s-kustomize"` is excluded.

---

**Test GSK-03**
- **Name:** `filterSkills_infraGroup_orchestratorKubernetes_includesK8sSkills`
- **Scenario:** `orchestrator = "kubernetes"`.
- **Expected:**
  - `"setup-environment"` is included.
  - `"k8s-deployment"` is included.

---

**Test GSK-04**
- **Name:** `filterSkills_infraGroup_containerNone_excludesDockerfile`
- **Scenario:** `container = "none"`.
- **Expected:** `"dockerfile"` is excluded.

---

**Test GSK-05**
- **Name:** `filterSkills_infraGroup_iacTerraform_includesIacTerraform`
- **Scenario:** `iac = "terraform"`.
- **Expected:** `"iac-terraform"` is included.

---

**Test GSK-06**
- **Name:** `filterSkills_infraGroup_kustomize_includesK8sKustomize`
- **Scenario:** `templating = "kustomize"`, `orchestrator = "kubernetes"`.
- **Expected:** `"k8s-kustomize"` is included.

---

**Test GSK-07**
- **Name:** `filterSkills_infraGroup_iacNone_excludesIacTerraform`
- **Scenario:** `iac = "none"`.
- **Expected:** `"iac-terraform"` is excluded.

---

**Test GSK-08**
- **Name:** `filterSkills_infraGroup_containerDocker_includesDockerfile`
- **Scenario:** `container = "docker"`.
- **Expected:** `"dockerfile"` is included.

---

### Test Group: `assemble`

---

**Test GSK-09**
- **Name:** `assemble_allGroupsPresent_generatesAllSkills`
- **Scenario:** Templates exist for multiple groups.
- **Setup:** Create skill templates for several groups.
- **Expected:**
  - Files generated in `{outputDir}/github/skills/`.
  - Each skill is in its own `{skillName}/SKILL.md` subdirectory.

---

**Test GSK-10**
- **Name:** `assemble_templateDirMissing_skipsGroup`
- **Scenario:** `github-skills-templates/` directory does not exist for a group.
- **Expected:** That group produces no output. No error thrown.

---

**Test GSK-11**
- **Name:** `assemble_templateFileMissing_skipsSkill`
- **Scenario:** Group directory exists but specific skill template file is missing.
- **Expected:** That individual skill is skipped. No error thrown.

---

**Test GSK-12**
- **Name:** `assemble_appliesPlaceholderReplacement`
- **Scenario:** Template contains `{project_name}` placeholder.
- **Expected:** Output file has placeholder replaced with project name.

---

**Test GSK-13**
- **Name:** `assemble_outputStructure_skillName_SKILL_md`
- **Scenario:** Verify output path structure.
- **Expected:** Each skill is written to
  `{outputDir}/github/skills/{skillName}/SKILL.md`.

---

**Test GSK-14**
- **Name:** `assemble_infraFiltered_reducedOutput`
- **Scenario:** Config has `orchestrator = "none"`, `container = "none"`,
  `iac = "none"`.
- **Setup:** Create templates for all infra skills.
- **Expected:**
  - Infrastructure skills that require orchestrator/container/iac are excluded.
  - Remaining infra skills (if any unconditional ones exist) are included.

---

**Test GSK-15**
- **Name:** `assemble_createsSkillsDirectory`
- **Scenario:** `{outputDir}/github/skills/` does not exist before call.
- **Expected:** Directory is created automatically.

---

## Part 5 — GithubAgentsAssembler

### Helpers

```typescript
function createGithubCoreAgent(
  resourcesDir: string,
  agentName: string,
  content?: string,
): void {
  const dir = path.join(resourcesDir, "github-agents-templates", "core");
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(
    path.join(dir, agentName),
    content ?? `# ${agentName}\nProject: {project_name}`,
  );
}

function createGithubConditionalAgent(
  resourcesDir: string,
  agentName: string,
  content?: string,
): void {
  const dir = path.join(
    resourcesDir, "github-agents-templates", "conditional",
  );
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(
    path.join(dir, agentName),
    content ?? `# ${agentName}\nProject: {project_name}`,
  );
}

function createGithubDeveloperAgent(
  resourcesDir: string,
  agentName: string,
  content?: string,
): void {
  const dir = path.join(
    resourcesDir, "github-agents-templates", "developers",
  );
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(
    path.join(dir, agentName),
    content ?? `# ${agentName}\nProject: {project_name}`,
  );
}
```

### Exported pure function: `selectGithubConditionalAgents`

### Test Group: `selectGithubConditionalAgents` (pure function)

---

**Test GAG-01**
- **Name:** `selectGithubConditionalAgents_containerDocker_includesDevops`
- **Scenario:** `container = "docker"`.
- **Expected:** Result contains `"devops-engineer.md"`.

---

**Test GAG-02**
- **Name:** `selectGithubConditionalAgents_allInfraNone_excludesDevops`
- **Scenario:** `container = "none"`, `orchestrator = "none"`, `iac = "none"`,
  `serviceMesh = "none"`.
- **Expected:** Result does NOT contain `"devops-engineer.md"`.

---

**Test GAG-03**
- **Name:** `selectGithubConditionalAgents_restInterface_includesApiEngineer`
- **Scenario:** Config has `rest` interface.
- **Expected:** Result contains `"api-engineer.md"`.

---

**Test GAG-04**
- **Name:** `selectGithubConditionalAgents_grpcInterface_includesApiEngineer`
- **Scenario:** Config has `grpc` interface.
- **Expected:** Result contains `"api-engineer.md"`.

---

**Test GAG-05**
- **Name:** `selectGithubConditionalAgents_graphqlInterface_includesApiEngineer`
- **Scenario:** Config has `graphql` interface.
- **Expected:** Result contains `"api-engineer.md"`.

---

**Test GAG-06**
- **Name:** `selectGithubConditionalAgents_cliInterface_excludesApiEngineer`
- **Scenario:** Config has only `cli` interface.
- **Expected:** Result does NOT contain `"api-engineer.md"`.

---

**Test GAG-07**
- **Name:** `selectGithubConditionalAgents_eventDriven_includesEventEngineer`
- **Scenario:** `architecture.eventDriven = true`.
- **Expected:** Result contains `"event-engineer.md"`.

---

**Test GAG-08**
- **Name:** `selectGithubConditionalAgents_eventConsumerInterface_includesEventEngineer`
- **Scenario:** Config has `event-consumer` interface.
- **Expected:** Result contains `"event-engineer.md"`.

---

**Test GAG-09**
- **Name:** `selectGithubConditionalAgents_eventProducerInterface_includesEventEngineer`
- **Scenario:** Config has `event-producer` interface.
- **Expected:** Result contains `"event-engineer.md"`.

---

**Test GAG-10**
- **Name:** `selectGithubConditionalAgents_noEventConfig_excludesEventEngineer`
- **Scenario:** `eventDriven = false`, no event interfaces.
- **Expected:** Result does NOT contain `"event-engineer.md"`.

---

**Test GAG-11 (parametrized)**
- **Name:** `selectGithubConditionalAgents_infraField_includesDevops` (via `it.each`)
- **Parameters:**

  | Field | Value |
  |-------|-------|
  | `container` | `"docker"` |
  | `orchestrator` | `"kubernetes"` |
  | `iac` | `"terraform"` |
  | `serviceMesh` | `"istio"` |

- **Expected per row:** Result contains `"devops-engineer.md"`.

---

### Test Group: `assemble`

---

**Test GAG-12**
- **Name:** `assemble_coreAgents_copiedWithAgentMdExtension`
- **Scenario:** Core agent templates exist.
- **Setup:** Create `architect.md` and `tech-lead.md` in core dir.
- **Expected:**
  - Output files are `architect.agent.md` and `tech-lead.agent.md`.
  - Files exist at `{outputDir}/github/agents/`.

---

**Test GAG-13**
- **Name:** `assemble_conditionalAgents_copiedWhenSelected`
- **Scenario:** Config selects `devops-engineer`. Template exists.
- **Setup:** Create `devops-engineer.md` in conditional dir. Config with
  `container = "docker"`.
- **Expected:**
  - `devops-engineer.agent.md` exists in output.

---

**Test GAG-14**
- **Name:** `assemble_developerAgent_copiedByLanguage`
- **Scenario:** Config has `language = "typescript"`.
- **Setup:** Create `typescript-developer.md` in developers dir.
- **Expected:**
  - `typescript-developer.agent.md` exists in output.

---

**Test GAG-15**
- **Name:** `assemble_missingConditionalTemplate_collectsWarning`
- **Scenario:** Config selects a conditional agent but template file is missing.
- **Setup:** Config with `container = "docker"`. No `devops-engineer.md` on disk.
- **Expected:**
  - `result.warnings` contains a warning mentioning `"devops-engineer.md"`.

---

**Test GAG-16**
- **Name:** `assemble_missingDeveloperTemplate_collectsWarning`
- **Scenario:** Config has `language = "rust"`. No `rust-developer.md` on disk.
- **Expected:**
  - `result.warnings` contains a warning mentioning `"rust-developer.md"`.

---

**Test GAG-17**
- **Name:** `assemble_placeholderReplacement_applied`
- **Scenario:** Template contains `{project_name}` placeholder.
- **Setup:** Create core agent with content `"# Agent for {project_name}"`.
- **Expected:**
  - Output file contains `"# Agent for my-app"`.

---

**Test GAG-18**
- **Name:** `assemble_outputExtension_agentMd`
- **Scenario:** Verify all output files use `.agent.md` extension (not `.md`).
- **Setup:** Create core, conditional, and developer agents.
- **Expected:**
  - Every file in `result.files` ends with `.agent.md`.

---

**Test GAG-19**
- **Name:** `assemble_fullCombination_allAgentTypes`
- **Scenario:** Config triggers core + conditional + developer agents.
- **Setup:** Create core agents, conditional agents matching config, and
  developer agent for the language.
- **Expected:**
  - `result.files` contains entries from all 3 categories.
  - `result.warnings` is empty.

---

**Test GAG-20**
- **Name:** `assemble_coreAgentsSorted`
- **Scenario:** Multiple core agents exist with different names.
- **Setup:** Create `z-agent.md`, `a-agent.md`, `m-agent.md` in core dir.
- **Expected:**
  - Core agents are processed in alphabetical order.

---

**Test GAG-21**
- **Name:** `assemble_createsAgentsDirectory`
- **Scenario:** `{outputDir}/github/agents/` does not exist before call.
- **Expected:** Directory is created automatically.

---

## Part 6 — GithubPromptsAssembler

### Helpers

```typescript
function createGithubPromptTemplate(
  resourcesDir: string,
  templateName: string,
  content: string,
): void {
  const dir = path.join(resourcesDir, "github-prompts-templates");
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(path.join(dir, templateName), content, "utf-8");
}
```

### Exported constant: `GITHUB_PROMPT_TEMPLATES`

The constant contains the 4 template filenames:
`["new-feature.prompt.md.j2", "decompose-spec.prompt.md.j2", "code-review.prompt.md.j2", "troubleshoot.prompt.md.j2"]`.

### Test Group: `assemble`

---

**Test GPR-01**
- **Name:** `assemble_validConfig_generates4Prompts`
- **Scenario:** All 4 Nunjucks template files exist.
- **Setup:** Create all 4 `.j2` templates with Nunjucks content using
  `{{ project.name }}`.
- **Expected:**
  - Return array has length 4.
  - All 4 files exist at `{outputDir}/github/prompts/`.

---

**Test GPR-02**
- **Name:** `assemble_templatesDirMissing_returnsEmpty`
- **Scenario:** `github-prompts-templates/` directory does not exist.
- **Expected:**
  - Return array is `[]`.
  - No files created.

---

**Test GPR-03**
- **Name:** `assemble_individualTemplateMissing_skipsIt`
- **Scenario:** Only 2 of 4 templates exist.
- **Setup:** Create only `new-feature.prompt.md.j2` and
  `code-review.prompt.md.j2`.
- **Expected:**
  - Return array has length 2.
  - Only the 2 corresponding prompts are generated.

---

**Test GPR-04**
- **Name:** `assemble_outputName_removesJ2Suffix`
- **Scenario:** Input template is `new-feature.prompt.md.j2`.
- **Expected:**
  - Output file is `new-feature.prompt.md` (`.j2` suffix removed).

---

**Test GPR-05**
- **Name:** `assemble_rendersNunjucksTemplates_notJustPlaceholders`
- **Scenario:** Template uses Nunjucks syntax `{{ project.name }}` (not legacy
  `{project_name}` placeholders).
- **Setup:** Create template with `"Project: {{ project.name }}"`.
- **Expected:**
  - Output file contains `"Project: my-app"` (Nunjucks rendered).

---

**Test GPR-06**
- **Name:** `assemble_renderedContent_containsNoRawPlaceholders`
- **Scenario:** Verify no `{{ ... }}` or `{% ... %}` remain in output.
- **Setup:** Create template with Nunjucks variables.
- **Expected:**
  - Output file does NOT contain `{{` or `{%` patterns.

---

**Test GPR-07**
- **Name:** `assemble_createsPromptsDirectory`
- **Scenario:** `{outputDir}/github/prompts/` does not exist before call.
- **Expected:** Directory is created automatically.

---

**Test GPR-08**
- **Name:** `assemble_usesRenderTemplate_notReplacePlaceholders`
- **Scenario:** Template uses full Nunjucks syntax (conditionals, loops).
- **Setup:** Create template with `{% if project.name %}{{ project.name }}{% endif %}`.
- **Expected:**
  - Output contains the project name (rendered via Nunjucks, not simple
    placeholder replacement).

---

## Part 7 — Parity Tests

### Test Group: Parity with Python output

Parity tests compare the TypeScript assembler output against expected Python
output for representative configurations. These tests use the project's real
`resources/` directory (not synthetic fixtures) to catch any template file
discrepancies.

---

**Test PAR-01**
- **Name:** `parity_githubHooks_copies3JsonFiles`
- **Scenario:** Run `GithubHooksAssembler.assemble` with the real `resources/`
  directory. Verify all 3 JSON hook templates are copied.
- **Expected:**
  - 3 files copied to `{outputDir}/github/hooks/`.
  - Content is byte-identical to source templates.

---

**Test PAR-02**
- **Name:** `parity_githubMcp_twoServers_matchesExpectedJson`
- **Scenario:** Run `GithubMcpAssembler.assemble` with 2 MCP servers.
- **Expected:**
  - `copilot-mcp.json` matches the expected JSON structure.
  - 2-space indent and trailing newline.

---

**Test PAR-03**
- **Name:** `parity_githubInstructions_fullConfig_matchesExpectedContent`
- **Scenario:** Run `GithubInstructionsAssembler.assemble` with a full config
  using real resources.
- **Expected:**
  - `copilot-instructions.md` contains project name, stack table, and constraints.
  - Contextual files are generated with placeholders replaced.

---

**Test PAR-04**
- **Name:** `parity_githubSkills_infraFiltering_matchesExpectedOutput`
- **Scenario:** Run `GithubSkillsAssembler.assemble` with
  `orchestrator = "none"`, `container = "docker"`.
- **Expected:**
  - K8s-related skills are NOT in output.
  - Docker-related skills ARE in output.

---

**Test PAR-05**
- **Name:** `parity_githubAgents_fullConfig_matchesExpectedOutput`
- **Scenario:** Run `GithubAgentsAssembler.assemble` with a config that triggers
  core + conditional + developer agents, using real resources.
- **Expected:**
  - Core agents present with `.agent.md` extension.
  - Conditional agents match selection criteria.
  - Developer agent present for the configured language.

---

**Test PAR-06**
- **Name:** `parity_githubPrompts_fullConfig_4PromptsRendered`
- **Scenario:** Run `GithubPromptsAssembler.assemble` with real resources.
- **Expected:**
  - 4 prompt files generated.
  - No raw `{{ }}` Nunjucks placeholders remain.
  - Content contains project-specific values.

---

## Summary Table

| ID | Assembler | Behavior Under Test | Key Assertion |
|----|-----------|---------------------|---------------|
| GHK-01 | GithubHooks | All 3 JSON files copied | `result.length === 3` |
| GHK-02 | GithubHooks | Templates dir missing | `result === []` |
| GHK-03 | GithubHooks | Individual file missing: skip | `result.length === 2` |
| GHK-04 | GithubHooks | Hooks dir auto-created | Dir exists |
| GHK-05 | GithubHooks | Byte-for-byte content fidelity | Content match |
| GHK-06 | GithubHooks | Engine not used (API uniformity) | No placeholder replacement |
| GMC-01 | GithubMcp | All env values `$`-prefixed | `warnings === []` |
| GMC-02 | GithubMcp | Literal env value | Warning returned |
| GMC-03 | GithubMcp | Empty env | `warnings === []` |
| GMC-04 | GithubMcp | Mixed env values | Warns only literals |
| GMC-05 | GithubMcp | Two servers: dict structure | `mcpServers` with 2 keys |
| GMC-06 | GithubMcp | No capabilities: field omitted | No `capabilities` key |
| GMC-07 | GithubMcp | No env: field omitted | No `env` key |
| GMC-08 | GithubMcp | With capabilities: field included | `capabilities` present |
| GMC-09 | GithubMcp | With env: field included | `env` present |
| GMC-10 | GithubMcp | No servers: empty result | `files === []` |
| GMC-11 | GithubMcp | Two servers: JSON generated | File exists, 2 entries |
| GMC-12 | GithubMcp | JSON format: 2-space + newline | Format check |
| GMC-13 | GithubMcp | Literal env: warning collected | Warning string |
| GMC-14 | GithubMcp | Github dir auto-created | Dir exists |
| GIN-01 | GithubInstructions | Contains project name | `"my-app"` in output |
| GIN-02 | GithubInstructions | REST uppercased | `"REST"` in output |
| GIN-03 | GithubInstructions | gRPC uppercased | `"GRPC"` in output |
| GIN-04 | GithubInstructions | CLI lowercase | `"cli"` in output |
| GIN-05 | GithubInstructions | Multiple interfaces comma-separated | `"REST, GRPC"` |
| GIN-06 | GithubInstructions | No interfaces | None indicator |
| GIN-07 | GithubInstructions | Framework version space prefix | `" 3.0"` |
| GIN-08 | GithubInstructions | Empty version no space | No trailing space |
| GIN-09 | GithubInstructions | Booleans lowercase | `"true"` not `"True"` |
| GIN-10 | GithubInstructions | Stack table present | Table headers |
| GIN-11 | GithubInstructions | Constraints present | Constraint text |
| GIN-12 | GithubInstructions | Contextual references listed | `.instructions.md` refs |
| GIN-13 | GithubInstructions | Trailing newline | Ends with `\n` |
| GIN-14 | GithubInstructions | Global file generated | File exists |
| GIN-15 | GithubInstructions | 4 contextual files generated | 4 `.instructions.md` files |
| GIN-16 | GithubInstructions | Missing template: skip | 2 of 4 generated |
| GIN-17 | GithubInstructions | Templates dir missing: global only | `result.length === 1` |
| GIN-18 | GithubInstructions | Placeholder replacement in contextual | `{project_name}` replaced |
| GIN-19 | GithubInstructions | Directory structure created | Both dirs exist |
| GIN-20 | GithubInstructions | Returns all paths | `result.length === 5` |
| GSK-01 | GithubSkills | Non-infra: no filtering | All returned |
| GSK-02 | GithubSkills | Orchestrator none: excludes K8s | K8s skills excluded |
| GSK-03 | GithubSkills | Orchestrator kubernetes: includes K8s | K8s skills included |
| GSK-04 | GithubSkills | Container none: excludes dockerfile | `dockerfile` excluded |
| GSK-05 | GithubSkills | IAC terraform: includes iac-terraform | `iac-terraform` included |
| GSK-06 | GithubSkills | Kustomize: includes k8s-kustomize | `k8s-kustomize` included |
| GSK-07 | GithubSkills | IAC none: excludes iac-terraform | `iac-terraform` excluded |
| GSK-08 | GithubSkills | Container docker: includes dockerfile | `dockerfile` included |
| GSK-09 | GithubSkills | All groups: generates skills | Files in output |
| GSK-10 | GithubSkills | Template dir missing: skip group | No error |
| GSK-11 | GithubSkills | Template file missing: skip skill | No error |
| GSK-12 | GithubSkills | Placeholder replacement | `{project_name}` replaced |
| GSK-13 | GithubSkills | Output path structure | `skills/{name}/SKILL.md` |
| GSK-14 | GithubSkills | Infra all-none: filtered out | Reduced output |
| GSK-15 | GithubSkills | Skills dir auto-created | Dir exists |
| GAG-01 | GithubAgents | Docker: includes devops | `devops-engineer.md` |
| GAG-02 | GithubAgents | All infra none: excludes devops | Not in result |
| GAG-03 | GithubAgents | REST: includes api-engineer | `api-engineer.md` |
| GAG-04 | GithubAgents | gRPC: includes api-engineer | `api-engineer.md` |
| GAG-05 | GithubAgents | GraphQL: includes api-engineer | `api-engineer.md` |
| GAG-06 | GithubAgents | CLI: excludes api-engineer | Not in result |
| GAG-07 | GithubAgents | Event-driven: includes event-engineer | `event-engineer.md` |
| GAG-08 | GithubAgents | Event-consumer: includes event-engineer | `event-engineer.md` |
| GAG-09 | GithubAgents | Event-producer: includes event-engineer | `event-engineer.md` |
| GAG-10 | GithubAgents | No events: excludes event-engineer | Not in result |
| GAG-11 | GithubAgents | Parametrized: infra fields include devops | it.each 4 combos |
| GAG-12 | GithubAgents | Core agents: `.agent.md` extension | Extension check |
| GAG-13 | GithubAgents | Conditional: copied when selected | File exists |
| GAG-14 | GithubAgents | Developer: copied by language | File exists |
| GAG-15 | GithubAgents | Missing conditional template: warning | Warning message |
| GAG-16 | GithubAgents | Missing developer template: warning | Warning message |
| GAG-17 | GithubAgents | Placeholder replacement | `{project_name}` replaced |
| GAG-18 | GithubAgents | All output files: `.agent.md` | Extension check |
| GAG-19 | GithubAgents | Full combination: all agent types | All categories |
| GAG-20 | GithubAgents | Core agents: sorted alphabetically | Sort order |
| GAG-21 | GithubAgents | Agents dir auto-created | Dir exists |
| GPR-01 | GithubPrompts | 4 prompts generated | `result.length === 4` |
| GPR-02 | GithubPrompts | Templates dir missing | `result === []` |
| GPR-03 | GithubPrompts | Individual template missing: skip | `result.length === 2` |
| GPR-04 | GithubPrompts | `.j2` suffix removed | Output name check |
| GPR-05 | GithubPrompts | Nunjucks rendering (not placeholders) | `{{ }}` rendered |
| GPR-06 | GithubPrompts | No raw placeholders in output | No `{{` in output |
| GPR-07 | GithubPrompts | Prompts dir auto-created | Dir exists |
| GPR-08 | GithubPrompts | Full Nunjucks syntax (conditionals) | Rendered correctly |
| PAR-01 | Parity | GithubHooks: 3 JSON files | Byte-identical |
| PAR-02 | Parity | GithubMcp: 2 servers JSON | Structure match |
| PAR-03 | Parity | GithubInstructions: full content | Stack table + constraints |
| PAR-04 | Parity | GithubSkills: infra filtering | Conditional match |
| PAR-05 | Parity | GithubAgents: full config | All agent types |
| PAR-06 | Parity | GithubPrompts: 4 rendered | No raw placeholders |

---

## Notes

1. **GithubHooksAssembler uses `fs.copyFileSync`:** Unlike the Claude
   `HooksAssembler` which copies shell scripts with executable permissions,
   the GitHub variant copies JSON files verbatim. No `chmod` is needed.

2. **GithubMcpAssembler returns `AssembleResult`:** The `warnLiteralEnvValues`
   function returns warning strings (not logging) for testability. Python used
   `logger.warning()`, but TypeScript collects warnings in the result object
   following the pattern from `AgentsAssembler` and `RulesAssembler`.

3. **GithubAgentsAssembler output extension:** Output files use `.agent.md`
   extension (GitHub convention), not `.md` (Claude convention). The template
   stem (e.g., `architect`) is extracted and the output filename becomes
   `architect.agent.md`. Test GAG-18 specifically validates this.

4. **GithubPromptsAssembler uses full Nunjucks rendering:** This is the only
   GitHub assembler that calls `engine.renderTemplate()` instead of
   `engine.replacePlaceholders()`. Templates use `{{ variable }}` and
   `{% control %}` syntax. Tests must create `.j2` files in the temp
   `resourcesDir` so the `TemplateEngine` Nunjucks loader can find them.

5. **Interface type formatting:** The `buildCopilotInstructions` function
   uppercases `rest` and `grpc` but leaves other interface types as-is. Python
   uses the same logic. Tests GIN-02 through GIN-04 verify this behavior.

6. **Boolean formatting:** Python uses `str(bool).lower()` producing
   `"true"/"false"`. TypeScript must produce identical output. Test GIN-09
   verifies lowercase boolean rendering.

7. **`it.each` usage:** Tests GAG-11 and the filterSkills group use
   parametrized tests via `it.each` to reduce boilerplate while covering all
   condition combinations.

8. **Parity tests use real resources:** PAR-01 through PAR-06 run against the
   project's actual `resources/` directory to catch template file discrepancies
   between what tests expect and what exists on disk.

9. **Total test count:** 75 tests (6 GithubHooks + 14 GithubMcp +
   20 GithubInstructions + 15 GithubSkills + 21 GithubAgents +
   8 GithubPrompts + 6 Parity). This covers all branches identified in the
   implementation plan and all edge cases from the Python source.

10. **`selectGithubConditionalAgents` reuses `hasAnyInterface`:** The function
    imports and uses `hasAnyInterface` from `src/assembler/conditions.ts`
    (already migrated in STORY-008) for interface-based agent selection. No
    need to re-test `hasAnyInterface` itself -- only its integration with
    the selection logic.
