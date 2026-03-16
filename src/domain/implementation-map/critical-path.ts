/**
 * Critical path computation for the DAG.
 *
 * Uses topological-sort-based longest path algorithm to identify
 * the critical path through the dependency graph.
 */
import type { DagNode } from "./types.js";

/** Compute topological order of the DAG. */
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
    const current = queue.shift()!;
    sorted.push(current);
    const node = dag.get(current);
    if (node === undefined) continue;
    for (const depId of node.blocks) {
      const deg = (inDegree.get(depId) ?? 0) - 1;
      inDegree.set(depId, deg);
      if (deg === 0) queue.push(depId);
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
    dist.set(id, 0);
    pred.set(id, undefined);
  }

  for (const id of sorted) {
    const node = dag.get(id);
    if (node === undefined) continue;
    const currentDist = dist.get(id) ?? 0;
    for (const depId of node.blocks) {
      const newDist = currentDist + 1;
      if (newDist > (dist.get(depId) ?? 0)) {
        dist.set(depId, newDist);
        pred.set(depId, id);
      }
    }
  }

  let maxDist = -1;
  let endNode = sorted[0] ?? "";
  for (const [id, d] of dist) {
    if (d > maxDist) {
      maxDist = d;
      endNode = id;
    }
  }

  return reconstructPath(endNode, pred);
}

/** Mark nodes on the critical path with isOnCriticalPath = true. */
export function markCriticalPath(
  dag: Map<string, DagNode>,
  criticalPath: readonly string[],
): void {
  const pathSet = new Set(criticalPath);
  for (const [id, node] of dag) {
    node.isOnCriticalPath = pathSet.has(id);
  }
}
