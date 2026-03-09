from __future__ import annotations

import pytest

from ia_dev_env.assembler.agents import AgentsAssembler


@pytest.fixture
def assembler():
    return AgentsAssembler()


# --- select_conditional_agents ---


class TestSelectConditionalAgents:

    def test_database_includes_database_engineer(
        self, assembler, config_factory,
    ) -> None:
        config = config_factory(
            data={"database": {"name": "postgresql", "version": "15"}},
        )
        result = assembler.select_conditional_agents(config)
        assert "database-engineer.md" in result

    def test_database_none_excludes_database_engineer(
        self, assembler, minimal_cli_config,
    ) -> None:
        result = assembler.select_conditional_agents(minimal_cli_config)
        assert "database-engineer.md" not in result

    def test_observability_includes_observability_engineer(
        self, assembler, config_factory,
    ) -> None:
        config = config_factory(
            infrastructure={
                "observability": {"tool": "opentelemetry"},
            },
        )
        result = assembler.select_conditional_agents(config)
        assert "observability-engineer.md" in result

    def test_observability_none_excludes_observability_engineer(
        self, assembler, minimal_cli_config,
    ) -> None:
        result = assembler.select_conditional_agents(minimal_cli_config)
        assert "observability-engineer.md" not in result

    def test_container_includes_devops_engineer(
        self, assembler, config_factory,
    ) -> None:
        config = config_factory(
            infrastructure={"container": "docker"},
        )
        result = assembler.select_conditional_agents(config)
        assert "devops-engineer.md" in result

    def test_orchestrator_includes_devops_engineer(
        self, assembler, config_factory,
    ) -> None:
        config = config_factory(
            infrastructure={
                "container": "none",
                "orchestrator": "kubernetes",
            },
        )
        result = assembler.select_conditional_agents(config)
        assert "devops-engineer.md" in result

    def test_iac_includes_devops_engineer(
        self, assembler, config_factory,
    ) -> None:
        config = config_factory(
            infrastructure={
                "container": "none",
                "iac": "terraform",
            },
        )
        result = assembler.select_conditional_agents(config)
        assert "devops-engineer.md" in result

    def test_service_mesh_includes_devops_engineer(
        self, assembler, config_factory,
    ) -> None:
        config = config_factory(
            infrastructure={
                "container": "none",
                "service_mesh": "istio",
            },
        )
        result = assembler.select_conditional_agents(config)
        assert "devops-engineer.md" in result

    def test_no_infra_excludes_devops_engineer(
        self, assembler, config_factory,
    ) -> None:
        config = config_factory(
            infrastructure={
                "container": "none",
                "orchestrator": "none",
                "iac": "none",
                "service_mesh": "none",
            },
        )
        result = assembler.select_conditional_agents(config)
        assert "devops-engineer.md" not in result

    def test_rest_includes_api_engineer(
        self, assembler, config_factory,
    ) -> None:
        config = config_factory(interfaces=[{"type": "rest"}])
        result = assembler.select_conditional_agents(config)
        assert "api-engineer.md" in result

    def test_grpc_includes_api_engineer(
        self, assembler, config_factory,
    ) -> None:
        config = config_factory(interfaces=[{"type": "grpc"}])
        result = assembler.select_conditional_agents(config)
        assert "api-engineer.md" in result

    def test_graphql_includes_api_engineer(
        self, assembler, config_factory,
    ) -> None:
        config = config_factory(interfaces=[{"type": "graphql"}])
        result = assembler.select_conditional_agents(config)
        assert "api-engineer.md" in result

    def test_cli_only_excludes_api_engineer(
        self, assembler, minimal_cli_config,
    ) -> None:
        result = assembler.select_conditional_agents(minimal_cli_config)
        assert "api-engineer.md" not in result

    def test_event_driven_includes_event_engineer(
        self, assembler, config_factory,
    ) -> None:
        config = config_factory(
            architecture={"style": "library", "event_driven": True},
        )
        result = assembler.select_conditional_agents(config)
        assert "event-engineer.md" in result

    def test_event_consumer_includes_event_engineer(
        self, assembler, config_factory,
    ) -> None:
        config = config_factory(
            interfaces=[{"type": "event-consumer"}],
        )
        result = assembler.select_conditional_agents(config)
        assert "event-engineer.md" in result

    def test_event_producer_includes_event_engineer(
        self, assembler, config_factory,
    ) -> None:
        config = config_factory(
            interfaces=[{"type": "event-producer"}],
        )
        result = assembler.select_conditional_agents(config)
        assert "event-engineer.md" in result

    def test_no_events_excludes_event_engineer(
        self, assembler, minimal_cli_config,
    ) -> None:
        result = assembler.select_conditional_agents(minimal_cli_config)
        assert "event-engineer.md" not in result

    def test_full_featured_includes_all(
        self, assembler, full_featured_config,
    ) -> None:
        result = assembler.select_conditional_agents(full_featured_config)
        expected = [
            "database-engineer.md",
            "observability-engineer.md",
            "devops-engineer.md",
            "api-engineer.md",
            "event-engineer.md",
        ]
        for agent in expected:
            assert agent in result

    def test_minimal_excludes_all_conditional(
        self, assembler, config_factory,
    ) -> None:
        config = config_factory(
            infrastructure={
                "container": "none",
                "orchestrator": "none",
                "iac": "none",
                "service_mesh": "none",
            },
        )
        result = assembler.select_conditional_agents(config)
        assert result == []


# --- select_developer_agent ---


class TestSelectDeveloperAgent:

    @pytest.mark.parametrize(
        "language, expected",
        [
            ("python", "python-developer.md"),
            ("java", "java-developer.md"),
            ("typescript", "typescript-developer.md"),
            ("go", "go-developer.md"),
            ("kotlin", "kotlin-developer.md"),
            ("rust", "rust-developer.md"),
        ],
    )
    def test_language_mapping(
        self, assembler, config_factory, language, expected,
    ) -> None:
        config = config_factory(
            language={"name": language, "version": "1.0"},
        )
        assert assembler.select_developer_agent(config) == expected

    def test_unknown_language_maps_directly(
        self, assembler, config_factory,
    ) -> None:
        config = config_factory(
            language={"name": "cobol", "version": "85"},
        )
        assert assembler.select_developer_agent(config) == "cobol-developer.md"


# --- select_core_agents ---


class TestSelectCoreAgents:

    def test_scans_core_directory(self, assembler, tmp_path) -> None:
        core = tmp_path / "agents-templates" / "core"
        core.mkdir(parents=True)
        (core / "review.md").write_text("# Review", encoding="utf-8")
        (core / "implement.md").write_text("# Impl", encoding="utf-8")
        result = assembler.select_core_agents(tmp_path)
        assert "review.md" in result
        assert "implement.md" in result

    def test_only_md_files(self, assembler, tmp_path) -> None:
        core = tmp_path / "agents-templates" / "core"
        core.mkdir(parents=True)
        (core / "review.md").write_text("# Review", encoding="utf-8")
        (core / ".gitkeep").write_text("", encoding="utf-8")
        result = assembler.select_core_agents(tmp_path)
        assert "review.md" in result
        assert ".gitkeep" not in result

    def test_empty_core_returns_empty(self, assembler, tmp_path) -> None:
        core = tmp_path / "agents-templates" / "core"
        core.mkdir(parents=True)
        assert assembler.select_core_agents(tmp_path) == []

    def test_no_core_dir_returns_empty(self, assembler, tmp_path) -> None:
        assert assembler.select_core_agents(tmp_path) == []
