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
  if (typeof value !== "string") {
    const detail = value == null
      ? `is required in ${context}`
      : `must be a string in ${context}`;
    throw new CheckpointValidationError(field, detail);
  }
  return value;
}

function requireNumber(
  data: Record<string, unknown>,
  field: string,
  context: string,
): number {
  const value = data[field];
  if (typeof value !== "number") {
    const detail = value == null
      ? `is required in ${context}`
      : `must be a number in ${context}`;
    throw new CheckpointValidationError(field, detail);
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

function optionalString(
  data: Record<string, unknown>,
  field: string,
  ctx: string,
): void {
  const v = data[field];
  if (v !== undefined && typeof v !== "string") {
    throw new CheckpointValidationError(
      field,
      `must be a string in ${ctx}`,
    );
  }
}

function optionalNumber(
  data: Record<string, unknown>,
  field: string,
  ctx: string,
): void {
  const v = data[field];
  if (v !== undefined && typeof v !== "number") {
    throw new CheckpointValidationError(
      field,
      `must be a number in ${ctx}`,
    );
  }
}

function optionalStringArray(
  data: Record<string, unknown>,
  field: string,
  ctx: string,
): void {
  const v = data[field];
  if (v === undefined) return;
  if (!Array.isArray(v)) {
    throw new CheckpointValidationError(
      field,
      `must be an array in ${ctx}`,
    );
  }
  for (const item of v) {
    if (typeof item !== "string") {
      throw new CheckpointValidationError(
        field,
        `must be an array of strings in ${ctx}`,
      );
    }
  }
}

export function validateStoryEntry(
  entry: unknown,
  storyId: string,
): void {
  const data = requireNonNullObject(entry, storyId);
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
  optionalString(data, "commitSha", ctx);
  optionalString(data, "duration", ctx);
  optionalString(data, "summary", ctx);
  optionalNumber(data, "findingsCount", ctx);
  optionalStringArray(data, "blockedBy", ctx);
}

export function validateIntegrityGateEntry(
  entry: unknown,
  gateKey: string,
): void {
  const data = requireNonNullObject(entry, gateKey);
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
  optionalNumber(data, "branchCoverage", ctx);
  optionalStringArray(data, "failedTests", ctx);
  optionalString(data, "regressionSource", ctx);
}

export function validateMetrics(
  data: unknown,
): void {
  const m = requireNonNullObject(data, "metrics");
  requireNumber(m, "storiesCompleted", "metrics");
  requireNumber(m, "storiesTotal", "metrics");
  optionalNumber(m, "estimatedRemainingMinutes", "metrics");
}

function requireNonNullObject(
  data: unknown,
  context: string,
): Record<string, unknown> {
  if (data === null || data === undefined) {
    throw new CheckpointValidationError(
      context,
      "input is null or undefined",
    );
  }
  if (typeof data !== "object" || Array.isArray(data)) {
    throw new CheckpointValidationError(
      context,
      "input must be an object",
    );
  }
  return data as Record<string, unknown>;
}

function requireBoolean(
  data: Record<string, unknown>,
  field: string,
  context: string,
): void {
  if (typeof data[field] !== "boolean") {
    throw new CheckpointValidationError(
      field,
      `must be a boolean in ${context}`,
    );
  }
}

function validateMode(
  data: Record<string, unknown>,
): void {
  const mode = requireObject(data, "mode", "ExecutionState");
  requireBoolean(mode, "parallel", "ExecutionState");
  requireBoolean(mode, "skipReview", "ExecutionState");
}

function validateStoriesMap(
  data: Record<string, unknown>,
): void {
  const stories = requireObject(data, "stories", "ExecutionState");
  for (const key of Object.keys(stories)) {
    validateStoryEntry(stories[key], key);
  }
}

function validateGatesMap(
  data: Record<string, unknown>,
): void {
  const gates = requireObject(data, "integrityGates", "ExecutionState");
  for (const key of Object.keys(gates)) {
    validateIntegrityGateEntry(gates[key], key);
  }
}

export function validateExecutionState(
  data: unknown,
): ExecutionState {
  const d = requireNonNullObject(data, "ExecutionState");
  requireString(d, "epicId", "ExecutionState");
  requireString(d, "branch", "ExecutionState");
  requireString(d, "startedAt", "ExecutionState");
  requireNumber(d, "currentPhase", "ExecutionState");
  validateMode(d);
  validateStoriesMap(d);
  validateGatesMap(d);
  validateMetrics(
    requireObject(d, "metrics", "ExecutionState"),
  );
  return data as ExecutionState;
}
