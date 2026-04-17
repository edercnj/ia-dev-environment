# Spec — EPIC-0048: Java-Only Generator + Correção de Bugs A e B

**Autor:** Eder Celeste Nunes Junior
**Data:** 2026-04-16
**Versão:** 1.0
**Status:** Em Refinamento
**Target Epic ID:** EPIC-0048

## 1. Overview

O gerador `ia-dev-env` neste repositório (`<repo-root>`) suporta hoje 6 linguagens de programação (java, python, go, kotlin, typescript, rust) com suporte parcial a csharp/.NET. Na prática 100% do uso é Java e a manutenção multi-linguagem gera custo constante: ~2.835 arquivos golden não-Java, 25+ smoke tests parametrizados por 17 perfis, duplicação de agents/hooks/rules/knowledge-packs por linguagem e débito técnico acumulado (`csharp-dotnet` leftover em `StackMapping` sem perfil/golden).

Simultaneamente, dois bugs foram identificados:

- **Bug A — Pastas vazias no output real da CLI**: `ia-dev-env generate` cria diretórios como `.github/`, `.codex/`, `.cursor/` vazios no projeto gerado. Confirmado pelo usuário em execução real de CLI em projeto alvo (não é golden contamination). Raiz provável: `CopyHelpers.copyDirectory#preVisitDirectory` em `java/src/main/java/dev/iadev/application/assembler/CopyHelpers.java` chama `Files.createDirectories` antes de saber se haverá conteúdo útil no destino.
- **Bug B — CLAUDE.md raiz não é gerado**: `FileCategorizer.isRootFile` (`java/src/main/java/dev/iadev/cli/FileCategorizer.java:88`) reconhece `CLAUDE.md` como arquivo raiz, mas nenhum assembler o produz. Por contrato, todo projeto Claude-Code gerado deveria ter um `CLAUDE.md` executivo auto-loaded (conforme CLAUDE.md raiz deste repo, linhas 13-14).

Este épico entrega três coisas:
1. **Escopo Java-only**: remover python, go, kotlin, typescript, rust, csharp de todos os mapeamentos, templates, skills, rules, goldens, smoke tests e YAMLs. Bancos, mensageria, padrões de arquitetura, interface types e compliance **permanecem intactos** (são dimensões ortogonais).
2. **Fix Bug A**: teste invariante `OutputDirectoryIntegrityTest` (RED-first) + fix estrutural em `CopyHelpers` + `pruneEmptyDirs` em `AssemblerPipeline` (pós-assembly) + regeneração dos 9 goldens Java.
3. **Fix Bug B**: novo `ClaudeMdAssembler` dedicado (decisão arquitetural confirmada: single-responsibility), template Pebble `shared/templates/CLAUDE.md` com placeholders, registro em `AssemblerFactory` como último grupo `buildRootDocAssemblers`, atualização controlada dos 9 goldens Java com o novo arquivo.

**Fora de escopo:** introdução de novas linguagens; revamp de pipeline de assembly; telemetria; mudanças em rules de arquitetura/compliance/databases/mensageria; renomear classes/pacotes; alterar configuração JaCoCo thresholds.

**Breaking change**: release **v4.0.0** (MAJOR). Usuários que precisam de python/go/kotlin/typescript/rust devem pinar v3.x. Branch `legacy/v3` mantida read-only.

## 2. Decisões Consolidadas (respostas do autor no planning)

| Ponto | Decisão | Justificativa |
|---|---|---|
| Kotlin | Remover também | Tratado como linguagem separada (não "Java-like"), apesar de reutilizar `java-gradle.json` em `SETTINGS_LANG_MAP`. |
| Bug A origem | Output real da CLI | Confirmado pelo autor. Fix estrutural, não patch em golden. |
| CLAUDE.md generation | Novo `ClaudeMdAssembler` dedicado | Single-responsibility; evita misturar com `ReadmeAssembler` (que escreve em `AssemblerTarget.CLAUDE` para `.claude/README.md`). Mais testável. |
| Release strategy | 1 PR por story, squash-merge em `develop`, v4.0.0 no final | Bisect-able (Rule 18); rollback granular; Git Flow (Rule 09); 13 PRs pequenos. |

## 3. Referências (arquivos críticos)

