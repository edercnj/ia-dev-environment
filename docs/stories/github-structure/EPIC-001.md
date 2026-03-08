# Épico: Gerador de Estrutura `.github/` para GitHub Copilot

**Autor:** Product Owner / Architect
**Data:** 2026-03-08
**Versão:** 3.0
**Status:** Em Refinamento

## 1. Visão Geral

**Chave Jira:** EPIC-001

Este épico trata da extensão do gerador Python `claude_setup` para produzir, além da estrutura `.claude/` já existente, a estrutura completa `.github/` compatível com GitHub Copilot. O `claude_setup` é uma CLI que lê arquivos de configuração YAML e, por meio de um pipeline de assemblers, gera deterministicamente todos os artefatos de configuração para AI coding assistants. Atualmente o gerador já produz `.claude/rules/`, `.claude/skills/`, `.claude/agents/`, `.claude/hooks/`, `.claude/settings.json`, patterns, protocols e README. Com a STORY-001 já implementada, o `GithubInstructionsAssembler` gera `.github/copilot-instructions.md` e `.github/instructions/*.instructions.md`. As demais histórias deste épico adicionam novos assemblers para cobrir skills, agents, hooks, prompts e MCP do Copilot — garantindo que o mesmo repositório sirva como environment configuration para **ambas** as ferramentas com o mesmo nível de customização, governança e automação. Não se trata de cópia literal — cada ferramenta tem suas convenções idiomáticas que devem ser respeitadas.

## 2. Contexto Técnico do Gerador

### Arquitetura do `claude_setup`

O gerador segue um pipeline sequencial de assemblers orquestrado em `assembler/__init__.py`:

```
YAML config → ProjectConfig → _build_assemblers() → run_pipeline() → output atômico
```

Cada assembler implementa o método `assemble(config, output_dir, engine)` e retorna a lista de `Path` dos arquivos gerados. O pipeline usa output atômico (temp dir + rename) para garantir consistência.

### Assemblers Existentes (geram `.claude/`)

| Assembler | Output |
|-----------|--------|
| `RulesAssembler` | `.claude/rules/*.md` |
| `SkillsAssembler` | `.claude/skills/*/SKILL.md` + `references/` |
| `AgentsAssembler` | `.claude/agents/*.md` |
| `HooksAssembler` | `.claude/hooks/*.sh` |
| `SettingsAssembler` | `.claude/settings.json` |
| `PatternsAssembler` | Pattern files |
| `ProtocolsAssembler` | Protocol files |
| `ReadmeAssembler` | `README.md` |

### Assemblers para `.github/` (novos ou já implementados)

| Assembler | Output | Status |
|-----------|--------|--------|
| `GithubInstructionsAssembler` | `.github/copilot-instructions.md` + `.github/instructions/*.instructions.md` | **Implementado** (STORY-001) |
| `GithubMcpAssembler` | `.github/copilot-mcp.json` | Pendente (STORY-002) |
| `GithubSkillsAssembler` | `.github/skills/*/` | Pendente (STORY-003 a STORY-009) |
| `GithubAgentsAssembler` | `.github/agents/*.agent.md` | Pendente (STORY-010) |
| `GithubHooksAssembler` | `.github/hooks/*.json` | Pendente (STORY-011) |
| `GithubPromptsAssembler` | `.github/prompts/*.prompt.md` | Pendente (STORY-012) |

### Resource Templates

Templates Jinja2 ficam em `resources/` (ex: `resources/github-instructions-templates/`). Cada novo assembler de `.github/` deve ter seu diretório de templates correspondente.

### Padrão de Implementação

Para cada nova história, o ciclo de implementação é:

1. **Criar assembler** — nova classe em `src/claude_setup/assembler/` seguindo a interface existente
2. **Criar resource templates** — templates Jinja2 em `resources/<feature>-templates/`
3. **Registrar no pipeline** — adicionar à lista em `_build_assemblers()` no `assembler/__init__.py`
4. **Atualizar classificação CLI** — se necessário, ajustar flags e opções do CLI
5. **Adicionar golden files** — snapshots de referência para verificação byte-for-byte
6. **Escrever testes** — unit tests do assembler, golden file verification (`tests/test_byte_for_byte.py`), e pipeline integration tests (`tests/test_pipeline.py`)

