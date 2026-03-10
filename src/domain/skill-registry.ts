/**
 * Skill registry constants and helpers.
 *
 * Migrated from Python `domain/skill_registry.py`.
 * Provides CORE_KNOWLEDGE_PACKS and infrastructure pack rule builder.
 */
import type { InfraConfig } from "../models.js";

/** Core knowledge packs included in every generation (11 entries). */
export const CORE_KNOWLEDGE_PACKS: readonly string[] = Object.freeze([
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
]);

/** Fields of InfraConfig used in condition evaluation. */
type ConditionField =
  | "orchestrator"
  | "templating"
  | "container"
  | "registry"
  | "iac";

function fieldEquals(
  infra: InfraConfig, field: ConditionField, value: string,
): boolean {
  return infra[field] === value;
}

function fieldNotEquals(
  infra: InfraConfig, field: ConditionField, value: string,
): boolean {
  return infra[field] !== value;
}

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
    ["k8s-deployment", fieldEquals(infra, "orchestrator", "kubernetes")],
    ["k8s-kustomize", fieldEquals(infra, "templating", "kustomize")],
    ["k8s-helm", fieldEquals(infra, "templating", "helm")],
    ["dockerfile", fieldNotEquals(infra, "container", "none")],
    ["container-registry", fieldNotEquals(infra, "registry", "none")],
    ["iac-terraform", fieldEquals(infra, "iac", "terraform")],
    ["iac-crossplane", fieldEquals(infra, "iac", "crossplane")],
  ];
}
