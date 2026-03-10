import { describe, it, expect, beforeEach, afterEach } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";
import { tmpdir } from "node:os";
import {
  GithubPromptsAssembler,
  GITHUB_PROMPT_TEMPLATES,
} from "../../../src/assembler/github-prompts-assembler.js";
import { TemplateEngine } from "../../../src/template-engine.js";
import {
  ProjectConfig,
  ProjectIdentity,
  ArchitectureConfig,
  InterfaceConfig,
  LanguageConfig,
  FrameworkConfig,
} from "../../../src/models.js";

function buildConfig(): ProjectConfig {
  return new ProjectConfig(
    new ProjectIdentity("my-app", "A test application"),
    new ArchitectureConfig("microservice", false, false),
    [new InterfaceConfig("rest")],
    new LanguageConfig("typescript", "5"),
    new FrameworkConfig("commander", "", "npm"),
  );
}

function createPromptTemplate(
  resourcesDir: string,
  filename: string,
  content: string,
): void {
  const dir = path.join(resourcesDir, "github-prompts-templates");
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(path.join(dir, filename), content, "utf-8");
}

describe("GithubPromptsAssembler", () => {
  let tmpDir: string;
  let resourcesDir: string;
  let outputDir: string;
  let assembler: GithubPromptsAssembler;

  beforeEach(() => {
    tmpDir = fs.mkdtempSync(
      path.join(tmpdir(), "gh-prompts-asm-test-"),
    );
    resourcesDir = path.join(tmpDir, "resources");
    fs.mkdirSync(resourcesDir, { recursive: true });
    outputDir = path.join(tmpDir, "output");
    fs.mkdirSync(outputDir, { recursive: true });
    assembler = new GithubPromptsAssembler();
  });

  afterEach(() => {
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  describe("GITHUB_PROMPT_TEMPLATES constant", () => {
    it("contains4Templates", () => {
      expect(GITHUB_PROMPT_TEMPLATES).toHaveLength(4);
    });
  });

  describe("assemble", () => {
    it("assemble_validConfig_generates4Prompts", () => {
      for (const template of GITHUB_PROMPT_TEMPLATES) {
        createPromptTemplate(
          resourcesDir, template,
          `# Prompt for {{ project_name }}`,
        );
      }
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result).toHaveLength(4);
      for (const filePath of result) {
        expect(fs.existsSync(filePath)).toBe(true);
      }
    });

    it("assemble_templatesDirMissing_returnsEmpty", () => {
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result).toEqual([]);
    });

    it("assemble_individualTemplateMissing_skipsIt", () => {
      createPromptTemplate(
        resourcesDir, "new-feature.prompt.md.j2",
        "# New feature for {{ project_name }}",
      );
      createPromptTemplate(
        resourcesDir, "code-review.prompt.md.j2",
        "# Review for {{ project_name }}",
      );
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result).toHaveLength(2);
    });

    it("assemble_outputName_removesJ2Suffix", () => {
      createPromptTemplate(
        resourcesDir, "new-feature.prompt.md.j2",
        "# Feature",
      );
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result[0]).toContain("new-feature.prompt.md");
      expect(result[0]).not.toContain(".j2");
    });

    it("assemble_rendersNunjucksTemplates_notJustPlaceholders", () => {
      createPromptTemplate(
        resourcesDir, "new-feature.prompt.md.j2",
        "Project: {{ project_name }}",
      );
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const content = fs.readFileSync(
        path.join(outputDir, "github", "prompts", "new-feature.prompt.md"),
        "utf-8",
      );
      expect(content).toBe("Project: my-app");
    });

    it("assemble_renderedContent_containsNoRawPlaceholders", () => {
      createPromptTemplate(
        resourcesDir, "new-feature.prompt.md.j2",
        "{{ project_name }} uses {{ language_name }}",
      );
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const content = fs.readFileSync(
        path.join(outputDir, "github", "prompts", "new-feature.prompt.md"),
        "utf-8",
      );
      expect(content).not.toContain("{{");
      expect(content).not.toContain("}}");
      expect(content).toContain("my-app");
      expect(content).toContain("typescript");
    });

    it("assemble_createsPromptsDirectory", () => {
      createPromptTemplate(
        resourcesDir, "new-feature.prompt.md.j2", "# Test",
      );
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const promptsDir = path.join(outputDir, "github", "prompts");
      expect(fs.existsSync(promptsDir)).toBe(true);
      expect(fs.statSync(promptsDir).isDirectory()).toBe(true);
    });

    it("assemble_usesRenderTemplate_notReplacePlaceholders", () => {
      createPromptTemplate(
        resourcesDir, "new-feature.prompt.md.j2",
        "{% if project_name %}{{ project_name }}{% endif %}",
      );
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const content = fs.readFileSync(
        path.join(outputDir, "github", "prompts", "new-feature.prompt.md"),
        "utf-8",
      );
      expect(content).toBe("my-app");
    });
  });
});
