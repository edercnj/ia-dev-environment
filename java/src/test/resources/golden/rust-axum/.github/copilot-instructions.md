# Project Identity — my-rust-service

## Identity

- **Name:** my-rust-service
- **Architecture Style:** microservice
- **Domain-Driven Design:** false
- **Event-Driven:** true
- **Interfaces:** REST, GRPC, event-consumer, event-producer
- **Language:** rust 2024
- **Framework:** axum

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Architecture | Microservice |
| Language | Rust 2024 |
| Framework | Axum |
| Build Tool | Cargo |
| Container | Docker |
| Orchestrator | Kubernetes |
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
