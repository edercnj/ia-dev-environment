---
name: x-container-scan
description: "Scans Docker images for CVEs and Dockerfile best practices violations. Uses Trivy (primary), Grype (alternative), or Snyk Container for image vulnerability scanning and Dockerfile linting. Produces SARIF output with scoring and CI integration."
allowed-tools: Read, Write, Bash, Grep, Glob
argument-hint: "[--image name:tag] [--dockerfile path] [--severity-threshold CRITICAL|HIGH|MEDIUM|LOW] [--ignore-unfixed]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Container Security Scanner

## Purpose

Scans Docker container images for known CVEs and analyzes Dockerfiles for security best practices violations. Generates a combined SARIF 2.1.0 report with severity scoring and grade assignment.

## Triggers

- `/x-container-scan --image myapp:1.0` -- scan image for vulnerabilities
- `/x-container-scan --dockerfile ./Dockerfile` -- lint Dockerfile for best practices
- `/x-container-scan --image myapp:1.0 --dockerfile ./Dockerfile` -- combined scan
- `/x-container-scan --image myapp:1.0 --ignore-unfixed` -- exclude CVEs without available fix
- `/x-container-scan --image myapp:1.0 --severity-threshold HIGH` -- filter by minimum severity

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `--image` | String | One of image/dockerfile required | (none) | Docker image name:tag to scan |
| `--dockerfile` | String | One of image/dockerfile required | ./Dockerfile | Path to Dockerfile to analyze |
| `--severity-threshold` | Enum | No | LOW | Minimum severity: CRITICAL, HIGH, MEDIUM, LOW |
| `--ignore-unfixed` | Boolean | No | false | Exclude CVEs without an available fix |

## Workflow

```
1. VALIDATE   -> Check parameters, verify at least --image or --dockerfile
2. DETECT     -> Detect available scanning tools (Trivy > Grype > Snyk)
3. IMAGE-SCAN -> Scan container image for CVEs (if --image provided)
4. DF-LINT    -> Lint Dockerfile for best practices (if --dockerfile provided)
5. COMBINE    -> Merge findings from image scan and Dockerfile lint
6. FILTER     -> Apply --severity-threshold and --ignore-unfixed filters
7. SARIF      -> Generate SARIF 2.1.0 JSON output
8. SCORE      -> Calculate security score and grade
9. REPORT     -> Generate Markdown report with summary, findings, and score
```

### Step 1 -- Validate Parameters

- At least one of `--image` or `--dockerfile` MUST be provided
- If `--image` is provided, validate name:tag format
- If `--dockerfile` is provided, validate file exists at path

### Step 2 -- Detect Scanning Tools

Check tool availability in order of preference:

| Priority | Tool | Image Scan | Dockerfile Lint | Detection Command |
|----------|------|------------|-----------------|-------------------|
| 1 | Trivy | Yes | Yes (config mode) | `trivy --version` |
| 2 | Grype | Yes | No | `grype version` |
| 3 | Snyk Container | Yes | No | `snyk --version` |
| 4 | hadolint | No | Yes | `hadolint --version` |

If no tools are available, produce a single INFO finding with installation instructions:

```
Install Trivy (recommended):
  brew install trivy        # macOS
  apt install trivy         # Debian/Ubuntu
  https://aquasecurity.github.io/trivy/
```

### Step 3 -- Image Vulnerability Scanning

Scan the Docker image for known CVEs in OS packages and application dependencies.

#### Trivy (Preferred)

```bash
trivy image --format json --severity CRITICAL,HIGH,MEDIUM,LOW {image}
trivy image --format json --severity CRITICAL,HIGH,MEDIUM,LOW --ignore-unfixed {image}
```

#### Grype (Fallback)

```bash
grype {image} -o json
```

#### Snyk Container (Alternative)

```bash
snyk container test {image} --json
```

#### CVE Finding Schema

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| ruleId | String | Yes | CVE identifier (e.g., CVE-2024-21626) |
| severity | Enum | Yes | CRITICAL, HIGH, MEDIUM, LOW |
| packageName | String | Yes | Affected package name |
| installedVersion | String | Yes | Currently installed version |
| fixedVersion | String | No | Version with fix (empty if unfixed) |
| message | String | Yes | CVE description |

### Step 4 -- Dockerfile Linting

Analyze Dockerfile against 7 mandatory security checks.

#### Trivy Config Mode (Preferred)

```bash
trivy config --format json {dockerfile_dir}
```

