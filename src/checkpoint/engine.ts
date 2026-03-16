import { readFile, rename, stat, writeFile } from "node:fs/promises";
import { join } from "node:path";
import { CheckpointIOError, CheckpointValidationError } from "../exceptions.js";
import type {
  CreateCheckpointInput,
  ExecutionState,
  IntegrityGateInput,
  MetricsUpdate,
  StoryEntry,
  StoryEntryUpdate,
} from "./types.js";
import { validateExecutionState } from "./validation.js";

function stripUndefined<T extends Record<string, unknown>>(
  obj: T,
): Partial<T> {
  const result: Record<string, unknown> = {};
  for (const [k, v] of Object.entries(obj)) {
    if (v !== undefined) {
      result[k] = v;
    }
  }
  return result as Partial<T>;
}

const STATE_FILE = "execution-state.json";
const TMP_FILE = ".execution-state.json.tmp";

async function atomicWriteJson(
  dir: string,
  state: ExecutionState,
): Promise<void> {
  const tmpPath = join(dir, TMP_FILE);
  const finalPath = join(dir, STATE_FILE);
  const json = JSON.stringify(state, null, 2);
  await writeFile(tmpPath, json, "utf-8");
  await rename(tmpPath, finalPath);
}

async function assertDirectoryExists(
  dir: string,
): Promise<void> {
  try {
    const s = await stat(dir);
    if (!s.isDirectory()) {
      throw new CheckpointIOError(dir, "stat");
    }
  } catch (err: unknown) {
    if (err instanceof CheckpointIOError) throw err;
    throw new CheckpointIOError(dir, "stat");
  }
}

function buildInitialStories(
  stories: ReadonlyArray<{
    readonly id: string;
    readonly phase: number;
  }>,
): Record<string, StoryEntry> {
  const map: Record<string, StoryEntry> = {};
  for (const s of stories) {
    map[s.id] = {
      status: "PENDING",
      phase: s.phase,
      retries: 0,
    };
  }
  return map;
}

export async function createCheckpoint(
  epicDir: string,
  input: CreateCheckpointInput,
): Promise<ExecutionState> {
  await assertDirectoryExists(epicDir);
  const state: ExecutionState = {
    epicId: input.epicId,
    branch: input.branch,
    startedAt: new Date().toISOString(),
    currentPhase: 0,
    mode: input.mode,
    stories: buildInitialStories(input.stories),
    integrityGates: {},
    metrics: {
      storiesCompleted: 0,
      storiesTotal: input.stories.length,
    },
  };
  await atomicWriteJson(epicDir, state);
  return state;
}

export async function readCheckpoint(
  epicDir: string,
): Promise<ExecutionState> {
  const filePath = join(epicDir, STATE_FILE);
  let raw: string;
  try {
    raw = await readFile(filePath, "utf-8");
  } catch {
    throw new CheckpointIOError(filePath, "read");
  }
  let parsed: unknown;
  try {
    parsed = JSON.parse(raw) as unknown;
  } catch {
    throw new CheckpointIOError(filePath, "parse");
  }
  return validateExecutionState(parsed);
}

export async function updateStoryStatus(
  epicDir: string,
  storyId: string,
  update: StoryEntryUpdate,
): Promise<ExecutionState> {
  const current = await readCheckpoint(epicDir);
  const existing = current.stories[storyId];
  if (!existing) {
    throw new CheckpointValidationError(
      storyId,
      "story not found in checkpoint",
    );
  }
  const cleaned = stripUndefined(
    update as Record<string, unknown>,
  ) as Partial<StoryEntry>;
  const updated: StoryEntry = { ...existing, ...cleaned };
  const stories = { ...current.stories, [storyId]: updated };
  const state: ExecutionState = { ...current, stories };
  await atomicWriteJson(epicDir, state);
  return state;
}

export async function updateIntegrityGate(
  epicDir: string,
  phase: number,
  result: IntegrityGateInput,
): Promise<ExecutionState> {
  const current = await readCheckpoint(epicDir);
  const key = `phase-${String(phase)}`;
  const entry = {
    ...result,
    timestamp: new Date().toISOString(),
  };
  const integrityGates = {
    ...current.integrityGates,
    [key]: entry,
  };
  const state: ExecutionState = {
    ...current,
    integrityGates,
  };
  await atomicWriteJson(epicDir, state);
  return state;
}

export async function updateMetrics(
  epicDir: string,
  update: MetricsUpdate,
): Promise<ExecutionState> {
  const current = await readCheckpoint(epicDir);
  const cleaned = stripUndefined(
    update as Record<string, unknown>,
  ) as Partial<ExecutionState["metrics"]>;
  const metrics = { ...current.metrics, ...cleaned };
  const state: ExecutionState = { ...current, metrics };
  await atomicWriteJson(epicDir, state);
  return state;
}
