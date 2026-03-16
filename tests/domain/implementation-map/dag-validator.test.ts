import { describe, it, expect } from "vitest";

import {
  validateSymmetry,
  detectCycles,
  validateRoots,
} from "../../../src/domain/implementation-map/dag-validator.js";
import {
  CircularDependencyError,
  InvalidDagError,
} from "../../../src/domain/implementation-map/types.js";
import { createDag } from "./helpers.js";

describe("validateSymmetry", () => {
  it("validateSymmetry_emptyDag_returnsNoWarnings", () => {
    const dag = new Map();
    const warnings = validateSymmetry(dag);
    expect(warnings).toEqual([]);
  });

  it("validateSymmetry_symmetricDag_returnsNoWarnings", () => {
    const dag = createDag([
      { id: "0001", blocks: ["0002"] },
      { id: "0002", blockedBy: ["0001"] },
    ]);
    const warnings = validateSymmetry(dag);
    expect(warnings).toEqual([]);
  });

  it("validateSymmetry_asymmetricBlocksMissing_returnsWarningAndCorrects", () => {
    const dag = createDag([
      { id: "0001", blocks: ["0002"] },
      { id: "0002" },
    ]);

    const warnings = validateSymmetry(dag);

    expect(warnings).toHaveLength(1);
    expect(warnings[0]?.type).toBe("asymmetric-dependency");
    expect(dag.get("0002")?.blockedBy).toContain("0001");
  });

  it("validateSymmetry_asymmetricBlockedByMissing_returnsWarningAndCorrects", () => {
    const dag = createDag([
      { id: "0001" },
      { id: "0002", blockedBy: ["0001"] },
    ]);

    const warnings = validateSymmetry(dag);

    expect(warnings).toHaveLength(1);
    expect(warnings[0]?.type).toBe("asymmetric-dependency");
    expect(dag.get("0001")?.blocks).toContain("0002");
  });

  it("validateSymmetry_multipleAsymmetries_returnsMultipleWarnings", () => {
    const dag = createDag([
      { id: "0001", blocks: ["0002", "0003"] },
      { id: "0002" },
      { id: "0003" },
    ]);

    const warnings = validateSymmetry(dag);

    expect(warnings).toHaveLength(2);
    expect(warnings.every((w) => w.type === "asymmetric-dependency")).toBe(true);
  });
});

describe("detectCycles", () => {
  it("detectCycles_emptyDag_doesNotThrow", () => {
    const dag = new Map();
    expect(() => detectCycles(dag)).not.toThrow();
  });

  it("detectCycles_acyclicDag_doesNotThrow", () => {
    const dag = createDag([
      { id: "0001", blocks: ["0002"] },
      { id: "0002", blockedBy: ["0001"], blocks: ["0003"] },
      { id: "0003", blockedBy: ["0002"] },
    ]);
    expect(() => detectCycles(dag)).not.toThrow();
  });

  it("detectCycles_directCycleTwoNodes_throwsWithCycleChain", () => {
    const dag = createDag([
      { id: "0001", blocks: ["0002"], blockedBy: ["0002"] },
      { id: "0002", blocks: ["0001"], blockedBy: ["0001"] },
    ]);

    expect(() => detectCycles(dag)).toThrow(CircularDependencyError);
    try {
      detectCycles(dag);
    } catch (error) {
      const err = error as CircularDependencyError;
      expect(err.message).toContain("0001");
      expect(err.message).toContain("0002");
    }
  });

  it("detectCycles_indirectCycleThreeNodes_throwsWithFullChain", () => {
    const dag = createDag([
      { id: "0001", blocks: ["0002"], blockedBy: ["0003"] },
      { id: "0002", blocks: ["0003"], blockedBy: ["0001"] },
      { id: "0003", blocks: ["0001"], blockedBy: ["0002"] },
    ]);

    expect(() => detectCycles(dag)).toThrow(CircularDependencyError);
    try {
      detectCycles(dag);
    } catch (error) {
      const err = error as CircularDependencyError;
      expect(err.message).toContain("0001");
      expect(err.message).toContain("0002");
      expect(err.message).toContain("0003");
    }
  });
});

describe("validateRoots", () => {
  it("validateRoots_emptyDag_doesNotThrow", () => {
    const dag = new Map();
    expect(() => validateRoots(dag)).not.toThrow();
  });

  it("validateRoots_dagWithRoots_doesNotThrow", () => {
    const dag = createDag([
      { id: "0001" },
      { id: "0002", blockedBy: ["0001"] },
    ]);
    expect(() => validateRoots(dag)).not.toThrow();
  });

  it("validateRoots_dagWithNoRoots_throwsInvalidDagError", () => {
    const dag = createDag([
      { id: "0001", blockedBy: ["0002"] },
      { id: "0002", blockedBy: ["0001"] },
    ]);
    expect(() => validateRoots(dag)).toThrow(InvalidDagError);
  });
});

describe("validateSymmetry edge cases", () => {
  it("validateSymmetry_blocksReferencesNonexistentNode_noWarning", () => {
    const dag = createDag([
      { id: "0001", blocks: ["nonexistent"] },
    ]);
    const warnings = validateSymmetry(dag);
    expect(warnings).toEqual([]);
  });

  it("validateSymmetry_blockedByReferencesNonexistentNode_noWarning", () => {
    const dag = createDag([
      { id: "0001", blockedBy: ["nonexistent"] },
    ]);
    const warnings = validateSymmetry(dag);
    expect(warnings).toEqual([]);
  });
});

describe("detectCycles edge cases", () => {
  it("detectCycles_nodeBlocksNonexistentNode_doesNotThrow", () => {
    const dag = createDag([
      { id: "0001", blocks: ["nonexistent"] },
    ]);
    expect(() => detectCycles(dag)).not.toThrow();
  });

  it("detectCycles_multipleDisconnectedComponents_doesNotThrow", () => {
    const dag = createDag([
      { id: "0001", blocks: ["0002"] },
      { id: "0002", blockedBy: ["0001"] },
      { id: "0003", blocks: ["0004"] },
      { id: "0004", blockedBy: ["0003"] },
    ]);
    expect(() => detectCycles(dag)).not.toThrow();
  });
});
