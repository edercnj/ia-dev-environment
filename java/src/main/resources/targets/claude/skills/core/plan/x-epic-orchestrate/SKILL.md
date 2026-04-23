---
name: x-epic-orchestrate
description: "Orchestrates multi-agent planning for all stories in an epic, respecting dependency order, with checkpoint and resume support."
user-invocable: true
allowed-tools: "Read, Write, Edit, Bash, Grep, Glob, Agent, AskUserQuestion, Skill"
argument-hint: "[EPIC-ID] [--resume] [--story story-XXXX-YYYY] [--dry-run]"
---

## Global Output Policy

- **Language**: Use English for orchestration text, status/checkpoint messages, and control-flow markers. Use pt-BR for user-facing planning report content when required by later rules (including RULE-006).
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Epic Planning Orchestrator

## Purpose

Orchestrate multi-agent planning for all stories in an epic by invoking `/x-story-plan` for each story in dependency order. Parse the implementation map to determine phase ordering, dispatch planning in parallel within each phase, track planning status in `execution-state.json`, and support checkpoint/resume for interrupted runs.

## When to Use

- `/x-epic-orchestrate XXXX` -- plan all stories in an epic
- `/x-epic-orchestrate XXXX --resume` -- resume planning from last checkpoint
- `/x-epic-orchestrate XXXX --story story-XXXX-YYYY` -- plan only a specific story

## CRITICAL EXECUTION RULE

**4 phases (0-3). ALL mandatory. NEVER stop before the final phase.**

After each phase 0-2: `>>> Phase N/3 completed. Proceeding to Phase N+1...`
After Phase 3: `>>> Phase 3/3 completed. Epic planning complete.`

## Workflow Overview

```
Phase 0: PREREQUISITES         -> Parse args, validate epic dir, map, stories (inline)
Phase 1: DEPENDENCY ORDER      -> Read map, extract phases, order stories (inline)
Phase 2: PLAN LOOP             -> For each phase, invoke /x-story-plan per story (subagents)
Phase 3: REPORT                -> Generate readiness summary, update epic file (inline)
```

## Input Parsing

### Positional Argument (Required)

| Argument | Format | Required | Description |
|----------|--------|----------|-------------|
| `EPIC-ID` | `XXXX` (4-digit zero-padded) | **Mandatory** | The epic identifier, e.g., `0028` |

The epic ID is a required positional argument. If missing, abort immediately:

```
ERROR: Epic ID is required. Usage: /x-epic-orchestrate [EPIC-ID] [--resume] [--story story-XXXX-YYYY]
```

### Optional Flags

| Flag | Type | Default | Description |
|------|------|---------|-------------|
| `--resume` | boolean | `false` | Continue from last checkpoint (skip stories with `planningStatus == "READY"`) |
| `--story story-XXXX-YYYY` | string | (all stories) | Plan only a specific story by ID |
| `--dry-run` | boolean | `false` | Artifacts written to disk but Steps P1 / P2 / P4 / P5 become no-ops (EPIC-0049 / RULE-007) |

**Mutual exclusivity:** `--resume` and `--story` are mutually exclusive. If both are provided, abort:

```
ERROR: --resume and --story are mutually exclusive. Use only one.
```

---

## Step P1 — Detect Worktree Context (EPIC-0049 / RULE-001)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-epic-orchestrate Phase-P1-Worktree-Detect`

Invoke `x-git-worktree` in detect-context mode. Result is advisory only — `x-internal-epic-branch-ensure` (Step P2) makes the authoritative decision.

    Skill(skill: "x-git-worktree", args: "detect-context")

Continue on any detect-context failure (fail-open, RULE-006).

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-epic-orchestrate Phase-P1-Worktree-Detect ok`

## Step P2 — Ensure `epic/<ID>` Branch (EPIC-0049 / RULE-001)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-epic-orchestrate Phase-P2-Epic-Branch-Ensure`

