# Task Decomposition -- STORY-003: Models (Python to TypeScript Migration)

**Status:** PLANNED
**Date:** 2026-03-09
**Blocked By:** STORY-001 (project foundation)
**Blocks:** STORY-004 (config loader), STORY-006, STORY-007, STORY-008, STORY-017

---

## G1 -- Foundation

**Dependencies:** None (leaf helper + leaf model, zero imports)

### T1.1 -- Add `require` helper function
- **File:** `src/models.ts`
- **Description:** Add exported function `require(data: Record<string, unknown>, key: string, model: string): unknown`. Check if `key in data` -- if not, throw `Error` with message `"Missing required field '${key}' in ${model}"`. If present, return `data[key]`. Place below existing `DEFAULT_FOUNDATION` constant.
- **Estimated lines:** ~8
- **Tier:** Junior
- **Notes:** Uses `key in data` guard (not `data[key] === undefined`) to handle `noUncheckedIndexedAccess`. Message uses `"in"` per story spec (Python uses `"for"` -- intentional divergence).

### T1.2 -- Add `TechComponent` class
- **File:** `src/models.ts`
- **Description:** Create exported class with `readonly name: string` (default `"none"`) and `readonly version: string` (default `""`). Constructor takes both as optional params with defaults. Static `fromDict(data: Record<string, unknown>): TechComponent` reads `data["name"]` and `data["version"]` with `as string | undefined` cast and `??` defaults. Export from module.
- **Estimated lines:** ~15
- **Tier:** Junior
- **Python reference:** Lines 91-100 of `models.py`

### Compilation checkpoint
```
npx tsc --noEmit   # zero errors with require + TechComponent exported
```
**Expected output:** Clean compilation, zero errors.

---

## G2 -- Simple Models

**Dependencies:** G1 (`require` helper used in all `fromDict` methods)

### T2.1 -- Add `ProjectIdentity` class
- **File:** `src/models.ts`
- **Description:** Class with `readonly name: string` and `readonly purpose: string` (both required). Constructor takes both as required params. `fromDict` uses `require(data, "name", "ProjectIdentity") as string` and same for `purpose`.
- **Estimated lines:** ~14
- **Tier:** Junior
- **Python reference:** Lines 18-27 of `models.py`

### T2.2 -- Add `ArchitectureConfig` class
- **File:** `src/models.ts`
- **Description:** Class with `readonly style: string` (required), `readonly domainDriven: boolean` (default `false`), `readonly eventDriven: boolean` (default `false`). `fromDict` reads snake_case keys: `style`, `domain_driven`, `event_driven`.
- **Estimated lines:** ~16
- **Tier:** Junior
- **Python reference:** Lines 31-42 of `models.py`

### T2.3 -- Add `InterfaceConfig` class
- **File:** `src/models.ts`
- **Description:** Class with `readonly type: string` (required), `readonly spec: string` (default `""`), `readonly broker: string` (default `""`). `fromDict` reads keys: `type`, `spec`, `broker`.
- **Estimated lines:** ~16
- **Tier:** Junior
- **Python reference:** Lines 45-57 of `models.py`

### T2.4 -- Add `LanguageConfig` class
- **File:** `src/models.ts`
- **Description:** Class with `readonly name: string` and `readonly version: string` (both required). `fromDict` uses `require` for both.
- **Estimated lines:** ~14
- **Tier:** Junior
- **Python reference:** Lines 60-70 of `models.py`

### T2.5 -- Add `FrameworkConfig` class
- **File:** `src/models.ts`
- **Description:** Class with `readonly name: string` (required), `readonly version: string` (required), `readonly buildTool: string` (default `"pip"`), `readonly nativeBuild: boolean` (default `false`). `fromDict` reads snake_case keys: `name`, `version`, `build_tool`, `native_build`.
- **Estimated lines:** ~18
- **Tier:** Junior
- **Python reference:** Lines 74-87 of `models.py`

