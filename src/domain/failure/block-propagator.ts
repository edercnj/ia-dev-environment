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

  const blockedByMap = new Map<string, Set<string>>();
  const visited = new Set<string>([failedStoryId]);
  const queue: string[] = [];

  enqueueDirectDependents(
    failedNode, failedStoryId, visited, queue, blockedByMap,
  );
  processQueue(dag, visited, queue, blockedByMap);

  const blocked: BlockedStoryEntry[] = [];
  for (const [storyId, blockers] of blockedByMap) {
    blocked.push({
      storyId,
      blockedBy: Array.from(blockers),
    });
  }

  return { failedStory: failedStoryId, blockedStories: blocked };
}

function enqueueDirectDependents(
  node: DagNode,
  sourceId: string,
  visited: Set<string>,
  queue: string[],
  blockedByMap: Map<string, Set<string>>,
): void {
  for (const dependentId of node.blocks) {
    addBlocker(blockedByMap, dependentId, sourceId);
    if (!visited.has(dependentId)) {
      visited.add(dependentId);
      queue.push(dependentId);
    }
  }
}

function addBlocker(
  blockedByMap: Map<string, Set<string>>,
  storyId: string,
  blockerId: string,
): void {
  let blockers = blockedByMap.get(storyId);
  if (!blockers) {
    blockers = new Set<string>();
    blockedByMap.set(storyId, blockers);
  }
  blockers.add(blockerId);
}

function processQueue(
  dag: ReadonlyMap<string, DagNode>,
  visited: Set<string>,
  queue: string[],
  blockedByMap: Map<string, Set<string>>,
): void {
  let head = 0;
  while (head < queue.length) {
    const currentId = queue[head]!;
    head += 1;

    const currentNode = dag.get(currentId);
    if (currentNode) {
      enqueueDirectDependents(
        currentNode, currentId, visited, queue, blockedByMap,
      );
    }
  }
}