Use the resolved `EPIC-ID` argument (`XXXX`).

Invoke `x-internal-epic-branch-ensure` so the canonical `epic/<ID>` branch exists locally AND on origin (idempotent):

    Skill(skill: "x-internal-epic-branch-ensure", args: "--epic-id <XXXX>")

On failure (non-zero exit), abort with `EPIC_BRANCH_ENSURE_FAILED`. When `--dry-run` is set, skip this step.

This step runs ONCE at orchestrator entry. Child `/x-story-plan` invocations in Phase 2 receive `--no-commit` so they do not re-ensure the branch nor issue per-story commits — the wave-level Step P4 below aggregates commits per wave.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-epic-orchestrate Phase-P2-Epic-Branch-Ensure ok`

---

## Phase 0 -- Prerequisites (Orchestrator -- Inline)

### 0.1 Parse Epic ID

Extract the 4-digit zero-padded epic ID from the positional argument.

- Input: `0028` -> epic ID = `0028`
- If the argument is not a 4-digit number, abort with format error:
  ```
  ERROR: Invalid epic ID format. Expected 4-digit zero-padded number (e.g., 0028).
  ```

### 0.2 Parse Flags

Check for `--resume` and `--story` flags. Validate mutual exclusivity.

### 0.3 Resolve Epic Directory

Resolve the epic directory using a glob to support suffix variants (e.g., `plans/epic-XXXX-title-slug/`):

```bash
# Resolve epic directory (exact match first, then suffix variant)
epicDir=$(ls -d plans/epic-{epicId}/ plans/epic-{epicId}-*/ 2>/dev/null | head -1)
```

If no match is found, abort:

```
ERROR: Directory plans/epic-{epicId}/ (or suffix variant) not found. Run /x-epic-decompose first.
```

Use the resolved `epicDir` path for ALL subsequent reads/writes (IMPLEMENTATION-MAP.md, stories, execution-state.json, reports).

### 0.4 Validate Implementation Map

Check that `IMPLEMENTATION-MAP.md` exists in the epic directory.

```
ERROR: IMPLEMENTATION-MAP.md not found in plans/epic-{epicId}/. Run /x-epic-map first.
```

### 0.5 Validate Story Files

Glob for `story-XXXX-*.md` files in the epic directory. At least one must exist.

```
ERROR: No story files found matching story-{epicId}-*.md in plans/epic-{epicId}/.
```

### 0.6 Resume Checkpoint Validation (Conditional)

If `--resume` flag is set, check that `execution-state.json` exists in the epic directory.

```
ERROR: No checkpoint found (execution-state.json missing). Cannot resume. Run without --resume.
```

If `--resume` is set and `execution-state.json` exists, load it and apply reclassification:

| Current `planningStatus` | New `planningStatus` | Rationale |
|--------------------------|----------------------|-----------|
| `READY` | `READY` | Preserved -- skip this story |
| `NOT_READY` | `PENDING` | Re-attempt planning |
| `IN_PROGRESS` | `PENDING` | Interrupted -- reset for retry |
| `PENDING` | `PENDING` | No change |

### 0.7 Create Plans Directory

Create `{epicDir}/plans/` if it does not exist:

```bash
mkdir -p {epicDir}/plans
```

Log: `"Created plans/ directory for EPIC-{epicId}"` if created. Skip silently if already exists.

>>> Phase 0/3 completed. Proceeding to Phase 1...

---

## Phase 1 -- Dependency Order (Orchestrator -- Inline)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-epic-orchestrate Phase-1-Discovery`

### 1.1 Read Implementation Map

Read `{epicDir}/IMPLEMENTATION-MAP.md` and extract phases and dependencies from two sections:

1. **Dependency declarations** from **Section 1 — Dependency Matrix**:

```markdown
| Story | Título | Chave Jira | Blocked By | Blocks | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| story-XXXX-0001 | Title | KEY-1 | — | story-XXXX-0002 | Pendente |
| story-XXXX-0002 | Title | KEY-2 | story-XXXX-0001 | — | Pendente |
```

