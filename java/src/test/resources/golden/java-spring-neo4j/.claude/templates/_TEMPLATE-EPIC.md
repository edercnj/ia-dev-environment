# Épico: <Título do Épico>

**Autor:** <Papel/Nome do autor>
**Data:** <Data de criação>
**Versão:** <Versão do documento>
**Status:** <Em Refinamento | Pronto | Em Andamento | Concluído>

> **Status Transitions (Rule 22 — lifecycle-integrity):**
> artifacts lifecycle-controlados (Story/Task) usam o enum canônico
> `Pendente | Planejada | Em Andamento | Concluída | Falha | Bloqueada`.
> O campo Status do Épico aqui é documental e reflete o estado
> agregado das histórias filhas. Transições permitidas do enum:
> `Pendente → Planejada | Em Andamento | Falha | Bloqueada`;
> `Planejada → Em Andamento | Falha | Bloqueada`;
> `Em Andamento → Concluída | Falha | Bloqueada`;
> reabertura `Concluída → Em Andamento` (via `x-status-reconcile --apply`) e
> `Falha → Pendente`; `Bloqueada → Pendente | Planejada | Em Andamento | Falha`.
> Ver [`.claude/rules/22-lifecycle-integrity.md`](../.claude/rules/22-lifecycle-integrity.md).

---

## 1. Contexto & Escopo

**Chave Jira:** <CHAVE-JIRA>

<Descrição concisa (3-5 frases) do escopo do épico. O que será construído, qual problema resolve, qual é o boundary do que está incluído e excluído.>

### 1.1 Referências e Anexos

- <Nome do documento/spec> (Link)
- <Diagrama/RFC/ADR relevante> (Link)

---

## 2. Packages (Hexagonal)

> Catálogo de packages novos/tocados pelo épico em todas as 5 camadas.
> Camadas sem impacto devem ser marcadas `—`.
> Direção de dependência obrigatória: `adapter → application → domain`.

### Domain Layer

{{PACKAGES_DOMAIN}}

### Application Layer

{{PACKAGES_APPLICATION}}

### Adapter Inbound

{{PACKAGES_ADAPTER_INBOUND}}

### Adapter Outbound

{{PACKAGES_ADAPTER_OUTBOUND}}

### Infrastructure

{{PACKAGES_INFRASTRUCTURE}}

**Dependency direction:** `adapter.inbound/outbound → application → domain` (inward only).
Domain MUST NOT import adapter or framework code (Rule 04).

---

## 3. Contratos & Endpoints

> Lista de todos os endpoints/eventos expostos pelo épico.
> Cada história preenche o contrato completo em sua própria seção 3.

| Endpoint / Evento | Método / Protocolo | Descrição |
| :--- | :--- | :--- |
| <path ou event-type> | <GET/POST/COMMAND/EVENT> | <Breve descrição> |

---

## 4. Materialização SOLID

> Regras de negócio transversais que se aplicam a múltiplas histórias.
> Cada história referencia as regras pelo ID.
> Alterações propagam automaticamente para histórias dependentes.

| ID | Título | Descrição |
| :--- | :--- | :--- |
| **[RULE-001]** | <Título da regra> | <Descrição detalhada. Usar `<br>` para quebras de linha. Incluir cenários e prioridade entre regras conflitantes.> |
| **[RULE-00N]** | <Título da regra> | <Descrição> |

---

## 5. Quality Gates

### Global Definition of Ready (DoR)

- <Critério 1 que deve estar satisfeito para qualquer história entrar em desenvolvimento>
- <Critério 2>
- <Critério N>

### Global Definition of Done (DoD)

- **Cobertura:** <Meta de cobertura — ex: ≥ 95% Line, ≥ 90% Branch (Rule 05 — absolute gate)>
- **Testes Automatizados:** <Tipos de testes exigidos e cenários obrigatórios>. Cada história DEVE ter pelo menos 1 teste automatizado validando o critério de aceite principal.
- **Smoke Tests:** Obrigatório quando testing.smoke_tests == true. Cada história deve passar no smoke gate.
- **Relatório de Cobertura:** <Formato e granularidade esperada>
- **Documentação:** <Artefatos de documentação que devem estar atualizados>
- **Persistência:** <Critério de integridade de dados, se aplicável>
- **Performance:** <SLO de latência/throughput>
- **TDD Compliance:** Commits show test-first pattern. Explicit refactoring after green. Tests are incremental (from simple to complex via TPP — Transformation Priority Premise).
- **Double-Loop TDD:** Acceptance tests derived from Gherkin scenarios (outer loop). Unit tests guided by TPP (inner loop).

---

## 6. Segurança

> Políticas de segurança que se aplicam a todas as histórias do épico.

| Área | Controle | Rule âncora |
| :--- | :--- | :--- |
| Input validation | <Estratégia de validação de entrada> | Rule 06 |
| Authentication | <Requerimentos de autenticação> | Rule 06 |
| Sensitive data | <Classificação de dados sensíveis — PII, credentials, tokens> | Rule 06 |
| Path operations | <Normalização + rejeição de traversal> | Rule 06 (CWE-22) |

---

## 7. Observabilidade

> SLOs/SLIs e requisitos de observabilidade aplicáveis ao épico.

| Componente | SLO | Métrica |
| :--- | :--- | :--- |
| <Handler/UseCase> | <P99 < Xms> | <latency_ms / throughput_rps> |

**Health checks:** `/health/live` (liveness), `/health/ready` (readiness) — Rule 07.
**Correlation ID:** `X-Correlation-ID` propagated through all downstream calls.
**Structured logging fields required:** `timestamp`, `level`, `message`, `trace_id`, `span_id`, `service`.

---

## 8. Decision Rationale

> Registro de decisões arquiteturais do épico.
> Cada item usa o micro-template de 4 linhas (obrigatório — Rule 24 / planning-standards-kp).
> Task aceita `N/A — <motivo curto>`.

{{DECISION_RATIONALE}}

**Decisão:** <statement da decisão tomada>
**Motivo:** <por que esta opção foi escolhida — restrição técnica ou de negócio>
**Alternativa descartada:** <o que foi rejeitado e por quê>
**Consequência:** <trade-off ou implicação futura>

---

## 9. Dependências & File Footprint

### Índice de Histórias

| ID | Título | Dependências (Blocked By) | Entrega de Valor | Planejamento |
| :--- | :--- | :--- | :--- | :--- |
| [story-XXXX-0001](./story-XXXX-0001.md) | <Título da história> | - | <Valor mensurável de negócio> | {{PLANNING_STATUS}} |
| [story-XXXX-0002](./story-XXXX-0002.md) | <Título da história> | story-XXXX-0001 | <Valor mensurável de negócio> | {{PLANNING_STATUS}} |
| [story-XXXX-YYYY](./story-XXXX-YYYY.md) | <Título da história> | <Dependências separadas por vírgula> | <Valor mensurável de negócio> | {{PLANNING_STATUS}} |

### File Footprint (EPIC-0041 parallelism evaluation)

```
write:
  - <path/to/new/file.java>
read:
  - <path/to/existing/dependency.java>
regen:
  - <path/to/golden/file.md>
```
