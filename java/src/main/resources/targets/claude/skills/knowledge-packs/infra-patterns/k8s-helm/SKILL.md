---
name: k8s-helm
description: "Helm chart patterns: chart structure, values templates, multi-environment configuration, dependencies, testing, GitOps integration, and Helmfile."
user-invocable: false
allowed-tools:
  - Read
  - Grep
  - Glob
---

# Pattern: Helm Chart Patterns

## Purpose

Provides production-grade Helm chart patterns for packaging, deploying, and managing Kubernetes applications across multiple environments with GitOps workflows.

## Supplements

Supplements `infrastructure` knowledge pack with Helm chart-specific patterns.

## Stack Compatibility

- **Helm:** ≥ 3.12
- **Kubernetes:** ≥ 1.26
- **GitOps:** ArgoCD ≥ 2.8 or Flux v2 (HelmRelease CRD)
- **Orchestration:** Helmfile ≥ 0.156
- **Values strictness:** `required` / `fail` template funcs for fail-fast validation

## Patterns Index

| Pattern | Use case | File |
| :--- | :--- | :--- |
| Chart Structure | Canonical chart layout, `Chart.yaml`, `_helpers.tpl`, deployment template | [`references/examples-chart-structure.md`](references/examples-chart-structure.md) |
| Values Template | Complete `values.yaml` with image, resources, probes, ingress, autoscaling | [`references/examples-values-template.md`](references/examples-values-template.md) |
| Multi-Environment | `values-dev.yaml` / `values-staging.yaml` / `values-prod.yaml` overlays | [`references/examples-multi-environment.md`](references/examples-multi-environment.md) |
| Dependencies | Subchart management in `Chart.yaml`, alias and import-values configuration | [`references/examples-dependencies.md`](references/examples-dependencies.md) |
| Testing | `helm test` hooks, post-deployment connection + API tests | [`references/examples-testing.md`](references/examples-testing.md) |
| GitOps Integration | ArgoCD Application / ApplicationSet, Flux HelmRelease | [`references/examples-gitops.md`](references/examples-gitops.md) |
| Helmfile | Multi-chart orchestration, `helmfile.yaml`, usage commands | [`references/examples-helmfile.md`](references/examples-helmfile.md) |

## When to Open an Example File

Open a specific `references/examples-<pattern>.md` only when implementing that pattern. The Patterns Index is enough to decide *which* file applies; each example file carries the complete chart fragments.

## References

All pattern examples live under `references/examples-*.md` next to this SKILL.md. The naming convention is `examples-<slug>.md` where `<slug>` matches the third column of the Patterns Index above.
