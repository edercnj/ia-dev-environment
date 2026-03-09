# Implementation Plan -- STORY-003: Models (Python to TypeScript Migration)

**Status:** PLANNED
**Date:** 2026-03-09
**Blocked By:** STORY-001 (project foundation)
**Blocks:** STORY-004 (config loader), STORY-006, STORY-007, STORY-008, STORY-017

---

## 1. Affected Layers and Components

This is a **library project** -- no database, no API, no events. All changes are in the shared models module.

| Layer | Component | Impact |
|-------|-----------|--------|
| Shared / Models | `src/models.ts` | Add 17 model classes with `fromDict` factories + `require` helper, preserving existing `ProjectFoundation` |
| Tests | `tests/node/models.test.ts` | New test file covering all `fromDict` methods, defaults, and error cases |

No other source files are modified. No assemblers, CLI, domain modules, exceptions, or utils are touched.

---

## 2. New Classes/Interfaces to Create

All new types go in `src/models.ts`, below the existing `ProjectFoundation` interface and `DEFAULT_FOUNDATION` constant.

### 2.1 Helper Function

| Function | Signature | Responsibility |
|----------|-----------|----------------|
| `require` | `(data: Record<string, unknown>, key: string, model: string): unknown` | Extracts a required field from `data` or throws `Error` with message `"Missing required field '{key}' in {model}"` |

**Note:** The story document says message format `"Missing required field '{key}' in {model}"` (section 3.4). The Python source uses `"Missing required field '{key}' for {model}"`. The implementation will use `"Missing required field '{key}' in {model}"` per the story spec since this is the TypeScript contract.

### 2.2 Model Classes

Each Python dataclass maps to a TypeScript class with:
- `readonly` properties for all fields
- Constructor with required params first, optional params with defaults
- `static fromDict(data: Record<string, unknown>): ClassName` factory method

| # | Class | Required Fields | Optional Fields (with defaults) | Nested Types |
|---|-------|----------------|--------------------------------|-------------|
| 1 | `TechComponent` | (none) | `name = "none"`, `version = ""` | -- |
| 2 | `ProjectIdentity` | `name`, `purpose` | (none) | -- |
| 3 | `ArchitectureConfig` | `style` | `domainDriven = false`, `eventDriven = false` | -- |
| 4 | `InterfaceConfig` | `type` | `spec = ""`, `broker = ""` | -- |
| 5 | `LanguageConfig` | `name`, `version` | (none) | -- |
| 6 | `FrameworkConfig` | `name`, `version` | `buildTool = "pip"`, `nativeBuild = false` | -- |
| 7 | `DataConfig` | (none) | `database`, `migration`, `cache` (all default `TechComponent`) | `TechComponent` |
| 8 | `SecurityConfig` | (none) | `frameworks = []` | -- |
| 9 | `ObservabilityConfig` | (none) | `tool = "none"`, `metrics = "none"`, `tracing = "none"` | -- |
| 10 | `InfraConfig` | (none) | `container = "docker"`, `orchestrator = "none"`, `templating = "kustomize"`, `iac = "none"`, `registry = "none"`, `apiGateway = "none"`, `serviceMesh = "none"`, `observability` | `ObservabilityConfig` |
| 11 | `TestingConfig` | (none) | `smokeTests = true`, `contractTests = false`, `performanceTests = true`, `coverageLine = 95`, `coverageBranch = 90` | -- |
| 12 | `McpServerConfig` | `id`, `url` | `capabilities = []`, `env = {}` | -- |
| 13 | `McpConfig` | (none) | `servers = []` | `McpServerConfig` |
| 14 | `ProjectConfig` | `project`, `architecture`, `interfaces`, `language`, `framework` | `data`, `infrastructure`, `security`, `testing`, `mcp` | All config types |
| 15 | `PipelineResult` | `success`, `outputDir`, `filesGenerated`, `warnings`, `durationMs` | (none) | -- |
| 16 | `FileDiff` | `path`, `diff`, `pythonSize`, `referenceSize` | (none) | -- |
| 17 | `VerificationResult` | `success`, `totalFiles`, `mismatches`, `missingFiles`, `extraFiles` | (none) | `FileDiff` |

### 2.3 Naming Convention: Python snake_case to TypeScript camelCase

| Python | TypeScript |
|--------|-----------|
| `domain_driven` | `domainDriven` |
| `event_driven` | `eventDriven` |
| `build_tool` | `buildTool` |
| `native_build` | `nativeBuild` |
| `smoke_tests` | `smokeTests` |
| `contract_tests` | `contractTests` |
| `performance_tests` | `performanceTests` |
| `coverage_line` | `coverageLine` |
| `coverage_branch` | `coverageBranch` |
| `api_gateway` | `apiGateway` |
| `service_mesh` | `serviceMesh` |
| `output_dir` | `outputDir` |
| `files_generated` | `filesGenerated` |
| `duration_ms` | `durationMs` |
| `python_size` | `pythonSize` |
| `reference_size` | `referenceSize` |
| `total_files` | `totalFiles` |
| `missing_files` | `missingFiles` |
| `extra_files` | `extraFiles` |

