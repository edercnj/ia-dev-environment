# Especificação Técnica: Lifecycle Integrity — Status Propagation & Report Commits

**Autor:** Eder Celeste Nunes Junior
**Data:** 2026-04-16
**Versão:** 0.1 (draft)
**Status:** Em Refinamento
**Épico:** EPIC-0046

---

## 1. Contexto

O gerador `ia-dev-env` expõe uma taxonomia de skills orquestradoras (`x-epic-create`, `x-epic-decompose`, `x-epic-map`, `x-story-plan`, `x-task-plan`, `x-arch-plan`, `x-test-plan` na camada de planejamento; `x-task-implement`, `x-story-implement`, `x-epic-implement` na camada de execução). Toda skill produz artefatos markdown padronizados por templates com um campo `**Status:**` como metadado humano principal:

- `plans/epic-XXXX/epic-XXXX.md` → `**Status:** Em Refinamento`
- `plans/epic-XXXX/story-XXXX-YYYY.md` → `**Status:** Pendente`
- `plans/epic-XXXX/plans/task-TASK-XXXX-YYYY-NNN.md` (schema v2) → `**Status:** Pendente`
- `plans/epic-XXXX/implementation-map-XXXX.md` (ou `IMPLEMENTATION-MAP.md`) → colunas `Planejamento` e `Status`

Paralelamente, cada épico mantém `plans/epic-XXXX/execution-state.json` como telemetria (checkpoint de execução dos orquestradores): story-level `status`, `commitSha`, `prUrl`, `prMergeStatus` e, em schema v2.0, task-level equivalente.

A convenção implícita do projeto assume que os dois planos de informação convergem: o campo `**Status:**` nos markdowns deveria refletir o estado real do trabalho, alinhado ao `execution-state.json`. Essa convergência hoje não acontece.

## 2. Problema

### 2.1 Sintoma principal (GAP-1 — divergência de status)

Nenhuma skill, planejamento ou execução, atualiza o campo `**Status:**` dos artefatos que produz ou consome. Como resultado, o SoT humano (markdown) permanece congelado em `Pendente` / `Em Refinamento` ainda que o ciclo tenha concluído.

**Evidência empírica — EPIC-0024 (artifact-persistence), concluído:**

- `plans/epic-0024-artifact-persistence/execution-state.json` — `finalStatus: "COMPLETE"`; 16 stories com `status: "SUCCESS"`, `prMergeStatus: "MERGED"`, `commitSha` preenchido.
- `plans/epic-0024-artifact-persistence/story-0024-000N.md` (todas as 16) — continuam `**Status:** Pendente`.
- `plans/epic-0024-artifact-persistence/IMPLEMENTATION-MAP.md` — coluna Status inteira em `Pendente` apesar de 100% mergeado.
- `plans/epic-0024-artifact-persistence/epic-0024.md` — permanece `**Status:** Em Refinamento`.

Quantificação: **0% de sync** markdown ↔ checkpoint em épico com 100% de PRs mergeados.

### 2.2 Sintoma secundário (GAP-2 — reports órfãos bloqueando release)

`x-epic-implement` (ver `java/src/main/resources/targets/claude/skills/core/dev/x-epic-implement/SKILL.md:220,233,264,706`) grava arquivos em `plans/epic-XXXX/reports/`:

- `execution-plan-epic-XXXX.md` (pré-execução)
- `phase-report-epic-XXXX.md` (pós-fase)

Essas escritas não são seguidas por invocação de `x-git-commit`. Os arquivos ficam órfãos no working tree.

Consequência: `x-release` tem precondição sempre-obrigatória `VALIDATE_DIRTY_WORKDIR` (ver `x-release/SKILL.md:277-308`) que executa `git status --porcelain` e aborta com `VALIDATE_DIRTY_WORKDIR` se o output for não-vazio. Em releases executados imediatamente após um `x-epic-implement`, essa precondição falha e exige limpeza manual.

### 2.3 Causa raiz

Duas causas estruturais:

