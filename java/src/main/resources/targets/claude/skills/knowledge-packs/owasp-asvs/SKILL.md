---
name: owasp-asvs
description: "OWASP ASVS 4.0.3 verification standard with L1/L2/L3 levels, cross-reference tables (OWASP Top 10, CIS Controls, NIST CSF, SANS Top 25), and verification items for all 14 chapters (V1-V14)."
version: 1.0
user-invocable: false
---

# Knowledge Pack: OWASP ASVS 4.0.3

## Purpose

Provides the OWASP Application Security Verification Standard (ASVS) 4.0.3 as a structured reference for security verification. Maps verification items across 14 chapters with three assurance levels, cross-referenced to OWASP Top 10, CIS Controls v8, NIST CSF, and SANS Top 25.

## ASVS Levels Overview

| Level | Name | Applicability | Description |
|-------|------|---------------|-------------|
| L1 | Opportunistic | All applications | Minimum baseline covering the most critical CWEs. Achievable via automated tooling and code review. |
| L2 | Standard | Most applications | Covers the majority of CWEs. Recommended for applications handling sensitive data, business-critical transactions, or healthcare/financial information. |
| L3 | Advanced | Critical applications | Defense-in-depth for high-value targets: military, financial infrastructure, healthcare systems. Requires architecture review and threat modeling. |

**Level hierarchy:** L1 is a subset of L2, and L2 is a subset of L3. Every L3-compliant application also satisfies L2 and L1.

## Detailed References

| Reference | Content |
|-----------|---------|
| `references/chapters-v1-v7.md` | Verification items for V1 through V7 with ID, description, level, and CWE |
| `references/chapters-v8-v14.md` | Verification items for V8 through V14 with ID, description, level, and CWE |
| `references/cross-references.md` | Cross-reference tables: OWASP Top 10, CIS Controls, NIST CSF, SANS Top 25 |

## Cross-Reference Tables

### OWASP Top 10 2021 to ASVS Mapping

| OWASP Top 10 | ID | ASVS Chapter(s) | Notes |
|--------------|-----|-----------------|-------|
| Broken Access Control | A01 | V4 | Access Control |
| Cryptographic Failures | A02 | V6, V9 | Stored Cryptography, Communication |
| Injection | A03 | V5 | Validation, Sanitization and Encoding |
| Insecure Design | A04 | V1 | Architecture, Design and Threat Modeling |
| Security Misconfiguration | A05 | V14 | Configuration |
| Vulnerable and Outdated Components | A06 | V14 | Delegated to dependency audit tooling |
| Identification and Authentication Failures | A07 | V2, V3 | Authentication, Session Management |
| Software and Data Integrity Failures | A08 | V10 | Malicious Code |
| Security Logging and Monitoring Failures | A09 | V7 | Error Handling and Logging |
| Server-Side Request Forgery | A10 | V5, V13 | Validation, API and Web Service |

### CIS Controls v8 to ASVS Mapping

| CIS Control | ID | ASVS Chapter(s) | Notes |
|-------------|-----|-----------------|-------|
| Inventory and Control of Enterprise Assets | CIS-01 | V14 | Configuration |
| Inventory and Control of Software Assets | CIS-02 | V14 | Configuration |
| Data Protection | CIS-03 | V6, V8 | Stored Cryptography, Data Protection |
| Secure Configuration of Enterprise Assets | CIS-04 | V14 | Configuration |
| Account Management | CIS-05 | V2 | Authentication |
| Access Control Management | CIS-06 | V4 | Access Control |
| Continuous Vulnerability Management | CIS-07 | V14 | Configuration and dependency management |
| Audit Log Management | CIS-08 | V7 | Error Handling and Logging |
| Email and Web Browser Protections | CIS-09 | V5, V13 | Validation, API and Web Service |
| Malware Defenses | CIS-10 | V10 | Malicious Code |
| Data Recovery | CIS-11 | V8 | Data Protection |
| Network Infrastructure Management | CIS-12 | V9 | Communication |
| Network Monitoring and Defense | CIS-13 | V7, V9 | Logging, Communication |
| Security Awareness and Skills Training | CIS-14 | V1 | Architecture and Design |
| Service Provider Management | CIS-15 | V13 | API and Web Service |
| Application Software Security | CIS-16 | V1, V5, V14 | Architecture, Validation, Configuration |
| Incident Response Management | CIS-17 | V7 | Error Handling and Logging |
| Penetration Testing | CIS-18 | V1 | Architecture, Design and Threat Modeling |

### NIST CSF to ASVS Mapping

