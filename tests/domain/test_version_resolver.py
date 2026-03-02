from __future__ import annotations

from pathlib import Path

from claude_setup.domain.version_resolver import find_version_dir


class TestVersionResolver:

    def test_find_version_dir_exact_match_returns_path(
        self, tmp_path: Path,
    ) -> None:
        exact_dir = tmp_path / "python-3.9"
        exact_dir.mkdir()
        result = find_version_dir(tmp_path, "python", "3.9")
        assert result == exact_dir

    def test_find_version_dir_major_fallback_returns_path(
        self, tmp_path: Path,
    ) -> None:
        fallback_dir = tmp_path / "python-3.x"
        fallback_dir.mkdir()
        result = find_version_dir(tmp_path, "python", "3.9")
        assert result == fallback_dir

    def test_find_version_dir_no_match_returns_none(
        self, tmp_path: Path,
    ) -> None:
        result = find_version_dir(tmp_path, "python", "3.9")
        assert result is None

    def test_find_version_dir_exact_preferred_over_fallback(
        self, tmp_path: Path,
    ) -> None:
        exact_dir = tmp_path / "python-3.9"
        exact_dir.mkdir()
        fallback_dir = tmp_path / "python-3.x"
        fallback_dir.mkdir()
        result = find_version_dir(tmp_path, "python", "3.9")
        assert result == exact_dir

    def test_find_version_dir_multi_dot_version_extracts_major(
        self, tmp_path: Path,
    ) -> None:
        fallback_dir = tmp_path / "java-3.x"
        fallback_dir.mkdir()
        result = find_version_dir(tmp_path, "java", "3.9.1")
        assert result == fallback_dir

    def test_find_version_dir_single_segment_version(
        self, tmp_path: Path,
    ) -> None:
        exact_dir = tmp_path / "java-21"
        exact_dir.mkdir()
        result = find_version_dir(tmp_path, "java", "21")
        assert result == exact_dir

    def test_find_version_dir_single_segment_fallback(
        self, tmp_path: Path,
    ) -> None:
        fallback_dir = tmp_path / "java-21.x"
        fallback_dir.mkdir()
        result = find_version_dir(tmp_path, "java", "21")
        assert result == fallback_dir
