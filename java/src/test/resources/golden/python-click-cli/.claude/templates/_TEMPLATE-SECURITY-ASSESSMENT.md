# Security Assessment -- {{STORY_ID}}

> **Epic:** {{EPIC_ID}}
> **Date:** {{GENERATION_DATE}}
> **Author:** Security Engineer (AI-assisted)
> **Template Version:** 1.0

## Data Classification

| Data Element | Classification | Storage | Transmission | Retention |
| :--- | :--- | :--- | :--- | :--- |
| _Example: User credentials_ | _Confidential_ | _Encrypted at rest (AES-256)_ | _TLS 1.3_ | _Until account deletion_ |

### Classification Levels

- **Public**: No restrictions on access or disclosure
- **Internal**: Restricted to organization members
- **Confidential**: Restricted to authorized personnel, requires encryption
- **Restricted**: Highest sensitivity, requires encryption + access logging + approval

## Encryption Requirements

| Scope | Algorithm | Key Size | Key Management | Notes |
| :--- | :--- | :--- | :--- | :--- |
| Data at rest | _AES-256-GCM_ | _256-bit_ | _{{FRAMEWORK}} secrets / vault_ | _Database, file storage_ |
| Data in transit | _TLS 1.3_ | _N/A_ | _Certificate authority_ | _All external communication_ |
| Sensitive fields | _AES-256-GCM_ | _256-bit_ | _Application-level key rotation_ | _PII, credentials, tokens_ |

### Key Rotation Policy

- Rotation frequency: {{KEY_ROTATION_FREQUENCY}}
- Rotation mechanism: {{KEY_ROTATION_MECHANISM}}
- Key compromise procedure: {{KEY_COMPROMISE_PROCEDURE}}

## Authentication & Authorization

### Authentication

| Aspect | Requirement | Implementation | Status |
| :--- | :--- | :--- | :--- |
| Authentication method | _JWT / OAuth2 / Session_ | _{{FRAMEWORK}} security module_ | _Open_ |
| Credential storage | _Bcrypt/Argon2 hashing_ | _{{LANGUAGE}} crypto library_ | _Open_ |
| Session management | _Secure, HttpOnly cookies_ | _{{FRAMEWORK}} session config_ | _Open_ |
| MFA support | _TOTP / WebAuthn_ | _Third-party provider_ | _Open_ |

### Authorization

