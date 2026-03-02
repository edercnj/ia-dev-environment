from __future__ import annotations

import difflib
from pathlib import Path
from typing import List, Optional

from claude_setup.models import FileDiff, VerificationResult

BINARY_DIFF_MESSAGE = "<binary files differ>"
MAX_DIFF_LINES = 200


def verify_output(
    python_dir: Path,
    reference_dir: Path,
) -> VerificationResult:
    """Compare two directory trees byte-for-byte."""
    _validate_directory(python_dir, "python_dir")
    _validate_directory(reference_dir, "reference_dir")
    python_paths = set(_collect_relative_paths(python_dir))
    reference_paths = set(_collect_relative_paths(reference_dir))
    missing = sorted(reference_paths - python_paths)
    extra = sorted(python_paths - reference_paths)
    common = sorted(reference_paths & python_paths)
    mismatches = _find_mismatches(
        python_dir, reference_dir, common,
    )
    total = len(reference_paths | python_paths)
    success = not mismatches and not missing and not extra
    return VerificationResult(
        success=success,
        total_files=total,
        mismatches=mismatches,
        missing_files=[Path(p) for p in missing],
        extra_files=[Path(p) for p in extra],
    )


def _validate_directory(path: Path, name: str) -> None:
    """Raise ValueError if path is not an existing directory."""
    if not path.exists():
        raise ValueError(
            f"{name} does not exist: {path}"
        )
    if not path.is_dir():
        raise ValueError(
            f"{name} is not a directory: {path}"
        )


def _collect_relative_paths(base_dir: Path) -> List[Path]:
    """Walk directory recursively, return sorted relative paths."""
    paths = []
    for item in sorted(base_dir.rglob("*")):
        if item.is_file():
            paths.append(item.relative_to(base_dir))
    return paths


def _find_mismatches(
    python_dir: Path,
    reference_dir: Path,
    common_paths: List[Path],
) -> List[FileDiff]:
    """Compare common files, return list of mismatches."""
    mismatches: List[FileDiff] = []
    for rel_path in common_paths:
        result = _compare_files(
            python_dir / rel_path,
            reference_dir / rel_path,
            rel_path,
        )
        if result is not None:
            mismatches.append(result)
    return mismatches


def _compare_files(
    python_file: Path,
    reference_file: Path,
    relative_path: Path,
) -> Optional[FileDiff]:
    """Compare two files byte-for-byte, return FileDiff if different."""
    python_bytes = python_file.read_bytes()
    reference_bytes = reference_file.read_bytes()
    if python_bytes == reference_bytes:
        return None
    diff_text = _generate_text_diff(
        python_file, reference_file, relative_path,
    )
    return FileDiff(
        path=relative_path,
        diff=diff_text,
        python_size=len(python_bytes),
        reference_size=len(reference_bytes),
    )


def _generate_text_diff(
    python_path: Path,
    reference_path: Path,
    relative_path: Path,
) -> str:
    """Generate unified diff string for text files."""
    try:
        python_lines = python_path.read_text(
            encoding="utf-8",
        ).splitlines(keepends=True)
        reference_lines = reference_path.read_text(
            encoding="utf-8",
        ).splitlines(keepends=True)
    except UnicodeDecodeError:
        return BINARY_DIFF_MESSAGE
    diff = difflib.unified_diff(
        reference_lines,
        python_lines,
        fromfile=f"reference/{relative_path}",
        tofile=f"python/{relative_path}",
    )
    lines = list(diff)[:MAX_DIFF_LINES]
    return "".join(lines)
