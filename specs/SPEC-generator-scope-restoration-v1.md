# Prompt: Geração de Épico e Histórias — Generator Scope Restoration

> **Instrução de uso**: Execute `/x-epic-decompose` com este arquivo como especificação de entrada.
> Exemplo: `/x-epic-decompose specs/SPEC-generator-scope-restoration-v1.md`

---

## Sistema

**Projeto**: `ia-dev-environment` — CLI generator de ambientes de desenvolvimento assistidos por IA. Hoje apenas o target `claude-code` está implementado.

**Versão base analisada**: branch `fix/audit-remediation-p1b-tests-refactors`, ~pós-EPIC-0051.

**Objetivo desta especificação**: Restaurar o escopo original do CLI — um **gerador de pacote** que copia `.md` / `.sh` / `.yml` / `.json` de `java/src/main/resources/targets/claude/` para o projeto destino, resolvendo placeholders Pebble. Toda a infraestrutura Java que cresceu para suportar features de runtime (orquestração de release, checkpoint de execução, análise de paralelismo, reportagem de progresso, emissão/análise de telemetria, smoke framework, linter CI) deve ser **removida**. Features futuras do projeto entram como artefatos `.md` em `resources/targets/claude/`, nunca como código Java novo. Esta regra passa a ser uma **rule carregada em toda conversa** (`.claude/rules/21-generator-scope.md`).

**Princípio central de todas as histórias**: "O CLI é apenas um gerador. Código Java novo requer justificativa explícita." A partir deste épico, qualquer PR que adicionar pacote Java em `dev.iadev.*` sem justificativa documentada é rejeitado pela rule 21. Features do usuário final são skills, hooks, agents, knowledge packs, rules — todos `.md` copiados via Pebble.

---

## Escopo do Épico

### Contexto de negócio

O CLI `ia-dev-env` nasceu para copiar templates de configuração do Claude Code com substituição de placeholders. Com o tempo, 7 subsistemas Java foram incorporados ao JAR para suportar workflows de runtime (não de geração):

1. **`release/`** — SemVer, parser de Conventional Commits, orquestradores preflight/dryrun/handoff/resume/abort. Serve à skill `/x-release`.
2. **`checkpoint/`** — `ExecutionState`, `StoryEntry`, `TaskEntry`, persistência em `execution-state.json`. Serve às skills `/x-epic-implement`, `/x-story-implement`, `/x-task-implement`.
3. **`parallelism/`** — `CollisionDetector`, `FileFootprint`, `HotspotCatalog`. Serve à skill `/x-parallel-eval`.
4. **`progress/`** — `ProgressReporter`, `MetricsCalculator`. UX de execução longa.
5. **`telemetry/`** (Java side) — `TelemetryWriter`, `TelemetryScrubber`, `TelemetryAnalyzeCli`, `TelemetryTrendCli`, `PiiAudit`. Servem às skills de análise + gate CI.
6. **`smoke/`** — `ExpectedArtifactsGenerator` (main). Framework interno de regressão do CLI.
7. **`ci/`** — `TelemetryMarkerLint`. Gate CI para SKILL.md.

Cada um desses existe porque uma necessidade apareceu mid-flight e a decisão naquele momento foi "implementar em Java porque é mais rápido". O resultado: 15 pacotes Java quando o escopo real precisa de ~8, 5 entry points `main()` quando só 1 é a CLI real, e 22 Assemblers dos quais 19 são cópia pura e 3 acoplam o CLI a features de runtime.

**O custo** é triplo: (a) confusão cognitiva — desenvolvedor novo lendo `progress/MetricsCalculator.java` pergunta "por que isso está num gerador?"; (b) acoplamento invertido — skills `.md` chamam `java -cp ... dev.iadev.XxxCli`, quando a direção natural é LLM consome o JAR, não o contrário; (c) bloqueio para novas ferramentas-alvo — quando `cursor`, `codex` e outros targets forem ativados, esses 7 subsistemas criam divergência de comportamento por target.

### Dimensões de mudança

