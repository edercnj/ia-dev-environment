# x-infra-scan

> Infrastructure Security Scanner -- scans Kubernetes manifests, Terraform modules, Helm charts, and Docker Compose files for misconfigurations against CIS benchmarks.

| | |
|---|---|
| **Category** | Conditional |
| **Condition** | `security.scanning.infraScan = true` |
| **Invocation** | `/x-infra-scan [--scope k8s\|terraform\|helm\|compose\|all] [--benchmark cis-1.8\|cis-1.7\|custom]` |
| **Reads** | security (references: security-principles, security-skill-template, sarif-template, security-scoring) |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## When Available

This skill is generated when `security.scanning.infraScan = true` in the project configuration.

## What It Does

Scans Infrastructure as Code (IaC) files for security misconfigurations against CIS benchmarks and cloud security best practices. Detects missing security contexts, open security groups, permissive RBAC, plaintext secrets, absent resource limits, and Pod Security Standards violations. Uses checkov as the primary tool with kube-bench and tfsec as alternatives, producing SARIF 2.1.0 output with CIS benchmark references.

## Usage

```
/x-infra-scan
/x-infra-scan --scope k8s
/x-infra-scan --scope terraform --benchmark cis-1.8
/x-infra-scan --scope all
```

## See Also

- [x-container-scan](../x-container-scan/) -- Container image CVE scanning and Dockerfile linting
- [x-sast-scan](../x-sast-scan/) -- Static application security testing
- [setup-environment](../setup-environment/) -- Dev environment setup with orchestrator
