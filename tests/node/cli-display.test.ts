import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { join } from "node:path";
import { tmpdir } from "node:os";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import {
  classifyFiles,
  displayResult,
  formatSummaryTable,
  isKnowledgePackFile,
} from "../../src/cli-display.js";
import type { ComponentCounts } from "../../src/cli-display.js";
import { PipelineResult } from "../../src/models.js";

// --- Helpers ---

function buildComponentCounts(
  overrides: Partial<ComponentCounts> = {},
): ComponentCounts {
  return {
    rules: 0,
    skills: 0,
    knowledgePacks: 0,
    agents: 0,
    hooks: 0,
    settings: 0,
    readme: 0,
    github: 0,
    codex: 0,
    ...overrides,
  };
}

function buildPipelineResult(
  overrides: Partial<{
    success: boolean;
    outputDir: string;
    filesGenerated: readonly string[];
    warnings: readonly string[];
    durationMs: number;
  }> = {},
): PipelineResult {
  return new PipelineResult(
    overrides.success ?? true,
    overrides.outputDir ?? "/out",
    overrides.filesGenerated ?? [],
    overrides.warnings ?? [],
    overrides.durationMs ?? 42,
  );
}

function createSkillDir(
  tmpDir: string,
  name: string,
  content: string,
): string {
  const skillDir = join(tmpDir, "skills", name);
  mkdirSync(skillDir, { recursive: true });
  const skillMdPath = join(skillDir, "SKILL.md");
  writeFileSync(skillMdPath, content, "utf-8");
  return skillDir;
}

// --- Test Suite ---

describe("isKnowledgePackFile", () => {
  let tmpDir: string;

  beforeEach(() => {
    tmpDir = mkdtempSync(join(tmpdir(), "cli-display-test-"));
  });

  afterEach(() => {
    rmSync(tmpDir, { recursive: true, force: true });
  });

  it("isKnowledgePackFile_userInvocableFalse_returnsTrue", () => {
    const skillDir = createSkillDir(
      tmpDir,
      "api-design",
      "---\nname: api-design\nuser-invocable: false\n---\nContent here",
    );
    const skillMd = join(skillDir, "SKILL.md");

    expect(isKnowledgePackFile(skillMd)).toBe(true);
  });

  it("isKnowledgePackFile_knowledgePackHeading_returnsTrue", () => {
    const skillDir = createSkillDir(
      tmpDir,
      "architecture",
      "# Knowledge Pack\n\nSome content.",
    );
    const skillMd = join(skillDir, "SKILL.md");

    expect(isKnowledgePackFile(skillMd)).toBe(true);
  });

  it("isKnowledgePackFile_regularSkill_returnsFalse", () => {
    const skillDir = createSkillDir(
      tmpDir,
      "run-tests",
      "---\nname: run-tests\nuser-invocable: true\n---\n# Run Tests",
    );
    const skillMd = join(skillDir, "SKILL.md");

    expect(isKnowledgePackFile(skillMd)).toBe(false);
  });

  it("isKnowledgePackFile_nonExistentFile_returnsFalse", () => {
    expect(isKnowledgePackFile("/nonexistent/SKILL.md")).toBe(false);
  });

  it("isKnowledgePackFile_nonSkillMdLooksUpParent_returnsTrue", () => {
    const skillDir = createSkillDir(
      tmpDir,
      "coding-kp",
      "---\nuser-invocable: false\n---\nKP content",
    );
    const siblingFile = join(skillDir, "extra.md");
    writeFileSync(siblingFile, "sibling content", "utf-8");

    expect(isKnowledgePackFile(siblingFile)).toBe(true);
  });

  it("isKnowledgePackFile_noSkillMdInParent_returnsFalse", () => {
    const dirNoSkill = join(tmpDir, "no-skill-dir");
    mkdirSync(dirNoSkill, { recursive: true });
    const someFile = join(dirNoSkill, "other.md");
    writeFileSync(someFile, "content", "utf-8");

    expect(isKnowledgePackFile(someFile)).toBe(false);
  });

  it("isKnowledgePackFile_userInvocableFalseInYamlFrontmatter_returnsTrue", () => {
    const skillDir = createSkillDir(
      tmpDir,
      "infra-kp",
      "---\nname: infra\ndescription: Infra KP\nuser-invocable: false\n---\n# Infra",
    );
    const skillMd = join(skillDir, "SKILL.md");

    expect(isKnowledgePackFile(skillMd)).toBe(true);
  });

  it("isKnowledgePackFile_knowledgePackHeadingWithLeadingWhitespace_returnsTrue", () => {
    const skillDir = createSkillDir(
      tmpDir,
      "whitespace-kp",
      "\n  \n# Knowledge Pack\n\nSome content.",
    );
    const skillMd = join(skillDir, "SKILL.md");

    expect(isKnowledgePackFile(skillMd)).toBe(true);
  });

  it("isKnowledgePackFile_userInvocableTrue_returnsFalse", () => {
    const skillDir = createSkillDir(
      tmpDir,
      "regular",
      "---\nname: regular\nuser-invocable: true\n---\n# Regular Skill",
    );
    const skillMd = join(skillDir, "SKILL.md");

    expect(isKnowledgePackFile(skillMd)).toBe(false);
  });
});

