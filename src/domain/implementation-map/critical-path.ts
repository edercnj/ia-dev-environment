/**
 * Critical path computation for the DAG.
 *
 * Uses topological-sort-based longest path algorithm to identify
 * the critical path through the dependency graph.
 */
import type { DagNode } from "./types.js";

const INITIAL_DISTANCE = 0;
const INITIAL_MAX_DISTANCE = -1;

/** Compute topological order of the DAG using Kahn's algorithm. */
function topologicalSort(dag: Map<string, DagNode>): string[] {
  const inDegree = new Map<string, number>();
  for (const [id, node] of dag) {
    inDegree.set(id, node.blockedBy.length);
  }

  const queue: string[] = [];
  for (const [id, degree] of inDegree) {
    if (degree === 0) queue.push(id);
  }

  const sorted: string[] = [];
  while (queue.length > 0) {
    const nodeId = queue.shift();
    if (nodeId === undefined) break;
    sorted.push(nodeId);
    const node = dag.get(nodeId);
    if (node === undefined) continue;
    for (const depId of node.blocks) {
      const currentDeg = inDegree.get(depId);
      if (currentDeg === undefined) continue;
      const newDeg = currentDeg - 1;
      inDegree.set(depId, newDeg);
      if (newDeg === 0) queue.push(depId);
    }
  }
  return sorted;
}

/** Backtrack from the end node to reconstruct the critical path. */
function reconstructPath(
  endNode: string,
  predecessors: Map<string, string | undefined>,
): string[] {
  const path: string[] = [];
  let current: string | undefined = endNode;
  while (current !== undefined) {
    path.unshift(current);
    current = predecessors.get(current);
  }
  return path;
}

/** Find the longest path (critical path) in the DAG. */
export function findCriticalPath(
  dag: Map<string, DagNode>,
  _phases: Map<number, readonly string[]>,
): string[] {
  if (dag.size === 0) return [];

  const sorted = topologicalSort(dag);
  const dist = new Map<string, number>();
  const pred = new Map<string, string | undefined>();

  for (const id of sorted) {
    dist.set(id, INITIAL_DISTANCE);
    pred.set(id, undefined);
  }

  for (const id of sorted) {
    const node = dag.get(id);
    if (node === undefined) continue;
    const currentDist = dist.get(id);
    if (currentDist === undefined) continue;
    for (const depId of node.blocks) {
      const depDist = dist.get(depId);
      const newDist = currentDist + 1;
      if (depDist === undefined || newDist > depDist) {
        dist.set(depId, newDist);
        pred.set(depId, id);
      }
    }
  }

  let maxDist = INITIAL_MAX_DISTANCE;
  let endNode = "";
  for (const [id, d] of dist) {
    if (d > maxDist) {
      maxDist = d;
      endNode = id;
    }
  }

  if (endNode === "") return [];
  return reconstructPath(endNode, pred);
}

/** Mark nodes on the critical path with isOnCriticalPath = true. */
export function markCriticalPath(
  dag: Map<string, DagNode>,
  criticalPath: readonly string[],
): void {
  const pathSet = new Set(criticalPath);
  for (const [, node] of dag) {
    node.isOnCriticalPath = pathSet.has(node.storyId);
  }
}
