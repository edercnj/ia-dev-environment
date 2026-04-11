# Jira Field Mapping — Epic & Story Templates

This document defines the de-para (mapping) between local markdown template fields and Jira issue fields.

## Epic Field Mapping

| Template Field | Location | Jira Field | API Parameter | Notes |
|---|---|---|---|---|
| Título do Épico | `# Épico: <título>` (line 1) | Summary | `summary` | Direct mapping |
| Visão Geral | Section 1 text | Description | `description` | Use `contentFormat: "markdown"` |
| Status | Header `**Status:**` | Workflow status | `transitionJiraIssue` | Map to available transitions |
| Chave Jira | Header `**Chave Jira:**` | Issue Key | `key` (returned) | Read-only, synced back to markdown |
| epic-XXXX (local ID) | Directory name | Labels | `additional_fields.labels` | Stored as label for bidirectional lookup |
| Autor | Header `**Autor:**` | — | — | Not mapped (markdown only) |
| Data | Header `**Data:**` | Created | — | Auto-set by Jira, read-only |
| Versão | Header `**Versão:**` | Fix Version | `fixVersion` (optional) | Only if project has versions configured |

## Story Field Mapping

| Template Field | Location | Jira Field | API Parameter | Notes |
|---|---|---|---|---|
| Título da História | `# História: <título>` (line 1) | Summary | `summary` | Direct mapping |
| Descrição | Section 3 ("Como Persona...") | Description | `description` | Concatenate with Section 3.5 (Entrega de Valor) |
| Entrega de Valor | Section 3.5 | Description (appendage) | `description` | Appended as markdown block. No native Jira field exists |
| story-XXXX-YYYY (local ID) | Header `**ID:**` | Labels | `additional_fields.labels` | Stored as label for bidirectional lookup |
| Chave Jira | Header `**Chave Jira:**` | Issue Key | `key` (returned) | Read-only, synced back to markdown |
| Status | Header `**Status:**` | Workflow status | `transitionJiraIssue` | Map to available transitions |
| Épico pai | From epic file | Parent | `parent` | Set to epic's Jira key |
| Blocked By | Section 1, Blocked By column | Issue Links | `createIssueLink` type "Blocks" | Second pass after all stories created |
| Blocks | Section 1, Blocks column | Issue Links | `createIssueLink` type "Blocks" | Implicit (inverse of Blocked By) |
| Sub-tarefas | Section 8 | — | — | Not mapped (too granular for Jira) |
| Critérios de Aceite | Section 7 (Gherkin) | — | — | Not mapped (too detailed for Jira) |
| Contratos de Dados | Section 5 | — | — | Not mapped |
| DoD Local | Section 4 | Description (optional) | — | Can be appended as checklist |

## Fields NOT Mapped to Jira

These fields are intentionally kept as markdown-only:

| Field | Reason |
|---|---|
| Sub-tarefas (Section 8) | Too granular — Jira sub-tasks add overhead without value |
| Critérios de Aceite (Section 7) | Gherkin scenarios are too detailed for Jira description |
| Contratos de Dados (Section 5) | Technical detail best kept in the story file |
| Diagramas (Section 6) | Mermaid diagrams don't render in Jira |
| Regras Transversais (Section 2) | Internal cross-reference, not meaningful in Jira |
| DoR Local (Section 4) | Process metadata, not issue content |

## Description Construction for Jira

When creating a Story in Jira, the `description` field is constructed by concatenating:

```markdown
{Section 3 — User story paragraph and technical context}

---

## Entrega de Valor

- **Valor Principal:** {value from Section 3.5}
- **Métrica de Sucesso:** {metric from Section 3.5}
- **Impacto no Negócio:** {impact from Section 3.5}
```

This ensures the business value is visible in Jira without requiring a custom field.

## ID Synchronization Strategy

### Local → Jira (Lookup)

The local ID is stored as a Jira label:
```
labels: ["generated-by-ia-dev-env", "story-0012-0001"]
```

JQL query to find a local story in Jira:
```
labels = "story-0012-0001" AND labels = "generated-by-ia-dev-env"
```

### Jira → Local (Lookup)

The Jira key is stored in the markdown file header:
```
**Chave Jira:** PROJ-123
```

To find a Jira issue's local file, search markdown files:
```
grep -r "**Chave Jira:** PROJ-123" plans/
```

## Status Mapping

| Local Status (Markdown) | Jira Transition Target |
|---|---|
| Pendente | To Do / Open |
| Em Andamento | In Progress |
| Concluída | Done |
| Falha | — (no transition, keep current) |
| Bloqueada | — (no transition, keep current) |
| Parcial | — (no transition, keep current) |

**Note:** Jira workflow transitions vary by project. Always use `mcp__atlassian__getTransitionsForJiraIssue` to discover available transitions dynamically. Match by name containing "Done", "Concluído", or "Resolved" for completion transitions.

## Labels Convention

All issues created by ia-dev-env use these labels:
- `generated-by-ia-dev-env` — Identifies auto-generated issues
- `epic-XXXX` or `story-XXXX-YYYY` — The local ID for bidirectional lookup