1. **Remoção de pacotes Java** — `release/`, `checkpoint/`, `parallelism/`, `progress/`, `telemetry/`, `smoke/`, `ci/` + testes espelho.
2. **Remoção de entry points `main()` extras** — `TelemetryAnalyzeCli`, `TelemetryTrendCli`, `PiiAudit`, `ExpectedArtifactsGenerator`.
3. **Reescrita de 7 skills** para LLM+bash (zero dependência de JVM além de ferramentas padrão como `jq`, `awk`, `git`, `gh`).
4. **Simplificação do `CicdAssembler`** — workflows gerados não referenciam mais classes Java removidas.
5. **Nova rule 21** — guardrail duradouro carregado em toda conversa.
6. **Atualização do `CLAUDE.md` raiz** — nota executiva apontando para a rule 21.

### Invariantes a preservar

- **Contrato do comando `generate`**: todas as 9 flags atuais (`-c/--config`, `-i/--interactive`, `-o/--output`, `-s/--stack`, `-v/--verbose`, `--dry-run`, `-f/--force`, `--overwrite-constitution`, `-p/--platform`) permanecem com mesmos defaults e comportamento.
- **18 stacks suportadas** em `ConfigProfiles.STACK_KEYS` permanecem: `java-picocli-cli`, `java-quarkus`, `java-spring`, `java-spring-clickhouse`, `java-spring-cqrs-es`, `java-spring-elasticsearch`, `java-spring-event-driven`, `java-spring-fintech-pci`, `java-spring-hexagonal`, `java-spring-neo4j`, `python-fastapi`, `python-fastapi-timescale`, `python-click-cli`, `go-gin`, `kotlin-ktor`, `typescript-nestjs`, `typescript-commander-cli`, `rust-axum`.
- **Fidelidade byte-a-byte dos Assemblers categoria A** — os 19 Assemblers classificados como cópia pura (abaixo) geram output idêntico aos golden files existentes em `src/test/resources/golden/`.
- **Hooks shell `.sh`** em `resources/targets/claude/hooks/` permanecem intactos. Os 5 hooks de telemetria (`telemetry-emit.sh`, `telemetry-lib.sh`, `telemetry-phase.sh`, etc.) continuam capturando NDJSON em `plans/epic-*/telemetry/events.ndjson` — só a **análise** desse NDJSON muda (Java → bash+LLM).
- **Rules 01–20** não são modificadas. Rule 21 é adicionada.

### Java — inventário fonte/destino

**Pacotes que permanecem** (marcados `[KEEP]`):

| Pacote | Papel | Classes principais |
|---|---|---|
| `cli/` | Picocli root + comandos | `IaDevEnvApplication`, `GenerateCommand`, `ValidateCommand`, `ConfigSourceLoader`, `ProjectConfigFactory`, `InteractivePrompter`, `ProjectSummaryFormatter`, `PlatformPrecedenceResolver`, `VerbosePipelineRunner` |
| `config/` | Carga YAML + prompts | `ConfigLoader`, `ContextBuilder`, `ContextArchitectureBuilder`, `ConfigProfiles` |
| `domain/` | Modelo de negócio | `ProjectConfig`, `StackValidator`, `StackResolver`, `PipelineResult`, `GenerateEnvironmentService` |
| `application/` | Pipeline de geração + 19 Assemblers categoria A | `AssemblerPipeline`, `AssemblerFactory`, todos os Assemblers listados abaixo como categoria A |
| `template/` | Engine Pebble | `TemplateEngine`, `TemplateEngineFactory`, `PythonBoolExtension`, `PythonBoolFilter` |
| `infrastructure/` | Adaptadores I/O | `FileSystemWriterAdapter`, `YamlStackProfileRepository`, `PebbleTemplateRenderer` |
| `exception/` | Exceções custom | Todas as exceções existentes exceto as referentes a pacotes removidos |
| `util/` | Helpers | `PathUtils`, `ResourceResolver`, `JarResourceExtractor`, `OverwriteDetector` |

**Pacotes que saem por completo** (marcados `[DELETE]`):

| Pacote | Motivo |
|---|---|
| `release/` | Feature de runtime consumida apenas pela skill `/x-release`; será reescrita em LLM+bash |
| `checkpoint/` | Feature de runtime consumida pelas skills `/x-*-implement`; `execution-state.json` passa a ser JSON leve manejado pela skill |
| `parallelism/` | Feature de runtime consumida pela skill `/x-parallel-eval`; heurística será aplicada pelo LLM |
| `progress/` | UX interna que cresceu de utilitário a subsistema |
| `telemetry/` | Emissão + análise. **Os hooks shell permanecem** e continuam escrevendo NDJSON. A análise Java (`TelemetryAnalyzeCli`, `TelemetryTrendCli`, `PiiAudit`, scrubber Java, whitelist Java) é removida |
| `smoke/` | Framework de regressão interno; testes unitários + de integração em `src/test/java/` cobrem os Assemblers sobreviventes |
| `ci/` | `TelemetryMarkerLint` desaparece junto com telemetria Java |

