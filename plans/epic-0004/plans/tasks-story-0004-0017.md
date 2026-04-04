# Task Breakdown: Post-Deploy Verification Step

**Story:** story-0004-0017
**Epic:** EPIC-0004 (Feature Lifecycle Evolution)
**Date:** 2026-03-16
**Type:** Template-only change (Markdown content)

---

## Summary

This story adds a post-deploy verification sub-step to Phase 7 of the `x-dev-lifecycle` skill.
No TypeScript source code changes are required. All work is template content, golden file updates,
and test verification.

**Total tasks:** 7
**Estimated effort:** Low (template-only, no logic changes)

---

## Task Dependency Graph

```
TASK-1 (Claude template) ──┐
                            ├──> TASK-3 (regenerate golden files) ──> TASK-5 (run tests)
TASK-2 (GitHub template) ──┘                                              │
                                                                          v
TASK-4 (dual copy diff) ──────────────────────────────────────────> TASK-6 (generated outputs)
                                                                          │
                                                                          v
                                                                    TASK-7 (final verification)
```

---

## TASK-1: Add post-deploy verification to Claude Code lifecycle template

**Description:** Modify the source-of-truth template for the Claude Code `x-dev-lifecycle` skill.
Add a post-deploy verification sub-step within Phase 7, a new conditional DoD item, and renumber
subsequent items.

**Files to modify:**
- `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`

**Changes:**
1. In Phase 7, add item 6 "Post-Deploy Verification" between current item 5 (Conditional DoD items) and item 6 (Report PASS/FAIL):
   - Health Check: GET /health (or configured endpoint) -> 200 OK
   - Critical Path: Execute primary request flow -> valid response
   - Response Time: Verify p95 latency < configured SLO
   - Error Rate: Verify error rate < 1% threshold
   - Result: PASS / FAIL / SKIP with details
   - Non-blocking: emit result for human decision; do NOT auto-rollback
   - Reference to `/run-e2e` or `/run-smoke-api` for automated verification
   - Conditional on `testing.smoke_tests == true` (runtime check, always present in template)
2. Add to existing item 5 (Conditional DoD items):
   - `Post-deploy verification passed or skipped (if testing.smoke_tests == true)`
3. Renumber items 6-7 to 7-8

**Depends On:** None (first task)
**Parallel:** yes (with TASK-2)
**Complexity:** Low

---

## TASK-2: Mirror post-deploy verification to GitHub Copilot lifecycle template

**Description:** Apply the same content changes from TASK-1 to the GitHub Copilot dual copy
template. Adjust skill path references from `skills/` to `.github/skills/` where applicable
(matching existing conventions in the GitHub template).

**Files to modify:**
- `resources/github-skills-templates/dev/x-dev-lifecycle.md`

**Changes:**
1. Same Phase 7 additions as TASK-1 (post-deploy verification sub-step)
2. Same conditional DoD item addition
3. Same renumbering (items 6-7 become 7-8)
4. Verify path references use `.github/skills/` prefix (consistent with existing GitHub template)

**Depends On:** None (first task)
**Parallel:** yes (with TASK-1)
**Complexity:** Low

---

## TASK-3: Regenerate golden files for all 8 profiles

**Description:** Run the generation pipeline for all 8 profiles and update the golden files.
Since the template has no Nunjucks conditionals for this section (it uses runtime conditions),
all 8 profiles will produce identical lifecycle output. Each profile has 3 output targets
(.claude, .github, .agents) = 24 golden files total.

**Files to modify (24 files):**

Claude Code golden files (8):
- `tests/golden/go-gin/.claude/skills/x-dev-lifecycle/SKILL.md`
- `tests/golden/java-quarkus/.claude/skills/x-dev-lifecycle/SKILL.md`
- `tests/golden/java-spring/.claude/skills/x-dev-lifecycle/SKILL.md`
- `tests/golden/kotlin-ktor/.claude/skills/x-dev-lifecycle/SKILL.md`
- `tests/golden/python-click-cli/.claude/skills/x-dev-lifecycle/SKILL.md`
- `tests/golden/python-fastapi/.claude/skills/x-dev-lifecycle/SKILL.md`
- `tests/golden/rust-axum/.claude/skills/x-dev-lifecycle/SKILL.md`
- `tests/golden/typescript-nestjs/.claude/skills/x-dev-lifecycle/SKILL.md`

GitHub Copilot golden files (8):
- `tests/golden/go-gin/.github/skills/x-dev-lifecycle/SKILL.md`
- `tests/golden/java-quarkus/.github/skills/x-dev-lifecycle/SKILL.md`
- `tests/golden/java-spring/.github/skills/x-dev-lifecycle/SKILL.md`
- `tests/golden/kotlin-ktor/.github/skills/x-dev-lifecycle/SKILL.md`
- `tests/golden/python-click-cli/.github/skills/x-dev-lifecycle/SKILL.md`
- `tests/golden/python-fastapi/.github/skills/x-dev-lifecycle/SKILL.md`
- `tests/golden/rust-axum/.github/skills/x-dev-lifecycle/SKILL.md`
- `tests/golden/typescript-nestjs/.github/skills/x-dev-lifecycle/SKILL.md`

