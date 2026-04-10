# x-dev-implement

> Implements a feature/story using TDD (Red-Green-Refactor) workflow. Delegates preparation to a subagent that reads architecture, coding, and test plan KPs, then implements test-first with Double-Loop TDD, layer-by-layer with compile checks after each cycle.

| | |
|---|---|
| **Category** | Implementation |
| **Invocation** | `/x-dev-implement [STORY-ID or feature-description]` |
| **Reads** | architecture, coding-standards, layer-templates, testing |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Implements a feature or story end-to-end using strict TDD discipline. It reuses any existing plans (implementation, architecture, test) produced by prior planning skills, then executes Red-Green-Refactor cycles in Transformation Priority Premise order. Each cycle includes a compile check, and the final output meets coverage thresholds (95% line, 90% branch).

## Usage

```
/x-dev-implement
/x-dev-implement plans/epic-0012/story-0012-0003.md
/x-dev-implement "Add payment retry logic"
```

## Workflow

1. Pre-check for existing plans (implementation, architecture, test) and assess staleness
2. Subagent reads KPs, story, and available plans to produce a TDD implementation plan
3. Write acceptance tests (outer loop, starts RED)
4. Execute inner-loop Red-Green-Refactor per unit test in TPP order with compile checks
5. Validate coverage thresholds and all acceptance tests GREEN
6. Create atomic TDD commits (one per Red-Green-Refactor cycle)

## Outputs

| Artifact | Path |
|----------|------|
| Implementation plan | `plans/epic-XXXX/plans/plan-story-XXXX-YYYY.md` |
| Production code + tests | Project source tree |

## See Also

- [x-dev-story-implement](../x-dev-story-implement/) — Full lifecycle: code, review, fix, PR
- [x-test-plan](../x-test-plan/) — Generate test plan before implementation
- [x-dev-architecture-plan](../x-dev-architecture-plan/) — Generate architecture plan before implementation
