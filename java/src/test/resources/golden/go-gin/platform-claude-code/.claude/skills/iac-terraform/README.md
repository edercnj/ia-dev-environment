# iac-terraform

> Terraform patterns reference covering module structure, remote state, naming conventions, CI/CD workflows, drift detection, and common infrastructure modules.

| | |
|---|---|
| **Category** | Infrastructure Pattern |
| **Supplements** | infrastructure |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete pattern reference.

## Key Patterns

- Module structure with root, modules, and environments layout
- Remote state management with locking and backend configuration
- Naming conventions for resources, variables, and outputs
- CI/CD workflows for plan, apply, and approval gates
- Drift detection and automated reconciliation
- Common reusable modules (VPC, database, cache, k8s-cluster)

## See Also

- [iac-crossplane](../iac-crossplane/) — Crossplane XRDs, Compositions, Kubernetes-native IaC
- [k8s-deployment](../k8s-deployment/) — Kubernetes workload types, pod specs, autoscaling
- [infrastructure](../../infrastructure/) — Cloud-agnostic infrastructure patterns and 12-Factor principles
