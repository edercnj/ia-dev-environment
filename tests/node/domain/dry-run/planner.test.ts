import { describe, it, expect } from "vitest";
import type {
  ParsedMap,
  DryRunOptions,
  StoryNode,
  DryRunPhaseInfo,
  ExecutionState,
  StoryEntry,
} from "../../../../src/domain/dry-run/types.js";
import { buildDryRunPlan } from "../../../../src/domain/dry-run/planner.js";
import {
  formatPlan,
  formatStoryDetail,
} from "../../../../src/domain/dry-run/formatter.js";

const EPIC_ID = "EPIC-0042";

function emptyMap(): ParsedMap {
  return {
    stories: new Map(),
    phases: [],
    criticalPath: [],
  };
}

function makeStory(overrides: Partial<StoryNode>): StoryNode {
  return {
    id: "0042-0001",
    title: "Default Story",
    blockedBy: [],
    blocks: [],
    phase: 0,
    isOnCriticalPath: false,
    ...overrides,
  };
}

function defaultOptions(
  overrides: Partial<DryRunOptions> = {},
): DryRunOptions {
  return {
    resume: false,
    parallelMode: false,
    ...overrides,
  };
}

function storyId(n: number): string {
  return `0042-${String(n).padStart(4, "0")}`;
}

function build14StoryDag(): ParsedMap {
  const stories = new Map<string, StoryNode>();
  const defs = dag14Definitions();
  for (const d of defs) {
    stories.set(d.id, d);
  }
  const criticalIds = defs
    .filter((d) => d.isOnCriticalPath)
    .map((d) => d.id);
  return {
    stories,
    phases: [0, 1, 2, 3, 4],
    criticalPath: criticalIds,
  };
}

function dag14Definitions(): StoryNode[] {
  return [
    ...dag14Phase0(),
    ...dag14Phase1(),
    ...dag14Phase2(),
    ...dag14Phase3(),
    ...dag14Phase4(),
  ];
}

function dag14Phase0(): StoryNode[] {
  return [
    makeStory({ id: storyId(1), title: "S1", phase: 0, isOnCriticalPath: true, blocks: [storyId(4)] }),
    makeStory({ id: storyId(2), title: "S2", phase: 0, blocks: [storyId(5)] }),
    makeStory({ id: storyId(3), title: "S3", phase: 0, blocks: [storyId(6)] }),
  ];
}

function dag14Phase1(): StoryNode[] {
  return [
    makeStory({
      id: storyId(4), title: "S4", phase: 1,
      blockedBy: [storyId(1)], isOnCriticalPath: true,
      blocks: [storyId(7)],
    }),
    makeStory({
      id: storyId(5), title: "S5", phase: 1,
      blockedBy: [storyId(2)], blocks: [storyId(8)],
    }),
    makeStory({
      id: storyId(6), title: "S6", phase: 1,
      blockedBy: [storyId(3)], blocks: [storyId(9)],
    }),
  ];
}

function dag14Phase2(): StoryNode[] {
  return [
    makeStory({
      id: storyId(7), title: "S7", phase: 2,
      blockedBy: [storyId(4)], isOnCriticalPath: true,
      blocks: [storyId(10)],
    }),
    makeStory({
      id: storyId(8), title: "S8", phase: 2,
      blockedBy: [storyId(5)], blocks: [storyId(11)],
    }),
    makeStory({
      id: storyId(9), title: "S9", phase: 2,
      blockedBy: [storyId(6)], blocks: [storyId(12)],
    }),
  ];
}

function dag14Phase3(): StoryNode[] {
  return [
    makeStory({
      id: storyId(10), title: "S10", phase: 3,
      blockedBy: [storyId(7)], isOnCriticalPath: true,
      blocks: [storyId(13)],
    }),
    makeStory({
      id: storyId(11), title: "S11", phase: 3,
      blockedBy: [storyId(8)], blocks: [storyId(13)],
    }),
    makeStory({
      id: storyId(12), title: "S12", phase: 3,
      blockedBy: [storyId(9)], blocks: [storyId(14)],
    }),
  ];
}

function dag14Phase4(): StoryNode[] {
  return [
    makeStory({
      id: storyId(13), title: "S13", phase: 4,
      blockedBy: [storyId(10), storyId(11)],
      isOnCriticalPath: true,
    }),
    makeStory({
      id: storyId(14), title: "S14", phase: 4,
      blockedBy: [storyId(12)],
    }),
  ];
}