**Código fonte (modificar):**
- `java/src/main/java/dev/iadev/cli/LanguageFrameworkMapping.java` — LANGUAGES, FRAMEWORKS, BUILD_TOOLS, DEFAULT_VERSIONS, FRAMEWORK_VERSIONS, ARCH_PATTERN_LANGUAGES
- `java/src/main/java/dev/iadev/cli/GenerateCommand.java` — validação early de `--language`
- `java/src/main/java/dev/iadev/cli/InteractivePrompter.java` — short-circuit quando lista tem 1 item
- `java/src/main/java/dev/iadev/cli/FileCategorizer.java` — já reconhece CLAUDE.md; confirmar
- `java/src/main/java/dev/iadev/domain/stack/StackMapping.java` — LANGUAGE_COMMANDS, FRAMEWORK_LANGUAGE_RULES, DOCKER_BASE_IMAGES, HOOK_TEMPLATE_MAP, SETTINGS_LANG_MAP, FRAMEWORK_PORTS, FRAMEWORK_HEALTH_PATHS, INTERFACE_SPEC_PROTOCOL_MAP; remover csharp-dotnet leftover
- `java/src/main/java/dev/iadev/domain/stack/StackResolver.java` — remover detectores go.mod, package.json, pyproject.toml, Cargo.toml, *.csproj, build.gradle.kts
- `java/src/main/java/dev/iadev/domain/stack/StackValidator.java` — limpar constantes de versão não-java
- `java/src/main/java/dev/iadev/application/assembler/AssemblerFactory.java` — registrar novo ClaudeMdAssembler em grupo buildRootDocAssemblers
- `java/src/main/java/dev/iadev/application/assembler/CopyHelpers.java` — fix Bug A
- `java/src/main/java/dev/iadev/application/assembler/AssemblerPipeline.java` — chamar pruneEmptyDirs pós-assembly
- `java/src/main/java/dev/iadev/application/assembler/HooksAssembler.java`, `SettingsAssembler.java`, `AgentsAssembler.java`, `SkillsAssembler.java`, `SkillsCopyHelper.java`, `FrameworkKpWriter.java`, `LanguageKpWriter.java`, `AntiPatternsRuleWriter.java`, `SecurityAntiPatternsRuleWriter.java` — remover ramos não-Java

**Código fonte (criar):**
- `java/src/main/java/dev/iadev/application/assembler/ClaudeMdAssembler.java`
- `java/src/main/java/dev/iadev/cli/UnsupportedLanguageException.java`
- `java/src/main/resources/shared/templates/CLAUDE.md` (template Pebble)

**Recursos a deletar:**
- `java/src/main/resources/targets/claude/agents/developers/{python,go,kotlin,typescript,rust,csharp}-developer.md` (6 arquivos)
- `java/src/main/resources/targets/claude/hooks/{python,go,kotlin,typescript,rust,csharp}/` (6 dirs)
- `java/src/main/resources/targets/claude/settings/{python-pip,go,typescript-npm,rust-cargo,csharp-dotnet}.json` (5 arquivos)
- `java/src/main/resources/targets/claude/skills/knowledge-packs/stack-patterns/{fastapi,django,click-cli,gin,ktor,nestjs,express,axum,dotnet,commander-cli}-patterns/` (dirs)
- `java/src/main/resources/targets/claude/rules/conditional/anti-patterns/10-anti-patterns.{python-*,go-*,kotlin-*,typescript-*,rust-*}.md` (8 arquivos)
- `java/src/main/resources/targets/claude/rules/conditional/security-anti-patterns/12-security-anti-patterns.{python,go,kotlin,typescript,rust}.md` (5 arquivos)
- `java/src/main/resources/shared/config-templates/setup-config.{go-gin,kotlin-ktor,python-click-cli,python-fastapi,python-fastapi-timescale,rust-axum,typescript-commander-cli,typescript-nestjs}.yaml` (8 YAMLs)

**Golden files (deletar 8 dirs ≈ 2.835 arquivos):**
- `java/src/test/resources/golden/go-gin/`
- `java/src/test/resources/golden/kotlin-ktor/`
- `java/src/test/resources/golden/python-click-cli/`
- `java/src/test/resources/golden/python-fastapi/`
- `java/src/test/resources/golden/python-fastapi-timescale/`
- `java/src/test/resources/golden/rust-axum/`
- `java/src/test/resources/golden/typescript-commander-cli/`
- `java/src/test/resources/golden/typescript-nestjs/`

