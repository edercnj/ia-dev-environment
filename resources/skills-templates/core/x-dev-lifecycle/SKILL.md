---
name: x-dev-lifecycle
description: "Orchestrates the complete feature implementation cycle: branch creation, planning, task decomposition, implementation, parallel review, fixes, PR creation, and final verification. Delegates heavy phases to subagents for context efficiency."
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill
argument-hint: "[STORY-ID or feature-name]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Feature Lifecycle (Orchestrator)

## When to Use

- Full story/feature implementation with review cycle
- End-to-end development workflow (plan → code → review → PR)

## CRITICAL EXECUTION RULE

**8 phases (0-7). ALL mandatory. NEVER stop before Phase 7.**

After EACH phase: `>>> Phase N/7 completed. Proceeding to Phase N+1...`

## Complete Flow

```
Phase 0: Preparation          (orchestrator — inline)
Phase 1: Planning              (subagent — reads architecture KPs)
Phase 1B-1E: Parallel Planning (up to 4 subagents — SINGLE message)
Phase 2: Implementation        (subagent — reads coding + layer KPs)
Phase 3: Review                (invoke /x-review skill — launches its own subagents)
Phase 4-5: Fixes + PR          (orchestrator — inline)
Phase 6: Tech Lead Review      (invoke /x-review-pr skill)
Phase 7: Verification          (orchestrator — inline)
```

---

## Phase 0 — Preparation (Orchestrator — Inline)

1. Read story file and extract acceptance criteria, sub-tasks, dependencies
2. Verify dependencies (predecessor stories complete)
3. Check if test plan exists at `docs/stories/epic-XXXX/plans/tests-story-XXXX-YYYY.md`
   - If present: Phase 2 will use TDD mode
   - If absent: Phase 1B will produce it; if 1B also fails, Phase 2 falls back to G1-G7
4. Extract epic ID from story ID (e.g., `story-0001-0003` → epic ID `0001`)
5. Ensure directories exist: `mkdir -p docs/stories/epic-XXXX/plans docs/stories/epic-XXXX/reviews`
6. Create branch: `git checkout -b feat/{STORY_ID}-description`

## Phase 1 — Architecture Planning (Subagent via Task)

Launch a **single** `general-purpose` subagent:

> You are a **Senior Architect** planning feature implementation for {{PROJECT_NAME}}.
>
> **Step 1 — Read context:**
> - Read story file: `{STORY_PATH}`
> - Read `skills/architecture/references/architecture-principles.md` — layer structure, dependency direction
> - Read `skills/layer-templates/SKILL.md` — code templates per architecture layer
> - Read any relevant ADRs in `docs/adr/`
>
> **Step 2 — Produce implementation plan** with these sections:
> 1. Affected layers and components
> 2. New classes/interfaces to create (with package locations)
> 3. Existing classes to modify
> 4. Dependency direction validation
> 5. Integration points
> 6. Database changes (if applicable)
> 7. API changes (if applicable)
> 8. Event changes (if applicable)
> 9. Configuration changes
> 10. Risk assessment
>
> Save to `docs/stories/epic-XXXX/plans/plan-story-XXXX-YYYY.md` (where XXXX is the epic ID and YYYY is the story sequence, extracted from the story ID).

## Phases 1B-1E — Parallel Planning (Subagents via Task — SINGLE message)

**CRITICAL: ALL planning subagents MUST be launched in a SINGLE message.**

### 1B: Test Planning (MANDATORY DRIVER for Phase 2)
Invoke skill `x-test-plan` → produces `docs/stories/epic-XXXX/plans/tests-story-XXXX-YYYY.md`

The test plan is the **implementation roadmap** for Phase 2. It produces:
- Acceptance tests (AT-N) as outer loop (Double-Loop TDD)
- Unit tests (UT-N) in TPP order as inner loop (Levels 1-6: degenerate → edge cases)
- Integration tests (IT-N) positioned after related UTs
- `Depends On: TASK-N` and `Parallel` markers per scenario

**Gate:** If Phase 1B fails or produces no output, Phase 2 MUST use G1-G7 fallback mode.

### 1C: Task Decomposition
Invoke skill `x-lib-task-decomposer` → produces `docs/stories/epic-XXXX/plans/tasks-story-XXXX-YYYY.md`

The task decomposer auto-detects decomposition mode:
- If test plan with TPP markers exists → test-driven tasks (RED/GREEN/REFACTOR per task, with `Parallel` flags)
- If no test plan → fallback to G1-G7 layer-based decomposition

