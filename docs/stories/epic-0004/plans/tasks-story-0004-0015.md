# Task Breakdown -- story-0004-0015: ADR Automation Skill

## Summary

This story creates a new core skill template (`x-dev-adr-automation`) that instructs AI agents to automate ADR generation from architecture plan mini-ADRs. The primary deliverable is a SKILL.md template file. One TypeScript source code change is required: registering the new skill in `GithubSkillsAssembler.SKILL_GROUPS.dev` (the Claude Code `SkillsAssembler` auto-discovers new core skills, but the GitHub assembler uses a hardcoded registry).

**Decomposition Mode:** TDD (RED/GREEN/REFACTOR). Tasks follow test-first discipline.

**New Files:**
1. `resources/skills-templates/core/x-dev-adr-automation/SKILL.md` -- Claude Code skill template (source of truth)
2. `resources/github-skills-templates/dev/x-dev-adr-automation.md` -- GitHub Copilot skill template (dual copy)
3. `tests/node/content/x-dev-adr-automation-content.test.ts` -- Content validation tests

**Modified Files:**
1. `src/assembler/github-skills-assembler.ts` -- Add `"x-dev-adr-automation"` to `SKILL_GROUPS.dev`
2. Golden files for all 8 profiles (both `.claude/` and `.github/` copies) + README.md files

**Plan correction:** The implementation plan (Section 5.4, Section 10) states that `GithubSkillsAssembler` auto-mirrors core skills. This is **incorrect**. The `GithubSkillsAssembler` uses a hardcoded `SKILL_GROUPS` registry (see `src/assembler/github-skills-assembler.ts:23-55`). Adding a new skill requires: (a) creating the GitHub template file in `resources/github-skills-templates/dev/`, and (b) adding its name to the `SKILL_GROUPS.dev` array. Tasks below account for this.

**Total Tasks:** 10
**Estimated Effort:** Medium

---

## TASK-1: [RED] Write content validation test for SKILL.md frontmatter

- **Tier:** Mid
- **Budget:** S
- **Parallel:** no
- **Depends On:** none

**File:** `tests/node/content/x-dev-adr-automation-content.test.ts`

**Scope:**

Create the test file following the pattern in `x-story-create-content.test.ts`. Write tests that:

1. Read `resources/skills-templates/core/x-dev-adr-automation/SKILL.md` (Claude source)
2. Read `resources/github-skills-templates/dev/x-dev-adr-automation.md` (GitHub source)
3. Assert Claude source contains YAML frontmatter with:
   - `name: x-dev-adr-automation`
   - `description:` field (non-empty)
   - `allowed-tools:` field listing Read, Write, Edit, Bash, Grep, Glob
   - `argument-hint:` field
4. Assert GitHub source contains YAML frontmatter with:
   - `name: x-dev-adr-automation`
   - `description:` field (non-empty)

**Expected result:** Tests FAIL because neither SKILL.md file exists yet.

**Acceptance Criteria:**
- Test file compiles (`npx tsc --noEmit`)
- Tests fail with "ENOENT: no such file or directory" or content assertion failures
- Test naming follows `[methodUnderTest]_[scenario]_[expectedBehavior]` convention

---

## TASK-2: [GREEN] Create Claude SKILL.md with frontmatter and GitHub template with frontmatter

- **Tier:** Senior
- **Budget:** S
- **Parallel:** no
- **Depends On:** TASK-1

**Files:**
- `resources/skills-templates/core/x-dev-adr-automation/SKILL.md`
- `resources/github-skills-templates/dev/x-dev-adr-automation.md`

**Scope:**

1. Create `resources/skills-templates/core/x-dev-adr-automation/SKILL.md` with YAML frontmatter:
   ```yaml
   ---
   name: x-dev-adr-automation
   description: "Automates ADR generation from architecture plan mini-ADRs: extracts inline decisions, expands to full ADR format, assigns sequential numbering, updates the ADR index, and adds cross-references."
   allowed-tools:
     - Read
     - Write
     - Edit
     - Bash
     - Grep
     - Glob
   argument-hint: "[architecture-plan-path] [story-id]"
   ---
   ```
   Add a placeholder heading: `# Skill: ADR Automation`

2. Create `resources/github-skills-templates/dev/x-dev-adr-automation.md` with matching YAML frontmatter (GitHub format -- `name` and `description` only, with `>` multi-line description):
   ```yaml
   ---
   name: x-dev-adr-automation
   description: >
     Automates ADR generation from architecture plan mini-ADRs: extracts inline
     decisions, expands to full ADR format, assigns sequential numbering, updates
     the ADR index, and adds cross-references.
   ---
   ```
   Add same placeholder heading.

