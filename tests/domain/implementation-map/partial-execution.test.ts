import { describe, it, expect } from "vitest";

import {
  parsePartialExecutionMode,
  validatePhasePrerequisites,
  validateStoryPrerequisites,
  getStoriesForPhase,
} from "../../../src/domain/implementation-map/partial-execution.js";
import { PartialExecutionError } from "../../../src/exceptions.js";
import { StoryStatus } from "../../../src/domain/implementation-map/types.js";
import {
  createDagNode,
  createExecutionState,
  createParsedMap,
} from "./helpers.js";

// ---------------------------------------------------------------------------
// parsePartialExecutionMode
// ---------------------------------------------------------------------------
describe("parsePartialExecutionMode", () => {
  it("parsePartialExecutionMode_neitherFlagProvided_returnsFullMode", () => {
    const result = parsePartialExecutionMode(undefined, undefined);
    expect(result).toEqual({ kind: "full" });
  });

  it("parsePartialExecutionMode_phaseOnly_returnsPhaseMode", () => {
    const result = parsePartialExecutionMode(2, undefined);
    expect(result).toEqual({ kind: "phase", phase: 2 });
  });

  it("parsePartialExecutionMode_storyOnly_returnsStoryMode", () => {
    const result = parsePartialExecutionMode(undefined, "0042-0003");
    expect(result).toEqual({ kind: "story", storyId: "0042-0003" });
  });

  it("parsePartialExecutionMode_bothFlagsProvided_throwsPartialExecutionError", () => {
    expect(() => parsePartialExecutionMode(2, "0042-0003"))
      .toThrow(PartialExecutionError);
  });

  it("parsePartialExecutionMode_bothFlagsProvided_errorMessageContainsMutuallyExclusive", () => {
    try {
      parsePartialExecutionMode(2, "0042-0003");
      expect.unreachable("Should have thrown");
    } catch (err: unknown) {
      expect(err).toBeInstanceOf(PartialExecutionError);
      const pe = err as PartialExecutionError;
      expect(pe.message).toContain("mutually exclusive");
      expect(pe.code).toBe("MUTUAL_EXCLUSIVITY");
      expect(pe.context).toEqual({ phase: 2, storyId: "0042-0003" });
      expect(pe.name).toBe("PartialExecutionError");
    }
  });

  it("parsePartialExecutionMode_phaseZero_returnsPhaseMode", () => {
    const result = parsePartialExecutionMode(0, undefined);
    expect(result).toEqual({ kind: "phase", phase: 0 });
  });

  it("parsePartialExecutionMode_nonIntegerPhase_throwsPartialExecutionError", () => {
    expect(() => parsePartialExecutionMode(1.5, undefined))
      .toThrow(PartialExecutionError);
  });

  it("parsePartialExecutionMode_nanPhase_throwsPartialExecutionError", () => {
    expect(() => parsePartialExecutionMode(NaN, undefined))
      .toThrow(PartialExecutionError);
  });

  it("parsePartialExecutionMode_nonIntegerPhase_errorCodeIsInvalidPhase", () => {
    try {
      parsePartialExecutionMode(2.7, undefined);
      expect.unreachable("Should have thrown");
    } catch (err: unknown) {
      expect(err).toBeInstanceOf(PartialExecutionError);
      const pe = err as PartialExecutionError;
      expect(pe.code).toBe("INVALID_PHASE");
      expect(pe.context).toEqual({ phase: 2.7 });
    }
  });
});

