---
name: x-ci-cd-generate
description: >
  Generate or update CI/CD pipelines based on project stack: detect language,
  analyze existing workflows, generate CI/CD/release/security pipelines,
  validate with actionlint, support monorepo triggers.
  Reference: `.github/skills/x-ci-cd-generate/SKILL.md`
---

# Skill: CI/CD Pipeline Generation

## Purpose

Generates or updates CI/CD pipeline configurations for {{PROJECT_NAME}} based on detected project stack. Analyzes existing workflows to avoid duplication, generates customized GitHub Actions workflows for CI, CD, release, and security scanning, validates generated YAML with actionlint, and supports monorepo path-based triggers.

## Triggers

- `/x-ci-cd-generate` -- generate all pipelines (default: all)
- `/x-ci-cd-generate ci` -- generate CI pipeline (build + test + security scan)
- `/x-ci-cd-generate cd` -- generate CD pipeline (deploy staging + production + rollback)
- `/x-ci-cd-generate release` -- generate release pipeline (semantic versioning + changelog)
- `/x-ci-cd-generate security` -- generate security scan pipeline (scheduled SAST + dependency audit)
- `/x-ci-cd-generate all` -- generate all pipeline types
- `/x-ci-cd-generate ci --monorepo` -- generate with path-based triggers for monorepo
- `/x-ci-cd-generate ci --force` -- overwrite existing workflows

## Arguments

| Argument | Type | Default | Description |
|----------|------|---------|-------------|
| `type` | Enum | `all` | Pipeline type: ci, cd, release, security, all |
| `--monorepo` | Flag | false | Activate path-based triggers for monorepo |
| `--force` | Flag | false | Overwrite existing workflow files |

## Workflow

```
1. DETECT     -> Detect project stack from config files (pom.xml, package.json, etc.)
2. ANALYZE    -> Check existing .github/workflows/ for conflicts
3. GENERATE   -> Generate workflow YAML files based on stack and type
4. VALIDATE   -> Run actionlint (if available) on generated files
5. REPORT     -> Report generated/updated files and any validation issues
```

### Step 1 -- Detect Stack

Analyze project root to identify language, build tool, and dependencies:

| Config File | Language | Build Tool |
|-------------|----------|------------|
| `pom.xml` | Java | Maven |
| `build.gradle` / `build.gradle.kts` | Java/Kotlin | Gradle |
| `package.json` | TypeScript/JavaScript | npm/yarn/pnpm |
| `go.mod` | Go | go |
| `Cargo.toml` | Rust | cargo |
| `pyproject.toml` / `requirements.txt` | Python | pip/poetry |

### Step 2 -- Analyze Existing Workflows

Check `.github/workflows/` for existing pipeline files and handle conflicts based on `--force` flag.

### Step 3 -- Generate Workflows

**Pipeline Types:**

| Type | Files Generated | Purpose |
|------|----------------|---------|
| CI | `ci.yml` | Build, test, security scan |
| CD | `deploy-staging.yml`, `deploy-production.yml`, `rollback.yml` | Deployment |
| Release | `release.yml` | Semantic versioning, changelog |
| Security | `security-scan.yml`, `dependency-audit.yml` | Scheduled security scanning |

**Language-Specific Build Steps:**

| Language | Setup Action | Cache | Build | Test |
|----------|-------------|-------|-------|------|
| Java/Maven | `setup-java@v4` | `~/.m2/repository` | `mvn package -DskipTests` | `mvn verify` |
| Java/Gradle | `setup-java@v4` | `~/.gradle/caches` | `./gradlew build -x test` | `./gradlew test` |
| Node.js | `setup-node@v4` | `node_modules` | `npm ci && npm run build` | `npm test` |
| Go | `setup-go@v5` | `~/go/pkg/mod` | `go build ./...` | `go test ./...` |
| Rust | `dtolnay/rust-toolchain@stable` | `target/` | `cargo build` | `cargo test` |
| Python | `setup-python@v5` | `~/.cache/pip` | `pip install -e .` | `pytest` |

### Step 4 -- Validate with actionlint

Validate generated files with actionlint if available. Skip validation with warning if not installed.

### Step 5 -- Report

Generate summary of generated files with status (GENERATED, SKIPPED, OVERWRITTEN).

## Monorepo Support

When `--monorepo` flag is active, generated workflows include path-based triggers derived from project structure (services/, packages/, apps/ directories).

## Error Handling

| Scenario | Action |
|----------|--------|
| Language not detected | List supported languages, ask user to specify |
| Workflow file exists (no --force) | Report "file exists, use --force to overwrite" |
| actionlint not installed | Warn and skip validation (non-blocking) |
| No Dockerfile found (CD requested) | Generate CD without container steps, warn user |

## Integration Notes

- References CI/CD patterns KP for pipeline templates and best practices
- Output follows GitHub Actions workflow syntax
- Works with any detected language stack (Java, TypeScript, Go, Rust, Python)
- Generated workflows follow caching strategies from CI/CD patterns KP
- Security scanning integrates with GitHub Security tab via SARIF uploads
