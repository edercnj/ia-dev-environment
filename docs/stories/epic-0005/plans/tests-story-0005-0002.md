# Test Plan -- story-0005-0002: Epic Execution Report Template

**Story:** [story-0005-0002](../story-0005-0002.md)
**Implementation Plan:** [plan-story-0005-0002](./plan-story-0005-0002.md)
**Date:** 2026-03-16
**Status:** Draft

---

## Overview

Double-Loop TDD test plan for the Epic Execution Report Template. The outer loop (acceptance tests) validates end-to-end pipeline behavior: template presence in output, mandatory sections, placeholders, and dual-copy parity. The inner loop (unit tests) drives the `EpicReportAssembler` class implementation following the Transformation Priority Premise (TPP). Content validation tests verify the static template resource independently of the assembler.

### Test Files

| File | Type | Location |
|------|------|----------|
| `epic-report-assembler.test.ts` | Unit (assembler) | `tests/node/assembler/epic-report-assembler.test.ts` |
| `epic-execution-report-content.test.ts` | Content validation | `tests/node/content/epic-execution-report-content.test.ts` |
| `byte-for-byte.test.ts` | Integration (golden) | `tests/node/integration/byte-for-byte.test.ts` (existing, no code changes) |

### Constants and Fixtures

- **`aFullProjectConfig()`** from `tests/fixtures/project-config.fixture.ts` -- full config with all conditional sections
- **`RESOURCES_DIR`**, **`GOLDEN_DIR`**, **`CONFIG_PROFILES`** from `tests/helpers/integration-constants.ts`
- Template source: `resources/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md`

---

## Acceptance Tests (Outer Loop)

These tests validate the acceptance criteria from the Gherkin scenarios in the story spec. They run against the full pipeline output or directly against the template resource.

### AT-1: Template file exists at expected output paths after pipeline runs

| Field | Value |
|-------|-------|
| **Test ID** | AT-1 |
| **Test Name** | `pipelineOutput_epicReportTemplate_existsAtAllThreeOutputPaths` |
| **Depends On** | Full pipeline registered (Task 3.1: pipeline.ts modification) |
| **Parallel** | Yes (independent of AT-2..AT-4) |
| **TPP Level** | constant (file existence check) |
| **Gherkin** | "Template contém todas as secoes obrigatorias" (DADO que o template existe) |

**Description:** Run `runPipeline()` with a valid config. Assert that the 3 output paths exist:
- `{outputDir}/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md`
- `{outputDir}/.claude/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md`
- `{outputDir}/.github/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md`

---

### AT-2: Generated template contains all 8 mandatory sections

| Field | Value |
|-------|-------|
| **Test ID** | AT-2 |
| **Test Name** | `pipelineOutput_epicReportTemplate_containsAllEightMandatorySections` |
| **Depends On** | AT-1 (template must exist) |
| **Parallel** | Yes (reads from same pipeline output as AT-1 but independent assertion) |
| **TPP Level** | collection (8 sections checked) |
| **Gherkin** | "Template contém todas as secoes obrigatorias" (ENTAO contém a secao ...) |

**Description:** Read the template from any of the 3 output paths. Assert each of the 8 mandatory section headings is present:
1. `## Sumario Executivo`
2. `## Timeline de Execucao`
3. `## Status Final por Story`
4. `## Findings Consolidados`
5. `## Coverage Delta`
6. `## Commits e SHAs`
7. `## Issues Nao Resolvidos`
8. `## PR Link`

---

### AT-3: Generated template contains all 16 placeholders

| Field | Value |
|-------|-------|
| **Test ID** | AT-3 |
| **Test Name** | `pipelineOutput_epicReportTemplate_containsAllSixteenPlaceholders` |
| **Depends On** | AT-1 (template must exist) |
| **Parallel** | Yes |
| **TPP Level** | collection (16 placeholders checked) |
| **Gherkin** | "Template contém todos os placeholders definidos" |

**Description:** Read the template from any output path. Assert all 16 `{{PLACEHOLDER}}` tokens are present:
`EPIC_ID`, `BRANCH`, `STARTED_AT`, `FINISHED_AT`, `STORIES_COMPLETED`, `STORIES_FAILED`, `STORIES_BLOCKED`, `STORIES_TOTAL`, `COMPLETION_PERCENTAGE`, `PHASE_TIMELINE_TABLE`, `STORY_STATUS_TABLE`, `FINDINGS_SUMMARY`, `COVERAGE_BEFORE`, `COVERAGE_AFTER`, `COVERAGE_DELTA`, `COMMIT_LOG`, `UNRESOLVED_ISSUES`, `PR_LINK`.

