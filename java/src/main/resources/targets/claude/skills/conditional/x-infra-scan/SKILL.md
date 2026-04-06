---
name: x-infra-scan
description: "Infrastructure Security Scanner — scans Kubernetes manifests, Terraform modules, Helm charts, and Docker Compose files for misconfigurations against CIS benchmarks"
argument-hint: "[--scope k8s|terraform|helm|compose|all] [--benchmark cis-1.8|cis-1.7|custom]"
allowed-tools:
  - Bash
  - Read
  - Write
  - Glob
  - Grep
---

## Global Output Policy

- **Language**: English ONLY. (Ignore input language, always respond in English).
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.
- **Preservation**: All existing technical constraints below must be followed strictly.

# Infrastructure Security Scanner

## Purpose

Scans Infrastructure as Code (IaC) files for security misconfigurations against CIS benchmarks and cloud security best practices. Detects missing security contexts, open security groups, permissive RBAC, plaintext secrets, absent resource limits, and Pod Security Standards violations.

**Condition**: This skill is included when `security.scanning.infraScan: true`.

## Knowledge Pack References

Before scanning, read the relevant conventions:
- `skills/security/references/security-principles.md` — data classification, input validation, fail-secure patterns
- `skills/security/references/security-skill-template.md` — canonical structure, error handling, CI integration
- `skills/security/references/sarif-template.md` — SARIF 2.1.0 output format
- `skills/security/references/security-scoring.md` — scoring model, grade thresholds

## Tool Selection

| IaC Type | Preferred Tool | Fallback Tool | Install Command |
|----------|---------------|--------------|----------------|
| Kubernetes | kube-bench + checkov | kubescape | `pip install checkov` |
| Terraform | checkov | tfsec | `pip install checkov` |
| Helm | checkov | kubescape | `pip install checkov` |
| Docker Compose | checkov | (none) | `pip install checkov` |

### Tool Selection Rules

- Attempt Preferred Tool first, then Fallback Tool
- checkov is the primary tool for all IaC types
- kube-bench provides CIS Kubernetes Benchmark-specific checks
- If neither tool is available, follow Error Handling conventions

## Parameters

| Parameter | Type | Default | Required | Description |
|----------|------|---------|----------|-------------|
| `--scope` | string | `all` | No | IaC type to scan: k8s, terraform, helm, compose, all |
| `--benchmark` | string | `cis-1.8` | No | CIS benchmark version: cis-1.8, cis-1.7, custom |
| `--framework` | string | (auto) | No | Specific checkov framework name |
| `--target` | string | `.` | No | Path to scan |
| `--severity` | string | `CRITICAL,HIGH` | No | Minimum severity to report |
| `--output-dir` | string | `results/security` | No | Output directory for reports |
| `--format` | string | `sarif` | No | Output format (sarif, json, table) |
| `--timeout` | integer | `300` | No | Scan timeout in seconds |
| `--fail-on` | string | `CRITICAL` | No | Severity threshold that causes non-zero exit |

## Auto-Detection Rules

When `--scope all` is used, auto-detect IaC types present in the project:

| IaC Type | Detection Criteria |
|----------|-------------------|
| Kubernetes | YAML files containing both `apiVersion` and `kind` fields |
| Terraform | Files with `.tf` extension |
| Helm | Directory containing `Chart.yaml` |
| Docker Compose | File named `docker-compose.yml` or `compose.yml` |

### Auto-Detection Workflow

1. Scan project root recursively for detection criteria
2. Build list of detected IaC types
3. If no IaC files detected, generate INFO finding and score 100
4. For each detected type, select tool and execute scan

## Checks by IaC Type

### Kubernetes Checks

