# Task Breakdown — story-0003-0009

## x-story-create — Enriched Gherkin with Degenerate & Boundary Cases

### Overview

This story modifies 2 source-of-truth Markdown files and propagates changes to 24 golden files. No TypeScript code changes. The work is purely content edits to skill instruction files.

**Source files:**
1. `resources/skills-templates/core/x-story-create/SKILL.md` (Claude copy — pt-BR headers)
2. `resources/github-skills-templates/story/x-story-create.md` (GitHub copy — English headers)

**Golden files:** 8 profiles x 3 output dirs = 24 files

---

## G1 — Read and Understand Current Files

**Goal:** Build full understanding of current file contents, diff points, and the plan's exact edit locations.

| # | Task | File(s) | Output |
|---|------|---------|--------|
| G1.1 | Read Claude source template | `resources/skills-templates/core/x-story-create/SKILL.md` | Understand current structure (196 lines), identify exact line ranges for each edit point |
| G1.2 | Read GitHub source template | `resources/github-skills-templates/story/x-story-create.md` | Understand differences from Claude copy: English headers, GitHub paths, extra "Detailed References" section at end |
| G1.3 | Read the implementation plan | `docs/stories/epic-0003/plans/plan-story-0003-0009.md` | Internalize all 4 edit zones (Sections 3.1.1 through 3.1.4) and the difference map (Section 14) |
| G1.4 | Verify Rule 13 path references | Check that `.claude/skills/story-planning/references/story-decomposition.md` exists in golden output; check `resources/core/13-story-decomposition.md` exists | Confirm correct paths for both copies |
| G1.5 | Verify one golden file matches source byte-for-byte | `diff` any `.claude` golden file against the Claude source | Confirm golden files are currently in sync (clean baseline) |

**Acceptance:** All 5 tasks complete; exact line numbers for each edit zone documented.

---

## G2 — Update Claude Source Template (SKILL.md)

**Goal:** Apply all 4 content edits to the Claude copy.

**File:** `resources/skills-templates/core/x-story-create/SKILL.md`

| # | Task | Edit Zone | Description |
|---|------|-----------|-------------|
| G2.1 | Add Rule 13 prerequisite reference | Prerequisites section (after line 36) | Add new prerequisite block: "Gherkin completeness (mandatory categories and ordering):" with path `.claude/skills/story-planning/references/story-decomposition.md` referencing SD-02 and SD-05a |
| G2.2 | Replace Section 7 required scenarios | Lines 135-139 (Required scenarios list) | Replace the 4-item generic list (happy path, business rule violation, malformed input, edge case) with 5-item TPP-ordered list: degenerate cases, happy path, error paths, boundary values, complex edge cases |
| G2.3 | Add TPP ordering and minimum validation sub-sections | After the quality rules block (after line 145) | Add "Scenario Ordering (TPP):" sub-section with 5-step ordering instruction. Add "Minimum Scenario Validation:" sub-section requiring >= 4 scenarios with fallback guidance. Add Rule 13 reference note |
| G2.4 | Update sizing heuristics minimum | Line 182 ("Less than 2 Gherkin scenarios") | Change `2` to `4` in the "Too small" criteria |
| G2.5 | Add new common mistakes entries | After line 195 (end of Common Mistakes section) | Append 4 new entries: missing degenerate cases, happy-path-first ordering, boundary values without triplet, fewer than 4 scenarios |

**Acceptance:** Claude source template contains all enriched Gherkin instructions. File length approximately 240 lines. All existing content preserved (backward compatible).

### Verification for G2

- Manual review: all 5 edits applied at correct locations
- No existing sections removed or broken
- Portuguese section headers preserved (`Critérios de Aceite`, `Dependências`, etc.)
- Path references use `.claude/` prefix

---

## G3 — Update GitHub Source Template (x-story-create.md)

**Goal:** Apply semantically identical changes to the GitHub copy, using English headers and GitHub-specific paths.

