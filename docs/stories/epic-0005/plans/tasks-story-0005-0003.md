# Task Breakdown -- story-0005-0003: SKILL.md Skeleton + Input Parsing

## Summary

This story creates the `x-dev-epic-implement` skill template -- the skeleton SKILL.md with frontmatter, input parsing, prerequisite checks, and phase structure placeholders. The deliverable is a new core skill template that gets auto-discovered by `SkillsAssembler` and mirrored to `.agents/` by `CodexSkillsAssembler`. The GitHub variant requires explicit registration in `SKILL_GROUPS.dev`.

**Decomposition Mode:** TDD (RED/GREEN/REFACTOR). Tasks follow test-first discipline.

**New Files:**
1. `resources/skills-templates/core/x-dev-epic-implement/SKILL.md` -- Claude Code skill template (source of truth)
2. `resources/github-skills-templates/dev/x-dev-epic-implement.md` -- GitHub Copilot skill template (dual copy)
3. `tests/node/content/x-dev-epic-implement-content.test.ts` -- Content validation tests

**Modified Files:**
1. `src/assembler/github-skills-assembler.ts` -- Add `"x-dev-epic-implement"` to `SKILL_GROUPS.dev`
2. Golden files for all 8 profiles (`.claude/`, `.github/`, `.agents/` copies + `README.md` files)

**Auto-Discovery Notes:**
- `SkillsAssembler.selectCoreSkills()` auto-discovers `resources/skills-templates/core/x-dev-epic-implement/` -- no code change needed for `.claude/` output
- `CodexSkillsAssembler` mirrors `.claude/skills/` to `.agents/skills/` -- no code change needed for `.agents/` output
- `GithubSkillsAssembler` uses a hardcoded `SKILL_GROUPS` registry -- requires adding `"x-dev-epic-implement"` to the `dev` group array

**Total Tasks:** 10
**Estimated Effort:** Medium

---

## TASK-1: [RED] Write content validation test for frontmatter and basic structure

- **Tier:** Mid
- **Budget:** S
- **Parallel:** no
- **Depends On:** none

**File:** `tests/node/content/x-dev-epic-implement-content.test.ts`

**Scope:**

Create the test file following the pattern in `x-dev-adr-automation-content.test.ts`. Write tests that:

1. Read `resources/skills-templates/core/x-dev-epic-implement/SKILL.md` (Claude source)
2. Read `resources/github-skills-templates/dev/x-dev-epic-implement.md` (GitHub source)
3. Assert Claude source contains YAML frontmatter with:
   - `name: x-dev-epic-implement`
   - `description:` field (non-empty)
   - `allowed-tools:` field listing Read, Write, Edit, Bash, Grep, Glob, Skill
   - `argument-hint:` field containing `EPIC-ID`, `--phase`, `--story`, `--skip-review`, `--dry-run`, `--resume`, `--parallel`
4. Assert GitHub source contains YAML frontmatter with:
   - `name: x-dev-epic-implement`
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
- `resources/skills-templates/core/x-dev-epic-implement/SKILL.md`
- `resources/github-skills-templates/dev/x-dev-epic-implement.md`

**Scope:**

1. Create `resources/skills-templates/core/x-dev-epic-implement/SKILL.md` with YAML frontmatter:
   ```yaml
   ---
   name: x-dev-epic-implement
   description: "Orchestrates the implementation of an entire epic by executing stories sequentially or in parallel via worktrees. Parses epic ID and flags, validates prerequisites (epic directory, IMPLEMENTATION-MAP.md, story files), then delegates story execution to x-dev-lifecycle subagents."
   allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill
   argument-hint: "[EPIC-ID] [--phase N] [--story XXXX-YYYY] [--skip-review] [--dry-run] [--resume] [--parallel]"
   ---
   ```
   Add Global Output Policy and placeholder heading: `# Skill: Epic Implementation Orchestrator`

