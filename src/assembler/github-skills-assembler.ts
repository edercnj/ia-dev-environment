/**
 * GithubSkillsAssembler -- generates github/skills/{name}/SKILL.md from templates.
 *
 * Migrated from Python `assembler/github_skills_assembler.py` (163 lines).
 * Skill groups are iterated in insertion order; the infrastructure group
 * applies conditional filtering based on project config.
 *
 * @module
 */
import * as fs from "node:fs";
import * as path from "node:path";
import type { ProjectConfig } from "../models.js";
import type { TemplateEngine } from "../template-engine.js";

const GITHUB_SKILLS_TEMPLATES_DIR = "github-skills-templates";
const SKILL_MD = "SKILL.md";
const INFRA_GROUP = "infrastructure";

/** Groups whose skills are nested under a subdirectory in the output. */
export const NESTED_GROUPS: ReadonlySet<string> = new Set(["lib"]);

/** Skill groups mapping group name to template filenames (without .md). */
export const SKILL_GROUPS: Record<string, readonly string[]> = {
  "story": [
    "x-story-epic", "x-story-create", "x-story-map",
    "x-story-epic-full", "story-planning",
  ],
  "dev": [
    "x-dev-implement", "x-dev-lifecycle", "layer-templates",
  ],
  "review": [
    "x-review", "x-review-api", "x-review-pr",
    "x-review-grpc", "x-review-events", "x-review-gateway",
  ],
  "testing": [
    "x-test-plan", "x-test-run", "run-e2e",
    "run-smoke-api", "run-contract-tests", "run-perf-test",
  ],
  "infrastructure": [
    "setup-environment", "k8s-deployment", "k8s-kustomize",
    "dockerfile", "iac-terraform",
  ],
  "knowledge-packs": [
    "architecture", "coding-standards", "patterns",
    "protocols", "observability", "resilience",
    "security", "compliance", "api-design",
  ],
  "git-troubleshooting": [
    "x-git-push", "x-ops-troubleshoot",
  ],
  "lib": [
    "x-lib-task-decomposer", "x-lib-audit-rules",
    "x-lib-group-verifier",
  ],
};

/** Infrastructure skill conditions: maps skill name to config predicate. */
export const INFRA_SKILL_CONDITIONS: Record<
  string, (config: ProjectConfig) => boolean
> = {
  "setup-environment": (c) => c.infrastructure.orchestrator !== "none",
  "k8s-deployment": (c) => c.infrastructure.orchestrator === "kubernetes",
  "k8s-kustomize": (c) => c.infrastructure.templating === "kustomize",
  "dockerfile": (c) => c.infrastructure.container !== "none",
  "iac-terraform": (c) => c.infrastructure.iac === "terraform",
};

/** Generates github/skills/{name}/SKILL.md from group templates. */
export class GithubSkillsAssembler {
  /** Generate skill files from all registered groups. */
  assemble(
    config: ProjectConfig,
    outputDir: string,
    resourcesDir: string,
    engine: TemplateEngine,
  ): string[] {
    const results: string[] = [];
    for (const [group, skillNames] of Object.entries(SKILL_GROUPS)) {
      const filtered = this.filterSkills(config, group, skillNames);
      const srcDir = path.join(
        resourcesDir, GITHUB_SKILLS_TEMPLATES_DIR, group,
      );
      const subDir = NESTED_GROUPS.has(group) ? group : undefined;
      results.push(
        ...this.generateGroup(engine, srcDir, outputDir, filtered, subDir),
      );
    }
    return results;
  }

  /** Filter skills based on config conditions (infrastructure group only). */
  private filterSkills(
    config: ProjectConfig,
    group: string,
    skillNames: readonly string[],
  ): string[] {
    if (group !== INFRA_GROUP) return [...skillNames];
    return skillNames.filter((name) => {
      const condition = INFRA_SKILL_CONDITIONS[name];
      return condition === undefined || condition(config);
    });
  }

  private generateGroup(
    engine: TemplateEngine,
    srcDir: string,
    outputDir: string,
    skillNames: readonly string[],
    subDir?: string,
  ): string[] {
    if (!fs.existsSync(srcDir)) return [];
    const results: string[] = [];
    for (const name of skillNames) {
      const dest = this.renderSkill(engine, srcDir, outputDir, name, subDir);
      if (dest !== null) results.push(dest);
    }
    return results;
  }

  private renderSkill(
    engine: TemplateEngine,
    srcDir: string,
    outputDir: string,
    name: string,
    subDir?: string,
  ): string | null {
    const src = path.join(srcDir, `${name}.md`);
    if (!fs.existsSync(src)) return null;
    const rendered = engine.replacePlaceholders(
      fs.readFileSync(src, "utf-8"),
    );
    const segments = [outputDir, "github", "skills"];
    if (subDir !== undefined) segments.push(subDir);
    segments.push(name);
    const skillDir = path.join(...segments);
    fs.mkdirSync(skillDir, { recursive: true });
    const dest = path.join(skillDir, SKILL_MD);
    fs.writeFileSync(dest, rendered, "utf-8");
    return dest;
  }
}
