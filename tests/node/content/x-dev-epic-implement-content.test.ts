import { describe, it, expect } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";

const SKILL_PATH = path.resolve(
  __dirname, "../../..",
  "resources/skills-templates/core/x-dev-epic-implement/SKILL.md",
);

const GITHUB_SKILL_PATH = path.resolve(
  __dirname, "../../..",
  "resources/github-skills-templates/dev/x-dev-epic-implement.md",
);

const content = fs.readFileSync(SKILL_PATH, "utf-8");

const REQUIRED_TOOLS = [
  "Read", "Write", "Edit", "Bash", "Grep", "Glob", "Skill",
];

const ARGUMENT_TOKENS = [
  "EPIC-ID", "--phase", "--story",
  "--skip-review", "--dry-run", "--resume", "--parallel",
];

const REQUIRED_SECTIONS = [
  "When to Use",
  "Input Parsing",
  "Prerequisites",
  "Phase 0",
  "Phase 1",
  "Phase 2",
  "Phase 3",
];

const OPTIONAL_FLAGS = [
  "--phase", "--story", "--skip-review",
  "--dry-run", "--resume", "--parallel",
];

const PREREQUISITE_KEYWORDS: readonly (readonly [string, string])[] = [
  ["docs/stories/epic-", "epicDirectory"],
  ["EPIC-", "epicFile"],
  ["IMPLEMENTATION-MAP", "implementationMap"],
  ["story-", "storyFiles"],
  ["execution-state.json", "resumeCheckpoint"],
];

describe("x-dev-epic-implement SKILL.md — frontmatter", () => {
  it("skillMd_fileExists_hasValidYamlFrontmatter", () => {
    expect(content).toMatch(/^---\n/);
    expect(content).toContain("name: x-dev-epic-implement");
    expect(content).toContain("description:");
  });

  it.each(
    REQUIRED_TOOLS.map((t) => [t]),
  )("skillMd_frontmatter_allowedTools_containsTool_%s", (tool) => {
    expect(content).toContain("allowed-tools:");
    expect(content).toMatch(
      new RegExp(`allowed-tools:.*${tool}`, "s"),
    );
  });

  it.each(
    ARGUMENT_TOKENS.map((t) => [t]),
  )("skillMd_frontmatter_argumentHint_contains_%s", (token) => {
    expect(content).toContain("argument-hint:");
    expect(content).toContain(token);
  });
});

describe("x-dev-epic-implement SKILL.md — global output policy", () => {
  it("skillMd_containsGlobalOutputPolicy_englishOnly", () => {
    expect(content).toContain("Global Output Policy");
    expect(content).toMatch(/English ONLY/i);
  });
});

describe("x-dev-epic-implement SKILL.md — required sections", () => {
  it.each(
    REQUIRED_SECTIONS.map((s) => [s]),
  )("skillMd_containsSection_%s", (section) => {
    expect(content).toContain(section);
  });
});

describe("x-dev-epic-implement SKILL.md — input parsing", () => {
  it("skillMd_inputParsing_containsEpicIdAsRequired", () => {
    expect(content).toMatch(/epic/i);
    expect(content).toMatch(/required|mandatory|positional/i);
  });

  it.each(
    OPTIONAL_FLAGS.map((f) => [f]),
  )("skillMd_inputParsing_containsFlag_%s", (flag) => {
    expect(content).toContain(flag);
  });
});

describe("x-dev-epic-implement SKILL.md — prerequisites check", () => {
  it.each(
    PREREQUISITE_KEYWORDS.map(([kw, label]) => [kw, label]),
  )("skillMd_prerequisites_containsCheck_%s", (keyword) => {
    expect(content).toContain(keyword as string);
  });

  it("skillMd_prerequisites_containsErrorGuidance", () => {
    expect(content).toMatch(/not found|abort|error|missing/i);
  });
});

