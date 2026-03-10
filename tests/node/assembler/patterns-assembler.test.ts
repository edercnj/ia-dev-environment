import { describe, it, expect, beforeEach, afterEach } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";
import { tmpdir } from "node:os";
import { PatternsAssembler } from "../../../src/assembler/patterns-assembler.js";
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
  style?: string;
  eventDriven?: boolean;
} = {}): ProjectConfig {
  return new ProjectConfig(
    new ProjectIdentity("my-app", "A test application"),
    new ArchitectureConfig(
      overrides.style ?? "microservice",
      false,
      overrides.eventDriven ?? false,
    ),
    [new InterfaceConfig("rest")],
    new LanguageConfig("java", "21"),
    new FrameworkConfig("quarkus", "3.0", "maven"),
  );
}

function createPatternFile(
  resourcesDir: string,
  category: string,
  filename: string,
  content: string = `# ${filename}`,
): void {
  const dir = path.join(resourcesDir, "patterns", category);
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(path.join(dir, filename), content, "utf-8");
}

describe("PatternsAssembler", () => {
  let tmpDir: string;
  let resourcesDir: string;
  let outputDir: string;
  let assembler: PatternsAssembler;

  beforeEach(() => {
    tmpDir = fs.mkdtempSync(
      path.join(tmpdir(), "patterns-asm-test-"),
    );
    resourcesDir = path.join(tmpDir, "resources");
    fs.mkdirSync(resourcesDir, { recursive: true });
    outputDir = path.join(tmpDir, "output");
    fs.mkdirSync(outputDir, { recursive: true });
    assembler = new PatternsAssembler();
  });

  afterEach(() => {
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  describe("assemble", () => {
    it("returnsEmpty_whenUnknownArchitectureStyle", () => {
      const config = buildConfig({ style: "unknown-style" });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result).toEqual([]);
    });

    it("returnsEmpty_whenNoPatternsOnDisk", () => {
      const config = buildConfig({ style: "microservice" });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result).toEqual([]);
    });

    it("selectsUniversalPatterns_forLibraryStyle", () => {
      createPatternFile(resourcesDir, "architectural", "arch.md");
      createPatternFile(resourcesDir, "data", "data.md");
      const config = buildConfig({ style: "library" });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result.length).toBeGreaterThanOrEqual(3);
      expect(result.some((f) => f.includes("architectural"))).toBe(
        true,
      );
      expect(result.some((f) => f.includes("data"))).toBe(true);
    });

    it.each([
      ["microservice", ["architectural", "data", "integration", "microservice", "resilience"]],
      ["hexagonal", ["architectural", "data", "integration"]],
      ["monolith", ["architectural", "data", "integration"]],
      ["library", ["architectural", "data"]],
    ])(
      "style_%s_includesCategories",
      (style, expectedCategories) => {
        for (const cat of expectedCategories) {
          createPatternFile(resourcesDir, cat, `${cat}-guide.md`);
        }
        const config = buildConfig({ style });
        const engine = new TemplateEngine(resourcesDir, config);
        const result = assembler.assemble(
          config, outputDir, resourcesDir, engine,
        );
        for (const cat of expectedCategories) {
          expect(
            result.some((f) => f.includes(cat)),
            `Expected category '${cat}' in results`,
          ).toBe(true);
        }
      },
    );

    it("includesEventDrivenPatterns_whenEnabled", () => {
      createPatternFile(resourcesDir, "architectural", "arch.md");
      createPatternFile(resourcesDir, "data", "data.md");
      createPatternFile(
        resourcesDir, "saga-pattern", "saga.md",
      );
      createPatternFile(
        resourcesDir, "outbox-pattern", "outbox.md",
      );
      createPatternFile(
        resourcesDir, "event-sourcing", "es.md",
      );
      createPatternFile(
        resourcesDir, "dead-letter-queue", "dlq.md",
      );
      const config = buildConfig({
        style: "microservice",
        eventDriven: true,
      });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result.some((f) => f.includes("saga-pattern"))).toBe(
        true,
      );
      expect(
        result.some((f) => f.includes("outbox-pattern")),
      ).toBe(true);
    });

    it("excludesEventDrivenPatterns_whenDisabled", () => {
      createPatternFile(resourcesDir, "architectural", "arch.md");
      createPatternFile(
        resourcesDir, "saga-pattern", "saga.md",
      );
      const config = buildConfig({
        style: "microservice",
        eventDriven: false,
      });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(
        result.some((f) => f.includes("saga-pattern")),
      ).toBe(false);
    });

    it("writesFilesToReferencesWithCategoryStructure", () => {
      createPatternFile(
        resourcesDir, "architectural", "patterns.md",
      );
      createPatternFile(resourcesDir, "data", "data-guide.md");
      const config = buildConfig({ style: "library" });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const archFile = path.join(
        outputDir, "skills", "patterns", "references",
        "architectural", "patterns.md",
      );
      const dataFile = path.join(
        outputDir, "skills", "patterns", "references",
        "data", "data-guide.md",
      );
      expect(fs.existsSync(archFile)).toBe(true);
      expect(fs.existsSync(dataFile)).toBe(true);
    });

    it("createsConsolidatedSkillMd", () => {
      createPatternFile(
        resourcesDir, "architectural", "arch.md", "# Arch",
      );
      createPatternFile(
        resourcesDir, "data", "data.md", "# Data",
      );
      const config = buildConfig({ style: "library" });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const skillMd = path.join(
        outputDir, "skills", "patterns", "SKILL.md",
      );
      expect(fs.existsSync(skillMd)).toBe(true);
      const content = fs.readFileSync(skillMd, "utf-8");
      expect(content).toContain("# Arch");
      expect(content).toContain("# Data");
      expect(content).toContain("\n\n---\n\n");
    });

    it("consolidatedFileJoinsWithSeparator", () => {
      createPatternFile(
        resourcesDir, "architectural", "a.md", "AAA",
      );
      createPatternFile(
        resourcesDir, "data", "b.md", "BBB",
      );
      const config = buildConfig({ style: "library" });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const content = fs.readFileSync(
        path.join(outputDir, "skills", "patterns", "SKILL.md"),
        "utf-8",
      );
      expect(content).toBe("AAA\n\n---\n\nBBB");
    });

    it("singleFile_noSeparatorInConsolidated", () => {
      createPatternFile(
        resourcesDir, "architectural", "only.md", "ONLY",
      );
      const config = buildConfig({ style: "library" });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const content = fs.readFileSync(
        path.join(outputDir, "skills", "patterns", "SKILL.md"),
        "utf-8",
      );
      expect(content).toBe("ONLY");
      expect(content).not.toContain("---");
    });

    it("replacesPlaceholdersViaEngine", () => {
      createPatternFile(
        resourcesDir,
        "architectural",
        "arch.md",
        "Project: {project_name}",
      );
      const config = buildConfig({ style: "library" });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const refContent = fs.readFileSync(
        path.join(
          outputDir, "skills", "patterns", "references",
          "architectural", "arch.md",
        ),
        "utf-8",
      );
      expect(refContent).toBe("Project: my-app");
      const skillContent = fs.readFileSync(
        path.join(outputDir, "skills", "patterns", "SKILL.md"),
        "utf-8",
      );
      expect(skillContent).toBe("Project: my-app");
    });

    it("returnsAllWrittenFilePaths", () => {
      createPatternFile(
        resourcesDir, "architectural", "a.md",
      );
      createPatternFile(resourcesDir, "data", "b.md");
      const config = buildConfig({ style: "library" });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result).toHaveLength(3);
      const refFiles = result.filter((f) =>
        f.includes("references"),
      );
      const skillFile = result.filter((f) =>
        f.endsWith("SKILL.md"),
      );
      expect(refFiles).toHaveLength(2);
      expect(skillFile).toHaveLength(1);
    });

    it("multipleFilesPerCategory_allWritten", () => {
      createPatternFile(
        resourcesDir, "architectural", "a.md", "A",
      );
      createPatternFile(
        resourcesDir, "architectural", "b.md", "B",
      );
      const config = buildConfig({ style: "library" });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      const archFiles = result.filter((f) =>
        f.includes(path.join("references", "architectural")),
      );
      expect(archFiles).toHaveLength(2);
    });
  });
});
