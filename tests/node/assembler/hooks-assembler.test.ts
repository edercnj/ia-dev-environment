import { describe, it, expect, beforeEach, afterEach } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";
import { tmpdir } from "node:os";
import { HooksAssembler } from "../../../src/assembler/hooks-assembler.js";
import { TemplateEngine } from "../../../src/template-engine.js";
import {
  ProjectConfig,
  ProjectIdentity,
  ArchitectureConfig,
  InterfaceConfig,
  LanguageConfig,
  FrameworkConfig,
} from "../../../src/models.js";

function buildConfig(overrides: {
  language?: string;
  buildTool?: string;
} = {}): ProjectConfig {
  const lang = overrides.language ?? "java";
  const tool = overrides.buildTool ?? "maven";
  return new ProjectConfig(
    new ProjectIdentity("my-app", "A test application"),
    new ArchitectureConfig("microservice", false, false),
    [new InterfaceConfig("rest")],
    new LanguageConfig(lang, "21"),
    new FrameworkConfig("quarkus", "3.0", tool),
  );
}

function createHookScript(
  resourcesDir: string,
  templateKey: string,
  content: string = "#!/bin/bash\necho compile",
): void {
  const dir = path.join(resourcesDir, "hooks-templates", templateKey);
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(
    path.join(dir, "post-compile-check.sh"),
    content,
    "utf-8",
  );
}

describe("HooksAssembler", () => {
  let tmpDir: string;
  let resourcesDir: string;
  let outputDir: string;
  let assembler: HooksAssembler;

  beforeEach(() => {
    tmpDir = fs.mkdtempSync(
      path.join(tmpdir(), "hooks-asm-test-"),
    );
    resourcesDir = path.join(tmpDir, "resources");
    fs.mkdirSync(resourcesDir, { recursive: true });
    outputDir = path.join(tmpDir, "output");
    fs.mkdirSync(outputDir, { recursive: true });
    assembler = new HooksAssembler();
  });

  afterEach(() => {
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  describe("assemble — compiled languages", () => {
    it.each([
      ["java", "maven", "java-maven"],
      ["java", "gradle", "java-gradle"],
      ["kotlin", "gradle", "kotlin"],
      ["typescript", "npm", "typescript"],
      ["go", "go", "go"],
      ["rust", "cargo", "rust"],
      ["csharp", "dotnet", "csharp"],
    ])(
      "assemble_%s_%s_copiesHookScript",
      (language, buildTool, templateKey) => {
        createHookScript(resourcesDir, templateKey);
        const config = buildConfig({ language, buildTool });
        const engine = new TemplateEngine(resourcesDir, config);
        const result = assembler.assemble(
          config, outputDir, resourcesDir, engine,
        );
        expect(result).toHaveLength(1);
        const dest = path.join(
          outputDir, "hooks", "post-compile-check.sh",
        );
        expect(result[0]).toBe(dest);
        expect(fs.existsSync(dest)).toBe(true);
      },
    );

    it("assemble_javaWithMaven_copiedContentMatchesSource", () => {
      const scriptContent = "#!/bin/bash\nset -e\nmvn compile";
      createHookScript(resourcesDir, "java-maven", scriptContent);
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      const copiedContent = fs.readFileSync(result[0]!, "utf-8");
      expect(copiedContent).toBe(scriptContent);
    });
  });

  describe("assemble — interpreted languages (no hook)", () => {
    it("assemble_pythonWithPip_returnsEmptyArray", () => {
      const config = buildConfig({
        language: "python",
        buildTool: "pip",
      });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result).toEqual([]);
      const hooksDir = path.join(outputDir, "hooks");
      expect(fs.existsSync(hooksDir)).toBe(false);
    });

    it("assemble_unknownLanguage_returnsEmptyArray", () => {
      const config = buildConfig({
        language: "ruby",
        buildTool: "bundler",
      });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result).toEqual([]);
    });
  });

  describe("assemble — file permissions", () => {
    it("assemble_hookFileIsExecutable_chmod755Applied", () => {
      createHookScript(resourcesDir, "java-maven");
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      const stat = fs.statSync(result[0]!);
      const mode = stat.mode & 0o777;
      expect(mode).toBe(0o755);
    });
  });

  describe("assemble — directory creation", () => {
    it("assemble_createsHooksDirectory_whenNotPreExisting", () => {
      createHookScript(resourcesDir, "java-maven");
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const hooksDir = path.join(outputDir, "hooks");
      expect(fs.existsSync(hooksDir)).toBe(true);
      expect(fs.statSync(hooksDir).isDirectory()).toBe(true);
    });
  });

  describe("assemble — missing template file", () => {
    it("assemble_templateKeyExistsButFileAbsent_returnsEmptyArray", () => {
      const dir = path.join(
        resourcesDir, "hooks-templates", "java-maven",
      );
      fs.mkdirSync(dir, { recursive: true });
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result).toEqual([]);
    });
  });
});
