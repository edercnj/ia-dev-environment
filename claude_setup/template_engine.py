from __future__ import annotations

import re
from pathlib import Path
from typing import Any, Dict, List, Optional

from jinja2 import FileSystemLoader, StrictUndefined
from jinja2.sandbox import SandboxedEnvironment

from claude_setup.models import ProjectConfig

PLACEHOLDER_PATTERN = re.compile(r"\{(\w+)\}")


def _build_default_context(config: ProjectConfig) -> Dict[str, Any]:
    """Extract flat key-value pairs from ProjectConfig."""
    return {
        "project_name": config.project.name,
        "project_purpose": config.project.purpose,
        "language_name": config.language.name,
        "language_version": config.language.version,
        "framework_name": config.framework.name,
        "framework_version": config.framework.version,
        "build_tool": config.framework.build_tool,
        "architecture_style": config.architecture.style,
        "domain_driven": config.architecture.domain_driven,
        "event_driven": config.architecture.event_driven,
        "container": config.infrastructure.container,
        "orchestrator": config.infrastructure.orchestrator,
        "database_name": config.data.database.name,
        "cache_name": config.data.cache.name,
        "coverage_line": config.testing.coverage_line,
        "coverage_branch": config.testing.coverage_branch,
    }


def _build_placeholder_map(
    config: ProjectConfig,
) -> Dict[str, str]:
    """Build string mapping for legacy placeholder replacement."""
    context = _build_default_context(config)
    return {k: str(v) for k, v in context.items()}


class TemplateEngine:
    """Jinja2-based template rendering engine."""

    def __init__(
        self,
        src_dir: Path,
        config: ProjectConfig,
    ) -> None:
        self._config = config
        self._env = SandboxedEnvironment(
            loader=FileSystemLoader(str(src_dir)),
            autoescape=False,
            keep_trailing_newline=True,
            trim_blocks=False,
            lstrip_blocks=False,
            undefined=StrictUndefined,
        )
        self._default_context = _build_default_context(config)

    def _merge_context(
        self,
        context: Optional[Dict[str, Any]],
    ) -> Dict[str, Any]:
        """Merge default context with provided overrides."""
        merged = dict(self._default_context)
        if context:
            merged.update(context)
        return merged

    def render_template(
        self,
        template_path: Path,
        context: Optional[Dict[str, Any]] = None,
    ) -> str:
        """Load and render a Jinja2 template file."""
        template = self._env.get_template(str(template_path))
        merged = self._merge_context(context)
        return template.render(merged)

    def render_string(
        self,
        template_str: str,
        context: Optional[Dict[str, Any]] = None,
    ) -> str:
        """Render an inline Jinja2 template string."""
        template = self._env.from_string(template_str)
        merged = self._merge_context(context)
        return template.render(merged)

    def replace_placeholders(
        self,
        content: str,
        config: ProjectConfig,
    ) -> str:
        """Replace legacy {placeholder} patterns with config values."""
        mapping = _build_placeholder_map(config)

        def _replacer(match: re.Match) -> str:
            key = match.group(1)
            if key in mapping:
                return mapping[key]
            return match.group(0)

        return PLACEHOLDER_PATTERN.sub(_replacer, content)

    @staticmethod
    def inject_section(
        base_content: str,
        section: str,
        marker: str,
    ) -> str:
        """Replace marker in base_content with section content."""
        return base_content.replace(marker, section)

    @staticmethod
    def concat_files(
        paths: List[Path],
        separator: str = "\n",
    ) -> str:
        """Read and concatenate files with separator."""
        if not paths:
            return ""
        contents: List[str] = []
        for path in paths:
            contents.append(path.read_text())
        return separator.join(contents)
