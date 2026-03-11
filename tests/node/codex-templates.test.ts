import { describe, it, expect } from "vitest";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { dirname } from "node:path";
import {
  TemplateEngine,
  buildDefaultContext,
} from "../../src/template-engine.js";
import {
  aProjectConfig,
  aMinimalProjectConfig,
  aFullProjectConfig,
} from "../fixtures/project-config.fixture.js";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const RESOURCES_DIR = resolve(__dirname, "../../resources");

const AGENTS_MD = "codex-templates/agents-md.md.njk";
const CONFIG_TOML = "codex-templates/config.toml.njk";

function section(name: string): string {
  return `codex-templates/sections/${name}.md.njk`;
}

function fullContext(): Record<string, unknown> {
  return {
    ...buildDefaultContext(aFullProjectConfig()),
    observability: "opentelemetry",
    resolved_stack: {
      buildCmd: "npm run build",
      testCmd: "npm test",
      compileCmd: "npx tsc --noEmit",
      coverageCmd: "npm run test:coverage",
    },
    agents_list: [
      { name: "architect", description: "System architect" },
      { name: "tech-lead", description: "Technical lead" },
      { name: "qa-engineer", description: "Quality assurance" },
    ],
    skills_list: [
      { name: "x-dev-implement", description: "Implement features", user_invocable: true },
      { name: "x-review", description: "Code review", user_invocable: true },
      { name: "x-lib-audit", description: "Internal audit", user_invocable: false },
    ],
    has_hooks: true,
    mcp_servers: [
      {
        id: "firecrawl",
        command: ["npx", "firecrawl-mcp"],
        env: { API_KEY: "test-key-xxx" },
      },
    ],
    security_frameworks: ["owasp", "pci-dss"],
    model: "o4-mini",
    approval_policy: "on-request",
    sandbox_mode: "workspace-write",
  };
}

function minimalContext(): Record<string, unknown> {
  return {
    ...buildDefaultContext(aMinimalProjectConfig()),
    observability: "none",
    resolved_stack: {
      buildCmd: "go build ./...",
      testCmd: "go test ./...",
      compileCmd: "go vet ./...",
      coverageCmd: "go test -coverprofile=coverage.out ./...",
    },
    agents_list: [],
    skills_list: [],
    has_hooks: false,
    mcp_servers: [],
    security_frameworks: [],
    model: "o4-mini",
    approval_policy: "untrusted",
    sandbox_mode: "workspace-write",
  };
}

// ---------------------------------------------------------------------------
// Section rendering tests
// ---------------------------------------------------------------------------

