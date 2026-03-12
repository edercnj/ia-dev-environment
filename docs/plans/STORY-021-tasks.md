# Task Decomposition -- STORY-021: Codex Nunjucks Templates

**Status:** PENDING
**Date:** 2026-03-11
**Blocked By:** EPIC-001/STORY-005 (Template Engine) -- complete
**Blocks:** STORY-022 (CodexAgentsMdAssembler), STORY-023 (CodexConfigAssembler)

---

## G1 -- Foundation (directory structure + simple section templates)

**Purpose:** Create the `resources/codex-templates/` directory structure and the three simplest section templates that use only flat string interpolation with no conditional logic or iteration.
**Dependencies:** None
**Compiles independently:** N/A -- pure resource files, no TypeScript changes.

### T1.1 -- Create directory structure

- **Directories:** `resources/codex-templates/` and `resources/codex-templates/sections/`
- **What to implement:** Create both directories (empty initially).
- **Dependencies on other tasks:** None
- **Estimated complexity:** XS

### T1.2 -- Create `sections/header.md.njk`

- **File:** `resources/codex-templates/sections/header.md.njk` (create)
- **Context consumed:** `project_name`, `project_purpose`
- **What to implement:**
  - Render `# {{ project_name }}` heading
  - Render `{{ project_purpose }}` paragraph below
  - No conditional logic -- both fields are always present
- **Dependencies on other tasks:** T1.1
- **Estimated complexity:** XS

### T1.3 -- Create `sections/architecture.md.njk`

- **File:** `resources/codex-templates/sections/architecture.md.njk` (create)
- **Context consumed:** `architecture_style`, `language_name`, `language_version`, `framework_name`, `framework_version`
- **What to implement:**
  - `## Architecture` heading
  - Architecture style, package structure, dependency direction rules
  - All fields are always-present strings (no conditionals needed)
- **Dependencies on other tasks:** T1.1
- **Estimated complexity:** S

### T1.4 -- Create `sections/conventions.md.njk`

- **File:** `resources/codex-templates/sections/conventions.md.njk` (create)
- **Context consumed:** `language_name`
- **What to implement:**
  - `## Conventions` heading
  - Commit format (Conventional Commits), branch naming, code language (English), documentation language
  - No conditional logic -- `language_name` is always present
- **Dependencies on other tasks:** T1.1
- **Estimated complexity:** S

### Verification checkpoint G1

```bash
ls resources/codex-templates/sections/   # verify header.md.njk, architecture.md.njk, conventions.md.njk
```

---

## G2 -- Data-Driven Sections (conditional rows and object access)

**Purpose:** Create section templates that involve conditional row rendering (tech-stack omits rows when value is `"none"`) and object property access (commands from `resolved_stack`).
**Dependencies:** G1 (directory structure must exist)
**Compiles independently:** N/A -- pure resource files.

### T2.1 -- Create `sections/tech-stack.md.njk`

- **File:** `resources/codex-templates/sections/tech-stack.md.njk` (create)
- **Context consumed:** `language_name`, `language_version`, `framework_name`, `framework_version`, `build_tool`, `database_name`, `cache_name`, `container`, `orchestrator`, `observability`
- **What to implement:**
  - `## Tech Stack` heading
  - Markdown table with Language, Framework, Build Tool, Container as always-present rows
  - Conditional rows for Database, Cache, Orchestrator, Observability: use `{% if field != "none" %}` guards per row to omit when value is `"none"`
  - `observability` is an extended context field provided by the assembler (not in `buildDefaultContext()`)
- **Dependencies on other tasks:** T1.1
- **Estimated complexity:** M

### T2.2 -- Create `sections/commands.md.njk`

- **File:** `resources/codex-templates/sections/commands.md.njk` (create)
- **Context consumed:** `resolved_stack` (object with `buildCmd`, `testCmd`, `compileCmd`, `coverageCmd`)
- **What to implement:**
  - `## Commands` heading
  - Markdown table with Build, Test, Compile, Coverage rows
  - Access nested properties: `{{ resolved_stack.buildCmd }}`, etc.
  - No conditional logic -- `resolved_stack` is always present with all 4 commands
- **Dependencies on other tasks:** T1.1
- **Estimated complexity:** S

### Verification checkpoint G2

```bash
ls resources/codex-templates/sections/   # verify tech-stack.md.njk, commands.md.njk added
```

---

## G3 -- Standards Sections (static content with minimal interpolation)

