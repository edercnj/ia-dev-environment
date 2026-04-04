# Test Plan -- story-0004-0016

## Security Threat Model Documentation

### Summary

This story creates a STRIDE-based threat model template (`resources/templates/_TEMPLATE-THREAT-MODEL.md`) and modifies four skill instruction files (x-review and x-dev-lifecycle, both Claude and GitHub copies) to include threat model update instructions with severity-based auto-add rules. No TypeScript pipeline code changes. Testing relies on content validation of the new template, content validation of the 4 modified skill files, dual copy consistency checks, and byte-for-byte golden file parity across all 8 profiles x 3 output directories.

---

## 1. Test Strategy Overview

| Category | Scope | New Tests? | Test File |
|----------|-------|------------|-----------|
| Content validation (template) | Verify `_TEMPLATE-THREAT-MODEL.md` structure, STRIDE sections, risk tables, enums | YES | `tests/node/content/threat-model-template-content.test.ts` |
| Content validation (x-review Claude) | Verify threat model update instructions in Claude x-review source | YES | `tests/node/content/threat-model-template-content.test.ts` |
| Content validation (x-review GitHub) | Verify threat model update instructions in GitHub x-review source | YES | `tests/node/content/threat-model-template-content.test.ts` |
| Content validation (x-dev-lifecycle Claude) | Verify threat model reference in Claude x-dev-lifecycle source | YES | `tests/node/content/threat-model-template-content.test.ts` |
| Content validation (x-dev-lifecycle GitHub) | Verify threat model reference in GitHub x-dev-lifecycle source | YES | `tests/node/content/threat-model-template-content.test.ts` |
| Dual copy consistency (RULE-001) | Verify Claude and GitHub copies contain semantically equivalent threat model content | YES | `tests/node/content/threat-model-template-content.test.ts` |
| Golden file integration | Verify pipeline output matches updated golden files | NO (existing) | `tests/node/integration/byte-for-byte.test.ts` |
| Assembler unit tests | Verify copy logic works for x-review, x-dev-lifecycle | NO (existing) | `tests/node/assembler/skills-assembler.test.ts` |

---

## 2. Acceptance Tests (AT-N) -- Outer Loop (Double-Loop TDD)

Acceptance tests define the "done" criteria. They stay RED until all inner-loop unit tests drive the implementation to completion.

### AT-1: Template exists with all STRIDE sections

| Field | Value |
|-------|-------|
| **What is tested** | The `_TEMPLATE-THREAT-MODEL.md` template file exists in `resources/templates/` and contains all 6 STRIDE categories as H3 headings under a `## STRIDE Analysis` section, a `## Trust Boundaries` section with Mermaid diagram, a `## Risk Summary` section, and a `## Change History` section |
| **Expected result** | File exists and all structural assertions pass |
| **Depends On** | TASK-1 (create template file), TASK-2 (populate template content) |
| **Parallel** | Independent of AT-2 and AT-3 |
| **Implementation** | Read file, assert `.toContain()` for each H2/H3 heading, assert Mermaid code fence, assert risk table column headers, assert severity/status enum values |

### AT-2: Skill modifications contain threat model instructions

| Field | Value |
|-------|-------|
| **What is tested** | All 4 skill source files (Claude x-review, GitHub x-review, Claude x-dev-lifecycle, GitHub x-dev-lifecycle) contain threat model update instructions. The x-review files contain severity-based auto-add rules and threat model extraction step. The x-dev-lifecycle files reference threat model in documentation phase. |
| **Expected result** | All 4 files contain the required threat model content |
| **Depends On** | TASK-3 (modify x-review Claude), TASK-4 (modify x-review GitHub), TASK-5 (modify x-dev-lifecycle Claude), TASK-6 (modify x-dev-lifecycle GitHub) |
| **Parallel** | Independent of AT-1 and AT-3 |
| **Implementation** | Read each file, assert threat model keywords present |

### AT-3: Golden files match after pipeline generation

| Field | Value |
|-------|-------|
| **What is tested** | After updating golden files, the pipeline generates output that matches golden files byte-for-byte for all 8 profiles across .claude, .agents, and .github output directories |
| **Expected result** | All 8 profiles pass (40 test assertions: 5 per profile) |
| **Depends On** | TASK-7 (update x-review golden files -- 24 files), TASK-8 (update x-dev-lifecycle golden files -- 24 files) |
| **Parallel** | Depends on AT-1 and AT-2 being GREEN (template and skills must be finalized before golden files are updated) |
| **Implementation** | Existing `byte-for-byte.test.ts` infrastructure -- no new test code needed |

---

## 3. Unit Tests (UT-N) -- Inner Loop, TPP Order (simple to complex)

Tests are ordered by TPP (Transformation Priority Premise): degenerate cases first, then unconditional paths, then conditionals.

### 3.1 Template Existence and Structure (Degenerate / Constant)

#### UT-1: Template file exists

