# Test Plan -- story-0005-0006: Integrity Gate Between Phases

**Story:** `story-0005-0006.md`
**Framework:** Vitest (pool: forks, maxForks: 3, maxConcurrency: 5)
**Coverage Targets:** >= 95% line, >= 90% branch
**Test Naming:** `[functionUnderTest]_[scenario]_[expectedBehavior]`

---

## Test File Structure

```
tests/
  node/
    checkpoint/
      validation.test.ts          # UT-1 through UT-3, UT-7 (additions to existing file)
      engine.test.ts              # UT-4 through UT-6 (additions to existing file)
      acceptance.test.ts          # IT-1 (addition to existing file)
    integration/
      byte-for-byte.test.ts       # IT-2 (covered by existing golden file tests)
    content/
      x-dev-epic-implement-content.test.ts  # CT-1 through CT-6 (additions to existing file)
```

---

## 1. Unit Tests (UT-N) -- Inner Loop (TPP Order)

### 1.1 Validation: `branchCoverage` field on `IntegrityGateEntry`

**Test File:** `tests/node/checkpoint/validation.test.ts` (existing `validateIntegrityGateEntry` describe block)

| ID | Description | Input/Setup | Expected Result | TPP Level | Depends On | Parallel |
|:---|:---|:---|:---|:---|:---|:---|
| UT-1a | `validateIntegrityGateEntry_branchCoverageUndefined_doesNotThrow` | `aValidIntegrityGate()` (no `branchCoverage` field) | Does not throw -- field is optional | 1 ({} -> nil) | none | yes |
| UT-1b | `validateIntegrityGateEntry_branchCoverageValidNumber_doesNotThrow` | `aValidIntegrityGate({ branchCoverage: 92.5 })` | Does not throw | 2 (nil -> constant) | none | yes |
| UT-1c | `validateIntegrityGateEntry_branchCoverageZero_doesNotThrow` | `aValidIntegrityGate({ branchCoverage: 0 })` | Does not throw -- zero is a valid number | 3 (constant -> constant+) | none | yes |
| UT-1d | `validateIntegrityGateEntry_branchCoverageString_throwsValidationError` | `aValidIntegrityGate({ branchCoverage: "90" })` | Throws `CheckpointValidationError` with message containing `branchCoverage` | 4 (unconditional -> conditional) | none | yes |
| UT-1e | `validateIntegrityGateEntry_branchCoverageBoolean_throwsValidationError` | `aValidIntegrityGate({ branchCoverage: true })` | Throws `CheckpointValidationError` | 4 (unconditional -> conditional) | none | yes |

### 1.2 Validation: `regressionSource` field on `IntegrityGateEntry`

**Test File:** `tests/node/checkpoint/validation.test.ts` (existing `validateIntegrityGateEntry` describe block)

| ID | Description | Input/Setup | Expected Result | TPP Level | Depends On | Parallel |
|:---|:---|:---|:---|:---|:---|:---|
| UT-2a | `validateIntegrityGateEntry_regressionSourceUndefined_doesNotThrow` | `aValidIntegrityGate()` (no `regressionSource` field) | Does not throw -- field is optional | 1 ({} -> nil) | none | yes |
| UT-2b | `validateIntegrityGateEntry_regressionSourceValidString_doesNotThrow` | `aValidIntegrityGate({ regressionSource: "0042-0005" })` | Does not throw | 2 (nil -> constant) | none | yes |
| UT-2c | `validateIntegrityGateEntry_regressionSourceEmptyString_doesNotThrow` | `aValidIntegrityGate({ regressionSource: "" })` | Does not throw -- empty string is a valid string | 3 (constant -> constant+) | none | yes |
| UT-2d | `validateIntegrityGateEntry_regressionSourceNumber_throwsValidationError` | `aValidIntegrityGate({ regressionSource: 42 })` | Throws `CheckpointValidationError` with message containing `regressionSource` | 4 (unconditional -> conditional) | none | yes |
| UT-2e | `validateIntegrityGateEntry_regressionSourceArray_throwsValidationError` | `aValidIntegrityGate({ regressionSource: ["0042-0005"] })` | Throws `CheckpointValidationError` | 4 (unconditional -> conditional) | none | yes |

### 1.3 Validation: Both new fields together

