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

  it("claudeSource_phase3_containsGraphqlDispatch", () => {
    expect(claudeContent).toMatch(/graphql.*doc|GraphQL.*generator/i);
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

  it("githubSource_phase3_containsGraphqlDispatch", () => {
    expect(githubContent).toMatch(/graphql.*doc|GraphQL.*generator/i);
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

  // UT-32: Both contain Performance Baseline heading
  it("dualCopy_bothContainPerformanceBaselineHeading", () => {
    expect(claudeContent).toMatch(/Performance Baseline/);
    expect(githubContent).toMatch(/Performance Baseline/);
  });

  // UT-33: Both contain "recommended" language
  it("dualCopy_bothContainPerformanceBaselineRecommended", () => {
    expect(claudeContent).toMatch(/[Rr]ecommended/);
    expect(githubContent).toMatch(/[Rr]ecommended/);
  });

  // UT-34: Both reference template file
  it("dualCopy_bothReferenceTemplateFile", () => {
    expect(claudeContent).toContain(
      "_TEMPLATE-PERFORMANCE-BASELINE.md",
    );
    expect(githubContent).toContain(
      "_TEMPLATE-PERFORMANCE-BASELINE.md",
    );
  });

  // UT-35: Both reference output file
  it("dualCopy_bothReferenceOutputFile", () => {
    expect(claudeContent).toContain("docs/performance/baselines.md");
    expect(githubContent).toContain("docs/performance/baselines.md");
  });

  // UT-36: Both contain 10% warning threshold
  it("dualCopy_bothContainDeltaWarningThreshold", () => {
    expect(claudeContent).toMatch(/10%/);
    expect(githubContent).toMatch(/10%/);
  });

  // UT-37: Both contain 25% investigation threshold
  it("dualCopy_bothContainInvestigationThreshold", () => {
    expect(claudeContent).toMatch(/25%/);
    expect(githubContent).toMatch(/25%/);
  });
});

// ---------------------------------------------------------------------------
// AT-9: Claude source — Performance Baseline prompt in Phase 3
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle Claude source — Performance Baseline", () => {
  // UT-17: Contains Performance Baseline heading
  it("claudeSource_phase3_containsPerformanceBaselineHeading", () => {
    expect(claudeContent).toMatch(/Performance Baseline/);
  });

  // UT-18: Contains "recommended" or "Recommended"
  it("claudeSource_phase3_performanceBaselineRecommended", () => {
    expect(claudeContent).toMatch(/[Rr]ecommended/);
  });

  // UT-19: Skip does not block phase
  it("claudeSource_phase3_performanceBaselineSkipNotBlock", () => {
    expect(claudeContent).toMatch(
      /[Ss]kip does not block the phase/,
    );
  });

  // UT-20: References template file
  it("claudeSource_phase3_referencesTemplateFile", () => {
    expect(claudeContent).toContain(
      "_TEMPLATE-PERFORMANCE-BASELINE.md",
    );
  });

  // UT-21: References output file
  it("claudeSource_phase3_referencesOutputFile", () => {
    expect(claudeContent).toContain("docs/performance/baselines.md");
  });

  // UT-22: Contains 10% warning threshold
  it("claudeSource_phase3_containsDeltaWarningThreshold", () => {
    expect(claudeContent).toMatch(/10%/);
  });

  // UT-23: Contains 25% investigation threshold
  it("claudeSource_phase3_containsInvestigationThreshold", () => {
    expect(claudeContent).toMatch(/25%/);
  });

  // UT-24: Performance Baseline within Phase 3 (after Phase 3, before Phase 4)
  it("claudeSource_performanceBaseline_withinPhase3", () => {
    const phase3Idx = claudeContent.search(/## Phase 3/);
    const phase4Idx = claudeContent.search(/## Phase 4/);
    const perfBaselineIdx = claudeContent.search(/Performance Baseline/);
    expect(phase3Idx).toBeGreaterThan(-1);
    expect(phase4Idx).toBeGreaterThan(-1);
    expect(perfBaselineIdx).toBeGreaterThan(-1);
    expect(perfBaselineIdx).toBeGreaterThan(phase3Idx);
    expect(perfBaselineIdx).toBeLessThan(phase4Idx);
  });
});

// ---------------------------------------------------------------------------
// AT-11: GitHub source — Performance Baseline prompt in Phase 3
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle GitHub source — Performance Baseline", () => {
  // UT-25: Contains Performance Baseline heading
  it("githubSource_phase3_containsPerformanceBaselineHeading", () => {
    expect(githubContent).toMatch(/Performance Baseline/);
  });

  // UT-26: Contains "recommended" or "Recommended"
  it("githubSource_phase3_performanceBaselineRecommended", () => {
    expect(githubContent).toMatch(/[Rr]ecommended/);
  });

  // UT-27: Skip does not block phase
  it("githubSource_phase3_performanceBaselineSkipNotBlock", () => {
    expect(githubContent).toMatch(
      /[Ss]kip does not block the phase/,
    );
  });

  // UT-28: References template file
  it("githubSource_phase3_referencesTemplateFile", () => {
    expect(githubContent).toContain(
      "_TEMPLATE-PERFORMANCE-BASELINE.md",
    );
  });

  // UT-29: References output file
  it("githubSource_phase3_referencesOutputFile", () => {
    expect(githubContent).toContain("docs/performance/baselines.md");
  });

  // UT-30: Contains 10% warning threshold
  it("githubSource_phase3_containsDeltaWarningThreshold", () => {
    expect(githubContent).toMatch(/10%/);
  });

  // UT-31: Contains 25% investigation threshold
  it("githubSource_phase3_containsInvestigationThreshold", () => {
    expect(githubContent).toMatch(/25%/);
  });
});

