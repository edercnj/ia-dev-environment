from __future__ import annotations

import click

from claude_setup import __version__


@click.group(invoke_without_command=True)
@click.version_option(version=__version__)
@click.pass_context
def main(ctx: click.Context) -> None:
    """Claude Setup - Project scaffolding tool."""
    if ctx.invoked_subcommand is None:
        click.echo(ctx.get_help())


if __name__ == "__main__":
    main()
