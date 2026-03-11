# Test Plan -- STORY-021: Codex Nunjucks Templates

## Summary

- New test file: `tests/node/codex-templates.test.ts`
- Total test methods: 52
- Categories: Unit (section rendering), Conditional rendering, Orchestrator integration, TOML validity, Snapshot, Edge cases
- Coverage targets: >= 95% line, >= 90% branch
- Templates under test: 13 Nunjucks templates in `resources/codex-templates/`
- Performance budget: < 3s (pure in-memory rendering, no file I/O beyond template loading)

---

## 1. Test File Location and Naming

**Path:** `tests/node/codex-templates.test.ts`

**Rationale:** Follows existing convention (`tests/node/template-engine.test.ts`). Codex templates are resource files rendered by `TemplateEngine`, so tests live alongside the existing template engine tests rather than under `tests/node/assembler/`.

**Naming convention:** `[methodUnderTest]_[scenario]_[expectedBehavior]` per Rule 05.

---

## 2. Test Infrastructure

### 2.1 Engine Setup

The test file uses the real `resources/` directory (read-only) as the Nunjucks `FileSystemLoader` root. Templates are static files and `renderTemplate()` does not write to disk, so no temp directory management is needed.

```typescript
import { resolve } from "node:path";
import { TemplateEngine, buildDefaultContext } from "../../src/template-engine.js";
import { aProjectConfig } from "../fixtures/project-config.fixture.js";

const RESOURCES_DIR = resolve(__dirname, "../../resources");
```

### 2.2 Engine Instantiation

A shared `TemplateEngine` instance is created per describe block using `aProjectConfig()` as the base config. Context overrides are passed via `renderTemplate(path, overrides)`.

---

## 3. Test Fixtures

### 3.1 Full Context (all conditional sections enabled)

```typescript
function fullContext(): Record<string, unknown> {
  return {
    ...buildDefaultContext(aProjectConfig()),
    observability: "opentelemetry",
    resolved_stack: {
      buildCmd: "npm run build",
      testCmd: "npm test",
      compileCmd: "npx tsc --noEmit",
      coverageCmd: "npm run test:coverage",
    },
    agents_list: [
      { name: "architect", description: "System architect" },
      { name: "tech-lead", description: "Technical lead" },
      { name: "qa-engineer", description: "Quality assurance" },
    ],
    skills_list: [
      { name: "x-dev-implement", description: "Implement features", user_invocable: true },
      { name: "x-review", description: "Code review", user_invocable: true },
      { name: "x-lib-audit", description: "Internal audit", user_invocable: false },
    ],
    has_hooks: true,
    mcp_servers: [
      {
        id: "firecrawl",
        command: ["npx", "firecrawl-mcp"],
        env: { API_KEY: "test-key-xxx" },
      },
    ],
    security_frameworks: ["owasp", "pci-dss"],
    model: "o4-mini",
    approval_policy: "on-request",
    sandbox_mode: "workspace-write",
  };
}
```

**Key properties:**
- `domain_driven` is `"True"` (from `aProjectConfig()` which uses `ArchitectureConfig("hexagonal", true, false)`)
- `database_name` is `"postgresql"`, `cache_name` is `"redis"` (non-"none" values)
- `orchestrator` is `"kubernetes"` (non-"none")
- `security_frameworks` has 2 entries (security section included)
- `agents_list` has 3 entries (agents section included)
- `skills_list` has 3 entries, 2 user-invocable, 1 non-invocable (skills section included)
- `mcp_servers` has 1 entry (MCP section in config.toml included)
- `has_hooks` is `true` (approval_policy = "on-request")

### 3.2 Minimal Context (conditional sections disabled)

```typescript
function minimalContext(): Record<string, unknown> {
  const config = aMinimalProjectConfig();
  return {
    ...buildDefaultContext(config),
    observability: "none",
    resolved_stack: {
      buildCmd: "go build ./...",
      testCmd: "go test ./...",
      compileCmd: "go vet ./...",
      coverageCmd: "go test -coverprofile=coverage.out ./...",
    },
    agents_list: [],
    skills_list: [],
    has_hooks: false,
    mcp_servers: [],
    security_frameworks: [],
    model: "o4-mini",
    approval_policy: "untrusted",
    sandbox_mode: "workspace-write",
  };
}
```