**Golden files (preservar 9 dirs Java):**
- `java/src/test/resources/golden/{java-quarkus,java-spring,java-spring-clickhouse,java-spring-cqrs-es,java-spring-elasticsearch,java-spring-event-driven,java-spring-fintech-pci,java-spring-hexagonal,java-spring-neo4j}/`

**Testes a modificar:**
- `java/src/test/java/dev/iadev/smoke/SmokeProfiles.java` — 17→9 perfis
- `java/src/test/java/dev/iadev/golden/GoldenFileTest.java` — refatorar `profiles()` para delegar a `SmokeProfiles.profiles()`
- `java/src/test/java/dev/iadev/smoke/GoldenFileCoverageTest.java` — atualizar `PENDING_SMOKE_PROFILES`
- `java/src/test/java/dev/iadev/smoke/ProfileRegistrationIntegrityTest.java` — simetria YAML↔STACK_KEYS↔SmokeProfiles
- `java/src/test/java/dev/iadev/smoke/{ContentIntegrity,Pipeline,Frontmatter,AssemblerRegression,CrossProfileConsistency,etc}SmokeTest.java` — trim cases
- `java/src/test/java/dev/iadev/config/ConfigProfilesTest.java` — reduzir parametrização
- `java/src/test/java/dev/iadev/cli/LanguageFrameworkMappingTest.java`, `InteractivePrompterTest.java`, `StackMappingTest.java`, `StackResolverTest.java`, `StackValidatorVersionTest.java` — ajustes

**Testes a criar:**
- `java/src/test/java/dev/iadev/cli/CliLanguageValidationTest.java` — rejeição de `--language python|go|kotlin|typescript|rust|csharp|foo`
- `java/src/test/java/dev/iadev/domain/stack/StackMappingJavaOnlyIntegrityTest.java`
- `java/src/test/java/dev/iadev/pipeline/OutputDirectoryIntegrityTest.java` — invariante zero empty dirs
- `java/src/test/java/dev/iadev/application/assembler/CopyHelpersPruneEmptyDirsTest.java`
- `java/src/test/java/dev/iadev/application/assembler/ClaudeMdAssemblerTest.java`
- `java/src/test/java/dev/iadev/application/assembler/ClaudeMdRootPresenceTest.java` — parametrizado 9 perfis
- `java/src/test/java/dev/iadev/template/ClaudeMdTemplateSyntaxTest.java` — Pebble parseable
- `java/src/test/java/dev/iadev/e2e/Epic0048EndToEndTest.java` — CLI real, 2 perfis Java

**ADRs:**
- `adr/ADR-0048-java-only-scope.md`
- `adr/ADR-0048-B-claude-md-contract.md`

## 4. Business Rules (Cross-Cutting)

### RULE-048-01 Java-Only Scope
`LanguageFrameworkMapping.LANGUAGES == List.of("java")`. Qualquer outra linguagem em (i) config YAML, (ii) CLI flag, (iii) resource template, é rejeitada fail-fast com `UnsupportedLanguageException`.
**Aplicação**: todas as histórias de remoção (0048-0003 a 0048-0008). Precedência: sobrepõe-se a qualquer mapping legado ainda não limpo.

### RULE-048-02 Non-Language Dimensions Preserved
Dimensões ortogonais a linguagem **permanecem intactas**: databases (PostgreSQL, MySQL, ClickHouse, Elasticsearch, Neo4j, TimescaleDB), mensageria (Kafka, RabbitMQ, SQS), padrões de arquitetura (hexagonal, CQRS, event-driven, clean, layered, DDD), interface types (REST, gRPC, GraphQL, WebSocket, CLI, event-consumer/producer, scheduled), compliance frameworks (PCI, HIPAA, LGPD, SOX).
**Aplicação**: gate em cada PR — qualquer alteração em `targets/claude/skills/knowledge-packs/{architecture,data-management,infrastructure,compliance,resilience,observability}/**` é fora de escopo e deve ser rejeitada no review.

### RULE-048-03 Golden Byte-for-Byte Parity (9 Java Profiles)
Os 9 goldens `java-*` em `java/src/test/resources/golden/` são invariantes deste épico **exceto** (a) remoção controlada de dirs vazios em STORY-0048-0009 (Bug A) e (b) adição controlada de `CLAUDE.md` raiz em STORY-0048-0011 (Bug B). Nenhuma outra mudança byte-a-byte em arquivos existentes. `GoldenFileTest` é o oracle.

