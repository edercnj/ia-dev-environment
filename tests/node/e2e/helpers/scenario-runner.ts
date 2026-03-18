/**
 * E2E scenario runner — wires all orchestrator modules together.
 *
 * Composes: map parser, checkpoint engine, retry evaluator,
 * block propagator, progress reporter, and mock subagent dispatch.
 */
import { mkdirSync, mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";

import {
  createCheckpoint,
  readCheckpoint,
  updateIntegrityGate,
  updateStoryStatus,
} from "../../../../src/checkpoint/engine.js";
import { prepareResume } from "../../../../src/checkpoint/resume.js";
import { StoryStatus } from "../../../../src/checkpoint/types.js";
import type { ExecutionState } from "../../../../src/checkpoint/types.js";
import { propagateBlocks } from "../../../../src/domain/failure/block-propagator.js";
import { evaluateRetry } from "../../../../src/domain/failure/retry-evaluator.js";
import { MAX_RETRIES } from "../../../../src/domain/failure/types.js";
import {
  getExecutableStories,
  parseImplementationMap,
} from "../../../../src/domain/implementation-map/index.js";
import { StoryStatus as DomainStoryStatus } from "../../../../src/domain/implementation-map/types.js";
import type {
  ExecutionState as DomainExecutionState,
  ParsedMap,
} from "../../../../src/domain/implementation-map/types.js";
import { createProgressReporter } from "../../../../src/progress/reporter.js";
import type { WriteFn } from "../../../../src/progress/types.js";
import { BRANCH, EPIC_ID } from "./mini-implementation-map.js";
import type { CallLogEntry, MockSubagentDispatch } from "./mock-subagent.js";

export interface ScenarioConfig {
  readonly mapContent: string;
  readonly mockDispatch: MockSubagentDispatch;
  readonly resume?: boolean;
  readonly phaseFilter?: number;
  readonly parallel?: boolean;
  readonly preExistingState?: Record<string, unknown>;
}

export interface ScenarioResult {
  readonly state: ExecutionState;
  readonly output: string[];
  readonly callLog: readonly CallLogEntry[];
  readonly epicDir: string;
  readonly tmpDir: string;
  readonly parsedMap: ParsedMap;
}

interface StoryContext {
  readonly epicDir: string;
  readonly storyId: string;
  readonly phase: number;
  readonly storyIndex: number;
  readonly storiesTotal: number;
  readonly config: ScenarioConfig;
  readonly parsedMap: ParsedMap;
  readonly reporter: ReturnType<typeof createProgressReporter>;
}

const GATE_TEST_COUNT = 42;
const GATE_COVERAGE = 98.5;
const GATE_BRANCH_COVERAGE = 95;

function toDomainState(
  state: ExecutionState,
): DomainExecutionState {
  const stories: Record<
    string,
    { readonly status: DomainStoryStatus }
  > = {};
  for (const [id, entry] of Object.entries(state.stories)) {
    stories[id] = {
      status: entry.status as unknown as DomainStoryStatus,
    };
  }
  return { epicId: state.epicId, stories };
}

async function initCheckpoint(
  epicDir: string,
  config: ScenarioConfig,
  parsedMap: ParsedMap,
): Promise<void> {
  if (config.preExistingState) {
    writeFileSync(
      join(epicDir, "execution-state.json"),
      JSON.stringify(config.preExistingState, null, 2),
    );
    if (config.resume) {
      const existing = await readCheckpoint(epicDir);
      const { state } = prepareResume(existing);
      writeFileSync(
        join(epicDir, "execution-state.json"),
        JSON.stringify(state, null, 2),
      );
    }
    return;
  }
  const storyInputs = Array.from(
    parsedMap.stories.entries(),
  ).map(([id, node]) => ({ id, phase: node.phase }));
  await createCheckpoint(epicDir, {
    epicId: EPIC_ID,
    branch: BRANCH,
    stories: storyInputs,
    mode: {
      parallel: config.parallel ?? false,
      skipReview: false,
    },
  });
}

async function handleFailure(
  ctx: StoryContext,
  startMs: number,
  retries: number,
  result: { summary: string; findingsCount: number },
): Promise<void> {
  await updateStoryStatus(ctx.epicDir, ctx.storyId, {
    status: StoryStatus.FAILED,
    retries,
    summary: result.summary,
    findingsCount: result.findingsCount,
  });
  await ctx.reporter.emit({
    type: "STORY_COMPLETE",
    storyId: ctx.storyId,
    status: "FAILED",
    durationMs: Date.now() - startMs,
  });
  const blockResult = propagateBlocks(
    ctx.storyId,
    ctx.parsedMap.stories,
  );
  for (const blocked of blockResult.blockedStories) {
    await updateStoryStatus(ctx.epicDir, blocked.storyId, {
      status: StoryStatus.BLOCKED,
      blockedBy: blocked.blockedBy,
    });
  }
  if (blockResult.blockedStories.length > 0) {
    await ctx.reporter.emit({
      type: "BLOCK",
      storyId: ctx.storyId,
      blockedStories: blockResult.blockedStories.map(
        (b) => b.storyId,
      ),
    });
  }
}

async function processStory(ctx: StoryContext): Promise<void> {
  await ctx.reporter.emit({
    type: "STORY_START",
    storyId: ctx.storyId,
    phase: ctx.phase,
    storyIndex: ctx.storyIndex,
    storiesTotal: ctx.storiesTotal,
  });
  await updateStoryStatus(ctx.epicDir, ctx.storyId, {
    status: StoryStatus.IN_PROGRESS,
  });

  const startMs = Date.now();
  let result = ctx.config.mockDispatch.dispatch(ctx.storyId);
  let retries = 0;

  while (result.status === "FAILED") {
    const decision = evaluateRetry(
      ctx.storyId,
      retries,
      result.summary,
      BRANCH,
    );
    if (!decision.shouldRetry) {
      await handleFailure(ctx, startMs, retries, result);
      return;
    }
    retries++;
    await ctx.reporter.emit({
      type: "RETRY",
      storyId: ctx.storyId,
      retryNumber: retries,
      maxRetries: MAX_RETRIES,
      previousError: result.summary,
    });
    result = ctx.config.mockDispatch.dispatch(ctx.storyId);
  }

  await updateStoryStatus(ctx.epicDir, ctx.storyId, {
    status: StoryStatus.SUCCESS,
    retries,
    commitSha: result.commitSha,
    findingsCount: result.findingsCount,
    summary: result.summary,
  });
  await ctx.reporter.emit({
    type: "STORY_COMPLETE",
    storyId: ctx.storyId,
    status: "SUCCESS",
    durationMs: Date.now() - startMs,
    commitSha: result.commitSha,
  });
}

async function executePhase(
  epicDir: string,
  phase: number,
  config: ScenarioConfig,
  parsedMap: ParsedMap,
  reporter: ReturnType<typeof createProgressReporter>,
): Promise<void> {
  const phaseStoryIds = parsedMap.phases.get(phase) ?? [];
  await reporter.emit({
    type: "PHASE_START",
    phase,
    totalPhases: parsedMap.totalPhases,
    phaseName: `Phase ${phase}`,
    storiesCount: phaseStoryIds.length,
  });

  const state = await readCheckpoint(epicDir);
  const executable = getExecutableStories(
    parsedMap,
    toDomainState(state),
  ).filter((id) => phaseStoryIds.includes(id));

  let storyIndex = 0;
  for (const storyId of executable) {
    storyIndex++;
    await processStory({
      epicDir,
      storyId,
      phase,
      storyIndex,
      storiesTotal: phaseStoryIds.length,
      config,
      parsedMap,
      reporter,
    });
  }

  await updateIntegrityGate(epicDir, phase, {
    status: "PASS",
    testCount: GATE_TEST_COUNT,
    coverage: GATE_COVERAGE,
    branchCoverage: GATE_BRANCH_COVERAGE,
  });
  await reporter.emit({
    type: "GATE_RESULT",
    phase,
    status: "PASS",
    testCount: GATE_TEST_COUNT,
    coverage: GATE_COVERAGE,
  });
}

function countByStatus(
  state: ExecutionState,
  status: string,
): number {
  return Object.values(state.stories).filter(
    (s) => s.status === status,
  ).length;
}

export async function runScenario(
  config: ScenarioConfig,
  cleanupDirs?: string[],
): Promise<ScenarioResult> {
  const tmpDir = mkdtempSync(
    join(tmpdir(), "e2e-orchestrator-"),
  );
  cleanupDirs?.push(tmpDir);
  const epicDir = join(tmpDir, `epic-${EPIC_ID}`);
  mkdirSync(epicDir, { recursive: true });

  const parsedMap = parseImplementationMap(config.mapContent);
  await initCheckpoint(epicDir, config, parsedMap);

  const output: string[] = [];
  const writeFn: WriteFn = (text) => output.push(text);
  const reporter = createProgressReporter({
    epicDir,
    writeFn,
    persistMetrics: true,
  });

  const phases =
    config.phaseFilter !== undefined
      ? [config.phaseFilter]
      : Array.from(
          { length: parsedMap.totalPhases },
          (_, i) => i,
        );

  for (const phase of phases) {
    await executePhase(epicDir, phase, config, parsedMap, reporter);
  }

  const finalState = await readCheckpoint(epicDir);
  const totalRetries = Object.values(finalState.stories).reduce(
    (sum, s) => sum + s.retries,
    0,
  );

  await reporter.emit({
    type: "EPIC_COMPLETE",
    storiesCompleted: countByStatus(finalState, StoryStatus.SUCCESS),
    storiesTotal: Object.keys(finalState.stories).length,
    storiesFailed: countByStatus(finalState, StoryStatus.FAILED),
    storiesBlocked: countByStatus(finalState, StoryStatus.BLOCKED),
    elapsedMs: reporter.getElapsedMs(),
    retryCount: totalRetries,
  });

  return {
    state: finalState,
    output,
    callLog: config.mockDispatch.callLog,
    epicDir,
    tmpDir,
    parsedMap,
  };
}
