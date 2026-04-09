---
name: x-dev-lifecycle
description: >
  Orchestrates the complete feature implementation cycle with task-centric workflow:
  branch creation, planning, per-task TDD execution with individual PRs and approval
  gates, story-level verification, and final cleanup. Delegates implementation to
  x-tdd, commits to x-commit, PRs to x-pr-create.
  Use for full story implementation with review.
---

# Skill: Feature Lifecycle (Task-Centric Orchestrator)

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

## Complete Flow

```
Phase 0: Preparation             (orchestrator -- inline, includes resume detection)
{% if has_contract_interfaces == 'True' %}Phase 0.5: API-First Contract     (orchestrator -- conditional, pauses for approval)
{% endif %}Phase 1: Planning                 (subagent -- reads architecture KPs)
Phase 1B-1F: Parallel Planning    (up to 5 subagents -- SINGLE message)
Phase 2: Task Execution Loop      (for each task: branch -> x-tdd -> x-pr-create -> approval gate)
Phase 3: Story-Level Verification (coverage, cross-file consistency, review, final report)
```

### RULE-001: Task as Unit of Delivery

Each task = 1 branch = 1 PR. The lifecycle orchestrates, individual skills implement:

| Concern | Skill |
|---------|-------|
| TDD implementation | `x-tdd` |
| Atomic commits | `x-commit` |
| PR creation | `x-pr-create` |
| Code formatting | `x-format` |
| Code linting | `x-lint` |
| Task planning | `x-plan-task` |

---

## Phase 0 -- Preparation (Orchestrator -- Inline)

1. Read story file and extract acceptance criteria, sub-tasks, dependencies
2. Verify dependencies (predecessor stories complete)
3. Check if test plan exists at `plans/epic-XXXX/plans/tests-story-XXXX-YYYY.md`
   - If present: Phase 2 will use TDD mode
   - If absent: Phase 1B will produce it; if 1B also fails, Phase 2 falls back to G1-G7
4. Check if architecture plan exists at `plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md`
   - If present: Phase 1 will skip architecture planning (use existing plan)
   - If absent: Phase 1 will evaluate decision tree and invoke x-dev-architecture-plan
5. Extract epic ID from story ID (e.g., `story-0001-0003` -> epic ID `0001`)
6. Ensure directories exist: `mkdir -p plans/epic-XXXX/plans plans/epic-XXXX/reviews`
7. **Branch Creation:**
   - If `--auto-approve-pr`: create parent branch `feat/story-XXXX-YYYY-desc` from `develop`
   - If NOT `--auto-approve-pr`: no parent branch needed. Task branches target `develop`
8. **Scope Assessment** -- classify the story to determine lifecycle phase optimization:

### Scope Assessment

Analyze the story content to classify its scope tier. The classification determines which phases execute:

**Classification Criteria:**

| Criterion | How to detect |
|-----------|--------------|
| Components affected | Count distinct `.java`/`.kt`/`.py`/`.ts`/`.go`/`.rs` file mentions in tech description |
| New endpoints | Count `POST/GET/PUT/DELETE/PATCH /path` patterns in data contracts |
| Schema changes | Presence of "migration script", "ALTER TABLE", "CREATE TABLE", "DROP TABLE", "ADD COLUMN" |
| Compliance | `compliance:` field with value other than "none" |
| Dependents | Count stories that depend on this one (from IMPLEMENTATION-MAP) |

**Tier Classification:**

| Tier | Criteria | Phase Behavior |
|------|----------|---------------|
| SIMPLE | <=1 component, 0 endpoints, 0 schema changes, no compliance | Skip phases 1B, 1C, 1D, 1E |
| STANDARD | 2-3 components OR 1-2 new endpoints | All phases execute normally |
| COMPLEX | >=4 components OR schema changes OR compliance requirement | All phases + stakeholder review after Phase 2 |

**Elevation Rules:**
- Compliance **always** elevates to COMPLEX regardless of other criteria
- Schema changes **always** elevate to at least COMPLEX
- A single COMPLEX criterion is sufficient for COMPLEX classification

