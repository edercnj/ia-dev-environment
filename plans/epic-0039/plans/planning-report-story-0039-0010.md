# Story Planning Report — story-0039-0010

## Header

| Field | Value |
|-------|-------|
| Story ID | story-0039-0010 |
| Epic ID | 0039 |
| Date | 2026-04-15 |
| Agents Participating | Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner |
| Schema | v1 (planningSchemaVersion absent in execution-state.json → SCHEMA_VERSION_FALLBACK_MISSING_FIELD) |

## Planning Summary

Story adds two operational sub-commands to `/x-release`: `--status` (read-only introspection) and `--abort` (safe cleanup with double-confirm). Scope is tightly bounded: two application-layer components (`StatusReporter`, `AbortOrchestrator`), SKILL.md documentation updates to the source-of-truth under `java/src/main/resources/targets/claude/skills/core/x-release/`, and one abort-lifecycle smoke test. Dependency on story-0039-0002 (state file v2) is a hard blocker — `--status` rendering and `--abort` resource enumeration both consume v2 state semantics. Plan produces 14 tasks organized in a strict RED→GREEN→REFACTOR→VERIFY flow with TPP ordering from nil → iteration.

## Architecture Assessment

Two new classes under `java/src/main/java/dev/iadev/release/`:

- `status/StatusReporter` — pure read-only application service. Depends on state-file parser (from story-0039-0002), outputs formatted multi-line text per §5.3. No filesystem writes, no network calls, no CLI spawning. Fits Application layer (orchestrates domain parsing but owns no business rule).
- `abort/AbortOrchestrator` — mutational Application service. Orchestrates: (1) enumerate resources (PR via `gh pr list`, branches via `git branch -l`, state file), (2) render dry-run report, (3) request double confirmation via `ConfirmationPort`, (4) execute cleanup with per-step try/catch (warn-only semantics). Depends on outbound ports for `gh`, `git`, `Filesystem`, and an inbound `ConfirmationPort` (AskUserQuestion in production, fake in tests).

SKILL.md under `x-release` extends Triggers/Parameters sections and error catalog. No new adapter.inbound or adapter.outbound classes beyond ports (existing `GhAdapter`, `GitAdapter`, `FilesystemAdapter` from predecessor stories are reused). Dependency direction inward-only: application → ports → adapters. Domain is not touched.

## Test Strategy Summary

Double-Loop TDD:

- **Outer loop (AT):** 8 acceptance tests mapped 1:1 from §7 Gherkin scenarios. Remain RED until all UTs for `StatusReporter` + `AbortOrchestrator` close.
- **Inner loop (UT) — TPP order:**
  - nil (TASK-001/002): status without state
  - constant (TASK-003/004): status with valid state v2
  - scalar (TASK-005/006): status with corrupted JSON (STATUS_PARSE_FAILED)
  - collection (TASK-007/008): abort happy-path, dry-run listing, cancel on first confirm
  - conditional (TASK-009/010): `--yes`/`--force` bypass; gh/branch cleanup warn-only
  - iteration (TASK-013): smoke lifecycle fixture

Estimated coverage: ≥ 95% line / ≥ 90% branch. All 8 Gherkin scenarios have at least one automated test.

## Security Assessment Summary

OWASP categories applicable:

- **A01 Broken Access Control** — N/A (no auth surface; operator-local CLI).
- **A03 Injection** — state file path canonicalized (Rule 06 + J6 path traversal); CLI flag values are positional-only flags (no user-supplied path args).
- **A05 Security Misconfiguration** — double-confirm must remain ON by default; `--yes`/`--force` logs explicit "FORCE MODE" warning (audit trail).
- **A09 Security Logging & Monitoring** — error messages must not leak internal paths or stack traces (J7). PR URLs are public; safe to log. gh CLI output may include auth hints — filter.
- **CWE-209** — exception messages from gh/git failures MUST be summarized (warn-only code emitted), not printed raw.
- **Secret management** — no new credentials introduced. Do not log `GH_TOKEN` or any env var.

Controls verified in TASK-012 (SEC-VERIFY).

## Implementation Approach

Chosen by Tech Lead over Architect alternative:

- **Chosen:** Introduce `ConfirmationPort` interface in `domain.port` (or `application.port`) so AbortOrchestrator is testable without TTY. Production wiring uses AskUserQuestion; integration/smoke use a deterministic fake. This keeps AskUserQuestion out of application code directly, honors DIP, and enables the 14 tests to run headless.
- **Considered alternative (recorded):** inline AskUserQuestion calls in AbortOrchestrator with a `--yes` gate wrapping the whole method. Rejected: couples application to framework, reduces testability, violates DIP.

Quality gates (TL-enforced in TASK-011 and TASK-014):

- Methods ≤ 25 lines; classes ≤ 250 lines.
- No train wrecks; no boolean flag parameters (use enum for `BypassMode.INTERACTIVE/FORCE`).
- Per-step try/catch returns a `CleanupResult` aggregate rather than exceptions propagating.
- Error code registry in SKILL.md enumerates STATUS_PARSE_FAILED, ABORT_NO_RELEASE, ABORT_USER_CANCELLED, ABORT_PR_CLOSE_FAILED, ABORT_BRANCH_DELETE_FAILED.

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 14 |
| Architecture tasks | 4 (merged into GREEN) |
| Test tasks | 7 (4 RED + 3 paired GREEN-verify + 1 smoke) |
| Security tasks | 1 VERIFY (augmented 3 GREENs with SEC criteria) |
| Quality gate tasks | 2 (REFACTOR, final VERIFY) |
| Validation tasks | 1 (PO merged into TASK-014) |
| Merged tasks | 5 |
| Augmented tasks | 3 (TASK-006, TASK-008, TASK-014 carry SEC criteria) |

## Consolidated Risk Matrix

| Risk | Source Agent | Severity | Likelihood | Mitigation |
|------|------------|----------|------------|------------|
| AskUserQuestion blocks headless tests | TL | High | High | Introduce ConfirmationPort; fakes for IT/smoke |
| gh CLI not installed / unauthenticated in smoke | QA | Medium | Medium | Stub gh via PATH override in fixture; document in SKILL.md |
| Double-confirm UX annoys operator, pressure to remove | PO | Medium | Medium | RULE-010 is hard gate; `--yes`/`--force` is the only bypass |
| `--force` used in CI silently wipes releases | SEC | High | Low | Explicit "FORCE MODE" warn log; document in SKILL.md with bold warning |
| Branch delete failure leaves orphan | TL | Medium | Medium | Warn-only per story §3.2; `--status` next run surfaces it |
| State v2 schema drift from story-0039-0002 | ARCH | High | Low | Hard DoR: story-0039-0002 merged before impl; reuse its parser directly |
| Path traversal via crafted state file name | SEC | Low | Low | State file path is fixed (`plans/release-state-X.Y.Z.json`); canonicalize + prefix check (Rule 06) |

## DoR Status

READY — see `dor-story-0039-0010.md` for per-check breakdown.
