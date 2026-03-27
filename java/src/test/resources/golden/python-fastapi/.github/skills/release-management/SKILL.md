---
name: release-management
description: >
  Knowledge Pack: Release Management -- Semantic versioning, version lifecycle,
  release branching strategies, artifact publishing, signing, hotfix process,
  rollback procedures, and release communication for my-fastapi-service.
---

# Knowledge Pack: Release Management

## Summary

Release management practices for my-fastapi-service using python 3.12 with fastapi.

### Semantic Versioning

- MAJOR.MINOR.PATCH: breaking changes, new features, bug fixes
- Pre-release: alpha < beta < rc < GA (with numeric sub-identifiers)
- Build metadata (+build.123) ignored for precedence; used for traceability
- Version 0.y.z: initial development, no stability guarantees

### Version Lifecycle

- Alpha: feature-incomplete, internal only, no backward-compatibility
- Beta: feature-complete, API may change with notice, external testing
- RC: production-ready candidate, only critical fixes allowed
- GA: stable release, full support commitment
- LTS: extended maintenance, security patches only
- EOL: no further updates, migration guidance provided

### Release Branching Strategies

- Trunk-based: simplicity, CI/CD aligned, requires feature flags (any team size)
- GitFlow: parallel release tracks, complex, merge conflicts (large teams)
- Release branches: isolation, cherry-pick overhead (medium teams)
- Decision by: team size x release frequency x compliance requirements

### Artifact Registry Management

- Maven Central (Java): staging, release, SNAPSHOT repositories
- npm registry (TypeScript): scoped packages, provenance, access tokens
- PyPI (Python): twine upload, trusted publishers, classifiers
- crates.io (Rust): cargo publish, yanking, version requirements
- Container registries: multi-arch manifests, tag conventions, retention policies

### Release Signing & Attestation

- GPG signing: dedicated project key, keyserver publication, CI integration
- Sigstore/cosign: keyless signing, OIDC identity, transparency log
- SLSA provenance: levels 1-4, build attestation, supply chain security
- GitHub Attestations API: build provenance and SBOM attestation

### Hotfix Process

- Cherry-pick from main when fix applies cleanly to release branch
- Dedicated hotfix branch when multiple commits or different implementation needed
- PATCH increment only; full CI pipeline required; focused regression testing

### Rollback Procedures

- Application: revert deployment (fastest), redeploy prior version, git revert + deploy
- Database: expand/contract alignment, tested rollback scripts, never roll back destructive migrations
- Feature flags: instant disable without redeploy (fastest method)
- Decision by: severity x blast radius x time-to-fix

### Release Communication

- Release notes: audience-appropriate, breaking changes highlighted, categorized changes
- Migration guides: step-by-step, automated scripts, before/after examples
- Deprecation: 2 minor version minimum warning, alternatives provided, removal date stated

## References

- [Semantic Versioning 2.0.0](https://semver.org/) -- Version numbering rules and precedence
- [Conventional Commits](https://www.conventionalcommits.org/) -- Commit message format for automated versioning
- [SLSA Framework](https://slsa.dev/) -- Supply chain security levels and provenance
