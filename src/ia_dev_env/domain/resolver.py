from __future__ import annotations

from typing import Dict, List

from ia_dev_env.domain.resolved_stack import ResolvedStack
from ia_dev_env.domain.stack_mapping import (
    DEFAULT_DOCKER_IMAGE,
    DEFAULT_HEALTH_PATH,
    DEFAULT_PORT_FALLBACK,
    DOCKER_BASE_IMAGES,
    FRAMEWORK_HEALTH_PATHS,
    FRAMEWORK_PORTS,
    INTERFACE_PROTOCOL_MAP,
    LANGUAGE_COMMANDS,
    NATIVE_SUPPORTED_FRAMEWORKS,
)
from ia_dev_env.models import (
    FrameworkConfig,
    LanguageConfig,
    ProjectConfig,
)

EMPTY_COMMAND = ""
CLI_INTERFACE = "cli"
EVENT_CONSUMER_INTERFACE = "event-consumer"
REST_INTERFACE = "rest"


def resolve_stack(config: ProjectConfig) -> ResolvedStack:
    """Resolve all derived stack values from project config."""
    commands = _resolve_commands(config.language, config.framework)
    protocols = _derive_protocols(config)
    return ResolvedStack(
        build_cmd=commands.get("build_cmd", EMPTY_COMMAND),
        test_cmd=commands.get("test_cmd", EMPTY_COMMAND),
        compile_cmd=commands.get("compile_cmd", EMPTY_COMMAND),
        coverage_cmd=commands.get("coverage_cmd", EMPTY_COMMAND),
        docker_base_image=_resolve_docker_image(config.language),
        health_path=_resolve_health_path(config.framework),
        package_manager=commands.get("package_manager", EMPTY_COMMAND),
        default_port=_resolve_default_port(config.framework),
        file_extension=commands.get("file_extension", EMPTY_COMMAND),
        build_file=commands.get("build_file", EMPTY_COMMAND),
        native_supported=_infer_native_build(config),
        project_type=_derive_project_type(config),
        protocols=protocols,
    )


def _resolve_commands(
    language: LanguageConfig,
    framework: FrameworkConfig,
) -> Dict[str, str]:
    """Look up commands by (language, build_tool) key."""
    key = (language.name, framework.build_tool)
    return LANGUAGE_COMMANDS.get(key, {})


def _resolve_docker_image(language: LanguageConfig) -> str:
    """Resolve Docker base image from language and version."""
    template = DOCKER_BASE_IMAGES.get(language.name)
    if template is None:
        return DEFAULT_DOCKER_IMAGE
    try:
        return template.format(version=language.version)
    except (KeyError, ValueError, IndexError):
        return DEFAULT_DOCKER_IMAGE


def _resolve_health_path(framework: FrameworkConfig) -> str:
    """Resolve health check endpoint path from framework."""
    return FRAMEWORK_HEALTH_PATHS.get(
        framework.name, DEFAULT_HEALTH_PATH,
    )


def _resolve_default_port(framework: FrameworkConfig) -> int:
    """Resolve default port from framework."""
    return FRAMEWORK_PORTS.get(
        framework.name, DEFAULT_PORT_FALLBACK,
    )


def _infer_native_build(config: ProjectConfig) -> bool:
    """Infer whether native build is supported."""
    if not config.framework.native_build:
        return False
    return config.framework.name in NATIVE_SUPPORTED_FRAMEWORKS


def _derive_project_type(config: ProjectConfig) -> str:
    """Derive project type from architecture style and interfaces."""
    style = config.architecture.style
    interface_types = _extract_interface_types(config)
    dispatch = {
        "microservice": lambda: _microservice_type(interface_types),
        "modular-monolith": lambda: "api",
        "monolith": lambda: "api",
        "library": lambda: _library_type(interface_types),
        "serverless": lambda: "api",
    }
    handler = dispatch.get(style, lambda: "api")
    return handler()


def _microservice_type(interface_types: List[str]) -> str:
    """Determine type for microservice architecture."""
    has_event = EVENT_CONSUMER_INTERFACE in interface_types
    has_rest = REST_INTERFACE in interface_types
    if has_event and not has_rest:
        return "worker"
    return "api"


def _library_type(interface_types: List[str]) -> str:
    """Determine type for library architecture."""
    if CLI_INTERFACE in interface_types:
        return "cli"
    return "library"


def _derive_protocols(config: ProjectConfig) -> List[str]:
    """Derive protocol list from interface types."""
    interface_types = _extract_interface_types(config)
    protocols = []
    for itype in interface_types:
        protocol = INTERFACE_PROTOCOL_MAP.get(itype)
        if protocol is not None:
            protocols.append(protocol)
    return protocols


def _extract_interface_types(config: ProjectConfig) -> List[str]:
    """Extract interface type strings from config."""
    return [iface.type for iface in config.interfaces]
