---
name: x-review-compliance
description: "PCI-DSS compliance review with 25-point checklist for code changes involving payment card data. Produces per-point PASS/FAIL report with remediation."
user-invocable: true
allowed-tools: Read, Grep, Glob, Bash
argument-hint: "[PR number or file paths]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: PCI-DSS Compliance Review

## Purpose

Review code changes against PCI-DSS v4.0 requirements. Produce a per-point PASS/FAIL compliance report with remediation guidance for each finding.

## Activation Condition

Include this skill when the project handles payment card data and compliance frameworks include PCI-DSS.

## Triggers

- `/x-review-compliance 42` -- review PR #42 for PCI-DSS compliance
- `/x-review-compliance src/main/java/com/example/payment/` -- review specific file paths

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `target` | String | No | (current changes) | PR number or file paths to review |

## Knowledge Pack References

| Pack | Files | Purpose |
|------|-------|---------|
| security | `skills/security/references/security-principles.md` | Data classification, input validation, fail-secure patterns |
| security | `skills/security/references/cryptography.md` | TLS, hashing, key management |
| compliance | `skills/compliance/SKILL.md` and `skills/compliance/references/` | PCI-DSS requirements |

## Workflow

### Step 1 — Read Compliance Requirements

Read `skills/compliance/references/pci-dss.md` for full PCI-DSS requirement mapping.

### Step 2 — Identify Cardholder Data Files

Identify all files handling cardholder data (PAN, CVV, expiry, track data).

### Step 3 — Evaluate Checklist

Evaluate each of the 25 checklist points against the changed code. For each point: mark PASS or FAIL with file location and evidence.

### Step 4 — Produce Compliance Report

Generate report with remediation for each FAIL.

## PCI-DSS Compliance Checklist

### Category: Data Protection (1-5)

1. PAN (Primary Account Number) MUST NOT appear in logs, traces, or error messages. Verify no `log.*` or `System.out` call includes raw PAN values.
2. PAN MUST be masked in all user-facing output using format `****NNNN` (last 4 digits only).
3. CVV/CVC MUST NOT be stored after authorization, even if encrypted. Verify no persistence of security codes.
4. Card expiration date MUST NOT appear in logs or unencrypted storage alongside PAN.
5. Sensitive authentication data (full track, CAV2/CVC2/CVV2, PIN) MUST NOT be stored post-authorization.

### Category: Cryptography (6-9)

6. Encryption at rest for stored cardholder data MUST use AES-256 or stronger algorithm.
7. Cryptographic keys MUST be loaded from environment variables, KMS, or HSM -- never hardcoded.
8. Key rotation mechanism MUST exist with documented rotation schedule.
9. Hashing of PAN for indexing MUST use keyed cryptographic hash (HMAC-SHA256+), not plain SHA.

### Category: Transmission Security (10-12)

10. All transmission of cardholder data MUST use TLS 1.2 or higher. Verify no fallback to older protocols.
11. Card data MUST NOT appear in URL query strings or path parameters.
12. API responses containing card data MUST include appropriate security headers (no-cache, no-store).

### Category: Authentication (13-16)

13. Access to cardholder data endpoints MUST require authentication. No anonymous access to payment data.
14. Multi-factor authentication MUST be required for administrative access to cardholder data environments.
15. Session tokens MUST have configured expiration (max 15 minutes idle, 8 hours absolute).
16. Failed authentication attempts MUST trigger account lockout after configurable threshold.

### Category: Access Control (17-19)

17. Access to cardholder data MUST be restricted by role-based access control (RBAC).
18. Principle of least privilege: services MUST only have permissions required for their function.
19. Access to cryptographic keys MUST be restricted to minimum number of custodians.

### Category: Logging and Monitoring (20-23)

20. All access to cardholder data MUST generate an audit log entry with timestamp, user, action, and resource.
21. Audit logs MUST NOT contain cardholder data (PAN, CVV, expiry). Log only masked/tokenized references.
22. Log integrity MUST be protected -- logs must be immutable or tamper-evident.
23. Security events (failed auth, access violations) MUST trigger alerts within defined SLA.

### Category: Secure Development (24-25)

24. No use of `Math.random()` or non-cryptographic RNG for generating tokens, session IDs, or keys.
25. Input validation MUST be applied to all cardholder data fields before processing.

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

## Error Handling

| Scenario | Action |
|----------|--------|
| No cardholder data files found | Report N/A for all points with explanation |
| Compliance KP not available | Warn and proceed with checklist-only review |
| PR number invalid or inaccessible | Report error with PR number and suggest checking access |
