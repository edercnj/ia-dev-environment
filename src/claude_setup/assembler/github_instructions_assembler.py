from __future__ import annotations

import logging
from pathlib import Path
from typing import List

from claude_setup.models import ProjectConfig
from claude_setup.template_engine import TemplateEngine

logger = logging.getLogger(__name__)

TEMPLATES_DIR_NAME = "github-instructions-templates"

CONTEXTUAL_TEMPLATES = (
    "domain",
    "coding-standards",
    "architecture",
    "quality-gates",
)


class GithubInstructionsAssembler:
    """Generates .github/ instructions adapting .claude/rules/ for Copilot."""

    def __init__(self, resources_dir: Path) -> None:
        self._resources_dir = resources_dir

    def assemble(
        self,
        config: ProjectConfig,
        output_dir: Path,
        engine: TemplateEngine,
    ) -> List[Path]:
        """Generate copilot-instructions.md and instructions/*.instructions.md."""
        github_dir = output_dir / "github"
        instructions_dir = github_dir / "instructions"
        github_dir.mkdir(parents=True, exist_ok=True)
        instructions_dir.mkdir(parents=True, exist_ok=True)
        generated: List[Path] = []
        generated.append(
            self._generate_global(config, github_dir),
        )
        generated.extend(
            self._generate_contextual(engine, instructions_dir),
        )
        return generated

    def _generate_global(
        self,
        config: ProjectConfig,
        github_dir: Path,
    ) -> Path:
        """Generate the global copilot-instructions.md from config."""
        dest = github_dir / "copilot-instructions.md"
        content = _build_copilot_instructions(config)
        dest.write_text(content, encoding="utf-8")
        return dest

    def _generate_contextual(
        self,
        engine: TemplateEngine,
        instructions_dir: Path,
    ) -> List[Path]:
        """Generate contextual *.instructions.md from templates."""
        templates_dir = self._resources_dir / TEMPLATES_DIR_NAME
        if not templates_dir.is_dir():
            logger.warning(
                "Templates dir not found: %s", templates_dir,
            )
            return []
        generated: List[Path] = []
        for name in CONTEXTUAL_TEMPLATES:
            template = templates_dir / f"{name}.md"
            if not template.is_file():
                logger.warning("Template not found: %s", template)
                continue
            content = template.read_text(encoding="utf-8")
            content = engine.replace_placeholders(content)
            dest = instructions_dir / f"{name}.instructions.md"
            dest.write_text(content, encoding="utf-8")
            generated.append(dest)
        return generated


def _build_copilot_instructions(config: ProjectConfig) -> str:
    """Build the global copilot-instructions.md content."""
    ifaces = ", ".join(
        i.type.upper() if i.type in ("rest", "grpc") else i.type
        for i in config.interfaces
    ) or "none"
    fw_ver = (
        f" {config.framework.version}"
        if config.framework.version
        else ""
    )
    lines = [
        f"# Project Identity — {config.project.name}",
        "",
        "## Identity",
        "",
        f"- **Name:** {config.project.name}",
        f"- **Architecture Style:** {config.architecture.style}",
        f"- **Domain-Driven Design:** "
        f"{str(config.architecture.domain_driven).lower()}",
        f"- **Event-Driven:** "
        f"{str(config.architecture.event_driven).lower()}",
        f"- **Interfaces:** {ifaces}",
        f"- **Language:** {config.language.name} "
        f"{config.language.version}",
        f"- **Framework:** {config.framework.name}{fw_ver}",
        "",
        "## Technology Stack",
        "",
        "| Layer | Technology |",
        "|-------|-----------|",
        f"| Architecture | {config.architecture.style.capitalize()} |",
        f"| Language | {config.language.name.capitalize()} "
        f"{config.language.version} |",
        f"| Framework | {config.framework.name.capitalize()}{fw_ver} |",
        f"| Build Tool | {config.framework.build_tool.capitalize()} |",
        f"| Container | {config.infrastructure.container.capitalize()} |",
        f"| Orchestrator | "
        f"{config.infrastructure.orchestrator.capitalize()} |",
        "| Resilience | Mandatory (always enabled) |",
        f"| Native Build | "
        f"{str(config.framework.native_build).lower()} |",
        f"| Smoke Tests | "
        f"{str(config.testing.smoke_tests).lower()} |",
        f"| Contract Tests | "
        f"{str(config.testing.contract_tests).lower()} |",
        "",
        "## Constraints",
        "",
        "- Cloud-Agnostic: ZERO dependencies on cloud-specific services",
        "- Horizontal scalability: Application must be stateless",
        "- Externalized configuration: All configuration via "
        "environment variables or ConfigMaps",
        "",
        "## Contextual Instructions",
        "",
        "The following instruction files provide domain-specific "
        "context:",
        "",
        "- `instructions/domain.instructions.md` — Domain model, "
        "business rules, sensitive data",
        "- `instructions/coding-standards.instructions.md` — "
        "Clean Code, SOLID, naming, error handling",
        "- `instructions/architecture.instructions.md` — "
        "Hexagonal architecture, layer rules, package structure",
        "- `instructions/quality-gates.instructions.md` — "
        "Coverage thresholds, test categories, merge checklist",
        "",
        "For deep-dive references, see the knowledge packs in "
        "`.claude/skills/` (generated alongside this structure).",
    ]
    return "\n".join(lines) + "\n"
