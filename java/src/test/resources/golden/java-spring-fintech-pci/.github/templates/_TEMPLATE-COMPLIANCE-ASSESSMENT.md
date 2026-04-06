# Compliance Assessment -- {{STORY_ID}}

> **Epic:** {{EPIC_ID}}
> **Date:** {{GENERATION_DATE}}
> **Author:** Security Engineer (AI-assisted)
> **Template Version:** 1.0

## Data Classification Impact

| Data Element | Classification | Processing Purpose | Legal Basis | Cross-Border | Retention Period |
| :--- | :--- | :--- | :--- | :--- | :--- |
| _Example: User email_ | _Confidential / PII_ | _Account management_ | _Consent_ | _Yes (EU -> US)_ | _Account lifetime + 30 days_ |

### Classification Levels

| Level | Description | Handling Requirements |
| :--- | :--- | :--- |
| Public | Non-sensitive, freely shareable | No special handling |
| Internal | Organization-internal only | Access control required |
| Confidential | Sensitive business or personal data | Encryption + access logging |
| Restricted | Highest sensitivity (PII, PHI, PCI) | Encryption + access logging + approval + audit |

### Data Inventory

- Total data elements processed by this feature: {{DATA_ELEMENT_COUNT}}
- Elements containing PII: {{PII_ELEMENT_COUNT}}
- Elements requiring encryption: {{ENCRYPTED_ELEMENT_COUNT}}
- Elements with cross-border transfer: {{CROSS_BORDER_ELEMENT_COUNT}}

<!-- CONDITIONAL: compliance-frameworks configured -->
## Framework-Specific Assessment

### GDPR (General Data Protection Regulation)

| Requirement | Article | Applicable | Implementation | Status |
| :--- | :--- | :--- | :--- | :--- |
| Lawful basis for processing | Art. 6 | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |
| Consent management | Art. 7 | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |
| Right to access | Art. 15 | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |
| Right to rectification | Art. 16 | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |
| Right to erasure | Art. 17 | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |
| Data portability | Art. 20 | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |
| Privacy by design | Art. 25 | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |
| Data breach notification | Art. 33-34 | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |
| DPIA requirement | Art. 35 | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |

### LGPD (Lei Geral de Protecao de Dados)

| Requirement | Article | Applicable | Implementation | Status |
| :--- | :--- | :--- | :--- | :--- |
| Legal basis for processing | Art. 7 | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |
| Consent requirements | Art. 8 | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |
| Data subject rights | Art. 17-22 | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |
| International transfer | Art. 33-36 | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |
| Data Protection Officer | Art. 41 | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |
| Security measures | Art. 46 | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |
| Incident notification | Art. 48 | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |

### HIPAA (Health Insurance Portability and Accountability Act)

| Requirement | Rule | Applicable | Implementation | Status |
| :--- | :--- | :--- | :--- | :--- |
| PHI identification | Privacy Rule | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |
| Minimum necessary standard | Privacy Rule | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |
| Access controls | Security Rule | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |
| Audit controls | Security Rule | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |
| Transmission security | Security Rule | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |
| Integrity controls | Security Rule | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |
| Breach notification | Breach Rule | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |
| BAA requirements | Privacy Rule | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |

### PCI-DSS (Payment Card Industry Data Security Standard)

| Requirement | Section | Applicable | Implementation | Status |
| :--- | :--- | :--- | :--- | :--- |
| Network segmentation | Req. 1 | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |
| Cardholder data protection | Req. 3 | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |
| Encryption in transit | Req. 4 | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |
| Access control | Req. 7 | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |
| Authentication | Req. 8 | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |
| Monitoring and logging | Req. 10 | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |
| Vulnerability management | Req. 11 | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |
| Security policy | Req. 12 | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |

### SOX (Sarbanes-Oxley Act)

| Requirement | Section | Applicable | Implementation | Status |
| :--- | :--- | :--- | :--- | :--- |
| Internal controls | Section 302 | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |
| Financial reporting integrity | Section 404 | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |
| Access controls for financial data | Section 404 | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |
| Audit trail for financial transactions | Section 802 | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |
| Change management controls | Section 404 | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |
| Segregation of duties | Section 404 | {{APPLICABLE}} | {{IMPLEMENTATION}} | {{STATUS}} |

