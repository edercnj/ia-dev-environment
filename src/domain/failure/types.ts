/**
 * Types and constants for the failure handling subsystem.
 *
 * Covers retry budget evaluation (RULE-005) and
 * transitive block propagation (RULE-006).
 */

/** Maximum number of retries per story before marking as FAILED. (RULE-005) */
export const MAX_RETRIES = 2;

/** Context passed to the retry subagent on a retry attempt. */
export interface RetryContext {
  readonly storyId: string;
  readonly previousError: string;
  readonly retryNumber: number;
  readonly branchName: string;
}

/** Discriminated union returned by evaluateRetry. */
export type RetryDecision =
  | { readonly shouldRetry: true; readonly retryContext: RetryContext }
  | { readonly shouldRetry: false; readonly reason: "budget_exhausted" };

/** A single entry in the block propagation result. */
export interface BlockedStoryEntry {
  readonly storyId: string;
  readonly blockedBy: readonly string[];
}

/** Result of propagating blocks through the DAG after a story failure. */
export interface BlockPropagationResult {
  readonly failedStory: string;
  readonly blockedStories: readonly BlockedStoryEntry[];
}