describe("Section rendering", () => {
  const engine = new TemplateEngine(RESOURCES_DIR, aFullProjectConfig());
  const ctx = fullContext();

  it("header_fullContext_rendersProjectNameAndPurpose", () => {
    const result = engine.renderTemplate(section("header"), ctx);
    expect(result).toContain("# my-service");
    expect(result).toContain("A sample service");
  });

  it("architecture_fullContext_rendersStyleAndLanguage", () => {
    const result = engine.renderTemplate(section("architecture"), ctx);
    expect(result).toContain("hexagonal");
    expect(result).toContain("python");
    expect(result).toContain("3.9");
    expect(result).toContain("click");
  });

  it("techStack_fullContext_rendersAllRows", () => {
    const result = engine.renderTemplate(section("tech-stack"), ctx);
    expect(result).toContain("postgresql");
    expect(result).toContain("redis");
    expect(result).toContain("docker");
    expect(result).toContain("kubernetes");
    expect(result).toContain("opentelemetry");
  });

  it.each([
    { field: "database_name", label: "Database" },
    { field: "cache_name", label: "Cache" },
    { field: "orchestrator", label: "Orchestrator" },
    { field: "observability", label: "Observability" },
  ])("techStack_${field}None_omits${label}Row", ({ field, label }) => {
    const result = engine.renderTemplate(section("tech-stack"), {
      ...ctx,
      [field]: "none",
    });
    expect(result).not.toMatch(new RegExp(`\\|\\s*${label}\\s*\\|`));
  });

  it("commands_fullContext_rendersAllCommands", () => {
    const result = engine.renderTemplate(section("commands"), ctx);
    expect(result).toContain("npm run build");
    expect(result).toContain("npm test");
    expect(result).toContain("npx tsc --noEmit");
    expect(result).toContain("npm run test:coverage");
  });

  it("codingStandards_fullContext_rendersHardLimits", () => {
    const result = engine.renderTemplate(section("coding-standards"), ctx);
    expect(result).toContain("25 lines");
    expect(result).toContain("250 lines");
    expect(result).toContain("SRP");
    expect(result).toContain("OCP");
    expect(result).toContain("python");
  });

  it("qualityGates_fullContext_rendersCoverageThresholds", () => {
    const result = engine.renderTemplate(section("quality-gates"), ctx);
    expect(result).toContain("95");
    expect(result).toContain("90");
    expect(result).toContain("[methodUnderTest]");
  });

  it("domain_fullContext_rendersDomainSection", () => {
    const result = engine.renderTemplate(section("domain"), ctx);
    expect(result).toContain("## Domain");
    expect(result).toContain("Domain-Driven Design");
  });

  it("security_fullContext_rendersFrameworks", () => {
    const result = engine.renderTemplate(section("security"), ctx);
    expect(result).toContain("owasp");
    expect(result).toContain("pci-dss");
  });

  it("conventions_fullContext_rendersCommitConventions", () => {
    const result = engine.renderTemplate(section("conventions"), ctx);
    expect(result).toContain("Conventional Commits");
    expect(result).toContain("English");
  });

  it("skills_fullContext_rendersUserInvocableOnly", () => {
    const result = engine.renderTemplate(section("skills"), ctx);
    expect(result).toContain("x-dev-implement");
    expect(result).toContain("x-review");
    expect(result).not.toContain("x-lib-audit");
  });

  it("agents_fullContext_rendersAgentsTable", () => {
    const result = engine.renderTemplate(section("agents"), ctx);
    expect(result).toContain("architect");
    expect(result).toContain("tech-lead");
    expect(result).toContain("qa-engineer");
  });
});

// ---------------------------------------------------------------------------
// Orchestrator — agents-md.md.njk (full context)
// ---------------------------------------------------------------------------

