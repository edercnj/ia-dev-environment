# Example: Testing

### Handler Unit Test with oneshot

```rust
#[cfg(test)]
mod tests {
    use super::*;
    use axum::body::Body;
    use axum::http::{Request, StatusCode};
    use tower::ServiceExt; // for oneshot

    fn test_app(state: AppState) -> Router {
        Router::new()
            .route("/merchants/{id}", get(get_merchant))
            .with_state(state)
    }

    #[tokio::test]
    async fn test_get_merchant_found() {
        let mock_service = Arc::new(MockMerchantService::new());
        mock_service
            .expect_find_by_id()
            .with(eq(1))
            .returning(|_| Ok(Some(Merchant {
                id: 1,
                mid: "12345".into(),
                name: "Test Shop".into(),
                status: "active".into(),
            })));

        let state = AppState {
            merchant_service: mock_service,
            db_pool: create_test_pool().await,
        };

        let response = test_app(state)
            .oneshot(
                Request::builder()
                    .uri("/merchants/1")
                    .body(Body::empty())
                    .unwrap(),
            )
            .await
            .unwrap();

        assert_eq!(response.status(), StatusCode::OK);

        let body = axum::body::to_bytes(response.into_body(), usize::MAX).await.unwrap();
        let merchant: MerchantResponse = serde_json::from_slice(&body).unwrap();
        assert_eq!(merchant.mid, "12345");
    }

    #[tokio::test]
    async fn test_get_merchant_not_found() {
        let mock_service = Arc::new(MockMerchantService::new());
        mock_service
            .expect_find_by_id()
            .with(eq(999))
            .returning(|_| Ok(None));

        let state = AppState {
            merchant_service: mock_service,
            db_pool: create_test_pool().await,
        };

        let response = test_app(state)
            .oneshot(
                Request::builder()
                    .uri("/merchants/999")
                    .body(Body::empty())
                    .unwrap(),
            )
            .await
            .unwrap();

        assert_eq!(response.status(), StatusCode::NOT_FOUND);
    }

    #[tokio::test]
    async fn test_create_merchant() {
        let mock_service = Arc::new(MockMerchantService::new());
        mock_service
            .expect_create()
            .returning(|_| Ok(Merchant {
                id: 1,
                mid: "12345".into(),
                name: "New Shop".into(),
                status: "active".into(),
            }));

        let state = AppState {
            merchant_service: mock_service,
            db_pool: create_test_pool().await,
        };

        let app = Router::new()
            .route("/merchants", post(create_merchant))
            .with_state(state);

        let body = serde_json::json!({
            "mid": "12345",
            "name": "New Shop"
        });

        let response = app
            .oneshot(
                Request::builder()
                    .method("POST")
                    .uri("/merchants")
                    .header("Content-Type", "application/json")
                    .body(Body::from(serde_json::to_string(&body).unwrap()))
                    .unwrap(),
            )
            .await
            .unwrap();

        assert_eq!(response.status(), StatusCode::CREATED);
    }
}
```

### Integration Test with Test Database

```rust
#[cfg(test)]
mod integration {
    use sqlx::PgPool;

    #[sqlx::test(migrations = "migrations")]
    async fn test_create_and_find_merchant(pool: PgPool) {
        let repo = MerchantRepository::new(pool);

        let new_merchant = NewMerchant {
            mid: "12345".into(),
            name: "Integration Test Shop".into(),
        };

        let created = repo.create(&new_merchant).await.unwrap();
        assert!(created.id > 0);
        assert_eq!(created.mid, "12345");

        let found = repo.find_by_id(created.id).await.unwrap();
        assert!(found.is_some());
        assert_eq!(found.unwrap().name, "Integration Test Shop");
    }

    #[sqlx::test(migrations = "migrations")]
    async fn test_list_merchants_with_pagination(pool: PgPool) {
        let repo = MerchantRepository::new(pool);

        for i in 0..5 {
            repo.create(&NewMerchant {
                mid: format!("{:05}", i),
                name: format!("Shop {}", i),
            }).await.unwrap();
        }

        let (merchants, total) = repo.find_all(0, 2, None).await.unwrap();
        assert_eq!(merchants.len(), 2);
        assert_eq!(total, 5);
    }
}
```
