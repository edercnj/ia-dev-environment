import type { BuildMetricsParams } from "./types.js";
import type { MetricsUpdate } from "../checkpoint/types.js";

const MS_PER_MINUTE = 60000;

export function calculateAverageStoryDuration(
  storyDurations: ReadonlyMap<string, number>,
): number | undefined {
  if (storyDurations.size === 0) return undefined;
  let sum = 0;
  for (const d of storyDurations.values()) {
    sum += d;
  }
  return sum / storyDurations.size;
}

export function calculateEstimatedRemaining(
  storiesPending: number,
  averageDurationMs: number | undefined,
): number | undefined {
  if (averageDurationMs === undefined) return undefined;
  const clamped = Math.max(storiesPending, 0);
  return clamped * averageDurationMs;
}

function mapToRecord<K extends string | number>(
  map: ReadonlyMap<K, number>,
): Record<string, number> {
  const record: Record<string, number> = {};
  for (const [key, value] of map) {
    record[String(key)] = value;
  }
  return record;
}

function deriveRemainingMinutes(
  estimatedRemainingMs: number | undefined,
): number | undefined {
  if (estimatedRemainingMs === undefined) return undefined;
  return Math.round(estimatedRemainingMs / MS_PER_MINUTE);
}

export function buildMetricsUpdate(
  params: BuildMetricsParams,
): MetricsUpdate {
  const avg = calculateAverageStoryDuration(params.storyDurations);
  const pending = params.storiesTotal - params.storiesCompleted;
  const estMs = calculateEstimatedRemaining(pending, avg);
  return {
    storiesCompleted: params.storiesCompleted,
    storiesTotal: params.storiesTotal,
    storiesFailed: params.storiesFailed,
    storiesBlocked: params.storiesBlocked,
    elapsedMs: params.elapsedMs,
    averageStoryDurationMs: avg,
    estimatedRemainingMs: estMs,
    estimatedRemainingMinutes: deriveRemainingMinutes(estMs),
    storyDurations: mapToRecord(params.storyDurations),
    phaseDurations: mapToRecord(params.phaseDurations),
  };
}
