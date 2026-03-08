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
DEV_SKILLS = SKILL_GROUPS["dev"]
REVIEW_SKILLS = SKILL_GROUPS["review"]
ALL_SKILLS = tuple(
    name
    for group in SKILL_GROUPS.values()
    for name in group
)


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
    """Create template files for all skill groups."""
    for group, names in SKILL_GROUPS.items():
        tpl_dir = base / "github-skills-templates" / group
        tpl_dir.mkdir(parents=True)
        for name in names:
            (tpl_dir / f"{name}.md").write_text(
                f"---\nname: {name}\n"
                f"description: >\n"
                f"  Skill {name} for {{project_name}}.\n"
                f"---\n\n# {name}\n\n"
                f"Project: {{project_name}}\n"
                f"Language: {{language_name}}\n"
                f"Reference: `../../.claude/skills/"
                f"{name}/references/`\n",
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
    def test_generates_all_files(
        self, assembled_skills: tuple,
    ) -> None:
        result, _ = assembled_skills
        assert len(result) == len(ALL_SKILLS)

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
        assert skill_dirs == set(ALL_SKILLS)

    def test_creates_github_skills_directory_structure(
        self, assembled_skills: tuple,
    ) -> None:
        _, output_dir = assembled_skills
        skills_dir = output_dir / "github" / "skills"
        assert skills_dir.is_dir()


class TestGenerateGroup:
    def test_generates_story_skill_files(
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

    def test_generates_dev_skill_files(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        resources = _create_templates(tmp_path / "res")
        assembler = GithubSkillsAssembler(resources)
        engine = TemplateEngine(tmp_path, config)

        result = assembler._generate_group(
            engine, tmp_path / "output", "dev", DEV_SKILLS,
        )

        assert len(result) == 3

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


@pytest.mark.parametrize("skill_name", list(ALL_SKILLS))
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


@pytest.mark.parametrize("skill_name", list(ALL_SKILLS))
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
    def test_story_content_in_portuguese(
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
        story_results = [
            p for p in result
            if p.parent.name in STORY_SKILLS
        ]
        for path in story_results:
            content = path.read_text(encoding="utf-8")
            found = any(
                kw in content for kw in portuguese_keywords
            )
            assert found, (
                f"{path.parent.name} lacks pt-BR content"
            )

    def test_dev_content_in_english(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        resources = Path("resources")
        assembler = GithubSkillsAssembler(resources)
        output_dir = tmp_path / "output"
        engine = TemplateEngine(resources, config)

        result = assembler.assemble(config, output_dir, engine)

        english_keywords = [
            "implement",
            "feature",
            "layer",
        ]
        dev_results = [
            p for p in result
            if p.parent.name in DEV_SKILLS
        ]
        for path in dev_results:
            content = path.read_text(encoding="utf-8")
            found = any(
                kw in content for kw in english_keywords
            )
            assert found, (
                f"{path.parent.name} lacks English content"
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
        for skill_name in ALL_SKILLS:
            refs_dir = skills_dir / skill_name / "references"
            assert not refs_dir.exists(), (
                f"{skill_name} has duplicated references dir"
            )


@pytest.mark.parametrize("skill_name", list(DEV_SKILLS))
class TestDevSkillContent:
    def test_dev_skill_has_claude_skills_reference(
        self, tmp_path: Path, skill_name: str,
    ) -> None:
        config = _make_config()
        resources = Path("resources")
        assembler = GithubSkillsAssembler(resources)
        output_dir = tmp_path / "output"
        engine = TemplateEngine(resources, config)

        result = assembler.assemble(config, output_dir, engine)

        path = _find_skill(result, skill_name)
        content = path.read_text(encoding="utf-8")
        assert ".claude/skills/" in content

    def test_dev_skill_name_is_lowercase_hyphens(
        self, tmp_path: Path, skill_name: str,
    ) -> None:
        assert skill_name == skill_name.lower()
        assert " " not in skill_name


class TestDevSkillDescriptionKeywords:
    def test_x_dev_implement_description(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        resources = Path("resources")
        assembler = GithubSkillsAssembler(resources)
        output_dir = tmp_path / "output"
        engine = TemplateEngine(resources, config)

        result = assembler.assemble(config, output_dir, engine)

        path = _find_skill(result, "x-dev-implement")
        content = path.read_text(encoding="utf-8")
        assert "implement" in content.lower()
        assert "feature" in content.lower()

    def test_x_dev_lifecycle_description(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        resources = Path("resources")
        assembler = GithubSkillsAssembler(resources)
        output_dir = tmp_path / "output"
        engine = TemplateEngine(resources, config)

        result = assembler.assemble(config, output_dir, engine)

        path = _find_skill(result, "x-dev-lifecycle")
        content = path.read_text(encoding="utf-8")
        assert "lifecycle" in content.lower()

    def test_layer_templates_description(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        resources = Path("resources")
        assembler = GithubSkillsAssembler(resources)
        output_dir = tmp_path / "output"
        engine = TemplateEngine(resources, config)

        result = assembler.assemble(config, output_dir, engine)

        path = _find_skill(result, "layer-templates")
        content = path.read_text(encoding="utf-8")
        assert "layer" in content.lower()


class TestLayerTemplatesContent:
    def test_contains_domain_patterns(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        resources = Path("resources")
        assembler = GithubSkillsAssembler(resources)
        output_dir = tmp_path / "output"
        engine = TemplateEngine(resources, config)

        result = assembler.assemble(config, output_dir, engine)

        path = _find_skill(result, "layer-templates")
        content = path.read_text(encoding="utf-8")
        assert "domain" in content.lower()
        assert "port" in content.lower()
        assert "adapter" in content.lower()
        assert "application" in content.lower()


class TestGenerateReviewGroup:
    def test_generates_review_skill_files(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        resources = _create_templates(tmp_path / "res")
        assembler = GithubSkillsAssembler(resources)
        engine = TemplateEngine(tmp_path, config)

        result = assembler._generate_group(
            engine, tmp_path / "output",
            "review", REVIEW_SKILLS,
        )

        assert len(result) == 6


@pytest.mark.parametrize("skill_name", list(REVIEW_SKILLS))
class TestReviewSkillContent:
    def test_review_skill_has_claude_skills_reference(
        self, tmp_path: Path, skill_name: str,
    ) -> None:
        config = _make_config()
        resources = Path("resources")
        assembler = GithubSkillsAssembler(resources)
        output_dir = tmp_path / "output"
        engine = TemplateEngine(resources, config)

        result = assembler.assemble(config, output_dir, engine)

        path = _find_skill(result, skill_name)
        content = path.read_text(encoding="utf-8")
        assert ".claude/skills/" in content

    def test_review_skill_name_is_lowercase_hyphens(
        self, tmp_path: Path, skill_name: str,
    ) -> None:
        assert skill_name == skill_name.lower()
        assert " " not in skill_name

    def test_review_skill_content_in_english(
        self, tmp_path: Path, skill_name: str,
    ) -> None:
        config = _make_config()
        resources = Path("resources")
        assembler = GithubSkillsAssembler(resources)
        output_dir = tmp_path / "output"
        engine = TemplateEngine(resources, config)

        result = assembler.assemble(config, output_dir, engine)

        path = _find_skill(result, skill_name)
        content = path.read_text(encoding="utf-8")
        english_keywords = ["review", "checklist", "findings"]
        found = any(
            kw in content.lower()
            for kw in english_keywords
        )
        assert found, (
            f"{skill_name} lacks English review content"
        )


class TestReviewSkillDescriptionKeywords:
    def test_x_review_has_parallel_keyword(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        resources = Path("resources")
        assembler = GithubSkillsAssembler(resources)
        output_dir = tmp_path / "output"
        engine = TemplateEngine(resources, config)

        result = assembler.assemble(config, output_dir, engine)

        path = _find_skill(result, "x-review")
        content = path.read_text(encoding="utf-8")
        assert "parallel" in content.lower()
        assert "specialist" in content.lower()

    def test_x_review_api_has_rest_keywords(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        resources = Path("resources")
        assembler = GithubSkillsAssembler(resources)
        output_dir = tmp_path / "output"
        engine = TemplateEngine(resources, config)

        result = assembler.assemble(config, output_dir, engine)

        path = _find_skill(result, "x-review-api")
        content = path.read_text(encoding="utf-8")
        assert "rest" in content.lower()
        assert "rfc 7807" in content.lower()
        assert "openapi" in content.lower()

    def test_x_review_pr_has_tech_lead_keywords(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        resources = Path("resources")
        assembler = GithubSkillsAssembler(resources)
        output_dir = tmp_path / "output"
        engine = TemplateEngine(resources, config)

        result = assembler.assemble(config, output_dir, engine)

        path = _find_skill(result, "x-review-pr")
        content = path.read_text(encoding="utf-8")
        assert "tech lead" in content.lower()
        assert "40-point" in content.lower()
        assert "go/no-go" in content.lower()

    def test_x_review_grpc_has_grpc_keywords(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        resources = Path("resources")
        assembler = GithubSkillsAssembler(resources)
        output_dir = tmp_path / "output"
        engine = TemplateEngine(resources, config)

        result = assembler.assemble(config, output_dir, engine)

        path = _find_skill(result, "x-review-grpc")
        content = path.read_text(encoding="utf-8")
        assert "grpc" in content.lower()
        assert "proto3" in content.lower()
        assert "protobuf" in content.lower()

    def test_x_review_events_has_event_keywords(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        resources = Path("resources")
        assembler = GithubSkillsAssembler(resources)
        output_dir = tmp_path / "output"
        engine = TemplateEngine(resources, config)

        result = assembler.assemble(config, output_dir, engine)

        path = _find_skill(result, "x-review-events")
        content = path.read_text(encoding="utf-8")
        assert "event" in content.lower()
        assert "dead letter" in content.lower()
        assert "cloudevents" in content.lower()

    def test_x_review_gateway_has_gateway_keywords(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        resources = Path("resources")
        assembler = GithubSkillsAssembler(resources)
        output_dir = tmp_path / "output"
        engine = TemplateEngine(resources, config)

        result = assembler.assemble(config, output_dir, engine)

        path = _find_skill(result, "x-review-gateway")
        content = path.read_text(encoding="utf-8")
        assert "gateway" in content.lower()
        assert "routing" in content.lower()

    def test_no_keyword_overlap_between_api_and_grpc(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        resources = Path("resources")
        assembler = GithubSkillsAssembler(resources)
        output_dir = tmp_path / "output"
        engine = TemplateEngine(resources, config)

        result = assembler.assemble(config, output_dir, engine)

        api_path = _find_skill(result, "x-review-api")
        api_desc = _extract_description(api_path)
        grpc_path = _find_skill(result, "x-review-grpc")
        grpc_desc = _extract_description(grpc_path)

        assert "grpc" not in api_desc.lower()
        assert "proto3" not in api_desc.lower()
        assert "rest" not in grpc_desc.lower()
        assert "rfc 7807" not in grpc_desc.lower()


def _extract_description(path: Path) -> str:
    """Extract description field from frontmatter."""
    content = path.read_text(encoding="utf-8")
    in_desc = False
    lines = []
    for line in content.split("\n"):
        if line.startswith("description:"):
            in_desc = True
            rest = line[len("description:"):].strip()
            if rest and rest != ">":
                lines.append(rest)
        elif in_desc:
            if line.startswith("  "):
                lines.append(line.strip())
            else:
                break
    return " ".join(lines)


def _find_skill(result: List[Path], skill_name: str) -> Path:
    """Find a skill file by name from assembler results."""
    for path in result:
        if path.parent.name == skill_name:
            return path
    raise AssertionError(f"Skill {skill_name} not found")
