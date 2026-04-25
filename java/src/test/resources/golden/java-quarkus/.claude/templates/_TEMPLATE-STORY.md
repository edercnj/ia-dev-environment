# História: <Título da História>

**ID:** <story-XXXX-YYYY>
**Chave Jira:** <CHAVE-JIRA>
**Status:** Pendente

> **Status Transitions (Rule 22 — lifecycle-integrity):**
> valores permitidos `Pendente | Planejada | Em Andamento | Concluída | Falha | Bloqueada`.
> Transições válidas: `Pendente → Planejada | Em Andamento | Falha | Bloqueada`;
> `Planejada → Em Andamento | Falha | Bloqueada`;
> `Em Andamento → Concluída | Falha | Bloqueada`;
> reabertura `Concluída → Em Andamento` (via `x-status-reconcile --apply`) e
> `Falha → Pendente`; `Bloqueada → Pendente | Planejada | Em Andamento | Falha`.
> Ver [`.claude/rules/22-lifecycle-integrity.md`](../.claude/rules/22-lifecycle-integrity.md).

---

## 1. Contexto & Escopo

Como **<Persona>**, eu quero <ação/capacidade>, garantindo que <benefício/resultado esperado>.

<Contexto adicional (2-3 parágrafos) explicando o porquê desta história, como ela se encaixa no épico, e quaisquer decisões de design relevantes.>

### 1.1 Regras Transversais Aplicáveis

> Referência às regras definidas no Épico (seção 4). Listar apenas as regras que impactam esta história.

| ID | Título |
| :--- | :--- |
| <RULE-NNN> | <Título da regra> |

### 1.2 Entrega de Valor

- **Valor Principal:** <Descrição do valor de negócio mensurável>
- **Métrica de Sucesso:** <Como medir que o valor foi entregue>
- **Impacto no Negócio:** <Impacto direto para o usuário/stakeholder>

---

## 2. Packages (Hexagonal)

> Packages novos/tocados por esta história em cada camada.
> Camadas sem impacto: marcadas `—`.
> Direção: `adapter → application → domain` (inward only — Rule 04).

### Domain Layer

- `domain/<package>/` — <Entity|VO|Service|Port>: `<NomeClasse>`
- <ou `—` se nenhuma classe de domínio é alterada>

### Application Layer

- `application/<package>/` — UseCases: `<NomeUseCase>`
- Portas outbound: `<NomePort>`
- <ou `—`>

### Adapter Inbound

- `adapter/inbound/<type>/` — <Controller|CLI|Handler>: `<NomeClasse>`
- DTOs: `<NomeDTO>`
- <ou `—`>

### Adapter Outbound

- `adapter/outbound/<type>/` — `<NomeAdapter>`
- <ou `—`>

### Infrastructure

- `infrastructure/<package>/` — <Config|Observability>
- <ou `—`>

**Dependency direction:** `adapter.inbound/outbound → application → domain` (inward only).

---

## 3. Contratos & Endpoints

> Contratos de dados (request/response), endpoints expostos, ou eventos produzidos/consumidos.

### 3.1 Request

| Campo | Tipo | M/O | Validações | Exemplo |
| :--- | :--- | :--- | :--- | :--- |
| `<campo>` | `<UUID/BigDecimal/String(255)/Integer/List<String>>` | `<M ou O>` | `<min/max, regex, enum values>` | `<valor concreto>` |

### 3.2 Response

| Campo | Tipo | Sempre presente | Descrição |
| :--- | :--- | :--- | :--- |
| `<campo>` | `<UUID/String/BigDecimal>` | `<Sim ou Não>` | `<descrição do campo>` |

### 3.3 Error Codes Mapeados

| HTTP Status | Error Code | Condição | Mensagem (RFC 7807) |
| :--- | :--- | :--- | :--- |
| `<status>` | `<code>` | `<condição que dispara>` | `<mensagem padrão RFC 7807>` |

### 3.4 Event Schema (para event-driven)

> Incluir apenas quando `eventDriven: true`.

