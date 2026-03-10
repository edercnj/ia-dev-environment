import { describe, it, expect, beforeEach, afterEach } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";
import { tmpdir } from "node:os";
import { SkillsAssembler } from "../../../src/assembler/skills-assembler.js";
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

function buildConfig(overrides: {
  interfaces?: InterfaceConfig[];
  observabilityTool?: string;
  orchestrator?: string;
  apiGateway?: string;
  smokeTests?: boolean;
  contractTests?: boolean;
  performanceTests?: boolean;
  securityFrameworks?: string[];
  dbName?: string;
  cacheName?: string;
  framework?: string;
  container?: string;
  templating?: string;
  iac?: string;
  registry?: string;
} = {}): ProjectConfig {
  return new ProjectConfig(
    new ProjectIdentity("my-app", "A test application"),
    new ArchitectureConfig("microservice", true, false),
    overrides.interfaces ?? [new InterfaceConfig("rest")],
    new LanguageConfig("java", "21"),
    new FrameworkConfig(overrides.framework ?? "quarkus", "3.0", "maven"),
    new DataConfig(
      new TechComponent(overrides.dbName ?? "none"),
      new TechComponent(),
      new TechComponent(overrides.cacheName ?? "none"),
    ),
    new InfraConfig(
      overrides.container ?? "docker",
      overrides.orchestrator ?? "none",
      overrides.templating ?? "kustomize",
      overrides.iac ?? "none",
      overrides.registry ?? "none",
      overrides.apiGateway ?? "none",
      "none",
      new ObservabilityConfig(overrides.observabilityTool ?? "none"),
    ),
    new SecurityConfig(overrides.securityFrameworks ?? []),
    new TestingConfig(
      overrides.smokeTests ?? true,
      overrides.contractTests ?? false,
      overrides.performanceTests ?? true,
    ),
  );
}

function setupSkillsResources(tmpDir: string): string {
  const resourcesDir = path.join(tmpDir, "resources");
  fs.mkdirSync(resourcesDir, { recursive: true });
  return resourcesDir;
}

function createCoreSkill(resourcesDir: string, skillName: string): void {
  const dir = path.join(
    resourcesDir, "skills-templates", "core", skillName,
  );
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(
    path.join(dir, "SKILL.md"),
    `# ${skillName}\nuser-invocable: true`,
  );
}

function createLibSkill(
  resourcesDir: string, libName: string,
): void {
  const dir = path.join(
    resourcesDir, "skills-templates", "core", "lib", libName,
  );
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(
    path.join(dir, "SKILL.md"),
    `# lib/${libName}`,
  );
}

function createConditionalSkill(
  resourcesDir: string, skillName: string,
): void {
  const dir = path.join(
    resourcesDir, "skills-templates", "conditional", skillName,
  );
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(
    path.join(dir, "SKILL.md"),
    `# ${skillName}\nuser-invocable: true`,
  );
}

function createKnowledgePack(
  resourcesDir: string, packName: string,
): void {
  const dir = path.join(
    resourcesDir, "skills-templates", "knowledge-packs", packName,
  );
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(
    path.join(dir, "SKILL.md"),
    `# ${packName}\nuser-invocable: false\n# Knowledge Pack`,
  );
  const refs = path.join(dir, "references");
  fs.mkdirSync(refs, { recursive: true });
  fs.writeFileSync(path.join(refs, "ref.md"), `# ${packName} ref`);
}

function createStackPattern(
  resourcesDir: string, packName: string,
): void {
  const dir = path.join(
    resourcesDir, "skills-templates", "knowledge-packs",
    "stack-patterns", packName,
  );
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(
    path.join(dir, "SKILL.md"),
    `# ${packName}\nuser-invocable: false`,
  );
}

function createInfraPattern(
  resourcesDir: string, packName: string,
): void {
  const dir = path.join(
    resourcesDir, "skills-templates", "knowledge-packs",
    "infra-patterns", packName,
  );
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(
    path.join(dir, "SKILL.md"),
    `# ${packName}\nuser-invocable: false`,
  );
}

