---
name: x-review-gateway
description: >
  Review API gateway configuration for best practices including routing rules,
  authentication, rate limiting, CORS, security headers, TLS configuration,
  and observability integration. Use when reviewing gateway config files.
---

# Skill: API Gateway Review

## Description

Reviews API gateway configuration against best practices: routing rules, authentication, rate limiting, CORS, security headers, TLS configuration, and observability integration.

**Condition**: This skill applies when the project uses an API gateway.

## Prerequisites

- API gateway configuration files exist in the codebase
- Gateway type identified (Kong, Istio, AWS APIGW, Traefik, etc.)

## Knowledge Pack References

Before reviewing, read the gateway conventions:
- `.claude/skills/infrastructure/references/infrastructure-principles.md` -- gateway patterns, security, observability

## Execution Flow

1. **Identify gateway type** -- Determine which gateway is configured
2. **Discover configuration files** -- Scan for gateway config files
3. **Verify routing rules** -- Check route definitions, path matching, upstream targets
4. **Check authentication** -- Validate auth plugins/middleware (JWT, OAuth2, API keys)
5. **Check rate limiting** -- Verify rate limit configuration per route/consumer
6. **Check CORS** -- Validate allowed origins, methods, headers
7. **Check security headers** -- Verify HSTS, CSP, X-Frame-Options, etc.
8. **Check TLS configuration** -- Validate certificate management, TLS versions
9. **Validate observability** -- Check access logs, tracing, metrics integration
10. **Generate report** -- Summarize findings as checklist

## Review Checklist

- [ ] Routing rules correctly map paths to upstream services
- [ ] Authentication configured on all non-public routes
- [ ] Rate limiting configured per route or consumer
- [ ] CORS properly scoped (not wildcard in production)
- [ ] Security headers present (HSTS, CSP, X-Frame-Options)
- [ ] TLS 1.2+ enforced, weak ciphers disabled
- [ ] Access logs enabled with structured format
- [ ] Tracing headers propagated through gateway
- [ ] Health check endpoints excluded from auth
- [ ] Circuit breaker configured for upstream services

## Output Format

```
## Gateway Review -- [Change Description]

### Gateway Type: [Kong/Istio/AWS APIGW/Traefik]

### Findings
1. [Finding with file, line, remediation]

### Verdict: APPROVE / REQUEST CHANGES
```

## Detailed References

For in-depth guidance on gateway configuration, consult:
- `.claude/skills/x-review-gateway/SKILL.md`
- `.claude/skills/infrastructure/SKILL.md`
