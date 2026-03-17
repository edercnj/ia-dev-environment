/**
 * Dry-run planning types for execution simulation.
 *
 * StoryNode/ParsedMap stubs will be replaced with real
 * imports once story-0005-0004 (implementation map parser)
 * is available.
 */

// --- Stubs from story-0005-0004 (implementation map parser) ---

export interface StoryNode {
  readonly id: string;
  readonly title: string;
  readonly blockedBy: readonly string[];
  readonly blocks: readonly string[];
  readonly phase: number;
  readonly isOnCriticalPath: boolean;
}

export interface ParsedMap {
  readonly stories: ReadonlyMap<string, StoryNode>;
  readonly phases: readonly number[];
  readonly criticalPath: readonly string[];
}

// --- Stubs from story-0005-0001 (execution state) ---

export type StoryStatus =
  | "SUCCESS"
  | "FAILED"
  | "PENDING"
  | "BLOCKED"
  | "IN_PROGRESS";

export interface StoryEntry {
  readonly id: string;
  readonly status: StoryStatus;
}

export interface ExecutionState {
  readonly epicId: string;
  readonly stories: ReadonlyMap<string, StoryEntry>;
}

// --- Dry-run specific types ---

export type DryRunStoryStatus =
  | "COMPLETED"
  | "PENDING"
  | "FAILED"
  | "BLOCKED"
  | "IN_PROGRESS";

export type DryRunMode = "full" | "phase" | "story";

export interface DryRunOptions {
  readonly resume: boolean;
  readonly executionState?: ExecutionState;
  readonly phaseFilter?: number;
  readonly parallelMode: boolean;
  readonly storyFilter?: string;
}

export interface DryRunStoryInfo {
  readonly id: string;
  readonly title: string;
  readonly status: DryRunStoryStatus;
  readonly isCriticalPath: boolean;
  readonly dependenciesSatisfied: boolean;
  readonly blockedBy: readonly string[];
}

export interface DryRunPhaseInfo {
  readonly phase: number;
  readonly stories: readonly DryRunStoryInfo[];
  readonly parallelCount: number;
}

export interface DryRunStoryDetail {
  readonly id: string;
  readonly title: string;
  readonly phase: number;
  readonly status: DryRunStoryStatus;
  readonly isCriticalPath: boolean;
  readonly dependencies: readonly string[];
  readonly dependents: readonly string[];
}

export interface DryRunPlan {
  readonly epicId: string;
  readonly mode: DryRunMode;
  readonly totalStories: number;
  readonly totalPhases: number;
  readonly criticalPath: readonly string[];
  readonly phases: readonly DryRunPhaseInfo[];
  readonly storyDetail?: DryRunStoryDetail | undefined;
}
