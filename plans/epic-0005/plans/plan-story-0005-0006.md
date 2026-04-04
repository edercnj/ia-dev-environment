# Implementation Plan -- story-0005-0006: Integrity Gate Between Phases

**Story:** `story-0005-0006.md`

---

## 1. Affected Layers and Components

| Layer | Impact | Details |
|-------|--------|---------|
| `src/checkpoint/types.ts` | **MODIFY** | Add `branchCoverage` (optional number) and `regressionSource` (optional string) to `IntegrityGateEntry` |
| `src/checkpoint/validation.ts` | **MODIFY** | Add validation for `branchCoverage` and `regressionSource` in `validateIntegrityGateEntry()` |
| `src/checkpoint/engine.ts` | **NO CHANGE** | Engine already passes through all fields via spread operator; no code changes required |
| `resources/templates/_TEMPLATE-EXECUTION-STATE.json` | **MODIFY** | Add `branchCoverage` and `regressionSource` to the integrity gate example entry |
| `resources/skills-templates/core/x-dev-epic-implement/SKILL.md` | **MODIFY** | Add Integrity Gate section between Phase 1 and Phase 2 (or replace Phase 1 placeholder) |
| `resources/github-skills-templates/dev/x-dev-epic-implement.md` | **MODIFY** | Add corresponding Integrity Gate section for GitHub Copilot consistency |
| `tests/golden/` (8 profiles, 3 locations each) | **MODIFY** | Update golden files that contain the x-dev-epic-implement SKILL.md |
| `tests/node/checkpoint/validation.test.ts` | **MODIFY** | Add tests for `branchCoverage` and `regressionSource` validation |
| `tests/node/checkpoint/engine.test.ts` | **MODIFY** | Add tests for `updateIntegrityGate` with `branchCoverage` and `regressionSource` |
| `tests/node/checkpoint/acceptance.test.ts` | **MODIFY** | Add acceptance test for gate with new optional fields; update template field assertions |
| `tests/node/content/x-dev-epic-implement-content.test.ts` | **MODIFY** | Add content test for the integrity gate section |

---

## 2. New Classes/Interfaces to Create

No new files are created. All changes are modifications to existing types and templates.

### 2.1 Type Modifications (`src/checkpoint/types.ts`)

| Change | Kind | Description |
|--------|------|-------------|
| `IntegrityGateEntry.branchCoverage` | new optional field | `readonly branchCoverage?: number \| undefined` -- branch coverage percentage |
| `IntegrityGateEntry.regressionSource` | new optional field | `readonly regressionSource?: string \| undefined` -- story ID that caused regression |

The `IntegrityGateInput` type (defined as `Omit<IntegrityGateEntry, "timestamp">`) will automatically inherit both new fields since it is derived from `IntegrityGateEntry`.

---

## 3. Existing Classes to Modify

### 3.1 `src/checkpoint/types.ts`

Add two optional fields to the `IntegrityGateEntry` interface:

```
interface IntegrityGateEntry {
  readonly status: "PASS" | "FAIL";
  readonly timestamp: string;
  readonly testCount: number;
  readonly coverage: number;
+ readonly branchCoverage?: number | undefined;     // NEW
  readonly failedTests?: readonly string[] | undefined;
+ readonly regressionSource?: string | undefined;   // NEW
}
```

### 3.2 `src/checkpoint/validation.ts`

In `validateIntegrityGateEntry()`, add two validation calls after the existing `optionalStringArray(data, "failedTests", ctx)` line:

- `optionalNumber(data, "branchCoverage", ctx)` -- validates `branchCoverage` is a number when present
- `optionalString(data, "regressionSource", ctx)` -- validates `regressionSource` is a string when present

### 3.3 `resources/templates/_TEMPLATE-EXECUTION-STATE.json`

Add `branchCoverage` and `regressionSource` to the sample `phase-0` integrity gate entry to document the full schema:

```json
"integrityGates": {
  "phase-0": {
    "status": "PASS",
    "timestamp": "2024-01-01T00:00:00.000Z",
    "testCount": 0,
    "coverage": 0,
    "branchCoverage": 0,
    "regressionSource": null
  }
}
```

