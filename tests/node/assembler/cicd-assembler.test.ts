import { describe, it, expect, beforeEach, afterEach } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";
import { tmpdir } from "node:os";
import { CicdAssembler } from "../../../src/assembler/cicd-assembler.js";
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
import type { AssembleResult } from "../../../src/assembler/rules-assembler.js";

const REAL_RESOURCES_DIR = path.resolve(
  __dirname, "../../../resources",
);

function buildConfig(overrides: {
  language?: string;
  buildTool?: string;
  framework?: string;
  languageVersion?: string;
  container?: string;
  orchestrator?: string;
  smokeTests?: boolean;
  projectName?: string;
} = {}): ProjectConfig {
  return new ProjectConfig(
    new ProjectIdentity(
      overrides.projectName ?? "my-app",
      "A test application",
    ),
    new ArchitectureConfig("microservice", false, false),
    [new InterfaceConfig("rest")],
    new LanguageConfig(
      overrides.language ?? "typescript",
      overrides.languageVersion ?? "5",
    ),
    new FrameworkConfig(
      overrides.framework ?? "nestjs",
      "3.0",
      overrides.buildTool ?? "npm",
    ),
    undefined,
    new InfraConfig(
      overrides.container ?? "docker",
      overrides.orchestrator ?? "kubernetes",
    ),
    undefined,
    new TestingConfig(
      overrides.smokeTests ?? true,
    ),
  );
}

