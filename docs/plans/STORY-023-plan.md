# Implementation Plan — STORY-023: CodexConfigAssembler

## 1. Affected Layers and Components

| Component | Path | Action |
|-----------|------|--------|
| New assembler | `src/assembler/codex-config-assembler.ts` | Create |
| Pipeline integration | `src/assembler/pipeline.ts` | Modify |
| Barrel export | `src/assembler/index.ts` | Modify |
| Test file | `tests/node/assembler/codex-config-assembler.test.ts` | Create |

---

## 2. New Classes/Interfaces to Create

### 2.1 `src/assembler/codex-config-assembler.ts`

**Named constants:**

```typescript
const TEMPLATE_PATH = "codex-templates/config.toml.njk";
const DEFAULT_MODEL = "o4-mini";
const POLICY_ON_REQUEST = "on-request";
const POLICY_UNTRUSTED = "untrusted";
const SANDBOX_WORKSPACE_WRITE = "workspace-write";
```

**Note:** These constants are the same as those already defined in `codex-agents-md-assembler.ts`. They should be duplicated (not shared) since each assembler is an independent module and extracting shared constants would introduce coupling for trivial string literals.

**Class:**

```typescript
export class CodexConfigAssembler {
  assemble(
    config: ProjectConfig,
    outputDir: string,
    _resourcesDir: string,
    engine: TemplateEngine,
  ): AssembleResult;
}
```

**Exported helper functions** (for testability):

```typescript
/** Check if hooks exist in the .claude/ output directory. */
export function detectHooks(claudeDir: string): boolean;

/** Derive approval_policy from hooks presence. */
export function deriveApprovalPolicy(hasHooks: boolean): string;

/** Map McpServerConfig[] to template-ready context objects. */
export function mapMcpServers(
  servers: readonly McpServerConfig[],
): Array<{ id: string; command: string[]; env: Record<string, string> }>;

/** Build the config.toml rendering context. */
export function buildConfigContext(
  config: ProjectConfig,
  hasHooks: boolean,
): Record<string, unknown>;
```

### 2.2 Function Specifications

**`detectHooks(claudeDir: string): boolean`**
- Check if `{claudeDir}/hooks/` exists and contains at least 1 file
- Uses `fs.existsSync()` and `fs.readdirSync()` with file count check
- Returns `false` if directory does not exist or is empty

**`deriveApprovalPolicy(hasHooks: boolean): string`**
- Returns `POLICY_ON_REQUEST` (`"on-request"`) if `hasHooks` is `true`
- Returns `POLICY_UNTRUSTED` (`"untrusted"`) if `hasHooks` is `false`
- Pure function, no side effects

**`mapMcpServers(servers)`**
- Maps each `McpServerConfig` to a template-friendly object:
  ```typescript
  {
    id: s.id,
    command: s.url ? s.url.split(/\s+/) : [],
    env: { ...s.env },
  }
  ```
- The `url` field in `McpServerConfig` contains the full command string (e.g., `"npx -y @anthropic-ai/firecrawl-mcp"`). Splitting by whitespace produces the command array where `command[0]` is the executable and the rest are args
- Returns empty array when `servers` is empty

**`buildConfigContext(config, hasHooks)`**
- Returns:
  ```typescript
  {
    project_name: config.project.name,
    model: DEFAULT_MODEL,
    approval_policy: deriveApprovalPolicy(hasHooks),
    sandbox_mode: SANDBOX_WORKSPACE_WRITE,
    mcp_servers: mapMcpServers(config.mcp.servers),
    has_mcp: config.mcp.servers.length > 0,
  }
  ```
- Note: Does NOT use `buildDefaultContext(config)` — the config.toml template only needs 6 fields. Including all 24 flat fields would be wasteful and misleading

---

## 3. Existing Classes to Modify

### 3.1 `src/assembler/pipeline.ts`

**Change:** Add `CodexConfigAssembler` as the 16th assembler in the `buildAssemblers()` array.

The pipeline already has the `"codex"` target type (added in STORY-022) and the `codexDir` computation in `executeAssemblers()`. No changes needed to the target type or directory resolution logic.

**Position in array:** After `CodexAgentsMdAssembler` (current last entry, index 14). The `CodexConfigAssembler` must run after `HooksAssembler` (index 5), because it checks for hooks in `.claude/hooks/`. Placing it last satisfies this constraint.

```typescript
{ name: "CodexAgentsMdAssembler", target: "codex", assembler: new CodexAgentsMdAssembler() },
{ name: "CodexConfigAssembler", target: "codex", assembler: new CodexConfigAssembler() },
```

**Import to add:**
```typescript
import { CodexConfigAssembler } from "./codex-config-assembler.js";
```

### 3.2 `src/assembler/index.ts`

**Change:** Add export for the new module:

