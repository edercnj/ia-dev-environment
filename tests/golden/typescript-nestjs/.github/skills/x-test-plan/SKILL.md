---
name: x-test-plan
description: >
  Generates a Double-Loop TDD test plan with TPP-ordered scenarios before
  implementation. Produces Acceptance Tests (outer loop) and Unit Tests in
  Transformation Priority Premise order (inner loop). Use when planning tests
  for a story or feature.
---

# Skill: Plan Tests (Orchestrator)

## Purpose

Produces a Double-Loop TDD test plan that drives implementation order. With line coverage >= 95% and branch coverage >= 90% enforced, the plan serves as the implementation roadmap — each test scenario maps to one Red-Green-Refactor cycle, ordered by Transformation Priority Premise (TPP).

**Condition**: Use before implementing tests for any story or feature.

## Prerequisites

- Story file with acceptance criteria and sub-tasks
- Existing codebase with established test patterns
- Knowledge of typescript test frameworks and conventions

## Knowledge Pack References

Before planning tests, read:
- `.github/skills/testing/SKILL.md` — 8 test categories, fixture patterns, data uniqueness, typescript-specific test frameworks, naming conventions
- `.github/skills/architecture/SKILL.md` — layer boundaries for unit vs integration decisions

## Execution Flow (Orchestrator Pattern)

```
1. GATHER CONTEXT  -> Subagent reads testing + architecture KPs, story, existing code
2. PLAN TESTS      -> Orchestrator generates Double-Loop + TPP-ordered scenarios (inline)
3. ESTIMATE        -> Orchestrator estimates coverage and validates completeness (inline)
```

### Step 1: Gather Context (Subagent)

Launch a context-gathering subagent to read:
- Testing knowledge packs (philosophy + conventions)
- Architecture knowledge pack (layer boundaries)
- Story file (acceptance criteria, sub-tasks, business rules)
- Existing test classes and patterns

### Step 2: Generate Test Scenarios (Double-Loop + TPP Order)

Using the gathered context, generate a Double-Loop TDD test plan:

**Acceptance Tests (Outer Loop):**
- One AT per Gherkin scenario
- Status RED until all related unit tests pass
- Includes component list and acceptance criteria

**Unit Tests (Inner Loop — TPP Order):**

| TPP Level | Scenarios | Transform |
|-----------|-----------|-----------|
| 1 — Degenerate | Null, empty, zero | `{}→nil`, `nil→constant` |
| 2 — Unconditional | Single path, no branching | `constant→variable` |
| 3 — Simple conditions | Single if/else | `unconditional→conditional` |
| 4 — Complex conditions | Multiple branches | Deeper conditionals |
| 5 — Iterations | Collections, loops | `scalar→collection` |
| 6 — Edge cases | Boundary values | `value→mutated value` |

Each UT includes: test name, implementation hint, TPP transform, dependencies, parallel flag.

**Integration Tests:** Positioned after unit tests of involved components.

### Step 3: Estimate & Validate

| Class | Public Methods | Branches | Est. Tests | Line % | Branch % |
|-------|---------------|----------|-----------|--------|----------|
| [Name] | [count] | [count] | [count] | [%] | [%] |

Flag any class where estimated coverage < 95% line or < 90% branch.

**Quality Checks:**
1. Every Gherkin scenario maps to >= 1 AT
2. Every acceptance criterion maps to >= 1 UT chain
3. UT-1 is ALWAYS a degenerate case (TPP Level 1)
4. UTs follow non-decreasing TPP level order
5. Dependency markers complete (no orphan UTs)
6. Estimated coverage meets thresholds
7. No unnecessary UTs for CRUD-only stories (max Level 3 unless justified)

## Test Naming Convention

```
[methodUnderTest]_[scenario]_[expectedBehavior]
```

## Output

Save to: `docs/stories/epic-XXXX/plans/tests-story-XXXX-YYYY.md` (extract epic ID XXXX and story sequence YYYY from the story ID). Ensure directory exists: `mkdir -p docs/stories/epic-XXXX/plans`.

## Anti-Patterns

- Do NOT write test code — only plan scenarios
- Do NOT organize test plan by category (Happy Path, Error Path, etc.) — use TPP ordering
- Do NOT skip error paths or boundary values
- Do NOT plan tests for trivial getters/setters
- Do NOT ignore existing test patterns in the codebase
- Do NOT create redundant tests covering the same branch

## Detailed References

For in-depth guidance on test planning, consult:
- `.github/skills/x-test-plan/SKILL.md`
- `.github/skills/testing/SKILL.md`
