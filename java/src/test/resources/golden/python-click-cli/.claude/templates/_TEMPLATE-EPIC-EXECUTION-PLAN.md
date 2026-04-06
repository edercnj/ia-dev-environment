# Epic Execution Plan -- {{EPIC_ID}}

> **Epic ID:** {{EPIC_ID}}
> **Title:** {{EPIC_TITLE}}
> **Date:** {{GENERATION_DATE}}
> **Total Stories:** {{TOTAL_STORIES}}
> **Total Phases:** {{TOTAL_PHASES}}
> **Author:** {{AUTHOR_ROLE}}
> **Template Version:** {{TEMPLATE_VERSION}}

---

## Execution Strategy

| Attribute | Value |
|-----------|-------|
| Strategy | {{EXECUTION_STRATEGY}} |
| Max Parallelism | {{MAX_PARALLELISM}} |
| Checkpoint Frequency | {{CHECKPOINT_FREQUENCY}} |
| Dry Run | {{DRY_RUN}} |

{{EXECUTION_STRATEGY_NOTES}}

---

## Phase Timeline

| Phase | Name | Stories | Parallelism | Estimated Duration | Dependencies |
|-------|------|---------|-------------|-------------------|--------------|
| {{PHASE_NUMBER}} | {{PHASE_NAME}} | {{PHASE_STORIES}} | {{PHASE_PARALLELISM}} | {{PHASE_ESTIMATED_DURATION}} | {{PHASE_DEPENDENCIES}} |

> **Total estimated duration:** {{TOTAL_ESTIMATED_DURATION}}

---

## Story Execution Order

| Order | Story ID | Title | Phase | Dependencies | Critical Path | Estimated Effort |
|-------|----------|-------|-------|--------------|---------------|-----------------|
| {{STORY_ORDER}} | {{STORY_ID}} | {{STORY_TITLE}} | {{STORY_PHASE}} | {{STORY_DEPENDENCIES}} | {{CRITICAL_PATH}} | {{ESTIMATED_EFFORT}} |

> **Critical Path Legend:** `Yes` = story is on the critical path (delay impacts epic deadline); `No` = story has slack.
> **Estimated Effort:** `S` (small), `M` (medium), `L` (large), `XL` (extra-large).

---

## Pre-flight Analysis Summary

| Check | Status | Details |
|-------|--------|---------|
| Story files present | {{PREFLIGHT_STORIES_STATUS}} | {{PREFLIGHT_STORIES_DETAILS}} |
| Dependencies resolved | {{PREFLIGHT_DEPS_STATUS}} | {{PREFLIGHT_DEPS_DETAILS}} |
| Circular dependencies | {{PREFLIGHT_CIRCULAR_STATUS}} | {{PREFLIGHT_CIRCULAR_DETAILS}} |
| Implementation map valid | {{PREFLIGHT_MAP_STATUS}} | {{PREFLIGHT_MAP_DETAILS}} |

{{PREFLIGHT_NOTES}}

---

## Resource Requirements

| Resource | Estimate | Notes |
|----------|----------|-------|
| Estimated tokens | {{ESTIMATED_TOKENS}} | {{TOKENS_NOTES}} |
| Estimated wall time | {{ESTIMATED_WALL_TIME}} | {{WALL_TIME_NOTES}} |
| Max parallel subagents | {{MAX_PARALLEL_SUBAGENTS}} | {{SUBAGENTS_NOTES}} |
| Peak memory estimate | {{PEAK_MEMORY_ESTIMATE}} | {{MEMORY_NOTES}} |

---

## Risk Assessment

| Risk | Severity | Likelihood | Mitigation |
|------|----------|------------|------------|
| {{RISK_DESCRIPTION}} | {{RISK_SEVERITY}} | {{RISK_LIKELIHOOD}} | {{RISK_MITIGATION}} |

> **Severity levels:** Critical, High, Medium, Low.
> **Likelihood levels:** Very Likely, Likely, Possible, Unlikely.

{{RISK_ASSESSMENT_NOTES}}

---

## Checkpoint Strategy

| Parameter | Value |
|-----------|-------|
| Checkpoint frequency | {{CHECKPOINT_FREQUENCY}} |
| Save on phase completion | {{SAVE_ON_PHASE_COMPLETION}} |
| Save on story completion | {{SAVE_ON_STORY_COMPLETION}} |
| Save on integrity gate failure | {{SAVE_ON_GATE_FAILURE}} |
| State file location | {{STATE_FILE_LOCATION}} |

### Recovery Procedures

{{RECOVERY_PROCEDURES}}

### Resume Behavior

{{RESUME_BEHAVIOR}}
