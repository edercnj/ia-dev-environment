import { readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, it, expect } from "vitest";

import {
  parseImplementationMap,
  getExecutableStories,
  CircularDependencyError,
  StoryStatus,
} from "../../../src/domain/implementation-map/index.js";
import { readFixture, createExecutionState } from "./helpers.js";

describe("parseImplementationMap (Acceptance Tests)", () => {
  it("AT-01: empty map returns empty ParsedMap with totalPhases 0", () => {
    const content = readFixture("empty-map.md");
    const result = parseImplementationMap(content);

    expect(result.stories.size).toBe(0);
    expect(result.phases.size).toBe(0);
    expect(result.totalPhases).toBe(0);
    expect(result.criticalPath).toEqual([]);
    expect(result.warnings).toEqual([]);
  });

  it("AT-02: single root story in phase 0 with criticalPath of length 1", () => {
    const content = readFixture("single-story.md");
    const result = parseImplementationMap(content);

    expect(result.stories.size).toBe(1);
    const story = result.stories.get("story-0042-0001");
    expect(story).toBeDefined();
    expect(story?.phase).toBe(0);
    expect(story?.blockedBy).toEqual([]);
    expect(result.phases.get(0)).toEqual(["story-0042-0001"]);
    expect(result.criticalPath).toEqual(["story-0042-0001"]);
  });

  it("AT-03: linear dependencies produce N phases and full chain as critical path", () => {
    const content = readFixture("linear-chain.md");
    const result = parseImplementationMap(content);

    expect(result.totalPhases).toBe(3);
    expect(result.phases.get(0)).toEqual(["story-0042-0001"]);
    expect(result.phases.get(1)).toEqual(["story-0042-0002"]);
    expect(result.phases.get(2)).toEqual(["story-0042-0003"]);
    expect(result.criticalPath).toEqual([
      "story-0042-0001",
      "story-0042-0002",
      "story-0042-0003",
    ]);
  });

  it("AT-04: parallelism puts roots in phase 0 and dependent in phase 1", () => {
    const content = readFixture("parallel-roots.md");
    const result = parseImplementationMap(content);

    const phase0 = result.phases.get(0) ?? [];
    expect(phase0).toContain("story-0042-0001");
    expect(phase0).toContain("story-0042-0002");
    expect(result.phases.get(1)).toEqual(["story-0042-0003"]);
    expect(result.criticalPath).toContain("story-0042-0003");
  });

  it("AT-05: asymmetric dependency produces warning and auto-corrects", () => {
    const content = readFixture("asymmetric-map.md");
    const result = parseImplementationMap(content);

    expect(result.warnings.length).toBeGreaterThanOrEqual(1);
    expect(result.warnings.some(
      (w) => w.type === "asymmetric-dependency",
    )).toBe(true);
    const story0002 = result.stories.get("story-0042-0002");
    expect(story0002?.blockedBy).toContain("story-0042-0001");
  });

  it("AT-06: cyclic dependency throws CircularDependencyError", () => {
    const content = readFixture("cyclic-map.md");

    expect(() => parseImplementationMap(content)).toThrow(
      CircularDependencyError,
    );
    try {
      parseImplementationMap(content);
    } catch (error) {
      const err = error as CircularDependencyError;
      expect(err.message).toContain("story-0042-0001");
      expect(err.message).toContain("story-0042-0002");
      expect(err.message).toContain("story-0042-0003");
    }
  });

  it("AT-07: executable stories with partial state returns satisfied deps only", () => {
    const content = readFixture("five-stories-three-phases.md");
    const parsedMap = parseImplementationMap(content);
    const state = createExecutionState({
      "story-0042-0001": StoryStatus.SUCCESS,
      "story-0042-0002": StoryStatus.SUCCESS,
      "story-0042-0003": StoryStatus.PENDING,
      "story-0042-0004": StoryStatus.PENDING,
      "story-0042-0005": StoryStatus.PENDING,
    });

    const result = getExecutableStories(parsedMap, state);
    expect(result).toContain("story-0042-0003");
    expect(result).toContain("story-0042-0004");
    expect(result).not.toContain("story-0042-0005");
  });

  it("AT-08: critical path stories appear before non-critical-path stories", () => {
    const content = readFixture("five-stories-three-phases.md");
    const parsedMap = parseImplementationMap(content);
    const state = createExecutionState({
      "story-0042-0001": StoryStatus.SUCCESS,
      "story-0042-0002": StoryStatus.SUCCESS,
      "story-0042-0003": StoryStatus.PENDING,
      "story-0042-0004": StoryStatus.PENDING,
      "story-0042-0005": StoryStatus.PENDING,
    });

    const result = getExecutableStories(parsedMap, state);
    const idx0003 = result.indexOf("story-0042-0003");
    const idx0004 = result.indexOf("story-0042-0004");
    expect(idx0003).toBeLessThan(idx0004);
  });
});

