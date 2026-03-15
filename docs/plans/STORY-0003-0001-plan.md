# Implementation Plan -- STORY-0003-0001: Testing KP -- TDD Workflow & Transformation Priority Premise

**Status:** DRAFT
**Date:** 2026-03-15
**Story:** story-0003-0001
**Blocked by:** None (foundation story)
**Blocks:** story-0003-0003, story-0003-0004, story-0003-0006, story-0003-0007

---

## 1. Affected Layers and Components

| Layer | Component | Action |
|-------|-----------|--------|
| Source template | `resources/core/03-testing-philosophy.md` | **Append** -- add 4 new H2 sections at the end |
| Golden (.claude) | `tests/golden/{profile}/.claude/skills/testing/references/testing-philosophy.md` (x8) | **Update** -- must match source exactly |
| Golden (.agents) | `tests/golden/{profile}/.agents/skills/testing/references/testing-philosophy.md` (x8) | **Update** -- must match source exactly |
| Tests | `tests/node/integration/byte-for-byte.test.ts` | **No change** -- existing test validates parity automatically |
| Pipeline | `src/assembler/rules-assembler.ts` (routeCoreToKps) | **No change** -- existing routing copies `03-testing-philosophy.md` to `testing/references/testing-philosophy.md` |
| Pipeline | `src/assembler/codex-skills-assembler.ts` | **No change** -- mirrors `.claude/skills/` to `.agents/skills/` |
| Pipeline | `src/domain/core-kp-routing.ts` | **No change** -- route `{sourceFile: "03-testing-philosophy.md", kpName: "testing", destFile: "testing-philosophy.md"}` already exists |

This is a **content-only story**. No TypeScript code changes are required. The existing copy pipeline (`RulesAssembler.routeCoreToKps` -> `CodexSkillsAssembler`) propagates the source file automatically. Only the source template and golden files need updating.

### Copy Pipeline Flow

```
resources/core/03-testing-philosophy.md
  |
  |--(core-kp-routing.ts, RulesAssembler.routeCoreToKps)-->
  |   .claude/skills/testing/references/testing-philosophy.md
  |
  |--(CodexSkillsAssembler, mirrors .claude/skills/)-->
      .agents/skills/testing/references/testing-philosophy.md
```

The story's DoR mentions `resources/skills-templates/core/testing/references/` and `resources/github-skills-templates/testing/references/` as dual copy locations. These paths do **not exist** in the codebase. The actual "dual copy" is automatic: `RulesAssembler` writes the `.claude/` copy, `CodexSkillsAssembler` writes the `.agents/` copy, both from the single source at `resources/core/03-testing-philosophy.md`.

---

## 2. New Content Sections to Create

All sections are appended to the end of `resources/core/03-testing-philosophy.md`, after the existing "When to Use Real Database vs In-Memory" section.

### 2.1 `## TDD Workflow` (Red-Green-Refactor)

- 3 sub-sections: **RED**, **GREEN**, **REFACTOR**
- RED: Write ONE failing test. Simplest possible. Verify it fails for the right reason.
- GREEN: Write MINIMUM code to pass. No optimization, no generalization.
- REFACTOR: Improve design (eliminate duplication, improve naming, extract methods). All tests MUST stay green.
- Rule: NEVER write production code without a failing test first.
- Rule: NEVER add new behavior during refactoring.

### 2.2 `## Double-Loop TDD`

- **Outer Loop (Acceptance Test)**: Derived from Gherkin/story. Written FIRST. Stays RED throughout development. Goes GREEN only when ALL functionality is complete.
- **Inner Loop (Unit Test)**: Rapid Red-Green-Refactor cycles within the outer loop. Each inner cycle implements one piece of functionality, bringing the acceptance test closer to GREEN.
- Mermaid sequence diagram showing interaction between loops (from story section 6.1).
- Rule: The acceptance test is the DONE criterion for the story.

### 2.3 `## Transformation Priority Premise`

Ordered list of transformations (simplest to most complex):
1. `{}->nil` -- no code to returning nil/null/undefined
2. `nil->constant` -- return a fixed value
3. `constant->variable` -- replace constant with variable
4. `unconditional->conditional` -- add if/else
5. `scalar->collection` -- work with lists/arrays
6. `statement->recursion/iteration` -- loops
7. `value->mutated value` -- transform values

- Rule: Always choose the LOWEST priority transformation that makes the test pass.
- Rule: Test scenarios should be ordered to guide transformations in this order.

### 2.4 `## Test Scenario Ordering`

6 levels of complexity:
1. **Level 1 -- Degenerate cases**: null, empty, zero, constant returns
2. **Level 2 -- Unconditional paths**: single path without branching
3. **Level 3 -- Simple conditions**: basic if/else
4. **Level 4 -- Complex conditions**: multiple branches, switch/match
5. **Level 5 -- Iterations**: loops, map/filter/reduce
6. **Level 6 -- Edge cases**: boundary values (at-min, at-max, past-max), overflow, concurrent access

---

## 3. Existing Files to Modify

### 3.1 Source of Truth (1 file)

| # | File | Action |
|---|------|--------|
| 1 | `resources/core/03-testing-philosophy.md` | **Append** 4 new H2 sections after line 165 |

### 3.2 Golden Files -- `.claude/` variant (8 files)

