---
name: x-sast-scan
description: "Static Application Security Testing — scans source code for security vulnerabilities without executing the application"
allowed-tools: Read, Write, Edit, Bash, Grep, Glob
argument-hint: "[--scope all|owasp|custom-rules] [--severity-threshold CRITICAL|HIGH|MEDIUM|LOW|INFO]"
---

## Global Output Policy

- **Language**: English ONLY. (Ignore input language, always respond in English).
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.
- **Preservation**: All existing technical constraints below must be followed strictly.

# Skill: SAST Scanner (x-sast-scan)

## Purpose

Performs Static Application Security Testing (SAST) by analyzing source code to identify security vulnerabilities, coding errors, and compliance violations without executing the application. Detects patterns such as SQL injection, XSS, path traversal, insecure deserialization, and other OWASP Top 10 vulnerabilities.

The skill automatically selects the appropriate scanning tool based on the project's build tool and language, with Semgrep as the universal fallback for all ecosystems.

**Condition**: This skill is generated when `security.scanning.sast = true` in the project configuration.

## Knowledge Pack References

Read these references before starting a scan:
- `skills/security/references/security-skill-template.md` -- canonical structure for all security scanning skills
- `skills/security/references/sarif-template.md` -- SARIF 2.1.0 output schema and required fields
- `skills/security/references/security-scoring.md` -- scoring model and grade thresholds
- `skills/security/references/security-principles.md` -- data classification, input validation, fail-secure patterns

## Tool Selection

| Build Tool | Language | Preferred Tool | Fallback Tool | Install Command |
|-----------|----------|---------------|--------------|----------------|
| maven | java | SpotBugs + FindSecBugs | Semgrep | `mvn com.github.spotbugs:spotbugs-maven-plugin:check` |
| gradle | java/kotlin | SpotBugs + FindSecBugs | Semgrep | `gradle spotbugsMain` |
| npm | javascript/typescript | ESLint security plugin | Semgrep | `npx eslint --plugin security .` |
| pip | python | Bandit | Semgrep | `pip install bandit && bandit -r .` |
| go | go | gosec | Semgrep | `go install github.com/securego/gosec/v2/cmd/gosec@latest` |
| cargo | rust | cargo-audit | Semgrep | `cargo install cargo-audit && cargo audit` |

### Tool Selection Rules

1. Detect the project build tool from the project root (pom.xml, build.gradle, package.json, setup.py/pyproject.toml, go.mod, Cargo.toml)
2. Attempt the **Preferred Tool** first
3. If the preferred tool is not installed, attempt the **Fallback Tool** (Semgrep)
4. If neither tool is available, generate an INFO-level finding with installation instructions

### Semgrep Fallback

Semgrep is the universal fallback scanner supporting 30+ languages. Install with:
```bash
pip install semgrep
# or
brew install semgrep
```

Run with:
```bash
semgrep scan --config auto --sarif --output results/security/sast-$(date +%Y%m%d-%H%M%S).sarif.json .
```

## Parameters

| Parameter | Type | Default | Required | Description |
|----------|------|---------|----------|-------------|
| `--scope` | string | `all` | No | Scan scope: `all` (full analysis), `owasp` (OWASP Top 10 focused), `custom-rules` (project-specific rules) |
| `--fix` | string | `report-only` | No | Fix mode: `auto` (apply fixes), `suggest` (show suggestions), `report-only` (findings only) |
| `--severity-threshold` | string | `LOW` | No | Minimum severity to include: `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`, `INFO` |
| `--exclude` | string | `""` | No | Comma-separated glob patterns to exclude from scan (e.g., `test/**,docs/**`) |

### Parameter Validation

- `--scope` must be one of: `all`, `owasp`, `custom-rules`
- `--fix` must be one of: `auto`, `suggest`, `report-only`
- `--severity-threshold` must be one of: `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`, `INFO`
- `--exclude` patterns must be valid glob expressions

## OWASP Top 10 Mapping

Every finding MUST be mapped to an OWASP Top 10 (2021) category when applicable:

