/**
 * Core rule to knowledge pack routing.
 *
 * Migrated from Python `domain/core_kp_routing.py`.
 */

import type { ProjectConfig } from "../models.js";

/** Maps a core rule source file to a knowledge pack destination. */
export interface CoreKpRoute {
  readonly sourceFile: string;
  readonly kpName: string;
  readonly destFile: string;
}

/** Supported condition fields for conditional routing. */
export type ConditionField = "architecture_style";

/** Route that is conditionally included based on config values. */
export interface ConditionalCoreKpRoute extends CoreKpRoute {
  readonly conditionField: ConditionField;
  readonly conditionExclude: string;
}

/** 12 static routes from core rules to knowledge packs. */
export const CORE_TO_KP_MAPPING: readonly CoreKpRoute[] = [
  { sourceFile: "01-clean-code.md", kpName: "coding-standards", destFile: "clean-code.md" },
  { sourceFile: "02-solid-principles.md", kpName: "coding-standards", destFile: "solid-principles.md" },
  { sourceFile: "03-testing-philosophy.md", kpName: "testing", destFile: "testing-philosophy.md" },
  { sourceFile: "05-architecture-principles.md", kpName: "architecture", destFile: "architecture-principles.md" },
  { sourceFile: "06-api-design-principles.md", kpName: "api-design", destFile: "api-design-principles.md" },
  { sourceFile: "07-security-principles.md", kpName: "security", destFile: "security-principles.md" },
  { sourceFile: "08-observability-principles.md", kpName: "observability", destFile: "observability-principles.md" },
  { sourceFile: "09-resilience-principles.md", kpName: "resilience", destFile: "resilience-principles.md" },
  { sourceFile: "10-infrastructure-principles.md", kpName: "infrastructure", destFile: "infrastructure-principles.md" },
  { sourceFile: "11-database-principles.md", kpName: "database-patterns", destFile: "database-principles.md" },
  { sourceFile: "13-story-decomposition.md", kpName: "story-planning", destFile: "story-decomposition.md" },
  { sourceFile: "14-refactoring-guidelines.md", kpName: "coding-standards", destFile: "refactoring-guidelines.md" },
];

/** 1 conditional route: cloud-native excluded for library style. */
export const CONDITIONAL_CORE_KP: readonly ConditionalCoreKpRoute[] = [
  {
    sourceFile: "12-cloud-native-principles.md",
    kpName: "infrastructure",
    destFile: "cloud-native-principles.md",
    conditionField: "architecture_style",
    conditionExclude: "library",
  },
];

/** Return all routes whose conditions are met for the given config. */
export function getActiveRoutes(config: ProjectConfig): CoreKpRoute[] {
  const routes: CoreKpRoute[] = [...CORE_TO_KP_MAPPING];
  for (const route of CONDITIONAL_CORE_KP) {
    const configValue = resolveConditionValue(config, route.conditionField);
    if (configValue !== route.conditionExclude) {
      routes.push(route);
    }
  }
  return routes;
}

/** Resolve a condition field name to a config value. */
function resolveConditionValue(config: ProjectConfig, field: ConditionField): string {
  switch (field) {
    case "architecture_style":
      return config.architecture.style;
  }
}
