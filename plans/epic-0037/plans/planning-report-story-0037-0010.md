# Story Planning Report — story-0037-0010

| Story ID | story-0037-0010 | Epic ID | 0037 | Date | 2026-04-13 |
| Agents | Architect, QA, Security, TechLead, PO |

## Planning Summary
**Epic-closing sync barrier**. Depends on stories 0001-0007 + 0009 (NOT 0008, blocked by EPIC-0035). Verification + regen + smoke + CHANGELOG. No new SoT content. 5 Gherkin scenarios. 7 consolidated tasks.

## Architecture
17+ profile golden regen across 6 modified skills + 1 new rule file. Canonical sequence: `mvn process-resources` → `GoldenFileRegenerator` → `mvn test` (per memory `reference_golden_regen_command`). `expected-artifacts.json` schema update for Rule 14.

## Test Strategy
5 ATs (regen / expected-artifacts / smoke epic parallel / success criteria / CHANGELOG). Critical: smoke epic parallel must be reproducible (ephemeral test epic with 2 stories no deps; cleanup post-test mandatory). 4 success-criteria deterministic grep/ls checks.

## Security
- Validation grep commands must be deterministic in CI (no false negatives)
- Smoke fixture must clean up worktrees post-test (no `.claude/worktrees/` leakage)
- No new attack surface; verification-only changes

## Implementation Approach
TechLead: this story IS the epic completion gate. All upstream stories MUST be merged first (TASK-001 verifies). Atomic commits per concern. PR body includes complete success-criteria checklist, smoke evidence, STORY 8 BLOCKED note, rollback procedure (PO addition).

## Risk Matrix
| Risk | Sev | Likely | Mitigation |
|------|-----|--------|-----------|
| Upstream story not merged → regen produces wrong baseline | Critical | Low | TASK-001 mandatory preflight check |
| Regen produces noisy/spurious diffs | Medium | Medium | `git diff --stat` review; rollback path documented in PR |
| Smoke leaves worktree leak in `.claude/worktrees/` | Medium | Low | Security DoD: explicit cleanup verification post-test |
| STORY 8 accidentally pulled in despite BLOCKED status | Low | Low | TASK-001 verifies STORY 8 status snapshot |
| Grep checks flaky (false positives or negatives) | Low | Low | Run twice; verify identical output |
| CHANGELOG entry omits a deliverable | Low | Medium | TASK-006 DoD explicitly enumerates: 6 skills + Rule 14 + ADR-0004 + Operation 5 + deprecation |

## DoR Status
**READY** — 10/10 mandatory pass. Note: this story cannot START until upstream stories are READY in this planning run AND merged in develop at execution time. See `dor-story-0037-0010.md`.