| Field | Value |
|-------|-------|
| **What is tested** | File `resources/templates/_TEMPLATE-THREAT-MODEL.md` exists on disk |
| **Expected result** | `fs.existsSync()` returns `true` |
| **Depends On** | TASK-1 |
| **Parallel** | yes |
| **TPP Level** | 1 (degenerate -- file existence is the simplest assertion) |
| **Test name** | `templateFile_exists_fileIsPresent` |

#### UT-2: Template contains H1 with `{{SERVICE_NAME}}` placeholder

| Field | Value |
|-------|-------|
| **What is tested** | Template starts with an H1 heading containing `{{SERVICE_NAME}}` placeholder |
| **Expected result** | Content matches `/^# Threat Model.*\{\{SERVICE_NAME\}\}/m` |
| **Depends On** | TASK-2 |
| **Parallel** | yes |
| **TPP Level** | 2 (unconditional -- single content assertion) |
| **Test name** | `templateContent_h1Heading_containsServiceNamePlaceholder` |

#### UT-3: Template contains Trust Boundaries section with Mermaid diagram

| Field | Value |
|-------|-------|
| **What is tested** | Template has `## Trust Boundaries` section containing a Mermaid code block (`\`\`\`mermaid`) with `subgraph` declarations for External, DMZ, and Internal zones |
| **Expected result** | Content contains `## Trust Boundaries`, `` ```mermaid ``, `subgraph External`, `subgraph DMZ` or equivalent, `subgraph Internal` |
| **Depends On** | TASK-2 |
| **Parallel** | yes |
| **TPP Level** | 2 (unconditional) |
| **Test name** | `templateContent_trustBoundaries_containsMermaidDiagramWithZones` |

### 3.2 STRIDE Categories (Unconditional -- Collection)

#### UT-4: Template contains all 6 STRIDE categories as H3 sections

| Field | Value |
|-------|-------|
| **What is tested** | Template contains 6 H3 headings under `## STRIDE Analysis`: `### Spoofing`, `### Tampering`, `### Repudiation`, `### Information Disclosure`, `### Denial of Service`, `### Elevation of Privilege` |
| **Expected result** | All 6 headings found via `.toContain()`. Use `it.each` for each STRIDE category. |
| **Depends On** | TASK-2 |
| **Parallel** | yes |
| **TPP Level** | 2 (unconditional -- iterating a fixed collection) |
| **Test name** | `templateContent_strideAnalysis_containsCategory_%s` (parameterized) |

#### UT-5: Each STRIDE category has risk table with correct columns

| Field | Value |
|-------|-------|
| **What is tested** | Each STRIDE category section contains a Markdown table with columns: `Threat`, `Severity`, `Mitigation`, `Status`, `Story Ref` |
| **Expected result** | Content matches table header pattern `| Threat | Severity | Mitigation | Status | Story Ref |` appearing after each STRIDE H3 heading |
| **Depends On** | TASK-2 |
| **Parallel** | yes |
| **TPP Level** | 2 (unconditional) |
| **Test name** | `templateContent_riskTable_containsRequiredColumns` |

### 3.3 Supplementary Sections (Unconditional)

#### UT-6: Template contains Risk Summary section with severity counts table

| Field | Value |
|-------|-------|
| **What is tested** | Template has `## Risk Summary` section with a table containing rows for `Critical`, `High`, `Medium`, `Low` severity counts |
| **Expected result** | Content contains `## Risk Summary` and a table with `Critical`, `High`, `Medium`, `Low` rows |
| **Depends On** | TASK-2 |
| **Parallel** | yes |
| **TPP Level** | 2 (unconditional) |
| **Test name** | `templateContent_riskSummary_containsSeverityCountsTable` |

#### UT-7: Template contains Change History section with table

| Field | Value |
|-------|-------|
| **What is tested** | Template has `## Change History` section with a table containing columns: `Date`, `Story`, `Threats Added/Updated` (or semantically equivalent column names) |
| **Expected result** | Content contains `## Change History` and a table header with date, story, and threats columns |
| **Depends On** | TASK-2 |
| **Parallel** | yes |
| **TPP Level** | 2 (unconditional) |
| **Test name** | `templateContent_changeHistory_containsTableWithRequiredColumns` |

### 3.4 Enum Values (Unconditional)

#### UT-8: Severity enum values are Critical, High, Medium, Low

| Field | Value |
|-------|-------|
| **What is tested** | Template contains all 4 severity enum values: `Critical`, `High`, `Medium`, `Low` |
| **Expected result** | Content contains all 4 values. Use `it.each` for each severity level. |
| **Depends On** | TASK-2 |
| **Parallel** | yes |
| **TPP Level** | 2 (unconditional) |
| **Test name** | `templateContent_severityEnum_containsValue_%s` (parameterized) |

#### UT-9: Status enum values are Open, Mitigated, Accepted, Under Review

| Field | Value |
|-------|-------|
| **What is tested** | Template contains all 4 status enum values: `Open`, `Mitigated`, `Accepted`, `Under Review` |
| **Expected result** | Content contains all 4 values. Use `it.each` for each status value. |
| **Depends On** | TASK-2 |
| **Parallel** | yes |
| **TPP Level** | 2 (unconditional) |
| **Test name** | `templateContent_statusEnum_containsValue_%s` (parameterized) |

