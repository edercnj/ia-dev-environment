# Example: Middleware (Tower)

### Tower ServiceBuilder

```rust
use tower::ServiceBuilder;
use tower_http::{
    cors::{Any, CorsLayer},
    compression::CompressionLayer,
    trace::TraceLayer,
    timeout::TimeoutLayer,
};
use std::time::Duration;

pub fn app(state: AppState) -> Router {
    let middleware_stack = ServiceBuilder::new()
        .layer(TraceLayer::new_for_http())
        .layer(CompressionLayer::new())
        .layer(TimeoutLayer::new(Duration::from_secs(30)))
        .layer(
            CorsLayer::new()
                .allow_origin(Any)
                .allow_methods([Method::GET, Method::POST, Method::PUT, Method::DELETE])
                .allow_headers([AUTHORIZATION, CONTENT_TYPE]),
        );

    Router::new()
        .nest("/api/v1", api_routes())
        .layer(middleware_stack)
        .with_state(state)
}
```

### Custom Middleware with from_fn

```rust
use axum::middleware::{self, Next};
use axum::http::Request;
use axum::response::Response;

async fn logging_middleware(
    request: Request<Body>,
    next: Next,
) -> Response {
    let method = request.method().clone();
    let uri = request.uri().clone();
    let start = std::time::Instant::now();

    let response = next.run(request).await;

    let duration = start.elapsed();
    tracing::info!(
        method = %method,
        uri = %uri,
        status = %response.status(),
        duration_ms = %duration.as_millis(),
        "request completed"
    );

    response
}

async fn auth_middleware(
    State(state): State<AppState>,
    mut request: Request<Body>,
    next: Next,
) -> Result<Response, AppError> {
    let token = request
        .headers()
        .get("Authorization")
        .and_then(|v| v.to_str().ok())
        .and_then(|v| v.strip_prefix("Bearer "))
        .ok_or(AppError::Unauthorized("missing token".into()))?;

    let claims = state.auth_service.validate_token(token).await?;
    request.extensions_mut().insert(claims);

    Ok(next.run(request).await)
}

// Apply to specific routes
fn protected_routes() -> Router<AppState> {
    Router::new()
        .route("/merchants", get(list_merchants))
        .layer(middleware::from_fn_with_state(
            app_state.clone(),
            auth_middleware,
        ))
}
```

### Request ID Middleware

```rust
use uuid::Uuid;

async fn request_id_middleware(
    mut request: Request<Body>,
    next: Next,
) -> Response {
    let request_id = request
        .headers()
        .get("X-Request-ID")
        .and_then(|v| v.to_str().ok())
        .map(String::from)
        .unwrap_or_else(|| Uuid::new_v4().to_string());

    request.extensions_mut().insert(RequestId(request_id.clone()));

    let mut response = next.run(request).await;
    response.headers_mut().insert(
        "X-Request-ID",
        request_id.parse().unwrap(),
    );

    response
}
```
