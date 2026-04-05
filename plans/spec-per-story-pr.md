# Especificacao: PR por Story no x-dev-epic-implement

## Visao Geral

O skill `/x-dev-epic-implement` orquestra a implementacao de epicos completos, executando stories sequencialmente ou em paralelo via worktrees. Atualmente, todas as stories sao mergeadas em uma unica branch epica (`feat/epic-{epicId}-full-implementation`) e ao final, um unico Pull Request eh criado contendo todas as mudancas de todas as stories.

## Problema

Para epicos com muitas stories (10, 15, 30), o PR resultante eh impossivel de revisar por humanos. A quantidade de mudancas, arquivos e contextos diferentes tornam a revisao inviavel. Isso contradiz boas praticas de code review e reduz a qualidade das revisoes.

## Objetivo

Refatorar o skill `x-dev-epic-implement` para que **cada story gere seu proprio PR** targeting `main`, com ciclo completo de review (especialistas + tech lead) executado por story. A branch epica eh eliminada — cada story cria sua propria branch via `x-dev-lifecycle`.

## Componentes do Sistema

### 1. x-dev-epic-implement/SKILL.md (Orquestrador de Epico)

Arquivo principal (~1345 linhas) que contem toda a logica de orquestracao. Localizado em `.claude/skills/x-dev-epic-implement/SKILL.md`.

**Secoes relevantes:**
- Phase 0 (Preparation): Steps 1-10, inclui criacao da branch epica (Step 7)
- Phase 0.5 (Pre-flight Conflict Analysis): Sections 0.5.1-0.5.5, detecta overlaps e particiona em paralelo/sequencial
- Phase 1 (Execution Loop): Sections 1.1-1.8
  - 1.1: Initialize Execution State (schema do execution-state.json)
  - 1.2: Branch Management (cria branch epica)
  - 1.3: Core Loop Algorithm (getExecutableStories, dispatch loop)
  - 1.4: Subagent Dispatch Sequential (prompt template com SubagentResult)
  - 1.4a: Parallel Worktree Dispatch (default, lanca worktrees)
  - 1.4b: Merge Strategy Rebase-Before-Merge (merge worktrees na branch epica)
  - 1.4c: Conflict Resolution Subagent (resolve conflitos de merge/rebase)
  - 1.5: Result Validation e Failure Handling
  - 1.6: Checkpoint Update
  - 1.7: Integrity Gate (roda entre fases na branch epica)
  - 1.8: Cross-Story Consistency Gate
- Phase 2 (Consolidation):
  - 2.1: Tech Lead Review (review do diff total do epico)
  - 2.2: Report Generation
  - 2.3: PR Creation (cria 1 unico PR para todo o epico)
- Phase 3 (Verification): Sections 3.1-3.4

### 2. x-dev-lifecycle/SKILL.md (Orquestrador de Story)

Arquivo (~742 linhas) que implementa o ciclo de vida de cada story. Localizado em `.claude/skills/x-dev-lifecycle/SKILL.md`.

**Fases relevantes:**
- Phase 0: Branch & Setup (cria branch da story)
- Phase 4: Specialist Review (via /x-review)
- Phase 6: Commit & PR (cria PR da story targeting main)
- Phase 7: Tech Lead Review (via /x-review-pr)
- Phase 8: Final Verification & DoD Gate

### 3. execution-state.json (Schema de Estado)

Schema atual com campos por story: `id`, `phase`, `status`, `commitSha`, `retries`, `summary`, `duration`, `findingsCount`, `blockedBy`.

Status possiveis: PENDING, IN_PROGRESS, SUCCESS, FAILED, BLOCKED, PARTIAL, REBASING, REBASE_SUCCESS, REBASE_FAILED.

## Mudancas Necessarias

### MUD-01: Eliminar Branch Epica

**O que:** Remover a criacao e uso da branch `feat/epic-{epicId}-full-implementation`.

**Onde:**
- Phase 0, Step 7: remover `git checkout -b feat/epic-{epicId}-implementation`
- Section 1.2 (Branch Management): remover criacao da branch epica, documentar que branching eh delegado ao x-dev-lifecycle
- Section 1.4a: mudar branch naming de `feat/epic-{epicId}-{storyId}` para o padrao story `feat/{storyId}-description`
- Resume Step 3 (Branch Recovery): remover recovery da branch epica

**Impacto:** ~200 linhas removidas/modificadas

### MUD-02: Remover Rebase-Before-Merge e Conflict Resolution

**O que:** Remover toda a logica de merge de worktrees na branch epica.

**Onde:**
- Section 1.4b (Rebase-Before-Merge): remover completamente
- Section 1.4c (Conflict Resolution Subagent): remover completamente
- Status REBASING, REBASE_SUCCESS, REBASE_FAILED: remover do schema

**Impacto:** ~250 linhas removidas

### MUD-03: Delegar PR ao x-dev-lifecycle

**O que:** Permitir que o x-dev-lifecycle execute seu ciclo completo (9 fases) incluindo criacao de PR (Phase 6) e review (Phases 4, 7).

**Onde:**
- Section 1.4 (Sequential Dispatch): atualizar prompt template para NAO suprimir Phase 6/7
- Section 1.4a (Parallel Dispatch): mesmo update no prompt
- SubagentResult: adicionar campos `prUrl` e `prNumber`
- x-dev-lifecycle Phase 6: adicionar referencia ao epico no PR body ("Part of EPIC-{epicId}")

**Impacto:** ~80 linhas modificadas