| Check ID | Check Name | Severity | CIS Reference | Description |
|----------|-----------|----------|--------------|-------------|
| INFRA-001 | missing-security-context | HIGH | CIS-5.2.6 | Container has no securityContext defined |
| INFRA-002 | privilege-escalation-allowed | CRITICAL | CIS-5.2.5 | allowPrivilegeEscalation is true or unset |
| INFRA-003 | run-as-root | HIGH | CIS-5.2.6 | runAsNonRoot is false or unset |
| INFRA-004 | read-only-fs-missing | MEDIUM | CIS-5.2.4 | readOnlyRootFilesystem is false or unset |
| INFRA-005 | missing-network-policy | HIGH | CIS-5.3.2 | Namespace has no NetworkPolicy defined |
| INFRA-006 | rbac-wildcard | CRITICAL | CIS-5.1.3 | ClusterRole uses wildcard (*) permissions |
| INFRA-007 | default-service-account | MEDIUM | CIS-5.1.5 | Pod uses default ServiceAccount |
| INFRA-008 | plaintext-secret | CRITICAL | CIS-5.4.1 | Secret data in plaintext in manifest |
| INFRA-009 | missing-resource-limits | HIGH | CIS-5.2.1 | Container missing CPU/memory requests or limits |
| INFRA-010 | pss-violation | HIGH | CIS-5.2.2 | Pod Security Standards Restricted profile violation |

### Terraform Checks

| Check ID | Check Name | Severity | Description |
|----------|-----------|----------|-------------|
| INFRA-101 | open-security-group | CRITICAL | Security group ingress allows 0.0.0.0/0 |
| INFRA-102 | encryption-disabled | HIGH | Encryption at rest not enabled for storage resource |
| INFRA-103 | public-access-enabled | HIGH | Public access enabled on storage or database |
| INFRA-104 | logging-disabled | MEDIUM | CloudTrail/VPC Flow Logs/audit logging not enabled |
| INFRA-105 | iam-wildcard-policy | CRITICAL | IAM policy uses Action: * or Resource: * |

### Helm Checks

All Kubernetes checks (INFRA-001 through INFRA-010) apply to rendered Helm templates. Additional:

| Check ID | Check Name | Severity | Description |
|----------|-----------|----------|-------------|
| INFRA-201 | insecure-values-default | MEDIUM | values.yaml contains insecure defaults (e.g., securityContext.enabled: false) |

## Output Format

### SARIF Integration

Output MUST follow SARIF 2.1.0 format. Reference `skills/security/references/sarif-template.md`.

**Required fields per finding:**

| Field | Path | Description |
|-------|------|-------------|
| ruleId | `results[].ruleId` | Check ID (e.g., INFRA-001, CIS-5.2.6) |
| level | `results[].level` | SARIF level: error, warning, note, none |
| message | `results[].message.text` | Human-readable finding description |
| location | `results[].locations[].physicalLocation` | File path and line number |
| properties.severity | `results[].properties.severity` | CRITICAL, HIGH, MEDIUM, LOW, INFO |
| properties.iacType | `results[].properties.iacType` | kubernetes, terraform, helm, compose |
| properties.check | `results[].properties.check` | Check name (e.g., missing-security-context) |
| properties.benchmark | `results[].properties.benchmark` | CIS benchmark reference |
| properties.resource | `results[].properties.resource` | IaC resource name (e.g., Deployment/myapp) |
| properties.fixRecommendation | `results[].properties.fixRecommendation` | Remediation guidance |

### Severity Mapping

| Standard Severity | SARIF Level | Score Impact |
|-------------------|-------------|-------------|
| CRITICAL | error | -25 per finding |
| HIGH | error | -15 per finding |
| MEDIUM | warning | -5 per finding |
| LOW | note | -2 per finding |
| INFO | none | 0 (informational) |

### Scoring Integration

Score computed per `skills/security/references/security-scoring.md`.

```
score = max(0, 100 - sum(severity_penalties))
```

**Grade thresholds:**

| Grade | Score Range | CI Gate |
|-------|-----------|---------|
| A | 90-100 | Pass |
| B | 75-89 | Pass (with warnings) |
| C | 50-74 | Fail (configurable) |
| D | 25-49 | Fail |
| F | 0-24 | Fail |

