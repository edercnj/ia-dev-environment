# Implementation Plan — STORY-013: HooksAssembler + SettingsAssembler

## Story Summary

Migrate `HooksAssembler` (48 lines) and `SettingsAssembler` (175 lines) from Python to TypeScript.
These two assemblers are complementary: HooksAssembler copies post-compile hook scripts for compiled languages,
and SettingsAssembler generates `settings.json` (with merged permissions and optional hooks section) plus `settings.local.json`.

**Blocked by:** STORY-006 (stack mapping helpers), STORY-008 (assembler helpers) — both complete.
**Blocks:** STORY-016.

---

## 1. Affected Layers and Components

| Layer | Component | Action | Path |
|-------|-----------|--------|------|
| assembler | HooksAssembler | **Create** | `src/assembler/hooks-assembler.ts` |
| assembler | SettingsAssembler | **Create** | `src/assembler/settings-assembler.ts` |
| assembler | Barrel export | **Modify** | `src/assembler/index.ts` |
| domain | Stack mapping (getHookTemplateKey, getSettingsLangKey, getDatabaseSettingsKey, getCacheSettingsKey) | Read-only (already exists) | `src/domain/stack-mapping.ts` |
| resources | Settings templates (20 JSON files) | Read-only (already exists) | `resources/settings-templates/*.json` |
| resources | Hooks templates (7 directories) | Read-only (already exists) | `resources/hooks-templates/*/post-compile-check.sh` |
| tests | HooksAssembler unit tests | **Create** | `tests/node/assembler/hooks-assembler.test.ts` |
| tests | SettingsAssembler unit tests | **Create** | `tests/node/assembler/settings-assembler.test.ts` |

---

## 2. New Classes/Interfaces to Create

### 2.1 `src/assembler/hooks-assembler.ts` — HooksAssembler class

**Purpose:** Copy `post-compile-check.sh` for compiled languages only.

```
export class HooksAssembler {
  assemble(config, outputDir, resourcesDir, engine): string[]
  private copyHook(hookSrc, outputDir): string[]
}
```

**Key logic:**
1. Call `getHookTemplateKey(config.language.name, config.framework.buildTool)` from `src/domain/stack-mapping.ts`.
2. If key is empty string, return `[]` (interpreted language — no hook needed).
3. Resolve source: `${resourcesDir}/hooks-templates/${key}/post-compile-check.sh`.
4. If source file does not exist, return `[]`.
5. Create `${outputDir}/hooks/` directory.
6. Copy file to `${outputDir}/hooks/post-compile-check.sh` using `fs.copyFileSync`.
7. Set executable permission via `fs.chmodSync(dest, 0o755)`.
8. Return `[destPath]`.

**Signature alignment:** Follows the same 4-parameter `assemble(config, outputDir, resourcesDir, engine)` pattern used by `PatternsAssembler` and `ProtocolsAssembler`. The `engine` parameter is accepted for API uniformity but not used (same as ProtocolsAssembler).

**Python parity notes:**
- Python uses `shutil.copy2` + `chmod` with `stat.st_mode | 0o111`. TypeScript uses `fs.copyFileSync` + `fs.chmodSync(dest, 0o755)`.
- Python constructor took `resources_dir` as instance state. TypeScript passes `resourcesDir` per-call to match the established assembler pattern (PatternsAssembler, ProtocolsAssembler, AgentsAssembler all receive `resourcesDir` as a method parameter, not constructor state).

### 2.2 `src/assembler/settings-assembler.ts` — SettingsAssembler class

**Purpose:** Generate `settings.json` and `settings.local.json` with merged, deduplicated permissions.

```
export class SettingsAssembler {
  assemble(config, outputDir, resourcesDir, engine): string[]
  private collectPermissions(config, templatesDir): string[]
  private collectInfraPermissions(config, templatesDir, result): string[]
  private collectDataPermissions(config, templatesDir, result): string[]
  private mergeFile(base, filename, templatesDir): string[]
  private writeSettings(outputDir, settings): string
  private writeSettingsLocal(outputDir): string
}

// Module-level pure functions (exported for testing):
export function mergeJsonArrays(base, overlay): string[]
export function deduplicate(items): string[]
export function readJsonArray(filePath): string[]
export function buildSettingsDict(permissions, hasHooks): SettingsJson
export function buildHooksSection(): HooksSection
```

