# Conventions — Super-Set de Desenvolvimento Assistido por IA

> **Propósito.** Este documento consolida as convenções que emergiram do conjunto de **Rules + Skills + Hooks + Knowledge Packs + Agents + Templates** que o projeto `ia-dev-environment` gera. Serve como **referência executiva** para demonstrar ganhos de padronização e como **guia técnico único** para onboarding de desenvolvedores e operadores que usam o workflow.
>
> **Status.** Representa o estado em **2026-04-24**, com a base consolidada (Rules 01–24, épicos até 0054 concluídos) e um roadmap de convenções em evolução (épicos 0055–0057).
>
> **Nota.** Este documento **referencia** as regras e artefatos. A fonte canônica (**source of truth**) é `java/src/main/resources/targets/claude/` — lá vivem as rules, skills, knowledge packs, agents, hooks e templates originais. O diretório `.claude/` é **output gerado** por `ia-dev-env generate` e **nunca** deve ser editado manualmente. Este documento referencia os paths do output por conveniência de leitura, mas a ordem canônica ao corrigir qualquer item é: alterar o SoT → regenerar `.claude/`. Nenhum trecho aqui substitui a regra original.

---

## Sumário

- [0. Sumário Executivo](#0-sumário-executivo)
- [1. Visão do Workflow End-to-End](#1-visão-do-workflow-end-to-end)
- [2. Convenções de Planejamento](#2-convenções-de-planejamento)
- [3. Convenções de Implementação (TDD)](#3-convenções-de-implementação-tdd)
- [4. Convenções de Review](#4-convenções-de-review)
- [5. Convenções de Git & Pull Requests](#5-convenções-de-git--pull-requests)
- [6. Convenções de Qualidade — Gates Não-Negociáveis](#6-convenções-de-qualidade--gates-não-negociáveis)
- [7. Convenções de Telemetria e Integridade](#7-convenções-de-telemetria-e-integridade)
- [8. Convenções de Modelos e Custo](#8-convenções-de-modelos-e-custo)
- [9. Convenções de Invocação de Skills](#9-convenções-de-invocação-de-skills)
- [10. Roadmap — Convenções em Evolução (Épicos 0055+)](#10-roadmap--convenções-em-evolução-épicos-0055)
- [11. Apêndices](#11-apêndices)
- [12. Referências Cruzadas](#12-referências-cruzadas)

---

## 0. Sumário Executivo

O super-set transformou um fluxo de desenvolvimento **ad-hoc** em um pipeline **determinístico, auditável e de custo controlado**. Oito dimensões mudaram de forma tangível:

| Dimensão | Antes do super-set | Com o super-set | Mecanismo / Regra |
| :--- | :--- | :--- | :--- |
| **Planejamento** | Individual, sem perspectivas cruzadas | 5 agentes especialistas em paralelo (Architect, QA, Security, Tech Lead, PO) produzindo artefatos estruturados | `x-story-plan` + Rule 13 SUBAGENT-GENERAL |
| **Implementação** | TDD opcional, sem rastro | Red-Green-Refactor obrigatório com **TPP** e tags em commits (`[TDD:RED]`, `[TDD:GREEN]`) | Rule 03, Rule 05, `x-test-tdd` |
| **Qualidade** | Coverage variável, deficits acumulam | **Absolute gate** ≥95% line / ≥90% branch, zero tolerância a deficits pré-existentes | Rule 05 (RULE-005-01) |
| **Review** | Um revisor humano, parcial | 7 especialistas paralelos + Tech Lead 45-point gate + dashboard consolidado | `x-review`, `x-review-pr`, templates |
| **Merge** | Squash direto em `develop` | **Epic branch** (`epic/XXXX`) como ponto de integração único + manual gate humano para `develop` | Rule 21, EPIC-0049 |
| **Telemetria** | Ausente | Phase markers NDJSON + análise P95 multi-épico + alertas de bottleneck | Rule 13 (seção Telemetry), `x-telemetry-analyze`, `x-telemetry-trend` |
| **Custo LLM** | `inherit` = ~84% Opus (caro) | Matriz explícita Opus/Sonnet/Haiku por camada (orquestrador, planner, reviewer, executor, utility, KP) | Rule 23, EPIC-0050 |
| **Integridade** | "Confio no modelo" | **Rule 24 Execution Integrity** — 4 camadas (normativa, runtime hook, CI audit, evidence files) que bloqueiam simulação inline | Rule 24, EPIC-0052 |

Em números:

- **11 Rules carregadas por padrão** (`01, 03, 04, 05, 06, 07, 08, 09, 13, 23` + project identity) com **slots condicionais 10/11/12** (`10-anti-patterns.*`, `11-security-pci`, `12-security-anti-patterns.*`) ativados por stack, e rules adicionais `14, 19, 21, 22, 24` presentes no SoT para projetos que fazem uso de worktrees, epic branches, visibilidade de skill e execution integrity
- **~73 Skills** (62 públicas + 11 internas `x-internal-*`) em 10 categorias — inventário por projeto em [Apêndice B](#apêndice-b--inventário-de-skills-por-categoria-73)
- **5 hook commands de telemetria registrados em `settings.json`** (nos eventos `SessionStart`, `PreToolUse`, `PostToolUse`, `SubagentStop`, `Stop`) + `post-compile-check` + `verify-story-completion`; além de **3 scripts auxiliares (libs)** em `.claude/hooks/` — `telemetry-emit.sh`, `telemetry-lib.sh`, `telemetry-phase.sh` — que são copiados, mas **não** são hooks registrados
- **17 Knowledge Packs core** garantidos por `SkillRegistry.CORE_KNOWLEDGE_PACKS` (coding-standards, architecture, testing, security, compliance, api-design, observability, resilience, infrastructure, protocols, story-planning, ci-cd-patterns, sre-practices, release-management, data-management, performance-engineering, feature-flags), mais packs condicionais adicionados por `KnowledgePackSelection` (p.ex. `layer-templates`, `database-patterns`, `data-modeling`, `pci-dss-requirements`, `owasp-asvs`, `dockerfile`, `k8s-*`)
- **Agentes com `Recommended Model` explícito** no gerador, distribuídos entre perfis Opus (deep planner) e Sonnet (reviewers, executors) conforme a configuração vigente — ver [Apêndice E](#apêndice-e--inventário-de-agentes)
- **12 templates de planejamento e review** emitidos em `.claude/templates/` pelo `PlanTemplatesAssembler` (RULE-003), cobrindo os sete estágios do workflow
- **7 fases rastreáveis** por telemetria (planning → implementation → review → release)

A tese operacional é simples: **padronizar o que pode ser padronizado, delegar o que pode ser delegado, auditar o que precisa ser auditado**. O restante do documento detalha cada dimensão.

---

## 1. Visão do Workflow End-to-End

```
┌────────────────────────────────────────────────────────────────────────┐
│                          spec-<domain>.md                              │
│                               │                                        │
│                               ▼                                        │
│   x-epic-create → x-epic-decompose → x-epic-map → x-epic-orchestrate   │
│                                                         │              │
│                                                         ▼              │
│                            x-story-create (1..N stories)               │
│                                                         │              │
│                                                         ▼              │
│                              x-story-plan                              │
│                  ┌─────────────┼─────────────┐                         │
│                  ▼             ▼             ▼                         │
│             Architect      QA/Sec/TL/PO   x-task-plan (per task)       │
│             (Opus)         (Sonnet)       ──────────────────           │
│                                                         │              │
│                                                         ▼              │
│                              x-epic-implement                          │
│                                   │                                    │
│                                   ▼                                    │
│   for each story (seq default, parallel via flag):                     │
│      x-story-implement → for each task: x-task-implement               │
│         │                         │                                    │
│         │                         ▼ (Double-Loop TDD)                  │
│         │             Red → Green → Refactor ────┐                     │
│         │                                         │                    │
│         │                      format → lint → compile → x-git-commit  │
│         │                                                              │
│         ├→ x-pr-create (target: epic/XXXX, auto-merge)                 │
│         ├→ x-pr-watch-ci (8 exit codes)                                │
│         ├→ x-review (paralelo: QA, Sec, Perf, DevOps, DB, Arch)        │
│         ├→ x-review-pr (Tech Lead gate)                                │
│         └→ x-internal-story-verify (evidence artifacts — Rule 24)      │
│                                                                        │
│   (after all stories merged to epic/XXXX)                              │
│                                                                        │
│   MANUAL GATE: PR epic/XXXX → develop (human reviewer)                 │
│                                                                        │
│   x-release (release/X.Y.Z → main tag → back-merge develop)            │
└────────────────────────────────────────────────────────────────────────┘

Observabilidade transversal:
  • Hooks: SessionStart / PreToolUse / PostToolUse / SubagentStop / Stop
  • Telemetria: plans/epic-XXXX/telemetry/events.ndjson (NDJSON)
  • Estado: plans/epic-XXXX/execution-state.json (flowVersion, storyStatuses)
  • Rule 24: evidence files obrigatórios em plans/epic-XXXX/{plans,reports}/
```

---

## 2. Convenções de Planejamento

### 2.1 Hierarquia Epic › Story › Task

Três níveis canônicos, com schemas de artefato:

| Nível | Identificador | Arquivo | Conteúdo |
| :--- | :--- | :--- | :--- |
| Epic | `EPIC-XXXX` | `plans/epic-XXXX/epic-XXXX.md` | Visão, regras transversais, DoR/DoD global, índice de stories |
| Story | `story-XXXX-YYYY` | `plans/epic-XXXX/story-XXXX-YYYY.md` | Contratos, Gherkin AC, dependências, tasks |
| Task (v2) | `TASK-XXXX-YYYY-NNN` | `plans/epic-XXXX/plans/task-TASK-XXXX-YYYY-NNN.md` | I/O contract, testability, exit criteria |

**Dois schemas convivem (Rule 19 — Backward Compatibility):**

| Schema | Introduzido por | Task como | `x-task-plan` |
| :--- | :--- | :--- | :--- |
| **v1** (legacy) | Pré-EPIC-0038 | Sub-seção dentro de `tasks-story-*.md` | Órfão (não invocado) |
| **v2** (task-first) | EPIC-0038 | Arquivo próprio + `plan-task-*.md` | Invocada 1× por task |

O campo discriminador `planningSchemaVersion` (`"1.0"` | `"2.0"`) vive em `execution-state.json`. Ausência = v1 com warning.

### 2.2 Nomenclatura Canônica de Artefatos

| Artefato | Padrão | Regex |
| :--- | :--- | :--- |
| Epic | `epic-XXXX.md` | `^epic-\d{4}\.md$` |
| Story | `story-XXXX-YYYY.md` | `^story-\d{4}-\d{4}\.md$` |
| Task (v2) | `task-TASK-XXXX-YYYY-NNN.md` | `^task-TASK-\d{4}-\d{4}-\d{3}\.md$` |
| Plano story | `plan-story-XXXX-YYYY.md` | — |
| Plano task | `plan-task-TASK-XXXX-YYYY-NNN.md` | — |
| Testes story | `tests-story-XXXX-YYYY.md` | — |
| Mapa tasks | `task-implementation-map-story-XXXX-YYYY.md` | — |
| Review spec | `review-story-XXXX-YYYY.md` | — |
| Review TL | `techlead-review-story-XXXX-YYYY.md` | — |
| Verify envelope | `verify-envelope-STORY-ID.json` | Rule 24 evidence |
| Story report | `story-completion-report-STORY-ID.md` | Rule 24 evidence |

### 2.3 Templates de Planejamento e Review

**12 templates principais** vivem em `.claude/templates/_TEMPLATE-*.md` (SoT: `java/src/main/resources/shared/templates/`). Placeholders `{{KEY}}` são resolvidos em runtime pelo LLM (não na geração). Projetos podem estender com templates adicionais por stack; o conjunto abaixo agrupa os emitidos pelo `PlanTemplatesAssembler` nos sete estágios do workflow — nem todos os nomes listados são obrigatórios, e a lista reflete a convenção nomenclatural alvo quando o épico 0056 (RA9) estiver concluído:

| Estágio | Templates |
| :--- | :--- |
| Epic | `_TEMPLATE-EPIC.md`, `_TEMPLATE-EPIC-EXECUTION-PLAN.md`, `_TEMPLATE-IMPLEMENTATION-MAP.md` |
| Story | `_TEMPLATE-STORY.md`, `_TEMPLATE-STORY-PLANNING-REPORT.md`, `_TEMPLATE-TASK-BREAKDOWN.md` |
| Task | `_TEMPLATE-TASK.md`, `_TEMPLATE-TASK-PLAN.md`, `_TEMPLATE-TASK-IMPLEMENTATION-MAP.md`, `_TEMPLATE-IMPLEMENTATION-PLAN.md`, `_TEMPLATE-TEST-PLAN.md` |
| Arquitetura | `_TEMPLATE-ARCHITECTURE-PLAN.md` |
| Review | `_TEMPLATE-SPECIALIST-REVIEW.md`, `_TEMPLATE-TECH-LEAD-REVIEW.md`, `_TEMPLATE-CONSOLIDATED-REVIEW-DASHBOARD.md`, `_TEMPLATE-REVIEW-REMEDIATION.md` |
| Quality gates | `_TEMPLATE-DOR-CHECKLIST.md`, `_TEMPLATE-SECURITY-ASSESSMENT.md`, `_TEMPLATE-COMPLIANCE-ASSESSMENT.md` |
| Release/Closure | `_TEMPLATE-STORY-COMPLETION-REPORT.md`, `_TEMPLATE-PHASE-COMPLETION-REPORT.md`, `_TEMPLATE-EPIC-EXECUTION-REPORT.md` |

A renderização é feita por `x-internal-report-write` (placeholders simples + `{{#each}}` loops + modo `--append` com dedup por `## ID:`).

### 2.4 Multi-Agent Planning (5 Subagents Paralelos)

`x-story-plan` dispara — em **uma única mensagem** do assistente — cinco `Agent(subagent_type: "general-purpose")` como siblings:

| Subagent | Role | Modelo (Rule 23) |
| :--- | :--- | :--- |
| Architect | Design, ADRs, plano de implementação | **Opus** |
| QA Engineer | Plano de testes (Double-Loop + TPP) | Sonnet |
| Security Engineer | Security Assessment (OWASP, STRIDE) | Sonnet |
| Tech Lead | Task breakdown + priorização | Sonnet |
| Product Owner | Compliance Assessment + DoR validation | Sonnet |

Padrão de batching (Rule 13): **Batch A** = `TaskCreate` + `Agent(...)` × 5 em siblings → **Batch B** = `TaskUpdate` × 5 em siblings quando os resultados retornam.

---

## 3. Convenções de Implementação (TDD)

### 3.1 Double-Loop TDD + TPP

**Outer loop** (acceptance): cada Gherkin scenario em `story-XXXX-YYYY.md#seção-7` precisa de um teste de aceitação que falha inicialmente.
**Inner loop** (unit): Red → Green → Refactor atômico por transformação.

**TPP (Transformation Priority Premise)** — ordem canônica:

```
{}  →  nil  →  constant  →  constant+  →  scalar  →
  collection  →  iterated  →  recursive  →  generic
```

Um teste por transformação. Um commit por ciclo completo. Refactoring **nunca** adiciona comportamento — se comportamento muda, novo ciclo RED começa.

### 3.2 Pre-Commit Chain (Rule 07)

Ordem mandatória antes de todo commit de código:

```
x-code-format  →  x-code-lint  →  compile ({{COMPILE_COMMAND}})  →  x-git-commit
```

Qualquer etapa que falhar **aborta** a chain. Arquivos auto-reformatados são re-staged automaticamente. Exceções (`.md` na raiz, planning artifacts) usam `x-planning-commit` que pula format/lint/compile.

### 3.3 Conventional Commits + Task ID + TDD Tags

Formato canônico:

```
<type>(<TASK-XXXX-YYYY-NNN>): <subject> [TDD:<TAG>]

<body opcional>

<footer opcional: Coalesces-with / Relates-to / Closes>
```

| Componente | Valores |
| :--- | :--- |
| `type` | `feat`, `fix`, `test`, `refactor`, `docs`, `chore`, `perf` |
| `TDD:TAG` | `RED`, `GREEN`, `REFACTOR` (omitido em `docs:` / `chore:`) |
| Scope | Task ID obrigatório em commits de código; `(epic-XXXX)` aceito em commits de planejamento |

**Bump de versão** (Rule 08): `feat` = MINOR, `fix` / `perf` = PATCH, `feat!` / `BREAKING CHANGE` = MAJOR.

### 3.4 Worktrees (Rule 14)

Execução paralela e isolada usa git worktrees sob `.claude/worktrees/{identifier}/`:

| Escopo | Padrão | Branch base |
| :--- | :--- | :--- |
| Task | `.claude/worktrees/task-XXXX-YYYY-NNN/` | Parent story branch |
| Story | `.claude/worktrees/story-XXXX-YYYY/` | `epic/XXXX` (v2) ou `develop` (v1) |
| Epic | `.claude/worktrees/epic-XXXX/` | `develop` |

**Invariantes:**
- **Creator-owned removal** — quem criou, remove.
- **Preservar em falha** — remoção só em sucesso.
- **Idempotência** — segunda criação com mesmo id é no-op.

---

## 4. Convenções de Review

### 4.1 Paralelo Multi-Especialista (`x-review`)

Sete specialists disparam em paralelo em **uma única mensagem**:

| Skill | Foco | Ativação |
| :--- | :--- | :--- |
| `x-review-qa` | Coverage, TDD compliance, naming | Sempre |
| `x-review-perf` | N+1, pools, pagination, timeouts, circuit breakers | Sempre |
| `x-review-devops` | Dockerfile, container security, CI/CD, probes | Sempre |
| `x-review-security` | OWASP Top 10, anti-patterns, threat model, PCI | Condicional (se `security`/`pci` habilitado no perfil) |
| `x-review-arch` | Layering, SOLID, domain purity | Condicional |
| `x-review-db` | Migrações, queries, índices | Condicional (se há database) |
| `x-review-obs` / `x-review-api` / `x-review-event` | Observabilidade, API, eventos | Condicional por stack |
| `x-review-pr` | Tech Lead holistic (45-point) | Sempre (gate final) |

A lista exata é montada por `x-review` com base no perfil do projeto — esta tabela ilustra o conjunto típico e não substitui o SKILL.md do `x-review` como fonte canônica de ativação.

Resultado agregado em `_TEMPLATE-CONSOLIDATED-REVIEW-DASHBOARD.md`.

### 4.2 Tech Lead Gate — 45-Point Checklist

`x-review-pr` aplica checklist cobrindo Clean Code, SOLID, arquitetura, convenções de framework, testes, TDD process, security, cross-file consistency. Emite decisão **GO / NO-GO**.

### 4.3 Interactive Gates — PROCEED / FIX-PR / ABORT (Rule 20, ADR-0010)

Gates interativos em skills orquestradoras (`x-release`, `x-story-implement`, `x-epic-implement`, `x-review-pr`) usam menu fixo de 3 opções:

| Opção | Ação |
| :--- | :--- |
| **PROCEED** | Avança no fluxo |
| **FIX-PR** | Invoca `x-pr-fix` / `x-pr-fix-epic` e volta ao mesmo gate |
| **ABORT** | Interrompe com exit code |

Guard-rail: **3 FIX-PR consecutivos** disparam `GATE_FIX_LOOP_EXCEEDED`. Flag `--non-interactive` para CI.

---

## 5. Convenções de Git & Pull Requests

### 5.1 Git Flow + Epic Branch Model (Rules 09, 21)

```
main  ←── release/*  ←── develop  ←── epic/XXXX  ←── feat/task-*
       ▲                  (manual gate)
       │
hotfix/* ───┘
```

| Branch | Criado de | Merge em | Merge strategy |
| :--- | :--- | :--- | :--- |
| `epic/XXXX` | `develop` | `develop` (manual) | Merge commit |
| `feat/*` (v2) | `epic/XXXX` | `epic/XXXX` | Auto-merge (merge) |
| `feat/*` (v1) | `develop` | `develop` | Squash |
| `release/*` | `develop` | `main` + `develop` | Merge commit |
| `hotfix/*` | `main` | `main` + `develop` | Merge commit |

### 5.2 Branch Naming

| Tipo | Padrão | Exemplo |
| :--- | :--- | :--- |
| Epic | `epic/XXXX` | `epic/0049` |
| Task | `feat/task-XXXX-YYYY-NNN-<desc>` | `feat/task-0049-0003-002-add-planner` |
| Story | `feat/story-XXXX-YYYY-<desc>` | `feat/story-0049-0003-inline-plan` |
| Release | `release/X.Y.Z` | `release/3.8.0` |
| Hotfix | `hotfix/<ticket>-<desc>` | `hotfix/PROJ-456-crash-fix` |

**Invariantes Rule 21:**
- Uma branch por epic ID — `epic/0049-refactor` é proibido.
- `x-git-cleanup-branches` **não** remove `epic/*` até o manual gate passar.
- Em `--parallel`, story worktrees usam `epic/XXXX` como base, **nunca** `develop`.

### 5.3 PR Lifecycle com CI-Watch (Rule 21 CI-Watch, EPIC-0045)

Fluxo canônico de PR:

```
x-pr-create --target-branch epic/XXXX --auto-merge merge
     ↓
x-pr-watch-ci  (polling, 8 exit codes estáveis)
     ↓
  SUCCESS=0 │ CI_PENDING_PROCEED=10 │ CI_FAILED=20 │ TIMEOUT=30
  PR_ALREADY_MERGED=40 │ NO_CI_CONFIGURED=50 │ PR_CLOSED=60 │ PR_NOT_FOUND=70
     ↓
(se CI_FAILED) x-pr-fix  →  commit  →  retry CI-Watch
(se SUCCESS)   auto-merge via GitHub native
```

Opt-out em contextos de recovery: `--no-ci-watch` (uso restrito, auditado).

### 5.4 Merge Train e Auto-Merge

`x-pr-merge-train` descobre, valida e mergea uma sequência determinística de PRs em `develop`. Modos de descoberta: `--prs`, `--epic`, `--pattern`. `--dry-run` para auditoria.

---

## 6. Convenções de Qualidade — Gates Não-Negociáveis

### 6.1 Coverage Absolute Gate (Rule 05 — RULE-005-01)

| Métrica | Mínimo | Comportamento |
| :--- | :--- | :--- |
| Line coverage | ≥ 95% | **Absolute gate** — pre-existing deficits não são grandfathered |
| Branch coverage | ≥ 90% | Idem |

Se a PR derrubar (ou herdar) coverage abaixo do limite, o merge falha. Três saídas:

1. Adicionar testes na PR atual.
2. Abrir PR antecessora que fecha o gap em `develop` primeiro.
3. ADR com exceção temporária + sunset date.

Silenciar o gate é **proibido**.

### 6.2 Coding Standards (Rule 03)

| Métrica | Limite |
| :--- | :--- |
| Método/função | ≤ 25 linhas |
| Classe/módulo | ≤ 250 linhas |
| Parâmetros por função | ≤ 4 (usar parameter object acima disso) |
| Largura de linha | ≤ 120 caracteres |
| Train wreck (cadeias `.`) | ≤ 2 níveis |

**Proibições:** boolean flags como parâmetro; comentários que repetem código; mutable global state; wildcard imports; `sleep()` para sincronização; `System.out/err` / `print()` em produção; concatenação com `+` em mensagens; fully qualified names quando import basta; mutable fields em data carriers imutáveis; duplicação (DRY estrito).

### 6.3 Domain Purity + Hexagonal (Rule 04)

```
adapter.inbound → application → domain ← adapter.outbound
                                  ↑
                           (ports/interfaces)
```

Domain **não** importa: serialização (Jackson, Gson, serde), I/O, frameworks. Se precisar: define **port**, implementa em adapter.

Inbound adapters **devem** chamar use cases; orquestração direta de domain services é proibida.

### 6.4 Security Baseline + Anti-patterns (Rules 06, 12)

Secure defaults: safe deserialization, full-spec escaping, restricted temp permissions, path canonicalization + prefix check, erro sem vazar internals, cryptographic RNG (nunca `Math.random()` / `rand()`).

Java anti-patterns catalogados (Rule 12) com pares vulnerable/fixed: SQL concat (CWE-89), `Math.random()` security (CWE-330), `ObjectInputStream` sem whitelist (CWE-502), hard-coded credentials (CWE-798), trust-all `X509TrustManager` (CWE-295), path sem normalização (CWE-22), exception message em HTTP response (CWE-209), CORS `*` (CWE-942).

---

## 7. Convenções de Telemetria e Integridade

### 7.1 Phase Markers + NDJSON (Rule 13 seção Telemetry)

Toda skill de implementação emite, em cada fase numerada:

```markdown
<!-- TELEMETRY: phase.start -->
Bash: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start <skill> Phase-N-<Name>
... phase body ...
<!-- TELEMETRY: phase.end -->
Bash: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end <skill> Phase-N-<Name> ok|failed|skipped
```

Eventos persistidos em `plans/epic-XXXX/telemetry/events.ndjson` (NDJSON). Helper **fail-open** — telemetria quebrada **não** aborta skill.

Subagent markers (`subagent.start` / `subagent.end`) existem para planning skills com dispatch paralelo.

CI `TelemetryMarkerLint` detecta `DUPLICATE_START`, `DUPLICATE_END`, `DANGLING_END`, `UNCLOSED_START`.

### 7.2 `execution-state.json` — flowVersion (Rule 19)

Estado executivo único por epic. Campo discriminador **`flowVersion`**:

| Valor | Fluxo |
| :--- | :--- |
| `"1"` | Legacy (pré-EPIC-0049): story PRs → `develop`, sem epic branch |
| `"2"` | Novo (EPIC-0049+): story PRs → `epic/XXXX`, manual gate → `develop` |
| ausente / inválido | Fallback para `"1"` com warning visível |

Flag `--legacy-flow` força v1 em epic novo. Deprecation window: 2 releases pós-EPIC-0049. Mixing mode (stories v1 em epic v2) é permitido durante a janela.

Novos campos em `execution-state.json` **devem** ser opcionais com fallback documentado (Rule 19).

### 7.3 Rule 24 — Execution Integrity (4 Camadas)

Toda declaração `Skill(skill: "...", args: "...")` em SKILL.md é **TOOL CALL OBRIGATÓRIO**. Simular inline é violação.

| Camada | Mecanismo |
| :--- | :--- |
| **1 — Normativa** | Rule 24 + CLAUDE.md assertivo + SKILL.md com "MANDATORY TOOL CALL" |
| **2 — Runtime Stop hook** | `.claude/hooks/verify-story-completion.sh` — checa evidence files em disco ao término do turno |
| **3 — CI Audit** | `scripts/audit-execution-integrity.sh` — exit codes `OK=0`, `EIE_EVIDENCE_MISSING=1`, `EIE_BASELINE_CORRUPT=2`, `EIE_INVALID_EXEMPTION=3` |
| **4 — Observabilidade** | Evidence file **é** a prova; ausência = sub-skill não invocada |

Escape hatches únicos: flags `--skip-*` (somente em `## Recovery`) e `<!-- audit-exempt: <reason> -->` em story markdown (raro).

### 7.4 Evidence Artifacts Obrigatórios

| Sub-skill | Artefato | Caminho |
| :--- | :--- | :--- |
| `x-internal-story-verify` | `verify-envelope-STORY-ID.json` | `plans/epic-XXXX/reports/` |
| `x-review` | `review-story-STORY-ID.md` | `plans/epic-XXXX/plans/` |
| `x-review-pr` | `techlead-review-story-STORY-ID.md` | `plans/epic-XXXX/plans/` |
| `x-internal-story-report` | `story-completion-report-STORY-ID.md` | `plans/epic-XXXX/reports/` |
| `x-arch-plan` | `arch-story-STORY-ID.md` | `plans/epic-XXXX/plans/` (soft check) |

Ausência em story merged = build falha no CI.

---

## 8. Convenções de Modelos e Custo

### 8.1 Matriz de Modelo (Rule 23, EPIC-0050)

| Camada | Tier padrão | Exemplos | Justificativa |
| :--- | :--- | :--- | :--- |
| Orchestrator | **Sonnet** | `x-epic-implement`, `x-story-implement`, `x-release`, `x-review`, `x-task-implement` | Dispatch, sem deep reasoning inline |
| Deep Planner | **Opus** | `x-arch-plan`, Architect subagent | ADRs, trade-offs, design |
| Reviewer | **Sonnet** | `x-review-qa`, `x-review-perf`, `x-review-pr`, `x-review-devops` | Checklist estruturado |
| Executor | **Sonnet** | `x-task-implement`, `x-test-tdd` | TDD procedural |
| Utility | **Haiku** | `x-git-worktree`, `x-git-commit`, `x-code-format`, `x-code-lint` | Ops sem reasoning |
| Knowledge Pack | **Haiku** | `architecture`, `coding-standards`, `testing`, `layer-templates`, `patterns`, `dockerfile` | Read-only reference |

**Pontos de enforcement (3 contratos técnicos):**

1. **Frontmatter YAML** — `model:` obrigatório em SKILL.md de orquestradores.
2. **`Agent(subagent_type: "general-purpose", model: "...")`** — explícito, sem inheritance.
3. **`Skill(skill: "...", model: "...")`** — explícito quando tier diverge do pai.

`Adaptive` é **proibido** (resolve para Opus em prática). Baseline antes: 84.4% Opus / 0.2% Sonnet / 5.8% Haiku. Alvo: ≤50% Opus / ≥35% Sonnet / ≥12% Haiku. CI script `scripts/audit-model-selection.sh` faz cumprir essa regra.

### 8.2 Skill Visibility (Rule 22)

Duas classes de skill:

| Classe | Prefixo | User-invocável? | `/help`? |
| :--- | :--- | :--- | :--- |
| **Público** | `x-{subject}-{action}` | Sim | Sim |
| **Interno** | `x-internal-{subject}-{action}` | **Não** | **Não** |

Frontmatter interno **obrigatório**:

```yaml
visibility: internal
user-invocable: false
```

Body marker obrigatório:

```markdown
> 🔒 **INTERNAL SKILL** — Invoked only by other skills via the Skill tool. Not user-invocable.
```

Audit: `scripts/audit-skill-visibility.sh` (exit `SKILL_VISIBILITY_VIOLATION`). **11 skills internas** atuais: `x-internal-args-normalize`, `x-internal-status-update`, `x-internal-report-write`, `x-internal-story-load-context`, `x-internal-story-build-plan`, `x-internal-story-verify`, `x-internal-story-resume`, `x-internal-story-report`, `x-internal-epic-build-plan`, `x-internal-epic-integrity-gate`, `x-internal-epic-branch-ensure`.

---

## 9. Convenções de Invocação de Skills

### 9.1 Três Patterns Permitidos (Rule 13)

Delegação skill-to-skill usa **exclusivamente** um destes padrões. A forma bare-slash `/x-foo` é **proibida** em contexto de delegação (silenciosamente cai em execução inline).

**Pattern 1 — INLINE-SKILL** (síncrono, valor de retorno direto):

```markdown
Invoke the `x-foo` skill via the Skill tool:

    Skill(skill: "x-foo", args: "--flag value")
```

Frontmatter `allowed-tools` deve incluir `Skill`.

**Pattern 2 — SUBAGENT-GENERAL** (isolamento de contexto, possível paralelismo):

```markdown
Agent(
  subagent_type: "general-purpose",
  description: "Run x-foo for X",
  prompt: "FIRST ACTION: TaskCreate(...). Invoke x-foo via the Skill tool: Skill(skill: \"x-foo\", args: \"...\"). LAST ACTION: TaskUpdate(...)."
)
```

Paralelismo: múltiplos `Agent(...)` como **siblings** em **uma única** mensagem do assistente.

**Pattern 3 — SUBAGENT-RESEARCH** (exploração read-only, sem delegar skill):

```markdown
Agent(
  subagent_type: "Explore",
  description: "Find references to X",
  prompt: "Search the codebase for ... Report file:line matches."
)
```

**Exceções permitidas** do bare-slash: seções `## Triggers`, `## Examples`, tabelas de documentação, README e CHANGELOG — nesses contextos `/x-foo` é literal (usuário digita no chat).

---

## 10. Roadmap — Convenções em Evolução (Épicos 0055+)

Convenções **propostas** em épicos ainda não concluídos. **Não são vigentes** — documentadas aqui para preparar operadores para a próxima iteração.

### 10.1 EPIC-0055 — Task Hierarchy & Phase Gate Enforcement

**Problema.** O Task API do Claude Code permite apenas 1 nível de hierarquia; épicos grandes perdem rastreabilidade entre Epic › Story › Phase › Wave.

**Convenções propostas:**

- **RULE-25 — Task Hierarchy & Phase Gate Contract** (`.claude/rules/25-task-hierarchy.md`).
- Separador hierárquico **`›`** (U+203A) no campo `subject` de `TaskCreate`, até 4 níveis:
  `story-0055-0001 › Phase 1 › Rule 25 spec`
  `TASK-0060-0001-003 › Step 2 › Cycle 1 › Red`
- Regex válida: `^[A-Z0-9\-]+ › .* › .* › .*$` (no máximo 4 segmentos).
- Campo canônico para dependências: `addBlockedBy` (renderiza como `blocked by #N`).
- Nova skill **`x-internal-phase-gate`** (tier Haiku, visibility internal) com modos:
  - `--mode pre` — valida ausência de tasks órfãs da fase anterior.
  - `--mode post` — valida completude de sub-tasks + evidence artifacts.
  - `--mode wave` — valida conclusão de Batch B de wave paralela.
  - `--mode final` — gate terminal (compõe com `x-internal-epic-integrity-gate`).
  - Exit codes: `0=OK`, `12=PHASE_GATE_FAILED`, `13=PHASE_GATE_MALFORMED`, `14=PHASE_GATE_TIMEOUT`.
- Novo campo em `execution-state.json`:
  ```json
  "taskTracking": { "enabled": true, "phaseGateResults": [ ... ] }
  ```
  Default `false` para legacy (backward compat via Rule 19).

### 10.2 EPIC-0056 — RA9 Standardized Planning Templates

**Problema.** Templates de Epic/Story/Task possuem seções inconsistentes; decisões arquiteturais ficam implícitas.

**Convenções propostas:**

- **9 seções fixas** (ordem invariável) em Epic, Story e Task:
  1. **Contexto & Escopo**
  2. **Packages (Hexagonal)** (novo) — estrutura nas 5 camadas
  3. **Contratos & Endpoints** (novo) — APIs, interfaces, eventos
  4. **Materialização SOLID**
  5. **Quality Gates**
  6. **Segurança**
  7. **Observabilidade**
  8. **Decision Rationale** (novo — obrigatório em Epic/Story; opcional em Task como "N/A")
  9. **Dependências & File Footprint**
- Micro-template Decision Rationale (4 linhas):
  `**Decisão:** … **Motivo:** … **Alternativa descartada:** … **Consequência:** …`
- Knowledge pack **`planning-standards-kp`** (não invocável; referenciado via `@planning-standards-kp`) como fonte única das 9 seções.
- Audit rules novas: `RA9_SECTIONS_MISSING`, `RA9_RATIONALE_EMPTY`, `RA9_PACKAGES_MISSING`.
- Escape hatch: `<!-- audit-exempt -->` (raro, revisado).

### 10.3 EPIC-0057 — CI-Watch Integrity (extensão Rule 24)

**Problema.** CI-Watch (Rule 21 CI-Watch) carece de auditoria de evidence; exit codes inconsistentes entre orquestradores.

**Convenções propostas:**

- **RULE-45 — CI-Watch Integrity** (`.claude/rules/45-ci-watch-integrity.md`).
- 8 exit codes canônicos reafirmados:
  `0=CI_PASSED`, `1=CI_FAILED`, `2=CI_TIMEOUT`, `3=CI_ABORTED`, `4=COPILOT_CHANGES_REQUESTED`, `5=COPILOT_TIMEOUT`, `6=PR_NOT_FOUND`, `7=CI_SKIPPED`.
- Opt-out flag `--no-ci-watch` restrito a `## Recovery` / CI/automation.
- Marker obrigatório `MANDATORY TOOL CALL — NON-NEGOTIABLE (Rule 24)` antes de cada invocação de `x-pr-watch-ci` em orquestrador.
- Fallback matrix explícita para CI ausente / Copilot indisponível.
- Novos evidence files em `.claude/state/pr-watch-*.json` auditados pelo Stop hook (Camada 2).

---

## 11. Apêndices

### Apêndice A — Inventário de Rules

Paths do **source of truth** (`java/src/main/resources/targets/claude/rules/`). No output gerado `.claude/rules/` alguns arquivos recebem nomes "planos" (p.ex. `12-security-anti-patterns.md` em vez de `12-security-anti-patterns.java.md`) — o assembler resolve a variante por stack.

**Base (sempre presentes no SoT):**

| # | Path no SoT | Escopo | Epic de origem |
| :--- | :--- | :--- | :--- |
| 01 | `rules/01-project-identity.md` | Identidade do projeto | — |
| 03 | `rules/03-coding-standards.md` | Padrões de código | — |
| 04 | `rules/04-architecture-summary.md` | Arquitetura (Hexagonal) | — |
| 05 | `rules/05-quality-gates.md` | Coverage absolute gate | — |
| 06 | `rules/06-security-baseline.md` | Secure defaults | — |
| 07 | `rules/07-operations-baseline.md` | SRE / observabilidade | — |
| 08 | `rules/08-release-process.md` | SemVer + Conventional Commits + CHANGELOG | — |
| 09 | `rules/09-branching-model.md` | Git Flow | — |
| 13 | `rules/13-skill-invocation-protocol.md` | 3 patterns + telemetria markers | EPIC-0033 / 0040 |
| 14 | `rules/14-project-scope.md` | Escopo de código + worktree lifecycle | EPIC-0049 |
| 19 | `rules/19-backward-compatibility.md` | `flowVersion` + deprecation window | EPIC-0049 |
| 21 | `rules/21-epic-branch-model.md` | Epic branch como integração única | EPIC-0049 |
| 22 | `rules/22-skill-visibility.md` | `x-internal-*` convention | EPIC-0049 |
| 23 | `rules/23-model-selection.md` | Matriz Opus/Sonnet/Haiku | EPIC-0050 |
| 24 | `rules/24-execution-integrity.md` | 4 camadas de enforcement | EPIC-0052 |

**Condicionais (`rules/conditional/`, ativadas por stack/profile):**

| # | Path no SoT | Escopo | Ativação |
| :--- | :--- | :--- | :--- |
| 09 (data) | `rules/conditional/09-data-management.md` | Regras de dados (quando há database) | Projeto com banco |
| 10 | `rules/conditional/anti-patterns/10-anti-patterns.{stack}.md` | Anti-patterns por stack (Java-Spring-Boot / Java-Quarkus) | Java Spring/Quarkus |
| 11 | `rules/conditional/11-security-pci.md` | PCI-DSS v4 enforcement | Perfil PCI |
| 12 | `rules/conditional/security-anti-patterns/12-security-anti-patterns.{lang}.md` | Anti-patterns de segurança por linguagem (p.ex. `.java`) | Stack correspondente |

**Rule 02 — `02-domain.md`:** é um artefato **gerado pelo assembler** a partir da configuração de domínio do projeto; não existe como arquivo fixo no SoT. No output `.claude/rules/02-domain.md`, o template é preenchido com entidades, value objects e regras específicas do projeto.

Gaps numéricos (15–18, 20) ficam reservados para rules futuras.

### Apêndice B — Inventário de Skills por Categoria (~73)

| Categoria | Qtd | Exemplos |
| :--- | :--- | :--- |
| Planning & Architecture | 10 | `x-arch-plan`, `x-arch-update`, `x-adr-generate`, `x-epic-create`, `x-epic-decompose`, `x-epic-map`, `x-epic-orchestrate`, `x-story-create`, `x-story-plan`, `x-task-plan` |
| Implementation & Testing | 13 | `x-epic-implement`, `x-story-implement`, `x-task-implement`, `x-test-tdd`, `x-test-plan`, `x-test-run`, `x-test-e2e`, `x-code-format`, `x-code-lint`, `x-code-audit`, `x-doc-generate`, `x-ci-generate`, `x-dependency-audit` |
| Security & Compliance | 8 | `x-owasp-scan`, `x-threat-model`, `x-security-pipeline`, `x-security-dashboard`, `x-hardening-eval`, `x-supply-chain-audit`, `x-runtime-eval`, `x-spec-drift` |
| Git & PR | 10 | `x-git-branch`, `x-git-commit`, `x-git-merge`, `x-git-push`, `x-git-worktree`, `x-git-cleanup-branches`, `x-pr-create`, `x-pr-fix`, `x-pr-merge`, `x-pr-merge-train` |
| Code Review | 6 | `x-review`, `x-review-pr`, `x-review-qa`, `x-review-perf`, `x-review-devops`, `x-parallel-eval` |
| Release & Observability | 5 | `x-release`, `x-release-changelog`, `x-telemetry-analyze`, `x-telemetry-trend`, `x-perf-profile` |
| Operations | 3 | `x-ops-incident`, `x-ops-troubleshoot`, `x-setup-env` |
| Platform & Integrations | 7 | `x-mcp-recommend`, `x-pr-watch-ci`, `x-pr-fix-epic`, `x-jira-create-epic`, `x-jira-create-stories`, `x-planning-commit`, `x-status-reconcile` |
| Internal (`x-internal-*`) | 11 | `x-internal-args-normalize`, `x-internal-status-update`, `x-internal-report-write`, `x-internal-story-load-context`, `x-internal-story-build-plan`, `x-internal-story-verify`, `x-internal-story-resume`, `x-internal-story-report`, `x-internal-epic-build-plan`, `x-internal-epic-integrity-gate`, `x-internal-epic-branch-ensure` |

### Apêndice C — Inventário de Hooks

**Hooks registrados em `settings.json`** (7 comandos em 6 eventos):

| Hook | Evento | Papel |
| :--- | :--- | :--- |
| `telemetry-session.sh` | SessionStart | Emite evento `session.start` |
| `telemetry-pretool.sh` | PreToolUse `*` | Emite `tool.precall` |
| `telemetry-posttool.sh` | PostToolUse `*` | Emite `tool.postcall` com status |
| `post-compile-check.sh` | PostToolUse `Write\|Edit` | Valida compile pós-edição |
| `telemetry-subagent.sh` | SubagentStop | Captura resultado de subagent |
| `telemetry-stop.sh` | Stop | Emite `session.end` |
| `verify-story-completion.sh` | Stop | Rule 24 Camada 2 — verifica evidence files |

**Scripts auxiliares (libs)** copiados para `.claude/hooks/` mas **não** registrados como hooks — consumidos via `source` pelos hooks registrados e invocados por `telemetry-phase.sh start|end` dentro das skills:

| Script | Papel |
| :--- | :--- |
| `telemetry-emit.sh` | Escrita NDJSON com scrubber de privacidade (Rule 20 telemetria) |
| `telemetry-lib.sh` | Utilitários compartilhados de resolução de caminho e env |
| `telemetry-phase.sh` | Helper `start`/`end`/`subagent-start`/`subagent-end` fail-open (Rule 13 seção Telemetry) |

### Apêndice D — Inventário de Knowledge Packs

A fonte canônica é `SkillRegistry.CORE_KNOWLEDGE_PACKS` (`java/src/main/java/dev/iadev/domain/stack/SkillRegistry.java`). Packs adicionais são habilitados por `KnowledgePackSelection` baseado em stack/profile.

**17 Core KPs (sempre presentes):**

| KP | Categoria | Escopo |
| :--- | :--- | :--- |
| `coding-standards` | Quality | Naming, limites, imports |
| `architecture` | Architecture | Padrões core, dependency direction |
| `testing` | Testing | TDD, TPP, coverage strategies, test anti-patterns |
| `security` | Security | OWASP fundamentals, crypto, pentest readiness |
| `compliance` | Governance | Retention, auditing, changelogs |
| `api-design` | API | REST/GraphQL/gRPC conventions |
| `observability` | Ops | Metrics, logs, traces, SLOs |
| `resilience` | Ops | Circuit breakers, retries, timeouts |
| `infrastructure` | DevOps | K8s, Docker, networking, load balancing |
| `protocols` | Process | Branching, release, hotfix |
| `story-planning` | Process | Story structure, AC format, DoR |
| `ci-cd-patterns` | DevOps | Pipelines, stages, artifacts |
| `sre-practices` | Ops | Error budgets, on-call, runbooks |
| `release-management` | Release | SemVer, changelog, rollback |
| `data-management` | Data | Retention, migrations, privacy |
| `performance-engineering` | Performance | Profiling, hotspots, tuning |
| `feature-flags` | Release | Flag lifecycle, safe rollouts |

**Packs condicionais (exemplos habilitados por stack/profile):**

| KP | Ativação |
| :--- | :--- |
| `layer-templates` | Projetos com arquitetura hexagonal |
| `database-patterns`, `data-modeling` | Projetos com banco de dados |
| `pci-dss-requirements` | Perfil PCI |
| `owasp-asvs` | Perfil security/ASVS |
| `dockerfile`, `k8s-deployment`, `k8s-kustomize`, `k8s-helm` | `InfraConfig` com container/k8s |
| `iac-terraform`, `container-registry` | Infra IaC/registry habilitados |

### Apêndice E — Inventário de Agentes (10)

| Agent | Recommended Model | Role |
| :--- | :--- | :--- |
| `architect` | **Opus** | Deep design reasoning |
| `tech-lead` | Sonnet | 45-point holistic review |
| `qa-engineer` | Sonnet | Test coverage + quality |
| `security-engineer` | Sonnet | OWASP / STRIDE / PCI |
| `devops-engineer` | Sonnet | CI/CD, containers |
| `sre-engineer` | Sonnet | Health, observability |
| `performance-engineer` | Sonnet | N+1, pools, JVM |
| `devsecops-engineer` | Sonnet | SDLC security, supply chain |
| `java-developer` | Sonnet | Implementation (Senior Java) |
| `product-owner` | Sonnet | DoR validation, AC |

### Apêndice F — Inventário de Templates

Emitidos em `.claude/templates/` pelo `PlanTemplatesAssembler` (RULE-003). **12 templates principais** cobrindo o workflow de planejamento → review → release (listados na [seção 2.3](#23-templates-de-planejamento-e-review)). Projetos podem estender via `java/src/main/resources/shared/templates/` se necessário; o inventário acima reflete a base garantida.

### Apêndice G — Glossário

| Termo | Definição |
| :--- | :--- |
| **Absolute gate** | Coverage threshold que **não** tolera pre-existing deficits (Rule 05). |
| **Batch A / Batch B** | Convenção de paralelismo (Rule 13): Batch A = `TaskCreate` + `Agent(...)` siblings; Batch B = `TaskUpdate` siblings quando resultados retornam. |
| **CI-Watch** | Polling de PR CI + Copilot review status com 8 exit codes (EPIC-0045, EPIC-0057). |
| **Creator-owned removal** | Worktrees são removidas apenas pela skill que as criou. |
| **Double-Loop TDD** | Outer loop (acceptance) + inner loop (unit Red-Green-Refactor). |
| **Epic branch** | `epic/XXXX` — integração única por épico (Rule 21). |
| **Evidence file** | Artefato persistente que prova execução de uma sub-skill (Rule 24). |
| **`flowVersion`** | Discriminador em `execution-state.json`: `"1"` legacy / `"2"` novo (Rule 19). |
| **Hotspot** | Arquivo com alto risco de colisão paralela (EPIC-0041). |
| **INLINE-SKILL** | Pattern 1 de invocação skill-to-skill (Rule 13). |
| **Interactive gate** | Menu PROCEED/FIX-PR/ABORT em skills orquestradoras (Rule 20, ADR-0010). |
| **Knowledge pack (KP)** | Material de referência não invocável, consumido como contexto. |
| **Manual gate** | Merge `epic/XXXX → develop` requer revisão humana explícita. |
| **NDJSON** | Newline-delimited JSON — formato de `events.ndjson`. |
| **Opus / Sonnet / Haiku** | Tiers de modelo da família Claude 4.X (Rule 23). |
| **Phase marker** | `phase.start` / `phase.end` em skills (Rule 13 Telemetry). |
| **RA9** | Rule-Aligned 9-section template (EPIC-0056, roadmap). |
| **SUBAGENT-GENERAL** | Pattern 2 de invocação (Rule 13) — subagent isolado. |
| **Task-first (v2)** | Schema de planejamento com task como arquivo primário (EPIC-0038). |
| **TDD tag** | `[TDD:RED]` / `[TDD:GREEN]` / `[TDD:REFACTOR]` em commit message. |
| **TPP** | Transformation Priority Premise — ordem de complexidade dos testes. |
| **Worktree** | Git worktree sob `.claude/worktrees/{id}/` (Rule 14). |

---

## 12. Referências Cruzadas

- **Rules canônicas:** `.claude/rules/01-project-identity.md` … `24-execution-integrity.md`
- **CLAUDE.md raiz:** blocos `> **Concluded**` / `> **In progress**` para status de épicos
- **ADRs relevantes:**
  - `adr/ADR-0003-skill-taxonomy-and-naming.md` — refatoração de taxonomia (EPIC-0036)
  - `adr/ADR-0004-worktree-lifecycle.md` — lifecycle de worktrees
  - `adr/ADR-0005-telemetry-architecture.md` — arquitetura de telemetria
  - `adr/ADR-0006-file-conflict-aware-parallelism.md` — collisão em paralelo (EPIC-0041)
  - `adr/ADR-0010-interactive-gates-convention.md` — gates interativos (EPIC-0043)
  - `adr/ADR-0012-*` — rollout slim (EPIC-0054)
- **Planos de épicos em evolução:** `plans/epic-0055/epic-0055.md`, `plans/epic-0056/epic-0056.md`, `plans/epic-0057/epic-0057.md`
- **Especificações:** `plans/epic-0055/spec-task-granularity-phase-gates.md`, `plans/epic-0056/spec-ra9-standardized-planning-templates.md`

---

> **Como manter este documento.** Ao concluir um épico que altere convenções (nova rule, novo campo em `execution-state.json`, mudança em pattern de skill), atualize a seção correspondente e o apêndice relevante. Para mudanças estruturais grandes, crie um PR dedicado referenciando o épico. Este documento **não** é source of truth — é o mapa consolidado; a fonte canônica são as rules em `.claude/rules/` e os artefatos gerados em `.claude/`.
