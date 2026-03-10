/**
 * Pure skill selection functions based on project config feature gates.
 *
 * These functions evaluate config conditions and return skill/pack names.
 * No file I/O — consumed by {@link SkillsAssembler} for assembly decisions.
 *
 * @module
 */
import type { ProjectConfig } from "../models.js";
import { hasInterface, hasAnyInterface } from "./conditions.js";
import { CORE_KNOWLEDGE_PACKS } from "../domain/skill-registry.js";

/** Select skills based on interface types. */
export function selectInterfaceSkills(config: ProjectConfig): string[] {
  const skills: string[] = [];
  if (hasInterface(config, "rest")) skills.push("x-review-api");
  if (hasInterface(config, "grpc")) skills.push("x-review-grpc");
  if (hasInterface(config, "graphql")) skills.push("x-review-graphql");
  if (hasAnyInterface(config, "event-consumer", "event-producer")) {
    skills.push("x-review-events");
  }
  return skills;
}

/** Select skills based on infrastructure config. */
export function selectInfraSkills(config: ProjectConfig): string[] {
  const skills: string[] = [];
  if (config.infrastructure.observability.tool !== "none") {
    skills.push("instrument-otel");
  }
  if (config.infrastructure.orchestrator !== "none") {
    skills.push("setup-environment");
  }
  if (config.infrastructure.apiGateway !== "none") {
    skills.push("x-review-gateway");
  }
  return skills;
}

/** Select skills based on testing config. */
export function selectTestingSkills(config: ProjectConfig): string[] {
  const skills: string[] = [];
  if (config.testing.smokeTests && hasInterface(config, "rest")) {
    skills.push("run-smoke-api");
  }
  if (config.testing.smokeTests && hasInterface(config, "tcp-custom")) {
    skills.push("run-smoke-socket");
  }
  skills.push("run-e2e");
  if (config.testing.performanceTests) skills.push("run-perf-test");
  if (config.testing.contractTests) skills.push("run-contract-tests");
  return skills;
}

/** Select skills based on security config. */
export function selectSecuritySkills(config: ProjectConfig): string[] {
  if (config.security.frameworks.length > 0) {
    return ["x-review-security"];
  }
  return [];
}

/** Evaluate all feature gates and return conditional skill names. */
export function selectConditionalSkills(config: ProjectConfig): string[] {
  return [
    ...selectInterfaceSkills(config),
    ...selectInfraSkills(config),
    ...selectTestingSkills(config),
    ...selectSecuritySkills(config),
  ];
}

/** Select data-related knowledge packs. */
function selectDataPacks(config: ProjectConfig): string[] {
  if (
    config.data.database.name !== "none" ||
    config.data.cache.name !== "none"
  ) {
    return ["database-patterns"];
  }
  return [];
}

/** Determine which knowledge packs to include. */
export function selectKnowledgePacks(config: ProjectConfig): string[] {
  const packs = [...CORE_KNOWLEDGE_PACKS];
  packs.push("layer-templates");
  packs.push(...selectDataPacks(config));
  return packs;
}
