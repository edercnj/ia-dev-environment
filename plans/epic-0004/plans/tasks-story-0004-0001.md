# Task Decomposition -- story-0004-0001: ADR Template & Structure `docs/adr/`

**Story:** [story-0004-0001](../story-0004-0001.md)
**Plan:** [plan-story-0004-0001](plan-story-0004-0001.md)
**Test Plan:** [tests-story-0004-0001](tests-story-0004-0001.md)
**Date:** 2026-03-15
**Status:** Draft

---

## Task Summary

| Total Tasks | RED | GREEN | REFACTOR |
|-------------|-----|-------|----------|
| 30 | 13 | 14 | 3 |

---

## TDD Cycle 1: Degenerate Case — Missing Template

> **TPP Level 1:** Constant -> Scalar. Assembler returns empty array when template is missing.

| Task ID | Type | Description | Files Affected | Depends On | Parallel | TDD Ref |
|---------|------|-------------|----------------|------------|----------|---------|
| TASK-1 | RED | Write test `assemble_templateMissing_returnsEmptyArray`: verify `assemble()` returns `[]` when `_TEMPLATE-ADR.md` does not exist in `resourcesDir/templates/`. | `tests/node/assembler/docs-adr-assembler.test.ts` | -- | Yes | UT-1 |
| TASK-2 | GREEN | Create `DocsAdrAssembler` class with `assemble()` method stub that checks for template existence and returns `[]` when missing. | `src/assembler/docs-adr-assembler.ts` | TASK-1 | No | UT-1 |

---

## TDD Cycle 2: Unconditional — README File Generation

> **TPP Level 2:** Unconditional. Assembler generates the `docs/adr/README.md` file.

| Task ID | Type | Description | Files Affected | Depends On | Parallel | TDD Ref |
|---------|------|-------------|----------------|------------|----------|---------|
| TASK-3 | RED | Write test `assemble_validConfig_generatesDocsAdrReadme`: verify `assemble()` creates `docs/adr/README.md` in `outputDir` given a valid config and template. | `tests/node/assembler/docs-adr-assembler.test.ts` | TASK-2 | No | UT-2 |
| TASK-4 | GREEN | Implement `assemble()` to read `_TEMPLATE-ADR.md`, create `docs/adr/` directory, and write `README.md` with basic content. Create `resources/templates/_TEMPLATE-ADR.md` with all mandatory sections and frontmatter. | `src/assembler/docs-adr-assembler.ts`, `resources/templates/_TEMPLATE-ADR.md` | TASK-3 | No | UT-2 |

---

## TDD Cycle 3: Placeholder Substitution

> **TPP Level 2 (continued):** Unconditional. Project name placeholder is resolved in the generated README.

| Task ID | Type | Description | Files Affected | Depends On | Parallel | TDD Ref |
|---------|------|-------------|----------------|------------|----------|---------|
| TASK-5 | RED | Write test `assemble_validConfig_readmeContainsProjectName`: verify the generated README contains the project name from config (placeholder `{{PROJECT_NAME}}` resolved). | `tests/node/assembler/docs-adr-assembler.test.ts` | TASK-4 | No | UT-3 |
| TASK-6 | GREEN | Implement `engine.replacePlaceholders()` call in `assemble()` to substitute `{{PROJECT_NAME}}` in the generated README content. | `src/assembler/docs-adr-assembler.ts` | TASK-5 | No | UT-3 |

---

## TDD Cycle 4: Return File Paths

> **TPP Level 3:** Scalar -> Collection. Assembler returns the list of generated file paths.

| Task ID | Type | Description | Files Affected | Depends On | Parallel | TDD Ref |
|---------|------|-------------|----------------|------------|----------|---------|
| TASK-7 | RED | Write test `assemble_validConfig_returnsGeneratedFilePaths`: verify `assemble()` returns an array containing the relative path `docs/adr/README.md`. | `tests/node/assembler/docs-adr-assembler.test.ts` | TASK-4 | Yes | UT-4 |
| TASK-8 | GREEN | Update `assemble()` to collect and return the array of generated file paths instead of empty array on success. | `src/assembler/docs-adr-assembler.ts` | TASK-7 | No | UT-4 |

---

## TDD Cycle 5: README Content Validation

> **TPP Level 3 (continued):** Content validation. README contains required table headers and H1 title.

