# pci-dss-requirements

> PCI-DSS v4.0 requirements mapped to Java code practices for automated code review. Contains 12 requirements with prohibited/correct examples and reviewer checklists.

| | |
|---|---|
| **Category** | Knowledge Pack |
| **Referenced by** | `x-review` (Security specialist), `x-owasp-scan`, `compliance`, `security-engineer` agent |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete reference.

## Topics Covered

- Requirement 1: Network security controls and TLS enforcement
- Requirement 2: Secure configurations (no default credentials)
- Requirement 3: Stored account data protection (PAN encryption/tokenization)
- Requirement 4: Strong cryptography during transmission (TLS 1.2+)
- Requirement 5: Malicious software protection (safe deserialization, file upload validation)
- Requirement 6: Secure SDLC (SQL injection, XSS prevention)
- Requirements 7-8: Access control (RBAC) and authentication (MFA, password hashing)
- Requirements 9-12: Physical security, audit logging, security testing, organizational policies

## Key Concepts

This pack maps all 12 PCI-DSS v4.0 requirements to concrete Java code patterns, providing prohibited and correct code examples for each mappable requirement. Each requirement includes a code reviewer checklist that can be used during automated or manual review. Requirements 9 and 12 are organizational and include explanatory notes about their non-code nature. The pack is essential for any project handling cardholder data or operating within a PCI-DSS compliance scope.

## See Also

- [security](../security/) — OWASP Top 10, cryptography, and secrets management
- [owasp-asvs](../owasp-asvs/) — ASVS verification standard cross-referenced to PCI-DSS
- [compliance](../compliance/) — GDPR, HIPAA, LGPD, and SOX compliance frameworks
