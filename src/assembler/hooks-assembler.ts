/**
 * HooksAssembler — copies post-compile hook scripts for compiled languages.
 *
 * Migrated from Python `assembler/hooks_assembler.py` (48 lines).
 * Template key resolution lives in {@link ../domain/stack-mapping.ts}.
 *
 * @remarks
 * Hook scripts are copied verbatim — no template placeholder replacement.
 * The `engine` parameter is accepted for API uniformity but is not used.
 *
 * @module
 */
import * as fs from "node:fs";
import * as path from "node:path";
import type { ProjectConfig } from "../models.js";
import type { TemplateEngine } from "../template-engine.js";
import { getHookTemplateKey } from "../domain/stack-mapping.js";

const HOOKS_DIR = "hooks";
const HOOK_FILENAME = "post-compile-check.sh";
const HOOKS_TEMPLATES_DIR = "hooks-templates";
const EXECUTE_BITS = 0o111;

/** Assembles post-compile hook scripts for compiled languages. */
export class HooksAssembler {
  /** Copy hook scripts for compiled languages to the output directory. */
  assemble(
    config: ProjectConfig,
    outputDir: string,
    resourcesDir: string,
    _engine: TemplateEngine,
  ): string[] {
    const key = getHookTemplateKey(
      config.language.name,
      config.framework.buildTool,
    );
    if (key === "") return [];
    const hookSrc = path.join(
      resourcesDir, HOOKS_TEMPLATES_DIR, key, HOOK_FILENAME,
    );
    if (!fs.existsSync(hookSrc)) return [];
    return this.copyHook(hookSrc, outputDir);
  }

  private copyHook(hookSrc: string, outputDir: string): string[] {
    const hooksDir = path.join(outputDir, HOOKS_DIR);
    fs.mkdirSync(hooksDir, { recursive: true });
    const dest = path.join(hooksDir, HOOK_FILENAME);
    fs.copyFileSync(hookSrc, dest);
    const currentMode = fs.statSync(dest).mode;
    fs.chmodSync(dest, currentMode | EXECUTE_BITS);
    return [dest];
  }
}
