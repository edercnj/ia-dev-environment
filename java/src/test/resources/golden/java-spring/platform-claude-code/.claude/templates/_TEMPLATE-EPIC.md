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
> Ver [`.claude/rules/22-lifecycle-integrity.md`](../rules/22-lifecycle-integrity.md).

## 1. Visão Geral

**Chave Jira:** <CHAVE-JIRA>

<Descrição concisa (3-5 frases) do escopo do épico. O que será construído, qual problema resolve, e qual é o boundary do que está incluído e excluído.>

## 2. Anexos e Referências

- <Nome do documento/spec> (Link)
- <Diagrama/RFC/ADR relevante> (Link)

## 3. Definições de Qualidade Globais

### Global Definition of Ready (DoR)

- <Critério 1 que deve estar satisfeito para qualquer história entrar em desenvolvimento>
- <Critério 2>
- <Critério N>

### Global Definition of Done (DoD)

- **Cobertura:** <Meta de cobertura — ex: ≥ 95% Line, ≥ 90% Branch>
- **Testes Automatizados:** <Tipos de testes exigidos e cenários obrigatórios>. Cada história DEVE ter pelo menos 1 teste automatizado validando o critério de aceite principal.
- **Smoke Tests:** Obrigatório quando testing.smoke_tests == true. Cada história deve passar no smoke gate.
- **Relatório de Cobertura:** <Formato e granularidade esperada>
- **Documentação:** <Artefatos de documentação que devem estar atualizados>
- **Persistência:** <Critério de integridade de dados, se aplicável>
- **Performance:** <SLO de latência/throughput>
- **TDD Compliance:** Commits show test-first pattern. Explicit refactoring after green. Tests are incremental (from simple to complex via TPP — Transformation Priority Premise).
- **Double-Loop TDD:** Acceptance tests derived from Gherkin scenarios (outer loop). Unit tests guided by TPP (inner loop).

## 4. Regras de Negócio Transversais (Source of Truth)

> Regras que se aplicam a múltiplas histórias. Cada história referencia as regras pelo ID. Alterações de regra propagam automaticamente para todas as histórias dependentes.

| ID | Título | Descrição |
| :--- | :--- | :--- |
| **[RULE-001]** | <Título da regra> | <Descrição detalhada. Usar `<br>` para quebras de linha dentro da célula. Incluir cenários de aplicação e prioridade entre regras conflitantes.> |
| **[RULE-00N]** | <Título da regra> | <Descrição> |

## 5. Índice de Histórias

| ID | Título | Dependências (Blocked By) | Entrega de Valor | Planejamento |
| :--- | :--- | :--- | :--- | :--- |
| [story-XXXX-0001](./story-XXXX-0001.md) | <Título da história> | - | <Valor mensurável de negócio> | {{PLANNING_STATUS}} |
| [story-XXXX-0002](./story-XXXX-0002.md) | <Título da história> | story-XXXX-0001 | <Valor mensurável de negócio> | {{PLANNING_STATUS}} |
| [story-XXXX-YYYY](./story-XXXX-YYYY.md) | <Título da história> | <Dependências separadas por vírgula> | <Valor mensurável de negócio> | {{PLANNING_STATUS}} |
