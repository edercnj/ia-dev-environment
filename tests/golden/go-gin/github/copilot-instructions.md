# Project Identity — my-go-service

## Identity

- **Name:** my-go-service
- **Architecture Style:** microservice
- **Domain-Driven Design:** false
- **Event-Driven:** true
- **Interfaces:** REST, GRPC, event-consumer, event-producer
- **Language:** Go 1.22
- **Framework:** Gin

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Architecture | Microservice |
| Language | Go 1.22 |
| Framework | Gin |
| Build Tool | Go-mod |
| Container | docker |
| Orchestrator | kubernetes |
| Resilience | Mandatory (always enabled) |
| Native Build | false |
| Smoke Tests | true |
| Contract Tests | false |

## Language Policy

- Output language: English only
- Code: English (classes, methods, variables)
- Commits: English (Conventional Commits)
- Documentation: English
- Application logs: English

## Constraints

- Cloud-Agnostic: ZERO dependencies on cloud-specific services
- Horizontal scalability: Application must be stateless
- Externalized configuration: All configuration via environment variables or ConfigMaps

## Source of Truth (Hierarchy)

1. Epics / PRDs (vision and global rules)
2. ADRs (architectural decisions)
3. Stories / tickets (detailed requirements)
4. Instructions (`.github/instructions/`)
5. Source code

## Contextual Instructions

For detailed guidance on specific topics, the following contextual instructions are
loaded automatically when relevant:

- `instructions/domain.instructions.md` — Domain model, business rules, ubiquitous language
- `instructions/coding-standards.instructions.md` — Clean Code, SOLID, language conventions
- `instructions/architecture.instructions.md` — Architecture style, layer rules, package structure
- `instructions/quality-gates.instructions.md` — Coverage thresholds, test categories, merge checklist

For deep-dive references, see the knowledge packs in `.claude/skills/`.
