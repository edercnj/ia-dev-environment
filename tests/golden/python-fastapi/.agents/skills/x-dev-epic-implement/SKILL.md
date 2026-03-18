---
name: x-dev-epic-implement
description: "Orchestrates the implementation of an entire epic by executing stories sequentially or in parallel via worktrees. Parses epic ID and flags, validates prerequisites (epic directory, IMPLEMENTATION-MAP.md, story files), then delegates story execution to x-dev-lifecycle subagents."
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill
argument-hint: "[EPIC-ID] [--phase N] [--story XXXX-YYYY] [--skip-review] [--dry-run] [--resume] [--parallel]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Epic Implementation Orchestrator

## When to Use

- Full epic implementation spanning multiple stories
- Multi-story orchestration with dependency-aware execution order
- Resumable epic execution after interruption
- Parallel story execution via worktrees

## Input Parsing

### Positional Argument (Required)

| Argument | Format | Required | Description |
|----------|--------|----------|-------------|
| `EPIC-ID` | `XXXX` (4-digit zero-padded) | **Mandatory** | The epic identifier, e.g., `0042` |

The epic ID is a required positional argument. If missing, abort immediately:

```
ERROR: Epic ID is required. Usage: /x-dev-epic-implement [EPIC-ID] [flags]
```

### Optional Flags

| Flag | Type | Default | Description |
|------|------|---------|-------------|
| `--phase N` | number | (all phases) | Execute only phase N (0-3) |
| `--story XXXX-YYYY` | string | (all stories) | Execute only a specific story by ID |
| `--skip-review` | boolean | `false` | Skip review phases in x-dev-lifecycle subagents |
| `--dry-run` | boolean | `false` | Generate execution plan without executing |
| `--resume` | boolean | `false` | Continue from last checkpoint (execution-state.json) |
| `--parallel` | boolean | `false` | Enable parallel worktrees (default: sequential) |

## Prerequisites Check

Before execution, validate all prerequisites in order. Abort on first failure.

### 1. Epic Directory

Check that `docs/stories/epic-XXXX/` exists (where XXXX is the parsed epic ID).

```
ERROR: Directory docs/stories/epic-{epicId}/ not found. Run /x-story-epic-full first.
```

### 2. Epic File

Check that `EPIC-XXXX.md` exists in the epic directory.

```
ERROR: EPIC-{epicId}.md not found in docs/stories/epic-{epicId}/. Run /x-story-epic first.
```

### 3. Implementation Map

Check that `IMPLEMENTATION-MAP.md` exists in the epic directory.

```
ERROR: IMPLEMENTATION-MAP.md not found. Run /x-story-map first.
```

### 4. Story Files

Glob for `story-XXXX-*.md` files in the epic directory. At least one must exist.

```
ERROR: No story files found matching story-{epicId}-*.md.
```

### 5. Resume Checkpoint (conditional)

If `--resume` flag is set, check that `execution-state.json` exists in the epic directory.

```
ERROR: No checkpoint found (execution-state.json missing). Cannot resume. Run without --resume.
```

## Phase 0 — Preparation (Orchestrator — Inline)

1. **Parse arguments**: Extract epic ID from positional argument and all optional flags
2. **Run prerequisites checks**: Execute all 5 prerequisite validations (abort on first failure)
3. **Read IMPLEMENTATION-MAP.md**: Extract the story dependency graph and execution order
4. **Read EPIC-XXXX.md**: Load epic context (title, description, acceptance criteria)
5. **Discover story files**: Glob `story-XXXX-*.md` to collect all story files in the epic directory
6. **Determine execution order**: Use the dependency graph from IMPLEMENTATION-MAP.md to order stories; stories without dependencies can run in parallel if `--parallel` is set
7. **Create branch**: `git checkout -b feat/epic-{epicId}-implementation`
8. **Dry-run exit**: If `--dry-run` is set, output the execution plan (story order, dependencies, estimated phases) and stop
9. **Resume handling**: If `--resume` is set, read `execution-state.json` and skip already-completed stories
10. **Delegate**: For each story in execution order, invoke `/x-dev-lifecycle` with appropriate flags

## Phase 1 — Execution Loop

### 1.1 Initialize Execution State

