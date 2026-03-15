# Test Plan -- STORY-0003-0008: x-lib-task-decomposer -- Test-Driven Task Decomposition

## Summary

This story modifies **template content only** -- 2 Markdown source templates + 24 golden files. No TypeScript source code changes. The existing test infrastructure (byte-for-byte golden file parity, assembler unit tests) already covers the assembler logic for `x-lib-task-decomposer`. The test plan focuses on validating content correctness of the modified templates and ensuring all golden files remain in sync.

## Test Strategy

| Category | New Tests Needed | Rationale |
|----------|-----------------|-----------|
| Golden file parity | 0 (existing) | `byte-for-byte.test.ts` automatically validates all 24 golden files across 8 profiles x 3 targets |
| Assembler unit | 0 (existing) | `skills-assembler.test.ts` and `github-skills-assembler.test.ts` already test lib skill copying including `task-decomposer` |
| Template content | 0 (manual verification) | Content correctness validated by reviewing source templates against story DoD |

## Test Coverage

### T1: Golden File Parity (Existing -- `byte-for-byte.test.ts`)

The byte-for-byte integration test at `tests/node/integration/byte-for-byte.test.ts` runs the full pipeline for each of the 8 profiles and compares every generated file against its golden reference. This **automatically** validates all 24 task-decomposer golden files.

| Test ID | Profile | Target | Golden File Path |
|---------|---------|--------|-----------------|
| T1-01 | go-gin | .claude | `tests/golden/go-gin/.claude/skills/lib/x-lib-task-decomposer/SKILL.md` |
| T1-02 | go-gin | .github | `tests/golden/go-gin/.github/skills/lib/x-lib-task-decomposer/SKILL.md` |
| T1-03 | go-gin | .agents | `tests/golden/go-gin/.agents/skills/lib/x-lib-task-decomposer/SKILL.md` |
| T1-04 | java-quarkus | .claude | `tests/golden/java-quarkus/.claude/skills/lib/x-lib-task-decomposer/SKILL.md` |
| T1-05 | java-quarkus | .github | `tests/golden/java-quarkus/.github/skills/lib/x-lib-task-decomposer/SKILL.md` |
| T1-06 | java-quarkus | .agents | `tests/golden/java-quarkus/.agents/skills/lib/x-lib-task-decomposer/SKILL.md` |
| T1-07 | java-spring | .claude | `tests/golden/java-spring/.claude/skills/lib/x-lib-task-decomposer/SKILL.md` |
| T1-08 | java-spring | .github | `tests/golden/java-spring/.github/skills/lib/x-lib-task-decomposer/SKILL.md` |
| T1-09 | java-spring | .agents | `tests/golden/java-spring/.agents/skills/lib/x-lib-task-decomposer/SKILL.md` |
| T1-10 | kotlin-ktor | .claude | `tests/golden/kotlin-ktor/.claude/skills/lib/x-lib-task-decomposer/SKILL.md` |
| T1-11 | kotlin-ktor | .github | `tests/golden/kotlin-ktor/.github/skills/lib/x-lib-task-decomposer/SKILL.md` |
| T1-12 | kotlin-ktor | .agents | `tests/golden/kotlin-ktor/.agents/skills/lib/x-lib-task-decomposer/SKILL.md` |
| T1-13 | python-click-cli | .claude | `tests/golden/python-click-cli/.claude/skills/lib/x-lib-task-decomposer/SKILL.md` |
| T1-14 | python-click-cli | .github | `tests/golden/python-click-cli/.github/skills/lib/x-lib-task-decomposer/SKILL.md` |
| T1-15 | python-click-cli | .agents | `tests/golden/python-click-cli/.agents/skills/lib/x-lib-task-decomposer/SKILL.md` |
| T1-16 | python-fastapi | .claude | `tests/golden/python-fastapi/.claude/skills/lib/x-lib-task-decomposer/SKILL.md` |
| T1-17 | python-fastapi | .github | `tests/golden/python-fastapi/.github/skills/lib/x-lib-task-decomposer/SKILL.md` |
| T1-18 | python-fastapi | .agents | `tests/golden/python-fastapi/.agents/skills/lib/x-lib-task-decomposer/SKILL.md` |
| T1-19 | rust-axum | .claude | `tests/golden/rust-axum/.claude/skills/lib/x-lib-task-decomposer/SKILL.md` |
| T1-20 | rust-axum | .github | `tests/golden/rust-axum/.github/skills/lib/x-lib-task-decomposer/SKILL.md` |
| T1-21 | rust-axum | .agents | `tests/golden/rust-axum/.agents/skills/lib/x-lib-task-decomposer/SKILL.md` |
| T1-22 | typescript-nestjs | .claude | `tests/golden/typescript-nestjs/.claude/skills/lib/x-lib-task-decomposer/SKILL.md` |
| T1-23 | typescript-nestjs | .github | `tests/golden/typescript-nestjs/.github/skills/lib/x-lib-task-decomposer/SKILL.md` |
| T1-24 | typescript-nestjs | .agents | `tests/golden/typescript-nestjs/.agents/skills/lib/x-lib-task-decomposer/SKILL.md` |

