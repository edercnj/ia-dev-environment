# Task Plan: TASK-0041-0002-001

## Header

| Field | Value |
|-------|-------|
| Task ID | TASK-0041-0002-001 |
| Story ID | story-0041-0002 |
| Epic ID | epic-0041 |
| Layer | Doc |
| Type | Verification |
| TDD Cycles | 0 (doc-only) |
| Estimated Effort | S |
| Generated | 2026-04-17 |

## Objective

Add Phase 4.5 "Compute File Footprint" to `x-task-plan/SKILL.md` so every task plan emits a structured `## File Footprint` block with `write:`, `read:`, and `regen:` sub-sections.

## Implementation Guide

### Target Class/Method

`java/src/main/resources/targets/claude/skills/core/plan/x-task-plan/SKILL.md` — authoritative source; `.claude/skills/x-task-plan/SKILL.md` is a mirrored copy (`regen:` target).

### Design Pattern

Skill documentation — Phase insertion between "Analyze Affected Files" (Phase 3) and "Generate Security Checklist" (Phase 4).

### Implementation Steps (Layer Order)

1. Insert Phase 4.5 section describing inference rules (R1-R5) and output layout
2. Inject `## File Footprint` placeholder into the plan template (before `## Definition of Done`)
3. Add `parallelism-heuristics` row under "Knowledge Pack References"
4. Mirror source to `.claude/skills/x-task-plan/SKILL.md`

## TDD Cycles

Not applicable (doc-only task).

## Affected Files

| # | Path | Action | Layer | Purpose |
|---|------|--------|-------|---------|
| 1 | `java/src/main/resources/targets/claude/skills/core/plan/x-task-plan/SKILL.md` | MODIFY | Doc | Add Phase 4.5 |
| 2 | `.claude/skills/x-task-plan/SKILL.md` | MODIFY | Doc (generated) | Mirror source |

## Security Checklist

N/A — documentation change only.

## Dependencies

| Depends On | Reason |
|------------|--------|
| story-0041-0001 | KP `parallelism-heuristics` must exist to be referenced |

## File Footprint

### write:
- `java/src/main/resources/targets/claude/skills/core/plan/x-task-plan/SKILL.md`

### regen:
- `.claude/skills/x-task-plan/SKILL.md`

## Definition of Done

- [ ] Phase 4.5 documented with inference rules
- [ ] Knowledge Pack Reference for `parallelism-heuristics` present
- [ ] Generated mirror copied to `.claude/skills/x-task-plan/SKILL.md`
