# dockerfile

> Dockerfile patterns per language covering multi-stage builds, security hardening, .dockerignore templates, layer optimization, health checks, and OCI labels.

| | |
|---|---|
| **Category** | Infrastructure Pattern |
| **Supplements** | infrastructure |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete pattern reference.

## Key Patterns

- Multi-stage build templates for Java, Go, Python, TypeScript, Rust, and .NET
- Security hardening with non-root users and minimal base images
- .dockerignore templates to reduce build context
- Layer optimization for dependency caching
- Health check configuration per language
- OCI labels and image metadata

## See Also

- [container-registry](../container-registry/) — Tagging strategy, immutability, vulnerability scanning
- [k8s-deployment](../k8s-deployment/) — Kubernetes workload types, pod specs, probes
- [infrastructure](../../infrastructure/) — Cloud-agnostic infrastructure patterns and 12-Factor principles
