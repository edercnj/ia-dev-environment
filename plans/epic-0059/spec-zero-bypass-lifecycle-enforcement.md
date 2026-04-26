# Spec — Zero-Bypass Lifecycle Enforcement

> **Proposta de Épico:** EPIC-0059
> **Target branch:** `epic/0059`
> **Worktree:** `.claude/worktrees/epic-0059/`
> **Destino:** este arquivo é o **input autoritativo** para `/x-epic-create` (ou `/x-epic-decompose`), que produzirá `epic-0059.md`, `story-0059-*.md` e `IMPLEMENTATION-MAP.md`.
> **Autor:** Eder Celeste Nunes Junior
> **Data:** 2026-04-26
> **Versão da spec:** 1.0

---

## 1. Contexto e Problema

### 1.1 Sintoma observado

Análise de 4 épicos consecutivos (EPIC-0054, 0055, 0056, 0057) revelou que **nenhum** materializou os 6 artefatos de planejamento por story exigidos por `x-story-implement` Phase 1B–1F. O comando `find plans/epic-005[4-7]/plans/` retorna vazio para esses épicos. A telemetria de `plans/epic-0057/telemetry/events.ndjson` (171 eventos) **não contém um único evento** de `x-story-implement`, `x-internal-story-build-plan`, `x-arch-plan` ou `x-story-plan`. Os commits do `epic/0057` não seguem a assinatura atômica do TDD loop (`feat(task-XXXX-YYYY-NNN): [RED]/[GREEN]/[REFACTOR]`); usam mensagens livres (`feat(camada-2): extend Stop hook…`). Os PRs vieram de branches livres (`feat/story-0056-0008-golden-files-changelog`) em vez do padrão RULE-001 (`feat/task-XXXX-YYYY-NNN-desc`).

**Conclusão**: o orquestrador `/x-story-implement` foi sistematicamente bypassado. Stories foram implementadas direto pelo operador / LLM, abrindo branch, codando, commitando e abrindo PR fora do fluxo. Toda a wave de planejamento, todos os gates, toda a telemetria semântica, todo o contrato de evidência da Rule 24 — silenciosamente puladas.

### 1.2 Diagnóstico técnico

Inventário das defesas existentes vs. cobertura efetiva contra o bypass observado:

| # | Mecanismo | Camada | O que cobre | Falha contra EPIC-0057 porque… |
| :--- | :--- | :--- | :--- | :--- |
| 1 | `audit-execution-integrity.sh` | CI (Camada 3) | 4 artefatos de Fase 3 (`verify-envelope`, `review`, `techlead-review`, `story-completion-report`) | Não exige os 6 artefatos de Fase 1; passa verde mesmo sem nenhum plan |
| 2 | `audit-bypass-flags.sh` | CI | `--skip-*` em SKILL.md fora de `## Recovery` | Detecta documentação inválida, não detecta uso real do flag em runtime |
| 3 | `audit-phase-gates.sh` | CI | Phase gates declarados em SKILL.md | Não detecta gates que nunca foram **executados** |
| 4 | `audit-task-hierarchy.sh` | CI | `TaskCreate` declarado em SKILL.md | Não detecta tasks que nunca foram **emitidas** em runtime |
| 5 | `verify-story-completion.sh` | Stop hook | Detecta atividade de PR e checa evidência | Falha-aberto: branches manuais não casam padrão `feat/task-*`, hook fica silencioso |
| 6 | `verify-phase-gates.sh` | Stop hook | Lê `taskTracking.phaseGateResults` | Sem orquestrador rodando, `phaseGateResults` permanece vazio — nada a verificar |
| 7 | `enforce-phase-sequence.sh` | PreToolUse | Bloqueia `Skill()` se predecessor não passou | Só dispara se houver `Skill()` chamado — bypass não chama nada |
| 8 | `.githooks/pre-push` | Git hook local | Smoke tests antes de push | `--no-verify` ou `CLAUDE_SMOKE_DISABLED=1` bypassa em 1 segundo |
| 9 | `LifecycleIntegrityAuditTest` | Maven CI | Regressões em SKILL.md | Não cobre PRs que não tocam SKILL.md |
| 10 | RA9 audit checkers (EPIC-0056) | CI | 9 seções fixas em plans existentes | Não tem nada a checar quando os plans nem existem |

