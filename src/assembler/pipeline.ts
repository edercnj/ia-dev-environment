import type { ProjectConfig } from "../models.js";
import type { TemplateEngine } from "../template-engine.js";
import type { AssembleResult } from "./rules-assembler.js";

/** Warning message appended to dry-run results. */
export const DRY_RUN_WARNING = "Dry run -- no files written";

/**
 * Named assembler entry: associates a display name with its assembler
 * instance and a uniform `assemble()` signature.
 */
export interface AssemblerDescriptor {
  readonly name: string;
  readonly assembler: {
    assemble(
      config: ProjectConfig,
      outputDir: string,
      resourcesDir: string,
      engine: TemplateEngine,
    ): string[] | AssembleResult;
  };
}

/** Internal normalized result from a single assembler call. */
export interface NormalizedResult {
  readonly files: string[];
  readonly warnings: string[];
}

/**
 * Normalize an assembler return value into a uniform shape.
 *
 * If the result is a plain `string[]`, treats it as files with no warnings.
 * If it is an `AssembleResult` (object with `files` and `warnings`),
 * extracts both arrays.
 */
export function normalizeResult(
  result: string[] | AssembleResult,
): NormalizedResult {
  if (Array.isArray(result)) {
    return { files: result, warnings: [] };
  }
  return { files: [...result.files], warnings: [...result.warnings] };
}