```typescript
// --- STORY-023: CodexConfigAssembler ---
export * from "./codex-config-assembler.js";
```

Place it after the STORY-022 export line and before the pipeline export.

---

## 4. Dependency Direction Validation

```
codex-config-assembler.ts
├── imports from: ../models.ts (ProjectConfig, McpServerConfig)
├── imports from: ../template-engine.ts (TemplateEngine)
├── imports from: ./rules-assembler.ts (AssembleResult)
└── imports from: node:fs, node:path (standard library)
```

**Validation:**
- All dependencies point inward or to peer-level modules within the `assembler/` layer
- No circular dependencies — `codex-config-assembler` is a new leaf module
- Does NOT depend on `codex-agents-md-assembler` — no coupling between codex assemblers
- Does NOT depend on `resolveStack()` or `buildDefaultContext()` — the config.toml template needs only 6 context fields, not the full 24+
- No domain module imports from assembler (direction preserved)

---

## 5. Integration Points

### 5.1 Pipeline Ordering

The assembler MUST run after `HooksAssembler` (index 5), which generates `.claude/hooks/`. The `detectHooks()` function checks for hook files in `.claude/hooks/`.

Placing it as the last assembler (after `CodexAgentsMdAssembler`) satisfies this constraint.

### 5.2 Template Engine

Uses `engine.renderTemplate('codex-templates/config.toml.njk', context)` where context contains:

| Field | Type | Source |
|-------|------|--------|
| `project_name` | `string` | `config.project.name` |
| `model` | `string` | Hardcoded `"o4-mini"` |
| `approval_policy` | `string` | Derived: `has_hooks ? "on-request" : "untrusted"` |
| `sandbox_mode` | `string` | Hardcoded `"workspace-write"` |
| `mcp_servers` | `Array<{ id, command, env }>` | `mapMcpServers(config.mcp.servers)` |
| `has_mcp` | `boolean` | `config.mcp.servers.length > 0` |

### 5.3 Template File

The template `resources/codex-templates/config.toml.njk` already exists (created in STORY-021). It uses:
- `{{ model }}`, `{{ approval_policy }}`, `{{ sandbox_mode }}` for scalar values
- `{% if mcp_servers and mcp_servers.length > 0 %}` for conditional MCP section
- `{% for server in mcp_servers %}` to iterate servers
- `{% for part in server.command %}` to render the command array
- `{% for key, value in server.env %}` to render env vars

**Template parity check:** The template references `server.id`, `server.command`, `server.env` — all provided by `mapMcpServers()`. The template does NOT reference `project_name` (only used in the comment header). Verified: the template header line `# Codex CLI configuration for {{ project_name }}` does use it.

### 5.4 File System Operations

**Read operations:**
- `{claudeDir}/hooks/` — Check directory existence and file count

**Write operations:**
- `{codexDir}/config.toml` — Rendered template output

The assembler receives `outputDir = {tempDir}/.codex/` (when target is `"codex"`). To find `.claude/`, it navigates up one level:
```typescript
const claudeDir = path.join(path.dirname(outputDir), ".claude");
```

This is the same pattern used by `CodexAgentsMdAssembler`.

### 5.5 Reuse of `isAccessibleDirectory` Helper

The `isAccessibleDirectory()` function exists in `codex-agents-md-assembler.ts` as a private module-level function. For STORY-023, two options:

1. **Option A (Recommended):** Duplicate the 5-line helper in `codex-config-assembler.ts`. Keeps assemblers independent. The function is trivial.
2. **Option B:** Extract to a shared utility. Requires modifying `codex-agents-md-assembler.ts` (out of scope for this story).

Decision: **Option A** — duplicate the helper. Avoids cross-story changes.

---

## 6. Database Changes

None.

---

## 7. API Changes

None.

---

## 8. Event Changes

None.

---

## 9. Configuration Changes

None.

---

## 10. Risk Assessment

### 10.1 MCP Server `url` Splitting

**Risk:** The `McpServerConfig.url` field stores the command as a single string (e.g., `"npx -y @anthropic-ai/firecrawl-mcp"`). Splitting by `/\s+/` works for simple commands but could break if paths contain spaces.

**Mitigation:** This is the same splitting logic used in `CodexAgentsMdAssembler.buildExtendedContext()` (line 170 of the existing assembler). The YAML schema does not support quoted paths with spaces — this is a known limitation documented by the MCP config format. Consistent behavior across both assemblers.

### 10.2 Empty `url` Field

**Risk:** A `McpServerConfig` could have an empty or whitespace-only `url` string.

**Mitigation:** The `mapMcpServers()` function guards with `s.url ? s.url.split(/\s+/) : []`, producing an empty command array. The template will render `command = []` which is valid TOML. An additional guard to filter out entries with empty `url` could be added as a defensive measure, emitting a warning.

