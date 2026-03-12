import { describe, it, expect, beforeEach, afterEach } from "vitest";
import * as fs from "node:fs";
import { mkdtemp, rm, mkdir, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join, resolve, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import {
  CodexAgentsMdAssembler,
  scanAgents,
  scanSkills,
  buildExtendedContext,
} from "../../../src/assembler/codex-agents-md-assembler.js";
import { buildAssemblers } from "../../../src/assembler/pipeline.js";
import { TemplateEngine } from "../../../src/template-engine.js";
import {
  aFullProjectConfig,
  aMinimalProjectConfig,
} from "../../fixtures/project-config.fixture.js";
import {
  McpConfig,
  McpServerConfig,
} from "../../../src/models.js";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const RESOURCES_DIR = resolve(__dirname, "../../../resources");

let tempDir: string;

beforeEach(async () => {
  tempDir = await mkdtemp(join(tmpdir(), "codex-assembler-test-"));
});

afterEach(async () => {
  await rm(tempDir, { recursive: true, force: true });
});

// --- Helpers ---

async function seedAgentsDir(
  rootDir: string,
  agents: Array<{ name: string; content: string }>,
): Promise<void> {
  const agentsDir = join(rootDir, ".claude", "agents");
  await mkdir(agentsDir, { recursive: true });
  for (const agent of agents) {
    await writeFile(
      join(agentsDir, `${agent.name}.md`), agent.content,
    );
  }
}

async function seedSkillsDir(
  rootDir: string,
  skills: Array<{ name: string; frontmatter: string }>,
): Promise<void> {
  const skillsDir = join(rootDir, ".claude", "skills");
  await mkdir(skillsDir, { recursive: true });
  for (const skill of skills) {
    const skillDir = join(skillsDir, skill.name);
    await mkdir(skillDir, { recursive: true });
    await writeFile(join(skillDir, "SKILL.md"), skill.frontmatter);
  }
}

async function seedHooksDir(rootDir: string): Promise<void> {
  const hooksDir = join(rootDir, ".claude", "hooks");
  await mkdir(hooksDir, { recursive: true });
  await writeFile(
    join(hooksDir, "post-compile-check.sh"), "#!/bin/bash\n",
  );
}

// Fixtures
const AGENT_PLAIN = "System architect persona for design decisions.\n\nDetailed instructions follow.";
const AGENT_TITLED = "# Technical Lead\n\nLeads code reviews and architectural decisions.";
const AGENT_QA = "Quality assurance engineer persona.";

const SKILL_INVOCABLE = `---
name: x-dev-implement
description: Implements a feature following project conventions
user-invocable: true
---
# Skill instructions`;

const SKILL_REVIEW = `---
name: x-review
description: Code review
user-invocable: true
---
# Review skill`;

const SKILL_KP = `---
name: coding-standards
description: Coding standards knowledge pack
user-invocable: false
---
# Knowledge pack content`;

// ---------------------------------------------------------------------------
// scanAgents
// ---------------------------------------------------------------------------

describe("scanAgents", () => {
  it("scanAgents_emptyDir_returnsEmptyArray", async () => {
    const agentsDir = join(tempDir, "agents");
    await mkdir(agentsDir, { recursive: true });
    expect(scanAgents(agentsDir)).toEqual([]);
  });

  it("scanAgents_missingDir_returnsEmptyArray", () => {
    const agentsDir = join(tempDir, "nonexistent");
    expect(scanAgents(agentsDir)).toEqual([]);
  });

  it("scanAgents_oneAgent_returnsSingleAgentInfo", async () => {
    const agentsDir = join(tempDir, "agents");
    await mkdir(agentsDir, { recursive: true });
    await writeFile(
      join(agentsDir, "architect.md"), AGENT_PLAIN,
    );
    const result = scanAgents(agentsDir);
    expect(result).toHaveLength(1);
    expect(result[0]!.name).toBe("architect");
    expect(result[0]!.description).toBe(
      "System architect persona for design decisions.",
    );
  });

  it("scanAgents_multipleAgents_returnsSortedArray", async () => {
    const agentsDir = join(tempDir, "agents");
    await mkdir(agentsDir, { recursive: true });
    await writeFile(join(agentsDir, "tech-lead.md"), AGENT_TITLED);
    await writeFile(join(agentsDir, "architect.md"), AGENT_PLAIN);
    await writeFile(join(agentsDir, "qa-engineer.md"), AGENT_QA);
    const result = scanAgents(agentsDir);
    expect(result).toHaveLength(3);
    expect(result[0]!.name).toBe("architect");
    expect(result[1]!.name).toBe("qa-engineer");
    expect(result[2]!.name).toBe("tech-lead");
  });

  it("scanAgents_plainFirstLine_extractsDescription", async () => {
    const agentsDir = join(tempDir, "agents");
    await mkdir(agentsDir, { recursive: true });
    await writeFile(join(agentsDir, "dev.md"), AGENT_PLAIN);
    const result = scanAgents(agentsDir);
    expect(result[0]!.description).toBe(
      "System architect persona for design decisions.",
    );
  });

  it("scanAgents_titleLine_extractsDescriptionFromTitle", async () => {
    const agentsDir = join(tempDir, "agents");
    await mkdir(agentsDir, { recursive: true });
    await writeFile(join(agentsDir, "lead.md"), AGENT_TITLED);
    const result = scanAgents(agentsDir);
    expect(result[0]!.description).toBe("Technical Lead");
  });

  it("scanAgents_nonMdFiles_ignoresNonMarkdownFiles", async () => {
    const agentsDir = join(tempDir, "agents");
    await mkdir(agentsDir, { recursive: true });
    await writeFile(join(agentsDir, "agent.md"), AGENT_PLAIN);
    await writeFile(join(agentsDir, "README.txt"), "readme");
    await writeFile(join(agentsDir, "config.json"), "{}");
    const result = scanAgents(agentsDir);
    expect(result).toHaveLength(1);
    expect(result[0]!.name).toBe("agent");
  });

  it("scanAgents_emptyContentFile_returnsEmptyDescription", async () => {
    const agentsDir = join(tempDir, "agents");
    await mkdir(agentsDir, { recursive: true });
    await writeFile(join(agentsDir, "empty.md"), "");
    const result = scanAgents(agentsDir);
    expect(result).toHaveLength(1);
    expect(result[0]!.description).toBe("");
  });

  it("scanAgents_leadingBlankLines_extractsFirstNonBlankLine", async () => {
    const agentsDir = join(tempDir, "agents");
    await mkdir(agentsDir, { recursive: true });
    await writeFile(
      join(agentsDir, "spaced.md"),
      "\n\n\nActual first line content.\n",
    );
    const result = scanAgents(agentsDir);
    expect(result[0]!.description).toBe(
      "Actual first line content.",
    );
  });
});

// ---------------------------------------------------------------------------
// scanSkills
// ---------------------------------------------------------------------------

describe("scanSkills", () => {
  it("scanSkills_emptyDir_returnsEmptyArray", async () => {
    const skillsDir = join(tempDir, "skills");
    await mkdir(skillsDir, { recursive: true });
    expect(scanSkills(skillsDir)).toEqual([]);
  });

  it("scanSkills_missingDir_returnsEmptyArray", () => {
    const skillsDir = join(tempDir, "nonexistent");
    expect(scanSkills(skillsDir)).toEqual([]);
  });

  it("scanSkills_oneSkill_returnsSingleSkillInfo", async () => {
    const skillsDir = join(tempDir, "skills");
    const skillDir = join(skillsDir, "x-dev-implement");
    await mkdir(skillDir, { recursive: true });
    await writeFile(join(skillDir, "SKILL.md"), SKILL_INVOCABLE);
    const result = scanSkills(skillsDir);
    expect(result).toHaveLength(1);
    expect(result[0]).toEqual({
      name: "x-dev-implement",
      description: "Implements a feature following project conventions",
      user_invocable: true,
    });
  });

  it("scanSkills_multipleSkills_returnsSortedArray", async () => {
    const skillsDir = join(tempDir, "skills");
    for (const [name, content] of [
      ["x-review", SKILL_REVIEW],
      ["x-dev-implement", SKILL_INVOCABLE],
      ["coding-standards", SKILL_KP],
    ] as const) {
      const dir = join(skillsDir, name);
      await mkdir(dir, { recursive: true });
      await writeFile(join(dir, "SKILL.md"), content);
    }
    const result = scanSkills(skillsDir);
    expect(result).toHaveLength(3);
    expect(result[0]!.name).toBe("coding-standards");
    expect(result[1]!.name).toBe("x-dev-implement");
    expect(result[2]!.name).toBe("x-review");
  });

  it("scanSkills_userInvocableTrue_setsTrue", async () => {
    const skillsDir = join(tempDir, "skills");
    const skillDir = join(skillsDir, "x-dev-implement");
    await mkdir(skillDir, { recursive: true });
    await writeFile(join(skillDir, "SKILL.md"), SKILL_INVOCABLE);
    const result = scanSkills(skillsDir);
    expect(result[0]!.user_invocable).toBe(true);
  });

  it("scanSkills_userInvocableFalse_setsFalse", async () => {
    const skillsDir = join(tempDir, "skills");
    const skillDir = join(skillsDir, "coding-standards");
    await mkdir(skillDir, { recursive: true });
    await writeFile(join(skillDir, "SKILL.md"), SKILL_KP);
    const result = scanSkills(skillsDir);
    expect(result[0]!.user_invocable).toBe(false);
  });

  it("scanSkills_dirWithoutSkillMd_ignoresDirectory", async () => {
    const skillsDir = join(tempDir, "skills");
    const withSkill = join(skillsDir, "x-dev-implement");
    const withoutSkill = join(skillsDir, "lib-internal");
    await mkdir(withSkill, { recursive: true });
    await mkdir(withoutSkill, { recursive: true });
    await writeFile(join(withSkill, "SKILL.md"), SKILL_INVOCABLE);
    await writeFile(join(withoutSkill, "README.md"), "# Internal");
    const result = scanSkills(skillsDir);
    expect(result).toHaveLength(1);
    expect(result[0]!.name).toBe("x-dev-implement");
  });

  it("scanSkills_noFrontmatter_usesDirectoryNameAsDefault", async () => {
    const skillsDir = join(tempDir, "skills");
    const skillDir = join(skillsDir, "my-skill");
    await mkdir(skillDir, { recursive: true });
    await writeFile(
      join(skillDir, "SKILL.md"), "# No frontmatter here\nJust content.",
    );
    const result = scanSkills(skillsDir);
    expect(result).toHaveLength(1);
    expect(result[0]!.name).toBe("my-skill");
    expect(result[0]!.description).toBe("");
    expect(result[0]!.user_invocable).toBe(true);
  });

  it("scanSkills_contentBeforeFrontmatter_ignoresPreContent", async () => {
    const skillsDir = join(tempDir, "skills");
    const skillDir = join(skillsDir, "pre-content");
    await mkdir(skillDir, { recursive: true });
    await writeFile(
      join(skillDir, "SKILL.md"),
      "Some preamble text\n---\nname: actual-name\ndescription: Actual description\n---\n",
    );
    const result = scanSkills(skillsDir);
    expect(result[0]!.name).toBe("actual-name");
    expect(result[0]!.description).toBe("Actual description");
  });
});

// ---------------------------------------------------------------------------
// buildExtendedContext
// ---------------------------------------------------------------------------

describe("buildExtendedContext", () => {
  const fullConfig = aFullProjectConfig();
  const agents: Array<{ name: string; description: string }> = [
    { name: "architect", description: "System architect" },
    { name: "tech-lead", description: "Technical lead" },
    { name: "qa-engineer", description: "Quality assurance" },
  ];
  const skills: Array<{
    name: string; description: string; user_invocable: boolean;
  }> = [
    { name: "x-dev-implement", description: "Implement features", user_invocable: true },
    { name: "coding-standards", description: "Standards pack", user_invocable: false },
  ];

  it("buildExtendedContext_fullConfig_returnsAllFields", () => {
    const ctx = buildExtendedContext(
      fullConfig, agents, skills, true,
    );
    expect(Object.keys(ctx).length).toBeGreaterThanOrEqual(30);
    expect(ctx).toHaveProperty("project_name");
    expect(ctx).toHaveProperty("resolved_stack");
    expect(ctx).toHaveProperty("agents_list");
    expect(ctx).toHaveProperty("skills_list");
    expect(ctx).toHaveProperty("has_hooks");
    expect(ctx).toHaveProperty("mcp_servers");
    expect(ctx).toHaveProperty("security_frameworks");
    expect(ctx).toHaveProperty("model");
    expect(ctx).toHaveProperty("approval_policy");
    expect(ctx).toHaveProperty("sandbox_mode");
  });

  it("buildExtendedContext_fullConfig_includesResolvedStack", () => {
    const ctx = buildExtendedContext(
      fullConfig, agents, skills, true,
    );
    const stack = ctx.resolved_stack as Record<string, string>;
    expect(stack).toHaveProperty("buildCmd");
    expect(stack).toHaveProperty("testCmd");
    expect(stack).toHaveProperty("compileCmd");
    expect(stack).toHaveProperty("coverageCmd");
  });

  it("buildExtendedContext_fullConfig_includesAgentsList", () => {
    const ctx = buildExtendedContext(
      fullConfig, agents, skills, true,
    );
    expect(ctx.agents_list).toHaveLength(3);
    expect(ctx.agents_list).toEqual(agents);
  });

  it("buildExtendedContext_fullConfig_includesSkillsList", () => {
    const ctx = buildExtendedContext(
      fullConfig, agents, skills, true,
    );
    expect(ctx.skills_list).toHaveLength(2);
    expect(ctx.skills_list).toEqual(skills);
  });

  it("buildExtendedContext_withHooks_setsHasHooksTrue", () => {
    const ctx = buildExtendedContext(
      fullConfig, agents, skills, true,
    );
    expect(ctx.has_hooks).toBe(true);
  });

  it("buildExtendedContext_withoutHooks_setsHasHooksFalse", () => {
    const ctx = buildExtendedContext(
      fullConfig, agents, skills, false,
    );
    expect(ctx.has_hooks).toBe(false);
    expect(ctx.approval_policy).toBe("untrusted");
  });

  it("buildExtendedContext_fullConfig_includesSecurityFrameworks", () => {
    const ctx = buildExtendedContext(
      fullConfig, agents, skills, true,
    );
    expect(ctx.security_frameworks).toEqual(["owasp", "pci-dss"]);
  });

  it("buildExtendedContext_configWithMcp_mapsServersCorrectly", () => {
    const configWithMcp = aFullProjectConfig();
    const mcpConfig = new McpConfig([
      new McpServerConfig(
        "firecrawl", "npx firecrawl-mcp", [], { API_KEY: "key1" },
      ),
    ]);
    // Override mcp via creating a new config-like object
    const config = Object.create(
      Object.getPrototypeOf(configWithMcp),
      Object.getOwnPropertyDescriptors(configWithMcp),
    ) as typeof configWithMcp;
    Object.defineProperty(config, "mcp", { value: mcpConfig });
    const ctx = buildExtendedContext(config, agents, skills, true);
    const servers = ctx.mcp_servers as Array<{
      id: string; command: string[]; env: Record<string, string>;
    }>;
    expect(servers).toHaveLength(1);
    expect(servers[0]!.id).toBe("firecrawl");
    expect(servers[0]!.command).toEqual(["npx", "firecrawl-mcp"]);
    expect(servers[0]!.env).toEqual({ API_KEY: "key1" });
  });

  it("buildExtendedContext_noMcpServers_returnsEmptyArray", () => {
    const ctx = buildExtendedContext(
      fullConfig, agents, skills, true,
    );
    expect(ctx.mcp_servers).toEqual([]);
  });
});

// ---------------------------------------------------------------------------
// assemble
// ---------------------------------------------------------------------------

describe("assemble", () => {
  async function setupFullContext(rootDir: string): Promise<void> {
    await seedAgentsDir(rootDir, [
      { name: "architect", content: AGENT_PLAIN },
      { name: "tech-lead", content: AGENT_TITLED },
      { name: "qa-engineer", content: AGENT_QA },
    ]);
    await seedSkillsDir(rootDir, [
      { name: "x-dev-implement", frontmatter: SKILL_INVOCABLE },
      { name: "x-review", frontmatter: SKILL_REVIEW },
    ]);
    await seedHooksDir(rootDir);
  }

  it("assemble_fullConfig_generatesAgentsMdFile", async () => {
    await setupFullContext(tempDir);
    const config = aFullProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new CodexAgentsMdAssembler();
    const codexDir = join(tempDir, ".codex");
    assembler.assemble(config, codexDir, RESOURCES_DIR, engine);
    expect(fs.existsSync(join(codexDir, "AGENTS.md"))).toBe(true);
  });

  it("assemble_fullConfig_returnsFileAndNoWarnings", async () => {
    await setupFullContext(tempDir);
    const config = aFullProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new CodexAgentsMdAssembler();
    const codexDir = join(tempDir, ".codex");
    const result = assembler.assemble(
      config, codexDir, RESOURCES_DIR, engine,
    );
    expect(result.files).toHaveLength(1);
    expect(result.files[0]).toContain("AGENTS.md");
    expect(result.warnings).toEqual([]);
  });

  it("assemble_fullConfig_containsAllSections", async () => {
    await setupFullContext(tempDir);
    const config = aFullProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new CodexAgentsMdAssembler();
    const codexDir = join(tempDir, ".codex");
    assembler.assemble(config, codexDir, RESOURCES_DIR, engine);
    const content = fs.readFileSync(
      join(codexDir, "AGENTS.md"), "utf-8",
    );
    expect(content).toContain("## Architecture");
    expect(content).toContain("## Tech Stack");
    expect(content).toContain("## Commands");
    expect(content).toContain("## Coding Standards");
    expect(content).toContain("## Quality Gates");
    expect(content).toContain("## Domain");
    expect(content).toContain("## Security");
    expect(content).toContain("## Conventions");
    expect(content).toContain("## Available Skills");
    expect(content).toContain("## Agent Personas");
  });

  it("assemble_minimalConfig_omitsDomainSection", async () => {
    await setupFullContext(tempDir);
    const config = aMinimalProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new CodexAgentsMdAssembler();
    const codexDir = join(tempDir, ".codex");
    assembler.assemble(config, codexDir, RESOURCES_DIR, engine);
    const content = fs.readFileSync(
      join(codexDir, "AGENTS.md"), "utf-8",
    );
    expect(content).not.toContain("## Domain");
  });

  it("assemble_minimalConfig_omitsSecuritySection", async () => {
    await setupFullContext(tempDir);
    const config = aMinimalProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new CodexAgentsMdAssembler();
    const codexDir = join(tempDir, ".codex");
    assembler.assemble(config, codexDir, RESOURCES_DIR, engine);
    const content = fs.readFileSync(
      join(codexDir, "AGENTS.md"), "utf-8",
    );
    expect(content).not.toContain("## Security");
  });

  it("assemble_noAgents_returnsWarning", async () => {
    await seedSkillsDir(tempDir, [
      { name: "x-dev-implement", frontmatter: SKILL_INVOCABLE },
    ]);
    const config = aFullProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new CodexAgentsMdAssembler();
    const codexDir = join(tempDir, ".codex");
    const result = assembler.assemble(
      config, codexDir, RESOURCES_DIR, engine,
    );
    expect(result.warnings).toContain(
      "No agents found in output directory",
    );
  });

  it("assemble_noSkills_returnsWarning", async () => {
    await seedAgentsDir(tempDir, [
      { name: "architect", content: AGENT_PLAIN },
    ]);
    const config = aFullProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new CodexAgentsMdAssembler();
    const codexDir = join(tempDir, ".codex");
    const result = assembler.assemble(
      config, codexDir, RESOURCES_DIR, engine,
    );
    expect(result.warnings).toContain(
      "No skills found in output directory",
    );
  });

  it("assemble_noAgents_omitsAgentsSection", async () => {
    await seedSkillsDir(tempDir, [
      { name: "x-dev-implement", frontmatter: SKILL_INVOCABLE },
    ]);
    const config = aFullProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new CodexAgentsMdAssembler();
    const codexDir = join(tempDir, ".codex");
    assembler.assemble(config, codexDir, RESOURCES_DIR, engine);
    const content = fs.readFileSync(
      join(codexDir, "AGENTS.md"), "utf-8",
    );
    expect(content).not.toContain("## Agent Personas");
  });

  it("assemble_noSkills_omitsSkillsSection", async () => {
    await seedAgentsDir(tempDir, [
      { name: "architect", content: AGENT_PLAIN },
    ]);
    const config = aFullProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new CodexAgentsMdAssembler();
    const codexDir = join(tempDir, ".codex");
    assembler.assemble(config, codexDir, RESOURCES_DIR, engine);
    const content = fs.readFileSync(
      join(codexDir, "AGENTS.md"), "utf-8",
    );
    expect(content).not.toContain("## Available Skills");
  });

  it("assemble_codexDirNotExists_createsDirectory", async () => {
    await setupFullContext(tempDir);
    const config = aFullProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new CodexAgentsMdAssembler();
    const codexDir = join(tempDir, ".codex");
    expect(fs.existsSync(codexDir)).toBe(false);
    assembler.assemble(config, codexDir, RESOURCES_DIR, engine);
    expect(fs.existsSync(codexDir)).toBe(true);
    expect(fs.existsSync(join(codexDir, "AGENTS.md"))).toBe(true);
  });

  it("assemble_fullConfig_noTemplateArtifacts", async () => {
    await setupFullContext(tempDir);
    const config = aFullProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new CodexAgentsMdAssembler();
    const codexDir = join(tempDir, ".codex");
    assembler.assemble(config, codexDir, RESOURCES_DIR, engine);
    const content = fs.readFileSync(
      join(codexDir, "AGENTS.md"), "utf-8",
    );
    expect(content).not.toMatch(/\{\{/);
    expect(content).not.toMatch(/\{%/);
    expect(content).not.toMatch(/\{#/);
  });

  it("assemble_templateRenderingFails_returnsWarningAndEmptyFiles", async () => {
    await seedAgentsDir(tempDir, [
      { name: "architect", content: AGENT_PLAIN },
    ]);
    const config = aFullProjectConfig();
    const fakeResourcesDir = join(tempDir, "empty-resources");
    fs.mkdirSync(fakeResourcesDir, { recursive: true });
    const engine = new TemplateEngine(fakeResourcesDir, config);
    const assembler = new CodexAgentsMdAssembler();
    const codexDir = join(tempDir, ".codex");
    const result = assembler.assemble(
      config, codexDir, fakeResourcesDir, engine,
    );
    expect(result.files).toEqual([]);
    expect(result.warnings).toContain(
      "Template not found: codex-templates/agents-md.md.njk",
    );
  });

  it("assemble_fullConfig_noExcessiveBlankLines", async () => {
    await setupFullContext(tempDir);
    const config = aFullProjectConfig();
    const engine = new TemplateEngine(RESOURCES_DIR, config);
    const assembler = new CodexAgentsMdAssembler();
    const codexDir = join(tempDir, ".codex");
    assembler.assemble(config, codexDir, RESOURCES_DIR, engine);
    const content = fs.readFileSync(
      join(codexDir, "AGENTS.md"), "utf-8",
    );
    expect(content).not.toMatch(/\n{5,}/);
  });
});

// ---------------------------------------------------------------------------
// Pipeline integration
// ---------------------------------------------------------------------------

describe("Pipeline integration", () => {
  it("pipeline_assemblerList_includesCodexAgentsMdAssembler", () => {
    const assemblers = buildAssemblers();
    const names = assemblers.map((a) => a.name);
    expect(names).toContain("CodexAgentsMdAssembler");
  });

  it("pipeline_assemblerOrder_codexAfterAgentsAndSkills", () => {
    const assemblers = buildAssemblers();
    const names = assemblers.map((a) => a.name);
    const codexIdx = names.indexOf("CodexAgentsMdAssembler");
    const agentsIdx = names.indexOf("AgentsAssembler");
    const skillsIdx = names.indexOf("SkillsAssembler");
    expect(codexIdx).toBeGreaterThan(agentsIdx);
    expect(codexIdx).toBeGreaterThan(skillsIdx);
  });
});
