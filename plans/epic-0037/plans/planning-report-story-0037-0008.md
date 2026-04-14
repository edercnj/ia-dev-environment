# Story Planning Report — story-0037-0008

## Header

| Field | Value |
|-------|-------|
| Story ID | story-0037-0008 |
| Epic ID | 0037 |
| Title | `x-release` Worktree para Release/Hotfix Branches |
| Date | 2026-04-13 |
| Agents Participating | Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner |
| Mode | Multi-agent consolidated (x-story-plan) |

## Planning Summary

Story doc-only desbloqueada após conclusão de EPIC-0035 (que reescreveu `x-release/SKILL.md` com phases `VALIDATE-DEEP`, `OPEN-RELEASE-PR`, `APPROVAL-GATE`, `RESUME-AND-TAG`, `BACK-MERGE-DEVELOP` e o state file `plans/release-state-<X.Y.Z>.json`). Esta story adiciona **worktree-aware branch creation** (phase BRANCH reescrita) e **phase CLEANUP-WORKTREE** pós-tag, estendendo o schema do state file com campo opcional `worktreePath` e a enum `phase` com valor `WORKTREE_CLEANED`. Não há código Java; impacto é limitado a `java/src/main/resources/targets/claude/skills/core/x-release/` + regeneração de golden fixtures.

5 agentes produziram 37 propostas brutas; consolidação determinística resultou em 18 tasks (8 merges, 1 augmentation, 2 pares RED-GREEN). Critical path = 12 tasks (XS/S/M mix).

## Architecture Assessment

- **Layers afetadas:** Documentação apenas — generator source of truth (`java/src/main/resources/targets/claude/skills/core/x-release/`) + golden fixtures (`java/src/test/resources/golden/**/`).
- **Componentes modificados:**
  - `SKILL.md` — Phase BRANCH reescrita (3 substeps: detect-context, idempotent create, persist worktreePath) + nova Phase CLEANUP-WORKTREE após RESUME-AND-TAG.
  - `references/state-file-schema.md` — campo `worktreePath` opcional; enum `phase` estendido com `WORKTREE_CLEANED`; 4 novos error codes.
- **Dependency direction:** N/A (doc-only).
- **Backward compatibility:** Schema `schemaVersion=1` preservado — additive only. State files pré-EPIC-0037 sem `worktreePath` permanecem válidos.
- **Integration points:** `x-git-worktree` via Skill tool (RULE-013 Pattern 1); state file compartilhado com EPIC-0035.
- **Implementation order:** TASK-001 (pre-flight) → TASK-002 (slug validation doc) → TASK-003 (BRANCH phase) → TASK-004/005 em paralelo → TASK-006 (consistência) → testes RED/GREEN → smokes → PR.

## Test Strategy Summary

- **Automated coverage:** Golden file regression (TASK-010 RED → TASK-011 GREEN) + `ReleaseStateFileSchemaTest` backward-compat cases (TASK-008 RED → TASK-009 GREEN).
- **Mapping Gherkin → Tests:** 6 cenários originais + 4 amendments (TASK-007) → TASK-013..017 smoke runs em repo de teste disposable.
- **TPP:** Doc-only story; testes automatizados são N/A (schema) e GREEN-only (goldens). TPP não se aplica.
- **Coverage:** Coverage Java mantém baseline (nenhuma classe Java tocada). Zero regressão esperada.
- **Test effort:** 5×M (smokes) + 2×S + 3×XS. Smokes requerem repo de teste + `gh` CLI autenticado.

## Security Assessment Summary

- **OWASP mapping:** A03:2021 (Injection) é risco primário — bash snippets documentados são copy-paste por operadores. Secundários: A04 (Insecure Design, idempotent reuse sem branch check = CWE-367), A09 (error leakage, CWE-209).
- **CWE coverage:** CWE-78 (OS Command Injection), CWE-22 (Path Traversal), CWE-59 (Link Following), CWE-209 (Error Message Leakage), CWE-367 (TOCTOU).
- **Controles:**
  1. HOTFIX_SLUG regex `^[a-z0-9][a-z0-9-]{0,62}$` antes de interpolação — TASK-002 standalone + TASK-003 aplicação.
  2. Branch match check em idempotent reuse (`git -C "$WT_PATH" rev-parse --abbrev-ref HEAD`) — TASK-003 DoD.
  3. State file path canonicalization via `realpath` + prefix assertion — TASK-003 DoD.
  4. Error message sanitization: apenas error code + WT_ID relativo no output user-visible — TASK-003 DoD + TASK-005 catálogo.
  5. RULE-001 Source-of-Truth audit (`.claude/` zero-edit) — TASK-001 DoD.
- **Risk level:** Medium (injection surface existe, mas mitigações são documentais e direcionam copy-paste para snippets seguros).
- **Compliance:** N/A — ia-dev-environment não ativa compliance framework.

## Implementation Approach

