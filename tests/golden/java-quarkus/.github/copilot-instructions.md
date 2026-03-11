# Project Identity — my-quarkus-service

## Identity

- **Name:** my-quarkus-service
- **Architecture Style:** microservice
- **Domain-Driven Design:** true
- **Event-Driven:** true
- **Interfaces:** REST, GRPC, event-consumer, event-producer
- **Language:** java 21
- **Framework:** quarkus 3.17

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Architecture | Microservice |
| Language | Java 21 |
| Framework | Quarkus 3.17 |
| Build Tool | Maven |
| Container | Docker |
| Orchestrator | Kubernetes |
| Resilience | Mandatory (always enabled) |
| Native Build | true |
| Smoke Tests | true |
| Contract Tests | true |

## Constraints

- Cloud-Agnostic: ZERO dependencies on cloud-specific services
- Horizontal scalability: Application must be stateless
- Externalized configuration: All configuration via environment variables or ConfigMaps

## Contextual Instructions

The following instruction files provide domain-specific context:

- `instructions/domain.instructions.md` — Domain model, business rules, sensitive data
- `instructions/coding-standards.instructions.md` — Clean Code, SOLID, naming, error handling
- `instructions/architecture.instructions.md` — Hexagonal architecture, layer rules, package structure
- `instructions/quality-gates.instructions.md` — Coverage thresholds, test categories, merge checklist

For deep-dive references, see the knowledge packs in `.claude/skills/` (generated alongside this structure).