### RULE-048-04 Zero Empty Directories
Output do gerador (qualquer perfil, qualquer plataforma) NÃO pode conter diretórios vazios. Definição: diretório sem arquivos regulares próprios E sem subdiretórios contendo arquivos regulares (bottom-up check).
**Enforcement**: `OutputDirectoryIntegrityTest` novo em STORY-0048-0009 falha se encontrar algum.

### RULE-048-05 CLAUDE.md é Root-File Obrigatório
Geração Claude-Code sempre produz `CLAUDE.md` no root do output. Template em `shared/templates/CLAUDE.md` (criado em STORY-0048-0010) com placeholders Pebble: `{{PROJECT_NAME}}`, `{{LANGUAGE}}`, `{{FRAMEWORK}}`, `{{ARCHITECTURE}}`, `{{DATABASES}}`, `{{INTERFACE_TYPES}}`, `{{BUILD_COMMAND}}`, `{{TEST_COMMAND}}`. Assembler dedicado em STORY-0048-0011.

### RULE-048-06 Unsupported Language Message
CLI com `--language <x>` onde `x ∉ {"java"}` retorna exit code != 0 com mensagem exata:
`"Language '<x>' is not supported. Only 'java' is available (see CHANGELOG v4.0.0 / EPIC-0048)."`
Nunca NPE, nunca silent default. Validado por `CliLanguageValidationTest` em STORY-0048-0003.

### RULE-048-07 Atomic, Reversible Commits
Cada task = 1 commit Conventional Commits (Rule 08/18). Escopos:
- `feat(task-0048-YYYY-NNN):` para código novo (assemblers, exceptions)
- `refactor(task-0048-YYYY-NNN):` para remoções de código legado
- `test(task-0048-YYYY-NNN):` para testes
- `docs(task-0048-YYYY-NNN):` para ADRs/READMEs/CHANGELOG
- `chore(task-0048-YYYY-NNN):` para deleção de resources/goldens

### RULE-048-08 Investigation Precedes Removal
Nenhuma story de remoção (0048-0003 a 0048-0008) pode iniciar antes de STORY-0048-0001 estar mergeada. A story de investigação é o gate — produz inventário canônico + repro-bugs.

### RULE-048-09 TDD Red-Green-Refactor
Toda story com bug fix (0048-0009, 0048-0011) executa RED-first: teste que reproduz o bug é escrito e commitado ANTES do fix. Historie no git log. Acceptance tests (Gherkin) derivados via Double-Loop TDD.

### RULE-048-10 JaCoCo Coverage Mantido
≥ 95% line / ≥ 90% branch (Rule 05, `java/pom.xml:293-342`) validado em cada PR via `mvn verify`. Nenhuma classe nova pode nascer abaixo do threshold.

## 5. Global Definition of Ready (DoR)

- Baseline verde em `develop`: `mvn clean verify` passa com coverage ≥95%/90% antes da STORY-0048-0001.
- Branch `feature/epic-0048-java-only-generator` criada a partir de `develop` atualizado.
- Tag `pre-epic-0048-java-only` criada em `develop` (rollback anchor).
- Branch `backup/pre-epic-0048` congelada.
- STORY-0048-0001 concluída antes de qualquer remoção (RULE-048-08).
- ADR-0048-A e ADR-0048-B mergeadas antes de STORY-0048-0003 e STORY-0048-0011 respectivamente.
- `planningSchemaVersion: "2.0"` declarado em `execution-state.json`.
- Nenhuma edição em `.claude/**` do repo-raiz (RULE-001): apenas `java/src/main/resources/targets/claude/**` + regeneração via `mvn process-resources`.

## 6. Global Definition of Done (DoD)

- **Cobertura**: ≥95% line / ≥90% branch (JaCoCo) global.
- **Testes Automatizados**: cada story tem ≥1 teste unitário validando acceptance criterion principal. Stories de bug (0009, 0011) têm teste RED-first.
- **Smoke Tests**: `mvn verify` verde após cada story.
- **Golden Parity**: 9 goldens Java preservados exceto adição controlada em 0009 (prune empty dirs) e 0011 (adição CLAUDE.md).
- **Zero Skipped Tests**: grep `@Disabled` e "skipped profile" retorna 0 hits novos.
- **Sem Código Morto**: `grep -rn "python\|kotlin\|typescript\|rust\|golang\|\"go\"\|csharp\|dotnet" java/src/main/java` retorna hits apenas em comentários/mensagens de erro, nunca em caminhos executáveis.
- **Documentação**: README.md, CLAUDE.md raiz, CHANGELOG.md (v4.0.0) atualizados; ADR-0048-A e ADR-0048-B criadas.
- **Commits Atômicos**: 1 commit/PR por task (Rule 18).
- **Performance**: tempo de `mvn test` reduz ≥30%.
- **Backward Compat**: `--language java` e omissão continuam funcionando; outros valores retornam erro claro (RULE-048-06).

