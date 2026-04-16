# Especificação Técnica: CI Watch no Fluxo de PR

**Autor:** Eder Celeste Nunes Junior
**Data:** 2026-04-16
**Versão:** 0.1 (draft)
**Status:** Em Refinamento
**Épico:** EPIC-0045

---

## 1. Contexto

O gerador `ia-dev-env` adota um fluxo de orquestração de PR onde a skill `x-story-implement` (source of truth: `java/src/main/resources/targets/claude/skills/core/dev/x-story-implement/SKILL.md`) executa o seguinte ciclo por tarefa na Phase 2.2:

```
2.2.5  x-test-tdd   (implementação TDD)
2.2.6  git push     (branch remota)
2.2.7  x-pr-create  (cria PR, retorna prNumber + prUrl)
2.2.8  update state  (task.status = PR_CREATED)
2.2.9  APPROVAL GATE (EPIC-0042: auto-merge ou AskUserQuestion PROCEED/FIX-PR/ABORT do EPIC-0043)
```

Entre `PR_CREATED` (2.2.8) e `APPROVAL GATE` (2.2.9) não há espera por:

1. Conclusão dos checks de CI remoto (`gh pr checks`, `statusCheckRollup`).
2. Postagem do review automático do Copilot (`copilot-pull-request-reviewer[bot]`).

O mesmo gap existe em `x-task-implement --worktree` quando invocado standalone e, em menor grau, no `x-release` Phase APPROVAL-GATE (que aguarda merge humano, mas não aguarda CI antes de apresentar o gate).

## 2. Problema

### 2.1 Sintoma principal

Quando o menu interativo do EPIC-0043 (`PROCEED / FIX-PR / ABORT`) oferece `FIX-PR`, a skill `x-pr-fix` é invocada via Rule 13 Pattern 1 INLINE-SKILL. Essa skill chama `gh api repos/{owner}/{repo}/pulls/{N}/comments` e `gh api .../pulls/{N}/reviews` e **encontra zero comentários**, porque o Copilot ainda não postou (o review automático leva tipicamente 30–180s após a criação do PR, dependendo da fila do GitHub).

Consequência observada: operador aciona `FIX-PR` por reflexo, skill reporta "nenhum comentário actionable", menu reaparece, operador escolhe `PROCEED` sem feedback real, PR é auto-mergeado sem passar pelo crivo automatizado. O objetivo primário do menu interativo — **aproveitar feedback de CI + Copilot antes do merge** — fica inviabilizado.

### 2.2 Sintomas secundários

| # | Sintoma | Impacto |
| :--- | :--- | :--- |
| 1 | Auto-merge (EPIC-0042) ocorre antes do CI terminar em repositórios com pipelines lentas (>60s). | PRs são mergeados em `develop` com CI ainda verde-amarelo; rollback só aparece pós-merge. |
| 2 | Nenhum exit code distinto para "CI falhou" vs. "CI passou sem review do Copilot". | Orquestrador não pode formar prompt do menu com precisão; operador decide às cegas. |
| 3 | Ausência de state-file para resume. | Se a sessão Claude cair durante espera, o orquestrador recomeça o CI-Watch do zero em vez de retomar de onde parou. |
| 4 | Nenhum limite de tempo configurável. | Em repos com Copilot desativado, orquestrador travaria indefinidamente — gate precisa ter timeout gracioso. |

### 2.3 Causa raiz

Falta de uma primitiva reusável "aguardar PR atingir estado observável" na taxonomia de skills (`core/pr/`). Cada orquestrador que precisasse desse comportamento teria que re-implementar polling + timeout + detecção de Copilot inline, violando DRY (Rule 03) e SRP.

## 3. Objetivo

Introduzir uma skill dedicada `x-pr-watch-ci` em `core/pr/` que encapsule o polling de checks de CI + detecção de review automático, exponha exit codes estáveis como contrato público, e seja invocável pelos orquestradores via Rule 13 Pattern 1 INLINE-SKILL entre `x-pr-create` e a approval gate.

