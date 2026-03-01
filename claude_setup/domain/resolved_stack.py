from __future__ import annotations

from dataclasses import dataclass, field
from typing import List


@dataclass(frozen=True)
class ResolvedStack:
    """Computed stack values derived from ProjectConfig."""

    build_cmd: str
    test_cmd: str
    compile_cmd: str
    coverage_cmd: str
    docker_base_image: str
    health_path: str
    package_manager: str
    default_port: int
    file_extension: str
    build_file: str
    native_supported: bool
    project_type: str
    protocols: List[str] = field(default_factory=list)
