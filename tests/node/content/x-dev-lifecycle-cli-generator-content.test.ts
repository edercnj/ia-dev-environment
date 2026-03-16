import { describe, it, expect } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";

const CLAUDE_SOURCE = path.resolve(
  __dirname,
  "../../..",
  "resources/skills-templates/core/x-dev-lifecycle/SKILL.md",
);
const GITHUB_SOURCE = path.resolve(
  __dirname,
  "../../..",
  "resources/github-skills-templates/dev/x-dev-lifecycle.md",
);

const claudeContent = fs.readFileSync(CLAUDE_SOURCE, "utf-8");
const githubContent = fs.readFileSync(GITHUB_SOURCE, "utf-8");

// ---------------------------------------------------------------------------
// Claude source — Documentation Phase exists
// ---------------------------------------------------------------------------

describe("Claude source — Documentation Phase exists", () => {
  it("claudeSource_documentationPhase_hasPhase3Heading", () => {
    expect(claudeContent).toMatch(
      /^## Phase 3 — Documentation/m,
    );
  });

  it("claudeSource_phaseCount_shows9Phases0To8", () => {
    expect(claudeContent).toContain("9 phases (0-8)");
  });

  it("claudeSource_criticalRule_neverStopBeforePhase8", () => {
    expect(claudeContent).toContain(
      "NEVER stop before Phase 8",
    );
  });

  it("claudeSource_phaseOrder_phase3BetweenPhase2AndPhase4", () => {
    const phase2Idx = claudeContent.indexOf(
      "## Phase 2",
    );
    const phase3Idx = claudeContent.indexOf(
      "## Phase 3 — Documentation",
    );
    const phase4Idx = claudeContent.indexOf(
      "## Phase 4",
    );
    expect(phase2Idx).toBeGreaterThan(-1);
    expect(phase3Idx).toBeGreaterThan(-1);
    expect(phase4Idx).toBeGreaterThan(-1);
    expect(phase3Idx).toBeGreaterThan(phase2Idx);
    expect(phase3Idx).toBeLessThan(phase4Idx);
  });

  it("claudeSource_completeFlow_showsPhase3Documentation", () => {
    expect(claudeContent).toMatch(
      /Phase 3: Documentation/,
    );
  });
});

// ---------------------------------------------------------------------------
// Claude source — CLI generator section
// ---------------------------------------------------------------------------

describe("Claude source — CLI generator section", () => {
  it("claudeSource_cliGenerator_hasHeading", () => {
    expect(claudeContent).toContain(
      "CLI Documentation Generator",
    );
  });

  it("claudeSource_cliGenerator_hasInterfaceCondition", () => {
    expect(claudeContent).toMatch(
      /interface.*cli|interfaces.*cli/i,
    );
  });

  it("claudeSource_cliGenerator_hasOutputPath", () => {
    expect(claudeContent).toContain(
      "docs/api/cli-reference.md",
    );
  });

  it.each([
    ["CLI Reference"],
    ["Quick Start"],
    ["Global Flags"],
    ["Command:"],
    ["Subcommand:"],
    ["Exit Codes"],
  ])(
    "claudeSource_cliGenerator_outputFormat_contains_%s",
    (section) => {
      expect(claudeContent).toContain(section);
    },
  );

  it("claudeSource_cliGenerator_flagsTable_hasColumns", () => {
    expect(claudeContent).toMatch(
      /Flag.*Type.*Default.*Description/,
    );
  });

  it("claudeSource_cliGenerator_argsTable_hasColumns", () => {
    expect(claudeContent).toMatch(
      /Argument.*Type.*Required.*Description/,
    );
  });

  it("claudeSource_cliGenerator_usageLine_hasPattern", () => {
    expect(claudeContent).toMatch(
      /\$.*\{tool-name\}.*\{command\}/,
    );
  });

  it("claudeSource_cliGenerator_commandSection_requiresExample", () => {
    expect(claudeContent).toMatch(
      /example.*code block|at least 1 example/i,
    );
  });

  it.each([
    ["Commander.js", ".command()"],
    ["Click", "@click.command()"],
    ["Cobra", "cobra.Command"],
    ["Clap", "derive(Parser)"],
  ])(
    "claudeSource_cliGenerator_frameworkPattern_%s_contains_%s",
    (framework, pattern) => {
      expect(claudeContent).toContain(framework);
      expect(claudeContent).toContain(pattern);
    },
  );

  it("claudeSource_cliGenerator_quickStart_requiresAtLeast2Examples", () => {
    expect(claudeContent).toMatch(
      /at least 2.*example|2.*usage example/i,
    );
  });

  it("claudeSource_cliGenerator_exitCodes_hasCodeAndMeaning", () => {
    expect(claudeContent).toMatch(
      /Code.*Meaning/,
    );
  });

  it("claudeSource_cliGenerator_skipBehavior_skipsSilently", () => {
    expect(claudeContent).toMatch(
      /skip silently/i,
    );
  });
});

