import {
  describe,
  it,
  expect,
  beforeEach,
  afterEach,
} from "vitest";
import { mkdtempSync, rmSync, readFileSync, existsSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import { tmpdir } from "node:os";
import {
  createCheckpoint,
  readCheckpoint,
  updateIntegrityGate,
  updateMetrics,
  updateStoryStatus,
} from "../../../src/checkpoint/engine.js";
import {
  CheckpointIOError,
  CheckpointValidationError,
} from "../../../src/exceptions.js";
import type {
  CreateCheckpointInput,
  ExecutionMode,
  ExecutionState,
} from "../../../src/checkpoint/types.js";

// --- Helpers ---

const DEFAULT_MODE: ExecutionMode = {
  parallel: false,
  skipReview: false,
};

function anInput(
  overrides?: Partial<CreateCheckpointInput>,
): CreateCheckpointInput {
  return {
    epicId: "0042",
    branch: "feat/epic-0042",
    stories: [],
    mode: DEFAULT_MODE,
    ...overrides,
  };
}

function threeStories(): ReadonlyArray<{
  readonly id: string;
  readonly phase: number;
}> {
  return [
    { id: "0042-0001", phase: 1 },
    { id: "0042-0002", phase: 1 },
    { id: "0042-0003", phase: 2 },
  ];
}

function fiveStories(): ReadonlyArray<{
  readonly id: string;
  readonly phase: number;
}> {
  return [
    { id: "0042-0001", phase: 1 },
    { id: "0042-0002", phase: 1 },
    { id: "0042-0003", phase: 2 },
    { id: "0042-0004", phase: 2 },
    { id: "0042-0005", phase: 3 },
  ];
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
        status: "IN_PROGRESS",
        phase: 1,
        retries: 0,
      },
    },
    integrityGates: {},
    metrics: { storiesCompleted: 0, storiesTotal: 2 },
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

// --- 4.2 createCheckpoint ---

