---
name: planning-standards-kp
description: "Fonte da verdade do modelo RA9 (Rule-Aligned 9-Section) para templates de planejamento Epic/Story/Task. Define as 9 seções fixas, granularidade por nível, micro-template Decision Rationale, e mapeamento rule ↔ seção."
user-invocable: false
context-budget: medium
---

> 🔒 **INTERNAL KNOWLEDGE PACK** — Referenced by plan skills (`x-epic-create`, `x-epic-decompose`, `x-story-plan`, `x-task-plan`). Not user-invocable.

# Knowledge Pack: Planning Standards (RA9 — Rule-Aligned 9-Section)

## Purpose

Single source of truth for the RA9 model. Every plan artifact (Epic, Story, Task) MUST contain the 9 fixed sections in the canonical order below. Skills reference this KP via `@planning-standards-kp` instead of duplicating the contract inline.

---

## 1. Contexto & Escopo

**Rule anchor:** Rule 01 (Project Identity)
**KP anchor:** N/A (project-identity)
**Required at:**

| Level | Required content |
| :--- | :--- |
| Epic | 3-5 sentence description: what is built, problem solved, scope boundaries (in/out). Jira key. |
| Story | User story sentence (`As <persona>, I want <action> so that <benefit>`). 2-3 paragraphs of context. Entrega de Valor subsection. |
| Task | Single objective sentence. What this task implements specifically. |

**Granularity note:** Epic = vision + boundaries; Story = feature + value; Task = single implementation unit.

---

## 2. Packages (Hexagonal)

**Rule anchor:** Rule 04 (Architecture Summary)
**KP anchor:** `knowledge/architecture.md`
**Required at:** Epic, Story, Task (minimum 1 layer declared)

| Level | Required content |
| :--- | :--- |
| Epic | Full package catalog across all 5 layers for all stories in the epic. Mark untouched layers with `—`. Declare dependency direction explicitly: `adapter → application → domain`. |
| Story | Packages touched by this story. New classes/interfaces per layer. Dependency direction statement. |
| Task | 1-3 specific files. Layer classification. |

**5 canonical layers:**
1. `domain/` — entities, value objects, engines, ports
2. `application/` — use cases, orchestration
3. `adapter/inbound/` — REST, gRPC, CLI handlers, DTOs, mappers
4. `adapter/outbound/` — database, external clients, entities, mappers
5. `infrastructure/` — framework config, observability wiring

**Example (Story level):**
```
### Domain Layer
- `domain/payment/model/PaymentOrder.java` — new entity
- Dependency direction: application → domain (inward only)

### Application Layer
- `application/payment/ProcessPaymentUseCase.java` — new use case
- Port: `domain/payment/port/PaymentRepository.java`

### Adapter Inbound
- `adapter/inbound/rest/PaymentController.java` — new REST handler
- DTO: `adapter/inbound/rest/dto/PaymentRequest.java`

### Adapter Outbound
- `adapter/outbound/database/JpaPaymentRepository.java` — implements port

### Infrastructure
— (no changes)
```

---

## 3. Contratos & Endpoints

**Rule anchor:** Rule 03 (Coding Standards)
**KP anchor:** `knowledge/protocols.md`
**Required at:** Epic (summary), Story (full contract), Task (single handler/method signature)

| Level | Required content |
| :--- | :--- |
| Epic | List of all endpoints/events exposed by the epic (method + path + protocol). |
| Story | Full request/response schema (field, type, M/O, validation, example). Error codes (RFC 7807). Event schema when event-driven. |
| Task | Single method signature or handler I/O contract. |

**Formats:** REST → OpenAPI fields; Event → AsyncAPI payload; gRPC → Protobuf message definition.

---

## 4. Materialização SOLID

**Rule anchor:** Rule 03 (Coding Standards)
**KP anchor:** `knowledge/coding-standards.md`
**Required at:** Epic (transversal business rules), Story (local rules), Task (SOLID constraints)

