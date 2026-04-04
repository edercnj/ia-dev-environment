# Task Breakdown -- story-0004-0016: Security Threat Model Documentation

## Summary

This story creates a STRIDE-based threat model template (`resources/templates/_TEMPLATE-THREAT-MODEL.md`) and modifies 4 skill instruction files to reference threat model extraction and update logic. No TypeScript source code changes are required -- all changes are Markdown template/skill content.

**Decomposition Mode:** TDD-driven (RED/GREEN/REFACTOR per task). Since this is a template-and-skill-content story, TDD phases apply to content tests validating template structure and skill instruction presence.

**Total Tasks:** 9
**Total Files Modified:** 5 source files + up to 48 golden files = 53 files
**Estimated Effort:** Medium

---

## TASK-1: Create threat model template (RED)

- **Tier:** Mid
- **Budget:** M
- **Group:** G1
- **Parallel:** yes (with TASK-2, TASK-3)
- **Depends On:** none
- **TDD Phase:** RED

**Description:**
Write a content test that validates the threat model template exists at `resources/templates/_TEMPLATE-THREAT-MODEL.md` and contains all required STRIDE sections, trust boundary diagram, risk table columns, severity/status enums, risk summary, and change history.

**File Created:**
- `tests/node/content/template-threat-model-sections.test.ts`

**Test Scenarios (following existing pattern from `template-tdd-sections.test.ts`):**

1. `threatModelTemplate_exists_canBeReadFromDisk` -- file exists and is non-empty
2. `threatModelTemplate_containsServiceNamePlaceholder` -- contains `{{SERVICE_NAME}}`
3. `threatModelTemplate_containsTrustBoundaries_hasMermaidDiagram` -- contains `## Trust Boundaries` and ` ```mermaid`
4. `threatModelTemplate_containsStrideAnalysis_allSixCategories` -- parametrized test for: Spoofing, Tampering, Repudiation, Information Disclosure, Denial of Service, Elevation of Privilege
5. `threatModelTemplate_riskTableColumns_containsAllRequired` -- contains Threat, Severity, Mitigation, Status, Story Ref
6. `threatModelTemplate_severityEnum_allLevels` -- contains Critical, High, Medium, Low
7. `threatModelTemplate_statusEnum_allValues` -- contains Open, Mitigated, Accepted, Under Review
8. `threatModelTemplate_containsRiskSummary` -- contains `## Risk Summary`
9. `threatModelTemplate_containsChangeHistory` -- contains `## Change History`
10. `threatModelTemplate_mermaidDiagram_hasTrustZones` -- External, DMZ, Internal subgraphs present
11. `threatModelTemplate_allHeadingsUseValidMarkdownSyntax` -- structural validation

**Acceptance Criteria:**
- All tests fail (RED) because the template file does not yet exist
- Test file compiles cleanly: `npx tsc --noEmit`

---

## TASK-2: Write failing test for x-review threat model instructions (RED)

- **Tier:** Mid
- **Budget:** M
- **Group:** G1
- **Parallel:** yes (with TASK-1, TASK-3)
- **Depends On:** none
- **TDD Phase:** RED

**Description:**
Write content tests that validate `resources/skills-templates/core/x-review/SKILL.md` contains threat model update instructions, severity-based auto-add rules, and references to the threat model template.

**File Created:**
- `tests/node/content/x-review-threat-model-content.test.ts`

**Test Scenarios:**

1. `xReviewSkill_containsThreatModelSection_afterConsolidation` -- skill contains a section about updating the threat model
2. `xReviewSkill_containsSeverityRules_criticalHighOpen` -- mentions Critical/High findings get status `Open`
3. `xReviewSkill_containsSeverityRules_mediumUnderReview` -- mentions Medium findings get status `Under Review`
4. `xReviewSkill_referencesStrideCategories_inExtraction` -- references STRIDE categorization
5. `xReviewSkill_referencesIncrementalUpdate_neverOverwrite` -- contains incremental/append-only language
6. `xReviewSkill_referencesTemplatePath_templateThreatModel` -- references `_TEMPLATE-THREAT-MODEL.md`

**Acceptance Criteria:**
- All tests fail (RED) because the instructions are not yet present in the skill file
- Test file compiles cleanly: `npx tsc --noEmit`

---

## TASK-3: Write failing test for x-dev-lifecycle threat model reference (RED)

- **Tier:** Mid
- **Budget:** S
- **Group:** G1
- **Parallel:** yes (with TASK-1, TASK-2)
- **Depends On:** none
- **TDD Phase:** RED

**Description:**
Write content tests that validate `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` references the threat model in its documentation/verification phase.

