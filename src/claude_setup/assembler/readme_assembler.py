from __future__ import annotations

import logging
import re
from pathlib import Path
from typing import List

from claude_setup import __version__
from claude_setup.domain.stack_mapping import get_hook_template_key
from claude_setup.models import ProjectConfig
from claude_setup.template_engine import TemplateEngine

logger = logging.getLogger(__name__)


class ReadmeAssembler:
    """Generates README.md from template or minimal fallback."""

    def __init__(self, resources_dir: Path) -> None:
        self._resources_dir = resources_dir

    def assemble(
        self,
        config: ProjectConfig,
        output_dir: Path,
        engine: TemplateEngine,
    ) -> List[Path]:
        """Generate README.md in output_dir."""
        template = self._resources_dir / "readme-template.md"
        if template.is_file():
            content = _generate_readme(config, output_dir, template)
        else:
            content = generate_minimal_readme(config)
        dest = output_dir / "README.md"
        dest.write_text(content, encoding="utf-8")
        logger.info("Generated %s", dest)
        return [dest]


def _generate_readme(
    config: ProjectConfig,
    output_dir: Path,
    template_path: Path,
) -> str:
    """Build full README from template with placeholders."""
    content = template_path.read_text(encoding="utf-8")
    rules_count = _count_rules(output_dir)
    skills_count = _count_skills(output_dir)
    agents_count = _count_agents(output_dir)
    content = content.replace("{{PROJECT_NAME}}", config.project.name)
    content = content.replace("{{RULES_COUNT}}", str(rules_count))
    content = content.replace("{{SKILLS_COUNT}}", str(skills_count))
    content = content.replace("{{AGENTS_COUNT}}", str(agents_count))
    content = content.replace("{{RULES_TABLE}}", _build_rules_table(output_dir))
    content = content.replace("{{SKILLS_TABLE}}", _build_skills_table(output_dir))
    content = content.replace("{{AGENTS_TABLE}}", _build_agents_table(output_dir))
    content = content.replace("{{HOOKS_SECTION}}", _build_hooks_section(config))
    content = content.replace(
        "{{KNOWLEDGE_PACKS_TABLE}}",
        _build_knowledge_packs_table(output_dir),
    )
    content = content.replace("{{SETTINGS_SECTION}}", _build_settings_section())
    content = content.replace(
        "{{MAPPING_TABLE}}",
        _build_mapping_table(output_dir),
    )
    content = content.replace(
        "{{GENERATION_SUMMARY}}",
        _build_generation_summary(output_dir, config),
    )
    return content


def generate_minimal_readme(config: ProjectConfig) -> str:
    """Generate minimal README with basic project info."""
    ifaces = " ".join(i.type for i in config.interfaces) or "none"
    header = (
        f"# .claude/ \u2014 {config.project.name}\n"
        "\n"
        f"This directory contains the Claude Code configuration"
        f" for **{config.project.name}**.\n"
        "\n"
    )
    structure = _build_structure_block()
    tips = _build_tips_block(config.architecture.style, ifaces)
    return header + structure + tips


def _build_structure_block() -> str:
    """Build the directory structure section."""
    return (
        "## Structure\n\n```\n"
        ".claude/\n"
        "\u251c\u2500\u2500 README.md               \u2190 You are here\n"
        "\u251c\u2500\u2500 settings.json           \u2190 Shared config (committed to git)\n"
        "\u251c\u2500\u2500 settings.local.json     \u2190 Local overrides (gitignored)\n"
        "\u251c\u2500\u2500 rules/                  \u2190 Coding rules (loaded in system prompt)\n"
        "\u2502   \u251c\u2500\u2500 patterns/           \u2190 Design patterns (architecture-driven)\n"
        "\u2502   \u2514\u2500\u2500 protocols/          \u2190 Protocol conventions (interface-driven)\n"
        "\u251c\u2500\u2500 skills/                 \u2190 Skills invocable via /command\n"
        "\u251c\u2500\u2500 agents/                 \u2190 AI personas (used by skills)\n"
        "\u2514\u2500\u2500 hooks/                  \u2190 Automation (post-compile, etc.)\n"
        "```\n\n"
    )


def _build_tips_block(arch_style: str, ifaces: str) -> str:
    """Build the tips section."""
    return (
        "## Tips\n\n"
        "- **Rules are always active** \u2014 loaded automatically in every conversation\n"
        f"- **Patterns are selected** \u2014 based on architecture style ({arch_style})\n"
        f"- **Protocols are selected** \u2014 based on interfaces ({ifaces})\n"
        "- **Skills are lazy** \u2014 only load when you type `/name`\n"
        "- **Agents are not invoked directly** \u2014 used by skills internally\n"
        "- **Hooks run automatically** \u2014 compile check after editing source files\n"
    )


def _count_rules(output_dir: Path) -> int:
    rules_dir = output_dir / "rules"
    if not rules_dir.is_dir():
        return 0
    return len(list(rules_dir.glob("*.md")))


def _count_skills(output_dir: Path) -> int:
    skills_dir = output_dir / "skills"
    if not skills_dir.is_dir():
        return 0
    return len(list(skills_dir.glob("*/SKILL.md")))


