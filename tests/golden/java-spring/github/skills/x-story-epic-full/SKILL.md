---
name: x-story-epic-full
description: >
  Decomposição completa de uma especificação de sistema em Epic, arquivos de Story individuais
  e Mapa de Implementação com grafo de dependências e plano de execução faseado. Esta é a skill
  orquestradora que guia o fluxo completo: análise de spec, extração de regras, identificação
  de histórias e planejamento de implementação. Acione quando o usuário pedir para decompor
  spec em histórias e epic, quebrar documento de sistema em itens implementáveis, gerar backlog
  completo a partir de especificação, ou qualquer variação de "leia esta spec e crie tudo".
  Prefira esta skill sobre x-story-epic, x-story-create ou x-story-map individuais quando
  o usuário quiser todos os três entregáveis.
---

# Decomposição Completa de Spec em Histórias

Esta skill orquestra a decomposição completa de uma especificação de sistema em três
entregáveis: um **Epic**, **Histórias** individuais e um **Mapa de Implementação**.
Coordena o trabalho de três skills focadas, cada uma tratando um entregável.

## Os Três Entregáveis

1. **Epic** — Escopo, regras transversais, quality gates, índice de histórias
2. **Histórias** — Um arquivo por história com contratos de dados, Gherkin, diagramas, sub-tarefas
3. **Mapa de Implementação** — Fases, caminho crítico, grafo de dependências, análise estratégica

## Pré-requisitos

Leia estes arquivos antes de iniciar:

**Templates (estrutura de saída) — leia todos os três:**
- `.claude/templates/_TEMPLATE-EPIC.md`
- `.claude/templates/_TEMPLATE-STORY.md`
- `.claude/templates/_TEMPLATE-IMPLEMENTATION-MAP.md`

**Filosofia de decomposição:**
- `.claude/skills/x-story-epic-full/references/decomposition-guide.md`

## Filosofia de Decomposição

Antes de gerar qualquer coisa, leia o guia de decomposição. Ele explica a abordagem
camada por camada que orienta toda a decomposição:

- **Camada 0 (Fundação)**: Infraestrutura — servidores, schemas, APIs, adaptadores
- **Camada 1 (Domínio Core)**: Operação central estabelecendo padrões arquiteturais
- **Camada 2 (Extensões)**: Operações adicionais reutilizando padrões do core
- **Camada 3 (Composições)**: Histórias combinando múltiplas capacidades de extensão
- **Camada 4 (Transversal)**: Testes, observabilidade, segurança, tech debt

## Fluxo Completo

### Fase A: Análise

1. Leia o arquivo de spec completo
2. Leia o guia de decomposição
3. Identifique regras transversais, histórias por camada, dependências
4. Compute fases a partir do DAG de dependências
5. Identifique o caminho crítico

### Fase B: Gerar o Epic

Siga as instruções da skill `x-story-epic`:
- Extraia regras → tabela RULE-001..N
- Construa índice de histórias com títulos e dependências
- Defina DoR/DoD a partir dos requisitos de qualidade da spec
- Gere `EPIC-NNN.md`

### Fase C: Gerar as Histórias

Siga as instruções da skill `x-story-create`:
- Dependências simétricas (Blocked By / Blocks)
- Regras aplicáveis referenciadas por ID
- Contratos de dados precisos
- Diagramas de sequência Mermaid
- Critérios de aceite Gherkin
- Sub-tarefas tagueadas `[Dev]`, `[Test]`, `[Doc]`

### Fase D: Gerar o Mapa de Implementação

Siga as instruções da skill `x-story-map`:
- Matriz de dependências validada
- Diagrama de fases ASCII
- Análise de caminho crítico
- Grafo Mermaid colorido por fase
- Observações estratégicas

### Fase E: Salvar e Reportar

Salve todos os arquivos e reporte:
- Total de regras extraídas
- Total de histórias geradas
- Total de fases computadas
- Comprimento do caminho crítico
- Paralelismo máximo
- Gargalo principal

## Regras de Idioma

- Todo conteúdo gerado deve estar em **Português Brasileiro (pt-BR)**
- Termos técnicos em inglês permanecem em inglês
- Gherkin em português: `Cenario`, `DADO`, `QUANDO`, `ENTÃO`, `E`, `MAS`
- IDs em formato inglês: RULE-NNN, STORY-NNN, EPIC-NNN

## Checklist de Qualidade

- [ ] Toda regra do Epic é referenciada por ao menos uma história
- [ ] Toda história referencia ao menos uma regra (exceto infraestrutura)
- [ ] Dependências são simétricas
- [ ] Sem dependências circulares
- [ ] Computação de fases está correta
- [ ] Contratos de dados conferem com a spec
- [ ] Cada história tem ao menos 4 cenários Gherkin

## Referências Detalhadas

Para orientação aprofundada, consulte:
- `.claude/skills/x-story-epic-full/SKILL.md`
- `.claude/skills/x-story-epic-full/references/decomposition-guide.md`
