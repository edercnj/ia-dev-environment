import { describe, it, expect, beforeEach, afterEach } from "vitest";
import * as fs from "node:fs";
import * as path from "node:path";
import { tmpdir } from "node:os";
import {
  GithubMcpAssembler,
  warnLiteralEnvValues,
  buildCopilotMcpDict,
} from "../../../src/assembler/github-mcp-assembler.js";
import { TemplateEngine } from "../../../src/template-engine.js";
import {
  ProjectConfig,
  ProjectIdentity,
  ArchitectureConfig,
  InterfaceConfig,
  LanguageConfig,
  FrameworkConfig,
  McpConfig,
  McpServerConfig,
} from "../../../src/models.js";

function buildConfig(overrides: {
  mcpServers?: McpServerConfig[];
} = {}): ProjectConfig {
  return new ProjectConfig(
    new ProjectIdentity("my-app", "A test application"),
    new ArchitectureConfig("microservice", false, false),
    [new InterfaceConfig("rest")],
    new LanguageConfig("typescript", "5"),
    new FrameworkConfig("commander", "", "npm"),
    undefined,
    undefined,
    undefined,
    undefined,
    new McpConfig(overrides.mcpServers ?? []),
  );
}

function buildServer(overrides: {
  id?: string;
  url?: string;
  capabilities?: string[];
  env?: Record<string, string>;
} = {}): McpServerConfig {
  return new McpServerConfig(
    overrides.id ?? "server-1",
    overrides.url ?? "https://mcp.example.com",
    overrides.capabilities ?? [],
    overrides.env ?? {},
  );
}

describe("warnLiteralEnvValues", () => {
  it("allDollarPrefixed_noWarnings", () => {
    const servers = [buildServer({
      env: { API_KEY: "$API_KEY", SECRET: "$SECRET" },
    })];
    expect(warnLiteralEnvValues(servers)).toEqual([]);
  });

  it("literalValue_returnsWarning", () => {
    const servers = [buildServer({
      id: "test-srv",
      env: { API_KEY: "sk-hardcoded-value" },
    })];
    const warnings = warnLiteralEnvValues(servers);
    expect(warnings).toHaveLength(1);
    expect(warnings[0]).toContain("test-srv");
    expect(warnings[0]).toContain("API_KEY");
  });

  it("emptyEnv_noWarnings", () => {
    const servers = [buildServer({ env: {} })];
    expect(warnLiteralEnvValues(servers)).toEqual([]);
  });

  it("mixedValues_warnsOnlyLiterals", () => {
    const servers = [buildServer({
      env: { GOOD: "$VAR", BAD: "literal" },
    })];
    const warnings = warnLiteralEnvValues(servers);
    expect(warnings).toHaveLength(1);
    expect(warnings[0]).toContain("BAD");
  });
});

describe("buildCopilotMcpDict", () => {
  it("twoServers_correctStructure", () => {
    const config = buildConfig({
      mcpServers: [
        buildServer({ id: "srv-a", url: "https://a.com" }),
        buildServer({ id: "srv-b", url: "https://b.com" }),
      ],
    });
    const dict = buildCopilotMcpDict(config);
    const servers = dict["mcpServers"] as Record<string, unknown>;
    expect(Object.keys(servers)).toHaveLength(2);
    expect(servers["srv-a"]).toBeDefined();
    expect(servers["srv-b"]).toBeDefined();
  });

  it("serverWithoutCapabilities_omitsField", () => {
    const config = buildConfig({
      mcpServers: [buildServer({ capabilities: [] })],
    });
    const dict = buildCopilotMcpDict(config);
    const servers = dict["mcpServers"] as Record<string, Record<string, unknown>>;
    expect(servers["server-1"]).not.toHaveProperty("capabilities");
  });

  it("serverWithoutEnv_omitsField", () => {
    const config = buildConfig({
      mcpServers: [buildServer({ env: {} })],
    });
    const dict = buildCopilotMcpDict(config);
    const servers = dict["mcpServers"] as Record<string, Record<string, unknown>>;
    expect(servers["server-1"]).not.toHaveProperty("env");
  });

  it("serverWithCapabilities_includesField", () => {
    const config = buildConfig({
      mcpServers: [buildServer({
        capabilities: ["tools", "prompts"],
      })],
    });
    const dict = buildCopilotMcpDict(config);
    const servers = dict["mcpServers"] as Record<string, Record<string, unknown>>;
    expect(servers["server-1"]!["capabilities"]).toEqual(["tools", "prompts"]);
  });

  it("serverWithEnv_includesField", () => {
    const config = buildConfig({
      mcpServers: [buildServer({
        env: { API_KEY: "$API_KEY" },
      })],
    });
    const dict = buildCopilotMcpDict(config);
    const servers = dict["mcpServers"] as Record<string, Record<string, unknown>>;
    expect(servers["server-1"]!["env"]).toEqual({ API_KEY: "$API_KEY" });
  });
});

