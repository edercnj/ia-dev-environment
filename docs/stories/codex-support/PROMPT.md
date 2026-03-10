# Prompt: Suporte ao OpenAI Codex no ia-dev-environment

## Contexto do Projeto

O **ia-dev-environment** é um gerador de scaffolding reutilizável e project-agnostic que produz diretórios `.claude/` e `.github/` completos para projetos com desenvolvimento assistido por IA. É uma CLI que recebe configuração YAML e gera regras, skills, agents, configurações, patterns, protocols e hooks.

**Migração em andamento:** O projeto está sendo migrado de Python para Node.js com TypeScript (ver EPIC-001 em `stories/migration-python-to-node-ts/`). As novas funcionalidades descritas neste épico devem ser implementadas **em TypeScript**, seguindo os mesmos padrões e arquitetura definidos na migração.

**Versão atual:** 0.1.0
**Linguagem alvo:** TypeScript 5
**Framework CLI:** Commander
**Template Engine:** Nunjucks
**Test Runner:** Vitest

---

## Objetivo

Estender o ia-dev-environment para gerar, além dos diretórios `.claude/` e `.github/`, também o diretório `.codex/` para projetos que utilizam o **OpenAI Codex CLI**. O gerador deve produzir configurações equivalentes adaptadas ao ecossistema Codex, mantendo consistência semântica com os artefatos já gerados para Claude Code e GitHub Copilot.

### Goals

1. **Geração de `.codex/AGENTS.md`** — Consolidar rules, architecture, coding standards, domain e agents em um único Markdown de instrução de projeto
2. **Geração de `.codex/config.toml`** — Produzir configuração de projeto (model, approval policy, sandbox, history, MCP)
3. **Geração de skills Codex** — Copiar/adaptar skills do formato Claude Code para o diretório de skills Codex
4. **Mapeamento semântico** — Traduzir conceitos entre ecossistemas (rules → seções AGENTS.md, agents → agent personas, hooks → approval policies, settings → config.toml)
5. **Config YAML inalterada** — Nenhuma alteração no formato de configuração YAML existente; o Codex é mais um target de output
6. **Cobertura de testes** — ≥ 95% line, ≥ 90% branch para todo código novo

### Non-Goals

- Modificar o formato YAML de configuração (v3)
- Alterar a geração existente de `.claude/` ou `.github/`
- Implementar MCP servers para Codex (apenas a configuração TOML de referência)
- Gerar scripts executáveis para skills Codex
- Implementar multi-agent orchestration real (apenas documentar agent personas)

---

## Ecossistema Codex — Referência Técnica

### Visão Geral do Codex CLI

O OpenAI Codex CLI (anteriormente "codex-rs") é uma ferramenta de terminal para desenvolvimento assistido por IA com modelos OpenAI. A configuração de projeto segue um modelo baseado em:

1. **AGENTS.md** — Instruções em Markdown (equivalente a CLAUDE.md + rules/)
2. **config.toml** — Configuração estruturada (equivalente a settings.json)
3. **Skills** — Mesma estrutura de SKILL.md do Claude Code

### Estrutura de Arquivos Codex

```
project-root/
├── .codex/
│   ├── AGENTS.md              # Instruções de projeto (equivale a rules/ + CLAUDE.md)
│   └── config.toml            # Configurações do projeto (model, approval, sandbox, MCP)
├── AGENTS.md                  # Opcional: instruções de nível raiz (como CLAUDE.md)
└── source code...
```

> **Resolução de AGENTS.md:** O Codex CLI resolve AGENTS.md em cascata:
> 1. `~/.codex/AGENTS.md` — Instruções globais pessoais (fora do escopo do gerador)
> 2. `{repo-root}/AGENTS.md` — Instruções compartilhadas do projeto
> 3. `{repo-root}/.codex/AGENTS.md` — Instruções scoped do projeto
> 4. `{current-dir}/AGENTS.md` — Instruções específicas de subdiretório
>
> O gerador produz `.codex/AGENTS.md` (opção 3), que é o equivalente mais próximo às rules do Claude Code.

### Mapeamento Claude Code → Codex