### 3.5 x-review Skill Modifications (Unconditional / Conditional)

#### UT-10: x-review Claude SKILL.md contains threat model update instructions

| Field | Value |
|-------|-------|
| **What is tested** | The Claude x-review source (`resources/skills-templates/core/x-review/SKILL.md`) contains instructions for extracting security findings and updating the threat model after the review consolidation phase |
| **Expected result** | Content contains `threat model` (case-insensitive) in the context of a post-review step |
| **Depends On** | TASK-3 |
| **Parallel** | yes |
| **TPP Level** | 2 (unconditional) |
| **Test name** | `xReviewClaude_containsThreatModelUpdateInstructions` |

#### UT-11: x-review Claude SKILL.md contains severity-based auto-add rules

| Field | Value |
|-------|-------|
| **What is tested** | The Claude x-review source contains severity-based auto-add rules: Critical/High findings get status `Open`, Medium findings get status `Under Review` |
| **Expected result** | Content contains references to severity-based rules with `Critical`, `High`, `Open`, `Medium`, `Under Review` in proximity |
| **Depends On** | TASK-3 |
| **Parallel** | yes |
| **TPP Level** | 3 (conditional -- rules have branching logic based on severity) |
| **Test name** | `xReviewClaude_containsSeverityBasedAutoAddRules` |

#### UT-12: x-dev-lifecycle Claude SKILL.md references threat model

| Field | Value |
|-------|-------|
| **What is tested** | The Claude x-dev-lifecycle source (`resources/skills-templates/core/x-dev-lifecycle/SKILL.md`) references the threat model in its documentation or post-architecture-plan phase |
| **Expected result** | Content contains `threat model` (case-insensitive) |
| **Depends On** | TASK-5 |
| **Parallel** | yes |
| **TPP Level** | 2 (unconditional) |
| **Test name** | `xDevLifecycleClaude_referencesThreatModel` |

### 3.6 GitHub Skill Modifications (Unconditional / Conditional)

#### UT-13: GitHub x-review.md contains parallel threat model instructions

| Field | Value |
|-------|-------|
| **What is tested** | The GitHub x-review source (`resources/github-skills-templates/review/x-review.md`) contains threat model update instructions semantically equivalent to the Claude source |
| **Expected result** | Content contains `threat model` (case-insensitive) and severity-based rules |
| **Depends On** | TASK-4 |
| **Parallel** | yes |
| **TPP Level** | 2 (unconditional) |
| **Test name** | `xReviewGithub_containsThreatModelUpdateInstructions` |

#### UT-14: GitHub x-dev-lifecycle.md references threat model

| Field | Value |
|-------|-------|
| **What is tested** | The GitHub x-dev-lifecycle source (`resources/github-skills-templates/dev/x-dev-lifecycle.md`) references the threat model |
| **Expected result** | Content contains `threat model` (case-insensitive) |
| **Depends On** | TASK-6 |
| **Parallel** | yes |
| **TPP Level** | 2 (unconditional) |
| **Test name** | `xDevLifecycleGithub_referencesThreatModel` |

### 3.7 Template Structural Integrity (Unconditional)

#### UT-15: Template H1 is the first heading

| Field | Value |
|-------|-------|
| **What is tested** | The first markdown heading in the template is an H1 (not H2 or H3) |
| **Expected result** | First line matching `/^#{1,6}\s/` is an H1 (`/^# /`) |
| **Depends On** | TASK-2 |
| **Parallel** | yes |
| **TPP Level** | 2 (unconditional) |
| **Test name** | `templateStructure_firstHeading_isH1` |

#### UT-16: Template uses valid Markdown syntax for all headings

| Field | Value |
|-------|-------|
| **What is tested** | All heading lines use valid Markdown syntax (`#{1,6} followed by space and non-empty text`) |
| **Expected result** | Every line matching `/^#{1,6}\s/` also matches `/^#{1,6}\s+\S/` |
| **Depends On** | TASK-2 |
| **Parallel** | yes |
| **TPP Level** | 2 (unconditional) |
| **Test name** | `templateStructure_allHeadings_useValidMarkdownSyntax` |

#### UT-17: STRIDE Analysis section appears after Trust Boundaries

| Field | Value |
|-------|-------|
| **What is tested** | The `## STRIDE Analysis` section appears in the document after `## Trust Boundaries` |
| **Expected result** | Index of `## STRIDE Analysis` is greater than index of `## Trust Boundaries` |
| **Depends On** | TASK-2 |
| **Parallel** | yes |
| **TPP Level** | 2 (unconditional) |
| **Test name** | `templateStructure_strideAnalysis_appearsAfterTrustBoundaries` |

#### UT-18: Risk Summary appears after STRIDE Analysis