## 3. Anexos e Referências

- [SPEC-github-copilot-structure.md](../SPEC-github-copilot-structure.md) — Especificação técnica completa
- `.claude/rules/` — 5 rules existentes (fonte para adaptação de instructions)
- `.claude/skills/` — 42 skills existentes (fonte para adaptação de skills)
- `.claude/agents/` — 10 agents existentes (fonte para adaptação de custom agents)
- `.claude/hooks/post-compile-check.sh` — Hook existente (fonte para hooks JSON)
- Documentação oficial GitHub Copilot: https://docs.github.com/copilot

## 4. Definições de Qualidade Globais

### Global Definition of Ready (DoR)

- Estrutura `.claude/` existente lida e mapeada para o componente em questão
- Convenções oficiais do GitHub Copilot documentadas e validadas para o tipo de artefato
- Dependências de histórias anteriores (Blocked By) concluídas e disponíveis
- Template de output definido para o componente (frontmatter, naming, estrutura de diretórios)

### Global Definition of Done (DoD)

- **Validação de formato:** YAML frontmatter válido e parseável em todos os arquivos `.md` que o exigem
- **Convenções Copilot:** Nomes de arquivos, extensões (`.instructions.md`, `.agent.md`, `.prompt.md`) e estrutura seguem exatamente as convenções documentadas do GitHub Copilot
- **Sem duplicação:** Conteúdo técnico referenciado via links relativos (não duplicado) de `.claude/` onde possível
- **Idioma:** Conteúdo em inglês (exceção: skills de story/epic em pt-BR conforme RULE-004)
- **Progressive disclosure:** Skills com 3 níveis (frontmatter, body, references/)
- **Documentação:** README.md na raiz de `.github/` documentando a estrutura completa
- **Integração:** Copilot reconhece e carrega os artefatos corretamente (validação manual)
- **Assembler unit tests:** Cada novo assembler deve ter testes unitários com cobertura ≥ 95% line / ≥ 90% branch
- **Golden file verification:** Output do assembler verificado byte-for-byte contra golden files em `tests/test_byte_for_byte.py`
- **Pipeline integration:** Assembler registrado em `_build_assemblers()` e validado via `tests/test_pipeline.py`
- **Idempotência:** Executar o pipeline duas vezes consecutivas produz output idêntico

## 5. Regras de Negócio Transversais (Source of Truth)

> Regras que se aplicam a múltiplas histórias. Cada história referencia as regras pelo ID. Alterações de regra propagam automaticamente para todas as histórias dependentes.