Parse the `Blocked By` and `Blocks` columns to build the dependency graph.

2. **Phase assignments** from **Section 5 — Resumo por Fase**:

```markdown
| Fase | Histórias | Camada | Paralelismo | Pré-requisito |
| :--- | :--- | :--- | :--- | :--- |
| 0 | story-XXXX-0001 | Domain | 1 paralela | — |
| 1 | story-XXXX-0002, story-XXXX-0003 | Application | 2 paralelas | Fase 0 concluída |
| 2 | story-XXXX-0004 | Adapter | 1 paralela | Fase 1 concluída |
```

Parse the `Fase` and `Histórias` columns to determine phase grouping. Extract total phases from the number of distinct `Fase` values.

### 1.2 Order Stories by Phase

1. Group stories by their phase number
2. Sort phases numerically: Phase 0, Phase 1, Phase 2, ...
3. Within each phase, stories can be planned in **parallel** (no inter-phase dependencies within the same phase)

### 1.3 Single-Story Mode (Conditional)

If `--story` flag is provided:

1. Locate the specified story in the implementation map
2. If not found, abort:
   ```
   ERROR: Story {storyId} not found in implementation map.
   ```
3. Identify the story's dependencies from the implementation map
4. Validate that all dependencies have `planningStatus == "READY"` in `execution-state.json`:
   - If `execution-state.json` does not exist, check if dependency story files have existing planning artifacts (tasks file exists at `{epicDir}/plans/tasks-story-XXXX-YYYY.md`)
   - If any dependency is not satisfied, abort:
     ```
     ERROR: Dependencies not satisfied for {storyId}: [{unsatisfied deps list}]. Plan dependencies first.
     ```
5. If all dependencies are satisfied, set the plan loop to process only this story

### 1.4 Initialize Execution State

If `execution-state.json` does not exist in `{epicDir}/`, create it using the same top-level schema as `/x-epic-implement`, adding planning-specific fields under each story entry:

```json
{
  "epicId": "XXXX",
  "baseBranch": "develop",
  "startedAt": "{ISO-8601 timestamp}",
  "totalPhases": {count},
  "stories": [
    {"id": "story-XXXX-0001", "phase": 0},
    {"id": "story-XXXX-0002", "phase": 1}
  ],
  "storyEntries": {
    "story-XXXX-0001": {
      "id": "story-XXXX-0001",
      "phase": 0,
      "planningStatus": "PENDING",
      "planningStartedAt": null,
      "planningCompletedAt": null,
      "dorVerdict": null,
      "artifacts": []
    }
  }
}
```

**Schema compatibility with `/x-epic-implement`:** The `stories` array and `baseBranch` field match the schema expected by `/x-epic-implement`. Planning-specific fields (`planningStatus`, `dorVerdict`, `artifacts`) are added under `storyEntries` alongside where `/x-epic-implement` adds its own fields (`status`, `commitSha`, `prUrl`, etc.). Both skills read/write the same file without conflict.

If `execution-state.json` already exists (either from `--resume` or prior partial run), preserve existing data and only add `storyEntries` for stories not yet tracked. Never overwrite fields owned by `/x-epic-implement`.

**Per-story planning fields in `storyEntries`:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | String | Yes | Story ID (e.g., `story-0028-0001`) |
| `phase` | Integer | Yes | Phase number from implementation map |
| `planningStatus` | String | Yes | `PENDING`, `IN_PROGRESS`, `READY`, `NOT_READY` |
| `planningStartedAt` | String | When IN_PROGRESS | ISO-8601 timestamp of planning start |
| `planningCompletedAt` | String | When READY/NOT_READY | ISO-8601 timestamp of planning completion |
| `dorVerdict` | String | When completed | `READY` or `NOT_READY` from `/x-story-plan` output |
| `artifacts` | String[] | When completed | List of generated artifact paths |

