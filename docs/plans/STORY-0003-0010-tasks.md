# Task Decomposition -- STORY-0003-0010: x-story-epic -- DoD with TDD Criteria

**Status:** PENDING
**Date:** 2026-03-15
**Story:** story-0003-0010
**Blocked By:** story-0003-0005 (Templates with TDD sections -- DONE)
**Blocks:** story-0003-0011

---

## G1 -- Source Template Update (Claude Copy)

**Purpose:** Modify the Claude/Agents source template to add TDD-related cross-cutting rules extraction guidance in Step 2 and TDD DoD items in Step 4. All changes are additive (RULE-003: Backward Compatibility).
**Dependencies:** None (story-0003-0005 already delivered)
**Compiles independently:** N/A -- markdown template file, no TypeScript changes.

### T1.1 -- Add TDD rules extraction guidance to Step 2

- **File:** `resources/skills-templates/core/x-story-epic/SKILL.md` (modify)
- **What to implement:**
  - Append new content **after** the existing "What stays in individual stories" block and after the paragraph ending with "when rules can conflict." (line 67)
  - New subsection: "**TDD-related cross-cutting rules (extract when applicable):**"
  - Content describes when and how to extract TDD cross-cutting rules:
    - **Red-Green-Refactor**: All production code follows the Red-Green-Refactor cycle. Test must fail first (RED), then minimum code to pass (GREEN), then design improvement (REFACTOR). Refactoring never adds behavior.
    - **Atomic TDD Commits**: Each Red-Green-Refactor cycle produces one or more atomic commits following Conventional Commits format. Test commit precedes or accompanies implementation commit.
    - **Gherkin Completeness**: Every user-facing behavior has a corresponding Gherkin scenario. Acceptance tests are derived from Gherkin and drive the outer TDD loop.
  - Include guidance: these rules appear in the Rules table with unique IDs (RULE-NNN) alongside other cross-cutting rules, only when spec or project context justifies them.
- **Preservation check:** All content before this insertion point must remain character-for-character identical. Verify with diff.
- **Dependencies on other tasks:** None
- **Estimated complexity:** S

### T1.2 -- Add TDD DoD items to Step 4

- **File:** `resources/skills-templates/core/x-story-epic/SKILL.md` (modify)
- **What to implement:**
  - Append two new bullet items to the "**Global Definition of Done (DoD):**" list in Step 4, after the existing item "Persistence/data integrity criteria" (line 102)
  - New items:
    - **TDD Compliance**: Commits show test-first pattern (test precedes implementation in git log). Explicit refactoring step after green. Tests are incremental, progressing from simple to complex following the Transformation Priority Premise (TPP).
    - **Double-Loop TDD**: Acceptance tests derived from Gherkin scenarios (outer loop). Unit tests guided by the Transformation Priority Premise (inner loop).
  - These are ADDITIONAL items -- all existing DoD items (coverage, test types, documentation, SLOs, persistence) are preserved.
- **Preservation check:** Existing 5 DoD items must remain unchanged. The two new items are appended after the last existing item.
- **Dependencies on other tasks:** None (can be done in parallel with T1.1)
- **Estimated complexity:** XS

### Verification checkpoint G1

```bash
# Verify template is well-formed markdown
head -12 resources/skills-templates/core/x-story-epic/SKILL.md  # YAML frontmatter intact
# Verify Step 2 contains TDD rules extraction guidance
grep 'TDD-related cross-cutting rules' resources/skills-templates/core/x-story-epic/SKILL.md
grep 'Red-Green-Refactor' resources/skills-templates/core/x-story-epic/SKILL.md
grep 'Atomic TDD Commits' resources/skills-templates/core/x-story-epic/SKILL.md
grep 'Gherkin Completeness' resources/skills-templates/core/x-story-epic/SKILL.md
# Verify Step 4 contains TDD DoD items
grep 'TDD Compliance' resources/skills-templates/core/x-story-epic/SKILL.md
grep 'Double-Loop TDD' resources/skills-templates/core/x-story-epic/SKILL.md
# Verify existing content preserved (Step 1, Step 3, Step 5, Step 6 unchanged)
git diff -- resources/skills-templates/core/x-story-epic/SKILL.md
```

