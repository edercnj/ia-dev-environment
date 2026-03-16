/**
 * RunbookAssembler — renders deploy runbook template to docs/runbook/.
 *
 * Generates `docs/runbook/deploy-runbook.md` from the Nunjucks template
 * `_TEMPLATE-DEPLOY-RUNBOOK.md` with conditional sections for Docker,
 * Kubernetes, and database migration.
 *
 * @module
 */
import * as fs from "node:fs";
import * as path from "node:path";
import type { ProjectConfig } from "../models.js";
import type { TemplateEngine } from "../template-engine.js";

const TEMPLATE_RELATIVE_PATH = "templates/_TEMPLATE-DEPLOY-RUNBOOK.md";
const OUTPUT_SUBDIR = "docs/runbook";
const OUTPUT_FILENAME = "deploy-runbook.md";

/** Generates docs/runbook/deploy-runbook.md from Nunjucks template. */
export class RunbookAssembler {
  /** Render deploy runbook template and write to output. */
  assemble(
    _config: ProjectConfig,
    outputDir: string,
    resourcesDir: string,
    engine: TemplateEngine,
  ): string[] {
    const templatePath = path.join(
      resourcesDir, TEMPLATE_RELATIVE_PATH,
    );
    if (!fs.existsSync(templatePath)) return [];
    const runbookDir = path.join(outputDir, OUTPUT_SUBDIR);
    fs.mkdirSync(runbookDir, { recursive: true });
    const content = engine.renderTemplate(TEMPLATE_RELATIVE_PATH);
    const dest = path.join(runbookDir, OUTPUT_FILENAME);
    fs.writeFileSync(dest, content, "utf-8");
    return [dest];
  }
}
