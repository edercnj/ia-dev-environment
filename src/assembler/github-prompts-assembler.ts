/**
 * GithubPromptsAssembler -- renders Nunjucks prompt templates to github/prompts/.
 *
 * Migrated from Python `assembler/github_prompts_assembler.py` (61 lines).
 * This is the only GitHub assembler that uses full Nunjucks rendering
 * via {@link TemplateEngine.renderTemplate} (not just placeholder replacement).
 *
 * @module
 */
import * as fs from "node:fs";
import * as path from "node:path";
import type { ProjectConfig } from "../models.js";
import type { TemplateEngine } from "../template-engine.js";

const TEMPLATES_DIR_NAME = "github-prompts-templates";

/** The 4 Nunjucks prompt template filenames. */
export const GITHUB_PROMPT_TEMPLATES: readonly string[] = [
  "new-feature.prompt.md.j2",
  "decompose-spec.prompt.md.j2",
  "code-review.prompt.md.j2",
  "troubleshoot.prompt.md.j2",
] as const;

/** Generates github/prompts/*.prompt.md from Nunjucks templates. */
export class GithubPromptsAssembler {
  /** Render prompt templates and write to output. */
  assemble(
    _config: ProjectConfig,
    outputDir: string,
    resourcesDir: string,
    engine: TemplateEngine,
  ): string[] {
    const srcDir = path.join(resourcesDir, TEMPLATES_DIR_NAME);
    if (!fs.existsSync(srcDir)) return [];
    const promptsDir = path.join(outputDir, "prompts");
    fs.mkdirSync(promptsDir, { recursive: true });
    const results: string[] = [];
    for (const templateName of GITHUB_PROMPT_TEMPLATES) {
      const src = path.join(srcDir, templateName);
      if (!fs.existsSync(src)) continue;
      const outputName = templateName.replace(/\.j2$/, "");
      const content = engine.renderTemplate(
        `${TEMPLATES_DIR_NAME}/${templateName}`,
      );
      const dest = path.join(promptsDir, outputName);
      fs.writeFileSync(dest, content, "utf-8");
      results.push(dest);
    }
    return results;
  }
}
