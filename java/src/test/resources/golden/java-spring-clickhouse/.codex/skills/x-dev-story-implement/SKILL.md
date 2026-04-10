---
name: x-dev-story-implement
description: "Orchestrates the complete feature implementation cycle with task-centric workflow: branch creation, planning, per-task TDD execution with individual PRs and approval gates, story-level verification, and final cleanup. Delegates implementation to x-test-tdd, commits to x-git-commit, PRs to x-pr-create."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill
argument-hint: "[STORY-ID or feature-name] [--auto-approve-pr] [--task TASK-ID] [--skip-verification] [--full-lifecycle]"
context-budget: medium
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Feature Lifecycle (Task-Centric Orchestrator)

## Purpose

Orchestrate the complete feature implementation cycle using a task-centric workflow. Each task in the story produces its own branch, PR, and approval gate. The lifecycle delegates TDD implementation to `x-test-tdd`, commits to `x-git-commit`, and PR creation to `x-pr-create`. Phase 2 is a Task Execution Loop; Phase 3 is Story-Level Verification that absorbs the old review/fix/PR/verification phases.

## When to Use

- Full story/feature implementation with review cycle
- End-to-end development workflow (plan -> code -> review -> PR)
- Task-centric delivery with per-task PRs and approval gates

## CLI Arguments

| Argument | Type | Default | Description |
|----------|------|---------|-------------|
| `--auto-approve-pr` | Boolean | false | Activate auto-approve mode with parent branch (RULE-004) |
| `--task` | String | -- | Execute only a specific task (TASK-XXXX-YYYY-NNN) |
| `--skip-verification` | Boolean | false | Skip Phase 3 (story-level verification) |
| `--full-lifecycle` | Boolean | false | Force full execution regardless of scope tier |

## CRITICAL EXECUTION RULE

**3 phases (0-2) + optional Phase 3. ALL phases through Phase 2 mandatory. NEVER stop before Phase 2 completes.**

After Phase 0: `>>> Phase 0 completed. Proceeding to Phase 1...`
After Phase 1: `>>> Phase 1 completed. Proceeding to Phase 2 (Task Execution Loop)...`
After Phase 2: `>>> Phase 2 completed. All tasks executed. Proceeding to Phase 3 (Verification)...`
After Phase 3: `>>> Phase 3 completed. Lifecycle complete.`

## On-Demand References

| Condition | Reference File | Content |
|-----------|---------------|---------|
| Phase 0 scope assessment | `references/scope-assessment.md` | SIMPLE/STANDARD/COMPLEX classification |
| Phase 1 planning details | `references/planning-phases.md` | Phase 1A-1F subagent prompts |
| Phase 3 verification | `references/verification-phase.md` | Coverage, review, fixes, PR, tech lead, cleanup |
| REST documentation | `references/openapi-generator.md` | OpenAPI/Swagger generation |

> **RULE-002 (Graceful Degradation):** If a reference file is not found, log `"WARNING: Reference {filename} not found"` and continue. The core workflow is self-sufficient for basic execution.

### RULE-001: Task as Unit of Delivery

Each task = 1 branch = 1 PR. Tasks are the atomic unit of delivery:

| Concern | Skill | Invoked By |
|---------|-------|------------|
| TDD implementation | `x-test-tdd` | Phase 2 (per task) |
| Atomic commits | `x-git-commit` | `x-test-tdd` (per TDD cycle) |
| PR creation | `x-pr-create` | Phase 2 (per task) |
| Code formatting | `x-code-format` | `x-git-commit` (pre-commit chain) |
| Code linting | `x-code-lint` | `x-git-commit` (pre-commit chain) |
| Task planning | `x-task-plan` | Phase 1 (if not PRE_PLANNED) |

## Workflow Overview

```
Phase 0: Preparation             (orchestrator -- inline, includes artifact pre-checks and resume)
{% if has_contract_interfaces == 'True' %}Phase 0.5: API-First Contract     (orchestrator -- conditional, pauses for approval)
{% endif %}Phase 1: Planning                 -> Read references/planning-phases.md
Phase 1B-1F: Parallel Planning    (up to 5 subagents -- SINGLE message, skips reusable artifacts)
Phase 2: Task Execution Loop      (for each task: branch -> x-test-tdd -> x-pr-create -> approval gate)
Phase 3: Story-Level Verification -> Read references/verification-phase.md
```

