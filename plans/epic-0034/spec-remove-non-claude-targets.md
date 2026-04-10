# Especificação: Remoção de Targets Não-Claude do Gerador

## Visão Geral

O gerador `ia-dev-environment` (Java 21 / Maven / picocli) produz hoje artefatos para quatro plataformas de assistentes de IA:

- **Claude Code** (`.claude/`) — o target principal e único a ser mantido
- **GitHub Copilot** (`.github/`) — suporte legado a ser REMOVIDO
- **Codex** (`.codex/`) — suporte legado a ser REMOVIDO
- **Agents genérico** (`.agents/`) — suporte legado a ser REMOVIDO

Esta especificação define a remoção completa do suporte às três plataformas não-Claude. O resultado é um gerador focado exclusivamente em Claude Code, com redução drástica de superfície de código, resources, testes e golden files (~650-700 arquivos afetados, com queda de ~82% no volume de golden files).

## Problema

1. **Manutenção duplicada**: Toda nova feature (skill, rule, agent, hook) precisa ser implementada e validada em 4 targets distintos, aumentando o custo de cada mudança e criando oportunidade para drift entre plataformas.
2. **Build inflado**: Cada profile gera hoje ~9.500 arquivos de saída, dos quais ~8.273 (87%) são para targets não-Claude. Isso alonga CI/CD, aumenta o uso de disco e torna golden-file comparisons lentas.
3. **Golden files inflados**: `java/src/test/resources/golden/` contém 17 profiles × 3 targets extras = 51 subdirs legados ocupando espaço e tempo de regeneração.
4. **Complexidade de roteamento**: `AssemblerFactory` mantém três `buildXxxAssemblers()` métodos distintos; `PlatformFilter`, `PlatformContextBuilder` e `FileCategorizer` carregam lógica condicional `if (hasCopilot)`, `if (hasCodex)` que polui classes compartilhadas.
5. **Test suite carrega classes irrelevantes**: Há ~29 classes de teste específicas de targets não-Claude que continuam rodando em todo `mvn verify`, adicionando tempo sem benefício.
6. **Documentação polui contexto**: `CLAUDE.md` na raiz descreve três targets legados, adicionando ~200 linhas de contexto irrelevante carregadas automaticamente em todas as conversas com Claude Code.
7. **CLI confusa**: `--platform copilot|codex|agents` continua válida e sugere suporte ativo. Usuários podem gerar artefatos que não são mais mantidos.

## Objetivo

Produzir um gerador **Claude-only**: um único valor no enum `Platform`, um único `AssemblerTarget`, zero referências a Copilot/Codex/agents em código, resources, testes ou documentação. Build verde, testes remanescentes 100% passando, coverage mantida dentro do threshold (≥ 95% line / ≥ 90% branch — Rule 05).

Métricas de sucesso:

- **Classes Java:** 22 arquivos deletados, 17 editados
- **Resources:** 2 diretórios inteiros removidos (`targets/github-copilot/`, `targets/codex/`), 17 YAMLs editados
- **Testes:** 29 classes de teste removidas, 5 classes editadas, ~8.273 golden files deletados
- **Documentação:** `CLAUDE.md` e `.claude/rules/` limpos de referências não-Claude
- **Manifest `expected-artifacts.json`** regenerado refletindo o novo escopo
- **CLI:** `--platform copilot` falha com erro claro; `--platform claude-code` e default permanecem funcionais

## Componentes do Sistema

### 1. Domain / Model — `java/src/main/java/dev/iadev/domain/model/Platform.java`

Enum central que define os valores aceitos pelo gerador. Atualmente contém `CLAUDE_CODE`, `COPILOT`, `CODEX`, `SHARED`. Após a remoção: apenas `CLAUDE_CODE` e `SHARED`.

**Referências a atualizar:**
- `allUserSelectable()` deve retornar apenas `[CLAUDE_CODE]`
- Remover constantes `COPILOT` e `CODEX` completamente

### 2. Application / AssemblerTarget — `java/src/main/java/dev/iadev/application/assembler/AssemblerTarget.java`

Enum de targets de saída, atualmente com entradas `CLAUDE(".claude")`, `GITHUB(".github")`, `CODEX(".codex")`, `CODEX_AGENTS(".agents")`. Após a remoção: apenas `CLAUDE(".claude")` (ou equivalente).

### 3. CLI — `java/src/main/java/dev/iadev/cli/`

