# Spec — EPIC-0051: Knowledge Packs fora de `.claude/skills/`

> **Idioma:** pt-BR no corpo narrativo; termos técnicos e identificadores em inglês.
> **Status:** Draft — input para `/x-epic-decompose`.
> **Branch:** `feat/epic-0051-knowledge-packs-dir`
> **Worktree:** `.claude/worktrees/epic-0051/`

## 1. Problema

Hoje o projeto `ia-dev-environment` armazena **Knowledge Packs** (KPs) dentro de
`.claude/skills/{nome}/SKILL.md`, lado a lado com skills invocáveis (`x-*`). Isso
cria três problemas:

1. **Semântica errada.** Um KP não é skill: não tem triggers, não é invocável, não
   tem `allowed-tools` úteis. Está em `skills/` apenas porque Claude Code escaneia
   `.claude/skills/**/SKILL.md` e esse foi o caminho mais rápido para
   disponibilizá-los via `Read`/`Skill` em runtime.
2. **Contrato frágil.** A maioria dos KPs declara `user-invocable: false` no
   frontmatter, mas nem todos. Exemplo concreto: `.claude/skills/patterns/SKILL.md`
   é **Markdown puro sem frontmatter algum**, violando o contrato mínimo que os
   demais KPs honram.
3. **Ruído no inventário.** A listagem `/` de skills do Claude Code filtra
   `user-invocable: false`, mas a pasta fica poluída; humanos lendo `ls .claude/skills/`
   veem ~32 pastas "skills" que não são skills; assemblers e auditorias precisam
   reconhecer o caso especial em múltiplos pontos.

## 2. Escopo

Mover Knowledge Packs para um diretório irmão dedicado, `.claude/knowledge/`
(relativo à raiz do repositório), e ajustar toda a cadeia (source-of-truth,
assembler, skills consumidoras, rules, goldens) para refletir o novo path.

### 2.1 Fora de escopo

- Alterar o **conteúdo** dos KPs (somente mover arquivos e atualizar paths).
- Alterar a API pública de qualquer skill `x-*`.
- Criar um plugin MCP ou resource server (explicitamente rejeitado como
  overengineering — ver seção 7).

## 3. Estado atual (source of truth)

- **Fonte:** `java/src/main/resources/targets/claude/skills/knowledge-packs/` com
  ~32 subdiretórios (ex.: `architecture/`, `patterns/`, `security/`, `testing/`,
  `owasp-asvs/`, `pci-dss-requirements/`, `stack-patterns/`, `infra-patterns/`…).
  Cada KP é um `SKILL.md` com frontmatter `user-invocable: false` (quando honrado).
- **Assembler:** `SkillsAssembler.java` + `SkillsCopyHelper.java` +
  `KnowledgePackSelection.java` em `java/src/main/java/dev/iadev/application/assembler/`.
- **Saída gerada:** `.claude/skills/{nome}/SKILL.md` (achatado junto com skills reais).
- **Consumidores:** ~30 skills em `java/src/main/resources/targets/claude/skills/core/**/SKILL.md`
  fazem `Read(".claude/skills/{nome}/SKILL.md")` ou citam esse path em prosa.
- **Rules que apontam:** `03-coding-standards.md`, `04-architecture-summary.md`,
  `05-quality-gates.md`, `06-security-baseline.md`, `07-operations-baseline.md`,
  `08-release-process.md`, `09-branching-model.md` — todas citam
  `skills/{nome}/SKILL.md` como referência de leitura obrigatória.
- **Docs:** `CLAUDE.md` (project root) e `.claude/README.md` descrevem a estrutura
  atual.

## 4. Estado-alvo

```
.claude/
├── agents/
├── hooks/
├── rules/               # docs estáticos, referenciados por path (inalterado)
├── skills/              # APENAS skills invocáveis (x-*) + layer-templates
└── knowledge/           # NOVO — knowledge packs como Markdown puro
    ├── architecture.md
    ├── patterns.md
    ├── security.md
    ├── testing.md
    ├── owasp-asvs.md
    └── ...
```

### 4.1 Contrato do novo diretório

