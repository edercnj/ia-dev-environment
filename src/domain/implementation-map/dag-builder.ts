/**
 * DAG builder for the implementation map.
 *
 * Converts parsed dependency matrix rows into an adjacency list
 * represented as Map<storyId, DagNode>.
 */
import type { DagNode, DependencyMatrixRow } from "./types.js";

const UNCOMPUTED_PHASE = -1;

/** Build a DAG from parsed dependency matrix rows. */
export function buildDag(
  rows: readonly DependencyMatrixRow[],
): Map<string, DagNode> {
  const dag = new Map<string, DagNode>();

  for (const row of rows) {
    dag.set(row.storyId, {
      storyId: row.storyId,
      title: row.title,
      blockedBy: [...row.blockedBy],
      blocks: [...row.blocks],
      phase: UNCOMPUTED_PHASE,
      isOnCriticalPath: false,
    });
  }

  return dag;
}
