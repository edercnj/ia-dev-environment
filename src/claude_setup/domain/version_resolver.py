from __future__ import annotations

from pathlib import Path
from typing import Optional


def find_version_dir(
    base_dir: Path,
    name: str,
    version: str,
) -> Optional[Path]:
    """Find a version-specific directory with fallback.

    Resolution order:
    1. Exact match: {base_dir}/{name}-{version}
    2. Major version fallback: {base_dir}/{name}-{major}.x

    Returns None if neither directory exists.
    """
    exact = base_dir / f"{name}-{version}"
    if exact.is_dir():
        return exact
    major = version.split(".")[0]
    fallback = base_dir / f"{name}-{major}.x"
    if fallback.is_dir():
        return fallback
    return None
