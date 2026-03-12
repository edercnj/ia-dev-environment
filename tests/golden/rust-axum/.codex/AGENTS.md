
# my-rust-service

Describe your service purpose here

## Architecture

- **Style:** microservice
- **Language:** rust 2024
- **Framework:** axum 

### Dependency Direction

Dependencies point inward toward the domain. Domain NEVER imports adapter or framework code.

```
adapter.inbound → application → domain ← adapter.outbound
                                  ↑
                           (ports/interfaces)
```

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | rust 2024 |
| Framework | axum  |
| Build Tool | cargo |
| Container | docker |
| Orchestrator | kubernetes |

## Commands

| Command | Script |
|---------|--------|
| Build | `cargo build` |
| Test | `cargo test` |
| Compile | `cargo check` |
| Coverage | `cargo tarpaulin` |

## Coding Standards

### Hard Limits

| Constraint | Limit |
|-----------|-------|
| Method/function length | ≤ 25 lines |
| Class/module length | ≤ 250 lines |
| Parameters per function | ≤ 4 (use parameter object if more) |
| Line width | ≤ 120 characters |

### Naming

- Intent-revealing names: `elapsedTimeInMs` not `d`
- Verbs for methods: `processTransaction()`, `extractAmount()`
- Nouns for types: `TransactionHandler`, `DecisionEngine`
- Named constants: never magic numbers or strings

### SOLID Principles

- **SRP**: One class = one reason to change
- **OCP**: New behavior = new class, never modify existing handlers
- **LSP**: Every implementation must fulfill its interface contract
- **ISP**: Small focused interfaces; no empty method implementations
- **DIP**: Depend on abstractions (ports), not concrete implementations

### Error Handling

- NEVER return null — use Optional, empty collection, or Result type
- NEVER pass null as argument
- Exceptions MUST carry context (values that caused the error)
- Catch at the right level, not where convenient

### Forbidden Patterns

- Boolean flags as function parameters
- Comments that repeat what code says
- Mutable global state
- God classes / train wrecks (chained calls across objects)
- Wildcard imports
- `sleep()` for synchronization

### Language-Specific

Follow rust 2024 idiomatic conventions for formatting, constructor injection, and mapper patterns.

## Quality Gates

### Coverage Thresholds

| Metric | Minimum |
|--------|---------|
| Line Coverage | ≥ 95% |
| Branch Coverage | ≥ 90% |

### Test Categories

- **Unit** — domain models, engines, business rules
- **Integration** — database + framework
- **API** — HTTP/gRPC/GraphQL endpoints
- **E2E** — full flow with real database
- **Performance** — latency SLAs, throughput, resource usage
- **Smoke** — black-box against running environment

### Test Naming

```
[methodUnderTest]_[scenario]_[expectedBehavior]
```

### Merge Checklist

- [ ] All tests passing
- [ ] Coverage ≥ 95% line, ≥ 90% branch
- [ ] Zero compiler/linter warnings
- [ ] Security review for sensitive changes

## Conventions

### Commits

