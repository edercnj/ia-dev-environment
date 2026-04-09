---
name: x-test-plan
description: "Generates a Double-Loop TDD test plan with TPP-ordered scenarios before implementation. Delegates KP reading to a context-gathering subagent, then produces structured Acceptance Tests (outer loop) and Unit Tests in Transformation Priority Premise order (inner loop)."
user-invocable: true
allowed-tools: Read, Grep, Glob
argument-hint: "[STORY-ID]"
context-budget: medium
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Plan Tests

## Purpose

Produces a Double-Loop TDD test plan that drives implementation order. With 95% line / 90% branch coverage enforced, the plan serves as the implementation roadmap — each test scenario maps to one Red-Green-Refactor cycle, ordered by Transformation Priority Premise (TPP).

## Triggers

- `/x-test-plan STORY-ID` — generate test plan for a specific story

## Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `STORY-ID` | Yes | Story identifier to plan tests for. If not provided, prompt for it. |

## Workflow

```
0. PRE-CHECK       -> Verify existing plan freshness (mtime comparison)
1. GATHER CONTEXT  -> Subagent (model: opus) reads testing + architecture KPs, story, template, existing code
2. PLAN TESTS      -> Generate Double-Loop + TPP-ordered scenarios (inline)
3. ESTIMATE        -> Estimate coverage and validate completeness (inline)
```

### Step 0 — Pre-Check: Idempotency (RULE-002 — Idempotency via Staleness Check)

Before generating a test plan, verify whether a valid plan already exists:

1. **Resolve paths:** Extract epic ID (XXXX) and story sequence (YYYY) from the story ID. Compute:
   - Story path: the story file provided as input
   - Plan path: `plans/epic-XXXX/plans/tests-story-XXXX-YYYY.md`

2. **Check existence:** If the plan file does NOT exist, proceed to generation (Step 1).

