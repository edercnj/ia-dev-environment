---
name: x-secret-scan
description: "Scans code and git history for leaked credentials, API keys, tokens, and secrets. Produces SARIF output with scoring and CI integration."
argument-hint: "[--scope current|history|both] [--baseline path] [--since-commit SHA] [--format sarif|markdown|both]"
allowed-tools:
  - Bash
  - Read
  - Write
  - Glob
  - Grep
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Secret Scanner

## Purpose

Detects leaked secrets (API keys, tokens, passwords, certificates, connection strings) in the current codebase and git history. Credential leakage is among the most common causes of security breaches. Even secrets removed in later commits remain accessible in git history, making historical scanning essential.

This skill produces SARIF 2.1.0 output, computes a security score and grade, and supports a baseline system for excluding known false positives.

## Knowledge Pack References

Read these before starting the scan:
- `skills/security/references/sarif-template.md` -- SARIF 2.1.0 schema and required fields
- `skills/security/references/security-scoring.md` -- scoring model and grade thresholds
- `skills/security/references/security-skill-template.md` -- canonical structure for security skills

## Tool Selection

| Build Tool | Language | Preferred Tool | Fallback Tool | Install Command |
|-----------|----------|---------------|--------------|----------------|
| maven | java | gitleaks | trufflehog | `brew install gitleaks` or `go install github.com/gitleaks/gitleaks/v8@latest` |
| gradle | java | gitleaks | trufflehog | `brew install gitleaks` or `go install github.com/gitleaks/gitleaks/v8@latest` |
| gradle | kotlin | gitleaks | trufflehog | `brew install gitleaks` or `go install github.com/gitleaks/gitleaks/v8@latest` |
| npm | typescript | gitleaks | trufflehog | `brew install gitleaks` or `npm install -g gitleaks` |
| pip | python | gitleaks | detect-secrets | `brew install gitleaks` or `pip install detect-secrets` |
| go | go | gitleaks | trufflehog | `brew install gitleaks` or `go install github.com/gitleaks/gitleaks/v8@latest` |
| cargo | rust | gitleaks | trufflehog | `brew install gitleaks` or `cargo install gitleaks` |
| dotnet | csharp | gitleaks | trufflehog | `brew install gitleaks` or `dotnet tool install -g gitleaks` |

### Tool Selection Rules

1. Check if `gitleaks` is available: `command -v gitleaks`
2. If not, check `trufflehog`: `command -v trufflehog`
3. For Python projects, also check `detect-secrets`: `command -v detect-secrets`
4. If no tool is available, generate an INFO-level SARIF finding with install instructions

## Parameters

| Parameter | Type | Default | Required | Description |
|----------|------|---------|----------|-------------|
| `--scope` | string | `current` | No | Scan scope: `current` (working tree only), `history` (git log), `both` |
| `--baseline` | string | `.security-baseline.json` | No | Path to baseline file with accepted false positives |
| `--since-commit` | string | (none) | No | SHA of the commit from which to scan history (40 hex chars) |
| `--format` | string | `both` | No | Output format: `sarif`, `markdown`, `both` |
| `--output-dir` | string | `results/security` | No | Output directory for reports |
| `--severity` | string | `CRITICAL,HIGH,MEDIUM` | No | Minimum severity to report |
| `--timeout` | integer | `300` | No | Scan timeout in seconds |
| `--fail-on` | string | `CRITICAL` | No | Severity threshold that causes non-zero exit |

### Parameter Validation

- `--scope` MUST be one of: `current`, `history`, `both`
- `--since-commit` MUST be a valid 7-40 character hex string when provided
- `--format` MUST be one of: `sarif`, `markdown`, `both`
- `--baseline` path MUST exist if specified (skip baseline filtering otherwise)

## Secret Categories

The scanner MUST detect the following 8 categories of secrets:

### SECRET-001: AWS Credentials (CRITICAL)

| Pattern | Example | Redacted |
|---------|---------|----------|
| AWS Access Key ID | `AKIAIOSFODNN7EXAMPLE` | `AKIA****EXAMPLE` |
| AWS Secret Access Key | `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY` | `wJal****EKEY` |
| AWS Session Token | `FwoGZXIvYXdzE...` | `FwoG****...` |

### SECRET-002: GCP Credentials (CRITICAL)

| Pattern | Example | Redacted |
|---------|---------|----------|
| Service Account Key | `"private_key": "-----BEGIN RSA..."` | `"private_key": "***REDACTED***"` |
| GCP API Key | `AIzaSy...` | `AIza****...` |
| OAuth Client Secret | `GOCSPX-...` | `GOCS****...` |