---

## G2 -- Source Template Update (GitHub Copy)

**Purpose:** Mirror the TDD changes from G1 into the GitHub skills template, maintaining RULE-001 (Dual Copy Consistency). The GitHub copy has minor structural differences (English section headers in Step 5, different prerequisite paths, "Detailed References" section at end) but Step 2 and Step 4 are structurally identical to the Claude copy.
**Dependencies:** G1 (Claude template must be finalized to mirror from)
**Compiles independently:** N/A -- markdown template file, no TypeScript changes.

### T2.1 -- Add TDD rules extraction guidance to Step 2

- **File:** `resources/github-skills-templates/story/x-story-epic.md` (modify)
- **What to implement:**
  - Append the same TDD rules extraction subsection as T1.1, placed after the "when rules can conflict." paragraph (line 67)
  - Content is identical to Claude copy (no language adaptation needed -- new content uses English technical terms)
- **Key constraint:** The insertion point in the GitHub copy matches the Claude copy structurally. Lines 50-67 of both files are identical.
- **Dependencies on other tasks:** T1.1
- **Estimated complexity:** XS

### T2.2 -- Add TDD DoD items to Step 4

- **File:** `resources/github-skills-templates/story/x-story-epic.md` (modify)
- **What to implement:**
  - Append the same two TDD DoD items as T1.2, after "Persistence/data integrity criteria" (line 102)
  - Content is identical to Claude copy
- **Dependencies on other tasks:** T1.2
- **Estimated complexity:** XS

### T2.3 -- Verify structural parity with Claude copy

- **Action:** Diff the modified sections (Step 2 and Step 4) between Claude and GitHub copies.
- **Expected differences (pre-existing, not introduced by this story):**
  - Prerequisites section: different paths (`.claude/templates/` vs `resources/templates/`, `.claude/skills/` vs `.github/skills/`)
  - Step 3: different decomposition guide reference path
  - Step 5: English section headers ("Overview" vs "Visao Geral", "Attachments and References" vs "Anexos e Referencias")
  - End of file: GitHub copy has "Detailed References" section
  - Common Mistakes: minor language differences ("Validar entidade" vs "Validate entity")
- **New content (Steps 2 and 4) must be identical between both copies.**
- **Dependencies on other tasks:** T2.1, T2.2
- **Estimated complexity:** XS

### Verification checkpoint G2

```bash
# Verify GitHub template contains TDD content
grep 'TDD-related cross-cutting rules' resources/github-skills-templates/story/x-story-epic.md
grep 'TDD Compliance' resources/github-skills-templates/story/x-story-epic.md
grep 'Double-Loop TDD' resources/github-skills-templates/story/x-story-epic.md
# Verify no placeholders were introduced (this skill has no profile-specific placeholders)
grep -c '{language_name}' resources/github-skills-templates/story/x-story-epic.md  # expect: 0
```

---

## G3 -- Golden Files Update (.claude/)

**Purpose:** Copy the updated Claude template to all 8 `.claude/` golden file paths. The `.claude/` golden files contain the template as-is (no placeholder resolution). All 8 profiles receive identical content.
**Dependencies:** G1 (Claude template must be finalized)
**Compiles independently:** N/A -- markdown files.

### T3.1 -- Copy template to all 8 `.claude/` golden paths

