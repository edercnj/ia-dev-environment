import { describe, it, expect } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";

const CLAUDE_SOURCE = path.resolve(
  __dirname,
  "../../..",
  "resources/skills-templates/core/x-dev-lifecycle/SKILL.md",
);
const GITHUB_SOURCE = path.resolve(
  __dirname,
  "../../..",
  "resources/github-skills-templates/dev/x-dev-lifecycle.md",
);

const claudeContent = fs.readFileSync(CLAUDE_SOURCE, "utf-8");
const githubContent = fs.readFileSync(GITHUB_SOURCE, "utf-8");

// ---------------------------------------------------------------------------
// UT-2: Phase count header — "9 phases (0-8)"
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle Claude source — Phase count and stop rule", () => {
  it("claudeSource_criticalRule_contains9Phases0to8", () => {
    expect(claudeContent).toMatch(/9 phases \(0-8\)/);
  });

  it("claudeSource_criticalRule_doesNotContain8Phases0to7", () => {
    expect(claudeContent).not.toMatch(/8 phases \(0-7\)/);
  });

  // UT-3: "NEVER stop before Phase 8"
  it("claudeSource_criticalRule_neverStopBeforePhase8", () => {
    expect(claudeContent).toContain("NEVER stop before Phase 8");
  });

  it("claudeSource_criticalRule_doesNotReferenceOldPhase7Stop", () => {
    expect(claudeContent).not.toContain("NEVER stop before Phase 7");
  });

  // UT-12: Progress messages Phase N/8
  it("claudeSource_progressMessage_phaseNof8", () => {
    expect(claudeContent).toMatch(/Phase N\/8/);
  });

  it("claudeSource_progressMessage_noOldPhaseNof7", () => {
    expect(claudeContent).not.toMatch(/Phase N\/7/);
  });
});

// ---------------------------------------------------------------------------
// UT-1: Phase 3 Documentation heading
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle Claude source — Phase 3 Documentation", () => {
  it("claudeSource_phase3_containsDocumentationHeading", () => {
    expect(claudeContent).toMatch(/## Phase 3 .*Documentation/);
  });

  // UT-9: Interface dispatch mechanism
  it("claudeSource_phase3_containsInterfacesFieldRead", () => {
    expect(claudeContent).toMatch(/interfaces.*field|Read.*interfaces/i);
  });

  it("claudeSource_phase3_containsRestDispatch", () => {
    expect(claudeContent).toContain("rest");
    expect(claudeContent).toMatch(/OpenAPI|Swagger/);
  });

  it("claudeSource_phase3_containsGrpcDispatch", () => {
    expect(claudeContent).toMatch(/grpc.*doc|gRPC.*generator/i);
  });

  it("claudeSource_phase3_containsCliDispatch", () => {
    expect(claudeContent).toMatch(/cli.*doc|CLI.*generator/i);
  });

  it("claudeSource_phase3_containsEventDispatch", () => {
    expect(claudeContent).toMatch(
      /websocket|event.*driven|event-consumer|event-producer/i,
    );
  });

  it("claudeSource_phase3_containsNoInterfaceSkipLog", () => {
    expect(claudeContent).toMatch(/[Nn]o documentable interfaces/);
  });

  // UT-10: Changelog generation
  it("claudeSource_phase3_containsChangelogGeneration", () => {
    expect(claudeContent).toMatch(/changelog.*entry|CHANGELOG\.md/i);
  });

  it("claudeSource_phase3_changelogAlwaysGenerated", () => {
    expect(claudeContent).toMatch(/ALWAYS.*regardless|always.*regardless/i);
  });

  it("claudeSource_phase3_changelogUsesConventionalCommits", () => {
    expect(claudeContent).toMatch(/[Cc]onventional [Cc]ommits/);
  });

  it("claudeSource_phase3_changelogReadsGitLog", () => {
    expect(claudeContent).toMatch(/git log|commits since/);
  });
});

