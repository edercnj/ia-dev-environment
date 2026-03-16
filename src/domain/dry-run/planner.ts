/**
 * Dry-run planner — builds an execution plan from a ParsedMap.
 */

import type {
  ParsedMap,
  DryRunOptions,
  DryRunPlan,
  DryRunMode,
  DryRunPhaseInfo,
  DryRunStoryInfo,
  DryRunStoryStatus,
  DryRunStoryDetail,
  StoryNode,
  ExecutionState,
} from "./types.js";

const STATUS_MAP: Record<string, DryRunStoryStatus> = {
  SUCCESS: "COMPLETED",
  FAILED: "FAILED",
  IN_PROGRESS: "IN_PROGRESS",
  BLOCKED: "BLOCKED",
  PENDING: "PENDING",
};

export function buildDryRunPlan(
  parsedMap: ParsedMap,
  epicId: string,
  options: DryRunOptions,
): DryRunPlan {
  validateOptions(parsedMap, options);
  const mode = resolveMode(options);
  const phases = buildPhases(parsedMap, options);
  const storyDetail = buildStoryDetail(parsedMap, options);
  return {
    epicId,
    mode,
    totalStories: parsedMap.stories.size,
    totalPhases: parsedMap.phases.length,
    criticalPath: [...parsedMap.criticalPath],
    phases,
    storyDetail,
  };
}

function validateOptions(
  parsedMap: ParsedMap,
  options: DryRunOptions,
): void {
  validatePhaseFilter(parsedMap, options);
  validateStoryFilter(parsedMap, options);
}

function validatePhaseFilter(
  parsedMap: ParsedMap,
  options: DryRunOptions,
): void {
  if (options.phaseFilter === undefined) {
    return;
  }
  if (!parsedMap.phases.includes(options.phaseFilter)) {
    throw new Error(
      `Phase ${options.phaseFilter} does not exist. ` +
        `Available: [${parsedMap.phases.join(", ")}]`,
    );
  }
}

function validateStoryFilter(
  parsedMap: ParsedMap,
  options: DryRunOptions,
): void {
  if (options.storyFilter === undefined) {
    return;
  }
  if (!parsedMap.stories.has(options.storyFilter)) {
    throw new Error(
      `Story "${options.storyFilter}" not found in map.`,
    );
  }
}

function resolveMode(options: DryRunOptions): DryRunMode {
  if (options.storyFilter !== undefined) {
    return "story";
  }
  if (options.phaseFilter !== undefined) {
    return "phase";
  }
  return "full";
}

function buildPhases(
  parsedMap: ParsedMap,
  options: DryRunOptions,
): readonly DryRunPhaseInfo[] {
  const targetPhases = filterPhases(parsedMap, options);
  return targetPhases.map((phase) =>
    buildPhaseInfo(parsedMap, phase, options),
  );
}

function filterPhases(
  parsedMap: ParsedMap,
  options: DryRunOptions,
): readonly number[] {
  if (options.phaseFilter !== undefined) {
    return [options.phaseFilter];
  }
  return parsedMap.phases;
}

function buildPhaseInfo(
  parsedMap: ParsedMap,
  phase: number,
  options: DryRunOptions,
): DryRunPhaseInfo {
  const stories = storiesForPhase(parsedMap, phase, options);
  const phaseIds = collectPhaseIds(stories);
  return {
    phase,
    stories,
    parallelCount: countParallel(stories, phaseIds, options),
  };
}

function collectPhaseIds(
  stories: readonly DryRunStoryInfo[],
): ReadonlySet<string> {
  return new Set(stories.map((s) => s.id));
}

function countParallel(
  stories: readonly DryRunStoryInfo[],
  phaseIds: ReadonlySet<string>,
  options: DryRunOptions,
): number {
  if (!options.parallelMode) {
    return stories.length;
  }
  return stories.filter((s) =>
    hasNoSamePhaseBlocker(s, phaseIds),
  ).length;
}

function hasNoSamePhaseBlocker(
  story: DryRunStoryInfo,
  phaseIds: ReadonlySet<string>,
): boolean {
  return !story.blockedBy.some((dep) => phaseIds.has(dep));
}

function storiesForPhase(
  parsedMap: ParsedMap,
  phase: number,
  options: DryRunOptions,
): readonly DryRunStoryInfo[] {
  const result: DryRunStoryInfo[] = [];
  for (const story of parsedMap.stories.values()) {
    if (story.phase === phase) {
      result.push(toStoryInfo(story, options));
    }
  }
  return result;
}

function toStoryInfo(
  story: StoryNode,
  options: DryRunOptions,
): DryRunStoryInfo {
  const status = resolveStatus(story.id, options);
  return {
    id: story.id,
    title: story.title,
    status,
    isCriticalPath: story.isOnCriticalPath,
    dependenciesSatisfied: story.blockedBy.length === 0,
    blockedBy: [...story.blockedBy],
  };
}

function buildStoryDetail(
  parsedMap: ParsedMap,
  options: DryRunOptions,
): DryRunStoryDetail | undefined {
  if (options.storyFilter === undefined) {
    return undefined;
  }
  // Safe: validateStoryFilter already confirmed existence
  const story = parsedMap.stories.get(options.storyFilter)!;
  return toStoryDetail(story, options);
}

function toStoryDetail(
  story: StoryNode,
  options: DryRunOptions,
): DryRunStoryDetail {
  return {
    id: story.id,
    title: story.title,
    phase: story.phase,
    status: resolveStatus(story.id, options),
    isCriticalPath: story.isOnCriticalPath,
    dependencies: [...story.blockedBy],
    dependents: [...story.blocks],
  };
}

function resolveStatus(
  storyId: string,
  options: DryRunOptions,
): DryRunStoryStatus {
  if (!options.resume || !options.executionState) {
    return "PENDING";
  }
  return mapCheckpointStatus(options.executionState, storyId);
}

function mapCheckpointStatus(
  state: ExecutionState,
  storyId: string,
): DryRunStoryStatus {
  const entry = state.stories.get(storyId);
  if (!entry) {
    return "PENDING";
  }
  return STATUS_MAP[entry.status] ?? "PENDING";
}