describe("SkillsAssembler", () => {
  let tmpDir: string;
  let resourcesDir: string;
  let outputDir: string;
  let assembler: SkillsAssembler;

  beforeEach(() => {
    tmpDir = path.join(tmpdir(), `skills-asm-test-${Date.now()}`);
    fs.mkdirSync(tmpDir, { recursive: true });
    resourcesDir = setupSkillsResources(tmpDir);
    outputDir = path.join(tmpDir, "output");
    fs.mkdirSync(outputDir, { recursive: true });
    assembler = new SkillsAssembler();
  });

  afterEach(() => {
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  describe("selectCoreSkills", () => {
    it("returnsEmpty_whenCoreDirMissing", () => {
      const result = assembler.selectCoreSkills(resourcesDir);
      expect(result).toEqual([]);
    });

    it("returnsSortedSkillNames", () => {
      createCoreSkill(resourcesDir, "z-skill");
      createCoreSkill(resourcesDir, "a-skill");
      createCoreSkill(resourcesDir, "m-skill");
      const result = assembler.selectCoreSkills(resourcesDir);
      expect(result).toEqual(["a-skill", "m-skill", "z-skill"]);
    });

    it("skipsNonDirectoryEntries", () => {
      createCoreSkill(resourcesDir, "real-skill");
      const coreDir = path.join(
        resourcesDir, "skills-templates", "core",
      );
      fs.writeFileSync(path.join(coreDir, "readme.txt"), "ignore me");
      const result = assembler.selectCoreSkills(resourcesDir);
      expect(result).toEqual(["real-skill"]);
    });

    it("expandsLibSubdirectories", () => {
      createLibSkill(resourcesDir, "task-decomposer");
      createLibSkill(resourcesDir, "group-verifier");
      createCoreSkill(resourcesDir, "x-review");
      const result = assembler.selectCoreSkills(resourcesDir);
      expect(result).toContain("lib/group-verifier");
      expect(result).toContain("lib/task-decomposer");
      expect(result).toContain("x-review");
    });

    it("libSubdirectoriesSorted", () => {
      createLibSkill(resourcesDir, "z-lib");
      createLibSkill(resourcesDir, "a-lib");
      const result = assembler.selectCoreSkills(resourcesDir);
      const libSkills = result.filter((s) => s.startsWith("lib/"));
      expect(libSkills).toEqual(["lib/a-lib", "lib/z-lib"]);
    });

    it("skipsNonDirectoryEntriesInsideLib", () => {
      createLibSkill(resourcesDir, "real-lib");
      const libDir = path.join(
        resourcesDir, "skills-templates", "core", "lib",
      );
      fs.writeFileSync(path.join(libDir, "readme.txt"), "ignore");
      const result = assembler.selectCoreSkills(resourcesDir);
      const libSkills = result.filter((s) => s.startsWith("lib/"));
      expect(libSkills).toEqual(["lib/real-lib"]);
    });
  });

  describe("selectConditionalSkills", () => {
    describe("interface skills", () => {
      it.each([
        ["rest", "x-review-api"],
        ["grpc", "x-review-grpc"],
        ["graphql", "x-review-graphql"],
        ["event-consumer", "x-review-events"],
        ["event-producer", "x-review-events"],
      ])("%s_includes_%s", (ifaceType, expectedSkill) => {
        const config = buildConfig({
          interfaces: [new InterfaceConfig(ifaceType)],
        });
        const result = assembler.selectConditionalSkills(config);
        expect(result).toContain(expectedSkill);
      });

      it("noMatchingInterface_noInterfaceSkills", () => {
        const config = buildConfig({
          interfaces: [new InterfaceConfig("cli")],
        });
        const result = assembler.selectConditionalSkills(config);
        expect(result).not.toContain("x-review-api");
        expect(result).not.toContain("x-review-grpc");
        expect(result).not.toContain("x-review-graphql");
        expect(result).not.toContain("x-review-events");
      });

      it("multipleInterfaces_includesAll", () => {
        const config = buildConfig({
          interfaces: [
            new InterfaceConfig("rest"),
            new InterfaceConfig("grpc"),
            new InterfaceConfig("event-consumer"),
          ],
        });
        const result = assembler.selectConditionalSkills(config);
        expect(result).toContain("x-review-api");
        expect(result).toContain("x-review-grpc");
        expect(result).toContain("x-review-events");
      });
    });

    describe("infra skills", () => {
      it.each([
        ["observabilityTool", "opentelemetry", "instrument-otel"],
        ["orchestrator", "kubernetes", "setup-environment"],
        ["apiGateway", "kong", "x-review-gateway"],
      ] as const)("%s=%s_includes_%s", (field, value, expectedSkill) => {
        const config = buildConfig({ [field]: value });
        const result = assembler.selectConditionalSkills(config);
        expect(result).toContain(expectedSkill);
      });

      it.each([
        ["observabilityTool", "none", "instrument-otel"],
        ["orchestrator", "none", "setup-environment"],
        ["apiGateway", "none", "x-review-gateway"],
      ] as const)("%s=none_excludes_%s", (field, value, excludedSkill) => {
        const config = buildConfig({ [field]: value });
        const result = assembler.selectConditionalSkills(config);
        expect(result).not.toContain(excludedSkill);
      });
    });

    describe("testing skills", () => {
      it.each([
        [true, "rest", "run-smoke-api", true],
        [true, "tcp-custom", "run-smoke-socket", true],
        [true, "grpc", "run-smoke-api", false],
        [false, "rest", "run-smoke-api", false],
        [false, "tcp-custom", "run-smoke-socket", false],
      ])(
        "smoke=%s+iface=%s_%s_present=%s",
        (smoke, iface, skill, expected) => {
          const config = buildConfig({
            smokeTests: smoke,
            interfaces: [new InterfaceConfig(iface)],
          });
          const result = assembler.selectConditionalSkills(config);
          if (expected) {
            expect(result).toContain(skill);
          } else {
            expect(result).not.toContain(skill);
          }
        },
      );

      it("alwaysIncludesRunE2e", () => {
        const config = buildConfig();
        const result = assembler.selectConditionalSkills(config);
        expect(result).toContain("run-e2e");
      });

      it.each([
        ["performanceTests", true, "run-perf-test", true],
        ["performanceTests", false, "run-perf-test", false],
        ["contractTests", true, "run-contract-tests", true],
        ["contractTests", false, "run-contract-tests", false],
      ] as const)(
        "%s=%s_%s_present=%s",
        (field, value, skill, expected) => {
          const config = buildConfig({ [field]: value });
          const result = assembler.selectConditionalSkills(config);
          if (expected) {
            expect(result).toContain(skill);
          } else {
            expect(result).not.toContain(skill);
          }
        },
      );
    });

    describe("security skills", () => {
      it("withFrameworks_includesSecurityReview", () => {
        const config = buildConfig({
          securityFrameworks: ["hipaa", "gdpr"],
        });
        const result = assembler.selectConditionalSkills(config);
        expect(result).toContain("x-review-security");
      });

      it("noFrameworks_excludesSecurityReview", () => {
        const config = buildConfig({ securityFrameworks: [] });
        const result = assembler.selectConditionalSkills(config);
        expect(result).not.toContain("x-review-security");
      });
    });
  });

  describe("selectKnowledgePacks", () => {
    it("includesAllCoreKnowledgePacks", () => {
      const config = buildConfig();
      const packs = assembler.selectKnowledgePacks(config);
      expect(packs).toContain("coding-standards");
      expect(packs).toContain("architecture");
      expect(packs).toContain("testing");
      expect(packs).toContain("security");
      expect(packs).toContain("compliance");
      expect(packs).toContain("api-design");
      expect(packs).toContain("observability");
      expect(packs).toContain("resilience");
      expect(packs).toContain("infrastructure");
      expect(packs).toContain("protocols");
      expect(packs).toContain("story-planning");
    });

    it("includesLayerTemplates", () => {
      const config = buildConfig();
      const packs = assembler.selectKnowledgePacks(config);
      expect(packs).toContain("layer-templates");
    });

    it("includesDataPacks_whenDatabaseConfigured", () => {
      const config = buildConfig({ dbName: "postgresql" });
      const packs = assembler.selectKnowledgePacks(config);
      expect(packs).toContain("database-patterns");
    });

    it("includesDataPacks_whenCacheConfigured", () => {
      const config = buildConfig({ cacheName: "redis" });
      const packs = assembler.selectKnowledgePacks(config);
      expect(packs).toContain("database-patterns");
    });

    it("excludesDataPacks_whenNoDatabaseOrCache", () => {
      const config = buildConfig({ dbName: "none", cacheName: "none" });
      const packs = assembler.selectKnowledgePacks(config);
      expect(packs).not.toContain("database-patterns");
    });

    it("has12CorePacksPlusLayerTemplates_noData", () => {
      const config = buildConfig({ dbName: "none", cacheName: "none" });
      const packs = assembler.selectKnowledgePacks(config);
      expect(packs).toHaveLength(12);
    });

    it("has13Packs_withDatabase", () => {
      const config = buildConfig({ dbName: "postgresql" });
      const packs = assembler.selectKnowledgePacks(config);
      expect(packs).toHaveLength(13);
    });
  });

  describe("assemble", () => {
    it("returnsEmptyArray_whenNoTemplates", () => {
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result).toEqual([]);
    });

    it("copiesCoreSkillsToOutput", () => {
      createCoreSkill(resourcesDir, "x-review");
      createCoreSkill(resourcesDir, "commit");
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result).toHaveLength(2);
      expect(
        fs.existsSync(path.join(outputDir, "skills", "x-review", "SKILL.md")),
      ).toBe(true);
      expect(
        fs.existsSync(path.join(outputDir, "skills", "commit", "SKILL.md")),
      ).toBe(true);
    });

    it("copiesLibSkillsToOutput", () => {
      createLibSkill(resourcesDir, "task-decomposer");
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result).toHaveLength(1);
      expect(
        fs.existsSync(
          path.join(outputDir, "skills", "lib", "task-decomposer", "SKILL.md"),
        ),
      ).toBe(true);
    });

    it("copiesConditionalSkills_whenSourceExists", () => {
      createConditionalSkill(resourcesDir, "x-review-api");
      const config = buildConfig({
        interfaces: [new InterfaceConfig("rest")],
      });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result).toContain(
        path.join(outputDir, "skills", "x-review-api"),
      );
    });

    it("skipsConditionalSkills_whenSourceMissing", () => {
      const config = buildConfig({
        interfaces: [new InterfaceConfig("rest")],
      });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result).toEqual([]);
    });

    it("copiesKnowledgePacks_whenSourceExists", () => {
      createKnowledgePack(resourcesDir, "coding-standards");
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result.some((f) => f.includes("coding-standards"))).toBe(true);
      expect(
        fs.existsSync(
          path.join(outputDir, "skills", "coding-standards", "SKILL.md"),
        ),
      ).toBe(true);
    });

    it("copiesKnowledgePackReferences", () => {
      createKnowledgePack(resourcesDir, "architecture");
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      expect(
        fs.existsSync(
          path.join(
            outputDir, "skills", "architecture", "references", "ref.md",
          ),
        ),
      ).toBe(true);
    });

    it("replacesPlaceholdersInKnowledgePackSkillMd", () => {
      const kpDir = path.join(
        resourcesDir, "skills-templates", "knowledge-packs", "testing",
      );
      fs.mkdirSync(kpDir, { recursive: true });
      fs.writeFileSync(
        path.join(kpDir, "SKILL.md"),
        "# Testing for {project_name}",
      );
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const content = fs.readFileSync(
        path.join(outputDir, "skills", "testing", "SKILL.md"), "utf-8",
      );
      expect(content).toContain("# Testing for my-app");
    });

    it("skipsExistingNonSkillItems", () => {
      createKnowledgePack(resourcesDir, "coding-standards");
      const destDir = path.join(outputDir, "skills", "coding-standards");
      const refsDir = path.join(destDir, "references");
      fs.mkdirSync(refsDir, { recursive: true });
      fs.writeFileSync(
        path.join(refsDir, "ref.md"),
        "existing content",
      );
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const content = fs.readFileSync(
        path.join(refsDir, "ref.md"), "utf-8",
      );
      expect(content).toBe("existing content");
    });

    it("overwritesSkillMdEvenIfExists", () => {
      createKnowledgePack(resourcesDir, "coding-standards");
      const destDir = path.join(outputDir, "skills", "coding-standards");
      fs.mkdirSync(destDir, { recursive: true });
      fs.writeFileSync(
        path.join(destDir, "SKILL.md"),
        "old content",
      );
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const content = fs.readFileSync(
        path.join(destDir, "SKILL.md"), "utf-8",
      );
      expect(content).not.toBe("old content");
    });

    it("copiesStackPatterns_whenFrameworkMatches", () => {
      createStackPattern(resourcesDir, "quarkus-patterns");
      const config = buildConfig({ framework: "quarkus" });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(
        result.some((f) => f.includes("quarkus-patterns")),
      ).toBe(true);
    });

    it("skipsStackPatterns_whenUnknownFramework", () => {
      const config = buildConfig({ framework: "unknown-fw" });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result.every((f) => !f.includes("patterns"))).toBe(true);
    });

    it.each([
      ["k8s-deployment", { orchestrator: "kubernetes" }],
      ["k8s-helm", { templating: "helm" }],
      ["dockerfile", { container: "docker" }],
      ["iac-terraform", { iac: "terraform" }],
      ["iac-crossplane", { iac: "crossplane" }],
      ["container-registry", { registry: "ecr" }],
      ["k8s-kustomize", { orchestrator: "kubernetes", templating: "kustomize" }],
    ] as const)(
      "copiesInfraPattern_%s_whenConditionTrue",
      (packName, overrides) => {
        createInfraPattern(resourcesDir, packName);
        const config = buildConfig(overrides);
        const engine = new TemplateEngine(resourcesDir, config);
        const result = assembler.assemble(
          config, outputDir, resourcesDir, engine,
        );
        expect(result.some((f) => f.includes(packName))).toBe(true);
      },
    );

    it("skipsInfraPatterns_whenConditionFalse", () => {
      createInfraPattern(resourcesDir, "k8s-deployment");
      const config = buildConfig({ orchestrator: "none" });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(
        result.every((f) => !f.includes("k8s-deployment")),
      ).toBe(true);
    });

    it("fullAssembly_combinedOutput", () => {
      createCoreSkill(resourcesDir, "commit");
      createConditionalSkill(resourcesDir, "x-review-api");
      createKnowledgePack(resourcesDir, "coding-standards");
      createStackPattern(resourcesDir, "quarkus-patterns");
      createInfraPattern(resourcesDir, "dockerfile");
      const config = buildConfig({
        interfaces: [new InterfaceConfig("rest")],
        framework: "quarkus",
        container: "docker",
      });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result.some((f) => f.includes("commit"))).toBe(true);
      expect(result.some((f) => f.includes("x-review-api"))).toBe(true);
      expect(result.some((f) => f.includes("coding-standards"))).toBe(true);
      expect(result.some((f) => f.includes("quarkus-patterns"))).toBe(true);
      expect(result.some((f) => f.includes("dockerfile"))).toBe(true);
    });

    it("knowledgePackWithoutSkillMd_copiesOtherItems", () => {
      const kpDir = path.join(
        resourcesDir, "skills-templates", "knowledge-packs", "security",
      );
      fs.mkdirSync(kpDir, { recursive: true });
      fs.writeFileSync(path.join(kpDir, "overview.md"), "# Security");
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      expect(
        fs.existsSync(
          path.join(outputDir, "skills", "security", "overview.md"),
        ),
      ).toBe(true);
      expect(
        fs.existsSync(
          path.join(outputDir, "skills", "security", "SKILL.md"),
        ),
      ).toBe(false);
    });

    it("copiesNonSkillDirectories", () => {
      const kpDir = path.join(
        resourcesDir, "skills-templates", "knowledge-packs", "compliance",
      );
      const subDir = path.join(kpDir, "frameworks");
      fs.mkdirSync(subDir, { recursive: true });
      fs.writeFileSync(path.join(kpDir, "SKILL.md"), "# Compliance");
      fs.writeFileSync(path.join(subDir, "hipaa.md"), "# HIPAA");
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      expect(
        fs.existsSync(
          path.join(
            outputDir, "skills", "compliance", "frameworks", "hipaa.md",
          ),
        ),
      ).toBe(true);
    });

    it("copiesNonSkillFiles", () => {
      const kpDir = path.join(
        resourcesDir, "skills-templates", "knowledge-packs", "resilience",
      );
      fs.mkdirSync(kpDir, { recursive: true });
      fs.writeFileSync(path.join(kpDir, "SKILL.md"), "# Resilience");
      fs.writeFileSync(path.join(kpDir, "patterns.md"), "# Patterns");
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      expect(
        fs.existsSync(
          path.join(outputDir, "skills", "resilience", "patterns.md"),
        ),
      ).toBe(true);
    });

  });
});
