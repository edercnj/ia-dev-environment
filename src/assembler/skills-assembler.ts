/**
 * SkillsAssembler — assembles .claude/skills/ from templates based on project config.
 *
 * Migrated from Python `assembler/skills.py` (285 lines).
 * Selects core, conditional, and knowledge-pack skills based on feature gates.
 *
 * @module
 */
import * as fs from "node:fs";
import * as path from "node:path";
import type { ProjectConfig } from "../models.js";
import type { TemplateEngine } from "../template-engine.js";
import { hasInterface, hasAnyInterface } from "./conditions.js";
import {
  copyTemplateTree,
  copyTemplateTreeIfExists,
  copyTemplateFile,
} from "./copy-helpers.js";
import { CORE_KNOWLEDGE_PACKS, buildInfraPackRules } from "../domain/skill-registry.js";
import { getStackPackName } from "../domain/stack-pack-mapping.js";

const SKILLS_TEMPLATES_DIR = "skills-templates";
const CORE_DIR = "core";
const CONDITIONAL_DIR = "conditional";
const KNOWLEDGE_PACKS_DIR = "knowledge-packs";
const INFRA_PATTERNS_DIR = "infra-patterns";
const STACK_PATTERNS_DIR = "stack-patterns";
const LIB_DIR = "lib";
const SKILL_MD = "SKILL.md";
const SKILLS_OUTPUT = "skills";

/** Assembles skills from templates based on project config. */
export class SkillsAssembler {
  /** Scan core skills directories, returning skill names. */
  selectCoreSkills(resourcesDir: string): string[] {
    const corePath = path.join(
      resourcesDir, SKILLS_TEMPLATES_DIR, CORE_DIR,
    );
    if (!fs.existsSync(corePath)) return [];
    const skills: string[] = [];
    const entries = fs.readdirSync(corePath, { withFileTypes: true });
    for (const entry of entries.sort((a, b) => a.name.localeCompare(b.name))) {
      if (!entry.isDirectory()) continue;
      if (entry.name === LIB_DIR) {
        const libPath = path.join(corePath, LIB_DIR);
        const subs = fs.readdirSync(libPath, { withFileTypes: true });
        for (const sub of subs.sort((a, b) => a.name.localeCompare(b.name))) {
          if (sub.isDirectory()) {
            skills.push(`${LIB_DIR}/${sub.name}`);
          }
        }
      } else {
        skills.push(entry.name);
      }
    }
    return skills;
  }

  /** Evaluate feature gates and return conditional skill names. */
  selectConditionalSkills(config: ProjectConfig): string[] {
    return [
      ...this.selectInterfaceSkills(config),
      ...this.selectInfraSkills(config),
      ...this.selectTestingSkills(config),
      ...this.selectSecuritySkills(config),
    ];
  }

  /** Select knowledge packs based on config. */
  selectKnowledgePacks(config: ProjectConfig): string[] {
    const packs = [...CORE_KNOWLEDGE_PACKS];
    packs.push("layer-templates");
    packs.push(...this.selectDataPacks(config));
    return packs;
  }

  /** Main entry point: assemble all skills. */
  assemble(
    config: ProjectConfig,
    outputDir: string,
    resourcesDir: string,
    engine: TemplateEngine,
  ): string[] {
    const results: string[] = [];
    results.push(...this.assembleCore(resourcesDir, outputDir, engine));
    results.push(
      ...this.assembleConditional(config, resourcesDir, outputDir, engine),
    );
    results.push(
      ...this.assembleKnowledge(config, resourcesDir, outputDir, engine),
    );
    return results;
  }

  private selectInterfaceSkills(config: ProjectConfig): string[] {
    const skills: string[] = [];
    if (hasInterface(config, "rest")) skills.push("x-review-api");
    if (hasInterface(config, "grpc")) skills.push("x-review-grpc");
    if (hasInterface(config, "graphql")) skills.push("x-review-graphql");
    if (hasAnyInterface(config, "event-consumer", "event-producer")) {
      skills.push("x-review-events");
    }
    return skills;
  }

  private selectInfraSkills(config: ProjectConfig): string[] {
    const skills: string[] = [];
    if (config.infrastructure.observability.tool !== "none") {
      skills.push("instrument-otel");
    }
    if (config.infrastructure.orchestrator !== "none") {
      skills.push("setup-environment");
    }
    if (config.infrastructure.apiGateway !== "none") {
      skills.push("x-review-gateway");
    }
    return skills;
  }

  private selectTestingSkills(config: ProjectConfig): string[] {
    const skills: string[] = [];
    if (config.testing.smokeTests && hasInterface(config, "rest")) {
      skills.push("run-smoke-api");
    }
    if (config.testing.smokeTests && hasInterface(config, "tcp-custom")) {
      skills.push("run-smoke-socket");
    }
    skills.push("run-e2e");
    if (config.testing.performanceTests) skills.push("run-perf-test");
    if (config.testing.contractTests) skills.push("run-contract-tests");
    return skills;
  }

  private selectSecuritySkills(config: ProjectConfig): string[] {
    if (config.security.frameworks.length > 0) {
      return ["x-review-security"];
    }
    return [];
  }

