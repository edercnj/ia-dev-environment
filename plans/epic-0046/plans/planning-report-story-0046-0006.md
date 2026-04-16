# Story Planning Report — story-0046-0006

| Field | Value |
|-------|-------|
| Story ID | story-0046-0006 |
| Epic ID | 0046 |
| Date | 2026-04-16 |
| Agents | Architect, QA, Security, Tech Lead, PO |

## Planning Summary

Nova skill `x-status-reconcile` em `core/ops/` (opt-in) que compara `execution-state.json` vs markdowns + coluna Status do `implementation-map`. Dois modos: diagnose (default, read-only) + apply (com gate interativo). NUNCA modifica `state.json` (Rule 19 + RULE-046-07). Permite backfill manual de épicos legados como EPIC-0024.

## Architecture Assessment

**Taxonomia (ADR-0003):** skill em `core/ops/` — categoria correta para operações em estado do épico.

**Componentes novos:**
- `dev.iadev.application.lifecycle.LifecycleReconciler` — core da skill; `diff(Path) : List<Divergence>` + `apply(List<Divergence>) : CommitSha`.
- `dev.iadev.domain.lifecycle.Divergence` — record `(artifact, from, to)`.
- `dev.iadev.adapter.inbound.cli.StatusReconcileCli` — expõe CLI.
- `java/src/main/resources/targets/claude/skills/core/ops/x-status-reconcile/SKILL.md` — contrato Claude Code.

**Mapeamento state.json → LifecycleStatus (tabela canônica):**

| state.json | LifecycleStatus sugerido |
|------------|--------------------------|
| `SUCCESS` + `MERGED` | `Concluída` |
| `IN_PROGRESS` | `Em Andamento` |
| `PENDING` | `Pendente` |
| `FAILED` | `Falha` |
| outros | log WARN, skip |

## Test Strategy Summary

**Outer loop E2E:** `StatusReconcileApplySmokeTest` — cria épico "legado" toy com divergências; `--apply --non-interactive`; assert 1 commit + markdowns atualizados.

**Inner loop TPP:**
- L1: `diff_whenStateFileMissing_throws`
- L2: `diff_happy_returnsExpectedList`
- L3: `apply_whenV1_skipsWithMessage`
- L4: `diff_parametrized_allMappings`
- L5: `apply_whenSuspiciousTransition_abort40`
- L6: `apply_preservesStateJson_neverTouches` (boundary crítico)

**Coverage:** ≥ 95% em `LifecycleReconciler`.

## Security Assessment Summary

**A01 (Broken Access Control) + A08 (Integrity):**
- TASK-002 NÃO pode escrever em arquivos fora do epic dir (path canonicalization + startsWith(epicDir)).
- `state.json` NUNCA é modificado — enforcement via test que verifica mtime antes/depois.
- Commit message inclui apenas: epic-id, count de divergences, não paths absolutos.

**Gate interativo (via `AskUserQuestion`):** sem risco de mass-write sem confirmação. `--non-interactive` aceita a escolha implícita PROCEED — uso em CI/automação é supported mas registrado em JSON final.

## Implementation Approach

**Reuse EPIC-0043 gate pattern** se disponível:

```
Operador: /x-status-reconcile --epic 0024 --apply
LifecycleReconciler.diff() → 33 divergences
SKILL invoca AskUserQuestion(PROCEED/FIX/ABORT)
  PROCEED → reconciler.apply() → x-git-commit
  FIX → re-diagnose com detalhe por file
  ABORT → exit 50
```

**Fallback se EPIC-0043 não mergeado ainda:** `AskUserQuestion` inline.

**Rule 19 enforcement:** `reconciler.apply()` lê `planningSchemaVersion` via `SchemaVersionResolver`; se v1, retorna imediatamente com message "legacy epic; skipping sync per Rule 19" — aplica-se a todos os épicos 0025-0037 e qualquer pre-v2.

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total | 5 |
| ARCH | 3 |
| QA | 5 embedded |
| SEC | 1 (TASK-002 augmented) |
| Merged | 4 |

## Consolidated Risk Matrix

| Risk | Severity | Likelihood | Mitigation |
|------|----------|------------|------------|
| Apply modifica state.json por engano | CRITICAL | LOW | Test mtime invariant; SEC review; Rule 19 explicit |
| Path traversal via --epic `../../../etc/passwd` | HIGH | LOW | Canonicalize + startsWith check |
| Gate interativo hang em CI sem `--non-interactive` | MEDIUM | MEDIUM | Timeout de 60s + documentação explicita `--non-interactive` para CI |
| Transição suspeita legítima (reopening) bloqueada | LOW | MEDIUM | Documentação: operador edita markdown manualmente para forçar override |

## DoR Status

**READY** — ver `dor-story-0046-0006.md`.