| Conceito Claude Code | Localização Claude | Equivalente Codex | Localização Codex |
|---|---|---|---|
| Executive Summary | `CLAUDE.md` | Seção Overview do AGENTS.md | `.codex/AGENTS.md` |
| Rules (system prompt) | `.claude/rules/*.md` | Seções consolidadas no AGENTS.md | `.codex/AGENTS.md` |
| Skills (invocáveis) | `.claude/skills/{name}/SKILL.md` | Referência no AGENTS.md (seção Skills) | `.codex/AGENTS.md` |
| Knowledge Packs | `.claude/skills/{name}/SKILL.md` (user-invocable: false) | Seção de referências no AGENTS.md | `.codex/AGENTS.md` |
| Agents (personas) | `.claude/agents/*.md` | Agent personas no AGENTS.md | `.codex/AGENTS.md` |
| Hooks (post-tool) | `.claude/hooks/*.sh` | Referência na seção Build & Test | `.codex/AGENTS.md` |
| Settings (permissions) | `.claude/settings.json` | Config TOML (approval, sandbox) | `.codex/config.toml` |
| GitHub instructions | `.github/copilot-instructions.md` | N/A (AGENTS.md cobre tudo) | — |

### AGENTS.md — Formato e Conteúdo

O `AGENTS.md` é Markdown livre (sem schema rígido). O Codex CLI carrega o conteúdo inteiro como contexto para o modelo. A estrutura recomendada para o gerador:

```markdown
# {project_name}

> {project_purpose}

## Architecture

**Style:** {architecture_style}
**Domain-Driven Design:** {domain_driven}
**Event-Driven:** {event_driven}

### Package Structure

```
{package_structure_from_architecture_rule}
```

### Dependency Rules

{dependency_direction_rules}

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | {language} {version} |
| Framework | {framework} {version} |
| Build Tool | {build_tool} |
| Database | {database} |
| Cache | {cache} |
| Container | {container} |
| Orchestrator | {orchestrator} |
| Observability | {observability} |

## Build & Test Commands

| Command | Description |
|---------|-------------|
| `{build_cmd}` | Build |
| `{test_cmd}` | Run tests |
| `{compile_cmd}` | Type check / compile |
| `{coverage_cmd}` | Test with coverage |

## Coding Standards

### Hard Limits

| Constraint | Limit |
|-----------|-------|
| Method/function length | ≤ 25 lines |
| Class/module length | ≤ 250 lines |
| Parameters per function | ≤ 4 |
| Line width | ≤ 120 characters |

### SOLID Principles

{solid_summary}

### Error Handling

{error_handling_rules}

### Forbidden Patterns

{forbidden_patterns}

## Quality Gates

| Metric | Minimum |
|--------|---------|
| Line Coverage | ≥ {coverage_line}% |
| Branch Coverage | ≥ {coverage_branch}% |

### Test Categories

{test_categories}

### Test Naming

```
[methodUnderTest]_[scenario]_[expectedBehavior]
```

## Domain

{domain_overview_if_domain_driven}

## Security

{security_guidelines_if_present}

## Conventions

- **Commits:** Conventional Commits (English)
- **Branches:** feature/, fix/, chore/
- **Code language:** English (classes, methods, variables)
- **Documentation:** English

## Agent Personas

{foreach_agent}
### {agent_name}
{agent_description_and_role}
{end_foreach}
```

**Pontos importantes:**
- O AGENTS.md **consolida** informações de múltiplas rules do Claude em seções de um único arquivo
- Seções são **condicionais**: Domain só aparece se `domain_driven=true`, Security só se security frameworks presentes, etc.
- O conteúdo é **derivado** dos mesmos templates/resources já usados para gerar rules — não é duplicação, é consolidação

### config.toml — Formato e Referência

O `config.toml` do Codex CLI suporta as seguintes seções:

```toml
# .codex/config.toml — Project-scoped configuration

# Model to use (default: o4-mini)
model = "o4-mini"

# Approval mode: "suggest" | "auto-edit" | "full-auto"
# - suggest: reads freely, approval for all writes and commands
# - auto-edit: auto-applies file patches, approval for commands
# - full-auto: unrestricted in sandbox (network disabled, writes confined)
approval_policy = "suggest"

[sandbox]
# Writable paths (globs) — beyond the project root
# writable_roots = []

[history]
# max_size = 1000
# save_history = true
# sensitive_patterns = []

# MCP servers (if project uses MCP)
# [[mcp_servers]]
# name = "server-name"
# transport = "stdio"
# command = ["npx", "-y", "@scope/mcp-server"]
# env = {}
```

