/**
 * Identity content builders for 01-project-identity.md generation.
 *
 * @module
 */
import type { ProjectConfig } from "../models.js";

const NONE_VALUE = "none";

/** Build the full 01-project-identity.md content. */
export function buildIdentityContent(config: ProjectConfig): string {
  const ifaces =
    config.interfaces.map((i) => i.type).join(", ") || NONE_VALUE;
  const fwVer = config.framework.version
    ? ` ${config.framework.version}`
    : "";
  const lines: string[] = [];
  lines.push(...identityHeader(config, ifaces, fwVer));
  lines.push(...identityTechStack(config, fwVer));
  lines.push(...identityFooter());
  return lines.join("\n") + "\n";
}

function identityHeader(
  config: ProjectConfig,
  ifaces: string,
  fwVer: string,
): string[] {
  return [
    "# Global Behavior & Language Policy",
    "- **Output Language**: English ONLY. (Mandatory for all responses and internal reasoning).",
    "- **Token Optimization**: Eliminate all greetings, apologies, and conversational fluff. " +
      "Start responses directly with technical information.",
    "- **Priority**: Maintain 100% fidelity to the technical constraints defined in the original rules below.",
    "",
    `# Project Identity — ${config.project.name}`,
    "",
    "## Identity",
    `- **Name:** ${config.project.name}`,
    `- **Purpose:** ${config.project.purpose}`,
    `- **Architecture Style:** ${config.architecture.style}`,
    `- **Domain-Driven Design:** ${String(config.architecture.domainDriven).toLowerCase()}`,
    `- **Event-Driven:** ${String(config.architecture.eventDriven).toLowerCase()}`,
    `- **Interfaces:** ${ifaces}`,
    `- **Language:** ${config.language.name} ${config.language.version}`,
    `- **Framework:** ${config.framework.name}${fwVer}`,
  ];
}

function identityTechStack(
  config: ProjectConfig,
  fwVer: string,
): string[] {
  const obs = config.infrastructure.observability;
  return [
    "",
    "## Technology Stack",
    "| Layer | Technology |",
    "|-------|-----------|",
    `| Architecture | ${config.architecture.style} |`,
    `| Language | ${config.language.name} ${config.language.version} |`,
    `| Framework | ${config.framework.name}${fwVer} |`,
    `| Build Tool | ${config.framework.buildTool} |`,
    `| Database | ${config.data.database.name} |`,
    `| Migration | ${config.data.migration.name} |`,
    `| Cache | ${config.data.cache.name} |`,
    `| Message Broker | none |`,
    `| Container | ${config.infrastructure.container} |`,
    `| Orchestrator | ${config.infrastructure.orchestrator} |`,
    `| Observability | ${obs.tool} (${obs.tracing}) |`,
    "| Resilience | Mandatory (always enabled) |",
    `| Native Build | ${String(config.framework.nativeBuild).toLowerCase()} |`,
    `| Smoke Tests | ${String(config.testing.smokeTests).toLowerCase()} |`,
    `| Contract Tests | ${String(config.testing.contractTests).toLowerCase()} |`,
  ];
}

function identityFooter(): string[] {
  return [
    "",
    "## Source of Truth (Hierarchy)",
    "1. Epics / PRDs (vision and global rules)",
    "2. ADRs (architectural decisions)",
    "3. Stories / tickets (detailed requirements)",
    "4. Rules (.claude/rules/)",
    "5. Source code",
    "",
    "## Language",
    "- Code: English (classes, methods, variables)",
    "- Commits: English (Conventional Commits)",
    "- Documentation: English (customize as needed)",
    "- Application logs: English",
    "",
    "## Constraints",
    "<!-- Customize constraints for your project -->",
    "- Cloud-Agnostic: ZERO dependencies on cloud-specific services",
    "- Horizontal scalability: Application must be stateless",
    "- Externalized configuration: All configuration via environment variables or ConfigMaps",
  ];
}

/** Fallback domain content when template is missing. */
export function fallbackDomainContent(config: ProjectConfig): string {
  return `# Rule — {DOMAIN_NAME} Domain\n\n${config.project.name}\n`;
}
