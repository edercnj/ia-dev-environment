from __future__ import annotations

from pathlib import Path

import pytest

from claude_setup.assembler.readme_assembler import (
    ReadmeAssembler,
    _build_generation_summary,
    _build_rules_table,
    _count_hooks,
    _count_knowledge_packs,
    _count_settings,
    _is_knowledge_pack,
    generate_minimal_readme,
)
from claude_setup.models import ProjectConfig
from claude_setup.template_engine import TemplateEngine


def _create_readme_src(base: Path) -> Path:
    """Create src dir with readme template."""
    base.mkdir(parents=True, exist_ok=True)
    template = base / "readme-template.md"
    template.write_text(
        "# {{PROJECT_NAME}}\n\n"
        "Rules: {{RULES_COUNT}}\n"
        "Skills: {{SKILLS_COUNT}}\n"
        "Agents: {{AGENTS_COUNT}}\n\n"
        "{{RULES_TABLE}}\n\n"
        "{{SKILLS_TABLE}}\n\n"
        "{{AGENTS_TABLE}}\n\n"
        "{{HOOKS_SECTION}}\n\n"
        "{{KNOWLEDGE_PACKS_TABLE}}\n\n"
        "{{SETTINGS_SECTION}}\n\n"
        "{{GENERATION_SUMMARY}}\n",
        encoding="utf-8",
    )
    return base


def _populate_output(out: Path) -> None:
    """Create sample rules, skills, agents in output."""
    rules = out / "rules"
    rules.mkdir(parents=True)
    (rules / "01-identity.md").write_text("# Identity\n")
    (rules / "02-domain.md").write_text("# Domain\n")
    (rules / "03-coding.md").write_text("# Coding\n")
    skills = out / "skills"
    (skills / "commit").mkdir(parents=True)
    (skills / "commit" / "SKILL.md").write_text(
        'description: "Create commits"\n',
    )
    (skills / "review").mkdir(parents=True)
    (skills / "review" / "SKILL.md").write_text(
        'description: "Review code"\n',
    )
    (skills / "testing-kp").mkdir(parents=True)
    (skills / "testing-kp" / "SKILL.md").write_text(
        "# Knowledge Pack\nTesting reference\nuser-invocable: false\n",
    )
    agents = out / "agents"
    agents.mkdir(parents=True)
    (agents / "architect.md").write_text("# Architect\n")
    (agents / "developer.md").write_text("# Developer\n")


def _minimal_config() -> ProjectConfig:
    return ProjectConfig.from_dict({
        "project": {"name": "my-cli", "purpose": "CLI tool"},
        "architecture": {"style": "library"},
        "interfaces": [{"type": "cli"}],
        "language": {"name": "python", "version": "3.9"},
        "framework": {"name": "click", "version": "8.1"},
    })


def _java_config() -> ProjectConfig:
    return ProjectConfig.from_dict({
        "project": {"name": "java-svc", "purpose": "Java service"},
        "architecture": {"style": "microservice"},
        "interfaces": [{"type": "rest"}],
        "language": {"name": "java", "version": "21"},
        "framework": {"name": "quarkus", "version": "3.17", "build_tool": "maven"},
    })


