---
name: iac-terraform
description: >
  Skill: Terraform Infrastructure as Code -- Provides production-grade Terraform
  patterns covering module structure, remote state management, naming conventions,
  CI/CD workflows, drift detection, and reusable infrastructure modules.
---

# Skill: Terraform Infrastructure as Code

## Description

Provides production-grade Terraform patterns for my-spring-service infrastructure. Covers module structure with environments and reusable modules, remote state management, naming conventions, CI/CD workflows with plan-on-PR and apply-on-merge, drift detection, and common infrastructure modules. All patterns are cloud-agnostic and portable.

**Condition**: This skill applies when IaC tool is "terraform".

## Prerequisites

- Terraform CLI installed (>= 1.7.0)
- Cloud provider credentials configured (via environment or OIDC)
- Remote state backend provisioned
- Understanding of target infrastructure requirements

## Knowledge Pack References

Before provisioning, read the relevant conventions:
- `.claude/skills/iac-terraform/SKILL.md` -- Module structure, remote state, naming conventions, CI/CD workflows, drift detection, common modules
- `.claude/skills/infrastructure/SKILL.md` -- Infrastructure principles, cloud-agnostic design, security context
- `.claude/skills/security/SKILL.md` -- Security hardening, encryption at rest, IAM best practices

## Execution Flow

1. **Set up project structure** -- Create modules + environments layout:
   - `terraform/modules/` -- Reusable infrastructure modules (vpc, database, cache, k8s-cluster)
   - `terraform/environments/dev/` -- Development environment configuration
   - `terraform/environments/staging/` -- Staging environment configuration
   - `terraform/environments/prod/` -- Production environment configuration
   - `terraform/global/` -- Cross-environment resources (IAM, DNS)

2. **Define modules** -- Create reusable infrastructure components:
   - Each module has: `main.tf`, `variables.tf`, `outputs.tf`, `versions.tf`
   - Variables include descriptions and validation rules
   - Outputs expose only necessary values
   - Sensitive outputs marked with `sensitive = true`

3. **Configure remote state** -- Set up state management:
   - Configure backend (S3+DynamoDB, GCS, or Azure Blob)
   - Enable state encryption
   - Enable state locking
   - Enable versioning for state recovery

4. **Apply naming conventions** -- Follow consistent naming:
   - Resources: `snake_case`, descriptive names
   - Variables: `snake_case`, noun-based with descriptions
   - Outputs: `snake_case`, prefixed by resource type
   - Cloud resources: `{project}-{env}-{component}` pattern
   - Standard file set: `main.tf`, `variables.tf`, `outputs.tf`, `versions.tf`

5. **Configure CI/CD** -- Set up automated workflows:
   - Plan on pull request (with PR comment showing changes)
   - Apply on merge to main branch
   - Detect changed environments to avoid unnecessary runs
   - Use OIDC authentication (no long-lived credentials)
   - Sequential apply with `max-parallel: 1`

6. **Set up drift detection** -- Schedule periodic checks:
   - Scheduled plan with `--detailed-exitcode`
   - Notification on drift detected (exit code 2)
   - Alert on error (exit code 1)
   - Run during business hours (Mon-Fri)

7. **Validate configuration** -- Verify infrastructure code:
   - Run `terraform validate` for syntax checking
   - Run `terraform plan` for change preview
   - Review security group rules and IAM policies
   - Check for hardcoded values that should be variables

## Terraform Checklist

- [ ] Module structure follows root + modules + environments pattern
- [ ] Each module has main.tf, variables.tf, outputs.tf, versions.tf
- [ ] Variables have descriptions and validation rules
- [ ] Outputs marked sensitive where appropriate
- [ ] Remote state configured with encryption and locking
- [ ] Naming conventions followed consistently
- [ ] CI/CD pipeline plans on PR, applies on merge
- [ ] OIDC authentication used (no long-lived credentials)
- [ ] Drift detection scheduled and notifications configured
- [ ] Production resources have deletion protection
- [ ] Common tags applied to all resources
- [ ] Provider versions pinned with `~>` constraint
- [ ] Terraform version constraint set in versions.tf
- [ ] No hardcoded secrets in configuration files

## Output Format

```
## Terraform Review -- my-spring-service

### Module Structure: VALID / NEEDS IMPROVEMENT
### State Management: SECURE / NEEDS IMPROVEMENT
### CI/CD Integration: COMPLETE / PARTIAL / MISSING

### Module Findings
1. [Finding with module path, issue, fix]

### State Management Findings
1. [Finding with backend config, issue, fix]

### CI/CD Findings
1. [Finding with workflow file, issue, fix]

### Checklist Results
[Items that passed / failed / not applicable]

### Verdict: APPROVE / REQUEST CHANGES
```

## Detailed References

For in-depth guidance on Terraform patterns, consult:
- `.claude/skills/iac-terraform/SKILL.md`
- `.claude/skills/infrastructure/SKILL.md`
- `.claude/skills/security/SKILL.md`
