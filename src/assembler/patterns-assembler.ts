/**
 * PatternsAssembler — selects and consolidates pattern knowledge packs.
 *
 * Migrated from Python `assembler/patterns_assembler.py` (117 lines).
 * Selection logic lives in {@link ../domain/pattern-mapping.ts}.
 *
 * @module
 */
import * as fs from "node:fs";
import * as path from "node:path";
import type { ProjectConfig } from "../models.js";
import type { TemplateEngine } from "../template-engine.js";
import {
  selectPatterns,
  selectPatternFiles,
} from "../domain/pattern-mapping.js";

const SKILLS_DIR = "skills";
const PATTERNS_SKILL_DIR = "patterns";
const REFERENCES_DIR = "references";
const CONSOLIDATED_FILENAME = "SKILL.md";
const SECTION_SEPARATOR = "\n\n---\n\n";

/** Assembles pattern knowledge packs from source templates. */
export class PatternsAssembler {
  /** Orchestrate pattern selection, rendering, and consolidation. */
  assemble(
    config: ProjectConfig,
    outputDir: string,
    resourcesDir: string,
    engine: TemplateEngine,
  ): string[] {
    const categories = selectPatterns(config);
    if (categories.length === 0) return [];
    const patternFiles = selectPatternFiles(resourcesDir, categories);
    if (patternFiles.length === 0) return [];
    return this.generateOutput(patternFiles, outputDir, engine);
  }

  private generateOutput(
    patternFiles: readonly string[],
    outputDir: string,
    engine: TemplateEngine,
  ): string[] {
    const rendered = this.renderContents(patternFiles, engine);
    const refsDir = this.buildRefsDir(outputDir);
    const results = this.flushPatterns(patternFiles, rendered, refsDir);
    const consolidated = this.buildConsolidatedPath(outputDir);
    results.push(this.flushConsolidated(rendered, consolidated));
    return results;
  }

  private renderContents(
    patternFiles: readonly string[],
    engine: TemplateEngine,
  ): string[] {
    return patternFiles.map((srcFile) => {
      const content = fs.readFileSync(srcFile, "utf-8");
      return engine.replacePlaceholders(content);
    });
  }

  private buildRefsDir(outputDir: string): string {
    return path.join(
      outputDir, SKILLS_DIR, PATTERNS_SKILL_DIR, REFERENCES_DIR,
    );
  }

  private buildConsolidatedPath(outputDir: string): string {
    return path.join(
      outputDir, SKILLS_DIR, PATTERNS_SKILL_DIR, CONSOLIDATED_FILENAME,
    );
  }

  private flushPatterns(
    patternFiles: readonly string[],
    rendered: readonly string[],
    destDir: string,
  ): string[] {
    const results: string[] = [];
    for (let i = 0; i < patternFiles.length; i++) {
      const srcFile = patternFiles[i]!;
      const content = rendered[i]!;
      const category = path.basename(path.dirname(srcFile));
      const targetDir = path.join(destDir, category);
      fs.mkdirSync(targetDir, { recursive: true });
      const destFile = path.join(targetDir, path.basename(srcFile));
      fs.writeFileSync(destFile, content, "utf-8");
      results.push(destFile);
    }
    return results;
  }

  private flushConsolidated(
    rendered: readonly string[],
    destPath: string,
  ): string {
    fs.mkdirSync(path.dirname(destPath), { recursive: true });
    const merged = rendered.join(SECTION_SEPARATOR);
    fs.writeFileSync(destPath, merged, "utf-8");
    return destPath;
  }
}
