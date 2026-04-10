---
name: x-security-sast
description: "Static Application Security Testing -- scans source code for security vulnerabilities without executing the application. Produces SARIF output with OWASP mapping."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob
argument-hint: "[--scope all|owasp|custom-rules] [--severity-threshold CRITICAL|HIGH|MEDIUM|LOW|INFO]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: SAST Scanner

## Purpose

Perform Static Application Security Testing (SAST) by analyzing source code to identify security vulnerabilities, coding errors, and compliance violations without executing the application. Detect patterns such as SQL injection, XSS, path traversal, insecure deserialization, and other OWASP Top 10 vulnerabilities.

Automatically select the appropriate scanning tool based on the project's build tool and language, with Semgrep as the universal fallback.

## Activation Condition

Include this skill when `security.scanning.sast = true` in the project configuration.

## Triggers

- `/x-security-sast` -- full scan with all rules
- `/x-security-sast --scope owasp` -- OWASP Top 10 focused scan
- `/x-security-sast --scope custom-rules` -- project-specific rules only
- `/x-security-sast --severity-threshold HIGH` -- filter findings by severity

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `--scope` | String | No | `all` | Scan scope: `all`, `owasp`, `custom-rules` |
| `--fix` | String | No | `report-only` | Fix mode: `auto`, `suggest`, `report-only` |
| `--severity-threshold` | String | No | `LOW` | Minimum severity: `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`, `INFO` |
| `--exclude` | String | No | `""` | Comma-separated glob patterns to exclude |

## Knowledge Pack References

| Pack | Files | Purpose |
|------|-------|---------|
| security | `skills/security/references/security-skill-template.md` | Canonical structure for security scanning skills |
| security | `skills/security/references/sarif-template.md` | SARIF 2.1.0 output schema and required fields |
| security | `skills/security/references/security-scoring.md` | Scoring model and grade thresholds |
| security | `skills/security/references/security-principles.md` | Data classification, input validation, fail-secure patterns |

## Workflow

### Step 1 — Detect Build Tool

Detect the project build tool from the project root (pom.xml, build.gradle, package.json, setup.py/pyproject.toml, go.mod, Cargo.toml).

### Step 2 — Select Scanner

| Build Tool | Language | Preferred Tool | Fallback Tool | Install Command |
|-----------|----------|---------------|--------------|----------------|
| maven | java | SpotBugs + FindSecBugs | Semgrep | `mvn com.github.spotbugs:spotbugs-maven-plugin:check` |
| gradle | java/kotlin | SpotBugs + FindSecBugs | Semgrep | `gradle spotbugsMain` |
| npm | javascript/typescript | ESLint security plugin | Semgrep | `npx eslint --plugin security .` |
| pip | python | Bandit | Semgrep | `pip install bandit && bandit -r .` |
| go | go | gosec | Semgrep | `go install github.com/securego/gosec/v2/cmd/gosec@latest` |
| cargo | rust | cargo-audit | Semgrep | `cargo install cargo-audit && cargo audit` |

### Step 3 — Execute Scan

Run the selected tool with `--scope` and `--exclude` parameters.

### Step 4 — Map Findings

Map each finding to OWASP Top 10 category and CWE:

| OWASP ID | Category | Common SAST Findings |
|----------|----------|---------------------|
| A01 | Broken Access Control | Missing authorization checks, IDOR |
| A02 | Cryptographic Failures | Weak algorithms, hardcoded keys |
| A03 | Injection | SQL injection, XSS, command injection |
| A04 | Insecure Design | Missing input validation |
| A05 | Security Misconfiguration | Debug enabled, verbose errors |
| A06 | Vulnerable Components | Outdated dependencies (delegate to x-dependency-audit) |
| A07 | Auth Failures | Weak passwords, missing MFA |
| A08 | Data Integrity Failures | Insecure deserialization |
| A09 | Logging Failures | Missing audit logs, sensitive data in logs |
| A10 | SSRF | Server-side request forgery |

### Step 5 — Filter and Score

Filter findings below `--severity-threshold`. Compute score:

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

- SARIF 2.1.0 output to `results/security/sast-{YYYYMMDD}-{HHMMSS}.sarif.json`
- Markdown summary report with findings by OWASP category

## SARIF Rule ID Convention

| Rule ID | Severity | OWASP | CWE | Description |
|---------|----------|-------|-----|-------------|
| SAST-001 | CRITICAL | A03 | CWE-89 | SQL Injection |
| SAST-002 | CRITICAL | A03 | CWE-79 | Cross-Site Scripting (XSS) |
| SAST-003 | HIGH | A03 | CWE-78 | OS Command Injection |
| SAST-004 | HIGH | A01 | CWE-862 | Missing Authorization |
| SAST-005 | HIGH | A02 | CWE-327 | Weak Cryptographic Algorithm |
| SAST-006 | HIGH | A02 | CWE-798 | Hardcoded Credentials |
| SAST-007 | HIGH | A08 | CWE-502 | Insecure Deserialization |
| SAST-008 | MEDIUM | A03 | CWE-90 | LDAP Injection |
| SAST-009 | MEDIUM | A05 | CWE-209 | Information Exposure in Error Messages |
| SAST-010 | MEDIUM | A03 | CWE-22 | Path Traversal |
| SAST-011 | MEDIUM | A10 | CWE-918 | Server-Side Request Forgery |
| SAST-012 | MEDIUM | A02 | CWE-330 | Insecure Random Number Generation |
| SAST-013 | LOW | A09 | CWE-532 | Sensitive Data in Log Files |
| SAST-014 | LOW | A05 | CWE-489 | Debug Code in Production |
| SAST-015 | INFO | --- | --- | Tool Not Found (installation instructions) |

## Error Handling

| Scenario | Action |
|----------|--------|
| Tool not found (preferred and Semgrep) | Generate SARIF with INFO finding (SAST-015), score 100 |
| Scan timeout (> 300s) | Generate partial SARIF with findings so far, add warning |
| Tool crash (non-zero exit) | Capture stderr, generate error SARIF finding, score 0 |
| Zero findings | Generate valid SARIF with empty results, score 100, grade A |

## Idempotency

- Each scan run produces a new dated file (never overwrite)
- Create `results/security/` directory if it does not exist
- Previous scan results are NOT deleted or modified
- File naming: `results/security/sast-{YYYYMMDD}-{HHMMSS}.sarif.json`

## CI Integration

### GitHub Actions

```yaml
- name: SAST Scan
  run: |
    mkdir -p results/security
    TIMESTAMP=$(date +%Y%m%d-%H%M%S)
    if [ -f pom.xml ]; then
      mvn com.github.spotbugs:spotbugs-maven-plugin:check || true
    elif command -v semgrep &> /dev/null; then
      semgrep scan --config auto --sarif --output results/security/sast-${TIMESTAMP}.sarif.json .
    fi
- name: Upload SARIF
  if: always()
  uses: github/codeql-action/upload-sarif@v3
  with:
    sarif_file: results/security/
    category: sast
```
