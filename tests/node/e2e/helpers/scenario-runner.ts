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
import {
  MAX_RETRIES,
  StoryStatus,
} from "../../../../src/checkpoint/types.js";
import type { ExecutionState } from "../../../../src/checkpoint/types.js";
import { propagateBlocks } from "../../../../src/domain/failure/block-propagator.js";
import { evaluateRetry } from "../../../../src/domain/failure/retry-evaluator.js";
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

function toDomainState(state: ExecutionState): DomainExecutionState {
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
  const storyInputs = Array.from(parsedMap.stories.entries()).map(
    ([id, node]) => ({ id, phase: node.phase }),
  );
  await createCheckpoint(epicDir, {
    epicId: EPIC_ID,
    branch: BRANCH,
    stories: storyInputs,
    mode: { parallel: config.parallel ?? false, skipReview: false },
  });
}

async function processStory(
  epicDir: string,
  storyId: string,
  phase: number,
  storyIndex: number,
  storiesTotal: number,
  config: ScenarioConfig,
  parsedMap: ParsedMap,
  reporter: ReturnType<typeof createProgressReporter>,
): Promise<void> {
  await reporter.emit({
    type: "STORY_START",
    storyId,
    phase,
    storyIndex,
    storiesTotal,
  });
  await updateStoryStatus(epicDir, storyId, {
    status: StoryStatus.IN_PROGRESS,
  });

  const startMs = Date.now();
  let result = config.mockDispatch.dispatch(storyId);
  let retries = 0;

  while (result.status === "FAILED") {
    const decision = evaluateRetry(
      storyId,
      retries,
      result.summary,
      BRANCH,
    );
    if (!decision.shouldRetry) {
      await updateStoryStatus(epicDir, storyId, {
        status: StoryStatus.FAILED,
        retries,
        summary: result.summary,
        findingsCount: result.findingsCount,
      });
      await reporter.emit({
        type: "STORY_COMPLETE",
        storyId,
        status: "FAILED",
        durationMs: Date.now() - startMs,
      });
      const blockResult = propagateBlocks(storyId, parsedMap.stories);
      for (const blocked of blockResult.blockedStories) {
        await updateStoryStatus(epicDir, blocked.storyId, {
          status: StoryStatus.BLOCKED,
          blockedBy: blocked.blockedBy,
        });
      }
      if (blockResult.blockedStories.length > 0) {
        await reporter.emit({
          type: "BLOCK",
          storyId,
          blockedStories: blockResult.blockedStories.map(
            (b) => b.storyId,
          ),
        });
      }
      return;
    }
    retries++;
    await reporter.emit({
      type: "RETRY",
      storyId,
      retryNumber: retries,
      maxRetries: MAX_RETRIES,
      previousError: result.summary,
    });
    result = config.mockDispatch.dispatch(storyId);
  }

  await updateStoryStatus(epicDir, storyId, {
    status: StoryStatus.SUCCESS,
    retries,
    commitSha: result.commitSha,
    findingsCount: result.findingsCount,
    summary: result.summary,
  });
  await reporter.emit({
    type: "STORY_COMPLETE",
    storyId,
    status: "SUCCESS",
    durationMs: Date.now() - startMs,
    commitSha: result.commitSha,
  });
}

export async function runScenario(
  config: ScenarioConfig,
): Promise<ScenarioResult> {
  const tmpDir = mkdtempSync(join(tmpdir(), "e2e-orchestrator-"));
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
      : Array.from({ length: parsedMap.totalPhases }, (_, i) => i);

  for (const phase of phases) {
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
      await processStory(
        epicDir,
        storyId,
        phase,
        storyIndex,
        phaseStoryIds.length,
        config,
        parsedMap,
        reporter,
      );
    }

    await updateIntegrityGate(epicDir, phase, {
      status: "PASS",
      testCount: 42,
      coverage: 98.5,
      branchCoverage: 95,
    });
    await reporter.emit({
      type: "GATE_RESULT",
      phase,
      status: "PASS",
      testCount: 42,
      coverage: 98.5,
    });
  }

  const finalState = await readCheckpoint(epicDir);
  const stories = Object.values(finalState.stories);
  const successCount = stories.filter(
    (s) => s.status === StoryStatus.SUCCESS,
  ).length;
  const failedCount = stories.filter(
    (s) => s.status === StoryStatus.FAILED,
  ).length;
  const blockedCount = stories.filter(
    (s) => s.status === StoryStatus.BLOCKED,
  ).length;

  await reporter.emit({
    type: "EPIC_COMPLETE",
    storiesCompleted: successCount,
    storiesTotal: stories.length,
    storiesFailed: failedCount,
    storiesBlocked: blockedCount,
    elapsedMs: reporter.getElapsedMs(),
    retryCount: stories.reduce((sum, s) => sum + s.retries, 0),
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
