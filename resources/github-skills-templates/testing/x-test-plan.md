---
name: x-test-plan
description: >
  Generates a comprehensive test plan before implementation. Delegates KP
  reading to a context-gathering subagent, then produces structured test
  scenarios covering unit, integration, API, E2E, contract, and performance
  tests. Use when planning tests for a story or feature.
---

# Skill: Plan Tests (Orchestrator)

## Purpose

Produces a comprehensive, actionable test plan BEFORE any test code is written. With line coverage >= 95% and branch coverage >= 90% enforced, upfront planning avoids coverage gaps.

**Condition**: Use before implementing tests for any story or feature.

## Prerequisites

- Story file with acceptance criteria and sub-tasks
- Existing codebase with established test patterns
- Knowledge of {language_name} test frameworks and conventions

## Knowledge Pack References

Before planning tests, read:
- `.github/skills/testing/SKILL.md` — 8 test categories, fixture patterns, data uniqueness, {language_name}-specific test frameworks, naming conventions
- `.github/skills/architecture/SKILL.md` — layer boundaries for unit vs integration decisions

## Execution Flow (Orchestrator Pattern)

```
1. GATHER CONTEXT  -> Subagent reads testing + architecture KPs, story, existing code
2. PLAN TESTS      -> Orchestrator generates scenarios using returned context (inline)
3. ESTIMATE        -> Orchestrator estimates coverage and validates completeness (inline)
```

### Step 1: Gather Context (Subagent)

Launch a context-gathering subagent to read:
- Testing knowledge packs (philosophy + conventions)
- Architecture knowledge pack (layer boundaries)
- Story file (acceptance criteria, sub-tasks, business rules)
- Existing test classes and patterns

### Step 2: Generate Test Scenarios

Using the gathered context, generate scenarios by category:

| Category | When to Use | Key Patterns |
|----------|-------------|--------------|
| Unit | Domain logic, engines, mappers | AAA pattern, mock only external |
| Integration | DB interactions, framework features | Real or in-memory DB |
| API | REST/gRPC endpoints | Status codes, error formats |
| E2E | Full flow with real database | Container-based, independent |
| Contract | Protocol/format compliance | Parametrized, consumer-driven |
| Performance | Latency SLAs, throughput | Baseline, normal, peak, sustained |

### Step 3: Estimate & Validate

| Class | Public Methods | Branches | Est. Tests | Line % | Branch % |
|-------|---------------|----------|-----------|--------|----------|
| [Name] | [count] | [count] | [count] | [%] | [%] |

Flag any class where estimated coverage < 95% line or < 90% branch.

## Test Naming Convention

```
[methodUnderTest]_[scenario]_[expectedBehavior]
```

## Output

Save to: `docs/plans/STORY-ID-tests.md`

## Anti-Patterns

- Do NOT write test code — only plan scenarios
- Do NOT skip error paths or boundary values
- Do NOT plan tests for trivial getters/setters
- Do NOT ignore existing test patterns in the codebase
- Do NOT create redundant tests covering the same branch

## Detailed References

For in-depth guidance on test planning, consult:
- `.github/skills/x-test-plan/SKILL.md`
- `.github/skills/testing/SKILL.md`
