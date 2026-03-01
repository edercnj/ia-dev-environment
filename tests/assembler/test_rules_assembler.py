from __future__ import annotations

import copy
from pathlib import Path
from typing import Callable

import pytest

from claude_setup.assembler.rules_assembler import RulesAssembler
from claude_setup.models import ProjectConfig
from claude_setup.template_engine import TemplateEngine


def _create_src_tree(base: Path) -> Path:
    """Create a minimal src directory tree for testing."""
    src = base / "src"
    # core-rules
    core_rules = src / "core-rules"
    core_rules.mkdir(parents=True)
    (core_rules / "03-coding-standards.md").write_text(
        "# Coding Standards for {framework_name}"
    )
    (core_rules / "04-architecture-summary.md").write_text(
        "# Architecture: {architecture_style}"
    )
    # core (detailed)
    core = src / "core"
    core.mkdir()
    (core / "01-clean-code.md").write_text("# Clean Code")
    (core / "03-testing-philosophy.md").write_text("# Testing")
    (core / "12-cloud-native-principles.md").write_text("# Cloud Native")
    # languages
    lang_common = src / "languages" / "python" / "common"
    lang_common.mkdir(parents=True)
    (lang_common / "coding-conventions.md").write_text("# Python Conventions")
    (lang_common / "testing-conventions.md").write_text("# Python Testing")
    lang_ver = src / "languages" / "python" / "python-3.9"
    lang_ver.mkdir()
    (lang_ver / "version-features.md").write_text("# Python 3.9 Features")
    # frameworks
    fw_common = src / "frameworks" / "click" / "common"
    fw_common.mkdir(parents=True)
    (fw_common / "cli-patterns.md").write_text("# Click CLI Patterns")
    # templates
    templates = src / "templates"
    templates.mkdir()
    (templates / "domain-template.md").write_text(
        "# Domain for {project_name}"
    )
    # databases
    db_sql = src / "databases" / "sql" / "postgresql"
    db_sql.mkdir(parents=True)
    (db_sql / "pg-patterns.md").write_text("# PostgreSQL patterns")
    db_common = src / "databases" / "sql" / "common"
    db_common.mkdir(parents=True)
    (db_common / "sql-basics.md").write_text("# SQL basics")
    (src / "databases" / "version-matrix.md").write_text("# DB Matrix")
    cache_redis = src / "databases" / "cache" / "redis"
    cache_redis.mkdir(parents=True)
    (cache_redis / "redis-patterns.md").write_text("# Redis")
    cache_common = src / "databases" / "cache" / "common"
    cache_common.mkdir(parents=True)
    (cache_common / "cache-basics.md").write_text("# Cache basics")
    # security
    sec = src / "security"
    sec.mkdir()
    (sec / "application-security.md").write_text("# App Security")
    (sec / "cryptography.md").write_text("# Crypto")
    compliance = sec / "compliance"
    compliance.mkdir()
    (compliance / "owasp.md").write_text("# OWASP")
    (compliance / "soc2.md").write_text("# SOC2")
    # cloud-providers
    cloud = src / "cloud-providers"
    cloud.mkdir()
    (cloud / "aws.md").write_text("# AWS")
    # infrastructure
    infra = src / "infrastructure"
    k8s = infra / "kubernetes"
    k8s.mkdir(parents=True)
    (k8s / "deployment-patterns.md").write_text("# K8s Deploy")
    (k8s / "kustomize-patterns.md").write_text("# Kustomize")
    containers = infra / "containers"
    containers.mkdir()
    (containers / "dockerfile-patterns.md").write_text("# Dockerfile")
    (containers / "registry-patterns.md").write_text("# Registry")
    iac = infra / "iac"
    iac.mkdir()
    (iac / "terraform-patterns.md").write_text("# Terraform")
    return src


def _full_config_dict():
    return {
        "project": {"name": "test-svc", "purpose": "Test service"},
        "architecture": {
            "style": "hexagonal",
            "domain_driven": True,
            "event_driven": False,
        },
        "interfaces": [
            {"type": "rest", "spec": "openapi-3.1"},
            {"type": "cli"},
        ],
        "language": {"name": "python", "version": "3.9"},
        "framework": {
            "name": "click",
            "version": "8.1",
            "build_tool": "pip",
        },
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
        },
        "security": {"frameworks": ["owasp", "soc2"]},
    }


