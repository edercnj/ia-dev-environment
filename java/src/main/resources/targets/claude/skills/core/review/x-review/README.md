# x-review

> Parallel code review with specialist engineers (Security, QA, Performance, Database, Observability, DevOps, API, Event). Launches parallel subagents, each reading their own knowledge pack, then consolidates into a scored report. Use for pre-PR quality validation.

| | |
|---|---|
| **Category** | Orchestrator |
| **Invocation** | `/x-review [STORY-ID or --scope reviewer1,reviewer2]` |
| **Delegates to** | Security, QA, Performance, Database, Data Modeling, Observability, DevOps, API, Event specialist subagents |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## Overview

x-review orchestrates a multi-specialist code review by launching parallel subagents, each with its own knowledge pack and checklist. It detects the branch diff, determines which specialists are applicable based on the project stack, dispatches all reviews in a single message for true parallelism, then consolidates individual scores into a dashboard with remediation tracking. Optionally generates a correction story for critical findings.

## Execution Flow

```mermaid
flowchart TD
    START(["/x-review STORY-ID"]) --> P0

    subgraph P0["Phase 0 -- Idempotency Pre-Check"]
        P0A[Check existing reports] --> P0B{Reports valid?}
        P0B -->|Yes, code unchanged| REUSE([Reuse existing reports])
        P0B -->|No| P0C[Proceed to detect]
    end

    P0C --> P1

    subgraph P1["Phase 1 -- Detect Context"]
        P1A[Extract story ID + branch] --> P1B[git diff main]
        P1B --> P1C{Changes found?}
        P1C -->|No| ABORT([No changes -- abort])
        P1C -->|Yes| P1D[Determine applicable specialists]
    end

    P1D --> P2

    subgraph P2["Phase 2 -- Parallel Reviews (SINGLE message)"]
        direction LR
        SEC["Security\n/30"]
        QA["QA\n/36"]
        PERF["Performance\n/26"]
        DB["Database\n/40"]
        DM["Data Modeling\n/20"]
        OBS["Observability\n/18"]
        DEVOPS["DevOps\n/20"]
        API["API\n/16"]
        EVENT["Event\n/28"]
    end

    P2 --> P3

    subgraph P3["Phase 3 -- Consolidation"]
        P3A[Collect scores + findings] --> P3B[Save individual reports]
        P3B --> P3C[Generate dashboard]
        P3C --> P3D[Generate remediation tracking]
        P3D --> P3E[Update threat model]
    end

    P3 --> P4

    subgraph P4["Phase 4 -- Story Generation"]
        P4A{Critical/High/Medium findings?}
        P4A -->|No| DONE([Review complete])
        P4A -->|Yes| P4B[Ask user confirmation]
        P4B -->|No| DONE
        P4B -->|Yes| P4C[Generate correction story]
        P4C --> DONE
    end

    style REUSE fill:#2d6a4f,color:#fff
    style ABORT fill:#e94560,color:#fff
    style DONE fill:#2d6a4f,color:#fff
    style SEC fill:#533483,color:#fff
    style QA fill:#533483,color:#fff
    style PERF fill:#533483,color:#fff
    style DB fill:#533483,color:#fff
    style DM fill:#533483,color:#fff
    style OBS fill:#533483,color:#fff
    style DEVOPS fill:#533483,color:#fff
    style API fill:#533483,color:#fff
    style EVENT fill:#533483,color:#fff
```

## Specialists

| # | Specialist | Focus Area | Max Score | Condition |
|---|-----------|------------|-----------|-----------|
| 1 | Security | Input validation, auth, crypto, OWASP Top 10 | /30 | Always |
| 2 | QA | TDD compliance, coverage, test naming, TPP | /36 | Always |
| 3 | Performance | N+1, pooling, pagination, circuit breakers | /26 | Always |
| 4 | Database | Migrations, indexes, audit columns, locking | /40 | database != none |
| 5 | Data Modeling | Aggregates, value objects, repository patterns | /20 | database != none AND architecture in [hexagonal, ddd, cqrs] |
| 6 | Observability | Tracing, metrics, structured logging, health checks | /18 | observability != none |
| 7 | DevOps | Dockerfile, K8s manifests, probes, image scanning | /20 | container/orchestrator != none |
| 8 | API | RESTful URLs, status codes, RFC 7807, pagination | /16 | interfaces contain protocol types |
| 9 | Event | CloudEvents, idempotency, DLT, outbox pattern | /28 | event_driven or event interfaces |

## Scoring

Each specialist applies a checklist where every item scores 0 (fail), 1 (partial), or 2 (pass). **All items must score 2/2 for a specialist to be Approved.** Any item at 0 results in Rejected; any at 1 with none at 0 results in Partial. The overall status is Approved only when every active specialist is individually Approved.

Findings are classified by severity: `CRITICAL | HIGH | MEDIUM | LOW`. Any item scoring below 2 must be fixed before merge.

## Outputs

| Artifact | Path | Description |
|----------|------|-------------|
| Individual reports | `plans/epic-XXXX/reviews/review-{engineer}-story-XXXX-YYYY.md` | Per-specialist scored review |
| Consolidated dashboard | `plans/epic-XXXX/reviews/dashboard-story-XXXX-YYYY.md` | Aggregate scores, severity distribution, review history |
| Remediation tracking | `plans/epic-XXXX/reviews/remediation-story-XXXX-YYYY.md` | Finding tracker with status and fix commit references |
| Threat model update | `results/security/threat-model.md` | STRIDE-classified threats from security findings |
| Correction story | `plans/epic-XXXX/reviews/correction-story-XXXX-YYYY.md` | Generated only when user confirms (Phase 4) |

## See Also

- [x-review-pr](../x-review-pr/SKILL.md) -- Tech Lead holistic 45-point review (runs after x-review)
- [x-story-implement](../x-story-implement/SKILL.md) -- Full development cycle (invokes x-review in Phase 4)
- [x-epic-implement](../x-epic-implement/SKILL.md) -- Epic orchestrator (delegates stories to x-story-implement)
- [x-test-run](../x-test-run/SKILL.md) -- Coverage validation used during review verification
