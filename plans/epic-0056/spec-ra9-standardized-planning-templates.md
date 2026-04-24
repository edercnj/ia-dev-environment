# Spec — RA9 Standardized Planning Templates (Rule-Aligned 9-Section Model)

> **Proposta de Épico:** EPIC-0056
> **Target branch:** `epic/0056`
> **Worktree:** `.claude/worktrees/epic-0056/`
> **Destino:** este arquivo é o **input autoritativo** para `/x-epic-decompose`, que produzirá `epic-0056.md`, `story-0056-*.md` e `IMPLEMENTATION-MAP.md`.
> **Autor:** Eder Celeste Nunes Junior
> **Data:** 2026-04-23
> **Versão da spec:** 1.0

---

## 1. Contexto e Problema

### 1.1 Sintoma observado

Os templates de planejamento atuais do projeto `ia-dev-environment` são **inconsistentes entre os três níveis** da hierarquia (Epic → Story → Task). Cada nível apresenta um conjunto de seções diferente, o que gera três sintomas concretos observados em épicos recentes (0043, 0046, 0049, 0053, 0055):

1. **Alucinações de escopo.** Como cada nível tem seções distintas, o LLM preenche "em branco" e improvisa decisões de arquitetura (escolha de package, direção de dependência, forma do endpoint) em vez de herdar invariantes já declaradas no nível superior. Exemplo concreto: em EPIC-0053, três stories recriaram de forma divergente a convenção de pacote de `adapter/inbound/cli/` — o épico não tinha uma seção declarando essa convenção.
2. **Decisões sem rastro.** Escolhas estruturais como *cascade delete*, *timeout de API=5s*, *fallback silencioso vs throw*, *retry count* aparecem no código sem ficar registradas no plan. Hoje, apenas `_TEMPLATE-IMPLEMENTATION-PLAN.md` e `_TEMPLATE-ARCHITECTURE-PLAN.md` possuem campo **Architecture Decisions** com Rationale. `_TEMPLATE-EPIC.md`, `_TEMPLATE-STORY.md` e `_TEMPLATE-TASK.md` **não têm**.
3. **Rules/KPs subutilizadas.** O projeto possui 15 rules numeradas (01, 03, 04, 05, 06, 07, 08, 09, 13, 14, 19, 20, 21, 22, 23, 24) e ~13 knowledge packs (`x-lib-*`, `x-internal-*`), mas os templates de plan **não têm seções ancoradas** em cada uma. Resultado: ao redigir um plan, o LLM não sabe que "aqui entra Rule 06 (segurança)" porque não existe um slot nomeado.

### 1.2 Diagnóstico técnico

Auditoria dos 6 templates de planejamento em `java/src/main/resources/shared/templates/`:

| Template | Seções `##` relevantes | Tem Rationale? | Tem Packages (Hexagonal)? | Tem Endpoints? |
| :--- | :--- | :---: | :---: | :---: |
| `_TEMPLATE-EPIC.md` | Visão Geral · Anexos · Qualidade Global · Regras Transversais · Índice de Histórias | ❌ | ❌ | ❌ |
| `_TEMPLATE-STORY.md` | Dependências · Regras · Descrição · Qualidade Local · Contratos de Dados · Diagramas · Critérios de Aceite · Tasks | ❌ | ❌ | ⚠️ só Data Contracts |
| `_TEMPLATE-TASK.md` | Objetivo · Contratos I/O · DoD · Dependências · Plano | ❌ | ❌ | ❌ |
| `_TEMPLATE-IMPLEMENTATION-PLAN.md` | Affected Layers · Classes · Dep Direction · API Changes · **Architecture Decisions** · Risk | ✅ | ⚠️ só Dependency Direction | ✅ |
| `_TEMPLATE-ARCHITECTURE-PLAN.md` | Component/Sequence/Deploy Diagrams · **Architecture Decisions** · NFRs · Observability · Resilience | ✅ | ❌ | ❌ |
| `_TEMPLATE-SERVICE-ARCHITECTURE.md` | Container Diagram · ADR links · Data Model · NFRs | ✅ | ⚠️ Container Diagram | ❌ |