**Arquivos afetados:**
- `PlatformConverter.java` — `ACCEPTED_VALUES` reduz de `["claude-code", "copilot", "codex", "all"]` para `["claude-code"]`. Parser falha com mensagem clara para valores antigos.
- `GenerateCommand.java` — atualizar descrição do `@Option --platform` (linhas 96-103), remover menção a copilot/codex/agents.
- `IaDevEnvApplication.java` — atualizar descrição geral (linha 25).
- `FileCategorizer.java` — remover categorização de `.github/`, `.codex/`, `.agents/` (linhas ~51-75).

### 4. Application / Assemblers GitHub (8 classes — DELETAR)

Localizadas em `java/src/main/java/dev/iadev/application/assembler/`:

- `GithubInstructionsAssembler.java`
- `GithubMcpAssembler.java`
- `GithubSkillsAssembler.java`
- `GithubAgentsAssembler.java`
- `GithubHooksAssembler.java`
- `GithubPromptsAssembler.java`
- `GithubAgentRenderer.java`
- `PrIssueTemplateAssembler.java`

**Responsabilidade:** Geração de artefatos para `.github/` (Copilot instructions, skills, agents, hooks, prompts, MCP, PR/issue templates).

### 5. Application / Assemblers Codex (7 classes — DELETAR)

- `CodexConfigAssembler.java`
- `CodexSkillsAssembler.java`
- `CodexRequirementsAssembler.java`
- `CodexOverrideAssembler.java`
- `CodexAgentsMdAssembler.java`
- `CodexScanner.java`
- `CodexShared.java`

**Responsabilidade:** Geração de artefatos para `.codex/` e `.agents/` (AGENTS.md, config.toml, requirements.toml, skills do codex).

### 6. Application / AssemblerFactory — `java/src/main/java/dev/iadev/application/assembler/AssemblerFactory.java`

**Mudanças:**
- Remover métodos `buildGithubInputAssemblers()`, `buildGithubOutputAssemblers()`, `buildCodexAssemblers()`
- Remover chamadas em `buildAllAssemblers()` (linhas ~74-78)
- Remover instanciações das 15 classes deletadas acima

### 7. Application / Classes Compartilhadas com Lógica Condicional (EDITAR)

Classes shared que hoje contêm condicionais `if (hasCopilot)`, `if (hasCodex)` ou referências diretas a targets não-Claude:

- `PlatformFilter.java` — simplificar: agora só filtra para `CLAUDE_CODE`
- `PlatformContextBuilder.java` — remover `hasCopilot`, `hasCodex` do contexto; simplificar `countActive()`
- `ReadmeAssembler.java` — remover blocos condicionais para Copilot/Codex
- `ReadmeGithubCounter.java` — **DELETAR** (contagem de GitHub deixa de existir)
- `MappingTableBuilder.java` — remover colunas `.github/`, `.codex/`, `.agents/`
- `PlanTemplatesAssembler.java` — remover constante `GITHUB_OUTPUT_SUBDIR`; copiar templates apenas para `.claude/templates/`
- `SummaryTableBuilder.java` — remover linhas de Copilot/Codex
- `EpicReportAssembler.java` — remover referências condicionais
- `FileTreeWalker.java` — remover walk para `.github/`, `.codex/`, `.agents/`

### 8. Util — `java/src/main/java/dev/iadev/util/OverwriteDetector.java`

Remover `".github"`, `".codex"`, `".agents"` da lista `ARTIFACT_DIRS`.

### 9. Validators / Parsers

- `PlatformParser.java` — remover parsing de "copilot" e "codex"
- `StackValidator.java` — remover validação específica de plataformas removidas
- `PlatformPrecedenceResolver.java` — simplificar

### 10. Resources — `java/src/main/resources/targets/` (DELETAR)

**Diretórios a deletar integralmente:**

- `java/src/main/resources/targets/github-copilot/` (~131 arquivos)
  - `agents/` (core, conditional, developers) — 23 arquivos
  - `instructions/` — 5 arquivos (.instructions.md)
  - `prompts/` — 4 arquivos (.prompt.md)
  - `skills/` — 65+ arquivos
  - `hooks/` — 3 arquivos JSON
  - `pr-issue-templates/` — 4 arquivos

- `java/src/main/resources/targets/codex/` (~15 arquivos)
  - `templates/config.toml.njk`
  - `templates/requirements.toml.njk`
  - `templates/agents*.md.njk`
  - `templates/sections/*.md.njk`

**Se existir diretório `targets/agents/` distinto do Codex:** deletar também.

