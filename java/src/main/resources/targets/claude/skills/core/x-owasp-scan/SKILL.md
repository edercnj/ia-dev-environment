---
name: x-owasp-scan
description: "Automated OWASP Top 10 (2021) verification mapped to ASVS levels (L1/L2/L3). Checks all 10 categories (A01-A10) with per-category pass/fail, ASVS coverage percentage, score grading, SARIF 2.1.0 output, and CI integration. Delegates A06 to x-dependency-audit."
user-invocable: true
allowed-tools: Read, Write, Bash, Grep, Glob, Agent
argument-hint: "[--level L1|L2|L3] [--category A01-A10|all] [--report-format markdown|sarif|both]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: OWASP Top 10 Verification

## Purpose

Verifies {{PROJECT_NAME}} against the OWASP Top 10 (2021) with verification items mapped to ASVS chapters and levels. Produces per-category pass/fail results, an overall score (0-100), ASVS coverage percentage, and generates both SARIF 2.1.0 and Markdown reports.

## Triggers

- `/x-owasp-scan` — full L1 scan, all categories
- `/x-owasp-scan --level L2` — L2 scan (standard defense)
- `/x-owasp-scan --level L3` — L3 scan (advanced/critical apps)
- `/x-owasp-scan --category A03` — single category scan
- `/x-owasp-scan --report-format sarif` — SARIF output only
- `/x-owasp-scan --report-format markdown` — Markdown only
- `/x-owasp-scan --report-format both` — both formats (default)

## Parameters

| Parameter | Type | Default | Values | Description |
|-----------|------|---------|--------|-------------|
| `--level` | String | L1 | L1, L2, L3 | ASVS verification depth |
| `--category` | String | all | A01-A10, all | OWASP category filter |
| `--report-format` | String | both | markdown, sarif, both | Output format |

## ASVS Level Definitions

| Level | Name | Target | Description |
|-------|------|--------|-------------|
| L1 | Opportunistic | Any application | Minimum verification; automated checks only |
| L2 | Standard | Most applications | Defensive depth; automated + manual review |
| L3 | Advanced | Critical systems (health, finance, infra) | Maximum assurance; comprehensive review |

## Workflow

```
1. PARSE      -> Parse CLI parameters (level, category, report-format)
2. LOAD       -> Load ASVS verification items from knowledge pack
3. MAP        -> Map OWASP Top 10 categories to ASVS chapters
4. VERIFY     -> Execute verification checks per category
5. DELEGATE   -> Delegate A06 to x-dependency-audit (RULE-011)
6. SCORE      -> Calculate per-category and overall scores
7. REPORT     -> Generate SARIF 2.1.0 + Markdown reports
```

### Step 1 — Parse Parameters

Validate CLI arguments:
- `--level` must be L1, L2, or L3 (default: L1)
- `--category` must be A01-A10 or "all" (default: all)
- `--report-format` must be markdown, sarif, or both (default: both)

### Step 2 — Load ASVS Verification Items

Read the OWASP ASVS knowledge pack to load verification items per chapter and level:

```
Read skills/security/SKILL.md
Read skills/security/references/application-security.md
```

Each verification item has:
- ASVS requirement ID (e.g., V4.1.1)
- Description
- Level (L1, L2, or L3)
- Verification method (automated, manual, or both)

### Step 3 — Map OWASP Top 10 to ASVS Chapters

| OWASP Category | ID | ASVS Chapter(s) | Focus Areas |
|----------------|-----|-----------------|-------------|
| Broken Access Control | A01 | V4 | RBAC enforcement, path traversal, CORS, IDOR |
| Cryptographic Failures | A02 | V6, V9 | Encryption at rest, TLS config, key management, cipher suites |
| Injection | A03 | V5 | Input validation, output encoding, parameterized queries, XSS |
| Insecure Design | A04 | V1 | Threat modeling, secure architecture patterns, trust boundaries |
| Security Misconfiguration | A05 | V14 | Default configs, error handling, hardening, unnecessary features |
| Vulnerable Components | A06 | N/A | **DELEGATED** to `x-dependency-audit` (RULE-011 — Skill Composability) |
| Auth Failures | A07 | V2, V3 | Authentication mechanisms, session management, credential storage |
| Software/Data Integrity | A08 | V10 | Code integrity, deserialization safety, CI/CD security |
| Logging Failures | A09 | V7 | Logging completeness, monitoring, alerting, log injection |
| SSRF | A10 | V5, V13 | URL validation, API security, server-side request handling |