---

## Phase 0 -- Preparation (Orchestrator -- Inline)

1. Read story file and extract acceptance criteria, sub-tasks, dependencies
2. Verify dependencies (predecessor stories complete)
3. **Artifact Pre-checks (RULE-002 -- Idempotency via Staleness Check):**
   For each artifact type, check existence and staleness. If `mtime(story) <= mtime(plan)`, reuse. If stale or missing, mark for (re)generation.

   | # | Artifact Type | File Pattern | Phase |
   |---|---------------|--------------|-------|
   | 1 | Test Plan | `plans/epic-XXXX/plans/tests-story-XXXX-YYYY.md` | 1B |
   | 2 | Architecture Plan | `plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md` | 1A |
   | 3 | Implementation Plan | `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md` | 1B |
   | 4 | Task Breakdown | `plans/epic-XXXX/plans/tasks-story-XXXX-YYYY.md` | 1C |
   | 5 | Security Assessment | `plans/epic-XXXX/plans/security-story-XXXX-YYYY.md` | 1E |
   | 6 | Compliance Assessment | `plans/epic-XXXX/plans/compliance-story-XXXX-YYYY.md` | 1F |

4. Extract epic ID from story ID (e.g., `story-0001-0003` -> epic ID `0001`)
5. Ensure directories exist: `mkdir -p plans/epic-XXXX/plans plans/epic-XXXX/reviews`
6. **Branch Creation:**
   - If `--auto-approve-pr`: create parent branch `feat/story-XXXX-YYYY-desc` from `develop`. Task branches target this parent.
   - If NOT `--auto-approve-pr`: no parent branch. Task branches target `develop`.
7. **Scope Assessment** -- Read `references/scope-assessment.md` and classify the story

### Phase 0.4 -- Planning Mode Detection

Check for pre-existing planning artifacts (7 types including per-task plans):

| Mode | Condition | Behavior |
|------|-----------|----------|
| PRE_PLANNED | All 7 types exist and fresh | Skip Phase 1A-1F. Phase 2 uses task plans directly. |
| HYBRID | Some artifacts exist | Generate ONLY missing artifacts in Phase 1. |
| INLINE | No artifacts exist | Execute Phase 1A-1F (backward compatible). |

### Phase 0.5 -- Resume Detection (RULE-014)

Check for existing `execution-state.json`. If found, reclassify task statuses by checking actual PR state via `gh pr view`:

| Current Status | Condition | New Status |
|----------------|-----------|------------|
| IN_PROGRESS | No PR found | PENDING |
| IN_PROGRESS | PR open | PR_CREATED |
| PR_CREATED | PR approved | PR_APPROVED |
| PR_CREATED | PR merged | DONE |
| PR_CREATED | PR closed | FAILED |
| BLOCKED | Dependencies resolved | PENDING |
| BLOCKED | Still blocked | BLOCKED (keep) |
| DONE | -- | DONE (skip) |

Tasks with DONE are skipped in Phase 2. If no execution-state exists, initialize all tasks as PENDING.

{% if has_contract_interfaces == 'True' %}

## Phase 0.5 -- API-First Contract Generation (Orchestrator -- Conditional)

> **RULE-005:** Formal contract before implementation.

**Activation:** Only when story declares `rest`, `grpc`, `event-consumer`, `event-producer`, or `websocket` interfaces.

### Step 0.5.1 -- Interface Detection

Read the story file and identify declared interface types:

| Interface Type | Contract Format | Output Path |
|---------------|----------------|-------------|
| `rest` | OpenAPI 3.1 | `contracts/{STORY_ID}-openapi.yaml` |
| `grpc` | Protobuf 3 | `contracts/{STORY_ID}.proto` |
| `event-consumer` | AsyncAPI 2.6 | `contracts/{STORY_ID}-asyncapi.yaml` |
| `event-producer` | AsyncAPI 2.6 | `contracts/{STORY_ID}-asyncapi.yaml` |
| `websocket` | AsyncAPI 2.6 | `contracts/{STORY_ID}-asyncapi.yaml` |

### Step 0.5.2-3 -- Contract Generation and Validation

Generate draft contract from story data contracts. Invoke `/x-test-contract-lint {CONTRACT_PATH}` to validate.

