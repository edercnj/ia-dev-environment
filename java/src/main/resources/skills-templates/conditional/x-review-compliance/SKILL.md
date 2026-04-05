---
name: x-review-compliance
description: "PCI-DSS compliance review with 20+ point checklist for code changes involving payment card data"
argument-hint: "[PR number or file paths]"
user-invocable: true
---

# PCI-DSS Compliance Review

## Purpose
Reviews code changes against PCI-DSS v4.0 requirements. Produces a per-point PASS/FAIL compliance report with remediation guidance for each finding.

## Knowledge Pack References

Read these before starting the review:
- `skills/security/references/security-principles.md` — data classification, input validation, fail-secure patterns
- `skills/security/references/cryptography.md` — TLS, hashing, key management
- `skills/compliance/SKILL.md` — then read each file in `skills/compliance/references/` for PCI-DSS requirements

## PCI-DSS Compliance Checklist

### Category: Data Protection

1. PAN (Primary Account Number) MUST NOT appear in logs, traces, or error messages. Verify no `log.*` or `System.out` call includes raw PAN values.
   - **Violation example:** `log.info("Processing card: " + pan)`
   - **What to verify:** Search for PAN variables in log statements, exception messages, and toString() output

2. PAN MUST be masked in all user-facing output using format `****NNNN` (last 4 digits only).
   - **Violation example:** Displaying full 16-digit card number in API response
   - **What to verify:** All display/response paths mask PAN before rendering

3. CVV/CVC MUST NOT be stored after authorization, even if encrypted. Verify no persistence of security codes.
   - **Violation example:** Saving CVV to database column or cache entry
   - **What to verify:** No entity, DTO, or cache key stores CVV post-authorization

4. Card expiration date MUST NOT appear in logs or unencrypted storage alongside PAN.
   - **Violation example:** `log.debug("Card exp: " + expiry)`
   - **What to verify:** Expiration fields excluded from logging and toString()

5. Sensitive authentication data (full track, CAV2/CVC2/CVV2, PIN) MUST NOT be stored post-authorization.
   - **Violation example:** Persisting magnetic stripe data to audit table
   - **What to verify:** No storage mechanism retains authentication data after auth response

### Category: Cryptography

6. Encryption at rest for stored cardholder data MUST use AES-256 or stronger algorithm.
   - **Violation example:** Using DES or 3DES for card data encryption
   - **What to verify:** Encryption algorithms in crypto configuration are AES-256-GCM or equivalent

7. Cryptographic keys MUST be loaded from environment variables, KMS, or HSM — never hardcoded.
   - **Violation example:** `private static final String KEY = "abc123..."`
   - **What to verify:** No encryption key literals in source code; keys sourced from external config

8. Key rotation mechanism MUST exist with documented rotation schedule.
   - **Violation example:** Single static key with no rotation procedure
   - **What to verify:** Key versioning or rotation configuration present in deployment manifests

9. Hashing of PAN for indexing MUST use keyed cryptographic hash (HMAC-SHA256+), not plain SHA.
   - **Violation example:** `MessageDigest.getInstance("SHA-256").digest(pan.getBytes())`
   - **What to verify:** PAN hashing uses HMAC with a managed secret key

### Category: Transmission Security

10. All transmission of cardholder data MUST use TLS 1.2 or higher. Verify no fallback to older protocols.
    - **Violation example:** Allowing SSLv3 or TLS 1.0 in server configuration
    - **What to verify:** TLS configuration enforces minimum TLS 1.2; cipher suite excludes weak ciphers

11. Card data MUST NOT appear in URL query strings or path parameters.
    - **Violation example:** `GET /api/cards?pan=4111111111111111`
    - **What to verify:** No endpoint accepts PAN, CVV, or expiry as query or path parameter

12. API responses containing card data MUST include appropriate security headers (no-cache, no-store).
    - **Violation example:** Missing Cache-Control headers on card data endpoints
    - **What to verify:** Responses with cardholder data set Cache-Control: no-store, no-cache

