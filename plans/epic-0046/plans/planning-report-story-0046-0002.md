# Story Planning Report — story-0046-0002

## Header

| Field | Value |
|-------|-------|
| Story ID | story-0046-0002 |
| Epic ID | 0046 |
| Date | 2026-04-16 |
| Agents Participating | Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner |

## Planning Summary

Retrofita 7 skills de planejamento para propagar `**Status:** Pendente → Planejada` no MESMO commit em que geram o plan artefact. Maior story do épico em volume textual (7 SKILL.md diffs + 1 CLI wrapper + 6 smoke tests). V2-gated via `SchemaVersionResolver`. Consome CLI `StatusFieldParserCli` (TASK-001).

## Architecture Assessment

**Layers afetadas:** Adapter (`StatusFieldParserCli` em `dev.iadev.adapter.inbound.cli`) + Doc (7 SKILL.md) + Test (integration).

**Novos componentes:**
- `dev.iadev.adapter.inbound.cli.StatusFieldParserCli` — subcomandos `read <file>` e `write <file> <status>`, exit codes 0/20/40 alinhados com `StatusFieldParser`.

**SKILL.md retrofitadas:**
- `core/plan/x-story-plan/SKILL.md` — adiciona bloco "Status Update" no fim da Phase 4 (antes do commit).
- `core/plan/x-task-plan/SKILL.md` — análogo; atualiza `task-TASK-*.md` ao gerar `plan-task-*.md`.
- `core/plan/x-arch-plan/SKILL.md` + `core/test/x-test-plan/SKILL.md` — idempotente (se Status já Planejada, no-op).
- `core/plan/x-epic-create/SKILL.md` — seta Status inicial `Em Refinamento` no epic criado.
- `core/plan/x-epic-decompose/SKILL.md` — seta `Pendente` em todas stories criadas; delega à `x-epic-map` o preenchimento da coluna Planejamento.
- `core/plan/x-epic-map/SKILL.md` — substitui `{{PLANNING_STATUS}}` por valor real; popula coluna Planejamento.

## Test Strategy Summary

**Outer loop:** para cada retrofit, 1 smoke test em sandbox: cria épico toy v2, invoca a skill, verifica `**Status:**` transicionou + commit inclui source + plan artefact.

**Inner loop (TPP):**
- Level 1 (nil): `read_whenFileMissing_exitsWith20`
- Level 2 (constant): `read_whenValidPendente_printsPendente`
- Level 3 (scalar): `write_validTransition_succeeds`
- Level 4 (collection): `write_fullMatrix_parametrized`
- Level 5 (conditional): `write_invalidTransition_exitsWith40`
- Level 6 (iteration): idempotency + whitespace tolerance

**Coverage:** ≥ 95% no CLI; helpers (StatusFieldParser) já cobertos pela story 0001.

## Security Assessment Summary

**OWASP A04 (Insecure Design):** CLI recebe path do usuário → canonicalização obrigatória antes de qualquer I/O. Reusa `StatusFieldParser` (já canonicaliza).

**A08 (Software and Data Integrity):** Commit atômico via `x-git-commit` garantido por Rule 13 INLINE-SKILL; nenhum commit parcial.

**Dependency security:** picocli já no classpath; nenhuma dep nova.

## Implementation Approach

**V2-gating pattern canônico** (reusado em todas 7 skills):

```
1. Read execution-state.json
2. Resolve planningSchemaVersion via SchemaVersionResolver
3. If V2:
   a. Read current Status of source artifact
   b. Validate transition Pendente → Planejada (via LifecycleTransitionMatrix)
   c. Write new status via StatusFieldParserCli write <file> Planejada
   d. Stage source + plan + map updates
   e. Skill(skill: "x-git-commit", args: "docs(scope): ...")
4. Else (v1): skip — no status sync (Rule 19)
```

**Rejected alternative:** writing the status update directly via Edit tool in the SKILL.md prompts. Rejeitado porque: (a) dependência direta de regex na prompt é frágil; (b) CLI wrapper reusa testes.

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 6 |
| Architecture tasks | 5 |
| Test tasks | 6 pairs embedded em TASKs 001-006 |
| Security tasks | 1 augmented em TASK-001 |
| Quality gate tasks | 1 (TL-001 em DoD) |
| Validation tasks | 1 (PO-001) |
| Merged tasks | 5 (todas TASKs 001-006 são merged ARCH+QA ou ARCH+QA+SEC) |
| Augmented tasks | 1 (TASK-001) |

## Consolidated Risk Matrix

| Risk | Source Agent | Severity | Likelihood | Mitigation |
|------|------------|----------|------------|------------|
| 7 retrofits no mesmo PR → review exaustivo | Tech Lead | MEDIUM | HIGH | Dividir em 4 PRs por TASK (001, 002/003, 004, 005) — 006 consolida |
| SKILL.md diff quebra golden test | QA | MEDIUM | MEDIUM | Golden regen em cada TASK + CI verde antes de merge |
| Paradoxo: auto-planejamento do EPIC-0046 foi feito com skills pre-fix | Tech Lead | LOW | HIGH | Explicitamente documentado na spec §8; audit da story 0007 valida |
| V2-gating inconsistente entre skills | Tech Lead | MEDIUM | MEDIUM | Extrair helper markdown `v2-gating-snippet.md` reusável em todas as SKILL.md |

## DoR Status

**READY** — ver `dor-story-0046-0002.md`.
