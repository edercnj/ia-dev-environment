---
name: story-planning
description: >
  Knowledge pack on story decomposition and planning: layer-by-layer decomposition (foundation,
  core domain, extensions, compositions, cross-cutting), story self-containment (data contracts,
  acceptance criteria), dependency DAG, sizing rules, and phase computation. Use this when the
  user asks for guidance on story planning, backlog decomposition, story sizing, or dependency
  management between stories.
---

# Knowledge Pack: Story Planning

## Purpose

Provides story decomposition and planning patterns for translating system specifications
into independently implementable work items. Enables layered decomposition, dependency
management, sizing consistency, and phased delivery planning.

## Quick Reference

See the reference guides for the essential story decomposition summary:
5 layers (foundation, core domain, extensions, compositions, cross-cutting), self-containment
rules, dependency DAG, and sizing.

## Detailed References

| Reference | Content |
|-----------|---------|
| Layered Decomposition | 5 layers (Layer 0: foundation infrastructure, Layer 1: core domain pattern establishment, Layer 2: extensions reusing Layer 1, Layer 3: compositions combining multiple capabilities, Layer 4: cross-cutting quality/observability) |
| Story Self-Containment | Data contracts (all fields with type, mandatory/optional, derivation rules), Gherkin acceptance criteria (concrete values), Mermaid sequence diagrams (real component names), sub-tasks (estimable at 2-4 hours, tagged [Dev]/[Test]/[Doc]) |
| Dependency DAG | Directed acyclic graph construction, circular dependency detection, dependency types (structural, data, pattern), bidirectional consistency |
| Cross-Cutting Rules | Rule extraction patterns, unique sequential IDs (RULE-001), implementation-ready descriptions |
| Story Sizing | Metrics (endpoints per story: max 2, protocol flows: max 1, Gherkin scenarios: 2-8, sub-tasks: max 10) |
| Phase Computation | Automatic phase derivation from DAG, critical path identification, phase parallelization |

## Full References

For in-depth guidance on each topic, see:
- `.github/skills/story-planning/SKILL.md`
- `.github/skills/x-story-epic-full/references/decomposition-guide.md`
