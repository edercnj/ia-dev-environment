# Test Plan — story-0005-0005: Orchestrator Core Loop + Sequential Dispatcher

## Summary

- **Total test classes:** 1 (modified: `x-dev-epic-implement-content.test.ts`)
- **Total test methods:** ~25 (estimated: 8 new + 17 existing modified/preserved)
- **Categories covered:** Content tests, Dual-copy consistency, Golden file (byte-for-byte)
- **Estimated line coverage:** N/A (template-only change; content tests validate Markdown)
- **TDD Mode:** Double-Loop — Acceptance tests (AT) drive unit tests (UT) in TPP order

## Test Strategy

This is a **template-only** change (SKILL.md Markdown). No new TypeScript modules. Tests are:
1. **Content assertions** — validate Phase 1 sections, keywords, and structure
2. **Dual-copy consistency** — verify Claude + GitHub templates contain matching terms
3. **Golden file regeneration** — byte-for-byte match across 8 profiles

## Acceptance Tests (Outer Loop)

### AT-1: Phase 1 is no longer a placeholder

**Type:** Content test (Acceptance)
**Depends On:** None (first test)
**Parallel:** No

```
GIVEN the x-dev-epic-implement SKILL.md template
WHEN Phase 1 content is extracted (between "Phase 1" and "Phase 2" headings)
THEN it does NOT match the placeholder pattern /placeholder|implemented in/i
AND it contains at least 50 lines of content (not a stub)
```

**Test Name:** `skillMd_phase1_isNotPlaceholder_containsSubstantiveContent`

### AT-2: Phase 1 contains complete core loop documentation

**Type:** Content test (Acceptance)
**Depends On:** AT-1
**Parallel:** No

```
GIVEN the x-dev-epic-implement SKILL.md template
WHEN Phase 1 content is extracted
THEN it contains ALL of: checkpoint integration, map parser integration, subagent dispatch,
     result validation, branch management, critical path priority, and extension points
```

**Test Name:** `skillMd_phase1_containsAllRequiredSubsections`

### AT-3: Dual-copy consistency includes new Phase 1 terms

**Type:** Dual-copy consistency test (Acceptance)
**Depends On:** AT-1
**Parallel:** Yes (independent of AT-2)

```
GIVEN both Claude SKILL.md and GitHub mirror
WHEN checked for critical Phase 1 terms
THEN both contain: getExecutableStories, SubagentResult, IN_PROGRESS, createCheckpoint, RULE-008
```

**Test Name:** `bothContainTerm_%s_dualCopyConsistency` (extended CRITICAL_TERMS array)

### AT-4: Golden files match updated templates

**Type:** Integration (byte-for-byte)
**Depends On:** AT-1, AT-2
**Parallel:** Yes (runs independently in byte-for-byte.test.ts)

```
GIVEN the pipeline generates output for all 8 profiles
WHEN comparing generated .claude/skills/x-dev-epic-implement/SKILL.md against golden files
THEN all 8 profiles produce byte-for-byte matches
```

**Test Name:** Existing `byte-for-byte.test.ts` — requires golden file regeneration only

---

## Unit Tests (Inner Loop — TPP Order)

### Level 1: Degenerate / Constant (simplest assertions)

#### UT-1: Phase 1 heading exists

**Depends On:** None
**Parallel:** No (foundation)

```
GIVEN the SKILL.md content
WHEN searched for "Phase 1"
THEN it is found (content.indexOf("Phase 1") > -1)
```

**Test Name:** `skillMd_phase1_headingExists`

#### UT-2: Phases 2 and 3 remain placeholders

**Depends On:** None
**Parallel:** Yes (with UT-1)

```
GIVEN the SKILL.md content
WHEN Phase 2 and Phase 3 sections are extracted
THEN both still match /placeholder|story-0005|implemented in/i
```

**Test Name:** `skillMd_phases2And3_remainPlaceholders`

*Refactored from existing `skillMd_phases1to3_arePlaceholders`.*

### Level 2: Scalar (single keyword assertions)

#### UT-3: Phase 1 mentions checkpoint integration

**Depends On:** UT-1
**Parallel:** No

