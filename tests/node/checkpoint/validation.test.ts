import { describe, it, expect } from "vitest";
import {
  isValidStoryStatus,
  validateExecutionState,
  validateIntegrityGateEntry,
  validateMetrics,
  validateStoryEntry,
} from "../../../src/checkpoint/validation.js";
import { CheckpointValidationError } from "../../../src/exceptions.js";
import { StoryStatus } from "../../../src/checkpoint/types.js";

// --- Helpers ---

function aValidStoryEntry(
  overrides?: Record<string, unknown>,
): Record<string, unknown> {
  return {
    status: "PENDING",
    phase: 1,
    retries: 0,
    ...overrides,
  };
}

function aValidMetrics(
  overrides?: Record<string, unknown>,
): Record<string, unknown> {
  return {
    storiesCompleted: 0,
    storiesTotal: 3,
    ...overrides,
  };
}

function aValidIntegrityGate(
  overrides?: Record<string, unknown>,
): Record<string, unknown> {
  return {
    status: "PASS",
    timestamp: "2024-01-01T00:00:00.000Z",
    testCount: 10,
    coverage: 95.0,
    ...overrides,
  };
}

function aValidExecutionState(
  overrides?: Record<string, unknown>,
): Record<string, unknown> {
  return {
    epicId: "0042",
    branch: "feat/epic-0042-full-implementation",
    startedAt: "2024-01-01T00:00:00.000Z",
    currentPhase: 0,
    mode: { parallel: false, skipReview: false },
    stories: {
      "0042-0001": aValidStoryEntry(),
    },
    integrityGates: {},
    metrics: aValidMetrics(),
    ...overrides,
  };
}

// --- 3.1 validateExecutionState ---

describe("validateExecutionState", () => {
  it("validateExecutionState_nullInput_throwsValidationError", () => {
    expect(() => validateExecutionState(null)).toThrow(
      CheckpointValidationError,
    );
  });

  it("validateExecutionState_undefinedInput_throwsValidationError", () => {
    expect(() => validateExecutionState(undefined)).toThrow(
      CheckpointValidationError,
    );
  });

  it("validateExecutionState_arrayInput_throwsValidationError", () => {
    expect(() => validateExecutionState([1, 2])).toThrow(
      CheckpointValidationError,
    );
  });

  it("validateExecutionState_primitiveInput_throwsValidationError", () => {
    expect(() => validateExecutionState("hello")).toThrow(
      CheckpointValidationError,
    );
  });

  it("validateExecutionState_emptyObject_throwsWithEpicIdRequired", () => {
    expect(() => validateExecutionState({})).toThrow(
      /epicId/,
    );
  });

  it("validateExecutionState_missingBranch_throwsWithBranchRequired", () => {
    expect(() =>
      validateExecutionState(
        aValidExecutionState({ branch: undefined }),
      ),
    ).toThrow(/branch/);
  });

  it("validateExecutionState_missingStartedAt_throwsWithStartedAtRequired", () => {
    expect(() =>
      validateExecutionState(
        aValidExecutionState({ startedAt: undefined }),
      ),
    ).toThrow(/startedAt/);
  });

  it("validateExecutionState_missingCurrentPhase_throwsWithCurrentPhaseRequired", () => {
    expect(() =>
      validateExecutionState(
        aValidExecutionState({ currentPhase: undefined }),
      ),
    ).toThrow(/currentPhase/);
  });

  it("validateExecutionState_missingMode_throwsWithModeRequired", () => {
    expect(() =>
      validateExecutionState(
        aValidExecutionState({ mode: undefined }),
      ),
    ).toThrow(/mode/);
  });

  it("validateExecutionState_missingStories_throwsWithStoriesRequired", () => {
    expect(() =>
      validateExecutionState(
        aValidExecutionState({ stories: undefined }),
      ),
    ).toThrow(/stories/);
  });

  it("validateExecutionState_missingMetrics_throwsWithMetricsRequired", () => {
    expect(() =>
      validateExecutionState(
        aValidExecutionState({ metrics: undefined }),
      ),
    ).toThrow(/metrics/);
  });

  it("validateExecutionState_epicIdWrongType_throwsWithTypeError", () => {
    expect(() =>
      validateExecutionState(
        aValidExecutionState({ epicId: 42 }),
      ),
    ).toThrow(CheckpointValidationError);
  });

  it("validateExecutionState_currentPhaseNotNumber_throwsWithTypeError", () => {
    expect(() =>
      validateExecutionState(
        aValidExecutionState({ currentPhase: "zero" }),
      ),
    ).toThrow(CheckpointValidationError);
  });

  it("validateExecutionState_modeParallelNotBoolean_throwsWithTypeError", () => {
    expect(() =>
      validateExecutionState(
        aValidExecutionState({
          mode: { parallel: "yes", skipReview: false },
        }),
      ),
    ).toThrow(CheckpointValidationError);
  });

  it("validateExecutionState_modeSkipReviewNotBoolean_throwsWithTypeError", () => {
    expect(() =>
      validateExecutionState(
        aValidExecutionState({
          mode: { parallel: false, skipReview: "no" },
        }),
      ),
    ).toThrow(CheckpointValidationError);
  });

  it("validateExecutionState_storiesNotObject_throwsWithTypeError", () => {
    expect(() =>
      validateExecutionState(
        aValidExecutionState({ stories: "not-an-object" }),
      ),
    ).toThrow(CheckpointValidationError);
  });

  it("validateExecutionState_validCompleteState_doesNotThrow", () => {
    expect(() =>
      validateExecutionState(aValidExecutionState()),
    ).not.toThrow();
  });

  it("validateExecutionState_validState_returnsTypedState", () => {
    const result = validateExecutionState(
      aValidExecutionState(),
    );
    expect(result.epicId).toBe("0042");
    expect(result.branch).toBe(
      "feat/epic-0042-full-implementation",
    );
    expect(result.currentPhase).toBe(0);
  });

  it("validateExecutionState_missingIntegrityGates_throwsWithIntegrityGatesRequired", () => {
    const state = aValidExecutionState();
    delete (state as Record<string, unknown>)["integrityGates"];
    expect(() => validateExecutionState(state)).toThrow(
      /integrityGates/,
    );
  });
});

