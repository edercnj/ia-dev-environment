from __future__ import annotations

from pathlib import Path
from typing import Dict, List

from ia_dev_env.models import ProjectConfig

UNIVERSAL_PATTERNS: List[str] = ["architectural", "data"]

ARCHITECTURE_PATTERNS: Dict[str, List[str]] = {
    "microservice": [
        "microservice",
        "resilience",
        "integration",
    ],
    "hexagonal": ["integration"],
    "monolith": ["integration"],
    "library": [],
}

EVENT_DRIVEN_PATTERNS: List[str] = [
    "saga-pattern",
    "outbox-pattern",
    "event-sourcing",
    "dead-letter-queue",
]


def select_patterns(config: ProjectConfig) -> List[str]:
    """Map architecture style to pattern category directories.

    Returns sorted, deduplicated list of pattern category names.
    Returns empty list for unknown styles.
    """
    style = config.architecture.style
    if style not in ARCHITECTURE_PATTERNS:
        return []
    categories = list(UNIVERSAL_PATTERNS)
    categories.extend(ARCHITECTURE_PATTERNS[style])
    if config.architecture.event_driven:
        categories.extend(EVENT_DRIVEN_PATTERNS)
    return sorted(set(categories))


def select_pattern_files(
    resources_dir: Path,
    pattern_categories: List[str],
) -> List[Path]:
    """List .md files from pattern category directories.

    Skips missing directories without error.
    Returns sorted list of file paths.
    """
    patterns_root = resources_dir / "patterns"
    files: List[Path] = []
    for category in pattern_categories:
        category_dir = patterns_root / category
        if not category_dir.is_dir():
            continue
        md_files = sorted(category_dir.glob("*.md"))
        files.extend(md_files)
    return files
