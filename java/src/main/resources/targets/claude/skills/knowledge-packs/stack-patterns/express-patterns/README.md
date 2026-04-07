# express-patterns

> Express-specific patterns: middleware architecture, manual/tsyringe/inversify DI, Prisma/TypeORM/Knex data access, express.Router, centralized error handling, dotenv config, supertest testing.

| | |
|---|---|
| **Category** | Stack Pattern |
| **Supplements** | layer-templates |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete pattern reference.

## Key Patterns

- Middleware architecture with chain ordering and error middleware
- Dependency injection patterns (manual, tsyringe, inversify)
- Data access with Prisma, TypeORM, and Knex
- Web/HTTP with express.Router and route composition
- Configuration with dotenv and environment validation
- Testing with supertest, mocking, and integration patterns

## See Also

- [nestjs-patterns](../nestjs-patterns/) — NestJS @Injectable DI, Prisma/TypeORM, Guards/Interceptors/Pipes
- [fastapi-patterns](../fastapi-patterns/) — FastAPI Depends() DI, Pydantic models, async testing
- [layer-templates](../../layer-templates/) — Generic code templates per hexagonal architecture layer