2. Create `resources/github-skills-templates/dev/x-dev-epic-implement.md` with matching YAML frontmatter (GitHub format -- `name` and `description` only, with `>` multi-line description):
   ```yaml
   ---
   name: x-dev-epic-implement
   description: >
     Orchestrates the implementation of an entire epic by executing stories
     sequentially or in parallel via worktrees. Parses epic ID and flags,
     validates prerequisites, then delegates story execution to x-dev-lifecycle.
   ---
   ```
   Add same placeholder heading.

**Expected result:** TASK-1 frontmatter tests turn GREEN.

**Acceptance Criteria:**
- Both files exist with valid YAML frontmatter
- TASK-1 tests pass for frontmatter assertions

---

## TASK-3: [RED] Write content tests for required sections (input parsing, prerequisites, phases)

- **Tier:** Mid
- **Budget:** S
- **Parallel:** no
- **Depends On:** TASK-2

**File:** `tests/node/content/x-dev-epic-implement-content.test.ts`

**Scope:**

Add tests asserting both Claude and GitHub sources contain the required section headings and key content:

1. **Input Parsing section:**
   - Contains section heading for input/argument parsing
   - Contains `epic-XXXX` or `EPIC-ID` positional argument reference
   - Contains all 6 flags: `--phase`, `--story`, `--skip-review`, `--dry-run`, `--resume`, `--parallel`
   - Contains default value documentation (e.g., `false` for boolean flags, `sequential` for parallel)

2. **Prerequisites section:**
   - Contains `docs/stories/epic-` path reference
   - Contains `IMPLEMENTATION-MAP.md` reference
   - Contains `EPIC-` file reference
   - Contains `story-` file pattern reference
   - Contains `execution-state.json` reference (for `--resume`)
   - Contains error message guidance for missing prerequisites

3. **Phase structure:**
   - Contains Phase 0 heading (Preparation)
   - Contains Phase 1 heading (Execution Loop) with placeholder indication
   - Contains Phase 2 heading (Consolidation) with placeholder indication
   - Contains Phase 3 heading (Verification) with placeholder indication

4. **Global Output Policy:**
   - Contains `English ONLY` directive

Use `it.each` for repetitive assertions.

**Expected result:** Tests FAIL because SKILL.md files only have frontmatter and a placeholder heading.

**Acceptance Criteria:**
- New tests compile
- New tests fail on missing section content
- Existing TASK-1 tests still pass

---

## TASK-4: [GREEN] Add input parsing, prerequisites, and phase structure to both templates

- **Tier:** Senior
- **Budget:** L
- **Parallel:** no
- **Depends On:** TASK-3

**Files:**
- `resources/skills-templates/core/x-dev-epic-implement/SKILL.md`
- `resources/github-skills-templates/dev/x-dev-epic-implement.md`

**Scope:**

Add all required sections to the Claude SKILL.md:

1. **Global Output Policy** -- English ONLY, technical, direct, concise (matching `x-dev-lifecycle` pattern)

2. **When to Use** -- decision tree: full epic implementation, multi-story orchestration

3. **Input Parsing** -- Complete argument documentation:
   - Positional: `epic-XXXX` (mandatory epic ID, 4-digit zero-padded)
   - `--phase N` -- execute only phase N (0-3)
   - `--story XXXX-YYYY` -- execute only a specific story
   - `--skip-review` -- skip review phases in subagents (default: false)
   - `--dry-run` -- generate execution plan without executing (default: false)
   - `--resume` -- continue from last checkpoint (default: false)
   - `--parallel` -- enable parallel worktrees (default: false, sequential)
   - Error handling: missing epic ID aborts with usage message

4. **Prerequisites Check** -- 5 validation steps:
   - Verify `docs/stories/epic-XXXX/` directory exists
   - Verify `EPIC-XXXX.md` exists in the directory
   - Verify `IMPLEMENTATION-MAP.md` exists in the directory
   - Verify at least one `story-XXXX-YYYY.md` exists
   - If `--resume`: verify `execution-state.json` exists
   - Each failure aborts with clear error message and suggested action

5. **Phase 0 -- Preparation** -- inline: parsing + prerequisites + branch creation

6. **Phase 1 -- Execution Loop** -- placeholder with comment marker:
   `<!-- PHASE_1_CONTENT: Implemented in story-0005-0005 -->`