**Conclusão**: existe **duplicação parcial** (Architecture Decisions em 3 templates) e **ausência total** de seções padronizadas para (a) estrutura de packages hexagonais, (b) Decision Rationale em Epic/Story/Task, (c) materialização de SOLID.

### 1.3 Diagnóstico do fluxo de skills

`/x-epic-orchestrate` e `/x-epic-implement` são **dois fluxos paralelos e independentes**:

| Skill | Papel | Saída principal |
| :--- | :--- | :--- |
| `/x-epic-decompose` | Quebra spec em epic + stories + map | `epic-XXXX.md`, `story-*.md`, `IMPLEMENTATION-MAP.md` |
| `/x-epic-orchestrate` | (opcional, manual) Orquestra `/x-story-plan` em paralelo para todas as stories | `execution-state.json` com `planningStatus=READY/NOT_READY` |
| `/x-epic-implement` | Executa TDD por story→task, cria PRs, merge | Commits, PRs, relatórios de coverage |

`x-epic-implement` **lê** o `execution-state.json` deixado por `x-epic-orchestrate` como gate opcional, mas **não o invoca**. Consequência: qualquer mudança de template precisa ser consumida por **todos os quatro** skills de plan (`x-epic-create`, `x-epic-decompose`, `x-story-plan`, `x-task-plan`) para que os artefatos gerados em qualquer ponto do fluxo sejam coerentes.

### 1.4 Origem do problema

Os templates atuais evoluíram organicamente ao longo de 55 épicos. Novos templates (ARCHITECTURE-PLAN em EPIC-0031, SERVICE-ARCHITECTURE em EPIC-0034) adicionaram o conceito de "Architecture Decisions com Rationale" de forma isolada, sem retrofit nos templates de primeiro nível (EPIC/STORY/TASK). A **Rule 04 (Architecture Summary)** define que o projeto segue arquitetura hexagonal, mas **nenhum template obriga** a declarar a decomposição em `domain/`, `application/`, `adapter/inbound/`, `adapter/outbound/`, `infrastructure/`. A validação ocorre post-hoc via `LifecycleIntegrityAuditTest` (EPIC-0046) e `x-parallel-eval` (EPIC-0041), mas reativamente — depois do plan já estar escrito.

---

## 2. Objetivo

Padronizar os templates de plan de Epic/Story/Task em um **modelo único de 9 seções fixas ancoradas em Rules e Knowledge Packs**, chamado **RA9 (Rule-Aligned 9-Section)**, reduzindo alucinações ao forçar que cada nível cubra as mesmas categorias de invariantes arquiteturais.

Resultados esperados:

1. **Uniformidade de seções** entre Epic/Story/Task — profundidade varia, categorias não.
2. **Decision Rationale obrigatória** em Epic e Story (opcional em Task) com formato determinístico `Decisão → Motivo → Alternativa descartada → Consequência`.
3. **Seção de Packages Hexagonais** presente em todos os níveis, listando os packages em `domain/ · application/ · adapter/inbound/ · adapter/outbound/ · infrastructure/` que serão criados/tocados, com validação de direção de dependência (Rule 04).
4. **Mapeamento explícito rule ↔ seção**, documentado no KP fonte-da-verdade `planning-standards-kp`.
5. **Enforcement em CI** via extensão de `LifecycleIntegrityAuditTest` — adição de template ou plan que não atenda ao contrato RA9 falha o build.
6. **Substituição direta** dos templates v1 por v2 (sem janela de coexistência) no mesmo commit, com regeneração em massa de golden files.

### 2.1 Exemplo do resultado esperado — Epic/Story/Task coerentes

Um épico hipotético "EPIC-0060 — Rate Limiting por API Key" gera artefatos coerentes nas 9 seções:

**Epic (estratégico):**

```markdown
## 2. Packages (Hexagonal)
- application/: RateLimitUseCase (novo), UseCaseFactory (tocado)
- adapter/inbound/http: RateLimitInterceptor (novo)
- adapter/outbound/redis: RateLimitCounterRepo (novo)
- infrastructure: RedisConfig (tocado)
- Direção de dependência validada: adapter → application → domain (Rule 04)

## 8. Decision Rationale
- **Decisão:** Sliding window em vez de fixed window.
  **Motivo:** Evita burst no boundary do minuto.
  **Alternativa descartada:** Token bucket — complexidade de tuning do refill rate.
  **Consequência:** Redis storage O(N) por chave em vez de O(1); aceitável até 10k RPS.
```

