import { describe, expect, it } from "vitest";

import { parseImplementationMap } from "../../../../src/domain/implementation-map/index.js";
import {
  BRANCH,
  buildMiniImplementationMap,
  EPIC_ID,
  PHASE_STORIES,
  STORY_IDS,
  TOTAL_PHASES,
  TOTAL_STORIES,
} from "./mini-implementation-map.js";

describe("buildMiniImplementationMap", () => {
  it("buildMap_returns_nonEmptyString", () => {
    const map = buildMiniImplementationMap();
    expect(map.length).toBeGreaterThan(0);
  });

  it("buildMap_containsSectionHeader_dependencyMatrix", () => {
    const map = buildMiniImplementationMap();
    expect(map).toContain("## 1.");
  });

  it("buildMap_containsAllStoryIds", () => {
    const map = buildMiniImplementationMap();
    for (const id of STORY_IDS) {
      expect(map).toContain(id);
    }
  });

  it("buildMap_parsesSuccessfully_withParseImplementationMap", () => {
    const map = buildMiniImplementationMap();
    const parsed = parseImplementationMap(map);
    expect(parsed.stories.size).toBe(TOTAL_STORIES);
    expect(parsed.totalPhases).toBe(TOTAL_PHASES);
  });

  it("buildMap_parsedPhases_matchExpectedPhaseStories", () => {
    const parsed = parseImplementationMap(buildMiniImplementationMap());

    for (const [phase, expectedStories] of PHASE_STORIES) {
      const actualStories = parsed.phases.get(phase);
      expect(actualStories).toBeDefined();
      for (const id of expectedStories) {
        expect(actualStories).toContain(id);
      }
    }
  });

  it("buildMap_parsedDag_hasNoWarnings", () => {
    const parsed = parseImplementationMap(buildMiniImplementationMap());
    expect(parsed.warnings).toHaveLength(0);
  });

  it("buildMap_parsedDag_symmetryIsCorrect", () => {
    const parsed = parseImplementationMap(buildMiniImplementationMap());

    for (const [id, node] of parsed.stories) {
      for (const blockedId of node.blocks) {
        const blockedNode = parsed.stories.get(blockedId);
        expect(blockedNode?.blockedBy).toContain(id);
      }
      for (const depId of node.blockedBy) {
        const depNode = parsed.stories.get(depId);
        expect(depNode?.blocks).toContain(id);
      }
    }
  });

  it("buildMap_rootNode_hasNoDependencies", () => {
    const parsed = parseImplementationMap(buildMiniImplementationMap());
    const root = parsed.stories.get("story-e2e-0001")!;
    expect(root.blockedBy).toHaveLength(0);
    expect(root.phase).toBe(0);
  });

  it("buildMap_leafNodes_haveNoBlocks", () => {
    const parsed = parseImplementationMap(buildMiniImplementationMap());
    const story4 = parsed.stories.get("story-e2e-0004")!;
    const story5 = parsed.stories.get("story-e2e-0005")!;
    expect(story4.blocks).toHaveLength(0);
    expect(story5.blocks).toHaveLength(0);
  });
});

describe("module constants", () => {
  it("STORY_IDS_has5Elements", () => {
    expect(STORY_IDS).toHaveLength(5);
  });

  it("TOTAL_STORIES_equals5", () => {
    expect(TOTAL_STORIES).toBe(5);
  });

  it("TOTAL_PHASES_equals3", () => {
    expect(TOTAL_PHASES).toBe(3);
  });

  it("EPIC_ID_isNonEmpty", () => {
    expect(EPIC_ID.length).toBeGreaterThan(0);
  });

  it("BRANCH_isNonEmpty", () => {
    expect(BRANCH.length).toBeGreaterThan(0);
  });

  it("PHASE_STORIES_has3Entries", () => {
    expect(PHASE_STORIES.size).toBe(3);
  });
});
