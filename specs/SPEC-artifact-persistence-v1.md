# Prompt: Geração de Épico e Histórias — ia-dev-environment Artifact Persistence & Standardization

> **Instrução de uso**: Execute `/x-story-epic-full` com este arquivo como especificação de entrada.
> Exemplo: `/x-story-epic-full specs/SPEC-artifact-persistence-v1.md`

---

## Sistema

**Projeto**: `ia-dev-environment` — CLI generator de ambientes de desenvolvimento assistidos por IA.

**Versão base analisada**: branch `feat/epic-0022-implementation`, ~1010 commits.

**Objetivo desta especificação**: Garantir que TODOS os artefatos produzidos durante o ciclo de
desenvolvimento assistido por IA (planos de implementação, planos de teste, avaliações de segurança,
reviews de especialistas, reviews do Tech Lead, relatórios de fase) sejam persistidos com formato
padronizado, verificáveis antes de re-execução, e suficientemente detalhados para revisão humana.

**Princípio central de todas as histórias**: Execuções de desenvolvimento assistido por IA são
frequentemente longas e custosas em tokens. Se uma sessão é interrompida, o trabalho de planejamento
e avaliação deve ser recuperável sem re-execução. Além disso, revisores humanos precisam de artefatos
com formato consistente e previsível para auditar decisões tomadas pelo agente. Templates padronizados
reduzem alucinação ao fornecer a estrutura exata que a LLM deve preencher.

---

## Escopo do Épico

### Contexto de negócio

O gerador `ia-dev-environment` produz skills (SKILL.md) que orquestram ciclos completos de
desenvolvimento: planejamento arquitetural, planos de teste TDD, implementação, reviews paralelos
por engenheiros especialistas, review do Tech Lead, e execução de épicos inteiros com múltiplas
histórias.

Atualmente, várias skills **definem** que artefatos devem ser salvos em
`plans/epic-XXXX/plans/` e `plans/epic-XXXX/reviews/`, mas existem gaps críticos:

1. **Sem templates padronizados** — Os outputs de planos e reviews variam entre execuções porque
   não existem templates de referência que as LLMs sigam. O formato depende da sessão e do contexto.

2. **Sem verificação de idempotência** — Skills como `x-test-plan` e `x-dev-architecture-plan`
   não verificam se um plano já existe antes de regenerar, desperdiçando tokens e contexto.

3. **Planos de implementação rasos** — O `plan-story-XXXX-YYYY.md` produzido pela Phase 1B do
   `x-dev-lifecycle` não tem template. Faltam: diagramas de classes, method signatures, estratégia
   TDD com mapeamento para cenários UT-N/AT-N, schema DB, decisões arquiteturais, considerações
   por linguagem/framework.

4. **Sem plano de execução do épico** — O `x-dev-epic-implement` não salva um plano global
   de execução antes de iniciar. Quando em dry-run, a análise é descartada.

5. **Sem relatórios de fase** — Não há relatório intermediário ao final de cada fase do épico
   com métricas de coverage, TDD compliance, e status de integrity gates.

6. **Sem dashboard consolidado de reviews** — Os 8 reviews de especialistas e o review do Tech
   Lead são salvos como arquivos individuais, mas não há agregação em um dashboard único.

7. **Sem tracking de remediação** — Quando findings são corrigidos após review, não há
   rastreamento estruturado de quais findings foram fixados, deferidos ou aceitos.

### Dimensões de melhoria

1. **12 novos templates padronizados** — Um template para cada tipo de artefato, com seções
   obrigatórias, formato consistente, e marcadores para preenchimento pela LLM

2. **Assembler Java para distribuição** — Um único `PlanTemplatesAssembler` que copia os 12
   templates para `.claude/templates/` e `.github/templates/` durante a geração

3. **Pre-checks de idempotência em 8 skills** — Verificação de existência e staleness antes de
   regenerar planos, usando mtime comparison (story file vs plan file)

4. **Novo diretório `reports/`** — Junto a `plans/` e `reviews/`, para relatórios de fase e
   dashboards consolidados

5. **Enrichment do implementation plan** — Plano de implementação com class diagram, method
   signatures, TDD strategy, dependency validation, e seções condicionais por linguagem

---

## Regras de Negócio Transversais (Cross-Cutting Rules)

**RULE-001**: Todo artefato de planejamento ou avaliação produzido por uma skill DEVE seguir
o template padronizado correspondente em `.claude/templates/`. Templates definem seções
obrigatórias que a LLM DEVE preencher. Seções marcadas como condicionais (ex: DB schema, event
schema) são incluídas apenas quando o projeto possui a capability correspondente.

**RULE-002**: Antes de gerar qualquer artefato de planejamento, a skill DEVE verificar se o
artefato já existe. Se o arquivo existe E o mtime do story file é anterior ou igual ao mtime
do plano, a skill DEVE reutilizar o plano existente e logar `Reusing existing {artifact} from
{date}`. Se o story file foi modificado após o plano, a skill DEVE regenerar o artefato.

**RULE-003**: Templates de planos DEVEM ser language-agnostic com seções marcadas por
`{{LANGUAGE}}`, `{{FRAMEWORK}}`, `{{DATABASE}}`, `{{ARCHITECTURE}}` que são preenchidas em
runtime pela LLM com base na project identity e nos knowledge packs. O template NÃO é renderizado
pelo engine de templates durante a geração — é copiado verbatim.

**RULE-004**: O `PlanTemplatesAssembler` DEVE copiar templates para DOIS destinos:
`.claude/templates/` e `.github/templates/`. DEVE validar que cada template contém todas as
seções obrigatórias definidas para seu tipo antes de copiar.

**RULE-005**: Todo template de review (specialist e tech lead) DEVE incluir seção de score com
formato numérico padronizado (`XX/YY`) e status padronizado (`Approved`/`Rejected`/`Partial`),
permitindo parsing automático por skills de consolidação.

**RULE-006**: O dashboard consolidado de reviews DEVE ser cumulativo: criado pelo `x-review`
após reviews de especialistas, e atualizado pelo `x-review-pr` após review do Tech Lead. O
histórico de rounds de review é preservado.

**RULE-007**: Skills que produzem planos para subagentes (x-dev-lifecycle delegando a
x-test-plan, x-dev-architecture-plan, etc.) DEVEM instruir o subagente a ler o template
correspondente ANTES de produzir o output. A instrução deve ser explícita: "Read template at
`.claude/templates/_TEMPLATE-{TYPE}.md` for required output format".

**RULE-008**: O implementation plan template DEVE incluir: (a) class diagram em Mermaid, (b)
method signatures por classe, (c) affected layers table, (d) TDD strategy com mapeamento para
cenários UT-N/AT-N do test plan, (e) mini-ADRs para decisões tomadas durante o planejamento.
Seções condicionais incluem: DB schema changes, API changes, Event changes.

**RULE-009**: Artefatos de planejamento são produzidos por modelos capazes de planejamento
profundo (Opus). A instrução `model: opus` DEVE estar presente nas seções das skills que
delegam planejamento a subagentes.

**RULE-010**: O assembler DEVE ter um mecanismo de validação de seções obrigatórias por
template. Se um template no source (`shared/templates/`) não contiver todas as seções
obrigatórias definidas, o assembler DEVE falhar com mensagem indicando quais seções estão
ausentes.

