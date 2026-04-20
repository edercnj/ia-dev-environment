ENGINEER: DevOps
STORY: story-0045-0002
SCORE: 16/20

STATUS: PARTIAL

### PASSED
- [DEVOPS-01] Multi-stage build — Dockerfile uses eclipse-temurin:21-jdk-alpine AS builder (build) + eclipse-temurin:21-jre-alpine (runtime). Compliant.
- [DEVOPS-02] Non-root user — addgroup -S appgroup && adduser -S appuser -G appgroup + USER appuser. Compliant.
- [DEVOPS-03] Minimal base image — eclipse-temurin:21-jre-alpine (JRE-only alpine) in runtime stage. Compliant.
- [DEVOPS-05] No secrets in image layers — Dockerfile contains no hardcoded credentials. Compliant.
- [DEVOPS-09] Graceful shutdown — ENTRYPOINT uses exec form; JVM handles SIGTERM. Compliant.
- [DEVOPS-10] Config externalized — no hardcoded values beyond port declaration. Compliant.

### PARTIAL
- [DEVOPS-04] .dockerignore — not part of this story's diff; existing .dockerignore status unverified.
  - Finding: .dockerignore not updated; cannot confirm target/, .claude/, plans/ excluded
  - Fix: Verify .dockerignore excludes build artifacts and project-specific dirs — LOW priority

### FAILED
- [DEVOPS-06] Image pinned to digest — pre-existing: eclipse-temurin:21-jdk-alpine and :21-jre-alpine use floating tags
  - Finding: Dockerfile:2,12 — mutable tags allow silent base image changes between CI rebuilds [LOW]
  - Fix: Pin to digest: FROM eclipse-temurin:21-jdk-alpine@sha256:<digest> AS builder
  - NOTE: Pre-existing finding, not introduced by story-0045-0002. Tool not deployed to production.

- [DEVOPS-07] Resource limits — no K8s deployment manifest in diff
  - Finding: No CPU/memory limits defined [LOW — tool is not deployed to K8s]
  - Fix: Add resources.limits if deploying to container orchestrator; N/A if local tool only

- [DEVOPS-08] Health probes — HEALTHCHECK in Dockerfile but no K8s liveness/readiness probes
  - Finding: No K8s probe config [LOW — N/A if not deployed to K8s]
  - Fix: Add liveness/readiness probes in K8s deployment manifest if applicable