**Purpose:** Create coding-standards and quality-gates section templates. These contain substantial static Markdown content derived from Rule 03 and Rule 05 with a few interpolated values.
**Dependencies:** G1 (directory structure)
**Compiles independently:** N/A -- pure resource files.

### T3.1 -- Create `sections/coding-standards.md.njk`

- **File:** `resources/codex-templates/sections/coding-standards.md.njk` (create)
- **Context consumed:** `language_name`, `language_version`
- **What to implement:**
  - `## Coding Standards` heading
  - Hard limits table: method length <= 25 lines, class <= 250 lines, parameters <= 4, line width <= 120
  - SOLID one-liners (SRP, OCP, LSP, ISP, DIP)
  - Error handling rules (no null returns, no null args, exceptions with context)
  - Forbidden patterns list (boolean flags, mutable global state, god classes, wildcard imports, sleep for sync)
  - Content derived from Rule 03 (`resources/core-rules/03-coding-standards.md`)
- **Dependencies on other tasks:** T1.1
- **Estimated complexity:** S

### T3.2 -- Create `sections/quality-gates.md.njk`

- **File:** `resources/codex-templates/sections/quality-gates.md.njk` (create)
- **Context consumed:** `coverage_line`, `coverage_branch`, `smoke_tests`, `contract_tests`, `performance_tests`
- **What to implement:**
  - `## Quality Gates` heading
  - Coverage thresholds table with `{{ coverage_line }}` and `{{ coverage_branch }}` values
  - Test categories list (Unit, Integration, API, Contract, E2E, Performance, Smoke)
  - Conditional rows for smoke/contract/performance tests based on their `"True"`/`"False"` string values
  - Test naming convention: `[method]_[scenario]_[expected]`
  - Merge checklist
  - Content derived from Rule 05 (`resources/core-rules/05-quality-gates.md`)
- **Dependencies on other tasks:** T1.1
- **Estimated complexity:** S

### Verification checkpoint G3

```bash
ls resources/codex-templates/sections/   # verify coding-standards.md.njk, quality-gates.md.njk added
```

---

## G4 -- Conditional Sections (guarded by orchestrator-level {% if %})

**Purpose:** Create the four section templates that are conditionally included by the orchestrator. These sections use `{% for %}` loops or are only rendered when their guard condition is true.
**Dependencies:** G1 (directory structure)
**Compiles independently:** N/A -- pure resource files.

### T4.1 -- Create `sections/domain.md.njk`

- **File:** `resources/codex-templates/sections/domain.md.njk` (create)
- **Context consumed:** None directly (the orchestrator guards with `domain_driven == "True"`)
- **What to implement:**
  - `## Domain` heading
  - Domain model structural scaffolding with guidance comments
  - Placeholders for entities, value objects, aggregates, business rules
  - This section provides a template for project-specific domain content that cannot be auto-generated from config
- **Dependencies on other tasks:** T1.1
- **Estimated complexity:** S

### T4.2 -- Create `sections/security.md.njk`

- **File:** `resources/codex-templates/sections/security.md.njk` (create)
- **Context consumed:** `security_frameworks` (array of strings)
- **What to implement:**
  - `## Security` heading
  - `{% for framework in security_frameworks %}` loop to list each framework
  - Security guidelines based on configured frameworks
  - Only included when `security_frameworks` is non-empty (guarded by orchestrator)
- **Dependencies on other tasks:** T1.1
- **Estimated complexity:** S

### T4.3 -- Create `sections/skills.md.njk`

- **File:** `resources/codex-templates/sections/skills.md.njk` (create)
- **Context consumed:** `skills_list` (array of `{ name, description, user_invocable }`)
- **What to implement:**
  - `## Available Skills` heading
  - Markdown table with Skill and Description columns
  - `{% for skill in skills_list %}{% if skill.user_invocable %}` to filter to user-invocable skills only
  - Only included when `skills_list` is non-empty (guarded by orchestrator)
- **Dependencies on other tasks:** T1.1
- **Estimated complexity:** S

### T4.4 -- Create `sections/agents.md.njk`

- **File:** `resources/codex-templates/sections/agents.md.njk` (create)
- **Context consumed:** `agents_list` (array of `{ name, description }`)
- **What to implement:**
  - `## Agent Personas` heading
  - Markdown table with Agent and Role columns
  - `{% for agent in agents_list %}` loop to render each row
  - Only included when `agents_list` is non-empty (guarded by orchestrator)
- **Dependencies on other tasks:** T1.1
- **Estimated complexity:** S

### Verification checkpoint G4

