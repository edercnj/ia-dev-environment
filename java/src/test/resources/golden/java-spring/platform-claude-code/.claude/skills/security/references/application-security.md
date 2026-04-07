# Global Behavior & Language Policy
- **Output Language**: English ONLY. (Mandatory for all responses and internal reasoning).
- **Token Optimization**: Eliminate all greetings, apologies, and conversational fluff. Start responses directly with technical information.
- **Priority**: Maintain 100% fidelity to the technical constraints defined in the original rules below.

# Application Security

## OWASP Top 10 (2021) — Implementation Guidelines

### A01:2021 — Broken Access Control

**Description:** Restrictions on authenticated users are not properly enforced, allowing attackers to access unauthorized functionality or data.

**Impact:** Confidentiality (HIGH), Integrity (HIGH), Availability (LOW)

**Associated CWEs:** CWE-200, CWE-201, CWE-352, CWE-285, CWE-862, CWE-863, CWE-22

**Detection Patterns:**
- Missing authorization checks on controller/handler methods
- Direct object reference without ownership verification
- CORS misconfiguration with wildcard origins on authenticated endpoints
- Missing CSRF token validation on state-changing operations

**SANS Top 25 Cross-Reference:** CWE-862 (Missing Authorization), CWE-863 (Incorrect Authorization), CWE-22 (Path Traversal), CWE-352 (CSRF)

- Deny by default: every endpoint requires explicit authorization
- Enforce server-side access control; never rely on client-side checks
- Disable directory listing; remove default credentials
- Rate-limit API and controller access to minimize automated attacks
- Invalidate JWT/session tokens on logout; use short-lived access tokens
- Implement RBAC or ABAC consistently across all endpoints

```{{LANGUAGE}}
// GOOD — Explicit authorization check before resource access
function getResource(userId, resourceId):
    resource = repository.find(resourceId)
    if resource.ownerId != userId AND NOT hasRole(userId, "ADMIN"):
        throw ForbiddenException("Access denied")
    return resource

// BAD — Relies on client-side filtering
function getResource(resourceId):
    return repository.find(resourceId)  // No ownership check
```

### A02:2021 — Cryptographic Failures

**Description:** Failures related to cryptography that lead to exposure of sensitive data or system compromise.

**Impact:** Confidentiality (HIGH), Integrity (MEDIUM), Availability (LOW)

**Associated CWEs:** CWE-259, CWE-327, CWE-331, CWE-261, CWE-916

**Detection Patterns:**
- Use of deprecated algorithms (MD5, SHA-1, DES, RC4)
- Hardcoded encryption keys or passwords in source code
- Missing TLS enforcement on data transmission
- Weak password hashing (plain SHA-256 without salt/iterations)

**SANS Top 25 Cross-Reference:** CWE-327 (Broken Crypto Algorithm), CWE-259 (Hard-coded Password), CWE-916 (Insufficient Password Hashing)

- Classify data processed, stored, or transmitted; apply controls per classification
- Never store sensitive data unnecessarily; discard it as soon as possible
- Encrypt all sensitive data at rest (AES-256-GCM minimum)
- Enforce TLS 1.3 for all data in transit; disable older protocols
- Use strong adaptive hashing for passwords (argon2id, bcrypt cost >= 12)
- Never use deprecated algorithms (MD5, SHA-1, DES, RC4)

```{{LANGUAGE}}
// GOOD — Strong adaptive hashing with salt
function hashPassword(password):
    return passwordEncoder.encode(password, algorithm="argon2id", memoryCost=65536, timeCost=3)

// BAD — Weak hashing without salt
function hashPassword(password):
    return sha256(password)  // No salt, no iterations, not adaptive
```

### A03:2021 — Injection

**Description:** User-supplied data is sent to an interpreter as part of a command or query without proper validation or escaping.

**Impact:** Confidentiality (HIGH), Integrity (HIGH), Availability (HIGH)

**Associated CWEs:** CWE-79, CWE-89, CWE-73, CWE-77, CWE-78, CWE-917

**Detection Patterns:**
- String concatenation in SQL queries, LDAP queries, or OS commands
- User input rendered in HTML without context-aware escaping
- Dynamic evaluation of user-controlled expressions (eval, template injection)
- Missing parameterized bindings in ORM queries

**SANS Top 25 Cross-Reference:** CWE-89 (SQL Injection), CWE-79 (XSS), CWE-78 (OS Command Injection), CWE-77 (Command Injection)

