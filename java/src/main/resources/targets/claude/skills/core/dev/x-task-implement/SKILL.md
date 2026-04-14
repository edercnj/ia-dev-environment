---
name: x-task-implement
description: "Implements a feature/story using TDD (Red-Green-Refactor) workflow. Delegates preparation to a subagent that reads architecture, coding, and test plan KPs, then implements test-first with Double-Loop TDD, layer-by-layer with compile checks after each cycle."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill
argument-hint: "[TASK-ID (TASK-XXXX-YYYY-NNN) or STORY-ID or feature-description] [--worktree]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Implement Story

## Purpose

Implements a feature or story following TDD (Red-Green-Refactor) workflow for {{PROJECT_NAME}}. Delegates preparation to a subagent that reads architecture, coding, and test plan knowledge packs, then implements test-first with Double-Loop TDD, layer-by-layer with compile checks after each cycle.

| Scenario | Use |
|----------|-----|
| Quick implementation (single class, small fix) | This skill |
| Full story with multi-persona review | `/x-story-implement` |
| Coding without the review phases | This skill |
| Complete lifecycle: code, review, fix, PR | `/x-story-implement` |

## Triggers

- `/x-task-implement STORY-ID` — implement a story by ID
- `/x-task-implement feature-description` — implement a feature from description

## Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `STORY-ID` or description | Yes | Story identifier or free-text feature description |

## CLI Arguments

| Argument | Type | Default | Description |
|----------|------|---------|-------------|
| `--worktree` | Boolean | false | Opt-in: create a dedicated worktree for the task branch (standalone mode). Ignored when the skill is already running inside a worktree (Rule 14 §3 — non-nesting invariant). See ADR-0004 §D2. |

## Workflow

```
0.   PRE-CHECK                  -> Verify existing plans (implementation, architecture, test)
0.5  WORKTREE-FIRST BRANCHING   -> detect-context + mode selection (REUSE / CREATE / LEGACY), Rule 14 + ADR-0004
1.   PREPARE + UNDERSTAND       -> Subagent reads KPs + available plans, produces TDD implementation plan
2.   TDD LOOP                   -> For each scenario (TPP order): RED -> GREEN -> REFACTOR -> compile check
3.   VALIDATE                   -> Coverage thresholds, all acceptance tests GREEN (inline)
4.   COMMIT                     -> Atomic TDD commits: one per Red-Green-Refactor cycle (inline)
5.   MODE-AWARE CLEANUP         -> Remove worktree (Mode 2 success) + `git checkout develop && git pull` (Modes 2-success / 3)
```

### Step 0 — Pre-Check: Plan Reuse (RULE-002 — Idempotency via Staleness Check)

Before starting implementation, check for existing plans produced by `x-story-implement` or other planning skills. Reusing existing plans ensures consistency between the full lifecycle workflow and the simplified implement workflow.

1. **Resolve paths:** Extract epic ID (XXXX) and story sequence (YYYY) from the story ID. Compute:
   - Story path: the story file provided as input
   - Implementation plan path: `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md`
   - Architecture plan path: `plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md`
   - Test plan path: `plans/epic-XXXX/plans/tests-story-XXXX-YYYY.md`

2. **Check each artifact:** For each plan type, check existence and staleness:

   | # | Artifact Type | File Pattern | Context Injection Instruction |
   |---|---------------|--------------|-------------------------------|
   | 1 | Implementation Plan | `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md` | "Use implementation plan at {path} for class diagram, method signatures, affected layers, and TDD strategy" |
   | 2 | Architecture Plan | `plans/epic-XXXX/plans/architecture-story-XXXX-YYYY.md` | "Use architecture plan at {path} for component structure, dependency matrix, and mini-ADRs" |
   | 3 | Test Plan | `plans/epic-XXXX/plans/tests-story-XXXX-YYYY.md` | "Use test plan at {path} for acceptance tests and unit test scenarios" |

3. **Staleness check:** For each plan that exists:
   - If `mtime(story file) <= mtime(plan file)` — plan is **fresh**. Log: `"Reusing existing {type} from {date}"`
   - If `mtime(story file) > mtime(plan file)` — plan is **stale**. Log WARNING: `"Plan {type} may be stale (story modified after plan generation), using as context anyway"`
   - Stale plans are still used as context — do NOT regenerate (regeneration is the responsibility of `x-story-implement`)

