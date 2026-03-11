# Project Identity — my-cli-tool

## Identity

- **Name:** my-cli-tool
- **Architecture Style:** library
- **Domain-Driven Design:** false
- **Event-Driven:** false
- **Interfaces:** cli
- **Language:** python 3.9
- **Framework:** click 8.1

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Architecture | Library |
| Language | Python 3.9 |
| Framework | Click 8.1 |
| Build Tool | Pip |
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
