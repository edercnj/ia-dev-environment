/**
 * Pattern selection by architecture style.
 *
 * Migrated from Python `domain/pattern_mapping.py`.
 */

import type { ProjectConfig } from "../models.js";

import { readdirSync, statSync } from "node:fs";
import { join } from "node:path";

/** Universal patterns included for all known styles. */
export const UNIVERSAL_PATTERNS: readonly string[] = ["architectural", "data"];

/** Architecture style to pattern categories. */
export const ARCHITECTURE_PATTERNS: Readonly<Record<string, readonly string[]>> = {
  "microservice": ["microservice", "resilience", "integration"],
  "hexagonal": ["integration"],
  "monolith": ["integration"],
  "library": [],
};

/** Event-driven architecture patterns. */
export const EVENT_DRIVEN_PATTERNS: readonly string[] = [
  "saga-pattern",
  "outbox-pattern",
  "event-sourcing",
  "dead-letter-queue",
];

/**
 * Map architecture style to pattern category directories.
 *
 * Returns sorted, deduplicated list of pattern category names.
 * Returns empty list for unknown styles.
 */
export function selectPatterns(config: ProjectConfig): string[] {
  const style = config.architecture.style;
  const stylePatterns = ARCHITECTURE_PATTERNS[style];
  if (stylePatterns === undefined) {
    return [];
  }
  const categories = [...UNIVERSAL_PATTERNS, ...stylePatterns];
  if (config.architecture.eventDriven) {
    categories.push(...EVENT_DRIVEN_PATTERNS);
  }
  return [...new Set(categories)].sort();
}

/**
 * List .md files from pattern category directories.
 *
 * Skips missing directories without error.
 * Returns sorted list of file paths.
 */
export function selectPatternFiles(
  resourcesDir: string,
  patternCategories: readonly string[],
): string[] {
  const patternsRoot = join(resourcesDir, "patterns");
  const files: string[] = [];
  for (const category of patternCategories) {
    const categoryDir = join(patternsRoot, category);
    try {
      if (!statSync(categoryDir).isDirectory()) {
        continue;
      }
    } catch {
      continue;
    }
    const entries = readdirSync(categoryDir)
      .filter((f) => f.endsWith(".md"))
      .sort();
    for (const entry of entries) {
      files.push(join(categoryDir, entry));
    }
  }
  return files;
}