- **Source:** `resources/skills-templates/core/x-story-epic/SKILL.md`
- **Destinations (8 files):**
  1. `tests/golden/go-gin/.claude/skills/x-story-epic/SKILL.md`
  2. `tests/golden/java-quarkus/.claude/skills/x-story-epic/SKILL.md`
  3. `tests/golden/java-spring/.claude/skills/x-story-epic/SKILL.md`
  4. `tests/golden/kotlin-ktor/.claude/skills/x-story-epic/SKILL.md`
  5. `tests/golden/python-click-cli/.claude/skills/x-story-epic/SKILL.md`
  6. `tests/golden/python-fastapi/.claude/skills/x-story-epic/SKILL.md`
  7. `tests/golden/rust-axum/.claude/skills/x-story-epic/SKILL.md`
  8. `tests/golden/typescript-nestjs/.claude/skills/x-story-epic/SKILL.md`
- **Method:** Byte-for-byte copy (`cp`). All 8 files are identical.
- **Verification:**
  ```bash
  for p in go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs; do
    diff resources/skills-templates/core/x-story-epic/SKILL.md \
         tests/golden/$p/.claude/skills/x-story-epic/SKILL.md
  done
  # Expect: zero output (all identical)
  ```
- **Dependencies on other tasks:** T1.1, T1.2
- **Estimated complexity:** XS

---

## G4 -- Golden Files Update (.agents/)

**Purpose:** Copy the `.claude/` golden files to all 8 `.agents/` golden file paths. The `.agents/` copies must be byte-for-byte identical to `.claude/` copies (produced by `CodexSkillsAssembler` mirroring mechanism).
**Dependencies:** G3 (`.claude/` golden files must be updated first)
**Compiles independently:** N/A -- markdown files.

### T4.1 -- Copy `.claude/` to all 8 `.agents/` golden paths

- **Source:** The source template (or any `.claude/` golden file -- all are identical).
- **Destinations (8 files):**
  1. `tests/golden/go-gin/.agents/skills/x-story-epic/SKILL.md`
  2. `tests/golden/java-quarkus/.agents/skills/x-story-epic/SKILL.md`
  3. `tests/golden/java-spring/.agents/skills/x-story-epic/SKILL.md`
  4. `tests/golden/kotlin-ktor/.agents/skills/x-story-epic/SKILL.md`
  5. `tests/golden/python-click-cli/.agents/skills/x-story-epic/SKILL.md`
  6. `tests/golden/python-fastapi/.agents/skills/x-story-epic/SKILL.md`
  7. `tests/golden/rust-axum/.agents/skills/x-story-epic/SKILL.md`
  8. `tests/golden/typescript-nestjs/.agents/skills/x-story-epic/SKILL.md`
- **Method:** Byte-for-byte copy. All 8 files identical to `.claude/` counterparts.
- **Verification:**
  ```bash
  for p in go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs; do
    diff tests/golden/$p/.claude/skills/x-story-epic/SKILL.md \
         tests/golden/$p/.agents/skills/x-story-epic/SKILL.md
  done
  # Expect: zero output (all identical)
  ```
- **Dependencies on other tasks:** T3.1
- **Estimated complexity:** XS

---

## G5 -- Golden Files Update (.github/)

**Purpose:** Copy the updated GitHub template to all 8 `.github/` golden file paths. Verified: the x-story-epic skill has NO profile-specific placeholders (`{language_name}` does not appear). All 8 `.github/` golden files are byte-for-byte identical.
**Dependencies:** G2 (GitHub template must be finalized)
**Compiles independently:** N/A -- markdown files.

### T5.1 -- Copy template to all 8 `.github/` golden paths

- **Source:** `resources/github-skills-templates/story/x-story-epic.md`
- **Destinations (8 files):**
  1. `tests/golden/go-gin/.github/skills/x-story-epic/SKILL.md`
  2. `tests/golden/java-quarkus/.github/skills/x-story-epic/SKILL.md`
  3. `tests/golden/java-spring/.github/skills/x-story-epic/SKILL.md`
  4. `tests/golden/kotlin-ktor/.github/skills/x-story-epic/SKILL.md`
  5. `tests/golden/python-click-cli/.github/skills/x-story-epic/SKILL.md`
  6. `tests/golden/python-fastapi/.github/skills/x-story-epic/SKILL.md`
  7. `tests/golden/rust-axum/.github/skills/x-story-epic/SKILL.md`
  8. `tests/golden/typescript-nestjs/.github/skills/x-story-epic/SKILL.md`
