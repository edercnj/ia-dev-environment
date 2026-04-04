# Test Plan — story-0004-0017

## Summary

This story adds a post-deploy verification sub-step to Phase 7 of the `x-dev-lifecycle` skill template and a new conditional DoD item for smoke test verification. No TypeScript source code changes are required -- this is purely a template content modification. Testing relies on content validation of the 2 modified source files, dual copy consistency checks, and byte-for-byte golden file parity across all 8 profiles x 3 output directories (24 golden files).

---

## 1. Test Strategy Overview

| Category | Scope | New Tests? | Test File |
|----------|-------|------------|-----------|
| Content validation (Claude source) | Verify post-deploy verification section in `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` | YES | `tests/node/content/x-dev-lifecycle-postdeploy-content.test.ts` |
| Content validation (GitHub source) | Verify post-deploy verification section in `resources/github-skills-templates/dev/x-dev-lifecycle.md` | YES | `tests/node/content/x-dev-lifecycle-postdeploy-content.test.ts` (same file, separate describe block) |
| Dual copy consistency | Verify both sources contain semantically identical post-deploy verification content | YES | `tests/node/content/x-dev-lifecycle-postdeploy-content.test.ts` |
| Backward compatibility | Verify existing Phase 7 structure, conditional DoD items, and phase numbering are preserved | YES | `tests/node/content/x-dev-lifecycle-postdeploy-content.test.ts` |
| Golden file integration | Verify pipeline output matches updated golden files | NO (existing) | `tests/node/integration/byte-for-byte.test.ts` |
| Assembler unit tests | Verify copy logic works for x-dev-lifecycle | NO (existing) | `tests/node/assembler/skills-assembler.test.ts` |

---

## 2. Acceptance Tests (AT) — Outer Loop

These are the high-level integration-style tests that validate the complete story is done. They form the outer loop of Double-Loop TDD and should be written first (RED), then turn GREEN once all implementation is complete.

### AT-1: Pipeline generates lifecycle template with post-deploy verification for all profiles

**What:** Run the pipeline for all 8 profiles and verify that the generated `.claude/skills/x-dev-lifecycle/SKILL.md` output contains the post-deploy verification section.

**Validation:** Byte-for-byte golden file comparison via existing `byte-for-byte.test.ts`.

- **Depends On:** TASK-1 (source template modification), TASK-2 (GitHub template modification), TASK-3 (golden file update)
- **Parallel:** no (depends on all implementation tasks)

### AT-2: Both dual copies are consistent after modification

**What:** Both the Claude source template and the GitHub source template contain semantically equivalent post-deploy verification content (RULE-001).

**Validation:** Dual copy consistency tests (Section 2.3 below).

- **Depends On:** TASK-1, TASK-2
- **Parallel:** yes (can run in parallel with AT-1 golden file update)

---

## 3. Unit Tests (UT) — Inner Loop (TPP Order)

Content validation tests, ordered from degenerate (simplest) to edge cases (most complex), following the Transformation Priority Premise.

### 3.1 File: `tests/node/content/x-dev-lifecycle-postdeploy-content.test.ts`

**Source file under test (Claude):** `resources/skills-templates/core/x-dev-lifecycle/SKILL.md`
**Source file under test (GitHub):** `resources/github-skills-templates/dev/x-dev-lifecycle.md`

---

#### 3.1.1 Claude Source — Post-Deploy Verification Section Existence (TPP Level 1: nil -> constant)

| # | Test Name | What It Validates | Depends On | Parallel |
|---|-----------|-------------------|------------|----------|
| UT-1 | `claudeSource_phase7_containsPostDeployVerificationSubsection` | Phase 7 contains a sub-section or numbered item mentioning "Post-Deploy Verification" | TASK-1 | yes |
| UT-2 | `claudeSource_phase7_postDeployIsConditionalOnSmokeTests` | Post-deploy verification is conditional on `smoke_tests` being true | TASK-1 | yes |
| UT-3 | `claudeSource_phase7_postDeployContainsHealthCheck` | Sub-section includes "Health Check" verification (GET /health -> 200 OK) | TASK-1 | yes |
| UT-4 | `claudeSource_phase7_postDeployContainsCriticalPath` | Sub-section includes "Critical Path" verification | TASK-1 | yes |
| UT-5 | `claudeSource_phase7_postDeployContainsResponseTimeSLO` | Sub-section includes "Response Time" or "p95" or "SLO" verification | TASK-1 | yes |
| UT-6 | `claudeSource_phase7_postDeployContainsErrorRate` | Sub-section includes "Error Rate" or error rate threshold (1%) | TASK-1 | yes |

#### 3.1.2 Claude Source — Post-Deploy Result Semantics (TPP Level 2: constant -> constant+)

