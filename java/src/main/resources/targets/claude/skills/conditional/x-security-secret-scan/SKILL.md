---
name: x-security-secret-scan
description: "Scans code and git history for leaked credentials, API keys, tokens, and secrets. Produces SARIF output with scoring and baseline support."
user-invocable: true
allowed-tools: Bash, Read, Write, Glob, Grep
argument-hint: "[--scope current|history|both] [--baseline path] [--since-commit SHA] [--format sarif|markdown|both]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Secret Scanner

## Purpose

Detect leaked secrets (API keys, tokens, passwords, certificates, connection strings) in the current codebase and git history. Credential leakage is among the most common causes of security breaches. Even secrets removed in later commits remain accessible in git history, making historical scanning essential.

Produce SARIF 2.1.0 output, compute a security score and grade, and support a baseline system for excluding known false positives.

## Activation Condition

Include this skill when secret scanning is required for the project security posture.

## Triggers

- `/x-security-secret-scan` -- scan current working tree
- `/x-security-secret-scan --scope history` -- scan git history
- `/x-security-secret-scan --scope both` -- scan current tree and git history
- `/x-security-secret-scan --scope both --baseline .security-baseline.json` -- scan with false positive exclusions

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `--scope` | String | No | `current` | Scan scope: `current`, `history`, `both` |
| `--baseline` | String | No | `.security-baseline.json` | Path to baseline file with accepted false positives |
| `--since-commit` | String | No | (none) | SHA of the commit from which to scan history |
| `--format` | String | No | `both` | Output format: `sarif`, `markdown`, `both` |
| `--output-dir` | String | No | `results/security` | Output directory for reports |
| `--severity` | String | No | `CRITICAL,HIGH,MEDIUM` | Minimum severity to report |
| `--timeout` | Integer | No | `300` | Scan timeout in seconds |
| `--fail-on` | String | No | `CRITICAL` | Severity threshold that causes non-zero exit |

## Knowledge Pack References

| Pack | Files | Purpose |
|------|-------|---------|
| security | `skills/security/references/sarif-template.md` | SARIF 2.1.0 schema and required fields |
| security | `skills/security/references/security-scoring.md` | Scoring model and grade thresholds |
| security | `skills/security/references/security-skill-template.md` | Canonical structure for security skills |

## Workflow

### Step 1 — Detect Tool

Check tool availability:
1. `gitleaks` (preferred): `command -v gitleaks`
2. `trufflehog` (fallback): `command -v trufflehog`
3. For Python projects, also check `detect-secrets`
4. If no tool available, generate INFO-level SARIF finding with install instructions

### Step 2 — Execute Scan

Run the detected tool against the specified scope:

**gitleaks (current):**
```bash
gitleaks detect --source . --report-format sarif --report-path results/security/secret-scan-$(date +%Y%m%d-%H%M%S).sarif.json --no-banner
```

**gitleaks (history):**
```bash
gitleaks detect --source . --report-format sarif --report-path results/security/secret-scan-$(date +%Y%m%d-%H%M%S).sarif.json --log-opts="--all" --no-banner
```

### Step 3 — Load Baseline and Filter

If `--baseline` file exists, load fingerprints and exclude matching findings. Compute fingerprint: `SHA-256(ruleId + ":" + file + ":" + line + ":" + redactedMatch)`.

### Step 4 — Redact Secrets

Every finding MUST have `redactedMatch` that masks the secret:
- Show first 4 characters, replace middle with `****`, show last 4 characters
- For secrets shorter than 12 characters, show first 2 and last 2

### Step 5 — Score and Grade

| Severity | Penalty |
|----------|---------|
| CRITICAL | -25 |
| HIGH | -15 |
| MEDIUM | -5 |
| LOW | -2 |
| INFO | 0 |

| Grade | Score Range | CI Gate |
|-------|-----------|---------|
| A | 90-100 | Pass |
| B | 75-89 | Pass (with warnings) |
| C | 50-74 | Fail (configurable) |
| D | 25-49 | Fail |
| F | 0-24 | Fail |

### Step 6 — Generate Reports

- SARIF to `results/security/secret-scan-{YYYYMMDD}-{HHMMSS}.sarif.json`
- Markdown to `results/security/secret-scan-{YYYYMMDD}-{HHMMSS}.md`

## Secret Categories

| Category | Rule ID | Severity | Examples |
|----------|---------|----------|----------|
| AWS Credentials | SECRET-001 | CRITICAL | Access Key ID, Secret Access Key, Session Token |
| GCP Credentials | SECRET-002 | CRITICAL | Service Account Key, GCP API Key, OAuth Client Secret |
| Azure Credentials | SECRET-003 | CRITICAL | Storage Account Key, SAS Token, Client Secret |
| API Tokens | SECRET-004 | HIGH | GitHub, GitLab, Slack, Stripe, Twilio, SendGrid tokens |
| Private Keys | SECRET-005 | CRITICAL | RSA, ECDSA, Ed25519, PGP private keys |
| Hardcoded Passwords | SECRET-006 | HIGH | Passwords in config, properties, env files |
| JWT Tokens | SECRET-007 | MEDIUM | Hardcoded JWT tokens |
| Database Connection Strings | SECRET-008 | HIGH | PostgreSQL, MySQL, MongoDB, Redis connection URIs |

## Tool Selection

| Build Tool | Language | Preferred Tool | Fallback Tool |
|-----------|----------|---------------|--------------|
| maven | java | gitleaks | trufflehog |
| gradle | java/kotlin | gitleaks | trufflehog |
| npm | typescript | gitleaks | trufflehog |
| pip | python | gitleaks | detect-secrets |
| go | go | gitleaks | trufflehog |
| cargo | rust | gitleaks | trufflehog |

## Baseline System

File: `.security-baseline.json`

```json
{
  "version": "1.0",
  "entries": [
    {
      "fingerprint": "e3b0c44298fc...",
      "reason": "Test fixture, not real credential",
      "approvedBy": "security-team",
      "approvedDate": "2026-01-15"
    }
  ]
}
```

Rules:
- Baseline file MUST be version-controlled
- Each entry MUST have all 4 fields: fingerprint, reason, approvedBy, approvedDate
- Report MUST show how many findings were excluded by baseline

## Error Handling

| Scenario | Action |
|----------|--------|
| Tool not found (gitleaks and fallback) | Generate INFO SARIF finding with install instructions, score 100 |
| Scan timeout | Generate partial SARIF with findings so far, add warning finding |
| Tool crash (non-zero exit) | Capture stderr, generate error SARIF finding, score 0 |
| Zero findings | Generate valid SARIF with empty results, score 100, grade A |

## Idempotency

- Each scan run produces a new dated file (never overwrite)
- Create `results/security/` directory if it does not exist
- Previous scan results are NOT deleted or modified

## CI Integration

### GitHub Actions

```yaml
- name: Secret Scan
  run: |
    mkdir -p results/security
    if command -v gitleaks &> /dev/null; then
      gitleaks detect --source . --report-format sarif --report-path results/security/secret-scan-$(date +%Y%m%d-%H%M%S).sarif.json --no-banner || true
    fi
- name: Upload SARIF
  if: always()
  uses: github/codeql-action/upload-sarif@v3
  with:
    sarif_file: results/security/
    category: secret-scan
```
