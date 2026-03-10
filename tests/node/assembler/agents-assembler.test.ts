import { describe, it, expect, beforeEach, afterEach } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";
import { tmpdir } from "node:os";
import { AgentsAssembler } from "../../../src/assembler/agents-assembler.js";
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
  ObservabilityConfig,
  SecurityConfig,
  TestingConfig,
} from "../../../src/models.js";
import {
  selectConditionalAgents,
  buildChecklistRules,
  checklistMarker,
} from "../../../src/assembler/agents-selection.js";

function buildConfig(overrides: {
  interfaces?: InterfaceConfig[];
  observabilityTool?: string;
  orchestrator?: string;
  container?: string;
  iac?: string;
  serviceMesh?: string;
  templating?: string;
  registry?: string;
  securityFrameworks?: string[];
  dbName?: string;
  eventDriven?: boolean;
  language?: string;
} = {}): ProjectConfig {
  return new ProjectConfig(
    new ProjectIdentity("my-app", "A test application"),
    new ArchitectureConfig(
      "microservice", true, overrides.eventDriven ?? false,
    ),
    overrides.interfaces ?? [new InterfaceConfig("rest")],
    new LanguageConfig(overrides.language ?? "java", "21"),
    new FrameworkConfig("quarkus", "3.0", "maven"),
    new DataConfig(
      new TechComponent(overrides.dbName ?? "none"),
      new TechComponent(),
      new TechComponent(),
    ),
    new InfraConfig(
      overrides.container ?? "docker",
      overrides.orchestrator ?? "none",
      overrides.templating ?? "kustomize",
      overrides.iac ?? "none",
      overrides.registry ?? "none",
      "none",
      overrides.serviceMesh ?? "none",
      new ObservabilityConfig(overrides.observabilityTool ?? "none"),
    ),
    new SecurityConfig(overrides.securityFrameworks ?? []),
    new TestingConfig(),
  );
}

function createCoreAgent(
  resourcesDir: string, agentName: string, content?: string,
): void {
  const dir = path.join(resourcesDir, "agents-templates", "core");
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(
    path.join(dir, agentName),
    content ?? `# ${agentName}\nProject: {project_name}`,
  );
}

function createConditionalAgent(
  resourcesDir: string, agentName: string, content?: string,
): void {
  const dir = path.join(resourcesDir, "agents-templates", "conditional");
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(
    path.join(dir, agentName),
    content ?? `# ${agentName}\nProject: {project_name}`,
  );
}

function createDeveloperAgent(
  resourcesDir: string, agentName: string, content?: string,
): void {
  const dir = path.join(resourcesDir, "agents-templates", "developers");
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(
    path.join(dir, agentName),
    content ?? `# ${agentName}\nProject: {project_name}`,
  );
}

function createChecklist(
  resourcesDir: string, checklistName: string, content: string,
): void {
  const dir = path.join(resourcesDir, "agents-templates", "checklists");
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(path.join(dir, checklistName), content);
}