**RULE-011**: Todo artefato persistido DEVE incluir um header padronizado com: Story ID (ou
Epic ID), Data de geração, Autor (nome do agente/role), e Template Version. Isso permite
rastreamento de quando e por quem o artefato foi produzido.

**RULE-012**: Skills DEVEM funcionar gracefully quando templates não estão disponíveis (projetos
gerados antes desta melhoria). Se o template não existir no path esperado, a skill DEVE usar
o formato inline atual como fallback e logar um warning.

---

## Histórias

---

### STORY-0001: Templates de Artefatos de Planejamento (Implementation Plan, Test Plan, Architecture Plan, Task Breakdown)

**Título**: Criação de 4 templates padronizados para artefatos de planejamento de story

**Tipo**: Feature — Template Files

**Prioridade**: Alta (fundação de todas as demais stories)

**Dependências**: Nenhuma. Esta story é independente e pode ser o ponto de entrada do épico.

**Contexto técnico**:
O gerador já possui 19 templates em `java/src/main/resources/shared/templates/` (ex: `_TEMPLATE-EPIC.md`,
`_TEMPLATE-EPIC-EXECUTION-REPORT.md`). Porém, nenhum template existe para os artefatos produzidos durante
o planejamento de cada story: implementation plan, test plan, architecture plan, e task breakdown.

Estes artefatos são produzidos pelas skills `x-dev-lifecycle` (Phase 1B), `x-test-plan`,
`x-dev-architecture-plan`, e `x-lib-task-decomposer` respectivamente. Atualmente, o formato do
output está embutido inline no SKILL.md de cada skill, resultando em variação entre execuções.

**Escopo de implementação**:

Criar 4 novos arquivos de template em `java/src/main/resources/shared/templates/`:

#### Template 1: `_TEMPLATE-IMPLEMENTATION-PLAN.md`

Seções obrigatórias:
1. **Header** — Story ID, Epic ID, Plan Level (Full/Simplified), Date, Author (Senior Architect), Template Version
2. **Executive Summary** — Parágrafo descrevendo o que será implementado e por quê
3. **Affected Layers and Components** — Tabela: Layer | Package | Component | Action (Create/Modify/Delete)
4. **New Classes/Interfaces** — Tabela: Class Name | Package | Type (Class/Interface/Record/Enum) | Purpose
5. **Existing Classes to Modify** — Tabela: Class Name | Package | Change Description | Risk (Low/Medium/High)
6. **Class Diagram** — Mermaid classDiagram mostrando classes novas/modificadas com relacionamentos
7. **Method Signatures** — Por classe nova: method name, parameters, return type, visibility
8. **Dependency Direction Validation** — Tabela mostrando que o fluxo inbound→domain→outbound é respeitado
9. **Database Schema Changes** — (Condicional: `{{DATABASE}} != none`) Migration skeleton, entity mapping, indexes
10. **API Changes** — (Condicional: interfaces incluem rest/grpc/graphql) Endpoints novos/modificados com request/response DTOs
11. **Event Changes** — (Condicional: `eventDriven == true`) Novos eventos, topic names, payload schemas
12. **Configuration Changes** — Novas properties, environment variables
13. **TDD Strategy** — Mapeamento de classes para cenários do test plan (UT-N, AT-N, IT-N references)
14. **Architecture Decisions** — Mini-ADRs: Context, Decision, Rationale, Consequences
15. **Integration Points** — Sistemas externos, protocolos, SLOs
16. **Risk Assessment** — Tabela: Risk | Probability | Impact | Mitigation
17. **Language-Specific Considerations** — `{{LANGUAGE}}`/`{{FRAMEWORK}}` specific patterns, idioms, libraries

#### Template 2: `_TEMPLATE-TEST-PLAN.md`

Extrair e padronizar o formato que hoje está inline no `x-test-plan/SKILL.md`:
1. **Header** — Story ID, Date, Test Framework (`{{TEST_FRAMEWORK}}`), Language (`{{LANGUAGE}}`), Template Version
2. **Summary** — AT count, UT count, IT count, estimated coverage
3. **Acceptance Tests (Outer Loop)** — Por AT-N: Gherkin ref, status (Pending/Green), components, depends-on, parallel
4. **Unit Tests (Inner Loop — TPP Order)** — Por UT-N: test name, implementation hint, transform, TPP level, components, depends-on, parallel
5. **Integration Tests** — Por IT-N: description, components, depends-on, parallel
6. **Coverage Estimation Table** — Tabela: Class | Public Methods | Branches | Est. Tests | Line % | Branch %
7. **Risks and Gaps** — Cenários difíceis de testar, gaps de coverage antecipados
8. **Language-Specific Notes** — Frameworks de teste, assertion libraries, fixture patterns

#### Template 3: `_TEMPLATE-ARCHITECTURE-PLAN.md`

Extrair o formato que hoje está inline no `x-dev-architecture-plan/SKILL.md`:
1. **Header** — Story ID, Epic ID, Plan Level (Full/Simplified/Skip), Date, Author, Template Version
2. **Executive Summary** — Contexto e decisões arquiteturais
3. **Component Diagram** — Mermaid graph
4. **Sequence Diagrams** — Mermaid sequenceDiagram (1+ diagramas)
5. **Deployment Diagram** — Mermaid graph (condicional: se infra relevante)
6. **External Connections** — Tabela: System | Protocol | Direction | SLO
7. **Architecture Decisions** — Mini-ADRs com Context/Decision/Rationale/Consequences/Story Reference
8. **Technology Stack** — Tabela: Component | Technology | Version | Justification
9. **Non-Functional Requirements** — Tabela: NFR | Target | Measurement | Priority
10. **Data Model** — Mermaid erDiagram (condicional: `{{DATABASE}} != none`)
11. **Observability Strategy** — Traces, metrics, logs, health checks
12. **Resilience Strategy** — Circuit breaker, retry, timeout, fallback
13. **Impact Analysis** — Componentes existentes afetados

#### Template 4: `_TEMPLATE-TASK-BREAKDOWN.md`

Extrair o formato do `x-lib-task-decomposer`:
1. **Header** — Story ID, Mode (TDD-Driven/Layer-Based), Date, Template Version
2. **Summary** — Total tasks, parallelizable tasks, estimated effort
3. **Dependency Graph** — Mermaid graph de dependências entre tasks
4. **Tasks Table** — Por TASK-N: Test Scenario Ref, TPP Level, Type (UT/AT/IT), Phase (RED/GREEN/REFACTOR), Layer, Components, Parallel (Y/N), Depends On, Tier, Budget
5. **Escalation Notes** — Tasks sinalizadas para escalação

**Critérios de Aceitação (DoD)**:

