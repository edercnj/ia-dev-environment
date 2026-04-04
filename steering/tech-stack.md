# Tech Stack — ia-dev-environment

> Derived from: README.md, .claude/rules/01-project-identity.md

## Core Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 21 |
| Framework | Picocli | 4.7 |
| Build Tool | Maven | 3.9 |
| Template Engine | Pebble | — |
| Container | Docker | — |
| Native Build | GraalVM (optional) | — |

## No External Services

| Layer | Value |
|-------|-------|
| Database | none |
| Cache | none |
| Message Broker | none |
| Orchestrator | none |
| Observability | none |

## Bundled Stack Profiles

The generator ships with 10 ready-to-use profiles:

| Profile | Language | Framework | Database |
|---------|----------|-----------|----------|
| `go-gin` | Go 1.22 | Gin | PostgreSQL |
| `java-picocli-cli` | Java 21 | Picocli 4.7 | — |
| `java-quarkus` | Java 21 | Quarkus 3.17 | PostgreSQL |
| `java-spring` | Java 21 | Spring Boot 3.4 | PostgreSQL |
| `kotlin-ktor` | Kotlin 2.0 | Ktor | PostgreSQL |
| `python-click-cli` | Python 3.9 | Click 8.1 | — |
| `python-fastapi` | Python 3.12 | FastAPI | PostgreSQL |
| `rust-axum` | Rust 2024 | Axum | PostgreSQL |
| `typescript-commander-cli` | TypeScript 5 | Commander | — |
| `typescript-nestjs` | TypeScript 5 | NestJS | PostgreSQL |

## Testing Infrastructure

| Component | Technology |
|-----------|-----------|
| Test Framework | JUnit 5 |
| Coverage | JaCoCo |
| Golden File Tests | Custom (byte-for-byte parity) |
| Smoke Tests | enabled |
| Contract Tests | disabled |

## Constraints

- Cloud-Agnostic: zero dependencies on cloud-specific services
- Stateless: application must be stateless for horizontal scalability
- Externalized configuration: all configuration via environment variables or ConfigMaps
