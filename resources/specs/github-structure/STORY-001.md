# História: Fundação de Instructions do Copilot

**ID:** STORY-001

## 1. Dependências

| Blocked By | Blocks |
| :--- | :--- |
| - | STORY-002 |

## 2. Regras Transversais Aplicáveis

| ID | Título |
| :--- | :--- |
| RULE-001 | Paridade funcional entre .claude e .github |
| RULE-002 | Aderência às convenções oficiais do Copilot |
| RULE-003 | Sem duplicação de conteúdo técnico (usar referências) |
| RULE-004 | Idioma em inglês, exceto skills de story em pt-BR |

## 3. Descrição

Como **Tech Lead de Plataforma**, eu quero estruturar `copilot-instructions.md` e `instructions/*.instructions.md`, garantindo que o Copilot carregue contexto global e contextual sem ambiguidade.

Mapeia `.claude/rules/` para o modelo nativo do Copilot com adaptação de formato e semântica.

Define idioma, escopo de carregamento e política de referência para as camadas seguintes.

### 3.1 Mapeamento de arquivos

- `01-project-identity.md` -> `copilot-instructions.md`.
- `02..05` -> `instructions/*.instructions.md`.
- Sem copy-paste literal de blocos extensos.

### 3.2 Compatibilidade de loading

- Global carregado automaticamente.
- Arquivos contextuais carregados sob demanda.
- Links relativos válidos para referências.

## 4. Definições de Qualidade Locais

### DoR Local (Definition of Ready)

- [ ] Dependências de `Blocked By` concluídas.
- [ ] Convenções de naming/frontmatter validadas para o componente.
- [ ] Critérios de aceite e evidências de teste definidos.

### DoD Local (Definition of Done)

- [ ] Artefatos da história criados no formato exigido.
- [ ] Validação estrutural da história aprovada sem erro crítico.
- [ ] Evidências de execução registradas para revisão.

### Global Definition of Done (DoD)

- **Cobertura:** validação de 100% dos artefatos `.github/` gerados neste épico.
- **Testes Automatizados:** checks de naming, frontmatter, links relativos e carregamento de skills/prompts.
- **Relatório de Cobertura:** saída consolidada pass/fail por componente.
- **Documentação:** README da `.github/` atualizado com estrutura e fluxo de uso.
- **Persistência:** arquivos versionados com ordenação determinística e sem duplicação literal.
- **Performance:** hooks síncronos dentro do timeout configurado (<= 60s).

## 5. Contratos de Dados (Data Contract)

**Instruction Mapping Contract:**

| Campo | Formato | Request | Response | Origem / Regra |
| :--- | :--- | :--- | :--- | :--- |
| `source_rule_file` | string path | M | - | Origem em `.claude/rules/*.md` |
| `target_file` | string path | M | - | Destino em `.github/` |
| `scope` | enum(global,contextual) | M | - | Define estratégia de carregamento |
| `language` | enum(en-US,pt-BR) | M | - | en-US por padrão, exceções explícitas |

## 6. Diagramas

### 6.1 Fluxo principal

```mermaid
sequenceDiagram
    participant U as Usuário
    participant C as Copilot Runtime
    participant A as Artefatos da História
    participant V as Validador

    U->>C: Solicitar execução da capacidade
    C->>A: Carregar arquivos e metadados
    A->>V: Executar validações estruturais
    V-->>C: Pass/Fail detalhado
    C-->>U: Resultado + evidências
```

## 7. Critérios de Aceite (Gherkin)

```gherkin
Cenario: Fluxo de sucesso de STORY-001
  DADO que os pré-requisitos da história estão atendidos
  QUANDO a implementação é executada conforme o contrato
  ENTÃO o resultado esperado da capacidade é entregue
  E os artefatos obrigatórios ficam disponíveis

Cenario: Violação de regra transversal em STORY-001
  DADO que uma regra do épico não foi respeitada
  QUANDO a validação de conformidade roda
  ENTÃO a história é reprovada com evidência objetiva
  E a correção é marcada como bloqueadora

Cenario: Input malformado em STORY-001
  DADO que um arquivo obrigatório está malformado
  QUANDO o parser do Copilot tenta carregar o artefato
  ENTÃO o carregamento falha com erro explícito
  E o relatório identifica arquivo e campo inválido

Cenario: Edge case de concorrência/ordem em STORY-001
  DADO que duas alterações paralelas ocorrem na mesma camada
  QUANDO a validação de dependências é executada
  ENTÃO a ordem de execução respeita o DAG definido
  E não há regressão de artefatos já válidos
```

## 8. Sub-tarefas

- [ ] [Dev] Implementar artefatos e estrutura da história.
- [ ] [Dev] Garantir aderência a naming/frontmatter do Copilot.
- [ ] [Test] Executar validação estrutural e de dependências.
- [ ] [Test] Cobrir cenário de erro e edge case da história.
- [ ] [Doc] Atualizar documentação de uso e manutenção.