| # | Test Name | What It Validates | Depends On | Parallel |
|---|-----------|-------------------|------------|----------|
| UT-7 | `claudeSource_phase7_postDeployResultPASS` | Result includes PASS outcome (all checks green -> "Deploy confirmed") | TASK-1 | yes |
| UT-8 | `claudeSource_phase7_postDeployResultFAIL` | Result includes FAIL outcome (any check red -> "Investigate rollback") | TASK-1 | yes |
| UT-9 | `claudeSource_phase7_postDeployResultSKIP` | Result includes SKIP outcome (smoke_tests == false -> verification skipped) | TASK-1 | yes |
| UT-10 | `claudeSource_phase7_postDeployIsNonBlocking` | Post-deploy verification is explicitly non-blocking (emits result for human decision, no auto-rollback) | TASK-1 | yes |

#### 3.1.3 Claude Source — Skill Invocation References (TPP Level 3: scalar)

| # | Test Name | What It Validates | Depends On | Parallel |
|---|-----------|-------------------|------------|----------|
| UT-11 | `claudeSource_phase7_postDeployReferencesRunE2EOrSmokeSkill` | Sub-section references `/run-e2e` or `/run-smoke-api` for automated verification | TASK-1 | yes |

#### 3.1.4 Claude Source — Conditional DoD Item Addition (TPP Level 2: constant -> constant+)

| # | Test Name | What It Validates | Depends On | Parallel |
|---|-----------|-------------------|------------|----------|
| UT-12 | `claudeSource_phase7_conditionalDoDContainsPostDeployItem` | The conditional DoD items list (item 5) includes a post-deploy verification entry | TASK-1 | yes |
| UT-13 | `claudeSource_phase7_conditionalDoDPostDeployLinkedToSmokeTests` | The new DoD item references `smoke_tests` as its condition | TASK-1 | yes |

#### 3.1.5 Claude Source — Item Renumbering (TPP Level 3: scalar)

| # | Test Name | What It Validates | Depends On | Parallel |
|---|-----------|-------------------|------------|----------|
| UT-14 | `claudeSource_phase7_reportPassFailIsRenumbered` | The "Report PASS/FAIL result" item is renumbered (was 6, now 7 or later) | TASK-1 | yes |
| UT-15 | `claudeSource_phase7_gitCheckoutMainIsRenumbered` | The `git checkout main && git pull origin main` item is renumbered (was 7, now 8 or later) | TASK-1 | yes |

#### 3.1.6 Claude Source — Backward Compatibility (TPP Level 4: conditions / edge cases)

| # | Test Name | What It Validates | Depends On | Parallel |
|---|-----------|-------------------|------------|----------|
| UT-16 | `claudeSource_preserves8PhaseCount` | "8 phases (0-7)" wording is preserved | TASK-1 | yes |
| UT-17 | `claudeSource_preservesCriticalExecutionRule` | "NEVER stop before Phase 7" rule is preserved | TASK-1 | yes |
| UT-18 | `claudeSource_preservesPhase7OnlyLegitimateStoppingPoint` | "Phase 7 is the ONLY legitimate stopping point" text is preserved | TASK-1 | yes |
| UT-19 | `claudeSource_preservesExistingConditionalDoDItems` | All existing conditional DoD items are preserved (contract_tests, event_driven, compliance, api_gateway, grpc, graphql) | TASK-1 | yes |
| UT-20 | `claudeSource_preservesTDDDoDItems` | Existing TDD DoD items (item 4) are preserved | TASK-1 | yes |
| UT-21 | `claudeSource_preservesUpdateREADMEStep` | Phase 7 item 1 "Update README if needed" is preserved | TASK-1 | yes |
| UT-22 | `claudeSource_preservesUpdateImplementationMapStep` | Phase 7 item 2 "Update IMPLEMENTATION-MAP" is preserved | TASK-1 | yes |
| UT-23 | `claudeSource_preservesDoDChecklistStep` | Phase 7 item 3 "Run DoD checklist" is preserved | TASK-1 | yes |
| UT-24 | `claudeSource_preservesAllPlaceholderTokens` | All `{{PLACEHOLDER}}` tokens remain (PROJECT_NAME, LANGUAGE, COMPILE_COMMAND, TEST_COMMAND, COVERAGE_COMMAND, LANGUAGE_VERSION) | TASK-1 | yes |
| UT-25 | `claudeSource_preservesCompleteFlowDiagram` | Complete Flow block (Phase 0-7 listing) is present and unchanged | TASK-1 | yes |
| UT-26 | `claudeSource_preservesRolesAndModelsTable` | Roles and Models table structure is preserved | TASK-1 | yes |
| UT-27 | `claudeSource_preservesIntegrationNotes` | Integration Notes section is preserved | TASK-1 | yes |

