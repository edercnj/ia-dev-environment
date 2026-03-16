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
// Claude Source — Post-Deploy Verification Section Existence (UT-1 to UT-6)
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle Claude source — post-deploy verification existence", () => {
  it("claudeSource_phase7_containsPostDeployVerificationSubsection", () => {
    expect(claudeContent).toMatch(/Post-Deploy Verification/);
  });

  it("claudeSource_phase7_postDeployIsConditionalOnSmokeTests", () => {
    expect(claudeContent).toMatch(/smoke_tests/);
  });

  it("claudeSource_phase7_postDeployContainsHealthCheck", () => {
    expect(claudeContent).toMatch(/Health Check/);
  });

  it("claudeSource_phase7_postDeployContainsCriticalPath", () => {
    expect(claudeContent).toMatch(/Critical Path/);
  });

  it("claudeSource_phase7_postDeployContainsResponseTimeSLO", () => {
    expect(claudeContent).toMatch(/Response Time|p95|SLO/);
  });

  it("claudeSource_phase7_postDeployContainsErrorRate", () => {
    expect(claudeContent).toMatch(/Error Rate|error rate|1%/);
  });
});

// ---------------------------------------------------------------------------
// Claude Source — Post-Deploy Result Semantics (UT-7 to UT-10)
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle Claude source — post-deploy result semantics", () => {
  it("claudeSource_phase7_postDeployResultPASS", () => {
    expect(claudeContent).toMatch(/PASS.*Deploy confirmed|Deploy confirmed.*PASS/is);
  });

  it("claudeSource_phase7_postDeployResultFAIL", () => {
    expect(claudeContent).toMatch(/FAIL.*rollback|rollback.*FAIL/is);
  });

  it("claudeSource_phase7_postDeployResultSKIP", () => {
    expect(claudeContent).toMatch(/SKIP.*smoke_tests|smoke_tests.*SKIP/is);
  });

  it("claudeSource_phase7_postDeployIsNonBlocking", () => {
    expect(claudeContent).toMatch(
      /non-blocking|Non-blocking|NOT auto-rollback|human decision|not.*auto.*rollback/i,
    );
  });
});

// ---------------------------------------------------------------------------
// Claude Source — Skill Invocation References (UT-11)
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle Claude source — skill invocation references", () => {
  it("claudeSource_phase7_postDeployReferencesRunE2EOrSmokeSkill", () => {
    expect(claudeContent).toMatch(/\/run-e2e|\/run-smoke-api/);
  });
});

// ---------------------------------------------------------------------------
// Claude Source — Conditional DoD Item Addition (UT-12 to UT-13)
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle Claude source — conditional DoD item", () => {
  it("claudeSource_phase7_conditionalDoDContainsPostDeployItem", () => {
    expect(claudeContent).toMatch(/Post-deploy verification/i);
  });

  it("claudeSource_phase7_conditionalDoDPostDeployLinkedToSmokeTests", () => {
    const conditionalSection = claudeContent.slice(
      claudeContent.indexOf("Conditional DoD items"),
    );
    expect(conditionalSection).toMatch(/smoke_tests/);
  });
});

