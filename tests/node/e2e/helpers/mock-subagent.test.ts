import { describe, expect, it } from "vitest";

import type { SubagentResult } from "../../../../src/checkpoint/types.js";
import {
  createMockDispatch,
  FAILED_RESULT,
  SUCCESS_RESULT,
} from "./mock-subagent.js";

describe("createMockDispatch", () => {
  it("dispatch_configuredStory_returnsConfiguredResult", () => {
    const { dispatch } = createMockDispatch({
      results: { "story-0001": SUCCESS_RESULT },
    });
    const result = dispatch("story-0001");
    expect(result.status).toBe("SUCCESS");
    expect(result.summary).toBe("Mock success");
  });

  it("dispatch_unconfiguredStoryWithDefault_returnsDefault", () => {
    const { dispatch } = createMockDispatch({
      results: {},
      defaultResult: SUCCESS_RESULT,
    });
    const result = dispatch("story-unknown");
    expect(result.status).toBe("SUCCESS");
  });

  it("dispatch_unconfiguredStoryNoDefault_throws", () => {
    const { dispatch } = createMockDispatch({ results: {} });
    expect(() => dispatch("story-missing")).toThrow(
      'No mock result configured for story "story-missing"',
    );
  });

  it("dispatch_arrayResults_consumesSequentially", () => {
    const { dispatch } = createMockDispatch({
      results: {
        "story-0001": [FAILED_RESULT, FAILED_RESULT, SUCCESS_RESULT],
      },
    });
    expect(dispatch("story-0001").status).toBe("FAILED");
    expect(dispatch("story-0001").status).toBe("FAILED");
    expect(dispatch("story-0001").status).toBe("SUCCESS");
  });

  it("dispatch_arrayExhausted_repeatsLastElement", () => {
    const { dispatch } = createMockDispatch({
      results: { "story-0001": [FAILED_RESULT, SUCCESS_RESULT] },
    });
    dispatch("story-0001");
    dispatch("story-0001");
    const third = dispatch("story-0001");
    expect(third.status).toBe("SUCCESS");
  });

  it("dispatch_callLog_recordsAllDispatches", () => {
    const { dispatch, callLog } = createMockDispatch({
      results: {},
      defaultResult: SUCCESS_RESULT,
    });
    dispatch("story-0001");
    dispatch("story-0002");
    dispatch("story-0001");

    expect(callLog).toHaveLength(3);
    expect(callLog[0]).toEqual({ storyId: "story-0001", attempt: 1 });
    expect(callLog[1]).toEqual({ storyId: "story-0002", attempt: 1 });
    expect(callLog[2]).toEqual({ storyId: "story-0001", attempt: 2 });
  });

  it("dispatch_attemptCounter_incrementsPerStory", () => {
    const { dispatch, callLog } = createMockDispatch({
      results: {
        "story-0001": [FAILED_RESULT, FAILED_RESULT, SUCCESS_RESULT],
      },
    });
    dispatch("story-0001");
    dispatch("story-0001");
    dispatch("story-0001");

    expect(callLog[0].attempt).toBe(1);
    expect(callLog[1].attempt).toBe(2);
    expect(callLog[2].attempt).toBe(3);
  });
});

describe("constant results", () => {
  it("SUCCESS_RESULT_hasExpectedShape", () => {
    expect(SUCCESS_RESULT.status).toBe("SUCCESS");
    expect(SUCCESS_RESULT.findingsCount).toBe(0);
    expect(SUCCESS_RESULT.commitSha).toBeDefined();
  });

  it("FAILED_RESULT_hasExpectedShape", () => {
    expect(FAILED_RESULT.status).toBe("FAILED");
    expect(FAILED_RESULT.findingsCount).toBe(1);
    expect(FAILED_RESULT.commitSha).toBeUndefined();
  });
});
