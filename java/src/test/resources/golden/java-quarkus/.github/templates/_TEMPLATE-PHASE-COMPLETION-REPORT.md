# Phase Completion Report -- {{EPIC_ID}} Phase {{PHASE_NUMBER}}

> **Epic ID:** {{EPIC_ID}}
> **Phase Number:** {{PHASE_NUMBER}}
> **Phase Name:** {{PHASE_NAME}}
> **Started:** {{START_TIMESTAMP}}
> **Finished:** {{END_TIMESTAMP}}
> **Author:** {{AUTHOR_ROLE}}
> **Template Version:** {{TEMPLATE_VERSION}}

---

## Stories Completed

| Story ID | Title | Status | Duration | Commit SHA |
|----------|-------|--------|----------|------------|
| {{STORY_ID}} | {{STORY_TITLE}} | {{STORY_STATUS}} | {{STORY_DURATION}} | {{STORY_COMMIT_SHA}} |

> **Status values:** `Success`, `Failed`, `Blocked`, `Partial`, `Skipped`.

### Summary

| Metric | Value |
|--------|-------|
| Stories attempted | {{STORIES_ATTEMPTED}} |
| Stories succeeded | {{STORIES_SUCCEEDED}} |
| Stories failed | {{STORIES_FAILED}} |
| Stories blocked | {{STORIES_BLOCKED}} |

---

## Integrity Gate Results

| Gate | Result | Details | Duration |
|------|--------|---------|----------|
| Compilation | {{COMPILATION_RESULT}} | {{COMPILATION_DETAILS}} | {{COMPILATION_DURATION}} |
| Tests | {{TESTS_RESULT}} | {{TESTS_DETAILS}} | {{TESTS_DURATION}} |
| Coverage | {{COVERAGE_RESULT}} | {{COVERAGE_DETAILS}} | {{COVERAGE_DURATION}} |

> **Result values:** `Pass`, `Fail`, `Skip`.

### Gate Decision

{{GATE_DECISION}}

---

## Findings Summary

| Severity | Count | Examples |
|----------|-------|---------|
| Critical | {{FINDINGS_CRITICAL_COUNT}} | {{FINDINGS_CRITICAL_EXAMPLES}} |
| High | {{FINDINGS_HIGH_COUNT}} | {{FINDINGS_HIGH_EXAMPLES}} |
| Medium | {{FINDINGS_MEDIUM_COUNT}} | {{FINDINGS_MEDIUM_EXAMPLES}} |
| Low | {{FINDINGS_LOW_COUNT}} | {{FINDINGS_LOW_EXAMPLES}} |

> **Total findings this phase:** {{FINDINGS_TOTAL_PHASE}}
> **Cumulative findings:** {{FINDINGS_TOTAL_CUMULATIVE}}

{{FINDINGS_NOTES}}

---

## TDD Compliance

| Story ID | TDD Cycles | Test-First Commits | Total Commits | TDD % | TPP Progression | Status |
|----------|------------|-------------------|---------------|-------|-----------------|--------|
| {{STORY_ID}} | {{TDD_CYCLES}} | {{TEST_FIRST_COMMITS}} | {{TOTAL_COMMITS}} | {{TDD_PERCENTAGE}} | {{TPP_PROGRESSION}} | {{TDD_STATUS}} |

### Phase TDD Summary

| Metric | Value |
|--------|-------|
| Average TDD compliance | {{AVG_TDD_COMPLIANCE}} |
| Stories with 100% TDD | {{STORIES_FULL_TDD}} |
| Stories below threshold | {{STORIES_BELOW_TDD}} |

---

## Coverage Delta

| Metric | Before Phase | After Phase | Delta |
|--------|-------------|-------------|-------|
| Line Coverage | {{LINE_COVERAGE_BEFORE}} | {{LINE_COVERAGE_AFTER}} | {{LINE_COVERAGE_DELTA}} |
| Branch Coverage | {{BRANCH_COVERAGE_BEFORE}} | {{BRANCH_COVERAGE_AFTER}} | {{BRANCH_COVERAGE_DELTA}} |

### Coverage Gate

| Threshold | Required | Actual | Status |
|-----------|----------|--------|--------|
| Line Coverage | {{LINE_COVERAGE_REQUIRED}} | {{LINE_COVERAGE_ACTUAL}} | {{LINE_COVERAGE_STATUS}} |
| Branch Coverage | {{BRANCH_COVERAGE_REQUIRED}} | {{BRANCH_COVERAGE_ACTUAL}} | {{BRANCH_COVERAGE_STATUS}} |

---

## Blockers Encountered

| Blocker | Story ID | Description | Resolution | Impact |
|---------|----------|-------------|------------|--------|
| {{BLOCKER_ID}} | {{BLOCKER_STORY_ID}} | {{BLOCKER_DESCRIPTION}} | {{BLOCKER_RESOLUTION}} | {{BLOCKER_IMPACT}} |

> **Resolution values:** `Resolved`, `Deferred`, `Workaround`, `Unresolved`.

### Retry Summary

| Story ID | Retry Count | Final Status | Root Cause |
|----------|-------------|--------------|------------|
| {{RETRY_STORY_ID}} | {{RETRY_COUNT}} | {{RETRY_FINAL_STATUS}} | {{RETRY_ROOT_CAUSE}} |

---

## Next Phase Readiness

### Readiness Checklist

- [ ] All integrity gates passed for this phase
- [ ] No unresolved critical blockers
- [ ] Coverage thresholds met (line >= {{LINE_COVERAGE_REQUIRED}}, branch >= {{BRANCH_COVERAGE_REQUIRED}})
- [ ] TDD compliance above threshold
- [ ] Findings reviewed and triaged
- [ ] Dependencies for next phase satisfied

### Recommendation

{{NEXT_PHASE_RECOMMENDATION}}

### Next Phase Preview

| Attribute | Value |
|-----------|-------|
| Next phase number | {{NEXT_PHASE_NUMBER}} |
| Next phase name | {{NEXT_PHASE_NAME}} |
| Stories in next phase | {{NEXT_PHASE_STORIES}} |
| Expected parallelism | {{NEXT_PHASE_PARALLELISM}} |
