---
status: Accepted
date: 2026-04-17
deciders:
  - Tech Lead
story-ref: "audit-2026-04-17 finding C-001"
---

# ADR-0007: ConsoleProgressReporter Uses System.out / System.err Intentionally

## Status

Accepted | 2026-04-17

## Context

The codebase audit of 2026-04-17 (see `results/audits/codebase-audit-2026-04-17.md`,
finding C-001) flagged `ConsoleProgressReporter` as violating Rule 03
(`rules/03-coding-standards.md`), which forbids `System.out` / `System.err` in
production code.

The class in question
(`java/src/main/java/dev/iadev/infrastructure/adapter/output/progress/ConsoleProgressReporter.java`)
is the concrete outbound adapter for the `ProgressReporter` port. Its job is to
emit human-readable progress messages during CLI generator runs:

```
[START] assemble-skills (42 steps)
[1] assemble-skills: copying skills/x-git-commit
[DONE] assemble-skills
[ERROR] assemble-skills: missing template
```

Rule 03's prohibition exists to prevent arbitrary stdout writes scattered through
business logic. This adapter is the opposite: it is the single, explicit place
where CLI progress is written, sitting behind a port interface so that alternate
implementations (silent, JSON-lines, SLF4J) can be swapped in via
`ApplicationFactory` without touching callers.

The audit finding is formally correct but contextually misaligned — the rule
targets ad-hoc println statements, not the CLI adapter whose contract IS stdout.

## Decision

`ConsoleProgressReporter` is **exempt** from the Rule 03 ban on `System.out` and
`System.err`. The exemption is scoped to this single class and justified by its
role as the canonical CLI-output adapter.

### Constraints

1. The exemption applies ONLY to
   `dev.iadev.infrastructure.adapter.output.progress.ConsoleProgressReporter`.
   No other production class may introduce `System.out` / `System.err` calls
   without a new ADR.
2. The class MUST continue to implement `ProgressReporter` and expose NO
   non-interface public methods. Callers MUST depend on the port, never on
   `ConsoleProgressReporter` directly.
3. The file header Javadoc MUST reference this ADR so future auditors
   immediately understand the exemption.
4. Any new progress-reporting adapter (e.g., `JsonProgressReporter`,
   `Slf4jProgressReporter`) MUST live in the same package and MUST NOT inherit
   this exemption — they use their respective output mechanisms (JSON
   serialization, SLF4J logger) instead of `System.out`.

## Consequences

### Positive

- CLI stays debuggable via stdout without bending Rule 03 to accommodate
  adapters, keeping the rule crisp for business code.
- Alternate progress reporters can be introduced without touching this file.
- Audit tooling can suppress this single-file match via a documented exemption
  rather than a broad Rule 03 change.

### Negative

- Static analysis that greps for `System.out` across the main tree will return
  one expected hit that must be manually skipped. A future linter rule can
  whitelist this specific file.

### Neutral

- This ADR does NOT reopen the question for other adapters. Logging adapters
  must use SLF4J; database adapters must use their driver; HTTP adapters must
  use their client library. Only the CLI progress adapter gets this exemption
  because stdout IS its transport.

## Related ADRs

- ADR-0001 — Intentional Architectural Deviations for CLI Code-Generation Tool
  (precedent: flat package layout justified by CLI context)

## Story Reference

- audit-2026-04-17 finding C-001
- `results/audits/codebase-audit-2026-04-17.md`
