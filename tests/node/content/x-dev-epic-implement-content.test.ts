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
  it("skillMd_phases1to3_arePlaceholders", () => {
    const phase1Idx = content.indexOf("Phase 1");
    const phase1Content = content.slice(
      phase1Idx, content.indexOf("Phase 2", phase1Idx),
    );
    expect(phase1Content).toMatch(
      /placeholder|story-0005|TODO|implemented in|extended by/i,
    );

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
});
