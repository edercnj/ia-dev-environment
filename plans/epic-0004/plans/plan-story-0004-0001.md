# Implementation Plan -- story-0004-0001: ADR Template & `docs/adr/` Structure

**Story:** [story-0004-0001](../story-0004-0001.md)
**Date:** 2026-03-15
**Status:** Draft

---

## 1. Affected Layers and Components

This story is scoped to the **assembler layer** (generation pipeline) and the **resources layer** (templates). No domain model, ports, or adapters are affected in the hexagonal sense -- `ia-dev-environment` is a CLI library, not a service with hexagonal layers. The relevant project layers are:

| Layer | Impact | Rationale |
|-------|--------|-----------|
| `resources/templates/` | **New file** | Source of truth for `_TEMPLATE-ADR.md` (RULE-002, RULE-005) |
| `src/assembler/` | **New assembler** | New `DocsAdrAssembler` to generate `docs/adr/README.md` in the pipeline |
| `src/assembler/pipeline.ts` | **Modified** | Register the new assembler in the pipeline descriptor list |
| `tests/node/assembler/` | **New test file** | Unit tests for `DocsAdrAssembler` |
| `tests/golden/*/` | **Modified** | All 8 golden profiles gain a `docs/adr/README.md` file |

---

## 2. New Classes/Interfaces to Create

### 2.1 Template File (static resource)

| File | Location | Description |
|------|----------|-------------|
| `_TEMPLATE-ADR.md` | `resources/templates/_TEMPLATE-ADR.md` | ADR template with YAML frontmatter (status, date, deciders, story-ref) and mandatory sections (Status, Context, Decision, Consequences with Positive/Negative/Neutral subsections, Related ADRs, Story Reference). Contains `{{PROJECT_NAME}}` placeholder. |

Template structure (Nygard format adapted):

```markdown
---
status: Proposed
date: YYYY-MM-DD
deciders:
  - <name>
story-ref: ""
---

# ADR-NNNN: <Title>

## Status

Proposed | <date>

## Context

<Context description>

## Decision

<Decision in assertive tone>

## Consequences

### Positive

- <positive consequence>

### Negative

- <negative consequence>

### Neutral

- <neutral consequence>

## Related ADRs

- None

## Story Reference

- None
```

### 2.2 Assembler Class

| Class | Location | Description |
|-------|----------|-------------|
| `DocsAdrAssembler` | `src/assembler/docs-adr-assembler.ts` | Generates `docs/adr/README.md` as an ADR index. Reads the ADR template from `resources/templates/_TEMPLATE-ADR.md`, validates it contains mandatory sections, and writes the `docs/adr/README.md` index file to the output directory. |

**Interface contract** (follows existing assembler pattern):

```typescript
export class DocsAdrAssembler {
  assemble(
    config: ProjectConfig,
    outputDir: string,
    resourcesDir: string,
    engine: TemplateEngine,
  ): string[];
}
```

The `outputDir` for this assembler will be the **root** output directory (same target as `CodexAgentsMdAssembler`), since `docs/adr/` lives at the project root level, not inside `.claude/` or `.github/`.

### 2.3 ADR Numbering Utility (optional, for story-0004-0015 prep)

Per the story scope, sequential numbering logic needs to be **functional** but the full automation (story-0004-0015) is a separate story. The minimal implementation for this story:

| Function | Location | Description |
|----------|----------|-------------|
| `getNextAdrNumber(adrDir: string): number` | `src/assembler/docs-adr-assembler.ts` | Scans existing `ADR-NNNN-*.md` files in a directory and returns the next sequential number. Returns 1 if directory is empty or doesn't exist. |
| `formatAdrFilename(number: number, title: string): string` | `src/assembler/docs-adr-assembler.ts` | Formats `ADR-NNNN-title-in-kebab-case.md` from a number and title string. |

### 2.4 Test File

| File | Location | Description |
|------|----------|-------------|
| `docs-adr-assembler.test.ts` | `tests/node/assembler/docs-adr-assembler.test.ts` | Unit tests for DocsAdrAssembler: template validation, README generation, numbering logic, backward compatibility |

---

## 3. Existing Classes to Modify

### 3.1 `src/assembler/pipeline.ts`

**Change:** Add `DocsAdrAssembler` to the `buildAssemblers()` function.

- Import `DocsAdrAssembler` from `./docs-adr-assembler.js`
- Add new descriptor entry in the assembler list:
  ```typescript
  { name: "DocsAdrAssembler", target: "root", assembler: new DocsAdrAssembler() },
  ```
