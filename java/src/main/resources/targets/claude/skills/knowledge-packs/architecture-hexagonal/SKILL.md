---
name: architecture-hexagonal
description: "Hexagonal architecture reference: canonical package structure, dependency rules with violation examples, compilable Port/Adapter patterns, and ArchUnit boundary validation suite. Read before implementing hexagonal-style projects."
user-invocable: false
allowed-tools:
  - Read
  - Grep
  - Glob
---

# Knowledge Pack: Hexagonal Architecture

## Purpose

Provides specialized hexagonal architecture guidance for projects with `architecture.style: hexagonal`. Includes canonical package structure, dependency rules with violation examples, compilable Port/Adapter code patterns, and an ArchUnit test suite for CI boundary validation.

> **Base package:** `{BASE_PACKAGE}`

---

## Section 1 -- Canonical Package Structure

```
{BASE_PACKAGE}/
+-- domain/
|   +-- model/           # Entities, value objects, enums, aggregates
|   +-- port/
|   |   +-- inbound/     # Use-case interfaces (driving ports)
|   |   +-- outbound/    # SPI interfaces (driven ports)
|   +-- engine/          # Pure business logic, decision rules
+-- application/
|   +-- usecase/         # Use-case implementations (orchestrators)
+-- adapter/
|   +-- inbound/
|   |   +-- rest/        # REST controllers + DTOs + mappers
|   |   +-- grpc/        # gRPC handlers + proto mappers
|   |   +-- cli/         # CLI commands
|   +-- outbound/
|       +-- persistence/ # JPA/JDBC entities + repositories + mappers
|       +-- client/      # External HTTP/gRPC clients
|       +-- messaging/   # Event publishers
+-- config/              # Framework configuration, DI wiring
```

### Layer Responsibilities

| Layer | Responsibility | Allowed Dependencies |
|-------|---------------|---------------------|
| `domain.model` | Entities, value objects, aggregates | Standard library only |
| `domain.port.inbound` | Driving port interfaces | `domain.model` |
| `domain.port.outbound` | Driven port interfaces | `domain.model` |
| `domain.engine` | Pure business logic | `domain.model`, `domain.port` |
| `application.usecase` | Orchestration, transaction boundaries | `domain.*` |
| `adapter.inbound.*` | Protocol translation (HTTP/gRPC/CLI to domain) | `application`, `domain.port`, `domain.model` |
| `adapter.outbound.*` | Infrastructure translation (domain to DB/MQ) | `domain.port`, `domain.model` |
| `config` | Framework wiring, bean definitions | All layers (assembly root) |

---

## Section 2 -- Dependency Rules with Violation Examples

### Rule Matrix

| Source | Can Depend On | CANNOT Depend On |
|--------|--------------|-----------------|
| `domain.*` | Standard library | `adapter.*`, `application.*`, `config.*`, any framework |
| `application.*` | `domain.*` | `adapter.*`, `config.*` |
| `adapter.inbound.*` | `application.*`, `domain.port.*`, `domain.model.*` | `adapter.outbound.*` |
| `adapter.outbound.*` | `domain.port.*`, `domain.model.*` | `adapter.inbound.*`, `application.*` |

### Violation Examples

**Violation 1 -- Domain imports adapter:**

```java
// FORBIDDEN: domain depends on adapter
package {BASE_PACKAGE}.domain.engine;

import {BASE_PACKAGE}.adapter.outbound.persistence.UserEntity; // VIOLATION
```

**Expected error:** `ArchUnit: domain classes should not depend on adapter classes`

**Violation 2 -- Domain imports framework:**

```java
// FORBIDDEN: domain depends on framework
package {BASE_PACKAGE}.domain.model;

import org.springframework.data.annotation.Id; // VIOLATION
import jakarta.persistence.Entity;              // VIOLATION
```

**Expected error:** `ArchUnit: domain classes should not depend on framework classes`

**Violation 3 -- Inbound adapter imports outbound adapter:**

