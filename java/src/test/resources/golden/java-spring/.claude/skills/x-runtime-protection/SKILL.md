---
name: x-runtime-protection
description: "Evaluates runtime protection controls: rate limiting, WAF rules, bot protection, DDoS mitigation, account lockout, brute force protection, CSP enforcement, and permissions policy. Uses SARIF output and ASVS compliance mapping."
user-invocable: true
argument-hint: "--target <url> [--scope all|rate-limit|waf|bot-protection|account-lockout|brute-force|csp|permissions] [--intensity passive|moderate|aggressive] [--login-endpoint /path]"
allowed-tools: Read, Write, Bash, Glob, Grep
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Runtime Protection Eval

## Purpose

Evaluates runtime protection controls for {{PROJECT_NAME}} by analyzing active defense mechanisms including rate limiting, WAF rules, bot protection, account lockout, brute force mitigation, CSP enforcement, and permissions policy. Produces SARIF 2.1.0 output with ASVS compliance mapping and a scored Markdown report.

## Triggers

- `/x-runtime-protection --target https://app.example.com` -- evaluate all dimensions
- `/x-runtime-protection --target https://app.example.com --scope rate-limit` -- rate limiting only
- `/x-runtime-protection --target https://app.example.com --scope waf` -- WAF rules only
- `/x-runtime-protection --target https://app.example.com --scope account-lockout --login-endpoint /api/auth/login` -- account lockout
- `/x-runtime-protection --target https://app.example.com --intensity passive` -- observe headers only
- `/x-runtime-protection --target https://app.example.com --intensity aggressive` -- full volume testing (local/dev only)

## Parameters

| Parameter | Type | Required | Default | Validation | Example |
|-----------|------|----------|---------|------------|---------|
| `--target` | URL | Yes | -- | Valid HTTP/HTTPS URL | `https://app.example.com` |
| `--scope` | Enum | No | `all` | `all`, `rate-limit`, `waf`, `bot-protection`, `account-lockout`, `brute-force`, `csp`, `permissions` | `rate-limit` |
| `--intensity` | Enum | No | `moderate` | `passive`, `moderate`, `aggressive` | `passive` |
| `--login-endpoint` | Path | No | -- | Relative path starting with `/` | `/api/auth/login` |

## Intensity Levels

| Level | Behavior | Allowed Environments |
|-------|----------|---------------------|
| **passive** | Observe headers and configurations only; no payloads sent | All environments |
| **moderate** | Send non-destructive test payloads (default) | All environments |
| **aggressive** | Test limits with higher volume of requests | Local and dev only |

**Environment restriction:** If `--intensity=aggressive` is used in a production environment, automatically downgrade to `passive` and emit a warning: `"Aggressive intensity not allowed in production"`.

## Workflow

```
1. VALIDATE   -> Validate parameters and check target reachability
2. CONFIGURE  -> Resolve intensity (enforce env restrictions)
3. DISCOVER   -> Probe target for baseline response headers
4. EVALUATE   -> Run per-dimension checks based on --scope
5. SCORE      -> Calculate per-dimension and overall scores
6. REPORT     -> Generate SARIF 2.1.0 + Markdown report
```

### Step 1 -- VALIDATE Parameters

Validate all inputs before execution:

- `--target` must be a valid HTTP or HTTPS URL
- `--target` must be reachable (TCP connection within 10s timeout)
- If target is unreachable, emit error `"Target unreachable: {url}"` and abort
- `--scope` must match one of the 8 valid values
- `--login-endpoint` is required when `--scope=account-lockout`

### Step 2 -- CONFIGURE Intensity

Resolve effective intensity:

1. If `--intensity=aggressive` and environment is production:
   - Log warning: `"Aggressive intensity not allowed in production"`
   - Downgrade to `passive`
2. Record effective intensity in report metadata

### Step 3 -- DISCOVER Baseline

Probe the target to establish baseline:

- Send a single GET request to `--target`
- Record response headers for later comparison
- Extract security-relevant headers: `X-RateLimit-*`, `Content-Security-Policy`, `Permissions-Policy`, `X-Content-Type-Options`, `Strict-Transport-Security`
- Record server identification if exposed

