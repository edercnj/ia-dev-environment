from __future__ import annotations

import logging
import shutil
from pathlib import Path
from typing import List

from claude_setup.models import ProjectConfig
from claude_setup.template_engine import TemplateEngine

logger = logging.getLogger(__name__)

TEMPLATES_DIR_NAME = "github-hooks-templates"

HOOK_TEMPLATES = (
    "post-compile-check.json",
    "pre-commit-lint.json",
    "session-context-loader.json",
)


class GithubHooksAssembler:
    """Generates github/hooks/*.json from templates."""

    def __init__(self, resources_dir: Path) -> None:
        self._resources_dir = resources_dir

    def assemble(
        self,
        config: ProjectConfig,
        output_dir: Path,
        engine: TemplateEngine,
    ) -> List[Path]:
        """Generate hook JSON files for GitHub Copilot."""
        templates_dir = (
            self._resources_dir / TEMPLATES_DIR_NAME
        )
        if not templates_dir.is_dir():
            logger.warning(
                "Templates dir not found: %s",
                templates_dir,
            )
            return []
        hooks_dir = output_dir / "github" / "hooks"
        hooks_dir.mkdir(parents=True, exist_ok=True)
        generated: List[Path] = []
        for template_name in HOOK_TEMPLATES:
            src = templates_dir / template_name
            if not src.is_file():
                logger.warning(
                    "Hook template not found: %s", src,
                )
                continue
            dest = hooks_dir / template_name
            shutil.copy2(str(src), str(dest))
            generated.append(dest)
        return generated
