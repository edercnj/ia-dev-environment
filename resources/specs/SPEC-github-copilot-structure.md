# System Specification — GitHub Copilot Configuration Structure (.github)

## 1. Visão Geral

### 1.1 Objetivo

Criar a estrutura de configuração `.github/` no repositório `claude-environment`, espelhando e complementando a estrutura existente `.claude/`. O objetivo é que o mesmo repositório sirva como environment configuration para **ambas** as ferramentas — Claude Code e GitHub Copilot — permitindo que equipes que usam Copilot tenham o mesmo nível de customização, governança e automação que já temos com Claude Code.

### 1.2 Escopo

Implementar a estrutura completa `.github/` com os seguintes componentes:

| Componente | .claude (existente) | .github (novo) | Descrição |
|:---|:---|:---|:---|
| Instructions globais | `.claude/rules/*.md` | `.github/copilot-instructions.md` + `.github/instructions/*.instructions.md` | Instruções que o Copilot carrega automaticamente em todo prompt |
| Skills | `.claude/skills/*/SKILL.md` | `.github/skills/*/SKILL.md` | Skills com YAML frontmatter e progressive disclosure (3 níveis) |
| Custom Agents | `.claude/agents/*.md` | `.github/agents/*.agent.md` | Agentes especializados com tool boundaries e persona |
| Hooks | `.claude/hooks/*.sh` + `settings.json` | `.github/hooks/*.json` | Hooks determinísticos em pontos estratégicos do workflow |
| Prompts/Templates | `resources/templates/_TEMPLATE-*.md` | `.github/prompts/*.prompt.md` | Templates reutilizáveis para tarefas recorrentes |
| MCP Config | `.claude/settings.json` (parcial) | `.github/copilot-mcp.json` | Configuração de MCP servers para integrações externas |

### 1.3 Premissas

- A estrutura `.github/` segue as convenções oficiais do GitHub Copilot (docs.github.com/copilot)
- Skills usam YAML frontmatter com campos `name` (required, lowercase-hyphens) e `description` (required)
- Custom Agents usam formato `.agent.md` com frontmatter definindo tools, persona e constraints
- Hooks usam JSON com hook types: `sessionStart`, `sessionEnd`, `userPromptSubmitted`, `preToolUse`, `postToolUse`, `agentStop`, `subagentStop`, `errorOccurred`
- Prompts usam formato `.prompt.md` com YAML frontmatter
- O conteúdo deve ser adaptado (não copiado literalmente) do `.claude/` — cada ferramenta tem suas convenções

### 1.4 Fora de Escopo

- Configuração de organização ou enterprise (será feita separadamente)
- Personal skills em `~/.copilot/skills/` (escopo pessoal do desenvolvedor)
- Implementação de código — esta spec trata apenas da estrutura de configuração

## 2. Regras de Negócio

### 2.1 Regras Cross-Cutting

| ID | Regra | Impacto |
|:---|:---|:---|
| RULE-001 | **Paridade funcional**: Toda capability existente em `.claude/` deve ter um equivalente em `.github/`, adaptado às convenções do Copilot | Todas as stories |
| RULE-002 | **Convenções do Copilot**: Nomes de arquivos, frontmatter e estrutura devem seguir exatamente as convenções documentadas do GitHub Copilot | Todas as stories |
| RULE-003 | **Sem duplicação de conteúdo**: O conteúdo técnico (patterns, references) deve ser referenciado, não duplicado. Usar links relativos quando possível | Skills, Instructions |
| RULE-004 | **Idioma**: Instruções em inglês (consistente com `01-project-identity.md`). Exceção: skills de story/epic em pt-BR conforme convenção existente | Instructions, Skills |
| RULE-005 | **Progressive disclosure**: Skills devem usar o modelo de 3 níveis — frontmatter (sempre carregado), body (sob demanda), references (deep-dive) | Skills |
| RULE-006 | **Tool boundaries**: Custom agents devem declarar explicitamente quais tools podem usar e quais são proibidos | Agents |
| RULE-007 | **Consistência de hooks**: Hooks `.github/` devem cobrir os mesmos pontos de verificação que hooks `.claude/` (ex: post-compile-check) | Hooks |

## 3. Componentes Técnicos

### 3.1 Instructions (copilot-instructions.md + instructions/*.instructions.md)

**Mapeamento de `.claude/rules/` → `.github/instructions/`:**

| .claude/rules/ | .github/ equivalent |
|:---|:---|
| `01-project-identity.md` | `copilot-instructions.md` (global, auto-incluído) |
| `02-domain.md` | `instructions/domain.instructions.md` |
| `03-coding-standards.md` | `instructions/coding-standards.instructions.md` |
| `04-architecture-summary.md` | `instructions/architecture.instructions.md` |
| `05-quality-gates.md` | `instructions/quality-gates.instructions.md` |

O arquivo `copilot-instructions.md` é carregado automaticamente em toda interação. Os `.instructions.md` são carregados condicionalmente quando relevantes.

### 3.2 Skills (.github/skills/*/SKILL.md)

Formato do SKILL.md para Copilot:

```yaml
---
name: skill-name-lowercase
description: >
  Description that helps Copilot decide when to load this skill.
  Must be concise but specific enough for accurate triggering.
---

# Skill Title

Body with detailed instructions, loaded only when the skill is triggered.
```