| Level | Required content |
| :--- | :--- |
| Epic | Cross-cutting business rules table (`RULE-NNN | Title | Description`). Applied to multiple stories. |
| Story | Business rules applicable to this story (reference epic RULE-NNN by ID). Local constraints. |
| Task | SOLID principles applicable (SRP: one reason to change; OCP: new behavior = new class; etc.). Coding standard constraints (≤25 lines/method, ≤250 lines/class). |

---

## 5. Quality Gates

**Rule anchor:** Rule 05 (Quality Gates)
**KP anchor:** `knowledge/testing.md`
**Required at:** Epic (global DoR/DoD), Story (local DoR/DoD + Gherkin), Task (DoD checklist)

| Level | Required content |
| :--- | :--- |
| Epic | Global DoR (pre-conditions for any story). Global DoD (coverage ≥95% line, ≥90% branch; TDD compliance; smoke tests; CHANGELOG). |
| Story | Local DoR + local DoD. Gherkin scenarios (subsection 5.2 Acceptance Criteria). TPP order: degenerate → unconditional → conditions → iterations → edge cases. |
| Task | DoD checklist (code implemented, test Red→Green→Refactor, `{{COMPILE_COMMAND}}` green, contracts respected, atomic commit). |

**Coverage threshold (non-negotiable):** ≥ 95% line, ≥ 90% branch (Rule 05 absolute gate).

---

## 6. Segurança

**Rule anchor:** Rule 06 (Security Baseline)
**KP anchor:** `knowledge/security/index.md`
**Required at:** Epic (global security rules), Story (relevant security checks), Task (code-level checks)

| Level | Required content |
| :--- | :--- |
| Epic | Security policies that apply to all stories. Sensitive data classification. |
| Story | OWASP checks applicable. Input validation strategy. Authentication/authorization requirements. |
| Task | Code-level security: no SQL concatenation, no hardcoded credentials, no `Math.random()` for tokens, path normalization for file ops. |

**Minimum required per Story:** identify whether task touches user input, file I/O, auth tokens, or external calls; if yes, declare the security control.

---

## 7. Observabilidade

**Rule anchor:** Rule 07 (Operations Baseline)
**KP anchor:** `knowledge/observability/index.md`
**Required at:** Epic (global SLOs), Story (logging/tracing requirements), Task (structured logging)

| Level | Required content |
| :--- | :--- |
| Epic | SLO/SLI definitions. Health check requirements. Shutdown timeout. |
| Story | Structured logging fields. Correlation ID propagation. Metrics to emit. |
| Task | Specific log statements with level and message. Trace propagation. |

**Required fields in structured logs:** `timestamp`, `level`, `message`, `trace_id`, `span_id`, `service`.

---

## 8. Decision Rationale

**Rule anchor:** Rule 24 (Execution Integrity)
**KP anchor:** N/A
**Required at:** Epic (mandatory, ≥1 item), Story (mandatory, ≥1 item), Task (optional — `N/A — <short reason>` accepted)

### Micro-template (4-line format — MANDATORY for each decision)

```markdown
**Decisão:** <statement of the decision made>
**Motivo:** <why this option was chosen — technical or business constraint>
**Alternativa descartada:** <what was rejected and why>
**Consequência:** <trade-off or future implication>
```

### Validation rules

| Level | Rule |
| :--- | :--- |
| Epic | Section 8 MUST have ≥1 filled item using the 4-line micro-template. Placeholders (`{{...}}`), `TODO`, empty body → `RA9_RATIONALE_EMPTY` audit failure. |
| Story | Same as Epic. |
| Task | `N/A — <reason>` is accepted. If a design decision was made (retry count, lazy vs eager, algorithm choice), use the micro-template. |

### Example (Story level)

```markdown
**Decisão:** Use `JpaPaymentRepository` as the outbound adapter instead of a custom JDBC template.
**Motivo:** Spring Data JPA provides built-in transaction management and reduces boilerplate by 60% for this simple CRUD pattern.
**Alternativa descartada:** Custom JDBC — rejected because it requires manual transaction handling and duplicates RowMapper code already present in 3 other adapters.
**Consequência:** Binds the adapter to Spring Data JPA; future migration to reactive persistence requires adapter rewrite, not domain change.
```

