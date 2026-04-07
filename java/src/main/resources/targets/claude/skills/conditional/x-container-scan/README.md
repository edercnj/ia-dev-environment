# x-container-scan

> Scans Docker images for CVEs and Dockerfile best practices violations. Uses Trivy, Grype, or Snyk Container for image vulnerability scanning and Dockerfile linting.

| | |
|---|---|
| **Category** | Conditional |
| **Condition** | `security.scanning.containerScan = true` |
| **Invocation** | `/x-container-scan [--image name:tag] [--dockerfile path] [--severity-threshold CRITICAL\|HIGH\|MEDIUM\|LOW]` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## When Available

This skill is generated when container scanning is enabled in the project security configuration and Docker is part of the technology stack.

## What It Does

Scans Docker container images for known CVEs and analyzes Dockerfiles for security best practices violations. Supports combined scans (image vulnerabilities plus Dockerfile lint) and produces a unified SARIF 2.1.0 report with severity scoring and grade assignment. Automatically selects Trivy, Grype, or Snyk Container based on tool availability, and supports filtering by severity threshold and excluding CVEs without available fixes.

## Usage

```
/x-container-scan --image myapp:1.0
/x-container-scan --dockerfile ./Dockerfile
/x-container-scan --image myapp:1.0 --dockerfile ./Dockerfile
/x-container-scan --image myapp:1.0 --ignore-unfixed --severity-threshold HIGH
```

## See Also

- [x-infra-scan](../x-infra-scan/) -- Infrastructure-as-Code security scanning
- [x-sast-scan](../x-sast-scan/) -- Static code analysis for vulnerabilities
- [x-secret-scan](../x-secret-scan/) -- Secret detection in code and git history