> **Instructions:** Include only the framework sub-sections relevant to the project's configured compliance frameworks. Remove sub-sections for frameworks that do not apply. For each applicable requirement, assess Implementation status and set Status to `Compliant` / `Partial` / `Non-Compliant` / `N/A`.
<!-- END CONDITIONAL -->

## Personal Data Processing

| Processing Activity | Data Categories | Data Subjects | Legal Basis | Automated Decision | Profiling |
| :--- | :--- | :--- | :--- | :--- | :--- |
| _Example: User registration_ | _Name, email, password_ | _End users_ | _Consent_ | _No_ | _No_ |

### Data Minimization Checklist

- [ ] Only necessary data fields are collected
- [ ] Data retention periods are defined and enforced
- [ ] Data is anonymized/pseudonymized where possible
- [ ] No excessive data collection beyond stated purpose
- [ ] Data deletion procedures are implemented and tested

### Consent Management

- Consent collection mechanism: {{CONSENT_MECHANISM}}
- Consent withdrawal mechanism: {{CONSENT_WITHDRAWAL_MECHANISM}}
- Consent records storage: {{CONSENT_RECORDS_STORAGE}}
- Granular consent options: {{GRANULAR_CONSENT}}

## Audit Trail Requirements

| Event | Required Fields | Integrity Check | Retention | Tamper Protection |
| :--- | :--- | :--- | :--- | :--- |
| Data creation | _who, what, when, source_ | _Hash chain_ | _Per regulation_ | _Append-only log_ |
| Data modification | _who, what, when, before, after_ | _Hash chain_ | _Per regulation_ | _Append-only log_ |
| Data deletion | _who, what, when, reason, authorization_ | _Hash chain_ | _Per regulation_ | _Append-only log_ |
| Data access | _who, what, when, purpose_ | _Hash chain_ | _Per regulation_ | _Append-only log_ |
| Consent change | _who, what, when, old_consent, new_consent_ | _Hash chain_ | _Per regulation_ | _Append-only log_ |

### Audit Infrastructure

- Audit log format: Structured JSON ({{LANGUAGE}} logging framework)
- Audit log destination: {{AUDIT_LOG_DESTINATION}}
- Audit log search capability: {{AUDIT_LOG_SEARCH}}
- Audit log backup: {{AUDIT_LOG_BACKUP}}

## Cross-Border Considerations

| Transfer | Source Region | Destination Region | Mechanism | Adequacy Decision | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| _Example: User data_ | _EU_ | _US_ | _SCCs (Standard Contractual Clauses)_ | _No (Schrems II)_ | _Under Review_ |

### Transfer Impact Assessment

- Data localization requirements: {{DATA_LOCALIZATION}}
- Transfer mechanisms available: SCCs, BCRs, Adequacy Decision, Consent
- Supplementary measures needed: {{SUPPLEMENTARY_MEASURES}}
- Government access risk assessment: {{GOVERNMENT_ACCESS_RISK}}

## Remediation Actions

| ID | Finding | Severity | Framework | Remediation | Owner | Deadline | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| REM-001 | {{FINDING}} | {{SEVERITY}} | {{FRAMEWORK}} | {{REMEDIATION}} | {{OWNER}} | {{DEADLINE}} | {{STATUS}} |
| REM-002 | {{FINDING}} | {{SEVERITY}} | {{FRAMEWORK}} | {{REMEDIATION}} | {{OWNER}} | {{DEADLINE}} | {{STATUS}} |

> **Severity:** `Critical` / `High` / `Medium` / `Low`
> **Status:** `Open` / `In Progress` / `Resolved` / `Accepted` / `Deferred`
> **Instructions:** Add one row per compliance finding. Track remediation progress. Critical and High findings MUST have a deadline within 30 days.

## Sign-off

| Role | Name | Decision | Date | Notes |
| :--- | :--- | :--- | :--- | :--- |
| Security Engineer | {{SECURITY_ENGINEER}} | {{DECISION}} | {{DATE}} | {{NOTES}} |
| Compliance Officer | {{COMPLIANCE_OFFICER}} | {{DECISION}} | {{DATE}} | {{NOTES}} |
| Tech Lead | {{TECH_LEAD}} | {{DECISION}} | {{DATE}} | {{NOTES}} |

> **Decision:** `Approved` / `Approved with Conditions` / `Rejected` / `Pending`
> **Instructions:** All sign-offs are required before the feature proceeds to production. Conditional approvals MUST list the conditions in Notes.
