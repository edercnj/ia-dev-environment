from __future__ import annotations

import pytest

from claude_setup.domain.stack_pack_mapping import (
    FRAMEWORK_STACK_PACK,
    get_stack_pack_name,
)

EXPECTED_MAPPING_COUNT = 11


class TestStackPackMapping:

    @pytest.mark.parametrize(
        "framework, expected",
        [
            ("quarkus", "quarkus-patterns"),
            ("spring-boot", "spring-patterns"),
            ("nestjs", "nestjs-patterns"),
            ("express", "express-patterns"),
            ("fastapi", "fastapi-patterns"),
            ("django", "django-patterns"),
            ("gin", "gin-patterns"),
            ("ktor", "ktor-patterns"),
            ("axum", "axum-patterns"),
            ("dotnet", "dotnet-patterns"),
            ("click", "click-cli-patterns"),
        ],
        ids=[
            "quarkus", "spring-boot", "nestjs", "express",
            "fastapi", "django", "gin", "ktor",
            "axum", "dotnet", "click",
        ],
    )
    def test_get_stack_pack_name_known_framework_returns_pack_name(
        self, framework: str, expected: str,
    ) -> None:
        assert get_stack_pack_name(framework) == expected

    def test_get_stack_pack_name_unknown_framework_returns_empty(self) -> None:
        assert get_stack_pack_name("unknown-fw") == ""

    def test_get_stack_pack_name_empty_string_returns_empty(self) -> None:
        assert get_stack_pack_name("") == ""

    def test_get_stack_pack_name_uppercase_returns_empty(self) -> None:
        assert get_stack_pack_name("QUARKUS") == ""

    def test_framework_stack_pack_has_expected_count(self) -> None:
        assert len(FRAMEWORK_STACK_PACK) == EXPECTED_MAPPING_COUNT
