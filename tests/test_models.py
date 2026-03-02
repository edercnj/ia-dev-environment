from __future__ import annotations

from pathlib import Path

import pytest

from claude_setup.models import (
    ArchitectureConfig,
    DataConfig,
    FileDiff,
    FrameworkConfig,
    InfraConfig,
    InterfaceConfig,
    LanguageConfig,
    ObservabilityConfig,
    ProjectConfig,
    ProjectIdentity,
    SecurityConfig,
    TechComponent,
    TestingConfig,
    VerificationResult,
)


# --- ProjectIdentity ---


class TestProjectIdentity:

    def test_init_direct_stores_attributes(self) -> None:
        identity = ProjectIdentity(name="tool", purpose="A tool")
        assert identity.name == "tool"
        assert identity.purpose == "A tool"

    def test_from_dict_complete_returns_instance(self) -> None:
        data = {"name": "my-app", "purpose": "My app"}
        result = ProjectIdentity.from_dict(data)
        assert result.name == "my-app"
        assert result.purpose == "My app"

    def test_from_dict_missing_name_raises_key_error(self) -> None:
        with pytest.raises(KeyError, match="Missing required field 'name'"):
            ProjectIdentity.from_dict({"purpose": "x"})


# --- ArchitectureConfig ---


class TestArchitectureConfig:

    def test_init_direct_stores_attributes(self) -> None:
        cfg = ArchitectureConfig(
            style="hexagonal",
            domain_driven=True,
            event_driven=True,
        )
        assert cfg.style == "hexagonal"
        assert cfg.domain_driven is True
        assert cfg.event_driven is True

    def test_from_dict_complete_returns_instance(self) -> None:
        data = {
            "style": "hexagonal",
            "domain_driven": True,
            "event_driven": False,
        }
        result = ArchitectureConfig.from_dict(data)
        assert result.style == "hexagonal"
        assert result.domain_driven is True
        assert result.event_driven is False

    def test_from_dict_defaults_returns_false(self) -> None:
        result = ArchitectureConfig.from_dict({"style": "library"})
        assert result.domain_driven is False
        assert result.event_driven is False

    @pytest.mark.parametrize(
        "input_dict, exp_style, exp_dd, exp_ed",
        [
            (
                {"style": "hex", "domain_driven": True, "event_driven": True},
                "hex", True, True,
            ),
            ({"style": "lib"}, "lib", False, False),
            (
                {"style": "ms", "domain_driven": True},
                "ms", True, False,
            ),
        ],
    )
    def test_from_dict_parametrized(
        self,
        input_dict: dict,
        exp_style: str,
        exp_dd: bool,
        exp_ed: bool,
    ) -> None:
        result = ArchitectureConfig.from_dict(input_dict)
        assert result.style == exp_style
        assert result.domain_driven is exp_dd
        assert result.event_driven is exp_ed


# --- InterfaceConfig ---


class TestInterfaceConfig:

    def test_init_direct_stores_attributes(self) -> None:
        cfg = InterfaceConfig(type="rest", spec="oas3", broker="")
        assert cfg.type == "rest"
        assert cfg.spec == "oas3"

    def test_from_dict_rest_returns_spec(self) -> None:
        data = {"type": "rest", "spec": "openapi-3.1"}
        result = InterfaceConfig.from_dict(data)
        assert result.type == "rest"
        assert result.spec == "openapi-3.1"
        assert result.broker == ""

    def test_from_dict_event_returns_broker(self) -> None:
        data = {"type": "event-consumer", "broker": "kafka"}
        result = InterfaceConfig.from_dict(data)
        assert result.broker == "kafka"
        assert result.spec == ""

    def test_from_dict_minimal_returns_defaults(self) -> None:
        result = InterfaceConfig.from_dict({"type": "cli"})
        assert result.spec == ""
        assert result.broker == ""


# --- LanguageConfig ---


class TestLanguageConfig:

    def test_init_direct_stores_attributes(self) -> None:
        cfg = LanguageConfig(name="python", version="3.9")
        assert cfg.name == "python"
        assert cfg.version == "3.9"

    def test_from_dict_complete_returns_instance(self) -> None:
        data = {"name": "python", "version": "3.9"}
        result = LanguageConfig.from_dict(data)
        assert result.name == "python"
        assert result.version == "3.9"

    def test_from_dict_missing_version_raises(self) -> None:
        with pytest.raises(KeyError, match="Missing required field 'version'"):
            LanguageConfig.from_dict({"name": "python"})


# --- FrameworkConfig ---