#### hadolint (Fallback)

```bash
hadolint --format json {dockerfile}
```

#### Dockerfile Checks

| Check ID | Check Name | Severity | Detection | Fix Recommendation |
|----------|------------|----------|-----------|-------------------|
| DOCKER-001 | root-user | HIGH | No USER directive with non-root user | Add `USER <non-root-user>` directive |
| DOCKER-002 | secrets-in-layers | CRITICAL | ADD/COPY of .env, .key, .pem, credentials files | Use multi-stage build + .dockerignore |
| DOCKER-003 | latest-tag | MEDIUM | FROM with :latest or no tag | Specify exact image tag |
| DOCKER-004 | no-multi-stage | LOW | Single FROM without multi-stage build | Use multi-stage build pattern |
| DOCKER-005 | unnecessary-packages | LOW | apt-get/apk without --no-install-recommends | Add --no-install-recommends flag |
| DOCKER-006 | excessive-permissions | MEDIUM | chmod 777 or similarly permissive | Use restrictive permissions (755/644) |
| DOCKER-007 | missing-healthcheck | MEDIUM | No HEALTHCHECK instruction | Add HEALTHCHECK instruction |

#### Dockerfile Finding Schema

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| ruleId | String | Yes | Check ID (e.g., DOCKER-001) |
| severity | Enum | Yes | CRITICAL, HIGH, MEDIUM, LOW |
| check | String | Yes | Check name (e.g., root-user) |
| line | int | Yes | Line number in Dockerfile (> 0) |
| message | String | Yes | Finding description |
| fixRecommendation | String | Yes | Remediation guidance |

### Step 5 -- Combine Findings

Merge all findings from image scan and Dockerfile lint into a single collection.

### Step 6 -- Apply Filters

- **Severity threshold**: Exclude findings below the threshold (CRITICAL > HIGH > MEDIUM > LOW)
- **Ignore unfixed**: When `--ignore-unfixed` is true, exclude CVE findings where `fixedVersion` is empty

### Step 7 -- Generate SARIF Output

Produce SARIF 2.1.0 JSON combining all findings:

```json
{
  "$schema": "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/main/sarif-2.1/schema/sarif-schema-2.1.0.json",
  "version": "2.1.0",
  "runs": [{
    "tool": {
      "driver": {
        "name": "x-container-scan",
        "version": "1.0.0",
        "rules": []
      }
    },
    "results": []
  }]
}
```

Write SARIF to `results/security/container-scan-YYYY-MM-DD.sarif.json`.

### Step 8 -- Calculate Score

#### Dockerfile Score (0-100)

Start at 100, deduct per finding:

| Severity | Deduction |
|----------|-----------|
| CRITICAL | -25 |
| HIGH | -15 |
| MEDIUM | -5 |
| LOW | -2 |

Minimum score is 0. A Dockerfile following all best practices scores >= 90.

#### Grade Assignment

| Score Range | Grade |
|-------------|-------|
| 90-100 | A |
| 80-89 | B |
| 70-79 | C |
| 60-69 | D |
| 0-59 | F |

### Step 9 -- Generate Markdown Report

Write report to `results/security/container-scan-YYYY-MM-DD.md`:

```markdown
# Container Security Scan Report

**Date:** YYYY-MM-DD
**Image:** {image_name_tag}
**Dockerfile:** {dockerfile_path}
**Scanner:** {tool_name} {tool_version}

## Summary

| Category | CRITICAL | HIGH | MEDIUM | LOW | Total |
|----------|----------|------|--------|-----|-------|
| Image CVEs | N | N | N | N | N |
| Dockerfile | N | N | N | N | N |

## Image Vulnerability Findings

| CVE ID | Package | Installed | Fixed | Severity |
|--------|---------|-----------|-------|----------|
| CVE-YYYY-NNNNN | {pkg} | {ver} | {fix_ver} | CRITICAL |

## Dockerfile Findings

| Check | Severity | Line | Finding | Fix |
|-------|----------|------|---------|-----|
| root-user | HIGH | N | {msg} | {fix} |

## Score

- **Dockerfile Score:** {score}/100
- **Grade:** {grade}

## Recommendations

1. **Immediate:** Fix CRITICAL CVEs and Dockerfile violations
2. **Short-term:** Remediate HIGH-severity findings
3. **Long-term:** Address MEDIUM/LOW findings, adopt multi-stage builds
```

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
    # Upload SARIF to GitHub Security tab
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
