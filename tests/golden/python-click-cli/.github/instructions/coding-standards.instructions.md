# Coding Standards

> Full reference: `.claude/skills/coding-standards/SKILL.md`
> (generated alongside this file) for python 3.9-specific
> idioms, constructor injection patterns, mapper conventions,
> and formatting rules.

## Hard Limits

| Constraint | Limit |
|-----------|-------|
| Method/function length | ≤ 25 lines |
| Class/module length | ≤ 250 lines |
| Parameters per function | ≤ 4 (use parameter object if more) |
| Line width | ≤ 120 characters |

## Naming

- Intent-revealing: `elapsedTimeInMs` not `d`
- Verbs for methods: `processTransaction()`, `extractAmount()`
- Nouns for types: `TransactionHandler`, `DecisionEngine`
- Named constants: never magic numbers or strings

## SOLID Principles

- **SRP**: One class = one reason to change
- **OCP**: New behavior = new class, never modify existing handlers
- **LSP**: Every implementation must fulfill its interface contract
- **ISP**: Small focused interfaces; no empty method implementations
- **DIP**: Depend on abstractions (ports), not concrete implementations

## Error Handling

- NEVER return null — use `Optional`, empty collection, or Result type
- NEVER pass null as argument
- Exceptions MUST carry context (values that caused the error)
- Catch at the right level, not where convenient

## Forbidden Patterns

- Boolean flags as function parameters
- Comments that repeat what code says
- Mutable global state
- God classes / train wrecks (chained calls across objects)
- Wildcard imports
- `sleep()` for synchronization (use condition-based polling)

## TDD Practices

- **Red-Green-Refactor** is mandatory for all production code
  1. **Red**: Write a failing test that defines the expected behavior
  2. **Green**: Write the minimum code to make the test pass
  3. **Refactor**: Improve design without changing behavior
- Refactoring criteria: extract method when > 25 lines, eliminate duplication, improve naming
- Refactoring NEVER adds behavior — if behavior changes, write a new failing test first
- Test-first commits: test must appear in git history before or in the same commit as its implementation

> **Full TDD reference:** Read `.claude/skills/testing/SKILL.md` for Double-Loop TDD, Transformation Priority Premise, and advanced TDD patterns.