  private selectDataPacks(config: ProjectConfig): string[] {
    if (
      config.data.database.name !== "none" ||
      config.data.cache.name !== "none"
    ) {
      return ["database-patterns"];
    }
    return [];
  }

  private copyCoreSkill(
    skillName: string,
    resourcesDir: string,
    outputDir: string,
    engine: TemplateEngine,
  ): string {
    const src = path.join(
      resourcesDir, SKILLS_TEMPLATES_DIR, CORE_DIR, skillName,
    );
    const dest = path.join(outputDir, SKILLS_OUTPUT, skillName);
    return copyTemplateTree(src, dest, engine);
  }

  private copyConditionalSkill(
    skillName: string,
    resourcesDir: string,
    outputDir: string,
    engine: TemplateEngine,
  ): string | null {
    const src = path.join(
      resourcesDir, SKILLS_TEMPLATES_DIR, CONDITIONAL_DIR, skillName,
    );
    const dest = path.join(outputDir, SKILLS_OUTPUT, skillName);
    return copyTemplateTreeIfExists(src, dest, engine);
  }

  private copyKnowledgePack(
    packName: string,
    resourcesDir: string,
    outputDir: string,
    engine: TemplateEngine,
  ): string | null {
    const src = path.join(
      resourcesDir, SKILLS_TEMPLATES_DIR, KNOWLEDGE_PACKS_DIR, packName,
    );
    if (!fs.existsSync(src)) return null;
    const dest = path.join(outputDir, SKILLS_OUTPUT, packName);
    fs.mkdirSync(dest, { recursive: true });
    const skillMdSrc = path.join(src, SKILL_MD);
    if (fs.existsSync(skillMdSrc)) {
      copyTemplateFile(skillMdSrc, path.join(dest, SKILL_MD), engine);
    }
    this.copyNonSkillItems(src, dest);
    return dest;
  }

  private copyNonSkillItems(src: string, dest: string): void {
    const entries = fs.readdirSync(src, { withFileTypes: true });
    for (const entry of entries) {
      if (entry.name === SKILL_MD) continue;
      const target = path.join(dest, entry.name);
      if (fs.existsSync(target)) continue;
      const source = path.join(src, entry.name);
      if (entry.isDirectory()) {
        fs.cpSync(source, target, { recursive: true });
      } else {
        fs.copyFileSync(source, target);
      }
    }
  }

  private copyStackPatterns(
    config: ProjectConfig,
    resourcesDir: string,
    outputDir: string,
    engine: TemplateEngine,
  ): string | null {
    const packName = getStackPackName(config.framework.name);
    if (!packName) return null;
    const src = path.join(
      resourcesDir, SKILLS_TEMPLATES_DIR,
      KNOWLEDGE_PACKS_DIR, STACK_PATTERNS_DIR, packName,
    );
    const dest = path.join(outputDir, SKILLS_OUTPUT, packName);
    return copyTemplateTreeIfExists(src, dest, engine);
  }

  private copyInfraPatterns(
    config: ProjectConfig,
    resourcesDir: string,
    outputDir: string,
    engine: TemplateEngine,
  ): string[] {
    const results: string[] = [];
    for (const [packName, condition] of buildInfraPackRules(config)) {
      if (!condition) continue;
      const src = path.join(
        resourcesDir, SKILLS_TEMPLATES_DIR,
        KNOWLEDGE_PACKS_DIR, INFRA_PATTERNS_DIR, packName,
      );
      const dest = path.join(outputDir, SKILLS_OUTPUT, packName);
      const copied = copyTemplateTreeIfExists(src, dest, engine);
      if (copied !== null) results.push(copied);
    }
    return results;
  }

  private assembleCore(
    resourcesDir: string,
    outputDir: string,
    engine: TemplateEngine,
  ): string[] {
    return this.selectCoreSkills(resourcesDir).map(
      (skill) => this.copyCoreSkill(skill, resourcesDir, outputDir, engine),
    );
  }

  private assembleConditional(
    config: ProjectConfig,
    resourcesDir: string,
    outputDir: string,
    engine: TemplateEngine,
  ): string[] {
    const results: string[] = [];
    for (const skill of this.selectConditionalSkills(config)) {
      const copied = this.copyConditionalSkill(
        skill, resourcesDir, outputDir, engine,
      );
      if (copied !== null) results.push(copied);
    }
    return results;
  }

  private assembleKnowledge(
    config: ProjectConfig,
    resourcesDir: string,
    outputDir: string,
    engine: TemplateEngine,
  ): string[] {
    const results: string[] = [];
    for (const pack of this.selectKnowledgePacks(config)) {
      const copied = this.copyKnowledgePack(
        pack, resourcesDir, outputDir, engine,
      );
      if (copied !== null) results.push(copied);
    }
    const stack = this.copyStackPatterns(
      config, resourcesDir, outputDir, engine,
    );
    if (stack !== null) results.push(stack);
    results.push(
      ...this.copyInfraPatterns(config, resourcesDir, outputDir, engine),
    );
    return results;
  }
}
