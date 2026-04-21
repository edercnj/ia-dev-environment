---
name: axum-patterns
description: "Axum (Rust) patterns: request extractors, router composition, Tower middleware, sqlx data access, configuration loading, async integration testing, and typed error handling."
user-invocable: false
allowed-tools:
  - Read
  - Grep
  - Glob
---

# Pattern: Axum (Rust)

## Purpose

Provides Axum-based HTTP service patterns for Rust web APIs: extractors, router composition, Tower middleware, async data access with sqlx, typed error propagation via `IntoResponse`, and integration testing with `tokio::test`. Agents reference this pack when generating Axum handlers, middleware stacks, or `sqlx` repository code.

## Supplements

Supplements `architecture` and `layer-templates` knowledge packs with Axum-specific handler/middleware/router conventions.

## Stack Compatibility

- **Rust:** ≥ 1.75 (stable async traits in impl, required for recent Axum middleware)
- **Axum:** ≥ 0.7
- **Tokio:** ≥ 1.35 (multi-thread runtime for `axum::serve`)
- **Tower:** ≥ 0.4 (middleware composition via `ServiceBuilder`)
- **sqlx:** ≥ 0.7 (compile-time checked queries; async + connection pool)
- **Tracing:** `tracing` ≥ 0.1 + `tracing-subscriber` ≥ 0.3

## Patterns Index

| Pattern | Use case | File |
| :--- | :--- | :--- |
| Extractors | `Path`, `Query`, `Json`, `State`, custom `FromRequest` implementations | [`references/examples-extractors.md`](references/examples-extractors.md) |
| Router Composition | `Router::new()`, nested routers, typed state via `with_state` | [`references/examples-router-composition.md`](references/examples-router-composition.md) |
| Middleware (Tower) | `ServiceBuilder`, request logging, auth, timeout, compression layers | [`references/examples-middleware-tower.md`](references/examples-middleware-tower.md) |
| Data Access (sqlx) | Connection pool, compile-time checked queries, transaction patterns | [`references/examples-data-access-sqlx.md`](references/examples-data-access-sqlx.md) |
| Configuration | Typed config via `serde`, env overlay, fail-fast validation | [`references/examples-configuration.md`](references/examples-configuration.md) |
| Testing | `tokio::test`, `axum::http::Request::builder()`, `TestServer` patterns, sqlx test fixtures | [`references/examples-testing.md`](references/examples-testing.md) |
| Error Handling | `AppError` enum, `IntoResponse` impl, RFC 7807 problem JSON mapping | [`references/examples-error-handling.md`](references/examples-error-handling.md) |

## When to Open an Example File

Open a specific `references/examples-<pattern>.md` only when you are about to implement that pattern. The slim body above is enough to decide *which* pattern applies; the example file carries the complete Rust code.

## References

All pattern examples live under `references/examples-*.md` next to this SKILL.md. The naming convention is `examples-<slug>.md` where `<slug>` matches the third column of the Patterns Index above.

## Anti-Patterns (Axum-Specific)

- **`unwrap()` in handlers** — always use `?` with a proper error type implementing `IntoResponse`; `unwrap()` causes a panic that crashes the task, returning an opaque 500
- **Blocking in async handlers** — never call synchronous I/O (file reads, CPU-heavy computation) directly; use `tokio::task::spawn_blocking` to offload to a blocking thread
- **`clone()` on large State** — wrap expensive-to-clone state in `Arc`; `State` requires `Clone`, so `State<Arc<AppState>>` avoids deep copies
- **Missing error handling** — every fallible operation must propagate errors via `?`; silent `let _ =` discards errors that should be logged or returned
- **`panic!` in request handlers** — panics abort the current task and produce unhelpful responses; always return `Result<_, AppError>`
- **Body extractor not last** — `Json`, `Form`, and other body-consuming extractors must appear as the last function parameter
- **Not using `#[derive(Clone)]` on State** — `AppState` must implement `Clone` for `with_state()`; use `Arc` for fields that are expensive to clone
- **Forgetting `.await` on sqlx queries** — sqlx query builders return futures; missing `.await` silently does nothing and the query never executes
