# Knowledge Packs fora de `.claude/skills/` — alternativas

## Contexto

Hoje o projeto `ia-dev-environment` trata Knowledge Packs (KPs) como "fake skills":

- Fonte da verdade: `java/src/main/resources/targets/claude/skills/knowledge-packs/` (~32 pastas, cada uma com `SKILL.md`).
- Saída gerada: `SkillsAssembler` achata tudo em `.claude/skills/` junto com skills reais (`x-*`).
- Cada KP usa `user-invocable: false` no frontmatter, o que filtra do menu `/`, mas Claude Code ainda escaneia o diretório.
- `.claude/skills/patterns/SKILL.md` é o pior caso: **não tem frontmatter algum** — é Markdown puro num arquivo cujo nome promete ser skill. Isso confunde leitura humana e ferramentas que validam o contrato de skill.

Motivação: KPs não são invocáveis, não têm triggers nem `allowed-tools` úteis. Estão ali só porque o Claude Code escaneia `.claude/skills/**/SKILL.md` e era o caminho mais rápido para disponibilizá-los via `Read`/`Skill` em tempo de execução. O custo: ruído no inventário, validação frágil, semântica errada.

## Alternativas (elegância crescente)

### A) Status quo + higienização mínima

- Manter em `.claude/skills/`, **exigir** frontmatter `user-invocable: false` em 100% dos KPs (corrigir `patterns/SKILL.md` que está sem).
- Adicionar validação no `SkillsAssembler` / teste de golden para reprovar KP sem frontmatter.
- **Prós:** mudança cirúrgica, zero impacto em skills que fazem `Read(".claude/skills/patterns/SKILL.md")`.
- **Contras:** continua "fake skill". Não resolve o incômodo semântico que você apontou.

### B) Subpasta dedicada dentro de `skills/` — `skills/_kp/{nome}/`

- Mover fonte para `skills/knowledge-packs/_kp/{nome}/` e gerar em `.claude/skills/_kp/{nome}/SKILL.md`.
- Prefixo `_` sinaliza "não é skill de usuário"; listagens humanas ficam visualmente separadas.
- **Prós:** baixo esforço, agrupa sem sair de `skills/`.
- **Contras:** ainda é SKILL.md; Claude Code ainda escaneia. Semântica errada persiste.

### C) **[Recomendada]** Mover para `.claude/knowledge/` (diretório próprio, fora de `skills/`)

Layout proposto:

```
.claude/
├── skills/              # somente skills invocáveis (x-*) + layer-templates
└── knowledge/           # knowledge packs (Markdown puro, sem contrato de skill)
    ├── architecture.md
    ├── patterns.md
    ├── security.md
    └── ...
```

Mudanças necessárias:

1. **Fonte da verdade** — renomear `java/.../targets/claude/skills/knowledge-packs/` → `targets/claude/knowledge/`; arquivos viram `{nome}.md` simples (sem subpasta, sem SKILL.md obrigatório). Remover frontmatter `user-invocable: false` (deixa de ser necessário — não é mais skill).
2. **Novo assembler** — `KnowledgeAssembler` (espelha `RulesAssembler`): copia verbatim para `.claude/knowledge/`. Remove responsabilidade correspondente do `SkillsAssembler` / `SkillsCopyHelper` (ver `java/src/main/java/dev/iadev/application/assembler/SkillsCopyHelper.java`).
3. **Referências em skills** — substituir em todos os `SKILL.md` de `core/`:
   `skills/patterns/SKILL.md` → `knowledge/patterns.md` (e análogos). É `grep -rln "skills/\(patterns\|architecture\|security\|testing\|...\)" java/src/main/resources/targets/claude/skills/core` + edição mecânica.