### Markdown Report

```markdown
## Infrastructure Security Scan Report

**Scan Date:** {YYYY-MM-DD HH:MM:SS UTC}
**Scope:** {scanned IaC types}
**Benchmark:** {CIS version}
**Score:** {score}/100 (Grade: {grade})

### Findings Summary

| Severity | Count |
|----------|-------|
| CRITICAL | {n} |
| HIGH     | {n} |
| MEDIUM   | {n} |
| LOW      | {n} |

### Findings by IaC Type

#### Kubernetes ({n} findings)

| # | Check | Severity | File | Resource | Recommendation |
|---|-------|----------|------|----------|----------------|
| 1 | missing-security-context | HIGH | k8s/deployment.yaml:15 | Deployment/myapp | Add securityContext with runAsNonRoot: true |

#### Terraform ({n} findings)

| # | Check | Severity | File | Resource | Recommendation |
|---|-------|----------|------|----------|----------------|
| 1 | open-security-group | CRITICAL | main.tf:42 | aws_security_group.web | Restrict ingress to specific CIDR blocks |
```

## Execution Flow

1. **Check precondition** — Verify `security.scanning.infraScan: true`
2. **Resolve scope** — If `--scope all`, run auto-detection; otherwise use specified type
3. **For each IaC type**:
   a. Select tool (preferred, then fallback)
   b. Execute scan with benchmark parameter
   c. Parse tool output into normalized findings
4. **Combine findings** from all IaC types
5. **Generate SARIF** 2.1.0 combined report
6. **Compute score** using severity penalties
7. **Generate Markdown** report with findings by IaC type
8. **Write outputs** to `results/security/infra-scan-{YYYYMMDD}-{HHMMSS}.sarif.json`

## Error Handling

### Tool Not Found

When the preferred tool is not installed AND the fallback tool is also unavailable:

1. Generate a SARIF report with exactly 1 finding
2. Set finding level to `none` (INFO)
3. Include install instructions in the finding message
4. Set score to 100 (no real vulnerabilities detected)

### Scan Timeout

When the scan exceeds the configured timeout:

1. Generate a partial SARIF report with findings collected so far
2. Add a warning finding with level `warning`
3. Include the timeout value and elapsed time in the message
4. Score based on partial findings only

### Tool Crash

When the scanning tool exits with a non-zero code or produces unparseable output:

1. Capture stderr output
2. Generate a SARIF report with exactly 1 finding
3. Set finding level to `error`
4. Include the exit code and stderr excerpt in the message
5. Score: 0 (unable to verify security posture)

### Zero Findings

When the scan completes successfully with no findings:

1. Generate a valid SARIF report with empty `results[]` array
2. Set score to 100
3. Set grade to A
4. Report as success

### No IaC Files Detected

When `--scope all` and auto-detection finds no IaC files:

1. Generate a SARIF report with exactly 1 finding
2. Set finding level to `none` (INFO)
3. Message: "No IaC files detected in project"
4. Set score to 100

## CI Integration

### GitHub Actions

```yaml
- name: Infrastructure Security Scan
  id: infra-scan
  run: |
    if command -v checkov &> /dev/null; then
      checkov -d . \
        --framework kubernetes terraform helm \
        --output sarif \
        --output-file-path results/security/ \
        --soft-fail
      mv results/security/results_sarif.sarif \
        results/security/infra-scan-$(date +%Y%m%d-%H%M%S).sarif.json
    elif command -v kubescape &> /dev/null; then
      kubescape scan . \
        --format sarif \
        --output results/security/infra-scan-$(date +%Y%m%d-%H%M%S).sarif.json
    else
      echo "::warning::No infrastructure scanning tool available"
    fi

- name: Upload SARIF
  if: always()
  uses: github/codeql-action/upload-sarif@v3
  with:
    sarif_file: results/security/
    category: infra-scan

- name: Upload scan artifacts
  if: always()
  uses: actions/upload-artifact@v4
  with:
    name: security-infra-scan-results
    path: results/security/infra-scan-*.sarif.json
    retention-days: 90
```

