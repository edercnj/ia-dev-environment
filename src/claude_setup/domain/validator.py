from __future__ import annotations

from pathlib import Path
from typing import List, Optional

from claude_setup.domain.stack_mapping import (
    FRAMEWORK_LANGUAGE_RULES,
    NATIVE_SUPPORTED_FRAMEWORKS,
    VALID_ARCHITECTURE_STYLES,
    VALID_INTERFACE_TYPES,
)
from claude_setup.models import ProjectConfig

JAVA_17_MINIMUM = 17
PYTHON_310_MINOR = 10
FRAMEWORK_VERSION_3 = 3
FRAMEWORK_VERSION_5 = 5

EXPECTED_DIRECTORIES = (
    "skills",
    ".claude/rules",
)


def validate_stack(config: ProjectConfig) -> List[str]:
    """Run all validations and return aggregated errors."""
    errors: List[str] = []
    errors.extend(_validate_language_framework(config))
    errors.extend(_validate_version_requirements(config))
    errors.extend(_validate_native_build(config))
    errors.extend(_validate_interface_types(config))
    errors.extend(_validate_architecture_style(config))
    return errors


def _validate_language_framework(
    config: ProjectConfig,
) -> List[str]:
    """Check language-framework compatibility."""
    framework_name = config.framework.name
    language_name = config.language.name
    valid_languages = FRAMEWORK_LANGUAGE_RULES.get(framework_name)
    if valid_languages is None:
        return []
    if language_name not in valid_languages:
        expected = ", ".join(valid_languages)
        return [
            f"Framework '{framework_name}' requires language "
            f"'{expected}', got '{language_name}'"
        ]
    return []


def _validate_version_requirements(
    config: ProjectConfig,
) -> List[str]:
    """Check version constraints for known combinations."""
    errors: List[str] = []
    errors.extend(_check_java_framework_version(config))
    errors.extend(_check_django_python_version(config))
    return errors


def _check_java_framework_version(
    config: ProjectConfig,
) -> List[str]:
    """Check Quarkus/Spring Boot Java version requirements."""
    fw = config.framework.name
    lang = config.language.name
    if lang != "java" or fw not in ("quarkus", "spring-boot"):
        return []
    fw_major = _parse_major_version(config.framework.version)
    lang_major = _parse_major_version(config.language.version)
    if lang_major is None:
        return []
    return _check_java_17_requirement(fw, fw_major, lang_major)


def _check_java_17_requirement(
    fw: str,
    fw_major: Optional[int],
    lang_major: int,
) -> List[str]:
    """Return error if Java 17+ is required but not met."""
    needs_check = (
        fw_major is not None
        and fw_major >= FRAMEWORK_VERSION_3
        and fw in ("quarkus", "spring-boot")
    )
    if needs_check and lang_major < JAVA_17_MINIMUM:
        return [
            f"{fw.title()} {fw_major}.x requires Java 17+, "
            f"got Java {lang_major}"
        ]
    return []


def _check_django_python_version(
    config: ProjectConfig,
) -> List[str]:
    """Check Django 5.x Python version requirement."""
    if config.framework.name != "django":
        return []
    fw_major = _parse_major_version(config.framework.version)
    if fw_major is None or fw_major < FRAMEWORK_VERSION_5:
        return []
    py_minor = _parse_minor_version(config.language.version)
    if py_minor is None:
        return []
    if py_minor < PYTHON_310_MINOR:
        return [
            f"Django 5.x requires Python 3.10+, "
            f"got Python {config.language.version}"
        ]
    return []


def _validate_native_build(config: ProjectConfig) -> List[str]:
    """Check native_build compatibility with framework."""
    if not config.framework.native_build:
        return []
    fw = config.framework.name
    if fw not in NATIVE_SUPPORTED_FRAMEWORKS:
        return [
            f"Native build is not supported for "
            f"framework '{fw}'"
        ]
    return []


def _validate_interface_types(
    config: ProjectConfig,
) -> List[str]:
    """Check all interface types are valid."""
    errors: List[str] = []
    for iface in config.interfaces:
        if iface.type not in VALID_INTERFACE_TYPES:
            errors.append(
                f"Invalid interface type: '{iface.type}'. "
                f"Valid: {', '.join(VALID_INTERFACE_TYPES)}"
            )
    return errors


def _validate_architecture_style(
    config: ProjectConfig,
) -> List[str]:
    """Check architecture style is valid."""
    style = config.architecture.style
    if style not in VALID_ARCHITECTURE_STYLES:
        return [
            f"Invalid architecture style: '{style}'. "
            f"Valid: {', '.join(VALID_ARCHITECTURE_STYLES)}"
        ]
    return []


def verify_cross_references(
    config: ProjectConfig,
    resources_dir: str,
) -> List[str]:
    """Verify referenced directories exist on filesystem."""
    errors: List[str] = []
    src = Path(resources_dir)
    if not src.is_dir():
        return [f"Source directory does not exist: {resources_dir}"]
    for directory in EXPECTED_DIRECTORIES:
        if not (src / directory).is_dir():
            errors.append(
                f"Expected directory not found: {directory}"
            )
    return errors


def _parse_major_version(version: str) -> Optional[int]:
    """Extract major version number from version string."""
    if not version:
        return None
    parts = version.split(".")
    try:
        return int(parts[0])
    except ValueError:
        return None


def _parse_minor_version(version: str) -> Optional[int]:
    """Extract minor version number from version string."""
    if not version:
        return None
    parts = version.split(".")
    if len(parts) < 2:
        return None
    try:
        return int(parts[1])
    except ValueError:
        return None