**Important:** The `fromDict` methods must read from the **snake_case** keys (matching the Python YAML/dict format) and map to camelCase TypeScript properties. This preserves YAML config compatibility (RULE-001: output compatibility).

### 2.4 TypeScript Type Mapping

| Python Type | TypeScript Type | Notes |
|------------|----------------|-------|
| `str` | `string` | |
| `bool` | `boolean` | |
| `int` | `number` | |
| `List[str]` | `readonly string[]` | Immutable by default |
| `List[X]` | `readonly X[]` | Immutable by default |
| `Dict[str, str]` | `Readonly<Record<string, string>>` | Immutable by default |
| `Optional[X]` | `X \| undefined` | NOT `?:` due to `exactOptionalPropertyTypes` |
| `Path` | `string` | For `PipelineResult`, `FileDiff`, `VerificationResult` |

### 2.5 Design Decisions

**Classes over interfaces for models with `fromDict`:** The story specifies `class` + `interface` pattern (interface for shape, class for `fromDict`). However, for simplicity and cohesion, each model will be a single class with `readonly` properties and a static `fromDict`. Interfaces are unnecessary since the classes themselves define the shape, and consumers can use `InstanceType<typeof ClassName>` or the class directly as a type.

**`PipelineResult`, `FileDiff`, `VerificationResult` have no `fromDict`:** The Python source defines these as plain dataclasses without `from_dict` classmethods. They are constructed programmatically, not from YAML/dict input. They will be simple classes with constructors only.

**`readonly` properties:** All class properties are `readonly` to match Python dataclass immutability semantics and TypeScript best practices.

**`noUncheckedIndexedAccess` handling:** When accessing `data[key]` on `Record<string, unknown>`, the result is `unknown | undefined`. The `require` helper must check for `undefined` explicitly (using `key in data` check or `=== undefined` after access). For optional fields, `data.get()` equivalent is direct index access with nullish coalescing: `(data["key"] as T | undefined) ?? defaultValue`.

**`exactOptionalPropertyTypes` handling:** No optional properties with `?:` will be used on model classes. All properties are required in the constructor. For `ProjectConfig`, the optional config sections (`data`, `infrastructure`, `security`, `testing`, `mcp`) receive defaults in `fromDict` -- they are NOT optional on the class, they always have a value.

### 2.6 New Test File

| File | Coverage Target |
|------|----------------|
| `tests/node/models.test.ts` | All 14 `fromDict` methods (complete data, missing optionals, missing required fields), `require` helper, nested type construction, default values |

---

## 3. Existing Classes to Modify

| File | Change |
|------|--------|
| `src/models.ts` | Add `require` function and 17 model classes below existing `ProjectFoundation` interface and `DEFAULT_FOUNDATION` constant. No changes to existing code. |

No other files require modification.

---

## 4. Dependency Direction Validation

```
src/models.ts
    imports: nothing (pure data classes, zero external dependencies)
```

**Assessment: COMPLIANT.**

- `models.ts` has zero dependencies -- pure data model classes with no imports.
- The `require` helper uses only standard `Error`. No framework imports.
- The module is a "leaf" dependency -- imported by others, imports nothing from the project.
- No circular dependencies. No upward dependency violations.

Dependency graph after STORY-003:

```
src/models.ts ──> (nothing)

(future consumers)
src/config.ts ──> src/models.ts    (STORY-004: ProjectConfig.fromDict consumer)
src/assembler/ ──> src/models.ts   (future: PipelineResult consumer)
```

---

## 5. Integration Points

| Integration Point | Consumer | When |
|-------------------|----------|------|
| `ProjectConfig.fromDict` | `src/config.ts` (config loader) | STORY-004 |
| `PipelineResult` | Pipeline orchestrator | STORY-016/STORY-017 |
| `VerificationResult`, `FileDiff` | Verification module | STORY-017 |
| All config models | Template engine, assemblers | STORY-006, STORY-007, STORY-008 |

All integration points are **downstream** -- this story provides data structures consumed by later stories.

---

## 6. Database Changes

N/A -- This is a CLI library project with no database.

---

## 7. API Changes

N/A -- No HTTP/REST/gRPC interfaces.

---

## 8. Event Changes

N/A -- No event-driven architecture.

---

## 9. Configuration Changes

N/A -- No runtime configuration changes. The models themselves represent the configuration schema that will be parsed from YAML in STORY-004.

---

## 10. Risk Assessment

