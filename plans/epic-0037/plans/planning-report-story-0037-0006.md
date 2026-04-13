# Story Planning Report — story-0037-0006

| Story ID | story-0037-0006 | Epic ID | 0037 | Date | 2026-04-13 |
| Agents | Architect, QA, Security, TechLead, PO |

## Planning Summary
Doc-only opt-in `--worktree` for `x-dev-implement`. Closes the LAST direct-branch-creation point in core implementation skills. Mirror of story-0037-0005 with TASK_OWNS_WORKTREE state. Critical: nested prevention when task invoked from inside story worktree (largest risk per epic risk table). Depends on stories 0002 + 0005. 6 Gherkin ATs. 7 consolidated tasks.

## Architecture
2 substeps (detect / decide+persist). 3-case parent branch resolution per story §5.3: explicit `--parent` flag → `<branch>`; detected `feature/story-XXXX-YYYY-*` → story branch; else `develop`. TASK_OWNS_WORKTREE state mirrors STORY_OWNS_WORKTREE pattern.

## Test Strategy
6 ATs (backward / standalone develop / standalone story / nested prevention / cleanup success / cleanup failure). Inner loop structural assertions on Step content + decision table cells. **Nested-prevention smoke is the critical blocker** — task invoked inside actual story worktree fixture must verify zero nested worktree created.

## Security
- TASK_ID regex `^task-\d{4}-\d{4}-\d{3}$` validation BEFORE shell interpolation (CWE-78)
- Parent branch from `git branch --show-current` treated as untrusted; same validation; quote all expansions
- `--parent <missing>` validated via `git rev-parse --verify` before worktree create (fail-fast)
- CWE-209: scrub paths from PRESERVED log (task ID only)

## Implementation Approach
TechLead: opt-in default (RULE-004), atomic commits, nested-prevention mandatory blocker, parent resolution deterministic. PO added: `--parent <missing>` error path, TASK_OWNS_WORKTREE state wording standardized.

## Risk Matrix
| Risk | Sev | Likely | Mitigation |
|------|-----|--------|-----------|
| Task creates nested worktree inside story | **Critical** | Medium (largest epic risk) | TASK-006 nested-prevention smoke is mandatory blocker |
| Parent branch detection wrong (uses develop when should use story) | Medium | Low | TASK-002 deterministic 3-case decision tree; smoke covers all |
| `--parent <missing>` silent failure | High | Low | TASK-004 explicit `git rev-parse --verify` check |
| TASK_ID injection | Medium | Low | TASK-004 regex validation |
| RULE-004 violation | Critical | Low | TASK-006 backward-compat AT |

## DoR Status
**READY** — 10/10 mandatory pass. See `dor-story-0037-0006.md`.
