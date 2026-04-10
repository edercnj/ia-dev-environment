# x-security-container

> Scans Docker images for CVEs and Dockerfile best practices violations. Uses Trivy, Grype, or Snyk Container for image vulnerability scanning and Dockerfile linting.

| | |
|---|---|
| **Category** | Conditional |
| **Condition** | `security.scanning.containerScan = true` |
| **Invocation** | `/x-security-container [--image name:tag] [--dockerfile path] [--ignore-unfixed] [--severity-threshold CRITICAL\|HIGH\|MEDIUM\|LOW]` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## When Available

This skill is generated when container scanning is enabled in the project security configuration and Docker is part of the technology stack.

## What It Does

Scans Docker container images for known CVEs and analyzes Dockerfiles for security best practices violations. Supports combined scans (image vulnerabilities plus Dockerfile lint) and produces a unified SARIF 2.1.0 report with severity scoring and grade assignment. Automatically selects Trivy, Grype, or Snyk Container based on tool availability, and supports filtering by severity threshold and excluding CVEs without available fixes.

## Usage

```
/x-security-container --image myapp:1.0
/x-security-container --dockerfile ./Dockerfile
/x-security-container --image myapp:1.0 --dockerfile ./Dockerfile
/x-security-container --image myapp:1.0 --ignore-unfixed --severity-threshold HIGH
```

## See Also

- [x-security-infra](../x-security-infra/) -- Infrastructure-as-Code security scanning
- [x-security-sast](../x-security-sast/) -- Static code analysis for vulnerabilities
- [x-security-secret-scan](../x-security-secret-scan/) -- Secret detection in code and git history
