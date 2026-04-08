---
name: x-infra-scan
description: "Scans Kubernetes manifests, Terraform modules, Helm charts, and Docker Compose files for misconfigurations against CIS benchmarks."
user-invocable: true
allowed-tools: Bash, Read, Write, Glob, Grep
argument-hint: "[--scope k8s|terraform|helm|compose|all] [--benchmark cis-1.8|cis-1.7|custom]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Infrastructure Security Scanner

## Purpose

Scan Infrastructure as Code (IaC) files for security misconfigurations against CIS benchmarks and cloud security best practices. Detect missing security contexts, open security groups, permissive RBAC, plaintext secrets, absent resource limits, and Pod Security Standards violations.

## Activation Condition

Include this skill when `security.scanning.infraScan: true` in the project configuration.

## Triggers

- `/x-infra-scan` -- scan all IaC types auto-detected in project
- `/x-infra-scan --scope k8s` -- scan Kubernetes manifests only
- `/x-infra-scan --scope terraform --benchmark cis-1.8` -- scan Terraform with specific benchmark
- `/x-infra-scan --scope helm --target charts/myapp` -- scan a specific Helm chart

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `--scope` | String | No | `all` | IaC type: `k8s`, `terraform`, `helm`, `compose`, `all` |
| `--benchmark` | String | No | `cis-1.8` | CIS benchmark version: `cis-1.8`, `cis-1.7`, `custom` |
| `--framework` | String | No | (auto) | Specific checkov framework name |
| `--target` | String | No | `.` | Path to scan |
| `--severity` | String | No | `CRITICAL,HIGH` | Minimum severity to report |
| `--output-dir` | String | No | `results/security` | Output directory for reports |
| `--format` | String | No | `sarif` | Output format: `sarif`, `json`, `table` |
| `--timeout` | Integer | No | `300` | Scan timeout in seconds |
| `--fail-on` | String | No | `CRITICAL` | Severity threshold that causes non-zero exit |

## Knowledge Pack References

| Pack | Files | Purpose |
|------|-------|---------|
| security | `skills/security/references/security-principles.md` | Data classification, input validation, fail-secure patterns |
| security | `skills/security/references/security-skill-template.md` | Canonical structure, error handling, CI integration |
| security | `skills/security/references/sarif-template.md` | SARIF 2.1.0 output format |
| security | `skills/security/references/security-scoring.md` | Scoring model, grade thresholds |

## Workflow

### Step 1 — Resolve Scope

If `--scope all`, run auto-detection:

| IaC Type | Detection Criteria |
|----------|-------------------|
| Kubernetes | YAML files containing both `apiVersion` and `kind` fields |
| Terraform | Files with `.tf` extension |
| Helm | Directory containing `Chart.yaml` |
| Docker Compose | File named `docker-compose.yml` or `compose.yml` |

If no IaC files detected, generate INFO finding and score 100.

### Step 2 — Select Tool

| IaC Type | Preferred Tool | Fallback Tool | Install Command |
|----------|---------------|--------------|----------------|
| Kubernetes | kube-bench + checkov | kubescape | `pip install checkov` |
| Terraform | checkov | tfsec | `pip install checkov` |
| Helm | checkov | kubescape | `pip install checkov` |
| Docker Compose | checkov | (none) | `pip install checkov` |

### Step 3 — Execute Scans

For each detected IaC type, execute scan with benchmark parameter and parse tool output into normalized findings.

### Step 4 — Combine and Score

Combine findings from all IaC types. Compute score:

```
score = max(0, 100 - sum(severity_penalties))
```

| Severity | Penalty |
|----------|---------|
| CRITICAL | -25 |
| HIGH | -15 |
| MEDIUM | -5 |
| LOW | -2 |

| Grade | Score Range | CI Gate |
|-------|-----------|---------|
| A | 90-100 | Pass |
| B | 75-89 | Pass (with warnings) |
| C | 50-74 | Fail (configurable) |
| D | 25-49 | Fail |
| F | 0-24 | Fail |

### Step 5 — Generate Reports

- SARIF 2.1.0 to `results/security/infra-scan-{YYYYMMDD}-{HHMMSS}.sarif.json`
- Markdown report with findings by IaC type

## Checks by IaC Type

### Kubernetes Checks

| Check ID | Check Name | Severity | CIS Reference |
|----------|-----------|----------|--------------|
| INFRA-001 | missing-security-context | HIGH | CIS-5.2.6 |
| INFRA-002 | privilege-escalation-allowed | CRITICAL | CIS-5.2.5 |
| INFRA-003 | run-as-root | HIGH | CIS-5.2.6 |
| INFRA-004 | read-only-fs-missing | MEDIUM | CIS-5.2.4 |
| INFRA-005 | missing-network-policy | HIGH | CIS-5.3.2 |
| INFRA-006 | rbac-wildcard | CRITICAL | CIS-5.1.3 |
| INFRA-007 | default-service-account | MEDIUM | CIS-5.1.5 |
| INFRA-008 | plaintext-secret | CRITICAL | CIS-5.4.1 |
| INFRA-009 | missing-resource-limits | HIGH | CIS-5.2.1 |
| INFRA-010 | pss-violation | HIGH | CIS-5.2.2 |

### Terraform Checks

| Check ID | Check Name | Severity |
|----------|-----------|----------|
| INFRA-101 | open-security-group | CRITICAL |
| INFRA-102 | encryption-disabled | HIGH |
| INFRA-103 | public-access-enabled | HIGH |
| INFRA-104 | logging-disabled | MEDIUM |
| INFRA-105 | iam-wildcard-policy | CRITICAL |

### Helm Checks

All Kubernetes checks (INFRA-001 through INFRA-010) apply to rendered Helm templates. Additional:

| Check ID | Check Name | Severity |
|----------|-----------|----------|
| INFRA-201 | insecure-values-default | MEDIUM |

## Error Handling

| Scenario | Action |
|----------|--------|
| Tool not found (preferred and fallback) | Generate INFO SARIF finding with install instructions, score 100 |
| Scan timeout | Generate partial SARIF with findings so far, add warning finding |
| Tool crash (non-zero exit) | Capture stderr, generate error SARIF finding, score 0 |
| Zero findings | Generate valid SARIF with empty results, score 100, grade A |
| No IaC files detected | Generate INFO finding: "No IaC files detected in project", score 100 |

## Idempotency

- Each scan run produces a new dated file (never overwrite)
- Create `results/security/` directory if it does not exist
- Previous scan results are NOT deleted or modified
- File naming: `results/security/infra-scan-{YYYYMMDD}-{HHMMSS}.sarif.json`

## CI Integration

### GitHub Actions

```yaml
- name: Infrastructure Security Scan
  run: |
    if command -v checkov &> /dev/null; then
      checkov -d . --framework kubernetes terraform helm --output sarif --output-file-path results/security/ --soft-fail
    fi
- name: Upload SARIF
  if: always()
  uses: github/codeql-action/upload-sarif@v3
  with:
    sarif_file: results/security/
    category: infra-scan
```

### GitLab CI

```yaml
infra-security-scan:
  stage: test
  script:
    - checkov -d . --framework kubernetes terraform helm --output sarif --output-file-path results/security/ --soft-fail
  artifacts:
    paths:
      - results/security/infra-scan-*.sarif.json
    reports:
      sast: results/security/infra-scan-*.sarif.json
```
