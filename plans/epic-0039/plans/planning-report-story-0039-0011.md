# Story Planning Report -- story-0039-0011

## Header

| Field | Value |
|-------|-------|
| Story ID | story-0039-0011 |
| Epic ID | 0039 |
| Date | 2026-04-15 |
| Agents Participating | Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner |
| Schema | v1 (planningSchemaVersion absent -> FALLBACK_MISSING_FIELD) |

## Planning Summary

Story-0039-0011 implements the handoff loop integrating `/x-pr-fix-comments` inside the `x-release` interactive prompt flow (Phase 8 APPROVAL_GATE) delivered by story-0039-0007. The multi-agent analysis produced 12 consolidated tasks covering: a new `HandoffOrchestrator` application component, a decision-table-driven prompt re-render logic keyed on `(state, mergedAt, reviewDecision)`, hardened `gh` CLI invocation, SKILL.md + `references/prompt-flow.md` documentation of the Handoff Contract, a smoke test simulating the full loop with mocked skill-invocation and `gh` ports, and verification gates (coverage, Gherkin coverage, security input hardening). Architecture fits cleanly in the `application` layer with two outbound ports (SkillInvokerPort, GhCliPort). Non-functional requirements (UX coherence metric ≥80% via-handoff usage) are tracked in Section 5 of the story and validated in TASK-011.

## Architecture Assessment

**Affected layers:** application (new), adapter.outbound (extend `GhCliPort` invocation; add `SkillInvokerPort`), config (SKILL.md docs).

**New components:**
- `dev.iadev.release.handoff.HandoffOrchestrator` (application) — orchestrates: invoke fix-comments skill -> wait -> re-read PR -> re-render prompt.
- `dev.iadev.release.handoff.PrState` (value object, domain) — record{state: enum OPEN/CLOSED/MERGED, mergedAt: Optional<Instant>, reviewDecision: enum}.
- `dev.iadev.release.handoff.HandoffError` (enum) — `HANDOFF_SKILL_FAILED`, `HANDOFF_PR_NOT_FOUND`.
- `dev.iadev.release.port.SkillInvokerPort` (outbound port) — `invoke(skillName: String, args: String): Result<Void, SkillError>`.
- `dev.iadev.release.port.GhCliPort` (outbound port, extend) — `viewPr(int prNumber): Result<PrState, GhError>`.

**Modified components:**
- `SKILL.md` (Phase 8 block) + `references/prompt-flow.md` (add Handoff Contract section) in `java/src/main/resources/targets/claude/skills/core/x-release/`.
- Integration wiring with the `PromptEngine` from story-0039-0007 (HandoffOrchestrator invoked from the `APPROVAL_GATE` halt point when operator selects "Rodar fix-comments").

**Dependency direction validation:** All new code lives in `application` and depends on `domain.port` interfaces only. Adapters for `gh` CLI and Skill tool remain in `adapter.outbound`. Zero framework imports in domain/application layers (Rule 04).

**Integration points:**
- PromptEngine (story-0039-0007) -> HandoffOrchestrator (this story).
- HandoffOrchestrator -> SkillInvokerPort -> (adapter wraps `Skill(...)` tool call).
- HandoffOrchestrator -> GhCliPort -> ProcessBuilder-based `gh` client.

**Implementation order:** PrState (value object) -> HandoffError (enum) -> HandoffOrchestrator skeleton (TASK-001/002) -> decision table resolveOptions (TASK-003/004) -> error handling (TASK-005/006) -> docs (TASK-008) -> smoke (TASK-009) -> verification (TASK-010/011/012).

## Test Strategy Summary

**Double-Loop TDD:**
- **Outer loop (Acceptance Tests):** 5 Gherkin scenarios in Section 7 (happy, boundary mergeado, degenerate closed, error skill fail, error PR not found). Each maps 1:1 to a test class method in `HandoffLoopSmokeTest` (TASK-009).
- **Inner loop (Unit Tests, TPP order):**
  - TPP Level 1 (nil/degenerate): TASK-001 happy path minimal invocation (null state handling).
  - TPP Level 2 (constant): TASK-002 GREEN for minimal HandoffOrchestrator contract.
  - TPP Level 3 (scalar): TASK-003/004 boundary PR mergeado -> single state transition.
  - TPP Level 4 (collection): TASK-005/006 error paths (CLOSED, skill fail, 404) -> multiple decision table branches.

**Estimated coverage:** line ≥95%, branch ≥90% per Rule 05 (verified in TASK-010).

**AT-N entries:**
- AT-1: Handoff básico happy path (Section 7 Gherkin 1)
- AT-2: PR mergeado durante handoff (Section 7 Gherkin 2)
- AT-3: PR fechado durante handoff (Section 7 Gherkin 3)
- AT-4: Skill tool retorna erro (Section 7 Gherkin 4)
- AT-5: PR removido (Section 7 Gherkin 5)

**UT-N entries in TPP order:** TASK-001 (nil) -> TASK-003 (scalar) -> TASK-005 (collection/conditional).

## Security Assessment Summary