### Objetivos específicos

- **O1** — Bloquear a approval gate até os checks de CI reportarem conclusão (sucesso ou falha).
- **O2** — Aguardar o review do `copilot-pull-request-reviewer[bot]` com timeout gracioso configurável; não travar quando Copilot está desativado.
- **O3** — Expor exit codes distintos (`SUCCESS`, `CI_PENDING_PROCEED`, `CI_FAILED`, `TIMEOUT`, etc.) para o orquestrador formar o prompt do menu com informação precisa.
- **O4** — Reutilizar o padrão de state-file + resume introduzido pelo EPIC-0035 story 0035-0004 (x-release APPROVAL-GATE) para suportar retomada após interrupção da sessão.
- **O5** — Manter backward compatibility com epics schema v1.0 (Rule 19) — CI-Watch é ligado por default apenas em schema v2.0.

## 4. Escopo

### 4.1 Dentro de escopo

- Nova skill `x-pr-watch-ci` em `java/src/main/resources/targets/claude/skills/core/pr/x-pr-watch-ci/`.
- Nova Rule 20 em `java/src/main/resources/targets/claude/rules/20-ci-watch.md` formalizando o comportamento default e o opt-out `--no-ci-watch`.
- Integração em `x-story-implement/SKILL.md` Phase 2.2 (novo passo 2.2.8.5 entre PR_CREATED e APPROVAL GATE).
- Integração em `x-task-implement/SKILL.md` quando invocado standalone com `--worktree`.
- Integração opcional em `x-release/SKILL.md` via flag `--ci-watch` antes da APPROVAL-GATE.
- Helper Java opcional `PrWatchStatusClassifier` para lógica de classificação de exit code (se valor agregado justificar extração).
- Golden diff regen para todas as SKILL.md afetadas.
- Smoke test real contra PR do próprio repo.

### 4.2 Fora de escopo

- Auto-classificação de comentários (já é responsabilidade de `x-pr-fix`).
- Auto-merge de PRs (já é responsabilidade do EPIC-0042 / `x-story-implement` Phase 2.2.9).
- Retrofits em skills que não abrem PR (`x-code-format`, `x-code-lint`, `x-test-run`, etc.).
- Dashboards de métricas de CI-Watch (adiado para épico futuro).
- Integração com CI providers específicos além de `gh` CLI (GitHub Actions, CircleCI webhook-bridge para GH Checks API são cobertos implicitamente; integração nativa com GitLab CI ou Jenkins fica fora).

## 5. Contrato da skill `x-pr-watch-ci`

### 5.1 Argumentos

| Argumento | Tipo | Default | Semântica |
| :--- | :--- | :--- | :--- |
| `--pr-number <N>` | int | — (obrigatório) | Número do PR a monitorar. |
| `--timeout-seconds <N>` | int | 1800 | Timeout global. Bounds: 60–7200. |
| `--poll-interval-seconds <N>` | int | 60 | Intervalo entre polls. Bounds: 15–300. |
| `--require-copilot-review` | boolean | true | Se true, aguarda review do Copilot até `--copilot-review-timeout`. |
| `--require-checks-passing` | boolean | true | Se true, exige todos os checks com `conclusion=success`. Se false, aceita neutral/skipped. |
| `--copilot-review-timeout <N>` | int | 900 | Timeout específico para Copilot. Após estourar, retorna `CI_PENDING_PROCEED`. |
| `--state-file <path>` | path | `.claude/state/pr-watch-<N>.json` | State file para resume. |
| `--no-state-file` | flag | — | Desabilita persistência de state (fire-and-forget). |

### 5.2 Exit codes (contrato público estável)

