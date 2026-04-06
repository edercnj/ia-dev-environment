---
name: x-hardening-eval
description: >
  Evaluates application hardening posture against CIS and OWASP benchmarks:
  HTTP security headers, TLS configuration, CORS policy, cookie security,
  error handling, input limits, and information disclosure. Produces SARIF
  output with weighted scoring.
  Reference: `.github/skills/x-hardening-eval/SKILL.md`
---

# Skill: Application Hardening Eval

## Purpose

Evaluates the defensive posture of {{PROJECT_NAME}} against recognized benchmarks (CIS, OWASP). Analyzes 7 hardening dimensions with weighted scoring and produces SARIF 2.1.0 output with a Markdown report.

## Triggers

- `/x-hardening-eval --target <url>` -- full hardening evaluation
- `/x-hardening-eval --target <url> --scope headers` -- HTTP security headers only
- `/x-hardening-eval --target <url> --benchmark cis` -- CIS benchmark
- `/x-hardening-eval --target <url> --level L2` -- ASVS Level 2

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `--target` | URL | Yes | -- | Target application URL (HTTP/HTTPS) |
| `--scope` | Enum | No | `all` | Dimension: all, headers, tls, cors, cookies, errors, limits, disclosure |
| `--benchmark` | Enum | No | `owasp` | Benchmark: cis or owasp |
| `--level` | Enum | No | `L1` | ASVS level: L1, L2, L3 |

## Workflow

```
1. VALIDATE   -> Parse and validate CLI parameters
2. CONFIGURE  -> Load benchmark checks and ASVS level mapping
3. PROBE      -> Send HTTP requests to collect headers, TLS info, CORS behavior
4. EVALUATE   -> Assess each dimension against benchmark checks
5. SCORE      -> Calculate weighted scores per dimension and overall
6. REPORT     -> Generate SARIF 2.1.0 output + Markdown report
```

## Hardening Dimensions

| Dimension | Weight | Key Checks |
|-----------|--------|------------|
| HTTP Headers | 25% | HSTS, X-Frame-Options, X-Content-Type-Options, CSP, CORP, Permissions-Policy, Referrer-Policy |
| TLS | 20% | Minimum TLS 1.2, cipher suites, OCSP stapling, certificate chain |
| CORS | 15% | Origin whitelist, credentials handling, preflight cache, exposed headers |
| Cookies | 15% | Secure flag, HttpOnly flag, SameSite attribute, path scope, prefix |
| Error Handling | 10% | Stack trace suppression, error codes, custom error pages, debug mode |
| Input Limits | 10% | Request body size, URL length, header size, upload limits, rate limiting |
| Info Disclosure | 5% | Server header, X-Powered-By, version disclosure, directory listing |

## Benchmark Support

- **OWASP**: Application-focused checks derived from OWASP ASVS V14 and Secure Headers Project
- **CIS**: Infrastructure-focused checks derived from CIS Benchmarks for web servers

## ASVS Level Mapping

| Level | Description |
|-------|-------------|
| L1 | Basic security checks for all applications |
| L2 | Standard checks for applications with sensitive data |
| L3 | Advanced checks for high-value applications |

## Score Calculation

```
dimension_score = (passed_checks / total_checks) * 100
overall_score = sum(dimension_score * weight) / sum(applicable_weights)
```

| Grade | Score Range |
|-------|-------------|
| A | 90-100 |
| B | 80-89 |
| C | 70-79 |
| D | 60-69 |
| F | 0-59 |

## Output

- **SARIF 2.1.0**: `results/security/hardening-eval-YYYY-MM-DD.sarif.json`
- **Markdown**: `results/security/hardening-eval-YYYY-MM-DD.md` with score heatmap

## Error Handling

| Scenario | Action |
|----------|--------|
| Target unreachable | Error with URL, no score |
| Invalid certificate | Warn and proceed |
| TLS probe unavailable | Skip TLS, adjust weights |
| Unknown scope | Reject with error |
