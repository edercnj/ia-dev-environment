# Review Remediation Tracker

> **Story ID:** story-0047-0001
> **Epic ID:** epic-0047
> **Date:** 2026-04-21
> **Total findings pending remediation:** 0 blocking / 15 deferred follow-ups (4 specialist + 4 specialist + 3 Medium Tech Lead + 4 Low Tech Lead)

## Findings Tracker

| Finding ID | Engineer | Severity | Description | Status | Fix Commit SHA |
| :--- | :--- | :--- | :--- | :--- | :--- |
| FIND-001 | QA | Low | `SkillsAssembler` class-level branch coverage 82.1% vs. story DoD Global §4 stricter bar of >= 90% (Rule 05's 80% floor is met). Gap driven by pre-existing `pruneStaleSkills` branches; new `assembleShared` code is fully covered. | Deferred | — |
| FIND-002 | QA | Low | Story §7 Cenario 2 ("snippet ausente falha assembly cedo") superseded by ADR-0011 Option (b) but not annotated in the Gherkin block. | Deferred (doc-only) | — |
| FIND-003 | Performance | Low | No automated assertion enforcing the ">10% assembly-time regression" gate declared in story DoD Global §4. Relies on human observation of CI wall-clock. | Deferred | — |
| FIND-004 | Performance | Low | 17 per-profile serial assembler runs in `Epic0047CompressionSmokeTest`; each adds one `_shared/` directory copy. Immeasurable at current profile count; revisit if profile count doubles. | Deferred | — |
| FIND-005 | Security | Low | `CopyHelpers.copyDirectory` does not pass `NOFOLLOW_LINKS`; Rule 06 forbids "following symlinks without explicit opt-in". Pre-existing behavior, not introduced by this story. Threat model: low (developer-controlled build tree). | Deferred (pre-existing, project-wide) | — |
| FIND-006 | Security | Low | No CI lint scanning `_shared/*.md` for accidentally committed secrets / credentials. Today the snippets are pure docs; a future author could regress. | Deferred (follow-up epic) | — |
| FIND-007 | DevOps | Low | Branch base is `6b2470212` (pre-chore PR #534); current develop is `83f637f59` (post-chore). GitHub reports MERGEABLE; squash-merge flattens this — non-blocking cosmetic. | Deferred (accepted by squash policy) | — |
| FIND-008 | DevOps | Low | No written rollback playbook for `_shared/` regeneration in PR description. Standard `git revert` of the 8 commits suffices. | Deferred (follow-up, epic-level playbook) | — |
| FIND-009 | Tech Lead | Medium | `SkillsAssembler.java` grew from 348 → 396 lines, aggravating a pre-existing violation of the 250-line class-size hard limit (Rule 03). This PR adds ~48 lines thematically correct as a peer of existing `assembleXxx` orchestrators. | Deferred — follow-up: extract the four `assembleXxx` into `SkillsAssemblerPipeline`. Track under code-health story in next epic. | — |
| FIND-010 | Tech Lead | Medium | `adr/ADR-0011-shared-snippets-inclusion-strategy.md` §Consequences / Neutral claims the assembler "does NOT copy `_shared/` to the output" — contradicts the delivered implementation and its smoke tests. Reader of the ADR would be misled. | Deferred (but strongly recommended) — post-merge doc fix. Draft replacement text included in the Tech Lead report under Medium #2. | — |
| FIND-011 | Tech Lead | Medium | Story §7 Cenario 2 ("snippet ausente em _shared/ falha o assembly cedo") no longer achievable under Option (b). DoD has it checked but semantics supersede it. | Deferred (doc-only) — add supersession note per Tech Lead report Medium #3. Can ride any follow-up story PR in epic-0047. | — |
| FIND-012 | Tech Lead | Low | Project-wide coverage 94.83/89.54 is 0.17/0.46 pts shy of aspirational 95/90 target in story DoD Global §4. CI-enforced floor 85/80 comfortably met. `SkillsAssembler` specifically meets 95% line (96.6%). | Deferred — no action. Observation for audit clarity only. | — |
| FIND-013 | Tech Lead | Low | `sharedPath()` / `corePath()` / `conditionalPath()` are near-identical 4-line helpers differing only by directory-name constant — minor DRY smell. | Deferred — leave as-is unless a fourth category appears. | — |
| FIND-014 | Tech Lead | Low | No explicit "refactor" commit between RED and GREEN for TASK-003. Refactor step implicit in the compact GREEN commit. | Deferred — acceptable for single-method addition. For larger additions, prefer explicit `refactor:` commit after GREEN. | — |
| FIND-015 | Tech Lead | Low | `plans/epic-0051/telemetry/events.ndjson` showed uncommitted modification in the initial `git status` on the repo root worktree. Not part of this PR (different worktree). | Accepted — feat branch worktree is clean; no action on this PR. | — |

## Remediation Summary

| Status | Count |
| :--- | :--- |
| Open | 0 |
| Fixed | 0 |
| Deferred | 14 |
| Accepted | 1 |
| **Total** | **15** |

## Merge Blockers

**None.** No Critical, no High. Three Medium findings from the Tech Lead review (FIND-009 class-size pre-existing, FIND-010 ADR self-contradiction, FIND-011 Cenario 2 supersession) are all doc / code-health follow-ups — none blocks the mechanical correctness of the PR or the CI gate. Twelve Low findings spread across specialists + Tech Lead are pre-existing concerns, measurement gaps, or aspirational-target variances. All documented for audit trail.

## Next Actions

1. Merge PR #535 to `develop` (squash-merge per Rule 09). No code changes required.
2. File follow-up tasks (not blocking this PR):
   - **Hardening:** Pass `NOFOLLOW_LINKS` in `CopyHelpers.copyDirectory` (FIND-005). Touches every assembler; file as an independent story under a future hardening epic.
   - **Security CI:** Add gitleaks/trufflehog Markdown-aware scan (FIND-006). Coordinate with x-security-dashboard owner.
   - **Perf telemetry:** Extend `x-telemetry-trend` to emit full-verify wall-clock and flag >10% regressions (FIND-003). Under EPIC-0040 follow-up.
   - **Doc nit:** Annotate story §7 Cenario 2 as superseded by ADR-0011 Option (b) (FIND-002). Non-blocking; can ride any subsequent story-0047-* PR.
