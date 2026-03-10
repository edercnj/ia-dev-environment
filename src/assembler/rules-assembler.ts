/**
 * RulesAssembler — assembles .claude/rules/ and skills/ from source knowledge packs.
 *
 * @remarks
 * Migrated from Python `assembler/rules_assembler.py` (540 lines).
 * Split into three modules for 250-line compliance:
 * - `rules-assembler.ts` — main class (this file)
 * - `rules-identity.ts` — identity content builders
 * - `rules-conditionals.ts` — conditional assembly functions
 *
 * @module
 */
import * as fs from "node:fs";
import * as path from "node:path";
import type { ProjectConfig } from "../models.js";
import type { TemplateEngine } from "../template-engine.js";
import { getActiveRoutes } from "../domain/core-kp-routing.js";
import { getStackPackName } from "../domain/stack-pack-mapping.js";
import { findVersionDir } from "../domain/version-resolver.js";
import { auditRulesContext } from "./auditor.js";
import { buildIdentityContent, fallbackDomainContent } from "./rules-identity.js";
import {
  copyDatabaseRefs,
  copyCacheRefs,
  assembleSecurityRules,
  assembleCloudKnowledge,
  assembleInfraKnowledge,
} from "./rules-conditionals.js";

/** Result returned by {@link RulesAssembler.assemble}. */
export interface AssembleResult {
  readonly files: string[];
  readonly warnings: string[];
}

/** Assembles .claude/rules/ and skills/ from source knowledge packs. */
export class RulesAssembler {
  /** Orchestrate all assembly layers. */
  assemble(
    config: ProjectConfig,
    outputDir: string,
    resourcesDir: string,
    engine: TemplateEngine,
  ): AssembleResult {
    const rulesDir = path.join(outputDir, "rules");
    const skillsDir = path.join(outputDir, "skills");
    fs.mkdirSync(rulesDir, { recursive: true });
    fs.mkdirSync(skillsDir, { recursive: true });
    const generated: string[] = [];
    generated.push(...this.copyCoreRules(resourcesDir, rulesDir, engine));
    generated.push(...this.routeCoreToKps(config, resourcesDir, skillsDir));
    generated.push(...this.copyLanguageKps(config, resourcesDir, skillsDir));
    generated.push(...this.copyFrameworkKps(config, resourcesDir, skillsDir));
    generated.push(this.generateProjectIdentity(config, rulesDir));
    generated.push(this.copyDomainTemplate(config, resourcesDir, rulesDir, engine));
    generated.push(...copyDatabaseRefs(config, resourcesDir, skillsDir, engine));
    generated.push(...copyCacheRefs(config, resourcesDir, skillsDir));
    generated.push(...assembleSecurityRules(config, resourcesDir, skillsDir));
    generated.push(...assembleCloudKnowledge(config, resourcesDir, skillsDir));
    generated.push(...assembleInfraKnowledge(config, resourcesDir, skillsDir));
    const audit = auditRulesContext(rulesDir);
    return { files: generated, warnings: [...audit.warnings] };
  }

  /** Layer 1: Copy core-rules/*.md with placeholder replacement. */
  private copyCoreRules(
    resourcesDir: string,
    rulesDir: string,
    engine: TemplateEngine,
  ): string[] {
    const coreRules = path.join(resourcesDir, "core-rules");
    if (!fs.existsSync(coreRules) || !fs.statSync(coreRules).isDirectory()) {
      return [];
    }
    const generated: string[] = [];
    const files = fs.readdirSync(coreRules).filter((f) => f.endsWith(".md")).sort();
    for (const file of files) {
      const content = fs.readFileSync(path.join(coreRules, file), "utf-8");
      const replaced = engine.replacePlaceholders(content);
      const dest = path.join(rulesDir, file);
      fs.writeFileSync(dest, replaced, "utf-8");
      generated.push(dest);
    }
    return generated;
  }

  /** Layer 1b: Route core detailed rules to knowledge packs. */
  private routeCoreToKps(
    config: ProjectConfig,
    resourcesDir: string,
    skillsDir: string,
  ): string[] {
    const coreDir = path.join(resourcesDir, "core");
    if (!fs.existsSync(coreDir) || !fs.statSync(coreDir).isDirectory()) {
      return [];
    }
    const routes = getActiveRoutes(config);
    const generated: string[] = [];
    for (const route of routes) {
      const src = path.join(coreDir, route.sourceFile);
      if (!fs.existsSync(src) || !fs.statSync(src).isFile()) continue;
      const destDir = path.join(skillsDir, route.kpName, "references");
      fs.mkdirSync(destDir, { recursive: true });
      const dest = path.join(destDir, route.destFile);
      fs.copyFileSync(src, dest);
      generated.push(dest);
    }
    return generated;
  }

