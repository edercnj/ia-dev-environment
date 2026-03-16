import { describe, it, expect } from "vitest";

import { computePhases } from "../../../src/domain/implementation-map/phase-computer.js";
import { InvalidDagError } from "../../../src/domain/implementation-map/types.js";
import { createDag } from "./helpers.js";

describe("computePhases", () => {
  it("computePhases_emptyDag_returnsEmptyMap", () => {
    const dag = new Map();
    const result = computePhases(dag);
    expect(result.size).toBe(0);
  });

  it("computePhases_singleRoot_returnsSinglePhaseZero", () => {
    const dag = createDag([{ id: "0001" }]);
    const result = computePhases(dag);

    expect(result.size).toBe(1);
    expect(result.get(0)).toEqual(["0001"]);
  });

  it("computePhases_twoRoots_bothInPhaseZero", () => {
    const dag = createDag([
      { id: "0001" },
      { id: "0002" },
    ]);
    const result = computePhases(dag);

    expect(result.size).toBe(1);
    const phase0 = result.get(0) ?? [];
    expect(phase0).toContain("0001");
    expect(phase0).toContain("0002");
  });

  it("computePhases_linearChain_incrementalPhases", () => {
    const dag = createDag([
      { id: "0001", blocks: ["0002"] },
      { id: "0002", blockedBy: ["0001"], blocks: ["0003"] },
      { id: "0003", blockedBy: ["0002"] },
    ]);
    const result = computePhases(dag);

    expect(result.size).toBe(3);
    expect(result.get(0)).toEqual(["0001"]);
    expect(result.get(1)).toEqual(["0002"]);
    expect(result.get(2)).toEqual(["0003"]);
  });

  it("computePhases_diamondDependency_correctPhaseAssignment", () => {
    const dag = createDag([
      { id: "0001", blocks: ["0002", "0003"] },
      { id: "0002", blockedBy: ["0001"], blocks: ["0004"] },
      { id: "0003", blockedBy: ["0001"], blocks: ["0004"] },
      { id: "0004", blockedBy: ["0002", "0003"] },
    ]);
    const result = computePhases(dag);

    expect(result.size).toBe(3);
    expect(result.get(0)).toEqual(["0001"]);
    const phase1 = result.get(1) ?? [];
    expect(phase1).toContain("0002");
    expect(phase1).toContain("0003");
    expect(result.get(2)).toEqual(["0004"]);
  });

  it("computePhases_updatesNodePhaseField_nodesReflectPhase", () => {
    const dag = createDag([
      { id: "0001", blocks: ["0002"] },
      { id: "0002", blockedBy: ["0001"] },
    ]);
    computePhases(dag);

    expect(dag.get("0001")?.phase).toBe(0);
    expect(dag.get("0002")?.phase).toBe(1);
  });

  it("computePhases_nodeWithMissingDependency_throwsInvalidDagError", () => {
    const dag = createDag([
      { id: "0001" },
      { id: "0002", blockedBy: ["missing-node"] },
    ]);

    expect(() => computePhases(dag)).toThrow(InvalidDagError);
    expect(() => computePhases(dag)).toThrow(/unresolvable stories/);
    expect(() => computePhases(dag)).toThrow(/0002/);
  });
});