**Display the assessment before proceeding:**

```
Scope Assessment: [TIER]
> [Phases that will execute]
> Rationale: [justification]
> [Override instruction if SIMPLE]
```

**`--full-lifecycle` Flag:**
If the user passes `--full-lifecycle`, force full execution regardless of tier:
- All phases execute (equivalent to STANDARD)
- Display: "Scope override: running full lifecycle as requested"

**SIMPLE Execution Flow:**
Phases 0 (Prepare) > 1A (Plan) > 2 (Task Execution Loop) > 3 (Verify) -- skips 1B, 1C, 1D, 1E

**COMPLEX Execution Flow:**
All phases execute normally. After Phase 2, **pause** with:
"Scope COMPLEX -- stakeholder review required."
Wait for developer confirmation before executing Phase 3.

**Default Behavior:**
If scope assessment cannot be performed, default to STANDARD. No error is raised.

### Resume Detection (RULE-014)

Check for existing `execution-state.json`. If found, reclassify task statuses by checking actual PR state via `gh pr view`. Tasks with status DONE are skipped. Tasks with status FAILED are reclassified to PENDING for retry.

{% if has_contract_interfaces == 'True' %}

## Phase 0.5 -- API-First Contract Generation (Orchestrator -- Conditional)

> **RULE-005:** Formal contract before implementation.

**Activation:** ONLY when story declares `rest`, `grpc`, `event-consumer`, `event-producer`, or `websocket`.

### Step 0.5.1 -- Interface Detection

| Interface Type | Contract Format | Output Path |
|---------------|----------------|-------------|
| `rest` | OpenAPI 3.1 | `contracts/{STORY_ID}-openapi.yaml` |
| `grpc` | Protobuf 3 | `contracts/{STORY_ID}.proto` |
| `event-consumer` | AsyncAPI 2.6 | `contracts/{STORY_ID}-asyncapi.yaml` |
| `event-producer` | AsyncAPI 2.6 | `contracts/{STORY_ID}-asyncapi.yaml` |
| `websocket` | AsyncAPI 2.6 | `contracts/{STORY_ID}-asyncapi.yaml` |

### Step 0.5.2 -- Contract Generation

Generate draft contract using data contracts from the story.

### Step 0.5.3 -- Contract Validation

Invoke `/x-contract-lint {CONTRACT_PATH}` to validate.

### Step 0.5.4 -- Approval Gate

Pause lifecycle for contract review (APPROVE/REJECT).
{% endif %}

## Phase 1 -- Architecture Planning (Skill Invocation + Subagent Fallback)

**If architecture plan exists, skip Step 1A.**

### Step 1A: Architecture Plan via x-dev-architecture-plan

Evaluate change scope:

| Condition | Plan Level |
|-----------|-----------|
| New service / integration / contract / infra | **Full** |
| New feature, no contract or infra change | **Simplified** |
| Bug fix / refactor / docs-only | **Skip** |

Invoke `/x-dev-architecture-plan {STORY_PATH}` if Full or Simplified.

### Step 1B: Implementation Plan (Subagent via Task)

Launch subagent to produce implementation plan at `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md`.

### Fallback: Inline Architecture Planning

If `x-dev-architecture-plan` is not available, combine into Step 1B subagent.

## Phases 1B-1F -- Parallel Planning (Subagents via Task -- SINGLE message)

**CRITICAL: ALL planning subagents MUST be launched in a SINGLE message.**

### 1B: Test Planning (MANDATORY DRIVER for Phase 2)
Invoke skill `x-test-plan` -> produces test plan as implementation roadmap.

### 1C: Task Decomposition
Invoke skill `x-lib-task-decomposer` -> produces task breakdown.

### 1D: Event Schema Design (if event_driven)
Launch subagent for event schema design.

### 1E: Security Assessment (MANDATORY)
Launch subagent for security assessment.

### 1F: Compliance Assessment (if compliance active)
Launch subagent for compliance assessment.

---

