from __future__ import annotations

from pathlib import Path

import pytest

from claude_setup.assembler.skills import SkillsAssembler
from claude_setup.models import ProjectConfig
from claude_setup.template_engine import TemplateEngine


@pytest.fixture
def assembler():
    return SkillsAssembler()


def _create_skill_template(
    src_dir: Path, category: str, skill_name: str, content: str,
) -> Path:
    """Helper to create a skill template file."""
    path = src_dir / "skills-templates" / category / skill_name
    path.mkdir(parents=True, exist_ok=True)
    skill_md = path / "SKILL.md"
    skill_md.write_text(content, encoding="utf-8")
    return path


def _create_kp_template(
    src_dir: Path, pack_name: str, content: str,
) -> Path:
    """Helper to create a knowledge pack template."""
    path = src_dir / "skills-templates" / "knowledge-packs" / pack_name
    path.mkdir(parents=True, exist_ok=True)
    (path / "SKILL.md").write_text(content, encoding="utf-8")
    return path


def _make_engine(src_dir: Path, config: ProjectConfig) -> TemplateEngine:
    return TemplateEngine(src_dir, config)


class TestCopyCoreSkill:

    def test_creates_directory(
        self, assembler, tmp_path, minimal_cli_config,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        _create_skill_template(src, "core", "coding-standards", "# CS")
        engine = _make_engine(src, minimal_cli_config)
        result = assembler._copy_core_skill(
            "coding-standards", src, output, engine,
        )
        assert result.exists()
        assert (output / "skills" / "coding-standards" / "SKILL.md").exists()

    def test_replaces_placeholders(
        self, assembler, tmp_path, minimal_cli_config,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        _create_skill_template(
            src, "core", "test-skill", "Lang: {language_name}",
        )
        engine = _make_engine(src, minimal_cli_config)
        assembler._copy_core_skill("test-skill", src, output, engine)
        content = (output / "skills" / "test-skill" / "SKILL.md").read_text(
            encoding="utf-8",
        )
        assert "python" in content
        assert "{language_name}" not in content


class TestCopyConditionalSkill:

    def test_exists_returns_path(
        self, assembler, tmp_path, minimal_cli_config,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        _create_skill_template(src, "conditional", "x-review-api", "# API")
        engine = _make_engine(src, minimal_cli_config)
        result = assembler._copy_conditional_skill(
            "x-review-api", src, output, engine,
        )
        assert result is not None
        assert result.exists()

    def test_missing_returns_none(
        self, assembler, tmp_path, minimal_cli_config,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        engine = _make_engine(src, minimal_cli_config)
        result = assembler._copy_conditional_skill(
            "nonexistent", src, output, engine,
        )
        assert result is None


class TestCopyKnowledgePack:

    def test_overwrites_skill_md(
        self, assembler, tmp_path, minimal_cli_config,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        _create_kp_template(src, "layer-templates", "NEW content")
        dest = output / "skills" / "layer-templates"
        dest.mkdir(parents=True)
        (dest / "SKILL.md").write_text("OLD content", encoding="utf-8")
        engine = _make_engine(src, minimal_cli_config)
        assembler._copy_knowledge_pack(
            "layer-templates", src, output, engine,
        )
        content = (dest / "SKILL.md").read_text(encoding="utf-8")
        assert "NEW content" in content
        assert "OLD content" not in content

    def test_preserves_existing_references(
        self, assembler, tmp_path, minimal_cli_config,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        kp_src = _create_kp_template(src, "layer-templates", "# KP")
        ref_src = kp_src / "references"
        ref_src.mkdir()
        (ref_src / "template.md").write_text(
            "template ref", encoding="utf-8",
        )
        dest = output / "skills" / "layer-templates" / "references"
        dest.mkdir(parents=True)
        (dest / "existing.md").write_text(
            "EXISTING", encoding="utf-8",
        )
        engine = _make_engine(src, minimal_cli_config)
        assembler._copy_knowledge_pack(
            "layer-templates", src, output, engine,
        )
        assert (dest / "existing.md").read_text(encoding="utf-8") == "EXISTING"

    def test_missing_source_returns_none(
        self, assembler, tmp_path, minimal_cli_config,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        engine = _make_engine(src, minimal_cli_config)
        result = assembler._copy_knowledge_pack(
            "nonexistent", src, output, engine,
        )
        assert result is None


class TestCopyStackPatterns:

    def test_known_framework_copies(
        self, assembler, tmp_path, minimal_cli_config,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        sp = (
            src / "skills-templates" / "knowledge-packs"
            / "stack-patterns" / "click-cli-patterns"
        )
        sp.mkdir(parents=True)
        (sp / "SKILL.md").write_text(
            "# Click patterns {project_name}", encoding="utf-8",
        )
        engine = _make_engine(src, minimal_cli_config)
        result = assembler._copy_stack_patterns(
            minimal_cli_config, src, output, engine,
        )
        assert result is not None
        content = (result / "SKILL.md").read_text(encoding="utf-8")
        assert "minimal-cli" in content

    def test_unknown_framework_returns_none(
        self, assembler, tmp_path, config_factory,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        config = config_factory(
            framework={"name": "unknown", "version": "1.0"},
        )
        engine = _make_engine(src, config)
        result = assembler._copy_stack_patterns(
            config, src, output, engine,
        )
        assert result is None


class TestCopyInfraPatterns:

    def test_kubernetes_copies_k8s_deployment(
        self, assembler, tmp_path, config_factory,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        config = config_factory(
            infrastructure={"orchestrator": "kubernetes"},
        )
        ip = (
            src / "skills-templates" / "knowledge-packs"
            / "infra-patterns" / "k8s-deployment"
        )
        ip.mkdir(parents=True)
        (ip / "SKILL.md").write_text("# K8s", encoding="utf-8")
        engine = _make_engine(src, config)
        result = assembler._copy_infra_patterns(
            config, src, output, engine,
        )
        assert len(result) >= 1
        names = [p.name for p in result]
        assert "k8s-deployment" in names

    def test_docker_copies_dockerfile(
        self, assembler, tmp_path, config_factory,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        config = config_factory(
            infrastructure={"container": "docker", "orchestrator": "none"},
        )
        ip = (
            src / "skills-templates" / "knowledge-packs"
            / "infra-patterns" / "dockerfile"
        )
        ip.mkdir(parents=True)
        (ip / "SKILL.md").write_text("# Docker", encoding="utf-8")
        engine = _make_engine(src, config)
        result = assembler._copy_infra_patterns(
            config, src, output, engine,
        )
        names = [p.name for p in result]
        assert "dockerfile" in names

    def test_all_none_returns_empty(
        self, assembler, tmp_path, config_factory,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        config = config_factory(
            infrastructure={
                "container": "none",
                "orchestrator": "none",
                "templating": "none",
                "iac": "none",
                "registry": "none",
            },
        )
        engine = _make_engine(src, config)
        result = assembler._copy_infra_patterns(
            config, src, output, engine,
        )
        assert result == []

    def test_terraform_copies_iac_terraform(
        self, assembler, tmp_path, config_factory,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        config = config_factory(
            infrastructure={"iac": "terraform"},
        )
        ip = (
            src / "skills-templates" / "knowledge-packs"
            / "infra-patterns" / "iac-terraform"
        )
        ip.mkdir(parents=True)
        (ip / "SKILL.md").write_text("# Terraform", encoding="utf-8")
        engine = _make_engine(src, config)
        result = assembler._copy_infra_patterns(
            config, src, output, engine,
        )
        names = [p.name for p in result]
        assert "iac-terraform" in names


class TestCoreSkillSkipsFiles:

    def test_non_dir_items_in_core_skipped(
        self, assembler, tmp_path,
    ) -> None:
        core = tmp_path / "skills-templates" / "core"
        core.mkdir(parents=True)
        (core / "readme.txt").write_text("skip me", encoding="utf-8")
        (core / "real-skill").mkdir()
        (core / "real-skill" / "SKILL.md").write_text("# Skill", encoding="utf-8")
        result = assembler.select_core_skills(tmp_path)
        assert "readme.txt" not in result
        assert "real-skill" in result


class TestKnowledgePackEdgeCases:

    def test_kp_without_skill_md(
        self, assembler, tmp_path, minimal_cli_config,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        kp = src / "skills-templates" / "knowledge-packs" / "empty-pack"
        kp.mkdir(parents=True)
        (kp / "other.txt").write_text("data", encoding="utf-8")
        engine = _make_engine(src, minimal_cli_config)
        result = assembler._copy_knowledge_pack(
            "empty-pack", src, output, engine,
        )
        assert result is not None
        assert not (result / "SKILL.md").exists()
        assert (result / "other.txt").exists()

    def test_kp_copies_new_file(
        self, assembler, tmp_path, minimal_cli_config,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        kp = src / "skills-templates" / "knowledge-packs" / "test-pack"
        kp.mkdir(parents=True)
        (kp / "SKILL.md").write_text("# Pack", encoding="utf-8")
        (kp / "extra.txt").write_text("extra data", encoding="utf-8")
        engine = _make_engine(src, minimal_cli_config)
        result = assembler._copy_knowledge_pack(
            "test-pack", src, output, engine,
        )
        assert (result / "extra.txt").read_text(encoding="utf-8") == "extra data"

    def test_kp_copies_new_subdir(
        self, assembler, tmp_path, minimal_cli_config,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        kp = src / "skills-templates" / "knowledge-packs" / "dir-pack"
        kp.mkdir(parents=True)
        (kp / "SKILL.md").write_text("# Pack", encoding="utf-8")
        sub = kp / "templates"
        sub.mkdir()
        (sub / "t.md").write_text("tmpl", encoding="utf-8")
        engine = _make_engine(src, minimal_cli_config)
        result = assembler._copy_knowledge_pack(
            "dir-pack", src, output, engine,
        )
        assert (result / "templates" / "t.md").exists()


class TestAssemble:

    def test_creates_skills_directory(
        self, assembler, tmp_path, minimal_cli_config,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        _create_skill_template(src, "core", "coding-standards", "# CS")
        engine = _make_engine(src, minimal_cli_config)
        assembler.assemble(minimal_cli_config, output, src, engine)
        assert (output / "skills").exists()

    def test_returns_list_of_paths(
        self, assembler, tmp_path, minimal_cli_config,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        _create_skill_template(src, "core", "coding-standards", "# CS")
        engine = _make_engine(src, minimal_cli_config)
        result = assembler.assemble(
            minimal_cli_config, output, src, engine,
        )
        assert isinstance(result, list)
        assert len(result) >= 1

    def test_replaces_placeholders_in_all(
        self, assembler, tmp_path, minimal_cli_config,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        _create_skill_template(
            src, "core", "cs", "Project: {project_name}",
        )
        engine = _make_engine(src, minimal_cli_config)
        assembler.assemble(minimal_cli_config, output, src, engine)
        content = (output / "skills" / "cs" / "SKILL.md").read_text(
            encoding="utf-8",
        )
        assert "{project_name}" not in content
        assert "minimal-cli" in content

    def test_writes_utf8(
        self, assembler, tmp_path, minimal_cli_config,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        _create_skill_template(
            src, "core", "utf-test", "Accented: cafe\u0301",
        )
        engine = _make_engine(src, minimal_cli_config)
        assembler.assemble(minimal_cli_config, output, src, engine)
        content = (output / "skills" / "utf-test" / "SKILL.md").read_text(
            encoding="utf-8",
        )
        assert "cafe\u0301" in content

    def test_assemble_full_with_conditional_and_kp(
        self, assembler, tmp_path, config_factory,
    ) -> None:
        src = tmp_path / "src"
        output = tmp_path / "out"
        config = config_factory(
            interfaces=[{"type": "rest"}],
            infrastructure={
                "orchestrator": "kubernetes",
                "container": "docker",
            },
            data={"database": {"name": "postgresql", "version": "15"}},
            testing={"smoke_tests": True, "performance_tests": True},
        )
        _create_skill_template(src, "core", "coding-standards", "# CS")
        _create_skill_template(
            src, "conditional", "x-review-api", "# API review",
        )
        _create_skill_template(src, "conditional", "run-e2e", "# E2E")
        _create_skill_template(
            src, "conditional", "run-smoke-api", "# Smoke",
        )
        _create_skill_template(
            src, "conditional", "run-perf-test", "# Perf",
        )
        _create_kp_template(src, "layer-templates", "# LT")
        _create_kp_template(src, "database-patterns", "# DB")
        sp = (
            src / "skills-templates" / "knowledge-packs"
            / "stack-patterns" / "click-cli-patterns"
        )
        sp.mkdir(parents=True)
        (sp / "SKILL.md").write_text("# Click", encoding="utf-8")
        ip = (
            src / "skills-templates" / "knowledge-packs"
            / "infra-patterns" / "k8s-deployment"
        )
        ip.mkdir(parents=True)
        (ip / "SKILL.md").write_text("# K8s", encoding="utf-8")
        ip2 = (
            src / "skills-templates" / "knowledge-packs"
            / "infra-patterns" / "dockerfile"
        )
        ip2.mkdir(parents=True)
        (ip2 / "SKILL.md").write_text("# Docker", encoding="utf-8")
        engine = _make_engine(src, config)
        result = assembler.assemble(config, output, src, engine)
        assert len(result) >= 5
        skill_names = [p.name for p in result]
        assert "coding-standards" in skill_names
        assert "x-review-api" in skill_names
        assert "layer-templates" in skill_names
