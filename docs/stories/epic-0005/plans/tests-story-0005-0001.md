# Test Plan -- story-0005-0001: Execution State Schema + Checkpoint Engine

## 1. Overview

**Double-Loop TDD** plan for the checkpoint subsystem. The outer loop defines acceptance tests mapped 1:1 from Gherkin scenarios. The inner loop defines unit tests ordered by **Transformation Priority Premise (TPP)** -- from degenerate cases to complex behavior.

**Modules under test:**

| Module | Location | Responsibility |
|--------|----------|----------------|
| `validation.ts` | `src/checkpoint/validation.ts` | Schema validation (field presence, type checks, enum guards) |
| `engine.ts` | `src/checkpoint/engine.ts` | CRUD operations with atomic persistence |
| `types.ts` | `src/checkpoint/types.ts` | Type exports (compile-time only -- no runtime tests) |
| `exceptions.ts` | `src/exceptions.ts` | `CheckpointValidationError`, `CheckpointIOError` |

**Test files:**

| File | Type | Contents |
|------|------|----------|
| `tests/node/checkpoint/validation.test.ts` | Unit | Schema validation functions |
| `tests/node/checkpoint/engine.test.ts` | Unit | Checkpoint engine operations |
| `tests/node/checkpoint/acceptance.test.ts` | Acceptance | Gherkin scenario mapping (outer loop) |
| `tests/node/exceptions.test.ts` | Unit | New error classes (append to existing file) |

---

## 2. Acceptance Tests (Outer Loop)

Each acceptance test maps to one Gherkin scenario from section 7 of the story. These tests exercise the public API end-to-end through real filesystem operations using `mkdtempSync` temporary directories.

| ID | Gherkin Scenario | Test Name | Depends On | Parallel |
|----|-----------------|-----------|------------|----------|
| AT-1 | Criacao de checkpoint em diretorio inexistente | `createCheckpoint_directoryDoesNotExist_throwsCheckpointIOError` | -- | yes |
| AT-2 | Rejeicao de checkpoint com schema invalido | `readCheckpoint_missingEpicIdField_throwsValidationErrorWithMessage` | -- | yes |
| AT-3 | Rejeicao de status enum invalido no checkpoint | `readCheckpoint_invalidStatusEnum_throwsValidationErrorWithMessage` | -- | yes |
| AT-4 | Criacao de checkpoint inicial com estado vazio | `createCheckpoint_fiveStoriesWithDefaults_createsFileWithAllPending` | -- | yes |
| AT-5 | Leitura de checkpoint existente com validacao de schema | `readCheckpoint_validFileWithThreeStories_returnsTypedExecutionState` | AT-4 | yes |
| AT-6 | Atualizacao atomica de status de story para SUCCESS | `updateStoryStatus_setSuccessWithCommitSha_atomicallyUpdatesOnlyTargetStory` | AT-4 | yes |
| AT-7 | Atualizacao atomica de status de story para FAILED com retry increment | `updateStoryStatus_setFailedWithRetryIncrement_updatesStatusAndRetries` | AT-4 | yes |
| AT-8 | Atualizacao de integrity gate result | `updateIntegrityGate_phaseZeroPass_recordsResultWithAutoTimestamp` | AT-4 | yes |

### AT-1: createCheckpoint_directoryDoesNotExist_throwsCheckpointIOError

```
GIVEN the directory "docs/stories/epic-9999/" does not exist
WHEN createCheckpoint is called
THEN a CheckpointIOError is thrown with message indicating missing directory
```

### AT-2: readCheckpoint_missingEpicIdField_throwsValidationErrorWithMessage

```
GIVEN an "execution-state.json" exists with the "epicId" field absent
WHEN readCheckpoint is called for the directory
THEN a CheckpointValidationError is thrown
AND the message includes "epicId is required"
```

### AT-3: readCheckpoint_invalidStatusEnum_throwsValidationErrorWithMessage