### SECRET-003: Azure Credentials (CRITICAL)

| Pattern | Example | Redacted |
|---------|---------|----------|
| Storage Account Key | `DefaultEndpointsProtocol=https;AccountKey=...` | `***AccountKey=REDACTED***` |
| SAS Token | `sv=2021-06-08&ss=bfqt&srt=sco&sp=...&sig=...` | `sv=****&sig=REDACTED` |
| Client Secret | `~...` (40+ chars) | `~****...` |

### SECRET-004: API Tokens (HIGH)

| Pattern | Example | Redacted |
|---------|---------|----------|
| GitHub Token | `ghp_xxxxxxxxxxxx` | `ghp_****xxxx` |
| GitLab Token | `glpat-xxxxxxxxxxxx` | `glpat-****xxxx` |
| Slack Token | `xoxb-xxxxxxxxxxxx` | `xoxb-****xxxx` |
| Stripe Secret | `sk_live_xxxxxxxxxxxx` | `sk_live_****xxxx` |
| Twilio Auth Token | `(32 hex chars)` | `****xxxx` |
| SendGrid API Key | `SG.xxxxxxxxxxxx` | `SG.****xxxx` |

### SECRET-005: Private Keys (CRITICAL)

| Pattern | Example | Redacted |
|---------|---------|----------|
| RSA Private Key | `-----BEGIN RSA PRIVATE KEY-----` | `***RSA KEY REDACTED***` |
| ECDSA Private Key | `-----BEGIN EC PRIVATE KEY-----` | `***EC KEY REDACTED***` |
| Ed25519 Private Key | `-----BEGIN OPENSSH PRIVATE KEY-----` | `***SSH KEY REDACTED***` |
| PGP Private Key | `-----BEGIN PGP PRIVATE KEY BLOCK-----` | `***PGP KEY REDACTED***` |

### SECRET-006: Hardcoded Passwords (HIGH)

| Pattern | Example | Redacted |
|---------|---------|----------|
| Password in config | `password: mysecretpass` | `password: ****` |
| Password in properties | `db.password=secretpass` | `db.password=****` |
| Password in env file | `DB_PASSWORD=secretpass` | `DB_PASSWORD=****` |

### SECRET-007: JWT Tokens (MEDIUM)

| Pattern | Example | Redacted |
|---------|---------|----------|
| Hardcoded JWT | `eyJhbGciOiJIUzI1NiIs...` | `eyJh****...` |

### SECRET-008: Database Connection Strings (HIGH)

| Pattern | Example | Redacted |
|---------|---------|----------|
| PostgreSQL | `postgresql://user:pass@host/db` | `postgresql://user:****@host/db` |
| MySQL | `mysql://user:pass@host/db` | `mysql://user:****@host/db` |
| MongoDB | `mongodb://user:pass@host/db` | `mongodb://user:****@host/db` |
| Redis | `redis://:pass@host:6379` | `redis://:****@host:6379` |

## Workflow

### Step 1: Tool Detection

```bash
# Check preferred tool
if command -v gitleaks &> /dev/null; then
  TOOL="gitleaks"
  TOOL_VERSION=$(gitleaks version 2>/dev/null || echo "unknown")
# Check fallback
elif command -v trufflehog &> /dev/null; then
  TOOL="trufflehog"
  TOOL_VERSION=$(trufflehog --version 2>/dev/null || echo "unknown")
else
  # No tool available — generate INFO finding
  TOOL="none"
fi
```

### Step 2: Execute Scan

**gitleaks (current):**
```bash
gitleaks detect --source . --report-format sarif \
  --report-path results/security/secret-scan-$(date +%Y%m%d-%H%M%S).sarif.json \
  --no-banner
```

**gitleaks (history):**
```bash
gitleaks detect --source . --report-format sarif \
  --report-path results/security/secret-scan-$(date +%Y%m%d-%H%M%S).sarif.json \
  --log-opts="--all" --no-banner
```

**gitleaks (history with since-commit):**
```bash
gitleaks detect --source . --report-format sarif \
  --report-path results/security/secret-scan-$(date +%Y%m%d-%H%M%S).sarif.json \
  --log-opts="--since-commit={SHA}" --no-banner
```

**trufflehog (current):**
```bash
trufflehog filesystem --directory . --format sarif \
  > results/security/secret-scan-$(date +%Y%m%d-%H%M%S).sarif.json
```

**trufflehog (history):**
```bash
trufflehog git file://. --format sarif \
  > results/security/secret-scan-$(date +%Y%m%d-%H%M%S).sarif.json
```

### Step 3: Load Baseline