**Story 0060-0001:**

```markdown
## 2. Packages (Hexagonal) — subset
- application/: RateLimitUseCase (apenas esta story)
- domain/: RateLimitPolicy (value object)

## 8. Decision Rationale — feature-level
- **Decisão:** Policy como VO imutável em vez de Entity.
  **Motivo:** Não tem identidade própria; é derivada da API Key.
  **Alternativa descartada:** Entity com ID — overkill para dado read-only.
  **Consequência:** Equals/hashCode por valor, não por ID.
```

**Task 0060-0001-001:**

```markdown
## 2. Packages (Hexagonal) — 1 arquivo
- domain/policy/RateLimitPolicy.java (novo)

## 8. Decision Rationale — N/A (task trivial, criação de VO)
```

O operador lê o épico, desce para a story, desce para a task e **nunca se pergunta** "onde esse componente vai ficar?" ou "por que essa escolha?" — a informação está no mesmo slot em todos os níveis.

---

## 3. Proposta Técnica — Modelo RA9

### 3.1 As 9 seções fixas

| # | Seção | Rule âncora | KP âncora | Conteúdo obrigatório |
| :---: | :--- | :--- | :--- | :--- |
| 1 | **Contexto & Escopo** | 01, 14 | `x-internal-story-load-context` | Por que este trabalho existe, motivadores, o que está fora de escopo |
| 2 | **Packages (Hexagonal)** | 04 | `planning-standards-kp` *(novo)* | Lista enxuta dos packages novos/tocados em `domain/ · application/ · adapter/inbound/ · adapter/outbound/ · infrastructure/` + validação da direção de dependência (sem diagramas — referência a `_TEMPLATE-ARCHITECTURE-PLAN.md` se houver diagrama C4) |
| 3 | **Contratos & Endpoints** | 03 | — | REST/gRPC/eventos: método + path + DTO + status codes. Epic = catálogo; Story = endpoints específicos; Task = 1 handler |
| 4 | **Materialização SOLID / Clean Code** | 03, 04 | `x-lib-audit-rules` | Classes que demonstram SRP/DIP; abstrações criadas; anti-patterns proibidos no escopo |
| 5 | **Quality Gates** | 05 | `x-lib-group-verifier` | Coverage ≥ 95/90, lint level, compile-clean, smoke scope. DoR/DoD embutidos aqui como subseção |
| 6 | **Segurança (OWASP/ASVS)** | 06 | `x-lib-audit-rules` | Ameaças STRIDE relevantes, input validation points, secrets handling |
| 7 | **Observabilidade** | 07 | — | Métricas/logs/traces, correlation IDs, dashboards afetados |
| 8 | **Decision Rationale** ⭐ | 04, 19 | — | Formato `Decisão → Motivo → Alternativa descartada → Consequência`. **Obrigatória em Epic e Story** (audit bloqueia vazio); **opcional em Task** (aceita "N/A") |
| 9 | **Dependências & File Footprint** | 04, EPIC-0041 | `x-internal-story-verify`, `x-parallel-eval` | `write:` / `read:` / `regen:` + IDs de predecessores (stories ou tasks) |

### 3.2 Granularidade por nível

| Seção | Epic (estratégico) | Story (tático) | Task (operacional) |
| :--- | :--- | :--- | :--- |
| 1. Contexto | Objetivo + motivadores | Objetivo da história | 1 frase |
| 2. Packages | Catálogo consolidado das camadas | Subset da story | 1-3 arquivos/classes exatos |
| 3. Contratos | Lista agregada | Endpoints da story | Assinatura do método |
| 4. SOLID | Princípios dominantes | Aplicação na story | Regra específica (ex: "handler não chama repo direto") |
| 5. Quality Gates | Thresholds globais | Override se houver | Comando exato (`mvn test -Dtest=X`) |
| 6. Segurança | Ameaças transversais | OWASP mapeados | Validação de 1 input |
| 7. Observabilidade | Métricas-chave | Logs da feature | 1 log/trace point |
| 8. **Decisões** | Estruturais (schema, eventos) — **obrigatório** | Decisões de feature (cache? fallback?) — **obrigatório** | Decisões locais (lazy/eager, retry count) — **opcional, N/A aceito** |
| 9. Dependências | Stories predecessoras | Tasks predecessoras + footprint | File footprint exato |

