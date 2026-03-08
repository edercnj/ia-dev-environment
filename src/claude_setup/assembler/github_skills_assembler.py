from __future__ import annotations

import logging
from pathlib import Path
from typing import List

from claude_setup.models import ProjectConfig
from claude_setup.template_engine import TemplateEngine

logger = logging.getLogger(__name__)

# Map: (template_subdir, [template_names])
SKILL_GROUPS = {
    "story": (
        "x-story-epic",
        "x-story-create",
        "x-story-map",
        "x-story-epic-full",
        "story-planning",
    ),
    "dev": (
        "x-dev-implement",
        "x-dev-lifecycle",
        "layer-templates",
    ),
    "review": (
        "x-review",
        "x-review-api",
        "x-review-pr",
        "x-review-grpc",
        "x-review-events",
        "x-review-gateway",
    ),
    "testing": (
        "x-test-plan",
        "x-test-run",
        "run-e2e",
        "run-smoke-api",
        "run-contract-tests",
        "run-perf-test",
    ),
    "infrastructure": (
        "setup-environment",
        "k8s-deployment",
        "k8s-kustomize",
        "dockerfile",
        "iac-terraform",
    ),
}


class GithubSkillsAssembler:
    """Generates .github/skills/ from templates."""

    def __init__(self, resources_dir: Path) -> None:
        self._resources_dir = resources_dir

    def assemble(
        self,
        config: ProjectConfig,
        output_dir: Path,
        engine: TemplateEngine,
    ) -> List[Path]:
        """Generate skill files from all registered groups."""
        generated: List[Path] = []
        for group, skill_names in SKILL_GROUPS.items():
            generated.extend(
                self._generate_group(
                    engine, output_dir, group, skill_names,
                ),
            )
        return generated

    def _generate_group(
        self,
        engine: TemplateEngine,
        output_dir: Path,
        group: str,
        skill_names: tuple,
    ) -> List[Path]:
        """Generate skills for a single group."""
        templates_dir = (
            self._resources_dir
            / "github-skills-templates"
            / group
        )
        if not templates_dir.is_dir():
            logger.warning(
                "Templates dir not found: %s", templates_dir,
            )
            return []
        generated: List[Path] = []
        for name in skill_names:
            template = templates_dir / f"{name}.md"
            if not template.is_file():
                logger.warning(
                    "Template not found: %s", template,
                )
                continue
            content = template.read_text(encoding="utf-8")
            content = engine.replace_placeholders(content)
            skill_dir = (
                output_dir / "github" / "skills" / name
            )
            skill_dir.mkdir(parents=True, exist_ok=True)
            dest = skill_dir / "SKILL.md"
            dest.write_text(content, encoding="utf-8")
            generated.append(dest)
        return generated
