/**
 * GithubMcpAssembler -- generates github/copilot-mcp.json with env var validation.
 *
 * Migrated from Python `assembler/github_mcp_assembler.py` (64 lines).
 * Returns {@link AssembleResult} with collected warnings for literal env values.
 *
 * @module
 */
import * as fs from "node:fs";
import * as path from "node:path";
import type { ProjectConfig, McpServerConfig } from "../models.js";
import type { TemplateEngine } from "../template-engine.js";
import type { AssembleResult } from "./rules-assembler.js";

/**
 * Warn if any MCP server env values use literals instead of $VARIABLE references.
 *
 * @param servers - The list of MCP server configurations.
 * @returns Array of warning strings for literal env values.
 */
export function warnLiteralEnvValues(
  servers: readonly McpServerConfig[],
): string[] {
  const warnings: string[] = [];
  for (const server of servers) {
    for (const [key, value] of Object.entries(server.env)) {
      if (value && !value.startsWith("$")) {
        warnings.push(
          `MCP server '${server.id}': env var '${key}' uses literal value instead of $VARIABLE format`,
        );
      }
    }
  }
  return warnings;
}

/**
 * Build the copilot-mcp.json structure from project config.
 *
 * @param config - The project configuration with MCP server definitions.
 * @returns A plain object ready for JSON serialization.
 */
export function buildCopilotMcpDict(
  config: ProjectConfig,
): Record<string, unknown> {
  const mcpServers: Record<string, unknown> = {};
  for (const server of config.mcp.servers) {
    const entry: Record<string, unknown> = { url: server.url };
    if (server.capabilities.length > 0) {
      entry["capabilities"] = [...server.capabilities];
    }
    if (Object.keys(server.env).length > 0) {
      entry["env"] = { ...server.env };
    }
    mcpServers[server.id] = entry;
  }
  return { mcpServers };
}

/** Generates github/copilot-mcp.json if MCP servers are configured. */
export class GithubMcpAssembler {
  /** Generate copilot-mcp.json with env var validation. */
  assemble(
    config: ProjectConfig,
    outputDir: string,
    _resourcesDir: string,
    _engine: TemplateEngine,
  ): AssembleResult {
    if (config.mcp.servers.length === 0) {
      return { files: [], warnings: [] };
    }
    const warnings = warnLiteralEnvValues(config.mcp.servers);
    const githubDir = path.join(outputDir, "github");
    fs.mkdirSync(githubDir, { recursive: true });
    const dict = buildCopilotMcpDict(config);
    const content = JSON.stringify(dict, null, 2) + "\n";
    const dest = path.join(githubDir, "copilot-mcp.json");
    fs.writeFileSync(dest, content, "utf-8");
    return { files: [dest], warnings };
  }
}