```bash
ls resources/codex-templates/sections/   # verify all 11 section files exist
```

---

## G5 -- Orchestrator + Config Templates

**Purpose:** Create the two top-level templates: `agents-md.md.njk` (orchestrator that includes all sections with conditional guards) and `config.toml.njk` (standalone TOML template).
**Dependencies:** G1-G4 (all section templates must exist for includes to resolve)
**Compiles independently:** N/A -- pure resource files.

### T5.1 -- Create `agents-md.md.njk` (orchestrator)

- **File:** `resources/codex-templates/agents-md.md.njk` (create)
- **What to implement:**
  - Nunjucks comment: `{# Codex AGENTS.md -- consolidated project instructions #}`
  - Always-included sections (6): `{% include "codex-templates/sections/header.md.njk" %}`, architecture, tech-stack, commands, coding-standards, quality-gates
  - Conditionally-included sections (4):
    - `{% if domain_driven == "True" %}{% include "codex-templates/sections/domain.md.njk" %}{% endif %}`
    - `{% if security_frameworks and security_frameworks.length > 0 %}{% include "codex-templates/sections/security.md.njk" %}{% endif %}`
    - `{% if skills_list and skills_list.length > 0 %}{% include "codex-templates/sections/skills.md.njk" %}{% endif %}`
    - `{% if agents_list and agents_list.length > 0 %}{% include "codex-templates/sections/agents.md.njk" %}{% endif %}`
  - Always-included section: conventions
  - Include paths MUST use `codex-templates/sections/` prefix (FileSystemLoader root is `resourcesDir`)
  - Whitespace control: use `{%- -%}` trim markers on conditional closing tags where needed to avoid excess blank lines
  - Blank line separators between each `{% include %}` block
- **Dependencies on other tasks:** G1-G4 (all 11 section files)
- **Estimated complexity:** M

### T5.2 -- Create `config.toml.njk`

- **File:** `resources/codex-templates/config.toml.njk` (create)
- **Context consumed:** `model`, `approval_policy`, `sandbox_mode`, `project_name`, `mcp_servers` (optional array of `{ id, command, env }`)
- **What to implement:**
  - TOML comment: `# Codex CLI configuration for {{ project_name }}`
  - Generated-file warning comment
  - Top-level keys: `model = "{{ model }}"`, `approval_policy = "{{ approval_policy }}"`
  - `[sandbox]` section: `mode = "{{ sandbox_mode }}"`
  - Conditional MCP servers section: `{% if mcp_servers and mcp_servers.length > 0 %}`
    - `{% for server in mcp_servers %}` loop producing `[mcp_servers.{{ server.id }}]` sections
    - Command array rendering: `command = [{% for part in server.command %}"{{ part }}"{% if not loop.last %}, {% endif %}{% endfor %}]`
    - Environment variables: `{% for key, value in server.env %}` producing key-value pairs under `[mcp_servers.{{ server.id }}.env]`
  - Proper TOML quoting (double quotes around string values)
- **Dependencies on other tasks:** T1.1 (directory only)
- **Estimated complexity:** M

### Verification checkpoint G5

```bash
ls resources/codex-templates/   # verify agents-md.md.njk, config.toml.njk at root level
ls resources/codex-templates/sections/   # verify all 11 section files
# Total: 13 template files
```

---

## G6 -- Tests

**Purpose:** Create the test file `tests/node/codex-templates.test.ts` with all 35 test scenarios covering individual section rendering, orchestrator composition, config.toml generation, conditional logic, and snapshots.
**Dependencies:** G5 (all 13 templates must exist for rendering tests)
**Compiles independently:** Yes -- imports from existing `src/template-engine.ts` and `src/models.ts`.

### T6.1 -- Create test file with fixtures

- **File:** `tests/node/codex-templates.test.ts` (create)
- **What to implement:**
  - Import `TemplateEngine`, `buildDefaultContext` from `src/template-engine.ts`
  - Import model classes from `src/models.ts`
  - Use the real `resources/` directory (read-only, no temp dir needed since `renderTemplate` does not write to disk)
  - Two fixture functions:
    - `fullContext()` -- all sections enabled, all arrays populated, `domain_driven = "True"`, `security_frameworks = ["owasp", "pci-dss"]`
    - `minimalContext()` -- conditional sections disabled, empty arrays, `domain_driven = "False"`, `security_frameworks = []`
  - Construct `TemplateEngine` with the project's `resources/` directory and a suitable `ProjectConfig`
- **Dependencies on other tasks:** G5
- **Estimated complexity:** S

