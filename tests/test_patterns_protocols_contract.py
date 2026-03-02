from __future__ import annotations

import copy
from pathlib import Path

import pytest

from claude_setup.assembler.patterns_assembler import (
    PatternsAssembler,
)
from claude_setup.assembler.protocols_assembler import (
    ProtocolsAssembler,
)
from claude_setup.models import ProjectConfig
from claude_setup.template_engine import TemplateEngine

FIXTURES_DIR = Path(__file__).parent / "fixtures"
FIXTURES_SRC = FIXTURES_DIR / "src"
REFERENCE_DIR = FIXTURES_DIR / "reference"

MICROSERVICE_CONFIG_DICT = {
    "project": {
        "name": "contract-service",
        "purpose": "Contract test service",
    },
    "architecture": {
        "style": "microservice",
        "domain_driven": True,
        "event_driven": False,
    },
    "interfaces": [
        {"type": "rest", "spec": "openapi-3.1"},
        {"type": "grpc", "spec": "proto3"},
    ],
    "language": {"name": "python", "version": "3.9"},
    "framework": {
        "name": "click",
        "version": "8.1",
        "build_tool": "pip",
    },
}


@pytest.fixture
def microservice_config() -> ProjectConfig:
    return ProjectConfig.from_dict(
        copy.deepcopy(MICROSERVICE_CONFIG_DICT),
    )


@pytest.fixture
def microservice_engine(
    microservice_config: ProjectConfig,
) -> TemplateEngine:
    return TemplateEngine(FIXTURES_SRC, microservice_config)


def _normalize_content(content: str) -> str:
    """Normalize line endings for cross-platform comparison."""
    return content.replace("\r\n", "\n").rstrip("\n")


class TestContractPatterns:

    def test_contract_patterns_output_matches_reference(
        self,
        microservice_config: ProjectConfig,
        microservice_engine: TemplateEngine,
        tmp_path: Path,
    ) -> None:
        asm = PatternsAssembler(FIXTURES_SRC)
        asm.assemble(
            microservice_config, tmp_path, microservice_engine,
        )
        consolidated = (
            tmp_path / "skills" / "patterns" / "SKILL.md"
        )
        assert consolidated.exists()
        reference = (
            REFERENCE_DIR / "patterns" / "consolidated.md"
        )
        actual = _normalize_content(consolidated.read_text())
        expected = _normalize_content(reference.read_text())
        assert actual == expected

    def test_contract_patterns_individual_files_exist(
        self,
        microservice_config: ProjectConfig,
        microservice_engine: TemplateEngine,
        tmp_path: Path,
    ) -> None:
        asm = PatternsAssembler(FIXTURES_SRC)
        results = asm.assemble(
            microservice_config, tmp_path, microservice_engine,
        )
        md_results = [p for p in results if p.name != "SKILL.md"]
        assert len(md_results) >= 4


class TestContractProtocols:

    def test_contract_protocols_output_matches_reference(
        self,
        microservice_config: ProjectConfig,
        microservice_engine: TemplateEngine,
        tmp_path: Path,
    ) -> None:
        asm = ProtocolsAssembler(FIXTURES_SRC)
        asm.assemble(
            microservice_config, tmp_path, microservice_engine,
        )
        refs_dir = (
            tmp_path / "skills" / "protocols" / "references"
        )
        rest_file = refs_dir / "rest-conventions.md"
        assert rest_file.exists()
        reference = (
            REFERENCE_DIR / "protocols" / "rest-conventions.md"
        )
        actual = _normalize_content(rest_file.read_text())
        expected = _normalize_content(reference.read_text())
        assert actual == expected

    def test_contract_protocols_grpc_matches_reference(
        self,
        microservice_config: ProjectConfig,
        microservice_engine: TemplateEngine,
        tmp_path: Path,
    ) -> None:
        asm = ProtocolsAssembler(FIXTURES_SRC)
        asm.assemble(
            microservice_config, tmp_path, microservice_engine,
        )
        refs_dir = (
            tmp_path / "skills" / "protocols" / "references"
        )
        grpc_file = refs_dir / "grpc-conventions.md"
        assert grpc_file.exists()
        reference = (
            REFERENCE_DIR / "protocols" / "grpc-conventions.md"
        )
        actual = _normalize_content(grpc_file.read_text())
        expected = _normalize_content(reference.read_text())
        assert actual == expected
