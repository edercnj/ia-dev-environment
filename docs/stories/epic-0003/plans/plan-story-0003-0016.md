# Implementation Plan — story-0003-0016

## x-review-pr — TDD Rubric Criteria

### 1. Summary

This story adds TDD criteria to the `x-review-pr` skill's rubric, updates the GO/NO-GO decision logic to account for the new criteria, and adds a testing KP TDD reference to the Gather Context step. The changes are purely content edits to skill instruction files -- no TypeScript code or pipeline logic changes.

**Key insight from context analysis:** The tech-lead **agent** templates (`resources/agents-templates/core/tech-lead.md` and `resources/github-agents-templates/core/tech-lead.md`) were already updated by story-0003-0006 to include a "TDD Process (41-45)" category, bringing the agent's checklist from 40 to 45 points. However, the **skill** templates (`x-review-pr`) still reference a "40-Point Rubric" with only 10 categories (A-J). This story aligns the skill with the agent by adding TDD criteria to the skill's rubric.

---

### 2. Architecture — How the x-review-pr Copy System Works

```
resources/skills-templates/core/x-review-pr/SKILL.md   <-- SOURCE OF TRUTH (Claude)
        |
        | copyTemplateTree (no {{placeholders}} in this file)
        | via: src/assembler/skills-assembler.ts :: assembleCore()
        v
{outputDir}/.claude/skills/x-review-pr/SKILL.md        <-- pipeline output (.claude)
        |
        | mirror from .claude/skills/
        | via: src/assembler/codex-skills-assembler.ts
        v
{outputDir}/.agents/skills/x-review-pr/SKILL.md        <-- pipeline output (.agents)

---

resources/github-skills-templates/review/x-review-pr.md <-- SOURCE OF TRUTH (GitHub)
        |
        | renderSkill (no {{placeholders}} in this file)
        | via: src/assembler/github-skills-assembler.ts
        v
{outputDir}/.github/skills/x-review-pr/SKILL.md        <-- pipeline output (.github)
```

Key properties:
- The Claude source template has **no `{{placeholders}}`** -- the pipeline copies it byte-for-byte.
- The `.agents` copy is an exact mirror of the `.claude` copy (via `codex-skills-assembler`).
- The GitHub copy is a separate source file with different YAML frontmatter and path references.
- All 8 profiles produce identical copies (the skill is profile-independent).

---

### 3. Decision: Option A (New Category K) vs Option B (Expand Category I)

**Recommendation: Option A -- New Category K: TDD Process**

**Rationale:**

1. **Consistency with agent template:** The tech-lead agent (already updated in story-0003-0006) uses a separate "TDD Process (41-45)" category with 5 dedicated items, numbered 41-45. Adding a matching category K in the skill rubric mirrors this structure and avoids confusion.

2. **Separation of concerns:** Category I (Tests) evaluates the _quality of the tests themselves_ (coverage, naming, scenarios). TDD Process evaluates the _development methodology_ (commit history, workflow discipline, transformation ordering). These are orthogonally different concerns.

3. **Category I expansion issues (Option B):**
   - Expanding I from 3 to 9 points would make it the second-largest category (after A with 8), creating a scoring imbalance.
   - I4-I9 items (test-first commits, Double-Loop TDD, TPP ordering) are process/workflow checks, not test quality checks. They belong in their own category.
   - The rubric table would have one oversized row mixing two distinct review dimensions.

4. **Cleaner GO/NO-GO rule:** With a separate category K, the NO-GO rule can state "any K item = 0 triggers NO-GO" without tangling with existing I items.

5. **Point total:** The rubric goes from 40 to 45 points (5 new items in K), matching the existing "TDD Process (41-45)" structure in the tech-lead agent rubric. The skill rubric's Category K mirrors that agent rubric with five TDD items.

**Point total update:** 40 -> 45 (5 new points in category K).

---

### 4. Affected Layers and Components

| Layer | Component | Impact |
|-------|-----------|--------|
| Resources (Claude source) | `resources/skills-templates/core/x-review-pr/SKILL.md` | **MODIFIED** -- add category K, update totals, add KP reference |
| Resources (GitHub source) | `resources/github-skills-templates/review/x-review-pr.md` | **MODIFIED** -- parallel changes with GitHub-specific paths |
| Golden files (.claude) | `tests/golden/{profile}/.claude/skills/x-review-pr/SKILL.md` | **MUST UPDATE** -- 8 files |
| Golden files (.agents) | `tests/golden/{profile}/.agents/skills/x-review-pr/SKILL.md` | **MUST UPDATE** -- 8 files |
| Golden files (.github) | `tests/golden/{profile}/.github/skills/x-review-pr/SKILL.md` | **MUST UPDATE** -- 8 files |