| Field | Value |
|-------|-------|
| **What is tested** | The `## Risk Summary` section appears after all STRIDE category sections |
| **Expected result** | Index of `## Risk Summary` is greater than index of `### Elevation of Privilege` (last STRIDE category) |
| **Depends On** | TASK-2 |
| **Parallel** | yes |
| **TPP Level** | 2 (unconditional) |
| **Test name** | `templateStructure_riskSummary_appearsAfterStrideAnalysis` |

#### UT-19: Change History appears after Risk Summary

| Field | Value |
|-------|-------|
| **What is tested** | The `## Change History` section is the last major section in the template |
| **Expected result** | Index of `## Change History` is greater than index of `## Risk Summary` |
| **Depends On** | TASK-2 |
| **Parallel** | yes |
| **TPP Level** | 2 (unconditional) |
| **Test name** | `templateStructure_changeHistory_appearsAfterRiskSummary` |

### 3.8 Dual Copy Consistency (RULE-001)

#### UT-20: Both x-review copies contain threat model instructions

| Field | Value |
|-------|-------|
| **What is tested** | Both Claude and GitHub x-review sources contain `threat model` content |
| **Expected result** | Both files pass `.toContain()` for threat model keywords |
| **Depends On** | TASK-3, TASK-4 |
| **Parallel** | yes |
| **TPP Level** | 2 (unconditional) |
| **Test name** | `dualCopy_xReview_bothContainThreatModelInstructions` |

#### UT-21: Both x-review copies contain severity-based auto-add rules

| Field | Value |
|-------|-------|
| **What is tested** | Both Claude and GitHub x-review sources define severity-based auto-add rules with the same severity-to-status mappings |
| **Expected result** | Both files contain `Critical`, `High`, `Open`, `Medium`, `Under Review` in threat model context |
| **Depends On** | TASK-3, TASK-4 |
| **Parallel** | yes |
| **TPP Level** | 3 (conditional -- verifying branching logic consistency) |
| **Test name** | `dualCopy_xReview_bothContainSeverityAutoAddRules` |

#### UT-22: Both x-dev-lifecycle copies reference threat model

| Field | Value |
|-------|-------|
| **What is tested** | Both Claude and GitHub x-dev-lifecycle sources reference the threat model |
| **Expected result** | Both files contain `threat model` (case-insensitive) |
| **Depends On** | TASK-5, TASK-6 |
| **Parallel** | yes |
| **TPP Level** | 2 (unconditional) |
| **Test name** | `dualCopy_xDevLifecycle_bothReferenceThreatModel` |

#### UT-23: Both x-review copies reference the template file

| Field | Value |
|-------|-------|
| **What is tested** | Both copies reference the `_TEMPLATE-THREAT-MODEL.md` template (or its semantic equivalent path) |
| **Expected result** | Both files contain `_TEMPLATE-THREAT-MODEL` or `threat-model` template reference |
| **Depends On** | TASK-3, TASK-4 |
| **Parallel** | yes |
| **TPP Level** | 2 (unconditional) |
| **Test name** | `dualCopy_xReview_bothReferenceTemplateFile` |

#### UT-24: Both x-review copies contain STRIDE category reference

| Field | Value |
|-------|-------|
| **What is tested** | Both copies mention STRIDE methodology or individual STRIDE categories in the threat model extraction instructions |
| **Expected result** | Both files contain `STRIDE` or at least 3 of the 6 STRIDE category names |
| **Depends On** | TASK-3, TASK-4 |
| **Parallel** | yes |
| **TPP Level** | 2 (unconditional) |
| **Test name** | `dualCopy_xReview_bothContainStrideReference` |

### 3.9 Backward Compatibility

#### UT-25: x-review Claude preserves existing review phases

| Field | Value |
|-------|-------|
| **What is tested** | The Claude x-review source still contains its original execution flow: DETECT, REVIEW, CONSOLIDATE, STORY phases |
| **Expected result** | Content contains `DETECT`, `REVIEW`, `CONSOLIDATE`, `STORY` |
| **Depends On** | TASK-3 |
| **Parallel** | yes |
| **TPP Level** | 2 (unconditional) |
| **Test name** | `xReviewClaude_preservesExistingReviewPhases` |

#### UT-26: x-review GitHub preserves existing review phases

| Field | Value |
|-------|-------|
| **What is tested** | The GitHub x-review source still contains its original execution flow |
| **Expected result** | Content contains `DETECT`, `REVIEW`, `CONSOLIDATE`, `STORY` |
| **Depends On** | TASK-4 |
| **Parallel** | yes |
| **TPP Level** | 2 (unconditional) |
| **Test name** | `xReviewGithub_preservesExistingReviewPhases` |

#### UT-27: x-dev-lifecycle Claude preserves 8-phase structure

| Field | Value |
|-------|-------|
| **What is tested** | The Claude x-dev-lifecycle source still declares "8 phases (0-7)" and "NEVER stop before Phase 7" |
| **Expected result** | Content matches `/8 phases.*0-7/i` and contains `NEVER stop before Phase 7` |
| **Depends On** | TASK-5 |
| **Parallel** | yes |
| **TPP Level** | 2 (unconditional) |
| **Test name** | `xDevLifecycleClaude_preserves8PhaseStructure` |

