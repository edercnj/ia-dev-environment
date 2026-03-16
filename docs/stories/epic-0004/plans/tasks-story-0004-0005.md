# Task Breakdown: story-0004-0005 — Documentation Phase in x-dev-lifecycle

**Story:** [story-0004-0005](../story-0004-0005.md)
**Plan:** [plan-story-0004-0005](./plan-story-0004-0005.md)
**Date:** 2026-03-15
**Mode:** TDD (RED/GREEN/REFACTOR per task)

## Summary

This story inserts a new Phase 3 (Documentation) into the x-dev-lifecycle SKILL.md template, renumbers all subsequent phases (+1), and updates all cross-references. Two source templates and 8 golden files are affected. No TypeScript source code changes are required.

**Files modified (Source of Truth):**
1. `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` (Claude template)
2. `resources/github-skills-templates/dev/x-dev-lifecycle.md` (GitHub template)

**Files regenerated (Golden Files):**
3-10. `tests/golden/{profile}/.agents/skills/x-dev-lifecycle/SKILL.md` (8 profiles)

---

## Task Dependency Graph

```
TASK-1 ──┐
TASK-2 ──┼──> TASK-4 ──> TASK-5 ──> TASK-6 ──> TASK-7 ──> TASK-8 ──> TASK-9 ──> TASK-10 ──> TASK-11
TASK-3 ──┘
```

---

## TASK-1: Update header — "9 phases (0-8)"

**Description:** Change the CRITICAL EXECUTION RULE header from "8 phases (0-7)" to "9 phases (0-8)" and update the phase progress format from `N/7` to `N/8` in the Claude template.

**Files to modify:**
- `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`

**TDD Cycle:**
- **RED:** Write a test (or assertion in a test harness file) that reads the Claude template and asserts:
  - Contains `**9 phases (0-8). ALL mandatory. NEVER stop before Phase 8.**`
  - Contains `>>> Phase N/8 completed. Proceeding to Phase N+1...`
  - Does NOT contain `8 phases (0-7)`
  - Does NOT contain `N/7 completed`
- **GREEN:** Edit lines 23 and 25 of the Claude template:
  - `**8 phases (0-7). ALL mandatory. NEVER stop before Phase 7.**` -> `**9 phases (0-8). ALL mandatory. NEVER stop before Phase 8.**`
  - `After EACH phase: \`>>> Phase N/7 completed. Proceeding to Phase N+1...\`` -> `After EACH phase: \`>>> Phase N/8 completed. Proceeding to Phase N+1...\``
- **REFACTOR:** None needed (single-line text changes).

**Depends On:** None
**Parallel:** yes (independent of TASK-2, TASK-3)

---

## TASK-2: Update "ONLY legitimate stopping point" — Phase 7 to Phase 8

**Description:** Update the footer rule that declares the only legitimate stopping point from Phase 7 to Phase 8 in the Claude template.

**Files to modify:**
- `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`

**TDD Cycle:**
- **RED:** Assert the Claude template contains `**Phase 8 is the ONLY legitimate stopping point.**` and does NOT contain `**Phase 7 is the ONLY legitimate stopping point.**`
- **GREEN:** Edit line 240:
  - `**Phase 7 is the ONLY legitimate stopping point.**` -> `**Phase 8 is the ONLY legitimate stopping point.**`
- **REFACTOR:** None needed.

**Depends On:** None
**Parallel:** yes (independent of TASK-1, TASK-3)

---

## TASK-3: Update Complete Flow block — add Documentation line, renumber

**Description:** Insert `Phase 3: Documentation` into the Complete Flow code block and renumber all subsequent phases within that block.

**Files to modify:**
- `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`

**TDD Cycle:**
- **RED:** Assert the Complete Flow block contains exactly:
  ```
  Phase 0: Preparation          (orchestrator — inline)
  Phase 1: Planning              (subagent — reads architecture KPs)
  Phase 1B-1E: Parallel Planning (up to 4 subagents — SINGLE message)
  Phase 2: Implementation        (subagent — reads coding + layer KPs)
  Phase 3: Documentation         (orchestrator — inline)
  Phase 4: Review                (invoke /x-review skill — launches its own subagents)
  Phase 5-6: Fixes + PR          (orchestrator — inline)
  Phase 7: Tech Lead Review      (invoke /x-review-pr skill)
  Phase 8: Verification          (orchestrator — inline)
  ```
- **GREEN:** Replace lines 29-38 of the Claude template with the new block (add Phase 3 line, renumber Phase 3->4, Phase 4-5->5-6, Phase 6->7, Phase 7->8).
- **REFACTOR:** None needed.