Note: Using `null` for `regressionSource` is intentional as a documentation placeholder. However, the validation function treats `undefined` (absence) as valid. If this causes a validation issue (since `optionalString` checks `typeof v !== "string"` when `v !== undefined`), we should use `undefined` by omitting the field or choosing a sample value like `""`. The safer approach is to leave `regressionSource` out of the template to demonstrate the optional nature, or set it to a sample string value. Decision: include both fields with sample values (`0` for branchCoverage, omit `regressionSource` since it only appears on FAIL with identified regression).

Revised approach for the template:

```json
"integrityGates": {
  "phase-0": {
    "status": "PASS",
    "timestamp": "2024-01-01T00:00:00.000Z",
    "testCount": 0,
    "coverage": 0,
    "branchCoverage": 0
  }
}
```

### 3.4 `resources/skills-templates/core/x-dev-epic-implement/SKILL.md`

Add a new section **"Integrity Gate"** between Phase 1 and Phase 2. This documents the integrity gate protocol that the AI orchestrator must follow between each phase:

Content to add (approximately):

```markdown
## Integrity Gate (Between Phases)

After all stories in a phase complete successfully, execute an integrity gate before advancing to the next phase.

### Gate Execution

1. **Compile**: Run `{{COMPILE_COMMAND}}` -- full project compilation
2. **Test**: Run `{{TEST_COMMAND}}` -- full test suite (not just current phase)
3. **Coverage**: Run `{{COVERAGE_COMMAND}}` -- validate thresholds (>= 95% line, >= 90% branch)

### Gate Result

Record the result via `updateIntegrityGate(epicDir, phase, result)`:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `status` | `PASS` \| `FAIL` | M | Overall gate result |
| `testCount` | number | M | Total tests executed |
| `coverage` | number | M | Line coverage percentage |
| `branchCoverage` | number | O | Branch coverage percentage |
| `failedTests` | string[] | O | Names of failed tests |
| `regressionSource` | string | O | Story ID that caused regression |

### On PASS

Advance to the next phase.

### On FAIL -- Regression Identified

1. Correlate failed tests with story commits (via git log)
2. Identify the regression source story
3. `git revert` the offending story's commit(s)
4. Mark story as FAILED with summary: "Regression detected by integrity gate"
5. Execute block propagation for dependents
6. Record gate result with `regressionSource`

### On FAIL -- Regression Unidentified

1. Pause execution
2. Report to user for manual diagnosis
3. Record gate result without `regressionSource`
```

### 3.5 `resources/github-skills-templates/dev/x-dev-epic-implement.md`

Add a condensed version of the same Integrity Gate section for consistency with the Claude skill template. The GitHub template is typically more compact, so the section will be a summarized version covering gate execution, result fields, and PASS/FAIL handling.

### 3.6 Golden Files (8 profiles x 3 locations)

Update all golden file copies of the x-dev-epic-implement SKILL.md to include the new Integrity Gate section. The affected locations per profile are:

| Location | Path Pattern |
|----------|-------------|
| `.claude/skills/` | `tests/golden/{profile}/.claude/skills/x-dev-epic-implement/SKILL.md` |
| `.github/skills/` | `tests/golden/{profile}/.github/skills/x-dev-epic-implement/SKILL.md` |
| `.agents/skills/` | `tests/golden/{profile}/.agents/skills/x-dev-epic-implement/SKILL.md` |

The 8 profiles are: `go-gin`, `java-quarkus`, `java-spring`, `kotlin-ktor`, `python-click-cli`, `python-fastapi`, `rust-axum`, `typescript-nestjs`.

Total golden files to update: **24 files** (8 profiles x 3 locations).

### 3.7 Test Files

See Section 6 (Test Strategy) below.

---

## 4. Dependency Direction Validation