| # | File |
|---|------|
| 1 | `tests/golden/go-gin/.claude/skills/testing/references/testing-philosophy.md` |
| 2 | `tests/golden/java-quarkus/.claude/skills/testing/references/testing-philosophy.md` |
| 3 | `tests/golden/java-spring/.claude/skills/testing/references/testing-philosophy.md` |
| 4 | `tests/golden/kotlin-ktor/.claude/skills/testing/references/testing-philosophy.md` |
| 5 | `tests/golden/python-click-cli/.claude/skills/testing/references/testing-philosophy.md` |
| 6 | `tests/golden/python-fastapi/.claude/skills/testing/references/testing-philosophy.md` |
| 7 | `tests/golden/rust-axum/.claude/skills/testing/references/testing-philosophy.md` |
| 8 | `tests/golden/typescript-nestjs/.claude/skills/testing/references/testing-philosophy.md` |

### 3.3 Golden Files -- `.agents/` variant (8 files)

| # | File |
|---|------|
| 1 | `tests/golden/go-gin/.agents/skills/testing/references/testing-philosophy.md` |
| 2 | `tests/golden/java-quarkus/.agents/skills/testing/references/testing-philosophy.md` |
| 3 | `tests/golden/java-spring/.agents/skills/testing/references/testing-philosophy.md` |
| 4 | `tests/golden/kotlin-ktor/.agents/skills/testing/references/testing-philosophy.md` |
| 5 | `tests/golden/python-click-cli/.agents/skills/testing/references/testing-philosophy.md` |
| 6 | `tests/golden/python-fastapi/.agents/skills/testing/references/testing-philosophy.md` |
| 7 | `tests/golden/rust-axum/.agents/skills/testing/references/testing-philosophy.md` |
| 8 | `tests/golden/typescript-nestjs/.agents/skills/testing/references/testing-philosophy.md` |

### Total files to modify: **17** (1 source + 8 `.claude/` golden + 8 `.agents/` golden)

All 16 golden files must be byte-for-byte identical to the source file after modification.

---

## 4. Dependency Direction Validation

**N/A** -- This is a content-only change. No TypeScript imports, no dependency graph changes. The file `resources/core/03-testing-philosophy.md` is a static Markdown template with no placeholders (confirmed: it contains no `{{...}}` or `{...}` template variables beyond the standard header block).

---

## 5. Integration Points

### 5.1 Byte-for-Byte Test (Primary Gate)

- **Test file:** `tests/node/integration/byte-for-byte.test.ts`
- **Mechanism:** Runs `runPipeline()` for each of the 8 config profiles, then calls `verifyOutput()` to compare every generated file against the golden directory.
- **Impact:** If the source file is updated but golden files are not, all 8 profile tests fail with content mismatch on `testing-philosophy.md`.
- **Action:** Update all 16 golden files to match the source.

### 5.2 Skills That Reference This KP

The following skills and agents read `testing/references/testing-philosophy.md` at runtime. They will automatically benefit from the new TDD sections:

- `x-test-plan` (SKILL.md references testing KP)
- `x-test-run` (SKILL.md references testing KP)
- `x-review` (SKILL.md references testing KP)
- `run-e2e` (SKILL.md references testing KP)
- `qa-engineer` agent (reads testing KP)

No changes needed in these consumers -- they already read the full file.

---

## 6. Database Changes

**N/A** -- No database in this project.

---

## 7. API Changes

**N/A** -- No API endpoints affected.

---

## 8. Event Changes

**N/A** -- No event-driven components affected.

---

## 9. Configuration Changes

**N/A** -- No configuration files, environment variables, or settings changes.

---

## 10. Risk Assessment

### 10.1 Backward Compatibility

| Risk | Mitigation | Severity |
|------|-----------|----------|
| Existing content modified or removed | Append-only strategy: new sections go after line 165 (end of file). Existing 11 sections remain untouched. | LOW |
| Template placeholders broken | Source file uses no template placeholders. Content is static Markdown. | NONE |
| Golden file staleness | All 16 golden files updated in same commit as source. Byte-for-byte test catches drift. | LOW |

### 10.2 Content Risks

| Risk | Mitigation | Severity |
|------|-----------|----------|
| TDD sections too prescriptive for non-TDD projects | Story acceptance criterion 7: sections are informative, not prescriptive. Use declarative language ("TDD recommends..." not "you MUST..."). | LOW |
| Mermaid diagram rendering issues | Diagram wrapped in standard ```mermaid fenced block. Already used elsewhere in the project (e.g., story-0003-0001.md section 6.1). | LOW |

### 10.3 Test Impact

| Risk | Mitigation | Severity |
|------|-----------|----------|
| Byte-for-byte test failures | Update all 16 golden files before running tests. Single-source copy ensures consistency. | LOW |
| Unit test `rules-assembler.test.ts` failure | Test uses minimal stub content (`"# Testing"`), not real file content. No impact. | NONE |

---

## Implementation Strategy

### Recommended Order

1. **Write new content** in `resources/core/03-testing-philosophy.md` (append after line 165)
2. **Copy source to all 16 golden files** (single `cp` operation, since all golden copies are identical to source)
3. **Run byte-for-byte tests** (`npx vitest run tests/node/integration/byte-for-byte.test.ts`) to verify parity
4. **Run full test suite** (`npx vitest run`) to confirm no regressions

### Efficiency Note

Since all 16 golden files are byte-for-byte identical to the source, the implementation can use a script or loop to copy the source to all golden locations after editing:

```bash
SRC="resources/core/03-testing-philosophy.md"
for profile in go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs; do
  cp "$SRC" "tests/golden/$profile/.claude/skills/testing/references/testing-philosophy.md"
  cp "$SRC" "tests/golden/$profile/.agents/skills/testing/references/testing-philosophy.md"
done
```
