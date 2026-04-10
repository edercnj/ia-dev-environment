---
name: x-ci-generate
description: "Generate or update CI/CD pipelines based on project stack: detect language, analyze existing workflows, generate CI/CD/release/security pipelines, validate with actionlint, support monorepo triggers."
user-invocable: true
allowed-tools: Read, Write, Edit, Glob, Grep, Bash, Agent
argument-hint: "[ci|cd|release|security|all] [--monorepo] [--force]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: CI/CD Pipeline Generation

## Purpose

Generates or updates CI/CD pipeline configurations for {{PROJECT_NAME}} based on detected project stack. Analyzes existing workflows to avoid duplication, generates customized GitHub Actions workflows for CI, CD, release, and security scanning, validates generated YAML with actionlint, and supports monorepo path-based triggers.

## Triggers

- `/x-ci-generate` — generate all pipelines (default: all)
- `/x-ci-generate ci` — generate CI pipeline (build + test + security scan)
- `/x-ci-generate cd` — generate CD pipeline (deploy staging + production + rollback)
- `/x-ci-generate release` — generate release pipeline (semantic versioning + changelog)
- `/x-ci-generate security` — generate security scan pipeline (scheduled SAST + dependency audit)
- `/x-ci-generate all` — generate all pipeline types
- `/x-ci-generate ci --monorepo` — generate with path-based triggers for monorepo
- `/x-ci-generate ci --force` — overwrite existing workflows

## Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
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

### Step 1 — Detect Stack

Analyze project root to identify language, build tool, and dependencies:

| Config File | Language | Build Tool | Framework Detection |
|-------------|----------|------------|-------------------|
| `pom.xml` | Java | Maven | Spring Boot, Quarkus (from dependencies) |
| `build.gradle` / `build.gradle.kts` | Java/Kotlin | Gradle | Spring Boot, Ktor (from plugins) |
| `package.json` | TypeScript/JavaScript | npm/yarn/pnpm | NestJS, Express (from dependencies) |
| `go.mod` | Go | go | Gin, Echo (from require) |
| `Cargo.toml` | Rust | cargo | Axum, Actix (from dependencies) |
| `pyproject.toml` / `requirements.txt` | Python | pip/poetry | FastAPI, Django (from dependencies) |

```bash
# Auto-detect language from project root
ls -la pom.xml build.gradle* go.mod Cargo.toml pyproject.toml package.json 2>/dev/null
```

Cross-reference with `.claude/rules/01-project-identity.md` (RULE-001 — Project Identity) if available for authoritative stack information.

Additionally detect:
- **Dockerfile presence**: enables container build steps
- **docker-compose.yml**: enables integration test services
- **Helm charts / k8s manifests**: enables deployment steps
- **Terraform / IaC**: enables infrastructure pipeline steps

### Step 2 — Analyze Existing Workflows

Check `.github/workflows/` for existing pipeline files:

```bash
# List existing workflows
ls -la .github/workflows/*.yml .github/workflows/*.yaml 2>/dev/null
```

For each existing workflow:
- Parse the `name:` field to identify purpose
- Check trigger events (`on:` section)
- Identify potential conflicts with requested generation

**Conflict Resolution:**

| Scenario | Without --force | With --force |
|----------|----------------|--------------|
| File exists, same purpose | Report "exists, use --force" | Overwrite |
| File exists, different purpose | Skip (no conflict) | Skip (no conflict) |
| File does not exist | Generate | Generate |

### Step 3 — Generate Workflows

Reference the CI/CD patterns knowledge pack (`skills/ci-cd-patterns/`) for pipeline templates and best practices.

#### 3.1 — CI Pipeline (`ci.yml`)

Generate continuous integration workflow:

```yaml
# Git Flow: CI runs on integration (develop), release, and hotfix branches
name: CI
on:
  push:
    branches: [develop, 'release/**', 'hotfix/**']
  pull_request:
    branches: [develop, main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      # Language-specific setup (Java/Node/Go/Rust/Python)
      # Dependency caching
      # Build step
      # Unit tests with coverage
      # Integration tests (if applicable)

  security:
    runs-on: ubuntu-latest
    needs: build
    steps:
      # SAST scanning
      # Dependency vulnerability scan
      # License compliance check
```

**Language-Specific Build Steps:**

| Language | Setup Action | Cache | Build | Test |
|----------|-------------|-------|-------|------|
| Java/Maven | `setup-java@v4` | `~/.m2/repository` | `mvn package -DskipTests` | `mvn verify` |
| Java/Gradle | `setup-java@v4` | `~/.gradle/caches` | `./gradlew build -x test` | `./gradlew test` |
| Node.js | `setup-node@v4` | `node_modules` | `npm ci && npm run build` | `npm test` |
| Go | `setup-go@v5` | `~/go/pkg/mod` | `go build ./...` | `go test ./...` |
| Rust | `dtolnay/rust-toolchain@stable` | `target/` | `cargo build` | `cargo test` |
| Python | `setup-python@v5` | `~/.cache/pip` | `pip install -e .` | `pytest` |

