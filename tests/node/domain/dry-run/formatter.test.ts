import { describe, it, expect } from "vitest";
import type {
  DryRunPlan,
  DryRunPhaseInfo,
  DryRunStoryDetail,
} from "../../../../src/domain/dry-run/types.js";
import {
  formatPlan,
  formatStoryDetail,
} from "../../../../src/domain/dry-run/formatter.js";

const EPIC_ID = "EPIC-0042";

function emptyPlan(): DryRunPlan {
  return {
    epicId: EPIC_ID,
    mode: "full",
    totalStories: 0,
    totalPhases: 0,
    criticalPath: [],
    phases: [],
  };
}

function fullPlan(): DryRunPlan {
  const phase0: DryRunPhaseInfo = {
    phase: 0,
    stories: [
      {
        id: "0042-0001",
        title: "Setup project",
        status: "PENDING",
        isCriticalPath: true,
        dependenciesSatisfied: true,
        blockedBy: [],
      },
      {
        id: "0042-0002",
        title: "Side task",
        status: "PENDING",
        isCriticalPath: false,
        dependenciesSatisfied: true,
        blockedBy: [],
      },
    ],
    parallelCount: 2,
  };
  const phase1: DryRunPhaseInfo = {
    phase: 1,
    stories: [
      {
        id: "0042-0003",
        title: "Integration",
        status: "PENDING",
        isCriticalPath: true,
        dependenciesSatisfied: false,
        blockedBy: ["0042-0001"],
      },
    ],
    parallelCount: 1,
  };
  return {
    epicId: EPIC_ID,
    mode: "full",
    totalStories: 3,
    totalPhases: 2,
    criticalPath: ["0042-0001", "0042-0003"],
    phases: [phase0, phase1],
  };
}

function sampleDetail(): DryRunStoryDetail {
  return {
    id: "0042-0005",
    title: "Auth module",
    phase: 1,
    status: "PENDING",
    isCriticalPath: true,
    dependencies: ["0042-0002"],
    dependents: ["0042-0008"],
  };
}

describe("formatPlan", () => {
  it("emptyPlan_returnsHeaderWithZeros", () => {
    const output = formatPlan(emptyPlan());

    expect(output).toContain("EPIC-0042");
    expect(output).toContain("0 stories");
    expect(output).toContain("0 phases");
  });

  it("fullPlan_returnsFormattedText", () => {
    const output = formatPlan(fullPlan());

    expect(output).toContain("3 stories");
    expect(output).toContain("2 phases");
    expect(output).toContain("Phase 0");
    expect(output).toContain("Phase 1");
    expect(output).toContain("0042-0001");
    expect(output).toContain("Setup project");
    expect(output).toContain("[CRITICAL]");
    expect(output).toContain("0042-0002");
    expect(output).not.toContain(
      "0042-0002" + ".*CRITICAL",
    );
  });
});

describe("formatStoryDetail", () => {
  it("storyDetail_returnsFormattedDetail", () => {
    const output = formatStoryDetail(sampleDetail());

    expect(output).toContain("0042-0005");
    expect(output).toContain("Auth module");
    expect(output).toContain("Phase: 1");
    expect(output).toContain("PENDING");
    expect(output).toContain("0042-0002");
    expect(output).toContain("0042-0008");
  });

  it("storyDetail_emptyDeps_showsNone", () => {
    const detail: DryRunStoryDetail = {
      id: "0042-0001",
      title: "Root story",
      phase: 0,
      status: "PENDING",
      isCriticalPath: false,
      dependencies: [],
      dependents: [],
    };

    const output = formatStoryDetail(detail);

    expect(output).toContain("Dependencies: none");
    expect(output).toContain("Dependents: none");
    expect(output).toContain("Critical Path: No");
  });
});
