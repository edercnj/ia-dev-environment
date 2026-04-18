---
status: Accepted
date: 2026-04-17
deciders:
  - Tech Lead
story-ref: "audit-2026-04-17 findings M-008, M-009"
---

# ADR-0009: Wide Records Bound to External Schemas are Exempt from Rule 03 Parameter Limit

## Status

Accepted | 2026-04-17

## Context

The codebase audit of 2026-04-17 (see `results/audits/codebase-audit-2026-04-17.md`,
findings M-008 and M-009) flagged two records for violating Rule 03
(`rules/03-coding-standards.md`), which caps constructors at ≤ 4 parameters
and recommends a parameter object when the list grows beyond that:
`dev.iadev.checkpoint.ExecutionState` carries 12 components and
`dev.iadev.telemetry.TelemetryEvent` carries 16. In both cases the obvious
suggestion — group the components into sub-records (`Metrics`, `Parallelism`,
`Stories` for `ExecutionState`; `Identity`, `Timing`, `Metadata` for
`TelemetryEvent`) — was prototyped during the P1 audit-remediation wave and
then deferred. This ADR records the engineering reason the deferral is
permanent.

Both records are **schema-bound**: their in-memory shape is the wire and
on-disk shape of a file format that is committed to the repository and read
by long-lived tooling. `ExecutionState` is Jackson-serialized to
`plans/epic-*/execution-state.json` files that predate EPIC-0038; Rule 19
(Backward Compatibility of Planning Schema) mandates that legacy epics whose
`planningSchemaVersion` is absent or `"1.0"` continue to deserialize into the
same record via `SchemaVersionResolver`, with an enumerated fallback matrix
covering missing fields, invalid values, and absent files. Any restructuring
of the record's component list without a compensating Jackson customization
changes the JSON key set at the root object and breaks that matrix.
`TelemetryEvent` is serialized as NDJSON to `plans/epic-*/telemetry/events.ndjson`;
Rule 20 (Telemetry Privacy) pins the key set that the scrubber operates on,
the PII fuzz corpus under `java/src/test/resources/fixtures/telemetry/pii-corpus.txt`
exercises that key set, and historical committed NDJSON is expected to remain
readable by `TelemetryAnalyzeCli`, `TelemetryTrendCli`, and `PiiAudit`
indefinitely (per Rule 20 §4 Rotation & Retention).

The natural way to preserve the wire shape while narrowing the Java
constructor is Jackson's `@JsonUnwrapped`, which flattens a nested object's
fields into its parent during serialization. `@JsonUnwrapped` does NOT work
cleanly on Java records during deserialization — the limitation is tracked
upstream as jackson-databind #1467 and related issues, and requires custom
`JsonDeserializer` + `JsonSerializer` pairs (plus a creator-bound
`@JsonCreator`) per enclosing record to round-trip. Introducing those
serializer pairs for two records whose entire point is to be transparent JSON
data carriers trades a Rule 03 constructor-arity smell for a bigger, more
fragile anti-pattern: hand-written deserializers that must be kept in lock-step
with the schema, tested against the same fuzz corpus, and audited on every
field addition. The Rule 03 violation is cosmetic (a symptom of the schema
being wide); the custom-deserializer path would be structural.

## Decision

Two narrow, enumerated exemptions to Rule 03's ≤ 4-parameter constraint.

### Exemption 1 — `dev.iadev.checkpoint.ExecutionState`

`dev.iadev.checkpoint.ExecutionState` is **exempt** from Rule 03's ≤ 4
parameter cap. Its component list IS the record's JSON schema contract with
legacy epic-state files under `plans/epic-*/execution-state.json`. Any
future change to the component list MUST:

1. Be an additive evolution (new optional components, never renames or
   removals of existing ones) that remains compatible with the fallback
   matrix documented in Rule 19 §"Fallback Matrix".
2. Be accompanied by a parse test that reads a committed historical
   `execution-state.json` from at least one v1 epic and at least one v2 epic
   and asserts round-trip equivalence.
3. Keep the `SchemaVersionResolver` log-code contract intact
   (`SCHEMA_VERSION_FALLBACK_NO_FILE`, `SCHEMA_VERSION_FALLBACK_MISSING_FIELD`,
   `SCHEMA_VERSION_INVALID_VALUE`) — these are an interop signal Rule 19 forbids
   removing.

### Exemption 2 — `dev.iadev.telemetry.TelemetryEvent`

`dev.iadev.telemetry.TelemetryEvent` is **exempt** for the same class of
reason. Its component list IS the NDJSON schema contract governed by Rule 20
(Telemetry Privacy) and validated by the PII fuzz corpus. Any future change
to the component list MUST:

1. Be tested round-trip against committed NDJSON in `plans/epic-*/telemetry/`
   — the committed data is the acceptance fixture, not a courtesy.
2. Pass the existing `TelemetryScrubberTest` and `TelemetryScrubberFuzzTest`
   without modification of replacement markers (Rule 20 §7 forbids renaming
   markers).
3. If a new field would accept free-form strings, be added to the scrubber's
   scan set AND to the fuzz corpus in the same commit (Rule 20 §2).
4. Keep the `MetadataWhitelist.ALLOWED_KEYS` set disciplined — adding a new
   metadata key requires amending Rule 20 §3 in the same PR (this ADR does
   NOT grant cover for skipping that step).

### Scope limit

These exemptions apply ONLY to the two classes named above. Other wide
records in the codebase do NOT inherit the exemption. Concretely:

- `dev.iadev.cli.ProjectSummary` (M-010) was refactored into composite
  sub-records during the same audit cycle and is NOT an exempt class.
