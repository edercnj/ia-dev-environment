# Epic Execution Plan -- EPIC-0054

> **Epic ID:** EPIC-0054
> **Title:** Rollout ADR-0012 — Slim-by-default para 8 orchestrators
> **Date:** 2026-04-23
> **Total Stories:** 4
> **Total Phases:** 2
> **Author:** x-internal-epic-build-plan (orchestrator)
> **Template Version:** 2.0

---

## Execution Strategy

| Attribute | Value |
|-----------|-------|
| Strategy | Sequential |
| Max Parallelism | 1 |
| Checkpoint Frequency | Per story |
| Dry Run | false |

Sequential execution: stories within each phase run one after another. Phase 0 contains 3 independent stories (0001, 0002, 0003). Phase 1 contains 1 story (0004) blocked by 0003.

---

## Phase Timeline

| Phase | Name | Stories | Parallelism | Estimated Duration | Dependencies |
|-------|------|---------|-------------|-------------------|--------------|
| 0 | Rollout tier 1-2 | story-0054-0001, story-0054-0002, story-0054-0003 | Sequential (3) | ~2–3 days | None |
| 1 | Rollout tier XL | story-0054-0004 | Sequential (1) | ~1–2 days | Phase 0 complete (story-0054-0003 must merge first — RULE-054-07) |

> **Total estimated duration:** ~3–5 days wall clock

---

## Story Execution Order

| Order | Story ID | Title | Phase | Dependencies | Critical Path | Estimated Effort |
|-------|----------|-------|-------|--------------|---------------|-----------------|
| 1 | story-0054-0001 | Slim rewrite — PR-domain (x-pr-fix-epic + x-pr-merge-train) | 0 | None | No | M |
| 2 | story-0054-0002 | Slim rewrite — Medium orchestrators (x-task-implement + x-security-pipeline + x-git-worktree) | 0 | None | No | M |
| 3 | story-0054-0003 | Slim rewrite — x-story-plan (com partial-carve existente) | 0 | None | **Yes** | M |
| 4 | story-0054-0004 | Slim rewrite — High-impact orchestrators (x-epic-implement + x-release) | 1 | story-0054-0003 | **Yes** | XL |

> **Critical Path Legend:** `Yes` = story is on the critical path (delay impacts epic deadline); `No` = story has slack.
> **Estimated Effort:** `S` (small), `M` (medium), `L` (large), `XL` (extra-large).

---

## Pre-flight Analysis Summary

| Check | Status | Details |
|-------|--------|---------|
| Story files present | ✅ PASS | 4 story files found under plans/epic-0054/ |
| Dependencies resolved | ✅ PASS | story-0054-0003 → story-0054-0004 (single hard dependency) |
| Circular dependencies | ✅ PASS | No cycles detected (Kahn's algorithm: DAG valid) |
| Implementation map valid | ✅ PASS | IMPLEMENTATION-MAP.md parsed; all references cross-validated |

No pre-flight warnings. Soft hotspots: `audits/skill-size-baseline.txt` and `CHANGELOG.md` shared across Phase 0 stories — RULE-054 treats these as append-ordered (no serial demotion required per IMPLEMENTATION-MAP §5).

---

## Resource Requirements

| Resource | Estimate | Notes |
|----------|----------|-------|
| Estimated tokens | ~800K | 4 stories × ~200K avg (heavy SKILL.md rewrite work) |
| Estimated wall time | 3–5 days | Sequential; gated at Phase 0 → Phase 1 by story-0054-0003 merge |
| Max parallel subagents | 1 | Sequential mode; no worktrees required |
| Peak memory estimate | ~50MB | All-markdown edits; no Java compilation required |

---

## Risk Assessment

| Risk | Severity | Likelihood | Mitigation |
|------|----------|------------|------------|
| x-release partial-carve audit reveals unexpected cross-links | High | Possible | story-0054-0004 TASK-001 audits references/ before carve begins |
| Golden byte-parity regression on SKILL.md regen | High | Likely | `mvn process-resources` + GoldenFileRegenerator mandatory before each commit |
| story-0054-0003 delays unblock of story-0054-0004 | Medium | Possible | story-0054-0003 is the critical path gatekeeper; prioritize its review + merge |
| Rule 13 audit grep returns matches in slim body | High | Possible | Run `grep -rnE "^\s*/x-[a-z-]+\s"` in slim bodies; must return 0 matches |
| Baseline entry removal causes SkillSizeLinter regression | Medium | Unlikely | Remove baseline entry AFTER confirming SKILL.md ≤ 250 lines in golden |

> **Severity levels:** Critical, High, Medium, Low.
> **Likelihood levels:** Very Likely, Likely, Possible, Unlikely.

---

## Checkpoint Strategy

| Parameter | Value |
|-----------|-------|
| Checkpoint frequency | Per story completion |
| Save on phase completion | Yes |
| Save on story completion | Yes |
| Save on integrity gate failure | Yes |
| State file location | plans/epic-0054/execution-state.json |

### Recovery Procedures

On any story failure: inspect the failed story's PR diff, address the failure, push fix commits to the same branch, and re-run `x-story-implement story-0054-YYYY --resume`. Do NOT skip story-0054-0003 even if 0001/0002 complete — it is the RULE-054-07 gate for story-0054-0004.

### Resume Behavior

Re-run `/x-epic-implement epic-0054 --resume` to continue from the last successful checkpoint. Stories in `COMPLETE` status are skipped. `PENDING` and `BLOCKED→unblocked` stories re-enter the loop. Phase 1 waits for story-0054-0003 `status=COMPLETE` and `prMergeStatus=MERGED` before dispatching story-0054-0004.