### 3.3 Formato fixo da seção 8 (Decision Rationale)

Cada decisão segue o mesmo micro-template de 4 linhas:

```markdown
- **Decisão:** <o que foi decidido, em 1 frase>
  **Motivo:** <por que — constraint, requisito não-funcional, lição aprendida>
  **Alternativa descartada:** <a segunda melhor opção e o trade-off>
  **Consequência:** <o custo que essa decisão impõe adiante>
```

Auditoria CI (`LifecycleIntegrityAuditTest`) verifica, para Epic e Story:
- Presença da seção `## 8. Decision Rationale`
- Pelo menos 1 item com as 4 linhas preenchidas (não placeholder / não `TODO`)
- Regex match em `**Decisão:**`, `**Motivo:**`, `**Alternativa descartada:**`, `**Consequência:**`

Para Task, apenas a presença da seção — o corpo pode ser literalmente `N/A — <motivo curto>`.

### 3.4 Estratégia de migração — substituição direta

Os 3 templates atuais (`_TEMPLATE-EPIC.md`, `_TEMPLATE-STORY.md`, `_TEMPLATE-TASK.md`) são **sobrescritos** pela v2 no mesmo commit da respectiva story. Não há flag de migração, não há v1+v2 coexistindo, não há ADR de versionamento separado.

Consequências aceitas:
- Golden files em `src/test/resources/golden/**` serão regenerados em massa (story 0056-0008).
- Épicos em andamento (0043, 0046, 0055) que **já possuem** markdowns gerados **não são afetados** (markdown existente não é re-renderizado).
- Skills de plan emitem RA9 **a partir do merge deste épico**.

---

## 4. Escopo — Stories Propostas

O épico decompõe-se em **8 stories**. A ordem de dependências segue o grafo:

```
0001 (KP fonte-da-verdade)
  ├─ 0002 (_TEMPLATE-EPIC.md v2)
  ├─ 0003 (_TEMPLATE-STORY.md v2)
  ├─ 0004 (_TEMPLATE-TASK.md v2)
  └─ 0005 (_TEMPLATE-IMPLEMENTATION-PLAN.md: nova seção Package Structure)
        │
        └─ 0006 (atualizar skills x-epic-create/decompose, x-story-plan, x-task-plan)
              │
              └─ 0007 (estender LifecycleIntegrityAuditTest)
                    │
                    └─ 0008 (regenerar golden files + CHANGELOG)
```

**Paralelismo**: 0002, 0003, 0004, 0005 tocam arquivos distintos e podem rodar em paralelo (hard collision check via `x-parallel-eval` — esperado verde). 0006 e 0007 serializados após a conclusão de 0002-0005.

### 4.1 Story 0056-0001 — KP `planning-standards-kp`

**Objetivo**: criar a fonte da verdade das 9 seções em forma de knowledge pack interno (não user-invocable).

**Arquivos**:
- `java/src/main/resources/targets/claude/skills/plan/planning-standards-kp/SKILL.md` (novo, frontmatter `user-invocable: false`)

**Conteúdo do KP**: definição das 9 seções, mapeamento rule ↔ seção, formato fixo da seção 8 (Decision Rationale), tabela de granularidade por nível.

**DoD**: KP aparece na lista de KPs ao rodar `ia-dev-env generate`; referenciável via `@planning-standards-kp` em outros skills.

### 4.2 Story 0056-0002 — `_TEMPLATE-EPIC.md` v2

**Objetivo**: reescrever o template do épico com as 9 seções.

**Arquivos**:
- `java/src/main/resources/shared/templates/_TEMPLATE-EPIC.md` (sobrescrito)