// --- 3.2 validateStoryEntry ---

describe("validateStoryEntry", () => {
  it("validateStoryEntry_nullEntry_throwsValidationError", () => {
    expect(() =>
      validateStoryEntry(null, "0042-0001"),
    ).toThrow(CheckpointValidationError);
  });

  it("validateStoryEntry_missingStatus_throwsWithStatusRequired", () => {
    expect(() =>
      validateStoryEntry(
        aValidStoryEntry({ status: undefined }),
        "0042-0001",
      ),
    ).toThrow(/status/);
  });

  it("validateStoryEntry_missingPhase_throwsWithPhaseRequired", () => {
    expect(() =>
      validateStoryEntry(
        aValidStoryEntry({ phase: undefined }),
        "0042-0001",
      ),
    ).toThrow(/phase/);
  });

  it("validateStoryEntry_missingRetries_throwsWithRetriesRequired", () => {
    expect(() =>
      validateStoryEntry(
        aValidStoryEntry({ retries: undefined }),
        "0042-0001",
      ),
    ).toThrow(/retries/);
  });

  it("validateStoryEntry_invalidStatusEnum_throwsWithInvalidStatus", () => {
    expect(() =>
      validateStoryEntry(
        aValidStoryEntry({ status: "UNKNOWN" }),
        "0042-0001",
      ),
    ).toThrow(CheckpointValidationError);
  });

  it("validateStoryEntry_retriesNotNumber_throwsWithTypeError", () => {
    expect(() =>
      validateStoryEntry(
        aValidStoryEntry({ retries: "zero" }),
        "0042-0001",
      ),
    ).toThrow(CheckpointValidationError);
  });

  it("validateStoryEntry_validEntryWithOptionalFields_doesNotThrow", () => {
    expect(() =>
      validateStoryEntry(
        aValidStoryEntry({
          commitSha: "abc123",
          duration: "5m",
          summary: "done",
          findingsCount: 2,
          blockedBy: ["0042-0002"],
        }),
        "0042-0001",
      ),
    ).not.toThrow();
  });

  it("validateStoryEntry_validMinimalEntry_doesNotThrow", () => {
    expect(() =>
      validateStoryEntry(aValidStoryEntry(), "0042-0001"),
    ).not.toThrow();
  });

  it("validateStoryEntry_phaseNotNumber_throwsWithTypeError", () => {
    expect(() =>
      validateStoryEntry(
        aValidStoryEntry({ phase: "one" }),
        "0042-0001",
      ),
    ).toThrow(CheckpointValidationError);
  });

  it("validateStoryEntry_stringInput_throwsValidationError", () => {
    expect(() =>
      validateStoryEntry("not-an-object", "0042-0001"),
    ).toThrow(CheckpointValidationError);
  });

  it("validateStoryEntry_arrayInput_throwsValidationError", () => {
    expect(() =>
      validateStoryEntry([1, 2], "0042-0001"),
    ).toThrow(CheckpointValidationError);
  });

  it("validateStoryEntry_commitShaNotString_throwsValidationError", () => {
    expect(() =>
      validateStoryEntry(
        aValidStoryEntry({ commitSha: 123 }),
        "0042-0001",
      ),
    ).toThrow(CheckpointValidationError);
  });

  it("validateStoryEntry_durationNotString_throwsValidationError", () => {
    expect(() =>
      validateStoryEntry(
        aValidStoryEntry({ duration: true }),
        "0042-0001",
      ),
    ).toThrow(CheckpointValidationError);
  });

  it("validateStoryEntry_summaryNotString_throwsValidationError", () => {
    expect(() =>
      validateStoryEntry(
        aValidStoryEntry({ summary: 42 }),
        "0042-0001",
      ),
    ).toThrow(CheckpointValidationError);
  });

  it("validateStoryEntry_findingsCountNotNumber_throwsValidationError", () => {
    expect(() =>
      validateStoryEntry(
        aValidStoryEntry({ findingsCount: "two" }),
        "0042-0001",
      ),
    ).toThrow(CheckpointValidationError);
  });

  it("validateStoryEntry_blockedByNotArray_throwsValidationError", () => {
    expect(() =>
      validateStoryEntry(
        aValidStoryEntry({ blockedBy: "0042-0002" }),
        "0042-0001",
      ),
    ).toThrow(CheckpointValidationError);
  });

  it("validateStoryEntry_blockedByWithNonString_throwsValidationError", () => {
    expect(() =>
      validateStoryEntry(
        aValidStoryEntry({ blockedBy: [123] }),
        "0042-0001",
      ),
    ).toThrow(CheckpointValidationError);
  });
});

