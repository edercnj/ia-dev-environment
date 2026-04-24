# Spec — EPIC-0058: Audit Scripts Lifecycle & Generation

> **Proposta de Épico:** EPIC-0058
> **Target branch:** epic/0058
> **Worktree:** .claude/worktrees/epic-0058/

## 1. Problem Statement

As Rules 19, 21, 22, 23 e 24 referenciam 5 scripts de auditoria CI como mecanismo de enforcement, mas o catálogo está em estado inconsistente:

- **3 dos 5 scripts não existem fisicamente no repositório:**
  - `scripts/audit-flow-version.sh` (citado em `.claude/rules/19-backward-compatibility.md:103`) — ausente.
  - `scripts/audit-epic-branches.sh` (citado em `.claude/rules/21-epic-branch-model.md:71`) — ausente.
  - `scripts/audit-skill-visibility.sh` (citado em `.claude/rules/22-skill-visibility.md:76,93`) — ausente.
  - `scripts/audit-model-selection.sh` (Rule 23:90) — existe.
  - `scripts/audit-execution-integrity.sh` (Rule 24:34,65,77,105) — existe.

- **Nenhum dos 5 scripts é gerado pelo `ia-dev-env`.** O pipeline de geração (22 assemblers em `AssemblerFactory.java`) produz `.claude/hooks/` via `HooksAssembler` (para Claude Code hooks de runtime) e `.github/workflows/` via `CicdAssembler`, mas **não produz nenhum diretório `scripts/` ou `.claude/scripts/`**. Os dois scripts existentes (`audit-model-selection.sh` e `audit-execution-integrity.sh`) vivem apenas em `/scripts/` na raiz deste repositório, manuscritos, versionados direto — e portanto **nenhum projeto gerado por `ia-dev-env` herda esses gates de CI.** Quebra a simetria com outros artefatos (hooks, rules, skills, templates).

- **Fragmentação sem catálogo canônico.** Os gates de governance deste repositório estão espalhados em 4 lugares distintos sem um documento mestre que cruze `gate → rule → tipo → localização → exit code`:
  - Hooks runtime: `.claude/hooks/verify-story-completion.sh` (Stop hook, Rule 24 Camada 2).
  - CI scripts: `scripts/audit-*.sh` (Rules 22/23/24, Camada 3).
  - Java tests: `LifecycleIntegrityAuditTest.java` (EPIC-0046), `TelemetryMarkerLint.java` (Rule 13).
  - CI workflows: `.github/workflows/*.yml` (parcial — invoca alguns dos scripts).

- **Não há Rule que defina o lifecycle de um audit gate.** Decisões ad hoc em cada epic (Rule 23 escolheu CI script; Rule 13 escolheu Java test; Rule 24 escolheu híbrido hook+script+baseline). Stories futuras não têm orientação formal sobre qual mecanismo escolher.

## 2. Causa raiz

1. **Rules criadas antes da infra.** EPIC-0049 (Rule 19, 21, 22) documentou os scripts como requisito — marcado "(or equivalent)" no texto — mas não criou stories para implementá-los. EPIC-0050 (Rule 23) e EPIC-0052/0057 (Rule 24) só fecharam gaps parciais.
2. **Scope guard Rule 14 restritivo demais.** Rule 14 veta classes Java de runtime, empurrando todo enforcement para bash. Mas o pipeline de geração não contempla geração de `scripts/` → scripts viram "repo-local do gerador" em vez de "artefato gerado".
3. **Catálogo inexistente.** Nenhum documento, assembler, nem story force-materializou a tabela de gates. Logo, novos gates entram sem convenção; gates existentes divergem em naming, exit code, self-check.

## 3. Escopo

### 3.1 Incluído

- Nova Rule 25 "Audit Gate Lifecycle" formalizando taxonomia (Hook runtime / CI script / Java test / CI workflow), matriz de decisão, convenção de naming `audit-{subject}.sh`, exit code padronizado, requisito de flag `--self-check`.
- ADR correspondente registrando a decisão.
- Catálogo canônico `docs/audit-gates-catalog.md` indexando os 5+ gates existentes com colunas: nome, rule, tipo, localização, exit code, self-check flag, referenciado por CI workflow.
- Criação física dos 3 scripts faltantes (`audit-flow-version.sh`, `audit-epic-branches.sh`, `audit-skill-visibility.sh`) com `--self-check`, exit codes conforme Rules 19/21/22, fixtures de teste em `scripts/fixtures/`.
- Novo `ScriptsAssembler` + source-of-truth `java/src/main/resources/targets/claude/scripts/` contendo os 5 scripts canônicos. Registro em `AssemblerFactory` entre `SettingsAssembler` e `DocsAssembler`.
- Movimentação dos 2 scripts existentes (`audit-model-selection.sh`, `audit-execution-integrity.sh`) da raiz `/scripts/` para a source-of-truth. Raiz passa a ser cópia gerada (ou symlink documentado) para não quebrar referências nas Rules.
- Atualização das golden files de todos os 9 perfis (`java/src/test/resources/golden/{profile}/.claude/scripts/`) via `GoldenFileRegenerator`. Asserts no `GoldenFileTest` cobrindo o novo diretório.
- Template CI workflow `shared/ci/audit.yml.tmpl` invocando os 5 scripts em toda PR para `develop` e `epic/*`. Propagação via `CicdAssembler`.

