# Task Breakdown -- {{STORY_ID}}

## Header

| Field | Value |
|-------|-------|
| Story ID | {{STORY_ID}} |
| Epic ID | {{EPIC_ID}} |
| Date | {{DATE}} |
| Author | {{AUTHOR}} |
| Template Version | 1.0.0 |

## Summary

| Metric | Value |
|--------|-------|
| Total Tasks | {{TOTAL_TASKS}} |
| Parallelizable Tasks | {{PARALLEL_TASKS}} |
| Estimated Effort | {{ESTIMATED_EFFORT}} |
| Mode | {{MODE}} |

## Dependency Graph

```mermaid
graph TD
    {{DEPENDENCY_GRAPH}}
```

## Tasks Table

| Task ID | Test Scenario Ref | TPP Level | Type | Phase | Layer | Components | Parallel | Depends On | Tier | Budget | Agent | DoD |
|---------|------------------|-----------|------|-------|-------|-----------|----------|-----------|------|--------|-------|-----|
| TASK-1 | {{TEST_REF}} | nil | UT | RED | {{LAYER}} | {{COMPONENTS}} | {{PARALLEL}} | -- | {{TIER}} | {{BUDGET}} | {{AGENT}} | {{DOD}} |
| TASK-2 | {{TEST_REF}} | nil | UT | GREEN | {{LAYER}} | {{COMPONENTS}} | {{PARALLEL}} | TASK-1 | {{TIER}} | {{BUDGET}} | {{AGENT}} | {{DOD}} |
| TASK-3 | {{TEST_REF}} | nil | UT | REFACTOR | {{LAYER}} | {{COMPONENTS}} | {{PARALLEL}} | TASK-2 | {{TIER}} | {{BUDGET}} | {{AGENT}} | {{DOD}} |
| TASK-4 | {{TEST_REF}} | constant | UT | RED | {{LAYER}} | {{COMPONENTS}} | {{PARALLEL}} | TASK-3 | {{TIER}} | {{BUDGET}} | {{AGENT}} | {{DOD}} |
| TASK-5 | {{TEST_REF}} | constant | UT | GREEN | {{LAYER}} | {{COMPONENTS}} | {{PARALLEL}} | TASK-4 | {{TIER}} | {{BUDGET}} | {{AGENT}} | {{DOD}} |
| TASK-6 | {{TEST_REF}} | constant | UT | REFACTOR | {{LAYER}} | {{COMPONENTS}} | {{PARALLEL}} | TASK-5 | {{TIER}} | {{BUDGET}} | {{AGENT}} | {{DOD}} |
| TASK-7 | {{TEST_REF}} | scalar | AT | RED | {{LAYER}} | {{COMPONENTS}} | {{PARALLEL}} | TASK-6 | {{TIER}} | {{BUDGET}} | {{AGENT}} | {{DOD}} |

## Escalation Notes

| Task ID | Reason | Recommended Action |
|---------|--------|--------------------|
| {{TASK_ID}} | {{REASON}} | {{ACTION}} |
