import { describe, it, expect } from "vitest";

import { propagateBlocks } from "../../../src/domain/failure/block-propagator.js";
import {
  propagateBlocks as propagateBlocksBarrel,
  MAX_RETRIES as maxRetriesBarrel,
  evaluateRetry as evaluateRetryBarrel,
} from "../../../src/domain/failure/index.js";
import { createDag } from "../implementation-map/helpers.js";

describe("barrel exports", () => {
  it("barrelExports_allPublicSymbols_resolveCorrectly", () => {
    expect(propagateBlocksBarrel).toBe(propagateBlocks);
    expect(typeof evaluateRetryBarrel).toBe("function");
    expect(maxRetriesBarrel).toBe(2);
  });
});

describe("propagateBlocks", () => {
  // --- Cycle 3.1: Degenerate cases (UT-10, UT-11) ---

  it("propagateBlocks_storyNotInDag_returnsEmptyBlockedStories", () => {
    const dag = createDag([
      { id: "A", blocks: [] },
    ]);

    const result = propagateBlocks("unknown-story", dag);

    expect(result.blockedStories).toEqual([]);
  });

  it("propagateBlocks_storyWithNoDependents_returnsEmptyBlockedStories", () => {
    const dag = createDag([
      { id: "A", blocks: [] },
    ]);

    const result = propagateBlocks("A", dag);

    expect(result.blockedStories).toEqual([]);
  });

  // --- Cycle 3.2: Direct dependent (UT-12, UT-13, UT-19) ---

  it("propagateBlocks_singleDirectDependent_returnsOneBlocked", () => {
    const dag = createDag([
      { id: "A", blocks: ["B"] },
      { id: "B", blockedBy: ["A"], blocks: [] },
    ]);

    const result = propagateBlocks("A", dag);

    expect(result.blockedStories).toHaveLength(1);
    expect(result.blockedStories[0].storyId).toBe("B");
  });

  it("propagateBlocks_singleDirectDependent_blockedByContainsFailedStory", () => {
    const dag = createDag([
      { id: "A", blocks: ["B"] },
      { id: "B", blockedBy: ["A"], blocks: [] },
    ]);

    const result = propagateBlocks("A", dag);

    expect(result.blockedStories[0].blockedBy).toEqual(["A"]);
  });

  it("propagateBlocks_failedStoryId_isInResult", () => {
    const dag = createDag([
      { id: "A", blocks: ["B"] },
      { id: "B", blockedBy: ["A"], blocks: [] },
    ]);

    const result = propagateBlocks("A", dag);

    expect(result.failedStory).toBe("A");
  });

  // --- Cycle 3.3: Transitive chain (UT-14, UT-15) ---

  it("propagateBlocks_transitiveChain_returnsAllTransitivelyBlocked", () => {
    const dag = createDag([
      { id: "A", blocks: ["B"] },
      { id: "B", blockedBy: ["A"], blocks: ["C"] },
      { id: "C", blockedBy: ["B"], blocks: [] },
    ]);

    const result = propagateBlocks("A", dag);
    const blockedIds = result.blockedStories.map((e) => e.storyId);

    expect(blockedIds).toContain("B");
    expect(blockedIds).toContain("C");
    expect(result.blockedStories).toHaveLength(2);
  });

  it("propagateBlocks_transitiveChain_blockedByChainIsCorrect", () => {
    const dag = createDag([
      { id: "A", blocks: ["B"] },
      { id: "B", blockedBy: ["A"], blocks: ["C"] },
      { id: "C", blockedBy: ["B"], blocks: [] },
    ]);

    const result = propagateBlocks("A", dag);
    const entryB = result.blockedStories.find((e) => e.storyId === "B");
    const entryC = result.blockedStories.find((e) => e.storyId === "C");

    expect(entryB?.blockedBy).toEqual(["A"]);
    expect(entryC?.blockedBy).toEqual(["B"]);
  });

  // --- Cycle 3.4: Complex topologies (UT-16, UT-17, UT-18) ---

  it("propagateBlocks_diamondDag_returnsAllDependentsOnce", () => {
    const dag = createDag([
      { id: "A", blocks: ["B", "C"] },
      { id: "B", blockedBy: ["A"], blocks: ["D"] },
      { id: "C", blockedBy: ["A"], blocks: ["D"] },
      { id: "D", blockedBy: ["B", "C"], blocks: [] },
    ]);

    const result = propagateBlocks("A", dag);
    const blockedIds = result.blockedStories.map((e) => e.storyId);

    expect(blockedIds).toContain("B");
    expect(blockedIds).toContain("C");
    expect(blockedIds).toContain("D");
    // D appears exactly once (no duplicates)
    expect(blockedIds.filter((id) => id === "D")).toHaveLength(1);
    expect(result.blockedStories).toHaveLength(3);
  });

  it("propagateBlocks_fanOut_returnsAllDirectDependents", () => {
    const dag = createDag([
      { id: "A", blocks: ["B", "C", "D"] },
      { id: "B", blockedBy: ["A"], blocks: [] },
      { id: "C", blockedBy: ["A"], blocks: [] },
      { id: "D", blockedBy: ["A"], blocks: [] },
    ]);

    const result = propagateBlocks("A", dag);
    const blockedIds = result.blockedStories.map((e) => e.storyId);

    expect(blockedIds).toContain("B");
    expect(blockedIds).toContain("C");
    expect(blockedIds).toContain("D");
    expect(result.blockedStories).toHaveLength(3);
  });

  it("propagateBlocks_unrelatedStoriesNotAffected_notIncluded", () => {
    const dag = createDag([
      { id: "A", blocks: ["B"] },
      { id: "B", blockedBy: ["A"], blocks: [] },
      { id: "E", blocks: [] },
    ]);

    const result = propagateBlocks("A", dag);
    const blockedIds = result.blockedStories.map((e) => e.storyId);

    expect(blockedIds).not.toContain("E");
    expect(result.blockedStories).toHaveLength(1);
  });

  // --- Cycle 3.5: Acceptance test (AT-2) ---

  it("blockPropagation_transitiveGraph_blocksAllDependentsCorrectly", () => {
    // Complex DAG:
    // A -> B -> D -> E
    // A -> C -> D
    // F (unrelated)
    const dag = createDag([
      { id: "A", blocks: ["B", "C"] },
      { id: "B", blockedBy: ["A"], blocks: ["D"] },
      { id: "C", blockedBy: ["A"], blocks: ["D"] },
      { id: "D", blockedBy: ["B", "C"], blocks: ["E"] },
      { id: "E", blockedBy: ["D"], blocks: [] },
      { id: "F", blocks: [] },
    ]);

    const result = propagateBlocks("A", dag);
    const blockedIds = result.blockedStories.map((e) => e.storyId);

    // All dependents blocked
    expect(blockedIds).toContain("B");
    expect(blockedIds).toContain("C");
    expect(blockedIds).toContain("D");
    expect(blockedIds).toContain("E");

    // Unrelated not blocked
    expect(blockedIds).not.toContain("F");

    // Failed story itself not in blocked list
    expect(blockedIds).not.toContain("A");

    // No duplicates
    expect(new Set(blockedIds).size).toBe(blockedIds.length);

    // Result carries the failed story ID
    expect(result.failedStory).toBe("A");
    expect(result.blockedStories).toHaveLength(4);
  });
});