Agents golden files (8):
- `tests/golden/go-gin/.agents/skills/x-dev-lifecycle/SKILL.md`
- `tests/golden/java-quarkus/.agents/skills/x-dev-lifecycle/SKILL.md`
- `tests/golden/java-spring/.agents/skills/x-dev-lifecycle/SKILL.md`
- `tests/golden/kotlin-ktor/.agents/skills/x-dev-lifecycle/SKILL.md`
- `tests/golden/python-click-cli/.agents/skills/x-dev-lifecycle/SKILL.md`
- `tests/golden/python-fastapi/.agents/skills/x-dev-lifecycle/SKILL.md`
- `tests/golden/rust-axum/.agents/skills/x-dev-lifecycle/SKILL.md`
- `tests/golden/typescript-nestjs/.agents/skills/x-dev-lifecycle/SKILL.md`

**Approach:** Run the pipeline per profile, copy the generated `x-dev-lifecycle/SKILL.md` to each
golden file location. Alternatively, since the lifecycle template has no profile-specific placeholders,
the .claude golden files will be identical to the source template, the .github golden files will
match the GitHub template, and the .agents files will mirror the .claude files.

**Depends On:** TASK-1, TASK-2
**Parallel:** no (must wait for both templates to be finalized)
**Complexity:** Low (mechanical copy, but touches 24 files)

---

## TASK-4: Verify dual copy consistency

**Description:** Diff the two templates (Claude Code vs GitHub Copilot) to ensure content parity.
The only acceptable differences are:
- YAML frontmatter format (Claude uses full frontmatter, GitHub uses description block)
- Skill path references (`skills/` vs `.github/skills/`)
- Detailed References section at the bottom (GitHub-specific)

The post-deploy verification content itself must be semantically identical.

**Files to compare:**
- `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`
- `resources/github-skills-templates/dev/x-dev-lifecycle.md`

**Depends On:** TASK-1, TASK-2
**Parallel:** yes (with TASK-3)
**Complexity:** Low

---

## TASK-5: Run test suite and verify byte-for-byte parity

**Description:** Run `npm test` to execute the full test suite. The byte-for-byte integration test
(`byte-for-byte.test.ts`) will validate that generated output matches the updated golden files
for all 8 profiles across all 3 output targets.

**Command:** `npm test`

**Expected result:** All 1,384+ tests pass with coverage >= 95% line, >= 90% branch.

**Depends On:** TASK-3
**Parallel:** no (must wait for golden files to be updated)
**Complexity:** Low

---

## TASK-6: Update generated outputs (dogfood)

**Description:** Regenerate the project's own `.claude/` and `.agents/` outputs since
ia-dev-environment dogfoods its own generated configuration. The `.github/` directory for
this project does not contain a lifecycle SKILL.md at root level (no match found), so only
.claude and .agents need updating.

**Files to modify:**
- `.claude/skills/x-dev-lifecycle/SKILL.md`
- `.agents/skills/x-dev-lifecycle/SKILL.md`

**Depends On:** TASK-5 (tests must pass first)
**Parallel:** no
**Complexity:** Low

---

## TASK-7: Final verification and commit readiness

**Description:** Perform final checks before marking the story as implementation-complete.

**Checklist:**
1. Both templates (Claude + GitHub) contain the post-deploy verification sub-step
2. Dual copy consistency verified (TASK-4)
3. All 24 golden files updated and matching generated output
4. Test suite green (`npm test`)
5. Generated outputs (.claude, .agents) updated
6. No TypeScript source code modified (template-only change)
7. Content is in English (RULE-012)
8. Backward compatible: projects with `smoke_tests: false` see the section but agent skips at runtime (RULE-003)

**Depends On:** TASK-5, TASK-6
**Parallel:** no (final gate)
**Complexity:** Low

---

## Execution Order (Optimal)

```
Step 1 (parallel):  TASK-1 + TASK-2     (modify both templates simultaneously)
Step 2 (parallel):  TASK-3 + TASK-4     (regenerate golden files + verify dual copy)
Step 3 (sequential): TASK-5              (run tests)
Step 4 (sequential): TASK-6              (update generated outputs)
Step 5 (sequential): TASK-7              (final verification)
```

**Total sequential steps:** 5
**Parallelizable tasks:** 2 groups (TASK-1+2, TASK-3+4)

---

## Risk Summary

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Golden file mismatch | Medium | Low | Regenerate via pipeline, byte-for-byte test catches drift |
| Dual copy divergence | Low | Medium | TASK-4 explicit diff check |
| Merge conflict with story-0004-0013 | Low | Low | Different phases targeted (Phase 1 vs Phase 7) |
| Phase numbering error | Low | Low | Plan specifies exact renumbering (items 6-7 become 7-8) |
