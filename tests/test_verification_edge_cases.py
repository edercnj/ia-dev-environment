from __future__ import annotations

import copy
from pathlib import Path

import pytest

from claude_setup.assembler import run_pipeline
from claude_setup.models import ProjectConfig
from claude_setup.verifier import verify_output
from tests.conftest import MINIMAL_PROJECT_DICT, create_file_tree

RESOURCES_DIR = Path(__file__).resolve().parent.parent / "resources"


def _make_minimal_config() -> ProjectConfig:
    """Build minimal ProjectConfig for edge case tests."""
    return ProjectConfig.from_dict(
        copy.deepcopy(MINIMAL_PROJECT_DICT),
    )


class TestMinimalConfig:

    def test_minimal_config_produces_output(
        self, tmp_path: Path,
    ) -> None:
        config = _make_minimal_config()
        output_dir = tmp_path / "output"
        result = run_pipeline(config, RESOURCES_DIR, output_dir)
        assert result.success is True
        assert len(result.files_generated) > 0

    def test_minimal_config_verifies_against_self(
        self, tmp_path: Path,
    ) -> None:
        config = _make_minimal_config()
        dir_a = tmp_path / "a"
        dir_b = tmp_path / "b"
        run_pipeline(config, RESOURCES_DIR, dir_a)
        run_pipeline(config, RESOURCES_DIR, dir_b)
        result = verify_output(dir_a, dir_b)
        assert result.success is True


class TestIdempotency:

    def test_pipeline_is_idempotent(
        self, tmp_path: Path,
    ) -> None:
        config = _make_minimal_config()
        dir_a = tmp_path / "run1"
        dir_b = tmp_path / "run2"
        run_pipeline(config, RESOURCES_DIR, dir_a)
        run_pipeline(config, RESOURCES_DIR, dir_b)
        result = verify_output(dir_a, dir_b)
        assert result.success is True


class TestEmptyReference:

    def test_all_files_reported_as_extra(
        self, tmp_path: Path,
    ) -> None:
        config = _make_minimal_config()
        python_dir = tmp_path / "output"
        ref_dir = tmp_path / "empty_ref"
        run_pipeline(config, RESOURCES_DIR, python_dir)
        ref_dir.mkdir()
        result = verify_output(python_dir, ref_dir)
        assert result.success is False
        assert len(result.extra_files) > 0
        assert result.missing_files == []


class TestEmptyOutput:

    def test_all_files_reported_as_missing(
        self, tmp_path: Path,
    ) -> None:
        ref_dir = tmp_path / "reference"
        python_dir = tmp_path / "empty_output"
        create_file_tree(ref_dir, {"a.txt": "a", "b.txt": "b"})
        python_dir.mkdir()
        result = verify_output(python_dir, ref_dir)
        assert result.success is False
        assert len(result.missing_files) == 2
        assert result.extra_files == []


class TestInvalidDirectories:

    def test_nonexistent_python_dir_raises_valueerror(
        self, tmp_path: Path,
    ) -> None:
        ref_dir = tmp_path / "ref"
        ref_dir.mkdir()
        with pytest.raises(ValueError, match="python_dir"):
            verify_output(tmp_path / "nope", ref_dir)

    def test_nonexistent_reference_dir_raises_valueerror(
        self, tmp_path: Path,
    ) -> None:
        py_dir = tmp_path / "py"
        py_dir.mkdir()
        with pytest.raises(ValueError, match="reference_dir"):
            verify_output(py_dir, tmp_path / "nope")

    def test_file_as_python_dir_raises_valueerror(
        self, tmp_path: Path,
    ) -> None:
        file_path = tmp_path / "not_a_dir"
        file_path.write_text("content")
        ref_dir = tmp_path / "ref"
        ref_dir.mkdir()
        with pytest.raises(ValueError, match="not a directory"):
            verify_output(file_path, ref_dir)

    def test_file_as_reference_dir_raises_valueerror(
        self, tmp_path: Path,
    ) -> None:
        py_dir = tmp_path / "py"
        py_dir.mkdir()
        file_path = tmp_path / "not_a_dir"
        file_path.write_text("content")
        with pytest.raises(ValueError, match="not a directory"):
            verify_output(py_dir, file_path)
