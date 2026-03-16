import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

import type {
  DagNode,
  DagWarning,
  DependencyMatrixRow,
  ExecutionState,
  ParsedMap,
} from "../../../src/domain/implementation-map/types.js";
import { StoryStatus } from "../../../src/domain/implementation-map/types.js";

const __dirname = dirname(fileURLToPath(import.meta.url));

const FIXTURES_DIR = join(
  __dirname,
  "..",
  "..",
  "fixtures",
  "implementation-maps",
);

const DEFAULT_PHASE = -1;

/** Read a fixture markdown file as string. */
export function readFixture(filename: string): string {
  return readFileSync(join(FIXTURES_DIR, filename), "utf-8");
}

/** Factory for DependencyMatrixRow with sensible defaults. */
export function createMatrixRow(
  overrides: Partial<DependencyMatrixRow> = {},
): DependencyMatrixRow {
  return {
    storyId: "story-0042-0001",
    title: "Default Story",
    blockedBy: [],
    blocks: [],
    status: "Pendente",
    ...overrides,
  };
}

/** Factory for DagNode with sensible defaults. */
export function createDagNode(
  overrides: Partial<DagNode> = {},
): DagNode {
  return {
    storyId: "story-0042-0001",
    title: "Default Story",
    blockedBy: [],
    blocks: [],
    phase: DEFAULT_PHASE,
    isOnCriticalPath: false,
    ...overrides,
  };
}

/** Factory for a DAG Map from an array of node descriptions. */
export function createDag(
  nodes: Array<{
    id: string;
    title?: string;
    blockedBy?: string[];
    blocks?: string[];
  }>,
): Map<string, DagNode> {
  const dag = new Map<string, DagNode>();
  for (const node of nodes) {
    dag.set(node.id, createDagNode({
      storyId: node.id,
      title: node.title ?? node.id,
      blockedBy: node.blockedBy ?? [],
      blocks: node.blocks ?? [],
    }));
  }
  return dag;
}

/** Factory for ExecutionState from a story-status record. */
export function createExecutionState(
  stories: Record<string, StoryStatus>,
  epicId: string = "epic-0042",
): ExecutionState {
  const storyEntries: Record<
    string,
    { readonly status: StoryStatus }
  > = {};
  for (const [id, status] of Object.entries(stories)) {
    storyEntries[id] = { status };
  }
  return { epicId, stories: storyEntries };
}

/** Factory for ParsedMap with sensible defaults. */
export function createParsedMap(
  overrides: Partial<{
    stories: Map<string, DagNode>;
    phases: Map<number, string[]>;
    criticalPath: string[];
    totalPhases: number;
    warnings: DagWarning[];
  }> = {},
): ParsedMap {
  const stories = overrides.stories ?? new Map<string, DagNode>();
  const phases = overrides.phases ?? new Map<number, string[]>();
  return {
    stories,
    phases,
    criticalPath: overrides.criticalPath ?? [],
    totalPhases: overrides.totalPhases ?? phases.size,
    warnings: overrides.warnings ?? [],
  };
}