---

#### 3.1.7 GitHub Source — Post-Deploy Verification Section Existence (mirrors UT-1 through UT-6)

**Source file under test:** `resources/github-skills-templates/dev/x-dev-lifecycle.md`

| # | Test Name | What It Validates | Depends On | Parallel |
|---|-----------|-------------------|------------|----------|
| UT-28 | `githubSource_phase7_containsPostDeployVerificationSubsection` | Phase 7 contains post-deploy verification sub-section | TASK-2 | yes |
| UT-29 | `githubSource_phase7_postDeployIsConditionalOnSmokeTests` | Conditional on `smoke_tests` being true | TASK-2 | yes |
| UT-30 | `githubSource_phase7_postDeployContainsHealthCheck` | Includes health check verification | TASK-2 | yes |
| UT-31 | `githubSource_phase7_postDeployContainsCriticalPath` | Includes critical path verification | TASK-2 | yes |
| UT-32 | `githubSource_phase7_postDeployContainsResponseTimeSLO` | Includes response time SLO verification | TASK-2 | yes |
| UT-33 | `githubSource_phase7_postDeployContainsErrorRate` | Includes error rate threshold | TASK-2 | yes |

#### 3.1.8 GitHub Source — Post-Deploy Result Semantics (mirrors UT-7 through UT-11)

| # | Test Name | What It Validates | Depends On | Parallel |
|---|-----------|-------------------|------------|----------|
| UT-34 | `githubSource_phase7_postDeployResultPASS` | PASS outcome present | TASK-2 | yes |
| UT-35 | `githubSource_phase7_postDeployResultFAIL` | FAIL outcome present | TASK-2 | yes |
| UT-36 | `githubSource_phase7_postDeployResultSKIP` | SKIP outcome present | TASK-2 | yes |
| UT-37 | `githubSource_phase7_postDeployIsNonBlocking` | Non-blocking behavior documented | TASK-2 | yes |
| UT-38 | `githubSource_phase7_postDeployReferencesRunE2EOrSmokeSkill` | References `/run-e2e` or `/run-smoke-api` | TASK-2 | yes |

#### 3.1.9 GitHub Source — Conditional DoD Item & Renumbering (mirrors UT-12 through UT-15)

| # | Test Name | What It Validates | Depends On | Parallel |
|---|-----------|-------------------|------------|----------|
| UT-39 | `githubSource_phase7_conditionalDoDContainsPostDeployItem` | Conditional DoD items include post-deploy verification | TASK-2 | yes |
| UT-40 | `githubSource_phase7_conditionalDoDPostDeployLinkedToSmokeTests` | New DoD item references `smoke_tests` | TASK-2 | yes |
| UT-41 | `githubSource_phase7_reportPassFailIsRenumbered` | Report PASS/FAIL item is renumbered | TASK-2 | yes |
| UT-42 | `githubSource_phase7_gitCheckoutMainIsRenumbered` | Git checkout main item is renumbered | TASK-2 | yes |

#### 3.1.10 GitHub Source — Backward Compatibility (mirrors UT-16 through UT-27)

| # | Test Name | What It Validates | Depends On | Parallel |
|---|-----------|-------------------|------------|----------|
| UT-43 | `githubSource_preserves8PhaseCount` | "8 phases (0-7)" preserved | TASK-2 | yes |
| UT-44 | `githubSource_preservesCriticalExecutionRule` | "NEVER stop before Phase 7" preserved | TASK-2 | yes |
| UT-45 | `githubSource_preservesPhase7OnlyLegitimateStoppingPoint` | "Phase 7 is the ONLY legitimate stopping point" preserved | TASK-2 | yes |
| UT-46 | `githubSource_preservesExistingConditionalDoDItems` | All existing conditional DoD items preserved | TASK-2 | yes |
| UT-47 | `githubSource_preservesTDDDoDItems` | Existing TDD DoD items preserved | TASK-2 | yes |
| UT-48 | `githubSource_preservesUpdateREADMEStep` | "Update README if needed" preserved | TASK-2 | yes |
| UT-49 | `githubSource_preservesUpdateImplementationMapStep` | "Update IMPLEMENTATION-MAP" preserved | TASK-2 | yes |
| UT-50 | `githubSource_preservesDoDChecklistStep` | "Run DoD checklist" preserved | TASK-2 | yes |
| UT-51 | `githubSource_preservesAllPlaceholderTokens` | All `{{PLACEHOLDER}}` tokens preserved | TASK-2 | yes |
| UT-52 | `githubSource_preservesCompleteFlowDiagram` | Complete Flow block preserved | TASK-2 | yes |
| UT-53 | `githubSource_preservesRolesAndModelsTable` | Roles and Models table preserved | TASK-2 | yes |
| UT-54 | `githubSource_preservesIntegrationNotes` | Integration Notes preserved | TASK-2 | yes |

