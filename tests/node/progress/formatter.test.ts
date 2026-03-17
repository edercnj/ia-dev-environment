import { describe, it, expect } from "vitest";
import {
  formatDuration,
  formatPhaseStart,
  formatStoryStart,
  formatStoryComplete,
  formatGateResult,
  formatRetry,
  formatBlock,
  formatEpicComplete,
  formatProgressSummary,
} from "../../../src/progress/formatter.js";
import type {
  PhaseStartEvent,
  StoryStartEvent,
  StoryCompleteEvent,
  GateResultEvent,
  RetryEvent,
  BlockEvent,
  EpicCompleteEvent,
  StoryCompleteContext,
} from "../../../src/progress/types.js";
import { ProgressEventType } from "../../../src/progress/types.js";

// --- Helpers ---

function aPhaseStartEvent(
  overrides?: Partial<PhaseStartEvent>,
): PhaseStartEvent {
  return {
    type: ProgressEventType.PHASE_START,
    phase: 0,
    totalPhases: 4,
    phaseName: "Foundation",
    storiesCount: 3,
    ...overrides,
  };
}

function aStoryStartEvent(
  overrides?: Partial<StoryStartEvent>,
): StoryStartEvent {
  return {
    type: ProgressEventType.STORY_START,
    storyId: "0042-0001",
    phase: 0,
    storyIndex: 1,
    storiesTotal: 14,
    ...overrides,
  };
}

function aStoryCompleteEvent(
  overrides?: Partial<StoryCompleteEvent>,
): StoryCompleteEvent {
  return {
    type: ProgressEventType.STORY_COMPLETE,
    storyId: "0042-0001",
    status: "SUCCESS",
    durationMs: 154000,
    commitSha: "abc123",
    ...overrides,
  };
}

function aGateResultEvent(
  overrides?: Partial<GateResultEvent>,
): GateResultEvent {
  return {
    type: ProgressEventType.GATE_RESULT,
    phase: 0,
    status: "PASS",
    testCount: 42,
    coverage: 96.3,
    ...overrides,
  };
}

function aRetryEvent(
  overrides?: Partial<RetryEvent>,
): RetryEvent {
  return {
    type: ProgressEventType.RETRY,
    storyId: "0042-0003",
    retryNumber: 1,
    maxRetries: 2,
    previousError: "test failure",
    ...overrides,
  };
}

function aBlockEvent(
  overrides?: Partial<BlockEvent>,
): BlockEvent {
  return {
    type: ProgressEventType.BLOCK,
    storyId: "0042-0003",
    blockedStories: ["0042-0004", "0042-0005"],
    ...overrides,
  };
}

function anEpicCompleteEvent(
  overrides?: Partial<EpicCompleteEvent>,
): EpicCompleteEvent {
  return {
    type: ProgressEventType.EPIC_COMPLETE,
    storiesCompleted: 14,
    storiesTotal: 14,
    storiesFailed: 0,
    storiesBlocked: 0,
    elapsedMs: 2700000,
    retryCount: 2,
    ...overrides,
  };
}

// --- formatDuration ---

describe("formatDuration", () => {
  it("formatDuration_zeroMs_returnsZeroSeconds", () => {
    expect(formatDuration(0)).toBe("0s");
  });

  it("formatDuration_secondsOnly_returnsSecondsFormat", () => {
    expect(formatDuration(45000)).toBe("45s");
  });

  it("formatDuration_minutesAndSeconds_returnsMinutesFormat", () => {
    expect(formatDuration(154000)).toBe("2m 34s");
  });

  it("formatDuration_exactMinute_returnsMinuteAndZeroSeconds", () => {
    expect(formatDuration(60000)).toBe("1m 0s");
  });

  it("formatDuration_hoursMinutesSeconds_returnsHoursFormat", () => {
    expect(formatDuration(3661000)).toBe("1h 1m 1s");
  });

  it("formatDuration_exactHour_returnsHourAndZeros", () => {
    expect(formatDuration(3600000)).toBe("1h 0m 0s");
  });

  it("formatDuration_subSecond_returnsZeroSeconds", () => {
    expect(formatDuration(500)).toBe("0s");
  });

  it("formatDuration_largeValue_returnsCorrectHours", () => {
    expect(formatDuration(86400000)).toBe("24h 0m 0s");
  });

  it("formatDuration_negativeValue_returnsZeroSeconds", () => {
    expect(formatDuration(-1000)).toBe("0s");
  });
});

// --- formatPhaseStart ---

describe("formatPhaseStart", () => {
  it("formatPhaseStart_basicPhaseAndCount_returnsCorrectBanner", () => {
    const result = formatPhaseStart(aPhaseStartEvent());
    expect(result).toContain("Phase 0/4");
    expect(result).toContain("Foundation");
    expect(result).toContain("3 stories");
  });

  it("formatPhaseStart_singularStory_usesStorySingular", () => {
    const result = formatPhaseStart(
      aPhaseStartEvent({
        phase: 3,
        totalPhases: 4,
        phaseName: "Integration",
        storiesCount: 1,
      }),
    );
    expect(result).toContain("1 story");
    expect(result).not.toContain("1 stories");
  });
});

