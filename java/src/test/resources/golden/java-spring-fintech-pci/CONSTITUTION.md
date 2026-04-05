# Constitution -- my-spring-fintech-pci

> This document defines non-negotiable invariants, security constraints, and architecture boundaries
> for the my-spring-fintech-pci project. All code, reviews, and automated checks MUST enforce these rules.

## Invariants

| ID | Rule | Enforcement |
|----|------|-------------|
| RULE-SEC-001 | All user input MUST be validated before processing. No raw input reaches domain logic. | Input validation at adapter boundary; reject at gate. |
| RULE-SEC-002 | Secrets (API keys, passwords, tokens) MUST NOT appear in source code, logs, or error messages. | Static analysis scan; runtime log redaction. |
| RULE-SEC-003 | All sensitive data at rest MUST be encrypted using AES-256 or equivalent. | Encryption layer in outbound adapter; key rotation policy. |
| RULE-ARCH-001 | Domain layer MUST have zero external library imports. Only standard library and own domain packages allowed. | ArchUnit test or equivalent; CI gate. |
| RULE-ARCH-002 | Dependencies point inward: adapter -> application -> domain. Domain never imports adapter or framework code. | Package dependency analysis in CI. |
| RULE-ARCH-003 | Every outbound I/O operation MUST go through a port interface defined in the domain layer. | Code review checklist; ArchUnit test. |

| RULE-SEC-004 | Cardholder data (PAN, CVV, expiry) MUST NOT be stored after authorization. | PCI-DSS Req 3.2; automated data scan. |
| RULE-SEC-005 | All transmission of cardholder data MUST use TLS 1.2 or higher. | PCI-DSS Req 4.1; TLS configuration audit. |


## Security Constraints

| CWE ID | Description | Prohibited | Correct |
|--------|-------------|------------|---------|
| CWE-89 | SQL Injection | `"SELECT * FROM users WHERE id = " + userId` | Use parameterized queries: `PreparedStatement` with `?` placeholders or ORM named parameters. |
| CWE-312 | Cleartext Storage of Sensitive Information | `log.info("Card number: " + cardNumber)` | Never log sensitive fields. Use masking: `log.info("Card: {}", mask(cardNumber))` where mask returns `****1234`. |
| CWE-79 | Cross-Site Scripting (XSS) | `response.write("<div>" + userInput + "</div>")` | Escape all output: use context-aware encoding (HTML entity encoding for HTML context, JS encoding for script context). |
| CWE-798 | Hard-coded Credentials | `String apiKey = "sk-live-abc123"` | Load from environment variable or secrets manager: `System.getenv("API_KEY")` or vault integration. |
| CWE-327 | Use of Broken Crypto | `MessageDigest.getInstance("MD5")` | Use strong algorithms: `MessageDigest.getInstance("SHA-256")` or `bcrypt` for passwords. |

| CWE-311 | Missing Encryption of Sensitive Data | `db.store("pan", cardNumber)` | Encrypt before storage: `db.store("pan", encrypt(cardNumber, aesKey))` with AES-256-GCM. |
| CWE-532 | Insertion of Sensitive Information into Log File | `logger.debug("Processing payment for card {}", pan)` | Redact: `logger.debug("Processing payment for card {}", mask(pan))` showing only last 4 digits. |


## Architecture Boundaries

| Layer | Can Import | Cannot Import |
|-------|-----------|---------------|
| domain | Standard library, own `domain.*` packages | adapter, application, framework, serialization |
| application | `domain.*` | `adapter.*`, framework annotations |
| adapter.inbound | `application.*`, `domain.port.*` | `adapter.outbound.*` |
| adapter.outbound | `domain.port.*`, `domain.model.*` | `adapter.inbound.*` |
| config | All layers (wiring only) | Must not contain business logic |

## Naming Conventions

| Element | Pattern | Example |
|---------|---------|---------|
| Entity | `PascalCase` noun | `PaymentTransaction` |
| Value Object | `PascalCase` noun | `Money`, `CardToken` |
| Port (inbound) | verb-based interface | `ProcessPaymentPort` |
| Port (outbound) | verb-based interface | `SaveTransactionPort` |
| Adapter | `{Protocol}{Role}Adapter` | `RestPaymentAdapter` |
| Use Case | `{Verb}{Noun}UseCase` | `ProcessPaymentUseCase` |
| DTO | `{Name}{Request\|Response}` | `PaymentRequest` |
| Test | `{ClassUnderTest}Test` | `PaymentTransactionTest` |

## Compliance Requirements


### PCI-DSS Requirements

| PCI Requirement | Component | Implementation |
|----------------|-----------|----------------|
| Req 3.2 | Data Storage | Do not store CVV, full track data, or PIN after authorization. |
| Req 3.4 | PAN Display | Mask PAN: show only first 6 and last 4 digits. |
| Req 4.1 | Transmission | Enforce TLS 1.2+ for all network communication. |
| Req 6.5 | Coding | Address OWASP Top 10 in code reviews. |
| Req 8.2 | Authentication | Enforce MFA for administrative access. |
| Req 10.2 | Audit Trail | Log all access to cardholder data with timestamp, user, and action. |
| Req 11.3 | Penetration Testing | Schedule quarterly internal and annual external pen tests. |

