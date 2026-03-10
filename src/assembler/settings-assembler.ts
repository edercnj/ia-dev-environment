/**
 * SettingsAssembler — generates settings.json and settings.local.json.
 *
 * Migrated from Python `assembler/settings_assembler.py` (175 lines).
 * Permission sources are resolved via {@link ../domain/stack-mapping.ts}.
 *
 * @remarks
 * Settings JSON files do not contain template placeholders.
 * The `engine` parameter is accepted for API uniformity but is not used.
 *
 * @module
 */
import * as fs from "node:fs";
import * as path from "node:path";
import type { ProjectConfig } from "../models.js";
import type { TemplateEngine } from "../template-engine.js";
import {
  getHookTemplateKey,
  getSettingsLangKey,
  getDatabaseSettingsKey,
  getCacheSettingsKey,
} from "../domain/stack-mapping.js";

const SETTINGS_TEMPLATES_DIR = "settings-templates";
const SETTINGS_FILENAME = "settings.json";
const SETTINGS_LOCAL_FILENAME = "settings.local.json";
const BASE_PERMISSIONS_FILE = "base.json";
const DOCKER_PERMISSIONS_FILE = "docker.json";
const K8S_PERMISSIONS_FILE = "kubernetes.json";
const COMPOSE_PERMISSIONS_FILE = "docker-compose.json";
const NEWMAN_PERMISSIONS_FILE = "testing-newman.json";
const JSON_INDENT = 2;
const CONTAINER_DOCKER = "docker";
const CONTAINER_PODMAN = "podman";
const ORCH_KUBERNETES = "kubernetes";
const ORCH_DOCKER_COMPOSE = "docker-compose";
const HOOK_TIMEOUT = 60;

interface SettingsJson {
  permissions: { allow: string[] };
  hooks?: HooksSection;
}

interface HooksSection {
  PostToolUse: PostToolUseHook[];
}

interface PostToolUseHook {
  matcher: string;
  hooks: HookCommand[];
}

interface HookCommand {
  type: string;
  command: string;
  timeout: number;
  statusMessage: string;
}

/** Merge two string arrays by concatenation. */
export function mergeJsonArrays(
  base: string[],
  overlay: string[],
): string[] {
  return [...base, ...overlay];
}

/** Remove duplicates preserving insertion order. */
export function deduplicate(items: string[]): string[] {
  return [...new Set(items)];
}

/** Read a JSON file containing a string array; returns [] on error. */
export function readJsonArray(filePath: string): string[] {
  try {
    const text = fs.readFileSync(filePath, "utf-8");
    const data: unknown = JSON.parse(text);
    if (!Array.isArray(data)) return [];
    return data as string[];
  } catch {
    return [];
  }
}

/** Build the settings.json object structure. */
export function buildSettingsDict(
  permissions: string[],
  hasHooks: boolean,
): SettingsJson {
  const settings: SettingsJson = {
    permissions: { allow: permissions },
  };
  if (hasHooks) {
    settings.hooks = buildHooksSection();
  }
  return settings;
}

/** Build the hooks section for compiled languages. */
export function buildHooksSection(): HooksSection {
  return {
    PostToolUse: [
      {
        matcher: "Write|Edit",
        hooks: [
          {
            type: "command",
            command:
              '"$CLAUDE_PROJECT_DIR"/.claude/hooks/post-compile-check.sh',
            timeout: HOOK_TIMEOUT,
            statusMessage: "Checking compilation...",
          },
        ],
      },
    ],
  };
}

/** Generates settings.json and settings.local.json. */
export class SettingsAssembler {
  /** Generate settings files with merged, deduplicated permissions. */
  assemble(
    config: ProjectConfig,
    outputDir: string,
    resourcesDir: string,
    _engine: TemplateEngine,
  ): string[] {
    const templatesDir = path.join(
      resourcesDir, SETTINGS_TEMPLATES_DIR,
    );
    const raw = this.collectPermissions(config, templatesDir);
    const permissions = deduplicate(raw);
    const hasHooks = getHookTemplateKey(
      config.language.name, config.framework.buildTool,
    ) !== "";
    const settings = buildSettingsDict(permissions, hasHooks);
    const results: string[] = [];
    results.push(this.writeSettings(outputDir, settings));
    results.push(this.writeSettingsLocal(outputDir));
    return results;
  }

  private collectPermissions(
    config: ProjectConfig,
    templatesDir: string,
  ): string[] {
    let result = this.mergeFile([], BASE_PERMISSIONS_FILE, templatesDir);
    const langKey = getSettingsLangKey(
      config.language.name, config.framework.buildTool,
    );
    if (langKey !== "") {
      result = this.mergeFile(result, `${langKey}.json`, templatesDir);
    }
    result = this.collectInfra(config, templatesDir, result);
    result = this.collectData(config, templatesDir, result);
    if (config.testing.smokeTests) {
      result = this.mergeFile(result, NEWMAN_PERMISSIONS_FILE, templatesDir);
    }
    return result;
  }

  private collectInfra(
    config: ProjectConfig,
    templatesDir: string,
    result: string[],
  ): string[] {
    const container = config.infrastructure.container;
    if (container === CONTAINER_DOCKER || container === CONTAINER_PODMAN) {
      result = this.mergeFile(result, DOCKER_PERMISSIONS_FILE, templatesDir);
    }
    const orch = config.infrastructure.orchestrator;
    if (orch === ORCH_KUBERNETES) {
      result = this.mergeFile(result, K8S_PERMISSIONS_FILE, templatesDir);
    } else if (orch === ORCH_DOCKER_COMPOSE) {
      result = this.mergeFile(result, COMPOSE_PERMISSIONS_FILE, templatesDir);
    }
    return result;
  }

  private collectData(
    config: ProjectConfig,
    templatesDir: string,
    result: string[],
  ): string[] {
    const dbKey = getDatabaseSettingsKey(config.data.database.name);
    if (dbKey !== "") {
      result = this.mergeFile(result, `${dbKey}.json`, templatesDir);
    }
    const cacheKey = getCacheSettingsKey(config.data.cache.name);
    if (cacheKey !== "") {
      result = this.mergeFile(result, `${cacheKey}.json`, templatesDir);
    }
    return result;
  }

  private mergeFile(
    base: string[],
    filename: string,
    templatesDir: string,
  ): string[] {
    const filePath = path.join(templatesDir, filename);
    if (!fs.existsSync(filePath)) return base;
    const overlay = readJsonArray(filePath);
    return mergeJsonArrays(base, overlay);
  }

  private writeSettings(
    outputDir: string,
    settings: SettingsJson,
  ): string {
    const dest = path.join(outputDir, SETTINGS_FILENAME);
    const content = JSON.stringify(settings, null, JSON_INDENT) + "\n";
    fs.writeFileSync(dest, content, "utf-8");
    return dest;
  }

  private writeSettingsLocal(outputDir: string): string {
    const dest = path.join(outputDir, SETTINGS_LOCAL_FILENAME);
    const localSettings = { permissions: { allow: [] as string[] } };
    const content = JSON.stringify(localSettings, null, JSON_INDENT) + "\n";
    fs.writeFileSync(dest, content, "utf-8");
    return dest;
  }
}
