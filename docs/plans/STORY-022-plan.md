# Implementation Plan — STORY-022: CodexAgentsMdAssembler

## 1. Affected Layers and Components

| Component | Path | Action |
|-----------|------|--------|
| New assembler | `src/assembler/codex-agents-md-assembler.ts` | Create |
| Pipeline integration | `src/assembler/pipeline.ts` | Modify |
| Barrel export | `src/assembler/index.ts` | Modify |
| Test file | `tests/node/assembler/codex-agents-md-assembler.test.ts` | Create |

---

## 2. New Classes/Interfaces to Create

### 2.1 `src/assembler/codex-agents-md-assembler.ts`

**Internal interfaces:**

```typescript
/** Metadata extracted from a generated agent .md file. */
interface AgentInfo {
  readonly name: string;
  readonly description: string;
}

/** Metadata extracted from a generated skill's SKILL.md frontmatter. */
interface SkillInfo {
  readonly name: string;
  readonly description: string;
  readonly user_invocable: boolean;
}
```

**Class:**

```typescript
export class CodexAgentsMdAssembler {
  assemble(
    config: ProjectConfig,
    outputDir: string,
    resourcesDir: string,
    engine: TemplateEngine,
  ): AssembleResult;
}
```

**Exported helper functions** (for testability):

```typescript
export function scanAgents(agentsDir: string): AgentInfo[];
export function scanSkills(skillsDir: string): SkillInfo[];
export function buildExtendedContext(
  config: ProjectConfig,
  engine: TemplateEngine,
  agents: AgentInfo[],
  skills: SkillInfo[],
  hasHooks: boolean,
): Record<string, unknown>;
```

---

## 3. Existing Classes to Modify

### 3.1 `src/assembler/pipeline.ts`

**Change:** Add `CodexAgentsMdAssembler` to the `buildAssemblers()` array and introduce a new `AssemblerTarget` value `"codex"`.

**Current pipeline target types:**
```typescript
export type AssemblerTarget = "claude" | "github";
```

**New target type:**
```typescript
export type AssemblerTarget = "claude" | "github" | "codex";
```

**Rationale:** The CodexAgentsMdAssembler writes to `{outputDir}/.codex/`, which is neither `.claude/` nor `.github/`. Adding a `"codex"` target value keeps the pipeline clean and explicit. The `executeAssemblers()` function already computes `targetDir` based on the target value — we add a third branch:

```typescript
const claudeDir = join(outputDir, ".claude");
const githubDir = join(outputDir, ".github");
const codexDir = join(outputDir, ".codex");
// ...
const targetDir = target === "github"
  ? githubDir
  : target === "codex"
    ? codexDir
    : claudeDir;
```

**Position in array:** After `ReadmeAssembler` (index 14, last assembler), because:
- It must run AFTER `AgentsAssembler` (index 2) and `SkillsAssembler` (index 1) — it scans their output
- It must run AFTER `HooksAssembler` (index 5) — it checks for hooks directory
- Placing it last in the pipeline is the safest position

```typescript
{ name: "ReadmeAssembler", target: "claude", assembler: new ReadmeAssembler() },
{ name: "CodexAgentsMdAssembler", target: "codex", assembler: new CodexAgentsMdAssembler() },
```

**Note:** The `CodexAgentsMdAssembler` receives `outputDir = {tempDir}/.codex/` as its `outputDir` parameter. However, it needs to READ from `{tempDir}/.claude/agents/` and `{tempDir}/.claude/skills/`. To navigate from `.codex/` to `.claude/`, it uses `path.resolve(outputDir, '..', '.claude')`. This follows the same pattern that `ReadmeAssembler` uses — it receives the `.claude/` dir but operates within it.

### 3.2 `src/assembler/index.ts`

**Change:** Add export for the new module:

```typescript
// --- STORY-022: CodexAgentsMdAssembler ---
export * from "./codex-agents-md-assembler.js";
```

---

## 4. Dependency Direction Validation