| Risk | Severity | Likelihood | Mitigation |
|------|----------|------------|------------|
| **`exactOptionalPropertyTypes` violations** | High | High | Never use `?:` syntax on class properties. All properties are required with defaults applied in `fromDict`. Test that `tsc --noEmit` passes after every class addition. |
| **`noUncheckedIndexedAccess` in `require` and `fromDict`** | High | High | `Record<string, unknown>` index returns `unknown \| undefined`. Use `key in data` guard in `require`. For optional fields, cast with `as T \| undefined` then apply `??` default. Verify each `fromDict` compiles cleanly. |
| **Snake_case key mismatch in `fromDict`** | Medium | Medium | Python dicts use `snake_case` keys (from YAML). `fromDict` must read `data["domain_driven"]` not `data["domainDriven"]`. Add test cases with snake_case input to catch any mismatch. |
| **Mutable default array/object sharing** | Medium | Low | Python uses `field(default_factory=list)` to avoid shared mutable defaults. TypeScript class constructors create new arrays/objects per instance by default (`= []` in constructor). No risk if defaults are always fresh literals in `fromDict`, never shared references. |
| **File length exceeding 250 lines** | Medium | High | 17 classes + helper in one file will likely exceed 250 lines. The coding standard (Rule 03) limits class/module to 250 lines. **Mitigation:** The story explicitly specifies a single `src/models.ts` file. If the file exceeds 250 lines, split into logical groups (e.g., `src/models/config.ts`, `src/models/pipeline.ts`, `src/models/index.ts` re-export). Alternatively, request a waiver since models are pure data with minimal logic. |
| **`fromDict` type safety with `unknown`** | Medium | Medium | Input `data` is `Record<string, unknown>`. Every field access requires a cast. Use consistent pattern: `require()` returns `unknown`, caller casts with `as string`, `as boolean`, etc. For nested objects, cast to `Record<string, unknown>` before passing to child `fromDict`. Document the cast pattern. |
| **Test coverage for all 17 classes** | Low | Low | Systematic test generation: for each class, test (1) full data, (2) defaults only, (3) missing required field. Parametrize where possible. |
| **Python `_require` raises `KeyError` vs TypeScript `Error`** | Low | Low | Story explicitly says use `Error` with message format. No behavioral difference for callers -- both are catchable exceptions. |

---

## 11. Implementation Order

1. **`require` helper function** in `src/models.ts` -- no dependencies, needed by all `fromDict` methods
2. **Leaf models (no nested types):** `TechComponent`, `ProjectIdentity`, `ArchitectureConfig`, `InterfaceConfig`, `LanguageConfig`, `FrameworkConfig`, `SecurityConfig`, `ObservabilityConfig`, `TestingConfig`, `McpServerConfig`
3. **Composite models (depend on leaf models):** `DataConfig`, `InfraConfig`, `McpConfig`
4. **Root model:** `ProjectConfig` (depends on all config models)
5. **Pipeline models (standalone, no `fromDict`):** `PipelineResult`, `FileDiff`, `VerificationResult`
6. **Tests:** `tests/node/models.test.ts` -- all `fromDict` methods, defaults, error cases
7. **Verify:** `npx tsc --noEmit`, `npm run test:coverage`, thresholds pass

Inner-to-outer: leaf data types first, composites second, root last. Pipeline models are independent and can be added at any point.

---

## 12. File Size Strategy

The Python `models.py` is 337 lines. The TypeScript equivalent will be larger due to:
- Explicit constructor definitions (Python dataclasses auto-generate)
- Type annotations on all `fromDict` parameters and return types
- `readonly` modifiers on all properties

**Estimated size:** 400-500 lines for all 17 classes + helper.

**Strategy if exceeding 250-line limit:**
- Split into `src/models/config-models.ts` (config-related: items 1-13), `src/models/pipeline-models.ts` (pipeline-related: items 15-17), and `src/models/index.ts` (re-exports all + `require` helper + `ProjectFoundation`).
- Evaluate after initial implementation. If the file stays under 300 lines with compact class style, keep as single file.

---

## 13. `fromDict` Pattern Template

Each `fromDict` follows this consistent pattern:

```typescript
class ExampleConfig {
  readonly requiredField: string;
  readonly optionalField: boolean;

  constructor(requiredField: string, optionalField: boolean = false) {
    this.requiredField = requiredField;
    this.optionalField = optionalField;
  }

  static fromDict(data: Record<string, unknown>): ExampleConfig {
    return new ExampleConfig(
      require(data, "required_field", "ExampleConfig") as string,
      (data["optional_field"] as boolean | undefined) ?? false,
    );
  }
}
```

Key patterns:
- `require()` for mandatory fields, cast result with `as T`
- Direct index + `as T | undefined` + `?? default` for optional fields
- Snake_case keys in `data[]` access (matching YAML input)
- CamelCase property names on the class
- Nested objects: cast to `Record<string, unknown>` before passing to child `fromDict`