Log the execution plan:

```
Epic Planning -- EPIC-{epicId}
Total stories: {count}
Total phases: {count}
Mode: {all | resume | single-story}
Execution order:
  Phase 0: story-XXXX-0001
  Phase 1: story-XXXX-0002, story-XXXX-0003
  Phase 2: story-XXXX-0004
```

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-epic-orchestrate Phase-1-Discovery ok`

>>> Phase 1/3 completed. Proceeding to Phase 2...

---

## Phase 2 -- Plan Loop (Orchestrator -- Dispatches Subagents)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-epic-orchestrate Phase-2-Story-Orchestration`

Execute planning for each story in dependency order, phase by phase.

### 2.1 Phase-by-Phase Execution

For each phase (0..N), in order:

```
For each phase in (0..totalPhases-1):
  1. Collect all stories in this phase
  2. Filter out stories with planningStatus == "READY" (already planned)
  3. For each remaining story in this phase:
     a. Update execution-state.json: planningStatus = "IN_PROGRESS", planningStartedAt = now()
     b. Dispatch /x-story-plan via Agent subagent
     c. Collect DoR verdict from subagent result
     d. Update execution-state.json with result
  4. Log phase completion summary
```

### 2.2 Subagent Dispatch

**CONTEXT ISOLATION: You receive only metadata. Read all files yourself.
Do NOT expect source code, diffs, or knowledge pack content in this prompt.
The subagent reads all story files, KPs, and references independently.**

For each story to plan, invoke `/x-story-plan` via the Skill tool.

**Telemetry around per-story dispatch (story-0040-0007 §3.2):** Before each
subagent invocation emit a `subagent.start` marker with the story ID as the
role; after the subagent returns emit a `subagent.end` marker carrying the
outcome status. The role value is the full story ID (e.g., `story-0028-0001`)
so the telemetry analysis can group per-story planning latency:

<!-- TELEMETRY: subagent.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-start x-epic-orchestrate {storyId}`

**Skill invocation:**

```
Skill(skill: "x-story-plan", args: "{storyId} --no-commit")
```

`--no-commit` is always propagated (EPIC-0049 / RULE-007 batch-commit contract). Each child `x-story-plan` writes artifacts to disk but SKIPS its own P4 commit. The wave-level **Step P4 — Commit Wave Artifacts** (below, added to Phase 2.5) aggregates every story's artifacts + `execution-state.json` + reports into a single commit per wave.

<!-- TELEMETRY: subagent.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-end x-epic-orchestrate {storyId} ok`

The skill executes all 6 phases of `/x-story-plan` (Input Resolution, Context Gathering, Parallel Planning, Consolidation, Artifact Generation, DoR Validation) and returns a result containing the DoR verdict.

If `/x-story-plan` is unavailable via the Skill tool, fall back to the Agent tool:
```
Agent(prompt: "/x-story-plan {storyId} --no-commit")
```

**Parallel dispatch within a phase:**

Stories within the same phase have no inter-dependencies, so they CAN be dispatched in parallel using multiple Agent calls in a single message. However, to manage context window and memory pressure:

- If the phase contains <= 3 stories: dispatch all in parallel (single message with multiple Agent calls)
- If the phase contains > 3 stories: dispatch in batches of 3

### 2.3 Collect Results

After each subagent completes, extract the DoR verdict from the output:

1. Search the subagent output for the DoR verdict line: `Verdict: **READY**` or `Verdict: **NOT_READY**`
2. If the verdict cannot be extracted, check for the existence of the DoR file at `{epicDir}/plans/dor-story-XXXX-YYYY.md` and read the verdict from it
3. If neither source provides a verdict, set `planningStatus = "NOT_READY"` and log a warning:
   ```
   WARNING: Could not extract DoR verdict for {storyId}. Marking as NOT_READY.
   ```

### 2.4 Update Execution State