- Position: **before** `ReadmeAssembler` (last assembler). The `docs/adr/` generation does not depend on other assemblers and nothing depends on it, but inserting before README keeps the README as the final step (it scans all generated files).
- The `"root"` target maps to the project root `outputDir` (same as `CodexAgentsMdAssembler`), which places `docs/adr/README.md` correctly.

### 3.2 `src/assembler/index.ts`

**Change:** Add barrel export for the new assembler module.

```typescript
// --- STORY-0004-0001: DocsAdrAssembler ---
export * from "./docs-adr-assembler.js";
```

### 3.3 `src/assembler/readme-tables.ts` (minor)

**Change:** Update `buildGenerationSummary()` to include a count for `docs/` artifacts if the summary table needs to reflect docs output. This is optional -- depends on whether the README summary tracks docs-layer artifacts.

### 3.4 Golden Files (all 8 profiles)

**Change:** Add `docs/adr/README.md` golden file to each profile directory:

- `tests/golden/go-gin/docs/adr/README.md`
- `tests/golden/java-quarkus/docs/adr/README.md`
- `tests/golden/java-spring/docs/adr/README.md`
- `tests/golden/kotlin-ktor/docs/adr/README.md`
- `tests/golden/python-click-cli/docs/adr/README.md`
- `tests/golden/python-fastapi/docs/adr/README.md`
- `tests/golden/rust-axum/docs/adr/README.md`
- `tests/golden/typescript-nestjs/docs/adr/README.md`

Each golden file contains the expected `docs/adr/README.md` content with the profile's `project_name` substituted.

---

## 4. Dependency Direction Validation

```
resources/templates/         (static assets, no code dependencies)
        |
        v
src/assembler/docs-adr-assembler.ts  -->  src/models.ts (ProjectConfig)
        |                                  src/template-engine.ts (TemplateEngine)
        |
        v
src/assembler/pipeline.ts   (orchestrator, imports assembler)
```

**Validation checklist:**

- [x] `docs-adr-assembler.ts` depends only on `models.ts` and `template-engine.ts` (same as all other assemblers)
- [x] No circular dependencies introduced -- the new file is a leaf module imported only by `pipeline.ts` and `index.ts`
- [x] No domain layer affected -- this is purely an assembler/infrastructure concern
- [x] Template files in `resources/` have zero code dependencies (static markdown)
- [x] Golden files are test fixtures, not runtime dependencies

---

## 5. Integration Points

### 5.1 Pipeline Integration

The `DocsAdrAssembler` integrates with the existing pipeline via the `buildAssemblers()` function in `pipeline.ts`. It follows the identical contract as all 17 existing assemblers:

- Receives `(config, outputDir, resourcesDir, engine)`
- Returns `string[]` (list of generated file paths)
- Uses synchronous `node:fs` operations (consistent with all assemblers)
- Uses `engine.replacePlaceholders()` for `{{PROJECT_NAME}}` substitution

### 5.2 Template Engine Integration

The ADR README template uses `{{PROJECT_NAME}}` placeholder, which is already handled by `TemplateEngine.replacePlaceholders()`. No new placeholder keys are needed.

### 5.3 Golden File Test Integration

The byte-for-byte parity test (`tests/node/integration/byte-for-byte.test.ts`) automatically covers the new output because it compares the full pipeline output against golden directories. Adding `docs/adr/README.md` to each golden profile directory is sufficient -- no test code changes needed.

### 5.4 Downstream Story Dependencies

- **story-0004-0006** (`x-dev-architecture-plan`): Will consume the ADR template for creating new ADRs.
- **story-0004-0015** (`ADR Automation`): Will extend `getNextAdrNumber()` and add auto-indexing to the README.

---

## 6. Database Changes

**None.** This project has no database (`database: none` in project identity).

---

## 7. API Changes

**None.** This is a CLI tool -- no HTTP/gRPC/GraphQL API. The CLI interface (`ia-dev-env generate`) gains no new flags or commands. The new output directory (`docs/adr/`) is generated automatically as part of the existing `generate` command.

---

## 8. Event Changes

**None.** This project is not event-driven (`event_driven: false` in project identity).

---

## 9. Configuration Changes

### 9.1 No New Configuration Fields

The ADR template generation is unconditional -- every project gets `docs/adr/README.md`. There is no config toggle needed because ADR documentation is a universal concern per RULE-005 and RULE-009.

### 9.2 AssemblerTarget Reuse