describe("Orchestrator — agents-md.md.njk (full context)", () => {
  const engine = new TemplateEngine(RESOURCES_DIR, aFullProjectConfig());

  it("agentsMd_fullContext_containsAllSections", () => {
    const result = engine.renderTemplate(AGENTS_MD, fullContext());
    expect(result).toContain("# my-service");
    expect(result).toContain("## Architecture");
    expect(result).toContain("## Tech Stack");
    expect(result).toContain("## Commands");
    expect(result).toContain("## Coding Standards");
    expect(result).toContain("## Quality Gates");
    expect(result).toContain("## Domain");
    expect(result).toContain("## Security");
    expect(result).toContain("## Conventions");
    expect(result).toContain("## Available Skills");
    expect(result).toContain("## Agent Personas");
  });

  it("agentsMd_fullContext_noTemplateArtifacts", () => {
    const result = engine.renderTemplate(AGENTS_MD, fullContext());
    expect(result).not.toMatch(/\{\{/);
    expect(result).not.toMatch(/\{%/);
    expect(result).not.toMatch(/\{#/);
  });

  it("agentsMd_fullContext_startsWithProjectTitle", () => {
    const result = engine.renderTemplate(AGENTS_MD, fullContext());
    expect(result.trimStart()).toMatch(/^# my-service/);
  });

  it("agentsMd_fullContext_noExcessiveBlankLines", () => {
    const result = engine.renderTemplate(AGENTS_MD, fullContext());
    expect(result).not.toMatch(/\n{5,}/);
  });
});

// ---------------------------------------------------------------------------
// Orchestrator — agents-md.md.njk (minimal context)
// ---------------------------------------------------------------------------

describe("Orchestrator — agents-md.md.njk (minimal context)", () => {
  const engine = new TemplateEngine(RESOURCES_DIR, aMinimalProjectConfig());

  it("agentsMd_minimalContext_omitsDomainSection", () => {
    const result = engine.renderTemplate(AGENTS_MD, minimalContext());
    expect(result).not.toContain("## Domain");
  });

  it("agentsMd_minimalContext_omitsSecuritySection", () => {
    const result = engine.renderTemplate(AGENTS_MD, minimalContext());
    expect(result).not.toContain("## Security");
  });

  it("agentsMd_minimalContext_omitsSkillsSection", () => {
    const result = engine.renderTemplate(AGENTS_MD, minimalContext());
    expect(result).not.toContain("## Available Skills");
  });

  it("agentsMd_minimalContext_omitsAgentsSection", () => {
    const result = engine.renderTemplate(AGENTS_MD, minimalContext());
    expect(result).not.toContain("## Agent Personas");
  });

  it("agentsMd_minimalContext_includesAlwaysPresentSections", () => {
    const result = engine.renderTemplate(AGENTS_MD, minimalContext());
    expect(result).toContain("## Architecture");
    expect(result).toContain("## Tech Stack");
    expect(result).toContain("## Commands");
    expect(result).toContain("## Coding Standards");
    expect(result).toContain("## Quality Gates");
    expect(result).toContain("## Conventions");
  });

  it("agentsMd_minimalContext_noTemplateArtifacts", () => {
    const result = engine.renderTemplate(AGENTS_MD, minimalContext());
    expect(result).not.toMatch(/\{\{/);
    expect(result).not.toMatch(/\{%/);
    expect(result).not.toMatch(/\{#/);
  });
});

// ---------------------------------------------------------------------------
// Config — config.toml.njk
// ---------------------------------------------------------------------------

describe("Config — config.toml.njk", () => {
  const engine = new TemplateEngine(RESOURCES_DIR, aFullProjectConfig());

  it("configToml_fullContext_rendersModelAndPolicy", () => {
    const result = engine.renderTemplate(CONFIG_TOML, fullContext());
    expect(result).toContain('model = "o4-mini"');
    expect(result).toContain('approval_policy = "on-request"');
  });

  it("configToml_fullContext_rendersMcpServers", () => {
    const result = engine.renderTemplate(CONFIG_TOML, fullContext());
    expect(result).toContain("[mcp_servers.firecrawl]");
    expect(result).toContain('"npx"');
    expect(result).toContain('"firecrawl-mcp"');
  });

  it("configToml_fullContext_rendersMcpEnvVars", () => {
    const result = engine.renderTemplate(CONFIG_TOML, fullContext());
    expect(result).toContain('API_KEY = "test-key-xxx"');
  });

  it("configToml_fullContext_rendersSandboxMode", () => {
    const result = engine.renderTemplate(CONFIG_TOML, fullContext());
    expect(result).toContain('mode = "workspace-write"');
  });

  it("configToml_noMcpServers_omitsMcpSection", () => {
    const minEngine = new TemplateEngine(RESOURCES_DIR, aMinimalProjectConfig());
    const result = minEngine.renderTemplate(CONFIG_TOML, minimalContext());
    expect(result).not.toContain("[mcp_servers");
  });

  it("configToml_noHooks_usesUntrustedPolicy", () => {
    const minEngine = new TemplateEngine(RESOURCES_DIR, aMinimalProjectConfig());
    const result = minEngine.renderTemplate(CONFIG_TOML, minimalContext());
    expect(result).toContain('approval_policy = "untrusted"');
  });

  it("configToml_fullContext_noTemplateArtifacts", () => {
    const result = engine.renderTemplate(CONFIG_TOML, fullContext());
    expect(result).not.toMatch(/\{\{/);
    expect(result).not.toMatch(/\{%/);
    expect(result).not.toMatch(/\{#/);
  });

  it("configToml_fullContext_rendersProjectComment", () => {
    const result = engine.renderTemplate(CONFIG_TOML, fullContext());
    expect(result).toContain("# Codex CLI configuration for my-service");
  });

  it("configToml_fullContext_validTomlStructure", () => {
    const result = engine.renderTemplate(CONFIG_TOML, fullContext());
    const lines = result.split("\n");
    for (const line of lines) {
      const trimmed = line.trim();
      if (trimmed === "" || trimmed.startsWith("#")) continue;
      const isKeyValue = /^[\w.]+\s*=\s*.+$/.test(trimmed);
      const isSectionHeader = /^\[[\w.]+\]$/.test(trimmed);
      expect(
        isKeyValue || isSectionHeader,
      ).toBe(true);
    }
  });
});

// ---------------------------------------------------------------------------
// Snapshots
// ---------------------------------------------------------------------------

describe("Snapshots", () => {
  const fullEngine = new TemplateEngine(RESOURCES_DIR, aFullProjectConfig());
  const minEngine = new TemplateEngine(RESOURCES_DIR, aMinimalProjectConfig());

  it("agentsMd_fullContext_matchesSnapshot", () => {
    const result = fullEngine.renderTemplate(AGENTS_MD, fullContext());
    expect(result).toMatchSnapshot();
  });

  it("agentsMd_minimalContext_matchesSnapshot", () => {
    const result = minEngine.renderTemplate(AGENTS_MD, minimalContext());
    expect(result).toMatchSnapshot();
  });

  it("configToml_fullContext_matchesSnapshot", () => {
    const result = fullEngine.renderTemplate(CONFIG_TOML, fullContext());
    expect(result).toMatchSnapshot();
  });

  it("configToml_minimalContext_matchesSnapshot", () => {
    const result = minEngine.renderTemplate(CONFIG_TOML, minimalContext());
    expect(result).toMatchSnapshot();
  });
});

// ---------------------------------------------------------------------------
// Edge cases — empty arrays
// ---------------------------------------------------------------------------

describe("Edge cases — empty arrays", () => {
  const engine = new TemplateEngine(RESOURCES_DIR, aFullProjectConfig());
  const ctx = fullContext();

  it("skills_emptyList_rendersNoDataRows", () => {
    const result = engine.renderTemplate(section("skills"), {
      ...ctx,
      skills_list: [],
    });
    expect(result).toContain("## Available Skills");
    expect(result).not.toMatch(/\|\s*x-/);
  });

  it("agents_emptyList_rendersNoDataRows", () => {
    const result = engine.renderTemplate(section("agents"), {
      ...ctx,
      agents_list: [],
    });
    expect(result).toContain("## Agent Personas");
    expect(result).not.toMatch(/\|\s*architect/);
  });

  it("security_emptyFrameworks_rendersNoItems", () => {
    const result = engine.renderTemplate(section("security"), {
      ...ctx,
      security_frameworks: [],
    });
    expect(result).toContain("## Security");
    expect(result).not.toContain("owasp");
  });

  it("configToml_emptyMcpServers_noMcpSection", () => {
    const result = engine.renderTemplate(CONFIG_TOML, {
      ...ctx,
      mcp_servers: [],
    });
    expect(result).not.toContain("[mcp_servers");
  });
});

// ---------------------------------------------------------------------------
// Edge cases — "none" value handling
// ---------------------------------------------------------------------------

describe("Edge cases — none values", () => {
  const engine = new TemplateEngine(RESOURCES_DIR, aFullProjectConfig());
  const ctx = fullContext();

  it("techStack_allNone_rendersOnlyAlwaysPresentRows", () => {
    const result = engine.renderTemplate(section("tech-stack"), {
      ...ctx,
      database_name: "none",
      cache_name: "none",
      orchestrator: "none",
      observability: "none",
    });
    expect(result).toContain("python");
    expect(result).toContain("click");
    expect(result).toContain("docker");
    expect(result).not.toMatch(/\|\s*Database\s*\|/);
    expect(result).not.toMatch(/\|\s*Cache\s*\|/);
    expect(result).not.toMatch(/\|\s*Orchestrator\s*\|/);
    expect(result).not.toMatch(/\|\s*Observability\s*\|/);
  });

  it("techStack_someNone_rendersNonNoneRowsOnly", () => {
    const result = engine.renderTemplate(section("tech-stack"), {
      ...ctx,
      database_name: "none",
      cache_name: "redis",
      orchestrator: "none",
      observability: "opentelemetry",
    });
    expect(result).toContain("redis");
    expect(result).toContain("opentelemetry");
    expect(result).not.toMatch(/\|\s*Database\s*\|/);
    expect(result).not.toMatch(/\|\s*Orchestrator\s*\|/);
  });

  it("techStack_containerAlwaysPresent_rendersContainerRow", () => {
    const result = engine.renderTemplate(section("tech-stack"), {
      ...ctx,
      orchestrator: "none",
    });
    expect(result).toContain("docker");
  });
});

// ---------------------------------------------------------------------------
// Edge cases — quality gates conditionals
// ---------------------------------------------------------------------------

describe("Edge cases — quality gates conditionals", () => {
  const engine = new TemplateEngine(RESOURCES_DIR, aFullProjectConfig());
  const ctx = fullContext();

  it.each([
    { field: "smoke_tests", value: "True", label: "Smoke", present: true },
    { field: "contract_tests", value: "True", label: "Contract", present: true },
    { field: "performance_tests", value: "True", label: "Performance", present: true },
    { field: "smoke_tests", value: "False", label: "Smoke", present: false },
    { field: "contract_tests", value: "False", label: "Contract", present: false },
    { field: "performance_tests", value: "False", label: "Performance", present: false },
  ])("qualityGates_$field$value_$label", ({ field, value, label, present }) => {
    const result = engine.renderTemplate(section("quality-gates"), {
      ...ctx,
      [field]: value,
    });
    if (present) {
      expect(result).toContain(label);
    } else {
      expect(result).not.toContain(label);
    }
  });

  it("qualityGates_customThresholds_rendersCustomValues", () => {
    const result = engine.renderTemplate(section("quality-gates"), {
      ...ctx,
      coverage_line: 80,
      coverage_branch: 70,
    });
    expect(result).toContain("80");
    expect(result).toContain("70");
  });
});

// ---------------------------------------------------------------------------
// Edge cases — multiple MCP servers
// ---------------------------------------------------------------------------

describe("Edge cases — multiple MCP servers", () => {
  const engine = new TemplateEngine(RESOURCES_DIR, aFullProjectConfig());

  it("configToml_multipleMcpServers_rendersAllSections", () => {
    const ctx = fullContext();
    ctx.mcp_servers = [
      { id: "firecrawl", command: ["npx", "firecrawl-mcp"], env: { API_KEY: "key1" } },
      { id: "github", command: ["npx", "github-mcp"], env: { TOKEN: "ghp_xxx" } },
    ];
    const result = engine.renderTemplate(CONFIG_TOML, ctx);
    expect(result).toContain("[mcp_servers.firecrawl]");
    expect(result).toContain("[mcp_servers.github]");
  });

  it("configToml_mcpServerMultipleEnvVars_rendersAllVars", () => {
    const ctx = fullContext();
    ctx.mcp_servers = [
      {
        id: "multi-env",
        command: ["npx", "multi-mcp"],
        env: { API_KEY: "key1", SECRET: "s3cret" },
      },
    ];
    const result = engine.renderTemplate(CONFIG_TOML, ctx);
    expect(result).toContain('API_KEY = "key1"');
    expect(result).toContain('SECRET = "s3cret"');
  });
});

// ---------------------------------------------------------------------------
// throwOnUndefined compliance
// ---------------------------------------------------------------------------

describe("throwOnUndefined compliance", () => {
  it("agentsMd_missingExtendedVariable_throwsError", () => {
    const engine = new TemplateEngine(RESOURCES_DIR, aFullProjectConfig());
    const ctx = fullContext();
    delete ctx.resolved_stack;
    expect(() => engine.renderTemplate(AGENTS_MD, ctx)).toThrow();
  });
});
