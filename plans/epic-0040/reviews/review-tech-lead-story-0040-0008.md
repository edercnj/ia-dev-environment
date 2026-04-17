# Tech Lead Review — story-0040-0008

**Story:** story-0040-0008 — Instrument creation skills (Epic/Story/Jira)
**PR:** #418
**Branch:** `feat/story-0040-0008-instrument-creation-skills`
**Base:** `develop`
**Reviewer:** Tech Lead (inline)
**Date:** 2026-04-16
**Template Version:** inline (pre-EPIC-0024 fallback pattern)

## Decision: GO — 43/45

## Test Execution Results (EPIC-0042)

| Check | Result | Detail |
|-------|--------|--------|
| Test Suite (`mvn test`) | PASS | 6308 tests, 0 failures, 0 errors, 0 skipped |
| Coverage | N/A for this scope | Story is instrumentation-only (shell + markdown). Java coverage unchanged; JaCoCo thresholds apply to `dev.iadev.*` — no new Java production code in this story beyond the test-only `TelemetryMarkerLint` extensions which are already exercised by `TelemetryMarkerLintTest` + the 3 new markers ITs. |
| Smoke Tests | PASS | `CreationSkillsSmokeIT` + `PlanningSmokeIT` + `HooksSmokeIT` all green |
| Golden Files | PASS | 33/33 (regenerated via `GoldenFileRegenerator` after source changes) |
| Lint (TelemetryMarkerLint) | PASS | 0 findings on all 5 modified SKILL.md files |

## 45-Point Rubric

### A. Code Hygiene — 8/8

- [A1] No unused imports in new test files. (2/2)
- [A2] No dead code / leftover `console.log` / `println`. (2/2)
- [A3] Zero compiler warnings on the modified surface
  (`TelemetryMarkerLint.java` untouched; new ITs compile clean). (2/2)
- [A4] No magic strings in test code — `SkillSpec` record in
  `CreationSkillsSmokeIT` centralizes the 5-skill expectations
  (DRY). (2/2)

### B. Naming — 4/4

- [B1] `mcp-start` / `mcp-end` / `mcpMethod` / `MCP_TIMER_FILE` /
  `MCP_DURATION_MS` — all intention-revealing, consistent with
  project's kebab-case helper naming and SHELL-caps-for-vars
  convention. (2/2)
- [B2] Test method names follow
  `methodUnderTest_scenario_expectedBehavior` across all new
  ITs. (2/2)

### C. Functions — 5/5

- Shell script additions keep individual blocks under 25 lines.
- Each case-branch (`mcp-start` / `mcp-end`) is cohesive and
  single-purpose.
- Timer logic is isolated in a dedicated block with clear comments
  referencing §5.1 / §5.3 of the story contract.
- Test helpers (`runPhaseHelper`, `runHelperProcess`,
  `countLinesContaining`) are small (<15 lines each) with ≤4
  params.

### D. Vertical Formatting — 4/4

- Section comments demarcate concerns (`MCP timer handling`,
  `Argument validation`).
- No class/script exceeds 250 lines after the addition
  (telemetry-phase.sh grows from 229 -> ~300 lines, still
  well-structured).

### E. Design — 3/3

- Law of Demeter: helper never reaches across object boundaries.
- CQS: `mcp-start` is a command (side-effect on timer file),
  `mcp-end` is command+query (reads timer, writes event).
- DRY: `MCP_TIMER_FILE` computation is consolidated; the
  second-granularity fallback for `date +%s%3N` is a single
  shared snippet used in both start and end blocks.

### F. Error Handling — 3/3

- Fail-open contract preserved throughout. Every new failure
  path `echo` to stderr and `exit 0` (or continues with empty
  MCP_DURATION_MS so the event still emits).
- No generic catches introduced in Java test code (all assertions
  are specific with contextual messages).

### G. Architecture — 4/5

- [G1] Layer boundary respect: shell helper stays in
  `hooks/` directory (adapter boundary). Skills (markdown
  prompts) stay in the prompt source-of-truth. (2/2)
- [G2] Test layer placement is correct (`dev.iadev.skills.*` for
  skill contract ITs; `dev.iadev.telemetry.hooks.*` for the shell
  helper IT). (2/2)
- [G3] **Minor deduction:** The mcp-* markers are OPT-IN annotations
  the skill author must apply manually. There is no mechanical
  guarantee that a future MCP call site will carry markers — it
  relies on reviewer vigilance. `CreationSkillsSmokeIT` covers
  today's five skills; a future skill that adds a Jira MCP call
  would need to be added to the smoke registry manually. Tracked
  as a future hardening opportunity (lint could grep every
  `mcp__atlassian__` occurrence and require an adjacent
  `mcp-start`), but not a blocker. (1/2)

