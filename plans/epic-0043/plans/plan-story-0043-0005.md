# Implementation Plan — story-0043-0005

## Story: Retrofit `x-review-pr` Exhausted-Retry Gate

**Date:** 2026-04-19
**Story ID:** story-0043-0005
**Epic:** EPIC-0043

## 1. Affected Layers and Components

- `java/src/main/resources/targets/claude/skills/core/review/x-review-pr/SKILL.md` — primary
- `.claude/skills/x-review-pr/SKILL.md` — golden (regenerated in TASK-003)

## 2. Change Summary

### 2.1 TASK-0043-0005-001: Add exhausted-retry gate to Step 8

**Insertion point:** End of `### Step 8 — Handle NO-GO (Auto-Remediation — EPIC-0042)` section, after the current `"halt with final report"` sentence (line ~286).

**Changes:**
1. Update `allowed-tools` frontmatter: add `AskUserQuestion`, `Skill` (Rule 20 requirement)
2. Add sub-section `#### Step 8.4 — Exhausted-Retry Gate` with:
   - `--non-interactive` bypass: emit legacy text + return NO-GO (backward compat)
   - `AskUserQuestion` with exactly 3 canonical options per Rule 20:
     - PROCEED: "Continue (Recommended)" / "Re-dispatch auto-remediation (+2 loops)."
     - FIX-PR: "Fix PR" / "Run x-pr-fix and retry"
     - ABORT: "Abort" / "Terminates the skill with REVIEW_REMEDIATION_EXHAUSTED."
   - Loop-back handler for PROCEED (re-dispatches 2 more remediation cycles, guard rail at 3 total)
   - INLINE-SKILL handler for FIX-PR: `Skill(skill: "x-pr-fix", args: "<PR>")`
   - Guard-rail: 3 consecutive PROCEED or FIX-PR without convergence → `REVIEW_FIX_LOOP_EXCEEDED`
   - ABORT handler: exits with `REVIEW_REMEDIATION_EXHAUSTED`

### 2.2 TASK-0043-0005-002: Document state file + `--resume-review`

**Changes:**
1. Add new `## State File (opt-in)` section documenting:
   - Path: `plans/review/<pr>/state.json`
   - Schema per Rule 20 §5.1 (5 fields: phase, lastPhaseCompletedAt, lastGateDecision, fixAttempts, schemaVersion)
   - When written: only when operator selects FIX-PR (state persisted for resume)
   - `delegateSkill` is always `"x-pr-fix"` in this skill
2. Add `--resume-review <pr>` flag documentation
3. Add error codes table: `REVIEW_REMEDIATION_EXHAUSTED`, `REVIEW_FIX_LOOP_EXCEEDED`
4. Update `argument-hint` frontmatter to include new flags

### 2.3 TASK-0043-0005-003: Regenerate golden

- Run `mvn process-resources` then golden file regeneration
- Verify `.claude/skills/x-review-pr/SKILL.md` is byte-identical to source

## 3. Backward Compatibility

- `--non-interactive` → skips gate, returns NO-GO silently (current behavior preserved)
- State file is opt-in (only written on FIX-PR selection)
- Existing auto-remediation (2 cycles) unchanged; gate only fires when all cycles exhausted

## 4. Rule 13 Compliance

- FIX-PR uses INLINE-SKILL pattern: `Skill(skill: "x-pr-fix", args: "<PR>")`
- No bare-slash delegation
- `allowed-tools` updated to include `AskUserQuestion` + `Skill`

## 5. Rule 20 Compliance

- Exactly 3 options in AskUserQuestion
- Slot 1 = PROCEED (label invariant, description contextual)
- Slot 2 = FIX-PR (LOOP-BACK variant)
- Slot 3 = ABORT
- Guard-rail at 3 consecutive fix attempts → `REVIEW_FIX_LOOP_EXCEEDED`
