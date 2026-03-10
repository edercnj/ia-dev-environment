import { describe, it, expect, beforeEach, afterEach } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";
import { tmpdir } from "node:os";
import { RulesAssembler } from "../../../src/assembler/rules-assembler.js";
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
  SecurityConfig,
  TestingConfig,
} from "../../../src/models.js";

function buildFullConfig(overrides: {
  style?: string;
  dbName?: string;
  cacheName?: string;
  securityFrameworks?: string[];
  orchestrator?: string;
  container?: string;
  iac?: string;
} = {}): ProjectConfig {
  return new ProjectConfig(
    new ProjectIdentity("my-app", "A test application"),
    new ArchitectureConfig(overrides.style ?? "microservice", true, false),
    [new InterfaceConfig("rest")],
    new LanguageConfig("java", "21"),
    new FrameworkConfig("quarkus", "3.0", "maven"),
    new DataConfig(
      new TechComponent(overrides.dbName ?? "none"),
      new TechComponent(),
      new TechComponent(overrides.cacheName ?? "none"),
    ),
    new InfraConfig(
      overrides.container ?? "docker",
      overrides.orchestrator ?? "none",
      "kustomize",
      overrides.iac ?? "none",
    ),
    new SecurityConfig(overrides.securityFrameworks ?? []),
    new TestingConfig(),
  );
}

function setupResourcesDir(tmpDir: string): string {
  const resourcesDir = path.join(tmpDir, "resources");
  const coreRules = path.join(resourcesDir, "core-rules");
  const core = path.join(resourcesDir, "core");
  const templates = path.join(resourcesDir, "templates");
  fs.mkdirSync(coreRules, { recursive: true });
  fs.mkdirSync(core, { recursive: true });
  fs.mkdirSync(templates, { recursive: true });

  fs.writeFileSync(
    path.join(coreRules, "03-coding-standards.md"),
    "# Coding Standards for {project_name}",
  );
  fs.writeFileSync(
    path.join(coreRules, "05-quality-gates.md"),
    "# Quality Gates for {language_name}",
  );

  fs.writeFileSync(
    path.join(core, "01-clean-code.md"),
    "# Clean Code",
  );
  fs.writeFileSync(
    path.join(core, "02-solid-principles.md"),
    "# SOLID",
  );
  fs.writeFileSync(
    path.join(core, "03-testing-philosophy.md"),
    "# Testing",
  );

  fs.writeFileSync(
    path.join(templates, "domain-template.md"),
    "# Domain for {project_name}\n\nPurpose: {project_purpose}",
  );

  return resourcesDir;
}

