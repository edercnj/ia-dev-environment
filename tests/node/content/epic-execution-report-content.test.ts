import { describe, it, expect, beforeAll } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";

const TEMPLATE_PATH = path.resolve(
  __dirname,
  "../../..",
  "resources/templates/_TEMPLATE-EPIC-EXECUTION-REPORT.md",
);

let templateContent = "";

beforeAll(() => {
  if (fs.existsSync(TEMPLATE_PATH)) {
    templateContent = fs.readFileSync(TEMPLATE_PATH, "utf-8");
  }
});

const MANDATORY_SECTIONS = [
  "## Sumario Executivo",
  "## Timeline de Execucao",
  "## Status Final por Story",
  "## Findings Consolidados",
  "## Coverage Delta",
  "## Commits e SHAs",
  "## Issues Nao Resolvidos",
  "## PR Link",
] as const;

const REQUIRED_PLACEHOLDERS = [
  "{{EPIC_ID}}",
  "{{BRANCH}}",
  "{{STARTED_AT}}",
  "{{FINISHED_AT}}",
  "{{STORIES_COMPLETED}}",
  "{{STORIES_FAILED}}",
  "{{STORIES_BLOCKED}}",
  "{{STORIES_TOTAL}}",
  "{{COMPLETION_PERCENTAGE}}",
  "{{PHASE_TIMELINE_TABLE}}",
  "{{STORY_STATUS_TABLE}}",
  "{{FINDINGS_SUMMARY}}",
  "{{COVERAGE_BEFORE}}",
  "{{COVERAGE_AFTER}}",
  "{{COVERAGE_DELTA}}",
  "{{COMMIT_LOG}}",
  "{{UNRESOLVED_ISSUES}}",
  "{{PR_LINK}}",
] as const;

// ---------------------------------------------------------------------------
// Template — Existence and Naming (CV-4)
// ---------------------------------------------------------------------------

describe("Epic execution report template — existence and naming", () => {
  it("templateFile_exists_fileIsPresent", () => {
    expect(fs.existsSync(TEMPLATE_PATH)).toBe(true);
  });

  it("templateFile_namingConvention_startsWithTemplatePrefix", () => {
    const filename = path.basename(TEMPLATE_PATH);
    expect(filename).toMatch(/^_TEMPLATE-/);
  });

  it("templateFile_namingConvention_endsWithMdExtension", () => {
    const filename = path.basename(TEMPLATE_PATH);
    expect(filename).toMatch(/\.md$/);
  });

  it("templateFile_content_isNotEmpty", () => {
    expect(templateContent.length).toBeGreaterThan(0);
  });
});

// ---------------------------------------------------------------------------
// Template — Mandatory Sections (CV-2)
// ---------------------------------------------------------------------------

describe("Epic execution report template — mandatory sections", () => {
  it.each(
    MANDATORY_SECTIONS.map((section) => [section]),
  )(
    "templateContent_mandatorySection_contains_%s",
    (section) => {
      expect(templateContent).toContain(section);
    },
  );
});

// ---------------------------------------------------------------------------
// Template — Required Placeholders (CV-3)
// ---------------------------------------------------------------------------

describe("Epic execution report template — required placeholders", () => {
  it.each(
    REQUIRED_PLACEHOLDERS.map((placeholder) => [placeholder]),
  )(
    "templateContent_placeholder_contains_%s",
    (placeholder) => {
      expect(templateContent).toContain(placeholder);
    },
  );
});

// ---------------------------------------------------------------------------
// Template — Heading Hierarchy (CV-1)
// ---------------------------------------------------------------------------

describe("Epic execution report template — heading hierarchy", () => {
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

  it("templateStructure_noHeadingLevelSkip_hierarchyIsValid", () => {
    const lines = templateContent.split("\n");
    const headingLines = lines.filter((line) => line.match(/^#{1,6}\s/));
    let maxLevel = 0;
    for (const line of headingLines) {
      const match = line.match(/^(#{1,6})\s/);
      if (!match) continue;
      const level = match[1]!.length;
      if (maxLevel === 0) {
        maxLevel = level;
        continue;
      }
      expect(level).toBeLessThanOrEqual(maxLevel + 1);
      if (level > maxLevel) {
        maxLevel = level;
      }
    }
  });
});
