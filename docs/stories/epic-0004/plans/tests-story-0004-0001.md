# Test Plan -- story-0004-0001: ADR Template & Structure `docs/adr/`

## Summary

- Total test classes: 2 (unit + integration via golden files)
- Total test methods: ~24 (estimated)
- Categories covered: Unit, Integration (golden file byte-for-byte)
- Estimated line coverage: ~98%
- Estimated branch coverage: ~95%

## TDD Strategy

### Double-Loop TDD

- **Outer Loop (Acceptance Tests):** Integration-level golden file tests — the byte-for-byte test already covers all 8 profiles. Adding `docs/adr/README.md` to golden directories validates end-to-end pipeline output.
- **Inner Loop (Unit Tests):** `DocsAdrAssembler` methods tested in TPP order (degenerate → unconditional → conditional → iteration → edge cases).

### TPP Progression

| Level | Transformation | Scenarios |
|-------|---------------|-----------|
| 1 | Constant → Scalar | UT-1: assembler returns empty array for missing template |
| 2 | Unconditional | UT-2, UT-3: assembler generates README with correct structure |
| 3 | Scalar → Collection | UT-4: assembler returns list of generated file paths |
| 4 | Conditional | UT-5, UT-6: template validation (mandatory sections present/absent) |
| 5 | Iteration | UT-7, UT-8, UT-9: sequential numbering logic |
| 6 | Edge cases | UT-10, UT-11, UT-12: kebab-case, boundary numbers, backward compat |

---

## Acceptance Tests (Outer Loop)

### AT-1: Pipeline generates `docs/adr/README.md` for all profiles

| # | Test Name | Description | Depends On |
|---|-----------|-------------|------------|
| AT-1 | Golden file byte-for-byte parity includes `docs/adr/README.md` | After adding `docs/adr/README.md` to all 8 golden profile directories, the existing `byte-for-byte.test.ts` validates the new output automatically. No new test code — only golden file updates. | UT-2, UT-3 |

**Parallel:** No (sequential per existing `describe.sequential.each` pattern)

### AT-2: Pipeline descriptor count includes new assembler

| # | Test Name | Description | Depends On |
|---|-----------|-------------|------------|
| AT-2 | `pipeline.test.ts` assembler count incremented | The `buildAssemblers()` function returns N+1 descriptors after adding `DocsAdrAssembler`. Update expected count in existing pipeline test. | UT-2 |

**Parallel:** Yes (independent of AT-1)

---

## Test Class 1: `DocsAdrAssemblerTest`

**File:** `tests/node/assembler/docs-adr-assembler.test.ts`

### Unit Tests — TPP Order

#### Level 1: Degenerate (Constant → Scalar)

| # | Test Name | Description | Depends On | Parallel |
|---|-----------|-------------|------------|----------|
| UT-1 | `assemble_templateMissing_returnsEmptyArray` | When `_TEMPLATE-ADR.md` does not exist in `resourcesDir/templates/`, `assemble()` returns `[]` (graceful degradation, no crash). | — | Yes |

#### Level 2: Unconditional (Happy Path)

| # | Test Name | Description | Depends On | Parallel |
|---|-----------|-------------|------------|----------|
| UT-2 | `assemble_validConfig_generatesDocsAdrReadme` | Given a valid `ProjectConfig` and `resourcesDir` with `_TEMPLATE-ADR.md`, `assemble()` creates `docs/adr/README.md` in `outputDir`. | UT-1 | No |
| UT-3 | `assemble_validConfig_readmeContainsProjectName` | The generated `docs/adr/README.md` contains the project name from config (placeholder `{{PROJECT_NAME}}` resolved). | UT-2 | No |
| UT-4 | `assemble_validConfig_returnsGeneratedFilePaths` | `assemble()` returns an array containing the relative path `docs/adr/README.md`. | UT-2 | Yes |

#### Level 3: Content Validation

| # | Test Name | Description | Depends On | Parallel |
|---|-----------|-------------|------------|----------|
| UT-5 | `assemble_validConfig_readmeContainsAdrTableHeaders` | The generated README contains a markdown table with headers: ID, Title, Status, Date. | UT-2 | Yes |
| UT-6 | `assemble_validConfig_readmeContainsArchitectureDecisionRecordsTitle` | The generated README contains `# Architecture Decision Records` as H1 heading. | UT-2 | Yes |

