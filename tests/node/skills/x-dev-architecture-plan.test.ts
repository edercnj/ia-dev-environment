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

describe("x-dev-architecture-plan SKILL.md — Frontmatter", () => {
  it("fileExists_atExpectedPath", () => {
    expect(fs.existsSync(SKILL_PATH)).toBe(true);
  });

  it("frontmatter_containsNameField", () => {
    expect(skillContent).toMatch(/^---\n[\s\S]*?name:\s+/m);
  });

  it("frontmatter_nameEquals_xDevArchitecturePlan", () => {
    expect(skillContent).toMatch(
      /name:\s+x-dev-architecture-plan/,
    );
  });

  it("frontmatter_containsDescriptionField", () => {
    expect(skillContent).toMatch(/^---\n[\s\S]*?description:\s+/m);
  });

  it("frontmatter_containsAllowedToolsField", () => {
    expect(skillContent).toMatch(
      /^---\n[\s\S]*?allowed-tools:\s+/m,
    );
  });

  it("frontmatter_containsArgumentHintField", () => {
    expect(skillContent).toMatch(
      /^---\n[\s\S]*?argument-hint:\s+/m,
    );
  });

  it("frontmatter_doesNotContain_userInvocableFalse", () => {
    expect(skillContent).not.toMatch(/user-invocable:\s*false/);
  });
});

describe("x-dev-architecture-plan SKILL.md — Global Output Policy", () => {
  it("globalOutputPolicy_sectionExists", () => {
    expect(skillContent).toContain("## Global Output Policy");
  });
});

describe("x-dev-architecture-plan SKILL.md — Decision Tree", () => {
  it("whenToUse_sectionExists", () => {
    expect(skillContent).toContain("## When to Use");
  });

  it("decisionTree_containsFullPlanOutcome", () => {
    expect(skillContent).toContain("Full Architecture Plan");
  });

  it("decisionTree_containsSimplifiedPlanOutcome", () => {
    expect(skillContent).toContain("Simplified Architecture Plan");
  });

  it("decisionTree_containsSkipOutcome", () => {
    expect(skillContent).toContain("Skip Architecture Plan");
  });

  it("decisionTree_fullPlan_listsNewService", () => {
    expect(skillContent).toMatch(
      /full architecture plan[\s\S]*?new service/i,
    );
  });
});

