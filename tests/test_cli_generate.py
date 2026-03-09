from __future__ import annotations

from pathlib import Path
from unittest.mock import patch

import pytest
from click.testing import CliRunner

from ia_dev_env.__main__ import _classify_files, main
from ia_dev_env.exceptions import PipelineError
from ia_dev_env.models import PipelineResult


def _success_result(output_dir: Path = Path(".")) -> PipelineResult:
    """Build a successful PipelineResult for mocking."""
    return PipelineResult(
        success=True,
        output_dir=output_dir,
        files_generated=[
            Path("rules/01.md"),
            Path("rules/02.md"),
            Path("skills/commit/SKILL.md"),
            Path("agents/dev.md"),
            Path("hooks/post-compile.sh"),
            Path("settings.json"),
            Path("README.md"),
        ],
        warnings=[],
        duration_ms=42,
    )


def _dry_run_result(output_dir: Path = Path(".")) -> PipelineResult:
    """Build a dry-run PipelineResult for mocking."""
    return PipelineResult(
        success=True,
        output_dir=output_dir,
        files_generated=[Path("rules/01.md")],
        warnings=["Dry run -- no files written"],
        duration_ms=10,
    )


class TestGenerateCommand:

    def test_generate_help_exits_zero(self) -> None:
        runner = CliRunner()
        result = runner.invoke(main, ["generate", "--help"])
        assert result.exit_code == 0
        assert "--config" in result.output
        assert "--interactive" in result.output
        assert "--dry-run" in result.output

    @patch("ia_dev_env.__main__.run_pipeline")
    @patch("ia_dev_env.__main__.find_resources_dir")
    def test_generate_config_valid_exits_zero(
        self, mock_find, mock_pipeline, valid_v3_path: Path,
    ) -> None:
        mock_find.return_value = Path("src")
        mock_pipeline.return_value = _success_result()
        runner = CliRunner()
        result = runner.invoke(main, ["generate", "-c", str(valid_v3_path)])
        assert result.exit_code == 0
        assert "Success" in result.output

    def test_generate_config_missing_file_exits_two(self) -> None:
        runner = CliRunner()
        result = runner.invoke(main, ["generate", "-c", "/no/such/file.yaml"])
        assert result.exit_code == 2

    @patch("ia_dev_env.__main__.run_pipeline")
    @patch("ia_dev_env.__main__.find_resources_dir")
    @patch("ia_dev_env.__main__.run_interactive")
    def test_generate_interactive_exits_zero(
        self, mock_interactive, mock_find, mock_pipeline,
    ) -> None:
        from ia_dev_env.models import ProjectConfig
        mock_interactive.return_value = ProjectConfig.from_dict({
            "project": {"name": "test", "purpose": "test"},
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

    def test_generate_both_config_and_interactive_exits_error(
        self, valid_v3_path: Path,
    ) -> None:
        runner = CliRunner()
        result = runner.invoke(
            main,
            ["generate", "-c", str(valid_v3_path), "--interactive"],
        )
        assert result.exit_code != 0
        assert "mutually exclusive" in result.output.lower()

    def test_generate_neither_config_nor_interactive_exits_error(self) -> None:
        runner = CliRunner()
        result = runner.invoke(main, ["generate"])
        assert result.exit_code != 0
        assert "required" in result.output.lower()

    @patch("ia_dev_env.__main__.run_pipeline")
    @patch("ia_dev_env.__main__.find_resources_dir")
    def test_generate_dry_run_shows_plan(
        self, mock_find, mock_pipeline, valid_v3_path: Path,
    ) -> None:
        mock_find.return_value = Path("src")
        mock_pipeline.return_value = _dry_run_result()
        runner = CliRunner()
        result = runner.invoke(
            main,
            ["generate", "-c", str(valid_v3_path), "--dry-run"],
        )
        assert result.exit_code == 0
        assert "dry run" in result.output.lower()

    @patch("ia_dev_env.__main__.run_pipeline")
    @patch("ia_dev_env.__main__.find_resources_dir")
    def test_generate_verbose_enables_logging(
        self, mock_find, mock_pipeline, valid_v3_path: Path,
    ) -> None:
        mock_find.return_value = Path("src")
        mock_pipeline.return_value = _success_result()
        runner = CliRunner()
        result = runner.invoke(
            main,
            ["generate", "-c", str(valid_v3_path), "--verbose"],
        )
        assert result.exit_code == 0

    @patch("ia_dev_env.__main__.run_pipeline")
    @patch("ia_dev_env.__main__.find_resources_dir")
    def test_generate_output_dir_option(
        self, mock_find, mock_pipeline, valid_v3_path: Path, tmp_path: Path,
    ) -> None:
        mock_find.return_value = Path("src")
        mock_pipeline.return_value = _success_result(tmp_path / "custom")
        runner = CliRunner()
        result = runner.invoke(
            main,
            [
                "generate", "-c", str(valid_v3_path),
                "--output-dir", str(tmp_path / "custom"),
            ],
        )
        assert result.exit_code == 0

    @patch("ia_dev_env.__main__.find_resources_dir")
    def test_generate_pipeline_error_exits_one(
        self, mock_find, valid_v3_path: Path,
    ) -> None:
        mock_find.return_value = Path("src")
        with patch("ia_dev_env.__main__.run_pipeline") as mock_pipeline:
            mock_pipeline.side_effect = PipelineError("Rules", "fail")
            runner = CliRunner()
            result = runner.invoke(
                main,
                ["generate", "-c", str(valid_v3_path)],
            )
            assert result.exit_code == 1

    @patch("ia_dev_env.__main__.run_pipeline")
    def test_generate_resources_dir_option(
        self, mock_pipeline, valid_v3_path: Path, tmp_path: Path,
    ) -> None:
        resources = tmp_path / "my-resources"
        resources.mkdir()
        mock_pipeline.return_value = _success_result()
        runner = CliRunner()
        result = runner.invoke(
            main,
            ["generate", "-c", str(valid_v3_path), "--resources-dir", str(resources)],
        )
        assert result.exit_code == 0

    @patch("ia_dev_env.__main__.find_resources_dir")
    def test_generate_find_resources_dir_failure_exits_one(
        self, mock_find, valid_v3_path: Path,
    ) -> None:
        mock_find.side_effect = FileNotFoundError("not found")
        runner = CliRunner()
        result = runner.invoke(
            main,
            ["generate", "-c", str(valid_v3_path)],
        )
        assert result.exit_code == 1

    @patch("ia_dev_env.__main__.run_pipeline")
    @patch("ia_dev_env.__main__.find_resources_dir")
    def test_generate_failed_result_exits_one(
        self, mock_find, mock_pipeline, valid_v3_path: Path,
    ) -> None:
        mock_find.return_value = Path("src")
        mock_pipeline.return_value = PipelineResult(
            success=False,
            output_dir=Path("."),
            files_generated=[],
            warnings=["something went wrong"],
            duration_ms=10,
        )
        runner = CliRunner()
        result = runner.invoke(
            main,
            ["generate", "-c", str(valid_v3_path)],
        )
        assert result.exit_code == 1


class TestClassifyFiles:

    def test_classify_rules(self) -> None:
        files = [Path("rules/01.md"), Path("rules/02.md")]
        counts = _classify_files(files)
        assert counts["Rules"] == 2

    def test_classify_skills(self) -> None:
        files = [Path("skills/commit/SKILL.md")]
        counts = _classify_files(files)
        assert counts["Skills"] == 1

    def test_classify_agents(self) -> None:
        files = [Path("agents/dev.md")]
        counts = _classify_files(files)
        assert counts["Agents"] == 1

    def test_classify_hooks(self) -> None:
        files = [Path("hooks/post-compile.sh")]
        counts = _classify_files(files)
        assert counts["Hooks"] == 1

    def test_classify_settings(self) -> None:
        files = [Path("settings.json"), Path("settings.local.json")]
        counts = _classify_files(files)
        assert counts["Settings"] == 2

    def test_classify_readme(self) -> None:
        files = [Path("README.md")]
        counts = _classify_files(files)
        assert counts["README"] == 1

    def test_classify_mixed_files(self) -> None:
        files = [
            Path("rules/01.md"),
            Path("skills/commit/SKILL.md"),
            Path("agents/dev.md"),
            Path("hooks/post-compile.sh"),
            Path("settings.json"),
            Path("README.md"),
        ]
        counts = _classify_files(files)
        assert counts["Rules"] == 1
        assert counts["Skills"] == 1
        assert counts["Agents"] == 1
        assert counts["Hooks"] == 1
        assert counts["Settings"] == 1
        assert counts["README"] == 1


class TestDisplayResult:

    @patch("ia_dev_env.__main__.run_pipeline")
    @patch("ia_dev_env.__main__.find_resources_dir")
    def test_display_result_shows_summary_table(
        self, mock_find, mock_pipeline, valid_v3_path: Path,
    ) -> None:
        mock_find.return_value = Path("src")
        mock_pipeline.return_value = _success_result()
        runner = CliRunner()
        result = runner.invoke(
            main, ["generate", "-c", str(valid_v3_path)],
        )
        assert result.exit_code == 0
        assert "Component" in result.output
        assert "Rules" in result.output
        assert "Total" in result.output

    @patch("ia_dev_env.__main__.run_pipeline")
    @patch("ia_dev_env.__main__.find_resources_dir")
    def test_display_result_shows_duration(
        self, mock_find, mock_pipeline, valid_v3_path: Path,
    ) -> None:
        mock_find.return_value = Path("src")
        mock_pipeline.return_value = _success_result()
        runner = CliRunner()
        result = runner.invoke(
            main, ["generate", "-c", str(valid_v3_path)],
        )
        assert "42ms" in result.output
