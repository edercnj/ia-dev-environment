
# my-nestjs-service

Describe your service purpose here

## Architecture

- **Style:** microservice
- **Language:** typescript 5
- **Framework:** nestjs 

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
| Language | typescript 5 |
| Framework | nestjs  |
| Build Tool | npm |
| Container | docker |
| Orchestrator | kubernetes |

## Commands

| Command | Script |
|---------|--------|
| Build | `npm run build` |
| Test | `npm test` |
| Compile | `npx --no-install tsc --noEmit` |
| Coverage | `npm test -- --coverage` |

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

### TDD Practices

- **Red-Green-Refactor** is mandatory for all production code
- Refactoring criteria: extract method when > 25 lines, eliminate duplication, improve naming
- Refactoring NEVER adds behavior
- Full TDD reference: `skills/testing/SKILL.md`

### Language-Specific

Follow typescript 5 idiomatic conventions for formatting, constructor injection, and mapper patterns.

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
- **Contract** — parametrized business rules
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
- [ ] Commits show test-first pattern (test precedes implementation in git log)
- [ ] Explicit refactoring after green
- [ ] Tests are incremental (simple to complex via TPP)
- [ ] Tests precede or accompany implementation (no test-after in later commits)
- [ ] Acceptance tests exist and validate end-to-end behavior

### TDD Compliance

- **Double-Loop TDD**: Outer loop (acceptance test) drives inner loop (unit tests)
- **TPP**: Order tests from simple to complex
- **Atomic TDD commits**: Each cycle produces atomic commits

## Domain

This project follows Domain-Driven Design principles.

### Domain Model

Define your core entities, value objects, and aggregates here.

### Aggregate Boundaries

Each aggregate enforces its own invariants. Cross-aggregate communication happens through domain events or application services.

### Business Rules

Document business rules with unique identifiers (e.g., BR-001) for traceability.

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

