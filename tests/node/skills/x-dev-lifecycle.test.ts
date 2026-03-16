import { describe, it, expect, beforeAll } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";

const SKILL_PATH = path.resolve(
  __dirname,
  "../../../resources/skills-templates/core/x-dev-lifecycle/SKILL.md",
);
const GITHUB_SKILL_PATH = path.resolve(
  __dirname,
  "../../../resources/github-skills-templates/dev/x-dev-lifecycle.md",
);

let claudeContent: string;
let githubContent: string;

beforeAll(() => {
  claudeContent = fs.readFileSync(SKILL_PATH, "utf-8");
  githubContent = fs.readFileSync(GITHUB_SKILL_PATH, "utf-8");
});

function extractSection(
  content: string,
  heading: string,
): string {
  const parts = content.split(`## ${heading}`);
  if (parts.length < 2) return "";
  return parts[1]!.split(/\n## (?!#)/)[0]!;
}

function extractFrontmatter(content: string): string {
  const match = content.match(/^---\n([\s\S]*?)\n---/);
  return match ? match[1]! : "";
}

// ─── UT-1: Degenerate — File Existence ──────────────────────

describe("x-dev-lifecycle — Degenerate", () => {
  it("fileExists_claudeTemplate_returnsTrue", () => {
    expect(fs.existsSync(SKILL_PATH)).toBe(true);
  });

  it("fileExists_githubTemplate_returnsTrue", () => {
    expect(fs.existsSync(GITHUB_SKILL_PATH)).toBe(true);
  });

  it("fileContent_claudeTemplate_isNonEmpty", () => {
    expect(claudeContent.length).toBeGreaterThan(0);
  });

  it("fileContent_githubTemplate_isNonEmpty", () => {
    expect(githubContent.length).toBeGreaterThan(0);
  });
});

// ─── UT-2: Phase 1 — Decision Tree ─────────────────────────

describe("x-dev-lifecycle — Phase 1 Decision Tree", () => {
  it("phase1_containsDecisionTreeSection_returnsTrue", () => {
    const phase1 = extractSection(
      claudeContent,
      "Phase 1",
    );
    expect(phase1).toMatch(
      /decision tree|Condition.*Plan Level/i,
    );
  });

  it("phase1_decisionTree_containsFullPlanOutcome", () => {
    const phase1 = extractSection(
      claudeContent,
      "Phase 1",
    );
    expect(phase1).toContain("Full");
  });

  it("phase1_decisionTree_containsSimplifiedPlanOutcome", () => {
    const phase1 = extractSection(
      claudeContent,
      "Phase 1",
    );
    expect(phase1).toContain("Simplified");
  });

  it("phase1_decisionTree_containsSkipOutcome", () => {
    const phase1 = extractSection(
      claudeContent,
      "Phase 1",
    );
    expect(phase1).toContain("Skip");
  });

  it("phase1_decisionTreeTable_hasAtLeast3DataRows", () => {
    const phase1 = extractSection(
      claudeContent,
      "Phase 1",
    );
    const tableRows = phase1
      .split("\n")
      .filter(
        (line) =>
          line.startsWith("|") &&
          !line.includes("---") &&
          !line.includes("Condition"),
      );
    expect(tableRows.length).toBeGreaterThanOrEqual(3);
  });

  it("phase1_fullPlanCondition_listsNewService", () => {
    const phase1 = extractSection(
      claudeContent,
      "Phase 1",
    );
    expect(phase1).toMatch(/new service/i);
  });

  it("phase1_skipCondition_listsBugFix", () => {
    const phase1 = extractSection(
      claudeContent,
      "Phase 1",
    );
    expect(phase1).toMatch(/bug fix/i);
  });
});

// ─── UT-2B: Phase 1 — Existing Plan Pre-check ──────────────

describe("x-dev-lifecycle — Phase 1 Existing Plan Pre-check", () => {
  it("phase1_containsSkipStep1AIfPlanExists", () => {
    const phase1 = extractSection(
      claudeContent,
      "Phase 1",
    );
    expect(phase1).toMatch(
      /skip Step 1A.*proceed.*Step 1B/i,
    );
  });

  it("dualCopy_existingPlanPreCheck_existsInBothTemplates", () => {
    const claudePhase1 = extractSection(
      claudeContent,
      "Phase 1",
    );
    const githubPhase1 = extractSection(
      githubContent,
      "Phase 1",
    );
    expect(claudePhase1).toMatch(
      /skip Step 1A/i,
    );
    expect(githubPhase1).toMatch(
      /skip Step 1A/i,
    );
  });
});

// ─── UT-3: Phase 1 — Skill Invocation ──────────────────────

describe("x-dev-lifecycle — Phase 1 Skill Invocation", () => {
  it("phase1_containsArchitecturePlanSkillReference", () => {
    const phase1 = extractSection(
      claudeContent,
      "Phase 1",
    );
    expect(phase1).toContain("x-dev-architecture-plan");
  });

  it("phase1_containsSkillInvocationSyntax", () => {
    const phase1 = extractSection(
      claudeContent,
      "Phase 1",
    );
    expect(phase1).toMatch(
      /\/x-dev-architecture-plan/,
    );
  });

  it("phase1_containsArchitecturePlanOutputPath", () => {
    const phase1 = extractSection(
      claudeContent,
      "Phase 1",
    );
    expect(phase1).toMatch(
      /architecture-story-XXXX-YYYY\.md/,
    );
  });
});

// ─── UT-4: Phase 1 — Fallback Block ────────────────────────

describe("x-dev-lifecycle — Phase 1 Fallback", () => {
  it("phase1_containsFallbackBlock", () => {
    const phase1 = extractSection(
      claudeContent,
      "Phase 1",
    );
    expect(phase1).toMatch(/fallback|not available/i);
  });

  it("phase1_fallback_containsInlineSubagentText", () => {
    const phase1 = extractSection(
      claudeContent,
      "Phase 1",
    );
    expect(phase1).toMatch(/Senior Architect/);
  });

  it("phase1_fallback_containsImplementationPlanOutput", () => {
    const phase1 = extractSection(
      claudeContent,
      "Phase 1",
    );
    expect(phase1).toMatch(
      /plan-story-XXXX-YYYY\.md/,
    );
  });
});

// ─── UT-5: Phase 1 — Soft Dependency ───────────────────────

describe("x-dev-lifecycle — Phase 1 Soft Dependency", () => {
  it("phase1_containsWarningOnFailure", () => {
    const phase1 = extractSection(
      claudeContent,
      "Phase 1",
    );
    expect(phase1).toMatch(/WARNING/);
  });

  it("phase1_containsNonBlockingContinuation", () => {
    const phase1 = extractSection(
      claudeContent,
      "Phase 1",
    );
    expect(phase1).toMatch(
      /continu|proceed/i,
    );
  });
});

// ─── UT-6: Phase 2 — Architecture Plan Context ─────────────

describe("x-dev-lifecycle — Phase 2 Architecture Plan Context", () => {
  it("phase2_containsArchitecturePlanRead", () => {
    const phase2 = extractSection(
      claudeContent,
      "Phase 2",
    );
    expect(phase2).toMatch(
      /architecture.plan|architecture-story/i,
    );
  });

  it("phase2_architecturePlanPath_matchesPhase1Output", () => {
    const phase2 = extractSection(
      claudeContent,
      "Phase 2",
    );
    expect(phase2).toContain(
      "architecture-story-XXXX-YYYY.md",
    );
  });

  it("phase2_architecturePlanRead_isConditional", () => {
    const phase2 = extractSection(
      claudeContent,
      "Phase 2",
    );
    expect(phase2).toMatch(/if.*generated in Phase 1|if it does not exist/i);
  });
});

// ─── UT-7: Phase 4 — Review Context ────────────────────────

describe("x-dev-lifecycle — Phase 4 Review Context", () => {
  it("phase4_containsArchitecturePlanReference", () => {
    const phase4 = extractSection(
      claudeContent,
      "Phase 4",
    );
    expect(phase4).toMatch(
      /architecture plan/i,
    );
  });

  it("phase4_containsConformanceValidation", () => {
    const phase4 = extractSection(
      claudeContent,
      "Phase 4",
    );
    expect(phase4).toMatch(
      /conform|architecture.*decision/i,
    );
  });
});

// ─── UT-8: Integration Notes ────────────────────────────────

describe("x-dev-lifecycle — Integration Notes", () => {
  it("integrationNotes_containsArchitecturePlanSkill", () => {
    const notes = extractSection(
      claudeContent,
      "Integration Notes",
    );
    expect(notes).toContain(
      "x-dev-architecture-plan",
    );
  });
});

// ─── UT-9: Dual Copy Consistency (RULE-001) ─────────────────

describe("x-dev-lifecycle — Dual Copy Consistency (RULE-001)", () => {
  it("dualCopy_decisionTree_existsInBothTemplates", () => {
    const claudePhase1 = extractSection(
      claudeContent,
      "Phase 1",
    );
    const githubPhase1 = extractSection(
      githubContent,
      "Phase 1",
    );
    expect(claudePhase1).toMatch(
      /Condition.*Plan Level/,
    );
    expect(githubPhase1).toMatch(
      /Condition.*Plan Level/,
    );
  });

  it("dualCopy_skillInvocation_existsInBothTemplates", () => {
    const claudePhase1 = extractSection(
      claudeContent,
      "Phase 1",
    );
    const githubPhase1 = extractSection(
      githubContent,
      "Phase 1",
    );
    expect(claudePhase1).toContain(
      "/x-dev-architecture-plan",
    );
    expect(githubPhase1).toContain(
      "/x-dev-architecture-plan",
    );
  });

  it("dualCopy_fallbackBlock_existsInBothTemplates", () => {
    const claudePhase1 = extractSection(
      claudeContent,
      "Phase 1",
    );
    const githubPhase1 = extractSection(
      githubContent,
      "Phase 1",
    );
    expect(claudePhase1).toMatch(/fallback/i);
    expect(githubPhase1).toMatch(/fallback/i);
  });

  it("dualCopy_phase2Context_existsInBothTemplates", () => {
    const claudePhase2 = extractSection(
      claudeContent,
      "Phase 2",
    );
    const githubPhase2 = extractSection(
      githubContent,
      "Phase 2",
    );
    expect(claudePhase2).toContain(
      "architecture-story-XXXX-YYYY.md",
    );
    expect(githubPhase2).toContain(
      "architecture-story-XXXX-YYYY.md",
    );
  });

  it("dualCopy_phase4Context_existsInBothTemplates", () => {
    const claudePhase4 = extractSection(
      claudeContent,
      "Phase 4",
    );
    const githubPhase4 = extractSection(
      githubContent,
      "Phase 4",
    );
    expect(claudePhase4).toMatch(
      /architecture plan/i,
    );
    expect(githubPhase4).toMatch(
      /architecture plan/i,
    );
  });

  it("dualCopy_integrationNotes_existsInBothTemplates", () => {
    const claudeNotes = extractSection(
      claudeContent,
      "Integration Notes",
    );
    const githubNotes = extractSection(
      githubContent,
      "Integration Notes",
    );
    expect(claudeNotes).toContain(
      "x-dev-architecture-plan",
    );
    expect(githubNotes).toContain(
      "x-dev-architecture-plan",
    );
  });

  it("dualCopy_decisionTreeOutcomes_identicalInBoth", () => {
    const outcomes = ["Full", "Simplified", "Skip"];
    const claudePhase1 = extractSection(
      claudeContent,
      "Phase 1",
    );
    const githubPhase1 = extractSection(
      githubContent,
      "Phase 1",
    );
    for (const outcome of outcomes) {
      expect(claudePhase1).toContain(outcome);
      expect(githubPhase1).toContain(outcome);
    }
  });

  it("dualCopy_architecturePlanOutputPath_identicalInBoth", () => {
    const claudePhase1 = extractSection(
      claudeContent,
      "Phase 1",
    );
    const githubPhase1 = extractSection(
      githubContent,
      "Phase 1",
    );
    expect(claudePhase1).toContain(
      "architecture-story-XXXX-YYYY.md",
    );
    expect(githubPhase1).toContain(
      "architecture-story-XXXX-YYYY.md",
    );
  });
});

// ─── UT-10: GitHub Template — Path Convention ───────────────

describe("x-dev-lifecycle — Path Convention", () => {
  it("githubTemplate_phase1Fallback_usesGithubPaths", () => {
    const githubPhase1 = extractSection(
      githubContent,
      "Phase 1",
    );
    expect(githubPhase1).toMatch(
      /\.github\/skills\//,
    );
  });

  it("claudeTemplate_phase1Fallback_usesClaudePaths", () => {
    const claudePhase1 = extractSection(
      claudeContent,
      "Phase 1",
    );
    expect(claudePhase1).toMatch(
      /skills\/architecture/,
    );
    expect(claudePhase1).not.toMatch(
      /\.github\/skills\//,
    );
  });
});

// ─── UT-11: Exception Paths ────────────────────────────────

describe("x-dev-lifecycle — Exception Paths", () => {
  it("extractSection_missingPhaseHeading_returnsEmptyString", () => {
    const result = extractSection(
      claudeContent,
      "Nonexistent Phase",
    );
    expect(result).toBe("");
  });

  it("extractSection_emptyContent_returnsEmptyString", () => {
    const result = extractSection(
      "",
      "Any Heading",
    );
    expect(result).toBe("");
  });
});

// ─── AT-1: Architecture Plan Integration Acceptance Test ────

describe("x-dev-lifecycle — Architecture Plan Integration (AT-1)", () => {
  it("phase1Integration_containsDecisionTreeAndSkillInvocation_returnsTrue", () => {
    const phase1 = extractSection(
      claudeContent,
      "Phase 1",
    );
    // Decision tree
    expect(phase1).toMatch(
      /Condition.*Plan Level/,
    );
    // Skill invocation
    expect(phase1).toContain(
      "/x-dev-architecture-plan",
    );
    // Fallback
    expect(phase1).toMatch(/fallback/i);
    // Output path
    expect(phase1).toContain(
      "architecture-story-XXXX-YYYY.md",
    );
    // Phase 2 propagation
    const phase2 = extractSection(
      claudeContent,
      "Phase 2",
    );
    expect(phase2).toContain(
      "architecture-story-XXXX-YYYY.md",
    );
    // Phase 4 propagation
    const phase4 = extractSection(
      claudeContent,
      "Phase 4",
    );
    expect(phase4).toMatch(
      /architecture plan/i,
    );
  });
});