describe("GithubMcpAssembler", () => {
  let tmpDir: string;
  let resourcesDir: string;
  let outputDir: string;
  let assembler: GithubMcpAssembler;

  beforeEach(() => {
    tmpDir = fs.mkdtempSync(
      path.join(tmpdir(), "gh-mcp-asm-test-"),
    );
    resourcesDir = path.join(tmpDir, "resources");
    fs.mkdirSync(resourcesDir, { recursive: true });
    outputDir = path.join(tmpDir, "output");
    fs.mkdirSync(outputDir, { recursive: true });
    assembler = new GithubMcpAssembler();
  });

  afterEach(() => {
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  describe("assemble", () => {
    it("assemble_noServers_returnsEmptyResult", () => {
      const config = buildConfig({ mcpServers: [] });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result.files).toEqual([]);
      expect(result.warnings).toEqual([]);
    });

    it("assemble_twoServers_generatesJson", () => {
      const config = buildConfig({
        mcpServers: [
          buildServer({ id: "srv-a", url: "https://a.com" }),
          buildServer({ id: "srv-b", url: "https://b.com" }),
        ],
      });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result.files).toHaveLength(1);
      const dest = path.join(outputDir, "copilot-mcp.json");
      expect(result.files[0]).toBe(dest);
      expect(fs.existsSync(dest)).toBe(true);
      const parsed = JSON.parse(
        fs.readFileSync(dest, "utf-8"),
      ) as Record<string, unknown>;
      const servers = parsed["mcpServers"] as Record<string, unknown>;
      expect(Object.keys(servers)).toHaveLength(2);
    });

    it("assemble_jsonFormat_2spaceIndentWithTrailingNewline", () => {
      const config = buildConfig({
        mcpServers: [buildServer()],
      });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      const raw = fs.readFileSync(
        path.join(outputDir, "copilot-mcp.json"), "utf-8",
      );
      expect(raw).toContain("  ");
      expect(raw.endsWith("\n")).toBe(true);
      expect(raw).not.toContain("\t");
    });

    it("assemble_literalEnvValue_collectsWarning", () => {
      const config = buildConfig({
        mcpServers: [buildServer({
          id: "bad-srv",
          env: { API_KEY: "sk-literal" },
        })],
      });
      const engine = new TemplateEngine(resourcesDir, config);
      const result = assembler.assemble(
        config, outputDir, resourcesDir, engine,
      );
      expect(result.warnings.length).toBeGreaterThanOrEqual(1);
      expect(result.warnings[0]).toContain("bad-srv");
      expect(result.warnings[0]).toContain("API_KEY");
    });

    it("assemble_createsGithubDirectory", () => {
      const config = buildConfig({
        mcpServers: [buildServer()],
      });
      const engine = new TemplateEngine(resourcesDir, config);
      assembler.assemble(config, outputDir, resourcesDir, engine);
      expect(fs.existsSync(outputDir)).toBe(true);
      expect(fs.statSync(outputDir).isDirectory()).toBe(true);
    });
  });
});
