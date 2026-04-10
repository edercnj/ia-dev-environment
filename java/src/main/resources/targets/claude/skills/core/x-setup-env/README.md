# x-setup-dev-environment

> Validate and configure local development environment: detect stack, check prerequisites, verify versions, validate IDE config, test database connectivity, run initial build, and report status with fix suggestions.

| | |
|---|---|
| **Category** | Operations |
| **Invocation** | `/x-setup-dev-environment [--check-only] [--fix]` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Validates and configures the local development environment by detecting the project stack from config files, checking prerequisites (runtime, build tool, Docker, database client), verifying installed versions against project requirements, validating IDE configuration, testing database connectivity, and running an initial build. Reports status with PASS/FAIL/WARN per check and offers fix mode for automatic correction of detected issues.

## Usage

```
/x-setup-dev-environment
/x-setup-dev-environment --check-only
/x-setup-dev-environment --fix
```

## Workflow

1. **Detect** -- Detect project stack from config files (pom.xml, package.json, go.mod, etc.)
2. **Check** -- Check prerequisites (runtime, build tool, Docker, database client)
3. **Verify** -- Verify installed versions against required versions
4. **IDE** -- Check IDE configuration (.editorconfig, formatters, linters)
5. **Database** -- Verify database connectivity (skip if database=none)
6. **Build** -- Run initial build to validate dependency resolution
7. **Report** -- Generate status report with PASS/FAIL/WARN per check

## See Also

- [x-ops-troubleshoot](../x-ops-troubleshoot/) -- Diagnoses build failures and environment issues
- [x-mcp-recommend](../x-mcp-recommend/) -- Recommends MCP servers to enhance the development environment
