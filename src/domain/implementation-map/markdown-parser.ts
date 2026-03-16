/**
 * Markdown table parser for IMPLEMENTATION-MAP.md.
 *
 * Extracts the dependency matrix (Section 1) and phase summary
 * (Section 5) from markdown content using line-by-line parsing.
 */
import type { DependencyMatrixRow, PhaseSummaryRow } from "./types.js";

const SECTION_1_PATTERN = /^##\s+1\.\s/;
const SECTION_5_PATTERN = /^##\s+5\.\s/;
const NEXT_SECTION_PATTERN = /^##\s+\d+\.\s/;
const SEPARATOR_ROW_PATTERN = /^\|\s*:?-+/;
const TABLE_ROW_PATTERN = /^\|/;
const EMPTY_CELL_MARKERS = ["-", "\u2014", ""];
const DEPENDENCY_MATRIX_CELL_COUNT = 5;
const PHASE_SUMMARY_CELL_COUNT = 5;

/** Split a pipe-delimited table row into trimmed cells. */
function splitTableRow(line: string): string[] {
  return line
    .split("|")
    .map((cell) => cell.trim())
    .filter((_, index, arr) => index > 0 && index < arr.length - 1);
}

/** Get a cell value from an array, returning empty string if absent. */
function cellAt(cells: string[], index: number): string {
  return cells[index] ?? "";
}

/** Parse a comma-separated list of story IDs. */
function parseStoryList(cell: string): string[] {
  const trimmed = cell.trim();
  if (EMPTY_CELL_MARKERS.includes(trimmed)) return [];
  return trimmed.split(",").map((id) => id.trim()).filter(Boolean);
}

/** Extract lines belonging to a numbered section. */
function extractSectionLines(
  lines: readonly string[],
  sectionPattern: RegExp,
): string[] {
  let inSection = false;
  const sectionLines: string[] = [];

  for (const line of lines) {
    if (sectionPattern.test(line)) {
      inSection = true;
      continue;
    }
    if (inSection && NEXT_SECTION_PATTERN.test(line)) {
      break;
    }
    if (inSection) {
      sectionLines.push(line);
    }
  }
  return sectionLines;
}

/** Filter table data rows (exclude headers and separator rows). */
function filterDataRows(lines: readonly string[]): string[] {
  let headerFound = false;
  const dataRows: string[] = [];

  for (const line of lines) {
    if (!TABLE_ROW_PATTERN.test(line)) continue;
    if (!headerFound) {
      headerFound = true;
      continue;
    }
    if (SEPARATOR_ROW_PATTERN.test(line)) continue;
    dataRows.push(line);
  }
  return dataRows;
}

/** Parse a single dependency matrix row into a typed object. */
function parseDependencyRow(line: string): DependencyMatrixRow | undefined {
  const cells = splitTableRow(line);
  if (cells.length < DEPENDENCY_MATRIX_CELL_COUNT) return undefined;

  return {
    storyId: cellAt(cells, 0).trim(),
    title: cellAt(cells, 1).trim(),
    blockedBy: parseStoryList(cellAt(cells, 2)),
    blocks: parseStoryList(cellAt(cells, 3)),
    status: cellAt(cells, 4).trim(),
  };
}

/** Extract the dependency matrix from Section 1 of the map. */
export function extractDependencyMatrix(
  content: string,
): DependencyMatrixRow[] {
  if (!content.trim()) return [];

  const lines = content.split("\n");
  const sectionLines = extractSectionLines(lines, SECTION_1_PATTERN);
  const dataRows = filterDataRows(sectionLines);

  return dataRows
    .map(parseDependencyRow)
    .filter((row): row is DependencyMatrixRow => row !== undefined);
}

/** Parse a single phase summary row into a typed object. */
function parsePhaseSummaryRow(line: string): PhaseSummaryRow | undefined {
  const cells = splitTableRow(line);
  if (cells.length < PHASE_SUMMARY_CELL_COUNT) return undefined;

  const phaseStr = cellAt(cells, 0).trim();
  const phase = parseInt(phaseStr, 10);
  if (Number.isNaN(phase)) return undefined;

  const storiesRaw = cellAt(cells, 1).trim();
  const stories = storiesRaw
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);

  return {
    phase,
    stories,
    layer: cellAt(cells, 2).trim(),
    parallelism: cellAt(cells, 3).trim(),
    prerequisite: cellAt(cells, 4).trim(),
  };
}

/** Extract the phase summary from Section 5 of the map. */
export function extractPhaseSummary(
  content: string,
): PhaseSummaryRow[] {
  if (!content.trim()) return [];

  const lines = content.split("\n");
  const sectionLines = extractSectionLines(lines, SECTION_5_PATTERN);
  const dataRows = filterDataRows(sectionLines);

  return dataRows
    .map(parsePhaseSummaryRow)
    .filter((row): row is PhaseSummaryRow => row !== undefined);
}
