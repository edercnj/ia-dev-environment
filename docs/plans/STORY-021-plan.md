# Implementation Plan -- STORY-021: Codex Nunjucks Templates

## Story Summary

Create 13 Nunjucks templates in `resources/codex-templates/` for generating OpenAI Codex CLI artifacts: a consolidated `AGENTS.md` (1 orchestrator template + 10 modular sections) and a `config.toml`. Templates consume the existing 24-field flat context plus extended context variables (ResolvedStack, agents/skills lists, MCP servers). No source code changes -- this story is pure template creation plus unit tests for rendering.

**Blocked by:** EPIC-001/STORY-005 (Template Engine) -- complete.
**Blocks:** STORY-022 (CodexAgentsMdAssembler), STORY-023 (CodexConfigAssembler).

---

## 1. Affected Layers and Components

| Layer | Component | Action | Path |
|-------|-----------|--------|------|
| resources | codex-templates directory | **Create** | `resources/codex-templates/` |
| resources | codex-templates/sections directory | **Create** | `resources/codex-templates/sections/` |
| resources | agents-md orchestrator template | **Create** | `resources/codex-templates/agents-md.md.njk` |
| resources | header section | **Create** | `resources/codex-templates/sections/header.md.njk` |
| resources | architecture section | **Create** | `resources/codex-templates/sections/architecture.md.njk` |
| resources | tech-stack section | **Create** | `resources/codex-templates/sections/tech-stack.md.njk` |
| resources | commands section | **Create** | `resources/codex-templates/sections/commands.md.njk` |
| resources | coding-standards section | **Create** | `resources/codex-templates/sections/coding-standards.md.njk` |
| resources | quality-gates section | **Create** | `resources/codex-templates/sections/quality-gates.md.njk` |
| resources | domain section (conditional) | **Create** | `resources/codex-templates/sections/domain.md.njk` |
| resources | security section (conditional) | **Create** | `resources/codex-templates/sections/security.md.njk` |
| resources | conventions section | **Create** | `resources/codex-templates/sections/conventions.md.njk` |
| resources | skills section (conditional) | **Create** | `resources/codex-templates/sections/skills.md.njk` |
| resources | agents section (conditional) | **Create** | `resources/codex-templates/sections/agents.md.njk` |
| resources | config.toml template | **Create** | `resources/codex-templates/config.toml.njk` |
| tests | codex template rendering tests | **Create** | `tests/node/codex-templates.test.ts` |

**No existing source files are modified.** This is purely additive: 13 new template files + 1 new test file.

---

## 2. New Files to Create

### 2.1 Directory Structure

```
resources/codex-templates/
|-- agents-md.md.njk                  (orchestrator -- includes sections)
|-- config.toml.njk                   (standalone TOML template)
+-- sections/
    |-- header.md.njk
    |-- architecture.md.njk
    |-- tech-stack.md.njk
    |-- commands.md.njk
    |-- coding-standards.md.njk
    |-- quality-gates.md.njk
    |-- domain.md.njk
    |-- security.md.njk
    |-- conventions.md.njk
    |-- skills.md.njk
    +-- agents.md.njk
```

### 2.2 Template File Descriptions

#### `agents-md.md.njk` (Orchestrator)

The main template that produces the complete `AGENTS.md` output. Uses `{% include %}` directives to compose sections. Conditional sections are guarded by `{% if %}` blocks:

```nunjucks
{# Codex AGENTS.md -- consolidated project instructions #}
{% include "codex-templates/sections/header.md.njk" %}

{% include "codex-templates/sections/architecture.md.njk" %}

{% include "codex-templates/sections/tech-stack.md.njk" %}

{% include "codex-templates/sections/commands.md.njk" %}

{% include "codex-templates/sections/coding-standards.md.njk" %}

{% include "codex-templates/sections/quality-gates.md.njk" %}

{% if domain_driven == "True" %}
{% include "codex-templates/sections/domain.md.njk" %}
{% endif %}

{% if security_frameworks and security_frameworks.length > 0 %}
{% include "codex-templates/sections/security.md.njk" %}
{% endif %}

{% include "codex-templates/sections/conventions.md.njk" %}

{% if skills_list and skills_list.length > 0 %}
{% include "codex-templates/sections/skills.md.njk" %}
{% endif %}

{% if agents_list and agents_list.length > 0 %}
{% include "codex-templates/sections/agents.md.njk" %}
{% endif %}
```

**Design decisions:**
- Include paths are relative to `resourcesDir` (the Nunjucks FileSystemLoader root), so they must use `codex-templates/sections/` prefix.
- The orchestrator uses `{% include %}` not `{% block %}` because sections are self-contained Markdown fragments with no inheritance hierarchy.
- Whitespace control: Since `trimBlocks` and `lstripBlocks` are both `false` in the TemplateEngine config, the orchestrator must manage blank lines carefully. Each `{% include %}` is followed by a blank line separator. Guard conditionals (`{% if %}`) use the `-%}` trim syntax on closing tags where needed to avoid excess blank lines in output.

#### `sections/header.md.njk`

**Context consumed:** `project_name`, `project_purpose`

Renders the top of AGENTS.md with project title and purpose.

```markdown
# {{ project_name }}

{{ project_purpose }}
```

#### `sections/architecture.md.njk`

**Context consumed:** `architecture_style`, `language_name`, `language_version`, `framework_name`, `framework_version`

Renders architecture style, package structure, and dependency direction rules.

#### `sections/tech-stack.md.njk`

**Context consumed:** `language_name`, `language_version`, `framework_name`, `framework_version`, `build_tool`, `database_name`, `cache_name`, `container`, `orchestrator`, `observability`

Renders a table of technologies. Rows for database, cache, orchestrator, and observability are conditionally omitted when their value is `"none"`. Uses `{% if field != "none" %}` guards per row.

**Note on `observability`:** The existing `buildDefaultContext()` returns 24 fields and does not include a flat `observability` field. The story lists 25 fields but `observability` is actually nested under `InfraConfig.observability` as an `ObservabilityConfig` object with `tool`, `metrics`, `tracing` subfields. For this template, the assembler (STORY-022) will need to provide `observability` as a flat string in the extended context. The template should accept `observability` as a string and conditionally render it.

#### `sections/commands.md.njk`

**Context consumed:** `resolved_stack` (object with `buildCmd`, `testCmd`, `compileCmd`, `coverageCmd`)

Renders build/test/compile/coverage commands from the ResolvedStack.

```markdown
## Commands

| Command | Script |
|---------|--------|
| Build | `{{ resolved_stack.buildCmd }}` |
| Test | `{{ resolved_stack.testCmd }}` |
| Compile | `{{ resolved_stack.compileCmd }}` |
| Coverage | `{{ resolved_stack.coverageCmd }}` |
```

#### `sections/coding-standards.md.njk`

**Context consumed:** `language_name`, `language_version`

Renders coding standards: hard limits (method length <= 25 lines, class <= 250 lines, parameters <= 4, line width <= 120), SOLID one-liners, error handling rules, and forbidden patterns. Content derived from Rule 03 (`resources/core-rules/03-coding-standards.md`).

#### `sections/quality-gates.md.njk`

**Context consumed:** `coverage_line`, `coverage_branch`, `smoke_tests`, `contract_tests`, `performance_tests`

Renders coverage thresholds, test categories, test naming convention, and merge checklist. Content derived from Rule 05 (`resources/core-rules/05-quality-gates.md`). Conditional rows for smoke/contract/performance tests based on their boolean string values.

#### `sections/domain.md.njk`

**Context consumed:** (none beyond what the orchestrator guards with `domain_driven`)

