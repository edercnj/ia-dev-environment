import { describe, it, expect, beforeEach, afterEach } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";
import { tmpdir } from "node:os";
import {
  copyMdDir,
  copyDatabaseRefs,
  copyCacheRefs,
  assembleSecurityRules,
  assembleCloudKnowledge,
  assembleInfraKnowledge,
} from "../../../src/assembler/rules-conditionals.js";
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
import { TemplateEngine } from "../../../src/template-engine.js";

function buildConfig(overrides: {
  dbName?: string;
  cacheName?: string;
  securityFrameworks?: string[];
  orchestrator?: string;
  container?: string;
  iac?: string;
} = {}): ProjectConfig {
  return new ProjectConfig(
    new ProjectIdentity("test", "test purpose"),
    new ArchitectureConfig("microservice"),
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

describe("copyMdDir", () => {
  let tmpDir: string;

  beforeEach(() => {
    tmpDir = path.join(tmpdir(), `copymd-test-${Date.now()}`);
    fs.mkdirSync(tmpDir, { recursive: true });
  });

  afterEach(() => {
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  it("copiesMdFiles_fromSourceToTarget", () => {
    const src = path.join(tmpDir, "src");
    const tgt = path.join(tmpDir, "tgt");
    fs.mkdirSync(src, { recursive: true });
    fs.mkdirSync(tgt, { recursive: true });
    fs.writeFileSync(path.join(src, "a.md"), "alpha");
    fs.writeFileSync(path.join(src, "b.md"), "beta");
    fs.writeFileSync(path.join(src, "c.txt"), "ignored");
    const result = copyMdDir(src, tgt);
    expect(result).toHaveLength(2);
    expect(fs.readFileSync(path.join(tgt, "a.md"), "utf-8")).toBe("alpha");
    expect(fs.readFileSync(path.join(tgt, "b.md"), "utf-8")).toBe("beta");
    expect(fs.existsSync(path.join(tgt, "c.txt"))).toBe(false);
  });

  it("missingSourceDir_returnsEmpty", () => {
    const result = copyMdDir(path.join(tmpDir, "nope"), path.join(tmpDir, "tgt"));
    expect(result).toEqual([]);
  });

  it("skipsNonFileEntries_inSourceDir", () => {
    const src = path.join(tmpDir, "src");
    const tgt = path.join(tmpDir, "tgt");
    fs.mkdirSync(src, { recursive: true });
    fs.mkdirSync(tgt, { recursive: true });
    fs.writeFileSync(path.join(src, "real.md"), "content");
    fs.mkdirSync(path.join(src, "subdir.md"));
    const result = copyMdDir(src, tgt);
    expect(result).toHaveLength(1);
    expect(fs.readFileSync(path.join(tgt, "real.md"), "utf-8")).toBe("content");
  });

  it("emptySourceDir_returnsEmpty", () => {
    const src = path.join(tmpDir, "empty");
    fs.mkdirSync(src, { recursive: true });
    const tgt = path.join(tmpDir, "tgt");
    fs.mkdirSync(tgt, { recursive: true });
    const result = copyMdDir(src, tgt);
    expect(result).toEqual([]);
  });
});

describe("copyDatabaseRefs", () => {
  let tmpDir: string;
  let resourcesDir: string;
  let skillsDir: string;

  beforeEach(() => {
    tmpDir = path.join(tmpdir(), `db-refs-test-${Date.now()}`);
    resourcesDir = path.join(tmpDir, "resources");
    skillsDir = path.join(tmpDir, "skills");
    fs.mkdirSync(resourcesDir, { recursive: true });
    fs.mkdirSync(skillsDir, { recursive: true });
  });

  afterEach(() => {
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  it("databaseNone_returnsEmpty", () => {
    const config = buildConfig({ dbName: "none" });
    const engine = new TemplateEngine(resourcesDir, config);
    const result = copyDatabaseRefs(config, resourcesDir, skillsDir, engine);
    expect(result).toEqual([]);
  });

  it("sqlDatabase_copiesSqlCommonAndSpecific", () => {
    const sqlCommon = path.join(resourcesDir, "databases", "sql", "common");
    const sqlPg = path.join(resourcesDir, "databases", "sql", "postgresql");
    fs.mkdirSync(sqlCommon, { recursive: true });
    fs.mkdirSync(sqlPg, { recursive: true });
    fs.writeFileSync(path.join(sqlCommon, "sql-common.md"), "common sql");
    fs.writeFileSync(path.join(sqlPg, "pg-specific.md"), "pg specific");
    const config = buildConfig({ dbName: "postgresql" });
    const engine = new TemplateEngine(resourcesDir, config);
    const result = copyDatabaseRefs(config, resourcesDir, skillsDir, engine);
    expect(result.length).toBeGreaterThanOrEqual(2);
    const target = path.join(skillsDir, "database-patterns", "references");
    expect(fs.existsSync(path.join(target, "sql-common.md"))).toBe(true);
    expect(fs.existsSync(path.join(target, "pg-specific.md"))).toBe(true);
  });

  it("nosqlDatabase_copiesNosqlFiles", () => {
    const nosqlCommon = path.join(resourcesDir, "databases", "nosql", "common");
    const nosqlMongo = path.join(resourcesDir, "databases", "nosql", "mongodb");
    fs.mkdirSync(nosqlCommon, { recursive: true });
    fs.mkdirSync(nosqlMongo, { recursive: true });
    fs.writeFileSync(path.join(nosqlCommon, "nosql-common.md"), "nosql common");
    fs.writeFileSync(path.join(nosqlMongo, "mongo.md"), "mongo");
    const config = buildConfig({ dbName: "mongodb" });
    const engine = new TemplateEngine(resourcesDir, config);
    const result = copyDatabaseRefs(config, resourcesDir, skillsDir, engine);
    expect(result.length).toBeGreaterThanOrEqual(2);
  });

  it("copiesVersionMatrix_whenExists", () => {
    const dbDir = path.join(resourcesDir, "databases");
    fs.mkdirSync(dbDir, { recursive: true });
    fs.writeFileSync(path.join(dbDir, "version-matrix.md"), "matrix content");
    const config = buildConfig({ dbName: "postgresql" });
    const engine = new TemplateEngine(resourcesDir, config);
    const result = copyDatabaseRefs(config, resourcesDir, skillsDir, engine);
    const target = path.join(skillsDir, "database-patterns", "references", "version-matrix.md");
    expect(result).toContain(target);
  });
});

describe("copyCacheRefs", () => {
  let tmpDir: string;
  let resourcesDir: string;
  let skillsDir: string;

  beforeEach(() => {
    tmpDir = path.join(tmpdir(), `cache-refs-test-${Date.now()}`);
    resourcesDir = path.join(tmpDir, "resources");
    skillsDir = path.join(tmpDir, "skills");
    fs.mkdirSync(resourcesDir, { recursive: true });
    fs.mkdirSync(skillsDir, { recursive: true });
  });

  afterEach(() => {
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  it("cacheNone_returnsEmpty", () => {
    const config = buildConfig({ cacheName: "none" });
    const result = copyCacheRefs(config, resourcesDir, skillsDir);
    expect(result).toEqual([]);
  });

  it("redisCache_copiesCacheFiles", () => {
    const cacheCommon = path.join(resourcesDir, "databases", "cache", "common");
    const cacheRedis = path.join(resourcesDir, "databases", "cache", "redis");
    fs.mkdirSync(cacheCommon, { recursive: true });
    fs.mkdirSync(cacheRedis, { recursive: true });
    fs.writeFileSync(path.join(cacheCommon, "cache-common.md"), "common cache");
    fs.writeFileSync(path.join(cacheRedis, "redis.md"), "redis specific");
    const config = buildConfig({ cacheName: "redis" });
    const result = copyCacheRefs(config, resourcesDir, skillsDir);
    expect(result.length).toBeGreaterThanOrEqual(2);
  });
});

describe("assembleSecurityRules", () => {
  let tmpDir: string;
  let resourcesDir: string;
  let skillsDir: string;

  beforeEach(() => {
    tmpDir = path.join(tmpdir(), `sec-test-${Date.now()}`);
    resourcesDir = path.join(tmpDir, "resources");
    skillsDir = path.join(tmpDir, "skills");
    fs.mkdirSync(resourcesDir, { recursive: true });
    fs.mkdirSync(skillsDir, { recursive: true });
  });

  afterEach(() => {
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  it("noFrameworks_returnsEmpty", () => {
    const config = buildConfig({ securityFrameworks: [] });
    const result = assembleSecurityRules(config, resourcesDir, skillsDir);
    expect(result).toEqual([]);
  });

  it("withFrameworks_copiesSecurityBaseAndCompliance", () => {
    const secDir = path.join(resourcesDir, "security");
    const compDir = path.join(secDir, "compliance");
    fs.mkdirSync(compDir, { recursive: true });
    fs.writeFileSync(path.join(secDir, "application-security.md"), "sec");
    fs.writeFileSync(path.join(secDir, "cryptography.md"), "crypto");
    fs.writeFileSync(path.join(compDir, "gdpr.md"), "gdpr content");
    const config = buildConfig({ securityFrameworks: ["gdpr"] });
    const result = assembleSecurityRules(config, resourcesDir, skillsDir);
    expect(result).toHaveLength(3);
    expect(fs.existsSync(path.join(skillsDir, "security", "references", "application-security.md"))).toBe(true);
    expect(fs.existsSync(path.join(skillsDir, "compliance", "references", "gdpr.md"))).toBe(true);
  });

  it("missingComplianceFile_skipsGracefully", () => {
    const secDir = path.join(resourcesDir, "security");
    fs.mkdirSync(secDir, { recursive: true });
    fs.writeFileSync(path.join(secDir, "application-security.md"), "sec");
    const config = buildConfig({ securityFrameworks: ["nonexistent"] });
    const result = assembleSecurityRules(config, resourcesDir, skillsDir);
    expect(result).toHaveLength(1);
  });
});

describe("assembleCloudKnowledge", () => {
  let tmpDir: string;
  let resourcesDir: string;
  let skillsDir: string;

  beforeEach(() => {
    tmpDir = path.join(tmpdir(), `cloud-test-${Date.now()}`);
    resourcesDir = path.join(tmpDir, "resources");
    skillsDir = path.join(tmpDir, "skills");
    fs.mkdirSync(resourcesDir, { recursive: true });
    fs.mkdirSync(skillsDir, { recursive: true });
  });

  afterEach(() => {
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  it("noCloudProvider_returnsEmpty", () => {
    const config = buildConfig();
    const result = assembleCloudKnowledge(config, resourcesDir, skillsDir);
    expect(result).toEqual([]);
  });

  it("cloudProviderPresent_copiesFile", () => {
    const cloudDir = path.join(resourcesDir, "cloud-providers");
    fs.mkdirSync(cloudDir, { recursive: true });
    fs.writeFileSync(path.join(cloudDir, "aws.md"), "aws content");
    const config = buildConfig() as unknown as Record<string, unknown>;
    const infra = (config as unknown as { infrastructure: Record<string, unknown> }).infrastructure;
    infra["cloudProvider"] = "aws";
    const result = assembleCloudKnowledge(
      config as unknown as ProjectConfig, resourcesDir, skillsDir,
    );
    expect(result).toHaveLength(1);
    expect(fs.existsSync(path.join(skillsDir, "knowledge-packs", "cloud-aws.md"))).toBe(true);
  });

  it("cloudProviderPresent_noSourceFile_returnsEmpty", () => {
    const config = buildConfig() as unknown as Record<string, unknown>;
    const infra = (config as unknown as { infrastructure: Record<string, unknown> }).infrastructure;
    infra["cloudProvider"] = "gcp";
    const result = assembleCloudKnowledge(
      config as unknown as ProjectConfig, resourcesDir, skillsDir,
    );
    expect(result).toEqual([]);
  });
});

describe("assembleInfraKnowledge", () => {
  let tmpDir: string;
  let resourcesDir: string;
  let skillsDir: string;

  beforeEach(() => {
    tmpDir = path.join(tmpdir(), `infra-test-${Date.now()}`);
    resourcesDir = path.join(tmpDir, "resources");
    skillsDir = path.join(tmpDir, "skills");
    fs.mkdirSync(resourcesDir, { recursive: true });
    fs.mkdirSync(skillsDir, { recursive: true });
  });

  afterEach(() => {
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  it("noKubernetes_skipsK8s", () => {
    const config = buildConfig({ orchestrator: "none" });
    const result = assembleInfraKnowledge(config, resourcesDir, skillsDir);
    expect(result).toEqual([]);
  });

  it("kubernetes_noSourceFile_returnsEmpty", () => {
    const k8sDir = path.join(resourcesDir, "infrastructure", "kubernetes");
    fs.mkdirSync(k8sDir, { recursive: true });
    const config = buildConfig({ orchestrator: "kubernetes" });
    const result = assembleInfraKnowledge(config, resourcesDir, skillsDir);
    expect(result).toEqual([]);
  });

  it("kubernetes_copiesDeploymentPatterns", () => {
    const k8sDir = path.join(resourcesDir, "infrastructure", "kubernetes");
    fs.mkdirSync(k8sDir, { recursive: true });
    fs.writeFileSync(path.join(k8sDir, "deployment-patterns.md"), "k8s content");
    const config = buildConfig({ orchestrator: "kubernetes" });
    const result = assembleInfraKnowledge(config, resourcesDir, skillsDir);
    expect(result).toHaveLength(1);
    expect(fs.existsSync(path.join(skillsDir, "knowledge-packs", "k8s-deployment.md"))).toBe(true);
  });

  it("dockerContainer_copiesContainerFiles", () => {
    const containerDir = path.join(resourcesDir, "infrastructure", "containers");
    fs.mkdirSync(containerDir, { recursive: true });
    fs.writeFileSync(path.join(containerDir, "dockerfile-patterns.md"), "docker");
    fs.writeFileSync(path.join(containerDir, "registry-patterns.md"), "registry");
    const config = buildConfig({ container: "docker" });
    const result = assembleInfraKnowledge(config, resourcesDir, skillsDir);
    expect(result).toHaveLength(2);
    expect(fs.existsSync(path.join(skillsDir, "knowledge-packs", "dockerfile.md"))).toBe(true);
    expect(fs.existsSync(path.join(skillsDir, "knowledge-packs", "registry.md"))).toBe(true);
  });

  it("containerNone_skipsContainerFiles", () => {
    const containerDir = path.join(resourcesDir, "infrastructure", "containers");
    fs.mkdirSync(containerDir, { recursive: true });
    fs.writeFileSync(path.join(containerDir, "dockerfile-patterns.md"), "docker");
    const config = buildConfig({ container: "none" });
    const result = assembleInfraKnowledge(config, resourcesDir, skillsDir);
    expect(result).toEqual([]);
  });

  it("iacTerraform_copiesIacFiles", () => {
    const iacDir = path.join(resourcesDir, "infrastructure", "iac");
    fs.mkdirSync(iacDir, { recursive: true });
    fs.writeFileSync(path.join(iacDir, "terraform-patterns.md"), "terraform");
    const config = buildConfig({ iac: "terraform" });
    const result = assembleInfraKnowledge(config, resourcesDir, skillsDir);
    expect(result).toHaveLength(1);
    expect(fs.existsSync(path.join(skillsDir, "knowledge-packs", "iac-terraform.md"))).toBe(true);
  });

  it("iacTerraform_noSourceFile_returnsEmpty", () => {
    const iacDir = path.join(resourcesDir, "infrastructure", "iac");
    fs.mkdirSync(iacDir, { recursive: true });
    const config = buildConfig({ iac: "terraform" });
    const result = assembleInfraKnowledge(config, resourcesDir, skillsDir);
    expect(result).toEqual([]);
  });

  it("iacNone_skipsIacFiles", () => {
    const config = buildConfig({ iac: "none" });
    const result = assembleInfraKnowledge(config, resourcesDir, skillsDir);
    expect(result).toEqual([]);
  });
});
