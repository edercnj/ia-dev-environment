# Test Plan -- STORY-023: CodexConfigAssembler

## Summary

- New test file: `tests/node/assembler/codex-config-assembler.test.ts`
- Total test methods: 30
- Categories: Unit (deriveApprovalPolicy, buildConfigContext, mapMcpServers, assemble), Integration (Pipeline), Edge cases
- Coverage targets: >= 95% line, >= 90% branch
- Module under test: `src/assembler/codex-config-assembler.ts`
- Performance budget: < 3s (file I/O against temp directories + Nunjucks rendering)

---

## 1. Test File Location and Naming

**Path:** `tests/node/assembler/codex-config-assembler.test.ts`

**Rationale:** Follows the established convention for assembler tests (`tests/node/assembler/`). Mirrors the naming of `codex-agents-md-assembler.test.ts` from STORY-022.

**Naming convention:** `[methodUnderTest]_[scenario]_[expectedBehavior]` per Rule 05.

---

## 2. Test Infrastructure

### 2.1 Temp Directory Setup

Each test group uses a temporary directory created via `mkdtemp` in a `beforeEach` hook and removed in `afterEach`. The temp directory simulates the pipeline output directory where previous assemblers have already written `.claude/` and `.github/` artifacts.

```typescript
import { mkdtemp, rm, mkdir, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join, resolve, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, it, expect, beforeEach, afterEach } from "vitest";
import * as fs from "node:fs";
import { TemplateEngine } from "../../../src/template-engine.js";
import {
  aFullProjectConfig,
  aMinimalProjectConfig,
} from "../../fixtures/project-config.fixture.js";
import {
  McpConfig,
  McpServerConfig,
} from "../../../src/models.js";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const RESOURCES_DIR = resolve(__dirname, "../../../resources");

let tempDir: string;

beforeEach(async () => {
  tempDir = await mkdtemp(join(tmpdir(), "codex-config-test-"));
});

afterEach(async () => {
  await rm(tempDir, { recursive: true, force: true });
});
```

### 2.2 Helper Functions

```typescript
/**
 * Create a .claude/hooks/ directory with a dummy hook file.
 * This simulates a project that has hooks, causing approval_policy = "on-request".
 */
async function seedHooksDir(rootDir: string): Promise<void> {
  const hooksDir = join(rootDir, ".claude", "hooks");
  await mkdir(hooksDir, { recursive: true });
  await writeFile(
    join(hooksDir, "post-compile-check.sh"), "#!/bin/bash\n",
  );
}

/**
 * Create .claude/ and .github/ directories to simulate pre-existing pipeline output.
 * Used by RULE-105 (impact zero) tests.
 */
async function seedExistingOutput(rootDir: string): Promise<void> {
  const claudeDir = join(rootDir, ".claude");
  const githubDir = join(rootDir, ".github");
  await mkdir(claudeDir, { recursive: true });
  await mkdir(githubDir, { recursive: true });
  await writeFile(join(claudeDir, "settings.json"), '{"permissions":{}}');
  await writeFile(join(githubDir, "copilot-instructions.md"), "# Instructions");
}

/**
 * Build a ProjectConfig with MCP servers injected.
 * Returns a new object that delegates to aFullProjectConfig() with mcp overridden.
 */
function configWithMcp(
  servers: McpServerConfig[],
): ProjectConfig {
  const base = aFullProjectConfig();
  const withMcp = Object.create(
    Object.getPrototypeOf(base),
    Object.getOwnPropertyDescriptors(base),
  );
  Object.defineProperty(withMcp, "mcp", {
    value: new McpConfig(servers),
  });
  return withMcp;
}
```

---

## 3. Test Fixtures

### 3.1 MCP Server Fixtures

```typescript
const MCP_FIRECRAWL = new McpServerConfig(
  "firecrawl",
  "npx -y @anthropic-ai/firecrawl-mcp",
  [],
  { API_KEY: "test-key-123" },
);

const MCP_DOCS = new McpServerConfig(
  "docs",
  "docs-server --port 3000",
  [],
  { API_KEY: "docs-key", DOCS_PATH: "/var/docs" },
);

const MCP_SIMPLE = new McpServerConfig(
  "simple",
  "simple-mcp",
  [],
  {},
);
```

### 3.2 Expected TOML Fragments

