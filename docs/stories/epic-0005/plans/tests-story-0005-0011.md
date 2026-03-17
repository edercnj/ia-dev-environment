# Test Plan -- story-0005-0011: Consolidation Final — Review + Report + PR

**Story:** `story-0005-0011.md`
**Skill Under Test:** `resources/skills-templates/core/x-dev-epic-implement/SKILL.md`
**GitHub Mirror:** `resources/github-skills-templates/dev/x-dev-epic-implement.md`
**Existing Test File:** `tests/node/content/x-dev-epic-implement-content.test.ts`

**Framework:** Vitest (pool: forks, maxForks: 3, maxConcurrency: 5)
**Coverage Targets:** >= 95% line, >= 90% branch
**Test Naming:** `[subjectUnderTest]_[scenario]_[expectedBehavior]`

---

## Scope

Story-0005-0011 replaces the Phase 2 and Phase 3 **placeholders** in `SKILL.md` with substantive
consolidation content covering:

- **Phase 2 — Consolidation**: Tech lead review dispatch, report generation subagent, PR creation, partial completion handling
- **Phase 3 — Verification**: DoD checklist verification, final reporting

All tests are **content tests** against the generated SKILL.md templates and **integration tests**
via golden file byte-for-byte parity. No runtime domain logic is introduced by this story.

---

## Test File Structure

```
tests/
  node/
    content/
      x-dev-epic-implement-content.test.ts   # CT-01 through CT-24 (extend existing file)
  node/
    integration/
      byte-for-byte.test.ts                  # IT-01 (existing, validates golden parity)
```

---

## 1. Content Tests (CT-N) — Unit Level

All content tests extend the existing file `tests/node/content/x-dev-epic-implement-content.test.ts`.
They read the SKILL.md at test time and assert on string content. TPP order: degenerate -> happy path -> edge cases -> structural.

### 1.1 Degenerate — Phase 2 and Phase 3 Are No Longer Placeholders

These tests **invert** the existing `skillMd_phases2And3_remainPlaceholders` test. After implementation,
Phase 2 and Phase 3 must contain substantive content and must NOT match the placeholder patterns.

| ID | Test Name | Description | Assertion | TPP Level |
|:---|:---|:---|:---|:---|
| CT-01 | `skillMd_phase2_isNotPlaceholder_noLongerMatchesPlaceholderRegex` | Phase 2 content no longer matches the placeholder pattern | Phase 2 section does NOT match `/^[^]*>\s*\*?\*?Placeholder\*?\*?:/im` | 1 (degenerate) |
| CT-02 | `skillMd_phase3_isNotPlaceholder_noLongerMatchesPlaceholderRegex` | Phase 3 content no longer matches the placeholder pattern | Phase 3 section does NOT match `/^[^]*>\s*\*?\*?Placeholder\*?\*?:/im` | 1 (degenerate) |
| CT-03 | `skillMd_phase2_isNotPlaceholder_doesNotContainTodoMarker` | Phase 2 has no TODO or "implemented in" markers | Phase 2 section does NOT match `/TODO|implemented in|extended by/i` | 1 (degenerate) |
| CT-04 | `skillMd_phase3_isNotPlaceholder_doesNotContainTodoMarker` | Phase 3 has no TODO or "implemented in" markers | Phase 3 section does NOT match `/TODO|implemented in|extended by/i` | 1 (degenerate) |

### 1.2 Happy Path — Phase 2 Contains Required Subsection Content

