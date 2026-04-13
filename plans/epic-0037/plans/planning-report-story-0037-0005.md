# Story Planning Report — story-0037-0005

| Story ID | story-0037-0005 | Epic ID | 0037 | Date | 2026-04-13 |
| Agents | Architect, QA, Security, TechLead, PO |

## Planning Summary
Doc-only opt-in `--worktree` for `x-dev-story-implement`. Dual mode: standalone+flag creates worktree (STORY_OWNS_WORKTREE=true, removes in Phase 3); orchestrated detects existing worktree and reuses (STORY_OWNS_WORKTREE=false, never removes per RULE-003). RULE-004 backward-compat without flag. Depends on stories 0002 + 0003. 5 Gherkin ATs. 7 consolidated tasks.

## Architecture
Phase 0 Step 6 split into 6.1 detect (inline `detect_worktree_context()`), 6.2 decide (3-row decision table covering omit/standalone-create/orchestrated-reuse), 6.3 persist STORY_OWNS_WORKTREE env var (single-invocation, not file-backed). Phase 3 cleanup conditional. RULE-018 §5 xref.

## Test Strategy
5 ATs (backward / standalone wt / orchestrated reuse / failure preservation / parallel boundary). Inner loop structural assertions. **Parallel smoke is critical blocker**: 2 standalone runs concurrent must produce distinct worktrees with zero conflict. Orchestrated path verification: STORY_OWNS_WORKTREE=false ensures cleanup skipped (creator-owns).

## Security
- Validate ${STORY_ID} regex `^story-\d{4}-\d{4}$` before path interpolation
- STORY_OWNS_WORKTREE env-var only (no disk leak)
- CWE-209: scrub abs paths from user-facing errors (internal log only)
- Defensive: if STORY_OWNS_WORKTREE unset/corrupt, treat as false (never auto-remove)

## Implementation Approach
TechLead enforces opt-in default (RULE-004), atomic commits, parallel smoke as gate, RULE-003 explicit verification (orchestrated skips remove).

## Risk Matrix
| Risk | Sev | Likely | Mitigation |
|------|-----|--------|-----------|
| Cleanup removes orchestrator's worktree | Critical | Low | TASK-003 OWNS=false→skip + defensive default-false |
| Parallel runs collide on same worktree id | High | Low | Smoke verifies distinct paths; ID derives from STORY_ID |
| STORY_ID injection | Medium | Low | TASK-004 regex validation |
| RULE-004 violation (default behavior changes) | Critical | Low | TASK-005 backward-compat AT |

## DoR Status
**READY** — 10/10 mandatory pass. See `dor-story-0037-0005.md`.