### Step 0.5.4 -- Approval Gate

```
CONTRACT PENDING APPROVAL

Contract generated: {CONTRACT_PATH}
Format: {CONTRACT_FORMAT}
Status: PENDING_APPROVAL
```

On APPROVE: proceed to Phase 1. On REJECT: regenerate with feedback.
{% endif %}

## Phase 1 -- Planning

> **Reference:** Read `references/planning-phases.md` for full Phase 1A-1F details.

**Phase 1A:** Architecture plan via `/x-dev-architecture-plan` (conditional on scope)
**Phase 1B:** Implementation plan via subagent (Senior Architect)
**Phases 1B-1F (parallel -- SINGLE message):** Test plan, task decomposition, event schema, security assessment, compliance assessment.

## Phase 2 -- Task Execution Loop (RULE-001: 1 Task = 1 Branch = 1 PR)

### Step 2.0 -- Read Task List

1. Read tasks from story Section 8 or task breakdown file
2. Parse into ordered list respecting dependency declarations
3. If `--task TASK-XXXX-YYYY-NNN` specified, filter to only that task
4. **Backward compatibility:** If no formal tasks, treat entire story as single implicit task

### Step 2.1 -- Initialize Execution State

Create or update `plans/epic-XXXX/execution-state.json` with task-level tracking (status, branch, prNumber, prUrl, timestamps per task).

### Step 2.2 -- Task Execution Loop

```
FOR each TASK-NNN where status != DONE and status != BLOCKED:
  2.2.1  Check dependencies: if any dep not DONE -> mark BLOCKED, skip
  2.2.2  Update execution-state: task.status = IN_PROGRESS
  2.2.3  Create task branch from parent (--auto-approve-pr) or develop
  2.2.4  Read task plan (if PRE_PLANNED mode)
  2.2.5  Invoke /x-test-tdd TASK-XXXX-YYYY-NNN: read only the "## Slim Mode" section of x-test-tdd, x-git-commit, x-code-format, x-code-lint for minimum context
  2.2.6  Push branch: git push -u origin feat/task-XXXX-YYYY-NNN-desc
  2.2.7  Invoke /x-pr-create TASK-XXXX-YYYY-NNN
  2.2.8  Update execution-state: task.status = PR_CREATED
  2.2.9  APPROVAL GATE:
         --auto-approve-pr: auto-merge into parent branch, status = PR_MERGED
         Otherwise: AskUserQuestion (APPROVE/REJECT/PAUSE)
  2.2.10 Update execution-state with final status and completedAt
END FOR
```

### Auto-Approve Mode (--auto-approve-pr, RULE-004)

1. Parent branch `feat/story-XXXX-YYYY-desc` created from `develop` in Phase 0
2. Each task PR targets the parent branch (NOT `develop`)
3. Task PRs auto-merged into parent without approval gate
4. Parent branch **NEVER** auto-merges into `develop` -- requires human review PR
5. Phase 3 creates story-level PR from parent to `develop`

### G1-G7 Fallback (No Test Plan / No Tasks)

If no TPP-marked test plan and no formal tasks: use legacy G1-G7 group-based implementation as single implicit task. Emit warning.

## Phase 3 -- Story-Level Verification

> **Reference:** Read `references/verification-phase.md` for full Phase 3 details.

**Step 3.1:** Coverage consolidation (line >= 95%, branch >= 90%)
**Step 3.2:** Cross-file consistency check
**Step 3.3:** Documentation update (interface-specific generators + changelog)
**Step 3.4:** Review via `/x-review` + consolidated dashboard
**Step 3.5:** Fix ALL review findings with TDD discipline
**Step 3.6:** Tech Lead review via `/x-review-pr` (max 2 re-review cycles)
**Step 3.7:** Story-level PR (auto-approve mode only)
**Step 3.8:** Final verification + cleanup (IMPLEMENTATION-MAP, story status, Jira sync, DoD checklist)

**Phase 3 is the ONLY legitimate stopping point.**

## Roles and Models (Adaptive)

| Role | Phase | Tier |
|------|-------|------|
| Architect | Phase 1 | Senior |
| Task Decomposer | Phase 1C | Mid |
| Developer (via x-test-tdd) | Phase 2 | Adaptive (per task) |
| Specialist Reviews | Phase 3.4 | Adaptive |
| Tech Lead | Phase 3.6 | Adaptive |