  /** Layer 2: Route language files to coding-standards and testing KPs. */
  private copyLanguageKps(
    config: ProjectConfig,
    resourcesDir: string,
    skillsDir: string,
  ): string[] {
    const lang = config.language.name;
    const langDir = path.join(resourcesDir, "languages", lang);
    if (!fs.existsSync(langDir) || !fs.statSync(langDir).isDirectory()) {
      return [];
    }
    const codingRefs = path.join(skillsDir, "coding-standards", "references");
    const testingRefs = path.join(skillsDir, "testing", "references");
    fs.mkdirSync(codingRefs, { recursive: true });
    fs.mkdirSync(testingRefs, { recursive: true });
    const generated: string[] = [];
    generated.push(...this.copyLangCommon(langDir, codingRefs, testingRefs));
    generated.push(...this.copyLangVersion(config, langDir, codingRefs));
    return generated;
  }

  private copyLangCommon(
    langDir: string,
    codingRefs: string,
    testingRefs: string,
  ): string[] {
    const common = path.join(langDir, "common");
    if (!fs.existsSync(common) || !fs.statSync(common).isDirectory()) {
      return [];
    }
    const generated: string[] = [];
    const files = fs.readdirSync(common).filter((f) => f.endsWith(".md")).sort();
    for (const file of files) {
      const dest = file.includes("testing") ? testingRefs : codingRefs;
      const target = path.join(dest, file);
      fs.copyFileSync(path.join(common, file), target);
      generated.push(target);
    }
    return generated;
  }

  private copyLangVersion(
    config: ProjectConfig,
    langDir: string,
    codingRefs: string,
  ): string[] {
    const versionDir = findVersionDir(
      langDir, config.language.name, config.language.version,
    );
    if (!versionDir) return [];
    const generated: string[] = [];
    const files = fs.readdirSync(versionDir).filter((f) => f.endsWith(".md")).sort();
    for (const file of files) {
      const target = path.join(codingRefs, file);
      fs.copyFileSync(path.join(versionDir, file), target);
      generated.push(target);
    }
    return generated;
  }

  /** Layer 3: Route framework files to stack-patterns KP. */
  private copyFrameworkKps(
    config: ProjectConfig,
    resourcesDir: string,
    skillsDir: string,
  ): string[] {
    const fw = config.framework.name;
    const packName = getStackPackName(fw);
    if (!packName) return [];
    const fwDir = path.join(resourcesDir, "frameworks", fw);
    if (!fs.existsSync(fwDir) || !fs.statSync(fwDir).isDirectory()) {
      return [];
    }
    const refsDir = path.join(skillsDir, packName, "references");
    fs.mkdirSync(refsDir, { recursive: true });
    const generated: string[] = [];
    generated.push(...this.copyFwCommon(fwDir, refsDir));
    generated.push(...this.copyFwVersion(config, fwDir, refsDir));
    return generated;
  }

  private copyFwCommon(fwDir: string, refsDir: string): string[] {
    const common = path.join(fwDir, "common");
    if (!fs.existsSync(common) || !fs.statSync(common).isDirectory()) {
      return [];
    }
    const generated: string[] = [];
    const files = fs.readdirSync(common).filter((f) => f.endsWith(".md")).sort();
    for (const file of files) {
      const target = path.join(refsDir, file);
      fs.copyFileSync(path.join(common, file), target);
      generated.push(target);
    }
    return generated;
  }

  private copyFwVersion(
    config: ProjectConfig,
    fwDir: string,
    refsDir: string,
  ): string[] {
    const versionDir = findVersionDir(
      fwDir, config.framework.name, config.framework.version,
    );
    if (!versionDir) return [];
    const generated: string[] = [];
    const files = fs.readdirSync(versionDir).filter((f) => f.endsWith(".md")).sort();
    for (const file of files) {
      const target = path.join(refsDir, file);
      fs.copyFileSync(path.join(versionDir, file), target);
      generated.push(target);
    }
    return generated;
  }

  /** Layer 4: Generate 01-project-identity.md. */
  private generateProjectIdentity(
    config: ProjectConfig,
    rulesDir: string,
  ): string {
    const dest = path.join(rulesDir, "01-project-identity.md");
    const content = buildIdentityContent(config);
    fs.writeFileSync(dest, content, "utf-8");
    return dest;
  }

  /** Layer 4: Copy/generate 02-domain.md. */
  private copyDomainTemplate(
    config: ProjectConfig,
    resourcesDir: string,
    rulesDir: string,
    engine: TemplateEngine,
  ): string {
    const dest = path.join(rulesDir, "02-domain.md");
    const template = path.join(resourcesDir, "templates", "domain-template.md");
    if (fs.existsSync(template) && fs.statSync(template).isFile()) {
      const content = fs.readFileSync(template, "utf-8");
      const replaced = engine.replacePlaceholders(content);
      fs.writeFileSync(dest, replaced, "utf-8");
    } else {
      fs.writeFileSync(dest, fallbackDomainContent(config), "utf-8");
    }
    return dest;
  }
}
