# Prompt: Geração de Épico e Histórias — ia-dev-environment Architectural Improvements

> **Instrução de uso**: Cole este prompt integralmente no Claude Code dentro do repositório
> `ia-dev-environment` e execute `/x-story-epic-full` com ele como especificação de entrada.
> Alternativamente, salve como `docs/specs/SPEC-architectural-improvements-v1.md` e referencie
> via `/x-story-epic-full docs/specs/SPEC-architectural-improvements-v1.md`.

---

## Sistema

**Projeto**: `ia-dev-environment` — CLI generator de ambientes de desenvolvimento assistidos por IA.

**Versão base analisada**: `v2.0.0-SNAPSHOT` (branch `main`, ~1003 commits).

**Objetivo desta especificação**: Endereçar gaps arquiteturais, de conhecimento e de qualidade de
geração identificados em auditoria do gerador. O resultado esperado é um gerador capaz de produzir
artefatos (rules, knowledge packs, skills, profiles, agents) com granularidade e precisão
suficientes para que agentes de IA executem implementações complexas com o **mínimo de alucinação
possível**.

**Princípio central de todas as histórias**: LLMs alucam principalmente por dois motivos — ausência
de exemplos negativos explícitos (anti-patterns com código real) e knowledge packs abstratos
demais para guiar síntese de código. Toda melhoria deve ser avaliada contra este princípio.

---

## Escopo do Épico

### Contexto de negócio

O gerador atual produz ambientes funcionais para stacks comuns (Java Spring, Go Gin, Quarkus, etc.)
mas trata **estilo arquitetural** como implícito nos coding standards. Isso força o agente a
inferir a estrutura de pacotes, as regras de boundary e os padrões corretos a partir de descrições
textuais genéricas — terreno fértil para alucinação.

A melhoria central é introduzir uma **segunda dimensão de configuração**: além do stack tecnológico
(language + framework + database), o gerador passa a receber o **estilo arquitetural** como
parâmetro explícito, gerando knowledge packs, rules, templates de camadas e suítes de validação
específicos para cada estilo.

### Dimensões de melhoria

1. **Anti-patterns explícitos** — rules com exemplos de código errado/certo por domínio
2. **Knowledge packs especializados por estilo arquitetural** — com código de referência embutido
3. **Profiles arquiteturais dedicados** — segunda axis além do stack tecnológico
4. **Checklist de review estendido** — cobertura de event-driven e compliance/fintech
5. **API-first como fase explícita do lifecycle** — OpenAPI/AsyncAPI/proto antes da implementação
6. **DDD estratégico** — context map, bounded context, anti-corruption layer como artefatos
7. **ArchUnit integration** — validação automática de architectural boundaries em CI
8. **Contratos de story mais ricos** — schemas completos com tipos, validações e error codes

---

## Regras de Negócio Transversais (Cross-Cutting Rules)

**RULE-001**: Todo knowledge pack especializado DEVE conter ao menos um bloco de código de
referência completo e compilável no estilo do stack-alvo. Descrições puramente textuais de padrões
são insuficientes.

**RULE-002**: Todo arquivo de anti-patterns DEVE ter, para cada anti-pattern listado: (a) código
errado com comentário explicando POR QUE é errado, (b) código correto equivalente, (c) referência
à rule ou knowledge pack que define o comportamento correto.

**RULE-003**: Todo novo profile arquitetural DEVE incluir um `layer-templates` knowledge pack
específico com a estrutura de pacotes canônica para aquele estilo, diferenciando-se do template
genérico atual.

**RULE-004**: Todo profile que declare `architectureStyle: hexagonal` ou `architectureStyle: ddd`
DEVE gerar uma suíte ArchUnit mínima com ao menos 3 regras de boundary testadas em CI.

**RULE-005**: Qualquer skill ou fase do lifecycle que envolva design de interface (REST, gRPC,
AsyncAPI) DEVE produzir o contrato formal antes de qualquer código de implementação.

**RULE-006**: A geração de anti-patterns DEVE ser condicional ao `language` + `framework` do
profile. Anti-patterns Java Spring não devem aparecer em projetos Go ou Python.

**RULE-007**: Knowledge packs de arquitetura especializada (hexagonal, cqrs, saga, outbox) DEVEM
ser referenciados explicitamente nos agents `architect` e `tech-lead` gerados, para que o agente
tenha acesso ao contexto correto durante review e planejamento.

**RULE-008**: O checklist do `/x-review-pr` DEVE ter seções condicionais ativadas por flags do
config (`eventDriven: true`, `compliance: pci-dss`, `compliance: lgpd`, etc.), não ser um
checklist único e estático.

**RULE-009**: Profiles de CQRS/Event Sourcing DEVEM gerar configuração de infraestrutura para
event store (EventStoreDB ou Axon Server) no `docker-compose.yml` e nos manifests Kubernetes.

**RULE-010**: O skill `/x-story-create` DEVE gerar data contracts com tipos explícitos, regras de
validação (min/max, regex, enum values) e mapeamento completo de error codes para cada endpoint ou
evento declarado na story.

---

## Histórias

---

### STORY-0001: Anti-Patterns Rules por Language/Framework

**Título**: Geração de `07-anti-patterns.md` condicional por language e framework

**Tipo**: Feature — Template + Assembler

**Prioridade**: Alta (maior ROI em redução de alucinação com menor esforço de implementação)

**Dependências**: Nenhuma. Esta story é independente e pode ser o ponto de entrada do épico.

**Contexto técnico**:
O gerador atual produz 6 arquivos de rules (project-identity, domain, coding-standards,
architecture-summary, quality-gates, security-baseline). Nenhum deles contém exemplos de código
incorreto. O agente recebe apenas regras positivas e é livre para aluucinar implementações que
violam os patterns sem receber feedback estrutural.

Esta story adiciona um `07-anti-patterns.md` para cada combinação language/framework suportada,
gerado condicionalmente pelo `RulesAssembler` existente.

**Escopo de implementação**:

O template `07-anti-patterns.md` para `java-spring` DEVE cobrir os seguintes anti-patterns, cada
um com bloco de código incorreto anotado e bloco de código correto equivalente:

- Service layer com lógica de negócio misturada com lógica de persistência (God Service)
- Controller chamando Repository diretamente (bypass de service layer)
- `@Transactional` em método privado (sem efeito — Spring proxy não intercepta)
- `Optional.get()` sem `isPresent()` — NPE em produção
- `List<Entity>` retornado diretamente de endpoint REST (sem paginação, sem DTO)
- `catch (Exception e) { e.printStackTrace(); }` — swallowing exceptions sem log estruturado
- Injeção de dependência por field (`@Autowired` em field) vs constructor injection
- `@Scheduled` com lógica de negócio diretamente no método (viola SRP)
- Entity JPA com lógica de negócio (anemic domain model vs rich domain model)
- `Thread.sleep()` em testes de integração (timing-dependent tests)

Para `java-quarkus`, DEVE adicionar os anti-patterns Quarkus-específicos:
- Uso de `@Inject` em métodos estáticos (não suportado pelo CDI)
- Blocking I/O em Vert.x event loop thread (viola o modelo reativo)
- `@Transactional` em beans `@ApplicationScoped` sem `@Transactional` no método (escopo errado)

