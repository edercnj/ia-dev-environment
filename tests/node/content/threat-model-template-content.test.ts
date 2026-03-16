import { describe, it, expect } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";

// --- Source paths ---
const TEMPLATE_PATH = path.resolve(
  __dirname,
  "../../..",
  "resources/templates/_TEMPLATE-THREAT-MODEL.md",
);
const CLAUDE_XREVIEW_PATH = path.resolve(
  __dirname,
  "../../..",
  "resources/skills-templates/core/x-review/SKILL.md",
);
const GITHUB_XREVIEW_PATH = path.resolve(
  __dirname,
  "../../..",
  "resources/github-skills-templates/review/x-review.md",
);
const CLAUDE_LIFECYCLE_PATH = path.resolve(
  __dirname,
  "../../..",
  "resources/skills-templates/core/x-dev-lifecycle/SKILL.md",
);
const GITHUB_LIFECYCLE_PATH = path.resolve(
  __dirname,
  "../../..",
  "resources/github-skills-templates/dev/x-dev-lifecycle.md",
);

// --- Read content once ---
const templateContent = fs.readFileSync(TEMPLATE_PATH, "utf-8");
const claudeXReview = fs.readFileSync(CLAUDE_XREVIEW_PATH, "utf-8");
const githubXReview = fs.readFileSync(GITHUB_XREVIEW_PATH, "utf-8");
const claudeLifecycle = fs.readFileSync(CLAUDE_LIFECYCLE_PATH, "utf-8");
const githubLifecycle = fs.readFileSync(GITHUB_LIFECYCLE_PATH, "utf-8");

const STRIDE_CATEGORIES = [
  "Spoofing",
  "Tampering",
  "Repudiation",
  "Information Disclosure",
  "Denial of Service",
  "Elevation of Privilege",
];

const SEVERITY_VALUES = ["Critical", "High", "Medium", "Low"];

const STATUS_VALUES = ["Open", "Mitigated", "Accepted", "Under Review"];

// ---------------------------------------------------------------------------
// Template — Existence and Structure (UT-1, UT-2, UT-3)
// ---------------------------------------------------------------------------