```typescript
// Hardcoded values that appear in every generated config.toml
const EXPECTED_MODEL = 'model = "o4-mini"';
const EXPECTED_SANDBOX = 'mode = "workspace-write"';
const EXPECTED_POLICY_UNTRUSTED = 'approval_policy = "untrusted"';
const EXPECTED_POLICY_ON_REQUEST = 'approval_policy = "on-request"';
```

---

## 4. Test Groups

### Group 1: deriveApprovalPolicy() (3 tests)

Unit tests for the approval policy derivation logic based on hooks detection.

| # | Test Name | Setup | Assertion |
|---|-----------|-------|-----------|
| 1 | `deriveApprovalPolicy_hooksExist_returnsOnRequest` | `hasHooks = true` | Returns `"on-request"` |
| 2 | `deriveApprovalPolicy_noHooks_returnsUntrusted` | `hasHooks = false` | Returns `"untrusted"` |
| 3 | `deriveApprovalPolicy_emptyHooksDir_returnsUntrusted` | Create `.claude/hooks/` directory with no files inside | Returns `"untrusted"` |

#### Assertions Pattern

```typescript
// Test 1
const policy = deriveApprovalPolicy(true);
expect(policy).toBe("on-request");

// Test 2
const policy = deriveApprovalPolicy(false);
expect(policy).toBe("untrusted");

// Test 3 — empty hooks directory (exists but no files)
// The hooks detection should check for files inside, not just directory existence
const policy = deriveApprovalPolicy(false);
expect(policy).toBe("untrusted");
```

---

### Group 2: buildConfigContext() (6 tests)

Unit tests for context construction that produces the template rendering context.

| # | Test Name | Setup | Assertion |
|---|-----------|-------|-----------|
| 4 | `buildConfigContext_simpleConfig_returnsModelAndSandboxDefaults` | `aMinimalProjectConfig()`, no MCP, no hooks | `model === "o4-mini"`, `sandbox_mode === "workspace-write"` |
| 5 | `buildConfigContext_noHooks_setsApprovalPolicyUntrusted` | `aMinimalProjectConfig()`, `hasHooks = false` | `approval_policy === "untrusted"` |
| 6 | `buildConfigContext_withHooks_setsApprovalPolicyOnRequest` | `aFullProjectConfig()`, `hasHooks = true` | `approval_policy === "on-request"` |
| 7 | `buildConfigContext_withMcpServers_setsHasMcpTrue` | Config with 1 MCP server | `has_mcp === true` |
| 8 | `buildConfigContext_noMcpServers_setsHasMcpFalse` | Config with no MCP servers | `has_mcp === false` |
| 9 | `buildConfigContext_fullConfig_includesProjectName` | `aFullProjectConfig()` | `project_name === "my-service"` |

#### Assertions Pattern

```typescript
// Test 4
const ctx = buildConfigContext(config, false);
expect(ctx.model).toBe("o4-mini");
expect(ctx.sandbox_mode).toBe("workspace-write");

// Test 7
const ctx = buildConfigContext(configWithMcp([MCP_FIRECRAWL]), false);
expect(ctx.has_mcp).toBe(true);

// Test 8
const ctx = buildConfigContext(aMinimalProjectConfig(), false);
expect(ctx.has_mcp).toBe(false);

// Test 9
const ctx = buildConfigContext(aFullProjectConfig(), false);
expect(ctx.project_name).toBe("my-service");
```

---

### Group 3: mapMcpServers() (5 tests)

Unit tests for the MCP server mapping function that converts `McpServerConfig[]` to template context.

| # | Test Name | Setup | Assertion |
|---|-----------|-------|-----------|
| 10 | `mapMcpServers_zeroServers_returnsEmptyArray` | Empty MCP servers list | Returns `[]` |
| 11 | `mapMcpServers_oneServer_returnsSingleMappedServer` | 1 MCP server (firecrawl) | Returns array with 1 entry; `id === "firecrawl"`, `command` is split array, `env` has `API_KEY` |
| 12 | `mapMcpServers_multipleServers_returnsAllMapped` | 2 MCP servers (firecrawl + docs) | Returns array with 2 entries preserving order |
| 13 | `mapMcpServers_serverWithEnv_mapsEnvVariables` | MCP server with `env: { API_KEY: "key", DOCS_PATH: "/path" }` | `env` object contains both key-value pairs |
| 14 | `mapMcpServers_serverWithoutEnv_returnsEmptyEnv` | MCP server with no env vars | `env` is `{}` |

#### Assertions Pattern

