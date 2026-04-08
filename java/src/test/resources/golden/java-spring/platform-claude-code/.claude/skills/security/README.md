# security

> Complete security reference: OWASP Top 10, security headers, secrets management, input validation, cryptography (TLS, hashing, key management), and pentest readiness checklist.

| | |
|---|---|
| **Category** | Knowledge Pack |
| **Referenced by** | `x-review` (Security specialist), `x-owasp-scan`, `x-hardening-eval`, `x-supply-chain-audit`, `security-engineer` agent |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete reference.

## Topics Covered

- Security principles (data classification, input validation, secure error handling)
- Application security (OWASP Top 10, CSP, security headers, secrets management)
- Cryptography (TLS requirements, cipher suites, hashing algorithms, key management)
- Pentest readiness (hardening checklist, common vulnerabilities, remediation)
- Supply chain security (SBOM generation, artifact signing, SLSA framework, dependency pinning)
- Software composition analysis (SCA tools per language, license compliance)
- Transitive dependency risk (depth risk, typosquatting, maintainer risk)

## Key Concepts

This pack provides the comprehensive security reference covering application security, cryptography, and supply chain hardening. It includes SBOM generation guidance for both CycloneDX and SPDX formats with tools mapped per language, artifact signing via Sigstore/cosign with keyless OIDC identity, and SLSA framework compliance targeting Level 2 minimum. The software composition analysis section defines SCA tool recommendations per language, license compliance enforcement with SPDX identifiers categorized by risk level, and transitive dependency risk mitigation strategies.

## See Also

- [owasp-asvs](../owasp-asvs/) — ASVS 4.0.3 verification standard with L1/L2/L3 levels
- [pci-dss-requirements](../pci-dss-requirements/) — PCI-DSS v4.0 requirements mapped to code practices
- [compliance](../compliance/) — GDPR, HIPAA, LGPD, and SOX compliance frameworks
- [resilience](../resilience/) — Rate limiting and timeout patterns for security hardening
