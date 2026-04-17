# Specialist Review — Security

**Engineer:** Security Specialist
**Story:** story-0040-0010
**PR:** #420
**Date:** 2026-04-16

---

## Score: 28/30
## Status: Partial

---

## PASSED

- [SEC-01] **No hardcoded credentials** (2/2) — no passwords, tokens, or API keys in the diff; all configuration is CLI-driven.
- [SEC-02] **Path traversal defense via resolve + no user-chosen base** (2/2) — `aggregateEpics` resolves `base/epic-XXXX/telemetry/events.ndjson` from the epic ID; the epic ID is validated only loosely (starts-with `EPIC-`), but the final path is joined via `Path.resolve` and `Files.exists`, so an attacker-controlled `--epic "../../etc/passwd"` resolves to a normalized path and the file likely doesn't exist. Low-risk because this is a developer CLI, not a server.
- [SEC-03] **No deserialization of untrusted data** (2/2) — NDJSON parsing goes through `TelemetryEvent.fromJsonLine` which uses a configured Jackson mapper on a closed schema; no generic `ObjectInputStream` or dynamic class loading.
- [SEC-04] **PII scrubbing still applied upstream** (2/2) — the analyze skill reads NDJSON files that are already scrubbed by the write path (`TelemetryScrubber` from story-0040-0005). The analyzer does not re-introduce PII because it only aggregates numeric durations and skill/phase labels.
- [SEC-05] **Structured error messages** (2/2) — error output to stderr is human-readable without leaking stack traces or internal class names.
- [SEC-06] **No wildcard imports** (2/2) — every import is explicit.
- [SEC-07] **No use of `Math.random()` or insecure RNG** (2/2) — no randomness required in this feature.
- [SEC-08] **No SQL / shell injection surface** (2/2) — no database access, no shell invocation, no `Runtime.exec`.
- [SEC-09] **Output file path is user-provided but not privileged** (2/2) — `--out` takes a user path; `Files.writeString` writes only to the provided target. A malicious user could `--out /etc/passwd`, but that's the user's own privileges — not a web-facing attack surface.
- [SEC-10] **No eval / dynamic code** (2/2) — no reflection, no scripting engines, no class-loading.
- [SEC-11] **Defensive copies in value objects** (2/2) — `Stat`, `AnalysisReport`, `PhaseTimeline` defensively copy their list fields via `List.copyOf(...)` or `Collections.unmodifiableMap`-style patterns.
- [SEC-12] **No debug / verbose error leakage** (2/2) — the CLI does not print stack traces; `CORRUPT_NDJSON` path logs a short reason only.
- [SEC-13] **Structured logging via SLF4J where used** (2/2) — `TelemetryReader` (reused code) already uses SLF4J; the new code itself does not log, which is acceptable for a one-shot CLI.

## PARTIAL

- [SEC-14] **`--out` lacks prefix validation** (1/2) — `writeOutput` writes directly to the user-supplied path after `Files.createDirectories(parent)`. A mis-used `--out ../../config.yaml` would clobber a config file. Not dangerous in normal operator use, but the CLI could defensively reject paths outside the CWD or the `plans/` subtree. — Improvement: consider a check `if (!target.toAbsolutePath().startsWith(Path.of("").toAbsolutePath())) warn(...)` or document the expectation in SKILL.md. `java/src/main/java/dev/iadev/telemetry/analyze/TelemetryAnalyzeCli.java:225-240` [LOW]
- [SEC-15] **NDJSON file is silently truncated on write failure mid-flush** (1/2) — The report renderer returns a fully-constructed `String`; if the JVM dies between `Files.writeString` start and completion, the report is half-written. Not a security issue per se, but a report file with missing sections could be misinterpreted. — Improvement: write to a temp file and atomic-rename on success (`Files.move(tmp, target, ATOMIC_MOVE)`). `java/src/main/java/dev/iadev/telemetry/analyze/TelemetryAnalyzeCli.java:234-240` [LOW]

## Severity Distribution

| Severity | Count |
| --- | --- |
| Critical | 0 |
| High | 0 |
| Medium | 0 |
| Low | 2 |