7. **Phase 2 -- Consolidation** -- placeholder with comment marker:
   `<!-- PHASE_2_CONTENT: Implemented in story-0005-0011 -->`

8. **Phase 3 -- Verification** -- placeholder with comment marker:
   `<!-- PHASE_3_CONTENT: Implemented in story-0005-0011 -->`

Mirror all sections to the GitHub template with `.github/skills/` path references.

**Expected result:** TASK-3 section heading and content tests turn GREEN.

**Acceptance Criteria:**
- All required sections present in both files
- All TASK-1 and TASK-3 tests pass
- Input parsing covers all 7 arguments (1 positional + 6 flags)
- Prerequisites cover all 5 checks + resume check
- Phase placeholders include clear markers for future extension

---

## TASK-5: [RED] Write content tests for detailed prerequisites and error messages

- **Tier:** Mid
- **Budget:** S
- **Parallel:** no
- **Depends On:** TASK-4

**File:** `tests/node/content/x-dev-epic-implement-content.test.ts`

**Scope:**

Add tests asserting specific content within sections:

1. **Error message specifics:**
   - Contains suggestion to run `/x-story-epic-full` when epic directory missing
   - Contains suggestion to run `/x-story-map` when IMPLEMENTATION-MAP.md missing
   - Contains "Epic ID is required" error message for missing argument
   - Contains "No checkpoint found" or equivalent for `--resume` without state

2. **Prerequisite check flow:**
   - Contains abort/stop instruction on prerequisite failure
   - Contains `glob` or file scanning instruction for story file discovery

3. **Phase 0 specifics:**
   - Contains branch creation instruction (e.g., `git checkout -b`)
   - Contains IMPLEMENTATION-MAP.md reading step
   - Contains story ordering/dependency extraction step

4. **Dual copy consistency (RULE-001):**
   - Both Claude and GitHub sources contain all critical terms
   - Use `it.each` over array of critical terms: `docs/stories/epic-`, `IMPLEMENTATION-MAP.md`, `--phase`, `--resume`, `execution-state.json`, `x-dev-lifecycle`, `Phase 0`, `Phase 1`, `Phase 2`, `Phase 3`

**Expected result:** Tests FAIL because sections lack detailed error messages and flow instructions.

**Acceptance Criteria:**
- New tests compile
- New tests fail on missing detailed content
- All prior tests still pass

---

## TASK-6: [GREEN] Add detailed error messages, prerequisite flow, and Phase 0 content

- **Tier:** Senior
- **Budget:** L
- **Parallel:** no
- **Depends On:** TASK-5

**Files:**
- `resources/skills-templates/core/x-dev-epic-implement/SKILL.md`
- `resources/github-skills-templates/dev/x-dev-epic-implement.md`

**Scope:**

Expand sections with detailed content:

1. **Prerequisites Check** -- Add specific error messages:
   - Missing epic directory: `"ERROR: Directory docs/stories/epic-{epicId}/ not found. Run /x-story-epic-full first."`
   - Missing epic file: `"ERROR: EPIC-{epicId}.md not found in docs/stories/epic-{epicId}/. Run /x-story-epic first."`
   - Missing IMPLEMENTATION-MAP: `"ERROR: IMPLEMENTATION-MAP.md not found. Run /x-story-map first."`
   - No story files: `"ERROR: No story files found matching story-{epicId}-*.md."`
   - Missing checkpoint: `"ERROR: No checkpoint found. Cannot resume. Run without --resume."`
   - Missing epic ID: `"ERROR: Epic ID is required. Usage: /x-dev-epic-implement [EPIC-ID] [flags]"`

2. **Phase 0 -- Preparation** -- Expand with detailed steps:
   - Parse arguments (epic ID + flags)
   - Run all prerequisite checks (abort on first failure)
   - Read `IMPLEMENTATION-MAP.md` to extract story dependency graph
   - Read `EPIC-XXXX.md` for epic context
   - Glob `story-XXXX-*.md` to collect all story files
   - Determine execution order from dependency graph
   - Create branch: `git checkout -b feat/epic-{epicId}-implementation`
   - If `--dry-run`: output execution plan and stop
   - If `--resume`: read `execution-state.json` and skip completed stories
   - Reference to `/x-dev-lifecycle` as the skill used per story