---

### 3.2 Dual Copy Consistency (RULE-001)

| # | Test Name | What It Validates | Depends On | Parallel |
|---|-----------|-------------------|------------|----------|
| UT-55 | `dualCopy_bothContainPostDeployVerificationSection` | Both sources contain post-deploy verification sub-section in Phase 7 | TASK-1, TASK-2 | no |
| UT-56 | `dualCopy_bothContainHealthCheckVerification` | Both sources include health check verification | TASK-1, TASK-2 | no |
| UT-57 | `dualCopy_bothContainCriticalPathVerification` | Both sources include critical path verification | TASK-1, TASK-2 | no |
| UT-58 | `dualCopy_bothContainResponseTimeSLO` | Both sources include response time SLO check | TASK-1, TASK-2 | no |
| UT-59 | `dualCopy_bothContainErrorRateThreshold` | Both sources include error rate threshold | TASK-1, TASK-2 | no |
| UT-60 | `dualCopy_bothContainPASSFAILSKIPResults` | Both sources document PASS, FAIL, SKIP outcomes | TASK-1, TASK-2 | no |
| UT-61 | `dualCopy_bothContainNonBlockingBehavior` | Both sources document non-blocking behavior | TASK-1, TASK-2 | no |
| UT-62 | `dualCopy_bothContainConditionalDoDPostDeployItem` | Both sources have post-deploy conditional DoD item | TASK-1, TASK-2 | no |
| UT-63 | `dualCopy_bothContainSmokeTestsCondition` | Both sources condition on `smoke_tests` | TASK-1, TASK-2 | no |
| UT-64 | `dualCopy_bothContainRunE2EOrSmokeReference` | Both sources reference `/run-e2e` or `/run-smoke-api` | TASK-1, TASK-2 | no |
| UT-65 | `dualCopy_phaseCount_identical` | Both sources declare "8 phases (0-7)" | TASK-1, TASK-2 | no |
| UT-66 | `dualCopy_pathDifferences_onlyExpected` | The only differences between the two templates are expected path references (`skills/` vs `.github/skills/`), frontmatter format, Global Output Policy, and Detailed References section | TASK-1, TASK-2 | no |

---

## 4. Integration Tests (IT) — Golden File Parity

### IT-1: Pipeline output matches golden files for all 8 profiles

**File:** `tests/node/integration/byte-for-byte.test.ts` (existing, no changes)

**What it validates:** Pipeline output matches golden files byte-for-byte for all 8 profiles.

**How it covers this story:** After updating both source templates and regenerating 24 golden files, the pipeline will produce output identical to the updated golden files. The x-dev-lifecycle SKILL.md in each golden file will now contain the post-deploy verification sub-section.

**Profiles covered:**

| Profile | `.claude/` golden | `.agents/` golden | `.github/` golden |
|---------|-------------------|-------------------|-------------------|
| go-gin | `tests/golden/go-gin/.claude/skills/x-dev-lifecycle/SKILL.md` | `tests/golden/go-gin/.agents/skills/x-dev-lifecycle/SKILL.md` | `tests/golden/go-gin/.github/skills/x-dev-lifecycle/SKILL.md` |
| java-quarkus | `tests/golden/java-quarkus/.claude/skills/x-dev-lifecycle/SKILL.md` | `tests/golden/java-quarkus/.agents/skills/x-dev-lifecycle/SKILL.md` | `tests/golden/java-quarkus/.github/skills/x-dev-lifecycle/SKILL.md` |
| java-spring | `tests/golden/java-spring/.claude/skills/x-dev-lifecycle/SKILL.md` | `tests/golden/java-spring/.agents/skills/x-dev-lifecycle/SKILL.md` | `tests/golden/java-spring/.github/skills/x-dev-lifecycle/SKILL.md` |
| kotlin-ktor | `tests/golden/kotlin-ktor/.claude/skills/x-dev-lifecycle/SKILL.md` | `tests/golden/kotlin-ktor/.agents/skills/x-dev-lifecycle/SKILL.md` | `tests/golden/kotlin-ktor/.github/skills/x-dev-lifecycle/SKILL.md` |
| python-click-cli | `tests/golden/python-click-cli/.claude/skills/x-dev-lifecycle/SKILL.md` | `tests/golden/python-click-cli/.agents/skills/x-dev-lifecycle/SKILL.md` | `tests/golden/python-click-cli/.github/skills/x-dev-lifecycle/SKILL.md` |
| python-fastapi | `tests/golden/python-fastapi/.claude/skills/x-dev-lifecycle/SKILL.md` | `tests/golden/python-fastapi/.agents/skills/x-dev-lifecycle/SKILL.md` | `tests/golden/python-fastapi/.github/skills/x-dev-lifecycle/SKILL.md` |
| rust-axum | `tests/golden/rust-axum/.claude/skills/x-dev-lifecycle/SKILL.md` | `tests/golden/rust-axum/.agents/skills/x-dev-lifecycle/SKILL.md` | `tests/golden/rust-axum/.github/skills/x-dev-lifecycle/SKILL.md` |
| typescript-nestjs | `tests/golden/typescript-nestjs/.claude/skills/x-dev-lifecycle/SKILL.md` | `tests/golden/typescript-nestjs/.agents/skills/x-dev-lifecycle/SKILL.md` | `tests/golden/typescript-nestjs/.github/skills/x-dev-lifecycle/SKILL.md` |

