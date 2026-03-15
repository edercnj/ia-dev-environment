# Task Decomposition: STORY-0003-0013 -- x-git-push TDD Commit Format Documentation

## Overview

Documentation-only change: add 3 new Markdown sections to the `x-git-push` skill template (TDD Commit Format, Atomic TDD Commit Rules, Git History Storytelling). No TypeScript source code is modified.

**Total affected files:** 26 (2 source templates + 24 golden files)

---

## G1: Foundation -- Read and Understand Current Skill Content

**Dependencies:** None
**Purpose:** Verify current structure and identify exact insertion points.

### G1-T1: Read Claude source template

- **File:** `resources/skills-templates/core/x-git-push/SKILL.md`
- **Action:** Read and confirm structure. The file has 161 lines. The 3 new sections must be inserted between `## Tagging Releases` (ends at line 154) and `## Integration Notes` (starts at line 156).
- **Key observation:** File has YAML frontmatter (lines 1-6) with `allowed-tools` and `argument-hint`, plus a `## Global Output Policy` block (lines 8-13) that is unique to the Claude template.

### G1-T2: Read GitHub source template

- **File:** `resources/github-skills-templates/git-troubleshooting/x-git-push.md`
- **Action:** Read and confirm structure. The file has 157 lines. The 3 new sections must be inserted between `## Tagging Releases` (ends at line 150) and `## Integration Notes` (starts at line 152).
- **Key observation:** File has different YAML frontmatter (multi-line `description:` with `Reference:` line, no `allowed-tools`/`argument-hint`). No `## Global Output Policy` block. Has `- Reference: .github/skills/x-git-push/SKILL.md` as last line of Integration Notes.

### G1-T3: Verify golden file identity

- **Action:** Confirm that all 24 golden files are byte-identical copies of their respective source templates (no placeholder substitution occurs for `x-git-push`).
- **Verified:** All 8 `.claude/` golden files share hash `6cc0b7d28a31568b42bd07fa57f6533d` (identical to source template). All 8 `.agents/` golden files share the same hash. All 8 `.github/` golden files share hash `62fd95e480aff07789776c41b0c98712` (identical to GitHub source template).
- **Implication:** Golden files can be updated by copying the modified source template to all 8 profile directories per category. No pipeline run is required (but running the pipeline + tests validates correctness).

### G1-T4: Confirm RULE-003 -- catalog existing Conventional Commits content

- **Action:** Verify the following existing content that MUST NOT be modified:
  - `## Commit Convention (Conventional Commits)` section (Format, Types table, Scopes, Rules)
  - 8 commit types: feat, test, fix, refactor, docs, build, chore, infra
  - 4 commit rules (atomic commits, subject line, body, tests with features)
  - All branch naming, workflow, PR, and tagging content
- **Rule:** TDD content is ADDITIVE. `[TDD]` tags are suffixes to the existing `<type>(<scope>): <subject>` format.

---

## G2: Core Changes -- Edit Claude Source Template

**Dependencies:** G1 completed
**File:** `resources/skills-templates/core/x-git-push/SKILL.md`

### G2-T1: Insert `## TDD Commit Format` section

- **Insertion point:** After line 154 (`git push origin v0.1.0` closing), before `## Integration Notes`
- **Content:** Table with 4 TDD commit formats:

| Format | Phase | Usage |
|--------|-------|-------|
| `test(scope): add test for [behavior] [TDD:RED]` | Red | Failing test only (no implementation) |
| `feat(scope): implement [behavior] [TDD:GREEN]` | Green | Minimal implementation to pass test |
| `refactor(scope): [improvement] [TDD:REFACTOR]` | Refactor | Restructuring, no new behavior |
| `feat(scope): implement [behavior] [TDD]` | Combined | Test + implementation in one commit (**recommended**) |

- **Note:** The combined `[TDD]` format must be marked as the recommended default. Individual phase tags are optional alternatives for finer-grained history.

### G2-T2: Insert `## Atomic TDD Commit Rules` section

- **Insertion point:** Immediately after `## TDD Commit Format` section
- **Content:** Numbered list with 5 rules:
  1. One commit per complete Red-Green-Refactor cycle
  2. Test and implementation in the SAME commit (avoid separate test-only commits)
  3. Refactoring MAY be a separate commit (immediately after green)
  4. Each commit adds ONE testable behavior
  5. Maximum ~50 lines changed per commit; larger changes should be split