// ---------------------------------------------------------------------------
// Architecture Document Update in Phase 3
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle Claude source — Architecture Doc Update", () => {
  it("claudeSource_archDocUpdateHeading_existsInContent", () => {
    expect(claudeContent).toMatch(/Architecture Document Update/);
  });

  it("claudeSource_archDocUpdateInPhase3_markedAsRecommended", () => {
    const phase3to4 = claudeContent.slice(
      claudeContent.search(/## Phase 3/),
      claudeContent.search(/## Phase 4/),
    );
    expect(phase3to4).toMatch(/[Rr]ecommended/);
  });

  it("claudeSource_archDocUpdate_referencesArchPlanPath", () => {
    expect(claudeContent).toMatch(/architecture-story-XXXX-YYYY\.md/);
  });

  it("claudeSource_archDocUpdate_referencesServiceArchDoc", () => {
    expect(claudeContent).toContain("docs/architecture/service-architecture.md");
  });

  it("claudeSource_archDocUpdatePosition_betweenPhase3AndPhase4", () => {
    const phase3Idx = claudeContent.search(/## Phase 3/);
    const phase4Idx = claudeContent.search(/## Phase 4/);
    const archDocIdx = claudeContent.search(/Architecture Document Update/);
    expect(archDocIdx).toBeGreaterThan(phase3Idx);
    expect(archDocIdx).toBeLessThan(phase4Idx);
  });

  it("claudeSource_archDocUpdateNoPlan_containsSkipLog", () => {
    expect(claudeContent).toMatch(/[Nn]o architecture plan found/);
  });

  it("claudeSource_archDocUpdate_invokesArchUpdateSkill", () => {
    expect(claudeContent).toContain("x-dev-arch-update");
  });
});

describe("x-dev-lifecycle GitHub source — Architecture Doc Update", () => {
  it("githubSource_archDocUpdateHeading_existsInContent", () => {
    expect(githubContent).toMatch(/Architecture Document Update/);
  });

  it("githubSource_archDocUpdate_referencesServiceArchDoc", () => {
    expect(githubContent).toContain("docs/architecture/service-architecture.md");
  });

  it("githubSource_archDocUpdateNoPlan_containsSkipLog", () => {
    expect(githubContent).toMatch(/[Nn]o architecture plan found/);
  });
});

describe("x-dev-lifecycle dual copy — Architecture Doc Update", () => {
  it("dualCopy_archDocUpdateHeading_existsInBothSources", () => {
    expect(claudeContent).toMatch(/Architecture Document Update/);
    expect(githubContent).toMatch(/Architecture Document Update/);
  });

  it("dualCopy_serviceArchDocRef_existsInBothSources", () => {
    expect(claudeContent).toContain("docs/architecture/service-architecture.md");
    expect(githubContent).toContain("docs/architecture/service-architecture.md");
  });

  it("dualCopy_skipLogMessage_existsInBothSources", () => {
    expect(claudeContent).toMatch(/[Nn]o architecture plan found/);
    expect(githubContent).toMatch(/[Nn]o architecture plan found/);
  });
});
