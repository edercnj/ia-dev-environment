# Story Planning Report — story-0037-0010

| Field | Value |
|-------|-------|
| Story ID | story-0037-0010 |
| Epic ID | 0037 |
| Date | 2026-04-13 |

## Planning Summary

Sync-barrier closeout story. Regenerates golden files for all modified skills (6) + new rule file (14-worktree-lifecycle.md), updates `expected-artifacts.json`, runs end-to-end smoke with 2-story parallel epic, verifies success criteria (zero `isolation.*worktree` hits, zero stale RULE-018 references), and updates CHANGELOG.

## Architecture Assessment

No code. Executes canonical regen sequence (see memory `reference_golden_regen_command`): `mvn compile test-compile` → `java -cp target/test-classes:target/classes dev.iadev.golden.GoldenFileRegenerator` → `mvn test`. Memory `feedback_mvn_process_resources_before_regen` reminds operator that `mvn process-resources` is required when running regenerator standalone.

## Test Strategy Summary

Verification-driven: smoke epic with 2 parallel stories end-to-end. Assertions on `.claude/worktrees/` state during and after execution. Success-criterion greps confirm epic-level invariants.

## Security Assessment Summary

- Smoke runs create real worktrees on disk. Test fixture uses ephemeral epic to avoid polluting production artifacts.
- No new input surfaces. Risk level: **LOW**.

## Implementation Approach

Strict sequential: regen → expected-artifacts update → smoke → success-criteria verifications → CHANGELOG + PR.

## Risk Matrix

| Risk | Severity | Likelihood | Mitigation |
|------|----------|------------|------------|
| Golden regen conflicts with parallel epics in flight | Medium | Medium | Coordinate merge timing with other open epics (noted in map Section 6.4) |
| Smoke epic fails revealing a hidden regression | HIGH | Low | Stories 1-7 carry their own smoke ACs; this story is the integration check |
| `expected-artifacts.json` schema change breaks other tests | Medium | Low | Only additive change (new rule file entry) |

## DoR Status

**READY** — see `dor-story-0037-0010.md`.