describe("x-dev-architecture-plan SKILL.md — Knowledge Packs", () => {
  it("knowledgePacks_sectionExists", () => {
    expect(skillContent).toContain("## Knowledge Packs");
  });

  it("knowledgePacks_listsAtLeast6KPs", () => {
    const kpSection = skillContent.split("## Knowledge Packs")[1]!
      .split(/\n## /)[0]!;
    const pipeRows = kpSection
      .split("\n")
      .filter(
        (line) =>
          line.startsWith("|") &&
          !line.includes("---") &&
          !line.includes("# "),
      );
    const dataRows = pipeRows.filter(
      (row) => !row.includes("Knowledge Pack") && row.trim().length > 0,
    );
    expect(dataRows.length).toBeGreaterThanOrEqual(6);
  });

  it("knowledgePacks_includesArchitecture", () => {
    expect(skillContent).toMatch(
      /knowledge packs[\s\S]*?architecture/i,
    );
  });

  it("knowledgePacks_includesProtocols", () => {
    expect(skillContent).toMatch(
      /knowledge packs[\s\S]*?protocols/i,
    );
  });

  it("knowledgePacks_includesSecurity", () => {
    expect(skillContent).toMatch(
      /knowledge packs[\s\S]*?security/i,
    );
  });

  it("knowledgePacks_includesObservability", () => {
    expect(skillContent).toMatch(
      /knowledge packs[\s\S]*?observability/i,
    );
  });

  it("knowledgePacks_includesResilience", () => {
    expect(skillContent).toMatch(
      /knowledge packs[\s\S]*?resilience/i,
    );
  });

  it("knowledgePacks_pathsContainSkillsPrefix", () => {
    const kpSection = skillContent.split("## Knowledge Packs")[1]!
      .split(/\n## /)[0]!;
    expect(kpSection).toMatch(/skills\//);
  });
});

describe("x-dev-architecture-plan SKILL.md — Output Structure", () => {
  it("outputStructure_sectionExists", () => {
    expect(skillContent).toContain("## Output Structure");
  });

  it("outputStructure_containsComponentDiagram", () => {
    expect(skillContent).toContain("Component Diagram");
  });

  it("outputStructure_containsSequenceDiagrams", () => {
    expect(skillContent).toContain("Sequence Diagrams");
  });

  it("outputStructure_containsDeploymentDiagram", () => {
    expect(skillContent).toContain("Deployment Diagram");
  });

  it("outputStructure_containsExternalConnections", () => {
    expect(skillContent).toContain("External Connections");
  });

  it("outputStructure_containsArchitectureDecisions", () => {
    expect(skillContent).toContain("Architecture Decisions");
  });

  it("outputStructure_containsTechnologyStack", () => {
    expect(skillContent).toContain("Technology Stack");
  });

  it("outputStructure_containsNFRs", () => {
    expect(skillContent).toContain("Non-Functional Requirements");
  });

  it("outputStructure_containsObservabilityStrategy", () => {
    expect(skillContent).toContain("Observability Strategy");
  });

  it("outputStructure_containsResilienceStrategy", () => {
    expect(skillContent).toContain("Resilience Strategy");
  });

  it("outputStructure_containsImpactAnalysis", () => {
    expect(skillContent).toContain("Impact Analysis");
  });

  it("outputStructure_hasAtLeast10MandatorySections", () => {
    const outputSection = skillContent
      .split("## Output Structure")[1]!
      .split(/\n## (?!#)/)[0]!;
    const mandatoryMarkers = outputSection
      .split("\n")
      .filter((line) => line.includes("| M |") || line.includes("| Yes |"));
    expect(mandatoryMarkers.length).toBeGreaterThanOrEqual(10);
  });

  it("outputStructure_containsOutputPath", () => {
    expect(skillContent).toMatch(
      /architecture-story-XXXX-YYYY\.md/,
    );
  });
});

describe("x-dev-architecture-plan SKILL.md — Mini-ADR Format", () => {
  it("miniAdrFormat_sectionExists", () => {
    expect(skillContent).toContain("## Mini-ADR Format");
  });

  it("miniAdrFormat_containsContext", () => {
    const adrSection = skillContent.split("## Mini-ADR Format")[1]!
      .split(/\n## /)[0]!;
    expect(adrSection).toContain("Context");
  });

  it("miniAdrFormat_containsDecision", () => {
    const adrSection = skillContent.split("## Mini-ADR Format")[1]!
      .split(/\n## /)[0]!;
    expect(adrSection).toContain("Decision");
  });

  it("miniAdrFormat_containsRationale", () => {
    const adrSection = skillContent.split("## Mini-ADR Format")[1]!
      .split(/\n## /)[0]!;
    expect(adrSection).toContain("Rationale");
  });

  it("miniAdrFormat_containsStoryReference", () => {
    const adrSection = skillContent.split("## Mini-ADR Format")[1]!
      .split(/\n## /)[0]!;
    expect(adrSection).toContain("Story Reference");
  });
});

describe("x-dev-architecture-plan SKILL.md — Subagent Prompt", () => {
  it("subagentPrompt_sectionExists", () => {
    expect(skillContent).toContain("## Subagent Prompt");
  });

  it("subagentPrompt_mentionsArchitectRole", () => {
    const promptSection = skillContent
      .split("## Subagent Prompt")[1]!
      .split(/\n## /)[0]!;
    expect(promptSection).toMatch(/architect/i);
  });

  it("subagentPrompt_referencesProjectNamePlaceholder", () => {
    const promptSection = skillContent
      .split("## Subagent Prompt")[1]!
      .split(/\n## /)[0]!;
    expect(promptSection).toContain("{{PROJECT_NAME}}");
  });

  it("subagentPrompt_mentionsKnowledgePacks", () => {
    const promptSection = skillContent
      .split("## Subagent Prompt")[1]!
      .split(/\n## /)[0]!;
    expect(promptSection).toMatch(/skills\/architecture/);
  });

  it("subagentPrompt_mentionsDecisionTree", () => {
    const promptSection = skillContent
      .split("## Subagent Prompt")[1]!
      .split(/\n## /)[0]!;
    expect(promptSection).toMatch(/decision tree/i);
  });

  it("subagentPrompt_mentionsMermaid", () => {
    const promptSection = skillContent
      .split("## Subagent Prompt")[1]!
      .split(/\n## /)[0]!;
    expect(promptSection).toMatch(/mermaid/i);
  });
});

describe("x-dev-architecture-plan — GitHub template", () => {
  it("githubTemplate_fileExists", () => {
    expect(fs.existsSync(GITHUB_SKILL_PATH)).toBe(true);
  });

  it("githubTemplate_frontmatter_nameMatches", () => {
    expect(githubSkillContent).toMatch(
      /name:\s+x-dev-architecture-plan/,
    );
  });

  it("githubTemplate_containsDecisionTree", () => {
    expect(githubSkillContent).toContain("Full Architecture Plan");
    expect(githubSkillContent).toContain("Simplified Architecture Plan");
    expect(githubSkillContent).toContain("Skip Architecture Plan");
  });

  it("githubTemplate_containsKnowledgePacks", () => {
    expect(githubSkillContent).toContain("## Knowledge Packs");
  });

  it("githubTemplate_containsOutputStructure", () => {
    expect(githubSkillContent).toContain("## Output Structure");
  });

  it("githubTemplate_containsMiniAdrFormat", () => {
    expect(githubSkillContent).toContain("## Mini-ADR Format");
  });

  it("githubTemplate_containsSubagentPrompt", () => {
    expect(githubSkillContent).toContain("## Subagent Prompt");
  });

  it("githubTemplate_usesGithubPaths", () => {
    expect(githubSkillContent).toMatch(/\.github\/skills\//);
  });
});
