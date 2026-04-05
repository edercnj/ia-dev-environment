# Rule 06 — Security Baseline

> **Full reference:** Read `skills/security/SKILL.md` for OWASP Top 10, cryptography, and pentest readiness.

## Secure Defaults (Non-Negotiable)

| Practice | Requirement |
|----------|-------------|
| Input deserialization | Explicit safe/strict mode (e.g., SafeConstructor, strict JSON parser) |
| String escaping | Full spec compliance (RFC 8259 for JSON, OWASP for HTML/XML) |
| Temp files/directories | Explicit restrictive permissions (owner-only: 700/600) |
| Path operations | Normalize + reject traversal (`..`, symlinks) before any I/O |
| Error messages | Never expose internal paths, stack traces, or class names to end users |

## Forbidden

- Deserializing untrusted input without explicit safe mode
- Partial escaping (escaping only some special characters)
- Following symlinks in file operations without explicit opt-in
- Hardcoded secrets, tokens, or credentials anywhere in source
- `Math.random()` / `rand()` for security-sensitive values (use cryptographic RNG)

## Defensive Coding

- All path inputs: canonicalize, then verify prefix against allowed base directory
- Filename sanitization: multi-pass until idempotent (single pass of `..` removal is insufficient)
- Temp directories: always set explicit permissions, always clean up in `finally` / `defer`

> Read `skills/security/SKILL.md` for input validation patterns, secrets management, and security headers.