describe("createCheckpoint", () => {
  let tmpDir: string;

  beforeEach(() => {
    tmpDir = mkdtempSync(
      join(tmpdir(), "checkpoint-test-"),
    );
  });

  afterEach(() => {
    rmSync(tmpDir, { recursive: true, force: true });
  });

  it("createCheckpoint_directoryDoesNotExist_throwsCheckpointIOError", async () => {
    await expect(
      createCheckpoint(
        "/tmp/nonexistent-dir-999",
        anInput(),
      ),
    ).rejects.toThrow(CheckpointIOError);
  });

  it("createCheckpoint_pathIsFile_throwsCheckpointIOError", async () => {
    const filePath = join(tmpDir, "not-a-dir.txt");
    writeFileSync(filePath, "hello", "utf-8");
    await expect(
      createCheckpoint(filePath, anInput()),
    ).rejects.toThrow(CheckpointIOError);
  });

  it("createCheckpoint_emptyStoriesList_createsFileWithEmptyStoriesMap", async () => {
    const state = await createCheckpoint(
      tmpDir,
      anInput(),
    );
    expect(Object.keys(state.stories)).toHaveLength(0);
  });

  it("createCheckpoint_singleStory_createsFileWithOnePendingEntry", async () => {
    const state = await createCheckpoint(
      tmpDir,
      anInput({
        stories: [{ id: "0042-0001", phase: 1 }],
      }),
    );
    expect(Object.keys(state.stories)).toHaveLength(1);
    expect(state.stories["0042-0001"]?.status).toBe(
      "PENDING",
    );
  });

  it("createCheckpoint_fiveStories_allStatusPending", async () => {
    const state = await createCheckpoint(
      tmpDir,
      anInput({ stories: fiveStories() }),
    );
    for (const entry of Object.values(state.stories)) {
      expect(entry.status).toBe("PENDING");
    }
  });

  it("createCheckpoint_fiveStories_allRetriesZero", async () => {
    const state = await createCheckpoint(
      tmpDir,
      anInput({ stories: fiveStories() }),
    );
    for (const entry of Object.values(state.stories)) {
      expect(entry.retries).toBe(0);
    }
  });

  it("createCheckpoint_fiveStories_metricsStoriesCompletedIsZero", async () => {
    const state = await createCheckpoint(
      tmpDir,
      anInput({ stories: fiveStories() }),
    );
    expect(state.metrics.storiesCompleted).toBe(0);
  });

  it("createCheckpoint_fiveStories_metricsStoriesTotalIsFive", async () => {
    const state = await createCheckpoint(
      tmpDir,
      anInput({ stories: fiveStories() }),
    );
    expect(state.metrics.storiesTotal).toBe(5);
  });

  it("createCheckpoint_setsEpicIdCorrectly", async () => {
    const state = await createCheckpoint(
      tmpDir,
      anInput(),
    );
    expect(state.epicId).toBe("0042");
  });

  it("createCheckpoint_setBranchCorrectly", async () => {
    const state = await createCheckpoint(
      tmpDir,
      anInput({ branch: "feat/epic-0042-full" }),
    );
    expect(state.branch).toBe("feat/epic-0042-full");
  });

  it("createCheckpoint_setsStartedAtToIso8601", async () => {
    const before = new Date().toISOString();
    const state = await createCheckpoint(
      tmpDir,
      anInput(),
    );
    const after = new Date().toISOString();
    expect(state.startedAt >= before).toBe(true);
    expect(state.startedAt <= after).toBe(true);
  });

  it("createCheckpoint_setsCurrentPhaseToZero", async () => {
    const state = await createCheckpoint(
      tmpDir,
      anInput(),
    );
    expect(state.currentPhase).toBe(0);
  });

  it("createCheckpoint_setsModeFromInput", async () => {
    const mode: ExecutionMode = {
      parallel: true,
      skipReview: true,
    };
    const state = await createCheckpoint(
      tmpDir,
      anInput({ mode }),
    );
    expect(state.mode.parallel).toBe(true);
    expect(state.mode.skipReview).toBe(true);
  });

  it("createCheckpoint_writesValidJsonToFile", async () => {
    await createCheckpoint(
      tmpDir,
      anInput({
        stories: [{ id: "0042-0001", phase: 1 }],
      }),
    );
    const raw = readFileSync(
      join(tmpDir, "execution-state.json"),
      "utf-8",
    );
    const parsed = JSON.parse(raw) as Record<
      string,
      unknown
    >;
    expect(parsed["epicId"]).toBe("0042");
  });

  it("createCheckpoint_noTmpFileRemainsAfterWrite", async () => {
    await createCheckpoint(tmpDir, anInput());
    expect(
      existsSync(
        join(tmpDir, ".execution-state.json.tmp"),
      ),
    ).toBe(false);
  });

  it("createCheckpoint_returnsExecutionStateObject", async () => {
    const state = await createCheckpoint(
      tmpDir,
      anInput({ stories: threeStories() }),
    );
    expect(state.epicId).toBeDefined();
    expect(state.branch).toBeDefined();
    expect(state.startedAt).toBeDefined();
    expect(state.stories).toBeDefined();
    expect(state.metrics).toBeDefined();
  });

  it("createCheckpoint_integrityGatesInitializedEmpty", async () => {
    const state = await createCheckpoint(
      tmpDir,
      anInput(),
    );
    expect(
      Object.keys(state.integrityGates),
    ).toHaveLength(0);
  });
});

// --- 4.3 readCheckpoint ---

