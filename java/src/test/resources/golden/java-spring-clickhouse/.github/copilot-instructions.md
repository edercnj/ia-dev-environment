# Project Identity — my-spring-clickhouse

## Identity

- **Name:** my-spring-clickhouse
- **Architecture Style:** microservice
- **Domain-Driven Design:** true
- **Event-Driven:** false
- **Interfaces:** REST
- **Language:** java 21
- **Framework:** spring-boot 3.x

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Architecture | Microservice |
| Language | Java 21 |
| Framework | Spring-boot 3.x |
| Build Tool | Gradle |
| Container | Docker |
| Orchestrator | None |
| Resilience | Mandatory (always enabled) |
| Native Build | false |
| Smoke Tests | true |
| Contract Tests | false |

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
