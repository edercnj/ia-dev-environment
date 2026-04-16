# Review Remediation Tracker — story-0040-0006

**Story:** story-0040-0006
**PR:** #415
**Date:** 2026-04-16
**Status:** 5 findings pending remediation (0 CRITICAL / 0 HIGH / 2 MEDIUM / 2 LOW)

## Findings Tracker

| Finding ID | Engineer | Severity | Description | Status | Fix Commit SHA |
| :--- | :--- | :--- | :--- | :--- | :--- |
| FIND-001 | QA | MEDIUM | TelemetryPhaseHelperIT file length ~320 lines exceeds 250-line soft limit. Split helpers into fixture class OR wrap in `@Nested`. | Open | — |
| FIND-002 | QA | LOW | Four Markers ITs share structure. Extract parametrized base `AbstractSkillMarkerContract(skillFile, expectedPairs, skillToken)`. | Open | — |
| FIND-003 | DevOps | LOW | No tree-walking SKILL.md scanner. Add `AllSkillsMarkerLintIT` that lints every `skills/core/**/SKILL.md`. | Open | — |
| FIND-004 | Tech Lead | MEDIUM | `TelemetryMarkerLint.validateBalance()` is 60 lines; exceeds Rule 03 §Hard Limits (≤25). Extract `handleStartMarker()`, `handleEndMarker()`, `reportUnclosedStarts()`. | Open | — |
| FIND-005 | Tech Lead | LOW | New `dev.iadev.ci` package lacks ADR reference (Rule 04 §Deviations). Add one-line pointer in `package-info.java` or a short ADR stub. | Open | — |

## Remediation Summary

| Status | Count |
| :--- | ---: |
| Open | 5 |
| Fixed | 0 |
| Deferred | 0 |
| Accepted | 0 |

## Resolution Policy

- MEDIUM and LOW findings do not block merge under EPIC-0042 auto-approval rules.
- Items MAY be deferred to a follow-up story; if deferred, tag with DEFERRED and link the follow-up ID.
