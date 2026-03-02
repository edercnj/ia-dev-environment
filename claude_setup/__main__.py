from __future__ import annotations

import sys
from pathlib import Path

import click

from claude_setup import __version__
from claude_setup.assembler import run_pipeline
from claude_setup.config import load_config
from claude_setup.domain.validator import validate_stack
from claude_setup.exceptions import ConfigValidationError, PipelineError
from claude_setup.interactive import run_interactive
from claude_setup.models import PipelineResult
from claude_setup.utils import find_src_dir, setup_logging


@click.group(invoke_without_command=True)
@click.version_option(version=__version__)
@click.pass_context
def main(ctx: click.Context) -> None:
    """Claude Setup - Project scaffolding tool."""
    if ctx.invoked_subcommand is None:
        click.echo(ctx.get_help())


@main.command()
@click.option("--config", "-c", type=click.Path(exists=True), default=None, help="Path to YAML config file.")
@click.option("--interactive", "-i", is_flag=True, default=False, help="Run in interactive mode.")
@click.option("--output-dir", "-o", type=click.Path(), default=".", help="Output directory.")
@click.option("--src-dir", "-s", type=click.Path(exists=True), default=None, help="Source templates directory.")
@click.option("--verbose", "-v", is_flag=True, default=False, help="Enable verbose logging.")
@click.option("--dry-run", is_flag=True, default=False, help="Show what would be generated without writing.")
def generate(config, interactive, output_dir, src_dir, verbose, dry_run) -> None:
    """Generate project scaffolding from config or interactive mode."""
    _validate_generate_options(config, interactive)
    if verbose:
        setup_logging(verbose=True)
    project_config = _load_project_config(config, interactive)
    resolved_src = _resolve_src_dir(src_dir)
    resolved_output = Path(output_dir)
    result = _execute_generate(
        project_config, resolved_src, resolved_output, dry_run,
    )
    _display_result(result)
    if not result.success:
        sys.exit(1)


def _validate_generate_options(config, interactive) -> None:
    """Ensure --config and --interactive are mutually exclusive."""
    if config and interactive:
        raise click.UsageError(
            "Options --config and --interactive are mutually exclusive."
        )
    if not config and not interactive:
        raise click.UsageError(
            "Either --config or --interactive is required."
        )


def _load_project_config(config, interactive):
    """Load project config from file or interactive mode."""
    try:
        if config:
            return load_config(Path(config))
        return run_interactive()
    except ConfigValidationError as exc:
        raise click.ClickException(str(exc)) from exc


def _resolve_src_dir(src_dir) -> Path:
    """Resolve source directory from option or auto-detect."""
    if src_dir:
        return Path(src_dir)
    try:
        return find_src_dir()
    except FileNotFoundError as exc:
        raise click.ClickException(str(exc)) from exc


def _execute_generate(project_config, src_dir, output_dir, dry_run) -> PipelineResult:
    """Run the pipeline with error handling."""
    try:
        return run_pipeline(project_config, src_dir, output_dir, dry_run=dry_run)
    except PipelineError as exc:
        raise click.ClickException(str(exc)) from exc


def _display_result(result: PipelineResult) -> None:
    """Display pipeline result summary."""
    status = "Success" if result.success else "Failed"
    click.echo(f"Pipeline: {status}")
    click.echo(f"Files generated: {len(result.files_generated)}")
    click.echo(f"Duration: {result.duration_ms}ms")
    for warning in result.warnings:
        click.echo(f"Warning: {warning}")


@main.command()
@click.option("--config", "-c", type=click.Path(exists=True), required=True, help="Path to YAML config file.")
@click.option("--verbose", "-v", is_flag=True, default=False, help="Enable verbose logging.")
def validate(config, verbose) -> None:
    """Validate a config file without generating output."""
    if verbose:
        setup_logging(verbose=True)
    try:
        project_config = load_config(Path(config))
    except ConfigValidationError as exc:
        click.echo(f"Error: {exc}", err=True)
        sys.exit(1)
    errors = validate_stack(project_config)
    if errors:
        for error in errors:
            click.echo(f"Error: {error}", err=True)
        sys.exit(1)
    click.echo("Config is valid.")


if __name__ == "__main__":
    main()