```
GIVEN an "execution-state.json" exists with a story having status "UNKNOWN"
WHEN readCheckpoint is called for the directory
THEN a CheckpointValidationError is thrown
AND the message includes "invalid status"
```

### AT-4: createCheckpoint_fiveStoriesWithDefaults_createsFileWithAllPending

```
GIVEN the directory "docs/stories/epic-0042/" exists
AND no "execution-state.json" file exists in the directory
WHEN createCheckpoint is called with epicId "0042", branch "feat/epic-0042-full-implementation", 5 stories, and mode {parallel: false, skipReview: false}
THEN "execution-state.json" is created in the directory
AND all 5 stories have status "PENDING"
AND retries is 0 for all stories
AND metrics.storiesCompleted is 0
AND metrics.storiesTotal is 5
```

### AT-5: readCheckpoint_validFileWithThreeStories_returnsTypedExecutionState

```
GIVEN a valid "execution-state.json" with 3 stories exists in the directory
WHEN readCheckpoint is called for the directory
THEN it returns a typed ExecutionState object
AND all mandatory fields are present
AND all status enum values are valid
```

### AT-6: updateStoryStatus_setSuccessWithCommitSha_atomicallyUpdatesOnlyTargetStory

```
GIVEN a checkpoint exists with story "0042-0001" in status "IN_PROGRESS"
WHEN updateStoryStatus is called with storyId "0042-0001" and {status: "SUCCESS", commitSha: "abc123"}
THEN a temporary file ".execution-state.json.tmp" is created and renamed to "execution-state.json"
AND story "0042-0001" has status "SUCCESS" and commitSha "abc123"
AND all other stories remain unchanged
```

### AT-7: updateStoryStatus_setFailedWithRetryIncrement_updatesStatusAndRetries

```
GIVEN a checkpoint exists with story "0042-0003" in status "IN_PROGRESS" and retries 1
WHEN updateStoryStatus is called with {status: "FAILED", retries: 2}
THEN story "0042-0003" has status "FAILED" and retries 2
```

### AT-8: updateIntegrityGate_phaseZeroPass_recordsResultWithAutoTimestamp

```
GIVEN a valid checkpoint exists
WHEN updateIntegrityGate is called with phase 0 and {status: "PASS", testCount: 42, coverage: 96.3}
THEN integrityGates["phase-0"] contains the recorded result
AND the timestamp is automatically populated
```

---

## 3. Unit Tests -- validation.ts (Inner Loop)

TPP ordering: degenerate (nil/throw) -> constant -> scalar -> collection

### 3.1 validateExecutionState

| ID | Test Name | TPP Level | Type | Depends On | Parallel |
|----|-----------|-----------|------|------------|----------|
| UT-V01 | `validateExecutionState_nullInput_throwsValidationError` | 1 (nil) | unit | -- | yes |
| UT-V02 | `validateExecutionState_undefinedInput_throwsValidationError` | 1 (nil) | unit | -- | yes |
| UT-V03 | `validateExecutionState_emptyObject_throwsWithEpicIdRequired` | 2 (constant) | unit | -- | yes |
| UT-V04 | `validateExecutionState_missingBranch_throwsWithBranchRequired` | 2 (constant) | unit | -- | yes |
| UT-V05 | `validateExecutionState_missingStartedAt_throwsWithStartedAtRequired` | 2 (constant) | unit | -- | yes |
| UT-V06 | `validateExecutionState_missingCurrentPhase_throwsWithCurrentPhaseRequired` | 2 (constant) | unit | -- | yes |
| UT-V07 | `validateExecutionState_missingMode_throwsWithModeRequired` | 2 (constant) | unit | -- | yes |
| UT-V08 | `validateExecutionState_missingStories_throwsWithStoriesRequired` | 2 (constant) | unit | -- | yes |
| UT-V09 | `validateExecutionState_missingMetrics_throwsWithMetricsRequired` | 2 (constant) | unit | -- | yes |
| UT-V10 | `validateExecutionState_epicIdWrongType_throwsWithTypeError` | 3 (scalar) | unit | -- | yes |
| UT-V11 | `validateExecutionState_currentPhaseNotNumber_throwsWithTypeError` | 3 (scalar) | unit | -- | yes |
| UT-V12 | `validateExecutionState_modeParallelNotBoolean_throwsWithTypeError` | 3 (scalar) | unit | -- | yes |
| UT-V13 | `validateExecutionState_modeSkipReviewNotBoolean_throwsWithTypeError` | 3 (scalar) | unit | -- | yes |
| UT-V14 | `validateExecutionState_storiesNotObject_throwsWithTypeError` | 4 (collection) | unit | -- | yes |
| UT-V15 | `validateExecutionState_validCompleteState_doesNotThrow` | 5 (constant+) | unit | -- | yes |

