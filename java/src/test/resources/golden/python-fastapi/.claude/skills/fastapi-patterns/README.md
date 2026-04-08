# fastapi-patterns

> FastAPI-specific patterns: Depends() DI, SQLAlchemy/Tortoise ORM data access, Pydantic models, APIRouter, BaseSettings config, httpx async testing, uvicorn deployment.

| | |
|---|---|
| **Category** | Stack Pattern |
| **Supplements** | layer-templates |

> **Full content**: See [SKILL.md](./SKILL.md) for the complete pattern reference.

## Key Patterns

- Dependency injection with Depends() and yield dependencies
- Data access with SQLAlchemy async and Tortoise ORM
- Web/HTTP with APIRouter, Pydantic request/response models
- Configuration with BaseSettings and environment validation
- Testing with httpx AsyncClient and dependency overrides
- Build and deployment with uvicorn and Docker

## See Also

- [django-patterns](../django-patterns/) — Django ORM, DRF serializers/viewsets, TestCase patterns
- [express-patterns](../express-patterns/) — Express middleware architecture, centralized error handling
- [layer-templates](../../layer-templates/) — Generic code templates per hexagonal architecture layer