**Test File:** `tests/node/checkpoint/validation.test.ts` (existing `validateIntegrityGateEntry` describe block)

| ID | Description | Input/Setup | Expected Result | TPP Level | Depends On | Parallel |
|:---|:---|:---|:---|:---|:---|:---|
| UT-3a | `validateIntegrityGateEntry_bothNewFieldsPresent_doesNotThrow` | `aValidIntegrityGate({ branchCoverage: 91.2, regressionSource: "0042-0005" })` | Does not throw | 5 (collection -> collection) | UT-1, UT-2 | yes |
| UT-3b | `validateIntegrityGateEntry_failWithAllOptionalFields_doesNotThrow` | `aValidIntegrityGate({ status: "FAIL", failedTests: ["test-a"], branchCoverage: 88.0, regressionSource: "0042-0003" })` | Does not throw -- all optional fields coexist with existing ones | 5 (collection -> collection) | UT-1, UT-2 | yes |

### 1.4 Engine: `updateIntegrityGate` stores `branchCoverage`

**Test File:** `tests/node/checkpoint/engine.test.ts` (existing `updateIntegrityGate` describe block)

| ID | Description | Input/Setup | Expected Result | TPP Level | Depends On | Parallel |
|:---|:---|:---|:---|:---|:---|:---|
| UT-4a | `updateIntegrityGate_passWithBranchCoverage_storesBranchCoverage` | Write valid state, call `updateIntegrityGate(tmpDir, 0, { status: "PASS", testCount: 42, coverage: 96.3, branchCoverage: 91.5 })` | `state.integrityGates["phase-0"]?.branchCoverage === 91.5` | 3 (constant -> constant+) | UT-1 | yes |
| UT-4b | `updateIntegrityGate_withoutBranchCoverage_fieldIsUndefined` | Write valid state, call `updateIntegrityGate(tmpDir, 0, { status: "PASS", testCount: 42, coverage: 96.3 })` | `state.integrityGates["phase-0"]?.branchCoverage === undefined` | 2 (nil -> constant) | UT-1 | yes |

### 1.5 Engine: `updateIntegrityGate` stores `regressionSource`

**Test File:** `tests/node/checkpoint/engine.test.ts` (existing `updateIntegrityGate` describe block)

| ID | Description | Input/Setup | Expected Result | TPP Level | Depends On | Parallel |
|:---|:---|:---|:---|:---|:---|:---|
| UT-5a | `updateIntegrityGate_failWithRegressionSource_storesRegressionSource` | Write valid state, call `updateIntegrityGate(tmpDir, 1, { status: "FAIL", testCount: 100, coverage: 88, failedTests: ["test-a"], regressionSource: "0042-0005" })` | `state.integrityGates["phase-1"]?.regressionSource === "0042-0005"` | 3 (constant -> constant+) | UT-2 | yes |
| UT-5b | `updateIntegrityGate_passWithoutRegressionSource_fieldIsUndefined` | Write valid state, call `updateIntegrityGate(tmpDir, 0, { status: "PASS", testCount: 42, coverage: 96.3 })` | `state.integrityGates["phase-0"]?.regressionSource === undefined` | 2 (nil -> constant) | UT-2 | yes |

### 1.6 Engine: `updateIntegrityGate` round-trip with FAIL + `regressionSource`

**Test File:** `tests/node/checkpoint/engine.test.ts` (existing `updateIntegrityGate` describe block)

| ID | Description | Input/Setup | Expected Result | TPP Level | Depends On | Parallel |
|:---|:---|:---|:---|:---|:---|:---|
| UT-6 | `updateIntegrityGate_failWithRegressionSource_roundTripsCorrectly` | Write valid state, call `updateIntegrityGate(tmpDir, 1, { status: "FAIL", testCount: 100, coverage: 88, branchCoverage: 85.5, failedTests: ["test-a", "test-b"], regressionSource: "0042-0005" })`, then `readCheckpoint(tmpDir)` | Read state matches: `status === "FAIL"`, `testCount === 100`, `coverage === 88`, `branchCoverage === 85.5`, `failedTests` deep equals `["test-a", "test-b"]`, `regressionSource === "0042-0005"`, `timestamp` is a valid ISO-8601 string | 5 (collection -> collection) | UT-4, UT-5 | yes |

### 1.7 Template JSON: new optional fields validate

