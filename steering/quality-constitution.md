# Quality Constitution — ia-dev-environment

> Derived from: .claude/rules/05-quality-gates.md, .claude/rules/03-coding-standards.md

## Coverage Thresholds (Non-Negotiable)

| Metric | Minimum |
|--------|---------|
| Line Coverage | >= 95% |
| Branch Coverage | >= 90% |

Enforced by JaCoCo in `mvn verify`. Build fails if thresholds are not met.

## TDD Practices (Mandatory)

- **Red-Green-Refactor** is mandatory for all production code
  1. **Red**: Write a failing test that defines the expected behavior
  2. **Green**: Write the minimum code to make the test pass
  3. **Refactor**: Improve design without changing behavior
- Double-Loop TDD: outer loop (acceptance tests) drives inner loop (unit tests)
- Transformation Priority Premise (TPP): order tests from simple to complex
- Test-first commits: test must appear in git history before or with its implementation

## Coding Hard Limits

| Constraint | Limit |
|-----------|-------|
| Method/function length | <= 25 lines |
| Class/module length | <= 250 lines |
| Parameters per function | <= 4 |
| Line width | <= 120 characters |
| Train wreck depth | <= 2 levels |

## Merge Checklist

- All tests passing
- Coverage >= 95% line, >= 90% branch
- Zero compiler/linter warnings
- Commits show test-first pattern
- Explicit refactoring after green
- Tests are incremental (simple to complex via TPP)
- Acceptance tests exist and validate end-to-end behavior
- No cross-file consistency violations
- No weak assertions (every test verifies specific behavior)
- Test plan was generated before implementation

## Forbidden Practices

- Skipping tests to make CI pass
- Mocking domain logic
- Using production data in tests
- Depending on test execution order
- `sleep()` for async waiting
- Weak assertions (`isNotNull()` alone is never sufficient)
- Boolean flags as function parameters
- Mutable global state
- Wildcard imports
- Dead code
