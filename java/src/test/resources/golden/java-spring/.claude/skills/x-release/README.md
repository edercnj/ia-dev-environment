# x-release

> Orchestrates complete release flow using Git Flow release branches: version bump (auto-detect or explicit), release branch creation from develop, version file updates, changelog generation, release commit, dual merge (main + develop), git tag on main, and cleanup. Supports hotfix releases from main and dry-run mode.

| | |
|---|---|
| **Category** | Git/Release |
| **Invocation** | `/x-release [major\|minor\|patch\|version] [--dry-run] [--skip-tests] [--no-publish] [--hotfix]` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Automates the end-to-end release process following Git Flow. Creates a release branch from `develop`, bumps the version (auto-detected from Conventional Commits or explicitly specified), updates version files, delegates changelog generation to `x-release-changelog`, performs dual merge into `main` and back to `develop`, creates an annotated git tag on `main`, and cleans up the release branch. Supports hotfix releases from `main` (PATCH only) and a dry-run mode for safe previewing.

## Usage

```
/x-release
/x-release minor
/x-release 2.1.0
/x-release patch --hotfix
/x-release minor --dry-run
```

## Workflow

1. **Determine** -- Parse argument and calculate target version (auto-detect, increment type, or explicit)
2. **Validate** -- Check pre-conditions (clean working directory, correct branch, tests passing)
3. **Branch** -- Create `release/X.Y.Z` from `develop` (or `hotfix/*` from `main`)
4. **Update** -- Update version in project files (strip SNAPSHOT suffix for Java)
5. **Changelog** -- Delegate to `x-release-changelog` for CHANGELOG.md generation
6. **Merge + Tag** -- Merge to `main` with `--no-ff`, create annotated tag, merge back to `develop`
7. **Publish + Cleanup** -- Push branches and tag to remote, delete release branch

## Outputs

| Artifact | Path |
|----------|------|
| Updated version file | `pom.xml` / `build.gradle` / `package.json` / `pyproject.toml` / `Cargo.toml` |
| Changelog | `CHANGELOG.md` |
| Git tag | `vX.Y.Z` |

## See Also

- [x-release-changelog](../x-release-changelog/) -- Changelog generation delegated during release
- [x-git-push](../x-git-push/) -- Conventional Commits format used for release commits
