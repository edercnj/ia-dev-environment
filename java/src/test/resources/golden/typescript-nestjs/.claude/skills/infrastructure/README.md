# infrastructure

> Infrastructure patterns: Docker multi-stage builds, Kubernetes manifests (cloud-agnostic), security context, 12-Factor App principles, graceful shutdown, resource management, and cloud-native design.

| | |
|---|---|
| **Category** | Knowledge Pack |
| **Referenced by** | `x-task-implement`, `x-story-implement`, `x-review` (DevOps specialist), `devops-engineer` agent |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete reference.

## Topics Covered

- Docker multi-stage builds and layer optimization
- Container security (non-root user, read-only filesystem, capability dropping)
- Kubernetes manifests (Deployments, Services, ConfigMaps, Secrets)
- Kustomize organization (base vs overlays)
- Health probes (liveness, readiness, startup)
- Graceful shutdown and connection draining
- Resource management (CPU/memory requests and limits, QoS classes)
- Cloud-native and 12-Factor App principles

## Key Concepts

This pack provides cloud-agnostic infrastructure patterns covering the full deployment lifecycle from container builds to Kubernetes orchestration. It enforces security hardening at the container level (non-root, read-only filesystem, minimal base images) and mandates proper health probe implementation for all services. Resource management guidelines cover QoS classes, autoscaling configuration, and sizing, while 12-Factor compliance ensures statelessness, configuration externalization, and dev/prod parity.

## See Also

- [observability](../observability/) — Distributed tracing, metrics, structured logging, and health check implementation details
- [resilience](../resilience/) — Graceful degradation, circuit breakers, and timeout patterns that complement infrastructure design
- [security](../security/) — Container image scanning, supply chain security, and SBOM generation
