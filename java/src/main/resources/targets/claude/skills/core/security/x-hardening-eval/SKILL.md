---
name: x-hardening-eval
description: "Evaluates application hardening posture against CIS and OWASP benchmarks: HTTP security headers, TLS configuration, CORS policy, cookie security, error handling, input limits, and information disclosure. Produces SARIF output with weighted scoring."
user-invocable: true
allowed-tools: Read, Write, Bash, Grep, Glob, Agent
argument-hint: "--target <url> [--scope all|headers|tls|cors|cookies|errors|limits|disclosure] [--benchmark cis|owasp] [--level L1|L2|L3]"
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Application Hardening Eval

## Purpose

Evaluates the defensive posture of {{PROJECT_NAME}} against recognized benchmarks (CIS, OWASP). Analyzes 7 hardening dimensions with weighted scoring: HTTP security headers, TLS configuration, CORS policy, cookie security, error handling, input limits, and information disclosure. Produces SARIF 2.1.0 output and a Markdown report with per-dimension scores.

## Triggers

- `/x-hardening-eval --target <url>` — full hardening evaluation (all dimensions, OWASP benchmark, L1)
- `/x-hardening-eval --target <url> --scope headers` — evaluate HTTP security headers only
- `/x-hardening-eval --target <url> --scope tls` — evaluate TLS configuration only
- `/x-hardening-eval --target <url> --benchmark cis` — evaluate against CIS benchmark
- `/x-hardening-eval --target <url> --level L2` — evaluate against ASVS Level 2

## Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `--target` | URL | Yes | — | Target application URL (HTTP/HTTPS) |
| `--scope` | Enum | No | `all` | Dimension filter: all, headers, tls, cors, cookies, errors, limits, disclosure |
| `--benchmark` | Enum | No | `owasp` | Benchmark: cis (infrastructure-focused) or owasp (application-focused) |
| `--level` | Enum | No | `L1` | ASVS verification level: L1, L2, L3 |

## Workflow

```
1. VALIDATE   -> Parse and validate CLI parameters
2. CONFIGURE  -> Load benchmark checks and ASVS level mapping
3. PROBE      -> Send HTTP requests to collect headers, TLS info, CORS behavior
4. EVALUATE   -> Assess each dimension against benchmark checks
5. SCORE      -> Calculate weighted scores per dimension and overall
6. REPORT     -> Generate SARIF 2.1.0 output + Markdown report
```

### Step 1 — Validate Parameters

Validate all CLI parameters before proceeding:

- `--target` must be a valid HTTP or HTTPS URL
- `--scope` must be one of: all, headers, tls, cors, cookies, errors, limits, disclosure
- `--benchmark` must be one of: cis, owasp
- `--level` must be one of: L1, L2, L3

**Error — Target Unreachable:**

If the target URL is unreachable (connection refused, DNS failure, timeout):

```
ERROR: Target unreachable: <url>
Reason: <connection error detail>
No score calculated. Verify the URL and ensure the application is running.
```

### Step 2 — Configure Benchmark Checks

Load the appropriate check set based on `--benchmark` and `--level`:

#### OWASP Benchmark (Default)

Checks derived from OWASP ASVS V14 (Configuration) and OWASP Secure Headers Project.

#### CIS Benchmark

Checks derived from CIS Benchmarks for web servers (Apache, Nginx, IIS). Includes additional server-specific hardening checks.

#### ASVS Level Mapping

| Level | Scope | Description |
|-------|-------|-------------|
| L1 | Opportunistic | Basic security checks applicable to all applications |
| L2 | Standard | Covers most applications with sensitive data |
| L3 | Advanced | High-value applications requiring maximum assurance |

Higher levels include all checks from lower levels plus additional requirements.

### Step 3 — Probe Target

Send HTTP requests to collect security-relevant data:

```bash
# Collect response headers
curl -sI --max-time 10 <target-url>

# Test CORS with preflight
curl -sI -X OPTIONS \
  -H "Origin: https://evil.example.com" \
  -H "Access-Control-Request-Method: POST" \
  --max-time 10 <target-url>

# Check TLS configuration (if HTTPS)
openssl s_client -connect <host>:<port> \
  -servername <host> </dev/null 2>/dev/null

# Test error handling
curl -s --max-time 10 <target-url>/nonexistent-path-404
```

### Step 4 — Evaluate Dimensions

Evaluate each dimension (filtered by `--scope`) against the configured benchmark checks.

#### 4.1 — HTTP Security Headers (Weight: 25%)

| Header | CIS | OWASP | Check | Severity |
|--------|-----|-------|-------|----------|
| Strict-Transport-Security | Yes | Yes | Present, max-age >= 31536000, includeSubDomains | HIGH |
| X-Frame-Options | Yes | Yes | DENY or SAMEORIGIN | MEDIUM |
| X-Content-Type-Options | Yes | Yes | nosniff | MEDIUM |
| Content-Security-Policy | No | Yes | Present, no unsafe-inline or unsafe-eval | HIGH |
| Cross-Origin-Resource-Policy | No | Yes | same-origin or same-site | MEDIUM |
| Permissions-Policy | No | Yes | Present and restrictive | LOW |
| Referrer-Policy | No | Yes | strict-origin-when-cross-origin or no-referrer | LOW |

**HSTS Fix Recommendation:**

```
Strict-Transport-Security: max-age=31536000; includeSubDomains
```

**CSP Fix Recommendation:**

```
Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self'; font-src 'self'
```

#### 4.2 — TLS Configuration (Weight: 20%)

| Check | CIS | OWASP | Requirement | Severity |
|-------|-----|-------|-------------|----------|
| TLS Version | Yes | Yes | Minimum TLS 1.2 (L1), TLS 1.3 only (L3) | HIGH |
| Cipher Suites | Yes | Yes | Strong ciphers only (AEAD) | HIGH |
| OCSP Stapling | Yes | No | Enabled for certificate validation | MEDIUM |
| Certificate Chain | Yes | Yes | Valid, not expired, correct hostname | HIGH |
| HSTS Preload | No | Yes | Listed in HSTS preload list (L2+) | LOW |

#### 4.3 — CORS Policy (Weight: 15%)

| Check | CIS | OWASP | Requirement | Severity |
|-------|-----|-------|-------------|----------|
| Origin Whitelist | No | Yes | No wildcard (*) with credentials | HIGH |
| Credentials Handling | No | Yes | Access-Control-Allow-Credentials only with explicit origins | HIGH |
| Preflight Cache | No | Yes | Access-Control-Max-Age set (recommended >= 600) | LOW |
| Exposed Headers | No | Yes | Minimal set of exposed headers | MEDIUM |
| Allowed Methods | Yes | Yes | Restrict to required HTTP methods only | MEDIUM |

#### 4.4 — Cookie Security (Weight: 15%)

| Check | CIS | OWASP | Requirement | Severity |
|-------|-----|-------|-------------|----------|
| Secure Flag | Yes | Yes | All session cookies use Secure flag | HIGH |
| HttpOnly Flag | Yes | Yes | Session cookies use HttpOnly flag | HIGH |
| SameSite Attribute | No | Yes | SameSite=Strict or SameSite=Lax | MEDIUM |
| Path Scope | No | Yes | Path restricted to application root | LOW |
| Cookie Prefix | No | Yes | __Secure- or __Host- prefix (L2+) | LOW |

#### 4.5 — Error Handling (Weight: 10%)

| Check | CIS | OWASP | Requirement | Severity |
|-------|-----|-------|-------------|----------|
| Stack Trace Suppression | Yes | Yes | No stack traces in error responses | HIGH |
| Error Code Format | No | Yes | Standardized error codes (RFC 7807) | MEDIUM |
| Custom Error Pages | Yes | Yes | Custom 404/500 pages without server info | MEDIUM |
| Debug Mode | Yes | Yes | Debug mode disabled in production | CRITICAL |