- Use parameterized queries / prepared statements for ALL database access
- Use ORM with parameterized bindings; never concatenate user input into queries
- Apply server-side input validation using allowlists
- Escape output based on context (HTML, JS, URL, CSS, LDAP)
- Limit query result sets with `LIMIT` to prevent mass data disclosure

```{{LANGUAGE}}
// GOOD — Parameterized query
query = "SELECT * FROM users WHERE email = ? AND status = ?"
result = db.execute(query, [email, status])

// BAD — String concatenation
query = "SELECT * FROM users WHERE email = '" + email + "'"
result = db.execute(query)
```

### A04:2021 — Insecure Design

**Description:** Fundamental design flaws that cannot be fixed by implementation alone; missing or ineffective security controls by design.

**Impact:** Confidentiality (MEDIUM), Integrity (HIGH), Availability (MEDIUM)

**Associated CWEs:** CWE-209, CWE-256, CWE-501, CWE-522

**Detection Patterns:**
- Absence of rate limiting on authentication or resource-intensive endpoints
- Missing threat model documentation for critical business flows
- No abuse case scenarios in user story acceptance criteria
- Single-layer validation without defense in depth

**SANS Top 25 Cross-Reference:** CWE-522 (Insufficiently Protected Credentials), CWE-256 (Plaintext Storage of Password)

- Use threat modeling for critical business flows (STRIDE, PASTA)
- Establish secure design patterns library for the team
- Implement rate limiting on resource-intensive operations
- Integrate security into user stories with abuse cases
- Use defense in depth: validate at every architectural layer

```{{LANGUAGE}}
// GOOD — Rate limiting on resource-intensive operation
function processOrder(userId, orderData):
    if NOT rateLimiter.tryAcquire(userId, limit=10, window="1m"):
        throw TooManyRequestsException("Rate limit exceeded")
    validate(orderData)
    return orderService.create(userId, orderData)

// BAD — No rate limiting, no validation layers
function processOrder(orderData):
    return orderService.create(orderData)  // No rate limit, no user binding
```

### A05:2021 — Security Misconfiguration

**Description:** Missing security hardening, unnecessary features enabled, default accounts, overly permissive configurations.

**Impact:** Confidentiality (MEDIUM), Integrity (MEDIUM), Availability (LOW)

**Associated CWEs:** CWE-16, CWE-611, CWE-1004, CWE-942

**Detection Patterns:**
- Default credentials or accounts still active
- Verbose error messages exposing stack traces to end users
- XML external entity (XXE) processing enabled
- Unnecessary HTTP methods enabled (TRACE, OPTIONS without CORS)
- Missing security headers in HTTP responses

**SANS Top 25 Cross-Reference:** CWE-611 (XXE), CWE-16 (Configuration)

- Automate hardened environment configuration (IaC)
- Remove unused features, frameworks, components, and documentation
- Review and update all security configurations as part of patch management
- Segment application architecture; isolate tenants and components
- Send security directives to clients via security headers (see below)
- Disable XML external entity (XXE) processing in all XML parsers

```{{LANGUAGE}}
// GOOD — Hardened XML parser configuration
function parseXml(input):
    parser = XmlParser.newInstance()
    parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    parser.setFeature("http://xml.org/sax/features/external-general-entities", false)
    parser.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
    return parser.parse(input)

// BAD — Default XML parser allows XXE
function parseXml(input):
    return XmlParser.newInstance().parse(input)  // XXE enabled by default
```

### A06:2021 — Vulnerable and Outdated Components

**Description:** Using components with known vulnerabilities, unsupported libraries, or unpatched software.

**Impact:** Confidentiality (HIGH), Integrity (HIGH), Availability (HIGH)

**Associated CWEs:** CWE-1104, CWE-937

**Detection Patterns:**
- Dependencies with known CVEs detected by SCA tools
- Outdated libraries without active maintenance
- Missing lock files or unpinned dependency versions
- Transitive dependencies with known vulnerabilities

**SANS Top 25 Cross-Reference:** CWE-1104 (Use of Unmaintained Third-Party Components)

- Remove unused dependencies, features, and components
- Continuously inventory client-side and server-side component versions
- Monitor CVE databases (NVD, GitHub Advisories) for vulnerabilities
- Obtain components only from official sources over secure links
- Pin exact dependency versions; use lock files
- See **Dependency Security** section below for CVE policy

### A07:2021 — Identification and Authentication Failures