| Task ID | Type | Description | Files Affected | Depends On | Parallel | TDD Ref |
|---------|------|-------------|----------------|------------|----------|---------|
| TASK-9 | RED | Write tests `assemble_validConfig_readmeContainsAdrTableHeaders` and `assemble_validConfig_readmeContainsArchitectureDecisionRecordsTitle`: verify README contains markdown table with ID/Title/Status/Date headers and `# Architecture Decision Records` H1 heading. | `tests/node/assembler/docs-adr-assembler.test.ts` | TASK-4 | Yes | UT-5, UT-6 |
| TASK-10 | GREEN | Update README generation logic to include the `# Architecture Decision Records` heading and the empty ADR index table with headers `| ID | Title | Status | Date |`. | `src/assembler/docs-adr-assembler.ts` | TASK-9 | No | UT-5, UT-6 |

---

## TDD Cycle 6: Template Section Validation

> **TPP Level 4:** Conditional. Assembler validates the template contains all mandatory sections.

| Task ID | Type | Description | Files Affected | Depends On | Parallel | TDD Ref |
|---------|------|-------------|----------------|------------|----------|---------|
| TASK-11 | RED | Write test `assemble_templateWithAllSections_generatesSuccessfully`: verify that when `_TEMPLATE-ADR.md` contains all mandatory sections (Status, Context, Decision, Consequences), the assembler succeeds without warnings. | `tests/node/assembler/docs-adr-assembler.test.ts` | TASK-4 | Yes | UT-7 |
| TASK-12 | GREEN | Implement mandatory section validation in `assemble()`: parse template content and verify presence of `## Status`, `## Context`, `## Decision`, `## Consequences`. Emit warning if any section is missing. | `src/assembler/docs-adr-assembler.ts` | TASK-11 | No | UT-7 |

---

## TDD Cycle 7: Sequential Numbering — Degenerate

> **TPP Level 5:** Iteration (degenerate case). `getNextAdrNumber()` returns 1 for empty directory.

| Task ID | Type | Description | Files Affected | Depends On | Parallel | TDD Ref |
|---------|------|-------------|----------------|------------|----------|---------|
| TASK-13 | RED | Write test `getNextAdrNumber_emptyDirectory_returns1`: verify that given an empty or non-existent directory, `getNextAdrNumber()` returns `1`. | `tests/node/assembler/docs-adr-assembler.test.ts` | -- | Yes | UT-8 |
| TASK-14 | GREEN | Implement `getNextAdrNumber()` function stub that returns `1` when directory is empty or does not exist. | `src/assembler/docs-adr-assembler.ts` | TASK-13 | No | UT-8 |

---

## TDD Cycle 8: Sequential Numbering — Iteration

> **TPP Level 5 (continued):** Iteration. Numbering logic for existing ADRs and gaps.

| Task ID | Type | Description | Files Affected | Depends On | Parallel | TDD Ref |
|---------|------|-------------|----------------|------------|----------|---------|
| TASK-15 | RED | Write tests `getNextAdrNumber_existingAdrs_returnsNextSequential` and `getNextAdrNumber_gapInNumbers_returnsMaxPlusOne`: verify sequential numbering returns max+1 (never fills gaps). Include parametrized test PT-1 with 4 data rows. | `tests/node/assembler/docs-adr-assembler.test.ts` | TASK-14 | No | UT-9, UT-10, PT-1 |
| TASK-16 | GREEN | Implement full `getNextAdrNumber()` logic: scan directory for `ADR-NNNN-*.md` files, parse numbers, return `max + 1`. Handle gaps by always using max, not filling. | `src/assembler/docs-adr-assembler.ts` | TASK-15 | No | UT-9, UT-10, PT-1 |

---

## TDD Cycle 9: Filename Formatting

> **TPP Level 6:** Edge cases. `formatAdrFilename()` kebab-case conversion, special characters, zero-padding.

| Task ID | Type | Description | Files Affected | Depends On | Parallel | TDD Ref |
|---------|------|-------------|----------------|------------|----------|---------|
| TASK-17 | RED | Write tests `formatAdrFilename_simpleTitle_returnsKebabCase`, `formatAdrFilename_specialCharacters_sanitizesTitle`, and `formatAdrFilename_largeNumber_padsCorrectly`. Include parametrized test PT-2 with 5 data rows. | `tests/node/assembler/docs-adr-assembler.test.ts` | -- | Yes | UT-11, UT-12, UT-13, PT-2 |
| TASK-18 | GREEN | Implement `formatAdrFilename()` function: convert title to lowercase kebab-case, remove non-alphanumeric characters (except hyphens), collapse consecutive hyphens, zero-pad number to 4 digits. | `src/assembler/docs-adr-assembler.ts` | TASK-17 | No | UT-11, UT-12, UT-13, PT-2 |