describe("classifyFiles", () => {
  it("classifyFiles_rulesPath_countsAsRules", () => {
    const result = classifyFiles(["/out/.claude/rules/01-foo.md"]);

    expect(result.rules).toBe(1);
  });

  it("classifyFiles_skillsPath_countsAsSkills", () => {
    const result = classifyFiles([
      "/out/.claude/skills/nonexistent/SKILL.md",
    ]);

    expect(result.skills).toBe(1);
  });

  it("classifyFiles_agentsPath_countsAsAgents", () => {
    const result = classifyFiles([
      "/out/.claude/agents/architect.md",
    ]);

    expect(result.agents).toBe(1);
  });

  it("classifyFiles_hooksPath_countsAsHooks", () => {
    const result = classifyFiles([
      "/out/.claude/hooks/post-compile-check.sh",
    ]);

    expect(result.hooks).toBe(1);
  });

  it("classifyFiles_settingsFile_countsAsSettings", () => {
    const result = classifyFiles([
      "/out/.claude/settings.json",
    ]);

    expect(result.settings).toBe(1);
  });

  it("classifyFiles_readmeFile_countsAsReadme", () => {
    const result = classifyFiles(["/out/.claude/README.md"]);

    expect(result.readme).toBe(1);
  });

  it("classifyFiles_githubPath_countsAsGithub", () => {
    const result = classifyFiles([
      "/out/.github/instructions/foo.md",
    ]);

    expect(result.github).toBe(1);
  });

  it("classifyFiles_githubSkillsPath_countsAsGithub_notSkills", () => {
    const result = classifyFiles([
      "/out/.github/skills/bar/SKILL.md",
    ]);

    expect(result.github).toBe(1);
    expect(result.skills).toBe(0);
  });

  it("classifyFiles_githubAgentsPath_countsAsGithub_notAgents", () => {
    const result = classifyFiles([
      "/out/.github/agents/dev.agent.md",
    ]);

    expect(result.github).toBe(1);
    expect(result.agents).toBe(0);
  });

  it("classifyFiles_codexConfigPath_countsAsCodex", () => {
    const result = classifyFiles(["/out/.codex/config.toml"]);

    expect(result.codex).toBe(1);
  });

  it("classifyFiles_rootAgentsMdPath_countsAsCodex", () => {
    const result = classifyFiles(["/out/AGENTS.md"]);

    expect(result.codex).toBe(1);
  });

  it("classifyFiles_agentsSkillsPath_countsAsCodex", () => {
    const result = classifyFiles([
      "/out/.agents/skills/x-dev/SKILL.md",
    ]);

    expect(result.codex).toBe(1);
  });

  it("classifyFiles_agentsSkillsRefsPath_countsAsCodex", () => {
    const result = classifyFiles([
      "/out/.agents/skills/coding/references/clean-code.md",
    ]);

    expect(result.codex).toBe(1);
  });

  it("classifyFiles_agentsAndCodexPaths_bothCountAsCodex", () => {
    const result = classifyFiles([
      "/out/.codex/config.toml",
      "/out/.codex/AGENTS.md",
      "/out/.agents/skills/x-dev/SKILL.md",
      "/out/.agents/skills/x-test/SKILL.md",
    ]);

    expect(result.codex).toBe(4);
  });

  it("classifyFiles_emptyArray_returnsAllZeros", () => {
    const result = classifyFiles([]);
    const expected = buildComponentCounts();

    expect(result).toEqual(expected);
  });

  it("classifyFiles_unknownPath_notCounted", () => {
    const result = classifyFiles(["/out/random/file.txt"]);
    const expected = buildComponentCounts();

    expect(result).toEqual(expected);
  });

  it("classifyFiles_mixedPaths_correctCounts", () => {
    const result = classifyFiles([
      "/out/rules/01-id.md",
      "/out/rules/02-domain.md",
      "/out/agents/architect.md",
      "/out/.github/instructions/coding.md",
      "/out/hooks/post.sh",
      "/out/settings.json",
      "/out/README.md",
    ]);

    expect(result.rules).toBe(2);
    expect(result.agents).toBe(1);
    expect(result.github).toBe(1);
    expect(result.hooks).toBe(1);
    expect(result.settings).toBe(1);
    expect(result.readme).toBe(1);
  });

  it("classifyFiles_multipleSameCategory_accumulates", () => {
    const result = classifyFiles([
      "/out/.claude/rules/01-id.md",
      "/out/.claude/rules/02-domain.md",
      "/out/.claude/rules/03-coding.md",
    ]);

    expect(result.rules).toBe(3);
  });
});

