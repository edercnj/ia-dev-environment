---
name: x-changelog
description: "Generates CHANGELOG.md from Conventional Commits history. Parses git log, groups by commit type, maps to Keep a Changelog sections (Added, Changed, Fixed, etc.), and performs incremental updates preserving existing entries."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob
argument-hint: "[version-tag | --unreleased | --full]"
context-budget: light
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Changelog Generator

## Purpose

Generates or updates `CHANGELOG.md` for {{PROJECT_NAME}} following the [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) specification, automatically parsing Conventional Commits from git history.

## Triggers

- `/x-changelog` — generate changelog for unreleased changes
- `/x-changelog v1.2.0` — generate changelog for version v1.2.0
- `/x-changelog --unreleased` — only unreleased changes since last tag
- `/x-changelog --full` — regenerate entire changelog from all tags

## Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| Version tag | No | Target version (e.g., `v1.2.0`). Defaults to unreleased. |
| `--unreleased` | No | Generate only unreleased changes since last tag |
| `--full` | No | Regenerate entire changelog from all tags |

## Workflow

```
1. DETECT     -> Determine version range from tags
2. PARSE      -> Extract commits in range, parse Conventional Commits
3. GROUP      -> Map commit types to Keep a Changelog sections
4. GENERATE   -> Create or update CHANGELOG.md (incremental)
5. REPORT     -> Summary of entries added
```

### Step 1 — Detect Version Range

```bash
# List all version tags, sorted by version
git tag --list 'v*' --sort=-v:refname

# Determine range based on argument
# If version tag given: previous_tag..version_tag
# If --unreleased: latest_tag..HEAD
# If --full: process all tags
# If no argument: latest_tag..HEAD (same as --unreleased)
```

Determine previous tag:
```bash
# Get the tag before the target version
git describe --tags --abbrev=0 HEAD~1 2>/dev/null || echo "INITIAL"
```

### Step 2 — Parse Commits

```bash
# Extract commits in range with full metadata
git log {previous_tag}..{target} --format="%H|%s|%b|%an|%ai" --no-merges
```

Parse each commit subject as Conventional Commit:
```
<type>(<scope>): <description>
```

Extract:
- **type**: feat, fix, refactor, perf, docs, test, chore, ci, build, style
- **scope**: optional module/area
- **description**: commit summary
- **breaking**: presence of `BREAKING CHANGE:` in body or `!` after type
- **body**: additional context (multi-line)

### Step 3 — Map to Keep a Changelog Sections

| Commit Type | Changelog Section | Notes |
|-------------|------------------|-------|
| `feat` | **Added** | New features |
| `fix` | **Fixed** | Bug fixes |
| `refactor` | **Changed** | Code changes that neither fix a bug nor add a feature |
| `perf` | **Changed** | Performance improvements |
| `docs` | **Documentation** | Non-standard section, optional |
| `chore` | **Maintenance** | Non-standard section, optional |
| `build`, `ci` | _(skip)_ | Not user-facing |
| `test` | _(skip)_ | Not user-facing |
| `style` | _(skip)_ | Not user-facing |
| `BREAKING CHANGE` | **Breaking Changes** | Highlighted at top of version |
| `deprecate` | **Deprecated** | Deprecated features |
| `revert` | **Removed** | Reverted features |

### Step 4 — Generate CHANGELOG.md

**If CHANGELOG.md does not exist**, create it:

```markdown
# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- {feat entries}

### Changed
- {refactor/perf entries}

### Fixed
- {fix entries}
```

**If CHANGELOG.md exists**, insert new version section:

1. Read existing CHANGELOG.md
2. Find insertion point (after `## [Unreleased]` section or after header)
3. Insert new version section with date:
   ```markdown
   ## [{version}] - YYYY-MM-DD
   ```
4. Preserve all existing entries below
5. Update `[Unreleased]` section (clear entries that moved to the new version)

**Entry format:**
```markdown
- {description} ([#{PR_number}]({PR_url})) — if PR linkable
- {description} ({commit_hash_short}) — if no PR
```

**Grouping within version:**
- Only include sections that have entries (do not add empty sections)
- Order: Breaking Changes > Added > Changed > Deprecated > Removed > Fixed > Security

### Step 5 — Report

```
CHANGELOG.md updated:
  Version: {version or Unreleased}
  Entries: {count}
  Sections: Added ({n}), Changed ({n}), Fixed ({n})
  Range: {from_tag}..{to_ref}
```

## Version Link References

At the bottom of the CHANGELOG, maintain comparison links:

```markdown
[unreleased]: https://github.com/{owner}/{repo}/compare/{latest_tag}...HEAD
[1.2.0]: https://github.com/{owner}/{repo}/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/{owner}/{repo}/compare/v1.0.0...v1.1.0
```

## Error Handling

| Scenario | Action |
|----------|--------|
| No tags found | Use all commits from initial commit |
| No Conventional Commits in range | Report "No conventional commits found in range" |
| Commit doesn't follow convention | Skip with warning |
| CHANGELOG.md has unexpected format | Append new section at the end with warning |
| Version tag already in CHANGELOG | Skip with "Version already documented" |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-release` | called-by | Invoked during release Step 5 for changelog generation |
| `x-git-push` | reads | Parses Conventional Commits format from git log |
