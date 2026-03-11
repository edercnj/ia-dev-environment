import { describe, it, expect, beforeEach, afterEach } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";
import { tmpdir } from "node:os";
import {
  GithubHooksAssembler,
  GITHUB_HOOK_TEMPLATES,
} from "../../../src/assembler/github-hooks-assembler.js";
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

function createGithubHookTemplate(
  resourcesDir: string,
  filename: string,
  content: string = '{"event": "test"}',
): void {
  const dir = path.join(resourcesDir, "github-hooks-templates");
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(path.join(dir, filename), content, "utf-8");
}

describe("GithubHooksAssembler", () => {
  let tmpDir: string;
  let resourcesDir: string;
  let outputDir: string;
  let assembler: GithubHooksAssembler;

  beforeEach(() => {
    tmpDir = fs.mkdtempSync(
      path.join(tmpdir(), "gh-hooks-asm-test-"),
    );
    resourcesDir = path.join(tmpDir, "resources");
    fs.mkdirSync(resourcesDir, { recursive: true });
    outputDir = path.join(tmpDir, "output");
    fs.mkdirSync(outputDir, { recursive: true });
    assembler = new GithubHooksAssembler();
  });

  afterEach(() => {
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  describe("GITHUB_HOOK_TEMPLATES constant", () => {
    it("contains3Templates", () => {
      expect(GITHUB_HOOK_TEMPLATES).toHaveLength(3);
      expect(GITHUB_HOOK_TEMPLATES).toContain("post-compile-check.json");
      expect(GITHUB_HOOK_TEMPLATES).toContain("pre-commit-lint.json");
      expect(GITHUB_HOOK_TEMPLATES).toContain("session-context-loader.json");
    });
  });

  describe("assemble", () => {
    it("assemble_validConfig_copies3HookFiles", () => {
      for (const template of GITHUB_HOOK_TEMPLATES) {
        createGithubHookTemplate(resourcesDir, template);
      }
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result).toHaveLength(3);
      for (const template of GITHUB_HOOK_TEMPLATES) {
        const dest = path.join(outputDir, "hooks", template);
        expect(fs.existsSync(dest)).toBe(true);
      }
    });

    it("assemble_templatesDirMissing_returnsEmpty", () => {
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result).toEqual([]);
      const hooksDir = path.join(outputDir, "hooks");
      expect(fs.existsSync(hooksDir)).toBe(false);
    });

    it("assemble_individualTemplateFileMissing_skipsIt", () => {
      createGithubHookTemplate(resourcesDir, "post-compile-check.json");
      createGithubHookTemplate(resourcesDir, "pre-commit-lint.json");
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result).toHaveLength(2);
      const missing = path.join(
        outputDir, "hooks", "session-context-loader.json",
      );
      expect(fs.existsSync(missing)).toBe(false);
    });

    it("assemble_createsHooksDirectory_whenNotPreExisting", () => {
      for (const template of GITHUB_HOOK_TEMPLATES) {
        createGithubHookTemplate(resourcesDir, template);
      }
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const hooksDir = path.join(outputDir, "hooks");
      expect(fs.existsSync(hooksDir)).toBe(true);
      expect(fs.statSync(hooksDir).isDirectory()).toBe(true);
    });

    it("assemble_copiedContentMatchesSource", () => {
      const content = '{"event":"post-compile","active":true}';
      createGithubHookTemplate(
        resourcesDir, "post-compile-check.json", content,
      );
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      const copiedContent = fs.readFileSync(result[0]!, "utf-8");
      expect(copiedContent).toBe(content);
    });

    it("assemble_engineParameterNotUsed", () => {
      const content = '{"name": "{project_name}"}';
      createGithubHookTemplate(
        resourcesDir, "post-compile-check.json", content,
      );
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      const copiedContent = fs.readFileSync(result[0]!, "utf-8");
      expect(copiedContent).toContain("{project_name}");
    });
  });
});
