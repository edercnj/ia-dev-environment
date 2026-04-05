# Epic Execution Report -- {{EPIC_ID}}

> Branch: `{{BRANCH}}`
> Started: {{STARTED_AT}} | Finished: {{FINISHED_AT}}

## Executive Summary

| Metric | Value |
|--------|-------|
| Stories Completed | {{STORIES_COMPLETED}} |
| Stories Failed | {{STORIES_FAILED}} |
| Stories Blocked | {{STORIES_BLOCKED}} |
| Stories Total | {{STORIES_TOTAL}} |
| Completion | {{COMPLETION_PERCENTAGE}} |

## Execution Timeline

{{PHASE_TIMELINE_TABLE}}

## Final Story Status

{{STORY_STATUS_TABLE}}

## Consolidated Findings

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

## Review Scores Per Story

| Story | Specialist Score | Tech Lead Score | Overall |
|---|---|---|---|
{{REVIEW_SCORES_TABLE}}

## Coverage Trend

| Story | Line Coverage | Branch Coverage | Delta |
|---|---|---|---|
{{COVERAGE_TREND_TABLE}}

## Conventional Commits Compliance

| Story | Total Commits | CC Violations | Status |
|---|---|---|---|
{{CC_COMPLIANCE_TABLE}}

## PO Acceptance

| Story | @GK-N Coverage | AT-N Status | Decision |
|---|---|---|---|
{{PO_ACCEPTANCE_TABLE}}

## Commits and SHAs

{{COMMIT_LOG}}

## Unresolved Issues

{{UNRESOLVED_ISSUES}}

## PR Link

{{PR_LINK}}
