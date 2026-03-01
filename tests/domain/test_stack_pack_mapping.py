from __future__ import annotations

import pytest

from claude_setup.domain.stack_pack_mapping import (
    FRAMEWORK_STACK_PACK,
    get_stack_pack_name,
)

EXPECTED_FRAMEWORK_COUNT = 11


class TestFrameworkStackPack:
    def test_dict_has_expected_count(self) -> None:
        assert len(FRAMEWORK_STACK_PACK) == EXPECTED_FRAMEWORK_COUNT


class TestGetStackPackName:
    @pytest.mark.parametrize(
        "framework,expected",
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
            "fastapi", "django", "gin", "ktor", "axum",
            "dotnet", "click",
        ],
    )
    def test_known_framework_returns_pack_name(
        self, framework: str, expected: str,
    ) -> None:
        assert get_stack_pack_name(framework) == expected

    def test_unknown_framework_returns_empty(self) -> None:
        assert get_stack_pack_name("unknown") == ""

    def test_empty_string_returns_empty(self) -> None:
        assert get_stack_pack_name("") == ""

    def test_uppercase_returns_empty(self) -> None:
        assert get_stack_pack_name("QUARKUS") == ""
