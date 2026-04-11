---
name: x-security-pipeline
description: "Generate CI/CD pipeline configurations with conditional security stages based on SecurityConfig flags. Support GitHub Actions, GitLab CI, and Azure DevOps with minimal and full stage modes, configurable severity thresholds, and SARIF artifact upload."
user-invocable: true
allowed-tools: Read, Write, Edit, Glob, Grep, Bash, Agent
argument-hint: "[--ci github|gitlab|azure] [--stages all|minimal] [--trigger push|pr|schedule] [--fail-on-findings true|false] [--severity-threshold CRITICAL|HIGH|MEDIUM]"
context-budget: heavy
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Security CI Pipeline Generator

## Purpose

Generate CI/CD pipeline configuration files with conditional security stages for {{PROJECT_NAME}}. Each stage is included only when its corresponding SecurityConfig flag is enabled. Support three CI platforms (GitHub Actions, GitLab CI, Azure DevOps), two stage modes (minimal, all), and three trigger types (push, pr, schedule). Reference atomic scanning skills (x-security-sast, x-security-secrets, x-security-container, x-security-dast, x-owasp-scan, x-security-sonar) instead of duplicating scan logic (RULE-011 — Composability).

## Triggers

- `/x-security-pipeline` — generate security pipeline (default: GitHub Actions, all stages, pr trigger)
- `/x-security-pipeline --ci github` — generate GitHub Actions security pipeline
- `/x-security-pipeline --ci gitlab` — generate GitLab CI security pipeline
- `/x-security-pipeline --ci azure` — generate Azure DevOps security pipeline
- `/x-security-pipeline --stages minimal` — generate minimal pipeline (SAST + secret scan + dependency audit)
- `/x-security-pipeline --stages all` — generate full pipeline with all 9 conditional stages
- `/x-security-pipeline --trigger schedule` — generate pipeline with scheduled trigger

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `--ci` | Enum | No | `github` | CI platform: `github`, `gitlab`, `azure` |
| `--stages` | Enum | No | `all` | Stage mode: `all` (9 stages), `minimal` (3 stages) |
| `--trigger` | Enum | No | `pr` | Pipeline trigger: `push`, `pr`, `schedule` |
| `--fail-on-findings` | Boolean | No | `true` | Fail pipeline on security findings |
| `--severity-threshold` | Enum | No | `HIGH` | Minimum severity to fail: `CRITICAL`, `HIGH`, `MEDIUM` |

## Workflow

### Step 1 — Read Configuration

Read project configuration to evaluate stage conditions:

| Config Key | Usage |
|-----------|-------|
| `security.scanning.secrets` | Enables Secret Scan stage |
| `security.scanning.sast` | Enables SAST stage |
| `security.scanning.sonar` | Enables SonarQube and Quality Gate stages |
| `security.scanning.dast` | Enables DAST Passive stage |
| `security.scanning.hardening` | Enables Hardening Eval stage |
| `security.frameworks` | Enables OWASP Scan when contains "owasp" |
| `infrastructure.container` | Enables Container Scan when != "none" |

Cross-reference with `.claude/rules/01-project-identity.md` (RULE-001 — Project Identity) for authoritative stack information.

### Step 2 — Evaluate Stage Conditions

Evaluate each stage condition and build the stage list:

| Order | Stage | Skill Reference | Phase | Condition | Minimal |
|-------|-------|----------------|-------|-----------|---------|
| 1 | Secret Scan | x-security-secrets | pre-commit | `security.scanning.secrets = true` | Yes |
| 2 | SAST | x-security-sast | build | `security.scanning.sast = true` | Yes |
| 3 | Dependency Audit | x-dependency-audit | build | Always enabled (baseline) | Yes |
| 4 | SonarQube | x-security-sonar | build | `security.scanning.sonar = true` | No |
| 5 | Container Scan | x-security-container | build | `infrastructure.container != "none"` | No |
| 6 | DAST Passive | x-security-dast | deploy-staging | `security.scanning.dast = true` | No |
| 7 | OWASP Scan | x-owasp-scan | deploy-staging | `security.frameworks contains "owasp"` | No |
| 8 | Hardening Eval | x-hardening-eval | deploy-staging | `security.scanning.hardening = true` | No |
| 9 | Quality Gate | x-security-sonar | gate | `security.scanning.sonar = true` | No |

