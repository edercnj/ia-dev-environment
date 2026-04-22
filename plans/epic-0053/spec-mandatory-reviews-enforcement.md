# Spec — Mandatory Reviews Enforcement in x-story-implement

## Problem Statement

The `x-story-implement` skill contains two critical review steps inside Phase 3
(Story-Level Verification):

- **Step 3.4** (line ~1061): Specialist Review via `x-review`
- **Step 3.6** (line ~1120): Tech Lead Review via `x-review-pr`

These steps are currently described without explicit "MANDATORY" markers. As a result,
subagents executing the skill silently omit them — producing stories with
`reviewsExecuted: {specialist: false, techLead: false}` even when `--skip-verification`
was NOT passed. This constitutes a protocol violation with no visible warning.

Evidence: EPIC-0042 story-0042-0001 completed with `reviewsExecuted: {specialist: false,
techLead: false}` despite `--skip-verification` not being passed. The subagent followed
the path "manually" without invoking the full review cycle.

## Root Cause

1. No global Review Policy in the skill header that states reviews are mandatory by default.
2. Step 3.4 and Step 3.6 headers have no MANDATORY gate instruction.
3. The only existing escape hatch (`--skip-verification`) skips the ENTIRE Phase 3,
   not just the reviews — leading subagents to use it (or nothing) as a blunt instrument.

## Scope

**Affected file (source):**
`java/src/main/resources/targets/claude/skills/core/dev/x-story-implement/SKILL.md`

**Regenerated output:**
`.claude/skills/x-story-implement/SKILL.md`

**Golden file:**
`src/test/resources/golden/` (updated via `mvn process-resources`)

## Required Changes

### Change 1 — Global Review Policy section

Add a `## Review Policy` section immediately after `## When to Use`:

```
## Review Policy

> **MANDATORY:** Specialist Review (Step 3.4) and Tech Lead Review (Step 3.6) are
> NON-NEGOTIABLE by default. They MUST execute on every story unless the caller
> explicitly passes `--skip-verification`. Omitting either review without the flag
> is a protocol violation. Subagents MUST NOT silently skip these steps — if unable
> to execute, they MUST abort with an explicit error log `"REVIEW_SKIPPED_WITHOUT_FLAG"`.
```

### Change 2 — Step 3.4 MANDATORY marker

Add immediately after `### Step 3.4 -- Review (invoke x-review via Skill tool)`:

```
> **MANDATORY — NON-NEGOTIABLE:** This step MUST execute unless `--skip-verification`
> is explicitly present. A subagent that reaches this point without executing
> `Skill(skill: "x-review", ...)` MUST abort and emit
> `"PROTOCOL_VIOLATION: Step 3.4 skipped without --skip-verification"`.
```

### Change 3 — Step 3.6 MANDATORY marker

Add immediately after `### Step 3.6 -- Tech Lead Review`:

```
> **MANDATORY — NON-NEGOTIABLE:** This step MUST execute unless `--skip-verification`
> is explicitly present. A subagent that reaches this point without executing
> `Skill(skill: "x-review-pr", ...)` MUST abort and emit
> `"PROTOCOL_VIOLATION: Step 3.6 skipped without --skip-verification"`.
```

### Change 4 — CLI Arguments table: document --skip-review as reserved

Add explicit row to the CLI Arguments table (after `--skip-verification`):

```
| `--skip-review` | Boolean | false | **RESERVED — not yet implemented.** Use `--skip-verification` to bypass all of Phase 3. There is no supported path to skip only reviews while keeping other Phase 3 steps active. |
```

## Success Criteria

1. `x-story-implement` SKILL.md source contains the `## Review Policy` section.
2. Step 3.4 header contains the MANDATORY marker with `PROTOCOL_VIOLATION` log code.
3. Step 3.6 header contains the MANDATORY marker with `PROTOCOL_VIOLATION` log code.
4. `mvn process-resources` regenerates `.claude/skills/x-story-implement/SKILL.md`
   with all markers present.
5. Golden file in `src/test/resources/golden/` updated and `GoldenFileTest` passes.
6. A subagent following the skill instructions cannot omit reviews without explicitly
   violating a clearly documented protocol rule with a named error code.

## Stories

### story-0053-0001 — Add mandatory review policy to SKILL.md source

**Layer:** skills/core/dev

**Tasks:**
1. Add `## Review Policy` block to SKILL.md (Change 1)
2. Add MANDATORY marker to Step 3.4 (Change 2)
3. Add MANDATORY marker to Step 3.6 (Change 3)
4. Add `--skip-review` reserved row to CLI Arguments table (Change 4)
5. Run `mvn process-resources` to regenerate `.claude/skills/x-story-implement/SKILL.md`
6. Verify `.claude/skills/x-story-implement/SKILL.md` reflects all changes
7. Update golden files as needed

**Acceptance criteria (Gherkin):**
```
Cenário: Review Policy presente no output gerado
  DADO que o source SKILL.md contém "## Review Policy"
  QUANDO mvn process-resources é executado
  ENTÃO .claude/skills/x-story-implement/SKILL.md contém "## Review Policy"
  E contém "MANDATORY — NON-NEGOTIABLE" pelo menos 2 vezes
  E contém "REVIEW_SKIPPED_WITHOUT_FLAG"
  E contém "PROTOCOL_VIOLATION"
```

### story-0053-0002 — Add golden file test for mandatory review markers

**Layer:** test

**Tasks:**
1. Locate `SkillsAssemblerTest` (or equivalent test that validates generated SKILL.md content)
2. Add test method `xStoryImplement_containsMandatoryReviewMarkers` asserting:
   - String `MANDATORY — NON-NEGOTIABLE` appears ≥ 2 times
   - String `Review Policy` appears (section header)
   - String `REVIEW_SKIPPED_WITHOUT_FLAG` appears (error log code)
   - String `PROTOCOL_VIOLATION` appears
3. Verify test fails on unmodified source (red) and passes after story-0053-0001 (green)

**Dependency:** story-0053-0001 (test verifies the output of the source change)

## Non-Functional Requirements

- Change is **documentation-only** — no Java production code, no behavioral change.
- The `--skip-verification` flag retains its existing behavior (skips all of Phase 3).
- No new runtime flags are introduced in this epic; `--skip-review` is documented as reserved.
- All existing `SkillsAssemblerTest` scenarios must remain green.
