# Phase Reports Reference

> **Context:** This reference details phase completion report and epic progress report generation.
> Part of x-dev-epic-implement skill.

## Phase Completion Reports

After all stories in a phase complete (or reach terminal state) and the integrity
gate finishes, the orchestrator generates a **phase completion report** — a
human-readable record of the phase outcome saved alongside the execution plan.

### Report Generation

1. Read template at `.claude/templates/_TEMPLATE-PHASE-COMPLETION-REPORT.md` for required output format (RULE-007)
2. If template is found: generate the report following the template structure, filling all `{{PLACEHOLDER}}` tokens with real data from the checkpoint (story statuses, durations, findings, coverage, TDD metrics)
3. If template is NOT found (RULE-012 — graceful fallback): log `"WARNING: Template _TEMPLATE-PHASE-COMPLETION-REPORT.md not found, using inline format"` and generate the report with the following inline format:

```markdown
# Phase Completion Report — EPIC-{epicId} Phase {N}

> **Epic ID:** EPIC-{epicId}
> **Phase:** {N}
> **Date:** {currentDate}

## Stories Completed

| Story ID | Status | Duration |
|----------|--------|----------|
| story-{epicId}-YYYY | SUCCESS | 3m 45s |
| ... | ... | ... |

## Summary
- Stories attempted: {count}
- Stories succeeded: {count}
- Stories failed: {count}
- Stories blocked: {count}
- Phase duration: {duration}
```

4. Write the report to `plans/epic-{epicId}/reports/phase-{N}-completion-{epicId}.md`
5. The report header MUST include: Epic ID, Phase Number, Date, Author (role), Template Version (RULE-011)

### Report Content

The phase completion report contains:

- **Stories executed**: status (SUCCESS/FAILED/BLOCKED/SKIPPED), duration, commit SHA per story
- **Integrity gate results**: compilation, test, coverage, smoke gate results
- **Findings summary**: severity counts and examples from per-story reviews
- **TDD compliance**: TDD cycles, test-first commits, TPP progression per story
- **Coverage delta**: line and branch coverage before/after the phase
- **Blockers encountered**: descriptions, resolutions, impact assessments
- **Next phase readiness**: checklist and recommendation for proceeding

### Timing

The phase completion report is generated AFTER the integrity gate completes
(whether PASS or FAIL). This ensures the gate results are included in the report.
If the gate fails, the report documents the failure and serves as a diagnostic
artifact for the operator deciding whether to resume or abort.

## Phase 2 — Epic Progress Report Generation

After all stories in a phase complete (or reach terminal state), the orchestrator
generates a progress report. With per-story PRs, each story already has its own
tech lead review (via `x-dev-lifecycle` Phase 7) and its own PR (via Phase 6).
Phase 2 consolidates this information into a single report.

> **Note:** The legacy two-wave consolidation (tech lead review of full diff +
> mega-PR creation) is only used when `--single-pr` is set. See the `--single-pr`
> guard in Phase 0 Step 7.

**Skip condition:** If NO stories have status SUCCESS, skip report generation entirely.
Log: `"No successful stories — skipping report generation"` and proceed to Phase 3.

### 2.1 Generate Progress Report

After all stories reach terminal state (SUCCESS, FAILED, or BLOCKED):

1. Read checkpoint to collect all story results
2. Build PR links table from `prUrl`, `prNumber`, `prMergeStatus` per story
3. Generate `epic-execution-report.md` using the template
4. Replace `{{PR_LINKS_TABLE}}` with the per-story PR table:

```markdown
| Story | PR | Status | Tech Lead Score | Merged At |
|-------|-----|--------|-----------------|-----------|
| story-{epicId}-0001 | [#41](https://github.com/org/repo/pull/41) | MERGED | 42/45 | 2026-04-01T10:30:00Z |
| story-{epicId}-0002 | [#42](https://github.com/org/repo/pull/42) | OPEN | 38/45 | — |
| story-{epicId}-0003 | — | FAILED | — | — |
```

5. Replace other `{{PLACEHOLDER}}` tokens with real data:
   - `{{EPIC_ID}}`, `{{STARTED_AT}}`, `{{FINISHED_AT}}`
   - `{{STORIES_COMPLETED}}`, `{{STORIES_FAILED}}`, `{{STORIES_BLOCKED}}`, `{{STORIES_TOTAL}}`
   - `{{COMPLETION_PERCENTAGE}}`: completed/total x 100
   - `{{PHASE_TIMELINE_TABLE}}`: phase start/end times from checkpoint
   - `{{STORY_STATUS_TABLE}}`: per-story status with commit SHAs
   - `{{COVERAGE_BEFORE}}`, `{{COVERAGE_AFTER}}`, `{{COVERAGE_DELTA}}`
   - `{{TDD_COMPLIANCE_TABLE}}`: TDD compliance per-story table
   - `{{TDD_SUMMARY}}`: TDD compliance aggregated summary
   - `{{UNRESOLVED_ISSUES}}`: findings with severity >= Medium (from per-story reviews)
6. Include summary metrics: completed/failed/blocked counts, overall completion %
7. Validate: no unresolved `{{...}}` placeholders remain in output
8. Write `epic-execution-report.md` to `plans/epic-{epicId}/`

**Result Handling:**
- On SUCCESS: report written to `plans/epic-{epicId}/epic-execution-report.md`. Update checkpoint atomically (RULE-002)
- On FAILURE: log `"ERROR: Report generation failed"`, continue to Phase 3

### 2.2 Incremental Report Updates (RULE-010)

The report is updated incrementally as each story completes, not only at the end:

1. After each story reaches a terminal state (SUCCESS, FAILED, BLOCKED):
   - Append or update the corresponding row in `{{PR_LINKS_TABLE}}`
   - Update summary metrics (completed/failed/blocked counts)
2. At the end of all phases: generate the final version with all metrics resolved
3. The incremental report allows real-time progress monitoring during epic execution

### 2.3 Checkpoint Finalization

After report generation completes, persist final state:

1. Register report path: `updateCheckpoint(epicDir, { reportPath })`
2. Set `finishedAt` timestamp
3. Persist final `execution-state.json` with all metrics
