---
name: coding-standards
description: >
  Knowledge Pack: Coding Standards -- Clean Code rules, SOLID principles,
  rust 2024 idioms, naming patterns, constructor
  injection, mapper conventions, and approved libraries for my-rust-service.
---

# Knowledge Pack: Coding Standards

## Summary

Coding conventions for my-rust-service using rust 2024 with axum .

### Hard Limits

| Constraint | Limit |
|-----------|-------|
| Method/function length | 25 lines max |
| Class/module length | 250 lines max |
| Parameters per function | 4 max (use parameter object if more) |
| Line width | 120 characters max |

### SOLID Principles

- **SRP**: One class, one reason to change
- **OCP**: Extend via new classes, never modify existing handlers
- **LSP**: Implementations must fulfill interface contracts
- **ISP**: Small focused interfaces, no empty method stubs
- **DIP**: Depend on ports (abstractions), not concrete implementations

### Naming Conventions

- Intent-revealing names: `elapsedTimeInMs` not `d`
- Verbs for methods: `processTransaction()`, `extractAmount()`
- Nouns for types: `TransactionHandler`, `DecisionEngine`
- Named constants: never magic numbers or strings

### Forbidden Patterns

- Boolean flags as function parameters
- Returning or passing null (use Optional or empty collections)
- Mutable global state, wildcard imports, `sleep()` for synchronization

## References

- `.github/skills/coding-standards/SKILL.md` -- Full coding conventions