| Código | Nome | Semântica |
| :--- | :--- | :--- |
| 0 | `SUCCESS` | Checks green + Copilot review presente (ou `--require-copilot-review=false`). |
| 10 | `CI_PENDING_PROCEED` | Checks green + timeout de Copilot estourou sem review. Orquestrador pode seguir, mas menu deve sinalizar. |
| 20 | `CI_FAILED` | Algum check com `conclusion` ∈ {`failure`, `timed_out`, `cancelled`, `action_required`}. |
| 30 | `TIMEOUT` | Timeout global estourado com checks ainda pendentes. |
| 40 | `PR_ALREADY_MERGED` | Idempotência. Sai imediatamente com exit code 40 e warning. |
| 50 | `NO_CI_CONFIGURED` | `gh pr checks` retornou vazio. Sai com exit code 50 e warning; Copilot ainda respeitado. |
| 60 | `PR_CLOSED` | PR fechado sem merge. Aborta. |
| 70 | `PR_NOT_FOUND` | PR inexistente ou sem permissão. Aborta. |

### 5.3 Contrato de IO

- **stdin:** vazio.
- **stdout:** progress logs intermediários + objeto JSON final na última linha:
  ```json
  {
    "status": "SUCCESS",
    "prNumber": 42,
    "checks": [
      {"name": "build", "conclusion": "success"},
      {"name": "test", "conclusion": "success"}
    ],
    "copilotReview": {"present": true, "reviewId": 12345678},
    "elapsedSeconds": 87
  }
  ```
- **stderr:** progress logs estruturados (formato livre).

### 5.4 Edge cases

| Cenário | Comportamento |
| :--- | :--- |
| Rate limit `gh` | Retry com backoff exponencial 3× (30s, 60s, 120s); após isso, exit 30 (`TIMEOUT`). |
| Copilot desativado no repo | Detectar via ausência do reviewer em `gh api .../pulls/{N}/requested_reviewers` após 1º poll; downgrade imediato para `CI_PENDING_PROCEED` (exit 10). |
| State file corrompido | Log warning, recomeçar do zero. |
| PR fechado durante polling | Exit 60 imediatamente. |
| PR mergeado durante polling | Exit 40 imediatamente. |

## 6. Regras de Negócio Transversais

| ID | Título | Descrição |
| :--- | :--- | :--- |
| **RULE-045-01** | CI-Watch default em schema v2 | Em epics com `planningSchemaVersion == "2.0"`, a invocação de `x-pr-watch-ci` entre `x-pr-create` e APPROVAL GATE é **obrigatória**. Opt-out apenas via flag `--no-ci-watch` (uso em CI/automação). |
| **RULE-045-02** | No-op em schema v1 (Rule 19) | Em epics com `planningSchemaVersion == "1.0"` ou ausente, `x-pr-watch-ci` NÃO é invocada. Rule 19 (Backward Compatibility) é respeitada integralmente. |
| **RULE-045-03** | State-file canônico | Localização: `.claude/state/pr-watch-<N>.json`. Shape: `{ prNumber, startedAt, lastPollAt, pollCount, checksSnapshot[], copilotReview?, schemaVersion: "1.0" }`. Escrita atômica via `.tmp` + rename. |
| **RULE-045-04** | Copilot login canônico | Reviewer Copilot é identificado pelo login exato `copilot-pull-request-reviewer[bot]`. Alias ou nome humano diferente não é reconhecido. |
| **RULE-045-05** | Exit codes estáveis como contrato público | Códigos 0/10/20/30/40/50/60/70 da seção 5.2 são contrato público. Mudança de semântica exige MAJOR bump (Rule 08). Adição de novo código é MINOR. |
| **RULE-045-06** | Rule 13 INLINE-SKILL | Todos os orquestradores invocam `x-pr-watch-ci` via `Skill(skill: "x-pr-watch-ci", args: "...")`. Bare-slash proibido em delegação (Rule 13 §Forbidden). |
| **RULE-045-07** | Menu do EPIC-0043 consome exit code | O orquestrador passa o exit code + JSON final do `x-pr-watch-ci` como contexto para o prompt do menu `PROCEED/FIX-PR/ABORT`. Menu exibe status dos checks + presença do Copilot review no description do `AskUserQuestion`. |

