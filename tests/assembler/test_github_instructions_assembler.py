from __future__ import annotations

from pathlib import Path

import pytest

from ia_dev_env.assembler.github_instructions_assembler import (
    CONTEXTUAL_TEMPLATES,
    GithubInstructionsAssembler,
    _build_copilot_instructions,
)
from ia_dev_env.models import ProjectConfig
from ia_dev_env.template_engine import TemplateEngine

from tests.conftest import FULL_PROJECT_DICT, MINIMAL_PROJECT_DICT

import copy


def _make_config(**overrides) -> ProjectConfig:
    base = copy.deepcopy(FULL_PROJECT_DICT)
    for key, value in overrides.items():
        base[key] = value
    return ProjectConfig.from_dict(base)


def _make_minimal_config(**overrides) -> ProjectConfig:
    base = copy.deepcopy(MINIMAL_PROJECT_DICT)
    for key, value in overrides.items():
        base[key] = value
    return ProjectConfig.from_dict(base)


def _create_templates(base: Path) -> Path:
    """Create template files matching resources/github-instructions-templates/."""
    tpl_dir = base / "github-instructions-templates"
    tpl_dir.mkdir(parents=True)
    for name in CONTEXTUAL_TEMPLATES:
        (tpl_dir / f"{name}.md").write_text(
            f"# {name}\n\nProject: {{project_name}}\n",
            encoding="utf-8",
        )
    return base


class TestBuildCopilotInstructions:
    def test_contains_project_name(self) -> None:
        config = _make_config()
        content = _build_copilot_instructions(config)
        assert "my-service" in content

    def test_contains_architecture_style(self) -> None:
        config = _make_config()
        content = _build_copilot_instructions(config)
        assert "hexagonal" in content

    def test_contains_language_and_version(self) -> None:
        config = _make_config()
        content = _build_copilot_instructions(config)
        assert "python 3.9" in content

    def test_contains_framework_with_version(self) -> None:
        config = _make_config()
        content = _build_copilot_instructions(config)
        assert "click" in content
        assert "8.1" in content

    def test_interfaces_listed(self) -> None:
        config = _make_config()
        content = _build_copilot_instructions(config)
        assert "REST" in content
        assert "GRPC" in content

    def test_empty_interfaces_shows_none(self) -> None:
        config = _make_config(interfaces=[])
        content = _build_copilot_instructions(config)
        assert "**Interfaces:** none" in content

    def test_framework_without_version(self) -> None:
        config = _make_config(
            framework={
                "name": "flask",
                "version": "",
                "build_tool": "pip",
            },
        )
        content = _build_copilot_instructions(config)
        assert "Flask" in content

    def test_contains_constraints(self) -> None:
        config = _make_config()
        content = _build_copilot_instructions(config)
        assert "Cloud-Agnostic" in content
        assert "Horizontal scalability" in content

    def test_contains_contextual_instructions_section(self) -> None:
        config = _make_config()
        content = _build_copilot_instructions(config)
        assert "Contextual Instructions" in content
        assert "domain.instructions.md" in content
        assert "coding-standards.instructions.md" in content
        assert "architecture.instructions.md" in content
        assert "quality-gates.instructions.md" in content

    def test_ends_with_newline(self) -> None:
        config = _make_config()
        content = _build_copilot_instructions(config)
        assert content.endswith("\n")

    def test_no_yaml_frontmatter(self) -> None:
        config = _make_config()
        content = _build_copilot_instructions(config)
        assert not content.startswith("---")


class TestGenerateGlobal:
    def test_creates_copilot_instructions_file(self, tmp_path: Path) -> None:
        config = _make_config()
        resources = _create_templates(tmp_path / "res")
        assembler = GithubInstructionsAssembler(resources)
        github_dir = tmp_path / "github"
        github_dir.mkdir()
        result = assembler._generate_global(config, github_dir)

        assert result.name == "copilot-instructions.md"
        assert result.exists()

    def test_file_contains_project_name(self, tmp_path: Path) -> None:
        config = _make_config()
        resources = _create_templates(tmp_path / "res")
        assembler = GithubInstructionsAssembler(resources)
        github_dir = tmp_path / "github"
        github_dir.mkdir()
        assembler._generate_global(config, github_dir)

        content = (github_dir / "copilot-instructions.md").read_text(
            encoding="utf-8",
        )
        assert "my-service" in content


