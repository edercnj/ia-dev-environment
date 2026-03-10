# STORY-011: AgentsAssembler — Implementation Plan

## Affected Layers

- `src/assembler/` — new assembler module

## New Files

| File | Purpose | Lines (est.) |
|------|---------|-------------|
| `src/assembler/agents-selection.ts` | Pure selection logic: conditional agents + checklist rules | ~90 |
| `src/assembler/agents-assembler.ts` | I/O orchestration: copy agents + inject checklists | ~170 |
| `tests/node/assembler/agents-assembler.test.ts` | Unit + integration tests | ~600 |

## Modified Files

| File | Change |
|------|--------|
| `src/assembler/index.ts` | Add exports for new modules |

## Design Decisions

1. **Reuse `AssembleResult`** from `rules-assembler.ts` (same `{ files, warnings }` shape)
2. **Extract selection logic** into `agents-selection.ts` (follows `skills-selection.ts` pattern)
3. **Use existing helpers**: `copyTemplateFile`, `copyTemplateFileIfExists` from `copy-helpers.ts`
4. **Use existing conditions**: `hasInterface`, `hasAnyInterface` from `conditions.ts`
5. **Checklist injection** via `TemplateEngine.injectSection()` (static method)
6. **Marker format**: `<!-- FILENAME_UPPER -->` (e.g., `pci-dss-security.md` → `<!-- PCI_DSS_SECURITY -->`)

## Agent Selection Logic

### Core Agents
- Scan `agents-templates/core/` for `.md` files, sorted alphabetically

### Conditional Agents
| Condition | Agent |
|-----------|-------|
| `data.database.name != "none"` | `database-engineer.md` |
| `infrastructure.observability.tool != "none"` | `observability-engineer.md` |
| container/orchestrator/iac/serviceMesh != "none" | `devops-engineer.md` |
| interfaces contain rest/grpc/graphql | `api-engineer.md` |
| eventDriven OR event-consumer/event-producer | `event-engineer.md` |

### Developer Agent
- `{language.name}-developer.md`

### Checklist Injection Rules
| Target Agent | Checklist | Condition |
|-------------|-----------|-----------|
| security-engineer.md | pci-dss-security.md | "pci-dss" in frameworks |
| security-engineer.md | privacy-security.md | "lgpd" or "gdpr" in frameworks |
| security-engineer.md | hipaa-security.md | "hipaa" in frameworks |
| security-engineer.md | sox-security.md | "sox" in frameworks |
| api-engineer.md | grpc-api.md | hasInterface("grpc") |
| api-engineer.md | graphql-api.md | hasInterface("graphql") |
| api-engineer.md | websocket-api.md | hasInterface("websocket") |
| devops-engineer.md | helm-devops.md | templating == "helm" |
| devops-engineer.md | iac-devops.md | iac != "none" |
| devops-engineer.md | mesh-devops.md | serviceMesh != "none" |
| devops-engineer.md | registry-devops.md | registry != "none" |

## Risk Assessment

- Low risk: straightforward port following established patterns
- Checklist injection is the most complex part (file read + marker replacement + write)
