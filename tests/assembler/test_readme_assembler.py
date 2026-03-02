from __future__ import annotations

from pathlib import Path

import pytest

from claude_setup.assembler.readme_assembler import (
    ReadmeAssembler,
    _build_rules_table,
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
        "{{SETTINGS_SECTION}}\n",
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
    def test_assemble_populatedDir_writesReadme(self, tmp_path):
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

    def test_assemble_populatedDir_replacesProjectName(self, tmp_path):
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

    def test_assemble_threeRules_showsCorrectCount(self, tmp_path):
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

    def test_assemble_threeSkills_showsCorrectCount(self, tmp_path):
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

    def test_assemble_twoAgents_showsCorrectCount(self, tmp_path):
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

    def test_assemble_withRules_generatesRulesTable(self, tmp_path):
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

    def test_assemble_withKnowledgePacks_excludesFromSkillsTable(self, tmp_path):
        src = _create_readme_src(tmp_path / "src")
        out = tmp_path / "output"
        out.mkdir()
        _populate_output(out)
        config = _minimal_config()
        engine = TemplateEngine(src, config)
        asm = ReadmeAssembler(src)
        asm.assemble(config, out, engine)
        content = (out / "README.md").read_text()
        assert "**commit**" in content
        assert "**review**" in content

    def test_assemble_withAgents_generatesAgentsTable(self, tmp_path):
        src = _create_readme_src(tmp_path / "src")
        out = tmp_path / "output"
        out.mkdir()
        _populate_output(out)
        config = _minimal_config()
        engine = TemplateEngine(src, config)
        asm = ReadmeAssembler(src)
        asm.assemble(config, out, engine)
        content = (out / "README.md").read_text()
        assert "**architect**" in content
        assert "**developer**" in content

    def test_assemble_pythonConfig_showsNoHooksConfigured(self, tmp_path):
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

    def test_assemble_javaConfig_showsPostCompileCheck(self, tmp_path):
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

    def test_assemble_withKnowledgePacks_showsInKpTable(self, tmp_path):
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

    def test_assemble_anyConfig_includesSettingsSection(self, tmp_path):
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

    def test_assemble_noTemplate_generatesMinimalReadme(self, tmp_path):
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

    def test_assemble_emptyOutputDir_showsZeroCounts(self, tmp_path):
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


class TestBuildRulesTable:
    def test_buildRulesTable_noRulesDir_returnsNoRulesMsg(self, tmp_path):
        assert _build_rules_table(tmp_path) == "No rules configured."

    def test_buildRulesTable_emptyRulesDir_returnsNoRulesMsg(self, tmp_path):
        (tmp_path / "rules").mkdir()
        assert _build_rules_table(tmp_path) == "No rules configured."


class TestIsKnowledgePack:
    def test_isKnowledgePack_userInvocableFalse_returnsTrue(self, tmp_path):
        skill_md = tmp_path / "SKILL.md"
        skill_md.write_text("description: test\nuser-invocable: false\n")
        assert _is_knowledge_pack(skill_md) is True

    def test_isKnowledgePack_knowledgePackHeader_returnsTrue(self, tmp_path):
        skill_md = tmp_path / "SKILL.md"
        skill_md.write_text("# Knowledge Pack\nSome content\n")
        assert _is_knowledge_pack(skill_md) is True

    def test_isKnowledgePack_normalSkill_returnsFalse(self, tmp_path):
        skill_md = tmp_path / "SKILL.md"
        skill_md.write_text('description: "A skill"\nuser-invocable: true\n')
        assert _is_knowledge_pack(skill_md) is False


class TestGenerateMinimalReadme:
    def test_generateMinimalReadme_anyConfig_containsProjectName(self):
        config = _minimal_config()
        content = generate_minimal_readme(config)
        assert "my-cli" in content

    def test_generateMinimalReadme_anyConfig_containsArchStyle(self):
        config = _minimal_config()
        content = generate_minimal_readme(config)
        assert "library" in content

    def test_generateMinimalReadme_anyConfig_containsInterfaces(self):
        config = _minimal_config()
        content = generate_minimal_readme(config)
        assert "cli" in content

    def test_generateMinimalReadme_anyConfig_containsStructure(self):
        config = _minimal_config()
        content = generate_minimal_readme(config)
        assert "## Structure" in content
        assert ".claude/" in content
