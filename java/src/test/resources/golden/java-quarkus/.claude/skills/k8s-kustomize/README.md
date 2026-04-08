# k8s-kustomize

> Kustomize patterns for environment management covering directory structure, patches, components, secret management, generators, and patch types.

| | |
|---|---|
| **Category** | Infrastructure Pattern |
| **Supplements** | infrastructure |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete pattern reference.

## Key Patterns

- Directory structure with base, overlays, and components layout
- Common patches for replicas, resources, and environment variables
- Reusable components for cross-cutting concerns
- Secret management with sealed secrets and external providers
- ConfigMap and secret generators
- Patch types (strategic merge, JSON 6902, inline)

## See Also

- [k8s-helm](../k8s-helm/) — Helm chart structure, values templates, GitOps integration
- [k8s-deployment](../k8s-deployment/) — Kubernetes workload types, pod specs, security contexts
- [infrastructure](../../infrastructure/) — Cloud-agnostic infrastructure patterns and 12-Factor principles
