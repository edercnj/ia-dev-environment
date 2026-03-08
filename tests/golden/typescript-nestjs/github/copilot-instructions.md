# Project Identity — my-nestjs-service

## Identity

- **Name:** my-nestjs-service
- **Architecture Style:** microservice
- **Domain-Driven Design:** true
- **Event-Driven:** true
- **Interfaces:** REST, graphql, websocket, event-consumer, event-producer
- **Language:** typescript 5
- **Framework:** nestjs

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Architecture | Microservice |
| Language | Typescript 5 |
| Framework | Nestjs |
| Build Tool | Npm |
| Container | Docker |
| Orchestrator | Kubernetes |
| Resilience | Mandatory (always enabled) |
| Native Build | false |
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
