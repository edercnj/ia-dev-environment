# Implementation Plan -- STORY-005: Template Engine (Jinja2 to Nunjucks Migration)

**Status:** DRAFT
**Date:** 2026-03-10
**Story:** STORY-005
**Blocked by:** STORY-001 (models)
**Blocks:** STORY-008, STORY-014, STORY-015

---

## 1. Affected Layers and Components

| Layer | Component | Action |
|-------|-----------|--------|
| Source | `src/template-engine.ts` | **Rewrite** — replace stub with full TemplateEngine class |
| Tests | `tests/node/template-engine.test.ts` | **Create** — unit tests mirroring Python `test_template_engine.py` |
| Tests | `tests/fixtures/templates/*.j2` | **Reuse** — existing Jinja2/Nunjucks fixtures are compatible |
| Tests | `tests/fixtures/concat_a.txt`, `concat_b.txt` | **Reuse** — existing concat fixtures |
| Models | `src/models.ts` | **No change** — ProjectConfig already has all required fields |
| Config | `src/config.ts` | **No change** — RuntimePaths.resourcesDir already resolved |
| Exceptions | `src/exceptions.ts` | **No change** — no new exception types needed (Nunjucks throws its own errors) |

This is a **library-style** project (no hexagonal layers). The TemplateEngine is a utility class consumed by assemblers and the pipeline. It sits alongside `config.ts` and `models.ts` as a core module.

---

## 2. New Classes/Interfaces to Create

### 2.1 `TemplateEngine` class (rewrite of existing stub)

**File:** `src/template-engine.ts`

```typescript
// Public API surface:
export const PLACEHOLDER_PATTERN: RegExp;

export function buildDefaultContext(config: ProjectConfig): Record<string, unknown>;

export class TemplateEngine {
  constructor(resourcesDir: string, config: ProjectConfig);

  renderTemplate(templatePath: string, context?: Record<string, unknown>): string;
  renderString(templateStr: string, context?: Record<string, unknown>): string;
  replacePlaceholders(content: string, config?: ProjectConfig): string;

  static injectSection(baseContent: string, section: string, marker: string): string;
  static concatFiles(paths: string[], separator?: string): string;
}
```

### 2.2 Test file

**File:** `tests/node/template-engine.test.ts`

No new interfaces are required. The class depends only on:
- `nunjucks` (already in `package.json` dependencies)
- `ProjectConfig` from `src/models.ts`
- `node:fs` for `concatFiles`

---

## 3. Existing Classes to Modify

| File | Modification |
|------|-------------|
| `src/template-engine.ts` | Full rewrite of the 7-line stub into the complete TemplateEngine class |

No other files need modification. The existing stub exports `TemplateEngine` so the import path is already correct for any downstream consumers.

---

## 4. Dependency Direction Validation

```
src/template-engine.ts
  ├── depends on: nunjucks (npm package)
  ├── depends on: src/models.ts (ProjectConfig)
  └── depends on: node:fs (readFileSync for concatFiles)

tests/node/template-engine.test.ts
  ├── depends on: src/template-engine.ts
  ├── depends on: src/models.ts
  └── depends on: tests/fixtures/
```

**Validation:** No circular dependencies. TemplateEngine depends only on models (same layer) and a third-party library. Models does not import TemplateEngine. Direction is correct.

---

## 5. Integration Points

| Consumer | How it uses TemplateEngine |
|----------|---------------------------|
| Assemblers (`src/assembler/`) | Will call `renderTemplate()` and `replacePlaceholders()` to generate output files |
| Pipeline (future STORY-008) | Will instantiate TemplateEngine with `RuntimePaths.resourcesDir` and `ProjectConfig` |
| STORY-014, STORY-015 | Depend on TemplateEngine being available |

**Constructor contract:**
```typescript
const engine = new TemplateEngine(runtimePaths.resourcesDir, config);
```