- Format: Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`, `refactor:`, `test:`)
- Language: English

### Branches

- Feature: `feat/STORY-ID-description`
- Fix: `fix/STORY-ID-description`
- Chore: `chore/description`

### Code Language

- All code (classes, methods, variables): English
- All documentation: English
- Application logs: English

### Documentation

- Keep documentation close to the code it describes
- Update documentation when changing behavior

## Available Skills

| Skill | Description |
|-------|-------------|
| api-design | API design principles: {{LANGUAGE}}-specific patterns for REST/gRPC/GraphQL. URL structure, status codes, RFC 7807 errors, pagination, content negotiation, validation, request/response shaping, versioning strategies, and protocol conventions. |
| architecture | Full architecture reference: {{ARCHITECTURE}} principles, package structure, dependency rules, thread-safety, mapper patterns, persistence rules, and architecture variants. Read before designing or implementing features. |
| axum-patterns | Axum-specific patterns: extractors, Router composition, Tower middleware, sqlx async data access, config crate layered config, tokio testing, IntoResponse error handling. Internal reference for agents producing Axum code. |
| coding-standards | Complete coding conventions: Clean Code rules (CC-01 to CC-10), SOLID principles, {{LANGUAGE}} {{LANGUAGE_VERSION}} idioms, naming patterns, constructor injection, mapper conventions, version-specific features, and approved libraries. Read before writing any code. |
| compliance | Compliance frameworks (conditionally included): GDPR, HIPAA, LGPD, PCI-DSS, SOX. Data classification, rights enforcement, processing records, international transfers, security measures, audit logging, and framework-specific requirements. |
| dockerfile | Dockerfile patterns per language covering multi-stage builds, security hardening, .dockerignore templates, layer optimization, health checks, and OCI labels. Internal reference for agents managing infrastructure. |
| iac-terraform | Terraform patterns reference covering module structure, remote state, naming conventions, CI/CD workflows, drift detection, and common infrastructure modules. Internal reference for agents managing infrastructure. |
| infrastructure | Infrastructure patterns: Docker multi-stage builds, Kubernetes manifests (cloud-agnostic), security context, 12-Factor App principles, graceful shutdown, resource management, and cloud-native design. |
| k8s-deployment | Kubernetes deployment patterns reference covering workload types, pod specifications, resource sizing, probes, autoscaling, network policies, and security contexts. Internal reference for agents managing infrastructure. |
| k8s-kustomize | Kustomize patterns for environment management covering directory structure, patches, components, secret management, generators, and patch types. Internal reference for agents managing infrastructure. |
| layer-templates | Reference code templates for each hexagonal architecture layer. Provides consistent patterns for domain model, ports, DTOs, mappers, entities, repositories, use cases, REST resources, exception mappers, migrations, and configuration. Uses {{LANGUAGE}}, {{FRAMEWORK}} placeholders. |
| observability | Observability principles: distributed tracing (span trees, mandatory attributes), metrics naming conventions, structured logging with mandatory fields, health checks (liveness/readiness/startup), correlation IDs, and OpenTelemetry integration. |
| patterns |  |
| protocols | Protocol conventions: REST (OpenAPI 3.1), gRPC (Proto3), GraphQL, WebSocket, and event-driven messaging. URL structure, versioning, error handling per protocol, schema design, and integration patterns. |
| resilience | Resilience patterns: circuit breaker, rate limiting, bulkhead isolation, timeout control, retry with exponential backoff + jitter, fallback/graceful degradation, backpressure, and resilience metrics. |
| run-e2e | Skill: End-to-End Tests — Runs integration tests that validate the complete flow from request through all application layers to response, using a real database. |
| run-perf-test | Skill: Performance/Load Tests — Runs performance tests to validate latency SLAs, throughput targets, and resource stability under load. Supports baseline, normal, peak, and sustained scenarios. |
| run-smoke-api | Skill: REST API Smoke Tests — Runs automated smoke tests against the REST API using Newman/Postman. Supports local, container-orchestrated, and staging environments. |
| security | Complete security reference: OWASP Top 10, security headers, secrets management, input validation, cryptography (TLS, hashing, key management), and pentest readiness checklist. Read during security reviews or when implementing security-sensitive features. |
| setup-environment | Skill: Dev Environment Setup — Sets up the local development environment including container orchestrator, database, and build tools. |
| story-planning | Story decomposition and planning: layer-by-layer decomposition (foundation, core domain, extensions, compositions, cross-cutting), story self-containment (data contracts, acceptance criteria), dependency DAG, sizing rules, and phase computation. |
| testing | Complete testing reference: testing philosophy, 8 test categories, coverage thresholds, fixture patterns, data uniqueness, async handling, database strategy, and {{LANGUAGE}}-specific test frameworks. Read before writing tests. |
| x-dev-implement | Implements a feature/story following project conventions. Delegates preparation to a subagent that reads architecture and coding KPs, then implements layer-by-layer with intermediate compilation checks. |
| x-dev-lifecycle | Orchestrates the complete feature implementation cycle: branch creation, planning, task decomposition, implementation, parallel review, fixes, PR creation, and final verification. Delegates heavy phases to subagents for context efficiency. |
| x-git-push | Git operations: branch creation, atomic commits (Conventional Commits), push, and PR creation. Use for any git workflow task including branching, committing, pushing, creating PRs, or managing version control. |
| x-ops-troubleshoot | Diagnoses errors, stacktraces, build failures, and unexpected behavior. Systematic approach: reproduce, locate, understand, fix, verify. Use whenever something fails: compilation errors, test failures, runtime exceptions, coverage gaps, or performance issues. |
| x-review | Parallel code review with specialist engineers (Security, QA, Performance, Database, Observability, DevOps, API, Event). Launches parallel subagents, each reading their own knowledge pack, then consolidates into a scored report. Use for pre-PR quality validation. |
| x-review-api | Skill: REST API Design Review — Validates REST API endpoints for RFC 7807 error responses, pagination, URL versioning, OpenAPI documentation, status codes, and DTO patterns. |
| x-review-events | Skill: Event-Driven Review — Validates event schemas, producer/consumer patterns, error handling, dead letter topics, and operational readiness. |
| x-review-gateway | Review API gateway configuration for best practices |
| x-review-grpc | Skill: gRPC Service Review — Validates gRPC service definitions, proto3 conventions, implementation patterns, and operational readiness. |
| x-review-pr | Tech Lead holistic review with 40-point checklist covering Clean Code, SOLID, architecture, framework conventions, tests, security, and cross-file consistency. Produces GO/NO-GO decision. Use for final review before merge. |
| x-story-create | > |
| x-story-epic | > |
| x-story-epic-full | > |
| x-story-map | > |
| x-test-plan | Generates a comprehensive test plan before implementation. Delegates KP reading to a context-gathering subagent, then produces structured test scenarios covering unit, integration, API, E2E, contract, and performance tests. |
| x-test-run | Runs tests with coverage reporting and threshold validation. Use whenever writing, running, or analyzing tests. Triggers on: test, coverage, TDD, unit test, integration test, test failure, coverage gap, or Definition of Done validation. |


## Agent Personas

| Agent | Role |
|-------|------|
| api-engineer | Global Behavior & Language Policy |
| architect | Global Behavior & Language Policy |
| devops-engineer | Global Behavior & Language Policy |
| event-engineer | Global Behavior & Language Policy |
| performance-engineer | Global Behavior & Language Policy |
| product-owner | Global Behavior & Language Policy |
| qa-engineer | Global Behavior & Language Policy |
| rust-developer | Global Behavior & Language Policy |
| security-engineer | Global Behavior & Language Policy |
| tech-lead | Global Behavior & Language Policy |