```
codex-agents-md-assembler.ts
├── imports from: ../models.ts (ProjectConfig)
├── imports from: ../template-engine.ts (TemplateEngine, buildDefaultContext)
├── imports from: ../domain/resolver.ts (resolveStack)
├── imports from: ../domain/resolved-stack.ts (ResolvedStack)
├── imports from: ./rules-assembler.ts (AssembleResult)
└── imports from: node:fs, node:path (standard library)
```

**Validation:**
- All dependencies point inward or to peer-level modules within the `assembler/` layer
- No circular dependencies introduced — `codex-agents-md-assembler` is a new leaf module
- No domain module imports from assembler (direction preserved)
- `resolveStack` is called internally, not passed as parameter — avoids changing the `AssemblerDescriptor` interface signature

---

## 5. Integration Points

### 5.1 Pipeline Ordering

The assembler MUST run after all of these:
- `AgentsAssembler` (generates `.claude/agents/*.md`)
- `SkillsAssembler` (generates `.claude/skills/*/SKILL.md`)
- `HooksAssembler` (generates `.claude/hooks/`)

Placing it as the last assembler (after `ReadmeAssembler`) satisfies all ordering constraints.

### 5.2 Template Engine

Uses `engine.renderTemplate('codex-templates/agents-md.md.njk', extendedContext)` where:
- The 24 default context fields come from `buildDefaultContext(config)` (already available via engine)
- Extended fields are added by `buildExtendedContext()`:

| Extended Field | Type | Source |
|---------------|------|--------|
| `resolved_stack` | `{ buildCmd, testCmd, compileCmd, coverageCmd }` | `resolveStack(config)` |
| `agents_list` | `AgentInfo[]` | `scanAgents()` |
| `skills_list` | `SkillInfo[]` | `scanSkills()` |
| `has_hooks` | `boolean` | `fs.existsSync(hooksDir)` |
| `mcp_servers` | `McpServerConfig[]` (from config) | `config.mcp.servers` |
| `security_frameworks` | `string[]` | `config.security.frameworks` |
| `observability` | `string` | `config.infrastructure.observability.tool` |
| `model` | `string` | Hardcoded `"o4-mini"` (Codex default) |
| `approval_policy` | `string` | Derived: `has_hooks ? "on-request" : "untrusted"` |
| `sandbox_mode` | `string` | Hardcoded `"workspace-write"` |

### 5.3 ResolvedStack

The assembler computes `resolveStack(config)` internally rather than receiving it as a parameter. This avoids changing the `AssemblerDescriptor.assemble()` signature (which would be a breaking change to all existing assemblers).

**Note:** The story specifies `resolvedStack` as a parameter to `assemble()`, but the existing `AssemblerDescriptor` interface only passes `(config, outputDir, resourcesDir, engine)`. Options:

1. **Option A (Recommended):** Compute `resolveStack(config)` inside the assembler. No interface changes needed. The story signature is an aspirational interface, not a hard constraint on the pipeline integration.
2. **Option B:** Change `AssemblerDescriptor` interface to optionally pass `resolvedStack`. This would require updating all 14 existing assemblers. Too invasive for STORY-022.

Decision: **Option A**.

### 5.4 File System Operations

**Read operations:**
- `{claudeDir}/agents/*.md` — Read first line to extract description
- `{claudeDir}/skills/*/SKILL.md` — Read YAML frontmatter for name, description, user-invocable
- `{claudeDir}/hooks/` — Check directory existence

**Write operations:**
- `{codexDir}/AGENTS.md` — Rendered template output

The assembler receives `outputDir = {tempDir}/.codex/` (when target is `"codex"`), so it must navigate up one level to find `.claude/`:
```typescript
const rootDir = path.dirname(outputDir);     // {tempDir}
const claudeDir = path.join(rootDir, ".claude");
```

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

### 10.1 Pipeline AssemblerDescriptor Signature Mismatch

**Risk:** The story specifies `resolvedStack` as a 5th parameter to `assemble()`, but the pipeline interface only passes 4 parameters.

