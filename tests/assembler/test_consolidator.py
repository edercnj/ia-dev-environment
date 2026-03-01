from __future__ import annotations

from pathlib import Path
from typing import List

import pytest

from claude_setup.assembler.consolidator import (
    consolidate_files,
    consolidate_framework_rules,
)


class TestConsolidateFiles:
    def test_two_files_with_separator(self, tmp_path: Path) -> None:
        a = tmp_path / "a.md"
        b = tmp_path / "b.md"
        a.write_text("Alpha", encoding="utf-8")
        b.write_text("Beta", encoding="utf-8")
        out = tmp_path / "out.md"
        consolidate_files(out, [a, b])
        content = out.read_text(encoding="utf-8")
        assert "---" in content
        assert "Alpha" in content
        assert "Beta" in content

    def test_creates_parent_dirs(self, tmp_path: Path) -> None:
        src = tmp_path / "src.md"
        src.write_text("content", encoding="utf-8")
        out = tmp_path / "deep" / "nested" / "out.md"
        consolidate_files(out, [src])
        assert out.exists()

    def test_skips_missing_sources(self, tmp_path: Path) -> None:
        existing = tmp_path / "exists.md"
        existing.write_text("present", encoding="utf-8")
        missing = tmp_path / "missing.md"
        out = tmp_path / "out.md"
        consolidate_files(out, [existing, missing])
        content = out.read_text(encoding="utf-8")
        assert "present" in content

    def test_single_file_no_separator(self, tmp_path: Path) -> None:
        src = tmp_path / "only.md"
        src.write_text("sole content", encoding="utf-8")
        out = tmp_path / "out.md"
        consolidate_files(out, [src])
        content = out.read_text(encoding="utf-8")
        assert "sole content" in content
        lines = content.split("\n")
        assert lines.count("---") == 0

    def test_empty_sources(self, tmp_path: Path) -> None:
        out = tmp_path / "out.md"
        consolidate_files(out, [])
        assert out.exists()


class TestConsolidateFrameworkRules:
    def _create_fw_files(
        self, source_dir: Path, names: List[str],
    ) -> None:
        source_dir.mkdir(parents=True, exist_ok=True)
        for name in names:
            (source_dir / name).write_text(
                f"# {name}\ncontent", encoding="utf-8",
            )

    def test_produces_three_files(self, tmp_path: Path) -> None:
        source = tmp_path / "fw"
        self._create_fw_files(source, [
            "01-quarkus-cdi.md",
            "02-quarkus-jpa.md",
            "03-quarkus-testing.md",
        ])
        rules = tmp_path / "rules"
        rules.mkdir()
        result = consolidate_framework_rules("quarkus", rules, source)
        assert len(result) == 3
        assert any("30-quarkus-core.md" in str(p) for p in result)
        assert any("31-quarkus-data.md" in str(p) for p in result)
        assert any("32-quarkus-operations.md" in str(p) for p in result)

    def test_empty_group_skipped(self, tmp_path: Path) -> None:
        source = tmp_path / "fw"
        self._create_fw_files(source, ["01-quarkus-cdi.md"])
        rules = tmp_path / "rules"
        rules.mkdir()
        result = consolidate_framework_rules("quarkus", rules, source)
        assert len(result) == 1
        assert "30-quarkus-core.md" in str(result[0])

    def test_nonexistent_source_returns_empty(self, tmp_path: Path) -> None:
        rules = tmp_path / "rules"
        rules.mkdir()
        result = consolidate_framework_rules(
            "quarkus", rules, tmp_path / "missing",
        )
        assert result == []

    @pytest.mark.parametrize(
        "filename,expected_group",
        [
            ("01-fw-cdi.md", "core"),
            ("02-fw-di-patterns.md", "core"),
            ("03-fw-config.md", "core"),
            ("04-fw-web.md", "core"),
            ("05-fw-resteasy.md", "core"),
            ("06-fw-middleware.md", "core"),
            ("07-fw-resilience.md", "core"),
            ("10-fw-panache.md", "data"),
            ("11-fw-jpa.md", "data"),
            ("12-fw-prisma.md", "data"),
            ("13-fw-sqlalchemy.md", "data"),
            ("14-fw-exposed.md", "data"),
            ("15-fw-ef-core.md", "data"),
            ("16-fw-orm.md", "data"),
            ("17-fw-database.md", "data"),
            ("20-fw-testing.md", "operations"),
            ("21-fw-observability.md", "operations"),
            ("22-fw-native-build.md", "operations"),
            ("23-fw-infrastructure.md", "operations"),
        ],
        ids=[
            "cdi", "di", "config", "web", "resteasy",
            "middleware", "resilience",
            "panache", "jpa", "prisma", "sqlalchemy",
            "exposed", "ef", "orm", "database",
            "testing", "observability", "native-build", "infrastructure",
        ],
    )
    def test_file_routed_to_correct_group(
        self,
        filename: str,
        expected_group: str,
        tmp_path: Path,
    ) -> None:
        source = tmp_path / "fw"
        self._create_fw_files(source, [filename])
        rules = tmp_path / "rules"
        rules.mkdir()
        result = consolidate_framework_rules("test", rules, source)
        assert len(result) == 1
        num = {"core": "30", "data": "31", "operations": "32"}[expected_group]
        assert f"{num}-test-{expected_group}.md" in result[0].name
