---
name: x-security-sonar
description: "Integrates with SonarQube/SonarCloud for security hotspot tracking, quality gate enforcement, and SARIF output from findings."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob
argument-hint: "--server <url> --token <token> [--quality-gate default|strict] [--project-key <key>] [--branch <branch>] [--timeout <seconds>]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: SonarQube Quality Gate

## Purpose

Integrate with SonarQube or SonarCloud to generate `sonar-project.properties`, execute SonarScanner, poll quality gate status, and produce SARIF output with a Markdown report. Support default and strict quality gate modes for security hotspot tracking and vulnerability enforcement.

## Activation Condition

Include this skill when `security.qualityGate.provider` is not "none" in the project configuration.

## Triggers

- `/x-sonar-gate --server https://sonar.example.com --token squ_abc123...` -- default quality gate
- `/x-sonar-gate --server https://sonar.example.com --token squ_abc123... --quality-gate strict` -- strict mode for release pipeline
- `/x-sonar-gate --server https://sonar.example.com --token squ_abc123... --project-key my-service --branch release/1.0` -- custom project key and branch

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `--server` | String | Yes | -- | SonarQube/SonarCloud server URL |
| `--token` | String | Yes | -- | Authentication token (min 40 chars) |
| `--quality-gate` | String | No | `default` | Quality gate mode: `default`, `strict` |
| `--project-key` | String | No | auto-detect | Project key in SonarQube |
| `--branch` | String | No | current branch | Branch for analysis |
| `--timeout` | Integer | No | 300 | Polling timeout in seconds (30-3600) |

## Knowledge Pack References

| Pack | Files | Purpose |
|------|-------|---------|
| security | `skills/security/references/security-principles.md` | Data classification, input validation, fail-secure patterns |
| security | `skills/security/references/application-security.md` | OWASP Top 10, security headers, secrets management |

## Prerequisites

- SonarScanner CLI installed and available on PATH
- SonarQube/SonarCloud server accessible from CI environment
- Authentication token with `Execute Analysis` permission
- {{FRAMEWORK}} project with {{BUILD_TOOL}} build system

## Workflow

### Step 1 — Validate Parameters

- Verify `--server` is a reachable HTTP/HTTPS URL
- Verify `--token` is non-empty and at least 40 characters
- If server unreachable, abort with: `SonarQube server unreachable: <url>`
- If token invalid (401), abort with: `SonarQube authentication failed`

### Step 2 — Auto-Detect Project Configuration

Detect language, source directories, test directories, and project key from build files.

### Step 3 — Generate sonar-project.properties

Generate the properties file with auto-detected values including language-specific properties.

### Step 4 — Execute SonarScanner

```bash
sonar-scanner \
  -Dsonar.host.url=<server-url> \
  -Dsonar.token=<token> \
  -Dsonar.branch.name=<branch> \
  -Dsonar.projectKey=<project-key>
```

Capture the task ID from scanner output for polling.

### Step 5 — Poll Quality Gate Status

- Endpoint: `GET /api/qualitygates/project_status?analysisId=<id>`
- Polling interval: 10 seconds
- Timeout: configurable via `--timeout` (default: 300s)
- Terminal states: `OK`, `ERROR`

### Step 6 — Evaluate Quality Gate Result

**Default Mode:** Use the quality gate configured on the SonarQube server.

**Strict Mode:** Enforce stricter thresholds:

| Metric | Threshold | Operator |
|--------|-----------|----------|
| `new_vulnerabilities` | 0 | GT |
| `security_hotspots_reviewed` | 100% | LT |
| `security_rating` | A (1) | GT |
| `duplicated_lines_density` | 3% | GT |
| `coverage` | 95% | LT |

### Step 7 — Generate Reports

- Convert SonarQube findings to SARIF 2.1.0 format
- Generate Markdown report with quality gate status, metrics, and recommendations

## Error Handling

| Scenario | Action |
|----------|--------|
| Server unreachable | Abort with `SonarQube server unreachable: <url>`, verify URL and network |
| Authentication failed | Abort with `SonarQube authentication failed: 401`, verify token |
| Polling timeout | Report last known status with analysis ID, suggest increasing timeout |
| Scanner not found | Abort with `SonarScanner not found on PATH`, provide install instructions |
| Invalid project key | Abort with `Invalid project key format`, suggest pattern `[a-zA-Z0-9_:.-]+` |

## CI Integration

### GitHub Actions

```yaml
- name: SonarQube Quality Gate
  run: |
    sonar-scanner \
      -Dsonar.host.url=${{ '{{' }} secrets.SONAR_HOST_URL {{ '}}' }} \
      -Dsonar.token=${{ '{{' }} secrets.SONAR_TOKEN {{ '}}' }} \
      -Dsonar.branch.name=${{ '{{' }} github.ref_name {{ '}}' }} \
      -Dsonar.qualitygate.wait=true
```

### GitLab CI

```yaml
sonarqube-check:
  stage: quality
  image: sonarsource/sonar-scanner-cli:latest
  script:
    - sonar-scanner
      -Dsonar.host.url=${SONAR_HOST_URL}
      -Dsonar.token=${SONAR_TOKEN}
      -Dsonar.qualitygate.wait=true
```

## Review Checklist

- [ ] `sonar-project.properties` generated with correct source/test directories
- [ ] SonarScanner executed with proper authentication
- [ ] Quality gate status polled until terminal state or timeout
- [ ] All conditions evaluated against configured thresholds
- [ ] SARIF output includes security hotspots and vulnerabilities
- [ ] Markdown report includes all metrics with pass/fail status
- [ ] Error messages include actionable recovery guidance
- [ ] Strict mode enforces zero-vulnerability and 100% hotspot review
- [ ] Token and credentials are NEVER logged or exposed in output
