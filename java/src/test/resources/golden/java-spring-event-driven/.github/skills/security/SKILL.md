---
name: security
description: >
  Knowledge Pack: Security -- OWASP Top 10, security headers, secrets
  management, input validation, cryptography (TLS, hashing, key management),
  and pentest readiness checklist for my-spring-event-driven.
---

# Knowledge Pack: Security

## Summary

Security conventions for my-spring-event-driven using java 21 with spring-boot.

### OWASP Top 10 Prevention

- **Injection**: Parameterized queries, input validation, never concatenate user input
- **Broken Auth**: Strong password policies, MFA, secure session management
- **Sensitive Data**: Encrypt at rest and in transit, minimize data retention
- **XXE**: Disable external entity processing in XML parsers
- **Broken Access**: Deny by default, validate permissions on every request
- **Misconfig**: Harden defaults, disable debug endpoints in production
- **XSS**: Output encoding, Content-Security-Policy headers
- **Deserialization**: Validate types, use allowlists, avoid native serialization
- **Vulnerable Components**: Automated dependency scanning, update policy
- **Logging Gaps**: Log security events, monitor for anomalies

### Security Headers

- `Strict-Transport-Security`, `Content-Security-Policy`, `X-Content-Type-Options`
- `X-Frame-Options`, `Referrer-Policy`, `Permissions-Policy`

### Secrets Management

- Never hardcode secrets in source code or configuration files
- Use environment variables or secret management services
- Rotate secrets on a defined schedule

### Cryptography

- TLS 1.2+ for all network communication
- Strong hashing: bcrypt/scrypt/Argon2 for passwords, SHA-256+ for integrity
- Key management: separate encryption keys from data, rotate periodically

## References

- [OWASP Top 10](https://owasp.org/www-project-top-ten/) -- Common web application risks and prevention guidance
- [OWASP Cheat Sheet Series](https://cheatsheetseries.owasp.org/) -- Practical security hardening checklists
- [MDN Web Docs: Security](https://developer.mozilla.org/docs/Web/Security) -- Web platform security and secure headers
- [NIST SP 800-63](https://pages.nist.gov/800-63-3/) -- Digital identity and authentication best practices
