# Global Behavior & Language Policy
- **Output Language**: English ONLY. (Mandatory for all responses and internal reasoning).
- **Token Optimization**: Eliminate all greetings, apologies, and conversational fluff. Start responses directly with technical information.
- **Priority**: Maintain 100% fidelity to the technical constraints defined in the original rules below.

# Rule 03 — Testing Philosophy

## Coverage Thresholds

| Metric | Minimum |
|--------|---------|
| Line Coverage | >= 95% |
| Branch Coverage | >= 90% |

These thresholds are enforced by the coverage tool and are non-negotiable. Untested code does not ship.

## Naming Convention

```
[methodUnderTest]_[scenario]_[expectedBehavior]
```

Examples:
- `processDebit_approvedAmount_returnsRC00`
- `parseReversal_missingOriginalData_throwsException`
- `findMerchant_nonExistentId_returnsEmpty`
- `createMerchant_validPayload_returns201`

## Test Categories

### 1. Unit Tests (domain + business logic)
- Test domain models, decision engines, business rules
- No mocking of domain logic — use real objects
- Mocking PERMITTED only for: external clients, network services, clock/time
- Parametrized tests for business rules with multiple inputs/outputs

### 2. Integration Tests (application + database)
- Test real database interactions
- Use in-memory database for speed when compatible (e.g., H2 with PostgreSQL mode, SQLite)
- Use real database (via containers) when in-memory is insufficient
- Each test should be isolated — automatic rollback per test

### 3. API Tests (REST / GraphQL / gRPC)
- Test HTTP endpoints end-to-end within the framework
- Validate: status codes, response bodies, headers, error formats
- Use framework-provided test utilities (not raw HTTP clients)

### 4. Protocol Tests (TCP / WebSocket / custom)
- Test protocol-level communication (framing, encoding, handshake)
- Use test client utilities that mirror real protocol behavior
- Verify: connection persistence, error recovery, timeout handling

### 5. Contract Tests (parametrized)
- Test business rules exhaustively via parametrized inputs
- Example: cents-based decisions, pricing tiers, validation rules
- Each row in the parameter table represents a documented business scenario

### 6. End-to-End Tests (full flow)
- Test complete flow from external input to database and back
- Use real database (containers)
- Validate: data persisted correctly, response matches, side effects occur

### 7. Performance Tests (load / stress)
- Validate latency SLAs and throughput under load
- Mandatory scenarios: baseline, normal load, peak, sustained
- Collected metrics: p50, p75, p95, p99 latency, TPS, error rate, memory

### 8. Smoke Tests (external validation)
- Black-box tests against a running environment
- Validate health checks, critical endpoints, basic flows
- Idempotent: create own test data, clean up after
- See dedicated smoke test rules for scenarios and execution

## Test Directory Structure

```
tests/
├── unit/              # Domain unit tests
├── integration/       # Database + framework integration
├── api/               # REST/GraphQL/gRPC endpoint tests
├── protocol/          # TCP/WebSocket protocol tests
├── e2e/               # End-to-end flow tests
├── performance/       # Load and stress tests
└── fixtures/          # Test fixtures, builders, helpers
```

> Adapt directory naming to your language convention (e.g., `src/test/java/...` for Java, `__tests__/` for JS/TS, `tests/` for Python).

## Test Fixtures — Static Utility Pattern

Centralize test data in utility classes/modules with static/module-level factory functions:

```
// Pattern: final class + private constructor + static methods
class MerchantFixture:
    VALID_DOCUMENT = "12345678000190"

    static function aMerchant(id):
        return Merchant(id, "Test Store", VALID_DOCUMENT, "ACTIVE")

    static function aMerchantWithTimeout(id):
        return Merchant(id, "Timeout Store", VALID_DOCUMENT, "ACTIVE", timeout=true)
```

**Fixture Rules:**
- Utility class: never instantiate directly
- All methods static / module-level
- Naming: `a{Entity}()` or `a{Entity}With{Variation}()`
- Constants for default values
- Domain fixtures separate from protocol fixtures

## Data Uniqueness in Tests

Tests that create resources MUST generate unique identifiers to avoid conflicts:

```
function uniqueId():
    return String(currentTimeNanos() % 1_000_000_000)
```

**Rules:**
- NEVER use fixed IDs in tests that create resources
- Tests validating duplicate detection should create the resource first, then attempt duplicate
- Each test run must be independent of previous runs

## Asynchronous Resource Handling

Tests depending on asynchronous resources (servers, connections) MUST use polling with timeout:

```
// GOOD — poll until ready
awaitUntil(timeout=10s, condition=() -> server.isListening())

// BAD — sleep and hope
sleep(5000)
```

**Rules:**
- Use dedicated polling utilities (Awaitility, polling loops with backoff)
- Default timeout: 10 seconds for resource startup
- Use in setup/beforeEach to guarantee preconditions
- NEVER use `sleep()` for waiting on resources — use condition-based polling
- Exception: `sleep()` is permitted in tests that validate timeout behavior

## Prohibitions

- NEVER use framework-default assertions (`assertEquals`, `assertTrue`) when richer assertion libraries are available
- NEVER mock domain logic — test with real domain objects
- Mocking PERMITTED only for: external clients, network services, clock/time
- NEVER use production data in tests
- NEVER depend on test execution order
- NEVER skip tests to make CI pass

## When to Use Real Database vs In-Memory

| Scenario | Database |
|----------|----------|
| Repository unit tests | In-memory (default) |
| API endpoint tests | In-memory (default) |
| Protocol tests | In-memory (default) |
| Migration validation | Real database (containers) |
| DB-specific features (JSON, full-text, etc.) | Real database (containers) |
| Performance / volume testing | Real database (containers) |
| EXPLAIN ANALYZE / query plan validation | Real database (containers) |