**Estratégia de geração:**
- `model` → default "o4-mini" (pode ser parametrizado futuramente)
- `approval_policy` → derivar de hooks/settings: projetos com hooks pós-compilação → "auto-edit"; projetos sem hooks → "suggest"
- `[history]` → default (sem personalização por projeto)
- `[[mcp_servers]]` → gerar seção se `config.mcp.servers` não estiver vazio no YAML

### Comparação de Skills: Claude Code vs Codex

O formato de SKILL.md é **semanticamente compatível** entre Claude Code e Codex:

**Claude Code:**
```markdown
---
name: skill-name
description: Short description
user-invocable: true|false
---
# Skill: {name}
{instructions}
```

**Codex (via AGENTS.md):**
No Codex, não existe um diretório dedicado de skills separado do AGENTS.md. O equivalente é documentar skills como seções de referência no AGENTS.md, ou usar o padrão de `AGENTS.md` em subdiretórios para skills específicas.

Para o gerador, a abordagem é: listar skills disponíveis como seção do AGENTS.md com nome, descrição e quando usar.

---

## Arquitetura da Solução

### Novos Assemblers

| Assembler | Output | Responsabilidade |
|---|---|---|
| **CodexAgentsMdAssembler** | `.codex/AGENTS.md` | Consolida rules, architecture, coding standards, quality gates, domain, security, agents em um único Markdown estruturado |
| **CodexConfigAssembler** | `.codex/config.toml` | Gera config.toml com model, approval_policy, sandbox e MCP derivados da config YAML |

### Pipeline Expandido (16 → 18 Assemblers)

Ordem atualizada — novos assemblers após GitHub, antes do Readme:

```
 1. RulesAssembler
 2. SkillsAssembler
 3. AgentsAssembler
 4. PatternsAssembler
 5. ProtocolsAssembler
 6. HooksAssembler
 7. SettingsAssembler
 8. GithubInstructionsAssembler
 9. GithubMcpAssembler
10. GithubSkillsAssembler
11. GithubAgentsAssembler
12. GithubHooksAssembler
13. GithubPromptsAssembler
14. CodexAgentsMdAssembler      ← NEW
15. CodexConfigAssembler         ← NEW
16. ReadmeAssembler              ← UPDATED (inclui contagem Codex)
```

> **Nota:** O ReadmeAssembler existente deve ser atualizado para incluir contagem de artefatos Codex na tabela de sumário e na seção de mapping .claude ↔ .github ↔ .codex.

### Novos Resources (Templates)

```
resources/
├── codex-templates/
│   ├── agents-md.md.njk               # Template principal do AGENTS.md
│   ├── config.toml.njk                # Template do config.toml
│   └── sections/                       # Seções modulares do AGENTS.md
│       ├── header.md.njk              # Título + purpose
│       ├── architecture.md.njk        # Style, package structure, dependency rules
│       ├── tech-stack.md.njk          # Tabela de tecnologias
│       ├── commands.md.njk            # Build, test, compile, coverage
│       ├── coding-standards.md.njk    # Hard limits, SOLID, error handling, forbidden
│       ├── quality-gates.md.njk       # Coverage thresholds, test categories
│       ├── domain.md.njk              # Domain model (condicional: domain_driven)
│       ├── security.md.njk            # Security guidelines (condicional: security.frameworks)
│       ├── conventions.md.njk         # Commits, branches, language
│       ├── skills.md.njk              # Lista de skills disponíveis
│       └── agents.md.njk              # Agent personas
```

### Impacto em Módulos Existentes

