import { describe, it, expect, beforeEach, afterEach } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";
import { tmpdir } from "node:os";
import { RunbookAssembler } from "../../../src/assembler/runbook-assembler.js";
import { TemplateEngine } from "../../../src/template-engine.js";
import {
  ProjectConfig,
  ProjectIdentity,
  ArchitectureConfig,
  InterfaceConfig,
  LanguageConfig,
  FrameworkConfig,
  DataConfig,
  TechComponent,
  InfraConfig,
} from "../../../src/models.js";

function buildConfig(overrides?: {
  container?: string;
  orchestrator?: string;
  databaseName?: string;
}): ProjectConfig {
  const container = overrides?.container ?? "docker";
  const orchestrator = overrides?.orchestrator ?? "kubernetes";
  const databaseName = overrides?.databaseName ?? "postgresql";
  return new ProjectConfig(
    new ProjectIdentity("my-service", "A test service"),
    new ArchitectureConfig("microservice", false, false),
    [new InterfaceConfig("rest")],
    new LanguageConfig("typescript", "5"),
    new FrameworkConfig("nestjs", "10", "npm"),
    new DataConfig(new TechComponent(databaseName)),
    new InfraConfig(container, orchestrator),
  );
}

function copyTemplate(resourcesDir: string): void {
  const srcTemplate = path.resolve(
    __dirname, "../../..",
    "resources/templates/_TEMPLATE-DEPLOY-RUNBOOK.md",
  );
  const destDir = path.join(resourcesDir, "templates");
  fs.mkdirSync(destDir, { recursive: true });
  fs.copyFileSync(
    srcTemplate,
    path.join(destDir, "_TEMPLATE-DEPLOY-RUNBOOK.md"),
  );
}

const OUTPUT_FILE = path.join("docs", "runbook", "deploy-runbook.md");

function assembleAndRead(
  resourcesDir: string,
  outputDir: string,
  assembler: RunbookAssembler,
  overrides?: {
    container?: string;
    orchestrator?: string;
    databaseName?: string;
  },
): string {
  copyTemplate(resourcesDir);
  const config = buildConfig(overrides);
  const engine = new TemplateEngine(resourcesDir, config);
  assembler.assemble(config, outputDir, resourcesDir, engine);
  return fs.readFileSync(
    path.join(outputDir, OUTPUT_FILE), "utf-8",
  );
}