4. **Rules que apontam** — `.claude/rules/03-coding-standards.md`, `04-architecture-summary.md`, `05-quality-gates.md`, `06-security-baseline.md`, `07-operations-baseline.md`, `08-release-process.md`, `09-branching-model.md` todas citam `skills/{nome}/SKILL.md`. Atualizar ponteiros.
5. **CLAUDE.md** (project root) — atualizar a seção "Structure" e "Knowledge Packs, Agents, Hooks".
6. **Goldens** — regerar `src/test/resources/golden/` após `mvn process-resources` (per `feedback_mvn_process_resources_before_regen.md`).
7. **Mapa RULE-004 / EPIC-0041** — `KnowledgeAssembler.java` entra como novo hotspot; adicionar ao catálogo.

**Prós:**
- Semântica correta: knowledge não é skill.
- Claude Code não escaneia `.claude/knowledge/` — zero ruído em listagens / menu `/`.
- Permite validação independente (ex.: KP não precisa de frontmatter, não tem `allowed-tools`).
- Alinha com `.claude/rules/` (outro diretório plano de docs referenciados por path).

**Contras:**
- Mudança de superfície alta: ~30 skills fazem `Read` dos KPs por path. Precisa de varredura + edição em lote.
- Epic dedicado (estimar: 1 story de infra + 1 story de migração em massa + 1 story de regen de goldens).
- Invalida links históricos em ADRs e changelogs (aceitável — são registros de época).

### D) Plugin MCP de "resource server"

- Expor KPs como `resource://knowledge/patterns` via MCP server local.
- **Prós:** modelo nativo do protocolo para "dados sem ação".
- **Contras:** overengineering para arquivos estáticos; adiciona processo e latência; nenhuma skill atual consome via MCP.

### E) Referenciar via `skills/{skill}/references/*.md`

- Claude Code suporta `references/` dentro de uma skill. Mover cada KP para `references/` da skill primária que o consome.
- **Contras:** KPs são **compartilhados** (`architecture.md` é lido por `x-arch-plan`, `x-review-pr`, `x-story-plan`, etc.). Duplicação ou um "dono canônico" arbitrário. Pior que C.

## Recomendação

**Alternativa C** — `.claude/knowledge/` como diretório irmão de `skills/` e `rules/`. É a única opção que resolve o problema semântico de raiz sem adicionar infraestrutura. O custo de migração é contido num epic dedicado (migração mecânica + regen de goldens) e elimina uma classe inteira de confusão futura ("isto é skill ou doc?").

Como quick-win imediato enquanto o epic C não roda: aplicar **A** em `patterns/SKILL.md` — adicionar frontmatter `user-invocable: false` para pelo menos honrar o contrato mínimo dos outros KPs.

## Arquivos críticos (se avançar com C)

- `java/src/main/java/dev/iadev/application/assembler/SkillsAssembler.java`
- `java/src/main/java/dev/iadev/application/assembler/SkillsCopyHelper.java`
- `java/src/main/java/dev/iadev/application/assembler/KnowledgePackSelection.java`
- `java/src/main/java/dev/iadev/application/assembler/RulesAssembler.java` (modelo a espelhar)
- `java/src/main/resources/targets/claude/skills/knowledge-packs/**` (fonte a mover)
- `java/src/main/resources/targets/claude/skills/core/**/SKILL.md` (atualizar paths)
- `.claude/rules/0[3-9]-*.md` (atualizar ponteiros)
- `CLAUDE.md` (project root)
- `src/test/resources/golden/**` (regen obrigatório)

## Verificação ponta a ponta

1. `mvn process-resources && mvn test` — goldens batem, assemblers passam.
2. `mvn exec:java -Dexec.mainClass=...GoldenFileRegenerator` após edição para regerar baseline.
3. `ls .claude/skills/` não deve listar `patterns/`, `architecture/`, etc.; `ls .claude/knowledge/` lista os `.md`.
4. `grep -rn "skills/patterns/SKILL.md" java/src/main/resources/targets/claude/skills/core` retorna 0.
5. Invocar `/x-arch-plan` ou `/x-story-plan` num epic de teste e verificar que a skill lê `knowledge/architecture.md` sem erro.
6. Rodar `scripts/audit-interactive-gates.sh` e demais auditorias de Rule para garantir que nenhuma regex hard-coded no path antigo quebrou.