// ---------------------------------------------------------------------------
// UT-4 through UT-8: Phase renumbering
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle Claude source — Phase renumbering", () => {
  // UT-4: Review is now Phase 4
  it("claudeSource_phase4_containsReviewHeading", () => {
    expect(claudeContent).toMatch(/## Phase 4 .*Review/);
  });

  it("claudeSource_noOldPhase3Review", () => {
    expect(claudeContent).not.toMatch(/## Phase 3 .*Review/);
  });

  // UT-5: Fixes is now Phase 5
  it("claudeSource_phase5_containsFixesHeading", () => {
    expect(claudeContent).toMatch(/## Phase 5 .*Fixes/);
  });

  it("claudeSource_noOldPhase4Fixes", () => {
    expect(claudeContent).not.toMatch(/## Phase 4 .*Fixes/);
  });

  // UT-6: Commit is now Phase 6
  it("claudeSource_phase6_containsCommitHeading", () => {
    expect(claudeContent).toMatch(/## Phase 6 .*Commit/);
  });

  it("claudeSource_noOldPhase5Commit", () => {
    expect(claudeContent).not.toMatch(/## Phase 5 .*Commit/);
  });

  // UT-7: Tech Lead is now Phase 7
  it("claudeSource_phase7_containsTechLeadHeading", () => {
    expect(claudeContent).toMatch(/## Phase 7 .*Tech Lead/);
  });

  it("claudeSource_noOldPhase6TechLead", () => {
    expect(claudeContent).not.toMatch(/## Phase 6 .*Tech Lead/);
  });

  // UT-8: Verification is now Phase 8
  it("claudeSource_phase8_containsVerificationHeading", () => {
    expect(claudeContent).toMatch(/## Phase 8 .*Verification/);
  });

  it("claudeSource_noOldPhase7Verification", () => {
    expect(claudeContent).not.toMatch(/## Phase 7 .*Verification/);
  });

  it("claudeSource_phase8IsOnlyStoppingPoint", () => {
    expect(claudeContent).toMatch(
      /Phase 8 is the ONLY legitimate stopping point/,
    );
  });

  it("claudeSource_noOldPhase7StoppingPoint", () => {
    expect(claudeContent).not.toContain(
      "Phase 7 is the ONLY legitimate stopping point",
    );
  });
});

// ---------------------------------------------------------------------------
// UT-11: Complete Flow block lists all 9 phases
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle Claude source — Complete Flow block", () => {
  it("claudeSource_completeFlow_containsPhase0Preparation", () => {
    expect(claudeContent).toMatch(/Phase 0:.*Preparation/);
  });

  it("claudeSource_completeFlow_containsPhase1Planning", () => {
    expect(claudeContent).toMatch(/Phase 1:.*Planning/);
  });

  it("claudeSource_completeFlow_containsPhase1BParallel", () => {
    expect(claudeContent).toMatch(/Phase 1B-1E:.*Parallel/);
  });

  it("claudeSource_completeFlow_containsPhase2Implementation", () => {
    expect(claudeContent).toMatch(/Phase 2:.*Implementation/);
  });

  it("claudeSource_completeFlow_containsPhase3Documentation", () => {
    expect(claudeContent).toMatch(/Phase 3:.*Documentation/);
  });

  it("claudeSource_completeFlow_containsPhase4Review", () => {
    expect(claudeContent).toMatch(/Phase 4:.*Review/);
  });

  it("claudeSource_completeFlow_containsPhase56FixesPR", () => {
    expect(claudeContent).toMatch(
      /Phase 5-6:.*Fixes.*PR|Phase 5.*Fixes|Phase 6.*Commit/,
    );
  });

  it("claudeSource_completeFlow_containsPhase7TechLead", () => {
    expect(claudeContent).toMatch(/Phase 7:.*Tech Lead/);
  });

  it("claudeSource_completeFlow_containsPhase8Verification", () => {
    expect(claudeContent).toMatch(/Phase 8:.*Verification/);
  });

  it("claudeSource_completeFlow_noOldPhase3Review", () => {
    expect(claudeContent).not.toMatch(/Phase 3:.*Review/);
  });
});

// ---------------------------------------------------------------------------
// UT-13: Roles table updated
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle Claude source — Roles table renumbering", () => {
  it("claudeSource_rolesTable_reviewPhase4", () => {
    expect(claudeContent).toMatch(/Specialist Reviews.*Phase 4/);
  });

  it("claudeSource_rolesTable_techLeadPhase7", () => {
    expect(claudeContent).toMatch(/Tech Lead.*Phase 7/);
  });

  it("claudeSource_rolesTable_noOldPhase3Review", () => {
    expect(claudeContent).not.toMatch(/Specialist Reviews.*Phase 3/);
  });

  it("claudeSource_rolesTable_noOldPhase6TechLead", () => {
    expect(claudeContent).not.toMatch(/Tech Lead.*Phase 6\b/);
  });
});

// ---------------------------------------------------------------------------
// UT-14: Phase 3 positioned between Phase 2 and Phase 4
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle Claude source — Phase ordering", () => {
  it("claudeSource_phase3_afterPhase2", () => {
    const phase2Idx = claudeContent.search(/## Phase 2/);
    const phase3Idx = claudeContent.search(/## Phase 3/);
    expect(phase2Idx).toBeGreaterThan(-1);
    expect(phase3Idx).toBeGreaterThan(-1);
    expect(phase3Idx).toBeGreaterThan(phase2Idx);
  });

  it("claudeSource_phase3_beforePhase4", () => {
    const phase3Idx = claudeContent.search(/## Phase 3/);
    const phase4Idx = claudeContent.search(/## Phase 4/);
    expect(phase3Idx).toBeGreaterThan(-1);
    expect(phase4Idx).toBeGreaterThan(-1);
    expect(phase3Idx).toBeLessThan(phase4Idx);
  });
});

// ---------------------------------------------------------------------------
// UT-15: Structural preservation — unchanged elements intact
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle Claude source — Structural preservation", () => {
  it("claudeSource_preservesPhase0Preparation", () => {
    expect(claudeContent).toMatch(/## Phase 0 .*Preparation/);
  });

  it("claudeSource_preservesPhase1Architecture", () => {
    expect(claudeContent).toMatch(/## Phase 1 .*Architecture/);
  });

  it("claudeSource_preservesPhase2TDDImplementation", () => {
    expect(claudeContent).toMatch(/## Phase 2 .*TDD Implementation/);
  });

  it("claudeSource_preservesPhase1BTestPlanning", () => {
    expect(claudeContent).toContain("1B: Test Planning");
  });

  it("claudeSource_preservesPhase1CTaskDecomposition", () => {
    expect(claudeContent).toContain("1C: Task Decomposition");
  });

  it("claudeSource_preservesPhase1DEventSchema", () => {
    expect(claudeContent).toContain("1D: Event Schema Design");
  });

  it("claudeSource_preservesPhase1ECompliance", () => {
    expect(claudeContent).toContain("1E: Compliance Assessment");
  });

  it("claudeSource_preservesG1G7Fallback", () => {
    expect(claudeContent).toContain("G1-G7 Fallback");
  });

  it("claudeSource_preservesAllPlaceholderTokens", () => {
    const tokens = [
      "{{PROJECT_NAME}}",
      "{{LANGUAGE}}",
      "{{LANGUAGE_VERSION}}",
      "{{COMPILE_COMMAND}}",
      "{{TEST_COMMAND}}",
      "{{COVERAGE_COMMAND}}",
    ];
    for (const token of tokens) {
      expect(claudeContent).toContain(token);
    }
  });

  it("claudeSource_preservesIntegrationNotes", () => {
    expect(claudeContent).toContain("## Integration Notes");
  });

  it("claudeSource_preservesFrontmatter", () => {
    expect(claudeContent).toContain("name: x-dev-lifecycle");
  });
});

// ---------------------------------------------------------------------------
// GitHub source — Phase 3 Documentation and renumbering
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle GitHub source — Documentation phase", () => {
  it("githubSource_criticalRule_contains9Phases0to8", () => {
    expect(githubContent).toMatch(/9 phases \(0-8\)/);
  });

  it("githubSource_criticalRule_doesNotContain8Phases0to7", () => {
    expect(githubContent).not.toMatch(/8 phases \(0-7\)/);
  });

  it("githubSource_criticalRule_neverStopBeforePhase8", () => {
    expect(githubContent).toContain("NEVER stop before Phase 8");
  });

  it("githubSource_criticalRule_doesNotReferenceOldPhase7Stop", () => {
    expect(githubContent).not.toContain("NEVER stop before Phase 7");
  });

  it("githubSource_phase3_containsDocumentationHeading", () => {
    expect(githubContent).toMatch(/## Phase 3 .*Documentation/);
  });

  it("githubSource_phase4_containsReviewHeading", () => {
    expect(githubContent).toMatch(/## Phase 4 .*Review/);
  });

  it("githubSource_phase5_containsFixesHeading", () => {
    expect(githubContent).toMatch(/## Phase 5 .*Fixes/);
  });

  it("githubSource_phase6_containsCommitHeading", () => {
    expect(githubContent).toMatch(/## Phase 6 .*Commit/);
  });

  it("githubSource_phase7_containsTechLeadHeading", () => {
    expect(githubContent).toMatch(/## Phase 7 .*Tech Lead/);
  });

  it("githubSource_phase8_containsVerificationHeading", () => {
    expect(githubContent).toMatch(/## Phase 8 .*Verification/);
  });

  it("githubSource_phase3_containsInterfaceDispatchMechanism", () => {
    expect(githubContent).toMatch(/interfaces.*field|Read.*interfaces/i);
  });

  it("githubSource_phase3_containsNoInterfaceSkipLog", () => {
    expect(githubContent).toMatch(/[Nn]o documentable interfaces/);
  });

  it("githubSource_phase3_containsChangelogGeneration", () => {
    expect(githubContent).toMatch(/changelog.*entry|CHANGELOG\.md/i);
  });

  it("githubSource_phase3_changelogAlwaysGenerated", () => {
    expect(githubContent).toMatch(/ALWAYS.*regardless|always.*regardless/i);
  });

  // IT-2: GitHub-specific progress format
  it("githubSource_progressMessage_phase8of8Completed", () => {
    expect(githubContent).toMatch(/Phase 8\/8 completed/);
  });

  it("githubSource_progressMessage_noOldPhase7of7", () => {
    expect(githubContent).not.toMatch(/Phase 7\/7 completed/);
  });

  it("githubSource_afterEachPhases0to7", () => {
    expect(githubContent).toMatch(/Phases 0.7:/);
  });

  it("githubSource_afterPhase8_lifecycleComplete", () => {
    expect(githubContent).toMatch(/After Phase 8:/);
  });

  it("githubSource_completeFlow_containsPhase3Documentation", () => {
    expect(githubContent).toMatch(/Phase 3:.*Documentation/);
  });

  it("githubSource_completeFlow_containsPhase8Verification", () => {
    expect(githubContent).toMatch(/Phase 8:.*Verification/);
  });

  it("githubSource_detailedReferencesPreserved", () => {
    expect(githubContent).toContain("## Detailed References");
  });

  // Phase ordering in GitHub source
  it("githubSource_phase3_afterPhase2", () => {
    const phase2Idx = githubContent.search(/## Phase 2/);
    const phase3Idx = githubContent.search(/## Phase 3/);
    expect(phase2Idx).toBeGreaterThan(-1);
    expect(phase3Idx).toBeGreaterThan(-1);
    expect(phase3Idx).toBeGreaterThan(phase2Idx);
  });

  it("githubSource_phase3_beforePhase4", () => {
    const phase3Idx = githubContent.search(/## Phase 3/);
    const phase4Idx = githubContent.search(/## Phase 4/);
    expect(phase3Idx).toBeGreaterThan(-1);
    expect(phase4Idx).toBeGreaterThan(-1);
    expect(phase3Idx).toBeLessThan(phase4Idx);
  });

  it("githubSource_phase8IsOnlyStoppingPoint", () => {
    expect(githubContent).toMatch(
      /Phase 8 is the ONLY legitimate stopping point/,
    );
  });
});

// ---------------------------------------------------------------------------
// UT-16: Dual copy consistency (RULE-001)
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle dual copy consistency (RULE-001)", () => {
  it("dualCopy_bothContainPhase3DocumentationHeading", () => {
    expect(claudeContent).toMatch(/## Phase 3 .*Documentation/);
    expect(githubContent).toMatch(/## Phase 3 .*Documentation/);
  });

  it("dualCopy_bothContain9Phases0to8", () => {
    expect(claudeContent).toMatch(/9 phases \(0-8\)/);
    expect(githubContent).toMatch(/9 phases \(0-8\)/);
  });

  it("dualCopy_bothContainNeverStopBeforePhase8", () => {
    expect(claudeContent).toContain("NEVER stop before Phase 8");
    expect(githubContent).toContain("NEVER stop before Phase 8");
  });

  it("dualCopy_bothContainPhase8VerificationHeading", () => {
    expect(claudeContent).toMatch(/## Phase 8 .*Verification/);
    expect(githubContent).toMatch(/## Phase 8 .*Verification/);
  });

  it("dualCopy_bothContainPhase8OnlyStoppingPoint", () => {
    expect(claudeContent).toMatch(
      /Phase 8 is the ONLY legitimate stopping point/,
    );
    expect(githubContent).toMatch(
      /Phase 8 is the ONLY legitimate stopping point/,
    );
  });

  it("dualCopy_bothContainInterfaceDispatch", () => {
    expect(claudeContent).toMatch(/interfaces.*field|Read.*interfaces/i);
    expect(githubContent).toMatch(/interfaces.*field|Read.*interfaces/i);
  });

  it("dualCopy_bothContainChangelogGeneration", () => {
    expect(claudeContent).toMatch(/changelog.*entry|CHANGELOG\.md/i);
    expect(githubContent).toMatch(/changelog.*entry|CHANGELOG\.md/i);
  });

  it("dualCopy_bothContainNoInterfaceSkipLog", () => {
    expect(claudeContent).toMatch(/[Nn]o documentable interfaces/);
    expect(githubContent).toMatch(/[Nn]o documentable interfaces/);
  });

  it("dualCopy_bothContainPhase3InCompleteFlow", () => {
    expect(claudeContent).toMatch(/Phase 3:.*Documentation/);
    expect(githubContent).toMatch(/Phase 3:.*Documentation/);
  });

  it("dualCopy_phaseCountIdentical", () => {
    const claudeMatch = claudeContent.match(/(\d+) phases \(0-(\d+)\)/);
    const githubMatch = githubContent.match(/(\d+) phases \(0-(\d+)\)/);
    expect(claudeMatch).not.toBeNull();
    expect(githubMatch).not.toBeNull();
    expect(claudeMatch![1]).toBe(githubMatch![1]);
    expect(claudeMatch![2]).toBe(githubMatch![2]);
  });
});