| Módulo | Tipo de Mudança | Descrição |
|---|---|---|
| `src/assembler/index.ts` | **Modificação** | Adicionar 2 novos assemblers ao pipeline (posição 14-15) |
| `src/assembler/readme-assembler.ts` | **Modificação** | Incluir contagem de artefatos Codex na tabela de sumário |
| `src/domain/resolver.ts` | **Nenhuma** | ResolvedStack já fornece commands e ports necessários |
| `src/domain/skill-registry.ts` | **Nenhuma** | Skills já registradas; apenas leitura |
| `src/models.ts` | **Nenhuma** | ProjectConfig já tem todos os dados necessários |
| `resources/` | **Adição** | Novo diretório `codex-templates/` com templates Nunjucks |

### Dados Consumidos pelos Templates Codex

Os templates Codex consomem **exatamente os mesmos dados** já disponíveis:

**Do ProjectConfig (via context flat de 25 campos):**
- project_name, project_purpose
- language_name, language_version, framework_name, framework_version, build_tool
- architecture_style, domain_driven, event_driven
- database_name, cache_name
- container, orchestrator, observability
- coverage_line, coverage_branch
- smoke_tests, contract_tests, performance_tests

**Do ResolvedStack:**
- build_cmd, test_cmd, compile_cmd, coverage_cmd
- docker_base_image, health_path, default_port
- file_extension, build_file

**Do Pipeline (artefatos já gerados):**
- Lista de rules (de RulesAssembler)
- Lista de skills com nomes e descrições (de SkillsAssembler)
- Lista de agents com nomes (de AgentsAssembler)
- Informações de hooks (de HooksAssembler)
- MCP config (de ProjectConfig.mcp)

---

## Dependências com EPIC-001 (Migração Python → Node/TS)

Este épico **depende** da conclusão de stories específicas do EPIC-001:

| Dependência EPIC-001 | Motivo | Mínimo Necessário |
|---|---|---|
| STORY-001 (Setup projeto) | Infraestrutura base TypeScript | Compilação + testes funcionando |
| STORY-003 (Models) | Interfaces ProjectConfig, ResolvedStack | Tipos compiláveis |
| STORY-005 (Template Engine) | Nunjucks para renderizar templates Codex | render_template funcional |
| STORY-006 (Domain Mappings) | Stack mappings para commands e ports | Constantes exportadas |
| STORY-007 (Validator/Resolver) | ResolvedStack para commands | resolve_stack funcional |
| STORY-008 (Assembler Helpers) | copy_helpers, conditions | Helpers exportados |
| STORY-009 (RulesAssembler) | Conteúdo de rules como fonte para AGENTS.md | Assembler funcional |
| STORY-010 (SkillsAssembler) | Lista de skills para seção do AGENTS.md | Assembler funcional |
| STORY-011 (AgentsAssembler) | Agent definitions para seção do AGENTS.md | Assembler funcional |
| STORY-016 (Pipeline) | Integração dos novos assemblers | Pipeline extensível |

**Estratégia de intercalação:** As stories deste épico podem iniciar assim que as stories do EPIC-001 Phase 1-3 estiverem concluídas (STORY-001..008). Os assemblers Codex podem ser desenvolvidos **em paralelo** com os assemblers Claude/GitHub da Phase 4 do EPIC-001, desde que os helpers e domain layer estejam prontos. A integração no pipeline (STORY-016 do EPIC-001) é o ponto de convergência final.

---

## Instruções para Decomposição

### Templates a Seguir

Os arquivos gerados devem seguir exatamente estes templates:
- **Épico:** `resources/templates/_TEMPLATE-EPIC.md`
- **Histórias:** `resources/templates/_TEMPLATE-STORY.md`
- **Mapa de Implementação:** `resources/templates/_TEMPLATE-IMPLEMENTATION-MAP.md`

### Filosofia de Decomposição

- **Layer 0 (Foundation):** Templates Nunjucks para Codex (resources) + análise de mapeamento semântico
- **Layer 1 (Core Assemblers):** CodexAgentsMdAssembler (o maior e mais complexo) e CodexConfigAssembler
- **Layer 2 (Integration):** Atualização do Pipeline Orchestrator e ReadmeAssembler para incluir Codex
- **Layer 3 (Validation):** Testes de integração, verificação de output, documentação

### Regras de Negócio Transversais (Candidatas a RULE-NNN)

Ao gerar o épico, extrair como regras transversais:

1. **AGENTS.md Consolidation** — O AGENTS.md deve consolidar informações de rules (01-project-identity, 03-coding-standards, 04-architecture-summary, 05-quality-gates), domain rule, agents e skills em um único Markdown coerente. Cada conceito aparece **uma única vez** — sem duplicação entre seções. Seções são modulares e condicionalmente incluídas/excluídas baseado no config.

2. **Conditional Sections** — Seções do AGENTS.md são condicionais:
   - Domain → somente se `architecture.domain_driven = true`
   - Security → somente se `security.frameworks` não vazio
   - Database/Cache (em Tech Stack) → somente se != "none"
   - MCP (em config.toml) → somente se `mcp.servers` não vazio
   - Agent Personas → somente se agents foram gerados (sempre true no momento)

3. **Config Derivation** — O `config.toml` deve ser derivado deterministicamente da config YAML do projeto:
   - `model` → default "o4-mini" (hardcoded)
   - `approval_policy` → "auto-edit" se hooks presentes, "suggest" caso contrário
   - `[[mcp_servers]]` → mapeamento 1:1 de `config.mcp.servers`
   - Sem inputs adicionais do usuário

4. **Template Modularity** — O AGENTS.md é composto por seções modulares (templates `.njk` individuais em `sections/`). O template principal (`agents-md.md.njk`) inclui cada seção condicionalmente via `{% include %}` com guards `{% if %}`.

5. **No Impact on Existing Output** — A adição de output Codex NÃO deve alterar nenhum byte do output `.claude/` ou `.github/` existente. Os novos assemblers são **adicionais** ao pipeline, não substitutos.

6. **Pipeline Extension Pattern** — Novos assemblers devem:
   - Implementar a mesma interface Assembler existente
   - Ser inseridos após GithubPromptsAssembler e antes de ReadmeAssembler
   - Receber os mesmos parâmetros (config, outputDir, resourcesDir, engine)
   - Retornar `{ files: string[]; warnings: string[] }`

7. **Placeholder Parity** — Os mesmos 25 campos de placeholder context (definidos em `template-engine.ts`) devem ser usados nos templates Codex. Nenhum campo novo é necessário para a geração base.

8. **Extended Context for AGENTS.md** — Além do context flat de 25 campos, o CodexAgentsMdAssembler precisa de dados adicionais que **já existem** no pipeline:
   - `ResolvedStack` (build_cmd, test_cmd, compile_cmd, coverage_cmd)
   - Lista de agents gerados (lida do diretório de output após AgentsAssembler)
   - Lista de skills geradas (lida do diretório de output após SkillsAssembler)

9. **Feature Gating Codex** — A geração de artefatos Codex deve respeitar o mesmo feature gating existente. Exemplo: se database é "none", a seção de database não aparece no AGENTS.md. Se não há interfaces de messaging, seção de event-driven não aparece.

10. **TOML via Template** — O config.toml deve ser gerado via template Nunjucks (não serialização programática de TOML), para manter controle total do formato de saída e comentários inline.

11. **ReadmeAssembler Update** — O ReadmeAssembler deve ser atualizado para:
    - Contar artefatos Codex (AGENTS.md + config.toml = 2 artefatos mínimos)
    - Adicionar coluna Codex na tabela de mapping
    - Incluir seção Codex na tabela de Generation Summary

### Restrições Técnicas para as Histórias

Cada história deve especificar:

1. **Módulos TypeScript de destino** — caminho proposto no projeto
2. **Templates Nunjucks necessários** — quais templates criar em `resources/codex-templates/`
3. **Dependências com EPIC-001** — quais stories do EPIC-001 são pré-requisito
4. **Impacto em módulos existentes** — quais módulos existentes precisam ser modificados (se algum)
5. **Dados consumidos** — quais campos do context/ResolvedStack/pipeline são usados
6. **Testes** — cenários Gherkin + testes de integração + testes de output

### Estrutura Proposta dos Novos Arquivos

