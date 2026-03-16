import { CheckpointValidationError } from "../exceptions.js";
import type {
  ExecutionState,
  StoryStatus,
} from "./types.js";
import { StoryStatus as StoryStatusValues } from "./types.js";

const VALID_STATUSES: ReadonlySet<string> = new Set(
  Object.values(StoryStatusValues),
);

const VALID_GATE_STATUSES: ReadonlySet<string> = new Set([
  "PASS",
  "FAIL",
]);

export function isValidStoryStatus(
  value: unknown,
): value is StoryStatus {
  return (
    typeof value === "string" && VALID_STATUSES.has(value)
  );
}

function requireString(
  data: Record<string, unknown>,
  field: string,
  context: string,
): string {
  const value = data[field];
  if (value === undefined || value === null) {
    throw new CheckpointValidationError(
      field,
      `is required in ${context}`,
    );
  }
  if (typeof value !== "string") {
    throw new CheckpointValidationError(
      field,
      `must be a string in ${context}`,
    );
  }
  return value;
}

function requireNumber(
  data: Record<string, unknown>,
  field: string,
  context: string,
): number {
  const value = data[field];
  if (value === undefined || value === null) {
    throw new CheckpointValidationError(
      field,
      `is required in ${context}`,
    );
  }
  if (typeof value !== "number") {
    throw new CheckpointValidationError(
      field,
      `must be a number in ${context}`,
    );
  }
  return value;
}

function requireObject(
  data: Record<string, unknown>,
  field: string,
  context: string,
): Record<string, unknown> {
  const value = data[field];
  if (value === undefined || value === null) {
    throw new CheckpointValidationError(
      field,
      `is required in ${context}`,
    );
  }
  if (typeof value !== "object" || Array.isArray(value)) {
    throw new CheckpointValidationError(
      field,
      `must be an object in ${context}`,
    );
  }
  return value as Record<string, unknown>;
}

export function validateStoryEntry(
  entry: unknown,
  storyId: string,
): void {
  if (entry === null || entry === undefined) {
    throw new CheckpointValidationError(
      storyId,
      "story entry is null or undefined",
    );
  }
  const data = entry as Record<string, unknown>;
  const ctx = `story '${storyId}'`;
  const status = requireString(data, "status", ctx);
  if (!isValidStoryStatus(status)) {
    throw new CheckpointValidationError(
      "status",
      `invalid status '${status}' in ${ctx}`,
    );
  }
  requireNumber(data, "phase", ctx);
  requireNumber(data, "retries", ctx);
}

export function validateIntegrityGateEntry(
  entry: unknown,
  gateKey: string,
): void {
  if (entry === null || entry === undefined) {
    throw new CheckpointValidationError(
      gateKey,
      "integrity gate entry is null or undefined",
    );
  }
  const data = entry as Record<string, unknown>;
  const ctx = `gate '${gateKey}'`;
  const status = requireString(data, "status", ctx);
  if (!VALID_GATE_STATUSES.has(status)) {
    throw new CheckpointValidationError(
      "status",
      `invalid gate status '${status}' in ${ctx}`,
    );
  }
  requireString(data, "timestamp", ctx);
  requireNumber(data, "testCount", ctx);
  requireNumber(data, "coverage", ctx);
}

export function validateMetrics(
  data: unknown,
): void {
  if (data === null || data === undefined) {
    throw new CheckpointValidationError(
      "metrics",
      "metrics is null or undefined",
    );
  }
  const m = data as Record<string, unknown>;
  requireNumber(m, "storiesCompleted", "metrics");
  requireNumber(m, "storiesTotal", "metrics");
}

function validateMode(
  data: Record<string, unknown>,
): void {
  const mode = requireObject(data, "mode", "ExecutionState");
  const m = mode;
  if (typeof m["parallel"] !== "boolean") {
    throw new CheckpointValidationError(
      "mode.parallel",
      "must be a boolean in ExecutionState",
    );
  }
  if (typeof m["skipReview"] !== "boolean") {
    throw new CheckpointValidationError(
      "mode.skipReview",
      "must be a boolean in ExecutionState",
    );
  }
}

export function validateExecutionState(
  data: unknown,
): ExecutionState {
  if (data === null || data === undefined) {
    throw new CheckpointValidationError(
      "ExecutionState",
      "input is null or undefined",
    );
  }
  if (typeof data !== "object" || Array.isArray(data)) {
    throw new CheckpointValidationError(
      "ExecutionState",
      "input must be an object",
    );
  }
  const d = data as Record<string, unknown>;
  requireString(d, "epicId", "ExecutionState");
  requireString(d, "branch", "ExecutionState");
  requireString(d, "startedAt", "ExecutionState");
  requireNumber(d, "currentPhase", "ExecutionState");
  validateMode(d);
  const stories = requireObject(
    d,
    "stories",
    "ExecutionState",
  );
  for (const key of Object.keys(stories)) {
    validateStoryEntry(stories[key], key);
  }
  requireObject(d, "integrityGates", "ExecutionState");
  const gates = d["integrityGates"] as Record<
    string,
    unknown
  >;
  for (const key of Object.keys(gates)) {
    validateIntegrityGateEntry(gates[key], key);
  }
  validateMetrics(
    requireObject(d, "metrics", "ExecutionState"),
  );
  return data as ExecutionState;
}
