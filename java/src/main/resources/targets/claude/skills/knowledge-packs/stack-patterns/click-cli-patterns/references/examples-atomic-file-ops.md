# Example: Atomic File Operations

### Atomic Write (tempfile + os.replace)

```python
from __future__ import annotations

import os
import tempfile
from pathlib import Path


def atomic_write(target: Path, content: str) -> None:
    """Write file atomically — never leaves partial content."""
    target.parent.mkdir(parents=True, exist_ok=True)
    fd, tmp_path = tempfile.mkstemp(
        dir=target.parent,
        suffix=".tmp",
    )
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as f:
            f.write(content)
        os.replace(tmp_path, target)
    except BaseException:
        os.unlink(tmp_path)
        raise
```

### Context Manager for Atomic Writes

```python
from __future__ import annotations

import os
import tempfile
from contextlib import contextmanager
from pathlib import Path
from typing import Generator, TextIO


@contextmanager
def atomic_open(
    target: Path,
    mode: str = "w",
) -> Generator[TextIO, None, None]:
    """Context manager for atomic file writes."""
    target.parent.mkdir(parents=True, exist_ok=True)
    fd, tmp_path = tempfile.mkstemp(
        dir=target.parent,
        suffix=".tmp",
    )
    try:
        with os.fdopen(fd, mode, encoding="utf-8") as f:
            yield f
        os.replace(tmp_path, target)
    except BaseException:
        os.unlink(tmp_path)
        raise
```

### Directory Scaffolding

```python
from __future__ import annotations

import shutil
from dataclasses import dataclass, field
from pathlib import Path


@dataclass
class ScaffoldResult:
    created_files: list[Path] = field(default_factory=list)
    created_dirs: list[Path] = field(default_factory=list)


def scaffold_directory(
    target: Path,
    files: dict[Path, str],
    dry_run: bool = False,
) -> ScaffoldResult:
    """Create directory tree with rendered files."""
    result = ScaffoldResult()

    for relative_path, content in files.items():
        full_path = target / relative_path
        result.created_dirs.append(full_path.parent)
        result.created_files.append(full_path)

        if not dry_run:
            atomic_write(full_path, content)

    return result
```

### Safe Copy with Backup

```python
from __future__ import annotations

import shutil
from pathlib import Path


def safe_copy_tree(
    src: Path,
    dst: Path,
    backup: bool = True,
) -> None:
    """Copy directory tree with optional backup."""
    if dst.exists() and backup:
        backup_path = dst.with_suffix(".bak")
        if backup_path.exists():
            shutil.rmtree(backup_path)
        shutil.copytree(dst, backup_path)
    shutil.copytree(src, dst, dirs_exist_ok=True)
```

### Dry-Run Pattern

```python
from __future__ import annotations

from pathlib import Path

import click


def write_file(
    target: Path,
    content: str,
    dry_run: bool = False,
) -> None:
    """Write file or report what would be written."""
    if dry_run:
        click.echo(f"  [DRY RUN] Would create: {target}")
        return
    atomic_write(target, content)
```

### Path Utilities

```python
from __future__ import annotations

from pathlib import Path


def ensure_parent_dirs(path: Path) -> Path:
    path.parent.mkdir(parents=True, exist_ok=True)
    return path


def relative_to_cwd(path: Path) -> Path:
    try:
        return path.relative_to(Path.cwd())
    except ValueError:
        return path
```