- **Expected result:** All 8 profiles pass (40 test assertions: 5 per profile)
- **Depends On:** TASK-1, TASK-2, TASK-3
- **Parallel:** no (depends on all implementation and golden file update tasks)

---

## 5. Content Verification — Key Sections That Must Appear

The following sections and keywords MUST be present in the updated templates. Content tests use `toContain()` or `toMatch()` for substring/regex matching (not brittle exact line matching).

### 5.1 Post-Deploy Verification Sub-Section

| Keyword/Pattern | Purpose |
|-----------------|---------|
| `Post-Deploy Verification` | Section heading/label |
| `smoke_tests` | Conditional trigger |
| `Health Check` | Health check verification type |
| `/health` or `health endpoint` | Health endpoint reference |
| `200 OK` or `200` | Expected health check response |
| `Critical Path` | Critical path verification type |
| `Response Time` or `p95` | Response time SLO verification |
| `SLO` | SLO reference |
| `Error Rate` or `error rate` | Error rate threshold verification |
| `1%` | Default error rate threshold |

### 5.2 Result Outcomes

| Keyword/Pattern | Purpose |
|-----------------|---------|
| `PASS` | All checks green outcome |
| `Deploy confirmed` or `deploy confirmed` | PASS recommendation text |
| `FAIL` | Any check failed outcome |
| `rollback` or `Investigate` | FAIL recommendation text |
| `SKIP` | smoke_tests=false outcome |
| `skipped` or `verification skipped` | SKIP description |

### 5.3 Non-Blocking Behavior

| Keyword/Pattern | Purpose |
|-----------------|---------|
| `non-blocking` or `Non-blocking` or `NOT auto-rollback` or `human decision` | Non-blocking documented |

### 5.4 Skill Invocation

| Keyword/Pattern | Purpose |
|-----------------|---------|
| `/run-e2e` or `/run-smoke-api` | Automation reference |

### 5.5 Conditional DoD Item

| Keyword/Pattern | Purpose |
|-----------------|---------|
| `Post-deploy verification` in the conditional DoD items block | New DoD item |
| `smoke_tests` near the DoD item | Condition reference |

### 5.6 Preserved Existing Content (Backward Compatibility)

| Keyword/Pattern | Purpose |
|-----------------|---------|
| `8 phases (0-7)` | Phase count preserved (RULE-003) |
| `NEVER stop before Phase 7` | Critical execution rule preserved |
| `Phase 7 is the ONLY legitimate stopping point` | Stopping point rule preserved |
| `Contract tests pass` | Existing conditional DoD preserved |
| `Event schemas registered` | Existing conditional DoD preserved |
| `Compliance requirements met` | Existing conditional DoD preserved |
| `Gateway configuration updated` | Existing conditional DoD preserved |
| `gRPC proto backward compatible` | Existing conditional DoD preserved |
| `GraphQL schema backward compatible` | Existing conditional DoD preserved |
| `test-first pattern` | Existing TDD DoD preserved |
| `acceptance tests exist and pass` or `AT-N GREEN` | Existing TDD DoD preserved |
| `TPP ordering` or `simple to complex` | Existing TDD DoD preserved |
| `{{PROJECT_NAME}}` | Placeholder token preserved |
| `{{LANGUAGE}}` | Placeholder token preserved |
| `{{LANGUAGE_VERSION}}` | Placeholder token preserved |
| `{{COMPILE_COMMAND}}` | Placeholder token preserved |
| `{{TEST_COMMAND}}` | Placeholder token preserved |
| `{{COVERAGE_COMMAND}}` | Placeholder token preserved |

---

## 6. Backward Compatibility Verification

These tests ensure no existing functionality is removed (RULE-003):

### 6.1 Phase Structure Preservation