**Conclusão**: as 10 camadas existentes assumem implicitamente que **o orquestrador está rodando**. Quando o operador pula o orquestrador inteiramente, nenhuma camada dispara. O sistema é "policiado por dentro" — não há gate externo que detecte "este PR alega implementar story-X mas não há rastro de execução do orquestrador".

### 1.3 Origem do problema

Cada camada de enforcement foi adicionada **reativamente** após um incidente específico (Rule 24 após bypass de review, Rule 25 após desorganização de tasks, RA9 após inconsistência de templates). Nenhuma teve como premissa explícita "o operador pode escolher não invocar o orquestrador". Todas as camadas amarram o comportamento **de dentro do fluxo**, não **na chegada do PR**. O resultado é defesa-em-profundidade horizontal mas sem chokepoint vertical.

### 1.4 Surfaces de bypass catalogadas

Mapeamento exaustivo das 12 superfícies por onde o operador (ou LLM em sessão do operador) pode contornar o lifecycle:

| ID | Surface | Exemplo concreto | Defesa atual |
| :--- | :--- | :--- | :--- |
| **A** | Operador pula `/x-story-implement` | Implementa story direto, abre PR | Nenhuma |
| **B** | Phase 1 wave artifacts ausentes | `plans/epic-XXXX/plans/` vazio em PR de story | Nenhuma |
| **C** | `gh pr create` manual fora de `x-pr-create` | `gh pr create --title "feat: ..."` sem rodar skill | Nenhuma |
| **D** | `git commit` manual fora de `x-git-commit` | `git commit -m "fix"` sem ciclo TDD | Nenhuma |
| **E** | `git commit --no-verify` / `git push --no-verify` | Bypassa pre-commit chain local | Nenhuma (CI não re-roda) |
| **F** | Edição direta de `execution-state.json` | `vim plans/epic-XXXX/execution-state.json` | Nenhuma |
| **G** | `--skip-verification` / `--skip-review` / `--skip-smoke` / `--skip-pr-comments` / `--no-ci-watch` / `--no-auto-remediation` em runtime | Argumento real passado ao orquestrador | Audit cobre só docs |
| **H** | Telemetria sem eventos do orquestrador | `events.ndjson` sem `phase.start x-story-implement` | Nenhuma |
| **I** | Backfill retroativo de evidência | Commit `feat(retroactive): backfill X to satisfy audit` | Audit aceita arquivo, não checa origem |
| **J** | `taskTracking.enabled` ausente vira no-op | Rule 19 fallback engole silenciosamente | Tolerado por design |
| **K** | Branch protection fraca no GitHub | Merge sem required status checks | Out of repo |
| **L** | RA9 não enforced por story merged | Audit existe mas baseline tolera 0057 | Frouxo |

---

## 2. Objetivo

Estabelecer **enforcement total** ("zero-bypass") do lifecycle de stories e tasks: nenhum PR pode mergear em `develop` ou `main` sem cadeia completa e auditável de evidência produzida pelos orquestradores canônicos. Toda surface da §1.4 deve ter pelo menos uma defesa que **bloqueie**, não apenas alerte.

Resultados esperados:

1. **PR-gate único e inegociável** — `audit-execution-integrity.sh` estendido bloqueia merge de qualquer PR cujo conjunto de stories tocadas não tenha 10 artefatos (6 Fase 1 + 4 Fase 3) **+** eventos de telemetria do orquestrador.
2. **Defesa-em-profundidade upstream** — pre-commit/push hooks, PreToolUse hooks e Stop hooks alertam o operador *durante* a sessão (visibilidade), não só no CI (bloqueio).
3. **Markers de origem em artefatos** — todo plan/report carrega frontmatter `generated-by: <skill>@<commitSha>`; backfill manual é detectável e rejeitado.
4. **Sem hotfix path** — enforcement é universal. Hotfixes seguem o mesmo lifecycle (incluindo branch `hotfix/*` invocando `/x-story-implement` com `--target-branch main`).
5. **Anistia retroativa explícita** — stories mergeadas em EPIC-0054, 0055, 0056, 0057 são adicionadas à baseline imutável; novos PRs entram no regime estrito imediatamente.
6. **Rule 26 codifica o contrato** — "Zero-Bypass Lifecycle" é uma regra normativa nova, referenciada em CLAUDE.md.

