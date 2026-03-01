from __future__ import annotations

from pathlib import Path

from claude_setup.domain.version_resolver import find_version_dir


class TestFindVersionDir:
    def test_exact_match_returns_path(self, tmp_path: Path) -> None:
        exact = tmp_path / "python-3.9"
        exact.mkdir()
        result = find_version_dir(tmp_path, "python", "3.9")
        assert result == exact

    def test_major_fallback_returns_path(self, tmp_path: Path) -> None:
        fallback = tmp_path / "python-3.x"
        fallback.mkdir()
        result = find_version_dir(tmp_path, "python", "3.9")
        assert result == fallback

    def test_exact_preferred_over_fallback(self, tmp_path: Path) -> None:
        exact = tmp_path / "python-3.9"
        exact.mkdir()
        (tmp_path / "python-3.x").mkdir()
        result = find_version_dir(tmp_path, "python", "3.9")
        assert result == exact

    def test_no_match_returns_none(self, tmp_path: Path) -> None:
        result = find_version_dir(tmp_path, "python", "3.9")
        assert result is None

    def test_multi_dot_version_extracts_major(self, tmp_path: Path) -> None:
        fallback = tmp_path / "python-3.x"
        fallback.mkdir()
        result = find_version_dir(tmp_path, "python", "3.9.1")
        assert result == fallback

    def test_single_segment_version(self, tmp_path: Path) -> None:
        exact = tmp_path / "java-21"
        exact.mkdir()
        result = find_version_dir(tmp_path, "java", "21")
        assert result == exact

    def test_single_segment_fallback(self, tmp_path: Path) -> None:
        fallback = tmp_path / "java-21.x"
        fallback.mkdir()
        result = find_version_dir(tmp_path, "java", "21.0.1")
        assert result == fallback
