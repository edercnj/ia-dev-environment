from __future__ import annotations

import copy
from pathlib import Path
from typing import List

import pytest

from claude_setup.assembler.github_skills_assembler import (
    SKILL_GROUPS,
    GithubSkillsAssembler,
)
from claude_setup.models import ProjectConfig
from claude_setup.template_engine import TemplateEngine

from tests.conftest import FULL_PROJECT_DICT, MINIMAL_PROJECT_DICT

STORY_SKILLS = SKILL_GROUPS["story"]


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
    """Create template files for story skills."""
    tpl_dir = base / "github-skills-templates" / "story"
    tpl_dir.mkdir(parents=True)
    for name in STORY_SKILLS:
        (tpl_dir / f"{name}.md").write_text(
            f"---\nname: {name}\n"
            f"description: >\n  Skill {name} para {{project_name}}.\n"
            f"---\n\n# {name}\n\n"
            f"Projeto: {{project_name}}\n"
            f"Linguagem: {{language_name}}\n"
            f"Referência: `../../.claude/skills/{name}/references/`\n",
            encoding="utf-8",
        )
    return base


@pytest.fixture
def assembled_skills(tmp_path: Path) -> tuple:
    """Fixture: assemble all skills, return (result, output_dir)."""
    config = _make_config()
    resources = _create_templates(tmp_path / "res")
    assembler = GithubSkillsAssembler(resources)
    output_dir = tmp_path / "output"
    engine = TemplateEngine(tmp_path, config)
    result = assembler.assemble(config, output_dir, engine)
    return result, output_dir


class TestAssemble:
    def test_generates_five_files(
        self, assembled_skills: tuple,
    ) -> None:
        result, _ = assembled_skills
        assert len(result) == 5

    def test_all_returned_paths_exist(
        self, assembled_skills: tuple,
    ) -> None:
        result, _ = assembled_skills
        for path in result:
            assert path.exists(), f"Missing: {path}"

    def test_output_in_correct_directories(
        self, assembled_skills: tuple,
    ) -> None:
        result, _ = assembled_skills
        for path in result:
            assert "github/skills/" in str(path)
            assert path.name == "SKILL.md"

    def test_each_skill_has_own_directory(
        self, assembled_skills: tuple,
    ) -> None:
        result, _ = assembled_skills
        skill_dirs = {p.parent.name for p in result}
        assert skill_dirs == set(STORY_SKILLS)

    def test_creates_github_skills_directory_structure(
        self, assembled_skills: tuple,
    ) -> None:
        _, output_dir = assembled_skills
        skills_dir = output_dir / "github" / "skills"
        assert skills_dir.is_dir()


class TestGenerateGroup:
    def test_generates_skill_files(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        resources = _create_templates(tmp_path / "res")
        assembler = GithubSkillsAssembler(resources)
        engine = TemplateEngine(tmp_path, config)

        result = assembler._generate_group(
            engine, tmp_path / "output", "story", STORY_SKILLS,
        )

        assert len(result) == 5

    def test_missing_templates_dir_returns_empty(
        self, tmp_path: Path,
    ) -> None:
        config = _make_minimal_config()
        assembler = GithubSkillsAssembler(tmp_path / "nonexistent")
        engine = TemplateEngine(tmp_path, config)

        result = assembler._generate_group(
            engine, tmp_path / "output", "story", STORY_SKILLS,
        )

        assert result == []

    def test_missing_individual_template_skipped(
        self, tmp_path: Path,
    ) -> None:
        config = _make_minimal_config()
        resources = tmp_path / "res"
        tpl_dir = (
            resources / "github-skills-templates" / "story"
        )
        tpl_dir.mkdir(parents=True)
        (tpl_dir / "x-story-epic.md").write_text(
            "---\nname: x-story-epic\n"
            "description: >\n  Test.\n---\n\n# Epic\n",
            encoding="utf-8",
        )
        assembler = GithubSkillsAssembler(resources)
        engine = TemplateEngine(tmp_path, config)

        result = assembler._generate_group(
            engine, tmp_path / "output", "story", STORY_SKILLS,
        )

        assert len(result) == 1
        assert result[0].parent.name == "x-story-epic"


@pytest.mark.parametrize("skill_name", list(STORY_SKILLS))
class TestFrontmatterPerSkill:
    def test_starts_with_frontmatter(
        self,
        skill_name: str,
        assembled_skills: tuple,
    ) -> None:
        result, _ = assembled_skills
        path = _find_skill(result, skill_name)
        content = path.read_text(encoding="utf-8")
        assert content.startswith("---"), (
            f"{skill_name} missing frontmatter"
        )

    def test_contains_name_field(
        self,
        skill_name: str,
        assembled_skills: tuple,
    ) -> None:
        result, _ = assembled_skills
        path = _find_skill(result, skill_name)
        content = path.read_text(encoding="utf-8")
        assert f"name: {skill_name}" in content

    def test_contains_description_field(
        self,
        skill_name: str,
        assembled_skills: tuple,
    ) -> None:
        result, _ = assembled_skills
        path = _find_skill(result, skill_name)
        content = path.read_text(encoding="utf-8")
        assert "description:" in content


@pytest.mark.parametrize("skill_name", list(STORY_SKILLS))
class TestSkillContentPerSkill:
    def test_placeholders_replaced(
        self,
        skill_name: str,
        assembled_skills: tuple,
    ) -> None:
        result, _ = assembled_skills
        path = _find_skill(result, skill_name)
        content = path.read_text(encoding="utf-8")
        assert "{project_name}" not in content
        assert "{language_name}" not in content

    def test_project_name_present(
        self,
        skill_name: str,
        assembled_skills: tuple,
    ) -> None:
        result, _ = assembled_skills
        path = _find_skill(result, skill_name)
        content = path.read_text(encoding="utf-8")
        assert "my-service" in content

    def test_cross_reference_links_present(
        self,
        skill_name: str,
        assembled_skills: tuple,
    ) -> None:
        result, _ = assembled_skills
        path = _find_skill(result, skill_name)
        content = path.read_text(encoding="utf-8")
        assert ".claude/skills/" in content
        assert "github/skills/" not in content or "../../" in content


class TestSkillContentIntegration:
    def test_content_in_portuguese(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        resources = Path("resources")
        assembler = GithubSkillsAssembler(resources)
        output_dir = tmp_path / "output"
        engine = TemplateEngine(resources, config)

        result = assembler.assemble(config, output_dir, engine)

        portuguese_keywords = [
            "histórias",
            "Pré-requisitos",
            "especificação",
        ]
        for path in result:
            content = path.read_text(encoding="utf-8")
            found = any(
                kw in content for kw in portuguese_keywords
            )
            assert found, (
                f"{path.parent.name} lacks pt-BR content"
            )

    def test_no_reference_files_duplicated_in_github(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        resources = Path("resources")
        assembler = GithubSkillsAssembler(resources)
        output_dir = tmp_path / "output"
        engine = TemplateEngine(resources, config)

        assembler.assemble(config, output_dir, engine)

        skills_dir = output_dir / "github" / "skills"
        for skill_name in STORY_SKILLS:
            refs_dir = skills_dir / skill_name / "references"
            assert not refs_dir.exists(), (
                f"{skill_name} has duplicated references dir"
            )


def _find_skill(result: List[Path], skill_name: str) -> Path:
    """Find a skill file by name from assembler results."""
    for path in result:
        if path.parent.name == skill_name:
            return path
    raise AssertionError(f"Skill {skill_name} not found")