**Key properties:**
- `domain_driven` is `"False"` (domain section excluded)
- `database_name` is `"none"`, `cache_name` is `"none"` (rows omitted from tech-stack)
- `orchestrator` is `"none"` (row omitted from tech-stack)
- `observability` is `"none"` (row omitted from tech-stack)
- `security_frameworks` is `[]` (security section excluded)
- `agents_list` is `[]` (agents section excluded)
- `skills_list` is `[]` (skills section excluded)
- `mcp_servers` is `[]` (MCP section in config.toml excluded)
- `has_hooks` is `false` (approval_policy = "untrusted")

### 3.3 New Fixture: `aMinimalProjectConfig`

Add to `tests/fixtures/project-config.fixture.ts`:

```typescript
export function aMinimalProjectConfig(): ProjectConfig {
  return new ProjectConfig(
    new ProjectIdentity("minimal-api", "A minimal API service"),
    new ArchitectureConfig("layered", false, false),
    [new InterfaceConfig("rest")],
    new LanguageConfig("go", "1.22"),
    new FrameworkConfig("gin", "1.9", "go"),
    new DataConfig(), // database="none", cache="none"
    new InfraConfig("docker", "none"), // orchestrator="none"
    new SecurityConfig(), // frameworks=[]
    new TestingConfig(false, false, false, 80, 70),
  );
}
```

---

## 4. Test Groups

### Group 1: Section Templates -- Individual Rendering (15 tests)

Each section template is rendered individually via `engine.renderTemplate("codex-templates/sections/X.md.njk", context)` with `fullContext()`.

| # | Test Name | Template | Context | Assertion |
|---|-----------|----------|---------|-----------|
| 1 | `header_fullContext_rendersProjectNameAndPurpose` | `sections/header.md.njk` | full | Contains `# my-service` and `"A sample service"` |
| 2 | `architecture_fullContext_rendersStyleAndLanguage` | `sections/architecture.md.njk` | full | Contains `"hexagonal"`, `"python"`, `"3.9"` |
| 3 | `techStack_fullContext_rendersAllRows` | `sections/tech-stack.md.njk` | full | Contains `"postgresql"`, `"redis"`, `"docker"`, `"kubernetes"` |
| 4 | `techStack_databaseNone_omitsDatabaseRow` | `sections/tech-stack.md.njk` | full + `database_name: "none"` | Does NOT contain `"Database"` table row |
| 5 | `techStack_cacheNone_omitsCacheRow` | `sections/tech-stack.md.njk` | full + `cache_name: "none"` | Does NOT contain `"Cache"` table row |
| 6 | `techStack_orchestratorNone_omitsOrchestratorRow` | `sections/tech-stack.md.njk` | full + `orchestrator: "none"` | Does NOT contain `"Orchestrator"` table row |
| 7 | `techStack_observabilityNone_omitsObservabilityRow` | `sections/tech-stack.md.njk` | full + `observability: "none"` | Does NOT contain `"Observability"` table row |
| 8 | `commands_fullContext_rendersAllCommands` | `sections/commands.md.njk` | full | Contains `"npm run build"`, `"npm test"`, `"npx tsc --noEmit"`, `"npm run test:coverage"` |
| 9 | `codingStandards_fullContext_rendersHardLimits` | `sections/coding-standards.md.njk` | full | Contains `"25 lines"`, `"250 lines"`, `"python"` |
| 10 | `qualityGates_fullContext_rendersCoverageThresholds` | `sections/quality-gates.md.njk` | full | Contains `"95"` (coverage_line), `"90"` (coverage_branch) |
| 11 | `domain_fullContext_rendersDomainSection` | `sections/domain.md.njk` | full | Contains `"## Domain"` or equivalent domain heading |
| 12 | `security_fullContext_rendersFrameworks` | `sections/security.md.njk` | full | Contains `"owasp"`, `"pci-dss"` |
| 13 | `conventions_fullContext_rendersCommitConventions` | `sections/conventions.md.njk` | full | Contains `"Conventional Commits"` |
| 14 | `skills_fullContext_rendersUserInvocableOnly` | `sections/skills.md.njk` | full | Contains `"x-dev-implement"`, `"x-review"`; does NOT contain `"x-lib-audit"` |
| 15 | `agents_fullContext_rendersAgentsTable` | `sections/agents.md.njk` | full | Contains `"architect"`, `"tech-lead"`, `"qa-engineer"` |

