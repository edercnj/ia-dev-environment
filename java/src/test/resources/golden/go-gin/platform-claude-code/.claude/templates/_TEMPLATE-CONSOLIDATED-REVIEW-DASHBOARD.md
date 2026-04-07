# Consolidated Review Dashboard

> **Story ID:** {{STORY_ID}}
> **Epic ID:** {{EPIC_ID}}
> **Date:** {{DATE}}
> **Template Version:** 1.0

## Overall Score

00/00 | Status: Approved

> Replace `00/00` with actual score. Replace `Approved` with actual status.
> Parseable via regex: `(\d+)/(\d+)\s*\|\s*Status:\s*(Approved|Rejected|Partial)`

## Engineer Scores Table

| Engineer Type | Score | Max | Status |
| :--- | :--- | :--- | :--- |
| Security | {{SECURITY_SCORE}} | {{MAX}} | {{STATUS}} |
| QA | {{QA_SCORE}} | {{MAX}} | {{STATUS}} |
| Performance | {{PERF_SCORE}} | {{MAX}} | {{STATUS}} |
| Database | {{DB_SCORE}} | {{MAX}} | {{STATUS}} |
| Observability | {{OBS_SCORE}} | {{MAX}} | {{STATUS}} |
| DevOps | {{DEVOPS_SCORE}} | {{MAX}} | {{STATUS}} |
| API | {{API_SCORE}} | {{MAX}} | {{STATUS}} |
| Event | {{EVENT_SCORE}} | {{MAX}} | {{STATUS}} |

## Tech Lead Score

{{TECH_LEAD_SCORE}}/55 | Status: {{TECH_LEAD_STATUS}}

> Updated by `x-review-pr` after Tech Lead review.

## Critical Issues Summary

| # | Engineer | Severity | Description | File | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | {{ENGINEER}} | Critical | {{DESCRIPTION}} | {{FILE}} | {{STATUS}} |

## Severity Distribution

| Severity | Count |
| :--- | :--- |
| Critical | {{CRITICAL_COUNT}} |
| High | {{HIGH_COUNT}} |
| Medium | {{MEDIUM_COUNT}} |
| Low | {{LOW_COUNT}} |
| **Total** | **{{TOTAL_COUNT}}** |

## Remediation Status

| Status | Count |
| :--- | :--- |
| Open | {{OPEN_COUNT}} |
| Fixed | {{FIXED_COUNT}} |
| Deferred | {{DEFERRED_COUNT}} |
| Accepted | {{ACCEPTED_COUNT}} |
| **Total** | **{{TOTAL_COUNT}}** |

## Review History

> Cumulative section -- preserves previous rounds of review.
> Each round records date, scores, and status for traceability.

### Round 1

- **Date:** {{ROUND_1_DATE}}
- **Specialist Scores:** {{ROUND_1_SPECIALIST_SCORES}}
- **Tech Lead Score:** {{ROUND_1_TL_SCORE}}
- **Status:** {{ROUND_1_STATUS}}
- **Notes:** {{ROUND_1_NOTES}}

### Round 2

- **Date:** {{ROUND_2_DATE}}
- **Specialist Scores:** {{ROUND_2_SPECIALIST_SCORES}}
- **Tech Lead Score:** {{ROUND_2_TL_SCORE}}
- **Status:** {{ROUND_2_STATUS}}
- **Notes:** {{ROUND_2_NOTES}}

> Add subsequent rounds as needed (Round 3, Round 4, ...).

## Correction Story

{{CORRECTION_STORY_LINK}}

> Link to the correction story when applicable (e.g., `story-XXXX-YYYY`).
> Leave empty if no corrections are needed.