### T6.2 -- Section rendering tests (15 tests)

- **File:** `tests/node/codex-templates.test.ts` (extend)
- **What to implement:**
  - `describe("Section rendering")` block containing:
  1. `header_fullContext_rendersProjectNameAndPurpose` -- render `codex-templates/sections/header.md.njk`, assert contains `# my-service` and purpose text
  2. `architecture_fullContext_rendersStyleAndLanguage` -- assert contains `architecture_style`, `language_name`
  3. `techStack_fullContext_rendersAllRows` -- assert contains database, cache, container rows
  4. `techStack_databaseNone_omitsDatabaseRow` -- override `database_name = "none"`, assert does NOT contain "Database" row
  5. `techStack_cacheNone_omitsCacheRow` -- override `cache_name = "none"`, assert does NOT contain "Cache" row
  6. `techStack_orchestratorNone_omitsOrchestratorRow` -- override `orchestrator = "none"`, assert does NOT contain "Orchestrator" row
  7. `techStack_observabilityNone_omitsObservabilityRow` -- override `observability = "none"`, assert does NOT contain "Observability" row
  8. `commands_fullContext_rendersAllCommands` -- assert contains buildCmd, testCmd, compileCmd, coverageCmd values
  9. `codingStandards_fullContext_rendersHardLimits` -- assert contains "25 lines", "250 lines", SOLID references
  10. `qualityGates_fullContext_rendersCoverageThresholds` -- assert contains `coverage_line` and `coverage_branch` values
  11. `domain_fullContext_rendersDomainSection` -- assert contains "## Domain" heading
  12. `security_fullContext_rendersFrameworks` -- assert contains "owasp", "pci-dss"
  13. `conventions_fullContext_rendersCommitConventions` -- assert contains "Conventional Commits"
  14. `skills_fullContext_rendersSkillsTable` -- assert contains "x-dev-implement", "x-review"; does NOT contain "x-lib-audit"
  15. `agents_fullContext_rendersAgentsTable` -- assert contains "architect", "tech-lead", "qa-engineer"
- **Dependencies on other tasks:** T6.1
- **Estimated complexity:** M

### T6.3 -- Orchestrator template tests (8 tests)

- **File:** `tests/node/codex-templates.test.ts` (extend)
- **What to implement:**
  - `describe("Orchestrator -- agents-md.md.njk")` block containing:
  16. `agentsMd_fullContext_containsAllSections` -- render `codex-templates/agents-md.md.njk` with full context, assert contains all 11 section headings
  17. `agentsMd_fullContext_noTemplateArtifacts` -- assert output does NOT match `/{[{%]/` regex
  18. `agentsMd_fullContext_validMarkdown` -- assert no `{{`, no `{%`, no `{#` in output
  19. `agentsMd_minimalContext_omitsDomainSection` -- minimal context (`domain_driven="False"`), assert does NOT contain "## Domain"
  20. `agentsMd_minimalContext_omitsSecuritySection` -- minimal context (`security_frameworks=[]`), assert does NOT contain "## Security"
  21. `agentsMd_minimalContext_omitsSkillsSection` -- minimal context (`skills_list=[]`), assert does NOT contain "## Available Skills"
  22. `agentsMd_minimalContext_omitsAgentsSection` -- minimal context (`agents_list=[]`), assert does NOT contain "## Agent Personas"
  23. `agentsMd_minimalContext_includesAlwaysPresentSections` -- minimal context, assert contains Header, Architecture, Tech Stack, Commands, Coding Standards, Quality Gates, Conventions
- **Dependencies on other tasks:** T6.1
- **Estimated complexity:** M

### T6.4 -- Config TOML tests (8 tests)

- **File:** `tests/node/codex-templates.test.ts` (extend)
- **What to implement:**
  - `describe("Config -- config.toml.njk")` block containing:
  24. `configToml_fullContext_rendersModelAndPolicy` -- assert contains `model = "o4-mini"`, `approval_policy = "on-request"`
  25. `configToml_fullContext_rendersMcpServers` -- assert contains `[mcp_servers.firecrawl]`
  26. `configToml_noMcpServers_omitsMcpSection` -- minimal context, assert does NOT contain `[mcp_servers`
  27. `configToml_noHooks_usesUntrustedPolicy` -- minimal context, assert contains `approval_policy = "untrusted"`
  28. `configToml_withHooks_usesOnRequestPolicy` -- full context, assert contains `approval_policy = "on-request"`
  29. `configToml_fullContext_noTemplateArtifacts` -- assert no `{{`, `{%`, `{#`
  30. `configToml_fullContext_validTomlStructure` -- assert contains valid TOML key-value pairs, proper quoting
  31. `configToml_rendersSandboxMode` -- assert contains `mode = "workspace-write"`