**File Created:**
- `tests/node/content/x-dev-lifecycle-threat-model-content.test.ts`

**Test Scenarios:**

1. `xDevLifecycleSkill_referencesThreatModel_inDocumentationPhase` -- skill mentions threat model update in Phase 7 or documentation phase
2. `xDevLifecycleSkill_referencesThreatModelTemplate_path` -- references `_TEMPLATE-THREAT-MODEL.md` or `threat-model`
3. `xDevLifecycleSkill_threatModelUpdate_isConditional` -- threat model update is conditional (only when security findings exist)

**Acceptance Criteria:**
- All tests fail (RED) because the references are not yet present in the lifecycle skill
- Test file compiles cleanly: `npx tsc --noEmit`

---

## TASK-4: Create `_TEMPLATE-THREAT-MODEL.md` (GREEN for TASK-1)

- **Tier:** Senior
- **Budget:** L
- **Group:** G2
- **Parallel:** no
- **Depends On:** TASK-1
- **TDD Phase:** GREEN

**Description:**
Create the STRIDE-based threat model template at `resources/templates/_TEMPLATE-THREAT-MODEL.md` with all sections defined in the story data contract (Section 5).

**File Created:**
- `resources/templates/_TEMPLATE-THREAT-MODEL.md`

**Template Structure (per story data contract):**

```
# Threat Model -- {{SERVICE_NAME}}

## Trust Boundaries
  (Mermaid diagram with External/DMZ/Internal subgraphs)

## STRIDE Analysis

### Spoofing
  | Threat | Severity | Mitigation | Status | Story Ref |

### Tampering
  | Threat | Severity | Mitigation | Status | Story Ref |

### Repudiation
  | Threat | Severity | Mitigation | Status | Story Ref |

### Information Disclosure
  | Threat | Severity | Mitigation | Status | Story Ref |

### Denial of Service
  | Threat | Severity | Mitigation | Status | Story Ref |

### Elevation of Privilege
  | Threat | Severity | Mitigation | Status | Story Ref |

## Risk Summary
  | Severity | Count |
  Critical, High, Medium, Low, Total

## Change History
  | Date | Story | Threats Added/Updated |
```

**Constraints:**
- Severity enum: `Critical`, `High`, `Medium`, `Low`
- Status enum: `Open`, `Mitigated`, `Accepted`, `Under Review`
- Mermaid diagram must include External, DMZ, Internal zones (from story Section 6.1)
- Placeholder `{{SERVICE_NAME}}` for project name substitution
- No `{{placeholders}}` beyond `{{SERVICE_NAME}}` (RULE-005)

**Acceptance Criteria:**
- All TASK-1 tests pass (GREEN)
- Template is valid Markdown with valid Mermaid syntax
- Template follows existing `_TEMPLATE-*.md` naming convention

---

## TASK-5: Modify x-review skill -- add threat model extraction (GREEN for TASK-2)

- **Tier:** Senior
- **Budget:** L
- **Group:** G2
- **Parallel:** yes (with TASK-6)
- **Depends On:** TASK-2, TASK-4
- **TDD Phase:** GREEN

**Description:**
Modify `resources/skills-templates/core/x-review/SKILL.md` to add a post-consolidation step that extracts security findings and updates the threat model. Insert a new Phase 3.5 (or append to Phase 3c) with threat model update instructions.

**File Modified:**
- `resources/skills-templates/core/x-review/SKILL.md`

**Changes:**
1. Add a new section after Phase 3 consolidation (3c Save Artifacts): **Phase 3d: Threat Model Update**
2. Define extraction logic: parse Security Engineer findings from the consolidated report
3. Define severity-based auto-add rules (decision table from plan Section 15):
   - Critical/High -> Status: `Open`
   - Medium -> Status: `Under Review`
   - Low -> Not auto-added
4. Reference `_TEMPLATE-THREAT-MODEL.md` for format/structure
5. Define STRIDE category mapping instructions
6. Define incremental update behavior: append-only, never remove existing entries
7. Define risk summary recomputation after each update
8. Add change history entry with date, story reference, and summary

**Acceptance Criteria:**
- All TASK-2 tests pass (GREEN)
- Existing functionality unchanged (no removals from Phases 1-4)
- Skill compiles: `npx tsc --noEmit` (no TS changes, but verify no accidental breakage)

---

## TASK-6: Modify x-dev-lifecycle skill -- add threat model reference (GREEN for TASK-3)

- **Tier:** Mid
- **Budget:** M
- **Group:** G2
- **Parallel:** yes (with TASK-5)
- **Depends On:** TASK-3, TASK-4
- **TDD Phase:** GREEN

**Description:**
Modify `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` to reference the threat model update in the verification/documentation phase.