- [ ] 4 arquivos de template criados em `java/src/main/resources/shared/templates/`
- [ ] `_TEMPLATE-IMPLEMENTATION-PLAN.md` contém as 17 seções obrigatórias listadas
- [ ] `_TEMPLATE-TEST-PLAN.md` contém as 8 seções obrigatórias listadas
- [ ] `_TEMPLATE-ARCHITECTURE-PLAN.md` contém as 13 seções obrigatórias listadas
- [ ] `_TEMPLATE-TASK-BREAKDOWN.md` contém as 5 seções obrigatórias listadas
- [ ] Seções condicionais estão claramente marcadas com `<!-- CONDITIONAL: {condition} -->`
- [ ] Todos os headers incluem Template Version field
- [ ] Line coverage ≥ 95%, branch coverage ≥ 90% para código novo (se aplicável)

**Gherkin**:

```gherkin
Feature: Templates de artefatos de planejamento

  Cenario: Template de implementation plan contém todas as seções obrigatórias
    DADO o arquivo "_TEMPLATE-IMPLEMENTATION-PLAN.md" em shared/templates/
    QUANDO o conteúdo é validado
    ENTÃO todas as 17 seções obrigatórias estão presentes
    E seções condicionais estão marcadas com "<!-- CONDITIONAL:"
    E o header inclui campo "Template Version"

  Cenario: Template de test plan segue formato TPP
    DADO o arquivo "_TEMPLATE-TEST-PLAN.md" em shared/templates/
    QUANDO o conteúdo é validado
    ENTÃO a seção "Unit Tests" inclui coluna "TPP Level"
    E a seção "Acceptance Tests" inclui coluna "Gherkin ref"
    E a seção "Coverage Estimation" inclui colunas "Line %" e "Branch %"

  Cenario: Templates são copiados verbatim sem renderização
    DADO os 4 templates de planejamento
    QUANDO o assembler processa
    ENTÃO os tokens "{{LANGUAGE}}" e "{{FRAMEWORK}}" permanecem no output
    E nenhum placeholder é resolvido durante a geração
```

---

### STORY-0002: Templates de Avaliação de Segurança e Compliance

**Título**: Criação de 2 templates padronizados para assessments de segurança e compliance

**Tipo**: Feature — Template Files

**Prioridade**: Alta

**Dependências**: Nenhuma. Paralela com STORY-0001.

**Contexto técnico**:
A Phase 1E do `x-dev-lifecycle` produz avaliações de segurança e compliance em um único passo,
sem template padronizado. O output varia e frequentemente omite categorias OWASP, classificação
de dados, ou requisitos regulatórios. Separar em dois templates distintos (security + compliance)
melhora a completude e permite ativação condicional do compliance assessment.

**Escopo de implementação**:

#### Template 5: `_TEMPLATE-SECURITY-ASSESSMENT.md`

1. **Header** — Story ID, Assessor (Security Engineer), Date, Template Version
2. **Data Classification** — Tabela: Data Element | Classification (Public/Internal/Confidential/Restricted) | Handling Requirements
3. **Encryption Requirements** — At-rest, in-transit, key management
4. **Authentication & Authorization** — Auth flows impactados, novas permissões
5. **Input Validation** — Novas superfícies de ataque, regras de validação
6. **Audit Logging Requirements** — Eventos para audit log, política de retenção
7. **OWASP Top 10 Assessment** — Tabela por categoria aplicável: Risk | Mitigation | Status
8. **Dependency Security** — Novas dependências, CVEs conhecidos
9. **Regulatory Considerations** — Impactos GDPR/LGPD/PCI-DSS/HIPAA
10. **Risk Matrix** — Tabela: Severity | Likelihood | Impact | Mitigation

#### Template 6: `_TEMPLATE-COMPLIANCE-ASSESSMENT.md`

1. **Header** — Story ID, Active Frameworks, Date, Template Version
2. **Data Classification Impact** — Novos elementos de dados e classificação
3. **Framework-Specific Assessment** — Por framework ativo: Requirement | Status | Evidence
4. **Personal Data Processing** — Novos campos PII, base legal, retenção
5. **Audit Trail Requirements** — O que deve ser logado para compliance
6. **Cross-Border Considerations** — Requisitos de transferência internacional
7. **Remediation Actions** — Mudanças necessárias para conformidade
8. **Sign-off** — Tabela: Framework | Status (Compliant/Non-Compliant/Partial) | Notes

**Critérios de Aceitação (DoD)**:

- [ ] 2 arquivos de template criados em `java/src/main/resources/shared/templates/`
- [ ] `_TEMPLATE-SECURITY-ASSESSMENT.md` contém as 10 seções obrigatórias
- [ ] `_TEMPLATE-COMPLIANCE-ASSESSMENT.md` contém as 8 seções obrigatórias
- [ ] Security assessment inclui OWASP Top 10 como checklist obrigatório
- [ ] Compliance assessment é condicional (inclui nota sobre ativação por flags)
- [ ] Headers incluem Template Version field

**Gherkin**:

```gherkin
Feature: Templates de avaliação de segurança e compliance

  Cenario: Security assessment inclui OWASP Top 10
    DADO o arquivo "_TEMPLATE-SECURITY-ASSESSMENT.md"
    QUANDO o conteúdo é validado
    ENTÃO a seção "OWASP Top 10 Assessment" está presente
    E contém placeholder para cada categoria OWASP relevante

  Cenario: Compliance assessment é ativado condicionalmente
    DADO o arquivo "_TEMPLATE-COMPLIANCE-ASSESSMENT.md"
    QUANDO o conteúdo é validado
    ENTÃO contém nota indicando ativação por flags do config
    E a seção "Framework-Specific Assessment" é condicional
```

---

### STORY-0003: Templates de Review (Specialist, Tech Lead, Dashboard, Remediation)

**Título**: Criação de 4 templates padronizados para artefatos de review

**Tipo**: Feature — Template Files

**Prioridade**: Alta

**Dependências**: Nenhuma. Paralela com STORY-0001 e STORY-0002.

**Contexto técnico**:
O `x-review` lança até 8 subagentes especialistas em paralelo, cada um produzindo um relatório.
O formato atual está embutido inline no SKILL.md e é simples (ENGINEER/STORY/SCORE/STATUS +
PASSED/FAILED/PARTIAL). Não há template para o dashboard consolidado nem para tracking de
remediação. O `x-review-pr` (Tech Lead) tem checklist de 45 pontos mas sem template padronizado.

**Escopo de implementação**:

#### Template 7: `_TEMPLATE-SPECIALIST-REVIEW.md`

1. **Header** — Engineer Type, Story ID, Date, Reviewer Role, Template Version
2. **Review Scope** — Arquivos revisados, resumo do diff
3. **Score Summary** — Score: XX/YY | Status: Approved/Rejected/Partial
4. **Passed Items** — Por item: [ID] Description (points/max) — explanation
5. **Failed Items** — Por item: [ID] Description (0/max) | File:Line | Fix suggestion | Severity (CRITICAL/HIGH/MEDIUM/LOW)
6. **Partial Items** — Por item: [ID] Description (partial/max) | File:Line | Improvement | Severity
7. **Severity Summary** — CRITICAL: N | HIGH: N | MEDIUM: N | LOW: N
8. **Recommendations** — Observações adicionais fora do checklist

#### Template 8: `_TEMPLATE-TECH-LEAD-REVIEW.md`