#### Setup (shared)

```typescript
const engine = new TemplateEngine(RESOURCES_DIR, aProjectConfig());
const ctx = fullContext();
const result = engine.renderTemplate("codex-templates/sections/header.md.njk", ctx);
```

#### Assertions Pattern

```typescript
// Test 1
expect(result).toContain("# my-service");
expect(result).toContain("A sample service");

// Test 14 -- skills filtering
expect(result).toContain("x-dev-implement");
expect(result).toContain("x-review");
expect(result).not.toContain("x-lib-audit");
```

---

### Group 2: Orchestrator Template -- Full Context (4 tests)

Renders `codex-templates/agents-md.md.njk` with `fullContext()` and validates the composed output.

| # | Test Name | Assertion |
|---|-----------|-----------|
| 16 | `agentsMd_fullContext_containsAllSections` | Contains headings for: Header (project name), Architecture, Tech Stack, Commands, Coding Standards, Quality Gates, Domain, Security, Conventions, Skills, Agents |
| 17 | `agentsMd_fullContext_noTemplateArtifacts` | Does NOT match `/{[{%#]/` (no unrendered `{{`, `{%`, `{#` tags) |
| 18 | `agentsMd_fullContext_startsWithProjectTitle` | Starts with `# my-service` |
| 19 | `agentsMd_fullContext_noExcessiveBlankLines` | Does NOT contain 3+ consecutive blank lines (whitespace control validation) |

#### Setup

```typescript
const result = engine.renderTemplate("codex-templates/agents-md.md.njk", fullContext());
```

#### Assertions

```typescript
// Test 16 -- all sections present
expect(result).toContain("# my-service");
expect(result).toContain("## Architecture");
expect(result).toContain("## Tech Stack");
expect(result).toContain("## Commands");
expect(result).toContain("## Coding Standards");
expect(result).toContain("## Quality Gates");
expect(result).toContain("## Domain");
expect(result).toContain("## Security");
expect(result).toContain("## Conventions");
expect(result).toContain("## Available Skills");
expect(result).toContain("## Agent Personas");

// Test 17 -- no template artifacts
expect(result).not.toMatch(/\{\{/);
expect(result).not.toMatch(/\{%/);
expect(result).not.toMatch(/\{#/);

// Test 19 -- whitespace control
expect(result).not.toMatch(/\n{4,}/);
```

---

### Group 3: Orchestrator Template -- Minimal Context / Conditional Omission (5 tests)

Renders `codex-templates/agents-md.md.njk` with `minimalContext()` and validates conditional section omission.

| # | Test Name | Assertion |
|---|-----------|-----------|
| 20 | `agentsMd_minimalContext_omitsDomainSection` | Does NOT contain `"## Domain"` |
| 21 | `agentsMd_minimalContext_omitsSecuritySection` | Does NOT contain `"## Security"` |
| 22 | `agentsMd_minimalContext_omitsSkillsSection` | Does NOT contain `"## Available Skills"` |
| 23 | `agentsMd_minimalContext_omitsAgentsSection` | Does NOT contain `"## Agent Personas"` |
| 24 | `agentsMd_minimalContext_includesAlwaysPresentSections` | Contains: Architecture, Tech Stack, Commands, Coding Standards, Quality Gates, Conventions |

#### Setup

```typescript
const minEngine = new TemplateEngine(RESOURCES_DIR, aMinimalProjectConfig());
const result = minEngine.renderTemplate("codex-templates/agents-md.md.njk", minimalContext());
```

#### Assertions

