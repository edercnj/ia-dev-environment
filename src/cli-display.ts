/**
 * CLI display utilities — file classification, summary table formatting,
 * and pipeline result display.
 *
 * Migrated from Python `__main__.py` display functions.
 *
 * @module
 */
import type { PipelineResult } from "./models.js";
import { isKnowledgePackFile } from "./cli-kp-detect.js";

export { isKnowledgePackFile } from "./cli-kp-detect.js";

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
  readonly codex: number;
}

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
  ["Codex", "codex"],
];

/** Split a file path into its directory and filename segments. */
function splitPathSegments(filePath: string): readonly string[] {
  return filePath.split(/[\\/]/);
}

/**
 * Classify a single file path into a component category.
 *
 * @param filePath - Path to classify.
 * @param segments - Pre-split path segments.
 * @param kpCache - Cache for knowledge pack detection.
 * @returns The matching category key, or undefined if no match.
 */
function classifySingleFile(
  filePath: string,
  segments: readonly string[],
  kpCache: Map<string, boolean>,
): keyof ComponentCounts | undefined {
  const fileName = segments[segments.length - 1] ?? "";
  if (segments.includes(".agents")) return "codex";
  if (segments.includes(".codex")) return "codex";
  if (segments.includes(".github")) return "github";
  if (fileName.includes("README")) return "readme";
  if (fileName.includes("settings")) return "settings";
  if (segments.includes("hooks")) return "hooks";
  if (segments.includes("agents")) return "agents";
  if (segments.includes("skills")) {
    // In dry-run mode, files may not exist on disk (temp dir deleted).
    // isKnowledgePackFile returns false for missing files — matches Python behavior.
    return isKnowledgePackFile(filePath, kpCache)
      ? "knowledgePacks"
      : "skills";
  }
  if (segments.includes("rules")) return "rules";
  if (fileName === "AGENTS.md") return "codex";
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
    codex: 0,
  };

  const kpCache = new Map<string, boolean>();
  for (const filePath of filePaths) {
    const segments = splitPathSegments(filePath);
    const category = classifySingleFile(filePath, segments, kpCache);
    if (category !== undefined) {
      counts[category]++;
    }
  }

  return counts;
}

/** Compute the total count from all categories. */
function computeTotal(counts: ComponentCounts): number {
  return CATEGORY_LABELS.reduce(
    (sum, [, key]) => sum + counts[key],
    0,
  );
}

/** Compute the label column width for alignment. */
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

/** Build the data rows (non-zero categories) for the summary table. */
function buildDataRows(
  counts: ComponentCounts,
  labelWidth: number,
  countWidth: number,
): string[] {
  const rows: string[] = [];
  for (const [label, key] of CATEGORY_LABELS) {
    if (counts[key] > 0) {
      const countStr = String(counts[key]).padStart(countWidth);
      rows.push(`  ${label.padEnd(labelWidth)}  ${countStr}`);
    }
  }
  return rows;
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
  const sep = SEPARATOR_CHAR.repeat(labelWidth);
  const cSep = SEPARATOR_CHAR.repeat(countWidth);
  const header = `  ${HEADER_LABEL.padEnd(labelWidth)}  ${HEADER_COUNT}`;
  const divider = `  ${sep}  ${cSep}`;
  const dataRows = buildDataRows(counts, labelWidth, countWidth);
  const totalRow = `  ${TOTAL_LABEL.padEnd(labelWidth)}  ${String(total).padStart(countWidth)}`;

  return [header, divider, ...dataRows, divider, totalRow].join("\n");
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
