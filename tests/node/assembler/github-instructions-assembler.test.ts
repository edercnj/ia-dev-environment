import { describe, it, expect, beforeEach, afterEach } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";
import { tmpdir } from "node:os";
import {
  GithubInstructionsAssembler,
  buildCopilotInstructions,
  CONTEXTUAL_INSTRUCTIONS,
} from "../../../src/assembler/github-instructions-assembler.js";
import { TemplateEngine } from "../../../src/template-engine.js";
import {
  ProjectConfig,
  ProjectIdentity,
  ArchitectureConfig,
  InterfaceConfig,
  LanguageConfig,
  FrameworkConfig,
  InfraConfig,
  TestingConfig,
} from "../../../src/models.js";

function buildConfig(overrides: {
  interfaces?: InterfaceConfig[];
  frameworkVersion?: string;
  domainDriven?: boolean;
  eventDriven?: boolean;
  smokeTests?: boolean;
  contractTests?: boolean;
  container?: string;
  nativeBuild?: boolean;
} = {}): ProjectConfig {
  return new ProjectConfig(
    new ProjectIdentity("my-app", "A test application"),
    new ArchitectureConfig(
      "microservice",
      overrides.domainDriven ?? false,
      overrides.eventDriven ?? false,
    ),
    overrides.interfaces ?? [new InterfaceConfig("rest")],
    new LanguageConfig("typescript", "5"),
    new FrameworkConfig(
      "commander",
      overrides.frameworkVersion ?? "",
      "npm",
      overrides.nativeBuild ?? false,
    ),
    undefined,
    new InfraConfig(
      overrides.container ?? "docker",
      "none",
    ),
    undefined,
    new TestingConfig(
      overrides.smokeTests ?? true,
      overrides.contractTests ?? false,
    ),
  );
}

function createInstructionTemplate(
  resourcesDir: string,
  templateName: string,
  content: string = "# Template\n{project_name}",
): void {
  const dir = path.join(
    resourcesDir, "github-instructions-templates",
  );
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(
    path.join(dir, templateName), content, "utf-8",
  );
}

describe("buildCopilotInstructions", () => {
  it("fullConfig_containsProjectName", () => {
    const config = buildConfig();
    const output = buildCopilotInstructions(config);
    expect(output).toContain("my-app");
  });

  it("restInterface_uppercased", () => {
    const config = buildConfig({
      interfaces: [new InterfaceConfig("rest")],
    });
    const output = buildCopilotInstructions(config);
    expect(output).toContain("REST");
  });

  it("grpcInterface_uppercased", () => {
    const config = buildConfig({
      interfaces: [new InterfaceConfig("grpc")],
    });
    const output = buildCopilotInstructions(config);
    expect(output).toContain("GRPC");
  });

  it("cliInterface_lowercase", () => {
    const config = buildConfig({
      interfaces: [new InterfaceConfig("cli")],
    });
    const output = buildCopilotInstructions(config);
    expect(output).toContain("cli");
  });

  it("multipleInterfaces_commaSeparated", () => {
    const config = buildConfig({
      interfaces: [
        new InterfaceConfig("rest"),
        new InterfaceConfig("grpc"),
      ],
    });
    const output = buildCopilotInstructions(config);
    expect(output).toContain("REST, GRPC");
  });

  it("noInterfaces_showsNone", () => {
    const config = buildConfig({ interfaces: [] });
    const output = buildCopilotInstructions(config);
    expect(output).toContain("**Interfaces:** none");
  });

  it("frameworkVersionPresent_spacePrefix", () => {
    const config = buildConfig({ frameworkVersion: "3.0" });
    const output = buildCopilotInstructions(config);
    expect(output).toContain("commander 3.0");
  });

  it("frameworkVersionEmpty_noSpace", () => {
    const config = buildConfig({ frameworkVersion: "" });
    const output = buildCopilotInstructions(config);
    expect(output).toContain("**Framework:** commander\n");
    expect(output).not.toContain("commander \n");
  });

  it("booleans_lowercase", () => {
    const config = buildConfig({ domainDriven: true });
    const output = buildCopilotInstructions(config);
    expect(output).toContain("true");
    expect(output).not.toContain("True");
  });

  it("containsStackTable", () => {
    const config = buildConfig();
    const output = buildCopilotInstructions(config);
    expect(output).toContain("| Layer | Technology |");
    expect(output).toContain("Architecture");
    expect(output).toContain("Language");
    expect(output).toContain("Framework");
    expect(output).toContain("Build Tool");
  });

  it("containsConstraints", () => {
    const config = buildConfig();
    const output = buildCopilotInstructions(config);
    expect(output).toContain("Cloud-Agnostic");
    expect(output).toContain("stateless");
    expect(output).toContain("Externalized configuration");
  });

  it("containsContextualReferences", () => {
    const config = buildConfig();
    const output = buildCopilotInstructions(config);
    expect(output).toContain("domain.instructions.md");
    expect(output).toContain("coding-standards.instructions.md");
    expect(output).toContain("architecture.instructions.md");
    expect(output).toContain("quality-gates.instructions.md");
  });

  it("trailingNewline", () => {
    const config = buildConfig();
    const output = buildCopilotInstructions(config);
    expect(output.endsWith("\n")).toBe(true);
  });
});

