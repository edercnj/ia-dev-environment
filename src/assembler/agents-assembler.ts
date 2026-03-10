/**
 * AgentsAssembler — assembles .claude/agents/ from templates based on project config.
 *
 * Migrated from Python `assembler/agents.py` (263 lines).
 * Selection logic lives in {@link ./agents-selection.ts}; this module handles file I/O.
 *
 * @module
 */
import * as fs from "node:fs";
import * as path from "node:path";
import type { ProjectConfig } from "../models.js";
import { TemplateEngine } from "../template-engine.js";
import {
  copyTemplateFile,
  copyTemplateFileIfExists,
} from "./copy-helpers.js";
import type { AssembleResult } from "./rules-assembler.js";
import {
  selectConditionalAgents,
  buildChecklistRules,
  checklistMarker,
} from "./agents-selection.js";

const AGENTS_TEMPLATES_DIR = "agents-templates";
const CORE_DIR = "core";
const CONDITIONAL_DIR = "conditional";
const DEVELOPERS_DIR = "developers";
const CHECKLISTS_DIR = "checklists";
const AGENTS_OUTPUT = "agents";
const MD_EXTENSION = ".md";

/** Assembles agent files from templates based on project config. */
export class AgentsAssembler {
  /** Scan core agents directory for .md files, sorted alphabetically. */
  selectCoreAgents(resourcesDir: string): string[] {
    const corePath = path.join(
      resourcesDir, AGENTS_TEMPLATES_DIR, CORE_DIR,
    );
    if (!fs.existsSync(corePath)) return [];
    return fs
      .readdirSync(corePath, { withFileTypes: true })
      .filter((e) => e.isFile() && e.name.endsWith(MD_EXTENSION))
      .map((e) => e.name)
      .sort();
  }

  /** Evaluate feature gates and return conditional agent filenames. */
  selectConditionalAgents(config: ProjectConfig): string[] {
    return selectConditionalAgents(config);
  }

  /** Return developer agent filename for the project language. */
  selectDeveloperAgent(config: ProjectConfig): string {
    const safeName = path.basename(config.language.name);
    return `${safeName}-developer${MD_EXTENSION}`;
  }

  /** Main entry point: assemble all agents. */
  assemble(
    config: ProjectConfig,
    outputDir: string,
    resourcesDir: string,
    engine: TemplateEngine,
  ): AssembleResult {
    const files: string[] = [];
    files.push(
      ...this.assembleCore(resourcesDir, outputDir, engine),
    );
    files.push(
      ...this.assembleConditional(config, resourcesDir, outputDir, engine),
    );
    const dev = this.copyDeveloperAgent(
      config, resourcesDir, outputDir, engine,
    );
    if (dev !== null) {
      files.push(dev);
    }
    this.injectChecklists(config, outputDir, resourcesDir);
    return { files, warnings: [] };
  }

  private copyCoreAgent(
    agentFile: string, resourcesDir: string,
    outputDir: string, engine: TemplateEngine,
  ): string {
    const src = path.join(
      resourcesDir, AGENTS_TEMPLATES_DIR, CORE_DIR, agentFile,
    );
    const dest = path.join(outputDir, AGENTS_OUTPUT, agentFile);
    return copyTemplateFile(src, dest, engine);
  }

  private copyConditionalAgent(
    agentFile: string, resourcesDir: string,
    outputDir: string, engine: TemplateEngine,
  ): string | null {
    const src = path.join(
      resourcesDir, AGENTS_TEMPLATES_DIR, CONDITIONAL_DIR, agentFile,
    );
    const dest = path.join(outputDir, AGENTS_OUTPUT, agentFile);
    return copyTemplateFileIfExists(src, dest, engine);
  }

  private copyDeveloperAgent(
    config: ProjectConfig, resourcesDir: string,
    outputDir: string, engine: TemplateEngine,
  ): string | null {
    const agentFile = this.selectDeveloperAgent(config);
    const src = path.join(
      resourcesDir, AGENTS_TEMPLATES_DIR, DEVELOPERS_DIR, agentFile,
    );
    const dest = path.join(outputDir, AGENTS_OUTPUT, agentFile);
    return copyTemplateFileIfExists(src, dest, engine);
  }

  private injectChecklists(
    config: ProjectConfig, outputDir: string, resourcesDir: string,
  ): void {
    for (const rule of buildChecklistRules(config)) {
      if (!rule.active) continue;
      this.injectSingleChecklist(
        rule.agent, rule.checklist, outputDir, resourcesDir,
      );
    }
  }

  private injectSingleChecklist(
    agentFile: string, checklistFile: string,
    outputDir: string, resourcesDir: string,
  ): void {
    const agentPath = path.join(outputDir, AGENTS_OUTPUT, agentFile);
    if (!fs.existsSync(agentPath)) return;
    const checklistSrc = path.join(
      resourcesDir, AGENTS_TEMPLATES_DIR, CHECKLISTS_DIR, checklistFile,
    );
    if (!fs.existsSync(checklistSrc)) return;
    const marker = checklistMarker(checklistFile);
    const section = fs.readFileSync(checklistSrc, "utf-8");
    const base = fs.readFileSync(agentPath, "utf-8");
    const result = TemplateEngine.injectSection(base, section, marker);
    fs.writeFileSync(agentPath, result, "utf-8");
  }

  private assembleCore(
    resourcesDir: string, outputDir: string, engine: TemplateEngine,
  ): string[] {
    return this.selectCoreAgents(resourcesDir).map(
      (agent) => this.copyCoreAgent(agent, resourcesDir, outputDir, engine),
    );
  }

  private assembleConditional(
    config: ProjectConfig, resourcesDir: string,
    outputDir: string, engine: TemplateEngine,
  ): string[] {
    const results: string[] = [];
    for (const agent of this.selectConditionalAgents(config)) {
      const copied = this.copyConditionalAgent(
        agent, resourcesDir, outputDir, engine,
      );
      if (copied !== null) results.push(copied);
    }
    return results;
  }
}
