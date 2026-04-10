# x-security-secret-scan

> Scans code and git history for leaked credentials, API keys, tokens, and secrets. Produces SARIF output with scoring and CI integration.

| | |
|---|---|
| **Category** | Conditional |
| **Condition** | `security.scanning.secretScan = true` |
| **Invocation** | `/x-security-secret-scan [--scope current\|history\|both] [--baseline path] [--since-commit SHA]` |
| **Reads** | security (references: sarif-template, security-scoring, security-skill-template) |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## When Available

This skill is generated when secret scanning is enabled in the project security configuration.

## What It Does

Detects leaked secrets (API keys, tokens, passwords, certificates, connection strings) in the current codebase and git history using gitleaks or trufflehog. Credential leakage is among the most common causes of security breaches, and even secrets removed in later commits remain accessible in git history. Produces SARIF 2.1.0 output with severity scoring and supports a baseline system for excluding known false positives.

## Usage

```
/x-security-secret-scan
/x-security-secret-scan --scope history
/x-security-secret-scan --scope both --since-commit abc123
/x-security-secret-scan --baseline .gitleaks-baseline.json
```

## See Also

- [x-security-sast](../x-security-sast/) -- Static code analysis for security vulnerabilities
- [x-security-container](../x-security-container/) -- Container image vulnerability scanning
- [x-security-sonar](../x-security-sonar/) -- SonarQube quality gate enforcement
