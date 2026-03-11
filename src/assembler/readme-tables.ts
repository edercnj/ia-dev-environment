/**
 * ReadmeAssembler table and section builders.
 *
 * Extracted from readme-assembler to respect the 250-line limit.
 * Builds markdown tables, hooks section, settings section,
 * mapping table, and generation summary.
 *
 * @module
 */
import * as fs from "node:fs";
import * as path from "node:path";
import type { ProjectConfig } from "../models.js";
import { DEFAULT_FOUNDATION } from "../models.js";
import {
  getHookTemplateKey,
  LANGUAGE_COMMANDS,
} from "../domain/stack-mapping.js";
import {
  countRules,
  countSkills,
  countAgents,
  countKnowledgePacks,
  countHooks,
  countSettings,
  countGithubFiles,
  countGithubComponent,
  countGithubSkills,
  isKnowledgePack,
  extractRuleNumber,
  extractRuleScope,
  extractSkillDescription,
} from "./readme-utils.js";

/** Build markdown table of rules with number, file, and scope. */
export function buildRulesTable(outputDir: string): string {
  const rulesDir = path.join(outputDir, "rules");
  if (!fs.existsSync(rulesDir)) return "No rules configured.";
  const files = fs.readdirSync(rulesDir)
    .filter((f) => f.endsWith(".md")).sort();
  if (files.length === 0) return "No rules configured.";
  const lines = ["| # | File | Scope |", "|---|------|-------|"];
  for (const fname of files) {
    const num = extractRuleNumber(fname);
    const scope = extractRuleScope(fname);
    lines.push(`| ${num} | \`${fname}\` | ${scope} |`);
  }
  return lines.join("\n");
}

/** Build markdown table of skills (excludes knowledge packs). */
export function buildSkillsTable(outputDir: string): string {
  const skillsDir = path.join(outputDir, "skills");
  if (!fs.existsSync(skillsDir)) return "No skills configured.";
  const dirs = fs.readdirSync(skillsDir, { withFileTypes: true })
    .filter((d) => d.isDirectory())
    .map((d) => d.name).sort();
  const rows: string[] = [];
  for (const sname of dirs) {
    const skillMd = path.join(skillsDir, sname, "SKILL.md");
    if (!fs.existsSync(skillMd)) continue;
    if (isKnowledgePack(skillMd)) continue;
    const sdesc = extractSkillDescription(skillMd);
    rows.push(`| **${sname}** | \`/${sname}\` | ${sdesc} |`);
  }
  if (rows.length === 0) return "No skills configured.";
  const header = [
    "| Skill | Path | Description |",
    "|-------|------|-------------|",
  ];
  return [...header, ...rows].join("\n");
}

/** Build markdown table of agents. */
export function buildAgentsTable(outputDir: string): string {
  const agentsDir = path.join(outputDir, "agents");
  if (!fs.existsSync(agentsDir)) return "No agents configured.";
  const files = fs.readdirSync(agentsDir)
    .filter((f) => f.endsWith(".md")).sort();
  if (files.length === 0) return "No agents configured.";
  const rows: string[] = [];
  for (const fname of files) {
    const aname = path.basename(fname, ".md");
    rows.push(`| **${aname}** | \`${fname}\` |`);
  }
  const header = ["| Agent | File |", "|-------|------|"];
  return [...header, ...rows].join("\n");
}

/** Build markdown table of knowledge packs. */
export function buildKnowledgePacksTable(
  outputDir: string,
): string {
  const skillsDir = path.join(outputDir, "skills");
  if (!fs.existsSync(skillsDir)) {
    return "No knowledge packs configured.";
  }
  const dirs = fs.readdirSync(skillsDir, { withFileTypes: true })
    .filter((d) => d.isDirectory())
    .map((d) => d.name).sort();
  const rows: string[] = [];
  for (const sname of dirs) {
    const skillMd = path.join(skillsDir, sname, "SKILL.md");
    if (!fs.existsSync(skillMd)) continue;
    if (!isKnowledgePack(skillMd)) continue;
    rows.push(
      `| \`${sname}\` | Referenced internally by agents |`,
    );
  }
  if (rows.length === 0) {
    return "No knowledge packs configured.";
  }
  const header = "| Pack | Usage |\n|------|-------|";
  return header + "\n" + rows.join("\n");
}

/** Build hooks documentation section. */
export function buildReadmeHooksSection(config: ProjectConfig): string {
  const key = getHookTemplateKey(
    config.language.name, config.framework.buildTool,
  );
  if (!key) return "No hooks configured.";
  const langKey = `${config.language.name}-${config.framework.buildTool}`;
  const commands = LANGUAGE_COMMANDS[langKey];
  const ext = commands?.fileExtension ?? "";
  const compileCmd = commands?.compileCmd ?? "";
  return [
    "### Post-Compile Check",
    "",
    "- **Event:** `PostToolUse` (after `Write` or `Edit`)",
    "- **Script:** `.claude/hooks/post-compile-check.sh`",
    `- **Behavior:** When a \`${ext}\` file is modified,`
      + ` runs \`${compileCmd}\` automatically`,
    "- **Purpose:** Catch compilation errors immediately"
      + " after file changes",
  ].join("\n");
}

