---
name: x-story-create
description: >
  Gerar arquivos de User Story detalhados a partir de um Epic e especificação de sistema.
  Produz um arquivo por história com contratos de dados completos, critérios de aceite Gherkin,
  diagramas de sequência Mermaid, declarações de dependência e sub-tarefas tagueadas.
  Acione quando o usuário pedir para criar histórias, gerar user stories a partir de epic,
  detalhar histórias com critérios de aceite, escrever cenários Gherkin, ou qualquer variação
  de "gere histórias a partir deste epic/spec".
---

# Criar Histórias a partir de Epic e Especificação de Sistema

Esta skill gera arquivos individuais de história — os itens de trabalho implementáveis que
desenvolvedores assumem e constroem. Cada história é autocontida: um desenvolvedor deve
conseguir implementá-la sem voltar à spec original.

## Por Que Histórias Autocontidas Importam

Uma história que diz "implementar a operação principal" é inútil sem o contrato de dados,
os mapeamentos de campo, as regras de validação e os códigos de erro. O desenvolvedor não
deve precisar alternar contexto entre a história e a spec.

## Pré-requisitos

Leia os seguintes arquivos antes de iniciar:

**Template (estrutura de saída):**
- `.claude/templates/_TEMPLATE-STORY.md` — A estrutura exata a seguir

**Filosofia de decomposição:**
- `../../.claude/skills/x-story-epic-full/references/decomposition-guide.md`

**Entradas obrigatórias do usuário:**
- O arquivo de especificação do sistema (spec original)
- O arquivo Epic (com índice de histórias e tabela de regras)

## Fluxo de Trabalho

### Passo 1: Ler o Epic e a Spec

Leia ambos os arquivos completamente. Do Epic, extraia:
- O índice de histórias (IDs, títulos, dependências)
- A tabela de regras (RULE-001..N)
- O DoD (copiado em cada história para referência rápida)

### Passo 2: Gerar Cada História

Para cada história no índice do Epic, crie um arquivo seguindo `_TEMPLATE-STORY.md`:

#### Seção 1 — Dependências

Tabela com Blocked By e Blocks, consistente com o índice do Epic.

#### Seção 2 — Regras Transversais Aplicáveis

Referencie apenas as regras do Epic que impactam esta história específica.

#### Seção 3 — Descrição

Formato user story: "Como **<Persona>**, eu quero <capacidade>, garantindo que <resultado>."
Seguido de contexto técnico detalhado.

#### Seção 4 — Definições de Qualidade Locais

DoR Local, DoD Local e DoD Global (copiado do Epic).

#### Seção 5 — Contratos de Dados

Seção mais crítica. Contratos devem ter precisão de copy-paste:
campos, tipos, formatos, flags M/O, regras de derivação.

#### Seção 6 — Diagramas

Diagramas de sequência Mermaid com nomes reais de componentes.

#### Seção 7 — Critérios de Aceite (Gherkin)

Cenários em português (DADO/QUANDO/ENTÃO/E/MAS):
1. Happy path com valores concretos
2. Violação de regra de negócio
3. Entrada malformada
4. Caso de borda

#### Seção 8 — Sub-tarefas

Tarefas granulares (2-4 horas cada): `[Dev]`, `[Test]`, `[Doc]`.

### Passo 3: Salvar e Reportar

Salve cada história como `STORY-NNN.md` e reporte o resumo.

## Heurísticas de Dimensionamento

**Grande demais** (divida): +2 endpoints, +1 fluxo de protocolo, +8 cenários Gherkin.

**Pequena demais** (una): sem endpoint testável, <2 cenários Gherkin.

**Tamanho ideal**: 1 capacidade clara, 4-8 cenários Gherkin, 4-8 sub-tarefas.

## Regras de Idioma

- Todo conteúdo gerado deve estar em **Português Brasileiro (pt-BR)**
- Termos técnicos em inglês permanecem em inglês
- Gherkin em português: `Cenario`, `DADO`, `QUANDO`, `ENTÃO`, `E`, `MAS`
- IDs em formato inglês: STORY-NNN

## Referências Detalhadas

Para orientação aprofundada, consulte:
- `../../.claude/skills/x-story-create/SKILL.md`
- `../../.claude/skills/x-story-epic-full/references/decomposition-guide.md`