**Depends On:** None
**Parallel:** yes (independent of TASK-1, TASK-2)

---

## TASK-4: Insert new Phase 3 — Documentation section

**Description:** Insert a new `## Phase 3 — Documentation (Orchestrator — Inline)` section between the current Phase 2 (Implementation, ending around line 188) and the current Phase 3 (Parallel Review, line 190). The section defines the dispatch mechanism that reads `interfaces` from the project identity and invokes corresponding documentation generators.

**Files to modify:**
- `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`

**TDD Cycle:**
- **RED:** Assert the Claude template contains a section matching `## Phase 3 — Documentation` with the following content markers:
  - Contains `Read \`interfaces\` field from project identity`
  - Contains `rest` -> `OpenAPI/Swagger generator`
  - Contains `grpc` -> `gRPC/Proto doc generator`
  - Contains `cli` -> `CLI doc generator`
  - Contains `websocket`, `event-consumer`, `event-producer` -> `Event-Driven doc generator`
  - Contains `No documentable interfaces configured`
  - Contains `Generate changelog entry`
  - Contains `CHANGELOG.md`
  - Section appears AFTER `## Phase 2` content and BEFORE what will become `## Phase 4`
- **GREEN:** Insert the following new section before the current `## Phase 3 — Parallel Review` (which has not yet been renumbered):

```markdown
## Phase 3 — Documentation (Orchestrator — Inline)

1. Read `interfaces` field from project identity (Rule 01)
2. Build list of documentable interfaces: `rest`, `grpc`, `graphql`, `cli`, `websocket`, `event-consumer`, `event-producer`
3. For each configured interface, dispatch the corresponding documentation generator:
   - `rest` → OpenAPI/Swagger generator (story-0004-0007)
   - `grpc` → gRPC/Proto doc generator (story-0004-0008)
   - `cli` → CLI doc generator (story-0004-0009)
   - `websocket`, `event-consumer`, `event-producer` → Event-Driven doc generator (story-0004-0010)
4. If no documentable interfaces configured: emit log `"No documentable interfaces configured. Skipping interface documentation."`
5. Generate changelog entry (ALWAYS, regardless of interfaces):
   - Read commits since branch point (`git log main..HEAD --oneline`)
   - Group by Conventional Commits type (feat, fix, refactor, test, docs, chore)
   - Append formatted entry to `CHANGELOG.md`
6. Documentation output saved to `docs/` with subdirectories per type (RULE-009):
   - API docs → `docs/api/`
   - Architecture docs → `docs/architecture/`
```

- **REFACTOR:** Verify spacing and blank lines are consistent with the style of other Phase sections.

**Depends On:** TASK-1, TASK-2, TASK-3 (header/footer must be updated first to avoid conflicting edits)
**Parallel:** no

---

## TASK-5: Renumber Phase 3 (Review) to Phase 4

**Description:** Rename the current `## Phase 3 — Parallel Review` section header to `## Phase 4 — Parallel Review`. Update the internal reference in the Fixes phase that says "Phase 3 review" to "Phase 4 review". Update the Roles table entry from `Phase 3` to `Phase 4` for Specialist Reviews.

**Files to modify:**
- `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`

**TDD Cycle:**
- **RED:** Assert:
  - Template contains `## Phase 4 — Parallel Review`
  - Template does NOT contain `## Phase 3 — Parallel Review`
  - Template contains `Phase 4 review` in the Fixes section (cross-reference)
  - Roles table row for Specialist Reviews shows `Phase 4`
- **GREEN:**
  - `## Phase 3 — Parallel Review` -> `## Phase 4 — Parallel Review`
  - `Fix ALL failed items from Phase 3 review` -> `Fix ALL failed items from Phase 4 review`
  - Roles table: `| Specialist Reviews | Phase 3 |` -> `| Specialist Reviews | Phase 4 |`
- **REFACTOR:** None needed.

**Depends On:** TASK-4
**Parallel:** no (sequential renumbering)

---

## TASK-6: Renumber Phase 4 (Fixes) to Phase 5 and Phase 5 (PR) to Phase 6

**Description:** Rename `## Phase 4 — Fixes + Feedback` to `## Phase 5 — Fixes + Feedback` and `## Phase 5 — Commit & PR` to `## Phase 6 — Commit & PR`.

**Files to modify:**
- `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`

