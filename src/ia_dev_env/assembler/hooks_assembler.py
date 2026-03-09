from __future__ import annotations

import logging
import shutil
from pathlib import Path
from typing import List

from ia_dev_env.domain.stack_mapping import get_hook_template_key
from ia_dev_env.models import ProjectConfig
from ia_dev_env.template_engine import TemplateEngine

logger = logging.getLogger(__name__)


class HooksAssembler:
    """Copies post-compile-check.sh for compiled languages."""

    def __init__(self, resources_dir: Path) -> None:
        self._resources_dir = resources_dir

    def assemble(
        self,
        config: ProjectConfig,
        output_dir: Path,
        engine: TemplateEngine,
    ) -> List[Path]:
        """Copy hook scripts for compiled languages."""
        key = get_hook_template_key(
            config.language.name,
            config.framework.build_tool,
        )
        if not key:
            logger.info("No compile hook for %s, skipping.", config.language.name)
            return []
        hook_src = self._resources_dir / "hooks-templates" / key / "post-compile-check.sh"
        if not hook_src.is_file():
            logger.warning("Hook template not found: %s", hook_src)
            return []
        return self._copy_hook(hook_src, output_dir)

    def _copy_hook(self, hook_src: Path, output_dir: Path) -> List[Path]:
        hooks_dir = output_dir / "hooks"
        hooks_dir.mkdir(parents=True, exist_ok=True)
        dest = hooks_dir / "post-compile-check.sh"
        shutil.copy2(str(hook_src), str(dest))
        dest.chmod(dest.stat().st_mode | 0o111)
        logger.info("Copied %s", dest)
        return [dest]
