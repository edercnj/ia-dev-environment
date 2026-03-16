import { describe, it, expect } from "vitest";

import { getExecutableStories } from "../../../src/domain/implementation-map/executable-stories.js";
import { StoryStatus } from "../../../src/domain/implementation-map/types.js";
import {
  createDag,
  createDagNode,
  createExecutionState,
  createParsedMap,
} from "./helpers.js";

describe("getExecutableStories", () => {
  it("getExecutableStories_emptyParsedMap_returnsEmptyArray", () => {
    const parsedMap = createParsedMap();
    const state = createExecutionState({});
    const result = getExecutableStories(parsedMap, state);
    expect(result).toEqual([]);
  });

  it("getExecutableStories_singlePendingRootNoDeps_returnsIt", () => {
    const stories = new Map([
      ["0001", createDagNode({
        storyId: "0001",
        phase: 0,
      })],
    ]);
    const parsedMap = createParsedMap({
      stories,
      phases: new Map([[0, ["0001"]]]),
    });
    const state = createExecutionState({
      "0001": StoryStatus.PENDING,
    });

    const result = getExecutableStories(parsedMap, state);
    expect(result).toEqual(["0001"]);
  });

  it("getExecutableStories_singleStoryAlreadySuccess_returnsEmpty", () => {
    const stories = new Map([
      ["0001", createDagNode({
        storyId: "0001",
        phase: 0,
      })],
    ]);
    const parsedMap = createParsedMap({
      stories,
      phases: new Map([[0, ["0001"]]]),
    });
    const state = createExecutionState({
      "0001": StoryStatus.SUCCESS,
    });

    const result = getExecutableStories(parsedMap, state);
    expect(result).toEqual([]);
  });

  it("getExecutableStories_pendingWithSatisfiedDeps_returnsStory", () => {
    const stories = new Map([
      ["0001", createDagNode({
        storyId: "0001",
        blocks: ["0002"],
        phase: 0,
      })],
      ["0002", createDagNode({
        storyId: "0002",
        blockedBy: ["0001"],
        phase: 1,
      })],
    ]);
    const parsedMap = createParsedMap({
      stories,
      phases: new Map([[0, ["0001"]], [1, ["0002"]]]),
    });
    const state = createExecutionState({
      "0001": StoryStatus.SUCCESS,
      "0002": StoryStatus.PENDING,
    });

    const result = getExecutableStories(parsedMap, state);
    expect(result).toEqual(["0002"]);
  });

  it("getExecutableStories_pendingWithUnsatisfiedDeps_excludesStory", () => {
    const stories = new Map([
      ["0001", createDagNode({
        storyId: "0001",
        blocks: ["0002"],
        phase: 0,
      })],
      ["0002", createDagNode({
        storyId: "0002",
        blockedBy: ["0001"],
        phase: 1,
      })],
    ]);
    const parsedMap = createParsedMap({
      stories,
      phases: new Map([[0, ["0001"]], [1, ["0002"]]]),
    });
    const state = createExecutionState({
      "0001": StoryStatus.PENDING,
      "0002": StoryStatus.PENDING,
    });

    const result = getExecutableStories(parsedMap, state);
    expect(result).toEqual(["0001"]);
  });

  it("getExecutableStories_multipleSatisfied_returnsAll", () => {
    const dag = createDag([
      { id: "0001", blocks: ["0003"] },
      { id: "0002", blocks: ["0004"] },
      { id: "0003", blockedBy: ["0001"], blocks: ["0005"] },
      { id: "0004", blockedBy: ["0002"] },
      { id: "0005", blockedBy: ["0003"] },
    ]);
    dag.get("0001")!.phase = 0;
    dag.get("0002")!.phase = 0;
    dag.get("0003")!.phase = 1;
    dag.get("0004")!.phase = 1;
    dag.get("0005")!.phase = 2;
    dag.get("0003")!.isOnCriticalPath = true;

    const parsedMap = createParsedMap({
      stories: dag,
      phases: new Map([
        [0, ["0001", "0002"]],
        [1, ["0003", "0004"]],
        [2, ["0005"]],
      ]),
      criticalPath: ["0001", "0003", "0005"],
    });
    const state = createExecutionState({
      "0001": StoryStatus.SUCCESS,
      "0002": StoryStatus.SUCCESS,
      "0003": StoryStatus.PENDING,
      "0004": StoryStatus.PENDING,
      "0005": StoryStatus.PENDING,
    });

    const result = getExecutableStories(parsedMap, state);
    expect(result).toEqual(["0003", "0004"]);
  });

  it("getExecutableStories_criticalPathFirst_sortedByCriticalPath", () => {
    const dag = createDag([
      { id: "0001", blocks: ["0003"] },
      { id: "0002", blocks: ["0004"] },
      { id: "0003", blockedBy: ["0001"], blocks: ["0005"] },
      { id: "0004", blockedBy: ["0002"] },
      { id: "0005", blockedBy: ["0003"] },
    ]);
    dag.get("0001")!.phase = 0;
    dag.get("0002")!.phase = 0;
    dag.get("0003")!.phase = 1;
    dag.get("0004")!.phase = 1;
    dag.get("0005")!.phase = 2;
    dag.get("0003")!.isOnCriticalPath = true;

    const parsedMap = createParsedMap({
      stories: dag,
      phases: new Map([
        [0, ["0001", "0002"]],
        [1, ["0003", "0004"]],
        [2, ["0005"]],
      ]),
      criticalPath: ["0001", "0003", "0005"],
    });
    const state = createExecutionState({
      "0001": StoryStatus.SUCCESS,
      "0002": StoryStatus.SUCCESS,
      "0003": StoryStatus.PENDING,
      "0004": StoryStatus.PENDING,
      "0005": StoryStatus.PENDING,
    });

    const result = getExecutableStories(parsedMap, state);
    expect(result[0]).toBe("0003");
    expect(result[1]).toBe("0004");
  });

  it("getExecutableStories_storyMissingFromState_excludedGracefully", () => {
    const stories = new Map([
      ["0001", createDagNode({ storyId: "0001", phase: 0 })],
    ]);
    const parsedMap = createParsedMap({
      stories,
      phases: new Map([[0, ["0001"]]]),
    });
    const state = createExecutionState({});

    const result = getExecutableStories(parsedMap, state);
    expect(result).toEqual([]);
  });

  it("getExecutableStories_depMissingFromState_excludesStory", () => {
    const stories = new Map([
      ["0001", createDagNode({ storyId: "0001", blocks: ["0002"], phase: 0 })],
      ["0002", createDagNode({ storyId: "0002", blockedBy: ["0001"], phase: 1 })],
    ]);
    const parsedMap = createParsedMap({
      stories,
      phases: new Map([[0, ["0001"]], [1, ["0002"]]]),
    });
    const state = createExecutionState({
      "0002": StoryStatus.PENDING,
    });

    const result = getExecutableStories(parsedMap, state);
    expect(result).toEqual([]);
  });

  it("getExecutableStories_nonCriticalBeforeCritical_sortsCriticalFirst", () => {
    const stories = new Map([
      ["0002", createDagNode({ storyId: "0002", phase: 0, isOnCriticalPath: false })],
      ["0001", createDagNode({ storyId: "0001", phase: 0, isOnCriticalPath: true })],
    ]);
    const parsedMap = createParsedMap({
      stories,
      phases: new Map([[0, ["0002", "0001"]]]),
    });
    const state = createExecutionState({
      "0001": StoryStatus.PENDING,
      "0002": StoryStatus.PENDING,
    });

    const result = getExecutableStories(parsedMap, state);
    expect(result[0]).toBe("0001");
    expect(result[1]).toBe("0002");
  });

  it("getExecutableStories_noneExecutable_returnsEmptyArray", () => {
    const dag = createDag([
      { id: "0001", blocks: ["0002"] },
      { id: "0002", blockedBy: ["0001"], blocks: ["0003"] },
      { id: "0003", blockedBy: ["0002"] },
    ]);
    dag.get("0001")!.phase = 0;
    dag.get("0002")!.phase = 1;
    dag.get("0003")!.phase = 2;

    const parsedMap = createParsedMap({
      stories: dag,
      phases: new Map([
        [0, ["0001"]],
        [1, ["0002"]],
        [2, ["0003"]],
      ]),
    });
    const state = createExecutionState({
      "0001": StoryStatus.FAILED,
      "0002": StoryStatus.PENDING,
      "0003": StoryStatus.PENDING,
    });

    const result = getExecutableStories(parsedMap, state);
    expect(result).toEqual([]);
  });
});