describe("agents-selection", () => {
  describe("selectConditionalAgents", () => {
    it("includesDatabaseEngineer_whenDatabaseConfigured", () => {
      const config = buildConfig({ dbName: "postgresql" });
      expect(selectConditionalAgents(config)).toContain(
        "database-engineer.md",
      );
    });

    it("excludesDatabaseEngineer_whenNone", () => {
      const config = buildConfig({ dbName: "none" });
      expect(selectConditionalAgents(config)).not.toContain(
        "database-engineer.md",
      );
    });

    it("includesObservabilityEngineer_whenToolConfigured", () => {
      const config = buildConfig({ observabilityTool: "opentelemetry" });
      expect(selectConditionalAgents(config)).toContain(
        "observability-engineer.md",
      );
    });

    it("excludesObservabilityEngineer_whenNone", () => {
      const config = buildConfig({ observabilityTool: "none" });
      expect(selectConditionalAgents(config)).not.toContain(
        "observability-engineer.md",
      );
    });

    it.each([
      ["container", "docker"],
      ["orchestrator", "kubernetes"],
      ["iac", "terraform"],
      ["serviceMesh", "istio"],
    ] as const)(
      "includesDevopsEngineer_when_%s=%s",
      (field, value) => {
        const config = buildConfig({
          container: "none", [field]: value,
        });
        expect(selectConditionalAgents(config)).toContain(
          "devops-engineer.md",
        );
      },
    );

    it("excludesDevopsEngineer_whenAllNone", () => {
      const config = buildConfig({
        container: "none", orchestrator: "none",
        iac: "none", serviceMesh: "none",
      });
      expect(selectConditionalAgents(config)).not.toContain(
        "devops-engineer.md",
      );
    });

    it.each(["rest", "grpc", "graphql"])(
      "includesApiEngineer_when_%s",
      (ifaceType) => {
        const config = buildConfig({
          interfaces: [new InterfaceConfig(ifaceType)],
        });
        expect(selectConditionalAgents(config)).toContain(
          "api-engineer.md",
        );
      },
    );

    it("excludesApiEngineer_whenNoMatchingInterface", () => {
      const config = buildConfig({
        interfaces: [new InterfaceConfig("cli")],
      });
      expect(selectConditionalAgents(config)).not.toContain(
        "api-engineer.md",
      );
    });

    it("includesEventEngineer_whenEventDriven", () => {
      const config = buildConfig({ eventDriven: true });
      expect(selectConditionalAgents(config)).toContain(
        "event-engineer.md",
      );
    });

    it.each(["event-consumer", "event-producer"])(
      "includesEventEngineer_when_%s",
      (ifaceType) => {
        const config = buildConfig({
          interfaces: [new InterfaceConfig(ifaceType)],
        });
        expect(selectConditionalAgents(config)).toContain(
          "event-engineer.md",
        );
      },
    );

    it("excludesEventEngineer_whenNotEventDrivenAndNoEventInterfaces", () => {
      const config = buildConfig({
        eventDriven: false,
        interfaces: [new InterfaceConfig("rest")],
      });
      expect(selectConditionalAgents(config)).not.toContain(
        "event-engineer.md",
      );
    });
  });

  describe("buildChecklistRules", () => {
    it.each([
      ["pci-dss", "pci-dss-security.md", "security-engineer.md"],
      ["lgpd", "privacy-security.md", "security-engineer.md"],
      ["gdpr", "privacy-security.md", "security-engineer.md"],
      ["hipaa", "hipaa-security.md", "security-engineer.md"],
      ["sox", "sox-security.md", "security-engineer.md"],
    ])(
      "securityFramework_%s_activates_%s_on_%s",
      (framework, checklist, agent) => {
        const config = buildConfig({
          securityFrameworks: [framework],
        });
        const rules = buildChecklistRules(config);
        const match = rules.find(
          (r) => r.checklist === checklist && r.agent === agent,
        );
        expect(match?.active).toBe(true);
      },
    );

    it("noSecurityFrameworks_allSecurityInactive", () => {
      const config = buildConfig({ securityFrameworks: [] });
      const rules = buildChecklistRules(config);
      const securityRules = rules.filter(
        (r) => r.agent === "security-engineer.md",
      );
      expect(securityRules.every((r) => !r.active)).toBe(true);
    });

    it.each([
      ["grpc", "grpc-api.md"],
      ["graphql", "graphql-api.md"],
      ["websocket", "websocket-api.md"],
    ])(
      "interface_%s_activates_%s",
      (ifaceType, checklist) => {
        const config = buildConfig({
          interfaces: [new InterfaceConfig(ifaceType)],
        });
        const rules = buildChecklistRules(config);
        const match = rules.find(
          (r) => r.checklist === checklist,
        );
        expect(match?.active).toBe(true);
      },
    );

    it("noMatchingInterface_allApiInactive", () => {
      const config = buildConfig({
        interfaces: [new InterfaceConfig("cli")],
      });
      const rules = buildChecklistRules(config);
      const apiRules = rules.filter(
        (r) => r.agent === "api-engineer.md",
      );
      expect(apiRules.every((r) => !r.active)).toBe(true);
    });

    it.each([
      ["templating", "helm", "helm-devops.md"],
      ["iac", "terraform", "iac-devops.md"],
      ["serviceMesh", "istio", "mesh-devops.md"],
      ["registry", "ecr", "registry-devops.md"],
    ] as const)(
      "infra_%s=%s_activates_%s",
      (field, value, checklist) => {
        const config = buildConfig({ [field]: value });
        const rules = buildChecklistRules(config);
        const match = rules.find(
          (r) => r.checklist === checklist,
        );
        expect(match?.active).toBe(true);
      },
    );

    it("allInfraNone_allDevopsInactive", () => {
      const config = buildConfig({
        container: "none", orchestrator: "none",
        iac: "none", serviceMesh: "none", registry: "none",
        templating: "kustomize",
      });
      const rules = buildChecklistRules(config);
      const devopsRules = rules.filter(
        (r) => r.agent === "devops-engineer.md",
      );
      const activeDevops = devopsRules.filter((r) => r.active);
      expect(activeDevops).toHaveLength(0);
    });

    it("returns11TotalRules", () => {
      const config = buildConfig();
      expect(buildChecklistRules(config)).toHaveLength(11);
    });
  });

  describe("checklistMarker", () => {
    it.each([
      ["pci-dss-security.md", "<!-- PCI_DSS_SECURITY -->"],
      ["privacy-security.md", "<!-- PRIVACY_SECURITY -->"],
      ["helm-devops.md", "<!-- HELM_DEVOPS -->"],
      ["grpc-api.md", "<!-- GRPC_API -->"],
    ])("%s_produces_%s", (file, expected) => {
      expect(checklistMarker(file)).toBe(expected);
    });
  });
});