Para `go-gin`, DEVE cobrir:
- Error handling com `if err != nil { return }` sem wrap de contexto (`fmt.Errorf("context: %w", err)`)
- Goroutine leak em handlers HTTP (goroutine sem canal de cancelamento)
- Global state em handlers (não thread-safe)
- JSON unmarshaling sem validação de campos obrigatórios

Para `python-fastapi`, DEVE cobrir:
- `async def` com código bloqueante (requests, psycopg2 síncrono dentro de corrotina)
- Dependências globais mutáveis entre requests
- Ausência de `response_model` em endpoints (retorno de dados sensíveis acidental)

Para os demais profiles (`kotlin-ktor`, `rust-axum`, `typescript-nestjs`,
`typescript-commander-cli`, `python-click-cli`, `java-picocli-cli`), DEVE gerar ao menos 5
anti-patterns relevantes à linguagem/framework.

**Estrutura do template gerado** (para cada entrada):

```markdown
## ANTI-001 — [Nome do Anti-Pattern]

**Categoria**: [Service Layer | Persistence | Transaction | Error Handling | Security | Testing | Concurrency]
**Severidade**: [CRITICAL | HIGH | MEDIUM]
**Regra violada**: [referência ao rule file correspondente, ex: `03-coding-standards.md#section`]

### ❌ Código Incorreto

` `` [language]
// PROBLEMA: [explicação em 1-2 linhas de POR QUE este código é problemático]
// CONSEQUÊNCIA: [o que acontece em produção quando este pattern é usado]
[código real e compilável demonstrando o anti-pattern]
` ``

### ✅ Código Correto

` `` [language]
// SOLUÇÃO: [explicação do que foi corrigido e por quê]
[código real e compilável com o pattern correto]
` ``

### Quando identificar

[Padrão de reconhecimento: o que no código indica a presença deste anti-pattern]
```

**Critérios de Aceitação (DoD)**:

- [ ] `RulesAssembler.java` gera `07-anti-patterns.md` condicionalmente baseado em `language` + `framework`
- [ ] Cada profile bundled tem seu arquivo de anti-patterns correspondente nos golden files
- [ ] Template `java-spring` contém os 10 anti-patterns especificados com código compilável
- [ ] Template `java-quarkus` contém os 3 anti-patterns Quarkus-específicos adicionais
- [ ] Template `go-gin` contém os 4 anti-patterns Go especificados
- [ ] Template `python-fastapi` contém os 3 anti-patterns FastAPI especificados
- [ ] Demais profiles contêm ao menos 5 anti-patterns cada
- [ ] Golden file parity tests (`mvn verify -Pintegration-tests`) passam para todos os 10 profiles
- [ ] Line coverage ≥ 95%, branch coverage ≥ 90% para o código novo
- [ ] Nenhum anti-pattern de uma linguagem aparece no output de outra linguagem (teste de isolamento)

**Gherkin**:

```gherkin
Feature: Geração condicional de anti-patterns por language/framework

  Scenario: Profile java-spring gera anti-patterns específicos de Spring
    Given um config com language "java" e framework "spring-boot"
    When o gerador executa
    Then o arquivo ".claude/rules/07-anti-patterns.md" é gerado
    And o arquivo contém o anti-pattern "ANTI-001" com bloco de código incorreto e correto
    And o arquivo contém ao menos 10 entradas de anti-pattern
    And nenhuma entrada faz referência a APIs de outro framework

  Scenario: Profile go-gin não gera anti-patterns Java
    Given um config com language "go" e framework "gin"
    When o gerador executa
    Then o arquivo ".claude/rules/07-anti-patterns.md" é gerado
    And o arquivo não contém "@Transactional" nem "@Autowired"
    And o arquivo contém anti-patterns específicos de Go

  Scenario: Anti-pattern sem código de referência causa falha de validação
    Given um template de anti-pattern sem bloco de código "❌ Código Incorreto"
    When o assembler processa o template
    Then uma exceção "InvalidTemplateException" é lançada
    And a mensagem contém "anti-pattern requires both incorrect and correct code examples"