describe("classifyFiles with knowledge packs", () => {
  let tmpDir: string;

  beforeEach(() => {
    tmpDir = mkdtempSync(join(tmpdir(), "classify-kp-"));
  });

  afterEach(() => {
    rmSync(tmpDir, { recursive: true, force: true });
  });

  it("classifyFiles_knowledgePackPath_countsAsKnowledgePacks", () => {
    const skillDir = createSkillDir(
      tmpDir,
      "api-design",
      "---\nuser-invocable: false\n---\nKP content",
    );
    const skillMd = join(skillDir, "SKILL.md");

    const result = classifyFiles([skillMd]);

    expect(result.knowledgePacks).toBe(1);
    expect(result.skills).toBe(0);
  });
});

describe("formatSummaryTable", () => {
  it("formatSummaryTable_allCategories_formatsCorrectly", () => {
    const counts = buildComponentCounts({
      rules: 5,
      skills: 14,
      knowledgePacks: 13,
      agents: 8,
      hooks: 1,
      settings: 2,
      readme: 1,
      github: 52,
      codex: 2,
    });

    const table = formatSummaryTable(counts);

    expect(table).toContain("Component");
    expect(table).toContain("Files");
    expect(table).toContain("Rules (.claude)");
    expect(table).toContain("Skills (.claude)");
    expect(table).toContain("Knowledge Packs (.claude)");
    expect(table).toContain("Agents (.claude)");
    expect(table).toContain("Hooks (.claude)");
    expect(table).toContain("Settings (.claude)");
    expect(table).toContain("README");
    expect(table).toContain("GitHub");
    expect(table).toContain("Codex");
  });

  it("formatSummaryTable_onlyNonZero_displaysSubset", () => {
    const counts = buildComponentCounts({
      rules: 5,
      github: 10,
    });

    const table = formatSummaryTable(counts);

    expect(table).toContain("Rules (.claude)");
    expect(table).toContain("GitHub");
    expect(table).not.toContain("Skills (.claude)");
    expect(table).not.toContain("Agents (.claude)");
  });

  it("formatSummaryTable_allZeros_showsOnlyHeaderAndTotal", () => {
    const counts = buildComponentCounts();

    const table = formatSummaryTable(counts);

    expect(table).toContain("Component");
    expect(table).toContain("Total");
    expect(table).not.toContain("Rules (.claude)");
  });

  it("formatSummaryTable_totalIsCorrect", () => {
    const counts = buildComponentCounts({
      rules: 5,
      skills: 14,
      knowledgePacks: 13,
      agents: 8,
      hooks: 1,
      settings: 2,
      readme: 1,
      github: 52,
      codex: 2,
    });

    const table = formatSummaryTable(counts);
    const totalLine = table.split("\n").find(
      (line) => line.includes("Total"),
    );

    expect(totalLine).toContain("98");
  });

  it("formatSummaryTable_containsSeparatorLines", () => {
    const counts = buildComponentCounts({ rules: 1 });
    const table = formatSummaryTable(counts);

    expect(table).toContain("\u2500");
  });

  it("formatSummaryTable_containsHeaderRow", () => {
    const counts = buildComponentCounts({ rules: 1 });
    const table = formatSummaryTable(counts);
    const lines = table.split("\n");

    expect(lines[0]).toContain("Component");
    expect(lines[0]).toContain("Files");
  });

  it("formatSummaryTable_singleCategory_showsOneRowPlusTotal", () => {
    const counts = buildComponentCounts({ hooks: 3 });
    const table = formatSummaryTable(counts);
    const lines = table.split("\n");

    // header + separator + 1 data row + separator + total = 5 lines
    expect(lines).toHaveLength(5);
  });

  it("formatSummaryTable_largeNumbers_alignedCorrectly", () => {
    const counts = buildComponentCounts({ github: 999 });
    const table = formatSummaryTable(counts);

    expect(table).toContain("999");
    expect(table).toContain("Total");
  });

  it("formatSummaryTable_alignmentIsCorrect", () => {
    const counts = buildComponentCounts({
      rules: 5,
      skills: 14,
    });

    const table = formatSummaryTable(counts);
    const lines = table.split("\n");

    // Verify all data lines are consistently formatted
    const dataLines = lines.filter(
      (line) =>
        !line.includes("\u2500")
        && !line.includes("Component"),
    );
    for (const line of dataLines) {
      // Each line starts with "  " (2-space indent)
      expect(line.startsWith("  ")).toBe(true);
    }
  });
});

