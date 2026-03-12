/**
 * CodexAgentsMdAssembler — generates .codex/AGENTS.md from Nunjucks template.
 *
 * Operates in 3 phases:
 * 1. Collect extended context (scan agents, skills, hooks from .claude/)
 * 2. Build rendering context (24 flat config fields + extended fields)
 * 3. Render template and write output to .codex/AGENTS.md
 *
 * @module
 */
import * as fs from "node:fs";
import * as path from "node:path";
import type { ProjectConfig } from "../models.js";
import type { TemplateEngine } from "../template-engine.js";
import { buildDefaultContext } from "../template-engine.js";
import { resolveStack } from "../domain/resolver.js";
import type { AssembleResult } from "./rules-assembler.js";

/** Metadata extracted from a generated agent .md file. */
export interface AgentInfo {
  readonly name: string;
  readonly description: string;
}

/** Metadata extracted from a generated skill's SKILL.md frontmatter. */
export interface SkillInfo {
  readonly name: string;
  readonly description: string;
  readonly user_invocable: boolean;
}

const TEMPLATE_PATH = "codex-templates/agents-md.md.njk";

/**
 * Scan a directory for agent .md files.
 *
 * @param agentsDir - Absolute path to the agents directory.
 * @returns Sorted array of AgentInfo with name and description.
 */
export function scanAgents(agentsDir: string): AgentInfo[] {
  if (!fs.existsSync(agentsDir)) return [];
  const files = fs.readdirSync(agentsDir)
    .filter((f) => f.endsWith(".md"))
    .sort();
  const agents: AgentInfo[] = [];
  for (const file of files) {
    const name = file.replace(/\.md$/, "");
    const content = fs.readFileSync(
      path.join(agentsDir, file), "utf-8",
    );
    const description = extractDescription(content);
    agents.push({ name, description });
  }
  return agents;
}

/** Extract description from the first meaningful line of content. */
function extractDescription(content: string): string {
  for (const line of content.split("\n")) {
    const trimmed = line.trim();
    if (trimmed === "") continue;
    if (trimmed.startsWith("# ")) {
      return trimmed.slice(2).trim();
    }
    return trimmed;
  }
  return "";
}

/**
 * Scan a directory for skill subdirs containing SKILL.md.
 *
 * @param skillsDir - Absolute path to the skills directory.
 * @returns Sorted array of SkillInfo with name, description, user_invocable.
 */
export function scanSkills(skillsDir: string): SkillInfo[] {
  if (!fs.existsSync(skillsDir)) return [];
  const entries = fs.readdirSync(skillsDir, { withFileTypes: true })
    .filter((d) => d.isDirectory())
    .sort((a, b) => a.name.localeCompare(b.name));
  const skills: SkillInfo[] = [];
  for (const entry of entries) {
    const skillMdPath = path.join(
      skillsDir, entry.name, "SKILL.md",
    );
    if (!fs.existsSync(skillMdPath)) continue;
    const content = fs.readFileSync(skillMdPath, "utf-8");
    const parsed = parseSkillFrontmatter(content, entry.name);
    skills.push({
      name: parsed.name,
      description: parsed.description,
      user_invocable: parsed.userInvocable,
    });
  }
  return skills;
}

/** Parse YAML frontmatter from SKILL.md content. */
function parseSkillFrontmatter(
  content: string,
  dirName: string,
): { name: string; description: string; userInvocable: boolean } {
  let name = dirName;
  let description = "";
  let userInvocable = true;
  let inFrontmatter = false;
  for (const line of content.split("\n")) {
    if (line.trim() === "---") {
      if (inFrontmatter) break;
      inFrontmatter = true;
      continue;
    }
    if (!inFrontmatter) continue;
    if (line.startsWith("name:")) {
      name = line.slice(5).trim().replace(/^["']|["']$/g, "");
    } else if (line.startsWith("description:")) {
      description = line.slice(12).trim()
        .replace(/^["']|["']$/g, "");
    } else if (line.startsWith("user-invocable:")) {
      const value = line.slice(15).trim().toLowerCase();
      userInvocable = value !== "false";
    }
  }
  return { name, description, userInvocable };
}

/**
 * Build extended context for AGENTS.md template rendering.
 *
 * Merges the 24 flat config fields with extended fields:
 * resolved_stack, agents_list, skills_list, has_hooks,
 * mcp_servers, security_frameworks, observability,
 * model, approval_policy, sandbox_mode.
 */
export function buildExtendedContext(
  config: ProjectConfig,
  agents: AgentInfo[],
  skills: SkillInfo[],
  hasHooks: boolean,
): Record<string, unknown> {
  const resolved = resolveStack(config);
  return {
    ...buildDefaultContext(config),
    observability: config.infrastructure.observability.tool,
    resolved_stack: {
      buildCmd: resolved.buildCmd,
      testCmd: resolved.testCmd,
      compileCmd: resolved.compileCmd,
      coverageCmd: resolved.coverageCmd,
    },
    agents_list: agents,
    skills_list: skills,
    has_hooks: hasHooks,
    mcp_servers: config.mcp.servers.map((s) => ({
      id: s.id,
      command: s.url ? s.url.split(/\s+/) : [],
      env: { ...s.env },
    })),
    security_frameworks: [...config.security.frameworks],
    model: "o4-mini",
    approval_policy: hasHooks ? "on-request" : "untrusted",
    sandbox_mode: "workspace-write",
  };
}

/** Generates .codex/AGENTS.md from Nunjucks template. */
export class CodexAgentsMdAssembler {
  /** Generate .codex/AGENTS.md by scanning .claude/ output and rendering template. */
  assemble(
    config: ProjectConfig,
    outputDir: string,
    resourcesDir: string,
    engine: TemplateEngine,
  ): AssembleResult {
    const warnings: string[] = [];
    const rootDir = path.dirname(outputDir);
    const claudeDir = path.join(rootDir, ".claude");

    // Phase 1 — Collect extended context
    const agentsDir = path.join(claudeDir, "agents");
    const skillsDir = path.join(claudeDir, "skills");
    const hooksDir = path.join(claudeDir, "hooks");
    const agents = scanAgents(agentsDir);
    const skills = scanSkills(skillsDir);
    const hasHooks = fs.existsSync(hooksDir);

    if (agents.length === 0) {
      warnings.push("No agents found in output directory");
    }
    if (skills.length === 0) {
      warnings.push("No skills found in output directory");
    }

    // Phase 2 — Build rendering context
    const context = buildExtendedContext(
      config, agents, skills, hasHooks,
    );

    // Phase 3 — Render and write
    let rendered: string;
    try {
      rendered = engine.renderTemplate(TEMPLATE_PATH, context);
    } catch {
      warnings.push(
        `Template not found: ${TEMPLATE_PATH}`,
      );
      return { files: [], warnings };
    }
    fs.mkdirSync(outputDir, { recursive: true });
    const dest = path.join(outputDir, "AGENTS.md");
    fs.writeFileSync(dest, rendered, "utf-8");
    return { files: [dest], warnings };
  }
}
