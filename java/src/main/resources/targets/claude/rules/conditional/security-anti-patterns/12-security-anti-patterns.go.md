# Rule 12 — Security Anti-Patterns (Go)

> Language-specific security anti-patterns with vulnerable and fixed code examples.
> Each entry references its CWE identifier and severity level.

## Security Anti-Patterns

### G1: HTTP Without TLS
**CWE:** CWE-319 — Cleartext Transmission of Sensitive Information
**Severity:** HIGH

#### Vulnerable Code
```go
// HTTP server without TLS — all traffic in cleartext
func main() {
    mux := http.NewServeMux()
    mux.HandleFunc("/api/login", loginHandler)
    log.Fatal(http.ListenAndServe(":8080", mux))
}
```

#### Fixed Code
```go
// HTTPS server with TLS configuration
func main() {
    mux := http.NewServeMux()
    mux.HandleFunc("/api/login", loginHandler)
    srv := &http.Server{
        Addr:    ":8443",
        Handler: mux,
        TLSConfig: &tls.Config{
            MinVersion: tls.VersionTLS12,
        },
    }
    log.Fatal(srv.ListenAndServeTLS(
        "cert.pem", "key.pem"))
}
```

#### Why it is dangerous
Without TLS, all data including credentials, tokens, and personal information is transmitted in cleartext. Any network observer (proxy, ISP, attacker on shared WiFi) can read and modify traffic. Authentication endpoints are especially critical targets.

### G2: Ignored Crypto Errors
**CWE:** CWE-390 — Detection of Error Condition Without Action
**Severity:** HIGH

#### Vulnerable Code
```go
// Crypto errors silently ignored with blank identifier
func encrypt(
    plaintext []byte, key []byte,
) []byte {
    block, _ := aes.NewCipher(key)
    gcm, _ := cipher.NewGCM(block)
    nonce := make([]byte, gcm.NonceSize())
    _, _ = io.ReadFull(rand.Reader, nonce)
    return gcm.Seal(nonce, nonce, plaintext, nil)
}
```

#### Fixed Code
```go
// Every crypto error checked and propagated
func encrypt(
    plaintext []byte, key []byte,
) ([]byte, error) {
    block, err := aes.NewCipher(key)
    if err != nil {
        return nil, fmt.Errorf(
            "create cipher: %w", err)
    }
    gcm, err := cipher.NewGCM(block)
    if err != nil {
        return nil, fmt.Errorf(
            "create GCM: %w", err)
    }
    nonce := make([]byte, gcm.NonceSize())
    if _, err := io.ReadFull(
        rand.Reader, nonce,
    ); err != nil {
        return nil, fmt.Errorf(
            "generate nonce: %w", err)
    }
    return gcm.Seal(nonce, nonce, plaintext, nil), nil
}
```

#### Why it is dangerous
Ignoring errors from cryptographic operations means the code may silently produce nil ciphers, zero-length nonces, or uninitialized buffers. This results in data that appears encrypted but is actually unprotected or uses predictable values, completely undermining the encryption.

### G3: template.HTML(userInput)
**CWE:** CWE-79 — Improper Neutralization of Input During Web Page Generation
**Severity:** HIGH

#### Vulnerable Code
```go
// User input marked as safe HTML — bypasses escaping
func renderComment(w http.ResponseWriter, comment string) {
    tmpl := template.Must(
        template.New("comment").Parse(
            `<div>{{.}}</div>`))
    tmpl.Execute(w, template.HTML(comment))
}
```

#### Fixed Code
```go
// User input auto-escaped by template engine
func renderComment(w http.ResponseWriter, comment string) {
    tmpl := template.Must(
        template.New("comment").Parse(
            `<div>{{.}}</div>`))
    tmpl.Execute(w, comment)
}
```

#### Why it is dangerous
`template.HTML()` tells Go's template engine to treat the string as safe HTML, disabling automatic escaping. An attacker can inject `<script>` tags or event handlers that execute JavaScript in other users' browsers, stealing sessions or performing actions on their behalf.

### G4: SQL Query by String Concatenation
**CWE:** CWE-89 — SQL Injection
**Severity:** CRITICAL

#### Vulnerable Code
```go
// User input concatenated directly into SQL query
func findUser(
    db *sql.DB, name string,
) (*User, error) {
    query := "SELECT id, name FROM users WHERE name = '" +
        name + "'"
    row := db.QueryRow(query)
    var user User
    err := row.Scan(&user.ID, &user.Name)
    return &user, err
}
```

#### Fixed Code
```go
// Parameterized query prevents SQL injection
func findUser(
    db *sql.DB, name string,
) (*User, error) {
    query := "SELECT id, name FROM users WHERE name = $1"
    row := db.QueryRow(query, name)
    var user User
    err := row.Scan(&user.ID, &user.Name)
    return &user, err
}
```

#### Why it is dangerous
String concatenation in SQL queries allows an attacker to inject arbitrary SQL (e.g., `' OR 1=1 --`). This can bypass authentication, exfiltrate entire databases, or execute administrative operations. Parameterized queries treat user input as data, never as SQL.
