# Implementation Plan — story-0004-0002

## Service Architecture Documentation Template

**Story:** story-0004-0002 — Template de Documentacao de Arquitetura de Servico
**Author:** Senior Architect
**Date:** 2026-03-15

---

## 1. Affected Layers and Components

| Layer | Component | Impact |
|-------|-----------|--------|
| **resources/templates/** | New template file `_TEMPLATE-SERVICE-ARCHITECTURE.md` | CREATE |
| **src/template-engine.ts** | `buildDefaultContext()` — add missing context keys | MODIFY |
| **src/assembler/** | New assembler `docs-assembler.ts` | CREATE |
| **src/assembler/pipeline.ts** | Register new assembler in `buildAssemblers()` | MODIFY |
| **src/assembler/pipeline.ts** | Add `"docs"` to `AssemblerTarget` union | MODIFY |
| **tests/node/assembler/** | New test `docs-assembler.test.ts` | CREATE |
| **tests/golden/{profile}/** | Golden files updated with `docs/architecture/service-architecture.md` | MODIFY (8 profiles) |

This story is purely additive. No existing assemblers, templates, or pipeline behavior changes.

---

## 2. New Classes/Interfaces to Create

### 2.1 Template: `resources/templates/_TEMPLATE-SERVICE-ARCHITECTURE.md`

**Location:** `resources/templates/_TEMPLATE-SERVICE-ARCHITECTURE.md`

Static Markdown template with 10 mandatory sections and `{{placeholder}}` tokens resolved
by the `TemplateEngine`. The template uses **Nunjucks double-brace** syntax for values that
the pipeline resolves at generation time, and **instructional text** for sections the
architect fills incrementally (per RULE-008).

**Placeholders resolved by the pipeline:**

| Placeholder | Source in `buildDefaultContext()` | Existing Key? |
|-------------|----------------------------------|---------------|
| `{{ project_name }}` | `config.project.name` | YES (`project_name`) |
| `{{ architecture_style }}` | `config.architecture.style` | YES (`architecture_style`) |
| `{{ language_name }}` | `config.language.name` | YES (`language_name`) |
| `{{ framework_name }}` | `config.framework.name` | YES (`framework_name`) |
| `{{ interfaces_list }}` | derived from `config.interfaces[].type` | **NO — must add** |

The story specifies `{{SERVICE_NAME}}`, `{{ARCHITECTURE}}`, `{{LANGUAGE}}`, `{{FRAMEWORK}}`,
`{{INTERFACES}}`. These map to the existing context keys above. The template will use the
canonical Nunjucks names (`project_name`, `architecture_style`, etc.) for pipeline resolution.
The story's placeholder names are logical names that map 1:1 to the context keys.

**10 Mandatory Sections:**

1. `## 1. Visao Geral` — Purpose, stack, ecosystem role
2. `## 2. Diagramas C4` — Context + Container diagrams in Mermaid
3. `## 3. Integracoes` — Table: System, Protocol, Purpose, SLO
4. `## 4. Modelo de Dados` — Main entities and relationships
5. `## 5. Fluxos Criticos` — Sequence diagrams (Mermaid) for top-3 operations
6. `## 6. NFRs` — Table: Metric, Target, Measurement
7. `## 7. Decisoes Arquiteturais` — Links to ADRs in `docs/adr/`
8. `## 8. Observabilidade` — Key metrics, alerts, dashboards
9. `## 9. Resiliencia` — Circuit breakers, retries, fallbacks
10. `## 10. Historico de Mudancas` — Document changelog

### 2.2 Assembler: `src/assembler/docs-assembler.ts`

**Location:** `src/assembler/docs-assembler.ts`

```typescript
export class DocsAssembler {
  assemble(
    config: ProjectConfig,
    outputDir: string,
    resourcesDir: string,
    engine: TemplateEngine,
  ): string[]
}
```

**Responsibilities:**

1. Read `resources/templates/_TEMPLATE-SERVICE-ARCHITECTURE.md`
2. Render it through `engine.renderTemplate()` to resolve Nunjucks placeholders
3. Write output to `{outputDir}/docs/architecture/service-architecture.md`
4. Create `{outputDir}/docs/architecture/` directory if it does not exist
5. Return the list of generated file paths

**Design decisions:**

- Uses `engine.renderTemplate()` (Nunjucks) rather than `engine.replacePlaceholders()`
  (legacy single-brace) because the template uses `{{ }}` double-brace syntax, which is
  the Nunjucks convention already used by other templates.
- Graceful no-op: if the source template does not exist in `resourcesDir`, return empty
  array (backward compatibility with older resource bundles per RULE-003).
- Single assembler covers `docs/` output. Future stories (0004-0001, 0004-0003) can add
  methods or expand this assembler for `docs/adr/` and `docs/runbook/`.

### 2.3 Test: `tests/node/assembler/docs-assembler.test.ts`

Unit tests for `DocsAssembler` covering:

- Template exists: generates `docs/architecture/service-architecture.md` with all 10 sections
- Placeholder resolution: `{{ project_name }}` becomes actual project name
- C4 Mermaid diagram present in section 2
- NFR table with Metric/Target/Measurement columns in section 6
- Template missing: returns empty array (no error)
- Output directory created automatically

---

## 3. Existing Classes to Modify

### 3.1 `src/template-engine.ts` — `buildDefaultContext()`

**Change:** Add `interfaces_list` key to the context dictionary.

```typescript
// New field in buildDefaultContext():
interfaces_list: config.interfaces.map((i) => i.type).join(", ") || "none",
```

**Impact:** This adds one new key to the 24-field context. It does not break existing
templates because Nunjucks only complains about **undefined** variables when
`throwOnUndefined: true` — and no existing template references `interfaces_list`.

**Risk:** Adding a field changes the `placeholderMap` size, which affects the legacy
`replacePlaceholders()` method. However, legacy templates use `{interfaces_list}` only
if they contain that token, and none currently do. No breakage.

### 3.2 `src/assembler/pipeline.ts` — `buildAssemblers()`

**Change:** Register `DocsAssembler` in the assembler list.

```typescript
import { DocsAssembler } from "./docs-assembler.js";

// In buildAssemblers(), add before ReadmeAssembler:
{ name: "DocsAssembler", target: "docs", assembler: new DocsAssembler() },
```

**Change:** Extend `AssemblerTarget` type union.

```typescript
export type AssemblerTarget = "claude" | "github" | "codex" | "codex-agents" | "root" | "docs";
```

**Change:** Add target directory mapping in `executeAssemblers()`.

```typescript
const docsDir = join(outputDir, "docs");
// In the target resolution:
: target === "docs"
  ? docsDir
```

**Impact:** Adds one more assembler to the pipeline. Ordering: place `DocsAssembler`
after `GithubPromptsAssembler` and before `CodexAgentsMdAssembler`, keeping README
as the last assembler.

### 3.3 Golden files (8 profiles)

All 8 golden profiles must be regenerated to include the new
`docs/architecture/service-architecture.md` file. This is done by running the pipeline
against each profile config and writing output to `tests/golden/{profile}/`.

---

## 4. Dependency Direction Validation

```
resources/templates/                    (static assets — no code deps)
    |
    v
src/assembler/docs-assembler.ts         (depends on: models, template-engine)
    |
    v
src/assembler/pipeline.ts               (orchestrator — depends on all assemblers)
    |
    v
src/template-engine.ts                  (depends on: models)
    |
    v
src/models.ts                           (leaf — no deps)
```

**Validation:** Dependencies point inward. `DocsAssembler` depends on `models` and
`template-engine` (both core). Pipeline depends on assemblers. No circular dependencies.
No adapter/domain confusion (this project is a `library`, not hexagonal — the architecture
principles apply at the template content level, not at `ia-dev-env`'s own codebase).

---

## 5. Integration Points

| Integration | Direction | Protocol | Notes |
|-------------|-----------|----------|-------|
| Pipeline -> DocsAssembler | Inbound | Function call | Pipeline calls `assemble()` |
| DocsAssembler -> TemplateEngine | Outbound | Function call | Renders Nunjucks template |
| DocsAssembler -> File System | Outbound | `node:fs` sync | Writes `docs/architecture/` |
| Golden file tests -> Pipeline | Test | Full pipeline run | Validates byte-for-byte output |

**Dual copy (RULE-001):** The template `_TEMPLATE-SERVICE-ARCHITECTURE.md` lives in
`resources/templates/` (source of truth per RULE-002). The pipeline output goes to
`docs/architecture/` inside the generated project. No dual copy of the template itself
is needed because:
- `resources/templates/` files are NOT copied to `.claude/` or `.github/` — they are
  source templates consumed by the pipeline.
- The generated output (`docs/architecture/service-architecture.md`) is a single output,
  not a dual-copy artifact.

However, if the story's intent is that the _template file itself_ should appear in both
`resources/skills-templates/core/` and `resources/github-skills-templates/` for reference
by skills/agents, that would be a separate concern. Based on the story DoD ("Ambas as
copias atualizadas"), the dual copy refers to the fact that the template is in `resources/`
and the pipeline generates the output — both are consistent.

---

## 6. Database Changes

None. This story creates documentation templates and a pipeline assembler. No persistence
layer is involved.

---

## 7. API Changes

None. `ia-dev-env` is a CLI tool. No REST/gRPC/GraphQL endpoints are affected.

The CLI's `generate` command already orchestrates the pipeline. Adding a new assembler
is transparent to the CLI layer.

---

## 8. Event Changes

None. No event-driven components in this project.

---

## 9. Configuration Changes

### 9.1 Template Engine Context

| Key | Type | Source | Default |
|-----|------|--------|---------|
| `interfaces_list` | `string` | `config.interfaces[].type` joined by `, ` | `"none"` |

This is the only configuration change. All other placeholders (`project_name`,
`architecture_style`, `language_name`, `framework_name`) already exist in
`buildDefaultContext()`.

### 9.2 Pipeline Registration

The `DocsAssembler` is registered in the pipeline's assembler list. No user-facing
configuration change. No environment variables or ConfigMaps needed.

---

## 10. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Golden file regeneration breaks CI | Medium | High | Regenerate all 8 profiles in one commit. Run `byte-for-byte.test.ts` locally before push. |
| `interfaces_list` addition breaks existing templates | Low | Low | No existing template references this key. `throwOnUndefined` only triggers on access. |
| Template Nunjucks syntax error | Low | Medium | Unit test validates template renders without error for all 8 profile configs. |
| Backward compat: old `resources/` without new template | Low | Low | `DocsAssembler` checks `fs.existsSync()` before reading. Returns `[]` if template missing. |
| Pipeline ordering: DocsAssembler output counted in README stats | Low | Low | Place DocsAssembler before ReadmeAssembler so README can count `docs/` files if needed. |
| `docs/architecture/` directory creation race condition | Very Low | Low | `mkdirSync({ recursive: true })` is idempotent. |

---

## Implementation Order (TDD)

Following the story's TPP ordering and the project's inner-first convention:

### Phase 1: Template Creation (Red-Green)

1. **Test:** Write unit test asserting `_TEMPLATE-SERVICE-ARCHITECTURE.md` exists and
   contains all 10 mandatory section headers.
2. **Implement:** Create the template file in `resources/templates/`.
3. **Test:** Validate Mermaid code blocks exist in section 2.
4. **Test:** Validate NFR table structure in section 6.

### Phase 2: Context Extension (Red-Green)

5. **Test:** Assert `buildDefaultContext()` includes `interfaces_list` key.
6. **Implement:** Add `interfaces_list` to `buildDefaultContext()` in `template-engine.ts`.
7. **Test:** Verify `interfaces_list` resolves correctly for multi-interface configs.

### Phase 3: DocsAssembler (Red-Green)

8. **Test:** `DocsAssembler.assemble()` generates `docs/architecture/service-architecture.md`.
9. **Implement:** Create `src/assembler/docs-assembler.ts`.
10. **Test:** Placeholder resolution — `{{ project_name }}` replaced with actual name.
11. **Test:** Template missing — returns empty array.
12. **Test:** Output directory created automatically.

### Phase 4: Pipeline Integration (Red-Green)

13. **Test:** `buildAssemblers()` includes `DocsAssembler`.
14. **Implement:** Register in `pipeline.ts`, add `"docs"` target.
15. **Test:** Full pipeline run produces `docs/architecture/service-architecture.md`.

### Phase 5: Golden Files & Integration

16. Regenerate all 8 golden profiles.
17. Run `byte-for-byte.test.ts` to verify parity.
18. Run full test suite to confirm no regressions.

### Phase 6: Refactoring

19. Review for method length compliance (<=25 lines).
20. Verify no wildcard imports.
21. Ensure all files <=250 lines.

---

## File Manifest (Summary)

| Action | File Path |
|--------|-----------|
| CREATE | `resources/templates/_TEMPLATE-SERVICE-ARCHITECTURE.md` |
| CREATE | `src/assembler/docs-assembler.ts` |
| CREATE | `tests/node/assembler/docs-assembler.test.ts` |
| MODIFY | `src/template-engine.ts` (add `interfaces_list` to context) |
| MODIFY | `src/assembler/pipeline.ts` (register assembler, extend target type) |
| MODIFY | `tests/golden/go-gin/docs/architecture/service-architecture.md` |
| MODIFY | `tests/golden/java-quarkus/docs/architecture/service-architecture.md` |
| MODIFY | `tests/golden/java-spring/docs/architecture/service-architecture.md` |
| MODIFY | `tests/golden/kotlin-ktor/docs/architecture/service-architecture.md` |
| MODIFY | `tests/golden/python-click-cli/docs/architecture/service-architecture.md` |
| MODIFY | `tests/golden/python-fastapi/docs/architecture/service-architecture.md` |
| MODIFY | `tests/golden/rust-axum/docs/architecture/service-architecture.md` |
| MODIFY | `tests/golden/typescript-nestjs/docs/architecture/service-architecture.md` |
