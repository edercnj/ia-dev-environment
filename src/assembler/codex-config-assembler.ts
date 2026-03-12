/**
 * CodexConfigAssembler — generates .codex/config.toml from Nunjucks template.
 *
 * Operates in 2 phases:
 * 1. Derivation — computes model, approval_policy, sandbox_mode, maps MCP servers
 * 2. Rendering — passes derived values to config.toml.njk and writes output
 *
 * @module
 */
import * as fs from "node:fs";
import * as path from "node:path";
import type { ProjectConfig } from "../models.js";
import type { TemplateEngine } from "../template-engine.js";
import { buildDefaultContext } from "../template-engine.js";
import type { AssembleResult } from "./rules-assembler.js";

const TEMPLATE_PATH = "codex-templates/config.toml.njk";
const DEFAULT_MODEL = "o4-mini";
const POLICY_ON_REQUEST = "on-request";
const POLICY_UNTRUSTED = "untrusted";
const SANDBOX_WORKSPACE_WRITE = "workspace-write";

/**
 * Detect if hooks exist in the given directory.
 *
 * @param hooksDir - Absolute path to the hooks directory.
 * @returns true if directory exists and contains at least 1 file.
 */
export function detectHooks(hooksDir: string): boolean {
  try {
    if (!fs.statSync(hooksDir).isDirectory()) return false;
  } catch {
    return false;
  }
  return fs.readdirSync(hooksDir).length > 0;
}

/**
 * Derive the approval policy based on hooks presence.
 *
 * @param hasHooks - Whether hooks are detected.
 * @returns "on-request" if hooks exist, "untrusted" otherwise.
 */
export function deriveApprovalPolicy(hasHooks: boolean): string {
  return hasHooks ? POLICY_ON_REQUEST : POLICY_UNTRUSTED;
}

/**
 * Map MCP server configurations to template-ready objects.
 *
 * @param config - Project configuration.
 * @returns Array of objects with id, command array, and env for template rendering.
 */
export function mapMcpServers(
  config: ProjectConfig,
): Array<{
  id: string;
  command: string[];
  env: Record<string, string> | null;
}> {
  return config.mcp.servers.map((s) => ({
    id: s.id,
    command: s.url ? s.url.split(/\s+/) : [],
    env: Object.keys(s.env).length > 0 ? { ...s.env } : null,
  }));
}

/**
 * Build the template context for config.toml rendering.
 *
 * @param config - Project configuration.
 * @param hasHooks - Whether hooks were detected.
 * @returns Template context with all fields needed by config.toml.njk.
 */
export function buildConfigContext(
  config: ProjectConfig,
  hasHooks: boolean,
): Record<string, unknown> {
  const mcpServers = mapMcpServers(config);
  return {
    ...buildDefaultContext(config),
    model: DEFAULT_MODEL,
    approval_policy: deriveApprovalPolicy(hasHooks),
    sandbox_mode: SANDBOX_WORKSPACE_WRITE,
    mcp_servers: mcpServers,
    has_mcp: mcpServers.length > 0,
  };
}

/** Generates .codex/config.toml from Nunjucks template. */
export class CodexConfigAssembler {
  /** Generate .codex/config.toml by deriving config values and rendering template. */
  assemble(
    config: ProjectConfig,
    outputDir: string,
    _resourcesDir: string,
    engine: TemplateEngine,
  ): AssembleResult {
    const warnings: string[] = [];
    const hooksDir = path.join(
      path.dirname(outputDir), ".claude", "hooks",
    );
    const hasHooks = detectHooks(hooksDir);
    const context = buildConfigContext(config, hasHooks);
    let rendered: string;
    try {
      rendered = engine.renderTemplate(TEMPLATE_PATH, context);
    } catch (error: unknown) {
      const message = error instanceof Error
        ? error.message
        : String(error);
      if (message.includes("not found") || message.includes("ENOENT")) {
        warnings.push(`Template not found: ${TEMPLATE_PATH}`);
        return { files: [], warnings };
      }
      throw error;
    }
    fs.mkdirSync(outputDir, { recursive: true });
    const dest = path.join(outputDir, "config.toml");
    fs.writeFileSync(dest, rendered, "utf-8");
    return { files: [dest], warnings };
  }
}
