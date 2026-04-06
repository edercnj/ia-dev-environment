---
name: x-dev-implement
description: "Implements a feature/story using TDD (Red-Green-Refactor) workflow. Delegates preparation to a subagent that reads architecture, coding, and test plan KPs, then implements test-first with Double-Loop TDD, layer-by-layer with compile checks after each cycle."
allowed-tools: Read, Write, Edit, Bash, Grep, Glob
argument-hint: "[STORY-ID or feature-description]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Implement Story (Orchestrator)

## When to Use This vs `/x-dev-lifecycle`

| Scenario | Use |
|----------|-----|
| Quick implementation (single class, small fix) | This skill |
| Full story with multi-persona review | `/x-dev-lifecycle` |
| Coding without the review phases | This skill |
| Complete lifecycle: code → review → fix → PR | `/x-dev-lifecycle` |

## Pre-Check: Plan Reuse (RULE-002 — Idempotency via Staleness Check)

Before starting implementation, check for existing plans produced by `x-dev-lifecycle` or other planning skills. Reusing existing plans ensures consistency between the full lifecycle workflow and the simplified implement workflow.

1. **Resolve paths:** Extract epic ID (XXXX) and story sequence (YYYY) from the story ID. Compute:
   - Story path: the story file provided as input
   - Implementation plan path: `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md`
   - Architecture plan path: `plans/epic-XXXX/plans/arch-story-XXXX-YYYY.md`
   - Test plan path: `plans/epic-XXXX/plans/tests-story-XXXX-YYYY.md`

2. **Check each artifact:** For each plan type, check existence and staleness:

   | # | Artifact Type | File Pattern | Context Injection Instruction |
   |---|---------------|--------------|-------------------------------|
   | 1 | Implementation Plan | `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md` | "Use implementation plan at {path} for class diagram, method signatures, affected layers, and TDD strategy" |
   | 2 | Architecture Plan | `plans/epic-XXXX/plans/arch-story-XXXX-YYYY.md` | "Use architecture plan at {path} for component structure, dependency matrix, and mini-ADRs" |
   | 3 | Test Plan | `plans/epic-XXXX/plans/tests-story-XXXX-YYYY.md` | "Use test plan at {path} for acceptance tests and unit test scenarios" |

3. **Staleness check:** For each plan that exists:
   - If `mtime(story file) <= mtime(plan file)` → plan is **fresh**. Log: `"Using existing {type} at {path}"`
   - If `mtime(story file) > mtime(plan file)` → plan is **stale**. Log WARNING: `"Plan {type} may be stale (story modified after plan generation), using as context anyway"`
   - Stale plans are still used as context — do NOT regenerate (regeneration is the responsibility of `x-dev-lifecycle`)

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

## Execution Flow (Orchestrator Pattern)

```
0. PRE-CHECK             -> Verify existing plans (implementation, architecture, test)
1. PREPARE + UNDERSTAND  -> Subagent reads KPs + available plans, produces TDD implementation plan
2. TDD LOOP              -> For each scenario (TPP order): RED -> GREEN -> REFACTOR -> compile check
3. VALIDATE              -> Coverage thresholds, all acceptance tests GREEN (inline)
4. COMMIT                -> Atomic TDD commits: one per Red-Green-Refactor cycle (inline)
```

## Step 1: Prepare + Understand (Subagent via Task)

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
> - If the template file does NOT exist, log: `"Template not found, using inline format"` and continue without it (RULE-012: graceful fallback for projects without templates)
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
> Also create feature branch if not already on one:
> ```bash
> git checkout main && git pull origin main
> git checkout -b feat/STORY-ID-short-description
> ```

> **Fallback Mode (no test plan):**
> If no test plan file exists (neither from pre-check nor from direct lookup), log:
> `WARNING: No test plan found. Run /x-test-plan first for optimal TDD workflow. Proceeding with implementation-first approach.`
> In fallback mode, Step 2 reverts to the layer-by-layer implementation without strict TPP ordering. Tests are written alongside code (test-with) rather than test-first.

## Step 2: TDD Loop — Red-Green-Refactor (Orchestrator — Inline)

Using the TDD implementation plan returned by the subagent, execute Red-Green-Refactor cycles.

**Layer order is preserved WITHIN each cycle.** When a UT-N test touches domain, that cycle implements domain first, then adapters. Dependencies point inward (domain has no outward dependencies).

### 2.0 Write Acceptance Test First (Double-Loop — Outer Loop)