// ---------------------------------------------------------------------------
// validatePhasePrerequisites
// ---------------------------------------------------------------------------
describe("validatePhasePrerequisites", () => {
  it("validatePhasePrerequisites_phaseZero_returnsValid", () => {
    const parsedMap = createParsedMap({
      phases: new Map([[0, ["0001"]], [1, ["0002"]]]),
      totalPhases: 2,
    });
    const state = createExecutionState({});
    const result = validatePhasePrerequisites(0, parsedMap, state);
    expect(result).toEqual({ valid: true });
  });

  it("validatePhasePrerequisites_phase1WithPhase0AllSuccess_returnsValid", () => {
    const parsedMap = createParsedMap({
      phases: new Map([[0, ["0001"]], [1, ["0002"]]]),
      totalPhases: 2,
    });
    const state = createExecutionState({
      "0001": StoryStatus.SUCCESS,
    });
    const result = validatePhasePrerequisites(1, parsedMap, state);
    expect(result).toEqual({ valid: true });
  });

  it("validatePhasePrerequisites_phase2WithPhases0And1AllSuccess_returnsValid", () => {
    const parsedMap = createParsedMap({
      phases: new Map([
        [0, ["0001"]],
        [1, ["0002", "0003"]],
        [2, ["0004"]],
      ]),
      totalPhases: 3,
    });
    const state = createExecutionState({
      "0001": StoryStatus.SUCCESS,
      "0002": StoryStatus.SUCCESS,
      "0003": StoryStatus.SUCCESS,
    });
    const result = validatePhasePrerequisites(2, parsedMap, state);
    expect(result).toEqual({ valid: true });
  });

  it("validatePhasePrerequisites_phase2WithPhase1Pending_returnsInvalid", () => {
    const parsedMap = createParsedMap({
      phases: new Map([
        [0, ["0001"]],
        [1, ["0002"]],
        [2, ["0003"]],
      ]),
      totalPhases: 3,
    });
    const state = createExecutionState({
      "0001": StoryStatus.SUCCESS,
      "0002": StoryStatus.PENDING,
    });
    const result = validatePhasePrerequisites(2, parsedMap, state);
    expect(result.valid).toBe(false);
    expect(result.error).toBe(
      "Phases 0..1 must be complete before phase 2",
    );
  });

  it("validatePhasePrerequisites_phase2WithPhase0Failed_returnsInvalid", () => {
    const parsedMap = createParsedMap({
      phases: new Map([
        [0, ["0001"]],
        [1, ["0002"]],
        [2, ["0003"]],
      ]),
      totalPhases: 3,
    });
    const state = createExecutionState({
      "0001": StoryStatus.FAILED,
      "0002": StoryStatus.SUCCESS,
    });
    const result = validatePhasePrerequisites(2, parsedMap, state);
    expect(result.valid).toBe(false);
    expect(result.error).toBe(
      "Phases 0..1 must be complete before phase 2",
    );
  });

  it("validatePhasePrerequisites_phaseExceedsMaxPhase_returnsInvalid", () => {
    const parsedMap = createParsedMap({
      phases: new Map([
        [0, ["0001"]],
        [1, ["0002"]],
        [2, ["0003"]],
        [3, ["0004"]],
      ]),
      totalPhases: 4,
    });
    const state = createExecutionState({});
    const result = validatePhasePrerequisites(5, parsedMap, state);
    expect(result.valid).toBe(false);
    expect(result.error).toBe(
      "Phase 5 does not exist. Max phase is 3.",
    );
  });

  it("validatePhasePrerequisites_phaseEqualsMaxPhase_returnsInvalid", () => {
    const parsedMap = createParsedMap({
      phases: new Map([
        [0, ["0001"]],
        [1, ["0002"]],
        [2, ["0003"]],
        [3, ["0004"]],
      ]),
      totalPhases: 4,
    });
    const state = createExecutionState({});
    const result = validatePhasePrerequisites(4, parsedMap, state);
    expect(result.valid).toBe(false);
    expect(result.error).toBe(
      "Phase 4 does not exist. Max phase is 3.",
    );
  });

  it("validatePhasePrerequisites_negativePhase_returnsInvalid", () => {
    const parsedMap = createParsedMap({
      phases: new Map([[0, ["0001"]], [1, ["0002"]]]),
      totalPhases: 2,
    });
    const state = createExecutionState({});
    const result = validatePhasePrerequisites(-1, parsedMap, state);
    expect(result.valid).toBe(false);
  });

  it("validatePhasePrerequisites_nonIntegerPhase_returnsInvalid", () => {
    const parsedMap = createParsedMap({
      phases: new Map([[0, ["0001"]], [1, ["0002"]]]),
      totalPhases: 2,
    });
    const state = createExecutionState({});
    const result = validatePhasePrerequisites(0.5, parsedMap, state);
    expect(result.valid).toBe(false);
    expect(result.error).toContain("does not exist");
  });

  it("validatePhasePrerequisites_nanPhase_returnsInvalid", () => {
    const parsedMap = createParsedMap({
      phases: new Map([[0, ["0001"]], [1, ["0002"]]]),
      totalPhases: 2,
    });
    const state = createExecutionState({});
    const result = validatePhasePrerequisites(NaN, parsedMap, state);
    expect(result.valid).toBe(false);
  });
});

