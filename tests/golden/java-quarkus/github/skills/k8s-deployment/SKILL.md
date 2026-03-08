---
name: k8s-deployment
description: >
  Skill: Kubernetes Deployment Patterns -- Provides production-grade Kubernetes
  deployment patterns covering workload types, pod specifications, resource
  sizing, probes, autoscaling, network policies, and security contexts.
---

# Skill: Kubernetes Deployment Patterns

## Description

Provides production-grade Kubernetes deployment patterns for my-quarkus-service. Covers workload type selection, pod specifications, resource sizing for java runtime, probe configuration, autoscaling, network policies, and security contexts using vanilla Kubernetes (cloud-agnostic).

**Condition**: This skill applies when orchestrator is "kubernetes".

## Prerequisites

- Kubernetes cluster accessible via `kubectl`
- Container image built and available in registry
- Namespace created for the target environment
- RBAC configured for deployment

## Knowledge Pack References

Before deploying, read the relevant conventions:
- `.claude/skills/k8s-deployment/SKILL.md` -- Workload types, pod specs, resource sizing, probes, autoscaling, network policies, security contexts
- `.claude/skills/infrastructure/SKILL.md` -- Infrastructure principles, 12-Factor App, security context
- `.claude/skills/observability/SKILL.md` -- Prometheus annotations, health check endpoints

## Execution Flow

1. **Select workload type** -- Choose the appropriate controller:
   - Stateless web/API service: Deployment
   - Stable network identity or persistent storage: StatefulSet
   - Must run on every node: DaemonSet
   - One-time batch processing: Job
   - Scheduled recurring task: CronJob

2. **Configure pod specification** -- Define container spec:
   - Set resource requests and limits based on java runtime
   - Configure startup, liveness, and readiness probes
   - Apply security context (non-root, read-only filesystem, drop all capabilities)
   - Set topology spread constraints for high availability
   - Mount writable directories as emptyDir volumes

3. **Configure autoscaling** -- Set scaling policies:
   - Define HPA with CPU and memory targets
   - Configure scale-up and scale-down stabilization windows
   - Set Pod Disruption Budget for maintenance operations

4. **Configure networking** -- Define network policies:
   - Default deny-all ingress and egress
   - Allow specific ingress from ingress controller
   - Allow DNS resolution
   - Allow egress to required services

5. **Configure observability** -- Enable telemetry:
   - Set OTEL environment variables (OTEL_SERVICE_NAME, OTEL_EXPORTER_OTLP_ENDPOINT)
   - Configure trace sampling strategy (parentbased_traceidratio for prod, always_on for dev)
   - Add Prometheus scrape annotations to pod template
   - Ensure structured JSON logging to stdout
   - Inject trace context propagation headers

6. **Apply security context** -- Harden the deployment:
   - Pod-level: runAsNonRoot, seccompProfile RuntimeDefault
   - Container-level: allowPrivilegeEscalation false, readOnlyRootFilesystem true
   - ServiceAccount with disabled token auto-mount
   - Pod Security Standards (PSS) labels on namespace

7. **Validate deployment** -- Verify the deployment:
   - All pods are running and ready
   - Health checks are passing
   - Network policies are applied
   - Resource limits are respected

## Deployment Checklist

- [ ] Workload type matches application characteristics
- [ ] Resource requests and limits are set (based on java sizing guide)
- [ ] Startup, liveness, and readiness probes configured
- [ ] Security context applied (non-root, read-only FS, drop capabilities)
- [ ] Pod Disruption Budget defined
- [ ] HPA configured with appropriate thresholds
- [ ] Network policies restrict traffic to required paths only
- [ ] Topology spread constraints ensure zone distribution
- [ ] ServiceAccount created with minimal permissions
- [ ] OCI labels applied to container image
- [ ] Rolling update strategy configured (maxSurge, maxUnavailable)
- [ ] Revision history limit set
- [ ] OTEL environment variables configured (service name, exporter endpoint)
- [ ] Trace sampling strategy set per environment
- [ ] Prometheus scrape annotations on pod template
- [ ] Structured JSON logging configured

## Output Format

```
## Kubernetes Deployment Review -- my-quarkus-service

### Workload Type: Deployment / StatefulSet / DaemonSet
### Security: HIGH / MEDIUM / LOW
### Resource Sizing: APPROPRIATE / NEEDS ADJUSTMENT

### Findings
1. [Finding with manifest file, issue, fix]

### Checklist Results
[Items that passed / failed / not applicable]

### Verdict: APPROVE / REQUEST CHANGES
```

## Detailed References

For in-depth guidance on Kubernetes deployment, consult:
- `.claude/skills/k8s-deployment/SKILL.md`
- `.claude/skills/infrastructure/SKILL.md`
- `.claude/skills/security/SKILL.md`
