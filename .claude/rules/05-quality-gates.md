# Rule 05 — Quality Gates

> **Full reference:** Read `skills/testing/SKILL.md` for test patterns and conventions.

## Coverage Thresholds (Non-Negotiable)

| Metric | Minimum |
|--------|---------|
| Line Coverage | ≥ 95% |
| Branch Coverage | ≥ 90% |

## Test Categories

1. **Unit** — domain models, engines, business rules (no mocks of domain; mock only external)
2. **Integration** — database + framework (real or in-memory DB)
3. **API** — HTTP/gRPC/GraphQL endpoints (status codes, response bodies, error formats)
4. **Contract** — parametrized business rules (one row per scenario)
5. **E2E** — full flow with real database (containers)
6. **Performance** — latency SLAs, throughput, resource usage
7. **Smoke** — black-box against running environment

## Test Naming

```
[methodUnderTest]_[scenario]_[expectedBehavior]
```

- `@DisplayName` / docstrings provide readability but do NOT replace method name convention
- Non-conforming names are treated as violations during review

## Cross-File Consistency

- Error handling pattern MUST be uniform across classes of the same role (e.g., all assemblers, all controllers)
- Constructor patterns, return types, and internal type definitions MUST follow the same shape within a module
- Inconsistency across files of the same role is a MEDIUM-severity violation

## Merge Checklist

- [ ] All tests passing
- [ ] Coverage ≥ 95% line, ≥ 90% branch
- [ ] Zero compiler/linter warnings
- [ ] DB migration applied and tested (if applicable)
- [ ] Security review for sensitive changes
- [ ] Infrastructure manifests updated (if applicable)
- [ ] Commits show test-first pattern (test precedes implementation in git log)
- [ ] Explicit refactoring after green
- [ ] Tests are incremental (simple to complex via TPP)
- [ ] Tests precede or accompany implementation (no test-after in later commits)
- [ ] Acceptance tests exist and validate end-to-end behavior
- [ ] No cross-file consistency violations (uniform patterns within module)
- [ ] No weak assertions (every test verifies specific behavior)
- [ ] Test plan was generated before implementation (mandatory prerequisite)
- [ ] No G1-G7 fallback used

## Forbidden

- Skipping tests to make CI pass
- Mocking domain logic
- Using production data in tests
- Depending on test execution order
- `sleep()` for async waiting (use polling with timeout)
- Weak assertions: `isNotNull()` alone is never sufficient — assert specific values, sizes, or content
- Test files > 250 lines without nested class / inner module organization
- Duplicate type definitions (records, classes) across test files — extract to shared fixtures

## TDD Compliance (Non-Negotiable)

BLOCKING violations:
- Implementation without test plan (no G1-G7 fallback allowed)
- Test-after pattern (production code committed before corresponding test)
- Missing acceptance tests for Gherkin scenarios
- Refactoring commits that add new behavior

Enforced at three levels:
1. **x-dev-lifecycle Phase 0**: Aborts if no test plan exists
2. **Integrity Gate**: Reports TDD compliance warnings via `tddCompliance` data
3. **Tech Lead Review**: NO-GO if TDD process items (41-42) fail

Additional TDD requirements:
- **Double-Loop TDD**: Outer loop (acceptance test, failing) drives inner loop (unit tests, Red-Green-Refactor)
- **Transformation Priority Premise (TPP)**: Order tests from simple to complex — `{} → nil → constant → constant+ → scalar → collection → …`
- **Atomic TDD commits**: Each Red-Green-Refactor cycle produces one or more atomic commits with Conventional Commits format
- Coverage thresholds (see above) are NOT a substitute for TDD — high coverage with test-after is insufficient