def _minimal_config_dict():
    return {
        "project": {"name": "mini-cli", "purpose": "Mini CLI"},
        "architecture": {"style": "library"},
        "interfaces": [{"type": "cli"}],
        "language": {"name": "python", "version": "3.9"},
        "framework": {"name": "click", "version": "8.1"},
    }


@pytest.fixture
def src_tree(tmp_path: Path) -> Path:
    return _create_src_tree(tmp_path)


@pytest.fixture
def output_dir(tmp_path: Path) -> Path:
    out = tmp_path / "output"
    out.mkdir()
    return out


@pytest.fixture
def full_config() -> ProjectConfig:
    return ProjectConfig.from_dict(_full_config_dict())


@pytest.fixture
def minimal_config() -> ProjectConfig:
    return ProjectConfig.from_dict(_minimal_config_dict())


@pytest.fixture
def engine(src_tree: Path, full_config: ProjectConfig) -> TemplateEngine:
    return TemplateEngine(src_tree, full_config)


@pytest.fixture
def minimal_engine(
    src_tree: Path, minimal_config: ProjectConfig,
) -> TemplateEngine:
    return TemplateEngine(src_tree, minimal_config)


@pytest.fixture
def assembler(src_tree: Path) -> RulesAssembler:
    return RulesAssembler(src_tree)


class TestCopyCorRules:

    def test_copy_core_rules_copies_all_files(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        output_dir: Path, engine: TemplateEngine,
    ) -> None:
        rules_dir = output_dir / "rules"
        rules_dir.mkdir()
        result = assembler._copy_core_rules(full_config, rules_dir, engine)
        assert len(result) == 2
        assert all(p.is_file() for p in result)

    def test_copy_core_rules_replaces_placeholders(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        output_dir: Path, engine: TemplateEngine,
    ) -> None:
        rules_dir = output_dir / "rules"
        rules_dir.mkdir()
        assembler._copy_core_rules(full_config, rules_dir, engine)
        content = (rules_dir / "03-coding-standards.md").read_text()
        assert "click" in content
        assert "{framework_name}" not in content


class TestRouteCoreToKps:

    def test_route_core_to_knowledge_packs(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        output_dir: Path,
    ) -> None:
        skills_dir = output_dir / "skills"
        skills_dir.mkdir()
        result = assembler._route_core_to_kps(full_config, skills_dir)
        assert len(result) >= 2
        kp_names = [p.parent.parent.name for p in result]
        assert "coding-standards" in kp_names
        assert "testing" in kp_names

    def test_route_core_library_excludes_cloud_native(
        self, assembler: RulesAssembler, minimal_config: ProjectConfig,
        output_dir: Path,
    ) -> None:
        skills_dir = output_dir / "skills"
        skills_dir.mkdir()
        result = assembler._route_core_to_kps(minimal_config, skills_dir)
        filenames = [p.name for p in result]
        assert "cloud-native-principles.md" not in filenames

    def test_route_core_non_library_includes_cloud_native(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        output_dir: Path,
    ) -> None:
        skills_dir = output_dir / "skills"
        skills_dir.mkdir()
        result = assembler._route_core_to_kps(full_config, skills_dir)
        filenames = [p.name for p in result]
        assert "cloud-native-principles.md" in filenames


class TestCopyLanguageKps:

    def test_copy_language_kps_routes_testing_files(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        output_dir: Path,
    ) -> None:
        skills_dir = output_dir / "skills"
        skills_dir.mkdir()
        result = assembler._copy_language_kps(full_config, skills_dir)
        testing_files = [
            p for p in result
            if "testing" in str(p.parent)
        ]
        assert len(testing_files) >= 1
        assert any("testing-conventions" in p.name for p in testing_files)

    def test_copy_language_kps_routes_standards_files(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        output_dir: Path,
    ) -> None:
        skills_dir = output_dir / "skills"
        skills_dir.mkdir()
        result = assembler._copy_language_kps(full_config, skills_dir)
        coding_files = [
            p for p in result
            if "coding-standards" in str(p.parent)
        ]
        assert len(coding_files) >= 1


class TestCopyFrameworkKps:

    def test_copy_framework_kps_uses_stack_pack_name(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        output_dir: Path,
    ) -> None:
        skills_dir = output_dir / "skills"
        skills_dir.mkdir()
        result = assembler._copy_framework_kps(full_config, skills_dir)
        assert len(result) >= 1
        assert any("click-cli-patterns" in str(p) for p in result)