function makeExecutionState(
  entries: readonly StoryEntry[],
): ExecutionState {
  const stories = new Map<string, StoryEntry>();
  for (const entry of entries) {
    stories.set(entry.id, entry);
  }
  return { epicId: EPIC_ID, stories };
}

function assertPhaseStoryCounts(
  phases: readonly DryRunPhaseInfo[],
  expected: readonly number[],
): void {
  const actual = phases.map((p) => p.stories.length);
  expect(actual).toEqual(expected);
}

describe("buildDryRunPlan", () => {
  it("emptyMap_returnsEmptyPlan", () => {
    const plan = buildDryRunPlan(
      emptyMap(),
      EPIC_ID,
      defaultOptions(),
    );

    expect(plan.epicId).toBe(EPIC_ID);
    expect(plan.mode).toBe("full");
    expect(plan.totalStories).toBe(0);
    expect(plan.totalPhases).toBe(0);
    expect(plan.criticalPath).toEqual([]);
    expect(plan.phases).toEqual([]);
    expect(plan.storyDetail).toBeUndefined();
  });

  it("singleStorySinglePhase_returnsOnePhasePlan", () => {
    const story = makeStory({
      id: "0042-0001",
      title: "Setup project",
      phase: 0,
      isOnCriticalPath: true,
    });
    const parsed: ParsedMap = {
      stories: new Map([["0042-0001", story]]),
      phases: [0],
      criticalPath: ["0042-0001"],
    };

    const plan = buildDryRunPlan(
      parsed,
      EPIC_ID,
      defaultOptions(),
    );

    expect(plan.totalStories).toBe(1);
    expect(plan.totalPhases).toBe(1);
    expect(plan.phases).toHaveLength(1);
    const phase0 = plan.phases[0]!;
    expect(phase0.phase).toBe(0);
    expect(phase0.stories).toHaveLength(1);
    expect(phase0.stories[0]!.id).toBe("0042-0001");
    expect(phase0.stories[0]!.title).toBe("Setup project");
    expect(phase0.stories[0]!.status).toBe("PENDING");
    expect(phase0.stories[0]!.isCriticalPath).toBe(true);
    expect(phase0.stories[0]!.dependenciesSatisfied).toBe(true);
    expect(phase0.stories[0]!.blockedBy).toEqual([]);
  });

  it("linearChain_producesSequentialPhases", () => {
    const a = makeStory({
      id: "0042-0001",
      title: "Story A",
      phase: 0,
      blocks: ["0042-0002"],
      isOnCriticalPath: true,
    });
    const b = makeStory({
      id: "0042-0002",
      title: "Story B",
      phase: 1,
      blockedBy: ["0042-0001"],
      blocks: ["0042-0003"],
      isOnCriticalPath: true,
    });
    const c = makeStory({
      id: "0042-0003",
      title: "Story C",
      phase: 2,
      blockedBy: ["0042-0002"],
      isOnCriticalPath: true,
    });
    const parsed: ParsedMap = {
      stories: new Map([
        ["0042-0001", a],
        ["0042-0002", b],
        ["0042-0003", c],
      ]),
      phases: [0, 1, 2],
      criticalPath: ["0042-0001", "0042-0002", "0042-0003"],
    };

    const plan = buildDryRunPlan(
      parsed,
      EPIC_ID,
      defaultOptions(),
    );

    expect(plan.totalPhases).toBe(3);
    expect(plan.phases).toHaveLength(3);

    const p0 = plan.phases[0]!;
    expect(p0.stories).toHaveLength(1);
    expect(p0.stories[0]!.dependenciesSatisfied).toBe(true);

    const p1 = plan.phases[1]!;
    expect(p1.stories).toHaveLength(1);
    expect(p1.stories[0]!.dependenciesSatisfied).toBe(false);
    expect(p1.stories[0]!.blockedBy).toEqual(["0042-0001"]);

    const p2 = plan.phases[2]!;
    expect(p2.stories).toHaveLength(1);
    expect(p2.stories[0]!.isCriticalPath).toBe(true);
  });

  it("multipleStoriesSamePhase_groupedCorrectly", () => {
    const s1 = makeStory({
      id: "0042-0001",
      title: "Root A",
      phase: 0,
      blocks: ["0042-0003"],
    });
    const s2 = makeStory({
      id: "0042-0002",
      title: "Root B",
      phase: 0,
      blocks: ["0042-0003"],
    });
    const s3 = makeStory({
      id: "0042-0003",
      title: "Dependent C",
      phase: 1,
      blockedBy: ["0042-0001", "0042-0002"],
    });
    const parsed: ParsedMap = {
      stories: new Map([
        ["0042-0001", s1],
        ["0042-0002", s2],
        ["0042-0003", s3],
      ]),
      phases: [0, 1],
      criticalPath: [],
    };

    const plan = buildDryRunPlan(
      parsed,
      EPIC_ID,
      defaultOptions(),
    );

    expect(plan.phases[0]!.stories).toHaveLength(2);
    const ids = plan.phases[0]!.stories.map((s) => s.id);
    expect(ids).toContain("0042-0001");
    expect(ids).toContain("0042-0002");
    expect(plan.phases[1]!.stories).toHaveLength(1);
  });

  it("criticalPath_onlyMarkedStoriesTrue", () => {
    const s1 = makeStory({
      id: "0042-0001",
      title: "Critical Root",
      phase: 0,
      isOnCriticalPath: true,
    });
    const s2 = makeStory({
      id: "0042-0002",
      title: "Side Branch",
      phase: 0,
      isOnCriticalPath: false,
    });
    const s3 = makeStory({
      id: "0042-0003",
      title: "Critical Leaf",
      phase: 1,
      blockedBy: ["0042-0001"],
      isOnCriticalPath: true,
    });
    const parsed: ParsedMap = {
      stories: new Map([
        ["0042-0001", s1],
        ["0042-0002", s2],
        ["0042-0003", s3],
      ]),
      phases: [0, 1],
      criticalPath: ["0042-0001", "0042-0003"],
    };

    const plan = buildDryRunPlan(
      parsed,
      EPIC_ID,
      defaultOptions(),
    );

    const allStories = plan.phases.flatMap((p) => p.stories);
    const critical = allStories.filter((s) => s.isCriticalPath);
    const nonCritical = allStories.filter(
      (s) => !s.isCriticalPath,
    );

    expect(critical).toHaveLength(2);
    expect(nonCritical).toHaveLength(1);
    expect(nonCritical[0]!.id).toBe("0042-0002");
    expect(plan.criticalPath).toEqual([
      "0042-0001",
      "0042-0003",
    ]);
  });

  it("full14StoryDag_producesCorrectPlan", () => {
    const parsed = build14StoryDag();

    const plan = buildDryRunPlan(
      parsed,
      EPIC_ID,
      defaultOptions(),
    );

    expect(plan.totalStories).toBe(14);
    expect(plan.totalPhases).toBe(5);
    expect(plan.phases).toHaveLength(5);

    assertPhaseStoryCounts(plan.phases, [3, 3, 3, 3, 2]);
    expect(plan.criticalPath).toHaveLength(5);
  });

  it("resume_mergesCheckpointStatus", () => {
    const s1 = makeStory({
      id: "0042-0001",
      title: "Done Story",
      phase: 0,
    });
    const s2 = makeStory({
      id: "0042-0002",
      title: "Pending Story",
      phase: 0,
    });
    const parsed: ParsedMap = {
      stories: new Map([
        ["0042-0001", s1],
        ["0042-0002", s2],
      ]),
      phases: [0],
      criticalPath: [],
    };
    const state = makeExecutionState([
      { id: "0042-0001", status: "SUCCESS" },
    ]);

    const plan = buildDryRunPlan(
      parsed,
      EPIC_ID,
      defaultOptions({
        resume: true,
        executionState: state,
      }),
    );

    const stories = plan.phases[0]!.stories;
    const done = stories.find((s) => s.id === "0042-0001");
    const pending = stories.find((s) => s.id === "0042-0002");
    expect(done!.status).toBe("COMPLETED");
    expect(pending!.status).toBe("PENDING");
  });

  it("resume_mixedStatuses_correctlyCategorized", () => {
    const parsed = build14StoryDag();
    const successIds = [1, 2, 3, 4, 5].map(storyId);
    const entries: StoryEntry[] = successIds.map((id) => ({
      id,
      status: "SUCCESS" as const,
    }));
    const state = makeExecutionState(entries);

    const plan = buildDryRunPlan(
      parsed,
      EPIC_ID,
      defaultOptions({
        resume: true,
        executionState: state,
      }),
    );

    const allStories = plan.phases.flatMap((p) => p.stories);
    const completed = allStories.filter(
      (s) => s.status === "COMPLETED",
    );
    const pending = allStories.filter(
      (s) => s.status === "PENDING",
    );
    expect(completed).toHaveLength(5);
    expect(pending).toHaveLength(9);
  });

  it.each([
    ["SUCCESS", "COMPLETED"],
    ["FAILED", "FAILED"],
    ["IN_PROGRESS", "IN_PROGRESS"],
    ["BLOCKED", "BLOCKED"],
    ["PENDING", "PENDING"],
  ] as const)(
    "resume_status_%s_mapsTo_%s",
    (inputStatus, expectedOutput) => {
      const s = makeStory({
        id: "0042-0001",
        title: "S1",
        phase: 0,
      });
      const parsed: ParsedMap = {
        stories: new Map([["0042-0001", s]]),
        phases: [0],
        criticalPath: [],
      };
      const state = makeExecutionState([
        { id: "0042-0001", status: inputStatus },
      ]);

      const plan = buildDryRunPlan(
        parsed,
        EPIC_ID,
        defaultOptions({
          resume: true,
          executionState: state,
        }),
      );

      expect(plan.phases[0]!.stories[0]!.status).toBe(
        expectedOutput,
      );
    },
  );

  it("phaseFilter_returnsOnlyFilteredPhase", () => {
    const parsed = build14StoryDag();

    const plan = buildDryRunPlan(
      parsed,
      EPIC_ID,
      defaultOptions({ phaseFilter: 1 }),
    );

    expect(plan.mode).toBe("phase");
    expect(plan.phases).toHaveLength(1);
    expect(plan.phases[0]!.phase).toBe(1);
    expect(plan.phases[0]!.stories).toHaveLength(3);
    expect(plan.totalStories).toBe(14);
    expect(plan.totalPhases).toBe(5);
  });

  it("phaseFilter_dependencySatisfaction", () => {
    const a = makeStory({
      id: "0042-0001",
      title: "Root",
      phase: 0,
      blocks: ["0042-0002"],
    });
    const b = makeStory({
      id: "0042-0002",
      title: "Dep",
      phase: 1,
      blockedBy: ["0042-0001"],
      blocks: ["0042-0003"],
    });
    const c = makeStory({
      id: "0042-0003",
      title: "No Dep",
      phase: 1,
    });
    const parsed: ParsedMap = {
      stories: new Map([
        ["0042-0001", a],
        ["0042-0002", b],
        ["0042-0003", c],
      ]),
      phases: [0, 1],
      criticalPath: [],
    };

    const plan = buildDryRunPlan(
      parsed,
      EPIC_ID,
      defaultOptions({ phaseFilter: 1 }),
    );

    const dep = plan.phases[0]!.stories.find(
      (s) => s.id === "0042-0002",
    );
    const noDep = plan.phases[0]!.stories.find(
      (s) => s.id === "0042-0003",
    );
    expect(dep!.dependenciesSatisfied).toBe(false);
    expect(noDep!.dependenciesSatisfied).toBe(true);
  });

  it("phaseFilter_invalidPhase_throwsError", () => {
    const parsed = build14StoryDag();

    expect(() =>
      buildDryRunPlan(
        parsed,
        EPIC_ID,
        defaultOptions({ phaseFilter: 99 }),
      ),
    ).toThrow(/phase 99/i);

    expect(() =>
      buildDryRunPlan(
        parsed,
        EPIC_ID,
        defaultOptions({ phaseFilter: -1 }),
      ),
    ).toThrow(/phase -1/i);
  });

  it("parallelMode_showsParallelCount", () => {
    const parsed = build14StoryDag();

    const plan = buildDryRunPlan(
      parsed,
      EPIC_ID,
      defaultOptions({ parallelMode: true }),
    );

    expect(plan.phases[0]!.parallelCount).toBe(3);
    expect(plan.phases[1]!.parallelCount).toBe(3);
    expect(plan.phases[4]!.parallelCount).toBe(2);
  });

  it("parallelMode_excludesSamePhaseBlockedStories", () => {
    const a = makeStory({
      id: "0042-0001",
      title: "Independent A",
      phase: 1,
      blocks: ["0042-0003"],
    });
    const b = makeStory({
      id: "0042-0002",
      title: "Independent B",
      phase: 1,
    });
    const c = makeStory({
      id: "0042-0003",
      title: "Blocked by same-phase",
      phase: 1,
      blockedBy: ["0042-0001"],
    });
    const parsed: ParsedMap = {
      stories: new Map([
        ["0042-0001", a],
        ["0042-0002", b],
        ["0042-0003", c],
      ]),
      phases: [1],
      criticalPath: [],
    };

    const plan = buildDryRunPlan(
      parsed,
      EPIC_ID,
      defaultOptions({ parallelMode: true }),
    );

    expect(plan.phases[0]!.stories).toHaveLength(3);
    expect(plan.phases[0]!.parallelCount).toBe(2);
  });

  it("storyFilter_returnsStoryDetail", () => {
    const parsed = build14StoryDag();
    const targetId = storyId(5);

    const plan = buildDryRunPlan(
      parsed,
      EPIC_ID,
      defaultOptions({ storyFilter: targetId }),
    );

    expect(plan.mode).toBe("story");
    expect(plan.storyDetail).toBeDefined();
    expect(plan.storyDetail!.id).toBe(targetId);
    expect(plan.storyDetail!.title).toBe("S5");
    expect(plan.storyDetail!.phase).toBe(1);
    expect(plan.storyDetail!.status).toBe("PENDING");
  });

  it("storyFilter_upstreamDownstream", () => {
    const parsed = build14StoryDag();
    const targetId = storyId(5);

    const plan = buildDryRunPlan(
      parsed,
      EPIC_ID,
      defaultOptions({ storyFilter: targetId }),
    );

    expect(plan.storyDetail!.dependencies).toEqual([
      storyId(2),
    ]);
    expect(plan.storyDetail!.dependents).toEqual([storyId(8)]);
  });

  it("storyFilter_nonExistent_throwsError", () => {
    const parsed = build14StoryDag();

    expect(() =>
      buildDryRunPlan(
        parsed,
        EPIC_ID,
        defaultOptions({ storyFilter: "9999-9999" }),
      ),
    ).toThrow(/9999-9999/);
  });
});

