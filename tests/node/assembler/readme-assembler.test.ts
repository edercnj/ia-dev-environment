import { describe, it, expect, beforeEach, afterEach } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";
import { tmpdir } from "node:os";
import {
  countRules, countSkills, countAgents, countKnowledgePacks,
  countHooks, countSettings, countGithubFiles,
  countGithubComponent, countGithubSkills, countCodexFiles,
  isKnowledgePack, extractRuleNumber, extractRuleScope,
  extractSkillDescription,
  buildRulesTable, buildSkillsTable, buildAgentsTable,
  buildKnowledgePacksTable, buildReadmeHooksSection,
  buildSettingsSection, buildMappingTable, buildGenerationSummary,
  generateReadme, generateMinimalReadme,
  buildStructureBlock, buildTipsBlock,
  ReadmeAssembler,
} from "../../../src/assembler/readme-assembler.js";
import { TemplateEngine } from "../../../src/template-engine.js";
import {
  ProjectConfig, ProjectIdentity, ArchitectureConfig,
  InterfaceConfig, LanguageConfig, FrameworkConfig,
  DEFAULT_FOUNDATION,
} from "../../../src/models.js";

function buildConfig(overrides: {
  archStyle?: string;
  interfaces?: InterfaceConfig[];
  language?: string;
  buildTool?: string;
} = {}): ProjectConfig {
  return new ProjectConfig(
    new ProjectIdentity("test-project", "A test project"),
    new ArchitectureConfig(overrides.archStyle ?? "library"),
    overrides.interfaces ?? [new InterfaceConfig("cli")],
    new LanguageConfig(overrides.language ?? "typescript", "5"),
    new FrameworkConfig(
      "commander", "", overrides.buildTool ?? "npm",
    ),
  );
}

function createRule(outputDir: string, filename: string): void {
  const dir = path.join(outputDir, "rules");
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(path.join(dir, filename), `# ${filename}`);
}

function createSkill(
  outputDir: string, name: string,
  description: string, isKP = false,
): void {
  const dir = path.join(outputDir, "skills", name);
  fs.mkdirSync(dir, { recursive: true });
  const kpFlag = isKP ? "user-invocable: false\n" : "";
  fs.writeFileSync(
    path.join(dir, "SKILL.md"),
    `name: ${name}\n${kpFlag}description: "${description}"`,
  );
}

function createAgent(outputDir: string, name: string): void {
  const dir = path.join(outputDir, "agents");
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(path.join(dir, `${name}.md`), `# ${name}`);
}

function createHook(outputDir: string, name: string): void {
  const dir = path.join(outputDir, "hooks");
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(path.join(dir, name), "hook");
}

function createSettings(
  outputDir: string, includeLocal = false,
): void {
  fs.writeFileSync(
    path.join(outputDir, "settings.json"), "{}",
  );
  if (includeLocal) {
    fs.writeFileSync(
      path.join(outputDir, "settings.local.json"), "{}",
    );
  }
}

function createGithubArtifacts(
  baseDir: string,
  structure: Record<string, string[]>,
): void {
  for (const [dir, files] of Object.entries(structure)) {
    const fullDir = path.join(baseDir, dir);
    fs.mkdirSync(fullDir, { recursive: true });
    for (const file of files) {
      fs.writeFileSync(path.join(fullDir, file), "content");
    }
  }
}

function createCodexArtifacts(baseDir: string): void {
  const codexDir = path.join(baseDir, ".codex");
  fs.mkdirSync(codexDir, { recursive: true });
  fs.writeFileSync(path.join(codexDir, "AGENTS.md"), "# Agents");
  fs.writeFileSync(path.join(codexDir, "config.toml"), "[codex]");
}

function createReadmeTemplate(resourcesDir: string): void {
  const src = path.join(
    process.cwd(), "resources", "readme-template.md",
  );
  const dest = path.join(resourcesDir, "readme-template.md");
  fs.copyFileSync(src, dest);
}