#### UT-28: x-dev-lifecycle GitHub preserves 8-phase structure

| Field | Value |
|-------|-------|
| **What is tested** | The GitHub x-dev-lifecycle source preserves the 8-phase structure |
| **Expected result** | Content matches `/8 phases.*0-7/i` and contains `NEVER stop before Phase 7` |
| **Depends On** | TASK-6 |
| **Parallel** | yes |
| **TPP Level** | 2 (unconditional) |
| **Test name** | `xDevLifecycleGithub_preserves8PhaseStructure` |

### 3.10 x-review Threat Model Content Detail

#### UT-29: x-review Claude specifies incremental update behavior

| Field | Value |
|-------|-------|
| **What is tested** | The Claude x-review source instructs the AI to update the threat model incrementally, preserving existing entries |
| **Expected result** | Content contains `incremental` or `preserve` or `append` in the threat model context |
| **Depends On** | TASK-3 |
| **Parallel** | yes |
| **TPP Level** | 2 (unconditional) |
| **Test name** | `xReviewClaude_specifiesIncrementalUpdateBehavior` |

#### UT-30: x-review Claude specifies threat model output path

| Field | Value |
|-------|-------|
| **What is tested** | The Claude x-review source references the threat model output path `docs/security/threat-model.md` |
| **Expected result** | Content contains `docs/security/threat-model.md` or equivalent path reference |
| **Depends On** | TASK-3 |
| **Parallel** | yes |
| **TPP Level** | 2 (unconditional) |
| **Test name** | `xReviewClaude_specifiesThreatModelOutputPath` |

---

## 4. Integration Tests (IT-N)

### IT-1: Pipeline generates x-review skill with threat model content in .claude output

| Field | Value |
|-------|-------|
| **What is tested** | After running the pipeline for any profile, the output `.claude/skills/x-review/SKILL.md` contains threat model update instructions |
| **Expected result** | Generated file contains `threat model` (case-insensitive) |
| **Depends On** | TASK-3, TASK-7 (golden files updated) |
| **Parallel** | IT-1, IT-2, IT-3 can run in parallel (they read different output directories from the same pipeline run) |
| **Implementation** | Covered implicitly by `byte-for-byte.test.ts` -- if golden files contain threat model content and pipeline output matches golden files, the content is present. A supplementary spot-check can be added for clarity. |

### IT-2: Pipeline generates x-review skill with threat model content in .agents output

| Field | Value |
|-------|-------|
| **What is tested** | Pipeline output `.agents/skills/x-review/SKILL.md` contains threat model content (mirror of .claude) |
| **Expected result** | Generated file is byte-for-byte identical to `.claude/skills/x-review/SKILL.md` |
| **Depends On** | TASK-3, TASK-7 |
| **Parallel** | yes (see IT-1) |
| **Implementation** | Covered by `byte-for-byte.test.ts` golden file parity |

### IT-3: Pipeline generates x-review skill with threat model content in .github output

| Field | Value |
|-------|-------|
| **What is tested** | Pipeline output `.github/skills/x-review/SKILL.md` contains threat model content (from GitHub source) |
| **Expected result** | Generated file matches updated GitHub golden file |
| **Depends On** | TASK-4, TASK-7 |
| **Parallel** | yes (see IT-1) |
| **Implementation** | Covered by `byte-for-byte.test.ts` golden file parity |

### IT-4: Golden file byte-for-byte match for all 8 profiles

| Field | Value |
|-------|-------|
| **What is tested** | Pipeline output for each profile matches golden files with zero mismatches, zero missing files, zero extra files |
| **Expected result** | All 8 profiles pass: `verification.success === true`, `missingFiles === []`, `extraFiles === []`, `totalFiles > 0` |
| **Depends On** | ALL TASKs (template + skill modifications + golden file updates) |
| **Parallel** | Profiles run sequentially (existing `describe.sequential.each` pattern) |
| **Implementation** | Existing `tests/node/integration/byte-for-byte.test.ts` -- no changes to test infrastructure |

---

## 5. Content Verification -- Key Patterns That Must Appear

The following patterns are used by content tests. Tests use `toContain()` for substring checks and `toMatch()` for regex patterns to avoid brittle exact-line matching.

### 5.1 Template Content Patterns