```
src/checkpoint/types.ts      --> (no imports -- pure type definitions)
src/checkpoint/validation.ts --> types.ts, ../exceptions.ts (inward only)
src/checkpoint/engine.ts     --> types.ts, validation.ts, ../exceptions.ts (inward only)
```

**Verification checklist:**
- [x] No import from `src/assembler/`
- [x] No import from `src/cli*.ts`
- [x] No import from `src/config.ts`
- [x] No import from `src/models.ts`
- [x] Types are self-contained with no external dependencies
- [x] Validation depends only on types and exceptions
- [x] Engine depends only on types, validation, and exceptions
- [x] Template files are static JSON/Markdown -- no dependency issues

The new optional fields (`branchCoverage`, `regressionSource`) are primitive types (number, string) with no additional imports required.

---

## 5. Integration Points

| Consumer | How It Integrates | When |
|----------|-------------------|------|
| story-0005-0005 (Core Loop) | Calls `updateIntegrityGate()` with `branchCoverage` and `regressionSource` in the result object | Already implemented; new fields flow through automatically via spread |
| story-0005-0014 (blocked by this story) | Depends on `IntegrityGateEntry` having the full field set | After this story is complete |
| x-dev-epic-implement SKILL.md | AI agent reads the integrity gate section to know how to orchestrate gates | Runtime (AI reads the template) |
| Checkpoint validation | `validateIntegrityGateEntry()` validates new fields when reading checkpoint from disk | Every `readCheckpoint()` call |

### Backward Compatibility

Adding optional fields to `IntegrityGateEntry` is fully backward-compatible:

- Existing checkpoint files without `branchCoverage` and `regressionSource` will pass validation (the fields are optional)
- Existing code that creates `IntegrityGateInput` objects without the new fields will continue to work
- The `Omit<IntegrityGateEntry, "timestamp">` derivation of `IntegrityGateInput` automatically includes the new fields as optional
- The spread operator in `updateIntegrityGate()` will pass through any new fields without code changes

---

## 6. Configuration Changes

N/A -- No environment variables, config files, or build configuration changes. The only "configuration" is the template JSON file (`_TEMPLATE-EXECUTION-STATE.json`), which is a documentation artifact.

---

## 7. Risk Assessment

| Risk | Severity | Probability | Mitigation |
|------|----------|-------------|------------|
| Golden file count is large (24 files) | Low | Certain | All golden files for the `.claude` and `.agents` locations are byte-for-byte copies of the source template; use a systematic find-and-replace or regeneration approach. The `.github` golden files mirror the GitHub template. |
| Template JSON validation fails with added `null` values | Medium | Medium | Use `undefined` (omit field) rather than `null` for optional fields in the template. Validate the template passes `validateExecutionState()` after modification. |
| Content tests may need new assertions for the integrity gate section | Low | Certain | Add test cases in `x-dev-epic-implement-content.test.ts` that verify the integrity gate section exists and contains key terms (`branchCoverage`, `regressionSource`, `updateIntegrityGate`). |
| Existing validation tests may not cover all edge cases for new fields | Medium | Low | Add explicit tests for: valid number, valid string, wrong type (number where string expected), `undefined` (absent), and co-occurrence with `failedTests`. |
| story-0005-0005 (Core Loop) not yet implemented | Low | Known | This story only adds the type and template infrastructure. The core loop (story-0005-0005) will consume these types at integration time. |
| Dual-copy consistency between Claude and GitHub templates | Medium | Medium | Content test already verifies critical terms appear in both templates. Add `branchCoverage` and `regressionSource` to the consistency check list. |

---

## 8. Implementation Order (TDD)

### Phase A: Type Extension

1. Add `branchCoverage` and `regressionSource` to `IntegrityGateEntry` in `src/checkpoint/types.ts`.
2. Verify `IntegrityGateInput` (derived type) inherits the new fields.
3. Run `npx tsc --noEmit` to confirm compilation.

### Phase B: Validation (Red-Green-Refactor)