**Total: 2 source templates + 24 golden files = 26 files modified.**

---

### 5. New Content to Add

#### 5.1 Category K -- TDD Process (6 points)

Add to the 40-Point Rubric table:

```markdown
| K. TDD Process         | 6      | Test-first commits, Double-Loop TDD, TPP progression, atomic cycles |
```

Full criteria (to be detailed in the rubric description or inline):
- **K1:** Git history shows test-first pattern (test commits precede implementation commits)
- **K2:** Double-Loop TDD applied (acceptance test precedes unit tests)
- **K3:** TPP progression visible in test ordering (simple to complex)
- **K4:** Refactoring phases do not add behavior (tests unchanged during refactor commits)
- **K5:** Atomic commits (one behavior per Red-Green-Refactor cycle)
- **K6:** Acceptance tests validate end-to-end behavior

#### 5.2 GO/NO-GO Logic Update

Update the Decision Criteria table from:

```markdown
| 40/40 + zero issues         | GO              |
| < 40/40 OR any issue        | NO-GO           |
```

To:

```markdown
| 46/46 + zero issues         | GO              |
| < 46/46 OR any issue        | NO-GO           |
```

#### 5.3 KP Reference in Gather Context

Add a new bullet to Step 2 (Gather Context) "Read knowledge packs" list:

**Claude copy:**
```markdown
- `skills/testing/references/testing-philosophy.md` -- TDD workflow, Double-Loop TDD, TPP ordering (sections: TDD Workflow, Double-Loop TDD, Transformation Priority Premise)
```

**GitHub copy:**
```markdown
- `.github/skills/testing/SKILL.md` -- TDD workflow, Double-Loop TDD, TPP ordering
```

#### 5.4 Score Display Update

Update the Step 4 result template from `XX/40` to `XX/46`.

#### 5.5 Step 3 Checklist Reference Update

Update Step 3 item 3 from "apply 40-point checklist" to "apply 46-point checklist".

#### 5.6 Description and Frontmatter Update

Update all references from "40-point" to "46-point":
- YAML `description` field
- `## Description` paragraph
- Section heading: `## 46-Point Rubric`

---

### 6. Existing Content to Modify

| Section | Current | New |
|---------|---------|-----|
| YAML description | "40-point checklist" | "46-point checklist" |
| Description paragraph | "40-point rubric" | "46-point rubric" |
| Rubric heading | `## 40-Point Rubric` | `## 46-Point Rubric` |
| Rubric table | 10 rows (A-J), total 40 pts | 11 rows (A-K), total 46 pts |
| Decision Criteria | `40/40`, `< 40/40` | `46/46`, `< 46/46` |
| Step 3 item 3 | "apply 40-point checklist" | "apply 46-point checklist" |
| Step 4 score display | `XX/40` | `XX/46` |
| Step 2 KP list | 3 bullets (coding-standards, architecture, quality-gates) | 4 bullets (+testing TDD reference) |

---

### 7. Cascading References to Update

The "40-point" count is referenced in other files beyond x-review-pr. These are **out of scope** for this story but should be tracked for a follow-up:

| File | Reference | Action |
|------|-----------|--------|
| `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` | "holistic 40-point review. Requires 40/40 for GO." | **OUT OF SCOPE** -- update in follow-up |
| `resources/github-skills-templates/dev/x-dev-lifecycle.md` | Same as above | **OUT OF SCOPE** -- update in follow-up |
| `resources/github-prompts-templates/code-review.prompt.md.j2` | "Run the holistic 40-point checklist review" | **OUT OF SCOPE** -- update in follow-up |

**Rationale:** The story scope explicitly targets x-review-pr SKILL.md and testing KP reference. The x-dev-lifecycle merely invokes x-review-pr and its description of "40-point" is a loose reference that does not affect correctness (the skill itself defines the actual rubric). A follow-up task should align these references.