After each story planning completes, update the story's entry in `storyEntries`:

```json
{
  "id": "story-XXXX-YYYY",
  "phase": N,
  "planningStatus": "READY",
  "planningStartedAt": "2026-04-07T10:00:00Z",
  "planningCompletedAt": "2026-04-07T10:05:00Z",
  "dorVerdict": "READY",
  "artifacts": [
    "{epicDir}/plans/tasks-story-XXXX-YYYY.md",
    "{epicDir}/plans/planning-report-story-XXXX-YYYY.md",
    "{epicDir}/plans/dor-story-XXXX-YYYY.md"
  ]
}
```

Map DoR verdict to planningStatus:

| DoR Verdict | planningStatus |
|-------------|----------------|
| `READY` | `READY` |
| `NOT_READY` | `NOT_READY` |
| Error/timeout | `NOT_READY` |

Write `execution-state.json` atomically after each story update. This ensures checkpoint consistency even if the process is interrupted.

### 2.5 Phase Completion Log

After all stories in a phase are planned:

```
Phase {N} planning complete:
  story-XXXX-0001: READY
  story-XXXX-0002: NOT_READY (3 blockers)
  Phase {N} result: {passed}/{total} stories READY
```

### 2.5b Step P4 — Commit Wave Planning Artifacts (EPIC-0049 / RULE-007)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-epic-orchestrate Phase-P4-Wave-Commit`

If `--dry-run` is set, log `"dry-run, skipping commit"` and skip this step.

After all stories in the current wave (phase) have completed planning and `execution-state.json` has been updated, issue a **single consolidated commit** covering:

1. `execution-state.json` (wave-level checkpoint update)
2. Every story file touched during the wave (Section 8 updates via status flip)
3. Every planning artifact produced by the wave's stories:
   - `plans/epic-XXXX/plans/tasks-story-XXXX-YYYY.md`
   - `plans/epic-XXXX/plans/planning-report-story-XXXX-YYYY.md`
   - `plans/epic-XXXX/plans/dor-story-XXXX-YYYY.md`
   - `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md` (if present)
   - `plans/epic-XXXX/plans/task-TASK-*.md` (v2)
   - `plans/epic-XXXX/plans/plan-task-TASK-*.md` (v2)
   - `plans/epic-XXXX/plans/task-implementation-map-STORY-*.md` (v2)
4. Any reports written during the wave (`plans/epic-XXXX/reports/**` — added in Phase 3 but may accumulate incrementally).

Delegate to `x-planning-commit`:

    Skill(skill: "x-planning-commit",
          args: "--scope chore --epic-id <XXXX> --paths plans/epic-<XXXX>/execution-state.json plans/epic-<XXXX>/story-<XXXX>-*.md plans/epic-<XXXX>/plans/ plans/epic-<XXXX>/reports/ --subject \"planning orchestration cycle (wave <N>)\"")

Where `<N>` is the current phase number (0-based per Phase 1 — or the wave sequence number when a multi-wave orchestration runs phase-by-phase).

Cenario enforced (story-0049-0022): `"1 commit 'chore(epic-<XXXX>): planning orchestration cycle (wave <N>)' é criado"` per completed wave.

Idempotency: re-executing with identical inputs produces `commitSha=null` (silent no-op). On `COMMIT_FAILED` (exit 4), abort with the same code.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-epic-orchestrate Phase-P4-Wave-Commit ok`

### 2.6 Error Handling

If a subagent fails (throws error, times out, or returns no output):

1. Set `planningStatus = "NOT_READY"` for the story
2. Set `dorVerdict = null`
3. Log the error:
   ```
   ERROR: Planning failed for {storyId}: {error message}
   ```
4. Continue with the next story (do NOT abort the entire epic planning)

Stories in subsequent phases that depend on a `NOT_READY` story are still planned (planning is about generating artifacts, not about runtime dependencies). The `NOT_READY` status is informational -- it indicates the story may need attention before implementation begins.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-epic-orchestrate Phase-2-Story-Orchestration ok`

