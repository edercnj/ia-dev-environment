# Implementation Plan — story-0003-0009

## x-story-create — Enriched Gherkin with Degenerate & Boundary Cases

### 1. Summary

This story modifies the `x-story-create` skill templates to instruct the AI to generate enriched Gherkin scenarios with mandatory categories (degenerate cases, happy path, error paths, boundary values), TPP ordering, and a minimum of 4 scenarios validation. The changes are purely content edits to skill instruction files — no TypeScript code or pipeline logic changes.

Three source-of-truth files are modified:
1. `resources/skills-templates/core/x-story-create/SKILL.md` (Claude copy)
2. `resources/github-skills-templates/story/x-story-create.md` (GitHub copy)
3. `resources/templates/_TEMPLATE-STORY.md` (story template — optional, see Section 3.3)

The pipeline copies these verbatim (no template substitution) to the output, producing 24 golden files that must be updated.

---

### 2. Architecture — How the Triple Copy System Works

```
resources/skills-templates/core/x-story-create/SKILL.md   <-- SOURCE OF TRUTH (Claude)
        |
        | fs.copyFileSync (no transformation, no {{placeholders}})
        | via: src/assembler/skills-assembler.ts :: assembleCore()
        v
{outputDir}/skills/x-story-create/SKILL.md                <-- pipeline output (.claude)
        |
        | fs.copyFileSync (mirror from .claude/skills/)
        | via: src/assembler/codex-skills-assembler.ts
        v
{outputDir}/.agents/skills/x-story-create/SKILL.md        <-- pipeline output (.agents)

---

resources/github-skills-templates/story/x-story-create.md  <-- SOURCE OF TRUTH (GitHub)
        |
        | fs.copyFileSync (rename to SKILL.md in subdirectory)
        | via: src/assembler/github-skills-assembler.ts
        v
{outputDir}/github/skills/x-story-create/SKILL.md         <-- pipeline output (.github)
```

Key properties:
- The Claude source template has **no `{{placeholders}}`** — the pipeline copies it byte-for-byte.
- The `.agents` copy is an exact mirror of the `.claude` copy (via `codex-skills-assembler`).
- The GitHub copy is a separate source file with different path references and English section headers (vs pt-BR in Claude copy).
- The `_TEMPLATE-STORY.md` is **NOT** part of the pipeline output — it is only referenced as a reading instruction inside the skill. Changes to it do NOT require golden file updates.
- All 8 profiles produce identical copies (the skill is profile-independent).

---

### 3. Affected Layers and Components

| Layer | Component | Impact |
|-------|-----------|--------|
| Resources (Claude source) | `resources/skills-templates/core/x-story-create/SKILL.md` | **MODIFIED** — add enriched Gherkin instructions |
| Resources (GitHub source) | `resources/github-skills-templates/story/x-story-create.md` | **MODIFIED** — parallel changes with English section headers and GitHub-specific paths |
| Resources (template) | `resources/templates/_TEMPLATE-STORY.md` | **EVALUATED** — may need Gherkin section update to show enriched categories (see 3.3) |
| Domain (routing) | `src/domain/core-kp-routing.ts` | **UNCHANGED** — no filename changes |
| Assembler (skills) | `src/assembler/skills-assembler.ts` | **UNCHANGED** — copy logic unchanged |
| Assembler (github-skills) | `src/assembler/github-skills-assembler.ts` | **UNCHANGED** — copy logic unchanged |
| Assembler (codex-skills) | `src/assembler/codex-skills-assembler.ts` | **UNCHANGED** — mirrors .claude output |
| Golden files (.claude) | `tests/golden/{profile}/.claude/skills/x-story-create/SKILL.md` | **MUST UPDATE** — 8 files |
| Golden files (.agents) | `tests/golden/{profile}/.agents/skills/x-story-create/SKILL.md` | **MUST UPDATE** — 8 files |
| Golden files (.github) | `tests/golden/{profile}/.github/skills/x-story-create/SKILL.md` | **MUST UPDATE** — 8 files |
| Tests | `tests/node/integration/byte-for-byte.test.ts` | **UNCHANGED** — test logic unchanged |

