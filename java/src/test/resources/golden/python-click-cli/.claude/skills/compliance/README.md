# compliance

> Compliance frameworks (conditionally included): GDPR, HIPAA, LGPD, PCI-DSS, SOX. Data classification, rights enforcement, processing records, international transfers, security measures, audit logging, and framework-specific requirements.

| | |
|---|---|
| **Category** | Knowledge Pack |
| **Referenced by** | x-dev-lifecycle (Phase 1F), x-review (Security specialist), x-owasp-scan, x-security-dashboard, security-engineer agent |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete reference.

## Topics Covered

- GDPR: lawful basis, data subject rights, data minimization, processing records, international transfers, incident response
- HIPAA: PHI classification, access control, encryption (AES-256/TLS 1.3), audit logging, breach notification
- LGPD: data subject rights API, consent service, anonymization techniques, retention engine
- PCI-DSS: PAN tokenization, secure transmission, access control, vulnerability management
- SOX: financial data integrity, audit trail immutability, segregation of duties, compliance reporting
- OWASP Top 10 implementation and security headers
- Cryptography: TLS 1.3, AES-256-GCM, argon2id hashing, key management

## Key Concepts

This pack provides framework-specific compliance guidance conditionally included based on project regulatory requirements. Each framework (GDPR, HIPAA, LGPD, PCI-DSS, SOX) has dedicated reference material covering data classification, access controls, encryption requirements, audit logging, and incident response procedures. The pack integrates with the security knowledge pack for OWASP Top 10 implementation and cryptographic standards. Data handling rules enforce encryption at rest and in transit, PII masking in logs, and automated data subject access request capabilities.

## See Also

- [security](../security/) — OWASP Top 10, security headers, secrets management, cryptography
- [data-management](../data-management/) — Data governance, retention policies, PII handling
- [disaster-recovery](../disaster-recovery/) — Incident response and recovery procedures
