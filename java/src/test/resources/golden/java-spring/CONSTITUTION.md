# Constitution -- my-spring-service

> This document defines non-negotiable invariants, security constraints, and architecture boundaries
> for the my-spring-service project. All code, reviews, and automated checks MUST enforce these rules.

## Invariants

| ID | Rule | Enforcement |
|----|------|-------------|
| RULE-SEC-001 | All user input MUST be validated before processing. No raw input reaches domain logic. | Input validation at adapter boundary; reject at gate. |
| RULE-SEC-002 | Secrets (API keys, passwords, tokens) MUST NOT appear in source code, logs, or error messages. | Static analysis scan; runtime log redaction. |
| RULE-SEC-003 | All sensitive data at rest MUST be encrypted using AES-256 or equivalent. | Encryption layer in outbound adapter; key rotation policy. |
| RULE-ARCH-001 | Domain layer MUST have zero external library imports. Only standard library and own domain packages allowed. | ArchUnit test or equivalent; CI gate. |
| RULE-ARCH-002 | Dependencies point inward: adapter -> application -> domain. Domain never imports adapter or framework code. | Package dependency analysis in CI. |
| RULE-ARCH-003 | Every outbound I/O operation MUST go through a port interface defined in the domain layer. | Code review checklist; ArchUnit test. |


## Security Constraints

| CWE ID | Description | Prohibited | Correct |
|--------|-------------|------------|---------|
| CWE-89 | SQL Injection | `"SELECT * FROM users WHERE id = " + userId` | Use parameterized queries: `PreparedStatement` with `?` placeholders or ORM named parameters. |
| CWE-312 | Cleartext Storage of Sensitive Information | `log.info("Card number: " + cardNumber)` | Never log sensitive fields. Use masking: `log.info("Card: {}", mask(cardNumber))` where mask returns `****1234`. |
| CWE-79 | Cross-Site Scripting (XSS) | `response.write("<div>" + userInput + "</div>")` | Escape all output: use context-aware encoding (HTML entity encoding for HTML context, JS encoding for script context). |
| CWE-798 | Hard-coded Credentials | `String apiKey = "sk-live-abc123"` | Load from environment variable or secrets manager: `System.getenv("API_KEY")` or vault integration. |
| CWE-327 | Use of Broken Crypto | `MessageDigest.getInstance("MD5")` | Use strong algorithms: `MessageDigest.getInstance("SHA-256")` or `bcrypt` for passwords. |


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


### Active Compliance Frameworks


- lgpd


Specific requirements for these frameworks must be documented in the project ADRs and enforced through automated checks.