```typescript
// Test 20
expect(result).not.toContain("## Domain");

// Test 24 -- always-present sections
expect(result).toContain("## Architecture");
expect(result).toContain("## Tech Stack");
expect(result).toContain("## Commands");
expect(result).toContain("## Coding Standards");
expect(result).toContain("## Quality Gates");
expect(result).toContain("## Conventions");
```

---

### Group 4: config.toml Template (8 tests)

Renders `codex-templates/config.toml.njk` with full and minimal contexts.

| # | Test Name | Context | Assertion |
|---|-----------|---------|-----------|
| 25 | `configToml_fullContext_rendersModelAndPolicy` | full | Contains `model = "o4-mini"` and `approval_policy = "on-request"` |
| 26 | `configToml_fullContext_rendersMcpServers` | full | Contains `[mcp_servers.firecrawl]` and `command = ["npx", "firecrawl-mcp"]` |
| 27 | `configToml_fullContext_rendersMcpEnvVars` | full | Contains `API_KEY = "test-key-xxx"` |
| 28 | `configToml_fullContext_rendersSandboxMode` | full | Contains `mode = "workspace-write"` |
| 29 | `configToml_noMcpServers_omitsMcpSection` | minimal | Does NOT contain `[mcp_servers` |
| 30 | `configToml_noHooks_usesUntrustedPolicy` | minimal | Contains `approval_policy = "untrusted"` |
| 31 | `configToml_fullContext_noTemplateArtifacts` | full | No `{{`, `{%`, `{#` in output |
| 32 | `configToml_fullContext_rendersProjectComment` | full | Contains `# Codex CLI configuration for my-service` |

#### Setup

```typescript
const result = engine.renderTemplate("codex-templates/config.toml.njk", fullContext());
```

#### Assertions

```typescript
// Test 25
expect(result).toContain('model = "o4-mini"');
expect(result).toContain('approval_policy = "on-request"');

// Test 26
expect(result).toContain("[mcp_servers.firecrawl]");

// Test 29
const minResult = minEngine.renderTemplate("codex-templates/config.toml.njk", minimalContext());
expect(minResult).not.toContain("[mcp_servers");

// Test 30
expect(minResult).toContain('approval_policy = "untrusted"');
```

---

### Group 5: TOML Validity (2 tests)

Validates that `config.toml.njk` output is structurally valid TOML.

| # | Test Name | Context | Assertion |
|---|-----------|---------|-----------|
| 33 | `configToml_fullContext_validTomlKeyValuePairs` | full | Every non-empty, non-comment line matches TOML key-value, section header, or array syntax |
| 34 | `configToml_fullContext_parsesWithTomlParser` | full | Output parses without errors using TOML parser (if `@iarna/toml` or `smol-toml` is available), or validates structure via regex-based line-by-line check |

#### Implementation Note

If the project does not include a TOML parser dependency, test 34 uses a line-by-line structural validation:

```typescript
// Regex-based TOML structure validation
const lines = result.split("\n");
for (const line of lines) {
  const trimmed = line.trim();
  if (trimmed === "" || trimmed.startsWith("#")) continue;
  const isKeyValue = /^[\w.]+\s*=\s*.+$/.test(trimmed);
  const isSectionHeader = /^\[[\w.]+\]$/.test(trimmed);
  const isArrayValue = /^\[.*\]$/.test(trimmed);
  expect(isKeyValue || isSectionHeader || isArrayValue).toBe(true);
}
```

If a TOML parser is available:

```typescript
import { parse } from "smol-toml"; // or @iarna/toml
expect(() => parse(result)).not.toThrow();
```

---

### Group 6: Snapshot Tests (4 tests)

Capture full rendered output as snapshots for regression detection.

| # | Test Name | Context | Template |
|---|-----------|---------|----------|
| 35 | `agentsMd_fullContext_matchesSnapshot` | full | `agents-md.md.njk` |
| 36 | `agentsMd_minimalContext_matchesSnapshot` | minimal | `agents-md.md.njk` |
| 37 | `configToml_fullContext_matchesSnapshot` | full | `config.toml.njk` |
| 38 | `configToml_minimalContext_matchesSnapshot` | minimal | `config.toml.njk` |

#### Setup

