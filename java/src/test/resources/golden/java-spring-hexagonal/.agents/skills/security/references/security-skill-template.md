# Security Skill Template

## Overview

This document defines the canonical structure that ALL security scanning skills MUST follow. It ensures consistency across 13+ scanning skills (SAST, DAST, secret scan, container scan, infra scan, dependency audit, OWASP verification, pentest, etc.) and enables CI pipeline integration without manual configuration.

Every security scanning skill MUST include the sections defined below. Deviations from this template MUST be documented as an ADR with rationale.

## Mandatory SKILL.md Structure

### Frontmatter

```yaml
---
name: x-{scan-type}
description: "{Concise description of what the skill scans}"
argument-hint: "[target path or options]"
allowed-tools:
  - Bash
  - Read
  - Write
  - Glob
  - Grep
---
```

### Required Sections

Every security scanning skill MUST include these sections in order:

1. **Purpose** - What the skill scans and why
2. **Tool Selection** - Preferred and fallback tools per build tool/language
3. **Parameters** - CLI parameters accepted by the skill
4. **Output Format** - SARIF output and scoring integration
5. **Error Handling** - Conventions for tool-not-found, timeout, crash, zero-findings
6. **CI Integration** - Snippets for GitHub Actions, GitLab CI, Azure DevOps
7. **Idempotency** - File naming and output directory conventions

## Tool Selection

Each security scanning skill MUST include a tool-selection table with the following columns:

| Build Tool | Language | Preferred Tool | Fallback Tool | Install Command |
|-----------|----------|---------------|--------------|----------------|
| maven | java | {preferred} | Semgrep | {install cmd} |
| gradle | java | {preferred} | Semgrep | {install cmd} |
| gradle | kotlin | {preferred} | Semgrep | {install cmd} |
| npm | typescript | {preferred} | Semgrep | {install cmd} |
| pip | python | {preferred} | Semgrep | {install cmd} |
| go | go | {preferred} | Semgrep | {install cmd} |
| cargo | rust | {preferred} | Semgrep | {install cmd} |
| dotnet | csharp | {preferred} | Semgrep | {install cmd} |

### Tool Selection Rules

- Every row MUST have all five columns populated
- Fallback MUST be a universal tool (Semgrep, Trivy, or Grype) when possible
- Install Command MUST be a single command that installs the tool
- The skill MUST attempt the Preferred Tool first, then Fallback Tool
- If neither tool is available, the skill MUST follow the Error Handling conventions

## Parameters

Each security scanning skill MUST document its parameters in this format:

| Parameter | Type | Default | Required | Description |
|----------|------|---------|----------|-------------|
| `--target` | string | `.` | No | Path to scan |
| `--severity` | string | `CRITICAL,HIGH` | No | Minimum severity to report |
| `--output-dir` | string | `results/security` | No | Output directory for reports |
| `--format` | string | `sarif` | No | Output format (sarif, json, table) |
| `--timeout` | integer | `300` | No | Scan timeout in seconds |
| `--fail-on` | string | `CRITICAL` | No | Severity threshold that causes non-zero exit |

### Parameter Conventions

- All parameters MUST have sensible defaults
- Maximum 4 required parameters (use parameter object for more)
- `--output-dir` MUST default to `results/security`
- `--format` MUST support `sarif` as the primary format

## Output Format

### SARIF Integration

All scanning skills MUST produce output in SARIF 2.1.0 format. Reference the SARIF template for the exact schema:

> See `references/sarif-template.md` for the complete SARIF 2.1.0 schema, required fields, and examples.

**Required SARIF fields per finding:**

| Field | Path | Description |
|-------|------|-------------|
| ruleId | `results[].ruleId` | Unique rule identifier (e.g., `CWE-79`) |
| level | `results[].level` | Severity: `error`, `warning`, `note`, `none` |
| message | `results[].message.text` | Human-readable finding description |
| location | `results[].locations[].physicalLocation` | File path and line number |
| properties.severity | `results[].properties.severity` | CRITICAL, HIGH, MEDIUM, LOW, INFO |

### Severity Mapping

Map tool-native severity to the standardized levels:

| Standard Severity | SARIF Level | Score Impact |
|-------------------|-------------|-------------|
| CRITICAL | error | -25 per finding |
| HIGH | error | -15 per finding |
| MEDIUM | warning | -5 per finding |
| LOW | note | -2 per finding |
| INFO | none | 0 (informational) |

