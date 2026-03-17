import {
  describe,
  it,
  expect,
  beforeEach,
  afterEach,
} from "vitest";
import {
  mkdtempSync,
  rmSync,
  readFileSync,
  writeFileSync,
  existsSync,
} from "node:fs";
import { join, resolve } from "node:path";
import { tmpdir } from "node:os";
import {
  createCheckpoint,
  readCheckpoint,
  updateIntegrityGate,
  updateStoryStatus,
} from "../../../src/checkpoint/engine.js";
import { validateExecutionState } from "../../../src/checkpoint/validation.js";
import {
  CheckpointIOError,
  CheckpointValidationError,
} from "../../../src/exceptions.js";
import type {
  CreateCheckpointInput,
  ExecutionMode,
} from "../../../src/checkpoint/types.js";

// --- Template Tests (IT-01 through IT-04) ---

const TEMPLATE_PATH = resolve(
  import.meta.dirname,
  "../../../resources/templates/_TEMPLATE-EXECUTION-STATE.json",
);

describe("template validation", () => {
  it("template_fileExists_atExpectedPath", () => {
    expect(existsSync(TEMPLATE_PATH)).toBe(true);
  });

  it("template_validJson_parsesWithoutError", () => {
    const raw = readFileSync(TEMPLATE_PATH, "utf-8");
    expect(() => JSON.parse(raw)).not.toThrow();
  });

  it("template_hasAllRequiredFields_matchesExecutionStateInterface", () => {
    const raw = readFileSync(TEMPLATE_PATH, "utf-8");
    const data = JSON.parse(raw) as Record<
      string,
      unknown
    >;
    expect(data["epicId"]).toBeDefined();
    expect(data["branch"]).toBeDefined();
    expect(data["startedAt"]).toBeDefined();
    expect(data["currentPhase"]).toBeDefined();
    expect(data["mode"]).toBeDefined();
    expect(data["stories"]).toBeDefined();
    expect(data["integrityGates"]).toBeDefined();
    expect(data["metrics"]).toBeDefined();
  });

  it("template_storyEntry_hasAllRequiredFields", () => {
    const raw = readFileSync(TEMPLATE_PATH, "utf-8");
    const data = JSON.parse(raw) as Record<
      string,
      unknown
    >;
    const stories = data["stories"] as Record<
      string,
      Record<string, unknown>
    >;
    const first = Object.values(stories)[0];
    expect(first).toBeDefined();
    expect(first!["status"]).toBeDefined();
    expect(first!["phase"]).toBeDefined();
    expect(first!["retries"]).toBeDefined();
  });

  it("template_integrityGateEntry_hasAllRequiredFields", () => {
    const raw = readFileSync(TEMPLATE_PATH, "utf-8");
    const data = JSON.parse(raw) as Record<
      string,
      unknown
    >;
    const gates = data["integrityGates"] as Record<
      string,
      Record<string, unknown>
    >;
    const first = Object.values(gates)[0];
    expect(first).toBeDefined();
    expect(first!["status"]).toBeDefined();
    expect(first!["timestamp"]).toBeDefined();
    expect(first!["testCount"]).toBeDefined();
    expect(first!["coverage"]).toBeDefined();
  });

  it("template_passesValidation_noValidationErrors", () => {
    const raw = readFileSync(TEMPLATE_PATH, "utf-8");
    const data = JSON.parse(raw) as unknown;
    expect(() => validateExecutionState(data)).not.toThrow();
  });
});

// --- Acceptance Tests (AT-1 through AT-8) ---

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

