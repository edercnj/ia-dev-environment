# Schema: `task-TASK-XXXX-YYYY-NNN.md`

**Status:** Canonical (story-0038-0001)
**Filename pattern:** `task-TASK-XXXX-YYYY-NNN.md`
**Filename regex:** `^task-TASK-\d{4}-\d{4}-\d{3}\.md$`
**Derived plan filename:** `plan-task-TASK-XXXX-YYYY-NNN.md`

> The shorthand `task-TASK-NNN.md` appears in prose as a documental abbreviation only.
> The schema, parser, and examples always use the full pattern above.

This document defines the formal schema of a task file as introduced by EPIC-0038 (Task-First
Planning & Execution Architecture). It is the source of truth for `TaskFileParser` and for
authors of new task files.

## 1. Required Sections (Order Significant)

| # | Section | Required | Format | Validation Rule |
| :--- | :--- | :--- | :--- | :--- |
| 1 | Title `# Task: {title}` | Yes | H1 markdown | Non-empty |
| 2 | `**ID:** TASK-XXXX-YYYY-NNN` | Yes | Bold + regex `TASK-\d{4}-\d{4}-\d{3}` | Matches filename (TF-SCHEMA-001) |
| 3 | `**Story:** story-XXXX-YYYY` | Yes | Bold + regex `story-\d{4}-\d{4}` | Format check |
| 4 | `**Status:** <enum>` | Yes | Enum | TF-SCHEMA-002 |
| 5 | `## 1. Objetivo` | Yes | Markdown body | Non-empty |
| 6 | `## 2. Contratos I/O` | Yes | 3 subsections | All three present |
| 6.1 | `### 2.1 Inputs` | Yes | List/text | Non-empty (preconditions) |
| 6.2 | `### 2.2 Outputs` | Yes | List/text | Non-empty (TF-SCHEMA-004) |
| 6.3 | `### 2.3 Testabilidade` | Yes | Checklist with EXACTLY one `[x]` | TF-SCHEMA-003 |
| 7 | `## 3. Definition of Done` | Yes | Checklist | ≥ 6 items (TF-SCHEMA-005 WARN otherwise) |
| 8 | `## 4. Dependências` | Yes | Table or `—` | TF-SCHEMA-006 if COALESCED |
| 9 | `## 5. Plano de implementação` | No (placeholder) | Reference to `plan-task-TASK-XXXX-YYYY-NNN.md` | — |

## 2. Status Enum

Permitted values for the `**Status:**` field:

- `Pendente`
- `Em Andamento`
- `Concluída`
- `Bloqueada`
- `Falha`

Any other value triggers `TF-SCHEMA-002` (ERROR).

## 3. Testability Declaration (Section 2.3)

The Testabilidade subsection MUST contain a checklist with exactly one checked option:

```markdown
### 2.3 Testabilidade

- [x] Independentemente testável
- [ ] Requer mock de TASK-XXXX-YYYY-NNN
- [ ] Coalescível com TASK-XXXX-YYYY-NNN
```

Mapping to `TestabilityKind` enum:

| Checked option | `TestabilityKind` |
| :--- | :--- |
| Independentemente testável | `INDEPENDENT` |
| Requer mock de … | `REQUIRES_MOCK` |
| Coalescível com … | `COALESCED` |

Zero or multiple checked options trigger `TF-SCHEMA-003` (ERROR).

## 4. Validations Catalog

| Rule ID | Severity | Condition |
| :--- | :--- | :--- |
| `TF-SCHEMA-001` | ERROR | ID absent or does not match filename |
| `TF-SCHEMA-002` | ERROR | Status outside the permitted enum |
| `TF-SCHEMA-003` | ERROR | Testability absent, multiple, or unrecognised |
| `TF-SCHEMA-004` | ERROR | Outputs section empty |
| `TF-SCHEMA-005` | WARN | DoD checklist has fewer than 6 items |
| `TF-SCHEMA-006` | ERROR | COALESCED references a TASK-ID that does not exist in the validation context |

A task file with zero ERROR violations is considered `valid=true`. WARN-level violations
do NOT invalidate the file.

## 5. Inline Examples of Each Testability Kind

### 5.1 INDEPENDENT

```markdown
### 2.3 Testabilidade

- [x] Independentemente testável
- [ ] Requer mock de TASK-XXXX-YYYY-NNN
- [ ] Coalescível com TASK-XXXX-YYYY-NNN
```

The task can be implemented and tested in full isolation. Standard Red-Green-Refactor.

### 5.2 REQUIRES_MOCK

```markdown
### 2.3 Testabilidade

- [ ] Independentemente testável
- [x] Requer mock de TASK-0038-0002-002
- [ ] Coalescível com TASK-XXXX-YYYY-NNN
```

The task depends on the API surface of another task that is not yet implemented. Tests use a
mock or fake implementation of that surface. The mocked TASK-ID MUST be cited.

### 5.3 COALESCED

```markdown
### 2.3 Testabilidade

- [ ] Independentemente testável
- [ ] Requer mock de TASK-XXXX-YYYY-NNN
- [x] Coalescível com TASK-0038-0001-004
```

The task is mutually recursive with another task and the two MUST land in the same commit
(`Coalesces-with: TASK-0038-0001-004` footer per RULE-TF-04). The referenced TASK-ID MUST
exist in the validation context (TF-SCHEMA-006).

## 6. Minimal Valid File Skeleton

```markdown
# Task: <título>

**ID:** TASK-0038-0001-001
**Story:** story-0038-0001
**Status:** Pendente

## 1. Objetivo

Descrição do que esta task entrega como unidade atômica.

## 2. Contratos I/O

### 2.1 Inputs

- Pré-condição A
- Pré-condição B

### 2.2 Outputs

- Pós-condição verificável A (grep)
- Pós-condição verificável B (test)

### 2.3 Testabilidade

- [x] Independentemente testável
- [ ] Requer mock de TASK-XXXX-YYYY-NNN
- [ ] Coalescível com TASK-XXXX-YYYY-NNN

## 3. Definition of Done

- [ ] Código implementado
- [ ] Teste cobre output declarado
- [ ] `mvn compile` verde
- [ ] Red→Green→Refactor honesto
- [ ] Contratos I/O respeitados
- [ ] Commit atômico Conventional Commits

## 4. Dependências

| Depends on | Relação | Pode mockar? |
| :--- | :--- | :--- |
| — | — | — |

## 5. Plano de implementação

Ver `plan-task-TASK-0038-0001-001.md`.
```

## 7. Relationship to Cross-Cutting Rules

| Cross-cutting Rule | Schema Enforcement |
| :--- | :--- |
| RULE-TF-01 (Task Testability) | Section 2.3 mandatory; TF-SCHEMA-003 |
| RULE-TF-02 (I/O Contracts Are Mandatory) | Sections 2.1 + 2.2 mandatory; TF-SCHEMA-004 |
| RULE-TF-04 (Task Commits Are Atomic) | DoD section mentions atomic commit; TF-SCHEMA-005 (WARN) |

## 8. Out of Scope (Future Stories)

- Formal `_TEMPLATE-TASK.md` template — story-0038-0009.
- Schema for `task-implementation-map-STORY-XXXX-YYYY.md` — story-0038-0002.
- Plan file `plan-task-TASK-XXXX-YYYY-NNN.md` schema — story-0038-0003.
- Writer/serialiser (this story is read-only) — story-0038-0009.