Before any unit test cycle, write the acceptance test(s) from the test plan (AT-N entries):

1. Write the acceptance test (integration/API/E2E depending on scope)
2. Run it — it MUST be RED (failing)
3. This acceptance test stays RED throughout the inner loop
4. It turns GREEN only after all related unit test cycles complete

```bash
{{TEST_COMMAND}} -- [acceptance-test-file]
# Expected: FAIL (RED)
```

### 2.1 Inner Loop: Red-Green-Refactor per Unit Test (TPP Order)

For each UT-N in the test plan (strict TPP order: degenerate first, edge cases last):

#### RED — Write the Failing Test
1. Write the unit test for UT-N (test name follows `[method]_[scenario]_[expected]`)
2. Run it — it MUST fail
3. If the test passes without new code, the test is wrong or the scenario is already covered

```bash
{{TEST_COMMAND}} -- [test-file]
# Expected: FAIL (RED)
```

#### GREEN — Implement the Minimum
1. Write the MINIMUM production code to make UT-N pass
2. Respect layer order: domain -> ports -> adapters -> application -> inbound
3. Do NOT add code beyond what the test requires
4. Run the test — it MUST pass
5. Run ALL previous tests — they MUST still pass

```bash
{{TEST_COMMAND}}
# Expected: ALL PASS (GREEN)
```

#### REFACTOR — Improve Design
1. Look for: extract method (> 25 lines), duplicate code, unclear naming
2. Refactoring NEVER adds behavior — if behavior changes, write a new failing test first
3. Run all tests after refactoring — they MUST still pass

```bash
{{TEST_COMMAND}}
# Expected: ALL PASS (still GREEN)
```

#### Compile Check
After each complete cycle (Red-Green-Refactor):

```bash
{{COMPILE_COMMAND}}
# Expected: zero errors, zero warnings
```

### 2.2 Cycle Completion

After all UT-N cycles for a given acceptance test (AT-N) are complete:
1. Run the acceptance test — it should now be GREEN
2. If still RED, identify missing unit test cycles and add them
3. Proceed to the next AT-N

### 2.3 Code Conventions (from subagent's plan)

- Named constants (never magic numbers/strings)
- Methods ≤ 25 lines, classes ≤ 250 lines
- Self-documenting code (comments only for "why")
- Never return null — use Optional/empty types
- Constructor/initializer injection
- Immutable DTOs, value objects, events

### 2.4 Fallback Mode (No Test Plan)

When operating in fallback mode (no test plan available from pre-check or direct lookup):
- Implement layer-by-layer following the old approach
- Write tests alongside code (test-with) rather than test-first
- Still run compile check after each layer: `{{COMPILE_COMMAND}}`
- Log a reminder: `WARNING: Consider running /x-test-plan for future implementations`
- If implementation plan or architecture plan were found in pre-check, still use them for guidance on layer order and class structure

## Step 3: Validate (Orchestrator — Inline)

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

## Step 4: Commit (Orchestrator — Inline)

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

## Template Fallback (RULE-012)

When `.claude/templates/_TEMPLATE-IMPLEMENTATION-PLAN.md` is **not available** (projects predating EPIC-0024):

1. Log warning: `"Template not found, using inline format"`
2. Generate the implementation plan using the inline output format defined in Step 5
3. Execution continues normally — no interruption, no error
4. The inline format produces the same conceptual sections but without the template's strict structure

This ensures backward compatibility with projects that have not yet adopted template-based generation.

## Integration Notes

- **Prerequisite:** Run `/x-test-plan` first to generate the test plan with Double-Loop + TPP ordering
- **Plan reuse:** Pre-check (RULE-002) discovers existing plans from `x-dev-lifecycle` runs, ensuring consistency between full lifecycle and simplified implement workflows
- **Template reference:** RULE-007 instructs subagent to read implementation plan template when available
- **Graceful fallback:** RULE-012 ensures backward compatibility when templates are not available
- For the full lifecycle with reviews, use `x-dev-lifecycle` instead
- Invokes patterns from `x-test-run` and `x-git-push` skills
- Works with any {{FRAMEWORK}} project following layered/hexagonal architecture
- The developer agent (typescript-developer) already includes TDD workflow rules (story-0003-0006)
- All `{{PLACEHOLDER}}` tokens (e.g. `{{BUILD_COMMAND}}`, `{{TEST_COMMAND}}`) are runtime markers filled by the AI agent from project configuration — they are NOT resolved during generation