**File:** `resources/github-skills-templates/story/x-story-create.md`

| # | Task | Edit Zone | Description |
|---|------|-----------|-------------|
| G3.1 | Add Rule 13 prerequisite reference | Prerequisites section (after line 36) | Same content as G2.1 but with GitHub path: `resources/core/13-story-decomposition.md` instead of `.claude/skills/story-planning/references/story-decomposition.md` |
| G3.2 | Replace Section 7 required scenarios | Lines 135-139 (Required scenarios list) | Identical content to G2.2 — the enriched categories are language-neutral |
| G3.3 | Add TPP ordering and minimum validation sub-sections | After the quality rules block (after line 145) | Identical content to G2.3 but with GitHub Rule 13 path reference |
| G3.4 | Update sizing heuristics minimum | Line 182 ("Less than 2 Gherkin scenarios") | Change `2` to `4` — identical to G2.4 |
| G3.5 | Add new common mistakes entries | After line 195 (end of Common Mistakes section, before "Detailed References") | Same 4 entries as G2.5 but with English example text ("Send card data" style, matching existing line 191) |

**Acceptance:** GitHub source template has semantically identical enriched Gherkin instructions. English section headers preserved (`Acceptance Criteria`, `Dependencies`, etc.). "Detailed References" section at end of file preserved.

### Verification for G3

- `diff` both source files: only expected differences remain (paths, header language, example text, Detailed References section)
- No structural divergence in the enriched Gherkin content

### RULE-001 Compliance Check (Dual Copy Consistency)

| Aspect | Claude Template | GitHub Template |
|--------|----------------|-----------------|
| Rule 13 path | `.claude/skills/story-planning/references/story-decomposition.md` | `resources/core/13-story-decomposition.md` |
| Decomposition ref | `.claude/skills/x-story-epic-full/references/decomposition-guide.md` | `.github/skills/x-story-epic-full/SKILL.md` |
| Section 7 header | `Critérios de Aceite (Gherkin)` | `Acceptance Criteria (Gherkin)` |
| Enriched categories | Identical 5-item TPP list | Identical 5-item TPP list |
| TPP ordering block | Identical | Identical |
| Minimum validation block | Identical | Identical |
| Common mistakes | Portuguese example ("Enviar dados do cartao") | English example ("Send card data") |
| Detailed References | Not present | Present at end |

---

## G4 — Update Golden Files (24 files)

**Goal:** Propagate source template changes to all golden file copies.

### G4.1 — Copy Claude source to .claude golden files (8 files)

| # | Profile | Golden File Path |
|---|---------|-----------------|
| 1 | go-gin | `tests/golden/go-gin/.claude/skills/x-story-create/SKILL.md` |
| 2 | java-quarkus | `tests/golden/java-quarkus/.claude/skills/x-story-create/SKILL.md` |
| 3 | java-spring | `tests/golden/java-spring/.claude/skills/x-story-create/SKILL.md` |
| 4 | kotlin-ktor | `tests/golden/kotlin-ktor/.claude/skills/x-story-create/SKILL.md` |
| 5 | python-click-cli | `tests/golden/python-click-cli/.claude/skills/x-story-create/SKILL.md` |
| 6 | python-fastapi | `tests/golden/python-fastapi/.claude/skills/x-story-create/SKILL.md` |
| 7 | rust-axum | `tests/golden/rust-axum/.claude/skills/x-story-create/SKILL.md` |
| 8 | typescript-nestjs | `tests/golden/typescript-nestjs/.claude/skills/x-story-create/SKILL.md` |

**Method:** Copy `resources/skills-templates/core/x-story-create/SKILL.md` to each path.

### G4.2 — Copy Claude source to .agents golden files (8 files)

