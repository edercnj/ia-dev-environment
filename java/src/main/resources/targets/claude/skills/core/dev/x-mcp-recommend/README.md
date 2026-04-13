# x-mcp-recommend

> Analyzes project tech stack and recommends relevant MCP (Model Context Protocol) servers. Auto-detects language, framework, database, cache, and message broker from project config, then matches against a built-in catalog of MCP servers with installation instructions.

| | |
|---|---|
| **Category** | Operations |
| **Invocation** | `/x-mcp-recommend` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Analyzes the project tech stack by reading project configuration and identity files, then matches detected technologies against a curated catalog of MCP servers across categories (database, DevOps, productivity, development tools, observability). Generates prioritized recommendations (Essential/Recommended/Optional) with installation instructions for Claude Code. Optionally auto-configures MCP settings files.

## Usage

```
/x-mcp-recommend
/x-mcp-recommend --install
```

## Workflow

1. **Detect** -- Read project config and tech stack (language, framework, database, cache, broker)
2. **Match** -- Match detected technologies against MCP server catalog
3. **Recommend** -- Generate prioritized recommendations with rationale and install instructions
4. **Configure** -- Optionally update MCP config files (--install mode)

## See Also

- [x-setup-env](../x-setup-env/) -- Validates the local development environment