This aligns with `createRuntimePaths()` from `src/config.ts` which already resolves `resourcesDir`.

---

## 6. Database Changes

None. This story has no persistence requirements.

---

## 7. API Changes

None. This is an internal library module, not an HTTP/CLI endpoint change.

---

## 8. Event Changes

None. The project is not event-driven.

---

## 9. Configuration Changes

No new configuration. The Nunjucks environment is configured programmatically inside the constructor:

| Setting | Value | Rationale |
|---------|-------|-----------|
| `autoescape` | `false` | Templates produce Markdown/YAML, not HTML |
| `trimBlocks` | `false` | Preserve whitespace fidelity with Jinja2 output |
| `lstripBlocks` | `false` | Preserve leading whitespace in block tags |
| `throwOnUndefined` | `true` | Equivalent to Jinja2 `StrictUndefined` |

**Note on `keepTrailingNewline`:** Nunjucks does not have a direct equivalent of Jinja2's `keep_trailing_newline=True`. By default Nunjucks preserves trailing newlines in templates. This must be verified in tests (acceptance criterion in story). If Nunjucks strips trailing newlines, a post-processing step will be needed.

---

## 10. Risk Assessment

### 10.1 Risks

| # | Risk | Severity | Mitigation |
|---|------|----------|------------|
| R1 | Nunjucks `throwOnUndefined` behavior differs from Jinja2 `StrictUndefined` | Medium | Test with undefined variables in both `renderTemplate` and `renderString`; verify error type |
| R2 | Trailing newline behavior mismatch | Medium | Dedicated test case comparing output with Python reference fixtures in `tests/fixtures/reference/` |
| R3 | Nunjucks regex/filter differences from Jinja2 | Low | Current templates use only variable interpolation (`{{ var }}`), no filters or complex expressions |
| R4 | `concatFiles` uses synchronous `readFileSync` | Low | Acceptable for CLI tool; matches Python's `Path.read_text()` behavior |
| R5 | Placeholder regex `\{(\w+)\}` may match JSON braces | Low | Python tests already cover this edge case (`{"key": "value"}` should be unchanged because `\w+` won't match `"key": "value"`). Port the same test. |

### 10.2 Nunjucks vs Jinja2 Behavioral Differences to Verify

| Behavior | Jinja2 | Nunjucks | Action |
|----------|--------|----------|--------|
| Undefined variable | Raises `UndefinedError` | Throws error with `throwOnUndefined: true` | Test error type |
| Template not found | Raises `TemplateNotFound` | Throws `Error` with message | Test error message content |
| Trailing newline | `keep_trailing_newline=True` preserves | Preserved by default | Verify in whitespace test |
| Autoescape off | `autoescape=False` | `autoescape: false` | Direct mapping |
| Sandboxed environment | `SandboxedEnvironment` | No direct equivalent | Accept: Nunjucks has no sandbox mode; templates are trusted (generated by the tool itself) |

---

## 11. Implementation Order

Following inner-to-outer principle:

| Step | Task | Estimated LOC |
|------|------|---------------|
| 1 | Implement `PLACEHOLDER_PATTERN` constant | 1 |
| 2 | Implement `buildDefaultContext()` function (24 fields from ProjectConfig) | 30 |
| 3 | Implement `TemplateEngine` constructor (Nunjucks Environment setup) | 15 |
| 4 | Implement private `mergeContext()` method | 8 |
| 5 | Implement `renderTemplate()` | 5 |
| 6 | Implement `renderString()` | 5 |
| 7 | Implement `replacePlaceholders()` | 15 |
| 8 | Implement `static injectSection()` | 3 |
| 9 | Implement `static concatFiles()` | 10 |
| 10 | Write unit tests (all groups from Python test file) | ~250 |
| 11 | Run coverage, fix gaps | Variable |

**Estimated total:** ~100 LOC production code, ~250 LOC test code.

---

## 12. Context Field Mapping (25 fields)

The story specifies 25 fields but the Python source has 24. The discrepancy needs resolution. The Python `_build_default_context` produces exactly 24 fields:

| # | Field | Source Path | Type |
|---|-------|-------------|------|
| 1 | `project_name` | `config.project.name` | string |
| 2 | `project_purpose` | `config.project.purpose` | string |
| 3 | `language_name` | `config.language.name` | string |
| 4 | `language_version` | `config.language.version` | string |
| 5 | `framework_name` | `config.framework.name` | string |
| 6 | `framework_version` | `config.framework.version` | string |
| 7 | `build_tool` | `config.framework.buildTool` | string |
| 8 | `architecture_style` | `config.architecture.style` | string |
| 9 | `domain_driven` | `config.architecture.domainDriven` | boolean |
| 10 | `event_driven` | `config.architecture.eventDriven` | boolean |
| 11 | `container` | `config.infrastructure.container` | string |
| 12 | `orchestrator` | `config.infrastructure.orchestrator` | string |
| 13 | `templating` | `config.infrastructure.templating` | string |
| 14 | `iac` | `config.infrastructure.iac` | string |
| 15 | `registry` | `config.infrastructure.registry` | string |
| 16 | `api_gateway` | `config.infrastructure.apiGateway` | string |
| 17 | `service_mesh` | `config.infrastructure.serviceMesh` | string |
| 18 | `database_name` | `config.data.database.name` | string |
| 19 | `cache_name` | `config.data.cache.name` | string |
| 20 | `smoke_tests` | `config.testing.smokeTests` | boolean |
| 21 | `contract_tests` | `config.testing.contractTests` | boolean |
| 22 | `performance_tests` | `config.testing.performanceTests` | boolean |
| 23 | `coverage_line` | `config.testing.coverageLine` | number |
| 24 | `coverage_branch` | `config.testing.coverageBranch` | number |

**Decision:** Implement 24 fields matching the Python source exactly. The story's "25 fields" claim appears to be a documentation error. If a 25th field is identified later, it can be added without breaking changes.

---

## 13. Test Strategy

**File:** `tests/node/template-engine.test.ts`
**Framework:** vitest
**Naming:** `[methodUnderTest]_[scenario]_[expectedBehavior]`

### Test Groups (mirroring Python test file)

| Group | Test Count | Key Scenarios |
|-------|-----------|---------------|
| `constructor` | 3 | Stores config, sets Nunjucks options, nonexistent resources dir |
| `buildDefaultContext` | 3 | Full config (24 keys), minimal config (defaults), values match |
| `renderTemplate` | 10 | Variable resolution, no residuals, context override, whitespace, undefined error, missing file, empty template, multiline, none context |
| `renderString` | 7 | Simple variable, multiple vars, context override, undefined error, empty string, no variables, preserves newlines |
| `replacePlaceholders` | 12 | Single/multiple placeholders, all fields, unknown unchanged, Jinja2 syntax untouched, empty, no placeholders, idempotent, JSON in content, adjacent, inline, multiline |
| `injectSection` | 11 | Replaces marker, marker removed, missing marker, preserves surrounding, empty section, empty base, multiple markers, multiline section, whitespace, start/end markers |
| `concatFiles` | 9 | Two files, custom separator, single file, empty list, preserves order, nonexistent raises, empty file, trailing newlines, three files |

**Total: ~55 test cases**

### Fixtures Required

All existing fixtures in `tests/fixtures/templates/` and `tests/fixtures/concat_*.txt` are reusable. No new fixture files needed.

---

## 14. Quality Gates

| Gate | Target | Tool |
|------|--------|------|
| Line Coverage | >= 95% | `vitest --coverage` (v8) |
| Branch Coverage | >= 90% | `vitest --coverage` (v8) |
| Compilation | Zero errors | `tsc --noEmit` |
| Lint | Zero warnings | `tsc --noEmit` |
| JSDoc | All public methods | Manual review |