## 7. Definition of Ready (Global)

- Source-of-truth em `java/src/main/resources/targets/claude/` respeitado; nenhuma edição em `.claude/skills/**`, `.claude/rules/**`, `.claude/templates/**` ou `src/test/resources/golden/**` (RULE-001).
- Taxonomia ADR-0003 observada: nova skill em `core/pr/` (categoria correta).
- Rule 13 invocation patterns respeitados: delegação de orquestradores para `x-pr-watch-ci` usa Pattern 1 INLINE-SKILL; bare-slash proibido fora de `## Triggers` e `## Examples`.
- `execution-state.json` do épico declara `planningSchemaVersion: "2.0"`.
- Próximo slot de rule identificado: `20-ci-watch.md` (slots 13–19 ocupados; 10/11/12 reservados).
- Precondição dura: **EPIC-0043 mergeado em `develop`** antes de STORY-0045-0003 entrar em execução (evita merge conflict em `x-story-implement/SKILL.md:797-810`). Demais stories (0001, 0002, 0004, 0005) podem ser executadas em paralelo com EPIC-0043.

## 8. Definition of Done (Global)

- **Cobertura:** ≥ 95% Line, ≥ 90% Branch em qualquer helper Java novo (ex.: `PrWatchStatusClassifier`). A lógica em si da skill é prompt-driven — coberta via golden diff do SKILL.md + smoke test.
- **Testes Automatizados:** unit tests Java para classificador de exit codes (tabela completa das 8 rows da seção 5.2); golden diff obrigatório para `x-pr-watch-ci/SKILL.md` + retrofits em `x-story-implement`, `x-task-implement`, `x-release`; golden diff para nova rule `20-ci-watch.md`; smoke test (`Epic0045SmokeTest.java`) contra PR real com exit code 0 + JSON final válido em stdout.
- **Smoke Tests:** `mvn process-resources && mvn test` verde; `SkillsAssemblerTest` não regride; novo `RuleAssemblerTest.listRules_includesCiWatch` obrigatório.
- **Relatório de Cobertura:** JaCoCo agregado no Maven report padrão.
- **Documentação:** Rule 20 criada; CLAUDE.md atualizado no bloco "In progress" com referência ao épico enquanto não concluído; CHANGELOG seção Unreleased com uma entrada por story. `.claude/skills/**`, `.claude/rules/**` e `.claude/README.md` apenas regenerados via `mvn process-resources` — nunca editados à mão (RULE-001).
- **Persistência:** State-file schema versionado `"1.0"` conforme RULE-045-03; escrita atômica `.tmp` + rename; resume testado em smoke (simular restart da skill).
- **Performance:** Overhead do CI-Watch = tempo real de espera pelo CI. Happy path (CI já verde + Copilot review presente no 1º poll) deve completar em ≤ 1× `--poll-interval-seconds` (default 60s).
- **TDD Compliance:** commits mostram test-first — golden diff precede ou acompanha cada diff de SKILL.md; unit test de `PrWatchStatusClassifier` precede implementação. Tasks pequenas (Doc/Test size S–M) via TPP.
- **Double-Loop TDD:** acceptance tests derivados dos cenários Gherkin da STORY-0045-0006 (outer loop); unit tests do helper Java guiados por TPP (inner loop).
- **Backward Compatibility:** Rule 19 respeitada — epics schema v1.0 não invocam `x-pr-watch-ci` (RULE-045-02). Novo flag `--no-ci-watch` em orquestradores é opt-out explícito para CI/automação.
- **Rule 13 Audit:** grep padrão Rule 13 continua retornando 0 matches em `java/src/main/resources/targets/claude/skills/core/` após retrofits.
- **Rule 20 Audit:** novo audit (parte da STORY-0045-0006) verifica que todos os orquestradores que invocam `x-pr-create` também invocam `x-pr-watch-ci` (ou declaram `--no-ci-watch` explicitamente).

