from __future__ import annotations

from pathlib import Path
from unittest.mock import patch

import pytest

from claude_setup.assembler import run_pipeline
from claude_setup.exceptions import PipelineError
from claude_setup.models import ProjectConfig
from claude_setup.utils import find_resources_dir


def _build_config() -> ProjectConfig:
    """Build a config matching this project's identity."""
    return ProjectConfig.from_dict({
        "project": {"name": "my-cli-tool", "purpose": "CLI tool"},
        "architecture": {"style": "library"},
        "interfaces": [{"type": "cli"}],
        "language": {"name": "python", "version": "3.9"},
        "framework": {"name": "click", "version": "8.1"},
    })


class TestPipelineIntegration:

    def test_pipeline_success_with_valid_config(
        self, tmp_path: Path,
    ) -> None:
        config = _build_config()
        resources_dir = find_resources_dir()
        output = tmp_path / "output"
        result = run_pipeline(config, resources_dir, output)
        assert result.success is True

    def test_pipeline_generates_rules_directory(
        self, tmp_path: Path,
    ) -> None:
        config = _build_config()
        resources_dir = find_resources_dir()
        output = tmp_path / "output"
        run_pipeline(config, resources_dir, output)
        rules_dir = output / "rules"
        assert rules_dir.is_dir()
        md_files = list(rules_dir.glob("*.md"))
        assert len(md_files) > 0

    def test_pipeline_generates_settings_json(
        self, tmp_path: Path,
    ) -> None:
        config = _build_config()
        resources_dir = find_resources_dir()
        output = tmp_path / "output"
        run_pipeline(config, resources_dir, output)
        settings = output / "settings.json"
        assert settings.is_file()
        import json
        data = json.loads(settings.read_text())
        assert "permissions" in data

    def test_pipeline_generates_readme(
        self, tmp_path: Path,
    ) -> None:
        config = _build_config()
        resources_dir = find_resources_dir()
        output = tmp_path / "output"
        run_pipeline(config, resources_dir, output)
        readme = output / "README.md"
        assert readme.is_file()

    def test_pipeline_files_generated_not_empty(
        self, tmp_path: Path,
    ) -> None:
        config = _build_config()
        resources_dir = find_resources_dir()
        output = tmp_path / "output"
        result = run_pipeline(config, resources_dir, output)
        assert len(result.files_generated) > 0

    def test_pipeline_duration_ms_positive(
        self, tmp_path: Path,
    ) -> None:
        config = _build_config()
        resources_dir = find_resources_dir()
        output = tmp_path / "output"
        result = run_pipeline(config, resources_dir, output)
        assert result.duration_ms > 0

    def test_pipeline_dry_run_no_output(
        self, tmp_path: Path,
    ) -> None:
        config = _build_config()
        resources_dir = find_resources_dir()
        output = tmp_path / "output"
        result = run_pipeline(config, resources_dir, output, dry_run=True)
        assert result.success is True
        assert not output.exists()

    def test_pipeline_dry_run_lists_files(
        self, tmp_path: Path,
    ) -> None:
        config = _build_config()
        resources_dir = find_resources_dir()
        output = tmp_path / "output"
        result = run_pipeline(config, resources_dir, output, dry_run=True)
        assert len(result.files_generated) > 0

    def test_pipeline_generates_testing_skills(
        self, tmp_path: Path,
    ) -> None:
        config = _build_config()
        resources_dir = find_resources_dir()
        output = tmp_path / "output"
        result = run_pipeline(config, resources_dir, output)
        testing_skills = [
            "x-test-plan",
            "x-test-run",
            "run-e2e",
            "run-smoke-api",
            "run-contract-tests",
            "run-perf-test",
        ]
        generated_names = {
            p.parent.name for p in result.files_generated
        }
        for skill in testing_skills:
            assert skill in generated_names, (
                f"Testing skill {skill} missing from pipeline"
            )

    def test_pipeline_atomic_cleanup_on_failure(
        self, tmp_path: Path,
    ) -> None:
        config = _build_config()
        resources_dir = find_resources_dir()
        output = tmp_path / "output"
        with patch(
            "claude_setup.assembler.readme_assembler.ReadmeAssembler.assemble",
            side_effect=RuntimeError("forced failure"),
        ):
            with pytest.raises(PipelineError):
                run_pipeline(config, resources_dir, output)
        assert not output.exists()