// ---------------------------------------------------------------------------
// Claude Source — Item Renumbering (UT-14 to UT-15)
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle Claude source — item renumbering", () => {
  it("claudeSource_phase7_reportPassFailIsRenumbered", () => {
    expect(claudeContent).toMatch(/^7\.\s+Report PASS\/FAIL result/m);
  });

  it("claudeSource_phase7_gitCheckoutMainIsRenumbered", () => {
    expect(claudeContent).toMatch(/^8\.\s+`git checkout main/m);
  });
});

// ---------------------------------------------------------------------------
// Claude Source — Backward Compatibility (UT-16 to UT-27)
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle Claude source — backward compatibility", () => {
  it("claudeSource_preserves9PhaseCount", () => {
    expect(claudeContent).toMatch(/9 phases.*0-8/);
  });

  it("claudeSource_preservesCriticalExecutionRule", () => {
    expect(claudeContent).toContain("NEVER stop before Phase 8");
  });

  it("claudeSource_preservesPhase7OnlyLegitimateStoppingPoint", () => {
    expect(claudeContent).toContain(
      "Phase 8 is the ONLY legitimate stopping point",
    );
  });

  it("claudeSource_preservesExistingConditionalDoDItems", () => {
    expect(claudeContent).toContain("Contract tests pass");
    expect(claudeContent).toContain("Event schemas registered");
    expect(claudeContent).toContain("Compliance requirements met");
    expect(claudeContent).toContain("Gateway configuration updated");
    expect(claudeContent).toMatch(/gRPC proto backward compatible/);
    expect(claudeContent).toMatch(/GraphQL schema backward compatible/);
  });

  it("claudeSource_preservesTDDDoDItems", () => {
    expect(claudeContent).toMatch(/test-first pattern/);
    expect(claudeContent).toMatch(/AT-N GREEN/);
    expect(claudeContent).toMatch(/TPP ordering|simple to complex/);
  });

  it("claudeSource_preservesUpdateREADMEStep", () => {
    expect(claudeContent).toContain("Update README if needed");
  });

  it("claudeSource_preservesUpdateImplementationMapStep", () => {
    expect(claudeContent).toContain("Update IMPLEMENTATION-MAP");
  });

  it("claudeSource_preservesDoDChecklistStep", () => {
    expect(claudeContent).toContain("Run DoD checklist");
  });

  it("claudeSource_preservesAllPlaceholderTokens", () => {
    expect(claudeContent).toContain("{{PROJECT_NAME}}");
    expect(claudeContent).toContain("{{LANGUAGE}}");
    expect(claudeContent).toContain("{{LANGUAGE_VERSION}}");
    expect(claudeContent).toContain("{{COMPILE_COMMAND}}");
    expect(claudeContent).toContain("{{TEST_COMMAND}}");
    expect(claudeContent).toContain("{{COVERAGE_COMMAND}}");
  });

  it("claudeSource_preservesCompleteFlowDiagram", () => {
    expect(claudeContent).toContain("Phase 0: Preparation");
    expect(claudeContent).toContain("Phase 8: Verification");
  });

  it("claudeSource_preservesRolesAndModelsTable", () => {
    expect(claudeContent).toContain("Roles and Models");
    expect(claudeContent).toContain("Architect");
    expect(claudeContent).toContain("Developer");
    expect(claudeContent).toContain("Tech Lead");
  });

  it("claudeSource_preservesIntegrationNotes", () => {
    expect(claudeContent).toContain("Integration Notes");
    expect(claudeContent).toContain("x-test-plan");
    expect(claudeContent).toContain("x-review-pr");
  });
});

// ---------------------------------------------------------------------------
// GitHub Source — Post-Deploy Verification Section Existence (UT-28 to UT-33)
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle GitHub source — post-deploy verification existence", () => {
  it("githubSource_phase7_containsPostDeployVerificationSubsection", () => {
    expect(githubContent).toMatch(/Post-Deploy Verification/);
  });

  it("githubSource_phase7_postDeployIsConditionalOnSmokeTests", () => {
    expect(githubContent).toMatch(/smoke_tests/);
  });

  it("githubSource_phase7_postDeployContainsHealthCheck", () => {
    expect(githubContent).toMatch(/Health Check/);
  });

  it("githubSource_phase7_postDeployContainsCriticalPath", () => {
    expect(githubContent).toMatch(/Critical Path/);
  });

  it("githubSource_phase7_postDeployContainsResponseTimeSLO", () => {
    expect(githubContent).toMatch(/Response Time|p95|SLO/);
  });

  it("githubSource_phase7_postDeployContainsErrorRate", () => {
    expect(githubContent).toMatch(/Error Rate|error rate|1%/);
  });
});

// ---------------------------------------------------------------------------
// GitHub Source — Post-Deploy Result Semantics (UT-34 to UT-38)
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle GitHub source — post-deploy result semantics", () => {
  it("githubSource_phase7_postDeployResultPASS", () => {
    expect(githubContent).toMatch(/PASS.*Deploy confirmed|Deploy confirmed.*PASS/is);
  });

  it("githubSource_phase7_postDeployResultFAIL", () => {
    expect(githubContent).toMatch(/FAIL.*rollback|rollback.*FAIL/is);
  });

  it("githubSource_phase7_postDeployResultSKIP", () => {
    expect(githubContent).toMatch(/SKIP.*smoke_tests|smoke_tests.*SKIP/is);
  });

  it("githubSource_phase7_postDeployIsNonBlocking", () => {
    expect(githubContent).toMatch(
      /non-blocking|Non-blocking|NOT auto-rollback|human decision|not.*auto.*rollback/i,
    );
  });

  it("githubSource_phase7_postDeployReferencesRunE2EOrSmokeSkill", () => {
    expect(githubContent).toMatch(/\/run-e2e|\/run-smoke-api/);
  });
});

// ---------------------------------------------------------------------------
// GitHub Source — Conditional DoD Item & Renumbering (UT-39 to UT-42)
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle GitHub source — conditional DoD & renumbering", () => {
  it("githubSource_phase7_conditionalDoDContainsPostDeployItem", () => {
    expect(githubContent).toMatch(/Post-deploy verification/i);
  });

  it("githubSource_phase7_conditionalDoDPostDeployLinkedToSmokeTests", () => {
    const conditionalSection = githubContent.slice(
      githubContent.indexOf("Conditional DoD items"),
    );
    expect(conditionalSection).toMatch(/smoke_tests/);
  });

  it("githubSource_phase7_reportPassFailIsRenumbered", () => {
    expect(githubContent).toMatch(/^7\.\s+Report PASS\/FAIL result/m);
  });

  it("githubSource_phase7_gitCheckoutMainIsRenumbered", () => {
    expect(githubContent).toMatch(/^8\.\s+`git checkout main/m);
  });
});

