/**
 * E2E tests for the orchestrator pipeline.
 *
 * Tests the full flow: parse → loop → checkpoint → gate → report,
 * using a synthetic 5-story / 3-phase implementation map with
 * a mock subagent dispatch boundary.
 *
 * Scenarios follow TPP ordering:
 *   1. Dry-run (degenerate — no execution)
 *   2. Happy path (all SUCCESS)
 *   3. Failure path (retry + block propagation)
 *   4. Resume path (continue from checkpoint)
 *   5. Partial path (--phase filter)
 *   6. Parallel path (parallel mode flag)
 */
import { existsSync, rmSync } from "node:fs";
import { join } from "node:path";

import { afterEach, describe, expect, it } from "vitest";

import { StoryStatus } from "../../../src/checkpoint/types.js";
import { parseImplementationMap } from "../../../src/domain/implementation-map/index.js";
import {
  buildMiniImplementationMap,
  EPIC_ID,
  STORY_IDS,
  TOTAL_PHASES,
  TOTAL_STORIES,
} from "./helpers/mini-implementation-map.js";
import {
  createMockDispatch,
  FAILED_RESULT,
  SUCCESS_RESULT,
} from "./helpers/mock-subagent.js";
import { runScenario } from "./helpers/scenario-runner.js";
import type { ScenarioResult } from "./helpers/scenario-runner.js";

const MAP_CONTENT = buildMiniImplementationMap();

function buildCheckpointState(
  storiesConfig: Record<string, { status: string; phase: number }>,
  gates: Record<
    string,
    {
      status: string;
      timestamp: string;
      testCount: number;
      coverage: number;
    }
  > = {},
): Record<string, unknown> {
  const stories: Record<string, unknown> = {};
  for (const [id, cfg] of Object.entries(storiesConfig)) {
    stories[id] = { status: cfg.status, phase: cfg.phase, retries: 0 };
  }
  return {
    epicId: EPIC_ID,
    branch: "feat/e2e-test",
    startedAt: new Date().toISOString(),
    currentPhase: 0,
    mode: { parallel: false, skipReview: false },
    stories,
    integrityGates: gates,
    metrics: {
      storiesCompleted: Object.values(storiesConfig).filter(
        (s) => s.status === "SUCCESS",
      ).length,
      storiesTotal: Object.keys(storiesConfig).length,
    },
  };
}

