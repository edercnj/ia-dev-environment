# x-ci-cd-generate

> Generate or update CI/CD pipelines based on project stack: detect language, analyze existing workflows, generate CI/CD/release/security pipelines, validate with actionlint, support monorepo triggers.

| | |
|---|---|
| **Category** | Git/Release |
| **Invocation** | `/x-ci-cd-generate [ci\|cd\|release\|security\|all] [--monorepo] [--force]` |
| **Reads** | ci-cd-patterns |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Auto-detects the project language and framework from config files, then generates GitHub Actions workflow files for CI, CD, release, and security scanning. Analyzes existing workflows to avoid duplication, supports monorepo path-based triggers, and validates generated YAML with actionlint. Covers build, test, deploy (staging/production), rollback, release, security scan, and dependency audit pipelines.

## Usage

```
/x-ci-cd-generate
/x-ci-cd-generate ci
/x-ci-cd-generate all --monorepo
/x-ci-cd-generate ci --force
```

## Workflow

1. **Detect** -- Identify project stack from config files (pom.xml, package.json, go.mod, etc.)
2. **Analyze** -- Check existing `.github/workflows/` for conflicts
3. **Generate** -- Create workflow YAML files based on stack and requested type
4. **Validate** -- Run actionlint on generated files (non-blocking if unavailable)
5. **Report** -- List generated/updated files and validation results

## Outputs

| Artifact | Path |
|----------|------|
| CI pipeline | `.github/workflows/ci.yml` |
| Deploy staging | `.github/workflows/deploy-staging.yml` |
| Deploy production | `.github/workflows/deploy-production.yml` |
| Rollback | `.github/workflows/rollback.yml` |
| Release | `.github/workflows/release.yml` |
| Security scan | `.github/workflows/security-scan.yml` |
| Dependency audit | `.github/workflows/dependency-audit.yml` |

## See Also

- [x-release](../x-release/) -- Release flow that triggers the generated release pipeline
- [x-git-push](../x-git-push/) -- Branch strategy aligned with CI trigger configuration
