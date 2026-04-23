# Rule 08 — Release Process

> **Full reference:** Read `knowledge/protocols.md` for detailed release management procedures.
> **Related:** See Rule 09 for the branching model (Git Flow), branch types, and merge direction rules.

## Semantic Versioning (Non-Negotiable)

| Component | Bump When |
|-----------|-----------|
| MAJOR | Breaking changes to public API or contracts |
| MINOR | New features, backward-compatible additions |
| PATCH | Bug fixes, security patches, documentation |

- Version format: `MAJOR.MINOR.PATCH` (e.g., `1.2.3`)
- Pre-release versions: `MAJOR.MINOR.PATCH-rc.N` (e.g., `1.2.3-rc.1`)
- NEVER release breaking changes under a MINOR or PATCH bump

## Conventional Commits (Non-Negotiable)

| Prefix | Purpose | Version Bump |
|--------|---------|--------------|
| `feat:` | New feature | MINOR |
| `fix:` | Bug fix | PATCH |
| `feat!:` / `fix!:` / `BREAKING CHANGE:` | Breaking change | MAJOR |
| `docs:` | Documentation only | None |
| `refactor:` | Code restructuring | None |
| `test:` | Test additions/changes | None |
| `chore:` | Build, CI, tooling | None |
| `perf:` | Performance improvement | PATCH |

- Every commit message MUST follow Conventional Commits format
- Scope is optional but recommended: `feat(auth): add JWT refresh`
- Body and footer are optional; use for context and breaking change descriptions

## CHANGELOG (Non-Negotiable)

- `CHANGELOG.md` MUST be updated with every release
- Format: [Keep a Changelog](https://keepachangelog.com/)
- Sections: Added, Changed, Deprecated, Removed, Fixed, Security
- Unreleased section MUST track changes between releases

## Release Checklist

- [ ] All tests passing on release branch
- [ ] Coverage thresholds met (see Rule 05)
- [ ] CHANGELOG.md updated
- [ ] Version bumped according to SemVer
- [ ] No TODO/FIXME/HACK comments in release scope
- [ ] Security scan clean
- [ ] Rollback plan documented

## Release Branches (Git Flow)

- Release branches (`release/*`) MUST be created from `develop` (see Rule 09)
- Only bug fixes, documentation, and version bumps are allowed on release branches
- After merge to `main`, the release branch MUST be merged back into `develop`
- Hotfix branches (`hotfix/*`) follow the same merge pattern (see Rule 09)

## Rollback Plan

- Every release MUST have a documented rollback procedure
- Database migrations MUST support rollback
- Feature flags SHOULD be used for high-risk deployments

## Forbidden

- Releasing without updating CHANGELOG
- Manual version bumping (use CI/CD automation)
- Skipping release checklist items
- Releasing directly from `main` without a release branch or tag
- Deploying without a rollback plan

> Read `knowledge/protocols.md` for branching strategies, hotfix procedures, and CI/CD pipeline configuration.