describe("acceptance tests", () => {
  let tmpDir: string;

  beforeEach(() => {
    tmpDir = mkdtempSync(
      join(tmpdir(), "checkpoint-accept-"),
    );
  });

  afterEach(() => {
    rmSync(tmpDir, { recursive: true, force: true });
  });

  it("createCheckpoint_directoryDoesNotExist_throwsCheckpointIOError", async () => {
    await expect(
      createCheckpoint(
        "/tmp/nonexistent-epic-9999",
        anInput({ epicId: "9999", branch: "feat/epic-9999" }),
      ),
    ).rejects.toThrow(CheckpointIOError);
  });

  it("readCheckpoint_missingEpicIdField_throwsValidationErrorWithMessage", async () => {
    writeFileSync(
      join(tmpDir, "execution-state.json"),
      JSON.stringify({
        branch: "feat/epic-0042",
        startedAt: "2024-01-01T00:00:00.000Z",
        currentPhase: 0,
        mode: { parallel: false, skipReview: false },
        stories: {},
        integrityGates: {},
        metrics: {
          storiesCompleted: 0,
          storiesTotal: 0,
        },
      }),
      "utf-8",
    );
    await expect(
      readCheckpoint(tmpDir),
    ).rejects.toThrow(/epicId/);
  });

  it("readCheckpoint_invalidStatusEnum_throwsValidationErrorWithMessage", async () => {
    writeFileSync(
      join(tmpDir, "execution-state.json"),
      JSON.stringify({
        epicId: "0042",
        branch: "feat/epic-0042",
        startedAt: "2024-01-01T00:00:00.000Z",
        currentPhase: 0,
        mode: { parallel: false, skipReview: false },
        stories: {
          "0042-0001": {
            status: "UNKNOWN",
            phase: 1,
            retries: 0,
          },
        },
        integrityGates: {},
        metrics: {
          storiesCompleted: 0,
          storiesTotal: 1,
        },
      }),
      "utf-8",
    );
    await expect(
      readCheckpoint(tmpDir),
    ).rejects.toThrow(/status/);
  });

  it("createCheckpoint_fiveStoriesWithDefaults_createsFileWithAllPending", async () => {
    const stories = [
      { id: "0042-0001", phase: 1 },
      { id: "0042-0002", phase: 1 },
      { id: "0042-0003", phase: 2 },
      { id: "0042-0004", phase: 2 },
      { id: "0042-0005", phase: 3 },
    ];
    const state = await createCheckpoint(
      tmpDir,
      anInput({
        branch: "feat/epic-0042-full-implementation",
        stories,
      }),
    );
    expect(existsSync(
      join(tmpDir, "execution-state.json"),
    )).toBe(true);
    for (const entry of Object.values(state.stories)) {
      expect(entry.status).toBe("PENDING");
      expect(entry.retries).toBe(0);
    }
    expect(Object.keys(state.stories)).toHaveLength(5);
    expect(state.metrics.storiesCompleted).toBe(0);
    expect(state.metrics.storiesTotal).toBe(5);
  });

  it("readCheckpoint_validFileWithThreeStories_returnsTypedExecutionState", async () => {
    const stories = [
      { id: "0042-0001", phase: 1 },
      { id: "0042-0002", phase: 1 },
      { id: "0042-0003", phase: 2 },
    ];
    await createCheckpoint(
      tmpDir,
      anInput({ stories }),
    );
    const state = await readCheckpoint(tmpDir);
    expect(state.epicId).toBe("0042");
    expect(Object.keys(state.stories)).toHaveLength(3);
    for (const entry of Object.values(state.stories)) {
      expect(
        ["PENDING", "IN_PROGRESS", "SUCCESS", "FAILED", "BLOCKED", "PARTIAL"].includes(
          entry.status,
        ),
      ).toBe(true);
    }
  });

  it("updateStoryStatus_setSuccessWithCommitSha_atomicallyUpdatesOnlyTargetStory", async () => {
    const stories = [
      { id: "0042-0001", phase: 1 },
      { id: "0042-0002", phase: 1 },
    ];
    await createCheckpoint(
      tmpDir,
      anInput({ stories }),
    );
    const state = await updateStoryStatus(
      tmpDir,
      "0042-0001",
      { status: "SUCCESS", commitSha: "abc123" },
    );
    expect(state.stories["0042-0001"]?.status).toBe(
      "SUCCESS",
    );
    expect(state.stories["0042-0001"]?.commitSha).toBe(
      "abc123",
    );
    expect(state.stories["0042-0002"]?.status).toBe(
      "PENDING",
    );
    expect(
      existsSync(
        join(tmpDir, ".execution-state.json.tmp"),
      ),
    ).toBe(false);
  });

  it("updateStoryStatus_setFailedWithRetryIncrement_updatesStatusAndRetries", async () => {
    await createCheckpoint(
      tmpDir,
      anInput({
        stories: [{ id: "0042-0003", phase: 2 }],
      }),
    );
    await updateStoryStatus(tmpDir, "0042-0003", {
      status: "IN_PROGRESS",
    });
    const state = await updateStoryStatus(
      tmpDir,
      "0042-0003",
      { status: "FAILED", retries: 2 },
    );
    expect(state.stories["0042-0003"]?.status).toBe(
      "FAILED",
    );
    expect(state.stories["0042-0003"]?.retries).toBe(2);
  });

  it("updateIntegrityGate_phaseZeroPass_recordsResultWithAutoTimestamp", async () => {
    await createCheckpoint(
      tmpDir,
      anInput({
        stories: [{ id: "0042-0001", phase: 1 }],
      }),
    );
    const before = new Date().toISOString();
    const state = await updateIntegrityGate(tmpDir, 0, {
      status: "PASS",
      testCount: 42,
      coverage: 96.3,
    });
    const after = new Date().toISOString();
    const gate = state.integrityGates["phase-0"];
    expect(gate).toBeDefined();
    expect(gate?.status).toBe("PASS");
    expect(gate?.testCount).toBe(42);
    expect(gate?.coverage).toBe(96.3);
    expect(gate!.timestamp >= before).toBe(true);
    expect(gate!.timestamp <= after).toBe(true);
  });

  it("updateIntegrityGate_failWithRegressionSourceAndBranchCoverage_roundTripsCorrectly", async () => {
    await createCheckpoint(
      tmpDir,
      anInput({
        stories: [{ id: "0042-0001", phase: 1 }],
      }),
    );
    const state = await updateIntegrityGate(tmpDir, 0, {
      status: "FAIL",
      testCount: 100,
      coverage: 93.2,
      branchCoverage: 85.5,
      failedTests: ["test-a"],
      regressionSource: "0042-0003",
    });
    const gate = state.integrityGates["phase-0"];
    expect(gate).toBeDefined();
    expect(gate?.status).toBe("FAIL");
    expect(gate?.testCount).toBe(100);
    expect(gate?.coverage).toBe(93.2);
    expect(gate?.branchCoverage).toBe(85.5);
    expect(gate?.failedTests).toEqual(["test-a"]);
    expect(gate?.regressionSource).toBe("0042-0003");
  });
});

