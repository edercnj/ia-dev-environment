# k8s-helm

> Helm chart patterns for application deployment covering chart structure, values templates, multi-environment configuration, dependencies, testing, GitOps integration, and Helmfile orchestration.

| | |
|---|---|
| **Category** | Infrastructure Pattern |
| **Supplements** | infrastructure |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete pattern reference.

## Key Patterns

- Chart structure with Chart.yaml, _helpers.tpl, and template organization
- Values template design and multi-environment configuration
- Chart dependencies and conditional sub-charts
- Chart testing with helm test and ct lint
- GitOps integration (Flux, ArgoCD) and Helmfile orchestration

## See Also

- [k8s-kustomize](../k8s-kustomize/) — Kustomize overlays, patches, components
- [k8s-deployment](../k8s-deployment/) — Kubernetes workload types, pod specs, autoscaling
- [infrastructure](../../infrastructure/) — Cloud-agnostic infrastructure patterns and 12-Factor principles
