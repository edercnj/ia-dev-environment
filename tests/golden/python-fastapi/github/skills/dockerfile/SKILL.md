---
name: dockerfile
description: >
  Skill: Dockerfile Multi-Stage Build -- Provides production-grade Dockerfile
  patterns covering multi-stage builds, security hardening, layer optimization,
  health checks, OCI labels, and .dockerignore templates.
---

# Skill: Dockerfile Multi-Stage Build

## Description

Provides production-grade Dockerfile patterns for my-fastapi-service using python 3.12. Covers multi-stage builds for minimal image size, security hardening with non-root users, layer optimization for fast builds, health check configuration, OCI labels, and .dockerignore templates.

**Condition**: This skill applies when container is not "none".

## Prerequisites

- Container runtime (docker) installed and running
- Application source code with build configuration
- pip configured for the project
- Understanding of python runtime requirements

## Knowledge Pack References

Before building, read the relevant conventions:
- `.claude/skills/dockerfile/SKILL.md` -- Multi-stage builds, security hardening, .dockerignore, layer optimization, health checks, OCI labels
- `.claude/skills/infrastructure/SKILL.md` -- Infrastructure principles, container security, 12-Factor App
- `.claude/skills/security/SKILL.md` -- Security hardening and vulnerability scanning

## Execution Flow

1. **Design multi-stage build** -- Separate build and runtime:
   - Build stage: full SDK/tools, compile application
   - Runtime stage: minimal base image, copy only artifacts
   - Optional: native build stage for GraalVM/AOT compilation

2. **Optimize layer caching** -- Order instructions by change frequency:
   - Base image (changes rarely)
   - System packages (changes rarely)
   - Create non-root user (changes rarely)
   - Copy dependency manifests (changes on dep update)
   - Install dependencies with cache mounts (cached if manifests unchanged)
   - Copy source code (changes frequently)
   - Build application
   - Final stage: copy artifacts only

3. **Apply security hardening** -- Minimize attack surface:
   - Use minimal base images (distroless, alpine, slim variants)
   - Create and switch to non-root user (UID 10001)
   - Do not install package managers in runtime stage
   - Pin base image versions (never use `latest`)
   - Never copy secrets into the image
   - Scan for CVEs with Trivy or Grype

4. **Configure health checks** -- Add HEALTHCHECK instruction:
   - Set appropriate timing based on python startup time
   - Use language-native health check commands
   - Configure start-period, interval, timeout, and retries

5. **Add OCI labels** -- Apply standard metadata:
   - org.opencontainers.image.title, description, version
   - org.opencontainers.image.created, revision, source
   - Pass values via build arguments

6. **Create .dockerignore** -- Exclude unnecessary files:
   - Version control (.git)
   - IDE files (.idea, .vscode)
   - CI/CD configuration
   - Documentation
   - Test files and coverage reports
   - Build artifacts
   - Environment and secret files

7. **Validate image** -- Verify the built image:
   - Image size is reasonable for the runtime
   - Runs as non-root user
   - Health check passes
   - No unnecessary packages or files included

## Dockerfile Checklist

- [ ] Multi-stage build separates build and runtime
- [ ] Minimal base image used (distroless, alpine, or slim)
- [ ] Non-root user created and active (USER instruction)
- [ ] No package managers in runtime stage
- [ ] Base image versions pinned (no `latest` tag)
- [ ] Dependencies cached with BuildKit cache mounts
- [ ] Layer order optimized (least changing first)
- [ ] HEALTHCHECK instruction configured with appropriate timing
- [ ] OCI labels applied via build arguments
- [ ] .dockerignore excludes unnecessary files
- [ ] No secrets copied into the image
- [ ] ENTRYPOINT uses exec form (JSON array)
- [ ] EXPOSE declares the application port
- [ ] Multi-platform build supported (if applicable)

## Output Format

```
## Dockerfile Review -- my-fastapi-service

### Security: HIGH / MEDIUM / LOW
### Layer Optimization: HIGH / MEDIUM / LOW
### Image Size: OPTIMAL / ACCEPTABLE / NEEDS REDUCTION

### Security Findings
1. [Finding with Dockerfile line, issue, fix]

### Optimization Findings
1. [Finding with Dockerfile line, issue, fix]

### Checklist Results
[Items that passed / failed / not applicable]

### Verdict: APPROVE / REQUEST CHANGES
```

## Detailed References

For in-depth guidance on Dockerfile patterns, consult:
- `.claude/skills/dockerfile/SKILL.md`
- `.claude/skills/infrastructure/SKILL.md`
- `.claude/skills/security/SKILL.md`
