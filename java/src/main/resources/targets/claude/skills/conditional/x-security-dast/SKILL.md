---
name: x-security-dast
description: "Dynamic Application Security Testing -- tests the running application for XSS, injection, misconfiguration, and information disclosure using OWASP ZAP or Nuclei."
user-invocable: true
allowed-tools: Read, Bash
argument-hint: "--target <URL> [--env local|dev|homolog|prod] [--mode passive|active|full] [--confirm-prod] [--openapi <path>] [--auth-token <token>]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: DAST Scanner

## Purpose

Orchestrate Dynamic Application Security Testing against a running application. DAST complements SAST by testing from outside-in, simulating real attacks to detect runtime vulnerabilities that static analysis cannot find: missing security headers, insecure cookies, CORS misconfiguration, injection flaws, and information disclosure.

## Activation Condition

Include this skill when `security.scanning.dast = true` in the project configuration.

## Triggers

- `/x-dast-scan --target http://localhost:8080` -- scan local app with default passive mode
- `/x-dast-scan --target http://localhost:8080 --mode active` -- active scan in local
- `/x-dast-scan --target https://app.staging.com --env homolog` -- scan homolog (auto-downgrades)
- `/x-dast-scan --target https://app.example.com --env prod --confirm-prod` -- production passive scan

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `--target` | URL | Yes | (none) | Target application URL (http/https) |
| `--env` | Enum | No | `local` | Environment: `local`, `dev`, `homolog`, `prod` |
| `--mode` | Enum | No | `passive` | Scan mode: `passive`, `active`, `full` |
| `--confirm-prod` | Flag | No | `false` | Required confirmation for production scans |
| `--openapi` | Path | No | (none) | OpenAPI spec path to expand scan coverage |
| `--auth-token` | String | No | (none) | Bearer token for authenticated endpoint scanning |

## Workflow

### Step 1 — Validate Parameters

Verify `--target` is a valid URL (http/https scheme).

### Step 2 — Apply Environment Guard (RULE-004)

| Environment | Passive | Active | Full | Requirements |
|-------------|---------|--------|------|-------------|
| local | Yes | Yes | Yes | None |
| dev | Yes | Yes | Yes | None |
| homolog | Yes | Yes (non-destructive) | No | Full auto-downgrades to active |
| prod | Yes | No | No | Requires `--confirm-prod`; active/full auto-downgrade to passive |

Guard logic:
1. If `--env=prod` and `--confirm-prod` NOT provided: **BLOCK scan with error**
2. If `--env=prod` and `--mode` is `active` or `full`: **AUTO-DOWNGRADE to passive**
3. If `--env=homolog` and `--mode=full`: **AUTO-DOWNGRADE to active (non-destructive)**

### Step 3 — Detect Tool

Check for scanning tools in order of preference:

| Priority | Tool | Use Case |
|----------|------|----------|
| Preferred | OWASP ZAP | Full DAST scanner: passive interception, active scanning, fuzzing, OpenAPI import |
| Fallback | Nuclei | Template-based scanner: fast, targeted checks for known CVEs and misconfigurations |
| Lightweight | nikto | Quick web server misconfiguration checks |

### Step 4 — Configure and Execute Scan

Set mode, import OpenAPI spec if provided, configure auth token, and execute.

### Step 5 — Parse and Categorize Findings

Extract raw findings from tool output. Map each finding to category:

| Category | Description | OWASP Mapping |
|----------|-------------|---------------|
| injection | SQL, NoSQL, LDAP, OS command injection | A03:2021 |
| authentication | Weak auth, session fixation, credential exposure | A07:2021 |
| misconfiguration | Missing headers, CORS, TLS issues, verbose errors | A05:2021 |
| info-disclosure | Stack traces, internal paths, version info, debug endpoints | A01:2021 |

### Step 6 — Generate SARIF Output

Produce SARIF 2.1.0 compliant output with all findings.

### Step 7 — Score and Grade

Start at 100, deduct per finding severity:

| Severity | Deduction |
|----------|-----------|
| CRITICAL | -25 |
| HIGH | -15 |
| MEDIUM | -5 |
| LOW | -2 |
| INFO | 0 |

| Grade | Score Range | Criteria |
|-------|-------------|----------|
| A | 90-100 | No CRITICAL or HIGH findings |
| B | 75-89 | No CRITICAL, up to 2 HIGH |
| C | 60-74 | No CRITICAL, 3+ HIGH or 5+ MEDIUM |
| D | 40-59 | 1+ CRITICAL or 5+ HIGH |
| F | 0-39 | Multiple CRITICAL findings |

### Step 8 — Generate Report

Produce Markdown summary report with configuration, environment guard, findings by category, OWASP coverage, and recommendations.

## Scan Modes

### Passive Mode
- Intercepts HTTP responses and analyzes them without sending attack payloads
- Checks: missing security headers, insecure cookies, information disclosure, verbose errors
- Safe for all environments including production

### Active Mode
- Sends test payloads to detect injection vulnerabilities
- Checks: SQL injection, XSS (reflected/stored), path traversal, command injection
- NOT safe for production

### Full Mode
- Combines passive + active + advanced fuzzing
- Includes: parameter fuzzing, authentication bypass attempts, session management tests
- Only for local and dev environments

## Error Handling

| Scenario | Action |
|----------|--------|
| No DAST tool found | Report error with installation instructions for ZAP, Nuclei, or nikto |
| `--env=prod` without `--confirm-prod` | Block scan immediately, no requests sent |
| Target URL unreachable | Report connection failure with target URL |
| Scan timeout | Report partial findings collected so far |

## CI Integration

```yaml
# GitHub Actions example
- name: DAST Scan
  run: |
    /x-dast-scan \
      --target ${{ env.APP_URL }} \
      --env ${{ env.ENVIRONMENT }} \
      --mode passive \
      --openapi docs/openapi.yaml
- name: Upload SARIF
  uses: github/codeql-action/upload-sarif@v3
  with:
    sarif_file: dast-report.sarif
```