**Description:** Weaknesses in authentication mechanisms that allow attackers to assume other users' identities.

**Impact:** Confidentiality (HIGH), Integrity (HIGH), Availability (LOW)

**Associated CWEs:** CWE-255, CWE-259, CWE-287, CWE-288, CWE-290, CWE-307, CWE-798

**Detection Patterns:**
- Missing brute-force protection on login endpoints
- Default or well-known credentials in deployment configurations
- Session tokens with insufficient entropy
- Missing multi-factor authentication on sensitive operations
- Password storage without adaptive hashing

**SANS Top 25 Cross-Reference:** CWE-287 (Improper Authentication), CWE-798 (Hard-coded Credentials), CWE-307 (Brute Force), CWE-290 (Spoofing)

- Implement multi-factor authentication where possible
- Never ship or deploy default credentials
- Implement weak-password checks against top 10,000 breached passwords list
- Harden all authentication paths: login, registration, password reset
- Limit failed authentication attempts with exponential backoff and account lockout
- Use secure session management: server-side, high-entropy, invalidate on logout

```{{LANGUAGE}}
// GOOD — Rate-limited authentication with lockout
function authenticate(username, password):
    if rateLimiter.isBlocked(username):
        throw TooManyRequestsException("Account temporarily locked")
    user = userRepository.findByUsername(username)
    if NOT passwordEncoder.matches(password, user.hashedPassword):
        rateLimiter.recordFailure(username)
        throw UnauthorizedException("Invalid credentials")
    rateLimiter.reset(username)
    return sessionManager.createSession(user)

// BAD — No rate limiting, no account lockout
function authenticate(username, password):
    user = userRepository.findByUsername(username)
    if password == user.password:  // Plain text comparison
        return createToken(user)
    throw UnauthorizedException("Invalid credentials")
```

### A08:2021 — Software and Data Integrity Failures

**Description:** Code and infrastructure that does not protect against integrity violations, including insecure CI/CD pipelines and unsigned updates.

**Impact:** Confidentiality (LOW), Integrity (HIGH), Availability (HIGH)

**Associated CWEs:** CWE-345, CWE-353, CWE-426, CWE-494, CWE-502, CWE-565

**Detection Patterns:**
- Deserialization of untrusted data without validation
- Missing integrity checks on software updates or plugins
- CI/CD pipelines without proper access controls or segregation
- Unsigned artifacts deployed to production

**SANS Top 25 Cross-Reference:** CWE-502 (Deserialization of Untrusted Data), CWE-494 (Download Without Integrity Check), CWE-345 (Insufficient Verification of Data Authenticity)

- Verify software and data integrity via digital signatures
- Ensure CI/CD pipelines have proper segregation, configuration, and access control
- Do not send unsigned or unencrypted serialized data to untrusted clients
- Use dependency integrity verification (checksums, signed packages)
- Implement code review process for all changes to code and infrastructure

```{{LANGUAGE}}
// GOOD — Safe deserialization with type allowlist
function deserialize(data, expectedType):
    deserializer = SafeDeserializer.create()
    deserializer.setAllowedTypes([expectedType])
    deserializer.setMaxDepth(10)
    return deserializer.deserialize(data)

// BAD — Unrestricted deserialization of untrusted input
function deserialize(data):
    return ObjectDeserializer.fromBytes(data)  // Any type accepted
```

### A09:2021 — Security Logging and Monitoring Failures

**Description:** Insufficient logging, detection, monitoring, and active response that allows attackers to operate undetected.

**Impact:** Confidentiality (HIGH), Integrity (LOW), Availability (LOW)

**Associated CWEs:** CWE-117, CWE-223, CWE-532, CWE-778

**Detection Patterns:**
- Missing audit logs for authentication events
- Sensitive data (passwords, tokens, PII) present in log output
- Log injection vulnerabilities (unsanitized user input in log messages)
- No alerting thresholds for anomalous activity patterns

**SANS Top 25 Cross-Reference:** CWE-778 (Insufficient Logging), CWE-532 (Information Exposure Through Log Files), CWE-117 (Log Injection)

- Log all authentication events (success and failure)
- Log all access control failures
- Log all server-side input validation failures
- Ensure logs have enough context for forensic analysis
- Ensure logs are not vulnerable to injection attacks
- Implement alerting thresholds for suspicious activity patterns
- Never log sensitive data (passwords, tokens, PII) — see Rule 07

