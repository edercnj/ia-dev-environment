---
name: story-planning
description: >
  Pacote de conhecimento sobre decomposição e planejamento de histórias: decomposição camada
  por camada (fundação, domínio core, extensões, composições, transversal), autocontenção
  de histórias (contratos de dados, critérios de aceite), DAG de dependências, regras de
  dimensionamento e computação de fases. Acione quando o usuário pedir orientação sobre
  planejamento de histórias, decomposição de backlog, dimensionamento de stories, ou
  gerenciamento de dependências entre histórias.
---

# Pacote de Conhecimento: Planejamento de Histórias

## Propósito

Fornece padrões de decomposição e planejamento de histórias para traduzir especificações
de sistema em itens de trabalho independentemente implementáveis. Habilita decomposição
em camadas, gerenciamento de dependências, consistência de dimensionamento e planejamento
de entrega faseada.

## Referência Rápida

Consulte os guias de referência para o resumo essencial de decomposição de histórias:
5 camadas (fundação, domínio core, extensões, composições, transversal), regras de
autocontenção, DAG de dependências e dimensionamento.

## Referências Detalhadas

| Referência | Conteúdo |
|-----------|----------|
| Decomposição em Camadas | 5 camadas (Camada 0: infraestrutura de fundação, Camada 1: estabelecimento de padrão de domínio core, Camada 2: extensões reutilizando Camada 1, Camada 3: composições combinando múltiplas capacidades, Camada 4: qualidade/observabilidade transversal) |
| Autocontenção de Histórias | Contratos de dados (todos os campos com tipo, obrigatório/opcional, regras de derivação), critérios de aceite Gherkin (valores concretos), diagramas de sequência Mermaid (nomes reais de componentes), sub-tarefas (estimáveis em 2-4 horas, tagueadas [Dev]/[Test]/[Doc]) |
| DAG de Dependências | Construção de grafo acíclico dirigido, detecção de dependências circulares, tipos de dependência (estrutural, dados, padrão), consistência bidirecional |
| Regras Transversais | Padrões de extração de regras, IDs sequenciais únicos (RULE-001), descrições prontas para implementação |
| Dimensionamento de Histórias | Métricas (endpoints por história: máx 2, fluxos de protocolo: máx 1, cenários Gherkin: 2-8, sub-tarefas: máx 10) |
| Computação de Fases | Derivação automática de fases a partir do DAG, identificação de caminho crítico, paralelização de fases |

## Referências Completas

Para orientação aprofundada sobre cada tópico, consulte:
- `.claude/skills/story-planning/SKILL.md`
- `.claude/skills/x-story-epic-full/references/decomposition-guide.md`
