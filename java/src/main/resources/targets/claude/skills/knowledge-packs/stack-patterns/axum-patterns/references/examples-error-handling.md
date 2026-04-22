# Example: Error Handling

### Error Type with IntoResponse

```rust
use axum::response::{IntoResponse, Response};
use axum::http::StatusCode;
use axum::Json;
use thiserror::Error;

#[derive(Debug, Error)]
pub enum AppError {
    #[error("not found: {0}")]
    NotFound(String),

    #[error("validation error: {0}")]
    Validation(String),

    #[error("unauthorized: {0}")]
    Unauthorized(String),

    #[error("conflict: {0}")]
    Conflict(String),

    #[error("database error: {0}")]
    Database(#[from] sqlx::Error),

    #[error("internal error: {0}")]
    Internal(String),
}

impl IntoResponse for AppError {
    fn into_response(self) -> Response {
        let (status, problem) = match &self {
            AppError::NotFound(detail) => (
                StatusCode::NOT_FOUND,
                ProblemDetail {
                    r#type: "https://api.example.com/errors/not-found".into(),
                    title: "Not Found".into(),
                    status: 404,
                    detail: detail.clone(),
                },
            ),
            AppError::Validation(detail) => (
                StatusCode::BAD_REQUEST,
                ProblemDetail {
                    r#type: "https://api.example.com/errors/validation".into(),
                    title: "Validation Error".into(),
                    status: 400,
                    detail: detail.clone(),
                },
            ),
            AppError::Unauthorized(detail) => (
                StatusCode::UNAUTHORIZED,
                ProblemDetail {
                    r#type: "https://api.example.com/errors/unauthorized".into(),
                    title: "Unauthorized".into(),
                    status: 401,
                    detail: detail.clone(),
                },
            ),
            AppError::Conflict(detail) => (
                StatusCode::CONFLICT,
                ProblemDetail {
                    r#type: "https://api.example.com/errors/conflict".into(),
                    title: "Conflict".into(),
                    status: 409,
                    detail: detail.clone(),
                },
            ),
            AppError::Database(err) => {
                tracing::error!(error = %err, "database error");
                (
                    StatusCode::INTERNAL_SERVER_ERROR,
                    ProblemDetail {
                        r#type: "https://api.example.com/errors/internal".into(),
                        title: "Internal Server Error".into(),
                        status: 500,
                        detail: "An unexpected error occurred".into(),
                    },
                )
            }
            AppError::Internal(detail) => {
                tracing::error!(detail = %detail, "internal error");
                (
                    StatusCode::INTERNAL_SERVER_ERROR,
                    ProblemDetail {
                        r#type: "https://api.example.com/errors/internal".into(),
                        title: "Internal Server Error".into(),
                        status: 500,
                        detail: "An unexpected error occurred".into(),
                    },
                )
            }
        };

        (status, Json(problem)).into_response()
    }
}

#[derive(Debug, serde::Serialize)]
pub struct ProblemDetail {
    pub r#type: String,
    pub title: String,
    pub status: u16,
    pub detail: String,
}
```

### Handler Return Type

```rust
// Handlers return Result<impl IntoResponse, AppError>
async fn get_merchant(
    State(state): State<AppState>,
    Path(id): Path<i64>,
) -> Result<Json<MerchantResponse>, AppError> {
    let merchant = state
        .merchant_service
        .find_by_id(id)
        .await?
        .ok_or_else(|| AppError::NotFound(format!("merchant {} not found", id)))?;

    Ok(Json(merchant.into()))
}
```
