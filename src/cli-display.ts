/**
 * CLI display utilities — file classification, summary table formatting,
 * and pipeline result display.
 *
 * Migrated from Python `__main__.py` display functions.
 *
 * @module
 */
import { existsSync, readFileSync } from "node:fs";
import { basename, dirname, join } from "node:path";
import type { PipelineResult } from "./models.js";

/** Category counts for the summary table. */
export interface ComponentCounts {
  readonly rules: number;
  readonly skills: number;
  readonly knowledgePacks: number;
  readonly agents: number;
  readonly hooks: number;
  readonly settings: number;
  readonly readme: number;
  readonly github: number;
}

const SKILL_MD_FILENAME = "SKILL.md";
const KP_MARKER_INVOCABLE = "user-invocable: false";
const KP_MARKER_HEADING = "# Knowledge Pack";
const SEPARATOR_CHAR = "\u2500";
const HEADER_LABEL = "Component";
const HEADER_COUNT = "Files";
const TOTAL_LABEL = "Total";

/** Ordered label-to-key mapping for the summary table. */
const CATEGORY_LABELS: ReadonlyArray<readonly [string, keyof ComponentCounts]> = [
  ["Rules (.claude)", "rules"],
  ["Skills (.claude)", "skills"],
  ["Knowledge Packs (.claude)", "knowledgePacks"],
  ["Agents (.claude)", "agents"],
  ["Hooks (.claude)", "hooks"],
  ["Settings (.claude)", "settings"],
  ["README", "readme"],
  ["GitHub", "github"],
];

/**
 * Check whether a skill file belongs to a knowledge pack.
 *
 * If the file is not SKILL.md itself, looks for SKILL.md in the same directory.
 * Returns true if SKILL.md contains "user-invocable: false"
 * or starts with "# Knowledge Pack".
 *
 * @param filePath - Absolute or relative path to a file within a skill directory.
 * @returns true if the file belongs to a knowledge pack, false otherwise.
 */
export function isKnowledgePackFile(filePath: string): boolean {
  if (!existsSync(filePath)) {
    return false;
  }

  let skillMdPath = filePath;
  if (basename(filePath) !== SKILL_MD_FILENAME) {
    const candidate = join(dirname(filePath), SKILL_MD_FILENAME);
    if (!existsSync(candidate)) {
      return false;
    }
    skillMdPath = candidate;
  }

  const content = readFileSync(skillMdPath, "utf-8");
  if (content.includes(KP_MARKER_INVOCABLE)) {
    return true;
  }
  return content.trimStart().startsWith(KP_MARKER_HEADING);
}

/**
 * Split a file path into its directory and filename segments.
 *
 * @param filePath - File path to split.
 * @returns Array of path segments.
 */
function splitPathSegments(filePath: string): readonly string[] {
  return filePath.split(/[\\/]/);
}

/**
 * Classify a single file path into a component category.
 *
 * @param filePath - Path to classify.
 * @param segments - Pre-split path segments.
 * @returns The matching category key, or undefined if no match.
 */
function classifySingleFile(
  filePath: string,
  segments: readonly string[],
): keyof ComponentCounts | undefined {
  const fileName = segments[segments.length - 1] ?? "";
  if (segments.includes("github")) return "github";
  if (fileName.includes("README")) return "readme";
  if (fileName.includes("settings")) return "settings";
  if (segments.includes("hooks")) return "hooks";
  if (segments.includes("agents")) return "agents";
  if (segments.includes("skills")) {
    return isKnowledgePackFile(filePath)
      ? "knowledgePacks"
      : "skills";
  }
  if (segments.includes("rules")) return "rules";
  return undefined;
}

/**
 * Classify generated file paths into component categories.
 *
 * Categorizes by path segments (github, rules, skills, agents, hooks)
 * and file name patterns (README, settings). GitHub takes priority
 * over other categories. For skills, reads SKILL.md to detect
 * knowledge packs.
 *
 * @param filePaths - Array of generated file paths.
 * @returns Counts for each component category.
 */
export function classifyFiles(
  filePaths: readonly string[],
): ComponentCounts {
  const counts: Record<keyof ComponentCounts, number> = {
    rules: 0,
    skills: 0,
    knowledgePacks: 0,
    agents: 0,
    hooks: 0,
    settings: 0,
    readme: 0,
    github: 0,
  };

  for (const filePath of filePaths) {
    const segments = splitPathSegments(filePath);
    const category = classifySingleFile(filePath, segments);
    if (category !== undefined) {
      counts[category]++;
    }
  }

  return counts;
}

/**
 * Compute the total count from all categories.
 *
 * @param counts - Component counts.
 * @returns Sum of all category counts.
 */
function computeTotal(counts: ComponentCounts): number {
  return CATEGORY_LABELS.reduce(
    (sum, [, key]) => sum + counts[key],
    0,
  );
}

/**
 * Compute the label column width for alignment.
 *
 * @param counts - Component counts.
 * @returns Width of the widest label or header, whichever is larger.
 */
function computeLabelWidth(counts: ComponentCounts): number {
  let maxWidth = HEADER_LABEL.length;
  for (const [label, key] of CATEGORY_LABELS) {
    if (counts[key] > 0 && label.length > maxWidth) {
      maxWidth = label.length;
    }
  }
  if (TOTAL_LABEL.length > maxWidth) {
    maxWidth = TOTAL_LABEL.length;
  }
  return maxWidth;
}

/**
 * Format the summary table string for terminal output.
 *
 * Only displays categories with count > 0. Includes header,
 * separator lines using U+2500 (box-drawing horizontal), and total row.
 *
 * @param counts - Component counts to display.
 * @returns Formatted table string ready for console output.
 */
export function formatSummaryTable(
  counts: ComponentCounts,
): string {
  const total = computeTotal(counts);
  const labelWidth = computeLabelWidth(counts);
  const countWidth = HEADER_COUNT.length;
  const separator = SEPARATOR_CHAR.repeat(labelWidth);
  const countSeparator = SEPARATOR_CHAR.repeat(countWidth);

  const lines: string[] = [];
  lines.push(
    `  ${HEADER_LABEL.padEnd(labelWidth)}  ${HEADER_COUNT}`,
  );
  lines.push(`  ${separator}  ${countSeparator}`);

  for (const [label, key] of CATEGORY_LABELS) {
    if (counts[key] > 0) {
      const countStr = String(counts[key]).padStart(countWidth);
      lines.push(`  ${label.padEnd(labelWidth)}  ${countStr}`);
    }
  }

  lines.push(`  ${separator}  ${countSeparator}`);
  const totalStr = String(total).padStart(countWidth);
  lines.push(`  ${TOTAL_LABEL.padEnd(labelWidth)}  ${totalStr}`);

  return lines.join("\n");
}

/**
 * Display the full pipeline result: success line, summary table,
 * output directory, and warnings.
 *
 * @param result - The pipeline result to display.
 * @throws Error if `result.success` is false.
 */
export function displayResult(result: PipelineResult): void {
  if (!result.success) {
    throw new Error("Pipeline did not succeed");
  }

  console.log(`Pipeline: Success (${result.durationMs}ms)`);
  console.log();

  const counts = classifyFiles(result.filesGenerated);
  const table = formatSummaryTable(counts);
  console.log(table);

  console.log();
  console.log(`Output: ${result.outputDir}`);

  for (const warning of result.warnings) {
    console.log(`Warning: ${warning}`);
  }
}