If `--baseline` file exists, load it and extract fingerprints:

```json
{
  "version": "1.0",
  "entries": [
    {
      "fingerprint": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
      "reason": "Test fixture, not real credential",
      "approvedBy": "security-team",
      "approvedDate": "2026-01-15"
    }
  ]
}
```

### Step 4: Filter Findings

- Remove findings whose fingerprint matches a baseline entry
- Compute fingerprint: SHA-256 of `ruleId + file + line + redactedMatch`

### Step 5: Redact Secrets

Every finding MUST have `redactedMatch` that masks the secret:
- Show first 4 characters of the secret
- Replace middle with `****`
- Show last 4 characters
- For secrets shorter than 12 characters, show first 2 and last 2

### Step 6: Generate SARIF Output

Produce SARIF 2.1.0 with all findings mapped to secret categories:

| Category | SARIF ruleId | SARIF Level | Severity |
|----------|-------------|-------------|----------|
| aws | SECRET-001 | error | CRITICAL |
| gcp | SECRET-002 | error | CRITICAL |
| azure | SECRET-003 | error | CRITICAL |
| api-token | SECRET-004 | error | HIGH |
| private-key | SECRET-005 | error | CRITICAL |
| password | SECRET-006 | error | HIGH |
| jwt | SECRET-007 | warning | MEDIUM |
| database | SECRET-008 | error | HIGH |

### Step 7: Compute Score

Per `references/security-scoring.md`:

```
score = max(0, 100 - sum(severity_penalties))

Penalties:
  CRITICAL: -25 per finding
  HIGH:     -15 per finding
  MEDIUM:    -5 per finding
  LOW:       -2 per finding
  INFO:       0
```

| Grade | Score Range | CI Gate |
|-------|-----------|---------|
| A | 90-100 | Pass |
| B | 75-89 | Pass (with warnings) |
| C | 50-74 | Fail (configurable) |
| D | 25-49 | Fail |
| F | 0-24 | Fail |

## Output Format

### SARIF Output

File: `results/security/secret-scan-{YYYYMMDD}-{HHMMSS}.sarif.json`

```json
{
  "$schema": "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/main/sarif-2.1/schema/sarif-schema-2.1.0.json",
  "version": "2.1.0",
  "runs": [{
    "tool": {
      "driver": {
        "name": "x-secret-scan",
        "version": "1.0.0",
        "informationUri": "https://github.com/gitleaks/gitleaks",
        "rules": [
          {
            "id": "SECRET-001",
            "name": "AwsCredential",
            "shortDescription": { "text": "AWS credential detected" },
            "defaultConfiguration": { "level": "error" },
            "properties": { "category": "aws" }
          }
        ]
      }
    },
    "results": [
      {
        "ruleId": "SECRET-001",
        "level": "error",
        "message": { "text": "AWS Access Key ID detected" },
        "locations": [{
          "physicalLocation": {
            "artifactLocation": { "uri": "config/application.yml" },
            "region": { "startLine": 15 }
          }
        }],
        "fingerprints": {
          "secret-scan/v1": "e3b0c44298fc..."
        },
        "properties": {
          "severity": "CRITICAL",
          "category": "aws",
          "redactedMatch": "AKIA****EXAMPLE",
          "commit": null
        }
      }
    ]
  }]
}
```

### Markdown Report

```markdown
# Secret Scan Report

**Date:** {timestamp}
**Scope:** {current|history|both}
**Tool:** {gitleaks|trufflehog} v{version}
**Score:** {score}/100 (Grade: {grade})

## Summary

| Severity | Count |
|----------|-------|
| CRITICAL | {n} |
| HIGH | {n} |
| MEDIUM | {n} |
| LOW | {n} |
| INFO | {n} |
| **Total** | **{total}** |

## Findings

### [{ruleId}] {message} ({severity})
- **File:** {file}:{line}
- **Category:** {category}
- **Match:** `{redactedMatch}`
- **Commit:** {commit or "current"}
- **Fingerprint:** `{fingerprint}`

## Baseline

- **Entries loaded:** {count}
- **Findings excluded:** {count}

## Recommendations

1. Rotate all CRITICAL secrets immediately
2. Add false positives to `.security-baseline.json`
3. Enable pre-commit hooks to prevent future leaks
```

## Error Handling

### Tool Not Found

When gitleaks AND trufflehog (or detect-secrets) are unavailable:

1. Generate a SARIF report with exactly 1 finding
2. Set finding level to `none` (INFO)
3. Include install instructions for both tools in the message
4. Set score to 100 (no real vulnerabilities detected)

### Scan Timeout

