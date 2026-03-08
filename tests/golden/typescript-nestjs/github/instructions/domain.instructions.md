# Domain Rules

> For the full domain reference template with examples, see
> [`.claude/rules/02-domain.md`](../../.claude/rules/02-domain.md).

This file captures domain-specific knowledge that must be understood to produce
correct code. It is about the **business domain**, not coding patterns.

## Domain Overview

Define in 3-5 sentences: the problem being solved, the role of this system in the
larger ecosystem, and who the primary users/consumers are.

## System Role

- **Receives:** (define what the system receives)
- **Processes:** (define what the system processes)
- **Returns:** (define what the system returns)
- **Persists:** (define what the system persists)

## Domain Model

### Core Entities

Define entities with their key attributes. Use the ubiquitous language — every
developer must use these exact terms.

| Entity | Description | Key Attributes |
|--------|-------------|----------------|
| (Define entities here) | | |

### Value Objects

Immutable objects defined by their attributes, not identity.
Examples: `Money(amount, currency)`, `Address(street, city, zip)`, `DateRange(start, end)`.

### Aggregates and Boundaries

Define aggregate roots and their boundaries. Which entities are always loaded/saved
together?

## Business Rules

Each rule should have: an ID, a description, and the decision logic. Rules must be
testable and deterministic.

## Domain States and Transitions

Define the state machine for key entities. What states can they be in? What events
trigger transitions?

## Sensitive Data

| Data | Classification | Can Log? | Can Persist? | Can Return in API? |
|------|---------------|----------|--------------|-------------------|
| (Define per domain) | | | | |

## Glossary

| Term | Definition |
|------|-----------|
| (Define domain-specific terms) | |
