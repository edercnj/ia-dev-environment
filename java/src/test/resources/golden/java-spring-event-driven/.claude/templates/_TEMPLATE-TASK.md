# Task: {{TASK_TITLE}}

**ID:** {{TASK_ID}}
**Story:** {{STORY_ID}}
**Status:** Pendente

> **Status Transitions (Rule 22 — lifecycle-integrity):**
> valores permitidos `Pendente | Planejada | Em Andamento | Concluída | Falha | Bloqueada`.
> Transições válidas: `Pendente → Planejada | Em Andamento | Falha | Bloqueada`;
> `Planejada → Em Andamento | Falha | Bloqueada`;
> `Em Andamento → Concluída | Falha | Bloqueada`;
> reabertura `Concluída → Em Andamento` (via `x-status-reconcile --apply`) e
> `Falha → Pendente`; `Bloqueada → Pendente | Planejada | Em Andamento | Falha`.
> Ver [`.claude/rules/22-lifecycle-integrity.md`](../.claude/rules/22-lifecycle-integrity.md).

---

## 1. Contexto & Escopo

{{OBJECTIVE}}

> Single task objective: what specific unit of code this task implements, which layer it targets, and how it fits into the parent story.

---

## 2. Packages (Hexagonal)

> 1-3 specific files for this task. Layer classification required (Rule 04).

| Layer | File | Action |
| :--- | :--- | :--- |
| `<domain|application|adapter.inbound|adapter.outbound|infrastructure>` | `<path/to/File.java>` | `create|modify` |
| `<layer>` | `<path/to/TestFile.java>` | `create` |

**Dependency direction (if creating new classes):** `adapter → application → domain` (inward only).

---

## 3. Contratos & Endpoints

> I/O contract for this specific task (single method/handler signature or event payload).

### 3.1 Inputs

{{INPUTS_LIST}}

### 3.2 Outputs

{{OUTPUTS_LIST}}

### 3.3 Testabilidade

- [ ] Independentemente testável
- [ ] Requer mock de TASK-XXXX-YYYY-NNN
- [ ] Coalescível com TASK-XXXX-YYYY-NNN

> Mark exactly ONE of the boxes above (RULE-TF-01). When marking `Requer mock de ...`
> or `Coalescível com ...`, replace the TASK-ID placeholder with the real partner.

---

## 4. Materialização SOLID

> SOLID constraints applicable to code written in this task (Rule 03).

- **SRP:** This class/method has one reason to change: `<state the single responsibility>`.
- **OCP:** New behavior will be added via `<extension point>`, not by modifying existing code.
- **Coding limits:** ≤ 25 lines/method, ≤ 250 lines/class, ≤ 4 parameters/function.

---

## 5. Quality Gates

### 5.1 Definition of Done

- [ ] Código implementado conforme Seção 2 e 3
- [ ] Teste automatizado cobre os outputs declarados (Seção 3.2)
- [ ] `{{COMPILE_COMMAND}}` verde
- [ ] Novo teste segue Red → Green → Refactor (test-first)
- [ ] Contratos I/O respeitados (verificação via grep/assert)
- [ ] Commit atômico em Conventional Commits (RULE-TF-04)

---

## 6. Segurança

> Code-level security rules for this task (Rule 06).

- [ ] No SQL concatenation (use parameterized queries)
- [ ] No hardcoded credentials or tokens
- [ ] No `Math.random()` for security-sensitive values
- [ ] Path inputs normalized and validated against base directory (if applicable)

---

## 7. Observabilidade

> Specific log statements or metrics emitted by code in this task.

| Event | Level | Message pattern |
| :--- | :--- | :--- |
| `<event>` | `INFO/WARN/ERROR` | `"<log message with relevant fields>"` |

> Skip with `N/A — no I/O or state change` when task is pure computation.

---

## 8. Decision Rationale

> Optional for tasks — use `N/A — <short reason>` when no design trade-off was made.
> Use the 4-line micro-template when a local decision was taken (retry count, lazy vs eager,
> algorithm choice, etc.) — Rule 24 / planning-standards-kp.

N/A — <state why no significant design decision was made for this task, or replace with micro-template below>

> Or use micro-template:
>
> **Decisão:** <statement da decisão local>
> **Motivo:** <restrição técnica ou de negócio>
> **Alternativa descartada:** <o que foi rejeitado>
> **Consequência:** <trade-off ou implicação>

---

## 9. Dependências & File Footprint

### 9.1 Dependências

| Depends on | Relação | Pode mockar? |
| :--- | :--- | :--- |
| {{DEPENDS_ON}} | {{RELATION}} | {{CAN_MOCK}} |

### 9.2 Implementation Plan Reference

Ver `plan-task-{{TASK_ID}}.md` (gerado por `x-task-plan`).

### 9.3 File Footprint

```
write:
  - <path/to/new/file.java>
read:
  - <path/to/dependency.java>
```