// ---------------------------------------------------------------------------
// GitHub Source — Backward Compatibility (UT-43 to UT-54)
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle GitHub source — backward compatibility", () => {
  it("githubSource_preserves9PhaseCount", () => {
    expect(githubContent).toMatch(/9 phases.*0-8/);
  });

  it("githubSource_preservesCriticalExecutionRule", () => {
    expect(githubContent).toContain("NEVER stop before Phase 8");
  });

  it("githubSource_preservesPhase7OnlyLegitimateStoppingPoint", () => {
    expect(githubContent).toContain(
      "Phase 8 is the ONLY legitimate stopping point",
    );
  });

  it("githubSource_preservesExistingConditionalDoDItems", () => {
    expect(githubContent).toContain("Contract tests pass");
    expect(githubContent).toContain("Event schemas registered");
    expect(githubContent).toContain("Compliance requirements met");
    expect(githubContent).toContain("Gateway configuration updated");
    expect(githubContent).toMatch(/gRPC proto backward compatible/);
    expect(githubContent).toMatch(/GraphQL schema backward compatible/);
  });

  it("githubSource_preservesTDDDoDItems", () => {
    expect(githubContent).toMatch(/test-first pattern/);
    expect(githubContent).toMatch(/AT-N GREEN/);
    expect(githubContent).toMatch(/TPP ordering|simple to complex/);
  });

  it("githubSource_preservesUpdateREADMEStep", () => {
    expect(githubContent).toContain("Update README if needed");
  });

  it("githubSource_preservesUpdateImplementationMapStep", () => {
    expect(githubContent).toContain("Update IMPLEMENTATION-MAP");
  });

  it("githubSource_preservesDoDChecklistStep", () => {
    expect(githubContent).toContain("Run DoD checklist");
  });

  it("githubSource_preservesAllPlaceholderTokens", () => {
    expect(githubContent).toContain("{{PROJECT_NAME}}");
    expect(githubContent).toContain("{{LANGUAGE}}");
    expect(githubContent).toContain("{{LANGUAGE_VERSION}}");
    expect(githubContent).toContain("{{COMPILE_COMMAND}}");
    expect(githubContent).toContain("{{TEST_COMMAND}}");
    expect(githubContent).toContain("{{COVERAGE_COMMAND}}");
  });

  it("githubSource_preservesCompleteFlowDiagram", () => {
    expect(githubContent).toContain("Phase 0: Preparation");
    expect(githubContent).toContain("Phase 8: Verification");
  });

  it("githubSource_preservesRolesAndModelsTable", () => {
    expect(githubContent).toContain("Roles and Models");
    expect(githubContent).toContain("Architect");
    expect(githubContent).toContain("Developer");
    expect(githubContent).toContain("Tech Lead");
  });

  it("githubSource_preservesIntegrationNotes", () => {
    expect(githubContent).toContain("Integration Notes");
    expect(githubContent).toContain("x-test-plan");
    expect(githubContent).toContain("x-review-pr");
  });
});