**Key logic flow:**
1. Derive `templatesDir = path.join(resourcesDir, "settings-templates")`.
2. Collect permissions from multiple JSON sources (each is a JSON array of strings):
   - **Base:** `base.json` (always loaded)
   - **Language:** `${getSettingsLangKey(language, buildTool)}.json` (if key is non-empty)
   - **Infrastructure:**
     - `docker.json` if `config.infrastructure.container` is `"docker"` or `"podman"`
     - `kubernetes.json` if `config.infrastructure.orchestrator` is `"kubernetes"`
     - `docker-compose.json` if `config.infrastructure.orchestrator` is `"docker-compose"`
   - **Data:**
     - `${getDatabaseSettingsKey(db)}.json` if key is non-empty
     - `${getCacheSettingsKey(cache)}.json` if key is non-empty
   - **Testing:** `testing-newman.json` if `config.testing.smokeTests` is `true`
3. Merge is simple concatenation: `[...base, ...overlay]`.
4. Deduplicate preserving insertion order (Set-based).
5. Determine `hasHooks = getHookTemplateKey(language, buildTool) !== ""`.
6. Build settings object:
   ```json
   {
     "permissions": { "allow": [...deduplicated permissions...] },
     "hooks": { ... }  // only if hasHooks
   }
   ```
7. Write `settings.json` with `JSON.stringify(settings, null, 2) + "\n"`.
8. Write `settings.local.json` with `{"permissions": {"allow": []}}`.
9. Return array of both file paths.

**Hooks section structure** (when `hasHooks` is true):
```json
{
  "PostToolUse": [
    {
      "matcher": "Write|Edit",
      "hooks": [
        {
          "type": "command",
          "command": "\"$CLAUDE_PROJECT_DIR\"/.claude/hooks/post-compile-check.sh",
          "timeout": 60,
          "statusMessage": "Checking compilation..."
        }
      ]
    }
  ]
}
```

### 2.3 TypeScript interfaces for settings structure

Defined inside `settings-assembler.ts` (not exported — internal to the module):

```typescript
interface SettingsJson {
  permissions: { allow: string[] };
  hooks?: HooksSection;
}

interface HooksSection {
  PostToolUse: PostToolUseHook[];
}

interface PostToolUseHook {
  matcher: string;
  hooks: HookCommand[];
}

interface HookCommand {
  type: string;
  command: string;
  timeout: number;
  statusMessage: string;
}
```

---

## 3. Existing Classes to Modify

### 3.1 `src/assembler/index.ts` — Add barrel exports

Add at the end:
```typescript
// --- STORY-013: HooksAssembler + SettingsAssembler ---
export * from "./hooks-assembler.js";
export * from "./settings-assembler.js";
```

No other existing files require modification.

---

## 4. Dependency Direction Validation

```
hooks-assembler.ts  ──imports──> domain/stack-mapping.ts (getHookTemplateKey)
                    ──imports──> models.ts (ProjectConfig)

settings-assembler.ts ──imports──> domain/stack-mapping.ts (getHookTemplateKey,
                                    getSettingsLangKey, getDatabaseSettingsKey,
                                    getCacheSettingsKey)
                      ──imports──> models.ts (ProjectConfig)
```

**Validated:** Both assemblers depend on domain layer (stack-mapping) and models.
Neither depends on other assemblers, adapters, or framework code.
Domain layer remains untouched (zero modifications).
The `TemplateEngine` type is imported for signature uniformity but not used by either assembler
(permissions JSON files and shell scripts do not contain template placeholders).

---

## 5. Integration Points

### 5.1 Domain layer (read-only)

Both assemblers consume four functions from `src/domain/stack-mapping.ts`:
- `getHookTemplateKey(language, buildTool)` — returns hook template directory or `""`.
- `getSettingsLangKey(language, buildTool)` — returns settings JSON key or `""`.
- `getDatabaseSettingsKey(dbName)` — returns database settings key or `""`.
- `getCacheSettingsKey(cacheName)` — returns cache settings key or `""`.

All four are already migrated and tested (STORY-006).

### 5.2 Resources (read-only)

- `resources/settings-templates/` — 20 JSON files (arrays of permission strings).
- `resources/hooks-templates/` — 7 subdirectories, each containing `post-compile-check.sh`.

### 5.3 Pipeline integration (future STORY-016)

Both assemblers will be called from the main pipeline orchestrator. The `assemble()` method signature
matches the established pattern: `(config, outputDir, resourcesDir, engine) => string[]`.

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

None. The assemblers read from existing `resources/` templates. No new config is introduced.

---

