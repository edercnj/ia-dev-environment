from __future__ import annotations

from pathlib import Path

from click.testing import CliRunner

from claude_setup.__main__ import main


class TestInitWithConfigFile:

    def test_init_valid_v3_exits_zero(self, valid_v3_path: Path) -> None:
        runner = CliRunner()
        result = runner.invoke(main, ["init", "-c", str(valid_v3_path)])
        assert result.exit_code == 0
        assert "test-cli-tool" in result.output

    def test_init_v2_type_exits_zero(self, valid_v2_type_path: Path) -> None:
        runner = CliRunner()
        result = runner.invoke(main, ["init", "-c", str(valid_v2_type_path)])
        assert result.exit_code == 0
        assert "legacy-service" in result.output

    def test_init_v2_stack_exits_zero(
        self,
        valid_v2_stack_path: Path,
    ) -> None:
        runner = CliRunner()
        result = runner.invoke(main, ["init", "-c", str(valid_v2_stack_path)])
        assert result.exit_code == 0
        assert "legacy-cli" in result.output

    def test_init_missing_section_exits_one(
        self,
        missing_language_path: Path,
    ) -> None:
        runner = CliRunner()
        result = runner.invoke(
            main,
            ["init", "-c", str(missing_language_path)],
        )
        assert result.exit_code == 1
        assert "language" in result.output.lower()

    def test_init_nonexistent_file_exits_two(self) -> None:
        runner = CliRunner()
        result = runner.invoke(main, ["init", "-c", "/no/such/file.yaml"])
        assert result.exit_code == 2

    def test_init_minimal_v3_exits_zero(
        self,
        minimal_v3_path: Path,
    ) -> None:
        runner = CliRunner()
        result = runner.invoke(main, ["init", "-c", str(minimal_v3_path)])
        assert result.exit_code == 0
        assert "minimal-tool" in result.output


class TestInitInteractive:

    def test_init_interactive_exits_zero(self) -> None:
        runner = CliRunner()
        input_text = "\n".join([
            "my-project",
            "A test project",
            "library",
            "n",
            "n",
            "cli",
            "python",
            "3.9",
            "click",
            "8.1",
            "pip",
        ]) + "\n"
        result = runner.invoke(main, ["init"], input=input_text)
        assert result.exit_code == 0
        assert "my-project" in result.output