---

### AT-4: Dual copy maintains byte-for-byte parity between .claude/ and .github/ outputs

| Field | Value |
|-------|-------|
| **Test ID** | AT-4 |
| **Test Name** | `pipelineOutput_epicReportDualCopy_allThreeOutputsAreIdentical` |
| **Depends On** | AT-1 (all 3 paths must exist) |
| **Parallel** | Yes |
| **TPP Level** | scalar (byte comparison) |
| **Gherkin** | "Dual copy é mantida" |

**Description:** Read all 3 output files. Assert their content is strictly equal (`===`). This validates RULE-001 (Context Isolation) by ensuring no output-path-specific transformation occurs.

---

## Unit Tests (Inner Loop -- EpicReportAssembler)

These tests drive the `EpicReportAssembler` class, ordered from degenerate to complex per TPP.

**Test file:** `tests/node/assembler/epic-report-assembler.test.ts`

**Setup pattern** (matches `docs-adr-assembler.test.ts`):
- `beforeEach`: create a temp directory via `mkdtemp`
- `afterEach`: remove temp directory via `rm -rf`
- Import `EpicReportAssembler`, `TemplateEngine`, `aFullProjectConfig`

---

### UT-1: Assembler returns empty array when template source does not exist

| Field | Value |
|-------|-------|
| **Test ID** | UT-1 |
| **Test Name** | `assemble_templateMissing_returnsEmptyArray` |
| **Depends On** | None (first test -- drives class creation) |
| **Parallel** | No (first in TDD sequence) |
| **TPP Level** | {} -> nil (degenerate: no input, empty output) |
| **TDD Cycle** | Red: class does not exist. Green: create `EpicReportAssembler` with missing-file guard returning `[]`. |

**Description:** Point `resourcesDir` to an empty temp directory. Call `assemble()`. Assert result is `[]`.

---

### UT-2: Assembler returns empty array when template is missing mandatory sections

| Field | Value |
|-------|-------|
| **Test ID** | UT-2 |
| **Test Name** | `assemble_templateMissingMandatorySection_returnsEmptyArray` |
| **Depends On** | UT-1 (class exists) |
| **Parallel** | No (drives section validation logic) |
| **TPP Level** | nil -> constant (validates guard clause) |
| **TDD Cycle** | Red: write incomplete template missing `## Coverage Delta`. Green: add `hasAllMandatorySections()` check returning `[]`. |

**Description:** Create a fake `resources/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md` with only 4 of 8 mandatory sections. Call `assemble()`. Assert result is `[]`.

---

### UT-3: Assembler creates output directories when they do not exist

| Field | Value |
|-------|-------|
| **Test ID** | UT-3 |
| **Test Name** | `assemble_outputDirDoesNotExist_createsDirectoryStructure` |
| **Depends On** | UT-2 (validation logic exists) |
| **Parallel** | No (drives `mkdirSync` logic) |
| **TPP Level** | constant -> scalar (directory creation side effect) |
| **TDD Cycle** | Red: point to non-existent nested output dir. Green: add `fs.mkdirSync({recursive: true})` for all 3 target dirs. |

**Description:** Use a deeply nested `outputDir` that does not exist (`tempDir/nested/deep/output`). Call `assemble()` with real resources. Assert all 3 output directories were created and contain the template file.

---

### UT-4: Assembler copies template to docs/epic/ path

| Field | Value |
|-------|-------|
| **Test ID** | UT-4 |
| **Test Name** | `assemble_validTemplate_copiesToDocsEpicPath` |
| **Depends On** | UT-3 (directory creation works) |
| **Parallel** | No (drives first copy target) |
| **TPP Level** | scalar (single file output) |
| **TDD Cycle** | Red: assert file at `docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md`. Green: implement `writeFileSync` to first target. |

**Description:** Call `assemble()` with real `RESOURCES_DIR`. Assert file exists at `{outputDir}/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md`.

---

### UT-5: Assembler copies template to .claude/templates/ path

| Field | Value |
|-------|-------|
| **Test ID** | UT-5 |
| **Test Name** | `assemble_validTemplate_copiesTo ClaudeTemplatesPath` |
| **Depends On** | UT-4 (first target implemented) |
| **Parallel** | No (drives second copy target) |
| **TPP Level** | scalar -> collection (adding second output) |
| **TDD Cycle** | Red: assert file at `.claude/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md`. Green: add second `writeFileSync`. |

**Description:** Call `assemble()` with real `RESOURCES_DIR`. Assert file exists at `{outputDir}/.claude/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md`.

