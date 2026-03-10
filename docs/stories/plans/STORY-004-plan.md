# Implementation Plan: STORY-004 -- Config Loader + Migration v2-v3

**Story:** STORY-004 -- Config Loader + Migration v2-v3
**Branch:** feat/STORY-004-config-loader (based on feat/STORY-003-models)
**Blocked By:** STORY-002 (exceptions), STORY-003 (models)
**Blocks:** STORY-017 (interactive mode), STORY-018 (CLI entry point)
**Stack:** TypeScript 5 / Commander / js-yaml

---

## 1. Affected Components

| File | Change | Rationale |
|------|--------|-----------|
| `src/config.ts` | ADD functions + constants | Config loading, v2 detection, migration, validation |
| `src/config.ts` | PRESERVE existing code | `RuntimePaths` interface and `createRuntimePaths` remain untouched |
| `tests/config.test.ts` | NEW | Unit + contract tests for all config functions |
| `tests/fixtures/*.yaml` | NEW | YAML fixtures for v2 and v3 formats |

No other source files are modified.

---

## 2. Constants to Add (in `src/config.ts`)

### REQUIRED_SECTIONS

```typescript
const REQUIRED_SECTIONS = [
  "project", "architecture", "interfaces", "language", "framework",
] as const;
```

### TYPE_MAPPING

Maps v2 `type` field to architecture style + interfaces. Must match Python exactly.

| v2 type | architecture.style | interfaces |
|---------|-------------------|------------|
| `api` | `microservice` | `[{ type: "rest" }]` |
| `cli` | `library` | `[{ type: "cli" }]` |
| `library` | `library` | `[]` |
| `worker` | `microservice` | `[{ type: "event-consumer" }]` |
| `fullstack` | `monolith` | `[{ type: "rest" }]` |

Default (unknown type): `microservice` + `[{ type: "rest" }]`

### STACK_MAPPING

Maps v2 `stack` field to language + framework. Must match Python exactly.

| v2 stack | language | version | framework | fw version |
|----------|----------|---------|-----------|------------|
| `java-quarkus` | java | 21 | quarkus | 3.17 |
| `java-spring` | java | 21 | spring-boot | 3.4 |
| `python-fastapi` | python | 3.12 | fastapi | 0.115 |
| `python-click-cli` | python | 3.9 | click | 8.1 |
| `go-gin` | go | 1.23 | gin | 1.10 |
| `kotlin-ktor` | kotlin | 2.1 | ktor | 3.0 |
| `typescript-nestjs` | typescript | 5.7 | nestjs | 10.4 |
| `rust-axum` | rust | 1.83 | axum | 0.8 |

---

## 3. Functions to Implement

### 3.1 `detectV2Format(data: Record<string, unknown>): boolean`

- Returns `true` if `data.type` is a key in TYPE_MAPPING OR `data.stack` is a key in STACK_MAPPING.
- Pure function, no side effects.

### 3.2 `migrateV2ToV3(data: Record<string, unknown>): Record<string, unknown>`

- Delegates to two private helpers:
  - `buildArchitectureSection(data)` -- looks up `data.type` in TYPE_MAPPING, returns `{ architecture, interfaces }`.
  - `buildLanguageFramework(data)` -- looks up `data.stack` in STACK_MAPPING, throws `ConfigValidationError` if unknown.
- Preserves `data.project` if present, defaults to `{ name: "unnamed", purpose: "" }`.
- Emits `console.warn("Config uses legacy v2 format. Auto-migrating to v3.")`.
- Returns new object (never mutates input).

### 3.3 `validateConfig(data: Record<string, unknown> | null): void`

- If `data` is null/undefined, throws `ConfigValidationError` with all REQUIRED_SECTIONS.
- Collects all missing sections, throws single `ConfigValidationError` listing them all.

### 3.4 `loadConfig(path: string): ProjectConfig`

- Reads file via `fs.readFileSync(path, "utf-8")`.
- Parses with `yaml.load(content)` (js-yaml).
- Guards `null` result (empty file) with `ConfigValidationError`.
- Calls `detectV2Format` -> `migrateV2ToV3` if v2.
- Calls `validateConfig`.
- Returns `ProjectConfig.fromDict(data)`.

### 3.5 Private helpers

- `buildArchitectureSection(data)`: Returns `{ style, interfaces[] }` from TYPE_MAPPING lookup.
- `buildLanguageFramework(data)`: Returns `{ language, framework }` dicts from STACK_MAPPING lookup.