### Step 4 -- EVALUATE Dimensions

Execute checks for each dimension in scope. Each dimension produces a `RuntimeProtectionResult`.

#### 4.1 Rate Limiting

**ASVS Reference:** V4.3 (Access Control)

| Check | Passive | Moderate | Aggressive |
|-------|---------|----------|------------|
| `X-RateLimit-*` headers present | Yes | Yes | Yes |
| `Retry-After` header on 429 | No | Yes | Yes |
| Global rate limit triggers 429 | No | Yes | Yes |
| Per-endpoint rate limit | No | Yes | Yes |
| Per-user rate limit | No | No | Yes |
| Burst handling | No | No | Yes |

**Moderate test procedure:**
1. Send burst of 100 requests to target root endpoint within 10 seconds
2. Count responses with status 429
3. Validate `Retry-After` header is present on 429 responses
4. Check for `X-RateLimit-Remaining` and `X-RateLimit-Limit` headers

**Result fields:**

| Field | Type | Description |
|-------|------|-------------|
| endpoint | String | Tested endpoint path |
| requestsSent | int | Total requests sent |
| requestsBlocked | int | Responses with 429 status |
| statusCode | int | Expected blocking status (429) |
| retryAfterPresent | boolean | Whether Retry-After header was found |
| rateLimitHeaders | Map | Rate limit headers observed |

#### 4.2 WAF Rules (OWASP CRS)

**ASVS Reference:** V5.1 (Input Validation)

| Check | Passive | Moderate | Aggressive |
|-------|---------|----------|------------|
| Security headers indicating WAF | Yes | Yes | Yes |
| SQL injection blocking | No | Yes | Yes |
| XSS payload blocking | No | Yes | Yes |
| Path traversal blocking | No | Yes | Yes |
| Custom WAF rules | No | No | Yes |

**Non-destructive test payloads (moderate):**

```
SQL Injection:  GET /search?q=' OR '1'='1
XSS:            GET /search?q=<script>alert(1)</script>
Path Traversal: GET /files/../../../etc/passwd
Command Inject: GET /ping?host=;cat /etc/passwd
```

These payloads are intentionally non-destructive -- they probe for WAF blocking behavior (expecting 403 or custom block page) without causing harm.

**Status:** `PROTECTED` if all payloads return 403/blocking response; `PARTIAL` if some blocked; `UNPROTECTED` if none blocked; `SKIPPED` in passive mode.

#### 4.3 Bot Protection

**ASVS Reference:** V13.1 (API and Web Service)

| Check | Passive | Moderate | Aggressive |
|-------|---------|----------|------------|
| CAPTCHA presence in HTML | Yes | Yes | Yes |
| Bot detection headers | Yes | Yes | Yes |
| Automated request blocking | No | Yes | Yes |
| Challenge page detection | No | Yes | Yes |

**Checks:**
- Scan HTML responses for CAPTCHA markers (reCAPTCHA, hCaptcha, Turnstile)
- Check for bot-detection response headers
- Send requests with missing/invalid User-Agent
- Detect challenge/interstitial pages

#### 4.4 Account Lockout

**ASVS Reference:** V2.2 (Authentication)

| Check | Passive | Moderate | Aggressive |
|-------|---------|----------|------------|
| Login endpoint responds | Yes | Yes | Yes |
| Lockout after max attempts | No | Yes | Yes |
| Lockout duration enforced | No | Yes | Yes |
| Progressive delay detection | No | No | Yes |
| Lockout notification | No | No | Yes |
| Reset mechanism available | No | No | Yes |

**Requires:** `--login-endpoint` parameter.

**Moderate test procedure:**
1. Send 10 login attempts with invalid credentials to `--login-endpoint`
2. Check for 423 (Locked) or 429 (Too Many Requests) response
3. Validate lockout message in response body
4. Record number of attempts before lockout

**Finding on absence:** If no lockout is detected after 10 failed attempts, generate a `CRITICAL` finding with fix recommendation including max attempts and lockout duration configuration.

#### 4.5 Brute Force Protection

**ASVS Reference:** V2.2 (Authentication), V11.1 (Business Logic)