---

### UT-6: Assembler copies template to .github/templates/ path

| Field | Value |
|-------|-------|
| **Test ID** | UT-6 |
| **Test Name** | `assemble_validTemplate_copiesToGithubTemplatesPath` |
| **Depends On** | UT-5 (second target implemented) |
| **Parallel** | No (drives third copy target) |
| **TPP Level** | collection (adding third output) |
| **TDD Cycle** | Red: assert file at `.github/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md`. Green: add third `writeFileSync`. |

**Description:** Call `assemble()` with real `RESOURCES_DIR`. Assert file exists at `{outputDir}/.github/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md`.

---

### UT-7: Template content is copied verbatim (no placeholder rendering)

| Field | Value |
|-------|-------|
| **Test ID** | UT-7 |
| **Test Name** | `assemble_validTemplate_copiesContentVerbatimWithoutPlaceholderResolution` |
| **Depends On** | UT-6 (all 3 targets implemented) |
| **Parallel** | Yes (after UT-6, independent assertion) |
| **TPP Level** | scalar (byte comparison) |
| **TDD Cycle** | Red: compare output content against source template. Green: already satisfied by `readFileSync`/`writeFileSync` design. Refactor: extract constants. |

**Description:** Read the source template from `RESOURCES_DIR`. Read the output from `docs/epic/`. Assert content is strictly equal. This proves `{{PLACEHOLDER}}` tokens are NOT resolved by `TemplateEngine` -- the assembler copies verbatim.

---

### UT-8: All output paths are returned in the result array

| Field | Value |
|-------|-------|
| **Test ID** | UT-8 |
| **Test Name** | `assemble_validTemplate_returnsThreeFilePaths` |
| **Depends On** | UT-6 (all 3 targets implemented) |
| **Parallel** | Yes (after UT-6, independent assertion) |
| **TPP Level** | collection (array of 3 paths) |
| **TDD Cycle** | Red: assert `result.length === 3` and each path contains expected substrings. Green: already satisfied by implementation. |

**Description:** Call `assemble()`. Assert:
- `result` has length 3
- `result[0]` contains `docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md`
- `result[1]` contains `.claude/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md`
- `result[2]` contains `.github/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md`

---

## Integration Tests (Golden Files)

These tests leverage the existing `byte-for-byte.test.ts` infrastructure. **No new test code is required** -- the golden file comparison automatically covers the new output files once golden directories are updated.

---

### IT-1: Golden file test -- template appears in all 8 profile outputs

| Field | Value |
|-------|-------|
| **Test ID** | IT-1 |
| **Test Name** | `pipelineMatchesGoldenFiles_{profileName}` (existing parametrized test) |
| **Depends On** | Pipeline registration (Task 3.1), golden file update (Task 3.3) |
| **Parallel** | No (sequential per profile via `describe.sequential.each`) |
| **TPP Level** | collection (8 profiles x 3 files = 24 golden files) |

**Description:** The existing `byte-for-byte.test.ts` runs `runPipeline()` for each of the 8 profiles and compares the output against golden directories. After golden files are regenerated with the 3 new template files per profile, these tests automatically validate:
- No missing files (template appears in output)
- No extra files (no unexpected artifacts)
- Byte-for-byte parity with golden reference

**Action required:** Regenerate golden files for all 8 profiles:
- `tests/golden/{profile}/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md`
- `tests/golden/{profile}/.claude/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md`
- `tests/golden/{profile}/.github/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md`

---

### IT-2: Golden file content matches source template byte-for-byte

| Field | Value |
|-------|-------|
| **Test ID** | IT-2 |
| **Test Name** | `pipelineMatchesGoldenFiles_{profileName}` (same test as IT-1, different assertion) |
| **Depends On** | IT-1 (golden files exist) |
| **Parallel** | No (same sequential test suite) |
| **TPP Level** | scalar (content diff check) |

**Description:** Since the assembler copies verbatim (no profile-specific substitution), all 24 golden files across 8 profiles will be identical to each other and to the source template. The `verifyOutput()` function performs content-level diff comparison, so any deviation from golden reference is detected. The existing `noMissingFiles`, `noExtraFiles`, and mismatch assertions cover this.

---

## Content Validation Tests

These tests validate the static template resource directly, independent of the assembler. They read `resources/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md` and check structure.

**Test file:** `tests/node/content/epic-execution-report-content.test.ts`

**Pattern:** Follows `threat-model-template-content.test.ts` -- read file in `beforeAll`, assert sections/tokens in individual tests.

---

### CV-1: Template has valid markdown heading hierarchy (h1 -> h2 -> h3)