### Compilation checkpoint
```
npx tsc --noEmit   # zero errors with all 5 simple models
```
**Expected output:** Clean compilation, zero errors.

---

## G3 -- Composed Models

**Dependencies:** G2 (some depend on `TechComponent` from G1; all use `require` from G1)

### T3.1 -- Add `DataConfig` class
- **File:** `src/models.ts`
- **Description:** Class with `readonly database: TechComponent`, `readonly migration: TechComponent`, `readonly cache: TechComponent` (all default to `new TechComponent()`). `fromDict` reads each sub-object with `(data["database"] as Record<string, unknown> | undefined) ?? {}` and passes to `TechComponent.fromDict()`.
- **Estimated lines:** ~18
- **Tier:** Mid
- **Python reference:** Lines 107-131 of `models.py`

### T3.2 -- Add `SecurityConfig` class
- **File:** `src/models.ts`
- **Description:** Class with `readonly frameworks: readonly string[]` (default `[]`). `fromDict` reads `data["frameworks"]` with `as string[] | undefined` cast and `?? []` default.
- **Estimated lines:** ~12
- **Tier:** Junior
- **Python reference:** Lines 134-142 of `models.py`

### T3.3 -- Add `ObservabilityConfig` class
- **File:** `src/models.ts`
- **Description:** Class with `readonly tool: string` (default `"none"`), `readonly metrics: string` (default `"none"`), `readonly tracing: string` (default `"none"`). `fromDict` reads keys: `tool`, `metrics`, `tracing`.
- **Estimated lines:** ~16
- **Tier:** Junior
- **Python reference:** Lines 145-157 of `models.py`

### T3.4 -- Add `TestingConfig` class
- **File:** `src/models.ts`
- **Description:** Class with `readonly smokeTests: boolean` (default `true`), `readonly contractTests: boolean` (default `false`), `readonly performanceTests: boolean` (default `true`), `readonly coverageLine: number` (default `95`), `readonly coverageBranch: number` (default `90`). `fromDict` reads snake_case keys: `smoke_tests`, `contract_tests`, `performance_tests`, `coverage_line`, `coverage_branch`.
- **Estimated lines:** ~22
- **Tier:** Junior
- **Python reference:** Lines 193-209 of `models.py`

### Compilation checkpoint
```
npx tsc --noEmit   # zero errors with all composed models
```
**Expected output:** Clean compilation, zero errors.

---

## G4 -- Complex Models

**Dependencies:** G3 (`InfraConfig` depends on `ObservabilityConfig`; `McpConfig` depends on `McpServerConfig`)

### T4.1 -- Add `InfraConfig` class
- **File:** `src/models.ts`
- **Description:** Class with `readonly container: string` (default `"docker"`), `readonly orchestrator: string` (default `"none"`), `readonly templating: string` (default `"kustomize"`), `readonly iac: string` (default `"none"`), `readonly registry: string` (default `"none"`), `readonly apiGateway: string` (default `"none"`), `readonly serviceMesh: string` (default `"none"`), `readonly observability: ObservabilityConfig` (default `new ObservabilityConfig()`). `fromDict` reads snake_case keys: `container`, `orchestrator`, `templating`, `iac`, `registry`, `api_gateway`, `service_mesh`, `observability` (nested, pass to `ObservabilityConfig.fromDict`).
- **Estimated lines:** ~30
- **Tier:** Mid
- **Python reference:** Lines 164-190 of `models.py`

### T4.2 -- Add `McpServerConfig` class
- **File:** `src/models.ts`
- **Description:** Class with `readonly id: string` (required), `readonly url: string` (required), `readonly capabilities: readonly string[]` (default `[]`), `readonly env: Readonly<Record<string, string>>` (default `{}`). `fromDict` uses `require` for `id` and `url`, reads `capabilities` and `env` with defaults.
- **Estimated lines:** ~20
- **Tier:** Mid
- **Python reference:** Lines 212-226 of `models.py`

