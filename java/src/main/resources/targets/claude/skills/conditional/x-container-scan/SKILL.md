---
name: x-container-scan
description: "Scans Docker images for CVEs and Dockerfile best practices violations. Uses Trivy, Grype, or Snyk Container. Produces SARIF output with scoring."
user-invocable: true
allowed-tools: Read, Write, Bash, Grep, Glob
argument-hint: "[--image name:tag] [--dockerfile path] [--severity-threshold CRITICAL|HIGH|MEDIUM|LOW] [--ignore-unfixed]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Container Security Scanner

## Purpose

Scan Docker container images for known CVEs and analyze Dockerfiles for security best practices violations. Generate a combined SARIF 2.1.0 report with severity scoring and grade assignment.

## Activation Condition

Include this skill when container security scanning is required for the project.

## Triggers

- `/x-container-scan --image myapp:1.0` -- scan image for vulnerabilities
- `/x-container-scan --dockerfile ./Dockerfile` -- lint Dockerfile for best practices
- `/x-container-scan --image myapp:1.0 --dockerfile ./Dockerfile` -- combined scan
- `/x-container-scan --image myapp:1.0 --ignore-unfixed` -- exclude CVEs without available fix

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `--image` | String | One of image/dockerfile required | (none) | Docker image name:tag to scan |
| `--dockerfile` | String | One of image/dockerfile required | ./Dockerfile | Path to Dockerfile to analyze |
| `--severity-threshold` | Enum | No | LOW | Minimum severity: CRITICAL, HIGH, MEDIUM, LOW |
| `--ignore-unfixed` | Boolean | No | false | Exclude CVEs without an available fix |

## Workflow

### Step 1 — Validate Parameters

- At least one of `--image` or `--dockerfile` MUST be provided
- If `--image` is provided, validate name:tag format
- If `--dockerfile` is provided, validate file exists at path

### Step 2 — Detect Scanning Tools

Check tool availability in order of preference:

| Priority | Tool | Image Scan | Dockerfile Lint | Detection Command |
|----------|------|------------|-----------------|-------------------|
| 1 | Trivy | Yes | Yes (config mode) | `trivy --version` |
| 2 | Grype | Yes | No | `grype version` |
| 3 | Snyk Container | Yes | No | `snyk --version` |
| 4 | hadolint | No | Yes | `hadolint --version` |

If no tools are available, produce a single INFO finding with installation instructions.

### Step 3 — Image Vulnerability Scanning

Scan the Docker image for known CVEs in OS packages and application dependencies.

#### Trivy (Preferred)
```bash
trivy image --format json --severity CRITICAL,HIGH,MEDIUM,LOW {image}
```

#### Grype (Fallback)
```bash
grype {image} -o json
```

#### Snyk Container (Alternative)
```bash
snyk container test {image} --json
```

### Step 4 — Dockerfile Linting

Analyze Dockerfile against 7 mandatory security checks:

| Check ID | Check Name | Severity | Fix Recommendation |
|----------|------------|----------|--------------------|
| DOCKER-001 | root-user | HIGH | Add `USER <non-root-user>` directive |
| DOCKER-002 | secrets-in-layers | CRITICAL | Use multi-stage build + .dockerignore |
| DOCKER-003 | latest-tag | MEDIUM | Specify exact image tag |
| DOCKER-004 | no-multi-stage | LOW | Use multi-stage build pattern |
| DOCKER-005 | unnecessary-packages | LOW | Add --no-install-recommends flag |
| DOCKER-006 | excessive-permissions | MEDIUM | Use restrictive permissions (755/644) |
| DOCKER-007 | missing-healthcheck | MEDIUM | Add HEALTHCHECK instruction |

### Step 5 — Combine and Filter Findings

- Merge all findings from image scan and Dockerfile lint into a single collection
- **Severity threshold**: Exclude findings below the threshold
- **Ignore unfixed**: When `--ignore-unfixed` is true, exclude CVE findings where `fixedVersion` is empty

### Step 6 — Generate SARIF Output

Produce SARIF 2.1.0 JSON combining all findings. Write to `results/security/container-scan-YYYY-MM-DD.sarif.json`.

### Step 7 — Calculate Score and Grade

Start at 100, deduct per finding:

| Severity | Deduction |
|----------|-----------|
| CRITICAL | -25 |
| HIGH | -15 |
| MEDIUM | -5 |
| LOW | -2 |

| Score Range | Grade |
|-------------|-------|
| 90-100 | A |
| 80-89 | B |
| 70-79 | C |
| 60-69 | D |
| 0-59 | F |

### Step 8 — Generate Markdown Report

Write report to `results/security/container-scan-YYYY-MM-DD.md`.

## Error Handling

| Scenario | Action |
|----------|--------|
| No scanning tools installed | Return INFO finding with installation instructions, score 100 |
| Image not found locally | Suggest `docker pull {image}` first |
| Dockerfile not found | Report error with provided path |
| Scanner command fails | Report error, continue with other scanners |
| Neither --image nor --dockerfile provided | Error: at least one parameter required |
| Network error during scan | Suggest offline mode or retry |

## CI Integration

### GitHub Actions

```yaml
- name: Container Security Scan
  run: |
    trivy image --format sarif --output trivy-results.sarif $IMAGE
- name: Upload SARIF
  uses: github/codeql-action/upload-sarif@v3
  with:
    sarif_file: trivy-results.sarif
```

### GitLab CI

```yaml
container_scan:
  stage: security
  script:
    - trivy image --format json --output gl-container-scanning-report.json $IMAGE
  artifacts:
    reports:
      container_scanning: gl-container-scanning-report.json
```

### Fail Conditions

| Condition | Action |
|-----------|--------|
| Any CRITICAL CVE found | Fail pipeline (exit 1) |
| Dockerfile score < 70 | Fail pipeline (exit 1) |
| HIGH CVE count > threshold | Configurable warning/fail |