describe("Orchestrator E2E", { timeout: 30_000 }, () => {
  const tmpDirs: string[] = [];

  afterEach(() => {
    for (const dir of tmpDirs) {
      rmSync(dir, { recursive: true, force: true });
    }
    tmpDirs.length = 0;
  });

  function trackResult(result: ScenarioResult): ScenarioResult {
    tmpDirs.push(result.tmpDir);
    return result;
  }

  // --- Scenario 1: Dry-run ---

  describe("dry-run path", () => {
    it("dryRun_validMap_noDispatchAndNoCheckpoint", () => {
      const mockDispatch = createMockDispatch({
        results: {},
        defaultResult: SUCCESS_RESULT,
      });
      const parsedMap = parseImplementationMap(MAP_CONTENT);

      expect(mockDispatch.callLog).toHaveLength(0);
      expect(parsedMap.totalPhases).toBe(TOTAL_PHASES);
      expect(parsedMap.stories.size).toBe(TOTAL_STORIES);
    });

    it("dryRun_validMap_planContainsAllStoriesAndPhases", () => {
      const parsedMap = parseImplementationMap(MAP_CONTENT);

      for (const storyId of STORY_IDS) {
        expect(parsedMap.stories.has(storyId)).toBe(true);
      }
      for (let phase = 0; phase < TOTAL_PHASES; phase++) {
        expect(parsedMap.phases.has(phase)).toBe(true);
      }

      const phase0 = parsedMap.phases.get(0)!;
      expect(phase0).toContain("story-e2e-0001");

      const phase1 = parsedMap.phases.get(1)!;
      expect(phase1).toContain("story-e2e-0002");
      expect(phase1).toContain("story-e2e-0003");

      const phase2 = parsedMap.phases.get(2)!;
      expect(phase2).toContain("story-e2e-0004");
      expect(phase2).toContain("story-e2e-0005");
    });

    it("dryRun_validMap_criticalPathIsNonEmpty", () => {
      const parsedMap = parseImplementationMap(MAP_CONTENT);
      expect(parsedMap.criticalPath.length).toBeGreaterThan(0);
      expect(parsedMap.criticalPath[0]).toBe("story-e2e-0001");
    });
  });

  // --- Scenario 2: Happy path ---

  describe("happy path", () => {
    it("happyPath_allSuccess_finalStateIs5of5Success", async () => {
      const mockDispatch = createMockDispatch({
        results: {},
        defaultResult: SUCCESS_RESULT,
      });

      const result = trackResult(
        await runScenario({ mapContent: MAP_CONTENT, mockDispatch }),
      );
      const { state, callLog, output } = result;

      const statuses = Object.values(state.stories).map(
        (s) => s.status,
      );
      expect(
        statuses.every((s) => s === StoryStatus.SUCCESS),
      ).toBe(true);
      expect(Object.keys(state.stories)).toHaveLength(TOTAL_STORIES);

      expect(callLog).toHaveLength(TOTAL_STORIES);

      for (let phase = 0; phase < TOTAL_PHASES; phase++) {
        const gateKey = `phase-${phase}`;
        expect(state.integrityGates[gateKey]).toBeDefined();
        expect(state.integrityGates[gateKey].status).toBe("PASS");
      }

      const joined = output.join("\n");
      expect(joined).toContain("Phase 0");
      expect(joined).toContain("Phase 1");
      expect(joined).toContain("Phase 2");
      expect(joined).toContain("Epic Complete");
    });

    it("happyPath_allSuccess_storiesDispatchedInPhaseOrder", async () => {
      const mockDispatch = createMockDispatch({
        results: {},
        defaultResult: SUCCESS_RESULT,
      });

      const result = trackResult(
        await runScenario({ mapContent: MAP_CONTENT, mockDispatch }),
      );
      const dispatched = result.callLog.map((e) => e.storyId);

      const phase0Idx = dispatched.indexOf("story-e2e-0001");
      const phase1Idx0 = dispatched.indexOf("story-e2e-0002");
      const phase1Idx1 = dispatched.indexOf("story-e2e-0003");
      const phase2Idx0 = dispatched.indexOf("story-e2e-0004");
      const phase2Idx1 = dispatched.indexOf("story-e2e-0005");

      expect(phase0Idx).toBeLessThan(phase1Idx0);
      expect(phase0Idx).toBeLessThan(phase1Idx1);
      expect(phase1Idx0).toBeLessThan(phase2Idx0);
      expect(phase1Idx1).toBeLessThan(phase2Idx1);
    });
  });

  // --- Scenario 3: Failure path ---

  describe("failure path", () => {
    it("failurePath_retryExhausted_failedAndBlockPropagation", async () => {
      const mockDispatch = createMockDispatch({
        results: {
          "story-e2e-0003": [
            FAILED_RESULT,
            FAILED_RESULT,
            FAILED_RESULT,
          ],
        },
        defaultResult: SUCCESS_RESULT,
      });

      const result = trackResult(
        await runScenario({ mapContent: MAP_CONTENT, mockDispatch }),
      );
      const { state, callLog, output } = result;

      expect(state.stories["story-e2e-0003"].status).toBe(
        StoryStatus.FAILED,
      );
      expect(state.stories["story-e2e-0003"].retries).toBe(2);

      expect(state.stories["story-e2e-0004"].status).toBe(
        StoryStatus.BLOCKED,
      );
      expect(state.stories["story-e2e-0005"].status).toBe(
        StoryStatus.BLOCKED,
      );

      expect(state.stories["story-e2e-0001"].status).toBe(
        StoryStatus.SUCCESS,
      );
      expect(state.stories["story-e2e-0002"].status).toBe(
        StoryStatus.SUCCESS,
      );

      const story3Calls = callLog.filter(
        (c) => c.storyId === "story-e2e-0003",
      );
      expect(story3Calls).toHaveLength(3);

      const blockedCalls = callLog.filter(
        (c) =>
          c.storyId === "story-e2e-0004" ||
          c.storyId === "story-e2e-0005",
      );
      expect(blockedCalls).toHaveLength(0);

      const joined = output.join("\n");
      expect(joined).toContain("retry");
      expect(joined).toContain("BLOCKED");
    });
  });

  // --- Scenario 4: Resume path ---

  describe("resume path", () => {
    it("resumePath_twoSuccessThreePending_dispatchesOnlyPending", async () => {
      const preExisting = buildCheckpointState({
        "story-e2e-0001": { status: "SUCCESS", phase: 0 },
        "story-e2e-0002": { status: "SUCCESS", phase: 1 },
        "story-e2e-0003": { status: "PENDING", phase: 1 },
        "story-e2e-0004": { status: "PENDING", phase: 2 },
        "story-e2e-0005": { status: "PENDING", phase: 2 },
      });

      const mockDispatch = createMockDispatch({
        results: {},
        defaultResult: SUCCESS_RESULT,
      });

      const result = trackResult(
        await runScenario({
          mapContent: MAP_CONTENT,
          mockDispatch,
          resume: true,
          preExistingState: preExisting,
        }),
      );
      const { state, callLog } = result;

      expect(callLog).toHaveLength(3);

      const dispatched = new Set(callLog.map((c) => c.storyId));
      expect(dispatched.has("story-e2e-0001")).toBe(false);
      expect(dispatched.has("story-e2e-0002")).toBe(false);
      expect(dispatched.has("story-e2e-0003")).toBe(true);
      expect(dispatched.has("story-e2e-0004")).toBe(true);
      expect(dispatched.has("story-e2e-0005")).toBe(true);

      const statuses = Object.values(state.stories).map(
        (s) => s.status,
      );
      expect(
        statuses.every((s) => s === StoryStatus.SUCCESS),
      ).toBe(true);
    });

    it("resumePath_inProgressReclassified_toPendingThenDispatched", async () => {
      const preExisting = buildCheckpointState({
        "story-e2e-0001": { status: "SUCCESS", phase: 0 },
        "story-e2e-0002": { status: "IN_PROGRESS", phase: 1 },
        "story-e2e-0003": { status: "PENDING", phase: 1 },
        "story-e2e-0004": { status: "PENDING", phase: 2 },
        "story-e2e-0005": { status: "PENDING", phase: 2 },
      });

      const mockDispatch = createMockDispatch({
        results: {},
        defaultResult: SUCCESS_RESULT,
      });

      const result = trackResult(
        await runScenario({
          mapContent: MAP_CONTENT,
          mockDispatch,
          resume: true,
          preExistingState: preExisting,
        }),
      );

      expect(result.callLog).toHaveLength(4);

      const dispatched = new Set(
        result.callLog.map((c) => c.storyId),
      );
      expect(dispatched.has("story-e2e-0002")).toBe(true);
      expect(dispatched.has("story-e2e-0003")).toBe(true);

      expect(result.state.stories["story-e2e-0002"].status).toBe(
        StoryStatus.SUCCESS,
      );
    });
  });

  // --- Scenario 5: Partial path ---

  describe("partial path", () => {
    it("partialPath_phase2Only_dispatchesOnlyPhase2Stories", async () => {
      const ts = new Date().toISOString();
      const preExisting = buildCheckpointState(
        {
          "story-e2e-0001": { status: "SUCCESS", phase: 0 },
          "story-e2e-0002": { status: "SUCCESS", phase: 1 },
          "story-e2e-0003": { status: "SUCCESS", phase: 1 },
          "story-e2e-0004": { status: "PENDING", phase: 2 },
          "story-e2e-0005": { status: "PENDING", phase: 2 },
        },
        {
          "phase-0": {
            status: "PASS",
            timestamp: ts,
            testCount: 42,
            coverage: 98.5,
          },
          "phase-1": {
            status: "PASS",
            timestamp: ts,
            testCount: 42,
            coverage: 98.5,
          },
        },
      );

      const mockDispatch = createMockDispatch({
        results: {},
        defaultResult: SUCCESS_RESULT,
      });

      const result = trackResult(
        await runScenario({
          mapContent: MAP_CONTENT,
          mockDispatch,
          phaseFilter: 2,
          preExistingState: preExisting,
        }),
      );
      const { state, callLog } = result;

      expect(callLog).toHaveLength(2);

      const dispatched = new Set(callLog.map((c) => c.storyId));
      expect(dispatched.has("story-e2e-0001")).toBe(false);
      expect(dispatched.has("story-e2e-0002")).toBe(false);
      expect(dispatched.has("story-e2e-0003")).toBe(false);
      expect(dispatched.has("story-e2e-0004")).toBe(true);
      expect(dispatched.has("story-e2e-0005")).toBe(true);

      expect(state.integrityGates["phase-2"]).toBeDefined();
      expect(state.integrityGates["phase-2"].status).toBe("PASS");
    });
  });

  // --- Scenario 6: Parallel path ---

  describe("parallel path", () => {
    it("parallelPath_parallelFlag_setsParallelModeInCheckpoint", async () => {
      const mockDispatch = createMockDispatch({
        results: {},
        defaultResult: SUCCESS_RESULT,
      });

      const result = trackResult(
        await runScenario({
          mapContent: MAP_CONTENT,
          mockDispatch,
          parallel: true,
        }),
      );
      const { state, callLog } = result;

      expect(state.mode.parallel).toBe(true);
      expect(callLog).toHaveLength(TOTAL_STORIES);

      const statuses = Object.values(state.stories).map(
        (s) => s.status,
      );
      expect(
        statuses.every((s) => s === StoryStatus.SUCCESS),
      ).toBe(true);
    });

    it("parallelPath_phase1HasTwoIndependentStories_bothDispatched", async () => {
      const mockDispatch = createMockDispatch({
        results: {},
        defaultResult: SUCCESS_RESULT,
      });

      const result = trackResult(
        await runScenario({
          mapContent: MAP_CONTENT,
          mockDispatch,
          parallel: true,
        }),
      );

      const phase1Stories = result.callLog.filter(
        (c) =>
          c.storyId === "story-e2e-0002" ||
          c.storyId === "story-e2e-0003",
      );
      expect(phase1Stories).toHaveLength(2);

      const joined = result.output.join("\n");
      expect(joined).toContain("story-e2e-0002");
      expect(joined).toContain("story-e2e-0003");
    });
  });

  // --- Mini implementation map validation ---

  describe("map parser integration", () => {
    it("parseMap_miniMap_produces5StoriesAnd3Phases", () => {
      const parsedMap = parseImplementationMap(MAP_CONTENT);

      expect(parsedMap.stories.size).toBe(5);
      expect(parsedMap.totalPhases).toBe(3);
      expect(parsedMap.warnings).toHaveLength(0);
    });

    it("parseMap_miniMap_dagSymmetryIsValid", () => {
      const parsedMap = parseImplementationMap(MAP_CONTENT);
      const story0001 = parsedMap.stories.get("story-e2e-0001")!;
      expect(story0001.blockedBy).toHaveLength(0);
      expect(story0001.blocks).toContain("story-e2e-0002");
      expect(story0001.blocks).toContain("story-e2e-0003");

      const story0004 = parsedMap.stories.get("story-e2e-0004")!;
      expect(story0004.blockedBy).toContain("story-e2e-0002");
      expect(story0004.blockedBy).toContain("story-e2e-0003");
    });
  });

  // --- Checkpoint integration ---

  describe("checkpoint integration", () => {
    it("happyPath_checkpointFile_existsWithCorrectStructure", async () => {
      const mockDispatch = createMockDispatch({
        results: {},
        defaultResult: SUCCESS_RESULT,
      });

      const result = trackResult(
        await runScenario({ mapContent: MAP_CONTENT, mockDispatch }),
      );

      const stateFile = join(
        result.epicDir,
        "execution-state.json",
      );
      expect(existsSync(stateFile)).toBe(true);

      const tmpFile = join(
        result.epicDir,
        ".execution-state.json.tmp",
      );
      expect(existsSync(tmpFile)).toBe(false);

      expect(result.state.epicId).toBe(EPIC_ID);
      expect(result.state.branch).toBe("feat/e2e-test");
      expect(result.state.startedAt).toBeTruthy();
    });
  });
});
