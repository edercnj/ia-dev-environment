/**
 * Conditional assembly functions for rules assembler.
 *
 * @remarks
 * Extracted from the main RulesAssembler class for module size compliance.
 * Each function handles a conditional copy based on project configuration.
 *
 * @module
 */
import * as fs from "node:fs";
import * as path from "node:path";
import type { ProjectConfig } from "../models.js";
import type { TemplateEngine } from "../template-engine.js";
import { replacePlaceholdersInDir } from "./copy-helpers.js";

const NONE_VALUE = "none";
const SQL_DB_TYPES = ["postgresql", "oracle", "mysql"] as const;
const NOSQL_DB_TYPES = ["mongodb", "cassandra"] as const;

/** Copy .md files from a source directory to target. */
export function copyMdDir(
  sourceDir: string,
  target: string,
): string[] {
  if (!fs.existsSync(sourceDir) || !fs.statSync(sourceDir).isDirectory()) {
    return [];
  }
  const generated: string[] = [];
  const entries = fs.readdirSync(sourceDir).filter((f) => f.endsWith(".md")).sort();
  for (const entry of entries) {
    const src = path.join(sourceDir, entry);
    if (!fs.statSync(src).isFile()) continue;
    const dest = path.join(target, entry);
    fs.copyFileSync(src, dest);
    generated.push(dest);
  }
  return generated;
}

/** Conditional: copy database references. */
export function copyDatabaseRefs(
  config: ProjectConfig,
  resourcesDir: string,
  skillsDir: string,
  engine: TemplateEngine,
): string[] {
  const dbName = config.data.database.name;
  if (dbName === NONE_VALUE) return [];
  const dbDir = path.join(resourcesDir, "databases");
  const target = path.join(skillsDir, "database-patterns", "references");
  fs.mkdirSync(target, { recursive: true });
  const generated: string[] = [];
  generated.push(...copyDbVersionMatrix(dbDir, target));
  generated.push(...copyDbTypeFiles(dbName, dbDir, target));
  replacePlaceholdersInDir(target, engine);
  return generated;
}

function copyDbVersionMatrix(dbDir: string, target: string): string[] {
  const matrix = path.join(dbDir, "version-matrix.md");
  if (fs.existsSync(matrix) && fs.statSync(matrix).isFile()) {
    const dest = path.join(target, "version-matrix.md");
    fs.copyFileSync(matrix, dest);
    return [dest];
  }
  return [];
}

function copyDbTypeFiles(
  dbName: string,
  dbDir: string,
  target: string,
): string[] {
  const generated: string[] = [];
  if ((SQL_DB_TYPES as readonly string[]).includes(dbName)) {
    generated.push(...copyMdDir(path.join(dbDir, "sql", "common"), target));
    generated.push(...copyMdDir(path.join(dbDir, "sql", dbName), target));
  } else if ((NOSQL_DB_TYPES as readonly string[]).includes(dbName)) {
    generated.push(...copyMdDir(path.join(dbDir, "nosql", "common"), target));
    generated.push(...copyMdDir(path.join(dbDir, "nosql", dbName), target));
  }
  return generated;
}

/** Conditional: copy cache references. */
export function copyCacheRefs(
  config: ProjectConfig,
  resourcesDir: string,
  skillsDir: string,
): string[] {
  const cacheName = config.data.cache.name;
  if (cacheName === NONE_VALUE) return [];
  const dbDir = path.join(resourcesDir, "databases");
  const target = path.join(skillsDir, "database-patterns", "references");
  fs.mkdirSync(target, { recursive: true });
  const generated: string[] = [];
  generated.push(...copyMdDir(path.join(dbDir, "cache", "common"), target));
  generated.push(...copyMdDir(path.join(dbDir, "cache", cacheName), target));
  return generated;
}

/** Conditional: copy security files to security/compliance KPs. */
export function assembleSecurityRules(
  config: ProjectConfig,
  resourcesDir: string,
  skillsDir: string,
): string[] {
  if (config.security.frameworks.length === 0) return [];
  const secDir = path.join(resourcesDir, "security");
  const generated: string[] = [];
  generated.push(...copySecurityBase(secDir, skillsDir));
  generated.push(...copyCompliance(config, secDir, skillsDir));
  return generated;
}

