# Rule 10 — Anti-Patterns ({LANGUAGE_NAME} + {FRAMEWORK_NAME})

> Language-specific anti-patterns with incorrect and correct code examples.
> Each entry references the rule or knowledge pack it violates.

## Anti-Patterns

### ANTI-001: Goroutine Leak (CRITICAL)
**Category:** CONCURRENCY
**Rule violated:** `03-coding-standards.md` (concurrency)

**Incorrect code:**
```go
// Goroutine without cancellation — leaks on timeout
func fetchAll(urls []string) []string {
    results := make(chan string)
    for _, url := range urls {
        go func(u string) {
            resp, _ := http.Get(u) // no context, no cancel
            body, _ := io.ReadAll(resp.Body)
            results <- string(body) // blocks forever if no reader
        }(url)
    }
    // only reads first result, rest leak
    return []string{<-results}
}
```

**Correct code:**
```go
// Context-aware goroutines with proper cleanup
func fetchAll(ctx context.Context, urls []string) ([]string, error) {
    g, ctx := errgroup.WithContext(ctx)
    results := make([]string, len(urls))

    for i, url := range urls {
        i, url := i, url
        g.Go(func() error {
            req, err := http.NewRequestWithContext(ctx, http.MethodGet, url, nil)
            if err != nil {
                return fmt.Errorf("create request for %s: %w", url, err)
            }
            resp, err := http.DefaultClient.Do(req)
            if err != nil {
                return fmt.Errorf("fetch %s: %w", url, err)
            }
            defer resp.Body.Close()
            body, err := io.ReadAll(resp.Body)
            if err != nil {
                return fmt.Errorf("read body from %s: %w", url, err)
            }
            results[i] = string(body)
            return nil
        })
    }
    if err := g.Wait(); err != nil {
        return nil, err
    }
    return results, nil
}
```

### ANTI-002: Panic in HTTP Handler (CRITICAL)
**Category:** ERROR_HANDLING
**Rule violated:** `03-coding-standards.md#error-handling`

**Incorrect code:**
```go
// panic crashes the entire server
func GetUser(c *gin.Context) {
    id := c.Param("id")
    user, err := userRepo.FindByID(id)
    if err != nil {
        panic("user not found: " + id) // crashes server
    }
    c.JSON(http.StatusOK, user)
}
```

**Correct code:**
```go
// Return proper HTTP error — never panic in handlers
func GetUser(c *gin.Context) {
    id := c.Param("id")
    user, err := userRepo.FindByID(c.Request.Context(), id)
    if err != nil {
        c.JSON(http.StatusNotFound, gin.H{
            "error": fmt.Sprintf("user not found: %s", id),
        })
        return
    }
    c.JSON(http.StatusOK, toUserResponse(user))
}
```

### ANTI-003: Exported Mutex Without Documentation (HIGH)
**Category:** CONCURRENCY
**Rule violated:** `03-coding-standards.md` (concurrency)

**Incorrect code:**
```go
// Exported struct with unexplained mutex — race condition risk
type Cache struct {
    mu    sync.Mutex
    items map[string]string
}
```

**Correct code:**
```go
// Mutex purpose documented, fields unexported
type Cache struct {
    // mu protects concurrent access to items map.
    // Lock ordering: mu must be acquired before any I/O.
    mu    sync.Mutex
    items map[string]string
}

// Get retrieves a cached value by key (thread-safe).
func (c *Cache) Get(key string) (string, bool) {
    c.mu.Lock()
    defer c.mu.Unlock()
    val, ok := c.items[key]
    return val, ok
}
```

### ANTI-004: SQL Injection via String Concatenation (CRITICAL)
**Category:** SECURITY
**Rule violated:** `06-security-baseline.md`

**Incorrect code:**
```go
// String concatenation creates SQL injection vulnerability
func FindUser(db *sql.DB, name string) (*User, error) {
    query := "SELECT * FROM users WHERE name = '" + name + "'"
    row := db.QueryRow(query) // vulnerable to injection
    var user User
    err := row.Scan(&user.ID, &user.Name)
    return &user, err
}
```

**Correct code:**
```go
// Parameterized query prevents SQL injection
func FindUser(ctx context.Context, db *sql.DB, name string) (*User, error) {
    query := "SELECT id, name FROM users WHERE name = $1"
    row := db.QueryRowContext(ctx, query, name)
    var user User
    err := row.Scan(&user.ID, &user.Name)
    if err != nil {
        return nil, fmt.Errorf("find user %q: %w", name, err)
    }
    return &user, nil
}
```

### ANTI-005: Deferred Close Without Error Check (MEDIUM)
**Category:** ERROR_HANDLING
**Rule violated:** `03-coding-standards.md#error-handling`

**Incorrect code:**
```go
// Close error silently discarded
func readFile(path string) ([]byte, error) {
    f, err := os.Open(path)
    if err != nil {
        return nil, err
    }
    defer f.Close() // error ignored
    return io.ReadAll(f)
}
```

**Correct code:**
```go
// Close error captured and returned
func readFile(path string) (data []byte, retErr error) {
    f, err := os.Open(path)
    if err != nil {
        return nil, fmt.Errorf("open %s: %w", path, err)
    }
    defer func() {
        if cerr := f.Close(); cerr != nil && retErr == nil {
            retErr = fmt.Errorf("close %s: %w", path, cerr)
        }
    }()
    data, err = io.ReadAll(f)
    if err != nil {
        return nil, fmt.Errorf("read %s: %w", path, err)
    }
    return data, nil
}
```