### Scoring Integration

> See `references/security-scoring.md` for the complete scoring model, grade thresholds, and aggregation rules.

The skill MUST compute a security score using this formula:

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

## Error Handling

### Tool Not Found

When the preferred tool is not installed AND the fallback tool is also unavailable:

1. Generate a SARIF report with exactly 1 finding
2. Set finding level to `none` (INFO)
3. Include install instructions in the finding message
4. Set score to 100 (no real vulnerabilities detected)

**Example finding message:**
```
Tool '{tool_name}' is not installed. Install with: {install_command}
Fallback tool '{fallback_name}' is also not available. Install with: {fallback_install_command}
No security scan was performed. Score: 100 (no findings).
```

### Scan Timeout

When the scan exceeds the configured timeout:

1. Generate a partial SARIF report with findings collected so far
2. Add a warning finding with level `warning`
3. Include the timeout value and elapsed time in the message
4. Score based on partial findings only

**Example finding message:**
```
Scan timed out after {timeout}s. Partial results: {count} findings from {scanned}/{total} files.
Review partial results and consider increasing --timeout or reducing scan scope.
```

### Tool Crash

When the scanning tool exits with a non-zero code or produces unparseable output:

1. Capture stderr output
2. Generate a SARIF report with exactly 1 finding
3. Set finding level to `error`
4. Include the exit code and stderr excerpt in the message
5. Score: 0 (unable to verify security posture)

**Example finding message:**
```
Tool '{tool_name}' crashed with exit code {exit_code}.
stderr: {first_500_chars_of_stderr}
Unable to complete security scan. Manual investigation required.
```

### Zero Findings

When the scan completes successfully with no findings:

1. Generate a valid SARIF report with empty `results[]` array
2. Set score to 100
3. Set grade to A
4. Report as success

## CI Integration

### GitHub Actions

```yaml
- name: Security Scan ({scan_type})
  id: security-scan
  run: |
    # Attempt preferred tool
    if command -v {preferred_tool} &> /dev/null; then
      {preferred_tool} {scan_command} \
        --format sarif \
        --output results/security/{scan_type}-$(date +%Y%m%d-%H%M%S).sarif.json \
        {target}
    # Fallback to universal tool
    elif command -v {fallback_tool} &> /dev/null; then
      {fallback_tool} {fallback_command} \
        --sarif \
        --output results/security/{scan_type}-$(date +%Y%m%d-%H%M%S).sarif.json \
        {target}
    else
      echo "::warning::No scanning tool available for {scan_type}"
      # Generate INFO-level SARIF with install instructions
    fi

- name: Upload SARIF
  if: always()
  uses: github/codeql-action/upload-sarif@v3
  with:
    sarif_file: results/security/
    category: {scan_type}

- name: Upload scan artifacts
  if: always()
  uses: actions/upload-artifact@v4
  with:
    name: security-{scan_type}-results
    path: results/security/{scan_type}-*.sarif.json
    retention-days: 90
```

**Required secrets:** None (tools run locally). Optional: `SEMGREP_APP_TOKEN` for Semgrep Cloud.

### GitLab CI

```yaml
{scan_type}-security-scan:
  stage: test
  script:
    - |
      if command -v {preferred_tool} &> /dev/null; then
        {preferred_tool} {scan_command} \
          --format sarif \
          --output results/security/{scan_type}-$(date +%Y%m%d-%H%M%S).sarif.json \
          {target}
      elif command -v {fallback_tool} &> /dev/null; then
        {fallback_tool} {fallback_command} \
          --sarif \
          --output results/security/{scan_type}-$(date +%Y%m%d-%H%M%S).sarif.json \
          {target}
      else
        echo "WARNING: No scanning tool available for {scan_type}"
      fi
  artifacts:
    paths:
      - results/security/{scan_type}-*.sarif.json
    reports:
      sast: results/security/{scan_type}-*.sarif.json
    expire_in: 90 days
  allow_failure: false
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
    - if: '$CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH'
```

### Azure DevOps

