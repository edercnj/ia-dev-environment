> Returns to [slim body](../SKILL.md) after reading the required platform or stage.

# x-security-pipeline — Full Protocol

## Template Variables

Pipeline generation uses these placeholders (resolved at runtime):
- `{{LANGUAGE}}` — project language (java, typescript, python, go, rust)
- `{{BUILD_TOOL}}` — build tool (maven, gradle, npm, pip, cargo, go)
- `{{FRAMEWORK}}` — framework (spring, quarkus, nestjs, fastapi, gin, axum)
- `{{PROJECT_NAME}}` — project name

## Knowledge Pack References

| Pack | Purpose |
|------|---------|
| `ci-cd-patterns` | Caching strategies, artifact management, pipeline structure |
| `security` | SecurityConfig flags, severity classification, `DevOps` agent config |

## Stage Dependency Reference

Stages use `dependsOn:` declarations (Azure DevOps) or `needs:` (GitLab) to declare dependencies:

```yaml
stage: pre-commit
dependsOn: []
```

**Minimal mode (stages 1-3):** Only Secret Scan (pre-commit), SAST (build), Dependency Audit (build). For teams starting with security. Dependency Audit is `Always enabled` regardless of flags.

## Report

Generate summary Report after pipeline creation:
- Enabled stages list
- Disabled stages with reasons
- Output file path
- Validation status

## Write Pipeline File

After rendering platform-specific YAML, write the output file:
- GitHub: `.github/workflows/security.yml`
- GitLab: `.gitlab-ci-security.yml` (or appended to `.gitlab-ci.yml`)
- Azure DevOps: `azure-pipelines-security.yml`

## Validate Generated YAML

`Validate Generated YAML` step after generation:
```bash
# GitHub Actions
actionlint .github/workflows/security.yml
# GitLab CI (.gitlab-ci-security.yml or appended .gitlab-ci.yml)
# Azure DevOps
az pipelines validate
```

## Error Handling

| Scenario | Action |
|----------|--------|
| Unknown CI platform | Default to `github`; warn user |
| No SecurityConfig flags | Generate Dependency Audit only (`Always enabled`) |
| All conditions disabled | Pipeline with only Dependency Audit |
| Container scan without Dockerfile | Exclude; add to `disabledStages` |
| Invalid severity threshold | Default to `HIGH`; warn |
| Validation tool missing | Warn, skip non-blocking |

## Composability (RULE-011)

This skill `never duplicates their scan logic` — it references atomic scanning skills and provides only CI stage wrappers (triggers, artifacts, caching). The scan logic lives in the referenced skill.

---

## Workflow

### Step 1 — Read Configuration

Read project config to evaluate stage conditions:

| Config Key | Enables Stage |
|-----------|---------------|
| `security.scanning.secrets` | Secret Scan |
| `security.scanning.sast` | SAST |
| `security.scanning.sonar` | SonarQube + Quality Gate |
| `security.scanning.dast` | DAST Passive |
| `security.scanning.hardening` | Hardening Eval |
| `security.frameworks` contains `"owasp"` | OWASP Scan |
| `infrastructure.container != "none"` | Container Scan |

Cross-reference `.claude/rules/01-project-identity.md` for authoritative stack info.

### Step 2 — Evaluate Stage Conditions

| Order | Stage | Skill | Phase | Condition | In minimal? |
|-------|-------|-------|-------|-----------|-------------|
| 1 | Secret Scan | x-security-secrets | pre-commit | `security.scanning.secrets=true` | Yes |
| 2 | SAST | x-security-sast | build | `security.scanning.sast=true` | Yes |
| 3 | Dependency Audit | x-dependency-audit | build | Always | Yes |
| 4 | SonarQube | x-security-sonar | build | `security.scanning.sonar=true` | No |
| 5 | Container Scan | x-security-container | build | `container != none` | No |
| 6 | DAST Passive | x-security-dast | deploy-staging | `security.scanning.dast=true` | No |
| 7 | OWASP Scan | x-owasp-scan | deploy-staging | `frameworks contains "owasp"` | No |
| 8 | Hardening Eval | x-hardening-eval | deploy-staging | `security.scanning.hardening=true` | No |
| 9 | Quality Gate | x-security-sonar | gate | `security.scanning.sonar=true` | No |