**Note:** The tech-lead agent files (`resources/agents-templates/core/tech-lead.md` and `resources/github-agents-templates/core/tech-lead.md`) already say "45-Point Checklist" (from story-0003-0006). After this story, the skill will say 46 and the agent will say 45. This discrepancy exists because:
- The agent lists 5 TDD items (41-45), which are its own holistic checklist for the Tech Lead persona.
- The skill lists 6 TDD items (K1-K6), adding K6 (acceptance tests validate end-to-end behavior) as an additional criterion.

The agent and skill serve different purposes: the agent defines the persona's checklist, while the skill defines the review workflow's scoring rubric. The skill's K6 covers acceptance test validation which the agent subsumes under its general Testing category (item 26). This is acceptable divergence. Alternatively, the implementer may choose to add a 6th item to the agent (making both 46 points) or reduce the skill to 5 items (making both 45 points) for exact alignment. **Recommended: use 5 items (K1-K5) to match the agent's 45-point total**, making the skill 45 points.

**REVISED RECOMMENDATION:** Use 5 items (K1-K5) for an exact 45-point alignment with the tech-lead agent. The acceptance test validation from K6 is already covered by the existing I category (item I1: "scenarios covered") and the agent's item 26. This yields:

| Aspect | Value |
|--------|-------|
| New rubric total | 45 points |
| New items | K1-K5 (5 items) |
| Alignment | Matches tech-lead agent's 45-point checklist exactly |

---

### 8. Golden File Impact

#### 8.1 x-review-pr Skill -- 24 Golden Files

**Golden files (.claude) -- 8 files, identical to Claude source:**

| # | File |
|---|------|
| 1 | `tests/golden/go-gin/.claude/skills/x-review-pr/SKILL.md` |
| 2 | `tests/golden/java-quarkus/.claude/skills/x-review-pr/SKILL.md` |
| 3 | `tests/golden/java-spring/.claude/skills/x-review-pr/SKILL.md` |
| 4 | `tests/golden/kotlin-ktor/.claude/skills/x-review-pr/SKILL.md` |
| 5 | `tests/golden/python-click-cli/.claude/skills/x-review-pr/SKILL.md` |
| 6 | `tests/golden/python-fastapi/.claude/skills/x-review-pr/SKILL.md` |
| 7 | `tests/golden/rust-axum/.claude/skills/x-review-pr/SKILL.md` |
| 8 | `tests/golden/typescript-nestjs/.claude/skills/x-review-pr/SKILL.md` |

**Golden files (.agents) -- 8 files, identical to Claude source:**

| # | File |
|---|------|
| 9 | `tests/golden/go-gin/.agents/skills/x-review-pr/SKILL.md` |
| 10 | `tests/golden/java-quarkus/.agents/skills/x-review-pr/SKILL.md` |
| 11 | `tests/golden/java-spring/.agents/skills/x-review-pr/SKILL.md` |
| 12 | `tests/golden/kotlin-ktor/.agents/skills/x-review-pr/SKILL.md` |
| 13 | `tests/golden/python-click-cli/.agents/skills/x-review-pr/SKILL.md` |
| 14 | `tests/golden/python-fastapi/.agents/skills/x-review-pr/SKILL.md` |
| 15 | `tests/golden/rust-axum/.agents/skills/x-review-pr/SKILL.md` |
| 16 | `tests/golden/typescript-nestjs/.agents/skills/x-review-pr/SKILL.md` |

**Golden files (.github) -- 8 files, identical to GitHub source:**

| # | File |
|---|------|
| 17 | `tests/golden/go-gin/.github/skills/x-review-pr/SKILL.md` |
| 18 | `tests/golden/java-quarkus/.github/skills/x-review-pr/SKILL.md` |
| 19 | `tests/golden/java-spring/.github/skills/x-review-pr/SKILL.md` |
| 20 | `tests/golden/kotlin-ktor/.github/skills/x-review-pr/SKILL.md` |
| 21 | `tests/golden/python-click-cli/.github/skills/x-review-pr/SKILL.md` |
| 22 | `tests/golden/python-fastapi/.github/skills/x-review-pr/SKILL.md` |
| 23 | `tests/golden/rust-axum/.github/skills/x-review-pr/SKILL.md` |
| 24 | `tests/golden/typescript-nestjs/.github/skills/x-review-pr/SKILL.md` |

**Total: 24 golden files.**

