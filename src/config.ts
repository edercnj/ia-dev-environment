import { resolve } from "node:path";
import { readFileSync } from "node:fs";
import yaml from "js-yaml";
import { ConfigValidationError } from "./exceptions.js";
import { ProjectConfig } from "./models.js";

export interface RuntimePaths {
  readonly cwd: string;
  readonly outputDir: string;
  readonly resourcesDir: string;
}

const DEFAULT_OUTPUT_DIR = "dist";
const DEFAULT_RESOURCES_DIR = "resources";

export function createRuntimePaths(cwd: string = process.cwd()): RuntimePaths {
  return Object.freeze({
    cwd,
    outputDir: resolve(cwd, DEFAULT_OUTPUT_DIR),
    resourcesDir: resolve(cwd, DEFAULT_RESOURCES_DIR),
  });
}

// --- STORY-004: Config Loader ---

export const REQUIRED_SECTIONS = [
  "project",
  "architecture",
  "interfaces",
  "language",
  "framework",
] as const;

export const TYPE_MAPPING: Record<
  string,
  { style: string; interfaces: Array<{ type: string }> }
> = {
  api: { style: "microservice", interfaces: [{ type: "rest" }] },
  cli: { style: "library", interfaces: [{ type: "cli" }] },
  library: { style: "library", interfaces: [] },
  worker: {
    style: "microservice",
    interfaces: [{ type: "event-consumer" }],
  },
  fullstack: {
    style: "monolith",
    interfaces: [{ type: "rest" }],
  },
};

const DEFAULT_PROJECT = { name: "unnamed", purpose: "" } as const;

const DEFAULT_TYPE_MAPPING = {
  style: "microservice",
  interfaces: [{ type: "rest" }],
};

export const STACK_MAPPING: Record<
  string,
  {
    language: string;
    version: string;
    framework: string;
    frameworkVersion: string;
  }
> = {
  "java-quarkus": {
    language: "java",
    version: "21",
    framework: "quarkus",
    frameworkVersion: "3.17",
  },
  "java-spring": {
    language: "java",
    version: "21",
    framework: "spring-boot",
    frameworkVersion: "3.4",
  },
  "python-fastapi": {
    language: "python",
    version: "3.12",
    framework: "fastapi",
    frameworkVersion: "0.115",
  },
  "python-click-cli": {
    language: "python",
    version: "3.9",
    framework: "click",
    frameworkVersion: "8.1",
  },
  "go-gin": {
    language: "go",
    version: "1.23",
    framework: "gin",
    frameworkVersion: "1.10",
  },
  "kotlin-ktor": {
    language: "kotlin",
    version: "2.1",
    framework: "ktor",
    frameworkVersion: "3.0",
  },
  "typescript-nestjs": {
    language: "typescript",
    version: "5.7",
    framework: "nestjs",
    frameworkVersion: "10.4",
  },
  "rust-axum": {
    language: "rust",
    version: "1.83",
    framework: "axum",
    frameworkVersion: "0.8",
  },
};

/**
 * Detects whether the given data uses the legacy v2 config format.
 * Returns true if `data.type` exists in TYPE_MAPPING or `data.stack` exists in STACK_MAPPING.
 */
export function detectV2Format(
  data: Record<string, unknown>,
): boolean {
  const typeKey = data["type"] as string | undefined;
  const stackKey = data["stack"] as string | undefined;
  return (
    (typeKey !== undefined && typeKey in TYPE_MAPPING) ||
    (stackKey !== undefined && stackKey in STACK_MAPPING)
  );
}

function buildArchitectureSection(
  data: Record<string, unknown>,
): { architecture: { style: string }; interfaces: Array<{ type: string }> } {
  const typeKey = data["type"] as string | undefined;
  const mapping =
    typeKey !== undefined && typeKey in TYPE_MAPPING
      ? TYPE_MAPPING[typeKey]!
      : DEFAULT_TYPE_MAPPING;
  return {
    architecture: { style: mapping.style },
    interfaces: mapping.interfaces,
  };
}

function buildLanguageFramework(
  data: Record<string, unknown>,
): { language: { name: string; version: string }; framework: { name: string; version: string } } {
  const stackKey = data["stack"] as string | undefined;
  if (stackKey === undefined || !(stackKey in STACK_MAPPING)) {
    throw new ConfigValidationError([
      `Unknown stack: '${String(stackKey)}'. Valid stacks: ${Object.keys(STACK_MAPPING).join(", ")}`,
    ]);
  }
  const mapping = STACK_MAPPING[stackKey]!;
  return {
    language: { name: mapping.language, version: mapping.version },
    framework: {
      name: mapping.framework,
      version: mapping.frameworkVersion,
    },
  };
}

/**
 * Migrates a v2 config format to v3.
 * Emits a deprecation warning via console.warn.
 */
export function migrateV2ToV3(
  data: Record<string, unknown>,
): Record<string, unknown> {
  console.warn("Config uses legacy v2 format. Auto-migrating to v3.");

  const archSection = buildArchitectureSection(data);
  const langSection = buildLanguageFramework(data);

  const project =
    (data["project"] as Record<string, unknown> | undefined) ?? {
      ...DEFAULT_PROJECT,
    };

  const result: Record<string, unknown> = { ...data };
  delete result["type"];
  delete result["stack"];

  result["project"] = project;
  result["architecture"] = archSection.architecture;
  result["interfaces"] = archSection.interfaces;
  result["language"] = langSection.language;
  result["framework"] = langSection.framework;

  return result;
}

/**
 * Validates that all required sections are present in the config data.
 * Throws ConfigValidationError if any sections are missing.
 */
export function validateConfig(
  data: Record<string, unknown> | null | undefined,
): void {
  const missing: string[] = [];
  for (const section of REQUIRED_SECTIONS) {
    if (data == null || !(section in data)) {
      missing.push(section);
    }
  }
  if (missing.length > 0) {
    throw new ConfigValidationError(missing);
  }
}

/**
 * Loads a YAML config file, migrates from v2 if needed, validates, and returns a ProjectConfig.
 */
export function loadConfig(path: string): ProjectConfig {
  const content = readFileSync(path, "utf-8");
  let data: unknown;
  try {
    data = yaml.load(content);
  } catch (error: unknown) {
    const detail = error instanceof Error ? error.message : "unknown error";
    throw new ConfigValidationError([`Invalid YAML syntax: ${detail}`]);
  }

  if (data == null || typeof data !== "object" || Array.isArray(data)) {
    throw new ConfigValidationError([...REQUIRED_SECTIONS]);
  }

  const record = data as Record<string, unknown>;

  if (detectV2Format(record)) {
    const migrated = migrateV2ToV3(record);
    validateConfig(migrated);
    return ProjectConfig.fromDict(migrated);
  }

  validateConfig(record);
  return ProjectConfig.fromDict(record);
}