// ---------------------------------------------------------------------------
// Claude source — Phase renumbering
// ---------------------------------------------------------------------------

describe("Claude source — Phase renumbering", () => {
  it("claudeSource_phase4_isReview", () => {
    expect(claudeContent).toMatch(
      /^## Phase 4 — .*Review/m,
    );
  });

  it("claudeSource_phase5_isFixes", () => {
    expect(claudeContent).toMatch(
      /^## Phase 5 — Fixes/m,
    );
  });

  it("claudeSource_phase6_isCommitAndPR", () => {
    expect(claudeContent).toMatch(
      /^## Phase 6 — Commit & PR/m,
    );
  });

  it("claudeSource_phase7_isTechLeadReview", () => {
    expect(claudeContent).toMatch(
      /^## Phase 7 — Tech Lead Review/m,
    );
  });

  it("claudeSource_phase8_isVerification", () => {
    expect(claudeContent).toMatch(
      /^## Phase 8 — Final Verification/m,
    );
  });
});

// ---------------------------------------------------------------------------
// Claude source — Structural preservation
// ---------------------------------------------------------------------------

describe("Claude source — Structural preservation", () => {
  it("claudeSource_preservation_phase1ArchitectSubagent", () => {
    expect(claudeContent).toContain(
      "Senior Architect",
    );
  });

  it("claudeSource_preservation_phase2TDDHeading", () => {
    expect(claudeContent).toMatch(
      /^## Phase 2 — TDD Implementation/m,
    );
  });

  it("claudeSource_preservation_g1g7Fallback", () => {
    expect(claudeContent).toContain(
      "G1-G7 Fallback",
    );
  });

  it("claudeSource_preservation_integrationNotes", () => {
    expect(claudeContent).toContain(
      "## Integration Notes",
    );
  });

  it.each([
    ["PROJECT_NAME"],
    ["LANGUAGE"],
    ["COMPILE_COMMAND"],
    ["TEST_COMMAND"],
    ["COVERAGE_COMMAND"],
  ])(
    "claudeSource_preservation_placeholderToken_%s",
    (token) => {
      expect(claudeContent).toContain(`{{${token}}}`);
    },
  );
});

// ---------------------------------------------------------------------------
// GitHub source — Documentation Phase exists
// ---------------------------------------------------------------------------

describe("GitHub source — Documentation Phase exists", () => {
  it("githubSource_documentationPhase_hasPhase3Heading", () => {
    expect(githubContent).toMatch(
      /^## Phase 3 — Documentation/m,
    );
  });

  it("githubSource_phaseCount_shows9Phases0To8", () => {
    expect(githubContent).toContain("9 phases (0-8)");
  });

  it("githubSource_criticalRule_neverStopBeforePhase8", () => {
    expect(githubContent).toContain(
      "NEVER stop before Phase 8",
    );
  });

  it("githubSource_phaseOrder_phase3BetweenPhase2AndPhase4", () => {
    const phase2Idx = githubContent.indexOf(
      "## Phase 2",
    );
    const phase3Idx = githubContent.indexOf(
      "## Phase 3 — Documentation",
    );
    const phase4Idx = githubContent.indexOf(
      "## Phase 4",
    );
    expect(phase2Idx).toBeGreaterThan(-1);
    expect(phase3Idx).toBeGreaterThan(-1);
    expect(phase4Idx).toBeGreaterThan(-1);
    expect(phase3Idx).toBeGreaterThan(phase2Idx);
    expect(phase3Idx).toBeLessThan(phase4Idx);
  });

  it("githubSource_completeFlow_showsPhase3Documentation", () => {
    expect(githubContent).toMatch(
      /Phase 3: Documentation/,
    );
  });
});

