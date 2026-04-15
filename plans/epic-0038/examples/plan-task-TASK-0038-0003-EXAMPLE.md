# Task Implementation Plan — TASK-0038-0003-EXAMPLE

> Derived from `task-TASK-0038-0003-EXAMPLE.md` (story-0038-0001 schema).

## 1. Resumo

- **Objetivo:** Add `Greeter.greet(String name)` returning `"Hello, {name}!"` for
  non-empty input and throwing `IllegalArgumentException` on null/blank.
- **Testabilidade:** INDEPENDENT — pure method, no collaborators, no I/O.

## 2. Red-Green-Refactor Cycles (TPP Order)

| # | Phase | Test Name | Green Code Summary | Refactor Hint |
| :--- | :--- | :--- | :--- | :--- |
| 1 | Degenerate | `greet_nullName_throwsIllegalArgument` | Add guard `if (name == null) throw new IllegalArgumentException("name")` | None at this cycle |
| 2 | Degenerate | `greet_blankName_throwsIllegalArgument` | Extend guard to `name.isBlank()` | None at this cycle |
| 3 | Constant | `greet_anyValidName_returnsFixedPrefix` | Return hardcoded `"Hello, world!"` | None |
| 4 | Scalar | `greet_validName_returnsHelloCommaName` | Parameterise: `return "Hello, " + name + "!"` | Extract a constant `GREETING_FORMAT` |
| 5 | Edge case | `greet_nameWithTrailingSpaces_isTrimmed` | Add `name.trim()` before concatenation | Extract `normaliseName(String)` helper method |

## 3. File Impact Analysis

| Cycle | Layer | Files (new/modified) |
| :--- | :--- | :--- |
| 1 | Domain | `example/greet/Greeter.java` (new) — guard only |
| 1 | Test | `example/greet/GreeterTest.java` (new) |
| 2 | Domain | `example/greet/Greeter.java` (modified) |
| 2 | Test | `example/greet/GreeterTest.java` (modified) |
| 3 | Domain | `example/greet/Greeter.java` (modified) |
| 3 | Test | `example/greet/GreeterTest.java` (modified) |
| 4 | Domain | `example/greet/Greeter.java` (modified) |
| 4 | Test | `example/greet/GreeterTest.java` (modified) |
| 5 | Domain | `example/greet/Greeter.java` (modified) |
| 5 | Test | `example/greet/GreeterTest.java` (modified) |

## 4. TPP Justification

Degenerate cases (null + blank) come first so the method's contract surface is locked
before any happy-path code is written. The constant cycle (#3) precedes the scalar
cycle (#4) because forcing ourselves to return a hardcoded string first exposes the
temptation to skip the transformation — and the hardcoded answer is trivially wrong
against the scalar test, driving the minimum real change. The edge case (#5) lands
last because trimming is a requirement that only surfaces when a specific input
pattern is exercised; adding it earlier would be speculative.

## 5. Exit Criteria

- [ ] Cycles 1-5 all GREEN with passing tests
- [ ] Refactor step applied where noted (#4, #5)
- [ ] `mvn clean verify` green
- [ ] Commits follow Conventional Commits with TDD tags: `feat(task-0038-0003-EXAMPLE): degenerate null guard [TDD:RED]` etc.
- [ ] Contracts I/O from task file respected (`grep "greet" Greeter.java` returns the method)
- [ ] One commit per cycle (RULE-TF-04)