/** Build static settings section. */
export function buildSettingsSection(): string {
  return [
    "### settings.json",
    "",
    "Permissions are configured in `settings.json`"
      + " under `permissions.allow`.",
    "This controls which Bash commands Claude Code"
      + " can run without asking.",
    "",
    "### settings.local.json",
    "",
    "Local overrides (gitignored). Use for personal"
      + " preferences or team-specific tools.",
    "",
    "See the files directly for current configuration.",
  ].join("\n");
}

/** Resolve the sibling .github/ directory relative to the .claude/ outputDir. */
function resolveGithubDir(outputDir: string): string {
  return path.join(path.dirname(outputDir), ".github");
}

/** Build the `.claude/ <-> .github/` mapping table. */
export function buildMappingTable(outputDir: string): string {
  const rows = buildMappingRows();
  const lines = [
    "| .claude/ | .github/ | Notes |",
    "|----------|----------|-------|",
  ];
  for (const [claude, github, notes] of rows) {
    lines.push(`| ${claude} | ${github} | ${notes} |`);
  }
  const githubDir = resolveGithubDir(outputDir);
  const ghTotal = fs.existsSync(githubDir)
    ? countGithubFiles(githubDir) : 0;
  if (ghTotal > 0) {
    lines.push("");
    lines.push(`**Total .github/ artifacts: ${ghTotal}**`);
  }
  return lines.join("\n");
}

/** Build the generation summary with component counts. */
export function buildGenerationSummary(
  outputDir: string, config: ProjectConfig,
): string {
  const githubDir = resolveGithubDir(outputDir);
  const rows = buildSummaryRows(outputDir, githubDir);
  const lines = ["| Component | Count |", "|-----------|-------|"];
  for (const [label, count] of rows) {
    lines.push(`| ${label} | ${count} |`);
  }
  lines.push("");
  const ver = DEFAULT_FOUNDATION.version;
  lines.push(`Generated by \`ia-dev-env v${ver}\`.`);
  return lines.join("\n");
}

function buildMappingRows(): [string, string, string][] {
  return [
    ["Rules (`rules/*.md`)", "Instructions (`instructions/*.instructions.md`)", "Rules are system-prompt loaded; instructions are contextual"],
    ["Skills (`skills/*/SKILL.md`)", "Skills (`skills/*/SKILL.md`)", "Same structure, same YAML frontmatter"],
    ["Agents (`agents/*.md`)", "Agents (`agents/*.agent.md`)", "GitHub agents use `.agent.md` extension with YAML frontmatter"],
    ["Hooks (`hooks/`)", "Hooks (`hooks/*.json`)", "Both define event-driven automations"],
    ["Settings (`settings*.json`)", "N/A", "Claude Code specific"],
    ["N/A", "Prompts (`prompts/*.prompt.md`)", "GitHub Copilot prompt templates"],
    ["N/A", "MCP (`copilot-mcp.json`)", "GitHub Copilot MCP server configuration"],
    ["N/A", "Global instructions (`copilot-instructions.md`)", "Loaded in every Copilot session"],
  ];
}

function buildSummaryRows(
  outputDir: string, githubDir: string,
): [string, number][] {
  const kps = countKnowledgePacks(outputDir);
  const ghGlobal = fs.existsSync(
    path.join(githubDir, "copilot-instructions.md"),
  ) ? 1 : 0;
  const ghMcp = fs.existsSync(
    path.join(githubDir, "copilot-mcp.json"),
  ) ? 1 : 0;
  return [
    ["Rules (.claude)", countRules(outputDir)],
    ["Skills (.claude)", countSkills(outputDir) - kps],
    ["Knowledge Packs (.claude)", kps],
    ["Agents (.claude)", countAgents(outputDir)],
    ["Hooks (.claude)", countHooks(outputDir)],
    ["Settings (.claude)", countSettings(outputDir)],
    ["Instructions (.github)", countGithubComponent(githubDir, "instructions") + ghGlobal],
    ["Skills (.github)", countGithubSkills(githubDir)],
    ["Agents (.github)", countGithubComponent(githubDir, "agents")],
    ["Prompts (.github)", countGithubComponent(githubDir, "prompts")],
    ["Hooks (.github)", countGithubComponent(githubDir, "hooks")],
    ["MCP (.github)", ghMcp],
  ];
}