- Cada KP é **um arquivo `.md`** (não pasta). Nomes em kebab-case (ex.:
  `architecture-patterns.md`).
- Frontmatter **opcional**. Quando presente, aceita apenas `name`, `description`
  e `tags` (sem `user-invocable`, sem `allowed-tools` — não é skill).
- Claude Code **não escaneia** `.claude/knowledge/`. Discovery é 100% por path
  explícito via `Read`.
- KPs com bundled references (hoje em `skills/{nome}/references/*.md`) viram
  `.claude/knowledge/{nome}/` (pasta) ou são inlined no `.md` principal — decisão
  por KP.

## 5. Regras de negócio

### R1. Source-of-truth única

A fonte da verdade para KPs passa a ser
`java/src/main/resources/targets/claude/knowledge/` (novo). O antigo
`…/skills/knowledge-packs/` é removido após a migração. Nenhum KP pode existir
em ambos os locais simultaneamente (detectado por teste).

### R2. Assembler dedicado

Um novo `KnowledgeAssembler.java` (espelha a simplicidade de `RulesAssembler.java`)
copia verbatim de `targets/claude/knowledge/` para `.claude/knowledge/`. O
`SkillsAssembler` **não** deve mais conhecer KPs.

### R3. Path canônico em skills

Toda skill que hoje lê `skills/{nome}/SKILL.md` passa a ler
`knowledge/{nome}.md` (ou `knowledge/{nome}/index.md` se for pasta com references).
A migração é mecânica e auditada por grep.

### R4. Rules atualizadas

As rules que citam `skills/{nome}/SKILL.md` como "Full reference" passam a citar
`knowledge/{nome}.md`. Isso afeta as 7 rules listadas em §3.

### R5. Goldens regerados

Após a migração, `GoldenFileRegenerator` deve ser executado (ver
`reference_golden_regen_command.md` em memory) para atualizar
`src/test/resources/golden/**`. Antes: `mvn process-resources`.

### R6. Backward-compat mínima

Não há camada de compatibilidade por path antigo. A migração é atômica dentro do
epic (big-bang por PR de epic). Branches ou PRs abertos que referenciem o path
antigo serão rejeitados no rebase — aceitável, pois EPIC-0051 é infra.

### R7. CLAUDE.md e README

`CLAUDE.md` (root) e `.claude/README.md` devem descrever a nova estrutura. A
seção "Structure" em ambos é atualizada, e o bloco "Knowledge Packs, Agents,
Hooks" em `.claude/README.md` é reescrito para refletir que KPs agora vivem em
`.claude/knowledge/` e não são mais pseudo-skills.

### R8. EPIC-0041 (parallelism)

O novo `KnowledgeAssembler.java` entra como hotspot em Rule 04 / EPIC-0041. A
planilha de hotspots em `plans/epic-0041/` (ou equivalente) deve listá-lo para
que futuros epics que mexam em assemblers sigam as regras de paralelismo.

## 6. Critérios de aceite (alto nível)

- [ ] `.claude/skills/` não lista mais nenhum dos ~32 nomes de KP (ex.:
      `patterns/`, `architecture/`, `security/`, `testing/` etc.).
- [ ] `.claude/knowledge/` existe e contém todos os KPs em formato `.md`.
- [ ] `grep -rln "skills/\(patterns\|architecture\|security\|testing\|protocols\|resilience\|observability\|coding-standards\|layer-templates\|data-management\|feature-flags\|release-management\|ci-cd-patterns\|dockerfile\|infrastructure\|sre-practices\|story-planning\|performance-engineering\|disaster-recovery\|api-design\|compliance\|database-patterns\)\b" java/src/main/resources/targets/claude/skills/core`
      retorna 0 resultados.
- [ ] `mvn test` verde (todos os assembler tests + goldens atualizados).
- [ ] `scripts/audit-interactive-gates.sh` e demais auditorias passam.
- [ ] `ia-dev-env generate` rodado num projeto novo produz o layout novo
      sem warnings.
