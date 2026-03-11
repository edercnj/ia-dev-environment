import { describe, it, expect, beforeEach, afterEach } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";
import { tmpdir } from "node:os";
import {
  GithubAgentsAssembler,
  selectGithubConditionalAgents,
} from "../../../src/assembler/github-agents-assembler.js";
import { TemplateEngine } from "../../../src/template-engine.js";
import {
  ProjectConfig,
  ProjectIdentity,
  ArchitectureConfig,
  InterfaceConfig,
  LanguageConfig,
  FrameworkConfig,
  InfraConfig,
} from "../../../src/models.js";

function buildConfig(overrides: {
  interfaces?: InterfaceConfig[];
  container?: string;
  orchestrator?: string;
  iac?: string;
  serviceMesh?: string;
  eventDriven?: boolean;
  language?: string;
} = {}): ProjectConfig {
  return new ProjectConfig(
    new ProjectIdentity("my-app", "A test application"),
    new ArchitectureConfig(
      "microservice", false, overrides.eventDriven ?? false,
    ),
    overrides.interfaces ?? [new InterfaceConfig("cli")],
    new LanguageConfig(overrides.language ?? "typescript", "5"),
    new FrameworkConfig("commander", "", "npm"),
    undefined,
    new InfraConfig(
      overrides.container ?? "none",
      overrides.orchestrator ?? "none",
      "kustomize",
      overrides.iac ?? "none",
      "none",
      "none",
      overrides.serviceMesh ?? "none",
    ),
  );
}

function createGithubCoreAgent(
  resourcesDir: string,
  agentName: string,
  content?: string,
): void {
  const dir = path.join(
    resourcesDir, "github-agents-templates", "core",
  );
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(
    path.join(dir, agentName),
    content ?? `# ${agentName}\nProject: {project_name}`,
  );
}

function createGithubConditionalAgent(
  resourcesDir: string,
  agentName: string,
  content?: string,
): void {
  const dir = path.join(
    resourcesDir, "github-agents-templates", "conditional",
  );
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(
    path.join(dir, agentName),
    content ?? `# ${agentName}\nProject: {project_name}`,
  );
}

function createGithubDeveloperAgent(
  resourcesDir: string,
  agentName: string,
  content?: string,
): void {
  const dir = path.join(
    resourcesDir, "github-agents-templates", "developers",
  );
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(
    path.join(dir, agentName),
    content ?? `# ${agentName}\nProject: {project_name}`,
  );
}

describe("selectGithubConditionalAgents", () => {
  it("containerDocker_includesDevops", () => {
    const config = buildConfig({ container: "docker" });
    expect(selectGithubConditionalAgents(config)).toContain(
      "devops-engineer.md",
    );
  });

  it("allInfraNone_excludesDevops", () => {
    const config = buildConfig({
      container: "none", orchestrator: "none",
      iac: "none", serviceMesh: "none",
    });
    expect(selectGithubConditionalAgents(config)).not.toContain(
      "devops-engineer.md",
    );
  });

  it("restInterface_includesApiEngineer", () => {
    const config = buildConfig({
      interfaces: [new InterfaceConfig("rest")],
    });
    expect(selectGithubConditionalAgents(config)).toContain(
      "api-engineer.md",
    );
  });

  it("grpcInterface_includesApiEngineer", () => {
    const config = buildConfig({
      interfaces: [new InterfaceConfig("grpc")],
    });
    expect(selectGithubConditionalAgents(config)).toContain(
      "api-engineer.md",
    );
  });

  it("graphqlInterface_includesApiEngineer", () => {
    const config = buildConfig({
      interfaces: [new InterfaceConfig("graphql")],
    });
    expect(selectGithubConditionalAgents(config)).toContain(
      "api-engineer.md",
    );
  });

  it("cliInterface_excludesApiEngineer", () => {
    const config = buildConfig({
      interfaces: [new InterfaceConfig("cli")],
    });
    expect(selectGithubConditionalAgents(config)).not.toContain(
      "api-engineer.md",
    );
  });

  it("eventDriven_includesEventEngineer", () => {
    const config = buildConfig({ eventDriven: true });
    expect(selectGithubConditionalAgents(config)).toContain(
      "event-engineer.md",
    );
  });

  it("eventConsumerInterface_includesEventEngineer", () => {
    const config = buildConfig({
      interfaces: [new InterfaceConfig("event-consumer")],
    });
    expect(selectGithubConditionalAgents(config)).toContain(
      "event-engineer.md",
    );
  });

  it("eventProducerInterface_includesEventEngineer", () => {
    const config = buildConfig({
      interfaces: [new InterfaceConfig("event-producer")],
    });
    expect(selectGithubConditionalAgents(config)).toContain(
      "event-engineer.md",
    );
  });

  it("noEventConfig_excludesEventEngineer", () => {
    const config = buildConfig({
      eventDriven: false,
      interfaces: [new InterfaceConfig("rest")],
    });
    expect(selectGithubConditionalAgents(config)).not.toContain(
      "event-engineer.md",
    );
  });

  it.each([
    ["container", "docker"],
    ["orchestrator", "kubernetes"],
    ["iac", "terraform"],
    ["serviceMesh", "istio"],
  ] as const)(
    "infraField_%s=%s_includesDevops",
    (field, value) => {
      const config = buildConfig({ [field]: value });
      expect(selectGithubConditionalAgents(config)).toContain(
        "devops-engineer.md",
      );
    },
  );
});