Renders domain model placeholders. This section is only included when `domain_driven == "True"`. The actual domain content is project-specific and cannot be generated from config alone, so this template provides structural scaffolding with guidance comments.

#### `sections/security.md.njk`

**Context consumed:** `security_frameworks` (array of strings)

Renders security guidelines based on the configured security frameworks. Only included when `security_frameworks` is non-empty. Iterates over the array to list each framework.

#### `sections/conventions.md.njk`

**Context consumed:** `language_name`

Renders project conventions: commit format (Conventional Commits), branch naming, code language (English), documentation language.

#### `sections/skills.md.njk`

**Context consumed:** `skills_list` (array of `{ name, description, user_invocable }`)

Renders a table of available skills. Filters to only show user-invocable skills (where `user_invocable` is true). Only included when `skills_list` is non-empty.

```nunjucks
## Available Skills

| Skill | Description |
|-------|-------------|
{% for skill in skills_list %}{% if skill.user_invocable %}| {{ skill.name }} | {{ skill.description }} |
{% endif %}{% endfor %}
```

#### `sections/agents.md.njk`

**Context consumed:** `agents_list` (array of `{ name, description }`)

Renders a table of agent personas. Only included when `agents_list` is non-empty.

```nunjucks
## Agent Personas

| Agent | Role |
|-------|------|
{% for agent in agents_list %}| {{ agent.name }} | {{ agent.description }} |
{% endfor %}
```

#### `config.toml.njk`

**Context consumed:** `model`, `approval_policy`, `sandbox_mode`, `project_name`, `mcp_servers` (optional array)

Renders a valid TOML configuration file for Codex CLI. The MCP servers section is conditionally included.

```nunjucks
# Codex CLI configuration for {{ project_name }}
# Generated by ia-dev-environment -- do not edit manually.

model = "{{ model }}"
approval_policy = "{{ approval_policy }}"

[sandbox]
mode = "{{ sandbox_mode }}"
{% if mcp_servers and mcp_servers.length > 0 %}
{% for server in mcp_servers %}

[mcp_servers.{{ server.id }}]
command = [{% for part in server.command %}"{{ part }}"{% if not loop.last %}, {% endif %}{% endfor %}]
{% for key, value in server.env %}
[mcp_servers.{{ server.id }}.env]
{{ key }} = "{{ value }}"
{% endfor %}
{% endfor %}
{% endif %}
```

---

## 3. Template Design Decisions

### 3.1 File Extension: `.njk` vs `.j2`

The story specifies `.njk` extension. The existing github-prompts-templates use `.j2` (a Jinja2 convention inherited from the Python predecessor). For Codex templates, `.njk` is used because:
- It signals these are Nunjucks-native templates (not Python Jinja2 ports)
- It distinguishes new templates from legacy ones
- The TemplateEngine does not care about file extension -- it renders any file passed to `renderTemplate()`

### 3.2 Include Path Strategy

Nunjucks `{% include %}` resolves paths relative to the FileSystemLoader root, which is `resourcesDir`. Therefore, includes must use the full relative path from `resourcesDir`:

```nunjucks
{% include "codex-templates/sections/header.md.njk" %}
```

Not:
```nunjucks
{% include "sections/header.md.njk" %}  {# WRONG -- would look in resourcesDir/sections/ #}
```

### 3.3 Whitespace Management

The TemplateEngine is configured with `trimBlocks: false` and `lstripBlocks: false`. This means:
- `{% %}` tags produce a newline after them
- Block tags do not strip leading whitespace

To produce clean Markdown output:
- Use `{%- -%}` trim markers on conditional tags where blank lines would accumulate
- Place includes on their own lines with explicit blank line separators
- Test output carefully against expected Markdown structure

### 3.4 throwOnUndefined Compliance

