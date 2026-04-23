# OWASP ASVS Cross-Reference Tables

## OWASP Top 10 2021 to ASVS Detailed Mapping

### A01: Broken Access Control to V4

- V4.1.1: Verify the application enforces access control rules on a trusted service layer (L1, CWE-285)
- V4.1.2: Verify that all user and data attributes used by access controls cannot be manipulated by end users (L1, CWE-639)
- V4.1.3: Verify that the principle of least privilege exists (L1, CWE-285)
- V4.2.1: Verify that sensitive data and APIs are protected against Insecure Direct Object Reference (IDOR) attacks (L1, CWE-639)

### A02: Cryptographic Failures to V6, V9

- V6.1.1: Verify that regulated private data is stored encrypted while at rest (L2, CWE-311)
- V6.2.1: Verify that all cryptographic modules fail securely (L1, CWE-310)
- V9.1.1: Verify that TLS is used for all client connectivity (L1, CWE-319)
- V9.1.2: Verify using up-to-date TLS testing tools that only strong cipher suites are enabled (L1, CWE-326)

### A03: Injection to V5

- V5.1.1: Verify that the application has defenses against HTTP parameter pollution attacks (L1, CWE-235)
- V5.2.1: Verify that all untrusted HTML input from WYSIWYG editors or similar is properly sanitized (L1, CWE-116)
- V5.3.1: Verify that output encoding is relevant for the interpreter and context required (L1, CWE-116)
- V5.3.4: Verify that data selection or database queries use parameterized queries (L1, CWE-89)

### A04: Insecure Design to V1

- V1.1.1: Verify the use of a secure software development lifecycle (L2, CWE-1053)
- V1.1.2: Verify the use of threat modeling for every design change (L2, CWE-1053)
- V1.1.3: Verify that all user stories and features contain functional security constraints (L2, CWE-1110)

### A05: Security Misconfiguration to V14

- V14.1.1: Verify that the application build and deployment processes are performed in a secure fashion (L2, CWE-1104)
- V14.2.1: Verify that all components are up to date (L1, CWE-1104)
- V14.3.1: Verify that web or application server and application framework debug modes are disabled in production (L1, CWE-497)

### A07: Authentication Failures to V2, V3

- V2.1.1: Verify that user set passwords are at least 12 characters in length (L1, CWE-521)
- V2.2.1: Verify that anti-automation controls are effective at mitigating breached credential testing (L1, CWE-307)
- V3.1.1: Verify the application never reveals session tokens in URL parameters (L1, CWE-598)

### A08: Software and Data Integrity Failures to V10

- V10.1.1: Verify that a code analysis tool is in use (L3, CWE-1104)
- V10.2.1: Verify that the application source code and third-party libraries do not contain unauthorized functionality (L2, CWE-829)

### A09: Logging Failures to V7

- V7.1.1: Verify that the application does not log credentials or payment details (L1, CWE-532)
- V7.1.2: Verify that the application does not log other sensitive data as defined under local privacy laws (L1, CWE-532)
- V7.2.1: Verify that all authentication decisions are logged (L2, CWE-778)

### A10: SSRF to V5, V13

- V5.2.6: Verify that the application protects against SSRF attacks by validating untrusted data (L1, CWE-918)
- V13.1.1: Verify that all application components use the same encodings and parsers (L1, CWE-116)

## CIS Controls v8 to ASVS Detailed Mapping

