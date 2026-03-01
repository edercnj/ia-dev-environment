from __future__ import annotations

from typing import Dict

FRAMEWORK_STACK_PACK: Dict[str, str] = {
    "quarkus": "quarkus-patterns",
    "spring-boot": "spring-patterns",
    "nestjs": "nestjs-patterns",
    "express": "express-patterns",
    "fastapi": "fastapi-patterns",
    "django": "django-patterns",
    "gin": "gin-patterns",
    "ktor": "ktor-patterns",
    "axum": "axum-patterns",
    "dotnet": "dotnet-patterns",
    "click": "click-cli-patterns",
}


def get_stack_pack_name(framework: str) -> str:
    """Return the knowledge pack directory name for a framework.

    Returns empty string for unknown frameworks.
    """
    return FRAMEWORK_STACK_PACK.get(framework, "")