describe("CicdAssembler", () => {
  let tmpDir: string;
  let outputDir: string;
  let assembler: CicdAssembler;

  beforeEach(() => {
    tmpDir = fs.mkdtempSync(
      path.join(tmpdir(), "cicd-asm-test-"),
    );
    outputDir = path.join(tmpDir, "output");
    fs.mkdirSync(outputDir, { recursive: true });
    assembler = new CicdAssembler();
  });

  afterEach(() => {
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  describe("always-generated artifacts", () => {
    it("assemble_minimalConfig_generatesCiWorkflow", () => {
      const config = buildConfig({
        container: "none",
        orchestrator: "none",
        smokeTests: false,
      });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      const result = assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      ) as AssembleResult;
      const ciPath = path.join(
        outputDir, ".github", "workflows", "ci.yml",
      );
      expect(result.files).toContain(ciPath);
      expect(fs.existsSync(ciPath)).toBe(true);
    });

    it("assemble_minimalConfig_resultIsAssembleResult", () => {
      const config = buildConfig({
        container: "none",
        orchestrator: "none",
        smokeTests: false,
      });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      const result = assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      ) as AssembleResult;
      expect(result).toHaveProperty("files");
      expect(result).toHaveProperty("warnings");
      expect(Array.isArray(result.files)).toBe(true);
      expect(Array.isArray(result.warnings)).toBe(true);
    });
  });

  describe("conditional — Docker artifacts", () => {
    it("assemble_containerDocker_generatesDockerfile", () => {
      const config = buildConfig({ container: "docker" });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      const result = assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      ) as AssembleResult;
      const dockerfilePath = path.join(outputDir, "Dockerfile");
      expect(result.files).toContain(dockerfilePath);
      expect(fs.existsSync(dockerfilePath)).toBe(true);
    });

    it("assemble_containerDocker_generatesDockerCompose", () => {
      const config = buildConfig({ container: "docker" });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      const result = assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      ) as AssembleResult;
      const composePath = path.join(
        outputDir, "docker-compose.yml",
      );
      expect(result.files).toContain(composePath);
      expect(fs.existsSync(composePath)).toBe(true);
    });

    it("assemble_containerNone_skipsDockerfile", () => {
      const config = buildConfig({ container: "none" });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      const result = assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      ) as AssembleResult;
      const dockerfilePath = path.join(outputDir, "Dockerfile");
      expect(result.files).not.toContain(dockerfilePath);
      expect(fs.existsSync(dockerfilePath)).toBe(false);
    });

    it("assemble_containerNone_skipsDockerCompose", () => {
      const config = buildConfig({ container: "none" });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      const result = assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      ) as AssembleResult;
      const composePath = path.join(
        outputDir, "docker-compose.yml",
      );
      expect(result.files).not.toContain(composePath);
      expect(fs.existsSync(composePath)).toBe(false);
    });
  });

  describe("conditional — Kubernetes manifests", () => {
    it("assemble_orchestratorKubernetes_generatesDeployment", () => {
      const config = buildConfig({ orchestrator: "kubernetes" });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      const result = assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      ) as AssembleResult;
      const deployPath = path.join(
        outputDir, "k8s", "deployment.yaml",
      );
      expect(result.files).toContain(deployPath);
      expect(fs.existsSync(deployPath)).toBe(true);
    });

    it("assemble_orchestratorKubernetes_generatesService", () => {
      const config = buildConfig({ orchestrator: "kubernetes" });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      const result = assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      ) as AssembleResult;
      const svcPath = path.join(
        outputDir, "k8s", "service.yaml",
      );
      expect(result.files).toContain(svcPath);
      expect(fs.existsSync(svcPath)).toBe(true);
    });

    it("assemble_orchestratorKubernetes_generatesConfigMap", () => {
      const config = buildConfig({ orchestrator: "kubernetes" });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      const result = assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      ) as AssembleResult;
      const cmPath = path.join(
        outputDir, "k8s", "configmap.yaml",
      );
      expect(result.files).toContain(cmPath);
      expect(fs.existsSync(cmPath)).toBe(true);
    });

    it("assemble_orchestratorNone_skipsK8sManifests", () => {
      const config = buildConfig({ orchestrator: "none" });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      const result = assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      ) as AssembleResult;
      const k8sDir = path.join(outputDir, "k8s");
      const hasK8s = result.files.some(
        (f) => f.includes(path.join("k8s", "")),
      );
      expect(hasK8s).toBe(false);
      expect(fs.existsSync(k8sDir)).toBe(false);
    });
  });

  describe("conditional — smoke tests", () => {
    it("assemble_smokeTestsTrue_generatesSmokeConfig", () => {
      const config = buildConfig({ smokeTests: true });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      const result = assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      ) as AssembleResult;
      const smokePath = path.join(
        outputDir, "tests", "smoke", "smoke-config.md",
      );
      expect(result.files).toContain(smokePath);
      expect(fs.existsSync(smokePath)).toBe(true);
    });

    it("assemble_smokeTestsFalse_skipsSmokeConfig", () => {
      const config = buildConfig({ smokeTests: false });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      const result = assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      ) as AssembleResult;
      const smokeDir = path.join(outputDir, "tests", "smoke");
      const hasSmoke = result.files.some(
        (f) => f.includes(path.join("smoke", "")),
      );
      expect(hasSmoke).toBe(false);
      expect(fs.existsSync(smokeDir)).toBe(false);
    });
  });

  describe("full config — all artifacts", () => {
    it("assemble_fullConfig_generatesAllArtifacts", () => {
      const config = buildConfig({
        container: "docker",
        orchestrator: "kubernetes",
        smokeTests: true,
      });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      const result = assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      ) as AssembleResult;
      const expectedMinFiles = 7;
      expect(result.files.length).toBeGreaterThanOrEqual(
        expectedMinFiles,
      );
    });

    it("assemble_fullConfig_noWarnings", () => {
      const config = buildConfig({
        container: "docker",
        orchestrator: "kubernetes",
        smokeTests: true,
      });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      const result = assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      ) as AssembleResult;
      expect(result.warnings).toEqual([]);
    });
  });

  describe("partial config — Docker without K8s", () => {
    it("assemble_dockerNoK8s_generatesDockerSkipsK8s", () => {
      const config = buildConfig({
        container: "docker",
        orchestrator: "none",
        smokeTests: true,
      });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      const result = assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      ) as AssembleResult;
      expect(
        result.files.some((f) => f.endsWith("Dockerfile")),
      ).toBe(true);
      expect(
        result.files.some(
          (f) => f.includes(path.join("k8s", "")),
        ),
      ).toBe(false);
    });

    it("assemble_dockerNoK8s_warnsAboutK8sSkip", () => {
      const config = buildConfig({
        container: "docker",
        orchestrator: "none",
        smokeTests: true,
      });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      const result = assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      ) as AssembleResult;
      expect(
        result.warnings.some((w) => w.toLowerCase().includes("k8s")
          || w.toLowerCase().includes("kubernetes")),
      ).toBe(true);
    });
  });

  describe("minimal config — all disabled", () => {
    it("assemble_allDisabled_onlyGeneratesCiWorkflow", () => {
      const config = buildConfig({
        container: "none",
        orchestrator: "none",
        smokeTests: false,
      });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      const result = assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      ) as AssembleResult;
      const expectedFileCount = 1;
      expect(result.files).toHaveLength(expectedFileCount);
    });

    it("assemble_allDisabled_warnsAboutSkippedArtifacts", () => {
      const config = buildConfig({
        container: "none",
        orchestrator: "none",
        smokeTests: false,
      });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      const result = assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      ) as AssembleResult;
      const minWarnings = 3;
      expect(result.warnings.length).toBeGreaterThanOrEqual(
        minWarnings,
      );
    });
  });

  describe("template variable substitution", () => {
    it("assemble_typescriptNpm_ciContainsNodeSetup", () => {
      const config = buildConfig({
        language: "typescript",
        buildTool: "npm",
      });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      );
      const content = fs.readFileSync(
        path.join(outputDir, ".github", "workflows", "ci.yml"),
        "utf-8",
      );
      expect(content).toContain("Node.js");
    });

    it("assemble_typescriptNpm_dockerfileContainsNodeImage", () => {
      const config = buildConfig({
        language: "typescript",
        buildTool: "npm",
        container: "docker",
      });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      );
      const content = fs.readFileSync(
        path.join(outputDir, "Dockerfile"),
        "utf-8",
      );
      expect(content).toContain("node:");
    });

    it("assemble_javaGradle_ciContainsJavaSetup", () => {
      const config = buildConfig({
        language: "java",
        buildTool: "gradle",
        framework: "spring-boot",
        languageVersion: "21",
      });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      );
      const content = fs.readFileSync(
        path.join(outputDir, ".github", "workflows", "ci.yml"),
        "utf-8",
      );
      expect(content).toContain("JDK");
      expect(content).toContain("gradle");
    });

    it("assemble_javaGradle_dockerfileContainsTemurin", () => {
      const config = buildConfig({
        language: "java",
        buildTool: "gradle",
        framework: "spring-boot",
        languageVersion: "21",
        container: "docker",
      });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      );
      const content = fs.readFileSync(
        path.join(outputDir, "Dockerfile"),
        "utf-8",
      );
      expect(content).toContain("eclipse-temurin");
    });

    it("assemble_goGo_ciContainsGoSetup", () => {
      const config = buildConfig({
        language: "go",
        buildTool: "go",
        framework: "gin",
        languageVersion: "1.22",
      });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      );
      const content = fs.readFileSync(
        path.join(outputDir, ".github", "workflows", "ci.yml"),
        "utf-8",
      );
      expect(content).toContain("Go");
    });

    it("assemble_pythonPip_ciContainsPythonSetup", () => {
      const config = buildConfig({
        language: "python",
        buildTool: "pip",
        framework: "fastapi",
        languageVersion: "3.12",
      });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      );
      const content = fs.readFileSync(
        path.join(outputDir, ".github", "workflows", "ci.yml"),
        "utf-8",
      );
      expect(content).toContain("Python");
    });

    it("assemble_rustCargo_ciContainsRustSetup", () => {
      const config = buildConfig({
        language: "rust",
        buildTool: "cargo",
        framework: "axum",
        languageVersion: "1.77",
      });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      );
      const content = fs.readFileSync(
        path.join(outputDir, ".github", "workflows", "ci.yml"),
        "utf-8",
      );
      expect(content).toContain("Rust");
    });

    it("assemble_kotlinGradle_ciContainsKotlinSetup", () => {
      const config = buildConfig({
        language: "kotlin",
        buildTool: "gradle",
        framework: "ktor",
        languageVersion: "21",
      });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      );
      const content = fs.readFileSync(
        path.join(outputDir, ".github", "workflows", "ci.yml"),
        "utf-8",
      );
      expect(content).toContain("JDK");
    });

    it("assemble_anyConfig_ciNoRawNunjucksPlaceholders", () => {
      const config = buildConfig();
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      );
      const content = fs.readFileSync(
        path.join(outputDir, ".github", "workflows", "ci.yml"),
        "utf-8",
      );
      expect(content).toContain("my-app");
      expect(content).not.toContain("{{ project_name }}");
      expect(content).not.toContain("{{ language_name }}");
    });

    it("assemble_anyConfig_dockerfileNoRawPlaceholders", () => {
      const config = buildConfig({ container: "docker" });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      );
      const content = fs.readFileSync(
        path.join(outputDir, "Dockerfile"),
        "utf-8",
      );
      expect(content).not.toContain("{{ ");
      expect(content).not.toContain(" }}");
    });

  });

  describe("K8s template variable substitution", () => {
    it("assemble_k8sDeployment_containsProjectName", () => {
      const config = buildConfig({
        orchestrator: "kubernetes",
        projectName: "my-app",
      });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      );
      const content = fs.readFileSync(
        path.join(outputDir, "k8s", "deployment.yaml"),
        "utf-8",
      );
      expect(content).toContain("my-app");
    });

    it("assemble_k8sDeployment_containsContainerPort", () => {
      const config = buildConfig({
        orchestrator: "kubernetes",
        framework: "nestjs",
      });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      );
      const content = fs.readFileSync(
        path.join(outputDir, "k8s", "deployment.yaml"),
        "utf-8",
      );
      expect(content).toContain("3000");
    });

    it("assemble_k8sDeployment_containsHealthCheck", () => {
      const config = buildConfig({
        orchestrator: "kubernetes",
        framework: "spring-boot",
        language: "java",
        buildTool: "gradle",
        languageVersion: "21",
      });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      );
      const content = fs.readFileSync(
        path.join(outputDir, "k8s", "deployment.yaml"),
        "utf-8",
      );
      expect(content).toContain("/actuator/health");
    });

    it("assemble_k8sService_containsProjectName", () => {
      const config = buildConfig({
        orchestrator: "kubernetes",
        projectName: "my-app",
      });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      );
      const content = fs.readFileSync(
        path.join(outputDir, "k8s", "service.yaml"),
        "utf-8",
      );
      expect(content).toContain("my-app");
    });
  });

  describe("Dockerfile content", () => {
    it("assemble_dockerfile_containsMultiStageBuild", () => {
      const config = buildConfig({ container: "docker" });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      );
      const content = fs.readFileSync(
        path.join(outputDir, "Dockerfile"),
        "utf-8",
      );
      const fromCount = (content.match(/^FROM /gm) ?? []).length;
      const minFromDirectives = 2;
      expect(fromCount).toBeGreaterThanOrEqual(minFromDirectives);
    });

    it("assemble_dockerfile_containsHealthCheck", () => {
      const config = buildConfig({ container: "docker" });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      );
      const content = fs.readFileSync(
        path.join(outputDir, "Dockerfile"),
        "utf-8",
      );
      expect(content).toContain("HEALTHCHECK");
    });
  });

  describe("directory creation", () => {
    it("assemble_createsWorkflowsDirectory", () => {
      const config = buildConfig();
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      );
      const workflowsDir = path.join(
        outputDir, ".github", "workflows",
      );
      expect(fs.existsSync(workflowsDir)).toBe(true);
    });

    it("assemble_createsK8sDir_whenK8sEnabled", () => {
      const config = buildConfig({ orchestrator: "kubernetes" });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      );
      const k8sDir = path.join(outputDir, "k8s");
      expect(fs.existsSync(k8sDir)).toBe(true);
    });

    it("assemble_createsSmokeDir_whenEnabled", () => {
      const config = buildConfig({ smokeTests: true });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      );
      const smokeDir = path.join(outputDir, "tests", "smoke");
      expect(fs.existsSync(smokeDir)).toBe(true);
    });
  });

  describe("edge cases — missing templates and fallbacks", () => {
    it("assemble_ciTemplatesMissing_returnsWithWarning", () => {
      const emptyResDir = path.join(tmpDir, "empty-resources");
      fs.mkdirSync(emptyResDir, { recursive: true });
      const config = buildConfig({
        container: "none",
        orchestrator: "none",
        smokeTests: false,
      });
      const engine = new TemplateEngine(emptyResDir, config);
      const result = assembler.assemble(
        config, outputDir, emptyResDir, engine,
      ) as AssembleResult;
      expect(result.warnings.length).toBeGreaterThan(0);
    });

    it("assemble_unknownFramework_usesDefaultPort", () => {
      const config = buildConfig({
        framework: "unknown-fw",
        orchestrator: "kubernetes",
      });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      );
      const content = fs.readFileSync(
        path.join(outputDir, "k8s", "deployment.yaml"),
        "utf-8",
      );
      expect(content).toContain("8080");
    });

    it("assemble_unknownFramework_usesDefaultHealthPath", () => {
      const config = buildConfig({
        framework: "unknown-fw",
        orchestrator: "kubernetes",
      });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      );
      const content = fs.readFileSync(
        path.join(outputDir, "k8s", "deployment.yaml"),
        "utf-8",
      );
      expect(content).toContain("/health");
    });
  });

  describe("per-stack parametrized", () => {
    const STACKS: Array<[string, string, string, string]> = [
      ["java", "maven", "java-maven", "21"],
      ["java", "gradle", "java-gradle", "21"],
      ["kotlin", "gradle", "kotlin-gradle", "21"],
      ["typescript", "npm", "typescript-npm", "5"],
      ["python", "pip", "python-pip", "3.12"],
      ["go", "go", "go-go", "1.22"],
      ["rust", "cargo", "rust-cargo", "1.77"],
    ];

    it.each(STACKS)(
      "assemble_%s_%s_generatesCorrectDockerfile",
      (language, buildTool, stackKey, langVersion) => {
        const config = buildConfig({
          language,
          buildTool,
          languageVersion: langVersion,
          container: "docker",
          framework: "nestjs",
        });
        const engine = new TemplateEngine(
          REAL_RESOURCES_DIR, config,
        );
        assembler.assemble(
          config, outputDir, REAL_RESOURCES_DIR, engine,
        );
        const dockerfilePath = path.join(
          outputDir, "Dockerfile",
        );
        expect(fs.existsSync(dockerfilePath)).toBe(true);
        const content = fs.readFileSync(
          dockerfilePath, "utf-8",
        );
        expect(content).not.toContain("{{ ");
        expect(content).not.toContain(" }}");
      },
    );

    it.each(STACKS)(
      "assemble_%s_%s_generatesCorrectCiWorkflow",
      (language, buildTool, _stackKey, langVersion) => {
        const config = buildConfig({
          language,
          buildTool,
          languageVersion: langVersion,
          framework: "nestjs",
        });
        const engine = new TemplateEngine(
          REAL_RESOURCES_DIR, config,
        );
        assembler.assemble(
          config, outputDir, REAL_RESOURCES_DIR, engine,
        );
        const ciPath = path.join(
          outputDir, ".github", "workflows", "ci.yml",
        );
        expect(fs.existsSync(ciPath)).toBe(true);
        const content = fs.readFileSync(ciPath, "utf-8");
        expect(content).not.toContain("{{ project_name }}");
        expect(content).not.toContain("{{ language_name }}");
      },
    );
  });

  describe("review fix — lint, security context, image scan", () => {
    it("assemble_ciWorkflow_containsLintStep", () => {
      const config = buildConfig();
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      );
      const content = fs.readFileSync(
        path.join(outputDir, ".github", "workflows", "ci.yml"),
        "utf-8",
      );
      expect(content).toContain("Lint");
      expect(content).toContain("npm run lint");
    });

    it("assemble_ciWorkflow_containsImageScanWhenDocker", () => {
      const config = buildConfig({ container: "docker" });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      );
      const content = fs.readFileSync(
        path.join(outputDir, ".github", "workflows", "ci.yml"),
        "utf-8",
      );
      expect(content).toContain("image-scan");
      expect(content).toContain("trivy");
    });

    it("assemble_ciWorkflow_noImageScanWhenNoDocker", () => {
      const config = buildConfig({ container: "none" });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      );
      const content = fs.readFileSync(
        path.join(outputDir, ".github", "workflows", "ci.yml"),
        "utf-8",
      );
      expect(content).not.toContain("image-scan");
    });

    it("assemble_k8sDeployment_containsSecurityContext", () => {
      const config = buildConfig({ orchestrator: "kubernetes" });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      );
      const content = fs.readFileSync(
        path.join(outputDir, "k8s", "deployment.yaml"),
        "utf-8",
      );
      expect(content).toContain("runAsNonRoot: true");
      expect(content).toContain("allowPrivilegeEscalation: false");
      expect(content).toContain("readOnlyRootFilesystem: true");
      expect(content).toContain("seccompProfile");
    });

    it("assemble_k8sDeployment_dropsAllCapabilities", () => {
      const config = buildConfig({ orchestrator: "kubernetes" });
      const engine = new TemplateEngine(REAL_RESOURCES_DIR, config);
      assembler.assemble(
        config, outputDir, REAL_RESOURCES_DIR, engine,
      );
      const content = fs.readFileSync(
        path.join(outputDir, "k8s", "deployment.yaml"),
        "utf-8",
      );
      expect(content).toContain("drop:");
      expect(content).toContain("- ALL");
    });

    it("assemble_perStack_ciWorkflowContainsLintCmd", () => {
      const stacks = [
        { lang: "java", bt: "gradle", lv: "21", lint: "spotlessCheck" },
        { lang: "python", bt: "pip", lv: "3.12", lint: "ruff check" },
        { lang: "rust", bt: "cargo", lv: "1.75", lint: "cargo clippy" },
        { lang: "go", bt: "go", lv: "1.22", lint: "golangci-lint" },
      ];
      for (const s of stacks) {
        const config = buildConfig({
          language: s.lang,
          buildTool: s.bt,
          languageVersion: s.lv,
        });
        const engine = new TemplateEngine(
          REAL_RESOURCES_DIR, config,
        );
        const out = path.join(outputDir, s.lang);
        fs.mkdirSync(out, { recursive: true });
        assembler.assemble(
          config, out, REAL_RESOURCES_DIR, engine,
        );
        const content = fs.readFileSync(
          path.join(out, ".github", "workflows", "ci.yml"),
          "utf-8",
        );
        expect(content).toContain(s.lint);
      }
    });
  });
});
