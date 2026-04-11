---
name: x-security-pipeline
description: >
  Generates CI/CD pipeline configurations with conditional security stages
  based on SecurityConfig flags. Supports GitHub Actions, GitLab CI, and
  Azure DevOps with minimal and full stage modes.
  Reference: `.github/skills/x-security-pipeline/SKILL.md`
---

# Skill: Security CI Pipeline Generator

## Purpose

Generates CI/CD pipeline configuration files with conditional security stages for {{PROJECT_NAME}}. Each stage is included only when its corresponding SecurityConfig flag is enabled. Supports three CI platforms (GitHub Actions, GitLab CI, Azure DevOps), two stage modes (minimal, all), and three trigger types (push, pr, schedule). References atomic scanning skills (x-security-sast, x-security-secrets, x-security-container, x-security-dast, x-owasp-scan, x-security-sonar) instead of duplicating scan logic (RULE-011).

## Triggers

- `/x-security-pipeline` -- generate security pipeline (default: GitHub Actions, all stages, pr trigger)
- `/x-security-pipeline --ci github` -- generate GitHub Actions security pipeline
- `/x-security-pipeline --ci gitlab` -- generate GitLab CI security pipeline
- `/x-security-pipeline --ci azure` -- generate Azure DevOps security pipeline
- `/x-security-pipeline --stages minimal` -- generate minimal pipeline (SAST + secret scan + dependency audit)
- `/x-security-pipeline --stages all` -- generate full pipeline with all 9 conditional stages
- `/x-security-pipeline --trigger schedule` -- generate pipeline with scheduled trigger

## Arguments

| Argument | Type | Default | Description |
|----------|------|---------|-------------|
| `--ci` | Enum | `github` | CI platform: github, gitlab, azure |
| `--stages` | Enum | `all` | Stage mode: all (9 stages), minimal (3 stages) |
| `--trigger` | Enum | `pr` | Pipeline trigger: push, pr, schedule |
| `--fail-on-findings` | Boolean | `true` | Fail pipeline on security findings |
| `--severity-threshold` | Enum | `HIGH` | Minimum severity to fail: CRITICAL, HIGH, MEDIUM |

## Security Stages

| Order | Stage | Skill Reference | Phase | Condition | Minimal |
|-------|-------|----------------|-------|-----------|---------|
| 1 | Secret Scan | x-security-secrets | pre-commit | `security.scanning.secrets = true` | Yes |
| 2 | SAST | x-security-sast | build | `security.scanning.sast = true` | Yes |
| 3 | Dependency Audit | x-dependency-audit | build | Always enabled (baseline) | Yes |
| 4 | SonarQube | x-security-sonar | build | `security.scanning.sonar = true` | No |
| 5 | Container Scan | x-security-container | build | `infrastructure.container != "none"` | No |
| 6 | DAST Passive | x-security-dast | deploy-staging | `security.scanning.dast = true` | No |
| 7 | OWASP Scan | x-owasp-scan | deploy-staging | `security.frameworks contains "owasp"` | No |
| 8 | Hardening Eval | x-hardening-eval | deploy-staging | `security.scanning.hardening = true` | No |
| 9 | Quality Gate | x-security-sonar | gate | `security.scanning.sonar = true` | No |

## Workflow

```
1. READ CONFIG  -> Read SecurityConfig flags and infrastructure settings
2. EVALUATE     -> Evaluate stage conditions against config flags
3. SELECT       -> Filter stages by mode (minimal/all) and conditions
4. RENDER       -> Generate platform-specific YAML from stage definitions
5. VALIDATE     -> Validate generated YAML structure
6. WRITE        -> Write pipeline file to appropriate location
7. REPORT       -> Summarize enabled/disabled stages
```

## Output Files

| Platform | Output File |
|----------|------------|
| GitHub Actions | `.github/workflows/security.yml` |
| GitLab CI | `.gitlab-ci-security.yml` |
| Azure DevOps | `azure-pipelines-security.yml` |

## Composability (RULE-011)

This skill references atomic scanning skills and never duplicates their scan logic. Each CI stage provides the pipeline wrapper (triggers, artifacts, caching) while delegating scan configuration to the referenced skill.

## Error Handling

| Scenario | Action |
|----------|--------|
| Unknown CI platform | Default to GitHub Actions, warn user |
| No SecurityConfig flags set | Generate only Dependency Audit (always enabled) |
| Validation tool missing | Warn and skip validation (non-blocking) |
| All conditions disabled | Generate pipeline with only Dependency Audit stage |
| Container scan without Dockerfile | Exclude container scan, add to disabledStages |
