from __future__ import annotations

from pathlib import Path
from typing import Optional

import click

from claude_setup import __version__
from claude_setup.assembler import run_pipeline
from claude_setup.config import load_config
from claude_setup.domain.validator import validate_stack
from claude_setup.exceptions import ConfigValidationError, PipelineError
from claude_setup.interactive import run_interactive
from claude_setup.models import PipelineResult, ProjectConfig
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
def generate(
    config: Optional[str],
    interactive: bool,
    output_dir: str,
    src_dir: Optional[str],
    verbose: bool,
    dry_run: bool,
) -> None:
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


def _validate_generate_options(
    config: Optional[str], interactive: bool,
) -> None:
    """Ensure --config and --interactive are mutually exclusive."""
    if config and interactive:
        raise click.UsageError(
            "Options --config and --interactive are mutually exclusive."
        )
    if not config and not interactive:
        raise click.UsageError(
            "Either --config or --interactive is required."
        )


def _load_project_config(
    config: Optional[str], interactive: bool,
) -> ProjectConfig:
    """Load project config from file or interactive mode."""
    try:
        if config:
            return load_config(Path(config))
        return run_interactive()
    except ConfigValidationError as exc:
        raise click.ClickException(str(exc)) from exc


def _resolve_src_dir(src_dir: Optional[str]) -> Path:
    """Resolve source directory from option or auto-detect."""
    if src_dir:
        return Path(src_dir)
    try:
        return find_src_dir()
    except FileNotFoundError as exc:
        raise click.ClickException(str(exc)) from exc


def _execute_generate(
    project_config: ProjectConfig,
    src_dir: Path,
    output_dir: Path,
    dry_run: bool,
) -> PipelineResult:
    """Run the pipeline with error handling."""
    try:
        return run_pipeline(
            project_config, src_dir, output_dir, dry_run=dry_run,
        )
    except PipelineError as exc:
        raise click.ClickException(str(exc)) from exc


def _display_result(result: PipelineResult) -> None:
    """Display pipeline result summary."""
    if not result.success:
        raise click.ClickException("Pipeline failed")
    click.echo(f"Pipeline: Success")
    click.echo(f"Files generated: {len(result.files_generated)}")
    click.echo(f"Duration: {result.duration_ms}ms")
    for warning in result.warnings:
        click.echo(f"Warning: {warning}")


@main.command()
@click.option("--config", "-c", type=click.Path(exists=True), required=True, help="Path to YAML config file.")
@click.option("--verbose", "-v", is_flag=True, default=False, help="Enable verbose logging.")
def validate(config: str, verbose: bool) -> None:
    """Validate a config file without generating output."""
    if verbose:
        setup_logging(verbose=True)
    try:
        project_config = load_config(Path(config))
    except ConfigValidationError as exc:
        raise click.ClickException(str(exc)) from exc
    errors = validate_stack(project_config)
    if errors:
        message = "\n".join(str(error) for error in errors)
        raise click.ClickException(message)
    click.echo("Config is valid.")


if __name__ == "__main__":
    main()
