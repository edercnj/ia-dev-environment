# Rule 12 — Security Anti-Patterns (Java)

> Language-specific security anti-patterns with vulnerable and fixed code examples.
> Each entry references its CWE identifier and severity level.

## Security Anti-Patterns

### J1: SQL Concatenation with String
**CWE:** CWE-89 — SQL Injection
**Severity:** CRITICAL

#### Vulnerable Code
```java
// User input concatenated directly into SQL query
public List<User> findByName(String name) {
    String sql = "SELECT * FROM users WHERE name = '"
            + name + "'";
    return jdbcTemplate.query(sql, userMapper);
}
```

#### Fixed Code
```java
// Parameterized query prevents SQL injection
public List<User> findByName(String name) {
    String sql = "SELECT * FROM users WHERE name = ?";
    return jdbcTemplate.query(sql, userMapper, name);
}
```

#### Why it is dangerous
An attacker can inject arbitrary SQL through the `name` parameter (e.g., `' OR 1=1 --`), bypassing authentication, exfiltrating data, or dropping tables. Parameterized queries ensure user input is always treated as data, never as executable SQL.

### J2: Math.random() for Security
**CWE:** CWE-330 — Use of Insufficiently Random Values
**Severity:** HIGH

#### Vulnerable Code
```java
// Math.random() is predictable and not cryptographic
public String generateToken() {
    return String.valueOf(Math.random());
}
```

#### Fixed Code
```java
// SecureRandom provides cryptographically strong values
public String generateToken() {
    byte[] bytes = new byte[32];
    SecureRandom.getInstanceStrong().nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
}
```

#### Why it is dangerous
`Math.random()` uses a linear congruential generator whose seed can be predicted after observing a few outputs. Session tokens, CSRF tokens, and password reset links generated with `Math.random()` can be forged by an attacker.

### J3: ObjectInputStream Without Whitelist
**CWE:** CWE-502 — Deserialization of Untrusted Data
**Severity:** CRITICAL

#### Vulnerable Code
```java
// Deserializes any class from untrusted input
public Object deserialize(byte[] data) {
    try (ObjectInputStream ois =
            new ObjectInputStream(
                    new ByteArrayInputStream(data))) {
        return ois.readObject();
    }
}
```

#### Fixed Code
```java
// ObjectInputFilter restricts allowed classes
public Object deserialize(byte[] data) {
    try (ObjectInputStream ois =
            new ObjectInputStream(
                    new ByteArrayInputStream(data))) {
        ois.setObjectInputFilter(
                ObjectInputFilter.Config.createFilter(
                        "com.example.dto.*;!*"));
        return ois.readObject();
    }
}
```

#### Why it is dangerous
Java deserialization can instantiate arbitrary classes and trigger gadget chains (e.g., Commons Collections, Spring beans) leading to remote code execution. An attacker sending a crafted byte stream can execute arbitrary commands on the server.

### J4: Password Hardcoded in String
**CWE:** CWE-798 — Use of Hard-coded Credentials
**Severity:** CRITICAL

#### Vulnerable Code
```java
// Credentials embedded in source code
public class DatabaseConfig {
    private static final String DB_PASSWORD = "s3cret!";
    private static final String API_KEY =
            "ak_live_1234567890";
}
```

#### Fixed Code
```java
// Credentials loaded from external configuration
public class DatabaseConfig {
    private final String dbPassword;
    private final String apiKey;

    DatabaseConfig(
            @Value("${db.password}") String dbPassword,
            @Value("${api.key}") String apiKey) {
        this.dbPassword = dbPassword;
        this.apiKey = apiKey;
    }
}
```

#### Why it is dangerous
Hard-coded credentials are visible in source control history, compiled bytecode, and memory dumps. If the repository is leaked or decompiled, all environments using those credentials are immediately compromised. Credentials must come from a secrets manager or environment variables.

### J5: X509TrustManager Empty (Trust All)
**CWE:** CWE-295 — Improper Certificate Validation
**Severity:** CRITICAL

#### Vulnerable Code
```java
// Trust manager that accepts all certificates
TrustManager[] trustAll = new TrustManager[] {
    new X509TrustManager() {
        public void checkClientTrusted(
                X509Certificate[] c, String a) {}
        public void checkServerTrusted(
                X509Certificate[] c, String a) {}
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
};
SSLContext ctx = SSLContext.getInstance("TLS");
ctx.init(null, trustAll, null);
```

#### Fixed Code
```java
// Use default trust manager with system CA store
SSLContext ctx = SSLContext.getInstance("TLS");
TrustManagerFactory tmf =
        TrustManagerFactory.getInstance(
                TrustManagerFactory
                        .getDefaultAlgorithm());
tmf.init((KeyStore) null);
ctx.init(null, tmf.getTrustManagers(), null);
```

#### Why it is dangerous
A trust-all manager disables TLS certificate validation, allowing man-in-the-middle attacks. An attacker on the network can intercept, read, and modify all HTTPS traffic without detection. This completely negates the security guarantees of TLS.

### J6: new File(userInput) Without Normalization
**CWE:** CWE-22 — Improper Limitation of a Pathname to a Restricted Directory
**Severity:** HIGH

#### Vulnerable Code
```java
// User input used directly as file path
public byte[] readFile(String filename) {
    File file = new File("/uploads/" + filename);
    return Files.readAllBytes(file.toPath());
}
```

#### Fixed Code
```java
// Normalize path and verify it stays within base dir
public byte[] readFile(String filename) {
    Path base = Path.of("/uploads").toAbsolutePath();
    Path resolved = base.resolve(filename)
            .normalize().toAbsolutePath();
    if (!resolved.startsWith(base)) {
        throw new SecurityException(
                "Path traversal attempt: "
                        + filename);
    }
    return Files.readAllBytes(resolved);
}
```

#### Why it is dangerous
An attacker can use `../` sequences (e.g., `../../etc/passwd`) to escape the intended directory and read or overwrite arbitrary files on the server. Path normalization and prefix validation are both required to prevent directory traversal.

### J7: Exception Message in HTTP Response
**CWE:** CWE-209 — Generation of Error Message Containing Sensitive Information
**Severity:** MEDIUM

#### Vulnerable Code
```java
// Exception details exposed to client
@ExceptionHandler(Exception.class)
public ResponseEntity<String> handleError(Exception e) {
    return ResponseEntity.status(500)
            .body(e.getMessage());
}
```

#### Fixed Code
```java
// Generic message to client, details logged server-side
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleError(
        Exception e) {
    log.error("Unhandled exception", e);
    return ResponseEntity.status(500)
            .body(new ErrorResponse(
                    "Internal server error",
                    "ERR-500"));
}
```

#### Why it is dangerous
Exception messages may contain internal class names, SQL queries, file paths, or stack traces that reveal the application's technology stack and internal structure. Attackers use this information to craft targeted exploits.

### J8: CORS allowedOrigins("*")
**CWE:** CWE-942 — Permissive Cross-domain Policy with Untrusted Domains
**Severity:** HIGH

#### Vulnerable Code
```java
// Allows any origin to make cross-domain requests
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("*")
                .allowCredentials(true);
    }
}
```

#### Fixed Code
```java
// Restrict origins to known, trusted domains
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                        "https://app.example.com")
                .allowedMethods("GET", "POST", "PUT")
                .allowCredentials(true);
    }
}
```

#### Why it is dangerous
A wildcard CORS policy allows any website to make authenticated cross-origin requests to your API. An attacker can host a malicious page that reads sensitive data from your endpoints using the victim's cookies or tokens.
