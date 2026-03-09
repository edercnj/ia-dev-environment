from __future__ import annotations

from pathlib import Path
from typing import Dict, List

from ia_dev_env.domain.protocol_mapping import (
    derive_protocol_files,
    derive_protocols,
)
from ia_dev_env.models import ProjectConfig
from ia_dev_env.template_engine import TemplateEngine

PROTOCOLS_SKILL_DIR = "protocols"
REFERENCES_DIR = "references"
SKILLS_DIR = "skills"
PROTOCOL_SEPARATOR = "\n\n---\n\n"
CONVENTIONS_SUFFIX = "-conventions.md"


class ProtocolsAssembler:
    """Assembles protocol knowledge packs from source templates."""

    def __init__(self, resources_dir: Path) -> None:
        self._resources_dir = resources_dir

    def assemble(
        self,
        config: ProjectConfig,
        output_dir: Path,
        engine: TemplateEngine,
    ) -> List[Path]:
        """Orchestrate protocol derivation and concatenation."""
        protocol_names = derive_protocols(config)
        if not protocol_names:
            return []
        protocol_files = derive_protocol_files(
            self._resources_dir, protocol_names, config,
        )
        if not protocol_files:
            return []
        return self._generate_output(
            protocol_files, output_dir,
        )

    def _generate_output(
        self,
        protocol_files: Dict[str, List[Path]],
        output_dir: Path,
    ) -> List[Path]:
        """Generate concatenated protocol files."""
        refs_dir = self._build_refs_dir(output_dir)
        refs_dir.mkdir(parents=True, exist_ok=True)
        results: List[Path] = []
        for protocol, files in sorted(protocol_files.items()):
            dest_name = protocol + CONVENTIONS_SUFFIX
            dest_path = refs_dir / dest_name
            results.append(
                self._concat_protocol_dir(files, dest_path),
            )
        return results

    def _build_refs_dir(self, output_dir: Path) -> Path:
        """Build the references directory path."""
        return (
            output_dir / SKILLS_DIR / PROTOCOLS_SKILL_DIR
            / REFERENCES_DIR
        )

    def _concat_protocol_dir(
        self,
        protocol_files: List[Path],
        dest_path: Path,
    ) -> Path:
        """Concatenate protocol files with separator."""
        if not protocol_files:
            dest_path.write_text("", encoding="utf-8")
            return dest_path
        sections: List[str] = []
        for f in protocol_files:
            sections.append(f.read_text(encoding="utf-8"))
        merged = PROTOCOL_SEPARATOR.join(sections)
        dest_path.write_text(merged, encoding="utf-8")
        return dest_path
