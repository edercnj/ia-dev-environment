from __future__ import annotations

import json
from pathlib import Path

import pytest

from claude_setup.assembler.settings_assembler import (
    SettingsAssembler,
    _read_json_array,
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
    def test_merge_json_arrays_no_overlap_returns_concatenated(self):
        assert merge_json_arrays(["a", "b"], ["c", "d"]) == ["a", "b", "c", "d"]

    def test_merge_json_arrays_with_overlap_returns_concatenated(self):
        result = merge_json_arrays(["a", "b"], ["b", "c"])
        assert result == ["a", "b", "b", "c"]

    def test_merge_json_arrays_empty_base_returns_overlay(self):
        assert merge_json_arrays([], ["a", "b"]) == ["a", "b"]

    def test_merge_json_arrays_empty_overlay_returns_base(self):
        assert merge_json_arrays(["a", "b"], []) == ["a", "b"]

    def test_merge_json_arrays_both_empty_returns_empty(self):
        assert merge_json_arrays([], []) == []


class TestReadJsonArray:
    def test_read_json_array_non_list_json_returns_empty(self, tmp_path):
        f = tmp_path / "bad.json"
        f.write_text('{"not": "a list"}', encoding="utf-8")
        assert _read_json_array(f) == []

    def test_read_json_array_valid_list_returns_list(self, tmp_path):
        f = tmp_path / "good.json"
        f.write_text('["a", "b"]', encoding="utf-8")
        assert _read_json_array(f) == ["a", "b"]

    def test_read_json_array_malformed_json_returns_empty(self, tmp_path):
        f = tmp_path / "malformed.json"
        f.write_text("{not valid json", encoding="utf-8")
        assert _read_json_array(f) == []


class TestSettingsAssemblerAssemble:
    def test_assemble_any_config_writes_valid_json(self, tmp_path):
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

    def test_assemble_any_config_returns_two_paths(self, tmp_path):
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

    def test_assemble_any_config_writes_settings_local(self, tmp_path):
        src = _create_settings_src(tmp_path / "src")
        out = tmp_path / "output"
        out.mkdir()
        config = _minimal_config()
        engine = TemplateEngine(src, config)
        asm = SettingsAssembler(src)
        asm.assemble(config, out, engine)
        local = json.loads((out / "settings.local.json").read_text())
        assert local == {"permissions": {"allow": []}}

    def test_assemble_any_config_includes_base_permissions(self, tmp_path):
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

    def test_assemble_python_config_includes_lang_permissions(self, tmp_path):
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

    def test_assemble_docker_config_includes_docker_permissions(self, tmp_path):
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

    def test_assemble_k8s_config_includes_kubernetes_permissions(self, tmp_path):
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

    def test_assemble_postgresql_config_includes_db_permissions(self, tmp_path):
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

    def test_assemble_redis_config_includes_cache_permissions(self, tmp_path):
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

    def test_assemble_smoke_tests_enabled_includes_newman_permissions(self, tmp_path):
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

    def test_assemble_full_config_no_duplicate_permissions(self, tmp_path):
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

    def test_assemble_compiled_lang_includes_hooks_section(self, tmp_path):
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

    def test_assemble_python_config_no_hooks_section(self, tmp_path):
        src = _create_settings_src(tmp_path / "src")
        out = tmp_path / "output"
        out.mkdir()
        config = _minimal_config()
        engine = TemplateEngine(src, config)
        asm = SettingsAssembler(src)
        asm.assemble(config, out, engine)
        settings = json.loads((out / "settings.json").read_text())
        assert "hooks" not in settings

    def test_assemble_bare_config_only_base_and_lang_permissions(self, tmp_path):
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

    def test_assemble_docker_compose_includes_both_docker_and_compose_perms(self, tmp_path):
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