### Step 3 — Select Stages

For each stage: if `--stages=minimal` and not in minimal set → skip. Evaluate condition. Add to `enabledStages` or `disabledStages` (with reason). Dependency Audit always enabled regardless of flags.

### Step 4 — Render Platform-Specific YAML

---

## GitHub Actions Template (`.github/workflows/security.yml`)

```yaml
name: Security Pipeline

on:
  pull_request:
    branches: [develop, main]
  push:
    branches: [develop]
  # schedule: - cron: '0 2 * * 1'  # only when --trigger schedule

permissions:
  contents: read
  security-events: write  # required for SARIF upload

jobs:
  secret-scan:  # when security.scanning.secrets=true
    name: Secret Scan
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with: {fetch-depth: 0}
      - name: Run detect-secrets
        run: |
          pip install detect-secrets
          detect-secrets scan --all-files > secrets-report.json
          detect-secrets audit secrets-report.json
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: secret-scan-results
          path: secrets-report.json

  sast:  # when security.scanning.sast=true
    name: SAST (Semgrep)
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run Semgrep
        uses: semgrep/semgrep-action@v1
        with:
          config: >-
            p/ci
            p/java
            p/owasp-top-ten
        env:
          SEMGREP_APP_TOKEN: ${{ secrets.SEMGREP_APP_TOKEN }}
      - name: Upload SARIF
        uses: github/codeql-action/upload-sarif@v3
        if: always()
        with:
          sarif_file: semgrep.sarif

  dependency-audit:  # always enabled
    name: Dependency Audit
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run OWASP Dependency-Check / Grype
        run: |
          curl -sSfL https://raw.githubusercontent.com/anchore/grype/main/install.sh | sh
          grype dir:. --fail-on ${{ env.SEVERITY_THRESHOLD }} --output sarif > dependency-audit.sarif
        env:
          SEVERITY_THRESHOLD: ${{ vars.SECURITY_SEVERITY_THRESHOLD || 'HIGH' }}
      - uses: github/codeql-action/upload-sarif@v3
        if: always()
        with:
          sarif_file: dependency-audit.sarif

  container-scan:  # when infrastructure.container != none
    name: Container Scan (Trivy)
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run Trivy
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: fs
          format: sarif
          output: trivy-results.sarif
          severity: ${{ vars.SECURITY_SEVERITY_THRESHOLD || 'HIGH,CRITICAL' }}
          exit-code: '1'
      - uses: github/codeql-action/upload-sarif@v3
        if: always()
        with:
          sarif_file: trivy-results.sarif
```

---

## GitLab CI Template (appended to `.gitlab-ci.yml`)

```yaml
stages:
  - security-pre-commit
  - security-build
  - security-staging

variables:
  SEVERITY_THRESHOLD: "${SECURITY_SEVERITY_THRESHOLD:-HIGH}"

secret-scan:
  stage: security-pre-commit
  image: python:3.11
  script:
    - pip install detect-secrets
    - detect-secrets scan --all-files > secrets-report.json
  artifacts:
    reports:
      sast: secrets-report.json
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'

semgrep-sast:
  stage: security-build
  image: semgrep/semgrep:latest
  script:
    - semgrep ci --config=p/ci --config=p/java --config=p/owasp-top-ten --sarif > semgrep.sarif
  artifacts:
    reports:
      sast: semgrep.sarif
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'

trivy-scan:
  stage: security-build
  image: aquasec/trivy:latest
  script:
    - trivy fs . --format sarif --output trivy.sarif --severity $SEVERITY_THRESHOLD --exit-code 1
  artifacts:
    reports:
      container_scanning: trivy.sarif
```

---

## Azure DevOps Template (`azure-pipelines-security.yml`)