### 3.2 validateStoryEntry

| ID | Test Name | TPP Level | Type | Depends On | Parallel |
|----|-----------|-----------|------|------------|----------|
| UT-V16 | `validateStoryEntry_nullEntry_throwsValidationError` | 1 (nil) | unit | -- | yes |
| UT-V17 | `validateStoryEntry_missingStatus_throwsWithStatusRequired` | 2 (constant) | unit | -- | yes |
| UT-V18 | `validateStoryEntry_missingPhase_throwsWithPhaseRequired` | 2 (constant) | unit | -- | yes |
| UT-V19 | `validateStoryEntry_missingRetries_throwsWithRetriesRequired` | 2 (constant) | unit | -- | yes |
| UT-V20 | `validateStoryEntry_invalidStatusEnum_throwsWithInvalidStatus` | 3 (scalar) | unit | -- | yes |
| UT-V21 | `validateStoryEntry_retriesNotNumber_throwsWithTypeError` | 3 (scalar) | unit | -- | yes |
| UT-V22 | `validateStoryEntry_validEntryWithOptionalFields_doesNotThrow` | 5 (constant+) | unit | -- | yes |
| UT-V23 | `validateStoryEntry_validMinimalEntry_doesNotThrow` | 5 (constant+) | unit | -- | yes |

### 3.3 validateStoryStatus / isValidStoryStatus

| ID | Test Name | TPP Level | Type | Depends On | Parallel |
|----|-----------|-----------|------|------------|----------|
| UT-V24 | `isValidStoryStatus_nullValue_returnsFalse` | 1 (nil) | unit | -- | yes |
| UT-V25 | `isValidStoryStatus_emptyString_returnsFalse` | 2 (constant) | unit | -- | yes |
| UT-V26 | `isValidStoryStatus_unknownValue_returnsFalse` | 3 (scalar) | unit | -- | yes |
| UT-V27 | `isValidStoryStatus_pendingValue_returnsTrue` | 3 (scalar) | unit | -- | yes |
| UT-V28 | `isValidStoryStatus_inProgressValue_returnsTrue` | 3 (scalar) | unit | -- | yes |
| UT-V29 | `isValidStoryStatus_successValue_returnsTrue` | 3 (scalar) | unit | -- | yes |
| UT-V30 | `isValidStoryStatus_failedValue_returnsTrue` | 3 (scalar) | unit | -- | yes |
| UT-V31 | `isValidStoryStatus_blockedValue_returnsTrue` | 3 (scalar) | unit | -- | yes |
| UT-V32 | `isValidStoryStatus_partialValue_returnsTrue` | 3 (scalar) | unit | -- | yes |
| UT-V33 | `isValidStoryStatus_allValidValues_eachReturnsTrue` | 4 (collection) | unit | -- | yes |
| UT-V34 | `isValidStoryStatus_lowercasePending_returnsFalse` | 3 (scalar) | unit | -- | yes |

### 3.4 validateIntegrityGateEntry