## 9. Índice de Histórias

| ID | Título | Depende de | Valor entregue |
| :--- | :--- | :--- | :--- |
| **STORY-0045-0001** | Criar skill `x-pr-watch-ci` em `core/pr/` | — | Primitiva reusável de espera por CI + Copilot com exit codes estáveis. |
| **STORY-0045-0002** | Adicionar Rule 20 (CI-Watch) | — | Formalização do comportamento default + opt-out; Rule 19 compat garantida. |
| **STORY-0045-0003** | Integrar `x-pr-watch-ci` em `x-story-implement` Phase 2.2 | 0045-0001, EPIC-0043 mergeado | Menu interativo passa a receber contexto real de CI + Copilot. |
| **STORY-0045-0004** | Integrar `x-pr-watch-ci` em `x-task-implement --worktree` | 0045-0001 | Standalone task execution também aguarda CI antes de gate. |
| **STORY-0045-0005** | Integrar CI-Watch opcional em `x-release` | 0045-0001 | Release PR aguarda CI antes da approval-gate humana (opt-in via `--ci-watch`). |
| **STORY-0045-0006** | Golden diff regen + smoke test real | 0045-0003, 0045-0004, 0045-0005 | Validação end-to-end + regeneração de goldens fecha o épico. |

### 9.1 Grafo de dependências

```
0045-0001 ────┬───► 0045-0003 ────┐
              ├───► 0045-0004 ────┼───► 0045-0006
              └───► 0045-0005 ────┘
0045-0002 (independente, paralelo)
```

Wave 1 (paralelo): 0001, 0002, 0004, 0005
Wave 2: 0003 (bloqueado em EPIC-0043 merge + 0045-0001)
Wave 3: 0006

## 10. Referências

- [`x-story-implement` SKILL.md](../../java/src/main/resources/targets/claude/skills/core/dev/x-story-implement/SKILL.md) — ponto de inserção linhas 797-810.
- [`x-task-implement` SKILL.md](../../java/src/main/resources/targets/claude/skills/core/dev/x-task-implement/SKILL.md) — fluxo standalone `--worktree`.
- [`x-release` SKILL.md](../../java/src/main/resources/targets/claude/skills/core/ops/x-release/SKILL.md) — Phase APPROVAL-GATE com state-file pattern.
- [`x-pr-create` SKILL.md](../../java/src/main/resources/targets/claude/skills/core/pr/x-pr-create/SKILL.md) — produz prNumber consumido pelo CI-Watch.
- [`x-pr-fix` SKILL.md](../../java/src/main/resources/targets/claude/skills/core/pr/x-pr-fix/SKILL.md) — invocada pelo menu `FIX-PR` do EPIC-0043 após CI-Watch.
- [`x-pr-fix-epic` SKILL.md](../../java/src/main/resources/targets/claude/skills/core/pr/x-pr-fix-epic/SKILL.md) — login canônico do Copilot (SKILL.md:493).
- [Rule 13 — Skill Invocation Protocol](../../.claude/rules/13-skill-invocation-protocol.md)
- [Rule 14 — Worktree Lifecycle](../../.claude/rules/14-worktree-lifecycle.md)
- [Rule 19 — Backward Compatibility](../../.claude/rules/19-backward-compatibility.md)
- [EPIC-0035 — Release Orchestrator](../epic-0035/) — prior art do state-file + resume pattern.
- [EPIC-0038 — Task-First Architecture](../epic-0038/) — schema v2.0 reference.
- [EPIC-0042 — Merge-Train / Approval Gate](../epic-0042/) — ponto de integração.
- [EPIC-0043 — Interactive Gates](../epic-0043/) — menu `PROCEED/FIX-PR/ABORT` consumidor do exit code.