| Field | Value |
|-------|-------|
| **Test ID** | CV-1 |
| **Test Name** | `templateStructure_headingHierarchy_startsWithH1FollowedByH2` |
| **Depends On** | Template file created (Task 2.1) |
| **Parallel** | Yes (independent of CV-2..CV-4) |
| **TPP Level** | scalar (structural check) |
| **Gherkin** | "Template usa markdown valido" (headings seguem hierarquia h1 -> h2 -> h3) |

**Description:**
- Parse all lines matching `/^#{1,6}\s/`
- Assert the first heading is `# ` (h1)
- Assert no heading level skip (no h3 without preceding h2)
- Assert all heading lines match valid markdown syntax `/^#{1,6}\s+\S/`

**Additional sub-tests:**

| Sub-test | Test Name | Assertion |
|----------|-----------|-----------|
| CV-1a | `templateStructure_firstHeading_isH1` | First heading starts with `# ` |
| CV-1b | `templateStructure_allHeadings_useValidMarkdownSyntax` | Every heading matches `/^#{1,6}\s+\S/` |
| CV-1c | `templateStructure_noHeadingLevelSkip_hierarchyIsValid` | No h3 appears before an h2 |

---

### CV-2: Template contains all mandatory sections per story spec

| Field | Value |
|-------|-------|
| **Test ID** | CV-2 |
| **Test Name** | `templateContent_mandatorySections_containsAllEight` |
| **Depends On** | Template file created (Task 2.1) |
| **Parallel** | Yes |
| **TPP Level** | collection (8 items) |
| **Gherkin** | "Template contém todas as secoes obrigatorias" |

**Description:** Use `it.each` with the 8 mandatory sections:

```typescript
const MANDATORY_SECTIONS = [
  "## Sumario Executivo",
  "## Timeline de Execucao",
  "## Status Final por Story",
  "## Findings Consolidados",
  "## Coverage Delta",
  "## Commits e SHAs",
  "## Issues Nao Resolvidos",
  "## PR Link",
] as const;
```

For each section: `expect(templateContent).toContain(section)`.

**Additional sub-tests (parametrized):**

| Sub-test | Test Name Pattern | Assertion |
|----------|-------------------|-----------|
| CV-2a..CV-2h | `templateContent_mandatorySection_contains_{sectionName}` | Section heading present |

---

### CV-3: Template contains all 16 placeholders per data contract

| Field | Value |
|-------|-------|
| **Test ID** | CV-3 |
| **Test Name** | `templateContent_placeholders_containsAllSixteen` |
| **Depends On** | Template file created (Task 2.1) |
| **Parallel** | Yes |
| **TPP Level** | collection (16 items) |
| **Gherkin** | "Template contém todos os placeholders definidos" |

**Description:** Use `it.each` with the 16 placeholders from the story spec data contract (section 5):

```typescript
const REQUIRED_PLACEHOLDERS = [
  "{{EPIC_ID}}",
  "{{BRANCH}}",
  "{{STARTED_AT}}",
  "{{FINISHED_AT}}",
  "{{STORIES_COMPLETED}}",
  "{{STORIES_FAILED}}",
  "{{STORIES_BLOCKED}}",
  "{{STORIES_TOTAL}}",
  "{{COMPLETION_PERCENTAGE}}",
  "{{PHASE_TIMELINE_TABLE}}",
  "{{STORY_STATUS_TABLE}}",
  "{{FINDINGS_SUMMARY}}",
  "{{COVERAGE_BEFORE}}",
  "{{COVERAGE_AFTER}}",
  "{{COVERAGE_DELTA}}",
  "{{COMMIT_LOG}}",
  "{{UNRESOLVED_ISSUES}}",
  "{{PR_LINK}}",
] as const;
```

For each placeholder: `expect(templateContent).toContain(placeholder)`.

