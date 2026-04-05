---
name: x-mcp-recommend
description: >
  Analyzes project tech stack and recommends relevant MCP (Model Context Protocol)
  servers. Auto-detects language, framework, database, cache, and message broker,
  then matches against a built-in catalog with installation instructions.
  Reference: `.github/skills/x-mcp-recommend/SKILL.md`
---

# Skill: MCP Server Recommendations

## Purpose

Analyzes the {{PROJECT_NAME}} tech stack and recommends MCP servers that enhance AI-assisted development.

## Triggers

- `/x-mcp-recommend` -- analyze and recommend
- `/x-mcp-recommend --install` -- recommend and auto-configure

## Workflow

```
1. DETECT     -> Read project config and tech stack
2. MATCH      -> Match against MCP catalog
3. RECOMMEND  -> Generate recommendations
4. CONFIGURE  -> Optionally update config files
```

### Step 1 -- Detect Tech Stack

Read project configuration:
```bash
cat .claude/rules/01-project-identity.md 2>/dev/null
cat setup-config.yaml 2>/dev/null
```

Extract: language ({{LANGUAGE}}), framework ({{FRAMEWORK}}), database, cache, broker, container, orchestrator.

### Step 2 -- MCP Server Catalog

#### Database

| Tech | MCP Server | Package |
|------|------------|---------|
| PostgreSQL | Postgres MCP | `@modelcontextprotocol/server-postgres` |
| MySQL | MySQL MCP | `mysql-mcp-server` |
| MongoDB | MongoDB MCP | `mongodb-mcp-server` |
| SQLite | SQLite MCP | `@modelcontextprotocol/server-sqlite` |
| Redis | Redis MCP | `redis-mcp-server` |

#### DevOps

| Tech | MCP Server | Package |
|------|------------|---------|
| Docker | Docker MCP | `docker-mcp-server` |
| Kubernetes | K8s MCP | `kubernetes-mcp-server` |
| AWS | AWS MCP | `aws-mcp-server` |
| Terraform | Terraform MCP | `terraform-mcp-server` |

#### Productivity

| Tech | MCP Server | Package |
|------|------------|---------|
| GitHub | GitHub MCP | `@modelcontextprotocol/server-github` |
| Slack | Slack MCP | `@modelcontextprotocol/server-slack` |
| Linear | Linear MCP | `linear-mcp-server` |
| Jira | Jira MCP | `jira-mcp-server` |

#### Development Tools

| Tech | MCP Server | Package |
|------|------------|---------|
| Web | Puppeteer MCP | `@modelcontextprotocol/server-puppeteer` |
| Any | Filesystem MCP | `@modelcontextprotocol/server-filesystem` |
| Any | Memory MCP | `@modelcontextprotocol/server-memory` |
| Any | Fetch MCP | `@modelcontextprotocol/server-fetch` |

#### Observability

| Tech | MCP Server | Package |
|------|------------|---------|
| Sentry | Sentry MCP | `sentry-mcp-server` |
| Datadog | Datadog MCP | `datadog-mcp-server` |
| Grafana | Grafana MCP | `grafana-mcp-server` |

### Step 3 -- Recommendations

Priority levels:
- **Essential:** Directly matches primary tech
- **Recommended:** Matches secondary tech
- **Optional:** General productivity

Output format:
```markdown
## MCP Recommendations for {{PROJECT_NAME}}

| # | Server | Priority | Reason |
|---|--------|----------|--------|
| 1 | {name} | Essential | {reason} |
| 2 | {name} | Recommended | {reason} |
```

### Step 4 -- Configure (Optional)

With `--install`, update config files:

Claude Code (`settings.local.json`):
```json
"{server}": {
  "command": "npx",
  "args": ["-y", "{package}"]
}
```

GitHub Copilot (`copilot-mcp.json`):
```json
{"servers": {"{server}": {"command": "npx", "args": ["-y", "{package}"]}}}
```

## Error Handling

| Scenario | Action |
|----------|--------|
| No config found | File-based detection |
| No stack detected | Recommend general tools |
| Config parse error | Create new, warn about backup |