```typescript
// Test 10
const mapped = mapMcpServers([]);
expect(mapped).toEqual([]);

// Test 11
const mapped = mapMcpServers([MCP_FIRECRAWL]);
expect(mapped).toHaveLength(1);
expect(mapped[0].id).toBe("firecrawl");
expect(mapped[0].command).toEqual(["npx", "-y", "@anthropic-ai/firecrawl-mcp"]);
expect(mapped[0].env).toEqual({ API_KEY: "test-key-123" });

// Test 12
const mapped = mapMcpServers([MCP_FIRECRAWL, MCP_DOCS]);
expect(mapped).toHaveLength(2);
expect(mapped[0].id).toBe("firecrawl");
expect(mapped[1].id).toBe("docs");

// Test 14
const mapped = mapMcpServers([MCP_SIMPLE]);
expect(mapped[0].env).toEqual({});
```

---

### Group 4: assemble() (10 tests)

Unit tests for the full assemble orchestration covering hooks detection, context building, template rendering, and file writing.

| # | Test Name | Setup | Assertion |
|---|-----------|-------|-----------|
| 15 | `assemble_simpleConfig_generatesConfigToml` | `aMinimalProjectConfig()`, no hooks, no MCP | File `.codex/config.toml` exists in output directory |
| 16 | `assemble_simpleConfig_returnsFileAndNoWarnings` | `aMinimalProjectConfig()` | Returns `{ files: ["...config.toml"], warnings: [] }` |
| 17 | `assemble_simpleConfig_containsDefaultValues` | `aMinimalProjectConfig()`, no hooks | TOML contains `model = "o4-mini"`, `approval_policy = "untrusted"`, `mode = "workspace-write"` |
| 18 | `assemble_simpleConfig_omitsMcpSection` | `aMinimalProjectConfig()`, no MCP | TOML does NOT contain `[mcp_servers` |
| 19 | `assemble_withHooks_setsApprovalPolicyOnRequest` | `aFullProjectConfig()`, hooks seeded in `.claude/hooks/` | TOML contains `approval_policy = "on-request"` |
| 20 | `assemble_withMcpServers_generatesMcpSections` | Config with 2 MCP servers | TOML contains `[mcp_servers.firecrawl]` and `[mcp_servers.docs]` |
| 21 | `assemble_withMcpEnv_generatesEnvSection` | Config with MCP server having env vars | TOML contains `API_KEY = "test-key-123"` |
| 22 | `assemble_outputIsValidToml_parsesWithoutError` | Any valid config | `TOML.parse(content)` does not throw |
| 23 | `assemble_codexDirNotExists_createsDirectory` | No `.codex/` directory pre-exists | `.codex/` directory is created and contains `config.toml` |
| 24 | `assemble_fullConfig_noTemplateArtifacts` | Full config | Generated config.toml does NOT match `/\{\{/`, `/\{%/`, `/\{#/` |

#### Setup

```typescript
const config = aMinimalProjectConfig();
const engine = new TemplateEngine(RESOURCES_DIR, config);
const assembler = new CodexConfigAssembler();
const codexDir = join(tempDir, ".codex");

// For hooks tests (test 19):
await seedHooksDir(tempDir);

// For MCP tests (tests 20, 21):
const mcpConfig = configWithMcp([MCP_FIRECRAWL, MCP_DOCS]);

const result = assembler.assemble(config, codexDir, RESOURCES_DIR, engine);
```

#### Assertions Pattern

```typescript
// Test 15
const configToml = join(codexDir, "config.toml");
expect(fs.existsSync(configToml)).toBe(true);

// Test 16
expect(result.files).toHaveLength(1);
expect(result.files[0]).toContain("config.toml");
expect(result.warnings).toEqual([]);

// Test 17
const content = fs.readFileSync(join(codexDir, "config.toml"), "utf-8");
expect(content).toContain('model = "o4-mini"');
expect(content).toContain('approval_policy = "untrusted"');
expect(content).toContain('mode = "workspace-write"');

// Test 18
expect(content).not.toContain("[mcp_servers");

// Test 19
expect(content).toContain('approval_policy = "on-request"');

// Test 20
expect(content).toContain("[mcp_servers.firecrawl]");
expect(content).toContain("[mcp_servers.docs]");

// Test 22
import * as TOML from "@iarna/toml"; // or smol-toml
expect(() => TOML.parse(content)).not.toThrow();

// Test 23
expect(fs.existsSync(codexDir)).toBe(true);
expect(fs.existsSync(join(codexDir, "config.toml"))).toBe(true);

// Test 24
expect(content).not.toMatch(/\{\{/);
expect(content).not.toMatch(/\{%/);
expect(content).not.toMatch(/\{#/);
```

