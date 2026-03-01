from __future__ import annotations

from pathlib import Path
from typing import List

import pytest

from claude_setup.assembler.consolidator import (
    CORE_PATTERNS,
    DATA_PATTERNS,
    GENERATED_HEADER,
    OPS_PATTERNS,
    consolidate_files,
    consolidate_framework_rules,
)


class TestConsolidateFiles:

    def test_consolidate_two_files_with_separator(
        self, tmp_path: Path,
    ) -> None:
        src1 = tmp_path / "a.md"
        src2 = tmp_path / "b.md"
        src1.write_text("Content A")
        src2.write_text("Content B")
        output = tmp_path / "out" / "merged.md"
        consolidate_files(output, [src1, src2])
        text = output.read_text()
        assert GENERATED_HEADER in text
        assert "Content A" in text
        assert "Content B" in text
        assert "---" in text

    def test_consolidate_creates_parent_dirs(
        self, tmp_path: Path,
    ) -> None:
        src = tmp_path / "src.md"
        src.write_text("data")
        output = tmp_path / "deep" / "nested" / "out.md"
        consolidate_files(output, [src])
        assert output.is_file()

    def test_consolidate_skips_missing_sources(
        self, tmp_path: Path,
    ) -> None:
        existing = tmp_path / "exists.md"
        existing.write_text("real content")
        missing = tmp_path / "missing.md"
        output = tmp_path / "out.md"
        consolidate_files(output, [existing, missing])
        text = output.read_text()
        assert "real content" in text

    def test_consolidate_single_file_no_extra_separator(
        self, tmp_path: Path,
    ) -> None:
        src = tmp_path / "only.md"
        src.write_text("single content")
        output = tmp_path / "out.md"
        consolidate_files(output, [src])
        text = output.read_text()
        assert "single content" in text
        assert text.count("---") == 1

    def test_consolidate_empty_sources_produces_no_output(
        self, tmp_path: Path,
    ) -> None:
        output = tmp_path / "out.md"
        consolidate_files(output, [])
        assert not output.exists()


class TestConsolidateFrameworkRules:

    def test_consolidate_framework_produces_three_files(
        self, tmp_path: Path,
    ) -> None:
        src_dir = tmp_path / "src"
        src_dir.mkdir()
        rules_dir = tmp_path / "rules"
        rules_dir.mkdir()
        (src_dir / "quarkus-cdi.md").write_text("CDI content")
        (src_dir / "quarkus-jpa.md").write_text("JPA content")
        (src_dir / "quarkus-testing.md").write_text("Testing content")
        result = consolidate_framework_rules("quarkus", rules_dir, src_dir)
        assert len(result) == 3
        names = [p.name for p in result]
        assert "30-quarkus-core.md" in names
        assert "31-quarkus-data.md" in names
        assert "32-quarkus-operations.md" in names

    def test_consolidate_framework_empty_group_not_created(
        self, tmp_path: Path,
    ) -> None:
        src_dir = tmp_path / "src"
        src_dir.mkdir()
        rules_dir = tmp_path / "rules"
        rules_dir.mkdir()
        (src_dir / "quarkus-cdi.md").write_text("CDI only")
        result = consolidate_framework_rules("quarkus", rules_dir, src_dir)
        assert len(result) == 1
        assert result[0].name == "30-quarkus-core.md"

    def test_consolidate_framework_nonexistent_source_returns_empty(
        self, tmp_path: Path,
    ) -> None:
        rules_dir = tmp_path / "rules"
        rules_dir.mkdir()
        missing = tmp_path / "missing"
        result = consolidate_framework_rules("quarkus", rules_dir, missing)
        assert result == []

    @pytest.mark.parametrize(
        "filename, expected_group",
        [
            ("fw-cdi-patterns.md", "core"),
            ("fw-di-container.md", "core"),
            ("fw-config-guide.md", "core"),
            ("fw-web-setup.md", "core"),
            ("fw-resteasy-ref.md", "core"),
            ("fw-middleware-chain.md", "core"),
            ("fw-resilience-patterns.md", "core"),
            ("fw-jpa-guide.md", "data"),
            ("fw-panache-ref.md", "data"),
            ("fw-prisma-setup.md", "data"),
            ("fw-sqlalchemy-orm.md", "data"),
            ("fw-exposed-dsl.md", "data"),
            ("fw-ef-core.md", "data"),
            ("fw-orm-patterns.md", "data"),
            ("fw-database-setup.md", "data"),
            ("fw-testing-guide.md", "operations"),
            ("fw-observability-setup.md", "operations"),
            ("fw-native-build.md", "operations"),
            ("fw-infrastructure-deploy.md", "operations"),
        ],
        ids=[
            "cdi", "di", "config", "web", "resteasy",
            "middleware", "resilience",
            "jpa", "panache", "prisma", "sqlalchemy",
            "exposed", "ef", "orm", "database",
            "testing", "observability", "native-build", "infrastructure",
        ],
    )
    def test_framework_file_routed_to_correct_group(
        self,
        tmp_path: Path,
        filename: str,
        expected_group: str,
    ) -> None:
        src_dir = tmp_path / "src"
        src_dir.mkdir()
        rules_dir = tmp_path / "rules"
        rules_dir.mkdir()
        (src_dir / filename).write_text(f"{expected_group} content")
        result = consolidate_framework_rules("fw", rules_dir, src_dir)
        assert len(result) == 1
        group_prefixes = {"core": "30", "data": "31", "operations": "32"}
        prefix = group_prefixes[expected_group]
        assert result[0].name.startswith(prefix)
