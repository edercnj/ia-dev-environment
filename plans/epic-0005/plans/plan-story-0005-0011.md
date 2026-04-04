# Implementation Plan -- story-0005-0011: Consolidation Final -- Review + Report + PR

**Story:** `story-0005-0011.md`
**Pattern Reference:** story-0005-0005 (Phase 1 core loop implementation)

---

## 1. Affected Files (Exact Paths)

### Template Content Files (PRIMARY -- the "implementation")

| # | File | Action |
|---|------|--------|
| 1 | `resources/skills-templates/core/x-dev-epic-implement/SKILL.md` | **MODIFY** -- Replace Phase 2 and Phase 3 placeholders with substantive content |
| 2 | `resources/github-skills-templates/dev/x-dev-epic-implement.md` | **MODIFY** -- Replace Phase 2 and Phase 3 placeholders with abbreviated content (GitHub mirror) |

### Test Files

| # | File | Action |
|---|------|--------|
| 3 | `tests/node/content/x-dev-epic-implement-content.test.ts` | **MODIFY** -- Invert placeholder test, add Phase 2 and Phase 3 content assertions, add dual-copy consistency terms |

### Golden Files (24 files total -- 3 copies x 8 profiles)

| # | Path Pattern | Action |
|---|-------------|--------|
| 4 | `tests/golden/{profile}/.claude/skills/x-dev-epic-implement/SKILL.md` | **REGENERATE** -- byte-for-byte copy of template #1 |
| 5 | `tests/golden/{profile}/.agents/skills/x-dev-epic-implement/SKILL.md` | **REGENERATE** -- byte-for-byte copy of template #1 |
| 6 | `tests/golden/{profile}/.github/skills/x-dev-epic-implement/SKILL.md` | **REGENERATE** -- byte-for-byte copy of template #2 |

Profiles (8): `go-gin`, `java-quarkus`, `java-spring`, `kotlin-ktor`, `python-click-cli`, `python-fastapi`, `rust-axum`, `typescript-nestjs`

### Extension Point Update (within existing Phase 1 content)

| # | File | Action |
|---|------|--------|
| 7 | Same as #1 and #2 above | **MODIFY** -- Remove `[Placeholder: consolidation + verification -- story-0005-0011]` from section 1.7 Extension Points |

### Integration Notes Update

| # | File | Action |
|---|------|--------|
| 8 | Same as #1 and #2 above | **MODIFY** -- Add `x-review-pr` and `gh pr create` to Integration Notes |

---

## 2. Changes Per File

### 2.1 `resources/skills-templates/core/x-dev-epic-implement/SKILL.md` (Claude/Agents template)

**Change A -- Replace Phase 2 placeholder (lines 244-249):**

Remove:
```markdown
## Phase 2 -- Consolidation

> **Placeholder**: This phase will be implemented in story-0005-0011.
> It will contain cross-story consolidation, branch merging, conflict
> resolution, and epic-level documentation generation.
```

Replace with full Phase 2 content containing these subsections:
- **2.1 Tech Lead Review Subagent** -- Dispatch subagent that executes `x-review-pr` logic on the full epic diff (branch vs main). Input: epic branch, base branch. Returns `{ score, decision, findings }` as `ReviewResult`.
- **2.2 Report Generation Subagent** -- Dispatch subagent that reads `_TEMPLATE-EPIC-EXECUTION-REPORT.md`, reads `execution-state.json`, resolves all `{{PLACEHOLDER}}` tokens, computes metrics (completion percentage, coverage delta, timeline), writes `epic-execution-report.md` to the epic directory.
- **2.3 PR Creation** -- Push branch via `git push -u origin feat/epic-{epicId}-full-implementation`. Create PR via `gh pr create` with title format `feat(epic): implement EPIC-{epicId} -- {title}`. PR body contains summary from report (stories completed/failed/blocked, coverage, tech lead score). If completion < 100%, PR title includes `[PARTIAL]`.
- **2.4 Partial Completion Handling** -- If stories are FAILED or BLOCKED, consolidation still executes. Report clearly shows incomplete stories. PR body indicates partial implementation. PR title includes `[PARTIAL]` marker.
- **2.5 Checkpoint Finalization** -- Register PR link and report path in checkpoint. Update `execution-state.json` with final metrics.