class TestGenerateContextual:
    def test_generates_four_instruction_files(self, tmp_path: Path) -> None:
        config = _make_config()
        resources = _create_templates(tmp_path / "res")
        assembler = GithubInstructionsAssembler(resources)
        instructions_dir = tmp_path / "instructions"
        instructions_dir.mkdir()
        engine = TemplateEngine(tmp_path, config)
        result = assembler._generate_contextual(engine, instructions_dir)

        assert len(result) == 4

    def test_files_have_instructions_md_extension(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        resources = _create_templates(tmp_path / "res")
        assembler = GithubInstructionsAssembler(resources)
        instructions_dir = tmp_path / "instructions"
        instructions_dir.mkdir()
        engine = TemplateEngine(tmp_path, config)
        result = assembler._generate_contextual(engine, instructions_dir)

        for path in result:
            assert path.name.endswith(".instructions.md")

    def test_placeholders_replaced(self, tmp_path: Path) -> None:
        config = _make_config()
        resources = _create_templates(tmp_path / "res")
        assembler = GithubInstructionsAssembler(resources)
        instructions_dir = tmp_path / "instructions"
        instructions_dir.mkdir()
        engine = TemplateEngine(tmp_path, config)
        assembler._generate_contextual(engine, instructions_dir)

        domain = instructions_dir / "domain.instructions.md"
        content = domain.read_text(encoding="utf-8")
        assert "my-service" in content
        assert "{project_name}" not in content

    def test_missing_templates_dir_returns_empty(
        self, tmp_path: Path,
    ) -> None:
        config = _make_minimal_config()
        resources = tmp_path / "nonexistent"
        assembler = GithubInstructionsAssembler(resources)
        instructions_dir = tmp_path / "instructions"
        instructions_dir.mkdir()
        engine = TemplateEngine(tmp_path, config)

        result = assembler._generate_contextual(engine, instructions_dir)

        assert result == []

    def test_missing_individual_template_skipped(
        self, tmp_path: Path,
    ) -> None:
        config = _make_minimal_config()
        resources = tmp_path / "res"
        tpl_dir = resources / "github-instructions-templates"
        tpl_dir.mkdir(parents=True)
        (tpl_dir / "domain.md").write_text("# Domain\n", encoding="utf-8")
        # Only domain.md exists, other 3 are missing

        assembler = GithubInstructionsAssembler(resources)
        instructions_dir = tmp_path / "instructions"
        instructions_dir.mkdir()
        engine = TemplateEngine(tmp_path, config)

        result = assembler._generate_contextual(engine, instructions_dir)

        assert len(result) == 1
        assert result[0].name == "domain.instructions.md"


class TestAssemble:
    def test_returns_five_files(self, tmp_path: Path) -> None:
        config = _make_config()
        resources = _create_templates(tmp_path / "res")
        assembler = GithubInstructionsAssembler(resources)
        output_dir = tmp_path / "output"
        engine = TemplateEngine(tmp_path, config)

        result = assembler.assemble(config, output_dir, engine)

        assert len(result) == 5

    def test_creates_github_directory_structure(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        resources = _create_templates(tmp_path / "res")
        assembler = GithubInstructionsAssembler(resources)
        output_dir = tmp_path / "output"
        engine = TemplateEngine(tmp_path, config)

        assembler.assemble(config, output_dir, engine)

        assert (output_dir / "github").is_dir()
        assert (output_dir / "github" / "instructions").is_dir()

    def test_all_returned_paths_exist(self, tmp_path: Path) -> None:
        config = _make_config()
        resources = _create_templates(tmp_path / "res")
        assembler = GithubInstructionsAssembler(resources)
        output_dir = tmp_path / "output"
        engine = TemplateEngine(tmp_path, config)

        result = assembler.assemble(config, output_dir, engine)

        for path in result:
            assert path.exists(), f"Path does not exist: {path}"

    def test_global_file_in_github_root(self, tmp_path: Path) -> None:
        config = _make_config()
        resources = _create_templates(tmp_path / "res")
        assembler = GithubInstructionsAssembler(resources)
        output_dir = tmp_path / "output"
        engine = TemplateEngine(tmp_path, config)

        result = assembler.assemble(config, output_dir, engine)

        global_file = output_dir / "github" / "copilot-instructions.md"
        assert global_file in result
        assert global_file.exists()

    def test_contextual_files_in_instructions_subdir(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        resources = _create_templates(tmp_path / "res")
        assembler = GithubInstructionsAssembler(resources)
        output_dir = tmp_path / "output"
        engine = TemplateEngine(tmp_path, config)

        result = assembler.assemble(config, output_dir, engine)

        instructions_dir = output_dir / "github" / "instructions"
        contextual = [p for p in result if p.parent == instructions_dir]
        assert len(contextual) == 4