class TestGenerateProjectIdentity:

    def test_generate_project_identity_contains_config_values(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        output_dir: Path,
    ) -> None:
        rules_dir = output_dir / "rules"
        rules_dir.mkdir()
        path = assembler._generate_project_identity(full_config, rules_dir)
        content = path.read_text()
        assert "test-svc" in content
        assert "Test service" in content
        assert "hexagonal" in content
        assert "python 3.9" in content
        assert "click 8.1" in content


class TestCopyDomainTemplate:

    def test_copy_domain_template(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        output_dir: Path, engine: TemplateEngine,
    ) -> None:
        rules_dir = output_dir / "rules"
        rules_dir.mkdir()
        path = assembler._copy_domain_template(full_config, rules_dir, engine)
        content = path.read_text()
        assert "test-svc" in content
        assert "{project_name}" not in content


class TestConditionalLayers:

    def test_copy_database_references_for_postgresql(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        output_dir: Path, engine: TemplateEngine,
    ) -> None:
        skills_dir = output_dir / "skills"
        skills_dir.mkdir()
        result = assembler._copy_database_refs(full_config, skills_dir, engine)
        assert len(result) >= 1
        filenames = [p.name for p in result]
        assert "version-matrix.md" in filenames

    def test_copy_database_references_skipped_for_none(
        self, assembler: RulesAssembler, minimal_config: ProjectConfig,
        output_dir: Path, minimal_engine: TemplateEngine,
    ) -> None:
        skills_dir = output_dir / "skills"
        skills_dir.mkdir()
        result = assembler._copy_database_refs(
            minimal_config, skills_dir, minimal_engine,
        )
        assert result == []

    def test_copy_cache_references_for_redis(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        output_dir: Path, engine: TemplateEngine,
    ) -> None:
        skills_dir = output_dir / "skills"
        skills_dir.mkdir()
        result = assembler._copy_cache_refs(full_config, skills_dir, engine)
        assert len(result) >= 1

    def test_copy_cache_references_skipped_for_none(
        self, assembler: RulesAssembler, minimal_config: ProjectConfig,
        output_dir: Path, minimal_engine: TemplateEngine,
    ) -> None:
        skills_dir = output_dir / "skills"
        skills_dir.mkdir()
        result = assembler._copy_cache_refs(
            minimal_config, skills_dir, minimal_engine,
        )
        assert result == []

    def test_assemble_security_rules_when_configured(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        output_dir: Path,
    ) -> None:
        skills_dir = output_dir / "skills"
        skills_dir.mkdir()
        result = assembler._assemble_security(full_config, skills_dir)
        assert len(result) >= 2
        filenames = [p.name for p in result]
        assert "application-security.md" in filenames
        assert "owasp.md" in filenames

    def test_assemble_no_security_skips_security_files(
        self, assembler: RulesAssembler, minimal_config: ProjectConfig,
        output_dir: Path,
    ) -> None:
        skills_dir = output_dir / "skills"
        skills_dir.mkdir()
        result = assembler._assemble_security(minimal_config, skills_dir)
        assert result == []

    def test_assemble_cloud_knowledge_for_aws(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        output_dir: Path,
    ) -> None:
        skills_dir = output_dir / "skills"
        skills_dir.mkdir()
        result = assembler._assemble_cloud(full_config, skills_dir)
        assert len(result) == 1
        assert "cloud-aws.md" in result[0].name

    def test_assemble_cloud_knowledge_skipped_for_none(
        self, assembler: RulesAssembler, minimal_config: ProjectConfig,
        output_dir: Path,
    ) -> None:
        skills_dir = output_dir / "skills"
        skills_dir.mkdir()
        result = assembler._assemble_cloud(minimal_config, skills_dir)
        assert result == []

    def test_assemble_infrastructure_for_kubernetes(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        output_dir: Path,
    ) -> None:
        skills_dir = output_dir / "skills"
        skills_dir.mkdir()
        result = assembler._assemble_infra(full_config, skills_dir)
        assert len(result) >= 1
        filenames = [p.name for p in result]
        assert "k8s-deployment.md" in filenames

    def test_assemble_infrastructure_minimal_no_k8s_no_iac(
        self, assembler: RulesAssembler, minimal_config: ProjectConfig,
        output_dir: Path,
    ) -> None:
        skills_dir = output_dir / "skills"
        skills_dir.mkdir()
        result = assembler._assemble_infra(minimal_config, skills_dir)
        filenames = [p.name for p in result]
        assert "k8s-deployment.md" not in filenames
        # Container files still generated since default is docker
        assert "dockerfile.md" in filenames


