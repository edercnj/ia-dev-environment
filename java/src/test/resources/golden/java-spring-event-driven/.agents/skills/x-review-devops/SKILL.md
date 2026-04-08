---
name: x-review-devops
description: "DevOps specialist review: validates Dockerfile, container security, CI/CD pipeline, resource limits, health probes, graceful shutdown, and deployment configuration."
user-invocable: true
allowed-tools: Read, Grep, Glob, Bash
argument-hint: "[PR number or file paths]"
context-budget: light
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: DevOps Specialist Review

## Purpose

Review code changes for DevOps best practices: Dockerfile multi-stage builds, container security hardening, CI/CD pipeline configuration, resource limits, health probe configuration, graceful shutdown implementation, and deployment manifests.

## Activation Condition

Include this skill when `container != "none"` in the project configuration.

## When to Use

- Pre-PR quality validation for infrastructure changes
- Reviewing Dockerfile and container configuration
- Checking deployment manifests
- Validating CI/CD pipeline changes

## Triggers

- `/x-review-devops 42` -- review PR #42 for DevOps patterns
- `/x-review-devops Dockerfile` -- review Dockerfile specifically
- `/x-review-devops` -- review all current infrastructure changes

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `target` | String | No | (current changes) | PR number or file paths to review |

## Knowledge Pack References

| Pack | Files | Purpose |
|------|-------|---------|
| infrastructure | `skills/infrastructure/SKILL.md` | Docker, Kubernetes, 12-Factor, graceful shutdown, resource management |

## Checklist (10 Items, Max Score: /20)

Each item scores 0 (missing), 1 (partial), or 2 (fully compliant).

### Dockerfile (DEVOPS-01 to DEVOPS-04)

| # | Item | Score |
|---|------|-------|
| DEVOPS-01 | Multi-stage build (separate build and runtime stages) | /2 |
| DEVOPS-02 | Non-root user in final stage (no running as root) | /2 |
| DEVOPS-03 | Minimal base image (distroless, alpine, or slim) | /2 |
| DEVOPS-04 | .dockerignore configured (excludes build artifacts, tests, docs) | /2 |

### Container Security (DEVOPS-05 to DEVOPS-06)

| # | Item | Score |
|---|------|-------|
| DEVOPS-05 | No secrets in image layers (use runtime env vars or secrets manager) | /2 |
| DEVOPS-06 | Image pinned to specific digest or version (no :latest tag) | /2 |

### Deployment & Operations (DEVOPS-07 to DEVOPS-10)

| # | Item | Score |
|---|------|-------|
| DEVOPS-07 | Resource limits defined (CPU, memory) for container/pod | /2 |
| DEVOPS-08 | Health probes configured (liveness, readiness, startup) in deployment manifest | /2 |
| DEVOPS-09 | Graceful shutdown implemented (SIGTERM handling, connection draining) | /2 |
| DEVOPS-10 | Environment-specific configuration externalized (no hardcoded values) | /2 |

## Workflow

### Step 1 -- Gather Context

Read the infrastructure knowledge pack:
- `skills/infrastructure/SKILL.md`

### Step 2 -- Identify Changed Files

Determine scope: Dockerfile, docker-compose, Kubernetes manifests, CI/CD config.

### Step 3 -- Dockerfile Review

Check multi-stage build, non-root user, minimal base image, .dockerignore.

### Step 4 -- Security Review

Verify no secrets in layers, image version pinning.

### Step 5 -- Deployment Review

Check resource limits, health probes, graceful shutdown, config externalization.

### Step 6 -- Generate Report

Produce the scored report.

## Output Format

```
ENGINEER: DevOps
STORY: [story-id or change description]
SCORE: XX/20

STATUS: PASS | FAIL | PARTIAL

### PASSED
- [DEVOPS-XX] [Item description]

### FAILED
- [DEVOPS-XX] [Item description]
  - Finding: [file:line] [issue description]
  - Fix: [remediation guidance]

### PARTIAL
- [DEVOPS-XX] [Item description]
  - Finding: [partial compliance details]
```

## Error Handling

| Scenario | Action |
|----------|--------|
| No Dockerfile found | Report INFO: no container configuration discovered |
| No deployment manifests found | Skip DEVOPS-07, DEVOPS-08 and note N/A |
| No CI/CD config found | Warn and proceed with available files |