**Note:** The story spec section 3.2 lists 16 placeholders. The implementation plan section 2.1 confirms 18 tokens (16 from spec + `{{PHASE_TIMELINE_TABLE}}` and `{{STORY_STATUS_TABLE}}` which were implicit in the spec's "tabela gerada dinamicamente" references). The test uses the full 18-token list from the plan.

**Additional sub-tests (parametrized):**

| Sub-test | Test Name Pattern | Assertion |
|----------|-------------------|-----------|
| CV-3a..CV-3r | `templateContent_placeholder_contains_{placeholderName}` | Placeholder token present |

---

### CV-4: Template follows _TEMPLATE-*.md naming convention

| Field | Value |
|-------|-------|
| **Test ID** | CV-4 |
| **Test Name** | `templateFile_namingConvention_followsTemplatePrefix` |
| **Depends On** | Template file created (Task 2.1) |
| **Parallel** | Yes |
| **TPP Level** | constant (single check) |
| **Gherkin** | "Template segue convencao de naming do projeto" |

**Description:**
- Assert `_TEMPLATE-EPIC-EXECUTION-REPORT.md` exists in `resources/templates/`
- Assert filename starts with `_TEMPLATE-` prefix
- Assert filename ends with `.md` extension
- Assert file is located in the `resources/templates/` directory (not elsewhere)

---

## TDD Execution Order

The following table shows the recommended implementation sequence following Double-Loop TDD. The outer loop acceptance test (AT-1) is written first (Red), then inner loop unit tests drive the implementation to make it Green.

| Step | Loop | Test ID | Action | Status |
|------|------|---------|--------|--------|
| 1 | Content | CV-4 | Red: template file does not exist at `resources/templates/` | Failing |
| 2 | Content | CV-4 | Green: create `_TEMPLATE-EPIC-EXECUTION-REPORT.md` with all sections/placeholders | Passing |
| 3 | Content | CV-2 | Red: check 8 mandatory sections (already Green from step 2) | Passing |
| 4 | Content | CV-3 | Red: check 16 placeholders (already Green from step 2) | Passing |
| 5 | Content | CV-1 | Red: heading hierarchy validation (already Green from step 2) | Passing |
| 6 | Inner | UT-1 | Red: `EpicReportAssembler` class does not exist | Failing |
| 7 | Inner | UT-1 | Green: create class with missing-template guard | Passing |
| 8 | Inner | UT-2 | Red: incomplete template should return `[]` | Failing |
| 9 | Inner | UT-2 | Green: add `hasAllMandatorySections()` validation | Passing |
| 10 | Inner | UT-3 | Red: nested output dir does not exist | Failing |
| 11 | Inner | UT-3 | Green: add `mkdirSync({recursive: true})` | Passing |
| 12 | Inner | UT-4 | Red: file at `docs/epic/` path | Failing |
| 13 | Inner | UT-4 | Green: implement first `writeFileSync` | Passing |
| 14 | Inner | UT-5 | Red: file at `.claude/templates/` path | Failing |
| 15 | Inner | UT-5 | Green: add second `writeFileSync` | Passing |
| 16 | Inner | UT-6 | Red: file at `.github/templates/` path | Failing |
| 17 | Inner | UT-6 | Green: add third `writeFileSync` | Passing |
| 18 | Inner | UT-7 | Red: content comparison (already Green by design) | Passing |
| 19 | Inner | UT-8 | Red: return array length 3 (already Green) | Passing |
| 20 | -- | -- | Refactor: extract constants, consolidate validation | -- |
| 21 | Outer | AT-1 | Red: register assembler in pipeline, run pipeline | Failing |
| 22 | Outer | AT-1 | Green: add to `buildAssemblers()` in `pipeline.ts` | Passing |
| 23 | Outer | AT-2 | Green: (already satisfied by template content) | Passing |
| 24 | Outer | AT-3 | Green: (already satisfied by template content) | Passing |
| 25 | Outer | AT-4 | Green: (already satisfied by verbatim copy design) | Passing |
| 26 | Integration | IT-1 | Red: golden files missing new template files | Failing |
| 27 | Integration | IT-2 | Green: regenerate golden files for all 8 profiles | Passing |

---

## Test Count Summary

| Category | Count | File |
|----------|-------|------|
| Acceptance Tests (AT) | 4 | `epic-report-assembler.test.ts` |
| Unit Tests (UT) | 8 | `epic-report-assembler.test.ts` |
| Integration Tests (IT) | 2 (covered by existing `byte-for-byte.test.ts`, 8 profiles x 5 assertions) | `byte-for-byte.test.ts` (no changes) |
| Content Validation (CV) | 4 top-level + ~28 parametrized sub-tests | `epic-execution-report-content.test.ts` |
| **Total new test cases** | **~44** | 2 new files + 0 modified |

---

## Coverage Impact

| Metric | Before | After | Notes |
|--------|--------|-------|-------|
| Line Coverage | 99.6% | >= 99.6% | New assembler class is small (~30 lines), fully tested |
| Branch Coverage | 97.84% | >= 97.84% | Two branches (template missing, sections invalid) both covered by UT-1 and UT-2 |

The new `EpicReportAssembler` has exactly 2 branch points (template existence guard, section validation guard), both driven by degenerate unit tests UT-1 and UT-2.