1. **Header** — Story ID, PR Number, Date, Reviewer (Tech Lead), Score: XX/MAX, Template Version
2. **Decision** — GO / NO-GO
3. **Section Scores** — Tabela: Section (A-K) | Points | Max | Notes
4. **Cross-File Consistency Issues** — Concerns cross-cutting encontrados
5. **Critical Issues** — Must-fix antes do merge
6. **Medium Issues** — Should fix, negociável
7. **Low Issues** — Recomendações
8. **TDD Compliance Assessment** — Test-first pattern, TPP progression, Double-Loop adherence
9. **Specialist Review Validation** — Issues críticos do x-review foram corrigidos?
10. **Verdict** — GO/NO-GO final com rationale

#### Template 9: `_TEMPLATE-CONSOLIDATED-REVIEW-DASHBOARD.md`

1. **Header** — Story ID, Date, Review Round, Template Version
2. **Overall Score** — Total: XXX/YYY (XX%) | OVERALL: APPROVED/REJECTED
3. **Engineer Scores Table** — Tabela: Engineer | Score | Status | Critical | High | Medium | Low
4. **Tech Lead Score** — Score: XX/MAX | Decision: GO/NO-GO
5. **Critical Issues Summary** — Todos os findings críticos de todos os engenheiros, deduplicados
6. **Severity Distribution** — CRITICAL: N | HIGH: N | MEDIUM: N | LOW: N (total)
7. **Remediation Status** — Fixed: N | Pending: N | Deferred: N
8. **Review History** — Tabela: Round | Date | Overall Status | Action Taken
9. **Correction Story** — Link para correction story se gerada

#### Template 10: `_TEMPLATE-REVIEW-REMEDIATION.md`

1. **Header** — Story ID, Review Round, Date, Template Version
2. **Findings Tracker** — Tabela: Finding ID | Engineer | Severity | Description | Status (Open/Fixed/Deferred/Accepted) | Fix Commit SHA | Notes
3. **Remediation Summary** — Fixed: N | Deferred: N | Accepted: N | Remaining: N
4. **Deferred Justifications** — Por finding deferido: rationale, follow-up story
5. **Re-review Results** — Após fixes: novo score, issues restantes

**Critérios de Aceitação (DoD)**:

- [ ] 4 arquivos de template criados em `java/src/main/resources/shared/templates/`
- [ ] Specialist review template tem formato de score parseable (XX/YY)
- [ ] Tech Lead review template tem seções A-K correspondendo ao checklist
- [ ] Dashboard consolidado é cumulativo (Review History com múltiplos rounds)
- [ ] Remediation tracking tem tabela de findings com status tracking
- [ ] Todos os templates têm headers com Template Version

**Gherkin**:

```gherkin
Feature: Templates de review

  Cenario: Specialist review tem score parseable
    DADO o arquivo "_TEMPLATE-SPECIALIST-REVIEW.md"
    QUANDO o conteúdo é validado
    ENTÃO a seção "Score Summary" contém formato "Score: XX/YY | Status: {status}"
    E os valores de status válidos são "Approved", "Rejected", "Partial"

  Cenario: Dashboard consolidado suporta múltiplos rounds
    DADO o arquivo "_TEMPLATE-CONSOLIDATED-REVIEW-DASHBOARD.md"
    QUANDO o conteúdo é validado
    ENTÃO a seção "Review History" é uma tabela com colunas Round, Date, Status, Action
    E a seção "Tech Lead Score" está presente mas marcada como "updated by x-review-pr"

  Cenario: Remediation tracking rastreia status de cada finding
    DADO o arquivo "_TEMPLATE-REVIEW-REMEDIATION.md"
    QUANDO o conteúdo é validado
    ENTÃO a tabela "Findings Tracker" tem colunas: Finding ID, Engineer, Severity, Description, Status, Fix Commit SHA, Notes
    E os valores de Status válidos incluem "Open", "Fixed", "Deferred", "Accepted"
```

---

### STORY-0004: Templates de Orquestração de Épico (Execution Plan, Phase Report)

**Título**: Criação de 2 templates padronizados para orquestração de execução de épicos

**Tipo**: Feature — Template Files

**Prioridade**: Alta

**Dependências**: Nenhuma. Paralela com STORY-0001, 0002 e 0003.

**Contexto técnico**:
O `x-dev-epic-implement` orquestra a execução de épicos inteiros, gerenciando múltiplas stories
em fases paralelas. Ele já salva `execution-state.json` como checkpoint, mas não produz:
(a) um plano de execução legível por humanos antes de iniciar, (b) relatórios intermediários
ao final de cada fase com métricas consolidadas.

**Escopo de implementação**:

#### Template 11: `_TEMPLATE-EPIC-EXECUTION-PLAN.md`

1. **Header** — Epic ID, Title, Date, Total Stories, Total Phases, Template Version
2. **Execution Strategy** — Parallel/Sequential, Skip-Review flag, Resume status
3. **Phase Timeline** — Tabela: Phase N | Stories | Estimated Duration | Prerequisites
4. **Story Execution Order** — Tabela: Order | Story ID | Phase | Dependencies | Critical Path (Y/N) | Parallel Group
5. **Pre-flight Analysis Summary** — Resultados da análise de file overlap (por fase)
6. **Resource Requirements** — Worktree count, estimated context budget per story
7. **Risk Assessment** — Stories bottleneck, análise de caminho crítico
8. **Checkpoint Strategy** — Pontos de resume, definições de integrity gates

#### Template 12: `_TEMPLATE-PHASE-COMPLETION-REPORT.md`

1. **Header** — Epic ID, Phase Number, Phase Name, Start/End timestamps, Template Version
2. **Stories Completed** — Tabela: Story ID | Status | Duration | Commit SHA | Coverage Delta
3. **Integrity Gate Results** — Compilation, tests, coverage thresholds
4. **Findings Summary** — De reviews: CRITICAL/HIGH/MEDIUM/LOW counts
5. **TDD Compliance** — Por story: TDD commits %, TPP progression, acceptance test status
6. **Coverage Delta** — Before/After para esta fase
7. **Blockers Encountered** — Issues que causaram delays ou failures
8. **Next Phase Readiness** — Pré-requisitos atendidos para Phase N+1

**Critérios de Aceitação (DoD)**:

- [ ] 2 arquivos de template criados em `java/src/main/resources/shared/templates/`
- [ ] Execution plan tem tabela de Story Execution Order com coluna Critical Path
- [ ] Phase report tem seção de Integrity Gate Results com compile/test/coverage
- [ ] Templates são úteis tanto em dry-run quanto em execução real

**Gherkin**:

```gherkin
Feature: Templates de orquestração de épico

  Cenario: Epic execution plan contém análise de caminho crítico
    DADO o arquivo "_TEMPLATE-EPIC-EXECUTION-PLAN.md"
    QUANDO o conteúdo é validado
    ENTÃO a tabela "Story Execution Order" tem coluna "Critical Path"
    E a seção "Risk Assessment" referencia stories no caminho crítico

  Cenario: Phase completion report contém integrity gate results
    DADO o arquivo "_TEMPLATE-PHASE-COMPLETION-REPORT.md"
    QUANDO o conteúdo é validado
    ENTÃO a seção "Integrity Gate Results" contém sub-seções: Compilation, Tests, Coverage
    E a seção "Coverage Delta" tem formato Before/After
```

---