**Test File:** `tests/node/checkpoint/acceptance.test.ts` (existing `template validation` describe block)

| ID | Description | Input/Setup | Expected Result | TPP Level | Depends On | Parallel |
|:---|:---|:---|:---|:---|:---|:---|
| UT-7a | `template_integrityGateEntry_supportsOptionalBranchCoverage` | Read `_TEMPLATE-EXECUTION-STATE.json`, check `integrityGates.phase-0` | If `branchCoverage` is present, it is a number. If absent, template still validates. | 3 (constant -> constant+) | none | yes |
| UT-7b | `template_integrityGateEntry_supportsOptionalRegressionSource` | Read `_TEMPLATE-EXECUTION-STATE.json`, check `integrityGates.phase-0` | If `regressionSource` is present, it is a string. If absent, template still validates. | 3 (constant -> constant+) | none | yes |
| UT-7c | `template_withNewOptionalFields_passesValidation` | Read `_TEMPLATE-EXECUTION-STATE.json`, inject `branchCoverage: 90` and `regressionSource: "0042-0001"` into `phase-0` gate, run `validateExecutionState()` | Does not throw | 5 (collection -> collection) | UT-1, UT-2 | yes |

---

## 2. Integration Tests (IT-N)

### IT-1: Round-trip with new fields

**Test File:** `tests/node/checkpoint/acceptance.test.ts` (existing `integration round-trip` describe block)

| ID | Description | Input/Setup | Expected Result | Depends On | Parallel |
|:---|:---|:---|:---|:---|:---|
| IT-1 | `checkpointRoundTrip_createThenUpdateGateWithNewFields_readPreservesAll` | 1. `createCheckpoint(tmpDir, { epicId: "0042", stories: [{ id: "0042-0001", phase: 1 }] })` 2. `updateIntegrityGate(tmpDir, 0, { status: "FAIL", testCount: 100, coverage: 93.2, branchCoverage: 85.5, failedTests: ["test-a"], regressionSource: "0042-0003" })` 3. `readCheckpoint(tmpDir)` | Read state has `integrityGates["phase-0"]` with all fields preserved: `status === "FAIL"`, `testCount === 100`, `coverage === 93.2`, `branchCoverage === 85.5`, `failedTests` deep equals `["test-a"]`, `regressionSource === "0042-0003"`, `timestamp` is an ISO-8601 string. Stories map is unchanged. | all UTs | yes |

### IT-2: Golden files for all 8 profiles contain integrity gate section

**Test File:** `tests/node/integration/byte-for-byte.test.ts` (existing golden file validation)

| ID | Description | Input/Setup | Expected Result | Depends On | Parallel |
|:---|:---|:---|:---|:---|:---|
| IT-2 | `goldenFiles_allProfiles_skillMdContainsIntegrityGateSection` | For each of the 8 profiles (`go-gin`, `java-quarkus`, `java-spring`, `kotlin-ktor`, `python-click-cli`, `python-fastapi`, `rust-axum`, `typescript-nestjs`), read `tests/golden/{profile}/.claude/skills/x-dev-epic-implement/SKILL.md` | Each golden file SKILL.md contains the string `Integrity Gate` (case-sensitive). The byte-for-byte test already covers full file parity, so this is verified implicitly once golden files are regenerated with the new section. No new test code is needed -- updating golden files is sufficient for the existing `byte-for-byte.test.ts` to pass. | all UTs, template update | sequential |

> **Note on IT-2:** The existing `byte-for-byte.test.ts` runs `runPipeline` for each profile and compares output byte-for-byte against golden files. Once the SKILL.md template is updated with the integrity gate section and golden files are regenerated, this test inherently validates that all 8 profiles include the new section. A supplementary content assertion can be added to `x-dev-epic-implement-content.test.ts` (see CT-1) for explicit verification.

---

## 3. Content Tests (CT-N)

These tests validate that the rendered SKILL.md templates contain the integrity gate section with the correct technical content. They follow the pattern established in `tests/node/content/x-dev-epic-implement-content.test.ts`.

**Test File:** `tests/node/content/x-dev-epic-implement-content.test.ts` (additions to existing file)

