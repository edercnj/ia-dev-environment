from __future__ import annotations

import copy
import logging
from pathlib import Path

import pytest
import yaml

from claude_setup.assembler.github_prompts_assembler import (
    PROMPT_TEMPLATES,
    GithubPromptsAssembler,
)
from claude_setup.models import ProjectConfig
from claude_setup.template_engine import TemplateEngine

from tests.conftest import FULL_PROJECT_DICT, MINIMAL_PROJECT_DICT

EXPECTED_PROMPT_COUNT = 4

EXPECTED_OUTPUT_NAMES = (
    "new-feature.prompt.md",
    "decompose-spec.prompt.md",
    "code-review.prompt.md",
    "troubleshoot.prompt.md",
)

REQUIRED_FRONTMATTER_FIELDS = ("name", "description")

EXPECTED_FRONTMATTER_NAMES = {
    "new-feature.prompt.md": "new-feature",
    "decompose-spec.prompt.md": "decompose-spec",
    "code-review.prompt.md": "code-review",
    "troubleshoot.prompt.md": "troubleshoot",
}


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


def _project_resources() -> Path:
    return (
        Path(__file__).resolve().parent.parent.parent
        / "resources"
    )


def _build_assembler() -> GithubPromptsAssembler:
    return GithubPromptsAssembler(_project_resources())


def _build_engine(config: ProjectConfig) -> TemplateEngine:
    return TemplateEngine(_project_resources(), config)


def _parse_frontmatter(content: str) -> dict:
    """Extract YAML frontmatter from prompt content."""
    if not content.startswith("---"):
        return {}
    parts = content.split("---", 2)
    if len(parts) < 3:
        return {}
    return yaml.safe_load(parts[1]) or {}


