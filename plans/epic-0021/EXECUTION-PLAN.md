# Plano de Execucao — EPIC-0021 (Reimplementacao)

## Contexto

O EPIC-0021 refatora o skill `x-dev-epic-implement` de um modelo "single mega-PR"
para um modelo "per-story PR". Cada story implementada pelo orquestrador passa pelo
ciclo completo do `x-dev-lifecycle` (incluindo reviews de especialistas + tech lead)
e gera seu proprio PR targeting `main`.

A implementacao anterior (PR #138) foi revertida acidentalmente pelo commit `1ca021f3`.
A restauracao via `ba34fa71` nao gerou confianca. Esta e uma reimplementacao do zero.

## Pre-requisitos (JA CONCLUIDOS)

- [x] Branch: `feat/epic-0021-full-implementation` criada a partir de `main`
- [x] SKILL.md revertido para versao pre-EPIC-0021 (commit `2c3f4634`, 1,093 linhas)
- [x] README.md removido (sera criado pelo story-0021-0008)
- [x] Todas as 9 stories com status "Pendente"
- [x] IMPLEMENTATION-MAP.md com todos os statuses "Pendente"
- [x] Sem execution-state.json (clean slate)
- [x] Sem diretorio plans/epic-0021/plans/ (sera criado durante execucao)
- [x] Commit baseline: `8923a237`

## Comando de Execucao

Na nova janela de contexto, executar:

```
git checkout feat/epic-0021-full-implementation
/x-dev-epic-implement 0021 --sequential --skip-smoke-gate
```

### Flags explicadas

| Flag | Justificativa |
|------|---------------|
| `--sequential` | Todas as stories modificam o mesmo arquivo (SKILL.md). Parallelismo causaria conflitos. |
| `--skip-smoke-gate` | Mudancas sao em markdown (SKILL.md), nao ha testes ou compilacao. Smoke gate falharia. |

### Flags NAO usadas

| Flag | Motivo |
|------|--------|
| `--skip-review` | **NAO USAR** — queremos reviews para validar cada story |
| `--dry-run` | NAO USAR — queremos execucao real |
| `--single-pr` | NAO USAR — esse e o modelo antigo que estamos substituindo |

## Estrutura do Epico

### 4 Fases, 9 Stories

```
FASE 0 — Fundacao (2 stories, sequenciais por --sequential)
  story-0021-0001: Eliminar branch epica e adotar branching por story
  story-0021-0002: Delegar criacao de PR e review ao x-dev-lifecycle

FASE 1 — Core (2 stories)
  story-0021-0003: Enforcement de dependencias via PR merge status
  story-0021-0004: Substituir Phase 2 consolidada por tracking incremental

FASE 2 — Extensoes (4 stories)
  story-0021-0005: Pre-flight analysis em modo advisory
  story-0021-0006: Integrity e consistency gates na main
  story-0021-0007: Resume workflow para modelo per-story PR
  story-0021-0009: Auto-rebase e resolucao automatica de conflitos

FASE 3 — Finalizacao (1 story)
  story-0021-0008: Verificacao final e documentacao de integracao
```

### Caminho Critico

```
story-0021-0001 → story-0021-0003 → story-0021-0006 → story-0021-0008
                                   → story-0021-0009 → story-0021-0008
```

## O que cada story modifica no SKILL.md

### story-0021-0001 — Eliminar branch epica
- Phase 0 Step 7: Remover criacao de branch epica, add guard `--single-pr`
- Section 1.2: Reescrever — orquestrador NAO cria branch
- Section 1.4a: Atualizar branch naming para `feat/{storyId}-*`
- Section 1.4b: **REMOVER INTEIRAMENTE** (~80 linhas rebase-before-merge)
- Section 1.4c: Substituir por placeholder para story-0009
- Resume Step 3: Remover branch recovery
- Remover status REBASING/REBASE_SUCCESS/REBASE_FAILED do schema
- Add flag `--single-pr` na tabela de flags e argument-hint

### story-0021-0002 — Delegar PR ao lifecycle
- Section 1.4 prompt: Adicionar step 6 "Create PR and run reviews (Phases 4-8)"
- Section 1.4 prompt: PR MUST target `main`, incluir "Part of EPIC-{epicId}"
- SubagentResult: Adicionar prUrl, prNumber, reviewsExecuted, reviewScores, coverageLine, coverageBranch, tddCycles
- Section 1.5: Validar prUrl/prNumber quando status=SUCCESS

### story-0021-0003 — Dependency enforcement
- Section 1.3: `getExecutableStories()` verifica `prMergeStatus == "MERGED"`
- Add mecanismo PR Merge Wait (auto-merge + polling)
- Add flag `--auto-merge`
- Add campos per-story: prUrl, prNumber, prMergeStatus

### story-0021-0004 — Phase 2 incremental
- Section 2.1: **REMOVER** (Tech Lead Review do diff completo)
- Section 2.3: **REMOVER** (criacao de mega-PR)
- Phase 2: Reescrever como "Epic Progress Report Generation"
- Template: `{{PR_LINKS_TABLE}}` no lugar de `{{PR_LINK}}`

### story-0021-0005 — Pre-flight advisory
- Section 0.5.4: Dual-mode (advisory default, strict opt-in com `--strict-overlap`)
- Section 0.5.5: Core loop ignora partitioning no modo default
- Add flag `--strict-overlap`

### story-0021-0006 — Integrity gates na main
- Integrity Gate section: Rodar na main apos PRs merged
- Capturar `mainShaBeforePhase` no inicio de cada fase
- Add `mainShaBeforePhase` ao checkpoint schema

### story-0021-0007 — Resume workflow
- Resume Step 1: Add transicoes para PR_CREATED, PR_PENDING_REVIEW, PR_MERGED
- Failure handling: Fechar PR de story falhada via `gh pr close`
- Resume verifica status de PR via `gh pr view`

### story-0021-0009 — Auto-rebase e conflict resolution
- Section 1.4c: **REIMPLEMENTAR** (substituir placeholder de story-0001)
- Section 1.4e: **NOVA** — Auto-rebase trigger apos cada PR merge
- Add campos: rebaseStatus, lastRebaseSha, rebaseAttempts
- MAX_REBASE_RETRIES = 3

### story-0021-0008 — Verificacao final
- Phase 3: Atualizar para modelo per-story PR
- Phase 0 Step 8: Atualizar output do `--dry-run`
- Frontmatter: Atualizar argument-hint com todas as flags novas
- Criar README.md com fluxogramas Mermaid
- Consistencia: zero referencias orfas fora do guard `--single-pr`

## Validacao Pos-Execucao

Apos todas as stories completarem, verificar:

```bash
# 1. Verificar que todas as features per-story PR existem
grep -c "prUrl\|prNumber\|--single-pr\|--auto-merge\|--strict-overlap" \
  .claude/skills/x-dev-epic-implement/SKILL.md
# Esperado: >= 50 ocorrencias

# 2. Verificar que Section 1.4b NAO existe
grep -c "1\.4b" .claude/skills/x-dev-epic-implement/SKILL.md
# Esperado: 0

# 3. Verificar que Section 1.4e existe
grep -c "1\.4e" .claude/skills/x-dev-epic-implement/SKILL.md
# Esperado: >= 5

# 4. Verificar que epic branch so aparece no guard --single-pr
grep -n "full-implementation" .claude/skills/x-dev-epic-implement/SKILL.md
# Esperado: apenas dentro do guard --single-pr

# 5. Verificar que Phase 2 e "Progress Report" (nao "Consolidation")
grep "Phase 2" .claude/skills/x-dev-epic-implement/SKILL.md
# Esperado: "Epic Progress Report Generation"

# 6. Verificar que prompt tem step 6
grep "Create PR and run reviews" .claude/skills/x-dev-epic-implement/SKILL.md
# Esperado: presente

# 7. Verificar todas as stories concluidas
grep "Concluída" plans/epic-0021/IMPLEMENTATION-MAP.md | wc -l
# Esperado: 9
```

## Arquivos Afetados

| Arquivo | Acao |
|---------|------|
| `.claude/skills/x-dev-epic-implement/SKILL.md` | Modificado por todas as 9 stories |
| `.claude/skills/x-dev-epic-implement/README.md` | Removido (documentacao incorporada no SKILL.md) |
| `plans/epic-0021/execution-state.json` | Criado pelo orquestrador |
| `plans/epic-0021/IMPLEMENTATION-MAP.md` | Statuses atualizados |
| `plans/epic-0021/story-0021-*.md` | Statuses atualizados |
| `plans/epic-0021/plans/` | Planos de implementacao por story |

## Nota sobre TDD

As mudancas deste epico sao em arquivos SKILL.md (markdown), nao codigo de producao.
O "TDD" neste contexto e a validacao de consistencia interna do SKILL.md:
- Secoes referenciadas existem
- Schemas sao consistentes
- Exemplos refletem o novo modelo
- Nao ha referencias orfas ao modelo antigo fora do guard `--single-pr`

## Nota sobre Flags do Skill

O `/x-dev-epic-implement` com `--sequential` executa stories uma por uma (nao em paralelo).
Isso e necessario porque todas as stories modificam o mesmo arquivo (SKILL.md) e o paralelismo
causaria conflitos de merge inevitaveis.

O `--skip-smoke-gate` pula os smoke tests no integrity gate entre fases. Isso e necessario
porque as mudancas sao em markdown e nao ha smoke tests aplicaveis.
