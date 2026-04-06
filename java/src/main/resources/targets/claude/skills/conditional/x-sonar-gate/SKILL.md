---
name: x-sonar-gate
description: "Skill: SonarQube Quality Gate — Integrates with SonarQube/SonarCloud for security hotspot tracking, quality gate enforcement, and SARIF output from findings."
allowed-tools: Read, Write, Edit, Bash, Grep, Glob
argument-hint: "--server <url> --token <token> [--quality-gate default|strict] [--project-key <key>] [--branch <branch>] [--timeout <seconds>]"
---

## Global Output Policy

- **Language**: English ONLY. (Ignore input language, always respond in English).
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.
- **Preservation**: All existing technical constraints below must be followed strictly.

# Skill: SonarQube Quality Gate

## Description

Integrates with SonarQube or SonarCloud to generate `sonar-project.properties`, execute SonarScanner, poll quality gate status, and produce SARIF output with a Markdown report. Supports default and strict quality gate modes for security hotspot tracking and vulnerability enforcement.

**Condition**: This skill applies when `security.qualityGate.provider` is not "none".

## Prerequisites

- SonarScanner CLI installed and available on PATH
- SonarQube/SonarCloud server accessible from CI environment
- Authentication token with `Execute Analysis` permission
- {{FRAMEWORK}} project with {{BUILD_TOOL}} build system

## Knowledge Pack References

Before executing, read the security principles:
- `skills/security/references/security-principles.md` — data classification, input validation, fail-secure patterns
- `skills/security/references/application-security.md` — OWASP Top 10, security headers, secrets management

## Parameters

| Parameter | Type | Required | Default | Validation | Description |
|-----------|------|----------|---------|------------|-------------|
| `--server` | String | Yes | -- | Valid HTTP/HTTPS URL | SonarQube/SonarCloud server URL |
| `--token` | String | Yes | -- | Non-empty, min 40 chars | Authentication token |
| `--quality-gate` | String | No | `default` | enum: default, strict | Quality gate mode |
| `--project-key` | String | No | auto-detect | Pattern: `[a-zA-Z0-9_:.-]+` | Project key in SonarQube |
| `--branch` | String | No | current branch | Non-empty | Branch for analysis |
| `--timeout` | int | No | 300 | 30-3600 | Polling timeout in seconds |

## Execution Flow

### 1. Validate Parameters

- Verify `--server` is a reachable HTTP/HTTPS URL
- Verify `--token` is non-empty and at least 40 characters
- If server is unreachable, abort with: `SonarQube server unreachable: <url>`
- If token is invalid (401 from server), abort with: `SonarQube authentication failed`

### 2. Auto-Detect Project Configuration

- Detect language from project structure:
  - {{LANGUAGE}} project: scan for `{{BUILD_TOOL}}` build files
- Detect source directories:
  - Java/Kotlin: `src/main/java`, `src/main/kotlin`
  - Python: `src/`, `app/`
  - TypeScript: `src/`
  - Go: root directory
  - Rust: `src/`
- Detect test directories:
  - Java/Kotlin: `src/test/java`, `src/test/kotlin`
  - Python: `tests/`
  - TypeScript: `test/`, `tests/`
  - Go: `*_test.go` files
  - Rust: `tests/`
- Detect project key from build files if `--project-key` not provided

### 3. Generate sonar-project.properties

Generate the properties file with auto-detected values:

```properties
sonar.projectKey={{project_name}}
sonar.projectName={{project_name}}
sonar.sources=<detected-source-dirs>
sonar.tests=<detected-test-dirs>
sonar.sourceEncoding=UTF-8
sonar.exclusions=**/test/**,**/generated/**,**/node_modules/**
sonar.security.hotspots.reviewed=true
sonar.qualitygate.wait=true
```

Language-specific properties:
- **Java**: `sonar.java.binaries=target/classes`
- **Kotlin**: `sonar.kotlin.binaries=build/classes`
- **TypeScript**: `sonar.typescript.lcov.reportPaths=coverage/lcov.info`
- **Python**: `sonar.python.coverage.reportPaths=coverage.xml`
- **Go**: `sonar.go.coverage.reportPaths=coverage.out`
- **Rust**: `sonar.rust.coverage.reportPaths=lcov.info`

For multi-module Maven projects, include:
```properties
sonar.modules=module-a,module-b,module-c
module-a.sonar.sources=src/main/java
module-a.sonar.tests=src/test/java
module-a.sonar.java.binaries=target/classes
```

### 4. Execute SonarScanner

```bash
sonar-scanner \
  -Dsonar.host.url=<server-url> \
  -Dsonar.token=<token> \
  -Dsonar.branch.name=<branch> \
  -Dsonar.projectKey=<project-key>
```

Capture the task ID from scanner output for polling.

### 5. Poll Quality Gate Status

