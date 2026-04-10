# x-security-sonar

> SonarQube Quality Gate -- integrates with SonarQube/SonarCloud for security hotspot tracking, quality gate enforcement, and SARIF output from findings.

| | |
|---|---|
| **Category** | Conditional |
| **Condition** | `security.qualityGate.provider != "none"` |
| **Invocation** | `/x-security-sonar --server <url> --token <token> [--quality-gate default\|strict] [--project-key <key>]` |
| **Reads** | security (references: security-principles, application-security) |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## When Available

This skill is generated when `security.qualityGate.provider` is configured to a value other than "none" in the project configuration (e.g., `sonarqube` or `sonarcloud`).

## What It Does

Integrates with SonarQube or SonarCloud to generate `sonar-project.properties`, execute SonarScanner, poll quality gate status, and produce SARIF output with a Markdown report. Supports default and strict quality gate modes for security hotspot tracking and vulnerability enforcement. Provides actionable recommendations for failed quality gate conditions.

## Usage

```
/x-security-sonar --server https://sonar.example.com --token squ_xxxx
/x-security-sonar --server https://sonar.example.com --token squ_xxxx --quality-gate strict
/x-security-sonar --server https://sonarcloud.io --token squ_xxxx --project-key my-project
```

## See Also

- [x-security-sast](../x-security-sast/) -- Static application security testing
- [x-security-secret-scan](../x-security-secret-scan/) -- Secret detection in code and git history
- [x-security-pentest](../x-security-pentest/) -- Multi-phase penetration test orchestrator
