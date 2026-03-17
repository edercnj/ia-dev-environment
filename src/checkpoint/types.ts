export const StoryStatus = {
  PENDING: "PENDING",
  IN_PROGRESS: "IN_PROGRESS",
  SUCCESS: "SUCCESS",
  FAILED: "FAILED",
  BLOCKED: "BLOCKED",
  PARTIAL: "PARTIAL",
} as const;

export type StoryStatus =
  typeof StoryStatus[keyof typeof StoryStatus];

export interface ExecutionMode {
  readonly parallel: boolean;
  readonly skipReview: boolean;
}

export interface StoryEntry {
  readonly status: StoryStatus;
  readonly commitSha?: string | undefined;
  readonly phase: number;
  readonly duration?: string | undefined;
  readonly retries: number;
  readonly blockedBy?: readonly string[] | undefined;
  readonly summary?: string | undefined;
  readonly findingsCount?: number | undefined;
}

export interface IntegrityGateEntry {
  readonly status: "PASS" | "FAIL";
  readonly timestamp: string;
  readonly testCount: number;
  readonly coverage: number;
  readonly failedTests?: readonly string[] | undefined;
}

export interface ExecutionMetrics {
  readonly storiesCompleted: number;
  readonly storiesTotal: number;
  readonly estimatedRemainingMinutes?: number | undefined;
  readonly storiesFailed?: number | undefined;
  readonly storiesBlocked?: number | undefined;
  readonly elapsedMs?: number | undefined;
  readonly estimatedRemainingMs?: number | undefined;
  readonly averageStoryDurationMs?: number | undefined;
  readonly storyDurations?: Readonly<Record<string, number>> | undefined;
  readonly phaseDurations?: Readonly<Record<string, number>> | undefined;
}

export interface SubagentResult {
  readonly status: "SUCCESS" | "FAILED" | "PARTIAL";
  readonly commitSha?: string | undefined;
  readonly findingsCount: number;
  readonly summary: string;
}

export interface ExecutionState {
  readonly epicId: string;
  readonly branch: string;
  readonly startedAt: string;
  readonly currentPhase: number;
  readonly mode: ExecutionMode;
  readonly stories: Readonly<Record<string, StoryEntry>>;
  readonly integrityGates: Readonly<
    Record<string, IntegrityGateEntry>
  >;
  readonly metrics: ExecutionMetrics;
}

export interface StoryEntryUpdate {
  readonly status?: StoryStatus | undefined;
  readonly commitSha?: string | undefined;
  readonly phase?: number | undefined;
  readonly duration?: string | undefined;
  readonly retries?: number | undefined;
  readonly blockedBy?: readonly string[] | undefined;
  readonly summary?: string | undefined;
  readonly findingsCount?: number | undefined;
}

export interface MetricsUpdate {
  readonly storiesCompleted?: number | undefined;
  readonly storiesTotal?: number | undefined;
  readonly estimatedRemainingMinutes?: number | undefined;
  readonly storiesFailed?: number | undefined;
  readonly storiesBlocked?: number | undefined;
  readonly elapsedMs?: number | undefined;
  readonly estimatedRemainingMs?: number | undefined;
  readonly averageStoryDurationMs?: number | undefined;
  readonly storyDurations?: Readonly<Record<string, number>> | undefined;
  readonly phaseDurations?: Readonly<Record<string, number>> | undefined;
}

export type IntegrityGateInput = Omit<
  IntegrityGateEntry,
  "timestamp"
>;

export interface CreateCheckpointInput {
  readonly epicId: string;
  readonly branch: string;
  readonly stories: ReadonlyArray<{
    readonly id: string;
    readonly phase: number;
  }>;
  readonly mode: ExecutionMode;
}
