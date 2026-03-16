/**
 * Types, interfaces, enums, and custom errors for the Implementation Map Parser.
 *
 * This module defines all data structures used by the parser pipeline:
 * markdown parsing, DAG construction, validation, phase computation,
 * critical path analysis, and executable story filtering.
 */

// TODO(story-0005-0001): Replace with import from checkpoint engine module
/** Minimal stub for story execution status. */
export enum StoryStatus {
  PENDING = "PENDING",
  IN_PROGRESS = "IN_PROGRESS",
  SUCCESS = "SUCCESS",
  FAILED = "FAILED",
  BLOCKED = "BLOCKED",
  PARTIAL = "PARTIAL",
}

// TODO(story-0005-0001): Replace with import from checkpoint engine module
/** Minimal stub for epic execution state. */
export interface ExecutionState {
  readonly epicId: string;
  readonly stories: Readonly<
    Record<string, { readonly status: StoryStatus }>
  >;
}

/** A single row from the dependency matrix (Section 1). */
export interface DependencyMatrixRow {
  readonly storyId: string;
  readonly title: string;
  readonly blockedBy: readonly string[];
  readonly blocks: readonly string[];
  readonly status: string;
}

/** A single row from the phase summary (Section 5). */
export interface PhaseSummaryRow {
  readonly phase: number;
  readonly stories: readonly string[];
  readonly layer: string;
  readonly parallelism: string;
  readonly prerequisite: string;
}

/** Node in the dependency DAG. */
export interface DagNode {
  readonly storyId: string;
  readonly title: string;
  readonly blockedBy: string[];
  readonly blocks: string[];
  phase: number;
  isOnCriticalPath: boolean;
}

/** Validation warning emitted during DAG construction. */
export interface DagWarning {
  readonly type: "asymmetric-dependency" | "missing-story-reference";
  readonly message: string;
}

/** Complete result of parsing an implementation map. */
export interface ParsedMap {
  readonly stories: ReadonlyMap<string, DagNode>;
  readonly phases: ReadonlyMap<number, readonly string[]>;
  readonly criticalPath: readonly string[];
  readonly totalPhases: number;
  readonly warnings: readonly DagWarning[];
}

/** Error thrown when a circular dependency is detected in the DAG. */
export class CircularDependencyError extends Error {
  constructor(public readonly cycle: readonly string[]) {
    const chain = [...cycle, cycle[0]].join(" -> ");
    super(`Circular dependency detected: ${chain}`);
    this.name = "CircularDependencyError";
  }
}

/** Error thrown when the DAG has no root nodes (all nodes have dependencies). */
export class InvalidDagError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "InvalidDagError";
  }
}

/** Error thrown on malformed markdown input. */
export class MapParseError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "MapParseError";
  }
}