| Pattern | Purpose | Used by |
|---------|---------|---------|
| `# Threat Model` | H1 heading | UT-2 |
| `{{SERVICE_NAME}}` | Service name placeholder | UT-2 |
| `## Trust Boundaries` | Trust boundaries section | UT-3, UT-17 |
| `` ```mermaid `` | Mermaid code block | UT-3 |
| `subgraph` | Mermaid subgraph declarations | UT-3 |
| `External` | External trust zone | UT-3 |
| `Internal` | Internal trust zone | UT-3 |
| `## STRIDE Analysis` | STRIDE parent section | UT-4, UT-17 |
| `### Spoofing` | STRIDE category | UT-4 |
| `### Tampering` | STRIDE category | UT-4 |
| `### Repudiation` | STRIDE category | UT-4 |
| `### Information Disclosure` | STRIDE category | UT-4 |
| `### Denial of Service` | STRIDE category | UT-4 |
| `### Elevation of Privilege` | STRIDE category | UT-4 |
| `| Threat | Severity | Mitigation | Status | Story Ref |` | Risk table header | UT-5 |
| `## Risk Summary` | Risk summary section | UT-6, UT-18, UT-19 |
| `Critical` | Severity enum value | UT-6, UT-8 |
| `High` | Severity enum value | UT-6, UT-8 |
| `Medium` | Severity enum value | UT-6, UT-8 |
| `Low` | Severity enum value | UT-6, UT-8 |
| `## Change History` | Change history section | UT-7, UT-19 |
| `Date` | Change history column | UT-7 |
| `Story` | Change history column | UT-7 |
| `Open` | Status enum value | UT-9 |
| `Mitigated` | Status enum value | UT-9 |
| `Accepted` | Status enum value | UT-9 |
| `Under Review` | Status enum value | UT-9 |

### 5.2 Skill Content Patterns

| Pattern | Purpose | Used by |
|---------|---------|---------|
| `threat model` (case-insensitive) | Threat model reference in skills | UT-10, UT-12, UT-13, UT-14 |
| `Critical` + `High` + `Open` (in proximity) | Severity-to-status mapping rule | UT-11 |
| `Medium` + `Under Review` (in proximity) | Medium severity rule | UT-11 |
| `STRIDE` | Methodology reference | UT-24 |
| `DETECT` / `REVIEW` / `CONSOLIDATE` / `STORY` | Existing x-review phases | UT-25, UT-26 |
| `8 phases` + `0-7` | Lifecycle phase count | UT-27, UT-28 |
| `NEVER stop before Phase 7` | Critical execution rule | UT-27, UT-28 |
| `incremental` or `preserve` or `append` | Update behavior | UT-29 |
| `docs/security/threat-model.md` | Output path reference | UT-30 |

---

## 6. Golden Files Requiring Update

**Total: up to 48 golden files** (8 profiles x 3 output directories x 2 skills)

### 6.1 x-review Skill -- 24 Golden Files

| Copy | Count | Path Pattern |
|------|-------|-------------|
| .claude | 8 | `tests/golden/{profile}/.claude/skills/x-review/SKILL.md` |
| .agents | 8 | `tests/golden/{profile}/.agents/skills/x-review/SKILL.md` |
| .github | 8 | `tests/golden/{profile}/.github/skills/x-review/SKILL.md` |

### 6.2 x-dev-lifecycle Skill -- 24 Golden Files (if modified)

| Copy | Count | Path Pattern |
|------|-------|-------------|
| .claude | 8 | `tests/golden/{profile}/.claude/skills/x-dev-lifecycle/SKILL.md` |
| .agents | 8 | `tests/golden/{profile}/.agents/skills/x-dev-lifecycle/SKILL.md` |
| .github | 8 | `tests/golden/{profile}/.github/skills/x-dev-lifecycle/SKILL.md` |

### 6.3 Golden File Update Strategy

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

# x-dev-lifecycle (if modified)
CLAUDE_LIFECYCLE="resources/skills-templates/core/x-dev-lifecycle/SKILL.md"
GITHUB_LIFECYCLE="resources/github-skills-templates/dev/x-dev-lifecycle.md"
for profile in "${PROFILES[@]}"; do
  cp "$CLAUDE_LIFECYCLE" "tests/golden/$profile/.claude/skills/x-dev-lifecycle/SKILL.md"
  cp "$CLAUDE_LIFECYCLE" "tests/golden/$profile/.agents/skills/x-dev-lifecycle/SKILL.md"
  cp "$GITHUB_LIFECYCLE" "tests/golden/$profile/.github/skills/x-dev-lifecycle/SKILL.md"
done
```

---

## 7. Suggested Test Implementation Pattern

```typescript
import { describe, it, expect } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";

// --- Source paths ---
const TEMPLATE_PATH = path.resolve(
  __dirname, "../../..",
  "resources/templates/_TEMPLATE-THREAT-MODEL.md",
);
const CLAUDE_XREVIEW_PATH = path.resolve(
  __dirname, "../../..",
  "resources/skills-templates/core/x-review/SKILL.md",
);
const GITHUB_XREVIEW_PATH = path.resolve(
  __dirname, "../../..",
  "resources/github-skills-templates/review/x-review.md",
);
const CLAUDE_LIFECYCLE_PATH = path.resolve(
  __dirname, "../../..",
  "resources/skills-templates/core/x-dev-lifecycle/SKILL.md",
);
const GITHUB_LIFECYCLE_PATH = path.resolve(
  __dirname, "../../..",
  "resources/github-skills-templates/dev/x-dev-lifecycle.md",
);

