from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Dict, List


def _require(data: Dict[str, Any], key: str, model: str) -> Any:
    try:
        return data[key]
    except KeyError:
        raise KeyError(
            f"Missing required field '{key}' for {model}"
        ) from None


@dataclass
class ProjectIdentity:
    name: str
    purpose: str

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> ProjectIdentity:
        return cls(
            name=_require(data, "name", "ProjectIdentity"),
            purpose=_require(data, "purpose", "ProjectIdentity"),
        )


@dataclass
class ArchitectureConfig:
    style: str
    domain_driven: bool = False
    event_driven: bool = False

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> ArchitectureConfig:
        return cls(
            style=_require(data, "style", "ArchitectureConfig"),
            domain_driven=data.get("domain_driven", False),
            event_driven=data.get("event_driven", False),
        )


@dataclass
class InterfaceConfig:
    type: str
    spec: str = ""
    broker: str = ""

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> InterfaceConfig:
        return cls(
            type=_require(data, "type", "InterfaceConfig"),
            spec=data.get("spec", ""),
            broker=data.get("broker", ""),
        )


@dataclass
class LanguageConfig:
    name: str
    version: str

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> LanguageConfig:
        return cls(
            name=_require(data, "name", "LanguageConfig"),
            version=_require(data, "version", "LanguageConfig"),
        )


@dataclass
class FrameworkConfig:
    name: str
    version: str
    build_tool: str = "pip"
    native_build: bool = False

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> FrameworkConfig:
        return cls(
            name=_require(data, "name", "FrameworkConfig"),
            version=_require(data, "version", "FrameworkConfig"),
            build_tool=data.get("build_tool", "pip"),
            native_build=data.get("native_build", False),
        )


@dataclass
class TechComponent:
    name: str = "none"
    version: str = ""

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> TechComponent:
        return cls(
            name=data.get("name", "none"),
            version=data.get("version", ""),
        )


def _default_tech_component() -> TechComponent:
    return TechComponent()


@dataclass
class DataConfig:
    database: TechComponent = field(
        default_factory=_default_tech_component,
    )
    migration: TechComponent = field(
        default_factory=_default_tech_component,
    )
    cache: TechComponent = field(
        default_factory=_default_tech_component,
    )

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> DataConfig:
        return cls(
            database=TechComponent.from_dict(
                data.get("database", {}),
            ),
            migration=TechComponent.from_dict(
                data.get("migration", {}),
            ),
            cache=TechComponent.from_dict(
                data.get("cache", {}),
            ),
        )


@dataclass
class SecurityConfig:
    frameworks: List[str] = field(default_factory=list)

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> SecurityConfig:
        return cls(
            frameworks=data.get("frameworks", []),
        )


@dataclass
class ObservabilityConfig:
    tool: str = "none"
    metrics: str = "none"
    tracing: str = "none"

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> ObservabilityConfig:
        return cls(
            tool=data.get("tool", "none"),
            metrics=data.get("metrics", "none"),
            tracing=data.get("tracing", "none"),
        )


def _default_observability() -> ObservabilityConfig:
    return ObservabilityConfig()


@dataclass
class InfraConfig:
    container: str = "docker"
    orchestrator: str = "none"
    templating: str = "kustomize"
    iac: str = "none"
    registry: str = "none"
    api_gateway: str = "none"
    service_mesh: str = "none"
    observability: ObservabilityConfig = field(
        default_factory=_default_observability,
    )

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> InfraConfig:
        return cls(
            container=data.get("container", "docker"),
            orchestrator=data.get("orchestrator", "none"),
            templating=data.get("templating", "kustomize"),
            iac=data.get("iac", "none"),
            registry=data.get("registry", "none"),
            api_gateway=data.get("api_gateway", "none"),
            service_mesh=data.get("service_mesh", "none"),
            observability=ObservabilityConfig.from_dict(
                data.get("observability", {}),
            ),
        )


@dataclass
class TestingConfig:
    smoke_tests: bool = True
    contract_tests: bool = False
    performance_tests: bool = True
    coverage_line: int = 95
    coverage_branch: int = 90

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> TestingConfig:
        return cls(
            smoke_tests=data.get("smoke_tests", True),
            contract_tests=data.get("contract_tests", False),
            performance_tests=data.get("performance_tests", True),
            coverage_line=data.get("coverage_line", 95),
            coverage_branch=data.get("coverage_branch", 90),
        )


@dataclass
class ProjectConfig:
    project: ProjectIdentity
    architecture: ArchitectureConfig
    interfaces: List[InterfaceConfig]
    language: LanguageConfig
    framework: FrameworkConfig
    data: DataConfig = field(
        default_factory=DataConfig,
    )
    infrastructure: InfraConfig = field(
        default_factory=InfraConfig,
    )
    security: SecurityConfig = field(
        default_factory=SecurityConfig,
    )
    testing: TestingConfig = field(
        default_factory=TestingConfig,
    )

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> ProjectConfig:
        interfaces_raw = _require(
            data, "interfaces", "ProjectConfig",
        )
        interfaces = [
            InterfaceConfig.from_dict(i)
            for i in interfaces_raw
        ]
        return cls(
            project=ProjectIdentity.from_dict(
                _require(data, "project", "ProjectConfig"),
            ),
            architecture=ArchitectureConfig.from_dict(
                _require(data, "architecture", "ProjectConfig"),
            ),
            interfaces=interfaces,
            language=LanguageConfig.from_dict(
                _require(data, "language", "ProjectConfig"),
            ),
            framework=FrameworkConfig.from_dict(
                _require(data, "framework", "ProjectConfig"),
            ),
            data=DataConfig.from_dict(
                data.get("data", {}),
            ),
            infrastructure=InfraConfig.from_dict(
                data.get("infrastructure", {}),
            ),
            security=SecurityConfig.from_dict(
                data.get("security", {}),
            ),
            testing=TestingConfig.from_dict(
                data.get("testing", {}),
            ),
        )


@dataclass
class PipelineResult:
    """Outcome of a pipeline execution."""

    success: bool
    output_dir: Path
    files_generated: List[Path]
    warnings: List[str]
    duration_ms: int