When the scan exceeds `--timeout`:

1. Generate a partial SARIF report with findings collected so far
2. Add a warning finding with level `warning`
3. Include timeout value and elapsed time in the message
4. Score based on partial findings only

### Tool Crash

When the scanning tool exits with non-zero code:

1. Capture stderr output
2. Generate a SARIF report with 1 finding at level `error`
3. Include exit code and stderr excerpt in the message
4. Score: 0 (unable to verify security posture)

### Zero Findings

When scan completes with no secrets found:

1. Generate a valid SARIF report with empty `results[]`
2. Set score to 100, grade to A
3. Report as success

## CI Integration

### GitHub Actions

```yaml
- name: Secret Scan
  id: secret-scan
  run: |
    mkdir -p results/security
    if command -v gitleaks &> /dev/null; then
      gitleaks detect --source . --report-format sarif \
        --report-path results/security/secret-scan-$(date +%Y%m%d-%H%M%S).sarif.json \
        --no-banner || true
    elif command -v trufflehog &> /dev/null; then
      trufflehog filesystem --directory . --format sarif \
        > results/security/secret-scan-$(date +%Y%m%d-%H%M%S).sarif.json || true
    else
      echo "::warning::No secret scanning tool available"
    fi

- name: Upload SARIF
  if: always()
  uses: github/codeql-action/upload-sarif@v3
  with:
    sarif_file: results/security/
    category: secret-scan

- name: Upload scan artifacts
  if: always()
  uses: actions/upload-artifact@v4
  with:
    name: security-secret-scan-results
    path: results/security/secret-scan-*.sarif.json
    retention-days: 90
```

### GitLab CI

```yaml
secret-scan:
  stage: test
  script:
    - mkdir -p results/security
    - |
      if command -v gitleaks &> /dev/null; then
        gitleaks detect --source . --report-format sarif \
          --report-path results/security/secret-scan-$(date +%Y%m%d-%H%M%S).sarif.json \
          --no-banner || true
      elif command -v trufflehog &> /dev/null; then
        trufflehog filesystem --directory . --format sarif \
          > results/security/secret-scan-$(date +%Y%m%d-%H%M%S).sarif.json || true
      else
        echo "WARNING: No secret scanning tool available"
      fi
  artifacts:
    paths:
      - results/security/secret-scan-*.sarif.json
    reports:
      sast: results/security/secret-scan-*.sarif.json
    expire_in: 90 days
  allow_failure: false
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
    - if: '$CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH'
```

### Azure DevOps

```yaml
- task: CmdLine@2
  displayName: 'Secret Scan'
  inputs:
    script: |
      mkdir -p results/security
      if command -v gitleaks &> /dev/null; then
        gitleaks detect --source . --report-format sarif \
          --report-path results/security/secret-scan-$(date +%Y%m%d-%H%M%S).sarif.json \
          --no-banner || true
      elif command -v trufflehog &> /dev/null; then
        trufflehog filesystem --directory . --format sarif \
          > results/security/secret-scan-$(date +%Y%m%d-%H%M%S).sarif.json || true
      else
        echo "##vso[task.logissue type=warning]No secret scanning tool available"
      fi

- task: PublishBuildArtifacts@1
  displayName: 'Publish secret scan results'
  condition: always()
  inputs:
    PathtoPublish: 'results/security'
    ArtifactName: 'security-secret-scan-results'
```

## Idempotency

### Output Directory Convention

All results written to `results/security/` relative to the project root.

### File Naming Convention

```
results/security/secret-scan-{YYYYMMDD}-{HHMMSS}.sarif.json
results/security/secret-scan-{YYYYMMDD}-{HHMMSS}.md
```

### Idempotency Rules

- Each scan run produces a new dated file (never overwrite previous results)
- The `results/security/` directory is created if it does not exist
- Previous scan results are NOT deleted or modified
- The `.gitignore` SHOULD include `results/security/`

## Baseline System

### Baseline File Format

File: `.security-baseline.json`

```json
{
  "version": "1.0",
  "entries": [
    {
      "fingerprint": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
      "reason": "Test fixture, not real credential",
      "approvedBy": "security-team",
      "approvedDate": "2026-01-15"
    }
  ]
}
```

### Baseline Rules

- The baseline file MUST be version-controlled
- Each entry MUST have all 4 fields: fingerprint, reason, approvedBy, approvedDate
- Findings with fingerprint matching a baseline entry are excluded from the report
- The report MUST show how many findings were excluded by the baseline
- Fingerprints are computed as: `SHA-256(ruleId + ":" + file + ":" + line + ":" + redactedMatch)`