## 7. Story Index (13 stories em ordem topológica)

| ID | Título | Blocked By | Size | Risk | Valor |
|---|---|---|---|---|---|
| STORY-0048-0001 | Investigação: inventário + repro Bug A + repro Bug B | — | M (8h) | low | Gate de entrada; ambiguidades resolvidas; bugs reproduzíveis |
| STORY-0048-0002 | ADR-0048-A (Java-only scope) + ADR-0048-B (CLAUDE.md contract) | 0001 | S (4h) | low | Decisões arquiteturais documentadas |
| STORY-0048-0003 | Restringir LanguageFrameworkMapping + CliLanguageValidator + UnsupportedLanguageException | 0002 | S (5h) | med | Source-of-truth Java-only + UX-friendly error |
| STORY-0048-0004 | Limpar StackMapping + StackResolver + StackValidator (inclui csharp leftover) | 0003 | M (7h) | med | Remove branches mortas em mapeamentos de domínio |
| STORY-0048-0005 | Remover templates targets/claude/{agents,hooks,settings} não-Java | 0003 | M (5h) | med | ~17 arquivos/dirs deletados |
| STORY-0048-0006 | Remover skills, rules, anti-patterns, security-anti-patterns não-Java | 0003 | M (6h) | med | 15+ arquivos removidos |
| STORY-0048-0007 | Remover 8 goldens + 8 YAMLs setup-config não-Java | 0003,0005,0006 | L (10h) | high | 2.835 arquivos golden deletados |
| STORY-0048-0008 | Atualizar testes parametrizados: SmokeProfiles, GoldenFileTest, ConfigProfiles, simetria tests, expected-artifacts.json | 0007 | M (8h) | med | Matrizes reduzidas para 9 perfis; zero skipped |
| STORY-0048-0009 | Bug A — OutputDirectoryIntegrityTest (RED) → fix CopyHelpers + pruneEmptyDirs → regen 9 goldens | 0008 | M (8h) | high | Zero empty dirs invariante enforced |
| STORY-0048-0010 | Template Pebble shared/templates/CLAUDE.md + ClaudeMdTemplateSyntaxTest | 0002 | S (3h) | low | Template-fonte pronto para consumo |
| STORY-0048-0011 | Bug B — ClaudeMdAssembler novo + register AssemblerFactory + 9 goldens atualizados | 0010,0008 | L (10h) | high | CLAUDE.md gerado em todos os projetos |
| STORY-0048-0012 | Higienização + Epic0048EndToEndTest (CLI real, 2 perfis, valida A+B) | 0009,0011 | M (7h) | med | Smoke final + testes negativos completos |
| STORY-0048-0013 | Audit grep + README + CHANGELOG v4.0.0 + release notes + cleanup worktrees | 0012 | M (5h) | low | Entregáveis user-facing atualizados |

**Total: ~85h (13 stories).** Paralelizações possíveis: 0005/0006 (camadas disjuntas); 0010 pode rodar em paralelo com 0003-0008 (template é standalone).

## 8. Ordem Topológica (DAG)

```
0001 ─► 0002 ┬► 0003 ┬► 0004 ──────┐
             │       ├► 0005 ──────┤
             │       └► 0006 ──────┘
             │                      ▼
             │                   0007 (deletar goldens/YAMLs)
             │                      │
             │                      ▼
             │                   0008 (trim testes)
             │                      │
             │                      ▼
             │                   0009 (Bug A fix + regen 9 goldens)
             │                      │
             └► 0010 (template) ──► 0011 (Bug B assembler + regen 9 goldens)
                                    │
                                    ▼
                                 0012 (E2E smoke)
                                    │
                                    ▼
                                 0013 (audit + changelog + release)
```

## 9. Rollback Plan