## 10. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| File permission (chmod) not working on Windows | Low | Low | Use `0o755` which is compatible. Tests should verify on CI. Document that Windows may not respect file modes. |
| JSON parse errors in settings templates | Low | Medium | Implement graceful fallback (return `[]` on malformed JSON) matching Python behavior. |
| Hook command string has embedded quotes | Low | Medium | Use the exact same string as Python: `'"$CLAUDE_PROJECT_DIR"/.claude/hooks/post-compile-check.sh'`. Test serialization. |
| Missing template files for new language combos | Low | Low | Both assemblers gracefully skip missing files (return empty array). |
| Deduplication order sensitivity | Low | Medium | Use insertion-order-preserving Set (JavaScript Set preserves insertion order). Unit test with known duplicate inputs. |

---

## 11. Implementation Groups (Execution Order)

### G1: HooksAssembler (simplest, no dependencies on G2)

**Files:**
- `src/assembler/hooks-assembler.ts`
- `tests/node/assembler/hooks-assembler.test.ts`

**Test scenarios:**
1. `assemble_javaWithMaven_copiesHookScript` — verify file exists and is executable.
2. `assemble_javaWithGradle_copiesHookScript` — verify correct template key resolution.
3. `assemble_kotlinWithGradle_copiesHookScript` — verify kotlin template.
4. `assemble_rustWithCargo_copiesHookScript` — verify rust template.
5. `assemble_csharpWithDotnet_copiesHookScript` — verify csharp template.
6. `assemble_typescriptWithNpm_copiesHookScript` — TypeScript has a hook (key = "typescript").
7. `assemble_pythonWithPip_returnsEmpty` — interpreted language, no hook.
8. `assemble_goWithGo_copiesHookScript` — verify go template.
9. `assemble_unknownLanguage_returnsEmpty` — graceful skip.
10. `assemble_missingTemplateFile_returnsEmpty` — template directory exists but file missing.
11. `assemble_hookFileIsExecutable` — verify chmod 755 was applied.
12. `assemble_createsHooksDirectory` — verify parent dir created.

### G2: SettingsAssembler (depends on stack-mapping, more complex)

**Files:**
- `src/assembler/settings-assembler.ts`
- `tests/node/assembler/settings-assembler.test.ts`

**Test scenarios — Unit (pure functions):**
1. `mergeJsonArrays_combinesTwoArrays` — basic concatenation.
2. `mergeJsonArrays_emptyOverlay_returnsBase` — no-op merge.
3. `mergeJsonArrays_emptyBase_returnsOverlay` — empty starting point.
4. `deduplicate_removeDuplicatesPreservingOrder` — insertion-order preservation.
5. `deduplicate_emptyArray_returnsEmpty`.
6. `deduplicate_noDuplicates_returnsOriginal`.
7. `readJsonArray_validFile_returnsArray` — normal JSON array.
8. `readJsonArray_malformedJson_returnsEmpty` — graceful fallback.
9. `readJsonArray_nonArrayJson_returnsEmpty` — e.g. JSON object instead of array.
10. `readJsonArray_missingFile_returnsEmpty` — file does not exist (handled in mergeFile).
11. `buildSettingsDict_withHooks_includesHooksSection`.
12. `buildSettingsDict_withoutHooks_omitsHooksSection`.
13. `buildHooksSection_correctStructure` — validate exact JSON shape.

**Test scenarios — Integration (assemble method):**
14. `assemble_javaDocker_mergesBaseAndLangAndDocker` — multi-source merge.
15. `assemble_javaDockerPostgres_allFourSources` — base + lang + infra + data.
16. `assemble_javaKubernetes_includesK8sPermissions` — kubernetes orchestrator.
17. `assemble_javaDockerCompose_includesComposePermissions` — docker-compose.
18. `assemble_javaRedis_includesCachePermissions` — cache source.
19. `assemble_javaSmokeTests_includesNewmanPermissions` — testing source.
20. `assemble_python_noHooksSection` — interpreted language, no hooks in settings.
21. `assemble_java_includesHooksSection` — compiled language, hooks in settings.
22. `assemble_deduplicatesPermissions` — overlapping entries across sources.
23. `assemble_settingsLocalTemplate` — verify `settings.local.json` content.
24. `assemble_settingsJsonFormat` — verify 2-space indentation + trailing newline.
25. `assemble_returnsBothFilePaths` — returns exactly 2 paths.
26. `assemble_unknownLanguage_baseOnlyPermissions` — fallback to base.json only.
27. `assemble_podman_treatedLikeDocker` — container "podman" loads docker.json.

