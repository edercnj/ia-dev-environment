from __future__ import annotations

from pathlib import Path

import pytest

from claude_setup.assembler import run_pipeline
from claude_setup.config import load_config
from claude_setup.verifier import verify_output

CONFIG_PROFILES = [
    "go-gin",
    "java-quarkus",
    "java-spring",
    "kotlin-ktor",
    "python-click-cli",
    "python-fastapi",
    "rust-axum",
    "typescript-nestjs",
]

PROJECT_ROOT = Path(__file__).resolve().parent.parent
CONFIG_TEMPLATES_DIR = PROJECT_ROOT / "resources" / "config-templates"
GOLDEN_DIR = Path(__file__).resolve().parent / "golden"
RESOURCES_DIR = PROJECT_ROOT / "resources"

GOLDEN_MISSING_MSG = "Golden files not found. Run: python scripts/generate_golden.py --all"


def _run_pipeline_for_profile(
    profile_name: str,
    output_dir: Path,
):
    """Load config and run pipeline for a profile."""
    config_path = (
        CONFIG_TEMPLATES_DIR
        / f"setup-config.{profile_name}.yaml"
    )
    config = load_config(config_path)
    return run_pipeline(config, RESOURCES_DIR, output_dir)


def _golden_path_for_profile(profile_name: str) -> Path:
    """Return golden directory path for a profile."""
    return GOLDEN_DIR / profile_name


def _skip_if_no_golden(profile_name: str) -> None:
    """Skip test if golden directory does not exist."""
    golden = _golden_path_for_profile(profile_name)
    if not golden.exists():
        pytest.skip(GOLDEN_MISSING_MSG)


@pytest.mark.parametrize("profile_name", CONFIG_PROFILES)
class TestByteForByte:

    def test_pipeline_matches_golden_files(
        self, profile_name: str, tmp_path: Path,
    ) -> None:
        _skip_if_no_golden(profile_name)
        output_dir = tmp_path / "output"
        _run_pipeline_for_profile(profile_name, output_dir)
        golden = _golden_path_for_profile(profile_name)
        result = verify_output(output_dir, golden)
        assert result.success, _format_mismatches(result)

    def test_no_missing_files(
        self, profile_name: str, tmp_path: Path,
    ) -> None:
        _skip_if_no_golden(profile_name)
        output_dir = tmp_path / "output"
        _run_pipeline_for_profile(profile_name, output_dir)
        golden = _golden_path_for_profile(profile_name)
        result = verify_output(output_dir, golden)
        assert result.missing_files == []

    def test_no_extra_files(
        self, profile_name: str, tmp_path: Path,
    ) -> None:
        _skip_if_no_golden(profile_name)
        output_dir = tmp_path / "output"
        _run_pipeline_for_profile(profile_name, output_dir)
        golden = _golden_path_for_profile(profile_name)
        result = verify_output(output_dir, golden)
        assert result.extra_files == []

    def test_pipeline_success_for_profile(
        self, profile_name: str, tmp_path: Path,
    ) -> None:
        output_dir = tmp_path / "output"
        pipeline_result = _run_pipeline_for_profile(
            profile_name, output_dir,
        )
        assert pipeline_result.success is True

    def test_total_files_greater_than_zero(
        self, profile_name: str, tmp_path: Path,
    ) -> None:
        _skip_if_no_golden(profile_name)
        output_dir = tmp_path / "output"
        _run_pipeline_for_profile(profile_name, output_dir)
        golden = _golden_path_for_profile(profile_name)
        result = verify_output(output_dir, golden)
        assert result.total_files > 0


def _format_mismatches(result) -> str:
    """Format verification failures for readable output."""
    lines = []
    for m in result.mismatches:
        lines.append(
            f"MISMATCH: {m.path} "
            f"(py={m.python_size}B, ref={m.reference_size}B)"
        )
        lines.append(m.diff[:500])
    for p in result.missing_files:
        lines.append(f"MISSING: {p}")
    for p in result.extra_files:
        lines.append(f"EXTRA: {p}")
    return "\n".join(lines)