// ---------------------------------------------------------------------------
// GitHub source — CLI generator section
// ---------------------------------------------------------------------------

describe("GitHub source — CLI generator section", () => {
  it("githubSource_cliGenerator_hasHeading", () => {
    expect(githubContent).toContain(
      "CLI Documentation Generator",
    );
  });

  it("githubSource_cliGenerator_hasInterfaceCondition", () => {
    expect(githubContent).toMatch(
      /interface.*cli|interfaces.*cli/i,
    );
  });

  it("githubSource_cliGenerator_hasOutputPath", () => {
    expect(githubContent).toContain(
      "docs/api/cli-reference.md",
    );
  });

  it.each([
    ["CLI Reference"],
    ["Quick Start"],
    ["Global Flags"],
    ["Command:"],
    ["Subcommand:"],
    ["Exit Codes"],
  ])(
    "githubSource_cliGenerator_outputFormat_contains_%s",
    (section) => {
      expect(githubContent).toContain(section);
    },
  );

  it("githubSource_cliGenerator_flagsTable_hasColumns", () => {
    expect(githubContent).toMatch(
      /Flag.*Type.*Default.*Description/,
    );
  });

  it("githubSource_cliGenerator_argsTable_hasColumns", () => {
    expect(githubContent).toMatch(
      /Argument.*Type.*Required.*Description/,
    );
  });

  it("githubSource_cliGenerator_usageLine_hasPattern", () => {
    expect(githubContent).toMatch(
      /\$.*\{tool-name\}.*\{command\}/,
    );
  });

  it("githubSource_cliGenerator_commandSection_requiresExample", () => {
    expect(githubContent).toMatch(
      /example.*code block|at least 1 example/i,
    );
  });

  it.each([
    ["Commander.js", ".command()"],
    ["Click", "@click.command()"],
    ["Cobra", "cobra.Command"],
    ["Clap", "derive(Parser)"],
  ])(
    "githubSource_cliGenerator_frameworkPattern_%s_contains_%s",
    (framework, pattern) => {
      expect(githubContent).toContain(framework);
      expect(githubContent).toContain(pattern);
    },
  );

  it("githubSource_cliGenerator_quickStart_requiresAtLeast2Examples", () => {
    expect(githubContent).toMatch(
      /at least 2.*example|2.*usage example/i,
    );
  });

  it("githubSource_cliGenerator_exitCodes_hasCodeAndMeaning", () => {
    expect(githubContent).toMatch(
      /Code.*Meaning/,
    );
  });

  it("githubSource_cliGenerator_skipBehavior_skipsSilently", () => {
    expect(githubContent).toMatch(
      /skip silently/i,
    );
  });
});

// ---------------------------------------------------------------------------
// GitHub source — Phase renumbering
// ---------------------------------------------------------------------------

describe("GitHub source — Phase renumbering", () => {
  it("githubSource_phase4_isReview", () => {
    expect(githubContent).toMatch(
      /^## Phase 4 — .*Review/m,
    );
  });

  it("githubSource_phase5_isFixes", () => {
    expect(githubContent).toMatch(
      /^## Phase 5 — Fixes/m,
    );
  });

  it("githubSource_phase6_isCommitAndPR", () => {
    expect(githubContent).toMatch(
      /^## Phase 6 — Commit & PR/m,
    );
  });

  it("githubSource_phase7_isTechLeadReview", () => {
    expect(githubContent).toMatch(
      /^## Phase 7 — Tech Lead Review/m,
    );
  });

  it("githubSource_phase8_isVerification", () => {
    expect(githubContent).toMatch(
      /^## Phase 8 — Final Verification/m,
    );
  });
});