```{{LANGUAGE}}
// GOOD — Structured logging with sanitized input
function logAuthEvent(username, success, ipAddress):
    sanitizedUser = sanitizeForLog(username)
    logger.info("authentication_event",
        Map.of("user", sanitizedUser, "success", success,
               "ip", ipAddress, "timestamp", Instant.now()))

// BAD — User input directly in log message, sensitive data exposed
function logAuthEvent(username, password, success):
    logger.info("Login: user=" + username + " pass=" + password + " ok=" + success)
```

### A10:2021 — Server-Side Request Forgery (SSRF)

**Description:** Web application fetches a remote resource without validating the user-supplied URL, allowing attackers to reach internal services.

**Impact:** Confidentiality (HIGH), Integrity (MEDIUM), Availability (LOW)

**Associated CWEs:** CWE-918

**Detection Patterns:**
- User-supplied URLs passed directly to HTTP client without validation
- Missing allowlist for permitted URL schemes and domains
- No blocking of private/internal IP ranges in outbound requests
- HTTP redirects followed automatically on server-side requests

**SANS Top 25 Cross-Reference:** CWE-918 (Server-Side Request Forgery)

- Validate and sanitize ALL client-supplied URLs
- Enforce allowlist of permitted URL schemes (https only where possible)
- Block requests to private/internal IP ranges (10.x, 172.16.x, 192.168.x, 127.x, 169.254.x)
- Disable HTTP redirects for server-side requests
- Do not return raw responses from server-side requests to the client

```{{LANGUAGE}}
// GOOD — URL validation with allowlist
function fetchExternalResource(url):
    parsed = parseUrl(url)
    if parsed.scheme NOT IN ["https"]:
        throw ValidationException("Only HTTPS allowed")
    if isPrivateIp(parsed.host):
        throw ValidationException("Internal addresses not allowed")
    if parsed.host NOT IN allowedDomains:
        throw ValidationException("Domain not in allowlist")
    return httpClient.get(url, followRedirects=false, timeout=5s)

// BAD — No URL validation, follows redirects
function fetchExternalResource(url):
    return httpClient.get(url)  // SSRF: any URL, any scheme, follows redirects
```

## OWASP-to-SANS Top 25 Cross-Reference Table

| OWASP Category | SANS Top 25 CWEs | CWE IDs |
|----------------|-------------------|---------|
| A01: Broken Access Control | Missing Authorization, Incorrect Authorization, Path Traversal, CSRF | CWE-862, CWE-863, CWE-22, CWE-352 |
| A02: Cryptographic Failures | Broken Crypto, Hard-coded Password | CWE-327, CWE-259, CWE-916 |
| A03: Injection | SQL Injection, XSS, OS Command Injection, Command Injection | CWE-89, CWE-79, CWE-78, CWE-77 |
| A04: Insecure Design | Insufficiently Protected Credentials | CWE-522, CWE-256 |
| A05: Security Misconfiguration | XXE | CWE-611 |
| A06: Vulnerable Components | Unmaintained Third-Party Components | CWE-1104 |
| A07: Auth Failures | Improper Authentication, Hard-coded Credentials, Brute Force | CWE-287, CWE-798, CWE-307, CWE-290 |
| A08: Integrity Failures | Deserialization, Download Without Integrity Check | CWE-502, CWE-494, CWE-345 |
| A09: Logging Failures | Insufficient Logging, Log Info Exposure, Log Injection | CWE-778, CWE-532, CWE-117 |
| A10: SSRF | Server-Side Request Forgery | CWE-918 |

**SANS Top 25 additional CWEs mapped:** CWE-20 (Improper Input Validation, spans A01-A03), CWE-125 (Out-of-bounds Read, language-dependent), CWE-787 (Out-of-bounds Write, language-dependent), CWE-416 (Use After Free, language-dependent), CWE-476 (NULL Pointer Dereference, language-dependent), CWE-190 (Integer Overflow, spans A03-A04)

## Tool Recommendation by Vulnerability Type

| Vulnerability Type | Recommended Tool | Tool Type | Effectiveness |
|--------------------|-----------------|-----------|---------------|
| Injection (SQLi, XSS, CMD) | SAST + DAST | Preventive + Detective | High |
| Auth/Access Control | DAST + Manual Review | Detective | Medium-High |
| Cryptographic Failures | SAST | Preventive | High |
| Insecure Design | Threat Model + Manual Review | Preventive | Medium |
| Misconfiguration | SAST + Hardening Eval | Detective | High |
| Vulnerable Components | SCA (x-dependency-audit) | Detective | High |
| Data Integrity | SAST + DAST | Preventive + Detective | Medium |
| Logging Failures | Manual Review + SAST | Detective | Medium |
| SSRF | DAST + SAST | Detective | Medium-High |
| Deserialization | SAST + Manual Review | Preventive | Medium-High |

