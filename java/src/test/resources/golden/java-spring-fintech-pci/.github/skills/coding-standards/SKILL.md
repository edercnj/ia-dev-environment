---
name: coding-standards
description: >
  Knowledge Pack: Coding Standards -- Clean Code rules, SOLID principles,
  java 21 idioms, naming patterns, constructor
  injection, mapper conventions, and approved libraries for my-spring-fintech-pci.
---

# Knowledge Pack: Coding Standards

## Summary

Coding conventions for my-spring-fintech-pci using java 21 with spring-boot 3.x.

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

- [Clean Code — Robert C. Martin](https://www.oreilly.com/library/view/clean-code-a/9780136083238/) -- Naming, functions, and code organization principles
- [SOLID Principles](https://en.wikipedia.org/wiki/SOLID) -- Single responsibility, open-closed, Liskov, interface segregation, dependency inversion
- [Refactoring — Martin Fowler](https://refactoring.com/) -- Code smells and refactoring techniques