class TestFrameworkConfig:

    def test_from_dict_complete_returns_instance(self) -> None:
        data = {
            "name": "click",
            "version": "8.1",
            "build_tool": "pip",
            "native_build": False,
        }
        result = FrameworkConfig.from_dict(data)
        assert result.name == "click"
        assert result.version == "8.1"
        assert result.build_tool == "pip"
        assert result.native_build is False

    def test_from_dict_defaults_returns_pip_false(self) -> None:
        data = {"name": "click", "version": "8.1"}
        result = FrameworkConfig.from_dict(data)
        assert result.build_tool == "pip"
        assert result.native_build is False

    @pytest.mark.parametrize(
        "data, exp_build, exp_native",
        [
            (
                {"name": "q", "version": "3", "build_tool": "maven", "native_build": True},
                "maven", True,
            ),
            ({"name": "s", "version": "3"}, "pip", False),
        ],
    )
    def test_from_dict_parametrized(
        self,
        data: dict,
        exp_build: str,
        exp_native: bool,
    ) -> None:
        result = FrameworkConfig.from_dict(data)
        assert result.build_tool == exp_build
        assert result.native_build is exp_native


# --- TechComponent ---


class TestTechComponent:

    def test_init_defaults(self) -> None:
        tc = TechComponent()
        assert tc.name == "none"
        assert tc.version == ""

    def test_from_dict_complete_returns_instance(self) -> None:
        data = {"name": "postgresql", "version": "15"}
        result = TechComponent.from_dict(data)
        assert result.name == "postgresql"
        assert result.version == "15"

    def test_from_dict_empty_returns_defaults(self) -> None:
        result = TechComponent.from_dict({})
        assert result.name == "none"
        assert result.version == ""


# --- DataConfig ---


class TestDataConfig:

    def test_init_defaults_independent(self) -> None:
        a = DataConfig()
        b = DataConfig()
        assert a is not b
        assert a.database is not b.database

    def test_from_dict_complete_returns_components(self) -> None:
        data = {
            "database": {"name": "postgresql", "version": "15"},
            "migration": {"name": "flyway", "version": "9"},
            "cache": {"name": "redis", "version": "7"},
        }
        result = DataConfig.from_dict(data)
        assert result.database.name == "postgresql"
        assert result.migration.name == "flyway"
        assert result.cache.name == "redis"

    def test_from_dict_empty_returns_defaults(self) -> None:
        result = DataConfig.from_dict({})
        assert result.database.name == "none"
        assert result.cache.name == "none"

    def test_from_dict_partial_returns_mixed(self) -> None:
        data = {"database": {"name": "mysql", "version": "8"}}
        result = DataConfig.from_dict(data)
        assert result.database.name == "mysql"
        assert result.cache.name == "none"


# --- SecurityConfig ---


class TestSecurityConfig:

    def test_init_default_empty_list(self) -> None:
        cfg = SecurityConfig()
        assert cfg.frameworks == []

    def test_init_defaults_independent(self) -> None:
        a = SecurityConfig()
        b = SecurityConfig()
        assert a.frameworks is not b.frameworks

    def test_from_dict_complete_returns_list(self) -> None:
        data = {"frameworks": ["owasp", "soc2"]}
        result = SecurityConfig.from_dict(data)
        assert result.frameworks == ["owasp", "soc2"]

    def test_from_dict_empty_returns_defaults(self) -> None:
        result = SecurityConfig.from_dict({})
        assert result.frameworks == []


# --- ObservabilityConfig ---


class TestObservabilityConfig:

    def test_init_defaults(self) -> None:
        cfg = ObservabilityConfig()
        assert cfg.tool == "none"
        assert cfg.metrics == "none"
        assert cfg.tracing == "none"

    def test_from_dict_complete_returns_instance(self) -> None:
        data = {
            "tool": "otel",
            "metrics": "prom",
            "tracing": "jaeger",
        }
        result = ObservabilityConfig.from_dict(data)
        assert result.tool == "otel"
        assert result.metrics == "prom"
        assert result.tracing == "jaeger"

    def test_from_dict_empty_returns_defaults(self) -> None:
        result = ObservabilityConfig.from_dict({})
        assert result.tool == "none"


# --- InfraConfig ---


