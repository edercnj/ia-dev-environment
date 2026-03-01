from __future__ import annotations

import copy
from pathlib import Path
from typing import Any, Dict

import pytest

from claude_setup.assembler.auditor import audit_rules_context
from claude_setup.assembler.rules_assembler import RulesAssembler
from claude_setup.models import ProjectConfig
from claude_setup.template_engine import TemplateEngine


def _create_src_tree(base: Path) -> Path:
    """Build a minimal src/ tree for assembler tests."""
    src = base / "src"
    # core-rules
    cr = src / "core-rules"
    cr.mkdir(parents=True)
    (cr / "03-coding-standards.md").write_text(
        "# Coding Standards\nLanguage: {language_name}\n", encoding="utf-8",
    )
    (cr / "04-architecture-summary.md").write_text(
        "# Architecture\nStyle: {architecture_style}\n", encoding="utf-8",
    )
    # core detailed
    core = src / "core"
    core.mkdir()
    (core / "01-clean-code.md").write_text("# Clean Code\n", encoding="utf-8")
    (core / "03-testing-philosophy.md").write_text("# Testing\n", encoding="utf-8")
    (core / "12-cloud-native-principles.md").write_text("# Cloud\n", encoding="utf-8")
    # language
    lang_common = src / "languages" / "python" / "common"
    lang_common.mkdir(parents=True)
    (lang_common / "coding-conventions.md").write_text("# Python conventions\n", encoding="utf-8")
    (lang_common / "testing-conventions.md").write_text("# Python testing\n", encoding="utf-8")
    lang_ver = src / "languages" / "python" / "python-3.9"
    lang_ver.mkdir()
    (lang_ver / "version-features.md").write_text("# Python 3.9\n", encoding="utf-8")
    # framework
    fw_common = src / "frameworks" / "click" / "common"
    fw_common.mkdir(parents=True)
    (fw_common / "click-cli-patterns.md").write_text("# Click patterns\n", encoding="utf-8")
    # templates
    tmpl = src / "templates"
    tmpl.mkdir()
    (tmpl / "domain-template.md").write_text(
        "# Domain\nProject: {project_name}\n", encoding="utf-8",
    )
    # databases
    db = src / "databases"
    db.mkdir()
    (db / "version-matrix.md").write_text("# DB Matrix\n", encoding="utf-8")
    sql_common = db / "sql" / "common"
    sql_common.mkdir(parents=True)
    (sql_common / "sql-patterns.md").write_text("# SQL {database_name}\n", encoding="utf-8")
    sql_pg = db / "sql" / "postgresql"
    sql_pg.mkdir(parents=True)
    (sql_pg / "pg-patterns.md").write_text("# PostgreSQL\n", encoding="utf-8")
    cache_common = db / "cache" / "common"
    cache_common.mkdir(parents=True)
    (cache_common / "cache-patterns.md").write_text("# Cache\n", encoding="utf-8")
    cache_redis = db / "cache" / "redis"
    cache_redis.mkdir(parents=True)
    (cache_redis / "redis-patterns.md").write_text("# Redis\n", encoding="utf-8")
    # security
    sec = src / "security"
    sec.mkdir()
    (sec / "application-security.md").write_text("# App Security\n", encoding="utf-8")
    (sec / "cryptography.md").write_text("# Crypto\n", encoding="utf-8")
    comp = sec / "compliance"
    comp.mkdir()
    (comp / "owasp.md").write_text("# OWASP\n", encoding="utf-8")
    # infrastructure
    infra = src / "infrastructure"
    k8s = infra / "kubernetes"
    k8s.mkdir(parents=True)
    (k8s / "deployment-patterns.md").write_text("# K8s\n", encoding="utf-8")
    containers = infra / "containers"
    containers.mkdir(parents=True)
    (containers / "dockerfile-patterns.md").write_text("# Dockerfile\n", encoding="utf-8")
    return src