#### Level 4: Conditional (Template Validation)

| # | Test Name | Description | Depends On | Parallel |
|---|-----------|-------------|------------|----------|
| UT-7 | `assemble_templateWithAllSections_generatesSuccessfully` | When `_TEMPLATE-ADR.md` contains all mandatory sections (Status, Context, Decision, Consequences), assembler succeeds. | UT-2 | Yes |

#### Level 5: Iteration (Sequential Numbering)

| # | Test Name | Description | Depends On | Parallel |
|---|-----------|-------------|------------|----------|
| UT-8 | `getNextAdrNumber_emptyDirectory_returns1` | Given an empty or non-existent directory, returns `1`. | — | Yes |
| UT-9 | `getNextAdrNumber_existingAdrs_returnsNextSequential` | Given directory with `ADR-0001-foo.md` and `ADR-0002-bar.md`, returns `3`. | UT-8 | No |
| UT-10 | `getNextAdrNumber_gapInNumbers_returnsMaxPlusOne` | Given ADRs 0001 and 0003 (gap at 0002), returns `4` (never fills gaps). | UT-9 | No |

#### Level 6: Edge Cases (Formatting + Boundaries)

| # | Test Name | Description | Depends On | Parallel |
|---|-----------|-------------|------------|----------|
| UT-11 | `formatAdrFilename_simpleTitle_returnsKebabCase` | `formatAdrFilename(1, "Use PostgreSQL")` returns `"ADR-0001-use-postgresql.md"`. | — | Yes |
| UT-12 | `formatAdrFilename_specialCharacters_sanitizesTitle` | Removes non-alphanumeric characters except hyphens: `"Use gRPC (v2)"` → `"ADR-0001-use-grpc-v2.md"`. | UT-11 | No |
| UT-13 | `formatAdrFilename_largeNumber_padsCorrectly` | `formatAdrFilename(42, "Title")` → `"ADR-0042-title.md"` (4-digit zero-padded). | UT-11 | Yes |
| UT-14 | `assemble_outputDirDoesNotExist_createsDirectoryStructure` | When `outputDir/docs/adr/` does not exist, `assemble()` creates it via `mkdirSync({ recursive: true })`. | UT-2 | Yes |

### Parametrized Tests

| # | Matrix | Test Name | Source | Rows |
|---|--------|-----------|--------|------|
| PT-1 | Sequential numbering | `getNextAdrNumber_variousExistingCounts_returnsCorrectNext` | Inline array | 4 |

**PT-1 Data:**

| Existing ADR files | Expected next number |
|-------------------|---------------------|
| `[]` (empty) | 1 |
| `["ADR-0001-foo.md"]` | 2 |
| `["ADR-0001-foo.md", "ADR-0002-bar.md"]` | 3 |
| `["ADR-0001-foo.md", "ADR-0003-baz.md"]` | 4 |

| # | Matrix | Test Name | Source | Rows |
|---|--------|-----------|--------|------|
| PT-2 | Kebab-case formatting | `formatAdrFilename_variousTitles_formatsCorrectly` | Inline array | 5 |

**PT-2 Data:**

| Number | Title | Expected filename |
|--------|-------|------------------|
| 1 | `"Use PostgreSQL"` | `"ADR-0001-use-postgresql.md"` |
| 2 | `"Adopt Hexagonal Architecture"` | `"ADR-0002-adopt-hexagonal-architecture.md"` |
| 10 | `"Use gRPC (v2)"` | `"ADR-0010-use-grpc-v2.md"` |
| 42 | `"Simple"` | `"ADR-0042-simple.md"` |
| 1 | `"Title--with---dashes"` | `"ADR-0001-title-with-dashes.md"` |

---

## Test Class 2: Template Structure Validation

**File:** `tests/node/assembler/docs-adr-assembler.test.ts` (same file, separate `describe` block)

### Template Content Tests