### 2.1 Não-objetivos

- Refatorar os orquestradores existentes (são source-of-truth — Rule 24 já protege).
- Reescrever skills internas — a wave atual já produz os 6 artefatos quando invocada.
- Substituir `--skip-*` flags por novos mecanismos — eles continuam existindo para `## Recovery`, só não podem ser usados em happy-path.
- Adicionar caminho rápido para hotfix (decisão explícita do PO em 2026-04-26: enforcement total).

### 2.2 Métricas de sucesso

| Métrica | Linha de base (EPIC-0057) | Meta pós-EPIC-0059 |
| :--- | :--- | :--- |
| % de stories merged com 6 artefatos Fase 1 | 0% (0/8) | 100% (gate bloqueia) |
| % de stories merged com 4 artefatos Fase 3 | ~50% (backfill manual) | 100% (gate bloqueia) |
| % de stories merged com telemetria do orquestrador | 0% | 100% |
| Commits de backfill retroativo permitidos | livres | 0 (audit rejeita) |
| Tempo de detecção do bypass | nunca (post-mortem) | < 2 min (PR check fail) |

---

## 3. Escopo

### 3.1 Stories propostas (12)

Ordem por retorno-sobre-investimento — cada fase entrega valor sozinha.

#### Fase 0 — Tampar o vazamento principal (alta prioridade, baixa complexidade)

**story-0059-0001 — Estender `audit-execution-integrity.sh` para os 6 artefatos de Fase 1**
- Acrescentar à lista mandatória os 6 paths em `plans/epic-XXXX/plans/`: `arch-story-*.md`, `plan-story-*.md`, `tests-story-*.md`, `tasks-story-*.md`, `security-story-*.md`, `compliance-story-*.md`.
- SIMPLE scope (≤ 4 tasks).
- `--scope=fase1` permite rodar só essa parte (debug).
- Atualizar `audits/execution-integrity-baseline.txt` adicionando todas as stories de EPIC-0054 a 0057 (anistia explícita).
- Self-check `--self-check` valida que a tabela está completa.

**story-0059-0002 — Markers de origem em artefatos + audit anti-backfill**
- Toda skill de planejamento (`x-arch-plan`, `x-internal-story-build-plan`, `x-test-plan`, `x-task-plan`) emite frontmatter `generated-by: <skill-name>@<git-commit-sha>` no topo do artefato.
- `audit-execution-integrity.sh` rejeita PRs em que o artefato:
  - Não tem o frontmatter, OU
  - O `<commit-sha>` referenciado não existe na história do PR, OU
  - Foi adicionado em commit posterior ao merge da story (backfill detection).
- Permitir `<!-- audit-exempt: backfill <link-incident> -->` para casos legítimos com aprovação humana.
- STANDARD scope.

#### Fase 1 — Defesa upstream (média prioridade, média complexidade)

**story-0059-0003 — PreToolUse hook bloqueia `--skip-*` fora de Recovery**
- Novo hook `.claude/hooks/enforce-no-bypass-flags.sh` registrado em `PreToolUse` matcher `Skill`.
- Whitelist explícita: `--skip-*` só permitido quando o callsite vem de skill em modo recovery (detectado via `CLAUDE_RECOVERY_MODE=1` env var setada por `x-internal-story-resume` quando `staleWarnings != []`).
- Exit não-zero bloqueia a invocação; mensagem de erro orienta o operador.
- STANDARD scope.

