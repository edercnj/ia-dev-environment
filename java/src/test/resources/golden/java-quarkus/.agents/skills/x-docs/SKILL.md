---
name: x-docs
description: "Documentation automation: detects documentation type needed (API, README, ADR, changelog) from code changes, delegates to specialized skills or generates inline. Single entry point for all documentation updates."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill
argument-hint: "[--type api|readme|adr|changelog|all] [--scope path] [--force]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Documentation Automation

## Purpose

Single entry point for generating and updating all project documentation for {{PROJECT_NAME}}. Detects the type of documentation needed from code changes, delegates to specialized skills (`/x-changelog`, `/x-dev-adr-automation`, `/x-dev-arch-update`), or generates API docs and README updates inline.

## Triggers

- `/x-docs` — auto-detect documentation type from `git diff` and update accordingly
- `/x-docs --type api` — generate or update API documentation (OpenAPI/endpoint docs)
- `/x-docs --type readme` — update project README.md with new features/skills/configuration
- `/x-docs --type adr` — delegate to `/x-dev-adr-automation` for ADR generation
- `/x-docs --type changelog` — delegate to `/x-changelog` for changelog generation
- `/x-docs --type all` — process all applicable documentation types
- `/x-docs --type all --force` — regenerate all documentation regardless of change status

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `--type` | Enum | No | Documentation type: `api`, `readme`, `adr`, `changelog`, `all`. Default: auto-detect from `git diff`. |
| `--scope` | String | No | Path to limit change analysis (e.g., `src/main/java/com/example/api/`) |
| `--force` | Flag | No | Regenerate documentation even if no changes detected |

## Workflow

```
1. PARSE      -> Parse arguments (--type, --scope, --force)
2. DETECT     -> Analyze git diff to determine documentation types needed
3. DISPATCH   -> Delegate to specialized skills or generate inline
4. VERIFY     -> Confirm documentation was updated (idempotency check)
5. REPORT     -> Summary of documentation actions taken
```

### Step 1 — Parse Arguments

Parse the invocation arguments:

1. If `--type` is specified, use that type (skip auto-detection)
2. If `--scope` is specified, limit `git diff` to that path
3. If `--force` is specified, regenerate even if docs appear up-to-date

### Step 2 — Detect Documentation Type

When `--type` is NOT specified, analyze changed files to infer documentation type:

```bash
# Get list of changed files (staged + unstaged + untracked)
git diff --name-only HEAD 2>/dev/null || git diff --name-only
git diff --name-only --cached
git ls-files --others --exclude-standard
```

Apply the auto-detection rules:

| File Pattern | Inferred Type |
|-------------|---------------|
| `*Controller*`, `*Resource*`, `*Handler*`, `*Endpoint*`, `*Route*` | `api` |
| `*ADR*`, `*Decision*`, `architecture*`, `*adr*` | `adr` |
| Any commits since last tag (Conventional Commits present) | `changelog` |
| `SKILL.md`, `README*`, `config*`, `setup*`, `*.yaml` (config) | `readme` |
| No matches found | No action needed |

When multiple types are detected, process ALL detected types (same as `--type all` but scoped).

If no changes are detected and `--force` is NOT set:
```
LOG: "No documentation updates needed"
```

### Step 3 — Dispatch Documentation Generation

#### 3A — Changelog (`--type changelog`)

Delegate entirely to the `/x-changelog` skill:

```
Invoke Skill: /x-changelog --unreleased
```

Do NOT generate changelog content inline. The `/x-changelog` skill handles all changelog logic.

#### 3B — ADR (`--type adr`)

Delegate to `/x-dev-adr-automation`:

1. Locate the most recent architecture plan in `plans/`
2. If an architecture plan with mini-ADRs exists:
   ```
   Invoke Skill: /x-dev-adr-automation [architecture-plan-path] [story-id]
   ```
3. If no architecture plan exists, log: "No architecture plan found for ADR generation"

#### 3C — Architecture Documentation

When architectural changes are detected (new components, new integrations, infrastructure changes), delegate to `/x-dev-arch-update`:

```
Invoke Skill: /x-dev-arch-update [story-id or plan-path]
```

This is triggered automatically when `--type all` is specified and architecture plans exist.

#### 3D — API Documentation (`--type api`)

Generate or update API documentation inline:

1. **Detect endpoints** — scan changed files for controller/resource/handler classes:
   ```bash
   # Find controller files with changes
   git diff --name-only | grep -iE '(Controller|Resource|Handler|Endpoint|Route)\.'
   ```

2. **Extract endpoint metadata** — for each changed controller, extract:
   - HTTP method annotations (`@GET`, `@POST`, `@PUT`, `@DELETE`, `@PATCH`)
   - Path mappings (`@Path`, `@RequestMapping`, `@GetMapping`, etc.)
   - Request/response types (DTOs, records)
   - Path parameters, query parameters, request body types
   - Response status codes

3. **Generate/update documentation**:
   - If `docs/api/` directory exists, update endpoint documentation there
   - If OpenAPI/Swagger spec exists (`openapi.yaml`, `openapi.json`, `swagger.yaml`), update it
   - Otherwise, generate an API section in README.md with endpoint table:
     ```markdown
     ## API Endpoints

     | Method | Path | Description | Request Body | Response |
     |--------|------|-------------|-------------|----------|
     | GET | /api/v1/users | List users | — | UserListResponse |
     | POST | /api/v1/users | Create user | CreateUserRequest | UserResponse |
     ```

4. **Idempotency** — before writing, compare generated content with existing. Skip if identical.

#### 3E — README (`--type readme`)

Update the project README.md inline:

1. **Read current README.md** (or create from template if missing)
2. **Detect what changed**:
   - New skills added → update Skills section
   - New configuration options → update Configuration section
   - New features implemented → update Features section
   - New dependencies → update Dependencies/Prerequisites section
3. **Apply updates** — insert or update relevant sections without removing existing content
4. **Idempotency** — compare before writing, skip if no changes needed

#### 3F — All (`--type all`)

Process all documentation types in parallel where possible:

1. Run `git diff` analysis once (shared across all types)
2. For each applicable type, dispatch as described above
3. Skip types that have no applicable changes (unless `--force`)
4. Report results for each type processed

### Step 4 — Verify (Idempotency Check)

After generation, verify:

1. No duplicate content was introduced
2. Files were actually modified (or correctly skipped if up-to-date)
3. Generated documentation is syntactically valid (valid Markdown, valid YAML for OpenAPI)

### Step 5 — Report

```
Documentation update complete:
  Types processed: {list of types}
  Files updated:
    - {file_path_1} ({type})
    - {file_path_2} ({type})
  Delegated to:
    - /x-changelog (changelog)
    - /x-dev-adr-automation (adr)
  Skipped: {types skipped and reason}
  Duration: {time}
```

## Error Handling

| Scenario | Action |
|----------|--------|
| No git repository found | Report error: "Not a git repository" |
| No changes detected (no --force) | Log: "No documentation updates needed" |
| Delegated skill not available | Log warning, skip that type, continue with others |
| Target file is read-only | Report error for that file, continue with others |
| OpenAPI spec has invalid format | Log warning, generate fresh documentation instead of updating |
| README.md has unexpected structure | Append new sections at the end with warning |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `/x-changelog` | delegates-to | Changelog generation is fully delegated |
| `/x-dev-adr-automation` | delegates-to | ADR generation from architecture plans |
| `/x-dev-arch-update` | delegates-to | Architecture document updates |
| `/x-dev-lifecycle` | called-by | Phase 3 (Documentation) of the lifecycle |
| `/x-git-push` | preceded-by | Run after code changes, before committing |
