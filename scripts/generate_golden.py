"""Generate golden reference files for all config profiles."""
from __future__ import annotations

import argparse
import shutil
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(PROJECT_ROOT / "src"))
RESOURCES_DIR = PROJECT_ROOT / "resources"
CONFIG_DIR = RESOURCES_DIR / "config-templates"
GOLDEN_DIR = PROJECT_ROOT / "tests" / "golden"
CONFIG_PREFIX = "setup-config."
CONFIG_SUFFIX = ".yaml"


def _discover_profiles() -> list:
    """Find all config profile names from config-templates."""
    profiles = []
    for path in sorted(CONFIG_DIR.glob("setup-config.*.yaml")):
        name = path.stem.replace(CONFIG_PREFIX.rstrip("."), "")
        name = name.lstrip(".")
        profiles.append((name, path))
    return profiles


def _generate_single_profile(
    profile_name: str,
    config_path: Path,
) -> bool:
    """Generate golden files for one profile. Returns True on success."""
    # Lazy imports to avoid import errors when running --help
    from ia_dev_env.assembler import run_pipeline
    from ia_dev_env.config import load_config

    output_dir = GOLDEN_DIR / profile_name
    if output_dir.exists():
        shutil.rmtree(str(output_dir))
    output_dir.mkdir(parents=True, exist_ok=True)
    config = load_config(config_path)
    result = run_pipeline(config, RESOURCES_DIR, output_dir)
    return result.success


def _generate_all() -> int:
    """Generate golden files for all profiles. Returns failure count."""
    profiles = _discover_profiles()
    failures = 0
    for name, path in profiles:
        print(f"Generating golden files for: {name}")
        success = _generate_single_profile(name, path)
        status = "OK" if success else "FAILED"
        print(f"  {status}")
        if not success:
            failures += 1
    total = len(profiles)
    print(f"\nSummary: {total - failures}/{total} succeeded")
    return failures


def _generate_profile(profile_name: str) -> int:
    """Generate golden files for a single profile."""
    config_path = CONFIG_DIR / f"setup-config.{profile_name}.yaml"
    if not config_path.exists():
        print(f"Config not found: {config_path}")
        return 1
    print(f"Generating golden files for: {profile_name}")
    success = _generate_single_profile(profile_name, config_path)
    status = "OK" if success else "FAILED"
    print(f"  {status}")
    return 0 if success else 1


def _build_parser() -> argparse.ArgumentParser:
    """Build CLI argument parser."""
    parser = argparse.ArgumentParser(
        description="Generate golden reference files",
    )
    parser.add_argument(
        "--profile",
        type=str,
        default=None,
        help="Generate for a single profile",
    )
    return parser


def main() -> int:
    """Entry point for golden file generation."""
    parser = _build_parser()
    args = parser.parse_args()
    GOLDEN_DIR.mkdir(parents=True, exist_ok=True)
    if args.profile:
        return _generate_profile(args.profile)
    return _generate_all()


if __name__ == "__main__":
    sys.exit(main())
