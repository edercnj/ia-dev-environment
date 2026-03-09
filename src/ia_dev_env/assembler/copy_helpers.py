from __future__ import annotations

import shutil
from pathlib import Path
from typing import Optional

from ia_dev_env.template_engine import TemplateEngine


def copy_template_file(
    src: Path,
    dest: Path,
    engine: TemplateEngine,
) -> Path:
    """Copy a single template file with placeholder replacement."""
    dest.parent.mkdir(parents=True, exist_ok=True)
    content = src.read_text(encoding="utf-8")
    replaced = engine.replace_placeholders(content)
    dest.write_text(replaced, encoding="utf-8")
    return dest


def copy_template_file_if_exists(
    src: Path,
    dest: Path,
    engine: TemplateEngine,
) -> Optional[Path]:
    """Copy a template file if source exists."""
    if not src.exists():
        return None
    return copy_template_file(src, dest, engine)


def copy_template_tree(
    src: Path,
    dest: Path,
    engine: TemplateEngine,
) -> Path:
    """Copy a directory tree with placeholder replacement in .md files."""
    shutil.copytree(src, dest, dirs_exist_ok=True)
    replace_placeholders_in_dir(dest, engine)
    return dest


def copy_template_tree_if_exists(
    src: Path,
    dest: Path,
    engine: TemplateEngine,
) -> Optional[Path]:
    """Copy a directory tree if source exists."""
    if not src.exists():
        return None
    return copy_template_tree(src, dest, engine)


def replace_placeholders_in_dir(
    directory: Path,
    engine: TemplateEngine,
) -> None:
    """Replace placeholders in all .md files in directory."""
    for md_file in directory.rglob("*.md"):
        content = md_file.read_text(encoding="utf-8")
        replaced = engine.replace_placeholders(content)
        md_file.write_text(replaced, encoding="utf-8")
