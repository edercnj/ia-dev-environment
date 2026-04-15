# Story Planning Report -- story-0039-0007

## Header

| Field | Value |
|-------|-------|
| Story ID | story-0039-0007 |
| Epic ID | 0039 |
| Date | 2026-04-15 |
| Agents Participating | Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner |
| Schema | v1 (planningSchemaVersion absent -> FALLBACK_MISSING_FIELD) |

## Planning Summary

Story introduces `PromptEngine` (application layer) that wraps `AskUserQuestion` at three halt points in `/x-release` (APPROVAL_GATE, BACK-MERGE-DEVELOP, recoverable failures), persisting `waitingFor` / `nextActions` / `lastPromptAnsweredAt` to the state file v2. Non-interactive equivalence is preserved via `--no-prompt` (RULE-004). Plan has 14 tasks across 7 TDD RED->GREEN pairs, 1 security task, 2 documentation tasks, 1 smoke test, 2 quality gates, and 1 PO validation. Dependency on story-0039-0002 (state v2 schema) explicitly noted.

## Architecture Assessment

- **Affected layers:** application (PromptEngine, HaltPoint enum, PromptResult), cross-cutting (SKILL.md + reference doc).
- **New ports needed:** `AskUserQuestionPort` (outbound, abstracts Claude Code's AskUserQuestion tool), `ClockPort` (for lastPromptAnsweredAt), `StateFilePort` (already exists from story-0039-0002 — reused).
- **Dependency direction:** PromptEngine -> ports only; zero framework imports; zero Claude Code SDK imports in domain/application (ports isolate).
- **Integration points:** consumed by `/x-release` SKILL.md Phase 8 and Phase 10 via documented bash blocks (RULE-001 respected — edits go to generator source, not `.claude/`).
- **Implementation order:** Port interfaces + HaltPoint enum -> PromptEngine core -> state persistence -> recoverable failure dispatch -> SKILL.md integration -> reference doc -> smoke.
- **No mini-ADR required:** design stays within existing port/adapter pattern established in EPIC-0035.

## Test Strategy Summary

- **Outer loop (Acceptance Tests):** 5 Gherkin scenarios from Section 7 — AT-1 (degenerate `--no-prompt`), AT-2 (happy continue), AT-3 (boundary exit+resume), AT-4 (retry after failure), AT-5 (abort exit=2). Each maps to one or more RED/GREEN pairs + smoke.
- **Inner loop (TPP ordering):** TASK-001 (nil: `--no-prompt` degenerate) -> TASK-003 (scalar: happy path continue) -> TASK-005 (collection: state persistence) -> TASK-007 (conditional: retry/skip/abort branches).
- **Categories covered:** unit (TASK-001..008), smoke (TASK-012). No integration test required — all external interactions isolated behind ports; real AskUserQuestion invocation validated only in smoke via test double.
- **Estimated coverage after GREENs complete:** >=95% line, >=90% branch (Rule 05 gate wired in TASK-013).

## Security Assessment Summary

- **OWASP mapping:** A03 (Injection — rejected invalid responses to prompt), A09 (Logging — no user input echo), A04 (Insecure Design — `--no-prompt` must preserve non-interactive path for CI).
- **Controls:** input validation (PROMPT_INVALID_RESPONSE on unexpected); non-TTY environment fallback (no hang); no secrets in prompt options or state file fields added.
- **Risk level:** LOW. No authN/authZ change, no external network call beyond existing gh CLI (unchanged), no new secrets.
- **Compliance:** none applicable (project compliance field = none per project identity).

## Implementation Approach

Tech Lead selects the port-based approach proposed by Architect over any in-place AskUserQuestion invocation. Rationale: keeps application layer pure (zero Claude Code SDK coupling), allows smoke test to mock prompts via test double, aligns with the `StateFilePort` pattern already in use. Quality gates: method length <=25, class length <=250, coverage >=95/90, error codes follow existing `PROMPT_*` convention, constructor-only dependency injection (Rule 03). RULE-001 enforced (SKILL.md edits at `java/src/main/resources/targets/claude/skills/core/x-release/SKILL.md`, never `.claude/`).

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 14 |
| Architecture tasks | 2 (TASK-010, TASK-011) |
| Test tasks | 4 RED + 1 smoke (TASK-001, 003, 005, 007, 012) |
| Implementation tasks | 4 GREEN (TASK-002, 004, 006, 008) |
| Security tasks | 1 (TASK-009) |
| Quality gate tasks | 1 (TASK-013) |
| Validation tasks | 1 (TASK-014) |
| Merged tasks | 4 (ARCH+QA on GREEN pairs: TASK-002, 004, 006, 008) |
| Augmented tasks | 4 (SEC criteria folded into each GREEN impl task via Rule 2) |

## Consolidated Risk Matrix

| Risk | Source Agent | Severity | Likelihood | Mitigation |
|------|------------|----------|------------|------------|
| Non-TTY CI hangs waiting for AskUserQuestion | SEC | HIGH | MEDIUM | TASK-009: System.console() probe; fall through to textual instruction path identical to `--no-prompt`; covered by TASK-001 test |
| state v2 schema not yet merged (dep on story-0039-0002) | ARCH | MEDIUM | LOW | Escalation note on TASK-006; DoR blocker documented; do not reintroduce v1 shape (RULE-003) |
| Silent v1 state upgrade | SEC | HIGH | LOW | TASK-005 asserts NO silent upgrade; RULE-003 enforced; `STATE_SCHEMA_VERSION` abort unchanged |
| SKILL.md edits applied to `.claude/` directly (RULE-001 violation) | TL | MEDIUM | LOW | TASK-013 explicit cross-file check; edits only at generator source |
| Orphaned state file after exit | QA | MEDIUM | MEDIUM | TASK-012 smoke asserts state is consistently resumable across exit/reinvoke |
| `lastPromptAnsweredAt` uses system clock (non-determinism in tests) | TL | LOW | HIGH | ClockPort injected; tests use fixed-clock double |

## DoR Status

READY. All 10 mandatory checks pass; conditional checks (#11 compliance, #12 contract tests) N/A.