### G2-T3: Insert `## Git History Storytelling` section

- **Insertion point:** Immediately after `## Atomic TDD Commit Rules` section, before `## Integration Notes`
- **Content:** Guideline for commit sequence:
  - First commit: acceptance test + test infrastructure setup
  - Subsequent commits: incremental unit tests following TPP (Transformation Priority Premise) order
  - Final commits: refactoring and polish
  - The git log should read as a progression from simple to complex

### G2-T4: Verify no existing content was modified

- **Command:** `git diff resources/skills-templates/core/x-git-push/SKILL.md`
- **Expected:** Only additions (lines prefixed with `+`), no deletions or modifications to existing lines.

---

## G3: Dual Copy -- Edit GitHub Source Template

**Dependencies:** G2 completed (use G2 content as reference for identical body)
**File:** `resources/github-skills-templates/git-troubleshooting/x-git-push.md`

### G3-T1: Insert identical 3 sections into GitHub template

- **Insertion point:** After line 150 (`git push origin v0.1.0` closing), before `## Integration Notes` (which starts at line 152)
- **Content:** Byte-identical body content as G2 (same 3 sections: TDD Commit Format, Atomic TDD Commit Rules, Git History Storytelling)
- **Do NOT modify:** YAML frontmatter (lines 1-8), or the `- Reference:` line in Integration Notes

### G3-T2: Verify RULE-001 dual copy consistency

- **Command:** Diff the body content of both templates (excluding frontmatter, `## Global Output Policy` block, and `- Reference:` line). The new TDD sections must be byte-identical.
- **How to verify:** Strip the known differing sections and diff the rest:
  ```bash
  # Quick visual check: the 3 new sections should appear identically in both files
  diff <(sed -n '/^## TDD Commit Format$/,/^## Integration Notes$/p' resources/skills-templates/core/x-git-push/SKILL.md) \
       <(sed -n '/^## TDD Commit Format$/,/^## Integration Notes$/p' resources/github-skills-templates/git-troubleshooting/x-git-push.md)
  ```
- **Expected:** No differences in the new sections.

---

## G4: Golden Files -- Update All 24 Golden Files

**Dependencies:** G2 and G3 completed
**Strategy:** Since golden files are byte-identical copies of source templates (no placeholder substitution for x-git-push), copy the modified source templates directly.

### G4-T1: Update 8 `.claude/` golden files

- **Source:** `resources/skills-templates/core/x-git-push/SKILL.md`
- **Targets (8 files):**
  - `tests/golden/go-gin/.claude/skills/x-git-push/SKILL.md`
  - `tests/golden/java-quarkus/.claude/skills/x-git-push/SKILL.md`
  - `tests/golden/java-spring/.claude/skills/x-git-push/SKILL.md`
  - `tests/golden/kotlin-ktor/.claude/skills/x-git-push/SKILL.md`
  - `tests/golden/python-click-cli/.claude/skills/x-git-push/SKILL.md`
  - `tests/golden/python-fastapi/.claude/skills/x-git-push/SKILL.md`
  - `tests/golden/rust-axum/.claude/skills/x-git-push/SKILL.md`
  - `tests/golden/typescript-nestjs/.claude/skills/x-git-push/SKILL.md`
- **Command:**
  ```bash
  for profile in go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs; do
    cp resources/skills-templates/core/x-git-push/SKILL.md tests/golden/$profile/.claude/skills/x-git-push/SKILL.md
  done
  ```

### G4-T2: Update 8 `.agents/` golden files

- **Source:** `resources/skills-templates/core/x-git-push/SKILL.md` (`.agents/` mirrors `.claude/`)
- **Targets (8 files):**
  - `tests/golden/go-gin/.agents/skills/x-git-push/SKILL.md`
  - `tests/golden/java-quarkus/.agents/skills/x-git-push/SKILL.md`
  - `tests/golden/java-spring/.agents/skills/x-git-push/SKILL.md`
  - `tests/golden/kotlin-ktor/.agents/skills/x-git-push/SKILL.md`
  - `tests/golden/python-click-cli/.agents/skills/x-git-push/SKILL.md`
  - `tests/golden/python-fastapi/.agents/skills/x-git-push/SKILL.md`
  - `tests/golden/rust-axum/.agents/skills/x-git-push/SKILL.md`
  - `tests/golden/typescript-nestjs/.agents/skills/x-git-push/SKILL.md`