3. **Compare modification times:** If the plan file exists:
   - If `mtime(story file) <= mtime(plan file)` — the plan is **fresh**. Log: `"Reusing existing test plan from {date}"` (where `{date}` is the plan file's last modified date). Return the existing plan. **Do NOT invoke any subagent.**
   - If `mtime(story file) > mtime(plan file)` — the plan is **stale**. Log: `"Regenerating stale test plan for {story-id}"`. Proceed to generation (Step 1).

4. **First generation:** If the plan file does not exist at all, log: `"Generating test plan for {story-id}"`. Proceed to generation (Step 1).

### Step 1 — Gather Context (Subagent via Task, model: opus)

Launch a **single** `general-purpose` subagent with `model: opus`:

> You are a **Test Planning Assistant** gathering context for test plan generation.
>
> **Read the template for required output format (RULE-007):**
> - Read template at `.claude/templates/_TEMPLATE-TEST-PLAN.md` for required output format.
> - If the template file does NOT exist, log: `"Template not found, using inline format"` and continue without it (RULE-012 — graceful fallback for projects without templates).
>
> **Read these knowledge packs:**
> - `skills/testing/references/testing-philosophy.md` — 8 test categories, fixture patterns, data uniqueness, async handling, real vs in-memory DB decisions
> - `skills/testing/references/testing-conventions.md` — {{LANGUAGE}}-specific test frameworks, naming conventions, directory structure, assertion libraries
> - `skills/architecture/references/architecture-principles.md` — exception hierarchy, layer boundaries (unit vs integration), dependency direction
>
> **Read testing knowledge pack for {{LANGUAGE}}-specific patterns:**
> - Read `skills/testing/SKILL.md` for {{LANGUAGE}}-specific test frameworks, conventions, and patterns
>
> **Read the story:** `{STORY_PATH}`
> Extract: acceptance criteria, sub-tasks, business rules, dependencies.
>
> **Scan existing code:**
> - List existing classes in target packages
> - List existing test classes and patterns established
>
> **Return a structured context summary:**
> 1. **Test categories applicable** to this story (from the 8 categories in testing-philosophy)
> 2. **Naming convention:** `[method]_[scenario]_[expected]` pattern with {{LANGUAGE}}-specific format
> 3. **Test frameworks and assertion libraries** to use (from testing-conventions)
> 4. **Fixture pattern:** `final class` + `private constructor` + `static methods`, naming `a{Entity}()`
> 5. **Acceptance criteria** extracted from story (each becomes ≥1 test)
> 6. **Business rules** extracted (each maps to parametrized tests)
> 7. **Exception types** in scope (each needs error path test)
> 8. **Layer boundaries** — which classes need unit vs integration tests
> 9. **Existing patterns** found in current test codebase
> 10. **Contract tests applicable** (if testing.contract_tests == true)
> 11. **Chaos tests applicable** (if testing.chaos_tests == true)
> 12. **Template sections** — if template was found, list the 8 mandatory sections to populate

### Step 2 — Generate Test Scenarios (Inline)

Using the context returned by the subagent, generate a Double-Loop TDD test plan.
Organize scenarios by implementation order (TPP), NOT by test category.

#### 2.1 — Acceptance Tests (Outer Loop)

For each Gherkin scenario in the story, generate an acceptance test entry:

| Field | Description |
|-------|-------------|
| ID | `AT-N` (sequential) |
| Gherkin | Reference to the original scenario |
| Status | RED until all unit tests for this acceptance criteria complete |
| Components | List of classes/modules under test |
| Test Type | Integration, API, or E2E (depending on story scope) |
| Depends on | UT/IT IDs that must pass for this AT to go GREEN, or `--` |
| Parallel | `yes` if independent of other ATs |

#### 2.2 — Unit Tests (Inner Loop, TPP Order)

Generate unit test scenarios in strict TPP order. Each scenario represents one
Red-Green-Refactor cycle.

##### TPP Level 1 — Degenerate Cases
- Null, empty, zero inputs — return default/error
- Transform: `{}→nil` or `nil→constant`

##### TPP Level 2 — Unconditional Paths
- Single valid input — direct output (no branching)
- Transform: `constant→variable`

##### TPP Level 3 — Simple Conditions
- Single if/else branching
- Transform: `unconditional→conditional`

##### TPP Level 4 — Complex Conditions
- Multiple branches, switch/match, compound boolean
- Transform: deeper conditional logic

##### TPP Level 5 — Iterations
- Collection processing, loops, map/filter/reduce
- Transform: `scalar→collection`, `statement→recursion/iteration`

##### TPP Level 6 — Edge Cases
- Boundary values (at-min, at-max, past-max)
- Transform: `value→mutated value`

For each unit test entry, include:

| Field | Required | Description |
|-------|----------|-------------|
| ID | M | `UT-N` (sequential within TPP order) |
| Test | M | What to test (method + scenario + expected) |
| Implementation | O | Minimum code to pass this test |
| Transform | M | TPP transformation applied |
| TPP Level | M | 1-6 |
| Components | M | Classes/modules needed |
| Depends on | M | Previous test IDs that are prerequisites, or `--` |
| Parallel | M | `yes` if independent of other tests at same level |

#### 2.3 — Integration Tests (Cross-Component)

Position AFTER the unit tests of the components involved.
Include only when multiple components interact (DB, HTTP, messaging).

| Field | Required | Description |
|-------|----------|-------------|
| ID | M | `IT-N` (sequential) |
| Test | M | What to test |
| Components | M | Interacting components |
| Depends on | M | UT IDs that must pass first |
| Parallel | M | `yes`/`no` |

#### 2.4 — CRUD-Only Story Optimization

When a story describes a purely CRUD operation without branching logic:
- UTs should cover: degenerate (Level 1) — constant (Level 1) — variable (Level 2)
- Do NOT generate conditional or iteration UTs unless the business rules demand them
- Acceptance tests should focus on the full CRUD flow

### Step 3 — Estimate and Validate (Inline)

#### 3.1 — Coverage Estimation Table

| Class | Public Methods | Branches | Est. Tests | Line % | Branch % |
|-------|---------------|----------|-----------|--------|----------|
| [Name] | [count] | [count] | [count] | [%] | [%] |

Flag any class where estimated coverage < 95% line / 90% branch.

#### 3.2 — Quality Checks

1. Every Gherkin scenario maps to ≥1 acceptance test (AT)
2. Every acceptance criterion maps to ≥1 unit test chain (UT)
3. UT-1 is ALWAYS a degenerate case (TPP Level 1)
4. UTs follow non-decreasing TPP level order
5. Every exception has ≥1 error path test
6. Boundary values use triplet pattern (at-min, at-max, past-max)
7. Dependency markers are complete (no orphan UTs)
8. Estimated coverage meets thresholds (≥ 95% line, ≥ 90% branch)
9. Test naming follows convention: `[method]_[scenario]_[expected]`
10. No unnecessary UTs for CRUD-only stories (max Level 3 unless justified)

### Output

Save to: `plans/epic-XXXX/plans/tests-story-XXXX-YYYY.md` (extract epic ID XXXX and story sequence YYYY from the story ID). Ensure directory exists: `mkdir -p plans/epic-XXXX/plans`.

```markdown
# Test Plan — STORY-ID: [Title]

## Summary
- Acceptance tests: X (from Y Gherkin scenarios)
- Unit tests: ~Z (in TPP order)
- Integration tests: ~W
- Estimated line coverage: ~P%
- Estimated branch coverage: ~Q%

## Acceptance Tests (Outer Loop)

### AT-1: [Gherkin scenario name]
- **Gherkin**: [reference to story scenario]
- **Status**: RED until all unit tests complete
- **Components**: [list of components under test]
- **Acceptance Criteria**: [what must be true for this AT to pass]
- **Depends on**: UT-1, UT-2, ... (UTs that must pass)
- **Parallel**: yes/no

## Unit Tests (Inner Loop — TPP Order)

### UT-1: [degenerate case description] — TPP Level 1
- **Test**: [methodUnderTest]_[scenario]_[expectedBehavior]
- **Implementation**: [minimum code to pass]
- **Transform**: {}→nil
- **Components**: [class/module]
- **Depends on**: --
- **Parallel**: yes

### UT-2: [next case description] — TPP Level 2
- **Test**: [methodUnderTest]_[scenario]_[expectedBehavior]
- **Implementation**: [minimum code to pass]
- **Transform**: constant→variable
- **Components**: [class/module]
- **Depends on**: UT-1
- **Parallel**: no

...

## Integration Tests (Cross-Component)

### IT-1: [integration scenario]
- **Test**: [description]
- **Components**: [interacting components]
- **Depends on**: UT-3, UT-7
- **Parallel**: no

## Coverage Estimation
[table]

## Risks and Gaps
- [Hard-to-test scenarios]
- [Coverage gaps needing attention]
```

## Anti-Patterns

- Do NOT write test code — only plan scenarios
- Do NOT organize test plan by category (Happy Path, Error Path, etc.) — use TPP ordering
- Do NOT skip error paths
- Do NOT forget boundary values (0, -1, max, empty, null)
- Do NOT plan tests for trivial getters/setters
- Do NOT ignore existing test patterns
- Do NOT create redundant tests covering the same branch

## Error Handling

| Scenario | Action |
|----------|--------|
| No story ID provided | Prompt user for story identifier |
| Story file not found | Abort with file-not-found message |
| Existing plan is fresh (RULE-002) | Return existing plan, skip generation |
| Template not found (RULE-012) | Log warning, use inline format, continue |
| No Gherkin scenarios in story | Generate tests from acceptance criteria text |
| No acceptance criteria found | Abort with warning, request story refinement |

## Template Fallback (RULE-012)

When `.claude/templates/_TEMPLATE-TEST-PLAN.md` is **not available** (projects predating EPIC-0024):

1. Log warning: `"Template not found, using inline format"`
2. Generate the test plan using the inline output format defined in the **Output** section above
3. Execution continues normally — no interruption, no error
4. The inline format produces the same 8 conceptual sections but without the template's strict structure

This ensures backward compatibility with projects that have not yet adopted template-based generation.

## Knowledge Pack References

| Pack | Files | Purpose |
|------|-------|---------|
| testing | `skills/testing/references/testing-philosophy.md` | 8 test categories, fixture patterns, data uniqueness |
| testing | `skills/testing/references/testing-conventions.md` | {{LANGUAGE}}-specific test frameworks, naming, assertions |
| testing | `skills/testing/SKILL.md` | {{LANGUAGE}}-specific test patterns |
| architecture | `skills/architecture/references/architecture-principles.md` | Exception hierarchy, layer boundaries |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-dev-lifecycle` | called-by | Invoked during Phase 1B |
| `x-dev-implement` | reads | Output consumed as TDD roadmap |

- Pre-check (RULE-002) prevents redundant regeneration when story has not changed
- Template reference (RULE-007) ensures consistent 8-section output format when available
- Subagent uses `model: opus` (RULE-009) for deep test planning quality
- Output uses Double-Loop TDD format with TPP-ordered scenarios
- Output consumed by Phase 2 (developers) and Phase 3 (QA engineer validates coverage)
- Can be used standalone before any implementation task