describe("Threat model template — existence and structure", () => {
  it("templateFile_exists_fileIsPresent", () => {
    expect(fs.existsSync(TEMPLATE_PATH)).toBe(true);
  });

  it("templateContent_h1Heading_containsServiceNamePlaceholder", () => {
    expect(templateContent).toContain("{{SERVICE_NAME}}");
    expect(templateContent).toMatch(/^# Threat Model.*\{\{SERVICE_NAME\}\}/m);
  });

  it("templateContent_trustBoundaries_containsMermaidDiagramWithZones", () => {
    expect(templateContent).toContain("## Trust Boundaries");
    expect(templateContent).toContain("```mermaid");
    expect(templateContent).toContain("subgraph External");
    expect(templateContent).toContain("subgraph Internal");
  });
});

// ---------------------------------------------------------------------------
// Template — STRIDE Categories (UT-4, UT-5)
// ---------------------------------------------------------------------------

describe("Threat model template — STRIDE categories", () => {
  it.each(
    STRIDE_CATEGORIES.map((cat) => [cat]),
  )(
    "templateContent_strideAnalysis_containsCategory_%s",
    (category) => {
      expect(templateContent).toContain(`### ${category}`);
    },
  );

  it("templateContent_riskTable_containsRequiredColumns", () => {
    expect(templateContent).toContain(
      "| Threat | Severity | Mitigation | Status | Story Ref |",
    );
  });
});

// ---------------------------------------------------------------------------
// Template — Supplementary Sections (UT-6, UT-7)
// ---------------------------------------------------------------------------

describe("Threat model template — supplementary sections", () => {
  it("templateContent_riskSummary_containsSeverityCountsTable", () => {
    expect(templateContent).toContain("## Risk Summary");
    for (const severity of SEVERITY_VALUES) {
      expect(templateContent).toContain(severity);
    }
  });

  it("templateContent_changeHistory_containsTableWithRequiredColumns", () => {
    expect(templateContent).toContain("## Change History");
    expect(templateContent).toContain("Date");
    expect(templateContent).toContain("Story");
    expect(templateContent).toContain("Threats Added/Updated");
  });
});

// ---------------------------------------------------------------------------
// Template — Enum Values (UT-8, UT-9)
// ---------------------------------------------------------------------------

describe("Threat model template — enum values", () => {
  it.each(
    SEVERITY_VALUES.map((val) => [val]),
  )(
    "templateContent_severityEnum_containsValue_%s",
    (value) => {
      expect(templateContent).toContain(value);
    },
  );

  it.each(
    STATUS_VALUES.map((val) => [val]),
  )(
    "templateContent_statusEnum_containsValue_%s",
    (value) => {
      expect(templateContent).toContain(value);
    },
  );
});

// ---------------------------------------------------------------------------
// Template — Section Ordering (UT-15, UT-16, UT-17, UT-18, UT-19)
// ---------------------------------------------------------------------------

describe("Threat model template — section ordering", () => {
  it("templateStructure_firstHeading_isH1", () => {
    const lines = templateContent.split("\n");
    const firstHeading = lines.find((line) => line.match(/^#{1,6}\s/));
    expect(firstHeading).toBeDefined();
    expect(firstHeading).toMatch(/^# /);
  });

  it("templateStructure_allHeadings_useValidMarkdownSyntax", () => {
    const lines = templateContent.split("\n");
    const headingLines = lines.filter((line) => line.match(/^#{1,6}\s/));
    expect(headingLines.length).toBeGreaterThan(0);
    for (const line of headingLines) {
      expect(line).toMatch(/^#{1,6}\s+\S/);
    }
  });

  it("templateStructure_strideAnalysis_appearsAfterTrustBoundaries", () => {
    const trustIdx = templateContent.indexOf("## Trust Boundaries");
    const strideIdx = templateContent.indexOf("## STRIDE Analysis");
    expect(trustIdx).toBeGreaterThan(-1);
    expect(strideIdx).toBeGreaterThan(-1);
    expect(strideIdx).toBeGreaterThan(trustIdx);
  });

  it("templateStructure_riskSummary_appearsAfterStrideAnalysis", () => {
    const strideIdx = templateContent.indexOf("### Elevation of Privilege");
    const summaryIdx = templateContent.indexOf("## Risk Summary");
    expect(strideIdx).toBeGreaterThan(-1);
    expect(summaryIdx).toBeGreaterThan(-1);
    expect(summaryIdx).toBeGreaterThan(strideIdx);
  });

  it("templateStructure_changeHistory_appearsAfterRiskSummary", () => {
    const summaryIdx = templateContent.indexOf("## Risk Summary");
    const historyIdx = templateContent.indexOf("## Change History");
    expect(summaryIdx).toBeGreaterThan(-1);
    expect(historyIdx).toBeGreaterThan(-1);
    expect(historyIdx).toBeGreaterThan(summaryIdx);
  });
});

// ---------------------------------------------------------------------------
// x-review Claude source — threat model content (UT-10, UT-11, UT-25, UT-29, UT-30)
// ---------------------------------------------------------------------------

describe("x-review Claude source — threat model content", () => {
  it("xReviewClaude_containsThreatModelUpdateInstructions", () => {
    expect(claudeXReview.toLowerCase()).toContain("threat model");
  });

  it("xReviewClaude_containsSeverityBasedAutoAddRules", () => {
    expect(claudeXReview).toContain("Critical");
    expect(claudeXReview).toContain("High");
    expect(claudeXReview).toContain("Open");
    expect(claudeXReview).toContain("Medium");
    expect(claudeXReview).toContain("Under Review");
  });

  it("xReviewClaude_preservesExistingReviewPhases", () => {
    expect(claudeXReview).toContain("DETECT");
    expect(claudeXReview).toContain("REVIEW");
    expect(claudeXReview).toContain("CONSOLIDATE");
    expect(claudeXReview).toContain("STORY");
  });

  it("xReviewClaude_specifiesIncrementalUpdateBehavior", () => {
    const lowerContent = claudeXReview.toLowerCase();
    const hasIncremental = lowerContent.includes("incremental")
      || lowerContent.includes("preserve")
      || lowerContent.includes("append");
    expect(hasIncremental).toBe(true);
  });

  it("xReviewClaude_specifiesThreatModelOutputPath", () => {
    expect(claudeXReview).toContain("docs/security/threat-model.md");
  });
});

// ---------------------------------------------------------------------------
// x-review GitHub source — threat model content (UT-13, UT-26)
// ---------------------------------------------------------------------------

describe("x-review GitHub source — threat model content", () => {
  it("xReviewGithub_containsThreatModelUpdateInstructions", () => {
    expect(githubXReview.toLowerCase()).toContain("threat model");
  });

  it("xReviewGithub_preservesExistingReviewPhases", () => {
    expect(githubXReview).toContain("DETECT");
    expect(githubXReview).toContain("REVIEW");
    expect(githubXReview).toContain("CONSOLIDATE");
    expect(githubXReview).toContain("STORY");
  });
});

// ---------------------------------------------------------------------------
// x-dev-lifecycle Claude source — threat model reference (UT-12, UT-27)
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle Claude source — threat model reference", () => {
  it("xDevLifecycleClaude_referencesThreatModel", () => {
    expect(claudeLifecycle.toLowerCase()).toContain("threat model");
  });

  it("xDevLifecycleClaude_preserves8PhaseStructure", () => {
    expect(claudeLifecycle).toMatch(/8 phases.*0-7/i);
    expect(claudeLifecycle).toContain("NEVER stop before Phase 7");
  });
});

// ---------------------------------------------------------------------------
// x-dev-lifecycle GitHub source — threat model reference (UT-14, UT-28)
// ---------------------------------------------------------------------------

describe("x-dev-lifecycle GitHub source — threat model reference", () => {
  it("xDevLifecycleGithub_referencesThreatModel", () => {
    expect(githubLifecycle.toLowerCase()).toContain("threat model");
  });

  it("xDevLifecycleGithub_preserves8PhaseStructure", () => {
    expect(githubLifecycle).toMatch(/8 phases.*0-7/i);
    expect(githubLifecycle).toContain("NEVER stop before Phase 7");
  });
});

// ---------------------------------------------------------------------------
// Dual copy consistency — RULE-001 (UT-20, UT-21, UT-22, UT-23, UT-24)
// ---------------------------------------------------------------------------

describe("Dual copy consistency (RULE-001)", () => {
  it("dualCopy_xReview_bothContainThreatModelInstructions", () => {
    expect(claudeXReview.toLowerCase()).toContain("threat model");
    expect(githubXReview.toLowerCase()).toContain("threat model");
  });

  it("dualCopy_xReview_bothContainSeverityAutoAddRules", () => {
    for (const keyword of ["Critical", "High", "Open", "Medium", "Under Review"]) {
      expect(claudeXReview).toContain(keyword);
      expect(githubXReview).toContain(keyword);
    }
  });

  it("dualCopy_xDevLifecycle_bothReferenceThreatModel", () => {
    expect(claudeLifecycle.toLowerCase()).toContain("threat model");
    expect(githubLifecycle.toLowerCase()).toContain("threat model");
  });

  it("dualCopy_xReview_bothReferenceTemplateFile", () => {
    expect(claudeXReview).toContain("_TEMPLATE-THREAT-MODEL");
    expect(githubXReview).toContain("_TEMPLATE-THREAT-MODEL");
  });

  it("dualCopy_xReview_bothContainStrideReference", () => {
    expect(claudeXReview).toContain("STRIDE");
    expect(githubXReview).toContain("STRIDE");
  });
});