// --- 3.3 isValidStoryStatus ---

describe("isValidStoryStatus", () => {
  it("isValidStoryStatus_nullValue_returnsFalse", () => {
    expect(isValidStoryStatus(null)).toBe(false);
  });

  it("isValidStoryStatus_emptyString_returnsFalse", () => {
    expect(isValidStoryStatus("")).toBe(false);
  });

  it("isValidStoryStatus_unknownValue_returnsFalse", () => {
    expect(isValidStoryStatus("UNKNOWN")).toBe(false);
  });

  it("isValidStoryStatus_pendingValue_returnsTrue", () => {
    expect(isValidStoryStatus("PENDING")).toBe(true);
  });

  it("isValidStoryStatus_inProgressValue_returnsTrue", () => {
    expect(isValidStoryStatus("IN_PROGRESS")).toBe(true);
  });

  it("isValidStoryStatus_successValue_returnsTrue", () => {
    expect(isValidStoryStatus("SUCCESS")).toBe(true);
  });

  it("isValidStoryStatus_failedValue_returnsTrue", () => {
    expect(isValidStoryStatus("FAILED")).toBe(true);
  });

  it("isValidStoryStatus_blockedValue_returnsTrue", () => {
    expect(isValidStoryStatus("BLOCKED")).toBe(true);
  });

  it("isValidStoryStatus_partialValue_returnsTrue", () => {
    expect(isValidStoryStatus("PARTIAL")).toBe(true);
  });

  it("isValidStoryStatus_allValidValues_eachReturnsTrue", () => {
    const values = Object.values(StoryStatus);
    for (const v of values) {
      expect(isValidStoryStatus(v)).toBe(true);
    }
  });

  it("isValidStoryStatus_lowercasePending_returnsFalse", () => {
    expect(isValidStoryStatus("pending")).toBe(false);
  });
});

// --- 3.4 validateIntegrityGateEntry ---