### 1D: Event Schema Design (if event_driven)
Launch `general-purpose` subagent:

> You are an **Event Engineer** designing event schemas.
> Read `skills/protocols/references/event-driven-conventions.md` for standards.
> Read the implementation plan at `docs/stories/epic-XXXX/plans/plan-story-XXXX-YYYY.md`.
> Produce event schema design: event names (past tense), CloudEvents envelope, topic naming, partition key, producer/consumer contracts.
> Save to `docs/stories/epic-XXXX/plans/events-story-XXXX-YYYY.md`.

### 1E: Compliance Assessment (if compliance active)
Launch `general-purpose` subagent:

> You are a **Security Engineer** assessing compliance impact.
> Read `skills/security/SKILL.md` → then read its references.
> Read `skills/compliance/SKILL.md` → then read its references.
> Read the implementation plan at `docs/stories/epic-XXXX/plans/plan-story-XXXX-YYYY.md`.
> Produce compliance impact assessment: data classification, encryption requirements, audit logging needs, regulatory considerations.
> Save to `docs/stories/epic-XXXX/plans/compliance-story-XXXX-YYYY.md`.

## Phase 2 — TDD Implementation (Subagent via Task)

Launch a **single** `general-purpose` subagent for implementation:

> You are a **Developer** implementing story {STORY_ID} for {{PROJECT_NAME}}.
>
> **Step 1 — Read context:**
> - Read test plan: `docs/stories/epic-XXXX/plans/tests-story-XXXX-YYYY.md` — **MANDATORY** (implementation roadmap)
> - Read implementation plan: `docs/stories/epic-XXXX/plans/plan-story-XXXX-YYYY.md`
> - Read task breakdown: `docs/stories/epic-XXXX/plans/tasks-story-XXXX-YYYY.md`
> - Read `skills/coding-standards/references/coding-conventions.md` — {{LANGUAGE}} conventions
> - Read `skills/coding-standards/references/version-features.md` — {{LANGUAGE}} {{LANGUAGE_VERSION}} features
> - Read `skills/layer-templates/SKILL.md` — code templates per layer
> - Read `skills/architecture/references/architecture-principles.md` — layer boundaries
>
> If no test plan found: emit WARNING and use **G1-G7 Fallback** (see below).
>
> **Step 2 — TDD Loop (Red-Green-Refactor per scenario):**
>
> 2.0. **Write Acceptance Test First (Double-Loop — Outer Loop):**
>    - Write AT-1 from the test plan (integration/API/E2E depending on scope)
>    - Run it — it MUST be RED (failing)
>    - AT-1 stays RED throughout the inner loop until all related UTs complete
>
> 2.1. **Inner Loop — For each UT-N in TPP order** (degenerate first, edge cases last):
>    - **RED:** Write the failing unit test for UT-N
>    - **GREEN:** Implement the MINIMUM production code to pass (respecting layer order: domain → ports → adapters → application → inbound)
>    - **REFACTOR:** Improve design (extract method if > 25 lines, eliminate duplication) — NEVER add behavior
>    - **Compile check:** `{{COMPILE_COMMAND}}` — error blocks next cycle
>    - **Atomic commit:** test + implementation in one commit using TDD format (`[TDD]` suffix)
>
> 2.2. **Verify Acceptance Test:**
>    - After all UT-N for AT-1 complete → run AT-1, it should now be GREEN
>    - If still RED, identify missing unit test cycles and add them
>    - Repeat for AT-2, AT-3, etc.
>
> 2.3. **Final Validation:**
>    - Run `{{TEST_COMMAND}}` and `{{COVERAGE_COMMAND}}`
>    - Coverage targets: line ≥ 95%, branch ≥ 90%
>
> **Step 3 — Commit each TDD cycle** atomically following git conventions:
> - Use `feat(scope): implement [behavior] [TDD]` for combined cycles
> - Or `test(scope): [TDD:RED]` + `feat(scope): [TDD:GREEN]` + `refactor(scope): [TDD:REFACTOR]` for fine-grained history
> - Git history should tell TPP progression (simple to complex)
>
> Report: TDD cycles completed, acceptance tests status, coverage numbers.

### Parallelism in Phase 2

Independent test scenarios (no shared state/data dependencies) CAN run in parallel:

- Use `Parallel: yes/no` markers from the test plan and task breakdown
- Subagents working on independent layers MUST be launched in a SINGLE message
- Example: UT for outbound adapter can run in parallel with UT for inbound DTO if they share no state
- Dependent scenarios (marked `Depends On: TASK-N`) run sequentially

