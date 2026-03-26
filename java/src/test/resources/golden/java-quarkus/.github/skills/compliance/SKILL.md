---
name: compliance
description: >
  Knowledge Pack: Compliance -- GDPR, HIPAA, LGPD, PCI-DSS, SOX frameworks
  covering data classification, rights enforcement, processing records,
  international transfers, audit logging, and security measures for my-quarkus-service.
---

# Knowledge Pack: Compliance

## Summary

Compliance frameworks and data protection conventions for my-quarkus-service.

### Data Classification

| Level | Examples | Handling |
|-------|---------|---------|
| PROHIBITED | Raw credentials, private keys | Never store or log |
| RESTRICTED | SSN, health records, PAN | Encrypted, access-controlled, audited |
| PII | Email, name, phone, address | Consent-based, minimized, erasable |
| INTERNAL | Business metrics, logs | Standard access controls |
| PUBLIC | Product descriptions, docs | No restrictions |

### GDPR (General Data Protection Regulation)

- Lawful basis required for all personal data processing
- Data subject rights: access, rectification, erasure, portability
- Privacy by design and by default in all new features
- Data Protection Impact Assessment for high-risk processing

### LGPD / PCI-DSS

- **LGPD**: Similar to GDPR with Brazil-specific legal bases and data subject rights
- **PCI-DSS**: Never store full PAN, tokenize immediately, encrypt in transit/at rest
- **Audit Logging**: Immutable trails for all sensitive data access, tamper-evident storage

## References

- [GDPR Official Text](https://gdpr-info.eu/) -- General Data Protection Regulation full text and guidance
- [PCI DSS v4.0](https://www.pcisecuritystandards.org/) -- Payment Card Industry Data Security Standard
- [LGPD — Brazilian Data Protection Law](https://www.gov.br/cidadania/pt-br/acesso-a-informacao/lgpd) -- Lei Geral de Protecao de Dados
- [HIPAA Security Rule](https://www.hhs.gov/hipaa/for-professionals/security/) -- Health information security requirements
