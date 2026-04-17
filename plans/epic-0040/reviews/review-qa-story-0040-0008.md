# QA Specialist Review — story-0040-0008

ENGINEER: QA
STORY: story-0040-0008
PR: #418
SCORE: 34/36
STATUS: Approved

## Scope

Instrumentation story: adds `mcp-start` / `mcp-end` sub-commands to
`telemetry-phase.sh` and inserts phase / MCP markers into 5 creation
skills. All code changes are either shell-helper logic (covered by a
dedicated IT) or markdown annotations (covered by lint-based ITs).

## Checklist

PASSED:
- [QA-01] Test-first discipline: every task has a matching IT file committed
  in the same commit as its SKILL.md / helper change. Commits verifiable
  via `git show <task-sha> --stat`. (2/2)
- [QA-02] Test naming: all new tests follow
  `methodUnderTest_scenario_expectedBehavior`. Examples:
  `mcpStartAndEnd_emitsSchemaValidToolCallEventsWithDuration`,
  `xEpicCreate_containsExactlyFourPhaseMarkerPairs`,
  `xJiraCreateStories_wrapsTwoMcpCallSites`. (2/2)
- [QA-03] Happy + degenerate + error paths covered in
  `TelemetryMcpHelperIT` (happy with durationMs, mcp-end without prior
  start, missing mcpMethod fail-open). Matches §7 Gherkin ordering
  (degenerate -> happy -> error -> boundary). (2/2)
- [QA-04] Schema validation: every NDJSON event emitted by mcp-* is
  validated against `_TEMPLATE-TELEMETRY-EVENT.json` at runtime in
  `TelemetryMcpHelperIT`. (2/2)
- [QA-05] Assertions are specific: durationMs checked `> 0` after a
  deterministic 1.1s sleep (tolerates second-granularity BSD fallback);
  field absence checked via `.has(...)`; exact tool name assertions. No
  weak `isNotNull()` usage. (2/2)
- [QA-06] `CreationSkillsSmokeIT` proves the boundary scenario from §7
  (all 5 skills instrumented) with a single entry-point test class
  driven by a structured `SkillSpec` record. Future drift in any of the
  5 skills fails exactly one assertion with a clear message. (2/2)
- [QA-07] One-to-one mcp-start/mcp-end pairing asserted in smoke IT
  (§3.5 aggregation-ready property — precondition for durationMs P50/P95
  per tool). (2/2)
- [QA-08] Test isolation: MCP helper IT uses `@TempDir` and overrides
  `TMPDIR` so timer files never cross between tests. Uses unique
  `uniqueMethod` id (`"noStartMethod" + System.nanoTime()`) when
  testing the "missing timer" scenario. (2/2)
- [QA-09] Degenerate path: `mcpEndWithoutStart_emitsEventWithoutDuration`
  covers the §5.3 "Timer perdido" clause (event emitted without
  durationMs, fail-open). (2/2)
- [QA-10] All 5 SKILL.md files pass `TelemetryMarkerLint` — verified by
  5 `bothSkills_passTelemetryMarkerLint` / equivalent assertions across
  three IT classes. (2/2)
- [QA-11] Full regression: `mvn test` reports 6308/6308 tests passing
  after story-local changes + golden regeneration. (2/2)

PARTIAL:
- [QA-12] Test doubles: `TelemetryMcpHelperIT` exercises the real shell
  script end-to-end (no mocks), which is the correct choice for
  contract-level ITs but makes the test dependent on host `bash` + `jq`.
  Mitigated by `@Assumption` guards (`assumeBashAvailable`,
  `assumeJqAvailable`). Not a blocker; worth noting as a portability
  concern. (1/2)

## Findings Severity Distribution

| Severity | Count |
|----------|-------|
| Critical | 0 |
| High     | 0 |
| Medium   | 0 |
| Low      | 1 (QA-12 portability note — BSD vs GNU date granularity) |

## Summary

34/36 — Approved. The story is fully test-driven end-to-end. Helper
contract exercised via real process spawn; marker contracts enforced
via linter + dedicated ITs per skill; 5-skill boundary + aggregation
property covered by a single smoke IT. No critical or high findings.