| ID | Description | Input/Setup | Expected Result | Depends On | Parallel |
|:---|:---|:---|:---|:---|:---|
| CT-1 | `skillMd_containsSection_IntegrityGate` | Read `resources/skills-templates/core/x-dev-epic-implement/SKILL.md` | Content contains `Integrity Gate` as a section heading (matches `/## .*Integrity Gate/` or contains the string `Integrity Gate`) | none | yes |
| CT-2 | `skillMd_integrityGate_referencesCompileTestCoverageCommands` | Read the SKILL.md content | Content contains references to compile, test, and coverage commands: matches `{{COMPILE_COMMAND}}` or `tsc --noEmit` or `compile`; matches `{{TEST_COMMAND}}` or `vitest` or `test`; matches `{{COVERAGE_COMMAND}}` or `coverage` | CT-1 | yes |
| CT-3 | `skillMd_integrityGate_referencesRegressionDiagnosis` | Read the SKILL.md content | Content contains reference to regression diagnosis: matches `/regression/i` and one of `/diagnos/i`, `/correlat/i`, or `/identif/i` | CT-1 | yes |
| CT-4 | `skillMd_integrityGate_referencesGitRevert` | Read the SKILL.md content | Content contains reference to `git revert` (case-insensitive match for `/git revert/i`) | CT-1 | yes |
| CT-5 | `skillMd_integrityGate_referencesUpdateIntegrityGate` | Read the SKILL.md content | Content contains reference to `updateIntegrityGate` (exact match for the checkpoint engine function name) | CT-1 | yes |
| CT-6 | `githubSkillMd_containsIntegrityGateSection_dualCopyConsistency` | Read `resources/github-skills-templates/dev/x-dev-epic-implement.md` | GitHub template also contains `Integrity Gate` section. Both Claude and GitHub copies are consistent per RULE-001. | CT-1 | yes |

---

## 4. TPP Order Summary

The tests follow Transformation Priority Premise ordering within each validation group, progressing from degenerate cases to complex scenarios:

### validation.ts -- `branchCoverage` (UT-1)

```
Level 1: UT-1a  -- undefined (degenerate/absent) -> does not throw
Level 2: UT-1b  -- valid number -> does not throw
Level 3: UT-1c  -- boundary (zero) -> does not throw
Level 4: UT-1d, UT-1e  -- wrong types -> throws
```

### validation.ts -- `regressionSource` (UT-2)

```
Level 1: UT-2a  -- undefined (degenerate/absent) -> does not throw
Level 2: UT-2b  -- valid string -> does not throw
Level 3: UT-2c  -- boundary (empty string) -> does not throw
Level 4: UT-2d, UT-2e  -- wrong types -> throws
```

### validation.ts -- both fields (UT-3)

```
Level 5: UT-3a, UT-3b  -- both fields present, coexist with existing optional fields
```

### engine.ts -- `updateIntegrityGate` (UT-4 through UT-6)

```
Level 2: UT-4b, UT-5b  -- without new fields -> undefined
Level 3: UT-4a, UT-5a  -- with new fields -> stored
Level 5: UT-6           -- full round-trip with all fields
```

### acceptance.test.ts -- template (UT-7)

```
Level 3: UT-7a, UT-7b  -- individual optional fields
Level 5: UT-7c          -- both fields injected, passes validation
```

---

## 5. Implementation Changes Required

### 5.1 Type Changes (`src/checkpoint/types.ts`)

Add to `IntegrityGateEntry` interface:

```typescript
readonly branchCoverage?: number | undefined;
readonly regressionSource?: string | undefined;
```

No change to `IntegrityGateInput` (derives from `IntegrityGateEntry` via `Omit<IntegrityGateEntry, "timestamp">`), so the new fields automatically flow through.

### 5.2 Validation Changes (`src/checkpoint/validation.ts`)

Add to `validateIntegrityGateEntry()` function, after the existing `optionalStringArray(data, "failedTests", ctx)` line:

```typescript
optionalNumber(data, "branchCoverage", ctx);
optionalString(data, "regressionSource", ctx);
```

### 5.3 Engine Changes (`src/checkpoint/engine.ts`)

No changes required. The `updateIntegrityGate()` function uses spread operator (`{ ...result, timestamp }`) which automatically includes any new fields present in `IntegrityGateInput`.

### 5.4 Template Changes (`resources/templates/_TEMPLATE-EXECUTION-STATE.json`)