## Security Headers

All HTTP services MUST include the following response headers:

| Header | Value | Purpose |
|--------|-------|---------|
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains; preload` | Force HTTPS for 1 year, include subdomains |
| `Content-Security-Policy` | Per application needs (strict) | Prevent XSS, clickjacking, data injection |
| `X-Content-Type-Options` | `nosniff` | Prevent MIME type sniffing |
| `X-Frame-Options` | `DENY` or `SAMEORIGIN` | Prevent clickjacking |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | Control referrer information leakage |
| `Permissions-Policy` | Disable unused features: `camera=(), microphone=(), geolocation=()` | Restrict browser feature access |
| `Cache-Control` | `no-store` for sensitive endpoints; `public, max-age=N` for static | Prevent sensitive data caching |
| `X-Request-ID` | UUID per request (generated or forwarded) | Distributed tracing correlation |

### CSP Configuration Example

```
# API-only services (strictest)
Content-Security-Policy: default-src 'none'; frame-ancestors 'none'

# Web applications (adjust per requirements)
Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; font-src 'self'; connect-src 'self' https://api.example.com; frame-ancestors 'none'; base-uri 'self'; form-action 'self'
```

### Implementation Pattern

```{{LANGUAGE}}
// Middleware / Filter pattern for security headers
function securityHeadersMiddleware(request, response, next):
    response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload")
    response.setHeader("X-Content-Type-Options", "nosniff")
    response.setHeader("X-Frame-Options", "DENY")
    response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin")
    response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()")
    response.setHeader("X-Request-ID", request.getHeader("X-Request-ID") OR generateUUID())
    next(request, response)
```

## Secrets Management

### Hierarchy (least to most secure)

| Level | Method | When Acceptable |
|-------|--------|----------------|
| 1 | Environment variables | Local development only |
| 2 | Encrypted config files | CI/CD pipelines with encrypted storage |
| 3 | Platform secrets (K8s Secrets, AWS Parameter Store) | Staging environments |
| 4 | Secrets vault (HashiCorp Vault, AWS Secrets Manager) | **Production (preferred)** |

### Mandatory Rules

- **Never in source:** No secrets in code, configuration files, or container images
- **Environment variables:** Acceptable minimum; vault integration is preferred
- **Vault preferred:** Use centralized secrets management in production
- **Rotation:** Implement automated rotation policies per secret type
- **Short-lived credentials:** Prefer dynamic secrets with TTL (database credentials, API tokens)
- **Per-environment secrets:** Never share secrets across environments (dev, staging, prod)
- **Access audit:** Log all secret access; alert on anomalous patterns

### Rotation Policy

| Secret Type | Rotation Frequency | Method |
|-------------|-------------------|--------|
| Database passwords | 90 days | Automated via vault |
| API keys | 180 days | Automated via vault |
| TLS certificates | 90 days (or auto via ACME) | Automated via cert-manager |
| Encryption keys | 365 days | Automated via KMS |
| Service account tokens | 24 hours (dynamic) | Vault dynamic secrets |

## Input Validation Framework

### Principles

- **Validate at boundary:** All validation occurs at the entry point (API handler, message consumer, event listener)
- **Allowlist over denylist:** Define what IS allowed, not what is NOT
- **Type/range/format:** Validate data type, acceptable range, and format
- **Sanitize for output context:** Escape data based on where it will be rendered (HTML, SQL, JS, URL)
- **Max length on everything:** Every string field has an explicit maximum length
- **Content-type validation:** Validate file uploads by content inspection, not just extension

### Validation Layers

| Layer | Validates | Example |
|-------|-----------|---------|
| Transport | Size, encoding, content-type header | Max body 1MB, UTF-8 only |
| Schema | Structure, required fields, types | JSON Schema, OpenAPI, Protobuf |
| Business | Domain constraints, cross-field rules | Amount > 0, startDate < endDate |
| Output | Context-appropriate escaping | HTML encoding, SQL parameterization |

### File Upload Validation

```{{LANGUAGE}}
function validateUpload(file):
    // 1. Check file size
    if file.size > MAX_UPLOAD_SIZE:
        reject("File exceeds maximum size")

    // 2. Validate content-type by inspecting magic bytes, not extension
    detectedType = detectMimeType(file.content)
    if detectedType NOT IN ALLOWED_MIME_TYPES:
        reject("File type not allowed")

    // 3. Validate extension matches content
    if NOT extensionMatchesMime(file.extension, detectedType):
        reject("Extension does not match content")

    // 4. Strip metadata (EXIF, etc.)
    sanitized = stripMetadata(file)

    // 5. Store with generated name, never use original filename
    storedName = generateUUID() + "." + mimeToExtension(detectedType)
    storage.store(sanitized, storedName)
