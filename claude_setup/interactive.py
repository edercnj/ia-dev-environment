from __future__ import annotations

from typing import Dict, List, Tuple

import click

from claude_setup.models import (
    ArchitectureConfig,
    FrameworkConfig,
    InterfaceConfig,
    LanguageConfig,
    ProjectConfig,
    ProjectIdentity,
)

ARCHITECTURE_CHOICES = ["library", "microservice", "monolith"]
LANGUAGE_CHOICES = ["python", "java", "go", "kotlin", "typescript", "rust"]
INTERFACE_CHOICES = ["rest", "grpc", "cli", "event-consumer", "event-producer"]
BUILD_TOOL_CHOICES = ["pip", "maven", "gradle", "go", "cargo", "npm"]

FRAMEWORK_CHOICES: Dict[str, List[str]] = {
    "python": ["fastapi", "click", "django", "flask"],
    "java": ["quarkus", "spring-boot"],
    "go": ["stdlib", "gin", "fiber"],
    "kotlin": ["ktor"],
    "typescript": ["nestjs", "express", "fastify"],
    "rust": ["axum", "actix"],
}


def _prompt_input(label: str, default: str = "") -> str:
    """Prompt for free-text input."""
    return click.prompt(label, default=default, type=str)


def _prompt_select(label: str, choices: List[str]) -> str:
    """Prompt with constrained choices."""
    return click.prompt(label, type=click.Choice(choices))


def _prompt_yesno(label: str, default: bool = False) -> bool:
    """Prompt for yes/no confirmation."""
    return click.confirm(label, default=default)


def _collect_project_identity() -> ProjectIdentity:
    """Collect project name and purpose from user."""
    name = _prompt_input("Project name")
    purpose = _prompt_input("Project purpose")
    return ProjectIdentity(name=name, purpose=purpose)


def _collect_architecture() -> ArchitectureConfig:
    """Collect architecture settings from user."""
    style = _prompt_select("Architecture style", ARCHITECTURE_CHOICES)
    domain_driven = _prompt_yesno("Domain-driven design", default=False)
    event_driven = _prompt_yesno("Event-driven", default=False)
    return ArchitectureConfig(
        style=style,
        domain_driven=domain_driven,
        event_driven=event_driven,
    )


def _collect_interfaces() -> List[InterfaceConfig]:
    """Collect interface types from user."""
    iface_type = _prompt_select("Interface type", INTERFACE_CHOICES)
    return [InterfaceConfig(type=iface_type)]


def _collect_language_and_framework() -> Tuple[LanguageConfig, FrameworkConfig]:
    """Collect language and framework settings from user."""
    lang_name = _prompt_select("Language", LANGUAGE_CHOICES)
    lang_version = _prompt_input("Language version")
    fw_choices = FRAMEWORK_CHOICES.get(lang_name, ["other"])
    fw_name = _prompt_select("Framework", fw_choices)
    fw_version = _prompt_input("Framework version")
    build_tool = _prompt_select("Build tool", BUILD_TOOL_CHOICES)
    language = LanguageConfig(name=lang_name, version=lang_version)
    framework = FrameworkConfig(
        name=fw_name,
        version=fw_version,
        build_tool=build_tool,
    )
    return language, framework


def run_interactive() -> ProjectConfig:
    """Prompt user for all config fields interactively."""
    project = _collect_project_identity()
    architecture = _collect_architecture()
    interfaces = _collect_interfaces()
    language, framework = _collect_language_and_framework()
    return ProjectConfig(
        project=project,
        architecture=architecture,
        interfaces=interfaces,
        language=language,
        framework=framework,
    )