### G1-G7 Fallback (No Test Plan)

If no test plan with TPP markers was produced by Phase 1B, use legacy group-based implementation:

> **Step 2 (Fallback) — Implement groups G1-G7** following the task breakdown:
> - For each group: implement all tasks, then compile: `{{COMPILE_COMMAND}}`
> - If compilation fails: fix errors before proceeding
> - After G7: run `{{TEST_COMMAND}}` and `{{COVERAGE_COMMAND}}`
> - Coverage targets: line ≥ 95%, branch ≥ 90%
>
> **Step 3 (Fallback) — Commit each group** atomically following git conventions.

Emit warning: `WARNING: No TDD test plan available. Using G1-G7 group-based implementation. Consider running /x-test-plan for future implementations.`

## Phase 3 — Parallel Review

Invoke skill `/x-review` for the current story. The review skill launches its own parallel subagents (one per specialist engineer), each reading their own knowledge pack.

If `x-review` includes TDD checklist items, it validates: test-first pattern, TPP ordering, atomic TDD commits. If TDD checklist is not yet available, the review proceeds with existing criteria (backward compatible).

Collect the consolidated review report with scores and severity counts.

## Phase 4 — Fixes + Feedback (Orchestrator — Inline)

1. Fix ALL failed items from Phase 3 review (every specialist engineer must reach STATUS: Approved — all items at 2/2)
2. For each fix, follow TDD discipline: write/update the test FIRST, then apply the fix
3. Use atomic TDD commits for fixes (same commit format as Phase 2)
4. Run `{{COMPILE_COMMAND}}` + `{{TEST_COMMAND}}`
5. Update common-mistakes document with newly found errors

## Phase 5 — Commit & PR (Orchestrator — Inline)

1. Push: `git push -u origin feat/{STORY_ID}-description`
2. Create PR via `gh pr create` with review summary in body, including TDD compliance:
   - Number of TDD cycles completed
   - Test-first pattern verified
   - TPP progression in commit history

## Phase 6 — Tech Lead Review

Invoke skill `x-review-pr` for holistic 40-point review. Requires 40/40 for GO. If NO-GO, fix all failed items and re-review (max 2 cycles).

If `x-review-pr` includes TDD criteria, it validates TDD compliance in the checklist. If TDD criteria are not yet available, the review proceeds with existing checklist (backward compatible).

## Phase 7 — Final Verification + Cleanup (Orchestrator — Inline)

1. Update README if needed
2. Update IMPLEMENTATION-MAP
3. Run DoD checklist (24+ checks across phases, quality, git, artifacts)
4. TDD DoD items:
   - [ ] Commits show test-first pattern (test precedes or accompanies implementation in git log)
   - [ ] Acceptance tests exist and pass (AT-N GREEN)
   - [ ] Tests follow TPP ordering (simple to complex)
   - [ ] No test-after commits (all tests written before or with implementation)
5. Conditional DoD items:
   - Contract tests pass (if testing.contract_tests == true)
   - Event schemas registered (if event_driven)
   - Compliance requirements met (if security.compliance active)
   - Gateway configuration updated (if api_gateway != none)
   - gRPC proto backward compatible (if interfaces contain grpc)
   - GraphQL schema backward compatible (if interfaces contain graphql)
6. Report PASS/FAIL result
7. `git checkout main && git pull origin main`

**Phase 7 is the ONLY legitimate stopping point.**

## Roles and Models (Adaptive)

| Role | Phase | Tier |
|------|-------|------|
| Architect | Phase 1 | Senior |
| Task Decomposer | Phase 1C | Mid |
| Developer | Phase 2 | Adaptive (per Layer Task Catalog) |
| Specialist Reviews | Phase 3 | Adaptive (max task tier in domain) |
| Tech Lead | Phase 6 | Adaptive (story max tier) |

## Integration Notes

- Invokes: `x-test-plan`, `x-lib-task-decomposer`, `x-lib-group-verifier` (fallback only), `x-git-push`, `x-review`, `x-review-pr`
- TDD commit format follows `x-git-push` conventions (`[TDD]`, `[TDD:RED]`, `[TDD:GREEN]`, `[TDD:REFACTOR]` suffixes)
- All `{{PLACEHOLDER}}` tokens (e.g. `{{BUILD_COMMAND}}`, `{{TEST_COMMAND}}`) are runtime markers filled by the AI agent from project configuration — they are NOT resolved during generation
