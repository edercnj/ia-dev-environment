# Especificação: Remoção de Código Deprecated (`forRemoval = true`)

## Visão Geral

O repositório `ia-dev-environment` (Java 21 / Maven / picocli) contém 6 símbolos marcados com `@Deprecated(forRemoval = true)` distribuídos em 2 arquivos de produção. Todos possuem substitutos já implementados e testados, mas continuam sendo consumidos por callers legítimos — o que bloqueia a remoção imediata. Esta especificação define a migração dos callers remanescentes seguida da remoção física dos símbolos deprecated, sem introduzir regressão funcional nem quebra de golden files.

## Problema

1. **Dívida técnica acumulada**: Epic-0023 introduziu `DatabaseSettingsMapping` como substituto de parte de `StackMapping`. Um epic posterior introduziu `ResourceResolver.resolveResourceDir(String)` como substituto de `resolveResourcesRoot(...)` (com parâmetro `depth` frágil). Ambas as migrações ficaram no meio do caminho — APIs novas coexistindo com APIs deprecated.
2. **Duplicação de fonte de verdade**: `StackMapping.DATABASE_SETTINGS_MAP` e `DatabaseSettingsMapping.DATABASE_SETTINGS_MAP` expõem a mesma tabela de mapeamento, convidando drift silencioso.
3. **Parâmetro frágil**: `resolveResourcesRoot(String, int depth)` depende da posição física do recurso no classpath — qualquer reorganização do layout resources quebra silenciosamente os 23 assemblers que chamam o método, mesmo que o recurso exista.
4. **Poluição de IDE / grep**: Warnings `[removal]` aparecem em cada compilação; `grep resolveResourcesRoot` continua devolvendo 40+ linhas irrelevantes para novos assemblers.
5. **Violação de Rule 03**: dead code (“unused methods / test-only code in production source” — Rule 03 §Forbidden) se aplica a símbolos que já têm substituto pronto e declarado `forRemoval`.

## Objetivo

Zero símbolos Java com `@Deprecated(forRemoval = true)` no código de produção, zero warnings `[removal]` em `mvn compile`, zero callers consumindo as APIs removidas, 100% de testes passando com coverage mantida dentro do threshold (≥ 95% linha / ≥ 90% branch — Rule 05). Golden files regenerados quando a migração alterar o output determinístico dos assemblers.

Métricas de sucesso:

- **Símbolos removidos:** 6 (4 em `StackMapping.java`, 2 em `ResourceResolver.java`)
- **Arquivos de produção migrados:** 25 (2 de `StackMapping`, 23 de `ResourceResolver`)
- **Arquivos de teste migrados:** ≥ 29 (9 + 20)
- **Warnings `[removal]`:** de 40+ para 0
- **Testes:** 100% verde antes e depois
- **Golden files:** consistentes (regenerados se necessário via `GoldenFileRegenerator` após `mvn process-resources`)

## Inventário de Código Deprecated

### 1. `java/src/main/java/dev/iadev/domain/stack/StackMapping.java`

| Símbolo | Linha | Substituto | Callers prod | Callers teste |
|---|---|---|---|---|
| `DATABASE_SETTINGS_MAP` (field) | 207–211 | `DatabaseSettingsMapping.DATABASE_SETTINGS_MAP` | 1 (`RulesConditionals.java:90`) | 7 |
| `CACHE_SETTINGS_MAP` (field) | 213–217 | `DatabaseSettingsMapping.CACHE_SETTINGS_MAP` | 0 | compartilhado nos mesmos testes |
| `getDatabaseSettingsKey(...)` (method) | — | `DatabaseSettingsMapping.getDatabaseSettingsKey(...)` | 1 (`PermissionCollector.java:122`) | 4 |
| `getCacheSettingsKey(...)` (method) | — | `DatabaseSettingsMapping.getCacheSettingsKey(...)` | 1 (`PermissionCollector.java:128`) | 2 |

Callers de produção (apenas 2 arquivos):
- `java/src/main/java/dev/iadev/application/assembler/RulesConditionals.java:90`
- `java/src/main/java/dev/iadev/application/assembler/PermissionCollector.java:122,128`

### 2. `java/src/main/java/dev/iadev/util/ResourceResolver.java`

