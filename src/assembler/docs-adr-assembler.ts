/**
 * DocsAdrAssembler — generates docs/adr/README.md as an ADR index.
 *
 * Also exports utility functions for ADR sequential numbering
 * and filename formatting, used by downstream stories.
 *
 * @module
 */
import * as fs from "node:fs";
import * as path from "node:path";
import type { ProjectConfig } from "../models.js";
import type { TemplateEngine } from "../template-engine.js";

const TEMPLATE_FILENAME = "_TEMPLATE-ADR.md";
const TEMPLATES_SUBDIR = "templates";
const ADR_OUTPUT_SUBDIR = "docs/adr";
const README_FILENAME = "README.md";
const ADR_FILE_PATTERN = /^ADR-(\d{4,})-.*\.md$/;
const ADR_NUMBER_PAD_WIDTH = 4;
const ADR_TITLE_HEADING = "# Architecture Decision Records";

const MANDATORY_SECTIONS = [
  "## Status",
  "## Context",
  "## Decision",
  "## Consequences",
] as const;

/** Build the README.md content for the ADR index. */
function buildReadmeContent(
  projectName: string,
): string {
  const lines = [
    ADR_TITLE_HEADING,
    "",
    `> Architecture Decision Records for **${projectName}**.`,
    "",
    "| ID | Title | Status | Date |",
    "|----|-------|--------|------|",
    "",
    "## Creating a New ADR",
    "",
    "Copy `_TEMPLATE-ADR.md` and follow the naming convention:",
    "`ADR-NNNN-title-in-kebab-case.md`",
    "",
  ];
  return lines.join("\n");
}

/** Check that the ADR template contains all mandatory sections. */
function hasAllMandatorySections(templateContent: string): boolean {
  return MANDATORY_SECTIONS.every(
    (section) => templateContent.includes(section),
  );
}

/** Generates docs/adr/README.md at the project root. */
export class DocsAdrAssembler {
  assemble(
    config: ProjectConfig,
    outputDir: string,
    resourcesDir: string,
    _engine: TemplateEngine,
  ): string[] {
    const templatePath = path.join(
      resourcesDir, TEMPLATES_SUBDIR, TEMPLATE_FILENAME,
    );
    if (!fs.existsSync(templatePath)) {
      return [];
    }
    const templateContent = fs.readFileSync(templatePath, "utf-8");
    if (!hasAllMandatorySections(templateContent)) {
      return [];
    }
    const adrDir = path.join(outputDir, ADR_OUTPUT_SUBDIR);
    fs.mkdirSync(adrDir, { recursive: true });
    const readmeContent = buildReadmeContent(config.project.name);
    const readmeDest = path.join(adrDir, README_FILENAME);
    fs.writeFileSync(readmeDest, readmeContent, "utf-8");
    const templateDest = path.join(adrDir, TEMPLATE_FILENAME);
    fs.writeFileSync(templateDest, templateContent, "utf-8");
    return [readmeDest, templateDest];
  }
}

/**
 * Scan existing ADR files and return the next sequential number.
 *
 * @param adrDir - Absolute path to the ADR directory.
 * @returns The next ADR number (1 if directory is empty or missing).
 */
export function getNextAdrNumber(adrDir: string): number {
  if (!fs.existsSync(adrDir)) {
    return 1;
  }
  const files = fs.readdirSync(adrDir);
  const numbers = files
    .map((f) => ADR_FILE_PATTERN.exec(f))
    .filter((m): m is RegExpExecArray => m !== null)
    .map((m) => parseInt(m[1]!, 10));
  if (numbers.length === 0) {
    return 1;
  }
  return Math.max(...numbers) + 1;
}

/**
 * Format an ADR filename from a number and title.
 *
 * @param num - The ADR sequential number.
 * @param title - The ADR title in plain text.
 * @returns Formatted filename like `ADR-0001-title-in-kebab-case.md`.
 */
export function formatAdrFilename(
  num: number,
  title: string,
): string {
  const padded = String(num).padStart(ADR_NUMBER_PAD_WIDTH, "0");
  const sanitized = title
    .toLowerCase()
    .replace(/[^a-z0-9-]+/g, "-")
    .replace(/-{2,}/g, "-")
    .replace(/^-|-$/g, "");
  const slug = sanitized === "" ? "untitled" : sanitized;
  return `ADR-${padded}-${slug}.md`;
}