| NIST CSF Function | ID | ASVS Chapter(s) | Notes |
|-------------------|-----|-----------------|-------|
| Identify | ID | V1 | Architecture, Design and Threat Modeling |
| Protect | PR | V2, V3, V4, V5, V6, V8, V9, V14 | Authentication, Session, Access Control, Validation, Crypto, Data Protection, Communication, Configuration |
| Detect | DE | V7, V10 | Error Handling and Logging, Malicious Code |
| Respond | RS | V7, V11 | Error Handling and Logging, Business Logic |
| Recover | RC | V7, V8 | Error Handling and Logging, Data Protection |

### SANS Top 25 to ASVS Mapping

| SANS Top 25 | CWE | ASVS Chapter(s) | Notes |
|-------------|-----|-----------------|-------|
| Out-of-bounds Write | CWE-787 | V5 | Validation, Sanitization and Encoding |
| Improper Neutralization of Input During Web Page Generation (XSS) | CWE-79 | V5 | Validation, Sanitization and Encoding |
| Improper Neutralization of Special Elements used in SQL Command (SQL Injection) | CWE-89 | V5 | Validation, Sanitization and Encoding |
| Improper Input Validation | CWE-20 | V5 | Validation, Sanitization and Encoding |
| Out-of-bounds Read | CWE-125 | V5 | Validation, Sanitization and Encoding |
| Improper Neutralization of Special Elements used in OS Command (OS Command Injection) | CWE-78 | V5 | Validation, Sanitization and Encoding |
| Use After Free | CWE-416 | V5 | Validation, Sanitization and Encoding |
| Improper Limitation of a Pathname to a Restricted Directory (Path Traversal) | CWE-22 | V5, V12 | Validation, Files and Resources |
| Cross-Site Request Forgery (CSRF) | CWE-352 | V4, V13 | Access Control, API and Web Service |
| Unrestricted Upload of File with Dangerous Type | CWE-434 | V12 | Files and Resources |
| Missing Authorization | CWE-862 | V4 | Access Control |
| NULL Pointer Dereference | CWE-476 | V5 | Validation, Sanitization and Encoding |
| Improper Authentication | CWE-287 | V2 | Authentication |
| Integer Overflow or Wraparound | CWE-190 | V5 | Validation, Sanitization and Encoding |
| Deserialization of Untrusted Data | CWE-502 | V5 | Validation, Sanitization and Encoding |
| Improper Neutralization of Special Elements used in a Command (Command Injection) | CWE-77 | V5 | Validation, Sanitization and Encoding |
| Improper Restriction of Operations within the Bounds of a Memory Buffer | CWE-119 | V5 | Validation, Sanitization and Encoding |
| Use of Hard-coded Credentials | CWE-798 | V2, V14 | Authentication, Configuration |
| Server-Side Request Forgery (SSRF) | CWE-918 | V5, V13 | Validation, API and Web Service |
| Missing Authentication for Critical Function | CWE-306 | V2, V4 | Authentication, Access Control |
| Concurrent Execution using Shared Resource with Improper Synchronization (Race Condition) | CWE-362 | V11 | Business Logic |
| Improper Privilege Management | CWE-269 | V4 | Access Control |
| Improper Control of Generation of Code (Code Injection) | CWE-94 | V5 | Validation, Sanitization and Encoding |
| Incorrect Authorization | CWE-863 | V4 | Access Control |
| Incorrect Default Permissions | CWE-276 | V4, V14 | Access Control, Configuration |

## ASVS Chapters Summary

| Chapter | Name | Key Focus |
|---------|------|-----------|
| V1 | Architecture, Design and Threat Modeling | Secure SDLC, threat modeling, architecture review |
| V2 | Authentication | Credential storage, password policies, MFA, lookup secrets |
| V3 | Session Management | Session tokens, cookie security, session timeout, re-authentication |
| V4 | Access Control | Least privilege, RBAC/ABAC, vertical/horizontal access control |
| V5 | Validation, Sanitization and Encoding | Input validation, output encoding, injection prevention |
| V6 | Stored Cryptography | Encryption at rest, key management, random values |
| V7 | Error Handling and Logging | Secure error handling, audit logging, log protection |
| V8 | Data Protection | Data classification, privacy, sensitive data handling |
| V9 | Communication | TLS configuration, certificate validation, secure transport |
| V10 | Malicious Code | Code integrity, anti-tampering, supply chain security |
| V11 | Business Logic | Workflow security, anti-automation, transaction integrity |
| V12 | Files and Resources | File upload validation, file storage, path traversal prevention |
| V13 | API and Web Service | REST/SOAP/GraphQL security, input validation, rate limiting |
| V14 | Configuration | Build pipeline security, dependency management, HTTP security headers |