>>> Phase 2/3 completed. Proceeding to Phase 3...

---

## Phase 3 -- Report (Orchestrator -- Inline)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-epic-orchestrate Phase-3-Consolidation`

### 3.1 Generate Readiness Summary

Read the final `execution-state.json` and compute:

| Metric | Calculation |
|--------|-------------|
| `stories_planned` | Count of stories with `planningStatus != "PENDING"` |
| `stories_total` | Total number of stories in the epic |
| `stories_ready` | Count of stories with `planningStatus == "READY"` |
| `stories_not_ready` | Count of stories with `planningStatus == "NOT_READY"` |
| `stories_pending` | Count of stories with `planningStatus == "PENDING"` |
| `overall_status` | See determination table below |

**Overall status determination:**

| Condition | `overall_status` |
|-----------|------------------|
| All stories have `planningStatus == "READY"` | `READY` |
| At least one story is `READY` and at least one is `NOT_READY` or `PENDING` | `PARTIALLY_READY` |
| No stories are `READY` | `NOT_READY` |

### 3.2 Update Epic File

Read the epic file at `{epicDir}/EPIC-XXXX.md` (or `{epicDir}/epic-XXXX.md` -- case-insensitive glob).

Locate the story index table in Section 5 (or the main story listing). Add or update a `Planning` column:

```markdown
| # | Story ID | Title | Status | Planning |
|---|----------|-------|--------|----------|
| 1 | story-XXXX-0001 | Title | Draft | READY |
| 2 | story-XXXX-0002 | Title | Draft | NOT_READY |
| 3 | story-XXXX-0003 | Title | Draft | PENDING |
```

**Rules for updating the table:**

1. If a `Planning` column already exists, update the values
2. If no `Planning` column exists, add it as the last column
3. Preserve all existing columns and data
4. Only update rows for stories that were planned in this run (preserve existing values for stories not in scope)

### 3.3 Generate Planning Report

Write a planning summary to `{epicDir}/reports/epic-planning-report-XXXX.md`:

```markdown
# Epic Planning Report -- EPIC-{epicId}

> **Epic ID:** EPIC-{epicId}
> **Date:** {currentDate}
> **Total Stories:** {stories_total}
> **Stories Planned:** {stories_planned}
> **Overall Status:** {overall_status}

## Readiness Summary

| Metric | Count |
|--------|-------|
| Stories Total | {stories_total} |
| Stories Planned | {stories_planned} |
| Stories Ready (DoR READY) | {stories_ready} |
| Stories Not Ready (DoR NOT_READY) | {stories_not_ready} |
| Stories Pending | {stories_pending} |

## Per-Story Results

| # | Story ID | Phase | Planning Status | DoR Verdict | Duration |
|---|----------|-------|-----------------|-------------|----------|
| 1 | story-XXXX-0001 | 0 | READY | READY | 45s |
| 2 | story-XXXX-0002 | 1 | NOT_READY | NOT_READY | 38s |
| ... | ... | ... | ... | ... | ... |

## Blockers (if any)

{List stories with NOT_READY verdict and their failing DoR checks, if available from the DoR checklist files}

## Generated Artifacts

{List all artifact files generated during this planning run}
```

### 3.4 Staleness Check for Report (RULE-002)

Before generating the planning report:

1. Compute report path: `{epicDir}/reports/epic-planning-report-XXXX.md`
2. Check if the report file exists:
   - If NOT found: generate new. Log: `"Generating planning report for EPIC-{epicId}"`
   - If found, compare modification times:
     - `mtime(execution-state.json) <= mtime(report)` -> reuse existing report. Log: `"Reusing existing planning report from {date}"`
     - `mtime(execution-state.json) > mtime(report)` -> regenerate. Log: `"Regenerating planning report (execution state modified)"`

### 3.5 Console Summary

Log the final summary to console:

```
=== Epic Planning Complete ===

EPIC-{epicId}: {overall_status}

  Stories: {stories_planned}/{stories_total} planned
  Ready:   {stories_ready}
  Not Ready: {stories_not_ready}
  Pending: {stories_pending}

  Report: plans/epic-{epicId}/reports/epic-planning-report-{epicId}.md
  State:  plans/epic-{epicId}/execution-state.json
```

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-epic-orchestrate Phase-3-Consolidation ok`

## Step P5 — Push Epic Branch to Origin (optional, EPIC-0049)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-epic-orchestrate Phase-P5-Push`

If `--dry-run` is set, log `"dry-run, skipping push"` and skip.

Delegate the push of the canonical `epic/<XXXX>` branch to origin:

    Skill(skill: "x-git-push", args: "--branch epic/<XXXX>")

On push failure (remote rejection, no connectivity), log a WARNING and continue — wave commits are preserved locally. Do NOT abort.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-epic-orchestrate Phase-P5-Push ok`

>>> Phase 3/3 completed. Epic planning complete.

---

## Checkpoint/Resume Protocol

### Checkpoint Mechanism

`execution-state.json` in `{epicDir}/` serves as the checkpoint file. It is updated atomically after each story completes planning.

### Resume Behavior (`--resume`)

When `--resume` is set:

1. Load `execution-state.json` from `{epicDir}/`
2. Apply reclassification (see Phase 0.6)
3. Stories with `planningStatus == "READY"` are **skipped** (artifacts already generated)
4. Stories with `planningStatus == "IN_PROGRESS"` are **reset to PENDING** (interrupted work)
5. Stories with `planningStatus == "NOT_READY"` are **reset to PENDING** (re-attempt)
6. Stories with `planningStatus == "PENDING"` proceed normally
7. Enter Phase 2 plan loop with the filtered story set

### Idempotency

- Running `/x-epic-orchestrate XXXX` twice without `--resume` regenerates all artifacts (full run)
- Running `/x-epic-orchestrate XXXX --resume` after a partial run only plans stories not yet `READY`
- `/x-story-plan` itself has a staleness check (RULE-002): if source files have not changed, it reuses existing artifacts

---

## Output Artifacts

| Artifact | Path | Produced By |
|----------|------|-------------|
| Execution state (checkpoint) | `{epicDir}/execution-state.json` | x-epic-orchestrate (this skill) |
| Epic planning report | `{epicDir}/reports/epic-planning-report-XXXX.md` | x-epic-orchestrate Phase 3 |
| Epic file update (Planning column) | `{epicDir}/EPIC-XXXX.md` | x-epic-orchestrate Phase 3 |
| Task breakdown (per story) | `{epicDir}/plans/tasks-story-XXXX-YYYY.md` | x-story-plan (subagent) |
| Task plans (per task per story) | `{epicDir}/plans/task-plan-TASK-NNN-story-XXXX-YYYY.md` | x-story-plan (subagent) |
| Planning report (per story) | `{epicDir}/plans/planning-report-story-XXXX-YYYY.md` | x-story-plan (subagent) |
| DoR checklist (per story) | `{epicDir}/plans/dor-story-XXXX-YYYY.md` | x-story-plan (subagent) |
| Story file update (Section 8) | `{epicDir}/story-XXXX-YYYY.md` | x-story-plan (subagent) |

---

## Flat File Naming Convention (RULE-004)

All output files follow the flat naming convention under `{epicDir}/plans/`:

```
{epicDir}/
  EPIC-XXXX.md
  IMPLEMENTATION-MAP.md
  story-XXXX-0001.md
  story-XXXX-0002.md
  execution-state.json
  plans/
    tasks-story-XXXX-0001.md
    tasks-story-XXXX-0002.md
    planning-report-story-XXXX-0001.md
    planning-report-story-XXXX-0002.md
    dor-story-XXXX-0001.md
    dor-story-XXXX-0002.md
    task-plan-TASK-001-story-XXXX-0001.md
    task-plan-TASK-002-story-XXXX-0001.md
  reports/
    epic-planning-report-XXXX.md