describe("x-dev-epic-implement SKILL.md — phase structure", () => {
  it("skillMd_phase2_isNotPlaceholder_containsSubstantiveContent", () => {
    const phase2 = extractPhase2();
    expect(phase2).not.toMatch(
      /^[^]*>\s*\*?\*?Placeholder\*?\*?:/im,
    );
    const lines = phase2.split("\n").filter(
      (l) => l.trim().length > 0,
    );
    expect(lines.length).toBeGreaterThanOrEqual(40);
  });

  it("skillMd_phase3_isNotPlaceholder_containsSubstantiveContent", () => {
    const phase3 = extractPhase3();
    expect(phase3).not.toMatch(
      /^[^]*>\s*\*?\*?Placeholder\*?\*?:/im,
    );
    const lines = phase3.split("\n").filter(
      (l) => l.trim().length > 0,
    );
    expect(lines.length).toBeGreaterThanOrEqual(20);
  });

  it("skillMd_phase1_isNotPlaceholder_containsSubstantiveContent", () => {
    const phase1Idx = content.indexOf("Phase 1");
    const phase2Idx = content.indexOf("Phase 2", phase1Idx);
    const phase1Content = content.slice(phase1Idx, phase2Idx);
    expect(phase1Content).not.toMatch(
      /^[^]*>\s*\*?\*?Placeholder\*?\*?:/im,
    );
    const lines = phase1Content.split("\n").filter(
      (l) => l.trim().length > 0,
    );
    expect(lines.length).toBeGreaterThanOrEqual(50);
  });

  it("skillMd_phase0_containsPreparationSteps", () => {
    const phase0Idx = content.indexOf("Phase 0");
    const phase0Content = content.slice(
      phase0Idx, content.indexOf("Phase 1", phase0Idx),
    );
    const hasParsing = /pars/i.test(phase0Content);
    const hasPrereqs = /prerequisite/i.test(phase0Content);
    const hasBranch = /branch/i.test(phase0Content);
    const matchCount = [hasParsing, hasPrereqs, hasBranch]
      .filter(Boolean).length;
    expect(matchCount).toBeGreaterThanOrEqual(2);
  });
});

function extractPhase1(): string {
  const phase1Idx = content.indexOf("Phase 1");
  const phase2Idx = content.indexOf("Phase 2", phase1Idx);
  return content.slice(phase1Idx, phase2Idx);
}

function extractPhase2(): string {
  const phase2Idx = content.indexOf("Phase 2");
  const phase3Idx = content.indexOf("Phase 3", phase2Idx);
  return content.slice(phase2Idx, phase3Idx);
}

function extractPhase3(): string {
  const phase3Idx = content.indexOf("Phase 3");
  const integrationIdx = content.indexOf(
    "## Integration Notes", phase3Idx,
  );
  return integrationIdx > -1
    ? content.slice(phase3Idx, integrationIdx)
    : content.slice(phase3Idx);
}

