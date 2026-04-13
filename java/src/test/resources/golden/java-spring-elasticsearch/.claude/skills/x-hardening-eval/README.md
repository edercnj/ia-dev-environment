# x-hardening-eval

> Evaluates application hardening posture against CIS and OWASP benchmarks: HTTP security headers, TLS configuration, CORS policy, cookie security, error handling, input limits, and information disclosure. Produces SARIF output with weighted scoring.

| | |
|---|---|
| **Category** | Security |
| **Invocation** | `/x-hardening-eval --target <url> [--scope all\|headers\|tls\|cors\|cookies\|errors\|limits\|disclosure] [--benchmark cis\|owasp] [--level L1\|L2\|L3]` |
| **Reads** | `skills/security/references/security-principles.md`, `skills/security/references/application-security.md`, `skills/security/references/cryptography.md` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Evaluates the defensive posture of a running application against CIS and OWASP benchmarks across 7 weighted dimensions. Probes the target URL for HTTP security headers, TLS configuration, CORS behavior, cookie attributes, error handling patterns, input limits, and information disclosure. Produces per-dimension scores, an overall weighted grade, and actionable fix recommendations in SARIF 2.1.0 format.

## Usage

```
/x-hardening-eval --target https://app.example.com
/x-hardening-eval --target https://app.example.com --scope headers
/x-hardening-eval --target https://app.example.com --benchmark cis --level L2
```

## Workflow

1. Validate CLI parameters and target URL reachability
2. Load benchmark checks and ASVS level mapping (CIS or OWASP)
3. Probe target with HTTP requests to collect headers, TLS info, and CORS behavior
4. Evaluate each dimension against benchmark checks
5. Calculate weighted scores per dimension and overall grade
6. Generate SARIF 2.1.0 and Markdown reports

## Outputs

| Artifact | Path |
|----------|------|
| SARIF report | `results/security/hardening-eval-YYYY-MM-DD.sarif.json` |
| Markdown report | `results/security/hardening-eval-YYYY-MM-DD.md` |

## See Also

- [x-runtime-eval](../x-runtime-eval/) -- Runtime protection controls (rate limiting, WAF, bot protection)
- [x-owasp-scan](../x-owasp-scan/) -- OWASP Top 10 verification with ASVS mapping
- [x-security-dashboard](../x-security-dashboard/) -- Aggregated security posture view from all scanning skills