| ID | Test Name | Description | Assertion | TPP Level |
|:---|:---|:---|:---|:---|
| CT-05 | `skillMd_phase2_containsTechLeadReviewDispatch` | Phase 2 includes tech lead review dispatch instructions | Phase 2 contains `x-review-pr` AND matches `/tech.?lead.?review/i` | 2 (scalar) |
| CT-06 | `skillMd_phase2_containsReportGenerationSubagent` | Phase 2 includes report generation subagent instructions | Phase 2 contains `epic-execution-report` AND matches `/report.?generat/i` | 2 (scalar) |
| CT-07 | `skillMd_phase2_containsPRCreationInstructions` | Phase 2 includes PR creation via gh CLI | Phase 2 contains `gh pr create` AND contains `git push` | 2 (scalar) |
| CT-08 | `skillMd_phase2_containsPartialCompletionHandling` | Phase 2 includes [PARTIAL] handling for incomplete epics | Phase 2 contains `[PARTIAL]` AND matches `/completion.*100|partial/i` | 2 (scalar) |
| CT-09 | `skillMd_phase2_containsExecutionReportTemplate` | Phase 2 references the report template | Phase 2 contains `_TEMPLATE-EPIC-EXECUTION-REPORT` OR matches `/template.*report/i` | 2 (scalar) |
| CT-10 | `skillMd_phase2_containsExecutionStateJsonReference` | Phase 2 references execution-state.json as data source | Phase 2 contains `execution-state.json` | 2 (scalar) |
| CT-11 | `skillMd_phase2_containsSubagentDelegation` | Phase 2 delegates to subagents per RULE-001 | Phase 2 contains `Agent` AND matches `/subagent|dispatch|delegate/i` | 2 (scalar) |
| CT-12 | `skillMd_phase2_containsPRTitleFormat` | Phase 2 specifies PR title format | Phase 2 matches `/feat\(epic\).*implement.*EPIC/i` | 2 (scalar) |
| CT-13 | `skillMd_phase2_containsCheckpointUpdate` | Phase 2 updates checkpoint with PR link | Phase 2 matches `/checkpoint|prLink|pr.?link/i` | 2 (scalar) |

### 1.3 Happy Path — Phase 3 Contains Required Subsection Content

| ID | Test Name | Description | Assertion | TPP Level |
|:---|:---|:---|:---|:---|
| CT-14 | `skillMd_phase3_containsDoDChecklist` | Phase 3 includes Definition of Done verification | Phase 3 matches `/definition.?of.?done|DoD|checklist/i` | 2 (scalar) |
| CT-15 | `skillMd_phase3_containsFinalReporting` | Phase 3 includes final reporting | Phase 3 matches `/final.?report|completion.?report|summary/i` | 2 (scalar) |
| CT-16 | `skillMd_phase3_containsCoverageVerification` | Phase 3 includes coverage threshold verification | Phase 3 matches `/coverage|95%|90%/i` | 2 (scalar) |
| CT-17 | `skillMd_phase3_containsTestVerification` | Phase 3 verifies all tests pass | Phase 3 matches `/test.*pass|all.*tests|vitest/i` | 2 (scalar) |

### 1.4 Edge Cases — Minimum Substantive Content (Line Count)

| ID | Test Name | Description | Assertion | TPP Level |
|:---|:---|:---|:---|:---|
| CT-18 | `skillMd_phase2_hasMinimumSubstantiveContent_atLeast40NonEmptyLines` | Phase 2 must have enough content to cover review + report + PR + partial | Phase 2 non-empty line count >= 40 | 3 (collection) |
| CT-19 | `skillMd_phase3_hasMinimumSubstantiveContent_atLeast20NonEmptyLines` | Phase 3 must have enough content for DoD checklist + final reporting | Phase 3 non-empty line count >= 20 | 3 (collection) |

### 1.5 Edge Cases — Phase 1 Extension Point Updated

| ID | Test Name | Description | Assertion | TPP Level |
|:---|:---|:---|:---|:---|
| CT-20 | `skillMd_phase1_extensionPoint0011_removedOrResolved` | The Phase 1 placeholder referencing story-0005-0011 is resolved | Phase 1 does NOT contain a placeholder line matching `/\[Placeholder.*story-0005-0011\]/i` | 4 (conditional) |

---

## 2. Structural Tests (ST-N)

### 2.1 Phase 2 Subsection Structure

