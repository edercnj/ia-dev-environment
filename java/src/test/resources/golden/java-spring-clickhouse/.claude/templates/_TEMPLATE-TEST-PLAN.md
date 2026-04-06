# Test Plan -- {{STORY_ID}}

## Header

| Field | Value |
|-------|-------|
| Story ID | {{STORY_ID}} |
| Epic ID | {{EPIC_ID}} |
| Date | {{DATE}} |
| Author | {{AUTHOR}} |
| Template Version | 1.0.0 |

## Summary

| Metric | Count |
|--------|-------|
| Acceptance Tests (AT) | {{AT_COUNT}} |
| Unit Tests (UT) | {{UT_COUNT}} |
| Integration Tests (IT) | {{IT_COUNT}} |
| Estimated Line Coverage | {{LINE_COVERAGE}} |
| Estimated Branch Coverage | {{BRANCH_COVERAGE}} |

## Acceptance Tests (Outer Loop)

> Double-Loop TDD: Acceptance tests define the outer loop. Each AT validates end-to-end
> behavior derived from Gherkin scenarios. ATs start RED and remain RED until all inner-loop
> unit tests are GREEN and the feature is complete.

| AT ID | Gherkin Ref | Description | Status | Components | Depends On | Parallel |
|-------|------------|-------------|--------|------------|-----------|----------|
| AT-1 | {{GHERKIN_REF}} | {{DESCRIPTION}} | Pending | {{COMPONENTS}} | -- | {{PARALLEL}} |
| AT-2 | {{GHERKIN_REF}} | {{DESCRIPTION}} | Pending | {{COMPONENTS}} | AT-1 | {{PARALLEL}} |

## Unit Tests (Inner Loop - TPP Order)

> Transformation Priority Premise (TPP): Tests are ordered from simplest to most complex
> transformation. Each test drives the minimum code change needed.
>
> TPP Levels: nil -> constant -> constant+ -> scalar -> collection

| UT ID | Test Name | Implementation Hint | Transform | TPP Level | Components | Depends On | Parallel |
|-------|-----------|-------------------|-----------|-----------|------------|-----------|----------|
| UT-1 | {{TEST_NAME}} | {{HINT}} | {} -> nil | nil | {{COMPONENTS}} | -- | {{PARALLEL}} |
| UT-2 | {{TEST_NAME}} | {{HINT}} | nil -> constant | constant | {{COMPONENTS}} | UT-1 | {{PARALLEL}} |
| UT-3 | {{TEST_NAME}} | {{HINT}} | constant -> constant+ | constant+ | {{COMPONENTS}} | UT-2 | {{PARALLEL}} |
| UT-4 | {{TEST_NAME}} | {{HINT}} | constant+ -> scalar | scalar | {{COMPONENTS}} | UT-3 | {{PARALLEL}} |
| UT-5 | {{TEST_NAME}} | {{HINT}} | scalar -> collection | collection | {{COMPONENTS}} | UT-4 | {{PARALLEL}} |

## Integration Tests

| IT ID | Description | Components | Depends On | Parallel |
|-------|------------|------------|-----------|----------|
| IT-1 | {{DESCRIPTION}} | {{COMPONENTS}} | -- | {{PARALLEL}} |

## Coverage Estimation Table

| Class | Public Methods | Branches | Est. Tests | Line % | Branch % |
|-------|---------------|----------|-----------|--------|----------|
| {{CLASS}} | {{PUBLIC_METHODS}} | {{BRANCHES}} | {{EST_TESTS}} | {{LINE_PCT}} | {{BRANCH_PCT}} |

## Risks and Gaps

| Risk | Severity | Description | Mitigation |
|------|----------|-------------|------------|
| {{RISK}} | {{SEVERITY}} | {{DESCRIPTION}} | {{MITIGATION}} |

## Language-Specific Notes

### Test Framework: {{LANGUAGE}} / {{FRAMEWORK}}

- {{NOTE_1}}
- {{NOTE_2}}

### Assertion Libraries

| Library | Purpose |
|---------|---------|
| {{LIBRARY}} | {{PURPOSE}} |

### Fixture Patterns

{{FIXTURE_PATTERNS}}