**story-0059-0004 — Pre-commit hook protege `execution-state.json`**
- Novo hook `.githooks/pre-commit` (ou extensão do existente) rejeita commits que tocam `plans/epic-*/execution-state.json` se a mensagem do commit não contém `Co-Authored-By: x-internal-status-update@<sha>`.
- Trailer é injetado automaticamente por `x-internal-status-update` quando faz mutações.
- Exit não-zero bloqueia commit local; CI valida no merge commit também.
- SIMPLE scope.

**story-0059-0005 — Pre-commit hook exige assinatura do orquestrador em branches `feat/task-*`**
- Estensão do hook acima: commits em branches que casam `feat/task-XXXX-YYYY-NNN-*` precisam de trailer `x-git-commit-version: <hash>` (injetado por `x-git-commit`).
- Branches que não casam o padrão (ex: `fix/typo`) são exemptas — para forçar uso de feature branch padrão, `audit-execution-integrity.sh` valida que cada PR tem ao menos uma `STORY-ID` referenciada no body.
- STANDARD scope.

#### Fase 2 — PR-gate intransponível (alta prioridade, alta complexidade)

**story-0059-0006 — CI re-roda pre-commit chain no merge commit**
- Novo job no workflow `ci-release.yml`: para cada commit do PR, valida que `format → lint → compile` passa idempotente.
- Bloqueia bypass via `git commit --no-verify` local.
- Suporta cache para evitar re-build pesado.
- STANDARD scope.

**story-0059-0007 — PR template + CI valida "Orchestrator Evidence" preenchida**
- Novo arquivo `.github/pull_request_template.md` com seção fixa "## Orchestrator Evidence" contendo:
  - Story IDs implementadas
  - Commit SHA do orquestrador (`x-story-implement` invocation)
  - Lista de artefatos Fase 1 + Fase 3 com paths
- `x-pr-create` preenche automaticamente; PRs manuais ficam com seção vazia → rejeitados pelo audit.
- Audit `audit-pr-evidence.sh` (novo) parseia o body do PR via `gh pr view --json body`.
- STANDARD scope.

**story-0059-0008 — Telemetria como prova-de-vida do orquestrador**
- Audit estendido: para cada `STORY-ID` mencionada nos commits do PR, exigir presença em `plans/epic-XXXX/telemetry/events.ndjson` de eventos:
  - `phase.start x-story-implement Phase-0-Prepare`
  - `phase.start x-story-implement Phase-1-Plan` (ou `[phase-1] skipped — PRE_PLANNED`)
  - `phase.start x-story-implement Phase-2-Implement`
  - `phase.start x-story-implement Phase-3-Verify`
- `events.ndjson` é commit-tracked como evidência.
- Detecta o EPIC-0057-class de bypass de forma determinística.
- COMPLEX scope.

#### Fase 3 — Plataforma + normativa (baixa prioridade, baixa complexidade)

**story-0059-0009 — GitHub branch protection + CODEOWNERS**
- Documentar (em `.github/SETUP-PROTECTION.md`) os required status checks que devem ser ativados em `develop` e `main`:
  - `audit-execution-integrity`
  - `audit-bypass-flags`
  - `audit-phase-gates`
  - `audit-task-hierarchy`
  - `audit-model-selection`
  - `audit-pr-evidence` (novo, story-0059-0007)
  - `LifecycleIntegrityAuditTest`
  - `RA9-audit`
- Atualizar `.github/CODEOWNERS` para exigir review humana em paths críticos: `.claude/rules/`, `.claude/hooks/`, `scripts/audit-*`, `audits/*-baseline.txt`.
- Skill helper: `scripts/setup-branch-protection.sh` aplica via `gh api` (idempotente).
- SIMPLE scope.

**story-0059-0010 — Rule 26 + bloco "ZERO-BYPASS" em CLAUDE.md**
- Nova `.claude/rules/26-zero-bypass-lifecycle.md` consolida o contrato e cita as 12 surfaces.
- CLAUDE.md ganha bloco normativo "ZERO-BYPASS LIFECYCLE — INEGOCIÁVEL" no mesmo padrão da "EXECUTION INTEGRITY".
- Documenta `Rule 26 vs Rule 24`: Rule 24 enforce que sub-skills declaradas sejam invocadas; Rule 26 enforce que o orquestrador raiz seja invocado.
- Sem deprecation window — vale para todo PR aberto após o merge do EPIC-0059.
- SIMPLE scope.

