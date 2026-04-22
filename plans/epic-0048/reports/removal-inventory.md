# Inventário Canônico de Remoção — EPIC-0048

> **Gerado em:** 2026-04-22
> **Base commit:** d8f7ff0c2 (develop após bootstrap PR #545)
> **Escopo:** Tudo que precisa sair para atender RULE-048-01 (Java-Only Scope). Dimensões ortogonais (databases, mensageria, arquitetura, interface types, compliance) **permanecem intactas** per RULE-048-02.

## Legenda

- **Story-target:** id da story subsequente que fará a remoção efetiva. Não deletar nada antes de `0048-0001` estar mergeada (RULE-048-08 — gate).
- **path:lineno:** linha exata no estado atual de `develop`. Se a linha mudar antes do merge da story-target, verificar se o fim-de-linha ainda corresponde a uma linguagem não-Java.
- **Snippet:** conteúdo textual relevante da linha (sem aspas externas; `...` elide).

---

## 1. Código Fonte Java

### 1.1 `java/src/main/java/dev/iadev/cli/LanguageFrameworkMapping.java`

| Linguagem | path:lineno | Snippet | Story-target |
| :--- | :--- | :--- | :--- |
| all 5 | LanguageFrameworkMapping.java:20 | `"java", "python", "go", "kotlin", "typescript", "rust"` → manter apenas `"java"` | 0003 |
| kotlin | LanguageFrameworkMapping.java:26 | `Architecture pattern styles for java/kotlin.` (comment) | 0003 |
| kotlin | LanguageFrameworkMapping.java:38 | `List.of("java", "kotlin")` (ARCH_PATTERN_LANGUAGES) — manter apenas `"java"` | 0003 |
| python | LanguageFrameworkMapping.java:47 | `"python", List.of("fastapi", "click-cli")` (FRAMEWORKS map) | 0003 |
| go | LanguageFrameworkMapping.java:48 | `"go", List.of("gin")` | 0003 |
| kotlin | LanguageFrameworkMapping.java:49 | `"kotlin", List.of("ktor")` | 0003 |
| typescript | LanguageFrameworkMapping.java:50 | `"typescript", List.of("nestjs")` | 0003 |
| rust | LanguageFrameworkMapping.java:51 | `"rust", List.of("axum")` | 0003 |
| python | LanguageFrameworkMapping.java:56 | `"python", List.of("pip")` (BUILD_TOOLS map) | 0003 |
| go | LanguageFrameworkMapping.java:57 | `"go", List.of("go")` | 0003 |
| kotlin | LanguageFrameworkMapping.java:58 | `"kotlin", List.of("gradle")` | 0003 |
| typescript | LanguageFrameworkMapping.java:59 | `"typescript", List.of("npm")` | 0003 |
| rust | LanguageFrameworkMapping.java:60 | `"rust", List.of("cargo")` | 0003 |
| python | LanguageFrameworkMapping.java:65 | `"python", "3.12"` (DEFAULT_VERSIONS) | 0003 |
| go | LanguageFrameworkMapping.java:66 | `"go", "1.22"` | 0003 |
| kotlin | LanguageFrameworkMapping.java:67 | `"kotlin", "2.0"` | 0003 |
| typescript | LanguageFrameworkMapping.java:68 | `"typescript", "5"` | 0003 |
| rust | LanguageFrameworkMapping.java:69 | `"rust", "1.78"` | 0003 |
| python | LanguageFrameworkMapping.java:75 | `Map.entry("fastapi", "0.115")` (FRAMEWORK_VERSIONS) | 0003 |
| python | LanguageFrameworkMapping.java:76 | `Map.entry("click-cli", "8.1")` | 0003 |
| go | LanguageFrameworkMapping.java:77 | `Map.entry("gin", "1.10")` | 0003 |
| kotlin | LanguageFrameworkMapping.java:78 | `Map.entry("ktor", "3.0")` | 0003 |
| typescript | LanguageFrameworkMapping.java:79 | `Map.entry("nestjs", "10")` | 0003 |
| rust | LanguageFrameworkMapping.java:80 | `Map.entry("axum", "0.7")` | 0003 |

### 1.2 `java/src/main/java/dev/iadev/domain/stack/StackMapping.java`

| Linguagem | path:lineno | Snippet | Story-target |
| :--- | :--- | :--- | :--- |
| kotlin | StackMapping.java:31-36 | `Map.entry("kotlin-gradle", new LanguageCommandSet(...))` | 0004 |
| typescript | StackMapping.java:37-42 | `Map.entry("typescript-npm", new LanguageCommandSet(...))` | 0004 |
| python | StackMapping.java:43-48 | `Map.entry("python-pip", new LanguageCommandSet(...))` | 0004 |
| go | StackMapping.java:49-54 | `Map.entry("go-go", new LanguageCommandSet(...))` | 0004 |
| rust | StackMapping.java:55-60 | `Map.entry("rust-cargo", new LanguageCommandSet(...))` | 0004 |
| csharp | StackMapping.java:61-66 | `Map.entry("csharp-dotnet", new LanguageCommandSet(...))` — **LEFTOVER sem perfil/golden correspondente** | 0004 |
| — | StackMapping.java:17 | Comentário `(8 entries)` deve virar `(2 entries)` — java-maven + java-gradle | 0004 |
| nestjs | StackMapping.java:74 | `Map.entry("nestjs", 3000)` (FRAMEWORK_PORTS) | 0004 |
| fastapi | StackMapping.java:76 | `Map.entry("fastapi", 8000)` | 0004 |
| django | StackMapping.java:77 | `Map.entry("django", 8000)` | 0004 |
| gin | StackMapping.java:78 | `Map.entry("gin", 8080)` | 0004 |
| ktor | StackMapping.java:79 | `Map.entry("ktor", 8080)` | 0004 |
| axum | StackMapping.java:80 | `Map.entry("axum", 3000)` | 0004 |
| actix-web | StackMapping.java:81 | `Map.entry("actix-web", 8080)` (rust) | 0004 |
| aspnet | StackMapping.java:82 | `Map.entry("aspnet", 5000)` (csharp) | 0004 |
| — | StackMapping.java:69 | Comentário `(11 entries)` deve refletir apenas frameworks Java | 0004 |
| nestjs/express/etc. | StackMapping.java:93-101 | Entries em FRAMEWORK_HEALTH_PATHS para frameworks não-Java | 0004 |
| kotlin | StackMapping.java:110 | `Map.entry("quarkus", List.of("java", "kotlin"))` — remover "kotlin" | 0004 |
| kotlin | StackMapping.java:111 | `Map.entry("spring-boot", List.of("java", "kotlin"))` — remover "kotlin" | 0004 |
| typescript | StackMapping.java:112 | `Map.entry("nestjs", List.of("typescript"))` | 0004 |
| typescript | StackMapping.java:113 | `Map.entry("express", List.of("typescript"))` | 0004 |
| typescript | StackMapping.java:114 | `Map.entry("fastify", List.of("typescript"))` | 0004 |
| typescript | StackMapping.java:115 | `Map.entry("commander", List.of("typescript"))` | 0004 |
| python | StackMapping.java:116 | `Map.entry("fastapi", List.of("python"))` | 0004 |
| python | StackMapping.java:117 | `Map.entry("django", List.of("python"))` | 0004 |
| python | StackMapping.java:118 | `Map.entry("flask", List.of("python"))` | 0004 |
| go | StackMapping.java:119 | `Map.entry("stdlib", List.of("go"))` | 0004 |
| go | StackMapping.java:120 | `Map.entry("gin", List.of("go"))` | 0004 |
| go | StackMapping.java:121 | `Map.entry("fiber", List.of("go"))` | 0004 |
| kotlin | StackMapping.java:122 | `Map.entry("ktor", List.of("kotlin"))` | 0004 |
| rust | StackMapping.java:123 | `Map.entry("axum", List.of("rust"))` | 0004 |
| rust | StackMapping.java:124 | `Map.entry("actix-web", List.of("rust"))` | 0004 |
| csharp | StackMapping.java:125 | `Map.entry("aspnet", List.of("csharp"))` | 0004 |
| — | StackMapping.java:107 | Comentário `(16 entries)` deve virar `(2 entries — quarkus + spring-boot)` | 0004 |
| kotlin | StackMapping.java:172 | `"kotlin", "eclipse-temurin:{version}-jre-alpine"` (DOCKER_BASE_IMAGES) | 0004 |
| typescript | StackMapping.java:173 | `"typescript", "node:{version}-alpine"` | 0004 |
| python | StackMapping.java:174 | `"python", "python:{version}-slim"` | 0004 |
| go | StackMapping.java:175 | `"go", "golang:{version}-alpine"` | 0004 |
| rust | StackMapping.java:176 | `"rust", "rust:{version}-slim"` | 0004 |
| csharp | StackMapping.java:177 | `"csharp", "mcr.microsoft.com/dotnet/aspnet:{version}"` | 0004 |
| kotlin | StackMapping.java:187 | `"kotlin-gradle", "kotlin"` (HOOK_TEMPLATE_MAP) | 0004 |
| typescript | StackMapping.java:188 | `"typescript-npm", "typescript"` | 0004 |
| python | StackMapping.java:189 | `"python-pip", ""` | 0004 |
| go | StackMapping.java:190 | `"go-go", "go"` | 0004 |
| rust | StackMapping.java:191 | `"rust-cargo", "rust"` | 0004 |
| csharp | StackMapping.java:192 | `"csharp-dotnet", "csharp"` | 0004 |
| kotlin | StackMapping.java:199 | `"kotlin-gradle", "java-gradle"` (SETTINGS_LANG_MAP) | 0004 |
| typescript | StackMapping.java:200 | `"typescript-npm", "typescript-npm"` | 0004 |
| python | StackMapping.java:201 | `"python-pip", "python-pip"` | 0004 |
| go | StackMapping.java:202 | `"go-go", "go"` | 0004 |
| rust | StackMapping.java:203 | `"rust-cargo", "rust-cargo"` | 0004 |
| csharp | StackMapping.java:204 | `"csharp-dotnet", "csharp-dotnet"` | 0004 |

### 1.3 `java/src/main/java/dev/iadev/domain/stack/StackValidator.java`

| Linguagem | path:lineno | Snippet | Story-target |
| :--- | :--- | :--- | :--- |
| python | StackValidator.java:22-23 | `/** Minimum Python minor version for FastAPI. */` + `PYTHON_310_MINOR = 10` | 0004 |
| python | StackValidator.java:31-32 | `/** Python major version constant. */` + `PYTHON_3_MAJOR = 3` | 0004 |
| python | StackValidator.java:242 | `static List<String> checkDjangoPythonVersion(` — **método privado que fica órfão** | 0004 |
| python | StackValidator.java:245 | `.checkDjangoPythonVersion(config);` — chamada a remover junto do método | 0004 |

### 1.4 `java/src/main/java/dev/iadev/domain/stack/StackResolver.java`

Nenhum hit direto em `python/kotlin/typescript/rust/csharp` no arquivo. Revalidar em 0004 se detector de stack (pom.xml, go.mod, package.json, pyproject.toml, Cargo.toml, *.csproj) está em outro arquivo (ex.: `StackResolver` pode delegar a helpers em `cli/` ou `infrastructure/`).

---

## 2. Templates — `java/src/main/resources/targets/claude/`

### 2.1 `agents/developers/` (7 arquivos → manter apenas `java-developer.md`)

| Arquivo | Linguagem | Story-target |
| :--- | :--- | :--- |
| `csharp-developer.md` | csharp | 0005 |
| `go-developer.md` | go | 0005 |
| `kotlin-developer.md` | kotlin | 0005 |
| `python-developer.md` | python | 0005 |
| `rust-developer.md` | rust | 0005 |
| `typescript-developer.md` | typescript | 0005 |

### 2.2 `hooks/` (8 subdirs → manter `java-maven/`, `java-gradle/`, e os telemetry-*.sh)

| Subdir | Linguagem | Story-target |
| :--- | :--- | :--- |
| `csharp/` | csharp | 0005 |
| `go/` | go | 0005 |
| `kotlin/` | kotlin | 0005 |
| `rust/` | rust | 0005 |
| `typescript/` | typescript | 0005 |

> **Nota:** subdir `python/` **não existe** no repositório atual (divergência vs spec — spec esperava 6 hook dirs não-Java, apenas 5 presentes). `telemetry-*.sh` e `TELEMETRY-README.md` são language-agnostic — **preservar**.

### 2.3 `settings/*.json` (5 JSONs language-specific não-Java)

| Arquivo | Linguagem | Story-target |
| :--- | :--- | :--- |
| `csharp-dotnet.json` | csharp | 0005 |
| `go.json` | go | 0005 |
| `python-pip.json` | python | 0005 |
| `rust-cargo.json` | rust | 0005 |
| `typescript-npm.json` | typescript | 0005 |

> **Nota:** `kotlin-*.json` **não existe** isoladamente (kotlin mapeia para `java-gradle.json` via `SETTINGS_LANG_MAP`). Divergência vs spec (esperava 6 arquivos, apenas 5 presentes). `cache-*.json`, `database-*.json`, `docker*.json`, `kubernetes.json`, `testing-newman.json`, `base.json` são ortogonais a linguagem — **preservar**.

---

## 3. Skills / Knowledge-Packs

### 3.1 `skills/knowledge-packs/stack-patterns/` (9 dirs não-Java → manter apenas `quarkus-patterns/` e `spring-patterns/`)

| Subdir | Linguagem/Framework | Story-target |
| :--- | :--- | :--- |
| `axum-patterns/` | rust | 0006 |
| `click-cli-patterns/` | python | 0006 |
| `django-patterns/` | python | 0006 |
| `dotnet-patterns/` | csharp | 0006 |
| `express-patterns/` | typescript | 0006 |
| `fastapi-patterns/` | python | 0006 |
| `gin-patterns/` | go | 0006 |
| `ktor-patterns/` | kotlin | 0006 |
| `nestjs-patterns/` | typescript | 0006 |

> **Divergência vs spec:** spec do épico estima "10 stack-patterns não-Java". Inventário real: **9**. Ajustar valor da story 0006 de `10 → 9` no PR de 0006.

### 3.2 `rules/conditional/anti-patterns/` (8 arquivos não-Java → manter apenas `java-quarkus` e `java-spring-boot`)

| Arquivo | Linguagem/Framework | Story-target |
| :--- | :--- | :--- |
| `10-anti-patterns.go-gin.md` | go | 0006 |
| `10-anti-patterns.kotlin-ktor.md` | kotlin | 0006 |
| `10-anti-patterns.python-click.md` | python | 0006 |
| `10-anti-patterns.python-fastapi.md` | python | 0006 |
| `10-anti-patterns.rust-axum.md` | rust | 0006 |
| `10-anti-patterns.typescript-commander.md` | typescript | 0006 |
| `10-anti-patterns.typescript-nestjs.md` | typescript | 0006 |

> **Não existe `10-anti-patterns.csharp-*.md`.** Divergência vs spec (esperava 8 arquivos; real: **7**).

### 3.3 `rules/conditional/security-anti-patterns/` (5 arquivos não-Java → manter `java.md`)

| Arquivo | Linguagem | Story-target |
| :--- | :--- | :--- |
| `12-security-anti-patterns.go.md` | go | 0006 |
| `12-security-anti-patterns.kotlin.md` | kotlin | 0006 |
| `12-security-anti-patterns.python.md` | python | 0006 |
| `12-security-anti-patterns.rust.md` | rust | 0006 |
| `12-security-anti-patterns.typescript.md` | typescript | 0006 |

> **Não existe `12-security-anti-patterns.csharp.md`.** Divergência vs spec (esperava 5; real: **5** — OK).

---

## 4. Goldens — `java/src/test/resources/golden/` (8 dirs não-Java → deletar)

| Dir | Linguagem/Framework | File count | Story-target |
| :--- | :--- | :--- | :--- |
| `go-gin/` | go | 756 | 0007 |
| `kotlin-ktor/` | kotlin | 379 | 0007 |
| `python-click-cli/` | python | 323 | 0007 |
| `python-fastapi/` | python | 378 | 0007 |
| `python-fastapi-timescale/` | python | 341 | 0007 |
| `rust-axum/` | rust | 385 | 0007 |
| `typescript-commander-cli/` | typescript | 312 | 0007 |
| `typescript-nestjs/` | typescript | 382 | 0007 |

**TOTAL non-Java goldens:** **3256 arquivos** (spec estimou ~2835 — divergência de +421; atualizar valor em 0007 para 3256).

**Preservar (9 dirs, 3632 arquivos):** `java-quarkus/`, `java-spring/`, `java-spring-clickhouse/`, `java-spring-cqrs-es/`, `java-spring-elasticsearch/`, `java-spring-event-driven/`, `java-spring-fintech-pci/`, `java-spring-hexagonal/`, `java-spring-neo4j/`.

**Também preservar:** `parallelism-heuristics/`, `x-parallel-eval/` — não são perfis de linguagem; são fixtures de outros epics.

---

## 5. YAMLs — `java/src/main/resources/shared/config-templates/`

### 5.1 `setup-config.*.yaml` (8 arquivos não-Java → deletar)

| Arquivo | Linguagem/Framework | Story-target |
| :--- | :--- | :--- |
| `setup-config.go-gin.yaml` | go | 0007 |
| `setup-config.kotlin-ktor.yaml` | kotlin | 0007 |
| `setup-config.python-click-cli.yaml` | python | 0007 |
| `setup-config.python-fastapi.yaml` | python | 0007 |
| `setup-config.python-fastapi-timescale.yaml` | python | 0007 |
| `setup-config.rust-axum.yaml` | rust | 0007 |
| `setup-config.typescript-commander-cli.yaml` | typescript | 0007 |
| `setup-config.typescript-nestjs.yaml` | typescript | 0007 |

**Preservar (10 YAMLs Java):** `setup-config.java-picocli-cli.yaml`, `setup-config.java-quarkus.yaml`, `setup-config.java-spring.yaml`, `setup-config.java-spring-clickhouse.yaml`, `setup-config.java-spring-cqrs-es.yaml`, `setup-config.java-spring-elasticsearch.yaml`, `setup-config.java-spring-event-driven.yaml`, `setup-config.java-spring-fintech-pci.yaml`, `setup-config.java-spring-hexagonal.yaml`, `setup-config.java-spring-neo4j.yaml`.

---

## 6. Testes parametrizados a trimar

Esses arquivos não são removidos inteiramente — apenas têm suas listas reduzidas de 17→9 entries. Entram no escopo de **story 0008**.

| path:lineno | Intenção | Story-target |
| :--- | :--- | :--- |
| `java/src/test/java/dev/iadev/smoke/SmokeProfiles.java:27-45` | `SMOKE_PROFILES` — 17 entries → 9 | 0008 |
| `java/src/test/java/dev/iadev/golden/GoldenFileTest.java` (linha do `profiles()`) | Delegar a `SmokeProfiles.profileList()` (elimina duplicação) | 0008 |
| `java/src/test/java/dev/iadev/smoke/GoldenFileCoverageTest.java` | `PENDING_SMOKE_PROFILES` ajustar | 0008 |
| `java/src/test/java/dev/iadev/smoke/ProfileRegistrationIntegrityTest.java` | Simetria YAML↔STACK_KEYS validada | 0008 |
| `java/src/main/java/dev/iadev/domain/stack/ConfigProfiles.java` (ou similar) | `STACK_KEYS` trim 18→10 | 0007 |
| `java/src/test/resources/expected-artifacts.json` | Regenerar após remoção dos perfis | 0008 |

---

## 7. Sumário por Story-Target

| Story | Escopo | Itens |
| :--- | :--- | :--- |
| 0003 | LanguageFrameworkMapping (3 mapas + LANGUAGES) | ~24 linhas |
| 0004 | StackMapping + StackValidator (incl. csharp-dotnet leftover) | ~50 linhas |
| 0005 | Templates agents/hooks/settings | 6 agents + 5 hook dirs + 5 JSONs = 16 paths |
| 0006 | Skills + rules conditional | 9 stack-patterns + 7 anti-patterns + 5 security-anti-patterns = 21 paths |
| 0007 | Goldens + YAMLs + ConfigProfiles | 8 golden dirs (3256 files) + 8 YAMLs |
| 0008 | Test matrix trim (sem deleção — apenas redução) | 5 arquivos de teste + expected-artifacts.json |

---

## 8. Divergências vs Spec

| # | Divergência | Impacto | Ajuste recomendado |
| :--- | :--- | :--- | :--- |
| 1 | Total goldens não-Java: spec **~2835**, real **3256** | 0007 PR body descreve número errado | Atualizar PR body de 0007 |
| 2 | Stack-patterns não-Java: spec **10**, real **9** | 0006 PR body descreve número errado | Atualizar PR body de 0006 |
| 3 | Anti-patterns não-Java: spec **8**, real **7** | — | Aceitar real |
| 4 | Settings JSON não-Java: spec **5**, real **5** | — | OK |
| 5 | Hook subdirs não-Java: spec **6**, real **5** | `python/` dir esperado mas inexistente | Aceitar real |
| 6 | `.codex`/`.cursor` supostamente criados pela CLI → **DESMENTIDO** | Bug A não inclui `.codex`/`.cursor` | Ver `investigation-report.md` §2 |

---

## 9. Caminho Atomico (RULE-048-07)

Cada item da tabela **NÃO** corresponde a um commit — cada *agrupamento lógico* na story-target é **1 commit atômico** em formato `{type}(task-0048-YYYY-NNN): ...`. Exemplos:

- Story 0003: `refactor(task-0048-0003-001): restrict LANGUAGES to java-only` (remove 5 linhas da Map; atualiza 3 outros mapas).
- Story 0007: `chore(task-0048-0007-001): delete 8 non-Java goldens` (deleção atômica de 3256 arquivos em 1 commit).
- Story 0005: `refactor(task-0048-0005-001): delete non-Java agents, hooks, settings` (16 paths em 1 commit).

Os testes parametrizados de 0008 podem quebrar deliberadamente entre stories 0007 e 0008 (RED window). Aceitar isso — a story 0008 fecha o verde.
