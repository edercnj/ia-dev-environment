# Rule 12 — Security Anti-Patterns (Kotlin)

> Language-specific security anti-patterns with vulnerable and fixed code examples.
> Each entry references its CWE identifier and severity level.

## Security Anti-Patterns

### J1: SQL Concatenation with String
**CWE:** CWE-89 — SQL Injection
**Severity:** CRITICAL

#### Vulnerable Code
```kotlin
// User input concatenated directly into SQL query
fun findByName(name: String): List<User> {
    val sql = "SELECT * FROM users WHERE name = '$name'"
    return jdbcTemplate.query(sql, userMapper)
}
```

#### Fixed Code
```kotlin
// Parameterized query prevents SQL injection
fun findByName(name: String): List<User> {
    val sql = "SELECT * FROM users WHERE name = ?"
    return jdbcTemplate.query(sql, userMapper, name)
}
```

#### Why it is dangerous
An attacker can inject arbitrary SQL through the `name` parameter (e.g., `' OR 1=1 --`), bypassing authentication, exfiltrating data, or dropping tables. Parameterized queries ensure user input is always treated as data, never as executable SQL.

### J2: Math.random() for Security
**CWE:** CWE-330 — Use of Insufficiently Random Values
**Severity:** HIGH

#### Vulnerable Code
```kotlin
// Math.random() is predictable and not cryptographic
fun generateToken(): String =
    Math.random().toString()
```

#### Fixed Code
```kotlin
// SecureRandom provides cryptographically strong values
fun generateToken(): String {
    val bytes = ByteArray(32)
    SecureRandom.getInstanceStrong().nextBytes(bytes)
    return HexFormat.of().formatHex(bytes)
}
```

#### Why it is dangerous
`Math.random()` uses a linear congruential generator whose seed can be predicted after observing a few outputs. Session tokens, CSRF tokens, and password reset links generated with `Math.random()` can be forged by an attacker.

### J3: ObjectInputStream Without Whitelist
**CWE:** CWE-502 — Deserialization of Untrusted Data
**Severity:** CRITICAL

#### Vulnerable Code
```kotlin
// Deserializes any class from untrusted input
fun deserialize(data: ByteArray): Any {
    ObjectInputStream(
        ByteArrayInputStream(data)
    ).use { ois ->
        return ois.readObject()
    }
}
```

#### Fixed Code
```kotlin
// ObjectInputFilter restricts allowed classes
fun deserialize(data: ByteArray): Any {
    ObjectInputStream(
        ByteArrayInputStream(data)
    ).use { ois ->
        ois.setObjectInputFilter(
            ObjectInputFilter.Config.createFilter(
                "com.example.dto.*;!*"))
        return ois.readObject()
    }
}
```

#### Why it is dangerous
Java deserialization can instantiate arbitrary classes and trigger gadget chains (e.g., Commons Collections, Spring beans) leading to remote code execution. An attacker sending a crafted byte stream can execute arbitrary commands on the server.

### J4: Password Hardcoded in String
**CWE:** CWE-798 — Use of Hard-coded Credentials
**Severity:** CRITICAL

#### Vulnerable Code
```kotlin
// Credentials embedded in source code
object DatabaseConfig {
    const val DB_PASSWORD = "s3cret!"
    const val API_KEY = "ak_live_1234567890"
}
```

#### Fixed Code
```kotlin
// Credentials loaded from external configuration
class DatabaseConfig(
    @ConfigProperty(name = "db.password")
    private val dbPassword: String,
    @ConfigProperty(name = "api.key")
    private val apiKey: String,
)
```

#### Why it is dangerous
Hard-coded credentials are visible in source control history, compiled bytecode, and memory dumps. If the repository is leaked or decompiled, all environments using those credentials are immediately compromised. Credentials must come from a secrets manager or environment variables.

### J5: X509TrustManager Empty (Trust All)
**CWE:** CWE-295 — Improper Certificate Validation
**Severity:** CRITICAL