### Step 4 — Execute Verification Checks

For each category (except A06), execute verification checks at the requested ASVS level.

#### 4.1 — A01 Broken Access Control (ASVS V4)

**L1 Checks:**
- Access control enforced at server side (not client only)
- Directory listing disabled
- Path traversal prevention (normalize + reject `..`)
- CORS configuration restricts allowed origins

**L2 Checks (includes L1):**
- RBAC/ABAC consistently applied across all endpoints
- Resource-level authorization (not just role-based)
- Access control metadata in audit logs
- Anti-CSRF tokens for state-changing operations

**L3 Checks (includes L1+L2):**
- Anti-automation controls on sensitive operations
- Fraud detection patterns for access anomalies
- Multi-factor authorization for critical operations
- Attribute-based access control with policy engine

#### 4.2 — A02 Cryptographic Failures (ASVS V6, V9)

**L1 Checks:**
- TLS 1.2+ enforced for all external communications
- No hardcoded secrets, keys, or credentials in source
- Sensitive data encrypted at rest
- No use of deprecated algorithms (MD5, SHA1, DES)

**L2 Checks (includes L1):**
- Key rotation policy defined and implemented
- Certificate pinning for critical endpoints
- Proper random number generation (CSPRNG)
- Encryption key management separate from application

**L3 Checks (includes L1+L2):**
- HSM or KMS for key storage
- Perfect forward secrecy (PFS) enabled
- Quantum-resistant algorithm readiness assessment
- Cryptographic agility (algorithm switchable without code change)

#### 4.3 — A03 Injection (ASVS V5)

**L1 Checks:**
- Input validation on all user-supplied data
- Parameterized queries (no string concatenation in SQL)
- Output encoding appropriate to context (HTML, JS, URL)
- Content-Type headers set correctly

**L2 Checks (includes L1):**
- Context-specific output encoding (HTML body vs attribute vs JS)
- Server-side validation mirrors client-side rules
- Allowlist-based input validation where possible
- Stored procedure usage review for injection risks

**L3 Checks (includes L1+L2):**
- DOM-based XSS prevention audit
- Second-order injection prevention
- Template injection detection
- LDAP/XML/XPath injection prevention

#### 4.4 — A04 Insecure Design (ASVS V1)

**L1 Checks:**
- Threat model exists or is referenced
- Secure design patterns documented
- Trust boundaries identified

**L2 Checks (includes L1):**
- Threat model updated for current architecture
- Security requirements traced to design decisions
- Abuse case scenarios documented

**L3 Checks (includes L1+L2):**
- Attack surface analysis documented
- Defense-in-depth layers verified
- Formal security architecture review completed

#### 4.5 — A05 Security Misconfiguration (ASVS V14)

**L1 Checks:**
- Default credentials removed or changed
- Error messages do not expose internal details
- Unnecessary features and frameworks disabled
- Security headers present (X-Content-Type-Options, X-Frame-Options)

**L2 Checks (includes L1):**
- HTTP Strict-Transport-Security (HSTS) configured
- Content-Security-Policy (CSP) defined
- Server version headers suppressed
- Directory permissions restrictive (owner-only)

**L3 Checks (includes L1+L2):**
- Subresource Integrity (SRI) for external resources
- Feature-Policy/Permissions-Policy configured
- Automated security configuration scanning in CI
- Runtime configuration drift detection

#### 4.6 — A07 Authentication Failures (ASVS V2, V3)

**L1 Checks:**
- Password policy enforced (minimum length, complexity)
- Account lockout or rate limiting on login
- Session tokens regenerated after authentication
- Secure session cookie attributes (HttpOnly, Secure, SameSite)

**L2 Checks (includes L1):**
- Multi-factor authentication supported
- Credential storage uses strong hashing (bcrypt, Argon2)
- Session timeout configured appropriately
- Re-authentication required for sensitive operations