def _full_config_dict() -> Dict[str, Any]:
    return {
        "project": {"name": "test-svc", "purpose": "Test service"},
        "architecture": {"style": "hexagonal", "domain_driven": True},
        "interfaces": [{"type": "rest"}, {"type": "cli"}],
        "language": {"name": "python", "version": "3.9"},
        "framework": {"name": "click", "version": "8.1"},
        "data": {
            "database": {"name": "postgresql", "version": "15"},
            "migration": {"name": "flyway"},
            "cache": {"name": "redis", "version": "7"},
        },
        "infrastructure": {
            "container": "docker",
            "orchestrator": "kubernetes",
        },
        "security": {"frameworks": ["owasp"]},
    }


def _minimal_config_dict() -> Dict[str, Any]:
    return {
        "project": {"name": "mini-cli", "purpose": "Minimal CLI"},
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
    return tmp_path / "output"


@pytest.fixture
def full_config() -> ProjectConfig:
    return ProjectConfig.from_dict(_full_config_dict())


@pytest.fixture
def minimal_config() -> ProjectConfig:
    return ProjectConfig.from_dict(_minimal_config_dict())


@pytest.fixture
def assembler() -> RulesAssembler:
    return RulesAssembler()


class TestCopyCoreRules:
    def test_copies_all_core_rule_files(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        src_tree: Path, output_dir: Path,
    ) -> None:
        engine = TemplateEngine(src_tree, full_config)
        rules_dir = output_dir / "rules"
        rules_dir.mkdir(parents=True)
        result = assembler._copy_core_rules(full_config, src_tree, rules_dir, engine)
        assert len(result) >= 2

    def test_replaces_placeholders(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        src_tree: Path, output_dir: Path,
    ) -> None:
        engine = TemplateEngine(src_tree, full_config)
        rules_dir = output_dir / "rules"
        rules_dir.mkdir(parents=True)
        assembler._copy_core_rules(full_config, src_tree, rules_dir, engine)
        coding = rules_dir / "03-coding-standards.md"
        content = coding.read_text(encoding="utf-8")
        assert "python" in content

    def test_missing_core_rules_dir_returns_empty(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        tmp_path: Path,
    ) -> None:
        engine = TemplateEngine(tmp_path, full_config)
        result = assembler._copy_core_rules(
            full_config, tmp_path / "empty", tmp_path, engine,
        )
        assert result == []


class TestRouteCoreToKps:
    def test_routes_to_correct_kp_dirs(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        src_tree: Path, output_dir: Path,
    ) -> None:
        skills_dir = output_dir / "skills"
        skills_dir.mkdir(parents=True)
        result = assembler._route_core_to_kps(full_config, src_tree, skills_dir)
        assert len(result) >= 2
        kp_names = [p.parent.parent.name for p in result]
        assert "coding-standards" in kp_names

    def test_cloud_native_included_for_non_library(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        src_tree: Path, output_dir: Path,
    ) -> None:
        skills_dir = output_dir / "skills"
        skills_dir.mkdir(parents=True)
        result = assembler._route_core_to_kps(full_config, src_tree, skills_dir)
        names = [p.name for p in result]
        assert "cloud-native-principles.md" in names

    def test_cloud_native_excluded_for_library(
        self, assembler: RulesAssembler, minimal_config: ProjectConfig,
        src_tree: Path, output_dir: Path,
    ) -> None:
        skills_dir = output_dir / "skills"
        skills_dir.mkdir(parents=True)
        result = assembler._route_core_to_kps(minimal_config, src_tree, skills_dir)
        names = [p.name for p in result]
        assert "cloud-native-principles.md" not in names


class TestCopyLanguageKps:
    def test_routes_testing_files_to_testing_kp(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        src_tree: Path, output_dir: Path,
    ) -> None:
        skills_dir = output_dir / "skills"
        skills_dir.mkdir(parents=True)
        result = assembler._copy_language_kps(full_config, src_tree, skills_dir)
        testing_files = [
            p for p in result
            if "testing" in str(p.parent)
        ]
        assert len(testing_files) >= 1

    def test_routes_coding_files_to_coding_standards(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        src_tree: Path, output_dir: Path,
    ) -> None:
        skills_dir = output_dir / "skills"
        skills_dir.mkdir(parents=True)
        result = assembler._copy_language_kps(full_config, src_tree, skills_dir)
        coding_files = [
            p for p in result
            if "coding-standards" in str(p.parent)
        ]
        assert len(coding_files) >= 1

    def test_copies_version_specific_files(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        src_tree: Path, output_dir: Path,
    ) -> None:
        skills_dir = output_dir / "skills"
        skills_dir.mkdir(parents=True)
        result = assembler._copy_language_kps(full_config, src_tree, skills_dir)
        names = [p.name for p in result]
        assert "version-features.md" in names


class TestCopyFrameworkKps:
    def test_uses_stack_pack_name(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        src_tree: Path, output_dir: Path,
    ) -> None:
        skills_dir = output_dir / "skills"
        skills_dir.mkdir(parents=True)
        result = assembler._copy_framework_kps(full_config, src_tree, skills_dir)
        assert len(result) >= 1
        assert any("click-cli-patterns" in str(p) for p in result)


class TestGenerateProjectIdentity:
    def test_contains_config_values(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        output_dir: Path,
    ) -> None:
        rules_dir = output_dir / "rules"
        rules_dir.mkdir(parents=True)
        result = assembler._generate_project_identity(full_config, rules_dir)
        content = result.read_text(encoding="utf-8")
        assert "test-svc" in content
        assert "python" in content
        assert "click" in content

    def test_generates_01_file(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        output_dir: Path,
    ) -> None:
        rules_dir = output_dir / "rules"
        rules_dir.mkdir(parents=True)
        result = assembler._generate_project_identity(full_config, rules_dir)
        assert result.name == "01-project-identity.md"


class TestCopyDomainTemplate:
    def test_copies_with_placeholder_replacement(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        src_tree: Path, output_dir: Path,
    ) -> None:
        engine = TemplateEngine(src_tree, full_config)
        rules_dir = output_dir / "rules"
        rules_dir.mkdir(parents=True)
        result = assembler._copy_domain_template(
            full_config, src_tree, rules_dir, engine,
        )
        content = result.read_text(encoding="utf-8")
        assert "test-svc" in content

    def test_fallback_when_template_missing(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        tmp_path: Path, output_dir: Path,
    ) -> None:
        engine = TemplateEngine(tmp_path, full_config)
        rules_dir = output_dir / "rules"
        rules_dir.mkdir(parents=True)
        empty_src = tmp_path / "empty-src"
        empty_src.mkdir()
        result = assembler._copy_domain_template(
            full_config, empty_src, rules_dir, engine,
        )
        assert result.exists()


class TestCopyDatabaseRefs:
    def test_copies_for_postgresql(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        src_tree: Path, output_dir: Path,
    ) -> None:
        engine = TemplateEngine(src_tree, full_config)
        skills_dir = output_dir / "skills"
        skills_dir.mkdir(parents=True)
        result = assembler._copy_database_refs(
            full_config, src_tree, skills_dir, engine,
        )
        assert len(result) >= 2
        names = [p.name for p in result]
        assert "version-matrix.md" in names

    def test_skipped_for_none(
        self, assembler: RulesAssembler, minimal_config: ProjectConfig,
        src_tree: Path, output_dir: Path,
    ) -> None:
        engine = TemplateEngine(src_tree, minimal_config)
        skills_dir = output_dir / "skills"
        skills_dir.mkdir(parents=True)
        result = assembler._copy_database_refs(
            minimal_config, src_tree, skills_dir, engine,
        )
        assert result == []


class TestCopyCacheRefs:
    def test_copies_for_redis(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        src_tree: Path, output_dir: Path,
    ) -> None:
        skills_dir = output_dir / "skills"
        skills_dir.mkdir(parents=True)
        result = assembler._copy_cache_refs(
            full_config, src_tree, skills_dir,
        )
        assert len(result) >= 1

    def test_skipped_for_none(
        self, assembler: RulesAssembler, minimal_config: ProjectConfig,
        src_tree: Path, output_dir: Path,
    ) -> None:
        skills_dir = output_dir / "skills"
        skills_dir.mkdir(parents=True)
        result = assembler._copy_cache_refs(
            minimal_config, src_tree, skills_dir,
        )
        assert result == []


class TestAssembleSecurityRules:
    def test_copies_when_configured(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        src_tree: Path, output_dir: Path,
    ) -> None:
        skills_dir = output_dir / "skills"
        skills_dir.mkdir(parents=True)
        result = assembler._assemble_security_rules(
            full_config, src_tree, skills_dir,
        )
        assert len(result) >= 2
        names = [p.name for p in result]
        assert "application-security.md" in names

    def test_skipped_when_no_frameworks(
        self, assembler: RulesAssembler, minimal_config: ProjectConfig,
        src_tree: Path, output_dir: Path,
    ) -> None:
        skills_dir = output_dir / "skills"
        skills_dir.mkdir(parents=True)
        result = assembler._assemble_security_rules(
            minimal_config, src_tree, skills_dir,
        )
        assert result == []

    def test_copies_compliance_files(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        src_tree: Path, output_dir: Path,
    ) -> None:
        skills_dir = output_dir / "skills"
        skills_dir.mkdir(parents=True)
        result = assembler._assemble_security_rules(
            full_config, src_tree, skills_dir,
        )
        names = [p.name for p in result]
        assert "owasp.md" in names


class TestAssembleInfraKnowledge:
    def test_copies_k8s_for_kubernetes(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        src_tree: Path, output_dir: Path,
    ) -> None:
        skills_dir = output_dir / "skills"
        skills_dir.mkdir(parents=True)
        result = assembler._assemble_infra_knowledge(
            full_config, src_tree, skills_dir,
        )
        names = [p.name for p in result]
        assert "k8s-deployment.md" in names

    def test_copies_container_files(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        src_tree: Path, output_dir: Path,
    ) -> None:
        skills_dir = output_dir / "skills"
        skills_dir.mkdir(parents=True)
        result = assembler._assemble_infra_knowledge(
            full_config, src_tree, skills_dir,
        )
        names = [p.name for p in result]
        assert "dockerfile.md" in names

    def test_no_k8s_for_minimal(
        self, assembler: RulesAssembler, minimal_config: ProjectConfig,
        src_tree: Path, output_dir: Path,
    ) -> None:
        skills_dir = output_dir / "skills"
        skills_dir.mkdir(parents=True)
        result = assembler._assemble_infra_knowledge(
            minimal_config, src_tree, skills_dir,
        )
        names = [p.name for p in result]
        assert "k8s-deployment.md" not in names


class TestCopyLangVersionNoMatch:
    def test_version_dir_not_found_returns_empty(
        self, assembler: RulesAssembler, tmp_path: Path,
    ) -> None:
        lang_dir = tmp_path / "languages" / "go"
        lang_dir.mkdir(parents=True)
        data = _minimal_config_dict()
        data["language"] = {"name": "go", "version": "1.23"}
        config = ProjectConfig.from_dict(data)
        coding_refs = tmp_path / "coding"
        coding_refs.mkdir()
        result = assembler._copy_lang_version(config, lang_dir, coding_refs)
        assert result == []


class TestCloudKnowledgeWithProvider:
    def test_skipped_when_no_cloud_provider_attr(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        src_tree: Path, output_dir: Path,
    ) -> None:
        skills_dir = output_dir / "skills"
        skills_dir.mkdir(parents=True)
        result = assembler._assemble_cloud_knowledge(
            full_config, src_tree, skills_dir,
        )
        assert result == []


class TestIacWithFile:
    def test_skipped_when_infra_has_no_iac_attr(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        src_tree: Path, output_dir: Path,
    ) -> None:
        """InfraConfig has no iac field — getattr fallback returns 'none'."""
        kp_dir = output_dir / "kp"
        kp_dir.mkdir(parents=True)
        result = assembler._copy_iac_files(
            full_config, src_tree / "infrastructure", kp_dir,
        )
        assert result == []


class TestAssembleWithAuditWarnings:
    def test_assemble_logs_warnings_without_crash(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        src_tree: Path, output_dir: Path,
    ) -> None:
        engine = TemplateEngine(src_tree, full_config)
        rules_dir = output_dir / "rules"
        rules_dir.mkdir(parents=True)
        for i in range(12):
            (rules_dir / f"pre-rule-{i:02d}.md").write_text(
                "x" * 5000, encoding="utf-8",
            )
        result = assembler.assemble(full_config, output_dir, engine)
        assert len(result) >= 10


class TestCopyFwVersion:
    def test_copies_version_specific_files(
        self, assembler: RulesAssembler, src_tree: Path, output_dir: Path,
    ) -> None:
        fw_dir = src_tree / "frameworks" / "click"
        ver_dir = fw_dir / "click-8.1"
        ver_dir.mkdir(parents=True)
        (ver_dir / "version-specific.md").write_text("# v8.1\n", encoding="utf-8")
        refs_dir = output_dir / "refs"
        refs_dir.mkdir(parents=True)
        config = ProjectConfig.from_dict(_full_config_dict())
        result = assembler._copy_fw_version(config, fw_dir, refs_dir)
        assert len(result) == 1
        assert result[0].name == "version-specific.md"


class TestCopyDbTypeFiles:
    def test_nosql_mongodb(
        self, assembler: RulesAssembler, src_tree: Path, output_dir: Path,
    ) -> None:
        nosql_common = src_tree / "databases" / "nosql" / "common"
        nosql_common.mkdir(parents=True)
        (nosql_common / "nosql-patterns.md").write_text("# NoSQL\n", encoding="utf-8")
        target = output_dir / "target"
        target.mkdir(parents=True)
        result = assembler._copy_db_type_files("mongodb", src_tree / "databases", target)
        assert len(result) >= 1


class TestAssembleCloudKnowledge:
    def test_skipped_when_no_provider(
        self, assembler: RulesAssembler, minimal_config: ProjectConfig,
        src_tree: Path, output_dir: Path,
    ) -> None:
        skills_dir = output_dir / "skills"
        skills_dir.mkdir(parents=True)
        result = assembler._assemble_cloud_knowledge(
            minimal_config, src_tree, skills_dir,
        )
        assert result == []


class TestCopyContainerFiles:
    def test_skipped_when_container_none(
        self, assembler: RulesAssembler, tmp_path: Path,
    ) -> None:
        data = _minimal_config_dict()
        data["infrastructure"] = {"container": "none"}
        config = ProjectConfig.from_dict(data)
        result = assembler._copy_container_files(config, tmp_path, tmp_path)
        assert result == []


class TestCopyIacFiles:
    def test_skipped_when_no_iac(
        self, assembler: RulesAssembler, minimal_config: ProjectConfig,
        tmp_path: Path,
    ) -> None:
        result = assembler._copy_iac_files(minimal_config, tmp_path, tmp_path)
        assert result == []


class TestMissingDirectories:
    def test_route_core_missing_dir(
        self, assembler: RulesAssembler, minimal_config: ProjectConfig,
        tmp_path: Path,
    ) -> None:
        result = assembler._route_core_to_kps(
            minimal_config, tmp_path / "nope", tmp_path,
        )
        assert result == []

    def test_copy_language_missing_dir(
        self, assembler: RulesAssembler, minimal_config: ProjectConfig,
        tmp_path: Path,
    ) -> None:
        result = assembler._copy_language_kps(
            minimal_config, tmp_path / "nope", tmp_path,
        )
        assert result == []

    def test_copy_lang_common_missing(
        self, assembler: RulesAssembler, tmp_path: Path,
    ) -> None:
        lang_dir = tmp_path / "lang"
        lang_dir.mkdir()
        result = assembler._copy_lang_common(lang_dir, tmp_path, tmp_path)
        assert result == []

    def test_copy_framework_unknown(
        self, assembler: RulesAssembler, tmp_path: Path,
    ) -> None:
        data = _minimal_config_dict()
        data["framework"] = {"name": "unknown-fw", "version": "1.0"}
        config = ProjectConfig.from_dict(data)
        result = assembler._copy_framework_kps(config, tmp_path, tmp_path)
        assert result == []

    def test_copy_framework_dir_missing(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        tmp_path: Path,
    ) -> None:
        result = assembler._copy_framework_kps(full_config, tmp_path, tmp_path)
        assert result == []

    def test_copy_fw_common_missing(
        self, assembler: RulesAssembler, tmp_path: Path,
    ) -> None:
        fw_dir = tmp_path / "fw"
        fw_dir.mkdir()
        result = assembler._copy_fw_common(fw_dir, tmp_path)
        assert result == []

    def test_copy_db_version_matrix_missing(
        self, assembler: RulesAssembler, tmp_path: Path,
    ) -> None:
        db_dir = tmp_path / "db"
        db_dir.mkdir()
        result = assembler._copy_db_version_matrix(db_dir, tmp_path)
        assert result == []

    def test_copy_k8s_missing_file(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        tmp_path: Path,
    ) -> None:
        infra = tmp_path / "infra"
        infra.mkdir()
        result = assembler._copy_k8s_files(full_config, infra, tmp_path)
        assert result == []

    def test_copy_security_missing_files(
        self, assembler: RulesAssembler, tmp_path: Path,
    ) -> None:
        sec_dir = tmp_path / "sec"
        sec_dir.mkdir()
        result = assembler._copy_security_base(sec_dir, tmp_path)
        assert result == []


class TestAuditWarningLogged:
    def test_audit_warnings_logged(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        src_tree: Path, output_dir: Path,
    ) -> None:
        engine = TemplateEngine(src_tree, full_config)
        rules_dir = output_dir / "rules"
        rules_dir.mkdir(parents=True)
        for i in range(12):
            (rules_dir / f"rule-{i:02d}.md").write_text("x" * 5000, encoding="utf-8")
        assembler._copy_core_rules(full_config, src_tree, rules_dir, engine)
        # Force audit via assemble path — warnings should be logged not crash
        result = audit_rules_context(rules_dir)
        assert len(result.warnings) >= 1


class TestCopyDbTypeNosql:
    def test_nosql_common_and_specific(
        self, assembler: RulesAssembler, src_tree: Path, output_dir: Path,
    ) -> None:
        nosql_mongo = src_tree / "databases" / "nosql" / "mongodb"
        nosql_mongo.mkdir(parents=True)
        (nosql_mongo / "mongodb-patterns.md").write_text("# Mongo\n", encoding="utf-8")
        target = output_dir / "target"
        target.mkdir(parents=True)
        result = assembler._copy_db_type_files(
            "mongodb", src_tree / "databases", target,
        )
        assert len(result) >= 1

    def test_unknown_db_type_returns_empty(
        self, assembler: RulesAssembler, src_tree: Path, output_dir: Path,
    ) -> None:
        target = output_dir / "target"
        target.mkdir(parents=True)
        result = assembler._copy_db_type_files(
            "unknown-db", src_tree / "databases", target,
        )
        assert result == []


class TestAssembleFullPipeline:
    def test_full_config_generates_files(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        src_tree: Path, output_dir: Path,
    ) -> None:
        engine = TemplateEngine(src_tree, full_config)
        result = assembler.assemble(full_config, output_dir, engine)
        assert len(result) >= 10

    def test_returns_existing_paths(
        self, assembler: RulesAssembler, full_config: ProjectConfig,
        src_tree: Path, output_dir: Path,
    ) -> None:
        engine = TemplateEngine(src_tree, full_config)
        result = assembler.assemble(full_config, output_dir, engine)
        for path in result:
            assert path.exists(), f"Path does not exist: {path}"

    def test_minimal_config_generates_core_files(
        self, assembler: RulesAssembler, minimal_config: ProjectConfig,
        src_tree: Path, output_dir: Path,
    ) -> None:
        engine = TemplateEngine(src_tree, minimal_config)
        result = assembler.assemble(minimal_config, output_dir, engine)
        assert len(result) >= 5
        names = [p.name for p in result]
        assert "01-project-identity.md" in names
        assert "02-domain.md" in names