def _count_agents(output_dir: Path) -> int:
    agents_dir = output_dir / "agents"
    if not agents_dir.is_dir():
        return 0
    return len(list(agents_dir.glob("*.md")))


def _build_rules_table(output_dir: Path) -> str:
    rules_dir = output_dir / "rules"
    if not rules_dir.is_dir():
        return "No rules configured."
    files = sorted(rules_dir.glob("*.md"))
    if not files:
        return "No rules configured."
    lines = ["| # | File | Scope |", "|---|------|-------|"]
    for rule_file in files:
        fname = rule_file.name
        num = _extract_rule_number(fname)
        scope = _extract_rule_scope(fname)
        lines.append(f"| {num} | `{fname}` | {scope} |")
    return "\n".join(lines)


def _extract_rule_number(filename: str) -> str:
    match = re.match(r"^(\d+)", filename)
    return match.group(1) if match else ""


def _extract_rule_scope(filename: str) -> str:
    name = re.sub(r"^\d+-", "", filename)
    name = name.removesuffix(".md")
    return name.replace("-", " ")


def _build_skills_table(output_dir: Path) -> str:
    skills_dir = output_dir / "skills"
    if not skills_dir.is_dir():
        return "No skills configured."
    rows: List[str] = []
    for skill_md in sorted(skills_dir.glob("*/SKILL.md")):
        sname = skill_md.parent.name
        sdesc = _extract_skill_description(skill_md)
        if _is_knowledge_pack(skill_md):
            continue
        rows.append(f"| **{sname}** | `/{sname}` | {sdesc} |")
    if not rows:
        return "No skills configured."
    header = ["| Skill | Path | Description |", "|-------|------|-------------|"]
    return "\n".join(header + rows)


def _extract_skill_description(skill_md: Path) -> str:
    text = skill_md.read_text(encoding="utf-8")
    for line in text.splitlines():
        if line.startswith("description:"):
            desc = line.split(":", 1)[1].strip()
            return desc.strip('"').strip("'")
    return ""


def _is_knowledge_pack(skill_md: Path) -> bool:
    text = skill_md.read_text(encoding="utf-8")
    if "user-invocable: false" in text:
        return True
    for line in text.splitlines():
        if line.startswith("# Knowledge Pack"):
            return True
    return False


def _build_agents_table(output_dir: Path) -> str:
    agents_dir = output_dir / "agents"
    if not agents_dir.is_dir():
        return "No agents configured."
    rows: List[str] = []
    for agent_file in sorted(agents_dir.glob("*.md")):
        aname = agent_file.stem
        rows.append(f"| **{aname}** | `{agent_file.name}` |")
    if not rows:
        return "No agents configured."
    header = ["| Agent | File |", "|-------|------|"]
    return "\n".join(header + rows)


def _build_hooks_section(config: ProjectConfig) -> str:
    key = get_hook_template_key(
        config.language.name,
        config.framework.build_tool,
    )
    if not key:
        return "No hooks configured."
    ext = _get_file_extension(config)
    compile_cmd = _get_compile_command(config)
    return (
        "### Post-Compile Check\n"
        "\n"
        "- **Event:** `PostToolUse` (after `Write` or `Edit`)\n"
        "- **Script:** `.claude/hooks/post-compile-check.sh`\n"
        f"- **Behavior:** When a `{ext}` file is modified,"
        f" runs `{compile_cmd}` automatically\n"
        "- **Purpose:** Catch compilation errors immediately"
        " after file changes"
    )


def _get_file_extension(config: ProjectConfig) -> str:
    from claude_setup.domain.stack_mapping import LANGUAGE_COMMANDS
    key = (config.language.name, config.framework.build_tool)
    commands = LANGUAGE_COMMANDS.get(key, {})
    return commands.get("file_extension", "")


def _get_compile_command(config: ProjectConfig) -> str:
    from claude_setup.domain.stack_mapping import LANGUAGE_COMMANDS
    key = (config.language.name, config.framework.build_tool)
    commands = LANGUAGE_COMMANDS.get(key, {})
    return commands.get("compile_cmd", "")


def _build_knowledge_packs_table(output_dir: Path) -> str:
    skills_dir = output_dir / "skills"
    if not skills_dir.is_dir():
        return "No knowledge packs configured."
    lines: List[str] = []
    for skill_md in sorted(skills_dir.glob("*/SKILL.md")):
        if _is_knowledge_pack(skill_md):
            kp_name = skill_md.parent.name
            lines.append(
                f"| `{kp_name}` | Referenced internally by agents |",
            )
    if not lines:
        return "No knowledge packs configured."
    header = "| Pack | Usage |\n|------|-------|"
    return header + "\n" + "\n".join(lines)


def _build_settings_section() -> str:
    return (
        "### settings.json\n"
        "\n"
        "Permissions are configured in `settings.json`"
        " under `permissions.allow`.\n"
        "This controls which Bash commands Claude Code"
        " can run without asking.\n"
        "\n"
        "### settings.local.json\n"
        "\n"
        "Local overrides (gitignored). Use for personal"
        " preferences or team-specific tools.\n"
        "\n"
        "See the files directly for current configuration."
    )


