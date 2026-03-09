from __future__ import annotations

from pathlib import Path

import pytest

from ia_dev_env.assembler import run_pipeline
from ia_dev_env.config import load_config
from ia_dev_env.models import VerificationResult
from ia_dev_env.verifier import verify_output

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

GOLDEN_MISSING_MSG = (
    "Golden files not found. "
    "Run: python scripts/generate_golden.py --all"
)


def _format_failures(result: VerificationResult) -> str:
    """Format verification failures for readable output."""
    lines = []
    for m in result.mismatches:
        lines.append(
            f"MISMATCH: {m.path} "
            f"(python={m.python_size}B, ref={m.reference_size}B)"
        )
        lines.append(m.diff[:500])
    for p in result.missing_files:
        lines.append(f"MISSING: {p}")
    for p in result.extra_files:
        lines.append(f"EXTRA: {p}")
    return "\n".join(lines)


def _skip_if_no_golden(profile_name: str) -> None:
    """Skip test if golden directory does not exist."""
    golden = GOLDEN_DIR / profile_name
    if not golden.exists():
        pytest.skip(GOLDEN_MISSING_MSG)


@pytest.mark.parametrize("profile_name", CONFIG_PROFILES)
class TestE2EVerification:

    def test_full_flow_for_profile(
        self, profile_name: str, tmp_path: Path,
    ) -> None:
        _skip_if_no_golden(profile_name)
        config_path = (
            CONFIG_TEMPLATES_DIR
            / f"setup-config.{profile_name}.yaml"
        )
        config = load_config(config_path)
        output_dir = tmp_path / "output"
        result = run_pipeline(config, RESOURCES_DIR, output_dir)
        assert result.success is True
        golden_path = GOLDEN_DIR / profile_name
        verification = verify_output(output_dir, golden_path)
        assert verification.success, _format_failures(
            verification,
        )