- **Dependencies on other tasks:** T6.1
- **Estimated complexity:** M

### T6.5 -- Snapshot tests (4 tests)

- **File:** `tests/node/codex-templates.test.ts` (extend)
- **What to implement:**
  - `describe("Snapshots")` block containing:
  32. `agentsMd_fullContext_matchesSnapshot` -- `toMatchSnapshot()`
  33. `agentsMd_minimalContext_matchesSnapshot` -- `toMatchSnapshot()`
  34. `configToml_fullContext_matchesSnapshot` -- `toMatchSnapshot()`
  35. `configToml_minimalContext_matchesSnapshot` -- `toMatchSnapshot()`
  - Run `npx vitest run -u tests/node/codex-templates.test.ts` to generate initial snapshots
- **Dependencies on other tasks:** T6.2, T6.3, T6.4 (all tests must render successfully first)
- **Estimated complexity:** S

### Test execution checkpoint G6

```bash
npx vitest run tests/node/codex-templates.test.ts          # all 35 tests pass
npx vitest run -u tests/node/codex-templates.test.ts       # generate/update snapshots
```

---

## G7 -- Validation (compilation + coverage verification)

**Purpose:** Final verification that the entire project compiles cleanly, all tests pass (including existing ones), and coverage thresholds are met.
**Dependencies:** G6 (tests must exist and pass)

### T7.1 -- Full compilation check

- **Command:** `npx tsc --noEmit`
- **Expected:** Zero errors across the entire project.
- **Dependencies on other tasks:** G6

### T7.2 -- Run codex template tests with coverage

- **Command:** `npx vitest run --coverage tests/node/codex-templates.test.ts`
- **Expected:** >= 95% line coverage, >= 90% branch coverage.
- **Coverage strategy:**
  - Every template line exercised via full + minimal contexts
  - Every `{% if %}` guard exercised in both true (full context) and false (minimal context) states
  - Every `{% for %}` loop exercised with non-empty (full) and empty (minimal) arrays
  - All conditional tech-stack rows exercised individually (database, cache, orchestrator, observability set to `"none"`)
  - Skills filtering: `user_invocable: true` and `user_invocable: false` both in fixture
- **Dependencies on other tasks:** T6.5

### T7.3 -- Full test suite regression check

- **Command:** `npx vitest run`
- **Expected:** All 1,384+ tests pass (existing + 35 new). Zero regressions.
- **Dependencies on other tasks:** T7.2

### T7.4 -- Acceptance criteria verification

- **What to verify:**
  1. 13 templates exist in `resources/codex-templates/` (1 orchestrator + 11 sections + 1 config.toml)
  2. Full context rendering produces Markdown with all sections (Header, Architecture, Tech Stack, Commands, Coding Standards, Quality Gates, Domain, Security, Conventions, Skills, Agents)
  3. No template artifacts (`{{`, `{%`, `{#}`) in rendered output
  4. Domain section omitted when `domain_driven == "False"`
  5. Security section omitted when `security_frameworks` is empty
  6. Tech Stack omits rows when values are `"none"`
  7. config.toml renders valid TOML with model, approval_policy, sandbox_mode
  8. config.toml includes/omits MCP servers section based on `mcp_servers` array
  9. `approval_policy` derived correctly from `has_hooks`
  10. Coverage >= 95% line, >= 90% branch
  11. All tests passing, zero compiler warnings
- **Dependencies on other tasks:** T7.1, T7.2, T7.3

---

## Summary Table

| Group | Purpose | Files to Create | Files to Modify | Tasks | Test Cases | Complexity |
|-------|---------|----------------|----------------|-------|------------|------------|
| G1 | Foundation: dirs + simple sections | 3 templates | 0 | 4 | 0 | XS-S |
| G2 | Data-driven sections (tech-stack, commands) | 2 templates | 0 | 2 | 0 | S-M |
| G3 | Standards sections (coding, quality) | 2 templates | 0 | 2 | 0 | S |
| G4 | Conditional sections (domain, security, skills, agents) | 4 templates | 0 | 4 | 0 | S |
| G5 | Orchestrator + config.toml | 2 templates | 0 | 2 | 0 | M |
| G6 | Tests | 1 test file | 0 | 5 | 35 | M |
| G7 | Validation | 0 | 0 | 4 | 0 (verification) | S |
| **Total** | | **13 templates + 1 test** | **0** | **23 tasks** | **35 test cases** | |

