from __future__ import annotations

from pathlib import Path
from typing import Dict

import pytest

from claude_setup.verifier import (
    _collect_relative_paths,
    verify_output,
)


def _create_file_tree(base: Path, files: Dict[str, str]) -> None:
    """Create files under base directory from dict."""
    for rel_path, content in files.items():
        full_path = base / rel_path
        full_path.parent.mkdir(parents=True, exist_ok=True)
        full_path.write_text(content, encoding="utf-8")


def _create_binary_file(path: Path, data: bytes) -> None:
    """Write binary content to a file."""
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)


class TestVerifyOutput:

    def test_identical_dirs_returns_success(
        self, tmp_path: Path,
    ) -> None:
        python_dir = tmp_path / "python"
        ref_dir = tmp_path / "reference"
        files = {"a.txt": "hello", "b.txt": "world"}
        _create_file_tree(python_dir, files)
        _create_file_tree(ref_dir, files)
        result = verify_output(python_dir, ref_dir)
        assert result.success is True

    def test_identical_dirs_total_files_correct(
        self, tmp_path: Path,
    ) -> None:
        python_dir = tmp_path / "python"
        ref_dir = tmp_path / "reference"
        files = {"a.txt": "a", "b.txt": "b", "c.txt": "c"}
        _create_file_tree(python_dir, files)
        _create_file_tree(ref_dir, files)
        result = verify_output(python_dir, ref_dir)
        assert result.total_files == 3

    def test_mismatch_detected_returns_failure(
        self, tmp_path: Path,
    ) -> None:
        python_dir = tmp_path / "python"
        ref_dir = tmp_path / "reference"
        _create_file_tree(python_dir, {"a.txt": "new"})
        _create_file_tree(ref_dir, {"a.txt": "old"})
        result = verify_output(python_dir, ref_dir)
        assert result.success is False
        assert len(result.mismatches) == 1

    def test_mismatch_contains_diff_string(
        self, tmp_path: Path,
    ) -> None:
        python_dir = tmp_path / "python"
        ref_dir = tmp_path / "reference"
        _create_file_tree(python_dir, {"a.txt": "new\n"})
        _create_file_tree(ref_dir, {"a.txt": "old\n"})
        result = verify_output(python_dir, ref_dir)
        diff_text = result.mismatches[0].diff
        assert "---" in diff_text
        assert "+++" in diff_text

    def test_mismatch_contains_file_sizes(
        self, tmp_path: Path,
    ) -> None:
        python_dir = tmp_path / "python"
        ref_dir = tmp_path / "reference"
        _create_file_tree(python_dir, {"a.txt": "short"})
        _create_file_tree(ref_dir, {"a.txt": "much longer text"})
        result = verify_output(python_dir, ref_dir)
        m = result.mismatches[0]
        assert m.python_size == 5
        assert m.reference_size == 16

    def test_missing_file_detected(
        self, tmp_path: Path,
    ) -> None:
        python_dir = tmp_path / "python"
        ref_dir = tmp_path / "reference"
        _create_file_tree(python_dir, {"a.txt": "a"})
        _create_file_tree(ref_dir, {"a.txt": "a", "b.txt": "b"})
        result = verify_output(python_dir, ref_dir)
        assert result.success is False
        assert Path("b.txt") in result.missing_files

    def test_extra_file_detected(
        self, tmp_path: Path,
    ) -> None:
        python_dir = tmp_path / "python"
        ref_dir = tmp_path / "reference"
        _create_file_tree(python_dir, {"a.txt": "a", "c.txt": "c"})
        _create_file_tree(ref_dir, {"a.txt": "a"})
        result = verify_output(python_dir, ref_dir)
        assert result.success is False
        assert Path("c.txt") in result.extra_files

    def test_nested_directories_compared(
        self, tmp_path: Path,
    ) -> None:
        python_dir = tmp_path / "python"
        ref_dir = tmp_path / "reference"
        files = {"sub/dir/file.txt": "content"}
        _create_file_tree(python_dir, files)
        _create_file_tree(ref_dir, files)
        result = verify_output(python_dir, ref_dir)
        assert result.success is True
        assert result.total_files == 1

    def test_empty_dirs_returns_success(
        self, tmp_path: Path,
    ) -> None:
        python_dir = tmp_path / "python"
        ref_dir = tmp_path / "reference"
        python_dir.mkdir()
        ref_dir.mkdir()
        result = verify_output(python_dir, ref_dir)
        assert result.success is True
        assert result.total_files == 0

    def test_binary_file_mismatch_handled(
        self, tmp_path: Path,
    ) -> None:
        python_dir = tmp_path / "python"
        ref_dir = tmp_path / "reference"
        python_dir.mkdir()
        ref_dir.mkdir()
        _create_binary_file(
            python_dir / "img.bin", b"\x00\x01\x02",
        )
        _create_binary_file(
            ref_dir / "img.bin", b"\x00\x01\xff",
        )
        result = verify_output(python_dir, ref_dir)
        assert result.success is False
        assert len(result.mismatches) == 1
        assert "binary" in result.mismatches[0].diff.lower()

    def test_whitespace_difference_detected(
        self, tmp_path: Path,
    ) -> None:
        python_dir = tmp_path / "python"
        ref_dir = tmp_path / "reference"
        _create_file_tree(python_dir, {"a.txt": "hello "})
        _create_file_tree(ref_dir, {"a.txt": "hello"})
        result = verify_output(python_dir, ref_dir)
        assert result.success is False
        assert len(result.mismatches) == 1


class TestCollectRelativePaths:

    def test_returns_sorted_paths(
        self, tmp_path: Path,
    ) -> None:
        files = {"c.txt": "c", "a.txt": "a", "b.txt": "b"}
        _create_file_tree(tmp_path, files)
        paths = _collect_relative_paths(tmp_path)
        assert paths == [Path("a.txt"), Path("b.txt"), Path("c.txt")]

    def test_includes_nested_files(
        self, tmp_path: Path,
    ) -> None:
        files = {"root.txt": "r", "sub/nested.txt": "n"}
        _create_file_tree(tmp_path, files)
        paths = _collect_relative_paths(tmp_path)
        assert Path("sub/nested.txt") in paths

    def test_empty_dir_returns_empty(
        self, tmp_path: Path,
    ) -> None:
        paths = _collect_relative_paths(tmp_path)
        assert paths == []