- Endpoint: `GET /api/qualitygates/project_status?analysisId=<id>`
- Polling interval: 10 seconds
- Timeout: configurable via `--timeout` (default: 300s)
- Terminal states: `OK`, `ERROR`
- On timeout: report `Quality gate polling timeout after <timeout>s` with last known status and analysis ID

### 6. Evaluate Quality Gate Result

#### Default Mode

Uses the quality gate configured on the SonarQube server. Reports all conditions with their actual values.

#### Strict Mode

Enforces stricter thresholds beyond the server default:

| Metric | Threshold | Operator |
|--------|-----------|----------|
| `new_vulnerabilities` | 0 | GT (greater than) |
| `security_hotspots_reviewed` | 100% | LT (less than) |
| `security_rating` | A (1) | GT |
| `duplicated_lines_density` | 3% | GT |
| `coverage` | 95% | LT |

### 7. Generate Reports

#### SARIF Output

Convert SonarQube findings to SARIF 2.1.0 format:
- Map SonarQube rules to SARIF rule descriptors
- Map issues to SARIF results with locations
- Include security hotspots as SARIF results with `security-severity` tag
- Output to `sonar-results.sarif`

#### Markdown Report

```markdown
## SonarQube Quality Gate Report

### Status: PASS / FAIL / ERROR

| Metric | Status | Actual | Threshold |
|--------|--------|--------|-----------|
| New Vulnerabilities | OK | 0 | 0 |
| Security Hotspots Reviewed | OK | 100% | 100% |
| Security Rating | OK | A | A |
| Coverage | OK | 97.2% | 95% |
| Duplicated Lines | OK | 1.2% | 3% |

### Quality Gate Mode: default / strict
### Project Key: <project-key>
### Analysis ID: <analysis-id>
### Branch: <branch>

### Conditions Detail
- [condition details with pass/fail status]

### Recommendations
- [actionable recommendations for failed conditions]
- [link to SonarQube dashboard for review]
```

## CI Integration Snippets

### GitHub Actions

```yaml
- name: SonarQube Quality Gate
  run: |
    sonar-scanner \
      -Dsonar.host.url=${{ '{{' }} secrets.SONAR_HOST_URL {{ '}}' }} \
      -Dsonar.token=${{ '{{' }} secrets.SONAR_TOKEN {{ '}}' }} \
      -Dsonar.branch.name=${{ '{{' }} github.ref_name {{ '}}' }} \
      -Dsonar.qualitygate.wait=true
  env:
    SONAR_TOKEN: ${{ '{{' }} secrets.SONAR_TOKEN {{ '}}' }}
```

### GitLab CI

```yaml
sonarqube-check:
  stage: quality
  image: sonarsource/sonar-scanner-cli:latest
  variables:
    SONAR_USER_HOME: "${CI_PROJECT_DIR}/.sonar"
  script:
    - sonar-scanner
      -Dsonar.host.url=${SONAR_HOST_URL}
      -Dsonar.token=${SONAR_TOKEN}
      -Dsonar.branch.name=${CI_COMMIT_REF_NAME}
      -Dsonar.qualitygate.wait=true
  allow_failure: false
```

### Azure DevOps

```yaml
- task: SonarQubeAnalyze@6
  displayName: 'Run SonarQube Analysis'
- task: SonarQubePublish@6
  displayName: 'Publish Quality Gate Result'
  inputs:
    pollingTimeoutSec: '300'
```

## Error Handling

| Error | Message | Recovery |
|-------|---------|----------|
| Server unreachable | `SonarQube server unreachable: <url>` | Verify URL and network connectivity |
| Authentication failed | `SonarQube authentication failed: 401` | Verify token and permissions |
| Polling timeout | `Quality gate polling timeout after <timeout>s` | Increase timeout or check server load |
| Scanner not found | `SonarScanner not found on PATH` | Install sonar-scanner CLI |
| Invalid project key | `Invalid project key format: <key>` | Use pattern `[a-zA-Z0-9_:.-]+` |

## Quality Gate Result Schema

```json
{
  "status": "PASS|FAIL|ERROR",
  "qualityGateMode": "default|strict",
  "conditions": [
    {
      "metric": "new_vulnerabilities",
      "operator": "GT",
      "value": "0",
      "status": "OK",
      "actualValue": "0"
    }
  ],
  "projectKey": "my-project",
  "analysisId": "AYx..."
}
```

## Usage Examples

```bash
# Default quality gate
/x-sonar-gate --server https://sonar.example.com --token squ_abc123...

# Strict mode for release pipeline
/x-sonar-gate --server https://sonar.example.com --token squ_abc123... --quality-gate strict

# Custom project key and branch
/x-sonar-gate --server https://sonar.example.com --token squ_abc123... --project-key my-service --branch release/1.0

# Extended timeout for large projects
/x-sonar-gate --server https://sonar.example.com --token squ_abc123... --timeout 600
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
- [ ] CI integration snippets cover GitHub Actions, GitLab CI, and Azure DevOps
- [ ] Token and credentials are NEVER logged or exposed in output