| Verification | How Tested |
|--------------|-----------|
| 8 phases (0-7) count preserved | UT-16 / UT-43: regex match `8 phases.*0-7` |
| "NEVER stop before Phase 7" preserved | UT-17 / UT-44: exact substring match |
| "Phase 7 is the ONLY legitimate stopping point" preserved | UT-18 / UT-45: exact substring match |
| Complete Flow diagram present | UT-25 / UT-52: all phase entries in code block |
| Roles and Models table intact | UT-26 / UT-53: table with Architect, Developer, Tech Lead rows |

### 6.2 Existing Phase 7 Items Preserved

| Item | How Tested |
|------|-----------|
| Update README if needed | UT-21 / UT-48 |
| Update IMPLEMENTATION-MAP | UT-22 / UT-49 |
| Run DoD checklist | UT-23 / UT-50 |
| TDD DoD items (test-first, AT-N GREEN, TPP) | UT-20 / UT-47 |
| All 6 existing conditional DoD items | UT-19 / UT-46 |

### 6.3 Placeholder Tokens

| Token | How Tested |
|-------|-----------|
| `{{PROJECT_NAME}}` | UT-24 / UT-51 |
| `{{LANGUAGE}}` | UT-24 / UT-51 |
| `{{LANGUAGE_VERSION}}` | UT-24 / UT-51 |
| `{{COMPILE_COMMAND}}` | UT-24 / UT-51 |
| `{{TEST_COMMAND}}` | UT-24 / UT-51 |
| `{{COVERAGE_COMMAND}}` | UT-24 / UT-51 |

---

## 7. Golden Files Requiring Update

**Total: 24 golden files** (8 profiles x 3 output directories)

### 7.1 `.claude/` golden files (8 files, identical to Claude source)

| Profile | Path |
|---------|------|
| go-gin | `tests/golden/go-gin/.claude/skills/x-dev-lifecycle/SKILL.md` |
| java-quarkus | `tests/golden/java-quarkus/.claude/skills/x-dev-lifecycle/SKILL.md` |
| java-spring | `tests/golden/java-spring/.claude/skills/x-dev-lifecycle/SKILL.md` |
| kotlin-ktor | `tests/golden/kotlin-ktor/.claude/skills/x-dev-lifecycle/SKILL.md` |
| python-click-cli | `tests/golden/python-click-cli/.claude/skills/x-dev-lifecycle/SKILL.md` |
| python-fastapi | `tests/golden/python-fastapi/.claude/skills/x-dev-lifecycle/SKILL.md` |
| rust-axum | `tests/golden/rust-axum/.claude/skills/x-dev-lifecycle/SKILL.md` |
| typescript-nestjs | `tests/golden/typescript-nestjs/.claude/skills/x-dev-lifecycle/SKILL.md` |

### 7.2 `.agents/` golden files (8 files, identical to Claude source)

| Profile | Path |
|---------|------|
| go-gin | `tests/golden/go-gin/.agents/skills/x-dev-lifecycle/SKILL.md` |
| java-quarkus | `tests/golden/java-quarkus/.agents/skills/x-dev-lifecycle/SKILL.md` |
| java-spring | `tests/golden/java-spring/.agents/skills/x-dev-lifecycle/SKILL.md` |
| kotlin-ktor | `tests/golden/kotlin-ktor/.agents/skills/x-dev-lifecycle/SKILL.md` |
| python-click-cli | `tests/golden/python-click-cli/.agents/skills/x-dev-lifecycle/SKILL.md` |
| python-fastapi | `tests/golden/python-fastapi/.agents/skills/x-dev-lifecycle/SKILL.md` |
| rust-axum | `tests/golden/rust-axum/.agents/skills/x-dev-lifecycle/SKILL.md` |
| typescript-nestjs | `tests/golden/typescript-nestjs/.agents/skills/x-dev-lifecycle/SKILL.md` |

### 7.3 `.github/` golden files (8 files, identical to GitHub source)

| Profile | Path |
|---------|------|
| go-gin | `tests/golden/go-gin/.github/skills/x-dev-lifecycle/SKILL.md` |
| java-quarkus | `tests/golden/java-quarkus/.github/skills/x-dev-lifecycle/SKILL.md` |
| java-spring | `tests/golden/java-spring/.github/skills/x-dev-lifecycle/SKILL.md` |
| kotlin-ktor | `tests/golden/kotlin-ktor/.github/skills/x-dev-lifecycle/SKILL.md` |
| python-click-cli | `tests/golden/python-click-cli/.github/skills/x-dev-lifecycle/SKILL.md` |
| python-fastapi | `tests/golden/python-fastapi/.github/skills/x-dev-lifecycle/SKILL.md` |
| rust-axum | `tests/golden/rust-axum/.github/skills/x-dev-lifecycle/SKILL.md` |
| typescript-nestjs | `tests/golden/typescript-nestjs/.github/skills/x-dev-lifecycle/SKILL.md` |