The TemplateEngine sets `throwOnUndefined: true`. Every variable referenced in a template MUST be provided in the rendering context. This means:
- Optional sections must be guarded with `{% if variable %}` before accessing the variable
- Array iteration with `{% for x in arr %}` is safe if `arr` is `[]` (produces no output)
- Direct access like `{{ mcp_servers.length }}` requires `mcp_servers` to exist

The orchestrator template guards all optional sections with `{% if %}` before including the section template, so section templates can safely access their variables without additional guards.

### 3.5 Context Variable Mapping

| Template Variable | Source | Type | Notes |
|-------------------|--------|------|-------|
| `project_name` | `buildDefaultContext()` flat field | string | Always present |
| `project_purpose` | `buildDefaultContext()` flat field | string | Always present |
| `language_name` | `buildDefaultContext()` flat field | string | Always present |
| `language_version` | `buildDefaultContext()` flat field | string | Always present |
| `framework_name` | `buildDefaultContext()` flat field | string | Always present |
| `framework_version` | `buildDefaultContext()` flat field | string | May be empty string |
| `build_tool` | `buildDefaultContext()` flat field | string | Always present |
| `architecture_style` | `buildDefaultContext()` flat field | string | Always present |
| `domain_driven` | `buildDefaultContext()` flat field | string | `"True"` or `"False"` |
| `event_driven` | `buildDefaultContext()` flat field | string | `"True"` or `"False"` |
| `database_name` | `buildDefaultContext()` flat field | string | `"none"` when absent |
| `cache_name` | `buildDefaultContext()` flat field | string | `"none"` when absent |
| `container` | `buildDefaultContext()` flat field | string | Always present |
| `orchestrator` | `buildDefaultContext()` flat field | string | `"none"` when absent |
| `coverage_line` | `buildDefaultContext()` flat field | number | Default 95 |
| `coverage_branch` | `buildDefaultContext()` flat field | number | Default 90 |
| `smoke_tests` | `buildDefaultContext()` flat field | string | `"True"` or `"False"` |
| `contract_tests` | `buildDefaultContext()` flat field | string | `"True"` or `"False"` |
| `performance_tests` | `buildDefaultContext()` flat field | string | `"True"` or `"False"` |
| `observability` | Extended context (assembler) | string | `"none"` when absent |
| `resolved_stack` | Extended context (assembler) | object | `{ buildCmd, testCmd, compileCmd, coverageCmd }` |
| `agents_list` | Extended context (assembler) | array | `[{ name, description }]` |
| `skills_list` | Extended context (assembler) | array | `[{ name, description, user_invocable }]` |
| `has_hooks` | Extended context (assembler) | boolean | Used to derive `approval_policy` |
| `mcp_servers` | Extended context (assembler) | array | `[{ id, command, env }]` |
| `security_frameworks` | Extended context (assembler) | array | `string[]`, may be empty |
| `model` | config.toml context (hardcoded) | string | `"o4-mini"` (RULE-103) |
| `approval_policy` | config.toml context (derived) | string | `"on-request"` or `"untrusted"` |
| `sandbox_mode` | config.toml context (hardcoded) | string | `"workspace-write"` (RULE-103) |

**Important:** The existing `buildDefaultContext()` returns 24 fields. The story references "25 fields" but this count includes `observability` which is not currently in the flat context. The assemblers (STORY-022/023) will add `observability` and the other extended fields to the context before calling `renderTemplate()`. Templates must be designed to work with the merged context.

---

## 4. Test Strategy

### 4.1 Test File

**Path:** `tests/node/codex-templates.test.ts`

### 4.2 Test Infrastructure

Following the established pattern from `tests/node/assembler/github-prompts-assembler.test.ts`:
- Create a temp directory structure in `beforeEach`
- Copy or symlink `resources/codex-templates/` into the temp `resourcesDir`
- Instantiate `TemplateEngine` with the temp `resourcesDir` and a test `ProjectConfig`
- Clean up in `afterEach`

