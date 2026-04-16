# Story Planning Report — story-0046-0003

## Header

| Field | Value |
|-------|-------|
| Story ID | story-0046-0003 |
| Epic ID | 0046 |
| Date | 2026-04-16 |
| Agents Participating | Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner |

## Planning Summary

Retrofita `x-task-implement` com Phase 3.5 (status transition) que atualiza `**Status:** → Concluída` no task file + coluna Status do `task-implementation-map-STORY-*.md` no commit atômico final da task. Preserva Rule 18 (1 commit por task). Suporta Rule 15 COALESCED (ambos partners atualizados no mesmo commit compartilhado).

## Architecture Assessment

**Layers afetadas:** Application (`TaskMapRowUpdater`) + Adapter (CLI) + Doc (`x-task-implement/SKILL.md`).

**Novos componentes:**
- `dev.iadev.application.lifecycle.TaskMapRowUpdater` — regex substitui coluna Status de uma row específica de `task-implementation-map-STORY-*.md` identificada por TASK-ID.
- `dev.iadev.adapter.inbound.cli.TaskMapRowUpdaterCli` — exit codes alinhados.

**Ponto de inserção em `x-task-implement`:** entre Phase 3 (outputs verify, Rule 16) e Phase 4 (atomic commit, Rule 18). A nova Phase 3.5 stageia status + map row; Phase 4 commita tudo atomicamente.

**Coalesced pairs:** `x-task-implement` Phase 0e já detecta COALESCED partners (Rule 15). Phase 3.5 herda: atualiza ambos task files + ambas rows do map ANTES do commit compartilhado (Rule 18 Coalesced Exception).

## Test Strategy Summary

**Outer loop:** `TaskImplementStatusSmokeTest` — task toy v2 em sandbox; roda x-task-implement; assert 1 commit atômico contém TDD + task file Status=Concluída + map row.

**Inner loop TPP:**
- L1: `updateRow_whenTaskIdNotFound_throws`
- L2: `updateRow_happyPath_replacesStatus`
- L3: `updateRow_whenAlreadyConcluida_idempotent`
- L4: `updateRow_parametrized_fullMatrix` (Pendente→Concluída, Em Andamento→Concluída, etc.)
- L5: `updateRow_preservesOtherColumns` (boundary)
- L6: `updateRow_multipleRowsInMap_onlyTargetModified`

**Coverage:** ≥ 95% em `TaskMapRowUpdater`.

## Security Assessment Summary

Nenhuma nova surface. Reusa `StatusFieldParser` (já com path canonicalization). `TaskMapRowUpdater` opera em arquivos cujo path vem do próprio épico — path traversal implausível, mas canonicalize por defesa em profundidade.

## Implementation Approach

**Ponto-chave Rule 18:** status update NÃO cria commit próprio — é stageado JUNTO com os artefatos TDD e entra no único commit de conclusão da task. Enforcement: o audit de story 0046-0007 conta commits por task e assert = 1.

**COALESCED handling:** detectado via Phase 0e existente; Phase 3.5 apenas itera sobre ambos task files ao invés de um.

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 5 |
| Architecture tasks | 3 |
| Test tasks | 2 integration + unit embedded |
| Security tasks | 1 augmented |
| Quality gate tasks | 1 embedded (Rule 18 audit) |
| Merged tasks | 3 |

## Consolidated Risk Matrix

| Risk | Source Agent | Severity | Likelihood | Mitigation |
|------|------------|----------|------------|------------|
| Rule 18 violated (2 commits per task) | Tech Lead | HIGH | LOW | Test `TaskAtomicCommitAuditTest` explícito; review de SKILL.md diff |
| COALESCED pair desalinhado (1 partner atualizado, outro não) | Tech Lead | HIGH | MEDIUM | Test `CoalescedTaskStatusTest` cobre; fail-loud se 1 file ausente |
| task-implementation-map row regex quebra em formatação exótica | QA | MEDIUM | LOW | Testes com tabs, múltiplos espaços, coluna vazia |

## DoR Status

**READY** — ver `dor-story-0046-0003.md`.