### STORY-0005: PlanTemplatesAssembler — Distribuição de Templates na Geração

**Título**: Assembler Java que copia os 12 templates de planejamento/review para o output

**Tipo**: Feature — Java Assembler

**Prioridade**: Alta (habilita todas as stories de modificação de skills)

**Dependências**: STORY-0001, STORY-0002, STORY-0003, STORY-0004

**Contexto técnico**:
Os templates criados nas stories anteriores existem como source em
`java/src/main/resources/shared/templates/`. Para que as skills geradas possam referenciá-los,
eles precisam ser copiados para `.claude/templates/` e `.github/templates/` no diretório de
output. O pattern já existe no `EpicReportAssembler.java` (posição 22 de 23 no pipeline).

**Escopo de implementação**:

Criar `PlanTemplatesAssembler.java` seguindo exatamente o padrão de `EpicReportAssembler`:

1. Implementa interface `Assembler` com método `assemble(ProjectConfig, TemplateEngine, Path)`
2. Carrega cada template do classpath `shared/templates/_TEMPLATE-{TYPE}.md`
3. Valida seções obrigatórias por template (lista estática por tipo)
4. Copia verbatim para `.claude/templates/` e `.github/templates/`
5. Retorna lista de `GeneratedFile` com metadados

Registrar em `AssemblerFactory.java`:
- Novo método `buildPlanTemplatesAssemblers()` ou adição ao grupo CI/CD existente
- Posição: após `EpicReportAssembler` (posição 23 ou posterior)

Templates a copiar (12):
- `_TEMPLATE-IMPLEMENTATION-PLAN.md`
- `_TEMPLATE-TEST-PLAN.md`
- `_TEMPLATE-ARCHITECTURE-PLAN.md`
- `_TEMPLATE-TASK-BREAKDOWN.md`
- `_TEMPLATE-SECURITY-ASSESSMENT.md`
- `_TEMPLATE-COMPLIANCE-ASSESSMENT.md`
- `_TEMPLATE-SPECIALIST-REVIEW.md`
- `_TEMPLATE-TECH-LEAD-REVIEW.md`
- `_TEMPLATE-CONSOLIDATED-REVIEW-DASHBOARD.md`
- `_TEMPLATE-REVIEW-REMEDIATION.md`
- `_TEMPLATE-EPIC-EXECUTION-PLAN.md`
- `_TEMPLATE-PHASE-COMPLETION-REPORT.md`

