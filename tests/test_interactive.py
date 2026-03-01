from __future__ import annotations

import pytest
from click.testing import CliRunner

from claude_setup.interactive import run_interactive
from claude_setup.models import ProjectConfig


def _build_interactive_input(
    name: str = "my-tool",
    purpose: str = "A test tool",
    arch: str = "library",
    domain_driven: str = "n",
    event_driven: str = "n",
    iface: str = "cli",
    language: str = "python",
    lang_version: str = "3.9",
    framework: str = "click",
    fw_version: str = "8.1",
    build_tool: str = "pip",
) -> str:
    lines = [
        name, purpose, arch, domain_driven, event_driven,
        iface, language, lang_version, framework, fw_version,
        build_tool,
    ]
    return "\n".join(lines) + "\n"


def _run_interactive_with_input(input_text: str) -> ProjectConfig:
    """Execute run_interactive with simulated stdin."""
    runner = CliRunner()
    result_holder = {}

    @runner.isolated_filesystem()
    def _invoke() -> None:
        import click

        @click.command()
        def _cmd() -> None:
            result_holder["config"] = run_interactive()

        runner_result = runner.invoke(_cmd, input=input_text)
        if runner_result.exception:
            raise runner_result.exception
    _invoke()
    return result_holder["config"]


class TestRunInteractive:

    def test_complete_input_returns_project_config(self) -> None:
        config = _run_interactive_with_input(_build_interactive_input())
        assert isinstance(config, ProjectConfig)

    def test_project_name_preserved(self) -> None:
        config = _run_interactive_with_input(
            _build_interactive_input(name="my-custom-tool"),
        )
        assert config.project.name == "my-custom-tool"

    def test_project_purpose_preserved(self) -> None:
        config = _run_interactive_with_input(
            _build_interactive_input(purpose="Custom purpose"),
        )
        assert config.project.purpose == "Custom purpose"

    def test_library_architecture_sets_style(self) -> None:
        config = _run_interactive_with_input(
            _build_interactive_input(arch="library"),
        )
        assert config.architecture.style == "library"

    def test_microservice_architecture_sets_style(self) -> None:
        config = _run_interactive_with_input(
            _build_interactive_input(arch="microservice"),
        )
        assert config.architecture.style == "microservice"

    def test_domain_driven_false_by_default(self) -> None:
        config = _run_interactive_with_input(
            _build_interactive_input(domain_driven="n"),
        )
        assert config.architecture.domain_driven is False

    def test_domain_driven_true_when_yes(self) -> None:
        config = _run_interactive_with_input(
            _build_interactive_input(domain_driven="y"),
        )
        assert config.architecture.domain_driven is True

    def test_python_language_correct(self) -> None:
        config = _run_interactive_with_input(
            _build_interactive_input(),
        )
        assert config.language.name == "python"
        assert config.language.version == "3.9"

    def test_cli_interface_selected(self) -> None:
        config = _run_interactive_with_input(
            _build_interactive_input(iface="cli"),
        )
        assert config.interfaces[0].type == "cli"

    def test_framework_correct(self) -> None:
        config = _run_interactive_with_input(
            _build_interactive_input(),
        )
        assert config.framework.name == "click"
        assert config.framework.version == "8.1"

    def test_java_quarkus_choices(self) -> None:
        config = _run_interactive_with_input(
            _build_interactive_input(
                language="java",
                lang_version="21",
                framework="quarkus",
                fw_version="3.17",
                build_tool="maven",
            ),
        )
        assert config.language.name == "java"
        assert config.framework.name == "quarkus"