1. **Convenção ausente:** nenhuma rule formaliza a obrigação das skills de planejamento/execução atualizarem `**Status:**` e a coluna do implementation-map. Phases documentadas existem (`x-story-implement` Phase 3 "Final Verification + Cleanup" nas linhas 975-987 e `x-epic-implement` Section 1.6b "Markdown Status Sync" nas linhas 1143-1200) mas:
   - `x-story-implement` Phase 3 é skippable via `--skip-verification`.
   - `x-epic-implement` Section 1.6b é documentada mas nunca referenciada pelo Core Loop (chamado apenas "1.6 checkpoint" na linha 700).
   - Phases com status-sync ficam órfãs do caminho happy; silenciosamente não executam.
2. **Reconciliação ausente:** não existe skill nem hook que leia `execution-state.json` e escreva os campos `**Status:**` correspondentes. O checkpoint é "write-only" da perspectiva do markdown SoT.
3. **Commit de reports ausente:** escritas em `plans/epic-XXXX/reports/` não invocam `x-git-commit`. Não há rule que obrigue.

## 3. Objetivo

Fechar GAP-1 e GAP-2 com uma convenção formal (Rule 21), retrofit das skills afetadas, skill opt-in para reconciliação de legados, e CI audit que impede regressão.

### Objetivos específicos

- **O1** — Formalizar Rule 21 `lifecycle-integrity` cobrindo atualização de `**Status:**`, commit de reports e invariante de clean-workdir.
- **O2** — Retrofit de cada skill de planejamento (`x-story-plan`, `x-task-plan`, `x-epic-create`, `x-epic-decompose`, `x-epic-map`, `x-arch-plan`, `x-test-plan`) para atualizar `**Status:** Pendente → Planejada` no MESMO commit em que grava o plan artefact.
- **O3** — Retrofit de `x-task-implement` para atualizar `**Status:** Pendente → Concluída` + coluna Status do map no commit atômico final da task.
- **O4** — Promover `x-story-implement` Phase 3 (status sync + final verification) para não-skippable e cabear `x-epic-implement` Section 1.6b ao Core Loop como `Phase 1.7`.
- **O5** — Retrofit de `x-epic-implement` para invocar `x-git-commit` atomicamente após cada escrita em `plans/epic-XXXX/reports/`.
- **O6** — Introduzir skill opt-in `x-status-reconcile` em `core/ops/` que compare `execution-state.json` vs markdowns, reporte divergência (modo diagnose default) e, com `--apply`, aplique correções + commit (gate interativo Rule 20 do EPIC-0043 quando disponível).
- **O7** — Preservar backward compatibility (Rule 19): status sync é V2-gated via `SchemaVersionResolver`. Épicos `planningSchemaVersion == "1.0"` ou ausente NÃO ganham nem perdem comportamento.
- **O8** — Introduzir `LifecycleIntegrityAuditTest` (CI-blocking) que detecta regressões: (a) phases documentadas mas não referenciadas pelo Core Loop da skill; (b) escritas em `reports/` sem `x-git-commit` subsequente no mesmo SKILL.md; (c) flags `--skip-*` que bypassam status sync no happy path.

## 4. Escopo

### 4.1 Dentro de escopo

- Nova Rule 21 em `java/src/main/resources/targets/claude/rules/21-lifecycle-integrity.md`.
- Matriz de transição `Pendente → Planejada → Em Andamento → Concluída | Cancelada | Bloqueada` documentada no header dos templates `_TEMPLATE-TASK.md`, `_TEMPLATE-STORY.md`, `_TEMPLATE-EPIC.md`.
- Retrofits em SKILL.md das 7 skills de planejamento (O2).
- Retrofit em `x-task-implement/SKILL.md` (O3).
- Retrofit em `x-story-implement/SKILL.md` + `x-epic-implement/SKILL.md` (O4, O5).
- Nova skill `x-status-reconcile` em `core/ops/` (O6) — taxonomia ADR-0003.
- Helpers Java em `dev.iadev.application.lifecycle`:
  - `StatusFieldParser` — regex tolerante a whitespace para `**Status:** <valor>`; escrita atômica via `.tmp` + rename.
  - `LifecycleTransitionMatrix` — valida transições permitidas.
  - `LifecycleAuditRunner` — suporte ao `LifecycleIntegrityAuditTest`.
