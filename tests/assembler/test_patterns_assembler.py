from __future__ import annotations

from pathlib import Path

import pytest

from ia_dev_env.assembler.patterns_assembler import (
    CONSOLIDATED_FILENAME,
    SECTION_SEPARATOR,
    PatternsAssembler,
)
from ia_dev_env.models import ProjectConfig
from ia_dev_env.template_engine import TemplateEngine

FIXTURES_SRC = Path(__file__).parent.parent / "fixtures" / "src"


@pytest.fixture
def assembler() -> PatternsAssembler:
    return PatternsAssembler(FIXTURES_SRC)


@pytest.fixture
def full_config(full_project_dict: dict) -> ProjectConfig:
    return ProjectConfig.from_dict(full_project_dict)


@pytest.fixture
def engine(full_config: ProjectConfig) -> TemplateEngine:
    return TemplateEngine(FIXTURES_SRC, full_config)


class TestPatternsAssemblerAssemble:

    def test_assemble_hexagonal_generates_files(
        self,
        assembler: PatternsAssembler,
        full_config: ProjectConfig,
        engine: TemplateEngine,
        tmp_path: Path,
    ) -> None:
        results = assembler.assemble(
            full_config, tmp_path, engine,
        )
        assert len(results) > 0
        assert all(p.exists() for p in results)

    def test_assemble_library_generates_minimal(
        self,
        minimal_project_dict: dict,
        tmp_path: Path,
    ) -> None:
        config = ProjectConfig.from_dict(minimal_project_dict)
        asm = PatternsAssembler(FIXTURES_SRC)
        eng = TemplateEngine(FIXTURES_SRC, config)
        results = asm.assemble(config, tmp_path, eng)
        assert len(results) > 0
        names = [p.name for p in results]
        assert CONSOLIDATED_FILENAME in names

    def test_assemble_unknown_style_returns_empty(
        self,
        create_project_config,
        tmp_path: Path,
    ) -> None:
        config = create_project_config(
            architecture={"style": "serverless"},
        )
        asm = PatternsAssembler(FIXTURES_SRC)
        eng = TemplateEngine(FIXTURES_SRC, config)
        results = asm.assemble(config, tmp_path, eng)
        assert results == []

    def test_assemble_missing_source_returns_empty(
        self,
        full_config: ProjectConfig,
        engine: TemplateEngine,
        tmp_path: Path,
    ) -> None:
        asm = PatternsAssembler(Path("/tmp/nonexistent"))
        results = asm.assemble(full_config, tmp_path, engine)
        assert results == []

    def test_assemble_creates_references_dirs(
        self,
        assembler: PatternsAssembler,
        full_config: ProjectConfig,
        engine: TemplateEngine,
        tmp_path: Path,
    ) -> None:
        assembler.assemble(full_config, tmp_path, engine)
        refs_dir = (
            tmp_path / "skills" / "patterns" / "references"
        )
        assert refs_dir.is_dir()

    def test_assemble_replaces_placeholders(
        self,
        assembler: PatternsAssembler,
        full_config: ProjectConfig,
        engine: TemplateEngine,
        tmp_path: Path,
    ) -> None:
        assembler.assemble(full_config, tmp_path, engine)
        refs = tmp_path / "skills" / "patterns" / "references"
        arch_file = refs / "architectural" / "hexagonal-architecture.md"
        content = arch_file.read_text()
        assert "{architecture_style}" not in content
        assert "hexagonal" in content


class TestFlushPatterns:

    def test_flush_copies_to_category_dirs(
        self,
        assembler: PatternsAssembler,
        engine: TemplateEngine,
        tmp_path: Path,
    ) -> None:
        src_files = [
            FIXTURES_SRC / "patterns" / "architectural"
            / "hexagonal-architecture.md",
            FIXTURES_SRC / "patterns" / "data"
            / "repository-pattern.md",
        ]
        rendered = assembler._render_contents(src_files, engine)
        dest = tmp_path / "refs"
        results = assembler._flush_patterns(
            src_files, rendered, dest,
        )
        assert len(results) == 2
        assert (dest / "architectural" / "hexagonal-architecture.md").exists()
        assert (dest / "data" / "repository-pattern.md").exists()


class TestFlushConsolidated:

    def test_flush_consolidated_merges_files(
        self,
        assembler: PatternsAssembler,
        engine: TemplateEngine,
        tmp_path: Path,
    ) -> None:
        src_files = [
            FIXTURES_SRC / "patterns" / "architectural"
            / "hexagonal-architecture.md",
            FIXTURES_SRC / "patterns" / "data"
            / "repository-pattern.md",
        ]
        rendered = assembler._render_contents(src_files, engine)
        dest = tmp_path / "SKILL.md"
        result = assembler._flush_consolidated(rendered, dest)
        assert result == dest
        content = dest.read_text()
        assert "Hexagonal Architecture" in content
        assert "Repository Pattern" in content

    def test_flush_consolidated_separator_format(
        self,
        assembler: PatternsAssembler,
        engine: TemplateEngine,
        tmp_path: Path,
    ) -> None:
        src_files = [
            FIXTURES_SRC / "patterns" / "architectural"
            / "hexagonal-architecture.md",
            FIXTURES_SRC / "patterns" / "data"
            / "repository-pattern.md",
        ]
        rendered = assembler._render_contents(src_files, engine)
        dest = tmp_path / "SKILL.md"
        assembler._flush_consolidated(rendered, dest)
        content = dest.read_text()
        assert SECTION_SEPARATOR in content

    def test_flush_consolidated_single_no_separator(
        self,
        assembler: PatternsAssembler,
        engine: TemplateEngine,
        tmp_path: Path,
    ) -> None:
        src_files = [
            FIXTURES_SRC / "patterns" / "architectural"
            / "hexagonal-architecture.md",
        ]
        rendered = assembler._render_contents(src_files, engine)
        dest = tmp_path / "SKILL.md"
        assembler._flush_consolidated(rendered, dest)
        content = dest.read_text()
        assert SECTION_SEPARATOR not in content