```typescript
const result = engine.renderTemplate("codex-templates/agents-md.md.njk", fullContext());
expect(result).toMatchSnapshot();
```

#### First Run

Snapshots are generated on first run with `npx vitest run -u tests/node/codex-templates.test.ts`. Subsequent runs validate against the stored snapshot.

#### Snapshot Update Workflow

When templates are intentionally modified:
1. Run `npx vitest run tests/node/codex-templates.test.ts` -- expect failure.
2. Review diff to confirm changes are intentional.
3. Run `npx vitest run -u tests/node/codex-templates.test.ts` to update snapshots.
4. Commit updated `.snap` file alongside template changes.

---

### Group 7: Edge Cases -- Empty Arrays (4 tests)

Tests behavior when array context variables are empty (but present).

| # | Test Name | Context Override | Template | Assertion |
|---|-----------|-----------------|----------|-----------|
| 39 | `skills_emptyList_rendersNoRows` | `skills_list: []` | `sections/skills.md.njk` | Contains heading but no table data rows |
| 40 | `agents_emptyList_rendersNoRows` | `agents_list: []` | `sections/agents.md.njk` | Contains heading but no table data rows |
| 41 | `security_emptyFrameworks_rendersNoItems` | `security_frameworks: []` | `sections/security.md.njk` | Contains heading but no framework items listed |
| 42 | `configToml_emptyMcpServers_noMcpSection` | `mcp_servers: []` | `config.toml.njk` | No `[mcp_servers` text in output |

#### Note

Tests 39-41 render section templates directly (not via orchestrator). This validates that even if the orchestrator guard is bypassed, sections degrade gracefully with empty arrays. The `{% for %}` loop over an empty array produces no output, which is correct behavior.

---

### Group 8: Edge Cases -- "none" Value Handling (4 tests)

Tests that `"none"` values cause conditional row omission in tech-stack and observability.

| # | Test Name | Context Override | Template | Assertion |
|---|-----------|-----------------|----------|-----------|
| 43 | `techStack_allNone_rendersOnlyLanguageAndFramework` | `database_name: "none"`, `cache_name: "none"`, `orchestrator: "none"`, `observability: "none"` | `sections/tech-stack.md.njk` | Contains Language and Framework rows; does NOT contain Database, Cache, Orchestrator, Observability rows |
| 44 | `techStack_someNone_rendersNonNoneRowsOnly` | `database_name: "none"`, `cache_name: "redis"`, `orchestrator: "none"`, `observability: "opentelemetry"` | `sections/tech-stack.md.njk` | Contains Cache and Observability rows; does NOT contain Database, Orchestrator rows |
| 45 | `techStack_fullValues_rendersAllConditionalRows` | full (all non-"none") | `sections/tech-stack.md.njk` | Contains Database, Cache, Orchestrator, Observability rows |
| 46 | `techStack_containerAlwaysPresent_rendersContainerRow` | `orchestrator: "none"` (container still "docker") | `sections/tech-stack.md.njk` | Contains "docker" in Container row regardless of orchestrator |

---

### Group 9: Edge Cases -- Quality Gates Conditional Rows (3 tests)

Tests conditional test category rows based on boolean string values.

| # | Test Name | Context Override | Template | Assertion |
|---|-----------|-----------------|----------|-----------|
| 47 | `qualityGates_smokeTestsTrue_rendersSmokeLine` | `smoke_tests: "True"` | `sections/quality-gates.md.njk` | Contains "Smoke" test reference |
| 48 | `qualityGates_contractTestsFalse_omitsContractLine` | `contract_tests: "False"` | `sections/quality-gates.md.njk` | Does NOT contain "Contract" test reference (if template conditionally renders it) OR contains it as disabled |
| 49 | `qualityGates_customThresholds_rendersCustomValues` | `coverage_line: 80, coverage_branch: 70` | `sections/quality-gates.md.njk` | Contains `"80"` and `"70"` |

---

### Group 10: Edge Cases -- Multiple MCP Servers (2 tests)

Tests config.toml rendering with multiple MCP server entries.