**L3 Checks (includes L1+L2):**
- Hardware token or FIDO2/WebAuthn support
- Adaptive authentication based on risk signals
- Session binding to device/IP fingerprint
- Phishing-resistant authentication methods

#### 4.7 — A08 Software/Data Integrity (ASVS V10)

**L1 Checks:**
- Deserialization uses safe/strict mode
- CI/CD pipeline integrity (no unauthorized modifications)
- Dependency integrity verification (checksums/signatures)

**L2 Checks (includes L1):**
- Code signing for releases
- SBOM generated and maintained
- Auto-update mechanisms verify integrity

**L3 Checks (includes L1+L2):**
- SLSA Level 2+ build provenance
- Reproducible builds configured
- Supply chain attack monitoring

#### 4.8 — A09 Logging Failures (ASVS V7)

**L1 Checks:**
- Security-relevant events logged (auth, access control, input validation failures)
- Log format is structured (JSON)
- Sensitive data excluded from logs (PII, credentials)

**L2 Checks (includes L1):**
- Correlation IDs propagated across services
- Log integrity protection (tamper detection)
- Alerting configured for security events

**L3 Checks (includes L1+L2):**
- Immutable audit log storage
- Real-time security monitoring (SIEM integration)
- Log retention policy meets compliance requirements

#### 4.9 — A10 SSRF (ASVS V5, V13)

**L1 Checks:**
- URL validation on server-side requests
- Allowlist-based URL filtering for outbound requests
- Private/internal IP ranges blocked in user-supplied URLs

**L2 Checks (includes L1):**
- DNS rebinding protection
- Response validation (content-type, size limits)
- Network segmentation prevents internal service access

**L3 Checks (includes L1+L2):**
- Dedicated proxy for outbound requests
- URL canonicalization before validation
- Cloud metadata endpoint protection (169.254.169.254)

### Step 5 — Delegate A06 to x-dependency-audit

Per RULE-011 (Skill Composability), A06 (Vulnerable and Outdated Components) is delegated to `x-dependency-audit` via the Skill tool (Rule 10 — INLINE-SKILL pattern):

    Skill(skill: "x-dependency-audit", args: "--scope vulnerabilities")

The delegation result is recorded as:
```json
{
  "category": "A06",
  "categoryName": "Vulnerable and Outdated Components",
  "status": "DELEGATED",
  "delegatedTo": "x-dependency-audit",
  "totalChecks": 0,
  "passedChecks": 0,
  "failedChecks": 0
}
```

### Step 6 — Calculate Scores

#### 6.1 — Per-Category Score

For each category:
```
categoryScore = (passedChecks / totalChecks) * 100
status = categoryScore >= 70 ? "PASS" : "FAIL"
```

#### 6.2 — Overall Score

```
overallScore = sum(passedChecks for all categories)
             / sum(totalChecks for all categories) * 100
```

#### 6.3 — Grade Mapping

| Score Range | Grade |
|-------------|-------|
| 90-100 | A |
| 80-89 | B |
| 70-79 | C |
| 50-69 | D |
| 0-49 | F |

#### 6.4 — ASVS Coverage Percentage

```
asvsCoverage = (verifiedItems at requested level)
             / (totalItems at requested level) * 100
```

### Step 7 — Generate Reports

#### 7.1 — SARIF 2.1.0 Output

Write to `results/security/owasp-scan-YYYY-MM-DD.sarif.json`:

```json
{
  "$schema": "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/main/sarif-2.1/schema/sarif-schema-2.1.0.json",
  "version": "2.1.0",
  "runs": [{
    "tool": {
      "driver": {
        "name": "x-owasp-scan",
        "version": "1.0.0",
        "informationUri": "https://owasp.org/Top10/",
        "rules": [
          {
            "id": "OWASP-A01",
            "name": "BrokenAccessControl",
            "shortDescription": {
              "text": "A01:2021 - Broken Access Control"
            },
            "helpUri": "https://owasp.org/Top10/A01_2021-Broken_Access_Control/"
          }
        ]
      }
    },
    "results": [
      {
        "ruleId": "OWASP-A01",
        "level": "error",
        "message": {
          "text": "Finding description with fix recommendation"
        },
        "locations": [{
          "physicalLocation": {
            "artifactLocation": {
              "uri": "src/main/java/...",
              "uriBaseId": "%SRCROOT%"
            }
          }
        }],
        "properties": {
          "asvsChapter": "V4",
          "asvsRequirement": "V4.1.1",
          "asvsLevel": "L1",
          "owaspCategory": "A01",
          "fixRecommendation": "Implement server-side access control"
        }
      }
    ]
  }]
}
```