### 7.4 Golden File Update Strategy

```bash
# After editing the source templates:
CLAUDE_SRC="resources/skills-templates/core/x-dev-lifecycle/SKILL.md"
GITHUB_SRC="resources/github-skills-templates/dev/x-dev-lifecycle.md"
PROFILES=(go-gin java-quarkus java-spring kotlin-ktor python-click-cli python-fastapi rust-axum typescript-nestjs)

for profile in "${PROFILES[@]}"; do
  cp "$CLAUDE_SRC" "tests/golden/$profile/.claude/skills/x-dev-lifecycle/SKILL.md"
  cp "$CLAUDE_SRC" "tests/golden/$profile/.agents/skills/x-dev-lifecycle/SKILL.md"
  cp "$GITHUB_SRC" "tests/golden/$profile/.github/skills/x-dev-lifecycle/SKILL.md"
done
```

---

## 8. Suggested Test Implementation Pattern

```typescript
import { describe, it, expect } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";

const CLAUDE_SOURCE = path.resolve(
  __dirname, "../../..",
  "resources/skills-templates/core/x-dev-lifecycle/SKILL.md",
);
const GITHUB_SOURCE = path.resolve(
  __dirname, "../../..",
  "resources/github-skills-templates/dev/x-dev-lifecycle.md",
);

const claudeContent = fs.readFileSync(CLAUDE_SOURCE, "utf-8");
const githubContent = fs.readFileSync(GITHUB_SOURCE, "utf-8");

// ---------------------------------------------------------------------------
// Claude Source — Post-Deploy Verification Section
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle Claude source — post-deploy verification existence", () => {
  // UT-1 through UT-6
});

describe("x-dev-lifecycle Claude source — post-deploy result semantics", () => {
  // UT-7 through UT-10
});

describe("x-dev-lifecycle Claude source — skill invocation references", () => {
  // UT-11
});

describe("x-dev-lifecycle Claude source — conditional DoD item", () => {
  // UT-12 through UT-13
});

describe("x-dev-lifecycle Claude source — item renumbering", () => {
  // UT-14 through UT-15
});

describe("x-dev-lifecycle Claude source — backward compatibility", () => {
  // UT-16 through UT-27
});

// ---------------------------------------------------------------------------
// GitHub Source — Post-Deploy Verification Section
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle GitHub source — post-deploy verification existence", () => {
  // UT-28 through UT-33
});

describe("x-dev-lifecycle GitHub source — post-deploy result semantics", () => {
  // UT-34 through UT-38
});

describe("x-dev-lifecycle GitHub source — conditional DoD & renumbering", () => {
  // UT-39 through UT-42
});

describe("x-dev-lifecycle GitHub source — backward compatibility", () => {
  // UT-43 through UT-54
});

// ---------------------------------------------------------------------------
// Dual Copy Consistency (RULE-001)
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle dual copy consistency (RULE-001)", () => {
  // UT-55 through UT-66
});
```

---

## 9. TDD Execution Order

Following test-first approach:

| Step | Action | Test State |
|------|--------|-----------|
| 1 | Write content validation tests (`tests/node/content/x-dev-lifecycle-postdeploy-content.test.ts`) with all 66 test cases | RED (tests fail because source files not yet modified) |
| 2 | Edit Claude source template (`resources/skills-templates/core/x-dev-lifecycle/SKILL.md`) — add post-deploy verification sub-section and conditional DoD item in Phase 7, renumber items 6-7 to 7-8 | Partial GREEN (Claude tests pass UT-1 through UT-27, GitHub tests UT-28-54 still RED, dual copy tests partial) |
| 3 | Edit GitHub source template (`resources/github-skills-templates/dev/x-dev-lifecycle.md`) — mirror the same changes with `.github/skills/` path references | GREEN (all content + consistency tests pass: UT-1 through UT-66) |
| 4 | Update deployed copy (`.claude/skills/x-dev-lifecycle/SKILL.md`) to match Claude source | N/A (deployed copy updated) |
| 5 | Copy sources to 24 golden files (script from Section 7.4) | N/A (golden files updated) |
| 6 | Run byte-for-byte integration tests | GREEN (AT-1: golden file parity confirmed) |
| 7 | Run full test suite (`npx vitest run`) | GREEN (all existing tests pass, plus 66 new tests) |

---

## 10. Existing Tests — No Changes Needed

### 10.1 Golden File Integration Tests

