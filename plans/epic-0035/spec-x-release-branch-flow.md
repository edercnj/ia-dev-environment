# Especificação: Extensão do Skill `x-release` com Approval Gate, PR-Flow e Deep Validation

## Visão Geral

O skill `x-release` existente (`java/src/main/resources/targets/claude/skills/core/x-release/SKILL.md`, 449 linhas) orquestra o fluxo completo de release em Git Flow: version bump, criação de `release/*`, atualização de version files, geração de CHANGELOG, merge duplo (main + develop), tag em main, cleanup. É user-invocable via `/x-release [major|minor|patch|X.Y.Z] [--dry-run] [--skip-tests] [--no-publish] [--hotfix]`.

Este épico **estende** (não substitui) o `x-release` atual para corrigir três lacunas graves identificadas por auditoria e introduzir um gate de aprovação humana explícito entre a abertura do PR da release e a aplicação de tag + back-merge.

## Problema

### Gap 1: Violação direta da Rule 09 (Branching Model)

Os Steps 7 (MERGE-MAIN) e 9 (MERGE-BACK) do skill atual executam `git merge --no-ff` **direto** em `main` e `develop` via CLI local, sem PR. A Rule 09 proíbe explicitamente commits diretos para `main` e `develop`. O skill só funciona hoje para quem tem permissão de bypass da branch protection. Projetos que aplicam a regra corretamente verão o push final falhar com "protected branch", deixando o release em estado inconsistente.

### Gap 2: Ausência de approval gate

