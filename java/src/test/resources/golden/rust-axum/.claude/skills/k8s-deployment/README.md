# k8s-deployment

> Kubernetes deployment patterns reference covering workload types, pod specifications, resource sizing, probes, autoscaling, network policies, and security contexts.

| | |
|---|---|
| **Category** | Infrastructure Pattern |
| **Supplements** | infrastructure |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete pattern reference.

## Key Patterns

- Workload type decision tree (Deployment, StatefulSet, DaemonSet, Job, CronJob)
- Pod specification template with production-ready fields
- Resource sizing guide for CPU and memory requests/limits
- Probe configuration (liveness, readiness, startup)
- Autoscaling with HPA and network policies
- Security context hardening (non-root, read-only filesystem, capabilities)

## See Also

- [k8s-helm](../k8s-helm/) — Helm chart structure, values templates, GitOps integration
- [k8s-kustomize](../k8s-kustomize/) — Kustomize overlays, patches, components
- [infrastructure](../../infrastructure/) — Cloud-agnostic infrastructure patterns and 12-Factor principles
