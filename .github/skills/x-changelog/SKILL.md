---
name: x-changelog
description: >
  Generates CHANGELOG.md from Conventional Commits history. Parses git log,
  groups by commit type, maps to Keep a Changelog sections (Added, Changed,
  Fixed, etc.), and performs incremental updates preserving existing entries.
  Reference: `.github/skills/x-changelog/SKILL.md`
---

# Skill: Changelog Generator

## Purpose

Generates or updates `CHANGELOG.md` for {{PROJECT_NAME}} following the [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) specification, automatically parsing Conventional Commits from git history.

## Triggers

- `/x-changelog` -- generate changelog for unreleased changes
- `/x-changelog v1.2.0` -- generate changelog for version v1.2.0
- `/x-changelog --unreleased` -- only unreleased changes since last tag
- `/x-changelog --full` -- regenerate entire changelog from all tags

## Workflow

```
1. DETECT     -> Determine version range from tags
2. PARSE      -> Extract commits, parse Conventional Commits
3. GROUP      -> Map commit types to Keep a Changelog sections
4. GENERATE   -> Create or update CHANGELOG.md (incremental)
5. REPORT     -> Summary of entries added
```

### Step 1 -- Detect Version Range

```bash
# List version tags
git tag --list 'v*' --sort=-v:refname

# Determine range
# version tag: previous_tag..version_tag
# --unreleased: latest_tag..HEAD
# --full: process all tags
```

### Step 2 -- Parse Commits

```bash
git log {range} --format="%H|%s|%b|%an|%ai" --no-merges
```

Parse format: `<type>(<scope>): <description>`

### Step 3 -- Map to Changelog Sections

| Commit Type | Changelog Section |
|-------------|------------------|
| `feat` | **Added** |
| `fix` | **Fixed** |
| `refactor` | **Changed** |
| `perf` | **Changed** |
| `BREAKING CHANGE` | **Breaking Changes** |
| `deprecate` | **Deprecated** |
| `revert` | **Removed** |
| `docs` | **Documentation** |
| `build`, `ci`, `test`, `style` | _(skip — not user-facing)_ |

### Step 4 -- Generate CHANGELOG.md

**New file format:**

```markdown
# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- {feat entries}

### Fixed
- {fix entries}
```

**Existing file:** Insert new version after header, preserve existing entries.

**Entry format:**
```
- {description} ({short_hash})
```

**Section order:** Breaking Changes > Added > Changed > Deprecated > Removed > Fixed > Security

### Step 5 -- Report

```
CHANGELOG.md updated:
  Version: {version or Unreleased}
  Entries: {count}
  Range: {from}..{to}
```

## Error Handling

| Scenario | Action |
|----------|--------|
| No tags found | Use all commits from initial |
| No conventional commits | Report "No conventional commits found" |
| Version already documented | Skip with message |