Optionally add `branchCoverage` and `regressionSource` to the `phase-0` gate entry to demonstrate the new fields in the template. Since they are optional, the template remains valid without them.

### 5.5 SKILL.md Template Changes

Add an "Integrity Gate" section between phases in both:
- `resources/skills-templates/core/x-dev-epic-implement/SKILL.md`
- `resources/github-skills-templates/dev/x-dev-epic-implement.md`

### 5.6 Golden File Updates

Regenerate golden files for all 8 profiles after SKILL.md template update.

---

## 6. Dependency Graph

```
UT-1a,1b,1c,1d,1e ─┐
                    ├─> UT-3a, UT-3b ─┐
UT-2a,2b,2c,2d,2e ─┘                  │
                                       ├─> UT-6 ─> IT-1
UT-4a, UT-4b ─────────────────────────┘
UT-5a, UT-5b ─────────────────────────┘
UT-7a, UT-7b, UT-7c ──────────────────────────────> IT-2

CT-1 ─┬─> CT-2
      ├─> CT-3
      ├─> CT-4
      ├─> CT-5
      └─> CT-6
```

---

## 7. Test Execution Order

### Phase A: Validation Unit Tests (UT-1, UT-2, UT-3) -- All Parallel

Write all validation tests first. They test the `validateIntegrityGateEntry()` function with the new `branchCoverage` and `regressionSource` fields. These are pure synchronous functions with no I/O.

### Phase B: Engine Unit Tests (UT-4, UT-5, UT-6) -- All Parallel

Write engine tests that verify `updateIntegrityGate()` stores and round-trips the new fields. These require filesystem I/O (temp directories).

### Phase C: Template Tests (UT-7) -- Parallel

Write template validation tests after the template JSON is updated.

### Phase D: Integration Test (IT-1) -- Parallel

Write the full round-trip integration test after all unit tests pass.

### Phase E: Content Tests (CT-1 through CT-6) -- All Parallel

Write content tests after the SKILL.md templates are updated with the integrity gate section.

### Phase F: Golden File Regeneration (IT-2) -- Sequential

Regenerate golden files and verify byte-for-byte parity via existing `byte-for-byte.test.ts`.

---

## 8. Coverage Strategy

| File | Expected Lines | Expected Branches | Strategy |
|:---|:---|:---|:---|
| `types.ts` | 100% | 100% | Type-only changes; no logic branches added. |
| `validation.ts` | >= 95% | >= 90% | UT-1 through UT-3 cover optional field validation for both accept and reject paths. Existing tests already cover all other branches. |
| `engine.ts` | >= 95% | >= 90% | UT-4 through UT-6 cover presence and absence of new fields in `updateIntegrityGate`. No new branches in engine code (spread operator handles all cases). |
| SKILL.md templates | N/A | N/A | Content tests (CT-1 through CT-6) validate textual content. Golden file tests (IT-2) validate byte-for-byte output. |

**Total new test count:** 18 unit (UT-1a through UT-7c) + 2 integration (IT-1, IT-2) + 6 content (CT-1 through CT-6) = **26 test scenarios**

---

## 9. Traceability Matrix

| Gherkin Scenario (story-0005-0006) | Unit Tests | Integration Tests | Content Tests |
|:---|:---|:---|:---|
| Gate PASS -- compile, tests, coverage OK | UT-4a, UT-4b | IT-1 | CT-2 |
| Gate FAIL -- regression identified | UT-5a, UT-6 | IT-1 | CT-3, CT-4 |
| Gate FAIL -- coverage below threshold | UT-1b, UT-1c | -- | CT-2 |
| Gate FAIL -- regression unidentified | UT-5b | -- | CT-3 |
| Gate result registration (checkpoint) | UT-4a, UT-5a, UT-6, UT-7c | IT-1 | CT-5 |
| SKILL.md integrity gate section | -- | IT-2 | CT-1, CT-2, CT-3, CT-4, CT-5, CT-6 |
| branchCoverage validation | UT-1a through UT-1e | -- | -- |
| regressionSource validation | UT-2a through UT-2e | -- | -- |
| Both fields coexistence | UT-3a, UT-3b | IT-1 | -- |
| Template with new fields | UT-7a, UT-7b, UT-7c | -- | -- |
| Git revert documentation | -- | -- | CT-4 |
| Dual copy consistency (RULE-001) | -- | IT-2 | CT-6 |