### T4.3 -- Add `McpConfig` class
- **File:** `src/models.ts`
- **Description:** Class with `readonly servers: readonly McpServerConfig[]` (default `[]`). `fromDict` reads `data["servers"]` as array, maps each element through `McpServerConfig.fromDict()`.
- **Estimated lines:** ~14
- **Tier:** Mid
- **Python reference:** Lines 229-237 of `models.py`

### Compilation checkpoint
```
npx tsc --noEmit   # zero errors with all complex models
```
**Expected output:** Clean compilation, zero errors.

---

## G5 -- Top-Level + Constructor-Only Models

**Dependencies:** G4 (all config types must exist for `ProjectConfig`)

### T5.1 -- Add `ProjectConfig` class
- **File:** `src/models.ts`
- **Description:** Class with required readonly fields: `project: ProjectIdentity`, `architecture: ArchitectureConfig`, `interfaces: readonly InterfaceConfig[]`, `language: LanguageConfig`, `framework: FrameworkConfig`. Optional-with-defaults readonly fields: `data: DataConfig`, `infrastructure: InfraConfig`, `security: SecurityConfig`, `testing: TestingConfig`, `mcp: McpConfig`. All properties always have a value (no `?:` syntax -- defaults applied in `fromDict`). `fromDict` uses `require` for required top-level sections, maps `interfaces` array through `InterfaceConfig.fromDict()`, delegates optional sections to their respective `fromDict` with `?? {}` fallback.
- **Estimated lines:** ~40
- **Tier:** Senior
- **Python reference:** Lines 244-305 of `models.py`
- **Notes:** This is the root model. `fromDict` reads snake_case keys. Required sections (`project`, `architecture`, `interfaces`, `language`, `framework`) use `require()`. Optional sections (`data`, `infrastructure`, `security`, `testing`, `mcp`) use `data["key"] as Record<string, unknown> | undefined` with `?? {}` fallback passed to sub-model `fromDict`.

### T5.2 -- Add `PipelineResult` class
- **File:** `src/models.ts`
- **Description:** Constructor-only class (no `fromDict`). Fields: `readonly success: boolean`, `readonly outputDir: string`, `readonly filesGenerated: readonly string[]`, `readonly warnings: readonly string[]`, `readonly durationMs: number`. All required in constructor, no defaults.
- **Estimated lines:** ~14
- **Tier:** Junior
- **Python reference:** Lines 308-316 of `models.py`

### T5.3 -- Add `FileDiff` class
- **File:** `src/models.ts`
- **Description:** Constructor-only class (no `fromDict`). Fields: `readonly path: string`, `readonly diff: string`, `readonly pythonSize: number`, `readonly referenceSize: number`. All required in constructor, no defaults.
- **Estimated lines:** ~12
- **Tier:** Junior
- **Python reference:** Lines 319-326 of `models.py`

### T5.4 -- Add `VerificationResult` class
- **File:** `src/models.ts`
- **Description:** Constructor-only class (no `fromDict`). Fields: `readonly success: boolean`, `readonly totalFiles: number`, `readonly mismatches: readonly FileDiff[]`, `readonly missingFiles: readonly string[]`, `readonly extraFiles: readonly string[]`. All required in constructor, no defaults.
- **Estimated lines:** ~14
- **Tier:** Junior
- **Python reference:** Lines 329-337 of `models.py`

### Compilation checkpoint
```
npx tsc --noEmit   # zero errors -- all 17 classes + require helper compile cleanly
```
**Expected output:** Clean compilation, zero errors. All 17 model classes and the `require` helper function are exported.

---

## G6 -- Tests

**Dependencies:** G5 (all models implemented)

