---
name: setup-environment
description: >
  Skill: Dev Environment Setup -- Manages the local development environment
  lifecycle including starting/stopping the container orchestrator, provisioning
  dependencies, building the application, and verifying health checks.
---

# Skill: Dev Environment Setup

## Description

Manages the local development environment lifecycle for my-ktor-service. Handles starting/stopping the container orchestrator, provisioning dependencies, building the application with gradle, and verifying health checks.

**Condition**: This skill applies when orchestrator is not "none".

## Prerequisites

- Container runtime (docker) installed and accessible in PATH
- Orchestrator (kubernetes) installed and configured
- Build tool (gradle) installed for application builds
- kotlin 2.0 runtime available

## Knowledge Pack References

Before executing, read the relevant conventions:
- `.claude/skills/infrastructure/SKILL.md` -- Infrastructure principles, 12-Factor App, graceful shutdown
- `.claude/skills/dockerfile/SKILL.md` -- Container image build patterns and security hardening
- `.claude/skills/k8s-deployment/SKILL.md` -- Kubernetes deployment patterns and probes

## Execution Flow

1. **Verify prerequisites** -- Check that required tools are installed:
   - Verify container runtime is running (`docker info` or equivalent)
   - Verify orchestrator is accessible
   - Verify build tool is available
   - Report missing tools with installation instructions

2. **Start environment** (`--start`):
   - Start orchestrator (e.g., `minikube start`, `docker-compose up -d`)
   - Wait for orchestrator to be ready
   - Build application using gradle
   - Build container image with multi-stage Dockerfile
   - Deploy application and dependencies
   - Wait for all services to be healthy
   - Set up port-forwarding or networking as needed

3. **Stop environment** (`--stop`):
   - Stop port-forwarding / networking
   - Tear down application and dependencies
   - Stop orchestrator if applicable

4. **Check status** (`--status`):
   - Show orchestrator status
   - List running services/pods/containers
   - Check health endpoints
   - Report dependency connectivity

5. **Rebuild** (`--build`):
   - Rebuild application using gradle
   - Rebuild container image
   - Rolling restart / redeploy
   - Wait for health checks to pass

6. **Report status** -- After execution:
   - If successful: show access URLs (API, health, docs)
   - If failed: show error message and suggested fix

## Environment Checklist

- [ ] Container runtime (docker) is running
- [ ] Orchestrator (kubernetes) is accessible
- [ ] Application builds successfully with gradle
- [ ] Container image builds without errors
- [ ] All services start and pass health checks
- [ ] Port-forwarding is configured correctly
- [ ] Dependencies are reachable from the application
- [ ] Logs are accessible for debugging

## Troubleshooting

| Problem | Likely Cause | Solution |
|---------|-------------|----------|
| Orchestrator won't start | Container runtime not running | Start container runtime first |
| Service CrashLoop | Application error | Check application logs |
| Image not found | Image not built | Run with `--build` flag |
| Port conflict | Port already in use | Stop conflicting service or change port |
| Health check timeout | Application still starting | Wait and check logs |

## Output Format

```
## Environment Status -- my-ktor-service

### Orchestrator: RUNNING / STOPPED
### Services: N/N healthy
### Health Checks: PASS / FAIL

### Access URLs
- API: http://localhost:PORT
- Health: http://localhost:PORT/healthz
- Docs: http://localhost:PORT/docs (if available)
```

## Detailed References

For in-depth guidance on environment setup, consult:
- `.claude/skills/setup-environment/SKILL.md`
- `.claude/skills/infrastructure/SKILL.md`
- `.claude/skills/dockerfile/SKILL.md`
