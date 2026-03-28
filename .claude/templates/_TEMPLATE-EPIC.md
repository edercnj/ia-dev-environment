# Epico: {{EPIC_TITLE}}

**Autor:** {{AUTHOR}}
**Data:** {{DATE}}
**Versao:** {{VERSION}}
**Status:** {{STATUS}}

## 1. Visao Geral

**Chave Jira:** {{JIRA_KEY}}

{{EPIC_OVERVIEW}}

**Escopo incluido:**

- {{INCLUDED_SCOPE_1}}
- {{INCLUDED_SCOPE_2}}
- {{INCLUDED_SCOPE_3}}

**Escopo excluido:**

- {{EXCLUDED_SCOPE_1}}
- {{EXCLUDED_SCOPE_2}}

## 2. Anexos e Referencias

- [{{REFERENCE_1_TITLE}}]({{REFERENCE_1_PATH}}) -- {{REFERENCE_1_DESCRIPTION}}
- [{{REFERENCE_2_TITLE}}]({{REFERENCE_2_PATH}}) -- {{REFERENCE_2_DESCRIPTION}}

> **Instrucao:** Incluir links para especificacoes, skills, regras, planos, e documentos relacionados.
> Usar caminhos relativos a partir do diretorio do epico.

## 3. Definicoes de Qualidade Globais

### Global Definition of Ready (DoR)

- {{DOR_ITEM_1}}
- {{DOR_ITEM_2}}
- {{DOR_ITEM_3}}

> **Instrucao:** Extrair pre-condicoes globais da especificacao do sistema. Incluir itens de validacao tecnica, resolucao de dependencias, e revisao de contratos de dados.

### Global Definition of Done (DoD)

- {{DOD_ITEM_1}}
- {{DOD_ITEM_2}}
- {{DOD_ITEM_3}}
- **Cobertura:** >= 95% Line, >= 90% Branch
- **TDD Compliance:** Commits mostram padrao test-first (teste precede implementacao no git log), refactoring explicito apos green, testes incrementais via TPP
- **Double-Loop TDD:** Acceptance tests derivados dos cenarios Gherkin (outer loop), unit tests guiados por Transformation Priority Premise (inner loop)
- **Rastreabilidade @GK-N bidirecional:** Todo @GK-N mapeia para >= 1 AT-N, todo AT-N referencia um @GK-N valido
- **Commits atomicos por ciclo TDD:** Cada ciclo Red-Green-Refactor produz um ou mais commits atomicos com formato Conventional Commits
- **Test plan antes de implementacao:** Nenhuma historia inicia implementacao sem test plan gerado via `/x-test-plan`
- Zero regressao: funcionalidades existentes continuam funcionais

> **Instrucao:** Os itens TDD acima sao mandatorios em todo epico. Adicionar itens especificos do dominio antes deles.
> Extrair criterios de cobertura, tipos de teste, requisitos de documentacao, SLOs de performance, e criterios de integridade de dados da especificacao.

## 4. Regras de Negocio Transversais

| ID | Titulo | Descricao |
| :--- | :--- | :--- |
| RULE-001 | {{RULE_001_TITLE}} | {{RULE_001_DESCRIPTION}} |
| RULE-002 | {{RULE_002_TITLE}} | {{RULE_002_DESCRIPTION}} |
| RULE-003 | {{RULE_003_TITLE}} | {{RULE_003_DESCRIPTION}} |

> **Instrucao:** Incluir apenas regras que se aplicam a mais de uma historia (cross-cutting).
> Regras especificas de uma unica historia ficam na secao 2 da historia.
> Cada regra deve ter descricao detalhada o suficiente para implementacao sem consultar a especificacao original.
> Usar `<br>` para quebras de linha dentro de celulas da tabela.

## 5. Indice de Historias

| ID | Titulo | Dependencias (Blocked By) | Blocks | Entrega de Valor | Test Plan |
| :--- | :--- | :--- | :--- | :--- | :--- |
| [{{STORY_ID_1}}](./{{STORY_ID_1}}.md) | {{STORY_TITLE_1}} | -- | {{BLOCKS_1}} | {{VALUE_1}} | Pending |
| [{{STORY_ID_2}}](./{{STORY_ID_2}}.md) | {{STORY_TITLE_2}} | {{STORY_ID_1}} | {{BLOCKS_2}} | {{VALUE_2}} | Pending |
| [{{STORY_ID_3}}](./{{STORY_ID_3}}.md) | {{STORY_TITLE_3}} | {{STORY_ID_2}} | -- | {{VALUE_3}} | Pending |

> **Instrucao:**
> - **Test Plan** deve conter um dos valores: `Pending` (test plan ainda nao gerado), `Ready` (test plan gerado e revisado), ou `N/A` (historia nao requer test plan, ex: documentacao pura)
> - IDs de historias usam formato composto: `story-XXXX-YYYY` (numero do epico + sequencia da historia)
> - Links apontam para `./story-XXXX-YYYY.md` (relativo ao diretorio do epico)
> - Dependencias devem ser simetricas: se A bloqueia B, entao B deve listar A como blocker
> - Coluna **Entrega de Valor** deve expressar valor de negocio mensuravel, NAO tarefas tecnicas
> - Historias sem @GK-N ou sem coluna "Test Plan" em epicos pre-existentes sao aceitas com WARNING (backward compatibility)