class TestReadmeAssemblerAssemble:
    def test_assemble_populated_dir_writes_readme(self, tmp_path):
        src = _create_readme_src(tmp_path / "src")
        out = tmp_path / "output"
        out.mkdir()
        _populate_output(out)
        config = _minimal_config()
        engine = TemplateEngine(src, config)
        asm = ReadmeAssembler(src)
        result = asm.assemble(config, out, engine)
        assert len(result) == 1
        assert result[0].name == "README.md"
        assert result[0].is_file()

    def test_assemble_populated_dir_replaces_project_name(self, tmp_path):
        src = _create_readme_src(tmp_path / "src")
        out = tmp_path / "output"
        out.mkdir()
        _populate_output(out)
        config = _minimal_config()
        engine = TemplateEngine(src, config)
        asm = ReadmeAssembler(src)
        asm.assemble(config, out, engine)
        content = (out / "README.md").read_text()
        assert "# my-cli" in content
        assert "{{PROJECT_NAME}}" not in content

    def test_assemble_three_rules_shows_correct_count(self, tmp_path):
        src = _create_readme_src(tmp_path / "src")
        out = tmp_path / "output"
        out.mkdir()
        _populate_output(out)
        config = _minimal_config()
        engine = TemplateEngine(src, config)
        asm = ReadmeAssembler(src)
        asm.assemble(config, out, engine)
        content = (out / "README.md").read_text()
        assert "Rules: 3" in content

    def test_assemble_three_skills_shows_correct_count(self, tmp_path):
        src = _create_readme_src(tmp_path / "src")
        out = tmp_path / "output"
        out.mkdir()
        _populate_output(out)
        config = _minimal_config()
        engine = TemplateEngine(src, config)
        asm = ReadmeAssembler(src)
        asm.assemble(config, out, engine)
        content = (out / "README.md").read_text()
        assert "Skills: 3" in content

    def test_assemble_two_agents_shows_correct_count(self, tmp_path):
        src = _create_readme_src(tmp_path / "src")
        out = tmp_path / "output"
        out.mkdir()
        _populate_output(out)
        config = _minimal_config()
        engine = TemplateEngine(src, config)
        asm = ReadmeAssembler(src)
        asm.assemble(config, out, engine)
        content = (out / "README.md").read_text()
        assert "Agents: 2" in content

    def test_assemble_with_rules_generates_rules_table(self, tmp_path):
        src = _create_readme_src(tmp_path / "src")
        out = tmp_path / "output"
        out.mkdir()
        _populate_output(out)
        config = _minimal_config()
        engine = TemplateEngine(src, config)
        asm = ReadmeAssembler(src)
        asm.assemble(config, out, engine)
        content = (out / "README.md").read_text()
        assert "| # | File | Scope |" in content
        assert "`01-identity.md`" in content

    def test_assemble_with_skills_generates_skills_table(self, tmp_path):
        src = _create_readme_src(tmp_path / "src")
        out = tmp_path / "output"
        out.mkdir()
        _populate_output(out)
        config = _minimal_config()
        engine = TemplateEngine(src, config)
        asm = ReadmeAssembler(src)
        asm.assemble(config, out, engine)
        content = (out / "README.md").read_text()
        assert "| Skill | Path | Description |" in content
        assert "**commit**" in content
        assert "**review**" in content

    def test_assemble_with_agents_generates_agents_table(self, tmp_path):
        src = _create_readme_src(tmp_path / "src")
        out = tmp_path / "output"
        out.mkdir()
        _populate_output(out)
        config = _minimal_config()
        engine = TemplateEngine(src, config)
        asm = ReadmeAssembler(src)
        asm.assemble(config, out, engine)
        content = (out / "README.md").read_text()
        assert "| Agent | File |" in content
        assert "**architect**" in content
        assert "**developer**" in content

    def test_assemble_python_config_shows_no_hooks_configured(self, tmp_path):
        src = _create_readme_src(tmp_path / "src")
        out = tmp_path / "output"
        out.mkdir()
        _populate_output(out)
        config = _minimal_config()
        engine = TemplateEngine(src, config)
        asm = ReadmeAssembler(src)
        asm.assemble(config, out, engine)
        content = (out / "README.md").read_text()
        assert "No hooks configured." in content

    def test_assemble_java_config_shows_post_compile_check(self, tmp_path):
        src = _create_readme_src(tmp_path / "src")
        out = tmp_path / "output"
        out.mkdir()
        _populate_output(out)
        config = _java_config()
        engine = TemplateEngine(src, config)
        asm = ReadmeAssembler(src)
        asm.assemble(config, out, engine)
        content = (out / "README.md").read_text()
        assert "Post-Compile Check" in content
        assert "PostToolUse" in content

    def test_assemble_with_knowledge_packs_shows_in_kp_table(self, tmp_path):
        src = _create_readme_src(tmp_path / "src")
        out = tmp_path / "output"
        out.mkdir()
        _populate_output(out)
        config = _minimal_config()
        engine = TemplateEngine(src, config)
        asm = ReadmeAssembler(src)
        asm.assemble(config, out, engine)
        content = (out / "README.md").read_text()
        assert "testing-kp" in content

    def test_assemble_any_config_includes_settings_section(self, tmp_path):
        src = _create_readme_src(tmp_path / "src")
        out = tmp_path / "output"
        out.mkdir()
        _populate_output(out)
        config = _minimal_config()
        engine = TemplateEngine(src, config)
        asm = ReadmeAssembler(src)
        asm.assemble(config, out, engine)
        content = (out / "README.md").read_text()
        assert "settings.json" in content
        assert "permissions.allow" in content

    def test_assemble_no_template_generates_minimal_readme(self, tmp_path):
        src = tmp_path / "src"
        src.mkdir()
        out = tmp_path / "output"
        out.mkdir()
        config = _minimal_config()
        engine = TemplateEngine(src, config)
        asm = ReadmeAssembler(src)
        result = asm.assemble(config, out, engine)
        content = (out / "README.md").read_text()
        assert "my-cli" in content
        assert "library" in content

    def test_assemble_empty_output_dir_shows_zero_counts(self, tmp_path):
        src = _create_readme_src(tmp_path / "src")
        out = tmp_path / "output"
        out.mkdir()
        config = _minimal_config()
        engine = TemplateEngine(src, config)
        asm = ReadmeAssembler(src)
        asm.assemble(config, out, engine)
        content = (out / "README.md").read_text()
        assert "Rules: 0" in content
        assert "Skills: 0" in content
        assert "Agents: 0" in content

    def test_assemble_empty_output_dir_shows_no_skills_message(self, tmp_path):
        src = _create_readme_src(tmp_path / "src")
        out = tmp_path / "output"
        out.mkdir()
        config = _minimal_config()
        engine = TemplateEngine(src, config)
        asm = ReadmeAssembler(src)
        asm.assemble(config, out, engine)
        content = (out / "README.md").read_text()
        assert "No skills configured." in content
        assert "No agents configured." in content


