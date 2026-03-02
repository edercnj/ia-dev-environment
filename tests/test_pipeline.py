from __future__ import annotations

from pathlib import Path
from typing import List
from unittest.mock import MagicMock, patch

import pytest

from claude_setup.assembler import (
    _build_assemblers,
    _execute_assemblers,
    run_pipeline,
)
from claude_setup.exceptions import PipelineError
from claude_setup.models import PipelineResult, ProjectConfig
from claude_setup.template_engine import TemplateEngine


def _make_config() -> ProjectConfig:
    """Build a minimal ProjectConfig for testing."""
    return ProjectConfig.from_dict({
        "project": {"name": "test-proj", "purpose": "Testing"},
        "architecture": {"style": "library"},
        "interfaces": [{"type": "cli"}],
        "language": {"name": "python", "version": "3.9"},
        "framework": {"name": "click", "version": "8.1"},
    })


def _mock_assembler(name: str, return_files: List[Path] = None):
    """Create a mock assembler with a proper assemble method."""
    mock = MagicMock()
    mock.assemble.return_value = return_files or []
    return mock


class TestBuildAssemblers:

    def test_returns_eight_assemblers(self, tmp_path: Path) -> None:
        assemblers = _build_assemblers(tmp_path)
        assert len(assemblers) == 8

    def test_last_assembler_is_readme(self, tmp_path: Path) -> None:
        assemblers = _build_assemblers(tmp_path)
        name, _ = assemblers[-1]
        assert name == "ReadmeAssembler"

    def test_first_assembler_is_rules(self, tmp_path: Path) -> None:
        assemblers = _build_assemblers(tmp_path)
        name, _ = assemblers[0]
        assert name == "RulesAssembler"


class TestRunPipeline:

    @patch("claude_setup.assembler._execute_assemblers")
    @patch("claude_setup.assembler.TemplateEngine")
    def test_returns_pipeline_result(
        self, mock_engine, mock_exec, tmp_path: Path,
    ) -> None:
        mock_exec.return_value = ([], [])
        config = _make_config()
        src = tmp_path / "src"
        src.mkdir()
        output = tmp_path / "output"
        result = run_pipeline(config, src, output)
        assert isinstance(result, PipelineResult)

    @patch("claude_setup.assembler._execute_assemblers")
    @patch("claude_setup.assembler.TemplateEngine")
    def test_success_flag_true_on_completion(
        self, mock_engine, mock_exec, tmp_path: Path,
    ) -> None:
        mock_exec.return_value = ([], [])
        config = _make_config()
        src = tmp_path / "src"
        src.mkdir()
        output = tmp_path / "output"
        result = run_pipeline(config, src, output)
        assert result.success is True

    @patch("claude_setup.assembler._execute_assemblers")
    @patch("claude_setup.assembler.TemplateEngine")
    def test_files_generated_contains_assembler_outputs(
        self, mock_engine, mock_exec, tmp_path: Path,
    ) -> None:
        expected_files = [Path("a.md"), Path("b.md")]
        mock_exec.return_value = (expected_files, [])
        config = _make_config()
        src = tmp_path / "src"
        src.mkdir()
        output = tmp_path / "output"
        result = run_pipeline(config, src, output)
        assert result.files_generated == expected_files

    @patch("claude_setup.assembler._execute_assemblers")
    @patch("claude_setup.assembler.TemplateEngine")
    def test_duration_ms_is_positive(
        self, mock_engine, mock_exec, tmp_path: Path,
    ) -> None:
        mock_exec.return_value = ([], [])
        config = _make_config()
        src = tmp_path / "src"
        src.mkdir()
        output = tmp_path / "output"
        result = run_pipeline(config, src, output)
        assert result.duration_ms >= 0

    @patch("claude_setup.assembler._execute_assemblers")
    @patch("claude_setup.assembler.TemplateEngine")
    def test_output_dir_matches_requested(
        self, mock_engine, mock_exec, tmp_path: Path,
    ) -> None:
        mock_exec.return_value = ([], [])
        config = _make_config()
        src = tmp_path / "src"
        src.mkdir()
        output = tmp_path / "my-output"
        result = run_pipeline(config, src, output)
        assert result.output_dir == output

    @patch("claude_setup.assembler._execute_assemblers")
    @patch("claude_setup.assembler.TemplateEngine")
    def test_dry_run_does_not_write_to_output(
        self, mock_engine, mock_exec, tmp_path: Path,
    ) -> None:
        mock_exec.return_value = ([Path("a.md")], [])
        config = _make_config()
        src = tmp_path / "src"
        src.mkdir()
        output = tmp_path / "output"
        run_pipeline(config, src, output, dry_run=True)
        assert not output.exists()

    @patch("claude_setup.assembler._execute_assemblers")
    @patch("claude_setup.assembler.TemplateEngine")
    def test_dry_run_returns_success(
        self, mock_engine, mock_exec, tmp_path: Path,
    ) -> None:
        mock_exec.return_value = ([], [])
        config = _make_config()
        src = tmp_path / "src"
        src.mkdir()
        output = tmp_path / "output"
        result = run_pipeline(config, src, output, dry_run=True)
        assert result.success is True

    @patch("claude_setup.assembler._execute_assemblers")
    @patch("claude_setup.assembler.TemplateEngine")
    def test_dry_run_includes_warning(
        self, mock_engine, mock_exec, tmp_path: Path,
    ) -> None:
        mock_exec.return_value = ([], [])
        config = _make_config()
        src = tmp_path / "src"
        src.mkdir()
        output = tmp_path / "output"
        result = run_pipeline(config, src, output, dry_run=True)
        assert any("Dry run" in w for w in result.warnings)

    @patch("claude_setup.assembler._execute_assemblers")
    @patch("claude_setup.assembler.TemplateEngine")
    def test_assembler_failure_raises_pipeline_error(
        self, mock_engine, mock_exec, tmp_path: Path,
    ) -> None:
        mock_exec.side_effect = PipelineError("RulesAssembler", "boom")
        config = _make_config()
        src = tmp_path / "src"
        src.mkdir()
        output = tmp_path / "output"
        with pytest.raises(PipelineError, match="RulesAssembler"):
            run_pipeline(config, src, output)

    @patch("claude_setup.assembler._execute_assemblers")
    @patch("claude_setup.assembler.TemplateEngine")
    def test_assembler_failure_cleans_up_temp(
        self, mock_engine, mock_exec, tmp_path: Path,
    ) -> None:
        mock_exec.side_effect = PipelineError("RulesAssembler", "fail")
        config = _make_config()
        src = tmp_path / "src"
        src.mkdir()
        output = tmp_path / "output"
        with pytest.raises(PipelineError):
            run_pipeline(config, src, output)
        assert not output.exists()


class TestExecuteAssemblers:

    def test_wraps_exception_in_pipeline_error(self, tmp_path: Path) -> None:
        config = _make_config()
        src = tmp_path / "src"
        src.mkdir()
        engine = MagicMock(spec=TemplateEngine)
        with patch("claude_setup.assembler._build_assemblers") as mock_build:
            bad = MagicMock()
            bad.assemble.side_effect = RuntimeError("disk full")
            mock_build.return_value = [("TestAssembler", bad)]
            with patch("claude_setup.assembler._call_assembler") as mock_call:
                mock_call.side_effect = RuntimeError("disk full")
                with pytest.raises(PipelineError) as exc_info:
                    _execute_assemblers(config, src, tmp_path, engine)
                assert "TestAssembler" in str(exc_info.value)