The existing `"root"` target (used by `CodexAgentsMdAssembler`) maps the assembler output to the project root directory. The `docs/adr/README.md` path is built relative to this root. No new `AssemblerTarget` values are required.

### 9.3 Dual Copy Consistency (RULE-001)

The `_TEMPLATE-ADR.md` file lives in `resources/templates/` -- this is the **source of truth** (RULE-002). RULE-001 applies to skills, KPs, agents, and templates that exist in both `resources/skills-templates/core/` and `resources/github-skills-templates/`. Since the ADR template is a standalone file in `resources/templates/` (same location as `_TEMPLATE-STORY.md`, `_TEMPLATE-EPIC.md`), dual copy consistency means:

1. The template at `resources/templates/_TEMPLATE-ADR.md` is the source of truth
2. The pipeline copies/transforms it to the output `docs/adr/README.md`
3. No second copy location is needed for this template type (unlike skills/KPs which have `.claude/` + `.github/` copies)

If the story requires the ADR template itself to be emitted into the generated `.claude/` or `.github/` directory (for agent consumption), a `copyTemplateFile()` call in the assembler would handle that. Based on the story description, the primary deliverable is the `docs/adr/` structure generation, not skill/KP template duplication.

---

## 10. Risk Assessment

### 10.1 Low Risk

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Golden file updates break byte-for-byte tests | High (certain) | Low | Run pipeline for each profile, capture output, update golden files. This is the standard process for any new assembler. |
| New assembler introduces PipelineError | Low | Medium | The assembler performs simple file writes. Wrap in try/catch per pipeline convention. |
| Template placeholder `{{PROJECT_NAME}}` not resolved | Very Low | Low | The `TemplateEngine.replacePlaceholders()` is proven across all existing assemblers. Unit test validates substitution. |

### 10.2 No Risk

| Concern | Status |
|---------|--------|
| Backward compatibility | No existing files are modified or removed. `docs/adr/` is purely additive. Projects without `docs/adr/` get it created fresh (Gherkin scenario 6). |
| Performance | One additional `mkdirSync` + `writeFileSync` per pipeline run. Negligible cost. |
| Memory | No new Node.js processes spawned. Single synchronous file write. |
| Breaking changes to CLI interface | None -- no new flags, commands, or required config fields. |
| Dependency on external packages | None -- uses only `node:fs`, `node:path`, and existing project modules. |

### 10.3 Implementation Notes

1. **Template validation:** The assembler should verify that the ADR template at `resources/templates/_TEMPLATE-ADR.md` contains the mandatory sections (`## Status`, `## Context`, `## Decision`, `## Consequences`) before using it. If missing, emit a warning (not an error) for backward compatibility.

2. **README.md content:** The generated `docs/adr/README.md` should contain:
   - `# Architecture Decision Records` header
   - Project name reference
   - Empty ADR table with headers: `| ID | Title | Status | Date |`
   - Instructions for creating new ADRs using the template

3. **File ordering in pipeline:** Insert `DocsAdrAssembler` at position 17 (before `ReadmeAssembler` at position 18, which was previously at position 17). This keeps README generation last, allowing it to potentially count docs artifacts in the summary.

---

## Implementation Order (TDD)

Following TPP (Transformation Priority Premise):

1. **Red:** Write test for `_TEMPLATE-ADR.md` existence and mandatory sections
2. **Green:** Create `resources/templates/_TEMPLATE-ADR.md` with all required sections
3. **Red:** Write test for `DocsAdrAssembler.assemble()` generating `docs/adr/README.md`
4. **Green:** Implement `DocsAdrAssembler` class with README generation
5. **Red:** Write test for `getNextAdrNumber()` with empty dir, existing ADRs
6. **Green:** Implement sequential numbering utility
7. **Red:** Write test for `formatAdrFilename()` kebab-case conversion
8. **Green:** Implement filename formatter
9. **Refactor:** Extract constants, consolidate shared logic
10. **Integration:** Register in pipeline, update golden files, run byte-for-byte tests

---

## File Summary

| Action | Path |
|--------|------|
| CREATE | `resources/templates/_TEMPLATE-ADR.md` |
| CREATE | `src/assembler/docs-adr-assembler.ts` |
| CREATE | `tests/node/assembler/docs-adr-assembler.test.ts` |
| MODIFY | `src/assembler/pipeline.ts` |
| MODIFY | `src/assembler/index.ts` |
| UPDATE | `tests/golden/{all-8-profiles}/docs/adr/README.md` |