### 3.2 Fora de escopo

- Refatoração de `HooksAssembler` ou qualquer hook Claude Code existente (são artefato de runtime, lifecycle diferente).
- Alterações em `LifecycleIntegrityAuditTest.java` ou `TelemetryMarkerLint.java` (Java tests; Rule 25 catalogará mas não reescreverá).
- Novos linters Java ou extensões do `ia-dev-env` para análise dinâmica (Rule 14 scope guard).
- Refatoração de Rules 19/21/22/23/24 além da adição de nota cruzada apontando para Rule 25 e catálogo.
- Migração de `audits/execution-integrity-baseline.txt` ou alterações em baselines existentes.

## 4. Stories propostas

### Layer 0 — Foundation (Rule + ADR + catálogo)

- **Story 0058-0001** — Criar Rule 25 "Audit Gate Lifecycle" + ADR correspondente. Define taxonomia, matriz de decisão, naming, exit codes, `--self-check` requirement. Atualiza `.claude/README.md` e `CLAUDE.md` com referência cruzada. Regenerar golden para o arquivo de rule novo.
- **Story 0058-0002** — Criar catálogo canônico `docs/audit-gates-catalog.md` com tabela de todos os gates (5 scripts + hook EIE + 2 Java tests + workflows). Adiciona notas cruzadas em cada Rule (19, 21, 22, 23, 24, 13, 46) apontando para o catálogo. Golden das rules atualizado.

### Layer 1 — Gap fill (3 scripts faltantes)

- **Story 0058-0003** — Implementar `scripts/audit-flow-version.sh` conforme Rule 19: valida `flowVersion` em `plans/epic-*/execution-state.json`; exit code `FLOW_VERSION_VIOLATION` (1) em divergência, 0 em sucesso; flag `--self-check` (exit 0 confirmando estrutura do script); fixtures em `scripts/fixtures/audit-flow-version/` cobrindo happy-path, flowVersion ausente com warning, flowVersion inválido.
- **Story 0058-0004** — Implementar `scripts/audit-epic-branches.sh` conforme Rule 21: scan `gh pr list --base develop --head 'epic/*'`, verifica `flowVersion: "2"` e ausência de force-push após primeiro merge; exit code `EPIC_BRANCH_VIOLATION` (1); `--self-check`; fixtures mockando resposta `gh`.
- **Story 0058-0005** — Implementar `scripts/audit-skill-visibility.sh` conforme Rule 22: valida prefixo/frontmatter consistency, body marker "🔒 INTERNAL SKILL", ausência de trigger slash em internal skills, ausência de cross-reference em user-facing docs; exit code `SKILL_VISIBILITY_VIOLATION` (22); `--self-check`; fixtures com skills conformes e não-conformes.

### Layer 2 — Generation pipeline (scripts viram produto gerado)

- **Story 0058-0006** — Criar `ScriptsAssembler` em `dev.iadev.application.assembler`. Source-of-truth `java/src/main/resources/targets/claude/scripts/` popular com os 5 scripts (mover os 2 existentes para lá). Output `.claude/scripts/*.sh` (com bit executável). Registro em `AssemblerFactory` na categoria Claude Config, após `SettingsAssembler`. Cobertura ≥95% linha / ≥90% branch.
- **Story 0058-0007** — Regenerar golden files dos 9 perfis via `GoldenFileRegenerator`. Adicionar asserts em `GoldenFileTest` cobrindo `.claude/scripts/`. `mvn verify` passa. Preservar os scripts na raiz `/scripts/` como cópia gerada automaticamente (post-build step ou documentação explícita no README raiz) para manter compatibilidade com as Rules 19/21/22/23/24 que os referenciam por path `scripts/*.sh`.

### Layer 3 — CI wiring

- **Story 0058-0008** — Template de workflow `shared/ci/audit.yml.tmpl` com matriz dos 5 scripts, disparado em PR para `develop` e `epic/*`. Integrar em `CicdAssembler` (ou novo sub-assembler). Copilot/Actions verificam que cada script roda com os exit codes esperados. Adicionar job `audit-self-check` que roda todos os `--self-check` antes da matriz principal para detectar scripts corrompidos.