| ID | Test Name | TPP Level | Type | Depends On | Parallel |
|----|-----------|-----------|------|------------|----------|
| UT-V35 | `validateIntegrityGateEntry_nullEntry_throwsValidationError` | 1 (nil) | unit | -- | yes |
| UT-V36 | `validateIntegrityGateEntry_missingStatus_throwsWithStatusRequired` | 2 (constant) | unit | -- | yes |
| UT-V37 | `validateIntegrityGateEntry_invalidStatus_throwsWithInvalidGateStatus` | 3 (scalar) | unit | -- | yes |
| UT-V38 | `validateIntegrityGateEntry_missingTestCount_throwsWithTestCountRequired` | 2 (constant) | unit | -- | yes |
| UT-V39 | `validateIntegrityGateEntry_missingCoverage_throwsWithCoverageRequired` | 2 (constant) | unit | -- | yes |
| UT-V40 | `validateIntegrityGateEntry_validEntry_doesNotThrow` | 5 (constant+) | unit | -- | yes |

### 3.5 validateMetrics

| ID | Test Name | TPP Level | Type | Depends On | Parallel |
|----|-----------|-----------|------|------------|----------|
| UT-V41 | `validateMetrics_nullInput_throwsValidationError` | 1 (nil) | unit | -- | yes |
| UT-V42 | `validateMetrics_missingStoriesCompleted_throwsRequired` | 2 (constant) | unit | -- | yes |
| UT-V43 | `validateMetrics_missingStoriesTotal_throwsRequired` | 2 (constant) | unit | -- | yes |
| UT-V44 | `validateMetrics_storiesCompletedNotNumber_throwsTypeError` | 3 (scalar) | unit | -- | yes |
| UT-V45 | `validateMetrics_validMetrics_doesNotThrow` | 5 (constant+) | unit | -- | yes |
| UT-V46 | `validateMetrics_validMetricsWithOptionalEstimated_doesNotThrow` | 5 (constant+) | unit | -- | yes |

---

## 4. Unit Tests -- engine.ts (Inner Loop)

TPP ordering: degenerate (throw) -> nil -> constant -> scalar -> collection

All engine tests use `mkdtempSync` for isolated temp directories, cleaned up in `afterEach`.

### 4.1 atomicWriteJson (private, tested indirectly)

Atomic write behavior is verified through the public functions. No direct tests for the private helper -- its correctness is validated by checking file content after every write operation and verifying no `.tmp` file remains.

### 4.2 createCheckpoint

| ID | Test Name | TPP Level | Type | Depends On | Parallel |
|----|-----------|-----------|------|------------|----------|
| UT-E01 | `createCheckpoint_directoryDoesNotExist_throwsCheckpointIOError` | 1 (nil/degenerate) | unit | -- | yes |
| UT-E02 | `createCheckpoint_emptyStoriesList_createsFileWithEmptyStoriesMap` | 2 (constant) | unit | -- | yes |
| UT-E03 | `createCheckpoint_singleStory_createsFileWithOnePendingEntry` | 3 (scalar) | unit | UT-E02 | yes |
| UT-E04 | `createCheckpoint_fiveStories_allStatusPending` | 4 (collection) | unit | UT-E03 | yes |
| UT-E05 | `createCheckpoint_fiveStories_allRetriesZero` | 4 (collection) | unit | UT-E03 | yes |
| UT-E06 | `createCheckpoint_fiveStories_metricsStoriesCompletedIsZero` | 4 (collection) | unit | UT-E03 | yes |
| UT-E07 | `createCheckpoint_fiveStories_metricsStoriesTotalIsFive` | 4 (collection) | unit | UT-E03 | yes |
| UT-E08 | `createCheckpoint_setsEpicIdCorrectly` | 3 (scalar) | unit | UT-E02 | yes |
| UT-E09 | `createCheckpoint_setBranchCorrectly` | 3 (scalar) | unit | UT-E02 | yes |
| UT-E10 | `createCheckpoint_setsStartedAtToIso8601` | 3 (scalar) | unit | UT-E02 | yes |
| UT-E11 | `createCheckpoint_setsCurrentPhaseToZero` | 3 (scalar) | unit | UT-E02 | yes |
| UT-E12 | `createCheckpoint_setsModeFromInput` | 3 (scalar) | unit | UT-E02 | yes |
| UT-E13 | `createCheckpoint_writesValidJsonToFile` | 3 (scalar) | unit | UT-E02 | yes |
| UT-E14 | `createCheckpoint_noTmpFileRemainsAfterWrite` | 3 (scalar) | unit | UT-E02 | yes |
| UT-E15 | `createCheckpoint_returnsExecutionStateObject` | 3 (scalar) | unit | UT-E02 | yes |
| UT-E16 | `createCheckpoint_integrityGatesInitializedEmpty` | 3 (scalar) | unit | UT-E02 | yes |

