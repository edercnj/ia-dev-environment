# gin-patterns

> Gin-specific patterns: middleware chains, gin.Context, go-playground/validator, GORM/sqlx data access, viper config, httptest testing, centralized error handling.

| | |
|---|---|
| **Category** | Stack Pattern |
| **Supplements** | layer-templates |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete pattern reference.

## Key Patterns

- Middleware chain with auth, recovery, and logging middleware
- Request handling with gin.Context, binding, and validation
- Data access with GORM and sqlx patterns
- Configuration with viper and environment-based loading
- Testing with httptest and gin test mode
- Centralized error handling and error response formatting

## See Also

- [axum-patterns](../axum-patterns/) — Axum extractors, Router composition, Tower middleware
- [ktor-patterns](../ktor-patterns/) — Ktor routing DSL, Exposed ORM, Koin DI
- [layer-templates](../../layer-templates/) — Generic code templates per hexagonal architecture layer
