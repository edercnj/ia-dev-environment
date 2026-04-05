---
name: pci-dss-requirements
description: >
  Knowledge Pack: PCI-DSS v4.0 Requirements -- 12 PCI-DSS requirements mapped
  to Java code practices with prohibited/correct examples and code reviewer
  checklists for automated compliance review.
---

# Knowledge Pack: PCI-DSS v4.0 Requirements

## Summary

PCI-DSS v4.0 requirements mapped to concrete Java code practices for {project_name}. Each mappable requirement includes prohibited code patterns, correct implementations, and a code reviewer checklist. Requirements 9 and 12 are organizational and include explanatory notes.

## Requirements Overview

| Req | Title | Code-Mappable |
|-----|-------|--------------|
| 1 | Install and Maintain Network Security Controls | Yes |
| 2 | Apply Secure Configurations to All System Components | Yes |
| 3 | Protect Stored Account Data | Yes |
| 4 | Protect Cardholder Data with Strong Cryptography During Transmission | Yes |
| 5 | Protect All Systems and Networks from Malicious Software | Yes |
| 6 | Develop and Maintain Secure Systems and Software | Yes |
| 7 | Restrict Access by Business Need to Know | Yes |
| 8 | Identify Users and Authenticate Access | Yes |
| 9 | Restrict Physical Access to Cardholder Data | No (organizational) |
| 10 | Log and Monitor All Access | Yes |
| 11 | Test Security of Systems and Networks Regularly | Yes |
| 12 | Support Information Security with Organizational Policies | No (organizational) |

## Key Code Review Patterns

### Data Protection (Requirements 3-4)
- PAN must never be stored in cleartext or logged unmasked
- All cardholder data transmission requires TLS 1.2+
- Encryption keys managed via KMS, never hardcoded

### Access Control (Requirements 7-8)
- Every endpoint accessing cardholder data must have authorization checks
- RBAC follows least-privilege principle
- Passwords hashed with Argon2id, bcrypt, or scrypt
- MFA enforced for administrative and CDE access

### Audit and Monitoring (Requirement 10)
- All access to cardholder data generates audit log entries
- Audit logs never contain full PAN, CVV, or sensitive auth data
- Log entries include user ID, timestamp, action, and outcome

### Secure Development (Requirements 5-6)
- No unsafe deserialization of untrusted input
- All SQL queries use parameterized statements
- All user input validated and sanitized
- Security scanning (SAST/DAST) integrated in CI/CD

> For full requirement details with code examples, see the Claude knowledge pack `skills/pci-dss-requirements/SKILL.md`.
