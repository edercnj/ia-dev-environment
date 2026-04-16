# Story Planning Report — story-0046-0004

## Header

| Field | Value |
|-------|-------|
| Story ID | story-0046-0004 |
| Epic ID | 0046 |
| Date | 2026-04-16 |
| Agents | Architect, QA, Security, Tech Lead, PO |

## Planning Summary

Marco arquitetural do épico. Promove `x-story-implement` Phase 3 a obrigatória no caminho happy (`--skip-verification` documentada como "recovery only") e cabea `x-epic-implement` Section 1.6b ao Core Loop como Phase 1.7 + introduz Phase 5 (epic finalize). Consumindo helpers das stories 0001 e 0003 + novo `EpicMapRowUpdater`.

## Architecture Assessment

**Componentes novos:**
- `dev.iadev.application.lifecycle.EpicMapRowUpdater` — análogo a `TaskMapRowUpdater` da story 0003 mas para `implementation-map-XXXX.md` (epic-level, não task-level; regex e estrutura da tabela diferentes).
- CLI wrapper correspondente.

**Retrofits:**
- `x-story-implement/SKILL.md` Phase 3 steps 3.8.1-3.8.5 explícitos e não-skippable no happy path.
- `x-epic-implement/SKILL.md` Core Loop Phase 1.6 → Phase 1.7 (status sync por story pós-wave) → próxima wave; pós-último-wave: Phase 5 (epic-XXXX.md Status → Concluído).

**V2-gating:** Phase 3 e Phase 1.7/5 são V2-only. Épicos v1 rodam fluxo legacy.

## Test Strategy Summary

**Outer loop:** `EpicImplementFinalizeSmokeTest` — épico toy v2 com 2 stories; roda `x-epic-implement`; assert ambas stories + epic têm Status final + 3 commits visíveis.

**Inner loop:**
- L1 nil: `epicMapRow_whenMalformed_throws`
- L2 const: `epicMapRow_happyPath`
- L3 scalar: `phase5_whenAllStoriesConcluida_updatesEpicStatus`
- L4 collection: `phase17_forEachStoryInWave_updatesStatus`
- L5 cond: `epicImplement_whenV1_skipsPhase17And5`
- L6 bound: `epicImplement_idempotent_whenRereuninAlreadyConcluido`

## Security Assessment Summary

OWASP A08 (Software and Data Integrity): commit message de epic-finalize NÃO deve incluir conteúdo do state.json verbatim (informação operacional sensível). Sanitization: só o summary (story count, sha count).

## Implementation Approach

**Cabear Section 1.6b como Phase 1.7:** 
1. No `x-epic-implement` Core Loop pseudo-code, substituir "call 1.6 checkpoint" por "call 1.6 checkpoint; call 1.7 status sync".
2. Renomear heading "1.6b — Markdown Status Sync" para "1.7 — Markdown Status Sync (per-story pós-wave)".
3. Referências explícitas a "Phase 1.7" em tabelas/listas de phases.

**Phase 5 nova:** adicionada como seção "5. Epic Finalization" no SKILL.md, com steps:
- Read epic-XXXX.md Status
- Validate transition Em Refinamento → Concluído
- Write via StatusFieldParserCli
- Update implementation-map-XXXX.md coluna Status (todas as rows já devem estar Concluída via Phase 1.7 — validação)
- Stage + x-git-commit `chore(epic-XXXX): finalize Status to Concluído`

**Remover --skip-verification do happy path:** a flag permanece aceita mas produz `WARN: --skip-verification is recovery only; status sync will be skipped. Rule 046-04 prohibits use in CI/automation` + registra no execution-state.json.

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 4 |
| Architecture | 3 |
| Test | 4 embedded |
| Security | 1 augmented |
| Merged | 3 |

## Consolidated Risk Matrix

| Risk | Severity | Likelihood | Mitigation |
|------|----------|------------|------------|
| Phase 1.7 wiring incorreto (chamado antes de commit de story) → ordem inversa | HIGH | MEDIUM | Smoke E2E com git log ordering assertivo |
| Phase 5 roda antes de todas stories estarem Concluída → estado inconsistente | HIGH | LOW | Pre-check em Phase 5: assert todas rows do map = Concluída |
| --skip-verification usado inadvertidamente por usuário | MEDIUM | MEDIUM | WARN loud + entrada em execution-state.json + audit da story 0007 detecta uso em CI |

## DoR Status

**READY** — ver `dor-story-0046-0004.md`.
