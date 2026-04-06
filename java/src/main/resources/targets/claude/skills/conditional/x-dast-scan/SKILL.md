---
name: x-dast-scan
description: "Dynamic Application Security Testing (DAST) — Tests the running application for XSS, injection, misconfiguration, and information disclosure vulnerabilities using OWASP ZAP or Nuclei."
allowed-tools: Read, Bash
argument-hint: "--target <URL> [--env local|dev|homolog|prod] [--mode passive|active|full] [--confirm-prod] [--openapi <path>] [--auth-token <token>]"
user-invocable: true
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings.
- **Preservation**: All technical constraints below must be followed strictly.

# Skill: DAST Scanner (x-dast-scan)

## Description

Orchestrates Dynamic Application Security Testing against a running application. DAST complements SAST by testing from outside-in, simulating real attacks to detect runtime vulnerabilities that static analysis cannot find: missing security headers, insecure cookies, CORS misconfiguration, injection flaws, and information disclosure.

**Condition**: This skill is included when `security.scanning.dast = true`.

## Tool Selection

| Priority | Tool | Use Case |
|----------|------|----------|
| Preferred | OWASP ZAP | Full DAST scanner: passive interception, active scanning, fuzzing, OpenAPI import |
| Fallback | Nuclei | Template-based scanner: fast, targeted checks for known CVEs and misconfigurations |
| Lightweight | nikto | Quick web server misconfiguration checks when ZAP/Nuclei unavailable |

### Tool Detection

```bash
# Check preferred tool first
if command -v zap-cli &>/dev/null || command -v zap.sh &>/dev/null; then
  DAST_TOOL="zap"
elif command -v nuclei &>/dev/null; then
  DAST_TOOL="nuclei"
elif command -v nikto &>/dev/null; then
  DAST_TOOL="nikto"
else
  echo "ERROR: No DAST tool found. Install OWASP ZAP, Nuclei, or nikto."
  exit 1
fi
```

## CLI Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `--target` | URL | Yes | (none) | Target application URL (http/https) |
| `--env` | Enum | No | `local` | Environment: `local`, `dev`, `homolog`, `prod` |
| `--mode` | Enum | No | `passive` | Scan mode: `passive`, `active`, `full` |
| `--confirm-prod` | Flag | No | `false` | Required confirmation for production scans |
| `--openapi` | Path | No | (none) | OpenAPI spec path to expand scan coverage |
| `--auth-token` | String | No | (none) | Bearer token for authenticated endpoint scanning |

## Environment Restrictions (RULE-004)

| Environment | Passive | Active | Full | Requirements |
|-------------|---------|--------|------|-------------|
| local | Yes | Yes | Yes | None |
| dev | Yes | Yes | Yes | None |
| homolog | Yes | Yes (non-destructive) | No | Excludes destructive tests; full auto-downgrades to active |
| prod | Yes | No | No | Requires `--confirm-prod`; active/full auto-downgrade to passive |

### Environment Guard Logic

1. If `--env=prod` and `--confirm-prod` NOT provided: **BLOCK scan with error**
   - Message: `ERROR: --confirm-prod is required for production scans`
   - No requests sent to the target application
2. If `--env=prod` and `--mode` is `active` or `full`: **AUTO-DOWNGRADE to passive**
   - Emit: `WARNING: Mode auto-downgraded from [requested] to passive for production environment`
   - Only passive analysis is performed (no attack payloads sent)
3. If `--env=homolog` and `--mode=full`: **AUTO-DOWNGRADE to active (non-destructive)**
   - Emit: `WARNING: Mode auto-downgraded from full to active (non-destructive) for homolog environment`
   - Destructive fuzzing tests excluded

## Scan Modes

### Passive Mode
- Intercepts HTTP responses and analyzes them without sending attack payloads
- Checks: missing security headers, insecure cookies, information disclosure, verbose errors
- Safe for all environments including production
- No data modification risk

### Active Mode
- Sends test payloads to detect injection vulnerabilities
- Checks: SQL injection, XSS (reflected/stored), path traversal, command injection, LDAP injection
- May create test data or trigger error conditions
- NOT safe for production

### Full Mode
- Combines passive + active + advanced fuzzing
- Includes: parameter fuzzing, authentication bypass attempts, session management tests
- Maximum coverage, highest risk of side effects
- Only for local and dev environments