| Símbolo | Linha | Substituto | Callers prod | Callers teste |
|---|---|---|---|---|
| `resolveResourcesRoot(String probe)` | 116–119 | `resolveResourceDir(String)` (depth-free) | 23 assemblers | 20+ |
| `resolveResourcesRoot(String probe, int depth)` | 133–137 | `resolveResourceDir(String)` (descarta `depth`) | mesmos 23 | mesmos |

Callers de produção (23 assemblers, em `java/src/main/java/dev/iadev/application/assembler/`):
`GrpcDocsAssembler`, `OperationalRunbookAssembler`, `AssemblerFactory`, `RulesAssembler`, `PlanTemplatesAssembler`, `ReadmeAssembler`, `EpicReportAssembler`, `CicdAssembler`, `ProtocolsAssembler`, `DocsAdrAssembler`, `DataMigrationPlanAssembler`, `ConstitutionAssembler`, `SloSliTemplateAssembler`, `RunbookAssembler`, `SettingsAssembler`, `AgentsAssembler`, `DocsContributingAssembler`, `SkillsAssembler`, `HooksAssembler`, `PatternsAssembler`, `DocsAssembler`, `ReleaseChecklistAssembler`, `IncidentTemplatesAssembler`.

## Componentes do Sistema

### 1. `StackMapping` (domain)
**Arquivo:** `java/src/main/java/dev/iadev/domain/stack/StackMapping.java`

Responsabilidade atual: hospeda tabelas de mapeamento de database/cache settings + API consulta. Após Epic-0023, `DatabaseSettingsMapping` passou a ser o proprietário dessas tabelas. A API deprecated em `StackMapping` é uma ponte histórica que deve ser desmontada.

**Ação:** remover 4 símbolos (2 fields + 2 methods). Nenhuma migração de API interna — o substituto tem assinatura equivalente.

### 2. `DatabaseSettingsMapping` (domain) — substituto ativo
**Arquivo:** `java/src/main/java/dev/iadev/domain/stack/DatabaseSettingsMapping.java`

Nenhuma alteração necessária. Já cobre 100% do escopo dos símbolos deprecated. A suite `DatabaseSettingsMapping*Test` já existe e está verde.

### 3. `RulesConditionals` (application)
**Arquivo:** `java/src/main/java/dev/iadev/application/assembler/RulesConditionals.java` (linha 90)

**Ação:** trocar referência `StackMapping.CACHE_SETTINGS_MAP` por `DatabaseSettingsMapping.CACHE_SETTINGS_MAP`. Import update.

### 4. `PermissionCollector` (application)
**Arquivo:** `java/src/main/java/dev/iadev/application/assembler/PermissionCollector.java` (linhas 122, 128)

**Ação:** trocar `StackMapping.getDatabaseSettingsKey(...)` e `StackMapping.getCacheSettingsKey(...)` por chamadas equivalentes em `DatabaseSettingsMapping`. Import update.

### 5. `ResourceResolver` (util)
**Arquivo:** `java/src/main/java/dev/iadev/util/ResourceResolver.java` (linhas 116–137)

Responsabilidade: resolver diretório base de um recurso no classpath. A API deprecated exige `depth` explícito; a nova API usa probe + subida dinâmica até encontrar o marker (layout-agnostic).

**Ação:** remover 2 overloads `resolveResourcesRoot(...)`. Manter `resolveResourceDir(String)` intacto.

### 6. Assemblers (23 arquivos — migração mecânica)
**Diretório:** `java/src/main/java/dev/iadev/application/assembler/`

Cada assembler na lista acima declara uma constante de caminho relativa (ex: `TEMPLATE_PATH`, `CICD_TEMPLATES`, `PROTOCOLS_RES_DIR`) e invoca `ResourceResolver.resolveResourcesRoot(path, depth)`. A migração substitui essa chamada por `ResourceResolver.resolveResourceDir(path)` e elimina a constante `depth`.

Risco: um assembler que dependa de um `depth` diferente do natural pode apontar para diretório errado pós-migração. Mitigação: cada assembler já tem cobertura em `src/test/java/.../assembler/...Test.java` — suite verde após migração valida comportamento. Em caso de divergência de golden file, regenerar via `GoldenFileRegenerator` (rodar `mvn process-resources` antes, conforme memória do projeto).

### 7. Testes (29+ arquivos)