**Minimal mode**: Only stages 1-3 (Secret Scan, SAST, Dependency Audit). For teams starting with security.

**All mode**: All 9 stages, each conditionally included based on its flag evaluation.

### Step 3 — Select Stages

For each stage in the ordered list:

1. If `--stages=minimal` and stage is not in minimal set, skip
2. Evaluate the stage condition against the configuration
3. If condition is met, add to `enabledStages`
4. If condition is not met, add to `disabledStages` with reason

**Note**: Dependency Audit (stage 3) is always enabled regardless of flags.

### Step 4 — Render Platform-Specific YAML

Generate pipeline YAML using the selected stages and platform-specific syntax.

**Template placeholders** (RULE-015 — Template Conventions):
- `{{LANGUAGE}}` — project language (java, typescript, python, go, rust)
- `{{BUILD_TOOL}}` — build tool (maven, gradle, npm, pip, cargo, go)
- `{{FRAMEWORK}}` — framework name (spring, quarkus, nestjs, fastapi, gin, axum)
- `{{PROJECT_NAME}}` — project name

#### 4.1 — GitHub Actions (`.github/workflows/security.yml`)

```yaml
name: Security Pipeline
on:
  # Trigger: pr
  pull_request:
    branches: [main, develop]
  # Trigger: push
  # push:
  #   branches: [main, develop]
  # Trigger: schedule
  # schedule:
  #   - cron: '0 6 * * 1'

jobs:
  # Phase: pre-commit
  secret-scan:
    name: Secret Scan
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
      - name: Run secret scan
        # Reference: x-security-secrets skill
        run: |
          # Install and run gitleaks/trufflehog
          # Configured via x-security-secrets
        env:
          SEVERITY_THRESHOLD: HIGH
          FAIL_ON_FINDINGS: "true"
    continue-on-error: false

  # Phase: build
  sast:
    name: SAST Analysis
    runs-on: ubuntu-latest
    needs: [secret-scan]
    steps:
      - uses: actions/checkout@v4
      - name: Run SAST scan
        # Reference: x-security-sast skill
        run: |
          # Run semgrep/codeql for {{LANGUAGE}}
          # Configured via x-security-sast

  dependency-audit:
    name: Dependency Audit
    runs-on: ubuntu-latest
    needs: [secret-scan]
    steps:
      - uses: actions/checkout@v4
      - name: Run dependency audit
        # Reference: x-dependency-audit skill
        run: |
          # Run {{BUILD_TOOL}}-specific dependency audit
          # Configured via x-dependency-audit

  sonarqube:
    name: SonarQube Analysis
    runs-on: ubuntu-latest
    needs: [sast]
    steps:
      - uses: actions/checkout@v4
      - name: Run SonarQube scan
        # Reference: x-security-sonar skill
        run: |
          # Run sonar-scanner
          # Configured via x-security-sonar
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}

  container-scan:
    name: Container Scan
    runs-on: ubuntu-latest
    needs: [dependency-audit]
    steps:
      - uses: actions/checkout@v4
      - name: Build container image
        run: docker build -t {{PROJECT_NAME}}:scan .
      - name: Run container scan
        # Reference: x-security-container skill
        run: |
          # Run trivy/grype on built image
          # Configured via x-security-container

  # Phase: deploy-staging
  dast-passive:
    name: DAST Passive Scan
    runs-on: ubuntu-latest
    needs: [sonarqube, container-scan]
    steps:
      - uses: actions/checkout@v4
      - name: Run DAST passive scan
        # Reference: x-security-dast skill
        run: |
          # Run ZAP passive scan against staging
          # Configured via x-security-dast
        env:
          TARGET_URL: ${{ vars.STAGING_URL }}

  owasp-scan:
    name: OWASP Dependency Check
    runs-on: ubuntu-latest
    needs: [sonarqube, container-scan]
    steps:
      - uses: actions/checkout@v4
      - name: Run OWASP scan
        # Reference: x-owasp-scan skill
        run: |
          # Run OWASP dependency-check
          # Configured via x-owasp-scan

  hardening-eval:
    name: Hardening Evaluation
    runs-on: ubuntu-latest
    needs: [dast-passive, owasp-scan]
    steps:
      - uses: actions/checkout@v4
      - name: Run hardening evaluation
        # Reference: x-hardening-eval skill
        run: |
          # Evaluate container/infra hardening
          # Configured via x-hardening-eval

  # Phase: gate
  quality-gate:
    name: Security Quality Gate
    runs-on: ubuntu-latest
    needs: [hardening-eval]
    steps:
      - name: Check SonarQube quality gate
        # Reference: x-security-sonar skill
        run: |
          # Poll SonarQube quality gate status
          # Configured via x-security-sonar
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
      - name: Aggregate security results
        run: |
          echo "All security stages passed"
```