```

---

## Error Handling

| Error | Action | Recovery |
|-------|--------|----------|
| Epic directory not found | Abort with error message | Run `/x-epic-decompose` first |
| Implementation map not found | Abort with error message | Run `/x-epic-map` first |
| No story files found | Abort with error message | Run `/x-story-create` first |
| `execution-state.json` missing on `--resume` | Abort with error message | Run without `--resume` |
| `--resume` and `--story` both set | Abort with error message | Use only one flag |
| Story not in implementation map (`--story`) | Abort with error message | Check story ID |
| Dependencies not satisfied (`--story`) | Abort with error message | Plan dependencies first |
| Subagent fails for a story | Mark `NOT_READY`, continue | Fix issues, re-run with `--resume` |
| DoR verdict cannot be extracted | Mark `NOT_READY`, log warning | Check subagent output manually |
| `x-internal-epic-branch-ensure` fails (Step P2) | Abort with `EPIC_BRANCH_ENSURE_FAILED` | Repair remote state; re-run |
| `x-planning-commit` exit 4 (Step P4 wave commit) | Abort wave with `COMMIT_FAILED` | Inspect conflict; re-run with `--resume` |
| `x-planning-commit` exit 0 + `noOp=true` (Step P4) | Silent no-op — continue to next wave | — |
| `x-git-push` fails (Step P5) | WARN only; local commits preserved | Operator `git push` manually |
| `--dry-run` set | Steps P1 / P2 / P4 / P5 become no-ops | Re-run without `--dry-run` to version |

---

## Integration with x-epic-implement

The `execution-state.json` produced by `/x-epic-orchestrate` uses the same top-level schema as `/x-epic-implement` (including `baseBranch`, `stories` array, and `totalPhases`). Planning-specific fields are stored under `storyEntries` alongside implementation fields:

| Field | Used By | Values |
|-------|---------|--------|
| `status` | x-epic-implement | `PENDING`, `IN_PROGRESS`, `SUCCESS`, `FAILED`, `PARTIAL`, `BLOCKED` |
| `planningStatus` | x-epic-orchestrate | `PENDING`, `IN_PROGRESS`, `READY`, `NOT_READY` |

Both fields coexist on the same story entry in `storyEntries`. `/x-epic-implement` can use `planningStatus == "READY"` as a pre-condition gate before dispatching a story for implementation. Neither skill overwrites fields owned by the other.

---

## Skills Dependency Graph

```
/x-epic-orchestrate (this skill)
  |
  |-- Step P1: x-git-worktree detect-context          (advisory)
  |-- Step P2: x-internal-epic-branch-ensure           (canonical branch)
  |-- reads: IMPLEMENTATION-MAP.md (phase/dependency graph)
  |-- reads: EPIC-XXXX.md (epic context)
  |-- reads: story-XXXX-*.md (story files)
  |-- reads/writes: execution-state.json (checkpoint)
  |
  +-- per story (subagent): /x-story-plan --no-commit
  |     |
  |     +-- 5 parallel subagents: Architect, QA, Security, Tech Lead, PO
  |
  |-- Step P4 (per wave): x-planning-commit            (batch commit per wave)
  +-- Step P5 (end):      x-git-push                   (push epic branch)
```

---

## Constraints

- **Memory management**: Limit parallel subagent dispatch to 3 stories per batch to avoid OOM (each `/x-story-plan` spawns 5 internal subagents)
- **Checkpoint atomicity**: Always write `execution-state.json` after each story completes, never batch-update
- **Staleness check (RULE-002)**: `/x-story-plan` handles its own staleness check; `/x-epic-orchestrate` only checks for the planning report
- **Content language (RULE-006)**: User-facing planning report content in pt-BR; technical fields and log messages in English
