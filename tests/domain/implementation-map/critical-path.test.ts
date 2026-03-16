import { describe, it, expect } from "vitest";

import {
  findCriticalPath,
  markCriticalPath,
} from "../../../src/domain/implementation-map/critical-path.js";
import { computePhases } from "../../../src/domain/implementation-map/phase-computer.js";
import { createDag } from "./helpers.js";

describe("findCriticalPath", () => {
  it("findCriticalPath_emptyDag_returnsEmptyArray", () => {
    const dag = new Map();
    const phases = new Map();
    const result = findCriticalPath(dag, phases);
    expect(result).toEqual([]);
  });

  it("findCriticalPath_singleNode_returnsSingleElementPath", () => {
    const dag = createDag([{ id: "0001" }]);
    const phases = computePhases(dag);
    const result = findCriticalPath(dag, phases);
    expect(result).toEqual(["0001"]);
  });

  it("findCriticalPath_linearChain_returnsFullChain", () => {
    const dag = createDag([
      { id: "0001", blocks: ["0002"] },
      { id: "0002", blockedBy: ["0001"], blocks: ["0003"] },
      { id: "0003", blockedBy: ["0002"] },
    ]);
    const phases = computePhases(dag);
    const result = findCriticalPath(dag, phases);
    expect(result).toEqual(["0001", "0002", "0003"]);
  });

  it("findCriticalPath_twoParallelBranchesUnequalLength_selectsLongerBranch", () => {
    const dag = createDag([
      { id: "0001", blocks: ["0002", "0003"] },
      { id: "0002", blockedBy: ["0001"], blocks: ["0004"] },
      { id: "0003", blockedBy: ["0001"] },
      { id: "0004", blockedBy: ["0002"] },
    ]);
    const phases = computePhases(dag);
    const result = findCriticalPath(dag, phases);

    expect(result).toEqual(["0001", "0002", "0004"]);
  });

  it("findCriticalPath_diamondGraph_selectsLongestPath", () => {
    const dag = createDag([
      { id: "0001", blocks: ["0002", "0003"] },
      { id: "0002", blockedBy: ["0001"], blocks: ["0004"] },
      { id: "0003", blockedBy: ["0001"], blocks: ["0004"] },
      { id: "0004", blockedBy: ["0002", "0003"] },
    ]);
    const phases = computePhases(dag);
    const result = findCriticalPath(dag, phases);

    expect(result).toHaveLength(3);
    expect(result[0]).toBe("0001");
    expect(result[2]).toBe("0004");
  });

  it("findCriticalPath_marksNodesOnCriticalPath_isOnCriticalPathTrue", () => {
    const dag = createDag([
      { id: "0001", blocks: ["0002", "0003"] },
      { id: "0002", blockedBy: ["0001"], blocks: ["0004"] },
      { id: "0003", blockedBy: ["0001"] },
      { id: "0004", blockedBy: ["0002"] },
    ]);
    const phases = computePhases(dag);
    const criticalPath = findCriticalPath(dag, phases);
    markCriticalPath(dag, criticalPath);

    for (const id of criticalPath) {
      expect(dag.get(id)?.isOnCriticalPath).toBe(true);
    }
    expect(dag.get("0003")?.isOnCriticalPath).toBe(false);
  });

  it("findCriticalPath_twoDisconnectedRoots_selectsLongestBranch", () => {
    const dag = createDag([
      { id: "0001" },
      { id: "0002", blocks: ["0003"] },
      { id: "0003", blockedBy: ["0002"] },
    ]);
    const phases = computePhases(dag);
    const result = findCriticalPath(dag, phases);

    expect(result).toEqual(["0002", "0003"]);
  });
});