## 5. Dependências

- 0058-0001 bloqueia 0058-0002 (catálogo depende da Rule para convenção de naming).
- 0058-0001 bloqueia 0058-0003, 0058-0004, 0058-0005 (scripts novos seguem convenção da Rule).
- 0058-0001 bloqueia 0058-0006 (assembler implementa convenção).
- 0058-0003, 0058-0004, 0058-0005 bloqueiam 0058-0006 (assembler precisa dos 5 scripts prontos na source-of-truth).
- 0058-0006 bloqueia 0058-0007 (golden depende do assembler).
- 0058-0007 bloqueia 0058-0008 (workflow CI depende dos scripts gerados).
- 0058-0002 paralelo a 0058-0003/0004/0005 (catálogo é doc, não bloqueia scripts).

## 6. Rules aplicáveis

- RULE-001 — Rule 25 (Audit Gate Lifecycle) é criada por esta epic e usada nas demais stories.
- RULE-002 — Rule 19 (Backward Compatibility), Rule 21 (Epic Branch Model), Rule 22 (Skill Visibility) — alvos dos 3 scripts gap-fill.
- RULE-003 — Rule 23 (Model Selection), Rule 24 (Execution Integrity) — scripts existentes migrados para source-of-truth.
- RULE-004 — Rule 14 (Project Scope Guard) — `ScriptsAssembler` é código de geração (dentro do escopo), scripts gerados são bash (não Java runtime — dentro do escopo).
- RULE-005 — Rule 13 (Skill Invocation Protocol) preservada nas stories (sem novas invocações).
- RULE-006 — Rule 08 (Conventional Commits) para commits de cada story (`feat(epic-0058)`, `docs(epic-0058)`, `test(epic-0058)`).
- RULE-007 — Rule 21 (Epic Branch Model) — stories targetam `epic/0058` com auto-merge; gate final `epic/0058 → develop` manual.
- RULE-008 — Rule 05 (Quality Gates) — cobertura ≥95% linha / ≥90% branch no `ScriptsAssembler` e nos fixtures de validação dos scripts.

## 7. Entrega de valor

Três ganhos mensuráveis:

1. **Fantasmas eliminados.** Rules 19/21/22 param de referenciar scripts inexistentes. Gap de 3 scripts zerado. `scripts/audit-*.sh --self-check` passa em todos os 5.
2. **Governance herdada.** Projetos gerados por `ia-dev-env` passam a herdar os 5 gates de CI + workflow template. Hoje, 0 projetos gerados têm governance de audit; pós-épico, todos os perfis (9 golden) têm `.claude/scripts/` + `.github/workflows/audit.yml`.
3. **Catálogo canônico.** `docs/audit-gates-catalog.md` indexa 8 gates (5 scripts + 1 hook EIE + 2 Java tests). Novas Rules passam a ter orientação formal via Rule 25 sobre qual mecanismo escolher. Fragmentação histórica tratada documentalmente.

Impacto secundário: redução de friction em code review — reviewers ganham 1 tabela única para consultar em vez de caçar scripts por Rule.

## 8. Definition of Done (por story)

- Artefatos alvo atualizados na source-of-truth (`java/src/main/resources/targets/claude/` para scripts/rules, `java/src/main/resources/shared/` para workflows).
- Golden files regenerados (`mvn process-resources` + `GoldenFileRegenerator`) em todos os 9 perfis quando a story toca output gerado.
- `mvn verify` passa (incluindo golden tests e, quando aplicável, testes unitários do assembler).
- Story PR targeta `epic/0058` com auto-merge habilitado (Rule 21).
- Evidência completa Rule 24: arch-plan (quando SOLID), review-story, techlead-review-story, verify-envelope, completion-report.
- Cada script novo tem fixture de teste + `--self-check` verde.

## 9. Anti-escopo

- NÃO reformular Rules existentes além de adicionar nota cruzada para Rule 25 e catálogo.
- NÃO alterar `HooksAssembler` nem hooks Claude Code existentes (lifecycle diferente).
- NÃO introduzir classes Java runtime para análise (Rule 14 scope guard).
- NÃO migrar baselines (`audits/execution-integrity-baseline.txt`, `audits/lifecycle-integrity-baseline.txt`).
- NÃO substituir `LifecycleIntegrityAuditTest.java` ou `TelemetryMarkerLint.java` por scripts bash — estes permanecem como Java tests conforme sua decisão original; Rule 25 apenas os catalogará como "Java test tier".
- NÃO implementar `/x-epic-implement` — esta decomposição produz apenas os artefatos de planejamento.