// ---------------------------------------------------------------------------
// validateStoryPrerequisites
// ---------------------------------------------------------------------------
describe("validateStoryPrerequisites", () => {
  it("validateStoryPrerequisites_storyNotInMap_returnsInvalid", () => {
    const parsedMap = createParsedMap();
    const state = createExecutionState({});
    const result = validateStoryPrerequisites(
      "0042-9999", parsedMap, state,
    );
    expect(result.valid).toBe(false);
    expect(result.error).toBe(
      "Story 0042-9999 not found in implementation map",
    );
  });

  it("validateStoryPrerequisites_storyWithNoDeps_returnsValid", () => {
    const stories = new Map([
      ["0042-0001", createDagNode({
        storyId: "0042-0001",
        blockedBy: [],
        phase: 0,
      })],
    ]);
    const parsedMap = createParsedMap({ stories });
    const state = createExecutionState({
      "0042-0001": StoryStatus.PENDING,
    });
    const result = validateStoryPrerequisites(
      "0042-0001", parsedMap, state,
    );
    expect(result).toEqual({ valid: true });
  });

  it("validateStoryPrerequisites_allDepsSuccess_returnsValid", () => {
    const stories = new Map([
      ["0042-0001", createDagNode({
        storyId: "0042-0001",
        phase: 0,
      })],
      ["0042-0002", createDagNode({
        storyId: "0042-0002",
        phase: 0,
      })],
      ["0042-0003", createDagNode({
        storyId: "0042-0003",
        blockedBy: ["0042-0001", "0042-0002"],
        phase: 1,
      })],
    ]);
    const parsedMap = createParsedMap({ stories });
    const state = createExecutionState({
      "0042-0001": StoryStatus.SUCCESS,
      "0042-0002": StoryStatus.SUCCESS,
      "0042-0003": StoryStatus.PENDING,
    });
    const result = validateStoryPrerequisites(
      "0042-0003", parsedMap, state,
    );
    expect(result).toEqual({ valid: true });
  });

  it("validateStoryPrerequisites_oneDepPending_returnsInvalid", () => {
    const stories = new Map([
      ["0042-0001", createDagNode({
        storyId: "0042-0001",
        phase: 0,
      })],
      ["0042-0002", createDagNode({
        storyId: "0042-0002",
        phase: 0,
      })],
      ["0042-0003", createDagNode({
        storyId: "0042-0003",
        blockedBy: ["0042-0001", "0042-0002"],
        phase: 1,
      })],
    ]);
    const parsedMap = createParsedMap({ stories });
    const state = createExecutionState({
      "0042-0001": StoryStatus.SUCCESS,
      "0042-0002": StoryStatus.PENDING,
      "0042-0003": StoryStatus.PENDING,
    });
    const result = validateStoryPrerequisites(
      "0042-0003", parsedMap, state,
    );
    expect(result.valid).toBe(false);
    expect(result.error).toContain("Dependencies not satisfied");
    expect(result.unsatisfiedDeps).toEqual(["0042-0002"]);
  });

  it("validateStoryPrerequisites_multipleUnsatisfiedDeps_listsAll", () => {
    const stories = new Map([
      ["0042-0001", createDagNode({
        storyId: "0042-0001",
        phase: 0,
      })],
      ["0042-0002", createDagNode({
        storyId: "0042-0002",
        phase: 0,
      })],
      ["0042-0003", createDagNode({
        storyId: "0042-0003",
        phase: 0,
      })],
      ["0042-0004", createDagNode({
        storyId: "0042-0004",
        blockedBy: ["0042-0001", "0042-0002", "0042-0003"],
        phase: 1,
      })],
    ]);
    const parsedMap = createParsedMap({ stories });
    const state = createExecutionState({
      "0042-0001": StoryStatus.PENDING,
      "0042-0002": StoryStatus.FAILED,
      "0042-0003": StoryStatus.SUCCESS,
      "0042-0004": StoryStatus.PENDING,
    });
    const result = validateStoryPrerequisites(
      "0042-0004", parsedMap, state,
    );
    expect(result.valid).toBe(false);
    expect(result.unsatisfiedDeps).toEqual(["0042-0001", "0042-0002"]);
  });

  it("validateStoryPrerequisites_depMissingFromState_treatedAsUnsatisfied", () => {
    const stories = new Map([
      ["0042-0001", createDagNode({
        storyId: "0042-0001",
        phase: 0,
      })],
      ["0042-0002", createDagNode({
        storyId: "0042-0002",
        phase: 0,
      })],
      ["0042-0003", createDagNode({
        storyId: "0042-0003",
        blockedBy: ["0042-0001", "0042-0002"],
        phase: 1,
      })],
    ]);
    const parsedMap = createParsedMap({ stories });
    const state = createExecutionState({
      "0042-0001": StoryStatus.SUCCESS,
    });
    const result = validateStoryPrerequisites(
      "0042-0003", parsedMap, state,
    );
    expect(result.valid).toBe(false);
    expect(result.unsatisfiedDeps).toEqual(["0042-0002"]);
  });
});

// ---------------------------------------------------------------------------
// getStoriesForPhase
// ---------------------------------------------------------------------------
describe("getStoriesForPhase", () => {
  it("getStoriesForPhase_validPhaseWithStories_returnsStoryIds", () => {
    const parsedMap = createParsedMap({
      phases: new Map([
        [0, ["0001", "0002"]],
        [1, ["0003"]],
      ]),
    });
    const result = getStoriesForPhase(0, parsedMap);
    expect(result).toEqual(["0001", "0002"]);
  });

  it("getStoriesForPhase_phaseNotInMap_returnsEmptyArray", () => {
    const parsedMap = createParsedMap({
      phases: new Map([[0, ["0001"]]]),
    });
    const result = getStoriesForPhase(5, parsedMap);
    expect(result).toEqual([]);
  });

  it("getStoriesForPhase_phase0InMultiPhaseMap_returnsOnlyPhase0Stories", () => {
    const parsedMap = createParsedMap({
      phases: new Map([
        [0, ["0001"]],
        [1, ["0002", "0003"]],
        [2, ["0004"]],
      ]),
    });
    const result = getStoriesForPhase(0, parsedMap);
    expect(result).toEqual(["0001"]);
  });
});
