# x-dast-scan

> Dynamic Application Security Testing -- tests the running application for XSS, injection, misconfiguration, and information disclosure vulnerabilities using OWASP ZAP or Nuclei.

| | |
|---|---|
| **Category** | Conditional |
| **Condition** | `security.scanning.dast = true` |
| **Invocation** | `/x-dast-scan --target <URL> [--env local\|dev\|homolog\|prod] [--mode passive\|active\|full]` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## When Available

This skill is generated when `security.scanning.dast = true` in the project configuration.

## What It Does

Orchestrates Dynamic Application Security Testing against a running application, complementing SAST by testing from outside-in. Simulates real attacks to detect runtime vulnerabilities that static analysis cannot find: missing security headers, insecure cookies, CORS misconfiguration, injection flaws, and information disclosure. Automatically selects OWASP ZAP, Nuclei, or nikto based on tool availability, and supports OpenAPI-driven scanning with environment-based restrictions.

## Usage

```
/x-dast-scan --target http://localhost:8080
/x-dast-scan --target https://app.staging.example.com --env homolog --mode passive
/x-dast-scan --target http://localhost:8080 --openapi docs/openapi.yaml
```

## See Also

- [x-sast-scan](../x-sast-scan/) -- Static code analysis for vulnerabilities
- [x-pentest](../x-pentest/) -- Multi-phase penetration test orchestrator
- [run-smoke-api](../run-smoke-api/) -- REST API smoke tests against deployed environments