**TDD Cycle:**
- **RED:** Assert:
  - Template contains `## Phase 5 — Fixes + Feedback`
  - Template contains `## Phase 6 — Commit & PR`
  - Template does NOT contain `## Phase 4 — Fixes + Feedback`
  - Template does NOT contain `## Phase 5 — Commit & PR`
- **GREEN:**
  - `## Phase 4 — Fixes + Feedback` -> `## Phase 5 — Fixes + Feedback`
  - `## Phase 5 — Commit & PR` -> `## Phase 6 — Commit & PR`
- **REFACTOR:** None needed.

**Depends On:** TASK-5
**Parallel:** no (sequential renumbering)

---

## TASK-7: Renumber Phase 6 (Tech Lead) to Phase 7

**Description:** Rename `## Phase 6 — Tech Lead Review` to `## Phase 7 — Tech Lead Review`. Update the Roles table entry from `Phase 6` to `Phase 7` for Tech Lead.

**Files to modify:**
- `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`

**TDD Cycle:**
- **RED:** Assert:
  - Template contains `## Phase 7 — Tech Lead Review`
  - Template does NOT contain `## Phase 6 — Tech Lead Review`
  - Roles table row for Tech Lead shows `Phase 7`
- **GREEN:**
  - `## Phase 6 — Tech Lead Review` -> `## Phase 7 — Tech Lead Review`
  - Roles table: `| Tech Lead | Phase 6 |` -> `| Tech Lead | Phase 7 |`
- **REFACTOR:** None needed.

**Depends On:** TASK-6
**Parallel:** no (sequential renumbering)

---

## TASK-8: Renumber Phase 7 (Verification) to Phase 8

**Description:** Rename `## Phase 7 — Final Verification + Cleanup` to `## Phase 8 — Final Verification + Cleanup`. This is the last renumbering step for the Claude template.

**Files to modify:**
- `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`

**TDD Cycle:**
- **RED:** Assert:
  - Template contains `## Phase 8 — Final Verification + Cleanup`
  - Template does NOT contain `## Phase 7 — Final Verification + Cleanup`
- **GREEN:**
  - `## Phase 7 — Final Verification + Cleanup` -> `## Phase 8 — Final Verification + Cleanup`
- **REFACTOR:** Review the entire Claude template end-to-end for any remaining old phase number references. Verify no stale `Phase 7` or `Phase 3` references remain (except within the new Phase 3 Documentation content which references downstream stories).

**Depends On:** TASK-7
**Parallel:** no (sequential renumbering)

---

## TASK-9: Apply all changes to GitHub template (RULE-001 dual copy)

**Description:** Apply the same set of changes (TASK-1 through TASK-8) to the GitHub Copilot template. The GitHub template has a slightly different structure: different YAML frontmatter format, `.github/skills/` path references, and a different progress reporting format (`After each of Phases 0-6:` / `After Phase 7:`).

**Files to modify:**
- `resources/github-skills-templates/dev/x-dev-lifecycle.md`

**TDD Cycle:**
- **RED:** Assert the GitHub template contains:
  - `**9 phases (0-8). ALL mandatory. NEVER stop before Phase 8.**`
  - `After each of Phases 0–7:` `>>> Phase N/8 completed. Proceeding to Phase N+1...`
  - `After Phase 8:` `>>> Phase 8/8 completed. Lifecycle complete.`
  - `Phase 3: Documentation         (orchestrator — inline)` in Complete Flow
  - `## Phase 3 — Documentation (Orchestrator — Inline)` section with dispatch content
  - `## Phase 4 — Parallel Review`
  - `## Phase 5 — Fixes + Feedback`
  - `## Phase 6 — Commit & PR`
  - `## Phase 7 — Tech Lead Review`
  - `## Phase 8 — Final Verification + Cleanup`
  - `**Phase 8 is the ONLY legitimate stopping point.**`
  - `Fix ALL failed items from Phase 4 review`
  - Roles table: Specialist Reviews at `Phase 4`, Tech Lead at `Phase 7`
  - Does NOT contain any stale `Phase 7 is the ONLY` or `8 phases (0-7)` references
- **GREEN:** Apply all changes to the GitHub template:
  1. Header: `8 phases (0-7)` -> `9 phases (0-8)`, `NEVER stop before Phase 7` -> `Phase 8`
  2. Progress format: `Phases 0–6:` -> `Phases 0–7:`, `N/7` -> `N/8`, `After Phase 7:` -> `After Phase 8:`, `Phase 7/7` -> `Phase 8/8`
  3. Complete Flow block: add Documentation line, renumber
  4. Insert new Phase 3 Documentation section (identical content to Claude template)
  5. Renumber all section headers: Phase 3->4, Phase 4->5, Phase 5->6, Phase 6->7, Phase 7->8
  6. Cross-references: `Phase 3 review` -> `Phase 4 review`, Roles table phase numbers
  7. Footer: `Phase 7 is the ONLY` -> `Phase 8 is the ONLY`
