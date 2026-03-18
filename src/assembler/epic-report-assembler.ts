/**
 * EpicReportAssembler — copies the epic execution report template
 * to two output locations for runtime resolution.
 *
 * The template contains {{PLACEHOLDER}} tokens intended for
 * runtime resolution by the consolidation subagent (story-0005-0011),
 * NOT for build-time rendering. Content is copied verbatim.
 *
 * @module
 */
import * as fs from "node:fs";
import * as path from "node:path";
import type { ProjectConfig } from "../models.js";
import type { TemplateEngine } from "../template-engine.js";

const TEMPLATE_FILENAME = "_TEMPLATE-EPIC-EXECUTION-REPORT.md";
const TEMPLATES_SUBDIR = "templates";
const CLAUDE_OUTPUT_SUBDIR = path.join(".claude", "templates");
const GITHUB_OUTPUT_SUBDIR = path.join(".github", "templates");

const MANDATORY_SECTIONS = [
  "## Sumario Executivo",
  "## Timeline de Execucao",
  "## Status Final por Story",
  "## Findings Consolidados",
  "## Coverage Delta",
  "## Commits e SHAs",
  "## Issues Nao Resolvidos",
  "## PR Link",
] as const;

function hasAllMandatorySections(content: string): boolean {
  return MANDATORY_SECTIONS.every(
    (section) => content.includes(section),
  );
}

/**
 * Copies _TEMPLATE-EPIC-EXECUTION-REPORT.md verbatim to
 * .claude/templates/ and .github/templates/.
 */
export class EpicReportAssembler {
  assemble(
    _config: ProjectConfig,
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
    const content = fs.readFileSync(templatePath, "utf-8");
    if (!hasAllMandatorySections(content)) {
      return [];
    }
    const outputs = [
      CLAUDE_OUTPUT_SUBDIR,
      GITHUB_OUTPUT_SUBDIR,
    ];
    const results: string[] = [];
    for (const subdir of outputs) {
      const destDir = path.join(outputDir, subdir);
      fs.mkdirSync(destDir, { recursive: true });
      const destPath = path.join(destDir, TEMPLATE_FILENAME);
      fs.writeFileSync(destPath, content, "utf-8");
      results.push(destPath);
    }
    return results;
  }
}
