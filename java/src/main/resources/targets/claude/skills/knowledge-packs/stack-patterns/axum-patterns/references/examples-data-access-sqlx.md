# Example: Data Access (sqlx)

### Connection Pool Setup

```rust
use sqlx::postgres::{PgPool, PgPoolOptions};

pub async fn create_pool(config: &DatabaseConfig) -> Result<PgPool, sqlx::Error> {
    PgPoolOptions::new()
        .max_connections(config.max_connections)
        .min_connections(config.min_connections)
        .acquire_timeout(Duration::from_secs(config.acquire_timeout_secs))
        .idle_timeout(Duration::from_secs(config.idle_timeout_secs))
        .connect(&config.url)
        .await
}
```

### Compile-Time Checked Queries

```rust
use sqlx::FromRow;

#[derive(Debug, FromRow)]
struct MerchantRow {
    id: i64,
    mid: String,
    name: String,
    status: String,
    created_at: chrono::DateTime<chrono::Utc>,
    updated_at: chrono::DateTime<chrono::Utc>,
}

impl MerchantRepository {
    pub async fn find_by_id(&self, id: i64) -> Result<Option<Merchant>, AppError> {
        let row = sqlx::query_as!(
            MerchantRow,
            r#"SELECT id, mid, name, status, created_at, updated_at
               FROM merchants WHERE id = $1"#,
            id
        )
        .fetch_optional(&self.pool)
        .await
        .map_err(AppError::Database)?;

        Ok(row.map(|r| r.into()))
    }

    pub async fn find_all(
        &self,
        page: u32,
        limit: u32,
        status: Option<&str>,
    ) -> Result<(Vec<Merchant>, i64), AppError> {
        let offset = (page * limit) as i64;
        let limit = limit as i64;

        let total = sqlx::query_scalar!(
            r#"SELECT COUNT(*) as "count!" FROM merchants
               WHERE ($1::text IS NULL OR status = $1)"#,
            status
        )
        .fetch_one(&self.pool)
        .await
        .map_err(AppError::Database)?;

        let rows = sqlx::query_as!(
            MerchantRow,
            r#"SELECT id, mid, name, status, created_at, updated_at
               FROM merchants
               WHERE ($1::text IS NULL OR status = $1)
               ORDER BY created_at DESC
               LIMIT $2 OFFSET $3"#,
            status,
            limit,
            offset
        )
        .fetch_all(&self.pool)
        .await
        .map_err(AppError::Database)?;

        Ok((rows.into_iter().map(Into::into).collect(), total))
    }

    pub async fn create(&self, merchant: &NewMerchant) -> Result<Merchant, AppError> {
        let row = sqlx::query_as!(
            MerchantRow,
            r#"INSERT INTO merchants (mid, name, status, created_at, updated_at)
               VALUES ($1, $2, 'active', NOW(), NOW())
               RETURNING id, mid, name, status, created_at, updated_at"#,
            merchant.mid,
            merchant.name
        )
        .fetch_one(&self.pool)
        .await
        .map_err(AppError::Database)?;

        Ok(row.into())
    }
}
```

### Transaction Support

```rust
pub async fn create_with_audit(&self, merchant: &NewMerchant) -> Result<Merchant, AppError> {
    let mut tx = self.pool.begin().await.map_err(AppError::Database)?;

    let row = sqlx::query_as!(
        MerchantRow,
        r#"INSERT INTO merchants (mid, name, status, created_at, updated_at)
           VALUES ($1, $2, 'active', NOW(), NOW())
           RETURNING id, mid, name, status, created_at, updated_at"#,
        merchant.mid,
        merchant.name
    )
    .fetch_one(&mut *tx)
    .await
    .map_err(AppError::Database)?;

    sqlx::query!(
        r#"INSERT INTO audit_logs (entity_type, entity_id, action, created_at)
           VALUES ('merchant', $1, 'create', NOW())"#,
        row.id
    )
    .execute(&mut *tx)
    .await
    .map_err(AppError::Database)?;

    tx.commit().await.map_err(AppError::Database)?;

    Ok(row.into())
}
```

### Migrations with sqlx-cli

```bash
# Install
cargo install sqlx-cli --no-default-features --features postgres

# Create migration
sqlx migrate add create_merchants

# Run migrations
sqlx migrate run

# Revert last migration
sqlx migrate revert

# Prepare offline query data (for CI without DB)
cargo sqlx prepare
```