---

### Group 5: Pipeline Integration (3 tests)

Integration tests validating that `CodexConfigAssembler` is correctly registered in the pipeline.

| # | Test Name | Assertion |
|---|-----------|-----------|
| 25 | `pipeline_assemblerList_includesCodexConfigAssembler` | `buildAssemblers()` returns a descriptor with name `"CodexConfigAssembler"` |
| 26 | `pipeline_assemblerOrder_codexConfigAfterHooksAssembler` | `CodexConfigAssembler` index is greater than `HooksAssembler` (hooks must exist before config detects them) |
| 27 | `pipeline_assemblerTarget_isCodex` | `CodexConfigAssembler` descriptor has `target === "codex"` |

#### Assertions Pattern

```typescript
// Test 25
const assemblers = buildAssemblers();
const names = assemblers.map((a) => a.name);
expect(names).toContain("CodexConfigAssembler");

// Test 26
const assemblers = buildAssemblers();
const names = assemblers.map((a) => a.name);
const configIdx = names.indexOf("CodexConfigAssembler");
const hooksIdx = names.indexOf("HooksAssembler");
expect(configIdx).toBeGreaterThan(hooksIdx);

// Test 27
const assemblers = buildAssemblers();
const descriptor = assemblers.find((a) => a.name === "CodexConfigAssembler");
expect(descriptor!.target).toBe("codex");
```

---

### Group 6: Edge Cases (3 tests)

Edge case tests for error handling and boundary conditions.

| # | Test Name | Setup | Assertion |
|---|-----------|-------|-----------|
| 28 | `assemble_templateNotFound_returnsWarningAndEmptyFiles` | Fake resources dir with no templates | Returns `{ files: [], warnings: ["Template not found: ..."] }` |
| 29 | `assemble_existingCodexDir_overwritesConfigToml` | Pre-create `.codex/config.toml` with stale content | New `config.toml` is written, stale content replaced |
| 30 | `assemble_existingClaudeAndGithub_doesNotModifyThem` | Seed `.claude/settings.json` and `.github/copilot-instructions.md` before assembling | Files in `.claude/` and `.github/` remain unchanged after assemble (RULE-105) |

#### Assertions Pattern

```typescript
// Test 28
const fakeResourcesDir = join(tempDir, "empty-resources");
fs.mkdirSync(fakeResourcesDir, { recursive: true });
const engine = new TemplateEngine(fakeResourcesDir, config);
const assembler = new CodexConfigAssembler();
const codexDir = join(tempDir, ".codex");
const result = assembler.assemble(config, codexDir, fakeResourcesDir, engine);
expect(result.files).toEqual([]);
expect(result.warnings.some((w) => w.startsWith("Template not found:"))).toBe(true);

// Test 29
const codexDir = join(tempDir, ".codex");
fs.mkdirSync(codexDir, { recursive: true });
fs.writeFileSync(join(codexDir, "config.toml"), "stale content");
const result = assembler.assemble(config, codexDir, RESOURCES_DIR, engine);
const content = fs.readFileSync(join(codexDir, "config.toml"), "utf-8");
expect(content).not.toContain("stale content");
expect(content).toContain('model = "o4-mini"');

// Test 30
await seedExistingOutput(tempDir);
const claudeContent = fs.readFileSync(join(tempDir, ".claude", "settings.json"), "utf-8");
const githubContent = fs.readFileSync(join(tempDir, ".github", "copilot-instructions.md"), "utf-8");
const codexDir = join(tempDir, ".codex");
assembler.assemble(config, codexDir, RESOURCES_DIR, engine);
expect(fs.readFileSync(join(tempDir, ".claude", "settings.json"), "utf-8")).toBe(claudeContent);
expect(fs.readFileSync(join(tempDir, ".github", "copilot-instructions.md"), "utf-8")).toBe(githubContent);
```

---

## 5. Coverage Strategy

### 5.1 Branch Coverage Matrix

Every conditional path in the assembler is exercised in both true and false states:

| Condition | True Test IDs | False Test IDs |
|-----------|---------------|----------------|
| `hasHooks` (hooks directory exists with files) | 1, 6, 19 | 2, 3, 5, 17 |
| `has_mcp` (MCP servers present) | 7, 20, 21 | 8, 10, 18 |
| MCP server has env vars | 13, 21 | 14 |
| `.codex/` directory pre-exists | 29 | 15, 23 |
| Template found in resources | 15-24 | 28 |
| MCP servers count = 0 | 10, 18 | 11, 12, 20 |
| MCP servers count = 1 | 11 | 12 |
| MCP servers count > 1 | 12, 20 | 10, 11, 18 |

### 5.2 Line Coverage

Every code path in the assembler module is exercised:

- **deriveApprovalPolicy()**: Tests 1-3 cover hooks present, hooks absent, and empty hooks directory.
- **buildConfigContext()**: Tests 4-9 cover all context fields including model, sandbox_mode, approval_policy, has_mcp, project_name.
- **mapMcpServers()**: Tests 10-14 cover 0/1/N servers, env vars present/absent.
- **assemble()**: Tests 15-24 cover full execution, hooks detection, MCP generation, TOML validity, directory creation, and output quality.
- **Pipeline wiring**: Tests 25-27 cover assembler registration, ordering, and target.
- **Edge cases**: Tests 28-30 cover template missing, existing output, and RULE-105 compliance.

### 5.3 Coverage Targets

| Metric | Target | Strategy |
|--------|--------|----------|
| Line | >= 95% | All code paths exercised via unit + integration tests |
| Branch | >= 90% | Every conditional (hooks existence, MCP presence, env vars, directory existence) tested in both states |

---

## 6. Test Matrix Summary

| Group | Description | Test Count | Type |
|-------|-------------|------------|------|
| G1: deriveApprovalPolicy | Approval policy derivation from hooks | 3 | Unit |
| G2: buildConfigContext | Context construction for template rendering | 6 | Unit |
| G3: mapMcpServers | MCP server mapping to template context | 5 | Unit |
| G4: assemble | Full assembler orchestration | 10 | Unit |
| G5: Pipeline | Pipeline integration and ordering | 3 | Integration |
| G6: Edge Cases | Error handling and boundary conditions | 3 | Unit |
| **Total** | | **30** | |

---

## 7. Execution Commands

### Run CodexConfigAssembler Tests Only

```bash
npx vitest run tests/node/assembler/codex-config-assembler.test.ts
```

### Run with Coverage

```bash
npx vitest run --coverage tests/node/assembler/codex-config-assembler.test.ts
```

### Full Test Suite (regression check)

```bash
npx vitest run
```

---

## 8. Dependencies and Prerequisites

### Prerequisites

- STORY-021 templates exist in `resources/codex-templates/` (specifically `config.toml.njk`)
- `aFullProjectConfig()` and `aMinimalProjectConfig()` fixtures available in `tests/fixtures/project-config.fixture.ts`
- `McpConfig` and `McpServerConfig` classes available in `src/models.ts`
- `TemplateEngine` with `renderTemplate()` functional
- `buildAssemblers()` in `src/assembler/pipeline.ts` updated to include `CodexConfigAssembler`
- TOML parsing library available for validation test (test 22)

### Import Dependencies

| Module | Import | Used For |
|--------|--------|----------|
| `src/assembler/codex-config-assembler.ts` | `CodexConfigAssembler`, `deriveApprovalPolicy`, `buildConfigContext`, `mapMcpServers` | Module under test |
| `src/assembler/pipeline.ts` | `buildAssemblers` | Pipeline integration tests |
| `src/template-engine.ts` | `TemplateEngine` | Rendering templates |
| `src/models.ts` | `McpConfig`, `McpServerConfig` | MCP server fixtures |
| `tests/fixtures/project-config.fixture.ts` | `aFullProjectConfig`, `aMinimalProjectConfig` | Fixture configs |
| `vitest` | `describe`, `it`, `expect`, `beforeEach`, `afterEach` | Test framework |
| `node:fs/promises` | `mkdtemp`, `rm`, `mkdir`, `writeFile` | Temp directory management |
| `node:fs` | `existsSync`, `readFileSync`, `mkdirSync`, `writeFileSync` | File existence and content checks |
| `node:path` | `join`, `resolve`, `dirname` | Path resolution |
| TOML parser (e.g., `smol-toml` or `@iarna/toml`) | `parse` | TOML validity check (test 22) |

---

## 9. Vitest Configuration Notes