class TestAssembleReturnsAllPrompts:

    def test_returns_four_paths(self, tmp_path: Path) -> None:
        config = _make_config()
        engine = _build_engine(config)
        assembler = _build_assembler()
        result = assembler.assemble(config, tmp_path, engine)
        assert len(result) == EXPECTED_PROMPT_COUNT

    def test_all_paths_exist(self, tmp_path: Path) -> None:
        config = _make_config()
        engine = _build_engine(config)
        assembler = _build_assembler()
        result = assembler.assemble(config, tmp_path, engine)
        for path in result:
            assert path.is_file()

    def test_output_filenames(self, tmp_path: Path) -> None:
        config = _make_config()
        engine = _build_engine(config)
        assembler = _build_assembler()
        result = assembler.assemble(config, tmp_path, engine)
        names = {p.name for p in result}
        assert names == set(EXPECTED_OUTPUT_NAMES)

    def test_files_in_prompts_directory(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        engine = _build_engine(config)
        assembler = _build_assembler()
        result = assembler.assemble(config, tmp_path, engine)
        for path in result:
            assert path.parent.name == "prompts"
            assert path.parent.parent.name == "github"

    def test_prompt_extension(self, tmp_path: Path) -> None:
        config = _make_config()
        engine = _build_engine(config)
        assembler = _build_assembler()
        result = assembler.assemble(config, tmp_path, engine)
        for path in result:
            assert path.name.endswith(".prompt.md")


class TestFrontmatterValidity:

    def test_all_prompts_have_frontmatter(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        engine = _build_engine(config)
        assembler = _build_assembler()
        result = assembler.assemble(config, tmp_path, engine)
        for path in result:
            content = path.read_text(encoding="utf-8")
            assert content.startswith("---")

    def test_frontmatter_has_name(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        engine = _build_engine(config)
        assembler = _build_assembler()
        result = assembler.assemble(config, tmp_path, engine)
        for path in result:
            fm = _parse_frontmatter(
                path.read_text(encoding="utf-8"),
            )
            assert "name" in fm

    def test_frontmatter_has_description(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        engine = _build_engine(config)
        assembler = _build_assembler()
        result = assembler.assemble(config, tmp_path, engine)
        for path in result:
            fm = _parse_frontmatter(
                path.read_text(encoding="utf-8"),
            )
            assert "description" in fm

    def test_frontmatter_name_matches_filename(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        engine = _build_engine(config)
        assembler = _build_assembler()
        result = assembler.assemble(config, tmp_path, engine)
        for path in result:
            fm = _parse_frontmatter(
                path.read_text(encoding="utf-8"),
            )
            expected = EXPECTED_FRONTMATTER_NAMES[path.name]
            assert fm["name"] == expected

    def test_frontmatter_name_is_lowercase_hyphens(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        engine = _build_engine(config)
        assembler = _build_assembler()
        result = assembler.assemble(config, tmp_path, engine)
        for path in result:
            fm = _parse_frontmatter(
                path.read_text(encoding="utf-8"),
            )
            name = fm["name"]
            assert name == name.lower()
            assert " " not in name


class TestTemplateRendering:

    def test_project_name_rendered(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        engine = _build_engine(config)
        assembler = _build_assembler()
        assembler.assemble(config, tmp_path, engine)
        prompts_dir = tmp_path / "github" / "prompts"
        for path in prompts_dir.iterdir():
            content = path.read_text(encoding="utf-8")
            assert "my-service" in content
            assert "{{ project_name }}" not in content

    def test_no_jinja2_markers_in_output(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        engine = _build_engine(config)
        assembler = _build_assembler()
        assembler.assemble(config, tmp_path, engine)
        prompts_dir = tmp_path / "github" / "prompts"
        for path in prompts_dir.iterdir():
            content = path.read_text(encoding="utf-8")
            assert "{{" not in content
            assert "}}" not in content

    def test_minimal_config_renders(
        self, tmp_path: Path,
    ) -> None:
        config = _make_minimal_config()
        engine = _build_engine(config)
        assembler = _build_assembler()
        result = assembler.assemble(config, tmp_path, engine)
        assert len(result) == EXPECTED_PROMPT_COUNT


class TestSkillAndAgentReferences:

    def test_new_feature_references_skills(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        engine = _build_engine(config)
        assembler = _build_assembler()
        assembler.assemble(config, tmp_path, engine)
        content = (
            tmp_path / "github" / "prompts"
            / "new-feature.prompt.md"
        ).read_text(encoding="utf-8")
        assert "x-dev-lifecycle" in content
        assert "x-dev-implement" in content
        assert "x-review" in content

    def test_new_feature_references_agents(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        engine = _build_engine(config)
        assembler = _build_assembler()
        assembler.assemble(config, tmp_path, engine)
        content = (
            tmp_path / "github" / "prompts"
            / "new-feature.prompt.md"
        ).read_text(encoding="utf-8")
        assert "developer" in content
        assert "tech-lead" in content

    def test_decompose_references_epic_full(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        engine = _build_engine(config)
        assembler = _build_assembler()
        assembler.assemble(config, tmp_path, engine)
        content = (
            tmp_path / "github" / "prompts"
            / "decompose-spec.prompt.md"
        ).read_text(encoding="utf-8")
        assert "x-story-epic-full" in content

    def test_code_review_references_review_skills(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        engine = _build_engine(config)
        assembler = _build_assembler()
        assembler.assemble(config, tmp_path, engine)
        content = (
            tmp_path / "github" / "prompts"
            / "code-review.prompt.md"
        ).read_text(encoding="utf-8")
        assert "x-review" in content
        assert "x-review-api" in content
        assert "x-review-pr" in content

    def test_code_review_references_agents(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        engine = _build_engine(config)
        assembler = _build_assembler()
        assembler.assemble(config, tmp_path, engine)
        content = (
            tmp_path / "github" / "prompts"
            / "code-review.prompt.md"
        ).read_text(encoding="utf-8")
        assert "tech-lead" in content.lower()
        assert "security" in content.lower()
        assert "qa" in content.lower()

    def test_troubleshoot_references_skill(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        engine = _build_engine(config)
        assembler = _build_assembler()
        assembler.assemble(config, tmp_path, engine)
        content = (
            tmp_path / "github" / "prompts"
            / "troubleshoot.prompt.md"
        ).read_text(encoding="utf-8")
        assert "x-ops-troubleshoot" in content

    def test_troubleshoot_methodology_steps(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        engine = _build_engine(config)
        assembler = _build_assembler()
        assembler.assemble(config, tmp_path, engine)
        content = (
            tmp_path / "github" / "prompts"
            / "troubleshoot.prompt.md"
        ).read_text(encoding="utf-8")
        for step in ("Reproduce", "Locate", "Understand",
                      "Fix", "Verify"):
            assert step in content


class TestMissingTemplates:

    def test_missing_templates_dir_returns_empty(
        self, tmp_path: Path,
    ) -> None:
        resources = tmp_path / "no-resources"
        resources.mkdir()
        config = _make_config()
        engine = _build_engine(config)
        assembler = GithubPromptsAssembler(resources)
        result = assembler.assemble(config, tmp_path, engine)
        assert result == []

    def test_missing_templates_dir_logs_warning(
        self, tmp_path: Path, caplog,
    ) -> None:
        resources = tmp_path / "no-resources"
        resources.mkdir()
        config = _make_config()
        engine = _build_engine(config)
        assembler = GithubPromptsAssembler(resources)
        with caplog.at_level(logging.WARNING):
            assembler.assemble(config, tmp_path, engine)
        assert "Templates dir not found" in caplog.text

    def test_partial_templates_generates_available(
        self, tmp_path: Path,
    ) -> None:
        resources = tmp_path / "partial-resources"
        templates_dir = (
            resources / "github-prompts-templates"
        )
        templates_dir.mkdir(parents=True)
        template = templates_dir / "new-feature.prompt.md.j2"
        template.write_text(
            "---\nname: new-feature\n"
            "description: test\n---\n# Test\n",
            encoding="utf-8",
        )
        config = _make_config()
        engine = TemplateEngine(resources, config)
        assembler = GithubPromptsAssembler(resources)
        result = assembler.assemble(config, tmp_path, engine)
        assert len(result) == 1
        assert result[0].name == "new-feature.prompt.md"

    def test_missing_single_template_logs_warning(
        self, tmp_path: Path, caplog,
    ) -> None:
        resources = tmp_path / "partial-resources"
        templates_dir = (
            resources / "github-prompts-templates"
        )
        templates_dir.mkdir(parents=True)
        config = _make_config()
        engine = TemplateEngine(resources, config)
        assembler = GithubPromptsAssembler(resources)
        with caplog.at_level(logging.WARNING):
            assembler.assemble(config, tmp_path, engine)
        assert "Prompt template not found" in caplog.text


class TestIdempotency:

    def test_double_run_produces_same_result(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        engine = _build_engine(config)
        assembler = _build_assembler()
        result1 = assembler.assemble(config, tmp_path, engine)
        result2 = assembler.assemble(config, tmp_path, engine)
        names1 = sorted(p.name for p in result1)
        names2 = sorted(p.name for p in result2)
        assert names1 == names2