```

---

### STORY-0002: Knowledge Pack — Hexagonal Architecture com Código de Referência

**Título**: Knowledge pack `architecture-hexagonal` com estrutura de pacotes canônica e ArchUnit

**Tipo**: Feature — Template + Assembler + Conditional Profile Flag

**Prioridade**: Alta

**Dependências**: STORY-0001 (conceitos de anti-pattern influenciam o formato dos exemplos negativos
no knowledge pack)

**Contexto técnico**:
O knowledge pack `architecture` atual é genérico. Quando o agente recebe uma instrução como
"implemente usando arquitetura hexagonal", ele tem o conceito mas não tem o mapa concreto de:
onde cada tipo de classe vive no projeto, como nomear as interfaces de port, como estruturar os
adapters, e quais imports são proibidos em cada camada. Resultado: código com Spring annotations
no domain, repositories injetados diretamente em services, e arquitetura hexagonal apenas no nome.

Esta story cria um knowledge pack especializado `architecture-hexagonal` que é gerado quando
`architectureStyle: hexagonal` está declarado no config, substituindo (ou complementando) o pack
`architecture` genérico.

**Escopo de implementação**:

O knowledge pack gerado DEVE conter:

**Seção 1 — Estrutura de Pacotes Canônica** (como texto e como `layer-templates` reference):

```
{basePackage}/
├── domain/
│   ├── model/          # Entidades, Value Objects, Aggregates — SEM dependência de framework
│   ├── port/
│   │   ├── in/         # Interfaces de use case (CommandPort, QueryPort)
│   │   └── out/        # Interfaces de repositório e serviços externos
│   └── service/        # Implementações de use case — dependem APENAS de domain/port
├── application/
│   └── usecase/        # Orchestration, não lógica — chama domain services
├── adapter/
│   ├── in/
│   │   ├── web/        # Controllers REST, gRPC stubs, Consumers Kafka
│   │   └── cli/        # Command handlers
│   └── out/
│       ├── persistence/ # JPA Repositories, MongoDB Repositories
│       ├── messaging/   # Producers Kafka, SQS
│       └── client/     # HTTP clients para serviços externos
└── config/             # Spring @Configuration, Bean definitions — SEM lógica de negócio
```

**Seção 2 — Regras de Dependência** (explícitas e com exemplos):

Para cada direção de dependência proibida, o pack DEVE ter um exemplo de código incorreto e
o erro que o ArchUnit lançaria ao detectar a violação.

**Seção 3 — Exemplos de Port/Adapter** (código Java completo e compilável):

Port de entrada (use case interface):
```java
// domain/port/in/ProcessPaymentPort.java
// REGRA: sem imports de Spring, JPA, ou qualquer framework
public interface ProcessPaymentPort {
    PaymentResult execute(ProcessPaymentCommand command);
}
```

Port de saída (repositório interface):
```java
// domain/port/out/PaymentRepository.java
// REGRA: usa apenas tipos do domain, nunca JPA entities diretamente
public interface PaymentRepository {
    void save(Payment payment);
    Optional<Payment> findById(PaymentId id);
}
```

Adapter de entrada (controller):
```java
// adapter/in/web/PaymentController.java
// REGRA: injeta apenas ports de entrada, nunca domain services diretamente
@RestController
public class PaymentController {
    private final ProcessPaymentPort processPayment; // port, não service
    // ...
}
```

Adapter de saída (repositório):
```java
// adapter/out/persistence/PaymentJpaAdapter.java
// REGRA: implementa port de saída, converte entre domain model e JPA entity
@Component
public class PaymentJpaAdapter implements PaymentRepository {
    // mapeia Payment (domain) <-> PaymentEntity (JPA)
}
```

**Seção 4 — Suíte ArchUnit Mínima** (template de teste gerado automaticamente):

```java
// src/test/java/{basePackage}/architecture/HexagonalArchitectureTest.java
@AnalyzeClasses(packages = "{basePackage}")
public class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule domain_should_not_depend_on_spring =
        noClasses().that().resideInAPackage("{basePackage}.domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "javax.persistence..", "jakarta.persistence..");

    @ArchTest
    static final ArchRule adapters_should_not_depend_on_each_other =
        noClasses().that().resideInAPackage("{basePackage}.adapter..")
            .should().dependOnClassesThat()
            .resideInAPackage("{basePackage}.adapter..");

    @ArchTest
    static final ArchRule domain_services_should_only_use_domain_ports =
        classes().that().resideInAPackage("{basePackage}.domain.service..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("{basePackage}.domain..", "java..", "kotlin..");
}
```

Este arquivo DEVE ser gerado como parte do template de testes quando `architectureStyle: hexagonal`
e `language: java`.

**Configuração do profile** — novo campo no YAML de config:

```yaml
architecture:
  style: hexagonal          # hexagonal | layered | cqrs | event-driven | clean
  validateWithArchUnit: true # gera HexagonalArchitectureTest.java
  basePackage: com.example.service
```

**Data Contract — config YAML input**:

| Campo | Tipo | Obrigatório | Valores aceitos | Default |
|-------|------|-------------|-----------------|---------|
| `architecture.style` | `enum` | Não | `hexagonal`, `layered`, `cqrs`, `event-driven`, `clean` | `layered` |
| `architecture.validateWithArchUnit` | `boolean` | Não | `true`, `false` | `false` |
| `architecture.basePackage` | `string` | Sim se style != layered | package válido Java/Kotlin | — |

**Critérios de Aceitação (DoD)**:

- [ ] Campo `architecture.style` adicionado ao model `ArchitectureConfig.java` e ao schema de validação
- [ ] `KnowledgePackAssembler` gera `architecture-hexagonal/KNOWLEDGE.md` quando `style: hexagonal`
- [ ] O knowledge pack gerado contém as 4 seções especificadas com código compilável
- [ ] `TestAssembler` gera `HexagonalArchitectureTest.java` quando `style: hexagonal` e `language: java`
- [ ] Template de `layer-templates` knowledge pack é substituído pela estrutura de pacotes hexagonal
- [ ] Golden file para profile `java-spring-hexagonal` (novo profile) passa parity test
- [ ] `validate` command rejeita `architecture.validateWithArchUnit: true` sem `architecture.basePackage`
- [ ] ArchUnit adicionado como dependência no `pom.xml` gerado quando `validateWithArchUnit: true`
- [ ] Line coverage ≥ 95%, branch coverage ≥ 90%

**Gherkin**:

```gherkin
Feature: Geração de knowledge pack de arquitetura hexagonal

  Scenario: Config com style hexagonal gera knowledge pack especializado
    Given um config com "architecture.style: hexagonal" e "architecture.basePackage: com.acme.payment"
    When o gerador executa
    Then o arquivo ".claude/skills/architecture-hexagonal/KNOWLEDGE.md" é gerado
    And o arquivo contém a seção "Estrutura de Pacotes Canônica"
    And o arquivo contém exemplos de código Java para Port de entrada e saída
    And o arquivo contém a seção "Regras de Dependência" com exemplos de violação

  Scenario: Config hexagonal com java gera suíte ArchUnit
    Given um config com "architecture.style: hexagonal", "language: java", "validateWithArchUnit: true"
    When o gerador executa
    Then o arquivo "src/test/java/com/acme/payment/architecture/HexagonalArchitectureTest.java" é gerado
    And o arquivo contém ao menos 3 "@ArchTest" rules
    And o pom.xml gerado contém dependência "com.tngtech.archunit:archunit-junit5"

  Scenario: Config sem basePackage rejeita validateWithArchUnit
    Given um config com "architecture.style: hexagonal" e "validateWithArchUnit: true" sem "basePackage"
    When o comando validate executa
    Then o resultado é inválido
    And a mensagem de erro contém "basePackage is required when validateWithArchUnit is true"

  Scenario: Config com style layered (default) não gera conhecimento hexagonal
    Given um config sem campo "architecture.style" declarado
    When o gerador executa
    Then o arquivo ".claude/skills/architecture-hexagonal/KNOWLEDGE.md" NÃO é gerado
    And o arquivo ".claude/skills/architecture/KNOWLEDGE.md" é gerado normalmente
```

---

### STORY-0003: Knowledge Pack — CQRS + Event Sourcing

**Título**: Knowledge pack `architecture-cqrs` com command bus, event store e projection patterns

**Tipo**: Feature — Template + Assembler

**Prioridade**: Alta

**Dependências**: STORY-0002 (reutiliza o mecanismo de `architecture.style` conditional)

**Contexto técnico**:
CQRS com Event Sourcing é o padrão de maior complexidade geracional para LLMs. A confusão mais
comum: implementar CQRS apenas como "separação de classes" mantendo o mesmo modelo de dados e
a mesma base de dados — o que não entrega nenhum dos benefícios do padrão e adiciona toda a
complexidade. O knowledge pack deve dar ao agente o mapa completo: o que muda no modelo de dados,
como o event store funciona, como projeções são construídas e reconstruídas.

Esta story cria um knowledge pack especializado `architecture-cqrs` que é gerado quando
`architecture.style: cqrs` está declarado no config.

**Escopo de implementação**:

O knowledge pack `architecture-cqrs` DEVE conter:

**Seção 1 — Separação de Modelos**: diagrama Mermaid + explicação de write model (Aggregate) vs
read model (Projection), com exemplos de código para cada um. O agente DEVE entender que são
datastores diferentes.

**Seção 2 — Command Bus**: interface do command dispatcher, command handler, command result.
Exemplo completo com `ProcessPaymentCommand`, `ProcessPaymentCommandHandler`, e como o handler
interage com o aggregate.

**Seção 3 — Event Store**: interface `EventStore` com `append(streamId, events, expectedVersion)`
e `load(streamId)`. Explicação de optimistic concurrency com `expectedVersion`. Exemplo de
implementação com PostgreSQL (tabela `events`) e com EventStoreDB.

**Seção 4 — Aggregate com Event Sourcing**:

```java
// Aggregate que deriva estado de eventos — sem getters/setters de estado mutável externo
public class Payment extends AggregateRoot {
    private PaymentId id;
    private PaymentStatus status;
    private Money amount;

    // Apply methods reconstroem estado a partir de eventos
    @EventHandler
    private void apply(PaymentInitiated event) {
        this.id = event.paymentId();
        this.status = PaymentStatus.PENDING;
        this.amount = event.amount();
    }

    // Command handler emite eventos, não muda estado diretamente
    public List<DomainEvent> process(ProcessPaymentCommand command) {
        if (this.status != PaymentStatus.PENDING) {
            throw new InvalidPaymentStateException(this.id, this.status);
        }
        return List.of(new PaymentProcessed(this.id, command.gatewayRef(), Instant.now()));
    }
}
```

**Seção 5 — Projections**: event handler que constrói read model, rebuild strategy (replay de
eventos desde o início vs snapshot + replay incremental), e como lidar com eventual consistency
nos endpoints de query.

**Seção 6 — Snapshot Policy**: quando criar snapshots (a cada N eventos), como armazená-los,
como o event store usa snapshot + eventos posteriores ao carregar o aggregate.

**Seção 7 — Dead Letter e Error Handling em Projections**: o que fazer quando um projection
handler falha ao processar um evento (retry policy, dead letter queue, poison pill detection).

**Configuração de infraestrutura** gerada quando `architecture.style: cqrs`:

No `docker-compose.yml`:
```yaml
eventstore:
  image: eventstore/eventstore:23.10.0-jammy
  ports:
    - "1113:1113"
    - "2113:2113"
  environment:
    EVENTSTORE_CLUSTER_SIZE: 1
    EVENTSTORE_RUN_PROJECTIONS: All
    EVENTSTORE_INSECURE: true  # apenas dev
```

**Critérios de Aceitação (DoD)**:

- [ ] Knowledge pack gerado quando `architecture.style: cqrs`
- [ ] Contém as 7 seções especificadas
- [ ] Código de Aggregate com Event Sourcing é Java válido e compilável
- [ ] `docker-compose.yml` gerado inclui EventStoreDB quando `style: cqrs`
- [ ] Kubernetes manifest inclui StatefulSet do EventStoreDB quando `orchestrator: kubernetes`
- [ ] Golden file para profile `java-spring-cqrs` passa parity test
- [ ] Line coverage ≥ 95%, branch coverage ≥ 90%

**Gherkin**:

```gherkin
Feature: Geração de knowledge pack CQRS/ES

  Scenario: Config CQRS gera knowledge pack especializado com exemplos de aggregate
    Given um config com "architecture.style: cqrs" e "language: java"
    When o gerador executa
    Then ".claude/skills/architecture-cqrs/KNOWLEDGE.md" é gerado
    And o arquivo contém um exemplo de Aggregate com "@EventHandler" method
    And o arquivo contém a interface "EventStore" com método "append"
    And o arquivo contém seção sobre Snapshot Policy

  Scenario: docker-compose inclui EventStoreDB para CQRS
    Given um config com "architecture.style: cqrs"
    When o gerador executa
    Then "docker-compose.yml" contém serviço "eventstore"
    And a imagem do serviço é "eventstore/eventstore"
```

---

### STORY-0004: Profiles Arquiteturais Dedicados

**Título**: Novos profiles bundled: `java-spring-hexagonal`, `java-spring-cqrs-es`, `java-spring-event-driven`

**Tipo**: Feature — Config Profiles + Integration Tests

**Prioridade**: Média-Alta

**Dependências**: STORY-0002 (hexagonal), STORY-0003 (cqrs), STORY-0001 (anti-patterns)

**Contexto técnico**:
Os 10 profiles atuais definem a dimensão tecnológica (language + framework + database). Esta story
adiciona 3 profiles que definem a dimensão arquitetural sobre a base `java-spring`, resultando em
ambientes completamente diferentes para o agente: estrutura de pacotes diferente, knowledge packs
diferentes, rules adicionais, e templates de testes diferentes.

**Escopo de implementação**:

**Profile `java-spring-hexagonal`**:
```yaml
name: java-spring-hexagonal
description: Java 21 + Spring Boot 3.4 + PostgreSQL with Hexagonal Architecture and DDD
language: java
javaVersion: "21"
framework: spring-boot
frameworkVersion: "3.4"
database: postgresql
architecture:
  style: hexagonal
  validateWithArchUnit: true
  basePackage: com.example.service
testing:
  framework: junit5
  coverageThreshold:
    line: 95
    branch: 90
  archunitEnabled: true
```

Gera, adicionalmente aos artefatos do `java-spring` base:
- Knowledge pack `architecture-hexagonal`
- Anti-patterns hexagonal-specific (injeção de Spring no domain, etc.)
- `HexagonalArchitectureTest.java`
- `layer-templates` knowledge pack com estrutura hexagonal
- Rule `04-architecture-summary.md` com referência explícita ao estilo hexagonal

**Profile `java-spring-cqrs-es`**:
```yaml
name: java-spring-cqrs-es
description: Java 21 + Spring Boot 3.4 + CQRS + Event Sourcing with EventStoreDB
language: java
javaVersion: "21"
framework: spring-boot
frameworkVersion: "3.4"
database: postgresql
eventStore: eventstoredb
messageBroker: kafka
architecture:
  style: cqrs
  validateWithArchUnit: true
  basePackage: com.example.service
  snapshotPolicy:
    eventsPerSnapshot: 50
```

Gera, adicionalmente:
- Knowledge pack `architecture-cqrs`
- EventStoreDB no `docker-compose.yml` e Kubernetes StatefulSet
- Kafka no `docker-compose.yml`
- Anti-patterns CQRS-specific (write model sendo reutilizado como read model, etc.)
- Template de Aggregate com Event Sourcing
- Template de Projection handler

**Profile `java-spring-event-driven`**:
```yaml
name: java-spring-event-driven
description: Java 21 + Spring Boot 3.4 + Kafka + Event-Driven Microservice
language: java
javaVersion: "21"
framework: spring-boot
frameworkVersion: "3.4"
database: postgresql
messageBroker: kafka
schemaRegistry: confluent
architecture:
  style: event-driven
  outboxPattern: true
  deadLetterStrategy: kafka-dlq
interfaces:
  - type: rest
  - type: event
    broker: kafka
```

Gera:
- Knowledge pack `patterns-outbox`
- Configuração Kafka + Schema Registry no docker-compose
- Template de Outbox table migration
- Template de polling publisher
- Knowledge pack de dead letter queue strategy

**Novos campos no modelo de dados**:

| Campo | Tipo | Escopo |
|-------|------|--------|
| `eventStore` | `enum` (eventstoredb, axon, custom) | config root |
| `schemaRegistry` | `enum` (confluent, apicurio, glue) | config root |
| `architecture.snapshotPolicy.eventsPerSnapshot` | `integer` | architecture section |
| `architecture.outboxPattern` | `boolean` | architecture section |
| `architecture.deadLetterStrategy` | `enum` (kafka-dlq, sqs-dlq, database) | architecture section |

**Critérios de Aceitação (DoD)**:

- [ ] 3 novos profiles adicionados ao `ProfileRegistry`
- [ ] Golden files gerados para os 3 novos profiles
- [ ] Parity tests passam para os 3 novos profiles
- [ ] `ia-dev-env generate --stack java-spring-hexagonal -o ./test-out` executa sem erros
- [ ] `ia-dev-env generate --stack java-spring-cqrs-es -o ./test-out` executa sem erros
- [ ] `ia-dev-env generate --stack java-spring-event-driven -o ./test-out` executa sem erros
- [ ] `ia-dev-env validate --config java-spring-cqrs-es.yaml --verbose` valida corretamente
- [ ] Novos campos de modelo passam por schema validation no `validate` command
- [ ] Line coverage ≥ 95%, branch coverage ≥ 90%
- [ ] README.md atualizado com os 3 novos profiles na tabela de profiles bundled

**Gherkin**:

```gherkin
Feature: Novos profiles arquiteturais bundled

  Scenario: Profile java-spring-hexagonal gera ArchUnit e estrutura hexagonal
    Given o profile bundled "java-spring-hexagonal"
    When "ia-dev-env generate --stack java-spring-hexagonal -o ./out" executa
    Then a saída contém ".claude/skills/architecture-hexagonal/KNOWLEDGE.md"
    And a saída contém "src/test/java/.../HexagonalArchitectureTest.java"
    And a saída contém ".claude/rules/07-anti-patterns.md" com anti-patterns hexagonal

  Scenario: Profile cqrs-es gera EventStoreDB no docker-compose
    Given o profile bundled "java-spring-cqrs-es"
    When "ia-dev-env generate --stack java-spring-cqrs-es -o ./out" executa
    Then "docker-compose.yml" contém serviço "eventstore"
    And a saída contém ".claude/skills/architecture-cqrs/KNOWLEDGE.md"
    And a saída contém template de Aggregate com event sourcing
```

---

### STORY-0005: Knowledge Pack — DDD Estratégico (Context Map + Bounded Context)

**Título**: Knowledge pack `ddd-strategic` com context map, bounded context canvas e ACL patterns

**Tipo**: Feature — Template + Skill

**Prioridade**: Média

**Dependências**: STORY-0002 (conteúdo se sobrepõe ao hexagonal em conceitos de domain isolation)

**Contexto técnico**:
Sistemas com múltiplos serviços (como Bifrost) precisam que o agente entenda onde traçar
boundaries, como serviços se relacionam (upstream/downstream), e quando usar um Anti-Corruption
Layer. Sem esse contexto, o agente cria integrações ponto-a-ponto que violam os boundaries e
acoplam serviços que deveriam ser autônomos.

**Escopo de implementação**:

Knowledge pack `ddd-strategic` gerado quando `architecture.style: hexagonal` ou
`architecture.style: ddd` (novo valor de enum) ou quando `ddd.enabled: true` no config.

O pack DEVE conter:

**Seção 1 — Bounded Context**: definição, como identificar boundaries (one team owns one context),
exemplo de context com payload completo de responsabilidades.

**Seção 2 — Context Map com Integration Patterns**:

Tabela de patterns de integração entre contextos com quando usar cada um:

| Pattern | Quando usar | Coupling | Exemplo |
|---------|-------------|----------|---------|
| Shared Kernel | Times que colaboram fortemente, schema compartilhado | Alto | Auth tokens compartilhados |
| Customer/Supplier | Upstream define API, downstream consome | Médio | Payment notifica Settlement |
| Conformist | Downstream aceita modelo do upstream sem tradução | Alto | Integração com sistema legado |
| Anti-Corruption Layer | Downstream protege seu modelo do upstream | Baixo | Gateway para API de banco externo |
| Open Host Service | Upstream publica protocolo estável para múltiplos downstream | Baixo | API pública REST |
| Published Language | Contrato formal compartilhado (OpenAPI, Protobuf, AsyncAPI) | Baixo | Event schema no Schema Registry |

**Seção 3 — Anti-Corruption Layer Template**:

```java
// Padrão ACL: traduz o modelo externo para o modelo interno do bounded context
public class ExternalPaymentGatewayAcl implements PaymentGatewayPort {

    private final ExternalGatewayClient externalClient; // modelo externo

    @Override
    public GatewayResponse process(PaymentCommand command) {
        // Traduz do domain model para o modelo externo
        ExternalPaymentRequest request = toExternalModel(command);

        // Chama o sistema externo
        ExternalPaymentResponse response = externalClient.processPayment(request);

        // Traduz do modelo externo para o domain model — NUNCA deixa o modelo externo vazar
        return toDomainModel(response);
    }

    private ExternalPaymentRequest toExternalModel(PaymentCommand command) { ... }
    private GatewayResponse toDomainModel(ExternalPaymentResponse response) { ... }
}
```

**Seção 4 — Skill `/x-ddd-context-map`** (nova skill condicional):

Quando `ddd.enabled: true`, gerar a skill `/x-ddd-context-map` que:
- Lê o diretório do projeto e identifica bounded contexts existentes (por pacote ou por módulo)
- Gera um diagrama Mermaid de context map em `docs/architecture/context-map.md`
- Para cada par de contextos, classifica o integration pattern atual
- Identifica onde falta ACL (modelo externo vazando para o domain)

**Data Contract — nova skill**:

```
Input:  none (auto-descobre contextos no projeto)
Output: docs/architecture/context-map.md com:
        - Diagrama Mermaid
        - Tabela de integration patterns por par de contextos
        - Lista de ACLs faltantes com severidade
```

**Critérios de Aceitação (DoD)**:

- [ ] Knowledge pack `ddd-strategic` gerado quando `ddd.enabled: true`
- [ ] Pack contém as 4 seções com código compilável para ACL template
- [ ] Skill `/x-ddd-context-map` gerada condicionalmente
- [ ] Skill produz `context-map.md` com diagrama Mermaid válido
- [ ] Novo enum value `ddd` adicionado a `architecture.style`
- [ ] Line coverage ≥ 95%, branch coverage ≥ 90%

---

### STORY-0006: Checklist Condicional no `/x-review-pr`

**Título**: Seções condicionais no checklist do tech lead review (event-driven + compliance/fintech)

**Tipo**: Feature — Skill Template

**Prioridade**: Média-Alta

**Dependências**: Nenhuma (modifica template de skill existente, independente)

**Contexto técnico**:
O checklist de 45 pontos do `/x-review-pr` é estático. Para sistemas event-driven, faltam critérios
sobre versionamento de eventos, idempotência de consumers, dead letter strategy. Para sistemas
financeiros com PCI-DSS, faltam critérios sobre mascaramento de PAN, audit trail imutável,
segregação de dados de cartão. Ativar esses critérios para todos os projetos geraria falsos
positivos — precisam ser condicionais.

**Escopo de implementação**:

Adicionar ao template do `x-review-pr` skill as seguintes seções condicionais:

**Seção L — Event-Driven Review** (gerada quando `interfaces` contém `type: event`):

| # | Critério | Peso |
|---|----------|------|
| L1 | Eventos têm versão explícita no nome ou no campo `version` do payload | 2 |
| L2 | Consumer é idempotente — processar o mesmo evento duas vezes não causa efeito colateral | 3 |
| L3 | Dead letter queue configurada para o consumer | 2 |
| L4 | Schema do evento registrado no Schema Registry antes do merge | 2 |
| L5 | Outbox pattern implementado para garantia de entrega (se aplicável) | 2 |
| L6 | Consumer não assume ordem de chegada de eventos de streams diferentes | 1 |
| L7 | Evento não contém dados sensíveis não mascarados | 2 |
| L8 | Teste de consumer com evento de versão anterior (backward compatibility) | 2 |

**Seção M — Compliance / PCI-DSS** (gerada quando `compliance` contém `pci-dss`):

| # | Critério | Peso |
|---|----------|------|
| M1 | PAN (Primary Account Number) nunca é logado, mesmo em debug | 3 |
| M2 | PAN é mascarado no payload de resposta (apenas primeiros 6 e últimos 4 dígitos visíveis) | 3 |
| M3 | CVV nunca é persistido, nem mesmo temporariamente | 3 |
| M4 | Dados de cartão em trânsito apenas via TLS 1.2+ | 2 |
| M5 | Audit trail de acesso a dados de cartão é imutável (append-only) | 2 |
| M6 | Chaves de criptografia não estão hardcoded nem em variáveis de ambiente sem rotação | 3 |
| M7 | Acesso a dados de cartão requer autorização explícita (não apenas autenticação) | 2 |

**Seção N — Compliance / LGPD** (gerada quando `compliance` contém `lgpd`):

| # | Critério | Peso |
|---|----------|------|
| N1 | Dados pessoais identificados e documentados no ADR de privacidade | 2 |
| N2 | Retention policy definida para cada tipo de dado pessoal | 2 |
| N3 | Endpoint de exclusão de dados implementado (direito ao esquecimento) | 2 |
| N4 | Dados pessoais não são repassados a terceiros sem consentimento documentado | 2 |

**Atualização do scoring**: O score máximo do checklist aumenta conforme as seções condicionais
ativas. O threshold de GO deve ser recalculado como percentual (≥ 84% do máximo possível), não
como número absoluto, para manter consistência com projetos que têm seções adicionais.

**Novos campos de config**:

```yaml
compliance:
  - pci-dss
  - lgpd
interfaces:
  - type: event
    broker: kafka
```

| Campo | Tipo | Valores |
|-------|------|---------|
| `compliance` | `list<enum>` | `pci-dss`, `lgpd`, `sox`, `hipaa` |

**Critérios de Aceitação (DoD)**:

- [ ] Template `x-review-pr` SKILL.md aceita seções condicionais via Pebble template logic
- [ ] Seção L gerada quando config tem `interfaces[type=event]`
- [ ] Seção M gerada quando config tem `compliance` contendo `pci-dss`
- [ ] Seção N gerada quando config tem `compliance` contendo `lgpd`
- [ ] Scoring recalculado como percentual quando há seções condicionais
- [ ] Projeto sem compliance e sem event-driven gera checklist idêntico ao atual (regressão zero)
- [ ] Golden files para perfis com compliance atualizados
- [ ] Line coverage ≥ 95%, branch coverage ≥ 90%

**Gherkin**:

```gherkin
Feature: Checklist condicional de review por tipo de projeto

  Scenario: Projeto sem compliance e sem event gera checklist de 45 pontos
    Given um config sem "compliance" e sem interfaces "event"
    When o gerador executa
    Then a skill "x-review-pr/SKILL.md" contém exatamente 45 critérios de checklist
    And a skill não contém a seção "L — Event-Driven Review"

  Scenario: Projeto com pci-dss gera seção M no checklist
    Given um config com "compliance: [pci-dss]"
    When o gerador executa
    Then a skill "x-review-pr/SKILL.md" contém a seção "M — Compliance / PCI-DSS"
    And o checklist contém o critério "M1" sobre logging de PAN

  Scenario: Score GO recalculado como percentual com seções adicionais
    Given um config com "compliance: [pci-dss]" e interfaces "event"
    When o gerador executa
    Then a skill "x-review-pr/SKILL.md" define threshold como "≥ 84% do total de pontos"
    And não define threshold como número absoluto fixo
```

---

### STORY-0007: API-First Phase no `/x-dev-lifecycle`

**Título**: Phase 0.5 no lifecycle — geração de contrato formal (OpenAPI/AsyncAPI/Protobuf) antes da implementação

**Tipo**: Feature — Skill Template

**Prioridade**: Média

**Dependências**: STORY-0004 (profiles event-driven usam AsyncAPI)

**Contexto técnico**:
O lifecycle atual vai de architecture plan direto para implementação. Para sistemas com múltiplos
consumers (REST APIs públicas, eventos consumidos por outros serviços, gRPC entre microserviços),
o agente que implementa sem um contrato formal tende a criar interfaces que divergem do que foi
planejado — especialmente em nomes de campos, tipos de dados e error codes. A Phase 0.5 força a
criação do contrato como artefato revisável antes que qualquer código de implementação seja escrito.

**Escopo de implementação**:

Adicionar ao template do skill `x-dev-lifecycle` uma Phase 0.5 condicional, inserida entre
Phase 0 (Preparation) e Phase 1 (Architecture Planning):

```
Phase 0.5 — Contract First (condicional: ativada quando story declara interfaces REST, gRPC ou event)

  0.5.1 — Identificar tipo de interface declarado na story (REST | gRPC | Event | WebSocket)
  0.5.2 — Gerar rascunho do contrato no formato adequado:
           REST  → docs/contracts/{story-id}-openapi.yaml (OpenAPI 3.1)
           gRPC  → src/main/proto/{domain}/{version}/{service}.proto
           Event → docs/contracts/{story-id}-asyncapi.yaml (AsyncAPI 2.6)
  0.5.3 — Validar contrato gerado (openapi-cli lint, buf lint, asyncapi validate)
  0.5.4 — Aguardar aprovação explícita antes de prosseguir para Phase 1
           (output: "CONTRACT PENDING APPROVAL — review docs/contracts/{story-id}-openapi.yaml")
```

**Template de contrato REST gerado na Phase 0.5** (a partir dos data contracts da story):

```yaml
# docs/contracts/{story-id}-openapi.yaml
openapi: "3.1.0"
info:
  title: "{ServiceName} API"
  version: "{story-id}-draft"
  description: "Draft contract for {story-title}. Subject to change before approval."
paths:
  /endpoint:
    post:
      summary: "{operation summary from story}"
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/{RequestName}'
      responses:
        '200':
          description: "{success description}"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/{ResponseName}'
        '400':
          $ref: '#/components/responses/ValidationError'
        '422':
          $ref: '#/components/responses/BusinessRuleViolation'
        '500':
          $ref: '#/components/responses/InternalError'
components:
  schemas:
    {RequestName}:
      # Gerado a partir do data contract da story (campos M/O com tipos)
    {ResponseName}:
      # Gerado a partir do data contract da story
  responses:
    ValidationError:
      description: "RFC 7807 Problem Details for input validation errors"
      content:
        application/problem+json:
          schema:
            $ref: '#/components/schemas/ProblemDetails'
```

**Skill `/x-contract-lint`** (nova skill condicional gerada quando `interfaces` declarados):

```
Input:  path do contrato (openapi.yaml | .proto | asyncapi.yaml)
Output: relatório de lint com: erros, warnings, sugestões de melhoria
        e flag GO/NO-GO para prosseguir para implementação
```

**Critérios de Aceitação (DoD)**:

- [ ] Template `x-dev-lifecycle` SKILL.md contém Phase 0.5 quando story tem interfaces declaradas
- [ ] Phase 0.5 gera arquivo OpenAPI 3.1 válido para stories REST
- [ ] Phase 0.5 gera arquivo AsyncAPI 2.6 válido para stories event-driven
- [ ] Phase 0.5 emite mensagem de pausa aguardando aprovação explícita do contrato
- [ ] Skill `/x-contract-lint` gerada condicionalmente quando `interfaces` configurados
- [ ] Projects sem interfaces declaradas não têm Phase 0.5 no lifecycle (regressão zero)
- [ ] Line coverage ≥ 95%, branch coverage ≥ 90%

**Gherkin**:

```gherkin
Feature: API-First phase no lifecycle

  Scenario: Story com interface REST ativa Phase 0.5
    Given uma story com data contracts de request/response declarados
    And um config com "interfaces: [{type: rest}]"
    When "/x-dev-lifecycle STORY-ID" é executado
    Then Phase 0.5 é executada antes de Phase 1
    And o arquivo "docs/contracts/STORY-ID-openapi.yaml" é gerado
    And o lifecycle para aguardando aprovação com mensagem "CONTRACT PENDING APPROVAL"

  Scenario: Story sem interfaces pula Phase 0.5
    Given uma story sem data contracts de API
    And um config sem "interfaces" declarados
    When "/x-dev-lifecycle STORY-ID" é executado
    Then Phase 0.5 não é executada
    And o lifecycle vai direto de Phase 0 para Phase 1
```

---

### STORY-0008: Data Contracts Ricos nas Stories Geradas

**Título**: `/x-story-create` gera schemas completos com tipos, validações e error codes

**Tipo**: Feature — Skill Template

**Prioridade**: Média-Alta

**Dependências**: STORY-0007 (API-first usa os contratos gerados pelas stories)

**Contexto técnico**:
As stories geradas atualmente têm data contracts com notação M/O (mandatory/optional) e nomes de
campos. Sem tipos explícitos, regras de validação e error codes mapeados, o agente que implementa
tem liberdade demais — vai inventar tipos (String vs UUID vs Long), inventar validações (sem saber
se email deve ser validado por regex ou por formato RFC) e inventar error codes. Isso gera código
que não bate com outros serviços que consomem a mesma API.

**Escopo de implementação**:

Atualizar o template de story para que a seção "Data Contracts" gere a seguinte estrutura:

```markdown
## Data Contracts

### Request — {OperationName}Command / {OperationName}Request

| Campo | Tipo | M/O | Validações | Exemplo |
|-------|------|-----|-----------|---------|
| `paymentId` | `UUID` | M | formato UUID v4 | `550e8400-e29b-41d4-a716-446655440000` |
| `amount` | `BigDecimal` | M | > 0, escala máx 2 casas | `150.00` |
| `currency` | `String(3)` | M | ISO 4217, uppercase | `BRL` |
| `cardToken` | `String(64)` | M | alphanum, exatamente 64 chars | `tok_abc123...` |
| `merchantId` | `UUID` | M | referência válida em Merchant context | `...` |
| `description` | `String(255)` | O | máx 255 chars | `Pagamento pedido #123` |

### Response — {OperationName}Result

| Campo | Tipo | Sempre presente | Descrição |
|-------|------|-----------------|-----------|
| `transactionId` | `UUID` | Sim | ID único da transação gerada |
| `status` | `enum(APPROVED, DECLINED, PENDING)` | Sim | Status resultante |
| `authCode` | `String(6)` | Apenas se APPROVED | Código de autorização da rede |
| `declineReason` | `enum(INSUFFICIENT_FUNDS, INVALID_CARD, ...)` | Apenas se DECLINED | Motivo padronizado |
| `processingTime` | `Duration (ISO 8601)` | Sim | Tempo de processamento |

### Error Codes Mapeados

| HTTP Status | Error Code | Condição | Mensagem padrão (RFC 7807) |
|-------------|-----------|----------|--------------------------|
| 400 | `INVALID_AMOUNT` | amount ≤ 0 ou mais de 2 casas decimais | "Amount must be positive with max 2 decimal places" |
| 400 | `INVALID_CURRENCY` | currency não é ISO 4217 válido | "Currency must be a valid ISO 4217 code" |
| 422 | `CARD_TOKEN_EXPIRED` | token expirado no vault | "Card token has expired, please re-tokenize" |
| 422 | `INSUFFICIENT_FUNDS` | saldo insuficiente retornado pela rede | "Transaction declined: insufficient funds" |
| 422 | `MERCHANT_NOT_FOUND` | merchantId não existe | "Merchant {merchantId} not found or inactive" |
| 500 | `GATEWAY_TIMEOUT` | timeout na comunicação com a rede de cartões | "Payment gateway timeout, retry is safe" |
| 500 | `GATEWAY_ERROR` | erro não classificado do gateway | "Payment gateway returned unexpected error" |
```

Adicionalmente, para stories com interfaces de evento:

```markdown
### Event Schema — {EventName}

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `eventId` | `UUID` | ID único do evento (para idempotência) |
| `eventType` | `String` | Literal: `payment.processed.v1` |
| `eventVersion` | `String` | Literal: `1.0` |
| `occurredAt` | `Instant (ISO 8601)` | Timestamp do evento no domínio |
| `aggregateId` | `UUID` | ID do aggregate que originou o evento |
| `payload` | `object` | Ver sub-campos abaixo |
| `payload.transactionId` | `UUID` | ... |

**Notas de versionamento**: Este evento usa semântica de versionamento no campo `eventType`
(sufixo `.v1`). Mudanças backward-compatible incrementam o minor; mudanças breaking criam
novo tipo `.v2` com período de coexistência de 30 dias.
```

**Critérios de Aceitação (DoD)**:

- [ ] Template de story gerado pelo `x-story-create` inclui tabela de Data Contract com colunas Tipo/Validações/Exemplo
- [ ] Template inclui tabela de Error Codes Mapeados com HTTP status, error code, condição e mensagem
- [ ] Para stories com `eventDriven: true`, template inclui seção de Event Schema com versionamento
- [ ] Conhecimento pack `story-planning` atualizado com instruções sobre como preencher os novos campos
- [ ] Golden files de stories atualizados para refletir novo formato
- [ ] Line coverage ≥ 95%, branch coverage ≥ 90%

---

### STORY-0009: Knowledge Pack `patterns-outbox` com Transactional Outbox Pattern

**Título**: Knowledge pack dedicado ao Transactional Outbox Pattern com polling publisher e CDC

**Tipo**: Feature — Template

**Prioridade**: Média

**Dependências**: STORY-0003 (contextualmente relacionado a event-driven), STORY-0004 (profile
event-driven usa este pack)

**Contexto técnico**:
O Outbox pattern é crítico para garantia de entrega em sistemas event-driven sem 2PC. É também um
dos patterns mais mal implementados — agentes frequentemente implementam "publish diretamente no
Kafka dentro da transação" sem entender que isso viola atomicidade. O knowledge pack deve dar o
contexto completo de por que o padrão existe e como implementá-lo corretamente.

**Escopo de implementação**:

O pack `patterns-outbox` DEVE conter:

**Seção 1 — O Problema**: explicação de por que `saveEntity()` + `publishEvent()` na mesma
transação de banco NÃO garante consistência (falha entre os dois passos), com diagrama de
sequência mostrando o cenário de falha.

**Seção 2 — Solução: Transactional Outbox**: diagrama e código de inserção na tabela `outbox`
dentro da mesma transação do negócio.

Schema da tabela:
```sql
CREATE TABLE outbox_events (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic       VARCHAR(255) NOT NULL,
    partition_key VARCHAR(255),
    payload     JSONB NOT NULL,
    headers     JSONB,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP WITH TIME ZONE,
    retry_count INTEGER NOT NULL DEFAULT 0,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED', 'DEAD_LETTER'))
);
CREATE INDEX idx_outbox_status_created ON outbox_events(status, created_at)
    WHERE status IN ('PENDING', 'FAILED');
```

**Seção 3 — Polling Publisher**: implementação do scheduler que lê a tabela e publica no broker,
com locking otimista para múltiplas instâncias.

**Seção 4 — CDC com Debezium**: configuração do Debezium connector para capturar WAL do
PostgreSQL, como alternativa ao polling publisher. Quando preferir CDC vs polling.

**Seção 5 — Anti-patterns do Outbox**:
- Publicar no Kafka dentro da transação de negócio (o problema original)
- Outbox sem índice na coluna `status` (full table scan em produção)
- Polling muito frequente sem backoff exponencial (thundering herd)
- Não definir política de dead letter para eventos não processáveis

**Critérios de Aceitação (DoD)**:

- [ ] Knowledge pack gerado quando `architecture.outboxPattern: true`
- [ ] Pack contém as 5 seções com SQL e código Java compilável
- [ ] Migration SQL da tabela `outbox_events` incluída no pack
- [ ] Golden file para profile `java-spring-event-driven` inclui o pack
- [ ] Line coverage ≥ 95%, branch coverage ≥ 90%

---

### STORY-0010: Modo Interativo com Seleção de Estilo Arquitetural

**Título**: `--interactive` mode inclui pergunta sobre `architecture.style` e `compliance`

**Tipo**: Feature — CLI Enhancement

**Prioridade**: Baixa-Média

**Dependências**: STORY-0002, STORY-0003, STORY-0006 (os campos precisam existir antes)

**Contexto técnico**:
O modo interativo atual coleta language, framework, database, etc., mas não coleta `architecture.style`
nem `compliance`. Um usuário que usa `--interactive` termina com um projeto `layered` por padrão,
sem saber que poderia ter gerado um ambiente hexagonal com ArchUnit. Esta story adiciona as
perguntas faltantes ao wizard interativo.

**Escopo de implementação**:

Adicionar ao `InteractiveConfigBuilder` as seguintes perguntas, na ordem correta após a seleção
de framework:

```
? Architecture style:
  ❯ layered (default — MVC-style, controller/service/repository)
    hexagonal (ports and adapters, ArchUnit validation)
    cqrs (CQRS + Event Sourcing, EventStoreDB)
    event-driven (async-first, Kafka, Outbox pattern)
    clean (Clean Architecture, Uncle Bob style)

? Validate architecture boundaries with ArchUnit? (y/N)
  [visible only if hexagonal or clean selected]

? Compliance requirements (space to select, enter to confirm):
  ◯ none
  ◯ pci-dss (payment card data)
  ◯ lgpd (Brazilian data protection)
  ◯ sox (financial reporting)
  ◯ hipaa (health data)
```

A pergunta de `architectureStyle` DEVE aparecer somente quando `language: java` ou
`language: kotlin` (os únicos que têm profiles arquiteturais implementados neste épico).

**Critérios de Aceitação (DoD)**:

- [ ] `InteractiveConfigBuilder` inclui pergunta de `architectureStyle` para java/kotlin
- [ ] `InteractiveConfigBuilder` inclui pergunta de `compliance` (multi-select)
- [ ] Respostas são mapeadas corretamente para o modelo `ArchitectureConfig`
- [ ] `--interactive` com `language: go` não apresenta pergunta de `architectureStyle`
- [ ] Tests unitários para os novos steps do wizard
- [ ] Line coverage ≥ 95%, branch coverage ≥ 90%

---

## Mapa de Implementação

### Fases sugeridas

```
Fase 1 — Fundação Anti-Alucinação (paralelo possível)
  STORY-0001: Anti-Patterns Rules              [sem dependências, 3-5 dias]
  STORY-0008: Data Contracts Ricos nas Stories [sem dependências, 2-3 dias]

Fase 2 — Knowledge Packs Arquiteturais (sequencial dentro da fase)
  STORY-0002: KP Hexagonal + ArchUnit          [depende: STORY-0001, 4-6 dias]
  STORY-0003: KP CQRS + Event Sourcing         [depende: STORY-0002 mecanismo, 4-6 dias]
  STORY-0009: KP Outbox Pattern                [depende: STORY-0003 context, 2-3 dias]

Fase 3 — Profiles e Review (paralelo possível dentro da fase)
  STORY-0004: Profiles Arquiteturais           [depende: STORY-0002, STORY-0003, 3-4 dias]
  STORY-0006: Checklist Condicional Review     [sem dependências críticas, 2-3 dias]

Fase 4 — Lifecycle e DDD (sequencial)
  STORY-0005: KP DDD Estratégico               [depende: STORY-0002, 3-4 dias]
  STORY-0007: API-First Phase no Lifecycle     [depende: STORY-0004, 3-4 dias]

Fase 5 — Polish (após todas as fases)
  STORY-0010: Interactive Mode                 [depende: STORY-0002+0003+0006, 1-2 dias]
```

### Critical path

`STORY-0001 → STORY-0002 → STORY-0003 → STORY-0004 → STORY-0007`

### Bottleneck

`STORY-0002` é o gargalo da Fase 2 — o mecanismo de `architecture.style` condicional que ela
introduz é prerequisito para STORY-0003 e STORY-0004.

---

## Definition of Ready (DoR) Global

- [ ] Story tem data contracts com tipos explícitos (não apenas M/O)
- [ ] Story tem ao menos 3 cenários Gherkin cobrindo happy path e error cases
- [ ] Dependências da story estão completas ou tem mock/stub acordado
- [ ] Template Pebble existente identificado como base para modificação (quando aplicável)
- [ ] Golden files alvo identificados para parity tests

## Definition of Done (DoD) Global

- [ ] Line coverage ≥ 95% (JaCoCo)
- [ ] Branch coverage ≥ 90% (JaCoCo)
- [ ] `mvn verify -Pall-tests` green
- [ ] Golden file parity tests passam para todos os profiles afetados
- [ ] `ia-dev-env validate` aceita configs com os novos campos sem erros
- [ ] `ia-dev-env generate --dry-run` preview correto para os novos artefatos
- [ ] README.md atualizado se novos profiles ou skills foram adicionados
- [ ] CHANGELOG.md atualizado com entrada na seção `Added` ou `Changed`
- [ ] Zero regressões em profiles existentes (golden files de profiles não modificados permanecem byte-for-byte idênticos)