- `LifecycleIntegrityAuditTest` (CI-blocking) conforme O8.
- Golden diff regen para todas as SKILL.md afetadas + templates + rules.
- Smoke test `Epic0046SmokeTest.java` em sandbox git: roda planning → implementation toy → verifica `git status --porcelain` vazio + `**Status:** Concluída` propagado.

### 4.2 Fora de escopo

- Backfill automático de épicos legados (EPIC-0001..0045). Backfill só acontece via `x-status-reconcile --apply` acionado manualmente pelo operador — **não** faz parte da entrega automática deste épico.
- Migração de schema de `execution-state.json` (Rule 19 proíbe upgrade implícito).
- Integração com Jira (o épico segue `--no-jira`; status sync em Jira fica para épico futuro).
- Dashboards de lifecycle (métricas agregadas de progresso ficam fora).
- Alteração do formato de checkpoint — reusar `execution-state.json` existente; no máximo um campo auxiliar `lastStatusSyncedAt` não load-bearing.

## 5. Contrato da skill `x-status-reconcile`

### 5.1 Argumentos

| Argumento | Tipo | Default | Semântica |
| :--- | :--- | :--- | :--- |
| `--epic <XXXX>` | int | — (um de `--epic` ou `--story` obrigatório) | ID do épico a reconciliar. |
| `--story <story-XXXX-YYYY>` | string | — | ID da story (escopo reduzido). |
| `--apply` | flag | — | Aplica correções nos markdowns + commita. Sem `--apply`, apenas diagnose. |
| `--non-interactive` | flag | — | Pula gate interativo (uso em CI); requer confirmação implícita do operador. |
| `--dry-run` | flag | — | Força modo diagnose mesmo com `--apply` presente. |

### 5.2 Exit codes (contrato público estável)

| Código | Nome | Semântica |
| :--- | :--- | :--- |
| 0 | `SUCCESS` | Sem divergência OU divergência detectada em modo diagnose. |
| 10 | `APPLIED` | Modo `--apply`: correções aplicadas com sucesso + commitadas. |
| 20 | `STATUS_SYNC_FAILED` | Falha ao atualizar markdown (arquivo ausente, regex não casa, commit rejeitado). |
| 30 | `STATE_FILE_INVALID` | `execution-state.json` malformado ou ausente. |
| 40 | `STATUS_TRANSITION_INVALID` | Transição requerida viola `LifecycleTransitionMatrix`. |
| 50 | `USER_ABORTED` | Operador escolheu ABORT no gate interativo. |

### 5.3 Contrato de IO

- **stdin:** vazio.
- **stdout:** progress logs + JSON final na última linha:
  ```json
  {
    "status": "APPLIED",
    "epicId": "0024",
    "divergences": [
      {"artifact": "story-0024-0001.md", "from": "Pendente", "to": "Concluida"},
      {"artifact": "IMPLEMENTATION-MAP.md:row-5", "from": "Pendente", "to": "Concluida"}
    ],
    "commitSha": "abc1234"
  }
  ```
- **stderr:** progress logs estruturados.

### 5.4 Edge cases

| Cenário | Comportamento |
| :--- | :--- |
| `execution-state.json` ausente | Exit 30 (`STATE_FILE_INVALID`) com mensagem clara. |
| Markdown com `**Status:** Concluída` e checkpoint com `status: "PENDING"` | Exit 40 (`STATUS_TRANSITION_INVALID`) — divergência suspeita, operador deve investigar. |
| Épico `planningSchemaVersion == "1.0"` | Exit 0, mensagem "legacy epic; skipping sync" — Rule 19. |
| Rate-limit git (raro) | Retry 3× com backoff; após isso, exit 20. |
| Gate interativo + `--non-interactive` | Assume PROCEED, registra no JSON. |

## 6. Regras de Negócio Transversais

