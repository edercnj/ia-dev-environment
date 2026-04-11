# x-task-implement

> Executes a single task (TDD Red-Green-Refactor loop) from a story's task plan. Delegates preparation to a subagent that reads architecture, coding, and test plan KPs, then implements the task test-first with Double-Loop TDD, with compile checks after each cycle.

| | |
|---|---|
| **Category** | Implementation |
| **Invocation** | `/x-task-implement [TASK-ID or task-plan-path]` |
| **Reads** | architecture, coding-standards, layer-templates, testing |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Implements a **single task** (one unit of work from a story's task plan) using strict TDD discipline. The task plan is produced by `x-task-plan` (or by `x-story-implement` Phase 1 decomposition); this skill executes one task at a time. For each task, it runs Red-Green-Refactor cycles in Transformation Priority Premise order with compile checks, and creates atomic TDD commits per cycle.

For story-level orchestration (multiple tasks + planning + review + PR), use `x-story-implement` instead.

## Usage

```
/x-task-implement
/x-task-implement TASK-001
/x-task-implement plans/epic-0012/plans/task-plan-TASK-001-story-0012-0003.md
```

## Workflow

1. Pre-check for the task plan (produced by `x-task-plan` or `x-story-implement` Phase 1)
2. Subagent reads KPs and the task plan to prepare TDD execution
3. Write/extend acceptance tests for the task scope (outer loop, starts RED)
4. Execute inner-loop Red-Green-Refactor per unit test in TPP order with compile checks
5. Validate coverage thresholds and all task-scoped acceptance tests GREEN
6. Create atomic TDD commits (one per Red-Green-Refactor cycle)

## Outputs

| Artifact | Path |
|----------|------|
| Atomic TDD commits on the current branch | git history |
| Production code + tests | Project source tree |

## See Also

- [x-story-implement](../x-story-implement/) — Full story lifecycle: planning, implementation across multiple tasks, review, fix, PR
- [x-task-plan](../../plan/x-task-plan/) — Generate the task plan consumed by this skill
- [x-test-plan](../../test/x-test-plan/) — Generate test plan before implementation
- [x-arch-plan](../../plan/x-arch-plan/) — Generate architecture plan before implementation
