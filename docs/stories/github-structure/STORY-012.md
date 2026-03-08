# História: Prompts de Composição (.github/prompts/*.prompt.md)

**ID:** STORY-012

## 1. Dependências

| Blocked By | Blocks |
| :--- | :--- |
| STORY-003, STORY-004, STORY-005, STORY-010 | STORY-013 |

## 2. Regras Transversais Aplicáveis

| ID | Título |
| :--- | :--- |
| RULE-001 | Paridade funcional |
| RULE-002 | Convenções do Copilot |
| RULE-004 | Idioma |
| RULE-005 | Progressive disclosure |

## 3. Descrição

Como **Product Owner Técnico**, eu quero criar 4 prompts em `.github/prompts/*.prompt.md` que orquestram workflows completos, garantindo que tarefas recorrentes (nova feature, decomposição de spec, code review, troubleshooting) possam ser executadas com menor fricção.

Os prompts são composições de alto nível que conectam skills e agents em fluxos end-to-end. Cada prompt tem YAML frontmatter com `name` e `description`, seguido do template de instruções.

### 3.1 Prompts a criar

| Prompt | Baseado em | Skills orquestradas | Agents envolvidos |
| :--- | :--- | :--- | :--- |
| `new-feature.prompt.md` | Workflow de implementação | x-dev-lifecycle, x-dev-implement, x-review | java-developer, tech-lead |
| `decompose-spec.prompt.md` | Templates de epic/story | x-story-epic-full | product-owner, architect |
| `code-review.prompt.md` | Workflow de review | x-review, x-review-api, x-review-pr | tech-lead, security-engineer, qa-engineer |
| `troubleshoot.prompt.md` | Workflow de troubleshoot | x-ops-troubleshoot | java-developer |

### 3.2 Formato .prompt.md

```yaml
---
name: new-feature
description: >
  Orchestrates the complete feature implementation cycle: planning,
  layer-by-layer implementation, review, and PR creation.
---

# New Feature Implementation

Follow these steps to implement a new feature...
```

## 4. Definições de Qualidade Locais

### DoR Local (Definition of Ready)

- [ ] STORY-003, 004, 005, 010 concluídas (skills e agents disponíveis)
- [ ] Templates existentes em `resources/templates/` lidos
- [ ] Formato `.prompt.md` validado com Copilot docs

### DoD Local (Definition of Done)

- [ ] 4 prompts criados com extensão `.prompt.md`
- [ ] Cada prompt com frontmatter YAML válido
- [ ] Workflows referenciando skills e agents existentes
- [ ] Copilot reconhece e lista prompts disponíveis

### Global Definition of Done (DoD)

- **Validação de formato:** YAML frontmatter válido
- **Convenções Copilot:** Extensão `.prompt.md`, naming conforme docs
- **Sem duplicação:** Orquestra skills/agents, não duplica conteúdo
- **Idioma:** Inglês
- **Documentação:** README.md atualizado

## 5. Contratos de Dados (Data Contract)

**Prompt Composition Contract:**

| Campo | Formato | Request | Response | Origem / Regra |
| :--- | :--- | :--- | :--- | :--- |
| `frontmatter.name` | string (lowercase-hyphens) | M | — | Ex: `new-feature` |
| `frontmatter.description` | string (multiline) | M | — | Descrição do workflow |
| `orchestrated_skills` | array[string] | M | — | Skills ativadas pelo prompt |
| `involved_agents` | array[string] | O | — | Agents recomendados |

## 6. Diagramas

### 6.1 Prompt new-feature orquestrando skills

```mermaid
sequenceDiagram
    participant U as Usuário
    participant P as new-feature.prompt.md
    participant L as x-dev-lifecycle
    participant I as x-dev-implement
    participant R as x-review-pr
    participant G as x-git-push

    U->>P: Ativar prompt "new feature"
    P->>L: Iniciar ciclo de implementação
    L->>I: Implementar layer-by-layer
    I-->>L: Código implementado
    L->>R: Executar review
    R-->>L: GO/NO-GO
    L->>G: Push e criar PR
    G-->>U: URL do PR
```

## 7. Critérios de Aceite (Gherkin)

```gherkin
Cenario: Prompt new-feature disponível para seleção
  DADO que .github/prompts/new-feature.prompt.md existe com frontmatter válido
  QUANDO o usuário lista prompts disponíveis
  ENTÃO "new-feature" aparece na lista
  E a description descreve o workflow de implementação

Cenario: Prompt decompose-spec orquestra x-story-epic-full
  DADO que decompose-spec.prompt.md referencia x-story-epic-full
  QUANDO o usuário ativa o prompt com uma spec como input
  ENTÃO o workflow solicita o caminho da spec
  E ativa a skill x-story-epic-full para decomposição

Cenario: Prompt code-review com múltiplos agents
  DADO que code-review.prompt.md referencia tech-lead, security e qa agents
  QUANDO o prompt é ativado para um PR
  ENTÃO o workflow instrui reviews paralelos pelos 3 agents
  E consolida resultados em relatório único

Cenario: Prompt com frontmatter inválido
  DADO que um .prompt.md não tem campo "name" no frontmatter
  QUANDO o Copilot tenta indexar o prompt
  ENTÃO o prompt NÃO aparece na lista de prompts disponíveis
  E o erro indica campo obrigatório ausente

Cenario: Prompt troubleshoot com metodologia sistemática
  DADO que troubleshoot.prompt.md referencia x-ops-troubleshoot
  QUANDO o usuário ativa o prompt com uma stacktrace
  ENTÃO o workflow segue: reproduce → locate → understand → fix → verify
  E não pula etapas do diagnóstico
```

## 8. Sub-tarefas

- [ ] [Dev] Criar `.github/prompts/new-feature.prompt.md`
- [ ] [Dev] Criar `.github/prompts/decompose-spec.prompt.md`
- [ ] [Dev] Criar `.github/prompts/code-review.prompt.md`
- [ ] [Dev] Criar `.github/prompts/troubleshoot.prompt.md`
- [ ] [Test] Validar YAML frontmatter dos 4 prompts
- [ ] [Test] Verificar referências a skills e agents existentes
- [ ] [Test] Validar extensão `.prompt.md`
- [ ] [Doc] Documentar prompts no README