// --- Integration Round-Trip Tests (IT-05 through IT-09) ---

describe("integration round-trip", () => {
  let tmpDir: string;

  beforeEach(() => {
    tmpDir = mkdtempSync(
      join(tmpdir(), "checkpoint-roundtrip-"),
    );
  });

  afterEach(() => {
    rmSync(tmpDir, { recursive: true, force: true });
  });

  it("checkpointRoundTrip_createThenRead_returnsEquivalentState", async () => {
    const created = await createCheckpoint(
      tmpDir,
      anInput({
        stories: [{ id: "0042-0001", phase: 1 }],
      }),
    );
    const read = await readCheckpoint(tmpDir);
    expect(read.epicId).toBe(created.epicId);
    expect(read.branch).toBe(created.branch);
    expect(read.currentPhase).toBe(created.currentPhase);
    expect(read.metrics.storiesTotal).toBe(
      created.metrics.storiesTotal,
    );
  });

  it("checkpointRoundTrip_createUpdateRead_reflectsUpdate", async () => {
    await createCheckpoint(
      tmpDir,
      anInput({
        stories: [{ id: "0042-0001", phase: 1 }],
      }),
    );
    await updateStoryStatus(tmpDir, "0042-0001", {
      status: "SUCCESS",
      commitSha: "sha123",
    });
    const read = await readCheckpoint(tmpDir);
    expect(read.stories["0042-0001"]?.status).toBe(
      "SUCCESS",
    );
    expect(read.stories["0042-0001"]?.commitSha).toBe(
      "sha123",
    );
  });

  it("checkpointRoundTrip_multipleUpdates_allChangesPersistedCorrectly", async () => {
    await createCheckpoint(
      tmpDir,
      anInput({
        stories: [
          { id: "0042-0001", phase: 1 },
          { id: "0042-0002", phase: 2 },
        ],
      }),
    );
    await updateStoryStatus(tmpDir, "0042-0001", {
      status: "SUCCESS",
    });
    await updateStoryStatus(tmpDir, "0042-0002", {
      status: "FAILED",
      retries: 1,
    });
    await updateIntegrityGate(tmpDir, 0, {
      status: "PASS",
      testCount: 50,
      coverage: 97,
    });
    const read = await readCheckpoint(tmpDir);
    expect(read.stories["0042-0001"]?.status).toBe(
      "SUCCESS",
    );
    expect(read.stories["0042-0002"]?.status).toBe(
      "FAILED",
    );
    expect(read.integrityGates["phase-0"]?.status).toBe(
      "PASS",
    );
  });

  it("atomicWrite_afterSuccessfulWrite_tmpFileDoesNotExist", async () => {
    await createCheckpoint(
      tmpDir,
      anInput({
        stories: [{ id: "0042-0001", phase: 1 }],
      }),
    );
    await updateStoryStatus(tmpDir, "0042-0001", {
      status: "SUCCESS",
    });
    expect(
      existsSync(
        join(tmpDir, ".execution-state.json.tmp"),
      ),
    ).toBe(false);
  });

  it("atomicWrite_duringWrite_noCorruptedFileOnDisk", async () => {
    await createCheckpoint(
      tmpDir,
      anInput({
        stories: [{ id: "0042-0001", phase: 1 }],
      }),
    );
    const state = await readCheckpoint(tmpDir);
    expect(state.epicId).toBe("0042");
    expect(state.stories["0042-0001"]?.status).toBe(
      "PENDING",
    );
  });

  it("checkpointRoundTrip_integrityGateWithAllFields_preservesAllFields", async () => {
    await createCheckpoint(
      tmpDir,
      anInput({
        stories: [{ id: "0042-0001", phase: 1 }],
      }),
    );
    await updateIntegrityGate(tmpDir, 0, {
      status: "FAIL",
      testCount: 100,
      coverage: 93.2,
      branchCoverage: 85.5,
      failedTests: ["test-a"],
      regressionSource: "0042-0003",
    });
    const read = await readCheckpoint(tmpDir);
    const gate = read.integrityGates["phase-0"];
    expect(gate).toBeDefined();
    expect(gate?.status).toBe("FAIL");
    expect(gate?.testCount).toBe(100);
    expect(gate?.coverage).toBe(93.2);
    expect(gate?.branchCoverage).toBe(85.5);
    expect(gate?.failedTests).toEqual(["test-a"]);
    expect(gate?.regressionSource).toBe("0042-0003");
    expect(gate?.timestamp).toMatch(
      /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/,
    );
  });
});