Validações obrigatórias por template:
- Cada template tem uma lista de seções mandatory headers (## N. Section Name)
- Se uma seção obrigatória está ausente, o assembler loga warning e não copia o template

**Critérios de Aceitação (DoD)**:

- [ ] `PlanTemplatesAssembler.java` criado e compila sem erros
- [ ] Registrado no `AssemblerFactory` na posição correta do pipeline
- [ ] Copia 12 templates para `.claude/templates/` e `.github/templates/`
- [ ] Validação de seções obrigatórias funciona (teste com template incompleto)
- [ ] Golden tests atualizados para incluir os 12 novos templates em todos os profiles
- [ ] Pipeline `mvn verify` passa com os novos golden files
- [ ] Line coverage ≥ 95%, branch coverage ≥ 90%

**Gherkin**:

```gherkin
Feature: PlanTemplatesAssembler distribui templates

  Cenario: Assembler copia 12 templates para ambos os destinos
    DADO os 12 templates de planejamento em shared/templates/
    QUANDO o pipeline de geração executa
    ENTÃO o diretório ".claude/templates/" contém os 12 novos templates
    E o diretório ".github/templates/" contém os 12 novos templates

  Cenario: Assembler valida seções obrigatórias
    DADO um template "_TEMPLATE-IMPLEMENTATION-PLAN.md" sem a seção "Class Diagram"
    QUANDO o assembler processa o template
    ENTÃO um warning é logado indicando "Missing mandatory section: Class Diagram"
    E o template NÃO é copiado para o output

  Cenario: Assembler preserva tokens de placeholder
    DADO os templates contendo "{{LANGUAGE}}" e "{{FRAMEWORK}}"
    QUANDO o assembler copia
    ENTÃO os tokens permanecem inalterados no output
    E nenhum placeholder é resolvido
```

---

### STORY-0006: Pre-Checks e Template References no x-dev-lifecycle

**Título**: Adicionar verificação de idempotência e referência a templates no orquestrador principal

**Tipo**: Enhancement — SKILL.md Modification

**Prioridade**: Alta (skill mais utilizada)

**Dependências**: STORY-0005

**Contexto técnico**:
O `x-dev-lifecycle` é o orquestrador principal com 9 fases. Phase 0 já verifica test plan e
architecture plan, mas não verifica implementation plan, task breakdown, security assessment,
nem compliance assessment. Phases 1B, 1E, 4, 5, 7 produzem artefatos sem referenciar templates.

**Escopo de implementação**:

Modificar `java/src/main/resources/targets/claude/skills/core/x-dev-lifecycle/SKILL.md`:

**Phase 0 — Adicionar pre-checks para TODOS os artefatos**:
- Check `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md` (implementation plan)
- Check `plans/epic-XXXX/plans/tasks-story-XXXX-YYYY.md` (task breakdown)
- Check `plans/epic-XXXX/plans/security-story-XXXX-YYYY.md` (security assessment)
- Check `plans/epic-XXXX/plans/compliance-story-XXXX-YYYY.md` (compliance assessment)
- Lógica: "If file exists AND story_mtime <= plan_mtime: log 'Reusing existing {artifact}', skip"

**Phase 1B — Referência ao template de implementation plan**:
- Adicionar: "Read template at `.claude/templates/_TEMPLATE-IMPLEMENTATION-PLAN.md`. ALL mandatory
  sections MUST be present in the output. Save to `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md`"
- Adicionar instrução `model: opus` para planejamento profundo

**Phase 1E — Split em Security + Compliance**:
- Phase 1E: Security Assessment → salva `plans/epic-XXXX/plans/security-story-XXXX-YYYY.md`
  referenciando `_TEMPLATE-SECURITY-ASSESSMENT.md`
- Phase 1F (nova): Compliance Assessment (condicional: se compliance flags ativas) →
  salva `plans/epic-XXXX/plans/compliance-story-XXXX-YYYY.md` referenciando
  `_TEMPLATE-COMPLIANCE-ASSESSMENT.md`

**Phase 4 — Referência a review templates**:
- Instruir x-review a usar `_TEMPLATE-SPECIALIST-REVIEW.md`
- Após consolidação, gerar dashboard: `plans/epic-XXXX/reviews/dashboard-story-XXXX-YYYY.md`

**Phase 5 — Tracking de remediação**:
- Após fixes, gerar: `plans/epic-XXXX/reviews/remediation-story-XXXX-YYYY.md`

**Phase 7 — Referência ao tech lead template**:
- Instruir x-review-pr a usar `_TEMPLATE-TECH-LEAD-REVIEW.md`
- Atualizar dashboard consolidado com resultados do Tech Lead

**Critérios de Aceitação (DoD)**:

- [ ] Phase 0 verifica existência de 6 artefatos (impl plan, test plan, arch plan, tasks, security, compliance)
- [ ] Phase 0 usa comparação de mtime para staleness detection
- [ ] Phase 1B referencia `_TEMPLATE-IMPLEMENTATION-PLAN.md` e usa `model: opus`
- [ ] Phase 1E produz security assessment separado de compliance
- [ ] Nova Phase 1F produz compliance assessment condicional
- [ ] Phases 4, 5, 7 referenciam templates e produzem dashboard/remediation
- [ ] Fallback graceful quando templates não existem (RULE-012)

**Gherkin**:

```gherkin
Feature: Idempotência e templates no x-dev-lifecycle

  Cenario: Plano existente é reutilizado sem regeneração
    DADO um implementation plan existente em plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md
    E o story file NÃO foi modificado após o plano
    QUANDO x-dev-lifecycle Phase 0 executa
    ENTÃO o log contém "Reusing existing implementation plan"
    E Phase 1B é pulada para este artefato

  Cenario: Plano stale é regenerado
    DADO um implementation plan existente
    E o story file FOI modificado após o plano
    QUANDO x-dev-lifecycle Phase 0 executa
    ENTÃO Phase 1B executa normalmente e sobrescreve o plano

  Cenario: Security e compliance são fases separadas
    DADO uma execução do x-dev-lifecycle
    QUANDO Phase 1E executa
    ENTÃO um arquivo security-story-XXXX-YYYY.md é salvo
    E Phase 1F executa se compliance flags estão ativas
    E um arquivo compliance-story-XXXX-YYYY.md é salvo (se aplicável)
```

---

### STORY-0007: Pre-Check e Template Reference no x-test-plan

**Título**: Adicionar idempotência e referência a template no gerador de test plans

**Tipo**: Enhancement — SKILL.md Modification

**Prioridade**: Média

**Dependências**: STORY-0005

**Contexto técnico**:
O `x-test-plan` sempre regenera o test plan do zero. Quando invocado repetidamente (por retry
ou nova sessão), desperdiça tokens sem benefício se a story não mudou.

**Escopo de implementação**:

Modificar `java/src/main/resources/targets/claude/skills/core/x-test-plan/SKILL.md`:

1. **Pre-check**: No início, verificar se `plans/epic-XXXX/plans/tests-story-XXXX-YYYY.md` existe
   e se story_mtime <= plan_mtime. Se sim, log e retornar o conteúdo existente.
2. **Template reference**: Adicionar instrução para ler `.claude/templates/_TEMPLATE-TEST-PLAN.md`
   e seguir o formato. Usar `model: opus` para qualidade de planejamento.
3. **Fallback**: Se template não existir, usar formato inline atual.
4. **Language-specific**: Adicionar instrução para consultar knowledge packs de testing
   (`skills/testing/references/`) para frameworks de teste específicos.

**Critérios de Aceitação (DoD)**:

- [ ] Pre-check de idempotência implementado com mtime comparison
- [ ] Referência ao template `_TEMPLATE-TEST-PLAN.md` adicionada
- [ ] Fallback graceful quando template não existe
- [ ] Instrução `model: opus` para planejamento

**Gherkin**:

```gherkin
Feature: Idempotência no x-test-plan

  Cenario: Test plan existente é reutilizado
    DADO um test plan existente para a story
    E a story NÃO foi modificada desde o plano
    QUANDO x-test-plan é invocado
    ENTÃO o plano existente é retornado sem regeneração
    E o log contém "Reusing existing test plan"

  Cenario: Test plan segue template quando disponível
    DADO o template "_TEMPLATE-TEST-PLAN.md" existe em .claude/templates/
    QUANDO x-test-plan gera um novo plano
    ENTÃO o output segue a estrutura do template
    E todas as 8 seções obrigatórias estão presentes
```

---

### STORY-0008: Pre-Check e Template Reference no x-dev-architecture-plan

**Título**: Adicionar idempotência e referência a template no planejador arquitetural

**Tipo**: Enhancement — SKILL.md Modification

**Prioridade**: Média

**Dependências**: STORY-0005

**Contexto técnico**:
Similar ao STORY-0007, o `x-dev-architecture-plan` já define 13 seções inline mas não tem
template externo. A verificação de existência existe parcialmente no `x-dev-lifecycle` (Phase 1A
skipping) mas não no próprio skill.

**Escopo de implementação**:

Modificar `java/src/main/resources/targets/claude/skills/core/x-dev-architecture-plan/SKILL.md`:

1. **Pre-check**: Verificar existência e staleness do architecture plan
2. **Template reference**: Instruir a ler `.claude/templates/_TEMPLATE-ARCHITECTURE-PLAN.md`
3. **Fallback**: Usar formato inline atual se template ausente
4. **Validation checklist**: Após geração, validar que todas as 13 seções estão presentes

**Critérios de Aceitação (DoD)**:

- [ ] Pre-check de idempotência implementado
- [ ] Referência ao template adicionada
- [ ] Checklist de validação das 13 seções
- [ ] Fallback graceful

**Gherkin**:

```gherkin
Feature: Idempotência no x-dev-architecture-plan

  Cenario: Architecture plan existente é reutilizado
    DADO um architecture plan existente para a story
    E a story NÃO foi modificada desde o plano
    QUANDO x-dev-architecture-plan é invocado
    ENTÃO o plano existente é retornado

  Cenario: Architecture plan segue template com todas as seções
    DADO o template existe
    QUANDO um novo plano é gerado
    ENTÃO todas as 13 seções obrigatórias estão presentes no output
```

---

### STORY-0009: Pre-Check e Template Reference no x-lib-task-decomposer

**Título**: Adicionar idempotência e referência a template no decompositor de tarefas

**Tipo**: Enhancement — SKILL.md Modification

**Prioridade**: Média

**Dependências**: STORY-0005

**Contexto técnico**:
O `x-lib-task-decomposer` é uma lib interna chamada pelo `x-dev-lifecycle` na decomposição
de tasks. Não tem pre-check nem template externo.

**Escopo de implementação**:

Modificar o SKILL.md do task decomposer (em `lib/`):

1. **Pre-check**: Verificar `plans/epic-XXXX/plans/tasks-story-XXXX-YYYY.md`
2. **Template reference**: Instruir a ler `_TEMPLATE-TASK-BREAKDOWN.md`
3. **Fallback**: Formato inline se template ausente

**Critérios de Aceitação (DoD)**:

- [ ] Pre-check implementado
- [ ] Referência ao template
- [ ] Fallback graceful

**Gherkin**:

```gherkin
Feature: Idempotência no task decomposer

  Cenario: Task breakdown existente é reutilizado
    DADO um task breakdown existente
    E a story NÃO foi modificada
    QUANDO o decompositor é invocado
    ENTÃO o breakdown existente é retornado
```

---

### STORY-0010: Templates e Dashboard Consolidado no x-review

**Título**: Padronizar output de reviews de especialistas e gerar dashboard consolidado

**Tipo**: Enhancement — SKILL.md Modification

**Prioridade**: Alta

**Dependências**: STORY-0005

**Contexto técnico**:
O `x-review` lança 8 subagentes especialistas em paralelo. Cada subagente recebe o formato
de output inline no prompt. Após receber os resultados, não há consolidação em dashboard
nem tracking de remediação. O formato dos reports varia entre subagentes.

**Escopo de implementação**:

Modificar `java/src/main/resources/targets/claude/skills/core/x-review/SKILL.md`:

**Phase 2 — Subagent prompts**:
- Adicionar ao prompt de cada subagente: "Read template at
  `.claude/templates/_TEMPLATE-SPECIALIST-REVIEW.md` for required output format.
  ALL sections MUST follow the template structure exactly."
- Substituir formato inline por referência ao template

**Phase 3c — Consolidação**:
- Após salvar reports individuais, gerar dashboard consolidado:
  `plans/epic-XXXX/reviews/dashboard-story-XXXX-YYYY.md`
  usando `_TEMPLATE-CONSOLIDATED-REVIEW-DASHBOARD.md`
- Preencher Engineer Scores Table com resultados de cada especialista
- Calcular Overall Score e Severity Distribution

**Phase 3e (nova) — Remediation tracking**:
- Gerar `plans/epic-XXXX/reviews/remediation-story-XXXX-YYYY.md`
  usando `_TEMPLATE-REVIEW-REMEDIATION.md`
- Pre-popular Findings Tracker com todos os findings FAILED/PARTIAL de todos os engenheiros

**Critérios de Aceitação (DoD)**:

- [ ] Subagentes recebem referência ao template no prompt
- [ ] Reports individuais seguem formato padronizado com score parseable
- [ ] Dashboard consolidado é gerado após Phase 3
- [ ] Remediation tracking é gerado com findings pre-populados
- [ ] Fallback graceful quando templates não existem

**Gherkin**:

```gherkin
Feature: Reviews padronizados e dashboard consolidado

  Cenario: Subagentes produzem reports com formato padronizado
    DADO os templates de review existem em .claude/templates/
    QUANDO x-review executa com 8 especialistas
    ENTÃO cada report segue o formato de _TEMPLATE-SPECIALIST-REVIEW.md
    E cada report tem score no formato "XX/YY"

  Cenario: Dashboard consolidado é gerado
    DADO os 8 reports de especialistas foram salvos
    QUANDO a Phase 3c de consolidação executa
    ENTÃO um arquivo dashboard-story-XXXX-YYYY.md é salvo
    E contém tabela com scores de todos os engenheiros
    E calcula Overall Score como soma de todos os scores

  Cenario: Remediation tracking é gerado com findings
    DADO 3 findings FAILED e 2 PARTIAL foram identificados
    QUANDO Phase 3e executa
    ENTÃO remediation-story-XXXX-YYYY.md é gerado
    E contém 5 entries na Findings Tracker com status "Open"
```

---

### STORY-0011: Template e Dashboard Update no x-review-pr

**Título**: Padronizar output do Tech Lead review e atualizar dashboard consolidado

**Tipo**: Enhancement — SKILL.md Modification

**Prioridade**: Alta

**Dependências**: STORY-0005, STORY-0010

**Contexto técnico**:
O `x-review-pr` (Tech Lead) é executado após o `x-review` (especialistas). Precisa ler os
reports de especialistas, produzir seu próprio report padronizado, e atualizar o dashboard
consolidado criado pelo `x-review`.

**Escopo de implementação**:

Modificar `java/src/main/resources/targets/claude/skills/core/x-review-pr/SKILL.md`:

1. **Template reference**: Instruir a ler `_TEMPLATE-TECH-LEAD-REVIEW.md` para formato de output
2. **Dashboard update**: Após salvar o report, atualizar o dashboard consolidado existente
   adicionando o Tech Lead Score e atualizando Overall Score
3. **Remediation update**: Atualizar remediation tracking marcando findings corrigidos como "Fixed"
   baseado no review do Tech Lead

**Critérios de Aceitação (DoD)**:

- [ ] Report segue formato de `_TEMPLATE-TECH-LEAD-REVIEW.md`
- [ ] Dashboard consolidado é atualizado com Tech Lead Score
- [ ] Remediation tracking é atualizado com status de findings
- [ ] Fallback graceful

**Gherkin**:

```gherkin
Feature: Tech Lead review padronizado e dashboard update

  Cenario: Tech Lead review segue template
    DADO o template "_TEMPLATE-TECH-LEAD-REVIEW.md" existe
    QUANDO x-review-pr executa
    ENTÃO o report segue o formato do template
    E contém seções A-K com scores

  Cenario: Dashboard consolidado é atualizado
    DADO um dashboard existente de reviews de especialistas
    QUANDO x-review-pr executa e salva seu report
    ENTÃO o dashboard é atualizado com Tech Lead Score
    E o Review History adiciona novo round
```

---

### STORY-0012: Epic Execution Plan e Phase Reports no x-dev-epic-implement

**Título**: Salvar plano de execução do épico e relatórios intermediários por fase

**Tipo**: Enhancement — SKILL.md Modification

**Prioridade**: Alta

**Dependências**: STORY-0005

**Contexto técnico**:
O `x-dev-epic-implement` já salva `execution-state.json` para checkpoint, mas não produz
artefatos legíveis por humanos antes de iniciar (plano de execução) nem ao final de cada fase
(relatório intermediário). No dry-run, a análise é exibida mas não salva.

**Escopo de implementação**:

Modificar `java/src/main/resources/targets/claude/skills/core/x-dev-epic-implement/SKILL.md`:

**Phase 0 — Salvar Epic Execution Plan**:
- Após parsing do implementation map e antes de iniciar execução
- Salvar `plans/epic-XXXX/plans/epic-execution-plan-XXXX.md`
  usando `_TEMPLATE-EPIC-EXECUTION-PLAN.md`
- Em dry-run: salvar o plano e encerrar (o plano é o principal output do dry-run)
- Criar diretório `plans/epic-XXXX/reports/` se não existir

**Integrity Gates — Salvar Phase Completion Reports**:
- Ao final de cada fase (após integrity gate)
- Salvar `plans/epic-XXXX/reports/phase-N-completion-XXXX.md`
  usando `_TEMPLATE-PHASE-COMPLETION-REPORT.md`
- Incluir métricas coletadas: stories completed, coverage delta, findings count

**Critérios de Aceitação (DoD)**:

- [ ] Epic execution plan é salvo antes do início da execução
- [ ] Em dry-run, o plano é salvo como artefato principal
- [ ] Phase completion reports são salvos ao final de cada fase
- [ ] Diretório `reports/` é criado automaticamente
- [ ] Plano de execução inclui análise de caminho crítico
- [ ] Phase reports incluem integrity gate results

**Gherkin**:

```gherkin
Feature: Plano de execução e relatórios de fase

  Cenario: Epic execution plan é salvo no início
    DADO um épico com 10 stories em 3 fases
    QUANDO x-dev-epic-implement inicia Phase 0
    ENTÃO plans/epic-XXXX/plans/epic-execution-plan-XXXX.md é criado
    E contém tabela com ordem de execução das 10 stories
    E identifica stories no caminho crítico

  Cenario: Dry-run salva apenas o plano
    DADO flag --dry-run ativado
    QUANDO x-dev-epic-implement executa
    ENTÃO o execution plan é salvo
    E nenhuma story é executada
    E o log indica "Dry-run complete. Execution plan saved."

  Cenario: Phase completion report ao final de cada fase
    DADO a Phase 0 de um épico completou com 3 stories
    QUANDO o integrity gate da Phase 0 passa
    ENTÃO plans/epic-XXXX/reports/phase-0-completion-XXXX.md é criado
    E contém status de cada story, coverage delta, e TDD compliance
```

---

### STORY-0013: Pre-Check e Template References no x-dev-implement

**Título**: Adicionar verificação de planos existentes no implementador simplificado

**Tipo**: Enhancement — SKILL.md Modification

**Prioridade**: Baixa

**Dependências**: STORY-0006, STORY-0007

**Contexto técnico**:
O `x-dev-implement` é uma versão simplificada do `x-dev-lifecycle`, sem fases de review. Já
lê test plan se existente, mas não verifica implementation plan nem outros artefatos.

**Escopo de implementação**:

Modificar `java/src/main/resources/targets/claude/skills/core/x-dev-implement/SKILL.md`:

1. Adicionar pre-check para implementation plan existente
2. Adicionar pre-check para architecture plan existente
3. Se planos existem, usá-los como contexto durante implementação
4. Se não existem, manter comportamento atual (implementação direta)

**Critérios de Aceitação (DoD)**:

- [ ] Pre-checks para implementation plan e architecture plan
- [ ] Planos existentes são usados como contexto
- [ ] Comportamento degradado mantido se planos não existem

**Gherkin**:

```gherkin
Feature: Pre-checks no x-dev-implement

  Cenario: Implementation plan existente é usado como contexto
    DADO um implementation plan existe para a story
    QUANDO x-dev-implement executa
    ENTÃO o plano é lido e usado como guia de implementação
    E o log indica "Using existing implementation plan as context"
```

---

### STORY-0014: Auditoria de Consistência de Diretórios e Naming Conventions

**Título**: Padronizar convenções de diretório e nomes de arquivo entre todas as skills

**Tipo**: Enhancement — Cross-Skill Audit

**Prioridade**: Média

**Dependências**: STORY-0006, STORY-0007, STORY-0008, STORY-0009, STORY-0010, STORY-0011, STORY-0012

**Contexto técnico**:
Após modificar 8 skills independentemente, é necessário auditar a consistência entre elas:
mesmo padrão de diretório, mesma convenção de naming, mesma lógica de pre-check.

**Escopo de implementação**:

Auditar e padronizar:
1. **Diretórios**: `plans/epic-XXXX/plans/`, `plans/epic-XXXX/reviews/`, `plans/epic-XXXX/reports/`
   (novo) — consistente em todas as skills
2. **Naming**: `{type}-story-XXXX-YYYY.md` para story-level, `{type}-epic-XXXX.md` para epic-level
3. **mkdir -p**: Todas as skills criam diretórios necessários antes de salvar
4. **Pre-check lógica**: Mesma lógica de mtime comparison em todas as skills
5. **Fallback**: Todas as skills têm fallback consistente quando templates ausentes
6. **Template reference format**: Mesmo formato de instrução para subagentes

**Critérios de Aceitação (DoD)**:

- [ ] Todas as 8 skills modificadas usam mesmas convenções de diretório
- [ ] Naming convention é consistente: `{type}-story-XXXX-YYYY.md`
- [ ] Todas as skills criam diretórios com `mkdir -p` antes de salvar
- [ ] Lógica de pre-check é idêntica em formato
- [ ] Fallback é consistente em todas as skills

**Gherkin**:

```gherkin
Feature: Consistência entre skills

  Cenario: Todas as skills usam mesma convenção de diretório
    DADO as 8 skills modificadas
    QUANDO auditadas para padrões de diretório
    ENTÃO todas salvam planos em plans/epic-XXXX/plans/
    E todas salvam reviews em plans/epic-XXXX/reviews/
    E x-dev-epic-implement salva reports em plans/epic-XXXX/reports/

  Cenario: Naming convention é consistente
    DADO os artefatos produzidos por cada skill
    QUANDO auditados para naming
    ENTÃO todos seguem o padrão {type}-story-XXXX-YYYY.md para story-level
    E todos seguem {type}-epic-XXXX.md para epic-level
```

---

### STORY-0015: Golden Tests para Novos Templates

**Título**: Atualizar golden tests para incluir os 12 novos templates em todos os profiles

**Tipo**: Test — Golden File Updates

**Prioridade**: Alta

**Dependências**: STORY-0005

**Contexto técnico**:
O projeto usa golden file testing: para cada profile (java-quarkus, java-spring, etc.), existe
um diretório `tests/golden/{profile}/` com a saída esperada da geração. Novos templates devem
ser incluídos nestes golden files.

**Escopo de implementação**:

1. Executar `ia-dev-env generate` para cada profile e capturar os novos templates no output
2. Adicionar os 12 novos templates aos golden files de cada profile
3. Verificar que `mvn verify -Pintegration-tests` passa para todos os profiles
4. Criar ou atualizar `PlanTemplatesAssemblerTest.java` com testes unitários:
   - Teste de cópia para ambos destinos
   - Teste de validação de seções obrigatórias
   - Teste de fallback quando template ausente
   - Teste de preservação de placeholders

**Critérios de Aceitação (DoD)**:

- [ ] Golden files atualizados para todos os profiles
- [ ] `PlanTemplatesAssemblerTest.java` com cobertura de edge cases
- [ ] `mvn verify` passa para todos os profiles
- [ ] Line coverage ≥ 95%, branch coverage ≥ 90%

**Gherkin**:

```gherkin
Feature: Golden tests para templates

  Cenario: Cada profile inclui os 12 novos templates
    DADO um profile de geração (ex: java-quarkus)
    QUANDO a geração executa e compara com golden files
    ENTÃO os 12 novos templates estão presentes em .claude/templates/
    E os 12 novos templates estão presentes em .github/templates/
    E o conteúdo é byte-for-byte idêntico ao source
```

---

### STORY-0016: Atualização de README e CHANGELOG

**Título**: Documentar os novos templates, convenções e catálogo de artefatos

**Tipo**: Documentation

**Prioridade**: Baixa

**Dependências**: STORY-0014

**Contexto técnico**:
O README.md e CLAUDE.md do projeto devem refletir os novos artefatos. O CHANGELOG.md deve
registrar as mudanças.

**Escopo de implementação**:

1. **CLAUDE.md**: Adicionar seção "Plan & Review Templates" com catálogo completo:
   qual template, qual skill produz, onde salva
2. **README.md**: Atualizar seção de geração com contagem atualizada de artefatos
3. **CHANGELOG.md**: Entrada para EPIC-0024 com Added/Changed sections

**Critérios de Aceitação (DoD)**:

- [ ] CLAUDE.md contém catálogo de artefatos
- [ ] README.md com contagem atualizada
- [ ] CHANGELOG.md com entrada da melhoria

**Gherkin**:

```gherkin
Feature: Documentação atualizada

  Cenario: Catálogo de artefatos documentado
    DADO o CLAUDE.md do projeto
    QUANDO atualizado
    ENTÃO contém tabela com: Template | Produzido Por | Salvo Em | Pre-Check
    E lista todos os 12 novos templates
```
