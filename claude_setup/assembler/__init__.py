from __future__ import annotations

import logging
import tempfile
import time
from pathlib import Path
from typing import List, Tuple

from claude_setup.assembler.agents import AgentsAssembler
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


def _build_assemblers(src_dir: Path) -> List[Tuple[str, object]]:
    """Build ordered list of (name, assembler) tuples."""
    return [
        ("RulesAssembler", RulesAssembler()),
        ("SkillsAssembler", SkillsAssembler()),
        ("AgentsAssembler", AgentsAssembler()),
        ("PatternsAssembler", PatternsAssembler(src_dir)),
        ("ProtocolsAssembler", ProtocolsAssembler(src_dir)),
        ("HooksAssembler", HooksAssembler(src_dir)),
        ("SettingsAssembler", SettingsAssembler(src_dir)),
        ("ReadmeAssembler", ReadmeAssembler(src_dir)),
    ]


def _call_assembler(
    name: str,
    assembler: object,
    config: ProjectConfig,
    src_dir: Path,
    output_dir: Path,
    engine: TemplateEngine,
) -> List[Path]:
    """Call assembler.assemble() with the correct signature."""
    if name in ("SkillsAssembler", "AgentsAssembler"):
        return assembler.assemble(config, output_dir, src_dir, engine)
    return assembler.assemble(config, output_dir, engine)


def _execute_assemblers(
    config: ProjectConfig,
    src_dir: Path,
    output_dir: Path,
    engine: TemplateEngine,
) -> Tuple[List[Path], List[str]]:
    """Run all assemblers in order, collecting results."""
    files: List[Path] = []
    warnings: List[str] = []
    for name, assembler in _build_assemblers(src_dir):
        try:
            result = _call_assembler(
                name, assembler, config, src_dir, output_dir, engine,
            )
            files.extend(result)
        except Exception as exc:
            logger.debug("Assembler %s failed: %s", name, exc)
            raise PipelineError(name, type(exc).__name__) from exc
    return files, warnings


def _run_in_temp(
    config: ProjectConfig,
    src_dir: Path,
) -> Tuple[List[Path], List[str]]:
    """Execute pipeline in a temporary directory."""
    temp_dir = Path(tempfile.mkdtemp(prefix="claude-setup-dry-"))
    try:
        engine = TemplateEngine(src_dir, config)
        files, warnings = _execute_assemblers(
            config, src_dir, temp_dir, engine,
        )
        return files, warnings
    finally:
        import shutil
        if temp_dir.exists():
            shutil.rmtree(str(temp_dir))


def _compute_duration_ms(start: float, end: float) -> int:
    """Convert monotonic time delta to milliseconds."""
    return int((end - start) * MILLISECONDS_PER_SECOND)


def run_pipeline(
    config: ProjectConfig,
    src_dir: Path,
    output_dir: Path,
    dry_run: bool = False,
) -> PipelineResult:
    """Orchestrate all assemblers with atomic output."""
    start = time.monotonic()
    if dry_run:
        return _run_dry(config, src_dir, output_dir, start)
    return _run_real(config, src_dir, output_dir, start)


def _run_dry(
    config: ProjectConfig,
    src_dir: Path,
    output_dir: Path,
    start: float,
) -> PipelineResult:
    """Execute dry-run: run in temp, discard output."""
    files, warnings = _run_in_temp(config, src_dir)
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
    src_dir: Path,
    output_dir: Path,
    start: float,
) -> PipelineResult:
    """Execute pipeline with atomic output to dest."""
    with atomic_output(output_dir) as temp_dir:
        engine = TemplateEngine(src_dir, config)
        files, warnings = _execute_assemblers(
            config, src_dir, temp_dir, engine,
        )
    duration = _compute_duration_ms(start, time.monotonic())
    return PipelineResult(
        success=True,
        output_dir=output_dir,
        files_generated=files,
        warnings=warnings,
        duration_ms=duration,
    )
