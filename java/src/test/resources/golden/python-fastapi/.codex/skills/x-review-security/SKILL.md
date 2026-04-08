---
name: x-review-security
description: "Reviews code changes for compliance with selected security frameworks. Verifies sensitive data handling, audit trails, and access control patterns."
user-invocable: true
allowed-tools: Read, Grep, Glob, Bash
argument-hint: "[PR number or file paths]"
context-budget: light
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Security Compliance Review

## Purpose

Review code changes against the compliance frameworks selected in the project configuration. Verify sensitive data handling, audit trails, access control patterns, and cryptography usage per active framework requirements.

## Activation Condition

Include this skill when the project has compliance frameworks configured (PCI-DSS, LGPD, GDPR, HIPAA, SOX).

## Triggers

- `/x-review-security 42` -- review PR #42 for security compliance
- `/x-review-security src/main/java/com/example/auth/` -- review specific file paths
- `/x-review-security` -- review all current changes

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `target` | String | No | (current changes) | PR number or file paths to review |

## Knowledge Pack References

| Pack | Files | Purpose |
|------|-------|---------|
| security | `skills/security/references/security-principles.md` | Data classification, input validation, fail-secure patterns |
| security | `skills/security/references/application-security.md` | OWASP Top 10, security headers, secrets management |
| security | `skills/security/references/cryptography.md` | TLS, hashing, key management |
| compliance | `skills/compliance/SKILL.md` and `skills/compliance/references/` | Active framework requirements |

## Workflow

### Step 1 — Identify Active Frameworks

Read `skills/compliance/references/` to identify active frameworks (PCI-DSS, LGPD, GDPR, HIPAA, SOX).

### Step 2 — Verify Framework-Specific Requirements

For each active framework, verify the change against framework-specific requirements.

### Step 3 — Check Sensitive Data Handling

Check data classification, masking, and encryption per `skills/security/references/cryptography.md`.

### Step 4 — Verify Audit Trail Requirements

Ensure audit trail requirements are met for the active frameworks.

### Step 5 — Check Access Control Patterns

Verify access control patterns comply with framework requirements.

### Step 6 — Produce Compliance Report

Generate the compliance review report with per-framework results.

## Output Format

```
## Compliance Review — [Change Description]

### Active Frameworks: [list]

### Per-Framework Results

#### [Framework Name]
- [x] Requirement met / [ ] Gap identified
- Finding: [description + remediation]

### Overall Verdict: COMPLIANT / NON-COMPLIANT / NEEDS REVIEW
```

## Error Handling

| Scenario | Action |
|----------|--------|
| No compliance frameworks configured | Report INFO: no frameworks active, skip review |
| Compliance KP files missing | Warn and proceed with generic security review |
| PR number invalid or inaccessible | Report error with PR number and suggest checking access |