- Any new record that exceeds 4 components MUST either (a) be refactored to
  sub-records per Rule 03, or (b) be added to the exemption list of a
  successor ADR with an equivalent serialization / schema justification
  (committed on-disk format, fuzz-corpus coverage, or documented backward-compat
  contract). The default answer is (a); (b) requires an ADR.

Static analysis rules that flag "records with > 4 components" MUST carry this
two-class allow-list. Re-raising M-008 or M-009 on a future audit cycle is a
false positive that this ADR closes.

## Consequences

### Positive

- **Rule 19 and Rule 20 contracts preserved.** The committed JSON under
  `plans/epic-*/execution-state.json` and the committed NDJSON under
  `plans/epic-*/telemetry/events.ndjson` continue to deserialize with zero
  custom Jackson machinery. No risk of silent breakage for long-lived
  historical data.
- **Readers of the code understand the deviation.** A contributor who lands
  on `ExecutionState.java` or `TelemetryEvent.java` finds an ADR reference in
  the class Javadoc explaining why the wide constructor is deliberate, rather
  than being tempted to "fix" it in a drive-by refactor.
- **Rule 03 stays crisp elsewhere.** The general ≤ 4-parameter guidance is
  not relaxed for the codebase — it is preserved, with two enumerated
  exceptions whose justification is structural, not stylistic.

### Negative

- **Static analysis must allow-list these two classes.** Any linter that
  greps for "records with > 4 components" will return two expected hits;
  audit tooling MUST suppress them by class name, not by pattern.
- **Field additions cascade.** Adding a new component to either record
  propagates into every constructor call site, every test fixture, and (for
  `TelemetryEvent`) into the scrubber scan-set. That overhead is inherent to
  schema-bound data carriers; the ADR does not hide it.

### Neutral

- **Potential future supersession.** If jackson-databind closes #1467 and
  ships first-class record-`@JsonUnwrapped` deserialization, a successor
  ADR MAY permit splitting these records into composite sub-records while
  preserving the flat wire format. Until then the exemption stands; this
  ADR does not impose the refactor speculatively.
- **No precedent for "wide records are fine in general".** This decision
  does NOT say Rule 03 is wrong. It says two specific records carry a
  serialization contract that dominates the parameter-count concern, and
  enumerates them.

## Related ADRs

- [`ADR-0001-intentional-architectural-deviations-for-cli-tool.md`](ADR-0001-intentional-architectural-deviations-for-cli-tool.md)
  — precedent for narrowly-scoped, audit-closing ADR exemptions with a clear
  rationale and enumerated scope.
- [`ADR-0008-hybrid-hex-core-and-flat-cli-layout.md`](ADR-0008-hybrid-hex-core-and-flat-cli-layout.md)
  — companion audit-closing ADR (2026-04-17); demonstrates the pattern of
  exempting an enumerated set from a general rule rather than relaxing the
  rule itself.

### Related Rules

- [`rules/03-coding-standards.md`](../.claude/rules/03-coding-standards.md)
  — the constraint being narrowly exempted (≤ 4 parameters / use a parameter
  object).
- [`rules/19-backward-compatibility.md`](../.claude/rules/19-backward-compatibility.md)
  — the contract that dominates for `ExecutionState`; Rule 19's fallback
  matrix is the schema invariant the exemption exists to preserve.
- [`rules/20-telemetry-privacy.md`](../.claude/rules/20-telemetry-privacy.md)
  — the contract that dominates for `TelemetryEvent`; Rule 20's scrubber and
  fuzz corpus are the schema invariants the exemption exists to preserve.

## Migration Notes

Process for adding a new field to `ExecutionState`:

1. Confirm the field is additive and optional (default value, nullable, or
   `Optional<T>` at read time). Renames and removals are NOT permitted under
   Rule 19's fallback matrix.
2. Add the component to the record, keeping canonical position order stable
   (append; do not reorder).
3. Write a parse test that loads at least one committed legacy
   `execution-state.json` (v1) and at least one EPIC-0038-era file (v2) and
   asserts the record round-trips.
4. Run the `PlanningSchemaBackwardCompatSmokeTest` and ensure all
   `SCHEMA_VERSION_FALLBACK_*` log codes still fire on their matrix rows.
5. If the new field is a strongly-typed sub-structure, prefer an existing
   type (e.g., a neighbouring record that is NOT schema-bound) over a new
   nested record — nested records interact badly with `@JsonUnwrapped` and
   are the reason this ADR exists.

Process for adding a new field to `TelemetryEvent`:

1. Decide whether the field belongs on the top-level event or under `metadata`.
   If `metadata`, amend Rule 20 §3 (`ALLOWED_KEYS` table) in the same PR and
   update `MetadataWhitelist.ALLOWED_KEYS` — this ADR does NOT grant cover
   for skipping that step.
2. Add the top-level component to the record (append, stable order).
3. If the field can carry free-form strings, extend the scrubber scan-set
   (`TelemetryScrubber`) AND add representative hostile strings to the fuzz
   corpus (`java/src/test/resources/fixtures/telemetry/pii-corpus.txt`) in
   the same commit (Rule 20 §2).
4. Re-run `TelemetryScrubberTest`, `TelemetryScrubberFuzzTest`, and
   `PiiAuditSmokeIT`; all must stay green.
5. Verify that existing committed NDJSON under `plans/epic-*/telemetry/` still
   round-trips (missing fields default, no deserialization failure).

Any change that cannot satisfy these steps is outside the exemption and
requires either a new ADR or an actual record refactor.

## Story Reference

- audit-2026-04-17 finding M-008 (`ExecutionState`, 12 components)
- audit-2026-04-17 finding M-009 (`TelemetryEvent`, 16 components)

## Audit Reference

- `results/audits/codebase-audit-2026-04-17.md` — findings M-008, M-009
