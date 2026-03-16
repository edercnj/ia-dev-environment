# Implementation Plan -- story-0005-0002: Epic Execution Report Template

**Story:** [story-0005-0002](../story-0005-0002.md)
**Date:** 2026-03-16
**Status:** Draft

---

## 1. Affected Layers and Components

This story is scoped to the **assembler layer** (generation pipeline), the **resources layer** (template file), and **tests** (unit + golden files). `ia-dev-environment` is a CLI library, so the hexagonal domain/port/adapter layers do not apply. The relevant project layers are:

| Layer | Impact | Rationale |
|-------|--------|-----------|
| `resources/templates/` | **New file** | Source of truth for `_TEMPLATE-EPIC-EXECUTION-REPORT.md` |
| `src/assembler/` | **New assembler** | New `EpicReportAssembler` copies the template to output under `docs/epic/` and dual-copies to `.claude/templates/` and `.github/templates/` |
| `src/assembler/pipeline.ts` | **Modified** | Register the new assembler in `buildAssemblers()` |
| `src/assembler/index.ts` | **Modified** | Add barrel export for the new assembler module |
| `tests/node/assembler/` | **New test file** | Unit tests for `EpicReportAssembler` |
| `tests/node/content/` | **New test file** | Template structure validation (sections + placeholders) |
| `tests/golden/*/` | **Modified** | All 8 golden profiles gain the output files from the new assembler |

---

## 2. New Classes/Interfaces to Create

### 2.1 Template File (static resource)

| File | Location | Description |
|------|----------|-------------|
| `_TEMPLATE-EPIC-EXECUTION-REPORT.md` | `resources/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md` | Epic execution report template with all 8 mandatory sections and `{{PLACEHOLDER}}` tokens for runtime resolution |

**Template structure:**

```markdown
# Epic Execution Report -- {{EPIC_ID}}

> Branch: `{{BRANCH}}`
> Started: {{STARTED_AT}} | Finished: {{FINISHED_AT}}

## Sumario Executivo

| Metric | Value |
|--------|-------|
| Stories Completed | {{STORIES_COMPLETED}} |
| Stories Failed | {{STORIES_FAILED}} |
| Stories Blocked | {{STORIES_BLOCKED}} |
| Stories Total | {{STORIES_TOTAL}} |
| Completion | {{COMPLETION_PERCENTAGE}} |

## Timeline de Execucao

{{PHASE_TIMELINE_TABLE}}

## Status Final por Story

{{STORY_STATUS_TABLE}}

## Findings Consolidados

{{FINDINGS_SUMMARY}}

## Coverage Delta

| Metric | Before | After | Delta |
|--------|--------|-------|-------|
| Line Coverage | {{COVERAGE_BEFORE}} | {{COVERAGE_AFTER}} | {{COVERAGE_DELTA}} |

## Commits e SHAs

{{COMMIT_LOG}}

## Issues Nao Resolvidos

{{UNRESOLVED_ISSUES}}

## PR Link

{{PR_LINK}}
```

All 18 placeholders from the implementation plan (section 2.1) are present:
- `{{EPIC_ID}}`, `{{BRANCH}}`, `{{STARTED_AT}}`, `{{FINISHED_AT}}`
- `{{STORIES_COMPLETED}}`, `{{STORIES_FAILED}}`, `{{STORIES_BLOCKED}}`, `{{STORIES_TOTAL}}`
- `{{COMPLETION_PERCENTAGE}}`
- `{{PHASE_TIMELINE_TABLE}}`, `{{STORY_STATUS_TABLE}}`
- `{{FINDINGS_SUMMARY}}`
- `{{COVERAGE_BEFORE}}`, `{{COVERAGE_AFTER}}`, `{{COVERAGE_DELTA}}`
- `{{COMMIT_LOG}}`, `{{UNRESOLVED_ISSUES}}`, `{{PR_LINK}}`