```java
// FORBIDDEN: inbound adapter depends on outbound adapter
package {BASE_PACKAGE}.adapter.inbound.rest;

import {BASE_PACKAGE}.adapter.outbound.persistence.UserRepository; // VIOLATION
```

**Expected error:** `ArchUnit: inbound adapter should not depend on outbound adapter`

---

## Section 3 -- Compilable Port/Adapter Examples

### Inbound Port (Driving Port)

```java
package {BASE_PACKAGE}.domain.port.inbound;

import {BASE_PACKAGE}.domain.model.Order;
import java.util.Optional;

/**
 * Driving port for order management use cases.
 */
public interface ManageOrderUseCase {

    Order createOrder(String customerId, String productId, int quantity);

    Optional<Order> findOrder(String orderId);
}
```

### Outbound Port (Driven Port)

```java
package {BASE_PACKAGE}.domain.port.outbound;

import {BASE_PACKAGE}.domain.model.Order;
import java.util.Optional;

/**
 * Driven port for order persistence.
 */
public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(String orderId);
}
```

### Use Case (Application Layer)

```java
package {BASE_PACKAGE}.application.usecase;

import {BASE_PACKAGE}.domain.model.Order;
import {BASE_PACKAGE}.domain.port.inbound.ManageOrderUseCase;
import {BASE_PACKAGE}.domain.port.outbound.OrderRepository;
import java.util.Objects;
import java.util.Optional;

/**
 * Orchestrates order management by coordinating
 * domain logic and outbound ports.
 */
public final class ManageOrderService
        implements ManageOrderUseCase {

    private final OrderRepository orderRepository;

    public ManageOrderService(
            OrderRepository orderRepository) {
        this.orderRepository = Objects.requireNonNull(
                orderRepository,
                "orderRepository must not be null");
    }

    @Override
    public Order createOrder(
            String customerId,
            String productId,
            int quantity) {
        var order = Order.create(
                customerId, productId, quantity);
        return orderRepository.save(order);
    }

    @Override
    public Optional<Order> findOrder(String orderId) {
        return orderRepository.findById(orderId);
    }
}
```

### Outbound Adapter (Persistence)

```java
package {BASE_PACKAGE}.adapter.outbound.persistence;

import {BASE_PACKAGE}.domain.model.Order;
import {BASE_PACKAGE}.domain.port.outbound.OrderRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of OrderRepository
 * for testing and prototyping.
 */
public final class InMemoryOrderRepository
        implements OrderRepository {

    private final Map<String, Order> store =
            new ConcurrentHashMap<>();

    @Override
    public Order save(Order order) {
        store.put(order.id(), order);
        return order;
    }

    @Override
    public Optional<Order> findById(String orderId) {
        return Optional.ofNullable(store.get(orderId));
    }
}
```

---

## Section 4 -- ArchUnit Boundary Validation Suite

The following ArchUnit test class validates hexagonal boundaries in CI. Generate this class when `architecture.validateWithArchUnit: true`.

```java
package {BASE_PACKAGE};

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
        packages = "{BASE_PACKAGE}",
        importOptions = ImportOption.DoNotIncludeTests.class)
public class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule domain_should_not_depend_on_adapter =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..adapter..");

    @ArchTest
    static final ArchRule domain_should_not_depend_on_framework =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "jakarta..",
                            "javax.persistence..",
                            "io.quarkus..");

    @ArchTest
    static final ArchRule inbound_should_not_depend_on_outbound =
            noClasses()
                    .that().resideInAPackage("..adapter.inbound..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..adapter.outbound..");
}
```

### ArchUnit Maven Dependency

When `architecture.validateWithArchUnit: true`, add to `pom.xml`:

```xml
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <version>1.3.0</version>
    <scope>test</scope>
</dependency>
```

---

## Related Knowledge Packs

- `skills/architecture/` -- Generic architecture principles
- `skills/layer-templates/` -- Code templates per architecture layer
- `skills/coding-standards/` -- Language-specific coding conventions