describe("x-dev-epic-implement SKILL.md — Phase 1 content", () => {
  const phase1 = extractPhase1();

  // TPP Level 2: Scalar — single keyword pair assertions
  describe("TPP Level 2 — scalar keyword assertions", () => {
    it("skillMd_phase1_containsCheckpointIntegration", () => {
      expect(phase1).toContain("createCheckpoint");
      expect(phase1).toContain("updateStoryStatus");
    });

    it("skillMd_phase1_containsMapParserIntegration", () => {
      expect(phase1).toContain("parseImplementationMap");
      expect(phase1).toContain("getExecutableStories");
    });

    it("skillMd_phase1_containsBranchManagement", () => {
      expect(phase1).toContain("feat/epic-");
      expect(phase1).toMatch(/git checkout|branch/i);
    });

    it("skillMd_phase1_containsCriticalPathPriority", () => {
      expect(phase1).toMatch(/critical.?path/i);
    });

    it("skillMd_phase1_containsContextIsolation", () => {
      expect(phase1).toMatch(
        /RULE-001|context isolation|clean context/i,
      );
    });
  });

  // TPP Level 3: Collection — multiple field/reference assertions
  describe("TPP Level 3 — collection assertions", () => {
    it("skillMd_phase1_containsSubagentDispatch", () => {
      expect(phase1).toContain("Agent");
      expect(phase1).toContain("SubagentResult");
      expect(phase1).toContain("x-dev-lifecycle");
    });

    it("skillMd_phase1_containsResultValidation", () => {
      expect(phase1).toContain("status");
      expect(phase1).toContain("findingsCount");
      expect(phase1).toContain("summary");
      expect(phase1).toContain("commitSha");
      expect(phase1).toMatch(/RULE-008/);
    });

    it("skillMd_phase1_containsStatusValues", () => {
      expect(phase1).toContain("IN_PROGRESS");
      expect(phase1).toContain("SUCCESS");
      expect(phase1).toContain("FAILED");
    });

    it("skillMd_phase1_containsExtensionPlaceholders", () => {
      const refs = [
        "story-0005-0006", "story-0005-0007",
        "story-0005-0008", "story-0005-0013",
      ];
      const matchCount = refs.filter(
        (r) => phase1.includes(r),
      ).length;
      expect(matchCount).toBeGreaterThanOrEqual(3);
    });

    it("skillMd_phase1_referencesRuleMarkers", () => {
      const ruleRefs = ["RULE-001", "RULE-002", "RULE-007", "RULE-008"];
      const matchCount = ruleRefs.filter(
        (r) => phase1.includes(r),
      ).length;
      expect(matchCount).toBeGreaterThanOrEqual(3);
    });
  });

  // TPP Level 4: Composite — structural and ordering assertions
  describe("TPP Level 4 — structural assertions", () => {
    it("skillMd_phase1_containsMinimumSubsections", () => {
      const subsectionMatches = phase1.match(/^###\s+1\.\d/gm);
      expect(subsectionMatches).not.toBeNull();
      expect(subsectionMatches!.length).toBeGreaterThanOrEqual(7);
    });

    it("skillMd_phase1_subsectionsInLogicalOrder", () => {
      const initIdx = phase1.indexOf("Initialize");
      const branchIdx = phase1.indexOf("Branch");
      const coreIdx = phase1.indexOf("Core Loop");
      const dispatchIdx = phase1.indexOf("Dispatch");
      const validationIdx = phase1.indexOf("Validation");
      expect(initIdx).toBeGreaterThan(-1);
      expect(coreIdx).toBeGreaterThan(-1);
      expect(initIdx).toBeLessThan(coreIdx);
      expect(branchIdx).toBeLessThan(coreIdx);
      expect(coreIdx).toBeLessThan(
        Math.max(dispatchIdx, validationIdx),
      );
    });

    it("skillMd_phase1_doesNotContainSourceImports", () => {
      expect(phase1).not.toMatch(/^import .* from/m);
      expect(phase1).not.toMatch(/require\(/);
    });
  });
});

describe("x-dev-epic-implement SKILL.md — Phase 2 content", () => {
  const phase2 = extractPhase2();

  // TPP Level 2: Scalar — single keyword pair assertions
  describe("TPP Level 2 — scalar keyword assertions", () => {
    it("skillMd_phase2_containsTechLeadReviewDispatch", () => {
      expect(phase2).toContain("x-review-pr");
      expect(phase2).toMatch(/tech.?lead.?review/i);
    });

    it("skillMd_phase2_containsReportGenerationSubagent", () => {
      expect(phase2).toContain("epic-execution-report");
      expect(phase2).toMatch(/report.?generat/i);
    });

    it("skillMd_phase2_containsPRCreationInstructions", () => {
      expect(phase2).toContain("gh pr create");
      expect(phase2).toContain("git push");
    });

    it("skillMd_phase2_containsPartialCompletionHandling", () => {
      expect(phase2).toContain("[PARTIAL]");
      expect(phase2).toMatch(/partial/i);
    });

    it("skillMd_phase2_containsExecutionReportTemplate", () => {
      expect(phase2).toContain("_TEMPLATE-EPIC-EXECUTION-REPORT");
    });

    it("skillMd_phase2_containsExecutionStateReference", () => {
      expect(phase2).toContain("execution-state.json");
    });

    it("skillMd_phase2_containsSubagentDelegation", () => {
      expect(phase2).toContain("Agent");
      expect(phase2).toMatch(/subagent|dispatch|delegate/i);
    });

    it("skillMd_phase2_containsPRTitleFormat", () => {
      expect(phase2).toMatch(/feat\(epic\)/i);
    });

    it("skillMd_phase2_containsCheckpointUpdate", () => {
      expect(phase2).toMatch(/checkpoint|prLink|pr.?link/i);
    });
  });

  // TPP Level 3: Collection — multiple field/reference assertions
  describe("TPP Level 3 — collection assertions", () => {
    it("skillMd_phase2_containsStatusValues", () => {
      expect(phase2).toContain("SUCCESS");
      expect(phase2).toContain("FAILED");
      expect(phase2).toContain("BLOCKED");
    });

    it("skillMd_phase2_containsReviewResultFields", () => {
      expect(phase2).toMatch(/score/i);
      expect(phase2).toMatch(/decision/i);
      expect(phase2).toMatch(/GO|NO-GO/);
    });
  });

  // TPP Level 4: Composite — structural and ordering assertions
  describe("TPP Level 4 — structural assertions", () => {
    it("skillMd_phase2_containsMinimumSubsections", () => {
      const subsectionMatches = phase2.match(/^###\s+2\.\d/gm);
      expect(subsectionMatches).not.toBeNull();
      expect(subsectionMatches!.length).toBeGreaterThanOrEqual(3);
    });

    it("skillMd_phase2_subsectionsInLogicalOrder", () => {
      const reviewIdx = phase2.search(/tech.?lead.?review/i);
      const reportIdx = phase2.search(/report.?generat/i);
      const prIdx = phase2.indexOf("gh pr create");
      expect(reviewIdx).toBeGreaterThan(-1);
      expect(reportIdx).toBeGreaterThan(-1);
      expect(prIdx).toBeGreaterThan(-1);
      expect(reviewIdx).toBeLessThan(reportIdx);
      expect(reportIdx).toBeLessThan(prIdx);
    });

    it("skillMd_phase2_doesNotContainSourceImports", () => {
      expect(phase2).not.toMatch(/^import .* from/m);
      expect(phase2).not.toMatch(/require\(/);
    });
  });
});

describe("x-dev-epic-implement SKILL.md — Phase 3 content", () => {
  const phase3 = extractPhase3();

  // TPP Level 2: Scalar
  describe("TPP Level 2 — scalar keyword assertions", () => {
    it("skillMd_phase3_containsDoDChecklist", () => {
      expect(phase3).toMatch(/DoD|Definition of Done|checklist/i);
    });

    it("skillMd_phase3_containsFinalStatus", () => {
      expect(phase3).toContain("COMPLETE");
      expect(phase3).toContain("PARTIAL");
      expect(phase3).toContain("FAILED");
    });

    it("skillMd_phase3_containsTestVerification", () => {
      expect(phase3).toMatch(/test.*pass|all.*tests|test suite/i);
    });

    it("skillMd_phase3_containsCoverageVerification", () => {
      expect(phase3).toMatch(/coverage/i);
      expect(phase3).toContain("95%");
      expect(phase3).toContain("90%");
    });
  });

  // TPP Level 3: Collection
  describe("TPP Level 3 — collection assertions", () => {
    it("skillMd_phase3_containsCompletionOutputFields", () => {
      const fields = ["PR", "report", "elapsed"];
      const matchCount = fields.filter(
        (f) => phase3.toLowerCase().includes(f.toLowerCase()),
      ).length;
      expect(matchCount).toBeGreaterThanOrEqual(2);
    });
  });

  // TPP Level 4: Structural
  describe("TPP Level 4 — structural assertions", () => {
    it("skillMd_phase3_containsMinimumSubsections", () => {
      const subsectionMatches = phase3.match(/^###\s+3\.\d/gm);
      expect(subsectionMatches).not.toBeNull();
      expect(subsectionMatches!.length).toBeGreaterThanOrEqual(3);
    });
  });
});

describe("x-dev-epic-implement SKILL.md — Phase 1 extension point", () => {
  it("skillMd_phase1_extensionPoint0011_removedOrResolved", () => {
    const phase1 = extractPhase1();
    expect(phase1).not.toMatch(
      /\[Placeholder.*consolidation.*story-0005-0011\]/i,
    );
  });
});

describe("x-dev-epic-implement GitHub template", () => {
  const ghContent = fs.readFileSync(GITHUB_SKILL_PATH, "utf-8");

  it("githubSkillMd_fileExists_hasValidYamlFrontmatter", () => {
    expect(ghContent).toMatch(/^---\n/);
    expect(ghContent).toContain("name: x-dev-epic-implement");
    expect(ghContent).toContain("description:");
  });

  it("githubSkillMd_containsSection_WhenToUse", () => {
    expect(ghContent).toContain("When to Use");
  });
});

describe("x-dev-epic-implement SKILL.md — partial execution", () => {
  it("skillMd_containsPartialExecutionSection", () => {
    expect(content).toContain("Partial Execution");
  });

  it("skillMd_partialExecution_containsMutualExclusivityRule", () => {
    expect(content).toContain("mutually exclusive");
  });

  it("skillMd_partialExecution_containsPhaseFlowDescription", () => {
    const partialIdx = content.indexOf("## Partial Execution");
    expect(partialIdx).toBeGreaterThanOrEqual(0);
    const phase0Idx = content.indexOf("## Phase 0", partialIdx);
    expect(phase0Idx).toBeGreaterThan(partialIdx);
    const partialSection = content.slice(partialIdx, phase0Idx);
    expect(partialSection).toContain("--phase");
    expect(partialSection).toContain("integrity gate");
  });

  it("skillMd_partialExecution_containsStoryFlowDescription", () => {
    const partialIdx = content.indexOf("## Partial Execution");
    expect(partialIdx).toBeGreaterThanOrEqual(0);
    const phase0Idx = content.indexOf("## Phase 0", partialIdx);
    expect(phase0Idx).toBeGreaterThan(partialIdx);
    const partialSection = content.slice(partialIdx, phase0Idx);
    expect(partialSection).toContain("--story");
    expect(partialSection).toContain("no integrity gate");
  });

  it.each([
    ["does not exist. Max phase is"],
    ["must be complete before phase"],
    ["not found in implementation map"],
    ["Dependencies not satisfied"],
    ["mutually exclusive"],
  ])("skillMd_partialExecution_containsErrorSpec_%s", (pattern) => {
    expect(content).toContain(pattern);
  });
});

describe("x-dev-epic-implement SKILL.md — integrity gate section", () => {
  it("skillMd_containsSection_IntegrityGate", () => {
    expect(content).toContain("Integrity Gate");
  });

  it("skillMd_integrityGate_referencesCompileCommand", () => {
    expect(content).toContain("{{COMPILE_COMMAND}}");
  });

  it("skillMd_integrityGate_referencesTestCommand", () => {
    expect(content).toContain("{{TEST_COMMAND}}");
  });

  it("skillMd_integrityGate_referencesCoverageCommand", () => {
    expect(content).toContain("{{COVERAGE_COMMAND}}");
  });

  it("skillMd_integrityGate_referencesRegressionDiagnosis", () => {
    expect(content).toMatch(/regression/i);
    expect(content).toMatch(/correlat|diagnos|identif/i);
  });

  it("skillMd_integrityGate_referencesGitRevert", () => {
    expect(content).toMatch(/git revert/i);
  });

  it("skillMd_integrityGate_referencesUpdateIntegrityGate", () => {
    expect(content).toContain("updateIntegrityGate");
  });
});
describe("x-dev-epic-implement dual copy consistency (RULE-001)", () => {
  const ghContent = fs.readFileSync(GITHUB_SKILL_PATH, "utf-8");
  const CRITICAL_TERMS = [
    "docs/stories/epic-",
    "IMPLEMENTATION-MAP",
    "--phase",
    "--resume",
    "execution-state.json",
    "x-dev-lifecycle",
    "Phase 0",
    "Phase 1",
    "Phase 2",
    "Phase 3",
    "Integrity Gate",
    "updateIntegrityGate",
    "getExecutableStories",
    "SubagentResult",
    "IN_PROGRESS",
    "createCheckpoint",
    "RULE-008",
    "x-review-pr",
    "gh pr create",
    "epic-execution-report",
    "[PARTIAL]",
    "git push",
    "NO-GO",
    "DoD",
  ];

  it.each(
    CRITICAL_TERMS.map((term) => [term]),
  )("bothContainTerm_%s_dualCopyConsistency", (term) => {
    expect(content).toContain(term);
    expect(ghContent).toContain(term);
  });

  const PARTIAL_EXECUTION_TERMS = [
    "Partial Execution",
    "mutually exclusive",
    "integrity gate",
    "does not exist. Max phase is",
    "Dependencies not satisfied",
  ];

  it.each(
    PARTIAL_EXECUTION_TERMS.map((term) => [term]),
  )("bothContainPartialExecTerm_%s_dualCopyConsistency", (term) => {
    expect(content).toContain(term);
    expect(ghContent).toContain(term);
  });

  it("githubSkillMd_containsIntegrityGateSection", () => {
    expect(ghContent).toContain("Integrity Gate");
    expect(ghContent).toContain("{{COMPILE_COMMAND}}");
    expect(ghContent).toContain("{{TEST_COMMAND}}");
    expect(ghContent).toContain("{{COVERAGE_COMMAND}}");
  });
});

describe("x-dev-epic-implement SKILL.md — resume workflow", () => {
  const ghContent = fs.readFileSync(GITHUB_SKILL_PATH, "utf-8");

  function extractResumeSection(text: string): string {
    const startIdx = text.indexOf("Resume Workflow");
    expect(startIdx).toBeGreaterThanOrEqual(0);
    const endIdx = text.indexOf("Phase 1", startIdx);
    expect(endIdx).toBeGreaterThan(startIdx);
    return text.slice(startIdx, endIdx);
  }

  it("skillMd_containsResumeWorkflowSection", () => {
    expect(content).toMatch(/##\s+Resume Workflow/);
  });

  it("skillMd_resumeSection_containsReclassificationTable", () => {
    const resumeContent = extractResumeSection(content);
    expect(resumeContent).toContain("IN_PROGRESS");
    expect(resumeContent).toContain("SUCCESS");
    expect(resumeContent).toContain("FAILED");
    expect(resumeContent).toContain("BLOCKED");
    expect(resumeContent).toContain("PENDING");
  });

  it("skillMd_resumeSection_containsBranchRecovery", () => {
    const resumeContent = extractResumeSection(content);
    expect(resumeContent).toMatch(/checkout/i);
  });

  it("skillMd_resumeSection_containsReevaluate", () => {
    const resumeContent = extractResumeSection(content);
    expect(resumeContent).toMatch(/reevaluat/i);
  });

  it("dualCopy_bothContainResumeWorkflow", () => {
    expect(content).toMatch(/Resume Workflow/);
    expect(ghContent).toMatch(/Resume Workflow/);
  });

  it("dualCopy_bothContainReclassificationTerms", () => {
    const RESUME_TERMS = [
      "IN_PROGRESS",
      "MAX_RETRIES",
      "BLOCKED",
      "PENDING",
    ];
    for (const term of RESUME_TERMS) {
      expect(content).toContain(term);
      expect(ghContent).toContain(term);
    }
  });
});

// === story-0005-0010: Parallel Execution with Worktrees ===

function extractParallelSections(): string {
  const phase1 = extractPhase1();
  const parallelIdx = phase1.search(
    /parallel.*worktree.*dispatch|worktree.*dispatch/i,
  );
  if (parallelIdx === -1) return "";
  return phase1.slice(parallelIdx);
}

describe("x-dev-epic-implement SKILL.md — Parallel Execution (story-0005-0010)", () => {
  const phase1 = extractPhase1();

  // TPP Level 1: Degenerate — placeholder removed
  describe("TPP Level 1 — degenerate (placeholder removal)", () => {
    it("skillMd_phase1_parallelPlaceholder_removed", () => {
      expect(phase1).not.toMatch(
        /\[Placeholder.*parallel.*worktree.*story-0005-0010\]/i,
      );
    });

    it("skillMd_phase1_extensionPoint0010_removedFromList", () => {
      const extIdx = phase1.indexOf("Extension Points");
      expect(extIdx).toBeGreaterThanOrEqual(0);
      const extPoints = phase1.slice(extIdx);
      expect(extPoints).not.toMatch(
        /\[Placeholder.*parallel.*story-0005-0010\]/i,
      );
    });
  });

  // TPP Level 2: Scalar — keyword pair assertions
  describe("TPP Level 2 — scalar keyword assertions", () => {
    it("skillMd_phase1_containsWorktreeIsolationKeyword", () => {
      expect(phase1).toContain("isolation");
      expect(phase1).toMatch(/worktree/i);
    });

    it("skillMd_phase1_containsSingleMessageDispatch", () => {
      expect(phase1).toMatch(/SINGLE message/i);
    });

    it("skillMd_phase1_containsMergeStrategy", () => {
      expect(phase1).toMatch(/merge.*strategy|merge.*sequen/i);
    });

    it("skillMd_phase1_containsConflictResolution", () => {
      expect(phase1).toMatch(/conflict.*resol/i);
    });

    it("skillMd_phase1_containsWorktreeCleanup", () => {
      expect(phase1).toMatch(/worktree.*clean|cleanup/i);
    });
  });

  // TPP Level 3: Collection — multiple keyword assertions
  describe("TPP Level 3 — collection assertions", () => {
    it("skillMd_phase1_parallelDispatch_referencesRequiredKeywords", () => {
      const keywords = [
        "Agent", "isolation", "worktree",
        "SINGLE message", "getExecutableStories",
      ];
      const matchCount = keywords.filter(
        (kw) => phase1.includes(kw),
      ).length;
      expect(matchCount).toBeGreaterThanOrEqual(4);
    });

    it("skillMd_phase1_mergeStrategy_referencesRuleMarkers", () => {
      const markers = [
        "RULE-002", "RULE-007",
        "updateStoryStatus", "checkpoint",
      ];
      const matchCount = markers.filter(
        (m) => phase1.includes(m),
      ).length;
      expect(matchCount).toBeGreaterThanOrEqual(3);
    });

    it("skillMd_phase1_parallelFallback_documentsSequentialDefault", () => {
      expect(phase1).toMatch(/--parallel/);
      expect(phase1).toMatch(/sequential|sequen/i);
    });

    it("skillMd_phase1_conflictResolution_containsSuccessAndFailure", () => {
      const parallelContent = extractParallelSections();
      expect(parallelContent.length).toBeGreaterThan(0);
      expect(parallelContent).toMatch(/conflict/i);
      expect(parallelContent).toMatch(/irresol|FAILED|fail/i);
    });
  });

  // TPP Level 4: Composite — structural assertions
  describe("TPP Level 4 — structural assertions", () => {
    it("skillMd_phase1_parallelSections_haveMinimumSubsections", () => {
      const parallelKeywords = [
        /parallel.*dispatch|worktree.*dispatch/i,
        /merge.*strategy|merge.*sequen/i,
        /conflict.*resol/i,
        /worktree.*clean|cleanup/i,
      ];
      const matchCount = parallelKeywords.filter(
        (re) => re.test(phase1),
      ).length;
      expect(matchCount).toBeGreaterThanOrEqual(4);
    });

    it("skillMd_phase1_parallelSections_inLogicalOrder", () => {
      const dispatchIdx = phase1.search(
        /parallel.*worktree.*dispatch|worktree.*dispatch/i,
      );
      const mergeIdx = phase1.search(
        /merge.*strategy|merge.*sequen/i,
      );
      const conflictIdx = phase1.search(
        /conflict.*resolution.*subagent/i,
      );
      const cleanupIdx = phase1.search(
        /worktree.*cleanup/i,
      );
      expect(dispatchIdx).toBeGreaterThan(-1);
      expect(mergeIdx).toBeGreaterThan(-1);
      expect(conflictIdx).toBeGreaterThan(-1);
      expect(cleanupIdx).toBeGreaterThan(-1);
      expect(dispatchIdx).toBeLessThan(mergeIdx);
      expect(mergeIdx).toBeLessThan(conflictIdx);
      expect(conflictIdx).toBeLessThan(cleanupIdx);
    });

    it("skillMd_phase1_parallelSections_referenceFailureHandling", () => {
      expect(phase1).toMatch(
        /story-0005-0007|failure handling|retry|block propagation/i,
      );
    });

    it("skillMd_phase1_checkpointTiming_afterMergeNotAfterSubagent", () => {
      expect(phase1).toMatch(
        /checkpoint.*after.*merge|after.*each.*merge|merge.*checkpoint/i,
      );
    });
  });
});

describe("x-dev-epic-implement SKILL.md — Parallel dual-copy (story-0005-0010)", () => {
  const ghContent = fs.readFileSync(GITHUB_SKILL_PATH, "utf-8");

  const PARALLEL_TERMS = [
    "worktree",
    "SINGLE message",
    "conflict",
    "merge",
    "cleanup",
  ];

  // TPP Level 5: Edge — dual-copy consistency for new terms
  it.each(
    PARALLEL_TERMS.map((term) => [term]),
  )("bothContainParallelTerm_%s_dualCopyConsistency", (term) => {
    expect(content).toMatch(new RegExp(term, "i"));
    expect(ghContent).toMatch(new RegExp(term, "i"));
  });
});