### H. Framework & Infra — 4/4

- `bash` portability preserved (`date +%s%3N` graceful fallback
  for BSD/macOS without GNU coreutils).
- `jq` dependency already mandatory project-wide; no new external
  dependencies introduced.
- `CLAUDE_TELEMETRY_DISABLED=1` kill switch respected.

### I. Tests & Execution — 6/6

- [I1] 17 new tests (3 TelemetryMcpHelperIT + 5 CreationSkillsMarkersIT
  + 3 XEpicDecomposeMarkersIT + 5 XJiraCreateMarkersIT + 2
  CreationSkillsSmokeIT — actually 18 counting the smoke dual-test
  class). All pass. (2/2)
- [I2] Full regression: 6308/6308. No regressions. (2/2)
- [I3] Golden files regenerated and clean. (2/2)

### J. Security & Production — 1/1

- Timer file paths validated upstream (method length capped,
  fixed prefix directory). No new credential / secret surface. No
  new deserialization or PII emission paths (scrubber downstream
  unchanged).

### K. TDD Process — 5/5

- [K1] Per-task commits: 5 feature commits (TASK-001..005) + 1
  chore(generated). Each commit pairs source changes with its
  IT in the same commit (verifiable via `git show <sha> --stat`).
  (2/2)
- [K2] Conventional Commits scope `feat(task-0040-0008-NNN)`
  matches RULE-018 atomic-task-commit pattern. (2/2)
- [K3] Test-first discipline evident: test assertions validate
  the helper contract and the SKILL.md markers — the tests ARE
  the acceptance criteria from §7 Gherkin, translated into
  executable assertions. (1/1)

## Cross-File Consistency

Reviewed all 5 modified SKILL.md files and the shell helper as a
unit:

- Phase naming convention is consistent: `Phase-N-Pascal-Case-Name`,
  with `Phase-N_5-...` form for sub-phases (chosen to respect the
  linter regex `[A-Za-z0-9_-]+`). The same convention was used
  earlier by story-0040-0007 — no drift.
- MCP markers always wrap the `mcp__atlassian__*` tool call in
  the skill prompt, with the phase markers framing a larger
  section. Nesting depth is consistent across both Jira skills.
- Test class layout mirrors story-0040-0007's pattern (one
  `{Skill}MarkersIT` per skill + one `{StoryKind}SmokeIT`
  covering the boundary case).
- Shell helper case-branches are uniform in shape: validate args,
  compute event type, enrich BASE_EVENT, pipe to emit helper. No
  snowflake branches.

## Specialist Review Integration

All 3 applicable specialists (QA 34/36, Security 28/30, Performance
24/26) reported Approved with only Low-severity notes. Tech Lead
concurs with the specialist assessments and elevates none of the
Low findings. Four Low notes are deferred as out-of-scope hardening
opportunities (per `remediation-story-0040-0008.md`):

- FIND-001/003: BSD second-granularity (cross-platform constraint,
  test already tolerates)
- FIND-002: Temp-dir permissions consistency (epic-wide pattern,
  candidate for future hardening story)
- FIND-004: Per-call fork overhead at very high N (bounded in
  practice)

## Recommendations (Non-Blocking)

1. Track FIND-002 as a cross-cutting hardening story under
   EPIC-0040 — every telemetry helper would benefit from explicit
   `chmod 700` on its TMPDIR subdirectory.
2. Consider adding a lint rule that requires every
   `mcp__atlassian__*` mention in a SKILL.md to have an adjacent
   `mcp-start` marker — mechanical enforcement for G3 above.
3. Future: GNU-coreutils-aware second story could replace
   `date +%s%3N` with a bash-native millisecond reader once
   `EPOCHREALTIME` (bash 5+) is confirmed available in the
   project's supported bash versions.

## Conclusion

All DoR / DoD items from story-0040-0008 §4 satisfied:

- [x] Helper com sub-comandos mcp-start/mcp-end (§5.1)
- [x] Lista de fases por skill aprovada (§3.1 — 4/3/7/3/3)
- [x] 5 skills com markers em cada fase (boundary IT proves)
- [x] Chamadas MCP em `x-jira-create-*` envolvidas em mcp-start/end
- [x] Teste IT valida que tool.call com MCP prefix aparece no
      NDJSON (TelemetryMcpHelperIT schema-validated)
- [x] Degrade gracioso se MCP indisponível (skill não quebra) —
      fail-open preserved

**Decision: GO — 43/45.** Merge-ready from Tech Lead perspective.
