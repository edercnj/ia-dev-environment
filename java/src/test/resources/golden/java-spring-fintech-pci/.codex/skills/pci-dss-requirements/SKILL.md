---
name: pci-dss-requirements
description: "PCI-DSS v4.0 requirements mapped to code practices: 12 requirements with prohibited/correct examples and reviewer checklists."
user-invocable: false
allowed-tools:
  - Read
  - Grep
  - Glob
---

# Knowledge Pack: PCI-DSS v4.0 Requirements

## Purpose

Provides the 12 PCI-DSS v4.0 requirements mapped to concrete Java code practices. Each mappable requirement includes prohibited code patterns, correct implementations, and a code reviewer checklist. Requirements 9 and 12 are organizational and include explanatory notes.

## PCI-DSS v4.0 --- Requirement 1: Install and Maintain Network Security Controls

### O que o requisito exige

Network security controls (firewalls, security groups) must be installed, configured, and maintained to protect cardholder data environments. All inbound and outbound traffic must be restricted to only what is necessary.

### Verificacao em codigo Java

PROIBIDO:

```java
// Binding to all interfaces without restriction
ServerSocket server = new ServerSocket(8080,
    50, InetAddress.getByName("0.0.0.0"));

// No TLS configuration — plain HTTP
HttpServer.create(new InetSocketAddress(8080), 0);
```

CORRETO:

```java
// Bind only to specific interface
ServerSocket server = new ServerSocket(8080,
    50, InetAddress.getByName("127.0.0.1"));

// Enforce TLS with SSLContext
SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
sslContext.init(keyManagers, trustManagers, secureRandom);
HttpsServer httpsServer = HttpsServer.create(
    new InetSocketAddress(8443), 0);
httpsServer.setHttpsConfigurator(
    new HttpsConfigurator(sslContext));
```

### O que o code reviewer deve checar

- [ ] Server binds to specific interfaces, not 0.0.0.0 in production
- [ ] All external communication uses TLS 1.2 or higher
- [ ] No hardcoded ports or IP addresses (externalized config)

## PCI-DSS v4.0 --- Requirement 2: Apply Secure Configurations to All System Components

### O que o requisito exige

Default passwords, accounts, and insecure settings must be changed before deployment. System components must be hardened according to industry-accepted standards.

### Verificacao em codigo Java

PROIBIDO:

```java
// Default credentials in configuration
dataSource.setUsername("admin");
dataSource.setPassword("admin123");

// Debug mode enabled in production
app.setProperty("debug", "true");
app.setProperty("spring.jpa.show-sql", "true");
```

CORRETO:

```java
// Credentials from environment variables
dataSource.setUsername(
    System.getenv("DB_USERNAME"));
dataSource.setPassword(
    System.getenv("DB_PASSWORD"));

// Production-safe configuration
app.setProperty("debug", "false");
app.setProperty("spring.jpa.show-sql", "false");
```

### O que o code reviewer deve checar

- [ ] No default credentials in source code or configuration files
- [ ] Debug/verbose modes disabled in production profiles
- [ ] All configuration externalized via environment variables or secrets manager

## PCI-DSS v4.0 --- Requirement 3: Protect Stored Account Data

### O que o requisito exige

Stored account data must be protected. Primary Account Numbers (PAN) must be rendered unreadable anywhere they are stored using strong cryptography, truncation, or tokenization. Sensitive authentication data must not be stored after authorization.

### Verificacao em codigo Java

PROIBIDO:

```java
// Storing PAN in cleartext
String pan = "4111111111111111";
database.save("card_number", pan);

// Logging full PAN
logger.info("Processing card: {}", pan);
```

CORRETO:

```java
// Tokenize or encrypt PAN before storage
String token = tokenizationService.tokenize(pan);
database.save("card_token", token);

// Mask PAN in logs (show only last 4)
String masked = "****-****-****-"
    + pan.substring(pan.length() - 4);
logger.info("Processing card: {}", masked);
```

### O que o code reviewer deve checar

- [ ] PAN is never stored in cleartext in database or files
- [ ] PAN is masked in all log outputs (show max last 4 digits)
- [ ] Sensitive authentication data (CVV, PIN) is never stored post-auth
- [ ] Encryption keys are managed via KMS, not hardcoded

## PCI-DSS v4.0 --- Requirement 4: Protect Cardholder Data with Strong Cryptography During Transmission

### O que o requisito exige

Cardholder data must be protected with strong cryptography during transmission over open, public networks. Insecure protocols and cipher suites must not be used.

### Verificacao em codigo Java

PROIBIDO:

```java
// Using deprecated TLS versions
SSLContext ctx = SSLContext.getInstance("TLSv1.0");

// HTTP without TLS for sensitive data
URL url = new URL("http://payment-gateway.com/charge");
HttpURLConnection conn =
    (HttpURLConnection) url.openConnection();
```

CORRETO:

```java
// Enforce TLS 1.3 minimum
SSLContext ctx = SSLContext.getInstance("TLSv1.3");

// HTTPS only for payment data
URL url = new URL("https://payment-gateway.com/charge");
HttpsURLConnection conn =
    (HttpsURLConnection) url.openConnection();
conn.setSSLSocketFactory(ctx.getSocketFactory());
```

### O que o code reviewer deve checar

- [ ] All transmission of cardholder data uses TLS 1.2 or higher
- [ ] No fallback to insecure protocols (SSL, TLS 1.0, TLS 1.1)
- [ ] Certificate validation is not disabled (no trust-all managers)

## PCI-DSS v4.0 --- Requirement 5: Protect All Systems and Networks from Malicious Software

### O que o requisito exige

Anti-malware mechanisms must protect all systems and networks. Application code must be protected against known vulnerability patterns such as injection, deserialization attacks, and file upload exploits.

### Verificacao em codigo Java

PROIBIDO:

```java
// Deserializing untrusted input without safe mode
ObjectInputStream ois = new ObjectInputStream(
    untrustedStream);
Object obj = ois.readObject();

// Accepting arbitrary file uploads without validation
Path dest = Paths.get("/uploads/" + fileName);
Files.copy(uploadStream, dest);
```

CORRETO:

```java
// Use safe deserialization with allowlist
ObjectInputFilter filter = ObjectInputFilter.Config
    .createFilter("com.myapp.dto.*;!*");
ObjectInputStream ois = new ObjectInputStream(
    untrustedStream);
ois.setObjectInputFilter(filter);

// Validate file type and sanitize filename
String safeName = sanitizeFileName(fileName);
String contentType = detectContentType(uploadStream);
if (!ALLOWED_TYPES.contains(contentType)) {
    throw new SecurityException("Invalid file type");
}
```

### O que o code reviewer deve checar

- [ ] No unsafe deserialization of untrusted input
- [ ] File uploads validate content type and sanitize filenames
- [ ] Dependencies scanned for known vulnerabilities (CVE checks)

## PCI-DSS v4.0 --- Requirement 6: Develop and Maintain Secure Systems and Software

### O que o requisito exige

Security must be integrated into all phases of the software development lifecycle. Custom software must be developed securely, with code reviews and vulnerability testing before release.

### Verificacao em codigo Java

PROIBIDO:

```java
// SQL injection vulnerability
String query = "SELECT * FROM users WHERE id = '"
    + userInput + "'";
Statement stmt = conn.createStatement();
ResultSet rs = stmt.executeQuery(query);

// XSS vulnerability — unescaped output
response.getWriter().write(
    "<div>" + userInput + "</div>");
```

CORRETO:

```java
// Parameterized queries prevent SQL injection
String query = "SELECT * FROM users WHERE id = ?";
PreparedStatement stmt = conn.prepareStatement(query);
stmt.setString(1, userInput);
ResultSet rs = stmt.executeQuery();

// Escape output to prevent XSS
String safe = HtmlUtils.htmlEscape(userInput);
response.getWriter().write(
    "<div>" + safe + "</div>");
```

### O que o code reviewer deve checar

- [ ] All SQL queries use parameterized statements
- [ ] All user input is validated and sanitized before use
- [ ] Output encoding applied to prevent XSS
- [ ] No known vulnerable libraries (dependency audit passes)

## PCI-DSS v4.0 --- Requirement 7: Restrict Access to System Components and Cardholder Data by Business Need to Know

### O que o requisito exige

Access to cardholder data and system components must be limited to only those individuals and processes whose job requires such access. Role-based access control (RBAC) must be implemented.

### Verificacao em codigo Java

PROIBIDO:

```java
// No authorization check on sensitive endpoint
@GetMapping("/api/cards/{id}")
public CardData getCard(@PathVariable String id) {
    return cardRepository.findById(id);
}

// Broad role assignment
@PreAuthorize("isAuthenticated()")
public void viewAllTransactions() { }
```

CORRETO:

```java
// Fine-grained RBAC on sensitive endpoint
@GetMapping("/api/cards/{id}")
@PreAuthorize("hasRole('CARD_VIEWER') and "
    + "@accessPolicy.canViewCard(#id, principal)")
public CardData getCard(@PathVariable String id) {
    return cardRepository.findById(id);
}

// Least-privilege role check
@PreAuthorize("hasRole('TRANSACTION_AUDITOR')")
public void viewAllTransactions() { }
```

### O que o code reviewer deve checar

- [ ] Every endpoint accessing cardholder data has authorization checks
- [ ] RBAC follows least-privilege principle (no broad role grants)
- [ ] Service-to-service calls use scoped credentials

## PCI-DSS v4.0 --- Requirement 8: Identify Users and Authenticate Access to System Components

### O que o requisito exige

All users must be identified and authenticated before accessing system components. Multi-factor authentication (MFA) must be used for all access to the cardholder data environment. Passwords must meet complexity and rotation requirements.

### Verificacao em codigo Java

PROIBIDO:

```java
// Weak password validation
if (password.length() >= 4) {
    authenticateUser(username, password);
}

// Storing passwords in plaintext
userRepository.save(new User(username, password));
```

CORRETO:

```java
// Strong password policy enforcement
PasswordPolicy policy = PasswordPolicy.builder()
    .minLength(12)
    .requireUpperCase(true)
    .requireDigit(true)
    .requireSpecialChar(true)
    .build();
if (!policy.validate(password)) {
    throw new WeakPasswordException(
        "Password does not meet policy requirements");
}

// Hash passwords with strong algorithm
String hashed = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()
    .encode(password);
userRepository.save(new User(username, hashed));
```

### O que o code reviewer deve checar

- [ ] Passwords hashed with Argon2id, bcrypt, or scrypt (never MD5/SHA1)
- [ ] Password policy enforces minimum 12 characters with complexity
- [ ] MFA enforced for administrative and CDE access
- [ ] Session tokens have appropriate expiration and rotation

## PCI-DSS v4.0 --- Requirement 9: Restrict Physical Access to Cardholder Data

### O que o requisito exige

Este requisito e organizacional e nao mapeia diretamente para codigo. Physical access to systems and media containing cardholder data must be restricted. This includes facility access controls, visitor management, media handling and destruction procedures.

### Nota

This requirement addresses physical security controls (badge access, CCTV, visitor logs, media destruction) that are enforced at the infrastructure and facilities level, not in application code. Code reviewers should verify that the application does not bypass physical security by exposing cardholder data through unsecured channels (e.g., writing PAN to local files, unencrypted exports).

## PCI-DSS v4.0 --- Requirement 10: Log and Monitor All Access to System Components and Cardholder Data

### O que o requisito exige

All access to network resources and cardholder data must be logged and monitored. Audit trails must be maintained and regularly reviewed to detect anomalies and unauthorized access.

### Verificacao em codigo Java

PROIBIDO:

```java
// No audit logging for sensitive operations
public void processPayment(PaymentRequest req) {
    paymentGateway.charge(req);
}

// Logging sensitive data in audit trail
logger.info("Payment processed for card: {}",
    req.getCardNumber());
```

CORRETO:

```java
// Comprehensive audit logging without sensitive data
public void processPayment(PaymentRequest req) {
    String masked = maskPan(req.getCardNumber());
    auditLogger.log(AuditEvent.builder()
        .action("PAYMENT_PROCESSED")
        .userId(SecurityContext.getCurrentUserId())
        .maskedPan(masked)
        .amount(req.getAmount())
        .timestamp(Instant.now())
        .build());
    paymentGateway.charge(req);
}
```

### O que o code reviewer deve checar

- [ ] All access to cardholder data generates audit log entries
- [ ] Audit logs never contain full PAN, CVV, or sensitive auth data
- [ ] Log entries include user ID, timestamp, action, and outcome
- [ ] Audit logs are tamper-resistant (append-only, integrity checks)

## PCI-DSS v4.0 --- Requirement 11: Test Security of Systems and Networks Regularly

### O que o requisito exige

Security of systems and networks must be tested regularly through vulnerability scans, penetration tests, and intrusion detection. Unauthorized wireless access points must be detected and addressed.

### Verificacao em codigo Java

PROIBIDO:

```java
// No input validation — vulnerable to fuzzing
public String processInput(String raw) {
    return transform(raw);
}

// Ignoring security test findings
@SuppressWarnings("security")
public void handleRequest(HttpServletRequest req) {
    String param = req.getParameter("data");
    executeCommand(param);
}
```

CORRETO:

```java
// Input validation with boundary checks
public String processInput(String raw) {
    if (raw == null || raw.length() > MAX_INPUT_LENGTH) {
        throw new ValidationException(
            "Input exceeds maximum allowed length");
    }
    String sanitized = InputSanitizer.sanitize(raw);
    return transform(sanitized);
}

// Security-conscious request handling
public void handleRequest(HttpServletRequest req) {
    String param = InputValidator.validate(
        req.getParameter("data"),
        ALLOWED_PATTERN);
    processSecurely(param);
}
```

### O que o code reviewer deve checar

- [ ] All public endpoints have input validation with size limits
- [ ] Security scanning (SAST/DAST) integrated in CI/CD pipeline
- [ ] No suppression of security warnings without documented justification

## PCI-DSS v4.0 --- Requirement 12: Support Information Security with Organizational Policies and Procedures

### O que o requisito exige

Este requisito e organizacional e nao mapeia diretamente para codigo. An information security policy must be established, published, maintained, and disseminated. The policy must address all PCI-DSS requirements and define security responsibilities for all personnel.

### Nota

This requirement addresses organizational governance: security policies, risk assessments, awareness training, incident response plans, and third-party service provider management. While not directly enforceable in code, reviewers should verify that code changes align with documented security policies and that security-critical decisions are properly documented (e.g., ADRs for cryptographic choices, access control models).

## Related Knowledge Packs

| Pack | Relationship |
|------|-------------|
| `security` | OWASP Top 10, security headers, secrets management |
| `owasp-asvs` | OWASP ASVS 4.0.3 verification standard |
| `compliance` | Compliance frameworks (GDPR, HIPAA, PCI-DSS) |
