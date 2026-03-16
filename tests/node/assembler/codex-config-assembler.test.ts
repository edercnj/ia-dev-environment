import { describe, it, expect, beforeEach, afterEach } from "vitest";
import * as fs from "node:fs";
import { mkdtemp, rm, mkdir, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join, resolve, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import { parse as parseToml } from "smol-toml";
import {
  CodexConfigAssembler,
  buildConfigContext,
} from "../../../src/assembler/codex-config-assembler.js";
import {
  detectHooks,
  deriveApprovalPolicy,
  mapMcpServers,
  escapeTomlValue,
  isValidTomlBareKey,
} from "../../../src/assembler/codex-shared.js";
import { buildAssemblers } from "../../../src/assembler/pipeline.js";
import { TemplateEngine } from "../../../src/template-engine.js";
import {
  aFullProjectConfig,
  aMinimalProjectConfig,
} from "../../fixtures/project-config.fixture.js";
import {
  McpConfig,
  McpServerConfig,
  ProjectConfig,
} from "../../../src/models.js";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const RESOURCES_DIR = resolve(__dirname, "../../../resources");

let tempDir: string;

beforeEach(async () => {
  tempDir = await mkdtemp(join(tmpdir(), "codex-config-test-"));
});

afterEach(async () => {
  await rm(tempDir, { recursive: true, force: true });
});

// --- Helpers ---

async function seedHooksDir(
  rootDir: string,
  files: string[] = ["post-compile-check.sh"],
): Promise<void> {
  const hooksDir = join(rootDir, ".claude", "hooks");
  await mkdir(hooksDir, { recursive: true });
  for (const file of files) {
    await writeFile(join(hooksDir, file), "#!/bin/bash\n");
  }
}

function configWithMcp(
  servers: McpServerConfig[],
): ProjectConfig {
  const base = aFullProjectConfig();
  const mcpConfig = new McpConfig(servers);
  const config = Object.create(
    Object.getPrototypeOf(base),
    Object.getOwnPropertyDescriptors(base),
  ) as typeof base;
  Object.defineProperty(config, "mcp", { value: mcpConfig });
  return config;
}

function assembleAndRead(
  config: ProjectConfig,
  rootDir: string,
): string {
  const engine = new TemplateEngine(RESOURCES_DIR, config);
  const assembler = new CodexConfigAssembler();
  const codexDir = join(rootDir, ".codex");
  assembler.assemble(config, codexDir, RESOURCES_DIR, engine);
  return fs.readFileSync(join(codexDir, "config.toml"), "utf-8");
}

// ---------------------------------------------------------------------------
// detectHooks
// ---------------------------------------------------------------------------

describe("detectHooks", () => {
  it("detectHooks_missingDir_returnsFalse", () => {
    const hooksDir = join(tempDir, "nonexistent");
    expect(detectHooks(hooksDir)).toBe(false);
  });

  it("detectHooks_emptyDir_returnsFalse", async () => {
    const hooksDir = join(tempDir, "hooks");
    await mkdir(hooksDir, { recursive: true });
    expect(detectHooks(hooksDir)).toBe(false);
  });

  it("detectHooks_dirWithFiles_returnsTrue", async () => {
    const hooksDir = join(tempDir, "hooks");
    await mkdir(hooksDir, { recursive: true });
    await writeFile(join(hooksDir, "pre-commit.sh"), "#!/bin/bash\n");
    expect(detectHooks(hooksDir)).toBe(true);
  });

  it("detectHooks_dirWithMultipleFiles_returnsTrue", async () => {
    const hooksDir = join(tempDir, "hooks");
    await mkdir(hooksDir, { recursive: true });
    await writeFile(join(hooksDir, "pre-commit.sh"), "#!/bin/bash\n");
    await writeFile(join(hooksDir, "post-compile.sh"), "#!/bin/bash\n");
    expect(detectHooks(hooksDir)).toBe(true);
  });

  it("detectHooks_pathIsFile_returnsFalse", async () => {
    const filePath = join(tempDir, "hooks-file");
    await writeFile(filePath, "not a directory");
    expect(detectHooks(filePath)).toBe(false);
  });
});

// ---------------------------------------------------------------------------
// deriveApprovalPolicy
// ---------------------------------------------------------------------------

describe("deriveApprovalPolicy", () => {
  it("deriveApprovalPolicy_withHooks_returnsOnRequest", () => {
    expect(deriveApprovalPolicy(true)).toBe("on-request");
  });

  it("deriveApprovalPolicy_withoutHooks_returnsUntrusted", () => {
    expect(deriveApprovalPolicy(false)).toBe("untrusted");
  });
});

// ---------------------------------------------------------------------------
// mapMcpServers
// ---------------------------------------------------------------------------

describe("mapMcpServers", () => {
  it("mapMcpServers_noServers_returnsEmptyArray", () => {
    const config = aFullProjectConfig();
    expect(mapMcpServers(config)).toEqual([]);
  });

  it("mapMcpServers_oneServer_returnsMappedServer", () => {
    const config = configWithMcp([
      new McpServerConfig("firecrawl", "npx -y @anthropic-ai/firecrawl-mcp"),
    ]);
    const result = mapMcpServers(config);
    expect(result).toHaveLength(1);
    expect(result[0]!.id).toBe("firecrawl");
    expect(result[0]!.command).toEqual([
      "npx", "-y", "@anthropic-ai/firecrawl-mcp",
    ]);
    expect(result[0]!.env).toBeNull();
  });

  it("mapMcpServers_serverWithEnv_includesEnvVars", () => {
    const config = configWithMcp([
      new McpServerConfig(
        "docs", "docs-server", [], { API_KEY: "test-key" },
      ),
    ]);
    const result = mapMcpServers(config);
    expect(result[0]!.env).toEqual({ API_KEY: "test-key" });
  });

  it("mapMcpServers_multipleServers_returnsAllMapped", () => {
    const config = configWithMcp([
      new McpServerConfig("firecrawl", "npx firecrawl-mcp"),
      new McpServerConfig(
        "docs", "docs-server", [], { API_KEY: "value" },
      ),
    ]);
    const result = mapMcpServers(config);
    expect(result).toHaveLength(2);
    expect(result[0]!.id).toBe("firecrawl");
    expect(result[1]!.id).toBe("docs");
  });

  it("mapMcpServers_emptyUrl_returnsEmptyCommandArray", () => {
    const config = configWithMcp([
      new McpServerConfig("empty", ""),
    ]);
    const result = mapMcpServers(config);
    expect(result[0]!.command).toEqual([]);
  });

  it("mapMcpServers_multipleEnvVars_includesAll", () => {
    const config = configWithMcp([
      new McpServerConfig(
        "multi-env", "cmd", [],
        { API_KEY: "key1", SECRET: "secret1", TOKEN: "tok1" },
      ),
    ]);
    const result = mapMcpServers(config);
    expect(result[0]!.env).toEqual({
      API_KEY: "key1", SECRET: "secret1", TOKEN: "tok1",
    });
  });
});

// ---------------------------------------------------------------------------
// buildConfigContext
// ---------------------------------------------------------------------------

describe("buildConfigContext", () => {
  it("buildConfigContext_withoutHooks_setsUntrustedPolicy", () => {
    const config = aFullProjectConfig();
    const ctx = buildConfigContext(config, false);
    expect(ctx.model).toBe("o4-mini");
    expect(ctx.approval_policy).toBe("untrusted");
    expect(ctx.sandbox_mode).toBe("workspace-write");
  });

  it("buildConfigContext_withHooks_setsOnRequestPolicy", () => {
    const config = aFullProjectConfig();
    const ctx = buildConfigContext(config, true);
    expect(ctx.approval_policy).toBe("on-request");
  });

  it("buildConfigContext_noMcp_setsHasMcpFalse", () => {
    const config = aFullProjectConfig();
    const ctx = buildConfigContext(config, false);
    expect(ctx.has_mcp).toBe(false);
    expect(ctx.mcp_servers).toEqual([]);
  });

  it("buildConfigContext_withMcp_setsHasMcpTrue", () => {
    const config = configWithMcp([
      new McpServerConfig("firecrawl", "npx firecrawl-mcp"),
    ]);
    const ctx = buildConfigContext(config, false);
    expect(ctx.has_mcp).toBe(true);
    expect(
      (ctx.mcp_servers as Array<{ id: string }>),
    ).toHaveLength(1);
  });

  it("buildConfigContext_fullConfig_includesProjectName", () => {
    const config = aFullProjectConfig();
    const ctx = buildConfigContext(config, false);
    expect(ctx.project_name).toBe("my-service");
  });

  it("buildConfigContext_fullConfig_includesDefaultContextFields", () => {
    const config = aFullProjectConfig();
    const ctx = buildConfigContext(config, false);
    expect(ctx).toHaveProperty("language_name");
    expect(ctx).toHaveProperty("framework_name");
    expect(ctx).toHaveProperty("architecture_style");
  });
});

// ---------------------------------------------------------------------------
// assemble
// ---------------------------------------------------------------------------

describe("assemble", () => {
  it("assemble_simpleConfig_generatesConfigToml", async () => {
    const config = aFullProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new CodexConfigAssembler();
    const codexDir = join(tempDir, ".codex");
    assembler.assemble(config, codexDir, RESOURCES_DIR, engine);
    expect(fs.existsSync(join(codexDir, "config.toml"))).toBe(true);
  });

  it("assemble_simpleConfig_returnsFileAndNoWarnings", async () => {
    const config = aFullProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new CodexConfigAssembler();
    const codexDir = join(tempDir, ".codex");
    const result = assembler.assemble(
      config, codexDir, RESOURCES_DIR, engine,
    );
    expect(result.files).toHaveLength(1);
    expect(result.files[0]).toContain("config.toml");
    expect(result.warnings).toEqual([]);
  });

  it("assemble_simpleConfig_containsModelField", async () => {
    const content = assembleAndRead(aFullProjectConfig(), tempDir);
    expect(content).toContain('model = "o4-mini"');
  });

  it("assemble_noHooks_setsUntrustedPolicy", async () => {
    const content = assembleAndRead(aFullProjectConfig(), tempDir);
    expect(content).toContain('approval_policy = "untrusted"');
  });

  it("assemble_withHooks_setsOnRequestPolicy", async () => {
    await seedHooksDir(tempDir);
    const content = assembleAndRead(aFullProjectConfig(), tempDir);
    expect(content).toContain('approval_policy = "on-request"');
  });

  it("assemble_simpleConfig_containsSandboxMode", async () => {
    const content = assembleAndRead(aFullProjectConfig(), tempDir);
    expect(content).toContain('mode = "workspace-write"');
  });

  it("assemble_noMcpServers_omitsMcpSection", async () => {
    const content = assembleAndRead(aFullProjectConfig(), tempDir);
    expect(content).not.toContain("[mcp_servers");
  });

  it("assemble_withMcpServers_includesMcpSection", async () => {
    const config = configWithMcp([
      new McpServerConfig(
        "firecrawl", "npx -y @anthropic-ai/firecrawl-mcp",
      ),
    ]);
    const content = assembleAndRead(config, tempDir);
    expect(content).toContain("[mcp_servers.firecrawl]");
  });

  it("assemble_withMcpServers_includesCommandArray", async () => {
    const config = configWithMcp([
      new McpServerConfig(
        "firecrawl", "npx -y @anthropic-ai/firecrawl-mcp",
      ),
    ]);
    const content = assembleAndRead(config, tempDir);
    expect(content).toContain(
      'command = ["npx", "-y", "@anthropic-ai/firecrawl-mcp"]',
    );
  });

  it("assemble_mcpWithEnv_includesEnvSection", async () => {
    const config = configWithMcp([
      new McpServerConfig(
        "docs", "docs-server", [], { API_KEY: "test-value" },
      ),
    ]);
    const content = assembleAndRead(config, tempDir);
    expect(content).toContain("[mcp_servers.docs.env]");
    expect(content).toContain('API_KEY = "test-value"');
  });

  it("assemble_multipleMcpServers_includesAllSections", async () => {
    const config = configWithMcp([
      new McpServerConfig(
        "firecrawl", "npx -y @anthropic-ai/firecrawl-mcp",
      ),
      new McpServerConfig(
        "docs", "docs-server", [], { API_KEY: "value" },
      ),
    ]);
    const content = assembleAndRead(config, tempDir);
    expect(content).toContain("[mcp_servers.firecrawl]");
    expect(content).toContain("[mcp_servers.docs]");
  });

  it("assemble_codexDirNotExists_createsDirectory", async () => {
    const config = aFullProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new CodexConfigAssembler();
    const codexDir = join(tempDir, ".codex");
    expect(fs.existsSync(codexDir)).toBe(false);
    assembler.assemble(config, codexDir, RESOURCES_DIR, engine);
    expect(fs.existsSync(codexDir)).toBe(true);
    expect(
      fs.existsSync(join(codexDir, "config.toml")),
    ).toBe(true);
  });

  it("assemble_codexDirAlreadyExists_writesWithoutError", async () => {
    const config = aFullProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new CodexConfigAssembler();
    const codexDir = join(tempDir, ".codex");
    await mkdir(codexDir, { recursive: true });
    const result = assembler.assemble(
      config, codexDir, RESOURCES_DIR, engine,
    );
    expect(result.files).toHaveLength(1);
    expect(result.warnings).toEqual([]);
  });

  it("assemble_fullConfig_containsProjectComment", async () => {
    const content = assembleAndRead(aFullProjectConfig(), tempDir);
    expect(content).toContain("my-service");
  });

  it("assemble_fullConfig_noTemplateArtifacts", async () => {
    const content = assembleAndRead(aFullProjectConfig(), tempDir);
    expect(content).not.toMatch(/\{\{/);
    expect(content).not.toMatch(/\{%/);
    expect(content).not.toMatch(/\{#/);
  });

  it("assemble_templateNotFound_returnsWarningAndEmptyFiles", () => {
    const config = aFullProjectConfig();
    const fakeResourcesDir = join(tempDir, "empty-resources");
    fs.mkdirSync(fakeResourcesDir, { recursive: true });
    const engine = new TemplateEngine(fakeResourcesDir, config);
    const assembler = new CodexConfigAssembler();
    const codexDir = join(tempDir, ".codex");
    const result = assembler.assemble(
      config, codexDir, fakeResourcesDir, engine,
    );
    expect(result.files).toEqual([]);
    expect(
      result.warnings.some((w) => w.startsWith("Template not found:")),
    ).toBe(true);
  });

  it("assemble_minimalConfig_generatesValidOutput", async () => {
    const content = assembleAndRead(aMinimalProjectConfig(), tempDir);
    expect(content).toContain('model = "o4-mini"');
    expect(content).toContain('approval_policy = "untrusted"');
    expect(content).toContain('mode = "workspace-write"');
  });

  it("assemble_existingClaudeAndGithub_doesNotModifyThem", async () => {
    const claudeDir = join(tempDir, ".claude");
    const githubDir = join(tempDir, ".github");
    await mkdir(claudeDir, { recursive: true });
    await mkdir(githubDir, { recursive: true });
    const claudeFile = join(claudeDir, "settings.json");
    const githubFile = join(githubDir, "copilot-instructions.md");
    await writeFile(claudeFile, '{"original": true}');
    await writeFile(githubFile, "# Original instructions");
    const config = aFullProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new CodexConfigAssembler();
    const codexDir = join(tempDir, ".codex");
    assembler.assemble(config, codexDir, RESOURCES_DIR, engine);
    expect(
      fs.readFileSync(claudeFile, "utf-8"),
    ).toBe('{"original": true}');
    expect(
      fs.readFileSync(githubFile, "utf-8"),
    ).toBe("# Original instructions");
  });

  it("assemble_emptyHooksDir_setsUntrustedPolicy", async () => {
    const hooksDir = join(tempDir, ".claude", "hooks");
    await mkdir(hooksDir, { recursive: true });
    const content = assembleAndRead(aFullProjectConfig(), tempDir);
    expect(content).toContain('approval_policy = "untrusted"');
  });

  it("assemble_mcpServerNoEnv_omitsEnvSection", async () => {
    const config = configWithMcp([
      new McpServerConfig("simple", "simple-server"),
    ]);
    const content = assembleAndRead(config, tempDir);
    expect(content).toContain("[mcp_servers.simple]");
    expect(content).not.toContain("[mcp_servers.simple.env]");
  });

  it("assemble_unexpectedRenderError_rethrowsError", () => {
    const config = aFullProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const original = engine.renderTemplate.bind(engine);
    engine.renderTemplate = () => {
      throw new Error("Unexpected render failure");
    };
    const assembler = new CodexConfigAssembler();
    const codexDir = join(tempDir, ".codex");
    expect(() =>
      assembler.assemble(config, codexDir, RESOURCES_DIR, engine),
    ).toThrow("Unexpected render failure");
    engine.renderTemplate = original;
  });

  it("assemble_nonErrorThrown_convertsToString", () => {
    const config = aFullProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    engine.renderTemplate = () => {
      throw "template not found at path";
    };
    const assembler = new CodexConfigAssembler();
    const codexDir = join(tempDir, ".codex");
    const result = assembler.assemble(
      config, codexDir, RESOURCES_DIR, engine,
    );
    expect(result.files).toEqual([]);
    expect(
      result.warnings.some((w) => w.startsWith("Template not found:")),
    ).toBe(true);
  });

  it("assemble_enoentError_returnsWarning", () => {
    const config = aFullProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    engine.renderTemplate = () => {
      throw new Error("ENOENT: no such file or directory");
    };
    const assembler = new CodexConfigAssembler();
    const codexDir = join(tempDir, ".codex");
    const result = assembler.assemble(
      config, codexDir, RESOURCES_DIR, engine,
    );
    expect(result.files).toEqual([]);
    expect(
      result.warnings.some((w) => w.startsWith("Template not found:")),
    ).toBe(true);
  });
});

// ---------------------------------------------------------------------------
// TOML validity
// ---------------------------------------------------------------------------

describe("TOML validity", () => {
  it("assemble_simpleConfig_producesValidToml", () => {
    const content = assembleAndRead(aFullProjectConfig(), tempDir);
    const parsed = parseToml(content);
    expect(parsed.model).toBe("o4-mini");
    expect(parsed.approval_policy).toBe("untrusted");
  });

  it("assemble_withMcpServers_producesValidToml", () => {
    const config = configWithMcp([
      new McpServerConfig(
        "firecrawl", "npx -y @anthropic-ai/firecrawl-mcp",
        [], { API_KEY: "test-key" },
      ),
    ]);
    const content = assembleAndRead(config, tempDir);
    const parsed = parseToml(content);
    expect(parsed.model).toBe("o4-mini");
    const mcpServers = parsed.mcp_servers as Record<
      string, Record<string, unknown>
    >;
    expect(mcpServers.firecrawl).toBeDefined();
    expect(mcpServers.firecrawl.command).toEqual([
      "npx", "-y", "@anthropic-ai/firecrawl-mcp",
    ]);
  });

  it("assemble_minimalConfig_producesValidToml", () => {
    const content = assembleAndRead(aMinimalProjectConfig(), tempDir);
    const parsed = parseToml(content);
    expect(parsed.model).toBe("o4-mini");
    expect(parsed.approval_policy).toBe("untrusted");
    const sandbox = parsed.sandbox as Record<string, string>;
    expect(sandbox.mode).toBe("workspace-write");
  });

  it("assemble_withHooks_producesValidToml", async () => {
    await seedHooksDir(tempDir);
    const content = assembleAndRead(aFullProjectConfig(), tempDir);
    const parsed = parseToml(content);
    expect(parsed.approval_policy).toBe("on-request");
  });
});

// ---------------------------------------------------------------------------
// escapeTomlValue
// ---------------------------------------------------------------------------

describe("escapeTomlValue", () => {
  it("escapeTomlValue_plainString_returnsUnchanged", () => {
    expect(escapeTomlValue("hello")).toBe("hello");
  });

  it("escapeTomlValue_doubleQuotes_escapesQuotes", () => {
    expect(escapeTomlValue('value "with" quotes')).toBe(
      'value \\"with\\" quotes',
    );
  });

  it("escapeTomlValue_backslashes_escapesBackslashes", () => {
    expect(escapeTomlValue("path\\to\\file")).toBe(
      "path\\\\to\\\\file",
    );
  });

  it("escapeTomlValue_newlines_escapesNewlines", () => {
    expect(escapeTomlValue("line1\nline2")).toBe("line1\\nline2");
  });

  it("escapeTomlValue_tabs_escapesTabs", () => {
    expect(escapeTomlValue("col1\tcol2")).toBe("col1\\tcol2");
  });

  it("escapeTomlValue_carriageReturn_escapesCR", () => {
    expect(escapeTomlValue("line1\rline2")).toBe("line1\\rline2");
  });
});

// ---------------------------------------------------------------------------
// isValidTomlBareKey
// ---------------------------------------------------------------------------

describe("isValidTomlBareKey", () => {
  it("isValidTomlBareKey_alphanumeric_returnsTrue", () => {
    expect(isValidTomlBareKey("firecrawl")).toBe(true);
  });

  it("isValidTomlBareKey_withHyphens_returnsTrue", () => {
    expect(isValidTomlBareKey("firecrawl-mcp")).toBe(true);
  });

  it("isValidTomlBareKey_withUnderscores_returnsTrue", () => {
    expect(isValidTomlBareKey("my_server")).toBe(true);
  });

  it("isValidTomlBareKey_withDots_returnsFalse", () => {
    expect(isValidTomlBareKey("my.server")).toBe(false);
  });

  it("isValidTomlBareKey_withSpaces_returnsFalse", () => {
    expect(isValidTomlBareKey("my server")).toBe(false);
  });

  it("isValidTomlBareKey_withBrackets_returnsFalse", () => {
    expect(isValidTomlBareKey("server[1]")).toBe(false);
  });

  it("isValidTomlBareKey_emptyString_returnsFalse", () => {
    expect(isValidTomlBareKey("")).toBe(false);
  });
});

// ---------------------------------------------------------------------------
// mapMcpServers — edge cases
// ---------------------------------------------------------------------------

describe("mapMcpServers edge cases", () => {
  it("mapMcpServers_whitespaceOnlyUrl_returnsEmptyCommandArray", () => {
    const config = configWithMcp([
      new McpServerConfig("ws", "   "),
    ]);
    const result = mapMcpServers(config);
    expect(result[0]!.command).toEqual([]);
  });

  it("mapMcpServers_urlWithLeadingTrailingSpaces_trimsParts", () => {
    const config = configWithMcp([
      new McpServerConfig("spaced", "  npx firecrawl-mcp  "),
    ]);
    const result = mapMcpServers(config);
    expect(result[0]!.command).toEqual(["npx", "firecrawl-mcp"]);
  });

  it("mapMcpServers_envWithQuotes_escapesValues", () => {
    const config = configWithMcp([
      new McpServerConfig(
        "quoted", "cmd", [],
        { TOKEN: 'value "with" quotes' },
      ),
    ]);
    const result = mapMcpServers(config);
    expect(result[0]!.env).toEqual({
      TOKEN: 'value \\"with\\" quotes',
    });
  });
});

// ---------------------------------------------------------------------------
// assemble — validation warnings
// ---------------------------------------------------------------------------

describe("assemble validation", () => {
  it("assemble_invalidMcpServerId_emitsWarning", () => {
    const config = configWithMcp([
      new McpServerConfig("my.server", "cmd"),
    ]);
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new CodexConfigAssembler();
    const codexDir = join(tempDir, ".codex");
    const result = assembler.assemble(
      config, codexDir, RESOURCES_DIR, engine,
    );
    expect(result.warnings).toContain(
      'MCP server id "my.server" contains invalid TOML characters',
    );
    expect(result.files).toHaveLength(1);
  });

  it("assemble_validMcpServerId_noWarning", () => {
    const config = configWithMcp([
      new McpServerConfig("firecrawl-mcp", "npx firecrawl"),
    ]);
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new CodexConfigAssembler();
    const codexDir = join(tempDir, ".codex");
    const result = assembler.assemble(
      config, codexDir, RESOURCES_DIR, engine,
    );
    expect(result.warnings).toEqual([]);
  });
});

// ---------------------------------------------------------------------------
// Pipeline integration
// ---------------------------------------------------------------------------

describe("Pipeline integration", () => {
  it("pipeline_assemblerList_includesCodexConfigAssembler", () => {
    const assemblers = buildAssemblers();
    const names = assemblers.map((a) => a.name);
    expect(names).toContain("CodexConfigAssembler");
  });

  it("pipeline_assemblerOrder_configAfterAgentsMd", () => {
    const assemblers = buildAssemblers();
    const names = assemblers.map((a) => a.name);
    const configIdx = names.indexOf("CodexConfigAssembler");
    const agentsMdIdx = names.indexOf("CodexAgentsMdAssembler");
    expect(configIdx).toBeGreaterThan(agentsMdIdx);
  });

  it("pipeline_codexConfigAssembler_hasCodexTarget", () => {
    const assemblers = buildAssemblers();
    const descriptor = assemblers.find(
      (a) => a.name === "CodexConfigAssembler",
    );
    expect(descriptor).toBeDefined();
    expect(descriptor!.target).toBe("codex");
  });

  it("pipeline_assemblerCount_isTwentyOne", () => {
    const assemblers = buildAssemblers();
    expect(assemblers).toHaveLength(21);
  });
});
