---
name: x-setup-dev-environment
description: >
  Validates and configures local development environment: detects project stack,
  checks prerequisites, verifies versions, validates IDE config, tests database
  connectivity, runs initial build, and reports status with fix suggestions.
  Reference: `.github/skills/x-setup-dev-environment/SKILL.md`
---

# Skill: Setup Dev Environment

## Purpose

Validates and configures the local development environment for {{PROJECT_NAME}}, detecting the project stack, checking prerequisites, verifying versions, validating IDE configuration, testing database connectivity, running the initial build, and reporting status with fix suggestions.

## Modes

| Mode | Description |
|------|-------------|
| `--check-only` | Reports status only, does not modify anything (default) |
| `--fix` | Attempts to correct problems found (non-destructive) |

## Workflow

```
1. DETECT     -> Detect project stack from config files
2. CHECK      -> Check prerequisites (runtime, build tool, Docker, DB client)
3. VERIFY     -> Verify installed versions against required versions
4. IDE        -> Check IDE configuration (.editorconfig, formatters, linters)
5. DATABASE   -> Verify database connectivity (skip if database=none)
6. BUILD      -> Run initial build to validate dependency resolution
7. REPORT     -> Generate status report with PASS/FAIL/WARN per check
```

## Report Structure

| Check | Status | Description |
|-------|--------|-------------|
| Language Runtime | PASS/FAIL/WARN | Presence and version of runtime |
| Build Tool | PASS/FAIL/WARN | Presence and version of build tool |
| Docker | PASS/FAIL/SKIP | Presence (skip if container=none) |
| Database Client | PASS/FAIL/SKIP | Presence (skip if database=none) |
| IDE Configuration | PASS/WARN | .editorconfig, formatter configs |
| Database Connectivity | PASS/FAIL/SKIP | Connection to local DB |
| Initial Build | PASS/FAIL | Full build compiles successfully |