#### 3.2 — CD Pipeline (`deploy-staging.yml`, `deploy-production.yml`, `rollback.yml`)

Generate continuous deployment workflows:

**deploy-staging.yml:**
- Triggered on push to `develop` branch (Git Flow integration branch)
- Builds container image
- Pushes to container registry
- Deploys to staging environment
- Runs smoke tests

**deploy-production.yml:**
- Triggered on push to `main` (via release/hotfix merge) or version tags (`v*`)
- Also supports manual trigger (`workflow_dispatch`)
- Requires approval gate
- Promotes staging artifact to production
- Runs production smoke tests
- Includes health check validation

**rollback.yml:**
- Triggered manually with version input
- Rolls back to specified previous version
- Validates rollback health

#### 3.3 — Release Pipeline (`release.yml`)

Generate release workflow:

- Triggered on push of version tags (`v*.*.*`)
- Generates changelog from conventional commits
- Creates GitHub Release with notes
- Publishes artifacts to registry
- Updates version references

#### 3.4 — Security Pipeline (`security-scan.yml`)

Generate scheduled security scanning:

- Runs on schedule (weekly), on push to `develop` and `main`, and on-demand
- SAST with CodeQL or Semgrep
- Dependency audit (language-specific)
- Container image scanning (if Dockerfile present)
- Results uploaded as SARIF to GitHub Security tab

#### 3.5 — Dependency Audit Pipeline (`dependency-audit.yml`)

Generate scheduled dependency audit:

- Runs on schedule (daily)
- Checks for known vulnerabilities
- Reports outdated dependencies
- Creates issues for critical findings

### Step 4 — Validate with actionlint

If actionlint is available, validate all generated workflow files:

```bash
# Check if actionlint is available
which actionlint 2>/dev/null

# Validate each generated file
actionlint .github/workflows/ci.yml
```

If actionlint is not installed, report:
- "actionlint not found. Install with: brew install actionlint (macOS) or go install github.com/rhysd/actionlint/cmd/actionlint@latest"
- Continue without validation (non-blocking)

### Step 5 — Report

Generate summary of actions taken:

```
============================================
  CI/CD Pipeline Generation Report
  Project: {{PROJECT_NAME}}
  Stack:   {detected language} / {detected framework}
============================================

| Pipeline          | File                              | Status    |
|-------------------|-----------------------------------|-----------|
| CI                | .github/workflows/ci.yml          | GENERATED |
| Deploy Staging    | .github/workflows/deploy-staging.yml | GENERATED |
| Deploy Production | .github/workflows/deploy-production.yml | GENERATED |
| Rollback          | .github/workflows/rollback.yml    | GENERATED |
| Release           | .github/workflows/release.yml     | GENERATED |
| Security Scan     | .github/workflows/security-scan.yml | GENERATED |
| Dependency Audit  | .github/workflows/dependency-audit.yml | GENERATED |

Validation: PASSED (actionlint)
```

## Monorepo Support

When `--monorepo` flag is active, generated workflows include path-based triggers:

```yaml
on:
  push:
    paths:
      - 'services/my-service/**'
      - '.github/workflows/ci-my-service.yml'
  pull_request:
    paths:
      - 'services/my-service/**'
```

Path detection strategy:
1. Scan for service directories (`services/`, `packages/`, `apps/`)
2. Generate separate workflows per service or shared workflow with path matrix
3. Include shared library paths in triggers

## Generated Files

| Type | File | Description |
|------|------|-------------|
| CI | `.github/workflows/ci.yml` | Build, test, security scan |
| CD Staging | `.github/workflows/deploy-staging.yml` | Deploy to staging |
| CD Production | `.github/workflows/deploy-production.yml` | Deploy to production |
| Rollback | `.github/workflows/rollback.yml` | Rollback deployment |
| Release | `.github/workflows/release.yml` | Semantic release |
| Security | `.github/workflows/security-scan.yml` | Scheduled SAST scan |
| Dependency | `.github/workflows/dependency-audit.yml` | Scheduled dependency audit |

## Error Handling

| Scenario | Action |
|----------|--------|
| Language not detected | List supported languages, ask user to specify |
| Workflow file exists (no --force) | Report "file exists, use --force to overwrite" |
| actionlint not installed | Warn and skip validation (non-blocking) |
| Invalid type argument | Default to "all", warn user |
| No Dockerfile found (CD requested) | Generate CD without container steps, warn user |
| No project config found | Report error with setup instructions |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `devops-engineer` agent | calls | Used for advanced pipeline customization via Agent tool |
| `ci-cd-patterns` KP | reads | Pipeline templates and best practices |
| `x-dependency-audit` | reads | Dependency audit pipeline references audit commands |
| `x-security-pipeline` | reads | Security pipeline references scanning configurations |

## Knowledge Pack References

| Pack | Files | Purpose |
|------|-------|---------|
| ci-cd-patterns | `skills/ci-cd-patterns/SKILL.md` | Pipeline templates and best practices |
