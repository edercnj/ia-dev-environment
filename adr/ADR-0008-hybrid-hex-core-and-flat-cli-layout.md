---
status: Accepted
date: 2026-04-17
deciders:
  - Tech Lead
story-ref: "audit-2026-04-17 finding I-003"
---

# ADR-0008: Hybrid Hex-Core + Flat-CLI Package Layout

## Status

Accepted | 2026-04-17

## Context

The codebase audit of 2026-04-17 (see `results/audits/codebase-audit-2026-04-17.md`,
finding I-003) observed that the project currently hosts a bi-modal package
structure under `java/src/main/java/dev/iadev/`: a strict hexagonal core
(`domain/`, `application/`, `infrastructure/adapter/{input,output}`) sits
alongside roughly fifteen flat CLI / tooling sibling packages (`cli`,
`release/*`, `telemetry/*`, `parallelism/*`, `checkpoint`, `progress`, `ci`,
`template`, `config`, `util`, `smoke`, `exception`). The auditor classified
this as an INFO-level observation: the split is intentional, but undocumented.
A newcomer reading the tree cannot tell which packages MUST obey the inward
dependency rule of Rule 04 and which are exempt, because nothing in the tree
itself advertises the contract. Finding I-002 in the same audit simultaneously
confirmed that the hex core is clean — `domain/**` has zero imports of
Jackson / Gson / Spring / picocli / `application` / `infrastructure`, and
`adapter/input` has zero imports from `adapter/output` — which validates
keeping the hex zone strict.

Historically, ADR-0001 (2026-03-20) justified the project's flat layout at a
high level, on the grounds that a CLI code-generation tool has no external
I/O complexity that warrants full hexagonal scaffolding. ADR-001
(`ADR-001-hexagonal-architecture-migration.md`, 2026-04-04) subsequently
migrated the domain-adjacent core of the generator (project configuration,
template assembly, DAG-driven task dispatch) to a true Ports & Adapters
structure under `domain/`, `application/`, and `infrastructure/`. The two
decisions compose: the hex migration targeted the *generator pipeline*, while
the surrounding CLI subcommands, release tooling, telemetry CLIs, and
parallelism evaluator retained the flat ADR-0001 shape. The result is the
bi-modal tree the audit describes, with no prior ADR that pins the boundary.

## Decision

We formalize the current tree as a **bi-modal package contract** with two
zones, each governed by distinct rules.

### Zone 1 — Hex Zone (MUST obey inward dependency rule)

Packages below this prefix MUST conform to Rule 04
(`.claude/rules/04-architecture-summary.md`): dependencies point inward toward
`domain/`; `domain/` imports only the standard library and its own
sub-packages; framework / I/O code lives behind ports implemented in
`infrastructure/`.

- `dev.iadev.domain.**` — model, ports, services, and pure domain rules
  (`implementationmap`, `model`, `port`, `qualitygate`, `schemaversion`,
  `scopeassessment`, `service`, `stack`, `taskfile`, `taskmap`, `telemetry`,
  `traceability`).
- `dev.iadev.application.**` — use-case orchestration
  (`assembler`, `dag`, `factory`, `taskmap`).
- `dev.iadev.infrastructure.**` — composition root and adapters
  (`infrastructure.adapter.input.*`, `infrastructure.adapter.output.*`,
  `infrastructure.config`).

ArchUnit rules attached to the hex migration (ADR-001) continue to enforce
this zone on every build.

### Zone 2 — Flat Zone (exempt from the inward rule)

The following top-level packages are EXEMPT from the hexagonal dependency
contract. They are picocli subcommands, release-automation helpers, telemetry
CLIs, or CLI-adjacent cross-cutting utilities whose complexity is
orchestration and output formatting, not domain logic. They may freely import
standard-library and project utility classes in whichever direction is
clearest, subject to the usual coding-standards rules.