**How it works:** `byte-for-byte.test.ts` runs `runPipeline()` to a temp dir, then `verifyOutput()` compares every file byte-for-byte against the golden directory. Any content mismatch, missing file, or extra file causes test failure.

**Existing test names exercised:**
- `pipelineSuccessForProfile_{profile}` -- pipeline completes without error
- `pipelineMatchesGoldenFiles_{profile}` -- byte-for-byte content match
- `noMissingFiles_{profile}` -- no expected files absent from output
- `noExtraFiles_{profile}` -- no unexpected files in output
- `totalFilesGreaterThanZero_{profile}` -- output is not empty

### T2: Assembler Lib Skill Copying (Existing -- Unit Tests)

These existing unit tests validate that the assembler copies lib skills (including `task-decomposer`) correctly. No changes needed.

| Test ID | Test File | Test Name | What It Validates |
|---------|-----------|-----------|-------------------|
| T2-01 | `skills-assembler.test.ts` | `expandsLibSubdirectories` | `lib/task-decomposer` appears in core skill list |
| T2-02 | `skills-assembler.test.ts` | `copiesLibSkillsToOutput` | Lib skills are written to `skills/lib/task-decomposer/SKILL.md` |
| T2-03 | `github-skills-assembler.test.ts` | `SKILL_GROUPS_libGroup_contains3Skills` | `x-lib-task-decomposer` is in the lib SKILL_GROUPS |
| T2-04 | `github-skills-assembler.test.ts` | `assemble_libGroup_generates3Skills` | All 3 lib skills are assembled |
| T2-05 | `github-skills-assembler.test.ts` | `assemble_libGroup_nestedUnderLibDir` | Output paths contain `skills/lib` |
| T2-06 | `github-skills-assembler.test.ts` | `assemble_libGroup_exactOutputPaths` | Output matches expected `skills/lib/{name}/SKILL.md` |
| T2-07 | `github-skills-assembler.test.ts` | `assemble_libGroup_appliesPlaceholderReplacement` | Template variables are replaced (e.g., `{project_name}`) |
| T2-08 | `github-skills-assembler.test.ts` | `assemble_libGroup_templateMissing_skipsSkill` | Missing templates are gracefully skipped |

### T3: Source Template Content Verification (Manual Checklist)

After modifying the 2 source templates, verify these content requirements from the story DoD:

#### T3-A: Claude/Agents Source Template

**File:** `resources/skills-templates/core/lib/x-lib-task-decomposer/SKILL.md`