1. **Test (Red):** `validateIntegrityGateEntry_branchCoverageNotNumber_throwsValidationError` -- pass `branchCoverage: "high"`, expect `CheckpointValidationError`.
2. **Test (Red):** `validateIntegrityGateEntry_regressionSourceNotString_throwsValidationError` -- pass `regressionSource: 42`, expect `CheckpointValidationError`.
3. **Test (Red):** `validateIntegrityGateEntry_validWithBranchCoverage_doesNotThrow` -- pass valid gate with `branchCoverage: 90.5`.
4. **Test (Red):** `validateIntegrityGateEntry_validWithRegressionSource_doesNotThrow` -- pass valid gate with `regressionSource: "0042-0005"`.
5. **Test (Red):** `validateIntegrityGateEntry_validWithAllOptionalFields_doesNotThrow` -- pass gate with `failedTests`, `branchCoverage`, and `regressionSource` all present.
6. **Implement (Green):** Add `optionalNumber(data, "branchCoverage", ctx)` and `optionalString(data, "regressionSource", ctx)` to `validateIntegrityGateEntry()`.

### Phase C: Engine Tests

1. **Test:** `updateIntegrityGate_withBranchCoverage_storesBranchCoverage` -- verify the value persists through write/read.
2. **Test:** `updateIntegrityGate_withRegressionSource_storesRegressionSource` -- verify the value persists through write/read.
3. **Test:** `updateIntegrityGate_failWithRegressionAndBranchCoverage_storesAllFields` -- FAIL gate with all optional fields populated.
4. No engine code changes needed -- existing spread operator handles new fields.

### Phase D: Template Update

1. Modify `resources/templates/_TEMPLATE-EXECUTION-STATE.json` to include `branchCoverage` in the sample gate entry.
2. **Test:** Existing template validation test (`template_passesValidation_noValidationErrors`) must still pass.
3. **Test:** Add assertion for `branchCoverage` field presence in template gate entry.

### Phase E: SKILL.md Templates

1. Add Integrity Gate section to `resources/skills-templates/core/x-dev-epic-implement/SKILL.md`.
2. Add Integrity Gate section to `resources/github-skills-templates/dev/x-dev-epic-implement.md`.
3. **Test:** Add content test assertions for integrity gate presence and key terms.
4. **Test:** Add dual-copy consistency terms (`branchCoverage`, `regressionSource`, `Integrity Gate`, `updateIntegrityGate`).

### Phase F: Golden Files

1. Update all 24 golden file copies (8 profiles x 3 locations) to match the updated source templates.
2. **Verify:** Run the full byte-for-byte integration test suite to confirm all golden files match.

### Phase G: Refinement

1. Verify coverage >= 95% line, >= 90% branch.
2. Run `npx tsc --noEmit` for final type check.
3. Review all changes for dual-copy consistency (Claude vs GitHub templates).

---

## 9. Test File Structure

```
tests/node/checkpoint/
  validation.test.ts          # Add ~5 tests for branchCoverage/regressionSource validation
  engine.test.ts              # Add ~3 tests for engine round-trip with new fields
  acceptance.test.ts          # Add ~2 tests for template field assertions + acceptance

tests/node/content/
  x-dev-epic-implement-content.test.ts  # Add ~4 tests for integrity gate section content
```

### Estimated New Tests: ~14

---

## 10. Acceptance Criteria Traceability

| Story Gherkin Scenario | Test File | Phase |
|------------------------|-----------|-------|
| Gate PASS with coverage | `engine.test.ts` (existing + new branchCoverage test) | C |
| Gate FAIL with regression identified | `engine.test.ts` (new regressionSource test) | C |
| Gate FAIL without regressionSource | `validation.test.ts` (valid without optional fields) | B |
| Gate executed between each phase (RULE-004) | `x-dev-epic-implement-content.test.ts` (section exists) | E |
| branchCoverage field validation | `validation.test.ts` | B |
| regressionSource field validation | `validation.test.ts` | B |
| Template includes new fields | `acceptance.test.ts` | D |
| SKILL.md documents integrity gate | `x-dev-epic-implement-content.test.ts` | E |
| Dual-copy consistency | `x-dev-epic-implement-content.test.ts` | E |