describe("readCheckpoint", () => {
  let tmpDir: string;

  beforeEach(() => {
    tmpDir = mkdtempSync(
      join(tmpdir(), "checkpoint-test-"),
    );
  });

  afterEach(() => {
    rmSync(tmpDir, { recursive: true, force: true });
  });

  it("readCheckpoint_fileDoesNotExist_throwsCheckpointIOError", async () => {
    await expect(readCheckpoint(tmpDir)).rejects.toThrow(
      CheckpointIOError,
    );
  });

  it("readCheckpoint_invalidJson_throwsCheckpointIOError", async () => {
    writeFileSync(
      join(tmpDir, "execution-state.json"),
      "not valid json{{{",
      "utf-8",
    );
    await expect(readCheckpoint(tmpDir)).rejects.toThrow(
      CheckpointIOError,
    );
  });

  it("readCheckpoint_invalidSchema_throwsCheckpointValidationError", async () => {
    writeStateFile(tmpDir, { foo: "bar" });
    await expect(readCheckpoint(tmpDir)).rejects.toThrow(
      CheckpointValidationError,
    );
  });

  it("readCheckpoint_validFile_returnsExecutionState", async () => {
    writeStateFile(tmpDir, aValidStateJson());
    const state = await readCheckpoint(tmpDir);
    expect(state.epicId).toBe("0042");
  });

  it("readCheckpoint_validFile_allFieldsPreserved", async () => {
    const input = aValidStateJson();
    writeStateFile(tmpDir, input);
    const state = await readCheckpoint(tmpDir);
    expect(state.branch).toBe(
      "feat/epic-0042-full-implementation",
    );
    expect(state.currentPhase).toBe(0);
    expect(state.mode.parallel).toBe(false);
    expect(state.metrics.storiesTotal).toBe(2);
  });

  it("readCheckpoint_fileWithOptionalFields_preservesOptionals", async () => {
    const input = aValidStateJson({
      stories: {
        "0042-0001": {
          status: "SUCCESS",
          phase: 1,
          retries: 0,
          commitSha: "abc123",
          duration: "5m",
          summary: "done",
        },
      },
      metrics: {
        storiesCompleted: 1,
        storiesTotal: 1,
      },
    });
    writeStateFile(tmpDir, input);
    const state = await readCheckpoint(tmpDir);
    expect(state.stories["0042-0001"]?.commitSha).toBe(
      "abc123",
    );
    expect(state.stories["0042-0001"]?.summary).toBe(
      "done",
    );
  });

  it("readCheckpoint_validFile_storiesMapHasCorrectKeys", async () => {
    writeStateFile(tmpDir, aValidStateJson());
    const state = await readCheckpoint(tmpDir);
    expect(
      Object.keys(state.stories).sort(),
    ).toEqual(["0042-0001", "0042-0002"]);
  });

  it("readCheckpoint_invalidStoryStatus_throwsValidationError", async () => {
    writeStateFile(
      tmpDir,
      aValidStateJson({
        stories: {
          "0042-0001": {
            status: "UNKNOWN",
            phase: 1,
            retries: 0,
          },
        },
      }),
    );
    await expect(readCheckpoint(tmpDir)).rejects.toThrow(
      CheckpointValidationError,
    );
  });
});

// --- 4.4 updateStoryStatus ---