---

## TDD Cycle 10: Directory Creation

> **TPP Level 6 (continued):** Edge case. Assembler creates output directory structure when it does not exist.

| Task ID | Type | Description | Files Affected | Depends On | Parallel | TDD Ref |
|---------|------|-------------|----------------|------------|----------|---------|
| TASK-19 | RED | Write test `assemble_outputDirDoesNotExist_createsDirectoryStructure`: verify that when `outputDir/docs/adr/` does not exist, `assemble()` creates it via `mkdirSync({ recursive: true })`. | `tests/node/assembler/docs-adr-assembler.test.ts` | TASK-4 | Yes | UT-14 |
| TASK-20 | GREEN | Ensure `assemble()` uses `mkdirSync` with `{ recursive: true }` to create the full `docs/adr/` path under `outputDir` (handles backward compatibility with projects lacking `docs/adr/`). | `src/assembler/docs-adr-assembler.ts` | TASK-19 | No | UT-14 |

---

## TDD Cycle 11: Template Content Validation (Static Assertions)

> **TPP Level 2 (bulk):** Validate the static template file contains all required fields and sections.

| Task ID | Type | Description | Files Affected | Depends On | Parallel | TDD Ref |
|---------|------|-------------|----------------|------------|----------|---------|
| TASK-21 | RED | Write template content tests in a separate `describe` block: `template_containsFrontmatterWithStatusField` (UT-15), `template_containsFrontmatterWithDateField` (UT-16), `template_containsFrontmatterWithDecidersField` (UT-17), `template_containsMandatorySection_status` (UT-18), `template_containsMandatorySection_context` (UT-19), `template_containsMandatorySection_decision` (UT-20), `template_containsMandatorySection_consequences` (UT-21), `template_containsOptionalSection_relatedAdrs` (UT-22), `template_containsOptionalSection_storyReference` (UT-23), `template_containsProjectNamePlaceholder` (UT-24). All 10 tests assert content of `resources/templates/_TEMPLATE-ADR.md`. | `tests/node/assembler/docs-adr-assembler.test.ts` | TASK-4 | Yes | UT-15 -- UT-24 |
| TASK-22 | GREEN | Verify `resources/templates/_TEMPLATE-ADR.md` (created in TASK-4) contains all required frontmatter fields (`status`, `date`, `deciders`, `story-ref`), all mandatory sections (`## Status`, `## Context`, `## Decision`, `## Consequences` with Positive/Negative/Neutral subsections), optional sections (`## Related ADRs`, `## Story Reference`), and `{{PROJECT_NAME}}` placeholder. Update template if any field is missing. | `resources/templates/_TEMPLATE-ADR.md` | TASK-21 | No | UT-15 -- UT-24 |

---

## TDD Cycle 12: Pipeline Registration

> **Integration:** Register `DocsAdrAssembler` in the pipeline and validate.

| Task ID | Type | Description | Files Affected | Depends On | Parallel | TDD Ref |
|---------|------|-------------|----------------|------------|----------|---------|
| TASK-23 | RED | Write test `buildAssemblers_includesDocsAdrAssembler`: verify `buildAssemblers()` returns a descriptor with `name: "DocsAdrAssembler"` and `target: "root"`. Update expected assembler count in existing pipeline test (N+1). | `tests/node/assembler/pipeline.test.ts` | TASK-4 | No | IT-1, AT-2 |
| TASK-24 | GREEN | Import `DocsAdrAssembler` in `pipeline.ts` and add descriptor `{ name: "DocsAdrAssembler", target: "root", assembler: new DocsAdrAssembler() }` before `ReadmeAssembler`. Add barrel export in `src/assembler/index.ts`. | `src/assembler/pipeline.ts`, `src/assembler/index.ts` | TASK-23 | No | IT-1, AT-2 |

---

## TDD Cycle 13: Golden File Parity

> **Integration (end-to-end):** Update golden files and validate byte-for-byte parity across all 8 profiles.

