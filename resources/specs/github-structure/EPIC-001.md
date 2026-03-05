# Épico: Estrutura `.github/` para GitHub Copilot

**Autor:** GitHub Copilot CLI (x-story-epic-full)  
**Data:** 2026-03-04  
**Versão:** 1.0  
**Status:** Pronto

## 1. Visão Geral

**Chave Jira:** COPILOT-STRUCTURE-001

Este épico decompõe a criação da estrutura `.github/` equivalente à `.claude/`, adaptada às convenções oficiais do GitHub Copilot.
O escopo inclui instructions, skills, custom agents, prompts, hooks, MCP, documentação e validação final.
A execução segue camadas: Foundation (instructions), Core (skills de story/planning e development), Extensions (review/testing/infra e agents), Compositions (prompts) e Cross-cutting (hooks/MCP/README/validação).

## 2. Anexos e Referências

- Spec base: [`resources/specs/SPEC-github-copilot-structure.md`](../SPEC-github-copilot-structure.md)
- Template Epic: [`resources/templates/_TEMPLATE-EPIC.md`](../../templates/_TEMPLATE-EPIC.md)
- Template Story: [`resources/templates/_TEMPLATE-STORY.md`](../../templates/_TEMPLATE-STORY.md)
- Template Map: [`resources/templates/_TEMPLATE-IMPLEMENTATION-MAP.md`](../../templates/_TEMPLATE-IMPLEMENTATION-MAP.md)

## 3. Definições de Qualidade Globais

### Global Definition of Ready (DoR)

- Estrutura `.claude/` lida e mapeada por componente.
- Convenções do Copilot validadas para todos os tipos de arquivo.
- Diretório de saída e padrão de naming definidos.

### Global Definition of Done (DoD)

- **Cobertura:** validação de 100% dos artefatos `.github/` gerados neste épico.
- **Testes Automatizados:** checks de naming, frontmatter, links relativos e carregamento de skills/prompts.
- **Relatório de Cobertura:** saída consolidada pass/fail por componente.
- **Documentação:** README da `.github/` atualizado com estrutura e fluxo de uso.
- **Persistência:** arquivos versionados com ordenação determinística e sem duplicação literal.
- **Performance:** hooks síncronos dentro do timeout configurado (<= 60s).

## 4. Regras de Negócio Transversais (Source of Truth)

| ID | Título | Descrição |
| :--- | :--- | :--- |
| **[RULE-001]** | Paridade funcional | Toda capability de `.claude/` deve ter equivalente funcional em `.github/`, com adaptação de convenções. |
| **[RULE-002]** | Convenções Copilot | Naming, frontmatter e formato de artefatos seguem padrão oficial do Copilot. |
| **[RULE-003]** | Sem duplicação | Conteúdo técnico deve ser referenciado por links relativos, não copiado integralmente. |
| **[RULE-004]** | Idioma | Conteúdo em inglês por padrão; exceção explícita para skills de story em pt-BR. |
| **[RULE-005]** | Progressive disclosure | Skills devem manter estrutura em 3 níveis (frontmatter, body, references). |
| **[RULE-006]** | Tool boundaries | Agents devem declarar ferramentas permitidas e proibidas de forma explícita. |
| **[RULE-007]** | Paridade de hooks | Hooks em `.github/` devem cobrir checkpoints críticos já existentes em `.claude/`. |

## 5. Índice de Histórias

| ID | Título | Dependências (Blocked By) |
| :--- | :--- | :--- |
| [STORY-001](./STORY-001.md) | Fundação de Instructions do Copilot | - |
| [STORY-002](./STORY-002.md) | Skills Core: Story/Planning e Development | STORY-001 |
| [STORY-003](./STORY-003.md) | Skills de Extensão: Review, Testing e Infrastructure | STORY-002 |
| [STORY-004](./STORY-004.md) | Custom Agents em formato .agent.md | STORY-002 |
| [STORY-005](./STORY-005.md) | Prompts de Composição de Workflows (.prompt.md) | STORY-002, STORY-003, STORY-004 |
| [STORY-006](./STORY-006.md) | Cross-cutting Config: Hooks JSON + MCP | STORY-004 |
| [STORY-007](./STORY-007.md) | README e Validação End-to-End da estrutura Copilot | STORY-003, STORY-004, STORY-005, STORY-006 |