4. **Context combination:** Log the combination of available plans:

   | Impl Plan | Arch Plan | Test Plan | Log Message |
   |-----------|-----------|-----------|-------------|
   | Absent | Absent | Absent | `"No plans found, proceeding with direct implementation"` |
   | Present | Absent | Absent | `"Using implementation plan, no arch/test plans"` |
   | Absent | Present | Absent | `"Using architecture plan, no impl/test plans"` |
   | Absent | Absent | Present | `"Using test plan, no impl/arch plans"` |
   | Present | Present | Absent | `"Using implementation and architecture plans"` |
   | Present | Absent | Present | `"Using implementation and test plans"` |
   | Absent | Present | Present | `"Using architecture and test plans"` |
   | Present | Present | Present | `"Using all 3 plans as implementation context"` |

   No combination blocks execution — graceful degradation in all scenarios.

#### Additional Artifacts (Task-Aware Mode)

In addition to the 3 plan types above, check for per-task plans produced by `x-story-implement` (multi-agent planning):

   | # | Artifact Type | File Pattern | Context Injection Instruction |
   |---|---------------|--------------|-------------------------------|
   | 4 | Task Plans | `plans/epic-XXXX/plans/task-plan-TASK-*-story-XXXX-YYYY.md` | "Use per-task plans for implementation sequence and DoD criteria" |
   | 5 | Task Breakdown | `plans/epic-XXXX/plans/tasks-story-XXXX-YYYY.md` | "Use task breakdown for execution order and parallelism markers" |

5. **Mode determination:**
   - If task plans AND task breakdown exist AND are fresh: use **Task-Aware Mode** (iterate TASK-NNN)
   - Otherwise: use existing Mode A (test-driven via UT-N) or Mode B (layer-based fallback)

   Log: `"Found {N} per-task plans, using task-aware mode"` or `"No per-task plans found, using {test-driven|layer-based} mode"`

### Step 0.5 — Worktree-First Branch Creation (Rule 14 + ADR-0004)