```
GIVEN Phase 1 content (between "Phase 1" and "Phase 2")
WHEN checked for checkpoint keywords
THEN it contains "createCheckpoint" AND "updateStoryStatus"
```

**Test Name:** `skillMd_phase1_containsCheckpointIntegration`

#### UT-4: Phase 1 mentions map parser integration

**Depends On:** UT-1
**Parallel:** Yes (with UT-3)

```
GIVEN Phase 1 content
WHEN checked for parser keywords
THEN it contains "parseImplementationMap" AND "getExecutableStories"
```

**Test Name:** `skillMd_phase1_containsMapParserIntegration`

#### UT-5: Phase 1 mentions subagent dispatch

**Depends On:** UT-1
**Parallel:** Yes (with UT-3, UT-4)

```
GIVEN Phase 1 content
WHEN checked for dispatch keywords
THEN it contains "Agent" AND "SubagentResult" AND "x-dev-lifecycle"
```

**Test Name:** `skillMd_phase1_containsSubagentDispatch`

### Level 3: Collection (multiple keywords per assertion)

#### UT-6: Phase 1 mentions result validation fields (RULE-008)

**Depends On:** UT-5
**Parallel:** No

```
GIVEN Phase 1 content
WHEN checked for SubagentResult contract fields
THEN it contains ALL of: "status", "findingsCount", "summary", "commitSha"
AND it references "RULE-008" or "result contract"
```

**Test Name:** `skillMd_phase1_containsResultValidation`

#### UT-7: Phase 1 mentions branch management

**Depends On:** UT-1
**Parallel:** Yes (with UT-6)

```
GIVEN Phase 1 content
WHEN checked for branch management keywords
THEN it contains "feat/epic-" AND ("git checkout" OR "branch")
```

**Test Name:** `skillMd_phase1_containsBranchManagement`

#### UT-8: Phase 1 mentions critical path priority (RULE-007)

**Depends On:** UT-1
**Parallel:** Yes

```
GIVEN Phase 1 content
WHEN checked for prioritization keywords
THEN it matches /critical.?path/i
```

**Test Name:** `skillMd_phase1_containsCriticalPathPriority`

### Level 4: Composite (structural assertions)

#### UT-9: Phase 1 mentions context isolation (RULE-001)

**Depends On:** UT-1
**Parallel:** Yes

```
GIVEN Phase 1 content
WHEN checked for context isolation keywords
THEN it matches /RULE-001|context isolation|clean context/i
```

**Test Name:** `skillMd_phase1_containsContextIsolation`

#### UT-10: Phase 1 contains extension point placeholders

**Depends On:** UT-1
**Parallel:** Yes

```
GIVEN Phase 1 content
WHEN checked for extension point references
THEN it contains at least 3 of: "story-0005-0006", "story-0005-0007", "story-0005-0008",
     "story-0005-0010", "story-0005-0011", "story-0005-0013"
```

**Test Name:** `skillMd_phase1_containsExtensionPlaceholders`

#### UT-11: Phase 1 mentions status values

**Depends On:** UT-1
**Parallel:** Yes

```
GIVEN Phase 1 content
WHEN checked for StoryStatus values
THEN it contains ALL of: "IN_PROGRESS", "SUCCESS", "FAILED"
```

**Test Name:** `skillMd_phase1_containsStatusValues`

### Level 5: Dual-Copy Consistency (extended terms)

#### UT-12: New critical terms in both copies (parametrized)

**Depends On:** UT-3 through UT-11 (all keywords must be in Claude copy first)
**Parallel:** Yes

```
GIVEN both Claude SKILL.md and GitHub mirror
WHEN checked for each new Phase 1 critical term
THEN both contain the term
```

New terms to add to CRITICAL_TERMS:
- `getExecutableStories`
- `SubagentResult`
- `IN_PROGRESS`
- `createCheckpoint`

**Test Name:** `bothContainTerm_%s_dualCopyConsistency` (extended array)

### Level 6: Edge Cases

#### UT-13: Phase 1 does not contain source code (RULE-001 compliance)

**Depends On:** UT-9
**Parallel:** Yes

```
GIVEN Phase 1 content
WHEN checked for code import patterns
THEN it does NOT match /^import .* from/m
AND it does NOT match /require\(/
```