describe("validateIntegrityGateEntry", () => {
  it("validateIntegrityGateEntry_nullEntry_throwsValidationError", () => {
    expect(() =>
      validateIntegrityGateEntry(null, "phase-0"),
    ).toThrow(CheckpointValidationError);
  });

  it("validateIntegrityGateEntry_missingStatus_throwsWithStatusRequired", () => {
    expect(() =>
      validateIntegrityGateEntry(
        aValidIntegrityGate({ status: undefined }),
        "phase-0",
      ),
    ).toThrow(/status/);
  });

  it("validateIntegrityGateEntry_invalidStatus_throwsWithInvalidGateStatus", () => {
    expect(() =>
      validateIntegrityGateEntry(
        aValidIntegrityGate({ status: "MAYBE" }),
        "phase-0",
      ),
    ).toThrow(CheckpointValidationError);
  });

  it("validateIntegrityGateEntry_missingTestCount_throwsWithTestCountRequired", () => {
    expect(() =>
      validateIntegrityGateEntry(
        aValidIntegrityGate({ testCount: undefined }),
        "phase-0",
      ),
    ).toThrow(/testCount/);
  });

  it("validateIntegrityGateEntry_missingCoverage_throwsWithCoverageRequired", () => {
    expect(() =>
      validateIntegrityGateEntry(
        aValidIntegrityGate({ coverage: undefined }),
        "phase-0",
      ),
    ).toThrow(/coverage/);
  });

  it("validateIntegrityGateEntry_validEntry_doesNotThrow", () => {
    expect(() =>
      validateIntegrityGateEntry(
        aValidIntegrityGate(),
        "phase-0",
      ),
    ).not.toThrow();
  });

  it("validateIntegrityGateEntry_missingTimestamp_throwsWithTimestampRequired", () => {
    expect(() =>
      validateIntegrityGateEntry(
        aValidIntegrityGate({ timestamp: undefined }),
        "phase-0",
      ),
    ).toThrow(/timestamp/);
  });

  it("validateIntegrityGateEntry_stringInput_throwsValidationError", () => {
    expect(() =>
      validateIntegrityGateEntry("not-an-object", "phase-0"),
    ).toThrow(CheckpointValidationError);
  });

  it("validateIntegrityGateEntry_arrayInput_throwsValidationError", () => {
    expect(() =>
      validateIntegrityGateEntry([1, 2], "phase-0"),
    ).toThrow(CheckpointValidationError);
  });

  it("validateIntegrityGateEntry_failedTestsNotArray_throwsValidationError", () => {
    expect(() =>
      validateIntegrityGateEntry(
        aValidIntegrityGate({ failedTests: "test1" }),
        "phase-0",
      ),
    ).toThrow(CheckpointValidationError);
  });

  it("validateIntegrityGateEntry_failedTestsWithNonString_throwsValidationError", () => {
    expect(() =>
      validateIntegrityGateEntry(
        aValidIntegrityGate({ failedTests: [123] }),
        "phase-0",
      ),
    ).toThrow(CheckpointValidationError);
  });

  it("validateIntegrityGateEntry_validWithFailedTests_doesNotThrow", () => {
    expect(() =>
      validateIntegrityGateEntry(
        aValidIntegrityGate({
          status: "FAIL",
          failedTests: ["test1", "test2"],
        }),
        "phase-0",
      ),
    ).not.toThrow();
  });
});

// --- 3.5 validateMetrics ---

describe("validateMetrics", () => {
  it("validateMetrics_nullInput_throwsValidationError", () => {
    expect(() => validateMetrics(null)).toThrow(
      CheckpointValidationError,
    );
  });

  it("validateMetrics_missingStoriesCompleted_throwsRequired", () => {
    expect(() =>
      validateMetrics(
        aValidMetrics({ storiesCompleted: undefined }),
      ),
    ).toThrow(/storiesCompleted/);
  });

  it("validateMetrics_missingStoriesTotal_throwsRequired", () => {
    expect(() =>
      validateMetrics(
        aValidMetrics({ storiesTotal: undefined }),
      ),
    ).toThrow(/storiesTotal/);
  });

  it("validateMetrics_storiesCompletedNotNumber_throwsTypeError", () => {
    expect(() =>
      validateMetrics(
        aValidMetrics({ storiesCompleted: "zero" }),
      ),
    ).toThrow(CheckpointValidationError);
  });

  it("validateMetrics_validMetrics_doesNotThrow", () => {
    expect(() =>
      validateMetrics(aValidMetrics()),
    ).not.toThrow();
  });

  it("validateMetrics_validMetricsWithOptionalEstimated_doesNotThrow", () => {
    expect(() =>
      validateMetrics(
        aValidMetrics({ estimatedRemainingMinutes: 30 }),
      ),
    ).not.toThrow();
  });

  it("validateMetrics_storiesTotalNotNumber_throwsTypeError", () => {
    expect(() =>
      validateMetrics(
        aValidMetrics({ storiesTotal: "five" }),
      ),
    ).toThrow(CheckpointValidationError);
  });

  it("validateMetrics_stringInput_throwsValidationError", () => {
    expect(() => validateMetrics("not-an-object")).toThrow(
      CheckpointValidationError,
    );
  });

  it("validateMetrics_arrayInput_throwsValidationError", () => {
    expect(() => validateMetrics([1, 2])).toThrow(
      CheckpointValidationError,
    );
  });

  it("validateMetrics_estimatedRemainingMinutesNotNumber_throwsValidationError", () => {
    expect(() =>
      validateMetrics(
        aValidMetrics({
          estimatedRemainingMinutes: "thirty",
        }),
      ),
    ).toThrow(CheckpointValidationError);
  });
});
