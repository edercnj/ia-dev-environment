# Story Planning Report -- story-0039-0013

## Header

| Field | Value |
|-------|-------|
| Story ID | story-0039-0013 |
| Epic ID | 0039 |
| Date | 2026-04-15 |
| Schema | v1 (planningSchemaVersion absent) |
| Agents Participating | Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner |

## Planning Summary

Story adds an `--interactive` sub-modality to the already-existing `--dry-run` flag in `/x-release`. It pauses the release simulation before each of the 13 phases with a 3-option prompt (continue / skip / abort), writes a dummy state file under `/tmp`, and guarantees zero real side effects (no git/mvn/gh invocations). 12 tasks consolidated: 2 adapter.inbound (CLI guard), 6 application layer (executor + outcome enum + state port), 1 security verification, 1 docs (SKILL.md source), 1 smoke, 1 quality/acceptance gate.

## Architecture Assessment

**Layers affected:**
- `adapter.inbound` (picocli `ReleaseCommand`): adds `--interactive` option + validation that it requires `--dry-run`.
- `application` (`dev.iadev.release.dryrun.DryRunInteractiveExecutor`): new use case that loops over 13 phases, prompts per phase, records outcomes, writes/cleans dummy state.
- `domain` (`DryRunPhaseOutcome` enum, `DryRunSummary` value object): pure value types, zero framework imports.
- `adapter.outbound` (`DryRunStateWriter`): writes dummy JSON state to `/tmp` under POSIX 0600 permissions.
- `config` (SKILL.md source under `java/src/main/resources/targets/claude/skills/core/x-release/SKILL.md`): docs only.

**New components:**
- `DryRunInteractiveExecutor` (application use case)
- `DryRunPhaseOutcome` enum (domain): `SIMULATED`, `SKIPPED`, `ABORTED`, `NOT_REACHED`
- `DryRunSummary` value object (domain): phases counted by outcome, predicted command count
- `PromptPort`, `DryRunStatePort`, `PhaseCatalogPort` (domain ports — injected into executor)
- `DryRunStateWriter` (adapter.outbound)
- `DryRunSummaryFormatter` (application helper, <=25 lines)

**Dependency direction:** ReleaseCommand (inbound) -> DryRunInteractiveExecutor (application) -> ports (domain) <- outbound adapters. Domain imports nothing external.

**Integration points:** Reuses existing `ReleaseCommand` picocli entry (from EPIC-0035). Consumes phase list from shared `PhaseCatalogPort` (should already exist post story-0039-0007). PromptPort reuses AskUserQuestion plumbing introduced in story-0039-0007.

**Implementation order:** domain enum + value object -> ports -> application executor -> adapter (state writer) -> inbound CLI guard -> SKILL.md docs -> smoke.

## Test Strategy Summary

**Outer loop (acceptance — 5 tests, one per Gherkin scenario in §7):**
- AT-1: `--interactive` without `--dry-run` -> exit 1 with `INTERACTIVE_REQUIRES_DRYRUN` (TASK-001)
- AT-2: full 13-phase simulation with all "continuar" -> zero side effects, summary "13 / 13" (TASK-003, verified end-to-end in TASK-011)
- AT-3: "pular fase" on VALIDATED -> phase SKIPPED, BRANCHED still simulated (TASK-005)
- AT-4: "abortar" on PR_OPENED -> partial summary, `/tmp` state cleaned, exit 0 (TASK-007)
- AT-5: zero side effects acceptance (filesystem + git log inspection) — smoke TASK-011

**Inner loop (unit tests, TPP order):**
- nil (TASK-001): degenerate — missing `--dry-run` with `--interactive`
- constant (TASK-003): single path — all 13 "continuar"
- scalar (TASK-005): single branch — "pular fase" at one position
- conditional (TASK-007): multiple branches — "abortar" exits early + cleanup still runs
- iteration (TASK-011 smoke): edge case — full 13-phase loop with zero invocations asserted

**Coverage target:** line >=95%, branch >=90% (Rule 05). Covered by TASK-012.

**Test categories covered:** Unit (domain enum, outcome classification), Integration (executor + mocked ports), Smoke (TASK-011), Acceptance (all 5 Gherkin). No Performance / Contract / E2E required for this story.

## Security Assessment Summary

**OWASP Top 10 mapping:**
- **A01 Broken Access Control** — dummy state file created with POSIX 0600 permissions (owner-only); never placed outside `/tmp`.
- **A03 Injection** — JSON serialization via Jackson, no string concatenation; prompt responses validated against finite enum before branching.
- **A08 Software & Data Integrity Failures** — dummy state file prefixed/suffixed for clear diagnostic; atomic create via `Files.createTempFile`; guaranteed cleanup in `finally` under normal + exception + abort paths.

**Language-level anti-patterns avoided:** no `Math.random()` for temp file suffix (J2 — using `createTempFile`), no path concatenation (J6 — using atomic temp API), no hard-coded secrets (J4 — none needed), no exception-message leakage (J7 — errors surface via fixed error codes).

**Compliance:** project config has `compliance: none` (see Rule 01). No GDPR/LGPD/PCI mapping required — dummy state contains no PII (only phase names + outcomes + release version).

