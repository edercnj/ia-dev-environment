# Pre-flight Conflict Analysis — Phase 1

## File Overlap Matrix

| Story A | Story B | Overlapping Files | Classification |
|---------|---------|-------------------|----------------|
| story-0020-002 | story-0020-003 | — | unpredictable (no plans) |
| story-0020-002 | story-0020-004 | — | unpredictable (no plans) |
| story-0020-002 | story-0020-005 | — | unpredictable (no plans) |
| story-0020-002 | story-0020-006 | — | unpredictable (no plans) |
| story-0020-003 | story-0020-004 | — | unpredictable (no plans) |
| story-0020-003 | story-0020-005 | — | unpredictable (no plans) |
| story-0020-003 | story-0020-006 | — | unpredictable (no plans) |
| story-0020-004 | story-0020-005 | — | unpredictable (no plans) |
| story-0020-004 | story-0020-006 | — | unpredictable (no plans) |
| story-0020-005 | story-0020-006 | — | unpredictable (no plans) |

## Adjusted Execution Plan

### Parallel Batch
(none — all stories classified as unpredictable)

### Sequential Queue (critical path priority)
1. story-0020-002 (Claude targets — critical path, architectural checkpoint)
2. story-0020-003 (GitHub Copilot targets)
3. story-0020-004 (Codex targets — quick win)
4. story-0020-005 (Knowledge base — most complex, coupled with 002 via CoreRulesWriter)
5. story-0020-006 (Shared templates)

## Warnings
- story-0020-002: no implementation plan found (classified as unpredictable)
- story-0020-003: no implementation plan found (classified as unpredictable)
- story-0020-004: no implementation plan found (classified as unpredictable)
- story-0020-005: no implementation plan found (classified as unpredictable)
- story-0020-006: no implementation plan found (classified as unpredictable)
- NOTE: story-0020-002 and story-0020-005 both modify CoreRulesWriter (known coupling from IMPLEMENTATION-MAP.md). Sequential execution avoids merge conflicts.
