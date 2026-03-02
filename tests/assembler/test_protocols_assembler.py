from __future__ import annotations

from pathlib import Path

import pytest

from claude_setup.assembler.protocols_assembler import (
    PROTOCOL_SEPARATOR,
    ProtocolsAssembler,
)
from claude_setup.models import ProjectConfig
from claude_setup.template_engine import TemplateEngine

FIXTURES_SRC = Path(__file__).parent.parent / "fixtures" / "src"


@pytest.fixture
def assembler() -> ProtocolsAssembler:
    return ProtocolsAssembler(FIXTURES_SRC)


@pytest.fixture
def full_config(full_project_dict: dict) -> ProjectConfig:
    return ProjectConfig.from_dict(full_project_dict)


@pytest.fixture
def engine(full_config: ProjectConfig) -> TemplateEngine:
    return TemplateEngine(FIXTURES_SRC, full_config)


class TestProtocolsAssemblerAssemble:

    def test_assemble_rest_grpc_generates_files(
        self,
        create_project_config,
        tmp_path: Path,
    ) -> None:
        config = create_project_config(
            interfaces=[
                {"type": "rest"},
                {"type": "grpc"},
            ],
        )
        asm = ProtocolsAssembler(FIXTURES_SRC)
        eng = TemplateEngine(FIXTURES_SRC, config)
        results = asm.assemble(config, tmp_path, eng)
        assert len(results) == 2
        assert all(p.exists() for p in results)

    def test_assemble_cli_only_returns_empty(
        self,
        create_project_config,
        tmp_path: Path,
    ) -> None:
        config = create_project_config(
            interfaces=[{"type": "cli"}],
        )
        asm = ProtocolsAssembler(FIXTURES_SRC)
        eng = TemplateEngine(FIXTURES_SRC, config)
        results = asm.assemble(config, tmp_path, eng)
        assert results == []

    def test_assemble_event_consumer_kafka(
        self,
        create_project_config,
        tmp_path: Path,
    ) -> None:
        config = create_project_config(
            interfaces=[
                {"type": "event-consumer", "broker": "kafka"},
            ],
        )
        asm = ProtocolsAssembler(FIXTURES_SRC)
        eng = TemplateEngine(FIXTURES_SRC, config)
        results = asm.assemble(config, tmp_path, eng)
        assert len(results) >= 1
        names = [p.name for p in results]
        assert "event-driven-conventions.md" in names
        assert "messaging-conventions.md" in names

    def test_assemble_creates_references_dir(
        self,
        create_project_config,
        tmp_path: Path,
    ) -> None:
        config = create_project_config(
            interfaces=[{"type": "rest"}],
        )
        asm = ProtocolsAssembler(FIXTURES_SRC)
        eng = TemplateEngine(FIXTURES_SRC, config)
        asm.assemble(config, tmp_path, eng)
        refs = (
            tmp_path / "skills" / "protocols" / "references"
        )
        assert refs.is_dir()

    def test_assemble_missing_source_returns_empty(
        self,
        create_project_config,
        tmp_path: Path,
    ) -> None:
        config = create_project_config(
            interfaces=[{"type": "rest"}],
        )
        asm = ProtocolsAssembler(Path("/tmp/nonexistent"))
        eng = TemplateEngine(FIXTURES_SRC, config)
        results = asm.assemble(config, tmp_path, eng)
        assert results == []


class TestConcatProtocolDir:

    def test_concat_merges_sorted_files(
        self,
        assembler: ProtocolsAssembler,
        tmp_path: Path,
    ) -> None:
        files = sorted(
            (FIXTURES_SRC / "protocols" / "messaging").glob("*.md"),
        )
        dest = tmp_path / "messaging-conventions.md"
        result = assembler._concat_protocol_dir(files, dest)
        assert result == dest
        content = dest.read_text()
        assert "Kafka" in content
        assert "RabbitMQ" in content

    def test_concat_separator_format(
        self,
        assembler: ProtocolsAssembler,
        tmp_path: Path,
    ) -> None:
        files = sorted(
            (FIXTURES_SRC / "protocols" / "messaging").glob("*.md"),
        )
        dest = tmp_path / "messaging-conventions.md"
        assembler._concat_protocol_dir(files, dest)
        content = dest.read_text()
        assert PROTOCOL_SEPARATOR in content

    def test_concat_single_file_no_separator(
        self,
        assembler: ProtocolsAssembler,
        tmp_path: Path,
    ) -> None:
        files = [
            FIXTURES_SRC / "protocols" / "rest"
            / "openapi-conventions.md",
        ]
        dest = tmp_path / "rest-conventions.md"
        assembler._concat_protocol_dir(files, dest)
        content = dest.read_text()
        assert PROTOCOL_SEPARATOR not in content
        assert "OpenAPI" in content

    def test_concat_empty_list(
        self,
        assembler: ProtocolsAssembler,
        tmp_path: Path,
    ) -> None:
        dest = tmp_path / "empty-conventions.md"
        assembler._concat_protocol_dir([], dest)
        assert dest.read_text() == ""
