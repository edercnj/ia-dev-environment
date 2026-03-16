```
ENGINEER: DevOps
STORY: story-0004-0015
SCORE: 20/20
STATUS: Approved
---
PASSED:
- [1] Multi-stage Dockerfile (2/2) — N/A: No Dockerfile changes in this PR. Changes are limited to skill templates (.md), one assembler registration line, golden file updates, and a content test. No container build artifacts affected.
- [2] Non-root user (2/2) — N/A: No container runtime changes. Skill templates are markdown documentation files; they do not define container execution contexts.
- [3] Health check in container (2/2) — N/A: No HEALTHCHECK instruction changes. No Dockerfile or docker-compose modifications present in the diff.
- [4] Resource limits in K8s (2/2) — N/A: No Kubernetes manifests changed. No deployment.yaml, kustomization.yaml, or overlay files touched.
- [5] Security context (2/2) — N/A: No Pod securityContext changes. No K8s manifests in scope.
- [6] Probes configured (2/2) — N/A: No liveness/readiness/startup probe changes. No K8s manifests in scope.
- [7] Config externalized (2/2) — N/A: The new skill template does not introduce any hardcoded configuration values. The SKILL.md uses argument parameters (architecture-plan-path, story-id) passed at invocation time, following the externalized config pattern. The assembler change adds the skill name to a constant array, consistent with existing patterns.
- [8] Secrets via vault/sealed-secrets (2/2) — N/A: No secret management changes. The skill templates contain no credentials, API keys, or sensitive data. No ConfigMap or Secret manifests modified.
- [9] CI pipeline passing (2/2) — N/A: No CI pipeline configuration files (.github/workflows, Jenkinsfile, etc.) changed. The one-line assembler registration and golden file updates are covered by existing integration tests (byte-for-byte golden comparison + new content test at tests/node/content/x-dev-adr-automation-content.test.ts with 25 test cases).
- [10] Image scanning (2/2) — N/A: No container image changes. No base image updates, no dependency additions that would affect container scanning.
FAILED:
(none)
PARTIAL:
(none)
```

### Summary

This PR adds a new ADR Automation skill template (`x-dev-adr-automation`) to the ia-dev-environment generator. The change scope is:

- **2 new markdown skill templates** (Claude + GitHub variants)
- **1 line in `github-skills-assembler.ts`** registering the skill in `SKILL_GROUPS.dev`
- **1 new content test file** (25 test cases covering frontmatter, sections, dual-copy consistency)
- **40 golden file updates** (auto-generated README counts and skill table entries across 8 profiles)

No infrastructure artifacts (Dockerfiles, K8s manifests, CI pipelines, docker-compose, Kustomize overlays, secrets, or container configurations) are present in the diff. All 10 DevOps checklist items are genuinely not applicable to this change and score 2/2.