| # | Profile | Golden File Path |
|---|---------|-----------------|
| 1 | go-gin | `tests/golden/go-gin/.agents/skills/x-story-create/SKILL.md` |
| 2 | java-quarkus | `tests/golden/java-quarkus/.agents/skills/x-story-create/SKILL.md` |
| 3 | java-spring | `tests/golden/java-spring/.agents/skills/x-story-create/SKILL.md` |
| 4 | kotlin-ktor | `tests/golden/kotlin-ktor/.agents/skills/x-story-create/SKILL.md` |
| 5 | python-click-cli | `tests/golden/python-click-cli/.agents/skills/x-story-create/SKILL.md` |
| 6 | python-fastapi | `tests/golden/python-fastapi/.agents/skills/x-story-create/SKILL.md` |
| 7 | rust-axum | `tests/golden/rust-axum/.agents/skills/x-story-create/SKILL.md` |
| 8 | typescript-nestjs | `tests/golden/typescript-nestjs/.agents/skills/x-story-create/SKILL.md` |

**Method:** Copy `resources/skills-templates/core/x-story-create/SKILL.md` to each path. (.agents mirrors .claude)

### G4.3 — Copy GitHub source to .github golden files (8 files)

| # | Profile | Golden File Path |
|---|---------|-----------------|
| 1 | go-gin | `tests/golden/go-gin/.github/skills/x-story-create/SKILL.md` |
| 2 | java-quarkus | `tests/golden/java-quarkus/.github/skills/x-story-create/SKILL.md` |
| 3 | java-spring | `tests/golden/java-spring/.github/skills/x-story-create/SKILL.md` |
| 4 | kotlin-ktor | `tests/golden/kotlin-ktor/.github/skills/x-story-create/SKILL.md` |
| 5 | python-click-cli | `tests/golden/python-click-cli/.github/skills/x-story-create/SKILL.md` |
| 6 | python-fastapi | `tests/golden/python-fastapi/.github/skills/x-story-create/SKILL.md` |
| 7 | rust-axum | `tests/golden/rust-axum/.github/skills/x-story-create/SKILL.md` |
| 8 | typescript-nestjs | `tests/golden/typescript-nestjs/.github/skills/x-story-create/SKILL.md` |

**Method:** Copy `resources/github-skills-templates/story/x-story-create.md` to each path.

### Mechanical Execution Script

```bash
CLAUDE_SRC="resources/skills-templates/core/x-story-create/SKILL.md"
GITHUB_SRC="resources/github-skills-templates/story/x-story-create.md"
PROFILES=(go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs)

for profile in "${PROFILES[@]}"; do
  cp "$CLAUDE_SRC" "tests/golden/$profile/.claude/skills/x-story-create/SKILL.md"
  cp "$CLAUDE_SRC" "tests/golden/$profile/.agents/skills/x-story-create/SKILL.md"
  cp "$GITHUB_SRC" "tests/golden/$profile/.github/skills/x-story-create/SKILL.md"
done
```

**Acceptance:** All 24 golden files updated. Each .claude and .agents file is byte-identical to the Claude source. Each .github file is byte-identical to the GitHub source.

---

## G5 — Run Tests and Verify

**Goal:** Confirm all tests pass and coverage is maintained.

| # | Task | Command | Expected Result |
|---|------|---------|-----------------|
| G5.1 | Run byte-for-byte integration tests | `npx vitest run tests/node/integration/byte-for-byte.test.ts` | All assertions pass for all 8 profiles (golden files match pipeline output) |
| G5.2 | Run full test suite | `npx vitest run` | All 1,384+ tests pass |
| G5.3 | Verify coverage thresholds | Check coverage output from G5.2 | Line >= 95%, Branch >= 90% (no code changes, so unchanged from current 99.6% / 97.84%) |

**Acceptance:** Zero test failures. Coverage unchanged.

---

## G6 — Content Validation

**Goal:** Verify the enriched Gherkin content is correct and complete in the generated output.