describe("ReadmeAssembler", () => {
  let tmpDir: string;
  let outputDir: string;
  let resourcesDir: string;

  beforeEach(() => {
    tmpDir = fs.mkdtempSync(
      path.join(tmpdir(), "readme-asm-test-"),
    );
    outputDir = path.join(tmpDir, "output");
    resourcesDir = path.join(tmpDir, "resources");
    fs.mkdirSync(outputDir, { recursive: true });
    fs.mkdirSync(resourcesDir, { recursive: true });
  });

  afterEach(() => {
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  describe("counting functions", () => {
    it("countRules_withMdFiles_returnsCorrectCount", () => {
      createRule(outputDir, "01-identity.md");
      createRule(outputDir, "02-domain.md");
      createRule(outputDir, "03-coding.md");
      expect(countRules(outputDir)).toBe(3);
    });

    it("countRules_emptyDir_returnsZero", () => {
      fs.mkdirSync(path.join(outputDir, "rules"), { recursive: true });
      expect(countRules(outputDir)).toBe(0);
    });

    it("countRules_dirMissing_returnsZero", () => {
      expect(countRules(outputDir)).toBe(0);
    });

    it("countSkills_withSkillMdFiles_returnsCount", () => {
      createSkill(outputDir, "commit", "Commit changes");
      createSkill(outputDir, "review", "Code review");
      expect(countSkills(outputDir)).toBe(2);
    });

    it("countSkills_dirMissing_returnsZero", () => {
      expect(countSkills(outputDir)).toBe(0);
    });

    it("countAgents_withMdFiles_returnsCount", () => {
      createAgent(outputDir, "architect");
      createAgent(outputDir, "tech-lead");
      createAgent(outputDir, "qa-engineer");
      expect(countAgents(outputDir)).toBe(3);
    });

    it("countAgents_dirMissing_returnsZero", () => {
      expect(countAgents(outputDir)).toBe(0);
    });

    it("countKnowledgePacks_detectsUserInvocableFalse", () => {
      createSkill(outputDir, "coding-std", "Coding", true);
      createSkill(outputDir, "testing", "Tests", true);
      createSkill(outputDir, "commit", "Commit");
      expect(countKnowledgePacks(outputDir)).toBe(2);
    });

    it("countKnowledgePacks_excludesRegularSkills", () => {
      createSkill(outputDir, "commit", "Commit");
      createSkill(outputDir, "review", "Review");
      expect(countKnowledgePacks(outputDir)).toBe(0);
    });

    it("countKnowledgePacks_detectsKnowledgePackHeading", () => {
      const dir = path.join(outputDir, "skills", "my-kp");
      fs.mkdirSync(dir, { recursive: true });
      fs.writeFileSync(
        path.join(dir, "SKILL.md"),
        "# Knowledge Pack\nSome content",
      );
      expect(countKnowledgePacks(outputDir)).toBe(1);
    });

    it("countHooks_countsAllEntries", () => {
      createHook(outputDir, "post-compile.sh");
      createHook(outputDir, "pre-lint.sh");
      expect(countHooks(outputDir)).toBe(2);
    });

    it("countHooks_dirMissing_returnsZero", () => {
      expect(countHooks(outputDir)).toBe(0);
    });

    it("countSettings_bothExist_returnsTwo", () => {
      createSettings(outputDir, true);
      expect(countSettings(outputDir)).toBe(2);
    });

    it("countSettings_noneExist_returnsZero", () => {
      expect(countSettings(outputDir)).toBe(0);
    });

    it("countSettings_oneExists_returnsOne", () => {
      createSettings(outputDir, false);
      expect(countSettings(outputDir)).toBe(1);
    });

    it("countGithubFiles_recursiveCounting", () => {
      const ghDir = path.join(outputDir, "github");
      createGithubArtifacts(ghDir, {
        agents: ["a.md", "b.md"],
        instructions: ["c.md"],
        "skills/x": ["SKILL.md"],
      });
      expect(countGithubFiles(ghDir)).toBe(4);
    });

    it("countGithubFiles_dirMissing_returnsZero", () => {
      expect(countGithubFiles(path.join(outputDir, "github"))).toBe(0);
    });

    it("countGithubComponent_countsDirectFiles", () => {
      const ghDir = path.join(outputDir, "github");
      createGithubArtifacts(ghDir, {
        agents: ["a.md", "b.md", "c.md"],
      });
      expect(countGithubComponent(ghDir, "agents")).toBe(3);
    });

    it("countGithubComponent_dirMissing_returnsZero", () => {
      const ghDir = path.join(outputDir, "github");
      expect(countGithubComponent(ghDir, "agents")).toBe(0);
    });

    it("countGithubSkills_countsSkillMdInSubdirs", () => {
      const ghDir = path.join(outputDir, "github");
      createGithubArtifacts(ghDir, {
        "skills/x": ["SKILL.md"],
        "skills/y": ["SKILL.md"],
      });
      expect(countGithubSkills(ghDir)).toBe(2);
    });

    it("countGithubSkills_dirMissing_returnsZero", () => {
      const ghDir = path.join(outputDir, "github");
      expect(countGithubSkills(ghDir)).toBe(0);
    });

    it("countCodexFiles_withFiles_returnsCount", () => {
      const codexDir = path.join(outputDir, ".codex");
      fs.mkdirSync(codexDir, { recursive: true });
      fs.writeFileSync(path.join(codexDir, "AGENTS.md"), "# A");
      fs.writeFileSync(path.join(codexDir, "config.toml"), "[c]");
      expect(countCodexFiles(codexDir)).toBe(2);
    });

    it("countCodexFiles_dirMissing_returnsZero", () => {
      expect(countCodexFiles(path.join(outputDir, ".codex"))).toBe(0);
    });

    it("countCodexFiles_emptyDir_returnsZero", () => {
      const codexDir = path.join(outputDir, ".codex");
      fs.mkdirSync(codexDir, { recursive: true });
      expect(countCodexFiles(codexDir)).toBe(0);
    });

    it("countCodexFiles_ignoresSubdirectories", () => {
      const codexDir = path.join(outputDir, ".codex");
      fs.mkdirSync(path.join(codexDir, "subdir"), { recursive: true });
      fs.writeFileSync(path.join(codexDir, "AGENTS.md"), "# A");
      expect(countCodexFiles(codexDir)).toBe(1);
    });
  });

  describe("detection and parsing", () => {
    it.each([
      ["user-invocable: false", true],
      ["# Knowledge Pack\nSome content", true],
      ["user-invocable: true\ndescription: foo", false],
      ["name: my-skill\ndescription: foo", false],
    ])(
      "isKnowledgePack with content '%s' returns %s",
      (content, expected) => {
        const dir = path.join(outputDir, "skills", "test-skill");
        fs.mkdirSync(dir, { recursive: true });
        const p = path.join(dir, "SKILL.md");
        fs.writeFileSync(p, content);
        expect(isKnowledgePack(p)).toBe(expected);
      },
    );

    it("extractRuleNumber_numberedFile_returnsNumber", () => {
      expect(extractRuleNumber("01-project-identity.md")).toBe("01");
    });

    it("extractRuleNumber_multiDigit_returnsAllDigits", () => {
      expect(extractRuleNumber("123-many-digits.md")).toBe("123");
    });

    it("extractRuleNumber_unnumberedFile_returnsEmpty", () => {
      expect(extractRuleNumber("no-number.md")).toBe("");
    });

    it("extractRuleScope_removesNumberAndExtension", () => {
      expect(extractRuleScope("01-project-identity.md"))
        .toBe("project identity");
    });

    it("extractRuleScope_noNumber_removesExtension", () => {
      expect(extractRuleScope("coding-standards.md"))
        .toBe("coding standards");
    });

    it("extractSkillDescription_findsDescriptionField", () => {
      const dir = path.join(outputDir, "skills", "my-skill");
      fs.mkdirSync(dir, { recursive: true });
      const p = path.join(dir, "SKILL.md");
      fs.writeFileSync(p, 'description: "My cool skill"');
      expect(extractSkillDescription(p)).toBe("My cool skill");
    });

    it("extractSkillDescription_stripsQuotes", () => {
      const dir = path.join(outputDir, "skills", "my-skill");
      fs.mkdirSync(dir, { recursive: true });
      const p = path.join(dir, "SKILL.md");
      fs.writeFileSync(p, "description: 'Quoted skill'");
      expect(extractSkillDescription(p)).toBe("Quoted skill");
    });

    it("extractSkillDescription_noDescription_returnsEmpty", () => {
      const dir = path.join(outputDir, "skills", "my-skill");
      fs.mkdirSync(dir, { recursive: true });
      const p = path.join(dir, "SKILL.md");
      fs.writeFileSync(p, "name: my-skill\n");
      expect(extractSkillDescription(p)).toBe("");
    });
  });

  describe("table builders", () => {
    it("buildRulesTable_withRules_formatsMarkdownTable", () => {
      createRule(outputDir, "01-identity.md");
      createRule(outputDir, "02-domain.md");
      const result = buildRulesTable(outputDir);
      expect(result).toContain("| # | File | Scope |");
      expect(result).toContain("| 01 | `01-identity.md` | identity |");
      expect(result).toContain("| 02 | `02-domain.md` | domain |");
    });

    it("buildRulesTable_noRules_returnsMessage", () => {
      expect(buildRulesTable(outputDir)).toBe("No rules configured.");
    });

    it("buildRulesTable_emptyDir_returnsMessage", () => {
      fs.mkdirSync(path.join(outputDir, "rules"), { recursive: true });
      expect(buildRulesTable(outputDir)).toBe("No rules configured.");
    });

    it("buildRulesTable_sortsAlphabetically", () => {
      createRule(outputDir, "03-coding.md");
      createRule(outputDir, "01-identity.md");
      const result = buildRulesTable(outputDir);
      const idx01 = result.indexOf("01-identity");
      const idx03 = result.indexOf("03-coding");
      expect(idx01).toBeLessThan(idx03);
    });

    it("buildSkillsTable_withSkills_formatsTable", () => {
      createSkill(outputDir, "commit", "Commit changes");
      createSkill(outputDir, "review", "Code review");
      const result = buildSkillsTable(outputDir);
      expect(result).toContain("| Skill | Path | Description |");
      expect(result).toContain("| **commit** | `/commit` | Commit changes |");
    });

    it("buildSkillsTable_excludesKnowledgePacks", () => {
      createSkill(outputDir, "commit", "Commit changes");
      createSkill(outputDir, "coding-std", "Standards", true);
      const result = buildSkillsTable(outputDir);
      expect(result).toContain("commit");
      expect(result).not.toContain("coding-std");
    });

    it("buildSkillsTable_noSkills_returnsMessage", () => {
      expect(buildSkillsTable(outputDir)).toBe("No skills configured.");
    });

    it("buildSkillsTable_onlyKPs_returnsMessage", () => {
      createSkill(outputDir, "coding-std", "Standards", true);
      expect(buildSkillsTable(outputDir)).toBe("No skills configured.");
    });

    it("buildAgentsTable_withAgents_formatsTable", () => {
      createAgent(outputDir, "architect");
      createAgent(outputDir, "tech-lead");
      const result = buildAgentsTable(outputDir);
      expect(result).toContain("| Agent | File |");
      expect(result).toContain("| **architect** | `architect.md` |");
    });

    it("buildAgentsTable_noAgents_returnsMessage", () => {
      expect(buildAgentsTable(outputDir)).toBe("No agents configured.");
    });

    it("buildKnowledgePacksTable_withKps_formatsTable", () => {
      createSkill(outputDir, "coding-std", "Standards", true);
      createSkill(outputDir, "testing", "Tests", true);
      createSkill(outputDir, "commit", "Commit");
      const result = buildKnowledgePacksTable(outputDir);
      expect(result).toContain("| Pack | Usage |");
      expect(result).toContain("| `coding-std` | Referenced internally by agents |");
      expect(result).toContain("| `testing` | Referenced internally by agents |");
      expect(result).not.toContain("commit");
    });

    it("buildKnowledgePacksTable_noKps_returnsMessage", () => {
      createSkill(outputDir, "commit", "Commit");
      expect(buildKnowledgePacksTable(outputDir))
        .toBe("No knowledge packs configured.");
    });

    it("buildKnowledgePacksTable_dirWithoutSkillMd_skipped", () => {
      const dir = path.join(outputDir, "skills", "empty-dir");
      fs.mkdirSync(dir, { recursive: true });
      createSkill(outputDir, "coding-std", "Standards", true);
      const result = buildKnowledgePacksTable(outputDir);
      expect(result).not.toContain("empty-dir");
      expect(result).toContain("coding-std");
    });

    it("buildReadmeHooksSection_typescriptNpm_returnsPostCompileCheck", () => {
      const config = buildConfig({
        language: "typescript", buildTool: "npm",
      });
      const result = buildReadmeHooksSection(config);
      expect(result).toContain("### Post-Compile Check");
      expect(result).toContain(".ts");
      expect(result).toContain("npx --no-install tsc --noEmit");
    });

    it("buildReadmeHooksSection_noHookKey_returnsNoHooksMessage", () => {
      const config = buildConfig({
        language: "python", buildTool: "pip",
      });
      const result = buildReadmeHooksSection(config);
      expect(result).toBe("No hooks configured.");
    });

    it("buildSettingsSection_returnsStaticContent", () => {
      const result = buildSettingsSection();
      expect(result).toContain("### settings.json");
      expect(result).toContain("### settings.local.json");
      expect(result).toContain("permissions.allow");
    });

    it("buildMappingTable_withGithubDir_includesTotal", () => {
      const siblingGhDir = path.join(path.dirname(outputDir), ".github");
      createGithubArtifacts(siblingGhDir, {
        agents: ["a.md", "b.md"],
      });
      const result = buildMappingTable(outputDir);
      expect(result).toContain("**Total .github/ artifacts: 2**");
    });

    it("buildMappingTable_noGithubDir_omitsTotal", () => {
      const result = buildMappingTable(outputDir);
      expect(result).not.toContain("Total .github/ artifacts");
    });

    it("buildMappingTable_contains8Rows", () => {
      const result = buildMappingTable(outputDir);
      const dataRows = result.split("\n")
        .filter((l) => l.startsWith("| "))
        .filter((l) => !l.startsWith("| .claude/"))
        .filter((l) => !l.startsWith("|---"));
      expect(dataRows).toHaveLength(8);
    });

    it("buildMappingTable_includesCodexColumn", () => {
      const result = buildMappingTable(outputDir);
      expect(result).toContain("| .claude/ | .github/ | .codex/ | Notes |");
    });

    it("buildMappingTable_codexColumnHasConfigToml", () => {
      const result = buildMappingTable(outputDir);
      expect(result).toContain("`config.toml`");
    });

    it("buildMappingTable_codexColumnHasAgentsMd", () => {
      const result = buildMappingTable(outputDir);
      expect(result).toContain("Sections in AGENTS.md");
    });

    it("buildGenerationSummary_countsAllComponents", () => {
      createRule(outputDir, "01-identity.md");
      createSkill(outputDir, "commit", "Commit");
      createSkill(outputDir, "coding-std", "Standards", true);
      createAgent(outputDir, "architect");
      createHook(outputDir, "post-compile.sh");
      createSettings(outputDir, true);
      const config = buildConfig();
      const result = buildGenerationSummary(outputDir, config);
      expect(result).toContain("| Rules (.claude) | 1 |");
      expect(result).toContain("| Skills (.claude) | 1 |");
      expect(result).toContain("| Knowledge Packs (.claude) | 1 |");
      expect(result).toContain("| Agents (.claude) | 1 |");
      expect(result).toContain("| Hooks (.claude) | 1 |");
      expect(result).toContain("| Settings (.claude) | 2 |");
    });

    it("buildGenerationSummary_includesVersionStamp", () => {
      const config = buildConfig();
      const result = buildGenerationSummary(outputDir, config);
      expect(result).toContain(
        `Generated by \`ia-dev-env v${DEFAULT_FOUNDATION.version}\`.`,
      );
    });

    it("buildGenerationSummary_skillsCountExcludesKps", () => {
      createSkill(outputDir, "commit", "Commit");
      createSkill(outputDir, "review", "Review");
      createSkill(outputDir, "coding-std", "Standards", true);
      const config = buildConfig();
      const result = buildGenerationSummary(outputDir, config);
      expect(result).toContain("| Skills (.claude) | 2 |");
    });

    it("buildGenerationSummary_githubMcpCountedWhenExists", () => {
      const ghDir = path.join(path.dirname(outputDir), ".github");
      fs.mkdirSync(ghDir, { recursive: true });
      fs.writeFileSync(
        path.join(ghDir, "copilot-mcp.json"), "{}",
      );
      const config = buildConfig();
      const result = buildGenerationSummary(outputDir, config);
      expect(result).toContain("| MCP (.github) | 1 |");
    });

    it("buildGenerationSummary_githubMcpZeroWhenMissing", () => {
      const config = buildConfig();
      const result = buildGenerationSummary(outputDir, config);
      expect(result).toContain("| MCP (.github) | 0 |");
    });

    it("buildGenerationSummary_codexCountWhenExists", () => {
      createCodexArtifacts(path.dirname(outputDir));
      const config = buildConfig();
      const result = buildGenerationSummary(outputDir, config);
      expect(result).toContain("| Codex (.codex) | 2 |");
    });

    it("buildGenerationSummary_codexCountZeroWhenMissing", () => {
      const config = buildConfig();
      const result = buildGenerationSummary(outputDir, config);
      expect(result).toContain("| Codex (.codex) | 0 |");
    });

    it("buildGenerationSummary_githubInstructionsIncludesGlobal", () => {
      const ghDir = path.join(path.dirname(outputDir), ".github");
      createGithubArtifacts(ghDir, {
        instructions: ["a.md", "b.md", "c.md"],
      });
      fs.writeFileSync(
        path.join(ghDir, "copilot-instructions.md"), "global",
      );
      const config = buildConfig();
      const result = buildGenerationSummary(outputDir, config);
      expect(result).toContain("| Instructions (.github) | 4 |");
    });
  });

  describe("README generation", () => {
    it("generateReadme_replacesAllPlaceholders", () => {
      createRule(outputDir, "01-identity.md");
      createSkill(outputDir, "commit", "Commit");
      createAgent(outputDir, "architect");
      createReadmeTemplate(resourcesDir);
      const config = buildConfig();
      const tpl = path.join(resourcesDir, "readme-template.md");
      const result = generateReadme(config, outputDir, tpl);
      expect(result).not.toMatch(/\{\{[A-Z_]+\}\}/);
    });

    it("generateReadme_containsProjectName", () => {
      createReadmeTemplate(resourcesDir);
      const config = buildConfig();
      const tpl = path.join(resourcesDir, "readme-template.md");
      const result = generateReadme(config, outputDir, tpl);
      expect(result).toContain("test-project");
    });

    it("generateReadme_containsRulesTable", () => {
      createRule(outputDir, "01-identity.md");
      createReadmeTemplate(resourcesDir);
      const config = buildConfig();
      const tpl = path.join(resourcesDir, "readme-template.md");
      const result = generateReadme(config, outputDir, tpl);
      expect(result).toContain("| # | File | Scope |");
    });

    it("generateReadme_containsSkillsTable", () => {
      createSkill(outputDir, "commit", "Commit changes");
      createReadmeTemplate(resourcesDir);
      const config = buildConfig();
      const tpl = path.join(resourcesDir, "readme-template.md");
      const result = generateReadme(config, outputDir, tpl);
      expect(result).toContain("| Skill | Path | Description |");
    });

    it("generateReadme_containsAgentsTable", () => {
      createAgent(outputDir, "architect");
      createReadmeTemplate(resourcesDir);
      const config = buildConfig();
      const tpl = path.join(resourcesDir, "readme-template.md");
      const result = generateReadme(config, outputDir, tpl);
      expect(result).toContain("| Agent | File |");
    });

    it("generateReadme_containsHooksSection", () => {
      createReadmeTemplate(resourcesDir);
      const config = buildConfig({
        language: "typescript", buildTool: "npm",
      });
      const tpl = path.join(resourcesDir, "readme-template.md");
      const result = generateReadme(config, outputDir, tpl);
      expect(result).toContain("### Post-Compile Check");
    });

    it("generateReadme_containsSettingsSection", () => {
      createReadmeTemplate(resourcesDir);
      const config = buildConfig();
      const tpl = path.join(resourcesDir, "readme-template.md");
      const result = generateReadme(config, outputDir, tpl);
      expect(result).toContain("### settings.json");
    });

    it("generateReadme_containsMappingTable", () => {
      createReadmeTemplate(resourcesDir);
      const config = buildConfig();
      const tpl = path.join(resourcesDir, "readme-template.md");
      const result = generateReadme(config, outputDir, tpl);
      expect(result).toContain("| .claude/ | .github/ | .codex/ | Notes |");
    });

    it("generateReadme_containsGenerationSummary", () => {
      createReadmeTemplate(resourcesDir);
      const config = buildConfig();
      const tpl = path.join(resourcesDir, "readme-template.md");
      const result = generateReadme(config, outputDir, tpl);
      expect(result).toContain("| Component | Count |");
    });

    it("generateMinimalReadme_containsProjectName", () => {
      const config = buildConfig();
      const result = generateMinimalReadme(config);
      expect(result).toContain("test-project");
    });

    it("generateMinimalReadme_containsStructureBlock", () => {
      const config = buildConfig();
      const result = generateMinimalReadme(config);
      expect(result).toContain("## Structure");
      expect(result).toContain(".claude/");
    });

    it("generateMinimalReadme_containsTipsBlock", () => {
      const config = buildConfig();
      const result = generateMinimalReadme(config);
      expect(result).toContain("## Tips");
    });

    it("generateMinimalReadme_tipsContainArchStyle", () => {
      const config = buildConfig({ archStyle: "microservice" });
      const result = generateMinimalReadme(config);
      expect(result).toContain("microservice");
    });

    it("generateMinimalReadme_tipsContainInterfaces", () => {
      const config = buildConfig({
        interfaces: [new InterfaceConfig("rest")],
      });
      const result = generateMinimalReadme(config);
      expect(result).toContain("rest");
    });

    it("generateMinimalReadme_noInterfaces_showsNone", () => {
      const config = buildConfig({ interfaces: [] });
      const result = generateMinimalReadme(config);
      expect(result).toContain("(none)");
    });
  });

  describe("buildStructureBlock", () => {
    it("buildStructureBlock_default_containsDirectoryTree", () => {
      const result = buildStructureBlock();
      expect(result).toContain(".claude/");
      expect(result).toContain("rules/");
      expect(result).toContain("skills/");
      expect(result).toContain("agents/");
    });
  });

  describe("buildTipsBlock", () => {
    it("buildTipsBlock_withArchAndIfaces_includesValues", () => {
      const result = buildTipsBlock("library", "cli rest");
      expect(result).toContain("library");
      expect(result).toContain("cli rest");
    });
  });

  describe("ReadmeAssembler.assemble", () => {
    it("assemble_templateExists_generatesFullReadme", () => {
      createRule(outputDir, "01-identity.md");
      createSkill(outputDir, "commit", "Commit");
      createAgent(outputDir, "architect");
      createReadmeTemplate(resourcesDir);
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const assembler = new ReadmeAssembler();
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const content = fs.readFileSync(
        path.join(outputDir, "README.md"), "utf-8",
      );
      expect(content).toContain("test-project");
      expect(content).toContain("| # | File | Scope |");
    });

    it("assemble_templateMissing_generatesMinimalReadme", () => {
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const assembler = new ReadmeAssembler();
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const content = fs.readFileSync(
        path.join(outputDir, "README.md"), "utf-8",
      );
      expect(content).toContain("## Structure");
      expect(content).toContain("## Tips");
    });

    it("assemble_writesReadmeMd", () => {
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const assembler = new ReadmeAssembler();
      assembler.assemble(config, outputDir, resourcesDir, engine);
      expect(
        fs.existsSync(path.join(outputDir, "README.md")),
      ).toBe(true);
    });

    it("assemble_returnsArrayWithSinglePath", () => {
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const assembler = new ReadmeAssembler();
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result).toHaveLength(1);
      expect(result[0]).toContain("README.md");
    });

    it("assemble_fullReadme_countsMatchFilesystem", () => {
      createRule(outputDir, "01-identity.md");
      createRule(outputDir, "02-domain.md");
      createSkill(outputDir, "commit", "Commit");
      createSkill(outputDir, "review", "Review");
      createAgent(outputDir, "architect");
      createReadmeTemplate(resourcesDir);
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const assembler = new ReadmeAssembler();
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const content = fs.readFileSync(
        path.join(outputDir, "README.md"), "utf-8",
      );
      expect(content).toContain("**Total: 2 rules**");
      expect(content).toContain("**Total: 2 skills**");
      expect(content).toContain("**Total: 1 agents**");
    });

    it("assemble_fullReadme_knowledgePacksDetected", () => {
      createSkill(outputDir, "commit", "Commit");
      createSkill(outputDir, "coding-std", "Standards", true);
      createReadmeTemplate(resourcesDir);
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const assembler = new ReadmeAssembler();
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const content = fs.readFileSync(
        path.join(outputDir, "README.md"), "utf-8",
      );
      expect(content).toContain("| `coding-std` | Referenced internally by agents |");
      expect(content).not.toMatch(
        /\| \*\*coding-std\*\* \| `\/coding-std`/,
      );
    });

    it("assemble_fullReadme_codexCountInSummary", () => {
      createReadmeTemplate(resourcesDir);
      createCodexArtifacts(path.dirname(outputDir));
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const assembler = new ReadmeAssembler();
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const content = fs.readFileSync(
        path.join(outputDir, "README.md"), "utf-8",
      );
      expect(content).toContain("| Codex (.codex) | 2 |");
    });

    it("assemble_fullReadme_codexMappingColumnPresent", () => {
      createReadmeTemplate(resourcesDir);
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const assembler = new ReadmeAssembler();
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const content = fs.readFileSync(
        path.join(outputDir, "README.md"), "utf-8",
      );
      expect(content).toContain(".codex/");
      expect(content).toContain("`config.toml`");
    });

    it("assemble_fullReadme_codexStructureSectionPresent", () => {
      createReadmeTemplate(resourcesDir);
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const assembler = new ReadmeAssembler();
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const content = fs.readFileSync(
        path.join(outputDir, "README.md"), "utf-8",
      );
      expect(content).toContain("### .codex/ (OpenAI Codex)");
      expect(content).toContain("AGENTS.md");
      expect(content).toContain("config.toml");
    });

    it("assemble_engineNotUsed_noError", () => {
      createReadmeTemplate(resourcesDir);
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const assembler = new ReadmeAssembler();
      expect(() => assembler.assemble(
        config, outputDir, resourcesDir, engine,
      )).not.toThrow();
    });
  });
});
