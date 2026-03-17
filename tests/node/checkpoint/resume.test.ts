import { describe, it, expect } from "vitest";
import {
  reclassifyStories,
  reevaluateBlocked,
  prepareResume,
} from "../../../src/checkpoint/resume.js";
import {
  MAX_RETRIES,
  StoryStatus,
} from "../../../src/checkpoint/types.js";
import type {
  StoryEntry,
  ExecutionState,
  ExecutionMode,
  ReclassificationEntry,
} from "../../../src/checkpoint/types.js";

// --- Helpers ---

function aStoryEntry(
  overrides?: Partial<StoryEntry>,
): StoryEntry {
  return {
    status: "PENDING",
    phase: 1,
    retries: 0,
    ...overrides,
  };
}

const DEFAULT_MODE: ExecutionMode = {
  parallel: false,
  skipReview: false,
};

function anExecutionState(
  overrides?: Partial<ExecutionState>,
): ExecutionState {
  return {
    epicId: "0042",
    branch: "feat/epic-0042",
    startedAt: "2026-01-01T00:00:00Z",
    currentPhase: 1,
    mode: DEFAULT_MODE,
    stories: {},
    integrityGates: {},
    metrics: {
      storiesCompleted: 0,
      storiesTotal: 0,
    },
    ...overrides,
  };
}

// --- reclassifyStories ---

