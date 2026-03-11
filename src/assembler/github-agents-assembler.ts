/**
 * GithubAgentsAssembler -- generates github/agents/*.agent.md from templates.
 *
 * Migrated from Python `assembler/github_agents_assembler.py` (200 lines).
 * Output uses `.agent.md` extension (GitHub convention), not `.md` (Claude convention).
 *
 * @module
 */
import * as fs from "node:fs";
import * as path from "node:path";
import type { ProjectConfig } from "../models.js";
import type { TemplateEngine } from "../template-engine.js";
import { hasAnyInterface } from "./conditions.js";
import type { AssembleResult } from "./rules-assembler.js";

const GITHUB_AGENTS_TEMPLATES_DIR = "github-agents-templates";
const CORE_DIR = "core";
const CONDITIONAL_DIR = "conditional";
const DEVELOPERS_DIR = "developers";
const AGENT_MD_EXTENSION = ".agent.md";
const MD_EXTENSION = ".md";

/**
 * Select conditional agent filenames based on project configuration.
 *
 * @param config - The project configuration to evaluate.
 * @returns Array of agent template filenames to include.
 */
export function selectGithubConditionalAgents(
  config: ProjectConfig,
): string[] {
  const agents: string[] = [];
  const infra = config.infrastructure;
  const hasDevops =
    infra.container !== "none" ||
    infra.orchestrator !== "none" ||
    infra.iac !== "none" ||
    infra.serviceMesh !== "none";
  if (hasDevops) {
    agents.push("devops-engineer.md");
  }
  if (hasAnyInterface(config, "rest", "grpc", "graphql")) {
    agents.push("api-engineer.md");
  }
  const hasEvents =
    config.architecture.eventDriven ||
    hasAnyInterface(config, "event-consumer", "event-producer");
  if (hasEvents) {
    agents.push("event-engineer.md");
  }
  return agents;
}

/** Generates github/agents/*.agent.md with core, conditional, and developer agents. */
export class GithubAgentsAssembler {
  /** Main entry point: assemble all GitHub agents. */
  assemble(
    config: ProjectConfig,
    outputDir: string,
    resourcesDir: string,
    engine: TemplateEngine,
  ): AssembleResult {
    const agentsDir = path.join(outputDir, "agents");
    fs.mkdirSync(agentsDir, { recursive: true });
    const files: string[] = [];
    const warnings: string[] = [];
    files.push(
      ...this.assembleCore(resourcesDir, agentsDir, engine),
    );
    files.push(
      ...this.assembleConditional(
        config, resourcesDir, agentsDir, engine, warnings,
      ),
    );
    const dev = this.assembleDeveloper(
      config, resourcesDir, agentsDir, engine,
    );
    if (dev !== null) {
      files.push(dev);
    } else {
      const expected = `${config.language.name}-developer.md`;
      warnings.push(
        `Developer agent template missing: ${expected}`,
      );
    }
    return { files, warnings };
  }

  private assembleCore(
    resourcesDir: string,
    agentsDir: string,
    engine: TemplateEngine,
  ): string[] {
    const coreDir = path.join(
      resourcesDir, GITHUB_AGENTS_TEMPLATES_DIR, CORE_DIR,
    );
    if (!fs.existsSync(coreDir)) return [];
    const entries = fs
      .readdirSync(coreDir, { withFileTypes: true })
      .filter((e) => e.isFile())
      .sort((a, b) => a.name.localeCompare(b.name));
    return entries.map(
      (e) => this.renderAgent(
        path.join(coreDir, e.name), agentsDir, engine,
      ),
    );
  }

  private assembleConditional(
    config: ProjectConfig,
    resourcesDir: string,
    agentsDir: string,
    engine: TemplateEngine,
    warnings: string[],
  ): string[] {
    const condDir = path.join(
      resourcesDir, GITHUB_AGENTS_TEMPLATES_DIR, CONDITIONAL_DIR,
    );
    if (!fs.existsSync(condDir)) return [];
    const results: string[] = [];
    for (const name of selectGithubConditionalAgents(config)) {
      const src = path.join(condDir, name);
      if (!fs.existsSync(src)) {
        warnings.push(
          `Conditional agent template missing: ${name}`,
        );
        continue;
      }
      results.push(this.renderAgent(src, agentsDir, engine));
    }
    return results;
  }

  private assembleDeveloper(
    config: ProjectConfig,
    resourcesDir: string,
    agentsDir: string,
    engine: TemplateEngine,
  ): string | null {
    const devDir = path.join(
      resourcesDir, GITHUB_AGENTS_TEMPLATES_DIR, DEVELOPERS_DIR,
    );
    if (!fs.existsSync(devDir)) return null;
    const safeName = path.basename(config.language.name);
    const template = path.join(
      devDir, `${safeName}-developer.md`,
    );
    if (!fs.existsSync(template)) return null;
    return this.renderAgent(template, agentsDir, engine);
  }

  private renderAgent(
    srcPath: string,
    agentsDir: string,
    engine: TemplateEngine,
  ): string {
    const content = fs.readFileSync(srcPath, "utf-8");
    const rendered = engine.replacePlaceholders(content);
    const stem = path.basename(srcPath, MD_EXTENSION);
    const outputName = `${stem}${AGENT_MD_EXTENSION}`;
    const dest = path.join(agentsDir, outputName);
    fs.writeFileSync(dest, rendered, "utf-8");
    return dest;
  }
}