function copySecurityBase(secDir: string, skillsDir: string): string[] {
  const secKp = path.join(skillsDir, "security", "references");
  fs.mkdirSync(secKp, { recursive: true });
  const generated: string[] = [];
  for (const name of ["application-security.md", "cryptography.md"]) {
    const src = path.join(secDir, name);
    if (fs.existsSync(src) && fs.statSync(src).isFile()) {
      const dest = path.join(secKp, name);
      fs.copyFileSync(src, dest);
      generated.push(dest);
    }
  }
  return generated;
}

function copyCompliance(
  config: ProjectConfig,
  secDir: string,
  skillsDir: string,
): string[] {
  const compKp = path.join(skillsDir, "compliance", "references");
  fs.mkdirSync(compKp, { recursive: true });
  const generated: string[] = [];
  for (const framework of config.security.frameworks) {
    const src = path.join(secDir, "compliance", `${framework}.md`);
    if (fs.existsSync(src) && fs.statSync(src).isFile()) {
      const dest = path.join(compKp, `${framework}.md`);
      fs.copyFileSync(src, dest);
      generated.push(dest);
    }
  }
  return generated;
}

/** Conditional: copy cloud provider files. */
export function assembleCloudKnowledge(
  config: ProjectConfig,
  resourcesDir: string,
  skillsDir: string,
): string[] {
  const infra = config.infrastructure as unknown as Record<string, unknown>;
  const provider = typeof infra["cloudProvider"] === "string"
    ? (infra["cloudProvider"] as string)
    : NONE_VALUE;
  if (provider === NONE_VALUE) return [];
  const cloudDir = path.join(resourcesDir, "cloud-providers");
  const kpDir = path.join(skillsDir, "knowledge-packs");
  fs.mkdirSync(kpDir, { recursive: true });
  const src = path.join(cloudDir, `${provider}.md`);
  if (fs.existsSync(src) && fs.statSync(src).isFile()) {
    const dest = path.join(kpDir, `cloud-${provider}.md`);
    fs.copyFileSync(src, dest);
    return [dest];
  }
  return [];
}

/** Conditional: copy infrastructure knowledge packs. */
export function assembleInfraKnowledge(
  config: ProjectConfig,
  resourcesDir: string,
  skillsDir: string,
): string[] {
  const infraDir = path.join(resourcesDir, "infrastructure");
  const kpDir = path.join(skillsDir, "knowledge-packs");
  fs.mkdirSync(kpDir, { recursive: true });
  const generated: string[] = [];
  generated.push(...copyK8sFiles(config, infraDir, kpDir));
  generated.push(...copyContainerFiles(config, infraDir, kpDir));
  generated.push(...copyIacFiles(config, infraDir, kpDir));
  return generated;
}

function copyK8sFiles(
  config: ProjectConfig,
  infraDir: string,
  kpDir: string,
): string[] {
  if (config.infrastructure.orchestrator !== "kubernetes") return [];
  const src = path.join(infraDir, "kubernetes", "deployment-patterns.md");
  if (fs.existsSync(src) && fs.statSync(src).isFile()) {
    const dest = path.join(kpDir, "k8s-deployment.md");
    fs.copyFileSync(src, dest);
    return [dest];
  }
  return [];
}

function copyContainerFiles(
  config: ProjectConfig,
  infraDir: string,
  kpDir: string,
): string[] {
  if (config.infrastructure.container === NONE_VALUE) return [];
  const generated: string[] = [];
  const filePairs: Array<[string, string]> = [
    ["dockerfile-patterns.md", "dockerfile.md"],
    ["registry-patterns.md", "registry.md"],
  ];
  for (const [name, destName] of filePairs) {
    const src = path.join(infraDir, "containers", name);
    if (fs.existsSync(src) && fs.statSync(src).isFile()) {
      const dest = path.join(kpDir, destName);
      fs.copyFileSync(src, dest);
      generated.push(dest);
    }
  }
  return generated;
}

function copyIacFiles(
  config: ProjectConfig,
  infraDir: string,
  kpDir: string,
): string[] {
  const iac = config.infrastructure.iac;
  if (iac === NONE_VALUE || !iac) return [];
  const src = path.join(infraDir, "iac", `${iac}-patterns.md`);
  if (fs.existsSync(src) && fs.statSync(src).isFile()) {
    const dest = path.join(kpDir, `iac-${iac}.md`);
    fs.copyFileSync(src, dest);
    return [dest];
  }
  return [];
}