| Task ID | Type | Description | Files Affected | Depends On | Parallel | TDD Ref |
|---------|------|-------------|----------------|------------|----------|---------|
| TASK-25 | RED | Run existing `byte-for-byte.test.ts` -- it fails because golden directories lack `docs/adr/README.md`. | (no file changes -- test execution only) | TASK-24 | No | IT-2, AT-1 |
| TASK-26 | GREEN | Generate pipeline output for all 8 profiles and capture `docs/adr/README.md` into each golden directory. Each file contains the profile-specific project name substituted. | `tests/golden/go-gin/docs/adr/README.md`, `tests/golden/java-quarkus/docs/adr/README.md`, `tests/golden/java-spring/docs/adr/README.md`, `tests/golden/kotlin-ktor/docs/adr/README.md`, `tests/golden/python-click-cli/docs/adr/README.md`, `tests/golden/python-fastapi/docs/adr/README.md`, `tests/golden/rust-axum/docs/adr/README.md`, `tests/golden/typescript-nestjs/docs/adr/README.md` | TASK-25 | No | IT-2, AT-1 |

---

## Refactoring Tasks

> **REFACTOR phase:** Applied after all GREEN tasks pass. No new behavior -- structural improvements only.

| Task ID | Type | Description | Files Affected | Depends On | Parallel | TDD Ref |
|---------|------|-------------|----------------|------------|----------|---------|
| TASK-27 | REFACTOR | Extract mandatory section names (`STATUS`, `CONTEXT`, `DECISION`, `CONSEQUENCES`) into named constants at module level in `docs-adr-assembler.ts`. | `src/assembler/docs-adr-assembler.ts` | TASK-22 | Yes | -- |
| TASK-28 | REFACTOR | Extract ADR filename regex pattern (`/^ADR-(\d{4})-.*\.md$/`) and padding width (`4`) into named constants. Consolidate shared kebab-case logic if duplicated. | `src/assembler/docs-adr-assembler.ts` | TASK-18 | Yes | -- |
| TASK-29 | REFACTOR | Review `docs-adr-assembler.ts` for compliance with coding standards: method length <= 25 lines, class length <= 250 lines, parameter count <= 4. Extract helper methods if limits are exceeded. | `src/assembler/docs-adr-assembler.ts` | TASK-26 | No | -- |

---

## Final Verification

| Task ID | Type | Description | Files Affected | Depends On | Parallel | TDD Ref |
|---------|------|-------------|----------------|------------|----------|---------|
| TASK-30 | GREEN | Run full test suite (`npm test`) and verify: all tests pass, line coverage >= 95%, branch coverage >= 90%, zero compiler/linter warnings. | (no file changes -- verification only) | TASK-29 | No | All |

---

## Dependency Graph

```
TASK-1 ──► TASK-2 ──► TASK-3 ──► TASK-4 ──┬──► TASK-5 ──► TASK-6
                                           │
                                           ├──► TASK-7 ──► TASK-8
                                           │
                                           ├──► TASK-9 ──► TASK-10
                                           │
                                           ├──► TASK-11 ──► TASK-12
                                           │
                                           ├──► TASK-19 ──► TASK-20
                                           │
                                           ├──► TASK-21 ──► TASK-22 ──► TASK-27
                                           │
                                           └──► TASK-23 ──► TASK-24 ──► TASK-25 ──► TASK-26

TASK-13 ──► TASK-14 ──► TASK-15 ──► TASK-16        (independent chain)

TASK-17 ──► TASK-18 ──► TASK-28                     (independent chain)

TASK-26 ──► TASK-29 ──► TASK-30                     (final convergence)
```

## Parallelization Summary

**Independent entry points (can start simultaneously):**
- TASK-1 (Cycle 1)
- TASK-13 (Cycle 7)
- TASK-17 (Cycle 9)

**After TASK-4 completes (can run in parallel):**
- TASK-5 (Cycle 3)
- TASK-7 (Cycle 4)
- TASK-9 (Cycle 5)
- TASK-11 (Cycle 6)
- TASK-19 (Cycle 10)
- TASK-21 (Cycle 11)
- TASK-23 (Cycle 12)

**Sequential convergence point:**
- TASK-29 depends on TASK-26 (all golden files updated)
- TASK-30 depends on TASK-29 (final verification)
