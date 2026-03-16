import { describe, it, expect, beforeAll } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";
import {
  extractSection,
  extractFrontmatter,
} from "../../helpers/content-helpers.js";

const SKILL_PATH = path.resolve(
  __dirname,
  "../../../resources/skills-templates/core/x-dev-arch-update/SKILL.md",
);
const GITHUB_SKILL_PATH = path.resolve(
  __dirname,
  "../../../resources/github-skills-templates/dev/x-dev-arch-update.md",
);

let skillContent: string;
let githubSkillContent: string;

beforeAll(() => {
  skillContent = fs.readFileSync(SKILL_PATH, "utf-8");
  githubSkillContent = fs.readFileSync(GITHUB_SKILL_PATH, "utf-8");
});

// ---------------------------------------------------------------------------
// 1. Degenerate
// ---------------------------------------------------------------------------

describe("x-dev-arch-update SKILL.md — Degenerate", () => {
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

// ---------------------------------------------------------------------------
// 2. Frontmatter Fields
// ---------------------------------------------------------------------------

describe("x-dev-arch-update SKILL.md — Frontmatter Fields", () => {
  it("frontmatter_name_equalsXDevArchUpdate", () => {
    expect(skillContent).toMatch(/name:\s+x-dev-arch-update/);
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

  it("frontmatter_argumentHint_referencesStoryId", () => {
    const fm = extractFrontmatter(skillContent);
    expect(fm).toMatch(/argument-hint:.*STORY-ID/i);
  });

  it("frontmatter_doesNotContain_userInvocableFalse", () => {
    expect(skillContent).not.toMatch(/user-invocable:\s*false/);
  });
});

// ---------------------------------------------------------------------------
// 3. Global Output Policy
// ---------------------------------------------------------------------------

describe("x-dev-arch-update SKILL.md — Global Output Policy", () => {
  it("globalOutputPolicy_sectionExists_returnsTrue", () => {
    expect(skillContent).toContain("## Global Output Policy");
  });
});

// ---------------------------------------------------------------------------
// 4. Incremental Update Rules
// ---------------------------------------------------------------------------

describe("x-dev-arch-update SKILL.md — Incremental Update Rules", () => {
  it("incrementalUpdateRules_sectionExists_returnsTrue", () => {
    expect(skillContent).toContain("## Incremental Update Rules");
  });

  it("incrementalUpdateRules_containsNeverRemove", () => {
    const section = extractSection(
      skillContent,
      "Incremental Update Rules",
    );
    expect(section).toMatch(/NEVER remove/);
  });

  it("sectionUpdateProtocol_table_hasAtLeast10DataRows", () => {
    const section = extractSection(
      skillContent,
      "Incremental Update Rules",
    );
    const tableRows = section
      .split("\n")
      .filter(
        (line) =>
          line.startsWith("|") &&
          !line.includes("---") &&
          !line.includes("Section") &&
          !line.includes("# ") &&
          line.trim().length > 0,
      );
    expect(tableRows.length).toBeGreaterThanOrEqual(10);
  });
});

// ---------------------------------------------------------------------------
// 5. Input Documents
// ---------------------------------------------------------------------------

describe("x-dev-arch-update SKILL.md — Input Documents", () => {
  it("inputDocuments_referencesArchitecturePlanPattern", () => {
    expect(skillContent).toMatch(/architecture-story-XXXX-YYYY/);
  });

  it("inputDocuments_referencesServiceArchDoc", () => {
    expect(skillContent).toContain(
      "docs/architecture/service-architecture.md",
    );
  });

  it("inputDocuments_referencesTemplate", () => {
    expect(skillContent).toContain(
      "_TEMPLATE-SERVICE-ARCHITECTURE.md",
    );
  });
});

// ---------------------------------------------------------------------------
// 6. Document Creation
// ---------------------------------------------------------------------------

describe("x-dev-arch-update SKILL.md — Document Creation", () => {
  it("documentCreation_referencesCreationFromScratchWithTemplate", () => {
    expect(skillContent).toMatch(/does NOT exist/i);
    expect(skillContent).toContain(
      "_TEMPLATE-SERVICE-ARCHITECTURE.md",
    );
  });
});

// ---------------------------------------------------------------------------
// 7. Subagent Prompt
// ---------------------------------------------------------------------------

describe("x-dev-arch-update SKILL.md — Subagent Prompt", () => {
  it("subagentPrompt_sectionExists_returnsTrue", () => {
    expect(skillContent).toContain("## Subagent Prompt");
  });

  it("subagentPrompt_mentionsDocumentationEngineerRole", () => {
    const section = extractSection(skillContent, "Subagent Prompt");
    expect(section).toMatch(/Documentation Engineer/);
  });

  it("subagentPrompt_containsProjectNamePlaceholder", () => {
    const section = extractSection(skillContent, "Subagent Prompt");
    expect(section).toContain("{{PROJECT_NAME}}");
  });

  it("subagentPrompt_referencesRule008", () => {
    const section = extractSection(skillContent, "Subagent Prompt");
    expect(section).toContain("RULE-008");
  });

  it("subagentPrompt_containsNeverRemove", () => {
    const section = extractSection(skillContent, "Subagent Prompt");
    expect(section).toMatch(/NEVER remove/);
  });

  it("subagentPrompt_savesToServiceArchitecturePath", () => {
    const section = extractSection(skillContent, "Subagent Prompt");
    expect(section).toContain(
      "docs/architecture/service-architecture.md",
    );
  });
});

// ---------------------------------------------------------------------------
// 8. Change History
// ---------------------------------------------------------------------------

describe("x-dev-arch-update SKILL.md — Change History", () => {
  it("changeHistory_documentsEntryFormat", () => {
    expect(skillContent).toMatch(/YYYY-MM-DD/);
    expect(skillContent).toMatch(/story-XXXX-YYYY/);
  });
});

// ---------------------------------------------------------------------------
// 9. GitHub Template
// ---------------------------------------------------------------------------

describe("x-dev-arch-update — GitHub template", () => {
  it("githubTemplate_fileExists_returnsTrue", () => {
    expect(fs.existsSync(GITHUB_SKILL_PATH)).toBe(true);
  });

  it("githubTemplate_frontmatter_nameMatches", () => {
    expect(githubSkillContent).toMatch(
      /name:\s+x-dev-arch-update/,
    );
  });

  it("githubTemplate_usesGithubPaths_notClaudePaths", () => {
    expect(githubSkillContent).toMatch(/\.github\/skills\//);
  });

  it("githubTemplate_containsProjectNamePlaceholder", () => {
    expect(githubSkillContent).toContain("{{PROJECT_NAME}}");
  });
});

// ---------------------------------------------------------------------------
// 10. Dual Copy RULE-001
// ---------------------------------------------------------------------------

describe("x-dev-arch-update — Dual Copy Consistency (RULE-001)", () => {
  const SHARED_SECTIONS = [
    "Incremental Update Rules",
    "Input Documents",
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

  it("dualCopy_neverRemoveRule_sameInBothTemplates", () => {
    expect(skillContent).toMatch(/NEVER remove/);
    expect(githubSkillContent).toMatch(/NEVER remove/);
  });

  it("dualCopy_skipLogic_sameInBothTemplates", () => {
    expect(skillContent).toMatch(
      /[Nn]o architecture plan found/,
    );
    expect(githubSkillContent).toMatch(
      /[Nn]o architecture plan found/,
    );
  });
});

// ---------------------------------------------------------------------------
// 11. Exception Paths
// ---------------------------------------------------------------------------

describe("x-dev-arch-update — Exception Paths", () => {
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