- [ ] `.claude/rules/0[3-9]-*.md` apontam para `knowledge/{nome}.md`.
- [ ] `CLAUDE.md` e `.claude/README.md` refletem a nova estrutura.
- [ ] ADR criado documentando a decisão (nome sugerido: ADR-00XX-knowledge-packs-dedicated-dir).

## 7. Alternativas consideradas e rejeitadas

Registradas integralmente em [`discovery-note.md`](./discovery-note.md)
(contexto de descoberta do problema, committed in-repo para archaeology).
Resumo:

- **A (status quo + higienização)** — não resolve o problema semântico.
- **B (subpasta `_kp/` dentro de `skills/`)** — ainda é SKILL.md; Claude Code
  ainda escaneia.
- **D (plugin MCP)** — overengineering para arquivos estáticos.
- **E (`references/` dentro de cada skill dona)** — duplicação, pois KPs são
  compartilhados por múltiplas skills.

A alternativa escolhida é **C**: diretório dedicado `.claude/knowledge/` irmão
de `skills/` e `rules/`.

## 8. Decomposição sugerida (input para `/x-epic-decompose`)

> A skill decidirá a decomposição final. Abaixo é apenas sugestão de camadas
> lógicas para orientar a quebra em stories:

1. **Foundation / Domain** — novo `KnowledgeAssembler` + contrato de path +
   testes unitários isolados.
2. **Source-of-truth migration** — mover arquivos de
   `targets/claude/skills/knowledge-packs/` para `targets/claude/knowledge/` e
   normalizar formato (frontmatter opcional, nome em kebab-case).
3. **Skill consumers retrofit** — atualizar em massa os ~30 `SKILL.md` em
   `core/**` que citam `skills/{nome}/SKILL.md`.
4. **Rules retrofit** — atualizar as 7 rules que apontam para os KPs antigos.
5. **Docs + README** — `CLAUDE.md` (root) + `.claude/README.md` +
   `.claude/skills/README.md` (se existir).
6. **Goldens + smoke tests** — regenerar goldens, adicionar `KnowledgePackSmokeIT`
   que valida que `.claude/knowledge/` existe e contém os arquivos esperados.
7. **Cleanup** — remover lógica de KP do `SkillsAssembler` / `SkillsCopyHelper`
   / `KnowledgePackSelection` (ou renomear para o novo fluxo).
8. **ADR + CHANGELOG** — ADR documentando a decisão; CHANGELOG com entrada
   `Changed` descrevendo a quebra de path.

## 9. Riscos

| # | Risco | Mitigação |
|---|-------|-----------|
| R1 | Skill referencia KP por path antigo e quebra em runtime. | Auditoria grep obrigatória no PR final + smoke test que invoca `/x-arch-plan` num epic de teste. |
| R2 | Goldens dessincronizam. | Story dedicada à regeneração com `mvn process-resources` antes. |
| R3 | Plano de epic concorrente (ex.: EPIC-0052/0053 ativos) colide em hotspots do RULE-004. | Rodar `/x-parallel-eval --scope=epic` antes de abrir PR; se colidir, degradar para serial. |
| R4 | Usuários com projetos gerados por versões antigas do `ia-dev-env` têm layout antigo. | Release note explícita; `ia-dev-env generate` regenera tudo. Não implementamos migrador automático. |
| R5 | Perda de informação durante o flatten de `references/*.md` de alguns KPs (ex.: `patterns/references/`). | Decisão por KP durante story 2: ou inline no `.md` principal, ou virar subpasta `knowledge/{nome}/`. Registrar em ADR. |

## 10. Fora de escopo (reiterado)

- Não introduzimos MCP resource server.
- Não criamos camada de compatibilidade por path antigo.
- Não alteramos conteúdo de nenhum KP.
- Não tocamos na API pública das skills `x-*`.

## 11. Referências

- Nota de descoberta: [`discovery-note.md`](./discovery-note.md)
- Rule 04: `.claude/rules/04-architecture-summary.md`
- Rule 13: `.claude/rules/13-skill-invocation-protocol.md`
- EPIC-0041 (hotspots de paralelismo): `plans/epic-0041/`
- ADR-0006 (File-Conflict-Aware Parallelism): `.claude/adr/ADR-0006-file-conflict-aware-parallelism.md`