**Mitigation:** Compute `resolveStack(config)` inside the assembler. The `resolveStack` function is a pure computation with no side effects — calling it inside the assembler is safe and idempotent.

### 10.2 New AssemblerTarget `"codex"`

**Risk:** Adding a third target type changes the `AssemblerTarget` union and the `executeAssemblers()` function.

**Mitigation:**
- The change is additive (union grows from 2 to 3 members)
- `executeAssemblers()` only has one conditional (`target === "github" ? githubDir : claudeDir`) — we extend it with an explicit ternary
- Existing assembler tests are unaffected (they don't test the target enum directly)
- The pipeline test (`pipeline.test.ts`) may need a count update if it asserts on `buildAssemblers().length`

### 10.3 Path Navigation from `.codex/` to `.claude/`

**Risk:** Using `path.dirname(outputDir)` to navigate up from `.codex/` to find `.claude/` is fragile — it assumes the directory structure `{root}/.codex/` and `{root}/.claude/` are siblings.

**Mitigation:** This assumption is guaranteed by the pipeline's `executeAssemblers()` function, which computes `claudeDir`, `githubDir`, and `codexDir` from the same `outputDir` root. The assembler validates the existence of `.claude/agents/` and `.claude/skills/` before scanning, and returns warnings (not errors) if they are missing.

### 10.4 Output Directory `.codex/` Does Not Exist in `atomicOutput`

**Risk:** The `atomicOutput` utility in `utils.ts` moves the temp directory to the final destination. The `.codex/` directory is created inside the temp dir by the assembler. The `atomicOutput` utility moves the entire temp dir, so `.codex/` will be included automatically.

**Mitigation:** No special handling needed — `atomicOutput` operates on the entire temp dir tree. The `codexDir` is created by `mkdirSync({ recursive: true })` inside the assembler.

### 10.5 Pipeline Test Count

**Risk:** `tests/node/assembler/pipeline.test.ts` likely asserts `buildAssemblers().length === 14`. Adding a 15th assembler will fail this assertion.

**Mitigation:** Update the count assertion from 14 to 15 in the pipeline test.

---

## 11. Detailed Implementation Steps

### Phase 1: Create `src/assembler/codex-agents-md-assembler.ts`

1. Define `AgentInfo` and `SkillInfo` interfaces
2. Implement `scanAgents(agentsDir)`:
   - List `*.md` files in directory
   - For each file, extract name (filename without `.md`) and description (first `# ...` heading or first non-empty line)
   - Return sorted `AgentInfo[]`
   - Return empty array if directory does not exist
3. Implement `scanSkills(skillsDir)`:
   - List subdirectories containing `SKILL.md`
   - For each `SKILL.md`, parse YAML frontmatter to extract `name`, `description`, `user-invocable`
   - Default `user_invocable` to `true` if not specified
   - Return sorted `SkillInfo[]`
   - Return empty array if directory does not exist
4. Implement `buildExtendedContext()`:
   - Compute `resolveStack(config)` for resolved commands
   - Extract observability, security frameworks, MCP servers from config
   - Merge with agents/skills lists and hooks flag
   - Return the extended context record
5. Implement `CodexAgentsMdAssembler.assemble()`:
   - Navigate from `outputDir` (`.codex/`) up to root, then to `.claude/`
   - Call `scanAgents()` and `scanSkills()`
   - Call `buildExtendedContext()`
   - Call `engine.renderTemplate('codex-templates/agents-md.md.njk', context)`
   - Create output directory, write `AGENTS.md`
   - Collect warnings for empty agents/skills lists
   - Return `AssembleResult`

### Phase 2: Integrate into Pipeline

1. Add `import { CodexAgentsMdAssembler }` to `pipeline.ts`
2. Extend `AssemblerTarget` type with `"codex"`
3. Add `codexDir` computation in `executeAssemblers()`
4. Add assembler descriptor as last entry in `buildAssemblers()`
5. Update `index.ts` exports

### Phase 3: Create Tests

Test file: `tests/node/assembler/codex-agents-md-assembler.test.ts`

**Test categories:**

1. **`scanAgents` unit tests:**
   - `scanAgents_emptyDir_returnsEmptyArray`
   - `scanAgents_noDir_returnsEmptyArray`
   - `scanAgents_singleAgent_returnsOneAgentInfo`
   - `scanAgents_multipleAgents_returnsSortedArray`
   - `scanAgents_agentWithHashTitle_extractsTitle`
   - `scanAgents_agentWithNoTitle_usesFirstLine`

2. **`scanSkills` unit tests:**
   - `scanSkills_emptyDir_returnsEmptyArray`
   - `scanSkills_noDir_returnsEmptyArray`
   - `scanSkills_singleSkill_returnsOneSkillInfo`
   - `scanSkills_multipleSkills_returnsSortedArray`
   - `scanSkills_knowledgePack_userInvocableFalse`
   - `scanSkills_userInvocableDefault_true`
   - `scanSkills_dirWithoutSkillMd_skipped`

3. **`buildExtendedContext` unit tests:**
   - `buildExtendedContext_fullConfig_containsAllFields`
   - `buildExtendedContext_resolvedStackIncluded_hasBuildCmd`
   - `buildExtendedContext_securityFrameworks_fromConfig`
   - `buildExtendedContext_noMcpServers_emptyArray`
   - `buildExtendedContext_observabilityFromConfig_usesTool`

4. **`assemble` integration tests:**
   - `assemble_fullConfig_createsAgentsMd`
   - `assemble_fullConfig_containsAllSections`
   - `assemble_minimalConfig_omitsDomainSection`
   - `assemble_minimalConfig_omitsSecuritySection`
   - `assemble_noAgents_warningEmitted`
   - `assemble_noSkills_warningEmitted`
   - `assemble_noAgentsNoSkills_bothWarnings`
   - `assemble_createsCodexDirectory`
   - `assemble_returnsCorrectFilePath`
   - `assemble_renderedOutput_noTemplateArtifacts`
   - `assemble_claudeDirUnmodified` (RULE-105)
   - `assemble_githubDirUnmodified` (RULE-105)

5. **Pipeline integration test:**
   - `buildAssemblers_returns15Assemblers`
   - `buildAssemblers_codexAgentsMdAssemblerIsLast`

**Estimated test count: ~25-30 tests**

### Phase 4: Pipeline Test Updates

Update `tests/node/assembler/pipeline.test.ts`:
- Adjust `buildAssemblers().length` assertion from 14 to 15
- Add assertion that the last assembler is `CodexAgentsMdAssembler`

---

## 12. File Inventory

### New Files (2)

| File | Lines (est.) |
|------|-------------|
| `src/assembler/codex-agents-md-assembler.ts` | ~150 |
| `tests/node/assembler/codex-agents-md-assembler.test.ts` | ~400 |

### Modified Files (2)

| File | Change |
|------|--------|
| `src/assembler/pipeline.ts` | Add target type, codexDir, import, descriptor entry |
| `src/assembler/index.ts` | Add 1 export line |

### Test Files Modified (1)

| File | Change |
|------|--------|
| `tests/node/assembler/pipeline.test.ts` | Update assembler count assertion |

---

## 13. Acceptance Criteria Traceability

| Gherkin Scenario | Test(s) |
|-----------------|---------|
| Full config generates AGENTS.md with all sections | `assemble_fullConfig_containsAllSections` |
| Non-DDD config omits Domain section | `assemble_minimalConfig_omitsDomainSection` |
| No security frameworks omits Security section | `assemble_minimalConfig_omitsSecuritySection` |
| Scan agents extracts names and descriptions | `scanAgents_multipleAgents_returnsSortedArray` |
| Scan skills extracts frontmatter YAML | `scanSkills_multipleSkills_returnsSortedArray`, `scanSkills_knowledgePack_userInvocableFalse` |
| Warning when no agents found | `assemble_noAgents_warningEmitted` |
| `.claude/` and `.github/` output unmodified | `assemble_claudeDirUnmodified`, `assemble_githubDirUnmodified` |
