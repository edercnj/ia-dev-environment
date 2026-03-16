import { describe, it, expect, beforeAll } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";

const SKILL_PATH = path.resolve(
  __dirname,
  "../../../resources/skills-templates/core/x-dev-architecture-plan/SKILL.md",
);
const GITHUB_SKILL_PATH = path.resolve(
  __dirname,
  "../../../resources/github-skills-templates/dev/x-dev-architecture-plan.md",
);

let skillContent: string;
let githubSkillContent: string;

beforeAll(() => {
  skillContent = fs.readFileSync(SKILL_PATH, "utf-8");
  githubSkillContent = fs.readFileSync(GITHUB_SKILL_PATH, "utf-8");
});

function extractSection(content: string, heading: string): string {
  const parts = content.split(`## ${heading}`);
  if (parts.length < 2) return "";
  return parts[1]!.split(/\n## (?!#)/)[0]!;
}

function extractFrontmatter(content: string): string {
  const match = content.match(/^---\n([\s\S]*?)\n---/);
  return match ? match[1]! : "";
}

describe("x-dev-architecture-plan SKILL.md — Degenerate", () => {
  it("fileExists_atExpectedPath_returnsTrue", () => {
    expect(fs.existsSync(SKILL_PATH)).toBe(true);
  });

  it("fileContent_isNonEmpty_returnsTrue", () => {
    expect(skillContent.length).toBeGreaterThan(0);
  });

  it("frontmatter_exists_startsAndEndsWithTripleDash", () => {
    expect(skillContent).toMatch(/^---\n[\s\S]*?\n---/);
  });
});

describe("x-dev-architecture-plan SKILL.md — Frontmatter Fields", () => {
  it("frontmatter_name_equalsXDevArchitecturePlan", () => {
    expect(skillContent).toMatch(
      /name:\s+x-dev-architecture-plan/,
    );
  });

  it("frontmatter_description_isNonEmpty", () => {
    const fm = extractFrontmatter(skillContent);
    const descMatch = fm.match(/description:\s*"(.+?)"/s);
    expect(descMatch).not.toBeNull();
    expect(descMatch![1]!.trim().length).toBeGreaterThan(0);
  });

  it("frontmatter_allowedTools_containsRequiredTools", () => {
    const fm = extractFrontmatter(skillContent);
    expect(fm).toMatch(/allowed-tools:.*Read/);
    expect(fm).toMatch(/allowed-tools:.*Write/);
    expect(fm).toMatch(/allowed-tools:.*Edit/);
    expect(fm).toMatch(/allowed-tools:.*Bash/);
    expect(fm).toMatch(/allowed-tools:.*Grep/);
    expect(fm).toMatch(/allowed-tools:.*Glob/);
  });

  it("frontmatter_argumentHint_containsStoryIdOrFeatureName", () => {
    const fm = extractFrontmatter(skillContent);
    expect(fm).toMatch(/argument-hint:.*STORY-ID|feature/i);
  });

  it("frontmatter_doesNotContain_userInvocableFalse", () => {
    expect(skillContent).not.toMatch(/user-invocable:\s*false/);
  });
});

describe("x-dev-architecture-plan SKILL.md — Global Output Policy", () => {
  it("globalOutputPolicy_sectionExists_returnsTrue", () => {
    expect(skillContent).toContain("## Global Output Policy");
  });
});

describe("x-dev-architecture-plan SKILL.md — Decision Tree", () => {
  it("whenToUse_sectionExists_returnsTrue", () => {
    expect(skillContent).toContain("## When to Use");
  });

  it.each([
    "Full Architecture Plan",
    "Simplified Architecture Plan",
    "Skip Architecture Plan",
  ])("decisionTree_contains_%s_returnsTrue", (outcome) => {
    expect(skillContent).toContain(outcome);
  });

  it("decisionTree_fullPlan_listsNewServiceCondition", () => {
    expect(skillContent).toMatch(
      /full architecture plan[\s\S]*?new service/i,
    );
  });

  it("decisionTree_summaryTable_has3Rows", () => {
    const section = extractSection(skillContent, "When to Use");
    const tableRows = section
      .split("\n")
      .filter(
        (line) =>
          line.startsWith("|") &&
          !line.includes("---") &&
          !line.includes("Condition"),
      );
    expect(tableRows.length).toBeGreaterThanOrEqual(3);
  });
});

describe("x-dev-architecture-plan SKILL.md — Knowledge Packs", () => {
  it("knowledgePacks_sectionExists_returnsTrue", () => {
    expect(skillContent).toContain("## Knowledge Packs");
  });

  it("knowledgePacks_table_hasAtLeast6DataRows", () => {
    const section = extractSection(skillContent, "Knowledge Packs");
    const dataRows = section
      .split("\n")
      .filter(
        (line) =>
          line.startsWith("|") &&
          !line.includes("---") &&
          !line.includes("Knowledge Pack") &&
          !line.includes("# ") &&
          line.trim().length > 0,
      );
    expect(dataRows.length).toBeGreaterThanOrEqual(6);
  });

  it.each([
    "architecture",
    "protocols",
    "security",
    "observability",
    "resilience",
    "infrastructure",
    "compliance",
  ])("knowledgePacks_includes_%s_returnsTrue", (kp) => {
    const section = extractSection(skillContent, "Knowledge Packs");
    expect(section.toLowerCase()).toContain(kp);
  });

  it("knowledgePacks_paths_containSkillsPrefix", () => {
    const section = extractSection(skillContent, "Knowledge Packs");
    expect(section).toMatch(/skills\//);
  });
});

describe("x-dev-architecture-plan SKILL.md — Output Structure", () => {
  it("outputStructure_sectionExists_returnsTrue", () => {
    expect(skillContent).toContain("## Output Structure");
  });

  it.each([
    "Component Diagram",
    "Sequence Diagrams",
    "Deployment Diagram",
    "External Connections",
    "Architecture Decisions",
    "Technology Stack",
    "Non-Functional Requirements",
    "Observability Strategy",
    "Resilience Strategy",
    "Impact Analysis",
    "Data Model",
  ])("outputStructure_contains_%s_returnsTrue", (section) => {
    expect(skillContent).toContain(section);
  });

  it("outputStructure_hasAtLeast10MandatorySections", () => {
    const section = extractSection(skillContent, "Output Structure");
    const mandatoryMarkers = section
      .split("\n")
      .filter(
        (line) => line.includes("| M |") || line.includes("| Yes |"),
      );
    expect(mandatoryMarkers.length).toBeGreaterThanOrEqual(10);
  });

  it("outputStructure_containsOutputPath_withConvention", () => {
    expect(skillContent).toMatch(
      /architecture-story-XXXX-YYYY\.md/,
    );
  });
});

describe("x-dev-architecture-plan SKILL.md — Mini-ADR Format", () => {
  it("miniAdrFormat_sectionExists_returnsTrue", () => {
    expect(skillContent).toContain("## Mini-ADR Format");
  });

  it.each([
    "Context",
    "Decision",
    "Rationale",
    "Consequences",
    "Story Reference",
  ])("miniAdrFormat_contains_%s_returnsTrue", (field) => {
    const section = extractSection(skillContent, "Mini-ADR Format");
    expect(section).toContain(field);
  });

  it("miniAdrFormat_containsAdrNumberingPattern", () => {
    const section = extractSection(skillContent, "Mini-ADR Format");
    expect(section).toMatch(/ADR-NNN|ADR-001/);
  });

  it("miniAdrFormat_containsStatusValues", () => {
    const section = extractSection(skillContent, "Mini-ADR Format");
    expect(section).toContain("Proposed");
    expect(section).toContain("Accepted");
  });
});

describe("x-dev-architecture-plan SKILL.md — Subagent Prompt", () => {
  it("subagentPrompt_sectionExists_returnsTrue", () => {
    expect(skillContent).toContain("## Subagent Prompt");
  });

  it("subagentPrompt_mentionsArchitectRole_returnsTrue", () => {
    const section = extractSection(skillContent, "Subagent Prompt");
    expect(section).toMatch(/architect/i);
  });

  it("subagentPrompt_containsProjectNamePlaceholder", () => {
    const section = extractSection(skillContent, "Subagent Prompt");
    expect(section).toContain("{{PROJECT_NAME}}");
  });

  it("subagentPrompt_referencesArchitectureKP", () => {
    const section = extractSection(skillContent, "Subagent Prompt");
    expect(section).toMatch(/skills\/architecture/);
  });

  it("subagentPrompt_mentionsDecisionTree", () => {
    const section = extractSection(skillContent, "Subagent Prompt");
    expect(section).toMatch(/decision tree/i);
  });

  it("subagentPrompt_mentionsMermaidConvention", () => {
    const section = extractSection(skillContent, "Subagent Prompt");
    expect(section).toMatch(/mermaid/i);
  });
});

describe("x-dev-architecture-plan — GitHub template", () => {
  it("githubTemplate_fileExists_returnsTrue", () => {
    expect(fs.existsSync(GITHUB_SKILL_PATH)).toBe(true);
  });

  it("githubTemplate_frontmatter_nameMatches", () => {
    expect(githubSkillContent).toMatch(
      /name:\s+x-dev-architecture-plan/,
    );
  });

  it.each([
    "Full Architecture Plan",
    "Simplified Architecture Plan",
    "Skip Architecture Plan",
  ])("githubTemplate_decisionTree_contains_%s", (outcome) => {
    expect(githubSkillContent).toContain(outcome);
  });

  it.each([
    "Knowledge Packs",
    "Output Structure",
    "Mini-ADR Format",
    "Subagent Prompt",
  ])("githubTemplate_containsSection_%s", (section) => {
    expect(githubSkillContent).toContain(`## ${section}`);
  });

  it("githubTemplate_usesGithubPaths_notClaudePaths", () => {
    expect(githubSkillContent).toMatch(/\.github\/skills\//);
  });

  it("githubTemplate_containsProjectNamePlaceholder", () => {
    expect(githubSkillContent).toContain("{{PROJECT_NAME}}");
  });
});

describe("x-dev-architecture-plan — Dual Copy Consistency (RULE-001)", () => {
  const SHARED_SECTIONS = [
    "When to Use",
    "Knowledge Packs",
    "Output Structure",
    "Mini-ADR Format",
    "Subagent Prompt",
  ] as const;

  it.each(SHARED_SECTIONS)(
    "dualCopy_section_%s_existsInBothTemplates",
    (heading) => {
      const claudeSection = extractSection(skillContent, heading);
      const githubSection = extractSection(
        githubSkillContent,
        heading,
      );
      expect(claudeSection.length).toBeGreaterThan(0);
      expect(githubSection.length).toBeGreaterThan(0);
    },
  );

  it("dualCopy_decisionTree_sameOutcomesInBothTemplates", () => {
    const outcomes = [
      "Full Architecture Plan",
      "Simplified Architecture Plan",
      "Skip Architecture Plan",
    ];
    for (const outcome of outcomes) {
      expect(skillContent).toContain(outcome);
      expect(githubSkillContent).toContain(outcome);
    }
  });

  it("dualCopy_miniAdrFields_sameInBothTemplates", () => {
    const fields = [
      "Context",
      "Decision",
      "Rationale",
      "Story Reference",
    ];
    const claudeAdr = extractSection(skillContent, "Mini-ADR Format");
    const githubAdr = extractSection(
      githubSkillContent,
      "Mini-ADR Format",
    );
    for (const field of fields) {
      expect(claudeAdr).toContain(field);
      expect(githubAdr).toContain(field);
    }
  });
});

describe("x-dev-architecture-plan — Exception Paths", () => {
  it("extractSection_missingHeading_returnsEmptyString", () => {
    const result = extractSection(
      skillContent,
      "Nonexistent Section",
    );
    expect(result).toBe("");
  });

  it("extractSection_emptyContent_returnsEmptyString", () => {
    const result = extractSection("", "Any Heading");
    expect(result).toBe("");
  });

  it("extractFrontmatter_malformedContent_returnsEmptyString", () => {
    const result = extractFrontmatter("no frontmatter here");
    expect(result).toBe("");
  });

  it("extractFrontmatter_validContent_returnsNonEmpty", () => {
    const result = extractFrontmatter(skillContent);
    expect(result.length).toBeGreaterThan(0);
  });
});
