/**
 * DAG validation functions.
 *
 * Validates symmetry of dependency edges, detects cycles using DFS
 * with three-color marking, and verifies that root nodes exist.
 */
import type { DagNode, DagWarning } from "./types.js";
import { CircularDependencyError, InvalidDagError } from "./types.js";

const WHITE = 0;
const GRAY = 1;
const BLACK = 2;

/** Check blocks->blockedBy symmetry and auto-correct mismatches. */
function checkBlocksSymmetry(
  dag: Map<string, DagNode>,
  warnings: DagWarning[],
): void {
  for (const [nodeId, node] of dag) {
    for (const targetId of node.blocks) {
      const target = dag.get(targetId);
      if (target === undefined) continue;
      if (!target.blockedBy.includes(nodeId)) {
        target.blockedBy.push(nodeId);
        warnings.push({
          type: "asymmetric-dependency",
          message: `${nodeId} blocks ${targetId}, but ${targetId} missing ${nodeId} in blockedBy`,
        });
      }
    }
  }
}

/** Check blockedBy->blocks symmetry and auto-correct mismatches. */
function checkBlockedBySymmetry(
  dag: Map<string, DagNode>,
  warnings: DagWarning[],
): void {
  for (const [nodeId, node] of dag) {
    for (const depId of node.blockedBy) {
      const dep = dag.get(depId);
      if (dep === undefined) continue;
      if (!dep.blocks.includes(nodeId)) {
        dep.blocks.push(nodeId);
        warnings.push({
          type: "asymmetric-dependency",
          message: `${nodeId} lists ${depId} in blockedBy, but ${depId} missing ${nodeId} in blocks`,
        });
      }
    }
  }
}

/** Validate symmetry of the DAG edges, auto-correcting asymmetries. */
export function validateSymmetry(
  dag: Map<string, DagNode>,
): DagWarning[] {
  const warnings: DagWarning[] = [];
  checkBlocksSymmetry(dag, warnings);
  checkBlockedBySymmetry(dag, warnings);
  return warnings;
}

/** DFS visit for cycle detection using three-color marking. */
function dfsVisit(
  nodeId: string,
  dag: Map<string, DagNode>,
  colors: Map<string, number>,
  stack: string[],
): void {
  colors.set(nodeId, GRAY);
  stack.push(nodeId);

  const node = dag.get(nodeId);
  if (node === undefined) {
    stack.pop();
    colors.set(nodeId, BLACK);
    return;
  }

  for (const neighborId of node.blocks) {
    const color = colors.get(neighborId) ?? WHITE;
    if (color === GRAY) {
      const cycleStart = stack.indexOf(neighborId);
      const cycle = stack.slice(cycleStart);
      throw new CircularDependencyError(cycle);
    }
    if (color === WHITE) {
      dfsVisit(neighborId, dag, colors, stack);
    }
  }

  stack.pop();
  colors.set(nodeId, BLACK);
}

/** Detect cycles in the DAG using DFS. Throws CircularDependencyError. */
export function detectCycles(dag: Map<string, DagNode>): void {
  const colors = new Map<string, number>();
  for (const nodeId of dag.keys()) {
    colors.set(nodeId, WHITE);
  }

  for (const nodeId of dag.keys()) {
    if (colors.get(nodeId) === WHITE) {
      dfsVisit(nodeId, dag, colors, []);
    }
  }
}

/** Validate that at least one root (node with no dependencies) exists. */
export function validateRoots(dag: Map<string, DagNode>): void {
  if (dag.size === 0) return;

  const hasRoot = [...dag.values()].some(
    (node) => node.blockedBy.length === 0,
  );
  if (!hasRoot) {
    throw new InvalidDagError(
      "DAG has no root nodes: every node has dependencies",
    );
  }
}