| Campo | Tipo | Obrigatório | Descrição |
| :--- | :--- | :--- | :--- |
| `eventType` | `String` | Sim | Tipo do evento |
| `eventVersion` | `String` | Sim | Versão do schema |
| `timestamp` | `Instant` | Sim | Momento da emissão (ISO-8601 UTC) |
| `correlationId` | `UUID` | Sim | ID de correlação |
| `payload` | `Object` | Sim | Payload do evento |

---

## 4. Materialização SOLID

> Regras de negócio e restrições SOLID aplicáveis a esta história.
> Referenciar regras do épico por ID (RULE-NNN). Adicionar restrições locais se necessário.

### 4.1 Regras Aplicáveis do Épico

| ID | Título | Impacto nesta história |
| :--- | :--- | :--- |
| <RULE-NNN> | <Título da regra do épico> | <Como se aplica aqui> |

### 4.2 Princípios SOLID Aplicáveis

- **SRP:** <Uma classe = uma razão para mudar. Ex: `PaymentController` trata apenas HTTP, delega lógica ao use case.>
- **OCP:** <Novo comportamento = nova classe, nunca modificar handlers existentes.>
- **LSP:** <Toda implementação deve cumprir o contrato da interface.>
- **ISP:** <Interfaces pequenas e focadas; sem implementações vazias.>
- **DIP:** <Depender de abstrações (ports), não de implementações concretas.>

### 4.3 Coding Constraints

- Métodos: ≤ 25 linhas (Rule 03)
- Classes: ≤ 250 linhas (Rule 03)
- Parâmetros: ≤ 4 por função (usar parameter object se mais)
- Linha: ≤ 120 caracteres

---

## 5. Quality Gates

### 5.1 Definition of Ready (DoR Local)

- [ ] <Pré-condição específica desta história>
- [ ] <Decisão técnica que precisa estar tomada>
- [ ] <Artefato/schema/config que precisa existir>

### 5.2 Acceptance Criteria (Gherkin)

> Gherkin scenarios MUST follow TPP order: degenerate → unconditional → conditions → iterations → edge cases.

```gherkin
Cenario: <Nome do cenário de caso degenerado>
  DADO que <pré-condição de caso nulo/vazio>
  QUANDO <ação>
  ENTÃO <comportamento esperado>

Cenario: <Nome do cenário de sucesso (happy path)>
  DADO que <pré-condição>
  QUANDO <ação>
  E <condição adicional>
  ENTÃO <resultado esperado>
  E <validação adicional>

Cenario: <Nome do cenário de erro>
  DADO que <pré-condição>
  QUANDO <ação com dados inválidos>
  ENTÃO <comportamento de erro esperado>
  E <validação de integridade>

Cenario: <Nome do cenário de borda>
  DADO que <condição de borda>
  QUANDO <ação>
  ENTÃO <comportamento esperado>
```

**Mandatory scenario categories:**
- [ ] Degenerate cases (null, empty, zero)
- [ ] Happy path (basic success)
- [ ] Error paths (each error type)
- [ ] Boundary values (at-min, at-max, past-max)

### 5.3 Definition of Done (DoD Local)

- [ ] <Critério de aceite implementado e validado>
- [ ] <Componente/handler/endpoint funcional>
- [ ] <Teste específico passando>
- [ ] Pelo menos 1 teste automatizado (unitário, integração ou E2E) validando o critério de aceite principal
- [ ] Smoke test passando (quando testing.smoke_tests == true)

### 5.4 Global DoD (Reference)

> Copiar do Épico. Mantido para referência rápida durante code review.

- **Cobertura:** ≥ 95% Line, ≥ 90% Branch (Rule 05 — absolute gate)
- **TDD Compliance:** test-first (RED→GREEN→REFACTOR). Tests incremental (TPP).
- **Double-Loop TDD:** Gherkin scenarios → acceptance tests (outer loop); unit tests via TPP (inner loop).

---

## 6. Segurança

> Controles de segurança aplicáveis a esta história (Rule 06).

| Área | Controle | Status |
| :--- | :--- | :--- |
| Input validation | <Validação de entrada esperada> | Required |
| Authentication | <Requerimentos de autenticação/autorização> | Required |
| Sensitive data | <Campos sensíveis e como protegê-los> | Required |
| Path operations | <Normalização de caminhos se aplicável> | If applicable |