- Tag `pre-epic-0048-java-only` em `develop` ANTES de STORY-0048-0001 merge.
- Branch `backup/pre-epic-0048` congelada.
- Cada story = PR separado, squash-merge → `git revert <story-sha>` reverte isolado.
- Épico inteiro: `git revert` do range (NUNCA `git reset --hard`).
- **Feature flags v4.0.0 only** (removidas em v5.0.0):
  - `PipelineOptions.pruneEmptyDirs(boolean)` default `true` (opt-out: `--legacy-empty-dirs`)
  - `PipelineOptions.generateClaudeMd(boolean)` default `true` (opt-out: `--no-claude-md`)
- Remoção de linguagens = **irreversível por design** após v4.0.0. Usuários pinam `v3.x`. Branch `legacy/v3` read-only. Documentado em ADR-0048-A + release notes.

## 10. Métricas de Sucesso

| Métrica | Baseline | Target | Medição |
|---|---|---|---|
| Arquivos em golden/ | ~5.347 (17 perfis) | ≤2.500 (9 perfis) | `find golden -type f \| wc -l` |
| Perfis em SmokeProfiles | 17 | 9 | `SmokeProfilesTest` |
| Tempo `mvn test` | TBD em 0001 | −30% | Média de 3 runs pré/pós |
| Coverage line | ≥95% | ≥95% | JaCoCo |
| Coverage branch | ≥90% | ≥90% | JaCoCo |
| Golden parity Java | 100% | 100% | `GoldenFileTest` |
| Empty dirs output | ≥1 (Bug A) | 0 | `OutputDirectoryIntegrityTest` |
| CLAUDE.md output | ausente (Bug B) | 9 perfis | `ClaudeMdRootPresenceTest` |
| LANGUAGE_COMMANDS entries | 8 | 2 | `StackMappingTest` |
| LOC removidas | TBD | >10.000 | `git diff --stat` |

## 11. Journeys

### Journey 1 — Usuário gera projeto Java-only (happy path)
1. Usuário executa `ia-dev-env generate --profile java-spring --out ./my-project`
2. Pipeline resolve stack, aplica assemblers (22 + novo ClaudeMdAssembler)
3. Output contém: `.claude/`, `.github/workflows/`, `CLAUDE.md` (NOVO), `CONSTITUTION.md`, `Dockerfile`, `pom.xml`, etc.
4. **Invariantes**: zero dirs vazios, CLAUDE.md existe, exit code 0.

### Journey 2 — Usuário tenta linguagem removida (unhappy path)
1. Usuário executa `ia-dev-env generate --language python --out ./x`
2. `GenerateCommand` valida `language` early e lança `UnsupportedLanguageException`
3. Stderr: `"Language 'python' is not supported. Only 'java' is available (see CHANGELOG v4.0.0 / EPIC-0048)."`
4. Exit code != 0. Sem NPE, sem silent fallback.

### Journey 3 — Developer atualiza golden files (após Bug B)
1. Developer implementa ClaudeMdAssembler
2. Executa `GoldenFileRegenerator` nos 9 perfis Java
3. Inspeciona `git diff`: exclusivamente adição de `CLAUDE.md` em cada golden
4. Commit atômico (9 goldens em 1-3 commits para rollback granular)
5. `mvn verify` → `GoldenFileTest` verde

## 12. Dependencies (outbound)

- Git Flow (Rule 09): feature branches → develop → release/v4.0.0 → main + tag
- JaCoCo 0.8.14 (pom.xml)
- Pebble template engine (já em uso em `TemplateEngine.java`)
- JUnit 5.11.4, AssertJ 3.27.3, Mockito 5.14.2 (já em uso)

## 13. Interfaces (técnicas)

- **CLI**: `ia-dev-env generate [--language java] [--profile java-*] [--out <path>]` — única linguagem válida é "java"
- **Config YAML**: `setup-config.java-*.yaml` — 9 perfis válidos
- **Assembler interface**: `ClaudeMdAssembler implements Assembler`, target=`AssemblerTarget.ROOT`, platforms=`{CLAUDE_CODE}`
- **Template engine**: `{{PROJECT_NAME}}`, `{{LANGUAGE}}`, `{{FRAMEWORK}}`, `{{ARCHITECTURE}}`, `{{DATABASES}}`, `{{INTERFACE_TYPES}}`, `{{BUILD_COMMAND}}`, `{{TEST_COMMAND}}` em `CLAUDE.md` template
