# Architecture Summary

> Full reference: `.claude/skills/architecture/SKILL.md`
> (generated alongside this file) for detailed patterns, variants,
> and thread-safety rules.
> Code templates per layer: `.claude/skills/layer-templates/SKILL.md`.

## Architecture Style: Hexagonal (Ports and Adapters)

## Dependency Direction

```text
adapter.inbound → application → domain ← adapter.outbound
                                  ↑
                           (ports/interfaces)
```

**Golden Rule:** Dependencies point inward toward the domain. Domain NEVER imports
adapter or framework code.

## Package Structure

```text
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
|-------|--------------|--------------------|
| domain | Standard library only | adapter, application, framework |
| application | domain.* | adapter.*, framework |
| adapter.inbound | application, domain.port | adapter.outbound |
| adapter.outbound | domain.port, domain.model | adapter.inbound |

## Implementation Order

Inner layers first, outer layers last:
domain → ports → adapters → application → inbound → tests.