// --- Read content once ---
const templateContent = fs.readFileSync(TEMPLATE_PATH, "utf-8");
const claudeXReview = fs.readFileSync(CLAUDE_XREVIEW_PATH, "utf-8");
const githubXReview = fs.readFileSync(GITHUB_XREVIEW_PATH, "utf-8");
const claudeLifecycle = fs.readFileSync(CLAUDE_LIFECYCLE_PATH, "utf-8");
const githubLifecycle = fs.readFileSync(GITHUB_LIFECYCLE_PATH, "utf-8");

const STRIDE_CATEGORIES = [
  "Spoofing",
  "Tampering",
  "Repudiation",
  "Information Disclosure",
  "Denial of Service",
  "Elevation of Privilege",
];

const SEVERITY_VALUES = ["Critical", "High", "Medium", "Low"];

const STATUS_VALUES = ["Open", "Mitigated", "Accepted", "Under Review"];

// --- AT-1: Template exists with all STRIDE sections ---
describe("AT-1: Threat model template completeness", () => {
  // UT-1 through UT-9, UT-15 through UT-19
});

// --- AT-2: Skill modifications contain threat model instructions ---
describe("AT-2: Skill threat model instructions", () => {
  // UT-10 through UT-14, UT-20 through UT-30
});

// --- Template content (UT-1 to UT-9) ---
describe("Threat model template — existence and structure", () => {
  // UT-1: templateFile_exists_fileIsPresent
  // UT-2: templateContent_h1Heading_containsServiceNamePlaceholder
  // UT-3: templateContent_trustBoundaries_containsMermaidDiagramWithZones
});

describe("Threat model template — STRIDE categories", () => {
  // UT-4: templateContent_strideAnalysis_containsCategory_%s
  // UT-5: templateContent_riskTable_containsRequiredColumns
});

describe("Threat model template — supplementary sections", () => {
  // UT-6: templateContent_riskSummary_containsSeverityCountsTable
  // UT-7: templateContent_changeHistory_containsTableWithRequiredColumns
});

describe("Threat model template — enum values", () => {
  // UT-8: templateContent_severityEnum_containsValue_%s
  // UT-9: templateContent_statusEnum_containsValue_%s
});

describe("Threat model template — section ordering", () => {
  // UT-15: templateStructure_firstHeading_isH1
  // UT-16: templateStructure_allHeadings_useValidMarkdownSyntax
  // UT-17: templateStructure_strideAnalysis_appearsAfterTrustBoundaries
  // UT-18: templateStructure_riskSummary_appearsAfterStrideAnalysis
  // UT-19: templateStructure_changeHistory_appearsAfterRiskSummary
});

// --- x-review skill content (UT-10, UT-11) ---
describe("x-review Claude source — threat model content", () => {
  // UT-10: xReviewClaude_containsThreatModelUpdateInstructions
  // UT-11: xReviewClaude_containsSeverityBasedAutoAddRules
  // UT-25: xReviewClaude_preservesExistingReviewPhases
  // UT-29: xReviewClaude_specifiesIncrementalUpdateBehavior
  // UT-30: xReviewClaude_specifiesThreatModelOutputPath
});

// --- x-review GitHub source (UT-13) ---
describe("x-review GitHub source — threat model content", () => {
  // UT-13: xReviewGithub_containsThreatModelUpdateInstructions
  // UT-26: xReviewGithub_preservesExistingReviewPhases
});

// --- x-dev-lifecycle skill content (UT-12, UT-14) ---
describe("x-dev-lifecycle Claude source — threat model reference", () => {
  // UT-12: xDevLifecycleClaude_referencesThreatModel
  // UT-27: xDevLifecycleClaude_preserves8PhaseStructure
});

describe("x-dev-lifecycle GitHub source — threat model reference", () => {
  // UT-14: xDevLifecycleGithub_referencesThreatModel
  // UT-28: xDevLifecycleGithub_preserves8PhaseStructure
});

