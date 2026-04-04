---
status: Accepted
date: 2026-03-20
deciders:
  - Tech Lead
story-ref: "story-0008-0030"
---

# ADR-0001: Intentional Architectural Deviations for CLI Code-Generation Tool

## Status

Accepted | 2026-03-20

## Context

The codebase audit of 2026-03-20 (see `docs/audits/codebase-audit-2026-03-20.md`) identified five
architectural findings that deviate from the hexagonal architecture prescribed by Rule 04
(`rules/04-architecture-summary.md`):

| Finding | Severity | Description |
|---------|----------|-------------|
| M-007   | MEDIUM   | No `port/`, `application/`, or `adapter/` package hierarchy exists |
| M-008   | MEDIUM   | Domain package imports from sibling `model/` package instead of nested `domain/model/` |
| M-010   | MEDIUM   | CLI directly imports assemblers without an application service layer |
| L-005   | LOW      | `model/` and `exception/` exist as siblings of `domain/` rather than sub-packages |
| L-006   | LOW      | `progress/` is tightly coupled to `checkpoint/` without a formal interface boundary |

This project is a **CLI code-generation tool** (`ia-dev-environment`). It has no external I/O
complexity (no database, no HTTP server, no message broker, no cache). Its primary function is to
read configuration files and produce generated output files. The architectural trade-offs for this
kind of tool differ fundamentally from a typical hexagonal-architecture service.

Two related stories addressed findings where the deviation was **not** justified:

- **story-0008-0019** (commit `02f5d02`): Extracted Jackson `ObjectMapper` from `CheckpointEngine`
  (domain-adjacent code) into a `CheckpointPersistence` port and `JacksonCheckpointSerializer`
  adapter. This resolved findings C-005 and L-010, where a framework dependency in domain code was
  a genuine violation rather than an intentional deviation.

- **story-0008-0020** (commit `d68f17d`): Extracted `java.nio.file.Files` I/O from `VersionResolver`
  (domain class) into a `VersionDirectoryProvider` port and `FileSystemVersionProvider` adapter.
  This resolved finding M-009, where filesystem I/O in a domain class was a genuine violation.

The remaining five findings (M-007, M-008, M-010, L-005, L-006) are **intentional** trade-offs
documented in this ADR.

## Decision

We adopt four deliberate deviations from the canonical hexagonal architecture prescribed by Rule 04.

### Decision 1: Flat Package Layout (addresses M-007)

**We use a flat package layout instead of the prescribed `adapter/inbound`, `adapter/outbound`,
`domain/port`, and `application/` hierarchy.**

The project structure is:

```
dev.iadev/
  assembler/     # Pipeline steps that transform config into files
  cli/           # Picocli command handlers (inbound adapter equivalent)
  config/        # Configuration loading (outbound adapter equivalent)
  domain/        # Business rules and routing logic
  model/         # Shared DTOs and configuration records
  checkpoint/    # Execution state persistence
  progress/      # Progress reporting
  template/      # Template rendering
  util/          # Cross-cutting utilities
```

**Rationale:** The full hexagonal hierarchy (`adapter/inbound/`, `adapter/outbound/`,
`domain/port/`, `application/`) adds four levels of package nesting and multiple indirection
layers. For a CLI tool with no external I/O complexity (no database, no HTTP, no messaging),
this indirection provides no testability or substitutability benefit. The flat layout is
navigable and keeps the dependency direction clear without artificial wrapping.

### Decision 2: `model/` as Sibling of `domain/` (addresses M-008, L-005)

**We keep `model/` as a sibling package of `domain/` rather than nesting it under `domain/model/`.**

**Rationale:** The records in `model/` are **pure DTOs** (configuration records, stack profiles,
project metadata) shared across assemblers, CLI, and domain logic. They contain zero framework
imports and zero business logic. Placing them inside `domain/` would create a false impression
that they are domain entities with invariants and behavior. As siblings, they serve as a shared
vocabulary without implying domain ownership. This also avoids circular dependencies: assemblers
depend on `model/` for configuration records, and `domain/` depends on `model/` for the same
records. If `model/` were nested under `domain/`, assemblers would need to import from `domain/`,
violating the expected dependency direction.

### Decision 3: No `application/` Layer (addresses M-010)

**We do not create an `application/` layer with use-case classes. The assembler pipeline serves
as the orchestration mechanism.**

**Rationale:** Each assembler is an idempotent pipeline step that receives configuration and
produces output files. The `AssemblerPipeline` orchestrates these steps in dependency order.
Introducing use-case classes would duplicate this orchestration responsibility without adding
value. The CLI commands (`GenerateCommand`, `ValidateCommand`) invoke the pipeline directly,
which is the natural entry point for a CLI tool. An application layer would be a pass-through
wrapper adding complexity without behavior.

### Decision 4: Direct Coupling Between `progress/` and `checkpoint/` (addresses L-006)

**We allow `progress/` to depend directly on `checkpoint/` without an intermediary interface.**

**Rationale:** The `progress/` package reports execution state that is tracked by `checkpoint/`.
These two packages are **cohesive by nature**: progress cannot be reported without checkpoint
state, and checkpoint state is meaningless without progress reporting. They will not be
substituted independently (there is no scenario where progress would be backed by a different
state mechanism). Adding an interface between them would be speculative abstraction with no
concrete use case.

## Consequences

### Positive

- **Simplicity:** The flat layout is immediately navigable. New contributors can understand the
  package structure without hexagonal architecture knowledge.
- **Reduced indirection:** No pass-through layers, wrapper classes, or empty interfaces that exist
  solely for architectural compliance.
- **Faster development:** Adding a new assembler or command requires touching fewer files and
  creating fewer boilerplate classes.
- **Honest architecture:** The package structure reflects what the system actually does (read
  config, transform, write files) rather than forcing a pattern designed for systems with complex
  I/O boundaries.

### Negative

- **Deviation from Rule 04:** Future audits will flag these same findings unless they reference
  this ADR. All future audit reports must check this ADR before flagging M-007, M-008, M-010,
  L-005, or L-006.
- **Non-transferable patterns:** Developers moving from this project to a hexagonal-architecture
  service must not carry these patterns over. The flat layout is justified only for CLI tools
  without external I/O complexity.
- **Risk of drift:** Without the structural enforcement of hexagonal packages, dependency
  violations are harder to detect automatically. The audit must continue monitoring dependency
  direction even within the flat layout.

### Neutral

- **Port/Adapter pattern still applies selectively:** Stories 0019 and 0020 demonstrate that where
  a genuine framework dependency exists in domain code (Jackson in `CheckpointEngine`, filesystem
  I/O in `VersionResolver`), the Port/Adapter pattern is applied. The flat layout does not mean
  "no abstractions"; it means "abstractions where they provide concrete value."
- **The flat layout may evolve:** If the project gains external I/O complexity (database, HTTP
  client, message broker), this ADR should be revisited and the architecture restructured
  accordingly.

## Related ADRs

- None (first ADR in this project)

## Story Reference

- story-0008-0030 (this ADR)
- story-0008-0019 (Jackson extraction from checkpoint domain -- resolved C-005, L-010)
- story-0008-0020 (I/O extraction from VersionResolver domain -- resolved M-009)

## Audit Reference

- `docs/audits/codebase-audit-2026-03-20.md` -- findings M-007, M-008, M-010, L-005, L-006