| # | Test Name | Context Override | Template | Assertion |
|---|-----------|-----------------|----------|-----------|
| 50 | `configToml_multipleMcpServers_rendersAllSections` | 2 MCP servers with different IDs | `config.toml.njk` | Contains both `[mcp_servers.server1]` and `[mcp_servers.server2]` |
| 51 | `configToml_mcpServerMultipleEnvVars_rendersAllVars` | 1 MCP server with 2 env vars | `config.toml.njk` | Contains both env var key-value pairs |

#### Setup (Test 50)

```typescript
const ctx = fullContext();
ctx.mcp_servers = [
  { id: "firecrawl", command: ["npx", "firecrawl-mcp"], env: { API_KEY: "key1" } },
  { id: "github", command: ["npx", "github-mcp"], env: { TOKEN: "ghp_xxx" } },
];
const result = engine.renderTemplate("codex-templates/config.toml.njk", ctx);
expect(result).toContain("[mcp_servers.firecrawl]");
expect(result).toContain("[mcp_servers.github]");
```

---

### Group 11: throwOnUndefined Compliance (1 test)

Validates that the Nunjucks `throwOnUndefined: true` configuration causes errors for missing required context variables.

| # | Test Name | Context Override | Template | Assertion |
|---|-----------|-----------------|----------|-----------|
| 52 | `agentsMd_missingRequiredVariable_throwsError` | Incomplete context (missing `project_name`) | `agents-md.md.njk` | Throws error mentioning undefined variable |

#### Setup

```typescript
const incompleteCtx = { ...fullContext() };
delete incompleteCtx.project_name;
expect(() =>
  engine.renderTemplate("codex-templates/agents-md.md.njk", incompleteCtx),
).toThrow();
```

---

## 5. Coverage Strategy

### 5.1 Branch Coverage Matrix

Every `{% if %}` guard in the templates is exercised in both true and false states:

| Template Condition | True Test IDs | False Test IDs |
|--------------------|--------------|----------------|
| `domain_driven == "True"` | 16 (full context) | 20 (minimal context) |
| `security_frameworks and security_frameworks.length > 0` | 12, 16 | 21, 41 |
| `skills_list and skills_list.length > 0` | 14, 16 | 22, 39 |
| `agents_list and agents_list.length > 0` | 15, 16 | 23, 40 |
| `database_name != "none"` | 3, 45 | 4, 43 |
| `cache_name != "none"` | 3, 45 | 5, 43 |
| `orchestrator != "none"` | 3, 45 | 6, 43 |
| `observability != "none"` | 3, 45 | 7, 43 |
| `mcp_servers and mcp_servers.length > 0` | 26, 50 | 29, 42 |
| `skill.user_invocable` (in skills loop) | 14 (invocable skills) | 14 (non-invocable filtered) |

### 5.2 Line Coverage

Every template line is exercised by at least one test:
- Always-present sections (header, architecture, commands, coding-standards, quality-gates, conventions): Tests 1-3, 8-10, 13
- Conditional sections (domain, security, skills, agents): Tests 11-12, 14-15 (true path), 20-23 (false path)
- config.toml: Tests 25-32 (full), 29-30 (minimal)
- Iteration loops: Tests 12 (security frameworks), 14 (skills), 15 (agents), 26 (MCP servers)

### 5.3 Coverage Targets

| Metric | Target | Strategy |
|--------|--------|----------|
| Line | >= 95% | Full + minimal contexts exercise all template lines |
| Branch | >= 90% | Every `{% if %}` guard tested in both true and false states |

---

## 6. Test Matrix Summary

| Group | Description | Test Count | Type |
|-------|-------------|------------|------|
| G1: Section rendering | Individual section templates with full context | 15 | Unit |
| G2: Orchestrator full | agents-md.md.njk with full context | 4 | Unit |
| G3: Orchestrator minimal | agents-md.md.njk with minimal context (conditional omission) | 5 | Unit |
| G4: config.toml | config.toml.njk with full and minimal contexts | 8 | Unit |
| G5: TOML validity | Structural validation of TOML output | 2 | Unit |
| G6: Snapshots | Full output regression via toMatchSnapshot | 4 | Snapshot |
| G7: Empty arrays | Graceful degradation with empty array fields | 4 | Edge |
| G8: "none" values | Tech-stack row omission with "none" values | 4 | Edge |
| G9: Quality gates | Conditional test category rows | 3 | Edge |
| G10: Multiple MCP | config.toml with multiple MCP servers | 2 | Edge |
| G11: throwOnUndefined | Missing required variable causes error | 1 | Error |
| **Total** | | **52** | |

