/**
 * GithubInstructionsAssembler -- generates copilot-instructions.md and contextual files.
 *
 * Migrated from Python `assembler/github_instructions_assembler.py` (156 lines).
 * The global file is built programmatically; contextual files use placeholder replacement.
 *
 * @module
 */
import * as fs from "node:fs";
import * as path from "node:path";
import type { ProjectConfig } from "../models.js";
import type { TemplateEngine } from "../template-engine.js";

const INSTRUCTIONS_TEMPLATES_DIR = "github-instructions-templates";

/** Contextual instruction template names (without extension). */
export const CONTEXTUAL_INSTRUCTIONS: readonly string[] = [
  "domain",
  "coding-standards",
  "architecture",
  "quality-gates",
] as const;

/**
 * Build the complete copilot-instructions.md content programmatically.
 *
 * @param config - The project configuration.
 * @returns The full markdown content with trailing newline.
 */
export function buildCopilotInstructions(
  config: ProjectConfig,
): string {
  const ifaces = formatInterfaces(config);
  const fwVer = formatFrameworkVersion(config);
  const lines = [
    ...buildIdentitySection(config, ifaces, fwVer),
    ...buildStackSection(config, fwVer),
    ...buildConstraintsSection(),
    ...buildContextualRefsSection(),
  ];
  return lines.join("\n") + "\n";
}

function formatInterfaces(config: ProjectConfig): string {
  if (config.interfaces.length === 0) return "none";
  return config.interfaces
    .map((i) =>
      i.type === "rest" || i.type === "grpc"
        ? i.type.toUpperCase()
        : i.type,
    )
    .join(", ");
}

function formatFrameworkVersion(
  config: ProjectConfig,
): string {
  return config.framework.version
    ? ` ${config.framework.version}`
    : "";
}

function buildIdentitySection(
  config: ProjectConfig,
  ifaces: string,
  fwVer: string,
): string[] {
  return [
    `# Project Identity \u2014 ${config.project.name}`,
    "",
    "## Identity",
    "",
    `- **Name:** ${config.project.name}`,
    `- **Architecture Style:** ${config.architecture.style}`,
    `- **Domain-Driven Design:** ${String(config.architecture.domainDriven).toLowerCase()}`,
    `- **Event-Driven:** ${String(config.architecture.eventDriven).toLowerCase()}`,
    `- **Interfaces:** ${ifaces}`,
    `- **Language:** ${config.language.name} ${config.language.version}`,
    `- **Framework:** ${config.framework.name}${fwVer}`,
    "",
  ];
}

function buildStackSection(
  config: ProjectConfig,
  fwVer: string,
): string[] {
  const cap = (s: string): string =>
    s.charAt(0).toUpperCase() + s.slice(1);
  return [
    "## Technology Stack",
    "",
    "| Layer | Technology |",
    "|-------|-----------|",
    `| Architecture | ${cap(config.architecture.style)} |`,
    `| Language | ${cap(config.language.name)} ${config.language.version} |`,
    `| Framework | ${cap(config.framework.name)}${fwVer} |`,
    `| Build Tool | ${cap(config.framework.buildTool)} |`,
    `| Container | ${cap(config.infrastructure.container)} |`,
    `| Orchestrator | ${cap(config.infrastructure.orchestrator)} |`,
    "| Resilience | Mandatory (always enabled) |",
    `| Native Build | ${String(config.framework.nativeBuild).toLowerCase()} |`,
    `| Smoke Tests | ${String(config.testing.smokeTests).toLowerCase()} |`,
    `| Contract Tests | ${String(config.testing.contractTests).toLowerCase()} |`,
    "",
  ];
}

function buildConstraintsSection(): string[] {
  return [
    "## Constraints",
    "",
    "- Cloud-Agnostic: ZERO dependencies on cloud-specific services",
    "- Horizontal scalability: Application must be stateless",
    "- Externalized configuration: All configuration via environment variables or ConfigMaps",
    "",
  ];
}

function buildContextualRefsSection(): string[] {
  return [
    "## Contextual Instructions",
    "",
    "The following instruction files provide domain-specific context:",
    "",
    "- `instructions/domain.instructions.md` \u2014 Domain model, business rules, sensitive data",
    "- `instructions/coding-standards.instructions.md` \u2014 Clean Code, SOLID, naming, error handling",
    "- `instructions/architecture.instructions.md` \u2014 Hexagonal architecture, layer rules, package structure",
    "- `instructions/quality-gates.instructions.md` \u2014 Coverage thresholds, test categories, merge checklist",
    "",
    "For deep-dive references, see the knowledge packs in `.claude/skills/` (generated alongside this structure).",
  ];
}

/** Generates copilot-instructions.md (global) and contextual instruction files. */
export class GithubInstructionsAssembler {
  /** Generate global and contextual instruction files. */
  assemble(
    config: ProjectConfig,
    outputDir: string,
    resourcesDir: string,
    engine: TemplateEngine,
  ): string[] {
    const results: string[] = [];
    const githubDir = path.join(outputDir, "github");
    fs.mkdirSync(githubDir, { recursive: true });
    results.push(this.generateGlobal(config, githubDir));
    const instructionsDir = path.join(githubDir, "instructions");
    fs.mkdirSync(instructionsDir, { recursive: true });
    results.push(
      ...this.generateContextual(
        engine, resourcesDir, instructionsDir,
      ),
    );
    return results;
  }

  private generateGlobal(
    config: ProjectConfig,
    githubDir: string,
  ): string {
    const content = buildCopilotInstructions(config);
    const dest = path.join(githubDir, "copilot-instructions.md");
    fs.writeFileSync(dest, content, "utf-8");
    return dest;
  }

  private generateContextual(
    engine: TemplateEngine,
    resourcesDir: string,
    instructionsDir: string,
  ): string[] {
    const srcDir = path.join(
      resourcesDir, INSTRUCTIONS_TEMPLATES_DIR,
    );
    if (!fs.existsSync(srcDir)) return [];
    const results: string[] = [];
    for (const name of CONTEXTUAL_INSTRUCTIONS) {
      const src = path.join(srcDir, `${name}.md`);
      if (!fs.existsSync(src)) continue;
      const content = fs.readFileSync(src, "utf-8");
      const rendered = engine.replacePlaceholders(content);
      const dest = path.join(
        instructionsDir, `${name}.instructions.md`,
      );
      fs.writeFileSync(dest, rendered, "utf-8");
      results.push(dest);
    }
    return results;
  }
}