**Entry points `main()` extras** (todos `[DELETE]`):

- `telemetry.analyze.TelemetryAnalyzeCli.main`
- `telemetry.trend.TelemetryTrendCli.main`
- `telemetry.PiiAudit.main`
- `smoke.ExpectedArtifactsGenerator.main`

Apenas `cli.IaDevEnvApplication.main` sobrevive.

### Assemblers — classificação

**Categoria A (COPY) — 19 Assemblers `[KEEP]`**:

`ConstitutionAssembler`, `RulesAssembler`, `SkillsAssembler`, `AgentsAssembler`, `PatternsAssembler`, `ProtocolsAssembler`, `HooksAssembler`, `SettingsAssembler`, `DocsAssembler`, `DocsAdrAssembler`, `GrpcDocsAssembler`, `RunbookAssembler`, `IncidentTemplatesAssembler`, `OperationalRunbookAssembler`, `SloSliTemplateAssembler`, `DocsContributingAssembler`, `DataMigrationPlanAssembler`, `PlanTemplatesAssembler`, `ReadmeAssembler`.

**Categoria B — 3 Assemblers `[MODIFY]`**:

| Assembler | Ação |
|---|---|
| `CicdAssembler` | Simplificar: remover refs a `PiiAudit`, `TelemetryMarkerLint`, `ExpectedArtifactsGenerator` dos workflows/Dockerfiles gerados. Substituir por gates nativos de stack (`mvn verify`, `npm test`, etc.) |
| `EpicReportAssembler` | Auditar: se o payload é só template `.md` com placeholders `{{PLACEHOLDER}}` resolvidos pelo LLM em runtime, mantém. Remover se embute referência a classes Java removidas |
| `ReleaseChecklistAssembler` | Auditar: template `.md` de checklist de release. Mantém se for puro `.md`; simplifica se mencionar `x-release` de forma acoplada a Java |

### Skills — 130 totais

**123 skills intactas** — não tocam classes Java removidas. Nenhuma alteração.

**7 skills reescritas** — listagem completa, com referência ao arquivo:

| Skill | Arquivo | Reescrita |
|---|---|---|
| `x-telemetry-analyze` | `resources/targets/claude/skills/core/ops/x-telemetry-analyze/SKILL.md` | Invocação via `java -cp ... TelemetryAnalyzeCli` → bash com `jq` agregando `events.ndjson` + template Markdown preenchido pelo LLM |
| `x-telemetry-trend` | `resources/targets/claude/skills/core/ops/x-telemetry-trend/SKILL.md` | Idem: `jq` agregando múltiplos NDJSONs + heurística de comparação no LLM |
| `x-parallel-eval` | `resources/targets/claude/skills/core/plan/x-parallel-eval/SKILL.md` | Invocação via `java -cp ... ParallelEvalCli` → LLM lê blocos `## File Footprint` dos planos de story/task e aplica as regras do knowledge pack `parallelism-heuristics` sem JVM |
| `x-release` | `resources/targets/claude/skills/core/ops/x-release/SKILL.md` | Orquestrador Java → `git log`, `gh api`, `jq`, parsing de Conventional Commits em bash + decisões LLM. `.claude/state/release-state.json` é JSON leve manejado pela própria skill |
| `x-epic-implement` | `resources/targets/claude/skills/core/dev/x-epic-implement/SKILL.md` | `execution-state.json` deixa de ser validado por `checkpoint.*` e passa a ser JSON leve escrito/lido via Read/Write tools |
| `x-story-implement` | `resources/targets/claude/skills/core/dev/x-story-implement/SKILL.md` | Idem |
| `x-task-implement` | `resources/targets/claude/skills/core/dev/x-task-implement/SKILL.md` | Idem |

---

## Rule 21 — Draft Completo

Arquivo alvo: `java/src/main/resources/targets/claude/rules/21-generator-scope.md` (source of truth) e `.claude/rules/21-generator-scope.md` (output gerado).

Conteúdo obrigatório (a ser refinado pela history S1, mas este é o rascunho normativo):

