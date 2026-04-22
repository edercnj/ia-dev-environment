# Epic Execution Plan -- EPIC-0048

> **Epic ID:** EPIC-0048
> **Title:** Java-Only Generator + Correção de Bugs A (pastas vazias) e B (CLAUDE.md raiz)
> **Date:** 2026-04-22
> **Total Stories:** 13
> **Total Phases:** 9
> **Author:** Orchestrator (x-epic-implement)
> **Template Version:** 1.0

---

## Execution Strategy

| Attribute | Value |
|-----------|-------|
| Strategy | Sequential per-story PR (option C from session) |
| Max Parallelism | 1 (--sequential flag set) |
| Checkpoint Frequency | Per story completion |
| Dry Run | No |

Flags operativos desta execução:

- `--sequential` — todas as stories executadas serialmente (desliga Phase 0.5 conflict analysis)
- `--auto-merge` (default EPIC-0042) — PRs de story merge automático após CI/review approve
- Global DoR waived (feature branch `feature/epic-0048-java-only-generator` NÃO criada; tag `pre-epic-0048-java-only` NÃO criada). Orchestrator opera direto a partir de `develop` + cada story cria sua própria branch `feat/story-0048-YYYY-<desc>`.
- `planningSchemaVersion: "2.0"` declarado em `execution-state.json`.

---

## Phase Timeline

| Phase | Name | Stories | Parallelism | Estimated Duration | Dependencies |
|-------|------|---------|-------------|-------------------|--------------|
| 0 | Gate de Investigação | story-0048-0001 | 1 | ~8h | — |
| 1 | Decisões Arquiteturais (ADRs) | story-0048-0002 | 1 | ~6h | Phase 0 |
| 2 | Fundações (CLI + Template) | story-0048-0003, story-0048-0010 | 1 (forçado pelo `--sequential`) | ~8h | Phase 1 |
| 3 | Limpezas Paralelas | story-0048-0004, story-0048-0005, story-0048-0006 | 1 (forçado pelo `--sequential`) | ~18h | Phase 2 (story-0048-0003) |
| 4 | Deleção de Resources (HIGH RISK) | story-0048-0007 | 1 | ~10h | Phase 3 |
| 5 | Trim de Testes Parametrizados | story-0048-0008 | 1 | ~8h | Phase 4 |
| 6 | Bug Fixes | story-0048-0009, story-0048-0011 | 1 (recomendação operacional sequencial) | ~18h | Phase 5 + story-0048-0010 |
| 7 | E2E + Higienização | story-0048-0012 | 1 | ~6h | Phase 6 |
| 8 | Release v4.0.0 Prep | story-0048-0013 | 1 | ~5h | Phase 7 |

> **Total estimated duration:** ~85h serial (estimativa do IMPLEMENTATION-MAP). Modo `--sequential` não permite compressão via paralelismo.

---

## Story Execution Order

| Order | Story ID | Title | Phase | Dependencies | Critical Path | Estimated Effort |
|-------|----------|-------|-------|--------------|---------------|-----------------|
| 1 | story-0048-0001 | Investigação + repro Bug A + repro Bug B | 0 | — | Yes | L |
| 2 | story-0048-0002 | ADR-0048-A + ADR-0048-B | 1 | story-0048-0001 | Yes | M |
| 3 | story-0048-0003 | LANGUAGES=java + CliLanguageValidator | 2 | story-0048-0002 | Yes | M |
| 4 | story-0048-0010 | Template Pebble CLAUDE.md | 2 | story-0048-0002 | No | S |
| 5 | story-0048-0004 | StackMapping cleanup (+ csharp leftover) | 3 | story-0048-0003 | No | M |
| 6 | story-0048-0005 | Templates agents/hooks/settings delete | 3 | story-0048-0003 | Yes | M |
| 7 | story-0048-0006 | Skills, rules, anti-patterns delete | 3 | story-0048-0003 | No | M |
| 8 | story-0048-0007 | 8 goldens + 8 YAMLs atomic delete | 4 | story-0048-0003, 0005, 0006 | Yes | L |
| 9 | story-0048-0008 | SmokeProfiles 17→9 + test trim | 5 | story-0048-0007 | Yes | L |
| 10 | story-0048-0009 | Bug A fix (pruneEmptyDirs) + regen goldens | 6 | story-0048-0008 | No | L |
| 11 | story-0048-0011 | Bug B fix (ClaudeMdAssembler) + regen goldens | 6 | story-0048-0010, 0048-0008 | Yes | XL |
| 12 | story-0048-0012 | Epic0048EndToEndTest + higienização | 7 | story-0048-0009, 0048-0011 | Yes | M |
| 13 | story-0048-0013 | Audit + README + CHANGELOG v4.0.0 | 8 | story-0048-0012 | Yes | M |

> **Critical Path Legend:** `Yes` = story is on the critical path (delay impacts epic deadline); `No` = story has slack.
> **Estimated Effort:** `S` (small), `M` (medium), `L` (large), `XL` (extra-large).