1. Read `IMPLEMENTATION-MAP.md` content from the epic directory
2. Call `parseImplementationMap(content)` to obtain a `ParsedMap` containing:
   - `stories`: Map of story IDs to `DagNode` (with phase, dependencies, critical path flag)
   - `phases`: Map of phase numbers to story ID arrays
   - `criticalPath`: Ordered array of story IDs on the critical path
   - `totalPhases`: Number of execution phases
3. Build the stories array with `{ id, phase }` entries from the `ParsedMap`
4. Call `createCheckpoint(epicDir, input)` where input is:
   - `epicId`: The parsed epic ID
   - `branch`: `feat/epic-{epicId}-full-implementation`
   - `stories`: Array of `{ id, phase }` from step 3
   - `mode`: `{ parallel: false, skipReview: <from flags> }`
5. The returned `ExecutionState` tracks all story statuses, metrics, and integrity gates

### 1.2 Branch Management

1. Ensure a clean starting point:
   ```
   git checkout main && git pull origin main
   ```
2. Create the epic branch:
   ```
   git checkout -b feat/epic-{epicId}-full-implementation
   ```
3. If the branch already exists (resume scenario):
   ```
   git checkout feat/epic-{epicId}-full-implementation
   ```
   [Placeholder: resume from checkpoint — story-0005-0008]

### 1.3 Core Loop Algorithm

Execute stories phase-by-phase in dependency order:

```
For each phase in (0..totalPhases-1):
  1. Call getExecutableStories(parsedMap, executionState)
     → Returns stories sorted by critical path priority (RULE-007)
     → Only PENDING stories with all dependencies SUCCESS are returned
  2. If no executable stories and some remain PENDING:
     → Phase is blocked; log warning and advance to next phase
  3. For each executable story:
     a. updateStoryStatus(epicDir, storyId, { status: "IN_PROGRESS" })
     b. Dispatch subagent (see 1.4)
     c. Validate result (see 1.5)
     d. Update checkpoint (see 1.6)
  4. [Placeholder: integrity gate between phases — story-0005-0006]
  5. [Placeholder: progress reporting — story-0005-0013]
  6. Re-read checkpoint via readCheckpoint(epicDir) for next iteration
```

The loop ensures that:
- Stories are dispatched in dependency-safe order
- BLOCKED stories are never dispatched (filtered by `getExecutableStories`)
- Each phase completes before the next begins (sequential mode)
- [Placeholder: partial execution filter — story-0005-0009]

### 1.4 Subagent Dispatch (Sequential Mode)

For each executable story, launch a clean-context subagent using the `Agent` tool:

**Subagent Configuration:**
- Tool: `Agent` with `subagent_type: "general-purpose"`
- Context isolation (RULE-001): The orchestrator passes ONLY metadata to the subagent.
  Never pass source code, knowledge packs, or diffs. The subagent is born with
  clean context and dies after completion.

**Prompt Template for Subagent:**
```
You are implementing story {storyId} for epic {epicId}.

Story file: docs/stories/epic-{epicId}/story-{storyId}.md
Branch: {branchName}
Phase: {currentPhase}
Skip review: {skipReview}

Execute the x-dev-lifecycle workflow:
1. Read the story file for requirements
2. Create implementation plan
3. Implement following TDD (Red-Green-Refactor)
4. Run tests and verify coverage
5. Commit changes with Conventional Commits

Return a JSON result with this exact structure (SubagentResult):
{
  "status": "SUCCESS" | "FAILED" | "PARTIAL",
  "commitSha": "<git commit SHA if SUCCESS>",
  "findingsCount": <number of review findings>,
  "summary": "<brief description of what was done>"
}
```

### 1.4a Parallel Worktree Dispatch (Conditional: `--parallel`)

When `mode.parallel === true`, replace the sequential dispatch (1.4) with parallel
worktree dispatch. All executable stories in the current phase are launched
concurrently in a SINGLE message.

**Activation:** Only when `--parallel` flag is set. When `--parallel` is NOT active
(default), the sequential dispatch in Section 1.4 is used unchanged.

**Dispatch Algorithm:**

1. Call `getExecutableStories(parsedMap, executionState)` to get all executable stories for the current phase
2. For each executable story, mark `IN_PROGRESS` via `updateStoryStatus(epicDir, storyId, { status: "IN_PROGRESS" })`
3. Launch ALL stories in a SINGLE message using the `Agent` tool with `isolation: "worktree"`:

```
For each story in executableStories:
  Agent(
    subagent_type: "general-purpose",
    isolation: "worktree",
    prompt: "<same prompt template as Section 1.4, with story-specific metadata>"
  )
```

**Branch Naming:** Each worktree operates on branch `feat/epic-{epicId}-{storyId}`.

**Context Isolation (RULE-001):** Each worktree subagent receives clean context,
identical to sequential mode. The orchestrator passes ONLY metadata (story ID,
branch, phase, flags). The `isolation: "worktree"` parameter ensures each subagent
works on an isolated copy of the repository.

4. Wait for ALL subagents to complete before proceeding to merge (Section 1.4b)
5. Validate each `SubagentResult` using Section 1.5 rules

### 1.4b Merge Strategy (After Parallel Dispatch)

After all parallel subagents complete, merge their worktree branches sequentially
into the epic branch, ordered by critical path priority (RULE-007).

**Merge Algorithm:**

1. Collect all stories with `status: "SUCCESS"` from parallel dispatch results
2. Sort by critical path priority: `sortByCriticalPath(successStories, parsedMap)`
3. For each SUCCESS story (in order):
   a. Attempt merge: `git merge feat/epic-{epicId}-{storyId}` into the epic branch
   b. If merge succeeds:
      - Call `updateStoryStatus(epicDir, storyId, { status: "SUCCESS", commitSha })` (RULE-002)
      - Checkpoint is updated after EACH merge, not in batch
   c. If merge conflict detected:
      - Dispatch conflict resolution subagent (see Section 1.4c)
      - If resolution succeeds: commit merge, update checkpoint as SUCCESS
      - If resolution fails: mark story as FAILED, trigger failure handling
4. For stories with `status: "FAILED"` from subagent dispatch:
   - Do NOT attempt merge
   - Delegate to failure handling (retry + block propagation per story-0005-0007)

**Checkpoint Timing (RULE-002):** The checkpoint is updated after each individual
merge operation, ensuring atomic persistence. If the orchestrator crashes mid-merge,
the checkpoint reflects the last successfully merged story.

### 1.4c Conflict Resolution Subagent

When a merge conflict is detected during parallel merge (Section 1.4b), dispatch
a conflict resolution subagent to attempt automatic resolution.

**Subagent Configuration:**
- Tool: `Agent` with `subagent_type: "general-purpose"`
- Context isolation (RULE-001): pass only branch names and conflict file list

**Prompt Template:**
```
You are resolving a merge conflict for epic {epicId}.

Epic branch: feat/epic-{epicId}-full-implementation
Worktree branch: feat/epic-{epicId}-{storyId}
Conflict files: {conflictFileList}

Instructions:
1. Analyze the diff from both branches for each conflict file
2. Resolve conflicts preserving the intent of both stories
3. Stage resolved files and commit the merge resolution
4. Return a JSON result:
{
  "status": "SUCCESS" | "FAILED",
  "summary": "<description of resolution or reason for failure>"
}

If the conflict is irresolvable (semantic contradictions, incompatible changes),
return status FAILED with a clear explanation.
```

**On Resolution FAILED:**
- Mark the story as FAILED
- Trigger block propagation for dependent stories (per story-0005-0007)
- Preserve the worktree for manual diagnosis (see Section 1.4d)

### 1.4d Worktree Cleanup

After the merge phase completes, clean up worktree resources:

- **SUCCESS + merged:** Worktree is cleaned up automatically after successful merge
- **FAILED stories:** Worktree is preserved for diagnostic investigation. The branch
  and worktree path are logged for manual inspection
- **No-change worktrees:** The `Agent` tool with `isolation: "worktree"` automatically
  cleans up worktrees where no changes were made

### 1.5 Result Validation (RULE-008)

After receiving the subagent response, validate the `SubagentResult` contract:

1. **`status` field**: MUST be present, MUST be one of: `SUCCESS`, `FAILED`, `PARTIAL`
2. **`findingsCount` field**: MUST be present and be a number
3. **`summary` field**: MUST be present and be a string
4. **`commitSha` field**: If `status === "SUCCESS"`, MUST be present and be a string