class TestEdgeCases:

    def test_missing_core_rules_dir_returns_empty(
        self, tmp_path: Path,
    ) -> None:
        empty_src = tmp_path / "empty_src"
        empty_src.mkdir()
        (empty_src / "templates").mkdir()
        assembler = RulesAssembler(empty_src)
        config = ProjectConfig.from_dict(_minimal_config_dict())
        engine = TemplateEngine(empty_src, config)
        rules_dir = tmp_path / "rules"
        rules_dir.mkdir()
        result = assembler._copy_core_rules(config, rules_dir, engine)
        assert result == []

    def test_missing_core_dir_returns_empty(
        self, tmp_path: Path,
    ) -> None:
        empty_src = tmp_path / "empty_src"
        empty_src.mkdir()
        assembler = RulesAssembler(empty_src)
        config = ProjectConfig.from_dict(_minimal_config_dict())
        skills_dir = tmp_path / "skills"
        skills_dir.mkdir()
        result = assembler._route_core_to_kps(config, skills_dir)
        assert result == []

    def test_missing_lang_common_returns_empty(
        self, tmp_path: Path,
    ) -> None:
        src = tmp_path / "src"
        (src / "languages" / "ruby" / "common").mkdir(parents=True)
        assembler = RulesAssembler(src)
        d = _minimal_config_dict()
        d["language"]["name"] = "go"
        config = ProjectConfig.from_dict(d)
        skills_dir = tmp_path / "skills"
        skills_dir.mkdir()
        result = assembler._copy_language_kps(config, skills_dir)
        assert result == []

    def test_unknown_framework_returns_empty_kps(
        self, tmp_path: Path,
    ) -> None:
        src = tmp_path / "src"
        src.mkdir()
        assembler = RulesAssembler(src)
        d = _minimal_config_dict()
        d["framework"]["name"] = "unknown-fw"
        config = ProjectConfig.from_dict(d)
        skills_dir = tmp_path / "skills"
        skills_dir.mkdir()
        result = assembler._copy_framework_kps(config, skills_dir)
        assert result == []

    def test_fw_version_empty_skips_version_copy(
        self, assembler: RulesAssembler,
    ) -> None:
        result = assembler._copy_fw_version("click", "", Path("/tmp"))
        assert result == []

    def test_fw_version_dir_not_found_returns_empty(
        self, assembler: RulesAssembler, tmp_path: Path,
    ) -> None:
        fw_refs = tmp_path / "refs"
        fw_refs.mkdir()
        result = assembler._copy_fw_version("click", "99.0", fw_refs)
        assert result == []

    def test_domain_template_missing_uses_fallback(
        self, tmp_path: Path,
    ) -> None:
        src = tmp_path / "src"
        src.mkdir()
        assembler = RulesAssembler(src)
        config = ProjectConfig.from_dict(_minimal_config_dict())
        engine = TemplateEngine(src, config)
        rules_dir = tmp_path / "rules"
        rules_dir.mkdir()
        path = assembler._copy_domain_template(config, rules_dir, engine)
        content = path.read_text()
        assert "Domain" in content

    def test_nosql_database_mongodb(
        self, tmp_path: Path,
    ) -> None:
        src = _create_src_tree(tmp_path)
        nosql_common = src / "databases" / "nosql" / "common"
        nosql_common.mkdir(parents=True)
        (nosql_common / "nosql-basics.md").write_text("# NoSQL")
        mongo = src / "databases" / "nosql" / "mongodb"
        mongo.mkdir(parents=True)
        (mongo / "mongo-patterns.md").write_text("# MongoDB")
        assembler = RulesAssembler(src)
        d = _full_config_dict()
        d["data"]["database"]["name"] = "mongodb"
        config = ProjectConfig.from_dict(d)
        engine = TemplateEngine(src, config)
        skills_dir = tmp_path / "skills"
        skills_dir.mkdir()
        result = assembler._copy_database_refs(config, skills_dir, engine)
        filenames = [p.name for p in result]
        assert "nosql-basics.md" in filenames
        assert "mongo-patterns.md" in filenames

    def test_db_version_matrix_missing(
        self, tmp_path: Path,
    ) -> None:
        src = tmp_path / "src"
        (src / "databases" / "sql" / "postgresql").mkdir(parents=True)
        (src / "databases" / "sql" / "postgresql" / "pg.md").write_text("pg")
        assembler = RulesAssembler(src)
        d = _full_config_dict()
        config = ProjectConfig.from_dict(d)
        engine = TemplateEngine(src, config)
        target = tmp_path / "target"
        target.mkdir()
        result = assembler._copy_db_version_matrix(target)
        assert result == []

    def test_cloud_provider_file_missing(
        self, tmp_path: Path,
    ) -> None:
        src = tmp_path / "src"
        (src / "cloud-providers").mkdir(parents=True)
        assembler = RulesAssembler(src)
        d = _full_config_dict()
        d["infrastructure"]["registry"] = "gcr"
        config = ProjectConfig.from_dict(d)
        skills_dir = tmp_path / "skills"
        skills_dir.mkdir()
        result = assembler._assemble_cloud(config, skills_dir)
        assert result == []

    def test_iac_file_missing(
        self, tmp_path: Path,
    ) -> None:
        src = _create_src_tree(tmp_path)
        assembler = RulesAssembler(src)
        d = _full_config_dict()
        d["infrastructure"]["iac"] = "pulumi"
        config = ProjectConfig.from_dict(d)
        skills_dir = tmp_path / "skills"
        skills_dir.mkdir()
        result = assembler._assemble_infra(config, skills_dir)
        filenames = [p.name for p in result]
        assert "iac-pulumi.md" not in filenames

    def test_container_none_skips_container_files(
        self, tmp_path: Path,
    ) -> None:
        src = _create_src_tree(tmp_path)
        assembler = RulesAssembler(src)
        d = _full_config_dict()
        d["infrastructure"]["container"] = "none"
        d["infrastructure"]["orchestrator"] = "none"
        d["infrastructure"]["iac"] = "none"
        config = ProjectConfig.from_dict(d)
        skills_dir = tmp_path / "skills"
        skills_dir.mkdir()
        result = assembler._assemble_infra(config, skills_dir)
        assert result == []

    def test_security_dir_missing_returns_empty(
        self, tmp_path: Path,
    ) -> None:
        src = tmp_path / "empty_src"
        src.mkdir()
        assembler = RulesAssembler(src)
        d = _minimal_config_dict()
        d["security"] = {"frameworks": ["owasp"]}
        config = ProjectConfig.from_dict(d)
        skills_dir = tmp_path / "skills"
        skills_dir.mkdir()
        result = assembler._assemble_security(config, skills_dir)
        assert result == []

    def test_k8s_missing_deploy_file(
        self, tmp_path: Path,
    ) -> None:
        src = tmp_path / "src"
        k8s = src / "infrastructure" / "kubernetes"
        k8s.mkdir(parents=True)
        # no deployment-patterns.md, no kustomize-patterns.md
        assembler = RulesAssembler(src)
        d = _full_config_dict()
        config = ProjectConfig.from_dict(d)
        skills_dir = tmp_path / "skills"
        kp_dir = skills_dir / "knowledge-packs"
        kp_dir.mkdir(parents=True)
        from claude_setup.assembler.rules_assembler import _copy_k8s_files
        result = _copy_k8s_files(config, src / "infrastructure", kp_dir)
        assert result == []

    def test_lang_version_not_found_returns_empty(
        self, assembler: RulesAssembler, tmp_path: Path,
    ) -> None:
        coding_refs = tmp_path / "coding"
        coding_refs.mkdir()
        result = assembler._copy_lang_version("python", "9.9", coding_refs)
        assert result == []


class TestAssembleFullPipeline:

    def test_assemble_full_config_generates_all_core_rules(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        output_dir: Path, engine: TemplateEngine,
    ) -> None:
        result = assembler.assemble(full_config, output_dir, engine)
        assert len(result) >= 10

    def test_assemble_returns_existing_paths(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        output_dir: Path, engine: TemplateEngine,
    ) -> None:
        result = assembler.assemble(full_config, output_dir, engine)
        for path in result:
            assert path.is_file(), f"Missing: {path}"

    def test_assemble_minimal_config(
        self, assembler: RulesAssembler, minimal_config: ProjectConfig,
        output_dir: Path, minimal_engine: TemplateEngine,
    ) -> None:
        result = assembler.assemble(minimal_config, output_dir, minimal_engine)
        assert len(result) >= 5
        for path in result:
            assert path.is_file(), f"Missing: {path}"