describe("RulesAssembler", () => {
  let tmpDir: string;
  let resourcesDir: string;
  let outputDir: string;
  let assembler: RulesAssembler;

  beforeEach(() => {
    tmpDir = path.join(tmpdir(), `rules-asm-test-${Date.now()}`);
    fs.mkdirSync(tmpDir, { recursive: true });
    resourcesDir = setupResourcesDir(tmpDir);
    outputDir = path.join(tmpDir, "output");
    fs.mkdirSync(outputDir, { recursive: true });
    assembler = new RulesAssembler();
  });

  afterEach(() => {
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  describe("assemble", () => {
    it("createsRulesAndSkillsDirectories", () => {
      const config = buildFullConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      expect(fs.existsSync(path.join(outputDir, "rules"))).toBe(true);
      expect(fs.existsSync(path.join(outputDir, "skills"))).toBe(true);
    });

    it("returnsFilesAndWarnings", () => {
      const config = buildFullConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(config, outputDir, resourcesDir, engine);
      expect(result.files).toBeInstanceOf(Array);
      expect(result.warnings).toBeInstanceOf(Array);
      expect(result.files.length).toBeGreaterThan(0);
    });

    it("generatesProjectIdentityFile", () => {
      const config = buildFullConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const identity = path.join(outputDir, "rules", "01-project-identity.md");
      expect(fs.existsSync(identity)).toBe(true);
      const content = fs.readFileSync(identity, "utf-8");
      expect(content).toContain("# Project Identity — my-app");
      expect(content).toContain("- **Name:** my-app");
    });

    it("generatesDomainTemplateFile", () => {
      const config = buildFullConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const domain = path.join(outputDir, "rules", "02-domain.md");
      expect(fs.existsSync(domain)).toBe(true);
      const content = fs.readFileSync(domain, "utf-8");
      expect(content).toContain("# Domain for my-app");
      expect(content).toContain("Purpose: A test application");
    });
  });

  describe("Layer 1: copyCoreRules", () => {
    it("copiesCoreRulesWithPlaceholderReplacement", () => {
      const config = buildFullConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const rulesDir = path.join(outputDir, "rules");
      const codingStandards = fs.readFileSync(
        path.join(rulesDir, "03-coding-standards.md"), "utf-8",
      );
      expect(codingStandards).toContain("# Coding Standards for my-app");
      expect(codingStandards).not.toContain("{project_name}");
    });

    it("replacesLanguagePlaceholder", () => {
      const config = buildFullConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const qualityGates = fs.readFileSync(
        path.join(outputDir, "rules", "05-quality-gates.md"), "utf-8",
      );
      expect(qualityGates).toContain("# Quality Gates for java");
    });

    it("missingCoreRulesDir_skipsGracefully", () => {
      const emptyRes = path.join(tmpDir, "empty-resources");
      fs.mkdirSync(emptyRes, { recursive: true });
      fs.mkdirSync(path.join(emptyRes, "templates"), { recursive: true });
      fs.writeFileSync(
        path.join(emptyRes, "templates", "domain-template.md"),
        "# Domain",
      );
      const config = buildFullConfig();
      const engine = new TemplateEngine(emptyRes, config);
      const result = assembler.assemble(config, outputDir, emptyRes, engine);
      expect(result.files.length).toBeGreaterThan(0);
    });
  });

  describe("Layer 1b: routeCoreToKps", () => {
    it("routesCoreFilesToKnowledgePacks", () => {
      const config = buildFullConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const codingStd = path.join(
        outputDir, "skills", "coding-standards", "references", "clean-code.md",
      );
      expect(fs.existsSync(codingStd)).toBe(true);
      expect(fs.readFileSync(codingStd, "utf-8")).toBe("# Clean Code");
    });

    it("routesSolidPrinciplesToCodingStandards", () => {
      const config = buildFullConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const solid = path.join(
        outputDir, "skills", "coding-standards", "references", "solid-principles.md",
      );
      expect(fs.existsSync(solid)).toBe(true);
    });

    it("routesTestingPhilosophyToTestingKp", () => {
      const config = buildFullConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const testing = path.join(
        outputDir, "skills", "testing", "references", "testing-philosophy.md",
      );
      expect(fs.existsSync(testing)).toBe(true);
    });

    it("missingCoreSourceFile_skipsGracefully", () => {
      const config = buildFullConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(config, outputDir, resourcesDir, engine);
      expect(result.files).toBeInstanceOf(Array);
    });

    it("missingCoreDir_skipsGracefully", () => {
      fs.rmSync(path.join(resourcesDir, "core"), { recursive: true, force: true });
      const config = buildFullConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(config, outputDir, resourcesDir, engine);
      expect(result.files).toBeInstanceOf(Array);
    });
  });

  describe("Layer 2: copyLanguageKps", () => {
    it("copiesCommonLanguageFiles_routedByName", () => {
      const langCommon = path.join(resourcesDir, "languages", "java", "common");
      fs.mkdirSync(langCommon, { recursive: true });
      fs.writeFileSync(path.join(langCommon, "coding-conventions.md"), "java conventions");
      fs.writeFileSync(path.join(langCommon, "testing-patterns.md"), "java testing");
      const config = buildFullConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const codingRefs = path.join(outputDir, "skills", "coding-standards", "references");
      const testingRefs = path.join(outputDir, "skills", "testing", "references");
      expect(fs.existsSync(path.join(codingRefs, "coding-conventions.md"))).toBe(true);
      expect(fs.existsSync(path.join(testingRefs, "testing-patterns.md"))).toBe(true);
    });

    it("copiesVersionSpecificFiles", () => {
      const versionDir = path.join(resourcesDir, "languages", "java", "java-21");
      fs.mkdirSync(versionDir, { recursive: true });
      fs.writeFileSync(path.join(versionDir, "java21-features.md"), "records, sealed");
      const config = buildFullConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const codingRefs = path.join(outputDir, "skills", "coding-standards", "references");
      expect(fs.existsSync(path.join(codingRefs, "java21-features.md"))).toBe(true);
    });

    it("majorVersionFallback_copiesVersionFiles", () => {
      const versionDir = path.join(resourcesDir, "languages", "java", "java-21.x");
      fs.mkdirSync(versionDir, { recursive: true });
      fs.writeFileSync(path.join(versionDir, "java21x-features.md"), "fallback content");
      const config = new ProjectConfig(
        new ProjectIdentity("test", "test"),
        new ArchitectureConfig("microservice"),
        [new InterfaceConfig("rest")],
        new LanguageConfig("java", "21.0.2"),
        new FrameworkConfig("quarkus", "3.0", "maven"),
      );
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const codingRefs = path.join(outputDir, "skills", "coding-standards", "references");
      expect(fs.existsSync(path.join(codingRefs, "java21x-features.md"))).toBe(true);
    });

    it("missingLanguageDir_skipsGracefully", () => {
      const config = buildFullConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(config, outputDir, resourcesDir, engine);
      expect(result.files).toBeInstanceOf(Array);
    });
  });

  describe("Layer 3: copyFrameworkKps", () => {
    it("copiesCommonFrameworkFiles", () => {
      const fwCommon = path.join(resourcesDir, "frameworks", "quarkus", "common");
      fs.mkdirSync(fwCommon, { recursive: true });
      fs.writeFileSync(path.join(fwCommon, "quarkus-cdi.md"), "CDI patterns");
      const config = buildFullConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const refsDir = path.join(outputDir, "skills", "quarkus-patterns", "references");
      expect(fs.existsSync(path.join(refsDir, "quarkus-cdi.md"))).toBe(true);
    });

    it("copiesVersionSpecificFrameworkFiles", () => {
      const fwVersion = path.join(resourcesDir, "frameworks", "quarkus", "quarkus-3.0");
      fs.mkdirSync(fwVersion, { recursive: true });
      fs.writeFileSync(path.join(fwVersion, "v3-features.md"), "v3 content");
      const config = buildFullConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const refsDir = path.join(outputDir, "skills", "quarkus-patterns", "references");
      expect(fs.existsSync(path.join(refsDir, "v3-features.md"))).toBe(true);
    });

    it("unknownFramework_skipsGracefully", () => {
      const config = new ProjectConfig(
        new ProjectIdentity("test", "test"),
        new ArchitectureConfig("microservice"),
        [new InterfaceConfig("rest")],
        new LanguageConfig("java", "21"),
        new FrameworkConfig("unknown-fw", "1.0", "maven"),
      );
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(config, outputDir, resourcesDir, engine);
      expect(result.files).toBeInstanceOf(Array);
    });

    it("missingFrameworkDir_skipsGracefully", () => {
      const config = buildFullConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(config, outputDir, resourcesDir, engine);
      expect(result.files).toBeInstanceOf(Array);
    });
  });

  describe("Layer 4: generateProjectIdentity", () => {
    it("generatesIdentityWithProjectName", () => {
      const config = buildFullConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const identity = path.join(outputDir, "rules", "01-project-identity.md");
      const content = fs.readFileSync(identity, "utf-8");
      expect(content).toContain("my-app");
      expect(content).toContain("## Technology Stack");
    });
  });

  describe("Layer 4: copyDomainTemplate", () => {
    it("copiesDomainTemplateWithPlaceholders", () => {
      const config = buildFullConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const domain = path.join(outputDir, "rules", "02-domain.md");
      const content = fs.readFileSync(domain, "utf-8");
      expect(content).toContain("# Domain for my-app");
    });

    it("usesFallbackWhenTemplateIsMissing", () => {
      fs.unlinkSync(path.join(resourcesDir, "templates", "domain-template.md"));
      const config = buildFullConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const domain = path.join(outputDir, "rules", "02-domain.md");
      const content = fs.readFileSync(domain, "utf-8");
      expect(content).toContain("{DOMAIN_NAME}");
      expect(content).toContain("my-app");
    });
  });

  describe("Audit", () => {
    it("returnsWarnings_whenTooManyRuleFiles", () => {
      const coreRules = path.join(resourcesDir, "core-rules");
      for (let i = 1; i <= 12; i++) {
        const padded = String(i).padStart(2, "0");
        fs.writeFileSync(path.join(coreRules, `${padded}-rule.md`), "content");
      }
      const config = buildFullConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(config, outputDir, resourcesDir, engine);
      expect(result.warnings.length).toBeGreaterThan(0);
      expect(result.warnings[0]).toContain("rule files exceeds");
    });

    it("noWarnings_whenUnderThreshold", () => {
      const config = buildFullConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(config, outputDir, resourcesDir, engine);
      expect(result.warnings).toEqual([]);
    });
  });

  describe("Conditional: database", () => {
    it("postgresql_copiesDbRefs", () => {
      const sqlCommon = path.join(resourcesDir, "databases", "sql", "common");
      const sqlPg = path.join(resourcesDir, "databases", "sql", "postgresql");
      fs.mkdirSync(sqlCommon, { recursive: true });
      fs.mkdirSync(sqlPg, { recursive: true });
      fs.writeFileSync(path.join(sqlCommon, "sql-basics.md"), "sql basics");
      fs.writeFileSync(path.join(sqlPg, "pg-specific.md"), "pg");
      const config = buildFullConfig({ dbName: "postgresql" });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(config, outputDir, resourcesDir, engine);
      const dbRefs = path.join(outputDir, "skills", "database-patterns", "references");
      expect(fs.existsSync(path.join(dbRefs, "sql-basics.md"))).toBe(true);
      expect(fs.existsSync(path.join(dbRefs, "pg-specific.md"))).toBe(true);
      expect(result.files).toContain(path.join(dbRefs, "sql-basics.md"));
    });
  });

  describe("Conditional: cache", () => {
    it("redis_copiesCacheRefs", () => {
      const cacheCommon = path.join(resourcesDir, "databases", "cache", "common");
      const cacheRedis = path.join(resourcesDir, "databases", "cache", "redis");
      fs.mkdirSync(cacheCommon, { recursive: true });
      fs.mkdirSync(cacheRedis, { recursive: true });
      fs.writeFileSync(path.join(cacheCommon, "cache-basics.md"), "cache");
      fs.writeFileSync(path.join(cacheRedis, "redis.md"), "redis");
      const config = buildFullConfig({ cacheName: "redis" });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(config, outputDir, resourcesDir, engine);
      expect(result.files.some((f) => f.includes("cache-basics.md"))).toBe(true);
    });
  });

  describe("Conditional: security", () => {
    it("withSecurity_copiesSecurityAndCompliance", () => {
      const secDir = path.join(resourcesDir, "security");
      const compDir = path.join(secDir, "compliance");
      fs.mkdirSync(compDir, { recursive: true });
      fs.writeFileSync(path.join(secDir, "application-security.md"), "security");
      fs.writeFileSync(path.join(compDir, "hipaa.md"), "hipaa");
      const config = buildFullConfig({ securityFrameworks: ["hipaa"] });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(config, outputDir, resourcesDir, engine);
      expect(result.files.some((f) => f.includes("application-security.md"))).toBe(true);
      expect(result.files.some((f) => f.includes("hipaa.md"))).toBe(true);
    });
  });

  describe("Conditional: infrastructure", () => {
    it("kubernetes_copiesK8sFiles", () => {
      const k8sDir = path.join(resourcesDir, "infrastructure", "kubernetes");
      fs.mkdirSync(k8sDir, { recursive: true });
      fs.writeFileSync(path.join(k8sDir, "deployment-patterns.md"), "k8s");
      const config = buildFullConfig({ orchestrator: "kubernetes" });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(config, outputDir, resourcesDir, engine);
      expect(result.files.some((f) => f.includes("k8s-deployment.md"))).toBe(true);
    });
  });
});