Branch creation in `x-task-implement` follows the **worktree-first policy** defined in [ADR-0004 — Worktree-First Branch Creation Policy](../../../adr/ADR-0004-worktree-first-branch-creation-policy.md) and the normative invariants of [Rule 14 — Worktree Lifecycle](../../rules/14-worktree-lifecycle.md). The routine below is **mandatory** and MUST execute before any `git checkout -b` or `/x-git-worktree create` call (whether performed inline by this skill, by the Step 1 subagent's preparation bash block, or by any downstream task-level commit workflow).

**Step 0.5a — Detect worktree context (Rule 14 §3 — Non-Nesting Invariant).** Invoke the `x-git-worktree` skill via the Skill tool to classify the current execution context (Rule 13 Pattern 1 — INLINE-SKILL):

    Skill(skill: "x-git-worktree", args: "detect-context")

The skill returns a JSON envelope of the form:

```json
{
  "inWorktree": true,
  "worktreePath": "/abs/path/to/.claude/worktrees/story-XXXX-YYYY",
  "mainRepoPath": "/abs/path/to/repo"
}
```

Record `inWorktree`, `worktreePath`, and `mainRepoPath` for use in the following steps and for the Step 5 / end-of-workflow mode-aware cleanup. This call is **mandatory** before any branch creation — skipping it risks creating a nested worktree (Rule 14 §3) or causing the harness to apply file operations in the wrong checkout, which can wipe sibling files outside the intended worktree (see Rule 14 §7 — Anti-Patterns and ADR-0004 Context §3 — Lack of operator visibility).

**Step 0.5b — Select the branching mode.** Apply the three-way decision table below. This table is the single source of truth for branch creation in this skill:

| # | Condition | Mode | Action |
| :--- | :--- | :--- | :--- |
| 1 | `inWorktree == true` | **REUSE (orchestrated)** | Reuse the current worktree. Do NOT invoke `/x-git-worktree create`. Do NOT create a nested worktree (Rule 14 §3). Task branches (`feat/task-XXXX-YYYY-NNN-description`) are created *inside* the reused worktree via `git checkout -b`. The creator of the outer worktree (typically `x-story-implement` or `x-epic-implement`) owns its removal (Rule 14 §5). |
| 2 | `inWorktree == false` AND `--worktree` flag present | **CREATE (standalone opt-in)** | Provision a dedicated task-level worktree by invoking the `x-git-worktree` skill (see Step 0.5c, Mode 2, for the canonical `Skill(...)` call). This is the opt-in path documented in ADR-0004 §D2 for operators who run `x-task-implement` directly and want isolation. `x-task-implement` is the creator and owns removal (Rule 14 §5 — end of the workflow (Step 5) on success; preserved on failure per Rule 14 §4). |
| 3 | `inWorktree == false` AND `--worktree` flag absent | **LEGACY (main checkout)** | Fall back to the legacy / interactive behavior: the task branch is created directly in the main working tree via `git checkout -b`. This preserves backward compatibility for developers who run `/x-task-implement` interactively without requesting isolation. |

> **Orchestrator auto-path.** When this skill is dispatched by `x-story-implement` (Phase 2 task execution) or transitively by `x-epic-implement`, the parent has already placed the process inside a worktree and this invocation detects `inWorktree == true`, selecting Mode 1 (REUSE) automatically. No flag is required from the caller. This is the expected automatic path described in ADR-0004 §D2.

> **Anti-pattern (DO NOT USE):** `Agent(isolation:"worktree")` is DEPRECATED (see ADR-0004 and Rule 14 §7). The harness-native isolation is replaced by explicit `/x-git-worktree create` calls from orchestrators so the worktree lifecycle is visible in logs and recoverable on failure.

**Step 0.5c — Execute the selected branching mode.**

- **Mode 1 (REUSE — orchestrated).** The parent orchestrator has already placed the process inside the worktree at `worktreePath`. Proceed with task branch creation *inside* that worktree:
  - Create the task branch `feat/task-XXXX-YYYY-NNN-description` via `git checkout -b` from the appropriate base (parent story branch when `--auto-approve-pr` is active upstream, or `develop` otherwise).
  - Do NOT call `Skill(skill: "x-git-worktree", args: "remove ...")` here or at Step 5. The orchestrator is the creator and owns removal (Rule 14 §5).

- **Mode 2 (CREATE — standalone opt-in).** Invoke `x-git-worktree` via the Skill tool to provision the worktree (Rule 13 Pattern 1 — INLINE-SKILL):

      Skill(skill: "x-git-worktree", args: "create --branch feat/task-XXXX-YYYY-NNN-description --base develop --id task-XXXX-YYYY-NNN")

  The operation creates `.claude/worktrees/task-XXXX-YYYY-NNN/` with `feat/task-XXXX-YYYY-NNN-description` checked out. Record the returned worktree path; subsequent steps (TDD loop, commits) execute with that path as their working directory.
  - In standalone mode `x-task-implement` is the creator and MUST invoke `Skill(skill: "x-git-worktree", args: "remove --id task-XXXX-YYYY-NNN")` at Step 5 on success (Rule 13 Pattern 1 — INLINE-SKILL; NEVER use the bare-slash `/x-git-worktree ...` form in delegation). On failure, the worktree is preserved for diagnosis (Rule 14 §4).

- **Mode 3 (LEGACY — main checkout).** Execute the pre-EPIC-0037 behavior unchanged, operating directly in the main working tree:

      git checkout develop && git pull origin develop
      git checkout -b feat/task-XXXX-YYYY-NNN-description

  This replaces the legacy "Step 1 / Step 5 bash block" that created the branch from `main` inside the preparation subagent; branch creation now happens here, in the orchestrator, under Rule 14 + ADR-0004 control.

**Step 0.5d — Logging.** After mode selection, log one of:

- `"Branch creation mode: REUSE (inside worktree {worktreePath})"`
- `"Branch creation mode: CREATE (standalone --worktree, provisioning worktree for task-XXXX-YYYY-NNN)"`
- `"Branch creation mode: LEGACY (main checkout, no --worktree flag)"`

These lines are required for operator diagnostics — Rule 14 §3 detection SHOULD be auditable from the log stream alone.

### Step 1 — Prepare + Understand (Subagent via Task)

Launch a **single** `general-purpose` subagent:

> You are a **Senior Developer** preparing an implementation plan for {{PROJECT_NAME}}.
>
> **Step 1 — Read the story/requirements:** `{STORY_PATH_OR_DESCRIPTION}`
> Extract: acceptance criteria, sub-tasks, test scenarios, dependencies.
>
> **Step 1.5 — Read existing plans (from Pre-Check):**
> For each plan discovered during the pre-check, read it and incorporate its guidance:
> - **Implementation plan** (if found): Use implementation plan at `{IMPL_PLAN_PATH}` for class diagram, method signatures, affected layers, and TDD strategy. This takes priority over generating a new plan from scratch.
> - **Architecture plan** (if found): Use architecture plan at `{ARCH_PLAN_PATH}` for component structure, dependency matrix, and mini-ADRs. Respect architectural decisions documented in the plan.
> - **Test plan** (if found): Use test plan at `{TEST_PLAN_PATH}` for acceptance tests and unit test scenarios. Extract AT-N, UT-N, and IT-N entries for TDD loop ordering.
> Priority order when plans overlap: implementation plan > architecture plan > test plan.
>
> **Step 2 — Read test plan (MANDATORY when no existing test plan from pre-check):**
> - If test plan was found in pre-check: use it (already read in Step 1.5)
> - Otherwise: look for test plan at `plans/{STORY_ID}-tests.md` or `plans/epic-XXXX/plans/tests-story-XXXX-YYYY.md`
> - Extract: acceptance tests (AT-N), unit tests in TPP order (UT-N), integration tests (IT-N)
> - Identify the outer loop (acceptance tests that start RED)
> - Identify the inner loop order (unit tests in TPP sequence: degenerate first, complex last)
> - If NO test plan found: emit WARNING and suggest running `/x-test-plan` first, then continue with fallback mode (implement without strict TPP ordering)
>
> **Step 3 — Read project conventions:**
> - `skills/architecture/references/architecture-principles.md` — layer structure, dependency direction
> - `skills/coding-standards/references/coding-conventions.md` — {{LANGUAGE}} coding conventions
> - `skills/coding-standards/references/version-features.md` — {{LANGUAGE}} {{LANGUAGE_VERSION}} idioms
> - `skills/layer-templates/SKILL.md` — code templates per architecture layer (defines implementation order)
>
> **Step 3.5 — Read template for implementation plan format (RULE-007):**
> - Read template at `.claude/templates/_TEMPLATE-IMPLEMENTATION-PLAN.md` for required output format
> - If the template file does NOT exist, log: `"Template not found, using inline format"` and continue without it (RULE-012 — graceful fallback for projects without templates)
>
> **Step 4 — Review existing code** in the target packages to identify patterns to follow.
>
> **Step 5 — Produce implementation plan:**
> If an implementation plan was found in pre-check, use it as the base and supplement with any missing details. Otherwise, generate a new plan:
> 1. Layer-by-layer implementation order (from layer-templates)
> 2. For each layer: classes to create/modify, package location, key patterns
> 3. TDD cycle mapping: which UT-N scenarios apply to which layer
> 4. Acceptance test identification: AT-N entries that validate the feature end-to-end
> 5. Key conventions to follow (naming, immutability, injection style)
> 6. Dependencies to verify before starting
>
> **Branch creation is handled by the orchestrator in Step 0.5 (Worktree-First Branch Creation, Rule 14 + ADR-0004) and MUST NOT be performed by this subagent.** Do not run `git checkout -b`, `git worktree add`, or any other branch/worktree command; assume the correct task branch is already checked out inside the correct worktree (or main checkout, in LEGACY mode) when this subagent begins.

> **Fallback Mode (no test plan):**
> If no test plan file exists (neither from pre-check nor from direct lookup), log:
> `WARNING: No test plan found. Run /x-test-plan first for optimal TDD workflow. Proceeding with implementation-first approach.`
> In fallback mode, Step 2 reverts to the layer-by-layer implementation without strict TPP ordering. Tests are written alongside code (test-with) rather than test-first.

### Step 2 — TDD Loop: Red-Green-Refactor (Inline)

Using the TDD implementation plan returned by the subagent, execute Red-Green-Refactor cycles.

**Layer order is preserved WITHIN each cycle.** When a UT-N test touches domain, that cycle implements domain first, then adapters. Dependencies point inward (domain has no outward dependencies).

#### 2.0 — Write Acceptance Test First (Double-Loop Outer Loop)

Before any unit test cycle, write the acceptance test(s) from the test plan (AT-N entries):

1. Write the acceptance test (integration/API/E2E depending on scope)
2. Run it — it MUST be RED (failing)
3. This acceptance test stays RED throughout the inner loop
4. It turns GREEN only after all related unit test cycles complete

```bash
{{TEST_COMMAND}} -- [acceptance-test-file]
# Expected: FAIL (RED)
```

#### 2.1 — Inner Loop: Red-Green-Refactor per Unit Test (TPP Order)

For each UT-N in the test plan (strict TPP order: degenerate first, edge cases last):

##### RED — Write the Failing Test
1. Write the unit test for UT-N (test name follows `[method]_[scenario]_[expected]`)
2. Run it — it MUST fail
3. If the test passes without new code, the test is wrong or the scenario is already covered

```bash
{{TEST_COMMAND}} -- [test-file]
# Expected: FAIL (RED)
```

##### GREEN — Implement the Minimum
1. Write the MINIMUM production code to make UT-N pass
2. Respect layer order: domain -> ports -> adapters -> application -> inbound
3. Do NOT add code beyond what the test requires
4. Run the test — it MUST pass
5. Run ALL previous tests — they MUST still pass

```bash
{{TEST_COMMAND}}
# Expected: ALL PASS (GREEN)
```

##### REFACTOR — Improve Design
1. Look for: extract method (> 25 lines), duplicate code, unclear naming
2. Refactoring NEVER adds behavior — if behavior changes, write a new failing test first
3. Run all tests after refactoring — they MUST still pass

```bash
{{TEST_COMMAND}}
# Expected: ALL PASS (still GREEN)
```

##### Compile Check
After each complete cycle (Red-Green-Refactor):

```bash
{{COMPILE_COMMAND}}
# Expected: zero errors, zero warnings
```

#### 2.2 — Cycle Completion

After all UT-N cycles for a given acceptance test (AT-N) are complete:
1. Run the acceptance test — it should now be GREEN
2. If still RED, identify missing unit test cycles and add them
3. Proceed to the next AT-N

#### 2.3 — Code Conventions (from subagent's plan)

- Named constants (never magic numbers/strings)
- Methods ≤ 25 lines, classes ≤ 250 lines
- Self-documenting code (comments only for "why")
- Never return null — use Optional/empty types
- Constructor/initializer injection
- Immutable DTOs, value objects, events

#### 2.4 — Fallback Mode (No Test Plan)

When operating in fallback mode (no test plan available from pre-check or direct lookup):
- Implement layer-by-layer following the old approach
- Write tests alongside code (test-with) rather than test-first
- Still run compile check after each layer: `{{COMPILE_COMMAND}}`
- Log a reminder: `WARNING: Consider running /x-test-plan for future implementations`
- If implementation plan or architecture plan were found in pre-check, still use them for guidance on layer order and class structure

#### 2.5 — Task-Aware TDD Loop (when per-task plans exist)

When per-task plans are detected in Step 0, Step 2 replaces the UT-N/layer-based iteration with TASK-NNN iteration:

**Task Iteration Order:**
1. Read `tasks-story-XXXX-YYYY.md` for ordered task list with dependencies
2. Order tasks by: dependencies first (tasks with no deps before those that have deps), then TDD phase (RED->GREEN->REFACTOR for each component), then TPP level (nil->constant->scalar->collection)

**For each TASK-NNN:**

##### 1. Resume Check
Verify if task is already DONE:
- Check if the file/class from Implementation Guide exists
- If exists, run tests for that component
- If tests pass: log `"TASK-NNN: DONE (resume point verified)"` and skip
- If tests fail or file doesn't exist: proceed with implementation

##### 2. Read Task Plan
Read `task-plan-TASK-NNN-story-XXXX-YYYY.md`
- Extract: Objective, Implementation Guide, Definition of Done, Dependencies

##### 3. Execute Implementation Guide
Follow the step-by-step instructions in the task plan:
- Respect layer order: domain -> ports -> adapters -> application -> inbound
- Apply TDD: write test first (RED), implement (GREEN), refactor

##### 4. Validate DoD
Check each criterion from the task's Definition of Done:
- If DoD fails: log which criteria failed, do NOT mark as DONE

##### 5. Atomic Commit
```bash
git add [test-file] [implementation-files]
git commit -m "feat(TASK-XXXX-YYYY-NNN): implement TASK-XXXX-YYYY-NNN [TDD:GREEN]"
```

##### 6. Compile Check
```bash
{{COMPILE_COMMAND}}
# Must pass before next task
```

When per-task plans are NOT detected, Step 2 executes as currently defined (Mode A: test-driven via UT-N, or Mode B: layer-based fallback).

### Step 3 — Validate (Inline)

All TDD cycles are complete. Run final validation:

```bash
{{TEST_COMMAND}}
{{COVERAGE_COMMAND}}
```

**Definition of Done:**

| Criterion | Verification |
|-----------|-------------|
| All acceptance tests (AT-N) GREEN | Run full test suite |
| All unit tests (UT-N) GREEN | Run full test suite |
| Line coverage ≥ 95% | Coverage report |
| Branch coverage ≥ 90% | Coverage report |
| Code compiles cleanly | `{{COMPILE_COMMAND}}` with no warnings |
| All tests pass | `{{TEST_COMMAND}}` |
| Tests written BEFORE implementation | Verify test-first pattern in each cycle |
| Refactoring evaluated per cycle | Each cycle has explicit refactor step (even if noop) |
| Thread-safe (if applicable) | No mutable static state |
| Automated test validates primary AC | At least 1 test validates the story's primary acceptance criterion |
| Smoke test passes | `{{SMOKE_COMMAND}}` (if testing.smoke_tests == true) |

### Step 4 — Commit (Inline)

Make atomic commits per TDD cycle. Each commit contains the test AND its implementation from one Red-Green-Refactor cycle:

```bash
# Per TDD cycle (UT-N):
git add [test-file-for-UT-N]
git add [implementation-files-for-UT-N]
git commit -m "feat(scope): add [behavior] (UT-N)" \
  -m "- RED: [test description]" \
  -m "- GREEN: [minimum implementation]" \
  -m "- REFACTOR: [what was improved, or 'noop']"
```

For acceptance tests, commit when introduced (RED) and again when they turn GREEN (if updated):

```bash
# RED: introduce failing acceptance test
git add [acceptance-test-file]
git commit -m "test(scope): add acceptance test for [AT-N scenario] (RED)"

# Later, when the AT turns GREEN and you've updated it if needed:
git add [acceptance-test-file]
git commit -m "test(scope): update acceptance test for [AT-N scenario] (GREEN)"
```

**Commit ordering must reflect TDD progression:**
1. Acceptance test commit (RED) — first
2. Unit test + implementation commits (UT-1, UT-2, ...) — in TPP order
3. Final commit when AT turns GREEN (if AT content changed)

### Step 5 — Mode-Aware Worktree Removal + Repository Sync (Rule 14 §2 + §5)

Executed after all TDD cycles, validations, and commits for the task are complete. Rule 14 §2 forbids checking out `develop` (or any protected branch) inside a worktree; Rule 14 §5 assigns worktree removal to its creator. Apply the branch mode recorded in Step 0.5:

- **Mode 1 (REUSE — orchestrated).** Do NOT remove the worktree (the parent orchestrator — `x-story-implement` or `x-epic-implement` — is the creator and owns removal) and do NOT run `git checkout develop && git pull origin develop` here (would violate Rule 14 §2 by checking out a protected branch inside a worktree). Simply return control to the caller.

- **Mode 2 (CREATE — standalone opt-in) AND the task completed successfully (all Definition of Done criteria in Step 3 satisfied):**
  1. Remove the worktree first via the Skill tool (Rule 13 Pattern 1 — INLINE-SKILL):

         Skill(skill: "x-git-worktree", args: "remove --id task-XXXX-YYYY-NNN")

  2. After removal, switch execution context back to `mainRepoPath` (captured at Step 0.5a) and run:

         git checkout develop && git pull origin develop

- **Mode 2 (CREATE — standalone opt-in) AND the task FAILED or had unrecovered errors:** preserve the worktree for diagnosis (Rule 14 §4). Log the preserved path and instruct the operator to run `Skill(skill: "x-git-worktree", args: "remove --id task-XXXX-YYYY-NNN")` after triage (the `remove` operation internally uses `git worktree remove --force` — no user-facing `--force` flag is documented). Do NOT run `git checkout develop && git pull origin develop` while the worktree is preserved (same Rule 14 §2 protection).

- **Mode 3 (LEGACY — main checkout).** No worktree was created. Run `git checkout develop && git pull origin develop` in the main checkout as before.

## Error Handling

| Scenario | Action |
|----------|--------|
| No story ID or description provided | Prompt user for story identifier |
| Test plan not found | Log warning, proceed in fallback mode (test-with) |
| Compile failure after TDD cycle | Fix compilation error before proceeding to next cycle |
| Test regression (previously passing test fails) | Revert last change, investigate root cause |
| Coverage below threshold | Identify uncovered branches, add missing test scenarios |
| Acceptance test still RED after all UTs | Identify missing unit test cycles and add them |
| Task DoD validation fails | Log which criteria failed, do NOT mark task as DONE, attempt fix |
| Task dependency not met | Skip task, log warning, process remaining independent tasks first |
| Template not found (RULE-012) | Log warning, use inline format, continue normally |

## Template Fallback (RULE-012)

When `.claude/templates/_TEMPLATE-IMPLEMENTATION-PLAN.md` is **not available** (projects predating EPIC-0024):

1. Log warning: `"Template not found, using inline format"`
2. Generate the implementation plan using the inline output format defined in Step 5
3. Execution continues normally — no interruption, no error
4. The inline format produces the same conceptual sections but without the template's strict structure

This ensures backward compatibility with projects that have not yet adopted template-based generation.

## Knowledge Pack References

| Pack | Files | Purpose |
|------|-------|---------|
| architecture | `skills/architecture/references/architecture-principles.md` | Layer structure, dependency direction |
| coding-standards | `skills/coding-standards/references/coding-conventions.md` | {{LANGUAGE}} coding conventions |
| coding-standards | `skills/coding-standards/references/version-features.md` | {{LANGUAGE}} {{LANGUAGE_VERSION}} idioms |
| layer-templates | `skills/layer-templates/SKILL.md` | Code templates per architecture layer |
| testing | `skills/testing/SKILL.md` | Test frameworks and TDD patterns |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-test-plan` | reads | Consumes test plan for Double-Loop + TPP ordering |
| `x-story-implement` | called-by | Invoked during Phase 2 of the full lifecycle |
| `x-test-run` | calls | Invokes test execution and coverage validation patterns |
| `x-git-push` | calls | Uses commit conventions for atomic TDD commits |
| `x-lib-task-decomposer` | reads | Consumes task breakdown and per-task plans for task-aware mode |
| `x-git-worktree` | Invokes (Step 0.5 + Step 5) | Worktree context detection (mandatory pre-branch), standalone worktree creation (`--worktree` flag, Mode 2), and Creator-Owned removal at end of Step 5 (Rule 14 + ADR-0004) |

- **Prerequisite:** Run `/x-test-plan` first to generate the test plan with Double-Loop + TPP ordering
- **Plan reuse:** Pre-check (RULE-002) discovers existing plans from `x-story-implement` runs, ensuring consistency between full lifecycle and simplified implement workflows
- **Template reference:** RULE-007 instructs subagent to read implementation plan template when available
- **Graceful fallback:** RULE-012 ensures backward compatibility when templates are not available
- **Task-aware mode:** When per-task plans from `x-lib-task-decomposer` exist, Step 2 iterates TASK-NNN instead of UT-N, with resume check, DoD validation, and atomic commits per task
- For the full lifecycle with reviews, use `x-story-implement` instead
- Works with any {{FRAMEWORK}} project following layered/hexagonal architecture
- The developer agent (typescript-developer) already includes TDD workflow rules (story-0003-0006)
- All `{{PLACEHOLDER}}` tokens (e.g. `{{BUILD_COMMAND}}`, `{{TEST_COMMAND}}`) are runtime markers filled by the AI agent from project configuration — they are NOT resolved during generation