**File Modified:**
- `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`

**Changes:**
1. Add threat model update reference in Phase 7 (Final Verification + Cleanup), as a conditional step:
   - If security findings were identified during Phase 3 review, update `docs/security/threat-model.md`
   - Use `_TEMPLATE-THREAT-MODEL.md` as format reference
   - Extract security findings from review reports and map to STRIDE categories
2. Add conditional DoD item in Phase 7:
   - `[ ] Threat model updated (if security findings with severity >= Medium)`
3. Update Integration Notes to mention threat model template reference

**Acceptance Criteria:**
- All TASK-3 tests pass (GREEN)
- Existing phases and content unchanged (additive only)
- Backward compatible: projects without threat model are unaffected (conditional logic)

---

## TASK-7: Update GitHub dual copies (RULE-001 compliance)

- **Tier:** Mid
- **Budget:** M
- **Group:** G3
- **Parallel:** no
- **Depends On:** TASK-5, TASK-6
- **TDD Phase:** GREEN (dual copy parity)

**Description:**
Apply semantically equivalent changes to the GitHub Copilot skill copies, with GitHub-specific path adjustments.

**Files Modified:**
- `resources/github-skills-templates/review/x-review.md`
- `resources/github-skills-templates/dev/x-dev-lifecycle.md`

**Changes for x-review (GitHub copy):**
1. Mirror the Phase 3d threat model update section from TASK-5
2. Adjust KP path references to use `.github/skills/...` pattern
3. Maintain abbreviated format consistent with current GitHub template style

**Changes for x-dev-lifecycle (GitHub copy):**
1. Mirror the Phase 7 threat model conditional step from TASK-6
2. Mirror the conditional DoD item
3. Adjust KP path references to use `.github/skills/...` pattern
4. Update Detailed References section if needed

**Platform-specific path mapping:**

| Claude Code path | GitHub Copilot path |
|-----------------|---------------------|
| `skills/security/SKILL.md` | `.github/skills/security/SKILL.md` |
| `resources/templates/_TEMPLATE-THREAT-MODEL.md` | `resources/templates/_TEMPLATE-THREAT-MODEL.md` (shared) |

**Acceptance Criteria:**
- Threat model extraction logic is semantically identical in both copies
- Only path references differ between Claude and GitHub versions
- Semantic diff between Claude and GitHub copies shows only expected path differences

---

## TASK-8: Update golden files (all profiles x all output dirs)

- **Tier:** Junior
- **Budget:** S
- **Group:** G4
- **Parallel:** no
- **Depends On:** TASK-5, TASK-6, TASK-7
- **TDD Phase:** GREEN (byte-for-byte parity)

**Description:**
Copy updated skill source templates to all 8 profiles across 3 output directories (.claude, .agents, .github). The `.claude` and `.agents` copies are byte-for-byte identical (sourced from `resources/skills-templates/core/`). The `.github` copies come from `resources/github-skills-templates/`.

**Golden File Update Script:**

```bash
PROFILES=(go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs)

# x-review
CLAUDE_SRC="resources/skills-templates/core/x-review/SKILL.md"
GITHUB_SRC="resources/github-skills-templates/review/x-review.md"
for profile in "${PROFILES[@]}"; do
  cp "$CLAUDE_SRC" "tests/golden/$profile/.claude/skills/x-review/SKILL.md"
  cp "$CLAUDE_SRC" "tests/golden/$profile/.agents/skills/x-review/SKILL.md"
  cp "$GITHUB_SRC" "tests/golden/$profile/.github/skills/x-review/SKILL.md"
done

# x-dev-lifecycle
CLAUDE_LIFECYCLE="resources/skills-templates/core/x-dev-lifecycle/SKILL.md"
GITHUB_LIFECYCLE="resources/github-skills-templates/dev/x-dev-lifecycle.md"
for profile in "${PROFILES[@]}"; do
  cp "$CLAUDE_LIFECYCLE" "tests/golden/$profile/.claude/skills/x-dev-lifecycle/SKILL.md"
  cp "$CLAUDE_LIFECYCLE" "tests/golden/$profile/.agents/skills/x-dev-lifecycle/SKILL.md"
  cp "$GITHUB_LIFECYCLE" "tests/golden/$profile/.github/skills/x-dev-lifecycle/SKILL.md"
done
```

**Files Updated (48 total):**