**Placeholders esperados** (alguns já existem, outros novos):
- Seção 2: `{{PACKAGES_DOMAIN}}`, `{{PACKAGES_APPLICATION}}`, `{{PACKAGES_ADAPTER_INBOUND}}`, `{{PACKAGES_ADAPTER_OUTBOUND}}`, `{{PACKAGES_INFRASTRUCTURE}}`, `{{DEPENDENCY_DIRECTION_NOTE}}`
- Seção 8: `{{DECISION_RATIONALE}}` (repetido via `{{#each}}` para N decisões)

**DoD**: template passa pelo LifecycleIntegrityAuditTest estendido (story 0056-0007).

### 4.3 Story 0056-0003 — `_TEMPLATE-STORY.md` v2

**Objetivo**: reescrever o template de story com as 9 seções.

**Arquivos**:
- `java/src/main/resources/shared/templates/_TEMPLATE-STORY.md` (sobrescrito)

**Preservar**: seção "Critérios de Aceite (Gherkin)" do template atual — vira **subseção 5.2 (Quality Gates → Acceptance Criteria)**.

**DoD**: story gerada por `/x-epic-decompose` sobre spec sintética tem todas as 9 seções preenchidas sem `TODO`.

### 4.4 Story 0056-0004 — `_TEMPLATE-TASK.md` v2

**Objetivo**: reescrever o template de task com as 9 seções (rationale opcional, N/A aceito).

**Arquivos**:
- `java/src/main/resources/shared/templates/_TEMPLATE-TASK.md` (sobrescrito)

**Preservar**: "Contratos I/O" do template atual — vira **subseção 3 (Contratos & Endpoints) refinada**.

**DoD**: task gerada por `/x-task-plan` tem 9 seções; seção 8 aceita `N/A — <motivo>`.

### 4.5 Story 0056-0005 — `_TEMPLATE-IMPLEMENTATION-PLAN.md` ganha "Package Structure"

**Objetivo**: adicionar seção `## Package Structure` ao template de plano de implementação, alinhada à seção 2 do RA9.

**Arquivos**:
- `java/src/main/resources/shared/templates/_TEMPLATE-IMPLEMENTATION-PLAN.md` (editado, não sobrescrito)

**Razão**: o IMPLEMENTATION-PLAN é mais detalhado que a Story; precisa de subseções por camada com classes específicas. Não vira RA9 completo (fica como "referência técnica" apontada pela seção 2 da Story).

### 4.6 Story 0056-0006 — atualizar skills de plan

**Objetivo**: fazer `x-epic-create`, `x-epic-decompose`, `x-story-plan`, `x-task-plan` consumirem os templates v2 e emitirem os placeholders novos.

**Arquivos**:
- `java/src/main/resources/targets/claude/skills/plan/x-epic-create/SKILL.md`
- `java/src/main/resources/targets/claude/skills/plan/x-epic-decompose/SKILL.md`
- `java/src/main/resources/targets/claude/skills/plan/x-story-plan/SKILL.md`
- `java/src/main/resources/targets/claude/skills/plan/x-task-plan/SKILL.md`

**DoD**: smoke-test — rodar `/x-epic-decompose` sobre uma spec sintética gera artefatos com as 9 seções.

### 4.7 Story 0056-0007 — estender `LifecycleIntegrityAuditTest`

**Objetivo**: adicionar regras ao audit para validar conformidade RA9.

**Regras novas**:
- `RA9_SECTIONS_MISSING`: Epic/Story/Task sem uma das 9 seções.
- `RA9_RATIONALE_EMPTY`: Epic ou Story com seção 8 presente mas sem decisão preenchida (ou com placeholder `{{...}}`).
- `RA9_PACKAGES_MISSING`: seção 2 presente mas sem listar pelo menos uma das camadas.

**Arquivos**:
- `java/src/test/java/dev/iadev/audit/LifecycleIntegrityAuditTest.java`
- `java/src/test/resources/audits/lifecycle-integrity-baseline.txt` (atualizar baseline)

**DoD**: audit falha com mensagem clara quando um plan violando RA9 é adicionado; passa com plans RA9 conformes.