### 4.3 readCheckpoint

| ID | Test Name | TPP Level | Type | Depends On | Parallel |
|----|-----------|-----------|------|------------|----------|
| UT-E17 | `readCheckpoint_fileDoesNotExist_throwsCheckpointIOError` | 1 (nil/degenerate) | unit | -- | yes |
| UT-E18 | `readCheckpoint_invalidJson_throwsCheckpointIOError` | 1 (nil/degenerate) | unit | -- | yes |
| UT-E19 | `readCheckpoint_invalidSchema_throwsCheckpointValidationError` | 2 (constant) | unit | -- | yes |
| UT-E20 | `readCheckpoint_validFile_returnsExecutionState` | 3 (scalar) | unit | -- | yes |
| UT-E21 | `readCheckpoint_validFile_allFieldsPreserved` | 4 (collection) | unit | UT-E20 | yes |
| UT-E22 | `readCheckpoint_fileWithOptionalFields_preservesOptionals` | 5 (constant+) | unit | UT-E20 | yes |
| UT-E23 | `readCheckpoint_validFile_storiesMapHasCorrectKeys` | 4 (collection) | unit | UT-E20 | yes |
| UT-E24 | `readCheckpoint_invalidStoryStatus_throwsValidationError` | 3 (scalar) | unit | -- | yes |

### 4.4 updateStoryStatus

| ID | Test Name | TPP Level | Type | Depends On | Parallel |
|----|-----------|-----------|------|------------|----------|
| UT-E25 | `updateStoryStatus_nonexistentStoryId_throwsCheckpointValidationError` | 1 (nil/degenerate) | unit | -- | yes |
| UT-E26 | `updateStoryStatus_nonexistentFile_throwsCheckpointIOError` | 1 (nil/degenerate) | unit | -- | yes |
| UT-E27 | `updateStoryStatus_setStatusToSuccess_updatesStatus` | 3 (scalar) | unit | -- | yes |
| UT-E28 | `updateStoryStatus_setCommitSha_updatesCommitSha` | 3 (scalar) | unit | -- | yes |
| UT-E29 | `updateStoryStatus_setStatusToFailed_updatesStatus` | 3 (scalar) | unit | -- | yes |
| UT-E30 | `updateStoryStatus_incrementRetries_updatesRetries` | 3 (scalar) | unit | -- | yes |
| UT-E31 | `updateStoryStatus_otherStoriesUnchanged_preservesOtherEntries` | 4 (collection) | unit | UT-E27 | yes |
| UT-E32 | `updateStoryStatus_partialUpdate_mergesWithExistingFields` | 5 (constant+) | unit | UT-E27 | yes |
| UT-E33 | `updateStoryStatus_noTmpFileRemainsAfterWrite_atomicCleanup` | 3 (scalar) | unit | UT-E27 | yes |
| UT-E34 | `updateStoryStatus_returnsUpdatedExecutionState` | 3 (scalar) | unit | UT-E27 | yes |

### 4.5 updateIntegrityGate

