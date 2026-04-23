# Connection Pool Tuning Guide

Sizing guide for database connection pools by database type, language, and workload.

## Sizing Formulas

### PostgreSQL

**General formula:**

```
connections = ((core_count * 2) + effective_spindle_count)
```

- `core_count`: Number of CPU cores available to the application
- `effective_spindle_count`: Number of independent disk spindles (1 for SSD)

**SSD-optimized formula:**

```
connections = (core_count * 2) + 1
```

**Example:** 4-core server with SSD = `(4 * 2) + 1 = 9` connections per service instance.

### MongoDB

MongoDB uses a different model — each driver maintains its own connection pool:

```
maxPoolSize = min(100, available_connections / service_count)
```

Default: 100 connections per MongoClient. Adjust based on total available connections across all service instances.

### MySQL

MySQL connections are lightweight compared to PostgreSQL:

```
max_connections_per_service = total_max_connections / service_instance_count
```

Default `max_connections` is 151. For high-throughput systems, increase server-side and match pool size.

## Per-Database Defaults

| Database | Default Pool Min | Default Pool Max | Max Server Connections (default) |
|----------|-----------------|-----------------|--------------------------------|
| PostgreSQL | 5 | 20 | 100 |
| MySQL | 5 | 25 | 151 |
| MongoDB | 0 | 100 | 65536 |
| Oracle | 5 | 20 | Based on processes parameter |

## Framework-Specific Configuration

### HikariCP (Java — Spring Boot, Quarkus)

```yaml
# application.yml
spring:
  datasource:
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      connection-timeout: 30000    # 30s
      idle-timeout: 600000         # 10m
      max-lifetime: 1800000        # 30m
      leak-detection-threshold: 60000  # 60s
      validation-timeout: 5000     # 5s
```

**Key rules:**
- `maximum-pool-size` MUST follow the sizing formula
- `max-lifetime` MUST be less than database `idle_in_transaction_session_timeout`
- `minimum-idle` set equal to `maximum-pool-size` for stable workloads

### asyncpg pool (Python — FastAPI, Django)

```python
pool = await asyncpg.create_pool(
    dsn="postgresql://user:pass@host/db",
    min_size=5,
    max_size=20,
    max_inactive_connection_lifetime=600,  # 10m
    command_timeout=30,
)
```

**Key rules:**
- Use `asyncpg` for async frameworks (FastAPI, Starlette)
- Use `psycopg_pool.AsyncConnectionPool` for psycopg3
- Set `max_inactive_connection_lifetime` < server `idle_session_timeout`

### pgx pool (Go — Gin, Echo)

```go
config, _ := pgxpool.ParseConfig(databaseURL)
config.MaxConns = 20
config.MinConns = 5
config.MaxConnLifetime = 30 * time.Minute
config.MaxConnIdleTime = 10 * time.Minute
config.HealthCheckPeriod = 1 * time.Minute

pool, _ := pgxpool.NewWithConfig(ctx, config)
```

**Key rules:**
- Use `pgxpool` (not bare `pgx`) for connection pooling
- Set `MaxConnLifetime` < PostgreSQL `idle_in_transaction_session_timeout`
- `HealthCheckPeriod` ensures stale connections are detected

### sqlx pool (Rust — Axum, Actix)

```rust
let pool = PgPoolOptions::new()
    .max_connections(20)
    .min_connections(5)
    .acquire_timeout(Duration::from_secs(30))
    .idle_timeout(Duration::from_secs(600))
    .max_lifetime(Duration::from_secs(1800))
    .connect(&database_url)
    .await?;
```

**Key rules:**
- Use `sqlx::PgPool` for async connection pooling
- `acquire_timeout` prevents indefinite waits under load
- `idle_timeout` must be less than server-side timeout

### Prisma Connection Pool (TypeScript — NestJS)

```
DATABASE_URL="postgresql://user:pass@host/db?connection_limit=20&pool_timeout=30"
```

```typescript
// schema.prisma
datasource db {
  provider = "postgresql"
  url      = env("DATABASE_URL")
}
```

**Key rules:**
- `connection_limit` sets pool max size
- `pool_timeout` is the max wait time for a connection (seconds)
- For serverless, use Prisma Accelerate or external pooler (PgBouncer)

## Environment-Specific Recommendations

| Environment | Min Pool | Max Pool | Rationale |
|-------------|----------|----------|-----------|
| Development | 2 | 5 | Fast startup, minimal resources |
| Testing | 2 | 5 | Isolation, predictable behavior |
| Staging | 5 | 15 | Mirrors production ratio |
| Production | 5 | 20 | Tuned per sizing formula |

## Monitoring Checklist

| Metric | Source | Alert When |
|--------|--------|------------|
| Pool active connections | Application metrics | > 80% of max |
| Pool idle connections | Application metrics | < min for extended period |
| Connection wait time (p99) | Application metrics | > 1 second |
| Connection timeout count | Application metrics | > 0 per minute |
| Database total connections | Database server | > 80% of max_connections |
| Connection creation rate | Application metrics | Unexpected spikes |

## Anti-Patterns

- Setting pool max to database max_connections (leaves no room for admin or monitoring connections)
- Using pool max = 1 in production (serializes all database access)
- Not setting connection lifetime (stale connections accumulate)
- Not setting idle timeout (idle connections consume server resources)
- Using different pool libraries in the same application (inconsistent behavior)
- Ignoring connection leak detection (leads to pool exhaustion under load)
