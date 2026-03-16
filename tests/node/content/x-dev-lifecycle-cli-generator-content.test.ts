import { describe, it, expect } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";

// ---------------------------------------------------------------------------
// Shared constants
// ---------------------------------------------------------------------------

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
const DEPLOYED_COPY = path.resolve(
  __dirname,
  "../../..",
  ".claude/skills/x-dev-lifecycle/SKILL.md",
);

const claudeContent = fs.readFileSync(CLAUDE_SOURCE, "utf-8");
const githubContent = fs.readFileSync(GITHUB_SOURCE, "utf-8");
const deployedContent = fs.readFileSync(DEPLOYED_COPY, "utf-8");

const OUTPUT_FORMAT_SECTIONS = [
  "CLI Reference",
  "Quick Start",
  "Global Flags",
  "Command:",
  "Subcommand:",
  "Exit Codes",
] as const;

const FRAMEWORK_PATTERNS: [string, string][] = [
  ["Commander.js", ".command()"],
  ["Click", "@click.command()"],
  ["Cobra", "cobra.Command"],
  ["Clap", "derive(Parser)"],
];

const PLACEHOLDER_TOKENS = [
  "PROJECT_NAME",
  "LANGUAGE",
  "COMPILE_COMMAND",
  "TEST_COMMAND",
  "COVERAGE_COMMAND",
] as const;

const DISPATCH_INTERFACES: [string, string][] = [
  ["rest", "OpenAPI"],
  ["grpc", "gRPC"],
  ["cli", "CLI"],
  ["websocket", "Event"],
  ["kafka", "Event"],
];

const RENAMED_PHASES: [number, string][] = [
  [4, "Review"],
  [5, "Fixes"],
  [6, "Commit & PR"],
  [7, "Tech Lead Review"],
  [8, "Final Verification"],
];

const SOURCES: [string, string][] = [
  ["Claude", claudeContent],
  ["GitHub", githubContent],
];

// ---------------------------------------------------------------------------
// Documentation Phase exists (both sources)
// ---------------------------------------------------------------------------

