/**
 * Shared constants and helpers for Codex assemblers.
 *
 * Centralizes model defaults, policy derivation, hooks detection,
 * and MCP server mapping used by both CodexAgentsMdAssembler and
 * CodexConfigAssembler.
 *
 * @module
 */
import * as fs from "node:fs";
import type { ProjectConfig } from "../models.js";

/** Default Codex model for cost/performance balance. */
export const DEFAULT_MODEL = "o4-mini";

/** Approval policy when hooks are present. */
export const POLICY_ON_REQUEST = "on-request";

/** Approval policy when no hooks are detected. */
export const POLICY_UNTRUSTED = "untrusted";

/** Default sandbox mode — allows editing project files. */
export const SANDBOX_WORKSPACE_WRITE = "workspace-write";

/**
 * Check if a path is an accessible directory.
 *
 * @param dirPath - Absolute path to check.
 * @returns true if path exists and is a directory.
 */
export function isAccessibleDirectory(dirPath: string): boolean {
  try {
    return fs.statSync(dirPath).isDirectory();
  } catch {
    return false;
  }
}

/**
 * Detect if hooks exist in the given directory.
 *
 * @param hooksDir - Absolute path to the hooks directory.
 * @returns true if directory exists and contains at least 1 entry.
 */
export function detectHooks(hooksDir: string): boolean {
  if (!isAccessibleDirectory(hooksDir)) return false;
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

/** MCP server context ready for template rendering. */
export interface McpServerContext {
  readonly id: string;
  readonly command: string[];
  readonly env: Record<string, string> | null;
}

/**
 * Map MCP server configurations to template-ready objects.
 *
 * @param config - Project configuration.
 * @returns Array of MCP server contexts for template rendering.
 */
export function mapMcpServers(
  config: ProjectConfig,
): McpServerContext[] {
  return config.mcp.servers.map((s) => ({
    id: s.id,
    command: s.url ? s.url.split(/\s+/) : [],
    env: Object.keys(s.env).length > 0 ? { ...s.env } : null,
  }));
}