### G3: Barrel export + compile check

- Add exports to `src/assembler/index.ts`.
- Run `npx tsc --noEmit` to verify compilation.

---

## 12. Testing Strategy

### Test infrastructure

Each test file uses the same pattern as `patterns-assembler.test.ts`:
- `beforeEach`: create `tmpDir` with `fs.mkdtempSync`, set up `resourcesDir` and `outputDir`.
- `afterEach`: clean up with `fs.rmSync(tmpDir, { recursive: true, force: true })`.
- Helper functions to create fixture files (settings JSON arrays, hook shell scripts).

### Coverage targets

| Metric | Target | Strategy |
|--------|--------|----------|
| Line | >= 95% | Every code path exercised |
| Branch | >= 90% | All conditionals tested (compiled vs interpreted, each infra option, missing files) |

### Config builder helper

Tests will use a `buildConfig()` helper similar to `patterns-assembler.test.ts`:
```typescript
function buildConfig(overrides: {
  language?: string;
  buildTool?: string;
  container?: string;
  orchestrator?: string;
  database?: string;
  cache?: string;
  smokeTests?: boolean;
}): ProjectConfig
```

---

## 13. File-by-File Mapping (Python to TypeScript)

| Python Source | TypeScript Target | Lines (Python) | Notes |
|--------------|------------------|----------------|-------|
| `hooks_assembler.py` constructor | Not needed | 3 | `resourcesDir` passed per-call |
| `hooks_assembler.py` `.assemble()` | `HooksAssembler.assemble()` | 12 | Same logic, TS I/O APIs |
| `hooks_assembler.py` `._copy_hook()` | `HooksAssembler.copyHook()` (private) | 6 | `shutil.copy2` -> `fs.copyFileSync` |
| `settings_assembler.py` constructor | Not needed | 3 | `resourcesDir` passed per-call |
| `settings_assembler.py` `.assemble()` | `SettingsAssembler.assemble()` | 12 | Same flow |
| `settings_assembler.py` `._collect_permissions()` | `SettingsAssembler.collectPermissions()` (private) | 10 | Identical logic |
| `settings_assembler.py` `._collect_infra_permissions()` | `SettingsAssembler.collectInfraPermissions()` (private) | 8 | Identical logic |
| `settings_assembler.py` `._collect_data_permissions()` | `SettingsAssembler.collectDataPermissions()` (private) | 6 | Identical logic |
| `settings_assembler.py` `._merge_file()` | `SettingsAssembler.mergeFile()` (private) | 4 | Identical logic |
| `settings_assembler.py` `._write_settings()` | `SettingsAssembler.writeSettings()` (private) | 4 | `json.dumps` -> `JSON.stringify` |
| `settings_assembler.py` `._write_settings_local()` | `SettingsAssembler.writeSettingsLocal()` (private) | 4 | Same output |
| `settings_assembler.py` `merge_json_arrays()` | `mergeJsonArrays()` (exported) | 2 | Array concatenation |
| `settings_assembler.py` `_deduplicate()` | `deduplicate()` (exported) | 6 | Set-based dedup |
| `settings_assembler.py` `_read_json_array()` | `readJsonArray()` (exported) | 8 | `json.loads` -> `JSON.parse` |
| `settings_assembler.py` `_build_settings_dict()` | `buildSettingsDict()` (exported) | 5 | Identical structure |
| `settings_assembler.py` `_build_hooks_section()` | `buildHooksSection()` (exported) | 14 | Identical JSON |

---

## 14. Acceptance Criteria Checklist

From story Gherkin scenarios:

- [ ] Hook generated for Java with Maven: `post-compile-check.sh` copied to `hooks/` with execute permission.
- [ ] No hook for Python: HooksAssembler returns empty array.
- [ ] Settings merge from multiple sources: `settings.json` contains permissions from base + java + docker + postgresql, no duplicates.
- [ ] Settings local template generated: `settings.local.json` created as empty template.
- [ ] Hooks section in settings for compiled languages: `settings.json` contains `"hooks"` key.

From DoD:

- [ ] HooksAssembler copies hooks for compiled languages only.
- [ ] Hook file marked as executable (`chmod +x`).
- [ ] SettingsAssembler collects and merges permissions from all sources.
- [ ] Permission deduplication functional.
- [ ] `settings.json` and `settings.local.json` generated correctly.
- [ ] Coverage >= 95% line, >= 90% branch.
- [ ] JSDoc on all public classes and exported functions.