### 4.8 Story 0056-0008 — regenerar golden files + CHANGELOG

**Objetivo**: regenerar golden files após as mudanças de templates e skills; documentar a mudança.

**Arquivos**:
- `src/test/resources/golden/**` (regeneração em massa)
- `CHANGELOG.md` (entrada "Changed — RA9 Standardized Planning Templates")

**DoD**: `mvn test` passa; `ia-dev-env generate` produz diff limpo contra golden.

---

## 5. Mapeamento Rule ↔ Seção

| Rule | Seção(ões) que alimenta | Uso na auditoria |
| :---: | :--- | :--- |
| 01 — Project Identity | 1. Contexto | Verifica que o plan menciona o identificador do épico/projeto |
| 03 — Coding Standards | 3, 4 | Valida nomenclatura de packages/classes |
| 04 — Architecture Summary | 2, 4, 9 | Valida direção de dependência hexagonal |
| 05 — Quality Gates | 5 | Verifica thresholds ≥ 95/90 |
| 06 — Security Baseline | 6 | Valida presença de seção de ameaças |
| 07 — Operations Baseline | 7 | Valida presença de observability |
| 09 — Branching Model | 5 (DoD) | Valida referência a Git Flow |
| 13 — Skill Invocation | 5 (DoR) | Valida uso de skills no plan |
| 14 — Scope | 1 | Valida "fora de escopo" explícito |
| 19 — Backward Compatibility | 8 | Permite decisões que quebram compat serem auditadas |
| 21 — Epic Branch Model | 5 (DoD) | Valida branch `epic/XXXX` |
| 22 — Skill Visibility | — | KP é `user-invocable: false` |
| 24 — Execution Integrity | 5 (DoD) | Valida que evidências serão produzidas |

---

## 6. Arquivos Críticos

### 6.1 Substituição direta (v1 → v2 no mesmo commit)

- `java/src/main/resources/shared/templates/_TEMPLATE-EPIC.md`
- `java/src/main/resources/shared/templates/_TEMPLATE-STORY.md`
- `java/src/main/resources/shared/templates/_TEMPLATE-TASK.md`

### 6.2 Edição incremental

- `java/src/main/resources/shared/templates/_TEMPLATE-IMPLEMENTATION-PLAN.md`

### 6.3 Criação

- `java/src/main/resources/targets/claude/skills/plan/planning-standards-kp/SKILL.md`

### 6.4 Atualização de skills

- `java/src/main/resources/targets/claude/skills/plan/x-epic-create/SKILL.md`
- `java/src/main/resources/targets/claude/skills/plan/x-epic-decompose/SKILL.md`
- `java/src/main/resources/targets/claude/skills/plan/x-story-plan/SKILL.md`
- `java/src/main/resources/targets/claude/skills/plan/x-task-plan/SKILL.md`

### 6.5 Audit e regeneração

- `java/src/test/java/dev/iadev/audit/LifecycleIntegrityAuditTest.java`
- `java/src/test/resources/audits/lifecycle-integrity-baseline.txt`
- `src/test/resources/golden/**` (regeneração em massa)
- `CHANGELOG.md`

---

## 7. Non-Goals (Fora de Escopo)

O que este épico **não** faz:

1. **Não reescreve** `_TEMPLATE-ARCHITECTURE-PLAN.md`, `_TEMPLATE-TEST-PLAN.md`, `_TEMPLATE-SECURITY-ASSESSMENT.md`, `_TEMPLATE-COMPLIANCE-ASSESSMENT.md`, nem templates de review/report — esses ficam como "referências técnicas" apontadas por seções específicas do RA9.
2. **Não adiciona** seção de diagrama C4 inline no Epic/Story/Task — diagramas ficam em `_TEMPLATE-ARCHITECTURE-PLAN.md` quando necessário.
3. **Não cria** ADR de versionamento de templates — a decisão de substituição direta elimina essa necessidade.
4. **Não retrofita** épicos já gerados (0001-0055) — apenas os novos a partir do merge.
5. **Não altera** `_TEMPLATE-EPIC-EXECUTION-PLAN.md`, `_TEMPLATE-IMPLEMENTATION-MAP.md` (output de decomposição, não plan em si).

