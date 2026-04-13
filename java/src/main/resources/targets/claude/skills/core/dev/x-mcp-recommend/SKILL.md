---
name: x-mcp-recommend
description: "Analyzes project tech stack and recommends relevant MCP (Model Context Protocol) servers. Auto-detects language, framework, database, cache, and message broker from project config, then matches against a built-in catalog of MCP servers with installation instructions."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob
argument-hint: "[--install]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: MCP Server Recommendations

## Purpose

Analyzes the {{PROJECT_NAME}} tech stack and recommends MCP (Model Context Protocol) servers that enhance AI-assisted development. Auto-detects project configuration and matches against a curated catalog of MCP servers.

## Triggers

- `/x-mcp-recommend` — analyze and recommend MCP servers
- `/x-mcp-recommend --install` — recommend and auto-configure

## Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `--install` | Flag | false | Auto-configure recommended MCP servers into config files |

## Workflow

```
1. DETECT     -> Read project config and tech stack
2. MATCH      -> Match tech stack against MCP catalog
3. RECOMMEND  -> Generate recommendations with rationale
4. CONFIGURE  -> Optionally update MCP config files
```

### Step 1 — Detect Tech Stack

Read project configuration to identify:

```bash
# Check for project config files
cat .claude/rules/01-project-identity.md 2>/dev/null
cat setup-config.yaml 2>/dev/null
```

Extract from project identity:
- **Language:** {{LANGUAGE}}
- **Framework:** {{FRAMEWORK}}
- **Database:** Check config for database type
- **Cache:** Check config for cache type
- **Message Broker:** Check config for message broker
- **Container:** Check config for container runtime
- **Orchestrator:** Check config for orchestration platform
- **Observability:** Check config for monitoring tools
- **CI/CD:** Check for CI config files (.github/workflows, Jenkinsfile, etc.)
- **External Services:** Check for API integrations

Also detect from project files:
```bash
# Database indicators
ls -la **/migrations/ docker-compose.yml 2>/dev/null
grep -r "postgresql\|mysql\|mongodb\|redis" docker-compose.yml 2>/dev/null

# Service indicators
ls .github/workflows/ 2>/dev/null
ls Dockerfile 2>/dev/null
```

### Step 2 — Match Against MCP Catalog

#### Database Servers

| Tech Stack | MCP Server | Package | Use Case |
|-----------|------------|---------|----------|
| PostgreSQL | Postgres MCP | `@modelcontextprotocol/server-postgres` | Query execution, schema inspection |
| MySQL | MySQL MCP | `mysql-mcp-server` | Query execution, schema inspection |
| MongoDB | MongoDB MCP | `mongodb-mcp-server` | Document queries, collection management |
| SQLite | SQLite MCP | `@modelcontextprotocol/server-sqlite` | Local database operations |
| Redis | Redis MCP | `redis-mcp-server` | Cache inspection, key management |

#### DevOps and Infrastructure

| Tech Stack | MCP Server | Package | Use Case |
|-----------|------------|---------|----------|
| Docker | Docker MCP | `docker-mcp-server` | Container management, logs |
| Kubernetes | Kubernetes MCP | `kubernetes-mcp-server` | Cluster management, pod inspection |
| AWS | AWS MCP | `aws-mcp-server` | AWS service interaction |
| Terraform | Terraform MCP | `terraform-mcp-server` | Infrastructure state, plan preview |

#### Productivity and Collaboration

| Tech Stack | MCP Server | Package | Use Case |
|-----------|------------|---------|----------|
| GitHub | GitHub MCP | `@modelcontextprotocol/server-github` | Issues, PRs, repo management |
| Slack | Slack MCP | `@modelcontextprotocol/server-slack` | Channel messages, notifications |
| Linear | Linear MCP | `linear-mcp-server` | Issue tracking, project management |
| Jira | Jira MCP | `jira-mcp-server` | Issue tracking, sprint management |

#### Development Tools

| Tech Stack | MCP Server | Package | Use Case |
|-----------|------------|---------|----------|
| Any web project | Puppeteer MCP | `@modelcontextprotocol/server-puppeteer` | Browser automation, screenshots |
| Any project | Filesystem MCP | `@modelcontextprotocol/server-filesystem` | Advanced file operations |
| Any project | Memory MCP | `@modelcontextprotocol/server-memory` | Persistent knowledge graph |
| Any project | Fetch MCP | `@modelcontextprotocol/server-fetch` | HTTP requests, API testing |

#### Observability

| Tech Stack | MCP Server | Package | Use Case |
|-----------|------------|---------|----------|
| Sentry | Sentry MCP | `sentry-mcp-server` | Error tracking, issue analysis |
| Datadog | Datadog MCP | `datadog-mcp-server` | Metrics, logs, traces |
| Grafana | Grafana MCP | `grafana-mcp-server` | Dashboard queries, alerts |

### Step 3 — Generate Recommendations

For each matched MCP server, output:

```markdown
## MCP Server Recommendations for {{PROJECT_NAME}}

### Detected Tech Stack
- Language: {{LANGUAGE}}
- Framework: {{FRAMEWORK}}
- Database: {detected}
- Cache: {detected}
- Container: {detected}

### Recommended MCP Servers

#### 1. {Server Name} — {Priority: Essential/Recommended/Optional}

**Why:** {Rationale based on project tech stack}
**Package:** `{package_name}`

Install (Claude Code):
```json
// Add to .claude/settings.local.json under "mcpServers"
"{server-name}": {
  "command": "npx",
  "args": ["-y", "{package_name}"],
  "env": {
    "KEY": "value"
  }
}
```

### Summary

| # | Server | Priority | Reason |
|---|--------|----------|--------|
| 1 | {name} | Essential | {reason} |
| 2 | {name} | Recommended | {reason} |
| 3 | {name} | Optional | {reason} |
```

**Priority rules:**
- **Essential:** Directly matches primary tech (database, framework)
- **Recommended:** Matches secondary tech (CI/CD, monitoring)
- **Optional:** General productivity enhancement

### Step 4 — Configure (Optional)

If user requests `--install`:

1. Read existing MCP config:
   ```bash
   cat .claude/settings.local.json 2>/dev/null
   ```

2. Merge recommended servers into config (do not overwrite existing entries)

3. Write updated config files

4. Report changes:
   ```
   Updated .claude/settings.local.json: added {N} servers
   ```

## Error Handling

| Scenario | Action |
|----------|--------|
| No project config found | Use file-based detection (Dockerfile, pom.xml, package.json, etc.) |
| MCP server package not found | Mark as "Verify availability" in recommendations |
| Config file parse error | Create new config file, warn about backup |
| No tech stack detected | Recommend general-purpose servers only (filesystem, fetch, memory) |