class TestBuildRulesTable:
    def test_build_rules_table_no_rules_dir_returns_no_rules_msg(self, tmp_path):
        assert _build_rules_table(tmp_path) == "No rules configured."

    def test_build_rules_table_empty_rules_dir_returns_no_rules_msg(self, tmp_path):
        (tmp_path / "rules").mkdir()
        assert _build_rules_table(tmp_path) == "No rules configured."


class TestIsKnowledgePack:
    def test_is_knowledge_pack_user_invocable_false_returns_true(self, tmp_path):
        skill_md = tmp_path / "SKILL.md"
        skill_md.write_text("description: test\nuser-invocable: false\n")
        assert _is_knowledge_pack(skill_md) is True

    def test_is_knowledge_pack_knowledge_pack_header_returns_true(self, tmp_path):
        skill_md = tmp_path / "SKILL.md"
        skill_md.write_text("# Knowledge Pack\nSome content\n")
        assert _is_knowledge_pack(skill_md) is True

    def test_is_knowledge_pack_normal_skill_returns_false(self, tmp_path):
        skill_md = tmp_path / "SKILL.md"
        skill_md.write_text('description: "A skill"\nuser-invocable: true\n')
        assert _is_knowledge_pack(skill_md) is False


class TestGenerateMinimalReadme:
    def test_generate_minimal_readme_any_config_contains_project_name(self):
        config = _minimal_config()
        content = generate_minimal_readme(config)
        assert "my-cli" in content

    def test_generate_minimal_readme_any_config_contains_arch_style(self):
        config = _minimal_config()
        content = generate_minimal_readme(config)
        assert "library" in content

    def test_generate_minimal_readme_any_config_contains_interfaces(self):
        config = _minimal_config()
        content = generate_minimal_readme(config)
        assert "cli" in content

    def test_generate_minimal_readme_any_config_contains_structure(self):
        config = _minimal_config()
        content = generate_minimal_readme(config)
        assert "## Structure" in content
        assert ".claude/" in content


class TestCountKnowledgePacks:
    def test_count_knowledge_packs_with_one_kp(self, tmp_path):
        _populate_output(tmp_path)
        assert _count_knowledge_packs(tmp_path) == 1

    def test_count_knowledge_packs_no_skills_dir(self, tmp_path):
        assert _count_knowledge_packs(tmp_path) == 0


class TestCountHooks:
    def test_count_hooks_with_files(self, tmp_path):
        hooks = tmp_path / "hooks"
        hooks.mkdir()
        (hooks / "post-compile.sh").write_text("#!/bin/sh\n")
        assert _count_hooks(tmp_path) == 1

    def test_count_hooks_no_dir(self, tmp_path):
        assert _count_hooks(tmp_path) == 0


class TestCountSettings:
    def test_count_settings_both_files(self, tmp_path):
        (tmp_path / "settings.json").write_text("{}")
        (tmp_path / "settings.local.json").write_text("{}")
        assert _count_settings(tmp_path) == 2

    def test_count_settings_no_files(self, tmp_path):
        assert _count_settings(tmp_path) == 0


class TestBuildGenerationSummary:
    def test_summary_contains_table_header(self, tmp_path):
        _populate_output(tmp_path)
        config = _minimal_config()
        summary = _build_generation_summary(tmp_path, config)
        assert "| Component | Count |" in summary

    def test_summary_contains_rules_count(self, tmp_path):
        _populate_output(tmp_path)
        config = _minimal_config()
        summary = _build_generation_summary(tmp_path, config)
        assert "| Rules (.claude) | 3 |" in summary

    def test_summary_contains_version(self, tmp_path):
        _populate_output(tmp_path)
        config = _minimal_config()
        summary = _build_generation_summary(tmp_path, config)
        assert "claude-setup v" in summary

    def test_assemble_populated_dir_includes_generation_summary(
        self, tmp_path,
    ):
        src = _create_readme_src(tmp_path / "src")
        out = tmp_path / "output"
        out.mkdir()
        _populate_output(out)
        config = _minimal_config()
        engine = TemplateEngine(src, config)
        asm = ReadmeAssembler(src)
        asm.assemble(config, out, engine)
        content = (out / "README.md").read_text()
        assert "| Component | Count |" in content
        assert "claude-setup v" in content