### MUD-04: Enforcement de Dependencias via PR Merge Status

**O que:** `getExecutableStories()` deve verificar que o PR da dependencia foi merged (nao apenas status SUCCESS no checkpoint).

**Onde:**
- Section 1.3 (Core Loop): adicionar verificacao `gh pr view {prNumber} --json state`
- Novo mecanismo de polling/wait para PRs pendentes de review
- Nova flag `--auto-merge`: merge automatico apos reviews passarem
- Section 1.1: adicionar `prUrl`, `prNumber`, `prMergeStatus` ao schema do execution-state.json

**Impacto:** ~150 linhas adicionadas

### MUD-05: Pre-flight Analysis em Modo Advisory

**O que:** A analise de conflitos nao bloqueia mais execucao paralela — apenas emite warnings.

**Onde:**
- Section 0.5.4: output vira advisory (warnings em vez de particionamento)
- Section 0.5.5: remover logica de particionamento parallel/sequential baseada em overlap

**Impacto:** ~40 linhas modificadas

### MUD-06: Integrity/Consistency Gates na Main

**O que:** Gates rodam na `main` apos merge dos PRs de cada fase, em vez de na branch epica.

**Onde:**
- Section 1.7 (Integrity Gate): checkout main, rodar validacoes na main atualizada
- Section 1.8 (Consistency Gate): diff main antes/depois da fase
- Capturar SHA da main antes de cada fase para calcular diff

**Impacto:** ~60 linhas modificadas

### MUD-07: Resume Workflow para Modelo Per-Story PR

**O que:** Adaptar o resume para lidar com estados de PR.

**Onde:**
- Resume Step 1: novos status `PR_CREATED`, `PR_PENDING_REVIEW`, `PR_MERGED`
- Resume Step 3: remover recovery da branch epica
- Failure handling: fechar PR se story falha (`gh pr close {prNumber}`)

**Impacto:** ~80 linhas modificadas

### MUD-08: Substituir Phase 2 Consolidada por Tracking Incremental

**O que:** Em vez de criar 1 mega-PR na Phase 2, gerar relatorio de progresso com links de todos os PRs.

**Onde:**
- Remover Section 2.1 (Tech Lead Review do epico inteiro)
- Remover Section 2.3 (PR Creation)
- Reescrever Phase 2 como "Epic Progress Report Generation"
- Template: `{{PR_LINK}}` vira `{{PR_LINKS_TABLE}}` com tabela per-story
- Section 3.1-3.4: atualizar verificacao para modelo per-story

**Impacto:** ~250 linhas removidas, ~80 adicionadas

## Regras de Negocio

### RULE-001: Isolamento de Contexto de Subagents
Cada subagent recebe apenas metadados (story ID, branch, phase, flags). Nunca passar source code, knowledge packs ou diffs.

### RULE-002: Persistencia Atomica de Checkpoint
O checkpoint eh atualizado apos cada transicao de estado. Se o orquestrador crashar, o checkpoint reflete o ultimo estado completo.

### RULE-003: Ordem de Dependencias
Stories so podem ser executadas quando TODAS as dependencias estao com PR merged na main.

### RULE-004: Auto-merge Opcional
A flag `--auto-merge` permite merge automatico apos reviews aprovarem. Sem a flag, o orquestrador aguarda merge manual.

### RULE-005: Pre-flight eh Advisory
A analise de conflitos (Phase 0.5) emite warnings mas NAO bloqueia execucao paralela.

### RULE-006: Integridade na Main
Os integrity gates rodam na `main` apos merge dos PRs de cada fase, verificando integracao cross-story.

### RULE-007: Prioridade por Caminho Critico
Stories no caminho critico tem prioridade na execucao e no merge automatico.

### RULE-008: Rastreabilidade Epic-PR
Cada PR de story inclui referencia ao epico no body ("Part of EPIC-{epicId}") para rastreabilidade.

### RULE-009: Backward Compatibility
A flag `--single-pr` (nova, opcional) permite manter o comportamento legacy de 1 unico PR para todo o epico. Default eh per-story PR.

### RULE-010: Report Incremental
O relatorio de execucao do epico eh atualizado incrementalmente conforme cada story completa, nao apenas ao final.

## Fluxo Principal

```
1. Parse arguments e validacoes (Phase 0)
2. Pre-flight analysis advisory (Phase 0.5)
3. Para cada fase do implementation map:
   a. getExecutableStories() — verifica dependencias + PR merge status
   b. Dispatch stories (paralelo ou sequencial) via x-dev-lifecycle
   c. x-dev-lifecycle executa 9 fases completas incluindo PR e review
   d. Orchestrador coleta prUrl/prNumber do resultado
   e. (Se --auto-merge) Merge PR automaticamente
   f. (Se nao --auto-merge) Aguarda merge manual com polling
   g. Atualiza checkpoint com status do PR
   h. Integrity gate na main apos todos PRs da fase mergeados
4. Gera relatorio final com tabela de PRs (Phase 2)
5. Verificacao final na main (Phase 3)
```

## Restricoes

- Todos os artefatos gerados devem ser em portugues brasileiro (pt-BR)
- Termos tecnicos em ingles permanecem em ingles
- IDs seguem o padrao: epic-XXXX, story-XXXX-YYYY, RULE-NNN
- As mudancas sao exclusivamente nos SKILL.md files (nao ha codigo de producao)
- O projeto eh `ia-dev-environment` — uma meta-ferramenta que gera configuracoes de dev environment