| Arquivo | Deprecated referenciado |
|---|---|
| `src/test/java/.../util/ResourceResolverTest.java` | `resolveResourcesRoot` (11 linhas) |
| `src/test/java/.../knowledge/TimeseriesKnowledgeTest.java` | ambos |
| `src/test/java/.../application/assembler/*Test.java` (18 files) | `resolveResourcesRoot` |
| `src/test/java/.../domain/stack/StackMappingTest.java` | 4 símbolos |
| `src/test/java/.../domain/stack/StackMappingSearchTest.java` | 4 símbolos |
| `src/test/java/.../knowledge/NewsqlSettingsAndMappingTest.java` | 2 símbolos |
| `src/test/java/.../smoke/Epic0023IntegrationTest.java` | 4 símbolos |

**Regra:** nenhum teste pode manter referência a símbolo inexistente (senão o build quebra). Testes que hoje exercitam deprecated devem ser:
- **Migrados:** se a nova API tem cobertura equivalente via `DatabaseSettingsMapping*Test` / `ResourceResolverTest` (novo método), remover o teste duplicado.
- **Reescritos:** se o teste valida invariante de integração (ex: `Epic0023IntegrationTest`), reapontar para o substituto mantendo o cenário.

## Restrições

- **TDD (Rule 05):** cada mudança obedece Red-Green-Refactor. Passo Green: compile green, tests green. Passo Refactor: elimina warnings `[removal]`.
- **Coverage (Rule 05):** ≥ 95% linha / ≥ 90% branch antes e depois.
- **Golden files:** qualquer alteração que afete output determinístico requer regeneração via `GoldenFileRegenerator`, precedida de `mvn process-resources` (memória do projeto).
- **Git Flow (Rule 09):** histórias abrem branch `feature/epic-0044-...` a partir de `develop`, PR para `develop`.
- **Worktree (Rule 14):** orquestrador usa worktrees por história sob `.claude/worktrees/story-0044-NNNN/`.
- **Conventional Commits (Rule 08):** prefixo `refactor:` para remoção de deprecated; escopo `story(0044-NNNN)`. Sem mudança de comportamento público → sem bump MINOR/MAJOR.
- **Schema:** `planningSchemaVersion: "1.0"` (legacy v1). Remoção trivial não justifica o overhead task-first v2 de EPIC-0038. Histórias ficam exentas de Rules 15–18 (Rule 19).

## Proposta de Histórias

Duas áreas independentes → duas histórias independentes. Podem executar em paralelo, mas a ordem recomendada para review é STORY-0044-0001 antes de STORY-0044-0002 (menor risco primeiro, valida padrão).

### STORY-0044-0001 — Remover deprecated de `StackMapping`

**Escopo:** migrar `RulesConditionals.java:90`, `PermissionCollector.java:122,128` e 9 arquivos de teste para `DatabaseSettingsMapping`. Remover 4 símbolos deprecated em `StackMapping.java:207–217`.

**Entrega de valor:** fonte única para o mapa de database/cache settings; elimina risco de drift entre `StackMapping` e `DatabaseSettingsMapping`.

**Testabilidade:** INDEPENDENT. `DatabaseSettingsMappingTest` já cobre o substituto; `StackMappingTest` permanece verde após remoção dos 4 símbolos.

**Estimativa:** XS (2 arquivos produção + 9 teste).

### STORY-0044-0002 — Remover deprecated de `ResourceResolver`

**Escopo:** migrar 23 assemblers em `java/src/main/java/dev/iadev/application/assembler/` + 20+ testes para `resolveResourceDir(String)`. Remover 2 overloads `resolveResourcesRoot` em `ResourceResolver.java:116–137`.

**Entrega de valor:** elimina `depth` frágil (layout-sensitive), reduz superfície para bugs em reorganização de classpath, simplifica assinatura de 23 assemblers.

**Testabilidade:** INDEPENDENT. Cobertura individual de cada assembler; golden files regeneráveis se houver drift.

**Estimativa:** M (23 arquivos produção em waves de ~6, 20+ testes). Wave por wave valida compile+test.

## Fora de Escopo

- Deprecated em recursos YAML, markdown ou comentários (não há marcação `@Deprecated` equivalente).
- Deprecated em código Python/Groovy/Shell (nenhum encontrado).
- Introdução de novas APIs substitutas (ambos os substitutos já existem e estão em uso).
- Integração com Jira (pode ser feita depois via `x-jira-create-epic`).
- Renomear `DatabaseSettingsMapping` ou alterar sua assinatura pública.