#### 4.6 — Input Limits (Weight: 10%)

| Check | CIS | OWASP | Requirement | Severity |
|-------|-----|-------|-------------|----------|
| Request Body Size | Yes | Yes | Max body size configured | MEDIUM |
| URL Length | Yes | No | Max URL length enforced (8192 recommended) | LOW |
| Header Size | Yes | No | Max header size configured | LOW |
| Upload Limits | Yes | Yes | File upload size restricted | MEDIUM |
| Rate Limiting | No | Yes | Rate limiting headers present (X-RateLimit-*) | HIGH |

#### 4.7 — Information Disclosure (Weight: 5%)

| Check | CIS | OWASP | Requirement | Severity |
|-------|-----|-------|-------------|----------|
| Server Header | Yes | Yes | Removed or generic (no version) | MEDIUM |
| X-Powered-By | Yes | Yes | Removed entirely | MEDIUM |
| Version Disclosure | Yes | Yes | No version numbers in headers or body | MEDIUM |
| Directory Listing | Yes | No | Disabled | HIGH |

### Step 5 — Calculate Weighted Score

#### Dimension Weights

| Dimension | Weight |
|-----------|--------|
| headers | 0.25 |
| tls | 0.20 |
| cors | 0.15 |
| cookies | 0.15 |
| errors | 0.10 |
| limits | 0.10 |
| disclosure | 0.05 |

**Score Calculation:**

```
dimension_score = (passed_checks / total_checks) * 100

overall_score = sum(dimension_score * weight) / sum(applicable_weights)
```

When `--scope` filters dimensions, only the applicable dimensions are included in the weighted average. Excluded dimensions do not affect the overall score.

#### Grade Mapping

| Score Range | Grade |
|-------------|-------|
| 90-100 | A |
| 80-89 | B |
| 70-79 | C |
| 60-69 | D |
| 0-59 | F |

### Step 6 — Generate Reports

#### 6.1 — SARIF 2.1.0 Output

Write SARIF to `results/security/hardening-eval-YYYY-MM-DD.sarif.json`:

```json
{
  "$schema": "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/main/sarif-2.1/schema/sarif-schema-2.1.0.json",
  "version": "2.1.0",
  "runs": [{
    "tool": {
      "driver": {
        "name": "x-hardening-eval",
        "version": "1.0.0",
        "rules": [
          {
            "id": "HARDEN-HDR-001",
            "name": "MissingHSTS",
            "shortDescription": {
              "text": "Strict-Transport-Security header missing"
            },
            "defaultConfiguration": {
              "level": "error"
            },
            "properties": {
              "dimension": "headers",
              "benchmark": "owasp",
              "asvsLevel": "L1",
              "severity": "HIGH"
            }
          }
        ]
      }
    },
    "results": [
      {
        "ruleId": "HARDEN-HDR-001",
        "level": "error",
        "message": {
          "text": "HSTS header not found. Add: Strict-Transport-Security: max-age=31536000; includeSubDomains"
        },
        "locations": [{
          "physicalLocation": {
            "artifactLocation": {
              "uri": "https://app.example.com"
            }
          }
        }],
        "properties": {
          "fixRecommendation": "Strict-Transport-Security: max-age=31536000; includeSubDomains"
        }
      }
    ],
    "properties": {
      "overallScore": 78,
      "grade": "C",
      "benchmark": "owasp",
      "level": "L1"
    }
  }]
}
```

#### SARIF Rule ID Convention