Os Steps 7 → 8 → 9 → 10 (merge main → tag → merge develop → push) rodam em sequência sem qualquer pausa humana. Não há ponto onde o operador possa revisar a release consolidada antes de criar a tag — e tags pushed são difíceis de reverter. A release v2.3.0 do próprio `ia-dev-env` (PRs #262, #263, #264) ilustra o problema: múltiplas etapas manuais com janelas temporais em estado intermediário.

### Gap 3: Validação rasa

O Step 2 atual roda apenas `{{BUILD_COMMAND}}` (geralmente `mvn clean test`) e checa working directory + branch. Não valida:
1. Coverage thresholds (Rule 05 exige ≥95% line / ≥90% branch)
2. Golden file consistency (17+ profiles do projeto)
3. Generation dry-run compare (para projetos Claude)
4. Hardcoded version strings em scripts/docs
5. Cross-file version consistency (pom.xml ↔ CHANGELOG ↔ branch ↔ tag)
6. `[Unreleased]` não vazio no CHANGELOG

## Objetivo

Estender o `x-release` com 5 camadas novas, preservando 100% dos comportamentos corretos:

1. **Phase VALIDATE-DEEP** (substitui Step 2) — bateria completa de validação (8 checks + 1 condicional)
2. **Phase OPEN-RELEASE-PR** (substitui Step 7 MERGE-MAIN) — `gh pr create --base main --head release/X.Y.Z`
3. **Phase APPROVAL-GATE** (nova) — state file + halt; `--interactive` adiciona `AskUserQuestion`
4. **Phase RESUME-AND-TAG** (nova, ativada por `--continue-after-merge`) — verifica via `gh pr view` e só então cria tag
5. **Phase BACK-MERGE-DEVELOP** (substitui Step 9) — `gh pr create --base develop`, conflict detection

**Novas flags:**
- `--continue-after-merge` — resume a partir de RESUME-AND-TAG
- `--interactive` — pausa in-session via `AskUserQuestion` (fallback para state file)
- `--signed-tag` — tag GPG assinada
- `--state-file <path>` — override do caminho do state file

**Workflow completo após épico:**

```
 0. RESUME_DETECTION -> Step novo: verifica gh/jq, carrega state file se presente
 1. DETERMINE        -> [preservado] Parse argument / auto-detect bump
 2. VALIDATE_DEEP    -> [NOVO] 8 checks (tests, coverage, golden, consistency)
 3. BRANCH           -> [preservado] Create release/X.Y.Z from develop
 4. UPDATE           -> [preservado] Update version files (strip SNAPSHOT)
 5. CHANGELOG        -> [preservado] Delegate to x-release-changelog
 6. COMMIT           -> [preservado] Create release commit
 7. OPEN_RELEASE_PR  -> [NOVO] gh pr create --base main --head release/X.Y.Z
 8. APPROVAL_GATE    -> [NOVO] Save state, halt (or AskUserQuestion if --interactive)
                       === HUMAN MERGES PR VIA GITHUB UI ===
 9. RESUME_AND_TAG   -> [NOVO] Re-invoke with --continue-after-merge. Verify merged, tag main
10. BACK_MERGE_DEV   -> [NOVO] gh pr create --base develop (or conflict PR)
11. CLEANUP          -> [preservado+] Delete release branch + state file
```

## Componentes do Sistema

### 1. State File Schema

Arquivo: `plans/release-state-<X.Y.Z>.json`

```json
{
  "schemaVersion": 1,
  "version": "2.3.0",
  "phase": "APPROVAL_PENDING",
  "branch": "release/2.3.0",
  "baseBranch": "develop",
  "hotfix": false,
  "dryRun": false,
  "signedTag": false,
  "interactive": false,
  "prNumber": 262,
  "prUrl": "https://github.com/.../pull/262",
  "prTitle": "release: v2.3.0",
  "startedAt": "2026-04-10T10:00:00Z",
  "lastPhaseCompletedAt": "2026-04-10T10:15:00Z",
  "phasesCompleted": ["DETERMINE","VALIDATE_DEEP","BRANCH","UPDATE","CHANGELOG","COMMIT","OPEN_RELEASE_PR"],
  "targetVersion": "2.3.0",
  "previousVersion": "2.2.2",
  "bumpType": "minor",
  "changelogEntry": "## [2.3.0] - 2026-04-10\n### Added\n- ...",
  "tagMessage": "Release v2.3.0\n\nChanges..."
}
```

**Enum de `phase`:** `INITIALIZED`, `DETERMINED`, `VALIDATED`, `BRANCHED`, `UPDATED`, `CHANGELOG_DONE`, `COMMITTED`, `PR_OPENED`, `APPROVAL_PENDING`, `MERGED`, `TAGGED`, `BACKMERGE_OPENED`, `BACKMERGE_CONFLICT`, `COMPLETED`.

**Regra de idempotência:** Toda phase lê o state file no início, pula se já rodou. Escrita atômica via tmp file + rename.

### 2. Phase VALIDATE-DEEP — 8 checks (+1 condicional)

| # | Check | Abort se |
|---|---|---|
| 1 | `git status --porcelain` | output não-vazio |
| 2 | branch correta (develop ou main se hotfix) | outra branch |
| 3 | `[Unreleased]` não-vazio | seção vazia |
| 4 | `{{BUILD_COMMAND}}` | exit ≠ 0 |
| 5 | Coverage Jacoco | line < 95% ou branch < 90% |
| 6 | Golden file tests | exit ≠ 0 |
| 7 | Hardcoded version grep | matches fora de pom/CHANGELOG |
| 8 | Cross-file version consistency | qualquer diferença |
| 9 (cond) | `ia-dev-env generate --dry-run` diff | para projetos geradores |

`--skip-tests` pula apenas checks 4-6.

### 3. Phase OPEN-RELEASE-PR

```bash
git push -u origin "release/${VERSION}"
CHANGELOG_ENTRY=$(awk "/^## \[${VERSION}\]/,/^## \[/" CHANGELOG.md | sed '$d')
gh pr create --base main --head "release/${VERSION}" \
  --title "release: v${VERSION}" \
  --body "$(build_pr_body $CHANGELOG_ENTRY)"
# capture prNumber, prUrl, persist to state file
```

PR body inclui CHANGELOG entry, bump type, versão anterior, instruções `/x-release ${VERSION} --continue-after-merge`, checklist validado pela VALIDATE-DEEP.

### 4. Phase APPROVAL-GATE

Default:
```
Persist phase: APPROVAL_PENDING
Print instructions with PR URL + resume command
exit 0
```

Interactive (`--interactive`):
`AskUserQuestion` com 3 opções:
1. "PR merged, continue to tag" → verifica via `gh pr view` antes de prosseguir
2. "Halt — resume later with --continue-after-merge" → exit 0
3. "Cancel release entirely" → confirmação + delete state file + exit 2

### 5. Phase RESUME-AND-TAG (--continue-after-merge)

```bash
# Defense in depth: NEVER trust state file alone
gh pr view $PR_NUMBER --json state,mergedAt
# Require state == MERGED
git checkout main && git pull
if [ "$SIGNED_TAG" = "true" ]; then
  git tag -s "v${VERSION}" -m "$TAG_MESSAGE"
else
  git tag -a "v${VERSION}" -m "$TAG_MESSAGE"
fi
git push origin "v${VERSION}"  # warning if fails, not abort
```

### 6. Phase BACK-MERGE-DEVELOP

```bash
git checkout -b "chore/backmerge-v${VERSION}" origin/develop
git merge --no-commit --no-ff origin/main
# If clean: SNAPSHOT advance (Java) + gh pr create --base develop
# If conflict: commit --no-verify + gh pr create with conflict body
```

### 7. Error Catalog Expandido

Adicionar ≥20 novos error codes agrupados por phase: `DEP_*`, `STATE_*`, `RESUME_*`, `VALIDATE_*`, `PR_*`, `APPROVAL_*`, `BACKMERGE_*`, `HOTFIX_*`.

## Anti-escopo (preservado sem alteração)

- Step 1 DETERMINE — bump detection por Conventional Commits
- Step 3 BRANCH — criação de release/hotfix
- Step 4 UPDATE — version files + SNAPSHOT handling Java
- Step 5 CHANGELOG — delegação a `x-release-changelog` (interface preservada)
- Step 6 COMMIT — release commit Conventional Commits
- Step 11 CLEANUP — delete release branch
- Hotfix workflow — ajustado para PR-flow mas semântica preservada
- Dry-run mode — atualizado para mostrar nova sequência
- Flags `--skip-tests`, `--no-publish`, `--hotfix` — preservadas
- Integração com `x-release-changelog` — interface intacta
- `ia-dev-env` NÃO finaliza a release v2.3.0 pendente — remediação manual separada

## Acceptance Criteria de Alto Nível (Cross-Cutting Rules)

1. **RULE-A1 (Rule 09 — PR-flow):** Qualquer merge para main/develop via `gh pr create`. Proibido `git merge` direto.
2. **RULE-A2 (Preservação):** Steps 1, 3, 4, 5, 6, 11, hotfix, dry-run, flags antigas intactos.
3. **RULE-A3 (Idempotência):** Toda phase lê state no início; escrita atômica.
4. **RULE-A4 (Source of truth):** Edits em `java/src/main/resources/targets/claude/skills/core/x-release/`.
5. **RULE-A5 (Golden regen):** Stories que editam SKILL.md regeneram golden files nos 17+ profiles no mesmo commit.
6. **RULE-A6 (Rule 05):** Coverage ≥95% line / ≥90% branch preservada.
7. **RULE-A7 (Conventional Commits):** Commits `<type>(epic-0035): <desc>`.
8. **RULE-A8 (Defense in depth):** RESUME-AND-TAG sempre via `gh pr view`, nunca só state file.

## Constraints Técnicos

- Linguagem: Markdown (SKILL.md) + bash inline
- Dependências: `gh` CLI ≥ 2.0, `jq`, `git` ≥ 2.30
- Platform: Claude Code apenas (pós epic-0034)
- Template vars: `{{BUILD_COMMAND}}`, `{{COVERAGE_LINE_THRESHOLD}}`, `{{COVERAGE_BRANCH_THRESHOLD}}`, `{{GOLDEN_TEST_COMMAND}}`, `{{GENERATION_COMMAND}}` (novos)

## Referências (arquivos-chave)

### Arquivo principal
- `java/src/main/resources/targets/claude/skills/core/x-release/SKILL.md`
- `java/src/main/resources/targets/claude/skills/core/x-release/README.md`

### Skills delegadas/referenciadas
- `.../core/x-release-changelog/SKILL.md`
- `.../core/x-git-push/SKILL.md`
- `.../core/x-review-pr/SKILL.md`
- `.../core/x-test-run/SKILL.md`

### Padrões de orquestração
- `.../core/x-dev-story-implement/SKILL.md` (state file + resume pattern)
- `.../core/x-dev-epic-implement/SKILL.md` (execution-state.json pattern)

### Rules
- `.claude/rules/08-release-process.md`
- `.claude/rules/09-branching-model.md` (violada atualmente)
- `.claude/rules/05-quality-gates.md`
- `.claude/rules/03-coding-standards.md`

### Templates
- `java/src/main/resources/shared/templates/_TEMPLATE-{EPIC,STORY,IMPLEMENTATION-MAP}.md`

### Testes Java
- `ReleaseSkillTest.java`, `ReleaseChecklistAssemblerTest.java`, `ReleaseManagementGitFlowTest.java`
- `ReleaseStateFileSchemaTest.java` (novo)

### Golden files
- `java/src/test/resources/golden/*/.claude/skills/x-release/**` (17+ profiles)

### Documentação
- `CHANGELOG.md` — entry `[Unreleased]`
- Possível ajuste em `.claude/rules/08-release-process.md`

## Stories Previstas (decomposição guia)

| Layer | Story esperada |
|---|---|
| Foundation | 0001: frontmatter + flags + state file schema + Step 0 Resume Detection |
| Core-A | 0002: Phase VALIDATE-DEEP (8 checks) |
| Core-B | 0003: Phase OPEN-RELEASE-PR (gh pr create main) |
| Core-C | 0004: Phase APPROVAL-GATE (halt + --interactive) |
| Core-D | 0005: Phase RESUME-AND-TAG (verify + tag) |
| Core-E | 0006: Phase BACK-MERGE-DEVELOP (PR + conflict) |
| Cross-cutting | 0007: Dry-run + Error catalog + Hotfix PR-flow |
| Cross-cutting | 0008: Golden regen + tests Java + docs + CHANGELOG |

Dependências: 0001 → {0002, 0003} → 0004 → 0005 → 0006 → 0007 → 0008.
Caminho crítico: 7 fases (0001, 0003, 0004, 0005, 0006, 0007, 0008).
Paralelismo: 0002 ‖ 0003 após 0001.

## Métricas de Sucesso (DoD do Épico)

- [ ] `/x-release` funciona end-to-end em dogfood (release v2.4.0 ou equivalente)
- [ ] `mvn verify -Pall-tests` verde com golden files regenerados
- [ ] Coverage ≥ 95% / ≥ 90%
- [ ] Zero `git merge` direto para main/develop
- [ ] State file criado, lido e apagado em ciclo completo
- [ ] `--continue-after-merge` funciona após fechar/reabrir Claude
- [ ] `--interactive` pausa in-session corretamente
- [ ] Dry-run mostra nova sequência
- [ ] Hotfix continua funcionando (regressão zero)
- [ ] CHANGELOG tem entry EPIC-0035
- [ ] Documentação (README + references) atualizada