Alternatively, use the real `resources/` directory (read-only) since templates are static files and `renderTemplate` does not write to disk. This avoids temp directory management for rendering tests.

### 4.3 Test Fixtures

Two fixture configurations are needed:

**Full config (all sections enabled):**
```typescript
function fullContext(): Record<string, unknown> {
  return {
    ...buildDefaultContext(aFullProjectConfig()),
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
      { id: "firecrawl", command: ["npx", "firecrawl-mcp"], env: { API_KEY: "xxx" } },
    ],
    security_frameworks: ["owasp", "pci-dss"],
    model: "o4-mini",
    approval_policy: "on-request",
    sandbox_mode: "workspace-write",
  };
}
```

**Minimal config (conditional sections disabled):**
```typescript
function minimalContext(): Record<string, unknown> {
  return {
    ...buildDefaultContext(aMinimalProjectConfig()),
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

### 4.4 Test Scenarios

#### Section Rendering Tests (individual sections)

| # | Test Name | Template | Assertion |
|---|-----------|----------|-----------|
| 1 | `header_fullContext_rendersProjectNameAndPurpose` | `sections/header.md.njk` | Contains `# my-service` and purpose text |
| 2 | `architecture_fullContext_rendersStyleAndLanguage` | `sections/architecture.md.njk` | Contains architecture_style, language_name |
| 3 | `techStack_fullContext_rendersAllRows` | `sections/tech-stack.md.njk` | Contains database, cache, container rows |
| 4 | `techStack_databaseNone_omitsDatabaseRow` | `sections/tech-stack.md.njk` | Does NOT contain "Database" row |
| 5 | `techStack_cacheNone_omitsCacheRow` | `sections/tech-stack.md.njk` | Does NOT contain "Cache" row |
| 6 | `techStack_orchestratorNone_omitsOrchestratorRow` | `sections/tech-stack.md.njk` | Does NOT contain "Orchestrator" row |
| 7 | `techStack_observabilityNone_omitsObservabilityRow` | `sections/tech-stack.md.njk` | Does NOT contain "Observability" row |
| 8 | `commands_fullContext_rendersAllCommands` | `sections/commands.md.njk` | Contains buildCmd, testCmd, compileCmd, coverageCmd |
| 9 | `codingStandards_fullContext_rendersHardLimits` | `sections/coding-standards.md.njk` | Contains "25 lines", "250 lines", SOLID references |
| 10 | `qualityGates_fullContext_rendersCoverageThresholds` | `sections/quality-gates.md.njk` | Contains coverage_line, coverage_branch values |
| 11 | `domain_fullContext_rendersDomainSection` | `sections/domain.md.njk` | Contains "## Domain" header |
| 12 | `security_fullContext_rendersFrameworks` | `sections/security.md.njk` | Contains "owasp", "pci-dss" |
| 13 | `conventions_fullContext_rendersCommitConventions` | `sections/conventions.md.njk` | Contains "Conventional Commits" |
| 14 | `skills_fullContext_rendersSkillsTable` | `sections/skills.md.njk` | Contains "x-dev-implement", "x-review"; does NOT contain "x-lib-audit" |
| 15 | `agents_fullContext_rendersAgentsTable` | `sections/agents.md.njk` | Contains "architect", "tech-lead", "qa-engineer" |

#### Orchestrator Template Tests (agents-md.md.njk)

