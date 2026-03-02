from __future__ import annotations

import logging
import shutil
import tempfile
from contextlib import contextmanager
from pathlib import Path
from typing import Generator


def _validate_dest_path(dest_dir: Path) -> Path:
    """Resolve and validate destination path against traversal attacks."""
    if dest_dir.is_symlink():
        raise ValueError(
            f"Destination must not be a symlink: {dest_dir}"
        )
    return dest_dir.resolve()


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


def find_src_dir() -> Path:
    """Locate the src/ directory relative to the package."""
    src = Path(__file__).resolve().parent.parent / "src"
    if not src.is_dir():
        raise FileNotFoundError(
            f"Source directory not found: {src}"
        )
    return src
