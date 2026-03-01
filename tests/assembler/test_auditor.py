from __future__ import annotations

from pathlib import Path

from claude_setup.assembler.auditor import (
    MAX_FILE_COUNT,
    MAX_TOTAL_BYTES,
    AuditResult,
    audit_rules_context,
)


class TestAuditResult:
    def test_is_frozen(self, tmp_path: Path) -> None:
        result = audit_rules_context(tmp_path)
        try:
            result.total_files = 99  # type: ignore[misc]
            assert False, "Expected FrozenInstanceError"
        except AttributeError:
            pass


class TestAuditRulesContext:
    def test_empty_dir_returns_zeros(self, tmp_path: Path) -> None:
        result = audit_rules_context(tmp_path)
        assert result.total_files == 0
        assert result.total_bytes == 0
        assert result.file_sizes == []
        assert result.warnings == []

    def test_nonexistent_dir_handles_gracefully(self, tmp_path: Path) -> None:
        result = audit_rules_context(tmp_path / "nonexistent")
        assert result.total_files == 0

    def test_under_thresholds_no_warnings(self, tmp_path: Path) -> None:
        for i in range(3):
            (tmp_path / f"rule-{i}.md").write_text("content", encoding="utf-8")
        result = audit_rules_context(tmp_path)
        assert result.total_files == 3
        assert result.warnings == []

    def test_exceeds_file_count_warns(self, tmp_path: Path) -> None:
        for i in range(MAX_FILE_COUNT + 1):
            (tmp_path / f"rule-{i:02d}.md").write_text("x", encoding="utf-8")
        result = audit_rules_context(tmp_path)
        assert result.total_files == MAX_FILE_COUNT + 1
        assert len(result.warnings) == 1
        assert "exceeds" in result.warnings[0]

    def test_exceeds_total_size_warns(self, tmp_path: Path) -> None:
        big_content = "x" * (MAX_TOTAL_BYTES + 1)
        (tmp_path / "big.md").write_text(big_content, encoding="utf-8")
        result = audit_rules_context(tmp_path)
        assert len(result.warnings) == 1
        assert "50KB" in result.warnings[0]

    def test_both_thresholds_two_warnings(self, tmp_path: Path) -> None:
        big_content = "x" * (MAX_TOTAL_BYTES + 1)
        for i in range(MAX_FILE_COUNT + 1):
            (tmp_path / f"rule-{i:02d}.md").write_text(big_content, encoding="utf-8")
        result = audit_rules_context(tmp_path)
        assert len(result.warnings) == 2

    def test_file_sizes_sorted_descending(self, tmp_path: Path) -> None:
        (tmp_path / "small.md").write_text("a", encoding="utf-8")
        (tmp_path / "big.md").write_text("a" * 100, encoding="utf-8")
        result = audit_rules_context(tmp_path)
        assert result.file_sizes[0][0] == "big.md"
        assert result.file_sizes[1][0] == "small.md"