## Phase 2 -- Task Execution Loop (RULE-001: 1 Task = 1 Branch = 1 PR)

Phase 2 replaces the monolithic TDD implementation with a per-task execution loop.

### Step 2.0 -- Read Task List

1. Read tasks from story Section 8 or task breakdown
2. Parse into ordered list respecting dependencies
3. If `--task TASK-ID` specified, filter to only that task
4. **Backward compatibility:** Stories without formal tasks are treated as single implicit task

### Step 2.1 -- Initialize Execution State

Create or update `plans/epic-XXXX/execution-state.json` with task-level tracking.

### Step 2.2 -- Task Execution Loop

For each task in dependency order:

```
FOR each TASK-NNN where status != DONE and status != BLOCKED:
  2.2.1  Check dependencies -> mark BLOCKED if unresolved
  2.2.2  Update execution-state: task.status = IN_PROGRESS
  2.2.3  Create task branch (from parent branch or develop)
  2.2.4  Read task plan (if PRE_PLANNED mode)
  2.2.5  Invoke /x-tdd TASK-XXXX-YYYY-NNN
  2.2.6  Push branch
  2.2.7  Invoke /x-pr-create TASK-XXXX-YYYY-NNN
  2.2.8  Update execution-state: task.status = PR_CREATED
  2.2.9  APPROVAL GATE:
         --auto-approve-pr: auto-merge into parent branch
         Manual: AskUserQuestion (APPROVE/REJECT/PAUSE)
  2.2.10 Update execution-state with final status
END FOR
```

### Auto-Approve Mode (--auto-approve-pr, RULE-004)

1. Parent branch `feat/story-XXXX-YYYY-desc` created from `develop`
2. Task PRs target parent branch (NOT `develop`)
3. Task PRs auto-merged into parent branch
4. Parent branch **NEVER** auto-merges into `develop` or `main`
5. Phase 3 creates story-level PR from parent branch to `develop`

### G1-G7 Fallback (No Test Plan / No Tasks)

Legacy group-based implementation as single implicit task when no test plan or formal tasks exist.

---

## Phase 3 -- Story-Level Verification (Absorbs Old Phases 3-8)

Executes after all tasks have approved/merged PRs. Skip with `--skip-verification`.

### Step 3.1 -- Coverage Consolidation
Validate line >= 95%, branch >= 90%.

### Step 3.2 -- Cross-File Consistency Check
Verify uniform patterns across same-role classes.

### Step 3.3 -- Documentation Update
Generate documentation per configured interfaces. Always generate changelog entry.

### Step 3.4 -- Review (Invoke /x-review)
Parallel specialist reviews with consolidated dashboard.

### Step 3.5 -- Fixes + Remediation
Fix all review findings using TDD discipline with atomic commits via `/x-commit`.

### Step 3.6 -- Tech Lead Review
Invoke `x-review-pr` for holistic review.

### Step 3.7 -- Story-Level PR (Auto-Approve Mode Only)
Create PR from parent branch to `develop` (requires human review).

### Step 3.8 -- Final Verification + Cleanup
Update IMPLEMENTATION-MAP, story status, execution-state, run DoD checklist.

**Phase 3 is the ONLY legitimate stopping point.**

## Integration Notes

- Invokes: `x-tdd` (Phase 2), `x-commit` (via x-tdd), `x-pr-create` (Phase 2), `x-format`/`x-lint` (via x-commit chain), `x-plan-task` (Phase 1), `x-dev-architecture-plan` (Phase 1), `x-test-plan`, `x-lib-task-decomposer`, `x-review` (Phase 3), `x-review-pr` (Phase 3), `x-dev-arch-update` (Phase 3)
- All `{{PLACEHOLDER}}` tokens are runtime markers filled by the AI agent from project configuration

## Detailed References

For in-depth guidance on the lifecycle phases, consult:
- `.github/skills/x-dev-lifecycle/SKILL.md`
- `.github/skills/x-dev-implement/SKILL.md`
- `.github/skills/x-review/SKILL.md`
- `.github/skills/x-review-pr/SKILL.md`
