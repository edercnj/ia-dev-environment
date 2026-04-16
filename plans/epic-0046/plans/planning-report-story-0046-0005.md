# Story Planning Report — story-0046-0005

| Field | Value |
|-------|-------|
| Story ID | story-0046-0005 |
| Epic ID | 0046 |
| Date | 2026-04-16 |
| Agents | Architect, QA, Security, Tech Lead, PO |

## Planning Summary

Adiciona commits atômicos após escritas em `plans/epic-XXXX/reports/` no `x-epic-implement`: (1) `execution-plan-epic-XXXX.md` pré-wave-loop e (2) `phase-report-epic-XXXX-waveN.md` pós-cada-wave. Elimina o falso-positivo `VALIDATE_DIRTY_WORKDIR` no `x-release` pós-epic-implement. V2-gated. Requer Phase 1.7 da story 0004 para posicionar corretamente o commit de phase-report.

## Architecture Assessment

**Componentes novos:**
- `dev.iadev.application.lifecycle.ReportCommitMessageBuilder` — helper que formata mensagens conventional-commits consistentes (`docs(epic-XXXX): add <type> report`).

**Retrofits em `x-epic-implement/SKILL.md`:**
- **Pré-wave-loop:** após gerar `execution-plan-epic-XXXX.md`, invocar `Skill(skill: "x-git-commit", args: "docs(epic-XXXX): add execution plan")`.
- **Pós-cada-wave:** após Phase 1.7 (da story 0004), gerar `phase-report-epic-XXXX-waveN.md` + `Skill(skill: "x-git-commit", args: "docs(epic-XXXX): add phase-N report")`.

**Interação com story 0004:** a ordem canônica pós-retrofit é:

```
Wave N:
  dispatch stories → await DONE
  Phase 1.6: update execution-state.json
  Phase 1.7 (story 0004): update story Status + map row → commit docs(story-*)
  Phase 1.8 (esta story): write phase-report + commit docs(epic-*)
  Advance to Wave N+1
```

## Test Strategy Summary

**Outer loop E2E:** `EpicImplementReleaseCompatTest` — épico v2 completo → `x-release --dry-run` → VALIDATE_DIRTY_WORKDIR passa.

**Inner loop TPP:**
- L1: `buildMessage_whenEpicIdNull_throws`
- L2: `buildMessage_executionPlan_returnsCanonical`
- L3: `buildMessage_phaseReport_includesWaveNumber`
- L4: `buildMessage_allTypes_parametrized`
- L5: `buildMessage_sanitizesPathsInBody` (SEC)
- L6: `buildMessage_longBody_truncated` (boundary)

## Security Assessment Summary

**A08 (Integrity):** commit message NÃO deve incluir:
- Paths absolutos do sistema (privacy)
- Bytes raw do `execution-state.json` (pode conter SHA256, URLs privadas)
- Nomes de usuários internos

**Augmentation em TASK-001:** `ReportCommitMessageBuilder.sanitize` strip absolute paths (converte para relative), trim body a 500 chars.

## Implementation Approach

V2-gating: ambos commits (execution-plan + phase-report) só emitidos em v2. v1 permanece legacy — reports podem ou não ser gerados, nunca commitados automaticamente (Rule 19).

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 5 |
| Architecture | 3 |
| Test | 5 embedded + 2 dedicated |
| Security | 1 augmented (TASK-001) |
| Merged | 4 |

## Consolidated Risk Matrix

| Risk | Severity | Likelihood | Mitigation |
|------|----------|------------|------------|
| Ordem errada: phase-report committed antes da Phase 1.7 → map row não atualizada no report | HIGH | MEDIUM | Smoke assertivo de ordem de commits; TASK-003 REQUIRES_MOCK documentado |
| Commit de report rejeitado por hook pre-commit (linting md?) | MEDIUM | LOW | Smoke cobre; fail-loud exit REPORT_COMMIT_FAILED com output do hook em stderr |
| VALIDATE_DIRTY_WORKDIR continua falhando por outros writes órfãos não cobertos | MEDIUM | LOW | Audit da story 0007 detecta outros writes sem commit |

## DoR Status

**READY** — ver `dor-story-0046-0005.md`.