#### 4.2 — GitLab CI (`.gitlab-ci-security.yml`)

```yaml
stages:
  - pre-commit
  - build
  - deploy-staging
  - gate

# Phase: pre-commit
secret-scan:
  stage: pre-commit
  image: zricethezav/gitleaks:latest
  script:
    # Reference: x-security-secrets skill
    - gitleaks detect --source . --verbose
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
  artifacts:
    reports:
      sast: gl-secret-detection-report.json
    when: always

# Phase: build
sast:
  stage: build
  image: returntocorp/semgrep:latest
  script:
    # Reference: x-security-sast skill
    - semgrep ci --config auto
  needs: [secret-scan]
  artifacts:
    reports:
      sast: gl-sast-report.json

dependency-audit:
  stage: build
  script:
    # Reference: x-dependency-audit skill
    - echo "Running dependency audit for {{BUILD_TOOL}}"
  needs: [secret-scan]

sonarqube:
  stage: build
  image: sonarsource/sonar-scanner-cli:latest
  script:
    # Reference: x-security-sonar skill
    - sonar-scanner
  needs: [sast]
  variables:
    SONAR_TOKEN: $SONAR_TOKEN

container-scan:
  stage: build
  image: aquasec/trivy:latest
  script:
    # Reference: x-security-container skill
    - trivy image {{PROJECT_NAME}}:$CI_COMMIT_SHA
  needs: [dependency-audit]

# Phase: deploy-staging
dast-passive:
  stage: deploy-staging
  image: ghcr.io/zaproxy/zaproxy:stable
  script:
    # Reference: x-security-dast skill
    - zap-baseline.py -t $STAGING_URL
  needs: [sonarqube, container-scan]

owasp-scan:
  stage: deploy-staging
  script:
    # Reference: x-owasp-scan skill
    - echo "Running OWASP scan"
  needs: [sonarqube, container-scan]

hardening-eval:
  stage: deploy-staging
  script:
    # Reference: x-hardening-eval skill
    - echo "Running hardening evaluation"
  needs: [dast-passive, owasp-scan]

# Phase: gate
quality-gate:
  stage: gate
  script:
    # Reference: x-security-sonar skill
    - echo "Checking SonarQube quality gate"
  needs: [hardening-eval]
```

#### 4.3 — Azure DevOps (`azure-pipelines-security.yml`)

