---
name: x-runtime-eval
description: >
  Evaluates runtime protection controls: rate limiting, WAF rules, bot
  protection, DDoS mitigation, account lockout, brute force protection, CSP
  enforcement, and permissions policy. Uses SARIF output and ASVS compliance
  mapping.
  Reference: `.github/skills/x-runtime-eval/SKILL.md`
---

# Skill: Runtime Protection Eval

## Purpose

Evaluates runtime protection controls for {{PROJECT_NAME}} by analyzing active defense mechanisms including rate limiting, WAF rules, bot protection, account lockout, brute force mitigation, CSP enforcement, and permissions policy. Produces SARIF 2.1.0 output with ASVS compliance mapping and a scored Markdown report.

## Triggers

- `/x-runtime-eval --target https://app.example.com` -- evaluate all dimensions
- `/x-runtime-eval --target https://app.example.com --scope rate-limit` -- rate limiting only
- `/x-runtime-eval --target https://app.example.com --scope waf` -- WAF rules only
- `/x-runtime-eval --target https://app.example.com --scope account-lockout --login-endpoint /api/auth/login`
- `/x-runtime-eval --target https://app.example.com --intensity passive` -- observe headers only

## Parameters

| Parameter | Type | Required | Default | Validation |
|-----------|------|----------|---------|------------|
| `--target` | URL | Yes | -- | Valid HTTP/HTTPS URL |
| `--scope` | Enum | No | `all` | `all`, `rate-limit`, `waf`, `bot-protection`, `account-lockout`, `brute-force`, `csp`, `permissions` |
| `--intensity` | Enum | No | `moderate` | `passive`, `moderate`, `aggressive` |
| `--login-endpoint` | Path | No | -- | Relative path starting with `/` |

## Intensity Levels

| Level | Behavior | Allowed Environments |
|-------|----------|---------------------|
| **passive** | Observe headers and configurations only | All |
| **moderate** | Non-destructive test payloads (default) | All |
| **aggressive** | Higher volume testing | Local/dev only |

## Workflow

```
1. VALIDATE   -> Validate parameters, check target reachability
2. CONFIGURE  -> Resolve intensity (enforce env restrictions)
3. DISCOVER   -> Probe target for baseline response headers
4. EVALUATE   -> Run per-dimension checks based on --scope
5. SCORE      -> Calculate per-dimension and overall scores
6. REPORT     -> Generate SARIF 2.1.0 + Markdown report
```

## Evaluation Dimensions

| Dimension | ASVS Ref | Key Checks |
|-----------|----------|------------|
| Rate Limiting | V4.3 | `X-RateLimit-*` headers, 429 responses, `Retry-After`, burst handling |
| WAF Rules | V5.1 | SQL injection blocking, XSS blocking, path traversal blocking |
| Bot Protection | V13.1 | CAPTCHA presence, bot detection headers, challenge pages |
| Account Lockout | V2.2 | Max login attempts, lockout duration, progressive delay |
| Brute Force | V2.2, V11.1 | Auth rate limiting, timing attack mitigation, enumeration protection |
| CSP Enforcement | V14.4 | CSP header, directives completeness, `unsafe-inline`/`unsafe-eval` |
| Permissions Policy | V14.4 | Feature restrictions (geolocation, camera, microphone, payment) |

## Scoring

**Per-dimension:** `score = (passedChecks / totalChecks) * 100`

| Score | Status |
|-------|--------|
| 80-100 | PROTECTED |
| 40-79 | PARTIAL |
| 0-39 | UNPROTECTED |

**Overall:** `score = max(0, 100 - sum(severityWeight * count))`

Weights: CRITICAL=10, HIGH=5, MEDIUM=2, LOW=1

| Score | Grade |
|-------|-------|
| 90-100 | A |
| 80-89 | B |
| 70-79 | C |
| 60-69 | D |
| 0-59 | F |

## Output

- SARIF: `results/security/runtime-protection-{timestamp}.sarif.json`
- Report: `results/security/runtime-protection-{timestamp}-report.md`

## Error Handling

| Scenario | Action |
|----------|--------|
| Target unreachable | Abort with error, no score |
| Aggressive in production | Downgrade to passive with warning |
| Login endpoint missing for account-lockout | Skip dimension |
| Partial failure | Complete other dimensions, mark failed as SKIPPED |
