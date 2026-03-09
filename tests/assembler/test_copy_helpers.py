from __future__ import annotations

from pathlib import Path

import pytest

from ia_dev_env.assembler.copy_helpers import (
    copy_template_file,
    copy_template_file_if_exists,
    copy_template_tree,
    copy_template_tree_if_exists,
    replace_placeholders_in_dir,
)
from ia_dev_env.models import ProjectConfig
from ia_dev_env.template_engine import TemplateEngine


@pytest.fixture
def engine(minimal_project_dict, tmp_path):
    config = ProjectConfig.from_dict(minimal_project_dict)
    return TemplateEngine(tmp_path, config)


class TestCopyTemplateFile:

    def test_creates_parent_dirs(self, tmp_path, engine) -> None:
        src = tmp_path / "src" / "file.md"
        src.parent.mkdir(parents=True)
        src.write_text("hello", encoding="utf-8")
        dest = tmp_path / "out" / "nested" / "file.md"
        result = copy_template_file(src, dest, engine)
        assert result == dest
        assert dest.read_text(encoding="utf-8") == "hello"

    def test_replaces_placeholders(self, tmp_path, engine) -> None:
        src = tmp_path / "src" / "file.md"
        src.parent.mkdir(parents=True)
        src.write_text("lang: {language_name}", encoding="utf-8")
        dest = tmp_path / "out" / "file.md"
        copy_template_file(src, dest, engine)
        assert "python" in dest.read_text(encoding="utf-8")


class TestCopyTemplateFileIfExists:

    def test_returns_none_when_missing(self, tmp_path, engine) -> None:
        src = tmp_path / "missing.md"
        dest = tmp_path / "out" / "missing.md"
        assert copy_template_file_if_exists(src, dest, engine) is None

    def test_copies_when_exists(self, tmp_path, engine) -> None:
        src = tmp_path / "file.md"
        src.write_text("content", encoding="utf-8")
        dest = tmp_path / "out" / "file.md"
        result = copy_template_file_if_exists(src, dest, engine)
        assert result == dest
        assert dest.exists()


class TestCopyTemplateTree:

    def test_copies_directory_tree(self, tmp_path, engine) -> None:
        src = tmp_path / "src" / "skill"
        (src / "sub").mkdir(parents=True)
        (src / "SKILL.md").write_text("# {project_name}", encoding="utf-8")
        (src / "sub" / "ref.md").write_text("ref", encoding="utf-8")
        dest = tmp_path / "out" / "skill"
        result = copy_template_tree(src, dest, engine)
        assert result == dest
        assert (dest / "SKILL.md").exists()
        assert (dest / "sub" / "ref.md").exists()

    def test_replaces_placeholders_in_md(self, tmp_path, engine) -> None:
        src = tmp_path / "src" / "skill"
        src.mkdir(parents=True)
        (src / "file.md").write_text("{language_name}", encoding="utf-8")
        dest = tmp_path / "out" / "skill"
        copy_template_tree(src, dest, engine)
        assert "python" in (dest / "file.md").read_text(encoding="utf-8")


class TestCopyTemplateTreeIfExists:

    def test_returns_none_when_missing(self, tmp_path, engine) -> None:
        src = tmp_path / "missing"
        dest = tmp_path / "out" / "missing"
        assert copy_template_tree_if_exists(src, dest, engine) is None

    def test_copies_when_exists(self, tmp_path, engine) -> None:
        src = tmp_path / "src"
        src.mkdir(parents=True)
        (src / "file.md").write_text("x", encoding="utf-8")
        dest = tmp_path / "out"
        result = copy_template_tree_if_exists(src, dest, engine)
        assert result == dest


class TestReplacePlaceholdersInDir:

    def test_replaces_in_nested_md(self, tmp_path, engine) -> None:
        (tmp_path / "sub").mkdir()
        (tmp_path / "file.md").write_text("{language_name}", encoding="utf-8")
        (tmp_path / "sub" / "nested.md").write_text("{framework_name}", encoding="utf-8")
        replace_placeholders_in_dir(tmp_path, engine)
        assert "python" in (tmp_path / "file.md").read_text(encoding="utf-8")
        assert "click" in (tmp_path / "sub" / "nested.md").read_text(encoding="utf-8")

    def test_ignores_non_md_files(self, tmp_path, engine) -> None:
        (tmp_path / "file.txt").write_text("{language_name}", encoding="utf-8")
        replace_placeholders_in_dir(tmp_path, engine)
        assert (tmp_path / "file.txt").read_text(encoding="utf-8") == "{language_name}"
