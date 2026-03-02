from __future__ import annotations

import json
from pathlib import Path
from typing import Any, Dict

import pytest

from claude_setup.assembler.settings_assembler import (
    SettingsAssembler,
    merge_json_arrays,
)
from claude_setup.models import ProjectConfig
from claude_setup.template_engine import TemplateEngine


def _create_settings_src(base: Path) -> Path:
    """Create settings-templates directory with common fragments."""
    tpl = base / "settings-templates"
    tpl.mkdir(parents=True, exist_ok=True)
    (tpl / "base.json").write_text(
        '["Bash(git *)", "Bash(ls *)"]', encoding="utf-8",
    )
    (tpl / "python-pip.json").write_text(
        '["Bash(python3 *)", "Bash(pip *)"]', encoding="utf-8",
    )
    (tpl / "java-maven.json").write_text(
        '["Bash(mvn *)", "Bash(./mvnw *)"]', encoding="utf-8",
    )
    (tpl / "docker.json").write_text(
        '["Bash(docker build *)"]', encoding="utf-8",
    )
    (tpl / "kubernetes.json").write_text(
        '["Bash(kubectl *)"]', encoding="utf-8",
    )
    (tpl / "docker-compose.json").write_text(
        '["Bash(docker compose *)"]', encoding="utf-8",
    )
    (tpl / "database-psql.json").write_text(
        '["Bash(psql *)"]', encoding="utf-8",
    )
    (tpl / "cache-redis.json").write_text(
        '["Bash(redis-cli *)"]', encoding="utf-8",
    )
    (tpl / "testing-newman.json").write_text(
        '["Bash(newman *)"]', encoding="utf-8",
    )
    return base


def _minimal_config() -> ProjectConfig:
    return ProjectConfig.from_dict({
        "project": {"name": "my-cli", "purpose": "CLI"},
        "architecture": {"style": "library"},
        "interfaces": [{"type": "cli"}],
        "language": {"name": "python", "version": "3.9"},
        "framework": {"name": "click", "version": "8.1"},
    })


def _full_config() -> ProjectConfig:
    return ProjectConfig.from_dict({
        "project": {"name": "full-svc", "purpose": "Full"},
        "architecture": {"style": "microservice"},
        "interfaces": [{"type": "rest"}],
        "language": {"name": "java", "version": "21"},
        "framework": {"name": "quarkus", "version": "3.17", "build_tool": "maven"},
        "data": {
            "database": {"name": "postgresql"},
            "cache": {"name": "redis"},
        },
        "infrastructure": {
            "container": "docker",
            "orchestrator": "kubernetes",
        },
        "testing": {"smoke_tests": True},
    })


class TestMergeJsonArrays:
    def test_no_overlap(self):
        assert merge_json_arrays(["a", "b"], ["c", "d"]) == ["a", "b", "c", "d"]

    def test_with_overlap(self):
        result = merge_json_arrays(["a", "b"], ["b", "c"])
        assert result == ["a", "b", "b", "c"]

    def test_empty_base(self):
        assert merge_json_arrays([], ["a", "b"]) == ["a", "b"]

    def test_empty_overlay(self):
        assert merge_json_arrays(["a", "b"], []) == ["a", "b"]

    def test_both_empty(self):
        assert merge_json_arrays([], []) == []


