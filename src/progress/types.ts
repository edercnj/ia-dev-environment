export const ProgressEventType = {
  PHASE_START: "PHASE_START",
  STORY_START: "STORY_START",
  STORY_COMPLETE: "STORY_COMPLETE",
  GATE_RESULT: "GATE_RESULT",
  RETRY: "RETRY",
  BLOCK: "BLOCK",
  EPIC_COMPLETE: "EPIC_COMPLETE",
} as const;

export type ProgressEventType =
  typeof ProgressEventType[keyof typeof ProgressEventType];

export interface PhaseStartEvent {
  readonly type: typeof ProgressEventType.PHASE_START;
  readonly phase: number;
  readonly totalPhases: number;
  readonly phaseName: string;
  readonly storiesCount: number;
}

export interface StoryStartEvent {
  readonly type: typeof ProgressEventType.STORY_START;
  readonly storyId: string;
  readonly phase: number;
  readonly storyIndex: number;
  readonly storiesTotal: number;
}

export interface StoryCompleteEvent {
  readonly type: typeof ProgressEventType.STORY_COMPLETE;
  readonly storyId: string;
  readonly status: "SUCCESS" | "FAILED" | "PARTIAL";
  readonly durationMs: number;
  readonly commitSha?: string | undefined;
}

export interface GateResultEvent {
  readonly type: typeof ProgressEventType.GATE_RESULT;
  readonly phase: number;
  readonly status: "PASS" | "FAIL";
  readonly testCount: number;
  readonly coverage: number;
}

export interface RetryEvent {
  readonly type: typeof ProgressEventType.RETRY;
  readonly storyId: string;
  readonly retryNumber: number;
  readonly maxRetries: number;
  readonly previousError: string;
}

export interface BlockEvent {
  readonly type: typeof ProgressEventType.BLOCK;
  readonly storyId: string;
  readonly blockedStories: readonly string[];
}

export interface EpicCompleteEvent {
  readonly type: typeof ProgressEventType.EPIC_COMPLETE;
  readonly storiesCompleted: number;
  readonly storiesTotal: number;
  readonly storiesFailed: number;
  readonly storiesBlocked: number;
  readonly elapsedMs: number;
  readonly retryCount: number;
}

export type ProgressEvent =
  | PhaseStartEvent
  | StoryStartEvent
  | StoryCompleteEvent
  | GateResultEvent
  | RetryEvent
  | BlockEvent
  | EpicCompleteEvent;

export type WriteFn = (text: string) => void;

export interface ProgressReporterConfig {
  readonly epicDir: string;
  readonly writeFn?: WriteFn | undefined;
  readonly persistMetrics?: boolean | undefined;
}

export interface ProgressReporter {
  readonly emit: (event: ProgressEvent) => Promise<void>;
  readonly getStoryDurations: () => ReadonlyMap<string, number>;
  readonly getPhaseDurations: () => ReadonlyMap<number, number>;
  readonly getElapsedMs: () => number;
}

export interface StoryCompleteContext {
  readonly storyIndex: number;
  readonly storiesTotal: number;
}

export interface BuildMetricsParams {
  readonly storiesCompleted: number;
  readonly storiesTotal: number;
  readonly storiesFailed: number;
  readonly storiesBlocked: number;
  readonly elapsedMs: number;
  readonly storyDurations: ReadonlyMap<string, number>;
  readonly phaseDurations: ReadonlyMap<number, number>;
}