**OWASP Top 10 mapping:**
- **A03 Injection (CWE-78 OS Command Injection):** `gh` CLI invoked via ProcessBuilder with explicit argv (NEVER shell string concatenation). TASK-012 verifies.
- **A04 Insecure Design:** error messages MUST NOT expose internal paths / stack traces to the operator; exceptions carry prNumber context but sanitized messages only. TASK-007 verifies.
- **A05 Security Misconfiguration:** no hardcoded tokens; `gh` CLI uses its own credential chain; ProcessBuilder.inheritIO() is forbidden (no env var leak). TASK-007, TASK-012.
- **A09 Security Logging and Monitoring Failures:** no PR body content or user-controlled input logged verbatim; stderr from `gh` sanitized before logging (no token leak). TASK-007, TASK-012.

**Input validation:**
- `prNumber` validated as positive integer; rejects negative/zero/non-numeric -> `IllegalArgumentException(prNumber context)`.
- `gh pr view` output parsed with strict schema (enum for `state`, ISO-8601 for `mergedAt`); unknown values rejected -> `HANDOFF_PR_STATE_INVALID` (warn -> exit 1 if repeated).

**Secrets management:** No new credentials/keys. `gh` CLI token remains in user's local credential store — never read/passed by this code.

**Dependency security:** No new third-party libraries introduced. `gh` CLI is an existing project dependency. ProcessBuilder is JDK standard library.

**Risk level:** LOW. The component is glue code between two existing subsystems (PromptEngine + gh CLI) with well-defined I/O and no untrusted external input beyond the operator's own PR number.

## Implementation Approach

**Chosen approach:** lightweight application-layer orchestrator with two outbound ports. Favors the existing hexagonal pattern used in story-0039-0007 (PromptEngine + StateFilePort + ClockPort).

**Considered alternative:** embed handoff logic directly in PromptEngine. Rejected: violates Rule 03 (SRP) — PromptEngine already owns prompt rendering and halt-point resolution; adding skill invocation would push it past 250 lines and mix two responsibilities.

**Quality gates (TL):**
- Coverage thresholds per Rule 05 (≥95% line, ≥90% branch).
- Method length ≤25 lines (Rule 03); class length ≤250 lines.
- All port fields `final` (constructor injection, Rule 03).
- Error codes follow existing `HANDOFF_*` naming convention (consistent with `PROMPT_*` from story-0039-0007).
- No train-wreck depth >2 levels (Rule 03).
- RULE-001 respected: SKILL.md edits target `java/src/main/resources/targets/claude/` (generator source), NOT `.claude/` (generated output).

**TDD compliance:** test-first pattern strictly enforced. Every GREEN task depends on its paired RED task (TASK-001 -> TASK-002, TASK-003 -> TASK-004, TASK-005 -> TASK-006).

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 12 |
| Architecture tasks | 1 (TASK-008 docs) |
| Implementation tasks (GREEN) | 3 (TASK-002, TASK-004, TASK-006) |
| Test tasks (RED) | 3 (TASK-001, TASK-003, TASK-005) |
| Security tasks | 2 (TASK-007, TASK-012) |
| Quality gate tasks | 1 (TASK-010) |
| Validation tasks | 1 (TASK-011) |
| Smoke test tasks | 1 (TASK-009) |
| Merged tasks | 3 (TASK-002, TASK-004, TASK-006 merge ARCH+QA) |
| Augmented tasks | 1 (TASK-002 augmented with SEC DoD from TASK-007) |

## Consolidated Risk Matrix

| Risk | Source Agent | Severity | Likelihood | Mitigation |
|------|-------------|----------|------------|------------|
| `/x-pr-fix-comments` contract drift during handoff (skill evolution breaks handoff expectations) | ARCH | MEDIUM | MEDIUM | DoR check: "Contrato de retorno de `/x-pr-fix-comments` validado" (Section 4); document Handoff Contract verbatim in `references/prompt-flow.md` (TASK-008) |
| PR state changes (close/merge/reopen) during handoff by external actor -> stale prompt state | QA | MEDIUM | HIGH | gh pr view ALWAYS re-executed post-return (TASK-002); decision table covers CLOSED/MERGED/OPEN×reviewDecision exhaustively (TASK-004); Gherkin 3 covers PR CLOSED path |
| OS command injection via malformed PR number | SEC | HIGH | LOW | prNumber validated as positive integer (TASK-007); ProcessBuilder with explicit argv (TASK-012) |
| gh CLI timeout / hang (network issue, gh auth expired) | SEC/ARCH | MEDIUM | MEDIUM | Configurable timeout on ProcessBuilder (default 30s, TASK-012); stderr captured and surfaced to operator; HANDOFF_PR_NOT_FOUND exit 1 on 404 (TASK-006) |
| Sibling skill invocation pollutes main repo state (RULE violation — worktree discipline) | TL | LOW | LOW | Handoff is invoked INSIDE x-release's worktree by design; SkillInvokerPort delegates to the Skill tool which respects harness conventions; no additional worktree created by this skill (Rule 14) |
| CI/non-TTY execution lacking AskUserQuestion | SEC | MEDIUM | MEDIUM | `--no-prompt` mode documented in SKILL.md (TASK-008); HandoffOrchestrator falls back to textual instruction + exit 0 (inherited from PromptEngine contract, story-0039-0007) |
| Metric "≥80% via-handoff" not observable | PO | LOW | MEDIUM | TASK-011 notes tracking hook; implementation deferred unless observability infra added |

## DoR Status

**Verdict:** READY — all 10 mandatory checks passed; conditional checks N/A (compliance disabled, contract_tests disabled per project config). See `dor-story-0039-0011.md` for the per-check breakdown.