**Risk level:** LOW. New code has narrow blast radius: it literally CANNOT invoke side effects by construction (ports are fakes in this mode). Threat surface limited to the `/tmp` dummy file, mitigated by 0600 perms + guaranteed cleanup.

## Implementation Approach

Tech Lead decisions:

1. **Port-based isolation for zero-side-effects guarantee:** `DryRunInteractiveExecutor` receives `GitPort`, `MavenPort`, `GitHubPort` as injected dependencies. In `--dry-run --interactive` mode, the composition root wires NO-OP implementations. TASK-011 smoke asserts on the mocks that ZERO calls were made. This beats trying to "intercept" calls at runtime — it's impossible-by-construction.
2. **Enum for outcome, not strings:** `DryRunPhaseOutcome` enum (`SIMULATED`, `SKIPPED`, `ABORTED`, `NOT_REACHED`). Avoids magic strings (Rule 03 anti-pattern).
3. **`Files.createTempFile` over manual path building:** prevents CWE-22 by construction. Explicit `PosixFilePermissions.fromString("rw-------")` (0600).
4. **Prompt responses validated at the boundary:** `PromptPort.askUserQuestion(...)` returns a strongly-typed `PromptResponse` enum (`CONTINUAR`, `PULAR_FASE`, `ABORTAR`). No stringly-typed switch in executor.
5. **SKILL.md edits under generator source only:** RULE-001 + memory record enforce this. TASK-010 exclusively touches `java/src/main/resources/targets/claude/skills/core/x-release/SKILL.md`; `.claude/` is regenerated by `mvn process-resources` + `GoldenFileRegenerator` (cross-ref CLAUDE.md memory entry).
6. **Non-interactive CI fallback (RULE-004 of the epic):** add `--no-prompt` flag in the SKILL.md docs (TASK-010) as the CI-compatible form. Executor uses `PromptPort` abstraction, so a scripted responder can drive it headlessly (verified by TASK-011 smoke).

**Quality gates (TASK-012):**
- Line coverage >=95%, branch >=90% on `dev.iadev.release.dryrun` package.
- All 5 Gherkin scenarios map 1-to-1 to passing tests.
- Error code string `INTERACTIVE_REQUIRES_DRYRUN` present in 3 locations (ReleaseCommand, SKILL.md error catalog, test) — cross-file consistency check.
- No method >25 lines in executor (extract `handlePromptResponse`, `recordOutcome`, `writeDummyState`, `cleanupDummyState`, `emitSummary`).

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 12 |
| Architecture tasks | 1 (TASK-010 SKILL.md) |
| Test tasks (RED) | 4 (TASK-001/003/005/007) |
| Implementation tasks (GREEN) | 4 (TASK-002/004/006/008) |
| Security tasks | 1 (TASK-009) |
| Quality gate tasks | 1 (half of TASK-012) |
| Validation tasks | 1 (half of TASK-012) |
| Smoke tasks | 1 (TASK-011) |
| Merged tasks | 5 (TASK-002, TASK-004, TASK-006, TASK-008, TASK-012 — all merge ARCH+QA or TL+PO) |
| Augmented tasks | 1 (TASK-004 augmented with SEC criteria from TASK-009 inline) |

## Consolidated Risk Matrix

| Risk | Source Agent | Severity | Likelihood | Mitigation |
|------|------------|----------|------------|------------|
| Real side effect leaks via mis-wiring of Git/Maven/GitHub ports | SEC | HIGH | MEDIUM | Composition root has dedicated `DryRunWiring` that injects NO-OP adapters; smoke TASK-011 asserts zero calls on mocks; architecture test asserts executor never statically references concrete adapter classes |
| Dummy state file leaks out of `/tmp` or persists after exception | SEC | MEDIUM | LOW | `Files.createTempFile` with prefix `release-state-dryrun-`; cleanup in `finally`; POSIX 0600 perms; smoke asserts file absent post-run |
| SKILL.md edit bypasses generator (direct `.claude/` edit) | ARCH | MEDIUM | MEDIUM | RULE-001 enforced; TASK-010 scope restricted to `java/src/main/resources/...`; memory record `project_rule_numbering_reserved_slots.md` + CLAUDE.md reinforce; golden regen gated by `mvn process-resources` |
| Prompt TTY dependency breaks CI | TL | MEDIUM | MEDIUM | `PromptPort` abstraction; `--no-prompt` flag documented; smoke TASK-011 uses scripted responder (no real TTY) |
| Phase catalog drift between real release and dry-run simulation | TL | LOW | LOW | Both consume `PhaseCatalogPort`; single source of truth for 13 phases; architecture test asserts real + dry-run executors share the catalog |
| Operator skips critical validation phases (VALIDATED) during dry-run and misinterprets result | PO | LOW | MEDIUM | Summary output exhibits `Fases puladas: N (<phase names>)` so operator sees SKIPPED phases; final banner "MODO DRY-RUN — nenhum efeito colateral foi aplicado" reduces false confidence |

## DoR Status

**Verdict:** READY

All 10 mandatory checks PASS; 2 conditional checks marked N/A (compliance: none; contract_tests: false per project identity). See `dor-story-0039-0013.md` for full checklist.