### T6.1 -- Unit tests for `require` helper
- **File:** `tests/node/models.test.ts` (new file)
- **Description:** Test that `require` returns the value when key exists in data. Test that `require` throws `Error` with message `"Missing required field 'key' in Model"` when key is missing. Test with various data types (string, number, boolean, object, array). Test that `undefined` value for an existing key still returns (key exists but value is `undefined`).
- **Estimated lines:** ~25
- **Tier:** Junior

### T6.2 -- Unit tests for `TechComponent`
- **File:** `tests/node/models.test.ts`
- **Description:** Test default constructor (`name = "none"`, `version = ""`). Test `fromDict` with complete data. Test `fromDict` with empty object (defaults). Test `fromDict` with partial data (only `name`).
- **Estimated lines:** ~25
- **Tier:** Junior

### T6.3 -- Unit tests for simple models (`ProjectIdentity`, `ArchitectureConfig`, `InterfaceConfig`, `LanguageConfig`, `FrameworkConfig`)
- **File:** `tests/node/models.test.ts`
- **Description:** For each model: test `fromDict` with all fields provided, test `fromDict` with only required fields (verify defaults for optional fields), test `fromDict` with missing required field (verify error thrown with field name and model name in message). Verify snake_case keys are read correctly and mapped to camelCase properties.
- **Estimated lines:** ~100
- **Tier:** Mid

### T6.4 -- Unit tests for composed models (`DataConfig`, `SecurityConfig`, `ObservabilityConfig`, `TestingConfig`)
- **File:** `tests/node/models.test.ts`
- **Description:** `DataConfig`: test `fromDict` with complete nested data (verify `TechComponent` instances created), test with empty object (all defaults). `SecurityConfig`: test with frameworks array, test with empty object (default `[]`). `ObservabilityConfig`: test with all fields, test with empty object (all `"none"`). `TestingConfig`: test with all fields, test with empty object (verify defaults: `smokeTests=true`, `contractTests=false`, `performanceTests=true`, `coverageLine=95`, `coverageBranch=90`).
- **Estimated lines:** ~80
- **Tier:** Mid

### T6.5 -- Unit tests for complex models (`InfraConfig`, `McpServerConfig`, `McpConfig`)
- **File:** `tests/node/models.test.ts`
- **Description:** `InfraConfig`: test with complete data including nested `observability`, test with empty object (verify all defaults including `ObservabilityConfig` defaults). `McpServerConfig`: test with all fields, test with only required fields (`id`, `url`), test missing `id` throws, test missing `url` throws. `McpConfig`: test with servers array (verify `McpServerConfig` instances), test with empty object (default `[]`).
- **Estimated lines:** ~80
- **Tier:** Mid

### T6.6 -- Unit tests for `ProjectConfig`
- **File:** `tests/node/models.test.ts`
- **Description:** Test `fromDict` with complete data (all required + optional sections). Verify all nested types are correct instances. Test with only required sections (verify optional sections receive defaults: `DataConfig` with `TechComponent` defaults, `InfraConfig` with `ObservabilityConfig` defaults, etc.). Test missing `project` throws. Test missing `architecture` throws. Test missing `interfaces` throws. Test missing `language` throws. Test missing `framework` throws. Test `interfaces` is correctly mapped as array of `InterfaceConfig`.
- **Estimated lines:** ~90
- **Tier:** Senior

### T6.7 -- Unit tests for constructor-only models (`PipelineResult`, `FileDiff`, `VerificationResult`)
- **File:** `tests/node/models.test.ts`
- **Description:** Test construction with all fields, verify `readonly` properties hold correct values. `VerificationResult`: verify `mismatches` holds `FileDiff` instances. Verify no `fromDict` method exists on these classes.
- **Estimated lines:** ~50
- **Tier:** Junior

### Test + Coverage checkpoint
```
npx tsc --noEmit
npm run test -- tests/node/models.test.ts   # all green
npm run test:coverage   # line >= 95%, branch >= 90% for src/models.ts
```
**Expected output:** All tests pass. Coverage thresholds met for `src/models.ts`.