---

### 3.1 Changes to Claude Source Template

**File:** `resources/skills-templates/core/x-story-create/SKILL.md`

#### 3.1.1 Prerequisites Section — Add Rule 13 reference

**Location:** After the existing "Decomposition philosophy" bullet (line 36) in the Prerequisites section.

**Add:**

```markdown
**Gherkin completeness (mandatory categories and ordering):**
- `.claude/skills/story-planning/references/story-decomposition.md` — Read SD-02 (Gherkin Completeness Requirements) and SD-05a (Scenario Ordering)
```

#### 3.1.2 Section 7 — Replace Gherkin Requirements

**Location:** Lines 131-145 (Section 7 — Critérios de Aceite (Gherkin)).

**Replace the current "Required scenarios" block:**

```markdown
**Current:**
1. Happy path
2. Business rule violation
3. Malformed input
4. Edge case

**New:**
1. **Degenerate cases** (TPP Order 1): null input, empty collection, zero value, missing required field — at least 1 scenario
2. **Happy path** (TPP Order 2-3): main success flow with concrete values — at least 1 scenario
3. **Error paths** (TPP Order 4): business rule violations, validation failures, malformed input — at least 1 scenario per error type documented in the spec
4. **Boundary values** (TPP Order 5): triplet pattern (at-minimum, at-maximum, past-maximum) for each bounded input — at least 1 triplet
5. **Complex edge cases** (TPP Order 6): concurrency, timeout, state transitions — if applicable
```

**Add after the quality rules block a new sub-section:**

```markdown
**Scenario Ordering (TPP):**

Scenarios MUST be ordered following the Transformation Priority Premise:
1. Degenerate cases first (null, empty, zero)
2. Happy path (basic, then variations)
3. Error paths (business rules, validation)
4. Boundary values (at-min, at-max, past-max)
5. Complex edge cases (if applicable)

**Minimum Scenario Validation:**

Every story MUST contain at least 4 Gherkin scenarios. If a story has fewer than 4, either:
- Add degenerate/boundary/error scenarios to reach the minimum, OR
- Merge the story with another if scope is too narrow

If the story has no natural boundary values (no numeric/range parameters), the minimum of 4 MUST still be met with degenerate + happy + error scenarios.

> **Reference:** See Rule 13 (SD-02 and SD-05a) for full Gherkin completeness requirements and TPP ordering rationale.
```

#### 3.1.3 Sizing Heuristics — Update minimum

**Location:** Lines 181-183 (Sizing Heuristics — "Too small" section).

**Change:**

```markdown
**Before:** Less than 2 Gherkin scenarios
**After:** Less than 4 Gherkin scenarios
```

#### 3.1.4 Common Mistakes — Add new entries

**Location:** After line 195 (end of Common Mistakes section).

**Add:**

```markdown
- **Missing degenerate cases**: Every story must have at least one scenario testing null, empty, or zero input
- **Happy-path-first ordering**: Degenerate cases must come before happy path (TPP ordering)
- **Boundary values without triplet**: If a field has a valid range, test at-min, at-max, AND past-max — not just one
- **Fewer than 4 scenarios**: A story with 2-3 scenarios is under-specified; add degenerate/boundary/error scenarios
```

---

### 3.2 Changes to GitHub Source Template

**File:** `resources/github-skills-templates/story/x-story-create.md`

Apply semantically identical changes as Section 3.1, but with:
- English section headers (already in English)
- GitHub-specific path references (`.github/skills/...` instead of `.claude/skills/...`)
- Reference path for Rule 13: `resources/core/13-story-decomposition.md` (GitHub copy uses `resources/` paths)

The diff structure must mirror the Claude copy exactly, with only path prefixes differing.

---

### 3.3 Changes to Story Template (Optional)

**File:** `resources/templates/_TEMPLATE-STORY.md`

The current template Section 7 shows generic Gherkin placeholders:

```gherkin
Cenario: <Nome do cenário de sucesso>
Cenario: <Nome do cenário de erro>
Cenario: <Nome do cenário de edge case>
```

