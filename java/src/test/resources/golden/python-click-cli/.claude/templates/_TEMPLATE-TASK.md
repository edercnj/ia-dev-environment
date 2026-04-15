# Task: {{TASK_TITLE}}

**ID:** {{TASK_ID}}
**Story:** {{STORY_ID}}
**Status:** Pendente

## 1. Objetivo

{{OBJECTIVE}}

## 2. Contratos I/O

### 2.1 Inputs

{{INPUTS_LIST}}

### 2.2 Outputs

{{OUTPUTS_LIST}}

### 2.3 Testabilidade

- [ ] Independentemente testável
- [ ] Requer mock de TASK-XXXX-YYYY-NNN
- [ ] Coalescível com TASK-XXXX-YYYY-NNN

> Mark exactly ONE of the boxes above (RULE-TF-01). When marking `Requer mock de ...`
> or `Coalescível com ...`, replace the TASK-ID placeholder with the real partner.

## 3. Definition of Done

- [ ] Código implementado conforme Seção 2.2
- [ ] Teste automatizado cobre os outputs declarados
- [ ] `{{COMPILE_COMMAND}}` verde
- [ ] Novo teste é Red → Green → Refactor (test-first)
- [ ] Contratos I/O respeitados (verificação via grep/assert)
- [ ] Commit atômico em Conventional Commits (RULE-TF-04)

## 4. Dependências

| Depends on | Relação | Pode mockar? |
| :--- | :--- | :--- |
| {{DEPENDS_ON}} | {{RELATION}} | {{CAN_MOCK}} |

## 5. Plano de Implementação

Ver `plan-task-{{TASK_ID}}.md` (gerado por `x-task-plan`).