---

## Dependency Graph

```
G1 (Foundation: require + TechComponent)
 └──> G2 (Simple Models: ProjectIdentity, ArchitectureConfig, InterfaceConfig, LanguageConfig, FrameworkConfig)
       └──> G3 (Composed Models: DataConfig, SecurityConfig, ObservabilityConfig, TestingConfig)
             └──> G4 (Complex Models: InfraConfig, McpServerConfig, McpConfig)
                   └──> G5 (Top-Level + Constructor-Only: ProjectConfig, PipelineResult, FileDiff, VerificationResult)
                         └──> G6 (Tests: all unit tests)
```

**Strictly sequential:** Each group depends on the previous for compilation. No parallelization between groups.

## Critical Path

```
G1 --> G2 --> G3 --> G4 --> G5 --> G6
```

---

## File Summary

| File | Group(s) | Action |
|------|----------|--------|
| `src/models.ts` | G1, G2, G3, G4, G5 | Modified (add `require` helper + 17 model classes below existing `ProjectFoundation` and `DEFAULT_FOUNDATION`) |
| `tests/node/models.test.ts` | G6 | Created (new test file) |

---

## File Size Strategy

The Python `models.py` is 337 lines. The TypeScript equivalent is estimated at 400-500 lines due to explicit constructors, type annotations, and `readonly` modifiers.

**If `src/models.ts` exceeds 250 lines (Rule 03 limit):**
1. Split into `src/models/config-models.ts` (config-related: TechComponent through McpConfig)
2. Split into `src/models/pipeline-models.ts` (PipelineResult, FileDiff, VerificationResult)
3. Create `src/models/index.ts` re-exporting all + `require` helper + existing `ProjectFoundation`
4. Move existing `ProjectFoundation` and `DEFAULT_FOUNDATION` into the appropriate sub-module
5. Evaluate after G5 compilation checkpoint -- if under ~300 lines with compact style, keep as single file

---

## Key TypeScript Patterns

### `fromDict` pattern (with `noUncheckedIndexedAccess`)
```typescript
// Required field: use require() helper
require(data, "field_name", "ClassName") as string

// Optional field with default: cast + nullish coalescing
(data["field_name"] as boolean | undefined) ?? false

// Nested object: cast to Record, pass to child fromDict
const nested = (data["child"] as Record<string, unknown> | undefined) ?? {};
ChildClass.fromDict(nested)

// Nested array: cast to array, map through child fromDict
const items = (data["items"] as Record<string, unknown>[] | undefined) ?? [];
items.map((item) => ChildClass.fromDict(item))
```

### `require` helper (with `noUncheckedIndexedAccess`)
```typescript
export function require(
  data: Record<string, unknown>,
  key: string,
  model: string,
): unknown {
  if (!(key in data)) {
    throw new Error(`Missing required field '${key}' in ${model}`);
  }
  return data[key];
}
```

**Note:** `key in data` is the correct guard for `noUncheckedIndexedAccess` -- it narrows the type so `data[key]` is `unknown` (not `unknown | undefined`).

---

## Totals

| Group | Tasks | Estimated Lines (src) | Estimated Lines (test) |
|-------|-------|-----------------------|------------------------|
| G1 -- Foundation | 2 | ~23 | -- |
| G2 -- Simple Models | 5 | ~78 | -- |
| G3 -- Composed Models | 4 | ~68 | -- |
| G4 -- Complex Models | 3 | ~64 | -- |
| G5 -- Top-Level + Constructor-Only | 4 | ~80 | -- |
| G6 -- Tests | 7 | -- | ~450 |
| **Total** | **25 tasks** | **~313 lines** | **~450 lines** |

- **New files:** 1 (`tests/node/models.test.ts`)
- **Modified files:** 1 (`src/models.ts`)
- **Source classes:** 17 model classes + 1 helper function
- **Test file:** 7 test groups covering all models, defaults, and error cases
