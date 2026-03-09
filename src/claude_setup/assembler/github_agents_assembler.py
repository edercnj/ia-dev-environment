from __future__ import annotations

import logging
from pathlib import Path
from typing import List, Optional

from claude_setup.assembler.conditions import (
    has_any_interface,
)
from claude_setup.models import ProjectConfig
from claude_setup.template_engine import TemplateEngine

logger = logging.getLogger(__name__)

TEMPLATES_DIR_NAME = "github-agents-templates"
CORE_DIR = "core"
CONDITIONAL_DIR = "conditional"
DEVELOPERS_DIR = "developers"
AGENT_MD_EXTENSION = ".agent.md"


class GithubAgentsAssembler:
    """Generates .github/agents/*.agent.md from templates."""

    def __init__(self, resources_dir: Path) -> None:
        self._resources_dir = resources_dir

    def assemble(
        self,
        config: ProjectConfig,
        output_dir: Path,
        engine: TemplateEngine,
    ) -> List[Path]:
        """Generate agent files for GitHub Copilot."""
        agents_dir = output_dir / "github" / "agents"
        agents_dir.mkdir(parents=True, exist_ok=True)
        generated: List[Path] = []
        generated.extend(
            self._assemble_core(agents_dir, engine),
        )
        generated.extend(
            self._assemble_conditional(
                config, agents_dir, engine,
            ),
        )
        dev = self._assemble_developer(
            config, agents_dir, engine,
        )
        if dev is not None:
            generated.append(dev)
        return generated

    def _assemble_core(
        self,
        agents_dir: Path,
        engine: TemplateEngine,
    ) -> List[Path]:
        """Generate all core agents."""
        core_dir = (
            self._resources_dir / TEMPLATES_DIR_NAME / CORE_DIR
        )
        if not core_dir.is_dir():
            logger.warning(
                "Core templates dir not found: %s", core_dir,
            )
            return []
        generated: List[Path] = []
        for template in sorted(core_dir.iterdir()):
            if not template.is_file():
                continue
            path = self._render_agent(
                template, agents_dir, engine,
            )
            generated.append(path)
        return generated

    def _assemble_conditional(
        self,
        config: ProjectConfig,
        agents_dir: Path,
        engine: TemplateEngine,
    ) -> List[Path]:
        """Generate conditional agents based on config."""
        conditional_dir = (
            self._resources_dir
            / TEMPLATES_DIR_NAME
            / CONDITIONAL_DIR
        )
        if not conditional_dir.is_dir():
            return []
        generated: List[Path] = []
        for name in self._select_conditional(config):
            template = conditional_dir / name
            if not template.is_file():
                logger.warning(
                    "Conditional template not found: %s",
                    template,
                )
                continue
            path = self._render_agent(
                template, agents_dir, engine,
            )
            generated.append(path)
        return generated

    def _assemble_developer(
        self,
        config: ProjectConfig,
        agents_dir: Path,
        engine: TemplateEngine,
    ) -> Optional[Path]:
        """Generate the language-specific developer agent."""
        dev_dir = (
            self._resources_dir
            / TEMPLATES_DIR_NAME
            / DEVELOPERS_DIR
        )
        if not dev_dir.is_dir():
            return None
        safe_name = Path(config.language.name).name
        template = dev_dir / f"{safe_name}-developer.md"
        if not template.is_file():
            logger.warning(
                "Developer template not found: %s", template,
            )
            return None
        return self._render_agent(
            template, agents_dir, engine,
        )

    def _render_agent(
        self,
        template: Path,
        agents_dir: Path,
        engine: TemplateEngine,
    ) -> Path:
        """Read template, replace placeholders, write output."""
        content = template.read_text(encoding="utf-8")
        content = engine.replace_placeholders(content)
        agent_name = template.stem
        dest = agents_dir / f"{agent_name}{AGENT_MD_EXTENSION}"
        dest.write_text(content, encoding="utf-8")
        return dest

    @staticmethod
    def _select_conditional(
        config: ProjectConfig,
    ) -> List[str]:
        """Return conditional agent filenames for config."""
        agents: List[str] = []
        agents.extend(
            _select_infra_agents(config),
        )
        agents.extend(
            _select_interface_agents(config),
        )
        agents.extend(
            _select_event_agents(config),
        )
        return agents


def _select_infra_agents(
    config: ProjectConfig,
) -> List[str]:
    """Select agents based on infrastructure config."""
    infra = config.infrastructure
    has_devops = (
        infra.container != "none"
        or infra.orchestrator != "none"
        or infra.iac != "none"
        or infra.service_mesh != "none"
    )
    if has_devops:
        return ["devops-engineer.md"]
    return []


def _select_interface_agents(
    config: ProjectConfig,
) -> List[str]:
    """Select agents based on interface types."""
    if has_any_interface(config, "rest", "grpc", "graphql"):
        return ["api-engineer.md"]
    return []


def _select_event_agents(
    config: ProjectConfig,
) -> List[str]:
    """Select agents based on event config."""
    has_events = (
        config.architecture.event_driven
        or has_any_interface(
            config, "event-consumer", "event-producer",
        )
    )
    if has_events:
        return ["event-engineer.md"]
    return []