| ID | Título | Descrição |
| :--- | :--- | :--- |
| **RULE-046-01** | Source-of-truth invariant | Toda alteração de skill/rule/template/golden em `java/src/main/resources/targets/claude/`. `.claude/**` e `src/test/resources/golden/**` são saídas de `mvn process-resources` — edição manual proibida. |
| **RULE-046-02** | Planning updates status | Toda skill de planejamento que produza um plan artefact DEVE atualizar `**Status:** Pendente → Planejada` do artefato-fonte no MESMO commit do plano. Aplica-se a: `x-story-plan`, `x-task-plan`, `x-arch-plan`, `x-test-plan`, `x-epic-create`, `x-epic-decompose`, `x-epic-map`. |
| **RULE-046-03** | Implementation updates status | Toda skill de execução que termine uma etapa DEVE transicionar `**Status:** → Concluída` + coluna Status do implementation-map no MESMO commit atômico final: `x-task-implement` (task-level); `x-story-implement` Phase 3 (story-level); `x-epic-implement` Phase 1.7 promovida de 1.6b (story-level dentro do epic loop + epic-level ao final). |
| **RULE-046-04** | Status transition is non-skippable | Fases que executam status sync NÃO podem ser puladas por flags opt-out no happy path. Flag `--skip-status-sync` pode existir APENAS para recovery manual, documentada no SKILL.md. CI audit rejeita uso em caminho happy. |
| **RULE-046-05** | Reports are atomically committed | Toda skill que grave em `plans/epic-XXXX/reports/` DEVE invocar `Skill(skill: "x-git-commit", args: "...")` antes de retornar (Rule 13 Pattern 1 INLINE-SKILL). Reports não commitados = falha da fase que os produziu. |
| **RULE-046-06** | Clean workdir invariant | Nenhuma skill pode retornar com `git status --porcelain` não-vazio após sua última fase, salvo quando a skill declara explicitamente "dirty on exit by design" no frontmatter (nenhuma hoje). Enforcement: smoke test por skill. |
| **RULE-046-07** | Markdown é SoT; state.json é telemetria | `execution-state.json` permanece append-only como telemetria. SoT do status do artefato é o campo `**Status:**` no markdown. Reconciliação é opt-in via `x-status-reconcile`, nunca implícita. |
| **RULE-046-08** | Fail loud on status update failure | Se a atualização de status falhar, a skill aborta com exit ≠ 0 e código `STATUS_SYNC_FAILED`. Nunca swallow. Logs estruturados em stderr incluem o artefato + linha + valor esperado/encontrado. |

## 7. Definition of Ready (Global)

- Source-of-truth em `java/src/main/resources/targets/claude/` respeitado (RULE-046-01); nenhuma edição em `.claude/**` ou `src/test/resources/golden/**`.
- Taxonomia ADR-0003 observada: nova skill `x-status-reconcile` em `core/ops/`.
- Rule 13 invocation patterns respeitados: retrofits que delegam a `x-git-commit` ou outras skills usam Pattern 1 INLINE-SKILL; bare-slash proibido fora de `## Triggers` e `## Examples`.
- Frontmatter `allowed-tools` de cada skill retrofitada inclui `Skill` (se ainda não incluir).
- `execution-state.json` do épico declara `planningSchemaVersion: "2.0"`.
- Próximo slot de rule identificado: `21-lifecycle-integrity.md` (slot 20 consumido por EPIC-0045 ci-watch; slots 10/11/12 reservados para condicionais).
- Precondição dura: EPIC-0045 (ci-watch) não precisa estar mergeado — não há dependência direta, apenas reserva do slot de rule.

## 8. Definition of Done (Global)

- **Cobertura:** ≥ 95% Line, ≥ 90% Branch em todos os helpers Java novos (`StatusFieldParser`, `LifecycleTransitionMatrix`, `LifecycleAuditRunner`).
- **Testes Automatizados:**
  - Unit tests do `StatusFieldParser` cobrindo regex tolerante a whitespace + valores inválidos + arquivo ausente.
  - Unit tests do `LifecycleTransitionMatrix` cobrindo matriz completa (permitidas + proibidas).
  - `LifecycleIntegrityAuditTest` (CI-blocking) — 3 dimensões de audit (O8).
  - Golden diff obrigatório para: nova Rule 21; retrofits em 10 SKILL.md (7 planning + 3 implementation); `x-status-reconcile/SKILL.md` novo; templates atualizados.
  - Smoke test `Epic0046SmokeTest.java` em sandbox git: planning + implementation toy → `git status --porcelain` vazio + `**Status:** Concluída` no markdown + `execution-state.json` alinhado.
  - **Fail-loud test por story:** cada story DEVE ter pelo menos 1 teste que injete falha na atualização de status (arquivo renomeado, regex quebrado, commit rejeitado) e assert exit 20 (`STATUS_SYNC_FAILED`). Silêncio é bug.
  - **Clean-workdir integration test por skill retrofitada:** roda a skill em sandbox, checa `git status --porcelain` vazio no retorno.
