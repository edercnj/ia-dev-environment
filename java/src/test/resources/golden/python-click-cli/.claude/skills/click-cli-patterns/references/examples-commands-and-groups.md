# Example: Click Commands and Groups

### Root Group

```python
from __future__ import annotations

from pathlib import Path
from typing import Optional

import click

from my_cli.core.config import CliConfig, load_config


@click.group()
@click.option(
    "--config",
    "-c",
    "config_path",
    type=click.Path(exists=True, dir_okay=False, path_type=Path),
    default=None,
    help="Path to configuration file.",
)
@click.option("--verbose", "-v", is_flag=True, default=False, help="Enable verbose output.")
@click.version_option(package_name="my-cli")
@click.pass_context
def main(ctx: click.Context, config_path: Optional[Path], verbose: bool) -> None:
    """My CLI tool — generates project scaffolding."""
    ctx.ensure_object(dict)
    ctx.obj["verbose"] = verbose
    ctx.obj["config"] = load_config(config_path)
```

### Subcommand

```python
from __future__ import annotations

from pathlib import Path

import click


@click.command()
@click.argument("project_name")
@click.option(
    "--output-dir",
    "-o",
    type=click.Path(file_okay=False, path_type=Path),
    default=Path("."),
    help="Output directory.",
)
@click.option(
    "--template",
    "-t",
    type=click.Choice(["minimal", "standard", "full"], case_sensitive=False),
    default="standard",
    help="Project template to use.",
)
@click.option("--dry-run", is_flag=True, default=False, help="Show what would be created.")
@click.pass_context
def init(
    ctx: click.Context,
    project_name: str,
    output_dir: Path,
    template: str,
    dry_run: bool,
) -> None:
    """Initialize a new project from template."""
    config: CliConfig = ctx.obj["config"]
    verbose: bool = ctx.obj["verbose"]

    target = output_dir / project_name
    if target.exists() and not dry_run:
        raise click.ClickException(f"Directory already exists: {target}")

    result = scaffold_project(
        name=project_name,
        target=target,
        template_name=template,
        config=config,
        dry_run=dry_run,
    )

    if verbose:
        for file_path in result.created_files:
            click.echo(f"  Created: {file_path}")
    click.secho(f"Project 'my-cli-tool' initialized.", fg="green")
```

### Registering Commands

```python
from __future__ import annotations

from my_cli.commands.init_cmd import init
from my_cli.commands.build_cmd import build
from my_cli.commands.validate_cmd import validate

main.add_command(init)
main.add_command(build)
main.add_command(validate)
```

### Option Validation (Callbacks)

```python
from __future__ import annotations

import re

import click


def validate_project_name(
    ctx: click.Context,
    param: click.Parameter,
    value: str,
) -> str:
    if not re.match(r"^[a-z][a-z0-9_-]*$", value):
        raise click.BadParameter(
            f"Must start with lowercase letter, "
            f"contain only [a-z0-9_-]: {value}"
        )
    return value


@click.command()
@click.argument("name", callback=validate_project_name)
def init(name: str) -> None:
    """Initialize project."""
    ...
```

### Prompting for Missing Values

```python
from __future__ import annotations

import click


@click.command()
@click.option(
    "--author",
    prompt="Author name",
    help="Author name for project metadata.",
)
@click.option(
    "--license",
    "license_type",
    type=click.Choice(["MIT", "Apache-2.0", "GPL-3.0"]),
    prompt="License type",
    default="MIT",
    help="License type.",
)
def init(author: str, license_type: str) -> None:
    """Initialize project with author info."""
    ...
```

### Confirmation

```python
from __future__ import annotations

import click


@click.command()
@click.argument("target")
@click.option("--force", is_flag=True, default=False, help="Skip confirmation.")
def clean(target: str, force: bool) -> None:
    """Remove generated files."""
    if not force:
        click.confirm(f"Delete all files in '{target}'?", abort=True)
    perform_clean(target)
```
