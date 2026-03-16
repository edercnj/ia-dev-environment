# Task Decomposition -- story-0005-0002: Epic Execution Report Template

**Story:** [story-0005-0002](../story-0005-0002.md)
**Plan:** [plan-story-0005-0002](./plan-story-0005-0002.md)
**Date:** 2026-03-16
**Mode:** Layer-Based (no test plan with TPP markers found)

> Warning: No test plan found at `docs/stories/epic-0005/plans/tests-story-0005-0002.md`. Falling back to layer-based decomposition adapted for template + assembler story.

---

## Summary

| TASK | Phase | Depends On | Parallel | Complexity | Tier |
|------|-------|------------|----------|------------|------|
| TASK-1 | RED + GREEN | none | no | S | Junior |
| TASK-2 | RED + GREEN | TASK-1 | no | M | Mid |
| TASK-3 | RED + GREEN | TASK-2 | no | S | Junior |
| TASK-4 | RED + GREEN | TASK-3 | no | M | Mid |
| TASK-5 | REFACTOR | TASK-4 | no | S | Junior |

---

## TASK-1: Create template file with content validation tests (RED -> GREEN)

- **Phase:** RED -> GREEN
- **Tier:** Junior
- **Budget:** S
- **Parallel:** no
- **Depends On:** none

**RED:** Write content validation tests in `tests/node/content/epic-execution-report-content.test.ts`. Tests assert:

1. Template file `_TEMPLATE-EPIC-EXECUTION-REPORT.md` exists in `resources/templates/`
2. Template is not empty
3. Template contains all 8 mandatory sections (parametrized `it.each`):
   - `## Sumario Executivo`
   - `## Timeline de Execucao`
   - `## Status Final por Story`
   - `## Findings Consolidados`
   - `## Coverage Delta`
   - `## Commits e SHAs`
   - `## Issues Nao Resolvidos`
   - `## PR Link`
4. Template contains all 16 placeholders (parametrized `it.each`):
   - `{{EPIC_ID}}`, `{{BRANCH}}`, `{{STARTED_AT}}`, `{{FINISHED_AT}}`
   - `{{STORIES_COMPLETED}}`, `{{STORIES_FAILED}}`, `{{STORIES_BLOCKED}}`, `{{STORIES_TOTAL}}`
   - `{{COMPLETION_PERCENTAGE}}`
   - `{{PHASE_TIMELINE_TABLE}}`, `{{STORY_STATUS_TABLE}}`
   - `{{FINDINGS_SUMMARY}}`
   - `{{COVERAGE_BEFORE}}`, `{{COVERAGE_AFTER}}`, `{{COVERAGE_DELTA}}`
   - `{{COMMIT_LOG}}`, `{{UNRESOLVED_ISSUES}}`, `{{PR_LINK}}`
5. Template follows naming convention `_TEMPLATE-EPIC-EXECUTION-REPORT.md`
6. Valid markdown heading hierarchy: single `# h1`, all `## h2` sections, no heading level skips
7. First heading is `# Epic Execution Report`

**GREEN:** Create `resources/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md` with all 8 sections and 16 placeholders, following the exact structure defined in the implementation plan (section 2.1).

**Layer Components:**
- `resources/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md` (CREATE)
- `tests/node/content/epic-execution-report-content.test.ts` (CREATE)

**Acceptance Criteria covered:**
- Gherkin: "Template contem todas as secoes obrigatorias"
- Gherkin: "Template contem todos os placeholders definidos"
- Gherkin: "Template segue convencao de naming do projeto"
- Gherkin: "Template usa markdown valido"

---

## TASK-2: Create EpicReportAssembler with unit tests (RED -> GREEN)

- **Phase:** RED -> GREEN
- **Tier:** Mid
- **Budget:** M
- **Parallel:** no
- **Depends On:** TASK-1

**RED:** Write unit tests in `tests/node/assembler/epic-report-assembler.test.ts`. Tests assert (following TPP order -- degenerate to complex):