3. **Phase placeholders** -- Ensure each placeholder mentions:
   - The story ID that will implement it (story-0005-0005 or story-0005-0011)
   - A brief description of what the phase will do

Mirror all content to the GitHub template.

**Expected result:** All TASK-5 content-specific tests turn GREEN.

**Acceptance Criteria:**
- All content tests pass (TASK-1, TASK-3, TASK-5)
- Claude and GitHub templates contain equivalent content
- Error messages match Gherkin acceptance criteria from the story spec
- Phase 0 contains complete preparation flow

---

## TASK-7: [REFACTOR] Polish SKILL.md content and register GitHub skill in SKILL_GROUPS

- **Tier:** Senior
- **Budget:** M
- **Parallel:** no
- **Depends On:** TASK-6

**Files:**
- `resources/skills-templates/core/x-dev-epic-implement/SKILL.md`
- `resources/github-skills-templates/dev/x-dev-epic-implement.md`
- `src/assembler/github-skills-assembler.ts`

**Scope:**

1. **Polish both SKILL.md templates:**
   - Review all sections for clarity and completeness
   - Ensure consistent formatting (headers, bullet points, code blocks)
   - Verify all Gherkin acceptance criteria from the story are addressed
   - Ensure no `{single_brace}` placeholders are used (this is a profile-agnostic skill)
   - Ensure `{{DOUBLE_BRACE}}` runtime markers are used correctly for project-specific values
   - Verify the GitHub template uses `.github/skills/` paths, not `.claude/skills/` paths
   - Check line lengths (aim for readable markdown)

2. **Register in GithubSkillsAssembler:**
   - Add `"x-dev-epic-implement"` to `SKILL_GROUPS.dev` array in `src/assembler/github-skills-assembler.ts`
   - Current `dev` group (lines 29-33):
     ```typescript
     "dev": [
         "x-dev-implement", "x-dev-lifecycle",
         "x-dev-architecture-plan", "x-dev-arch-update",
         "layer-templates", "x-dev-adr-automation",
     ],
     ```
   - Add `"x-dev-epic-implement"` after `"x-dev-lifecycle"` (logical grouping: implement, lifecycle, epic-implement):
     ```typescript
     "dev": [
         "x-dev-implement", "x-dev-lifecycle",
         "x-dev-epic-implement",
         "x-dev-architecture-plan", "x-dev-arch-update",
         "layer-templates", "x-dev-adr-automation",
     ],
     ```

3. **Verify compilation:** `npx tsc --noEmit`

**Expected result:** All existing tests still pass. TypeScript compiles cleanly.

**Acceptance Criteria:**
- `SKILL_GROUPS.dev` includes `"x-dev-epic-implement"`
- TypeScript compiles without errors
- All content tests pass
- No functional behavior change (refactor only for SKILL.md polish; registry addition is a necessary code change that does not alter existing behavior)

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
   - **New** `.claude/skills/x-dev-epic-implement/SKILL.md` (one per profile, all identical since no `{single_brace}` tokens used)
   - **New** `.github/skills/x-dev-epic-implement/SKILL.md` (one per profile, all identical)
   - **New** `.agents/skills/x-dev-epic-implement/SKILL.md` (one per profile, mirrors `.claude/` copy)
   - **Updated** `.claude/README.md` (skill count and skill table updated with new entry)

**Procedure:**
1. Run `UPDATE_GOLDEN=true npx vitest run tests/node/integration/byte-for-byte.test.ts` or equivalent golden file update mechanism
2. Alternatively, run the generator per profile and manually copy outputs to golden directories
3. Verify no unexpected file changes beyond the new skill and count updates
4. Verify that `x-dev-epic-implement/SKILL.md` is identical across all 8 profiles for each output target (`.claude/`, `.github/`, `.agents/`)

**Expected result:** Golden files updated with the new skill in all 8 profiles.