describe("reclassifyStories", () => {
  it("reclassifyStories_emptyMap_returnsEmptyMap", () => {
    const result = reclassifyStories({}, MAX_RETRIES);
    expect(result.stories).toEqual({});
    expect(result.reclassified).toEqual([]);
  });

  it("reclassifyStories_singlePending_noChange", () => {
    const stories = { "0042-0001": aStoryEntry() };
    const result = reclassifyStories(stories, MAX_RETRIES);
    expect(result.stories["0042-0001"].status).toBe("PENDING");
    expect(result.reclassified).toEqual([]);
  });

  it("reclassifyStories_singleSuccess_preserved", () => {
    const stories = {
      "0042-0001": aStoryEntry({ status: "SUCCESS" }),
    };
    const result = reclassifyStories(stories, MAX_RETRIES);
    expect(result.stories["0042-0001"].status).toBe("SUCCESS");
    expect(result.reclassified).toEqual([]);
  });

  it("reclassifyStories_singleInProgress_becomesPending", () => {
    const stories = {
      "0042-0001": aStoryEntry({ status: "IN_PROGRESS" }),
    };
    const result = reclassifyStories(stories, MAX_RETRIES);
    expect(result.stories["0042-0001"].status).toBe("PENDING");
    expect(result.reclassified).toEqual([
      {
        storyId: "0042-0001",
        from: "IN_PROGRESS",
        to: "PENDING",
      },
    ]);
  });

  it("reclassifyStories_failedBelowMaxRetries_becomesPending", () => {
    const stories = {
      "0042-0001": aStoryEntry({
        status: "FAILED",
        retries: 1,
      }),
    };
    const result = reclassifyStories(stories, MAX_RETRIES);
    expect(result.stories["0042-0001"].status).toBe("PENDING");
    expect(result.reclassified).toHaveLength(1);
    expect(result.reclassified[0]).toEqual({
      storyId: "0042-0001",
      from: "FAILED",
      to: "PENDING",
    });
  });

  it("reclassifyStories_failedAtMaxRetries_staysFailed", () => {
    const stories = {
      "0042-0001": aStoryEntry({
        status: "FAILED",
        retries: MAX_RETRIES,
      }),
    };
    const result = reclassifyStories(stories, MAX_RETRIES);
    expect(result.stories["0042-0001"].status).toBe("FAILED");
    expect(result.reclassified).toEqual([]);
  });

  it("reclassifyStories_failedAboveMaxRetries_staysFailed", () => {
    const stories = {
      "0042-0001": aStoryEntry({
        status: "FAILED",
        retries: MAX_RETRIES + 1,
      }),
    };
    const result = reclassifyStories(stories, MAX_RETRIES);
    expect(result.stories["0042-0001"].status).toBe("FAILED");
    expect(result.reclassified).toEqual([]);
  });

  it("reclassifyStories_singlePartial_becomesPending", () => {
    const stories = {
      "0042-0001": aStoryEntry({ status: "PARTIAL" }),
    };
    const result = reclassifyStories(stories, MAX_RETRIES);
    expect(result.stories["0042-0001"].status).toBe("PENDING");
    expect(result.reclassified).toEqual([
      {
        storyId: "0042-0001",
        from: "PARTIAL",
        to: "PENDING",
      },
    ]);
  });

  it("reclassifyStories_singleBlocked_staysBlocked", () => {
    const stories = {
      "0042-0001": aStoryEntry({
        status: "BLOCKED",
        blockedBy: ["0042-0002"],
      }),
    };
    const result = reclassifyStories(stories, MAX_RETRIES);
    expect(result.stories["0042-0001"].status).toBe("BLOCKED");
    expect(result.reclassified).toEqual([]);
  });

  it("reclassifyStories_mixedStatuses_correctTransitions", () => {
    const stories = {
      "0042-0001": aStoryEntry({ status: "SUCCESS" }),
      "0042-0002": aStoryEntry({ status: "IN_PROGRESS" }),
      "0042-0003": aStoryEntry({
        status: "FAILED",
        retries: 0,
      }),
      "0042-0004": aStoryEntry({
        status: "FAILED",
        retries: MAX_RETRIES,
      }),
      "0042-0005": aStoryEntry({ status: "PARTIAL" }),
      "0042-0006": aStoryEntry({
        status: "BLOCKED",
        blockedBy: ["0042-0001"],
      }),
      "0042-0007": aStoryEntry(),
    };
    const result = reclassifyStories(stories, MAX_RETRIES);

    expect(result.stories["0042-0001"].status).toBe("SUCCESS");
    expect(result.stories["0042-0002"].status).toBe("PENDING");
    expect(result.stories["0042-0003"].status).toBe("PENDING");
    expect(result.stories["0042-0004"].status).toBe("FAILED");
    expect(result.stories["0042-0005"].status).toBe("PENDING");
    expect(result.stories["0042-0006"].status).toBe("BLOCKED");
    expect(result.stories["0042-0007"].status).toBe("PENDING");

    const reclassifiedIds = result.reclassified.map(
      (r) => r.storyId,
    );
    expect(reclassifiedIds).toContain("0042-0002");
    expect(reclassifiedIds).toContain("0042-0003");
    expect(reclassifiedIds).toContain("0042-0005");
    expect(reclassifiedIds).not.toContain("0042-0001");
    expect(reclassifiedIds).not.toContain("0042-0004");
    expect(reclassifiedIds).not.toContain("0042-0006");
    expect(reclassifiedIds).not.toContain("0042-0007");
  });

  it("reclassifyStories_preservesOtherFields", () => {
    const stories = {
      "0042-0001": aStoryEntry({
        status: "IN_PROGRESS",
        phase: 2,
        retries: 1,
        commitSha: "abc123",
        summary: "some work done",
        duration: "5m",
        findingsCount: 3,
      }),
    };
    const result = reclassifyStories(stories, MAX_RETRIES);
    const entry = result.stories["0042-0001"];
    expect(entry.status).toBe("PENDING");
    expect(entry.phase).toBe(2);
    expect(entry.retries).toBe(1);
    expect(entry.commitSha).toBe("abc123");
    expect(entry.summary).toBe("some work done");
    expect(entry.duration).toBe("5m");
    expect(entry.findingsCount).toBe(3);
  });

  it("reclassifyStories_failedZeroRetries_becomesPending", () => {
    const stories = {
      "0042-0001": aStoryEntry({
        status: "FAILED",
        retries: 0,
      }),
    };
    const result = reclassifyStories(stories, MAX_RETRIES);
    expect(result.stories["0042-0001"].status).toBe("PENDING");
    expect(result.reclassified).toHaveLength(1);
  });

  it("reclassifyStories_failedAtBoundary_staysFailed", () => {
    const stories = {
      "0042-0001": aStoryEntry({
        status: "FAILED",
        retries: 2,
      }),
    };
    const result = reclassifyStories(stories, 2);
    expect(result.stories["0042-0001"].status).toBe("FAILED");
    expect(result.reclassified).toEqual([]);
  });

  it("reclassifyStories_customMaxRetries_respectsLimit", () => {
    const stories = {
      "0042-0001": aStoryEntry({
        status: "FAILED",
        retries: 4,
      }),
    };
    const result = reclassifyStories(stories, 5);
    expect(result.stories["0042-0001"].status).toBe("PENDING");
    expect(result.reclassified).toHaveLength(1);
  });

  it("reclassifyStories_doesNotMutateInput", () => {
    const stories = {
      "0042-0001": aStoryEntry({ status: "IN_PROGRESS" }),
    };
    reclassifyStories(stories, MAX_RETRIES);
    expect(stories["0042-0001"].status).toBe("IN_PROGRESS");
  });
});

