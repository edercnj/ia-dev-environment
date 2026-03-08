---
name: x-story-epic
description: >
  Gerar um documento Epic a partir de uma especificação de sistema. Lê uma spec técnica
  e produz um arquivo Epic com regras de negócio transversais, definições de qualidade
  globais (DoR/DoD) e índice completo de histórias com declarações de dependência.
  Acione quando o usuário pedir para criar um epic, gerar epic a partir de spec,
  extrair regras de negócio, decompor especificação em epic, construir índice de histórias,
  ou qualquer variação de "leia esta spec e crie um epic".
---

# Criar Epic a partir de Especificação de Sistema

Esta skill lê um documento de especificação de sistema e gera um arquivo Epic — o artefato
de nível superior que define escopo, regras transversais, critérios de qualidade e índice
de histórias para um esforço de desenvolvimento.

## Por Que Isso Importa

O Epic é a fonte única de verdade para uma decomposição. Captura regras que abrangem múltiplas
histórias (evitando duplicação ou contradição), define quality gates que toda história deve
atender, e fornece o índice completo de histórias com relacionamentos de dependência.

## Pré-requisitos

Leia os seguintes arquivos antes de iniciar:

**Template (estrutura de saída):**
- `.claude/templates/_TEMPLATE-EPIC.md` — A estrutura exata a seguir

**Filosofia de decomposição:**
- `../../.claude/skills/x-story-epic-full/references/decomposition-guide.md`

Se algum template estiver ausente, pare e informe o usuário.

## Fluxo de Trabalho

### Passo 1: Ler a Spec de Entrada

Leia toda a especificação de sistema fornecida pelo usuário. Compreenda o escopo completo
antes de iniciar a extração.

### Passo 2: Extrair Regras de Negócio Transversais

Busque na spec regras de negócio que se aplicam a mais de uma jornada. Estas tornam-se
a tabela de Regras do Epic com IDs únicos (RULE-001, RULE-002, ...).

**O que qualifica como regra transversal:**
- Lógica de decisão que afeta múltiplas jornadas
- Restrição de plataforma (ex: "chave de idempotência obrigatória em todas as mutações")
- Política comportamental (ex: "tratamento de timeout: N segundos de sleep antes da resposta")
- Validação que controla múltiplas operações

**O que permanece em histórias individuais:**
- Regras que se aplicam a apenas uma jornada
- Detalhes de implementação específicos de um handler

### Passo 3: Identificar Histórias

Siga a abordagem camada por camada:

1. **Fundação (Camada 0):** Infraestrutura — servidores, schemas, APIs base
2. **Domínio Core (Camada 1):** Operação central que estabelece padrões arquiteturais
3. **Extensões (Camada 2):** Operações adicionais reutilizando padrões do core
4. **Composições (Camada 3):** Histórias combinando múltiplas capacidades
5. **Transversal (Camada 4):** Testes, observabilidade, segurança, tech debt

### Passo 4: Definir Critérios de Qualidade

**DoR Global:** Especificações técnicas validadas, dependências resolvidas, contratos revisados.

**DoD Global:** Metas de cobertura, tipos de teste obrigatórios, requisitos de documentação,
SLOs de performance.

### Passo 5: Gerar o Arquivo Epic

Escreva o Epic seguindo a estrutura do `_TEMPLATE-EPIC.md`:

1. **Header**: Título, autor, data, versão, status
2. **Visão Geral**: Escopo derivado da spec
3. **Anexos e Referências**: Links para spec e documentos relacionados
4. **Definições de Qualidade Globais**: DoR e DoD
5. **Regras de Negócio Transversais**: Tabela de regras
6. **Índice de Histórias**: Índice com links e dependências

### Passo 6: Salvar e Reportar

Salve o arquivo e reporte: número de regras extraídas, histórias identificadas, resumo
da estrutura de dependências.

## Regras de Idioma

- Todo conteúdo gerado deve estar em **Português Brasileiro (pt-BR)**
- Termos técnicos padrão da indústria permanecem em inglês
- Identificadores de código e nomes de campo permanecem em inglês
- IDs usam formato inglês: RULE-NNN, STORY-NNN

## Referências Detalhadas

Para orientação aprofundada sobre decomposição, consulte:
- `../../.claude/skills/x-story-epic/SKILL.md`
- `../../.claude/skills/x-story-epic-full/references/decomposition-guide.md`
