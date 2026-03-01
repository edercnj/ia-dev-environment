from __future__ import annotations

import copy

import pytest

from claude_setup.models import ProjectConfig


FULL_PROJECT_DICT = {
    "project": {
        "name": "my-service",
        "purpose": "A sample service",
    },
    "architecture": {
        "style": "hexagonal",
        "domain_driven": True,
        "event_driven": False,
    },
    "interfaces": [
        {"type": "rest", "spec": "openapi-3.1"},
        {"type": "grpc", "spec": "proto3"},
        {"type": "event-consumer", "broker": "kafka"},
        {"type": "cli"},
    ],
    "language": {
        "name": "python",
        "version": "3.9",
    },
    "framework": {
        "name": "click",
        "version": "8.1",
        "build_tool": "pip",
        "native_build": False,
    },
    "data": {
        "database": {"name": "postgresql", "version": "15"},
        "migration": {"name": "flyway", "version": "9"},
        "cache": {"name": "redis", "version": "7"},
    },
    "infrastructure": {
        "container": "docker",
        "orchestrator": "kubernetes",
        "observability": {
            "tool": "opentelemetry",
            "metrics": "prometheus",
            "tracing": "jaeger",
        },
    },
    "security": {
        "frameworks": ["owasp", "soc2"],
    },
    "testing": {
        "smoke_tests": True,
        "contract_tests": True,
        "coverage_line": 95,
        "coverage_branch": 90,
    },
}

MINIMAL_PROJECT_DICT = {
    "project": {
        "name": "minimal-tool",
        "purpose": "Minimal CLI tool",
    },
    "architecture": {
        "style": "library",
    },
    "interfaces": [
        {"type": "cli"},
    ],
    "language": {
        "name": "python",
        "version": "3.9",
    },
    "framework": {
        "name": "click",
        "version": "8.1",
    },
}


@pytest.fixture
def full_project_dict() -> dict:
    return copy.deepcopy(FULL_PROJECT_DICT)


@pytest.fixture
def minimal_project_dict() -> dict:
    return copy.deepcopy(MINIMAL_PROJECT_DICT)


@pytest.fixture
def create_project_config():
    """Factory fixture to build ProjectConfig with overrides."""
    def _create(**overrides):
        base = copy.deepcopy(MINIMAL_PROJECT_DICT)
        for key, value in overrides.items():
            base[key] = value
        return ProjectConfig.from_dict(base)

    return _create
