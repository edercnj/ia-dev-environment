# Story Planning Report — story-0037-0003

## Header

| Field | Value |
|-------|-------|
| Story ID | story-0037-0003 |
| Epic ID | 0037 |
| Date | 2026-04-13 |
| Agents | Architect, QA, Security, Tech Lead, PO |

## Planning Summary

**Critical behavioral migration.** The only story in EPIC-0037 that changes runtime behavior: replaces `Agent(isolation:"worktree")` with explicit `/x-git-worktree create|remove` calls in `x-epic-implement`. Highest-risk story; manual smoke with 2 parallel stories is a mandatory AC. Aligns with memory `project_agent_worktree_isolation_leak` (prior incident with harness-native isolation).

## Architecture Assessment

- Modifies `x-epic-implement/SKILL.md` sections 1.4a, 1.4d, 1.6 (three cohesive areas).
- Rewrites `x-git-worktree/SKILL.md` "Integration with Epic Execution" (deleted by story-0001, rewritten here with accurate diagram).
- New execution-state schema field: `worktreePath` per story.
- Error codes added: `WT_CREATE_FAILED`, `WT_REMOVE_FAILED`, `WT_PRESERVED`.
- Dispatch flow: 4 explicit phases (pre-dispatch create, dispatch without isolation, post-success remove, post-failure preserve).

## Test Strategy Summary

5 Gherkin scenarios in TPP order: happy (2 SUCCESS) → mixed (SUCCESS+FAILED preserved) → error (create fails) → smoke (state file) → boundary (zero parallel). PO adds 2 edge scenarios. Structural Java test asserts absence of `isolation.*worktree` literal. **Manual smoke with 2 parallel stories is the canonical gate.**

## Security Assessment Summary

- Migration itself is security-positive: removes "magic" harness behavior; makes lifecycle visible/operable.
- Preserved worktrees of failed stories contain partial source trees; operator inspects under same trust boundary.
- OWASP Top 10: N/A (orchestration behavior change; no new input surfaces).
- Risk level: **LOW** for security, **HIGH** for regression (see risk matrix).

## Implementation Approach

Four docs tasks (1.4a refactor → 1.4d cleanup → 1.6 schema → integration rewrite) land first, then structural test + grep verification, then manual smoke (mandatory gate), then golden regen + PR. PO gherkin amendments run in parallel early.

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 10 |
| Doc tasks | 4 |
| Test/verification tasks | 3 (incl. mandatory smoke) |
| Validation/PO tasks | 1 |
| Quality gate | 1 |

## Consolidated Risk Matrix

| Risk | Source | Severity | Likelihood | Mitigation |
|------|--------|----------|------------|------------|
| Migration breaks parallel dispatch | ARCH | **CRITICAL** | Medium | Mandatory 2-story smoke as AC; rollback via PR revert |
| Worktree cleanup races with auto-rebase | TL | High | Medium | Cleanup sequenced AFTER merge+auto-rebase; documented in §1.4d |
| `--resume` replay misbehaves with worktrees | PO | Medium | Medium | PO gherkin amendment covers replay; documented in §1.6 |
| Structural test false negatives | QA | Low | Low | Test includes both absence-of-harness check and presence-of-phases |
| State file grows unbounded with worktreePath | TL | Low | Low | Paths are short; no new growth vector |

## DoR Status

**READY** — see `dor-story-0037-0003.md`.