- **Smoke Tests:** `mvn process-resources && mvn test` verde; `SkillsAssemblerTest`, `RuleAssemblerTest`, `TemplatesAssemblerTest` não regridem; novo `RuleAssemblerTest.listRules_includesLifecycleIntegrity` obrigatório.
- **Relatório de Cobertura:** JaCoCo agregado no Maven report padrão.
- **Documentação:** Rule 21 criada; CLAUDE.md atualizado no bloco "In progress" referenciando o épico enquanto não concluído; CHANGELOG seção Unreleased com uma entrada por story. `.claude/skills/**`, `.claude/rules/**`, `.claude/templates/**` apenas regenerados via `mvn process-resources`.
- **Backward Compatibility:** Rule 19 respeitada — status sync é V2-gated via `SchemaVersionResolver.resolve() == V2`. Épicos v1 não executam as novas fases. Smoke test adicional prova que um épico v1 toy termina com behaviour legado (sem status update).
- **TDD Compliance:** commits mostram test-first — unit tests dos helpers Java precedem implementação; golden diff precede ou acompanha cada diff de SKILL.md. Tasks em ordem TPP.
- **Double-Loop TDD:** acceptance tests derivados dos cenários Gherkin das stories (outer loop); unit tests dos helpers guiados por TPP (inner loop).
- **Rule 13 Audit:** grep padrão Rule 13 continua retornando 0 matches em `java/src/main/resources/targets/claude/skills/core/` após retrofits.
- **Rule 21 Audit (novo):** `LifecycleIntegrityAuditTest` retorna 0 violations.

## 9. Índice de Histórias

| ID | Título | Depende de | Valor entregue |
| :--- | :--- | :--- | :--- |
| **STORY-0046-0001** | Rule 21 `lifecycle-integrity` + matriz de transição nos templates + helpers Java base | — | Formalização da convenção; `StatusFieldParser` + `LifecycleTransitionMatrix` prontos para consumo. |
| **STORY-0046-0002** | Planning status propagation (7 skills) | 0046-0001 | Plan skills passam a atualizar `**Status:** Pendente → Planejada` no commit do plan artefact + coluna Planejamento do map. |
| **STORY-0046-0003** | Task-level end-of-life status em `x-task-implement` | 0046-0001 | Task transita para `Concluída` no commit atômico final + row do implementation-map atualizada. |
| **STORY-0046-0004** | Story/Epic end-of-life status — Phase 3 unskippable + Phase 1.7 cabeada | 0046-0001 | `x-story-implement` Phase 3 always-on; `x-epic-implement` Core Loop passa por 1.7 em toda wave. |
| **STORY-0046-0005** | Atomic commit dos reports de épico | 0046-0001 | `x-epic-implement` commita `execution-plan-epic-*.md` e `phase-report-epic-*.md` atomicamente via `x-git-commit`. |
| **STORY-0046-0006** | Nova skill `x-status-reconcile` (opt-in) | 0046-0001 | Modo diagnose (default) + `--apply`; permite backfill manual de EPIC-0024 e outros legados sem violar Rule 19. |
| **STORY-0046-0007** | Enforcement CI audit `LifecycleIntegrityAuditTest` | 0046-0002, 0046-0003, 0046-0004, 0046-0005 | Impede regressão: detecta phases órfãs, escritas em `reports/` sem commit, flags `--skip-*` no happy path. |

### 9.1 Grafo de dependências

```
0046-0001 ────┬───► 0046-0002 ────┐
              ├───► 0046-0003 ────┤
              ├───► 0046-0004 ────┼───► 0046-0007
              ├───► 0046-0005 ────┘
              └───► 0046-0006 (paralelo, não bloqueia 0007)
```