- **REFACTOR:** Side-by-side comparison of both templates to verify content parity (accounting for structural differences like YAML frontmatter format and path prefixes).

**Depends On:** TASK-8 (Claude template must be complete first, to use as reference)
**Parallel:** no

---

## TASK-10: Regenerate all 8 golden files

**Description:** Run the generation pipeline for all 8 profiles to regenerate golden files. The byte-for-byte test (`tests/node/integration/byte-for-byte.test.ts`) compares pipeline output against golden files, so the golden files must be updated to match the new template content.

**Files to regenerate:**
- `tests/golden/go-gin/.agents/skills/x-dev-lifecycle/SKILL.md`
- `tests/golden/java-quarkus/.agents/skills/x-dev-lifecycle/SKILL.md`
- `tests/golden/java-spring/.agents/skills/x-dev-lifecycle/SKILL.md`
- `tests/golden/kotlin-ktor/.agents/skills/x-dev-lifecycle/SKILL.md`
- `tests/golden/python-click-cli/.agents/skills/x-dev-lifecycle/SKILL.md`
- `tests/golden/python-fastapi/.agents/skills/x-dev-lifecycle/SKILL.md`
- `tests/golden/rust-axum/.agents/skills/x-dev-lifecycle/SKILL.md`
- `tests/golden/typescript-nestjs/.agents/skills/x-dev-lifecycle/SKILL.md`

**TDD Cycle:**
- **RED:** The existing byte-for-byte tests are already RED at this point (templates changed but golden files are stale). This is the expected state.
- **GREEN:** Run `npm run generate` (or equivalent pipeline command) for each of the 8 profiles, writing output to the golden directories. Alternatively, copy the updated SKILL.md directly to each golden directory since the x-dev-lifecycle template has no profile-specific placeholder substitution.
- **REFACTOR:** Verify all 8 golden files are identical to each other (since x-dev-lifecycle uses no profile-specific placeholders, all golden copies should be byte-for-byte identical).

**Depends On:** TASK-9 (both templates must be finalized)
**Parallel:** no

---

## TASK-11: Run full test suite — final validation

**Description:** Run the complete test suite (`npm test`) to validate that all byte-for-byte golden file tests pass, no regressions in other tests, and coverage thresholds are met.

**Validation checklist:**
- [ ] All 8 profile golden file tests pass (byte-for-byte parity)
- [ ] No missing files in golden file comparison
- [ ] No extra files in golden file comparison
- [ ] All other existing tests remain passing (no regressions)
- [ ] Coverage: line >= 95%, branch >= 90%

**TDD Cycle:**
- This is the final GREEN verification. No new tests are written here; all previously RED tests (from TASK-1 through TASK-9) should now be GREEN.
- **Command:** `npm test`

**Depends On:** TASK-10
**Parallel:** no

---

## Cross-Reference Summary

All locations in the Claude template (`resources/skills-templates/core/x-dev-lifecycle/SKILL.md`) requiring changes:

| Line (approx.) | Current Text | New Text | Task |
|:---:|:---|:---|:---:|
| 23 | `**8 phases (0-7). ALL mandatory. NEVER stop before Phase 7.**` | `**9 phases (0-8). ALL mandatory. NEVER stop before Phase 8.**` | TASK-1 |
| 25 | `>>> Phase N/7 completed.` | `>>> Phase N/8 completed.` | TASK-1 |
| 34 | `Phase 3: Review` | `Phase 4: Review` | TASK-3 |
| 35 | `Phase 4-5: Fixes + PR` | `Phase 5-6: Fixes + PR` | TASK-3 |
| 36 | `Phase 6: Tech Lead Review` | `Phase 7: Tech Lead Review` | TASK-3 |
| 37 | `Phase 7: Verification` | `Phase 8: Verification` | TASK-3 |
| — | *(new line in block)* | `Phase 3: Documentation (orchestrator — inline)` | TASK-3 |
| 190 | *(insert before)* | `## Phase 3 — Documentation (Orchestrator — Inline)` + content | TASK-4 |
| 190 | `## Phase 3 — Parallel Review` | `## Phase 4 — Parallel Review` | TASK-5 |
| 200 | `Phase 3 review` | `Phase 4 review` | TASK-5 |
| 198 | `## Phase 4 — Fixes + Feedback` | `## Phase 5 — Fixes + Feedback` | TASK-6 |
| 206 | `## Phase 5 — Commit & PR` | `## Phase 6 — Commit & PR` | TASK-6 |
| 214 | `## Phase 6 — Tech Lead Review` | `## Phase 7 — Tech Lead Review` | TASK-7 |
| 220 | `## Phase 7 — Final Verification + Cleanup` | `## Phase 8 — Final Verification + Cleanup` | TASK-8 |
| 240 | `**Phase 7 is the ONLY legitimate stopping point.**` | `**Phase 8 is the ONLY legitimate stopping point.**` | TASK-2 |
| 249 | `Specialist Reviews \| Phase 3` | `Specialist Reviews \| Phase 4` | TASK-5 |
| 250 | `Tech Lead \| Phase 6` | `Tech Lead \| Phase 7` | TASK-7 |