describe("parseImplementationMap (Integration Tests)", () => {
  const EPIC_0005_MAP_PATH = join(
    import.meta.dirname,
    "..",
    "..",
    "..",
    "docs",
    "stories",
    "epic-0005",
    "IMPLEMENTATION-MAP.md",
  );

  const EPIC_0004_MAP_PATH = join(
    import.meta.dirname,
    "..",
    "..",
    "..",
    "docs",
    "stories",
    "epic-0004",
    "IMPLEMENTATION-MAP.md",
  );

  it("IT-01: epic-0005 real map extracts 14 stories in 6 phases", () => {
    const content = readFileSync(EPIC_0005_MAP_PATH, "utf-8");
    const result = parseImplementationMap(content);

    expect(result.stories.size).toBe(14);
    expect(result.totalPhases).toBe(6);

    const phase0 = result.phases.get(0) ?? [];
    expect(phase0).toContain("story-0005-0001");
    expect(phase0).toContain("story-0005-0002");
    expect(phase0).toContain("story-0005-0003");
    expect(phase0).toHaveLength(3);

    const phase3 = result.phases.get(3) ?? [];
    expect(phase3).toHaveLength(6);

    expect(result.criticalPath).toContain("story-0005-0001");
    expect(result.criticalPath).toContain("story-0005-0004");
    expect(result.criticalPath).toContain("story-0005-0005");
    expect(result.criticalPath).toContain("story-0005-0007");
    expect(result.criticalPath).toContain("story-0005-0010");
    expect(result.criticalPath).toContain("story-0005-0014");

    expect(result.warnings).toEqual([]);
  });

  it("IT-02: epic-0004 real map extracts 17 stories in 4 phases", () => {
    const content = readFileSync(EPIC_0004_MAP_PATH, "utf-8");
    const result = parseImplementationMap(content);

    expect(result.stories.size).toBe(17);
    expect(result.totalPhases).toBe(4);

    const phase0 = result.phases.get(0) ?? [];
    expect(phase0).toHaveLength(5);

    const story0005 = result.stories.get("story-0004-0005");
    expect(story0005?.blocks.length).toBe(7);

    expect(result.criticalPath).toContain("story-0004-0013");
    expect(result.criticalPath).toContain("story-0004-0017");
    expect(result.criticalPath.length).toBeGreaterThanOrEqual(4);

    expect(result.warnings).toEqual([]);
  });

  it("IT-03: epic-0005 initial state returns phase 0 stories as executable", () => {
    const content = readFileSync(EPIC_0005_MAP_PATH, "utf-8");
    const parsedMap = parseImplementationMap(content);

    const allStoryIds = [...parsedMap.stories.keys()];
    const storyStatuses: Record<string, StoryStatus> = {};
    for (const id of allStoryIds) {
      storyStatuses[id] = StoryStatus.PENDING;
    }
    const state = createExecutionState(storyStatuses, "epic-0005");

    const result = getExecutableStories(parsedMap, state);
    expect(result).toContain("story-0005-0001");
    expect(result).toContain("story-0005-0002");
    expect(result).toContain("story-0005-0003");
    expect(result).toHaveLength(3);
    expect(result[0]).toBe("story-0005-0001");
  });
});
