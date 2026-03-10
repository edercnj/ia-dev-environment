import { describe, it, expect, beforeEach, afterEach } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";
import { tmpdir } from "node:os";
import {
  SettingsAssembler,
  mergeJsonArrays,
  deduplicate,
  readJsonArray,
  buildSettingsDict,
  buildHooksSection,
} from "../../../src/assembler/settings-assembler.js";
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
  TestingConfig,
} from "../../../src/models.js";

function buildConfig(overrides: {
  language?: string;
  buildTool?: string;
  container?: string;
  orchestrator?: string;
  database?: string;
  cache?: string;
  smokeTests?: boolean;
} = {}): ProjectConfig {
  return new ProjectConfig(
    new ProjectIdentity("my-app", "A test application"),
    new ArchitectureConfig("microservice", false, false),
    [new InterfaceConfig("rest")],
    new LanguageConfig(overrides.language ?? "java", "21"),
    new FrameworkConfig(
      "quarkus", "3.0", overrides.buildTool ?? "maven",
    ),
    new DataConfig(
      new TechComponent(overrides.database ?? "none"),
      new TechComponent(),
      new TechComponent(overrides.cache ?? "none"),
    ),
    new InfraConfig(
      overrides.container ?? "none",
      overrides.orchestrator ?? "none",
    ),
    undefined,
    new TestingConfig(overrides.smokeTests ?? false),
  );
}

function createSettingsTemplate(
  resourcesDir: string,
  filename: string,
  permissions: string[],
): void {
  const dir = path.join(resourcesDir, "settings-templates");
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(
    path.join(dir, filename),
    JSON.stringify(permissions),
    "utf-8",
  );
}

function readSettingsJson(
  outputDir: string,
): Record<string, unknown> {
  const content = fs.readFileSync(
    path.join(outputDir, "settings.json"), "utf-8",
  );
  return JSON.parse(content) as Record<string, unknown>;
}

function getPermissions(outputDir: string): string[] {
  const settings = readSettingsJson(outputDir);
  const perms = settings["permissions"] as { allow: string[] };
  return perms.allow;
}