#### 8.2 Golden File Update Strategy

```bash
# After editing the source templates:
CLAUDE_SRC="resources/skills-templates/core/x-review-pr/SKILL.md"
GITHUB_SRC="resources/github-skills-templates/review/x-review-pr.md"
PROFILES=(go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs)

for profile in "${PROFILES[@]}"; do
  cp "$CLAUDE_SRC" "tests/golden/$profile/.claude/skills/x-review-pr/SKILL.md"
  cp "$CLAUDE_SRC" "tests/golden/$profile/.agents/skills/x-review-pr/SKILL.md"
  cp "$GITHUB_SRC" "tests/golden/$profile/.github/skills/x-review-pr/SKILL.md"
done
```

Verification:
```bash
npx vitest run tests/node/integration/byte-for-byte.test.ts
```

---

### 9. Files to Modify (Complete List)

#### 9.1 Source of Truth (2 files)

| # | File | Change |
|---|------|--------|
| 1 | `resources/skills-templates/core/x-review-pr/SKILL.md` | Add K category, update totals, add testing KP reference |
| 2 | `resources/github-skills-templates/review/x-review-pr.md` | Parallel changes with GitHub-specific paths |

#### 9.2 Golden Files (24 files)

Listed in Section 8.1.

#### 9.3 Files Explicitly UNCHANGED

| File | Reason |
|------|--------|
| `resources/agents-templates/core/tech-lead.md` | Already has TDD Process (41-45) from story-0003-0006 |
| `resources/github-agents-templates/core/tech-lead.md` | Already has TDD Process from story-0003-0006 |
| `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` | Out of scope; follow-up to update "40-point" reference |
| `resources/github-skills-templates/dev/x-dev-lifecycle.md` | Out of scope |
| `resources/github-prompts-templates/code-review.prompt.md.j2` | Out of scope |
| `resources/skills-templates/knowledge-packs/testing/SKILL.md` | No changes needed; KP already references testing-philosophy.md which contains TDD sections |
| `src/assembler/skills-assembler.ts` | Copy logic unchanged |
| `src/assembler/github-skills-assembler.ts` | Copy logic unchanged |
| `src/assembler/codex-skills-assembler.ts` | Mirror logic unchanged |
| `tests/node/integration/byte-for-byte.test.ts` | Test logic unchanged |

---

### 10. Backward Compatibility Assessment

This change is **purely additive**:
- No existing rubric categories (A-J) are removed or modified.
- All 40 original rubric points remain intact with their original descriptions.
- The new category K adds 5 additional points (total becomes 45).
- The GO/NO-GO logic changes from `40/40 + zero issues` to `45/45 + zero issues` (same structure, higher maximum score).
- Stories previously reviewed under the 40-point rubric are not invalidated.
- The skill's scoring output format changes from `XX/40` to `XX/45`, which is a cosmetic change in the artifact output.

The testing KP reference addition in Step 2 is purely additive (new bullet in an existing list).

---

### 11. Risk Assessment

| Risk | Severity | Probability | Mitigation |
|------|----------|-------------|------------|
| Golden file mismatch after edit | HIGH | LOW | Mechanical copy script (Section 8.2) eliminates drift. Run byte-for-byte tests immediately. |
| Inconsistency between Claude and GitHub source templates | HIGH | MEDIUM | After editing both, diff them to verify only path references differ. |
| Agent vs skill point total mismatch | MEDIUM | MEDIUM | Use 5 TDD items (K1-K5) to align with agent's 45-point total. See Section 7 analysis. |
| Cascading "40-point" references in other files | LOW | HIGH | Documented in Section 7. These are out of scope -- loose references in x-dev-lifecycle and code-review prompt that don't affect rubric correctness. File follow-up. |
| x-dev-lifecycle says "Requires 40/40 for GO" | MEDIUM | HIGH | Out of scope per story definition. The actual GO/NO-GO logic is defined in x-review-pr, not x-dev-lifecycle. The lifecycle just invokes the skill. |
| KP testing reference path incorrect in generated output | LOW | LOW | The path `skills/testing/references/testing-philosophy.md` is consistent with how the testing KP is assembled (the `references/` dir is copied from the golden output). |

---

### 12. RULE-001 Compliance (Dual Copy Consistency)

