# container-registry

> Container registry management patterns covering tagging strategy, immutability, retention policies, vulnerability scanning, multi-arch builds, and CI/CD integration.

| | |
|---|---|
| **Category** | Infrastructure Pattern |
| **Supplements** | infrastructure |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete pattern reference.

## Key Patterns

- Tagging strategy with semver, branch, and PR tag types
- Image immutability and tag protection rules
- Retention policies for automated cleanup
- Vulnerability scanning integration and enforcement
- Multi-arch builds with buildx and manifest lists
- CI/CD integration for automated image publishing

## See Also

- [dockerfile](../dockerfile/) — Multi-stage builds, security hardening, layer optimization
- [k8s-deployment](../k8s-deployment/) — Kubernetes workload types, pod specs, security contexts
- [infrastructure](../../infrastructure/) — Cloud-agnostic infrastructure patterns and 12-Factor principles
