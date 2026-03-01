from __future__ import annotations

import warnings
from pathlib import Path
from typing import Any, Dict, List, Tuple

import yaml

from claude_setup.exceptions import ConfigValidationError
from claude_setup.models import ProjectConfig

TYPE_MAPPING: Dict[str, Tuple[str, List[Dict[str, str]]]] = {
    "api": ("microservice", [{"type": "rest"}]),
    "cli": ("library", [{"type": "cli"}]),
    "library": ("library", []),
    "worker": ("microservice", [{"type": "event-consumer"}]),
    "fullstack": ("monolith", [{"type": "rest"}]),
}

STACK_MAPPING: Dict[str, Tuple[str, str, str, str]] = {
    "java-quarkus": ("java", "21", "quarkus", "3.17"),
    "java-spring": ("java", "21", "spring-boot", "3.4"),
    "python-fastapi": ("python", "3.12", "fastapi", "0.115"),
    "python-click-cli": ("python", "3.9", "click", "8.1"),
    "go-gin": ("go", "1.23", "gin", "1.10"),
    "kotlin-ktor": ("kotlin", "2.1", "ktor", "3.0"),
    "typescript-nestjs": ("typescript", "5.7", "nestjs", "10.4"),
    "rust-axum": ("rust", "1.83", "axum", "0.8"),
}

REQUIRED_SECTIONS = ("project", "architecture", "interfaces", "language", "framework")

DEFAULT_TYPE_MAPPING: Tuple[str, List[Dict[str, str]]] = (
    "microservice",
    [{"type": "rest"}],
)


def detect_v2_format(data: Dict[str, Any]) -> bool:
    """Check if data uses legacy v2 format."""
    if data.get("type") in TYPE_MAPPING:
        return True
    if data.get("stack") in STACK_MAPPING:
        return True
    return False


def _build_architecture_section(
    data: Dict[str, Any],
) -> Dict[str, Any]:
    """Build architecture and interfaces from v2 type field."""
    v2_type = data.get("type", "")
    style, interfaces = TYPE_MAPPING.get(v2_type, DEFAULT_TYPE_MAPPING)
    return {"style": style}, interfaces


def _build_language_framework(
    data: Dict[str, Any],
) -> Tuple[Dict[str, str], Dict[str, str]]:
    """Build language and framework sections from v2 stack."""
    stack_key = data.get("stack", "")
    if stack_key not in STACK_MAPPING:
        raise ConfigValidationError([f"Unknown stack: {stack_key}"])
    lang_name, lang_ver, fw_name, fw_ver = STACK_MAPPING[stack_key]
    language = {"name": lang_name, "version": lang_ver}
    framework = {"name": fw_name, "version": fw_ver}
    return language, framework


def migrate_v2_to_v3(data: Dict[str, Any]) -> Dict[str, Any]:
    """Transform v2 config dict into v3 structure."""
    result: Dict[str, Any] = {}
    if "project" in data:
        result["project"] = dict(data["project"])
    else:
        result["project"] = {"name": "unnamed", "purpose": ""}
    arch, interfaces = _build_architecture_section(data)
    result["architecture"] = arch
    result["interfaces"] = list(interfaces)
    language, framework = _build_language_framework(data)
    result["language"] = language
    result["framework"] = framework
    warnings.warn(
        "Config uses legacy v2 format. Auto-migrating to v3.",
        stacklevel=2,
    )
    return result


def validate_config(data: Dict[str, Any]) -> None:
    """Validate that all required top-level sections exist."""
    if data is None:
        raise ConfigValidationError(list(REQUIRED_SECTIONS))
    missing = [s for s in REQUIRED_SECTIONS if s not in data]
    if missing:
        raise ConfigValidationError(missing)


def load_config(path: Path) -> ProjectConfig:
    """Load, migrate if needed, validate, and return ProjectConfig."""
    content = path.read_text(encoding="utf-8")
    data = yaml.safe_load(content)
    if data is None:
        raise ConfigValidationError(list(REQUIRED_SECTIONS))
    if detect_v2_format(data):
        data = migrate_v2_to_v3(data)
    validate_config(data)
    return ProjectConfig.from_dict(data)