---

## 8. Definition of Done (Épico inteiro)

- [ ] Todas as 8 stories em status `DONE` com PRs mergeados em `epic/0056`
- [ ] `mvn test` passa com coverage ≥ 95% linha / ≥ 90% branch
- [ ] `LifecycleIntegrityAuditTest` valida RA9 (bloqueia regressões)
- [ ] Smoke test: rodar `/x-epic-decompose` sobre spec sintética — gera Epic/Story/Task com 9 seções preenchidas
- [ ] Golden files regenerados e `ia-dev-env generate` produz diff limpo
- [ ] `CHANGELOG.md` tem entrada "Changed — RA9 Standardized Planning Templates"
- [ ] PR final `epic/0056 → develop` aprovado e mergeado

---

## 9. Decision Rationale (da própria spec)

- **Decisão:** 9 seções fixas em vez de número variável por nível.
  **Motivo:** Uniformidade reduz alucinação; LLM aprende 1 schema, não 3.
  **Alternativa descartada:** Subset de seções por nível (Epic=5, Story=7, Task=3). Mais enxuto mas perde herança explícita.
  **Consequência:** Alguns plans terão seções curtas (ex: Task sem endpoint tem seção 3 curta) — aceitável.

- **Decisão:** Substituição direta em vez de v1+v2 coexistindo.
  **Motivo:** Sem janela de depreciação = sem dívida técnica; menos condicionais nos skills.
  **Alternativa descartada:** ADR-0011 "Template Versioning Strategy" com flag `--template-version=v1|v2`.
  **Consequência:** Golden files regenerados em massa; possível ruído no PR de 0056-0008.

- **Decisão:** Rationale obrigatória em Epic e Story; opcional em Task (N/A aceito).
  **Motivo:** Tasks triviais ("criar DTO", "adicionar import") não têm rationale real; forçar gera lixo.
  **Alternativa descartada:** Obrigatória nos 3 níveis. Excesso de overhead.
  **Consequência:** Audit precisa distinguir nível — complexidade extra na extensão do LifecycleIntegrityAuditTest (regra `RA9_RATIONALE_EMPTY` só se aplica a Epic/Story).

- **Decisão:** Novo KP `planning-standards-kp` em vez de embutir definição nos skills.
  **Motivo:** Fonte da verdade única; skills referenciam via `@planning-standards-kp`.
  **Alternativa descartada:** Definição inline nos 4 skills de plan. Duplicação.
  **Consequência:** Adição de 1 KP ao inventário; convenção de subpasta `plan/planning-standards-kp/`.

- **Decisão:** Packages enxutos (lista + direção) em vez de diagrama C4 inline.
  **Motivo:** Diagramas infla markdowns; ARCHITECTURE-PLAN já tem o C4 quando necessário.
  **Alternativa descartada:** Diagrama de componente inline em toda Story/Task.
  **Consequência:** Stories que precisam de diagrama apontam para `_TEMPLATE-ARCHITECTURE-PLAN.md` via seção 2.

---

## 10. Referências

- Rule 04 — `java/src/main/resources/targets/claude/rules/04-architecture-summary.md`
- Rule 05 — `java/src/main/resources/targets/claude/rules/05-quality-gates.md`
- Rule 06 — `java/src/main/resources/targets/claude/rules/06-security-baseline.md`
- Rule 24 — `java/src/main/resources/targets/claude/rules/24-execution-integrity.md`
- EPIC-0041 — File-Conflict-Aware Parallelism (introduziu `## File Footprint`)
- EPIC-0046 — Lifecycle Integrity Phase 2 (introduziu `LifecycleIntegrityAuditTest`)
- EPIC-0049 — Thin Orchestrators (refatoração que originou parte do problema)
- `_TEMPLATE-IMPLEMENTATION-PLAN.md` — template que já tem Architecture Decisions (referência)
- `_TEMPLATE-ARCHITECTURE-PLAN.md` — template que já tem Architecture Decisions (referência)
- `_TEMPLATE-ADR.md` — formato base para decisões (referência para a seção 8)