| # | Test Name | Context | Assertion |
|---|-----------|---------|-----------|
| 16 | `agentsMd_fullContext_containsAllSections` | full | Contains Header, Architecture, Tech Stack, Commands, Coding Standards, Quality Gates, Domain, Security, Conventions, Skills, Agents |
| 17 | `agentsMd_fullContext_noTemplateArtifacts` | full | Does NOT match `/{[{%]/` regex (no unrendered Nunjucks tags) |
| 18 | `agentsMd_fullContext_validMarkdown` | full | No `{{`, no `{%`, no `{#` in output |
| 19 | `agentsMd_minimalContext_omitsDomainSection` | minimal (domain_driven="False") | Does NOT contain "## Domain" |
| 20 | `agentsMd_minimalContext_omitsSecuritySection` | minimal (security_frameworks=[]) | Does NOT contain "## Security" |
| 21 | `agentsMd_minimalContext_omitsSkillsSection` | minimal (skills_list=[]) | Does NOT contain "## Available Skills" |
| 22 | `agentsMd_minimalContext_omitsAgentsSection` | minimal (agents_list=[]) | Does NOT contain "## Agent Personas" |
| 23 | `agentsMd_minimalContext_includesAlwaysPresentSections` | minimal | Contains Header, Architecture, Tech Stack, Commands, Coding Standards, Quality Gates, Conventions |

#### Config TOML Tests (config.toml.njk)

| # | Test Name | Context | Assertion |
|---|-----------|---------|-----------|
| 24 | `configToml_fullContext_rendersModelAndPolicy` | full | Contains `model = "o4-mini"`, `approval_policy = "on-request"` |
| 25 | `configToml_fullContext_rendersMcpServers` | full | Contains `[mcp_servers.firecrawl]` |
| 26 | `configToml_noMcpServers_omitsMcpSection` | minimal | Does NOT contain `[mcp_servers` |
| 27 | `configToml_noHooks_usesUntrustedPolicy` | minimal | Contains `approval_policy = "untrusted"` |
| 28 | `configToml_withHooks_usesOnRequestPolicy` | full | Contains `approval_policy = "on-request"` |
| 29 | `configToml_fullContext_noTemplateArtifacts` | full | No `{{`, `{%`, `{#` |
| 30 | `configToml_fullContext_validTomlStructure` | full | Contains valid TOML key-value pairs, proper quoting |
| 31 | `configToml_rendersSandboxMode` | full | Contains `mode = "workspace-write"` |

#### Snapshot Tests

| # | Test Name | Context | Assertion |
|---|-----------|---------|-----------|
| 32 | `agentsMd_fullContext_matchesSnapshot` | full | `toMatchSnapshot()` |
| 33 | `agentsMd_minimalContext_matchesSnapshot` | minimal | `toMatchSnapshot()` |
| 34 | `configToml_fullContext_matchesSnapshot` | full | `toMatchSnapshot()` |
| 35 | `configToml_minimalContext_matchesSnapshot` | minimal | `toMatchSnapshot()` |

### 4.5 Coverage Targets

| Metric | Target | Strategy |
|--------|--------|----------|
| Line | >= 95% | Every template line exercised via full + minimal contexts |
| Branch | >= 90% | Every `{% if %}` guard exercised in both true and false states |

---

## 5. Implementation Groups (Execution Order)

### G1: Create directory structure and section templates (10 files)

1. Create `resources/codex-templates/` directory
2. Create `resources/codex-templates/sections/` directory
3. Implement `sections/header.md.njk`
4. Implement `sections/architecture.md.njk`
5. Implement `sections/tech-stack.md.njk` (with conditional row guards)
6. Implement `sections/commands.md.njk`
7. Implement `sections/coding-standards.md.njk`
8. Implement `sections/quality-gates.md.njk`
9. Implement `sections/domain.md.njk`
10. Implement `sections/security.md.njk`
11. Implement `sections/conventions.md.njk`
12. Implement `sections/skills.md.njk`
13. Implement `sections/agents.md.njk`

**Validation:** Each section template can be rendered individually with `engine.renderTemplate("codex-templates/sections/X.md.njk", context)`.

### G2: Create orchestrator and config templates (2 files)

1. Implement `agents-md.md.njk` with conditional includes
2. Implement `config.toml.njk` with conditional MCP section

**Validation:** Full rendering produces valid Markdown (no template artifacts) and valid TOML (proper quoting and structure).

### G3: Create test file and run tests