| OWASP ID | Category | Common SAST Findings |
|----------|----------|---------------------|
| A01 | Broken Access Control | Missing authorization checks, IDOR, privilege escalation |
| A02 | Cryptographic Failures | Weak algorithms, hardcoded keys, insecure random |
| A03 | Injection | SQL injection, XSS, command injection, LDAP injection |
| A04 | Insecure Design | Missing input validation, business logic flaws |
| A05 | Security Misconfiguration | Debug enabled, default credentials, verbose errors |
| A06 | Vulnerable Components | Outdated dependencies (delegate to x-dependency-audit) |
| A07 | Auth Failures | Weak passwords, missing MFA, session fixation |
| A08 | Data Integrity Failures | Insecure deserialization, unsigned updates |
| A09 | Logging Failures | Missing audit logs, sensitive data in logs |
| A10 | SSRF | Server-side request forgery, unvalidated redirects |
| UNCLASSIFIED | Other | Findings without a direct OWASP mapping |

## SARIF Rule ID Convention

SAST findings use the prefix `SAST-NNN`:

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

## Output Format

### SARIF 2.1.0

All scan results MUST be produced in SARIF 2.1.0 format per `references/sarif-template.md`.

**Required fields per finding:**
- `ruleId`: Pattern `SAST-NNN`
- `level`: `error` (CRITICAL/HIGH), `warning` (MEDIUM), `note` (LOW), `none` (INFO)
- `message.text`: Human-readable finding description
- `locations[].physicalLocation`: File path and line number
- `properties.severity`: `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`, `INFO`
- `properties.owasp-category`: `A01`-`A10` or `UNCLASSIFIED`
- `properties.cwe-id`: `CWE-NNN` (when available)
- `properties.fix-recommendation`: Remediation guidance

**SARIF tool.driver.name**: Must reflect the actual tool used (e.g., `SpotBugs`, `Semgrep`, `Bandit`, `ESLint`, `gosec`, `cargo-audit`).

### Scoring Integration

Score computed per `references/security-scoring.md`:

```
score = max(0, 100 - sum(severity_penalties))
```

| Severity | Penalty per Finding |
|----------|-------------------|
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

### Markdown Report

The skill MUST also produce a Markdown report with:

```markdown
## SAST Scan Report

**Tool**: {tool_name} {tool_version}
**Scope**: {scope}
**Target**: {target_path}
**Date**: {YYYY-MM-DD HH:MM:SS}

### Summary

| Metric | Value |
|--------|-------|
| Total Findings | {count} |
| Critical | {critical_count} |
| High | {high_count} |
| Medium | {medium_count} |
| Low | {low_count} |
| Score | {score}/100 |
| Grade | {grade} |

### Findings by OWASP Category

| OWASP | Category | Count |
|-------|----------|-------|
| A01 | Broken Access Control | {count} |
| A03 | Injection | {count} |
| ... | ... | ... |

### Detailed Findings

#### [{severity}] {ruleId}: {message}
- **File**: {file}:{line}
- **CWE**: {cwe_id}
- **OWASP**: {owasp_category}
- **Fix**: {fix_recommendation}
```

## Error Handling

### Tool Not Found

When neither the preferred tool nor Semgrep is available:

1. Generate a SARIF report with 1 finding (level: `none`, ruleId: `SAST-015`)
2. Message includes installation instructions for both tools
3. Score: 100 (no real vulnerabilities detected)

### Scan Timeout

When the scan exceeds 300 seconds (default):

1. Generate partial SARIF with findings collected so far
2. Add a warning finding with timeout details
3. Score based on partial findings only

### Tool Crash

When the scanning tool exits non-zero or produces unparseable output:

1. Capture stderr output (first 500 characters)
2. Generate SARIF with 1 finding (level: `error`)
3. Score: 0 (unable to verify security posture)

### Zero Findings

When the scan completes with no findings:

1. Generate valid SARIF with empty `results[]`
2. Score: 100, Grade: A
3. Report as success

## CI Integration

### GitHub Actions

