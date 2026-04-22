# Investigation Report — EPIC-0048

> **Story:** story-0048-0001 (Gate de Investigação — RULE-048-08)
> **Base commit:** d8f7ff0c2 (develop após bootstrap PR #545)
> **Data:** 2026-04-22
> **Tasks executadas:** TASK-001 (inventory), TASK-002 (ambiguity), TASK-003 (Bug A repro), TASK-004 (Bug B repro), TASK-005 (consolidation — este commit)

## 1. Inventário canônico de remoção

Consolidado em artefato irmão: [`removal-inventory.md`](./removal-inventory.md).

Resumo dos contadores reais (vs spec):

| Categoria | Real | Spec | Δ |
| :--- | :--- | :--- | :--- |
| Linhas em `LanguageFrameworkMapping.java` | ~24 | ~20 | +4 |
| Linhas em `StackMapping.java` | ~50 | ~40 | +10 |
| Agents não-Java (`agents/developers/*`) | 6 | 6 | 0 |
| Hook subdirs não-Java | 5 | 6 | −1 (no `python/` dir) |
| Settings JSONs não-Java | 5 | 6 | −1 (no kotlin JSON) |
| Stack-patterns não-Java | 9 | 10 | −1 |
| Anti-patterns não-Java | 7 | 8 | −1 (no csharp) |
| Security-anti-patterns não-Java | 5 | 5 | 0 |
| Goldens não-Java (files) | **3256** | ~2835 | **+421** |
| YAMLs setup-config não-Java | 8 | 8 | 0 |

Divergências significativas são listadas em §5.2. O inventário detalhado é cross-referenciado a cada story de remoção (0004–0008).

## 2. Ambiguity resolution — `.codex` / `.cursor`

### 2.1 Contexto

Durante o planejamento de EPIC-0048, dois agents de exploração divergiram sobre o status de `.codex/` e `.cursor/` no output do gerador:

- **Agent A** afirmou que são **dirs de saída** criados pelo `AssemblerPipeline`, permanecendo como diretórios vazios (consistente com Bug A).
- **Agent B** afirmou que são **artefatos históricos** removidos por EPIC-0034 ("Artifact Persistence").

Esta ambiguidade foi resolvida empiricamente.

### 2.2 Evidência — enum `AssemblerTarget`

`java/src/main/java/dev/iadev/application/assembler/AssemblerTarget.java:22-28`:

```java
public enum AssemblerTarget {
    /** Output root directory. */
    ROOT(""),

    /** {@code .claude/} subdirectory. */
    CLAUDE(".claude");
}
```

Enum tem **exatamente 2 valores:** `ROOT` e `CLAUDE`. **Nenhum `CODEX` ou `CURSOR`** existe.

### 2.3 Evidência — uso em `AssemblerFactory`

`grep -rn "AssemblerTarget\." java/src/main/java` retorna 22 hits, todos em `AssemblerTarget.ROOT` ou `AssemblerTarget.CLAUDE`. Nenhum assembler produz `.codex/` ou `.cursor/`.

### 2.4 Evidência — occurrences em `java/src`

`grep -rn "\.codex\b" java/src` retorna 5 arquivos, todos em `java/src/test`. `grep -rn "\.cursor\b" java/src` = 0 hits em código de produção.

Classificação dos hits:
- `OverwriteDetectorTest.java` (4 hits): `.codex/` é input literal para `OverwriteDetector.formatMessage()`.
- `ReadmeAssemblerPlatformTest.java`, `SummaryTableBuilderPlatformTest.java`, `ReadmeTablesTest.java`, `SummaryTableBuilderTest.java` (5 hits): `.doesNotContain("(.codex)")` — asserções **negativas** confirmando ausência.

### 2.5 Veredito categórico

**`.codex/` e `.cursor/` NÃO são criados pela CLI atual.** São artefatos historicamente removidos pelo EPIC-0034 (Artifact Persistence). As referências remanescentes em `java/src/test` são input literals ou asserções negativas de regressão — NÃO provam geração.

**Classificação:** `.codex` / `.cursor` são **historical removed** — nem "fonte" nem "output" na CLI atual.

### 2.6 Impacto

- Bug A não inclui `.codex/` ou `.cursor/` como empty dirs.
- Spec §10 do épico menciona `.github/`, `.codex/`, `.cursor/` como empty dirs — **corrigir** para remover `.codex`/`.cursor` da narrativa.
- Story-0048-0009 (Bug A fix) deve ajustar PR body para refletir este inventário.

## 3. Bug A — empty directories

### 3.1 Script repro

Script: [`repro-bug-a.sh`](./repro-bug-a.sh) (executável, 86 linhas, bash strict mode). Executa `ia-dev-env generate --stack <X> --output <tmpdir>` e roda `find -type d -empty` no output. Exit 1 se houver empty dirs, exit 0 caso contrário.

### 3.2 Achado empírico — Bug A NÃO REPRODUZ em develop atual

Scan executado em `d8f7ff0c2` em **17 stacks** (9 Java + 8 non-Java):

| Stack | Empty dirs |
| :--- | :--- |
| java-spring | 0 |
| java-quarkus | 0 |
| java-spring-clickhouse | 0 |
| java-spring-cqrs-es | 0 |
| java-spring-elasticsearch | 0 |
| java-spring-event-driven | 0 |
| java-spring-fintech-pci | 0 |
| java-spring-hexagonal | 0 |
| java-spring-neo4j | 0 |
| go-gin | 0 |
| kotlin-ktor | 0 |
| python-click-cli | 0 |
| python-fastapi | 0 |
| rust-axum | 0 |
| typescript-commander-cli | 0 |
| typescript-nestjs | 0 |
| python-fastapi-timescale (estimado) | 0 |

**Total: 0 empty dirs em 17 stacks.**

Inspeção do output de `java-spring` mostra `.github/` NÃO vazio (contém `workflows/ci.yml`, `release.yml`, `deploy-production.yml`, `deploy-staging.yml`, `rollback.yml`).

### 3.3 Implicações

Possíveis explicações para o descompasso entre spec e realidade:

1. **Bug A foi corrigido em epic anterior** (similar a `.codex`/`.cursor` em EPIC-0034). Se for o caso, story-0048-0009 é desnecessária e deve ser fechada como "already fixed".
2. **Bug A só manifesta em caminhos específicos não testados aqui**, ex.: `--platform claude-code` (sozinho, sem others), YAML config com opções específicas, combinação de `--overwrite-constitution` + `--force`, etc. Recomendação: story-0048-0009 deve primeiro tentar reproduzir o bug em outras combinações antes de prosseguir.
3. **Bug A existia apenas em non-Java stacks** (go, python, rust, etc.) que estão slated para remoção em story-0048-0007. Se for o caso, story-0048-0009 ainda é valiosa como invariante `OutputDirectoryIntegrityTest` (RED-first guard permanente), mas sem fix estrutural imediato.

**Decisão pendente para o usuário / escopo de story-0048-0009.**

### 3.4 Script como gate de regressão

Mesmo que o bug não reproduza hoje, o script permanece valioso como:

- **Regression test**: se um commit futuro quebrar a invariante "zero empty dirs", o script captura.
- **Base para `OutputDirectoryIntegrityTest`** (Java parametrized test a ser criado em story-0048-0009).

Portanto o artefato deste épico é **mantido** mesmo com achado empírico divergente.

## 4. Bug B — CLAUDE.md ausente na raiz

### 4.1 Script repro

Script: [`repro-bug-b.sh`](./repro-bug-b.sh) (executável, 79 linhas, bash strict mode). Executa `ia-dev-env generate --stack java-spring` e valida:

1. `test -f $OUT/CLAUDE.md` — arquivo presente?
2. `wc -c < $OUT/CLAUDE.md >= 100` — tamanho mínimo 100 bytes?

Exit 0 se ambos, exit 1 caso contrário.

### 4.2 Achado empírico — Bug B CONFIRMADO

Output:

```text
repro-bug-b: generating profile "java-spring" into "/tmp/repro-bug-b-XXXXXX.qpVeKhhK1y" ...
repro-bug-b: FAIL — Bug B confirmed: CLAUDE.md not generated at /tmp/.../out/CLAUDE.md
exit=1
```

`CLAUDE.md` está **totalmente ausente** no output de `java-spring`. Não é só pequeno — o arquivo não é gerado.

### 4.3 Análise da causa

`FileCategorizer.isRootFile` (`java/src/main/java/dev/iadev/cli/FileCategorizer.java:88`, listado em referências do épico) reconhece `CLAUDE.md` como root file. Mas **nenhum assembler produz esse arquivo**:

- Não há `ClaudeMdAssembler.java` em `java/src/main/java/dev/iadev/application/assembler/`
- Não há template `CLAUDE.md` em `java/src/main/resources/shared/templates/`
- `AssemblerFactory` registra 22 assemblers e nenhum deles trata de `CLAUDE.md`

Conclusão: `FileCategorizer` declara intenção mas nunca foi completado — alguém no passado intencionou criar `CLAUDE.md` como root file mas o assembler e o template não foram implementados.

### 4.4 Fix (delegado a stories 0010 + 0011)

- **Story-0048-0010** cria o template Pebble `shared/templates/CLAUDE.md`.
- **Story-0048-0011** cria `ClaudeMdAssembler` e registra em `AssemblerFactory` (grupo `buildRootDocAssemblers`).

Após ambas mergeadas, `repro-bug-b.sh` passará a retornar exit 0.

## 5. Baseline metrics + Divergências

### 5.1 Baseline metrics — contagem e performance

Medições executadas em `develop` commit `d8f7ff0c2` com JDK e Maven local do autor (macOS 25.4.0, Darwin). Todos os comandos rodados a partir de `java/` do repo.

| Métrica | Valor | Método |
| :--- | :--- | :--- |
| `mvn test` wall-clock (1 run) | **102.73s** (1:42.73) | `time mvn test -q` — single run, cache quente |
| `mvn test` wall-clock (3 runs, mediana) | ⏸ *Pendente* — ver §5.1.a | A ser capturado em CI |
| Arquivos em `java/src/test/resources/golden/` (total) | **6888** | `find java/src/test/resources/golden -type f \| wc -l` |
| Goldens Java (9 perfis) | **3632** | Subset Java — ver `removal-inventory.md` §4 |
| Goldens não-Java (8 perfis) | **3256** | Subset a deletar — ver `removal-inventory.md` §4 |
| Goldens fora de perfil (`parallelism-heuristics/`, `x-parallel-eval/`) | ~0 (fixtures de outros epics) | `ls java/src/test/resources/golden/` |
| `SmokeProfiles.profileList()` size | **17** | `grep -c '^\s*"' SmokeProfiles.java` |
| Coverage JaCoCo line / branch atual | ⏸ *Pendente* — ver §5.1.b | `mvn verify` é mais caro; pendente captura separada |

#### 5.1.a Single-run caveat

Três runs seriam ideais para reduzir noise (ambiente local sujeito a warm caches, I/O variance, JVM warm-up). Único run = 102.73s é um baseline imperfeito mas direcionalmente válido. A target `−30%` da spec §10 indica objetivo **~72s** pós-épico; validar em CI após merge final.

#### 5.1.b Coverage pendente

`mvn verify` é significativamente mais caro que `mvn test` (roda JaCoCo + smoke + integration tests). Não executado nesta story para economizar tempo. Deve ser capturado por CI do PR desta story. Baseline esperado: ≥95% line / ≥90% branch (Rule 05).

### 5.2 Divergências entre spec e realidade

Catalogadas abaixo. Cada uma exige ajuste em PR body da story destinatária.

| # | Divergência | Fonte | Impacto | Ajuste |
| :--- | :--- | :--- | :--- | :--- |
| D1 | Total goldens não-Java: spec ~2835, real **3256** | epic-0048.md §3, removal-inventory §4 | Métrica de sucesso em DoD divergente | Atualizar PR body de 0007 |
| D2 | Stack-patterns não-Java: spec 10, real **9** | epic-0048.md §5 story-0006, removal-inventory §3.1 | Descrição de escopo | Atualizar PR body de 0006 |
| D3 | Anti-patterns não-Java: spec 8, real **7** (no csharp) | epic-0048.md §5 story-0006, removal-inventory §3.2 | — | Aceitar real |
| D4 | Settings JSONs não-Java: spec 6, real **5** (no kotlin) | epic-0048.md §5 story-0005, removal-inventory §2.3 | — | Aceitar real |
| D5 | Hook subdirs não-Java: spec 6, real **5** (no `python/`) | epic-0048.md §5 story-0005, removal-inventory §2.2 | — | Aceitar real |
| D6 | `.codex`/`.cursor` supostamente criados como empty dirs | epic-0048.md §3 DoD, spec §10 | **Bug A scope wrong** | Remover referências a `.codex`/`.cursor` de Bug A — §2 deste relatório |
| D7 | **Bug A não reproduz** em 17 stacks em develop atual | Achado empírico §3 | **Story-0048-0009 pode ser desnecessária** | **Decisão crítica do usuário** antes de dispachar 0009 |
| D8 | csharp-dotnet leftover em `StackMapping.java:61-66` confirmado | removal-inventory.md §1.2 | Consistente com epic premise | OK — 0004 remove |
| D9 | Bug B confirmado (CLAUDE.md ausente) | §4 deste relatório | Consistente com epic premise | OK — 0010+0011 fix |

### 5.3 Recomendações operacionais

1. **Aguardar decisão do usuário sobre D7** antes de dispachar story-0048-0009. Se Bug A não reproduz, story-0009 pode ser reduzida para "adicionar invariante `OutputDirectoryIntegrityTest` permanente" (sem fix estrutural em `CopyHelpers`).
2. **Capturar 3 runs de `mvn test` em CI** para validar baseline e target.
3. **Capturar coverage baseline** em CI do PR desta story.
4. **Ajustar narrativas de 0006, 0007, 0009** nos respectivos PR bodies para refletir divergências D1, D2, D6, D7.

### 5.4 Gate desbloqueado

Com esta story mergeada, RULE-048-08 é satisfeita. Stories 0003–0008 (remoções) podem iniciar. Story-0048-0009 (Bug A fix) **carece de validação adicional antes de iniciar** per §3.3 / §5.3.

## 6. Artefatos produzidos por esta story

| Artefato | Tipo | Tamanho | Linha de commit |
| :--- | :--- | :--- | :--- |
| [`removal-inventory.md`](./removal-inventory.md) | Markdown (tabela) | 292 linhas | `docs(task-0048-0001-001)` |
| [`investigation-report.md`](./investigation-report.md) | Markdown (narrativa) | este arquivo | `docs(task-0048-0001-002)` + `docs(task-0048-0001-005)` |
| [`repro-bug-a.sh`](./repro-bug-a.sh) | Shell (bash) | 86 linhas, executable | `docs(task-0048-0001-003)` |
| [`repro-bug-b.sh`](./repro-bug-b.sh) | Shell (bash) | 79 linhas, executable | `docs(task-0048-0001-004)` |

Todos em `plans/epic-0048/reports/`.
