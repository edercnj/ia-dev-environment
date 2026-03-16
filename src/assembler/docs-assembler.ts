/**
 * DocsAssembler — generates docs/architecture/service-architecture.md
 * from the _TEMPLATE-SERVICE-ARCHITECTURE.md Nunjucks template.
 *
 * Graceful no-op: if the source template does not exist in
 * resourcesDir, returns an empty array (backward compatibility).
 *
 * @module
 */
import * as fs from "node:fs";
import * as path from "node:path";
import type { ProjectConfig } from "../models.js";
import type { TemplateEngine } from "../template-engine.js";

const TEMPLATE_PATH = "templates/_TEMPLATE-SERVICE-ARCHITECTURE.md";
const OUTPUT_SUBDIR = "architecture";
const OUTPUT_FILENAME = "service-architecture.md";

/** Assembles docs/architecture/service-architecture.md. */
export class DocsAssembler {
  /** Generate service architecture documentation. */
  assemble(
    config: ProjectConfig,
    outputDir: string,
    resourcesDir: string,
    engine: TemplateEngine,
  ): string[] {
    const templateFile = path.join(resourcesDir, TEMPLATE_PATH);
    if (!fs.existsSync(templateFile)) {
      return [];
    }
    const rendered = engine.renderTemplate(TEMPLATE_PATH);
    const destDir = path.join(outputDir, OUTPUT_SUBDIR);
    fs.mkdirSync(destDir, { recursive: true });
    const destFile = path.join(destDir, OUTPUT_FILENAME);
    fs.writeFileSync(destFile, rendered, "utf-8");
    return [destFile];
  }
}
