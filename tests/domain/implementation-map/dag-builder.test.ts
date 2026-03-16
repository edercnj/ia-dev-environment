import { describe, it, expect } from "vitest";

import { buildDag } from "../../../src/domain/implementation-map/dag-builder.js";
import { createMatrixRow } from "./helpers.js";

const UNCOMPUTED_PHASE = -1;

describe("buildDag", () => {
  it("buildDag_emptyRows_returnsEmptyMap", () => {
    const result = buildDag([]);
    expect(result.size).toBe(0);
  });

  it("buildDag_singleRootRow_returnsMapWithOneNode", () => {
    const rows = [createMatrixRow({
      storyId: "0001",
      title: "Root Story",
      blockedBy: [],
      blocks: [],
    })];
    const result = buildDag(rows);

    expect(result.size).toBe(1);
    const node = result.get("0001");
    expect(node).toBeDefined();
    expect(node?.storyId).toBe("0001");
    expect(node?.blockedBy).toEqual([]);
    expect(node?.blocks).toEqual([]);
    expect(node?.phase).toBe(UNCOMPUTED_PHASE);
    expect(node?.isOnCriticalPath).toBe(false);
  });

  it("buildDag_twoStoriesWithDependency_setsBlockedByAndBlocks", () => {
    const rows = [
      createMatrixRow({
        storyId: "0001",
        blocks: ["0002"],
      }),
      createMatrixRow({
        storyId: "0002",
        blockedBy: ["0001"],
      }),
    ];
    const result = buildDag(rows);

    expect(result.size).toBe(2);
    expect(result.get("0001")?.blocks).toContain("0002");
    expect(result.get("0002")?.blockedBy).toContain("0001");
  });

  it("buildDag_linearChainThreeStories_buildsCorrectAdjacency", () => {
    const rows = [
      createMatrixRow({
        storyId: "0001",
        blocks: ["0002"],
      }),
      createMatrixRow({
        storyId: "0002",
        blockedBy: ["0001"],
        blocks: ["0003"],
      }),
      createMatrixRow({
        storyId: "0003",
        blockedBy: ["0002"],
      }),
    ];
    const result = buildDag(rows);

    expect(result.size).toBe(3);
    expect(result.get("0001")?.blocks).toEqual(["0002"]);
    expect(result.get("0002")?.blockedBy).toEqual(["0001"]);
    expect(result.get("0002")?.blocks).toEqual(["0003"]);
    expect(result.get("0003")?.blockedBy).toEqual(["0002"]);
  });

  it("buildDag_parallelRootsWithSharedDependent_handlesMultipleDeps", () => {
    const rows = [
      createMatrixRow({
        storyId: "0001",
        blocks: ["0003"],
      }),
      createMatrixRow({
        storyId: "0002",
        blocks: ["0003"],
      }),
      createMatrixRow({
        storyId: "0003",
        blockedBy: ["0001", "0002"],
      }),
    ];
    const result = buildDag(rows);

    expect(result.size).toBe(3);
    const dependent = result.get("0003");
    expect(dependent?.blockedBy).toHaveLength(2);
    expect(dependent?.blockedBy).toContain("0001");
    expect(dependent?.blockedBy).toContain("0002");
  });

  it("buildDag_preservesTitleFromRows_titlesMatchInput", () => {
    const rows = [createMatrixRow({
      storyId: "0001",
      title: "Execution State",
    })];
    const result = buildDag(rows);

    expect(result.get("0001")?.title).toBe("Execution State");
  });
});