| ID | Test Name | TPP Level | Type | Depends On | Parallel |
|----|-----------|-----------|------|------------|----------|
| UT-E35 | `updateIntegrityGate_nonexistentFile_throwsCheckpointIOError` | 1 (nil/degenerate) | unit | -- | yes |
| UT-E36 | `updateIntegrityGate_phaseZeroPass_createsGateEntry` | 3 (scalar) | unit | -- | yes |
| UT-E37 | `updateIntegrityGate_phaseZeroPass_setsTimestampAutomatically` | 3 (scalar) | unit | -- | yes |
| UT-E38 | `updateIntegrityGate_phaseZeroPass_preservesTestCount` | 3 (scalar) | unit | -- | yes |
| UT-E39 | `updateIntegrityGate_phaseZeroPass_preservesCoverage` | 3 (scalar) | unit | -- | yes |
| UT-E40 | `updateIntegrityGate_failWithFailedTests_storesFailedTestArray` | 4 (collection) | unit | UT-E36 | yes |
| UT-E41 | `updateIntegrityGate_multiplePhases_storesAllGates` | 4 (collection) | unit | UT-E36 | yes |
| UT-E42 | `updateIntegrityGate_returnsUpdatedExecutionState` | 3 (scalar) | unit | UT-E36 | yes |

### 4.6 updateMetrics

| ID | Test Name | TPP Level | Type | Depends On | Parallel |
|----|-----------|-----------|------|------------|----------|
| UT-E43 | `updateMetrics_nonexistentFile_throwsCheckpointIOError` | 1 (nil/degenerate) | unit | -- | yes |
| UT-E44 | `updateMetrics_updateStoriesCompleted_updatesField` | 3 (scalar) | unit | -- | yes |
| UT-E45 | `updateMetrics_updateEstimatedRemaining_updatesField` | 3 (scalar) | unit | -- | yes |
| UT-E46 | `updateMetrics_partialUpdate_mergesWithExisting` | 5 (constant+) | unit | UT-E44 | yes |
| UT-E47 | `updateMetrics_returnsUpdatedExecutionState` | 3 (scalar) | unit | UT-E44 | yes |

---

## 5. Unit Tests -- exceptions.ts (Inner Loop)

Appended to the existing `tests/node/exceptions.test.ts` file.

| ID | Test Name | TPP Level | Type | Depends On | Parallel |
|----|-----------|-----------|------|------------|----------|
| UT-X01 | `CheckpointValidationError_withFieldAndDetail_formatsMessageCorrectly` | 2 (constant) | unit | -- | yes |
| UT-X02 | `CheckpointValidationError_constructor_setsNameProperty` | 2 (constant) | unit | -- | yes |
| UT-X03 | `CheckpointValidationError_constructor_storesFieldAsReadonly` | 2 (constant) | unit | -- | yes |
| UT-X04 | `CheckpointValidationError_constructor_storesDetailAsReadonly` | 2 (constant) | unit | -- | yes |
| UT-X05 | `CheckpointValidationError_instanceof_isError` | 2 (constant) | unit | -- | yes |
| UT-X06 | `CheckpointIOError_withPathAndOperation_formatsMessageCorrectly` | 2 (constant) | unit | -- | yes |
| UT-X07 | `CheckpointIOError_constructor_setsNameProperty` | 2 (constant) | unit | -- | yes |
| UT-X08 | `CheckpointIOError_constructor_storesPathAsReadonly` | 2 (constant) | unit | -- | yes |
| UT-X09 | `CheckpointIOError_constructor_storesOperationAsReadonly` | 2 (constant) | unit | -- | yes |
| UT-X10 | `CheckpointIOError_instanceof_isError` | 2 (constant) | unit | -- | yes |

---

## 6. Integration Tests

### 6.1 Golden File Test for Template

| ID | Test Name | Type | Depends On | Parallel |
|----|-----------|------|------------|----------|
| IT-01 | `templateExecutionState_matchesGoldenFile_byteForByteParity` | integration | -- | yes |
| IT-02 | `templateExecutionState_isValidJson_parsesWithoutError` | integration | -- | yes |
| IT-03 | `templateExecutionState_containsAllRequiredFields_matchesTypeDefinition` | integration | -- | yes |
| IT-04 | `templateExecutionState_statusValues_useValidEnumValues` | integration | -- | yes |

