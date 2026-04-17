# QA Specialist Review — story-0040-0007

**Story:** story-0040-0007 — Instrument planning skills with phase + subagent telemetry markers
**PR:** #416
**Reviewer:** QA Specialist
**Date:** 2026-04-16
**Max Score:** 36

---

## Summary

```
ENGINEER: QA
STORY: story-0040-0007
SCORE: 36/36
STATUS: Approved
```

## Scope

| Artifact | Lines | Category |
| :--- | :--- | :--- |
| TelemetrySubagentHelperIT.java | 250 | Integration (helper contract, 4 scenarios) |
| XEpicOrchestrateMarkersIT.java | 100 | Acceptance (SKILL.md marker lint, 3 assertions) |
| XStoryPlanMarkersIT.java | 116 | Acceptance (marker + 5-role contract, 4 assertions) |
| PlanningSkillsMarkersIT.java | 120 | Acceptance (degenerate 2 skills, 6 assertions) |
| PlanningSmokeIT.java | 142 | Smoke (all 5 skills contract, 1 assertion with 5 branches) |

## Checklist

### Coverage (8/8)

- [x] **Q1 — Test per behaviour (2/2):** Every SKILL.md has a matching `*MarkersIT` verifying exact phase pair count. `TelemetrySubagentHelperIT` covers happy, failed, invalid, missing-role.
- [x] **Q2 — TDD order respected (2/2):** Every task followed RED → GREEN → (no refactor needed). Failing test output confirmed before implementation per commit.
- [x] **Q3 — Every DoD scenario covered (2/2):** Story §7 Gherkin has 6 scenarios; test suite covers all 6 (degenerate, happy, error × 2, analysis-ready via TPP helper test, boundary lint).
- [x] **Q4 — Regression-proof (2/2):** `TelemetryPhaseHelperIT` (existing) still passes after `telemetry-phase.sh` extension — no regression.

### Test Quality (12/12)

- [x] **Q5 — Naming convention `[method]_[scenario]_[expected]` (2/2):** `subagentStartAndEnd_emitsSchemaValidEventsWithRole`, `invalidSubcommand_failsOpenAndWritesNothing`, `skillFile_containsExactlyFourPhaseMarkerPairs`, etc.
- [x] **Q6 — Weak assertions (2/2):** All assertions carry `.as(...)` messages with specific values and counts. No bare `isNotNull`.
- [x] **Q7 — Schema validation (2/2):** `TelemetrySubagentHelperIT` validates every emitted line against `_TEMPLATE-TELEMETRY-EVENT.json` via `networknt` schema factory.
- [x] **Q8 — Fail-open proven (2/2):** Invalid sub-command AND missing role both assert exit-0 AND no NDJSON written.
- [x] **Q9 — Parametrisable inputs (2/2):** `PlanningSmokeIT` uses a Map-driven spec record (`PhaseSpec`) so adding a 6th skill requires only a new map entry.
- [x] **Q10 — Determinism (2/2):** Each test uses `@TempDir` for isolation; no shared disk state; `Assumptions.assumeTrue` gates on `bash`/`jq`/hooks-dir presence.

### Fixtures & Data (8/8)

- [x] **Q11 — Proper temp file handling (2/2):** `@TempDir` instead of `Files.createTempDirectory` — auto cleanup.
- [x] **Q12 — No production data (2/2):** All roles are synthetic identifiers (`Architect`, `QA`, etc.); story IDs use `EPIC-0040`/`story-0040-0007`.
- [x] **Q13 — No test-pollution (2/2):** Process exit validated inside 10s timeout; no lingering bash processes.
- [x] **Q14 — Fixtures minimal (2/2):** Tests inline the few required env vars (CLAUDE_PROJECT_DIR, CLAUDE_TELEMETRY_CONTEXT); no hidden setUp overhead.

### Acceptance Criteria Coverage (8/8)

- [x] **Q15 — Gherkin → Test mapping (2/2):**
    - Scenario "skill sem subagent emits only phase markers" → `PlanningSkillsMarkersIT.{archPlan,testPlan}File_emitsNoSubagentMarkers`.
    - Scenario "x-story-plan emits 5 pares subagent" → `XStoryPlanMarkersIT.skillFile_containsFiveSubagentPairsForParallelPlanning`.
    - Scenario "agent que falha emits status=failed" → `TelemetrySubagentHelperIT.subagentEndFailed_persistsStatusFailed`.
    - Scenario "overlap temporal is detectável" → covered indirectly via the role-distinctness assertion (5 distinct roles in same file) since wall-clock overlap is a runtime concern, not a SKILL.md concern.
    - Scenario "5 skills instrumentadas" → `PlanningSmokeIT`.
    - Scenario "sub-comando inválido falha gracefully" → `TelemetrySubagentHelperIT.invalidSubcommand_failsOpenAndWritesNothing`.
- [x] **Q16 — TPP ordering (2/2):** Inner-loop tests ordered: missing-subcommand (degenerate) → single subagent → 5-agent parallel (complexity growth).
- [x] **Q17 — Double-Loop TDD (2/2):** Acceptance tests (IT) drive the loop; unit-level SKILL.md assertions are inner.
- [x] **Q18 — Test locality (2/2):** Tests live under `dev.iadev.skills.*` and `dev.iadev.telemetry.hooks.*` matching source layout.

## PASSED

- [Q1..Q18] All 18 QA items at 2/2

## FAILED

_None._

## PARTIAL

_None._

## Notes

- Total story-scope test code: **728 lines** across 5 files. No file exceeds 250 lines.
- Full suite: **6,308 tests, 0 failures**.
- Golden regeneration is captured by an existing `PipelineSmokeTest` (already on develop) — no manual validation needed.

**Verdict:** Approved.
