---
name: setup-environment
description: "Sets up the local development environment including container orchestrator, database, and build tools."
user-invocable: true
allowed-tools: Bash, Read, Write
argument-hint: "[--start | --stop | --status | --build]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Dev Environment Setup

## Purpose

Manage the local development environment lifecycle. Handle starting/stopping the container orchestrator ({{ORCHESTRATOR}}), provisioning the database ({{DB_TYPE}}), building the application, and verifying health checks.

## Activation Condition

Include this skill when orchestrator is not "none" in the project configuration.

## Triggers

- `/setup-environment --start` -- start orchestrator, build, deploy, and set up networking
- `/setup-environment --stop` -- stop services, tear down containers, and clean up
- `/setup-environment --status` -- show running services and health check results
- `/setup-environment --build` -- rebuild application image and restart services

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `--start` | Flag | No | false | Start orchestrator, build image, deploy, set up networking |
| `--stop` | Flag | No | false | Stop services, tear down containers, clean up |
| `--status` | Flag | No | false | Show running services, pods/containers, health check results |
| `--build` | Flag | No | false | Rebuild application image and restart services |

## Prerequisites

- {{ORCHESTRATOR}} installed and accessible in PATH
- {{DB_TYPE}} client tools available (optional, for manual queries)
- {{BUILD_TOOL}} installed (for application builds)
- Container runtime (Docker or Podman) running

## Workflow

### Step 1 — Verify Prerequisites

Check that required tools are installed:
- Run `which` or `command -v` for each required tool
- Verify container runtime is running (`docker info` or `podman info`)
- Report missing tools with installation instructions

### Step 2 — Execute Requested Operation

**--start:**
- Start {{ORCHESTRATOR}} (e.g., `minikube start`, `docker-compose up -d`)
- Wait for orchestrator to be ready
- Build application: `{{BUILD_COMMAND}}`
- Build container image
- Deploy application and dependencies ({{DB_TYPE}}, etc.)
- Wait for all services to be healthy
- Set up port-forwarding or networking as needed

**--stop:**
- Stop port-forwarding / networking
- Tear down application and dependencies
- Stop {{ORCHESTRATOR}} if applicable

**--status:**
- Show orchestrator status
- List running services/pods/containers
- Check health endpoints
- Report database connectivity

**--build:**
- Rebuild application: `{{BUILD_COMMAND}}`
- Rebuild container image
- Rolling restart / redeploy
- Wait for health checks to pass

### Step 3 — Report Status

After execution:
- If successful: show access URLs (API, health, docs)
- If failed: show error message and suggested fix

## Helm Mode (when infrastructure.templating == helm)

**--start:**
1. Verify prerequisites: helm, kubectl
2. Build application: `{{BUILD_COMMAND}}`
3. Build container image: `docker build -t {{PROJECT_NAME}}:dev .`
4. Install/upgrade chart: `helm upgrade --install {{PROJECT_NAME}} ./chart -f chart/values-dev.yaml --wait --timeout 120s`
5. Wait for pods ready: `kubectl wait --for=condition=ready pod -l app={{PROJECT_NAME}} --timeout=120s`
6. Port-forward: `kubectl port-forward svc/{{PROJECT_NAME}} {{PORT}}:{{PORT}}`

**--stop:**
1. Stop port-forward
2. Uninstall chart: `helm uninstall {{PROJECT_NAME}}`

**--build:**
1. Build application
2. Rebuild image
3. Upgrade chart: `helm upgrade {{PROJECT_NAME}} ./chart -f chart/values-dev.yaml --wait`

**--status:**
1. `helm list` (show installed charts)
2. `helm status {{PROJECT_NAME}}`
3. `kubectl get pods -l app={{PROJECT_NAME}}`
4. Health check: `curl -s http://localhost:{{PORT}}/health`

## Related Files

- `scripts/dev-setup.sh` -- Main setup script (if exists)
- Container orchestration manifests (k8s/, docker-compose.yml)
- Helm chart directory (chart/)
- Application configuration files

## Error Handling

| Scenario | Action |
|----------|--------|
| Orchestrator won't start | Report container runtime not running, suggest starting Docker/Podman first |
| Service CrashLoop | Report application error, suggest checking application logs |
| Image not found | Suggest running with `--build` flag |
| Port conflict | Report port already in use, suggest stopping conflicting service or changing port |
| Health check timeout | Suggest waiting 30s then checking logs |
| DB connection error | Report database not ready, suggest checking database container logs |