**Acceptance Criteria:**
- Each profile's golden directory contains `x-dev-epic-implement/SKILL.md` under `.claude/skills/`, `.github/skills/`, and `.agents/skills/`
- README.md skill counts incremented correctly
- No unrelated golden file changes
- New golden files: 24 (8 profiles x 3 output targets)
- Modified golden files: 8 (README.md for each profile)

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
   - `x-dev-epic-implement-content.test.ts` -- content validation tests (TASK-1, TASK-3, TASK-5)
   - All existing tests (~1,384+) remain passing
3. Fix any test failures (common issues: trailing whitespace, missing newlines, skill count off-by-one in README)

**Expected result:** All tests pass. Zero failures.

**Acceptance Criteria:**
- `npx vitest run` exits with code 0
- No test regressions
- New content tests all pass
- Byte-for-byte parity holds for all 8 profiles

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
5. Verify the DoD checklist from the story:
   - [x] SKILL.md created in `resources/skills-templates/core/x-dev-epic-implement/SKILL.md`
   - [x] Frontmatter YAML valid with all fields
   - [x] Input parsing documented for all flags
   - [x] Prerequisites verified with clear error messages
   - [x] Phase structure with placeholders for future extension
   - [x] Dual copy maintained (`resources/` + `.claude/` + `.github/` + `.agents/`)
   - [x] Golden file tests validating the generated SKILL.md

**Expected result:** Coverage thresholds met. Clean compilation. All DoD items satisfied.

**Acceptance Criteria:**
- Line coverage >= 95% (expected: ~99.6%, minimal TypeScript change)
- Branch coverage >= 90% (expected: ~97.84%, no branch changes)
- Zero TypeScript compilation errors
- Zero linter warnings
- All story DoD items checked

---

## Execution Order

```
TASK-1  [RED]       Write frontmatter + argument-hint tests (fail)
   |
   v
TASK-2  [GREEN]     Create SKILL.md and GitHub template with frontmatter (pass)
   |
   v
TASK-3  [RED]       Write section heading + content structure tests (fail)
   |
   v
TASK-4  [GREEN]     Add input parsing, prerequisites, phases to both templates (pass)
   |
   v
TASK-5  [RED]       Write detailed error message + flow tests (fail)
   |
   v
TASK-6  [GREEN]     Add detailed error messages, Phase 0 flow, placeholder content (pass)
   |
   v
TASK-7  [REFACTOR]  Polish templates + register GitHub skill in SKILL_GROUPS
   |
   v
TASK-8              Regenerate golden files for all 8 profiles (24 new + 8 updated)
   |
   v
TASK-9              Run full test suite (byte-for-byte validation)
   |
   v
TASK-10             Coverage check + final verification + DoD checklist
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
| TASK-6 | GREEN | 2 (template updates) | MEDIUM |
| TASK-7 | REFACTOR | 3 (templates + TS source) | MEDIUM |
| TASK-8 | Golden files | ~32 golden files | MEDIUM (regeneration) |
| TASK-9 | Verification | 0 (read-only) | LOW |
| TASK-10 | Verification | 0 (read-only) | LOW |
| **Total** | | **~38 files** | |

### File Manifest

**New Files (27):**

| # | Path |
|---|------|
| 1 | `resources/skills-templates/core/x-dev-epic-implement/SKILL.md` |
| 2 | `resources/github-skills-templates/dev/x-dev-epic-implement.md` |
| 3 | `tests/node/content/x-dev-epic-implement-content.test.ts` |
| 4-11 | `tests/golden/{profile}/.claude/skills/x-dev-epic-implement/SKILL.md` (8 profiles) |
| 12-19 | `tests/golden/{profile}/.github/skills/x-dev-epic-implement/SKILL.md` (8 profiles) |
| 20-27 | `tests/golden/{profile}/.agents/skills/x-dev-epic-implement/SKILL.md` (8 profiles) |

**Modified Files (9):**

| # | Path | Change |
|---|------|--------|
| 1 | `src/assembler/github-skills-assembler.ts` | Add skill to `SKILL_GROUPS.dev` |
| 2-9 | `tests/golden/{profile}/.claude/README.md` (8 profiles) | Regenerated (new skill in table/count) |
