# x-review-compliance

> PCI-DSS compliance review with 20+ point checklist for code changes involving payment card data.

| | |
|---|---|
| **Category** | Conditional |
| **Condition** | `compliance.pciDss = true` |
| **Invocation** | `/x-review-compliance [PR number or file paths]` |
| **Reads** | security (references: security-principles, cryptography), compliance (references: pci-dss) |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## When Available

This skill is generated when PCI-DSS compliance is enabled in the project configuration, indicating the application handles payment card data.

## What It Does

Reviews code changes against PCI-DSS v4.0 requirements using a 20+ point checklist covering data protection, cryptography, access control, network security, and audit logging. Validates that PAN is never logged or stored unmasked, CVV is never persisted post-authorization, encryption uses AES-256 or stronger, and cryptographic keys come from KMS/HSM. Produces a per-point PASS/FAIL compliance report with remediation guidance for each finding.

## Usage

```
/x-review-compliance
/x-review-compliance 42
/x-review-compliance src/main/java/com/example/payment/
```

## See Also

- [x-review-security](../x-review-security/) -- Multi-framework security compliance review
- [x-security-sast](../x-security-sast/) -- Static application security testing
- [x-security-secrets](../x-security-secrets/) -- Secret detection in code and git history
