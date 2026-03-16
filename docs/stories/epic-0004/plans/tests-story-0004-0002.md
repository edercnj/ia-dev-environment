# Test Plan — story-0004-0002: Service Architecture Documentation Template

## Summary

- Total test classes: 3 (new: 1, modified: 2)
- Total test methods: ~32 (estimated)
- Categories covered: Unit, Integration (golden file)
- Estimated line coverage: ~97%
- Estimated branch coverage: ~94%

---

## Test Class 1: `tests/node/assembler/docs-assembler.test.ts` (NEW)

Tests for `DocsAssembler.assemble()` — renders `_TEMPLATE-SERVICE-ARCHITECTURE.md` template
and writes `docs/architecture/service-architecture.md`.

### TPP Progression: degenerate → unconditional → conditions → edge cases

### Happy Path

| # | Test ID | Test Name | Description | Depends On | Parallel |
|---|---------|-----------|-------------|------------|----------|
| 1 | UT-1 | `assemble_templateExists_returnsFilePathArray` | Template present → returns array with generated file path | — | yes |
| 2 | UT-2 | `assemble_templateExists_createsDocsArchitectureDir` | Ensures `docs/architecture/` directory is created | UT-1 | yes |
| 3 | UT-3 | `assemble_templateExists_writesServiceArchitectureMd` | Output file `docs/architecture/service-architecture.md` exists on disk | UT-1 | yes |
| 4 | UT-4 | `assemble_templateExists_resolvesServiceNamePlaceholder` | `{{ project_name }}` replaced with config project name | UT-3 | no |
| 5 | UT-5 | `assemble_templateExists_resolvesArchitecturePlaceholder` | `{{ architecture_style }}` replaced with config architecture style | UT-3 | no |
| 6 | UT-6 | `assemble_templateExists_resolvesLanguagePlaceholder` | `{{ language_name }}` replaced with config language name | UT-3 | no |
| 7 | UT-7 | `assemble_templateExists_resolvesFrameworkPlaceholder` | `{{ framework_name }}` replaced with config framework name | UT-3 | no |
| 8 | UT-8 | `assemble_templateExists_resolvesInterfacesListPlaceholder` | `{{ interfaces_list }}` replaced with joined interface types | UT-3 | no |
| 9 | UT-9 | `assemble_templateExists_containsAllTenSections` | Output has all 10 mandatory sections (## 1 through ## 10) | UT-3 | yes |
| 10 | UT-10 | `assemble_templateExists_containsMermaidC4Diagram` | Section 2 contains ` ```mermaid ` block with `graph TD` | UT-3 | yes |
| 11 | UT-11 | `assemble_templateExists_containsNfrTable` | Section 6 contains Markdown table with Metric, Target, Measurement columns | UT-3 | yes |
| 12 | UT-12 | `assemble_templateExists_containsIntegrationsTable` | Section 3 contains Markdown table with System, Protocol, Purpose, SLO columns | UT-3 | yes |

### Error / Degenerate Path

| # | Test ID | Test Name | Description | Depends On | Parallel |
|---|---------|-----------|-------------|------------|----------|
| 13 | UT-13 | `assemble_templateMissing_returnsEmptyArray` | No template file → returns `[]`, no error thrown | — | yes |
| 14 | UT-14 | `assemble_templateMissing_doesNotCreateDocsDir` | No template file → `docs/architecture/` is NOT created | UT-13 | yes |

### Boundary / Edge Cases

| # | Test ID | Test Name | Description | Depends On | Parallel |
|---|---------|-----------|-------------|------------|----------|
| 15 | UT-15 | `assemble_outputDirNotExists_createsRecursively` | Output dir doesn't exist yet → created with `{ recursive: true }` | — | yes |
| 16 | UT-16 | `assemble_multipleInterfaces_joinsWithComma` | Config with `[rest, grpc]` → `interfaces_list` = `"rest, grpc"` | — | yes |
| 17 | UT-17 | `assemble_singleInterface_noComma` | Config with `[rest]` → `interfaces_list` = `"rest"` | — | yes |
| 18 | UT-18 | `assemble_noInterfaces_defaultsToNone` | Config with `[]` → `interfaces_list` = `"none"` | — | yes |

### Acceptance Tests (AT — Outer Loop)

| # | Test ID | Test Name | Description | Maps to AC |
|---|---------|-----------|-------------|-----------|
| 19 | AT-1 | `assemble_fullConfig_generatesCompleteArchitectureDoc` | Full config → file with all 10 sections, resolved placeholders, Mermaid diagrams, NFR table | AC1-AC4 |
| 20 | AT-2 | `assemble_noTemplate_gracefulNoOp` | Missing template → empty result, backward compatible | AC6 |

---

## Test Class 2: `tests/node/template-engine.test.ts` (MODIFY — extend)

### Happy Path

| # | Test ID | Test Name | Description | Depends On | Parallel |
|---|---------|-----------|-------------|------------|----------|
| 21 | UT-19 | `buildDefaultContext_validConfig_returns25Fields` | Update from 24 to 25 fields (new: `interfaces_list`) | — | yes |
| 22 | UT-20 | `buildDefaultContext_multipleInterfaces_joinsInterfaceTypes` | Config with `[rest, grpc]` → `interfaces_list` = `"rest, grpc"` | — | yes |
| 23 | UT-21 | `buildDefaultContext_singleInterface_returnsType` | Config with `[rest]` → `interfaces_list` = `"rest"` | — | yes |
| 24 | UT-22 | `buildDefaultContext_noInterfaces_returnsNone` | Config with `[]` → `interfaces_list` = `"none"` | — | yes |

---

## Test Class 3: `tests/node/assembler/pipeline.test.ts` (MODIFY — extend)

### Happy Path

| # | Test ID | Test Name | Description | Depends On | Parallel |
|---|---------|-----------|-------------|------------|----------|
| 25 | UT-23 | `buildAssemblers_returns18Assemblers` | Update count from 17 → 18 (add DocsAssembler) | — | yes |
| 26 | UT-24 | `buildAssemblers_docsAssemblerBeforeReadme` | DocsAssembler positioned before ReadmeAssembler in pipeline order | — | yes |
| 27 | UT-25 | `buildAssemblers_docsAssemblerTargetIsDocs` | DocsAssembler has target `"docs"` | — | yes |

### Integration (executeAssemblers target resolution)

| # | Test ID | Test Name | Description | Depends On | Parallel |
|---|---------|-----------|-------------|------------|----------|
| 28 | UT-26 | `executeAssemblers_docsTarget_resolvesToDocsSubdir` | Target `"docs"` resolves to `{outputDir}/docs/` | — | yes |

---

## Integration Tests (Golden File — `tests/node/integration/byte-for-byte.test.ts`)

No code changes needed. Golden files for all 8 profiles must be regenerated to include `docs/architecture/service-architecture.md`.

| # | Test ID | Description | Profiles |
|---|---------|-------------|----------|
| 29 | IT-1 | Golden file parity includes `docs/architecture/service-architecture.md` | All 8 profiles |
| 30 | IT-2 | No missing files (new output included in golden set) | All 8 profiles |
| 31 | IT-3 | No extra files | All 8 profiles |
| 32 | IT-4 | Resolved placeholders match profile-specific values | All 8 profiles |

---

## Coverage Estimation

| Class / Module | Public Methods | Branches | Est. Tests | Line % | Branch % |
|----------------|---------------|----------|-----------|--------|----------|
| `DocsAssembler` | 1 (`assemble`) | 2 (template exists/missing) | 18 | 100% | 100% |
| `buildDefaultContext` (extension) | 1 | 1 (interfaces join/fallback) | 4 | 100% | 100% |
| `pipeline.ts` (extension) | 2 (`buildAssemblers`, `executeAssemblers`) | 1 (new target) | 4 | 98% | 95% |
| **Template** (`_TEMPLATE-SERVICE-ARCHITECTURE.md`) | N/A (static) | 0 | Validated via UT-9..12 | N/A | N/A |
| **Golden files** (integration) | N/A | N/A | 4 (existing tests, updated golden) | N/A | N/A |

**Overall estimated:** ~97% line, ~94% branch (exceeds 95% / 90% thresholds).

---

## Risks and Gaps

1. **Nunjucks rendering errors**: If the template uses syntax not supported by Nunjucks (e.g., Jinja2-only features), rendering will fail. Mitigation: validate template against Nunjucks in UT-1.
2. **Golden file regeneration scope**: All 8 profiles need new golden files. Risk of missed profile. Mitigation: CI runs all profiles; `byte-for-byte.test.ts` catches mismatches.
3. **Mermaid syntax in template**: The template contains `{{ }}` inside Mermaid code blocks which could conflict with Nunjucks. Mitigation: Use `{% raw %}...{% endraw %}` blocks or ensure Mermaid placeholders use different syntax.
4. **`interfaces_list` derivation**: Must handle edge case of empty interfaces array gracefully (default to `"none"`).

---

## Quality Checks

- [x] Every acceptance criterion maps to ≥1 test (AC1→UT-9, AC2→UT-4..8, AC3→UT-10, AC4→UT-11, AC5→IT-1, AC6→UT-13/AT-2)
- [x] Every exception type has ≥1 error path test (template missing→UT-13)
- [x] All applicable test categories represented (Unit + Integration)
- [x] Boundary values use triplet pattern (0 interfaces, 1 interface, multiple interfaces: UT-16..18)
- [x] Estimated coverage meets thresholds (97% / 94%)
- [x] Test naming follows `[method]_[scenario]_[expected]` convention