**story-0059-0011 — Anistia formal de EPIC-0054–0057 + immutability check**
- Adicionar todas as stories desses 4 épicos a `audits/execution-integrity-baseline.txt`.
- Adicionar todos os épicos correspondentes a `audits/rule-26-baseline.txt` (novo arquivo).
- Implementar checagem CI: baseline files são imutáveis após merge — qualquer adição posterior falha o build.
- Documentar a decisão em `adr/ADR-0015-zero-bypass-amnesty.md`.
- SIMPLE scope.

**story-0059-0012 — `taskTracking.enabled=true` mandatório para `flowVersion=2`**
- Atualizar Rule 19: para `flowVersion=2`, ausência de `taskTracking` ou `taskTracking.enabled=false` falha audit com `TASK_TRACKING_REQUIRED`.
- Migrar todos os `execution-state.json` de epics ativos via script idempotente.
- `--legacy-flow` continua aceitando `taskTracking.enabled=false` (para `flowVersion=1`).
- SIMPLE scope.

### 3.2 Dependências entre stories

```
0001 ── 0002 ── 0011
   \         \
    \         0008
     0007 ──── 0009
   /
0003 ── 0004 ── 0005 ── 0006
                              \
0010 ──────────────────────────── (transversal — pode rodar em paralelo)
0012 ──────────────────────────── (transversal — pode rodar em paralelo)
```

Phases derivadas:
- **Phase 0**: 0001, 0002 (sequencial — 0002 depende da estrutura tocada por 0001)
- **Phase 1**: 0003, 0004, 0005, 0006 (sequencial — 0006 depende de 0004+0005)
- **Phase 2**: 0007, 0008 (paralelo — touchpoints diferentes)
- **Phase 3**: 0009, 0010, 0011, 0012 (paralelo — temas independentes)

### 3.3 Out of scope

- Substituir o GitHub como provedor de PR/branch protection.
- Reescrever os orquestradores `x-story-implement` / `x-task-implement` / `x-epic-implement`.
- Criar UI / dashboard para visualizar evidência (futuro EPIC).
- Forçar uso de orquestrador em commits triviais (typo fix em README) — esses casos saem por `chore:` que não toca código de produção e não menciona STORY-ID.

---

## 4. Regras Transversais (Cross-cutting)

| ID | Regra | Aplicação |
| :--- | :--- | :--- |
| RULE-059-01 | Toda story do EPIC-0059 deve ela mesma ser implementada via `/x-story-implement` (dogfooding) | Falha auto-imposta se Phase 1 wave estiver ausente |
| RULE-059-02 | Cada story produz ≥ 1 acceptance test que tenta reproduzir o bypass coberto e valida que o gate dispara | Test smoke obrigatório |
| RULE-059-03 | Nenhuma story pode adicionar `<!-- audit-exempt -->` no próprio artefato | Self-check rejeita |
| RULE-059-04 | Mudanças em `audits/*-baseline.txt` exigem review humana (CODEOWNERS) | Configurado em story-0059-0009 |
| RULE-059-05 | `Rule 26` é loaded em toda conversa (renumerar índice de rules em CLAUDE.md) | Atualização do README de `.claude/rules/` |
| RULE-059-06 | Audit scripts novos seguem o contrato de exit codes da família existente (0=OK, 1=violation, 2=corrupt baseline, 3=invalid exemption, 4=enforcement broken) | Padronização |
| RULE-059-07 | Hooks novos respeitam `CLAUDE_TELEMETRY_DISABLED=1` mas NÃO respeitam outros bypass envs (intencional) | Sem env-var-based escape |

---

## 5. Definition of Ready (DoR) Global

Antes de iniciar implementação de cada story:

- [ ] Spec da story ≥ 1 página com Gherkin de aceitação
- [ ] Predecessoras todas `Concluída`
- [ ] Branch `epic/0059` existe e está atualizada com `develop`
- [ ] `plans/epic-0059/plans/` tem os 6 artefatos de planejamento da story (auto-imposto via RULE-059-01)
- [ ] Audit script alvo identificado (existente ou novo) e seu exit code reservado
- [ ] Baseline file path identificado (se aplicável)

## 6. Definition of Done (DoD) Global

Para encerrar cada story:

- [ ] Implementação merged via `/x-story-implement` (telemetria comprova)
- [ ] 6 artefatos Fase 1 + 4 artefatos Fase 3 presentes (auto-cobrado pelo audit já estendido)
- [ ] Smoke test do bypass coberto passa (gate dispara)
- [ ] Self-check do audit/hook adicionado retorna exit 0
- [ ] CHANGELOG atualizado (`### Added` ou `### Changed`)
- [ ] CLAUDE.md menciona o épico no bloco "In progress" / "Concluded"
- [ ] Cobertura ≥ 95% line / ≥ 90% branch nos arquivos tocados (Rule 05)

---

## 7. Riscos e Mitigações

| ID | Risco | Probabilidade | Impacto | Mitigação |
| :--- | :--- | :--- | :--- | :--- |
| R1 | Hook PreToolUse `enforce-no-bypass-flags.sh` produz falsos positivos e trava trabalho legítimo de recovery | Média | Alto | `CLAUDE_RECOVERY_MODE=1` env var explícita; documentar no `## Recovery` de cada SKILL.md |
| R2 | Pre-commit hook em `execution-state.json` quebra workflows existentes que escrevem direto | Alta | Alto | Migrar todos os call-sites para `x-internal-status-update` ANTES de ativar (story-0059-0004 inclui sweep) |
| R3 | CI re-rodar pre-commit chain inflate tempo de build em > 30% | Média | Médio | Cache de target/ + conditional execution (só se diff toca *.java) |
| R4 | Telemetria como prova-de-vida quebra se o operador roda orquestrador localmente sem committar `events.ndjson` | Média | Alto | Story-0059-0008 inclui hook que adiciona `events.ndjson` ao staging automaticamente após cada turn |
| R5 | Baseline immutability check rejeita correções legítimas de typo na baseline | Baixa | Baixo | `<!-- baseline-correction: <reason> -->` permite edição com review humana |
| R6 | Operador (Eder) acha o atrito intolerável e desativa hooks localmente | Média | Crítico | RULE-059-07 + audit no CI re-valida tudo no merge — bypass local não chega em PR |

---

## 8. Cronograma estimado

| Fase | Stories | Esforço estimado | Sequencial? |
| :--- | :--- | :--- | :--- |
| Phase 0 | 0001, 0002 | 1 dia | Sequencial (0002 depende de 0001) |
| Phase 1 | 0003, 0004, 0005, 0006 | 3 dias | Sequencial (0006 depende de 0004+0005) |
| Phase 2 | 0007, 0008 | 2 dias | Paralelo |
| Phase 3 | 0009, 0010, 0011, 0012 | 1 dia | Paralelo |
| **Total** | **12 stories** | **~7 dias** | |

---

## 9. Referências

- **Origem**: análise diagnóstica em sessão de 2026-04-26 (`epic/0057` HEAD `e184e0367`).
- **Rules tocadas**: 13 (skill invocation), 19 (backward compat — `flowVersion`), 21 (epic branch), 22 (skill visibility), 24 (execution integrity), 25 (task hierarchy).
- **ADRs novos**: ADR-0015 (zero-bypass amnesty for EPIC-0054–0057).
- **Audits estendidos**: `audit-execution-integrity.sh`, `audit-bypass-flags.sh` (já existentes); `audit-pr-evidence.sh` (novo); `audit-baseline-immutability.sh` (novo).
- **Hooks novos**: `.claude/hooks/enforce-no-bypass-flags.sh`, `.githooks/pre-commit`.
- **Templates novos**: `.github/pull_request_template.md`.
- **CHANGELOG**: cada story adiciona entrada própria em `### Added` ou `### Changed`.