**Forbidden (Rule 06):** SQL concatenation, hardcoded secrets, `Math.random()` for tokens, blindly following symlinks.

---

## 7. Observabilidade

> Logging, tracing, e métricas necessários para esta história (Rule 07).

### 7.1 Structured Logging

| Evento | Level | Campos obrigatórios |
| :--- | :--- | :--- |
| <nome do evento> | `INFO/WARN/ERROR` | `trace_id`, `span_id`, `<domain field>` |

### 7.2 Metrics

| Métrica | Tipo | Tags | SLO |
| :--- | :--- | :--- | :--- |
| `<nome_da_metrica>` | `counter/gauge/histogram` | `<tags>` | <SLO se aplicável> |

**Correlation ID propagation:** `X-Correlation-ID` header must flow through all downstream calls.

---

## 8. Decision Rationale

> Registro de decisões de design desta história.
> Obrigatório: ≥ 1 item com o micro-template de 4 linhas (Rule 24 / planning-standards-kp).
> Aceita `N/A — <motivo curto>` apenas quando genuinamente sem trade-off relevante.

**Decisão:** <statement da decisão tomada>
**Motivo:** <por que esta opção foi escolhida — restrição técnica ou de negócio>
**Alternativa descartada:** <o que foi rejeitado e por quê>
**Consequência:** <trade-off ou implicação futura>

---

## 9. Dependências & File Footprint

### 9.1 Dependências

| Blocked By | Blocks |
| :--- | :--- |
| <story-XXXX-YYYY ou -> | <story-XXXX-YYYY ou -> |

### 9.2 Tasks

> Each task = 1 branch = 1 PR. Minimum 3, maximum 8. Ideal size: M (50-150 LOC).

#### TASK-{{EPIC_ID}}-{{STORY_ID}}-001: <Imperative title (max 80 chars)>

- **Layer:** <Domain|Port|Adapter|Application|Config|Test|Doc>
- **Test Type:** <Unit|Integration|API|Contract|E2E|Smoke|Verification>
- **Size:** <S|M|L>
- **Dependencies:** <TASK IDs or —>
- **Branch:** `feat/task-{{EPIC_ID}}-{{STORY_ID}}-001-short-desc`
- **Testability:** <Domain + UnitTest|Port + Adapter + IT|UseCase + AT|Endpoint + APITest>
- **Files:**
  - `path/to/file1.ext`
  - `path/to/file2.ext`
- **Acceptance Criteria:**
  - [ ] <criterion 1>
  - [ ] <criterion 2>

#### TASK-{{EPIC_ID}}-{{STORY_ID}}-002: <Imperative title (max 80 chars)>

- **Layer:** <Domain|Port|Adapter|Application|Config|Test|Doc>
- **Test Type:** <Unit|Integration|API|Contract|E2E|Smoke|Verification>
- **Size:** <S|M|L>
- **Dependencies:** TASK-{{EPIC_ID}}-{{STORY_ID}}-001
- **Branch:** `feat/task-{{EPIC_ID}}-{{STORY_ID}}-002-short-desc`
- **Testability:** <Valid pattern>
- **Files:**
  - `path/to/file1.ext`
- **Acceptance Criteria:**
  - [ ] <criterion 1>

#### TASK-{{EPIC_ID}}-{{STORY_ID}}-003: <Imperative title (max 80 chars)>

- **Layer:** <Domain|Port|Adapter|Application|Config|Test|Doc>
- **Test Type:** <Unit|Integration|API|Contract|E2E|Smoke|Verification>
- **Size:** <S|M|L>
- **Dependencies:** TASK-{{EPIC_ID}}-{{STORY_ID}}-001, TASK-{{EPIC_ID}}-{{STORY_ID}}-002
- **Branch:** `feat/task-{{EPIC_ID}}-{{STORY_ID}}-003-short-desc`
- **Testability:** <Valid pattern>
- **Files:**
  - `path/to/file1.ext`
- **Acceptance Criteria:**
  - [ ] <criterion 1>

### 9.3 File Footprint (EPIC-0041 parallelism evaluation)

```
write:
  - <path/to/new/file.java>
read:
  - <path/to/existing/dependency.java>
regen:
  - <path/to/golden/file.md>
```
