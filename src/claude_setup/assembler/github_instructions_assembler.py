from __future__ import annotations

import logging
from pathlib import Path
from typing import List

from claude_setup.models import ProjectConfig
from claude_setup.template_engine import TemplateEngine

logger = logging.getLogger(__name__)


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
            self._generate_contextual(config, instructions_dir, engine),
        )
        return generated

    def _generate_global(
        self,
        config: ProjectConfig,
        github_dir: Path,
    ) -> Path:
        """Generate copilot-instructions.md from project config."""
        dest = github_dir / "copilot-instructions.md"
        content = _build_copilot_instructions(config)
        dest.write_text(content, encoding="utf-8")
        return dest

    def _generate_contextual(
        self,
        config: ProjectConfig,
        instructions_dir: Path,
        engine: TemplateEngine,
    ) -> List[Path]:
        """Generate contextual .instructions.md files from templates."""
        templates_dir = self._resources_dir / "github-instructions-templates"
        if not templates_dir.is_dir():
            logger.warning(
                "GitHub instructions templates not found: %s",
                templates_dir,
            )
            return []
        generated: List[Path] = []
        for template_file in sorted(templates_dir.glob("*.md")):
            dest_name = template_file.stem + ".instructions.md"
            content = template_file.read_text(encoding="utf-8")
            content = engine.replace_placeholders(content)
            dest = instructions_dir / dest_name
            dest.write_text(content, encoding="utf-8")
            generated.append(dest)
        return generated


def _build_copilot_instructions(config: ProjectConfig) -> str:
    """Build copilot-instructions.md content from config."""
    ifaces = ", ".join(
        i.type.upper() if i.type in ("rest", "grpc") else i.type
        for i in config.interfaces
    ) or "none"
    fw_ver = (
        f" {config.framework.version}"
        if config.framework.version
        else ""
    )
    lines: List[str] = []
    lines.extend(_header(config, ifaces, fw_ver))
    lines.extend(_tech_stack(config, fw_ver))
    lines.extend(_language_policy())
    lines.extend(_constraints())
    lines.extend(_source_of_truth())
    lines.extend(_contextual_refs())
    return "\n".join(lines) + "\n"


def _header(
    config: ProjectConfig,
    ifaces: str,
    fw_ver: str,
) -> List[str]:
    return [
        f"# Project Identity — {config.project.name}",
        "",
        "## Identity",
        "",
        f"- **Name:** {config.project.name}",
        f"- **Architecture Style:** {config.architecture.style}",
        f"- **Domain-Driven Design:** {str(config.architecture.domain_driven).lower()}",
        f"- **Event-Driven:** {str(config.architecture.event_driven).lower()}",
        f"- **Interfaces:** {ifaces}",
        f"- **Language:** {config.language.name.capitalize()} {config.language.version}",
        f"- **Framework:** {config.framework.name.capitalize()}{fw_ver}",
    ]


def _tech_stack(config: ProjectConfig, fw_ver: str) -> List[str]:
    obs = config.infrastructure.observability
    return [
        "",
        "## Technology Stack",
        "",
        "| Layer | Technology |",
        "|-------|-----------|",
        f"| Architecture | {config.architecture.style.capitalize()} |",
        f"| Language | {config.language.name.capitalize()} {config.language.version} |",
        f"| Framework | {config.framework.name.capitalize()}{fw_ver} |",
        f"| Build Tool | {config.framework.build_tool.capitalize()} |",
        f"| Container | {config.infrastructure.container} |",
        f"| Orchestrator | {config.infrastructure.orchestrator} |",
        f"| Resilience | Mandatory (always enabled) |",
        f"| Native Build | {str(config.framework.native_build).lower()} |",
        f"| Smoke Tests | {str(config.testing.smoke_tests).lower()} |",
        f"| Contract Tests | {str(config.testing.contract_tests).lower()} |",
    ]


def _language_policy() -> List[str]:
    return [
        "",
        "## Language Policy",
        "",
        "- Output language: English only",
        "- Code: English (classes, methods, variables)",
        "- Commits: English (Conventional Commits)",
        "- Documentation: English",
        "- Application logs: English",
    ]


def _constraints() -> List[str]:
    return [
        "",
        "## Constraints",
        "",
        "- Cloud-Agnostic: ZERO dependencies on cloud-specific services",
        "- Horizontal scalability: Application must be stateless",
        "- Externalized configuration: All configuration via environment variables or ConfigMaps",
    ]


def _source_of_truth() -> List[str]:
    return [
        "",
        "## Source of Truth (Hierarchy)",
        "",
        "1. Epics / PRDs (vision and global rules)",
        "2. ADRs (architectural decisions)",
        "3. Stories / tickets (detailed requirements)",
        "4. Instructions (`.github/instructions/`)",
        "5. Source code",
    ]


def _contextual_refs() -> List[str]:
    return [
        "",
        "## Contextual Instructions",
        "",
        "For detailed guidance on specific topics, the following contextual instructions are",
        "loaded automatically when relevant:",
        "",
        "- `instructions/domain.instructions.md` — Domain model, business rules, ubiquitous language",
        "- `instructions/coding-standards.instructions.md` — Clean Code, SOLID, language conventions",
        "- `instructions/architecture.instructions.md` — Architecture style, layer rules, package structure",
        "- `instructions/quality-gates.instructions.md` — Coverage thresholds, test categories, merge checklist",
        "",
        "For deep-dive references, see the knowledge packs in `.claude/skills/`.",
    ]