### 10.3 TOML Validity

**Risk:** The rendered output must be valid TOML. Special characters in MCP server env values (quotes, backslashes) could break TOML syntax.

**Mitigation:** The Nunjucks template wraps values in double quotes (`"{{ value }}"`). For STORY-023, values are expected to be simple strings (API keys, tokens). If escaping becomes necessary, a Nunjucks filter can be added in a future story. Add a test that parses the rendered output with a TOML parser to validate syntax.

### 10.4 Pipeline Test Count

**Risk:** `tests/node/assembler/pipeline.test.ts` likely asserts `buildAssemblers().length === 15` (updated in STORY-022). Adding a 16th assembler will fail this assertion.

**Mitigation:** Update the count assertion from 15 to 16 in the pipeline test.

### 10.5 Hooks Detection vs. Config Declaration

**Risk:** The story specifies two ways to detect hooks: (1) check if `{outputDir}/.claude/hooks/` exists and contains files, OR (2) check if `config` declares hooks. The `CodexAgentsMdAssembler` only checks directory existence (`fs.existsSync`), not file count.

**Mitigation:** For consistency with `CodexAgentsMdAssembler`, use the same `fs.existsSync()` check on the hooks directory. The `HooksAssembler` creates the hooks directory only if hooks are configured, so directory existence is a reliable proxy for "hooks are configured." However, the story specifies "contains at least 1 file" — implement this stricter check with `fs.readdirSync().length > 0` for correctness.

---

## 11. Detailed Implementation Steps

### Phase 1: Create `src/assembler/codex-config-assembler.ts`

1. Define named constants: `TEMPLATE_PATH`, `DEFAULT_MODEL`, `POLICY_ON_REQUEST`, `POLICY_UNTRUSTED`, `SANDBOX_WORKSPACE_WRITE`
2. Implement `isAccessibleDirectory(dirPath)` — duplicated 5-line helper
3. Implement `detectHooks(claudeDir)`:
   - Check if `{claudeDir}/hooks/` is an accessible directory
   - If yes, check `fs.readdirSync().length > 0`
   - Return boolean
4. Implement `deriveApprovalPolicy(hasHooks)`:
   - Return `POLICY_ON_REQUEST` if true, `POLICY_UNTRUSTED` if false
5. Implement `mapMcpServers(servers)`:
   - Map each server to `{ id, command: url.split(/\s+/), env: { ...s.env } }`
   - Guard against empty/undefined `url`
6. Implement `buildConfigContext(config, hasHooks)`:
   - Return object with `project_name`, `model`, `approval_policy`, `sandbox_mode`, `mcp_servers`, `has_mcp`
7. Implement `renderAndWrite(engine, context, outputDir, warnings)`:
   - Call `engine.renderTemplate(TEMPLATE_PATH, context)`
   - Handle template-not-found by adding warning and returning empty files
   - Create output directory with `mkdirSync({ recursive: true })`
   - Write `config.toml` to outputDir
   - Return `AssembleResult`
8. Implement `CodexConfigAssembler.assemble()`:
   - Navigate from outputDir (`.codex/`) to `.claude/` via `path.dirname(outputDir)`
   - Call `detectHooks(claudeDir)`
   - Call `buildConfigContext(config, hasHooks)`
   - Call `renderAndWrite(engine, context, outputDir, warnings)`
   - Return `AssembleResult`

### Phase 2: Integrate into Pipeline

1. Add `import { CodexConfigAssembler }` to `pipeline.ts`
2. Add descriptor as last entry in `buildAssemblers()`:
   ```typescript
   { name: "CodexConfigAssembler", target: "codex", assembler: new CodexConfigAssembler() },
   ```
3. Update `index.ts` with new export line

### Phase 3: Create Tests

Test file: `tests/node/assembler/codex-config-assembler.test.ts`

**Test categories:**

1. **`detectHooks` unit tests:**
   - `detectHooks_noDir_returnsFalse`
   - `detectHooks_emptyDir_returnsFalse`
   - `detectHooks_withFiles_returnsTrue`
   - `detectHooks_nonDirectory_returnsFalse`

2. **`deriveApprovalPolicy` unit tests:**
   - `deriveApprovalPolicy_hasHooksTrue_returnsOnRequest`
   - `deriveApprovalPolicy_hasHooksFalse_returnsUntrusted`

3. **`mapMcpServers` unit tests:**
   - `mapMcpServers_emptyArray_returnsEmptyArray`
   - `mapMcpServers_singleServer_returnsOneEntry`
   - `mapMcpServers_multipleServers_returnsAll`
   - `mapMcpServers_serverWithEnv_copiesEnv`
   - `mapMcpServers_serverWithEmptyUrl_returnsEmptyCommand`
   - `mapMcpServers_serverWithMultiWordUrl_splitsIntoCommandArray`
   - `mapMcpServers_serverWithNoEnv_returnsEmptyEnvObject`