// --- formatStoryStart ---

describe("formatStoryStart", () => {
  it("formatStoryStart_storyIndexAndTotal_returnsCorrectPrefix", () => {
    const result = formatStoryStart(aStoryStartEvent());
    expect(result).toContain("[1/14]");
    expect(result).toContain("story-0042-0001");
  });
});

// --- formatStoryComplete ---

describe("formatStoryComplete", () => {
  it("formatStoryComplete_successWithCommitSha_returnsSuccessLine", () => {
    const ctx: StoryCompleteContext = { storyIndex: 1, storiesTotal: 14 };
    const result = formatStoryComplete(aStoryCompleteEvent(), ctx);
    expect(result).toContain("SUCCESS");
    expect(result).toContain("2m 34s");
    expect(result).toContain("abc123");
  });

  it("formatStoryComplete_successWithoutCommitSha_omitsSha", () => {
    const ctx: StoryCompleteContext = { storyIndex: 1, storiesTotal: 14 };
    const event = aStoryCompleteEvent({ commitSha: undefined });
    const result = formatStoryComplete(event, ctx);
    expect(result).toContain("SUCCESS");
    expect(result).toContain("2m 34s");
    expect(result).not.toMatch(/\[[\da-f]+\]/);
  });

  it("formatStoryComplete_failed_returnsFailedLine", () => {
    const ctx: StoryCompleteContext = { storyIndex: 3, storiesTotal: 14 };
    const event = aStoryCompleteEvent({
      storyId: "0042-0003",
      status: "FAILED",
      durationMs: 192000,
      commitSha: undefined,
    });
    const result = formatStoryComplete(event, ctx);
    expect(result).toContain("FAILED");
    expect(result).toContain("3m 12s");
  });

  it("formatStoryComplete_partial_returnsPartialLine", () => {
    const ctx: StoryCompleteContext = { storyIndex: 2, storiesTotal: 14 };
    const event = aStoryCompleteEvent({
      storyId: "0042-0002",
      status: "PARTIAL",
      durationMs: 80000,
      commitSha: undefined,
    });
    const result = formatStoryComplete(event, ctx);
    expect(result).toContain("PARTIAL");
    expect(result).toContain("1m 20s");
  });
});

// --- formatRetry ---

describe("formatRetry", () => {
  it("formatRetry_retryNumberAndError_returnsRetryLine", () => {
    const result = formatRetry(aRetryEvent());
    expect(result).toContain("retry 1/2");
    expect(result).toContain("test failure");
  });
});

// --- formatGateResult ---

describe("formatGateResult", () => {
  it("formatGateResult_pass_returnsPassLine", () => {
    const result = formatGateResult(aGateResultEvent());
    expect(result).toContain("Gate Phase 0");
    expect(result).toContain("PASS");
    expect(result).toContain("42 tests");
    expect(result).toContain("96.3% coverage");
  });

  it("formatGateResult_fail_returnsFailLine", () => {
    const result = formatGateResult(
      aGateResultEvent({ status: "FAIL", coverage: 80.0 }),
    );
    expect(result).toContain("FAIL");
    expect(result).toContain("80.0% coverage");
  });
});

// --- formatBlock ---

describe("formatBlock", () => {
  it("formatBlock_storyIdAndBlockedStories_returnsBlockLine", () => {
    const result = formatBlock(aBlockEvent());
    expect(result).toContain("0042-0003");
    expect(result).toContain("0042-0004");
    expect(result).toContain("0042-0005");
  });
});

// --- formatEpicComplete ---

describe("formatEpicComplete", () => {
  it("formatEpicComplete_fullStats_returnsSummaryLine", () => {
    const result = formatEpicComplete(anEpicCompleteEvent());
    expect(result).toContain("Epic Complete");
    expect(result).toContain("14/14");
    expect(result).toContain("100%");
    expect(result).toContain("45m 0s");
  });

  it("formatEpicComplete_partialCompletion_includesFailureAndBlockCounts", () => {
    const result = formatEpicComplete(
      anEpicCompleteEvent({
        storiesCompleted: 10,
        storiesTotal: 14,
        storiesFailed: 2,
        storiesBlocked: 2,
        elapsedMs: 1800000,
        retryCount: 3,
      }),
    );
    expect(result).toContain("10/14");
    expect(result).toContain("71.4%");
  });
});

// --- formatProgressSummary ---

describe("formatProgressSummary", () => {
  it("formatProgressSummary_withEstimatedRemaining_returnsFullSummary", () => {
    const result = formatProgressSummary(6, 14, 881000, 1200000);
    expect(result).toContain("6/14");
    expect(result).toContain("42.9%");
    expect(result).toContain("14m 41s");
    expect(result).toContain("Est. remaining: ~20m");
  });

  it("formatProgressSummary_withoutEstimatedRemaining_omitsEstimate", () => {
    const result = formatProgressSummary(2, 14, 300000, undefined);
    expect(result).toContain("2/14");
    expect(result).toContain("14.3%");
    expect(result).not.toContain("Est. remaining");
  });
});