| Dimension | Prefix | Example |
|-----------|--------|---------|
| headers | HARDEN-HDR | HARDEN-HDR-001 |
| tls | HARDEN-TLS | HARDEN-TLS-001 |
| cors | HARDEN-COR | HARDEN-COR-001 |
| cookies | HARDEN-COK | HARDEN-COK-001 |
| errors | HARDEN-ERR | HARDEN-ERR-001 |
| limits | HARDEN-LIM | HARDEN-LIM-001 |
| disclosure | HARDEN-DIS | HARDEN-DIS-001 |

#### 6.2 — Markdown Report

Write report to `results/security/hardening-eval-YYYY-MM-DD.md`:

```markdown
# Application Hardening Evaluation — {{PROJECT_NAME}}

**Date:** YYYY-MM-DD
**Target:** <url>
**Benchmark:** OWASP / CIS
**ASVS Level:** L1 / L2 / L3
**Overall Score:** NN/100 (Grade: X)

## Score Heatmap

| Dimension | Weight | Score | Passed | Failed | Grade |
|-----------|--------|-------|--------|--------|-------|
| HTTP Headers | 25% | 85 | 6/7 | 1 | B |
| TLS | 20% | 100 | 5/5 | 0 | A |
| CORS | 15% | 60 | 3/5 | 2 | D |
| Cookies | 15% | 75 | 3/4 | 1 | C |
| Error Handling | 10% | 50 | 2/4 | 2 | F |
| Input Limits | 10% | 80 | 4/5 | 1 | B |
| Info Disclosure | 5% | 100 | 4/4 | 0 | A |

**Weakest Dimension:** Error Handling (50)
**Strongest Dimension:** TLS (100)

## Findings

### [HARDEN-HDR-001] Missing HSTS Header
- **Dimension:** HTTP Headers
- **Severity:** HIGH
- **Benchmark:** OWASP, CIS
- **ASVS Reference:** V14.4.1
- **Description:** Strict-Transport-Security header not present
- **Fix:** Add header `Strict-Transport-Security: max-age=31536000; includeSubDomains`

## Recommendations

1. **Immediate:** Address HIGH and CRITICAL findings
2. **Short-term:** Resolve MEDIUM findings in current sprint
3. **Long-term:** Review LOW findings for defense-in-depth
```

## CI Integration

### GitHub Actions

```yaml
- name: Hardening Eval
  run: |
    /x-hardening-eval --target ${{ env.APP_URL }} \
      --benchmark owasp --level L1
    # Upload SARIF to GitHub Security tab
    gh api repos/${{ github.repository }}/code-scanning/sarifs \
      -f "sarif=$(cat results/security/hardening-eval-*.sarif.json | base64)"
```

### GitLab CI

```yaml
hardening-eval:
  stage: security
  script:
    - /x-hardening-eval --target $APP_URL --benchmark owasp --level L1
  artifacts:
    reports:
      sast: results/security/hardening-eval-*.sarif.json
```

## Error Handling

| Scenario | Action |
|----------|--------|
| Target unreachable | Report error with URL, no score calculated |
| HTTPS target with invalid certificate | Warn, proceed with evaluation, note in findings |
| Non-HTTP target | Reject with "Target must be HTTP or HTTPS URL" |
| TLS probing unavailable | Skip TLS dimension, adjust weights |
| Partial response | Evaluate available data, note gaps in report |
| Unknown scope value | Reject with "Invalid scope: <value>" |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `security-engineer` agent | calls | Used for in-depth analysis via Agent tool |
| `x-security-dashboard` | reads | Dashboard aggregates results from this skill |
| `x-owasp-scan` | complementary | OWASP scan covers application-level vulnerabilities; hardening covers infrastructure posture |

## Knowledge Pack References

| Pack | Files | Purpose |
|------|-------|---------|
| security | `skills/security/references/security-principles.md` | Data classification, input validation, fail-secure patterns |
| security | `skills/security/references/application-security.md` | OWASP Top 10, security headers, secrets management |
| security | `skills/security/references/cryptography.md` | TLS requirements, cipher suites, certificate management |