- The existing `vitest.config.ts` pattern `tests/**/*.test.ts` automatically discovers `tests/node/assembler/codex-config-assembler.test.ts`. No config changes needed.
- Pool: `forks` with `maxForks: 3`, `maxConcurrency: 5` (project-wide setting).
- Each test creates and destroys a temp directory. The `beforeEach`/`afterEach` pattern ensures clean isolation between tests.
- Expected duration: < 3s. The assembler is simpler than CodexAgentsMdAssembler (no directory scanning, one template render).

---

## 10. Risk Areas

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Temp directory cleanup fails on test error | Low | Low | `afterEach` with `force: true` ensures cleanup even on assertion failures |
| TOML parser not installed | Medium | Medium | Test 22 depends on a TOML parsing library. If not available, use a regex-based validation as fallback or add the dependency to devDependencies. |
| McpServerConfig.url field split vs command array | Medium | High | The template expects `server.command` as an array. The model stores `url` as a string that gets split by whitespace. Tests must verify the split produces the correct array. |
| Hooks detection logic differs from STORY-022 | Low | Medium | STORY-023 defines hooks detection as checking `{outputDir}/../.claude/hooks/` for files. Tests 1-3 and 19 validate both presence and absence. The path resolution (`outputDir` is `.codex`, parent is project root) must be correct. |
| Generated TOML not valid due to template whitespace | Medium | High | Test 22 catches this by parsing the output. Test 24 ensures no Nunjucks artifacts remain. |
| Pipeline ordering changes break test 26 | Low | High | Test 26 uses relative ordering (greater-than), not absolute index, so inserting new assemblers does not break it. |
| Race conditions in parallel test execution | Low | Low | Each test uses its own unique temp directory via `mkdtemp`. No shared mutable state. |

---

## 11. Template Context Contract

The `config.toml.njk` template expects these context variables:

| Variable | Type | Source | Required |
|----------|------|--------|----------|
| `project_name` | `string` | `ProjectConfig.project.name` | Yes |
| `model` | `string` | Hardcoded `"o4-mini"` | Yes |
| `approval_policy` | `string` | Derived: `"on-request"` or `"untrusted"` | Yes |
| `sandbox_mode` | `string` | Hardcoded `"workspace-write"` | Yes |
| `mcp_servers` | `Array<{ id, command, env }>` | Mapped from `ProjectConfig.mcp.servers` | No (empty = omit section) |

Tests 4-9 (buildConfigContext) validate that every variable is present and correctly typed.

---

## 12. Naming Convention Reference

All test names follow `[methodUnderTest]_[scenario]_[expectedBehavior]`:

```
deriveApprovalPolicy_hooksExist_returnsOnRequest
deriveApprovalPolicy_noHooks_returnsUntrusted
deriveApprovalPolicy_emptyHooksDir_returnsUntrusted
buildConfigContext_simpleConfig_returnsModelAndSandboxDefaults
buildConfigContext_noHooks_setsApprovalPolicyUntrusted
buildConfigContext_withHooks_setsApprovalPolicyOnRequest
buildConfigContext_withMcpServers_setsHasMcpTrue
buildConfigContext_noMcpServers_setsHasMcpFalse
buildConfigContext_fullConfig_includesProjectName
mapMcpServers_zeroServers_returnsEmptyArray
mapMcpServers_oneServer_returnsSingleMappedServer
mapMcpServers_multipleServers_returnsAllMapped
mapMcpServers_serverWithEnv_mapsEnvVariables
mapMcpServers_serverWithoutEnv_returnsEmptyEnv
assemble_simpleConfig_generatesConfigToml
assemble_simpleConfig_returnsFileAndNoWarnings
assemble_simpleConfig_containsDefaultValues
assemble_simpleConfig_omitsMcpSection
assemble_withHooks_setsApprovalPolicyOnRequest
assemble_withMcpServers_generatesMcpSections
assemble_withMcpEnv_generatesEnvSection
assemble_outputIsValidToml_parsesWithoutError
assemble_codexDirNotExists_createsDirectory
assemble_fullConfig_noTemplateArtifacts
pipeline_assemblerList_includesCodexConfigAssembler
pipeline_assemblerOrder_codexConfigAfterHooksAssembler
pipeline_assemblerTarget_isCodex
assemble_templateNotFound_returnsWarningAndEmptyFiles
assemble_existingCodexDir_overwritesConfigToml
assemble_existingClaudeAndGithub_doesNotModifyThem
```
