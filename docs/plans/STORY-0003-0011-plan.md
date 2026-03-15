# Implementation Plan — STORY-0003-0011

## Story

x-story-epic-full — Propagação de Mudanças TDD

## Affected Files

### Source Templates (modified)

1. `resources/skills-templates/core/x-story-epic-full/SKILL.md` — .claude copy
2. `resources/github-skills-templates/story/x-story-epic-full.md` — .github copy

### Golden Files (regenerated)

8 profiles × 3 locations (.claude, .github, .agents) = 24 golden files

## Changes

### Phase B Description Update

Add to Phase B that the generated epic now includes TDD Compliance and Double-Loop TDD in its DoD (aligning with x-story-epic changes from story-0003-0010).

### Phase C Description Update

Update Phase C to reflect that Gherkin acceptance criteria now include mandatory categories (degenerate, happy path, error paths, boundary values) ordered by TPP (aligning with x-story-create changes from story-0003-0009).

### Quality Checklist Update

Add 4 TDD items:
- `[ ] Each story has at least 4 Gherkin scenarios with mandatory categories (degenerate, happy path, error paths, boundary values)`
- `[ ] Gherkin scenarios are ordered by TPP (degenerate → edge cases)`
- `[ ] Epic DoD includes TDD Compliance and Double-Loop TDD`
- `[ ] Boundary values use triplet pattern (at-min, at-max, past-max)`

## Dependency Direction Validation

N/A — template/documentation changes only, no code layer changes.

## Risk Assessment

- **Low risk**: Additive changes only, no content removed
- **Backward compatible**: Existing workflow phases preserved, TDD items are additions
- **Dual copy**: RULE-001 compliance via updating both source templates + regenerating golden files