```
src/
├── assembler/
│   ├── codex-agents-md-assembler.ts    # Gera .codex/AGENTS.md
│   └── codex-config-assembler.ts       # Gera .codex/config.toml
resources/
├── codex-templates/
│   ├── agents-md.md.njk               # Template principal (inclui sections/)
│   ├── config.toml.njk                # Template do config.toml
│   └── sections/                       # Seções modulares do AGENTS.md
│       ├── header.md.njk
│       ├── architecture.md.njk
│       ├── tech-stack.md.njk
│       ├── commands.md.njk
│       ├── coding-standards.md.njk
│       ├── quality-gates.md.njk
│       ├── domain.md.njk
│       ├── security.md.njk
│       ├── conventions.md.njk
│       ├── skills.md.njk
│       └── agents.md.njk
tests/
├── assembler/
│   ├── codex-agents-md-assembler.test.ts
│   └── codex-config-assembler.test.ts
```

---

## Output Esperado

Gerar dentro de `stories/codex-support/`:

1. **`EPIC-002.md`** — Épico completo seguindo o formato do EPIC-001
2. **`STORY_STORY-NNN_*.md`** — Uma história por story (formato idêntico às stories do EPIC-001)
3. **`IMPLEMENTATION-MAP.md`** — Mapa de implementação com fases, dependências e caminho crítico

### Decomposição Sugerida de Stories

| # | Título Sugerido | Escopo |
|---|---|---|
| STORY-021 | Templates Nunjucks para Codex | Criar todos os templates em `resources/codex-templates/` |
| STORY-022 | CodexAgentsMdAssembler | Implementar assembler que gera `.codex/AGENTS.md` |
| STORY-023 | CodexConfigAssembler | Implementar assembler que gera `.codex/config.toml` |
| STORY-024 | Pipeline + ReadmeAssembler Update | Integrar novos assemblers no pipeline e atualizar Readme |
| STORY-025 | Testes de Integração Codex | Testes end-to-end de geração Codex com múltiplas configs |

> **Nota:** Esta é uma sugestão. A decomposição final deve ser determinada pela análise de complexidade e dependências.

### Critérios de Qualidade

- [ ] Toda regra no épico é referenciada por pelo menos uma história
- [ ] Toda história referencia pelo menos uma regra
- [ ] Dependências são simétricas (A blocks B ↔ B blocked by A)
- [ ] Sem dependências circulares
- [ ] Dependências com EPIC-001 estão explícitas em cada história
- [ ] Fases respeitam dependências (stories só entram em fase quando TODAS deps resolvidas)
- [ ] Cada história tem pelo menos 4 cenários Gherkin
- [ ] Templates Nunjucks estão especificados com variáveis esperadas
- [ ] Impacto em módulos existentes está claro em cada história
- [ ] Output `.claude/` e `.github/` não é afetado (testes de regressão)

### Numeração

- Épico: **EPIC-002**
- Stories: Iniciar em **STORY-021** (continuação da numeração do EPIC-001 que vai até STORY-020)
- Rules: Iniciar em **RULE-101** (novo range para evitar conflito com RULE-001..015 do EPIC-001)

### Regras de Idioma

- Todo conteúdo gerado em **Português Brasileiro (pt-BR)**
- Termos técnicos em inglês permanecem em inglês (assembler, pipeline, template engine, skill, knowledge pack, config, approval policy, sandbox, etc.)
- Identificadores de código, nomes de campo, valores enum em inglês
- Gherkin em português: `Cenario`, `DADO`, `QUANDO`, `ENTÃO`, `E`, `MAS`
- IDs em formato inglês: RULE-NNN, STORY-NNN, EPIC-NNN

---

## Referências

- EPIC-001 (Migração Python → Node/TS): `stories/migration-python-to-node-ts/EPIC-001.md`
- IMPLEMENTATION-MAP do EPIC-001: `stories/migration-python-to-node-ts/IMPLEMENTATION-MAP.md`
- Código-fonte TypeScript (em migração): `src/`
- Código-fonte Python (referência): `src/ia_dev_env/`
- Resources (templates): `resources/`
- OpenAI Codex CLI docs: https://developers.openai.com/codex/config-basic/
- OpenAI Codex AGENTS.md guide: https://developers.openai.com/codex/guides/agents-md/
- OpenAI Codex config reference: https://developers.openai.com/codex/config-reference/
- OpenAI Codex config sample: https://developers.openai.com/codex/config-sample/