| Package | Role |
| :--- | :--- |
| `dev.iadev.cli` | picocli entry commands outside the generator pipeline |
| `dev.iadev.release` + sub-packages (`abort`, `changelog`, `dryrun`, `handoff`, `integrity`, `preflight`, `prompt`, `resume`, `state`, `status`, `summary`, `telemetry`, `validate`) | Release orchestration helpers (Git Flow, changelog, resume state) |
| `dev.iadev.telemetry` + sub-packages (`analyze`, `trend`) | Telemetry CLI tools (`TelemetryAnalyzeCli`, `TelemetryTrendCli`, `PiiAudit`) and the shared scrubber |
| `dev.iadev.parallelism` + `parallelism.cli` | `x-parallel-eval` supporting classes and CLI |
| `dev.iadev.checkpoint` | Execution-state persistence for the generator pipeline (ADR-0001 Decision 4) |
| `dev.iadev.progress` | Progress reporting (ADR-0001 Decision 4; ADR-0007 covers the stdout adapter) |
| `dev.iadev.ci` | CI-only lint/validator entrypoints (e.g., telemetry marker lint) |
| `dev.iadev.template` | Template rendering utilities shared across assemblers |
| `dev.iadev.config` | Configuration loading helpers not yet migrated into the hex zone |
| `dev.iadev.util` | Cross-cutting utilities (formatting, string helpers) |
| `dev.iadev.smoke` | Smoke-test entrypoints |
| `dev.iadev.exception` | Project-wide exception types |

This list IS the contract. If the list is wrong, the fix is to update this
ADR; it is not to silently add or remove members.

### Naming — Two Telemetry Packages

The codebase intentionally hosts TWO packages whose leaf name is
`telemetry`. They serve distinct domains and their coexistence is
documented at the package level via `package-info.java`:

| Package | Domain | Key types | Zone |
| :--- | :--- | :--- | :--- |
| `dev.iadev.telemetry` | Claude Code skill / phase / tool **execution events** (EPIC-0040). Owns the immutable event model, the Rule 20 PII scrubber, and the NDJSON writer/reader for `plans/epic-*/telemetry/events.ndjson`. | `TelemetryEvent`, `TelemetryScrubber`, `TelemetryWriter`, `TelemetryReader`, `EventType`, `EventStatus`, `PiiAudit`, `analyze/`, `trend/` | Flat Zone |
| `dev.iadev.domain.telemetry` | Release-phase **benchmark analytics** (story-0039-0012). Pure domain services that analyse `PhaseMetric` streams produced during `x-release`; zero framework imports, zero file I/O. | `BenchmarkAnalyzer`, `BenchmarkResult`, `PhaseBenchmark` | Hex Zone (domain) |

The coexistence is **intentional**. The two packages are not competing
names for the same concept — they model two unrelated subjects that
both happen to be called "telemetry" in the product vocabulary
(execution events vs release-phase profiling). Renaming either
package to disambiguate would be *less* accurate than the current
layout:

- Renaming `dev.iadev.telemetry` to `dev.iadev.cli.telemetry` would
  misclassify its contents — `TelemetryEvent`, `TelemetryScrubber`,
  `TelemetryWriter`, and `TelemetryReader` are the execution-event
  model, not CLI-layer concerns. The `analyze/` and `trend/` CLIs are
  *consumers* of that model, not its defining subject.
- Renaming `dev.iadev.domain.telemetry` would separate
  `BenchmarkAnalyzer` from its natural home inside the hex zone —
  `BenchmarkResult` is not an execution-event concern and does not
  belong anywhere near `TelemetryEvent`.

Each package's `package-info.java` carries the javadoc that
discriminates the two domains. `dev.iadev.telemetry` additionally
documents the separation-of-concerns table covering the three NDJSON
writers (`TelemetryWriter`, `ReleaseTelemetryWriter`,
`FileTelemetryWriter`) that operate in the same neighbourhood.

**Forward-looking rule.** Any NEW top-level or nested package whose
leaf segment is `telemetry` MUST either (a) amend this ADR with an
additional row in the table above, or (b) ship a `package-info.java`
whose javadoc declares its distinct domain and its relationship to the
two packages above. Silently introducing a third `telemetry` package
without either artefact is a violation of this decision and SHOULD be
blocked in code review.