describe("GithubAgentsAssembler", () => {
  let tmpDir: string;
  let resourcesDir: string;
  let outputDir: string;
  let assembler: GithubAgentsAssembler;

  beforeEach(() => {
    tmpDir = fs.mkdtempSync(
      path.join(tmpdir(), "gh-agents-asm-test-"),
    );
    resourcesDir = path.join(tmpDir, "resources");
    fs.mkdirSync(resourcesDir, { recursive: true });
    outputDir = path.join(tmpDir, "output");
    fs.mkdirSync(outputDir, { recursive: true });
    assembler = new GithubAgentsAssembler();
  });

  afterEach(() => {
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  describe("assemble", () => {
    it("coreAgents_copiedWithAgentMdExtension", () => {
      createGithubCoreAgent(resourcesDir, "architect.md");
      createGithubCoreAgent(resourcesDir, "tech-lead.md");
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(
        fs.existsSync(
          path.join(outputDir, "agents", "architect.agent.md"),
        ),
      ).toBe(true);
      expect(
        fs.existsSync(
          path.join(outputDir, "agents", "tech-lead.agent.md"),
        ),
      ).toBe(true);
      expect(result.files).toHaveLength(2);
    });

    it("conditionalAgents_copiedWhenSelected", () => {
      createGithubConditionalAgent(
        resourcesDir, "devops-engineer.md",
      );
      const config = buildConfig({ container: "docker" });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result.files).toContain(
        path.join(
          outputDir, "agents", "devops-engineer.agent.md",
        ),
      );
    });

    it("developerAgent_copiedByLanguage", () => {
      createGithubDeveloperAgent(
        resourcesDir, "typescript-developer.md",
      );
      const config = buildConfig({ language: "typescript" });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result.files).toContain(
        path.join(
          outputDir, "agents",
          "typescript-developer.agent.md",
        ),
      );
    });

    it("missingConditionalTemplate_collectsWarning", () => {
      // Create the conditional dir but NOT the devops-engineer.md file
      const condDir = path.join(
        resourcesDir, "github-agents-templates", "conditional",
      );
      fs.mkdirSync(condDir, { recursive: true });
      const config = buildConfig({ container: "docker" });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result.warnings).toContain(
        "Conditional agent template missing: devops-engineer.md",
      );
    });

    it("missingDeveloperTemplate_collectsWarning", () => {
      const config = buildConfig({ language: "rust" });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result.warnings).toContain(
        "Developer agent template missing: rust-developer.md",
      );
    });

    it("placeholderReplacement_applied", () => {
      createGithubCoreAgent(
        resourcesDir, "architect.md",
        "# Agent for {project_name}",
      );
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const content = fs.readFileSync(
        path.join(
          outputDir, "agents", "architect.agent.md",
        ),
        "utf-8",
      );
      expect(content).toBe("# Agent for my-app");
    });

    it("outputExtension_agentMd", () => {
      createGithubCoreAgent(resourcesDir, "architect.md");
      createGithubConditionalAgent(
        resourcesDir, "devops-engineer.md",
      );
      createGithubDeveloperAgent(
        resourcesDir, "typescript-developer.md",
      );
      const config = buildConfig({
        container: "docker", language: "typescript",
      });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      for (const file of result.files) {
        expect(file).toMatch(/\.agent\.md$/);
      }
    });

    it("fullCombination_allAgentTypes", () => {
      createGithubCoreAgent(resourcesDir, "architect.md");
      createGithubCoreAgent(resourcesDir, "tech-lead.md");
      createGithubConditionalAgent(
        resourcesDir, "devops-engineer.md",
      );
      createGithubConditionalAgent(
        resourcesDir, "api-engineer.md",
      );
      createGithubDeveloperAgent(
        resourcesDir, "typescript-developer.md",
      );
      const config = buildConfig({
        container: "docker",
        interfaces: [new InterfaceConfig("rest")],
        language: "typescript",
      });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result.files).toHaveLength(5);
      expect(result.warnings).toEqual([]);
    });

    it("coreAgentsSorted", () => {
      createGithubCoreAgent(resourcesDir, "z-agent.md");
      createGithubCoreAgent(resourcesDir, "a-agent.md");
      createGithubCoreAgent(resourcesDir, "m-agent.md");
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      const coreFiles = result.files.filter(
        (f) => !f.includes("developer"),
      );
      const names = coreFiles.map(
        (f) => path.basename(f),
      );
      expect(names).toEqual([
        "a-agent.agent.md",
        "m-agent.agent.md",
        "z-agent.agent.md",
      ]);
    });

    it("createsAgentsDirectory", () => {
      createGithubCoreAgent(resourcesDir, "architect.md");
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const agentsDir = path.join(outputDir, "agents");
      expect(fs.existsSync(agentsDir)).toBe(true);
      expect(fs.statSync(agentsDir).isDirectory()).toBe(true);
    });
  });
});