// ---------------------------------------------------------------------------
// GitHub source — Structural preservation
// ---------------------------------------------------------------------------

describe("GitHub source — Structural preservation", () => {
  it("githubSource_preservation_phase1ArchitectSubagent", () => {
    expect(githubContent).toContain(
      "Senior Architect",
    );
  });

  it("githubSource_preservation_phase2TDDHeading", () => {
    expect(githubContent).toMatch(
      /^## Phase 2 — TDD Implementation/m,
    );
  });

  it("githubSource_preservation_g1g7Fallback", () => {
    expect(githubContent).toContain(
      "G1-G7 Fallback",
    );
  });

  it("githubSource_preservation_integrationNotes", () => {
    expect(githubContent).toContain(
      "## Integration Notes",
    );
  });

  it("githubSource_preservation_detailedReferences", () => {
    expect(githubContent).toContain(
      "## Detailed References",
    );
  });

  it("githubSource_preservation_lifecycleCompleteMessage_phase8of8", () => {
    expect(githubContent).toContain(
      "Phase 8/8 completed. Lifecycle complete.",
    );
  });

  it.each([
    ["PROJECT_NAME"],
    ["LANGUAGE"],
    ["COMPILE_COMMAND"],
    ["TEST_COMMAND"],
    ["COVERAGE_COMMAND"],
  ])(
    "githubSource_preservation_placeholderToken_%s",
    (token) => {
      expect(githubContent).toContain(`{{${token}}}`);
    },
  );
});

// ---------------------------------------------------------------------------
// Dual copy consistency (RULE-001)
// ---------------------------------------------------------------------------

describe("Dual copy consistency (RULE-001)", () => {
  it("bothContain_documentationPhaseHeading", () => {
    expect(claudeContent).toMatch(
      /^## Phase 3 — Documentation/m,
    );
    expect(githubContent).toMatch(
      /^## Phase 3 — Documentation/m,
    );
  });

  it("bothContain_cliGeneratorHeading", () => {
    expect(claudeContent).toContain(
      "CLI Documentation Generator",
    );
    expect(githubContent).toContain(
      "CLI Documentation Generator",
    );
  });

  it("bothContain_interfaceCliCondition", () => {
    expect(claudeContent).toMatch(
      /interface.*cli|interfaces.*cli/i,
    );
    expect(githubContent).toMatch(
      /interface.*cli|interfaces.*cli/i,
    );
  });

  it("bothContain_outputPath", () => {
    expect(claudeContent).toContain(
      "docs/api/cli-reference.md",
    );
    expect(githubContent).toContain(
      "docs/api/cli-reference.md",
    );
  });

  it.each([
    ["CLI Reference"],
    ["Quick Start"],
    ["Global Flags"],
    ["Command:"],
    ["Subcommand:"],
    ["Exit Codes"],
  ])(
    "bothContain_outputFormatSection_%s",
    (section) => {
      expect(claudeContent).toContain(section);
      expect(githubContent).toContain(section);
    },
  );

  it.each([
    ["Commander.js"],
    ["Click"],
    ["Cobra"],
    ["Clap"],
  ])(
    "bothContain_frameworkPattern_%s",
    (framework) => {
      expect(claudeContent).toContain(framework);
      expect(githubContent).toContain(framework);
    },
  );

  it("bothContain_skipBehavior", () => {
    expect(claudeContent).toMatch(/skip silently/i);
    expect(githubContent).toMatch(/skip silently/i);
  });

  it("bothContain_9Phases0To8", () => {
    expect(claudeContent).toContain("9 phases (0-8)");
    expect(githubContent).toContain("9 phases (0-8)");
  });
});