### Category: Authentication

13. Access to cardholder data endpoints MUST require authentication. No anonymous access to payment data.
    - **Violation example:** Unsecured endpoint returning card details without auth token
    - **What to verify:** All card-data endpoints have authentication middleware/filter

14. Multi-factor authentication MUST be required for administrative access to cardholder data environments.
    - **Violation example:** Admin panel with password-only authentication
    - **What to verify:** Admin endpoints enforce MFA or reference MFA configuration

15. Session tokens MUST have configured expiration (max 15 minutes idle, 8 hours absolute).
    - **Violation example:** Infinite session timeout for payment processing sessions
    - **What to verify:** Session configuration enforces idle and absolute timeout values

16. Failed authentication attempts MUST trigger account lockout after configurable threshold.
    - **Violation example:** No rate limiting on login endpoint for card management portal
    - **What to verify:** Lockout or rate-limiting policy configured for auth endpoints

### Category: Access Control

17. Access to cardholder data MUST be restricted by role-based access control (RBAC).
    - **Violation example:** Any authenticated user can access full card details
    - **What to verify:** Endpoints check specific roles/permissions before returning card data

18. Principle of least privilege: services MUST only have permissions required for their function.
    - **Violation example:** Payment service has database admin privileges
    - **What to verify:** Service accounts and database roles follow minimal permission model

19. Access to cryptographic keys MUST be restricted to minimum number of custodians.
    - **Violation example:** Encryption key accessible to all application services
    - **What to verify:** Key access restricted via IAM, KMS policies, or equivalent

### Category: Logging and Monitoring

20. All access to cardholder data MUST generate an audit log entry with timestamp, user, action, and resource.
    - **Violation example:** Card data read without audit trail
    - **What to verify:** AuditLog or equivalent captures every CDE access with required fields

21. Audit logs MUST NOT contain cardholder data (PAN, CVV, expiry). Log only masked/tokenized references.
    - **Violation example:** Audit log includes `"cardNumber": "4111111111111111"`
    - **What to verify:** Audit log entries reference tokenized or masked identifiers only

22. Log integrity MUST be protected — logs must be immutable or tamper-evident.
    - **Violation example:** Audit logs stored in mutable database table with no integrity check
    - **What to verify:** Log storage uses append-only, signed, or tamper-evident mechanism

23. Security events (failed auth, access violations) MUST trigger alerts within defined SLA.
    - **Violation example:** Failed login attempts silently discarded without alerting
    - **What to verify:** Alert configuration exists for security-relevant log events

### Category: Secure Development

24. No use of `Math.random()` or non-cryptographic RNG for generating tokens, session IDs, or keys.
    - **Violation example:** `String token = String.valueOf(Math.random())`
    - **What to verify:** All security-sensitive random values use `SecureRandom` or equivalent CSPRNG

25. Input validation MUST be applied to all cardholder data fields before processing.
    - **Violation example:** Accepting arbitrary-length string for card number without Luhn check
    - **What to verify:** Card number, CVV, and expiry fields validated for format and length

## Workflow

1. Read `skills/compliance/references/pci-dss.md` for full PCI-DSS requirement mapping
2. Identify all files handling cardholder data (PAN, CVV, expiry, track data)
3. Evaluate each checklist point against the changed code
4. For each point: mark PASS or FAIL with file location and evidence
5. Produce compliance report with remediation for each FAIL

## Output Format
```
## PCI-DSS Compliance Review — [Change Description]

### Summary
- Points evaluated: 25
- PASS: [count]
- FAIL: [count]
- N/A: [count]

### Per-Category Results

#### Data Protection (1-5)
- [x] 1. PAN not in logs — PASS
- [ ] 2. PAN masked in output — FAIL: [file:line] — PAN returned unmasked in response DTO
  - **Remediation:** Apply PanMasker.mask() before setting response field

... (all 25 points)

### Overall Verdict: COMPLIANT / NON-COMPLIANT
### Risk Level: LOW / MEDIUM / HIGH / CRITICAL
```
