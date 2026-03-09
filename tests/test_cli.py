from __future__ import annotations

from click.testing import CliRunner

from ia_dev_env import __version__
from ia_dev_env.__main__ import main


class TestCli:

    def test_help_returns_zero(self) -> None:
        runner = CliRunner()
        result = runner.invoke(main, ["--help"])
        assert result.exit_code == 0
        assert "usage" in result.output.lower()

    def test_version_returns_version(self) -> None:
        runner = CliRunner()
        result = runner.invoke(main, ["--version"])
        assert result.exit_code == 0
        assert __version__ in result.output

    def test_no_args_shows_help(self) -> None:
        runner = CliRunner()
        result = runner.invoke(main, [])
        assert result.exit_code == 0
        assert "usage" in result.output.lower()

    def test_help_lists_generate_command(self) -> None:
        runner = CliRunner()
        result = runner.invoke(main, ["--help"])
        assert "generate" in result.output

    def test_help_lists_validate_command(self) -> None:
        runner = CliRunner()
        result = runner.invoke(main, ["--help"])
        assert "validate" in result.output
