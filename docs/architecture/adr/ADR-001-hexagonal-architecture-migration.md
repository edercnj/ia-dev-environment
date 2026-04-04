# ADR-001: Hexagonal Architecture Migration

**Status:** Accepted
**Date:** 2026-04-04
**Epic:** EPIC-0015 (15 stories, 8 phases)

## Context

The `ia-dev-env` Java CLI tool generates development environment configurations
(Claude Code, GitHub Copilot, Codex artifacts) from a YAML specification.
The original codebase used a flat package structure where domain logic, CLI
commands, template rendering, and file I/O were tightly coupled.

Key problems with the original architecture:

1. **Domain pollution**: Domain model classes (`ProjectConfig`, `MapHelper`)
   imported infrastructure exceptions (`dev.iadev.exception`), making the
   domain dependent on non-domain packages.
2. **No port/adapter separation**: CLI commands directly orchestrated domain
   logic and I/O without use case interfaces.
3. **Untestable in isolation**: Domain logic could not be tested without
   infrastructure dependencies (SnakeYAML, Pebble, Jackson).
4. **No architectural enforcement**: No automated rules prevented dependency
   violations from being introduced.

## Decision

Migrate to Hexagonal Architecture (Ports & Adapters) with the following
structure:

- **Domain layer** (`domain/`): Pure business logic with zero external
  dependencies. Contains model records, port interfaces (input and output),
  and domain services.
- **Application layer** (`application/`): Template assemblers that orchestrate
  domain ports to produce output files.
- **Infrastructure layer** (`infrastructure/`): Adapters implementing ports
  (CLI input, config/template/filesystem/checkpoint/progress output) and the
  composition root (`ApplicationFactory`).

Architecture enforcement via ArchUnit with 8 rules running on every build.

## Alternatives Considered

### 1. Clean Architecture (Uncle Bob)

Rejected because the additional layers (entities, use cases, interface adapters,
frameworks) add complexity without proportional benefit for a CLI tool with
a relatively simple request-response lifecycle.

### 2. Layered Architecture (traditional)

Rejected because it allows upward dependencies (service layer accessing
presentation DTOs) and does not enforce port/adapter contracts.

### 3. No migration (status quo)

Rejected because the tool that generates hexagonal architecture for third-party
projects was itself not following hexagonal principles, creating an
inconsistency between what we recommend and what we practice.

## Consequences

### Positive

- **Domain isolation**: Domain model has zero external library imports,
  validated by ArchUnit RULE-001 and RULE-004 on every build.
- **Testability**: Domain services can be tested with mock ports, no
  infrastructure required.
- **Architectural consistency**: The tool now follows the same hexagonal
  pattern it generates for other projects.
- **Regression protection**: 8 ArchUnit rules prevent future violations
  automatically.
- **Clear contracts**: Input and output ports define explicit interfaces
  between layers.

### Negative

- **Increased file count**: 16 new packages with package-info.java files,
  domain exception classes, port interfaces, and service implementations.
- **Legacy bridge**: Infrastructure exceptions extend domain exceptions for
  backward compatibility, adding a thin inheritance layer.
- **Partial migration**: Legacy packages (`cli/`, `config/`, `template/`,
  `checkpoint/`, `progress/`) remain active. Full removal requires additional
  stories beyond this epic.

### Risks

- **Legacy drift**: Legacy packages may accumulate new code that bypasses
  hexagonal patterns. Mitigated by ArchUnit rules preventing domain
  violations.
- **Maintainer confusion**: Two code paths (legacy + hexagonal) exist in
  parallel. Mitigated by documentation in `service-architecture.md`.

## Migration Approach

Incremental migration across 15 stories in 8 phases:

| Phase | Stories | Description |
| :--- | :--- | :--- |
| 0 | 0001 | ArchUnit baseline audit (document AS-IS violations) |
| 1 | 0002 | Package scaffolding (create hexagonal directories) |
| 2 | 0003 | Domain model extraction (records to domain.model) |
| 3 | 0004, 0005 | Port interfaces (input and output ports) |
| 4 | 0006 | Domain services (implement input ports) |
| 5 | 0007-0012 | Output adapters (6 parallel adapter implementations) |
| 6 | 0013 | Assembler migration to application layer |
| 7 | 0014 | Composition root (ApplicationFactory wiring) |
| 8 | 0015 | Final cleanup (activate all ArchUnit rules, documentation) |

## Metrics

| Metric | Before (AS-IS) | After (TO-BE) |
| :--- | :--- | :--- |
| ArchUnit rules active | 0 / 8 | 8 / 8 |
| Domain isolation violations | Multiple (MapHelper -> exception) | 0 |
| @Disabled ArchUnit rules | 1 | 0 |
| Test count | ~1961 | 3054 |
| Test failures | 0 | 0 |
