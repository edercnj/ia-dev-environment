from __future__ import annotations

import os
from pathlib import Path

import pytest

from claude_setup.assembler.hooks_assembler import HooksAssembler
from claude_setup.models import ProjectConfig
from claude_setup.template_engine import TemplateEngine


def _create_hook_src(base: Path, key: str) -> Path:
    """Create a minimal hook template directory."""
    hook_dir = base / "hooks-templates" / key
    hook_dir.mkdir(parents=True, exist_ok=True)
    script = hook_dir / "post-compile-check.sh"
    script.write_text("#!/bin/bash\necho 'compile check'\n", encoding="utf-8")
    return base


def _java_maven_config() -> ProjectConfig:
    return ProjectConfig.from_dict({
        "project": {"name": "test-svc", "purpose": "Test"},
        "architecture": {"style": "microservice"},
        "interfaces": [{"type": "rest"}],
        "language": {"name": "java", "version": "21"},
        "framework": {"name": "quarkus", "version": "3.17", "build_tool": "maven"},
    })


def _python_config() -> ProjectConfig:
    return ProjectConfig.from_dict({
        "project": {"name": "my-cli", "purpose": "CLI"},
        "architecture": {"style": "library"},
        "interfaces": [{"type": "cli"}],
        "language": {"name": "python", "version": "3.9"},
        "framework": {"name": "click", "version": "8.1"},
    })


def _go_config() -> ProjectConfig:
    return ProjectConfig.from_dict({
        "project": {"name": "go-svc", "purpose": "Go service"},
        "architecture": {"style": "microservice"},
        "interfaces": [{"type": "rest"}],
        "language": {"name": "go", "version": "1.21"},
        "framework": {"name": "gin", "version": "1.9", "build_tool": "go"},
    })


class TestHooksAssemblerAssemble:
    def test_assemble_javaMavenConfig_returnsOneHookPath(self, tmp_path):
        src = _create_hook_src(tmp_path / "src", "java-maven")
        out = tmp_path / "output"
        out.mkdir()
        engine = TemplateEngine(src, _java_maven_config())
        asm = HooksAssembler(src)
        result = asm.assemble(_java_maven_config(), out, engine)
        assert len(result) == 1
        assert result[0].name == "post-compile-check.sh"
        assert result[0].is_file()

    def test_assemble_javaMavenConfig_createsHooksDir(self, tmp_path):
        src = _create_hook_src(tmp_path / "src", "java-maven")
        out = tmp_path / "output"
        out.mkdir()
        engine = TemplateEngine(src, _java_maven_config())
        asm = HooksAssembler(src)
        asm.assemble(_java_maven_config(), out, engine)
        assert (out / "hooks").is_dir()

    def test_assemble_javaMavenConfig_setsExecutablePermission(self, tmp_path):
        src = _create_hook_src(tmp_path / "src", "java-maven")
        out = tmp_path / "output"
        out.mkdir()
        engine = TemplateEngine(src, _java_maven_config())
        asm = HooksAssembler(src)
        result = asm.assemble(_java_maven_config(), out, engine)
        assert os.access(result[0], os.X_OK)

    def test_assemble_pythonConfig_returnsEmptyList(self, tmp_path):
        src = tmp_path / "src"
        src.mkdir()
        out = tmp_path / "output"
        out.mkdir()
        engine = TemplateEngine(src, _python_config())
        asm = HooksAssembler(src)
        result = asm.assemble(_python_config(), out, engine)
        assert result == []

    def test_assemble_pythonConfig_noHooksDirCreated(self, tmp_path):
        src = tmp_path / "src"
        src.mkdir()
        out = tmp_path / "output"
        out.mkdir()
        engine = TemplateEngine(src, _python_config())
        asm = HooksAssembler(src)
        asm.assemble(_python_config(), out, engine)
        assert not (out / "hooks").exists()

    def test_assemble_goConfig_returnsOneHookPath(self, tmp_path):
        src = _create_hook_src(tmp_path / "src", "go")
        out = tmp_path / "output"
        out.mkdir()
        engine = TemplateEngine(src, _go_config())
        asm = HooksAssembler(src)
        result = asm.assemble(_go_config(), out, engine)
        assert len(result) == 1

    def test_assemble_missingTemplate_returnsEmptyList(self, tmp_path):
        src = tmp_path / "src"
        src.mkdir()
        out = tmp_path / "output"
        out.mkdir()
        config = _java_maven_config()
        engine = TemplateEngine(src, config)
        asm = HooksAssembler(src)
        result = asm.assemble(config, out, engine)
        assert result == []

    def test_assemble_javaMavenConfig_contentMatchesSource(self, tmp_path):
        src = _create_hook_src(tmp_path / "src", "java-maven")
        out = tmp_path / "output"
        out.mkdir()
        engine = TemplateEngine(src, _java_maven_config())
        asm = HooksAssembler(src)
        result = asm.assemble(_java_maven_config(), out, engine)
        source = src / "hooks-templates" / "java-maven" / "post-compile-check.sh"
        assert result[0].read_bytes() == source.read_bytes()
