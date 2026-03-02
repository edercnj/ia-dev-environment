from __future__ import annotations

import logging
from pathlib import Path
from typing import List, Optional, Tuple

from claude_setup.assembler.conditions import (
    has_any_interface,
    has_interface,
)
from claude_setup.assembler.copy_helpers import (
    copy_template_file,
    copy_template_file_if_exists,
)
from claude_setup.models import ProjectConfig
from claude_setup.template_engine import TemplateEngine

logger = logging.getLogger(__name__)

AGENTS_TEMPLATES_DIR = "agents-templates"
CORE_DIR = "core"
CONDITIONAL_DIR = "conditional"
DEVELOPERS_DIR = "developers"
CHECKLISTS_DIR = "checklists"
AGENTS_OUTPUT = "agents"
MD_EXTENSION = ".md"


class AgentsAssembler:
    """Assembles agent files from templates based on project config."""

    def select_core_agents(self, src_dir: Path) -> List[str]:
        """Scan core agents directory for .md files."""
        core_path = src_dir / AGENTS_TEMPLATES_DIR / CORE_DIR
        if not core_path.exists():
            return []
        return sorted(
            f.name for f in core_path.iterdir()
            if f.is_file() and f.suffix == MD_EXTENSION
        )

    def select_conditional_agents(
        self, config: ProjectConfig,
    ) -> List[str]:
        """Evaluate feature gates and return conditional agents."""
        agents: List[str] = []
        agents.extend(self._select_data_agents(config))
        agents.extend(self._select_infra_agents(config))
        agents.extend(self._select_interface_agents(config))
        agents.extend(self._select_event_agents(config))
        return agents

    def _select_data_agents(
        self, config: ProjectConfig,
    ) -> List[str]:
        """Select agents based on data configuration."""
        if config.data.database.name != "none":
            return ["database-engineer.md"]
        return []

    def _select_infra_agents(
        self, config: ProjectConfig,
    ) -> List[str]:
        """Select agents based on infrastructure configuration."""
        agents: List[str] = []
        infra = config.infrastructure
        if infra.observability.tool != "none":
            agents.append("observability-engineer.md")
        has_devops = (
            infra.container != "none"
            or infra.orchestrator != "none"
            or infra.iac != "none"
            or infra.service_mesh != "none"
        )
        if has_devops:
            agents.append("devops-engineer.md")
        return agents

    def _select_interface_agents(
        self, config: ProjectConfig,
    ) -> List[str]:
        """Select agents based on interface types."""
        if has_any_interface(config, "rest", "grpc", "graphql"):
            return ["api-engineer.md"]
        return []

    def _select_event_agents(
        self, config: ProjectConfig,
    ) -> List[str]:
        """Select agents based on event configuration."""
        has_events = (
            config.architecture.event_driven
            or has_any_interface(config, "event-consumer", "event-producer")
        )
        if has_events:
            return ["event-engineer.md"]
        return []

    def select_developer_agent(
        self, config: ProjectConfig,
    ) -> str:
        """Return developer agent filename for the language."""
        safe_name = Path(config.language.name).name
        return f"{safe_name}-developer.md"

    def _copy_core_agent(
        self, agent_file: str, src_dir: Path,
        output_dir: Path, engine: TemplateEngine,
    ) -> Path:
        """Copy a single core agent file with placeholders."""
        src = src_dir / AGENTS_TEMPLATES_DIR / CORE_DIR / agent_file
        dest = output_dir / AGENTS_OUTPUT / agent_file
        return copy_template_file(src, dest, engine)

    def _copy_conditional_agent(
        self, agent_file: str, src_dir: Path,
        output_dir: Path, engine: TemplateEngine,
    ) -> Optional[Path]:
        """Copy a conditional agent if source exists."""
        src = src_dir / AGENTS_TEMPLATES_DIR / CONDITIONAL_DIR / agent_file
        dest = output_dir / AGENTS_OUTPUT / agent_file
        return copy_template_file_if_exists(src, dest, engine)

    def _copy_developer_agent(
        self, config: ProjectConfig, src_dir: Path,
        output_dir: Path, engine: TemplateEngine,
    ) -> Optional[Path]:
        """Copy the language-specific developer agent."""
        agent_file = self.select_developer_agent(config)
        src = src_dir / AGENTS_TEMPLATES_DIR / DEVELOPERS_DIR / agent_file
        dest = output_dir / AGENTS_OUTPUT / agent_file
        return copy_template_file_if_exists(src, dest, engine)

    def _inject_checklists(
        self, config: ProjectConfig, output_dir: Path,
        src_dir: Path, engine: TemplateEngine,
    ) -> None:
        """Inject conditional checklists into agent files."""
        for agent_file, checklist_file, condition in self._build_checklist_rules(config):
            if not condition:
                continue
            self._inject_single_checklist(
                agent_file, checklist_file, output_dir, src_dir, engine,
            )

    def _build_checklist_rules(
        self, config: ProjectConfig,
    ) -> List[Tuple[str, str, bool]]:
        """Build (agent, checklist, condition) tuples."""
        frameworks = config.security.frameworks
        infra = config.infrastructure
        return [
            *self._security_checklist_rules(frameworks),
            *self._api_checklist_rules(config),
            *self._devops_checklist_rules(infra),
        ]

    def _security_checklist_rules(
        self, frameworks: List[str],
    ) -> List[Tuple[str, str, bool]]:
        """Build security checklist injection rules."""
        has_privacy = "lgpd" in frameworks or "gdpr" in frameworks
        return [
            ("security-engineer.md", "pci-dss-security.md", "pci-dss" in frameworks),
            ("security-engineer.md", "privacy-security.md", has_privacy),
            ("security-engineer.md", "hipaa-security.md", "hipaa" in frameworks),
            ("security-engineer.md", "sox-security.md", "sox" in frameworks),
        ]

    def _api_checklist_rules(
        self, config: ProjectConfig,
    ) -> List[Tuple[str, str, bool]]:
        """Build API checklist injection rules."""
        return [
            ("api-engineer.md", "grpc-api.md", has_interface(config, "grpc")),
            ("api-engineer.md", "graphql-api.md", has_interface(config, "graphql")),
            ("api-engineer.md", "websocket-api.md", has_interface(config, "websocket")),
        ]

    def _devops_checklist_rules(
        self, infra,
    ) -> List[Tuple[str, str, bool]]:
        """Build DevOps checklist injection rules."""
        return [
            ("devops-engineer.md", "helm-devops.md", infra.templating == "helm"),
            ("devops-engineer.md", "iac-devops.md", infra.iac != "none"),
            ("devops-engineer.md", "mesh-devops.md", infra.service_mesh != "none"),
            ("devops-engineer.md", "registry-devops.md", infra.registry != "none"),
        ]

    def _inject_single_checklist(
        self, agent_file: str, checklist_file: str,
        output_dir: Path, src_dir: Path, engine: TemplateEngine,
    ) -> None:
        """Inject a single checklist into an agent file."""
        agent_path = output_dir / AGENTS_OUTPUT / agent_file
        if not agent_path.exists():
            return
        checklist_src = (
            src_dir / AGENTS_TEMPLATES_DIR / CHECKLISTS_DIR / checklist_file
        )
        if not checklist_src.exists():
            return
        marker = self._checklist_marker(checklist_file)
        section = checklist_src.read_text(encoding="utf-8")
        base = agent_path.read_text(encoding="utf-8")
        result = TemplateEngine.inject_section(base, section, marker)
        agent_path.write_text(result, encoding="utf-8")

    def _checklist_marker(self, checklist_file: str) -> str:
        """Build marker string from checklist filename."""
        name = checklist_file.replace(MD_EXTENSION, "").upper()
        name = name.replace("-", "_")
        return f"<!-- {name} -->"

    def assemble(
        self, config: ProjectConfig, output_dir: Path,
        src_dir: Path, engine: TemplateEngine,
    ) -> List[Path]:
        """Main entry point: assemble all agents."""
        logger.info("Assembling agents for project '%s'", config.project.name)
        results: List[Path] = []
        core = self._assemble_core(src_dir, output_dir, engine)
        results.extend(core)
        logger.debug("Assembled %d core agents", len(core))
        conditional = self._assemble_conditional(
            config, src_dir, output_dir, engine,
        )
        results.extend(conditional)
        logger.debug("Assembled %d conditional agents", len(conditional))
        dev = self._copy_developer_agent(
            config, src_dir, output_dir, engine,
        )
        if dev is not None:
            results.append(dev)
            logger.debug("Developer agent: %s", dev.name)
        self._inject_checklists(config, output_dir, src_dir, engine)
        logger.info("Agents assembly complete: %d total artifacts", len(results))
        return results

    def _assemble_core(
        self, src_dir: Path, output_dir: Path, engine: TemplateEngine,
    ) -> List[Path]:
        """Copy all core agents."""
        return [
            self._copy_core_agent(agent, src_dir, output_dir, engine)
            for agent in self.select_core_agents(src_dir)
        ]

    def _assemble_conditional(
        self, config: ProjectConfig, src_dir: Path,
        output_dir: Path, engine: TemplateEngine,
    ) -> List[Path]:
        """Copy all conditional agents."""
        results: List[Path] = []
        for agent in self.select_conditional_agents(config):
            path = self._copy_conditional_agent(
                agent, src_dir, output_dir, engine,
            )
            if path is not None:
                results.append(path)
        return results