Wave 1: 0046-0001 (base — Rule + helpers + templates)
Wave 2: 0046-0002, 0046-0003, 0046-0004, 0046-0005, 0046-0006 (paralelo)
Wave 3: 0046-0007 (audit final)

## 10. Jornadas

### J1 — Planning path (happy)
`x-story-plan story-XXXX-YYYY` → grava `plans/epic-XXXX/plans/plan-story-*.md` + `tasks-story-*.md` → atualiza `**Status:** Pendente → Planejada` no `story-XXXX-YYYY.md` + coluna Planejamento do map → stage tudo → `Skill(skill: "x-git-commit", args: "docs(story-XXXX-YYYY): plan ready")`.

### J2 — Task implementation path (happy)
`x-task-implement TASK-XXXX-YYYY-NNN` → ciclos TPP (RED→GREEN→REFACTOR) → último commit atômico da task → ANTES de retornar: atualiza `**Status:** → Concluída` no `task-TASK-*.md` + coluna Status do map → stage → `Skill(skill: "x-git-commit", ...)` atômico (pode fundir-se ao commit do último ciclo TDD se ainda não escrito).

### J3 — Epic implementation path (happy)
`x-epic-implement 0046` Core Loop → para cada wave: dispatch waves → Phase 1.6 checkpoint → **NOVA** Phase 1.7 status sync (promovida de 1.6b) que atualiza story status no markdown + map → fim da wave. Ao final do épico: grava `execution-plan-epic-0046.md` + `phase-report-epic-0046.md` em `plans/epic-0046/reports/` → ANTES de retornar: `Skill(skill: "x-git-commit", ...)` de cada report → `git status --porcelain` deve retornar vazio.

### J4 — Reconciliação (opt-in)
Operador notou divergência em EPIC-0024 → `Skill(skill: "x-status-reconcile", args: "--epic 0024")` → modo diagnose reporta 16 divergências → operador roda `--epic 0024 --apply` → gate interativo Rule 20 (PROCEED/FIX/ABORT) → PROCEED → markdowns atualizados + commit `chore(epic-0024): reconcile lifecycle status backfill` → `execution-state.json` **não é alterado** (Rule 19).

### J5 — Failure path
Operador renomeou um story file durante a execução → `x-story-implement` Phase 3 tenta atualizar → `StatusFieldParser` retorna `FileNotFound` → skill aborta com exit 20 (`STATUS_SYNC_FAILED`) + log estruturado em stderr apontando o caminho esperado → operador restaura o arquivo e roda `x-status-reconcile` para recovery.

## 11. Interfaces

- **CLI nova:** `/x-status-reconcile [--epic XXXX | --story story-XXXX-YYYY] [--apply] [--non-interactive] [--dry-run]`
- **Error codes novos:** `STATUS_SYNC_FAILED` (20), `REPORT_COMMIT_FAILED` (novo em `x-epic-implement`), `STATUS_TRANSITION_INVALID` (40 em `x-status-reconcile`), `LIFECYCLE_AUDIT_REGRESSION` (emitido pelo `LifecycleIntegrityAuditTest` em CI).
- **Audit CI:** `mvn test -Dtest=LifecycleIntegrityAuditTest` — bloqueia merge se violations > 0.

## 12. Riscos e Anti-Patterns

