/**
 * GithubHooksAssembler -- copies hook JSON templates to github/hooks/.
 *
 * Migrated from Python `assembler/github_hooks_assembler.py` (57 lines).
 * Files are copied verbatim -- no template placeholder replacement.
 * The `engine` parameter is accepted for API uniformity but is not used.
 *
 * @module
 */
import * as fs from "node:fs";
import * as path from "node:path";
import type { ProjectConfig } from "../models.js";
import type { TemplateEngine } from "../template-engine.js";

const TEMPLATES_DIR_NAME = "github-hooks-templates";

/** The 3 GitHub Copilot hook template filenames. */
export const GITHUB_HOOK_TEMPLATES: readonly string[] = [
  "post-compile-check.json",
  "pre-commit-lint.json",
  "session-context-loader.json",
] as const;

/** Assembles github/hooks/ JSON files by copying templates verbatim. */
export class GithubHooksAssembler {
  /** Copy hook JSON templates to the output directory. */
  assemble(
    _config: ProjectConfig,
    outputDir: string,
    resourcesDir: string,
    _engine: TemplateEngine,
  ): string[] {
    const srcDir = path.join(resourcesDir, TEMPLATES_DIR_NAME);
    if (!fs.existsSync(srcDir)) return [];
    const hooksDir = path.join(outputDir, "github", "hooks");
    fs.mkdirSync(hooksDir, { recursive: true });
    const results: string[] = [];
    for (const template of GITHUB_HOOK_TEMPLATES) {
      const src = path.join(srcDir, template);
      if (!fs.existsSync(src)) continue;
      const dest = path.join(hooksDir, template);
      fs.copyFileSync(src, dest);
      results.push(dest);
    }
    return results;
  }
}