**Expected line count:** ~80-100 non-empty lines (comparable to Phase 1's ~136 lines, but simpler since consolidation has fewer subsections).

**Change B -- Replace Phase 3 placeholder (lines 251-254):**

Remove:
```markdown
## Phase 3 -- Verification

> **Placeholder**: This phase will be implemented in story-0005-0011.
> It will contain epic-level verification, cross-story integration
> testing, final DoD checklist, and epic completion reporting.
```

Replace with full Phase 3 content containing these subsections:
- **3.1 Epic-Level Test Suite** -- Run full test suite on the epic branch to validate cross-story integration. All tests must pass. Coverage thresholds enforced (>=95% line, >=90% branch).
- **3.2 DoD Checklist Validation** -- Verify: all stories completed or documented as incomplete, coverage thresholds met, no compiler/linter warnings, tech lead review executed, report generated, PR created.
- **3.3 Final Status Determination** -- Compute final epic status: `COMPLETE` (all stories SUCCESS), `PARTIAL` (some FAILED/BLOCKED), or `FAILED` (critical path stories failed). Persist to checkpoint.
- **3.4 Completion Output** -- Output final summary to the user: epic status, stories completed/failed/blocked, PR link, report path, total elapsed time.

**Expected line count:** ~50-70 non-empty lines.

**Change C -- Update Extension Points (section 1.7):**

Remove this line from the extension points list:
```markdown
- [Placeholder: consolidation + verification -- story-0005-0011]
```

This placeholder is now fulfilled by the new Phase 2 and Phase 3 content.

**Change D -- Update Integration Notes:**

Add to the Integration Notes section:
```markdown
- Invokes: `x-review-pr` (tech lead review on full epic diff)
- Uses: `gh pr create` (PR creation with summary body)
- Reads: `_TEMPLATE-EPIC-EXECUTION-REPORT.md` (report template), `execution-state.json` (checkpoint data)
```

### 2.2 `resources/github-skills-templates/dev/x-dev-epic-implement.md` (GitHub mirror)

Apply the same logical changes as 2.1 but in abbreviated form (matching the pattern where the GitHub mirror uses shorter bullet-point summaries instead of full procedural descriptions).

**Phase 2 abbreviated content (~20-30 lines):**
- 2.1 Tech Lead Review -- dispatch `x-review-pr` subagent on full diff
- 2.2 Report Generation -- resolve template placeholders, compute metrics
- 2.3 PR Creation -- `gh pr create` with title/body format
- 2.4 Partial Completion -- `[PARTIAL]` marker when completion < 100%

**Phase 3 abbreviated content (~15-20 lines):**
- 3.1 Epic-Level Tests -- full test suite, coverage thresholds
- 3.2 DoD Checklist -- verification items
- 3.3 Final Status -- COMPLETE/PARTIAL/FAILED determination
- 3.4 Completion Output -- final summary

**Extension Points and Integration Notes:** Same removals/additions as 2.1.

### 2.3 `tests/node/content/x-dev-epic-implement-content.test.ts`

**Change A -- Invert the placeholder test (line 114):**

Replace `skillMd_phases2And3_remainPlaceholders` with two new tests:
1. `skillMd_phase2_isNotPlaceholder_containsSubstantiveContent` -- Assert Phase 2 content has >=30 non-empty lines, does NOT match the placeholder pattern.
2. `skillMd_phase3_isNotPlaceholder_containsSubstantiveContent` -- Assert Phase 3 content has >=20 non-empty lines, does NOT match the placeholder pattern.

**Change B -- Add Phase 2 content assertion tests (new describe block):**

```
describe("x-dev-epic-implement SKILL.md -- Phase 2 content", () => { ... })
```

Tests organized by TPP level:

**TPP Level 2 -- Scalar keyword assertions:**
- `skillMd_phase2_containsTechLeadReview` -- Asserts `x-review-pr` and `ReviewResult` present
- `skillMd_phase2_containsReportGeneration` -- Asserts `epic-execution-report` and `_TEMPLATE-EPIC-EXECUTION-REPORT` present
- `skillMd_phase2_containsPrCreation` -- Asserts `gh pr create` and `git push` present
- `skillMd_phase2_containsPartialCompletion` -- Asserts `[PARTIAL]` and `completion` present

**TPP Level 3 -- Collection assertions:**
- `skillMd_phase2_containsReportFields` -- Asserts presence of: `storiesCompleted`, `storiesFailed`, `storiesBlocked`, `completionPercentage` (or equivalent kebab/snake forms from the template)
- `skillMd_phase2_containsPrTitleFormat` -- Asserts `feat(epic)` title format present
- `skillMd_phase2_containsSubagentPattern` -- Asserts `Agent` and `RULE-001` or `context isolation` present
- `skillMd_phase2_containsStatusValues` -- Asserts `SUCCESS`, `FAILED`, `BLOCKED` present

**TPP Level 4 -- Structural assertions:**
- `skillMd_phase2_containsMinimumSubsections` -- At least 4 subsections (### 2.N)
- `skillMd_phase2_subsectionsInLogicalOrder` -- Review < Report < PR Creation
- `skillMd_phase2_doesNotContainSourceImports` -- No `import` or `require()` statements

**Change C -- Add Phase 3 content assertion tests (new describe block):**

```
describe("x-dev-epic-implement SKILL.md -- Phase 3 content", () => { ... })
```

**TPP Level 2 -- Scalar:**
- `skillMd_phase3_containsTestSuiteValidation` -- Asserts `test suite` or `coverage` present
- `skillMd_phase3_containsDodChecklist` -- Asserts `DoD` or `Definition of Done` or `checklist` present
- `skillMd_phase3_containsFinalStatus` -- Asserts `COMPLETE` and `PARTIAL` and `FAILED` present

**TPP Level 3 -- Collection:**
- `skillMd_phase3_containsCoverageThresholds` -- Asserts `95%` and `90%` present
- `skillMd_phase3_containsCompletionOutput` -- Asserts presence of: PR link, report path, elapsed time (at least 2 of 3)

**TPP Level 4 -- Structural:**
- `skillMd_phase3_containsMinimumSubsections` -- At least 3 subsections (### 3.N)

**Change D -- Add dual-copy consistency terms:**

Add these terms to `CRITICAL_TERMS` in the dual-copy consistency test:
- `x-review-pr`
- `gh pr create`
- `epic-execution-report`
- `[PARTIAL]`
- `COMPLETE`
- `ReviewResult`

**Change E -- Update Extension Points test:**

The existing test `skillMd_phase1_containsExtensionPlaceholders` checks that at least 3 of the listed story references appear. After removing `story-0005-0011` from the extension points, update the expected refs list to remove `"story-0005-0011"`. The test should still pass since 5 other refs remain.

---

## 3. Test Scenarios to Add

### 3.1 Phase Structure Tests (Inversion)

| Test Name | TPP | Assertion |
|-----------|-----|-----------|
| `skillMd_phase2_isNotPlaceholder_containsSubstantiveContent` | L4 | Phase 2 has >=30 non-empty lines, no placeholder pattern |
| `skillMd_phase3_isNotPlaceholder_containsSubstantiveContent` | L4 | Phase 3 has >=20 non-empty lines, no placeholder pattern |

### 3.2 Phase 2 Content Tests

| Test Name | TPP | Assertion |
|-----------|-----|-----------|
| `skillMd_phase2_containsTechLeadReview` | L2 | `x-review-pr`, `ReviewResult` |
| `skillMd_phase2_containsReportGeneration` | L2 | `epic-execution-report`, template reference |
| `skillMd_phase2_containsPrCreation` | L2 | `gh pr create`, `git push` |
| `skillMd_phase2_containsPartialCompletion` | L2 | `[PARTIAL]`, completion logic |
| `skillMd_phase2_containsReportFields` | L3 | Report metric field names |
| `skillMd_phase2_containsPrTitleFormat` | L3 | `feat(epic)` format |
| `skillMd_phase2_containsSubagentPattern` | L3 | `Agent` tool, context isolation |
| `skillMd_phase2_containsStatusValues` | L3 | SUCCESS, FAILED, BLOCKED |
| `skillMd_phase2_containsMinimumSubsections` | L4 | >=4 subsections matching `### 2.\d` |
| `skillMd_phase2_subsectionsInLogicalOrder` | L4 | Review idx < Report idx < PR idx |
| `skillMd_phase2_doesNotContainSourceImports` | L4 | No `import`/`require` |

### 3.3 Phase 3 Content Tests

| Test Name | TPP | Assertion |
|-----------|-----|-----------|
| `skillMd_phase3_containsTestSuiteValidation` | L2 | Test suite / coverage keywords |
| `skillMd_phase3_containsDodChecklist` | L2 | DoD / checklist keywords |
| `skillMd_phase3_containsFinalStatus` | L2 | COMPLETE, PARTIAL, FAILED |
| `skillMd_phase3_containsCoverageThresholds` | L3 | 95%, 90% |
| `skillMd_phase3_containsCompletionOutput` | L3 | PR link, report path, elapsed time |
| `skillMd_phase3_containsMinimumSubsections` | L4 | >=3 subsections matching `### 3.\d` |

### 3.4 Dual-Copy Consistency Tests (additions)

| Term | Assertion |
|------|-----------|
| `x-review-pr` | Present in both Claude and GitHub templates |
| `gh pr create` | Present in both |
| `epic-execution-report` | Present in both |
| `[PARTIAL]` | Present in both |
| `COMPLETE` | Present in both |
| `ReviewResult` | Present in both |

### 3.5 Estimated New Test Count

- Phase structure inversion: 2 tests (replacing 1)
- Phase 2 content: 11 tests
- Phase 3 content: 6 tests
- Dual-copy new terms: 6 tests (via `it.each`)
- Extension point update: 0 (existing test remains, just update refs array)

**Total new tests: ~25** (net +24, since 1 existing test is replaced by 2)

---

## 4. Golden File Regeneration Strategy

Following the exact pattern from story-0005-0005 commit `cdbf47d`:

### 4.1 Approach

1. First update the source templates (#1 and #2 above)
2. Run the pipeline: `npm run build && npx vitest run tests/node/integration/byte-for-byte.test.ts`
3. If byte-for-byte tests fail (they will, since golden files are stale), regenerate by running the pipeline for each profile and copying outputs to golden directories
4. Alternatively, since `.claude/` and `.agents/` golden files are byte-for-byte copies of the Claude template, and `.github/` golden files are byte-for-byte copies of the GitHub template, use direct file copy:

```bash
# For each of the 8 profiles:
for profile in go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs; do
  cp resources/skills-templates/core/x-dev-epic-implement/SKILL.md \
     tests/golden/$profile/.claude/skills/x-dev-epic-implement/SKILL.md
  cp resources/skills-templates/core/x-dev-epic-implement/SKILL.md \
     tests/golden/$profile/.agents/skills/x-dev-epic-implement/SKILL.md
  cp resources/github-skills-templates/dev/x-dev-epic-implement.md \
     tests/golden/$profile/.github/skills/x-dev-epic-implement/SKILL.md
done
```

5. Validate with `npx vitest run tests/node/integration/byte-for-byte.test.ts`

### 4.2 File Count

- 8 profiles x 3 copies = **24 golden files** to regenerate
- This matches story-0005-0005 which also regenerated 24 files in commit `cdbf47d`

---

## 5. Dependency Direction Validation

This story modifies ONLY template content files (Markdown) and test files. No TypeScript source code in `src/` is created or modified.

**Verification checklist:**
- [x] No new files in `src/domain/`
- [x] No new files in `src/assembler/`
- [x] No new files in `src/cli*.ts`
- [x] No modifications to `src/config.ts`
- [x] No modifications to `src/models.ts`
- [x] No new npm dependencies
- [x] Template files are static Markdown -- no import graph to violate
- [x] Test file imports only from `vitest` and `node:fs`/`node:path` (unchanged from existing test)

**Conclusion:** No domain layer violations possible. This story is purely template content + test assertions.

---

## 6. Risk Assessment

| Risk | Severity | Probability | Mitigation |
|------|----------|-------------|------------|
| Phase 2/3 content too verbose, exceeding practical template length | Low | Medium | Target ~80-100 lines for Phase 2, ~50-70 for Phase 3 (Phase 1 is ~136 lines as reference ceiling). Keep procedural but not exhaustive. |
| Dual-copy consistency drift between Claude and GitHub templates | Medium | Medium | The existing `it.each` dual-copy test catches this mechanically. Add all new critical terms to the consistency array before committing templates. |
| Extension point removal breaks downstream stories | Low | Low | Only removing `story-0005-0011` from the list. Stories 0006-0010 and 0013 placeholders remain untouched. |
| Golden file regeneration misses a profile | Medium | Low | Use the loop-based copy approach (Section 4.1 step 4) to ensure all 8 profiles are covered. Verify with byte-for-byte integration test. |
| Content in Phase 2/3 references functions or types that do not yet exist in the codebase | Low | Low | The template content describes orchestrator behavior (runtime AI agent instructions), not compilable code. `{{PLACEHOLDER}}` tokens are explicitly documented as runtime markers. |
| Test count impacts Vitest run time | Low | Low | ~25 new tests are all string assertions on a single pre-loaded file (no I/O per test). Negligible impact. |

---

## 7. Implementation Order (TDD)

Following the exact commit sequence from story-0005-0005:

### Step 1: RED -- Invert placeholder test, add failing Phase 2/3 content assertions
- **File:** `tests/node/content/x-dev-epic-implement-content.test.ts`
- Replace `skillMd_phases2And3_remainPlaceholders` with the two "isNotPlaceholder" tests
- Add Phase 2 and Phase 3 describe blocks with content assertions
- **Expected:** Tests FAIL because Phase 2/3 are still placeholders
- **Commit:** `test(story-0005-0011): add Phase 2 and Phase 3 content assertions [TDD:RED]`

### Step 2: GREEN -- Write Phase 2 and Phase 3 content in Claude template
- **File:** `resources/skills-templates/core/x-dev-epic-implement/SKILL.md`
- Replace Phase 2 placeholder with full consolidation content (2.1-2.5)
- Replace Phase 3 placeholder with full verification content (3.1-3.4)
- Remove `story-0005-0011` from Extension Points (section 1.7)
- Update Integration Notes
- **Expected:** Content assertion tests on Claude template now PASS
- **Commit:** `feat(story-0005-0011): implement Phase 2 and Phase 3 in SKILL.md [TDD:GREEN]`

### Step 3: GREEN -- Update GitHub mirror and dual-copy tests
- **File:** `resources/github-skills-templates/dev/x-dev-epic-implement.md`
- Replace Phase 2 and Phase 3 placeholders with abbreviated content
- Remove `story-0005-0011` from Extension Points
- Update Integration Notes
- **File:** `tests/node/content/x-dev-epic-implement-content.test.ts`
- Add new terms to `CRITICAL_TERMS` dual-copy array
- Update extension points refs array
- **Expected:** All content tests and dual-copy tests PASS
- **Commit:** `feat(story-0005-0011): update GitHub mirror with consolidation and verification [TDD:GREEN]`

### Step 4: REFACTOR -- Regenerate golden files
- **Files:** 24 golden files across 8 profiles
- Copy Claude template to `.claude/` and `.agents/` golden dirs
- Copy GitHub template to `.github/` golden dirs
- **Expected:** Byte-for-byte integration tests PASS
- **Commit:** `refactor(story-0005-0011): regenerate golden files for all 8 profiles [TDD:REFACTOR]`

### Step 5: Verify
- Run full test suite: `npx vitest run`
- Verify coverage thresholds maintained
- Run `npx tsc --noEmit` for type checking

---

## 8. Content Design -- Phase 2 (Consolidation) Outline

The Phase 2 content describes three sequential actions the orchestrator performs after all stories complete:

```
### 2.1 Tech Lead Review Subagent
- Dispatch clean-context subagent (RULE-001) to execute x-review-pr on epic diff
- Input: branch name, base branch (main)
- Subagent reviews full diff (all commits from all stories)
- Returns ReviewResult: { score: "XX/40", decision: "GO"|"NO-GO", findings: [...] }
- On subagent failure: log warning, continue (review is informational)

### 2.2 Report Generation Subagent
- Dispatch clean-context subagent to generate epic-execution-report.md
- Subagent reads _TEMPLATE-EPIC-EXECUTION-REPORT.md
- Subagent reads execution-state.json for checkpoint data
- Resolves placeholders: {{EPIC_ID}}, {{STORIES_COMPLETED}}, {{COMPLETION_PERCENTAGE}}, etc.
- Computes metrics: completion percentage, coverage delta, phase timeline
- Writes epic-execution-report.md to the epic directory
- Validation: no unresolved {{...}} placeholders remain in output

### 2.3 PR Creation
- Push: git push -u origin feat/epic-{epicId}-full-implementation
- Title format: feat(epic): implement EPIC-{epicId} -- {title}
- If completion < 100%: title includes [PARTIAL] prefix
- Body structure:
  ## Summary
  - Stories completed: N/M
  - Tech Lead Review: XX/40 (GO/NO-GO)
  ## Metrics
  - Line coverage: XX%
  - Branch coverage: XX%
  - Completion: XX%
  ## Report
  - Link to epic-execution-report.md
- On push failure: log error, generate report without PR, persist failure in checkpoint

### 2.4 Partial Completion Handling
- Consolidation executes regardless of story failures
- Report shows FAILED stories with failure reasons
- Report shows BLOCKED stories with unsatisfied dependencies
- PR body explicitly indicates partial implementation

### 2.5 Checkpoint Finalization
- Register PR URL in checkpoint via updateCheckpoint(epicDir, { prUrl })
- Register report path in checkpoint
- Update finishedAt timestamp
- Persist final execution-state.json
```

---

## 9. Content Design -- Phase 3 (Verification) Outline

```
### 3.1 Epic-Level Test Suite
- Run full test suite on epic branch: all unit, integration, API tests
- Coverage thresholds: >=95% line, >=90% branch (non-negotiable)
- On test failure: log failures, mark epic as requiring attention
- Coverage results recorded in checkpoint

### 3.2 DoD Checklist Validation
- [ ] All stories completed (or documented as FAILED/BLOCKED in report)
- [ ] Coverage thresholds met
- [ ] Zero compiler/linter warnings
- [ ] Tech lead review executed (Phase 2.1)
- [ ] Report generated (Phase 2.2)
- [ ] PR created (Phase 2.3)
- [ ] No unresolved {{...}} placeholders in report

### 3.3 Final Status Determination
- COMPLETE: all stories SUCCESS, all DoD items checked
- PARTIAL: some stories FAILED/BLOCKED but critical path succeeded
- FAILED: critical path stories failed
- Status persisted to checkpoint: updateCheckpoint(epicDir, { finalStatus })

### 3.4 Completion Output
- Display final summary to user:
  Epic: EPIC-{epicId} -- {title}
  Status: COMPLETE | PARTIAL | FAILED
  Stories: X/Y completed, Z failed, W blocked
  Coverage: line XX%, branch XX%
  Tech Lead: XX/40 (GO/NO-GO)
  PR: {prUrl}
  Report: {reportPath}
  Elapsed: {totalTime}
```

---

## 10. Acceptance Criteria Traceability

| Gherkin Scenario (from story) | Test Coverage |
|------|-------------|
| Consolidation 100% SUCCESS | `skillMd_phase2_containsPrCreation` + `skillMd_phase2_containsReportGeneration` + `skillMd_phase3_containsFinalStatus` (COMPLETE keyword) |
| Consolidation partial (FAILED/BLOCKED) | `skillMd_phase2_containsPartialCompletion` (`[PARTIAL]` keyword) + `skillMd_phase2_containsStatusValues` (FAILED, BLOCKED) |
| Tech lead NO-GO | `skillMd_phase2_containsTechLeadReview` (ReviewResult, x-review-pr) |
| Placeholders resolved | `skillMd_phase2_containsReportGeneration` (template reference, placeholder resolution) |
| PR created with summary | `skillMd_phase2_containsPrTitleFormat` + `skillMd_phase2_containsPrCreation` |
| Push failure | `skillMd_phase2_containsPrCreation` (includes error handling description) |