| ID | Título | Descrição |
| :--- | :--- | :--- |
| **[RULE-001]** | Paridade funcional | Toda capability existente em `.claude/` deve ter um equivalente em `.github/`, adaptado às convenções do Copilot. Não é cópia literal — cada ferramenta tem suas idiomáticas.<br>Mapeamento: `rules/` → `instructions/`, `skills/` → `skills/`, `agents/` → `agents/`, `hooks/` → `hooks/`, templates → `prompts/`. |
| **[RULE-002]** | Convenções do Copilot | Nomes de arquivos, frontmatter YAML e estrutura de diretórios devem seguir exatamente as convenções documentadas do GitHub Copilot:<br>- Skills: `name` (lowercase-hyphens, required) + `description` (required) no frontmatter<br>- Agents: extensão `.agent.md` com `tools`/`disallowed-tools` no frontmatter<br>- Hooks: formato JSON com event types (`sessionStart`, `postToolUse`, `preToolUse`, etc.)<br>- Prompts: extensão `.prompt.md` com frontmatter YAML<br>- Instructions: extensão `.instructions.md` (exceto `copilot-instructions.md` global) |
| **[RULE-003]** | Sem duplicação de conteúdo | Conteúdo técnico (patterns, references, knowledge packs) deve ser referenciado via links relativos, não duplicado.<br>Se o conteúdo existe em `.claude/skills/*/references/`, o equivalente em `.github/` deve apontar para ele ou fornecer versão adaptada mínima.<br>Knowledge packs (`architecture`, `coding-standards`, etc.) podem referenciar diretamente os originais. |
| **[RULE-004]** | Idioma | Todo conteúdo em inglês, consistente com `01-project-identity.md`.<br>Exceção explícita: skills de story/epic (`x-story-epic`, `x-story-create`, `x-story-map`, `x-story-epic-full`) mantêm pt-BR conforme convenção existente.<br>Termos técnicos em inglês (cache, timeout, handler, endpoint) permanecem em inglês independente do idioma. |
| **[RULE-005]** | Progressive disclosure | Skills devem usar o modelo de 3 níveis:<br>1. **Frontmatter** (sempre carregado): `name` + `description` — suficiente para o Copilot decidir se carrega<br>2. **Body** (sob demanda): instruções detalhadas, carregadas quando a skill é ativada<br>3. **References** (deep-dive): arquivos auxiliares no diretório `references/` da skill |
| **[RULE-006]** | Tool boundaries | Custom agents devem declarar explicitamente no frontmatter YAML: `tools` (whitelist) e `disallowed-tools` (blacklist).<br>Nenhum agent deve ter acesso irrestrito — princípio de menor privilégio.<br>A combinação persona + tools deve ser coerente (ex: `qa-engineer` não tem acesso a deploy tools). |
| **[RULE-007]** | Consistência de hooks | Hooks em `.github/` devem cobrir os mesmos pontos de verificação que hooks em `.claude/`.<br>O hook `post-compile-check.sh` existente deve ter equivalente funcional no formato JSON do Copilot.<br>Hooks adicionais (`pre-commit-lint`, `session-context-loader`) expandem a cobertura. |
| **[RULE-008]** | Integração com o gerador | Todo output `.github/` **deve** ser produzido por assemblers registrados no pipeline do `claude_setup`, nunca criado manualmente.<br>Cada assembler deve: (a) usar templates de `resources/`, (b) ser registrado em `_build_assemblers()`, (c) ter golden files de referência, (d) ter testes unitários e de integração.<br>Arquivos `.github/` fora do pipeline são proibidos — a CLI é a única fonte de geração. |

## 6. Índice de Histórias

| ID | Título | Dependências (Blocked By) |
| :--- | :--- | :--- |
| [STORY-001](./STORY-001.md) | Instructions globais e contextuais (copilot-instructions.md + instructions/*.instructions.md) | — |
| [STORY-002](./STORY-002.md) | Configuração MCP (copilot-mcp.json) | — |
| [STORY-003](./STORY-003.md) | Skills de Story/Planning (x-story-epic, x-story-create, x-story-map, x-story-epic-full, story-planning) | STORY-001 |
| [STORY-004](./STORY-004.md) | Skills de Development (x-dev-implement, x-dev-lifecycle, layer-templates) | STORY-001 |
| [STORY-005](./STORY-005.md) | Skills de Review (x-review, x-review-api, x-review-pr, x-review-grpc, x-review-events, x-review-gateway) | STORY-001 |
| [STORY-006](./STORY-006.md) | Skills de Testing (x-test-plan, x-test-run, run-e2e, run-smoke-api, run-contract-tests, run-perf-test) | STORY-001 |
| [STORY-007](./STORY-007.md) | Skills de Infrastructure (setup-environment, k8s-deployment, k8s-kustomize, dockerfile, iac-terraform) | STORY-001 |
| [STORY-008](./STORY-008.md) | Skills Knowledge Packs (architecture, coding-standards, patterns, protocols, observability, resilience, security, compliance, api-design) | STORY-001 |
| [STORY-009](./STORY-009.md) | Skills de Git e Troubleshooting (x-git-push, x-ops-troubleshoot) | STORY-001 |
| [STORY-010](./STORY-010.md) | Custom Agents (.github/agents/*.agent.md) | STORY-003, STORY-004, STORY-005 |
| [STORY-011](./STORY-011.md) | Hooks (.github/hooks/*.json) | STORY-010 |
| [STORY-012](./STORY-012.md) | Prompts de Composição (.github/prompts/*.prompt.md) | STORY-003, STORY-004, STORY-005, STORY-010 |
| [STORY-013](./STORY-013.md) | README e Validação Final da Estrutura .github | STORY-001, STORY-002, STORY-003, STORY-004, STORY-005, STORY-006, STORY-007, STORY-008, STORY-009, STORY-010, STORY-011, STORY-012 |
