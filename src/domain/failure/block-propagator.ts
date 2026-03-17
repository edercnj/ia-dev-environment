/**
 * Propagates block status through the dependency DAG using BFS.
 *
 * Pure function — no I/O. Receives the DAG and a failed story ID,
 * returns all transitively blocked stories with their blockedBy chains.
 */
import type { DagNode } from "../implementation-map/types.js";
import type { BlockedStoryEntry, BlockPropagationResult } from "./types.js";

/**
 * Performs BFS on the DAG's `blocks` adjacency list to find
 * all stories transitively blocked by a failed story.
 *
 * @param failedStoryId - The story that failed after exhausting retries
 * @param dag - The DAG of story dependencies (readonly)
 * @returns A {@link BlockPropagationResult} with all blocked stories
 */
export function propagateBlocks(
  failedStoryId: string,
  dag: ReadonlyMap<string, DagNode>,
): BlockPropagationResult {
  const failedNode = dag.get(failedStoryId);
  if (!failedNode) {
    return { failedStory: failedStoryId, blockedStories: [] };
  }

  const blocked: BlockedStoryEntry[] = [];
  const visited = new Set<string>([failedStoryId]);
  const queue: Array<{ storyId: string; blockedBy: string }> = [];

  enqueueDirectDependents(failedNode, failedStoryId, visited, queue);
  processQueue(dag, visited, queue, blocked);

  return { failedStory: failedStoryId, blockedStories: blocked };
}

function enqueueDirectDependents(
  node: DagNode,
  sourceId: string,
  visited: Set<string>,
  queue: Array<{ storyId: string; blockedBy: string }>,
): void {
  for (const dependentId of node.blocks) {
    if (!visited.has(dependentId)) {
      visited.add(dependentId);
      queue.push({ storyId: dependentId, blockedBy: sourceId });
    }
  }
}

function processQueue(
  dag: ReadonlyMap<string, DagNode>,
  visited: Set<string>,
  queue: Array<{ storyId: string; blockedBy: string }>,
  blocked: BlockedStoryEntry[],
): void {
  let head = 0;
  while (head < queue.length) {
    const current = queue[head]!;
    head += 1;
    blocked.push({
      storyId: current.storyId,
      blockedBy: [current.blockedBy],
    });

    const currentNode = dag.get(current.storyId);
    if (currentNode) {
      enqueueDirectDependents(
        currentNode,
        current.storyId,
        visited,
        queue,
      );
    }
  }
}