1. Create `tests/node/codex-templates.test.ts`
2. Create test fixtures (full and minimal contexts)
3. Implement all 35 test scenarios
4. Run tests: `npx vitest run tests/node/codex-templates.test.ts`
5. Generate and update snapshots: `npx vitest run -u tests/node/codex-templates.test.ts`
6. Verify coverage meets thresholds

### G4: Final validation

1. Compile check: `npx tsc --noEmit`
2. Full test suite: `npx vitest run`
3. Coverage: `npx vitest run --coverage`
4. Verify zero compiler warnings

---

## 6. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Include paths resolve incorrectly because FileSystemLoader root is `resourcesDir`, not `codex-templates/` | Medium | High | Use full relative paths in includes: `codex-templates/sections/X.md.njk`. Verify in first test. |
| `throwOnUndefined: true` causes rendering failures for optional context variables | Medium | High | Guard every optional variable with `{% if %}` in the orchestrator before including sections. Pass all variables in test contexts (even if empty). |
| Whitespace/blank-line accumulation from conditional blocks produces malformed Markdown | Medium | Medium | Use `{%- -%}` whitespace control tags. Snapshot tests will catch regressions. |
| `observability` field not in existing `buildDefaultContext()` (24 fields, not 25) | Low | Medium | Templates accept `observability` as an extended context field provided by the assembler. Document this gap clearly for STORY-022. |
| TOML output validity -- template-generated TOML may have syntax errors | Medium | Medium | Snapshot tests validate structure. Optionally use a TOML parser in tests (`@iarna/toml` or similar) to validate parsed output. |
| MCP server `env` iteration requires Nunjucks object iteration syntax which differs from Jinja2 | Low | Medium | Use `{% for key, value in server.env %}` syntax (Nunjucks supports this for objects via `items()` or direct iteration). Test with at least one env variable. |
| Section templates not independently testable if they reference parent context variables | Low | Low | Each section receives the same merged context. Section tests pass the full context, not a subset. |
| Test fixture configs (`aFullProjectConfig`, `aMinimalProjectConfig`) do not exist yet | Certain | Low | Create them in the test file or add to `tests/fixtures/project-config.fixture.ts`. Use existing `aProjectConfig()` as base, extending with `SecurityConfig` and full `DataConfig`. |

---

## 7. Dependency Direction Validation

```
tests/node/codex-templates.test.ts
  |-- imports --> src/template-engine.ts (TemplateEngine, buildDefaultContext)
  |-- imports --> src/models.ts (ProjectConfig, etc.)
  |-- reads   --> resources/codex-templates/*.njk (via TemplateEngine)
```

No circular dependencies. Templates are pure resource files. The test file follows existing test patterns. No source code modifications.

---

## 8. Acceptance Criteria Checklist

From story Gherkin scenarios:

- [ ] 13 templates created in `resources/codex-templates/`
- [ ] Full context rendering produces Markdown with all sections (Header, Architecture, Tech Stack, Commands, Coding Standards, Quality Gates, Conventions, Skills, Agents)
- [ ] No template artifacts (`{{`, `{%`, `{#}`) in rendered output
- [ ] Domain section omitted when `domain_driven == "False"`
- [ ] Security section omitted when `security_frameworks` is empty
- [ ] Tech Stack omits database row when `database_name == "none"`
- [ ] Tech Stack omits cache row when `cache_name == "none"`
- [ ] config.toml renders valid TOML with model, approval_policy, sandbox_mode
- [ ] config.toml includes MCP servers section when `mcp_servers` is non-empty
- [ ] config.toml omits MCP section when `mcp_servers` is empty
- [ ] `approval_policy == "on-request"` when `has_hooks == true`
- [ ] `approval_policy == "untrusted"` when `has_hooks == false`
- [ ] Coverage >= 95% line, >= 90% branch
- [ ] All tests passing, zero compiler warnings
