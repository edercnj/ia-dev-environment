# Story Planning Report — story-0037-0001

## Header

| Field | Value |
|-------|-------|
| Story ID | story-0037-0001 |
| Epic ID | 0037 |
| Date | 2026-04-13 |
| Agents Participating | Architect, QA Engineer, Security Engineer, Tech Lead, Product Owner |

## Planning Summary

Foundation story. Promotes RULE-018 to a first-class rule file `14-worktree-lifecycle.md` and eliminates inline duplication in `x-git-worktree/SKILL.md`. Zero behavioral changes — only markdown/resources under `targets/`. Blocks all other stories in the epic.

## Architecture Assessment

- **Layers affected**: docs-only (`targets/claude/rules/`, `targets/claude/skills/core/x-git-worktree/`).
- **New components**: `14-worktree-lifecycle.md` (rule file with 7 sections).
- **Modified components**: `x-git-worktree/SKILL.md` (two edits: L49-57 replaced by pointer; L355-379 deleted).
- **Cross-references**: RULE-018 mentions throughout `targets/` updated to link new rule file.
- **Dependency direction**: N/A (docs); rule-numbering convention respected (slot 14 used; slots 10/11/12 preserved for conditional rules per memory `project_rule_numbering_reserved_slots`).

## Test Strategy Summary

5 Gherkin scenarios ordered by TPP: degenerate (rule file creation) → happy (inline replacement) → cleanup (drift deletion) → integration (cross-refs) → smoke (golden regen). Coverage by golden-file tests under `src/test/resources/golden/**`. No unit tests required (docs-only). Smoke covers `PlatformDirectorySmokeTest` + `mvn clean verify`.

## Security Assessment Summary

- OWASP Top 10 mapping: N/A (non-executable markdown).
- Rule file content itself documents security-positive anti-patterns (deprecation of `Agent(isolation:"worktree")` — relates to memory `project_agent_worktree_isolation_leak`).
- No secrets, no input parsing, no new dependencies.
- Risk level: **LOW**.

## Implementation Approach

1. Gate on DoR (TASK-000): baseline green, slot 14 available, branch created.
2. Create rule file (TASK-001) — single atomic GREEN commit.
3. Parallelizable edits to `x-git-worktree/SKILL.md` (TASK-002 replace inline, TASK-003 delete drift).
4. Sweep cross-references (TASK-004) after both edits land.
5. Golden regen (TASK-005) — must run `mvn process-resources` before `GoldenFileRegenerator` per memory `feedback_mvn_process_resources_before_regen`.
6. PR open against `develop` with `epic-0037` label (TASK-006).

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 7 |
| Architecture/doc tasks | 5 |
| Test/verification tasks | 1 |
| Security tasks | 0 (N/A for docs) |
| Quality gate tasks | 1 |
| Validation tasks | 1 (DoR gate) |
| Merged tasks | 0 |
| Augmented tasks | 0 |

## Consolidated Risk Matrix

| Risk | Source | Severity | Likelihood | Mitigation |
|------|--------|----------|------------|------------|
| Slot 14 taken by parallel work | TL | Medium | Low | TASK-000 verifies availability before TASK-001 |
| Golden regen forgets `mvn process-resources` | QA | Medium | Medium | DoD explicitly enumerates the command; memory reminds developers |
| Cross-reference sweep misses a file | ARCH | Low | Medium | TASK-004 includes `grep -rn RULE-018 targets/` as exit-criterion |
| Drift section deletion blocks STORY 3 accidentally | PO | Low | Low | Inline comment marker "to be rewritten by STORY 3" documents intent |

## DoR Status

**READY** — see `dor-story-0037-0001.md` for the full checklist.
