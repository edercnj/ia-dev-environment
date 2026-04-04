# Task Breakdown -- STORY-0003-0008: x-lib-task-decomposer -- Test-Driven Task Decomposition

## Summary

This story modifies template content only -- 2 Markdown skill template files + 24 golden files. No TypeScript source code changes are required. The byte-for-byte integration test automatically validates golden file parity across all profiles.

**Decomposition Mode:** Layer-based (G1-G3). No test plan with TPP markers exists for this story. Tasks are grouped by dependency order following the implementation plan Section 10.

**Total Tasks:** 7
**Estimated Effort:** Small-Medium

---

## G1: FOUNDATION -- Source Template Modifications

> **Parallel within group:** Yes (TASK-1 and TASK-2 can run in parallel)
> **Dependencies:** None

### TASK-1: Modify Claude/Agents source template with test-driven decomposition content

- **Tier:** Senior
- **Budget:** L
- **Group:** G1
- **Parallel:** yes (with TASK-2)
- **Depends On:** none

**File:** `resources/skills-templates/core/lib/x-lib-task-decomposer/SKILL.md`

**Scope:**

1. Update YAML frontmatter `description` field to reflect dual-mode capability:
   ```
   description: "Decomposes an implementation plan into tasks. Primary mode: derives tasks from test scenarios (x-test-plan output) using TDD structure (RED/GREEN/REFACTOR). Fallback mode: uses Layer Task Catalog (G1-G7) when no test plan exists."
   ```
