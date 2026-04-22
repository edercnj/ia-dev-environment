# Example: Router Composition

### Modular Router Design

```rust
use axum::{Router, routing::{get, post, delete}};

#[derive(Clone)]
struct AppState {
    merchant_service: Arc<dyn MerchantService>,
    db_pool: PgPool,
}

pub fn app(state: AppState) -> Router {
    Router::new()
        .nest("/api/v1", api_routes())
        .with_state(state)
}

fn api_routes() -> Router<AppState> {
    Router::new()
        .merge(health_routes())
        .merge(merchant_routes())
}

fn health_routes() -> Router<AppState> {
    Router::new()
        .route("/health", get(health_check))
}

fn merchant_routes() -> Router<AppState> {
    Router::new()
        .route("/merchants", get(list_merchants).post(create_merchant))
        .route("/merchants/{id}", get(get_merchant).delete(delete_merchant))
}
```

### Nested Routes with Prefix

```rust
fn admin_routes() -> Router<AppState> {
    Router::new()
        .nest("/admin", Router::new()
            .route("/users", get(list_users))
            .route("/users/{id}", get(get_user).delete(delete_user))
            .layer(middleware::from_fn(require_admin_role))
        )
}
```

### Layer Application

```rust
use tower_http::cors::CorsLayer;
use tower_http::trace::TraceLayer;

pub fn app(state: AppState) -> Router {
    Router::new()
        .nest("/api/v1", api_routes())
        .layer(TraceLayer::new_for_http())
        .layer(CorsLayer::permissive()) // restrict in production
        .with_state(state)
}
```