describe("updateStoryStatus", () => {
  let tmpDir: string;

  beforeEach(() => {
    tmpDir = mkdtempSync(
      join(tmpdir(), "checkpoint-test-"),
    );
  });

  afterEach(() => {
    rmSync(tmpDir, { recursive: true, force: true });
  });

  it("updateStoryStatus_nonexistentStoryId_throwsCheckpointValidationError", async () => {
    writeStateFile(tmpDir, aValidStateJson());
    await expect(
      updateStoryStatus(tmpDir, "9999-9999", {
        status: "SUCCESS",
      }),
    ).rejects.toThrow(CheckpointValidationError);
  });

  it("updateStoryStatus_nonexistentFile_throwsCheckpointIOError", async () => {
    await expect(
      updateStoryStatus(tmpDir, "0042-0001", {
        status: "SUCCESS",
      }),
    ).rejects.toThrow(CheckpointIOError);
  });

  it("updateStoryStatus_setStatusToSuccess_updatesStatus", async () => {
    writeStateFile(tmpDir, aValidStateJson());
    const state = await updateStoryStatus(
      tmpDir,
      "0042-0001",
      { status: "SUCCESS" },
    );
    expect(state.stories["0042-0001"]?.status).toBe(
      "SUCCESS",
    );
  });

  it("updateStoryStatus_setCommitSha_updatesCommitSha", async () => {
    writeStateFile(tmpDir, aValidStateJson());
    const state = await updateStoryStatus(
      tmpDir,
      "0042-0001",
      { commitSha: "abc123" },
    );
    expect(state.stories["0042-0001"]?.commitSha).toBe(
      "abc123",
    );
  });

  it("updateStoryStatus_setStatusToFailed_updatesStatus", async () => {
    writeStateFile(tmpDir, aValidStateJson());
    const state = await updateStoryStatus(
      tmpDir,
      "0042-0002",
      { status: "FAILED" },
    );
    expect(state.stories["0042-0002"]?.status).toBe(
      "FAILED",
    );
  });

  it("updateStoryStatus_incrementRetries_updatesRetries", async () => {
    writeStateFile(tmpDir, aValidStateJson());
    const state = await updateStoryStatus(
      tmpDir,
      "0042-0001",
      { retries: 2 },
    );
    expect(state.stories["0042-0001"]?.retries).toBe(2);
  });

  it("updateStoryStatus_otherStoriesUnchanged_preservesOtherEntries", async () => {
    writeStateFile(tmpDir, aValidStateJson());
    const state = await updateStoryStatus(
      tmpDir,
      "0042-0001",
      { status: "SUCCESS" },
    );
    expect(state.stories["0042-0002"]?.status).toBe(
      "IN_PROGRESS",
    );
    expect(state.stories["0042-0002"]?.retries).toBe(0);
  });

  it("updateStoryStatus_partialUpdate_mergesWithExistingFields", async () => {
    writeStateFile(tmpDir, aValidStateJson());
    const state = await updateStoryStatus(
      tmpDir,
      "0042-0001",
      { status: "SUCCESS", commitSha: "def456" },
    );
    expect(state.stories["0042-0001"]?.status).toBe(
      "SUCCESS",
    );
    expect(state.stories["0042-0001"]?.commitSha).toBe(
      "def456",
    );
    expect(state.stories["0042-0001"]?.phase).toBe(1);
    expect(state.stories["0042-0001"]?.retries).toBe(0);
  });

  it("updateStoryStatus_noTmpFileRemainsAfterWrite_atomicCleanup", async () => {
    writeStateFile(tmpDir, aValidStateJson());
    await updateStoryStatus(tmpDir, "0042-0001", {
      status: "SUCCESS",
    });
    expect(
      existsSync(
        join(tmpDir, ".execution-state.json.tmp"),
      ),
    ).toBe(false);
  });

  it("updateStoryStatus_returnsUpdatedExecutionState", async () => {
    writeStateFile(tmpDir, aValidStateJson());
    const state = await updateStoryStatus(
      tmpDir,
      "0042-0001",
      { status: "SUCCESS" },
    );
    expect(state.epicId).toBe("0042");
    expect(state.stories["0042-0001"]?.status).toBe(
      "SUCCESS",
    );
  });
});

// --- 4.5 updateIntegrityGate ---

