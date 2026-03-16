/**
 * Dry-run planner — builds an execution plan from a ParsedMap.
 */

import type {
  ParsedMap,
  DryRunOptions,
  DryRunPlan,
  DryRunPhaseInfo,
  DryRunStoryInfo,
  StoryNode,
} from "./types.js";

export function buildDryRunPlan(
  parsedMap: ParsedMap,
  epicId: string,
  _options: DryRunOptions,
): DryRunPlan {
  const phases = buildPhases(parsedMap);
  return {
    epicId,
    mode: "full",
    totalStories: parsedMap.stories.size,
    totalPhases: parsedMap.phases.length,
    criticalPath: [...parsedMap.criticalPath],
    phases,
  };
}

function buildPhases(
  parsedMap: ParsedMap,
): readonly DryRunPhaseInfo[] {
  return parsedMap.phases.map((phase) =>
    buildPhaseInfo(parsedMap, phase),
  );
}

function buildPhaseInfo(
  parsedMap: ParsedMap,
  phase: number,
): DryRunPhaseInfo {
  const stories = storiesForPhase(parsedMap, phase);
  return {
    phase,
    stories,
    parallelCount: stories.length,
  };
}

function storiesForPhase(
  parsedMap: ParsedMap,
  phase: number,
): readonly DryRunStoryInfo[] {
  const result: DryRunStoryInfo[] = [];
  for (const story of parsedMap.stories.values()) {
    if (story.phase === phase) {
      result.push(toStoryInfo(story));
    }
  }
  return result;
}

function toStoryInfo(story: StoryNode): DryRunStoryInfo {
  return {
    id: story.id,
    title: story.title,
    status: "PENDING",
    isCriticalPath: story.isOnCriticalPath,
    dependenciesSatisfied: story.blockedBy.length === 0,
    blockedBy: [...story.blockedBy],
  };
}