**Expected result:** TASK-1 frontmatter tests turn GREEN.

**Acceptance Criteria:**
- Both files exist with valid YAML frontmatter
- TASK-1 tests pass for frontmatter assertions

---

## TASK-3: [RED] Write content tests for required sections

- **Tier:** Mid
- **Budget:** S
- **Parallel:** no
- **Depends On:** TASK-2

**File:** `tests/node/content/x-dev-adr-automation-content.test.ts`

**Scope:**

Add tests asserting both Claude and GitHub sources contain the required section headings:

1. `## When to Use` -- decision tree for when this skill is appropriate
2. `## Input Format` -- mini-ADR structure (title, context, decision, rationale)
3. `## Output Format` -- full ADR structure with frontmatter
4. `## Algorithm` -- step-by-step agent instructions
5. `## Duplicate Detection` -- title similarity rules
6. `## Cross-Reference Rules` -- story-to-ADR and ADR-to-story linking
7. `## Sequential Numbering` -- scan docs/adr/, find max, increment
8. `## Index Update` -- append row to docs/adr/README.md table
9. `## Examples` -- before/after conversion examples

Use `it.each` for the section headings array to keep tests concise.

**Expected result:** Tests FAIL because SKILL.md files only have frontmatter and a placeholder heading.

**Acceptance Criteria:**
- New tests compile
- New tests fail on missing section content
- Existing TASK-1 tests still pass

---

## TASK-4: [GREEN] Add all required sections to both SKILL.md templates

- **Tier:** Senior
- **Budget:** L
- **Parallel:** no
- **Depends On:** TASK-3

**Files:**
- `resources/skills-templates/core/x-dev-adr-automation/SKILL.md`
- `resources/github-skills-templates/dev/x-dev-adr-automation.md`

**Scope:**

Add all required sections to the Claude SKILL.md with complete content:

1. **When to Use** -- after architecture plan phase, when mini-ADRs exist inline
2. **Input Format** -- mini-ADR structure (markdown format from architecture plan):
   ```markdown
   ### ADR: [Title]
   - **Context:** ...
   - **Decision:** ...
   - **Rationale:** ...
   ```
3. **Output Format** -- full ADR with YAML frontmatter (status, date, story-ref) and sections (Status, Context, Decision, Consequences)
4. **Algorithm** -- high-level step-by-step (details filled in TASK-6)
5. **Sequential Numbering** -- stub with basic description
6. **Duplicate Detection** -- stub with basic description
7. **Cross-Reference Rules** -- stub with basic description
8. **Index Update** -- stub with basic description
9. **Examples** -- stub placeholder

Mirror the same sections to the GitHub template, with `.github/skills/` path references instead of `.claude/skills/`.

**Expected result:** TASK-3 section heading tests turn GREEN.

**Acceptance Criteria:**
- All 9 required sections present in both files
- All TASK-1 and TASK-3 tests pass

---

## TASK-5: [RED] Write content tests for specific algorithm content

- **Tier:** Mid
- **Budget:** S
- **Parallel:** no
- **Depends On:** TASK-4

**File:** `tests/node/content/x-dev-adr-automation-content.test.ts`

**Scope:**

Add tests asserting specific content within sections:

1. **Duplicate detection specifics:**
   - Contains "title similarity" or "similar title" reference
   - Contains "skip" or "warning" for duplicate handling
   - Contains "Duplicate ADR detected" warning message text

2. **Cross-reference specifics:**
   - Contains `story-ref` frontmatter field reference
   - Contains bidirectional linking (story to ADR and ADR to story)
   - Contains `docs/adr/` path reference

3. **Sequential numbering specifics:**
   - Contains `ADR-NNNN` or `ADR-0001` pattern reference
   - Contains "max" or "maximum" number scanning logic
   - Contains "increment" or "next number" concept

4. **Index update specifics:**
   - Contains `docs/adr/README.md` reference
   - Contains "table" or "index" update instruction

5. **Dual copy consistency (RULE-001):**
   - Both sources contain all key terms (using `it.each` over an array of critical terms)

**Expected result:** Tests FAIL because section stubs lack detailed content.

**Acceptance Criteria:**
- New tests compile
- New tests fail on missing detailed content
- All prior tests still pass

---

## TASK-6: [GREEN] Add detailed algorithm content to both SKILL.md templates

- **Tier:** Senior
- **Budget:** XL
- **Parallel:** no
- **Depends On:** TASK-5