**Golden file location:** `tests/golden/checkpoint/_TEMPLATE-EXECUTION-STATE.json`
**Template location:** `resources/templates/_TEMPLATE-EXECUTION-STATE.json`

### 6.2 Filesystem Integration (Round-Trip)

| ID | Test Name | Type | Depends On | Parallel |
|----|-----------|------|------------|----------|
| IT-05 | `checkpointRoundTrip_createThenRead_returnsEquivalentState` | integration | -- | yes |
| IT-06 | `checkpointRoundTrip_createUpdateRead_reflectsUpdate` | integration | IT-05 | yes |
| IT-07 | `checkpointRoundTrip_multipleUpdates_allChangesPersistedCorrectly` | integration | IT-05 | yes |
| IT-08 | `atomicWrite_duringWrite_noCorruptedFileOnDisk` | integration | -- | yes |
| IT-09 | `atomicWrite_afterSuccessfulWrite_tmpFileDoesNotExist` | integration | -- | yes |

---

## 7. Test Execution Strategy

### 7.1 Implementation Order (TDD Inner Loop Drives Outer Loop)

```
Phase 1: Exceptions (UT-X01..UT-X10)
  -> RED:   Write CheckpointValidationError tests
  -> GREEN: Implement CheckpointValidationError in exceptions.ts
  -> RED:   Write CheckpointIOError tests
  -> GREEN: Implement CheckpointIOError in exceptions.ts
  -> REFACTOR

Phase 2: Validation (UT-V01..UT-V46)
  -> RED:   Write degenerate/nil tests (UT-V01, UT-V02, UT-V16, UT-V24)
  -> GREEN: Implement null/undefined guards
  -> RED:   Write constant tests -- missing required fields (UT-V03..UT-V09)
  -> GREEN: Implement requireField checks
  -> RED:   Write scalar tests -- type checks (UT-V10..UT-V13, UT-V20, UT-V21)
  -> GREEN: Implement type validation
  -> RED:   Write collection tests (UT-V14, UT-V33)
  -> GREEN: Implement stories/enum iteration validation
  -> RED:   Write constant+ tests -- valid inputs (UT-V15, UT-V22, UT-V23)
  -> GREEN: All validation passes -- these should pass from accumulated logic
  -> REFACTOR

Phase 3: Engine -- createCheckpoint (UT-E01..UT-E16)
  -> RED:   Write degenerate test (UT-E01)
  -> GREEN: Implement directory existence check
  -> RED:   Write constant test (UT-E02)
  -> GREEN: Implement minimal createCheckpoint with empty stories
  -> RED:   Write scalar tests (UT-E03, UT-E08..UT-E16)
  -> GREEN: Implement full createCheckpoint with all fields
  -> RED:   Write collection tests (UT-E04..UT-E07)
  -> GREEN: Implement multi-story initialization
  -> REFACTOR

Phase 4: Engine -- readCheckpoint (UT-E17..UT-E24)
  -> RED:   Write degenerate tests (UT-E17, UT-E18)
  -> GREEN: Implement file reading with error handling
  -> RED:   Write validation delegation test (UT-E19)
  -> GREEN: Wire validation into readCheckpoint
  -> RED:   Write happy path tests (UT-E20..UT-E24)
  -> GREEN: Implement full read with type casting
  -> REFACTOR

Phase 5: Engine -- updateStoryStatus (UT-E25..UT-E34)
  -> RED:   Write degenerate tests (UT-E25, UT-E26)
  -> GREEN: Implement story ID existence check
  -> RED:   Write scalar tests (UT-E27..UT-E30, UT-E33, UT-E34)
  -> GREEN: Implement read-modify-write cycle
  -> RED:   Write collection/merge tests (UT-E31, UT-E32)
  -> GREEN: Implement partial merge with spread
  -> REFACTOR

Phase 6: Engine -- updateIntegrityGate (UT-E35..UT-E42)
  -> RED:   Write degenerate test (UT-E35)
  -> GREEN: Implement IO guard
  -> RED:   Write scalar tests (UT-E36..UT-E39, UT-E42)
  -> GREEN: Implement gate entry insertion with auto-timestamp
  -> RED:   Write collection tests (UT-E40, UT-E41)
  -> GREEN: Implement multi-phase support and failedTests array
  -> REFACTOR

Phase 7: Engine -- updateMetrics (UT-E43..UT-E47)
  -> RED:   Write degenerate test (UT-E43)
  -> GREEN: Implement IO guard
  -> RED:   Write scalar/merge tests (UT-E44..UT-E47)
  -> GREEN: Implement partial metrics merge
  -> REFACTOR

Phase 8: Acceptance Tests (AT-1..AT-8)
  -> Verify all Gherkin scenarios pass end-to-end
  -> These should pass with no new production code (all logic is in place from phases 1-7)

Phase 9: Integration Tests (IT-01..IT-09)
  -> Create golden file for template
  -> Write round-trip and atomic write tests
  -> Verify filesystem integration
```

