from __future__ import annotations

import logging
import shutil
import tempfile
import time
from pathlib import Path
from typing import List, Tuple

from claude_setup.assembler.agents import AgentsAssembler
from claude_setup.assembler.github_instructions_assembler import GithubInstructionsAssembler
from claude_setup.assembler.github_mcp_assembler import GithubMcpAssembler
from claude_setup.assembler.github_skills_assembler import GithubSkillsAssembler
from claude_setup.assembler.hooks_assembler import HooksAssembler
from claude_setup.assembler.patterns_assembler import PatternsAssembler
from claude_setup.assembler.protocols_assembler import ProtocolsAssembler
from claude_setup.assembler.readme_assembler import ReadmeAssembler
from claude_setup.assembler.rules_assembler import RulesAssembler
from claude_setup.assembler.settings_assembler import SettingsAssembler
from claude_setup.assembler.skills import SkillsAssembler
from claude_setup.exceptions import PipelineError
from claude_setup.models import PipelineResult, ProjectConfig
from claude_setup.template_engine import TemplateEngine
from claude_setup.utils import atomic_output

logger = logging.getLogger(__name__)

MILLISECONDS_PER_SECOND = 1000
DRY_RUN_WARNING = "Dry run -- no files written"

__all__ = [
    "AgentsAssembler",
    "GithubInstructionsAssembler",
    "GithubMcpAssembler",
    "GithubSkillsAssembler",
    "HooksAssembler",
    "PatternsAssembler",
    "PipelineResult",
    "ProtocolsAssembler",
    "ReadmeAssembler",
    "RulesAssembler",
    "SettingsAssembler",
    "SkillsAssembler",
    "run_pipeline",
]


def _build_assemblers(resources_dir: Path) -> List[Tuple[str, object]]:
    """Build ordered list of (name, assembler) tuples."""
    return [
        ("RulesAssembler", RulesAssembler()),
        ("SkillsAssembler", SkillsAssembler()),
        ("AgentsAssembler", AgentsAssembler()),
        ("PatternsAssembler", PatternsAssembler(resources_dir)),
        ("ProtocolsAssembler", ProtocolsAssembler(resources_dir)),
        ("HooksAssembler", HooksAssembler(resources_dir)),
        ("SettingsAssembler", SettingsAssembler(resources_dir)),
        ("ReadmeAssembler", ReadmeAssembler(resources_dir)),
        ("GithubInstructionsAssembler", GithubInstructionsAssembler(resources_dir)),
        ("GithubMcpAssembler", GithubMcpAssembler()),
        ("GithubSkillsAssembler", GithubSkillsAssembler(resources_dir)),
    ]


ASSEMBLERS_WITH_RESOURCES_DIR = ("SkillsAssembler", "AgentsAssembler")


def _execute_assemblers(
    config: ProjectConfig,
    resources_dir: Path,
    output_dir: Path,
    engine: TemplateEngine,
) -> Tuple[List[Path], List[str]]:
    """Run all assemblers in order, collecting results."""
    files: List[Path] = []
    warnings: List[str] = []
    for name, assembler in _build_assemblers(resources_dir):
        try:
            if name in ASSEMBLERS_WITH_RESOURCES_DIR:
                result = assembler.assemble(
                    config, output_dir, resources_dir, engine,
                )
            else:
                result = assembler.assemble(
                    config, output_dir, engine,
                )
            files.extend(result)
        except Exception as exc:
            logger.debug("Assembler %s failed: %s", name, exc)
            raise PipelineError(name, type(exc).__name__) from exc
    return files, warnings


def _run_in_temp(
    config: ProjectConfig,
    resources_dir: Path,
) -> Tuple[List[Path], List[str]]:
    """Execute pipeline in a temporary directory."""
    temp_dir = Path(tempfile.mkdtemp(prefix="claude-setup-dry-"))
    try:
        engine = TemplateEngine(resources_dir, config)
        files, warnings = _execute_assemblers(
            config, resources_dir, temp_dir, engine,
        )
        return files, warnings
    finally:
        if temp_dir.exists():
            shutil.rmtree(str(temp_dir))


def _compute_duration_ms(start: float, end: float) -> int:
    """Convert monotonic time delta to milliseconds."""
    return int((end - start) * MILLISECONDS_PER_SECOND)


def run_pipeline(
    config: ProjectConfig,
    resources_dir: Path,
    output_dir: Path,
    dry_run: bool = False,
) -> PipelineResult:
    """Orchestrate all assemblers with atomic output."""
    start = time.monotonic()
    if dry_run:
        return _run_dry(config, resources_dir, output_dir, start)
    return _run_real(config, resources_dir, output_dir, start)


def _run_dry(
    config: ProjectConfig,
    resources_dir: Path,
    output_dir: Path,
    start: float,
) -> PipelineResult:
    """Execute dry-run: run in temp, discard output."""
    files, warnings = _run_in_temp(config, resources_dir)
    warnings.append(DRY_RUN_WARNING)
    duration = _compute_duration_ms(start, time.monotonic())
    return PipelineResult(
        success=True,
        output_dir=output_dir,
        files_generated=files,
        warnings=warnings,
        duration_ms=duration,
    )


def _run_real(
    config: ProjectConfig,
    resources_dir: Path,
    output_dir: Path,
    start: float,
) -> PipelineResult:
    """Execute pipeline with atomic output to dest."""
    with atomic_output(output_dir) as temp_dir:
        engine = TemplateEngine(resources_dir, config)
        files, warnings = _execute_assemblers(
            config, resources_dir, temp_dir, engine,
        )
    duration = _compute_duration_ms(start, time.monotonic())
    return PipelineResult(
        success=True,
        output_dir=output_dir,
        files_generated=files,
        warnings=warnings,
        duration_ms=duration,
    )