// ---------------------------------------------------------------------------
// Dual Copy Consistency (RULE-001) (UT-55 to UT-66)
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle dual copy consistency (RULE-001)", () => {
  it("dualCopy_bothContainPostDeployVerificationSection", () => {
    expect(claudeContent).toMatch(/Post-Deploy Verification/);
    expect(githubContent).toMatch(/Post-Deploy Verification/);
  });

  it("dualCopy_bothContainHealthCheckVerification", () => {
    expect(claudeContent).toMatch(/Health Check/);
    expect(githubContent).toMatch(/Health Check/);
  });

  it("dualCopy_bothContainCriticalPathVerification", () => {
    expect(claudeContent).toMatch(/Critical Path/);
    expect(githubContent).toMatch(/Critical Path/);
  });

  it("dualCopy_bothContainResponseTimeSLO", () => {
    expect(claudeContent).toMatch(/Response Time|p95/);
    expect(githubContent).toMatch(/Response Time|p95/);
  });

  it("dualCopy_bothContainErrorRateThreshold", () => {
    expect(claudeContent).toMatch(/Error Rate|1%/);
    expect(githubContent).toMatch(/Error Rate|1%/);
  });

  it("dualCopy_bothContainPASSFAILSKIPResults", () => {
    for (const result of ["PASS", "FAIL", "SKIP"]) {
      expect(claudeContent).toContain(result);
      expect(githubContent).toContain(result);
    }
  });

  it("dualCopy_bothContainNonBlockingBehavior", () => {
    expect(claudeContent).toMatch(/non-blocking|NOT auto-rollback|human decision/i);
    expect(githubContent).toMatch(/non-blocking|NOT auto-rollback|human decision/i);
  });

  it("dualCopy_bothContainConditionalDoDPostDeployItem", () => {
    expect(claudeContent).toMatch(/Post-deploy verification/i);
    expect(githubContent).toMatch(/Post-deploy verification/i);
  });

  it("dualCopy_bothContainSmokeTestsCondition", () => {
    expect(claudeContent).toMatch(/smoke_tests/);
    expect(githubContent).toMatch(/smoke_tests/);
  });

  it("dualCopy_bothContainRunE2EOrSmokeReference", () => {
    expect(claudeContent).toMatch(/\/run-e2e|\/run-smoke-api/);
    expect(githubContent).toMatch(/\/run-e2e|\/run-smoke-api/);
  });

  it("dualCopy_phaseCount_identical", () => {
    expect(claudeContent).toMatch(/9 phases.*0-8/);
    expect(githubContent).toMatch(/9 phases.*0-8/);
  });

  it("dualCopy_bothContainRenumberedItems", () => {
    expect(claudeContent).toMatch(/^7\.\s+Report PASS\/FAIL result/m);
    expect(githubContent).toMatch(/^7\.\s+Report PASS\/FAIL result/m);
    expect(claudeContent).toMatch(/^8\.\s+`git checkout main/m);
    expect(githubContent).toMatch(/^8\.\s+`git checkout main/m);
  });

  it("dualCopy_pathDifferences_onlyExpected", () => {
    const claudePhase8 = claudeContent.slice(
      claudeContent.indexOf("## Phase 8"),
      claudeContent.indexOf("## Roles and Models"),
    );
    const githubPhase8 = githubContent.slice(
      githubContent.indexOf("## Phase 8"),
      githubContent.indexOf("## Roles and Models"),
    );
    expect(claudePhase8).toBe(githubPhase8);
  });
});