All locations in the GitHub template (`resources/github-skills-templates/dev/x-dev-lifecycle.md`) requiring changes:

| Line (approx.) | Current Text | New Text | Task |
|:---:|:---|:---|:---:|
| 19 | `**8 phases (0-7). ALL mandatory. NEVER stop before Phase 7.**` | `**9 phases (0-8). ALL mandatory. NEVER stop before Phase 8.**` | TASK-9 |
| 21 | `After each of Phases 0–6:` `>>> Phase N/7` | `After each of Phases 0–7:` `>>> Phase N/8` | TASK-9 |
| 22 | `After Phase 7:` `>>> Phase 7/7 completed. Lifecycle complete.` | `After Phase 8:` `>>> Phase 8/8 completed. Lifecycle complete.` | TASK-9 |
| 31 | `Phase 3: Review` | `Phase 4: Review` | TASK-9 |
| 32 | `Phase 4-5: Fixes + PR` | `Phase 5-6: Fixes + PR` | TASK-9 |
| 33 | `Phase 6: Tech Lead Review` | `Phase 7: Tech Lead Review` | TASK-9 |
| 34 | `Phase 7: Verification` | `Phase 8: Verification` | TASK-9 |
| — | *(new line in block)* | `Phase 3: Documentation (orchestrator — inline)` | TASK-9 |
| 186 | *(insert before)* | `## Phase 3 — Documentation (Orchestrator — Inline)` + content | TASK-9 |
| 186 | `## Phase 3 — Parallel Review` | `## Phase 4 — Parallel Review` | TASK-9 |
| 196 | `Phase 3 review` | `Phase 4 review` | TASK-9 |
| 194 | `## Phase 4 — Fixes + Feedback` | `## Phase 5 — Fixes + Feedback` | TASK-9 |
| 202 | `## Phase 5 — Commit & PR` | `## Phase 6 — Commit & PR` | TASK-9 |
| 210 | `## Phase 6 — Tech Lead Review` | `## Phase 7 — Tech Lead Review` | TASK-9 |
| 216 | `## Phase 7 — Final Verification + Cleanup` | `## Phase 8 — Final Verification + Cleanup` | TASK-9 |
| 236 | `**Phase 7 is the ONLY legitimate stopping point.**` | `**Phase 8 is the ONLY legitimate stopping point.**` | TASK-9 |
| 245 | `Specialist Reviews \| Phase 3` | `Specialist Reviews \| Phase 4` | TASK-9 |
| 246 | `Tech Lead \| Phase 6` | `Tech Lead \| Phase 7` | TASK-9 |

---

## Implementation Notes

1. **No TypeScript source code changes required.** The assemblers (`SkillsAssembler`, `GithubSkillsAssembler`) copy templates verbatim. The `x-dev-lifecycle` template uses only `{{PLACEHOLDER}}` tokens that are runtime markers, not resolved during generation.

2. **Golden files are identical across profiles** for `x-dev-lifecycle` because no profile-specific substitution occurs. Regeneration can be done by running the pipeline or by directly copying the updated template.

3. **Testing strategy:** Since this is a Markdown template change (not TypeScript code), the primary tests are the byte-for-byte golden file integration tests. Additional assertions on specific content patterns (phase count, phase names, cross-references) can be added as unit-level content validation tests if desired, but the golden file tests provide complete coverage.

4. **Renumbering order matters.** Tasks 5-8 must execute sequentially because each rename could create transient ambiguity (e.g., renaming Phase 4 before Phase 3 is renamed could create two Phase 4 sections). The prescribed order (Phase 3->4, then Phase 4->5, etc.) avoids this.
