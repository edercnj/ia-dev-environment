---
name: release-management
description: "Release management practices: semantic versioning, version lifecycle (alpha/beta/RC/GA/LTS/EOL), release branching strategies, artifact registry management, release signing and attestation, hotfix process, rollback procedures, and release communication."
user-invocable: false
allowed-tools:
  - Read
  - Grep
  - Glob
---

# Knowledge Pack: Release Management

## Purpose

Provides comprehensive release management practices for {{LANGUAGE}} {{FRAMEWORK}} projects using {{BUILD_TOOL}}, enabling consistent versioning, artifact publishing, release signing, hotfix management, and rollback procedures. Covers the full release lifecycle from initial development through end-of-life.

## Semantic Versioning

### MAJOR.MINOR.PATCH Rules

Semantic Versioning (SemVer) uses a three-part version number: `MAJOR.MINOR.PATCH`.

| Component | Increment When | Example |
|-----------|---------------|---------|
| MAJOR | Incompatible API changes | 1.0.0 -> 2.0.0 |
| MINOR | Backward-compatible new functionality | 1.0.0 -> 1.1.0 |
| PATCH | Backward-compatible bug fixes | 1.0.0 -> 1.0.1 |

### Pre-Release Identifiers

Pre-release versions append a hyphen and identifiers after the patch version.

- Precedence: `1.0.0-alpha < 1.0.0-alpha.1 < 1.0.0-beta < 1.0.0-beta.2 < 1.0.0-rc.1 < 1.0.0`
- Numeric identifiers are compared as integers; alphanumeric identifiers are compared lexically
- A pre-release version has lower precedence than the associated normal version

### Build Metadata

Build metadata is appended with a `+` sign: `1.0.0+build.123`, `1.0.0-beta+exp.sha.5114f85`.

- Build metadata MUST be ignored when determining version precedence
- Use for CI build numbers, commit SHAs, or timestamps
- Never use build metadata for version ordering decisions

### Version 0.y.z Semantics

- Version `0.y.z` indicates initial development; anything MAY change at any time
- The public API SHOULD NOT be considered stable
- Version `1.0.0` defines the first stable public API
- Increment rules still apply within `0.y.z` but without backward-compatibility guarantees

## Version Lifecycle

### Phase Definitions

| Phase | Description | Stability | Support |
|-------|------------|-----------|---------|
| Alpha | Feature-incomplete, internal testing only | No backward-compatibility guarantee | Internal only |
| Beta | Feature-complete, external testing | API may change with notice | Limited external |
| RC | Production-ready candidate | Only critical fixes allowed | Full testing |
| GA | Stable release | Full backward-compatibility | Full support |
| LTS | Extended maintenance window | Security patches only | Extended support |
| EOL | No further updates | N/A | Migration guidance only |

### Transition Criteria

| Transition | Required Criteria |
|-----------|------------------|
| Alpha -> Beta | All planned features implemented; unit test coverage >= 80% |
| Beta -> RC | API frozen; integration tests passing; no known critical defects |
| RC -> GA | Zero critical defects; performance benchmarks met; stakeholder sign-off |
| GA -> LTS | Extended support declared; maintenance team assigned |
| GA/LTS -> EOL | Support window expired; migration path documented |
| RC -> Beta | Critical defect found requiring API change |
| Beta -> Alpha | Major design change required |

## Release Branching Strategies

> **Cross-reference:** See Rule 09 (`rules/09-branching-model.md`) for mandatory branching conventions and branch naming rules.

### Strategy Comparison

| Strategy | Pros | Cons | Team Size | Recommendation |
|----------|------|------|-----------|----------------|
| GitFlow (Recommended) | Parallel release tracks, clear separation, formal process | Complexity, merge conflicts | Any | **Default for all projects** |
| Trunk-based | Simplicity, CI/CD alignment, fast feedback | Requires feature flags, discipline | Small (1-4) | Alternative for CI/CD-focused teams |
| Release branches | Isolation, focused stabilization | Cherry-pick overhead, divergence risk | Medium (5-10) | Simplified alternative |

### Decision Matrix

GitFlow is the default recommendation. Choose an alternative only when specific criteria are met.

| Scenario | Recommended Strategy |
|----------|---------------------|
| Default for all new projects | GitFlow (default) |
| Large team, scheduled releases | GitFlow (default) |
| Compliance/audit requirements | GitFlow (mandatory) |
| Open-source projects | GitFlow |
| Small team, continuous deployment | Trunk-based (alternative) |
| Rapid prototyping | Trunk-based (alternative) |
| Medium team, periodic stabilization | Release branches (alternative) |

See `references/release-branching-guide.md` for detailed decision flowchart.

## Artifact Registry Management

### Registry Per Language

| Language | Registry | Publish Command | Snapshot Support |
|----------|----------|----------------|-----------------|
| Java | Maven Central (Sonatype OSSRH) | `mvn deploy` | Yes (SNAPSHOT) |
| TypeScript | npm registry | `npm publish` | No (use pre-release tags) |
| Python | PyPI | `twine upload` | No (use pre-release classifiers) |
| Rust | crates.io | `cargo publish` | No (use pre-release identifiers) |
| Go | Go module proxy | `git tag` | No (use v0.x or pre-release) |

### Container Registry

| Registry | Use Case | Tag Convention |
|----------|----------|---------------|
| Docker Hub | Public images | `image:version`, `image:latest` |
| GitHub Container Registry (GHCR) | GitHub-integrated workflows | `ghcr.io/org/image:version` |
| Amazon ECR | AWS deployments | `account.dkr.ecr.region.amazonaws.com/image:version` |

