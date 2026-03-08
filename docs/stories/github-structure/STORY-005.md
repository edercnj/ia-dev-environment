# História: Skills de Review

**ID:** STORY-005

## 1. Dependências

| Blocked By | Blocks |
| :--- | :--- |
| STORY-001 | STORY-010, STORY-012 |

## 2. Regras Transversais Aplicáveis

| ID | Título |
| :--- | :--- |
| RULE-001 | Paridade funcional |
| RULE-002 | Convenções do Copilot |
| RULE-003 | Sem duplicação de conteúdo |
| RULE-005 | Progressive disclosure |

## 3. Descrição

Como **Tech Lead**, eu quero adaptar as 6 skills de review (`x-review`, `x-review-api`, `x-review-pr`, `x-review-grpc`, `x-review-events`, `x-review-gateway`) para `.github/skills/`, garantindo que o processo de code review automatizado mantenha a mesma cobertura e rigor.

As skills de review são de alta prioridade e formam o pilar de qualidade do repositório. Cada skill tem um foco especializado (API design, PR holístico, gRPC, eventos, gateway) e produz relatórios com scoring padronizado.

### 3.1 Skills a criar

- `.github/skills/x-review/SKILL.md` — Review paralelo com 8 engenheiros especialistas (Security, QA, Performance, Database, Observability, DevOps, API, Event)
- `.github/skills/x-review-api/SKILL.md` — Validação REST: RFC 7807, pagination, URL versioning, OpenAPI, status codes
- `.github/skills/x-review-pr/SKILL.md` — Tech Lead review com checklist de 40 pontos, decisão GO/NO-GO
- `.github/skills/x-review-grpc/SKILL.md` — Validação de proto3, service definitions, patterns
- `.github/skills/x-review-events/SKILL.md` — Validação de event schemas, producer/consumer, dead letter
- `.github/skills/x-review-gateway/SKILL.md` — Review de API gateway configuration

### 3.2 Padrão de descriptions

Cada description deve incluir keywords específicas para evitar colisão de trigger entre skills de review similares. Ex: `x-review-api` usa "REST", "RFC 7807", "OpenAPI"; `x-review-grpc` usa "gRPC", "proto3", "protobuf".

## 4. Definições de Qualidade Locais

### DoR Local (Definition of Ready)

- [ ] STORY-001 concluída (instructions base disponíveis)
- [ ] Skills `.claude/skills/x-review*` lidas e mapeadas
- [ ] Padrão de frontmatter validado em STORY-003

### DoD Local (Definition of Done)

- [ ] 6 skills criadas com frontmatter válido
- [ ] Descriptions diferenciadas para evitar colisão de trigger
- [ ] Body com workflow de review e formato de output
- [ ] References linkam para knowledge packs originais
- [ ] Copilot seleciona skill correta para cada tipo de review

### Global Definition of Done (DoD)

- **Validação de formato:** YAML frontmatter válido e parseável
- **Convenções Copilot:** `name` em lowercase-hyphens, `description` presente
- **Sem duplicação:** References linkam para `.claude/skills/`
- **Idioma:** Inglês
- **Progressive disclosure:** 3 níveis implementados
- **Documentação:** README.md atualizado

## 5. Contratos de Dados (Data Contract)

**Review Skill Contract:**

| Campo | Formato | Request | Response | Origem / Regra |
| :--- | :--- | :--- | :--- | :--- |
| `frontmatter.name` | string (lowercase-hyphens) | M | — | Ex: `x-review-api` |
| `frontmatter.description` | string (multiline) | M | — | Keywords específicas por tipo de review |
| `review_focus` | string | M | — | Foco do review (REST, gRPC, holístico, etc.) |
| `output_format` | string | M | — | Formato do relatório (score, GO/NO-GO, etc.) |

## 6. Diagramas

### 6.1 Fluxo de Review Paralelo

```mermaid
sequenceDiagram
    participant U as Usuário
    participant XR as x-review
    participant S1 as Security Engineer
    participant S2 as QA Engineer
    participant S3 as Performance Engineer
    participant R as Relatório Consolidado

    U->>XR: Solicitar code review
    XR->>S1: Review de segurança (paralelo)
    XR->>S2: Review de qualidade (paralelo)
    XR->>S3: Review de performance (paralelo)
    S1-->>R: Achados de segurança
    S2-->>R: Achados de qualidade
    S3-->>R: Achados de performance
    R-->>U: Relatório consolidado com score
```

## 7. Critérios de Aceite (Gherkin)

```gherkin
Cenario: Trigger diferenciado entre x-review-api e x-review-grpc
  DADO que ambas as skills de review existem em .github/skills/
  QUANDO o usuário solicita "review da API REST"
  ENTÃO o Copilot seleciona x-review-api
  E NÃO seleciona x-review-grpc

Cenario: x-review-pr com checklist de 40 pontos
  DADO que .github/skills/x-review-pr/SKILL.md está carregado
  QUANDO o Copilot executa o review de um PR
  ENTÃO o relatório cobre Clean Code, SOLID, arquitetura, testes, segurança
  E produz decisão GO/NO-GO

Cenario: Review paralelo com subagentes
  DADO que x-review define 8 especialistas paralelos
  QUANDO o body da skill é carregado
  ENTÃO o workflow instrui lançamento de reviews paralelos
  E consolida resultados em relatório único com score

Cenario: Description com keywords insuficientes
  DADO que x-review-events tem description genérica "review events"
  QUANDO o usuário solicita "validar event schemas"
  ENTÃO o trigger pode falhar por falta de keywords específicas
  MAS se a description inclui "event schemas, producer/consumer, dead letter"
  ENTÃO o trigger é preciso

Cenario: Referência a knowledge pack de security
  DADO que x-review referencia .claude/skills/security/SKILL.md
  QUANDO o review de segurança precisa de detalhes OWASP
  ENTÃO o link relativo aponta para o knowledge pack original
  E NÃO duplica o conteúdo em .github/skills/
```

## 8. Sub-tarefas

- [ ] [Dev] Criar `.github/skills/x-review/SKILL.md` com workflow de review paralelo
- [ ] [Dev] Criar `.github/skills/x-review-api/SKILL.md` com validação REST
- [ ] [Dev] Criar `.github/skills/x-review-pr/SKILL.md` com checklist de 40 pontos
- [ ] [Dev] Criar `.github/skills/x-review-grpc/SKILL.md` com validação proto3
- [ ] [Dev] Criar `.github/skills/x-review-events/SKILL.md` com validação de eventos
- [ ] [Dev] Criar `.github/skills/x-review-gateway/SKILL.md` com review de gateway
- [ ] [Test] Validar YAML frontmatter das 6 skills
- [ ] [Test] Verificar diferenciação de trigger entre skills similares
- [ ] [Doc] Documentar skills de review no README
