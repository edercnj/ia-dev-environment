# x-sonar-gate

> SonarQube Quality Gate -- integrates with SonarQube/SonarCloud for security hotspot tracking, quality gate enforcement, and SARIF output from findings.

| | |
|---|---|
| **Category** | Conditional |
| **Condition** | `security.qualityGate.provider != "none"` |
| **Invocation** | `/x-sonar-gate --server <url> --token <token> [--quality-gate default\|strict] [--project-key <key>]` |
| **Reads** | security (references: security-principles, application-security) |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## When Available

This skill is generated when `security.qualityGate.provider` is configured to a value other than "none" in the project configuration (e.g., `sonarqube` or `sonarcloud`).

## What It Does

Integrates with SonarQube or SonarCloud to generate `sonar-project.properties`, execute SonarScanner, poll quality gate status, and produce SARIF output with a Markdown report. Supports default and strict quality gate modes for security hotspot tracking and vulnerability enforcement. Provides actionable recommendations for failed quality gate conditions.

## Usage

```
/x-sonar-gate --server https://sonar.example.com --token squ_xxxx
/x-sonar-gate --server https://sonar.example.com --token squ_xxxx --quality-gate strict
/x-sonar-gate --server https://sonarcloud.io --token squ_xxxx --project-key my-project
```

## See Also

- [x-sast-scan](../x-sast-scan/) -- Static application security testing
- [x-secret-scan](../x-secret-scan/) -- Secret detection in code and git history
- [x-pentest](../x-pentest/) -- Multi-phase penetration test orchestrator