Tech Lead selecionou a abordagem de **edição mínima focada no generator source of truth**:

1. **Pre-flight gate** antes de qualquer edit (EPIC-0035 completude + branch criada de `develop` + SoT audit).
2. **Phase BRANCH rewrite** como única edição crítica em `SKILL.md` — substitui `git checkout -b` direto por delegação a `x-git-worktree create` via Skill tool (RULE-013 Pattern 1), com hotfix/release variants, idempotency check, branch match validation.
3. **Phase CLEANUP-WORKTREE** adicionada após RESUME-AND-TAG — aproveita que EPIC-0035 já moveu merges para `gh pr merge` (worktree não precisa mais ser removido antes dos merges locais). Simplifica sequenciamento.
4. **Schema extension additive only** — zero bump de `schemaVersion`, preservando compat com state files legados.
5. **Golden regen + `mvn clean verify`** como gate obrigatório antes da PR.

Alternativa considerada (rejeitada): bump `schemaVersion=2` com migração. Descartado porque o campo é opcional e consumidores já tratam unknown fields.

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 18 |
| Architecture tasks | 3 |
| Test tasks | 10 |
| Security tasks | 1 standalone + augmented |
| Quality-gate tasks | 4 |
| Validation tasks | 1 (Gherkin amendment) |
| Merged tasks | 8 |
| Augmented tasks | 1 (TASK-003 com 4 SEC controls) |
| Critical path length | 12 tasks |
| Parallelizable pairs | TASK-008/TASK-010 (RED pair), TASK-013/TASK-014 (smokes independentes) |

## Consolidated Risk Matrix

| Risk | Source Agent | Severity | Likelihood | Mitigation |
|------|--------------|----------|------------|------------|
| Slug injection via `--hotfix <user-input>` | Security | HIGH | Medium | TASK-002 regex validation; TASK-003 aplicação antes de qualquer interpolação shell |
| Stale worktree reused sem branch check → wrong commit tagged | Security | HIGH | Low | TASK-003 DoD: `git rev-parse --abbrev-ref HEAD` match antes de reuse; abort com `WT_RELEASE_BRANCH_MISMATCH` |
| State file path traversal (symlink escape) | Security | MEDIUM | Low | TASK-003 DoD: `realpath` + prefix assert `$(git rev-parse --show-toplevel)/.claude/worktrees/` |
| Error messages expondo caminhos absolutos | Security | LOW | Medium | TASK-003 DoD + TASK-005: catálogo separa user-visible vs debug-only |
| Smoke tests requerem setup externo (test repo, gh auth) | QA | MEDIUM | High | Documentar setup antes; TASK-018 pode ter blocker temporário |
| Golden regeneration drift fora do escopo x-release | QA | MEDIUM | Low | TASK-011 DoD: diff escopo-limpo; investigar se surgir |
| Schema breaking consumidores pre-EPIC-0037 | TechLead | MEDIUM | Very Low | TASK-005: additive only, `schemaVersion=1` preservado; TASK-008 test backward-compat case |
| Divergência entre schema doc e `ReleaseStateFileSchemaTest` | QA+TL | LOW | Low | TASK-017 DoD explicitamente valida paridade |
| RULE-013 violation (bare-slash em delegação) | TechLead | HIGH | Low | TASK-003/004 DoD exige Skill tool form; TASK-006 audit grep |
| `.claude/` edit acidental | Security+TL | HIGH | Low | TASK-001 audit `git diff --name-only` |

## Amendments to Acceptance Criteria (§7 of story)

PO identificou 4 cenários adicionais a amendar via TASK-007:

1. **Stale worktree com branch divergente** — detecta mismatch e aborta (complementa §3.2.1 idempotency).
2. **Dry-run mode** — `/x-release --dry-run` não cria worktree (cobre DoD bullet "Smoke release dry-run passa").
3. **Invocação de dentro de worktree** — operador já em worktree, IN_WT=true WARNING branch.
4. **State file worktreePath cleared on success** — complementa cenário de erro (só erro-path cobrido originalmente).

## DoR Status

**READY** — Ver `dor-story-0037-0008.md` para checklist completa.

Todos os 10 checks mandatórios passam. Checks condicionais (compliance, contract tests) são N/A para este projeto.

## Artifacts Generated

- `plans/epic-0037/plans/tasks-story-0037-0008.md` — task breakdown consolidado (este planning round)
- `plans/epic-0037/plans/planning-report-story-0037-0008.md` — este relatório
- `plans/epic-0037/plans/dor-story-0037-0008.md` — DoR atualizado para READY

## Next Steps

1. Atualizar `execution-state.json` para refletir `planningStatus: READY` e `dorVerdict: READY` para story-0037-0008.
2. Garantir que `IMPLEMENTATION-MAP.md` reflita o desbloqueio (EPIC-0035 `Concluído`).
3. Proceder com `/x-epic-implement 0037` completo (todas 10 stories elegíveis).
