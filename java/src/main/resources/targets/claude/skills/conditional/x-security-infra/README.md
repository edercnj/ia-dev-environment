# x-security-infra

> Infrastructure Security Scanner -- scans Kubernetes manifests, Terraform modules, Helm charts, and Docker Compose files for misconfigurations against CIS benchmarks.

| | |
|---|---|
| **Category** | Conditional |
| **Condition** | `security.scanning.infraScan = true` |
| **Invocation** | `/x-security-infra [--scope k8s\|terraform\|helm\|compose\|all] [--benchmark cis-1.8\|cis-1.7\|custom]` |
| **Reads** | security (references: security-principles, security-skill-template, sarif-template, security-scoring) |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## When Available

This skill is generated when `security.scanning.infraScan = true` in the project configuration.

## What It Does

Scans Infrastructure as Code (IaC) files for security misconfigurations against CIS benchmarks and cloud security best practices. Detects missing security contexts, open security groups, permissive RBAC, plaintext secrets, absent resource limits, and Pod Security Standards violations. Uses checkov as the primary tool with kube-bench and tfsec as alternatives, producing SARIF 2.1.0 output with CIS benchmark references.

## Usage

```
/x-security-infra
/x-security-infra --scope k8s
/x-security-infra --scope terraform --benchmark cis-1.8
/x-security-infra --scope all
```

## See Also

- [x-security-container](../x-security-container/) -- Container image CVE scanning and Dockerfile linting
- [x-security-sast](../x-security-sast/) -- Static application security testing
- [setup-environment](../setup-environment/) -- Dev environment setup with orchestrator