1. **NÃO amendar `execution-state.json` de épicos completos** (viola Rule 19 — no implicit schema upgrades). RULE-046-07 explicita: markdown é SoT; state.json é telemetria.
2. **NÃO regravar markdowns de épicos legados automaticamente.** Backfill é opt-in via `x-status-reconcile --apply` com gate interativo.
3. **Preservar v1/v2 compat (Rule 19):** status-sync é V2-gated via `SchemaVersionResolver`. Épicos v1 não ganham nem perdem comportamento.
4. **NÃO introduzir novo formato de checkpoint.** Reusar `execution-state.json`; no máximo campo auxiliar `lastStatusSyncedAt` não load-bearing.
5. **Risco: regex frágil no markdown.** Mitigação: `StatusFieldParser` tolera whitespace variável, ancora em `^\*\*Status:\*\* ` começo de linha; testes de regex exaustivos em `StatusFieldParserTest`.
6. **Risco: commit duplo entre plan write e status update.** Política RULE-046-02: status update é stageado JUNTO com o plan artefact antes do único `x-git-commit` da fase — não há commit dedicado ao status update.
7. **Anti-pattern proibido:** adicionar flag `--skip-status-sync` opt-in no caminho happy. Proibido por RULE-046-04. CI audit (STORY-0046-0007) rejeita.
8. **Risco: auto-planejamento paradoxal.** `x-epic-decompose`, `x-story-plan`, `x-task-plan`, `x-epic-map` — skills que serão modificadas por STORY-0046-0002 — são usadas NESTE planning (execução atual). EPIC-0046 se auto-planeja com versões pre-fix; auto-corrige na execução; audit da STORY-0046-0007 começa vermelho e fica verde apenas no final. Sinal objetivo de conclusão.

## 13. Referências

- [`x-story-plan` SKILL.md](../../java/src/main/resources/targets/claude/skills/core/plan/x-story-plan/SKILL.md)
- [`x-task-plan` SKILL.md](../../java/src/main/resources/targets/claude/skills/core/plan/x-task-plan/SKILL.md)
- [`x-epic-create` SKILL.md](../../java/src/main/resources/targets/claude/skills/core/plan/x-epic-create/SKILL.md)
- [`x-epic-decompose` SKILL.md](../../java/src/main/resources/targets/claude/skills/core/plan/x-epic-decompose/SKILL.md)
- [`x-epic-map` SKILL.md](../../java/src/main/resources/targets/claude/skills/core/plan/x-epic-map/SKILL.md)
- [`x-arch-plan` SKILL.md](../../java/src/main/resources/targets/claude/skills/core/plan/x-arch-plan/SKILL.md)
- [`x-test-plan` SKILL.md](../../java/src/main/resources/targets/claude/skills/core/test/x-test-plan/SKILL.md)
- [`x-task-implement` SKILL.md](../../java/src/main/resources/targets/claude/skills/core/dev/x-task-implement/SKILL.md)
- [`x-story-implement` SKILL.md](../../java/src/main/resources/targets/claude/skills/core/dev/x-story-implement/SKILL.md) — Phase 3 (linhas 975-987) e gate `--skip-verification` a ser removido do happy path.
- [`x-epic-implement` SKILL.md](../../java/src/main/resources/targets/claude/skills/core/dev/x-epic-implement/SKILL.md) — Section 1.6b (linhas 1143-1200) a ser cabeada como Phase 1.7; escritas em `reports/` linhas 220, 233, 264, 706 a serem seguidas de commit atômico.
- [`x-release` SKILL.md](../../java/src/main/resources/targets/claude/skills/core/ops/x-release/SKILL.md) — precondição VALIDATE_DIRTY_WORKDIR (linhas 277-308) que motiva RULE-046-05/06.
- [`x-git-commit` SKILL.md](../../java/src/main/resources/targets/claude/skills/core/git/x-git-commit/SKILL.md) — ponto de delegação para commits atômicos.
- [Rule 05 — Quality Gates](../../.claude/rules/05-quality-gates.md)
- [Rule 08 — Release Process](../../.claude/rules/08-release-process.md)
- [Rule 09 — Branching Model](../../.claude/rules/09-branching-model.md)
- [Rule 13 — Skill Invocation Protocol](../../.claude/rules/13-skill-invocation-protocol.md)
- [Rule 19 — Backward Compatibility](../../.claude/rules/19-backward-compatibility.md) — status sync V2-gated via `SchemaVersionResolver`.
- [EPIC-0024 — Artifact Persistence](../epic-0024-artifact-persistence/) — evidência empírica do GAP-1.
- [EPIC-0038 — Task-First Architecture](../epic-0038/) — schema v2.0 reference + `SchemaVersionResolver`.
- [EPIC-0043 — Interactive Gates](../epic-0043/) — gate interativo reusado por `x-status-reconcile --apply`.
- [EPIC-0045 — CI Watch](../epic-0045/) — ocupa Rule 20; este épico usa Rule 21.