---

## 7. Execution Commands

### Run Codex Template Tests Only

```bash
npx vitest run tests/node/codex-templates.test.ts
```

### Generate/Update Snapshots

```bash
npx vitest run -u tests/node/codex-templates.test.ts
```

### Run with Coverage

```bash
npx vitest run --coverage tests/node/codex-templates.test.ts
```

### Full Test Suite (regression check)

```bash
npx vitest run
```

---

## 8. Dependencies and Prerequisites

### Prerequisites

- All 13 templates exist in `resources/codex-templates/` (created in implementation groups G1 and G2 of the story plan)
- `aMinimalProjectConfig()` fixture added to `tests/fixtures/project-config.fixture.ts`
- `resources/` directory is accessible from the test working directory

### Import Dependencies

| Module | Import | Used For |
|--------|--------|----------|
| `src/template-engine.ts` | `TemplateEngine`, `buildDefaultContext` | Rendering templates, building context |
| `src/models.ts` | `ProjectConfig`, model classes | Building test configs |
| `tests/fixtures/project-config.fixture.ts` | `aProjectConfig`, `aMinimalProjectConfig` | Fixture configs |
| `vitest` | `describe`, `it`, `expect` | Test framework |
| `node:path` | `resolve` | Path resolution |

### No Additional Dev Dependencies

- TOML validity test (G5) uses regex-based structural validation, avoiding the need to add a TOML parser dependency. If a TOML parser is later added to the project, test 34 can be upgraded to use it.

---

## 9. Vitest Configuration Notes

- The existing `vitest.config.ts` pattern `tests/**/*.test.ts` automatically discovers `tests/node/codex-templates.test.ts`. No config changes needed.
- Pool: `forks` with `maxForks: 3`, `maxConcurrency: 5` (project-wide setting). This test file is lightweight (pure rendering, no I/O) and will not stress memory.
- Expected duration: < 2s. Templates are small Markdown/TOML files rendered synchronously.

---

## 10. Risk Areas

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Include paths fail (FileSystemLoader root mismatch) | Medium | High | Test 16 validates full orchestrator rendering with includes. If includes fail, all orchestrator tests fail immediately. |
| `throwOnUndefined` failures for optional vars | Medium | High | Test 52 validates error on missing required var. All optional vars are guarded with `{% if %}` in orchestrator. |
| Whitespace accumulation in Markdown output | Medium | Medium | Test 19 validates no excessive blank lines. Snapshot tests (G6) catch whitespace regressions. |
| TOML syntax errors in config.toml output | Medium | Medium | Tests 33-34 validate TOML structure. Snapshot tests catch regressions. |
| `aMinimalProjectConfig` fixture values conflict with existing tests | Low | Low | New fixture uses distinct values (`minimal-api`, `go`, `gin`) that do not overlap with `aProjectConfig()`. |
| Nunjucks object iteration for MCP env vars | Low | Medium | Test 27 validates env var rendering. Test 51 validates multiple env vars. |

---

## 11. Naming Convention Reference

All test names follow `[methodUnderTest]_[scenario]_[expectedBehavior]`:

```
header_fullContext_rendersProjectNameAndPurpose
techStack_databaseNone_omitsDatabaseRow
agentsMd_fullContext_containsAllSections
agentsMd_minimalContext_omitsDomainSection
configToml_fullContext_rendersModelAndPolicy
configToml_noMcpServers_omitsMcpSection
configToml_fullContext_validTomlKeyValuePairs
agentsMd_fullContext_matchesSnapshot
skills_emptyList_rendersNoRows
techStack_allNone_rendersOnlyLanguageAndFramework
qualityGates_customThresholds_rendersCustomValues
configToml_multipleMcpServers_rendersAllSections
agentsMd_missingRequiredVariable_throwsError
```