def _count_knowledge_packs(output_dir: Path) -> int:
    skills_dir = output_dir / "skills"
    if not skills_dir.is_dir():
        return 0
    return sum(
        1 for s in skills_dir.glob("*/SKILL.md") if _is_knowledge_pack(s)
    )


def _count_hooks(output_dir: Path) -> int:
    hooks_dir = output_dir / "hooks"
    if not hooks_dir.is_dir():
        return 0
    return len(list(hooks_dir.iterdir()))


def _count_settings(output_dir: Path) -> int:
    count = 0
    if (output_dir / "settings.json").is_file():
        count += 1
    if (output_dir / "settings.local.json").is_file():
        count += 1
    return count


def _build_mapping_table(output_dir: Path) -> str:
    """Build the .claude/ <-> .github/ equivalence mapping table."""
    github_dir = output_dir / "github"
    rows = [
        (
            "Rules (`rules/*.md`)",
            "Instructions (`instructions/*.instructions.md`)",
            "Rules are system-prompt loaded; instructions are contextual",
        ),
        (
            "Skills (`skills/*/SKILL.md`)",
            "Skills (`skills/*/SKILL.md`)",
            "Same structure, same YAML frontmatter",
        ),
        (
            "Agents (`agents/*.md`)",
            "Agents (`agents/*.agent.md`)",
            "GitHub agents use `.agent.md` extension with YAML frontmatter",
        ),
        (
            "Hooks (`hooks/`)",
            "Hooks (`hooks/*.json`)",
            "Both define event-driven automations",
        ),
        (
            "Settings (`settings*.json`)",
            "N/A",
            "Claude Code specific",
        ),
        (
            "N/A",
            "Prompts (`prompts/*.prompt.md`)",
            "GitHub Copilot prompt templates",
        ),
        (
            "N/A",
            "MCP (`copilot-mcp.json`)",
            "GitHub Copilot MCP server configuration",
        ),
        (
            "N/A",
            "Global instructions (`copilot-instructions.md`)",
            "Loaded in every Copilot session",
        ),
    ]
    lines = [
        "| .claude/ | .github/ | Notes |",
        "|----------|----------|-------|",
    ]
    for claude_col, github_col, notes in rows:
        lines.append(f"| {claude_col} | {github_col} | {notes} |")
    gh_total = _count_github_files(github_dir) if github_dir.is_dir() else 0
    if gh_total > 0:
        lines.append("")
        lines.append(f"**Total .github/ artifacts: {gh_total}**")
    return "\n".join(lines)


def _count_github_files(github_dir: Path) -> int:
    """Count all generated files under github/ output directory."""
    if not github_dir.is_dir():
        return 0
    return len([f for f in github_dir.rglob("*") if f.is_file()])


def _build_generation_summary(
    output_dir: Path, config: ProjectConfig,
) -> str:
    """Build the generation summary table for README."""
    rules = _count_rules(output_dir)
    skills = _count_skills(output_dir) - _count_knowledge_packs(output_dir)
    kpacks = _count_knowledge_packs(output_dir)
    agents = _count_agents(output_dir)
    hooks = _count_hooks(output_dir)
    settings = _count_settings(output_dir)
    github_dir = output_dir / "github"
    gh_instructions = _count_github_component(github_dir, "instructions")
    gh_skills = _count_github_skills(github_dir)
    gh_agents = _count_github_component(github_dir, "agents")
    gh_prompts = _count_github_component(github_dir, "prompts")
    gh_hooks = _count_github_component(github_dir, "hooks")
    gh_global = 1 if (github_dir / "copilot-instructions.md").is_file() else 0
    gh_mcp = 1 if (github_dir / "copilot-mcp.json").is_file() else 0
    rows = [
        ("Rules (.claude)", rules),
        ("Skills (.claude)", skills),
        ("Knowledge Packs (.claude)", kpacks),
        ("Agents (.claude)", agents),
        ("Hooks (.claude)", hooks),
        ("Settings (.claude)", settings),
        ("Instructions (.github)", gh_instructions + gh_global),
        ("Skills (.github)", gh_skills),
        ("Agents (.github)", gh_agents),
        ("Prompts (.github)", gh_prompts),
        ("Hooks (.github)", gh_hooks),
        ("MCP (.github)", gh_mcp),
    ]
    lines = ["| Component | Count |", "|-----------|-------|"]
    for label, count in rows:
        lines.append(f"| {label} | {count} |")
    lines.append("")
    lines.append(f"Generated by `claude-setup v{__version__}`.")
    return "\n".join(lines)


def _count_github_component(github_dir: Path, component: str) -> int:
    """Count files in a github/ subdirectory."""
    comp_dir = github_dir / component
    if not comp_dir.is_dir():
        return 0
    return len(list(comp_dir.glob("*")))


def _count_github_skills(github_dir: Path) -> int:
    """Count github skills (each in its own subdirectory)."""
    skills_dir = github_dir / "skills"
    if not skills_dir.is_dir():
        return 0
    return len(list(skills_dir.glob("*/SKILL.md")))
