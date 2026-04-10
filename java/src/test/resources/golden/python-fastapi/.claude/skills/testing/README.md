# testing

> Complete testing reference: testing philosophy, 8 test categories, coverage thresholds, fixture patterns, data uniqueness, async handling, database strategy, and language-specific test frameworks.

| | |
|---|---|
| **Category** | Knowledge Pack |
| **Referenced by** | `x-test-plan`, `x-test-run`, `x-dev-implement`, `x-dev-story-implement`, `x-review` (QA specialist), `qa-engineer` agent |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete reference.

## Topics Covered

- Testing philosophy and 8 test categories (unit, integration, API, contract, E2E, performance, smoke, security)
- Coverage thresholds (95% line, 90% branch)
- Fixture patterns and data uniqueness strategies
- Async resource handling in tests
- Real vs in-memory database decisions
- Language-specific test frameworks and directory structure

## Key Concepts

This pack defines the complete testing philosophy with eight test categories spanning from unit tests through performance and smoke tests. It enforces strict coverage thresholds (95% line, 90% branch) as non-negotiable quality gates and mandates Double-Loop TDD with Transformation Priority Premise ordering. Fixture patterns ensure test isolation through data uniqueness strategies, while database strategy guidance helps choose between real and in-memory databases for integration testing. The philosophy emphasizes that high coverage with test-after is insufficient -- TDD compliance is required.

## See Also

- [coding-standards](../coding-standards/) — TDD practices, Red-Green-Refactor cycle, and code hygiene rules
- [layer-templates](../layer-templates/) — Code templates that tests validate per architecture layer
- [story-planning](../story-planning/) — Acceptance criteria and Gherkin scenarios that drive test plans
