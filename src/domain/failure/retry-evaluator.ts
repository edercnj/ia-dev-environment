/**
 * Evaluates whether a failed story should be retried or its budget is exhausted.
 *
 * Pure function — no I/O. The orchestrator acts on the returned decision.
 */
import type { RetryDecision } from "./types.js";
import { MAX_RETRIES } from "./types.js";

/**
 * Determines whether a story should be retried after failure.
 *
 * @param storyId - The failed story identifier
 * @param currentRetries - How many retries have already been attempted
 * @param previousError - Error message from the most recent failure
 * @param branchName - Git branch for the retry attempt
 * @returns A {@link RetryDecision} indicating retry or budget exhaustion
 */
export function evaluateRetry(
  storyId: string,
  currentRetries: number,
  previousError: string,
  branchName: string,
): RetryDecision {
  if (currentRetries >= MAX_RETRIES) {
    return { shouldRetry: false, reason: "budget_exhausted" };
  }

  return {
    shouldRetry: true,
    retryContext: {
      storyId,
      previousError,
      retryNumber: currentRetries + 1,
      branchName,
    },
  };
}