#### Vulnerable Code
```kotlin
// Trust manager that accepts all certificates
val trustAll = arrayOf<TrustManager>(
    object : X509TrustManager {
        override fun checkClientTrusted(
            certs: Array<X509Certificate>, auth: String,
        ) {}
        override fun checkServerTrusted(
            certs: Array<X509Certificate>, auth: String,
        ) {}
        override fun getAcceptedIssuers():
            Array<X509Certificate> = emptyArray()
    }
)
val ctx = SSLContext.getInstance("TLS")
ctx.init(null, trustAll, null)
```

#### Fixed Code
```kotlin
// Use default trust manager with system CA store
val ctx = SSLContext.getInstance("TLS")
val tmf = TrustManagerFactory.getInstance(
    TrustManagerFactory.getDefaultAlgorithm())
tmf.init(null as KeyStore?)
ctx.init(null, tmf.trustManagers, null)
```

#### Why it is dangerous
A trust-all manager disables TLS certificate validation, allowing man-in-the-middle attacks. An attacker on the network can intercept, read, and modify all HTTPS traffic without detection. This completely negates the security guarantees of TLS.

### J6: File(userInput) Without Normalization
**CWE:** CWE-22 — Improper Limitation of a Pathname to a Restricted Directory
**Severity:** HIGH

#### Vulnerable Code
```kotlin
// User input used directly as file path
fun readFile(filename: String): ByteArray {
    val file = File("/uploads/$filename")
    return file.readBytes()
}
```

#### Fixed Code
```kotlin
// Normalize path and verify it stays within base dir
fun readFile(filename: String): ByteArray {
    val base = Path.of("/uploads").toAbsolutePath()
    val resolved = base.resolve(filename)
        .normalize().toAbsolutePath()
    require(resolved.startsWith(base)) {
        "Path traversal attempt: $filename"
    }
    return Files.readAllBytes(resolved)
}
```

#### Why it is dangerous
An attacker can use `../` sequences (e.g., `../../etc/passwd`) to escape the intended directory and read or overwrite arbitrary files on the server. Path normalization and prefix validation are both required to prevent directory traversal.

### J7: Exception Message in HTTP Response
**CWE:** CWE-209 — Generation of Error Message Containing Sensitive Information
**Severity:** MEDIUM

#### Vulnerable Code
```kotlin
// Exception details exposed to client
@ExceptionHandler(Exception::class)
fun handleError(e: Exception): ResponseEntity<String> =
    ResponseEntity.status(500).body(e.message)
```

#### Fixed Code
```kotlin
// Generic message to client, details logged server-side
@ExceptionHandler(Exception::class)
fun handleError(
    e: Exception,
): ResponseEntity<ErrorResponse> {
    log.error("Unhandled exception", e)
    return ResponseEntity.status(500)
        .body(ErrorResponse("Internal server error",
            "ERR-500"))
}
```

#### Why it is dangerous
Exception messages may contain internal class names, SQL queries, file paths, or stack traces that reveal the application's technology stack and internal structure. Attackers use this information to craft targeted exploits.

### J8: CORS allowedOrigins("*")
**CWE:** CWE-942 — Permissive Cross-domain Policy with Untrusted Domains
**Severity:** HIGH

#### Vulnerable Code
```kotlin
// Allows any origin to make cross-domain requests
install(CORS) {
    anyHost()
    allowMethod(HttpMethod.Options)
    allowMethod(HttpMethod.Put)
    allowCredentials = true
}
```

#### Fixed Code
```kotlin
// Restrict origins to known, trusted domains
install(CORS) {
    allowHost("app.example.com", schemes = listOf("https"))
    allowMethod(HttpMethod.Get)
    allowMethod(HttpMethod.Post)
    allowMethod(HttpMethod.Put)
    allowCredentials = true
}
```

#### Why it is dangerous
A wildcard CORS policy allows any website to make authenticated cross-origin requests to your API. An attacker can host a malicious page that reads sensitive data from your endpoints using the victim's cookies or tokens.
