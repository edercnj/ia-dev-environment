from __future__ import annotations

from pathlib import Path
from typing import List

import pytest

from claude_setup.domain.protocol_mapping import (
    derive_protocol_files,
    derive_protocols,
)
from claude_setup.models import ProjectConfig

FIXTURES_SRC = Path(__file__).parent.parent / "fixtures" / "src"


class TestDeriveProtocols:

    @pytest.mark.parametrize(
        "interfaces, expected",
        [
            ([{"type": "rest"}], ["rest"]),
            ([{"type": "grpc"}], ["grpc"]),
            ([{"type": "graphql"}], ["graphql"]),
            ([{"type": "websocket"}], ["websocket"]),
            (
                [{"type": "event-consumer", "broker": "kafka"}],
                ["event-driven", "messaging"],
            ),
            (
                [{"type": "event-producer"}],
                ["event-driven", "messaging"],
            ),
            ([{"type": "cli"}], []),
        ],
        ids=[
            "rest", "grpc", "graphql", "websocket",
            "event-consumer", "event-producer", "cli",
        ],
    )
    def test_derive_protocols_by_interface(
        self,
        create_project_config,
        interfaces: list,
        expected: List[str],
    ) -> None:
        config = create_project_config(
            interfaces=interfaces,
        )
        result = derive_protocols(config)
        assert result == expected

    def test_derive_protocols_deduplicates(
        self,
        create_project_config,
    ) -> None:
        config = create_project_config(
            interfaces=[
                {"type": "event-consumer", "broker": "kafka"},
                {"type": "event-producer"},
            ],
        )
        result = derive_protocols(config)
        assert result == ["event-driven", "messaging"]
        assert len(result) == len(set(result))

    def test_derive_protocols_cli_only_returns_empty(
        self,
        create_project_config,
    ) -> None:
        config = create_project_config(
            interfaces=[{"type": "cli"}],
        )
        assert derive_protocols(config) == []

    def test_derive_protocols_no_interfaces_returns_empty(
        self,
        create_project_config,
    ) -> None:
        config = create_project_config(interfaces=[])
        assert derive_protocols(config) == []

    def test_derive_protocols_event_prefix_fallback(
        self,
        create_project_config,
    ) -> None:
        config = create_project_config(
            interfaces=[{"type": "event-custom"}],
        )
        result = derive_protocols(config)
        assert result == ["event-driven"]

    def test_derive_protocols_multiple_interfaces(
        self,
        create_project_config,
    ) -> None:
        config = create_project_config(
            interfaces=[
                {"type": "rest"},
                {"type": "grpc"},
            ],
        )
        result = derive_protocols(config)
        assert result == ["grpc", "rest"]

    def test_derive_protocols_sorted(
        self,
        create_project_config,
    ) -> None:
        config = create_project_config(
            interfaces=[
                {"type": "rest"},
                {"type": "grpc"},
                {"type": "event-consumer"},
            ],
        )
        result = derive_protocols(config)
        assert result == sorted(result)


class TestDeriveProtocolFiles:

    def test_derive_protocol_files_returns_md(
        self,
        create_project_config,
    ) -> None:
        config = create_project_config(
            interfaces=[{"type": "rest"}],
        )
        result = derive_protocol_files(
            FIXTURES_SRC, ["rest"], config,
        )
        assert "rest" in result
        assert all(f.suffix == ".md" for f in result["rest"])

    def test_derive_protocol_files_multiple_protocols(
        self,
        create_project_config,
    ) -> None:
        config = create_project_config(
            interfaces=[
                {"type": "rest"},
                {"type": "grpc"},
            ],
        )
        result = derive_protocol_files(
            FIXTURES_SRC, ["rest", "grpc"], config,
        )
        assert "rest" in result
        assert "grpc" in result

    def test_derive_protocol_files_kafka_broker_selects_kafka(
        self,
        create_project_config,
    ) -> None:
        config = create_project_config(
            interfaces=[
                {"type": "event-consumer", "broker": "kafka"},
            ],
        )
        result = derive_protocol_files(
            FIXTURES_SRC, ["messaging"], config,
        )
        assert "messaging" in result
        files = result["messaging"]
        assert len(files) == 1
        assert files[0].name == "kafka.md"

    def test_derive_protocol_files_rabbitmq_broker(
        self,
        create_project_config,
    ) -> None:
        config = create_project_config(
            interfaces=[
                {"type": "event-consumer", "broker": "rabbitmq"},
            ],
        )
        result = derive_protocol_files(
            FIXTURES_SRC, ["messaging"], config,
        )
        assert "messaging" in result
        files = result["messaging"]
        assert len(files) == 1
        assert files[0].name == "rabbitmq.md"

    def test_derive_protocol_files_unknown_broker_all(
        self,
        create_project_config,
    ) -> None:
        config = create_project_config(
            interfaces=[
                {"type": "event-consumer", "broker": "pulsar"},
            ],
        )
        result = derive_protocol_files(
            FIXTURES_SRC, ["messaging"], config,
        )
        assert "messaging" in result
        assert len(result["messaging"]) >= 2

    def test_derive_protocol_files_no_broker_all(
        self,
        create_project_config,
    ) -> None:
        config = create_project_config(
            interfaces=[{"type": "event-consumer"}],
        )
        result = derive_protocol_files(
            FIXTURES_SRC, ["messaging"], config,
        )
        assert "messaging" in result
        assert len(result["messaging"]) >= 2

    def test_derive_protocol_files_missing_dir_skipped(
        self,
        create_project_config,
    ) -> None:
        config = create_project_config(
            interfaces=[{"type": "rest"}],
        )
        result = derive_protocol_files(
            FIXTURES_SRC, ["nonexistent-protocol"], config,
        )
        assert result == {}

    def test_derive_protocol_files_nonexistent_src(
        self,
        create_project_config,
    ) -> None:
        config = create_project_config(
            interfaces=[{"type": "rest"}],
        )
        result = derive_protocol_files(
            Path("/tmp/nonexistent"), ["rest"], config,
        )
        assert result == {}
