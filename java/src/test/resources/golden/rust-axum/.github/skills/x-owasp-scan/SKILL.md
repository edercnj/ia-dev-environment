---
name: x-owasp-scan
description: >
  Automated OWASP Top 10 (2021) verification mapped to ASVS levels (L1/L2/L3).
  Checks all 10 categories (A01-A10) with per-category pass/fail, scoring, SARIF
  output, and CI integration. Delegates A06 to x-dependency-audit.
  Reference: `.github/skills/x-owasp-scan/SKILL.md`
---

# Skill: OWASP Top 10 Verification

## Purpose

Verifies {{PROJECT_NAME}} against the OWASP Top 10 (2021) with verification items mapped to ASVS chapters and levels. Produces per-category pass/fail, overall score (0-100), and ASVS coverage percentage.

## Triggers

- `/x-owasp-scan` -- full L1 scan, all categories
- `/x-owasp-scan --level L2` -- L2 scan (standard defense)
- `/x-owasp-scan --level L3` -- L3 scan (advanced/critical apps)
- `/x-owasp-scan --category A03` -- single category scan
- `/x-owasp-scan --report-format sarif` -- SARIF output only

## Parameters

| Parameter | Default | Values | Description |
|-----------|---------|--------|-------------|
| `--level` | L1 | L1, L2, L3 | ASVS verification depth |
| `--category` | all | A01-A10, all | OWASP category filter |
| `--report-format` | both | markdown, sarif, both | Output format |

## ASVS Levels

| Level | Name | Target |
|-------|------|--------|
| L1 | Opportunistic | Any application -- minimum automated checks |
| L2 | Standard | Most applications -- automated + manual review |
| L3 | Advanced | Critical systems -- comprehensive assurance |

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

## OWASP Top 10 to ASVS Mapping

| OWASP | ID | ASVS | Focus |
|-------|-----|------|-------|
| Broken Access Control | A01 | V4 | RBAC, path traversal, CORS |
| Cryptographic Failures | A02 | V6, V9 | Encryption, TLS, key management |
| Injection | A03 | V5 | Input validation, parameterized queries |
| Insecure Design | A04 | V1 | Threat modeling, secure patterns |
| Security Misconfiguration | A05 | V14 | Defaults, hardening, headers |
| Vulnerable Components | A06 | N/A | **DELEGATED** to `x-dependency-audit` |
| Auth Failures | A07 | V2, V3 | Authentication, session management |
| Software/Data Integrity | A08 | V10 | Deserialization, CI/CD security |
| Logging Failures | A09 | V7 | Structured logging, monitoring |
| SSRF | A10 | V5, V13 | URL validation, API security |

## Verification Checks per Level

Each category includes checks at L1 (minimum), L2 (standard), and L3 (advanced). Higher levels include all lower-level checks.

**Example -- A01 Broken Access Control:**

| Level | Sample Checks |
|-------|--------------|
| L1 | Server-side access control, directory listing disabled |
| L2 | RBAC across endpoints, anti-CSRF tokens |
| L3 | Anti-automation, fraud detection, MFA for critical ops |

## Delegation (RULE-011)

A06 (Vulnerable and Outdated Components) is delegated to `x-dependency-audit --scope vulnerabilities`. The result is recorded with status `DELEGATED` and `delegatedTo: x-dependency-audit`.

## Scoring

| Score | Grade |
|-------|-------|
| 90-100 | A |
| 80-89 | B |
| 70-79 | C |
| 50-69 | D |
| 0-49 | F |

Per-category: `(passedChecks / totalChecks) * 100`, status PASS if >= 70.
Overall: aggregate of all categories.

## Output

- **SARIF:** `results/security/owasp-scan-YYYY-MM-DD.sarif.json` (SARIF 2.1.0)
- **Markdown:** `results/security/owasp-scan-YYYY-MM-DD.md`

## CI Integration

- SARIF output compatible with GitHub Code Scanning
- Exit code 0 (pass) / 1 (fail)
- Upload via `github/codeql-action/upload-sarif`

## Error Handling

| Scenario | Action |
|----------|--------|
| Invalid parameter | Error with valid options list |
| KP not found | Warn, use built-in checks |
| x-dependency-audit unavailable | Mark A06 as SKIPPED |
| Partial scan | Report verified, mark others SKIPPED |