describe("acceptance: planner -> formatter E2E", () => {
  it("AT-1: complete plan renders all stories", () => {
    const parsed = build14StoryDag();
    const plan = buildDryRunPlan(
      parsed,
      EPIC_ID,
      defaultOptions(),
    );
    const output = formatPlan(plan);

    expect(output).toContain("14 stories");
    expect(output).toContain("5 phases");
    expect(output).toContain("Phase 0");
    expect(output).toContain("Phase 4");
    expect(output).toContain("[CRITICAL]");
    expect(output).toContain(storyId(1));
    expect(output).toContain(storyId(14));
  });

  it("AT-2: resume plan shows status markers", () => {
    const parsed = build14StoryDag();
    const entries: StoryEntry[] = [1, 2, 3, 4, 5].map(
      (n) => ({
        id: storyId(n),
        status: "SUCCESS" as const,
      }),
    );
    const state = makeExecutionState(entries);
    const plan = buildDryRunPlan(
      parsed,
      EPIC_ID,
      defaultOptions({
        resume: true,
        executionState: state,
      }),
    );
    const output = formatPlan(plan);

    expect(output).toContain("[COMPLETED]");
    expect(output).toContain("[PENDING]");
  });

  it("AT-3: phase filter renders only one phase", () => {
    const parsed = build14StoryDag();
    const plan = buildDryRunPlan(
      parsed,
      EPIC_ID,
      defaultOptions({ phaseFilter: 2 }),
    );
    const output = formatPlan(plan);

    expect(output).toContain("mode: phase");
    expect(output).toContain("Phase 2");
    expect(output).not.toContain("Phase 0 (");
    expect(output).not.toContain("Phase 1 (");
  });

  it("AT-4: parallel mode shows count", () => {
    const parsed = build14StoryDag();
    const plan = buildDryRunPlan(
      parsed,
      EPIC_ID,
      defaultOptions({ parallelMode: true }),
    );
    const output = formatPlan(plan);

    expect(output).toContain("parallel: 3");
  });

  it("AT-5: story filter renders detail", () => {
    const parsed = build14StoryDag();
    const plan = buildDryRunPlan(
      parsed,
      EPIC_ID,
      defaultOptions({ storyFilter: storyId(5) }),
    );
    const detail = plan.storyDetail!;
    const output = formatStoryDetail(detail);

    expect(output).toContain(storyId(5));
    expect(output).toContain("S5");
    expect(output).toContain("Phase: 1");
    expect(output).toContain(storyId(2));
    expect(output).toContain(storyId(8));
  });
});
