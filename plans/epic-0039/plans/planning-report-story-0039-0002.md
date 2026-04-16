# Story Planning Report — story-0039-0002

## Header

| Field | Value |
|-------|-------|
| Story ID | story-0039-0002 |
| Epic ID | 0039 |
| Date | 2026-04-15 |
| Schema Version | 1.0 (v1 legacy flow; EPIC-0039 execution-state has no planningSchemaVersion field) |
| Agents Participating | Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner |

## Planning Summary

Story decomposes a breaking state-file schema change (v1→v2) into 8 TDD-ordered tasks over 1 domain package (`dev.iadev.release.state`). Scope is narrow, fully self-contained, with zero external dependencies. Risk is LOW because:
- Breaking decision ratified up-front in story §3.2 (RULE-003).
- 6 Gherkin scenarios span all mandatory categories (degenerate, happy, boundary, error).
- Record/enum shape mechanical; validator logic <50 lines.

## Architecture Assessment

- **Layers affected**: domain only (state records, enum, validator). Cross-cutting: 1 doc file + 1 smoke test.
- **New components**:
  - `ReleaseState` (record, immutable, Jackson-annotated)
  - `NextAction` (record)
  - `WaitingFor` (enum, 6 values)
  - `StateFileValidator` (domain service)
- **Existing components modified**:
  - `ReleaseStateFileSchemaTest` — extend for v2 fields
  - `references/state-file-schema.md` — add "Schema v2 Fields" section
- **Dependency direction**: domain imports zero external libs (Rule 04). Jackson annotations are metadata-only — allowed. Enforced via architecture tests.
- **Implementation order**: domain records (TASK-002) → validator (TASK-004) → docs + smoke (TASK-006/007) → quality gates (TASK-008).

## Test Strategy Summary

- **Acceptance tests**: 6 Gherkin scenarios mapped 1:1 to test methods (AT-1..AT-6).
- **Unit tests (TPP order)**:
  - TPP-1 (nil/degenerate): schemaVersion=1 → rejection; null fields
  - TPP-2 (constant): enum WaitingFor values
  - TPP-3 (scalar): schemaVersion=2 happy path read
  - TPP-4 (collection): nextActions roundtrip (2 entries preserved)
  - TPP-5 (conditional): invalid enum, invalid command regex
  - TPP-6 (iteration/boundary): phaseDurations empty map
- **Smoke test**: StateFileRoundtripSmokeTest asserts full-field equality after serialize/deserialize.
- **Coverage target**: ≥95% line, ≥90% branch on `dev.iadev.release.state` package.

## Security Assessment Summary

- **OWASP mapping**:
  - **A03 Injection** (low risk): `nextActions[].command` is user-originated in downstream stories; enforced as allowlist regex `^/[a-z\-]+` in TASK-004. Mitigated.
  - **A04 Insecure Design** (low risk): breaking schema with explicit rejection is safer than silent migration; aligns with Rule 03 DRY/single-source-of-truth.
  - **A08 Data Integrity**: Jackson strict parser (no unknown fields) ensures schema conformance on read.
- **No PII, credentials, or secrets introduced**.
- **Error handling**: rejection message includes actionable instruction without leaking internal paths (Rule 06 J7 compliant).
- **Risk level**: LOW.

## Implementation Approach

Tech Lead endorses the story's authored task structure with these amendments:
1. Split TASK-0039-0002-001 (records) into RED (TASK-001) + GREEN (TASK-002) pair for TDD compliance (Rule 05).
2. Split TASK-0039-0002-002 (validator) into RED (TASK-003) + GREEN (TASK-004) + REFACTOR (TASK-005) trio.
3. Keep doc update (TASK-006) and smoke test (TASK-007) as originally authored.
4. Add quality-gate verification (TASK-008) to enforce DoD Local + coverage + Gherkin-to-test mapping.
5. `mvn process-resources` MUST run before `GoldenFileRegenerator` (project memory — `feedback_mvn_process_resources_before_regen.md`).

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 8 |
| Architecture tasks | 2 (TASK-002, TASK-005) |
| Test tasks | 3 (TASK-001, TASK-003, TASK-007) |
| Security tasks | 0 standalone (augmented into TASK-004) |
| Quality gate tasks | 1 (TASK-008) |
| Validation tasks | 1 (TASK-006 doc + TASK-008 PO verify) |
| Merged tasks | 3 (TASK-002, TASK-004, TASK-006, TASK-008 merged across 2+ agents) |
| Augmented tasks | 1 (TASK-004 injected with SEC criteria) |

## Consolidated Risk Matrix

| Risk | Source Agent | Severity | Likelihood | Mitigation |
|------|------------|----------|------------|------------|
| Existing state files v1 in active releases rejected post-merge | PO | HIGH | MEDIUM | Document migration via `/x-release --abort`; coordinated rollout after release with S02; decision ratified §3.2 |
| Golden file regen out-of-sync with records | Architect | MEDIUM | LOW | TASK-006 DoD requires `mvn process-resources` before regen (project memory) |
| `nextActions[].command` injection of privileged slash-command | Security | LOW | LOW | Allowlist regex `^/[a-z\-]+` enforced in TASK-004; downstream story (S07) executes command via whitelist, not shell |
| Jackson deserialization permissive mode | Security | LOW | LOW | Configure `FAIL_ON_UNKNOWN_PROPERTIES=true` on the ObjectMapper used for state read |
| Coverage gap on enum WaitingFor branches | QA | LOW | LOW | TASK-003 parametrized tests cover all 6 enum values |

## DoR Status

**READY** — All 10 mandatory checks pass; both conditional checks are N/A for this project (`compliance=none`, `contract_tests=false` per project identity).