### 7.2 File and Parallelism Notes

- **Pool:** `forks` with `maxForks: 3` (as configured in vitest)
- **Parallel:** All tests marked `Parallel: yes` can run concurrently within their file
- **Isolation:** Each test using filesystem operations creates its own `mkdtempSync` directory and cleans up in `afterEach`
- **No shared state:** Tests never depend on execution order or shared filesystem locations

### 7.3 Coverage Targets

| Module | Target Line Coverage | Target Branch Coverage |
|--------|---------------------|----------------------|
| `src/checkpoint/validation.ts` | >= 95% | >= 90% |
| `src/checkpoint/engine.ts` | >= 95% | >= 90% |
| `src/checkpoint/types.ts` | N/A (types only) | N/A (types only) |
| `src/checkpoint/index.ts` | 100% (barrel) | N/A |
| `src/exceptions.ts` (new classes) | >= 95% | >= 90% |

---

## 8. Test Count Summary

| Category | Count |
|----------|-------|
| Acceptance Tests (AT) | 8 |
| Unit Tests -- validation.ts (UT-V) | 46 |
| Unit Tests -- engine.ts (UT-E) | 47 |
| Unit Tests -- exceptions.ts (UT-X) | 10 |
| Integration Tests (IT) | 9 |
| **Total** | **120** |

---

## 9. Fixtures and Helpers

### 9.1 Shared Test Fixtures

```typescript
// tests/fixtures/checkpoint.fixture.ts

function aValidExecutionState(overrides?: Partial<Record<string, unknown>>): Record<string, unknown>;
function aValidStoryEntry(overrides?: Partial<Record<string, unknown>>): Record<string, unknown>;
function aValidIntegrityGateEntry(overrides?: Partial<Record<string, unknown>>): Record<string, unknown>;
function aValidMetrics(overrides?: Partial<Record<string, unknown>>): Record<string, unknown>;
function writeCheckpointFile(dir: string, state: Record<string, unknown>): void;
```

### 9.2 Temp Directory Helper

```typescript
// Each test file uses:
let tmpDir: string;

beforeEach(() => {
  tmpDir = mkdtempSync(join(tmpdir(), "checkpoint-test-"));
});

afterEach(() => {
  rmSync(tmpDir, { recursive: true, force: true });
});
```

---

## 10. Risk Mitigations in Tests

| Risk | Test Coverage |
|------|--------------|
| Corrupt JSON on disk | UT-E18 (invalid JSON read), IT-08 (no corruption during write) |
| Missing required fields | UT-V03..UT-V09, UT-V17..UT-V19, UT-V36..UT-V39, UT-V42..UT-V43 |
| Invalid enum values | UT-V20, UT-V24..UT-V34, UT-E24 |
| Tmp file not cleaned up | UT-E14, UT-E33, IT-09 |
| Non-existent directory | UT-E01, AT-1 |
| Non-existent story ID on update | UT-E25 |
| Partial update overwrites unrelated fields | UT-E31, UT-E32, AT-6 |
| Template drifts from types | IT-03, IT-04 |
