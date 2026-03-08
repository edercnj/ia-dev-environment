# Épico: Estrutura de Configuração `.github/` para GitHub Copilot

**Autor:** Product Owner / Architect
**Data:** 2026-03-08
**Versão:** 2.0
**Status:** Em Refinamento

## 1. Visão Geral

**Chave Jira:** EPIC-001

Criar a estrutura completa `.github/` no repositório `claude-environment`, espelhando e complementando a estrutura existente `.claude/`. O objetivo é que o mesmo repositório sirva como environment configuration para **ambas** as ferramentas — Claude Code e GitHub Copilot — permitindo que equipes que usam Copilot tenham o mesmo nível de customização, governança e automação. A implementação abrange instructions globais, skills com progressive disclosure, custom agents com tool boundaries, hooks determinísticos, prompts reutilizáveis e configuração MCP. Não se trata de cópia literal — cada ferramenta tem suas convenções idiomáticas que devem ser respeitadas.

## 2. Anexos e Referências

- [SPEC-github-copilot-structure.md](../SPEC-github-copilot-structure.md) — Especificação técnica completa
- `.claude/rules/` — 5 rules existentes (fonte para adaptação de instructions)
- `.claude/skills/` — 42 skills existentes (fonte para adaptação de skills)
- `.claude/agents/` — 10 agents existentes (fonte para adaptação de custom agents)
- `.claude/hooks/post-compile-check.sh` — Hook existente (fonte para hooks JSON)
- Documentação oficial GitHub Copilot: https://docs.github.com/copilot

## 3. Definições de Qualidade Globais

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

## 4. Regras de Negócio Transversais (Source of Truth)

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

## 5. Índice de Histórias

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
