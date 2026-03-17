import type {
  PhaseStartEvent,
  StoryStartEvent,
  StoryCompleteEvent,
  GateResultEvent,
  RetryEvent,
  BlockEvent,
  EpicCompleteEvent,
  StoryCompleteContext,
} from "./types.js";

const MS_PER_SECOND = 1000;
const MS_PER_MINUTE = 60000;
const MS_PER_HOUR = 3600000;
const SECONDS_PER_MINUTE = 60;
const MINUTES_PER_HOUR = 60;

const STATUS_SYMBOLS: Readonly<Record<string, string>> = {
  SUCCESS: "\u2713",
  FAILED: "\u2717",
  PARTIAL: "~",
};

function formatPercent(value: number): string {
  const fixed = value.toFixed(1);
  return fixed.endsWith(".0") ? fixed.slice(0, -2) : fixed;
}

export function formatDuration(ms: number): string {
  const clamped = Math.max(ms, 0);
  const totalSeconds = Math.floor(clamped / MS_PER_SECOND);
  const hours = Math.floor(totalSeconds / (SECONDS_PER_MINUTE * MINUTES_PER_HOUR));
  const minutes = Math.floor((totalSeconds % (SECONDS_PER_MINUTE * MINUTES_PER_HOUR)) / SECONDS_PER_MINUTE);
  const seconds = totalSeconds % SECONDS_PER_MINUTE;
  if (hours > 0) return `${String(hours)}h ${String(minutes)}m ${String(seconds)}s`;
  if (minutes > 0) return `${String(minutes)}m ${String(seconds)}s`;
  return `${String(seconds)}s`;
}

export function formatPhaseStart(event: PhaseStartEvent): string {
  const storiesLabel = event.storiesCount === 1 ? "story" : "stories";
  const count = `${String(event.storiesCount)} ${storiesLabel}`;
  return `\u2550\u2550\u2550 Phase ${String(event.phase)}/${String(event.totalPhases)} \u2014 ${event.phaseName} (${count}) \u2550\u2550\u2550`;
}

export function formatStoryStart(event: StoryStartEvent): string {
  return `  [${String(event.storyIndex)}/${String(event.storiesTotal)}] story-${event.storyId} ...`;
}

export function formatStoryComplete(
  event: StoryCompleteEvent,
  ctx: StoryCompleteContext,
): string {
  const symbol = STATUS_SYMBOLS[event.status] ?? "?";
  const duration = formatDuration(event.durationMs);
  const sha = event.commitSha ? ` [${event.commitSha}]` : "";
  return `  [${String(ctx.storyIndex)}/${String(ctx.storiesTotal)}] story-${event.storyId} ... ${symbol} ${event.status} (${duration})${sha}`;
}

export function formatGateResult(event: GateResultEvent): string {
  const cov = event.coverage.toFixed(1);
  return `  \u2500\u2500 Gate Phase ${String(event.phase)}: ${event.status} (${String(event.testCount)} tests, ${cov}% coverage) \u2500\u2500`;
}

export function formatRetry(event: RetryEvent): string {
  return `  retry ${String(event.retryNumber)}/${String(event.maxRetries)} \u2014 ${event.previousError}`;
}

export function formatBlock(event: BlockEvent): string {
  const blocked = event.blockedStories.join(", ");
  return `  \u2298 BLOCKED ${event.storyId} \u2192 [${blocked}]`;
}

export function formatEpicComplete(event: EpicCompleteEvent): string {
  const pct = formatPercent((event.storiesCompleted / event.storiesTotal) * 100);
  const duration = formatDuration(event.elapsedMs);
  return `\u2550\u2550\u2550 Epic Complete: ${String(event.storiesCompleted)}/${String(event.storiesTotal)} (${pct}%) | Total: ${duration} \u2550\u2550\u2550`;
}

export function formatProgressSummary(
  completed: number,
  total: number,
  elapsedMs: number,
  estimatedRemainingMs: number | undefined,
): string {
  const pct = formatPercent((completed / total) * 100);
  const elapsed = formatDuration(elapsedMs);
  let result = `\u2550\u2550\u2550 Progress: ${String(completed)}/${String(total)} (${pct}%) | Elapsed: ${elapsed}`;
  if (estimatedRemainingMs !== undefined) {
    const estMinutes = Math.round(estimatedRemainingMs / MS_PER_MINUTE);
    result += ` | Est. remaining: ~${String(estMinutes)}m`;
  }
  result += " \u2550\u2550\u2550";
  return result;
}
