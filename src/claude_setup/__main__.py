from __future__ import annotations

from pathlib import Path
from typing import Dict, List, Optional

import click

from claude_setup import __version__
from claude_setup.assembler import run_pipeline
from claude_setup.config import load_config
from claude_setup.domain.validator import validate_stack
from claude_setup.exceptions import ConfigValidationError, PipelineError
from claude_setup.interactive import run_interactive
from claude_setup.models import PipelineResult, ProjectConfig
from claude_setup.utils import find_resources_dir, setup_logging


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
@click.option("--resources-dir", "-s", type=click.Path(exists=True), default=None, help="Resources templates directory.")
@click.option("--verbose", "-v", is_flag=True, default=False, help="Enable verbose logging.")
@click.option("--dry-run", is_flag=True, default=False, help="Show what would be generated without writing.")
def generate(
    config: Optional[str],
    interactive: bool,
    output_dir: str,
    resources_dir: Optional[str],
    verbose: bool,
    dry_run: bool,
) -> None:
    """Generate project scaffolding from config or interactive mode."""
    _validate_generate_options(config, interactive)
    if verbose:
        setup_logging(verbose=True)
    project_config = _load_project_config(config, interactive)
    resolved_resources = _resolve_resources_dir(resources_dir)
    resolved_output = Path(output_dir)
    result = _execute_generate(
        project_config, resolved_resources, resolved_output, dry_run,
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


def _resolve_resources_dir(resources_dir: Optional[str]) -> Path:
    """Resolve source directory from option or auto-detect."""
    if resources_dir:
        return Path(resources_dir)
    try:
        return find_resources_dir()
    except FileNotFoundError as exc:
        raise click.ClickException(str(exc)) from exc


def _execute_generate(
    project_config: ProjectConfig,
    resources_dir: Path,
    output_dir: Path,
    dry_run: bool,
) -> PipelineResult:
    """Run the pipeline with error handling."""
    try:
        return run_pipeline(
            project_config, resources_dir, output_dir, dry_run=dry_run,
        )
    except PipelineError as exc:
        raise click.ClickException(str(exc)) from exc


def _display_result(result: PipelineResult) -> None:
    """Display pipeline result summary."""
    if not result.success:
        raise click.ClickException("Pipeline failed")
    click.echo(f"Pipeline: Success ({result.duration_ms}ms)")
    click.echo()
    counts = _classify_files(result.files_generated)
    _display_summary_table(counts)
    click.echo(f"Output: {result.output_dir}")
    for warning in result.warnings:
        click.echo(f"Warning: {warning}")


def _classify_files(files: List[Path]) -> Dict[str, int]:
    """Classify generated files by component."""
    counts: Dict[str, int] = {
        "Rules": 0,
        "Skills": 0,
        "Knowledge Packs": 0,
        "Agents": 0,
        "Hooks": 0,
        "Settings": 0,
        "README": 0,
        "GitHub": 0,
    }
    for file_path in files:
        parts = file_path.parts
        name = file_path.name
        if "github" in parts:
            counts["GitHub"] += 1
        elif "README" in name:
            counts["README"] += 1
        elif "settings" in name:
            counts["Settings"] += 1
        elif "hooks" in parts:
            counts["Hooks"] += 1
        elif "agents" in parts:
            counts["Agents"] += 1
        elif "skills" in parts:
            if _is_knowledge_pack_file(file_path):
                counts["Knowledge Packs"] += 1
            else:
                counts["Skills"] += 1
        elif "rules" in parts:
            counts["Rules"] += 1
    return counts


def _is_knowledge_pack_file(file_path: Path) -> bool:
    """Check if a skill file belongs to a knowledge pack."""
    if not file_path.is_file():
        return False
    skill_md = file_path
    if file_path.name != "SKILL.md":
        skill_dir = file_path.parent
        candidate = skill_dir / "SKILL.md"
        if candidate.is_file():
            skill_md = candidate
        else:
            return False
    text = skill_md.read_text(encoding="utf-8")
    if "user-invocable: false" in text:
        return True
    return text.lstrip().startswith("# Knowledge Pack")


def _display_summary_table(counts: Dict[str, int]) -> None:
    """Display formatted summary table."""
    total = sum(counts.values())
    label_width = max(len(label) for label in counts)
    header_label = "Component"
    header_count = "Files"
    label_width = max(label_width, len(header_label))
    click.echo(f"  {header_label:<{label_width}}  {header_count}")
    separator = "\u2500" * label_width
    click.echo(f"  {separator}  {'─' * len(header_count)}")
    for label, count in counts.items():
        if count > 0:
            click.echo(f"  {label:<{label_width}}  {count:>{len(header_count)}}")
    click.echo(f"  {separator}  {'─' * len(header_count)}")
    click.echo(f"  {'Total':<{label_width}}  {total:>{len(header_count)}}")
    click.echo()


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
