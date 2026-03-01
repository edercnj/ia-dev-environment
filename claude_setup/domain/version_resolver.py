from __future__ import annotations

from pathlib import Path
from typing import Optional


def find_version_dir(
    base_dir: Path,
    name: str,
    version: str,
) -> Optional[Path]:
    """Find version-specific directory with major-version fallback.

    Tries exact match first ({name}-{version}), then major version
    with .x suffix ({name}-{major}.x). Returns None if not found.
    """
    exact = base_dir / f"{name}-{version}"
    if exact.is_dir():
        return exact
    major = version.split(".")[0]
    fallback = base_dir / f"{name}-{major}.x"
    if fallback.is_dir():
        return fallback
    return None
