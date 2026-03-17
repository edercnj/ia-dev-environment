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
  it("skillMd_phases2And3_remainPlaceholders", () => {
    const phase2Idx = content.indexOf("Phase 2");
    const phase2Content = content.slice(
      phase2Idx, content.indexOf("Phase 3", phase2Idx),
    );
    expect(phase2Content).toMatch(
      /placeholder|story-0005|TODO|implemented in|extended by/i,
    );

    const phase3Idx = content.indexOf("Phase 3");
    const phase3Content = content.slice(phase3Idx);
    expect(phase3Content).toMatch(
      /placeholder|story-0005|TODO|implemented in|extended by/i,
    );
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
        "story-0005-0008", "story-0005-0010",
        "story-0005-0011", "story-0005-0013",
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
  ];

  it.each(
    CRITICAL_TERMS.map((term) => [term]),
  )("bothContainTerm_%s_dualCopyConsistency", (term) => {
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