> **Ordem de dispatch efetiva (sequential):** respeitando DAG do IMPLEMENTATION-MAP, na ordem: 0001 → 0002 → 0003 → 0010 → 0004 → 0005 → 0006 → 0007 → 0008 → 0009 → 0011 → 0012 → 0013. Within-phase ties quebradas por: (i) critical-path priority; (ii) menor ID numérico.

---

## Pre-flight Analysis Summary

| Check | Status | Details |
|-------|--------|---------|
| Story files present | PASS | 13 story files (story-0048-0001..0013) encontrados em plans/epic-0048/ |
| Dependencies resolved | PASS | IMPLEMENTATION-MAP.md Section 1 parseada sem conflitos |
| Circular dependencies | PASS | DAG topologicamente ordenável (9 fases) |
| Implementation map valid | PASS | Seção 2 (fases) + Seção 4 (Mermaid) consistentes com Seção 1 |

Preflight considerations:

- `--sequential` desativa Phase 0.5 (conflict analysis) per skill body. Todas as stories executam em ordem; risco de conflito inter-story minimizado.
- `--auto-merge` ativo: cada PR de story merge automaticamente em `develop` após checks verdes. Stories dependentes iniciam só após merge (verificação via `gh pr view --json state`).
- Global DoR waived: feature branch de épico, tag de rollback e backup branch NÃO criados. Rollback por PR revert individual se necessário.

---

## Resource Requirements

| Resource | Estimate | Notes |
|----------|----------|-------|
| Estimated tokens | ~2.5M-5M tokens total | 13 stories × ~200k-400k tokens por story implementation (varia com complexidade) |
| Estimated wall time | ~85h serial | Se CI média de 5-10min por PR + review humano, considere +overhead 20-30% |
| Max parallel subagents | 1 | --sequential forçado |
| Peak memory estimate | N/A | Session em Opus 4.7 1M context; conversa longa esperada |

---

## Risk Assessment

| Risk | Severity | Likelihood | Mitigation |
|------|----------|------------|------------|
| Telemetry hook re-enable after session restart | Low | Possible | settings.local.json gitignored; env var CLAUDE_TELEMETRY_DISABLED=1 persistente; reverter manualmente no fim |
| CI failure em story-0048-0007 (deleção 2835 goldens) | High | Likely | Story tem guard ProfileRegistrationIntegrityTest; RED window esperado entre 0007 e 0008 |
| Coverage drop abaixo de 95%/90% durante Fase 3 | Medium | Possible | Cada story roda `mvn verify` pré-commit; coverage validada em PR check |
| Conflito de regeneração entre stories 0009 e 0011 | High | Possible | IMPLEMENTATION-MAP Nota Fase 6 recomenda sequencial (0009 primeiro); --sequential flag já impõe isso |
| Bug A/B repro falha em workspace fresh (story 0001) | Medium | Unlikely | Story 0001 produz `repro-bug-{a,b}.sh` que confirma antes de prosseguir |
| Story subagent timeout / retrying > 2 vezes | Medium | Possible | MAX_RETRIES=2 default; após esgotar, checkpoint marca FAILED e pede `--resume` |
| Sessão interrompida meio-épico | High | Possible | `execution-state.json` persistido por story completion; `--resume` suportado |

---

## Checkpoint Strategy

| Parameter | Value |
|-----------|-------|
| Checkpoint frequency | Per story completion |
| Save on phase completion | Yes (implícito via last-story-of-phase) |
| Save on story completion | Yes |
| Save on integrity gate failure | Yes |
| State file location | plans/epic-0048/execution-state.json |

### Recovery Procedures

Cenários e ações:

- **Story FAILED (retries < 2):** orchestrator re-dispachar story após breve análise do erro no summary.
- **Story FAILED (retries >= 2):** marcar como FAILED terminal, propagar BLOCKED para dependentes. Usuário decide `--resume` manual ou abort.
- **Integrity gate FAIL no fim de fase:** per skill body, tentar agent-assisted regression fix (default EPIC-0042). Fallback: `git revert` do commit suspeito.
- **Session interrompida:** retomar com `/x-epic-implement 0048 --resume --sequential`. Reclassification dos status PENDING/IN_PROGRESS → PENDING para retry. PRs merged verificados via `gh pr view`.
- **PR comment remediation desejada:** ao fim do épico, invocar Phase 4 (task #17) para consolidar review comments e aplicar fixes.

### Resume Behavior

- Lê `plans/epic-0048/execution-state.json`.
- Re-classifica stories: IN_PROGRESS → PENDING; SUCCESS preservado; FAILED (retries < 2) → PENDING; BLOCKED reavaliadas com base em `status` das deps (não `prMergeStatus`, pois modo per-story PR com auto-merge).
- Re-entra no loop de execução no primeiro PENDING com deps satisfeitas.