| Dimension | Claude Source | GitHub Source |
|-----------|-------------|--------------|
| File path | `resources/skills-templates/core/x-review-pr/SKILL.md` | `resources/github-skills-templates/review/x-review-pr.md` |
| YAML frontmatter | `name`, `description`, `allowed-tools`, `argument-hint` | `name`, `description` (no tools/argument-hint) |
| Rubric content | Identical table structure and criteria | Identical table structure and criteria |
| KP reference paths | `skills/testing/references/testing-philosophy.md` | `.github/skills/testing/SKILL.md` |
| Other path references | `skills/coding-standards/references/...`, `skills/architecture/references/...`, `rules/05-quality-gates.md` | `.github/skills/coding-standards/SKILL.md`, `.github/skills/architecture/SKILL.md`, `.github/instructions/05-quality-gates.instructions.md` |
| Step 3 item 5 | `Compile and verify: {{COMPILE_COMMAND}} + {{BUILD_COMMAND}}` | Not present (GitHub copy omits compile step) |
| Step 3 item 6 | "If specialist reports exist..." (item 6) | Same text (item 5 due to missing compile step) |
| Common mistakes doc | Referenced in Step 2 | Not referenced |
| Integration Notes | Includes `/x-review` and `/x-review-pr` breakdown | Includes "Detailed References" section instead |

Differences are systematic and expected (the two copies serve different IDEs with different conventions). The **rubric content, criteria, GO/NO-GO logic, and TDD items must be semantically identical** in both copies.

---

### 13. Implementation Order

| Step | Action | Verification |
|------|--------|-------------|
| 1 | Write failing golden file test (verify x-review-pr contains TDD rubric) | Test fails (RED) |
| 2 | Edit Claude source: update YAML description (40 -> 45) | Manual review |
| 3 | Edit Claude source: update Description paragraph (40 -> 45) | Manual review |
| 4 | Edit Claude source: add testing KP reference to Step 2 | Manual review |
| 5 | Edit Claude source: update Step 3 checklist reference (40 -> 45) | Manual review |
| 6 | Edit Claude source: rename rubric heading (40 -> 45) | Manual review |
| 7 | Edit Claude source: add row K to rubric table | Manual review |
| 8 | Edit Claude source: update Decision Criteria (40/40 -> 45/45) | Manual review |
| 9 | Edit Claude source: update Step 4 score display (XX/40 -> XX/45) | Manual review |
| 10 | Edit GitHub source: apply parallel changes (English headers, GitHub paths) | Diff both sources |
| 11 | Copy Claude source to 16 golden files (.claude + .agents) | Script execution |
| 12 | Copy GitHub source to 8 golden files (.github) | Script execution |
| 13 | Run byte-for-byte test suite | All assertions pass (GREEN) |
| 14 | Run full test suite | All 1,384+ tests pass, coverage >= 95% line / >= 90% branch |

---

### 14. Difference Map Between Source Templates

For implementer reference, the Claude and GitHub source templates differ in these systematic ways:

| Aspect | Claude Template | GitHub Template |
|--------|----------------|-----------------|
| YAML frontmatter | Has `allowed-tools`, `argument-hint` | Only `name`, `description` |
| KP paths (Step 2) | `skills/{name}/references/...` | `.github/skills/{name}/SKILL.md` |
| Quality gates path | `rules/05-quality-gates.md` | `.github/instructions/05-quality-gates.instructions.md` |
| Compile step (Step 3) | Present (`{{COMPILE_COMMAND}} + {{BUILD_COMMAND}}`) | Absent |
| Common mistakes ref | Present in Step 2 | Absent |
| Integration Notes | `/x-review` = specialist, `/x-review-pr` = Tech Lead | "Detailed References" section with `.github/skills/` links |

The TDD rubric content (category K items, GO/NO-GO logic, point totals) must be **identical** in both copies.

---

### 15. Out of Scope

- Modifying tech-lead agent templates (already updated in story-0003-0006)
- Modifying x-dev-lifecycle templates (follow-up to align "40-point" reference)
- Modifying code-review prompt template (follow-up)
- Modifying testing KP SKILL.md or reference files
- Modifying any TypeScript source code (`src/**/*.ts`)
- Modifying test infrastructure (`byte-for-byte.test.ts`, `integration-constants.ts`)
- Modifying pipeline logic or assemblers
- Adding new profiles or config templates