```markdown
# Rule 21 — Generator Scope (RULE-GEN-SCOPE-01)

> **Ownership**: Platform Team. **Related**: CLAUDE.md raiz.

## 1. Rule

O projeto `ia-dev-environment` é **apenas um gerador**. Seu único papel é
copiar artefatos `.md` / `.sh` / `.yml` / `.json` de
`java/src/main/resources/targets/{platform}/` para o projeto destino,
resolvendo placeholders Pebble via `TemplateEngine`. Perfis YAML em
`resources/shared/config-templates/setup-config.*.yaml` governam a seleção
do conjunto de artefatos.

## 2. Escopo permitido de código Java

Apenas os seguintes pacotes existem em `dev.iadev.*`:

- `cli`, `config`, `domain`, `application`, `template`,
  `infrastructure`, `exception`, `util`.

Qualquer PR que adicionar um pacote fora dessa lista — incluindo
`release`, `checkpoint`, `parallelism`, `progress`, `telemetry`, `smoke`,
`ci`, ou novos pacotes análogos — DEVE ser rejeitado por review.

## 3. Como adicionar features

| Objetivo | Artefato | Localização |
|---|---|---|
| Novo comando para o LLM | Skill `.md` | `resources/targets/claude/skills/**` |
| Novo automation hook | Script `.sh` + entry em `settings.json` | `resources/targets/claude/hooks/**` |
| Nova persona LLM | Agent `.md` | `resources/targets/claude/agents/**` |
| Novo padrão de codificação | Knowledge pack `.md` | `resources/targets/claude/skills/knowledge-packs/**` |
| Nova regra do projeto gerado | Rule `.md` | `resources/targets/claude/rules/NN-name.md` |
| Novo template de plano/review | Template `.md` | `resources/shared/templates/_TEMPLATE-*.md` |
| Nova stack | Perfil YAML | `resources/shared/config-templates/setup-config.{stack}.yaml` |
| Nova plataforma-alvo (cursor, codex) | Diretório `resources/targets/{platform}/` + Assemblers categoria A | idem |

## 4. Quando código Java novo é aceitável

Apenas nos 8 pacotes `[KEEP]` acima, e SOMENTE quando:

- O caso de uso é **geração** (copiar + renderizar placeholders) ou
  **validação de geração** (StackValidator, ProjectConfig parsing).
- Não pode ser expresso como template Pebble + skill `.md`.
- O PR documenta a justificativa referenciando esta rule.

## 5. Forbidden

- Adicionar novo entry-point `main()` no JAR além de `cli.IaDevEnvApplication`.
- Criar classes `*Cli`, `*Orchestrator`, `*Engine` em pacotes fora dos 8 permitidos.
- Skills ou hooks que invoquem `java -cp` ou `java -jar` apontando para o JAR
  `ia-dev-env`.
- Templates que referenciem classes `dev.iadev.*` removidas.
```

---

## Critérios de Aceitação Globais

1. **Zero referência a pacotes removidos** em código Java ativo:
   ```bash
   rg -l 'dev\.iadev\.(release|checkpoint|parallelism|progress|smoke|ci)' \
      java/src/main/java java/src/test/java
   # Expected: nada (saída vazia)
   ```

2. **Zero referência a CLIs Java removidos** em skills + hooks:
   ```bash
   rg -l 'dev\.iadev\.telemetry\.(PiiAudit|TelemetryAnalyze|TelemetryTrend|analyze\.|trend\.)' \
      java/src/main/resources/targets/claude
   # Expected: nada
   ```

3. **Zero invocação de JVM em skills ou hooks**:
   ```bash
   rg -l 'java -(cp|jar)' java/src/main/resources/targets/claude/skills \
                          java/src/main/resources/targets/claude/hooks
   # Expected: nada
   ```

4. **Contrato do comando `generate` passa** para todas as 18 stacks:
   ```bash
   for s in <18 stacks>; do
     rm -rf /tmp/iadev-smoke
     java -jar target/ia-dev-env.jar generate --stack "$s" \
          -o /tmp/iadev-smoke --force --platform claude-code || echo "FAIL: $s"
   done
   # Expected: zero FAIL
   ```

5. **Golden files dos Assemblers categoria A permanecem byte-idênticos**:
   ```bash
   mvn -pl java test -Dtest='*Golden*'
   # Expected: BUILD SUCCESS
   ```

6. **Coverage ≥ 95% line, ≥ 90% branch** (Rule 05) no escopo restante:
   ```bash
   mvn -pl java verify
   ```

7. **Rule 21 carrega em conversa nova**:
   ```bash
   test -f .claude/rules/21-generator-scope.md
   grep -q '21-generator-scope' CLAUDE.md
   ```

8. **Nenhuma skill mencionada em §"Skills reescritas" invoca classe Java**:
   ```bash
   for skill in x-telemetry-analyze x-telemetry-trend x-parallel-eval \
                x-release x-epic-implement x-story-implement x-task-implement; do
     rg -l 'dev\.iadev\.' "java/src/main/resources/targets/claude/skills/**/$skill/SKILL.md"
   done
   # Expected: nada
   ```

---

## Histórias Sugeridas (DAG)

O decomposer tem liberdade para ajustar granularidade, mas deve preservar a ordem topológica abaixo:

### Fase 0 — Guardrail (bloqueia tudo)

**H1 — Adicionar rule 21 e atualizar CLAUDE.md**
- Criar `java/src/main/resources/targets/claude/rules/21-generator-scope.md` conforme §"Rule 21 — Draft Completo".
- Atualizar `CLAUDE.md` raiz com uma linha apontando para a rule 21.
- Regenerar output (`.claude/rules/21-generator-scope.md`).
- Atualizar golden files correspondentes.
- *Bloqueia*: H2–H9.

### Fase 1 — Reescrita de skills (3 histórias paralelas)

**H2 — Reescrever skills de análise de telemetria e paralelismo**
- Alvo: `x-telemetry-analyze`, `x-telemetry-trend`, `x-parallel-eval`.
- Substituir invocação `java -cp ...` por `jq`/`awk`/LLM reasoning.
- Manter comportamento observável (mesmos argumentos CLI, mesmo formato de saída Markdown).
- *Paralelo com*: H3, H4.

**H3 — Reescrever `x-release` em LLM+bash/git/gh**
- Alvo: `resources/targets/claude/skills/core/ops/x-release/SKILL.md`.
- Substituir orquestradores Java (`SemVer`, `ConventionalCommitsParser`, `preflight/*`, `dryrun/*`, `handoff/*`, `resume/*`, `abort/*`) por sequência `git log` + `gh api` + `jq` + parsing bash de Conventional Commits.
- Estado de release (`release-state.json`) passa a ser arquivo gerenciado pela própria skill via Read/Write.
- *Paralelo com*: H2, H4.

**H4 — Reescrever skills de implementação (epic/story/task)**
- Alvo: `x-epic-implement`, `x-story-implement`, `x-task-implement`.
- Remover dependência de `dev.iadev.checkpoint.*`.
- `execution-state.json` passa a ser JSON leve sem schema Java — a skill documenta o shape inline.
- *Paralelo com*: H2, H3.

### Fase 2 — Limpeza de entry points Java

**H5 — Deletar `main()` extras do JAR**
- Deletar classes `TelemetryAnalyzeCli`, `TelemetryTrendCli`, `PiiAudit`, `ExpectedArtifactsGenerator`.
- Verificar que `IaDevEnvApplication.main` permanece como único entry point no JAR buildado.
- *Depende de*: H2, H3 (as skills que usavam esses CLIs já não mais dependem deles).

### Fase 3 — Remoção de pacotes Java

**H6 — Deletar pacotes `release`, `checkpoint`, `parallelism`**
- Deletar `java/src/main/java/dev/iadev/{release,checkpoint,parallelism}/` + testes espelho em `java/src/test/java/dev/iadev/`.
- Remover referências em `application/` se houver (step classes do `CicdAssembler` podem referenciar — serão ajustadas em H8).
- *Depende de*: H3, H4 (as skills que usavam já foram reescritas).

**H7 — Deletar pacotes `progress`, `telemetry` (Java), `smoke`, `ci`**
- Deletar `java/src/main/java/dev/iadev/{progress,telemetry,smoke,ci}/` + testes.
- **Importante**: hooks shell `telemetry-*.sh` em `resources/targets/claude/hooks/` NÃO são tocados — são shell puro.
- Remover `TelemetryScrubber.java` e Rule 20 (privacidade de telemetria) se a rule ficou órfã — avaliar durante a execução. Provavelmente a Rule 20 continua relevante (privacidade dos `.ndjson` ainda importa) mas passa a apontar apenas para a regex shell.
- *Depende de*: H5, H2.

### Fase 4 — Ajuste de Assemblers

**H8 — Simplificar `CicdAssembler` e auditar 2 Assemblers categoria B**
- Remover do `CicdAssembler` e suas step-classes qualquer referência a `PiiAudit`, `TelemetryMarkerLint`, `ExpectedArtifactsGenerator` nos workflows/Dockerfiles gerados. Substituir por gates nativos (`mvn verify`, `npm test`, linters padrão da stack).
- Auditar `EpicReportAssembler` e `ReleaseChecklistAssembler`; se forem puro template `.md`, manter; se embutirem logic acoplada a Java removido, simplificar.
- *Depende de*: H6, H7.

### Fase 5 — Validação

**H9 — Smoke 18 stacks + refresh de golden files**
- Rodar `generate` para as 18 stacks, comparar com golden files.
- Atualizar golden files apenas onde mudança de saída é esperada (ex: workflows simplificados em H8).
- Rodar `mvn verify` full.
- Rodar as 8 validações do §"Critérios de Aceitação Globais".
- *Depende de*: todas anteriores; gate de merge.

---

## Out-of-scope

- Mudar o contrato da CLI `generate` (flags, defaults, comportamento).
- Adicionar suporte a novas plataformas (`cursor`, `codex`, `copilot`) — escopo de épico separado, facilitado por esta limpeza.
- Mudar rules 01–20.
- Remover ou alterar perfis YAML em `resources/shared/config-templates/`.
- Mudar hooks shell (`.sh`) em `resources/targets/claude/hooks/`.
- Refatorar Assemblers categoria A (19 itens).
- Mudar a engine Pebble ou introduzir template engine alternativo.
- Mexer em `java/src/main/java/dev/iadev/{cli,config,domain,template,infrastructure,exception,util}/` (exceto remoções incidentais de refs a pacotes deletados).

---

## Riscos e mitigação

- **R-A — Auto-dogfooding instável**: `/x-epic-implement` está sendo reescrito enquanto o épico seria executado. Mitigação: executar H1…H9 manualmente via `/x-story-implement` story-a-story; H9 valida o novo orchestrator contra um epic antigo já concluído como regressão.
- **R-B — Perda de dados históricos de telemetria**: NDJSONs em `plans/epic-*/telemetry/` permanecem. Só o analisador muda (Java → bash). Nada é apagado.
- **R-C — Regressão nos 18 stacks**: golden files são o teste de regressão. H9 atualiza apenas onde H8 mudou payload (workflows gerados).
- **R-D — Referências órfãs em documentação**: antes de H6/H7, `rg -r` em `specs/`, `adr/`, `plans/epic-*` por menções a classes removidas. Atualizar ou marcar como histórico.
- **R-E — Rule 20 (telemetry privacy)**: hoje aponta para `TelemetryScrubber` Java. Quando `telemetry/` Java sai, a Rule 20 precisa apontar para o scrubber shell (já existe no `telemetry-emit.sh`) ou ser removida. Decisão durante H7.

---

## Entregáveis esperados do `/x-epic-decompose`

O decomposer deve produzir:

1. `plans/epic-0052/EPIC-0052-generator-scope-restoration.md` — épico com contexto, story index, DoR/DoD.
2. `plans/epic-0052/stories/STORY-0052-XXXX-NNN-*.md` — 9 stories (uma por H1…H9), com `## File Footprint` obrigatório (RULE-004, RULE-008), I/O contracts (RULE-16), testability declaration (RULE-15).
3. `plans/epic-0052/IMPLEMENTATION-MAP.md` — DAG com as 5 fases e críticas de paralelismo (H2/H3/H4 paralelas; H6 e H7 paralelas dentro da fase 3).

---

## Notas para o decomposer

- Use `planningSchemaVersion = "2.0"` (task-first) para consistência com epics recentes (EPIC-0038+).
- Cada story deve declarar testability (`INDEPENDENT`, `REQUIRES_MOCK`, `COALESCED`) e I/O contracts verificáveis (RULE-16).
- As stories H6 e H7 são `INDEPENDENT` entre si mas dependem de H3/H4/H5. H2/H3/H4 são `INDEPENDENT` entre si.
- O épico NÃO deve criar novas tasks que adicionem código Java — apenas remover, reescrever `.md`, ajustar 3 Assemblers existentes.
- Critério de aceitação global deve ser replicado na DoD de cada story conforme aplicável.