---

## 9. Dependências & File Footprint

**Rule anchor:** Rule 14 (Project Scope)
**KP anchor:** `knowledge/parallelism-heuristics.md`
**Required at:** Epic (story index + dependency DAG), Story (task list + file footprint), Task (file list + `depends-on`)

| Level | Required content |
| :--- | :--- |
| Epic | Story index table with dependency column. Implementation map reference. |
| Story | Task list (Section 8). `## File Footprint` block for parallelism evaluation (Rule 04 EPIC-0041). |
| Task | Explicit file list (`write:` / `read:` / `regen:`). `Dependencies` field (other task IDs or `—`). |

### File Footprint format (Story level — RULE-004 EPIC-0041)

```markdown
## File Footprint

write:
  - src/main/java/dev/iadev/domain/payment/model/PaymentOrder.java
  - src/main/java/dev/iadev/adapter/inbound/rest/PaymentController.java
read:
  - src/main/java/dev/iadev/domain/payment/port/PaymentRepository.java
regen:
  - src/test/resources/golden/java-spring/.claude/skills/x-epic-create/SKILL.md
```

---

## Granularity Summary Table

| Section | Epic depth | Story depth | Task depth |
| :--- | :--- | :--- | :--- |
| 1. Contexto & Escopo | Vision + boundaries (3-5 sentences) | User story + context (2-3 paragraphs) | Single objective (1 sentence) |
| 2. Packages (Hexagonal) | Full catalog all 5 layers | Story-scoped packages | 1-3 specific files |
| 3. Contratos & Endpoints | Endpoint summary list | Full request/response schema | Single handler signature |
| 4. Materialização SOLID | Cross-cutting business rules table | Story rules (RULE-NNN refs) | SOLID constraints per method |
| 5. Quality Gates | Global DoR/DoD | Local DoR/DoD + Gherkin | DoD checklist |
| 6. Segurança | Global security policies | OWASP checks + auth requirements | Code-level security rules |
| 7. Observabilidade | SLO/SLI definitions | Logging + tracing requirements | Specific log statements |
| 8. Decision Rationale | ≥1 item (mandatory) | ≥1 item (mandatory) | N/A accepted |
| 9. Dependências & File Footprint | Story index + DAG | Task list + File Footprint block | File list + depends-on |

---

## Rule ↔ Section Mapping

| Rule | Section(s) | Enforcement |
| :--- | :--- | :--- |
| Rule 01 — Project Identity | 1. Contexto & Escopo | Story persona must match domain |
| Rule 03 — Coding Standards | 3. Contratos, 4. SOLID | Hard limits (≤25 lines, ≤250 class) |
| Rule 04 — Architecture | 2. Packages | Dependency direction mandatory |
| Rule 05 — Quality Gates | 5. Quality Gates | Absolute coverage gate (95/90) |
| Rule 06 — Security | 6. Segurança | At least one control per user-input story |
| Rule 07 — Operations | 7. Observabilidade | Structured logging fields required |
| Rule 14 — Project Scope | 9. Dependências | File footprint block in stories |
| Rule 24 — Execution Integrity | 8. Decision Rationale | 4-line micro-template mandatory |

---

## Audit Rules (RA9 CI enforcement — RULE-006 EPIC-0056)

| Code | Applies to | Fails when |
| :--- | :--- | :--- |
| `RA9_SECTIONS_MISSING` | Epic, Story, Task | Any of the 9 `## N. <name>` headers absent |
| `RA9_RATIONALE_EMPTY` | Epic, Story (NOT Task) | Section 8 present but body is empty, `{{...}}`, or `TODO` |
| `RA9_PACKAGES_MISSING` | Epic, Story, Task | Section 2 present but all 5 layers marked `—` |

Escape hatch: `<!-- audit-exempt -->` on the line immediately before the artifact (rare, reviewed exceptions only).