**Test Name:** `skillMd_phase1_doesNotContainSourceImports`

#### UT-14: Phase 1 section ordering is logical

**Depends On:** UT-1
**Parallel:** Yes

```
GIVEN Phase 1 content
WHEN the positions of "Initialize", "Branch", "Core Loop", "Dispatch", "Validation" are measured
THEN "Initialize" appears before "Core Loop"
AND "Core Loop" appears before "Dispatch" or "Dispatch" is within "Core Loop"
```

**Test Name:** `skillMd_phase1_subsectionsInLogicalOrder`

---

## Coverage Estimation

| Test File | Test Methods | Category |
|-----------|-------------|----------|
| `x-dev-epic-implement-content.test.ts` | ~25 (8 new + existing) | Content + Dual-copy |
| `byte-for-byte.test.ts` | Existing (8 profiles) | Golden file |

### Coverage Notes

- **Line/branch coverage** is not directly measurable since SKILL.md is Markdown, not TypeScript
- Content test effectiveness is measured by: every acceptance criterion from the story maps to ≥1 test
- Golden file tests ensure template changes propagate correctly across all 8 profiles

## Acceptance Criteria ↔ Test Mapping

| Gherkin Scenario | Test(s) |
|-----------------|---------|
| Single story execution | UT-5 (subagent dispatch), UT-3 (checkpoint) |
| Sequential two-phase execution | UT-4 (getExecutableStories), UT-11 (status values) |
| Critical path prioritization | UT-8 (critical path) |
| Valid SubagentResult validation | UT-6 (result fields) |
| Invalid SubagentResult → FAILED | UT-6 (validation), UT-11 (FAILED status) |
| Branch creation | UT-7 (branch management) |
| Checkpoint after each story (RULE-002) | UT-3 (checkpoint integration) |
| Context isolation (RULE-001) | UT-9, UT-13 (context isolation, no imports) |
| BLOCKED story not dispatched | UT-4 (getExecutableStories filters BLOCKED) |

## Risks and Gaps

1. **Existing test refactoring risk:** `skillMd_phases1to3_arePlaceholders` MUST be split before any template changes (Phase 1 placeholder goes away). This is the first RED step.
2. **Golden file regeneration:** Must run the full pipeline to regenerate all 16 golden files (8 profiles × 2 paths). If any profile config changes, tests fail.
3. **GitHub mirror brevity:** The GitHub mirror is abbreviated — dual-copy tests must only assert critical terms, not full section structure.
4. **No TypeScript unit tests:** Since there are no TypeScript source changes, coverage thresholds are met by existing tests (no regression). The risk is that content tests are weaker than unit tests at catching logical errors in the template.

## TDD Execution Order

```
1. RED:   UT-2  (split placeholder test — Phase 2+3 only; Phase 1 assertion removed)
2. RED:   AT-1  (Phase 1 is not placeholder — will FAIL because Phase 1 IS still placeholder)
3. GREEN: Write Phase 1 content in SKILL.md → AT-1 passes
4. RED:   UT-3  (checkpoint keywords — may FAIL if content incomplete)
5. RED:   UT-4  (parser keywords)
6. RED:   UT-5  (subagent dispatch keywords)
7. GREEN: Expand Phase 1 content → UT-3, UT-4, UT-5 pass
8. RED:   UT-6  (result validation fields)
9. RED:   UT-7  (branch management)
10. RED:  UT-8  (critical path)
11. GREEN: Expand Phase 1 → UT-6, UT-7, UT-8 pass
12. RED:  UT-9  (context isolation)
13. RED:  UT-10 (extension placeholders)
14. RED:  UT-11 (status values)
15. GREEN: Finalize Phase 1 → UT-9, UT-10, UT-11 pass
16. RED:  AT-2  (all required subsections) → should now pass
17. RED:  UT-12 (dual-copy new terms) → FAIL (GitHub mirror not updated)
18. GREEN: Update GitHub mirror → UT-12 passes, AT-3 passes
19. RED:  UT-13 (no source imports)
20. RED:  UT-14 (section ordering)
21. GREEN: Verify → should already pass
22. REFACTOR: Golden file regeneration → AT-4 passes
23. REFACTOR: Clean up test descriptions, remove redundancy
```
