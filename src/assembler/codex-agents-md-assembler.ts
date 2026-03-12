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
import yaml from "js-yaml";
import type { ProjectConfig } from "../models.js";
import type { TemplateEngine } from "../template-engine.js";
import { buildDefaultContext } from "../template-engine.js";
import { resolveStack } from "../domain/resolver.js";
import type { AssembleResult } from "./rules-assembler.js";
import {
  DEFAULT_MODEL,
  SANDBOX_WORKSPACE_WRITE,
  isAccessibleDirectory,
  detectHooks,
  deriveApprovalPolicy,
  mapMcpServers,
} from "./codex-shared.js";

/** Metadata extracted from a generated agent .md file. */
export interface AgentInfo {
  readonly name: string;
  readonly description: string;
}

/** Metadata extracted from a generated skill's SKILL.md frontmatter. */
export interface SkillInfo {
  readonly name: string;
  readonly description: string;
  readonly userInvocable: boolean;
}

const TEMPLATE_PATH = "codex-templates/agents-md.md.njk";

/**
 * Scan a directory for agent .md files.
 *
 * @param agentsDir - Absolute path to the agents directory.
 * @returns Sorted array of AgentInfo with name and description.
 */
export function scanAgents(agentsDir: string): AgentInfo[] {
  if (!isAccessibleDirectory(agentsDir)) return [];
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
 * @returns Sorted array of SkillInfo with name, description, userInvocable.
 */
export function scanSkills(skillsDir: string): SkillInfo[] {
  if (!isAccessibleDirectory(skillsDir)) return [];
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
    skills.push(parseSkillFrontmatter(content, entry.name));
  }
  return skills;
}

/** Extract raw YAML frontmatter block between --- delimiters. */
function extractFrontmatterBlock(content: string): string | null {
  const lines = content.split("\n");
  if (lines.length === 0 || lines[0]!.trim() !== "---") return null;
  for (let i = 1; i < lines.length; i++) {
    if (lines[i]!.trim() === "---") {
      return lines.slice(1, i).join("\n");
    }
  }
  return null;
}

/** Parse YAML frontmatter from SKILL.md content using js-yaml. */
function parseSkillFrontmatter(
  content: string,
  dirName: string,
): SkillInfo {
  const block = extractFrontmatterBlock(content);
  if (!block) {
    return { name: dirName, description: "", userInvocable: true };
  }
  const parsed = yaml.load(block) as Record<string, unknown> | null;
  if (!parsed || typeof parsed !== "object") {
    return { name: dirName, description: "", userInvocable: true };
  }
  const name = typeof parsed["name"] === "string"
    ? parsed["name"] : dirName;
  const rawDesc = parsed["description"];
  const description = typeof rawDesc === "string"
    ? rawDesc.trim() : "";
  const userInvocable = parsed["user-invocable"] !== false;
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
    skills_list: skills.map(toTemplateSkill),
    has_hooks: hasHooks,
    mcp_servers: mapMcpServers(config),
    security_frameworks: [...config.security.frameworks],
    model: DEFAULT_MODEL,
    approval_policy: deriveApprovalPolicy(hasHooks),
    sandbox_mode: SANDBOX_WORKSPACE_WRITE,
  };
}

/** Map SkillInfo (camelCase) to template context (snake_case). */
function toTemplateSkill(
  s: SkillInfo,
): { name: string; description: string; user_invocable: boolean } {
  return {
    name: s.name,
    description: s.description,
    user_invocable: s.userInvocable,
  };
}

/** Collect agents, skills, and hooks from .claude/ output directory. */
function collectContext(
  claudeDir: string,
  warnings: string[],
): { agents: AgentInfo[]; skills: SkillInfo[]; hasHooks: boolean } {
  const agents = scanAgents(path.join(claudeDir, "agents"));
  const skills = scanSkills(path.join(claudeDir, "skills"));
  const hooksDir = path.join(claudeDir, "hooks");
  const hasHooks = detectHooks(hooksDir);
  if (agents.length === 0) {
    warnings.push("No agents found in output directory");
  }
  if (skills.length === 0) {
    warnings.push("No skills found in output directory");
  }
  return { agents, skills, hasHooks };
}

/** Render template and write AGENTS.md to outputDir. */
function renderAndWrite(
  engine: TemplateEngine,
  context: Record<string, unknown>,
  outputDir: string,
  warnings: string[],
): AssembleResult {
  let rendered: string;
  try {
    rendered = engine.renderTemplate(TEMPLATE_PATH, context);
  } catch (error: unknown) {
    const message = error instanceof Error
      ? error.message
      : String(error);
    if (message.includes("not found") || message.includes("ENOENT")) {
      warnings.push(`Template not found: ${TEMPLATE_PATH}`);
      return { files: [], warnings };
    }
    throw error;
  }
  fs.mkdirSync(outputDir, { recursive: true });
  const dest = path.join(outputDir, "AGENTS.md");
  fs.writeFileSync(dest, rendered, "utf-8");
  return { files: [dest], warnings };
}

/** Generates .codex/AGENTS.md from Nunjucks template. */
export class CodexAgentsMdAssembler {
  /** Generate .codex/AGENTS.md by scanning .claude/ output and rendering template. */
  assemble(
    config: ProjectConfig,
    outputDir: string,
    _resourcesDir: string,
    engine: TemplateEngine,
  ): AssembleResult {
    const warnings: string[] = [];
    const claudeDir = path.join(path.dirname(outputDir), ".claude");
    const { agents, skills, hasHooks } = collectContext(
      claudeDir, warnings,
    );
    const context = buildExtendedContext(
      config, agents, skills, hasHooks,
    );
    return renderAndWrite(engine, context, outputDir, warnings);
  }
}
