from __future__ import annotations

import pytest

from ia_dev_env.domain.core_kp_routing import (
    CONDITIONAL_CORE_KP,
    CORE_TO_KP_MAPPING,
    ConditionalCoreKpRoute,
    CoreKpRoute,
    get_active_routes,
)
from ia_dev_env.models import ProjectConfig

EXPECTED_UNCONDITIONAL_COUNT = 11
EXPECTED_CONDITIONAL_COUNT = 1
EXPECTED_TOTAL_NON_LIBRARY = 12


class TestCoreKpRoutingConstants:

    def test_core_to_kp_mapping_has_expected_count(self) -> None:
        assert len(CORE_TO_KP_MAPPING) == EXPECTED_UNCONDITIONAL_COUNT

    def test_conditional_core_kp_has_one_entry(self) -> None:
        assert len(CONDITIONAL_CORE_KP) == EXPECTED_CONDITIONAL_COUNT

    def test_conditional_entry_is_cloud_native(self) -> None:
        route = CONDITIONAL_CORE_KP[0]
        assert route.source_file == "12-cloud-native-principles.md"
        assert route.condition_field == "architecture_style"
        assert route.condition_exclude == "library"

    @pytest.mark.parametrize(
        "route",
        CORE_TO_KP_MAPPING,
        ids=[r.source_file for r in CORE_TO_KP_MAPPING],
    )
    def test_core_route_fields_non_empty(self, route: CoreKpRoute) -> None:
        assert route.source_file
        assert route.kp_name
        assert route.dest_file

    def test_all_routes_are_frozen(self) -> None:
        for route in CORE_TO_KP_MAPPING:
            with pytest.raises(AttributeError):
                route.source_file = "changed"  # type: ignore[misc]


class TestGetActiveRoutes:

    def test_get_active_routes_library_excludes_cloud_native(
        self, minimal_project_dict: dict,
    ) -> None:
        config = ProjectConfig.from_dict(minimal_project_dict)
        routes = get_active_routes(config)
        source_files = [r.source_file for r in routes]
        assert "12-cloud-native-principles.md" not in source_files
        assert len(routes) == EXPECTED_UNCONDITIONAL_COUNT

    def test_get_active_routes_non_library_includes_all(
        self, minimal_project_dict: dict,
    ) -> None:
        minimal_project_dict["architecture"]["style"] = "microservice"
        config = ProjectConfig.from_dict(minimal_project_dict)
        routes = get_active_routes(config)
        source_files = [r.source_file for r in routes]
        assert "12-cloud-native-principles.md" in source_files
        assert len(routes) == EXPECTED_TOTAL_NON_LIBRARY

    def test_get_active_routes_returns_core_kp_route_instances(
        self, minimal_project_dict: dict,
    ) -> None:
        config = ProjectConfig.from_dict(minimal_project_dict)
        routes = get_active_routes(config)
        for route in routes:
            assert isinstance(route, CoreKpRoute)
