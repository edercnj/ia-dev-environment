# architecture-hexagonal

> Hexagonal architecture reference: canonical package structure, dependency rules with violation examples, compilable Port/Adapter patterns, and ArchUnit boundary validation suite.

| | |
|---|---|
| **Category** | Knowledge Pack |
| **Referenced by** | x-task-implement, x-story-implement, x-arch-plan, x-code-audit, x-review-pr, architect agent |
| **Condition** | Included when `architecture.style` is `hexagonal` |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete reference.

## Topics Covered

- Canonical package structure with layer responsibilities
- Dependency rules with concrete violation examples and expected ArchUnit errors
- Compilable Port/Adapter code examples (inbound port, outbound port, use case, adapter)
- ArchUnit boundary validation test suite for CI enforcement

## Key Concepts

This pack specializes the hexagonal architecture with a detailed package structure separating domain (model, port, engine), application (use case), adapter (inbound/outbound), and config layers. Each layer has explicit dependency rules enforced through ArchUnit tests that detect violations such as domain importing adapter classes or framework annotations. Compilable code examples demonstrate the full Port/Adapter pattern from driving ports through use case orchestration to driven port implementations. The ArchUnit test suite provides automated CI validation of architectural boundaries.

## See Also

- [architecture](../architecture/) — Generic architecture principles and dependency matrix
- [coding-standards](../coding-standards/) — Language-specific coding conventions
- [layer-templates](../layer-templates/) — Code templates per architecture layer