## Finding Categories

| Category | Description | OWASP Mapping |
|----------|-------------|---------------|
| injection | SQL, NoSQL, LDAP, OS command injection | A03:2021 |
| authentication | Weak auth, session fixation, credential exposure | A07:2021 |
| misconfiguration | Missing headers, CORS, TLS issues, verbose errors | A05:2021 |
| info-disclosure | Stack traces, internal paths, version info, debug endpoints | A01:2021 |

## Execution Flow

1. **Validate parameters**: Verify `--target` is a valid URL (http/https scheme)
2. **Apply environment guard**: Enforce RULE-004 restrictions (block/downgrade as needed)
3. **Detect tool**: Check for ZAP, then Nuclei, then nikto
4. **Configure scan**: Set mode, import OpenAPI spec if provided, configure auth token
5. **Execute scan**: Run the selected tool against the target
6. **Parse findings**: Extract raw findings from tool output
7. **Categorize**: Map each finding to category (injection, authentication, misconfiguration, info-disclosure)
8. **Map to OWASP**: Assign OWASP Top 10 category (A01-A10) to each finding
9. **Generate SARIF**: Produce SARIF 2.1.0 compliant output
10. **Score and grade**: Calculate security score and letter grade
11. **Generate report**: Produce Markdown summary report

## SARIF Output

Findings are output in SARIF 2.1.0 format with the following structure per result:

```json
{
  "ruleId": "DAST-001",
  "level": "error",
  "message": { "text": "SQL Injection detected in username parameter" },
  "locations": [{
    "physicalLocation": {
      "artifactLocation": { "uri": "https://app.local/api/users" }
    }
  }],
  "properties": {
    "category": "injection",
    "severity": "HIGH",
    "owaspCategory": "A03",
    "method": "POST",
    "parameter": "username",
    "evidence": "Response contains SQL error message",
    "fixRecommendation": "Use parameterized queries"
  }
}
```

## Scoring

| Grade | Score Range | Criteria |
|-------|-------------|----------|
| A | 90-100 | No CRITICAL or HIGH findings |
| B | 75-89 | No CRITICAL, up to 2 HIGH |
| C | 60-74 | No CRITICAL, 3+ HIGH or 5+ MEDIUM |
| D | 40-59 | 1+ CRITICAL or 5+ HIGH |
| F | 0-39 | Multiple CRITICAL findings |

Score calculation: Start at 100, deduct per finding severity:
- CRITICAL: -25 points
- HIGH: -15 points
- MEDIUM: -5 points
- LOW: -2 points
- INFO: 0 points

## Output Format

```markdown
## DAST Scan Report

### Configuration
- **Target**: [URL]
- **Environment**: [env]
- **Mode**: [effective mode after guard]
- **Tool**: [ZAP|Nuclei|nikto]
- **OpenAPI**: [path or N/A]

### Environment Guard
- [Applied restrictions and any auto-downgrade warnings]

### Summary
- **Total Findings**: [count]
- **Score**: [score]/100 (Grade: [A-F])
- CRITICAL: [count] | HIGH: [count] | MEDIUM: [count] | LOW: [count] | INFO: [count]

### Findings by Category

#### Injection ([count])
| # | Rule | Severity | URL | Method | Parameter | Evidence |
|---|------|----------|-----|--------|-----------|----------|
| 1 | DAST-001 | HIGH | /api/users | POST | username | SQL error in response |

#### Authentication ([count])
...

#### Misconfiguration ([count])
...

#### Information Disclosure ([count])
...

### OWASP Top 10 Coverage
| Category | Findings | Status |
|----------|----------|--------|
| A01 - Broken Access Control | [count] | [PASS/FAIL] |
| A03 - Injection | [count] | [PASS/FAIL] |
| A05 - Security Misconfiguration | [count] | [PASS/FAIL] |
| A07 - Identification and Authentication Failures | [count] | [PASS/FAIL] |

### Recommendations
[Prioritized list of fix recommendations]

### SARIF Output
- File: `dast-report.sarif`
```

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
  env:
    APP_URL: http://localhost:8080

# Upload SARIF to GitHub Security
- name: Upload SARIF
  uses: github/codeql-action/upload-sarif@v3
  with:
    sarif_file: dast-report.sarif
```