describe("RunbookAssembler", () => {
  let tmpDir: string;
  let resourcesDir: string;
  let outputDir: string;
  let assembler: RunbookAssembler;

  beforeEach(() => {
    tmpDir = fs.mkdtempSync(
      path.join(tmpdir(), "runbook-asm-test-"),
    );
    resourcesDir = path.join(tmpDir, "resources");
    fs.mkdirSync(resourcesDir, { recursive: true });
    outputDir = path.join(tmpDir, "output");
    fs.mkdirSync(outputDir, { recursive: true });
    assembler = new RunbookAssembler();
  });

  afterEach(() => {
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  describe("assemble — happy path", () => {
    // UT-10: Creates runbook directory
    it("assemble_validConfig_createsRunbookDirectory", () => {
      copyTemplate(resourcesDir);
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const runbookDir = path.join(outputDir, "docs", "runbook");
      expect(fs.existsSync(runbookDir)).toBe(true);
      expect(fs.statSync(runbookDir).isDirectory()).toBe(true);
    });

    // UT-11: Creates deploy-runbook.md file
    it("assemble_validConfig_createsDeployRunbookFile", () => {
      copyTemplate(resourcesDir);
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const filePath = path.join(
        outputDir, "docs", "runbook", "deploy-runbook.md",
      );
      expect(fs.existsSync(filePath)).toBe(true);
    });

    // UT-12: Returns generated file path
    it("assemble_validConfig_returnsGeneratedFilePath", () => {
      copyTemplate(resourcesDir);
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result).toHaveLength(1);
      expect(result[0]).toContain(
        path.join("docs", "runbook", "deploy-runbook.md"),
      );
    });

    // UT-13: Renders project name (no raw {{ }})
    it("assemble_validConfig_rendersProjectName", () => {
      const content = assembleAndRead(
        resourcesDir, outputDir, assembler,
      );
      expect(content).toContain("my-service");
      expect(content).not.toContain("{{ project_name }}");
    });
  });

  describe("assemble — conditional rendering", () => {
    // UT-14: Docker+K8s config -> kubectl commands
    it("assemble_dockerKubernetes_containsKubectlCommands", () => {
      const content = assembleAndRead(
        resourcesDir, outputDir, assembler,
        { container: "docker", orchestrator: "kubernetes" },
      );
      expect(content).toContain("kubectl");
    });

    // UT-15: K8s config -> kubectl rollout undo in rollback
    it("assemble_dockerKubernetes_containsKubectlRolloutUndo", () => {
      const content = assembleAndRead(
        resourcesDir, outputDir, assembler,
        { container: "docker", orchestrator: "kubernetes" },
      );
      expect(content).toContain("kubectl rollout undo");
    });

    // UT-16: Docker without K8s -> docker commands, no kubectl
    it("assemble_dockerNoOrchestrator_containsDockerCommands", () => {
      const content = assembleAndRead(
        resourcesDir, outputDir, assembler,
        { container: "docker", orchestrator: "none" },
      );
      expect(content).toContain("docker compose");
      expect(content).not.toContain("kubectl");
    });

    // UT-17: With database -> migration section
    it("assemble_withDatabase_containsMigrationSection", () => {
      const content = assembleAndRead(
        resourcesDir, outputDir, assembler,
        { databaseName: "postgresql" },
      );
      expect(content).toContain("### Database Migration");
    });

    // UT-18: No container -> no docker/kubectl
    it("assemble_noContainer_noDockerOrKubectl", () => {
      const content = assembleAndRead(
        resourcesDir, outputDir, assembler,
        { container: "none", orchestrator: "none" },
      );
      expect(content).not.toContain("docker");
      expect(content).not.toContain("kubectl");
    });

    // UT-19: No database -> no migration section
    it("assemble_noDatabase_noMigrationSection", () => {
      const content = assembleAndRead(
        resourcesDir, outputDir, assembler,
        { databaseName: "none" },
      );
      expect(content).not.toContain("### Database Migration");
    });
  });

  describe("assemble — edge cases", () => {
    // UT-20: Template missing -> returns empty array
    it("assemble_templateMissing_returnsEmptyArray", () => {
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result).toEqual([]);
    });

    // UT-21: Output dir doesn't exist -> creates it
    it("assemble_outputDirNotExist_createsItAutomatically", () => {
      copyTemplate(resourcesDir);
      const newOutputDir = path.join(tmpDir, "new-output");
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, newOutputDir, resourcesDir, engine);
      const filePath = path.join(
        newOutputDir, "docs", "runbook", "deploy-runbook.md",
      );
      expect(fs.existsSync(filePath)).toBe(true);
    });

    // UT-22: All conditions false -> only mandatory sections
    it("assemble_allConditionsFalse_containsOnlyMandatorySections", () => {
      const content = assembleAndRead(
        resourcesDir, outputDir, assembler,
        { container: "none", orchestrator: "none", databaseName: "none" },
      );
      expect(content).toContain("## 1. Service Info");
      expect(content).toContain("## 7. Contacts");
      expect(content).not.toContain("kubectl");
      expect(content).not.toContain("docker");
      expect(content).not.toContain("### Database Migration");
    });

    // UT-23: All conditions true -> all conditional sections
    it("assemble_allConditionsTrue_containsAllConditionalSections", () => {
      const content = assembleAndRead(
        resourcesDir, outputDir, assembler,
        { container: "docker", orchestrator: "kubernetes", databaseName: "postgresql" },
      );
      expect(content).toContain("kubectl");
      expect(content).toContain("kubectl rollout undo");
      expect(content).toContain("### Database Migration");
      expect(content).toContain("### Kubernetes Deployment");
      expect(content).toContain("### Kubernetes Rollback");
    });
  });
});