**On validation failure:**
- Mark the story as FAILED
- Set summary to: `"Invalid subagent result: missing {field} field"`
- Continue to checkpoint update (1.6)

[Placeholder: retry with error context — story-0005-0007]

### 1.6 Checkpoint Update (RULE-002)

After each story completes (success or failure), persist the result:

1. Call `updateStoryStatus(epicDir, storyId, update)` where update contains:
   - `status`: The validated status (`SUCCESS`, `FAILED`, or `PARTIAL`)
   - `commitSha`: The commit SHA (if status is `SUCCESS`)
   - `findingsCount`: Number of review findings from the subagent
   - `summary`: Brief description of what the subagent accomplished
2. Update metrics: increment `storiesCompleted` counter
3. The checkpoint is persisted atomically to `execution-state.json` via the checkpoint engine
4. Between story completions, the checkpoint always reflects the current execution state

### 1.7 Extension Points

The following sections are placeholders for downstream stories:

- [Placeholder: integrity gate between phases — story-0005-0006]
- [Placeholder: retry + block propagation — story-0005-0007]
- [Placeholder: resume from checkpoint — story-0005-0008]
- [Placeholder: partial execution filter — story-0005-0009]
- [Placeholder: progress reporting — story-0005-0013]

## Phase 2 — Consolidation

After all stories complete (or reach terminal state), the orchestrator runs three
sequential consolidation actions. Each action is dispatched to a clean-context
subagent (RULE-001) to keep the orchestrator's context lightweight.

### 2.1 Tech Lead Review Subagent

Dispatch a subagent that executes `x-review-pr` logic on the full epic diff:

**Subagent Configuration:**
- Tool: `Agent` with `subagent_type: "general-purpose"`
- Context isolation (RULE-001): pass only branch name and base branch

**Prompt Template:**
```
You are a Tech Lead reviewing the full diff for epic {epicId}.

Branch: feat/epic-{epicId}-full-implementation
Base: main

Execute the x-review-pr review logic on the complete diff (all commits
from all stories). Return a JSON ReviewResult:
{
  "score": "XX/40",
  "decision": "GO" | "NO-GO",
  "findings": [{ "item": "...", "severity": "...", "suggestion": "..." }]
}
```

**Result Handling:**
- On SUCCESS: record `ReviewResult` in checkpoint for report generation
- On subagent failure: log warning, continue (review is informational, not blocking)
- The review covers the COMPLETE epic diff (branch vs main), not individual stories

### 2.2 Report Generation Subagent

Dispatch a subagent that generates `epic-execution-report.md`:

**Subagent Prompt:**
```
You are generating the epic execution report for EPIC-{epicId}.

1. Read template: _TEMPLATE-EPIC-EXECUTION-REPORT.md
2. Read checkpoint: execution-state.json from the epic directory
3. Resolve ALL {{PLACEHOLDER}} tokens with real data:
   - {{EPIC_ID}}, {{BRANCH}}, {{STARTED_AT}}, {{FINISHED_AT}}
   - {{STORIES_COMPLETED}}, {{STORIES_FAILED}}, {{STORIES_BLOCKED}}, {{STORIES_TOTAL}}
   - {{COMPLETION_PERCENTAGE}}: completed/total × 100
   - {{PHASE_TIMELINE_TABLE}}: phase start/end times from checkpoint
   - {{STORY_STATUS_TABLE}}: per-story status with commit SHAs
   - {{FINDINGS_SUMMARY}}: consolidated findings from tech lead review
   - {{COVERAGE_BEFORE}}, {{COVERAGE_AFTER}}, {{COVERAGE_DELTA}}
   - {{COMMIT_LOG}}: git log main..HEAD --oneline
   - {{UNRESOLVED_ISSUES}}: findings with severity >= Medium
   - {{PR_LINK}}: populated after PR creation (or "Pending")
4. Validate: no unresolved {{...}} placeholders remain in output
5. Write epic-execution-report.md to docs/stories/epic-{epicId}/
```

**Validation:** After generation, scan the output file for any remaining `{{...}}`
patterns. If found, log a warning with the unresolved placeholder names.

### 2.3 PR Creation

Push the epic branch and create a PR via `gh` CLI:

1. **Push:** `git push -u origin feat/epic-{epicId}-full-implementation`
2. **Title format:**
   - Full completion: `feat(epic): implement EPIC-{epicId} — {title}`
   - Partial completion: `[PARTIAL] feat(epic): implement EPIC-{epicId} — {title}`
   - Include `[PARTIAL]` when completion percentage < 100%
3. **Body structure:**
   ```
   ## Summary
   - Stories completed: {completed}/{total}
   - Stories failed: {failed}
   - Stories blocked: {blocked}
   - Completion: {percentage}%

   ## Tech Lead Review
   - Score: {score} ({decision}: GO or NO-GO)

   ## Metrics
   - Line coverage: {lineCoverage}%
   - Branch coverage: {branchCoverage}%

   ## Report
   - See `docs/stories/epic-{epicId}/epic-execution-report.md`
   ```
4. **Create:** `gh pr create --title "{title}" --body "{body}" --base main`

**Error handling:** If `git push` fails (e.g., remote not accessible), log the error,
generate the report without a PR link, and persist the failure in checkpoint.

### 2.4 Partial Completion Handling

Consolidation executes regardless of whether all stories succeeded:

- If stories are FAILED: the report lists them with failure reasons and summaries
- If stories are BLOCKED: the report lists them with unsatisfied dependency chains
- The PR body explicitly indicates partial implementation with counts
- The PR title includes `[PARTIAL]` when any story is not SUCCESS
- The tech lead review still runs on whatever code was produced

### 2.5 Checkpoint Finalization

After consolidation actions complete, persist final state:

1. Register PR URL in checkpoint: `updateCheckpoint(epicDir, { prUrl })`
2. Register report path: `updateCheckpoint(epicDir, { reportPath })`
3. Set `finishedAt` timestamp
4. Persist final `execution-state.json` with all metrics

## Phase 3 — Verification

Final verification validates the epic as a whole before declaring completion.

### 3.1 Epic-Level Test Suite

Run the full test suite on the epic branch to validate cross-story integration:

1. Execute: `{{TEST_COMMAND}}` (all unit, integration, and API tests)
2. Coverage thresholds (non-negotiable): >=95% line, >=90% branch
3. If any test fails: log failures, mark epic as requiring attention
4. Record coverage results in checkpoint for the report

### 3.2 DoD Checklist Validation

Verify the Definition of Done (DoD) for the epic:

- [ ] All stories completed (or documented as FAILED/BLOCKED in report)
- [ ] Coverage thresholds met (>=95% line, >=90% branch)
- [ ] Zero compiler/linter warnings
- [ ] Tech lead review executed (Phase 2.1)
- [ ] Epic execution report generated with no unresolved placeholders (Phase 2.2)
- [ ] PR created or failure documented (Phase 2.3)
- [ ] All findings with severity >= Medium addressed or documented

### 3.3 Final Status Determination

Compute the final epic status based on story outcomes:

- **COMPLETE**: All stories reached SUCCESS status and all DoD items pass
- **PARTIAL**: Some stories FAILED or BLOCKED, but critical path stories succeeded
- **FAILED**: One or more critical path stories failed

Persist final status to checkpoint: `updateCheckpoint(epicDir, { finalStatus })`

### 3.4 Completion Output

Display the final summary to the user:

```
Epic: EPIC-{epicId} — {title}
Status: COMPLETE | PARTIAL | FAILED
Stories: {completed}/{total} completed, {failed} failed, {blocked} blocked
Coverage: line {lineCoverage}%, branch {branchCoverage}%
Tech Lead: {score} ({decision})
PR: {prUrl}
Report: docs/stories/epic-{epicId}/epic-execution-report.md
Elapsed: {totalElapsedTime}
```

Return to main branch: `git checkout main && git pull origin main`

## Integration Notes

- Invokes: `x-dev-lifecycle` (per-story execution), `x-story-map` (if map missing, via error guidance)
- Invokes: `x-review-pr` (tech lead review on full epic diff, Phase 2.1)
- Uses: `gh pr create` (PR creation with summary body, Phase 2.3)
- Reads: `_TEMPLATE-EPIC-EXECUTION-REPORT.md` (report template), `execution-state.json` (checkpoint data)
- All `{{PLACEHOLDER}}` tokens are runtime markers filled by the AI agent from project configuration — they are NOT resolved during generation
