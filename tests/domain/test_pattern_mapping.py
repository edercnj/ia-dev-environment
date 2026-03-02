from __future__ import annotations

from pathlib import Path
from typing import List

import pytest

from claude_setup.domain.pattern_mapping import (
    select_pattern_files,
    select_patterns,
)
from claude_setup.models import ProjectConfig

FIXTURES_SRC = Path(__file__).parent.parent / "fixtures" / "src"


class TestSelectPatterns:

    @pytest.mark.parametrize(
        "style, expected",
        [
            (
                "microservice",
                [
                    "architectural", "data", "integration",
                    "microservice", "resilience",
                ],
            ),
            (
                "hexagonal",
                ["architectural", "data", "integration"],
            ),
            (
                "library",
                ["architectural", "data"],
            ),
            (
                "monolith",
                ["architectural", "data", "integration"],
            ),
            ("unknown-style", []),
        ],
        ids=[
            "microservice", "hexagonal", "library",
            "monolith", "unknown",
        ],
    )
    def test_select_patterns_by_style(
        self,
        create_project_config,
        style: str,
        expected: List[str],
    ) -> None:
        config = create_project_config(
            architecture={"style": style},
        )
        result = select_patterns(config)
        assert result == expected

    def test_select_patterns_event_driven_adds_patterns(
        self,
        create_project_config,
    ) -> None:
        config = create_project_config(
            architecture={
                "style": "microservice",
                "event_driven": True,
            },
        )
        result = select_patterns(config)
        assert "dead-letter-queue" in result
        assert "event-sourcing" in result
        assert "outbox-pattern" in result
        assert "saga-pattern" in result

    def test_select_patterns_event_driven_library(
        self,
        create_project_config,
    ) -> None:
        config = create_project_config(
            architecture={
                "style": "library",
                "event_driven": True,
            },
        )
        result = select_patterns(config)
        assert "architectural" in result
        assert "data" in result
        assert "saga-pattern" in result

    def test_select_patterns_unknown_returns_empty(
        self,
        create_project_config,
    ) -> None:
        config = create_project_config(
            architecture={"style": "serverless"},
        )
        assert select_patterns(config) == []

    def test_select_patterns_deduplicated(
        self,
        create_project_config,
    ) -> None:
        config = create_project_config(
            architecture={"style": "microservice"},
        )
        result = select_patterns(config)
        assert len(result) == len(set(result))

    def test_select_patterns_sorted(
        self,
        create_project_config,
    ) -> None:
        config = create_project_config(
            architecture={"style": "microservice"},
        )
        result = select_patterns(config)
        assert result == sorted(result)


class TestSelectPatternFiles:

    def test_select_pattern_files_returns_md(self) -> None:
        files = select_pattern_files(
            FIXTURES_SRC, ["architectural"],
        )
        assert len(files) >= 1
        assert all(f.suffix == ".md" for f in files)

    def test_select_pattern_files_multiple_categories(
        self,
    ) -> None:
        files = select_pattern_files(
            FIXTURES_SRC, ["architectural", "data"],
        )
        names = [f.name for f in files]
        assert "hexagonal-architecture.md" in names
        assert "repository-pattern.md" in names

    def test_select_pattern_files_sorted(self) -> None:
        files = select_pattern_files(
            FIXTURES_SRC,
            ["architectural", "data", "microservice"],
        )
        arch_files = [f for f in files if "architectural" in str(f)]
        assert arch_files == sorted(arch_files)

    def test_select_pattern_files_missing_dir_skipped(
        self,
    ) -> None:
        files = select_pattern_files(
            FIXTURES_SRC, ["nonexistent-category"],
        )
        assert files == []

    def test_select_pattern_files_mixed_existing_missing(
        self,
    ) -> None:
        files = select_pattern_files(
            FIXTURES_SRC,
            ["architectural", "nonexistent"],
        )
        assert len(files) >= 1
        assert all("architectural" in str(f) for f in files)

    def test_select_pattern_files_empty_categories(
        self,
    ) -> None:
        files = select_pattern_files(FIXTURES_SRC, [])
        assert files == []

    def test_select_pattern_files_nonexistent_src_dir(
        self,
    ) -> None:
        files = select_pattern_files(
            Path("/tmp/nonexistent"), ["architectural"],
        )
        assert files == []