**Files:**
- `resources/skills-templates/core/x-dev-adr-automation/SKILL.md`
- `resources/github-skills-templates/dev/x-dev-adr-automation.md`

**Scope:**

Replace stubs with full detailed content:

1. **Algorithm** -- Complete step-by-step instructions:
   - Step 1: Read architecture plan file, parse mini-ADRs (search for `### ADR:` markers)
   - Step 2: Scan `docs/adr/` for existing ADR files, extract max number
   - Step 3: For each mini-ADR, check duplicate detection (title similarity)
   - Step 4: Expand mini-ADR to full ADR using `_TEMPLATE-ADR.md`
   - Step 5: Write `docs/adr/ADR-NNNN-title-in-kebab-case.md`
   - Step 6: Update `docs/adr/README.md` index table
   - Step 7: Add cross-references (story-ref in ADR frontmatter, ADR links in plan)

2. **Sequential Numbering** -- Detailed algorithm:
   - Glob `docs/adr/ADR-*.md`, extract numeric prefixes
   - Find maximum, increment by 1
   - Pad to 4 digits (ADR-0001 format)
   - Handle empty directory (start from ADR-0001)

3. **Duplicate Detection** -- Detailed rules:
   - Normalize titles (lowercase, strip punctuation)
   - Compare against existing ADR titles
   - If match found: emit "Duplicate ADR detected, skipping" warning
   - Do not overwrite existing ADRs

4. **Cross-Reference Rules** -- Detailed linking:
   - ADR frontmatter includes `story-ref: {story-id}`
   - ADR Context section references the originating story
   - Architecture plan updated with `[ADR-NNNN](docs/adr/ADR-NNNN-title.md)` links
   - Service architecture doc Section 7 updated if it exists

5. **Index Update** -- Table format and append logic:
   - Append row: `| ADR-NNNN | Title | Accepted | YYYY-MM-DD |`
   - Create index file if it does not exist

6. **Examples** -- Before/after mini-ADR to full ADR conversion example

Mirror all content to the GitHub template with appropriate path adjustments.

**Expected result:** All TASK-5 content-specific tests turn GREEN.

**Acceptance Criteria:**
- All content tests pass (TASK-1, TASK-3, TASK-5)
- Claude and GitHub templates contain equivalent content
- No placeholder stubs remain

---

## TASK-7: [REFACTOR] Polish SKILL.md content and register GitHub skill

- **Tier:** Senior
- **Budget:** M
- **Parallel:** no
- **Depends On:** TASK-6

**Files:**
- `resources/skills-templates/core/x-dev-adr-automation/SKILL.md`
- `resources/github-skills-templates/dev/x-dev-adr-automation.md`
- `src/assembler/github-skills-assembler.ts`

**Scope:**

1. **Polish both SKILL.md templates:**
   - Review algorithm steps for clarity and completeness
   - Ensure consistent formatting (headers, bullet points, code blocks)
   - Verify all Gherkin acceptance criteria from the story are covered by skill instructions
   - Ensure `{project_name}` placeholder is used where project-specific values are needed
   - Verify the GitHub template uses `.github/skills/` paths, not `.claude/skills/` paths

2. **Register in GithubSkillsAssembler:**
   - Add `"x-dev-adr-automation"` to `SKILL_GROUPS.dev` array in `src/assembler/github-skills-assembler.ts` (line 28-30)
   - Current value: `["x-dev-implement", "x-dev-lifecycle", "layer-templates"]`
   - New value: `["x-dev-implement", "x-dev-lifecycle", "layer-templates", "x-dev-adr-automation"]`

3. **Verify compilation:** `npx tsc --noEmit`

**Expected result:** All existing tests still pass. TypeScript compiles cleanly.

**Acceptance Criteria:**
- `SKILL_GROUPS.dev` includes `"x-dev-adr-automation"`
- TypeScript compiles without errors
- All content tests pass
- No functional behavior change (refactor only for SKILL.md polish; registry addition is a necessary code change)

---

## TASK-8: Regenerate golden files for all 8 profiles

- **Tier:** Mid
- **Budget:** M
- **Parallel:** no
- **Depends On:** TASK-7

**Files:** Golden file directories for all 8 profiles:
- `tests/golden/go-gin/`
- `tests/golden/java-quarkus/`
- `tests/golden/java-spring/`
- `tests/golden/kotlin-ktor/`
- `tests/golden/python-click-cli/`
- `tests/golden/python-fastapi/`
- `tests/golden/rust-axum/`
- `tests/golden/typescript-nestjs/`

