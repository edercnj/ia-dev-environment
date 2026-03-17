/**
 * Executable stories filter.
 *
 * Determines which stories can be executed given the current
 * execution state, sorted by critical path priority.
 */
import type { ExecutionState, ParsedMap } from "./types.js";
import { StoryStatus } from "./types.js";

/** Check if a story is PENDING and has all dependencies satisfied (SUCCESS). */
function isExecutable(
  storyId: string,
  parsedMap: ParsedMap,
  state: ExecutionState,
): boolean {
  const storyState = state.stories[storyId];
  if (storyState === undefined) return false;
  if (storyState.status !== StoryStatus.PENDING) return false;

  const node = parsedMap.stories.get(storyId);
  if (node === undefined) return false;

  return node.blockedBy.every((depId) => {
    const depState = state.stories[depId];
    return depState !== undefined && depState.status === StoryStatus.SUCCESS;
  });
}

/** Sort stories: critical path first, then alphabetically. */
function sortByCriticalPath(
  stories: string[],
  parsedMap: ParsedMap,
): string[] {
  return stories.sort((a, b) => {
    const aOnCp = parsedMap.stories.get(a)?.isOnCriticalPath ?? false;
    const bOnCp = parsedMap.stories.get(b)?.isOnCriticalPath ?? false;
    if (aOnCp && !bOnCp) return -1;
    if (!aOnCp && bOnCp) return 1;
    return a.localeCompare(b);
  });
}

/** Get executable stories sorted by critical path priority. */
export function getExecutableStories(
  parsedMap: ParsedMap,
  executionState: ExecutionState,
): string[] {
  const executable: string[] = [];
  for (const storyId of parsedMap.stories.keys()) {
    if (isExecutable(storyId, parsedMap, executionState)) {
      executable.push(storyId);
    }
  }
  return sortByCriticalPath(executable, parsedMap);
}
