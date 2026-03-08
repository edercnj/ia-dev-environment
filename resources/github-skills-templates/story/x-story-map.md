---
name: x-story-map
description: >
  Gerar um Mapa de Implementação a partir de um Epic e suas Histórias. Computa fases de
  implementação a partir do grafo de dependências, identifica o caminho crítico, produz
  diagramas de fase ASCII, grafos de dependência Mermaid e observações estratégicas sobre
  gargalos e paralelismo. Acione quando o usuário pedir para criar mapa de implementação,
  gerar grafo de dependências, computar fases, identificar caminho crítico, planejar ordem
  de implementação, ou qualquer variação de "crie um plano a partir destas histórias".
---

# Criar Mapa de Implementação a partir de Epic e Histórias

Esta skill recebe o Epic e todos os arquivos de Story e computa o plano de implementação:
quais histórias podem rodar em paralelo, qual o tempo mínimo de implementação, onde estão
os gargalos e como otimizar a alocação do time.

## Por Que Isso Importa

Sem um grafo de dependências, times ou serializam tudo (desperdiçando paralelismo) ou
iniciam histórias fora de ordem (encontrando bloqueios no meio do sprint). O mapa torna
a estrutura de dependências explícita e computável.

## Pré-requisitos

Leia os seguintes arquivos antes de iniciar:

**Template (estrutura de saída):**
- `.claude/templates/_TEMPLATE-IMPLEMENTATION-MAP.md` — A estrutura exata a seguir

**Entradas obrigatórias:**
- O arquivo Epic (com índice de histórias e declarações de dependência)
- Todos os arquivos de Story (com tabelas Blocked By / Blocks)

## Fluxo de Trabalho

### Passo 1: Construir a Matriz de Dependências

Leia a Seção 1 de cada história e o índice do Epic. Construa a matriz completa:

| Story | Título | Blocked By | Blocks | Status |

**Validações:**
- Toda história do índice do Epic deve aparecer na matriz
- Dependências devem ser simétricas
- Sem dependências circulares
- Histórias raiz (sem bloqueios) devem existir

### Passo 2: Computar Fases

Agrupe histórias em fases usando o DAG de dependências:

1. **Fase 0**: Histórias sem dependências (raízes)
2. **Fase 1**: Histórias cujas dependências estão todas na Fase 0
3. **Fase N**: Histórias cujas dependências estão todas nas Fases 0..N-1

Crie o diagrama de fase ASCII com caracteres box-drawing.

### Passo 3: Identificar o Caminho Crítico

O caminho crítico é a cadeia mais longa de dependências da raiz à folha.
Qualquer atraso em uma história do caminho crítico atrasa diretamente a entrega final.

### Passo 4: Gerar o Grafo de Dependências Mermaid

Crie um `graph TD` completo com todas as histórias e arestas de dependência.
Use classDef por fase para coloração consistente.

### Passo 5: Criar Tabela Resumo de Fases

| Fase | Histórias | Camada | Paralelismo | Pré-requisito |

### Passo 6: Detalhar Cada Fase

Para cada fase: tabela de escopo, artefatos chave e entregas concretas.

### Passo 7: Escrever Observações Estratégicas

- **Gargalo Principal**: Qual história bloqueia mais outras
- **Histórias Folha**: Histórias sem dependentes (absorvem atrasos)
- **Otimização de Tempo**: Onde o paralelismo é maximizado
- **Dependências Cruzadas**: Pontos de convergência
- **Marco de Validação Arquitetural**: Checkpoint antes de expandir escopo

### Passo 8: Salvar e Reportar

Salve como `IMPLEMENTATION-MAP.md` e reporte o resumo.

## Regras de Idioma

- Todo conteúdo gerado deve estar em **Português Brasileiro (pt-BR)**
- IDs de nós Mermaid e nomes classDef permanecem em inglês
- Nomes de fase em português (ex: "Fase 0 — Fundação")
- Termos técnicos: "critical path" → "caminho crítico", "bottleneck" → "gargalo"

## Referências Detalhadas

Para orientação aprofundada, consulte:
- `../../.claude/skills/x-story-map/SKILL.md`
- `../../.claude/skills/x-story-epic-full/references/decomposition-guide.md`
