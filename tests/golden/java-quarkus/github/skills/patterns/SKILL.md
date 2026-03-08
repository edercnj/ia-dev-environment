---
name: patterns
description: >
  Knowledge Pack: Design Patterns -- CQRS, command/query separation, event
  sourcing patterns, repository pattern, factory pattern, strategy pattern,
  and domain-driven design patterns for my-quarkus-service.
---

# Knowledge Pack: Design Patterns

## Summary

Design patterns and CQRS conventions for my-quarkus-service using java 21.

### CQRS (Command Query Responsibility Segregation)

- **Commands**: Modify state, return void (fire-and-forget or async acknowledgment)
- **Queries**: Read state, return data, no side effects
- Separate command and query models when read/write patterns diverge
- Command handlers validate, execute business rules, persist state
- Query handlers optimize for read performance (projections, denormalized views)

### Core Patterns

| Pattern | When to Use |
|---------|------------|
| Repository | Abstract data access behind domain-typed interface |
| Factory | Complex object creation with validation |
| Strategy | Interchangeable algorithms at runtime |
| Specification | Composable business rules for filtering/validation |
| Domain Event | Decouple side effects from command execution |
| Value Object | Immutable, identity-less domain concepts |

### CQS at Method Level

- Methods that change state return `void`
- Methods that return data do not change state
- Exceptions: stack `pop()`, queue `dequeue()` (document clearly)

### Event Sourcing (When Applicable)

- Store events as source of truth, derive current state by replaying
- Events are immutable, append-only, past tense named
- Snapshots for performance optimization on large event streams

## References

- `.claude/skills/patterns/SKILL.md` -- Full CQRS and patterns reference
- `.claude/skills/patterns/references/` -- Detailed documentation