class TestSettingsAssembler:
    def test_assemble_writes_valid_json(self, tmp_path):
        src = _create_settings_src(tmp_path / "src")
        out = tmp_path / "output"
        out.mkdir()
        config = _minimal_config()
        engine = TemplateEngine(src, config)
        asm = SettingsAssembler(src)
        asm.assemble(config, out, engine)
        settings = json.loads((out / "settings.json").read_text())
        assert "permissions" in settings
        assert "allow" in settings["permissions"]

    def test_assemble_returns_two_paths(self, tmp_path):
        src = _create_settings_src(tmp_path / "src")
        out = tmp_path / "output"
        out.mkdir()
        config = _minimal_config()
        engine = TemplateEngine(src, config)
        asm = SettingsAssembler(src)
        result = asm.assemble(config, out, engine)
        assert len(result) == 2
        names = {p.name for p in result}
        assert "settings.json" in names
        assert "settings.local.json" in names

    def test_assemble_writes_settings_local(self, tmp_path):
        src = _create_settings_src(tmp_path / "src")
        out = tmp_path / "output"
        out.mkdir()
        config = _minimal_config()
        engine = TemplateEngine(src, config)
        asm = SettingsAssembler(src)
        asm.assemble(config, out, engine)
        local = json.loads((out / "settings.local.json").read_text())
        assert local == {"permissions": {"allow": []}}

    def test_base_always_included(self, tmp_path):
        src = _create_settings_src(tmp_path / "src")
        out = tmp_path / "output"
        out.mkdir()
        config = _minimal_config()
        engine = TemplateEngine(src, config)
        asm = SettingsAssembler(src)
        asm.assemble(config, out, engine)
        settings = json.loads((out / "settings.json").read_text())
        perms = settings["permissions"]["allow"]
        assert "Bash(git *)" in perms

    def test_includes_lang_key(self, tmp_path):
        src = _create_settings_src(tmp_path / "src")
        out = tmp_path / "output"
        out.mkdir()
        config = _minimal_config()
        engine = TemplateEngine(src, config)
        asm = SettingsAssembler(src)
        asm.assemble(config, out, engine)
        settings = json.loads((out / "settings.json").read_text())
        perms = settings["permissions"]["allow"]
        assert "Bash(python3 *)" in perms

    def test_includes_docker(self, tmp_path):
        src = _create_settings_src(tmp_path / "src")
        out = tmp_path / "output"
        out.mkdir()
        config = _full_config()
        engine = TemplateEngine(src, config)
        asm = SettingsAssembler(src)
        asm.assemble(config, out, engine)
        settings = json.loads((out / "settings.json").read_text())
        perms = settings["permissions"]["allow"]
        assert "Bash(docker build *)" in perms

    def test_includes_kubernetes(self, tmp_path):
        src = _create_settings_src(tmp_path / "src")
        out = tmp_path / "output"
        out.mkdir()
        config = _full_config()
        engine = TemplateEngine(src, config)
        asm = SettingsAssembler(src)
        asm.assemble(config, out, engine)
        settings = json.loads((out / "settings.json").read_text())
        perms = settings["permissions"]["allow"]
        assert "Bash(kubectl *)" in perms

    def test_includes_database(self, tmp_path):
        src = _create_settings_src(tmp_path / "src")
        out = tmp_path / "output"
        out.mkdir()
        config = _full_config()
        engine = TemplateEngine(src, config)
        asm = SettingsAssembler(src)
        asm.assemble(config, out, engine)
        settings = json.loads((out / "settings.json").read_text())
        perms = settings["permissions"]["allow"]
        assert "Bash(psql *)" in perms

    def test_includes_cache(self, tmp_path):
        src = _create_settings_src(tmp_path / "src")
        out = tmp_path / "output"
        out.mkdir()
        config = _full_config()
        engine = TemplateEngine(src, config)
        asm = SettingsAssembler(src)
        asm.assemble(config, out, engine)
        settings = json.loads((out / "settings.json").read_text())
        perms = settings["permissions"]["allow"]
        assert "Bash(redis-cli *)" in perms

    def test_includes_newman(self, tmp_path):
        src = _create_settings_src(tmp_path / "src")
        out = tmp_path / "output"
        out.mkdir()
        config = _full_config()
        engine = TemplateEngine(src, config)
        asm = SettingsAssembler(src)
        asm.assemble(config, out, engine)
        settings = json.loads((out / "settings.json").read_text())
        perms = settings["permissions"]["allow"]
        assert "Bash(newman *)" in perms

    def test_no_duplicates(self, tmp_path):
        src = _create_settings_src(tmp_path / "src")
        out = tmp_path / "output"
        out.mkdir()
        config = _full_config()
        engine = TemplateEngine(src, config)
        asm = SettingsAssembler(src)
        asm.assemble(config, out, engine)
        settings = json.loads((out / "settings.json").read_text())
        perms = settings["permissions"]["allow"]
        assert len(perms) == len(set(perms))

    def test_hooks_section_for_compiled_lang(self, tmp_path):
        src = _create_settings_src(tmp_path / "src")
        out = tmp_path / "output"
        out.mkdir()
        config = _full_config()
        engine = TemplateEngine(src, config)
        asm = SettingsAssembler(src)
        asm.assemble(config, out, engine)
        settings = json.loads((out / "settings.json").read_text())
        assert "hooks" in settings
        assert "PostToolUse" in settings["hooks"]

    def test_no_hooks_for_python(self, tmp_path):
        src = _create_settings_src(tmp_path / "src")
        out = tmp_path / "output"
        out.mkdir()
        config = _minimal_config()
        engine = TemplateEngine(src, config)
        asm = SettingsAssembler(src)
        asm.assemble(config, out, engine)
        settings = json.loads((out / "settings.json").read_text())
        assert "hooks" not in settings

    def test_minimal_config_permissions(self, tmp_path):
        src = _create_settings_src(tmp_path / "src")
        out = tmp_path / "output"
        out.mkdir()
        config = ProjectConfig.from_dict({
            "project": {"name": "bare", "purpose": "Bare"},
            "architecture": {"style": "library"},
            "interfaces": [{"type": "cli"}],
            "language": {"name": "python", "version": "3.9"},
            "framework": {"name": "click", "version": "8.1"},
            "infrastructure": {"container": "none", "orchestrator": "none"},
            "testing": {"smoke_tests": False},
        })
        engine = TemplateEngine(src, config)
        asm = SettingsAssembler(src)
        asm.assemble(config, out, engine)
        settings = json.loads((out / "settings.json").read_text())
        perms = settings["permissions"]["allow"]
        expected = ["Bash(git *)", "Bash(ls *)", "Bash(python3 *)", "Bash(pip *)"]
        assert perms == expected

    def test_docker_compose_permissions(self, tmp_path):
        src = _create_settings_src(tmp_path / "src")
        out = tmp_path / "output"
        out.mkdir()
        config = ProjectConfig.from_dict({
            "project": {"name": "svc", "purpose": "Test"},
            "architecture": {"style": "microservice"},
            "interfaces": [{"type": "rest"}],
            "language": {"name": "python", "version": "3.9"},
            "framework": {"name": "click", "version": "8.1"},
            "infrastructure": {
                "container": "docker",
                "orchestrator": "docker-compose",
            },
        })
        engine = TemplateEngine(src, config)
        asm = SettingsAssembler(src)
        asm.assemble(config, out, engine)
        settings = json.loads((out / "settings.json").read_text())
        perms = settings["permissions"]["allow"]
        assert "Bash(docker build *)" in perms
        assert "Bash(docker compose *)" in perms
