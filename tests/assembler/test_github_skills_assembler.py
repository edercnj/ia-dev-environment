from __future__ import annotations

import copy
from pathlib import Path
from typing import List

import pytest
import yaml

from ia_dev_env.assembler.github_skills_assembler import (
    SKILL_GROUPS,
    GithubSkillsAssembler,
)
from ia_dev_env.models import ProjectConfig
from ia_dev_env.template_engine import TemplateEngine

from tests.conftest import FULL_PROJECT_DICT, MINIMAL_PROJECT_DICT

STORY_SKILLS = SKILL_GROUPS["story"]
DEV_SKILLS = SKILL_GROUPS["dev"]
REVIEW_SKILLS = SKILL_GROUPS["review"]
TESTING_SKILLS = SKILL_GROUPS["testing"]
INFRA_SKILLS = SKILL_GROUPS["infrastructure"]
KNOWLEDGE_PACK_SKILLS = SKILL_GROUPS["knowledge-packs"]
GIT_TROUBLESHOOT_SKILLS = SKILL_GROUPS["git-troubleshooting"]
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
    @pytest.fixture
    def review_results(self, tmp_path: Path) -> List[Path]:
        config = _make_config()
        resources = Path("resources")
        assembler = GithubSkillsAssembler(resources)
        output_dir = tmp_path / "output"
        engine = TemplateEngine(resources, config)
        return assembler.assemble(config, output_dir, engine)

    def test_x_review_has_parallel_keyword(
        self, review_results: List[Path],
    ) -> None:
        path = _find_skill(review_results, "x-review")
        content = path.read_text(encoding="utf-8")
        assert "parallel" in content.lower()
        assert "specialist" in content.lower()

    def test_x_review_api_has_rest_keywords(
        self, review_results: List[Path],
    ) -> None:
        path = _find_skill(review_results, "x-review-api")
        content = path.read_text(encoding="utf-8")
        assert "rest" in content.lower()
        assert "rfc 7807" in content.lower()
        assert "openapi" in content.lower()

    def test_x_review_pr_has_tech_lead_keywords(
        self, review_results: List[Path],
    ) -> None:
        path = _find_skill(review_results, "x-review-pr")
        content = path.read_text(encoding="utf-8")
        assert "tech lead" in content.lower()
        assert "40-point" in content.lower()
        assert "go/no-go" in content.lower()

    def test_x_review_grpc_has_grpc_keywords(
        self, review_results: List[Path],
    ) -> None:
        path = _find_skill(review_results, "x-review-grpc")
        content = path.read_text(encoding="utf-8")
        assert "grpc" in content.lower()
        assert "proto3" in content.lower()
        assert "protobuf" in content.lower()

    def test_x_review_events_has_event_keywords(
        self, review_results: List[Path],
    ) -> None:
        path = _find_skill(review_results, "x-review-events")
        content = path.read_text(encoding="utf-8")
        assert "event" in content.lower()
        assert "dead letter" in content.lower()
        assert "cloudevents" in content.lower()

    def test_x_review_gateway_has_gateway_keywords(
        self, review_results: List[Path],
    ) -> None:
        path = _find_skill(review_results, "x-review-gateway")
        content = path.read_text(encoding="utf-8")
        assert "gateway" in content.lower()
        assert "routing" in content.lower()

    def test_no_keyword_overlap_between_api_and_grpc(
        self, review_results: List[Path],
    ) -> None:
        api_path = _find_skill(review_results, "x-review-api")
        api_desc = _extract_description(api_path)
        grpc_path = _find_skill(
            review_results, "x-review-grpc",
        )
        grpc_desc = _extract_description(grpc_path)

        assert "grpc" not in api_desc.lower()
        assert "proto3" not in api_desc.lower()
        assert "rest" not in grpc_desc.lower()
        assert "rfc 7807" not in grpc_desc.lower()