| ID | Test Name | Description | Assertion | TPP Level |
|:---|:---|:---|:---|:---|
| ST-01 | `skillMd_phase2_hasExpectedSubsections_2dot1Through2dot4` | Phase 2 has subsections 2.1, 2.2, 2.3, 2.4 matching story description | Phase 2 matches at least 3 of: `/###\s+2\.1/`, `/###\s+2\.2/`, `/###\s+2\.3/`, `/###\s+2\.4/` | 5 (structural) |
| ST-02 | `skillMd_phase2_subsectionsInLogicalOrder_reviewBeforeReportBeforePR` | Subsections appear in logical order: review -> report -> PR | indexOf("Tech Lead Review" or "2.1") < indexOf("Report" or "2.2") < indexOf("PR" or "2.3") within Phase 2 | 5 (structural) |
| ST-03 | `skillMd_phase2_containsMinimumSubsections_atLeast3` | Phase 2 has at least 3 `###` subsection headers | Count of `/^###\s+2\.\d/gm` matches >= 3 | 5 (structural) |

### 2.2 Phase 3 Subsection Structure

| ID | Test Name | Description | Assertion | TPP Level |
|:---|:---|:---|:---|:---|
| ST-04 | `skillMd_phase3_hasExpectedSubsections` | Phase 3 has subsection headers | Count of `/^###\s+3\.\d/gm` matches >= 1 | 5 (structural) |

---

## 3. Dual Copy Consistency Tests (DC-N)

These tests extend the existing `x-dev-epic-implement dual copy consistency` describe block.
Both the core SKILL.md (`resources/skills-templates/core/x-dev-epic-implement/SKILL.md`) and the
GitHub mirror (`resources/github-skills-templates/dev/x-dev-epic-implement.md`) must contain
the new consolidation-specific critical terms.

### 3.1 New Critical Terms for Consolidation

| ID | Test Name | Critical Term | Rationale | TPP Level |
|:---|:---|:---|:---|:---|
| DC-01 | `bothContainTerm_x-review-pr_dualCopyConsistency` | `x-review-pr` | Tech lead review skill reference | 3 (collection) |
| DC-02 | `bothContainTerm_gh_pr_create_dualCopyConsistency` | `gh pr create` | PR creation command | 3 (collection) |
| DC-03 | `bothContainTerm_epic-execution-report_dualCopyConsistency` | `epic-execution-report` | Report file name | 3 (collection) |
| DC-04 | `bothContainTerm_PARTIAL_dualCopyConsistency` | `[PARTIAL]` | Partial completion flag | 3 (collection) |
| DC-05 | `bothContainTerm_git_push_dualCopyConsistency` | `git push` | Branch push command | 3 (collection) |
| DC-06 | `bothContainTerm_GO_NO-GO_dualCopyConsistency` | `GO` and `NO-GO` | Tech lead review decisions | 3 (collection) |
| DC-07 | `bothContainTerm_DoD_dualCopyConsistency` | Matches `/DoD|Definition of Done/i` | Verification phase reference | 3 (collection) |

---

## 4. Integration Tests (IT-N) — Golden File Byte-for-Byte Parity

The existing `byte-for-byte.test.ts` already validates all 8 profiles. After SKILL.md is updated:

1. Regenerate golden files for all profiles
2. Run `byte-for-byte.test.ts` to confirm parity

| ID | Test Name | Description | Expected Result | Depends On |
|:---|:---|:---|:---|:---|
| IT-01 | `pipelineMatchesGoldenFiles_{profile}` (all 8 profiles) | After SKILL.md update and golden regeneration, pipeline output matches golden files | `verification.success === true`, `missingFiles === []`, `extraFiles === []` | All CT and ST tests passing |

**Execution:** The byte-for-byte tests run via `describe.sequential.each(CONFIG_PROFILES)` in
`tests/node/integration/byte-for-byte.test.ts`. No new test file is needed; golden files must
be regenerated after the SKILL.md changes.

---

## 5. TPP Order Summary

```
Level 1 (Degenerate):   CT-01, CT-02, CT-03, CT-04  — Placeholders removed
Level 2 (Scalar):       CT-05..CT-17                 — Individual keyword/content assertions
Level 3 (Collection):   CT-18, CT-19, DC-01..DC-07   — Line counts, dual copy terms
Level 4 (Conditional):  CT-20                         — Extension point resolved
Level 5 (Structural):   ST-01..ST-04                  — Subsection structure and ordering
Integration:            IT-01                          — Golden file parity (8 profiles)
```

---

## 6. Implementation Notes

### 6.1 Modifying the Existing Test File