**Categorias de skills a criar (mapeando `.claude/skills/`):**

| Categoria | Skills | Prioridade |
|:---|:---|:---|
| Story/Planning | x-story-epic, x-story-create, x-story-map, x-story-epic-full | Alta |
| Development | x-dev-implement, x-dev-lifecycle, layer-templates | Alta |
| Review | x-review, x-review-api, x-review-pr, x-review-grpc, x-review-events, x-review-gateway | Alta |
| Testing | x-test-plan, x-test-run, run-e2e, run-smoke-api, run-contract-tests, run-perf-test | Média |
| Infrastructure | setup-environment, k8s-deployment, k8s-kustomize, dockerfile, iac-terraform | Média |
| Knowledge | architecture, coding-standards, patterns, protocols, observability, resilience, security, compliance, api-design | Baixa (reference material) |
| Git | x-git-push | Média |
| Troubleshooting | x-ops-troubleshoot | Média |

### 3.3 Custom Agents (.github/agents/*.agent.md)

Formato `.agent.md`:

```yaml
---
name: agent-name
description: Agent description
tools:
  - tool1
  - tool2
disallowed-tools:
  - tool3
---

# Agent persona and instructions
```

**Mapeamento `.claude/agents/` → `.github/agents/`:**

| .claude/agents/ | .github/agents/ | Tool Boundaries |
|:---|:---|:---|
| architect.md | architect.agent.md | Read-only code, can create docs/diagrams |
| tech-lead.md | tech-lead.agent.md | Full code access, review tools |
| java-developer.md | java-developer.agent.md | Full code + build + test tools |
| qa-engineer.md | qa-engineer.agent.md | Test tools, read-only code |
| security-engineer.md | security-engineer.agent.md | Read-only code, security scanning tools |
| devops-engineer.md | devops-engineer.agent.md | Docker, K8s, infra tools |
| performance-engineer.md | performance-engineer.agent.md | Profiling, load test tools |
| api-engineer.md | api-engineer.agent.md | API tools, code access |
| event-engineer.md | event-engineer.agent.md | Event/messaging tools, code access |
| product-owner.md | product-owner.agent.md | Read-only, docs/planning tools only |

### 3.4 Hooks (.github/hooks/*.json)

Formato JSON:

```json
{
  "hooks": [
    {
      "event": "postToolUse",
      "matcher": { "tool": "edit_file" },
      "command": "scripts/post-compile-check.sh",
      "timeout": 60000,
      "description": "Verify compilation after file edits"
    }
  ]
}
```

**Hooks a implementar:**

| Hook | Event | Descrição |
|:---|:---|:---|
| post-compile-check | postToolUse (edit_file) | Verificar compilação após edição (equivalente ao existente) |
| pre-commit-lint | preToolUse (git_commit) | Lint e format check antes de commit |
| session-context-loader | sessionStart | Carregar contexto do projeto na sessão |

### 3.5 Prompts (.github/prompts/*.prompt.md)

Formato `.prompt.md`:

```yaml
---
name: prompt-name
description: When to use this prompt
---

Prompt template content with instructions.
```

**Prompts a criar (derivados dos templates existentes):**

| Prompt | Baseado em | Uso |
|:---|:---|:---|
| new-feature.prompt.md | _TEMPLATE.md workflow | Iniciar implementação de nova feature |
| decompose-spec.prompt.md | _TEMPLATE-EPIC.md + _TEMPLATE-STORY.md | Decompor spec em epic + stories |
| code-review.prompt.md | x-review workflow | Review completo com subagents |
| troubleshoot.prompt.md | x-ops-troubleshoot | Diagnóstico de problemas |

### 3.6 MCP Config (.github/copilot-mcp.json)

Configuração de MCP servers para integrações externas (equivalente parcial de `.claude/settings.json`).

## 4. Dependências

### 4.1 Dependências Estruturais

```
Instructions → base para tudo (deve existir primeiro)
Skills → dependem de Instructions (referenciam conventions)
Agents → dependem de Skills (usam skills como capabilities)
Hooks → dependem de Agents (executam em workflow de agents)
Prompts → dependem de Skills + Agents (orquestram workflows)
MCP Config → independente (pode ser paralelo)
```

### 4.2 Dependências Externas

- Documentação oficial: https://docs.github.com/copilot
- GitHub Copilot extension atualizada (VS Code / JetBrains)
- Convenções do repositório `github/awesome-copilot`

## 5. Critérios de Qualidade

### 5.1 Definition of Ready

- [ ] Estrutura `.claude/` existente lida e mapeada
- [ ] Convenções do GitHub Copilot documentadas e validadas
- [ ] Templates de output definidos para cada componente

### 5.2 Definition of Done

- [ ] Estrutura `.github/` completa com todos os componentes
- [ ] Cada skill com YAML frontmatter válido e progressive disclosure
- [ ] Cada agent com tool boundaries explícitas
- [ ] Hooks cobrindo os mesmos checkpoints que `.claude/hooks/`
- [ ] README.md na raiz de `.github/` documentando a estrutura
- [ ] Validação: Copilot reconhece e carrega skills corretamente
- [ ] Sem duplicação de conteúdo entre `.claude/` e `.github/`
