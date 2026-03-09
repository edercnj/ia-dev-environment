from __future__ import annotations

from pathlib import Path
from unittest.mock import patch

from click.testing import CliRunner

from ia_dev_env.__main__ import main
from ia_dev_env.models import PipelineResult


def _success_result() -> PipelineResult:
    """Build a successful PipelineResult for mocking."""
    return PipelineResult(
        success=True,
        output_dir=Path("."),
        files_generated=[Path("rules/01.md")],
        warnings=[],
        duration_ms=42,
    )


class TestGenerateWithConfigFile:

    @patch("ia_dev_env.__main__.run_pipeline")
    @patch("ia_dev_env.__main__.find_resources_dir")
    def test_generate_valid_v3_exits_zero(
        self, mock_find, mock_pipeline, valid_v3_path: Path,
    ) -> None:
        mock_find.return_value = Path("src")
        mock_pipeline.return_value = _success_result()
        runner = CliRunner()
        result = runner.invoke(main, ["generate", "-c", str(valid_v3_path)])
        assert result.exit_code == 0

    @patch("ia_dev_env.__main__.run_pipeline")
    @patch("ia_dev_env.__main__.find_resources_dir")
    def test_generate_v2_type_exits_zero(
        self, mock_find, mock_pipeline, valid_v2_type_path: Path,
    ) -> None:
        mock_find.return_value = Path("src")
        mock_pipeline.return_value = _success_result()
        runner = CliRunner()
        result = runner.invoke(
            main, ["generate", "-c", str(valid_v2_type_path)],
        )
        assert result.exit_code == 0

    @patch("ia_dev_env.__main__.run_pipeline")
    @patch("ia_dev_env.__main__.find_resources_dir")
    def test_generate_v2_stack_exits_zero(
        self, mock_find, mock_pipeline, valid_v2_stack_path: Path,
    ) -> None:
        mock_find.return_value = Path("src")
        mock_pipeline.return_value = _success_result()
        runner = CliRunner()
        result = runner.invoke(
            main, ["generate", "-c", str(valid_v2_stack_path)],
        )
        assert result.exit_code == 0

    def test_generate_missing_section_exits_one(
        self, missing_language_path: Path,
    ) -> None:
        runner = CliRunner()
        result = runner.invoke(
            main,
            ["generate", "-c", str(missing_language_path)],
        )
        assert result.exit_code == 1
        assert "language" in result.output.lower()

    def test_generate_nonexistent_file_exits_two(self) -> None:
        runner = CliRunner()
        result = runner.invoke(
            main, ["generate", "-c", "/no/such/file.yaml"],
        )
        assert result.exit_code == 2

    @patch("ia_dev_env.__main__.run_pipeline")
    @patch("ia_dev_env.__main__.find_resources_dir")
    def test_generate_minimal_v3_exits_zero(
        self, mock_find, mock_pipeline, minimal_v3_path: Path,
    ) -> None:
        mock_find.return_value = Path("src")
        mock_pipeline.return_value = _success_result()
        runner = CliRunner()
        result = runner.invoke(
            main, ["generate", "-c", str(minimal_v3_path)],
        )
        assert result.exit_code == 0


class TestGenerateInteractive:

    @patch("ia_dev_env.__main__.run_pipeline")
    @patch("ia_dev_env.__main__.find_resources_dir")
    @patch("ia_dev_env.__main__.run_interactive")
    def test_generate_interactive_exits_zero(
        self, mock_interactive, mock_find, mock_pipeline,
    ) -> None:
        from ia_dev_env.models import ProjectConfig
        mock_interactive.return_value = ProjectConfig.from_dict({
            "project": {"name": "my-project", "purpose": "A test project"},
            "architecture": {"style": "library"},
            "interfaces": [{"type": "cli"}],
            "language": {"name": "python", "version": "3.9"},
            "framework": {"name": "click", "version": "8.1"},
        })
        mock_find.return_value = Path("src")
        mock_pipeline.return_value = _success_result()
        runner = CliRunner()
        result = runner.invoke(main, ["generate", "--interactive"])
        assert result.exit_code == 0