2. Update title from `# Skill: Task Decomposer (Layer-Based)` to `# Skill: Task Decomposer (Test-Driven + Layer Fallback)`
3. Update Purpose section to describe dual-mode behavior
4. Add test plan as optional input (Input #3)
5. Insert **STEP 1.5 -- Detect Decomposition Mode** between STEP 1 and STEP 2
6. Insert **STEP 2A -- Test-Driven Decomposition (Primary Mode)** with:
   - Task structure table (TDD fields: Test Scenario, TPP Level, RED, GREEN, REFACTOR, Layer Components, Parallel, Depends On, Tier, Budget)
   - Parallelism detection rules
   - Ordering rules (TPP level, inner-before-outer, AT-after-UT)
   - Task type classification table (UT, AT, IT markers and dependency rules)
7. Add **TDD Task Output Format** section with template
8. Relabel existing STEP 2-4 as STEP 2B-4B under Layer-Based Mode path
9. Wrap entire Layer Task Catalog under **Fallback: Layer Task Catalog (G1-G7)** heading with usage note
10. Update Integration Notes to reference test plan consumption and dual-mode behavior
11. Preserve all existing content: Context Budget Sizes, Review Tier Assignment, Escalation Rules, Layer Dependency Graph

**Acceptance Criteria:**
- Three-level fallback strategy documented (TPP markers present, file without TPP, file absent)
- G1-G7 catalog fully preserved under Fallback heading
- All existing sections (budgets, tiers, escalation) retained unchanged
- STEP numbering is consistent: 0, 1, 1.5, 2A, 2B, 3B, 4B, 5

---

### TASK-2: Modify GitHub source template with test-driven decomposition content

- **Tier:** Mid
- **Budget:** M
- **Group:** G1
- **Parallel:** yes (with TASK-1)
- **Depends On:** none

**File:** `resources/github-skills-templates/lib/x-lib-task-decomposer.md`

**Scope:**

1. Update YAML frontmatter `description` field (multi-line `>` format) to reflect dual-mode capability, including `Reference:` line
2. Update title from `# Skill: Task Decomposer (Layer-Based)` to `# Skill: Task Decomposer (Test-Driven + Layer Fallback)`
3. Update Purpose section to match Claude copy
4. Add test plan as optional input
5. Insert STEP 1.5, STEP 2A (abbreviated for GitHub -- key tables and rules, not full detail)
6. Relabel STEP 2-4 as 2B-4B under Layer-Based Mode
7. Wrap Layer Task Catalog under Fallback heading
8. Update Integration Notes with `Reference:` pointer
9. All architecture references use `.github/skills/...` paths (NOT `.claude/skills/...`)

**Key Differences from TASK-1:**
- Frontmatter uses `>` multi-line format with `Reference:` line
- Architecture path: `.github/skills/architecture/SKILL.md`
- Layer templates path: `.github/skills/layer-templates/SKILL.md`
- Content is abbreviated (no Context Budget Sizes, Review Tier Assignment, Escalation Rules sections -- these are only in the Claude copy)

**Acceptance Criteria:**
- GitHub-specific path references throughout
- Abbreviated format consistent with current GitHub template style
- `Reference:` pointer line in frontmatter and Integration Notes

---

## G2: GOLDEN FILE UPDATES

> **Parallel within group:** Yes (TASK-3, TASK-4, and TASK-5 can run in parallel)
> **Dependencies:** G1 (TASK-1 for TASK-3/TASK-4, TASK-2 for TASK-5)

### TASK-3: Update 8 `.claude` golden files (all profiles)

- **Tier:** Junior
- **Budget:** S
- **Group:** G2
- **Parallel:** yes (with TASK-4, TASK-5)
- **Depends On:** TASK-1

**Files (8):**
- `tests/golden/go-gin/.claude/skills/lib/x-lib-task-decomposer/SKILL.md`
- `tests/golden/java-quarkus/.claude/skills/lib/x-lib-task-decomposer/SKILL.md`
- `tests/golden/java-spring/.claude/skills/lib/x-lib-task-decomposer/SKILL.md`
- `tests/golden/kotlin-ktor/.claude/skills/lib/x-lib-task-decomposer/SKILL.md`
- `tests/golden/python-click-cli/.claude/skills/lib/x-lib-task-decomposer/SKILL.md`
- `tests/golden/python-fastapi/.claude/skills/lib/x-lib-task-decomposer/SKILL.md`
- `tests/golden/rust-axum/.claude/skills/lib/x-lib-task-decomposer/SKILL.md`
- `tests/golden/typescript-nestjs/.claude/skills/lib/x-lib-task-decomposer/SKILL.md`

**Scope:**
Copy the final content of `resources/skills-templates/core/lib/x-lib-task-decomposer/SKILL.md` verbatim to all 8 files. No template variables exist in this skill, so content is identical across all profiles.

**Acceptance Criteria:**
- All 8 files are byte-for-byte identical to the source template

---

### TASK-4: Update 8 `.agents` golden files (all profiles)

- **Tier:** Junior
- **Budget:** S
- **Group:** G2
- **Parallel:** yes (with TASK-3, TASK-5)
- **Depends On:** TASK-1

**Files (8):**
- `tests/golden/go-gin/.agents/skills/lib/x-lib-task-decomposer/SKILL.md`
- `tests/golden/java-quarkus/.agents/skills/lib/x-lib-task-decomposer/SKILL.md`
- `tests/golden/java-spring/.agents/skills/lib/x-lib-task-decomposer/SKILL.md`
- `tests/golden/kotlin-ktor/.agents/skills/lib/x-lib-task-decomposer/SKILL.md`
- `tests/golden/python-click-cli/.agents/skills/lib/x-lib-task-decomposer/SKILL.md`
- `tests/golden/python-fastapi/.agents/skills/lib/x-lib-task-decomposer/SKILL.md`
- `tests/golden/rust-axum/.agents/skills/lib/x-lib-task-decomposer/SKILL.md`
- `tests/golden/typescript-nestjs/.agents/skills/lib/x-lib-task-decomposer/SKILL.md`

**Scope:**
Copy the final content of `resources/skills-templates/core/lib/x-lib-task-decomposer/SKILL.md` verbatim to all 8 files. Content is identical to `.claude` golden files (same source template).

**Acceptance Criteria:**
- All 8 files are byte-for-byte identical to the source template and to the `.claude` golden files

---

### TASK-5: Update 8 `.github` golden files (all profiles)

- **Tier:** Junior
- **Budget:** S
- **Group:** G2
- **Parallel:** yes (with TASK-3, TASK-4)
- **Depends On:** TASK-2

**Files (8):**
- `tests/golden/go-gin/.github/skills/lib/x-lib-task-decomposer/SKILL.md`
- `tests/golden/java-quarkus/.github/skills/lib/x-lib-task-decomposer/SKILL.md`
- `tests/golden/java-spring/.github/skills/lib/x-lib-task-decomposer/SKILL.md`
- `tests/golden/kotlin-ktor/.github/skills/lib/x-lib-task-decomposer/SKILL.md`
- `tests/golden/python-click-cli/.github/skills/lib/x-lib-task-decomposer/SKILL.md`
- `tests/golden/python-fastapi/.github/skills/lib/x-lib-task-decomposer/SKILL.md`
- `tests/golden/rust-axum/.github/skills/lib/x-lib-task-decomposer/SKILL.md`
- `tests/golden/typescript-nestjs/.github/skills/lib/x-lib-task-decomposer/SKILL.md`

**Scope:**
Copy the final content of `resources/github-skills-templates/lib/x-lib-task-decomposer.md` verbatim to all 8 files. No template variables exist, so content is identical across all profiles.

**Acceptance Criteria:**
- All 8 files are byte-for-byte identical to the GitHub source template

---

## G3: VERIFICATION

> **Parallel within group:** No (TASK-6 runs first, TASK-7 validates its output)
> **Dependencies:** G2 (all golden files updated)

### TASK-6: Run full test suite -- validate byte-for-byte parity

- **Tier:** Junior
- **Budget:** S
- **Group:** G3
- **Parallel:** no
- **Depends On:** TASK-3, TASK-4, TASK-5

**Command:** `npm test`

**Scope:**
Run the complete test suite (1,384+ tests across 46 files). The byte-for-byte integration test (`tests/node/integration/byte-for-byte.test.ts`) will automatically validate that all 24 golden files match the pipeline output. Any mismatch triggers a test failure.

**Acceptance Criteria:**
- All tests pass (0 failures)
- No regressions in existing test suites
- byte-for-byte integration test validates all 24 updated golden files

---

### TASK-7: Verify coverage unchanged

- **Tier:** Junior
- **Budget:** S
- **Group:** G3
- **Parallel:** no
- **Depends On:** TASK-6

**Scope:**
Verify that code coverage remains at or above quality gate thresholds. Since this story modifies only Markdown template files (no TypeScript changes), coverage should remain unchanged at approximately 99.6% lines, 97.84% branches.

**Acceptance Criteria:**
- Line coverage >= 95% (quality gate)
- Branch coverage >= 90% (quality gate)
- No decrease from baseline (99.6% lines, 97.84% branches)

---

## Dependency Graph

```
TASK-1 (Claude template) ──┬──> TASK-3 (.claude golden) ──┐
                           ├──> TASK-4 (.agents golden) ──┤
TASK-2 (GitHub template) ──┴──> TASK-5 (.github golden) ──┴──> TASK-6 (test suite) ──> TASK-7 (coverage)
```

## Execution Summary

| Task | Group | Tier | Budget | Parallel | Depends On | Files Changed |
|------|-------|------|--------|----------|------------|---------------|
| TASK-1 | G1 | Senior | L | yes | none | 1 |
| TASK-2 | G1 | Mid | M | yes | none | 1 |
| TASK-3 | G2 | Junior | S | yes | TASK-1 | 8 |
| TASK-4 | G2 | Junior | S | yes | TASK-1 | 8 |
| TASK-5 | G2 | Junior | S | yes | TASK-2 | 8 |
| TASK-6 | G3 | Junior | S | no | TASK-3,4,5 | 0 |
| TASK-7 | G3 | Junior | S | no | TASK-6 | 0 |

**Total files modified:** 26 (2 source templates + 24 golden files)
**Total TypeScript changes:** 0
**Total new tests:** 0 (existing byte-for-byte test covers all changes)