describe.each(SOURCES)(
  "%s source — Documentation Phase exists",
  (_label, content) => {
    it("documentationPhase_hasPhase3Heading", () => {
      expect(content).toMatch(/^## Phase 3 — Documentation/m);
    });

    it("phaseCount_shows9Phases0To8", () => {
      expect(content).toContain("9 phases (0-8)");
    });

    it("criticalRule_neverStopBeforePhase8", () => {
      expect(content).toContain("NEVER stop before Phase 8");
    });

    it("phaseOrder_phase3BetweenPhase2AndPhase4", () => {
      const phase2Idx = content.indexOf("## Phase 2");
      const phase3Idx = content.indexOf("## Phase 3 — Documentation");
      const phase4Idx = content.indexOf("## Phase 4");
      expect(phase2Idx).toBeGreaterThan(-1);
      expect(phase3Idx).toBeGreaterThan(phase2Idx);
      expect(phase4Idx).toBeGreaterThan(phase3Idx);
    });

    it("completeFlow_showsPhase3Documentation", () => {
      expect(content).toMatch(/Phase 3: Documentation/);
    });
  },
);

// ---------------------------------------------------------------------------
// CLI generator section (both sources)
// ---------------------------------------------------------------------------

describe.each(SOURCES)(
  "%s source — CLI generator section",
  (_label, content) => {
    it("cliGenerator_hasHeading", () => {
      expect(content).toContain("CLI Documentation Generator");
    });

    it("cliGenerator_hasInterfaceCondition", () => {
      expect(content).toMatch(/interface.*cli|interfaces.*cli/i);
    });

    it("cliGenerator_hasOutputPath", () => {
      expect(content).toContain("docs/api/cli-reference.md");
    });

    it.each(
      OUTPUT_FORMAT_SECTIONS.map((s) => [s]),
    )("cliGenerator_outputFormat_contains_%s", (section) => {
      expect(content).toContain(section);
    });

    it("cliGenerator_flagsTable_hasColumns", () => {
      expect(content).toMatch(/Flag.*Type.*Default.*Description/);
    });

    it("cliGenerator_argsTable_hasColumns", () => {
      expect(content).toMatch(/Argument.*Type.*Required.*Description/);
    });

    it("cliGenerator_usageLine_hasPattern", () => {
      expect(content).toMatch(/\$.*\{tool-name\}.*\{command\}/);
    });

    it("cliGenerator_commandSection_requiresExample", () => {
      expect(content).toMatch(
        /example.*code block|at least 1 example/i,
      );
    });

    it.each(FRAMEWORK_PATTERNS)(
      "cliGenerator_frameworkPattern_%s_contains_%s",
      (framework, pattern) => {
        expect(content).toContain(framework);
        expect(content).toContain(pattern);
      },
    );

    it("cliGenerator_quickStart_requiresAtLeast2Examples", () => {
      expect(content).toMatch(
        /at least 2.*example|2.*usage example/i,
      );
    });

    it("cliGenerator_exitCodes_hasCodeAndMeaning", () => {
      expect(content).toMatch(/Code.*Meaning/);
    });

    it("cliGenerator_skipBehavior_skipsSilently", () => {
      expect(content).toMatch(/skip silently/i);
    });
  },
);

// ---------------------------------------------------------------------------
// Phase renumbering (both sources)
// ---------------------------------------------------------------------------

describe.each(SOURCES)(
  "%s source — Phase renumbering",
  (_label, content) => {
    it.each(RENAMED_PHASES)(
      "phase%i_is_%s",
      (phaseNum, phaseName) => {
        const pattern = new RegExp(
          `^## Phase ${phaseNum} — .*${phaseName}`,
          "m",
        );
        expect(content).toMatch(pattern);
      },
    );
  },
);

// ---------------------------------------------------------------------------
// Structural preservation (both sources)
// ---------------------------------------------------------------------------

describe.each(SOURCES)(
  "%s source — Structural preservation",
  (_label, content) => {
    it("preservation_phase1ArchitectSubagent", () => {
      expect(content).toContain("Senior Architect");
    });

    it("preservation_phase2TDDHeading", () => {
      expect(content).toMatch(/^## Phase 2 — TDD Implementation/m);
    });

    it("preservation_g1g7Fallback", () => {
      expect(content).toContain("G1-G7 Fallback");
    });

    it("preservation_integrationNotes", () => {
      expect(content).toContain("## Integration Notes");
    });

    it("preservation_lifecycleCompleteMessage_phase8of8", () => {
      expect(content).toContain(
        "Phase 8/8 completed. Lifecycle complete.",
      );
    });

    it.each(
      PLACEHOLDER_TOKENS.map((t) => [t]),
    )("preservation_placeholderToken_%s", (token) => {
      expect(content).toContain(`{{${token}}}`);
    });
  },
);

// ---------------------------------------------------------------------------
// GitHub source — additional structural preservation
// ---------------------------------------------------------------------------

describe("GitHub source — additional preservation", () => {
  it("githubSource_preservation_detailedReferences", () => {
    expect(githubContent).toContain("## Detailed References");
  });
});

// ---------------------------------------------------------------------------
// Documentation Phase dispatch table (both sources)
// ---------------------------------------------------------------------------

describe.each(SOURCES)(
  "%s source — Documentation Phase dispatch table",
  (_label, content) => {
    it.each(DISPATCH_INTERFACES)(
      "dispatchTable_contains_%s_interface_with_%s_generator",
      (iface, generator) => {
        expect(content).toContain(iface);
        expect(content).toContain(generator);
      },
    );

    it("dispatchTable_containsChangelogEntry", () => {
      expect(content).toMatch(/[Cc]hangelog/);
    });

    it("dispatchTable_skipWhenNoInterfaces", () => {
      expect(content).toMatch(/[Nn]o documentable interfaces/);
    });
  },
);

// ---------------------------------------------------------------------------
// Deployed copy matches source template
// ---------------------------------------------------------------------------

describe("Deployed copy matches source template", () => {
  it("deployedCopy_matchesClaudeSourceTemplate_byteForByte", () => {
    expect(deployedContent).toBe(claudeContent);
  });
});

// ---------------------------------------------------------------------------
// Dual copy consistency (RULE-001)
// ---------------------------------------------------------------------------

describe("Dual copy consistency (RULE-001)", () => {
  it("bothContain_documentationPhaseHeading", () => {
    expect(claudeContent).toMatch(/^## Phase 3 — Documentation/m);
    expect(githubContent).toMatch(/^## Phase 3 — Documentation/m);
  });

  it("bothContain_cliGeneratorHeading", () => {
    expect(claudeContent).toContain("CLI Documentation Generator");
    expect(githubContent).toContain("CLI Documentation Generator");
  });

  it("bothContain_interfaceCliCondition", () => {
    expect(claudeContent).toMatch(/interface.*cli|interfaces.*cli/i);
    expect(githubContent).toMatch(/interface.*cli|interfaces.*cli/i);
  });

  it("bothContain_outputPath", () => {
    expect(claudeContent).toContain("docs/api/cli-reference.md");
    expect(githubContent).toContain("docs/api/cli-reference.md");
  });

  it.each(
    OUTPUT_FORMAT_SECTIONS.map((s) => [s]),
  )("bothContain_outputFormatSection_%s", (section) => {
    expect(claudeContent).toContain(section);
    expect(githubContent).toContain(section);
  });

  it.each(
    FRAMEWORK_PATTERNS.map(([f]) => [f]),
  )("bothContain_frameworkPattern_%s", (framework) => {
    expect(claudeContent).toContain(framework);
    expect(githubContent).toContain(framework);
  });

  it("bothContain_skipBehavior", () => {
    expect(claudeContent).toMatch(/skip silently/i);
    expect(githubContent).toMatch(/skip silently/i);
  });

  it("bothContain_9Phases0To8", () => {
    expect(claudeContent).toContain("9 phases (0-8)");
    expect(githubContent).toContain("9 phases (0-8)");
  });
});
