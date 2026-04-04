# Rule 10 — Anti-Patterns ({LANGUAGE_NAME} + {FRAMEWORK_NAME})

> Language-specific anti-patterns with incorrect and correct code examples.
> Each entry references the rule or knowledge pack it violates.

## Anti-Patterns

### ANTI-001: Unwrap in Handler (CRITICAL)
**Category:** ERROR_HANDLING
**Rule violated:** `03-coding-standards.md#error-handling`

**Incorrect code:**
```rust
// unwrap() panics on error — crashes the server
async fn get_user(
    Path(id): Path<i64>,
    State(pool): State<PgPool>,
) -> Json<User> {
    let user = sqlx::query_as::<_, User>(
        "SELECT * FROM users WHERE id = $1",
    )
    .bind(id)
    .fetch_one(&pool)
    .await
    .unwrap(); // panics if user not found
    Json(user)
}
```

**Correct code:**
```rust
// Proper error handling with Result and status codes
async fn get_user(
    Path(id): Path<i64>,
    State(pool): State<PgPool>,
) -> Result<Json<UserResponse>, AppError> {
    let user = sqlx::query_as::<_, User>(
        "SELECT id, name FROM users WHERE id = $1",
    )
    .bind(id)
    .fetch_optional(&pool)
    .await
    .map_err(AppError::Database)?
    .ok_or(AppError::NotFound(
        format!("User not found: {id}"),
    ))?;
    Ok(Json(UserResponse::from(user)))
}
```

### ANTI-002: Shared Mutable State Without Synchronization (CRITICAL)
**Category:** CONCURRENCY
**Rule violated:** `03-coding-standards.md#forbidden`

**Incorrect code:**
```rust
// Mutable state without lock — does not compile,
// but RefCell would cause UB in async context
struct AppState {
    cache: RefCell<HashMap<String, String>>, // UB in async
}
```

**Correct code:**
```rust
// Arc<RwLock> for thread-safe shared state
struct AppState {
    cache: Arc<RwLock<HashMap<String, String>>>,
}

async fn get_cached(
    Path(key): Path<String>,
    State(state): State<Arc<AppState>>,
) -> Result<String, AppError> {
    let cache = state.cache.read().await;
    cache.get(&key)
        .cloned()
        .ok_or(AppError::NotFound(
            format!("Key not cached: {key}"),
        ))
}
```

### ANTI-003: SQL Injection via Format String (CRITICAL)
**Category:** SECURITY
**Rule violated:** `06-security-baseline.md`

**Incorrect code:**
```rust
// String interpolation in SQL — injection vulnerability
async fn find_user(pool: &PgPool, name: &str) -> Result<User, Error> {
    let query = format!(
        "SELECT * FROM users WHERE name = '{name}'",
    );
    sqlx::query_as::<_, User>(&query)
        .fetch_one(pool)
        .await
}
```

**Correct code:**
```rust
// Parameterized query prevents injection
async fn find_user(
    pool: &PgPool,
    name: &str,
) -> Result<User, Error> {
    sqlx::query_as::<_, User>(
        "SELECT id, name FROM users WHERE name = $1",
    )
    .bind(name)
    .fetch_one(pool)
    .await
}
```

### ANTI-004: Blocking in Async Context (CRITICAL)
**Category:** CONCURRENCY
**Rule violated:** `03-coding-standards.md` (async patterns)

**Incorrect code:**
```rust
// std::fs blocks the tokio runtime
async fn read_config() -> String {
    // Blocks the async runtime thread
    std::fs::read_to_string("config.toml").unwrap()
}
```

**Correct code:**
```rust
// Use tokio::fs for non-blocking file I/O
async fn read_config() -> Result<String, AppError> {
    tokio::fs::read_to_string("config.toml")
        .await
        .map_err(|e| AppError::Internal(
            format!("Failed to read config: {e}"),
        ))
}
```

### ANTI-005: Leaking Internal Types in API (HIGH)
**Category:** SERVICE_LAYER
**Rule violated:** `04-architecture-summary.md` (layer rules)

**Incorrect code:**
```rust
// Database row struct exposed directly in API
#[derive(sqlx::FromRow, Serialize)]
struct User {
    id: i64,
    name: String,
    password_hash: String, // leaks sensitive data
}

async fn list_users(State(pool): State<PgPool>) -> Json<Vec<User>> {
    let users = sqlx::query_as::<_, User>("SELECT * FROM users")
        .fetch_all(&pool).await.unwrap();
    Json(users)
}
```

**Correct code:**
```rust
// Separate database entity from API response
#[derive(sqlx::FromRow)]
struct UserEntity {
    id: i64,
    name: String,
    password_hash: String,
}

#[derive(Serialize)]
struct UserResponse {
    id: i64,
    name: String,
}

impl From<UserEntity> for UserResponse {
    fn from(e: UserEntity) -> Self {
        Self { id: e.id, name: e.name }
    }
}
```
