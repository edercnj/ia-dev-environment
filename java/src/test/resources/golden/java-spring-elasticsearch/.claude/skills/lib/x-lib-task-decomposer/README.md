# x-lib-task-decomposer

> Decomposes an implementation plan into tasks. Primary mode: derives tasks from test scenarios (x-test-plan output) using TDD structure (RED/GREEN/REFACTOR). Fallback mode: uses Layer Task Catalog (G1-G7) when no test plan exists.

| | |
|---|---|
| **Category** | Library (internal) |
| **Called by** | x-dev-story-implement |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## Purpose

Breaks down an Architect's implementation plan into granular, implementable tasks with tier assignments (Junior/Mid/Senior) and context budgets. When a test plan from `x-test-plan` exists, it produces TDD-structured tasks with RED/GREEN/REFACTOR steps; otherwise it falls back to the Layer Task Catalog (G1-G7) with parallelism groups.

## Integration Points

| Caller | Context | Input | Output |
|--------|---------|-------|--------|
| x-dev-story-implement | Phase 1C, after Architect plan and before implementation | Architect plan (`plan-story-*.md`), story file, test plan (`tests-story-*.md`, optional) | Task breakdown file (`tasks-story-*.md`) with ordered tasks, tiers, budgets, and dependencies |

## Procedure

1. **Idempotency check** -- verify if a fresh task breakdown already exists and reuse it if the story has not changed
2. **Read context** -- load architecture principles, layer templates, Architect plan, story file, and output template
3. **Detect mode** -- check for test plan with TPP markers; use test-driven decomposition (STEP 2A) if found, layer-based fallback (STEP 2B) otherwise
4. **Decompose** -- generate tasks with dependencies, parallelism flags, tier assignments, and context budgets
5. **Write output** -- save task breakdown to `plans/epic-XXXX/plans/tasks-story-XXXX-YYYY.md`

## See Also

- [x-test-plan](../../x-test-plan/) -- generates the test plan consumed as primary input
- [x-dev-story-implement](../../x-dev-story-implement/) -- orchestrator that invokes this skill during Phase 1C
- [x-lib-group-verifier](../x-lib-group-verifier/) -- verifies build gates between the parallelism groups this skill defines
