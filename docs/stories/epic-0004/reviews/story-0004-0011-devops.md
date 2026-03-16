ENGINEER: DevOps
STORY: story-0004-0011
SCORE: 14/20
STATUS: Rejected
---
PASSED:
- [DEVOPS-01] Multi-stage Dockerfile — builder + runtime stages in Dockerfile templates (2/2)
  All 8 Dockerfile templates use two-stage builds: `AS builder` for compilation, minimal runtime image for production.
  Verified in templates: `Dockerfile.typescript-npm.njk`, `Dockerfile.go-go-mod.njk`, `Dockerfile.java-gradle.njk`,
  `Dockerfile.python-pip.njk`, `Dockerfile.rust-cargo.njk`, and all golden outputs (typescript-nestjs, python-click-cli, go-gin, rust-axum, java-spring).

- [DEVOPS-02] Non-root user — Dockerfiles create/use non-root user (2/2)
  All Dockerfile templates create a dedicated `appuser` in a dedicated `appgroup` and switch to it via `USER appuser`.
  Alpine variants use `addgroup -S`/`adduser -S`; Debian/Python variants use `groupadd -r`/`useradd -r`.
  Verified in all templates and golden outputs.

- [DEVOPS-03] Health check in container — HEALTHCHECK instruction in Dockerfiles (2/2)
  All 8 Dockerfile templates include `HEALTHCHECK --interval=30s --timeout=5s --retries=3` with appropriate
  health check commands per stack (wget for alpine, curl for rust/debian, python urllib for python).
  Verified in templates and golden file outputs.

- [DEVOPS-04] Resource limits in K8s — Deployment has resource requests/limits (2/2)
  `deployment.yaml.njk` defines both requests (`memory: "256Mi"`, `cpu: "250m"`) and limits (`memory: "512Mi"`, `cpu: "500m"`).
  Verified in golden outputs: `typescript-nestjs/k8s/deployment.yaml:36-40`, `go-gin/k8s/deployment.yaml:36-40`.

- [DEVOPS-06] Probes configured — liveness/readiness probes in Deployment (2/2)
  `deployment.yaml.njk` includes both `livenessProbe` and `readinessProbe` with `httpGet` on the health path and port.
  `initialDelaySeconds` and `periodSeconds` are configured for both.
  Verified in golden outputs: `typescript-nestjs/k8s/deployment.yaml:22-33`, `go-gin/k8s/deployment.yaml:22-33`.

- [DEVOPS-07] Config externalized — ConfigMap for environment-specific config (2/2)
  `configmap.yaml.njk` generates a ConfigMap with non-sensitive config (APP_ENV, LOG_LEVEL).
  The Deployment references it via `envFrom.configMapRef`. No credentials are present in ConfigMap.
  Verified in golden outputs: `typescript-nestjs/k8s/configmap.yaml`, `go-gin/k8s/configmap.yaml`.

- [DEVOPS-08] Secrets via vault/sealed-secrets — no hardcoded secrets in templates (2/2)
  No secrets are hardcoded in any K8s manifests, Dockerfiles, or CI workflow templates.
  Docker Compose uses placeholder values (dbuser/dbpass) for local development only, which is acceptable.
  No Secret manifest is generated (intentional — secrets are left to vault/sealed-secrets per infrastructure principles).

FAILED:
- [DEVOPS-05] Security context in K8s — Pod/container security context defined (0/2) — resources/cicd-templates/k8s/deployment.yaml.njk:17-43 — Fix: Add pod-level `securityContext` (runAsNonRoot, runAsUser: 1001, seccompProfile: RuntimeDefault) and container-level `securityContext` (allowPrivilegeEscalation: false, readOnlyRootFilesystem: true, capabilities.drop: ["ALL"]) as required by infrastructure-principles.md Restricted PSS section. Also add `automountServiceAccountToken: false` and `/tmp` emptyDir volume. [CRITICAL]

- [DEVOPS-09] CI pipeline quality — CI workflow has build, test, lint jobs (0/2) — resources/cicd-templates/ci-workflow/ci.yml.njk:12-95 — Fix: The CI workflow has Build, Test, and Coverage steps but is missing a **Lint** step. The infrastructure principles mandate quality gates. Add a lint step (e.g., `npm run lint`, `golangci-lint run`, `cargo clippy`, `flake8`/`ruff`) per language. Additionally, all steps are in a single `build-and-test` job rather than separate parallel jobs for build, test, and lint — consider splitting for faster feedback. [HIGH]

- [DEVOPS-10] Image scanning — CI includes or documents image security scanning (0/2) — resources/cicd-templates/ci-workflow/ci.yml.njk — Fix: No image scanning step exists in the CI workflow. Add a container image scanning step using Trivy, Grype, or Snyk (e.g., `aquasecurity/trivy-action@master`). The infrastructure principles require "Immutable Infrastructure: Build once, deploy everywhere" and the deploy checklist requires "Container image created and tested." Security scanning is a mandatory quality gate for container images. [HIGH]

PARTIAL:
(none)

---

## Summary of Blocking Issues

### 1. Missing Security Context in K8s Deployment (DEVOPS-05)
**File:** `resources/cicd-templates/k8s/deployment.yaml.njk`
The Deployment template has zero security context configuration. The infrastructure principles (Restricted PSS) mandate:
- Pod-level: `runAsNonRoot: true`, `runAsUser: 1001`, `runAsGroup: 1001`, `fsGroup: 1001`, `seccompProfile: RuntimeDefault`
- Container-level: `allowPrivilegeEscalation: false`, `readOnlyRootFilesystem: true`, `capabilities.drop: ["ALL"]`
- `/tmp` emptyDir volume with sizeLimit
- `automountServiceAccountToken: false`

This is the most critical gap. Without security context, pods run with default (permissive) settings, violating the project's mandatory Restricted Pod Security Standard.

### 2. No Lint Step in CI (DEVOPS-09)
**File:** `resources/cicd-templates/ci-workflow/ci.yml.njk`
The workflow only has build, test, and coverage. A lint/static analysis step is missing. The quality gates rule (05) requires "Zero compiler/linter warnings" at merge time, which implies linting must be part of CI.

### 3. No Image Scanning in CI (DEVOPS-10)
**File:** `resources/cicd-templates/ci-workflow/ci.yml.njk`
No container image vulnerability scanning. The infrastructure principles require immutable, tested container images. Security scanning should be a CI step for any profile that builds Docker images.

### Additional Observations (Non-Blocking)
- **Missing startupProbe:** The Deployment template only has liveness and readiness probes. Infrastructure principles define three probes (startup, liveness, readiness) with build-type-specific timing.
- **Missing OCI labels:** Dockerfiles lack `org.opencontainers.image.*` labels (required by container rules table).
- **Missing standard K8s labels:** K8s manifests use only `app:` label instead of the mandatory `app.kubernetes.io/name`, `app.kubernetes.io/version`, `app.kubernetes.io/component`, `app.kubernetes.io/part-of`, `app.kubernetes.io/managed-by: kustomize` labels.
- **Missing graceful shutdown:** Deployment template lacks `terminationGracePeriodSeconds` and `preStop` lifecycle hook.
- **Docker Compose db service lacks healthcheck:** When database is enabled, the `db` service in docker-compose template has no `healthcheck` and no `depends_on.condition: service_healthy`, contrary to the infrastructure principles example.
- **`image: latest` in Deployment:** The K8s deployment uses `{{ project_name }}:latest` which violates "NEVER use latest in production."
