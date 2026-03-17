import {
  describe,
  it,
  expect,
  beforeEach,
  afterEach,
} from "vitest";
import { mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import { tmpdir } from "node:os";
import { createProgressReporter } from "../../../src/progress/reporter.js";
import { ProgressEventType } from "../../../src/progress/types.js";
import type {
  WriteFn,
  PhaseStartEvent,
  StoryStartEvent,
  StoryCompleteEvent,
  GateResultEvent,
  RetryEvent,
  BlockEvent,
  EpicCompleteEvent,
} from "../../../src/progress/types.js";
import {
  createCheckpoint,
  readCheckpoint,
} from "../../../src/checkpoint/engine.js";

// --- Helpers ---

function createCapturingWriteFn(): {
  readonly writeFn: WriteFn;
  readonly lines: string[];
} {
  const lines: string[] = [];
  const writeFn: WriteFn = (text: string) => {
    lines.push(text);
  };
  return { writeFn, lines };
}

function aPhaseStartEvent(
  overrides?: Partial<PhaseStartEvent>,
): PhaseStartEvent {
  return {
    type: ProgressEventType.PHASE_START,
    phase: 0,
    totalPhases: 4,
    phaseName: "Foundation",
    storiesCount: 3,
    ...overrides,
  };
}

function aStoryStartEvent(
  overrides?: Partial<StoryStartEvent>,
): StoryStartEvent {
  return {
    type: ProgressEventType.STORY_START,
    storyId: "0042-0001",
    phase: 0,
    storyIndex: 1,
    storiesTotal: 14,
    ...overrides,
  };
}

function aStoryCompleteEvent(
  overrides?: Partial<StoryCompleteEvent>,
): StoryCompleteEvent {
  return {
    type: ProgressEventType.STORY_COMPLETE,
    storyId: "0042-0001",
    status: "SUCCESS",
    durationMs: 154000,
    commitSha: "abc123",
    ...overrides,
  };
}

function aGateResultEvent(
  overrides?: Partial<GateResultEvent>,
): GateResultEvent {
  return {
    type: ProgressEventType.GATE_RESULT,
    phase: 0,
    status: "PASS",
    testCount: 42,
    coverage: 96.3,
    ...overrides,
  };
}

function aRetryEvent(
  overrides?: Partial<RetryEvent>,
): RetryEvent {
  return {
    type: ProgressEventType.RETRY,
    storyId: "0042-0003",
    retryNumber: 1,
    maxRetries: 2,
    previousError: "test failure",
    ...overrides,
  };
}

function aBlockEvent(
  overrides?: Partial<BlockEvent>,
): BlockEvent {
  return {
    type: ProgressEventType.BLOCK,
    storyId: "0042-0003",
    blockedStories: ["0042-0004", "0042-0005"],
    ...overrides,
  };
}

function anEpicCompleteEvent(
  overrides?: Partial<EpicCompleteEvent>,
): EpicCompleteEvent {
  return {
    type: ProgressEventType.EPIC_COMPLETE,
    storiesCompleted: 14,
    storiesTotal: 14,
    storiesFailed: 0,
    storiesBlocked: 0,
    elapsedMs: 2700000,
    retryCount: 2,
    ...overrides,
  };
}

function aValidStateJson(
  overrides?: Record<string, unknown>,
): Record<string, unknown> {
  return {
    epicId: "0042",
    branch: "feat/epic-0042-full-implementation",
    startedAt: "2024-01-01T00:00:00.000Z",
    currentPhase: 0,
    mode: { parallel: false, skipReview: false },
    stories: {
      "0042-0001": {
        status: "PENDING",
        phase: 1,
        retries: 0,
      },
      "0042-0002": {
        status: "PENDING",
        phase: 1,
        retries: 0,
      },
      "0042-0003": {
        status: "PENDING",
        phase: 2,
        retries: 0,
      },
    },
    integrityGates: {},
    metrics: { storiesCompleted: 0, storiesTotal: 3 },
    ...overrides,
  };
}

function writeStateFile(
  dir: string,
  state: Record<string, unknown>,
): void {
  writeFileSync(
    join(dir, "execution-state.json"),
    JSON.stringify(state, null, 2),
    "utf-8",
  );
}

// --- Acceptance Tests ---

describe("Acceptance Tests", () => {
  it("AT-01: emit PHASE_START produces correct banner output", async () => {
    const { writeFn, lines } = createCapturingWriteFn();
    const reporter = createProgressReporter({
      epicDir: "/tmp/test-at01",
      writeFn,
      persistMetrics: false,
    });
    await reporter.emit(
      aPhaseStartEvent({
        phase: 0,
        totalPhases: 4,
        phaseName: "Foundation",
        storiesCount: 3,
      }),
    );
    const output = lines.join("\n");
    expect(output).toContain("Phase 0/4");
    expect(output).toContain("Foundation");
    expect(output).toContain("3 stories");
  });

  it("AT-02: emit STORY_START then STORY_COMPLETE(SUCCESS) produces correct output and metrics", async () => {
    const { writeFn, lines } = createCapturingWriteFn();
    const reporter = createProgressReporter({
      epicDir: "/tmp/test-at02",
      writeFn,
      persistMetrics: false,
    });
    await reporter.emit(
      aStoryStartEvent({
        storyId: "0042-0001",
        phase: 0,
        storyIndex: 1,
        storiesTotal: 14,
      }),
    );
    await reporter.emit(
      aStoryCompleteEvent({
        storyId: "0042-0001",
        status: "SUCCESS",
        durationMs: 154000,
        commitSha: "abc123",
      }),
    );
    const output = lines.join("\n");
    expect(output).toContain("[1/14]");
    expect(output).toContain("story-0042-0001");
    expect(output).toContain("SUCCESS");
    expect(output).toContain("2m 34s");
    expect(output).toContain("abc123");
    expect(reporter.getStoryDurations().get("0042-0001")).toBe(154000);
  });

  it("AT-03: emit STORY_COMPLETE(FAILED) then RETRY produces correct output", async () => {
    const { writeFn, lines } = createCapturingWriteFn();
    const reporter = createProgressReporter({
      epicDir: "/tmp/test-at03",
      writeFn,
      persistMetrics: false,
    });
    await reporter.emit(
      aStoryStartEvent({
        storyId: "0042-0003",
        storyIndex: 3,
        storiesTotal: 14,
      }),
    );
    await reporter.emit(
      aStoryCompleteEvent({
        storyId: "0042-0003",
        status: "FAILED",
        durationMs: 192000,
        commitSha: undefined,
      }),
    );
    await reporter.emit(
      aRetryEvent({
        storyId: "0042-0003",
        retryNumber: 1,
        maxRetries: 2,
        previousError: "test failure",
      }),
    );
    const output = lines.join("\n");
    expect(output).toContain("FAILED");
    expect(output).toContain("3m 12s");
    expect(output).toContain("retry 1/2");
  });

  it("AT-04: estimated remaining time after multiple completions", async () => {
    const { writeFn, lines } = createCapturingWriteFn();
    const reporter = createProgressReporter({
      epicDir: "/tmp/test-at04",
      writeFn,
      persistMetrics: false,
    });
    await reporter.emit(aStoryStartEvent({ storiesTotal: 14 }));
    for (let i = 1; i <= 4; i++) {
      await reporter.emit(
        aStoryCompleteEvent({
          storyId: `0042-${String(i).padStart(4, "0")}`,
          status: "SUCCESS",
          durationMs: 150000,
          commitSha: undefined,
        }),
      );
    }
    expect(reporter.getElapsedMs()).toBeGreaterThanOrEqual(0);
    expect(reporter.getStoryDurations().size).toBe(4);
  });

  it("AT-05: metrics persisted in checkpoint", async () => {
    const tmpDir = mkdtempSync(join(tmpdir(), "at05-"));
    try {
      await createCheckpoint(tmpDir, {
        epicId: "0042",
        branch: "feat/epic-0042",
        stories: [
          { id: "0042-0001", phase: 0 },
          { id: "0042-0002", phase: 0 },
          { id: "0042-0003", phase: 0 },
          { id: "0042-0004", phase: 1 },
          { id: "0042-0005", phase: 1 },
          { id: "0042-0006", phase: 1 },
        ],
        mode: { parallel: false, skipReview: false },
      });
      const { writeFn } = createCapturingWriteFn();
      const reporter = createProgressReporter({
        epicDir: tmpDir,
        writeFn,
        persistMetrics: true,
      });
      await reporter.emit(aStoryStartEvent({ storiesTotal: 6 }));
      for (let i = 1; i <= 6; i++) {
        await reporter.emit(
          aStoryCompleteEvent({
            storyId: `0042-${String(i).padStart(4, "0")}`,
            status: "SUCCESS",
            durationMs: 100000 + i * 10000,
            commitSha: undefined,
          }),
        );
      }
      const state = await readCheckpoint(tmpDir);
      expect(state.metrics.storiesCompleted).toBe(6);
      expect(state.metrics.averageStoryDurationMs).toBeDefined();
      expect(state.metrics.storyDurations).toBeDefined();
      expect(
        Object.keys(state.metrics.storyDurations!),
      ).toHaveLength(6);
    } finally {
      rmSync(tmpDir, { recursive: true, force: true });
    }
  });

  it("AT-06: epic complete summary with full sequence", async () => {
    const { writeFn, lines } = createCapturingWriteFn();
    const reporter = createProgressReporter({
      epicDir: "/tmp/test-at06",
      writeFn,
      persistMetrics: false,
    });
    await reporter.emit(aPhaseStartEvent());
    for (let i = 1; i <= 12; i++) {
      await reporter.emit(
        aStoryStartEvent({
          storyId: `0042-${String(i).padStart(4, "0")}`,
          storyIndex: i,
          storiesTotal: 14,
        }),
      );
      await reporter.emit(
        aStoryCompleteEvent({
          storyId: `0042-${String(i).padStart(4, "0")}`,
          status: "SUCCESS",
          durationMs: 150000,
          commitSha: undefined,
        }),
      );
    }
    for (let i = 13; i <= 14; i++) {
      await reporter.emit(
        aStoryStartEvent({
          storyId: `0042-${String(i).padStart(4, "0")}`,
          storyIndex: i,
          storiesTotal: 14,
        }),
      );
      await reporter.emit(
        aStoryCompleteEvent({
          storyId: `0042-${String(i).padStart(4, "0")}`,
          status: "FAILED",
          durationMs: 150000,
          commitSha: undefined,
        }),
      );
    }
    await reporter.emit(
      anEpicCompleteEvent({
        storiesCompleted: 12,
        storiesTotal: 14,
        storiesFailed: 2,
        storiesBlocked: 0,
        elapsedMs: 2700000,
        retryCount: 3,
      }),
    );
    const output = lines.join("\n");
    expect(output).toContain("Epic Complete");
    expect(output).toContain("12/14");
    expect(output).toContain("85.7%");
  });
});

// --- Unit Tests ---

describe("Unit Tests", () => {
  it("createProgressReporter_defaultConfig_returnsObjectWithExpectedMethods", () => {
    const reporter = createProgressReporter({
      epicDir: "/tmp/test",
      persistMetrics: false,
    });
    expect(typeof reporter.emit).toBe("function");
    expect(typeof reporter.getStoryDurations).toBe("function");
    expect(typeof reporter.getPhaseDurations).toBe("function");
    expect(typeof reporter.getElapsedMs).toBe("function");
  });

  it("createProgressReporter_initialState_emptyDurationsAndZeroElapsed", () => {
    const reporter = createProgressReporter({
      epicDir: "/tmp/test",
      persistMetrics: false,
    });
    expect(reporter.getStoryDurations().size).toBe(0);
    expect(reporter.getPhaseDurations().size).toBe(0);
    expect(reporter.getElapsedMs()).toBe(0);
  });

  it("emit_phaseStart_callsWriteFnWithFormattedOutput", async () => {
    const { writeFn, lines } = createCapturingWriteFn();
    const reporter = createProgressReporter({
      epicDir: "/tmp/test",
      writeFn,
      persistMetrics: false,
    });
    await reporter.emit(aPhaseStartEvent());
    expect(lines.length).toBeGreaterThanOrEqual(1);
    expect(lines.some((l) => l.includes("Phase 0/4"))).toBe(true);
  });

  it("emit_storyStart_callsWriteFnWithFormattedOutput", async () => {
    const { writeFn, lines } = createCapturingWriteFn();
    const reporter = createProgressReporter({
      epicDir: "/tmp/test",
      writeFn,
      persistMetrics: false,
    });
    await reporter.emit(aStoryStartEvent());
    expect(lines.length).toBeGreaterThanOrEqual(1);
    expect(lines.some((l) => l.includes("[1/14]"))).toBe(true);
  });

  it("emit_storyCompleteSuccess_updatesStoryDurationsAndCallsWriteFn", async () => {
    const { writeFn, lines } = createCapturingWriteFn();
    const reporter = createProgressReporter({
      epicDir: "/tmp/test",
      writeFn,
      persistMetrics: false,
    });
    await reporter.emit(aStoryStartEvent({ storiesTotal: 14 }));
    await reporter.emit(
      aStoryCompleteEvent({
        storyId: "0042-0001",
        status: "SUCCESS",
        durationMs: 154000,
      }),
    );
    expect(lines.some((l) => l.includes("SUCCESS"))).toBe(true);
    expect(reporter.getStoryDurations().get("0042-0001")).toBe(154000);
  });

  it("emit_storyCompleteFailed_incrementsFailedCounterAndCallsWriteFn", async () => {
    const { writeFn, lines } = createCapturingWriteFn();
    const reporter = createProgressReporter({
      epicDir: "/tmp/test",
      writeFn,
      persistMetrics: false,
    });
    await reporter.emit(aStoryStartEvent({ storiesTotal: 14 }));
    await reporter.emit(
      aStoryCompleteEvent({
        storyId: "0042-0003",
        status: "FAILED",
        durationMs: 192000,
        commitSha: undefined,
      }),
    );
    expect(lines.some((l) => l.includes("FAILED"))).toBe(true);
  });

  it("emit_retry_callsWriteFnWithRetryFormat", async () => {
    const { writeFn, lines } = createCapturingWriteFn();
    const reporter = createProgressReporter({
      epicDir: "/tmp/test",
      writeFn,
      persistMetrics: false,
    });
    await reporter.emit(aRetryEvent());
    expect(lines.some((l) => l.includes("retry 1/2"))).toBe(true);
  });

  it("emit_gateResult_callsWriteFnWithGateFormat", async () => {
    const { writeFn, lines } = createCapturingWriteFn();
    const reporter = createProgressReporter({
      epicDir: "/tmp/test",
      writeFn,
      persistMetrics: false,
    });
    await reporter.emit(aGateResultEvent());
    expect(lines.some((l) => l.includes("Gate Phase 0"))).toBe(true);
  });

  it("emit_block_callsWriteFnWithBlockFormatAndIncrementsBlocked", async () => {
    const { writeFn, lines } = createCapturingWriteFn();
    const reporter = createProgressReporter({
      epicDir: "/tmp/test",
      writeFn,
      persistMetrics: false,
    });
    await reporter.emit(aBlockEvent());
    expect(lines.some((l) => l.includes("BLOCKED"))).toBe(true);
    expect(lines.some((l) => l.includes("0042-0003"))).toBe(true);
  });

  it("emit_epicComplete_callsWriteFnWithSummary", async () => {
    const { writeFn, lines } = createCapturingWriteFn();
    const reporter = createProgressReporter({
      epicDir: "/tmp/test",
      writeFn,
      persistMetrics: false,
    });
    await reporter.emit(anEpicCompleteEvent());
    expect(lines.some((l) => l.includes("Epic Complete"))).toBe(true);
  });

  it("getMetrics_afterMultipleStoryComplete_returnsCorrectRunningAverages", async () => {
    const { writeFn } = createCapturingWriteFn();
    const reporter = createProgressReporter({
      epicDir: "/tmp/test",
      writeFn,
      persistMetrics: false,
    });
    await reporter.emit(aStoryStartEvent({ storiesTotal: 14 }));
    await reporter.emit(
      aStoryCompleteEvent({
        storyId: "0042-0001",
        durationMs: 100000,
        commitSha: undefined,
      }),
    );
    await reporter.emit(
      aStoryCompleteEvent({
        storyId: "0042-0002",
        durationMs: 200000,
        commitSha: undefined,
      }),
    );
    await reporter.emit(
      aStoryCompleteEvent({
        storyId: "0042-0003",
        durationMs: 150000,
        commitSha: undefined,
      }),
    );
    expect(reporter.getStoryDurations().size).toBe(3);
  });

  it("getElapsedMs_afterEmittingEvents_returnsPositiveValue", async () => {
    const { writeFn } = createCapturingWriteFn();
    const reporter = createProgressReporter({
      epicDir: "/tmp/test",
      writeFn,
      persistMetrics: false,
    });
    expect(reporter.getElapsedMs()).toBe(0);
    await reporter.emit(aPhaseStartEvent());
    expect(reporter.getElapsedMs()).toBeGreaterThanOrEqual(0);
    expect(reporter.getElapsedMs()).toBeDefined();
  });

  it("emit_storyCompleteSuccess_persistMetricsFalse_doesNotCallUpdateMetrics", async () => {
    const tmpDir = mkdtempSync(join(tmpdir(), "ut34-"));
    try {
      const { writeFn } = createCapturingWriteFn();
      const reporter = createProgressReporter({
        epicDir: tmpDir,
        writeFn,
        persistMetrics: false,
      });
      await reporter.emit(aStoryStartEvent({ storiesTotal: 3 }));
      await reporter.emit(aStoryCompleteEvent());
      const files = require("node:fs").readdirSync(tmpDir) as string[];
      expect(files).not.toContain("execution-state.json");
    } finally {
      rmSync(tmpDir, { recursive: true, force: true });
    }
  });

  it("emit_concurrentCalls_serializedWithoutRaceCondition", async () => {
    const tmpDir = mkdtempSync(join(tmpdir(), "ut-serial-"));
    try {
      await createCheckpoint(tmpDir, {
        epicId: "0042",
        branch: "feat/epic-0042",
        stories: [
          { id: "0042-0001", phase: 0 },
          { id: "0042-0002", phase: 0 },
          { id: "0042-0003", phase: 0 },
        ],
        mode: { parallel: false, skipReview: false },
      });
      const { writeFn } = createCapturingWriteFn();
      const reporter = createProgressReporter({
        epicDir: tmpDir,
        writeFn,
        persistMetrics: true,
      });
      await reporter.emit(aStoryStartEvent({ storiesTotal: 3 }));
      const p1 = reporter.emit(
        aStoryCompleteEvent({
          storyId: "0042-0001",
          durationMs: 100000,
          commitSha: undefined,
        }),
      );
      const p2 = reporter.emit(
        aStoryCompleteEvent({
          storyId: "0042-0002",
          durationMs: 200000,
          commitSha: undefined,
        }),
      );
      await Promise.all([p1, p2]);
      const state = await readCheckpoint(tmpDir);
      expect(state.metrics.storiesCompleted).toBe(2);
      expect(
        Object.keys(state.metrics.storyDurations!),
      ).toHaveLength(2);
    } finally {
      rmSync(tmpDir, { recursive: true, force: true });
    }
  });

  it("emit_phaseStart_recordsPhaseStartTime", async () => {
    const { writeFn } = createCapturingWriteFn();
    const reporter = createProgressReporter({
      epicDir: "/tmp/test",
      writeFn,
      persistMetrics: false,
    });
    await reporter.emit(aPhaseStartEvent({ phase: 0 }));
    await reporter.emit(
      aPhaseStartEvent({ phase: 1, phaseName: "Phase1" }),
    );
    expect(reporter.getPhaseDurations().has(0)).toBe(true);
  });
});

// --- Integration Tests ---

describe.sequential("Integration Tests", () => {
  let tmpDir: string;

  beforeEach(() => {
    tmpDir = mkdtempSync(join(tmpdir(), "reporter-it-"));
  });

  afterEach(() => {
    rmSync(tmpDir, { recursive: true, force: true });
  });

  it("reporter_withRealCheckpointEngine_persistsMetricsOnStoryComplete", async () => {
    await createCheckpoint(tmpDir, {
      epicId: "0042",
      branch: "feat/epic-0042",
      stories: [
        { id: "0042-0001", phase: 0 },
        { id: "0042-0002", phase: 0 },
        { id: "0042-0003", phase: 1 },
      ],
      mode: { parallel: false, skipReview: false },
    });
    const { writeFn } = createCapturingWriteFn();
    const reporter = createProgressReporter({
      epicDir: tmpDir,
      writeFn,
      persistMetrics: true,
    });
    await reporter.emit(aStoryStartEvent({ storiesTotal: 3 }));
    await reporter.emit(
      aStoryCompleteEvent({
        storyId: "0042-0001",
        status: "SUCCESS",
        durationMs: 120000,
        commitSha: "def456",
      }),
    );
    const state = await readCheckpoint(tmpDir);
    expect(state.metrics.storiesCompleted).toBeGreaterThanOrEqual(1);
    expect(state.metrics.averageStoryDurationMs).toBeDefined();
    expect(state.metrics.storyDurations).toBeDefined();
    expect(state.metrics.storyDurations!["0042-0001"]).toBe(120000);
    expect(state.metrics.elapsedMs).toBeGreaterThanOrEqual(0);
    expect(state.metrics.elapsedMs).toBeDefined();
  });

  it("reporter_backwardCompatibility_existingCheckpointWithoutNewFields_canBeReadAndUpdated", async () => {
    writeStateFile(tmpDir, aValidStateJson());
    const { writeFn } = createCapturingWriteFn();
    const reporter = createProgressReporter({
      epicDir: tmpDir,
      writeFn,
      persistMetrics: true,
    });
    await reporter.emit(aStoryStartEvent({ storiesTotal: 3 }));
    await reporter.emit(
      aStoryCompleteEvent({
        storyId: "0042-0001",
        status: "SUCCESS",
        durationMs: 90000,
        commitSha: undefined,
      }),
    );
    const state = await readCheckpoint(tmpDir);
    expect(state.metrics.storiesCompleted).toBeGreaterThanOrEqual(1);
    expect(state.metrics.storiesTotal).toBe(3);
    expect(state.metrics.storyDurations).toBeDefined();
  });
});
