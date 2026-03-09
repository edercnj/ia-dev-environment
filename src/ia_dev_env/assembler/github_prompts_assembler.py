from __future__ import annotations

import logging
from pathlib import Path
from typing import List

from ia_dev_env.models import ProjectConfig
from ia_dev_env.template_engine import TemplateEngine

logger = logging.getLogger(__name__)

TEMPLATES_DIR_NAME = "github-prompts-templates"

PROMPT_TEMPLATES = (
    "new-feature.prompt.md.j2",
    "decompose-spec.prompt.md.j2",
    "code-review.prompt.md.j2",
    "troubleshoot.prompt.md.j2",
)


class GithubPromptsAssembler:
    """Generates .github/prompts/*.prompt.md from Jinja2 templates."""

    def __init__(self, resources_dir: Path) -> None:
        self._resources_dir = resources_dir

    def assemble(
        self,
        config: ProjectConfig,
        output_dir: Path,
        engine: TemplateEngine,
    ) -> List[Path]:
        """Render prompt templates and write to output."""
        templates_dir = (
            self._resources_dir / TEMPLATES_DIR_NAME
        )
        if not templates_dir.is_dir():
            logger.warning(
                "Templates dir not found: %s",
                templates_dir,
            )
            return []
        prompts_dir = output_dir / "github" / "prompts"
        prompts_dir.mkdir(parents=True, exist_ok=True)
        generated: List[Path] = []
        for template_name in PROMPT_TEMPLATES:
            src = templates_dir / template_name
            if not src.is_file():
                logger.warning(
                    "Prompt template not found: %s", src,
                )
                continue
            output_name = template_name.removesuffix(".j2")
            content = engine.render_template(
                Path(TEMPLATES_DIR_NAME) / template_name,
            )
            dest = prompts_dir / output_name
            dest.write_text(content, encoding="utf-8")
            generated.append(dest)
        return generated