### 11. Resources — `java/src/main/resources/shared/config-templates/*.yaml` (EDITAR)

17 arquivos `setup-config.{profile}.yaml` contêm comentários e valores referenciando `copilot`, `codex`, `agents`. Remover essas referências. Exemplo: `# Options: claude-code, copilot, codex, all` → `# Options: claude-code`.

### 12. Tests — Classes a DELETAR (29 arquivos)

Localizadas em `java/src/test/java/dev/iadev/application/assembler/`:

**GitHub/Copilot (14):**
- `GithubInstructionsCopilotTest`
- `GithubInstructionsFormatTest`
- `GithubInstructionsCoverageTest`
- `GithubInstructionsFileGenTest`
- `GithubInstructionsGoldenTest`
- `GithubMcpAssemblerTest`
- `GithubSkillsAssemblerTest`
- `GithubSkillsAssemblerConditionalTest`
- `GithubSkillsAssemblerIntegrationTest`
- `GithubHooksAssemblerTest`
- `GithubAgentsAssemblerTest`
- `GithubAgentsEventTest`
- `GithubAgentsConditionalTest`
- `GithubAgentsRenderCoreTest`

**Codex (6):**
- `CodexConfigAssemblerTest`
- `CodexSkillsAssemblerTest`
- `CodexRequirementsAssemblerTest`
- `CodexOverrideAssemblerTest`
- `CodexSharedTest`
- `CodexAgentsMdAssemblerTest`

**Agents (6):**
- `AgentsAssemblerTest`
- `AgentsAssemblerCoverageTest`
- `AgentsSelectionTest`
- `AgentsGoldenMatchTest`
- `AgentsConditionalGoldenTest`
- `AgentsCoreAndDevTest`

**Fixtures (2):**
- `GithubInstructionsTestFixtures.java`
- `AgentsTestFixtures.java`

### 13. Tests — Classes a EDITAR

- `PlatformDirectorySmokeTest.java` — remover `@Nested class Copilot` e `@Nested class Codex`. Remover validações negativas em `@Nested class ClaudeCode` (linhas ~88-123) que checam ausência de `.github/instructions` e `.codex/`.
- `AssemblerRegressionSmokeTest.java` — remover testes parametrizados de copilot/codex/agents.
- `CliModesSmokeTest.java` — remover `--platform copilot/codex/agents` das validações.
- `GoldenFileCoverageTest.java` — remover cobertura para plataformas removidas.
- `AssemblerTargetTest.java` — remover asserts para `GITHUB`, `CODEX`, `CODEX_AGENTS`.

### 14. Tests — Golden Files (DELETAR ~8.273 arquivos)

Para cada um dos 17 profiles em `java/src/test/resources/golden/{profile}/`:

- `.github/` (exceto `.github/workflows/` — ver RULE-003)
- `.codex/`
- `.agents/`

**Total:** 51 subdirs, ~8.273 arquivos.

Profiles afetados: go-gin, java-quarkus, java-spring (+7 variantes), kotlin-ktor, python-click-cli, python-fastapi (+1 variante), rust-axum, typescript-commander-cli, typescript-nestjs.

### 15. Tests — Manifest `expected-artifacts.json`

`java/src/test/resources/smoke/expected-artifacts.json` contém 187 referências a `.agents`, `.codex`, `.github`. Deve ser REGENERADO via `ExpectedArtifactsGenerator` após remoção dos assemblers. Conteúdo esperado pós-regeneração: ~830 arquivos por profile (vs. ~9.500 atualmente).

### 16. Documentação

- `CLAUDE.md` (raiz do projeto) — remover:
  - Seção `### .github/ (GitHub Copilot)` (linhas ~33-43)
  - Coluna `.github/` da tabela de mapeamento `.claude/ <-> .github/ <-> .codex/` (linhas ~49-58)
  - `Total .github/ artifacts: 52` (linha ~60)
  - Linhas de Copilot/Codex em `Generation Summary` (linhas ~271-277)
  - Atualizar descrição geral e totais

- `.claude/rules/*.md` — verificar e limpar referências residuais (exploração indicou nenhuma direta, mas confirmar após limpeza).

- `README.md` (raiz) — se houver referências, remover.

- `docs/` — se houver referências, remover.

## Escopo EXCLUÍDO (NÃO remover, proibido tocar)