**Note:** These `{{PLACEHOLDER}}` tokens use the same double-brace convention as other project templates but are NOT resolved by `TemplateEngine` (they are for runtime resolution by story-0005-0011's consolidation subagent). The assembler must copy the template **verbatim** (no placeholder substitution), unlike `DocsAssembler` or `RunbookAssembler` which render through the template engine.

### 2.2 Assembler Class

| Class | Location | Description |
|-------|----------|-------------|
| `EpicReportAssembler` | `src/assembler/epic-report-assembler.ts` | Copies `_TEMPLATE-EPIC-EXECUTION-REPORT.md` from `resources/templates/` to three output locations: `docs/epic/`, `.claude/templates/`, and `.github/templates/` |

**Interface contract** (follows existing assembler pattern):

```typescript
export class EpicReportAssembler {
  assemble(
    config: ProjectConfig,
    outputDir: string,
    resourcesDir: string,
    engine: TemplateEngine,
  ): string[];
}
```

**Key design decisions:**

1. **Target: `"root"`** -- The assembler writes to the root output directory (same as `DocsAdrAssembler`, `RunbookAssembler`). This allows it to write to three subdirectories: `docs/epic/`, `.claude/templates/`, and `.github/templates/`.

2. **Verbatim copy, not render** -- Unlike `DocsAssembler` and `RunbookAssembler` which call `engine.renderTemplate()`, this assembler uses `fs.readFileSync()` + `fs.writeFileSync()` to copy the template without placeholder substitution. The `{{PLACEHOLDER}}` tokens are intended for downstream runtime resolution by the consolidation subagent (story-0005-0011), not for build-time resolution.

3. **Section validation** -- Following the `DocsAdrAssembler` pattern, the assembler validates the template contains all 8 mandatory sections before copying. Returns `[]` (graceful no-op) if the source template is missing or incomplete.

4. **Dual copy** -- The story DoD specifies "dual copy: `resources/` + `.claude/` + `.github/`". In the pipeline context, `resources/` is the source; the assembler writes to all three output destinations from the single source.

**Assembler behavior:**

```
resources/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md  (source, read-only)
    |
    +--> {outputDir}/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md
    +--> {outputDir}/.claude/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md
    +--> {outputDir}/.github/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md
```

Returns an array of 3 file paths on success, or `[]` if template is missing or invalid.

### 2.3 Test Files

| File | Location | Description |
|------|----------|-------------|
| `epic-report-assembler.test.ts` | `tests/node/assembler/epic-report-assembler.test.ts` | Unit tests for `EpicReportAssembler`: degenerate cases, happy path, file paths, section validation, dual copy |
| `epic-execution-report-content.test.ts` | `tests/node/content/epic-execution-report-content.test.ts` | Template structure validation: all 8 sections present, all 18 placeholders present, valid markdown hierarchy, naming convention |

---

## 3. Existing Classes to Modify

### 3.1 `src/assembler/pipeline.ts`

**Change:** Add `EpicReportAssembler` to `buildAssemblers()`.

- Import `EpicReportAssembler` from `./epic-report-assembler.js`
- Add new descriptor entry:
  ```typescript
  { name: "EpicReportAssembler", target: "root", assembler: new EpicReportAssembler() },
  ```
- Position: **before** `ReadmeAssembler` (last assembler) and after `CicdAssembler`. Keeps README generation last so it can count all generated artifacts.
- Update the JSDoc comment "22 assemblers" to "23 assemblers" (or whatever the current count is).

### 3.2 `src/assembler/index.ts`

**Change:** Add barrel export for the new module.

```typescript
// --- STORY-0005-0002: EpicReportAssembler ---
export * from "./epic-report-assembler.js";
```

### 3.3 Golden Files (all 8 profiles)

**Change:** Add 3 new golden files per profile (24 total across 8 profiles):

For each profile in `{go-gin, java-quarkus, java-spring, kotlin-ktor, python-click-cli, python-fastapi, rust-axum, typescript-nestjs}`:

- `tests/golden/{profile}/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md`
- `tests/golden/{profile}/.claude/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md`
- `tests/golden/{profile}/.github/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md`

All 24 golden files will be **identical** to each other (and to the source template) because the assembler copies verbatim without any profile-specific placeholder substitution.

### 3.4 `CLAUDE.md` (root)

**Change:** The `CLAUDE.md` is auto-generated by the `ReadmeAssembler`. Since `ReadmeAssembler` runs last and dynamically counts artifacts, the generation summary table will automatically reflect the new files. No manual edit is needed to `CLAUDE.md`. However, the golden file for `.claude/README.md` in each profile will change (updated generation summary count), requiring golden file regeneration.

---

## 4. Dependency Direction Validation

```
resources/templates/                      (static asset, no code deps)
        |
        v
src/assembler/epic-report-assembler.ts --> src/models.ts (ProjectConfig type)
        |                                   src/template-engine.ts (TemplateEngine type, unused but API-required)
        |
        v
src/assembler/pipeline.ts                (orchestrator, imports assembler)
```

**Validation checklist:**

- [x] `epic-report-assembler.ts` depends only on `node:fs`, `node:path`, `models.ts`, and `template-engine.ts` (standard assembler deps)
- [x] No circular dependencies -- new file is a leaf module imported only by `pipeline.ts` and `index.ts`
- [x] No domain layer affected -- purely assembler/infrastructure concern
- [x] Template file in `resources/` has zero code dependencies (static markdown)
- [x] Golden files are test fixtures, not runtime dependencies

---

## 5. Integration Points

### 5.1 Pipeline Integration

The `EpicReportAssembler` follows the identical contract as all existing assemblers:

- Receives `(config, outputDir, resourcesDir, engine)`
- Returns `string[]` (list of generated file paths)
- Uses synchronous `node:fs` operations (consistent with all assemblers)
- Graceful no-op when template is missing (returns `[]`)

The `engine` parameter is accepted for API uniformity but is **not used** -- the template is copied verbatim. This follows the same pattern as `DocsAdrAssembler` which also accepts but does not use the engine for template content.

### 5.2 Golden File Test Integration

The byte-for-byte parity test (`tests/node/integration/byte-for-byte.test.ts`) automatically covers the new output because it compares the full pipeline output against golden directories. Adding the 3 new files to each golden profile directory is sufficient -- no test code changes needed.

### 5.3 Downstream Story Dependencies

- **story-0005-0011** (Consolidation Final): Will read the `_TEMPLATE-EPIC-EXECUTION-REPORT.md` from the generated output and resolve placeholders at runtime to produce the final `epic-execution-report.md`.
- The template serves as the **format contract** between this story and story-0005-0011.

---

## 6. Database Changes

**None.** This project has no database (`database: none` in project identity).

---

## 7. API Changes

**None.** This is a CLI tool -- no HTTP/gRPC/GraphQL API. The CLI interface (`ia-dev-env generate`) gains no new flags or commands. The new output files are generated automatically as part of the existing `generate` command.

---

## 8. Event Changes

**None.** This project is not event-driven (`event_driven: false` in project identity).

---

## 9. Configuration Changes

### 9.1 No New Configuration Fields

The epic execution report template generation is **unconditional** -- every project gets the template files. There is no config toggle needed because this template supports the epic orchestration lifecycle which is universal to all projects using `ia-dev-environment`.

### 9.2 AssemblerTarget Reuse

The existing `"root"` target maps the assembler output to the project root directory. The assembler internally writes to `docs/epic/`, `.claude/templates/`, and `.github/templates/` relative to this root. No new `AssemblerTarget` values are required.

### 9.3 Pipeline Count Update

The `buildAssemblers()` function comment will update from the current assembler count to count+1. The `AssemblerDescriptor` type and `executeAssemblers()` function require no changes.

---

## 10. Risk Assessment

### 10.1 Low Risk

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Golden file updates break byte-for-byte tests | High (certain) | Low | Standard process: run pipeline for each profile, capture output, update golden files. The `ReadmeAssembler` golden files will also change (updated artifact count in generation summary). |
| Template placeholders conflict with `TemplateEngine` | Medium | Low | The assembler copies **verbatim** (no `engine.renderTemplate()` or `engine.replacePlaceholders()`). This avoids `TemplateEngine` attempting to resolve `{{EPIC_ID}}` etc. as build-time placeholders. |
| `.claude/templates/` and `.github/templates/` directories are new output paths | Medium | Low | These are purely additive directories. No existing files or directories are affected. The pipeline creates directories recursively via `fs.mkdirSync({recursive: true})`. |

### 10.2 No Risk

| Concern | Status |
|---------|--------|
| Backward compatibility | No existing files are modified or removed. All 3 output paths are purely additive. |
| Performance | Three additional `mkdirSync` + `writeFileSync` calls per pipeline run. Negligible cost. |
| Memory | No new Node.js processes spawned. Single synchronous file read + three writes. |
| Breaking changes to CLI interface | None -- no new flags, commands, or required config fields. |
| Dependency on external packages | None -- uses only `node:fs`, `node:path`, and existing project modules. |

### 10.3 Implementation Notes

1. **Verbatim copy is critical.** The `{{PLACEHOLDER}}` tokens in this template are runtime tokens for story-0005-0011, not build-time tokens. If the assembler routes through `TemplateEngine`, the Nunjucks engine will either fail (unknown variable) or strip the placeholders. The assembler MUST use raw `fs.readFileSync()` + `fs.writeFileSync()`.

2. **Section validation.** The assembler should verify the template contains all 8 mandatory section headings before copying:
   - `## Sumario Executivo` (or equivalent English heading)
   - `## Timeline de Execucao`
   - `## Status Final por Story`
   - `## Findings Consolidados`
   - `## Coverage Delta`
   - `## Commits e SHAs`
   - `## Issues Nao Resolvidos`
   - `## PR Link`

   If any section is missing, return `[]` (graceful no-op, matching `DocsAdrAssembler` precedent).

3. **Placeholder validation.** A content test should verify all 18 placeholders are present in the template. This is a test-only concern, not an assembler concern.

4. **Markdown validity.** The template should follow strict heading hierarchy: single `# h1` followed by `## h2` sections. No heading level skips.

---

## Implementation Order (TDD)

Following TPP (Transformation Priority Premise):

1. **Red:** Write content test: `_TEMPLATE-EPIC-EXECUTION-REPORT.md` exists in `resources/templates/`
2. **Green:** Create the template file with all sections and placeholders
3. **Red:** Write content test: template contains all 8 mandatory sections
4. **Green:** (Already satisfied by step 2)
5. **Red:** Write content test: template contains all 18 placeholders
6. **Green:** (Already satisfied by step 2)
7. **Red:** Write content test: valid markdown heading hierarchy
8. **Green:** (Already satisfied by step 2)
9. **Red:** Write unit test: `EpicReportAssembler.assemble()` returns `[]` when template is missing
10. **Green:** Create `EpicReportAssembler` class with missing-template guard
11. **Red:** Write unit test: assembler copies template to `docs/epic/`
12. **Green:** Implement single-target copy
13. **Red:** Write unit test: assembler copies to `.claude/templates/` and `.github/templates/`
14. **Green:** Implement dual copy (three targets total)
15. **Red:** Write unit test: assembler returns 3 file paths
16. **Green:** (Already satisfied by step 14)
17. **Red:** Write unit test: template content is copied verbatim (no placeholder resolution)
18. **Green:** (Already satisfied by design -- using `fs.readFileSync`/`writeFileSync`)
19. **Refactor:** Extract constants, consolidate validation logic
20. **Integration:** Register in `pipeline.ts`, export from `index.ts`, regenerate golden files, run byte-for-byte tests

---

## File Summary

| Action | Path |
|--------|------|
| CREATE | `resources/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md` |
| CREATE | `src/assembler/epic-report-assembler.ts` |
| CREATE | `tests/node/assembler/epic-report-assembler.test.ts` |
| CREATE | `tests/node/content/epic-execution-report-content.test.ts` |
| MODIFY | `src/assembler/pipeline.ts` |
| MODIFY | `src/assembler/index.ts` |
| UPDATE | `tests/golden/{all-8-profiles}/docs/epic/_TEMPLATE-EPIC-EXECUTION-REPORT.md` |
| UPDATE | `tests/golden/{all-8-profiles}/.claude/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md` |
| UPDATE | `tests/golden/{all-8-profiles}/.github/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md` |
| UPDATE | `tests/golden/{all-8-profiles}/.claude/README.md` (generation summary count change) |
