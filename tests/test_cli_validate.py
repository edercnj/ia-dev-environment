from __future__ import annotations

from pathlib import Path

from click.testing import CliRunner

from ia_dev_env.__main__ import main


class TestValidateCommand:

    def test_validate_help_exits_zero(self) -> None:
        runner = CliRunner()
        result = runner.invoke(main, ["validate", "--help"])
        assert result.exit_code == 0
        assert "--config" in result.output

    def test_validate_valid_config_exits_zero(
        self, valid_v3_path: Path,
    ) -> None:
        runner = CliRunner()
        result = runner.invoke(
            main,
            ["validate", "-c", str(valid_v3_path)],
        )
        assert result.exit_code == 0
        assert "valid" in result.output.lower()

    def test_validate_invalid_config_exits_one(
        self, missing_language_path: Path,
    ) -> None:
        runner = CliRunner()
        result = runner.invoke(
            main,
            ["validate", "-c", str(missing_language_path)],
        )
        assert result.exit_code == 1

    def test_validate_missing_file_exits_two(self) -> None:
        runner = CliRunner()
        result = runner.invoke(
            main,
            ["validate", "-c", "/no/file.yaml"],
        )
        assert result.exit_code == 2

    def test_validate_missing_config_option_exits_error(self) -> None:
        runner = CliRunner()
        result = runner.invoke(main, ["validate"])
        assert result.exit_code == 2

    def test_validate_verbose_exits_zero(
        self, valid_v3_path: Path,
    ) -> None:
        runner = CliRunner()
        result = runner.invoke(
            main,
            ["validate", "-c", str(valid_v3_path), "--verbose"],
        )
        assert result.exit_code == 0

    def test_validate_stack_errors_exits_one(self, tmp_path: Path) -> None:
        """Validate with a config that has invalid interface type."""
        config_file = tmp_path / "bad_iface.yaml"
        config_file.write_text(
            "project:\n"
            "  name: test\n"
            "  purpose: test\n"
            "architecture:\n"
            "  style: library\n"
            "interfaces:\n"
            "  - type: invalid-type\n"
            "language:\n"
            "  name: python\n"
            "  version: '3.9'\n"
            "framework:\n"
            "  name: click\n"
            "  version: '8.1'\n"
        )
        runner = CliRunner()
        result = runner.invoke(
            main,
            ["validate", "-c", str(config_file)],
        )
        assert result.exit_code == 1