describe("AgentsAssembler", () => {
  let tmpDir: string;
  let resourcesDir: string;
  let outputDir: string;
  let assembler: AgentsAssembler;

  beforeEach(() => {
    tmpDir = fs.mkdtempSync(
      path.join(tmpdir(), "agents-asm-test-"),
    );
    resourcesDir = path.join(tmpDir, "resources");
    fs.mkdirSync(resourcesDir, { recursive: true });
    outputDir = path.join(tmpDir, "output");
    fs.mkdirSync(outputDir, { recursive: true });
    assembler = new AgentsAssembler();
  });

  afterEach(() => {
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  describe("selectCoreAgents", () => {
    it("returnsEmpty_whenCoreDirMissing", () => {
      expect(assembler.selectCoreAgents(resourcesDir)).toEqual([]);
    });

    it("returnsSortedMdFiles", () => {
      createCoreAgent(resourcesDir, "z-agent.md");
      createCoreAgent(resourcesDir, "a-agent.md");
      createCoreAgent(resourcesDir, "m-agent.md");
      expect(assembler.selectCoreAgents(resourcesDir)).toEqual([
        "a-agent.md", "m-agent.md", "z-agent.md",
      ]);
    });

    it("skipsNonMdFiles", () => {
      createCoreAgent(resourcesDir, "real-agent.md");
      const coreDir = path.join(
        resourcesDir, "agents-templates", "core",
      );
      fs.writeFileSync(path.join(coreDir, "readme.txt"), "ignore");
      expect(assembler.selectCoreAgents(resourcesDir)).toEqual([
        "real-agent.md",
      ]);
    });

    it("skipsDirectories", () => {
      createCoreAgent(resourcesDir, "real-agent.md");
      const coreDir = path.join(
        resourcesDir, "agents-templates", "core",
      );
      fs.mkdirSync(path.join(coreDir, "subdir"), { recursive: true });
      expect(assembler.selectCoreAgents(resourcesDir)).toEqual([
        "real-agent.md",
      ]);
    });
  });

  describe("selectDeveloperAgent", () => {
    it("returnsLanguageSpecificFilename", () => {
      const config = buildConfig({ language: "java" });
      expect(assembler.selectDeveloperAgent(config)).toBe(
        "java-developer.md",
      );
    });

    it("handlesTypescriptLanguage", () => {
      const config = buildConfig({ language: "typescript" });
      expect(assembler.selectDeveloperAgent(config)).toBe(
        "typescript-developer.md",
      );
    });
  });

  describe("assemble", () => {
    it("returnsEmptyResult_whenNoTemplates", () => {
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result.files).toEqual([]);
      expect(result.warnings).toEqual([]);
    });

    it("copiesCoreAgentsToOutput", () => {
      createCoreAgent(resourcesDir, "architect.md");
      createCoreAgent(resourcesDir, "tech-lead.md");
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result.files).toHaveLength(2);
      expect(
        fs.existsSync(path.join(outputDir, "agents", "architect.md")),
      ).toBe(true);
      expect(
        fs.existsSync(path.join(outputDir, "agents", "tech-lead.md")),
      ).toBe(true);
    });

    it("replacesPlaceholdersInCoreAgents", () => {
      createCoreAgent(
        resourcesDir, "architect.md",
        "# Architect for {project_name}",
      );
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const content = fs.readFileSync(
        path.join(outputDir, "agents", "architect.md"), "utf-8",
      );
      expect(content).toBe("# Architect for my-app");
    });

    it("copiesConditionalAgents_whenSourceExists", () => {
      createConditionalAgent(resourcesDir, "database-engineer.md");
      const config = buildConfig({ dbName: "postgresql" });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result.files).toContain(
        path.join(outputDir, "agents", "database-engineer.md"),
      );
    });

    it("skipsConditionalAgents_whenSourceMissing", () => {
      const config = buildConfig({ dbName: "postgresql" });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result.files).toEqual([]);
    });

    it("copiesDeveloperAgent_whenSourceExists", () => {
      createDeveloperAgent(resourcesDir, "java-developer.md");
      const config = buildConfig({ language: "java" });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result.files).toContain(
        path.join(outputDir, "agents", "java-developer.md"),
      );
    });

    it("skipsDeveloperAgent_whenSourceMissing", () => {
      const config = buildConfig({ language: "rust" });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result.files).toEqual([]);
    });

    it("injectsSecurityChecklist_whenFrameworkActive", () => {
      createCoreAgent(
        resourcesDir, "security-engineer.md",
        "# Security\n<!-- PCI_DSS_SECURITY -->\nEnd",
      );
      createChecklist(
        resourcesDir, "pci-dss-security.md",
        "## PCI-DSS Checklist",
      );
      const config = buildConfig({
        securityFrameworks: ["pci-dss"],
      });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const content = fs.readFileSync(
        path.join(outputDir, "agents", "security-engineer.md"), "utf-8",
      );
      expect(content).toContain("## PCI-DSS Checklist");
      expect(content).not.toContain("<!-- PCI_DSS_SECURITY -->");
    });

    it("injectsPrivacyChecklist_whenLgpd", () => {
      createCoreAgent(
        resourcesDir, "security-engineer.md",
        "# Security\n<!-- PRIVACY_SECURITY -->\nEnd",
      );
      createChecklist(
        resourcesDir, "privacy-security.md",
        "## Privacy Checklist",
      );
      const config = buildConfig({
        securityFrameworks: ["lgpd"],
      });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const content = fs.readFileSync(
        path.join(outputDir, "agents", "security-engineer.md"), "utf-8",
      );
      expect(content).toContain("## Privacy Checklist");
    });

    it("injectsPrivacyChecklist_whenGdpr", () => {
      createCoreAgent(
        resourcesDir, "security-engineer.md",
        "# Security\n<!-- PRIVACY_SECURITY -->\nEnd",
      );
      createChecklist(
        resourcesDir, "privacy-security.md",
        "## Privacy Checklist",
      );
      const config = buildConfig({
        securityFrameworks: ["gdpr"],
      });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const content = fs.readFileSync(
        path.join(outputDir, "agents", "security-engineer.md"), "utf-8",
      );
      expect(content).toContain("## Privacy Checklist");
    });

    it("injectsApiChecklist_whenGrpc", () => {
      createConditionalAgent(
        resourcesDir, "api-engineer.md",
        "# API\n<!-- GRPC_API -->\nEnd",
      );
      createChecklist(resourcesDir, "grpc-api.md", "## gRPC Checklist");
      const config = buildConfig({
        interfaces: [
          new InterfaceConfig("rest"),
          new InterfaceConfig("grpc"),
        ],
      });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const content = fs.readFileSync(
        path.join(outputDir, "agents", "api-engineer.md"), "utf-8",
      );
      expect(content).toContain("## gRPC Checklist");
    });

    it("injectsDevopsChecklist_whenHelm", () => {
      createConditionalAgent(
        resourcesDir, "devops-engineer.md",
        "# DevOps\n<!-- HELM_DEVOPS -->\nEnd",
      );
      createChecklist(
        resourcesDir, "helm-devops.md", "## Helm Checklist",
      );
      const config = buildConfig({
        container: "docker", templating: "helm",
      });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const content = fs.readFileSync(
        path.join(outputDir, "agents", "devops-engineer.md"), "utf-8",
      );
      expect(content).toContain("## Helm Checklist");
    });

    it("skipsChecklistInjection_whenAgentFileMissing", () => {
      createChecklist(
        resourcesDir, "pci-dss-security.md",
        "## PCI-DSS",
      );
      const config = buildConfig({
        securityFrameworks: ["pci-dss"],
      });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result.files).toEqual([]);
    });

    it("skipsChecklistInjection_whenChecklistFileMissing", () => {
      createCoreAgent(
        resourcesDir, "security-engineer.md",
        "# Security\n<!-- PCI_DSS_SECURITY -->\nEnd",
      );
      const config = buildConfig({
        securityFrameworks: ["pci-dss"],
      });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const content = fs.readFileSync(
        path.join(outputDir, "agents", "security-engineer.md"), "utf-8",
      );
      expect(content).toContain("<!-- PCI_DSS_SECURITY -->");
    });

    it("fullAssembly_combinedOutput", () => {
      createCoreAgent(resourcesDir, "architect.md");
      createCoreAgent(resourcesDir, "tech-lead.md");
      createCoreAgent(
        resourcesDir, "security-engineer.md",
        "# Security\n<!-- HIPAA_SECURITY -->\nEnd",
      );
      createConditionalAgent(resourcesDir, "database-engineer.md");
      createConditionalAgent(
        resourcesDir, "api-engineer.md",
        "# API\n<!-- GRAPHQL_API -->\nEnd",
      );
      createDeveloperAgent(resourcesDir, "java-developer.md");
      createChecklist(
        resourcesDir, "hipaa-security.md", "## HIPAA",
      );
      createChecklist(
        resourcesDir, "graphql-api.md", "## GraphQL",
      );
      const config = buildConfig({
        dbName: "postgresql",
        interfaces: [
          new InterfaceConfig("rest"),
          new InterfaceConfig("graphql"),
        ],
        securityFrameworks: ["hipaa"],
        language: "java",
      });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result.files).toHaveLength(6);
      expect(result.warnings).toEqual([]);
      const security = fs.readFileSync(
        path.join(outputDir, "agents", "security-engineer.md"), "utf-8",
      );
      expect(security).toContain("## HIPAA");
      const api = fs.readFileSync(
        path.join(outputDir, "agents", "api-engineer.md"), "utf-8",
      );
      expect(api).toContain("## GraphQL");
    });

    it("injectsMultipleChecklists_intoSameAgent", () => {
      createCoreAgent(
        resourcesDir, "security-engineer.md",
        "# Sec\n<!-- PCI_DSS_SECURITY -->\n<!-- HIPAA_SECURITY -->\nEnd",
      );
      createChecklist(
        resourcesDir, "pci-dss-security.md", "## PCI-DSS",
      );
      createChecklist(
        resourcesDir, "hipaa-security.md", "## HIPAA",
      );
      const config = buildConfig({
        securityFrameworks: ["pci-dss", "hipaa"],
      });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const content = fs.readFileSync(
        path.join(outputDir, "agents", "security-engineer.md"), "utf-8",
      );
      expect(content).toContain("## PCI-DSS");
      expect(content).toContain("## HIPAA");
      expect(content).not.toContain("<!-- PCI_DSS_SECURITY -->");
      expect(content).not.toContain("<!-- HIPAA_SECURITY -->");
    });

    it("eventEngineer_includedWhenEventDriven", () => {
      createConditionalAgent(resourcesDir, "event-engineer.md");
      const config = buildConfig({ eventDriven: true });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result.files).toContain(
        path.join(outputDir, "agents", "event-engineer.md"),
      );
    });

    it("coreAgentsAlwaysPresent_regardlessOfConfig", () => {
      createCoreAgent(resourcesDir, "architect.md");
      createCoreAgent(resourcesDir, "qa-engineer.md");
      const config = buildConfig({ dbName: "none" });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result.files).toHaveLength(2);
      expect(
        fs.existsSync(path.join(outputDir, "agents", "architect.md")),
      ).toBe(true);
      expect(
        fs.existsSync(path.join(outputDir, "agents", "qa-engineer.md")),
      ).toBe(true);
    });

    it("injectsAllFourSecurityChecklists_whenAllFrameworks", () => {
      createCoreAgent(
        resourcesDir, "security-engineer.md",
        [
          "# Sec",
          "<!-- PCI_DSS_SECURITY -->",
          "<!-- PRIVACY_SECURITY -->",
          "<!-- HIPAA_SECURITY -->",
          "<!-- SOX_SECURITY -->",
          "End",
        ].join("\n"),
      );
      createChecklist(resourcesDir, "pci-dss-security.md", "PCI");
      createChecklist(resourcesDir, "privacy-security.md", "PRIV");
      createChecklist(resourcesDir, "hipaa-security.md", "HIPAA");
      createChecklist(resourcesDir, "sox-security.md", "SOX");
      const config = buildConfig({
        securityFrameworks: ["pci-dss", "lgpd", "hipaa", "sox"],
      });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const content = fs.readFileSync(
        path.join(outputDir, "agents", "security-engineer.md"), "utf-8",
      );
      expect(content).toContain("PCI");
      expect(content).toContain("PRIV");
      expect(content).toContain("HIPAA");
      expect(content).toContain("SOX");
    });

    it("injectsAllDevopsChecklists_whenAllInfraActive", () => {
      createConditionalAgent(
        resourcesDir, "devops-engineer.md",
        [
          "# DevOps",
          "<!-- HELM_DEVOPS -->",
          "<!-- IAC_DEVOPS -->",
          "<!-- MESH_DEVOPS -->",
          "<!-- REGISTRY_DEVOPS -->",
          "End",
        ].join("\n"),
      );
      createChecklist(resourcesDir, "helm-devops.md", "HELM");
      createChecklist(resourcesDir, "iac-devops.md", "IAC");
      createChecklist(resourcesDir, "mesh-devops.md", "MESH");
      createChecklist(resourcesDir, "registry-devops.md", "REG");
      const config = buildConfig({
        container: "docker",
        templating: "helm",
        iac: "terraform",
        serviceMesh: "istio",
        registry: "ecr",
      });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const content = fs.readFileSync(
        path.join(outputDir, "agents", "devops-engineer.md"), "utf-8",
      );
      expect(content).toContain("HELM");
      expect(content).toContain("IAC");
      expect(content).toContain("MESH");
      expect(content).toContain("REG");
    });
  });
});