- **Method:** Byte-for-byte copy (`cp`). All 8 files are identical (no placeholder resolution needed for this skill).
- **Note:** Unlike other skills (e.g., x-test-plan) that resolve `{language_name}`, the x-story-epic GitHub template contains NO placeholders. The `GithubSkillsAssembler` placeholder resolution is a no-op for this file.
- **Verification:**
  ```bash
  for p in go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs; do
    diff resources/github-skills-templates/story/x-story-epic.md \
         tests/golden/$p/.github/skills/x-story-epic/SKILL.md
  done
  # Expect: zero output (all identical to source template)
  ```
- **Dependencies on other tasks:** T2.1, T2.2
- **Estimated complexity:** XS

---

## G6 -- Verification

**Purpose:** Run compilation check, full test suite, and verify coverage thresholds. Since this story modifies 0 TypeScript files, compilation should be a no-op pass. The byte-for-byte integration tests validate all 26 golden files automatically.
**Dependencies:** G3, G4, G5 (all 24 golden files must be updated)

### T6.1 -- TypeScript compilation check

- **Command:** `npx tsc --noEmit`
- **Expected:** Zero errors. No TypeScript files were modified.
- **Dependencies on other tasks:** G5
- **Estimated complexity:** XS

### T6.2 -- Run byte-for-byte parity tests

- **Command:** `npx vitest run tests/node/integration/byte-for-byte.test.ts`
- **Expected:** All 8 profiles pass. The `byte-for-byte.test.ts` integration test runs the pipeline for each profile and compares output against golden files.
- **Test mechanism:** `tests/node/integration/byte-for-byte.test.ts` uses `describe.sequential.each` over 8 profiles. For each profile:
  1. Loads config from `resources/config-templates/setup-config.{profile}.yaml`
  2. Runs `runPipeline()` to a temp directory
  3. Runs `verifyOutput()` comparing pipeline output against `tests/golden/{profile}/`
  4. Asserts: `pipelineSuccessForProfile`, `pipelineMatchesGoldenFiles`, `noMissingFiles`, `noExtraFiles`, `totalFilesGreaterThanZero`
- **What this validates:**
  - `.claude/skills/x-story-epic/SKILL.md` -- copied from source template, must match golden
  - `.agents/skills/x-story-epic/SKILL.md` -- mirrored from `.claude/`, must match golden
  - `.github/skills/x-story-epic/SKILL.md` -- rendered from GitHub template (no placeholder resolution for this skill), must match golden
- **Dependencies on other tasks:** T6.1
- **Estimated complexity:** S

### T6.3 -- Run full test suite

- **Command:** `npm test`
- **Expected:** All 1,384+ tests pass. Zero regressions.
- **Dependencies on other tasks:** T6.2
- **Estimated complexity:** S

### T6.4 -- Verify coverage thresholds

- **Command:** `npm test -- --coverage` (or equivalent)
- **Expected:** >= 95% line coverage, >= 90% branch coverage.
- **Note:** Since no TypeScript code was modified, coverage should remain at the existing level (~99.6% lines, ~97.84% branches).
- **Dependencies on other tasks:** T6.3
- **Estimated complexity:** XS

### T6.5 -- Acceptance criteria verification

- **What to verify against story DoD:**
  - [ ] x-story-epic generates DoD global with TDD Compliance -- verified by reading updated Step 4
  - [ ] x-story-epic generates DoD global with Double-Loop TDD -- verified by reading updated Step 4
  - [ ] x-story-epic extracts TDD cross-cutting rules when applicable -- verified by reading updated Step 2
  - [ ] Both copies updated (RULE-001) -- verified by comparing Claude and GitHub templates
  - [ ] Golden file tests updated and passing -- verified by T6.2
  - [ ] Coverage >= 95% line, >= 90% branch -- verified by T6.4
  - [ ] Backward compatibility: existing DoD items preserved (RULE-003) -- verified by diff showing additive-only changes
  - [ ] `.claude/` and `.agents/` byte-for-byte parity across all 8 profiles -- verified by diff
