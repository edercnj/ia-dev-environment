# Rule 05 — Quality Gates

> **Full reference:** Read `knowledge/testing.md` for test patterns and conventions.

## Coverage Thresholds (Non-Negotiable — Absolute Gate)

| Metric | Minimum |
|--------|---------|
| Line Coverage | ≥ 95% |
| Branch Coverage | ≥ 90% |

### Absolute-Gate Rule (RULE-005-01)

Coverage thresholds are an **absolute gate**. A PR fails the review if the
repository's line coverage is below 95% or branch coverage is below 90%,
**regardless of whether the deficit was caused by the PR or was pre-existing
on the base branch**. There is no "pre-existing exemption".

Rationale:

- Pre-existing deficits would accumulate indefinitely if grandfathered. The
  gate would become meaningless.
- A PR that does not add Java main-source code (e.g., documentation /
  metadata epics) is free to add tests to close pre-existing gaps.
- The gate keeps the `develop` baseline trustworthy: every merged PR leaves
  `develop` at or above the thresholds.

### Operator options when the gate fires

1. **Add tests in the current PR** to close the gap. This is the default
   and is what the review skills (`x-review`, `x-review-pr`) enforce.
2. **Split the concerns:** open a separate PR that adds tests to reach the
   thresholds on `develop` first, then rebase the feature PR.
3. **Document an explicit exception** via an ADR that temporarily lowers
   the gate for a specific package with a sunset date. The ADR MUST be
   approved by the tech lead and recorded under `adr/ADR-NNN-*.md`.

**Silently overriding the gate is forbidden.** No reviewer may merge a PR
that fails the gate without either (a) closing the gap in-PR, (b) closing
it in a predecessor PR, or (c) the ADR escape hatch.

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
- [ ] Coverage ≥ 95% line, ≥ 90% branch (absolute gate — pre-existing deficits MUST be closed in-PR or in a predecessor)
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

## Forbidden

- Skipping tests to make CI pass
- Mocking domain logic
- Using production data in tests
- Depending on test execution order
- `sleep()` for async waiting (use polling with timeout)
- Weak assertions: `isNotNull()` alone is never sufficient — assert specific values, sizes, or content
- Test files > 250 lines without nested class / inner module organization
- Duplicate type definitions (records, classes) across test files — extract to shared fixtures

## TDD Compliance

- **Double-Loop TDD**: Outer loop (acceptance test, failing) drives inner loop (unit tests, Red-Green-Refactor)
- **Transformation Priority Premise (TPP)**: Order tests from simple to complex — `{} → nil → constant → constant+ → scalar → collection → …`
- **Atomic TDD commits**: Each Red-Green-Refactor cycle produces one or more atomic commits with Conventional Commits format
- Coverage thresholds (see above) are NOT a substitute for TDD — high coverage with test-after is insufficient