describe("GithubInstructionsAssembler", () => {
  let tmpDir: string;
  let resourcesDir: string;
  let outputDir: string;
  let assembler: GithubInstructionsAssembler;

  beforeEach(() => {
    tmpDir = fs.mkdtempSync(
      path.join(tmpdir(), "gh-instr-asm-test-"),
    );
    resourcesDir = path.join(tmpDir, "resources");
    fs.mkdirSync(resourcesDir, { recursive: true });
    outputDir = path.join(tmpDir, "output");
    fs.mkdirSync(outputDir, { recursive: true });
    assembler = new GithubInstructionsAssembler();
  });

  afterEach(() => {
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  describe("assemble", () => {
    it("assemble_generatesGlobalFile", () => {
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      const globalFile = path.join(
        outputDir, "copilot-instructions.md",
      );
      expect(result).toContain(globalFile);
      expect(fs.existsSync(globalFile)).toBe(true);
      const content = fs.readFileSync(globalFile, "utf-8");
      expect(content).toBe(buildCopilotInstructions(config));
    });

    it("assemble_generatesContextualFiles_whenTemplatesExist", () => {
      for (const name of CONTEXTUAL_INSTRUCTIONS) {
        createInstructionTemplate(
          resourcesDir, `${name}.md`, `# ${name}\n{project_name}`,
        );
      }
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result).toHaveLength(5);
      for (const name of CONTEXTUAL_INSTRUCTIONS) {
        const dest = path.join(
          outputDir, "instructions",
          `${name}.instructions.md`,
        );
        expect(fs.existsSync(dest)).toBe(true);
      }
    });

    it("assemble_skipsContextualFile_whenTemplateFileMissing", () => {
      createInstructionTemplate(resourcesDir, "domain.md");
      createInstructionTemplate(resourcesDir, "architecture.md");
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result).toHaveLength(3);
    });

    it("assemble_templatesDirMissing_onlyGeneratesGlobal", () => {
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result).toHaveLength(1);
      expect(result[0]).toContain("copilot-instructions.md");
    });

    it("assemble_contextualFilesApplyPlaceholderReplacement", () => {
      createInstructionTemplate(
        resourcesDir, "domain.md", "Project: {project_name}",
      );
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const content = fs.readFileSync(
        path.join(
          outputDir, "instructions",
          "domain.instructions.md",
        ),
        "utf-8",
      );
      expect(content).toBe("Project: my-app");
    });

    it("assemble_createsDirectoryStructure", () => {
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      expect(fs.existsSync(outputDir)).toBe(true);
      expect(fs.existsSync(
        path.join(outputDir, "instructions"),
      )).toBe(true);
    });

    it("assemble_returnsAllGeneratedPaths", () => {
      for (const name of CONTEXTUAL_INSTRUCTIONS) {
        createInstructionTemplate(
          resourcesDir, `${name}.md`,
        );
      }
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result).toHaveLength(5);
    });
  });
});