## Dependency Graph

```
G1: FOUNDATION (dirs + header, architecture, conventions)
 |
 +---> G2: DATA-DRIVEN (tech-stack, commands)
 |
 +---> G3: STANDARDS (coding-standards, quality-gates)
 |
 +---> G4: CONDITIONAL (domain, security, skills, agents)
 |
 +---> G5: ORCHESTRATOR + CONFIG (agents-md.md.njk, config.toml.njk)
             |
             v
         G6: TESTS (35 test scenarios)
             |
             v
         G7: VALIDATION (compile, coverage, regression)
```

- G1 must be done first (creates directory structure).
- G2, G3, G4 can be done in parallel (independent section templates, all depend only on G1 for directories).
- G5 depends on G1-G4 (orchestrator `{% include %}` directives reference all section templates).
- G6 depends on G5 (tests render templates that must exist).
- G7 depends on G6 (verification requires tests).

## File Inventory

### Template files (13 new)

| File | Type | Content |
|------|------|---------|
| `resources/codex-templates/agents-md.md.njk` | Orchestrator | Includes all sections with conditional guards |
| `resources/codex-templates/config.toml.njk` | Standalone | TOML config with model, sandbox, MCP servers |
| `resources/codex-templates/sections/header.md.njk` | Section | Project name + purpose |
| `resources/codex-templates/sections/architecture.md.njk` | Section | Architecture style, structure, dependencies |
| `resources/codex-templates/sections/tech-stack.md.njk` | Section | Technology table with conditional rows |
| `resources/codex-templates/sections/commands.md.njk` | Section | Build/test/compile/coverage commands |
| `resources/codex-templates/sections/coding-standards.md.njk` | Section | Hard limits, SOLID, error handling, forbidden |
| `resources/codex-templates/sections/quality-gates.md.njk` | Section | Coverage thresholds, test categories, checklist |
| `resources/codex-templates/sections/domain.md.njk` | Section (conditional) | Domain model scaffolding |
| `resources/codex-templates/sections/security.md.njk` | Section (conditional) | Security frameworks list |
| `resources/codex-templates/sections/conventions.md.njk` | Section | Commit, branch, language conventions |
| `resources/codex-templates/sections/skills.md.njk` | Section (conditional) | Skills table (user-invocable only) |
| `resources/codex-templates/sections/agents.md.njk` | Section (conditional) | Agent personas table |

### Test files (1 new)

| File | Content |
|------|---------|
| `tests/node/codex-templates.test.ts` | 35 test scenarios across 5 describe blocks |

### Source files modified

None. This story is pure template creation + tests.

## Key Implementation Notes

1. **Include path prefix:** All `{% include %}` paths in the orchestrator must use `codex-templates/sections/` prefix because the Nunjucks FileSystemLoader root is `resourcesDir` (the `resources/` directory), NOT `codex-templates/`.
2. **`throwOnUndefined: true`:** Every variable referenced in any template MUST exist in the rendering context. The orchestrator guards optional sections with `{% if %}` before including their templates. Test fixtures must provide ALL variables (even if empty arrays or `"none"` strings).
3. **Whitespace control:** `trimBlocks: false` and `lstripBlocks: false` in the TemplateEngine config. Use `{%- -%}` trim markers on conditional tags where blank lines would accumulate. Snapshot tests catch whitespace regressions.
4. **`observability` not in `buildDefaultContext()`:** The existing function returns 24 fields. `observability` is provided as an extended context field by the assembler (STORY-022). Templates accept it as a string.
5. **Python-style booleans:** `domain_driven`, `smoke_tests`, `contract_tests`, `performance_tests` are `"True"`/`"False"` strings (not JavaScript booleans). Conditionals must compare against `"True"` as a string.
6. **Test approach:** Use the real `resources/` directory (read-only) since templates are static files and `renderTemplate()` does not write to disk. No temp directory management needed for rendering tests.
7. **Fixture configs:** Build `ProjectConfig` instances in the test file using existing model constructors (see `tests/fixtures/project-config.fixture.ts` for patterns). Extend with `SecurityConfig(["owasp", "pci-dss"])` for full context, `SecurityConfig()` for minimal.
8. **MCP `env` iteration:** Nunjucks supports `{% for key, value in obj %}` for object iteration. Test with at least one env variable to validate this syntax.
