/**
 * Partial execution validation for epic orchestrator.
 *
 * Pure functions for validating --phase N and --story XXXX-YYYY
 * preconditions against the parsed implementation map and
 * execution state.
 */
import type {
  ExecutionState,
  ParsedMap,
  PartialExecutionMode,
  PrerequisiteResult,
} from "./types.js";
import { StoryStatus } from "./types.js";
import { PartialExecutionError } from "../../exceptions.js";

/** Check if a story has completed successfully in the execution state. */
function isStoryComplete(
  storyId: string,
  executionState: ExecutionState,
): boolean {
  const entry = executionState.stories[storyId];
  return entry !== undefined && entry.status === StoryStatus.SUCCESS;
}

/** Parse --phase and --story flags into a typed execution mode. */
export function parsePartialExecutionMode(
  phase: number | undefined,
  storyId: string | undefined,
): PartialExecutionMode {
  if (phase !== undefined && storyId !== undefined) {
    throw new PartialExecutionError(
      "--phase and --story are mutually exclusive",
      "MUTUAL_EXCLUSIVITY",
      { phase, storyId },
    );
  }
  if (phase !== undefined) return { kind: "phase", phase };
  if (storyId !== undefined) return { kind: "story", storyId };
  return { kind: "full" };
}

/** Validate that all stories in phases 0..phase-1 have SUCCESS status. */
export function validatePhasePrerequisites(
  phase: number,
  parsedMap: ParsedMap,
  executionState: ExecutionState,
): PrerequisiteResult {
  const maxPhase = parsedMap.totalPhases - 1;
  if (phase < 0 || phase >= parsedMap.totalPhases) {
    return {
      valid: false,
      error: `Phase ${String(phase)} does not exist. Max phase is ${String(maxPhase)}.`,
    };
  }
  if (phase === 0) return { valid: true };

  for (let p = 0; p < phase; p++) {
    const stories = parsedMap.phases.get(p) ?? [];
    for (const sid of stories) {
      if (!isStoryComplete(sid, executionState)) {
        return {
          valid: false,
          error: `Phases 0..${String(phase - 1)} must be complete before phase ${String(phase)}`,
        };
      }
    }
  }
  return { valid: true };
}

/** Validate that all dependencies of a story have SUCCESS status. */
export function validateStoryPrerequisites(
  storyId: string,
  parsedMap: ParsedMap,
  executionState: ExecutionState,
): PrerequisiteResult {
  const node = parsedMap.stories.get(storyId);
  if (node === undefined) {
    return {
      valid: false,
      error: `Story ${storyId} not found in implementation map`,
    };
  }
  if (node.blockedBy.length === 0) return { valid: true };

  const unsatisfied: string[] = [];
  for (const depId of node.blockedBy) {
    if (!isStoryComplete(depId, executionState)) {
      unsatisfied.push(depId);
    }
  }
  if (unsatisfied.length > 0) {
    return {
      valid: false,
      error: `Dependencies not satisfied: [${unsatisfied.join(", ")}]`,
      unsatisfiedDeps: unsatisfied,
    };
  }
  return { valid: true };
}

/** Get story IDs for a specific phase from the parsed map. */
export function getStoriesForPhase(
  phase: number,
  parsedMap: ParsedMap,
): readonly string[] {
  return parsedMap.phases.get(phase) ?? [];
}