- **Dependencies on other tasks:** T6.1, T6.2, T6.3, T6.4
- **Estimated complexity:** XS

---

## Summary Table

| Group | Purpose | Files to Create | Files to Modify | Tasks | Complexity |
|-------|---------|----------------|----------------|-------|------------|
| G1 | Source Template (Claude Copy) | 0 | 1 template | 2 | XS-S |
| G2 | Source Template (GitHub Copy) | 0 | 1 template | 3 | XS |
| G3 | Golden Files (.claude/) | 0 | 8 golden files | 1 | XS |
| G4 | Golden Files (.agents/) | 0 | 8 golden files | 1 | XS |
| G5 | Golden Files (.github/) | 0 | 8 golden files | 1 | XS |
| G6 | Verification | 0 | 0 | 5 | XS-S |
| **Total** | | **0 new files** | **26 modified files** | **13 tasks** | |

## Dependency Graph

```
G1: SOURCE TEMPLATE (Claude copy -- modify Step 2 + Step 4)
 |
 +-------> G2: SOURCE TEMPLATE (GitHub copy -- mirror Step 2 + Step 4)
 |          |
 |          v
 |         G5: GOLDEN FILES .github/ (copy to 8 profiles)
 |
 +-------> G3: GOLDEN FILES .claude/ (copy to 8 profiles)
            |
            v
           G4: GOLDEN FILES .agents/ (copy from .claude/ to 8 profiles)

           G3 + G4 + G5 (all must be complete)
            |
            v
           G6: VERIFICATION (compile, test, coverage)
```

- G1 must be done first (modifies Claude source template).
- G2 depends on G1 (mirrors Claude changes to GitHub format).
- G3 depends on G1 (copies Claude template to `.claude/` golden files).
- G4 depends on G3 (mirrors `.claude/` to `.agents/`).
- G5 depends on G2 (copies GitHub template to `.github/` golden files).
- G6 depends on G3, G4, and G5 (all 24 golden files must be updated before running tests).
- G2 and G3 can run in parallel after G1 completes.
- G4 and G5 can run in parallel once their respective dependencies complete.

## File Inventory

### Template source files (2 modified)

| File | Action |
|------|--------|
| `resources/skills-templates/core/x-story-epic/SKILL.md` | MODIFY -- Add TDD rules extraction to Step 2, TDD DoD items to Step 4 |
| `resources/github-skills-templates/story/x-story-epic.md` | MODIFY -- Mirror TDD changes (identical content) |

### Golden files -- `.claude/` (8 modified)

| Profile | Path |
|---------|------|
| go-gin | `tests/golden/go-gin/.claude/skills/x-story-epic/SKILL.md` |
| java-quarkus | `tests/golden/java-quarkus/.claude/skills/x-story-epic/SKILL.md` |
| java-spring | `tests/golden/java-spring/.claude/skills/x-story-epic/SKILL.md` |
| kotlin-ktor | `tests/golden/kotlin-ktor/.claude/skills/x-story-epic/SKILL.md` |
| python-click-cli | `tests/golden/python-click-cli/.claude/skills/x-story-epic/SKILL.md` |
| python-fastapi | `tests/golden/python-fastapi/.claude/skills/x-story-epic/SKILL.md` |
| rust-axum | `tests/golden/rust-axum/.claude/skills/x-story-epic/SKILL.md` |
| typescript-nestjs | `tests/golden/typescript-nestjs/.claude/skills/x-story-epic/SKILL.md` |

### Golden files -- `.agents/` (8 modified, byte-for-byte identical to `.claude/`)

