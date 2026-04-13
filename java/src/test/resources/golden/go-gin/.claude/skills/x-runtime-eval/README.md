# x-runtime-eval

> Evaluates runtime protection controls: rate limiting, WAF rules, bot protection, DDoS mitigation, account lockout, brute force protection, CSP enforcement, and permissions policy. Uses SARIF output and ASVS compliance mapping.

| | |
|---|---|
| **Category** | Security |
| **Invocation** | `/x-runtime-eval --target <url> [--scope all\|rate-limit\|waf\|bot-protection\|account-lockout\|brute-force\|csp\|permissions] [--intensity passive\|moderate\|aggressive] [--login-endpoint /path]` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Evaluates active runtime defense mechanisms of a running application across 7 dimensions: rate limiting, WAF rules (OWASP CRS), bot protection, account lockout, brute force mitigation, CSP enforcement, and permissions policy. Supports three intensity levels (passive observation, moderate non-destructive testing, aggressive volume testing for dev only) and produces ASVS-mapped findings with SARIF 2.1.0 output.

## Usage

```
/x-runtime-eval --target https://app.example.com
/x-runtime-eval --target https://app.example.com --scope rate-limit
/x-runtime-eval --target https://app.example.com --intensity passive
/x-runtime-eval --target https://app.example.com --scope account-lockout --login-endpoint /api/auth/login
```

## Workflow

1. Validate parameters and check target reachability
2. Resolve effective intensity (auto-downgrade aggressive to passive in production)
3. Probe target for baseline response headers
4. Evaluate per-dimension checks based on scope and intensity
5. Calculate per-dimension and overall scores with grade mapping
6. Generate SARIF 2.1.0 and Markdown reports

## Outputs

| Artifact | Path |
|----------|------|
| SARIF report | `results/security/runtime-protection-{timestamp}.sarif.json` |
| Markdown report | `results/security/runtime-protection-{timestamp}-report.md` |

## See Also

- [x-hardening-eval](../x-hardening-eval/) -- Static hardening posture evaluation (headers, TLS, CORS, cookies)
- [x-owasp-scan](../x-owasp-scan/) -- OWASP Top 10 verification with ASVS mapping
- [x-security-dashboard](../x-security-dashboard/) -- Aggregated security posture view from all scanning skills