- **Command:**
  ```bash
  for profile in go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs; do
    cp resources/skills-templates/core/x-git-push/SKILL.md tests/golden/$profile/.agents/skills/x-git-push/SKILL.md
  done
  ```

### G4-T3: Update 8 `.github/` golden files

- **Source:** `resources/github-skills-templates/git-troubleshooting/x-git-push.md`
- **Targets (8 files):**
  - `tests/golden/go-gin/.github/skills/x-git-push/SKILL.md`
  - `tests/golden/java-quarkus/.github/skills/x-git-push/SKILL.md`
  - `tests/golden/java-spring/.github/skills/x-git-push/SKILL.md`
  - `tests/golden/kotlin-ktor/.github/skills/x-git-push/SKILL.md`
  - `tests/golden/python-click-cli/.github/skills/x-git-push/SKILL.md`
  - `tests/golden/python-fastapi/.github/skills/x-git-push/SKILL.md`
  - `tests/golden/rust-axum/.github/skills/x-git-push/SKILL.md`
  - `tests/golden/typescript-nestjs/.github/skills/x-git-push/SKILL.md`
- **Command:**
  ```bash
  for profile in go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs; do
    cp resources/github-skills-templates/git-troubleshooting/x-git-push.md tests/golden/$profile/.github/skills/x-git-push/SKILL.md
  done
  ```

### G4-T4: Verify all golden files match sources

- **Command:**
  ```bash
  md5 resources/skills-templates/core/x-git-push/SKILL.md tests/golden/*/.claude/skills/x-git-push/SKILL.md tests/golden/*/.agents/skills/x-git-push/SKILL.md
  md5 resources/github-skills-templates/git-troubleshooting/x-git-push.md tests/golden/*/.github/skills/x-git-push/SKILL.md
  ```
- **Expected:** All `.claude/` and `.agents/` golden files share the same hash as the Claude source template. All `.github/` golden files share the same hash as the GitHub source template.

---

## G5: Verification -- Run Tests

**Dependencies:** G4 completed

### G5-T1: Run TypeScript compilation check

- **Command:** `npx tsc --noEmit`
- **Expected:** Zero errors. No TypeScript files were modified, so this is a sanity check.

### G5-T2: Run byte-for-byte integration tests

- **Command:** `npx vitest run tests/node/integration/byte-for-byte.test.ts`
- **Expected:** All 8 profiles pass `pipelineMatchesGoldenFiles` assertions.

### G5-T3: Run full test suite with coverage

- **Command:** `npx vitest run --coverage`
- **Expected:** All 1,384+ tests pass. Coverage >= 95% line, >= 90% branch (unchanged since no TypeScript code was modified).

### G5-T4: Final git diff review

- **Command:** `git diff --stat`
- **Expected:** Exactly 26 files changed (2 source templates + 24 golden files). All changes are additions only (no deletions in existing content).

---

## G6: Not Applicable

Skipped. No database, API, event, or infrastructure changes.

---

## G7: Not Applicable

Skipped. No deployment or configuration changes.

---

## Dependency Graph

```
G1 (Foundation: read & understand)
 |
 v
G2 (Core: edit Claude source template)
 |
 v
G3 (Dual Copy: edit GitHub source template, using G2 content)
 |
 v
G4 (Golden Files: copy sources to 24 golden files)
 |
 v
G5 (Verification: compile, test, coverage)
```

All groups are sequential. No parallelism between groups. Within G4, the 3 subtasks (T1-T3) can run in parallel.

---

## Risk Mitigations

| Risk | Mitigation |
|------|------------|
| Existing content accidentally modified | G2-T4: verify `git diff` shows only additions |
| Dual copy inconsistency | G3-T2: diff the new sections between both templates |
| Golden file mismatch | G4-T4: md5 hash verification before running tests |
| Placeholder breakage | N/A: new sections contain no `{{...}}` placeholders |

---

## Summary Table

| Group | Tasks | Files Modified | Estimated Effort |
|-------|-------|---------------|-----------------|
| G1 | 4 (read-only) | 0 | Research |
| G2 | 4 | 1 source template | Small |
| G3 | 2 | 1 source template | Small (copy from G2) |
| G4 | 4 | 24 golden files | Mechanical (cp command) |
| G5 | 4 | 0 | Automated (tests) |
| **Total** | **18** | **26** | **Small** |
