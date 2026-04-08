# axum-patterns

> Axum-specific patterns: extractors, Router composition, Tower middleware, sqlx async data access, config crate layered config, tokio testing, IntoResponse error handling.

| | |
|---|---|
| **Category** | Stack Pattern |
| **Supplements** | layer-templates |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete pattern reference.

## Key Patterns

- Extractors (Path, Query, Json, State) and ordering rules
- Router composition with nesting and method routing
- Tower middleware integration (layers, services, interception)
- Async data access with sqlx (queries, pools, transactions)
- Layered configuration, testing with tokio, and anti-patterns
- IntoResponse error handling and AppError patterns

## See Also

- [gin-patterns](../gin-patterns/) — Gin middleware chains, GORM/sqlx data access, viper config
- [fastapi-patterns](../fastapi-patterns/) — FastAPI Depends() DI, SQLAlchemy data access, Pydantic models
- [layer-templates](../../layer-templates/) — Generic code templates per hexagonal architecture layer
