from __future__ import annotations

from dataclasses import dataclass
from typing import List

from claude_setup.models import ProjectConfig


@dataclass(frozen=True)
class CoreKpRoute:
    """Maps a core rule source file to a knowledge pack destination."""

    source_file: str
    kp_name: str
    dest_file: str


@dataclass(frozen=True)
class ConditionalCoreKpRoute(CoreKpRoute):
    """Route that is conditionally included based on config values."""

    condition_field: str = ""
    condition_exclude: str = ""


CORE_TO_KP_MAPPING: List[CoreKpRoute] = [
    CoreKpRoute("01-clean-code.md", "coding-standards", "clean-code.md"),
    CoreKpRoute("02-solid-principles.md", "coding-standards", "solid-principles.md"),
    CoreKpRoute("03-testing-philosophy.md", "testing", "testing-philosophy.md"),
    CoreKpRoute("05-architecture-principles.md", "architecture", "architecture-principles.md"),
    CoreKpRoute("06-api-design-principles.md", "api-design", "api-design-principles.md"),
    CoreKpRoute("07-security-principles.md", "security", "security-principles.md"),
    CoreKpRoute("08-observability-principles.md", "observability", "observability-principles.md"),
    CoreKpRoute("09-resilience-principles.md", "resilience", "resilience-principles.md"),
    CoreKpRoute("10-infrastructure-principles.md", "infrastructure", "infrastructure-principles.md"),
    CoreKpRoute("11-database-principles.md", "database-patterns", "database-principles.md"),
    CoreKpRoute("13-story-decomposition.md", "story-planning", "story-decomposition.md"),
]

CONDITIONAL_CORE_KP: List[ConditionalCoreKpRoute] = [
    ConditionalCoreKpRoute(
        source_file="12-cloud-native-principles.md",
        kp_name="infrastructure",
        dest_file="cloud-native-principles.md",
        condition_field="architecture_style",
        condition_exclude="library",
    ),
]


def get_active_routes(config: ProjectConfig) -> List[CoreKpRoute]:
    """Return all routes whose conditions are met for the given config."""
    routes: List[CoreKpRoute] = list(CORE_TO_KP_MAPPING)
    for route in CONDITIONAL_CORE_KP:
        config_value = _resolve_condition_value(config, route.condition_field)
        if config_value != route.condition_exclude:
            routes.append(route)
    return routes


def _resolve_condition_value(config: ProjectConfig, field: str) -> str:
    """Resolve a dotted condition field to a config value."""
    if field == "architecture_style":
        return config.architecture.style
    return ""