| Check ID | Requirement | Verification Method |
|----------|-------------|---------------------|
| T3-A-01 | Title updated to `Task Decomposer (Test-Driven + Layer Fallback)` | Search for heading text |
| T3-A-02 | YAML `description` reflects dual-mode capability | Read frontmatter |
| T3-A-03 | Purpose section mentions test-driven primary mode and layer fallback | Read Purpose section |
| T3-A-04 | Test plan added as optional input (Input #3) | Read Inputs section |
| T3-A-05 | STEP 1.5 (Detect Decomposition Mode) present between STEP 1 and STEP 2 | Read Procedure section |
| T3-A-06 | STEP 1.5 checks for TPP markers, not just file existence | Read STEP 1.5 content |
| T3-A-07 | STEP 2A (Test-Driven Decomposition) present with full TDD task structure | Read STEP 2A section |
| T3-A-08 | TDD Task structure contains: Test Scenario, TPP Level, RED, GREEN, REFACTOR, Layer Components, Parallel, Depends On | Read task structure table |
| T3-A-09 | Parallelism detection rules present (3 conditions) | Read parallelism section |
| T3-A-10 | Ordering rules present (4 rules: TPP, inner-outer, AT after UT, dependencies) | Read ordering section |
| T3-A-11 | Task type classification table (UT, AT, IT markers) present | Read classification table |
| T3-A-12 | TDD Task output format section with example | Read output format section |
| T3-A-13 | Layer Task Catalog (G1-G7) preserved under Fallback heading | Search for G1-G7 table |
| T3-A-14 | Fallback section has "When to use" note | Read fallback heading |
| T3-A-15 | Existing STEP 2-5 relabeled as layer-based mode (2B-5) | Read procedure steps |
| T3-A-16 | Layer Dependency Graph preserved verbatim | Compare graph content |
| T3-A-17 | Context Budget Sizes table unchanged | Compare budget table |
| T3-A-18 | Review Tier Assignment table unchanged | Compare tier table |
| T3-A-19 | Escalation Rules unchanged | Compare escalation section |
| T3-A-20 | Integration Notes updated with test plan consumption reference | Read integration notes |
| T3-A-21 | No `{{PLACEHOLDER}}` template variables introduced (content is profile-independent) | Search for `{{` |

#### T3-B: GitHub Source Template

**File:** `resources/github-skills-templates/lib/x-lib-task-decomposer.md`

| Check ID | Requirement | Verification Method |
|----------|-------------|---------------------|
| T3-B-01 | Title updated to `Task Decomposer (Test-Driven + Layer Fallback)` | Search for heading text |
| T3-B-02 | YAML `description` reflects dual-mode with `Reference:` line | Read frontmatter |
| T3-B-03 | Purpose section mentions dual-mode | Read Purpose section |
| T3-B-04 | Test plan added as optional input | Read Inputs section |
| T3-B-05 | STEP 1.5 (Detect Decomposition Mode) present | Read Procedure section |
| T3-B-06 | STEP 2A (Test-Driven Decomposition) present | Read STEP 2A section |
| T3-B-07 | Layer Task Catalog (G1-G7) preserved under Fallback heading | Search for G1-G7 table |
| T3-B-08 | Layer Dependency Graph preserved | Compare graph content |
| T3-B-09 | Path references use `.github/` convention (not `skills/architecture/`) | Search for path references |
| T3-B-10 | Integration Notes updated | Read integration notes |
| T3-B-11 | No `{{PLACEHOLDER}}` template variables introduced | Search for `{{` |

### T4: Cross-Copy Consistency (RULE-001)

Verify the two source templates are semantically consistent despite formatting differences.

| Check ID | Requirement | Verification Method |
|----------|-------------|---------------------|
| T4-01 | Both templates have same title text | Compare headings |
| T4-02 | Both templates have same Layer Task Catalog table (G1-G7) | Compare tables |
| T4-03 | Both templates have same Layer Dependency Graph | Compare graphs |
| T4-04 | Both templates have same STEP sequence (0, 1, 1.5, 2A, 2B-5) | Compare step numbering |
| T4-05 | Both templates have same TDD task structure fields | Compare task fields |
| T4-06 | GitHub copy uses `.github/` paths; Claude copy uses `skills/` paths | Search for path patterns |
| T4-07 | GitHub copy has `Reference:` line in description; Claude copy does not | Compare frontmatter |

### T5: Backward Compatibility (RULE-003)

| Check ID | Requirement | Verification Method |
|----------|-------------|---------------------|
| T5-01 | G1 (FOUNDATION) group preserved in catalog | Search for `G1` in both templates |
| T5-02 | G2 (CONTRACTS) group preserved in catalog | Search for `G2` in both templates |
| T5-03 | G3 (OUTBOUND ADAPTERS) group preserved in catalog | Search for `G3` in both templates |
| T5-04 | G4 (ORCHESTRATION) group preserved in catalog | Search for `G4` in both templates |
| T5-05 | G5 (INBOUND ADAPTERS) group preserved in catalog | Search for `G5` in both templates |
| T5-06 | G6 (OBSERVABILITY) group preserved in catalog | Search for `G6` in both templates |
| T5-07 | G7 (TESTS) group preserved in catalog | Search for `G7` in both templates |
| T5-08 | All 21 task types from original catalog still present | Count rows in catalog table |
| T5-09 | Output path unchanged: `docs/stories/epic-XXXX/plans/tasks-story-XXXX-YYYY.md` | Search for output path |
| T5-10 | Three-level fallback: TPP markers present, file exists no TPP, file absent | Read STEP 1.5 logic |

### T6: New TDD Content Verification

| Check ID | Requirement | Verification Method |
|----------|-------------|---------------------|
| T6-01 | RED field present in TDD task structure | Search for `RED` in task structure |
| T6-02 | GREEN field present in TDD task structure | Search for `GREEN` in task structure |
| T6-03 | REFACTOR field present in TDD task structure (optional) | Search for `REFACTOR` in task structure |
| T6-04 | Layer Components field present in TDD task structure | Search for `Layer Components` |
| T6-05 | Parallel field present (yes/no) | Search for `Parallel` in task structure |
| T6-06 | Depends On field present | Search for `Depends On` in task structure |
| T6-07 | TPP Level field present (1-7) | Search for `TPP Level` in task structure |
| T6-08 | Test Scenario reference field (UT-N / AT-N) present | Search for `Test Scenario` or `UT-N` |
| T6-09 | Task type markers defined: `[UT]`, `[AT]`, `[IT]` | Search for marker definitions |
| T6-10 | AT tasks depend on ALL related UT tasks (rule stated) | Read dependency rules |

### T7: Golden File Content Uniformity

Since the task-decomposer template has **no** `{{PLACEHOLDER}}` variables, content is identical across all profiles within the same target.

| Check ID | Requirement | Verification Method |
|----------|-------------|---------------------|
| T7-01 | All 8 `.claude` golden files are byte-identical to each other | Compare files across profiles |
| T7-02 | All 8 `.agents` golden files are byte-identical to each other | Compare files across profiles |
| T7-03 | All 8 `.github` golden files are byte-identical to each other | Compare files across profiles |
| T7-04 | `.claude` golden content matches `.agents` golden content | Compare .claude vs .agents |
| T7-05 | `.claude` golden content matches source template (after pipeline) | Compare against source |
| T7-06 | `.github` golden content matches GitHub source template (after pipeline) | Compare against source |

## Execution Plan

### Step 1: Before Implementation

Run the full test suite to establish baseline:

```bash
npm test
```

Confirm all 1,384+ tests pass, including byte-for-byte parity.

### Step 2: After Modifying Source Templates

After modifying the 2 source templates (Step 1-2 of implementation plan):

1. Run the byte-for-byte test -- it MUST FAIL (confirms the golden files need updating):
   ```bash
   npx vitest run tests/node/integration/byte-for-byte.test.ts
   ```

2. Verify failures are ONLY for `x-lib-task-decomposer` golden files (no collateral damage).

### Step 3: After Updating Golden Files

After updating all 24 golden files (Steps 3-5 of implementation plan):

1. Run the full test suite:
   ```bash
   npm test
   ```

2. Verify all tests pass.

3. Verify coverage remains at >= 95% line / >= 90% branch.

### Step 4: Content Verification

Manually verify checklists T3-A, T3-B, T4, T5, T6, T7 by reading the modified files.

### Step 5: Cross-Profile Uniformity Check

Verify T7 checks by comparing golden files:

```bash
# .claude golden files should all be identical
diff tests/golden/go-gin/.claude/skills/lib/x-lib-task-decomposer/SKILL.md \
     tests/golden/typescript-nestjs/.claude/skills/lib/x-lib-task-decomposer/SKILL.md

# .agents golden files should match .claude
diff tests/golden/go-gin/.claude/skills/lib/x-lib-task-decomposer/SKILL.md \
     tests/golden/go-gin/.agents/skills/lib/x-lib-task-decomposer/SKILL.md

# .github golden files should all be identical
diff tests/golden/go-gin/.github/skills/lib/x-lib-task-decomposer/SKILL.md \
     tests/golden/typescript-nestjs/.github/skills/lib/x-lib-task-decomposer/SKILL.md
```

## Coverage Estimation

| Metric | Before | After | Delta |
|--------|--------|-------|-------|
| Line Coverage | 99.6% | 99.6% | 0% |
| Branch Coverage | 97.84% | 97.84% | 0% |
| Test Count | 1,384+ | 1,384+ | 0 |
| Test Files | 46 | 46 | 0 |

No new test code is needed. Coverage metrics remain unchanged because:
- No TypeScript source code is modified
- No new TypeScript source code is added
- The existing byte-for-byte test already covers the updated golden files
- The existing assembler tests already cover the lib skill copying mechanism
