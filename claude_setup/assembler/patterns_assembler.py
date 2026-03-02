from __future__ import annotations

from pathlib import Path
from typing import List

from claude_setup.domain.pattern_mapping import (
    select_pattern_files,
    select_patterns,
)
from claude_setup.models import ProjectConfig
from claude_setup.template_engine import TemplateEngine

PATTERNS_SKILL_DIR = "patterns"
REFERENCES_DIR = "references"
SKILLS_DIR = "skills"
CONSOLIDATED_FILENAME = "SKILL.md"
SECTION_SEPARATOR = "\n\n---\n\n"


class PatternsAssembler:
    """Assembles pattern knowledge packs from source templates."""

    def __init__(self, src_dir: Path) -> None:
        self._src_dir = src_dir

    def assemble(
        self,
        config: ProjectConfig,
        output_dir: Path,
        engine: TemplateEngine,
    ) -> List[Path]:
        """Orchestrate pattern selection, flush, and consolidation."""
        categories = select_patterns(config)
        if not categories:
            return []
        pattern_files = select_pattern_files(
            self._src_dir, categories,
        )
        if not pattern_files:
            return []
        return self._generate_output(
            pattern_files, output_dir, engine,
        )

    def _generate_output(
        self,
        pattern_files: List[Path],
        output_dir: Path,
        engine: TemplateEngine,
    ) -> List[Path]:
        """Generate individual and consolidated output files."""
        rendered = self._render_contents(pattern_files, engine)
        results: List[Path] = []
        refs_dir = self._build_refs_dir(output_dir)
        results.extend(
            self._flush_patterns(pattern_files, rendered, refs_dir),
        )
        consolidated = self._build_consolidated_path(output_dir)
        results.append(
            self._flush_consolidated(rendered, consolidated),
        )
        return results

    def _render_contents(
        self,
        pattern_files: List[Path],
        engine: TemplateEngine,
    ) -> List[str]:
        """Read and render all pattern files once."""
        rendered: List[str] = []
        for src_file in pattern_files:
            content = src_file.read_text(encoding="utf-8")
            rendered.append(engine.replace_placeholders(content))
        return rendered

    def _build_refs_dir(self, output_dir: Path) -> Path:
        """Build the references directory path."""
        return (
            output_dir / SKILLS_DIR / PATTERNS_SKILL_DIR
            / REFERENCES_DIR
        )

    def _build_consolidated_path(self, output_dir: Path) -> Path:
        """Build the consolidated file path."""
        return (
            output_dir / SKILLS_DIR / PATTERNS_SKILL_DIR
            / CONSOLIDATED_FILENAME
        )

    def _flush_patterns(
        self,
        pattern_files: List[Path],
        rendered: List[str],
        dest_dir: Path,
    ) -> List[Path]:
        """Write pre-rendered pattern files to destination."""
        results: List[Path] = []
        for src_file, content in zip(pattern_files, rendered):
            category = src_file.parent.name
            target_dir = dest_dir / category
            target_dir.mkdir(parents=True, exist_ok=True)
            dest_file = target_dir / src_file.name
            dest_file.write_text(content, encoding="utf-8")
            results.append(dest_file)
        return results

    def _flush_consolidated(
        self,
        rendered: List[str],
        dest_path: Path,
    ) -> Path:
        """Merge pre-rendered content into a single consolidated file."""
        dest_path.parent.mkdir(parents=True, exist_ok=True)
        merged = SECTION_SEPARATOR.join(rendered)
        dest_path.write_text(merged, encoding="utf-8")
        return dest_path
