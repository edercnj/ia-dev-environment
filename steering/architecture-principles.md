# Architecture Principles — ia-dev-environment

> Derived from: .claude/rules/04-architecture-summary.md

## Architecture Style

Library architecture — the project is a CLI tool, not a web service.

## Dependency Direction

```
adapter.inbound -> application -> domain <- adapter.outbound
                                   ^
                            (ports/interfaces)
```

**Golden Rule:** Dependencies point inward toward the domain. Domain NEVER imports adapter or framework code.

## Package Structure

```
dev/iadev/
|-- domain/          # Core -- zero external dependencies
|   |-- model/       # Entities, value objects, enums
|   |-- engine/      # Business logic, decision rules
|   +-- port/        # Interfaces (inbound + outbound)
|-- application/     # Orchestration (use cases)
|-- adapter/
|   |-- inbound/     # CLI handlers + DTOs + mappers
|   +-- outbound/    # File system, template engine + entities + mappers
+-- config/          # Framework configuration
```

## Layer Rules

| Layer | Can depend on | Cannot depend on |
|-------|--------------|-----------------|
| domain | Standard library only | adapter, application, framework |
| application | domain.* | adapter.*, framework |
| adapter.inbound | application, domain.port | adapter.outbound |
| adapter.outbound | domain.port, domain.model | adapter.inbound |

## Domain Purity (Non-Negotiable)

Domain layer MUST have zero external library imports. Allowed: standard library only and project's own `domain.*` packages.

Forbidden in domain:
- Serialization libraries (Jackson, Gson)
- I/O operations (filesystem, network, database)
- Framework annotations or types

If domain needs I/O or serialization, define a port interface and implement in adapter.

## Implementation Order

Inner layers first, outer layers last: domain -> ports -> adapters -> application -> inbound -> tests.

## Deviations

Any deviation from this package structure MUST be documented as an ADR with rationale. Undocumented deviations are treated as violations.
