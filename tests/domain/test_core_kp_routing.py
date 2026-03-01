from __future__ import annotations

import copy

import pytest

from claude_setup.domain.core_kp_routing import (
    CONDITIONAL_CORE_KP,
    CORE_TO_KP_MAPPING,
    CoreKpRoute,
    get_active_routes,
)
from claude_setup.models import ProjectConfig

EXPECTED_UNCONDITIONAL_ROUTES = 11
EXPECTED_CONDITIONAL_ROUTES = 1


class TestCoreToKpMapping:
    def test_has_expected_count(self) -> None:
        assert len(CORE_TO_KP_MAPPING) == EXPECTED_UNCONDITIONAL_ROUTES

    def test_conditional_has_expected_count(self) -> None:
        assert len(CONDITIONAL_CORE_KP) == EXPECTED_CONDITIONAL_ROUTES

    @pytest.mark.parametrize(
        "route",
        CORE_TO_KP_MAPPING,
        ids=[r.source_file for r in CORE_TO_KP_MAPPING],
    )
    def test_route_fields_non_empty(self, route: CoreKpRoute) -> None:
        assert route.source_file
        assert route.kp_name
        assert route.dest_file


class TestGetActiveRoutes:
    def _make_config(self, style: str) -> ProjectConfig:
        data = {
            "project": {"name": "test", "purpose": "Test"},
            "architecture": {"style": style},
            "interfaces": [{"type": "cli"}],
            "language": {"name": "python", "version": "3.9"},
            "framework": {"name": "click", "version": "8.1"},
        }
        return ProjectConfig.from_dict(data)

    def test_library_excludes_cloud_native(self) -> None:
        config = self._make_config("library")
        routes = get_active_routes(config)
        names = [r.source_file for r in routes]
        assert "12-cloud-native-principles.md" not in names
        assert len(routes) == EXPECTED_UNCONDITIONAL_ROUTES

    def test_non_library_includes_cloud_native(self) -> None:
        config = self._make_config("microservice")
        routes = get_active_routes(config)
        names = [r.source_file for r in routes]
        assert "12-cloud-native-principles.md" in names
        assert len(routes) == EXPECTED_UNCONDITIONAL_ROUTES + 1

    def test_all_routes_are_core_kp_route(self) -> None:
        config = self._make_config("microservice")
        routes = get_active_routes(config)
        for route in routes:
            assert isinstance(route, CoreKpRoute)
