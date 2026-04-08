# x-docs

> Documentation automation: detects documentation type needed (API, README, ADR, changelog) from code changes, delegates to specialized skills or generates inline. Single entry point for all documentation updates.

| | |
|---|---|
| **Category** | Documentation |
| **Invocation** | `/x-docs [--type api\|readme\|adr\|changelog\|all] [--scope path] [--force]` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Serves as the single entry point for all documentation generation and updates. Analyzes code changes via `git diff` to auto-detect which documentation types need updating, then delegates to specialized skills (`/x-changelog`, `/x-dev-adr-automation`, `/x-dev-arch-update`) or generates API docs and README updates inline. Ensures documentation stays in sync with code changes.

## Usage

```
/x-docs
/x-docs --type api
/x-docs --type readme
/x-docs --type changelog
/x-docs --type adr
/x-docs --type all
/x-docs --type all --force
/x-docs --type api --scope src/main/java/com/example/api/
```

## Flags

| Flag | Description |
|------|-------------|
| `--type` | Documentation type to generate: `api`, `readme`, `adr`, `changelog`, `all`. Omit for auto-detection. |
| `--scope` | Limit change analysis to a specific path |
| `--force` | Regenerate documentation even if no changes detected |

## Auto-Detection

When `--type` is omitted, the skill analyzes `git diff` to infer which documentation types need updating:

| Changed File Pattern | Inferred Type |
|---------------------|---------------|
| `*Controller*`, `*Resource*`, `*Handler*`, `*Endpoint*` | `api` |
| `*ADR*`, `*Decision*`, `architecture*` | `adr` |
| Any commits since last tag | `changelog` |
| `SKILL.md`, `README*`, `config*`, `setup*` | `readme` |

## Delegation

| Type | Delegated To | Method |
|------|-------------|--------|
| `changelog` | `/x-changelog` | Skill tool invocation |
| `adr` | `/x-dev-adr-automation` | Skill tool invocation |
| Architecture | `/x-dev-arch-update` | Skill tool invocation |
| `api` | Inline | Direct generation |
| `readme` | Inline | Direct generation |

## Workflow

1. **Parse** -- Parse arguments (`--type`, `--scope`, `--force`)
2. **Detect** -- Analyze `git diff` to determine documentation types needed
3. **Dispatch** -- Delegate to specialized skills or generate inline
4. **Verify** -- Confirm updates are idempotent (no duplicate content)
5. **Report** -- Summary of documentation actions taken

## Outputs

| Type | Artifact | Path |
|------|----------|------|
| `api` | API documentation | `docs/api/openapi.yaml` or API section in README |
| `readme` | Project README | `README.md` |
| `adr` | Architecture Decision Records | `docs/adr/NNNN-*.md` |
| `changelog` | Changelog | `CHANGELOG.md` |
| Architecture | Architecture document | `steering/service-architecture.md` |

## See Also

- [x-changelog](../x-changelog/) -- Changelog generation from Conventional Commits
- [x-dev-adr-automation](../x-dev-adr-automation/) -- ADR generation from architecture plans
- [x-dev-arch-update](../x-dev-arch-update/) -- Architecture document updates
