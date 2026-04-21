# Example: CLI Output and Logging

### Standard Output with Click

```python
from __future__ import annotations

import click


def echo_info(message: str) -> None:
    click.echo(message)


def echo_success(message: str) -> None:
    click.secho(message, fg="green")


def echo_warning(message: str) -> None:
    click.secho(f"Warning: {message}", fg="yellow", err=True)


def echo_error(message: str) -> None:
    click.secho(f"Error: {message}", fg="red", err=True)
```

### Verbosity Levels

```python
from __future__ import annotations

from dataclasses import dataclass

import click


@dataclass
class Output:
    verbose: bool = False

    def info(self, message: str) -> None:
        click.echo(message)

    def detail(self, message: str) -> None:
        if self.verbose:
            click.echo(f"  {message}")

    def success(self, message: str) -> None:
        click.secho(message, fg="green")

    def warning(self, message: str) -> None:
        click.secho(f"Warning: {message}", fg="yellow", err=True)

    def error(self, message: str) -> None:
        click.secho(f"Error: {message}", fg="red", err=True)
```

### Progress Bar

```python
from __future__ import annotations

from pathlib import Path
from typing import Sequence

import click


def render_files(
    files: Sequence[tuple[Path, str]],
    output: Output,
) -> None:
    with click.progressbar(
        files,
        label="Generating files",
        show_pos=True,
    ) as progress:
        for file_path, content in progress:
            atomic_write(file_path, content)
            output.detail(f"Created: {file_path}")
```

### Stdout vs Stderr Separation

```
stdout → machine-parseable output (file paths, JSON, rendered content)
stderr → human-readable messages (progress, warnings, errors)
```

```python
from __future__ import annotations

from pathlib import Path

import click


def list_generated_files(files: list[Path]) -> None:
    """Machine output to stdout, status to stderr."""
    click.echo(
        f"Generated {len(files)} files:", err=True,
    )
    for f in files:
        click.echo(str(f))  # stdout — pipeable
```

### Structured Logging (for long-running CLIs)

```python
from __future__ import annotations

import logging
import sys


def configure_logging(verbose: bool = False) -> None:
    level = logging.DEBUG if verbose else logging.INFO
    handler = logging.StreamHandler(sys.stderr)
    handler.setFormatter(
        logging.Formatter(
            "%(asctime)s [%(levelname)s] %(name)s: %(message)s",
            datefmt="%Y-%m-%d %H:%M:%S",
        )
    )
    root = logging.getLogger("my_cli")
    root.setLevel(level)
    root.addHandler(handler)
```
