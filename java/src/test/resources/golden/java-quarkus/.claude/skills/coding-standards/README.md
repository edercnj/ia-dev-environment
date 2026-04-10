# coding-standards

> Complete coding conventions: Clean Code rules (CC-01 to CC-10), SOLID principles, language-specific idioms, naming patterns, constructor injection, mapper conventions, version-specific features, and approved libraries.

| | |
|---|---|
| **Category** | Knowledge Pack |
| **Referenced by** | x-dev-implement, x-dev-story-implement, x-review, x-review-pr, x-code-audit, tech-lead agent, typescript-developer agent |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete reference.

## Topics Covered

- Clean Code rules CC-01 through CC-10: naming, functions, SRP, magic values, DRY, error handling, documentation, formatting, Law of Demeter, class organization
- SOLID principles with examples and violation detection
- Language-specific naming, injection patterns, mapper conventions, domain exceptions
- Version-specific features (records, sealed types, pattern matching)
- Mandatory, recommended, and prohibited libraries
- Testing conventions: frameworks, fixture patterns, directory structure

## Key Concepts

This pack is the definitive coding reference that must be read before writing any code. It enforces Clean Code principles through ten numbered rules covering naming clarity, function length limits, single responsibility, constant extraction, DRY compliance, and error handling with context. SOLID principles are documented with concrete violation examples for detection during review. Language-specific conventions cover constructor injection patterns, mapper patterns between architectural layers, and approved library selections. Version-specific features ensure idiomatic use of the target language version.

## See Also

- [architecture](../architecture/) — Architecture principles and package structure
- [architecture-hexagonal](../architecture-hexagonal/) — Hexagonal-specific Port/Adapter patterns
- [layer-templates](../layer-templates/) — Code templates per architecture layer