| Profile | Path |
|---------|------|
| go-gin | `tests/golden/go-gin/.agents/skills/x-story-epic/SKILL.md` |
| java-quarkus | `tests/golden/java-quarkus/.agents/skills/x-story-epic/SKILL.md` |
| java-spring | `tests/golden/java-spring/.agents/skills/x-story-epic/SKILL.md` |
| kotlin-ktor | `tests/golden/kotlin-ktor/.agents/skills/x-story-epic/SKILL.md` |
| python-click-cli | `tests/golden/python-click-cli/.agents/skills/x-story-epic/SKILL.md` |
| python-fastapi | `tests/golden/python-fastapi/.agents/skills/x-story-epic/SKILL.md` |
| rust-axum | `tests/golden/rust-axum/.agents/skills/x-story-epic/SKILL.md` |
| typescript-nestjs | `tests/golden/typescript-nestjs/.agents/skills/x-story-epic/SKILL.md` |

### Golden files -- `.github/` (8 modified, byte-for-byte identical to GitHub template -- no placeholders)

| Profile | Path |
|---------|------|
| go-gin | `tests/golden/go-gin/.github/skills/x-story-epic/SKILL.md` |
| java-quarkus | `tests/golden/java-quarkus/.github/skills/x-story-epic/SKILL.md` |
| java-spring | `tests/golden/java-spring/.github/skills/x-story-epic/SKILL.md` |
| kotlin-ktor | `tests/golden/kotlin-ktor/.github/skills/x-story-epic/SKILL.md` |
| python-click-cli | `tests/golden/python-click-cli/.github/skills/x-story-epic/SKILL.md` |
| python-fastapi | `tests/golden/python-fastapi/.github/skills/x-story-epic/SKILL.md` |
| rust-axum | `tests/golden/rust-axum/.github/skills/x-story-epic/SKILL.md` |
| typescript-nestjs | `tests/golden/typescript-nestjs/.github/skills/x-story-epic/SKILL.md` |

### Files NOT modified

| File | Reason |
|------|--------|
| `src/assembler/skills-assembler.ts` | No change -- copies core skill templates as-is |
| `src/assembler/codex-skills-assembler.ts` | No change -- mirrors `.claude/skills/` to `.agents/skills/` |
| `src/assembler/github-skills-assembler.ts` | No change -- reads template, applies placeholder resolution (no-op for this skill) |
| `tests/node/integration/byte-for-byte.test.ts` | No change -- existing test validates golden files automatically |

## Key Implementation Notes

1. **All changes are additive (RULE-003):** Existing DoD items (coverage, test types, documentation, SLOs, persistence) and existing rule extraction guidance are fully preserved. Two new DoD items and one new Step 2 subsection are appended.

2. **RULE-001 (Dual Copy Consistency):** The TDD content added to Step 2 and Step 4 is identical between Claude and GitHub copies. The pre-existing differences between the two copies (Prerequisites paths, Step 5 section headers, Common Mistakes language, "Detailed References" section) are NOT modified.

3. **RULE-002 (Source of Truth):** The source files in `resources/` are the source of truth. Golden files are derived outputs that must be regenerated after template changes.

4. **No placeholder handling needed:** The x-story-epic skill contains NO profile-specific placeholders in either copy. This means:
   - `.claude/` golden files = exact copy of Claude source template
   - `.agents/` golden files = exact copy of `.claude/` golden files
   - `.github/` golden files = exact copy of GitHub source template
   - All 8 profiles within each target are byte-for-byte identical

5. **Golden file update strategy:** Simple file copy for all three targets. No pipeline execution required (unlike skills with `{language_name}` placeholders). A single `cp` per target set suffices.

6. **Content language (RULE-012):** The new TDD content uses English technical terms (Red-Green-Refactor, TDD, TPP, Gherkin, Conventional Commits). These are industry-standard terms that remain in English per the Language Rules section of the skill.
