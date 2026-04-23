# Rule 04 — Architecture Summary

> **Full reference:** Read `knowledge/architecture.md` before designing or implementing features.

## Architecture Style: {{ARCHITECTURE}} ({{ARCH_STYLE}})

## Dependency Direction

```
adapter.inbound → application → domain ← adapter.outbound
                                  ↑
                           (ports/interfaces)
```

**Golden Rule:** Dependencies point inward toward the domain. Domain NEVER imports adapter or framework code.

## Package Structure

```
{root}/
├── domain/          # Core — zero external dependencies
│   ├── model/       # Entities, value objects, enums
│   ├── engine/      # Business logic, decision rules
│   └── port/        # Interfaces (inbound + outbound)
├── application/     # Orchestration (use cases)
├── adapter/
│   ├── inbound/     # REST, gRPC, TCP, WebSocket handlers + DTOs + mappers
│   └── outbound/    # Database, external clients + entities + mappers
└── config/          # Framework configuration
```

## Layer Rules

| Layer | Can depend on | Cannot depend on |
|-------|--------------|-----------------|
| domain | Standard library only | adapter, application, framework |
| application | domain.* | adapter.*, framework |
| adapter.inbound | application, domain.port | adapter.outbound |
| adapter.outbound | domain.port, domain.model | adapter.inbound |

## Domain Purity (Non-Negotiable)

Domain layer MUST have **zero** external library imports. Allowed:
- Standard library only
- Project's own `domain.*` packages

Forbidden in domain:
- Serialization libraries (Jackson, Gson, serde, encoding/json)
- I/O operations (filesystem, network, database)
- Framework annotations or types

If domain needs I/O or serialization → define a **port interface**, implement in adapter.

## Inbound Adapter Rules

Inbound adapters (CLI, REST, gRPC) MUST call application-layer use cases. Direct orchestration of domain services or assemblers from the adapter is forbidden.

## Implementation Order

Inner layers first, outer layers last: domain → ports → adapters → application → inbound → tests.

> Read `knowledge/layer-templates.md` for code templates per layer and `knowledge/architecture.md` for full patterns.

## Deviations

Any deviation from this package structure MUST be documented as an ADR with rationale. Undocumented deviations are treated as violations.