- **`.github/workflows/`** em golden files — arquivos de CI/CD (GitHub Actions) são ORTOGONAIS ao suporte a GitHub Copilot. NÃO CONFUNDIR "Copilot" (assistente de IA) com ".github/workflows/" (pipeline de CI/CD). Esta distinção é crítica.
- **Templates compartilhados em `resources/shared/templates/`** — usados pelo Claude para gerar artefatos de planning, arquitetura, review, etc. Manter intactos.
- **Lógica shared/common de assemblers** — classes base (como `Assembler` interface, `AssemblerPipeline`, `AssemblerDescriptor`) e templates shared permanecem.
- **Testes de Claude Code** — todos os testes que validam geração para `.claude/` devem ser mantidos intactos.
- **Domain models não relacionados a plataforma** — `ProjectConfig`, `ProjectStack`, etc.
- **Outros branches / PRs em andamento** — não interferir em trabalhos paralelos.

## Mudanças Necessárias

### MUD-01: Remoção Completa de GitHub Copilot

**Escopo atômico (mesmo commit/story):**

- Deletar 8 classes Java GitHub/Copilot assemblers (Seção 4)
- Deletar 14 classes de teste Github* (Seção 12)
- Deletar fixture `GithubInstructionsTestFixtures.java`
- Deletar diretório `java/src/main/resources/targets/github-copilot/` (~131 arquivos)
- Deletar subdirs `.github/` em 17 golden profiles (exceto `.github/workflows/`)
- Atualizar `Platform.java` — remover constante `COPILOT`
- Atualizar `AssemblerTarget.java` — remover `GITHUB(".github")`
- Atualizar `PlatformConverter.java` — remover `"copilot"` dos `ACCEPTED_VALUES`
- Atualizar `GenerateCommand.java` — atualizar descrição do `--platform`
- Atualizar `AssemblerFactory.java` — remover `buildGithubInputAssemblers()`, `buildGithubOutputAssemblers()`
- Atualizar `FileCategorizer.java` — remover `.github/` (mas manter `.github/workflows/` se houver categorização específica)
- Atualizar `OverwriteDetector.java` — remover `".github"` do `ARTIFACT_DIRS`
- Limpar 17 YAMLs `setup-config.*.yaml` de referências a copilot
- Verificar: `mvn clean verify` verde antes do commit

### MUD-02: Remoção Completa de Codex

**Escopo atômico:**

- Deletar 7 classes Java Codex assemblers (Seção 5)
- Deletar 6 classes de teste Codex* (Seção 12)
- Deletar diretório `java/src/main/resources/targets/codex/` (~15 arquivos)
- Deletar subdirs `.codex/` em 17 golden profiles
- Atualizar `Platform.java` — remover constante `CODEX`
- Atualizar `AssemblerTarget.java` — remover `CODEX(".codex")`
- Atualizar `PlatformConverter.java` — remover `"codex"` dos `ACCEPTED_VALUES`
- Atualizar `AssemblerFactory.java` — remover `buildCodexAssemblers()`
- Atualizar `FileCategorizer.java` — remover `.codex/`
- Atualizar `OverwriteDetector.java` — remover `".codex"`
- Limpar YAMLs de referências a codex
- Verificar: `mvn clean verify` verde antes do commit

### MUD-03: Remoção Completa de Agents Genérico

**Escopo atômico:**

- Deletar 6 classes de teste Agents* (Seção 12)
- Deletar fixture `AgentsTestFixtures.java`
- Deletar diretório `targets/agents/` se existir (e for distinto de Codex)
- Deletar subdirs `.agents/` em 17 golden profiles
- Atualizar `AssemblerTarget.java` — remover `CODEX_AGENTS(".agents")`
- Atualizar `PlatformFilter.java` — simplificar (remover filtro para agents)
- Atualizar `PlatformConverter.java` — remover `"agents"` se existir
- Atualizar `FileCategorizer.java` — remover `.agents/`
- Atualizar `OverwriteDetector.java` — remover `".agents"`
- Verificar: `mvn clean verify` verde antes do commit

### MUD-04: Higienização de Classes Compartilhadas

**Escopo:**