describe("SettingsAssembler", () => {
  let tmpDir: string;
  let resourcesDir: string;
  let outputDir: string;
  let assembler: SettingsAssembler;

  beforeEach(() => {
    tmpDir = fs.mkdtempSync(
      path.join(tmpdir(), "settings-asm-test-"),
    );
    resourcesDir = path.join(tmpDir, "resources");
    fs.mkdirSync(resourcesDir, { recursive: true });
    outputDir = path.join(tmpDir, "output");
    fs.mkdirSync(outputDir, { recursive: true });
    assembler = new SettingsAssembler();
  });

  afterEach(() => {
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  describe("mergeJsonArrays", () => {
    it("mergeJsonArrays_twoNonEmptyArrays_returnsConcatenation", () => {
      expect(mergeJsonArrays(["a", "b"], ["c", "d"]))
        .toEqual(["a", "b", "c", "d"]);
    });

    it("mergeJsonArrays_emptyOverlay_returnsBaseUnchanged", () => {
      expect(mergeJsonArrays(["a", "b"], []))
        .toEqual(["a", "b"]);
    });

    it("mergeJsonArrays_emptyBase_returnsOverlay", () => {
      expect(mergeJsonArrays([], ["c", "d"]))
        .toEqual(["c", "d"]);
    });

    it("mergeJsonArrays_bothEmpty_returnsEmptyArray", () => {
      expect(mergeJsonArrays([], [])).toEqual([]);
    });
  });

  describe("deduplicate", () => {
    it("deduplicate_duplicateEntries_removedPreservingInsertionOrder", () => {
      expect(deduplicate(["a", "b", "a", "c", "b"]))
        .toEqual(["a", "b", "c"]);
    });

    it("deduplicate_noDuplicates_returnsOriginalOrder", () => {
      expect(deduplicate(["x", "y", "z"]))
        .toEqual(["x", "y", "z"]);
    });

    it("deduplicate_emptyArray_returnsEmptyArray", () => {
      expect(deduplicate([])).toEqual([]);
    });

    it("deduplicate_singleElement_returnsSameElement", () => {
      expect(deduplicate(["only"])).toEqual(["only"]);
    });
  });

  describe("readJsonArray", () => {
    it("readJsonArray_validJsonArray_returnsArray", () => {
      const filePath = path.join(tmpDir, "valid.json");
      fs.writeFileSync(
        filePath,
        JSON.stringify(["Bash(git *)", "Bash(npm *)"]),
        "utf-8",
      );
      expect(readJsonArray(filePath))
        .toEqual(["Bash(git *)", "Bash(npm *)"]);
    });

    it("readJsonArray_malformedJson_returnsEmptyArray", () => {
      const filePath = path.join(tmpDir, "bad.json");
      fs.writeFileSync(filePath, "{invalid json", "utf-8");
      expect(readJsonArray(filePath)).toEqual([]);
    });

    it("readJsonArray_jsonObjectNotArray_returnsEmptyArray", () => {
      const filePath = path.join(tmpDir, "obj.json");
      fs.writeFileSync(
        filePath, JSON.stringify({ key: "value" }), "utf-8",
      );
      expect(readJsonArray(filePath)).toEqual([]);
    });

    it("readJsonArray_emptyJsonArray_returnsEmptyArray", () => {
      const filePath = path.join(tmpDir, "empty.json");
      fs.writeFileSync(filePath, "[]", "utf-8");
      expect(readJsonArray(filePath)).toEqual([]);
    });

    it("readJsonArray_missingFile_returnsEmptyArray", () => {
      const filePath = path.join(tmpDir, "nonexistent.json");
      expect(readJsonArray(filePath)).toEqual([]);
    });
  });

  describe("buildSettingsDict", () => {
    it("buildSettingsDict_withHooksTrue_includesHooksSection", () => {
      const result = buildSettingsDict(["perm1", "perm2"], true);
      expect(result.permissions.allow).toEqual(["perm1", "perm2"]);
      expect(result.hooks).toBeDefined();
      expect(result.hooks!.PostToolUse).toHaveLength(1);
    });

    it("buildSettingsDict_withHooksFalse_omitsHooksSection", () => {
      const result = buildSettingsDict(["perm1"], false);
      expect(result.permissions.allow).toEqual(["perm1"]);
      expect(result.hooks).toBeUndefined();
    });

    it("buildSettingsDict_emptyPermissions_outputsEmptyAllowArray", () => {
      const result = buildSettingsDict([], false);
      expect(result).toEqual({ permissions: { allow: [] } });
    });
  });

  describe("buildHooksSection", () => {
    it("buildHooksSection_returnsCorrectStructure", () => {
      const section = buildHooksSection();
      expect(section.PostToolUse).toHaveLength(1);
      expect(section.PostToolUse[0]!.matcher).toBe("Write|Edit");
      expect(section.PostToolUse[0]!.hooks).toHaveLength(1);
      const hook = section.PostToolUse[0]!.hooks[0]!;
      expect(hook.type).toBe("command");
      expect(hook.timeout).toBe(60);
      expect(hook.statusMessage).toBe("Checking compilation...");
    });

    it("buildHooksSection_commandStringContainsClaudeProjectDirVariable", () => {
      const section = buildHooksSection();
      const cmd = section.PostToolUse[0]!.hooks[0]!.command;
      expect(cmd).toContain('"$CLAUDE_PROJECT_DIR"');
      expect(cmd).toBe(
        '"$CLAUDE_PROJECT_DIR"/.claude/hooks/post-compile-check.sh',
      );
    });
  });

  describe("assemble — base permissions", () => {
    it("assemble_basePermissions_alwaysLoaded", () => {
      createSettingsTemplate(
        resourcesDir, "base.json", ["Bash(git *)"],
      );
      const config = buildConfig({
        language: "ruby", buildTool: "bundler",
      });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      expect(getPermissions(outputDir)).toContain("Bash(git *)");
    });
  });

  describe("assemble — language permissions", () => {
    it.each([
      ["java", "maven", "java-maven"],
      ["java", "gradle", "java-gradle"],
      ["kotlin", "gradle", "java-gradle"],
      ["typescript", "npm", "typescript-npm"],
      ["python", "pip", "python-pip"],
      ["go", "go", "go"],
      ["rust", "cargo", "rust-cargo"],
      ["csharp", "dotnet", "csharp-dotnet"],
    ])(
      "assemble_%s_%s_mergesLangPermissions",
      (language, buildTool, settingsKey) => {
        createSettingsTemplate(
          resourcesDir, "base.json", ["base-perm"],
        );
        createSettingsTemplate(
          resourcesDir, `${settingsKey}.json`, ["lang-perm"],
        );
        const config = buildConfig({ language, buildTool });
        const engine = new TemplateEngine(resourcesDir, config);
        assembler.assemble(config, outputDir, resourcesDir, engine);
        const perms = getPermissions(outputDir);
        expect(perms).toContain("base-perm");
        expect(perms).toContain("lang-perm");
      },
    );
  });

  describe("assemble — infrastructure permissions", () => {
    it("assemble_dockerContainer_mergesDockerPermissions", () => {
      createSettingsTemplate(resourcesDir, "base.json", ["base"]);
      createSettingsTemplate(
        resourcesDir, "docker.json", ["Bash(docker *)"],
      );
      const config = buildConfig({ container: "docker" });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      expect(getPermissions(outputDir)).toContain("Bash(docker *)");
    });

    it("assemble_podmanContainer_mergesDockerPermissions", () => {
      createSettingsTemplate(resourcesDir, "base.json", ["base"]);
      createSettingsTemplate(
        resourcesDir, "docker.json", ["Bash(docker *)"],
      );
      const config = buildConfig({ container: "podman" });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      expect(getPermissions(outputDir)).toContain("Bash(docker *)");
    });

    it("assemble_noneContainer_doesNotLoadDockerPermissions", () => {
      createSettingsTemplate(resourcesDir, "base.json", ["base"]);
      createSettingsTemplate(
        resourcesDir, "docker.json", ["Bash(docker *)"],
      );
      const config = buildConfig({ container: "none" });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      expect(getPermissions(outputDir)).not.toContain("Bash(docker *)");
    });

    it("assemble_kubernetesOrchestrator_mergesK8sPermissions", () => {
      createSettingsTemplate(resourcesDir, "base.json", ["base"]);
      createSettingsTemplate(
        resourcesDir, "kubernetes.json", ["Bash(kubectl *)"],
      );
      const config = buildConfig({ orchestrator: "kubernetes" });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      expect(getPermissions(outputDir)).toContain("Bash(kubectl *)");
    });

    it("assemble_dockerComposeOrchestrator_mergesComposePerms", () => {
      createSettingsTemplate(resourcesDir, "base.json", ["base"]);
      createSettingsTemplate(
        resourcesDir,
        "docker-compose.json",
        ["Bash(docker compose *)"],
      );
      const config = buildConfig({ orchestrator: "docker-compose" });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      expect(getPermissions(outputDir))
        .toContain("Bash(docker compose *)");
    });

    it("assemble_noneOrchestrator_doesNotLoadOrchestratorPerms", () => {
      createSettingsTemplate(resourcesDir, "base.json", ["base"]);
      createSettingsTemplate(
        resourcesDir, "kubernetes.json", ["Bash(kubectl *)"],
      );
      createSettingsTemplate(
        resourcesDir,
        "docker-compose.json",
        ["Bash(docker compose *)"],
      );
      const config = buildConfig({ orchestrator: "none" });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const perms = getPermissions(outputDir);
      expect(perms).not.toContain("Bash(kubectl *)");
      expect(perms).not.toContain("Bash(docker compose *)");
    });

    it("assemble_dockerPlusKubernetes_mergesBothInfra", () => {
      createSettingsTemplate(resourcesDir, "base.json", ["base"]);
      createSettingsTemplate(
        resourcesDir, "docker.json", ["docker-perm"],
      );
      createSettingsTemplate(
        resourcesDir, "kubernetes.json", ["k8s-perm"],
      );
      const config = buildConfig({
        container: "docker", orchestrator: "kubernetes",
      });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const perms = getPermissions(outputDir);
      expect(perms).toContain("docker-perm");
      expect(perms).toContain("k8s-perm");
    });

    it("assemble_podmanPlusDockerCompose_mergesBothInfra", () => {
      createSettingsTemplate(resourcesDir, "base.json", ["base"]);
      createSettingsTemplate(
        resourcesDir, "docker.json", ["docker-perm"],
      );
      createSettingsTemplate(
        resourcesDir, "docker-compose.json", ["compose-perm"],
      );
      const config = buildConfig({
        container: "podman", orchestrator: "docker-compose",
      });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const perms = getPermissions(outputDir);
      expect(perms).toContain("docker-perm");
      expect(perms).toContain("compose-perm");
    });
  });

  describe("assemble — data permissions", () => {
    it.each([
      ["postgresql", "database-psql"],
      ["mysql", "database-mysql"],
      ["oracle", "database-oracle"],
      ["mongodb", "database-mongodb"],
      ["cassandra", "database-cassandra"],
    ])(
      "assemble_%s_mergesDbPermissions",
      (database, settingsKey) => {
        createSettingsTemplate(resourcesDir, "base.json", ["base"]);
        createSettingsTemplate(
          resourcesDir, `${settingsKey}.json`, ["db-perm"],
        );
        const config = buildConfig({ database });
        const engine = new TemplateEngine(resourcesDir, config);
        assembler.assemble(config, outputDir, resourcesDir, engine);
        expect(getPermissions(outputDir)).toContain("db-perm");
      },
    );

    it("assemble_noneDatabase_doesNotLoadAnyDbPermissions", () => {
      createSettingsTemplate(resourcesDir, "base.json", ["base"]);
      createSettingsTemplate(
        resourcesDir, "database-psql.json", ["Bash(psql *)"],
      );
      const config = buildConfig({ database: "none" });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      expect(getPermissions(outputDir)).not.toContain("Bash(psql *)");
    });

    it.each([
      ["redis", "cache-redis"],
      ["dragonfly", "cache-dragonfly"],
      ["memcached", "cache-memcached"],
    ])(
      "assemble_%s_mergesCachePermissions",
      (cache, settingsKey) => {
        createSettingsTemplate(resourcesDir, "base.json", ["base"]);
        createSettingsTemplate(
          resourcesDir, `${settingsKey}.json`, ["cache-perm"],
        );
        const config = buildConfig({ cache });
        const engine = new TemplateEngine(resourcesDir, config);
        assembler.assemble(config, outputDir, resourcesDir, engine);
        expect(getPermissions(outputDir)).toContain("cache-perm");
      },
    );

    it("assemble_noneCache_doesNotLoadAnyCachePermissions", () => {
      createSettingsTemplate(resourcesDir, "base.json", ["base"]);
      const config = buildConfig({ cache: "none" });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      expect(getPermissions(outputDir)).toEqual(["base"]);
    });

    it("assemble_postgresAndRedis_mergesBothData", () => {
      createSettingsTemplate(resourcesDir, "base.json", ["base"]);
      createSettingsTemplate(
        resourcesDir, "database-psql.json", ["db-perm"],
      );
      createSettingsTemplate(
        resourcesDir, "cache-redis.json", ["cache-perm"],
      );
      const config = buildConfig({
        database: "postgresql", cache: "redis",
      });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const perms = getPermissions(outputDir);
      expect(perms).toContain("db-perm");
      expect(perms).toContain("cache-perm");
    });
  });

  describe("assemble — testing permissions", () => {
    it("assemble_smokeTestsEnabled_mergesNewmanPermissions", () => {
      createSettingsTemplate(resourcesDir, "base.json", ["base"]);
      createSettingsTemplate(
        resourcesDir, "testing-newman.json", ["Bash(newman *)"],
      );
      const config = buildConfig({ smokeTests: true });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      expect(getPermissions(outputDir)).toContain("Bash(newman *)");
    });

    it("assemble_smokeTestsDisabled_doesNotLoadNewman", () => {
      createSettingsTemplate(resourcesDir, "base.json", ["base"]);
      createSettingsTemplate(
        resourcesDir, "testing-newman.json", ["Bash(newman *)"],
      );
      const config = buildConfig({ smokeTests: false });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      expect(getPermissions(outputDir))
        .not.toContain("Bash(newman *)");
    });
  });

  describe("assemble — multi-source merge and deduplication", () => {
    it("assemble_multipleSourcesMerged_allPermissionsPresent", () => {
      createSettingsTemplate(
        resourcesDir, "base.json", ["perm-base"],
      );
      createSettingsTemplate(
        resourcesDir, "java-maven.json", ["perm-java"],
      );
      createSettingsTemplate(
        resourcesDir, "docker.json", ["perm-docker"],
      );
      createSettingsTemplate(
        resourcesDir, "database-psql.json", ["perm-db"],
      );
      createSettingsTemplate(
        resourcesDir, "cache-redis.json", ["perm-cache"],
      );
      createSettingsTemplate(
        resourcesDir, "testing-newman.json", ["perm-newman"],
      );
      const config = buildConfig({
        container: "docker",
        database: "postgresql",
        cache: "redis",
        smokeTests: true,
      });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const perms = getPermissions(outputDir);
      expect(perms).toEqual([
        "perm-base", "perm-java", "perm-docker",
        "perm-db", "perm-cache", "perm-newman",
      ]);
    });

    it("assemble_overlappingPermissions_deduplicated", () => {
      createSettingsTemplate(
        resourcesDir, "base.json", ["Bash(git *)", "common"],
      );
      createSettingsTemplate(
        resourcesDir, "java-maven.json", ["common", "Bash(mvn *)"],
      );
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      expect(getPermissions(outputDir))
        .toEqual(["Bash(git *)", "common", "Bash(mvn *)"]);
    });

    it("assemble_unknownLanguage_basePermissionsOnly", () => {
      createSettingsTemplate(
        resourcesDir, "base.json", ["base-only"],
      );
      const config = buildConfig({
        language: "ruby", buildTool: "bundler",
      });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      expect(getPermissions(outputDir)).toEqual(["base-only"]);
    });
  });

  describe("assemble — hooks section in settings.json", () => {
    it.each([
      ["java", "maven", true],
      ["java", "gradle", true],
      ["kotlin", "gradle", true],
      ["typescript", "npm", true],
      ["go", "go", true],
      ["rust", "cargo", true],
      ["csharp", "dotnet", true],
      ["python", "pip", false],
    ])(
      "assemble_%s_%s_hooksPresence_%s",
      (language, buildTool, hasHooks) => {
        createSettingsTemplate(resourcesDir, "base.json", ["base"]);
        const config = buildConfig({ language, buildTool });
        const engine = new TemplateEngine(resourcesDir, config);
        assembler.assemble(config, outputDir, resourcesDir, engine);
        const settings = readSettingsJson(outputDir);
        if (hasHooks) {
          expect(settings).toHaveProperty("hooks");
        } else {
          expect(settings).not.toHaveProperty("hooks");
        }
      },
    );
  });

  describe("assemble — settings.json format", () => {
    it("assemble_settingsJson_twoSpaceIndentation", () => {
      createSettingsTemplate(resourcesDir, "base.json", ["perm"]);
      const config = buildConfig({
        language: "python", buildTool: "pip",
      });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const raw = fs.readFileSync(
        path.join(outputDir, "settings.json"), "utf-8",
      );
      expect(raw).toContain('{\n  "permissions"');
      expect(raw).not.toContain("\t");
    });

    it("assemble_settingsJson_endsWithTrailingNewline", () => {
      createSettingsTemplate(resourcesDir, "base.json", ["perm"]);
      const config = buildConfig({
        language: "python", buildTool: "pip",
      });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const raw = fs.readFileSync(
        path.join(outputDir, "settings.json"), "utf-8",
      );
      expect(raw.endsWith("\n")).toBe(true);
    });

    it("assemble_settingsJson_validJsonParseable", () => {
      createSettingsTemplate(resourcesDir, "base.json", ["perm"]);
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const raw = fs.readFileSync(
        path.join(outputDir, "settings.json"), "utf-8",
      );
      expect(() => JSON.parse(raw)).not.toThrow();
    });
  });

  describe("assemble — settings.local.json", () => {
    it("assemble_settingsLocalJson_emptyPermissionsTemplate", () => {
      createSettingsTemplate(resourcesDir, "base.json", ["base"]);
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const localPath = path.join(
        outputDir, "settings.local.json",
      );
      expect(fs.existsSync(localPath)).toBe(true);
      const parsed = JSON.parse(
        fs.readFileSync(localPath, "utf-8"),
      ) as Record<string, unknown>;
      expect(parsed).toEqual({ permissions: { allow: [] } });
    });

    it("assemble_settingsLocalJson_twoSpaceIndentTrailingNewline", () => {
      createSettingsTemplate(resourcesDir, "base.json", ["base"]);
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const raw = fs.readFileSync(
        path.join(outputDir, "settings.local.json"), "utf-8",
      );
      expect(raw).toContain('{\n  "permissions"');
      expect(raw.endsWith("\n")).toBe(true);
    });
  });

  describe("assemble — return value", () => {
    it("assemble_returnsBothFilePaths", () => {
      createSettingsTemplate(resourcesDir, "base.json", ["base"]);
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result).toHaveLength(2);
      expect(result[0]).toBe(
        path.join(outputDir, "settings.json"),
      );
      expect(result[1]).toBe(
        path.join(outputDir, "settings.local.json"),
      );
    });
  });

  describe("assemble — missing optional template files", () => {
    it("assemble_missingLangFile_skipsGracefully", () => {
      createSettingsTemplate(resourcesDir, "base.json", ["base"]);
      const config = buildConfig();
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      expect(getPermissions(outputDir)).toEqual(["base"]);
    });

    it("assemble_missingBaseFile_emptyPermissions", () => {
      const dir = path.join(resourcesDir, "settings-templates");
      fs.mkdirSync(dir, { recursive: true });
      const config = buildConfig({
        language: "ruby", buildTool: "bundler",
      });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      expect(getPermissions(outputDir)).toEqual([]);
    });

    it("assemble_missingInfraFiles_skipsGracefully", () => {
      createSettingsTemplate(resourcesDir, "base.json", ["base"]);
      const config = buildConfig({
        container: "docker", orchestrator: "kubernetes",
      });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      expect(getPermissions(outputDir)).toEqual(["base"]);
    });
  });
});
