import { describe, it, expect } from "vitest";

import { evaluateRetry } from "../../../src/domain/failure/retry-evaluator.js";
import { MAX_RETRIES } from "../../../src/domain/failure/types.js";
import type { RetryDecision } from "../../../src/domain/failure/types.js";

const STORY_ID = "story-0042-0001";
const PREVIOUS_ERROR = "compilation failed: missing export";
const BRANCH_NAME = "feat/epic-0042";

describe("evaluateRetry", () => {
  // --- Cycle 2.1: Budget exhausted (UT-1, UT-2, UT-9) ---

  it("evaluateRetry_retriesAtMax_returnsShouldRetryFalse", () => {
    const result: RetryDecision = evaluateRetry(
      STORY_ID,
      MAX_RETRIES,
      PREVIOUS_ERROR,
      BRANCH_NAME,
    );

    expect(result.shouldRetry).toBe(false);
  });

  it("evaluateRetry_retriesAboveMax_returnsShouldRetryFalse", () => {
    const result: RetryDecision = evaluateRetry(
      STORY_ID,
      MAX_RETRIES + 1,
      PREVIOUS_ERROR,
      BRANCH_NAME,
    );

    expect(result.shouldRetry).toBe(false);
  });

  it("evaluateRetry_budgetExhausted_reasonIsBudgetExhausted", () => {
    const result: RetryDecision = evaluateRetry(
      STORY_ID,
      MAX_RETRIES,
      PREVIOUS_ERROR,
      BRANCH_NAME,
    );

    expect(result).toEqual({
      shouldRetry: false,
      reason: "budget_exhausted",
    });
  });

  // --- Cycle 2.2: Retry allowed with context (UT-3 through UT-8) ---

  it("evaluateRetry_retriesZero_returnsShouldRetryTrue", () => {
    const result = evaluateRetry(STORY_ID, 0, PREVIOUS_ERROR, BRANCH_NAME);

    expect(result.shouldRetry).toBe(true);
  });

  it("evaluateRetry_retriesOne_returnsShouldRetryTrue", () => {
    const result = evaluateRetry(STORY_ID, 1, PREVIOUS_ERROR, BRANCH_NAME);

    expect(result.shouldRetry).toBe(true);
  });

  it("evaluateRetry_retriesZero_retryContextHasRetryNumberOne", () => {
    const result = evaluateRetry(STORY_ID, 0, PREVIOUS_ERROR, BRANCH_NAME);

    expect(result.shouldRetry).toBe(true);
    if (result.shouldRetry) {
      expect(result.retryContext.retryNumber).toBe(1);
    }
  });

  it("evaluateRetry_retriesZero_retryContextContainsPreviousError", () => {
    const result = evaluateRetry(STORY_ID, 0, PREVIOUS_ERROR, BRANCH_NAME);

    expect(result.shouldRetry).toBe(true);
    if (result.shouldRetry) {
      expect(result.retryContext.previousError).toBe(PREVIOUS_ERROR);
    }
  });

  it("evaluateRetry_retriesZero_retryContextContainsStoryId", () => {
    const result = evaluateRetry(STORY_ID, 0, PREVIOUS_ERROR, BRANCH_NAME);

    expect(result.shouldRetry).toBe(true);
    if (result.shouldRetry) {
      expect(result.retryContext.storyId).toBe(STORY_ID);
    }
  });

  it("evaluateRetry_retriesZero_retryContextContainsBranchName", () => {
    const result = evaluateRetry(STORY_ID, 0, PREVIOUS_ERROR, BRANCH_NAME);

    expect(result.shouldRetry).toBe(true);
    if (result.shouldRetry) {
      expect(result.retryContext.branchName).toBe(BRANCH_NAME);
    }
  });

  // --- Cycle 2.3: Acceptance test (AT-1) ---

  it.each([
    { retries: 0, expectedShouldRetry: true },
    { retries: 1, expectedShouldRetry: true },
    { retries: 2, expectedShouldRetry: false },
    { retries: 3, expectedShouldRetry: false },
  ])(
    "retryBudget_parametrized_enforcesMaxRetries (retries=$retries)",
    ({ retries, expectedShouldRetry }) => {
      const result = evaluateRetry(
        STORY_ID,
        retries,
        PREVIOUS_ERROR,
        BRANCH_NAME,
      );

      expect(result.shouldRetry).toBe(expectedShouldRetry);
    },
  );
});