// --- Dual copy consistency (UT-20 to UT-24) ---
describe("Dual copy consistency (RULE-001)", () => {
  // UT-20: dualCopy_xReview_bothContainThreatModelInstructions
  // UT-21: dualCopy_xReview_bothContainSeverityAutoAddRules
  // UT-22: dualCopy_xDevLifecycle_bothReferenceThreatModel
  // UT-23: dualCopy_xReview_bothReferenceTemplateFile
  // UT-24: dualCopy_xReview_bothContainStrideReference
});
```

---

## 8. TDD Execution Order

Following test-first approach with Double-Loop TDD:

| Step | Action | Test State |
|------|--------|-----------|
| 1 | Write all content validation tests (UT-1 through UT-30) in `tests/node/content/threat-model-template-content.test.ts` | RED (all tests fail -- template and skill modifications do not exist yet) |
| 2 | Create `resources/templates/_TEMPLATE-THREAT-MODEL.md` with full STRIDE template (TASK-1, TASK-2) | Partial GREEN (UT-1 through UT-9, UT-15 through UT-19 pass; skill tests still RED) |
| 3 | Modify `resources/skills-templates/core/x-review/SKILL.md` with threat model update step (TASK-3) | Partial GREEN (UT-10, UT-11, UT-25, UT-29, UT-30 pass) |
| 4 | Modify `resources/github-skills-templates/review/x-review.md` with parallel changes (TASK-4) | Partial GREEN (UT-13, UT-20, UT-21, UT-23, UT-24, UT-26 pass) |
| 5 | Modify `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` with threat model reference (TASK-5) | Partial GREEN (UT-12, UT-27 pass) |
| 6 | Modify `resources/github-skills-templates/dev/x-dev-lifecycle.md` with parallel changes (TASK-6) | GREEN (UT-14, UT-22, UT-28 pass; all content tests green) |
| 7 | Copy sources to golden files using script from Section 6.3 (TASK-7, TASK-8) | N/A (golden files updated) |
| 8 | Run byte-for-byte integration tests | GREEN (IT-1 through IT-4 pass) |
| 9 | Run full test suite (`npx vitest run`) | GREEN (all existing tests pass, plus ~30 new tests) |

---

## 9. Verification Checklist

- [ ] `npx vitest run tests/node/content/threat-model-template-content.test.ts` -- all content validation tests pass
- [ ] `npx vitest run tests/node/integration/byte-for-byte.test.ts` -- all 8 profiles pass (40 assertions)
- [ ] `npx vitest run` -- full suite passes
- [ ] Coverage remains >= 95% line, >= 90% branch (no TypeScript code changes, so coverage unaffected)
- [ ] No compiler/linter warnings introduced
- [ ] Template file `resources/templates/_TEMPLATE-THREAT-MODEL.md` contains all 6 STRIDE categories
- [ ] Both x-review copies contain severity-based auto-add rules (RULE-001)
- [ ] Both x-dev-lifecycle copies reference threat model (RULE-001)

---

## 10. Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| Golden file mismatch after skill edit | Mechanical copy script (Section 6.3) eliminates drift; byte-for-byte tests catch any mismatch immediately |
| Content test too brittle (exact string matching) | Use `toContain()` for substring checks and `toMatch()` for regex patterns; test semantic presence, not formatting |
| Dual copy inconsistency (RULE-001) | Dedicated consistency tests (UT-20 through UT-24) verify both copies have equivalent threat model content |
| x-review phase structure broken during modification | Backward compatibility tests (UT-25, UT-26) verify DETECT/REVIEW/CONSOLIDATE/STORY phases preserved |
| x-dev-lifecycle phase structure broken | Backward compatibility tests (UT-27, UT-28) verify 8-phase structure preserved |
| Template placeholder collision | `{{SERVICE_NAME}}` is a standard placeholder already used in other templates; no collision risk |
| story-0004-0006 not yet implemented (dependency) | Template can be created independently; skill modifications reference architecture plan output format from data contract |

---

## 11. Files Summary

### 11.1 New Files

| # | File | Description |
|---|------|-------------|
| 1 | `resources/templates/_TEMPLATE-THREAT-MODEL.md` | STRIDE-based threat model template |
| 2 | `tests/node/content/threat-model-template-content.test.ts` | Content validation tests (~30 tests) |

### 11.2 Modified Source Templates

| # | File | Change |
|---|------|--------|
| 3 | `resources/skills-templates/core/x-review/SKILL.md` | Add threat model update step with severity-based auto-add rules |
| 4 | `resources/github-skills-templates/review/x-review.md` | Parallel change for GitHub Copilot |
| 5 | `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` | Add threat model reference in documentation phase |
| 6 | `resources/github-skills-templates/dev/x-dev-lifecycle.md` | Parallel change for GitHub Copilot |

### 11.3 Golden Files Updated (up to 48 files)

8 profiles x 3 output directories x 2 skills = up to 48 golden files.

### 11.4 Existing Test Files (unchanged, covering this story)

| File | Coverage |
|------|----------|
| `tests/node/integration/byte-for-byte.test.ts` | Golden file parity for all 8 profiles |
| `tests/node/assembler/skills-assembler.test.ts` | Claude copy mechanism |
| `tests/node/assembler/codex-skills-assembler.test.ts` | Agents copy mechanism |
| `tests/node/assembler/github-skills-assembler.test.ts` | GitHub copy mechanism |

---

## 12. Test Count Summary

| Category | New Tests | Existing Tests |
|----------|-----------|----------------|
| Template content (UT-1 to UT-9, UT-15 to UT-19) | 14 | 0 |
| x-review content (UT-10, UT-11, UT-25, UT-26, UT-29, UT-30) | 6 | 0 |
| x-dev-lifecycle content (UT-12, UT-14, UT-27, UT-28) | 4 | 0 |
| x-review GitHub content (UT-13) | 1 | 0 |
| Dual copy consistency (UT-20 to UT-24) | 5 | 0 |
| Golden file integration (IT-1 to IT-4) | 0 | 40 (8 profiles x 5 assertions) |
| Assembler unit tests | 0 | ~50 (across 3 assembler test files) |
| **Total** | **30** | **~90** |

> Note: UT-4 (6 STRIDE categories via `it.each`), UT-8 (4 severities via `it.each`), and UT-9 (4 statuses via `it.each`) expand to 14 individual test assertions but are counted as 3 logical tests in the numbering above.
