---
name: k8s-kustomize
description: >
  Skill: Kustomize Environment Management -- Provides production-grade Kustomize
  patterns for managing Kubernetes manifests across multiple environments using
  base, overlays, components, patches, and generators.
---

# Skill: Kustomize Environment Management

## Description

Provides production-grade Kustomize patterns for managing Kubernetes manifests for my-go-service across multiple environments (dev, staging, prod). Covers directory structure, base and overlay organization, strategic merge patches, JSON 6902 patches, components for cross-cutting concerns, and generators for ConfigMaps and Secrets.

**Condition**: This skill applies when infrastructure templating is "kustomize".

## Prerequisites

- Kubernetes cluster accessible via `kubectl`
- `kustomize` CLI installed (or `kubectl` with built-in kustomize support)
- Base manifests defined for the application
- Environment-specific configuration identified

## Knowledge Pack References

Before configuring, read the relevant conventions:
- `.claude/skills/k8s-kustomize/SKILL.md` -- Directory structure, patches, components, secret management, generators, patch types
- `.claude/skills/k8s-deployment/SKILL.md` -- Base deployment manifests and pod specifications
- `.claude/skills/infrastructure/SKILL.md` -- Infrastructure principles and security context

## Execution Flow

1. **Set up directory structure** -- Create base + overlays layout:
   - `k8s/base/` -- Common manifests (deployment, service, serviceaccount, hpa, pdb, networkpolicy)
   - `k8s/overlays/dev/` -- Development environment overrides
   - `k8s/overlays/staging/` -- Staging environment overrides
   - `k8s/overlays/prod/` -- Production environment overrides
   - `k8s/components/` -- Reusable cross-cutting concerns

2. **Define base kustomization** -- Create base configuration:
   - List all resource files
   - Set common labels (app.kubernetes.io/name, managed-by)
   - Keep base environment-neutral

3. **Create environment overlays** -- Configure per-environment:
   - Set namespace
   - Reference base via `../../base`
   - Include relevant components
   - Apply patches for replicas, resources, environment variables
   - Configure ConfigMap generators from environment files
   - Set image tags via `images` transformer

4. **Create components** -- Extract cross-cutting concerns:
   - Observability sidecar injection
   - Security context enforcement
   - Service mesh sidecar configuration

5. **Define patches** -- Customize per environment:
   - Strategic merge patches for simple field changes (replicas, resources)
   - JSON 6902 patches for array operations (sidecar injection, volume addition)
   - Inline patches for small, targeted changes

6. **Configure generators** -- Manage configuration:
   - ConfigMapGenerator from environment files
   - SecretGenerator for development defaults
   - SealedSecrets or ExternalSecrets for production

7. **Validate output** -- Verify generated manifests:
   - Run `kustomize build k8s/overlays/<env>` for each environment
   - Verify no duplicate resources
   - Check that patches apply correctly
   - Validate YAML syntax

## Kustomize Checklist

- [ ] Base manifests are environment-neutral
- [ ] Each overlay sets namespace explicitly
- [ ] Replicas scaled appropriately per environment
- [ ] Resource requests/limits adjusted per environment
- [ ] Environment variables configured per environment
- [ ] ConfigMap generators use environment-specific files
- [ ] Components are reusable across overlays
- [ ] Patches use correct type (strategic merge vs JSON 6902)
- [ ] Image tags managed via kustomization `images` field
- [ ] Secret management uses SealedSecrets or ExternalSecrets in production
- [ ] `kustomize build` produces valid YAML for each overlay
- [ ] No hardcoded environment-specific values in base

## Output Format

```
## Kustomize Review -- my-go-service

### Structure: VALID / NEEDS IMPROVEMENT
### Environment Separation: HIGH / MEDIUM / LOW

### Directory Structure Findings
1. [Finding with path, issue, fix]

### Patch Findings
1. [Finding with patch file, issue, fix]

### Generator Findings
1. [Finding with generator config, issue, fix]

### Checklist Results
[Items that passed / failed / not applicable]

### Verdict: APPROVE / REQUEST CHANGES
```

## Detailed References

For in-depth guidance on Kustomize patterns, consult:
- `.claude/skills/k8s-kustomize/SKILL.md`
- `.claude/skills/k8s-deployment/SKILL.md`
- `.claude/skills/infrastructure/SKILL.md`