- Editar `ReadmeAssembler.java` — remover blocos condicionais `hasCopilot`/`hasCodex`
- **Deletar** `ReadmeGithubCounter.java` — classe dedicada à contagem de GitHub deixa de fazer sentido
- Editar `MappingTableBuilder.java` — remover colunas de Copilot/Codex/agents da tabela
- Editar `SummaryTableBuilder.java` — remover linhas de Copilot/Codex
- Editar `PlatformContextBuilder.java` — remover `hasCopilot`, `hasCodex` do context; simplificar `countActive()` se existir
- Editar `PlanTemplatesAssembler.java` — remover constante `GITHUB_OUTPUT_SUBDIR`; copiar templates apenas para `.claude/templates/`
- Editar `EpicReportAssembler.java` — remover referências condicionais
- Editar `FileTreeWalker.java` — remover lógica de walk para `.github/`, `.codex/`, `.agents/`
- Editar `PlatformParser.java`, `StackValidator.java`, `PlatformPrecedenceResolver.java` — simplificar
- Editar smoke tests: `PlatformDirectorySmokeTest` (remover `@Nested Copilot`, `@Nested Codex`), `AssemblerRegressionSmokeTest`, `CliModesSmokeTest`, `GoldenFileCoverageTest`, `AssemblerTargetTest`
- Editar `IaDevEnvApplication.java` — atualizar descrição geral
- Verificar: `mvn clean verify` verde antes do commit

### MUD-05: Documentação e Verificação Final

**Escopo:**

- Atualizar `/Users/edercnj/workspaces/ia-dev-environment/CLAUDE.md`:
  - Remover seção `### .github/ (GitHub Copilot)`
  - Remover coluna `.github/` da tabela de mapeamento
  - Remover linhas `.codex/` e `.agents/` da tabela
  - Remover `Total .github/ artifacts: 52`
  - Remover componentes Copilot/Codex do `Generation Summary`
  - Atualizar descrição geral (`.claude/` é o único target)
- Limpar `.claude/rules/*.md` de referências residuais
- Atualizar `README.md` (raiz) se houver referências
- Atualizar `docs/` se houver referências
- Regenerar `java/src/test/resources/smoke/expected-artifacts.json` via `ExpectedArtifactsGenerator.generate()`
- Executar `mvn clean verify` completo
- Validar coverage ≥ 95% line / ≥ 90% branch
- Grep sanity check: `grep -r "copilot\|codex\|\\.agents/" java/src/main` deve retornar zero ocorrências (exceto `.github/workflows/` legítimo)
- Criar PR final para `develop`

## Regras de Negócio Transversais

### RULE-001: Build Sempre Verde Entre Stories

Cada story individual DEVE deixar o repositório em estado compilável e com todos os testes remanescentes passando. É proibido deixar breakage intermediário entre stories.

**Rationale:** Permite que qualquer commit intermediário seja um ponto seguro de revisão ou rollback.

**Enforcement:** Cada story executa `mvn clean verify` como última etapa antes do PR. Falha bloqueia merge.

### RULE-002: Coverage Não Pode Degradar

Coverage line e branch não podem cair mais que 2 pontos percentuais vs. baseline pré-épico. Threshold mínimo absoluto: ≥ 95% line, ≥ 90% branch (Rule 05 do projeto).

**Rationale:** Deleção de testes é aceitável APENAS quando proporcional à deleção do código que eles cobriam. Deleção de código sem deleção de testes, ou vice-versa, quebra esta regra.

**Enforcement:** Relatório de coverage exigido no PR de cada story e verificado manualmente na story-0034-0005.

### RULE-003: `.github/workflows/` é PROTEGIDO

`.github/workflows/` contém pipelines de CI/CD (GitHub Actions) que existem INDEPENDENTEMENTE do suporte a GitHub Copilot. NUNCA deletar. NUNCA confundir com Copilot.

**Rationale:** "GitHub Copilot" (assistente de IA) e "GitHub Actions workflows" (CI/CD) compartilham o prefixo `.github/` mas são tecnologicamente ortogonais. Remover workflows quebra CI/CD.

**Enforcement:** Ao deletar subdirs `.github/` em golden files, aplicar exclusão explícita para `workflows/`. Grep final não deve reportar falsos positivos sobre workflows.

### RULE-004: Templates em `resources/shared/` são PROTEGIDOS

Todos os templates em `java/src/main/resources/shared/templates/` (incluindo `_TEMPLATE-EPIC.md`, `_TEMPLATE-STORY.md`, `_TEMPLATE-IMPLEMENTATION-MAP.md`, templates de planning, review, etc.) são usados pelo Claude Code. Manter intactos.

**Rationale:** O fato de que esses templates eram previamente copiados para múltiplos targets não significa que eram "de Copilot/Codex" — eles são compartilhados. Após a remoção, continuam sendo usados pelo Claude Code.

**Enforcement:** Nenhuma story pode deletar arquivos de `resources/shared/templates/`. Apenas `PlanTemplatesAssembler` é editado para parar de copiá-los para `.github/templates/`.