## Error Classification

When a tool call or subagent fails, classify the error before deciding on recovery action:

| Category | Detection Patterns (case-insensitive) | Action |
|----------|---------------------------------------|--------|
| **TRANSIENT** | `"overloaded"`, `"capacity"`, `"rate limit"`, `"429"`, `"timeout"`, `"ETIMEDOUT"`, `"503"`, `"504"`, `"502"` | Retry up to 3x with exponential backoff (1s, 2s, 4s + jitter). For timeouts, retry up to 2x. |
| **CONTEXT** | `"context"`, `"token limit"`, `"output too large"`, `"truncated"` | Reduce scope: drop non-essential references, compress instructions, or split task. Re-dispatch with reduced context. |
| **PERMANENT** | `"not found"`, `"no such file"`, `"invalid"`, `"malformed"`, `"compilation"`, `"compile error"`, `"test failure"`, `"assertion"`, `"permission denied"`, `"forbidden"` | Fail immediately. Mark task as FAILED. Include error details in summary. |
| **CIRCUIT** | 3+ consecutive failures (same task) or 5+ total failures in story | Pause execution (3 consecutive) or abort remaining tasks (5 total). |

**Default:** If no pattern matches, classify as **PERMANENT** and fail immediately.
**Log:** `"Error classified: {category} — Action: {action}"`

## Error Handling

| Scenario | Action |
|----------|--------|
| Story file not found | Abort: `Story file not found at {path}` |
| Dependency not complete | Abort: `Dependency {storyId} not complete` |
| Task dependency not resolved | Mark task BLOCKED, skip in current iteration |
| x-test-tdd fails | Mark task FAILED, update execution-state |
| Coverage below thresholds | Fail Phase 3.1, add tests |
| Architecture plan skill unavailable | Use inline fallback |
| Template file not found (RULE-012) | Log warning, use inline format |
| Reference file not found (RULE-002) | Log warning, continue without reference |
| Review score below approval | Fix and re-review (max 2 cycles) |
| Phase 1B produces no output | Phase 2 uses G1-G7 fallback |
| Resume with corrupted state | Reinitialize from PR statuses via `gh pr view` |

## Template Fallback

Templates follow RULE-012 (graceful degradation):
`_TEMPLATE-IMPLEMENTATION-PLAN.md`, `_TEMPLATE-TEST-PLAN.md`, `_TEMPLATE-TASK-BREAKDOWN.md`, `_TEMPLATE-SECURITY-ASSESSMENT.md`, `_TEMPLATE-COMPLIANCE-ASSESSMENT.md`, `_TEMPLATE-SPECIALIST-REVIEW.md`, `_TEMPLATE-CONSOLIDATED-REVIEW-DASHBOARD.md`, `_TEMPLATE-REVIEW-REMEDIATION.md`, `_TEMPLATE-TECH-LEAD-REVIEW.md`

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-test-tdd` | Invokes (Phase 2, per task) | TDD execution for each task (use Slim Mode for chain invocation) |
| `x-git-commit` | Invoked by x-test-tdd | Atomic commits with TDD tags and pre-commit chain (use Slim Mode) |
| `x-pr-create` | Invokes (Phase 2, per task) | Task-level PR creation |
| `x-code-format` | Invoked by x-git-commit chain | Code formatting in pre-commit (use Slim Mode) |
| `x-code-lint` | Invoked by x-git-commit chain | Code linting in pre-commit (use Slim Mode) |
| `x-task-plan` | Invokes (Phase 1, per task) | Individual task planning |
| `x-dev-architecture-plan` | Invokes (Phase 1, conditional) | Architecture planning |
| `x-test-plan` | Invokes (Phase 1B) | Test plan as implementation roadmap |
| `x-lib-task-decomposer` | Invokes (Phase 1C) | Task decomposition with TDD markers |
| `x-review` | Invokes (Phase 3.4) | Parallel specialist reviews |
| `x-review-pr` | Invokes (Phase 3.6) | Tech Lead holistic review |
| `x-dev-arch-update` | Invokes (Phase 3.3, conditional) | Architecture document update |
| `x-dev-epic-implement` | Called by | Epic orchestrator delegates story execution |
| `x-git-push` | Invokes (Phase 2, branch creation) | Branch creation for tasks |
