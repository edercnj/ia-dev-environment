# Example: Extractors

### Built-In Extractors

```rust
use axum::extract::{Path, Query, Json, State};
use serde::Deserialize;

#[derive(Deserialize)]
struct ListParams {
    page: Option<u32>,
    limit: Option<u32>,
    status: Option<String>,
}

// Path extractor — captures route parameters
async fn get_merchant(
    State(state): State<AppState>,
    Path(id): Path<i64>,
) -> Result<Json<MerchantResponse>, AppError> {
    let merchant = state.merchant_service.find_by_id(id).await?;
    Ok(Json(merchant.into()))
}

// Query extractor — parses query string
async fn list_merchants(
    State(state): State<AppState>,
    Query(params): Query<ListParams>,
) -> Result<Json<PaginatedResponse<MerchantResponse>>, AppError> {
    let page = params.page.unwrap_or(0);
    let limit = params.limit.unwrap_or(20);
    let result = state.merchant_service.find_all(page, limit, params.status).await?;
    Ok(Json(result.into()))
}

// Json extractor — deserializes request body
async fn create_merchant(
    State(state): State<AppState>,
    Json(payload): Json<CreateMerchantRequest>,
) -> Result<(StatusCode, Json<MerchantResponse>), AppError> {
    payload.validate()?;
    let merchant = state.merchant_service.create(payload).await?;
    Ok((StatusCode::CREATED, Json(merchant.into())))
}
```

### Extractor Ordering

Body extractors (`Json`, `Form`) consume the request body and **must be the last extractor**:

```rust
// CORRECT: body extractor last
async fn handler(
    State(state): State<AppState>,
    Path(id): Path<i64>,
    Json(body): Json<UpdateRequest>,
) -> Result<impl IntoResponse, AppError> { /* ... */ }

// WRONG: body extractor before Path — will not compile
// async fn handler(
//     Json(body): Json<UpdateRequest>,
//     Path(id): Path<i64>,
// ) -> ... { }
```

### Custom Extractor

```rust
use axum::extract::FromRequestParts;
use axum::http::request::Parts;

#[derive(Debug, Clone)]
struct AuthenticatedUser {
    user_id: String,
    roles: Vec<String>,
}

#[async_trait]
impl<S> FromRequestParts<S> for AuthenticatedUser
where
    S: Send + Sync,
{
    type Rejection = AppError;

    async fn from_request_parts(parts: &mut Parts, _state: &S) -> Result<Self, Self::Rejection> {
        let auth_header = parts
            .headers
            .get("Authorization")
            .and_then(|v| v.to_str().ok())
            .ok_or(AppError::Unauthorized("missing authorization header".into()))?;

        let token = auth_header
            .strip_prefix("Bearer ")
            .ok_or(AppError::Unauthorized("invalid authorization format".into()))?;

        let claims = decode_jwt(token)
            .map_err(|_| AppError::Unauthorized("invalid token".into()))?;

        Ok(AuthenticatedUser {
            user_id: claims.sub,
            roles: claims.roles,
        })
    }
}

// Usage — extractor is automatic
async fn get_profile(user: AuthenticatedUser) -> Json<ProfileResponse> {
    Json(ProfileResponse { user_id: user.user_id })
}
```
