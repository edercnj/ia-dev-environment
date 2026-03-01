from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import List, Tuple

MAX_FILE_COUNT = 10
MAX_TOTAL_BYTES = 51200  # 50KB


@dataclass(frozen=True)
class AuditResult:
    total_files: int
    total_bytes: int
    file_sizes: List[Tuple[str, int]]
    warnings: List[str]


def audit_rules_context(rules_dir: Path) -> AuditResult:
    """Audit rules directory for file count and size thresholds."""
    if not rules_dir.is_dir():
        return AuditResult(
            total_files=0,
            total_bytes=0,
            file_sizes=[],
            warnings=[],
        )
    file_sizes = _collect_file_sizes(rules_dir)
    total_files = len(file_sizes)
    total_bytes = sum(size for _, size in file_sizes)
    warnings = _check_thresholds(total_files, total_bytes)
    return AuditResult(
        total_files=total_files,
        total_bytes=total_bytes,
        file_sizes=file_sizes,
        warnings=warnings,
    )


def _collect_file_sizes(rules_dir: Path) -> List[Tuple[str, int]]:
    sizes: List[Tuple[str, int]] = []
    for md_file in sorted(rules_dir.glob("*.md")):
        if md_file.is_file():
            sizes.append((md_file.name, md_file.stat().st_size))
    return sorted(sizes, key=lambda x: x[1], reverse=True)


def _check_thresholds(
    total_files: int,
    total_bytes: int,
) -> List[str]:
    warnings: List[str] = []
    if total_files > MAX_FILE_COUNT:
        warnings.append(
            f"{total_files} rule files exceeds recommended maximum of {MAX_FILE_COUNT}."
        )
    if total_bytes > MAX_TOTAL_BYTES:
        total_kb = total_bytes // 1024
        warnings.append(
            f"{total_kb}KB total rules exceeds recommended maximum of 50KB."
        )
    return warnings