describe("displayResult", () => {
  let logSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    logSpy = vi.spyOn(console, "log").mockImplementation(
      () => undefined,
    );
  });

  afterEach(() => {
    logSpy.mockRestore();
  });

  it("displayResult_success_printsStatusLine", () => {
    const result = buildPipelineResult({
      durationMs: 42,
      filesGenerated: ["/out/.claude/rules/01-id.md"],
    });

    displayResult(result);

    expect(logSpy).toHaveBeenCalledWith(
      "Pipeline: Success (42ms)",
    );
  });

  it("displayResult_success_printsSummaryTable", () => {
    const result = buildPipelineResult({
      filesGenerated: ["/out/.claude/rules/01-id.md"],
    });

    displayResult(result);

    const allOutput = logSpy.mock.calls
      .map((call) => String(call[0]))
      .join("\n");
    expect(allOutput).toContain("Component");
    expect(allOutput).toContain("Rules (.claude)");
  });

  it("displayResult_success_printsOutputDir", () => {
    const result = buildPipelineResult({
      outputDir: "/custom/out",
    });

    displayResult(result);

    expect(logSpy).toHaveBeenCalledWith("Output: /custom/out");
  });

  it("displayResult_withWarnings_printsEachWarning", () => {
    const result = buildPipelineResult({
      warnings: ["Warning A", "Warning B"],
    });

    displayResult(result);

    expect(logSpy).toHaveBeenCalledWith("Warning: Warning A");
    expect(logSpy).toHaveBeenCalledWith("Warning: Warning B");
  });

  it("displayResult_noWarnings_noWarningLines", () => {
    const result = buildPipelineResult({ warnings: [] });

    displayResult(result);

    const allCalls = logSpy.mock.calls.map(
      (call) => String(call[0]),
    );
    const warningCalls = allCalls.filter(
      (msg) => msg.startsWith("Warning:"),
    );
    expect(warningCalls).toHaveLength(0);
  });

  it("displayResult_multipleWarnings_allPrinted", () => {
    const result = buildPipelineResult({
      warnings: ["W1", "W2", "W3"],
    });

    displayResult(result);

    expect(logSpy).toHaveBeenCalledWith("Warning: W1");
    expect(logSpy).toHaveBeenCalledWith("Warning: W2");
    expect(logSpy).toHaveBeenCalledWith("Warning: W3");
  });

  it("displayResult_notSuccess_throwsError", () => {
    const result = buildPipelineResult({ success: false });

    expect(() => displayResult(result)).toThrow(
      "Pipeline did not succeed",
    );
  });
});