**Evaluation:** The template is intentionally minimal — it shows structure, not content. The enriched Gherkin instructions live in the **skill** (which tells the AI what to generate), not in the template (which shows the output format). The template's 3 generic scenarios serve as placeholders for any category.

**Decision:** Do NOT modify `_TEMPLATE-STORY.md`. The enriched Gherkin requirements are enforced through the skill instructions (Section 3.1 and 3.2), which override the template's minimal examples. This maintains the template as a structural guide and keeps the enrichment logic centralized in the skill.

**Rationale:** If the template were updated to show 6 categorized scenarios, it would create a maintenance coupling between Rule 13 and the template. The skill already instructs the AI to read Rule 13 directly, making the template redundant for this purpose.

---

### 4. Files to Modify (Exact Paths)

#### 4.1 Source of Truth (2 files)

| # | File | Change |
|---|------|--------|
| 1 | `resources/skills-templates/core/x-story-create/SKILL.md` | Add enriched Gherkin instructions (Section 3.1) |
| 2 | `resources/github-skills-templates/story/x-story-create.md` | Parallel changes with English headers (Section 3.2) |

#### 4.2 Golden Files — .claude (8 files, identical to source #1)

| # | File |
|---|------|
| 3 | `tests/golden/go-gin/.claude/skills/x-story-create/SKILL.md` |
| 4 | `tests/golden/java-quarkus/.claude/skills/x-story-create/SKILL.md` |
| 5 | `tests/golden/java-spring/.claude/skills/x-story-create/SKILL.md` |
| 6 | `tests/golden/kotlin-ktor/.claude/skills/x-story-create/SKILL.md` |
| 7 | `tests/golden/python-click-cli/.claude/skills/x-story-create/SKILL.md` |
| 8 | `tests/golden/python-fastapi/.claude/skills/x-story-create/SKILL.md` |
| 9 | `tests/golden/rust-axum/.claude/skills/x-story-create/SKILL.md` |
| 10 | `tests/golden/typescript-nestjs/.claude/skills/x-story-create/SKILL.md` |

#### 4.3 Golden Files — .agents (8 files, identical to source #1)

| # | File |
|---|------|
| 11 | `tests/golden/go-gin/.agents/skills/x-story-create/SKILL.md` |
| 12 | `tests/golden/java-quarkus/.agents/skills/x-story-create/SKILL.md` |
| 13 | `tests/golden/java-spring/.agents/skills/x-story-create/SKILL.md` |
| 14 | `tests/golden/kotlin-ktor/.agents/skills/x-story-create/SKILL.md` |
| 15 | `tests/golden/python-click-cli/.agents/skills/x-story-create/SKILL.md` |
| 16 | `tests/golden/python-fastapi/.agents/skills/x-story-create/SKILL.md` |
| 17 | `tests/golden/rust-axum/.agents/skills/x-story-create/SKILL.md` |
| 18 | `tests/golden/typescript-nestjs/.agents/skills/x-story-create/SKILL.md` |

#### 4.4 Golden Files — .github (8 files, identical to source #2)

| # | File |
|---|------|
| 19 | `tests/golden/go-gin/.github/skills/x-story-create/SKILL.md` |
| 20 | `tests/golden/java-quarkus/.github/skills/x-story-create/SKILL.md` |
| 21 | `tests/golden/java-spring/.github/skills/x-story-create/SKILL.md` |
| 22 | `tests/golden/kotlin-ktor/.github/skills/x-story-create/SKILL.md` |
| 23 | `tests/golden/python-click-cli/.github/skills/x-story-create/SKILL.md` |
| 24 | `tests/golden/python-fastapi/.github/skills/x-story-create/SKILL.md` |
| 25 | `tests/golden/rust-axum/.github/skills/x-story-create/SKILL.md` |
| 26 | `tests/golden/typescript-nestjs/.github/skills/x-story-create/SKILL.md` |

**Total: 26 files modified** (2 sources + 24 golden copies)

#### 4.5 Files Explicitly UNCHANGED

