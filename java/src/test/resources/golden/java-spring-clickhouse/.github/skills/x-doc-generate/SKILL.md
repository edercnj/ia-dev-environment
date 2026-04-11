---
name: x-doc-generate
description: >
  Documentation automation: detects documentation type needed (API, README,
  ADR, changelog) from code changes, delegates to specialized skills or
  generates inline. Single entry point for all documentation updates.
  Reference: `.github/skills/x-doc-generate/SKILL.md`
---

# Skill: Documentation Automation

## Purpose

Single entry point for generating and updating all project documentation for {{PROJECT_NAME}}. Detects the type of documentation needed from code changes, delegates to specialized skills (`/x-release-changelog`, `/x-adr-generate`, `/x-arch-update`), or generates API docs and README updates inline.

## Triggers

- `/x-doc-generate` -- auto-detect documentation type from `git diff` and update accordingly
- `/x-doc-generate --type api` -- generate or update API documentation
- `/x-doc-generate --type readme` -- update project README.md
- `/x-doc-generate --type adr` -- delegate to `/x-adr-generate`
- `/x-doc-generate --type changelog` -- delegate to `/x-release-changelog`
- `/x-doc-generate --type all` -- process all applicable documentation types
- `/x-doc-generate --type all --force` -- regenerate all documentation regardless of change status

## Workflow

```
1. PARSE      -> Parse arguments (--type, --scope, --force)
2. DETECT     -> Analyze git diff for documentation types needed
3. DISPATCH   -> Delegate to specialized skills or generate inline
4. VERIFY     -> Confirm idempotent updates (no duplicate content)
5. REPORT     -> Summary of documentation actions taken
```

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `--type` | Enum | No | `api`, `readme`, `adr`, `changelog`, `all`. Default: auto-detect. |
| `--scope` | String | No | Path to limit change analysis |
| `--force` | Flag | No | Regenerate even if no changes detected |

## Auto-Detection

| Changed File Pattern | Inferred Type |
|---------------------|---------------|
| `*Controller*`, `*Resource*`, `*Handler*`, `*Endpoint*` | `api` |
| `*ADR*`, `*Decision*`, `architecture*` | `adr` |
| Any commits since last tag | `changelog` |
| `SKILL.md`, `README*`, `config*`, `setup*` | `readme` |

## Delegation

| Type | Delegated To | Method |
|------|-------------|--------|
| `changelog` | `/x-release-changelog` | Skill invocation |
| `adr` | `/x-adr-generate` | Skill invocation |
| Architecture | `/x-arch-update` | Skill invocation |
| `api` | Inline | Direct generation |
| `readme` | Inline | Direct generation |
