/**
 * Skill registry constants and helpers.
 *
 * Migrated from Python `domain/skill_registry.py`.
 * Provides CORE_KNOWLEDGE_PACKS and infrastructure pack rule builder.
 */
import type { InfraConfig } from "../models.js";

/** Core knowledge packs included in every generation (11 entries). */
export const CORE_KNOWLEDGE_PACKS: readonly string[] = [
  "coding-standards",
  "architecture",
  "testing",
  "security",
  "compliance",
  "api-design",
  "observability",
  "resilience",
  "infrastructure",
  "protocols",
  "story-planning",
] as const;

/**
 * Build conditional infrastructure pack rules.
 *
 * Returns tuples of `[packName, condition]` indicating
 * whether each infrastructure pack should be included.
 */
export function buildInfraPackRules(
  config: { readonly infrastructure: InfraConfig },
): ReadonlyArray<readonly [string, boolean]> {
  const infra = config.infrastructure;
  return [
    ["k8s-deployment", infra.orchestrator === "kubernetes"],
    ["k8s-kustomize", infra.templating === "kustomize"],
    ["k8s-helm", infra.templating === "helm"],
    ["dockerfile", infra.container !== "none"],
    ["container-registry", infra.registry !== "none"],
    ["iac-terraform", infra.iac === "terraform"],
    ["iac-crossplane", infra.iac === "crossplane"],
  ];
}