| Check | Passive | Moderate | Aggressive |
|-------|---------|----------|------------|
| Rate limiting on auth endpoints | Yes | Yes | Yes |
| Progressive delay observed | No | Yes | Yes |
| API key enumeration protection | No | Yes | Yes |
| Timing attack mitigation | No | Yes | Yes |

**Checks:**
- Verify rate limiting on authentication endpoints (separate from global rate limit)
- Measure response time variance for valid vs. invalid credentials (timing attack mitigation expects constant time)
- Send sequential requests with incrementing API key patterns to detect enumeration protection

#### 4.6 CSP Enforcement

**ASVS Reference:** V14.4 (Configuration)

| Check | Passive | Moderate | Aggressive |
|-------|---------|----------|------------|
| `Content-Security-Policy` header present | Yes | Yes | Yes |
| CSP directives completeness | Yes | Yes | Yes |
| `report-uri` or `report-to` configured | Yes | Yes | Yes |
| `unsafe-inline` absent | Yes | Yes | Yes |
| `unsafe-eval` absent | Yes | Yes | Yes |

**CSP directives to validate:**

| Directive | Expected | Severity if Missing |
|-----------|----------|-------------------|
| `default-src` | Present and restrictive | HIGH |
| `script-src` | No `unsafe-inline` or `unsafe-eval` | CRITICAL |
| `style-src` | Defined | MEDIUM |
| `img-src` | Defined | LOW |
| `connect-src` | Defined | MEDIUM |
| `frame-ancestors` | `'none'` or specific origins | HIGH |
| `report-uri` / `report-to` | Present | MEDIUM |

#### 4.7 Permissions Policy

**ASVS Reference:** V14.4 (Configuration)

| Check | Passive | Moderate | Aggressive |
|-------|---------|----------|------------|
| `Permissions-Policy` header present | Yes | Yes | Yes |
| Feature restrictions defined | Yes | Yes | Yes |
| Geolocation restricted | Yes | Yes | Yes |
| Camera/microphone restricted | Yes | Yes | Yes |
| Payment API restricted | Yes | Yes | Yes |

**Features to validate:**

| Feature | Expected | Severity if Unrestricted |
|---------|----------|------------------------|
| `geolocation` | `()` (disabled) or specific origins | MEDIUM |
| `camera` | `()` (disabled) | HIGH |
| `microphone` | `()` (disabled) | HIGH |
| `payment` | `()` (disabled) or specific origins | HIGH |
| `usb` | `()` (disabled) | MEDIUM |
| `autoplay` | Restricted | LOW |

### Step 5 -- SCORE Results

#### Per-Dimension Scoring

For each dimension, calculate:

```
dimensionScore = (passedChecks / totalChecks) * 100
```

**Status assignment:**

| Score Range | Status |
|-------------|--------|
| 80-100 | PROTECTED |
| 40-79 | PARTIAL |
| 0-39 | UNPROTECTED |
| N/A | SKIPPED |

#### Overall Scoring

```
overallScore = max(0, 100 - sum(severityWeight * findingCount))
```

**Severity weights:** CRITICAL=10, HIGH=5, MEDIUM=2, LOW=1, INFO=0

**Grade assignment:**

| Score | Grade |
|-------|-------|
| 90-100 | A |
| 80-89 | B |
| 70-79 | C |
| 60-69 | D |
| 0-59 | F |

### Step 6 -- REPORT Generation

Generate two output files:

#### SARIF 2.1.0 Output

Write to `results/security/runtime-protection-{timestamp}.sarif.json`:

```json
{
  "$schema": "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/main/sarif-2.1/schema/sarif-schema-2.1.0.json",
  "version": "2.1.0",
  "runs": [{
    "tool": {
      "driver": {
        "name": "x-runtime-protection",
        "version": "1.0.0",
        "informationUri": "https://owasp.org/www-project-application-security-verification-standard/",
        "rules": [
          {
            "id": "RTPROT-001",
            "shortDescription": { "text": "Rate limit not configured" },
            "helpUri": "https://cheatsheetseries.owasp.org/cheatsheets/Denial_of_Service_Cheat_Sheet.html",
            "properties": { "asvs-ref": "V4.3", "dimension": "rate-limit" }
          }
        ]
      }
    },
    "results": [
      {
        "ruleId": "RTPROT-001",
        "level": "error",
        "message": { "text": "No rate limiting detected on endpoint /api/users" },
        "locations": [{ "physicalLocation": { "artifactLocation": { "uri": "https://app.example.com/api/users" } } }],
        "properties": {
          "dimension": "rate-limit",
          "asvs-ref": "V4.3",
          "severity": "CRITICAL",
          "fix-recommendation": "Configure rate limiting (e.g., 100 req/min per IP) with 429 response and Retry-After header"
        }
      }
    ]
  }]
}
```

#### Markdown Report

Write to `results/security/runtime-protection-{timestamp}-report.md`:

```markdown
# Runtime Protection Report -- {{PROJECT_NAME}}

**Date:** YYYY-MM-DD
**Target:** {target_url}
**Scope:** {scope}
**Intensity:** {effective_intensity}

## Overall Score: {score}/100 (Grade {grade})

## Dimension Summary

| Dimension | Status | Score | Checks | Passed | ASVS Ref |
|-----------|--------|-------|--------|--------|----------|
| Rate Limiting | PROTECTED | 90 | 6 | 5 | V4.3 |
| WAF Rules | PARTIAL | 60 | 5 | 3 | V5.1 |
| Bot Protection | UNPROTECTED | 20 | 4 | 1 | V13.1 |
| Account Lockout | SKIPPED | -- | -- | -- | V2.2 |
| Brute Force | PROTECTED | 85 | 4 | 3 | V2.2, V11.1 |
| CSP Enforcement | PARTIAL | 70 | 5 | 3 | V14.4 |
| Permissions Policy | PROTECTED | 100 | 5 | 5 | V14.4 |

## Critical Gaps

- {dimension}: {description of gap and remediation}

## Detailed Findings

### [RTPROT-001] {Dimension} -- {Finding Title}
- **Severity:** CRITICAL
- **ASVS Reference:** V4.3
- **Description:** {detailed description}
- **Evidence:** {observed behavior}
- **Fix Recommendation:** {actionable fix steps}

## Recommendations

1. **Immediate:** Address CRITICAL findings ({count})
2. **Short-term:** Resolve HIGH findings ({count})
3. **Long-term:** Review MEDIUM/LOW findings ({count})
```

## ASVS Mapping Summary

| Dimension | ASVS Chapter | ASVS Section | Key Requirements |
|-----------|-------------|--------------|-----------------|
| Rate Limiting | V4 Access Control | V4.3 | Rate limiting, resource protection |
| WAF Rules | V5 Validation | V5.1 | Input validation, injection prevention |
| Bot Protection | V13 API Security | V13.1 | API security mechanisms |
| Account Lockout | V2 Authentication | V2.2 | Credential protection, lockout |
| Brute Force | V2 Authentication, V11 Business Logic | V2.2, V11.1 | Brute force mitigation |
| CSP Enforcement | V14 Configuration | V14.4 | HTTP security headers |
| Permissions Policy | V14 Configuration | V14.4 | Feature policy headers |

## Error Handling

| Scenario | Action |
|----------|--------|
| Target unreachable | Emit `"Target unreachable: {url}"`, abort with no score |
| Connection timeout | Retry once with doubled timeout, then report error |
| Target returns 5xx | Report as finding (server error under load) |
| Login endpoint not provided for account-lockout scope | Warn and skip account-lockout dimension |
| Aggressive in production | Downgrade to passive, emit warning |
| Partial dimension failure | Complete other dimensions, mark failed as SKIPPED |
| SSL certificate error | Warn and continue with `--insecure` flag |

## Integration Notes

- References OWASP ASVS knowledge pack for compliance mapping
- References security knowledge pack (`skills/security/`) for remediation recommendations
- Uses SARIF 2.1.0 output format compatible with GitHub Advanced Security
- Scoring model follows security scoring convention (0-100 scale, A-F grades)
- Uses `security-engineer` agent for in-depth analysis via Agent tool
- Can be integrated into CI/CD pipelines for automated runtime protection validation
