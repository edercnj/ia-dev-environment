from __future__ import annotations

from pathlib import Path

import click

from claude_setup import __version__
from claude_setup.config import load_config
from claude_setup.exceptions import ConfigValidationError
from claude_setup.interactive import run_interactive


@click.group(invoke_without_command=True)
@click.version_option(version=__version__)
@click.pass_context
def main(ctx: click.Context) -> None:
    """Claude Setup - Project scaffolding tool."""
    if ctx.invoked_subcommand is None:
        click.echo(ctx.get_help())


@main.command()
@click.option(
    "--config",
    "-c",
    type=click.Path(exists=True),
    default=None,
    help="Path to YAML config file.",
)
def init(config: str | None) -> None:
    """Initialize project from config file or interactive mode."""
    try:
        if config:
            project_config = load_config(Path(config))
        else:
            project_config = run_interactive()
        click.echo(f"Loaded config for: {project_config.project.name}")
    except ConfigValidationError as exc:
        raise click.ClickException(str(exc)) from exc


if __name__ == "__main__":
    main()
