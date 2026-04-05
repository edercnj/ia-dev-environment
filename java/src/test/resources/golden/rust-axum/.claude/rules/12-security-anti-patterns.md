# Rule 12 — Security Anti-Patterns (Rust)

> Language-specific security anti-patterns with vulnerable and fixed code examples.
> Each entry references its CWE identifier and severity level.

## Security Anti-Patterns

### R1: SQL Query by String Format
**CWE:** CWE-89 — SQL Injection
**Severity:** CRITICAL

#### Vulnerable Code
```rust
// User input interpolated directly into SQL query
async fn find_user(
    pool: &PgPool,
    name: &str,
) -> Result<User, sqlx::Error> {
    let query = format!(
        "SELECT id, name FROM users WHERE name = '{name}'"
    );
    sqlx::query_as::<_, User>(&query)
        .fetch_one(pool)
        .await
}
```

#### Fixed Code
```rust
// Parameterized query prevents SQL injection
async fn find_user(
    pool: &PgPool,
    name: &str,
) -> Result<User, sqlx::Error> {
    sqlx::query_as::<_, User>(
        "SELECT id, name FROM users WHERE name = $1",
    )
    .bind(name)
    .fetch_one(pool)
    .await
}
```

#### Why it is dangerous
String interpolation in SQL queries allows an attacker to inject arbitrary SQL (e.g., `' OR 1=1 --`). Even in Rust, where memory safety is guaranteed, SQL injection can bypass authentication, exfiltrate data, or execute administrative operations.

### R2: Unsafe Block for Convenience
**CWE:** CWE-787 — Out-of-bounds Write
**Severity:** HIGH

#### Vulnerable Code
```rust
// Unsafe used to bypass borrow checker — undefined behavior
fn get_value(data: &[u8], index: usize) -> u8 {
    unsafe { *data.as_ptr().add(index) }
}
```

#### Fixed Code
```rust
// Safe bounds-checked access with proper error handling
fn get_value(
    data: &[u8],
    index: usize,
) -> Result<u8, DataError> {
    data.get(index).copied().ok_or_else(|| {
        DataError::IndexOutOfBounds {
            index,
            length: data.len(),
        }
    })
}
```

#### Why it is dangerous
Using `unsafe` to bypass bounds checking reintroduces the entire class of memory safety vulnerabilities that Rust is designed to prevent. An out-of-bounds access can read sensitive memory (stack canaries, keys) or write arbitrary data, leading to code execution.

### R3: Hardcoded Secrets in Source
**CWE:** CWE-798 — Use of Hard-coded Credentials
**Severity:** CRITICAL

#### Vulnerable Code
```rust
// Credentials embedded in source code
const DB_PASSWORD: &str = "s3cret!";
const API_KEY: &str = "ak_live_1234567890";
```

#### Fixed Code
```rust
// Credentials loaded from environment variables
use std::env;

fn db_password() -> Result<String, env::VarError> {
    env::var("DB_PASSWORD")
}

fn api_key() -> Result<String, env::VarError> {
    env::var("API_KEY")
}
```

#### Why it is dangerous
Hard-coded credentials are visible in source control history and compiled binaries (strings are not stripped by default). If the repository or binary is leaked, all environments using those credentials are immediately compromised.

### R4: Unwrap on User Input
**CWE:** CWE-248 — Uncaught Exception
**Severity:** MEDIUM

#### Vulnerable Code
```rust
// unwrap() panics on invalid input — denial of service
async fn parse_id(input: &str) -> i64 {
    input.parse::<i64>().unwrap()
}
```

#### Fixed Code
```rust
// Proper error handling returns meaningful error
async fn parse_id(
    input: &str,
) -> Result<i64, ParseError> {
    input.parse::<i64>().map_err(|e| {
        ParseError::InvalidId {
            input: input.to_string(),
            source: e,
        }
    })
}
```

#### Why it is dangerous
`unwrap()` on user-controlled input causes a panic when parsing fails, crashing the handler or the entire service. An attacker can send malformed input to trigger repeated panics, causing a denial-of-service condition.