// --- reevaluateBlocked ---

describe("reevaluateBlocked", () => {
  it("reevaluateBlocked_emptyMap_returnsEmptyMap", () => {
    const result = reevaluateBlocked({});
    expect(result.stories).toEqual({});
    expect(result.reclassified).toEqual([]);
  });

  it("reevaluateBlocked_noBlockedStories_noChanges", () => {
    const stories = {
      "0042-0001": aStoryEntry({ status: "PENDING" }),
      "0042-0002": aStoryEntry({ status: "SUCCESS" }),
    };
    const result = reevaluateBlocked(stories);
    expect(result.stories["0042-0001"].status).toBe("PENDING");
    expect(result.stories["0042-0002"].status).toBe("SUCCESS");
    expect(result.reclassified).toEqual([]);
  });

  it("reevaluateBlocked_blockedAllDepsSuccess_becomesPending", () => {
    const stories = {
      "0042-0001": aStoryEntry({ status: "SUCCESS" }),
      "0042-0002": aStoryEntry({
        status: "BLOCKED",
        blockedBy: ["0042-0001"],
      }),
    };
    const result = reevaluateBlocked(stories);
    expect(result.stories["0042-0002"].status).toBe("PENDING");
    expect(result.reclassified).toEqual([
      {
        storyId: "0042-0002",
        from: "BLOCKED",
        to: "PENDING",
      },
    ]);
  });

  it("reevaluateBlocked_blockedDepNotSuccess_staysBlocked", () => {
    const stories = {
      "0042-0001": aStoryEntry({ status: "PENDING" }),
      "0042-0002": aStoryEntry({
        status: "BLOCKED",
        blockedBy: ["0042-0001"],
      }),
    };
    const result = reevaluateBlocked(stories);
    expect(result.stories["0042-0002"].status).toBe("BLOCKED");
    expect(result.reclassified).toEqual([]);
  });

  it("reevaluateBlocked_multipleDepsAllSuccess_becomesPending", () => {
    const stories = {
      "0042-0001": aStoryEntry({ status: "SUCCESS" }),
      "0042-0002": aStoryEntry({ status: "SUCCESS" }),
      "0042-0003": aStoryEntry({
        status: "BLOCKED",
        blockedBy: ["0042-0001", "0042-0002"],
      }),
    };
    const result = reevaluateBlocked(stories);
    expect(result.stories["0042-0003"].status).toBe("PENDING");
    expect(result.reclassified).toHaveLength(1);
  });

  it("reevaluateBlocked_multipleDepsOneNotSuccess_staysBlocked", () => {
    const stories = {
      "0042-0001": aStoryEntry({ status: "SUCCESS" }),
      "0042-0002": aStoryEntry({ status: "FAILED" }),
      "0042-0003": aStoryEntry({
        status: "BLOCKED",
        blockedBy: ["0042-0001", "0042-0002"],
      }),
    };
    const result = reevaluateBlocked(stories);
    expect(result.stories["0042-0003"].status).toBe("BLOCKED");
    expect(result.reclassified).toEqual([]);
  });

  it("reevaluateBlocked_blockedByUndefined_staysBlocked", () => {
    const stories = {
      "0042-0001": aStoryEntry({
        status: "BLOCKED",
        blockedBy: undefined,
      }),
    };
    const result = reevaluateBlocked(stories);
    expect(result.stories["0042-0001"].status).toBe("BLOCKED");
    expect(result.reclassified).toEqual([]);
  });

  it("reevaluateBlocked_blockedByEmptyArray_becomesPending", () => {
    const stories = {
      "0042-0001": aStoryEntry({
        status: "BLOCKED",
        blockedBy: [],
      }),
    };
    const result = reevaluateBlocked(stories);
    expect(result.stories["0042-0001"].status).toBe("PENDING");
    expect(result.reclassified).toHaveLength(1);
  });

  it("reevaluateBlocked_missingDepInMap_staysBlocked", () => {
    const stories = {
      "0042-0002": aStoryEntry({
        status: "BLOCKED",
        blockedBy: ["0042-0099"],
      }),
    };
    const result = reevaluateBlocked(stories);
    expect(result.stories["0042-0002"].status).toBe("BLOCKED");
    expect(result.reclassified).toEqual([]);
  });

  it("reevaluateBlocked_mixedBlocked_selectiveReclassification", () => {
    const stories = {
      "0042-0001": aStoryEntry({ status: "SUCCESS" }),
      "0042-0002": aStoryEntry({ status: "PENDING" }),
      "0042-0003": aStoryEntry({
        status: "BLOCKED",
        blockedBy: ["0042-0001"],
      }),
      "0042-0004": aStoryEntry({
        status: "BLOCKED",
        blockedBy: ["0042-0002"],
      }),
    };
    const result = reevaluateBlocked(stories);
    expect(result.stories["0042-0003"].status).toBe("PENDING");
    expect(result.stories["0042-0004"].status).toBe("BLOCKED");
    expect(result.reclassified).toHaveLength(1);
    expect(result.reclassified[0].storyId).toBe("0042-0003");
  });

  it("reevaluateBlocked_doesNotMutateInput", () => {
    const stories = {
      "0042-0001": aStoryEntry({ status: "SUCCESS" }),
      "0042-0002": aStoryEntry({
        status: "BLOCKED",
        blockedBy: ["0042-0001"],
      }),
    };
    reevaluateBlocked(stories);
    expect(stories["0042-0002"].status).toBe("BLOCKED");
  });

  it("reevaluateBlocked_singlePass_noCascade", () => {
    const stories = {
      "0042-0001": aStoryEntry({ status: "SUCCESS" }),
      "0042-0002": aStoryEntry({
        status: "BLOCKED",
        blockedBy: ["0042-0001"],
      }),
      "0042-0003": aStoryEntry({
        status: "BLOCKED",
        blockedBy: ["0042-0002"],
      }),
    };
    const result = reevaluateBlocked(stories);
    expect(result.stories["0042-0002"].status).toBe("PENDING");
    expect(result.stories["0042-0003"].status).toBe("BLOCKED");
    expect(result.reclassified).toHaveLength(1);
  });
});

