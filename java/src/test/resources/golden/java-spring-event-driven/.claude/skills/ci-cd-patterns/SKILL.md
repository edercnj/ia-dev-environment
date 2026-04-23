---
name: ci-cd-patterns
description: "CI/CD pipeline patterns: build stages, test parallelization, security scanning, artifact management, environment promotion, approval gates, caching strategies, and language-specific pipeline configurations."
---

# Knowledge Pack: CI/CD Patterns

## Purpose

Provides comprehensive CI/CD pipeline patterns for {{LANGUAGE}} {{FRAMEWORK}} projects using {{BUILD_TOOL}}, enabling automated build, test, security scan, artifact management, and deployment workflows. Covers pipeline stages, language-specific optimizations, caching strategies, environment promotion, and approval gates.

## Quick Reference (always in context)

See `references/github-actions-patterns.md` for GitHub Actions workflow patterns, `references/pipeline-security.md` for pipeline security hardening, and `references/caching-strategies.md` for language-specific caching matrices.

## Detailed References

Read these files for comprehensive CI/CD guidance:

| Reference | Content |
|-----------|---------|
| `references/github-actions-patterns.md` | GitHub Actions workflow structure, job dependencies, matrix builds, reusable workflows, composite actions, environment protection rules, concurrency control, and artifact handling |
| `references/pipeline-security.md` | Pipeline security hardening (SLSA compliance, provenance generation, artifact signing, OIDC authentication, least-privilege permissions, dependency pinning, secret scanning) |
| `references/caching-strategies.md` | Language-specific caching matrices for dependency and build caches, cache key strategies, cache invalidation patterns, and optimization techniques per build tool |

## Pipeline Stages

### Build Stage

- Dependency resolution and caching
- Compilation with incremental build support
- Artifact creation (JAR, binary, container image)
- Build metadata injection (version, commit SHA, timestamp)

### Test Stage

- Unit tests with coverage reporting
- Integration tests with service dependencies
- Parallelization strategies for test suites
- Test result aggregation and reporting

### Security Stage

- Static Application Security Testing (SAST)
- Dependency vulnerability scanning
- Container image scanning
- Secrets detection in source code
- License compliance checking

### Artifact Stage

- Semantic versioning with build metadata
- Registry push (container, package)
- Artifact signing and provenance (SLSA)
- Retention policies for build artifacts

### Deploy Stage

- Environment promotion (dev -> staging -> production)
- Rollback strategies (instant, gradual)
- Canary and blue-green deployment patterns
- Feature flags for progressive delivery

### Post-Deploy Stage

- Smoke tests against deployed environment
- Synthetic monitoring activation
- Rollback triggers based on error rates
- Deployment notification and audit logging

## Cross-Cutting Patterns

### Matrix Builds

- Language version x OS x configuration combinations
- Fail-fast vs. complete matrix execution
- Conditional matrix inclusion based on changed paths

### Caching Strategies

- Dependency cache (package manager lock files)
- Build cache (compiled artifacts, intermediate outputs)
- Test result cache (skip unchanged tests)
- Docker layer caching for container builds

### Artifact Management

- Versioned artifacts with semantic versioning
- Retention policies (keep last N, time-based expiry)
- Artifact promotion between registries
- Signed artifacts with SLSA provenance

### Environment Promotion

- Automated promotion with quality gates
- Manual approval gates for production
- Environment-specific configuration injection
- Promotion audit trail and rollback capability

### Parallel Execution

- Job concurrency limits and resource optimization
- Fan-out/fan-in pipeline patterns
- Dependency-aware job scheduling
- Resource pool management

### Secrets Management

- Environment-scoped secrets
- OIDC-based authentication (keyless)
- Vault integration for dynamic secrets
- Secret rotation during deployments

### Reusable Workflows

- Composite actions for common patterns
- Reusable workflow templates
- Shared pipeline libraries
- Version-pinned action references

### Monorepo Strategies

- Affected-only builds with path-based triggers
- Shared dependency detection
- Independent versioning per package
- Coordinated release across packages

## Related Knowledge Packs

- `skills/release-management/` — versioning, artifact registry, and release signing
- `skills/security/` — SAST, dependency scanning, and SBOM generation in pipelines
- `skills/infrastructure/` — Docker multi-stage builds and container image optimization
