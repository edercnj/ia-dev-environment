/**
 * Phase computation for the DAG.
 *
 * Assigns implementation phases using BFS-like iterative resolution:
 * Phase 0 = roots (no dependencies), Phase N = all deps in phases 0..N-1.
 */
import type { DagNode } from "./types.js";
import { InvalidDagError } from "./types.js";

/** Check if all dependencies of a node are already resolved. */
function allDependenciesResolved(
  node: DagNode,
  resolved: Set<string>,
): boolean {
  return node.blockedBy.every((dep) => resolved.has(dep));
}

/** Build a diagnostic message for unresolvable stories. */
function buildUnresolvedDiagnostic(
  dag: Map<string, DagNode>,
  resolved: Set<string>,
): string {
  const unresolvedIds: string[] = [];
  for (const [id] of dag) {
    if (!resolved.has(id)) unresolvedIds.push(id);
  }
  return `Cannot compute phases: unresolvable stories (${unresolvedIds.join(", ")})`;
}

/** Compute implementation phases from the validated DAG. */
export function computePhases(
  dag: Map<string, DagNode>,
): Map<number, string[]> {
  const phases = new Map<number, string[]>();
  const resolved = new Set<string>();
  let currentPhase = 0;

  while (resolved.size < dag.size) {
    const phaseStories: string[] = [];

    for (const [id, node] of dag) {
      if (resolved.has(id)) continue;
      if (allDependenciesResolved(node, resolved)) {
        phaseStories.push(id);
        node.phase = currentPhase;
      }
    }

    if (phaseStories.length === 0) {
      throw new InvalidDagError(
        buildUnresolvedDiagnostic(dag, resolved),
      );
    }

    phases.set(currentPhase, phaseStories);
    for (const id of phaseStories) {
      resolved.add(id);
    }
    currentPhase++;
  }

  return phases;
}
