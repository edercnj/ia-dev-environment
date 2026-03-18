import { describe, it, expect } from "vitest";
import {
  calculateAverageStoryDuration,
  calculateEstimatedRemaining,
  buildMetricsUpdate,
} from "../../../src/progress/metrics-calculator.js";
import type { BuildMetricsParams } from "../../../src/progress/types.js";

// --- Helpers ---

function aFullParams(
  overrides?: Partial<BuildMetricsParams>,
): BuildMetricsParams {
  return {
    storiesCompleted: 6,
    storiesTotal: 14,
    storiesFailed: 1,
    storiesBlocked: 1,
    elapsedMs: 900000,
    storyDurations: new Map([
      ["0001", 150000],
      ["0002", 150000],
      ["0003", 150000],
      ["0004", 150000],
      ["0005", 150000],
      ["0006", 150000],
    ]),
    phaseDurations: new Map([
      [0, 300000],
      [1, 400000],
    ]),
    ...overrides,
  };
}

// --- calculateAverageStoryDuration ---

describe("calculateAverageStoryDuration", () => {
  it("calculateAverageStoryDuration_emptyMap_returnsUndefined", () => {
    expect(calculateAverageStoryDuration(new Map())).toBeUndefined();
  });

  it("calculateAverageStoryDuration_singleEntry_returnsThatValue", () => {
    const durations = new Map([["0001", 150000]]);
    expect(calculateAverageStoryDuration(durations)).toBe(150000);
  });

  it("calculateAverageStoryDuration_multipleEntries_returnsCorrectMean", () => {
    const durations = new Map([
      ["0001", 100000],
      ["0002", 200000],
      ["0003", 150000],
    ]);
    expect(calculateAverageStoryDuration(durations)).toBe(150000);
  });

  it("calculateAverageStoryDuration_largeSpread_computesCorrectly", () => {
    const durations = new Map([
      ["0001", 10000],
      ["0002", 500000],
    ]);
    expect(calculateAverageStoryDuration(durations)).toBe(255000);
  });
});

// --- calculateEstimatedRemaining ---

describe("calculateEstimatedRemaining", () => {
  it("calculateEstimatedRemaining_zeroPending_returnsZero", () => {
    expect(calculateEstimatedRemaining(0, 150000)).toBe(0);
  });

  it("calculateEstimatedRemaining_pendingWithAverage_returnsCorrectEstimate", () => {
    expect(calculateEstimatedRemaining(10, 150000)).toBe(1500000);
  });

  it("calculateEstimatedRemaining_undefinedAverage_returnsUndefined", () => {
    expect(calculateEstimatedRemaining(10, undefined)).toBeUndefined();
  });

  it("calculateEstimatedRemaining_negativePending_returnsZero", () => {
    expect(calculateEstimatedRemaining(-1, 150000)).toBe(0);
  });
});

// --- buildMetricsUpdate ---

describe("buildMetricsUpdate", () => {
  it("buildMetricsUpdate_fullParams_returnsCompleteMetricsUpdate", () => {
    const result = buildMetricsUpdate(aFullParams());
    expect(result.storiesCompleted).toBe(6);
    expect(result.storiesTotal).toBe(14);
    expect(result.storiesFailed).toBe(1);
    expect(result.storiesBlocked).toBe(1);
    expect(result.elapsedMs).toBe(900000);
    expect(result.averageStoryDurationMs).toBe(150000);
    expect(result.estimatedRemainingMs).toBe(1200000);
    expect(result.estimatedRemainingMinutes).toBe(20);
    expect(result.storyDurations).toEqual({
      "0001": 150000,
      "0002": 150000,
      "0003": 150000,
      "0004": 150000,
      "0005": 150000,
      "0006": 150000,
    });
    expect(result.phaseDurations).toEqual({
      "0": 300000,
      "1": 400000,
    });
  });

  it("buildMetricsUpdate_noCompletedStories_returnsUndefinedForAveragesAndEstimates", () => {
    const result = buildMetricsUpdate(
      aFullParams({
        storiesCompleted: 0,
        storiesFailed: 0,
        storiesBlocked: 0,
        elapsedMs: 0,
        storyDurations: new Map(),
        phaseDurations: new Map(),
      }),
    );
    expect(result.averageStoryDurationMs).toBeUndefined();
    expect(result.estimatedRemainingMs).toBeUndefined();
    expect(result.estimatedRemainingMinutes).toBeUndefined();
  });

  it("buildMetricsUpdate_allStoriesComplete_estimatedRemainingIsZero", () => {
    const durations = new Map<string, number>();
    for (let i = 1; i <= 14; i++) {
      durations.set(String(i).padStart(4, "0"), 150000);
    }
    const result = buildMetricsUpdate(
      aFullParams({
        storiesCompleted: 14,
        storiesTotal: 14,
        storiesFailed: 0,
        storiesBlocked: 0,
        elapsedMs: 2100000,
        storyDurations: durations,
      }),
    );
    expect(result.estimatedRemainingMs).toBe(0);
  });

  it("buildMetricsUpdate_phaseDurationsMapWithNumberKeys_serializesToStringKeys", () => {
    const result = buildMetricsUpdate(aFullParams());
    expect(result.phaseDurations).toEqual({
      "0": 300000,
      "1": 400000,
    });
  });

  it("buildMetricsUpdate_estimatedRemainingMinutes_derivedFromMs", () => {
    const result = buildMetricsUpdate(aFullParams());
    expect(result.estimatedRemainingMinutes).toBe(
      Math.round(1200000 / 60000),
    );
  });
});
