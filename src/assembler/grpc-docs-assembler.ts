/**
 * GrpcDocsAssembler — generates docs/api/grpc-reference.md
 * from the _TEMPLATE-GRPC-REFERENCE.md Nunjucks template.
 *
 * Interface-aware: only generates when the project config
 * includes a `grpc` interface. Returns empty array otherwise.
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
import { hasInterface } from "./conditions.js";

const TEMPLATE_PATH = "templates/_TEMPLATE-GRPC-REFERENCE.md";
const OUTPUT_SUBDIR = "api";
const OUTPUT_FILENAME = "grpc-reference.md";

/** Assembles docs/api/grpc-reference.md for gRPC-enabled projects. */
export class GrpcDocsAssembler {
  /** Generate gRPC API reference documentation. */
  assemble(
    config: ProjectConfig,
    outputDir: string,
    resourcesDir: string,
    engine: TemplateEngine,
  ): string[] {
    if (!hasInterface(config, "grpc")) {
      return [];
    }
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