**Scope:**

1. Run the generator for each profile to produce updated output
2. Copy generated outputs to golden file directories
3. Updated golden files will include:
   - New `.claude/skills/x-dev-adr-automation/SKILL.md` (one per profile)
   - New `.github/skills/x-dev-adr-automation/SKILL.md` (one per profile)
   - Updated `.claude/README.md` (skill count and skill table updated)
   - Updated `.github/copilot-instructions.md` or equivalent (if skill count is reflected there)

**Procedure:**
1. Run `UPDATE_GOLDEN=true npx vitest run` or equivalent golden file update mechanism
2. Alternatively, run the generator per profile and copy outputs
3. Verify no unexpected file changes beyond the new skill and count updates

**Expected result:** Golden files updated with the new skill in all 8 profiles.

**Acceptance Criteria:**
- Each profile's golden directory contains `x-dev-adr-automation/SKILL.md` under both `.claude/skills/` and `.github/skills/`
- README.md skill counts incremented by the correct amount
- No unrelated golden file changes

---

## TASK-9: Run full test suite to verify golden file byte-for-byte match

- **Tier:** Mid
- **Budget:** S
- **Parallel:** no
- **Depends On:** TASK-8

**Command:** `npx vitest run`

**Scope:**

1. Run the complete test suite
2. Verify all tests pass, including:
   - `byte-for-byte.test.ts` -- golden file parity for all 8 profiles
   - `x-dev-adr-automation-content.test.ts` -- content validation tests (TASK-1, TASK-3, TASK-5)
   - All existing tests (~1,384+) remain passing
3. Fix any test failures

**Expected result:** All tests pass. Zero failures.

**Acceptance Criteria:**
- `npx vitest run` exits with code 0
- No test regressions
- New content tests all pass

---

## TASK-10: Run coverage check and final verification

- **Tier:** Mid
- **Budget:** S
- **Parallel:** no
- **Depends On:** TASK-9

**Commands:**
- `npx vitest run --coverage`
- `npx tsc --noEmit`

**Scope:**

1. Run coverage report to verify thresholds:
   - Line coverage >= 95%
   - Branch coverage >= 90%
2. Verify TypeScript compilation is clean
3. Review the single source code change (`github-skills-assembler.ts`) for correctness
4. Confirm no compiler or linter warnings

**Expected result:** Coverage thresholds met. Clean compilation.

**Acceptance Criteria:**
- Line coverage >= 95% (expected: ~99.6%, minimal change)
- Branch coverage >= 90% (expected: ~97.84%, no branch changes)
- Zero TypeScript compilation errors
- Zero linter warnings

---

## Execution Order

```
TASK-1  [RED]       Write frontmatter tests (fail)
   |
   v
TASK-2  [GREEN]     Create SKILL.md frontmatter (pass)
   |
   v
TASK-3  [RED]       Write section heading tests (fail)
   |
   v
TASK-4  [GREEN]     Add all required sections (pass)
   |
   v
TASK-5  [RED]       Write detailed content tests (fail)
   |
   v
TASK-6  [GREEN]     Add detailed algorithm content (pass)
   |
   v
TASK-7  [REFACTOR]  Polish + register GitHub skill in SKILL_GROUPS
   |
   v
TASK-8              Regenerate golden files for all 8 profiles
   |
   v
TASK-9              Run full test suite (byte-for-byte validation)
   |
   v
TASK-10             Coverage check + final verification
```

All tasks are **sequential** -- each depends on the previous one. There are no parallel execution opportunities because:
- TDD RED/GREEN tasks are inherently sequential (test must fail before implementation)
- Golden file regeneration depends on all source files being complete
- Test suite depends on golden files being current

---

## Summary

| Task | Phase | Files Changed | Impact |
|------|-------|---------------|--------|
| TASK-1 | RED | 1 (new test) | LOW |
| TASK-2 | GREEN | 2 (new templates) | LOW |
| TASK-3 | RED | 1 (test update) | LOW |
| TASK-4 | GREEN | 2 (template updates) | MEDIUM |
| TASK-5 | RED | 1 (test update) | LOW |
| TASK-6 | GREEN | 2 (template updates) | HIGH (largest content change) |
| TASK-7 | REFACTOR | 3 (templates + TS source) | MEDIUM |
| TASK-8 | Golden files | ~26 golden files | MEDIUM (regeneration) |
| TASK-9 | Verification | 0 (read-only) | LOW |
| TASK-10 | Verification | 0 (read-only) | LOW |
| **Total** | | **~35 files** | |