| Resource | Required Role | Enforcement Point | Granularity |
| :--- | :--- | :--- | :--- |
| _Example: /api/admin/*_ | _ADMIN_ | _{{FRAMEWORK}} security filter_ | _Endpoint-level_ |

## Input Validation

| Input Source | Validation Strategy | Sanitization | Encoding |
| :--- | :--- | :--- | :--- |
| HTTP request body | _Schema validation (JSON Schema / Bean Validation)_ | _Strip HTML tags_ | _UTF-8_ |
| HTTP query params | _Whitelist + type coercion_ | _URL decode + validate_ | _UTF-8_ |
| HTTP headers | _Whitelist known headers_ | _Reject unexpected_ | _ASCII_ |
| Path parameters | _Regex pattern match_ | _Reject traversal sequences_ | _UTF-8_ |
| File uploads | _Extension whitelist + magic bytes_ | _Rename + isolate_ | _Binary_ |

### Validation Rules

- All input MUST be validated before processing (fail-fast)
- Reject unknown fields (strict deserialization mode)
- Maximum request body size: {{MAX_REQUEST_BODY_SIZE}}
- Maximum URL length: {{MAX_URL_LENGTH}}

## Audit Logging Requirements

| Event Category | Log Level | Fields Required | Retention |
| :--- | :--- | :--- | :--- |
| Authentication success/failure | INFO/WARN | _user_id, ip, timestamp, result_ | _90 days_ |
| Authorization denial | WARN | _user_id, resource, action, reason_ | _90 days_ |
| Data access (sensitive) | INFO | _user_id, resource, action, data_classification_ | _1 year_ |
| Configuration change | INFO | _user_id, setting, old_value, new_value_ | _1 year_ |
| Security event | ERROR | _event_type, severity, details, correlation_id_ | _1 year_ |

### Audit Log Integrity

- Logs MUST be append-only (no modification or deletion)
- Logs MUST include tamper-detection mechanism (hash chain or HMAC)
- PII in logs MUST be masked or tokenized

## OWASP Top 10 Assessment

| Category | Applicable | Risk Level | Mitigation | Status |
| :--- | :--- | :--- | :--- | :--- |
| A01:2021 - Broken Access Control | {{APPLICABLE}} | {{RISK_LEVEL}} | {{MITIGATION}} | {{STATUS}} |
| A02:2021 - Cryptographic Failures | {{APPLICABLE}} | {{RISK_LEVEL}} | {{MITIGATION}} | {{STATUS}} |
| A03:2021 - Injection | {{APPLICABLE}} | {{RISK_LEVEL}} | {{MITIGATION}} | {{STATUS}} |
| A04:2021 - Insecure Design | {{APPLICABLE}} | {{RISK_LEVEL}} | {{MITIGATION}} | {{STATUS}} |
| A05:2021 - Security Misconfiguration | {{APPLICABLE}} | {{RISK_LEVEL}} | {{MITIGATION}} | {{STATUS}} |
| A06:2021 - Vulnerable and Outdated Components | {{APPLICABLE}} | {{RISK_LEVEL}} | {{MITIGATION}} | {{STATUS}} |
| A07:2021 - Identification and Authentication Failures | {{APPLICABLE}} | {{RISK_LEVEL}} | {{MITIGATION}} | {{STATUS}} |
| A08:2021 - Software and Data Integrity Failures | {{APPLICABLE}} | {{RISK_LEVEL}} | {{MITIGATION}} | {{STATUS}} |
| A09:2021 - Security Logging and Monitoring Failures | {{APPLICABLE}} | {{RISK_LEVEL}} | {{MITIGATION}} | {{STATUS}} |
| A10:2021 - Server-Side Request Forgery (SSRF) | {{APPLICABLE}} | {{RISK_LEVEL}} | {{MITIGATION}} | {{STATUS}} |

> **Instructions:** For each OWASP category, set Applicable to `Yes` or `No`. If applicable, assess Risk Level (`Critical` / `High` / `Medium` / `Low`), describe Mitigation applied or planned, and set Status (`Open` / `Mitigated` / `Accepted`). If not applicable, set Risk Level to `N/A` and Status to `N/A`.

## Dependency Security

| Dependency | Version | Known CVEs | Severity | Mitigation | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| _Example: {{FRAMEWORK}}_ | _{{FRAMEWORK_VERSION}}_ | _None_ | _N/A_ | _Keep updated_ | _Mitigated_ |

### Dependency Management Policy

- All dependencies MUST be pinned to specific versions
- Dependency audit MUST run in CI pipeline (e.g., `{{BUILD_TOOL}} audit`)
- Critical CVEs MUST be patched within 48 hours
- High CVEs MUST be patched within 7 days
- Transitive dependencies MUST be scanned

## Regulatory Considerations

| Regulation | Applicable | Requirements | Implementation Status |
| :--- | :--- | :--- | :--- |
| GDPR | {{APPLICABLE}} | _Data minimization, consent, right to erasure_ | {{STATUS}} |
| LGPD | {{APPLICABLE}} | _Similar to GDPR, Brazil-specific requirements_ | {{STATUS}} |
| HIPAA | {{APPLICABLE}} | _PHI protection, access controls, audit trails_ | {{STATUS}} |
| PCI-DSS | {{APPLICABLE}} | _Cardholder data protection, network segmentation_ | {{STATUS}} |
| SOX | {{APPLICABLE}} | _Financial data integrity, access controls_ | {{STATUS}} |

> **Instructions:** Mark applicable regulations based on the data the feature processes. Defer detailed compliance assessment to `COMPLIANCE-ASSESSMENT.md`.

## Risk Matrix

| Risk ID | Description | Likelihood | Impact | Risk Level | Owner |
| :--- | :--- | :--- | :--- | :--- | :--- |
| RISK-001 | {{RISK_DESCRIPTION}} | {{LIKELIHOOD}} | {{IMPACT}} | {{RISK_LEVEL}} | {{OWNER}} |
| RISK-002 | {{RISK_DESCRIPTION}} | {{LIKELIHOOD}} | {{IMPACT}} | {{RISK_LEVEL}} | {{OWNER}} |
| RISK-003 | {{RISK_DESCRIPTION}} | {{LIKELIHOOD}} | {{IMPACT}} | {{RISK_LEVEL}} | {{OWNER}} |

> **Likelihood scale:** `Rare` / `Unlikely` / `Possible` / `Likely` / `Almost Certain`
> **Impact scale:** `Negligible` / `Minor` / `Moderate` / `Major` / `Catastrophic`
> **Risk Level:** Calculated from Likelihood x Impact matrix:

| | Negligible | Minor | Moderate | Major | Catastrophic |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Almost Certain** | Medium | High | High | Critical | Critical |
| **Likely** | Medium | Medium | High | High | Critical |
| **Possible** | Low | Medium | Medium | High | High |
| **Unlikely** | Low | Low | Medium | Medium | High |
| **Rare** | Low | Low | Low | Medium | Medium |

> **Instructions:** Identify all security risks for this feature. Assign Likelihood and Impact based on the scales above. Calculate Risk Level using the matrix. Assign an Owner responsible for mitigation. Add or remove rows as needed.