class TestInfraConfig:

    def test_init_defaults(self) -> None:
        cfg = InfraConfig()
        assert cfg.container == "docker"
        assert cfg.orchestrator == "none"
        assert cfg.templating == "kustomize"
        assert cfg.iac == "none"
        assert cfg.registry == "none"
        assert cfg.api_gateway == "none"
        assert cfg.service_mesh == "none"
        assert isinstance(cfg.observability, ObservabilityConfig)

    def test_from_dict_complete_returns_instance(self) -> None:
        data = {
            "container": "docker",
            "orchestrator": "k8s",
            "templating": "helm",
            "iac": "terraform",
            "registry": "ecr",
            "api_gateway": "kong",
            "service_mesh": "istio",
            "observability": {
                "tool": "otel",
                "metrics": "prom",
                "tracing": "jaeger",
            },
        }
        result = InfraConfig.from_dict(data)
        assert result.container == "docker"
        assert result.orchestrator == "k8s"
        assert result.templating == "helm"
        assert result.iac == "terraform"
        assert result.registry == "ecr"
        assert result.api_gateway == "kong"
        assert result.service_mesh == "istio"
        assert result.observability.tool == "otel"

    def test_from_dict_empty_returns_defaults(self) -> None:
        result = InfraConfig.from_dict({})
        assert result.container == "docker"
        assert result.templating == "kustomize"
        assert result.iac == "none"
        assert result.registry == "none"
        assert result.api_gateway == "none"
        assert result.service_mesh == "none"
        assert result.observability.tool == "none"

    def test_init_defaults_independent(self) -> None:
        a = InfraConfig()
        b = InfraConfig()
        assert a.observability is not b.observability

    def test_from_dict_backward_compatible(self) -> None:
        data = {"container": "docker", "orchestrator": "k8s"}
        result = InfraConfig.from_dict(data)
        assert result.templating == "kustomize"
        assert result.iac == "none"


# --- TestingConfig ---


class TestTestingConfig:

    def test_init_defaults(self) -> None:
        cfg = TestingConfig()
        assert cfg.smoke_tests is True
        assert cfg.contract_tests is False
        assert cfg.performance_tests is True
        assert cfg.coverage_line == 95
        assert cfg.coverage_branch == 90

    def test_from_dict_complete_returns_instance(self) -> None:
        data = {
            "smoke_tests": False,
            "contract_tests": True,
            "performance_tests": False,
            "coverage_line": 80,
            "coverage_branch": 70,
        }
        result = TestingConfig.from_dict(data)
        assert result.smoke_tests is False
        assert result.contract_tests is True
        assert result.performance_tests is False
        assert result.coverage_line == 80
        assert result.coverage_branch == 70

    def test_from_dict_empty_returns_defaults(self) -> None:
        result = TestingConfig.from_dict({})
        assert result.smoke_tests is True
        assert result.performance_tests is True
        assert result.coverage_line == 95

    def test_from_dict_backward_compatible(self) -> None:
        data = {"smoke_tests": True, "contract_tests": False}
        result = TestingConfig.from_dict(data)
        assert result.performance_tests is True


# --- ProjectConfig ---


class TestProjectConfig:

    def test_from_dict_full_returns_populated(
        self,
        full_project_dict: dict,
    ) -> None:
        cfg = ProjectConfig.from_dict(full_project_dict)
        assert cfg.project.name == "my-service"
        assert cfg.architecture.style == "hexagonal"
        assert cfg.architecture.domain_driven is True
        assert cfg.language.name == "python"
        assert cfg.framework.name == "click"
        assert cfg.data.database.name == "postgresql"
        assert cfg.security.frameworks == ["owasp", "soc2"]
        assert cfg.testing.contract_tests is True

    def test_from_dict_full_interfaces_count(
        self,
        full_project_dict: dict,
    ) -> None:
        cfg = ProjectConfig.from_dict(full_project_dict)
        assert len(cfg.interfaces) == 4

    def test_from_dict_full_interface_types(
        self,
        full_project_dict: dict,
    ) -> None:
        cfg = ProjectConfig.from_dict(full_project_dict)
        types = [i.type for i in cfg.interfaces]
        assert types == [
            "rest", "grpc", "event-consumer", "cli",
        ]

    def test_from_dict_minimal_returns_defaults(
        self,
        minimal_project_dict: dict,
    ) -> None:
        cfg = ProjectConfig.from_dict(minimal_project_dict)
        assert cfg.project.name == "minimal-tool"
        assert cfg.data.database.name == "none"
        assert cfg.security.frameworks == []
        assert cfg.testing.smoke_tests is True
        assert cfg.infrastructure.container == "docker"

    def test_from_dict_minimal_interfaces_count(
        self,
        minimal_project_dict: dict,
    ) -> None:
        cfg = ProjectConfig.from_dict(minimal_project_dict)
        assert len(cfg.interfaces) == 1

    def test_from_dict_missing_project_raises(self) -> None:
        with pytest.raises(KeyError, match="Missing required field 'project'"):
            ProjectConfig.from_dict({
                "architecture": {"style": "lib"},
                "interfaces": [],
                "language": {"name": "py", "version": "3"},
                "framework": {"name": "c", "version": "1"},
            })

    def test_from_dict_missing_architecture_raises(self) -> None:
        with pytest.raises(KeyError, match="Missing required field 'architecture'"):
            ProjectConfig.from_dict({
                "project": {"name": "x", "purpose": "y"},
                "interfaces": [],
                "language": {"name": "py", "version": "3"},
                "framework": {"name": "c", "version": "1"},
            })

    def test_from_dict_missing_interfaces_raises(self) -> None:
        with pytest.raises(KeyError, match="Missing required field 'interfaces'"):
            ProjectConfig.from_dict({
                "project": {"name": "x", "purpose": "y"},
                "architecture": {"style": "lib"},
                "language": {"name": "py", "version": "3"},
                "framework": {"name": "c", "version": "1"},
            })

    def test_from_dict_missing_language_raises(self) -> None:
        with pytest.raises(KeyError, match="Missing required field 'language'"):
            ProjectConfig.from_dict({
                "project": {"name": "x", "purpose": "y"},
                "architecture": {"style": "lib"},
                "interfaces": [],
                "framework": {"name": "c", "version": "1"},
            })

    def test_from_dict_missing_framework_raises(self) -> None:
        with pytest.raises(KeyError, match="Missing required field 'framework'"):
            ProjectConfig.from_dict({
                "project": {"name": "x", "purpose": "y"},
                "architecture": {"style": "lib"},
                "interfaces": [],
                "language": {"name": "py", "version": "3"},
            })

    def test_from_dict_empty_interfaces_returns_empty(
        self,
    ) -> None:
        data = {
            "project": {"name": "x", "purpose": "y"},
            "architecture": {"style": "lib"},
            "interfaces": [],
            "language": {"name": "py", "version": "3"},
            "framework": {"name": "c", "version": "1"},
        }
        cfg = ProjectConfig.from_dict(data)
        assert cfg.interfaces == []

    def test_from_dict_infra_nested_observability(
        self,
        full_project_dict: dict,
    ) -> None:
        cfg = ProjectConfig.from_dict(full_project_dict)
        obs = cfg.infrastructure.observability
        assert obs.tool == "opentelemetry"
        assert obs.metrics == "prometheus"
        assert obs.tracing == "jaeger"


