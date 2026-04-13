# Story Planning Report — story-0037-0003

| Field | Value |
|-------|-------|
| Story ID | story-0037-0003 |
| Epic ID | 0037 |
| Date | 2026-04-13 |
| Agents Participating | Architect, QA, Security, TechLead, Product Owner |

## Planning Summary

**HIGHEST-RISK story of EPIC-0037**: only behavior-changing migration. Replaces `Agent(isolation:"worktree")` (harness-native) with explicit `/x-git-worktree create|remove` calls in `x-dev-epic-implement`. Doc-only edits (markdown describes orchestrator behavior). Depends on stories 0001 (Rule 14) and 0002 (detect-context). Forward-references ADR-0004 (story-0037-0009). Consolidated to 10 atomic tasks.

## Architecture Assessment

§1.4a refactored into 4 explicit phases: A (pre-dispatch worktree creation per parallel story), B (dispatch without `isolation` flag, subagent prompts include `cd <wt>` Step 0), C (post-success cleanup via `/x-git-worktree remove`), D (post-failure preservation with `WT_PRESERVED` log). §1.4d explicit cleanup matrix (SUCCESS+merged → remove; FAILED → preserve; epic-end → defensive `--dry-run` listing only). §1.6 schema adds `worktreePath` per story. x-git-worktree integration section rewritten with accurate ASCII sequence diagram.

## Test Strategy Summary

5 ATs from Gherkin (happy / mixed / error / smoke / boundary) + 2 PO-amendment scenarios (PR rejected; epic --resume restores worktreePath). Inner loop: structural assertions on SKILL.md content (4 phases present, schema field documented, Mermaid renders, defensive cleanup paragraph). New Java test `EpicImplementWorktreeMigrationTest` with RED commit (failing on current SKILL.md) → GREEN post-edits. Manual smoke (TASK-008) is mandatory blocker — ephemeral 2-story fixture verifies create/remove/preserve flow end-to-end.

## Security Assessment Summary

- **CWE-22 path traversal** via crafted `${slug}` interpolation: sanitize to `[a-z0-9-]+` regex before passing to `/x-git-worktree create`
- **CWE-209 information exposure**: absolute worktree paths in execution-state.json may leak in CI logs — note required, no auto-fix
- **OWASP A05 misconfig**: defensive `--dry-run` cleanup MUST report only, never auto-delete (operator decision)
- **Command injection**: branch name interpolation via `bash -c` is risky; recommend using array-based exec instead

## Implementation Approach

TechLead approves story decomposition with refinements: split each section edit into atomic commit (one per task); add explicit verification grep (TASK-006); promote manual smoke to blocker (TASK-008); use placeholder forward-reference for ADR-0004 if story-0037-0009 not yet merged. Quality gates: SoT compliance, Mermaid renders, smoke evidence in PR body, mvn verify green.

## Task Breakdown Summary

| Metric | Value |
|--------|-------|
| Total tasks | 10 |
| Architecture tasks | 4 (TASK-001..004) |
| Test tasks | 2 (TASK-005, TASK-006) + manual smoke (TASK-008) |
| Security tasks | augmented into TASK-001 + TASK-002 + TASK-004 |
| Quality gate tasks | TASK-009, TASK-010 |
| Validation tasks | TASK-007 |
| Merged tasks | 4 |
| Augmented tasks | 3 (security DoD injected) |

## Consolidated Risk Matrix

| Risk | Source | Severity | Likelihood | Mitigation |
|------|--------|----------|------------|------------|
| Migration breaks parallel epic dispatch | Architect | Critical | Medium | TASK-008 mandatory blocker; rollback plan via PR revert |
| `${slug}` injection if branch name crafted | Security | High | Low | TASK-001 sanitization regex `[a-z0-9-]+`; reject anything else |
| ADR-0004 forward-ref blocks merge if 0009 delayed | TL | Medium | Medium | Placeholder TODO accepted; update post-merge |
| Defensive cleanup auto-deletes worktrees | Security | High | Low | TASK-002 explicit "dry-run reports only" requirement |
| `worktreePath` absolute paths leak in CI | Security | Low | Medium | Documentation note in §1.6 |
| Subagent prompts forget `cd` Step 0 | QA | High | Low | TASK-001 DoD includes explicit `cd` requirement; TASK-005 verifies |

## DoR Status

**READY** — 10/10 mandatory pass; conditional N/A. See `dor-story-0037-0003.md`.