The existing test `skillMd_phases2And3_remainPlaceholders` (line 114 of the current test file)
must be **replaced** by CT-01 through CT-04. The old test asserts placeholders exist; the new
tests assert placeholders are gone. This is the key inversion for this story.

### 6.2 Phase Extraction Helper

Reuse the pattern already established in the test file for extracting phase content:

```typescript
function extractPhase2(): string {
  const phase2Idx = content.indexOf("Phase 2");
  const phase3Idx = content.indexOf("Phase 3", phase2Idx);
  return content.slice(phase2Idx, phase3Idx);
}

function extractPhase3(): string {
  const phase3Idx = content.indexOf("Phase 3");
  const integrationIdx = content.indexOf("## Integration Notes", phase3Idx);
  return integrationIdx > -1
    ? content.slice(phase3Idx, integrationIdx)
    : content.slice(phase3Idx);
}
```

### 6.3 Dual Copy Consistency — Extending CRITICAL_TERMS

Add the new DC-01 through DC-07 terms to the existing `CRITICAL_TERMS` array in the
`x-dev-epic-implement dual copy consistency` describe block. Use `it.each` for consistency
with the existing pattern.

### 6.4 Golden File Regeneration

After updating SKILL.md, run:

```bash
npx vitest run tests/node/integration/byte-for-byte.test.ts --update
```

Or regenerate golden files via the pipeline, then verify parity.

---

## 7. Traceability Matrix

| Gherkin Scenario (from story) | Test IDs |
|:---|:---|
| Consolidation 100% — all stories SUCCESS | CT-05, CT-07, CT-12, ST-02 |
| Consolidation partial — stories FAILED/BLOCKED | CT-08, CT-10 |
| Tech lead review returns NO-GO | CT-05, DC-06 |
| Report generated with all placeholders resolved | CT-06, CT-09, CT-10, DC-03 |
| PR created with summary | CT-07, CT-12, CT-13, DC-02, DC-05 |
| Push failure — consolidation reports error | CT-07 (presence of push instructions) |
| SKILL.md updated with consolidation section | CT-01..CT-04 (placeholders removed), ST-01..ST-04 |
| DoD: coverage >= 95% line, >= 90% branch | CT-16 (coverage in Phase 3) |
| DoD: tests pass | CT-17 (test verification in Phase 3) |

---

## 8. Coverage Strategy

| File Under Test | Tests | Expected Coverage |
|:---|:---|:---|
| `x-dev-epic-implement-content.test.ts` (content tests) | CT-01..CT-20, ST-01..ST-04, DC-01..DC-07 | N/A (test file itself) |
| `resources/skills-templates/core/x-dev-epic-implement/SKILL.md` | All CT, ST, DC | Content coverage: all Phase 2 and Phase 3 subsections verified |
| `resources/github-skills-templates/dev/x-dev-epic-implement.md` | DC-01..DC-07 | Dual copy terms verified |
| Golden files (8 profiles) | IT-01 | Byte-for-byte parity after regeneration |

**Total new test scenarios:** 24 content + 4 structural + 7 dual copy + 1 integration validation = **36 test scenarios**

---

## 9. Parallelism and Execution

All content tests (CT, ST, DC) are parallelizable:
- They read SKILL.md **once** at module load (already done in existing file via `const content = fs.readFileSync(...)`)
- No filesystem writes, no shared mutable state
- Each test performs only string matching on the cached content

Integration tests (IT-01) run sequentially per profile as established in `byte-for-byte.test.ts`.

**Estimated execution time:** < 2 seconds for content tests; < 60 seconds for integration tests.

---

## 10. Risks and Mitigations

| Risk | Mitigation |
|:---|:---|
| Phase 2/3 content too short to pass CT-18/CT-19 line count thresholds | Thresholds set conservatively (40/20 lines); review content during implementation |
| GitHub mirror diverges from core SKILL.md | DC-01..DC-07 enforce term parity; any drift fails CI |
| Golden files not regenerated after SKILL.md change | IT-01 fails immediately if golden files are stale |
| Existing `skillMd_phases2And3_remainPlaceholders` test not removed | CT-01..CT-04 directly contradict it; both cannot pass simultaneously, forcing removal |