#### 7.2 — Markdown Report

Write to `results/security/owasp-scan-YYYY-MM-DD.md`:

```markdown
# OWASP Top 10 Scan Report — {{PROJECT_NAME}}

**Date:** YYYY-MM-DD
**ASVS Level:** L1 | L2 | L3
**Score:** NN/100 (Grade: A-F)
**ASVS Coverage:** NN.N%

## Summary

| Category | Name | ASVS | Status | Checks | Passed | Failed |
|----------|------|------|--------|--------|--------|--------|
| A01 | Broken Access Control | V4 | PASS/FAIL | N | N | N |
| A02 | Cryptographic Failures | V6, V9 | PASS/FAIL | N | N | N |
| A03 | Injection | V5 | PASS/FAIL | N | N | N |
| A04 | Insecure Design | V1 | PASS/FAIL | N | N | N |
| A05 | Security Misconfiguration | V14 | PASS/FAIL | N | N | N |
| A06 | Vulnerable Components | -- | DELEGATED | -- | -- | -- |
| A07 | Auth Failures | V2, V3 | PASS/FAIL | N | N | N |
| A08 | Software/Data Integrity | V10 | PASS/FAIL | N | N | N |
| A09 | Logging Failures | V7 | PASS/FAIL | N | N | N |
| A10 | SSRF | V5, V13 | PASS/FAIL | N | N | N |

## Per-Category Details

### A01 — Broken Access Control (V4)

**Status:** PASS/FAIL | **Score:** NN/100 | **Level:** L1

| # | Check | ASVS Ref | Level | Result |
|---|-------|----------|-------|--------|
| 1 | Access control enforced server-side | V4.1.1 | L1 | PASS/FAIL |
| 2 | Directory listing disabled | V4.1.2 | L1 | PASS/FAIL |

#### Findings

- **[F-A01-001]** {description}
  - **ASVS:** V4.1.1
  - **Severity:** HIGH
  - **Fix:** {fixRecommendation}

### A06 — Vulnerable Components (DELEGATED)

**Status:** DELEGATED to `x-dependency-audit`

> Run `/x-dependency-audit --scope vulnerabilities` for component analysis.

## Scoring

- **Overall Score:** NN/100
- **Grade:** A/B/C/D/F
- **ASVS Coverage:** NN.N% (at level LN)
- **Passed Categories:** N/10
- **Failed Categories:** N/10
- **Delegated Categories:** 1/10

## CI Integration

Exit code: 0 (all categories PASS or DELEGATED) / 1 (any category FAIL)
```

## CI Integration

When run in CI mode, the skill:
1. Generates SARIF output for GitHub Code Scanning / GHAS integration
2. Returns exit code 0 (pass) or 1 (fail)
3. Supports `--report-format sarif` for CI-only output
4. SARIF file can be uploaded via `github/codeql-action/upload-sarif`

## Error Handling

| Scenario | Action |
|----------|--------|
| Invalid --level value | Error with valid options list |
| Invalid --category value | Error with valid options list |
| Knowledge pack not found | Warn, continue with built-in checks |
| x-dependency-audit unavailable | Mark A06 as SKIPPED, note in report |
| Partial scan (some categories fail to verify) | Report verified categories, mark others SKIPPED |
| No source files found | Report "No source files found for verification" |

## Knowledge Pack References

| # | Knowledge Pack | Path | Purpose |
|---|----------------|------|---------|
| 1 | Security | `skills/security/SKILL.md` | OWASP ASVS verification items |
| 2 | Security References | `skills/security/references/application-security.md` | Detailed ASVS chapter mappings |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| x-dependency-audit | Delegates to | A06 (Vulnerable Components) delegated per RULE-011 |
| x-security-dashboard | Consumed by | SARIF output aggregated into security posture dashboard |
| x-threat-model | Complements | Threat model informs A04 (Insecure Design) checks |
