from __future__ import annotations

from pathlib import Path

import pytest

from ia_dev_env.assembler.auditor import (
    MAX_FILE_COUNT,
    MAX_TOTAL_BYTES,
    AuditResult,
    audit_rules_context,
)

SMALL_FILE_BYTES = 100
LARGE_FILE_BYTES = 6000


class TestAuditResult:

    def test_audit_result_is_frozen(self) -> None:
        result = AuditResult(
            total_files=0, total_bytes=0, file_sizes=[], warnings=[],
        )
        with pytest.raises(AttributeError):
            result.total_files = 5  # type: ignore[misc]


class TestAuditRulesContext:

    def test_audit_empty_dir_returns_zeros(self, tmp_path: Path) -> None:
        result = audit_rules_context(tmp_path)
        assert result.total_files == 0
        assert result.total_bytes == 0
        assert result.file_sizes == []
        assert result.warnings == []

    def test_audit_nonexistent_dir_handles_gracefully(
        self, tmp_path: Path,
    ) -> None:
        missing = tmp_path / "nonexistent"
        result = audit_rules_context(missing)
        assert result.total_files == 0
        assert result.total_bytes == 0

    def test_audit_under_thresholds_no_warnings(
        self, tmp_path: Path,
    ) -> None:
        _create_files(tmp_path, count=5, size=SMALL_FILE_BYTES)
        result = audit_rules_context(tmp_path)
        assert result.total_files == 5
        assert result.total_bytes == 5 * SMALL_FILE_BYTES
        assert result.warnings == []

    def test_audit_exceeds_file_count_warns(
        self, tmp_path: Path,
    ) -> None:
        file_count = MAX_FILE_COUNT + 1
        _create_files(tmp_path, count=file_count, size=SMALL_FILE_BYTES)
        result = audit_rules_context(tmp_path)
        assert result.total_files == file_count
        assert len(result.warnings) == 1
        assert str(file_count) in result.warnings[0]

    def test_audit_exceeds_total_size_warns(
        self, tmp_path: Path,
    ) -> None:
        _create_files(tmp_path, count=5, size=MAX_TOTAL_BYTES // 5 + 1024)
        result = audit_rules_context(tmp_path)
        assert result.total_bytes > MAX_TOTAL_BYTES
        assert any("50KB" in w for w in result.warnings)

    def test_audit_both_thresholds_two_warnings(
        self, tmp_path: Path,
    ) -> None:
        file_count = MAX_FILE_COUNT + 1
        size_each = MAX_TOTAL_BYTES // file_count + 1024
        _create_files(tmp_path, count=file_count, size=size_each)
        result = audit_rules_context(tmp_path)
        assert len(result.warnings) == 2

    def test_audit_file_sizes_sorted_descending(
        self, tmp_path: Path,
    ) -> None:
        (tmp_path / "small.md").write_text("x" * 10)
        (tmp_path / "big.md").write_text("x" * 1000)
        (tmp_path / "medium.md").write_text("x" * 500)
        result = audit_rules_context(tmp_path)
        sizes = [s for _, s in result.file_sizes]
        assert sizes == sorted(sizes, reverse=True)
        assert result.file_sizes[0][0] == "big.md"


def _create_files(directory: Path, count: int, size: int) -> None:
    """Create count .md files of given size."""
    content = "x" * size
    for i in range(count):
        (directory / f"rule-{i:02d}.md").write_text(content)