## TDD Workflow (Red-Green-Refactor)

Test-Driven Development follows a strict three-phase cycle. Every production code change begins with a failing test.

### RED — Write a Failing Test

- Write ONE test that describes the next desired behavior.
- The test MUST fail before any production code is written.
- Keep the test as simple as possible — test one thing at a time.
- Run the test suite and confirm the new test fails (and only the new test).

### GREEN — Make It Pass

- Write the MINIMUM production code to make the failing test pass.
- No optimizations, no generalizations, no "while I'm here" changes.
- Hardcoding a return value is acceptable if it satisfies the test.
- Run the full test suite — all tests MUST be green.

### REFACTOR — Improve the Design

- Eliminate duplication between test and production code.
- Improve naming, extract methods, simplify conditionals.
- Apply design patterns only when duplication or complexity demands it.
- Run the full test suite after every refactoring step — all tests MUST remain green.
- NEVER add new behavior during refactoring. If you need new behavior, start a new RED phase.

### TDD Rules

- **NEVER** write production code without a failing test first.
- **NEVER** write more than one failing test at a time (unless using Double-Loop TDD outer test).
- **NEVER** add behavior during the REFACTOR phase.
- **NEVER** refactor while tests are red.
- Commit after each GREEN and after each REFACTOR.

## Double-Loop TDD

Double-Loop TDD connects acceptance-level requirements (from Gherkin scenarios) with unit-level implementation cycles.

### Outer Loop — Acceptance Test

- Derived directly from the story's Gherkin acceptance criteria.
- Written FIRST, before any unit tests.
- Stays RED throughout the entire implementation — it only turns GREEN when ALL required behavior is implemented.
- The acceptance test is the Definition of Done for the story.

### Inner Loop — Unit Test Cycles

- Rapid Red-Green-Refactor cycles within the outer loop.
- Each cycle implements one small piece of the required functionality.
- Each completed inner cycle moves the acceptance test closer to GREEN.
- Continue inner cycles until the acceptance test passes.

### Double-Loop Interaction

```
Outer Loop (Acceptance Test):  RED ──────────────────────────────────────── GREEN
                                │                                            ▲
                                ▼                                            │
Inner Loop (Unit Tests):       RED → GREEN → REFACTOR → RED → GREEN → ... ──┘
                               ├── cycle 1 ──┤├── cycle 2 ──┤     ├── cycle N ──┤
```

### Double-Loop Rules

- The acceptance test is written ONCE at the start and not modified until it passes.
- Each inner cycle must produce a meaningful increment toward the acceptance test.
- If an inner cycle does not contribute to the acceptance test, re-evaluate the test ordering.
- The acceptance test passing is the ONLY signal that the story is complete.

## Transformation Priority Premise (TPP)

The Transformation Priority Premise defines an ordered list of code transformations, from simplest to most complex. When making a test pass, always choose the transformation with the LOWEST priority number that satisfies the test.

### Transformation List (Ordered)

| Priority | Transformation | Description | Example |
|----------|---------------|-------------|---------|
| 1 | `{}→nil` | No code → return nil/null/undefined | Empty function returns null |
| 2 | `nil→constant` | Return a fixed literal value | `return 0`, `return ""`, `return true` |
| 3 | `constant→variable` | Replace constant with a variable or parameter | `return 0` → `return input` |
| 4 | `unconditional→conditional` | Add branching (if/else, ternary) | `return x` → `if (x > 0) return x else return 0` |
| 5 | `scalar→collection` | Single value → work with lists/arrays/sets | `int` → `int[]`, iterate over elements |
| 6 | `statement→recursion/iteration` | Add loops or recursive calls | `return sum` → `for (item in list) sum += item` |
| 7 | `value→mutated value` | Transform or compute derived values | `return x` → `return x * factor + offset` |

### TPP Rules

- Always choose the transformation of LOWEST priority that makes the current test pass.
- If a higher-priority transformation is needed, consider whether a simpler test should have been written first.
- Test scenarios MUST be ordered to guide transformations from low to high priority.
- Skipping priority levels (e.g., jumping from constant to iteration) signals missing intermediate test cases.

## Test Scenario Ordering

Test scenarios should be ordered by complexity level to guide the implementation through TPP transformations naturally. Write and implement tests in this order:

### Level 1 — Degenerate Cases

- Null, empty, zero, or undefined inputs.
- Constant return values.
- These scenarios drive `{}→nil` and `nil→constant` transformations.

### Level 2 — Unconditional Paths

- Single execution path with no branching.
- Simplest valid input producing a direct output.
- These scenarios drive `constant→variable` transformations.

### Level 3 — Simple Conditions

- Single if/else or ternary branching.
- Two distinct execution paths based on one condition.
- These scenarios drive `unconditional→conditional` transformations.

### Level 4 — Complex Conditions

- Multiple branches: switch/match, nested conditions, compound boolean logic.
- Three or more execution paths.
- These scenarios drive deeper conditional transformations.

### Level 5 — Iterations

- Processing collections: loops, map/filter/reduce, recursion.
- Variable-length input producing aggregated output.
- These scenarios drive `scalar→collection` and `statement→recursion/iteration` transformations.

### Level 6 — Edge Cases

- Boundary values: at-minimum, at-maximum, just-past-maximum.
- Overflow, underflow, precision limits.
- Concurrent access, race conditions (where applicable).
- These scenarios validate robustness and drive `value→mutated value` transformations.
