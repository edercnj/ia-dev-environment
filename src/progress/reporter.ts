import type {
  ProgressEvent,
  ProgressReporter,
  ProgressReporterConfig,
  WriteFn,
} from "./types.js";
import { ProgressEventType } from "./types.js";
import {
  formatPhaseStart,
  formatStoryStart,
  formatStoryComplete,
  formatGateResult,
  formatRetry,
  formatBlock,
  formatEpicComplete,
  formatProgressSummary,
} from "./formatter.js";
import { buildMetricsUpdate } from "./metrics-calculator.js";
import { updateMetrics } from "../checkpoint/engine.js";

const DEFAULT_WRITE_FN: WriteFn = (text: string) => {
  process.stdout.write(text + "\n");
};

function handlePhaseStart(
  event: ProgressEvent & { readonly type: "PHASE_START" },
  state: ReporterState,
): void {
  computePreviousPhaseDuration(event.phase, state);
  state.phaseStartTimes.set(event.phase, Date.now());
  state.storiesTotal = Math.max(state.storiesTotal, event.storiesCount);
  state.writeFn(formatPhaseStart(event));
}

function handleStoryStart(
  event: ProgressEvent & { readonly type: "STORY_START" },
  state: ReporterState,
): void {
  state.storiesTotal = Math.max(state.storiesTotal, event.storiesTotal);
  state.lastStoryIndex = event.storyIndex;
  state.lastStoriesTotal = event.storiesTotal;
  state.writeFn(formatStoryStart(event));
}

function handleStoryComplete(
  event: ProgressEvent & { readonly type: "STORY_COMPLETE" },
  state: ReporterState,
): void {
  state.storyDurations.set(event.storyId, event.durationMs);
  incrementCounters(event.status, state);
  const ctx = {
    storyIndex: state.lastStoryIndex,
    storiesTotal: state.lastStoriesTotal,
  };
  state.writeFn(formatStoryComplete(event, ctx));
}

function incrementCounters(
  status: "SUCCESS" | "FAILED" | "PARTIAL",
  state: ReporterState,
): void {
  state.storiesCompleted++;
  if (status === "FAILED") state.storiesFailed++;
}

function handleEpicComplete(
  event: ProgressEvent & { readonly type: "EPIC_COMPLETE" },
  state: ReporterState,
): void {
  computeLastPhaseDuration(state);
  state.writeFn(formatEpicComplete(event));
  const elapsed = Date.now() - state.startTime;
  const summary = formatProgressSummary(
    event.storiesCompleted,
    event.storiesTotal,
    elapsed,
    undefined,
  );
  state.writeFn(summary);
}

function computePreviousPhaseDuration(
  currentPhase: number,
  state: ReporterState,
): void {
  for (const [phase, start] of state.phaseStartTimes) {
    if (phase < currentPhase && !state.phaseDurations.has(phase)) {
      state.phaseDurations.set(phase, Date.now() - start);
    }
  }
}

function computeLastPhaseDuration(state: ReporterState): void {
  for (const [phase, start] of state.phaseStartTimes) {
    if (!state.phaseDurations.has(phase)) {
      state.phaseDurations.set(phase, Date.now() - start);
    }
  }
}

interface ReporterState {
  readonly storyDurations: Map<string, number>;
  readonly phaseDurations: Map<number, number>;
  readonly phaseStartTimes: Map<number, number>;
  readonly writeFn: WriteFn;
  readonly epicDir: string;
  readonly persistMetrics: boolean;
  startTime: number;
  storiesCompleted: number;
  storiesFailed: number;
  storiesBlocked: number;
  storiesTotal: number;
  lastStoryIndex: number;
  lastStoriesTotal: number;
}

function initializeStartTime(state: ReporterState): void {
  if (state.startTime === 0) {
    state.startTime = Date.now();
  }
}

async function persistIfEnabled(state: ReporterState): Promise<void> {
  if (!state.persistMetrics) return;
  const elapsed = Date.now() - state.startTime;
  const metricsUpdate = buildMetricsUpdate({
    storiesCompleted: state.storiesCompleted,
    storiesTotal: state.storiesTotal,
    storiesFailed: state.storiesFailed,
    storiesBlocked: state.storiesBlocked,
    elapsedMs: elapsed,
    storyDurations: state.storyDurations,
    phaseDurations: state.phaseDurations,
  });
  await updateMetrics(state.epicDir, metricsUpdate);
}

async function handleEmit(
  event: ProgressEvent,
  state: ReporterState,
): Promise<void> {
  initializeStartTime(state);
  switch (event.type) {
    case ProgressEventType.PHASE_START:
      handlePhaseStart(event, state);
      break;
    case ProgressEventType.STORY_START:
      handleStoryStart(event, state);
      break;
    case ProgressEventType.STORY_COMPLETE:
      handleStoryComplete(event, state);
      await persistIfEnabled(state);
      break;
    case ProgressEventType.GATE_RESULT:
      state.writeFn(formatGateResult(event));
      break;
    case ProgressEventType.RETRY:
      state.writeFn(formatRetry(event));
      break;
    case ProgressEventType.BLOCK:
      state.storiesBlocked++;
      state.writeFn(formatBlock(event));
      break;
    case ProgressEventType.EPIC_COMPLETE:
      handleEpicComplete(event, state);
      break;
  }
}

export function createProgressReporter(
  config: ProgressReporterConfig,
): ProgressReporter {
  const state: ReporterState = {
    storyDurations: new Map(),
    phaseDurations: new Map(),
    phaseStartTimes: new Map(),
    writeFn: config.writeFn ?? DEFAULT_WRITE_FN,
    epicDir: config.epicDir,
    persistMetrics: config.persistMetrics ?? true,
    startTime: 0,
    storiesCompleted: 0,
    storiesFailed: 0,
    storiesBlocked: 0,
    storiesTotal: 0,
    lastStoryIndex: 0,
    lastStoriesTotal: 0,
  };
  let pendingEmit: Promise<void> = Promise.resolve();
  return {
    emit: (event: ProgressEvent) => {
      pendingEmit = pendingEmit.then(() =>
        handleEmit(event, state),
      );
      return pendingEmit;
    },
    getStoryDurations: () => new Map(state.storyDurations),
    getPhaseDurations: () => new Map(state.phaseDurations),
    getElapsedMs: () =>
      state.startTime === 0 ? 0 : Date.now() - state.startTime,
  };
}
