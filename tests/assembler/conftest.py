from __future__ import annotations

from typing import Callable

import pytest

from claude_setup.models import ProjectConfig


def _full_featured_dict():
    """Config dict with all features enabled."""
    return {
        "project": {"name": "full-service", "purpose": "Full featured service"},
        "architecture": {"style": "hexagonal", "domain_driven": True, "event_driven": True},
        "interfaces": [
            {"type": "rest", "spec": "openapi-3.1"},
            {"type": "grpc", "spec": "proto3"},
            {"type": "event-consumer", "broker": "kafka"},
        ],
        "language": {"name": "java", "version": "21"},
        "framework": {"name": "quarkus", "version": "3.17", "build_tool": "maven"},
        "data": {
            "database": {"name": "postgresql", "version": "15"},
            "cache": {"name": "redis", "version": "7"},
        },
        "infrastructure": {
            "container": "docker",
            "orchestrator": "kubernetes",
            "templating": "kustomize",
            "iac": "terraform",
            "registry": "ecr",
            "api_gateway": "kong",
            "service_mesh": "istio",
            "observability": {
                "tool": "opentelemetry",
                "metrics": "prometheus",
                "tracing": "jaeger",
            },
        },
        "security": {
            "frameworks": ["owasp", "pci-dss", "lgpd", "hipaa", "sox"],
        },
        "testing": {
            "smoke_tests": True,
            "contract_tests": True,
            "performance_tests": True,
            "coverage_line": 95,
            "coverage_branch": 90,
        },
    }


def _minimal_cli_dict():
    """Minimal CLI config with no optional features."""
    return {
        "project": {"name": "minimal-cli", "purpose": "Minimal CLI"},
        "architecture": {"style": "library"},
        "interfaces": [{"type": "cli"}],
        "language": {"name": "python", "version": "3.9"},
        "framework": {"name": "click", "version": "8.1"},
    }


@pytest.fixture
def full_featured_config():
    return ProjectConfig.from_dict(_full_featured_dict())


@pytest.fixture
def minimal_cli_config():
    return ProjectConfig.from_dict(_minimal_cli_dict())


@pytest.fixture
def config_factory() -> Callable:
    """Factory to build ProjectConfig with selective overrides."""
    def _create(**overrides):
        base = _minimal_cli_dict()
        for key, value in overrides.items():
            base[key] = value
        return ProjectConfig.from_dict(base)
    return _create
