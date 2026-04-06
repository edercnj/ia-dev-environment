# Epic Execution Report -- {{EPIC_ID}}

> Branch: `{{BRANCH}}`
> Started: {{STARTED_AT}} | Finished: {{FINISHED_AT}}

## Sumário Executivo

| Metric | Value |
|--------|-------|
| Stories Completed | {{STORIES_COMPLETED}} |
| Stories Failed | {{STORIES_FAILED}} |
| Stories Blocked | {{STORIES_BLOCKED}} |
| Stories Total | {{STORIES_TOTAL}} |
| Completion | {{COMPLETION_PERCENTAGE}} |

## Timeline de Execução

{{PHASE_TIMELINE_TABLE}}

## Status Final por Story

{{STORY_STATUS_TABLE}}

## Findings Consolidados

{{FINDINGS_SUMMARY}}

## Coverage Delta

| Metric | Before | After | Delta |
|--------|--------|-------|-------|
| Line Coverage | {{COVERAGE_BEFORE}} | {{COVERAGE_AFTER}} | {{COVERAGE_DELTA}} |

## TDD Compliance

### Per-Story TDD Metrics

| Story | TDD Commits | Total Commits | TDD % | TPP Progression | Status |
|-------|-------------|---------------|-------|-----------------|--------|
{{TDD_COMPLIANCE_TABLE}}

### Summary

{{TDD_SUMMARY}}

## Commits e SHAs

{{COMMIT_LOG}}

## Issues Não Resolvidos

{{UNRESOLVED_ISSUES}}

## PR Link

{{PR_LINK}}
