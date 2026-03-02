from __future__ import annotations

import time
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
CONFIG_TEMPLATES_DIR = PROJECT_ROOT / "src" / "config-templates"
GOLDEN_DIR = Path(__file__).resolve().parent / "golden"
SRC_DIR = PROJECT_ROOT / "src"

PIPELINE_TIME_LIMIT_MS = 5000
VERIFICATION_TIME_LIMIT_MS = 1000

GOLDEN_MISSING_MSG = (
    "Golden files not found. "
    "Run: python scripts/generate_golden.py --all"
)


def _skip_if_no_golden(profile_name: str) -> None:
    """Skip test if golden directory does not exist."""
    golden = GOLDEN_DIR / profile_name
    if not golden.exists():
        pytest.skip(GOLDEN_MISSING_MSG)


@pytest.mark.parametrize("profile_name", CONFIG_PROFILES)
class TestPerformance:

    def test_pipeline_under_five_seconds(
        self, profile_name: str, tmp_path: Path,
    ) -> None:
        config_path = (
            CONFIG_TEMPLATES_DIR
            / f"setup-config.{profile_name}.yaml"
        )
        config = load_config(config_path)
        output_dir = tmp_path / "output"
        start = time.monotonic()
        run_pipeline(config, SRC_DIR, output_dir)
        elapsed_ms = int(
            (time.monotonic() - start) * 1000,
        )
        assert elapsed_ms < PIPELINE_TIME_LIMIT_MS, (
            f"{profile_name} took {elapsed_ms}ms"
        )

    def test_verification_under_one_second(
        self, profile_name: str, tmp_path: Path,
    ) -> None:
        _skip_if_no_golden(profile_name)
        config_path = (
            CONFIG_TEMPLATES_DIR
            / f"setup-config.{profile_name}.yaml"
        )
        config = load_config(config_path)
        output_dir = tmp_path / "output"
        run_pipeline(config, SRC_DIR, output_dir)
        golden = GOLDEN_DIR / profile_name
        start = time.monotonic()
        verify_output(output_dir, golden)
        elapsed_ms = int(
            (time.monotonic() - start) * 1000,
        )
        assert elapsed_ms < VERIFICATION_TIME_LIMIT_MS, (
            f"{profile_name} verification took {elapsed_ms}ms"
        )