- **File:** `tests/node/integration/byte-for-byte.test.ts`
- **What it validates:** Pipeline output matches golden files byte-for-byte for all 8 profiles
- **How it covers this story:** After updating both source templates and regenerating 24 golden files, the pipeline will produce output identical to the updated golden files
- **Expected result:** All 8 profiles pass (40 test assertions: 5 per profile)
- **Test logic unchanged:** The test infrastructure is generic and works with any content

### 10.2 Assembler Unit Tests

- **File:** `tests/node/assembler/skills-assembler.test.ts` -- Tests `SkillsAssembler` copy logic for `.claude/` output
- **File:** `tests/node/assembler/codex-skills-assembler.test.ts` -- Tests `CodexSkillsAssembler` mirror logic for `.agents/` output
- **File:** `tests/node/assembler/github-skills-assembler.test.ts` -- Tests `GithubSkillsAssembler` copy logic for `.github/` output
- **Impact:** None -- assembler logic unchanged; these tests continue to validate the copy mechanism works

---

## 11. Verification Checklist

- [ ] `npx vitest run tests/node/content/x-dev-lifecycle-postdeploy-content.test.ts` -- all 66 content validation tests pass
- [ ] `npx vitest run tests/node/integration/byte-for-byte.test.ts` -- all 8 profiles pass (40 assertions)
- [ ] `npx vitest run` -- full suite passes
- [ ] Coverage remains >= 95% line, >= 90% branch (no TypeScript code changes, so coverage unaffected)
- [ ] No compiler/linter warnings introduced
- [ ] Deployed copy (`.claude/skills/x-dev-lifecycle/SKILL.md`) matches Claude source template exactly
- [ ] Both source templates modified in same commit (RULE-001 compliance)
- [ ] All 24 golden files updated

---

## 12. Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| Golden file mismatch after source edit | Mechanical copy script (Section 7.4) eliminates drift; byte-for-byte tests catch any mismatch immediately |
| Content test too brittle (exact string matching) | Use `toContain()` for substring checks and `toMatch()` for regex patterns; test semantic presence, not formatting |
| Dual copy inconsistency (RULE-001) | Dedicated consistency tests (UT-55 through UT-66) verify both copies have equivalent post-deploy content |
| Post-deploy section breaks existing Phase 7 items | Backward compatibility tests (UT-16 through UT-27 for Claude, UT-43 through UT-54 for GitHub) verify all existing items preserved |
| Renumbering error breaks item references | UT-14/UT-15 and UT-41/UT-42 explicitly verify renumbered items appear in the correct position |
| Deployed copy diverges from source | Verification checklist item: deployed copy must match Claude source template exactly |
| Story dependency on story-0004-0013 not met | The implementation plan confirms story-0004-0013 modifies Phase 1, not Phase 7. No structural conflict. Post-deploy verification targets Phase 7 exclusively. |

---

## 13. Files Summary

### 13.1 Files Modified (Source Templates + Deployed Copy)

| # | File | Description |
|---|------|-------------|
| 1 | `resources/skills-templates/core/x-dev-lifecycle/SKILL.md` | Claude Code source of truth (RULE-002) |
| 2 | `resources/github-skills-templates/dev/x-dev-lifecycle.md` | GitHub Copilot source of truth (RULE-002) |
| 3 | `.claude/skills/x-dev-lifecycle/SKILL.md` | Deployed copy for this project |

### 13.2 Golden Files Updated (24 files)

8 profiles x 3 output directories (`.claude/`, `.agents/`, `.github/`) = 24 golden files.

### 13.3 New Test File (1 file)

| File | Test Count |
|------|-----------|
| `tests/node/content/x-dev-lifecycle-postdeploy-content.test.ts` | 66 |

### 13.4 Existing Test Files (unchanged, covering this story)

| File | Test Count | Coverage |
|------|-----------|----------|
| `tests/node/integration/byte-for-byte.test.ts` | 40 (8 profiles x 5 assertions) | Golden file parity |
| `tests/node/assembler/skills-assembler.test.ts` | ~20 | Claude copy mechanism |
| `tests/node/assembler/codex-skills-assembler.test.ts` | ~15 | Agents copy mechanism |
| `tests/node/assembler/github-skills-assembler.test.ts` | ~15 | GitHub copy mechanism |

---

## 14. Test Count Summary

| Category | New Tests | Existing Tests |
|----------|-----------|----------------|
| Content validation (Claude source) | 27 (UT-1 through UT-27) | 0 |
| Content validation (GitHub source) | 27 (UT-28 through UT-54) | 0 |
| Dual copy consistency | 12 (UT-55 through UT-66) | 0 |
| Golden file integration (AT-1) | 0 | 40 (8 profiles x 5 assertions) |
| Assembler unit tests | 0 | ~50 (across 3 assembler test files) |
| **Total** | **66** | **~90** |
