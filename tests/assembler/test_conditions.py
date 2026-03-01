from __future__ import annotations

import pytest

from claude_setup.assembler.conditions import (
    extract_interface_types,
    has_any_interface,
    has_interface,
)
from claude_setup.models import ProjectConfig


class TestExtractInterfaceTypes:

    def test_single_interface_returns_list(self, config_factory) -> None:
        config = config_factory(interfaces=[{"type": "rest"}])
        assert extract_interface_types(config) == ["rest"]

    def test_multiple_interfaces_returns_all(self, config_factory) -> None:
        config = config_factory(
            interfaces=[
                {"type": "rest"},
                {"type": "grpc"},
                {"type": "event-consumer"},
            ],
        )
        result = extract_interface_types(config)
        assert result == ["rest", "grpc", "event-consumer"]

    def test_cli_only_returns_cli(self, minimal_cli_config) -> None:
        assert extract_interface_types(minimal_cli_config) == ["cli"]

    def test_empty_interfaces_returns_empty(self, config_factory) -> None:
        config = config_factory(interfaces=[])
        assert extract_interface_types(config) == []

    def test_preserves_order(self, config_factory) -> None:
        config = config_factory(
            interfaces=[
                {"type": "grpc"},
                {"type": "rest"},
                {"type": "cli"},
            ],
        )
        assert extract_interface_types(config) == ["grpc", "rest", "cli"]


class TestHasInterface:

    def test_present_returns_true(self, config_factory) -> None:
        config = config_factory(
            interfaces=[{"type": "rest"}, {"type": "grpc"}],
        )
        assert has_interface(config, "rest") is True

    def test_absent_returns_false(self, minimal_cli_config) -> None:
        assert has_interface(minimal_cli_config, "rest") is False

    def test_event_consumer_present_returns_true(
        self, config_factory,
    ) -> None:
        config = config_factory(
            interfaces=[{"type": "event-consumer"}],
        )
        assert has_interface(config, "event-consumer") is True

    def test_empty_interfaces_returns_false(
        self, config_factory,
    ) -> None:
        config = config_factory(interfaces=[])
        assert has_interface(config, "rest") is False

    def test_unknown_type_returns_false(
        self, minimal_cli_config,
    ) -> None:
        assert has_interface(minimal_cli_config, "unknown-protocol") is False


class TestHasAnyInterface:

    def test_one_match_returns_true(self, config_factory) -> None:
        config = config_factory(interfaces=[{"type": "rest"}])
        assert has_any_interface(config, "rest", "grpc") is True

    def test_no_match_returns_false(self, minimal_cli_config) -> None:
        assert has_any_interface(minimal_cli_config, "rest", "grpc") is False

    def test_multiple_matches_returns_true(
        self, config_factory,
    ) -> None:
        config = config_factory(
            interfaces=[{"type": "rest"}, {"type": "grpc"}],
        )
        assert has_any_interface(config, "rest", "grpc") is True

    def test_empty_types_returns_false(self, config_factory) -> None:
        config = config_factory(interfaces=[{"type": "rest"}])
        assert has_any_interface(config) is False

    def test_empty_interfaces_returns_false(
        self, config_factory,
    ) -> None:
        config = config_factory(interfaces=[])
        assert has_any_interface(config, "rest") is False