1. **Degenerate:** `assemble_templateMissing_returnsEmptyArray` -- when template file does not exist in `resourcesDir`, returns `[]`
2. **Degenerate:** `assemble_templateMissingSections_returnsEmptyArray` -- when template exists but lacks mandatory sections, returns `[]`
3. **Happy path:** `assemble_validTemplate_copiesToDocsEpicDir` -- template is copied to `{outputDir}/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md`
4. **Happy path:** `assemble_validTemplate_copiesToClaudeTemplatesDir` -- template is copied to `{outputDir}/.claude/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md`
5. **Happy path:** `assemble_validTemplate_copiesToGithubTemplatesDir` -- template is copied to `{outputDir}/.github/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md`
6. **Return value:** `assemble_validTemplate_returnsThreeFilePaths` -- returns array of 3 file paths
7. **Verbatim copy:** `assemble_validTemplate_copiesContentVerbatim` -- output content matches source byte-for-byte (no placeholder resolution)
8. **Directory creation:** `assemble_outputDirDoesNotExist_createsDirectoryStructure` -- creates nested directories recursively
9. **Engine not used:** `assemble_validTemplate_doesNotCallEngineRender` -- the `TemplateEngine` parameter is accepted but not used (verbatim copy design)
10. **Section validation:** `assemble_templateWithAllSections_validatesSuccessfully` -- validates all 8 mandatory section headings before copying

**GREEN:** Create `src/assembler/epic-report-assembler.ts` implementing `EpicReportAssembler` class:

- Method signature: `assemble(config: ProjectConfig, outputDir: string, resourcesDir: string, engine: TemplateEngine): string[]`
- Read template from `{resourcesDir}/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md`
- Validate template contains all 8 mandatory section headings
- Return `[]` if template is missing or invalid (graceful no-op, matching `DocsAdrAssembler` pattern)
- Copy verbatim (raw `fs.readFileSync` + `fs.writeFileSync`, NO `engine.renderTemplate()`) to 3 destinations:
  - `{outputDir}/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md`
  - `{outputDir}/.claude/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md`
  - `{outputDir}/.github/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md`
- Return array of 3 absolute file paths on success
- Use `fs.mkdirSync({ recursive: true })` for directory creation

**Layer Components:**
- `src/assembler/epic-report-assembler.ts` (CREATE)
- `tests/node/assembler/epic-report-assembler.test.ts` (CREATE)

**Design Notes:**
- Follow `DocsAdrAssembler` pattern (same `_engine` unused parameter, same graceful no-op on missing/invalid template)
- Template uses `{{PLACEHOLDER}}` tokens but these are NOT build-time tokens -- they are runtime tokens for story-0005-0011. MUST NOT route through `TemplateEngine`.

---

## TASK-3: Register assembler in pipeline and barrel export (RED -> GREEN)

- **Phase:** RED -> GREEN
- **Tier:** Junior
- **Budget:** S
- **Parallel:** no
- **Depends On:** TASK-2

**RED:** Write/extend tests verifying the new assembler is registered:

1. In `tests/node/assembler/pipeline.test.ts` (if tests exist for `buildAssemblers()`): verify `EpicReportAssembler` appears in the assembler list
2. Verify the assembler count in `buildAssemblers()` is now correct (current 22 -> 23)
3. Verify the assembler target is `"root"` (since it writes to multiple subdirectories under root)
4. Verify the assembler is positioned before `ReadmeAssembler` (README must run last to count all artifacts)

**GREEN:** Modify two files:

1. `src/assembler/pipeline.ts`:
   - Add import: `import { EpicReportAssembler } from "./epic-report-assembler.js";`
   - Add descriptor in `buildAssemblers()` BEFORE `ReadmeAssembler` and AFTER `CicdAssembler`:
     ```typescript
     { name: "EpicReportAssembler", target: "root", assembler: new EpicReportAssembler() },
     ```
   - Update JSDoc comment from "22 assemblers" to "23 assemblers"

2. `src/assembler/index.ts`:
   - Add barrel export:
     ```typescript
     // --- STORY-0005-0002: EpicReportAssembler ---
     export * from "./epic-report-assembler.js";
     ```

**Layer Components:**
- `src/assembler/pipeline.ts` (MODIFY)
- `src/assembler/index.ts` (MODIFY)
- `tests/node/assembler/pipeline.test.ts` (MODIFY, if pipeline registration tests exist)

---

## TASK-4: Update golden files for all 8 profiles (RED -> GREEN)

- **Phase:** RED -> GREEN
- **Tier:** Mid
- **Budget:** M
- **Parallel:** no
- **Depends On:** TASK-3

**RED:** Run existing byte-for-byte golden file tests (`tests/node/integration/byte-for-byte.test.ts`). They will FAIL because the pipeline now generates 3 new files per profile (24 total) plus updated `.claude/README.md` (generation summary count change) that are not in the golden directories.

**GREEN:** Regenerate golden files for all 8 profiles:

1. Run the generation pipeline for each profile:
   - `go-gin`
   - `java-quarkus`
   - `java-spring`
   - `kotlin-ktor`
   - `python-click-cli`
   - `python-fastapi`
   - `rust-axum`
   - `typescript-nestjs`