---

## 4. Imports and Dependencies

```typescript
// Node built-ins
import { readFileSync } from "node:fs";

// Third-party
import yaml from "js-yaml";

// Internal
import { ConfigValidationError } from "./exceptions.js";
import { ProjectConfig } from "./models.js";
```

Dependency direction: `config.ts` imports from `exceptions.ts` and `models.ts` (same layer, library architecture). No circular dependencies.

---

## 5. Exports

All public functions and constants are exported:

- `loadConfig`, `detectV2Format`, `migrateV2ToV3`, `validateConfig`
- `REQUIRED_SECTIONS`, `TYPE_MAPPING`, `STACK_MAPPING`
- Existing exports preserved: `RuntimePaths`, `createRuntimePaths`

Private helpers (`buildArchitectureSection`, `buildLanguageFramework`) are NOT exported.

---

## 6. Test Plan

### 6.1 Fixtures (`tests/fixtures/`)

| File | Purpose |
|------|---------|
| `valid-v3-config.yaml` | Complete v3 config with all required sections |
| `valid-v2-config.yaml` | v2 config with `type: api` and `stack: java-spring` |
| `missing-section-config.yaml` | v3 config missing `language` section |
| `empty-config.yaml` | Empty file (yields null from YAML parser) |
| `minimal-v3-config.yaml` | Only required sections, no optional fields |

### 6.2 Unit Tests (`tests/config.test.ts`)

| Test | Validates |
|------|-----------|
| `loadConfig_validV3_returnsProjectConfig` | Happy path end-to-end |
| `loadConfig_v2Format_migratesAndReturns` | v2 detection + migration + result |
| `loadConfig_emptyFile_throwsConfigValidationError` | Null guard |
| `loadConfig_missingSection_throwsConfigValidationError` | Validation |
| `detectV2Format_withTypeKey_returnsTrue` | Type detection |
| `detectV2Format_withStackKey_returnsTrue` | Stack detection |
| `detectV2Format_v3Data_returnsFalse` | Negative case |
| `migrateV2ToV3_preservesProjectSection` | Project passthrough |
| `migrateV2ToV3_unknownStack_throwsError` | Error path |
| `migrateV2ToV3_emitsWarning` | console.warn called |
| `validateConfig_allPresent_noThrow` | Happy path |
| `validateConfig_null_throwsAllSections` | Null input |
| `validateConfig_multipleMissing_listsAll` | Collects all missing |

### 6.3 Contract Tests (parametrized)

| Test | Parameters |
|------|------------|
| `migrateV2ToV3_typeMappingParity` | Each TYPE_MAPPING entry (5 cases) |
| `migrateV2ToV3_stackMappingParity` | Each STACK_MAPPING entry (8 cases) |

---

## 7. Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| TYPE_MAPPING / STACK_MAPPING values diverge from Python | High | Contract tests validate every entry. Values copied verbatim from `config.py`. |
| `js-yaml` API differs from PyYAML `safe_load` | Medium | Use `yaml.load(content)` with default schema (equivalent to safe_load). Test with same fixtures. |
| Empty YAML returns `undefined` not `null` in js-yaml | Medium | Guard with `data == null` (covers both null and undefined). |
| File not found at path | Low | Let `readFileSync` throw native `ENOENT` error. Caller handles. |
| `console.warn` not testable | Low | Use `vi.spyOn(console, "warn")` in vitest to assert warning message. |

---

## 8. Implementation Order

1. Add constants: `REQUIRED_SECTIONS`, `TYPE_MAPPING`, `STACK_MAPPING`
2. Implement `detectV2Format`
3. Implement private helpers: `buildArchitectureSection`, `buildLanguageFramework`
4. Implement `migrateV2ToV3`
5. Implement `validateConfig`
6. Implement `loadConfig`
7. Create YAML test fixtures
8. Write unit + contract tests
9. Run `npx tsc --noEmit` and `npx vitest run --coverage` to validate

---

## 9. File Layout (after STORY-004)

```
src/
    config.ts              # MODIFIED: add config loader functions + constants
    exceptions.ts          # (unchanged from STORY-002)
    models.ts              # (unchanged from STORY-003)
tests/
    config.test.ts         # NEW: unit + contract tests
    fixtures/
        valid-v3-config.yaml
        valid-v2-config.yaml
        missing-section-config.yaml
        empty-config.yaml
        minimal-v3-config.yaml
```
