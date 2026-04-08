# ci-cd-patterns

> CI/CD pipeline patterns: build stages, test parallelization, security scanning, artifact management, environment promotion, approval gates, caching strategies, and language-specific pipeline configurations.

| | |
|---|---|
| **Category** | Knowledge Pack |
| **Referenced by** | x-ci-cd-generate, x-security-pipeline, x-release, devops-engineer agent |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete reference.

## Topics Covered

- Pipeline stages: build, test, security, artifact, deploy, post-deploy
- Matrix builds and conditional execution
- Caching strategies: dependency, build, test result, Docker layer caching
- Artifact management with semantic versioning and SLSA provenance
- Environment promotion with quality gates and approval workflows
- Parallel execution, fan-out/fan-in patterns, and resource management
- Secrets management with OIDC and Vault integration
- Monorepo strategies with affected-only builds

## Key Concepts

This pack defines a comprehensive CI/CD pipeline architecture covering six stages from build through post-deploy verification. It emphasizes security scanning (SAST, dependency scanning, container scanning, secrets detection) as a first-class pipeline stage and artifact management with SLSA provenance for supply chain security. Caching strategies are language-specific to optimize build times, and environment promotion follows automated quality gates with manual approval for production. The pack supports both single-repo and monorepo configurations with path-based triggers and independent versioning.

## See Also

- [infrastructure](../infrastructure/) — Docker multi-stage builds and Kubernetes manifests
- [compliance](../compliance/) — Security scanning and compliance framework requirements
- [feature-flags](../feature-flags/) — Progressive delivery patterns integrated with CI/CD