| File | Reason |
|------|--------|
| `resources/core/13-story-decomposition.md` | Already updated in story-0003-0004; this story only references it |
| `resources/templates/_TEMPLATE-STORY.md` | Template is structural only; enrichment lives in skill (see Section 3.3) |
| `src/assembler/skills-assembler.ts` | Copy logic unchanged; still byte-for-byte copy |
| `src/assembler/github-skills-assembler.ts` | Copy logic unchanged |
| `src/assembler/codex-skills-assembler.ts` | Mirror logic unchanged |
| `src/domain/core-kp-routing.ts` | No filename changes |
| `tests/node/integration/byte-for-byte.test.ts` | Test logic unchanged |
| `tests/helpers/integration-constants.ts` | No new profiles or constants |

---

### 5. New Classes/Interfaces to Create

None. This story is entirely content changes to existing Markdown files.

---

### 6. Dependency Direction Validation

Not applicable — no TypeScript code changes. The dependency direction between layers is unaffected because the modification targets only resource files (Markdown templates) that the pipeline copies verbatim.

---

### 7. Integration Points

| Integration Point | Description | Validation |
|-------------------|-------------|------------|
| Pipeline copy chain | `skills-assembler` copies Claude source to `.claude/skills/` output | Byte-for-byte test |
| Codex mirror | `codex-skills-assembler` mirrors `.claude/skills/` to `.agents/skills/` | Byte-for-byte test |
| GitHub copy chain | `github-skills-assembler` copies GitHub source to `.github/skills/` output | Byte-for-byte test |
| Rule 13 reference | Skill now instructs AI to read Rule 13 SD-02/SD-05a | Manual: verify path is correct in generated output |
| Dual copy consistency | Both source templates have semantically identical Gherkin enrichment | Manual: diff both sources and verify only expected differences (paths, language) |

---

### 8. Configuration Changes

None. No changes to `settings.json`, `settings.local.json`, config templates, or pipeline configuration.

---

### 9. Golden File Update Strategy

#### Strategy: Copy-After-Edit

1. Edit Claude source template (`resources/skills-templates/core/x-story-create/SKILL.md`)
2. Edit GitHub source template (`resources/github-skills-templates/story/x-story-create.md`)
3. Copy each source to its corresponding 16 (.claude + .agents) or 8 (.github) golden file locations
4. Run byte-for-byte test suite to verify parity

#### Mechanical execution:

```bash
# After editing the source templates:
CLAUDE_SRC="resources/skills-templates/core/x-story-create/SKILL.md"
GITHUB_SRC="resources/github-skills-templates/story/x-story-create.md"
PROFILES=(go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs)

for profile in "${PROFILES[@]}"; do
  cp "$CLAUDE_SRC" "tests/golden/$profile/.claude/skills/x-story-create/SKILL.md"
  cp "$CLAUDE_SRC" "tests/golden/$profile/.agents/skills/x-story-create/SKILL.md"
  cp "$GITHUB_SRC" "tests/golden/$profile/.github/skills/x-story-create/SKILL.md"
done
```

#### Verification:

```bash
npx vitest run tests/node/integration/byte-for-byte.test.ts
```

All 8 profiles x 5 test assertions = 40 test cases should pass.

---

### 10. Implementation Order

| Step | Action | Verification |
|------|--------|-------------|
| 1 | Edit Claude source: add Rule 13 prerequisite reference | Manual review |
| 2 | Edit Claude source: replace Section 7 required scenarios with enriched categories | Manual review |
| 3 | Edit Claude source: add TPP ordering and minimum validation sub-sections | Manual review |
| 4 | Edit Claude source: update sizing heuristics minimum (2 -> 4) | Manual review |
| 5 | Edit Claude source: add new common mistakes entries | Manual review |
| 6 | Edit GitHub source: apply parallel changes (English headers, GitHub paths) | `diff` both sources; verify only expected differences |
| 7 | Copy Claude source to 16 golden files (.claude + .agents) | `diff` confirmation |
| 8 | Copy GitHub source to 8 golden files (.github) | `diff` confirmation |
| 9 | Run byte-for-byte test suite | All 40 assertions pass |
| 10 | Run full test suite | All 1,384+ tests pass, coverage >= 95% line / >= 90% branch |

