# Quality Gates

> Full reference: `.claude/skills/testing/SKILL.md`
> (generated alongside this file) for test patterns, fixtures,
> and framework-specific conventions.

## Coverage Thresholds (Non-Negotiable)

| Metric | Minimum |
|--------|---------  |
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

```text
[methodUnderTest]_[scenario]_[expectedBehavior]
```

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
- [ ] No test written AFTER implementation
- [ ] Acceptance tests exist and validate end-to-end behavior

## Forbidden

- Skipping tests to make CI pass
- Mocking domain logic
- Using production data in tests
- Depending on test execution order
- `sleep()` for async waiting (use polling with timeout)

## TDD Compliance

- **Double-Loop TDD**: Outer loop (acceptance test, failing) drives inner loop (unit tests, Red-Green-Refactor)
- **Transformation Priority Premise (TPP)**: Order tests from simple to complex — `{} → nil → constant → constant+ → scalar → collection → …`
- **Atomic TDD commits**: Each Red-Green-Refactor cycle produces one or more atomic commits with Conventional Commits format
- Coverage thresholds (see above) are NOT a substitute for TDD — high coverage with test-after is insufficient