4. **`buildConfigContext` unit tests:**
   - `buildConfigContext_minimalConfig_containsAllRequiredFields`
   - `buildConfigContext_withHooks_approvalPolicyOnRequest`
   - `buildConfigContext_withoutHooks_approvalPolicyUntrusted`
   - `buildConfigContext_modelAlwaysO4Mini`
   - `buildConfigContext_sandboxAlwaysWorkspaceWrite`
   - `buildConfigContext_withMcpServers_hasMcpTrue`
   - `buildConfigContext_noMcpServers_hasMcpFalse`
   - `buildConfigContext_projectName_fromConfig`

5. **`assemble` integration tests:**
   - `assemble_minimalConfig_createsConfigToml`
   - `assemble_minimalConfig_modelIsO4Mini`
   - `assemble_minimalConfig_approvalPolicyUntrusted`
   - `assemble_minimalConfig_sandboxWorkspaceWrite`
   - `assemble_minimalConfig_noMcpSection`
   - `assemble_withHooks_approvalPolicyOnRequest`
   - `assemble_withMcpServers_generatesServerSections`
   - `assemble_withMcpServerEnv_generatesEnvSection`
   - `assemble_outputIsValidToml` (parse with TOML library)
   - `assemble_createsCodexDirectory`
   - `assemble_returnsCorrectFilePath`
   - `assemble_returnsEmptyWarnings_normalCase`
   - `assemble_claudeDirUnmodified` (RULE-105)
   - `assemble_githubDirUnmodified` (RULE-105)
   - `assemble_templateNotFound_warningEmitted`

6. **Pipeline integration tests:**
   - `buildAssemblers_returns16Assemblers`
   - `buildAssemblers_codexConfigAssemblerIsLast`

7. **Edge cases:**
   - `assemble_multipleMcpServers_allRendered`
   - `assemble_mcpServerWithMultipleArgs_correctlySplit`
   - `assemble_mcpServerWithEmptyEnv_noEnvSection`

**Estimated test count: ~35 tests**

### Phase 4: Pipeline Test Updates

Update `tests/node/assembler/pipeline.test.ts`:
- Adjust `buildAssemblers().length` assertion from 15 to 16
- Add assertion that the last assembler is `CodexConfigAssembler`

---

## 12. File Inventory

### New Files (2)

| File | Lines (est.) |
|------|-------------|
| `src/assembler/codex-config-assembler.ts` | ~80 |
| `tests/node/assembler/codex-config-assembler.test.ts` | ~350 |

### Modified Files (2)

| File | Change |
|------|--------|
| `src/assembler/pipeline.ts` | Add import + descriptor entry (2 lines) |
| `src/assembler/index.ts` | Add 1 export line |

### Test Files Modified (1)

| File | Change |
|------|--------|
| `tests/node/assembler/pipeline.test.ts` | Update assembler count assertion (15 -> 16) |

---

## 13. Acceptance Criteria Traceability

| Gherkin Scenario | Test(s) |
|-----------------|---------|
| config.toml with defaults for simple project | `assemble_minimalConfig_*` (model, approval, sandbox, noMcp) |
| approval on-request when hooks present | `assemble_withHooks_approvalPolicyOnRequest` |
| MCP servers mapped 1:1 | `assemble_withMcpServers_generatesServerSections`, `assemble_multipleMcpServers_allRendered` |
| MCP section omitted when no servers | `assemble_minimalConfig_noMcpSection` |
| Output is valid TOML | `assemble_outputIsValidToml` |
| `.codex/` directory created automatically | `assemble_createsCodexDirectory` |
| `.claude/` and `.github/` output unmodified | `assemble_claudeDirUnmodified`, `assemble_githubDirUnmodified` |

---

## 14. Comparison with CodexAgentsMdAssembler (STORY-022)

| Aspect | STORY-022 (AGENTS.md) | STORY-023 (config.toml) |
|--------|----------------------|------------------------|
| Complexity | 3 phases, ~250 lines | 2 phases, ~80 lines |
| Context fields | 24 default + 10 extended = 34 | 6 fields only |
| Dependencies | resolveStack, buildDefaultContext, js-yaml | None beyond models |
| FS reads | agents/, skills/, hooks/ | hooks/ only |
| Template | agents-md.md.njk | config.toml.njk |
| Output | .codex/AGENTS.md | .codex/config.toml |
| Reused constants | All 5 named constants | All 5 named constants (duplicated) |
| MCP mapping | Same url.split logic | Same url.split logic |

The CodexConfigAssembler is the simplest assembler in the pipeline. It has no dependency on `resolveStack`, `buildDefaultContext`, `js-yaml`, or agent/skill scanning. The only non-trivial logic is hooks detection and MCP server mapping.
