import { describe, it, expect } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";

const CLAUDE_SOURCE_PATH = path.resolve(
  __dirname,
  "../../..",
  "resources/skills-templates/core/x-dev-adr-automation/SKILL.md",
);

const GITHUB_SOURCE_PATH = path.resolve(
  __dirname,
  "../../..",
  "resources/github-skills-templates/dev/x-dev-adr-automation.md",
);

const claudeSource = fs.readFileSync(CLAUDE_SOURCE_PATH, "utf-8");
const githubSource = fs.readFileSync(GITHUB_SOURCE_PATH, "utf-8");

const REQUIRED_SECTIONS = [
  "When to Use",
  "Input Format",
  "Output Format",
  "Algorithm",
  "Duplicate Detection",
  "Cross-Reference",
  "Sequential Numbering",
  "Index Update",
];

const MINI_ADR_FIELDS = ["title", "context", "decision", "rationale"];

describe("x-dev-adr-automation SKILL.md — frontmatter", () => {
  // UT-1: YAML frontmatter
  it("skillMd_fileExists_hasValidYamlFrontmatter", () => {
    expect(claudeSource).toMatch(/^---\n/);
    expect(claudeSource).toContain("name: x-dev-adr-automation");
    expect(claudeSource).toContain("description:");
  });

  // UT-8: allowed-tools
  it("skillMd_frontmatter_containsAllowedTools", () => {
    expect(claudeSource).toContain("allowed-tools");
    expect(claudeSource).toContain("Read");
    expect(claudeSource).toContain("Write");
    expect(claudeSource).toContain("Edit");
    expect(claudeSource).toContain("Glob");
    expect(claudeSource).toContain("Grep");
  });

  // UT-9: argument-hint
  it("skillMd_frontmatter_containsArgumentHint", () => {
    expect(claudeSource).toContain("argument-hint");
  });
});

describe("x-dev-adr-automation SKILL.md — required sections", () => {
  // UT-2: required sections
  it.each(
    REQUIRED_SECTIONS.map((s) => [s]),
  )("skillMd_containsSection_%s", (section) => {
    expect(claudeSource).toContain(section);
  });

  // UT-2g: Examples section
  it("skillMd_containsSection_Examples", () => {
    expect(claudeSource).toMatch(/[Ee]xample/);
  });
});

describe("x-dev-adr-automation SKILL.md — duplicate detection", () => {
  // UT-3
  it("skillMd_duplicateDetection_containsTitleSimilarityCheck", () => {
    expect(claudeSource).toMatch(/[Dd]uplicate/);
    expect(claudeSource).toMatch(/title/i);
    expect(claudeSource).toMatch(/skip|warning/i);
  });
});

describe("x-dev-adr-automation SKILL.md — cross-reference rules", () => {
  // UT-4
  it("skillMd_crossReference_containsStoryRefAndADRLinks", () => {
    expect(claudeSource).toContain("story-ref");
    expect(claudeSource).toMatch(/architecture plan/i);
  });
});

describe("x-dev-adr-automation SKILL.md — sequential numbering", () => {
  // UT-5
  it("skillMd_algorithm_containsSequentialNumbering", () => {
    expect(claudeSource).toContain("docs/adr/");
    expect(claudeSource).toMatch(/ADR-\d{4}|ADR-NNNN|sequential/i);
  });
});

describe("x-dev-adr-automation SKILL.md — mini-ADR input format", () => {
  // UT-6
  it.each(
    MINI_ADR_FIELDS.map((f) => [f]),
  )("skillMd_inputFormat_containsField_%s", (field) => {
    expect(claudeSource).toMatch(new RegExp(field, "i"));
  });
});

describe("x-dev-adr-automation SKILL.md — example conversion", () => {
  // UT-7
  it("skillMd_examples_containsBeforeAfterConversion", () => {
    expect(claudeSource).toMatch(/[Ss]tatus/);
    expect(claudeSource).toMatch(/[Cc]onsequences/);
  });
});

describe("x-dev-adr-automation SKILL.md — output format", () => {
  // UT-10
  it("skillMd_outputFormat_containsADRFrontmatterFields", () => {
    expect(claudeSource).toMatch(/status/i);
    expect(claudeSource).toMatch(/date/i);
    expect(claudeSource).toMatch(/story-ref/);
  });
});

describe("x-dev-adr-automation SKILL.md — index update", () => {
  // UT-11
  it("skillMd_algorithm_containsIndexUpdateInstructions", () => {
    expect(claudeSource).toMatch(/README\.md/);
    expect(claudeSource).toMatch(/docs\/adr/);
  });
});

describe("x-dev-adr-automation SKILL.md — global output policy", () => {
  // UT-12
  it("skillMd_containsGlobalOutputPolicy_englishOnly", () => {
    expect(claudeSource).toMatch(/English ONLY/i);
  });
});

describe("x-dev-adr-automation GitHub template — frontmatter", () => {
  it("githubMd_fileExists_hasValidYamlFrontmatter", () => {
    expect(githubSource).toMatch(/^---\n/);
    expect(githubSource).toContain("name: x-dev-adr-automation");
    expect(githubSource).toContain("description:");
  });
});

describe("x-dev-adr-automation GitHub template — required sections", () => {
  it.each(
    REQUIRED_SECTIONS.map((s) => [s]),
  )("githubMd_containsSection_%s", (section) => {
    expect(githubSource).toContain(section);
  });

  it("githubMd_containsSection_Examples", () => {
    expect(githubSource).toMatch(/[Ee]xample/);
  });
});

describe("x-dev-adr-automation dual copy consistency (RULE-001)", () => {
  const CRITICAL_TERMS = [
    "docs/adr/",
    "story-ref",
    "README.md",
    "Duplicate",
    "ADR-",
    "Consequences",
    "Sequential",
  ];

  it.each(
    CRITICAL_TERMS.map((term) => [term]),
  )("bothContainTerm_%s_dualCopyConsistency", (term) => {
    expect(claudeSource).toContain(term);
    expect(githubSource).toContain(term);
  });
});