---

### 11. Risk Assessment

| Risk | Severity | Probability | Mitigation |
|------|----------|-------------|------------|
| Golden file mismatch after edit | HIGH | LOW | Mechanical copy script (Section 9) eliminates drift. Run byte-for-byte tests immediately after copy. |
| Inconsistency between Claude and GitHub source templates | HIGH | MEDIUM | After editing both, run a semantic diff to verify only path references and language differ. The enriched Gherkin content must be identical. |
| Skill becomes too long / verbose | LOW | MEDIUM | The additions are ~40 lines total. Current skill is 196 lines; new skill will be ~240 lines, well within the acceptable range for a skill instruction file. |
| Path references to Rule 13 incorrect | MEDIUM | LOW | The Claude copy references `.claude/skills/story-planning/references/story-decomposition.md` which may not exist in all project layouts. However, this is consistent with the existing pattern (other skills reference `.claude/` paths). The file is actually routed to `skills/story-planning/references/story-decomposition.md` in the output — the skill should reference that path instead. **ACTION:** Verify the correct runtime path for Rule 13 in the generated output and use that path. |
| `_TEMPLATE-STORY.md` stale vs enriched skill | LOW | LOW | Decided not to modify template (Section 3.3). If users follow the template literally instead of the skill instructions, they get minimal Gherkin. This is acceptable because the skill explicitly overrides the template's generic examples. |
| Backward compatibility — stories generated before this change | LOW | LOW | Previously generated stories remain valid. The change is additive: new stories will have enriched Gherkin, but old stories are not invalidated. |
| Merge conflict with other story-0003-XXXX branches | LOW | MEDIUM | This story modifies different files than story-0003-0004 (Rule 13) and story-0003-0005 (templates). No overlap expected. |

---

### 12. Backward Compatibility

This change is **purely additive**:
- No existing SKILL.md sections are removed
- The "Required scenarios" list is expanded (from 4 generic categories to 5 TPP-ordered categories)
- New sub-sections (TPP ordering, minimum validation) are appended after existing content
- Sizing heuristic minimum changes from 2 to 4 (tightening, not relaxing)
- New common mistakes are appended to existing list

Stories previously generated by the old skill remain valid. New stories generated after this change will include enriched Gherkin with mandatory categories and TPP ordering.

---

### 13. Out of Scope

- Modifying `resources/core/13-story-decomposition.md` (already done in story-0003-0004)
- Modifying `resources/templates/_TEMPLATE-STORY.md` (structural template; enrichment lives in skill)
- Modifying any TypeScript source code (`src/**/*.ts`)
- Modifying test infrastructure (`byte-for-byte.test.ts`, `integration-constants.ts`)
- Modifying pipeline logic or assemblers
- Adding new profiles or config templates

---

### 14. Difference Map Between Source Templates

For implementer reference, the Claude and GitHub source templates differ in these systematic ways:

| Aspect | Claude Template | GitHub Template |
|--------|----------------|-----------------|
| Section headers | Portuguese (`Dependências`, `Critérios de Aceite`) | English (`Dependencies`, `Acceptance Criteria`) |
| Template path | `.claude/templates/_TEMPLATE-STORY.md` | `resources/templates/_TEMPLATE-STORY.md` |
| Decomposition ref | `.claude/skills/x-story-epic-full/references/decomposition-guide.md` | `.github/skills/x-story-epic-full/SKILL.md` |
| Rule 13 ref (NEW) | `.claude/skills/story-planning/references/story-decomposition.md` | `resources/core/13-story-decomposition.md` or equivalent GitHub path |
| Quality def labels | `DoR Local`, `DoD Local`, `DoD Global` | `Local DoR`, `Local DoD`, `Global DoD` |
| Example text | Portuguese (`Enviar dados do cartão`) | English (`Send card data`) |
| Detailed References | Not present | Present at end of file |

All enriched Gherkin content (categories, TPP ordering, minimum validation, common mistakes) must be **semantically identical** in both copies.