### GitLab CI

```yaml
infra-security-scan:
  stage: test
  script:
    - |
      if command -v checkov &> /dev/null; then
        checkov -d . \
          --framework kubernetes terraform helm \
          --output sarif \
          --output-file-path results/security/ \
          --soft-fail
        mv results/security/results_sarif.sarif \
          results/security/infra-scan-$(date +%Y%m%d-%H%M%S).sarif.json
      elif command -v kubescape &> /dev/null; then
        kubescape scan . \
          --format sarif \
          --output results/security/infra-scan-$(date +%Y%m%d-%H%M%S).sarif.json
      else
        echo "WARNING: No infrastructure scanning tool available"
      fi
  artifacts:
    paths:
      - results/security/infra-scan-*.sarif.json
    reports:
      sast: results/security/infra-scan-*.sarif.json
    expire_in: 90 days
  allow_failure: false
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
    - if: '$CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH'
```

### Azure DevOps

```yaml
- task: CmdLine@2
  displayName: 'Infrastructure Security Scan'
  inputs:
    script: |
      if command -v checkov &> /dev/null; then
        checkov -d $(Build.SourcesDirectory) \
          --framework kubernetes terraform helm \
          --output sarif \
          --output-file-path results/security/ \
          --soft-fail
        mv results/security/results_sarif.sarif \
          results/security/infra-scan-$(date +%Y%m%d-%H%M%S).sarif.json
      elif command -v kubescape &> /dev/null; then
        kubescape scan $(Build.SourcesDirectory) \
          --format sarif \
          --output results/security/infra-scan-$(date +%Y%m%d-%H%M%S).sarif.json
      else
        echo "##vso[task.logissue type=warning]No infrastructure scanning tool available"
      fi

- task: PublishBuildArtifacts@1
  displayName: 'Publish infra scan results'
  condition: always()
  inputs:
    PathtoPublish: 'results/security'
    ArtifactName: 'security-infra-scan-results'

- task: PublishTestResults@2
  displayName: 'Publish SARIF results'
  condition: always()
  inputs:
    testResultsFormat: 'NUnit'
    testResultsFiles: 'results/security/infra-scan-*.sarif.json'
    searchFolder: '$(Build.SourcesDirectory)'
```

## Idempotency

### Output Directory Convention

All results written to `results/security/` relative to project root.

### File Naming Convention

```
results/security/infra-scan-{YYYYMMDD}-{HHMMSS}.sarif.json
```

### Idempotency Rules

- Each scan run MUST produce a new dated file (never overwrite previous results)
- The `results/security/` directory MUST be created if it does not exist
- Previous scan results MUST NOT be deleted or modified
- The latest result can be symlinked as `results/security/infra-scan-latest.sarif.json`
- The `.gitignore` SHOULD include `results/security/` to avoid committing scan results

## Usage Examples

```
/x-infra-scan
/x-infra-scan --scope k8s
/x-infra-scan --scope terraform --benchmark cis-1.8
/x-infra-scan --scope all --severity CRITICAL,HIGH,MEDIUM
/x-infra-scan --scope helm --target charts/myapp
```

## Compliance Checklist

- [x] Frontmatter includes name, description, argument-hint, allowed-tools
- [x] Purpose section present with scan description
- [x] Tool Selection table with all columns populated
- [x] Parameters table with type, default, required, description
- [x] Output Format references SARIF template and scoring model
- [x] Error Handling follows 4 conventions (not-found, timeout, crash, zero)
- [x] CI Integration includes GitHub Actions, GitLab CI, Azure DevOps snippets
- [x] Idempotency follows dated filename convention in results/security/
- [x] Auto-detection of IaC types documented
- [x] Checks per IaC type with severity and CIS mapping