# --- FileDiff ---


class TestFileDiff:

    def test_create_with_all_fields(self) -> None:
        diff = FileDiff(
            path=Path("a.md"),
            diff="--- a\n+++ b\n",
            python_size=100,
            reference_size=120,
        )
        assert diff.path == Path("a.md")
        assert diff.diff == "--- a\n+++ b\n"
        assert diff.python_size == 100
        assert diff.reference_size == 120

    def test_path_is_path_object(self) -> None:
        diff = FileDiff(
            path=Path("sub/file.txt"),
            diff="",
            python_size=0,
            reference_size=0,
        )
        assert isinstance(diff.path, Path)

    def test_diff_stores_string(self) -> None:
        content = "--- ref\n+++ py\n@@ -1 +1 @@\n-old\n+new\n"
        diff = FileDiff(
            path=Path("x.txt"),
            diff=content,
            python_size=3,
            reference_size=3,
        )
        assert diff.diff == content

    def test_sizes_are_integers(self) -> None:
        diff = FileDiff(
            path=Path("f.txt"),
            diff="",
            python_size=42,
            reference_size=99,
        )
        assert isinstance(diff.python_size, int)
        assert isinstance(diff.reference_size, int)


# --- VerificationResult ---


class TestVerificationResult:

    def test_create_success_result(self) -> None:
        result = VerificationResult(
            success=True,
            total_files=5,
            mismatches=[],
            missing_files=[],
            extra_files=[],
        )
        assert result.success is True
        assert result.total_files == 5
        assert result.mismatches == []

    def test_create_failure_result(self) -> None:
        mismatch = FileDiff(
            path=Path("a.txt"),
            diff="diff",
            python_size=10,
            reference_size=12,
        )
        result = VerificationResult(
            success=False,
            total_files=3,
            mismatches=[mismatch],
            missing_files=[],
            extra_files=[],
        )
        assert result.success is False
        assert len(result.mismatches) == 1

    def test_total_files_reflects_count(self) -> None:
        result = VerificationResult(
            success=True,
            total_files=10,
            mismatches=[],
            missing_files=[],
            extra_files=[],
        )
        assert result.total_files == 10

    def test_missing_files_stores_paths(self) -> None:
        paths = [Path("a.txt"), Path("b/c.txt")]
        result = VerificationResult(
            success=False,
            total_files=2,
            mismatches=[],
            missing_files=paths,
            extra_files=[],
        )
        assert result.missing_files == paths

    def test_extra_files_stores_paths(self) -> None:
        paths = [Path("extra.md")]
        result = VerificationResult(
            success=False,
            total_files=1,
            mismatches=[],
            missing_files=[],
            extra_files=paths,
        )
        assert result.extra_files == paths
