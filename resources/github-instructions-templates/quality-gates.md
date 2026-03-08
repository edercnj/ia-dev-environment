# Quality Gates

> Full reference: `.claude/skills/testing/SKILL.md`
> (generated alongside this file) for test patterns, fixtures,
> and framework-specific conventions.

## Coverage Thresholds (Non-Negotiable)

| Metric | Minimum |
|--------|---------  |
| Line Coverage | ≥ {coverage_line}% |
| Branch Coverage | ≥ {coverage_branch}% |

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
- [ ] Coverage ≥ {coverage_line}% line, ≥ {coverage_branch}% branch
- [ ] Zero compiler/linter warnings
- [ ] DB migration applied and tested (if applicable)
- [ ] Security review for sensitive changes
- [ ] Infrastructure manifests updated (if applicable)

## Forbidden

- Skipping tests to make CI pass
- Mocking domain logic
- Using production data in tests
- Depending on test execution order
- `sleep()` for async waiting (use polling with timeout)