// --- prepareResume ---

describe("prepareResume", () => {
  it("prepareResume_emptyStories_returnsUnchangedState", () => {
    const state = anExecutionState();
    const result = prepareResume(state);
    expect(result.state.stories).toEqual({});
    expect(result.reclassified).toEqual([]);
  });

  it("prepareResume_composesReclassifyAndReevaluate", () => {
    const state = anExecutionState({
      stories: {
        "0042-0001": aStoryEntry({ status: "IN_PROGRESS" }),
        "0042-0002": aStoryEntry({
          status: "BLOCKED",
          blockedBy: ["0042-0001"],
        }),
      },
    });
    const result = prepareResume(state);
    expect(result.state.stories["0042-0001"].status).toBe(
      "PENDING",
    );
    expect(result.state.stories["0042-0002"].status).toBe(
      "BLOCKED",
    );
    const ids = result.reclassified.map((r) => r.storyId);
    expect(ids).toContain("0042-0001");
  });

  it("prepareResume_blockedResolvesAfterReclassify", () => {
    const state = anExecutionState({
      stories: {
        "0042-0001": aStoryEntry({ status: "SUCCESS" }),
        "0042-0002": aStoryEntry({
          status: "BLOCKED",
          blockedBy: ["0042-0001"],
        }),
      },
    });
    const result = prepareResume(state);
    expect(result.state.stories["0042-0002"].status).toBe(
      "PENDING",
    );
    expect(result.reclassified).toContainEqual({
      storyId: "0042-0002",
      from: "BLOCKED",
      to: "PENDING",
    });
  });

  it("prepareResume_usesDefaultMaxRetries", () => {
    const state = anExecutionState({
      stories: {
        "0042-0001": aStoryEntry({
          status: "FAILED",
          retries: MAX_RETRIES,
        }),
      },
    });
    const result = prepareResume(state);
    expect(result.state.stories["0042-0001"].status).toBe(
      "FAILED",
    );
  });

  it("prepareResume_customMaxRetries_overridesDefault", () => {
    const state = anExecutionState({
      stories: {
        "0042-0001": aStoryEntry({
          status: "FAILED",
          retries: MAX_RETRIES,
        }),
      },
    });
    const result = prepareResume(state, MAX_RETRIES + 1);
    expect(result.state.stories["0042-0001"].status).toBe(
      "PENDING",
    );
  });

  it("prepareResume_preservesNonStoryFields", () => {
    const state = anExecutionState({
      epicId: "0099",
      branch: "feat/epic-0099",
      currentPhase: 2,
      mode: { parallel: true, skipReview: true },
      metrics: {
        storiesCompleted: 5,
        storiesTotal: 10,
      },
    });
    const result = prepareResume(state);
    expect(result.state.epicId).toBe("0099");
    expect(result.state.branch).toBe("feat/epic-0099");
    expect(result.state.currentPhase).toBe(2);
    expect(result.state.mode.parallel).toBe(true);
    expect(result.state.metrics.storiesCompleted).toBe(5);
  });

  it("prepareResume_fullScenario_correctComposition", () => {
    const state = anExecutionState({
      stories: {
        "0042-0001": aStoryEntry({ status: "SUCCESS" }),
        "0042-0002": aStoryEntry({ status: "IN_PROGRESS" }),
        "0042-0003": aStoryEntry({
          status: "FAILED",
          retries: 0,
        }),
        "0042-0004": aStoryEntry({
          status: "BLOCKED",
          blockedBy: ["0042-0001"],
        }),
        "0042-0005": aStoryEntry({
          status: "BLOCKED",
          blockedBy: ["0042-0002"],
        }),
      },
    });
    const result = prepareResume(state);

    expect(result.state.stories["0042-0001"].status).toBe(
      "SUCCESS",
    );
    expect(result.state.stories["0042-0002"].status).toBe(
      "PENDING",
    );
    expect(result.state.stories["0042-0003"].status).toBe(
      "PENDING",
    );
    expect(result.state.stories["0042-0004"].status).toBe(
      "PENDING",
    );
    // 0042-0005 depends on 0042-0002 which was IN_PROGRESS->PENDING
    // but single-pass: it sees the ORIGINAL status which was
    // IN_PROGRESS. After reclassify, 0042-0002 becomes PENDING.
    // reevaluateBlocked checks the reclassified map, where
    // 0042-0002 is PENDING (not SUCCESS), so 0042-0005 stays BLOCKED.
    expect(result.state.stories["0042-0005"].status).toBe(
      "BLOCKED",
    );
  });
});

// --- MAX_RETRIES constant ---

describe("MAX_RETRIES constant", () => {
  it("maxRetries_hasExpectedValue", () => {
    expect(MAX_RETRIES).toBe(2);
  });

  it("maxRetries_isPositiveInteger", () => {
    expect(Number.isInteger(MAX_RETRIES)).toBe(true);
    expect(MAX_RETRIES).toBeGreaterThan(0);
  });
});