2. For each profile, 3 new golden files are added (all identical to the source template, since the assembler copies verbatim without profile-specific substitution):
   - `tests/golden/{profile}/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md`
   - `tests/golden/{profile}/.claude/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md`
   - `tests/golden/{profile}/.github/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md`

3. Each profile's `.claude/README.md` golden file will also change (updated generation summary count from `ReadmeAssembler`).

4. Run byte-for-byte tests again to confirm all pass.

**Layer Components:**
- `tests/golden/{all-8-profiles}/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md` (CREATE -- 8 files)
- `tests/golden/{all-8-profiles}/.claude/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md` (CREATE -- 8 files)
- `tests/golden/{all-8-profiles}/.github/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md` (CREATE -- 8 files)
- `tests/golden/{all-8-profiles}/.claude/README.md` (UPDATE -- 8 files)

**Note:** All 24 new golden files are byte-for-byte identical to the source template since `EpicReportAssembler` copies verbatim. No test code changes needed in `byte-for-byte.test.ts` -- it automatically picks up new files in the golden directories.

---

## TASK-5: Refactor and final verification (REFACTOR)

- **Phase:** REFACTOR
- **Tier:** Junior
- **Budget:** S
- **Parallel:** no
- **Depends On:** TASK-4

**REFACTOR:** Review all code produced in TASK-1 through TASK-4:

1. **Naming consistency:** Verify all constants, variables, and method names follow project conventions (intent-revealing names, verbs for methods, nouns for types)
2. **Hard limits:** Verify method length <= 25 lines, class length <= 250 lines, line width <= 120 characters
3. **Extract constants:** Ensure template filename, section headings, and output subdirectories are extracted to named constants (no magic strings)
4. **Duplication:** Check for any duplicated section heading arrays between `epic-report-assembler.ts` and `epic-execution-report-content.test.ts` -- extract to shared constant if appropriate
5. **Compiler/linter:** Run `npx tsc --noEmit` and ensure zero warnings
6. **Coverage:** Run `npx vitest run --coverage` and verify >= 95% line coverage, >= 90% branch coverage for all new files
7. **Full test suite:** Run complete test suite and verify all 1384+ tests pass (including the new ones)
8. **DoD checklist:**
   - [ ] Template `_TEMPLATE-EPIC-EXECUTION-REPORT.md` in `resources/templates/`
   - [ ] All 8 sections and 16 placeholders present
   - [ ] Template registered in pipeline (dual copy to 3 destinations)
   - [ ] Golden file tests passing for all 8 profiles
   - [ ] Coverage >= 95% line, >= 90% branch
   - [ ] Zero compiler/linter warnings
   - [ ] Test-first pattern visible in commit history

**Layer Components:**
- All files from TASK-1 through TASK-4 (REVIEW, potential minor edits)

---

## File Impact Summary

| Action | Path | Task |
|--------|------|------|
| CREATE | `resources/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md` | TASK-1 |
| CREATE | `tests/node/content/epic-execution-report-content.test.ts` | TASK-1 |
| CREATE | `src/assembler/epic-report-assembler.ts` | TASK-2 |
| CREATE | `tests/node/assembler/epic-report-assembler.test.ts` | TASK-2 |
| MODIFY | `src/assembler/pipeline.ts` | TASK-3 |
| MODIFY | `src/assembler/index.ts` | TASK-3 |
| MODIFY | `tests/node/assembler/pipeline.test.ts` (if applicable) | TASK-3 |
| CREATE | `tests/golden/{8-profiles}/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md` | TASK-4 |
| CREATE | `tests/golden/{8-profiles}/.claude/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md` | TASK-4 |
| CREATE | `tests/golden/{8-profiles}/.github/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md` | TASK-4 |
| UPDATE | `tests/golden/{8-profiles}/.claude/README.md` | TASK-4 |

**Total new files:** 4 source/test + 24 golden files = 28
**Total modified files:** 2-3 source + 8 golden READMEs = 10-11

---

## Dependency Graph

```
TASK-1 (Template + Content Tests)
  |
  v
TASK-2 (Assembler + Unit Tests)
  |
  v
TASK-3 (Pipeline Registration)
  |
  v
TASK-4 (Golden File Regeneration)
  |
  v
TASK-5 (Refactor + Final Verification)
```

All tasks are strictly sequential. No parallelism is possible because each task depends on the output of the previous one.

---

## Review Tier Assignment

| Engineer | Relevant Tasks | Tier |
|----------|---------------|------|
| QA | TASK-1, TASK-2, TASK-4, TASK-5 | Mid |
| DevOps | TASK-3 | Junior |
| Tech Lead | ALL | Mid (story max) |