class TestGenerateTestingGroup:
    def test_generates_testing_skill_files(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        resources = _create_templates(tmp_path / "res")
        assembler = GithubSkillsAssembler(resources)
        engine = TemplateEngine(tmp_path, config)

        result = assembler._generate_group(
            engine, tmp_path / "output",
            "testing", TESTING_SKILLS,
        )

        assert len(result) == 6


@pytest.fixture
def testing_results(tmp_path: Path) -> List[Path]:
    """Fixture: assemble all skills, return results."""
    config = _make_config()
    resources = Path("resources")
    assembler = GithubSkillsAssembler(resources)
    output_dir = tmp_path / "output"
    engine = TemplateEngine(resources, config)
    return assembler.assemble(config, output_dir, engine)


@pytest.mark.parametrize(
    "skill_name", list(TESTING_SKILLS),
)
class TestTestingSkillContent:
    def test_testing_skill_has_claude_skills_reference(
        self, testing_results: List[Path],
        skill_name: str,
    ) -> None:
        path = _find_skill(testing_results, skill_name)
        content = path.read_text(encoding="utf-8")
        assert ".claude/skills/" in content

    def test_testing_skill_name_is_lowercase_hyphens(
        self, testing_results: List[Path],
        skill_name: str,
    ) -> None:
        assert skill_name == skill_name.lower()
        assert " " not in skill_name

    def test_testing_skill_content_in_english(
        self, testing_results: List[Path],
        skill_name: str,
    ) -> None:
        path = _find_skill(testing_results, skill_name)
        content = path.read_text(encoding="utf-8")
        english_keywords = [
            "test", "coverage", "checklist",
        ]
        found = any(
            kw in content.lower()
            for kw in english_keywords
        )
        assert found, (
            f"{skill_name} lacks English testing content"
        )


class TestTestingSkillDescriptionKeywords:

    def test_x_test_plan_has_plan_keywords(
        self, testing_results: List[Path],
    ) -> None:
        path = _find_skill(testing_results, "x-test-plan")
        content = path.read_text(encoding="utf-8")
        assert "test plan" in content.lower()
        assert "scenarios" in content.lower()
        assert "coverage" in content.lower()

    def test_x_test_run_has_coverage_keywords(
        self, testing_results: List[Path],
    ) -> None:
        path = _find_skill(testing_results, "x-test-run")
        content = path.read_text(encoding="utf-8")
        assert "coverage" in content.lower()
        assert "95%" in content
        assert "90%" in content

    def test_run_e2e_has_e2e_keywords(
        self, testing_results: List[Path],
    ) -> None:
        path = _find_skill(testing_results, "run-e2e")
        content = path.read_text(encoding="utf-8")
        assert "end-to-end" in content.lower()
        assert "database" in content.lower()
        assert "container" in content.lower()

    def test_run_smoke_api_has_smoke_keywords(
        self, testing_results: List[Path],
    ) -> None:
        path = _find_skill(
            testing_results, "run-smoke-api",
        )
        content = path.read_text(encoding="utf-8")
        assert "smoke" in content.lower()
        assert "newman" in content.lower()
        assert "health" in content.lower()

    def test_run_contract_tests_has_contract_keywords(
        self, testing_results: List[Path],
    ) -> None:
        path = _find_skill(
            testing_results, "run-contract-tests",
        )
        content = path.read_text(encoding="utf-8")
        assert "contract" in content.lower()
        assert "pact" in content.lower()
        assert "consumer" in content.lower()

    def test_run_perf_test_has_performance_keywords(
        self, testing_results: List[Path],
    ) -> None:
        path = _find_skill(
            testing_results, "run-perf-test",
        )
        content = path.read_text(encoding="utf-8")
        assert "performance" in content.lower()
        assert "latency" in content.lower()
        assert "throughput" in content.lower()

    def test_no_keyword_overlap_e2e_and_smoke(
        self, testing_results: List[Path],
    ) -> None:
        e2e_desc = _extract_description(
            _find_skill(testing_results, "run-e2e"),
        )
        smoke_desc = _extract_description(
            _find_skill(testing_results, "run-smoke-api"),
        )
        assert "newman" not in e2e_desc.lower()
        assert "postman" not in e2e_desc.lower()
        assert "end-to-end" not in smoke_desc.lower()


class TestGenerateInfrastructureGroup:
    def test_generates_infrastructure_skill_files(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        resources = _create_templates(tmp_path / "res")
        assembler = GithubSkillsAssembler(resources)
        engine = TemplateEngine(tmp_path, config)

        result = assembler._generate_group(
            engine, tmp_path / "output",
            "infrastructure", INFRA_SKILLS,
        )

        assert len(result) == 5

    def test_skips_inapplicable_infra_skills(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config(infrastructure={
            "container": "none",
            "orchestrator": "none",
            "templating": "none",
            "iac": "none",
        })
        resources = _create_templates(tmp_path / "res")
        assembler = GithubSkillsAssembler(resources)
        engine = TemplateEngine(tmp_path, config)
        output_dir = tmp_path / "output"

        result = assembler.assemble(
            config, output_dir, engine,
        )

        infra_paths = [
            p for p in result
            if p.parent.name in set(INFRA_SKILLS)
        ]
        assert infra_paths == []

    def test_generates_only_applicable_infra_skills(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config(infrastructure={
            "container": "docker",
            "orchestrator": "none",
            "templating": "none",
            "iac": "none",
        })
        resources = _create_templates(tmp_path / "res")
        assembler = GithubSkillsAssembler(resources)
        engine = TemplateEngine(tmp_path, config)
        output_dir = tmp_path / "output"

        result = assembler.assemble(
            config, output_dir, engine,
        )

        infra_names = {
            p.parent.name for p in result
            if p.parent.name in set(INFRA_SKILLS)
        }
        assert infra_names == {"dockerfile"}


def _assemble_real_templates(
    tmp_path: Path,
) -> List[Path]:
    """Assemble skills using real resource templates."""
    config = _make_config()
    resources = Path("resources")
    assembler = GithubSkillsAssembler(resources)
    output_dir = tmp_path / "output"
    engine = TemplateEngine(resources, config)
    return assembler.assemble(config, output_dir, engine)


@pytest.mark.parametrize("skill_name", list(INFRA_SKILLS))
class TestInfraSkillContent:
    @pytest.fixture
    def infra_content(
        self, tmp_path: Path, skill_name: str,
    ) -> str:
        result = _assemble_real_templates(tmp_path)
        path = _find_skill(result, skill_name)
        return path.read_text(encoding="utf-8")

    def test_infra_skill_has_claude_skills_reference(
        self, infra_content: str,
    ) -> None:
        assert ".claude/skills/" in infra_content

    def test_infra_skill_name_is_lowercase_hyphens(
        self, skill_name: str,
    ) -> None:
        assert skill_name == skill_name.lower()
        assert " " not in skill_name

    def test_infra_skill_content_in_english(
        self, infra_content: str, skill_name: str,
    ) -> None:
        english_keywords = [
            "checklist", "execution", "prerequisites",
        ]
        found = any(
            kw in infra_content.lower()
            for kw in english_keywords
        )
        assert found, (
            f"{skill_name} lacks English infra content"
        )

    def test_infra_skill_cloud_agnostic(
        self, infra_content: str, skill_name: str,
    ) -> None:
        cloud_specific = [
            "aws eks", "amazon eks",
            "google gke", "gke cluster",
            "azure aks", "aks cluster",
        ]
        for term in cloud_specific:
            assert term not in infra_content.lower(), (
                f"{skill_name} contains cloud-specific "
                f"reference: {term}"
            )


@pytest.mark.parametrize("skill_name", list(INFRA_SKILLS))
class TestInfraFrontmatterValid:
    def test_yaml_frontmatter_parseable(
        self, tmp_path: Path, skill_name: str,
    ) -> None:
        result = _assemble_real_templates(tmp_path)
        path = _find_skill(result, skill_name)
        content = path.read_text(encoding="utf-8")
        assert content.startswith("---")
        parts = content.split("---", 2)
        assert len(parts) >= 3, (
            f"{skill_name} missing frontmatter closing ---"
        )
        fm = yaml.safe_load(parts[1])
        assert isinstance(fm, dict)
        assert "name" in fm
        assert "description" in fm
        assert fm["name"] == skill_name


class TestInfraSkillDescriptionKeywords:
    @pytest.fixture
    def infra_results(self, tmp_path: Path) -> List[Path]:
        config = _make_config()
        resources = Path("resources")
        assembler = GithubSkillsAssembler(resources)
        output_dir = tmp_path / "output"
        engine = TemplateEngine(resources, config)
        return assembler.assemble(config, output_dir, engine)

    def test_setup_environment_has_setup_keywords(
        self, infra_results: List[Path],
    ) -> None:
        path = _find_skill(
            infra_results, "setup-environment",
        )
        content = path.read_text(encoding="utf-8")
        assert "environment" in content.lower()
        assert "health check" in content.lower()

    def test_k8s_deployment_has_k8s_keywords(
        self, infra_results: List[Path],
    ) -> None:
        path = _find_skill(
            infra_results, "k8s-deployment",
        )
        content = path.read_text(encoding="utf-8")
        assert "kubernetes" in content.lower()
        assert "deployment" in content.lower()
        assert "probe" in content.lower()

    def test_k8s_kustomize_has_kustomize_keywords(
        self, infra_results: List[Path],
    ) -> None:
        path = _find_skill(
            infra_results, "k8s-kustomize",
        )
        content = path.read_text(encoding="utf-8")
        assert "kustomize" in content.lower()
        assert "overlay" in content.lower()
        assert "patch" in content.lower()

    def test_dockerfile_has_docker_keywords(
        self, infra_results: List[Path],
    ) -> None:
        path = _find_skill(
            infra_results, "dockerfile",
        )
        content = path.read_text(encoding="utf-8")
        assert "multi-stage" in content.lower()
        assert "security" in content.lower()
        assert "healthcheck" in content.lower()

    def test_iac_terraform_has_terraform_keywords(
        self, infra_results: List[Path],
    ) -> None:
        path = _find_skill(
            infra_results, "iac-terraform",
        )
        content = path.read_text(encoding="utf-8")
        assert "terraform" in content.lower()
        assert "module" in content.lower()
        assert "remote state" in content.lower()


class TestGenerateKnowledgePacksGroup:
    def test_generates_knowledge_pack_skill_files(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        resources = _create_templates(tmp_path / "res")
        assembler = GithubSkillsAssembler(resources)
        engine = TemplateEngine(tmp_path, config)

        result = assembler._generate_group(
            engine, tmp_path / "output",
            "knowledge-packs", KNOWLEDGE_PACK_SKILLS,
        )

        assert len(result) == 9

    def test_knowledge_packs_always_generated(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config(infrastructure={
            "container": "none",
            "orchestrator": "none",
            "templating": "none",
            "iac": "none",
        })
        resources = _create_templates(tmp_path / "res")
        assembler = GithubSkillsAssembler(resources)
        engine = TemplateEngine(tmp_path, config)
        output_dir = tmp_path / "output"

        result = assembler.assemble(
            config, output_dir, engine,
        )

        kp_paths = [
            p for p in result
            if p.parent.name in set(KNOWLEDGE_PACK_SKILLS)
        ]
        assert len(kp_paths) == 9


@pytest.mark.parametrize(
    "skill_name", list(KNOWLEDGE_PACK_SKILLS),
)
class TestKnowledgePackContent:
    @pytest.fixture
    def kp_content(
        self, tmp_path: Path, skill_name: str,
    ) -> str:
        result = _assemble_real_templates(tmp_path)
        path = _find_skill(result, skill_name)
        return path.read_text(encoding="utf-8")

    def test_kp_has_claude_skills_reference(
        self, kp_content: str,
    ) -> None:
        assert ".claude/skills/" in kp_content

    def test_kp_name_is_lowercase_hyphens(
        self, skill_name: str,
    ) -> None:
        assert skill_name == skill_name.lower()
        assert " " not in skill_name

    def test_kp_content_in_english(
        self, kp_content: str, skill_name: str,
    ) -> None:
        english_keywords = [
            "summary", "knowledge pack", "references",
        ]
        found = any(
            kw in kp_content.lower()
            for kw in english_keywords
        )
        assert found, (
            f"{skill_name} lacks English KP content"
        )

    def test_kp_body_max_30_lines(
        self, kp_content: str, skill_name: str,
    ) -> None:
        parts = kp_content.split("---", 2)
        assert len(parts) >= 3
        body = parts[2].strip()
        body_lines = [
            ln for ln in body.split("\n")
            if ln.strip()
        ]
        assert len(body_lines) <= 30, (
            f"{skill_name} body has {len(body_lines)} "
            f"non-empty lines, max is 30"
        )


@pytest.mark.parametrize(
    "skill_name", list(KNOWLEDGE_PACK_SKILLS),
)
class TestKnowledgePackFrontmatterValid:
    def test_yaml_frontmatter_parseable(
        self, tmp_path: Path, skill_name: str,
    ) -> None:
        result = _assemble_real_templates(tmp_path)
        path = _find_skill(result, skill_name)
        content = path.read_text(encoding="utf-8")
        assert content.startswith("---")
        parts = content.split("---", 2)
        assert len(parts) >= 3, (
            f"{skill_name} missing frontmatter closing ---"
        )
        fm = yaml.safe_load(parts[1])
        assert isinstance(fm, dict)
        assert "name" in fm
        assert "description" in fm
        assert fm["name"] == skill_name


class TestKnowledgePackDescriptionKeywords:
    @pytest.fixture
    def kp_results(self, tmp_path: Path) -> List[Path]:
        config = _make_config()
        resources = Path("resources")
        assembler = GithubSkillsAssembler(resources)
        output_dir = tmp_path / "output"
        engine = TemplateEngine(resources, config)
        return assembler.assemble(config, output_dir, engine)

    def test_architecture_has_hexagonal_keywords(
        self, kp_results: List[Path],
    ) -> None:
        path = _find_skill(kp_results, "architecture")
        content = path.read_text(encoding="utf-8")
        assert "hexagonal" in content.lower()
        assert "dependency" in content.lower()
        assert "domain" in content.lower()

    def test_coding_standards_has_solid_keywords(
        self, kp_results: List[Path],
    ) -> None:
        path = _find_skill(
            kp_results, "coding-standards",
        )
        content = path.read_text(encoding="utf-8")
        assert "solid" in content.lower()
        assert "clean code" in content.lower()

    def test_patterns_has_cqrs_keywords(
        self, kp_results: List[Path],
    ) -> None:
        path = _find_skill(kp_results, "patterns")
        content = path.read_text(encoding="utf-8")
        assert "cqrs" in content.lower()
        assert "pattern" in content.lower()

    def test_protocols_has_grpc_websocket_keywords(
        self, kp_results: List[Path],
    ) -> None:
        path = _find_skill(kp_results, "protocols")
        content = path.read_text(encoding="utf-8")
        assert "grpc" in content.lower()
        assert "websocket" in content.lower()
        assert "event-driven" in content.lower()

    def test_observability_has_tracing_keywords(
        self, kp_results: List[Path],
    ) -> None:
        path = _find_skill(
            kp_results, "observability",
        )
        content = path.read_text(encoding="utf-8")
        assert "tracing" in content.lower()
        assert "metrics" in content.lower()
        assert "logging" in content.lower()

    def test_resilience_has_circuit_breaker_keywords(
        self, kp_results: List[Path],
    ) -> None:
        path = _find_skill(kp_results, "resilience")
        content = path.read_text(encoding="utf-8")
        assert "circuit breaker" in content.lower()
        assert "retry" in content.lower()
        assert "backpressure" in content.lower()

    def test_security_has_owasp_keywords(
        self, kp_results: List[Path],
    ) -> None:
        path = _find_skill(kp_results, "security")
        content = path.read_text(encoding="utf-8")
        assert "owasp" in content.lower()
        assert "top 10" in content.lower()

    def test_compliance_has_gdpr_keywords(
        self, kp_results: List[Path],
    ) -> None:
        path = _find_skill(kp_results, "compliance")
        content = path.read_text(encoding="utf-8")
        assert "gdpr" in content.lower()
        assert "lgpd" in content.lower()
        assert "pci-dss" in content.lower()

    def test_api_design_has_rest_pattern_keywords(
        self, kp_results: List[Path],
    ) -> None:
        path = _find_skill(kp_results, "api-design")
        content = path.read_text(encoding="utf-8")
        assert "status code" in content.lower()
        assert "rest" in content.lower()
        assert "pagination" in content.lower()
        assert "rfc 7807" in content.lower()

    def test_no_keyword_overlap_api_design_and_protocols(
        self, kp_results: List[Path],
    ) -> None:
        api_desc = _extract_description(
            _find_skill(kp_results, "api-design"),
        )
        proto_desc = _extract_description(
            _find_skill(kp_results, "protocols"),
        )
        assert "grpc" not in api_desc.lower()
        assert "websocket" not in api_desc.lower()
        assert "status code" not in proto_desc.lower()
        assert "pagination" not in proto_desc.lower()


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


class TestGenerateGitTroubleshootingGroup:
    def test_generates_git_troubleshooting_skill_files(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config()
        resources = _create_templates(tmp_path / "res")
        assembler = GithubSkillsAssembler(resources)
        engine = TemplateEngine(tmp_path, config)

        result = assembler._generate_group(
            engine, tmp_path / "output",
            "git-troubleshooting", GIT_TROUBLESHOOT_SKILLS,
        )

        assert len(result) == 2

    def test_git_troubleshoot_always_generated(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config(infrastructure={
            "container": "none",
            "orchestrator": "none",
            "templating": "none",
            "iac": "none",
        })
        resources = _create_templates(tmp_path / "res")
        assembler = GithubSkillsAssembler(resources)
        engine = TemplateEngine(tmp_path, config)
        output_dir = tmp_path / "output"

        result = assembler.assemble(
            config, output_dir, engine,
        )

        gt_paths = [
            p for p in result
            if p.parent.name in set(GIT_TROUBLESHOOT_SKILLS)
        ]
        assert len(gt_paths) == 2

    def test_missing_git_troubleshoot_dir_returns_empty(
        self, tmp_path: Path,
    ) -> None:
        config = _make_minimal_config()
        assembler = GithubSkillsAssembler(
            tmp_path / "nonexistent",
        )
        engine = TemplateEngine(tmp_path, config)

        result = assembler._generate_group(
            engine, tmp_path / "output",
            "git-troubleshooting", GIT_TROUBLESHOOT_SKILLS,
        )

        assert result == []

    def test_missing_single_template_skips_gracefully(
        self, tmp_path: Path,
    ) -> None:
        config = _make_minimal_config()
        resources = tmp_path / "res"
        tpl_dir = (
            resources
            / "github-skills-templates"
            / "git-troubleshooting"
        )
        tpl_dir.mkdir(parents=True)
        (tpl_dir / "x-git-push.md").write_text(
            "---\nname: x-git-push\n"
            "description: >\n  Git ops.\n---\n\n# Push\n",
            encoding="utf-8",
        )
        assembler = GithubSkillsAssembler(resources)
        engine = TemplateEngine(tmp_path, config)

        result = assembler._generate_group(
            engine, tmp_path / "output",
            "git-troubleshooting", GIT_TROUBLESHOOT_SKILLS,
        )

        assert len(result) == 1
        assert result[0].parent.name == "x-git-push"

    def test_git_troubleshoot_skills_in_all_skills(
        self,
    ) -> None:
        assert "x-git-push" in ALL_SKILLS
        assert "x-ops-troubleshoot" in ALL_SKILLS

    def test_git_troubleshoot_not_filtered_by_infra(
        self, tmp_path: Path,
    ) -> None:
        config = _make_config(infrastructure={
            "container": "none",
            "orchestrator": "none",
            "templating": "none",
            "iac": "none",
        })
        filtered = GithubSkillsAssembler._filter_skills(
            config,
            "git-troubleshooting",
            GIT_TROUBLESHOOT_SKILLS,
        )

        assert filtered == GIT_TROUBLESHOOT_SKILLS


@pytest.mark.parametrize(
    "skill_name", list(GIT_TROUBLESHOOT_SKILLS),
)
class TestGitTroubleshootContent:
    @pytest.fixture
    def gt_content(
        self, tmp_path: Path, skill_name: str,
    ) -> str:
        result = _assemble_real_templates(tmp_path)
        path = _find_skill(result, skill_name)
        return path.read_text(encoding="utf-8")

    def test_gt_has_claude_skills_reference(
        self, gt_content: str,
    ) -> None:
        assert ".claude/skills/" in gt_content

    def test_gt_name_is_lowercase_hyphens(
        self, skill_name: str,
    ) -> None:
        assert skill_name == skill_name.lower()
        assert " " not in skill_name

    def test_gt_content_in_english(
        self, gt_content: str, skill_name: str,
    ) -> None:
        english_keywords = [
            "workflow", "commit", "error", "debug",
        ]
        found = any(
            kw in gt_content.lower()
            for kw in english_keywords
        )
        assert found, (
            f"{skill_name} lacks English git/troubleshoot "
            f"content"
        )


@pytest.mark.parametrize(
    "skill_name", list(GIT_TROUBLESHOOT_SKILLS),
)
class TestGitTroubleshootFrontmatterValid:
    def test_yaml_frontmatter_parseable(
        self, tmp_path: Path, skill_name: str,
    ) -> None:
        result = _assemble_real_templates(tmp_path)
        path = _find_skill(result, skill_name)
        content = path.read_text(encoding="utf-8")
        assert content.startswith("---")
        parts = content.split("---", 2)
        assert len(parts) >= 3, (
            f"{skill_name} missing frontmatter closing ---"
        )
        fm = yaml.safe_load(parts[1])
        assert isinstance(fm, dict)
        assert "name" in fm
        assert "description" in fm
        assert fm["name"] == skill_name


class TestGitTroubleshootDescriptionKeywords:
    @pytest.fixture
    def gt_results(self, tmp_path: Path) -> List[Path]:
        return _assemble_real_templates(tmp_path)

    def test_x_git_push_has_git_keywords(
        self, gt_results: List[Path],
    ) -> None:
        path = _find_skill(gt_results, "x-git-push")
        content = path.read_text(encoding="utf-8")
        assert "git" in content.lower()
        assert "commit" in content.lower()
        assert "push" in content.lower()
        assert "branch" in content.lower()
        assert "pull request" in content.lower() or (
            "gh pr" in content.lower()
        )

    def test_x_git_push_has_conventional_commits(
        self, gt_results: List[Path],
    ) -> None:
        path = _find_skill(gt_results, "x-git-push")
        content = path.read_text(encoding="utf-8")
        assert "conventional commits" in content.lower()
        assert "type(scope)" in content.lower() or (
            "type" in content and "scope" in content
        )
        assert "feat" in content
        assert "fix" in content
        assert "chore" in content
        assert "refactor" in content
        assert "test" in content
        assert "docs" in content

    def test_x_ops_troubleshoot_has_debug_keywords(
        self, gt_results: List[Path],
    ) -> None:
        path = _find_skill(
            gt_results, "x-ops-troubleshoot",
        )
        content = path.read_text(encoding="utf-8")
        assert "error" in content.lower()
        assert "stacktrace" in content.lower()
        assert "debug" in content.lower()
        assert "diagnose" in content.lower() or (
            "diagnos" in content.lower()
        )

    def test_x_ops_troubleshoot_has_5_step_methodology(
        self, gt_results: List[Path],
    ) -> None:
        path = _find_skill(
            gt_results, "x-ops-troubleshoot",
        )
        content = path.read_text(encoding="utf-8")
        assert "reproduce" in content.lower()
        assert "locate" in content.lower()
        assert "understand" in content.lower()
        assert "fix" in content.lower()
        assert "verify" in content.lower()

    def test_no_keyword_overlap_git_and_troubleshoot(
        self, gt_results: List[Path],
    ) -> None:
        git_desc = _extract_description(
            _find_skill(gt_results, "x-git-push"),
        )
        troubleshoot_desc = _extract_description(
            _find_skill(
                gt_results, "x-ops-troubleshoot",
            ),
        )
        assert "error" not in git_desc.lower()
        assert "stacktrace" not in git_desc.lower()
        assert "commit" not in troubleshoot_desc.lower()
        assert "branch" not in troubleshoot_desc.lower()


def _find_skill(result: List[Path], skill_name: str) -> Path:
    """Find a skill file by name from assembler results."""
    for path in result:
        if path.parent.name == skill_name:
            return path
    raise AssertionError(f"Skill {skill_name} not found")