### Default for new packages

**New top-level packages default to the Hex Zone.** A new package MUST either
live under `domain/`, `application/`, or `infrastructure/`, or be added to
the Flat Zone table above by amending this ADR (or authoring a successor
ADR). Introducing a new flat sibling without an ADR update is a violation of
this decision and SHOULD be blocked in code review.

## Consequences

### Positive

- **Newcomer clarity.** A developer opening the tree can read this ADR and
  immediately know which packages follow Rule 04 and which do not, resolving
  the ambiguity the audit raised in I-003.
- **No forced migration cost.** Relocating the ~15 flat packages under
  `infrastructure/adapter/input/cli/**` would be a large mechanical refactor
  with no behavioural benefit; the bi-modal contract legalizes the current
  tree without the churn.
- **Strict hex zone stays strict.** Because the exemption is enumerated (not
  inferred), ArchUnit rules on `domain/**` and `application/**` remain
  maximally strict — the flat zone does not dilute the guarantees proven by
  I-002.
- **Honest default for new code.** Forcing new packages into the hex zone by
  default keeps the flat zone from growing organically; any future exemption
  requires a deliberate ADR amendment.

### Negative

- **Exemption list maintenance.** Adding, renaming, or removing a top-level
  package in the flat zone requires editing this ADR. Skipping the update
  will silently desynchronize the contract from the tree.
- **Audit tooling must suppress false positives.** Future audits should load
  the exemption list from this ADR before flagging Rule 04 violations in the
  flat zone; otherwise I-003 will re-appear on every audit cycle.
- **Two mental models coexist.** Contributors must learn both the strict
  hex-layer rules (for the core) and the looser flat-package conventions
  (for CLI tooling). The boundary is unambiguous once this ADR is read, but
  the cognitive overhead is non-zero.

### Neutral

- **Hybrid layout is idiomatic for CLI codegen tools.** Many generators (yo,
  rush, plop) mix a clean domain core with flat command handlers — hex purity
  across the entire tree is not a goal for the flat zone, only for the core.
- **ADR-0001 remains in force for the flat zone.** The rationale behind the
  original flat layout (no database, no HTTP server, no message broker; the
  assembler pipeline is the use case) continues to justify why these packages
  do not need ports and adapters, even after the core migration.

## Related ADRs

- [`ADR-0001-intentional-architectural-deviations-for-cli-tool.md`](ADR-0001-intentional-architectural-deviations-for-cli-tool.md)
  — original flat-layout precedent; this ADR refines it by drawing the
  explicit hex-vs-flat boundary.
- [`ADR-001-hexagonal-architecture-migration.md`](ADR-001-hexagonal-architecture-migration.md)
  — introduced the hex core; this ADR is the companion document that pins
  which packages belong to the migrated core and which stay outside.
- [`ADR-0007-console-progress-reporter-stdout-contract.md`](ADR-0007-console-progress-reporter-stdout-contract.md)
  — a focused exemption for `ConsoleProgressReporter`'s `System.out` use;
  cross-references the `progress/` row of the flat zone in this ADR.

## Migration Notes

Any subsequent change that adds or removes a top-level package MUST update
the Flat Zone table in this ADR in the same commit. If a new package belongs
in the hex zone (it models domain rules, coordinates use cases, or
implements a port), no ADR change is needed — it goes under
`domain/`, `application/`, or `infrastructure/` and picks up Rule 04
automatically. If a new package genuinely belongs in the flat zone, the
author MUST either (a) amend this ADR or (b) author a successor ADR that
supersedes it.

## Story Reference

- audit-2026-04-17 finding I-003 (this ADR)
- audit-2026-04-17 finding I-002 (evidence that hex core is clean)
- audit-2026-04-17 finding L-012 (closed via the "Naming — Two
  Telemetry Packages" subsection in this ADR)

## Audit Reference

- `results/audits/codebase-audit-2026-04-17.md` — findings I-002,
  I-003, L-012
