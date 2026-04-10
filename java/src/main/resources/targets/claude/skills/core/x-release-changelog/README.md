# x-release-changelog

> Generates CHANGELOG.md from Conventional Commits history. Parses git log, groups by commit type, maps to Keep a Changelog sections (Added, Changed, Fixed, etc.), and performs incremental updates preserving existing entries.

| | |
|---|---|
| **Category** | Git/Release |
| **Invocation** | `/x-release-changelog [version-tag \| --unreleased]` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Automates changelog generation by parsing the git history for Conventional Commits and organizing them into Keep a Changelog format. Supports generating entries for a specific version tag, unreleased changes since the last tag, or a full regeneration from all tags. Performs incremental updates that preserve existing CHANGELOG.md entries.

## Usage

```
/x-release-changelog
/x-release-changelog v1.2.0
/x-release-changelog --unreleased
/x-release-changelog --full
```

## Workflow

1. **Detect** -- Determine version range from git tags
2. **Parse** -- Extract commits in range and parse Conventional Commits format
3. **Group** -- Map commit types to Keep a Changelog sections (Added, Changed, Fixed, etc.)
4. **Generate** -- Create or incrementally update CHANGELOG.md
5. **Report** -- Summary of entries added per section

## Outputs

| Artifact | Path |
|----------|------|
| Changelog | `CHANGELOG.md` |

## See Also

- [x-release](../x-release/) -- Orchestrates the complete release flow including changelog generation
- [x-git-push](../x-git-push/) -- Conventional Commits format used as changelog input