describe("updateIntegrityGate", () => {
  let tmpDir: string;

  beforeEach(() => {
    tmpDir = mkdtempSync(
      join(tmpdir(), "checkpoint-test-"),
    );
  });

  afterEach(() => {
    rmSync(tmpDir, { recursive: true, force: true });
  });

  it("updateIntegrityGate_nonexistentFile_throwsCheckpointIOError", async () => {
    await expect(
      updateIntegrityGate(tmpDir, 0, {
        status: "PASS",
        testCount: 10,
        coverage: 95,
      }),
    ).rejects.toThrow(CheckpointIOError);
  });

  it("updateIntegrityGate_phaseZeroPass_createsGateEntry", async () => {
    writeStateFile(tmpDir, aValidStateJson());
    const state = await updateIntegrityGate(tmpDir, 0, {
      status: "PASS",
      testCount: 42,
      coverage: 96.3,
    });
    expect(state.integrityGates["phase-0"]).toBeDefined();
    expect(state.integrityGates["phase-0"]?.status).toBe(
      "PASS",
    );
  });

  it("updateIntegrityGate_phaseZeroPass_setsTimestampAutomatically", async () => {
    writeStateFile(tmpDir, aValidStateJson());
    const before = new Date().toISOString();
    const state = await updateIntegrityGate(tmpDir, 0, {
      status: "PASS",
      testCount: 42,
      coverage: 96.3,
    });
    const after = new Date().toISOString();
    const ts = state.integrityGates["phase-0"]?.timestamp;
    expect(ts).toBeDefined();
    expect(ts! >= before).toBe(true);
    expect(ts! <= after).toBe(true);
  });

  it("updateIntegrityGate_phaseZeroPass_preservesTestCount", async () => {
    writeStateFile(tmpDir, aValidStateJson());
    const state = await updateIntegrityGate(tmpDir, 0, {
      status: "PASS",
      testCount: 42,
      coverage: 96.3,
    });
    expect(
      state.integrityGates["phase-0"]?.testCount,
    ).toBe(42);
  });

  it("updateIntegrityGate_phaseZeroPass_preservesCoverage", async () => {
    writeStateFile(tmpDir, aValidStateJson());
    const state = await updateIntegrityGate(tmpDir, 0, {
      status: "PASS",
      testCount: 42,
      coverage: 96.3,
    });
    expect(
      state.integrityGates["phase-0"]?.coverage,
    ).toBe(96.3);
  });

  it("updateIntegrityGate_failWithFailedTests_storesFailedTestArray", async () => {
    writeStateFile(tmpDir, aValidStateJson());
    const state = await updateIntegrityGate(tmpDir, 1, {
      status: "FAIL",
      testCount: 100,
      coverage: 88,
      failedTests: ["test-a", "test-b"],
    });
    expect(
      state.integrityGates["phase-1"]?.failedTests,
    ).toEqual(["test-a", "test-b"]);
  });

  it("updateIntegrityGate_multiplePhases_storesAllGates", async () => {
    writeStateFile(tmpDir, aValidStateJson());
    await updateIntegrityGate(tmpDir, 0, {
      status: "PASS",
      testCount: 10,
      coverage: 95,
    });
    const state = await updateIntegrityGate(tmpDir, 1, {
      status: "PASS",
      testCount: 50,
      coverage: 97,
    });
    expect(state.integrityGates["phase-0"]).toBeDefined();
    expect(state.integrityGates["phase-1"]).toBeDefined();
  });

  it("updateIntegrityGate_returnsUpdatedExecutionState", async () => {
    writeStateFile(tmpDir, aValidStateJson());
    const state = await updateIntegrityGate(tmpDir, 0, {
      status: "PASS",
      testCount: 42,
      coverage: 96.3,
    });
    expect(state.epicId).toBe("0042");
    expect(state.integrityGates["phase-0"]).toBeDefined();
  });
});

// --- 4.6 updateMetrics ---

describe("updateMetrics", () => {
  let tmpDir: string;

  beforeEach(() => {
    tmpDir = mkdtempSync(
      join(tmpdir(), "checkpoint-test-"),
    );
  });

  afterEach(() => {
    rmSync(tmpDir, { recursive: true, force: true });
  });

  it("updateMetrics_nonexistentFile_throwsCheckpointIOError", async () => {
    await expect(
      updateMetrics(tmpDir, { storiesCompleted: 1 }),
    ).rejects.toThrow(CheckpointIOError);
  });

  it("updateMetrics_updateStoriesCompleted_updatesField", async () => {
    writeStateFile(tmpDir, aValidStateJson());
    const state = await updateMetrics(tmpDir, {
      storiesCompleted: 1,
    });
    expect(state.metrics.storiesCompleted).toBe(1);
  });

  it("updateMetrics_updateEstimatedRemaining_updatesField", async () => {
    writeStateFile(tmpDir, aValidStateJson());
    const state = await updateMetrics(tmpDir, {
      estimatedRemainingMinutes: 30,
    });
    expect(
      state.metrics.estimatedRemainingMinutes,
    ).toBe(30);
  });

  it("updateMetrics_partialUpdate_mergesWithExisting", async () => {
    writeStateFile(tmpDir, aValidStateJson());
    const state = await updateMetrics(tmpDir, {
      storiesCompleted: 1,
    });
    expect(state.metrics.storiesCompleted).toBe(1);
    expect(state.metrics.storiesTotal).toBe(2);
  });

  it("updateMetrics_returnsUpdatedExecutionState", async () => {
    writeStateFile(tmpDir, aValidStateJson());
    const state = await updateMetrics(tmpDir, {
      storiesCompleted: 2,
    });
    expect(state.epicId).toBe("0042");
    expect(state.metrics.storiesCompleted).toBe(2);
  });
});