| CIS Control | Description | Primary ASVS Chapters | Key Verification Items |
|-------------|-------------|----------------------|----------------------|
| CIS-01 | Inventory and Control of Enterprise Assets | V14 | V14.2.1, V14.2.2 |
| CIS-02 | Inventory and Control of Software Assets | V14 | V14.2.1, V14.2.3 |
| CIS-03 | Data Protection | V6, V8 | V6.1.1, V8.1.1, V8.3.1 |
| CIS-04 | Secure Configuration | V14 | V14.1.1, V14.3.1 |
| CIS-05 | Account Management | V2 | V2.1.1, V2.5.1 |
| CIS-06 | Access Control Management | V4 | V4.1.1, V4.1.3, V4.2.1 |
| CIS-07 | Continuous Vulnerability Management | V14 | V14.2.1, V14.2.2 |
| CIS-08 | Audit Log Management | V7 | V7.1.1, V7.2.1, V7.3.1 |
| CIS-09 | Email and Web Browser Protections | V5, V13 | V5.1.1, V13.1.1 |
| CIS-10 | Malware Defenses | V10 | V10.1.1, V10.2.1 |
| CIS-11 | Data Recovery | V8 | V8.1.1 |
| CIS-12 | Network Infrastructure Management | V9 | V9.1.1, V9.1.2 |
| CIS-13 | Network Monitoring and Defense | V7, V9 | V7.2.1, V9.1.1 |
| CIS-14 | Security Awareness | V1 | V1.1.1, V1.1.2 |
| CIS-15 | Service Provider Management | V13 | V13.1.1, V13.2.1 |
| CIS-16 | Application Software Security | V1, V5, V14 | V1.1.1, V5.1.1, V14.1.1 |
| CIS-17 | Incident Response Management | V7 | V7.2.1, V7.3.1 |
| CIS-18 | Penetration Testing | V1 | V1.1.2 |

## NIST CSF to ASVS Detailed Mapping

### Identify (ID)

| NIST CSF Subcategory | ASVS Mapping | Verification Items |
|---------------------|--------------|-------------------|
| ID.AM: Asset Management | V14 | V14.2.1, V14.2.2, V14.2.3 |
| ID.BE: Business Environment | V1 | V1.1.1, V1.1.3 |
| ID.GV: Governance | V1 | V1.1.1, V1.1.2 |
| ID.RA: Risk Assessment | V1 | V1.1.2, V1.1.3 |
| ID.SC: Supply Chain Risk Management | V10, V14 | V10.2.1, V14.2.1 |

### Protect (PR)

| NIST CSF Subcategory | ASVS Mapping | Verification Items |
|---------------------|--------------|-------------------|
| PR.AC: Identity Management and Access Control | V2, V3, V4 | V2.1.1, V3.1.1, V4.1.1 |
| PR.AT: Awareness and Training | V1 | V1.1.1 |
| PR.DS: Data Security | V6, V8, V9 | V6.1.1, V8.1.1, V9.1.1 |
| PR.IP: Information Protection Processes | V5, V14 | V5.1.1, V14.1.1 |
| PR.MA: Maintenance | V14 | V14.2.1 |
| PR.PT: Protective Technology | V5, V9, V14 | V5.3.1, V9.1.1, V14.3.1 |

### Detect (DE)

| NIST CSF Subcategory | ASVS Mapping | Verification Items |
|---------------------|--------------|-------------------|
| DE.AE: Anomalies and Events | V7 | V7.2.1 |
| DE.CM: Security Continuous Monitoring | V7, V10 | V7.2.1, V10.1.1 |
| DE.DP: Detection Processes | V7 | V7.3.1 |

### Respond (RS)

| NIST CSF Subcategory | ASVS Mapping | Verification Items |
|---------------------|--------------|-------------------|
| RS.AN: Analysis | V7 | V7.2.1, V7.3.1 |
| RS.CO: Communications | V7, V11 | V7.1.1 |
| RS.MI: Mitigation | V11 | V11.1.1 |
| RS.RP: Response Planning | V7 | V7.3.1 |

### Recover (RC)

| NIST CSF Subcategory | ASVS Mapping | Verification Items |
|---------------------|--------------|-------------------|
| RC.CO: Communications | V7 | V7.1.1 |
| RC.IM: Improvements | V8 | V8.1.1 |
| RC.RP: Recovery Planning | V7, V8 | V7.3.1, V8.1.1 |