- Multi-arch manifests: build for `linux/amd64` and `linux/arm64` minimum
- Tag conventions: `v1.2.3`, `v1.2`, `v1`, `latest` (mutable), `sha-abc123` (immutable)
- Retention: keep last 10 versions minimum; time-based expiry for pre-release tags

### Publishing Patterns

| Pattern | Description | When to Use |
|---------|------------|-------------|
| CI-triggered | Automatic publish on tag push | High-trust CI, frequent releases |
| Manual gated | CI builds, human approves publish | Compliance requirements |
| Hybrid | CI publishes snapshots, manual for releases | Development velocity + release control |

See `references/artifact-publishing-matrix.md` for complete registry commands per language.

## Release Signing & Attestation

### GPG Signing

- Generate a dedicated signing key for the project (not personal)
- Store private key in CI secrets with restricted access
- Publish public key to keyserver for verification
- Verify signatures before deploying artifacts

### Sigstore/Cosign

- Keyless signing using OIDC identity from CI provider
- Transparency log records all signing events (immutable audit trail)
- No key management overhead; identity tied to CI workflow
- Verify: `cosign verify --certificate-oidc-issuer <issuer> <image>`

### SLSA Provenance

| Level | Requirements |
|-------|-------------|
| SLSA 1 | Build process documented; provenance exists |
| SLSA 2 | Hosted build service; authenticated provenance |
| SLSA 3 | Hardened build platform; non-forgeable provenance |
| SLSA 4 | Hermetic, reproducible builds; two-person review |

### GitHub Attestations

- Use `actions/attest-build-provenance` for SLSA provenance
- Use `actions/attest-sbom` for software bill of materials
- Verify with `gh attestation verify <artifact>`

## Hotfix Process

### When to Cherry-Pick

- Single commit fix that applies cleanly to release branch
- Fix is already merged and tested on main
- Release branch is still actively maintained

### When to Use Dedicated Hotfix Branch

- Fix requires multiple commits or adaptation for older code
- Fix needs different implementation on release branch vs main
- Multiple release branches need the same fix

### Version Bumping Rules

- Hotfixes MUST increment PATCH version only (e.g., 1.2.3 -> 1.2.4)
- NEVER skip versions; sequential PATCH increments required
- Tag the hotfix release immediately after merge

### Validation Requirements

- Focused regression tests on affected area
- Full CI pipeline execution (not abbreviated)
- Smoke tests against staging environment
- Verify fix does not introduce new issues in adjacent functionality

## Rollback Procedures

### Application Rollback

| Method | Speed | Risk | When to Use |
|--------|-------|------|------------|
| Revert deployment | Fast (minutes) | Low | Bad deploy, prior version known good |
| Redeploy prior version | Fast (minutes) | Low | Current version has critical bug |
| Git revert + deploy | Medium (30 min+) | Medium | Need audit trail of rollback |

### Database Rollback Coordination

- Use expand/contract pattern: new code works with both old and new schema
- Migration rollback scripts MUST be tested before deployment
- Never roll back destructive migrations (column drops, data transforms)
- Coordinate application rollback with database state

### Feature Flag Rollback

- Instant disable without redeploy (fastest rollback method)
- Granular control: roll back specific features, not entire release
- Requires feature flag infrastructure (LaunchDarkly, Unleash, custom)
- Clean up flags within 30 days to prevent flag debt

### Rollback Decision Criteria

| Factor | Rollback | Fix Forward |
|--------|----------|-------------|
| Severity | Critical (data loss, security) | Minor (cosmetic, non-blocking) |
| Blast radius | Wide (all users affected) | Narrow (specific flow only) |
| Time to fix | > 2 hours | < 30 minutes |
| Data impact | Data corruption possible | No data impact |

See `references/rollback-decision-tree.md` for detailed decision flowchart.

## Release Communication

### Release Notes

- Target audience: end users (not developers)
- Highlight breaking changes prominently at the top
- Group changes by category: Added, Changed, Deprecated, Removed, Fixed, Security
- Include migration instructions for breaking changes

### Migration Guides

- Step-by-step upgrade instructions from version N-1 to N
- Automated migration scripts where possible (codemods, database migrations)
- Before/after code examples for API changes
- Estimated migration time and complexity rating

### Deprecation Notices

- Minimum warning period: 2 minor versions before removal
- Provide alternative API or migration path
- Include removal target version in deprecation message
- Log warnings when deprecated features are used at runtime

### Breaking Change Announcements

- Pre-release warning: announce in beta release notes
- Impact assessment: which users/integrations are affected
- Timeline: deprecation -> last supported version -> removal version
- Support channel for migration questions

## Quick Reference

See `references/release-branching-guide.md` for branching strategy decision matrix, `references/artifact-publishing-matrix.md` for registry commands per language, and `references/rollback-decision-tree.md` for rollback vs fix-forward decisions.

## Detailed References

Read these files for comprehensive release management guidance:

| Reference | Content |
|-----------|---------|
| `references/release-branching-guide.md` | Decision matrix for selecting branching strategy based on team size, release frequency, and compliance requirements |
| `references/artifact-publishing-matrix.md` | Registry configuration and publish commands per language with CI integration steps |
| `references/rollback-decision-tree.md` | Flowchart for rollback vs fix-forward decision based on severity, blast radius, and time-to-fix |