```

## Session Management

### Secure Session Practices

- Generate session IDs with cryptographically secure random number generator (minimum 128-bit entropy)
- Set session cookies with `Secure`, `HttpOnly`, and `SameSite=Strict` attributes
- Implement absolute session timeout (e.g., 8 hours) and idle timeout (e.g., 30 minutes)
- Regenerate session ID after authentication to prevent session fixation
- Invalidate all sessions on password change

```{{LANGUAGE}}
// GOOD — Secure session configuration
function createSession(user):
    sessionId = cryptoRandom.generateBytes(32).toHex()
    session = Session.create(
        id=sessionId,
        userId=user.id,
        createdAt=Instant.now(),
        absoluteTimeout=Duration.ofHours(8),
        idleTimeout=Duration.ofMinutes(30))
    sessionStore.save(session)
    cookie = Cookie.create("SESSION_ID", sessionId)
    cookie.setSecure(true)
    cookie.setHttpOnly(true)
    cookie.setSameSite("Strict")
    cookie.setPath("/")
    return cookie

// BAD — Predictable session ID, no security attributes
function createSession(user):
    sessionId = String.valueOf(user.id) + System.currentTimeMillis()
    return Cookie.create("sid", sessionId)  // Predictable, no flags
```

## Output Encoding

### Context-Aware Encoding

| Context | Encoding Method | Example |
|---------|----------------|---------|
| HTML body | HTML entity encoding | `<` becomes `&lt;` |
| HTML attribute | Attribute encoding | `"` becomes `&quot;` |
| JavaScript | JavaScript escaping | `'` becomes `\'` |
| URL parameter | Percent encoding | ` ` becomes `%20` |
| CSS | CSS hex encoding | `(` becomes `\28` |
| JSON | RFC 8259 escaping | `"` becomes `\"` |

- Apply encoding at the point of output, not at input
- Use framework-provided encoding functions; never build custom escapers
- Double-encoding is a bug: encode once, at the correct layer

## Dependency Security

### Lock Files

- **Always commit lock files** (`package-lock.json`, `pom.xml` with versions, `go.sum`, `Cargo.lock`, `poetry.lock`)
- Never use floating version ranges in production (`^`, `~`, `*`)
- Pin exact versions for all direct dependencies
- Review lock file diffs in code review

### Automated Scanning

- Run dependency vulnerability scanning in CI/CD (Snyk, Dependabot, Trivy, OWASP Dependency-Check)
- Scan on every pull request and on a daily schedule
- Scan both direct and transitive dependencies
- Scan container images for OS-level vulnerabilities

### CVE Response Policy

| Severity | SLA | Action |
|----------|-----|--------|
| **Critical (CVSS >= 9.0)** | Block merge / deploy immediately | Patch or mitigate within 24 hours |
| **High (CVSS 7.0-8.9)** | Block merge / deploy | Patch within 7 days |
| **Medium (CVSS 4.0-6.9)** | Warning, do not block | Patch within 30 days |
| **Low (CVSS < 4.0)** | Informational | Patch within 90 days |

### Private Registry

- Use a private registry / proxy for dependency resolution (Nexus, Artifactory, GitHub Packages)
- Cache approved versions to prevent supply-chain attacks
- Verify package signatures where available
- Block publication of internal packages to public registries

## Anti-Patterns (FORBIDDEN)

- Disable security headers "for convenience"
- Store secrets in source code, environment files committed to VCS, or container images
- Accept user input without validation at the boundary
- Use denylist-based validation instead of allowlist
- Trust file extensions for upload validation
- Use floating dependency versions in production
- Ignore CVE alerts or suppress vulnerability scanner findings
- Skip security headers for internal/microservice APIs
- Hardcode CORS `Access-Control-Allow-Origin: *` on authenticated endpoints