| # | Test Name | Description | Depends On | Parallel |
|---|-----------|-------------|------------|----------|
| UT-15 | `template_containsFrontmatterWithStatusField` | `_TEMPLATE-ADR.md` has YAML frontmatter with `status` field. | — | Yes |
| UT-16 | `template_containsFrontmatterWithDateField` | `_TEMPLATE-ADR.md` has YAML frontmatter with `date` field. | — | Yes |
| UT-17 | `template_containsFrontmatterWithDecidersField` | `_TEMPLATE-ADR.md` has YAML frontmatter with `deciders` field. | — | Yes |
| UT-18 | `template_containsMandatorySection_status` | Template contains `## Status` section. | — | Yes |
| UT-19 | `template_containsMandatorySection_context` | Template contains `## Context` section. | — | Yes |
| UT-20 | `template_containsMandatorySection_decision` | Template contains `## Decision` section. | — | Yes |
| UT-21 | `template_containsMandatorySection_consequences` | Template contains `## Consequences` section. | — | Yes |
| UT-22 | `template_containsOptionalSection_relatedAdrs` | Template contains `## Related ADRs` section. | — | Yes |
| UT-23 | `template_containsOptionalSection_storyReference` | Template contains `## Story Reference` section. | — | Yes |
| UT-24 | `template_containsProjectNamePlaceholder` | Template contains `{{PROJECT_NAME}}` placeholder. | — | Yes |

---

## Integration Tests

### IT-1: Pipeline Registration

| # | Test Name | Description | Depends On | Parallel |
|---|-----------|-------------|------------|----------|
| IT-1 | `buildAssemblers_includesDocsAdrAssembler` | `buildAssemblers()` returns a descriptor with `name: "DocsAdrAssembler"` and `target: "root"`. | UT-2 | No |

### IT-2: Golden File Parity (existing test — no new code)

| # | Test Name | Description | Depends On | Parallel |
|---|-----------|-------------|------------|----------|
| IT-2 | `byte-for-byte parity for all 8 profiles` | Update golden files with `docs/adr/README.md` for each profile. Existing `byte-for-byte.test.ts` validates automatically. | All UTs | No |

---

## Coverage Estimation

| Class/Module | Public Methods | Branches | Est. Tests | Line % | Branch % |
|-------------|---------------|----------|-----------|--------|----------|
| `DocsAdrAssembler` | 1 (`assemble`) | 2 (template exists/missing) | 8 | 100% | 100% |
| `getNextAdrNumber` | 1 | 3 (empty, sequential, gap) | 4 | 100% | 100% |
| `formatAdrFilename` | 1 | 2 (simple, special chars) | 5 | 100% | 100% |
| Template content | N/A (static) | N/A | 10 | N/A | N/A |
| Pipeline registration | 0 (config) | 0 | 1 | 100% | 100% |
| **Total** | **3** | **7** | **~28** | **~98%** | **~95%** |

---

## Risks and Gaps

1. **Golden file regeneration:** All 8 profile golden directories need `docs/adr/README.md` added. If any profile has a different `project_name`, the README content will differ — each golden file must match exactly.
2. **Template source of truth:** Tests reading `_TEMPLATE-ADR.md` from `resources/templates/` test the actual template, not a copy. If the template is modified, tests break immediately (desired behavior).
3. **Sequential numbering boundary:** `getNextAdrNumber` with >9999 ADRs — padding logic may need 5+ digits. Current scope caps at 4-digit padding per story spec. Add boundary test if format is `NNNN` (fixed 4 digits).
4. **Dual copy (RULE-001):** The plan identifies no dual-copy needed for this template type (it's in `resources/templates/`, not `resources/skills-templates/`). If the story intends dual-copy in `.claude/` + `.github/` output, additional tests are needed.

---

## Test Execution Order (TDD Sequence)

```
Cycle 1:  [RED] UT-1  → [GREEN] assemble() stub returning []
Cycle 2:  [RED] UT-2  → [GREEN] assemble() generates README file
Cycle 3:  [RED] UT-3  → [GREEN] placeholder substitution
Cycle 4:  [RED] UT-4  → [GREEN] return file path array
Cycle 5:  [RED] UT-5,6 → [GREEN] README content (table headers, H1)
Cycle 6:  [RED] UT-7  → [GREEN] template section validation
Cycle 7:  [RED] UT-8  → [GREEN] getNextAdrNumber() stub
Cycle 8:  [RED] UT-9,10 → [GREEN] sequential numbering logic
Cycle 9:  [RED] UT-11,12,13 → [GREEN] formatAdrFilename()
Cycle 10: [RED] UT-14 → [GREEN] directory creation
Cycle 11: [RED] UT-15-24 → [GREEN] template content validation (bulk — static assertions)
Cycle 12: [RED] IT-1  → [GREEN] pipeline registration
Cycle 13: Update golden files → [GREEN] IT-2 byte-for-byte parity
```
