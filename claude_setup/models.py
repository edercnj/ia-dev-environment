from __future__ import annotations

from dataclasses import dataclass, field
from typing import List


@dataclass
class ProjectIdentity:
    name: str
    purpose: str

    @classmethod
    def from_dict(cls, data: dict) -> ProjectIdentity:
        return cls(
            name=data["name"],
            purpose=data["purpose"],
        )


@dataclass
class ArchitectureConfig:
    style: str
    domain_driven: bool = False
    event_driven: bool = False

    @classmethod
    def from_dict(cls, data: dict) -> ArchitectureConfig:
        return cls(
            style=data["style"],
            domain_driven=data.get("domain_driven", False),
            event_driven=data.get("event_driven", False),
        )


@dataclass
class InterfaceConfig:
    type: str
    spec: str = ""
    broker: str = ""

    @classmethod
    def from_dict(cls, data: dict) -> InterfaceConfig:
        return cls(
            type=data["type"],
            spec=data.get("spec", ""),
            broker=data.get("broker", ""),
        )


@dataclass
class LanguageConfig:
    name: str
    version: str

    @classmethod
    def from_dict(cls, data: dict) -> LanguageConfig:
        return cls(
            name=data["name"],
            version=data["version"],
        )


@dataclass
class FrameworkConfig:
    name: str
    version: str
    build_tool: str = "pip"
    native_build: bool = False

    @classmethod
    def from_dict(cls, data: dict) -> FrameworkConfig:
        return cls(
            name=data["name"],
            version=data["version"],
            build_tool=data.get("build_tool", "pip"),
            native_build=data.get("native_build", False),
        )


@dataclass
class TechComponent:
    name: str = "none"
    version: str = ""

    @classmethod
    def from_dict(cls, data: dict) -> TechComponent:
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
    def from_dict(cls, data: dict) -> DataConfig:
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
    def from_dict(cls, data: dict) -> SecurityConfig:
        return cls(
            frameworks=data.get("frameworks", []),
        )


@dataclass
class ObservabilityConfig:
    tool: str = "none"
    metrics: str = "none"
    tracing: str = "none"

    @classmethod
    def from_dict(cls, data: dict) -> ObservabilityConfig:
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
    observability: ObservabilityConfig = field(
        default_factory=_default_observability,
    )

    @classmethod
    def from_dict(cls, data: dict) -> InfraConfig:
        return cls(
            container=data.get("container", "docker"),
            orchestrator=data.get("orchestrator", "none"),
            observability=ObservabilityConfig.from_dict(
                data.get("observability", {}),
            ),
        )


@dataclass
class TestingConfig:
    smoke_tests: bool = True
    contract_tests: bool = False
    coverage_line: int = 95
    coverage_branch: int = 90

    @classmethod
    def from_dict(cls, data: dict) -> TestingConfig:
        return cls(
            smoke_tests=data.get("smoke_tests", True),
            contract_tests=data.get("contract_tests", False),
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
    def from_dict(cls, data: dict) -> ProjectConfig:
        interfaces_raw = data["interfaces"]
        interfaces = [
            InterfaceConfig.from_dict(i)
            for i in interfaces_raw
        ]
        return cls(
            project=ProjectIdentity.from_dict(
                data["project"],
            ),
            architecture=ArchitectureConfig.from_dict(
                data["architecture"],
            ),
            interfaces=interfaces,
            language=LanguageConfig.from_dict(
                data["language"],
            ),
            framework=FrameworkConfig.from_dict(
                data["framework"],
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
