# x-review-security

> Reviews code changes for compliance with selected security and compliance frameworks.

| | |
|---|---|
| **Category** | Conditional |
| **Condition** | `compliance` frameworks enabled (GDPR, HIPAA, LGPD, PCI-DSS, SOX) |
| **Invocation** | `/x-review-security [PR number or file paths]` |
| **Reads** | security (references: security-principles, application-security), compliance (references per active framework) |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## When Available

This skill is generated when one or more compliance frameworks are enabled in the project configuration.

## What It Does

Reviews code changes against the compliance frameworks selected in the project configuration (PCI-DSS, LGPD, GDPR, HIPAA, SOX). For each active framework, verifies the change against framework-specific requirements, checks sensitive data handling (classification, masking, encryption), verifies audit trail requirements, and validates access control patterns. Produces a per-framework compliance review report with COMPLIANT / NON-COMPLIANT / NEEDS REVIEW verdict.

## Usage

```
/x-review-security
/x-review-security 42
/x-review-security src/main/java/com/example/payment/
```

## See Also

- [x-review-compliance](../x-review-compliance/) -- PCI-DSS specific compliance review
- [x-security-sast](../x-security-sast/) -- Static application security testing
- [x-security-secrets](../x-security-secrets/) -- Secret detection in code and git history