| Skill | Copy | Count | Path Pattern |
|-------|------|-------|-------------|
| x-review | .claude | 8 | `tests/golden/{profile}/.claude/skills/x-review/SKILL.md` |
| x-review | .agents | 8 | `tests/golden/{profile}/.agents/skills/x-review/SKILL.md` |
| x-review | .github | 8 | `tests/golden/{profile}/.github/skills/x-review/SKILL.md` |
| x-dev-lifecycle | .claude | 8 | `tests/golden/{profile}/.claude/skills/x-dev-lifecycle/SKILL.md` |
| x-dev-lifecycle | .agents | 8 | `tests/golden/{profile}/.agents/skills/x-dev-lifecycle/SKILL.md` |
| x-dev-lifecycle | .github | 8 | `tests/golden/{profile}/.github/skills/x-dev-lifecycle/SKILL.md` |

**Acceptance Criteria:**
- All 48 golden files updated
- `.claude` and `.agents` copies are byte-for-byte identical for each skill
- `.github` copies match the GitHub source template

---

## TASK-9: Run full test suite and verify coverage (VERIFY)

- **Tier:** Junior
- **Budget:** S
- **Group:** G5
- **Parallel:** no
- **Depends On:** TASK-8
- **TDD Phase:** VERIFY

**Description:**
Run the complete test suite to validate all changes are correct and no regressions exist.

**Commands:**

```bash
npx tsc --noEmit                                           # Compilation check
npx vitest run                                             # Full test suite
npx vitest run tests/node/integration/byte-for-byte.test.ts  # Byte-for-byte parity
```

**Verification Checklist:**
- [ ] TypeScript compilation clean (no errors from new test files)
- [ ] All content tests pass (template-threat-model-sections, x-review-threat-model-content, x-dev-lifecycle-threat-model-content)
- [ ] All byte-for-byte integration tests pass (48 golden files match pipeline output)
- [ ] No regression in existing tests (~1,384+ tests)
- [ ] Line coverage >= 95%
- [ ] Branch coverage >= 90%
- [ ] Coverage unchanged from baseline (expected: ~99.6% lines, ~97.84% branches)

**Acceptance Criteria:**
- 0 test failures
- Coverage thresholds met
- All new content tests pass

---

## Dependency Graph

```
TASK-1 (RED: template test) ──────────> TASK-4 (GREEN: create template) ──┐
                                                                          |
TASK-2 (RED: x-review test) ──────────> TASK-5 (GREEN: modify x-review) ──┤
                                             |                            |
TASK-3 (RED: lifecycle test) ─────────> TASK-6 (GREEN: modify lifecycle) ─┤
                                                                          |
                                        TASK-7 (GREEN: GitHub copies) ────┤
                                             Depends On: TASK-5, TASK-6   |
                                                                          |
                                        TASK-8 (GREEN: golden files) ─────┤
                                             Depends On: TASK-5,6,7       |
                                                                          |
                                        TASK-9 (VERIFY: test suite) ──────┘
                                             Depends On: TASK-8
```

**Parallelism notes:**
- TASK-1, TASK-2, TASK-3 are fully parallel (different test files, no shared state)
- TASK-4 depends only on TASK-1
- TASK-5 depends on TASK-2 and TASK-4
- TASK-6 depends on TASK-3 and TASK-4
- TASK-5 and TASK-6 are parallel (different files)
- TASK-7 depends on TASK-5 and TASK-6 (both source templates must be finalized)
- TASK-8 depends on TASK-5, TASK-6, TASK-7 (all source templates complete)
- TASK-9 is final verification (sequential, depends on everything)

---

## Execution Summary

| Task | Group | TDD Phase | Tier | Budget | Parallel | Depends On | Files Changed |
|------|-------|-----------|------|--------|----------|------------|---------------|
| TASK-1 | G1 | RED | Mid | M | yes | none | 1 (new test) |
| TASK-2 | G1 | RED | Mid | M | yes | none | 1 (new test) |
| TASK-3 | G1 | RED | Mid | S | yes | none | 1 (new test) |
| TASK-4 | G2 | GREEN | Senior | L | no | TASK-1 | 1 (new template) |
| TASK-5 | G2 | GREEN | Senior | L | yes | TASK-2, TASK-4 | 1 (modify skill) |
| TASK-6 | G2 | GREEN | Mid | M | yes | TASK-3, TASK-4 | 1 (modify skill) |
| TASK-7 | G3 | GREEN | Mid | M | no | TASK-5, TASK-6 | 2 (GitHub copies) |
| TASK-8 | G4 | GREEN | Junior | S | no | TASK-5, TASK-6, TASK-7 | 48 (golden files) |
| TASK-9 | G5 | VERIFY | Junior | S | no | TASK-8 | 0 (read-only) |

**Total files modified:** 53 (3 new test files + 1 new template + 4 skill modifications + 48 golden file updates - 3 tests not counted in golden)
**Total new tests:** 3 test files (~20 test cases)
**Total TypeScript source changes:** 0