### RULE-005: Remoção Atômica por Target

Cada target (GitHub, Codex, Agents) é removido em uma story atômica que inclui: código Java + resources + testes + golden files + referências em enums. Não é permitido remover código sem testes, ou enum sem classes dependentes.

**Rationale:** Atomicidade garante build verde entre stories e facilita rollback se necessário.

**Enforcement:** DoR de cada story (MUD-01, MUD-02, MUD-03) inclui checklist de todos os itens atômicos.

### RULE-006: TDD Compliance

Embora esta seja uma story de REMOÇÃO (não adição de features), o princípio TDD se aplica à verificação: antes de deletar um teste, confirmar que ele estava passando (red-to-green → green-to-removed). Nenhum teste pode ser deletado "porque está quebrado" — consertar ou reverter.

**Rationale:** Evita esconder regressões sob o pretexto de limpeza.

**Enforcement:** PR review verifica que deletes de testes são acompanhados de deletes proporcionais de classes produção.

## Dependências entre MUDs

```
MUD-01 (GitHub Copilot)   ┐
MUD-02 (Codex)            ├─→ MUD-04 (Higienização) ─→ MUD-05 (Docs + verificação)
MUD-03 (Agents)           ┘
```

- MUD-01, MUD-02, MUD-03 são independentes entre si (touch diferentes assemblers) mas editam os mesmos enums (`Platform`, `AssemblerTarget`). Executar SEQUENCIALMENTE para evitar conflitos de merge: 01 → 02 → 03.
- MUD-04 depende de 01, 02, 03 estarem completos (caso contrário, lógica condicional ainda é necessária).
- MUD-05 depende de MUD-04 (docs refletem o estado final).

## Critérios de Aceite Globais

- [ ] `mvn clean verify` verde em todas as stories e no final do épico
- [ ] `grep -r "GithubInstructionsAssembler\|CodexConfigAssembler\|AgentsAssembler" java/src/main/java` retorna zero ocorrências
- [ ] `grep -r "\\.codex/\|\\.agents/" java/src/main` retorna zero ocorrências
- [ ] `grep -r "COPILOT\|CODEX\|CODEX_AGENTS" java/src/main/java/dev/iadev/domain/model/Platform.java` retorna zero ocorrências
- [ ] `.github/workflows/` em golden files está intacto onde existia (RULE-003)
- [ ] Coverage ≥ 95% line / ≥ 90% branch
- [ ] `java -jar target/ia-dev-env.jar generate --platform copilot` falha com erro claro
- [ ] `java -jar target/ia-dev-env.jar generate --platform claude-code` funciona
- [ ] `java -jar target/ia-dev-env.jar generate` (sem flag) funciona
- [ ] Contagem de arquivos gerados para profile `java-spring` caiu de ~9.500 para ~830
- [ ] `expected-artifacts.json` regenerado refletindo o novo escopo
- [ ] `CLAUDE.md` raiz atualizado (sem menções a Copilot/Codex/agents, exceto workflows)
- [ ] `.claude/rules/*.md` limpo
- [ ] PR final aprovado e mergeado em `develop`

## Interfaces

### CLI (ponto de entrada)

**Antes:**
```
Usage: ia-dev-env generate [--platform=<platform>] ...
  --platform=<platform>   claude-code, copilot, codex, all
```

**Depois:**
```
Usage: ia-dev-env generate [--platform=<platform>] ...
  --platform=<platform>   claude-code (default)
```

### Assembler Pipeline

**Antes:** `AssemblerFactory.buildAllAssemblers()` instancia ~34 assemblers (Claude + GitHub + Codex + Agents).
**Depois:** Instancia apenas assemblers Claude + shared (~19 classes).

### Output Layout

**Antes:** `.claude/`, `.github/`, `.codex/`, `.agents/` gerados em paralelo para cada profile.
**Depois:** Apenas `.claude/` gerado. Estrutura interna inalterada.

## Glossário

- **Target:** Plataforma de saída do gerador. Ex: Claude Code, Copilot.
- **Assembler:** Classe Java responsável por montar um conjunto de arquivos de saída (ex: skills, rules, agents) para um target.
- **Profile:** Combinação pré-definida de stack técnico usada como fixture de teste (ex: `java-spring`, `python-fastapi`). 17 profiles ativos.
- **Golden file:** Snapshot de saída esperada usado para validar que regenerações não introduziram regressão byte-a-byte.
- **`.github/workflows/`:** Diretório de pipelines GitHub Actions. **NÃO confundir com Copilot.**
