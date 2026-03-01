from __future__ import annotations

from typing import Dict, List

import pytest

from claude_setup.config import (
    STACK_MAPPING,
    TYPE_MAPPING,
    migrate_v2_to_v3,
)
from claude_setup.models import ProjectConfig


@pytest.mark.parametrize(
    "v2_type, expected_style, expected_interfaces",
    [
        ("api", "microservice", [{"type": "rest"}]),
        ("cli", "library", [{"type": "cli"}]),
        ("library", "library", []),
        ("worker", "microservice", [{"type": "event-consumer"}]),
        ("fullstack", "monolith", [{"type": "rest"}]),
    ],
    ids=["api", "cli", "library", "worker", "fullstack"],
)
def test_v2_type_mapping_produces_correct_architecture(
    v2_type: str,
    expected_style: str,
    expected_interfaces: List[Dict[str, str]],
) -> None:
    data = {
        "type": v2_type,
        "stack": "java-quarkus",
        "project": {"name": "test", "purpose": "test"},
    }
    result = migrate_v2_to_v3(data)
    assert result["architecture"]["style"] == expected_style
    assert result["interfaces"] == expected_interfaces


@pytest.mark.parametrize(
    "v2_stack, expected_lang, expected_ver, expected_fw, expected_fw_ver",
    [
        ("java-quarkus", "java", "21", "quarkus", "3.17"),
        ("java-spring", "java", "21", "spring-boot", "3.4"),
        ("python-fastapi", "python", "3.12", "fastapi", "0.115"),
        ("python-click-cli", "python", "3.9", "click", "8.1"),
        ("go-gin", "go", "1.23", "gin", "1.10"),
        ("kotlin-ktor", "kotlin", "2.1", "ktor", "3.0"),
        ("typescript-nestjs", "typescript", "5.7", "nestjs", "10.4"),
        ("rust-axum", "rust", "1.83", "axum", "0.8"),
    ],
    ids=[
        "java-quarkus",
        "java-spring",
        "python-fastapi",
        "python-click-cli",
        "go-gin",
        "kotlin-ktor",
        "typescript-nestjs",
        "rust-axum",
    ],
)
def test_v2_stack_mapping_produces_correct_language_and_framework(
    v2_stack: str,
    expected_lang: str,
    expected_ver: str,
    expected_fw: str,
    expected_fw_ver: str,
) -> None:
    data = {
        "type": "api",
        "stack": v2_stack,
        "project": {"name": "test", "purpose": "test"},
    }
    result = migrate_v2_to_v3(data)
    assert result["language"]["name"] == expected_lang
    assert result["language"]["version"] == expected_ver
    assert result["framework"]["name"] == expected_fw
    assert result["framework"]["version"] == expected_fw_ver


@pytest.mark.parametrize(
    "v2_type, v2_stack",
    [
        ("api", "java-quarkus"),
        ("api", "java-spring"),
        ("api", "python-fastapi"),
        ("cli", "python-click-cli"),
        ("api", "go-gin"),
        ("api", "kotlin-ktor"),
        ("api", "typescript-nestjs"),
        ("api", "rust-axum"),
    ],
    ids=[
        "api-java-quarkus",
        "api-java-spring",
        "api-python-fastapi",
        "cli-python-click-cli",
        "api-go-gin",
        "api-kotlin-ktor",
        "api-typescript-nestjs",
        "api-rust-axum",
    ],
)
def test_v2_full_roundtrip_produces_valid_project_config(
    v2_type: str,
    v2_stack: str,
) -> None:
    data = {
        "type": v2_type,
        "stack": v2_stack,
        "project": {"name": "roundtrip", "purpose": "test"},
    }
    migrated = migrate_v2_to_v3(data)
    config = ProjectConfig.from_dict(migrated)
    assert isinstance(config, ProjectConfig)
    assert config.project.name == "roundtrip"
