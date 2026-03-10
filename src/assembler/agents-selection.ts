/**
 * Agent selection logic: conditional agents and checklist injection rules.
 *
 * Pure functions with no I/O — consumed by {@link AgentsAssembler}.
 *
 * @module
 */
import type { ProjectConfig, InfraConfig } from "../models.js";
import { hasInterface, hasAnyInterface } from "./conditions.js";

const MD_EXTENSION = ".md";

/** A (targetAgent, checklistFile, condition) tuple for checklist injection. */
export interface ChecklistRule {
  readonly agent: string;
  readonly checklist: string;
  readonly active: boolean;
}

/** Select conditional agent filenames based on project configuration. */
export function selectConditionalAgents(
  config: ProjectConfig,
): string[] {
  return [
    ...selectDataAgents(config),
    ...selectInfraAgents(config),
    ...selectInterfaceAgents(config),
    ...selectEventAgents(config),
  ];
}

/** Build the complete list of checklist injection rules. */
export function buildChecklistRules(
  config: ProjectConfig,
): ChecklistRule[] {
  return [
    ...securityChecklistRules(config.security.frameworks),
    ...apiChecklistRules(config),
    ...devopsChecklistRules(config.infrastructure),
  ];
}

/** Derive marker string from checklist filename. */
export function checklistMarker(checklistFile: string): string {
  const name = checklistFile
    .replace(MD_EXTENSION, "")
    .toUpperCase()
    .replaceAll("-", "_");
  return `<!-- ${name} -->`;
}

function selectDataAgents(config: ProjectConfig): string[] {
  if (config.data.database.name !== "none") {
    return ["database-engineer.md"];
  }
  return [];
}

function selectInfraAgents(config: ProjectConfig): string[] {
  const agents: string[] = [];
  const infra = config.infrastructure;
  if (infra.observability.tool !== "none") {
    agents.push("observability-engineer.md");
  }
  const hasDevops =
    infra.container !== "none" ||
    infra.orchestrator !== "none" ||
    infra.iac !== "none" ||
    infra.serviceMesh !== "none";
  if (hasDevops) {
    agents.push("devops-engineer.md");
  }
  return agents;
}

function selectInterfaceAgents(config: ProjectConfig): string[] {
  if (hasAnyInterface(config, "rest", "grpc", "graphql")) {
    return ["api-engineer.md"];
  }
  return [];
}

function selectEventAgents(config: ProjectConfig): string[] {
  const hasEvents =
    config.architecture.eventDriven ||
    hasAnyInterface(config, "event-consumer", "event-producer");
  if (hasEvents) {
    return ["event-engineer.md"];
  }
  return [];
}

function securityChecklistRules(
  frameworks: readonly string[],
): ChecklistRule[] {
  const hasPrivacy =
    frameworks.includes("lgpd") || frameworks.includes("gdpr");
  return [
    { agent: "security-engineer.md", checklist: "pci-dss-security.md", active: frameworks.includes("pci-dss") },
    { agent: "security-engineer.md", checklist: "privacy-security.md", active: hasPrivacy },
    { agent: "security-engineer.md", checklist: "hipaa-security.md", active: frameworks.includes("hipaa") },
    { agent: "security-engineer.md", checklist: "sox-security.md", active: frameworks.includes("sox") },
  ];
}

function apiChecklistRules(config: ProjectConfig): ChecklistRule[] {
  return [
    { agent: "api-engineer.md", checklist: "grpc-api.md", active: hasInterface(config, "grpc") },
    { agent: "api-engineer.md", checklist: "graphql-api.md", active: hasInterface(config, "graphql") },
    { agent: "api-engineer.md", checklist: "websocket-api.md", active: hasInterface(config, "websocket") },
  ];
}

function devopsChecklistRules(infra: InfraConfig): ChecklistRule[] {
  return [
    { agent: "devops-engineer.md", checklist: "helm-devops.md", active: infra.templating === "helm" },
    { agent: "devops-engineer.md", checklist: "iac-devops.md", active: infra.iac !== "none" },
    { agent: "devops-engineer.md", checklist: "mesh-devops.md", active: infra.serviceMesh !== "none" },
    { agent: "devops-engineer.md", checklist: "registry-devops.md", active: infra.registry !== "none" },
  ];
}