```yaml
trigger:
  branches:
    include:
      - develop
      - main

pool:
  vmImage: ubuntu-latest

variables:
  severityThreshold: $[coalesce(variables['SECURITY_SEVERITY_THRESHOLD'], 'HIGH')]

stages:
- stage: SecurityPre
  displayName: Security Pre-Checks
  jobs:
  - job: SecretScan
    displayName: Secret Scan
    steps:
    - script: |
        pip install detect-secrets
        detect-secrets scan --all-files > secrets-report.json
      displayName: Run detect-secrets

- stage: SecurityBuild
  displayName: Security Build Checks
  jobs:
  - job: SAST
    displayName: SAST (Semgrep)
    steps:
    - script: |
        docker run --rm -v $(Build.SourcesDirectory):/src semgrep/semgrep:latest \
          semgrep ci --config=p/java --config=p/owasp-top-ten --sarif > semgrep.sarif
      displayName: Run Semgrep
    - task: PublishBuildArtifacts@1
      inputs:
        pathToPublish: semgrep.sarif
        artifactName: SemgrepResults
```

---

## SARIF Artifact Upload

All three platforms support SARIF 2.1.0 upload for native security dashboard integration:

| Platform | Upload Mechanism | Dashboard |
|----------|-----------------|-----------|
| GitHub | `github/codeql-action/upload-sarif@v3` | Security → Code scanning |
| GitLab | `artifacts.reports.sast / container_scanning` | Security Dashboard |
| Azure | `PublishBuildArtifacts` + Security Center integration | Azure Security Center |

---

## Severity Threshold Configuration

| `--severity-threshold` | Pipeline fails on |
|-----------------------|-------------------|
| `CRITICAL` | Only CRITICAL findings |
| `HIGH` | CRITICAL + HIGH (default) |
| `MEDIUM` | CRITICAL + HIGH + MEDIUM |

Threshold passed to each scanning stage via `SEVERITY_THRESHOLD` environment variable. When `--fail-on-findings=false`, stages report findings as warnings but never exit non-zero.

---

## Tool Mapping

| Stage | Tool | Config/Rules |
|-------|------|-------------|
| Secret Scan | detect-secrets | `.secrets.baseline` |
| SAST | Semgrep | `p/ci`, `p/java`, `p/owasp-top-ten` |
| Dependency Audit | Grype (Syft/Anchore) | CVE databases |
| Container Scan | Trivy | Vuln + misconfig databases |
| SonarQube | SonarCloud/Server | `sonar-project.properties` |
| DAST | OWASP ZAP | Passive scan rules |
| OWASP Scan | x-owasp-scan | ASVS checklist |
| Hardening Eval | x-hardening-eval | CIS benchmarks |

---

## Composability (RULE-011)

This skill **references** atomic scanning skills — never duplicates their logic. To modify scan behavior (rules, exclusions, severity mappings), invoke the referenced skill directly.

| Stage | References Skill | This Skill Provides |
|-------|-----------------|---------------------|
| Secret Scan | x-security-secrets | CI stage wrapper (triggers, artifacts, caching) |
| SAST | x-security-sast | CI stage wrapper |
| Dependency Audit | x-dependency-audit | CI stage wrapper |
| SonarQube | x-security-sonar | CI stage wrapper |
| Container Scan | x-security-container | CI stage wrapper |
| DAST Passive | x-security-dast | CI stage wrapper |
| OWASP Scan | x-owasp-scan | CI stage wrapper |
| Hardening Eval | x-hardening-eval | CI stage wrapper |
| Quality Gate | x-security-sonar | CI stage wrapper (gate polling) |

---

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| x-security-secrets | references | Scan logic for Secret Scan stage |
| x-security-sast | references | Scan logic for SAST stage |
| x-dependency-audit | references | Scan logic for Dependency Audit stage |
| x-security-sonar | references | SonarQube + Quality Gate stages |
| x-security-container | references | Container Scan stage |
| x-security-dast | references | DAST Passive stage |
| x-owasp-scan | references | OWASP Scan stage |
| x-hardening-eval | references | Hardening Eval stage |
| x-ci-generate | complements | Generates general CI/CD; this skill adds security stages |
