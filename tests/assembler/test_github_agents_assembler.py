from __future__ import annotations

import copy
import logging
from pathlib import Path
from typing import List

import pytest
import yaml

from claude_setup.assembler.github_agents_assembler import (
    GithubAgentsAssembler,
)
from claude_setup.models import ProjectConfig
from claude_setup.template_engine import TemplateEngine

from tests.conftest import FULL_PROJECT_DICT, MINIMAL_PROJECT_DICT

CORE_AGENTS = (
    "architect",
    "performance-engineer",
    "product-owner",
    "qa-engineer",
    "security-engineer",
    "tech-lead",
)

CONDITIONAL_AGENTS = (
    "api-engineer",
    "devops-engineer",
    "event-engineer",
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


def _create_core_templates(resources: Path) -> None:
    """Create core agent templates."""
    core_dir = resources / "github-agents-templates" / "core"
    core_dir.mkdir(parents=True, exist_ok=True)
    for name in CORE_AGENTS:
        content = (
            f"---\nname: {name}\n"
            f"description: >\n  {name} agent\n"
            f"tools:\n  - read_file\n"
            f"disallowed-tools:\n  - deploy\n"
            f"---\n\n# {name}\n"
            f"Uses {{{{FRAMEWORK}}}}\n"
        )
        (core_dir / f"{name}.md").write_text(
            content, encoding="utf-8",
        )


def _create_conditional_templates(resources: Path) -> None:
    """Create conditional agent templates."""
    cond_dir = (
        resources / "github-agents-templates" / "conditional"
    )
    cond_dir.mkdir(parents=True, exist_ok=True)
    for name in CONDITIONAL_AGENTS:
        content = (
            f"---\nname: {name}\n"
            f"description: >\n  {name} agent\n"
            f"tools:\n  - read_file\n"
            f"disallowed-tools:\n  - deploy\n"
            f"---\n\n# {name}\n"
        )
        (cond_dir / f"{name}.md").write_text(
            content, encoding="utf-8",
        )


def _create_developer_templates(
    resources: Path,
    languages: tuple = ("python", "java", "go"),
) -> None:
    """Create developer agent templates."""
    dev_dir = (
        resources / "github-agents-templates" / "developers"
    )
    dev_dir.mkdir(parents=True, exist_ok=True)
    for lang in languages:
        content = (
            f"---\nname: {lang}-developer\n"
            f"description: >\n  {lang} developer agent\n"
            f"tools:\n  - edit_file\n"
            f"disallowed-tools:\n  - deploy\n"
            f"---\n\n# {lang} Developer\n"
            f"Uses {{{{FRAMEWORK}}}}\n"
        )
        (dev_dir / f"{lang}-developer.md").write_text(
            content, encoding="utf-8",
        )


def _create_all_templates(resources: Path) -> None:
    """Create all template files for testing."""
    _create_core_templates(resources)
    _create_conditional_templates(resources)
    _create_developer_templates(resources)


def _output_agent_names(output_dir: Path) -> List[str]:
    """Return sorted list of generated agent filenames."""
    agents_dir = output_dir / "github" / "agents"
    if not agents_dir.exists():
        return []
    return sorted(f.name for f in agents_dir.iterdir())


@pytest.fixture
def assembled_full(tmp_path: Path):
    """Assemble agents with FULL config, return (result, output)."""
    resources = tmp_path / "res"
    output = tmp_path / "out"
    _create_all_templates(resources)
    config = _make_config()
    engine = TemplateEngine(resources, config)
    assembler = GithubAgentsAssembler(resources)
    result = assembler.assemble(config, output, engine)
    return result, output


@pytest.fixture
def assembled_minimal(tmp_path: Path):
    """Assemble agents with MINIMAL config, return (result, output)."""
    resources = tmp_path / "res"
    output = tmp_path / "out"
    _create_all_templates(resources)
    config = _make_minimal_config()
    engine = TemplateEngine(resources, config)
    assembler = GithubAgentsAssembler(resources)
    result = assembler.assemble(config, output, engine)
    return result, output


class TestAssembleCore:

    def test_generates_all_core_agents(
        self, assembled_full,
    ) -> None:
        result, output = assembled_full
        for name in CORE_AGENTS:
            expected = (
                output / "github" / "agents"
                / f"{name}.agent.md"
            )
            assert expected.exists(), (
                f"{name}.agent.md not generated"
            )

    @pytest.mark.parametrize("agent_name", CORE_AGENTS)
    def test_core_agent_has_agent_md_extension(
        self, agent_name: str, assembled_full,
    ) -> None:
        _, output = assembled_full
        agent_path = (
            output / "github" / "agents"
            / f"{agent_name}.agent.md"
        )
        assert agent_path.exists()
        assert agent_path.name.endswith(".agent.md")

    def test_replaces_single_brace_placeholders(
        self, tmp_path: Path,
    ) -> None:
        resources = tmp_path / "res"
        output = tmp_path / "out"
        core_dir = (
            resources / "github-agents-templates" / "core"
        )
        core_dir.mkdir(parents=True, exist_ok=True)
        (core_dir / "test-agent.md").write_text(
            "---\nname: test\ntools:\n  - read_file\n"
            "disallowed-tools:\n  - deploy\n"
            "---\n\nUses {framework_name}\n",
            encoding="utf-8",
        )
        config = _make_minimal_config()
        engine = TemplateEngine(resources, config)
        assembler = GithubAgentsAssembler(resources)
        assembler.assemble(config, output, engine)
        agent = (
            output / "github" / "agents"
            / "test-agent.agent.md"
        )
        content = agent.read_text(encoding="utf-8")
        assert "{framework_name}" not in content
        assert config.framework.name in content


CONDITIONAL_PRESENT_CASES = [
    pytest.param(
        _make_config,
        "devops-engineer",
        {},
        id="devops-present-full-config",
    ),
    pytest.param(
        _make_config,
        "api-engineer",
        {},
        id="api-present-full-config",
    ),
    pytest.param(
        _make_config,
        "event-engineer",
        {},
        id="event-present-full-config",
    ),
]

CONDITIONAL_ABSENT_CASES = [
    pytest.param(
        _make_minimal_config,
        "devops-engineer",
        {"infrastructure": {
            "container": "none",
            "orchestrator": "none",
        }},
        id="devops-absent-no-infra",
    ),
    pytest.param(
        _make_minimal_config,
        "api-engineer",
        {},
        id="api-absent-cli-only",
    ),
    pytest.param(
        _make_minimal_config,
        "event-engineer",
        {},
        id="event-absent-no-events",
    ),
]


class TestAssembleConditional:

    @pytest.mark.parametrize(
        "config_fn,agent_name,overrides",
        CONDITIONAL_PRESENT_CASES,
    )
    def test_conditional_present(
        self, config_fn, agent_name, overrides,
        tmp_path: Path,
    ) -> None:
        resources = tmp_path / "res"
        output = tmp_path / "out"
        _create_all_templates(resources)
        config = config_fn(**overrides)
        engine = TemplateEngine(resources, config)
        assembler = GithubAgentsAssembler(resources)
        assembler.assemble(config, output, engine)
        assert (
            output / "github" / "agents"
            / f"{agent_name}.agent.md"
        ).exists(), f"{agent_name} should be generated"

    @pytest.mark.parametrize(
        "config_fn,agent_name,overrides",
        CONDITIONAL_ABSENT_CASES,
    )
    def test_conditional_absent(
        self, config_fn, agent_name, overrides,
        tmp_path: Path,
    ) -> None:
        resources = tmp_path / "res"
        output = tmp_path / "out"
        _create_all_templates(resources)
        config = config_fn(**overrides)
        engine = TemplateEngine(resources, config)
        assembler = GithubAgentsAssembler(resources)
        assembler.assemble(config, output, engine)
        assert not (
            output / "github" / "agents"
            / f"{agent_name}.agent.md"
        ).exists(), f"{agent_name} should not be generated"


class TestAssembleDeveloper:

    def test_generates_language_developer(
        self, assembled_minimal,
    ) -> None:
        _, output = assembled_minimal
        assert (
            output / "github" / "agents"
            / "python-developer.agent.md"
        ).exists()

    def test_no_developer_when_template_missing(
        self, tmp_path: Path,
    ) -> None:
        resources = tmp_path / "res"
        output = tmp_path / "out"
        _create_core_templates(resources)
        config = _make_minimal_config()
        engine = TemplateEngine(resources, config)
        assembler = GithubAgentsAssembler(resources)
        result = assembler.assemble(config, output, engine)
        dev_agents = [
            p for p in result
            if "developer" in p.name
        ]
        assert dev_agents == []


ALL_AGENTS_FULL = list(CORE_AGENTS) + list(CONDITIONAL_AGENTS)


class TestYamlFrontmatter:

    @pytest.mark.parametrize("agent_name", ALL_AGENTS_FULL)
    def test_frontmatter_has_required_fields(
        self, agent_name: str, assembled_full,
    ) -> None:
        _, output = assembled_full
        agent_file = (
            output / "github" / "agents"
            / f"{agent_name}.agent.md"
        )
        content = agent_file.read_text(encoding="utf-8")
        assert content.startswith("---"), (
            f"{agent_name} missing YAML frontmatter"
        )
        parts = content.split("---", 2)
        assert len(parts) >= 3, (
            f"{agent_name} malformed frontmatter"
        )
        fm = yaml.safe_load(parts[1])
        assert isinstance(fm["name"], str), (
            f"{agent_name}: 'name' must be a string"
        )
        assert len(fm["name"]) > 0, (
            f"{agent_name}: 'name' must be non-empty"
        )
        assert isinstance(fm["tools"], list), (
            f"{agent_name}: 'tools' must be a list"
        )
        assert len(fm["tools"]) > 0, (
            f"{agent_name}: 'tools' must not be empty"
        )
        assert isinstance(fm["disallowed-tools"], list), (
            f"{agent_name}: 'disallowed-tools' must be a list"
        )
        assert len(fm["disallowed-tools"]) > 0, (
            f"{agent_name}: 'disallowed-tools' must not be empty"
        )

    def test_developer_frontmatter_valid(
        self, assembled_minimal,
    ) -> None:
        _, output = assembled_minimal
        agent_file = (
            output / "github" / "agents"
            / "python-developer.agent.md"
        )
        content = agent_file.read_text(encoding="utf-8")
        parts = content.split("---", 2)
        fm = yaml.safe_load(parts[1])
        assert fm["name"] == "python-developer"
        assert isinstance(fm["tools"], list)
        assert isinstance(fm["disallowed-tools"], list)


class TestFullPipeline:

    def test_full_config_generates_all_agents(
        self, assembled_full,
    ) -> None:
        result, _ = assembled_full
        expected_count = (
            len(CORE_AGENTS)
            + 3  # devops, api, event (all active in FULL)
            + 1  # python developer
        )
        assert len(result) == expected_count

    def test_minimal_config_generates_core_dev_devops(
        self, assembled_minimal,
    ) -> None:
        result, _ = assembled_minimal
        # core + developer + devops (container defaults to docker)
        expected_count = len(CORE_AGENTS) + 1 + 1
        assert len(result) == expected_count

    def test_returns_list_of_paths(
        self, assembled_minimal,
    ) -> None:
        result, _ = assembled_minimal
        assert isinstance(result, list)
        for path in result:
            assert isinstance(path, Path)
            assert path.exists()


class TestEdgeCases:

    def test_missing_templates_dir_returns_empty(
        self, tmp_path: Path,
    ) -> None:
        resources = tmp_path / "empty-res"
        resources.mkdir()
        output = tmp_path / "out"
        config = _make_minimal_config()
        engine = TemplateEngine(resources, config)
        assembler = GithubAgentsAssembler(resources)
        result = assembler.assemble(config, output, engine)
        assert result == []

    def test_empty_core_dir_returns_empty(
        self, tmp_path: Path,
    ) -> None:
        resources = tmp_path / "res"
        core = (
            resources / "github-agents-templates" / "core"
        )
        core.mkdir(parents=True)
        output = tmp_path / "out"
        config = _make_minimal_config()
        engine = TemplateEngine(resources, config)
        assembler = GithubAgentsAssembler(resources)
        result = assembler.assemble(config, output, engine)
        assert result == []


class TestWarningPaths:

    def test_missing_core_dir_logs_warning(
        self, tmp_path: Path, caplog,
    ) -> None:
        resources = tmp_path / "res"
        resources.mkdir()
        output = tmp_path / "out"
        config = _make_minimal_config()
        engine = TemplateEngine(resources, config)
        assembler = GithubAgentsAssembler(resources)
        with caplog.at_level(logging.WARNING):
            assembler.assemble(config, output, engine)
        assert any(
            "Core templates dir not found" in msg
            for msg in caplog.messages
        )

    def test_missing_conditional_template_logs_warning(
        self, tmp_path: Path, caplog,
    ) -> None:
        resources = tmp_path / "res"
        output = tmp_path / "out"
        _create_core_templates(resources)
        cond_dir = (
            resources
            / "github-agents-templates"
            / "conditional"
        )
        cond_dir.mkdir(parents=True, exist_ok=True)
        # Create only devops, not api or event
        (cond_dir / "devops-engineer.md").write_text(
            "---\nname: devops\ntools:\n  - x\n"
            "disallowed-tools:\n  - y\n---\n",
            encoding="utf-8",
        )
        config = _make_config()
        engine = TemplateEngine(resources, config)
        assembler = GithubAgentsAssembler(resources)
        with caplog.at_level(logging.WARNING):
            assembler.assemble(config, output, engine)
        assert any(
            "Conditional template not found" in msg
            for msg in caplog.messages
        )

    def test_missing_developer_template_logs_warning(
        self, tmp_path: Path, caplog,
    ) -> None:
        resources = tmp_path / "res"
        output = tmp_path / "out"
        _create_core_templates(resources)
        dev_dir = (
            resources
            / "github-agents-templates"
            / "developers"
        )
        dev_dir.mkdir(parents=True, exist_ok=True)
        # No python-developer.md created
        config = _make_minimal_config()
        engine = TemplateEngine(resources, config)
        assembler = GithubAgentsAssembler(resources)
        with caplog.at_level(logging.WARNING):
            assembler.assemble(config, output, engine)
        assert any(
            "Developer template not found" in msg
            for msg in caplog.messages
        )
