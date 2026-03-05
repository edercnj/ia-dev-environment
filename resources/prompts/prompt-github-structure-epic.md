# Prompt para Claude Code — Gerar Épico e Histórias da Estrutura .github

## Como usar

Copie o bloco abaixo e cole no Claude Code. Ele vai acionar a skill `x-story-epic-full`
que gera Epic + Stories + Implementation Map em um único passo.

---

## Prompt

```
Use a skill x-story-epic-full para decompor a spec abaixo em Epic, Stories e Implementation Map.

Spec file: resources/specs/SPEC-github-copilot-structure.md

Contexto adicional:

1. Este projeto (claude-environment) já possui uma estrutura .claude/ completa com:
   - 5 rules em .claude/rules/
   - 10 agents em .claude/agents/
   - 40+ skills em .claude/skills/ (incluindo libs e knowledge packs)
   - Hooks de post-compile em .claude/hooks/
   - Templates em resources/templates/

2. O objetivo é criar a estrutura equivalente .github/ para GitHub Copilot,
   adaptando (não copiando) o conteúdo existente às convenções do Copilot.

3. Prioridades de implementação:
   - Layer 0 (Foundation): copilot-instructions.md + instructions/*.instructions.md
   - Layer 1 (Core): Skills de story/planning e development (maior valor)
   - Layer 2 (Extensions): Skills de review, testing, infra
   - Layer 3 (Compositions): Prompts que orquestram workflows completos
   - Layer 4 (Cross-cutting): Hooks, MCP config, README, validação

4. Regras específicas:
   - Seguir convenções oficiais do GitHub Copilot (YAML frontmatter com name + description)
   - Agents usam formato .agent.md com tools/disallowed-tools no frontmatter
   - Hooks usam JSON (não shell scripts diretos)
   - Prompts usam formato .prompt.md
   - Sem duplicação: referenciar conteúdo de .claude/ onde possível
   - Idioma: inglês (conforme 01-project-identity.md), exceto skills de story em pt-BR

5. Output:
   - Salvar Epic, Stories e Implementation Map em resources/specs/github-structure/
   - Nomear como EPIC-001.md, STORY-001.md..N, IMPLEMENTATION-MAP.md
```

---

## Variações

### Só o Epic (sem stories)

```
Use a skill x-story-epic para gerar o Epic a partir de:
resources/specs/SPEC-github-copilot-structure.md

Salvar em resources/specs/github-structure/EPIC-001.md
```

### Só as Stories (a partir de um Epic existente)

```
Use a skill x-story-create para gerar as Stories a partir do Epic:
resources/specs/github-structure/EPIC-001.md

Spec original: resources/specs/SPEC-github-copilot-structure.md
Salvar em resources/specs/github-structure/
```

### Só o Implementation Map (a partir de Epic + Stories)

```
Use a skill x-story-map para gerar o Implementation Map a partir de:
- Epic: resources/specs/github-structure/EPIC-001.md
- Stories: resources/specs/github-structure/STORY-*.md

Salvar em resources/specs/github-structure/IMPLEMENTATION-MAP.md
```