```yaml
- task: CmdLine@2
  displayName: 'Security Scan ({scan_type})'
  inputs:
    script: |
      if command -v {preferred_tool} &> /dev/null; then
        {preferred_tool} {scan_command} \
          --format sarif \
          --output results/security/{scan_type}-$(date +%Y%m%d-%H%M%S).sarif.json \
          $(Build.SourcesDirectory)
      elif command -v {fallback_tool} &> /dev/null; then
        {fallback_tool} {fallback_command} \
          --sarif \
          --output results/security/{scan_type}-$(date +%Y%m%d-%H%M%S).sarif.json \
          $(Build.SourcesDirectory)
      else
        echo "##vso[task.logissue type=warning]No scanning tool available"
      fi

- task: PublishBuildArtifacts@1
  displayName: 'Publish security results'
  condition: always()
  inputs:
    PathtoPublish: 'results/security'
    ArtifactName: 'security-{scan_type}-results'

- task: PublishTestResults@2
  displayName: 'Publish SARIF results'
  condition: always()
  inputs:
    testResultsFormat: 'NUnit'
    testResultsFiles: 'results/security/{scan_type}-*.sarif.json'
    searchFolder: '$(Build.SourcesDirectory)'
```

## Idempotency

### Output Directory Convention

All security scanning skills MUST write results to `results/security/` relative to the project root.

### File Naming Convention

```
results/security/{scan_type}-{YYYYMMDD}-{HHMMSS}.sarif.json
```

**Examples:**
- `results/security/sast-20260405-143022.sarif.json`
- `results/security/dependency-audit-20260405-143022.sarif.json`
- `results/security/container-scan-20260405-143022.sarif.json`

### Idempotency Rules

- Each scan run MUST produce a new dated file (never overwrite previous results)
- The `results/security/` directory MUST be created if it does not exist
- Previous scan results MUST NOT be deleted or modified
- The latest result can be symlinked as `results/security/{scan_type}-latest.sarif.json`
- The `.gitignore` SHOULD include `results/security/` to avoid committing scan results

## Example: SAST Skill Using This Template

```markdown
---
name: x-security-sast
description: "Static Application Security Testing — scans source code for security vulnerabilities"
argument-hint: "[target path]"
allowed-tools:
  - Bash
  - Read
  - Write
  - Glob
  - Grep
---

# SAST Scan

## Purpose

Performs static analysis of source code to identify security vulnerabilities,
coding errors, and compliance violations without executing the application.

## Tool Selection

| Build Tool | Language | Preferred Tool | Fallback Tool | Install Command |
|-----------|----------|---------------|--------------|----------------|
| maven | java | SpotBugs + FindSecBugs | Semgrep | mvn dependency:resolve |
| gradle | java | SpotBugs + FindSecBugs | Semgrep | gradle dependencies |
| gradle | kotlin | detekt-security | Semgrep | gradle dependencies |
| npm | typescript | ESLint security plugin | Semgrep | npm install |
| pip | python | Bandit | Semgrep | pip install bandit |
| go | go | gosec | Semgrep | go install github.com/securego/gosec/v2/cmd/gosec@latest |
| cargo | rust | cargo-audit | Semgrep | cargo install cargo-audit |
| dotnet | csharp | Security Code Scan | Semgrep | dotnet add package SecurityCodeScan.VS2019 |

## Parameters

| Parameter | Type | Default | Required | Description |
|----------|------|---------|----------|-------------|
| `--target` | string | `.` | No | Path to scan |
| `--severity` | string | `CRITICAL,HIGH` | No | Minimum severity |
| `--output-dir` | string | `results/security` | No | Output directory |
| `--format` | string | `sarif` | No | Output format |

## Output Format

Results in SARIF 2.1.0 format. See `references/sarif-template.md`.
Score computed per `references/security-scoring.md`.

## Error Handling

Per security skill template conventions. See `references/security-skill-template.md`.

## CI Integration

Per security skill template CI snippets. See `references/security-skill-template.md`.
```

## Template Compliance Checklist

Use this checklist to verify a security scanning skill conforms to this template:

- [ ] Frontmatter includes name, description, argument-hint, allowed-tools
- [ ] Purpose section present with scan description
- [ ] Tool Selection table with all 5 columns populated
- [ ] Parameters table with type, default, required, description
- [ ] Output Format references SARIF template and scoring model
- [ ] Error Handling follows the 4 conventions (not-found, timeout, crash, zero)
- [ ] CI Integration includes GitHub Actions, GitLab CI, Azure DevOps snippets
- [ ] Idempotency follows dated filename convention in results/security/
- [ ] Fallback tool is a universal scanner (Semgrep, Trivy, or Grype)
- [ ] All parameters have sensible defaults