```yaml
- name: SAST Scan
  id: sast-scan
  run: |
    mkdir -p results/security
    TIMESTAMP=$(date +%Y%m%d-%H%M%S)
    if command -v spotbugs &> /dev/null || [ -f pom.xml ]; then
      mvn com.github.spotbugs:spotbugs-maven-plugin:check \
        -Dspotbugs.sarifOutput=results/security/sast-${TIMESTAMP}.sarif.json \
        || true
    elif command -v semgrep &> /dev/null; then
      semgrep scan --config auto --sarif \
        --output results/security/sast-${TIMESTAMP}.sarif.json .
    else
      echo "::warning::No SAST tool available. Install SpotBugs or Semgrep."
    fi

- name: Upload SARIF
  if: always()
  uses: github/codeql-action/upload-sarif@v3
  with:
    sarif_file: results/security/
    category: sast

- name: Upload scan artifacts
  if: always()
  uses: actions/upload-artifact@v4
  with:
    name: security-sast-results
    path: results/security/sast-*.sarif.json
    retention-days: 90
```

### GitLab CI

```yaml
sast-security-scan:
  stage: test
  script:
    - mkdir -p results/security
    - |
      TIMESTAMP=$(date +%Y%m%d-%H%M%S)
      if command -v spotbugs &> /dev/null || [ -f pom.xml ]; then
        mvn com.github.spotbugs:spotbugs-maven-plugin:check \
          -Dspotbugs.sarifOutput=results/security/sast-${TIMESTAMP}.sarif.json \
          || true
      elif command -v semgrep &> /dev/null; then
        semgrep scan --config auto --sarif \
          --output results/security/sast-${TIMESTAMP}.sarif.json .
      else
        echo "WARNING: No SAST tool available"
      fi
  artifacts:
    paths:
      - results/security/sast-*.sarif.json
    reports:
      sast: results/security/sast-*.sarif.json
    expire_in: 90 days
  allow_failure: false
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
    - if: '$CI_COMMIT_BRANCH == $CI_DEFAULT_BRANCH'
```

### Azure DevOps

```yaml
- task: CmdLine@2
  displayName: 'SAST Scan'
  inputs:
    script: |
      mkdir -p results/security
      TIMESTAMP=$(date +%Y%m%d-%H%M%S)
      if command -v spotbugs &> /dev/null || [ -f pom.xml ]; then
        mvn com.github.spotbugs:spotbugs-maven-plugin:check \
          -Dspotbugs.sarifOutput=results/security/sast-${TIMESTAMP}.sarif.json \
          || true
      elif command -v semgrep &> /dev/null; then
        semgrep scan --config auto --sarif \
          --output results/security/sast-${TIMESTAMP}.sarif.json \
          $(Build.SourcesDirectory)
      else
        echo "##vso[task.logissue type=warning]No SAST tool available"
      fi

- task: PublishBuildArtifacts@1
  displayName: 'Publish SAST results'
  condition: always()
  inputs:
    PathtoPublish: 'results/security'
    ArtifactName: 'security-sast-results'
```

## Idempotency

### Output Directory

All results are written to `results/security/` relative to the project root.

### File Naming

```
results/security/sast-{YYYYMMDD}-{HHMMSS}.sarif.json
```

### Rules

- Each scan run produces a new dated file (never overwrite)
- Create `results/security/` if it does not exist
- Previous scan results are never deleted or modified
- `.gitignore` SHOULD include `results/security/`

## Workflow

1. **Detect** build tool from project root files
2. **Select** preferred scanner from Tool Selection table
3. **Verify** tool installation; fallback to Semgrep if needed
4. **Execute** scan with `--scope` and `--exclude` parameters
5. **Parse** raw findings from tool output
6. **Map** each finding to OWASP Top 10 category and CWE
7. **Filter** findings below `--severity-threshold`
8. **Generate** SARIF 2.1.0 output with all required fields
9. **Compute** score and grade per scoring model
10. **Produce** Markdown summary report
11. **Save** both outputs to `results/security/`