| # | Task | Validation Method | What to Check |
|---|------|-------------------|---------------|
| G6.1 | Verify Claude source contains all 4 mandatory categories | Read file, search for: "Degenerate cases", "Happy path", "Error paths", "Boundary values" | All 4 present in Section 7 |
| G6.2 | Verify TPP ordering instruction present | Search for "Transformation Priority Premise" or "TPP" ordering block | Present after quality rules |
| G6.3 | Verify minimum 4 scenarios instruction | Search for "at least 4 Gherkin scenarios" | Present in minimum validation sub-section |
| G6.4 | Verify boundary triplet pattern mentioned | Search for "at-minimum, at-maximum, past-maximum" or "at-min, at-max, past-max" | Present in category 4 description |
| G6.5 | Verify Rule 13 prerequisite reference | Search for "story-decomposition" in prerequisites | Present with correct path for each copy |
| G6.6 | Verify sizing heuristic updated | Search for "Less than 4 Gherkin scenarios" | Changed from 2 to 4 |
| G6.7 | Verify 4 new common mistakes added | Count entries in Common Mistakes section | Now has 9 entries (5 original + 4 new) |
| G6.8 | Verify GitHub copy has identical enriched content | Diff both sources, filter by enriched sections | Only path/language differences |
| G6.9 | Verify backward compatibility | Confirm no existing sections deleted | All original sections present in both copies |

**Acceptance:** All 9 content validations pass.

---

## G7 — Final Verification

**Goal:** End-to-end confirmation that the story's DoD is met.

| # | Task | Verification |
|---|------|-------------|
| G7.1 | DoD: x-story-create generates scenarios in 4 mandatory categories | Claude source Section 7 lists all 4 with TPP order labels |
| G7.2 | DoD: Scenarios ordered by TPP | TPP ordering sub-section present with 5-step priority list |
| G7.3 | DoD: Minimum of 4 scenarios validated | Minimum validation sub-section with explicit "at least 4" instruction |
| G7.4 | DoD: Boundary values use triplet pattern | Category 4 specifies "at-minimum, at-maximum, past-maximum" |
| G7.5 | DoD: Both copies updated (RULE-001) | Both source templates modified; diff shows only expected path/language differences |
| G7.6 | DoD: Golden file tests updated | 24 golden files updated and byte-for-byte tests pass |
| G7.7 | DoD: Coverage >= 95% line, >= 90% branch | No code changes; coverage unchanged |
| G7.8 | DoD: Backward compatibility | No existing sections removed; change is purely additive |
| G7.9 | Verify `_TEMPLATE-STORY.md` NOT modified | `git diff` shows no changes to `resources/templates/_TEMPLATE-STORY.md` |
| G7.10 | Verify no TypeScript code changes | `git diff --name-only` shows only `.md` files changed |

**Acceptance:** All 10 DoD checks pass. Story is complete.

---

## Execution Order and Dependencies

```
G1 (read & understand)
 |
 v
G2 (edit Claude source) --> G3 (edit GitHub source)
 |                            |
 v                            v
G4 (copy to 24 golden files) <-- depends on G2 + G3
 |
 v
G5 (run tests)
 |
 v
G6 (content validation)
 |
 v
G7 (final DoD verification)
```

**Critical path:** G1 -> G2 -> G4 -> G5 -> G7

G2 and G3 can run in parallel (independent source files).
G6 can run in parallel with G5 (content checks are manual, tests are automated).

---

## File Change Summary

| Category | Count | Files |
|----------|-------|-------|
| Source of Truth | 2 | `resources/skills-templates/core/x-story-create/SKILL.md`, `resources/github-skills-templates/story/x-story-create.md` |
| Golden (.claude) | 8 | `tests/golden/{profile}/.claude/skills/x-story-create/SKILL.md` |
| Golden (.agents) | 8 | `tests/golden/{profile}/.agents/skills/x-story-create/SKILL.md` |
| Golden (.github) | 8 | `tests/golden/{profile}/.github/skills/x-story-create/SKILL.md` |
| **Total** | **26** | |

**Explicitly unchanged:** `resources/templates/_TEMPLATE-STORY.md`, all TypeScript files, all test infrastructure, all assemblers.
