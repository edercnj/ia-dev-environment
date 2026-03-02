from __future__ import annotations

import logging
import shutil
import tempfile
from contextlib import contextmanager
from pathlib import Path
from typing import Generator


PROTECTED_PATHS = frozenset({"/", "/tmp", "/var", "/etc", "/usr"})


def _validate_dest_path(dest_dir: Path) -> Path:
    """Resolve and validate destination path against traversal attacks."""
    if dest_dir.is_symlink():
        raise ValueError(
            f"Destination must not be a symlink: {dest_dir}"
        )
    resolved = dest_dir.resolve()
    _reject_dangerous_path(resolved)
    return resolved


def _reject_dangerous_path(resolved: Path) -> None:
    """Reject paths that would cause destructive rmtree."""
    cwd = Path.cwd().resolve()
    home = Path.home().resolve()
    path_str = str(resolved)
    if resolved == cwd:
        raise ValueError(
            f"Destination must not be the current directory: {resolved}"
        )
    if resolved == home:
        raise ValueError(
            f"Destination must not be the home directory: {resolved}"
        )
    if path_str in PROTECTED_PATHS:
        raise ValueError(
            f"Destination is a protected system path: {resolved}"
        )


@contextmanager
def atomic_output(dest_dir: Path) -> Generator[Path, None, None]:
    """Context manager that provides atomic file output.

    Creates a temp directory, yields it for writing, and on
    success copies contents to dest_dir. On failure, cleans up
    temp without modifying dest_dir.
    """
    resolved_dest = _validate_dest_path(dest_dir)
    temp_dir = Path(tempfile.mkdtemp(prefix="claude-setup-"))
    try:
        yield temp_dir
        if resolved_dest.exists():
            shutil.rmtree(str(resolved_dest))
        shutil.copytree(str(temp_dir), str(resolved_dest))
    finally:
        if temp_dir.exists():
            shutil.rmtree(str(temp_dir))


def setup_logging(verbose: bool) -> None:
    """Configure root logger level based on verbose flag."""
    level = logging.DEBUG if verbose else logging.INFO
    logging.basicConfig(
        level=level,
        format="%(levelname)s: %(message)s",
        force=True,
    )


def find_resources_dir() -> Path:
    """Locate the resources/ directory relative to the package."""
    resources = Path(__file__).resolve().parent.parent.parent / "resources"
    if not resources.is_dir():
        raise FileNotFoundError(
            f"Resources directory not found: {resources}"
        )
    return resources