```yaml
trigger:
  branches:
    include:
      - main
      - develop

pool:
  vmImage: 'ubuntu-latest'

stages:
  - stage: PreCommit
    displayName: 'Pre-commit Security'
    jobs:
      - job: SecretScan
        displayName: 'Secret Scan'
        steps:
          - checkout: self
            fetchDepth: 0
          - script: |
              # Reference: x-security-secrets skill
              echo "Running secret scan"
            displayName: 'Run secret scan'

  - stage: Build
    displayName: 'Build Security'
    dependsOn: PreCommit
    jobs:
      - job: SAST
        displayName: 'SAST Analysis'
        steps:
          - checkout: self
          - script: |
              # Reference: x-security-sast skill
              echo "Running SAST for {{LANGUAGE}}"
            displayName: 'Run SAST scan'

      - job: DependencyAudit
        displayName: 'Dependency Audit'
        steps:
          - checkout: self
          - script: |
              # Reference: x-dependency-audit skill
              echo "Running dependency audit"
            displayName: 'Run dependency audit'

      - job: SonarQube
        displayName: 'SonarQube Analysis'
        dependsOn: SAST
        steps:
          - checkout: self
          - script: |
              # Reference: x-security-sonar skill
              echo "Running SonarQube"
            displayName: 'Run SonarQube scan'

      - job: ContainerScan
        displayName: 'Container Scan'
        dependsOn: DependencyAudit
        steps:
          - checkout: self
          - script: |
              # Reference: x-security-container skill
              docker build -t {{PROJECT_NAME}}:scan .
              echo "Running container scan"
            displayName: 'Run container scan'

  - stage: DeployStaging
    displayName: 'Deploy Staging Security'
    dependsOn: Build
    jobs:
      - job: DASTPassive
        displayName: 'DAST Passive Scan'
        steps:
          - script: |
              # Reference: x-security-dast skill
              echo "Running DAST passive scan"
            displayName: 'Run DAST scan'

      - job: OWASPScan
        displayName: 'OWASP Scan'
        steps:
          - script: |
              # Reference: x-owasp-scan skill
              echo "Running OWASP scan"
            displayName: 'Run OWASP scan'

      - job: HardeningEval
        displayName: 'Hardening Evaluation'
        dependsOn: [DASTPassive, OWASPScan]
        steps:
          - script: |
              # Reference: x-hardening-eval skill
              echo "Running hardening eval"
            displayName: 'Run hardening evaluation'

  - stage: Gate
    displayName: 'Security Quality Gate'
    dependsOn: DeployStaging
    jobs:
      - job: QualityGate
        displayName: 'Quality Gate Check'
        steps:
          - script: |
              # Reference: x-security-sonar skill
              echo "Checking quality gate"
            displayName: 'Check quality gate'
```

### Step 5 — Validate Generated YAML

Validate the generated YAML is structurally correct:

- For GitHub Actions: validate with `actionlint` (if available)
- For GitLab CI: validate YAML structure with `yamllint`
- For Azure DevOps: validate YAML structure with `yamllint`

```bash
# GitHub Actions validation
which actionlint 2>/dev/null && actionlint .github/workflows/security.yml

# Generic YAML validation
which yamllint 2>/dev/null && yamllint <generated-file>
```

If validation tools are not installed, report a warning and continue (non-blocking).

### Step 6 — Write Pipeline File

Write the generated pipeline to the platform-specific path:

| Platform | Output File |
|----------|------------|
| GitHub Actions | `.github/workflows/security.yml` |
| GitLab CI | `.gitlab-ci-security.yml` |
| Azure DevOps | `azure-pipelines-security.yml` |

### Step 7 — Report

Generate a summary of the pipeline generation:

```
============================================
  Security Pipeline Generation Report
  Project:  {{PROJECT_NAME}}
  Platform: {ci_platform}
  Mode:     {stages_mode}
  Trigger:  {trigger_type}
============================================

| # | Stage              | Phase          | Status   | Condition                              |
|---|--------------------|----------------|----------|----------------------------------------|
| 1 | Secret Scan        | pre-commit     | ENABLED  | security.scanning.secrets = true        |
| 2 | SAST               | build          | ENABLED  | security.scanning.sast = true           |
| 3 | Dependency Audit   | build          | ENABLED  | Always enabled (baseline)               |
| 4 | SonarQube          | build          | DISABLED | security.scanning.sonar = false         |
| 5 | Container Scan     | build          | ENABLED  | infrastructure.container = "docker"     |
| 6 | DAST Passive       | deploy-staging | DISABLED | security.scanning.dast = false          |
| 7 | OWASP Scan         | deploy-staging | DISABLED | security.frameworks missing "owasp"     |
| 8 | Hardening Eval     | deploy-staging | DISABLED | security.scanning.hardening = false     |
| 9 | Quality Gate       | gate           | DISABLED | security.scanning.sonar = false         |

Enabled:  4 stages
Disabled: 5 stages
Output:   .github/workflows/security.yml

Validation: PASSED (actionlint)
```

## Stage Configuration Details

### fail-on-findings

When `--fail-on-findings=true` (default), each scanning stage is configured to exit with a non-zero code when findings at or above the severity threshold are detected. This causes the pipeline to fail.

When `--fail-on-findings=false`, scanning stages report findings as warnings but do not fail the pipeline. Use this for initial adoption when baseline findings exist.

### severity-threshold

Controls the minimum severity level that triggers a pipeline failure:

| Threshold | Fails On |
|-----------|----------|
| `CRITICAL` | Only CRITICAL findings |
| `HIGH` | CRITICAL and HIGH findings |
| `MEDIUM` | CRITICAL, HIGH, and MEDIUM findings |

Each scanning stage receives the threshold via environment variable `SEVERITY_THRESHOLD`.

## Composability (RULE-011 — Skill Composability)

This skill **references** atomic scanning skills and never duplicates their scan logic:

| Stage | References Skill | What This Skill Does |
|-------|-----------------|---------------------|
| Secret Scan | x-security-secrets | Provides CI stage wrapper (triggers, artifacts, caching) |
| SAST | x-security-sast | Provides CI stage wrapper |
| Dependency Audit | x-dependency-audit | Provides CI stage wrapper |
| SonarQube | x-security-sonar | Provides CI stage wrapper |
| Container Scan | x-security-container | Provides CI stage wrapper |
| DAST Passive | x-security-dast | Provides CI stage wrapper |
| OWASP Scan | x-owasp-scan | Provides CI stage wrapper |
| Hardening Eval | x-hardening-eval | Provides CI stage wrapper |
| Quality Gate | x-security-sonar | Provides CI stage wrapper (gate polling) |

To modify scan behavior (rules, exclusions, severity mappings), use the referenced atomic skill directly.

## Error Handling

| Scenario | Action |
|----------|--------|
| Unknown CI platform | Default to GitHub Actions, warn user |
| No SecurityConfig flags set | Generate only Dependency Audit (always enabled) |
| Validation tool missing | Warn and skip validation (non-blocking) |
| All conditions disabled (all mode) | Generate pipeline with only Dependency Audit stage |
| Container scan without Dockerfile | Exclude container scan, add to disabledStages |
| Invalid severity threshold | Default to HIGH, warn user |
| Invalid stages mode | Default to all, warn user |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| x-security-secrets | references | Provides scan logic for Secret Scan stage |
| x-security-sast | references | Provides scan logic for SAST stage |
| x-dependency-audit | references | Provides scan logic for Dependency Audit stage |
| x-security-sonar | references | Provides scan logic for SonarQube and Quality Gate stages |
| x-security-container | references | Provides scan logic for Container Scan stage |
| x-security-dast | references | Provides scan logic for DAST Passive stage |
| x-owasp-scan | references | Provides scan logic for OWASP Scan stage |
| x-hardening-eval | references | Provides scan logic for Hardening Eval stage |
| x-ci-generate | complements | Generates general CI/CD pipelines; this skill adds security stages |

## Knowledge Pack References

| Knowledge Pack | Usage |
|----------------|-------|
| security | SecurityConfig flags, severity classification |
| ci-cd-patterns | Caching strategies, artifact management, pipeline structure |
